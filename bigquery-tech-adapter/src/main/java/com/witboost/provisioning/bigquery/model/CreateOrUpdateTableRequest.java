package com.witboost.provisioning.bigquery.model;

import com.witboost.provisioning.model.Column;
import java.util.List;

public record CreateOrUpdateTableRequest(
        String projectId, String datasetName, String tableName, List<Column> columns) {}
