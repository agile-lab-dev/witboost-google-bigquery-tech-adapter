package com.witboost.provisioning.bigquery.service.provision;

import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.google.cloud.Identity;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.witboost.provisioning.bigquery.model.BigQueryOutputPortSpecific;
import com.witboost.provisioning.bigquery.service.AclService;
import com.witboost.provisioning.bigquery.service.BigQueryService;
import com.witboost.provisioning.bigquery.service.PrincipalMappingService;
import com.witboost.provisioning.bigquery.util.ResourceUtils;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.request.AccessControlOperationRequest;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.parser.Parser;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutputPortProvisionServiceTest {

    @Mock
    private BigQueryService bigQueryService;

    @Mock
    private PrincipalMappingService principalMappingService;

    @Mock
    private AclService aclService;

    @InjectMocks
    private OutputPortProvisionService provisionService;

    private final Table mockedView = mock(Table.class);
    private final TableId viewId = TableId.of("project1", "dataset1", "viewName1");

    @Test
    void provisionOk() throws IOException {
        when(mockedView.getTableId()).thenReturn(viewId);
        when(bigQueryService.createOrUpdateView(any())).thenReturn(right(mockedView));
        when(principalMappingService.map(Set.of("user:name.surname_email.com", "group:dev")))
                .thenReturn(Map.of(
                        "user:name.surname_email.com",
                        right(Identity.user("name.username@email.com")),
                        "group:dev",
                        right(Identity.group("dev@email.com"))));
        when(aclService.applyAcls(anyList(), anyList(), any())).thenReturn(right(null));

        var actualRes = provisionService.provision(getProvisionOperationRequest(false));

        assertTrue(actualRes.isRight());
    }

    @Test
    void unprovisionNoRemoveData() throws IOException {
        when(aclService.revokeRoles(anyList(), any())).thenReturn(right(null));

        var actualRes = provisionService.unprovision(getProvisionOperationRequest(false));

        assertTrue(actualRes.isRight());
        verifyNoInteractions(bigQueryService, principalMappingService);
    }

    @Test
    void unprovisionWithRemoveData() throws IOException {
        when(aclService.revokeRoles(anyList(), any())).thenReturn(right(null));
        when(bigQueryService.deleteView(anyString(), anyString(), anyString())).thenReturn(right(null));

        var actualRes = provisionService.unprovision(getProvisionOperationRequest(true));

        assertTrue(actualRes.isRight());
        verifyNoInteractions(principalMappingService);
    }

    @Test
    void updateAclOk() throws IOException {
        when(aclService.revokeRoles(anyList(), any())).thenReturn(right(null));
        when(principalMappingService.map(Set.of("user:user1_email.com", "user:user2_email.com")))
                .thenReturn(Map.of(
                        "user:user1_email.com",
                        right(Identity.user("user1@email.com")),
                        "user:user2_email.com",
                        right(Identity.user("user2@email.com"))));
        when(aclService.applyAcls(anyList(), anyList(), any())).thenReturn(right(null));
        var users = Set.of("user:user1_email.com", "user:user2_email.com");
        var provisionOperationRequest = getProvisionOperationRequest(false);
        var updateAclRequest = new AccessControlOperationRequest<>(
                provisionOperationRequest.getDataProduct(), provisionOperationRequest.getComponent(), users);

        var actualRes = provisionService.updateAcl(updateAclRequest);

        assertTrue(actualRes.isRight());
    }

    private ProvisionOperationRequest<Specific, BigQueryOutputPortSpecific> getProvisionOperationRequest(
            boolean removeData) throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/pr_descriptor_bigquery_op.yml");
        var componentDescriptor =
                Parser.parseComponentDescriptor(ymlDescriptor, Specific.class).get();
        var component = componentDescriptor
                .getDataProduct()
                .getComponentToProvision(componentDescriptor.getComponentIdToProvision())
                .get();
        var op = Parser.parseComponent(component, OutputPort.class, BigQueryOutputPortSpecific.class)
                .get();
        return new ProvisionOperationRequest<>(componentDescriptor.getDataProduct(), op, removeData, Optional.empty());
    }
}
