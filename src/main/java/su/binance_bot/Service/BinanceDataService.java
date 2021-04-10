package su.binance_bot.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import su.binance_bot.Enum.PositionEnum;
import su.binance_bot.Model.AdxAndDi;
import su.binance_bot.Model.Canal;
import su.binance_bot.Model.Coin;
import su.binance_bot.Model.CoinChanged;
import su.binance_bot.Model.DI;
import su.binance_bot.Model.MyCandlestick;
import su.binance_bot.Utils.ADXutil;
import su.binance_bot.Utils.CanalUtil;

@Service
public class BinanceDataService {
  BinanceApiRestClient client;
  @Value("${binance.api.key}")
  String apiKey;
  @Value("${binance.secret}")
  String secret;
  private final ADXutil adxUtil = new ADXutil();
  private final CanalUtil cUtil = new CanalUtil();

  @PostConstruct
  private void init() {
    BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance(this.apiKey, this.secret);
    this.client = factory.newRestClient();
  }

  public CoinChanged getPositionChange(Coin coin) {
    try {
      // multiplier must be over 10.. 12+ is ok..
      // could make a cache but lookback size doesn't increase apis request weight.. relatively small anyway
      int lookback = coin.getAdx() * 20 < 1000 ? coin.getAdx() * 20 : 999;
      long start = System.currentTimeMillis();
      long lookbackTime = start - (lookback * 15 * 60 * 1000);
      List<Candlestick> candlesticks = client.getCandlestickBars(coin.getSymbol().toString(),
          CandlestickInterval.FIFTEEN_MINUTES, lookback, lookbackTime, start);
      List<MyCandlestick> myCandlestickList = new ArrayList<>();
      for (int i = 0; i < candlesticks.size(); i++) {
        Candlestick c = candlesticks.get(i);
        MyCandlestick m = new MyCandlestick(
            LocalDateTime.ofInstant(Instant.ofEpochMilli(c.getOpenTime()), ZoneId.systemDefault()),
            Float.valueOf(c.getOpen()), Float.valueOf(c.getHigh()), Float.valueOf(c.getLow()),
            Float.valueOf(c.getClose()), Float.valueOf(c.getVolume()));
        myCandlestickList.add(m);
      }

      AdxAndDi aad = this.adxUtil.getAdxAndDi(coin.getAdx(), myCandlestickList);
      List<Float> adxList = aad.getAdxList();
      DI di = aad.getDi();
      Canal canal = this.cUtil.getCanals(myCandlestickList, coin.getCL());
      int previousCandle = myCandlestickList.size() > 2 ? myCandlestickList.size() - 2 : 0;
      if (previousCandle != 0) {
        if (coin.getPosition() == PositionEnum.CLOSED) {
          boolean changed = this.isOpenLong(adxList.get(previousCandle), adxList.get(previousCandle - coin.getAL()),
              coin.getGoLong(), myCandlestickList.get(previousCandle).getClose(),
              canal.getHigherCanalList().get(previousCandle), di.getIsPositiveOver().get(previousCandle));
          return new CoinChanged(changed, myCandlestickList.get(myCandlestickList.size() - 1).getClose(),
              LocalDateTime.now());
        } else {
          boolean changed = this.isCloseLong(adxList.get(previousCandle), adxList.get(previousCandle - coin.getAL()),
              coin.getCloseLong(), myCandlestickList.get(previousCandle).getClose(),
              canal.getLowerCanalList().get(previousCandle), !di.getIsPositiveOver().get(previousCandle));
          return new CoinChanged(changed, myCandlestickList.get(myCandlestickList.size() - 1).getClose(),
              LocalDateTime.now());
        }
      } else {
        return null;
      }
    } catch (Exception e) {
      System.out.println("Error in binanceDataService\n" + e.toString());
      return null;
    }

  }

  private boolean isCloseLong(float adx, float lookbackAdx, int exitAdx, float close, float canalLow,
      boolean isNegativeOver) {
    return adx >= exitAdx && close < canalLow && adx >= lookbackAdx && isNegativeOver;
  }

  private boolean isOpenLong(float adx, float lookbackAdx, int enterAdx, float close, float canalHigh,
      boolean isPositiveOver) {
    return adx >= enterAdx && close > canalHigh && adx >= lookbackAdx && isPositiveOver;
  }

}
