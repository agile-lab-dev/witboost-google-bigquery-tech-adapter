package com.witboost.provisioning.bigquery.service;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.TableId;
import com.witboost.provisioning.framework.common.ErrorConstants;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AclService {
    private static final Logger logger = LoggerFactory.getLogger(AclService.class);

    private final BigQuery bigQueryClient;

    public AclService(BigQuery bigQueryClient) {
        this.bigQueryClient = bigQueryClient;
    }

    public Either<FailedOperation, Void> applyAcls(List<String> roles, List<Identity> principals, TableId tableOrView) {
        try {
            logger.info("Assigning roles {} to principals {} for table/view {}", roles, principals, tableOrView);
            var gcpRoles = roles.stream().map(Role::of).toList();
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
}
