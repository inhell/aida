package ru.inheaven.aida.happy.trading.strategy;

import com.google.common.util.concurrent.AtomicDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inheaven.aida.happy.trading.entity.*;
import ru.inheaven.aida.happy.trading.mapper.OrderMapper;
import ru.inheaven.aida.happy.trading.service.*;
import ru.inhell.aida.algo.arima.ArimaFitter;
import ru.inhell.aida.algo.arima.DefaultArimaForecaster;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Deque;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.math.BigDecimal.*;
import static java.math.RoundingMode.HALF_EVEN;
import static java.math.RoundingMode.HALF_UP;
import static ru.inheaven.aida.happy.trading.entity.OrderStatus.*;
import static ru.inheaven.aida.happy.trading.entity.OrderType.ASK;
import static ru.inheaven.aida.happy.trading.entity.OrderType.BID;

/**
 * @author inheaven on 08.07.2015 19:39.
 */
public class LevelStrategy extends BaseStrategy{
    private Logger log = LoggerFactory.getLogger(getClass());

    private Strategy strategy;
    private BigDecimal risk = ONE;

    private UserInfoService userInfoService;
    private TradeService tradeService;
    private OrderService orderService;

    private SecureRandom random = new SecureRandom();

    private static AtomicLong positionIdGen = new AtomicLong(System.nanoTime());

    private final static BigDecimal BD_0_1 = new BigDecimal("0.1");
    private final static BigDecimal BD_0_5 = new BigDecimal("0.5");
    private final static BigDecimal BD_0_8 = new BigDecimal("0.8");
    private final static BigDecimal BD_0_25 = new BigDecimal("0.25");
    private final static BigDecimal BD_0_33 = new BigDecimal("0.33");
    private final static BigDecimal BD_0_66 = new BigDecimal("0.66");
    private final static BigDecimal BD_0_01 = new BigDecimal("0.01");
    private final static BigDecimal BD_0_05 = new BigDecimal("0.05");
    private final static BigDecimal BD_0_001 = new BigDecimal("0.001");
    private final static BigDecimal BD_0_002 = new BigDecimal("0.002");
    private final static BigDecimal BD_1_1 = new BigDecimal("1.1");
    private final static BigDecimal BD_1_2 = new BigDecimal("1.2");
    private final static BigDecimal BD_1_3 = new BigDecimal("1.3");

    private final static BigDecimal BD_2 = new BigDecimal(2);
    private final static BigDecimal BD_2_5 = new BigDecimal("2.5");
    private final static BigDecimal BD_3 = new BigDecimal(3);
    private final static BigDecimal BD_3_5 = new BigDecimal("3.5");
    private final static BigDecimal BD_4 = new BigDecimal(4);
    private final static BigDecimal BD_5 = new BigDecimal(5);
    private final static BigDecimal BD_6 = new BigDecimal(6);
    private final static BigDecimal BD_8_45 = new BigDecimal("8.45");
    private final static BigDecimal BD_12 = new BigDecimal(12);

    private final static BigDecimal BD_TWO_PI = BigDecimal.valueOf(2*Math.PI);
    private final static BigDecimal BD_SQRT_TWO_PI = new BigDecimal("2.506628274631000502415765284811");
    private final static BigDecimal BD_TWO_SQRT_PI = new BigDecimal("3.5449077018110320545963349666823");

    private final static BigDecimal BD_PI = new BigDecimal(Math.PI);

    private AtomicReference<BigDecimal> lastPrice = new AtomicReference<>(ZERO);

    private Queue<BigDecimal> queue = new ConcurrentLinkedQueue<>();

    public LevelStrategy(StrategyService strategyService, Strategy strategy, OrderService orderService, OrderMapper orderMapper, TradeService tradeService,
                         DepthService depthService, UserInfoService userInfoService,  XChangeService xChangeService) {
        super(strategy, orderService, orderMapper, tradeService, depthService, xChangeService);

        this.strategy = strategy;
        this.userInfoService = userInfoService;
        this.tradeService = tradeService;
        this.orderService = orderService;

        if (strategy.getSymbolType() != null) {
            userInfoService.createUserInfoObservable(strategy.getAccount().getId(), strategy.getSymbol().substring(0, 3))
                    .filter(u -> u.getRiskRate() != null)
                    .subscribe(u -> {
                        if (u.getRiskRate().compareTo(BigDecimal.valueOf(10)) < 0){
                            risk = TEN.divide(u.getRiskRate(), 8, HALF_EVEN).pow(3).setScale(2, HALF_EVEN);
                        }else {
                            risk = ONE;
                        }
                    });
        }

        Executors.newSingleThreadScheduledExecutor().scheduleWithFixedDelay(()-> {
            if (!queue.isEmpty()) {
                actionLevel("schedule", queue.poll(), null);
            }
        }, 5000, 10, TimeUnit.MILLISECONDS);
    }

