package com.xeiam.xchange.okcoin;

import com.xeiam.xchange.BaseExchange;
import com.xeiam.xchange.ExchangeSpecification;
import com.xeiam.xchange.currency.CurrencyPair;
import com.xeiam.xchange.okcoin.service.polling.OkCoinAccountService;
import com.xeiam.xchange.okcoin.service.polling.OkCoinMarketDataService;
import com.xeiam.xchange.okcoin.service.polling.OkCoinTradeService;
import si.mazi.rescu.SynchronizedValueFactory;

import java.util.Arrays;
import java.util.List;

public class OkCoinExchange extends BaseExchange {

    /**
     * The parameter name of the symbols that will focus on.
     */

    private static final List<CurrencyPair> SYMBOLS = Arrays.asList(CurrencyPair.BTC_CNY, CurrencyPair.LTC_CNY);
    private static final List<CurrencyPair> INTL_SYMBOLS = Arrays.asList(CurrencyPair.BTC_USD, CurrencyPair.LTC_USD);

    @Override
    public void applySpecification(ExchangeSpecification exchangeSpecification) {
        super.applySpecification(exchangeSpecification);

        this.pollingMarketDataService = new OkCoinMarketDataService(this);
        if (exchangeSpecification.getApiKey() != null) {
            this.pollingAccountService = new OkCoinAccountService(this);
            this.pollingTradeService = new OkCoinTradeService(this);
        }
    }

    @Override
    public SynchronizedValueFactory<Long> getNonceFactory() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExchangeSpecification getDefaultExchangeSpecification() {

        ExchangeSpecification exchangeSpecification = new ExchangeSpecification(this.getClass().getCanonicalName());
        exchangeSpecification.setSslUri("https://www.okcoin.cn/api");
        exchangeSpecification.setHost("www.okcoin.cn");
        exchangeSpecification.setExchangeName("OKCoin");
        exchangeSpecification.setExchangeDescription("OKCoin is a globally oriented crypto-currency trading platform.");

        exchangeSpecification.setExchangeSpecificParametersItem("Intl_SslUri", "https://www.okcoin.com/api");
        exchangeSpecification.setExchangeSpecificParametersItem("Intl_Host", "www.okcoin.com");

        // set to true to automatically use the Intl_ parameters for ssluri and host
        exchangeSpecification.setExchangeSpecificParametersItem("Use_Intl", true);

        getMetaData().setCurrencyPairs(INTL_SYMBOLS);

        return exchangeSpecification;
    }

    @Override
    public String getMetaDataFileName(ExchangeSpecification exchangeSpecification) {

        if (exchangeSpecification.getExchangeSpecificParametersItem("Use_Intl").equals(false)) {
            return exchangeSpecification.getExchangeName().toLowerCase().replace(" ", "").replace("-", "").replace(".", "") + "_china";
        } else {
            return exchangeSpecification.getExchangeName().toLowerCase().replace(" ", "").replace("-", "").replace(".", "") + "_intl";

        }
    }

}
