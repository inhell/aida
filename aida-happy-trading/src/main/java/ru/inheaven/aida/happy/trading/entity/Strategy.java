package ru.inheaven.aida.happy.trading.entity;

import ru.inhell.aida.common.entity.AbstractEntity;

import java.math.BigDecimal;

/**
 * @author inheaven on 02.05.2015 23:48.
 */
public class Strategy extends AbstractEntity {
    private String name;
    private StrategyType type;
    private Long accountId;

    private ExchangeType exchangeType;
    private String symbol;
    private SymbolType symbolType;

    private BigDecimal levelLot;
    private BigDecimal levelSpread;
    private BigDecimal levelSize;

    private boolean active;

    private String apiKey;
    private String secretKey;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public StrategyType getType() {
        return type;
    }

    public void setType(StrategyType type) {
        this.type = type;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public ExchangeType getExchangeType() {
        return exchangeType;
    }

    public void setExchangeType(ExchangeType exchangeType) {
        this.exchangeType = exchangeType;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public SymbolType getSymbolType() {
        return symbolType;
    }

    public void setSymbolType(SymbolType symbolType) {
        this.symbolType = symbolType;
    }

    public BigDecimal getLevelLot() {
        return levelLot;
    }

    public void setLevelLot(BigDecimal levelLot) {
        this.levelLot = levelLot;
    }

    public BigDecimal getLevelSpread() {
        return levelSpread;
    }

    public void setLevelSpread(BigDecimal levelSpread) {
        this.levelSpread = levelSpread;
    }

    public BigDecimal getLevelSize() {
        return levelSize;
    }

    public void setLevelSize(BigDecimal levelSize) {
        this.levelSize = levelSize;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
