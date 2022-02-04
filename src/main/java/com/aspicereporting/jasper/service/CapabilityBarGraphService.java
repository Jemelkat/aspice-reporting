package com.aspicereporting.jasper.service;

import com.aspicereporting.entity.items.CapabilityBarGraph;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRenderable;
import net.sf.jasperreports.engine.design.*;
import net.sf.jasperreports.engine.type.PositionTypeEnum;
import net.sf.jasperreports.engine.type.ScaleImageEnum;
import net.sf.jasperreports.renderers.JCommonDrawableRendererImpl;
import net.sf.jasperreports.renderers.Renderable;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CapabilityBarGraphService extends BaseChartService {
    public JRDesignImage createElement(JasperDesign jasperDesign, CapabilityBarGraph capabilityBarGraph, Integer counter, Map<String, Object> parameters) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        dataset.addValue(1, "", "PROCESS1");
        dataset.addValue(2, "", "PROCESS2");
        dataset.addValue(3, "", "PROCESS3");
        dataset.addValue(1, "", "PROCESS4");
        dataset.addValue(9, "", "PROCESS5");
        dataset.addValue(1, "", "PROCESS6");


        final JFreeChart chart = ChartFactory.createBarChart(
                "",                                   // chart title
                "Process",                  // domain axis label
                "Capability level",                     // range axis label
                dataset,                     // data
                PlotOrientation.VERTICAL,    // the plot orientation
                false,                        // legend
                false,                        // tooltips
                false                        // urls
        );
        this.applyBarGraphTheme(chart);

        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setUpperBound(5);
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        CategoryAxis categoryAxis = plot.getDomainAxis();
        categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);

        parameters.put("Chart", new JCommonDrawableRendererImpl(chart));

        JRDesignParameter parameter = new JRDesignParameter();
        parameter.setValueClass(Renderable.class);
        parameter.setName("Chart");
        try {
            jasperDesign.addParameter(parameter);
        } catch (JRException e) {
            e.printStackTrace();
        }


        JRDesignImage imageElement = new JRDesignImage(jasperDesign);
        imageElement.setX(capabilityBarGraph.getX());
        imageElement.setY(capabilityBarGraph.getY());
        imageElement.setWidth(capabilityBarGraph.getWidth());
        imageElement.setHeight(capabilityBarGraph.getHeight());
        imageElement.setPositionType(PositionTypeEnum.FLOAT);
        imageElement.setScaleImage(ScaleImageEnum.FILL_FRAME);
        imageElement.setLazy(true);
        JRDesignExpression expression = new JRDesignExpression();
        expression.setText("$P{Chart}");
        expression.setValueClass(JRRenderable.class);
        imageElement.setExpression(expression);

        return imageElement;
    }
}
