package su.binance_bot;

import java.util.List;
import java.util.stream.Collectors;

import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import su.binance_bot.Enum.CoinSymbolEnum;
import su.binance_bot.Enum.DBCommandType;
import su.binance_bot.Model.ChatID;
import su.binance_bot.Model.Coin;
import su.binance_bot.Model.DBCommand;
import su.binance_bot.Model.DBCommandsQueue;
import su.binance_bot.Model.Message;
import su.binance_bot.Model.MessageToSend;
import su.binance_bot.Runnable.BinanceChecker;

@Component
public class TelegramBot extends TelegramLongPollingBot {
  @Value("${telegram.token}")
  private String token;

  @Value("${telegram.username}")
  private String username;

  @Value("${registration.key}")
  private String registrationKey;

  private final List<Coin> coinList;
  private final ChatList chatList;
  private final MessageToSend messageToSend;
  private final DBCommandsQueue dbCommandsQueue;

  @Autowired
  public TelegramBot(BinanceChecker binanceChecker, ChatList chatList, MessageToSend messageToSend,
      DBCommandsQueue dbCommandsQueue) {
    this.coinList = binanceChecker.getCoinList();
    this.chatList = chatList;
    this.messageToSend = messageToSend;
    this.dbCommandsQueue = dbCommandsQueue;
  }

  @Override
  public void onUpdateReceived(Update update) {
    if (update.hasMessage() && update.getMessage().hasText()) {

      String command = update.getMessage().getText();
      int chatId = Math.toIntExact(update.getMessage().getChatId());
      StringBuilder respondMessage = new StringBuilder();

      System.out.println("Got this msg >> " + update.getMessage().getText() + " | from > " + chatId);

      if (command.startsWith("/status") || command.startsWith("/register") || command.startsWith("/unregister")) {
        String[] symbols = command.trim().replaceAll("\\s+", " ").split(" ");
        if (symbols[0].equalsIgnoreCase("/unregister")) {
          this.unRegister(respondMessage, chatId);
        } else if (symbols[0].equalsIgnoreCase("/register")) {
          // no registation key needed atm
          this.registerNoKey(respondMessage, chatId);
        } else {
          List<String> wantedSymbols = Stream.of(symbols).skip(1).collect(Collectors.toList());
          if (!wantedSymbols.isEmpty()) {
            String commandString = symbols[0];
            switch (commandString) {
            case "/status":
              this.getStatus(respondMessage, wantedSymbols);
              break;
            // case "/register":
            // this.register(respondMessage, wantedSymbols, chatId);
            // break;
            }
          }
        }

      }

      if (!respondMessage.toString().isEmpty()) {
        try {
          messageToSend.getMessageQueue().put(new Message(chatId, respondMessage.toString()));
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      } else {
        System.out.println("Respond message is empty");
      }
    }

  }

  private void unRegister(StringBuilder respondMessage, int requestChatsId) {
    boolean isRegistered = false;
    for (ChatID c : this.chatList.getChatIds()) {
      if (requestChatsId == c.getChatId()) {
        isRegistered = true;
        break;
      }
    }
    if (isRegistered) {
      // TODO need to change this .. should move it to chatList component and make it
      // synchronizable list .. leave it for try/catchat the moment.. slim chances
      // that it will throw an error
      try {
        for (int i = 0; i < this.chatList.getChatIds().size(); i++) {
          if (this.chatList.getChatIds().get(i).getChatId() == requestChatsId) {
            this.chatList.getChatIds().remove(i);
            respondMessage.append("Bye!");
            DBCommand dbCommand = new DBCommand();
            dbCommand.setDbCommandType(DBCommandType.UNREGISTER);
            dbCommand.setChatId(requestChatsId);
            try {
              this.dbCommandsQueue.getDbCommandsQueue().put(dbCommand);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            break;
          }
        }
      } catch (Exception e) {
      }
    }
  }

  private void registerNoKey(StringBuilder respondMessage, int requestChatsId) {
    boolean alreadyRegistered = false;
    for (ChatID c : this.chatList.getChatIds()) {
      if (requestChatsId == c.getChatId()) {
        alreadyRegistered = true;
        break;
      }
    }
    if (!alreadyRegistered) {
      this.chatList.getChatIds().add(new ChatID(requestChatsId));
      respondMessage.append("Welcome!");
      DBCommand dbCommand = new DBCommand();
      dbCommand.setDbCommandType(DBCommandType.REGISTER);
      dbCommand.setChatId(requestChatsId);
      try {
        this.dbCommandsQueue.getDbCommandsQueue().put(dbCommand);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  // TODO 
  private void register(StringBuilder respondMessage, List<String> key, int requestChatsId) {
    String gotKey = key.get(0);
    if (gotKey.equalsIgnoreCase(this.registrationKey)) {
      boolean alreadyRegistered = false;
      for (ChatID c : this.chatList.getChatIds()) {
        if (requestChatsId == c.getChatId()) {
          alreadyRegistered = true;
          break;
        }
      }
      if (!alreadyRegistered) {
        this.chatList.getChatIds().add(new ChatID(requestChatsId));
        respondMessage.append("Welcome !");
        DBCommand dbCommand = new DBCommand();
        dbCommand.setDbCommandType(DBCommandType.REGISTER);
        dbCommand.setChatId(requestChatsId);
        try {
          this.dbCommandsQueue.getDbCommandsQueue().put(dbCommand);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void getStatus(StringBuilder respondMessage, List<String> symbols) {
    if (symbols.size() == 1 && symbols.get(0).contentEquals("all")) {
      for (Coin c : this.coinList) {
        respondMessage.append(c.positionToString() + '\n');
      }
    } else {
      for (String symbol : symbols) {
        CoinSymbolEnum searching = CoinSymbolEnum.BTCUSDT;
        for (CoinSymbolEnum c : CoinSymbolEnum.values()) {
          if (c.name().contains(symbol.toUpperCase())) {
            searching = c;
            break;
          }
        }
        for (int i = 0; i < this.coinList.size(); i++) {
          if (this.coinList.get(i).getSymbol() == searching) {
            if (respondMessage.length() > 1) {
              respondMessage.append("\n" + this.coinList.get(i).positionToString());
            } else {
              respondMessage.append(this.coinList.get(i).positionToString());
            }
            break;
          }
        }
      }
    }
  }

  @Override
  public String getBotUsername() {
    return username;
  }

  @Override
  public String getBotToken() {
    return token;
  }
}