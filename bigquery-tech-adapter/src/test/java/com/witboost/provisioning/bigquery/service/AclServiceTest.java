package com.witboost.provisioning.bigquery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.bigquery.*;
import com.witboost.provisioning.framework.common.ErrorConstants;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AclServiceTest {

    @Mock
    private BigQuery bigQueryClient;

    @InjectMocks
    private AclService aclService;

    private final BigQueryException ex = new BigQueryException(401, "Unauthorized");
    private final String project = "project";
    private final String dataset = "dataset";
    private final String view = "view";
    private final List<String> roles = List.of("roles/bigquery.dataOwner");
    private final List<Identity> principals = List.of(Identity.user("name.surname@example.com"));
    private final TableId viewId = TableId.of(project, dataset, view);
    private final Policy emptyPolicy = Policy.newBuilder().build();
    private final String expectedSolution = ErrorConstants.PLATFORM_TEAM_SOLUTION;

    @Test
    public void testApplyAclsOk() {
        when(bigQueryClient.getIamPolicy(any())).thenReturn(emptyPolicy);
        when(bigQueryClient.setIamPolicy(any(), any())).thenReturn(emptyPolicy);

        var actualRes = aclService.applyAcls(roles, principals, viewId);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testApplyAclsError() {
        when(bigQueryClient.getIamPolicy(any())).thenReturn(emptyPolicy);
        when(bigQueryClient.setIamPolicy(any(), any())).thenThrow(ex);
        String expectedDesc = "Failed to setup acls for view 'project.dataset.view': Unauthorized";

        var actualRes = aclService.applyAcls(roles, principals, viewId);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
            assertEquals(1, p.solutions().size());
            p.solutions().forEach(s -> assertEquals(expectedSolution, s));
        });
    }

    @Test
    public void testRevokeRolesOk() {
        when(bigQueryClient.getIamPolicy(any())).thenReturn(emptyPolicy);
        when(bigQueryClient.setIamPolicy(any(), any())).thenReturn(emptyPolicy);

        var actualRes = aclService.revokeRoles(roles, viewId);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testRevokeRolesViewNotExisting() {
        when(bigQueryClient.getIamPolicy(any())).thenThrow(new BigQueryException(404, "Not found"));

        var actualRes = aclService.revokeRoles(roles, viewId);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testRevokeRolesError() {
        when(bigQueryClient.getIamPolicy(any())).thenReturn(emptyPolicy);
        when(bigQueryClient.setIamPolicy(any(), any())).thenThrow(ex);
        String expectedDesc = "Failed to revoke roles for view 'project.dataset.view': Unauthorized";

        var actualRes = aclService.revokeRoles(roles, viewId);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
            assertEquals(1, p.solutions().size());
            p.solutions().forEach(s -> assertEquals(expectedSolution, s));
        });
    }
}
