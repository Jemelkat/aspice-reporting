package com.aspicereporting.jasper.service;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.ui.RectangleInsets;

import java.awt.*;

public abstract class BaseChartService extends AbstractItemService {
    protected static final String CHART = "chart";

    protected void applyBarGraphTheme(JFreeChart chart) {
        String fontName = "Lucida Sans";
        StandardChartTheme theme = (StandardChartTheme)org.jfree.chart.StandardChartTheme.createJFreeTheme();

        theme.setTitlePaint( Color.decode( "#4572a7" ) );
        theme.setExtraLargeFont( new Font(fontName,Font.PLAIN, 12) ); //title
        theme.setLargeFont( new Font(fontName,Font.BOLD, 10)); //axis-title
        theme.setRegularFont( new Font(fontName,Font.PLAIN, 8));
        theme.setRangeGridlinePaint( Color.decode("#C0C0C0"));
        theme.setPlotBackgroundPaint( Color.white );
        theme.setChartBackgroundPaint( Color.white );
        theme.setGridBandPaint( Color.red );
        theme.setAxisOffset( new RectangleInsets(0,0,0,0) );
        theme.setBarPainter(new StandardBarPainter());
        theme.setAxisLabelPaint( Color.decode("#666666")  );
        theme.apply( chart );
        chart.getCategoryPlot().setOutlineVisible( false );
        chart.getCategoryPlot().getRangeAxis().setAxisLineVisible( false );
        chart.getCategoryPlot().getRangeAxis().setTickMarksVisible( false );
        chart.getCategoryPlot().setRangeGridlineStroke( new BasicStroke() );
        chart.getCategoryPlot().getRangeAxis().setTickLabelPaint( Color.decode("#666666") );
        chart.getCategoryPlot().getDomainAxis().setTickLabelPaint( Color.decode("#666666") );
        chart.setTextAntiAlias( true );
        chart.setAntiAlias( true );
        chart.getCategoryPlot().getRenderer().setSeriesPaint( 0, Color.decode( "#619ED6" ));
        chart.getCategoryPlot().getRenderer().setSeriesPaint( 1, Color.decode( "#E64345" ));
        chart.getCategoryPlot().getRenderer().setSeriesPaint( 2, Color.decode( "#6BA547" ));
        chart.getCategoryPlot().getRenderer().setSeriesPaint( 3, Color.decode( "#E48F1B" ));
        chart.getCategoryPlot().getRenderer().setSeriesPaint( 4, Color.decode( "#B77EA3" ));
        chart.getCategoryPlot().getRenderer().setSeriesPaint( 5, Color.decode( "#60CEED" ));

        BarRenderer rend = (BarRenderer) chart.getCategoryPlot().getRenderer();
        rend.setShadowVisible( true );
        rend.setShadowXOffset( 2 );
        rend.setShadowYOffset( 0 );
        rend.setShadowPaint( Color.decode( "#C0C0C0"));
        rend.setMaximumBarWidth(1);
    }
}
