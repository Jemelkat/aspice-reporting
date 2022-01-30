package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.SourceColumn;
import com.aspicereporting.entity.TextStyle;
import com.aspicereporting.entity.items.*;
import com.aspicereporting.exception.JasperReportException;
import com.aspicereporting.jasper.SimpleTableModel;
import net.sf.jasperreports.components.ComponentsExtensionsRegistryFactory;
import net.sf.jasperreports.components.table.DesignCell;
import net.sf.jasperreports.components.table.StandardColumn;
import net.sf.jasperreports.components.table.StandardTable;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.component.ComponentKey;
import net.sf.jasperreports.engine.data.JRTableModelDataSource;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;

@Service
@Scope(value="prototype", proxyMode= ScopedProxyMode.TARGET_CLASS)
public class JasperService {

    private static final String TABLE_DATA = "tableData";
    private static final String TABLE_DATASET = "tableDataset";

    //Parameters with data - used to fill report
    private Map<String, Object> parameters = new HashMap();

    public ByteArrayOutputStream generateReport(Report report) {
        //Get JasperDesign
        JasperDesign jasperDesign = getJasperDesign(report);
        //TODO REMOVE
        try {
            JasperCompileManager.writeReportToXmlFile(jasperDesign, "test.jrxml");
        } catch (JRException e) {
            e.printStackTrace();
        }
        //Compile JasperDesign
        JasperReport jasperReport = compileReport(jasperDesign);
        JasperPrint jasperPrint = fillReport(jasperReport, this.parameters);

        //Export
        //TODO REMOVE
//        JRPdfExporter exporter = new JRPdfExporter();
//        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
//        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("file2.pdf"));
//        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
//        configuration.setMetadataAuthor("Petter");  //why not set some config as we like
//        exporter.setConfiguration(configuration);
//        try {
//            exporter.exportReport();
//        } catch (JRException e) {
//            e.printStackTrace();
//        }
        //return new ByteArrayOutputStream();
        return exportToPdf(jasperPrint, report.getReportUser().getUsername());
    }

    public JasperReport compileReport(JasperDesign jasperDesign) {
        JasperReport jasperReport;
        try {
            jasperReport = JasperCompileManager.compileReport(jasperDesign);
        } catch (JRException e) {
            throw new JasperReportException("Error compiling the report.", e);
        }
        return jasperReport;
    }

    public JasperPrint fillReport(JasperReport jasperReport,Map<String, Object> parameters) {
        JasperPrint jasperPrint;
        try {
            jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
        } catch (JRException e) {
            throw new JasperReportException("Error filling the report.", e);
        }
        return jasperPrint;
    }

