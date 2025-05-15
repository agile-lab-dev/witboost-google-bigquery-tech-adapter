package com.witboost.provisioning.bigquery.service.validation;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.witboost.provisioning.bigquery.model.BigQueryStorageSpecific;
import com.witboost.provisioning.bigquery.service.BigQueryService;
import com.witboost.provisioning.bigquery.service.ResourceManagerService;
import com.witboost.provisioning.framework.service.validation.ComponentValidationService;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.StorageArea;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.OperationRequest;
import io.vavr.control.Either;
import jakarta.validation.Valid;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
public class StorageValidationService implements ComponentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(StorageValidationService.class);

    private final BigQueryService bigQueryService;

    private final ResourceManagerService resourceManagerService;

    private final String VALIDATION_ERROR_USER_MESSAGE = "One or more validation errors occurred";

    public StorageValidationService(BigQueryService bigQueryService, ResourceManagerService resourceManagerService) {
        this.bigQueryService = bigQueryService;
        this.resourceManagerService = resourceManagerService;
    }

    @Override
    public Either<FailedOperation, Void> validate(
            @Valid OperationRequest<?, ? extends Specific> operationRequest, OperationType operationType) {
        var maybeComponent = operationRequest.getComponent();
        if (maybeComponent.isEmpty()) {
            return left(
                    new FailedOperation(
                            "No component to provision on input descriptor",
                            Collections.singletonList(
                                    new Problem(
                                            "Operation request didn't contain a component to operate with. Expected a component descriptor"))));
        }
        var component = maybeComponent.get();
        if (component instanceof StorageArea<? extends Specific>) {
            if (component.getSpecific() instanceof BigQueryStorageSpecific bigQueryStorageSpecific) {
                String projectId = bigQueryStorageSpecific.getProject();
                String datasetId = bigQueryStorageSpecific.getDataset();
                String tableId = bigQueryStorageSpecific.getTableName();

                // Check if the project exists
                var projectExists = resourceManagerService.isProjectExisting(projectId);
                if (projectExists.isLeft()) {
                    return left(projectExists.getLeft());
                }
                if (!projectExists.get()) {
                    logger.warn("Project {} does not exist", projectId);
                    return left(new FailedOperation(
                            VALIDATION_ERROR_USER_MESSAGE,
                            Collections.singletonList(
                                    new Problem("The specified BigQuery project does not exist: " + projectId))));
                }

                // Check if the table exists
                var tableOption = bigQueryService.getTable(projectId, datasetId, tableId);
                if (tableOption.isRight()) {
                    var maybeTable = tableOption.get();
                    if (maybeTable.isDefined()) {
                        var table = maybeTable.get();
                        Schema actualSchema = ((StandardTableDefinition) table.getDefinition()).getSchema();

                        // Check schema compatibility within the dataset
                        var schemaCompatible = bigQueryService.isTableSchemaCompatibleWithColumns(
                                actualSchema, bigQueryStorageSpecific.getSchema());
                        if (schemaCompatible.isLeft()) {
                            return left(schemaCompatible.getLeft());
                        }
                        if (!schemaCompatible.get()) {
                            logger.warn(
                                    "Detected schema mismatch: provided schema is not compatible with existing table: %s.%s.%s",
                                    projectId, datasetId, tableId);
                            return left(new FailedOperation(
                                    VALIDATION_ERROR_USER_MESSAGE,
                                    Collections.singletonList(new Problem(
                                            "Detected schema mismatch: provided schema is not compatible with existing table: "
                                                    + projectId + "." + datasetId + "." + tableId))));
                        }
                    }
                } else {
                    return left(tableOption.getLeft());
                }

                return right(null);
            }
        }
        // If we arrive here, provisioner errors, so we call the super implementation
        return ComponentValidationService.super.validate(operationRequest, operationType);
    }
}
