package ru.inheaven.aida.backtest;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ru.inheaven.aida.happy.trading.entity.Order;
import ru.inheaven.aida.happy.trading.entity.Trade;
import ru.inheaven.aida.happy.trading.mapper.TradeMapper;
import ru.inheaven.aida.happy.trading.service.Module;
import ru.inheaven.aida.happy.trading.util.OrderDoubleMap;
import ru.inhell.aida.algo.arima.ArimaFitter;
import ru.inhell.aida.algo.arima.DefaultArimaForecaster;

import javax.swing.*;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static java.math.BigDecimal.ZERO;
import static java.math.BigDecimal.valueOf;
import static java.math.RoundingMode.HALF_EVEN;
import static ru.inheaven.aida.happy.trading.entity.OrderType.ASK;
import static ru.inheaven.aida.happy.trading.entity.OrderType.BID;

/**
 * inheaven on 04.05.2016.
 */
public class LevelBacktest {
    private class Metric{
        private double price;
        private double total;
        private Date date;

        Metric(double price, double total, Date date) {
            this.price = price;
            this.total = total;
            this.date = date;
        }
    }

    public static void main(String[] args){
        TradeMapper tradeMapper = Module.getInjector().getInstance(TradeMapper.class);

        Date startDate = Date.from(LocalDateTime.of(2016, 5, 9, 20, 0, 0).atZone(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(LocalDateTime.of(2016, 5, 10 , 20, 0, 0).atZone(ZoneId.systemDefault()).toInstant());

        List<LevelBacktest> levelBacktestList = new ArrayList<>();

        System.out.println("LevelBacktest startDate: " + startDate + ", endDate: " + endDate + "\n");

        for (int i = 0; i <= 0; ++i){
            for (int j = 1; j <= 10; ++j) {
                for (int k = 0; k <= 0; ++k) {
                    for (int l = 0; l <= 10; ++l) {
                        levelBacktestList.add(new LevelBacktest(28260, 60000, 20, 90000, 8.45, 0.022, 0, 59000, 2, 1000, 300000, true, j, 1, l, 100000, 1, 2));
                    }
                }
            }
        }

        //base
        levelBacktestList.add(new LevelBacktest(28260, 60000, 20, 90000, 8.45, 0.022, 0, 59000, 2, 1000, 300000, true, 3, 1, 2, 100000, 1, 0));

        //amount level
//        for (int l = 1; l <= 10; ++l) {
//            levelBacktestList.add(new LevelBacktest(17000, 60000, 5, 10000 + 1000*l, Math.sqrt(2 * Math.PI), 0.01, 0, 60000, 2, 1000, 60000));
//        }

        List<Trade> trades = tradeMapper.getLightTrades("BTC/CNY", startDate, endDate, 0, 10000000);

        System.out.println("trade size: " + trades.size() + "\n");

        ExecutorService executorService = Executors.newFixedThreadPool(2);

        List<Future> futures = new ArrayList<>();

        DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));

        levelBacktestList.forEach(l -> {
            Future future = executorService.submit(() -> {
                try {
                    long time = System.currentTimeMillis();

                    //action
                    trades.forEach(l::action);

                    //clear
                    l.arimaPriceQueue.clear();
                    l.tradePriceQueue.clear();
                    l.standardDeviation = null;
                    l.metrics.clear();

                    System.out.println("" +
                            new Date() + " " +
                            (System.currentTimeMillis() - time)/1000 + "s " +
                            l.getStringRow());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });

            futures.add(future);
        });

        futures.forEach(f -> {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        levelBacktestList.sort((l1, l2) -> Double.compare(l2.total, l1.total));

        System.out.println("\n Rank");

        levelBacktestList.forEach(l -> {
            System.out.println(l.getStringRow());
        });

        //chart
        XYSeries priceSeries = new XYSeries("price");
        XYSeries totalSeries = new XYSeries("total");

        levelBacktestList.get(0).metrics.forEach(m -> {
            priceSeries.add(m.date.getTime(), m.price);
            totalSeries.add(m.date.getTime(), m.total);
        });

        XYPlot plot = new XYPlot();
        XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer(true, false);
        XYLineAndShapeRenderer renderer2 = new XYLineAndShapeRenderer(true, false);

        //y1
        plot.setDataset(0, new XYSeriesCollection(priceSeries));
        plot.setRenderer(0, renderer1);
        DateAxis domainAxis = new DateAxis("Date");
        plot.setDomainAxis(domainAxis);

        NumberAxis y1 = new NumberAxis("price");
        y1.setAutoRange(true);
        y1.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(y1);

        //y2
        plot.setDataset(1, new XYSeriesCollection(totalSeries));
        plot.setRenderer(1, renderer2);

        NumberAxis y2 = new NumberAxis("total");
        y2.setAutoRange(true);
        y2.setAutoRangeIncludesZero(false);
        plot.setRangeAxis(1, y2);

        plot.mapDatasetToRangeAxis(0, 0);
        plot.mapDatasetToRangeAxis(1, 1);

        JFreeChart chart = new JFreeChart("LevelBacktest", plot);

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.add(new ChartPanel(chart));
        frame.pack();
        frame.setVisible(true);
    }

    private double initialSubtotalCny;
    private double subtotalCny;
    private double subtotalBtc = 0d;
    private long cancelDelay;
    private double cancelRange;
    private int tradeSize;
    private double spreadDiv;
    private double amountLevel;
    private int amountRange;
    private int balanceDelay;
    private double balanceValue;
    private long tradeDelay;
    private long metricDelay;
    private boolean slip;

    private int p, d, q, arimaSize, arimaNext, arimaFilter;

    private OrderDoubleMap orderMap = new OrderDoubleMap();

    private Deque<Metric> metrics = new LinkedList<>();
    private double total = 0d;

    private double buyPrice = 0d;
    private double buyVolume = 0d;

    private double sellPrice = 0d;
    private double sellVolume = 0d;

    private LevelBacktest(double subtotalCny, long cancelDelay, double cancelRange, int tradeSize,
                          double spreadDiv, double amountLevel, int amountRange, int balanceDelay, double balanceValue,
                          long tradeDelay, long metricDelay, boolean slip, int p, int d, int q, int arimaSize, int arimaNext, int arimaFilter) {
        this.initialSubtotalCny = subtotalCny;

        this.subtotalCny = subtotalCny;
        this.cancelDelay = cancelDelay;
        this.cancelRange = cancelRange;
        this.tradeSize = tradeSize;
        this.spreadDiv = spreadDiv;
        this.amountLevel = amountLevel;
        this.amountRange = amountRange;
        this.balanceDelay = balanceDelay;
        this.balanceValue = balanceValue;
        this.tradeDelay = tradeDelay;
        this.metricDelay = metricDelay;
        this.slip = slip;

        this.p = p;
        this.d = d;
        this.q = q;
        this.arimaSize = arimaSize;
        this.arimaNext = arimaNext;
        this.arimaFilter = arimaFilter;
    }

    private Double getFreeBtc(){
        return subtotalBtc - (orderMap.getAskMap().values().stream()
                .flatMap(m -> m.values().stream())
                .mapToDouble(o -> o.getAmount().doubleValue())
                .sum());
    }

    private Double getFreeCny(){
        return subtotalCny - (orderMap.getBidMap().values().stream()
                .flatMap(m -> m.values().stream())
                .mapToDouble(o -> o.getAmount().multiply(o.getPrice()).doubleValue())
                .sum());
    }

    private StandardDeviation standardDeviation = new StandardDeviation(true);

    private double stdDev;
    private long lastStdDevTime = 0;

    private double getStdDev(Trade trade){
        if (lastStdDevTime == 0 || trade.getCreated().getTime() - lastStdDevTime > 60000){
            stdDev = standardDeviation.evaluate(tradePriceQueue.stream().mapToDouble(d -> d).toArray());

            lastStdDevTime = trade.getCreated().getTime();
        }

        return !Double.isNaN(stdDev) ? stdDev : 0.1;
    }

    private double getSpread(Trade trade){
        double stdDev = getStdDev(trade);

        return  stdDev / spreadDiv;
    }

    private long lastBalanceTime = 0;
    private boolean balance;

    private boolean isBalance(Trade trade){
        if (subtotalBtc == 0){
            return true;
        }

        if (lastBalanceTime == 0 || trade.getCreated().getTime() - lastBalanceTime > balanceDelay){
            double price = trade.getPrice().doubleValue();

            if (arimaSize > 0 && arimaPriceQueue.size() >= arimaSize) {
                double[] prices;
                                                
                if (arimaFilter == 1){
                    prices = arimaPriceQueue.stream().mapToDouble(Math::log).toArray();                                        
                } else if (arimaFilter == 2){
                    prices = arimaPriceQueue.stream().mapToDouble(d -> d).toArray();
                    
                    double[] pricesDelta = new double[prices.length - 1];
                    
                    for (int i = 0; i < prices.length - 1; ++i){
                        pricesDelta[i] = prices[i+1]/prices[i] - 1;                        
                    }
                    
                    prices = pricesDelta;                    
                } else{
                    prices = arimaPriceQueue.stream().mapToDouble(d -> d).toArray();                    
                }                
                
                price = new DefaultArimaForecaster(ArimaFitter.fit(prices, p, d, q), prices).next(arimaNext)[arimaNext -1];
                
                if (arimaFilter == 1){
                    price = Math.exp(price);                    
                }else if (arimaFilter == 2){
                    price = prices[prices.length -1] * (price + 1);                     
                }
            }

            balance = subtotalCny / (subtotalBtc * price) > balanceValue;

            lastBalanceTime = trade.getCreated().getTime();
        }

        return balance;
    }

    private void order(Order order){
        if (order.getType().equals(BID) && getFreeCny() > order.getAmount().multiply(order.getPrice()).doubleValue()){
            orderMap.put(order);
        }

        if (order.getType().equals(ASK) && getFreeBtc() > order.getAmount().doubleValue()){
            orderMap.put(order);
        }
    }

    private Deque<Double> tradePriceQueue = new LinkedList<>();
    private Deque<Double> arimaPriceQueue = new LinkedList<>();

    private long lastTradeTime = 0;
    private double lastTradePrice = 0;

    private void trade(Trade trade){
        double price = trade.getPrice().doubleValue();

        //trade queue
        tradePriceQueue.add(price);
        if (tradePriceQueue.size() > tradeSize){
            tradePriceQueue.removeFirst();
        }

        if (lastTradePrice != price){
            arimaPriceQueue.add(price);
            if (arimaPriceQueue.size() > arimaSize){
                arimaPriceQueue.removeFirst();
            }
        }

        //trade
        if (lastTradeTime > 0 && (trade.getCreated().getTime() - lastTradeTime < tradeDelay)){
            return;
        }

        orderMap.getBidMap().tailMap(price).forEach((k, v) -> v.values().forEach(o -> {
            subtotalBtc += o.getAmount().doubleValue();
            subtotalCny -= o.getAmount().multiply(o.getPrice()).doubleValue();

            orderMap.remove(o.getOrderId());

            double bv = buyVolume;
            buyVolume = o.getAmount().doubleValue() + bv;
            buyPrice = (buyPrice * bv  + o.getPrice().multiply(o.getAmount()).doubleValue()) / (o.getAmount().doubleValue() + bv);
        }));

        orderMap.getAskMap().headMap(price).forEach((k, v) -> v.values().forEach(o -> {
            subtotalBtc -= o.getAmount().doubleValue();
            subtotalCny += o.getAmount().multiply(o.getPrice()).doubleValue();

            orderMap.remove(o.getOrderId());

            double sv = sellVolume;
            sellVolume= o.getAmount().doubleValue() + sv;
            sellPrice = (sellPrice * sv  + o.getPrice().multiply(o.getAmount()).doubleValue()) / (o.getAmount().doubleValue() + sv);
        }));

        lastTradeTime = trade.getCreated().getTime();
        lastTradePrice = price;
    }

    private long lastCancelTime = 0;

    private void cancel(Trade trade){
        if (lastCancelTime > 0 && (trade.getCreated().getTime() - lastCancelTime < cancelDelay)){
            return;
        }

        double range = getSpread(trade) * cancelRange;

        orderMap.getBidMap().headMap(trade.getPrice().doubleValue() - range)
                .forEach((k, v) -> v.values().forEach(o -> orderMap.remove(o.getOrderId())));

        orderMap.getAskMap().tailMap(trade.getPrice().doubleValue() + range)
                .forEach((k, v) -> v.values().forEach(o -> orderMap.remove(o.getOrderId())));

        lastCancelTime = trade.getCreated().getTime();
    }

    private long lastMetricTime = 0;

    private void metric(Trade trade){
        if (lastMetricTime > 0 && trade.getCreated().getTime() - lastMetricTime < metricDelay){
            return;
        }

        total = (subtotalBtc * trade.getPrice().doubleValue()) + subtotalCny;
        metrics.add(new Metric(trade.getPrice().doubleValue(), total, trade.getCreated()));

        lastMetricTime = trade.getCreated().getTime();
    }

    private SecureRandom random = new SecureRandom();
    private long idGen = 0;

    private double lastBuyPrice = 0;
    private double lastSellPrice = 0;

    private BigDecimal lastActionPrice = ZERO;

//    private QuranRandom quranRandom = new QuranRandom();

    private void action(Trade trade){
        if (lastActionPrice.equals(trade.getPrice())){
            return;
        }

        boolean up = isBalance(trade);
        double spread = getSpread(trade);

        double p = trade.getPrice().doubleValue();
        double buyPrice = up ? p : p - spread;
        double sellPrice = up ? p + spread : p;

        if (!orderMap.contains(buyPrice, spread, BID) && !orderMap.contains(sellPrice, spread, ASK)){
            double buyAmount;
            double sellAmount;

            if (amountLevel == 0 ){
                //amount range
                double amount = (subtotalCny / p  + subtotalBtc) / (spreadDiv * amountRange);
                buyAmount = (up ? 2 : 1) * amount;
                sellAmount = (up ? 1 : 2) * amount;
            }else{
                //amount level
                buyAmount = (up ? random.nextGaussian()/2 + 2 : random.nextGaussian()/2 + 1) * amountLevel;
                sellAmount = (up ? random.nextGaussian()/2 + 1 : random.nextGaussian()/2 + 2) * amountLevel;
            }

            //slip
            if (slip) {
                if (lastBuyPrice > 0 && buyPrice < lastBuyPrice){
                    buyAmount = buyAmount * Math.abs(lastBuyPrice - buyPrice) / spread;
                }
                if (lastSellPrice > 0 && sellPrice > lastSellPrice){
                    sellAmount = sellAmount * Math.abs(sellPrice - lastSellPrice) / spread;
                }
            }

            lastBuyPrice = buyPrice;
            lastSellPrice = sellPrice;

            //less
            if (buyAmount < 0.01){
                buyAmount = 0.01;
            }
            if (sellAmount < 0.01){
                sellAmount = 0.01;
            }

            //order
            order(new Order(String.valueOf(++idGen), BID, valueOf(buyPrice), valueOf(buyAmount).setScale(3, HALF_EVEN)));
            order(new Order(String.valueOf(++idGen), ASK, valueOf(sellPrice), valueOf(sellAmount).setScale(3, HALF_EVEN)));

            metric(trade);
        }

        trade(trade);
        cancel(trade);

        lastActionPrice = trade.getPrice();
    }

    private Double getProfit(double price) {
        return (Math.min(sellVolume, buyVolume) * (sellPrice - buyPrice)) +
               (Math.abs(buyVolume - sellVolume) * (buyVolume > sellVolume ? price - buyPrice : sellPrice - price));
    }

    @Override
    public String toString() {
        return "LevelBacktest{" +
                "subtotalCny=" + subtotalCny +
                ", subtotalBtc=" + subtotalBtc +
                ", cancelDelay=" + cancelDelay +
                ", cancelRange=" + cancelRange +
                ", tradeSize=" + tradeSize +
                ", spreadDiv=" + spreadDiv +
                ", amountLevel=" + amountLevel +
                ", amountRange=" + amountRange +
                ", balanceDelay=" + balanceDelay +
                ", balanceValue=" + balanceValue +
                ", tradeDelay=" + tradeDelay +
                ", metricDelay=" + metricDelay +
                ", buyPrice=" + buyPrice +
                ", buyVolume=" + buyVolume +
                ", sellPrice=" + sellPrice +
                ", sellVolume=" + sellVolume +
                ", stdDev=" + stdDev +
                ", lastStdDevTime=" + lastStdDevTime +
                ", lastBalanceTime=" + lastBalanceTime +
                ", balance=" + balance +
                ", lastTradeTime=" + lastTradeTime +
                ", lastCancelTime=" + lastCancelTime +
                ", lastMetricTime=" + lastMetricTime +
                ", lastBuyPrice=" + lastBuyPrice +
                ", lastSellPrice=" + lastSellPrice +
                ", lastActionPrice=" + lastActionPrice +
                '}';
    }
    
    private String getStringRow(){
        DecimalFormat df = new DecimalFormat("0.00", DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        
        return  df.format(subtotalBtc * lastTradePrice + subtotalCny - initialSubtotalCny) + " " +
                df.format(getProfit(lastTradePrice)) + " " +
                df.format(subtotalCny) + " " +
                df.format(subtotalBtc) + " " +
                cancelDelay + " " +
                cancelRange + " " +
                tradeSize + " " +
                spreadDiv + " " +
                amountLevel + " " +
                amountRange + " " +
                balanceDelay + " " +
                balanceValue + " " +
                tradeDelay + " " +
                metricDelay + " " +
                df.format(buyVolume) + " " +
                df.format(sellVolume) + " " +
                slip + " " +
                p + " " +
                d + " " +
                q + " " +
                arimaSize + " " +
                arimaNext + " " +
                arimaFilter;
    }
}


