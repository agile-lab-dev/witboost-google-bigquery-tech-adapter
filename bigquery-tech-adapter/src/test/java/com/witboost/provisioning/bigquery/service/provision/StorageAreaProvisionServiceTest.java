package com.witboost.provisioning.bigquery.service.provision;

import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.google.cloud.Identity;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableId;
import com.witboost.provisioning.bigquery.model.BigQueryStorageSpecific;
import com.witboost.provisioning.bigquery.service.AclService;
import com.witboost.provisioning.bigquery.service.BigQueryService;
import com.witboost.provisioning.bigquery.service.PrincipalMappingService;
import com.witboost.provisioning.bigquery.util.ResourceUtils;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.StorageArea;
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
public class StorageAreaProvisionServiceTest {

    @Mock
    private BigQueryService bigQueryService;

    @Mock
    private PrincipalMappingService principalMappingService;

    @Mock
    private AclService aclService;

    @InjectMocks
    private StorageAreaProvisionService provisionService;

    private final Table mockedTable = mock(Table.class);
    private final TableId tableId = TableId.of("project1", "dataset1", "tableName1");

    @Test
    void provisionOk() throws IOException {
        when(mockedTable.getTableId()).thenReturn(tableId);
        when(bigQueryService.createDatasetIfNotExists(any()))
                .thenReturn(
                        right(mock(mock(com.google.cloud.bigquery.Dataset.class).getClass())));
        when(bigQueryService.createOrUpdateTable(any())).thenReturn(right(mockedTable));
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
        when(bigQueryService.deleteTable(any())).thenReturn(right(null));

        var actualRes = provisionService.unprovision(getProvisionOperationRequest(true));

        assertTrue(actualRes.isRight());
        verifyNoInteractions(principalMappingService);
    }

    private ProvisionOperationRequest<Specific, BigQueryStorageSpecific> getProvisionOperationRequest(
            boolean removeData) throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/pr_descriptor_bigquery_st.yml");
        var componentDescriptor =
                Parser.parseComponentDescriptor(ymlDescriptor, Specific.class).get();
        var component = componentDescriptor
                .getDataProduct()
                .getComponentToProvision(componentDescriptor.getComponentIdToProvision())
                .get();
        var st = Parser.parseComponent(component, StorageArea.class, BigQueryStorageSpecific.class)
                .get();
        return new ProvisionOperationRequest<>(componentDescriptor.getDataProduct(), st, removeData, Optional.empty());
    }
}
