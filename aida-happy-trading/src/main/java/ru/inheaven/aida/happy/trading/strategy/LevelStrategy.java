package ru.inheaven.aida.happy.trading.strategy;

import ru.inheaven.aida.happy.trading.entity.Order;
import ru.inheaven.aida.happy.trading.entity.OrderType;
import ru.inheaven.aida.happy.trading.entity.Strategy;
import ru.inheaven.aida.happy.trading.entity.Trade;
import ru.inheaven.aida.happy.trading.exception.CreateOrderException;
import ru.inheaven.aida.happy.trading.mapper.OrderMapper;
import ru.inheaven.aida.happy.trading.service.OrderService;
import ru.inheaven.aida.happy.trading.service.TradeService;

import java.math.BigDecimal;
import java.security.SecureRandom;

import static java.math.BigDecimal.ONE;
import static java.math.RoundingMode.HALF_UP;
import static ru.inheaven.aida.happy.trading.entity.OrderType.CLOSE_LONG;
import static ru.inheaven.aida.happy.trading.entity.OrderType.OPEN_LONG;

/**
 * @author inheaven on 08.07.2015 19:39.
 */
public class LevelStrategy extends BaseStrategy{
    private Strategy strategy;

    private int errorCount = 0;
    private long errorTime = 0;

    private SecureRandom random = new SecureRandom("LevelStrategy".getBytes());

    public LevelStrategy(Strategy strategy, OrderService orderService, OrderMapper orderMapper, TradeService tradeService) {
        super(strategy, orderService, orderMapper, tradeService);

        this.strategy = strategy;
    }

    @Override
    protected void onTrade(Trade trade) {
        if (errorCount > 10){
            if (System.currentTimeMillis() - errorTime < 60000){
                return;
            }else{
                errorCount = 0;
                errorTime = 0;
            }
        }

        BigDecimal delta = trade.getPrice().multiply(strategy.getLevelSpread()).divide(BigDecimal.valueOf(2), HALF_UP);

        try {
            if (!getOrderMap().values().parallelStream()
                    .filter(order -> OrderType.LONG.contains(order.getType()))
                    .filter(order -> order.getPrice().subtract(trade.getPrice()).abs()
                            .compareTo(delta.multiply(BigDecimal.valueOf(1.25))) < 0)
                    .findAny()
                    .isPresent()){
                BigDecimal balanceLong = getBalance(delta);
                createOrder(new Order(strategy, OPEN_LONG, trade.getPrice().subtract(delta).subtract(balanceLong), ONE));
                createOrder(new Order(strategy, CLOSE_LONG, trade.getPrice().add(delta).subtract(balanceLong), ONE));
            }
        } catch (CreateOrderException e) {
            errorCount++;
            errorTime = System.currentTimeMillis();
        }
    }

    private BigDecimal getBalance(BigDecimal delta){
        return delta.multiply(BigDecimal.valueOf(random.nextDouble() * (random.nextBoolean() ? 1/2 : -1/2)));
    }
}
