package com.witboost.provisioning.bigquery.config;

import com.witboost.provisioning.framework.service.ComponentClassProvider;
import com.witboost.provisioning.framework.service.SpecificClassProvider;
import com.witboost.provisioning.framework.service.impl.ComponentClassProviderImpl;
import com.witboost.provisioning.framework.service.impl.SpecificClassProviderImpl;
import com.witboost.provisioning.model.Specific;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * TODO Configuration bean to provide the Tech Adapter Framework ClassProviders
 *  Modify them to include your supported components and specific class.
 */
@Configuration
public class ClassProviderBean {

    @Bean
    public SpecificClassProvider specificClassProvider() {
        // TODO Include the mapping between your component useCaseTemplateIds and the
        //  Java classes modelling your specific sections. You may use the default implementation
        //  as shown here, or define your own SpecificClassProvider
        return SpecificClassProviderImpl.builder()
                .withDefaultSpecificClass(Specific.class)
                .withDefaultReverseProvisionSpecificClass(Specific.class)
                .build();
    }

    @Bean
    public ComponentClassProvider componentClassProvider() {
        /* TODO Include the mapping between your component useCaseTemplateIds and the
         *  Java classes modelling your components.
         *  You may use the default implementation as shown here, which uses the default StorageArea,
         *  Workload and OutputPort classes. Provide the useCaseTemplateId for each component,
         *  or leave null for the unsupported ones.
         *  As an alternative, you can define your own ComponentClassProvider
         */
        return ComponentClassProviderImpl.defaultComponentsImpl(null, null, null);
    }
}
