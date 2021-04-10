package su.binance_bot.Runnable;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import su.binance_bot.ChatList;
import su.binance_bot.Enum.DBCommandType;
import su.binance_bot.Enum.PositionEnum;
import su.binance_bot.Model.ChatID;
import su.binance_bot.Model.Coin;
import su.binance_bot.Model.CoinChanged;
import su.binance_bot.Model.CoinConfig;
import su.binance_bot.Model.CoinPosition;
import su.binance_bot.Model.DBCommand;
import su.binance_bot.Model.DBCommandsQueue;
import su.binance_bot.Model.Message;
import su.binance_bot.Model.MessageToSend;
import su.binance_bot.Service.BinanceDataService;
import su.binance_bot.Service.CoinConfigService;
import su.binance_bot.Service.CoinPositionService;

@Component
public class BinanceChecker {

  private final CoinConfigService coinConfigService;
  private final CoinPositionService coinPositionService;
  private final BinanceDataService binanceDataService;
  private final MessageToSend messageToSend;
  private final ChatList chatList;
  private final DBCommandsQueue dbCommandsQueue;
  private List<CoinConfig> coinConfigList;
  private List<CoinPosition> coinPositionList;
  private List<Coin> coinList = new ArrayList<>();
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

  @Autowired
  public BinanceChecker(CoinConfigService coinConfigService, CoinPositionService coinPositionService,
      BinanceDataService binanceDataService, MessageToSend messageToSend, ChatList chatList,
      DBCommandsQueue dbCommandsQueue) {
    this.coinConfigService = coinConfigService;
    this.coinPositionService = coinPositionService;
    this.binanceDataService = binanceDataService;
    this.messageToSend = messageToSend;
    this.chatList = chatList;
    this.dbCommandsQueue = dbCommandsQueue;
  }

  @PostConstruct
  private void getPositions() {
    this.coinConfigList = coinConfigService.getCoinConfigs();
    this.coinPositionList = coinPositionService.getLatestPositions(coinConfigList);

    for (int i = 0; i < this.coinPositionList.size(); i++) {
      for (int j = 0; j < this.coinConfigList.size(); j++) {
        if (this.coinConfigList.get(j).getSymbol() == this.coinPositionList.get(i).getSymbol()) {
          Coin c = new Coin(this.coinPositionList.get(i).getSymbol(), this.coinConfigList.get(j).getAdx(),
              this.coinConfigList.get(j).getGoLong(), this.coinConfigList.get(j).getCloseLong(),
              this.coinConfigList.get(j).getAL(), this.coinConfigList.get(j).getCL(),
              this.coinPositionList.get(i).getPosition(), this.coinPositionList.get(i).getPrice(),
              this.coinPositionList.get(i).getDate());
          this.coinList.add(c);
          break;
        }
      }
    }
  }

  @Scheduled(fixedDelay = 100)
  public void run() {
    for (int i = 0; i < this.coinList.size(); i++) {
      Coin coin = this.coinList.get(i);
      long requestStart = System.currentTimeMillis();

      CoinChanged coinChanged = this.binanceDataService.getPositionChange(coin);
      if (coinChanged != null && coinChanged.getPositionChanged()) {
        System.out.println("Pos changed...");
        String messageContent = "";
        if (coin.getPosition() == PositionEnum.LONG) {
          String messageFirst = coin.positionToString();
          coin.setDate(coinChanged.getDate());
          coin.setPrice(coinChanged.getPrice());
          coin.setPosition(PositionEnum.CLOSED);
          String messageSecond = coin.positionToString();
          messageContent = "Position changed\n" + messageFirst + '\n' + messageSecond;
        } else {
          coin.setDate(coinChanged.getDate());
          coin.setPrice(coinChanged.getPrice());
          coin.setPosition(PositionEnum.LONG);
          messageContent = "Position changed\n" + coin.positionToString();
        }
        System.out.println(messageContent);
        for (int j = 0; j < this.coinPositionList.size(); j++) {
          if (this.coinPositionList.get(j).getSymbol() == coin.getSymbol()) {
            CoinPosition cp = this.coinPositionList.get(j);
            cp.setDate(coinChanged.getDate());
            cp.setPrice(coinChanged.getPrice());
            cp.setPosition(coin.getPosition());
            DBCommand dbCommand = new DBCommand();
            dbCommand.setDbCommandType(DBCommandType.NEWPOSITION);
            dbCommand.setCoinPosition(new CoinPosition(cp.getSymbol(), cp.getPosition(), cp.getPrice(), cp.getDate()));
            try {
              dbCommandsQueue.getDbCommandsQueue().put(dbCommand);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            break;
          }
        }
        for (ChatID chat : this.chatList.getChatIds()) {
          try {
            messageToSend.getMessageQueue().put(new Message(chat.getChatId(), messageContent));
          } catch (InterruptedException e) {
            System.out.println("binanceChecker messageQueue error");
            e.printStackTrace();
          }
        }
      }
      try {
        TimeUnit.MILLISECONDS.sleep(2000 - (System.currentTimeMillis() - requestStart));
      } catch (InterruptedException e) {
        e.printStackTrace();
        System.out.println("Error in BinanceChecker failed to sleep");
      }
    }

  }

  public List<Coin> getCoinList() {
    return this.coinList;
  }

}
