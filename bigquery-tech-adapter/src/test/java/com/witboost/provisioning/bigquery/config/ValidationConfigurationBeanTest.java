package com.witboost.provisioning.bigquery.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.bigquery.service.validation.OutputPortValidationService;
import org.junit.jupiter.api.Test;

class ValidationConfigurationBeanTest {

    @Test
    void beanCreation() {
        var outputPort = new OutputPortValidationService(null);
        var bean = new ValidationConfigurationBean().validationConfiguration(outputPort);

        assertEquals(outputPort, bean.getOutputPortValidationService());
    }
}
