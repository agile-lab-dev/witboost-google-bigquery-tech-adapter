package com.witboost.provisioning.bigquery.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.bigquery.service.provision.OutputPortProvisionService;
import com.witboost.provisioning.bigquery.service.provision.StorageAreaProvisionService;
import com.witboost.provisioning.bigquery.service.provision.WorkloadProvisionService;
import org.junit.jupiter.api.Test;

class ProvisionConfigurationBeanTest {

    @Test
    void beanCreation() {
        var outputPort = new OutputPortProvisionService();
        var storageArea = new StorageAreaProvisionService();
        var workload = new WorkloadProvisionService();
        var bean = new ProvisionConfigurationBean().provisionConfiguration(outputPort, storageArea, workload);

        assertEquals(outputPort, bean.getOutputPortProvisionService());
        assertEquals(storageArea, bean.getStorageProvisionService());
        assertEquals(workload, bean.getWorkloadProvisionService());
    }
}
