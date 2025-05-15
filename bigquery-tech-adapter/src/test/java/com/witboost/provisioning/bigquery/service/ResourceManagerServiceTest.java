package com.witboost.provisioning.bigquery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.api.gax.grpc.GrpcStatusCode;
import com.google.api.gax.rpc.PermissionDeniedException;
import com.google.cloud.resourcemanager.v3.Project;
import com.google.cloud.resourcemanager.v3.ProjectName;
import com.google.cloud.resourcemanager.v3.ProjectsClient;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResourceManagerServiceTest {

    @Mock
    private ProjectsClient projectsClient;

    @InjectMocks
    private ResourceManagerService resourceManagerService;

    @Test
    public void testIsProjectExisting_Exists() {
        Project mockedProject = mock(Project.class);
        when(projectsClient.getProject(any(ProjectName.class))).thenReturn(mockedProject);

        var actualRes = resourceManagerService.isProjectExisting("test-project");

        assertTrue(actualRes.isRight());
        assertTrue(actualRes.get());
    }

    @Test
    public void testIsProjectExisting_PermissionDenied() {
        when(projectsClient.getProject(any(ProjectName.class)))
                .thenThrow(new PermissionDeniedException(
                        "Access denied", new Exception(), GrpcStatusCode.of(Status.Code.PERMISSION_DENIED), false));

        var actualRes = resourceManagerService.isProjectExisting("test-project");

        assertTrue(actualRes.isRight());
        assertFalse(actualRes.get());
    }

    @Test
    public void testIsProjectExisting_Error() {
        when(projectsClient.getProject(any(ProjectName.class))).thenThrow(new RuntimeException("Some error"));

        var actualRes = resourceManagerService.isProjectExisting("test-project");

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
    }
}
