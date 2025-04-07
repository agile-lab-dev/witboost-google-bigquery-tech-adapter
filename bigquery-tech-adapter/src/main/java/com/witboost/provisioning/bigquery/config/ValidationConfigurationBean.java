package com.witboost.provisioning.bigquery.config;

import com.witboost.provisioning.bigquery.service.validation.OutputPortValidationService;
import com.witboost.provisioning.framework.service.validation.ValidationConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidationConfigurationBean {

    @Bean
    ValidationConfiguration validationConfiguration(OutputPortValidationService outputPortValidationService) {
        return ValidationConfiguration.builder()
                .outputPortValidationService(outputPortValidationService)
                .build();
    }
}
