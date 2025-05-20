package com.witboost.provisioning.bigquery.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.bigquery.service.provision.OutputPortProvisionService;
import com.witboost.provisioning.bigquery.service.provision.StorageAreaProvisionService;
import org.junit.jupiter.api.Test;

class ProvisionConfigurationBeanTest {

    @Test
    void beanCreation() {
        var outputPort = new OutputPortProvisionService(null, null, null);
        var storageArea = new StorageAreaProvisionService(null, null, null);
        var bean = new ProvisionConfigurationBean().provisionConfiguration(outputPort, storageArea);

        assertEquals(outputPort, bean.getOutputPortProvisionService());
        assertEquals(storageArea, bean.getStorageProvisionService());
    }
}
