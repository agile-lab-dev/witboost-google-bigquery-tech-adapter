package com.witboost.provisioning.bigquery.config;

import com.witboost.provisioning.bigquery.service.provision.OutputPortProvisionService;
import com.witboost.provisioning.bigquery.service.provision.StorageAreaProvisionService;
import com.witboost.provisioning.bigquery.service.provision.WorkloadProvisionService;
import com.witboost.provisioning.framework.service.ProvisionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProvisionConfigurationBean {

    // TODO Remove the components you don't need to support on your Tech Adapter
    @Bean
    ProvisionConfiguration provisionConfiguration(
            OutputPortProvisionService outputPortProvisionService,
            StorageAreaProvisionService storageAreaProvisionService,
            WorkloadProvisionService workloadProvisionService) {
        return ProvisionConfiguration.builder()
                .outputPortProvisionService(outputPortProvisionService)
                .storageProvisionService(storageAreaProvisionService)
                .workloadProvisionService(workloadProvisionService)
                .build();
    }
}
