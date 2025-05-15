package com.witboost.provisioning.bigquery.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.bigquery.service.validation.OutputPortValidationService;
import com.witboost.provisioning.bigquery.service.validation.StorageValidationService;
import org.junit.jupiter.api.Test;

class ValidationConfigurationBeanTest {

    @Test
    void beanCreation() {
        var outputPort = new OutputPortValidationService(null);
        var storage = new StorageValidationService(null, null);
        var bean = new ValidationConfigurationBean().validationConfiguration(outputPort, storage);

        assertEquals(outputPort, bean.getOutputPortValidationService());
        assertEquals(storage, bean.getStorageValidationService());
    }
}
