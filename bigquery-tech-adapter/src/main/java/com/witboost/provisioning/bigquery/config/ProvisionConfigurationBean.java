package com.witboost.provisioning.bigquery.config;

import com.witboost.provisioning.bigquery.service.provision.OutputPortProvisionService;
import com.witboost.provisioning.bigquery.service.provision.StorageAreaProvisionService;
import com.witboost.provisioning.framework.service.ProvisionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProvisionConfigurationBean {

    @Bean
    ProvisionConfiguration provisionConfiguration(
            OutputPortProvisionService outputPortProvisionService,
            StorageAreaProvisionService storageAreaProvisionService) {
        return ProvisionConfiguration.builder()
                .storageProvisionService(storageAreaProvisionService)
                .outputPortProvisionService(outputPortProvisionService)
                .build();
    }
}
