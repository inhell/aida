package ru.inheaven.aida.happy.trading.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inheaven.aida.happy.trading.entity.*;
import ru.inheaven.aida.happy.trading.mapper.OrderMapper;
import ru.inheaven.aida.happy.trading.service.DepthService;
import ru.inheaven.aida.happy.trading.service.OrderService;
import ru.inheaven.aida.happy.trading.service.TradeService;
import ru.inheaven.aida.happy.trading.service.UserInfoService;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static java.math.BigDecimal.*;
import static java.math.RoundingMode.HALF_EVEN;
import static ru.inheaven.aida.happy.trading.entity.OrderStatus.*;
import static ru.inheaven.aida.happy.trading.entity.OrderType.*;

/**
 * @author inheaven on 08.07.2015 19:39.
 */
public class LevelStrategy extends BaseStrategy{
    private Logger log = LoggerFactory.getLogger(getClass());

    private SecureRandom random = new SecureRandom("LevelStrategy".getBytes());
    private Strategy strategy;
    private BigDecimal risk = ONE;

    private Map<String, Long> levelTimeMap = new ConcurrentHashMap<>();

    private UserInfoService userInfoService;
    private TradeService tradeService;

    private static AtomicLong positionId = new AtomicLong(System.nanoTime());

    public LevelStrategy(Strategy strategy, OrderService orderService, OrderMapper orderMapper, TradeService tradeService,
                         DepthService depthService, UserInfoService userInfoService) {
        super(strategy, orderService, orderMapper, tradeService, depthService);

        this.strategy = strategy;
        this.userInfoService = userInfoService;
        this.tradeService = tradeService;

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
    }

    private void pushOrders(BigDecimal price){
        BigDecimal spread = getSpread(price);
        boolean inverse = strategy.isLevelInverse();

        if (!isBidRefused()){
            getOrderMap().get(price.subtract(spread), BID).forEach((k,v) -> {
                v.forEach(o -> {
                    if (o.getStatus().equals(WAIT) && (!inverse || !getOrderMap().containsAsk(o.getPositionId()))){
                        pushWaitOrderAsync(o);
                    }
                });
            });
        }

        if (!isAskRefused()){
            getOrderMap().get(price.add(spread), ASK).forEach((k,v) -> {
                v.forEach(o -> {
                    if (o.getStatus().equals(WAIT) && (inverse || !getOrderMap().containsBid(o.getPositionId()))){
                        pushWaitOrderAsync(o);
                    }
                });
            });
        }
    }

    private Executor executor = Executors.newWorkStealingPool();
    private AtomicReference<BigDecimal> lastMarket = new AtomicReference<>(ZERO);

    private void closeByMarketAsync(BigDecimal price){
        if (lastMarket.get().compareTo(price) != 0) {
            executor.execute(() -> closeByMarket(price));
            lastMarket.set(price);
        }
    }

    @SuppressWarnings("Duplicates")
    private void closeByMarket(BigDecimal price){
        getOrderMap().get(price.add(getSideSpread(price)), BID, false).forEach((k,v) -> {
            v.forEach(o -> {
                if (o.getStatus().equals(OPEN)){
                    o.setStatus(CLOSED);
                    o.setClosed(new Date());
                    log.info("{} CLOSED by market {} {} {}", o.getStrategyId(), scale(o.getPrice()), price, o.getType());
                }
            });
        });

        getOrderMap().get(price.subtract(getSideSpread(price)), ASK, false).forEach((k,v) -> {
            v.forEach(o -> {
                if (o.getStatus().equals(OPEN)){
                    o.setStatus(CLOSED);
                    o.setClosed(new Date());
                    log.info("{} CLOSED by market {} {} {}", o.getStrategyId(), scale(o.getPrice()), price, o.getType());
                }
            });
        });
    }

    private Executor executor2 = Executors.newWorkStealingPool();
    private AtomicReference<BigDecimal> lastAction = new AtomicReference<>(ZERO);

    private void actionAsync(String key, BigDecimal price, OrderType orderType){
        if (lastAction.get().compareTo(price) != 0) {
            lastAction.set(price);
            executor2.execute(() -> action(key, price, orderType));
        }
    }

