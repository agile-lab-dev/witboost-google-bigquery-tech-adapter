package com.witboost.provisioning.bigquery.service.validation;

import static io.vavr.control.Either.right;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.cloud.bigquery.*;
import com.witboost.provisioning.bigquery.model.BigQueryOutputPortSpecific;
import com.witboost.provisioning.bigquery.service.BigQueryService;
import com.witboost.provisioning.bigquery.util.ResourceUtils;
import com.witboost.provisioning.model.OperationType;
import com.witboost.provisioning.model.OutputPort;
import com.witboost.provisioning.model.Specific;
import com.witboost.provisioning.model.request.ProvisionOperationRequest;
import com.witboost.provisioning.parser.Parser;
import io.vavr.control.Option;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OutputPortValidationServiceTest {

    @Mock
    private BigQueryService bigQueryService;

    @InjectMocks
    private OutputPortValidationService outputPortValidationService;

    private final ProvisionOperationRequest<Specific, BigQueryOutputPortSpecific> provisionOperationRequest;
    private final Table mockedTable = mock(Table.class);
    private final TableDefinition mockedTableDefinition = mock(TableDefinition.class);

    public OutputPortValidationServiceTest() throws IOException {
        String ymlDescriptor = ResourceUtils.getContentFromResource("/pr_descriptor_bigquery_op.yml");
        var componentDescriptor =
                Parser.parseComponentDescriptor(ymlDescriptor, Specific.class).get();
        var component = componentDescriptor
                .getDataProduct()
                .getComponentToProvision(componentDescriptor.getComponentIdToProvision())
                .get();
        var op = Parser.parseComponent(component, OutputPort.class, BigQueryOutputPortSpecific.class)
                .get();
        provisionOperationRequest =
                new ProvisionOperationRequest<>(componentDescriptor.getDataProduct(), op, false, Optional.empty());
    }

    @Test
    public void testValidateOk() {
        when(mockedTable.getDefinition()).thenReturn(mockedTableDefinition);
        Schema schema = Schema.of(Field.of("column1", StandardSQLTypeName.STRING));
        when(mockedTableDefinition.getSchema()).thenReturn(schema);
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.of(mockedTable)));
        when(bigQueryService.isViewSchemaCompatibleWithSourceTableSchema(any(), anyList()))
                .thenReturn(right(true));

        var actualRes = outputPortValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(actualRes.isRight());
    }

    @Test
    public void testValidateIncompatibleSchema() {
        when(mockedTable.getDefinition()).thenReturn(mockedTableDefinition);
        Schema schema = Schema.of(Field.of("column3", StandardSQLTypeName.STRING));
        when(mockedTableDefinition.getSchema()).thenReturn(schema);
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.of(mockedTable)));
        when(mockedTable.getTableId()).thenReturn(TableId.of("project1", "dataset1", "tableName1"));
        when(bigQueryService.isViewSchemaCompatibleWithSourceTableSchema(any(), anyList()))
                .thenReturn(right(false));
        String expectedDesc =
                "View schema of component urn:dmb:cmp:healthcare:vaccinations:0:bigquery-output-port is not compatible with schema of the source table project1.dataset1.tableName1";

        var actualRes = outputPortValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
        });
    }

    @Test
    public void testValidateNotExistingTable() {
        when(bigQueryService.getTable(anyString(), anyString(), anyString())).thenReturn(right(Option.none()));
        String expectedDesc = "The specified source table project1.dataset1.tableName1 doesn't exist";

        var actualRes = outputPortValidationService.validate(provisionOperationRequest, OperationType.VALIDATE);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
        });
        verify(bigQueryService, never()).isViewSchemaCompatibleWithSourceTableSchema(any(), anyList());
    }

    @Test
    public void testValidateNoComponent() {
        var provisionOperationRequestWithoutComponent = new ProvisionOperationRequest<
                Specific, BigQueryOutputPortSpecific>(null, Optional.empty(), false, Optional.empty());
        String expectedDesc =
                "Operation request didn't contain a component to operate with. Expected a component descriptor";

        var actualRes =
                outputPortValidationService.validate(provisionOperationRequestWithoutComponent, OperationType.VALIDATE);

        assertTrue(actualRes.isLeft());
        assertEquals(1, actualRes.getLeft().problems().size());
        actualRes.getLeft().problems().forEach(p -> {
            assertEquals(expectedDesc, p.description());
            assertTrue(p.cause().isEmpty());
        });
        verifyNoInteractions(bigQueryService);
    }
}
