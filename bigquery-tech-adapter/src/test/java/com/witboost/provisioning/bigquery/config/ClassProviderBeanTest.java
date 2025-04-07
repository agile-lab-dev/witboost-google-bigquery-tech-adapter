package com.witboost.provisioning.bigquery.config;

import static org.junit.jupiter.api.Assertions.*;

import com.witboost.provisioning.bigquery.model.BigQueryOutputPortSpecific;
import com.witboost.provisioning.model.OutputPort;
import io.vavr.control.Option;
import org.junit.jupiter.api.Test;

class ClassProviderBeanTest {

    ClassProviderBean classProviderBean = new ClassProviderBean();

    private final String useCaseTemplateId = "urn:dmb:utm:google-bigquery-outputport-template:0.0.0";

    @Test
    void defaultSpecificProvider() {
        var specificProvider = classProviderBean.specificClassProvider();

        assertEquals(Option.of(BigQueryOutputPortSpecific.class), specificProvider.get(useCaseTemplateId));
        assertEquals(Option.none(), specificProvider.getReverseProvisioningParams(useCaseTemplateId));
    }

    @Test
    void defaultComponentProvider() {
        var componentProvider = classProviderBean.componentClassProvider();

        assertEquals(Option.of(OutputPort.class), componentProvider.get(useCaseTemplateId));
    }
}
