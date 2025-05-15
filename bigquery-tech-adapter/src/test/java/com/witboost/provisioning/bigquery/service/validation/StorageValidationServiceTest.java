package com.witboost.provisioning.bigquery.service.validation;

import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.cloud.bigquery.*;
import com.witboost.provisioning.bigquery.model.BigQueryStorageSpecific;
import com.witboost.provisioning.bigquery.service.BigQueryService;
import com.witboost.provisioning.bigquery.service.ResourceManagerService;
import com.witboost.provisioning.bigquery.util.ResourceUtils;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.StorageArea;
import com.witboost.provisioning.model.common.FailedOperation;
import com.witboost.provisioning.model.common.Problem;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.parser.Parser;
import io.vavr.control.Either;
import io.vavr.control.Option;
import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StorageValidationServiceTest {

    @Mock
    private BigQueryService bigQueryService;

    @Mock
    private ResourceManagerService resourceManagerService;

    @InjectMocks
    private StorageValidationService storageValidationService;

    private final ProvisionOperationRequest<Specific, BigQueryStorageSpecific> provisionOperationRequest;

    private final Table mockedTable = mock(Table.class);
    private final StandardTableDefinition mockedTableDefinition = mock(StandardTableDefinition.class);

    public StorageValidationServiceTest() throws IOException {
        BigQueryStorageSpecific specific = new BigQueryStorageSpecific();
        String ymlDescriptor = ResourceUtils.getContentFromResource("/pr_descriptor_bigquery_st.yml");
        var componentDescriptor =
                Parser.parseComponentDescriptor(ymlDescriptor, Specific.class).get();
        var component = componentDescriptor
                .getDataProduct()
                .getComponentToProvision(componentDescriptor.getComponentIdToProvision())
                .get();
        var st = Parser.parseComponent(component, StorageArea.class, BigQueryStorageSpecific.class)
                .get();
        specific.setProject("project1");
        specific.setDataset("dataset1");
        specific.setTableName("table1");

        StorageArea<BigQueryStorageSpecific> storageArea = new StorageArea<>();
        storageArea.setSpecific(specific);

        provisionOperationRequest = new ProvisionOperationRequest<Specific, BigQueryStorageSpecific>(
                componentDescriptor.getDataProduct(), st, false, Optional.empty());
    }

    @Test
    public void testValidateOk() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(true));
        when(mockedTable.getDefinition()).thenReturn(mockedTableDefinition);
        when(mockedTableDefinition.getSchema()).thenReturn(Schema.of(Field.of("column1", StandardSQLTypeName.STRING)));
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.of(mockedTable)));
        when(bigQueryService.isTableSchemaCompatibleWithColumns(any(), anyList()))
                .thenReturn(right(true));

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isRight());
    }

    // Test that the project exists
    @Test
    public void testValidateProjectExists() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(true));

        StandardTableDefinition mockedDefinition = mock(StandardTableDefinition.class);

        when(mockedDefinition.getSchema()).thenReturn(Schema.of());
        when(mockedTable.getDefinition()).thenReturn(mockedDefinition);
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.of(mockedTable)));
        when(bigQueryService.isTableSchemaCompatibleWithColumns(any(), any())).thenReturn(Either.right(true));

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isRight());
    }

    // Test that the project doesn't exists
    @Test
    public void testValidateProjectNotExists() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(false));

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isLeft());
        assertEquals("One or more validation errors occurred", result.getLeft().message());
    }

    // Test that the table exists
    @Test
    public void testValidateTableExists() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(true));

        StandardTableDefinition tableDefinition = mock(StandardTableDefinition.class);
        Schema schema = Schema.of();

        when(mockedTable.getDefinition()).thenReturn(tableDefinition);
        when(tableDefinition.getSchema()).thenReturn(schema);
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.of(mockedTable)));
        when(bigQueryService.isTableSchemaCompatibleWithColumns(any(), any())).thenReturn(Either.right(true));

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isRight());
    }

    // Test that the table doesn't exists
    @Test
    public void testValidateTableNotExists() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(true));
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.none()));

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isRight());
    }

    // Test that skip the check of the schema if the table doesn't exists
    @Test
    public void testValidateSkipsSchemaCheckIfTableMissing() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(true));
        when(bigQueryService.getTable(anyString(), anyString(), anyString()))
                .thenReturn(right(Option.none())); // Table not found

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isRight());

        // Check that never call isTableSchemaCompatibleWithColumns
        verify(bigQueryService, never()).isTableSchemaCompatibleWithColumns(any(), anyList());
    }

    // Test result true with compatibility between schema and list of columns
    @Test
    public void testValidateCompatibleSchema() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(true));
        when(mockedTable.getDefinition()).thenReturn(mockedTableDefinition);
        when(mockedTableDefinition.getSchema()).thenReturn(Schema.of(Field.of("column1", StandardSQLTypeName.STRING)));
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.of(mockedTable)));
        when(bigQueryService.isTableSchemaCompatibleWithColumns(any(), anyList()))
                .thenReturn(right(true));

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isRight());
    }

    // Test result false with compatibility between schema and list of columns
    @Test
    public void testValidateIncompatibleSchema() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(true));
        when(mockedTable.getDefinition()).thenReturn(mockedTableDefinition);
        when(mockedTableDefinition.getSchema()).thenReturn(Schema.of(Field.of("column1", StandardSQLTypeName.STRING)));
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.of(mockedTable)));
        when(bigQueryService.isTableSchemaCompatibleWithColumns(any(), anyList()))
                .thenReturn(right(false));

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isLeft());
        assertEquals("One or more validation errors occurred", result.getLeft().message());
    }

    // Test check return error during the compatibility between schema and list of columns
    @Test
    public void testValidateSchemaCheckFailure() {
        when(resourceManagerService.isProjectExisting(anyString())).thenReturn(right(true));
        when(mockedTable.getDefinition()).thenReturn(mockedTableDefinition);
        when(mockedTableDefinition.getSchema()).thenReturn(Schema.of(Field.of("column1", StandardSQLTypeName.STRING)));
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.of(mockedTable)));

        var failedOp = new FailedOperation(
                "Schema validation error",
                Collections.singletonList(new Problem("Error while checking schema compatibility")));

        when(bigQueryService.isTableSchemaCompatibleWithColumns(any(), anyList()))
                .thenReturn(io.vavr.control.Either.left(failedOp));

        var result = storageValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(result.isLeft());
        assertEquals("Schema validation error", result.getLeft().message());
        assertEquals(1, result.getLeft().problems().size());
        assertEquals(
                "Error while checking schema compatibility",
                result.getLeft().problems().get(0).description());
    }

    @Test
    public void testValidateNoComponent() {
        ProvisionOperationRequest<Specific, BigQueryStorageSpecific> provisionOperationRequestWithoutComponent =
                new ProvisionOperationRequest<>(null, Optional.empty(), false, Optional.empty());

        var result =
                storageValidationService.validate(provisionOperationRequestWithoutComponent, OperationType.VALIDATE);

        assertTrue(result.isLeft());
        assertEquals(
                "Operation request didn't contain a component to operate with. Expected a component descriptor",
                result.getLeft().problems().get(0).description());
    }
}
