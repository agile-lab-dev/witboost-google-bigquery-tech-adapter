package com.witboost.provisioning.bigquery.config;

import com.witboost.provisioning.bigquery.model.BigQueryOutputPortSpecific;
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
                .withDefaultSpecificClass(BigQueryOutputPortSpecific.class)
                .build();
    }

    @Bean
    public ComponentClassProvider componentClassProvider() {
        return ComponentClassProviderImpl.defaultComponentsImpl(
                null, null, "urn:dmb:utm:google-bigquery-outputport-template:0.0.0");
    }
}
