package ru.inheaven.aida.coin.entity;

import com.xeiam.xchange.currency.CurrencyPair;

import java.io.Serializable;

/**
 * @author Anatoly Ivanov
 *         Date: 005 05.08.14 12:41
 */
public class ExchangePair implements Serializable{
    private Exchanges exchange;
    private String pair;

    public ExchangePair(Exchanges exchange, String pair) {
        this.exchange = exchange;
        this.pair = pair;
    }

    public static ExchangePair of(Exchanges exchange, CurrencyPair currencyPair) {
        return new ExchangePair(exchange, currencyPair.baseSymbol + "/" + currencyPair.counterSymbol);
    }

    public Exchanges getExchange() {
        return exchange;
    }

    public void setExchange(Exchanges exchange) {
        this.exchange = exchange;
    }

    public String getPair() {
        return pair;
    }

    public void setPair(String pair) {
        this.pair = pair;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ExchangePair that = (ExchangePair) o;

        return exchange == that.exchange && !(pair != null ? !pair.equals(that.pair) : that.pair != null);
    }

    @Override
    public int hashCode() {
        return 31 * (exchange != null ? exchange.hashCode() : 0) + (pair != null ? pair.hashCode() : 0);
    }
}
