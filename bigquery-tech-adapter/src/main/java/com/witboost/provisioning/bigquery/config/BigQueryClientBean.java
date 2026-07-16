package com.witboost.provisioning.bigquery.config;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import java.util.function.Function;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BigQueryClientBean {

    @Bean
    public Function<String, BigQuery> bigQueryClientSupplier() {
        return projectId ->
                BigQueryOptions.newBuilder().setProjectId(projectId).build().getService();
    }
}
