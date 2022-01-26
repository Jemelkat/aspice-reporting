package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.TextStyle;
import com.aspicereporting.entity.items.TextItem;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.exception.JasperReportException;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.Collections;

@Service
public class JasperService {

    public ByteArrayOutputStream generateReport(Report report) throws JRException {
        //Get JasperDesign
        JasperDesign jasperDesign = getJasperDesign(report);

        //Compile JasperDesign
        JasperReport jasperReport = null;
        try {
            jasperReport = JasperCompileManager.compileReport(jasperDesign);
        } catch (JRException e) {
            throw e;
            //TODO throw exception
        }

        //Fill report with data
        // TODO
        JasperPrint jasperPrint = null;
        try {
            jasperPrint = JasperFillManager.fillReport(jasperReport, null, new JREmptyDataSource());
        } catch (JRException e) {
            throw e;
            //TODO throw exception
        }
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

    private JasperDesign getJasperDesign(Report report) {
        JasperDesign jasperDesign = initializeJasperDesign(report);

        //Sort based on Y - will be filling report from top to down
        Collections.sort(report.getReportItems());

        //Make whole page design band
        JRDesignBand band = new JRDesignBand();
        //TODO MAKE DYNAMIC
        band.setHeight(1123);
        band.setSplitType(SplitTypeEnum.STRETCH);

        int styleCount = 0;
        //Get report item
        for (ReportItem reportItem : report.getReportItems()) {
            //Create report item jasper object
            switch (reportItem.getType()) {
                case STATIC_TEXT:
                    //Create TEXT ITEM
                    JRDesignStaticText textItem = createStaticText((TextItem) reportItem);

                    //Add style to text item - only if style is defined
                    if(((TextItem) reportItem).getTextStyle() != null) {
                        JRDesignStyle textStyle = createTextStyle(((TextItem) reportItem).getTextStyle(), styleCount++);
                        try {
                            jasperDesign.addStyle(textStyle);
                            textItem.setStyle(textStyle);
                        } catch (JRException e) {
                            throw new JasperReportException("Error setting the text style of report.", e);
                        }
                    }
                    textItem.setPositionType(PositionTypeEnum.FLOAT);
                    //Add created item to report
                    band.addElement(textItem);
                    break;
                case GRAPH:
                    System.out.println("");
                    break;
                case TABLE:
                    break;
                case IMAGE:
                    break;
                default:
                    //TODO THROW EXCEPTION
                    break;
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

    public JRDesignStyle createTextStyle(TextStyle textStyle, int styleCount){
        JRDesignStyle jrDesignStyle = new JRDesignStyle();
        jrDesignStyle.setName("Style" + styleCount);
        jrDesignStyle.setBold(Boolean.valueOf(textStyle.isBold()));
        jrDesignStyle.setItalic(Boolean.valueOf(textStyle.isItalic()));
        jrDesignStyle.setUnderline(Boolean.valueOf(textStyle.isUnderline()));
        //TODO ADD COLOR TO TEXT
        //jrDesignStyle.setForecolor(Color.decode(textStyle.getColor()));
        jrDesignStyle.setFontSize((float) textStyle.getFontSize());
        return jrDesignStyle;
    }

    private JasperDesign initializeJasperDesign(Report report) {
        //JasperDesign
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName("NoXmlDesignReport");
        jasperDesign.setPageWidth(794);
        jasperDesign.setPageHeight(1123);
        jasperDesign.setLeftMargin(0);
        jasperDesign.setRightMargin(0);
        jasperDesign.setTopMargin(0);
        jasperDesign.setBottomMargin(0);
        return jasperDesign;
    }

    private ByteArrayOutputStream exportToPdf(JasperPrint jasperPrint, String author) {
        //Report output stream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        //Exporter to PDF
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        configuration.setMetadataAuthor(author);  //why not set some config as we like
        exporter.setConfiguration(configuration);
        try {
            exporter.exportReport();
        } catch (JRException e) {
            //TODO: LOG
        }
        return outputStream;
    }
}
