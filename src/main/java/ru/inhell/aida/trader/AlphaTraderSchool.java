package ru.inhell.aida.trader;

import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.*;
import ru.inhell.aida.acml.ACML;
import ru.inhell.aida.cula.CULA;
import ru.inhell.aida.entity.*;
import ru.inhell.aida.inject.AidaInjector;
import ru.inhell.aida.oracle.AlphaOracleBean;
import ru.inhell.aida.oracle.AlphaOracleSchoolBean;
import ru.inhell.aida.oracle.AlphaOracleService;
import ru.inhell.aida.quik.TransactionBean;
import ru.inhell.aida.quotes.QuotesBean;
import ru.inhell.aida.ssa.RemoteVSSAException;
import ru.inhell.aida.ssa.VectorForecastSSA;
import ru.inhell.aida.ssa.VectorForecastSSAService;
import ru.inhell.aida.util.DateUtil;
import ru.inhell.aida.util.QuoteUtil;
import ru.inhell.aida.util.VectorForecastUtil;

import javax.swing.*;
import java.awt.*;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Array;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Anatoly A. Ivanov java@inhell.ru
 *         Date: 10.04.11 19:38
 */
public class AlphaTraderSchool {
    public static final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT);

    private final static int COUNT = 10000;
    private final static int THREAD = 4;

    static FileWriter fileWriter;

    public static void main(String... args) throws Exception{
//        fileWriter = new FileWriter("/home/anatoly/tools/aos.txt", true);
        fileWriter = new FileWriter("e:\\aida\\aos.txt", true);

//                scoreAll();

        initGUI();

//        AidaInjector.getInstance(AlphaOracleService.class).predict(new AlphaOracle(38L), 720*12, false, false);

//        score(AidaInjector.getInstance(AlphaOracleBean.class).getAlphaOracle(38L));

//        study("", "GZM1", 1005, 60, 37, 10, true, true, 1.001f);


//        for (int i = 0; i < 10000; i+=20) {
//            study("", "GZM1", 100 + i, 10, 2, 5, true, true, 1.002f);
//        }

//        transaction("GZM1", "03.06.2011");
//        transaction("GZM1", "31.05.2011");
//        transaction("GZM1", "01.06.2011");

//        study2("", "GAZP", "GAZP", 1562, 29, 6, new int[0], 5, true, true, 1.004f, false, 0);

//        study2(0, "", "GZM1", "GZM1", 5, 2, 1, new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8,9,10}, 2, true, true, 1.002f, false, 0, false, false, 5, 5, 1);




//        randomStudyAll();

//        fullSearch();

        for (int p = 2; p < 64; p++) {
            study2(0, "", "GZM1", "GZM1", 10240, 64, p, null, 5 + 1, true, true, 1.0012f, false, 3, false, false, 0, 5, 5, 0);
        }

//        benchmark();

//        session();

//        studyAll();

//        AidaInjector.getInstance(AlphaOracleService.class).predict(new AlphaOracle(38L), 720, false, false);
    }

    private static void benchmark() {
        int n = 10240;
        int l = 512;
        int p = 32;

        int m = 10;

        long time = System.currentTimeMillis();

        study2(0, "", "SBER03", "SBER03", n, l, p, null, m + 1, true, true, 1.0012f, false, 3, true, false, 0, 5, m, 0);

        System.out.println("0: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        study2(0, "", "SBER03", "SBER03", n, l, p, null, m + 1, true, true, 1.0012f, false, 3, true, false, 0, 5, m, 1);
        System.out.println("1: " + (System.currentTimeMillis() - time));

        time = System.currentTimeMillis();
        study2(0, "", "SBER03", "SBER03", n, l, p, null, m + 1, true, true, 1.0012f, false, 3, true, false, 0, 5, m, 2);
        System.out.println("2: " + (System.currentTimeMillis() - time));
    }

    private static void studyAll(){
        List<AlphaTrader> alphaTraders = AidaInjector.getInstance(AlphaTraderBean.class).getAlphaTraders();

        for (AlphaTrader alphaTrader : alphaTraders){
            VectorForecast vf = alphaTrader.getAlphaOracle().getVectorForecast();

            if (vf.getSymbol().equals("GAZP")) {
                study2(0, vf.getId() + "", "GZM1", "GZM1", vf.getN(), vf.getL(), vf.getP(), new int[0], vf.getM(), true, true,
                        alphaTrader.getAlphaOracle().getStopFactor(), false,1, false, false, 0, 10, vf.getM(), 0);
            }
        }
    }

    private static void session(){
        AlphaOracleSchoolBean alphaOracleSchoolBean = AidaInjector.getInstance(AlphaOracleSchoolBean.class);

        List<AlphaOracleSchool> alphaOracleSchools = alphaOracleSchoolBean.getAlphaOracleSchools();

        for (AlphaOracleSchool aos : alphaOracleSchools){
            study2(0, "", "GZM1", "GZM1", aos.getN(), aos.getL(), aos.getP(), new int[0], aos.getM(), aos.isAverage(), true,
                    aos.getStopFactor(), false, 1, false, false, 0, 10, aos.getM(), 0);
        }
    }


    static TimeSeriesCollection balanceDataSet = new TimeSeriesCollection();
    static TimeSeriesCollection forecastDataSet = new TimeSeriesCollection();
    static JLabel[] label = new JLabel[THREAD];

    private static void initGUI(){
        JFrame frame = new JFrame("Alpha Oracle School");

        JFreeChart chart = ChartFactory.createTimeSeriesChart("Alpha Oracle School", "time", "balance", balanceDataSet,
                true, true, false);

        chart.getXYPlot().setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        ((XYLineAndShapeRenderer)chart.getXYPlot().getRenderer()).setBaseShapesVisible(false);

//        SegmentedTimeline timeline = new SegmentedTimeline( SegmentedTimeline.MINUTE_SEGMENT_SIZE, 495, 945);
//        timeline.setStartTime(SegmentedTimeline.firstMondayAfter1900() + 630 * timeline.getSegmentSize());
//        timeline.setBaseTimeline(SegmentedTimeline.newMondayThroughFridayTimeline());
//
//        ((DateAxis)chart.getXYPlot().getDomainAxis()).setTimeline(SegmentedTimeline.newFifteenMinuteTimeline());

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new ChartPanel(chart), BorderLayout.CENTER);

        JPanel panelLabel = new JPanel(new GridLayout(2,3));
        panel.add(panelLabel, BorderLayout.SOUTH);

        for (int i = 0, labelLength = label.length; i < labelLength; i++) {
            label[i] = new JLabel();
            panelLabel.add(label[i]);
        }

        frame.add(panel);

        frame.pack();
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    private static void scoreAll() throws RemoteVSSAException {
        AlphaOracleBean alphaOracleBean = AidaInjector.getInstance(AlphaOracleBean.class);
        final AlphaOracleService alphaOracleService = AidaInjector.getInstance(AlphaOracleService.class);

        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(3);

        List<AlphaOracle> alphaOracles =  alphaOracleBean.getAlphaOracles();
        for (final AlphaOracle alphaOracle : alphaOracles){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (alphaOracle.getVectorForecast().getSymbol().equals("GZM1") && alphaOracle.getId() >= 20
                            && (alphaOracle.getId() == 37 || alphaOracle.getId() == 38)) {
//                        alphaOracleService.predict(alphaOracle, COUNT, true, false);
                        score(alphaOracle);
                    }
                }
            });
        }
    }

    private static void score(AlphaOracle alphaOracle){
        Long alphaOracleId = alphaOracle.getId();

        AlphaOracleBean alphaOracleBean = AidaInjector.getInstance(AlphaOracleBean.class);
        QuotesBean quotesBean = AidaInjector.getInstance(QuotesBean.class);

        List<Quote> quotes = quotesBean.getQuotes(alphaOracle.getVectorForecast().getSymbol(), COUNT);

        List<AlphaOracleData> alphaOracleDataList = alphaOracleBean.getAlphaOracleDatas(alphaOracleId, quotes.get(0).getDate());

        VectorForecast vf = alphaOracle.getVectorForecast();

        String name = vf.getId() + vf.getSymbol() +"-n" + vf.getN() + "l" + vf.getL() +"p" + vf.getP() + "m" + vf.getM() +
                (alphaOracle.getPriceType().equals(PriceType.AVERAGE)?"a":"c");

        TimeSeries balanceTimeSeries = new TimeSeries(name);
        balanceDataSet.addSeries(balanceTimeSeries);

        float balance = 0;
        float price = 0;
        int quantity = 0;

        for (int i=0; i < alphaOracleDataList.size()-1; ++i){
            AlphaOracleData alphaOracleData = alphaOracleDataList.get(i);
            AlphaOracleData alphaOracleDataNext = alphaOracleDataList.get(i+1);

            Date date = alphaOracleData.getDate();

            if (quantity == 0){
                price = alphaOracleData.getPrice();
                balanceTimeSeries.add(new Minute(date), balance);
//                balanceTimeSeries.add(new FixedMillisecond(i), balance);
            }

            switch (alphaOracleData.getPrediction()){
                case LONG:
                    if (quantity == -1){
                        balance += 2*(price - alphaOracleData.getPrice());
                        price = alphaOracleData.getPrice();

                        balanceTimeSeries.add(new Minute(date), balance);
//                        balanceTimeSeries.add(new FixedMillisecond(i), balance);`
                    }

                    quantity = 1;
                    break;
                case SHORT:
                    if (quantity == 1){
                        balance += 2*(alphaOracleData.getPrice() - price);
                        price = alphaOracleData.getPrice();

                        balanceTimeSeries.add(new Minute(date), balance);
//                        balanceTimeSeries.add(new FixedMillisecond(i), balance);
                    }

                    quantity = -1;
                    break;
                case STOP_BUY:
                    if (quantity == -1){
                        balance += (price - alphaOracleData.getPrice());
                        price = alphaOracleData.getPrice();

                        balanceTimeSeries.add(new Minute(date), balance);
//                        balanceTimeSeries.add(new FixedMillisecond(i), balance);

                        quantity = 0;
                    }
                    break;
                case STOP_SELL:
                    if (quantity == 1) {
                        balance += (alphaOracleData.getPrice() - price);
                        price = alphaOracleData.getPrice();

                        balanceTimeSeries.add(new Minute(date), balance);
//                        balanceTimeSeries.add(new FixedMillisecond(i), balance);

                        quantity = 0;
                    }
                    break;
            }


            if (!DateUtil.isSameDay(date, alphaOracleDataNext.getDate())){
                quantity = 0;
            }
        }
    }

    private static void fullSearch(){
        Random random = new Random();

        ExecutorService executor = Executors.newFixedThreadPool(THREAD);

        int index = 0;

        int n = 10240;

//        for (int n = 512; n < 513; n += 10) {
            for (int l = 5; l < n/2; ++l) {
                for (int p = 1; p < l/2; ++p) {
                    for (int mi = 5; mi < 6; mi++) {
                        executor.execute(getFullSearch((++index)%THREAD, n, l, p, mi));
                    }
                }
            }
//        }
    }

    private static Runnable getFullSearch(final int threadNum, final int n, final int l, final int p, final int mi) {
        return new Runnable() {
            Random random = new Random();

            @Override
            public void run() {
                study2(threadNum, "", "SBER03", "SBER03", n, l, p, null, mi + 1, true, true, 1.002f, false, 3, false, false, 0, 5, mi, 0);
            }
        };
    }

    private static void randomStudyAll() throws RemoteVSSAException{
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);

        for (int i=0; i < 1000; ++i){

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        Random random = new Random();

//                        int[] ll = {10, 20, 30, 60};
//                        int nn[] = {720, 720*2, 720*3};
                        int[] mm = {20, 50, 75};
                        float[] ss = {1.0005f, 1.001f, 1.0015f, 1.002f};

                        int[] maa = {10, 20, 30, 50, 100, 200};

                        int n = 24 + random.nextInt(1000);
                        int l = random.nextInt(n/2);
                        int p = random.nextInt(l);
                        int m = 1 + random.nextInt(5);
                        float s = ss[random.nextInt(4)];

                        int ma = 1 + random.nextInt(50);
                        int t = 2 + random.nextInt(4);

                        boolean anti = random.nextBoolean();
                        int d = random.nextInt(4);

                        boolean allP = true;

                        List<Integer> ppList = new ArrayList<Integer>();
                        for (int i = 0; i < p; ++i){
                            if (allP || random.nextBoolean()){
                                ppList.add(i);
                            }
                        }

                        int[] pp = new int[ppList.size()];
                        for (int i = 0; i<ppList.size(); ++i){
                            pp[i] = ppList.get(i);
                        }

                        study2(0, "", random.nextBoolean() ? "GAZP" : "GZM1", "GZM1", n, l, pp.length, pp, m+1, true, true, s, false, 3, false, false, 0, t, m, 0);

//                        study2("", "GAZP", "GAZP", n, l, p, pp, m, true, true, s, anti, d);
//                        if (random.nextBoolean()) study2("", "GZM1", "GZM1", n, l, p, pp, m, true, true, s, anti, d);
//                        if (random.nextBoolean()) study2("", "GAZP", "GZM1", n, l, p, pp, m, true, true, s, anti, d);
//                        if (random.nextBoolean()) study2("", "SBER03", "SRM1", n, l, pp.length, pp, m, true, true, s, anti, d);
//                        if (random.nextBoolean()) study2("", "SRM1", "SRM1", n, l, pp.length, pp, m, true, true, s, anti, d);

                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    static AtomicInteger currentLabel = new AtomicInteger(2);

    private final static boolean timing = false;

    private static final SimpleDateFormat TDF = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

    static int balance = 0;
    static int quantity = 0;

    public static void transaction(String symbol, String date) throws ParseException {
        TimeSeries balanceTimeSeries = new TimeSeries(symbol + "-" + date);
        balanceDataSet.addSeries(balanceTimeSeries);

        List<Transaction> transactions = AidaInjector.getInstance(TransactionBean.class).getTransactions(symbol, date);

        for (Transaction transaction : transactions){
            if ("Купля".equals(transaction.getType())){
                balance -= transaction.getQuantity()*transaction.getPrice();

                quantity += transaction.getQuantity();

                if (quantity == 0) {
                    balanceTimeSeries.addOrUpdate(new Minute(TDF.parse(transaction.getDate() + " " + transaction.getTime())), balance);
                }
            }else if ("Продажа".equals(transaction.getType())){
                balance += transaction.getQuantity()*transaction.getPrice();

                quantity -= transaction.getQuantity();

                if (quantity == 0) {
                    balanceTimeSeries.addOrUpdate(new Minute(TDF.parse(transaction.getDate() + " " + transaction.getTime())), balance);
                }
            }
        }
    }

    static float maxMoney = 0;
    static float minMoney = 0;

    private static void study2(int thread, String prefix, String symbol, String future, int n, int l, int p, int[] pp, int m, boolean average,
                               boolean useStop, float stopFactor, boolean anti, int d, boolean fiveSec, boolean useMA, int ma, int t, int md, int svd)
            throws RemoteVSSAException {
        if (pp == null || pp.length < 1){
            pp = new int[p];
            for (int i=0; i<p; ++i){
                pp[i] = i;
            }
        }

        currentLabel.incrementAndGet();

        QuotesBean quotesBean = AidaInjector.getInstance(QuotesBean.class);

        float money = 0;
        float balance = 0;
        float stopPrice = 0;
        int quantity = 0;

        int orderCount = 0;
        int stopCount = 0;

        int f_size = n + l + m -1;

        boolean closeDay = false;

        Date start = null, end = null;

        List<Quote> allQuotes = fiveSec ? quotesBean.getQuotes5Sec(symbol, COUNT + n) : quotesBean.getQuotes(symbol, COUNT + n);
        List<Quote> allFutureQuotes = fiveSec ? quotesBean.getQuotes5Sec(future, COUNT + n) : quotesBean.getQuotes(future, COUNT + n);

        String name = symbol + "-" + future +"-n" + n + "l" + l +"p" + p + "m" + m + (average?"a":"c") + (useStop?"s":"")
                + stopFactor + (anti?"A":"") + "d" + d + "ma" + ma + "t" + t + "md" + md + (fiveSec?"five":"");

        TimeSeries balanceTimeSeries = new TimeSeries(name);
        balanceDataSet.addSeries(balanceTimeSeries);

        Calendar current = Calendar.getInstance();

        float[] allforecast = new float[f_size*COUNT];
        float[] allprices = average ? QuoteUtil.getAveragePrices(allQuotes) : QuoteUtil.getClosePrices(allQuotes);

        if (svd == 0) {
            CULA.jni().vssa(n, l, p, pp, m, allprices, allforecast, COUNT);
        }else if (svd == 1){
            float[] forecast = new float[f_size];
            float[] timeseries = new float[n];

            for (int i = 0; i < COUNT; ++i){
                System.arraycopy(allprices, i, timeseries, 0, n);

                ACML.jni().vssa(n, l, p, pp, m, timeseries, forecast, 0);

                System.arraycopy(forecast, 0, allforecast, i*f_size, f_size);
            }
        }else{
            float[] forecast = new float[f_size];
            float[] timeseries = new float[n];
            VectorForecastSSA vectorForecastSSA = new VectorForecastSSA(n, l, p, pp, m);

            for (int i = 0; i < COUNT; ++i){
                System.arraycopy(allprices, i, timeseries, 0, n);

                vectorForecastSSA.execute(timeseries, forecast);

                System.arraycopy(forecast, 0, allforecast, i*f_size, f_size);
            }
        }

        long time;

        for (int index=0; index < COUNT-1; ++index) {
            if (timing) time = System.currentTimeMillis();

            List<Quote> quotes = allQuotes.subList(index, n + index);

            if (timing){
                System.out.println("AlphaTraderSchool.1 " + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();
            }

            float[] prices = average ? QuoteUtil.getAveragePrices(quotes) : QuoteUtil.getClosePrices(quotes);

            if (timing){
                System.out.println("AlphaTraderSchool.2 " + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();
            }


            Quote currentQuote = allQuotes.get(n + index);
            float currentPrice = currentQuote.getOpen();
            Date currentDate = currentQuote.getDate();

            float currentFuturePrice = allFutureQuotes.get(n+index).getOpen();

            if (index == 0){
                start = currentDate;
            }else if (index == COUNT - 2){
                end = currentDate;
            }

//            if ((i > 720*720 && money < 0)){
//                try {
//                    fileWriter.append(name).append(",,,,,").append("\n");
//                    balanceDataSet.removeSeries(balanceTimeSeries);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

//                if (!anti) {
//                    study2(prefix, symbol, future, n, l, p, pp, m , average, useStop, stopFactor, !anti, d, fiveSec, useMA, ma, t, md);
//                }

//                return;
//            }


            if ((index > 5000 && orderCount < 2)){
//                try {
//                    fileWriter.append(name).append(",,,,,").append("\n");
                    balanceDataSet.removeSeries(balanceTimeSeries);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }

                return;
            }

            //close day
            current.setTime(currentDate);
            if ((current.get(Calendar.HOUR_OF_DAY) == (symbol.equals(future) ? 23 : 18)
                    && current.get(Calendar.MINUTE) > 43) || index == COUNT - 2){

                closeDay = true;
            }

            if (current.get(Calendar.HOUR_OF_DAY) == 10){
                closeDay = false;
            }

            //stop
            if ((useStop && stopPrice != 0) || closeDay) {
                if (quantity > 0 && (currentPrice < stopPrice || closeDay || currentPrice > stopPrice*Math.pow(stopFactor, t))){ //stop sell
                    balance +=  currentFuturePrice;

                    stopPrice = 0;
                    quantity = 0;
                    stopCount++;

                    money = balance - 1.5f;
                    balanceTimeSeries.add(new Second(currentDate), money);
//                    System.out.println("STOP_SELL, " + currentFuturePrice + ", " + dateFormat.format(currentDate) + ", " + money);

                    continue;
                }else if (quantity < 0 && (currentPrice > stopPrice || closeDay || currentPrice < stopPrice/Math.pow(stopFactor, t))){ //stop buy
                    balance -= currentFuturePrice;

                    stopPrice = 0;
                    quantity = 0;
                    stopCount++;
                    money = balance - 1.5f;
                    balanceTimeSeries.add(new Second(currentDate), money);
//                    System.out.println("STOP_BUY, " + currentFuturePrice + ", " + dateFormat.format(currentDate) + ", " + money);

                    continue;
                }
            }

            if (timing){
                System.out.println("AlphaTraderSchool.3 " + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();
            }

            if (timing){
                System.out.println("AlphaTraderSchool.4 " + (System.currentTimeMillis() - time));
                time = System.currentTimeMillis();
            }

            if (!closeDay && anti ? isMax(allforecast, n + f_size*index, md, d) : isMin(allforecast, n + f_size*index, md, d)
                    && (!useMA || currentPrice < getMA(prices, ma))){ //LONG
                if (quantity == 0){
//                    System.out.println("LONG, " + currentFuturePrice + ", " + dateFormat.format(currentDate) + ", " + money);
                    balanceTimeSeries.add(new Second(currentDate), money);

                    stopPrice = currentPrice/stopFactor;

                    balance -= currentFuturePrice;
                    money -= 1.5;

                    orderCount++;
                    quantity = 1;
                }

                 if (quantity == -100){
                    balance -= currentFuturePrice;

                    orderCount++;
                    quantity = 0;

                    money = balance - 1.5f;
                    balanceTimeSeries.add(new Second(currentDate), money);
//                    System.out.println("LONG, " + currentFuturePrice + ", " + dateFormat.format(currentDate) + ", " + money);
                }

                if (quantity == -1){
                    balance -= 2*currentFuturePrice;

                    stopPrice = currentPrice/stopFactor;

                    orderCount++;
                    quantity = 1;

                    money = balance + currentFuturePrice - 3;
                    balanceTimeSeries.add(new Second(currentDate), money);
//                    System.out.println("LONG, " + currentFuturePrice + ", " + dateFormat.format(currentDate) + ", " + money);
                }
            }else if (!closeDay &&  (anti ? isMin(allforecast, n + f_size*index, md, d) : isMax(allforecast, n + f_size*index, md, d))
                    && (!useMA || currentPrice > getMA(prices, ma))){ //SHORT
                if (quantity == 0){
                    balanceTimeSeries.add(new Second(currentDate), money);
//                    System.out.println("SHORT, " + currentFuturePrice + ", " + dateFormat.format(currentDate) + ", " + money);

                    stopPrice = currentPrice/stopFactor;

                    balance += currentFuturePrice;
                    money -= 1.5;

                    orderCount++;
                    quantity = -1;
                }

                if (quantity == 100){
                    balance += currentFuturePrice;

                    orderCount++;
                    quantity = 0;

                    money = balance - 1.5f;
                    balanceTimeSeries.add(new Second(currentDate), money);
//                    System.out.println("SHORT, " + currentFuturePrice + ", " + dateFormat.format(currentDate) + ", " + money);
                }

                if (quantity == 1){
                    balance += 2*currentFuturePrice;

                    stopPrice = currentPrice*stopFactor;

                    orderCount++;
                    quantity = -1;

                    money = balance - currentFuturePrice - 3;
                    balanceTimeSeries.add(new Second(currentDate), money);
//                    System.out.println("SHORT, " + currentFuturePrice + ", " + dateFormat.format(currentDate) + ", " + money);
                }
            }

            String s = index + ": " + prefix+balanceTimeSeries.getKey() + ", " + money + ", " + orderCount + ", " + stopCount;
            label[thread].setText(s);

            if (timing){
                System.out.println("AlphaTraderSchool.5 " + (System.currentTimeMillis() - time));
            }

        }

//        if (money < maxMoney){
//            balanceDataSet.removeSeries(balanceTimeSeries);
//        }
//
//        if (money > maxMoney){
//            maxMoney = money;
//            try {
//                TimeSeries ts = (TimeSeries) balanceTimeSeries.clone();
//                balanceDataSet.removeAllSeries();
//                balanceDataSet.addSeries(ts);
//            } catch (CloneNotSupportedException e) {
//                e.printStackTrace();
//            }
//        } else if (money < minMoney){
//            minMoney = money;
//        }

        String s = prefix+balanceTimeSeries.getKey() + "," + money + "," + orderCount + "," + stopCount + "," + dateFormat.format(start) + "," + dateFormat.format(end);

        try {
            fileWriter.append(s).append("\n");
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


        System.out.println(s);
    }

    private static boolean isMin(float[] forecast, int n, int m, int d){
        switch (d){
            case 0: return VectorForecastUtil.isMin(forecast, n, m);
            case 1: return VectorForecastUtil.isMin(forecast, n + 1, m);
            case 2: return VectorForecastUtil.isMin(forecast, n - 1, m);
            case 3: return VectorForecastUtil.isMin(forecast, n, m)
                    || VectorForecastUtil.isMin(forecast, n - 1, m)
                    || VectorForecastUtil.isMin(forecast, n + 1, m);
        }

        throw new IllegalArgumentException();
    }

    private static boolean isMax(float[] forecast, int n, int m, int d){
        switch (d){
            case 0: return VectorForecastUtil.isMax(forecast, n, m);
            case 1: return VectorForecastUtil.isMax(forecast, n + 1, m);
            case 2: return VectorForecastUtil.isMax(forecast, n - 1, m);
            case 3: return VectorForecastUtil.isMax(forecast, n, m)
                || VectorForecastUtil.isMax(forecast, n - 1, m)
                || VectorForecastUtil.isMax(forecast, n + 1, m);
        }

        throw new IllegalArgumentException();
    }

    private static float getMA(float[] prices, int len){
        float sum = 0;

        for (int i=1; i <= len; ++i){
            sum += prices[prices.length - i];
        }

        return sum/len;
    }
}

