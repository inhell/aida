package com.xeiam.xchange.bittrex.v1.service.polling;

import com.xeiam.xchange.Exchange;
import com.xeiam.xchange.bittrex.v1.dto.marketdata.*;
import com.xeiam.xchange.exceptions.ExchangeException;

import java.io.IOException;
import java.util.ArrayList;

public class BittrexMarketDataServiceRaw extends BittrexBasePollingService {

  /**
   * Constructor
   *
   * @param exchange
   */
  public BittrexMarketDataServiceRaw(Exchange exchange) {

    super(exchange);
  }

  public BittrexCurrency[] getBittrexCurrencies() throws IOException {

    BittrexCurrenciesResponse response = bittrexAuthenticated.getCurrencies();

    if (response.isSuccess()) {
      return response.getCurrencies();
    } else {
      throw new ExchangeException(response.getMessage());
    }

  }

  public ArrayList<BittrexSymbol> getBittrexSymbols() throws IOException {

    BittrexSymbolsResponse response = bittrexAuthenticated.getSymbols();

    if (response.isSuccess()) {
      return response.getSymbols();
    } else {
      throw new ExchangeException(response.getMessage());
    }

  }

  public BittrexTicker getBittrexTicker(String pair) throws IOException {

    BittrexTickerResponse response = bittrexAuthenticated.getTicker(pair);

    if (response.getSuccess()) {
      return response.getTicker();
    } else {
      throw new ExchangeException(response.getMessage());
    }

  }

  public ArrayList<BittrexTicker> getBittrexTickers() throws IOException {

    BittrexTickersResponse response = bittrexAuthenticated.getTickers();

    if (response.isSuccess()) {
      return response.getTickers();
    } else {
      throw new ExchangeException(response.getMessage());
    }

  }

  public BittrexDepth getBittrexOrderBook(String pair, int depth) throws IOException {

    BittrexDepthResponse response = bittrexAuthenticated.getBook(pair, "both", depth);

    if (response.getSuccess()) {

      BittrexDepth bittrexDepth = response.getDepth();
      return bittrexDepth;
    } else {
      throw new ExchangeException(response.getMessage());
    }
  }

  public BittrexTrade[] getBittrexTrades(String pair, int count) throws IOException {

    BittrexTradesResponse response = bittrexAuthenticated.getTrades(pair, count);

    if (response.getSuccess()) {

      BittrexTrade[] bittrexTrades = response.getTrades();
      return bittrexTrades;
    } else {
      throw new ExchangeException(response.getMessage());
    }
  }
}
