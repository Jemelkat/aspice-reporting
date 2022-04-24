package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.ReportPage;
import com.aspicereporting.entity.User;
import com.aspicereporting.entity.enums.Orientation;
import com.aspicereporting.entity.items.*;
import com.aspicereporting.exception.JasperReportException;
import com.aspicereporting.jasper.service.*;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.engine.type.WhenNoDataTypeEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.*;

@Service
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class JasperService {

    @Autowired
    SimpleTableService simpleTableService;
    @Autowired
    CapabilityTableService capabilityTableService;
    @Autowired
    TextService textService;
    @Autowired
    LevelBarGraphService levelBarGraphService;
    @Autowired
    LevelPieGraphService levelPieGraphService;
    @Autowired
    ItemValidationService itemValidationService;


    //Parameters with data - used to fill report
    private Map<String, Object> parameters = new HashMap();

    public ByteArrayOutputStream generateReport(Report report, User user) {
        //Validate report items
        for (ReportPage reportPage : report.getReportPages()) {
            itemValidationService.validateItemsWithValid(reportPage.getReportItems(), false, user);
        }

        List<JasperPrint> jasperPrints = new ArrayList<>();
        for (ReportPage reportPage : report.getReportPages()) {
            //Get JasperDesign
            JasperDesign jasperDesign = getJasperDesign(reportPage);
            //Compile JasperDesign
            JasperReport jasperReport = compileReport(jasperDesign);
            //Fill report
            jasperPrints.add(fillReport(jasperReport, this.parameters));
        }
        //Export
        return exportToPdf(jasperPrints, report.getReportUser().getUsername());
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

    public JasperPrint fillReport(JasperReport jasperReport, Map<String, Object> parameters) {
        JasperPrint jasperPrint;
        try {
            jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, new JREmptyDataSource());
        } catch (JRException e) {
            throw new JasperReportException("Error filling the report.", e);
        }
        return jasperPrint;
    }

    private JasperDesign getJasperDesign(ReportPage reportPage) {
        JasperDesign jasperDesign = initializeJasperDesign(reportPage);

        //Create band for report items - same height as report
        JRDesignBand band = new JRDesignBand();
        band.setHeight(jasperDesign.getPageHeight());
        band.setSplitType(SplitTypeEnum.STRETCH);

        //Sort based on Y - will be filling report from top to down
        Collections.sort(reportPage.getReportItems());

        //Counters for item settings - will be used to name each parameter
        Integer textCounter = 0;
        Integer tableCounter = 0;
        Integer graphCounter = 0;
        for (ReportItem reportItem : reportPage.getReportItems()) {
            //Validate - report item is in bounds
            if ((reportItem.getX() + reportItem.getWidth()) > jasperDesign.getPageWidth() || (reportItem.getY() + reportItem.getHeight()) > jasperDesign.getPageHeight()) {
                throw new JasperReportException("Report item is out of page bounds.");
            }

            /*TEXT ITEM*/
            if (reportItem instanceof TextItem textItem) {
                try {
                    JRDesignStaticText jrStaticText = textService.createElement(jasperDesign, textItem, textCounter);
                    //Add created item to report
                    band.addElement(jrStaticText);
                    textCounter++;
                } catch (JRException e) {
                    throw new JasperReportException("Error setting the text style of report.", e);
                }
            }
            /*SIMPLE TABLE ITEM*/
            else if (reportItem instanceof SimpleTable simpleTable) {
                try {
                    //Create table element for JR
                    JRDesignComponentElement JRTableElement = simpleTableService.createElement(jasperDesign, simpleTable, tableCounter, parameters);
                    //Add table to JR
                    band.addElement(JRTableElement);
                    tableCounter++;
                } catch (JRException e) {
                    throw new JasperReportException("Error creating the table item for report", e);
                }
            }
            /*CAPABILITY TABLE ITEM*/
            else if (reportItem instanceof CapabilityTable capabilityTable) {
                try {
                    //Create table element for JR
                    JRDesignComponentElement JRTableElement = capabilityTableService.createElement(jasperDesign, capabilityTable, tableCounter, parameters);
                    //Add table to JR
                    band.addElement(JRTableElement);
                    tableCounter++;
                } catch (JRException e) {
                    throw new JasperReportException("Error creating the capability table item for report", e);
                }
            }
            /*SOURCES BAR GRAPH*/
            else if (reportItem instanceof LevelBarGraph levelBarGraph) {
                try {
                    JRDesignImage element = levelBarGraphService.createElement(jasperDesign, levelBarGraph, graphCounter, parameters);
                    band.addElement(element);
                    graphCounter++;
                } catch (JRException e) {
                    throw new JasperReportException("Error creating the capability graph item for report", e);
                }
            }
            /*LEVEL PIE GRAPH*/
            else if (reportItem instanceof LevelPieGraph levelPieGraph) {
                try {
                    JRDesignImage element = levelPieGraphService.createElement(jasperDesign, levelPieGraph, graphCounter, parameters);
                    band.addElement(element);
                    graphCounter++;
                } catch (JRException e) {
                    throw new JasperReportException("Error creating the level pie graph item for report", e);
                }
            } else {
                throw new JasperReportException("Unsupported report item type: " + reportItem.getType());
            }
            //Create required datasources and fields
        }
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(band);
        return jasperDesign;
    }

    private JasperDesign initializeJasperDesign(ReportPage reportPage) {
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName(reportPage.getReport().getReportName());
        if (reportPage.getOrientation().equals(Orientation.VERTICAL)) {
            jasperDesign.setPageWidth(794);
            jasperDesign.setPageHeight(1123);
        } else {
            jasperDesign.setPageWidth(1123);
            jasperDesign.setPageHeight(794);
        }
        jasperDesign.setLeftMargin(0);
        jasperDesign.setRightMargin(0);
        jasperDesign.setTopMargin(0);
        jasperDesign.setBottomMargin(0);
        jasperDesign.setWhenNoDataType(WhenNoDataTypeEnum.ALL_SECTIONS_NO_DETAIL);
        return jasperDesign;
    }

    private ByteArrayOutputStream exportToPdf(List<JasperPrint> jasperPrint, String author) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(SimpleExporterInput.getInstance(jasperPrint));
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
