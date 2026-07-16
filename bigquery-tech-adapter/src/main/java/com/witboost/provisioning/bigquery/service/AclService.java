package com.witboost.provisioning.bigquery.service;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.resourcemanager.v3.ProjectName;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import com.google.iam.v1.Binding;
import com.google.iam.v1.GetIamPolicyRequest;
import com.google.iam.v1.SetIamPolicyRequest;
import com.witboost.provisioning.bigquery.util.RetryHelper;
import com.witboost.provisioning.framework.common.ErrorConstants;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.*;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AclService {
    private static final Logger logger = LoggerFactory.getLogger(AclService.class);

    private final Function<String, BigQuery> bigQueryClientSupplier;
    private final RetryHelper retryHelper;
    private final ProjectsClient projectsClient;

    public AclService(
            Function<String, BigQuery> bigQueryClientSupplier, RetryHelper retryHelper, ProjectsClient projectsClient) {
        this.bigQueryClientSupplier = bigQueryClientSupplier;
        this.retryHelper = retryHelper;
        this.projectsClient = projectsClient;
    }

    public Either<FailedOperation, Void> applyAcls(List<String> roles, List<Identity> principals, TableId tableOrView) {
        try {
            logger.info("Assigning roles {} to principals {} for table/view {}", roles, principals, tableOrView);
            var gcpRoles = roles.stream().map(Role::of).toList();
            BigQuery bigQueryClient = bigQueryClientSupplier.apply(tableOrView.getProject());
            Policy policy = bigQueryClient.getIamPolicy(tableOrView);
            var policyBuilder = policy.toBuilder();
            for (var gcpRole : gcpRoles) {
                for (var principal : principals) {
                    policyBuilder.addIdentity(gcpRole, principal);
                }
            }
            bigQueryClient.setIamPolicy(tableOrView, policyBuilder.build());
            return right(null);
        } catch (Exception e) {
            String userMessage = "An unexpected error occurred";
            String error = String.format(
                    "Failed to setup acls for table/view '%s.%s.%s': %s",
                    tableOrView.getProject(), tableOrView.getDataset(), tableOrView.getTable(), e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(
                    userMessage,
                    List.of(new Problem(error, Optional.empty(), Set.of(ErrorConstants.PLATFORM_TEAM_SOLUTION)))));
        }
    }

    public Either<FailedOperation, Void> revokeRoles(List<String> roles, TableId tableOrView) {
        try {
            logger.info("Revoking roles {} for table/view {}", roles, tableOrView);
            var gcpRoles = roles.stream().map(Role::of).toList();
            BigQuery bigQueryClient = bigQueryClientSupplier.apply(tableOrView.getProject());
            Policy policy = bigQueryClient.getIamPolicy(tableOrView);
            Map<Role, Set<Identity>> bindings = new HashMap<>(policy.getBindings());
            for (var gcpRole : gcpRoles) {
                bindings.remove(gcpRole);
            }
            policy = policy.toBuilder().setBindings(bindings).build();
            bigQueryClient.setIamPolicy(tableOrView, policy);
            return right(null);
        } catch (Exception e) {
            if (e instanceof BigQueryException bqe && bqe.getCode() == 404) {
                // view doesn't exist, no roles to revoke
                return right(null);
            }
            String userMessage = "An unexpected error occurred";
            String error = String.format(
                    "Failed to revoke roles for table/view '%s.%s.%s': %s",
                    tableOrView.getProject(), tableOrView.getDataset(), tableOrView.getTable(), e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(
                    userMessage,
                    List.of(new Problem(error, Optional.empty(), Set.of(ErrorConstants.PLATFORM_TEAM_SOLUTION)))));
        }
    }

    public Either<FailedOperation, Void> applyProjectAcls(String projectId, List<String> roles, List<String> members) {
        try {
            retryHelper.retryOnAbortedException(() -> {
                logger.info("Assigning roles {} to members {} for project {}", roles, members, projectId);
                String resource = ProjectName.of(projectId).toString();
                com.google.iam.v1.Policy policy = projectsClient.getIamPolicy(
                        GetIamPolicyRequest.newBuilder().setResource(resource).build());
                com.google.iam.v1.Policy.Builder policyBuilder = policy.toBuilder();

                for (String role : roles) {
                    Set<String> mergedMembers = new HashSet<>();
                    Binding existing = null;
                    for (Binding binding : policyBuilder.getBindingsList()) {
                        if (binding.getRole().equals(role)) {
                            existing = binding;
                            mergedMembers.addAll(binding.getMembersList());
                            break;
                        }
                    }

                    mergedMembers.addAll(members);
                    if (existing != null) {
                        policyBuilder.removeBindings(
                                policyBuilder.getBindingsList().indexOf(existing));
                    }
                    policyBuilder.addBindings(Binding.newBuilder()
                            .setRole(role)
                            .addAllMembers(mergedMembers)
                            .build());
                }

                projectsClient.setIamPolicy(SetIamPolicyRequest.newBuilder()
                        .setResource(resource)
                        .setPolicy(policyBuilder.build())
                        .build());
                return null;
            });
            return right(null);
        } catch (Exception e) {
            String userMessage = "An unexpected error occurred while assigning project IAM roles";
            String error = String.format("Failed to setup project IAM roles for '%s': %s", projectId, e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(
                    userMessage,
                    List.of(new Problem(error, Optional.empty(), Set.of(ErrorConstants.PLATFORM_TEAM_SOLUTION)))));
        }
    }
}
