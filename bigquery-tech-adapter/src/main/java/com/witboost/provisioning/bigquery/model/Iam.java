package com.witboost.provisioning.bigquery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Iam(List<ScopedIamRole> developmentGroupRoles) {}
