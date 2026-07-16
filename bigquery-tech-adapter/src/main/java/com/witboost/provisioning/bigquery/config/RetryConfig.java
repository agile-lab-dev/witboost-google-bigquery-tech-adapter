package com.witboost.provisioning.bigquery.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "retry")
public record RetryConfig(
        @DefaultValue("500") long initialBackoffMs,
        @DefaultValue("8000") long maxBackoffMs,
        @DefaultValue("120000") long maxTotalWaitMs) {}