    private Semaphore semaphore = new Semaphore(1);

    private void action(String key, BigDecimal price, OrderType orderType){
        try {
            semaphore.acquire();
            actionLevel(key, price, orderType);
            pushOrders(price);
        } catch (Exception e) {
            log.error("error action level", e);
        } finally {
            semaphore.release();
        }
    }

    private void actionLevel(String key, BigDecimal price, OrderType orderType){
        boolean inverse = strategy.isLevelInverse();

        if ((!inverse && !isMinSpot()) || (inverse && !isMaxSpot())) {
            BigDecimal sideSpread = getSideSpread(price);

            for (int i = strategy.getLevelSize().intValue(); i > 0 ; --i){
                action(key, price.add(BigDecimal.valueOf(i).multiply(sideSpread)), orderType, i);
            }

            action(key, price, orderType, 0);
        }
    }

    private static final BigDecimal CNY_MIN = BigDecimal.valueOf(3000);
    private static final BigDecimal USD_MIN = BigDecimal.valueOf(300);

    @SuppressWarnings("Duplicates")
    private boolean isMinSpot(){
        BigDecimal volume = userInfoService.getVolume("free_spot", strategy.getAccount().getId(), null);

        if (volume != null){
            if (strategy.getSymbol().contains("CNY")){
                return volume.compareTo(CNY_MIN) < 0;
            }else if (strategy.getSymbol().contains("USD")){
                return volume.compareTo(USD_MIN) < 0;
            }
        }

        return false;
    }

    private static final BigDecimal CNY_MAX = BigDecimal.valueOf(42000);
    private static final BigDecimal USD_MAX = BigDecimal.valueOf(7000);

    @SuppressWarnings("Duplicates")
    private boolean isMaxSpot(){
        BigDecimal volume = userInfoService.getVolume("free_spot", strategy.getAccount().getId(), null);

        if (volume != null){
            if (strategy.getSymbol().contains("CNY")){
                return volume.compareTo(CNY_MAX) > 0;
            }else if (strategy.getSymbol().contains("USD")){
                return volume.compareTo(USD_MAX) > 0;
            }
        }

        return false;
    }

    private static final BigDecimal TWO = BigDecimal.valueOf(2);

