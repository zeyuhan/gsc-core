package org.gsc.core.wrapper.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExchangeProcessor {

  private long supply;

  public ExchangeProcessor(long supply) {
    this.supply = supply;
  }

  private long exchange_to_supply(long balance, long quant) {
    logger.info("balance: " + balance);
    long newBalance = balance + quant;
    logger.info("balance + quant: " + newBalance);

    double issuedSupply = -supply * (1.0 - Math.pow(1.0 + (double) quant / newBalance, 0.0005));
    logger.info("issuedSupply: " + issuedSupply);
    long out = (long) issuedSupply;
    supply += out;

    return out;
  }

  private long exchange_to_supply2(long balance, long quant) {
    logger.info("balance: " + balance);
    long newBalance = balance - quant;
    logger.info("balance - quant: " + (balance - quant));

    double issuedSupply = -supply * (1.0 - Math.pow(1.0 + (double) quant / newBalance, 0.0005));
    logger.info("issuedSupply: " + issuedSupply);
    long out = (long) issuedSupply;
    supply += out;

    return out;
  }

  private long exchange_from_supply(long balance, long supplyQuant) {
    supply -= supplyQuant;

    double exchangeBalance =
        balance * (Math.pow(1.0 + (double) supplyQuant / supply, 2000.0) - 1.0);
    logger.info("exchangeBalance: " + exchangeBalance);
    long out = (long) exchangeBalance;
    long newBalance = balance - out;

//    if (isTRX) {
//      out = Math.round(exchangeBalance / 100000) * 100000;
//      logger.info("---out: " + out);
//    }

    return out;
  }

  public long exchange(long sellTokenBalance, long buyTokenBalance, long sellTokenQuant) {
    long relay = exchange_to_supply(sellTokenBalance, sellTokenQuant);
    return exchange_from_supply(buyTokenBalance, relay);
  }

}
