package ru.inhell.aida.entity;

import java.util.Date;

/**
 * @author Anatoly A. Ivanov java@inhell.ru
 *         Date: 05.12.10 19:04
 */
public class VectorForecast {
    public static enum INTERVAL{ONE_MINUTE, FIVE_MINUTES, HALF_HOUR, ONE_HOUR}

    private Long id;
    private String symbol;
    private INTERVAL interval;
    private int n;
    private int l;
    private int p;
    private int m;
    private Date created;

    public VectorForecast() {
    }

    public VectorForecast(String symbol, INTERVAL interval, int n, int l, int p, int m, Date created) {
        this.symbol = symbol;
        this.interval = interval;
        this.n = n;
        this.l = l;
        this.p = p;
        this.m = m;
        this.created = created;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public INTERVAL getInterval() {
        return interval;
    }

    public void setInterval(INTERVAL interval) {
        this.interval = interval;
    }

    public int getN() {
        return n;
    }

    public void setN(int n) {
        this.n = n;
    }

    public int getL() {
        return l;
    }

    public void setL(int l) {
        this.l = l;
    }

    public int getP() {
        return p;
    }

    public void setP(int p) {
        this.p = p;
    }

    public int getM() {
        return m;
    }

    public void setM(int m) {
        this.m = m;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