    private void pushOrders(BigDecimal price){
        BigDecimal spread = ZERO;
        boolean inverse = strategy.isLevelInverse();

        if (!isBidRefused()){
            getOrderMap().get(price.subtract(spread), BID).forEach((k,v) -> {
                v.values().forEach(o -> {
                    if (o.getStatus().equals(WAIT) && (isVol() || !inverse || !getOrderMap().containsAsk(o.getPositionId()))){
                        pushWaitOrder(o);
                    }
                });
            });
        }

        if (!isAskRefused()){
            getOrderMap().get(price.add(spread), ASK).forEach((k,v) -> {
                v.values().forEach(o -> {
                    if (o.getStatus().equals(WAIT) && (isVol() || inverse || !getOrderMap().containsBid(o.getPositionId()))){
                        pushWaitOrder(o);
                    }
                });
            });
        }
    }

    private Executor executor = Executors.newCachedThreadPool();
    private AtomicReference<BigDecimal> lastMarket = new AtomicReference<>(ZERO);

    private void closeByMarketAsync(BigDecimal price, Date time){
        if (lastMarket.get().compareTo(price) != 0) {
            executor.execute(() -> closeByMarket(price, time));
            lastMarket.set(price);
        }
    }

    @SuppressWarnings("Duplicates")
    private void closeByMarket(BigDecimal price, Date time){
        try {
            getOrderMap().get(price.add(getSideSpread(price)), BID, false).forEach((k,v) -> {
                v.values().forEach(o -> {
                    if (o.getStatus().equals(OPEN) && time.getTime() - o.getOpen().getTime() > 0){
                        o.setStatus(CLOSED);
                        o.setClosed(new Date());
                        log.info("{} CLOSE by market {} {} {} {}", o.getStrategyId(), scale(o.getPrice()), price, o.getType(), time.getTime() - o.getOpen().getTime());
                    }
                });
            });

            getOrderMap().get(price.subtract(getSideSpread(price)), ASK, false).forEach((k,v) -> {
                v.values().forEach(o -> {
                    if (o.getStatus().equals(OPEN) && time.getTime() - o.getOpen().getTime() > 0){
                        o.setStatus(CLOSED);
                        o.setClosed(new Date());
                        log.info("{} CLOSE by market {} {} {} {}", o.getStrategyId(), scale(o.getPrice()), price, o.getType(), time.getTime() - o.getOpen().getTime());
                    }
                });
            });
        } catch (Exception e) {
            log.error("error close by market", e);
        }
    }

    private void actionLevel(BigDecimal price){
        actionLevel("subject", price, null);
    }

    private AtomicReference<BigDecimal> lastAction = new AtomicReference<>(ZERO);

    private Deque<Double> arimaPrices = new ConcurrentLinkedDeque<>();

    private void actionLevel(String key, BigDecimal price, OrderType orderType){
        try {
            if (isBidRefused() || isAskRefused()) {
                return;
            }

            if (lastAction.get().equals(price)){
                return;
            }

            action(key, price, orderType, 0);

            lastAction.set(price);

            arimaPrices.add(price.doubleValue());

            if (arimaPrices.size() > 100000){
                arimaPrices.removeFirst();
            }
        } catch (Exception e) {
            log.error("error actionLevel", e);
        }
    }

    private AtomicLong lastBalanceTime = new AtomicLong(System.currentTimeMillis());
    private AtomicBoolean balance = new AtomicBoolean(true);

    private BigDecimal balanceValue = new BigDecimal("2");

    private AtomicDouble forecast = new AtomicDouble(0);

    protected boolean getSpotBalance(){
        if (System.currentTimeMillis() - lastBalanceTime.get() >= 59000){
            String[] symbol = strategy.getSymbol().split("/");
            BigDecimal subtotalBtc = userInfoService.getVolume("subtotal", strategy.getAccount().getId(), symbol[0]);
            BigDecimal subtotalCny = userInfoService.getVolume("subtotal", strategy.getAccount().getId(), symbol[1]);

            BigDecimal price = lastAction.get();

            if (arimaPrices.size() > 0){
                double[] prices = arimaPrices.stream().mapToDouble(d -> d).toArray();
                double[] pricesDelta = new double[prices.length - 1];

                for (int i = 0; i < prices.length - 1; ++i){
                    pricesDelta[i] = prices[i+1]/prices[i] - 1;
                }

                double f = new DefaultArimaForecaster(ArimaFitter.fit(pricesDelta, 3, 1, 2), pricesDelta).next();
                double p = prices[prices.length -1] * (f + 1);

                price = BigDecimal.valueOf(p);

                forecast.set(p);
            }

            balance.set(subtotalCny.divide(subtotalBtc.multiply(price), 8, HALF_EVEN).compareTo(balanceValue) > 0);
            lastBalanceTime.set(System.currentTimeMillis());
        }

        return balance.get();
    }

