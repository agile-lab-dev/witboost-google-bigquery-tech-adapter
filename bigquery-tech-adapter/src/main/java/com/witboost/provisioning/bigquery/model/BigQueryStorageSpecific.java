package com.witboost.provisioning.bigquery.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.witboost.provisioning.model.Column;
import com.witboost.provisioning.model.Specific;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BigQueryStorageSpecific extends Specific {

    @NotBlank
    private String project;

    @NotBlank
    private String dataset;

    @NotBlank
    private String tableName;

    @NotNull
    private List<Column> schema;

    @NotNull
    @Valid
    private Iam iam;
}
