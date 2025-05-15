package com.witboost.provisioning.bigquery.config;

import com.google.cloud.resourcemanager.v3.ProjectsClient;
import com.google.cloud.resourcemanager.v3.ProjectsSettings;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ResourceManagerBean {

    @Bean
    public ProjectsClient projectsClient() throws IOException {
        return ProjectsClient.create(ProjectsSettings.newBuilder().build());
    }
}
