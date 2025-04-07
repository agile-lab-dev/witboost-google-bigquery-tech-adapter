package com.witboost.provisioning.bigquery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.witboost.provisioning.model.Specific;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BigQueryOutputPortSpecific extends Specific {

    @NotBlank
    private String project;

    @NotBlank
    private String dataset;

    @NotBlank
    private String tableName;

    @NotBlank
    private String viewName;
}