    @Override
    protected double getForecast() {
        return forecast.get();
    }

    private BigDecimal getStdDev(){
        return strategy.isLevelInverse() ? tradeService.getValue("ask") : tradeService.getValue("bid");
    }

    private BigDecimal spreadDiv = new BigDecimal("8.45");

    protected BigDecimal getSpread(BigDecimal price){
        BigDecimal spread = ZERO;
        BigDecimal sideSpread = getSideSpread(price);

        if (strategy.getSymbol().equals("BTC/CNY") || strategy.getSymbol().equals("LTC/CNY")){
            BigDecimal stdDev = getStdDev();

            if (stdDev != null){
                spread = stdDev.divide(spreadDiv, 8, HALF_UP);
            }
        }else {
            spread = strategy.getSymbolType() == null
                    ? strategy.getLevelSpread().multiply(price)
                    : strategy.getLevelSpread();
        }

        return spread.compareTo(sideSpread) > 0 ? spread : sideSpread;
    }


    private BigDecimal getSideSpread(BigDecimal price){
        BigDecimal sideSpread = strategy.getSymbolType() == null
                ? strategy.getLevelSideSpread().multiply(price)
                : strategy.getLevelSideSpread();

        return sideSpread.compareTo(getStep()) > 0 ? sideSpread : getStep();
    }

    private final static int[] prime = {101, 103, 107, 109, 113, 127, 131, 137, 139, 149, 151, 157, 163, 167, 173, 179,
            181, 191, 193, 197, 199, 211, 223, 227, 229, 233, 239, 241, 251, 257, 263, 269, 271, 277, 281, 283, 293, 307,
            311, 313, 317, 331, 337, 347, 349, 353, 359, 367, 373, 379, 383, 389, 397, 401, 409, 419, 421, 431, 433, 439,
            443, 449, 457, 461, 463, 467, 479, 487, 491, 499, 503, 509, 521, 523, 541, 547, 557, 563, 569, 571, 577, 587,
            593, 599, 601, 607, 613, 617, 619, 631, 641, 643, 647, 653, 659, 661, 673, 677, 683, 691, 701, 709, 719, 727,
            733, 739, 743, 751, 757, 761, 769, 773, 787, 797, 809, 811, 821, 823, 827, 829, 839, 853, 857, 859, 863, 877,
            881, 883, 887, 907, 911, 919, 929, 937, 941, 947, 953, 967, 971, 977, 983, 991, 997};


    private AtomicReference<BigDecimal> lastBuyPrice = new AtomicReference<>(ZERO);
    private AtomicReference<BigDecimal> lastSellPrice = new AtomicReference<>(ZERO);

    private BigDecimal amountRange = new BigDecimal(125);


