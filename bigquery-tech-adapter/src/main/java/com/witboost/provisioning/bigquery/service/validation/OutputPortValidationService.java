package com.witboost.provisioning.bigquery.service.validation;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.witboost.provisioning.bigquery.model.BigQueryOutputPortSpecific;
import com.witboost.provisioning.bigquery.service.BigQueryService;
import com.witboost.provisioning.framework.service.validation.ComponentValidationService;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.Specific;
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
public class OutputPortValidationService implements ComponentValidationService {

    private static final Logger logger = LoggerFactory.getLogger(OutputPortValidationService.class);

    private final BigQueryService bigQueryService;

    private final String VALIDATION_ERROR_USER_MESSAGE = "One or more validation errors occurred";

    public OutputPortValidationService(BigQueryService bigQueryService) {
        this.bigQueryService = bigQueryService;
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
        if (component instanceof OutputPort<? extends Specific> op) {
            if (component.getSpecific() instanceof BigQueryOutputPortSpecific bigQueryOPSpecific) {
                return bigQueryService
                        .getTable(
                                bigQueryOPSpecific.getProject(),
                                bigQueryOPSpecific.getDataset(),
                                bigQueryOPSpecific.getTableName())
                        .flatMap(optTable -> optTable.fold(
                                () -> {
                                    String errorMessage = String.format(
                                            "The specified source table %s.%s.%s doesn't exist",
                                            bigQueryOPSpecific.getProject(),
                                            bigQueryOPSpecific.getDataset(),
                                            bigQueryOPSpecific.getTableName());
                                    return left(new FailedOperation(
                                            VALIDATION_ERROR_USER_MESSAGE,
                                            Collections.singletonList(new Problem(errorMessage))));
                                },
                                table -> {
                                    return bigQueryService
                                            .isViewSchemaCompatibleWithSourceTableSchema(
                                                    table.getDefinition().getSchema(),
                                                    op.getDataContract().getSchema())
                                            .flatMap(isCompatible -> {
                                                if (isCompatible) return right(null);
                                                String errorMessage = String.format(
                                                        "View schema of component %s is not compatible with schema of the source table %s.%s.%s",
                                                        component.getId(),
                                                        bigQueryOPSpecific.getProject(),
                                                        bigQueryOPSpecific.getDataset(),
                                                        bigQueryOPSpecific.getTableName());
                                                logger.error(errorMessage);
                                                return left(new FailedOperation(
                                                        VALIDATION_ERROR_USER_MESSAGE,
                                                        Collections.singletonList(new Problem(errorMessage))));
                                            });
                                }));
            }
        }
        // If we arrive here, provisioner errors, so we call the super implementation
        return ComponentValidationService.super.validate(operationRequest, operationType);
    }
}
