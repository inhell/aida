package ru.inhell.aida.gui;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.Minute;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultHighLowDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inhell.aida.entity.*;
import ru.inhell.aida.inject.AidaInjector;
import ru.inhell.aida.oracle.AlphaOracleBean;
import ru.inhell.aida.oracle.AlphaOracleService;
import ru.inhell.aida.oracle.IAlphaOracleListener;
import ru.inhell.aida.quotes.QuotesBean;
import ru.inhell.aida.util.DateUtil;

import javax.swing.*;
import java.awt.*;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Anatoly A. Ivanov java@inheaven.ru
 *         Date: 22.03.11 18:15
 */
public class AlphaOracleChart extends JPanel{
    private final static Logger log = LoggerFactory.getLogger(AlphaOracleChart.class);

    public AlphaOracleChart() {
        setLayout(new BorderLayout());

        final QuotesBean quotesBean = AidaInjector.getInstance(QuotesBean.class);
        final AlphaOracleBean alphaOracleBean = AidaInjector.getInstance(AlphaOracleBean.class);
//        final VectorForecastBean vectorForecastBean = AidaInjector.getInstance(VectorForecastBean.class);
//        final CurrentBean currentBean = AidaInjector.getInstance(CurrentBean.class);
        final AlphaOracleService alphaOracleService = AidaInjector.getInstance(AlphaOracleService.class);

        final List<AlphaOracle> alphaOracles = alphaOracleBean.getAlphaOracles();

        final int size = 120;

        //model
        final Date[] date = new Date[size];
        final double[] open = new double[size];
        final double[] low = new double[size];
        final double[] high = new double[size];
        final double[] close = new double[size];
        final double[] volume = new double[size];

        //chart
        final JFreeChart chart = ChartFactory.createCandlestickChart("", "date", "price", null, true);
        ((NumberAxis)chart.getXYPlot().getRangeAxis()).setAutoRangeIncludesZero(false);
        add(new ChartPanel(chart));

        //prediction
        final TimeSeriesCollection predictionPoint = new TimeSeriesCollection();

        for (AlphaOracle ao : alphaOracles){
            predictionPoint.addSeries(new TimeSeries("long" + ao.getId()));
            predictionPoint.addSeries(new TimeSeries("short" + ao.getId()));
        }

        chart.getXYPlot().setDataset(1, predictionPoint);
        chart.getXYPlot().setRenderer(1, new XYLineAndShapeRenderer(false, true));

        //forecastLine
        final TimeSeriesCollection forecastLine = new TimeSeriesCollection();
        for (AlphaOracle ao : alphaOracles) {
            forecastLine.addSeries(new TimeSeries("forecast" + ao.getId()));
        }

        chart.getXYPlot().setDataset(2, forecastLine);
        chart.getXYPlot().setRenderer(2, new XYLineAndShapeRenderer(true, false));

        alphaOracleService.addListener(new IAlphaOracleListener() {
            @Override
            public void predicted(AlphaOracle ao, AlphaOracleData.PREDICTION prediction, List<Quote> quotes, float[] forecast) {
                TimeSeries forecastTimeSeries = forecastLine.getSeries("forecast" + ao.getId());

                forecastTimeSeries.clear();

                int n = ao.getVectorForecast().getN();

                Date p = quotes.get(n-1).getDate();

                if (date[0].after(p)){
                    return;
                }

                for (int i = 0; i < n; ++i){
                    Date d = quotes.get(i).getDate();
                    if (d.after(date[0])){
                        forecastTimeSeries.addOrUpdate(new Minute(d), forecast[i]);
                    }
                }

                for (int i = 0; i < ao.getVectorForecast().getM(); ++i){
                    Date d = DateUtil.getOneMinuteIndexDate(p, i+1);
                    forecastTimeSeries.addOrUpdate(new Minute(d), forecast[n+i]);
                }
            }
        });

        //executor
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

        executor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    //Quotes
                    List<Quote> quotes = quotesBean.getQuotes("GAZP", size);

                    for (int i = 0; i < size; ++i) {
                        Quote q = quotes.get(i);

                        date[i] = q.getDate();
                        open[i] = q.getOpen();
                        low[i] = q.getLow();
                        high[i] = q.getHigh();
                        close[i] = q.getClose();
                        volume[i] = q.getVolume();
                    }

                    //todo improve object creation
                    chart.getXYPlot().setDataset(new DefaultHighLowDataset("", date, high, low, open, close, volume));

                    //Prediction
                    for (AlphaOracle ao : alphaOracles) {
                        TimeSeries timeSeriesLong = predictionPoint.getSeries("long" + ao.getId());
                        timeSeriesLong.clear();

                        TimeSeries timeSeriesShort = predictionPoint.getSeries("short" + ao.getId());
                        timeSeriesShort.clear();

                        for (AlphaOracleData d : alphaOracleBean.getAlphaOracleDatas(ao.getId(), date[0])){
                            if (d.getPrediction().equals(AlphaOracleData.PREDICTION.LONG)){
                                timeSeriesLong.addOrUpdate(new Minute(d.getDate()), d.getPrice());
                            }else if (d.getPrediction().equals(AlphaOracleData.PREDICTION.SHORT)){
                                timeSeriesShort.addOrUpdate(new Minute(d.getDate()), d.getPrice());
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("Ошибка рисования графика", e);
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
}