    private void action(String key, BigDecimal price, OrderType orderType, int priceLevel) {
        try {
            boolean up = getSpotBalance();

            BigDecimal spread = scale(getSpread(price));

            BigDecimal p = scale(strategy.isLevelInverse() ? price.subtract(BD_0_05) : price.add(BD_0_05));
            BigDecimal buyPrice = scale(strategy.isLevelInverse() ? p : p.subtract(spread));
            BigDecimal sellPrice = scale(strategy.isLevelInverse() ? p.add(spread) : p);

            if (!getOrderMap().contains(buyPrice, spread, BID) && !getOrderMap().contains(sellPrice, spread, ASK)){
                log.info("{} "  + key + " {} {} {}", strategy.getId(), price.setScale(3, HALF_EVEN), orderType, spread);

                BigDecimal total = userInfoService.getVolume("total", strategy.getAccount().getId(), null).setScale(8, HALF_UP);
//                BigDecimal amount = total.divide(price, 8, HALF_UP).divide(spreadDiv.multiply(amountRange), 8, HALF_UP);

                BigDecimal buyAmount = strategy.getLevelLot().multiply(BigDecimal.valueOf(up ? random.nextGaussian()/2 + 2 : random.nextGaussian()/2 + 1));
                BigDecimal sellAmount = strategy.getLevelLot().multiply(BigDecimal.valueOf(up ? random.nextGaussian()/2 + 1 : random.nextGaussian()/2 + 2));

                //slip
                if (lastBuyPrice.get().compareTo(ZERO) > 0 && buyPrice.compareTo(lastBuyPrice.get()) < 0){
                    buyAmount = buyAmount.multiply(lastBuyPrice.get().subtract(buyPrice).abs().divide(spread, 8, HALF_UP));
                }
                if (lastSellPrice.get().compareTo(ZERO) > 0 && sellPrice.compareTo(lastSellPrice.get()) > 0){
                    sellAmount = sellAmount.multiply(sellPrice.subtract(lastSellPrice.get()).abs().divide(spread, 8, HALF_UP));
                }

                //less
                if (buyAmount.compareTo(BD_0_01) < 0){
                    buyAmount = BD_0_01;
                }
                if (sellAmount.compareTo(BD_0_01) < 0){
                    sellAmount = BD_0_01;
                }

                Long positionId = positionIdGen.incrementAndGet();
                Order buyOrder = new Order(strategy, positionId, BID, buyPrice, buyAmount.setScale(3, HALF_UP));
                Order sellOrder = new Order(strategy, positionId, ASK, sellPrice, sellAmount.setScale(3, HALF_UP));

                buyOrder.setSpread(spread);
                sellOrder.setSpread(spread);

                BigDecimal freeBtc = userInfoService.getVolume("free", strategy.getAccount().getId(), "BTC");
                BigDecimal freeCny = userInfoService.getVolume("free", strategy.getAccount().getId(), "CNY");

                if (freeCny.compareTo(buyAmount.multiply(buyPrice)) > 0){
                    createOrderAsync(buyOrder);

                    lastBuyPrice.set(buyPrice);
                }

                if (freeBtc.compareTo(sellAmount) > 0){
                    createOrderAsync(sellOrder);

                    lastSellPrice.set(sellPrice);
                }
            }
        } catch (Exception e) {
            log.error("action error -> ", e);
        }
    }

    private AtomicReference<BigDecimal> lastTrade = new AtomicReference<>(ZERO);

    @Override
    protected void onTrade(Trade trade) {
        if (lastTrade.get().compareTo(ZERO) != 0 &&
                lastTrade.get().subtract(trade.getPrice()).abs().divide(lastTrade.get(), 8, HALF_EVEN).compareTo(BD_0_01) < 0){

            if ((!strategy.isLevelInverse() && trade.getOrderType().equals(BID)) || (strategy.isLevelInverse() && trade.getOrderType().equals(ASK))) {
                lastPrice.set(trade.getPrice());

                queue.add(trade.getPrice());
            }

            //closeByMarketAsync(trade.getPrice(), trade.getOrigTime());
        }else{
            log.warn("trade price diff 1% {} {} {}", trade.getPrice(), trade.getSymbol(), Objects.toString(trade.getSymbolType(), ""));
        }

        lastTrade.set(trade.getPrice());
    }

    private AtomicReference<BigDecimal> depthSpread = new AtomicReference<>(BD_0_25);
    private AtomicReference<BigDecimal> depthBid = new AtomicReference<>(ZERO);
    private AtomicReference<BigDecimal> depthAsk = new AtomicReference<>(ZERO);

    @Override
    protected void onDepth(Depth depth) {
        BigDecimal ask = depth.getAsk();
        BigDecimal bid = depth.getBid();

        if (ask != null && bid != null && lastTrade.get().compareTo(ZERO) != 0 &&
                lastTrade.get().subtract(ask).abs().divide(lastTrade.get(), 8, HALF_EVEN).compareTo(BD_0_01) < 0 &&
                lastTrade.get().subtract(bid).abs().divide(lastTrade.get(), 8, HALF_EVEN).compareTo(BD_0_01) < 0) {

//            if (strategy.isLevelInverse()) {
////                lastPrice.set(ask);
////                queue.add(ask);
//            }else {
////                lastPrice.set(bid);
////                queue.add(bid);
//            }

            depthSpread.set(ask.subtract(bid).abs());
            depthBid.set(bid);
            depthAsk.set(ask);
        }
    }

    @Override
    protected void onRealTrade(Order order) {
        if (order.getStatus().equals(CLOSED) && order.getAvgPrice().compareTo(ZERO) > 0){
            if ((!strategy.isLevelInverse() && order.getType().equals(BID)) || (strategy.isLevelInverse() && order.getType().equals(ASK))) {
                lastPrice.set(order.getAvgPrice());
                queue.add(order.getAvgPrice());
            }
        }
    }
}

