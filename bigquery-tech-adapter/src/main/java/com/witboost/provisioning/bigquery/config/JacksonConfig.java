package com.witboost.provisioning.bigquery.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.witboost.provisioning.model.Column;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    abstract static class ColumnMixin {}

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer columnMixinCustomizer() {
        return builder -> builder.mixIn(Column.class, ColumnMixin.class);
    }
}
