package com.aspicereporting.jasper;

import com.aspicereporting.entity.Source;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.SourceData;
import com.aspicereporting.entity.items.SimpleTable;
import com.aspicereporting.entity.items.TableColumn;
import com.aspicereporting.entity.views.View;
import com.aspicereporting.jasper.service.SimpleTableService;
import net.sf.jasperreports.components.table.StandardTable;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.engine.design.JRDesignComponentElement;
import net.sf.jasperreports.engine.design.JasperDesign;
import org.junit.jupiter.api.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleTableTests {

    private SimpleTableService simpleTableService;

    private SimpleTable simpleTable;

    private static Source source;
    @BeforeAll
    public static void init() {
        source = new Source();
        SourceColumn assessor = new SourceColumn(0L, "Assessor");
        SourceData sd = new SourceData("Tomas");
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        sd = new SourceData("Jakub");
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);
        assessor.addSourceData(sd);

        SourceColumn process = new SourceColumn(1L, "Process");
        sd = new SourceData("SYS.5");
        process.addSourceData(sd);
        process.addSourceData(sd);
        process.addSourceData(sd);
        process.addSourceData(sd);
        process.addSourceData(sd);
        process.addSourceData(sd);
        process.addSourceData(sd);
        sd = new SourceData("SYS.4");
        process.addSourceData(sd);
        process.addSourceData(sd);
        sd = new SourceData("SYS.5");
        process.addSourceData(sd);
        process.addSourceData(sd);
        process.addSourceData(sd);

        SourceColumn attribute = new SourceColumn(2L, "Capability level");
        sd = new SourceData("Capability Level 1");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        sd = new SourceData("Capability Level 2");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        sd = new SourceData("Capability Level 1");
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);
        attribute.addSourceData(sd);

        SourceColumn criterion = new SourceColumn(3L, "Criterion");
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP3"));
        criterion.addSourceData(new SourceData("2.1"));
        criterion.addSourceData(new SourceData("2.1"));
        criterion.addSourceData(new SourceData("2.2"));
        criterion.addSourceData(new SourceData("2.2"));
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP1"));
        criterion.addSourceData(new SourceData("BP2"));
        criterion.addSourceData(new SourceData("BP3"));

        SourceColumn score = new SourceColumn(4L, "Score");
        score.addSourceData(new SourceData("F"));
        score.addSourceData(new SourceData("F"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("L"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("P"));
        score.addSourceData(new SourceData("L"));

        List<SourceColumn> sourceColumns = Arrays.asList(assessor, process, attribute, criterion, score);
        source.addSourceColumns(sourceColumns);
        source.setSourceName("Source1");
    }

    @BeforeEach
    public void beforeEach() {
        simpleTableService = new SimpleTableService();
        simpleTable = new SimpleTable();
        simpleTable.setSource(source);
        TableColumn tc = new TableColumn();
        tc.setSourceColumn(source.getSourceColumns().get(0));
        TableColumn tc2 = new TableColumn();
        tc2.setSourceColumn(source.getSourceColumns().get(1));
        simpleTable.setTableColumns(Arrays.asList(tc, tc2));
    }

    @DisplayName("Create table element test")
    @Test
    public void createElementTest() throws JRException {
        JasperDesign jasperDesign = new JasperDesign();
        Map<String, Object> parameters = new HashMap<>();
        JRDesignComponentElement element = simpleTableService.createElement(jasperDesign, simpleTable, 0,parameters);
        System.out.println(element);

       Assertions.assertEquals("tableDataset0", ((StandardTable)element.getComponent()).getDatasetRun().getDatasetName());
       Assertions.assertEquals(2, ((StandardTable)element.getComponent()).getColumns().size());
       Assertions.assertEquals(2, jasperDesign.getDatasetMap().get("tableDataset0").getFields().length);
       Assertions.assertEquals(1, parameters.size());
       Assertions.assertTrue(parameters.keySet().contains("tableData0"));
    }
}
