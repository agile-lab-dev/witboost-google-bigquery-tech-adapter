package com.witboost.provisioning.bigquery.service;

import static io.vavr.control.Either.left;
import static io.vavr.control.Either.right;

import com.google.cloud.bigquery.*;
import com.witboost.provisioning.bigquery.model.CreateViewRequest;
import com.witboost.provisioning.framework.common.ErrorConstants;
import com.witboost.provisioning.model.Column;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BigQueryService {

    private static final Logger logger = LoggerFactory.getLogger(BigQueryService.class);

    private final BigQuery bigQueryClient;

    public BigQueryService(BigQuery bigQueryClient) {
        this.bigQueryClient = bigQueryClient;
    }

    public Either<FailedOperation, Option<Table>> getTable(String project, String dataset, String table) {
        try {
            TableId tableId = TableId.of(project, dataset, table);
            logger.info("Checking existence of table {}", tableId);
            var tableObj = bigQueryClient.getTable(tableId);
            logger.info("Table {} exists: {}", tableId, tableObj != null);
            return right(Option.of(tableObj));
        } catch (Exception e) {
            String userMessage = "An unexpected error occurred";
            String error = String.format(
                    "Failed to check for existence of table '%s.%s.%s': %s", project, dataset, table, e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(
                    userMessage,
                    List.of(new Problem(error, Optional.empty(), Set.of(ErrorConstants.PLATFORM_TEAM_SOLUTION)))));
        }
    }

    public Either<FailedOperation, Boolean> isViewSchemaCompatibleWithSourceTableSchema(
            Schema sourceTableSchema, List<Column> viewColumns) {
        try {
            // we only check for column names
            // if the dataType is not compatible it will break on view creation
            // NTH: make the check here
            logger.info(
                    "Checking compatibility between table schema {} and view columns {}",
                    sourceTableSchema,
                    viewColumns);
            var viewColumnNamesSet = viewColumns.stream().map(Column::getName).collect(Collectors.toSet());
            var tableColumnNamesSet =
                    sourceTableSchema.getFields().stream().map(Field::getName).collect(Collectors.toSet());
            return right(tableColumnNamesSet.containsAll(viewColumnNamesSet));
        } catch (Exception e) {
            String userMessage = "An unexpected error occurred";
            String error = String.format("Failed to check for the compatibility of the schema: %s", e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(
                    userMessage,
                    List.of(new Problem(error, Optional.empty(), Set.of(ErrorConstants.PLATFORM_TEAM_SOLUTION)))));
        }
    }

    public Either<FailedOperation, Table> createOrUpdateView(CreateViewRequest createViewRequest) {
        try {
            TableId viewId =
                    TableId.of(createViewRequest.project(), createViewRequest.dataset(), createViewRequest.view());
            logger.info("Creating or updating view {}", viewId);
            String viewSchemaSql = generateViewSchemaSqlStatement(createViewRequest.schema());
            String query = String.format(
                    "SELECT %s FROM %s.%s.%s",
                    viewSchemaSql, createViewRequest.project(), createViewRequest.dataset(), createViewRequest.table());
            var existingView = bigQueryClient.getTable(viewId);
            Table createdOrUpdatedView;
            if (existingView == null) {
                ViewDefinition viewDefinition = ViewDefinition.of(query);
                bigQueryClient.create(TableInfo.newBuilder(viewId, viewDefinition)
                        .setDescription(createViewRequest.description())
                        .build());
            }
            // we need to update the view after creation in order to set column descriptions
            // else the API returns an error when creating it: "BigQueryException: Schema field shouldn't be used as
            // input with a view"
            createdOrUpdatedView = updateView(createViewRequest, query, viewId);
            return right(createdOrUpdatedView);
        } catch (Exception e) {
            String userMessage = "An unexpected error occurred";
            String error = String.format(
                    "Failed to create view '%s.%s.%s': %s",
                    createViewRequest.project(), createViewRequest.dataset(), createViewRequest.view(), e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(
                    userMessage,
                    List.of(new Problem(error, Optional.empty(), Set.of(ErrorConstants.PLATFORM_TEAM_SOLUTION)))));
        }
    }

    public Either<FailedOperation, Void> deleteView(String project, String dataset, String view) {
        try {
            TableId viewId = TableId.of(project, dataset, view);
            logger.info("Deleting view {}", viewId);
            bigQueryClient.delete(viewId);
            return right(null);
        } catch (Exception e) {
            String userMessage = "An unexpected error occurred";
            String error =
                    String.format("Failed to delete view '%s.%s.%s': %s", project, dataset, view, e.getMessage());
            logger.error(error, e);
            return left(new FailedOperation(
                    userMessage,
                    List.of(new Problem(error, Optional.empty(), Set.of(ErrorConstants.PLATFORM_TEAM_SOLUTION)))));
        }
    }

    private Table updateView(CreateViewRequest createViewRequest, String query, TableId viewId) {
        ViewDefinition viewDefinition = ViewDefinition.newBuilder(query)
                .setSchema(generateSchema(createViewRequest.schema()))
                .setUseLegacySql(false)
                .build();
        return bigQueryClient.update(TableInfo.newBuilder(viewId, viewDefinition)
                .setDescription(createViewRequest.description())
                .build());
    }

    private String generateViewSchemaSqlStatement(List<Column> schema) {
        // if no schema is defined on view, we use the whole source table schema
        if (schema.isEmpty()) return "*";
        return schema.stream().map(Column::getName).collect(Collectors.joining(", "));
    }

    private Schema generateSchema(List<Column> schema) {
        var fields = schema.stream()
                .map(c -> Field.newBuilder(c.getName(), StandardSQLTypeName.valueOf(c.getDataType()))
                        .setDescription(c.getDescription())
                        .setMaxLength(c.getDataLength().map(Long::valueOf).orElse(null))
                        .setScale(c.getScale().map(Long::valueOf).orElse(null))
                        .setPrecision(c.getPrecision().map(Long::valueOf).orElse(null))
                        .setMode(c.getConstraint().map(Field.Mode::valueOf).orElse(null))
                        .build())
                .toList();
        return Schema.of(fields);
    }
}
