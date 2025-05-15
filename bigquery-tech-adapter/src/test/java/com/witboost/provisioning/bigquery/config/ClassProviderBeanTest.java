package com.witboost.provisioning.bigquery.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.bigquery.model.BigQueryOutputPortSpecific;
import com.witboost.provisioning.bigquery.model.BigQueryStorageSpecific;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.StorageArea;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

class ClassProviderBeanTest {

    ClassProviderBean classProviderBean = new ClassProviderBean();

    private final String useCaseTemplateIdOutputPort = "urn:dmb:utm:google-bigquery-outputport-template:0.0.0";
    private final String useCaseTemplateIdStorage = "urn:dmb:utm:google-bigquery-storage-template:0.0.0";

    @Test
    void defaultSpecificProvider() {
        var specificProvider = classProviderBean.specificClassProvider();

        assertEquals(Option.of(BigQueryOutputPortSpecific.class), specificProvider.get(useCaseTemplateIdOutputPort));
        assertEquals(Option.of(BigQueryStorageSpecific.class), specificProvider.get(useCaseTemplateIdStorage));
        assertEquals(Option.none(), specificProvider.getReverseProvisioningParams(useCaseTemplateIdOutputPort));
    }

    @Test
    void defaultComponentProvider() {
        var componentProvider = classProviderBean.componentClassProvider();

        assertEquals(Option.of(StorageArea.class), componentProvider.get(useCaseTemplateIdStorage));
        assertEquals(Option.of(OutputPort.class), componentProvider.get(useCaseTemplateIdOutputPort));
    }
}
