package com.witboost.provisioning.bigquery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.google.cloud.bigquery.*;
import com.witboost.provisioning.bigquery.model.CreateDatasetRequest;
import com.witboost.provisioning.bigquery.model.CreateOrUpdateTableRequest;
import com.witboost.provisioning.bigquery.model.CreateViewRequest;
import com.witboost.provisioning.bigquery.model.DeleteTableRequest;
import com.witboost.provisioning.framework.common.ErrorConstants;
import com.witboost.provisioning.model.Column;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class BigQueryServiceTest {

    @Mock
    private BigQuery bigQueryClient;

    @InjectMocks
    private BigQueryService bigQueryService;

    private final BigQueryException ex = new BigQueryException(401, "Unauthorized");
    private final String project = "project";
    private final String dataset = "dataset";
    private final String table = "table";
    private final String view = "view";
    private final String viewDescription = "view description";
    private final List<Column> viewSchemaWithColumns = getViewSchemaWithColumns();

    @Test
    public void testGetTableExists() {
        final Table mockedTable = mock(Table.class);
        when(bigQueryClient.getTable(any(TableId.class))).thenReturn(mockedTable);

        var actualRes = bigQueryService.getTable(project, dataset, table);

        assertTrue(actualRes.isRight());
        assertTrue(actualRes.get().isDefined());
        assertEquals(mockedTable, actualRes.get().get());
    }

    @Test
    public void testGetTableNotExists() {
        when(bigQueryClient.getTable(any(TableId.class))).thenReturn(null);

        var actualRes = bigQueryService.getTable(project, dataset, table);

        assertTrue(actualRes.isRight());
        assertTrue(actualRes.get().isEmpty());
    }

    @Test
    public void testGetTableError() {
        when(bigQueryClient.getTable(any(TableId.class))).thenThrow(ex);
        String expectedDesc = "Failed to check for existence of table 'project.dataset.table': Unauthorized";
        String expectedSolution = ErrorConstants.PLATFORM_TEAM_SOLUTION;

        var actualRes = bigQueryService.getTable(project, dataset, table);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
            assertEquals(1, p.solutions().size());
            p.solutions().forEach(s -> assertEquals(expectedSolution, s));
        });
    }

    @Test
    public void testIsViewSchemaCompatibleWithSourceTableSchema_SchemasAreEquals() {
        Schema schema = Schema.of(
                Field.of("stringField", StandardSQLTypeName.STRING),
                Field.of("booleanField", StandardSQLTypeName.BOOL));
        var column1 = new Column();
        column1.setName("stringField");
        var column2 = new Column();
        column2.setName("booleanField");
        var viewColumnNames = List.of(column1, column2);

        var actualRes = bigQueryService.isViewSchemaCompatibleWithSourceTableSchema(schema, viewColumnNames);

        assertTrue(actualRes.isRight());
        assertTrue(actualRes.get());
    }

    @Test
    public void testIsViewSchemaCompatibleWithSourceTableSchema_SchemaIsSubset() {
        Schema schema = Schema.of(
                Field.of("stringField", StandardSQLTypeName.STRING),
                Field.of("booleanField", StandardSQLTypeName.BOOL));
        var column1 = new Column();
        column1.setName("stringField");
        var viewColumnNames = List.of(column1);

        var actualRes = bigQueryService.isViewSchemaCompatibleWithSourceTableSchema(schema, viewColumnNames);

        assertTrue(actualRes.isRight());
        assertTrue(actualRes.get());
    }

    @Test
    public void testIsViewSchemaCompatibleWithSourceTableSchema_SchemasAreIncompatible() {
        Schema schema = Schema.of(
                Field.of("stringField", StandardSQLTypeName.STRING),
                Field.of("booleanField", StandardSQLTypeName.BOOL));
        var column1 = new Column();
        column1.setName("stringField");
        var column2 = new Column();
        column2.setName("aNotExistingColumnInTable");
        var viewColumnNames = List.of(column1, column2);

        var actualRes = bigQueryService.isViewSchemaCompatibleWithSourceTableSchema(schema, viewColumnNames);

        assertTrue(actualRes.isRight());
        assertFalse(actualRes.get());
    }

    @Test
    public void testCreateOrUpdateViewNew() {
        final Table mockedTable = mock(Table.class);
        when(bigQueryClient.getTable(any(TableId.class))).thenReturn(null);
        when(bigQueryClient.create(any(TableInfo.class))).thenReturn(mockedTable);
        when(bigQueryClient.update(any(TableInfo.class))).thenReturn(mockedTable);
        CreateViewRequest createViewRequest =
                new CreateViewRequest(project, dataset, table, view, viewDescription, viewSchemaWithColumns);

        var actualRes = bigQueryService.createOrUpdateView(createViewRequest);

        assertTrue(actualRes.isRight());
        assertEquals(mockedTable, actualRes.get());
        verify(bigQueryClient, times(1)).getTable(any(TableId.class));
        verify(bigQueryClient, times(1)).create(any(TableInfo.class));
        verify(bigQueryClient, times(1)).update(any(TableInfo.class));
    }

    @Test
    public void testCreateOrUpdateViewExisting() {
        final Table mockedTable = mock(Table.class);
        when(bigQueryClient.getTable(any(TableId.class))).thenReturn(mockedTable);
        when(bigQueryClient.update(any(TableInfo.class))).thenReturn(mockedTable);
        CreateViewRequest createViewRequest =
                new CreateViewRequest(project, dataset, table, view, viewDescription, viewSchemaWithColumns);

        var actualRes = bigQueryService.createOrUpdateView(createViewRequest);

        assertTrue(actualRes.isRight());
        assertEquals(mockedTable, actualRes.get());
        verify(bigQueryClient, times(1)).getTable(any(TableId.class));
        verify(bigQueryClient, never()).create(any(TableInfo.class));
        verify(bigQueryClient, times(1)).update(any(TableInfo.class));
    }

    @Test
    public void testCreateOrUpdateViewError() {
        final Table mockedTable = mock(Table.class);
        when(bigQueryClient.getTable(any(TableId.class))).thenReturn(null);
        when(bigQueryClient.create(any(TableInfo.class))).thenReturn(mockedTable);
        when(bigQueryClient.update(any(TableInfo.class))).thenThrow(ex);
        CreateViewRequest createViewRequest =
                new CreateViewRequest(project, dataset, table, view, viewDescription, viewSchemaWithColumns);
        String expectedDesc = "Failed to create view 'project.dataset.view': Unauthorized";
        String expectedSolution = "Please try again. If the problem persists, contact the platform team.";

        var actualRes = bigQueryService.createOrUpdateView(createViewRequest);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
            assertEquals(1, p.solutions().size());
            p.solutions().forEach(s -> assertEquals(expectedSolution, s));
        });
    }

    @Test
    public void testDeleteViewOk() {
        when(bigQueryClient.delete(any(TableId.class))).thenReturn(true);

        var actualRes = bigQueryService.deleteView(project, dataset, view);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testDeleteViewError() {
        when(bigQueryClient.delete(any(TableId.class))).thenThrow(ex);
        String expectedDesc = "Failed to delete view 'project.dataset.view': Unauthorized";
        String expectedSolution = "Please try again. If the problem persists, contact the platform team.";

        var actualRes = bigQueryService.deleteView(project, dataset, view);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
            assertEquals(1, p.solutions().size());
            p.solutions().forEach(s -> assertEquals(expectedSolution, s));
        });
    }

    private List<Column> getViewSchemaWithColumns() {
        var column1 = new Column();
        column1.setName("day");
        column1.setDataType("DATE");
        column1.setDescription("day description");
        var column2 = new Column();
        column2.setName("top_term");
        column2.setDataType("STRING");
        column2.setDescription("top_term description");
        return List.of(column1, column2);
    }

    @Test
    public void testIsTableSchemaCompatibleWithColumns_AllPresent() {
        Schema schema =
                Schema.of(Field.of("col1", StandardSQLTypeName.STRING), Field.of("col2", StandardSQLTypeName.INT64));
        Column col1 = new Column();
        col1.setName("col1");
        Column col2 = new Column();
        col2.setName("col2");
        List<Column> requiredColumns = List.of(col1, col2);

        var actualRes = bigQueryService.isTableSchemaCompatibleWithColumns(schema, requiredColumns);

        assertTrue(actualRes.isRight());
        assertTrue(actualRes.get());
    }

    @Test
    public void testIsTableSchemaCompatibleWithColumns_MissingColumn() {
        Schema schema =
                Schema.of(Field.of("col1", StandardSQLTypeName.STRING), Field.of("col2", StandardSQLTypeName.INT64));
        Column col1 = new Column();
        col1.setName("col1");
        List<Column> requiredColumns = List.of(col1);

        var actualRes = bigQueryService.isTableSchemaCompatibleWithColumns(schema, requiredColumns);

        assertTrue(actualRes.isRight());
        assertFalse(actualRes.get());
    }

    @Test
    public void testIsTableSchemaCompatibleWithColumns_AllPresentAndAdded() {
        Schema schema =
                Schema.of(Field.of("col1", StandardSQLTypeName.STRING), Field.of("col2", StandardSQLTypeName.INT64));
        Column col1 = new Column();
        col1.setName("col1");
        Column col2 = new Column();
        col2.setName("col2");
        Column col3 = new Column();
        col3.setName("col3");
        List<Column> requiredColumns = List.of(col1, col2, col3);

        var actualRes = bigQueryService.isTableSchemaCompatibleWithColumns(schema, requiredColumns);

        assertTrue(actualRes.isRight());
        assertTrue(actualRes.get());
    }

    @Test
    public void testIsTableSchemaCompatibleWithColumns_Error() {
        var actualRes = bigQueryService.isTableSchemaCompatibleWithColumns(null, null);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
    }

    @Test
    public void testCreateDatasetIfNotExists_AlreadyExists() {
        final Dataset mockedDataset = mock(Dataset.class);
        when(bigQueryClient.getDataset(any(DatasetId.class))).thenReturn(mockedDataset);

        CreateDatasetRequest request = new CreateDatasetRequest(project, dataset);
        var actualRes = bigQueryService.createDatasetIfNotExists(request);

        assertTrue(actualRes.isRight());
        assertEquals(mockedDataset, actualRes.get());
    }

    @Test
    public void testCreateDatasetIfNotExists_CreatesDataset() {
        when(bigQueryClient.getDataset(any(DatasetId.class))).thenReturn(null);
        final Dataset createdDataset = mock(Dataset.class);
        when(bigQueryClient.create(any(DatasetInfo.class))).thenReturn(createdDataset);

        CreateDatasetRequest request = new CreateDatasetRequest(project, dataset);
        var actualRes = bigQueryService.createDatasetIfNotExists(request);

        assertTrue(actualRes.isRight());
        assertEquals(createdDataset, actualRes.get());
    }

    @Test
    public void testCreateDatasetIfNotExists_Error() {
        when(bigQueryClient.getDataset(any(DatasetId.class))).thenThrow(ex);

        CreateDatasetRequest request = new CreateDatasetRequest(project, dataset);
        String expectedDesc = "Failed to create dataset 'project.dataset': Unauthorized";

        var actualRes = bigQueryService.createDatasetIfNotExists(request);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        assertEquals(expectedDesc, actualRes.getLeft().problems().get(0).description());
    }

    @Test
    public void testCreateOrUpdateTable_NewTable() {
        when(bigQueryClient.getTable(any(TableId.class))).thenReturn(null);
        final Table mockedTable = mock(Table.class);
        when(bigQueryClient.create(any(TableInfo.class))).thenReturn(mockedTable);

        CreateOrUpdateTableRequest request = new CreateOrUpdateTableRequest(project, dataset, table, List.of());
        var actualRes = bigQueryService.createOrUpdateTable(request);

        assertTrue(actualRes.isRight());
        assertEquals(mockedTable, actualRes.get());
    }

    @Test
    public void testCreateOrUpdateTable_UpdateExisting() {
        final Table existingTable = mock(Table.class);
        final Table.Builder tableBuilder = mock(Table.Builder.class);
        final Table updatedTableBuilt = mock(Table.class);
        final Table updatedTableAfterUpdate = mock(Table.class);

        when(bigQueryClient.getTable(any(TableId.class))).thenReturn(existingTable);
        when(existingTable.toBuilder()).thenReturn(tableBuilder);
        when(tableBuilder.setDefinition(any(TableDefinition.class))).thenReturn(tableBuilder);
        when(tableBuilder.build()).thenReturn(updatedTableBuilt);
        when(bigQueryClient.update(eq(updatedTableBuilt))).thenReturn(updatedTableAfterUpdate);

        CreateOrUpdateTableRequest request = new CreateOrUpdateTableRequest(project, dataset, table, List.of());

        var actualRes = bigQueryService.createOrUpdateTable(request);

        assertTrue(actualRes.isRight());
        assertEquals(updatedTableAfterUpdate, actualRes.get());

        verify(bigQueryClient).getTable(TableId.of(project, dataset, table));
        verify(existingTable).toBuilder();
        verify(tableBuilder).setDefinition(any(StandardTableDefinition.class));
        verify(tableBuilder).build();
        verify(bigQueryClient).update(updatedTableBuilt);
    }

    @Test
    public void testCreateOrUpdateTable_Error() {
        when(bigQueryClient.getTable(any(TableId.class))).thenThrow(ex);

        CreateOrUpdateTableRequest request = new CreateOrUpdateTableRequest(project, dataset, table, List.of());
        String expectedDesc = "Failed to create or update table 'project.dataset.table': Unauthorized";

        var actualRes = bigQueryService.createOrUpdateTable(request);

        assertTrue(actualRes.isLeft());
        assertEquals(expectedDesc, actualRes.getLeft().problems().get(0).description());
    }

    @Test
    public void testDeleteTable_Ok() {
        when(bigQueryClient.delete(any(TableId.class))).thenReturn(true);

        DeleteTableRequest request = new DeleteTableRequest(project, dataset, table);
        var actualRes = bigQueryService.deleteTable(request);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testDeleteTable_Error() {
        when(bigQueryClient.delete(any(TableId.class))).thenThrow(ex);
        String expectedDesc = "Failed to delete table 'project.dataset.table': Unauthorized";

        DeleteTableRequest request = new DeleteTableRequest(project, dataset, table);
        var actualRes = bigQueryService.deleteTable(request);

        assertTrue(actualRes.isLeft());
        assertEquals(expectedDesc, actualRes.getLeft().problems().get(0).description());
    }
}