    private JasperDesign getJasperDesign(Report report) {
        JasperDesign jasperDesign = initializeJasperDesign(report);

        //Create band for report items - same height as report
        JRDesignBand band = new JRDesignBand();
        band.setHeight(jasperDesign.getPageHeight());
        band.setSplitType(SplitTypeEnum.STRETCH);

        //Sort based on Y - will be filling report from top to down
        Collections.sort(report.getReportItems());

        //Counters for item settings
        int styleCount = 0;
        int tableDatasetCount = 0;
        for (ReportItem reportItem : report.getReportItems()) {
            //Validate - report item is in bounds
            if ((reportItem.getX() + reportItem.getWidth()) > jasperDesign.getPageWidth() || (reportItem.getY() + reportItem.getHeight()) > jasperDesign.getPageHeight()) {
                throw new JasperReportException("Report item is out of page bounds.");
            }

            /*TEXT ITEM*/
            if (reportItem instanceof TextItem textItem) {
                JRDesignStaticText jrStaticText = createStaticText(textItem);

                //Add style to text item - only if style is defined
                if (textItem.getTextStyle() != null) {
                    JRDesignStyle textStyle = createTextStyle(textItem.getTextStyle(), styleCount++);
                    try {
                        jasperDesign.addStyle(textStyle);
                        jrStaticText.setStyle(textStyle);
                    } catch (JRException e) {
                        throw new JasperReportException("Error setting the text style of report.", e);
                    }
                }
                jrStaticText.setPositionType(PositionTypeEnum.FLOAT);
                //Add created item to report
                band.addElement(jrStaticText);
            }
            /*SIMPLE TABLE ITEM*/
            else if (reportItem instanceof TableItem tableItem) {
                try {
                    //Creates parameter for data TableModel
                    JRDesignParameter parameter = new JRDesignParameter();
                    parameter.setValueClass(JRTableModelDataSource.class);
                    parameter.setName(TABLE_DATA + tableDatasetCount);

                    jasperDesign.addParameter(parameter);


                    //Creates subdataset
                    JRDesignDataset tableSubdataset = new JRDesignDataset(false);
                    tableSubdataset.setName(TABLE_DATASET + tableDatasetCount);

                    //Creates required fields
                    List<String> columnArrray = new ArrayList<>();
                    int rows = 0;
                    int columns = 0;
                    for (TableColumn tableColumn : tableItem.getTableColumns()) {
                        SourceColumn sourceColumn = tableColumn.getSourceColumn();

                        JRDesignField field = new JRDesignField();
                        field.setValueClass(String.class);
                        field.setName(sourceColumn.getColumnName());
                        tableSubdataset.addField(field);

                        //Creates column names array
                        columnArrray.add(sourceColumn.getColumnName());
                        if (rows == 0 && columns == 0) {
                            rows = sourceColumn.getSourceData().size();
                            columns = tableItem.getTableColumns().size();
                        }
                    }
                    jasperDesign.addDataset(tableSubdataset);

                    //Creates object with data
                    Object[][] test = new Object[rows][columns];
                    for(int i = 0;i<tableItem.getTableColumns().size();i++){
                        SourceColumn sc = tableItem.getTableColumns().get(i).getSourceColumn();
                        for(int j = 0;j<sc.getSourceData().size();j++){
                            test[j][i] = sc.getSourceData().get(j).getValue();
                        }
                    }

                    //Creates data bean
                    SimpleTableModel tableModel = new SimpleTableModel(rows, columns);
                    tableModel.setColumnNames(columnArrray.toArray(new String[0]));
                    tableModel.setData(test);

                    this.parameters.put(TABLE_DATA+tableDatasetCount, new JRTableModelDataSource(tableModel));

                    //Create table element
                    JRDesignComponentElement tableElement = new JRDesignComponentElement(jasperDesign);
                    tableElement.setX(tableItem.getX());
                    tableElement.setY(tableItem.getY());
                    tableElement.setWidth(tableItem.getWidth());
                    tableElement.setHeight(tableItem.getHeight());
                    tableElement.setPositionType(PositionTypeEnum.FLOAT);

                    ComponentKey componentKey = new ComponentKey(ComponentsExtensionsRegistryFactory.NAMESPACE, "c",
                            ComponentsExtensionsRegistryFactory.TABLE_COMPONENT_NAME);
                    tableElement.setComponentKey(componentKey);

                    //Standard table
                    StandardTable table = new StandardTable();
                    //the table data source
                    JRDesignDatasetRun datasetRun = new JRDesignDatasetRun();
                    datasetRun.setDatasetName(TABLE_DATASET + tableDatasetCount);
                    datasetRun.setDataSourceExpression(new JRDesignExpression(
                            "$P{" + TABLE_DATA + tableDatasetCount + "}"));
                    table.setDatasetRun(datasetRun);

                    //TODO REMOVE FIRST COLUMN
//                    StandardColumn recNoColumn = createSimpleTableColumn(jasperDesign,100, 20, "Record", "$V{REPORT_COUNT}");
//                    table.addColumn(recNoColumn);

                    //Create all columns
                    for(TableColumn column : tableItem.getTableColumns()) {
                        String columnName = column.getSourceColumn().getColumnName();
                        StandardColumn fieldColumn = createSimpleTableColumn(jasperDesign,column.getWidth(), 20, columnName, "$F{" + columnName + "}");
                        table.addColumn(fieldColumn);
                    }

                    //Add element to report
                    tableElement.setComponent(table);
                    band.addElement(tableElement);
                    tableDatasetCount++;
                } catch (JRException e) {
                    throw new JasperReportException("Error creating the table item for report", e);
                }
            }
            /*CAPABILITY TABLE ITEM*/
            else if (reportItem instanceof CapabilityTable capabilityTable) {

            }
            /*GRAPH ITEM*/
            else if (reportItem instanceof GraphItem graphItem) {

            } else {
                throw new JasperReportException("Unknown report item type: " + reportItem.getType());
            }
            //Create required datasources and fields
        }
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(band);
        return jasperDesign;
    }

