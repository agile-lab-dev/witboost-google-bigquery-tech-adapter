package com.witboost.provisioning.bigquery.service.provision;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.witboost.provisioning.bigquery.model.BigQueryStorageSpecific;
import com.witboost.provisioning.bigquery.model.CreateDatasetRequest;
import com.witboost.provisioning.bigquery.model.CreateOrUpdateTableRequest;
import com.witboost.provisioning.bigquery.model.DeleteTableRequest;
import com.witboost.provisioning.bigquery.service.AclService;
import com.witboost.provisioning.bigquery.service.BigQueryService;
import com.witboost.provisioning.bigquery.service.PrincipalMappingService;
import com.witboost.provisioning.framework.service.ProvisionService;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.StorageArea;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.model.status.ProvisionInfo;
import io.vavr.collection.List;
import io.vavr.control.Either;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StorageAreaProvisionService implements ProvisionService {
    private static final Logger logger = LoggerFactory.getLogger(StorageAreaProvisionService.class);

    private final BigQueryService bigQueryService;
    private final PrincipalMappingService principalMappingService;
    private final AclService aclService;

    public StorageAreaProvisionService(
            BigQueryService bigQueryService, PrincipalMappingService principalMappingService, AclService aclService) {
        this.bigQueryService = bigQueryService;
        this.principalMappingService = principalMappingService;
        this.aclService = aclService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Either<FailedOperation, ProvisionInfo> provision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        var system = operationRequest.getDataProduct();
        var st = (StorageArea<BigQueryStorageSpecific>)
                operationRequest.getComponent().get();
        var stSpecific = st.getSpecific();

        var datasetResult = bigQueryService.createDatasetIfNotExists(
                new CreateDatasetRequest(stSpecific.getProject(), stSpecific.getDataset()));

        if (datasetResult.isLeft()) return left(datasetResult.getLeft());

        var updateTableRequest = new CreateOrUpdateTableRequest(
                stSpecific.getProject(), stSpecific.getDataset(), stSpecific.getTableName(), stSpecific.getSchema());

        return bigQueryService.createOrUpdateTable(updateTableRequest).flatMap(table -> {
            var mappedPrincipals = List.ofAll(principalMappingService
                    .map(List.of(system.getDataProductOwner(), system.getDevGroup())
                            .toJavaSet())
                    .entrySet());
            var identities = Either.sequenceRight(mappedPrincipals.map(Map.Entry::getValue));
            return identities.flatMap(ids -> aclService
                    .applyAcls(stSpecific.getOwnerRoles(), ids.asJava(), table.getTableId())
                    .flatMap(v -> right(toProvisionInfo(table))));
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Either<FailedOperation, ProvisionInfo> unprovision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        // controls have already been done on validation
        var st = (StorageArea<BigQueryStorageSpecific>)
                operationRequest.getComponent().get();
        var stSpecific = st.getSpecific();

        var deleteRequest =
                new DeleteTableRequest(stSpecific.getProject(), stSpecific.getDataset(), stSpecific.getTableName());

        var tableId = TableId.of(stSpecific.getProject(), stSpecific.getDataset(), stSpecific.getTableName());
        return aclService
                .revokeRoles(stSpecific.getOwnerRoles(), tableId)
                .flatMap(t ->
                        operationRequest.isRemoveData() ? bigQueryService.deleteTable((deleteRequest)) : right(null))
                .map(t -> ProvisionInfo.builder().build());
    }

    private ProvisionInfo toProvisionInfo(Table table) {
        var url = String.format(
                "https://console.cloud.google.com/bigquery?project=%s&ws=!1m5!1m4!4m3!1s%s!2s%s!3s%s",
                table.getTableId().getProject(),
                table.getTableId().getProject(),
                table.getTableId().getDataset(),
                table.getTableId().getTable());
        var publicInfo = Map.of(
                "project",
                        Map.of(
                                "type",
                                "string",
                                "label",
                                "Project",
                                "value",
                                table.getTableId().getProject()),
                "dataset",
                        Map.of(
                                "type",
                                "string",
                                "label",
                                "Dataset",
                                "value",
                                table.getTableId().getDataset()),
                "table",
                        Map.of(
                                "type",
                                "string",
                                "label",
                                "Table",
                                "value",
                                table.getTableId().getTable()),
                "url", Map.of("type", "string", "label", "Url", "value", "Open in BigQuery", "href", url));

        return ProvisionInfo.builder().publicInfo(Optional.of(publicInfo)).build();
    }
}
