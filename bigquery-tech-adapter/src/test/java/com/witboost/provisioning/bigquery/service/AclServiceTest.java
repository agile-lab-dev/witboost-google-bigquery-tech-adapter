package com.witboost.provisioning.bigquery.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.bigquery.*;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import com.google.iam.v1.Binding;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.SetIamPolicyRequest;
import com.witboost.provisioning.bigquery.util.RetryHelper;
import com.witboost.provisioning.framework.common.ErrorConstants;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AclServiceTest {

    @Mock
    private BigQuery bigQueryClient;

    @Mock
    private Function<String, BigQuery> bigQueryClientSupplier;

    @Mock
    private RetryHelper retryHelper;

    @Mock
    private ProjectsClient projectsClient;

    @InjectMocks
    private AclService aclService;

    private final BigQueryException ex = new BigQueryException(401, "Unauthorized");
    private final String project = "project";
    private final String dataset = "dataset";
    private final String tableOrView = "tableOrView";
    private final List<String> roles = List.of("roles/bigquery.dataOwner");
    private final List<Identity> principals = List.of(Identity.user("name.surname@example.com"));
    private final TableId tableOrViewId = TableId.of(project, dataset, tableOrView);
    private final Policy emptyPolicy = Policy.newBuilder().build();
    private final String expectedSolution = ErrorConstants.PLATFORM_TEAM_SOLUTION;
    private final List<String> members = List.of("user:name.surname@example.com");

    @Test
    public void testApplyAclsOk() {
        when(bigQueryClientSupplier.apply(any())).thenReturn(bigQueryClient);
        when(bigQueryClient.getIamPolicy(any())).thenReturn(emptyPolicy);
        when(bigQueryClient.setIamPolicy(any(), any())).thenReturn(emptyPolicy);

        var actualRes = aclService.applyAcls(roles, principals, tableOrViewId);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testApplyAclsError() {
        when(bigQueryClientSupplier.apply(any())).thenReturn(bigQueryClient);
        when(bigQueryClient.getIamPolicy(any())).thenReturn(emptyPolicy);
        when(bigQueryClient.setIamPolicy(any(), any())).thenThrow(ex);
        String expectedDesc = "Failed to setup acls for table/view 'project.dataset.tableOrView': Unauthorized";

        var actualRes = aclService.applyAcls(roles, principals, tableOrViewId);

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
        when(bigQueryClientSupplier.apply(any())).thenReturn(bigQueryClient);
        when(bigQueryClient.getIamPolicy(any())).thenReturn(emptyPolicy);
        when(bigQueryClient.setIamPolicy(any(), any())).thenReturn(emptyPolicy);

        var actualRes = aclService.revokeRoles(roles, tableOrViewId);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testRevokeRolesViewNotExisting() {
        when(bigQueryClientSupplier.apply(any())).thenReturn(bigQueryClient);
        when(bigQueryClient.getIamPolicy(any())).thenThrow(new BigQueryException(404, "Not found"));

        var actualRes = aclService.revokeRoles(roles, tableOrViewId);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testRevokeRolesError() {
        when(bigQueryClientSupplier.apply(any())).thenReturn(bigQueryClient);
        when(bigQueryClient.getIamPolicy(any())).thenReturn(emptyPolicy);
        when(bigQueryClient.setIamPolicy(any(), any())).thenThrow(ex);
        String expectedDesc = "Failed to revoke roles for table/view 'project.dataset.tableOrView': Unauthorized";

        var actualRes = aclService.revokeRoles(roles, tableOrViewId);

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
    public void testApplyProjectAclsOk() throws Exception {
        com.google.iam.v1.Policy emptyIamPolicy =
                com.google.iam.v1.Policy.newBuilder().build();
        when(projectsClient.getIamPolicy(any(GetIamPolicyRequest.class))).thenReturn(emptyIamPolicy);
        doAnswer(invocation -> {
                    var callable = invocation.getArgument(0, java.util.concurrent.Callable.class);
                    return callable.call();
                })
                .when(retryHelper)
                .retryOnAbortedException(any());

        var actualRes = aclService.applyProjectAcls(project, roles, members);

        assertTrue(actualRes.isRight());
        verify(projectsClient).setIamPolicy(any(SetIamPolicyRequest.class));
    }

    @Test
    public void testApplyProjectAclsMergesExistingBindings() throws Exception {
        String role = "roles/bigquery.dataOwner";
        String existingMember = "user:existing@example.com";
        com.google.iam.v1.Policy existingPolicy = com.google.iam.v1.Policy.newBuilder()
                .addBindings(Binding.newBuilder()
                        .setRole(role)
                        .addMembers(existingMember)
                        .build())
                .build();
        when(projectsClient.getIamPolicy(any(GetIamPolicyRequest.class))).thenReturn(existingPolicy);
        doAnswer(invocation -> {
                    var callable = invocation.getArgument(0, java.util.concurrent.Callable.class);
                    return callable.call();
                })
                .when(retryHelper)
                .retryOnAbortedException(any());

        var actualRes = aclService.applyProjectAcls(project, roles, members);

        assertTrue(actualRes.isRight());
        var captor = org.mockito.ArgumentCaptor.forClass(SetIamPolicyRequest.class);
        verify(projectsClient).setIamPolicy(captor.capture());
        var setPolicy = captor.getValue().getPolicy();
        var bindingForRole = setPolicy.getBindingsList().stream()
                .filter(b -> b.getRole().equals(role))
                .findFirst()
                .orElseThrow();
        assertTrue(bindingForRole.getMembersList().contains(existingMember));
        assertTrue(bindingForRole.getMembersList().contains(members.get(0)));
    }

    @Test
    public void testApplyProjectAclsError() throws Exception {
        doAnswer(invocation -> {
                    var callable = invocation.getArgument(0, java.util.concurrent.Callable.class);
                    return callable.call();
                })
                .when(retryHelper)
                .retryOnAbortedException(any());
        when(projectsClient.getIamPolicy(any(GetIamPolicyRequest.class)))
                .thenThrow(new RuntimeException("Permission denied"));
        String expectedDesc = "Failed to setup project IAM roles for 'project': Permission denied";

        var actualRes = aclService.applyProjectAcls(project, roles, members);

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
