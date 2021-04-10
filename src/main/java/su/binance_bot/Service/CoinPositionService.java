package su.binance_bot.Service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import su.binance_bot.Model.CoinConfig;
import su.binance_bot.Model.CoinPosition;
import su.binance_bot.Repository.CoinPositionRepository;

@Service
@Transactional
public class CoinPositionService {

  private final CoinPositionRepository coinPositionRepository;

  public CoinPositionService(CoinPositionRepository coinPositionRepository) {
    this.coinPositionRepository = coinPositionRepository;
  }

  // Better have some coin position in db or that coin will be left behind.. only runs once in post construction
  public List<CoinPosition> getLatestPositions(List<CoinConfig> coinConfigList) {
    List<CoinPosition> result = new ArrayList<>();
    coinConfigList.forEach(coin -> this.coinPositionRepository.findFirstBySymbolOrderByIdDesc(coin.getSymbol())
        .ifPresent(cp -> result.add(cp)));
    return result;
  }

  public void saveNewPosition(CoinPosition coinPosition) {
    this.coinPositionRepository.insert(coinPosition);
  }

}