    private BigDecimal getSpread(BigDecimal price){
        BigDecimal spread = ZERO;
        BigDecimal sideSpread = getSideSpread(price);

        if (strategy.getSymbol().equals("BTC/CNY")){
            BigDecimal stdDev = tradeService.getStdDev("BTC/CNY");

            if (stdDev != null){
                spread = stdDev.divide(TWO, HALF_EVEN).subtract(sideSpread).divide(TWO, HALF_EVEN);
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

    private void action(String key, BigDecimal price, OrderType orderType, int priceLevel) {
        try {
            BigDecimal priceF = scale(orderType.equals(ASK) ? price.subtract(getStep()) : price.add(getStep()));
            BigDecimal spread = scale(getSpread(priceF));
            BigDecimal sideSpread = scale(getSideSpread(priceF));
            BigDecimal level = priceF.divideToIntegralValue(spread);
            BigDecimal buyPrice = orderType.equals(ASK) ? priceF.subtract(spread) : priceF;
            BigDecimal sellPrice = orderType.equals(ASK) ? priceF : priceF.add(spread);

            if (!getOrderMap().contains(buyPrice, sideSpread, BID) && !getOrderMap().contains(sellPrice, sideSpread, ASK)){
                log.info(key + " {} {} {}", price.setScale(3, HALF_EVEN), orderType, spread);

                BigDecimal amountHFT = strategy.getLevelLot();

//                if (levelTimeMap.get(level.toString() + reversing) != null){
//                    Long time = System.currentTimeMillis() - levelTimeMap.get(level.toString() + reversing);
//
//                    if (strategy.getSymbolType() == null){
//                        if (time < 600000 && strategy.getAccount().getExchangeType().equals(ExchangeType.OKCOIN)){
//                            amountHFT = amountHFT.multiply(BigDecimal.valueOf(10.0 - 9.0*time/600000));
//
//                            log.info("HFT -> {}s {} {} -> {}", time/1000, price.setScale(3, HALF_EVEN),
//                                    strategy.getLevelLot().setScale(3, HALF_EVEN), amountHFT.setScale(3, HALF_EVEN));
//                        }
//
//                        if (time < 5000 && strategy.getAccount().getExchangeType().equals(ExchangeType.OKCOIN_CN)){
//                            amountHFT = amountHFT.multiply(BigDecimal.valueOf(3.0 - 2.0*time/5000));
//
//                            log.info("HFT -> {}ms {} {} -> {}", time, price.setScale(3, HALF_EVEN),
//                                    strategy.getLevelLot().setScale(3, HALF_EVEN), amountHFT.setScale(3, HALF_EVEN));
//                        }
//                    }else if (time < 15000){
//                        amountHFT = amountHFT.multiply(BigDecimal.valueOf(2));
//
//                        log.info("HFT -> {}s {} {} {}", time/1000, price.setScale(3, HALF_EVEN),
//                                amountHFT.setScale(3, HALF_EVEN), strategy.getSymbolType());
//                    }
//                }

                if (risk.compareTo(ONE) > 0){
                    log.warn("RISK RATE {} {} {}", risk.setScale(3, HALF_EVEN), strategy.getSymbol(),
                            Objects.toString(strategy.getSymbolType(), ""));
                }

                Long positionId = LevelStrategy.positionId.incrementAndGet();

                BigDecimal buyAmount = amountHFT;
                BigDecimal sellAmount = amountHFT;

                if (strategy.getSymbolType() == null){
                    buyAmount = amountHFT.add(amountHFT.multiply(spread).divide(buyPrice, 8, HALF_EVEN));
                    sellAmount = amountHFT;
                }

                Order buyOrder = new Order(strategy, positionId, strategy.getSymbolType() != null ? OPEN_LONG : BID,
                        buyPrice, buyAmount);
                Order sellOrder = new Order(strategy, positionId, strategy.getSymbolType() != null ? CLOSE_LONG : ASK,
                        sellPrice, sellAmount);

                if (strategy.isLevelInverse()){
                    createOrderAsync(sellOrder);
                    createWaitOrder(buyOrder);
                }else {
                    createOrderAsync(buyOrder);
                    createWaitOrder(sellOrder);
                }

                if (orderType.equals(ASK)) {
                    levelTimeMap.put(level.toString(),  System.currentTimeMillis());
                }
            }
        } catch (Exception e) {
            log.error("action error -> ", e);
        }
    }

    private BigDecimal lastTrade = ZERO;

    @Override
    protected void onTrade(Trade trade) {
        if (lastTrade.compareTo(ZERO) != 0 &&
                lastTrade.subtract(trade.getPrice()).abs().divide(lastTrade, 8, HALF_EVEN).compareTo(getStep()) < 0){
            actionAsync("on trade", trade.getPrice(), trade.getOrderType());
            closeByMarketAsync(trade.getPrice());
        }else{
            log.warn("trade price diff 1% {} {} {}", trade.getPrice(), trade.getSymbol(), Objects.toString(trade.getSymbolType(), ""));
        }

        lastTrade = trade.getPrice();
    }

    @Override
    protected void onDepth(Depth depth) {
        BigDecimal ask = depth.getAsk();
        BigDecimal bid = depth.getBid();

        if (ask != null && bid != null && lastTrade.compareTo(ZERO) != 0 &&
                lastTrade.subtract(ask).abs().divide(lastTrade, 8, HALF_EVEN).compareTo(getStep()) < 0 &&
                lastTrade.subtract(bid).abs().divide(lastTrade, 8, HALF_EVEN).compareTo(getStep()) < 0) {
            actionAsync("on depth ask", ask, ASK);
            actionAsync("on depth bid", bid, BID);
        }
    }

    @Override
    protected void onRealTrade(Order order) {
        if (order.getStatus().equals(CLOSED) && order.getAvgPrice().compareTo(ZERO) > 0){
            actionAsync("on real trade", order.getAvgPrice(), order.getType());
        }
    }
}
