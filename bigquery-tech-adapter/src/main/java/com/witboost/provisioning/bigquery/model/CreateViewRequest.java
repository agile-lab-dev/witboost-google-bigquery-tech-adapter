package com.witboost.provisioning.bigquery.model;

import com.witboost.provisioning.model.Column;
import java.util.List;

public record CreateViewRequest(
        String project, String dataset, String table, String view, String description, List<Column> schema) {}