    private JRDesignStaticText createStaticText(TextItem item) {
        JRDesignStaticText element = new JRDesignStaticText();
        element.setText(item.getTextArea());
        element.setX(item.getX());
        element.setY(item.getY());
        element.setWidth(item.getWidth());
        element.setHeight(item.getHeight());
        element.setPositionType(PositionTypeEnum.FLOAT);
        element.setHorizontalTextAlign(HorizontalTextAlignEnum.LEFT);
        return element;
    }

    public JRDesignStyle createTextStyle(TextStyle textStyle, int styleCount) {
        JRDesignStyle jrDesignStyle = new JRDesignStyle();
        jrDesignStyle.setName("Style" + styleCount);
        jrDesignStyle.setBold(Boolean.valueOf(textStyle.isBold()));
        jrDesignStyle.setItalic(Boolean.valueOf(textStyle.isItalic()));
        jrDesignStyle.setUnderline(Boolean.valueOf(textStyle.isUnderline()));
        jrDesignStyle.setForecolor(Color.decode(textStyle.getColor()));
        jrDesignStyle.setFontSize((float) textStyle.getFontSize());
        jrDesignStyle.setFontName("DejaVu Serif");
        return jrDesignStyle;
    }

    private JasperDesign initializeJasperDesign(Report report) {
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName(report.getReportName());
        jasperDesign.setPageWidth(794);
        jasperDesign.setPageHeight(1123);
        jasperDesign.setLeftMargin(0);
        jasperDesign.setRightMargin(0);
        jasperDesign.setTopMargin(0);
        jasperDesign.setBottomMargin(0);
        jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        return jasperDesign;
    }

    private StandardColumn createSimpleTableColumn(JasperDesign jasperDesign, int width, int height, String headerText, String detailExpression)
    {
        StandardColumn column = new StandardColumn();
        column.setWidth(width);

        //column header
        DesignCell header = new DesignCell();
        header.setDefaultStyleProvider(jasperDesign);
        header.getLineBox().getPen().setLineWidth(1f);
        header.setHeight(height);

        JRDesignStaticText headerElement = new JRDesignStaticText(jasperDesign);
        headerElement.setX(0);
        headerElement.setY(0);
        headerElement.setWidth(width);
        headerElement.setHeight(height);
        headerElement.setText(headerText);

        header.addElement(headerElement);
        column.setColumnHeader(header);

        //column detail
        DesignCell detail = new DesignCell();
        detail.setDefaultStyleProvider(jasperDesign);
        detail.getLineBox().getPen().setLineWidth(1f);
        detail.setHeight(height);

        JRDesignTextField detailElement = new JRDesignTextField(jasperDesign);
        detailElement.setX(0);
        detailElement.setY(0);
        detailElement.setWidth(width);
        detailElement.setHeight(height);
        detailElement.setExpression(new JRDesignExpression(detailExpression));

        detail.addElement(detailElement);
        column.setDetailCell(detail);

        return column;
    }

    private ByteArrayOutputStream exportToPdf(JasperPrint jasperPrint, String author) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        configuration.setMetadataAuthor(author);
        exporter.setConfiguration(configuration);
        try {
            exporter.exportReport();
        } catch (JRException e) {
            throw new JasperReportException("Could not export the report.", e);
        }
        return outputStream;
    }
}
