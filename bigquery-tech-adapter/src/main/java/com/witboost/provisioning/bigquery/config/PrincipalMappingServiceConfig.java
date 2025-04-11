package com.witboost.provisioning.bigquery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "principal-mapping-service")
public record PrincipalMappingServiceConfig(String groupMailDomain) {}
