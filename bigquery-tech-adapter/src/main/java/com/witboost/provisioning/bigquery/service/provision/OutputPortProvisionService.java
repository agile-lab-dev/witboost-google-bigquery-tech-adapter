package com.witboost.provisioning.bigquery.service.provision;

import com.witboost.provisioning.framework.service.ProvisionService;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.request.AccessControlOperationRequest;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.model.request.ReverseProvisionOperationRequest;
import com.witboost.provisioning.model.status.ProvisionInfo;
import com.witboost.provisioning.model.status.ReverseProvisionInfo;
import io.vavr.control.Either;
import org.springframework.stereotype.Component;

// TODO If your tech adapter doesn't support output ports, remove this class and its configuration
//  on the ProvisionConfigurationBean
@Component
public class OutputPortProvisionService implements ProvisionService {
    @Override
    public Either<FailedOperation, ProvisionInfo> provision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {
        // TODO Remember to remove the super call and implement the provision for the output port.
        return ProvisionService.super.provision(operationRequest);
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> unprovision(
            ProvisionOperationRequest<?, ? extends Specific> operationRequest) {
        // TODO Remember to remove the super call and implement the unprovision for the output port.
        return ProvisionService.super.unprovision(operationRequest);
    }

    @Override
    public Either<FailedOperation, ProvisionInfo> updateAcl(
            AccessControlOperationRequest<?, ? extends Specific> operationRequest) {
        // TODO Remember to remove the super call and implement the update acl for the output port.
        return ProvisionService.super.updateAcl(operationRequest);
    }

    @Override
    public Either<FailedOperation, ReverseProvisionInfo> reverseProvision(
            ReverseProvisionOperationRequest<? extends Specific> operationRequest) {
        // TODO Remember to remove the super call and implement the reverse provision for the output port.
        //  If your tech adapter doesn't support reverse provision, simply remove this override
        return ProvisionService.super.reverseProvision(operationRequest);
    }
}
