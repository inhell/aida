package ru.inheaven.aida.coin.entity;

import org.apache.wicket.protocol.ws.api.message.IWebSocketPushMessage;

/**
 * @author Anatoly Ivanov
 *         Date: 005 05.08.14 15:16
 */
public class ExchangeMessage<T> implements IWebSocketPushMessage{
    private Exchanges exchange;

    private T payload;

    public ExchangeMessage(Exchanges exchange, T payload) {
        this.exchange = exchange;
        this.payload = payload;
    }

    public T getPayload() {
        return payload;
    }

    public Exchanges getExchange() {
        return exchange;
    }
}
