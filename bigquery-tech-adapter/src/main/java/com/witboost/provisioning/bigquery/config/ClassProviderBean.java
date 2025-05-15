package com.witboost.provisioning.bigquery.config;

import com.witboost.provisioning.bigquery.model.BigQueryOutputPortSpecific;
import com.witboost.provisioning.bigquery.model.BigQueryStorageSpecific;
import com.witboost.provisioning.framework.service.ComponentClassProvider;
import com.witboost.provisioning.framework.service.SpecificClassProvider;
import com.witboost.provisioning.framework.service.impl.ComponentClassProviderImpl;
import com.witboost.provisioning.framework.service.impl.SpecificClassProviderImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClassProviderBean {

    @Bean
    public SpecificClassProvider specificClassProvider() {
        return SpecificClassProviderImpl.builder()
                .withSpecificClass(
                        "urn:dmb:utm:google-bigquery-outputport-template:0.0.0", BigQueryOutputPortSpecific.class)
                .withSpecificClass("urn:dmb:utm:google-bigquery-storage-template:0.0.0", BigQueryStorageSpecific.class)
                .build();
    }

    @Bean
    public ComponentClassProvider componentClassProvider() {
        return ComponentClassProviderImpl.defaultComponentsImpl(
                "urn:dmb:utm:google-bigquery-storage-template:0.0.0",
                null,
                "urn:dmb:utm:google-bigquery-outputport-template:0.0.0");
    }
}
