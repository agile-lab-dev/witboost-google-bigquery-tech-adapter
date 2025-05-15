package com.witboost.provisioning.bigquery.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.google.cloud.bigquery.*;
import com.witboost.provisioning.bigquery.model.CreateViewRequest;
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
}
