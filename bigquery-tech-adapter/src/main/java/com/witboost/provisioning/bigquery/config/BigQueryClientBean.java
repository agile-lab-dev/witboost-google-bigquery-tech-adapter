package com.witboost.provisioning.bigquery.config;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BigQueryClientBean {

    @Bean
    public BigQuery bigQueryClient() {
        return BigQueryOptions.getDefaultInstance().getService();
    }
}
