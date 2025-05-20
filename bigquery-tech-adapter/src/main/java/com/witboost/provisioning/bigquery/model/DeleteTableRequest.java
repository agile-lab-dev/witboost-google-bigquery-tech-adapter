package com.witboost.provisioning.bigquery.model;

public record DeleteTableRequest(String projectId, String datasetName, String tableName) {}
