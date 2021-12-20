package com.aspicereporting.service;

import com.aspicereporting.entity.Report;
import com.aspicereporting.entity.TextItem;
import com.aspicereporting.entity.items.ReportItem;
import com.aspicereporting.repository.UserRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStaticText;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.HorizontalTextAlignEnum;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.SplitTypeEnum;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Collections;

import static com.aspicereporting.entity.items.ReportItem.EItemType.STATIC_TEXT;

@Service
public class JasperService {

    public ByteArrayOutputStream generateReport(Report report) {
        //Get JasperDesign
        JasperDesign jasperDesign = getJasperDesign(report);

        //Compile JasperDesign
        JasperReport jasperReport = null;
        try {
            jasperReport = JasperCompileManager.compileReport(jasperDesign);
        } catch (JRException e) {
            //TODO throw exception
        }

        //Fill report with data
        // TODO
        JasperPrint jasperPrint = null;
        try {
            jasperPrint = JasperFillManager.fillReport(jasperReport, null, new JREmptyDataSource());
        } catch (JRException e) {
            //TODO throw exception
        }
        //Export
        //TODO REMOVE
        JRPdfExporter exporter = new JRPdfExporter();
        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(new SimpleOutputStreamExporterOutput("file2.pdf"));
        SimplePdfExporterConfiguration configuration = new SimplePdfExporterConfiguration();
        configuration.setMetadataAuthor("Petter");  //why not set some config as we like
        exporter.setConfiguration(configuration);
        try {
            exporter.exportReport();
        } catch (JRException e) {
            e.printStackTrace();
        }
        return new ByteArrayOutputStream();
        //return exportToPdf(jasperPrint, report.getReportUser().getUsername());
    }

    private JasperDesign getJasperDesign(Report report) {
        JasperDesign jasperDesign = initializeJasperDesign(report);

        //Sort based on Y - will be filling report from top to down
        Collections.sort(report.getReportItems());

        //Make whole page design band
        JRDesignBand band = new JRDesignBand();
        //TODO MAKE DYNAMIC
        band.setHeight(1122);
        band.setSplitType(SplitTypeEnum.STRETCH);

        //Get report item
        for (ReportItem reportItem : report.getReportItems()) {
            //Create report item jasper object
            switch (reportItem.getType()) {
                case STATIC_TEXT:
                    JRDesignStaticText textItem = createStaticText((TextItem) reportItem);
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

    private JasperDesign initializeJasperDesign(Report report) {
        //JasperDesign
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName("NoXmlDesignReport");
        jasperDesign.setPageWidth(794);
        jasperDesign.setPageHeight(1122);
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
