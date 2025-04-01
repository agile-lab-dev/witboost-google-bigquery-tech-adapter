package com.witboost.provisioning.bigquery.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.bigquery.service.validation.OutputPortValidationService;
import com.witboost.provisioning.bigquery.service.validation.StorageAreaValidationService;
import com.witboost.provisioning.bigquery.service.validation.WorkloadValidationService;
import org.junit.jupiter.api.Test;

class ValidationConfigurationBeanTest {

    @Test
    void beanCreation() {
        var outputPort = new OutputPortValidationService();
        var storageArea = new StorageAreaValidationService();
        var workload = new WorkloadValidationService();
        var bean = new ValidationConfigurationBean().validationConfiguration(outputPort, storageArea, workload);

        assertEquals(outputPort, bean.getOutputPortValidationService());
        assertEquals(storageArea, bean.getStorageValidationService());
        assertEquals(workload, bean.getWorkloadValidationService());
    }
}
