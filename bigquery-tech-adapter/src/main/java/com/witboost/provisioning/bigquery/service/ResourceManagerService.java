package com.witboost.provisioning.bigquery.service;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.resourcemanager.v3.ProjectName;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import com.witboost.provisioning.framework.common.ErrorConstants;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ResourceManagerService {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManagerService.class);

    private final ProjectsClient projectsClient;

    public ResourceManagerService(ProjectsClient projectsClient) {
        this.projectsClient = projectsClient;
    }

    public Either<FailedOperation, Boolean> isProjectExisting(String projectId) {
        try {
            logger.info("Checking if project {} exists", projectId);
            projectsClient.getProject(ProjectName.of(projectId));
            return right(true);
        } catch (PermissionDeniedException e) {
            logger.error(String.format("Project %s does not exist", projectId), e);

            return right(false);
        } catch (Exception e) {
            String userMessage = "An unexpected error occurred while checking project existence";
            String error = String.format("Failed to check project existence for %s: %s", projectId, e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(
                    userMessage,
                    List.of(new Problem(error, Optional.empty(), Set.of(ErrorConstants.PLATFORM_TEAM_SOLUTION)))));
        }
    }
}
