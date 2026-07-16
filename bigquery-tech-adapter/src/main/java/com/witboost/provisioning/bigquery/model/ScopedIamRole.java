package com.witboost.provisioning.bigquery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ScopedIamRole(@NotBlank String role, @NotNull IamScope scope) {}
