package com.witboost.provisioning.bigquery.service.provision;

import static io.vavr.control.Either.right;

import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.witboost.provisioning.bigquery.model.BigQueryOutputPortSpecific;
import com.witboost.provisioning.bigquery.model.CreateViewRequest;
import com.witboost.provisioning.bigquery.service.AclService;
import com.witboost.provisioning.bigquery.service.BigQueryService;
import com.witboost.provisioning.bigquery.service.PrincipalMappingService;
import com.witboost.provisioning.framework.service.ProvisionService;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.request.AccessControlOperationRequest;
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
public class OutputPortProvisionService implements ProvisionService {
    private static final Logger logger = LoggerFactory.getLogger(OutputPortProvisionService.class);

    private final BigQueryService bigQueryService;
    private final PrincipalMappingService principalMappingService;
    private final AclService aclService;

    private static final String READ_ROLE = "roles/bigquery.dataViewer";

    public OutputPortProvisionService(
            BigQueryService bigQueryService, PrincipalMappingService principalMappingService, AclService aclService) {
        this.bigQueryService = bigQueryService;
        this.principalMappingService = principalMappingService;
        this.aclService = aclService;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Either<FailedOperation, ProvisionInfo> provision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        // controls have already been done on validation
        var system = operationRequest.getDataProduct();
        var op = (OutputPort<BigQueryOutputPortSpecific>)
                operationRequest.getComponent().get();
        var opSpecific = op.getSpecific();

        var viewRequest = new CreateViewRequest(
                opSpecific.getProject(),
                opSpecific.getDataset(),
                opSpecific.getTableName(),
                opSpecific.getViewName(),
                op.getDescription(),
                op.getDataContract().getSchema());
        return bigQueryService.createOrUpdateView(viewRequest).flatMap(view -> {
            var mappedPrincipals = List.ofAll(principalMappingService
                    .map(List.of(system.getDataProductOwner(), system.getDevGroup())
                            .toJavaSet())
                    .entrySet());
            var identities = Either.sequenceRight(mappedPrincipals.map(Map.Entry::getValue));
            return identities.flatMap(ids -> aclService
                    .applyAcls(opSpecific.getOwnerRoles(), ids.asJava(), view.getTableId())
                    .flatMap(v -> right(toProvisionInfo(view))));
        });
    }

    @Override
    @SuppressWarnings("unchecked")
    public Either<FailedOperation, ProvisionInfo> unprovision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {

        // controls have already been done on validation
        var system = operationRequest.getDataProduct();
        var op = (OutputPort<BigQueryOutputPortSpecific>)
                operationRequest.getComponent().get();
        var opSpecific = op.getSpecific();

        var viewId = TableId.of(opSpecific.getProject(), opSpecific.getDataset(), opSpecific.getViewName());
        return aclService
                .revokeRoles(java.util.List.of(READ_ROLE), viewId)
                .flatMap(v -> operationRequest.isRemoveData()
                        ? bigQueryService.deleteView(
                                opSpecific.getProject(), opSpecific.getDataset(), opSpecific.getViewName())
                        : right(null))
                .map(v -> ProvisionInfo.builder().build());
    }

    @Override
    @SuppressWarnings("unchecked")
    public Either<FailedOperation, ProvisionInfo> updateAcl(
            AccessControlOperationRequest<?, ? extends Specific> operationRequest) {
        // controls have already been done on validation
        var op = (OutputPort<BigQueryOutputPortSpecific>)
                operationRequest.getComponent().get();
        var opSpecific = op.getSpecific();

        var viewId = TableId.of(opSpecific.getProject(), opSpecific.getDataset(), opSpecific.getViewName());
        return aclService.revokeRoles(java.util.List.of(READ_ROLE), viewId).flatMap(v -> {
            var mappedPrincipals = List.ofAll(
                    principalMappingService.map(operationRequest.getRefs()).entrySet());
            var identities = Either.sequenceRight(mappedPrincipals.map(Map.Entry::getValue));
            return identities.flatMap(ids -> aclService
                    .applyAcls(java.util.List.of(READ_ROLE), ids.asJava(), viewId)
                    .flatMap(ignored -> right(ProvisionInfo.builder().build())));
        });
    }

    private ProvisionInfo toProvisionInfo(Table view) {
        var url = String.format(
                "https://console.cloud.google.com/bigquery?project=%s&ws=!1m5!1m4!4m3!1s%s!2s%s!3s%s",
                view.getTableId().getProject(),
                view.getTableId().getProject(),
                view.getTableId().getDataset(),
                view.getTableId().getTable());
        var publicInfo = Map.of(
                "project",
                Map.of(
                        "type",
                        "string",
                        "label",
                        "Project",
                        "value",
                        view.getTableId().getProject()),
                "dataset",
                Map.of(
                        "type",
                        "string",
                        "label",
                        "Dataset",
                        "value",
                        view.getTableId().getDataset()),
                "view",
                Map.of(
                        "type",
                        "string",
                        "label",
                        "View",
                        "value",
                        view.getTableId().getTable()),
                "url",
                Map.of("type", "string", "label", "Url", "value", "Open in BigQuery", "href", url));
        return ProvisionInfo.builder().publicInfo(Optional.of(publicInfo)).build();
    }
}
