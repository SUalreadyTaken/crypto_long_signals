package su.binance_bot.Runnable;

import java.util.concurrent.TimeUnit;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import su.binance_bot.TelegramBot;
import su.binance_bot.Model.AllSleep;
import su.binance_bot.Model.Message;
import su.binance_bot.Model.MessageToSend;

public class ExecuteMessages implements Runnable {

  private final MessageToSend messageToSend;
  private final TelegramBot telegramBot;
  private final AllSleep allSleep;
  private int MESSAGES_SENT = 0;
  private long LAST_MESSAGE_SENT = System.currentTimeMillis();

  public ExecuteMessages(MessageToSend messageToSend, TelegramBot telegramBot, AllSleep allSleep) {
    this.messageToSend = messageToSend;
    this.telegramBot = telegramBot;
    this.allSleep = allSleep;
  }

  @Override
  public void run() {
    while (true) {
      try {
        Message message = messageToSend.getMessageQueue().take();
        while (allSleep.getIsSleep()) {
          System.out.println("ExMsg needs to sleep");
          TimeUnit.MILLISECONDS.sleep(1000);
        }
        SendMessage sendMessage = new SendMessage(message.getChatId(), message.getText());
        telegramBot.execute(sendMessage);
        sleepIfNeeded();
      } catch (InterruptedException e) {
        System.out.println("ExecuteMessage queue error");
        e.printStackTrace();
      } catch (TelegramApiException e) {
        System.out.println("ExecuteMessage sendMessage error");
        System.out.println();
        e.printStackTrace();
      }
      LAST_MESSAGE_SENT = System.currentTimeMillis();
    }
  }

  public void sleepIfNeeded() {
    MESSAGES_SENT++;
    if (MESSAGES_SENT >= 30) {
      try {
        TimeUnit.MILLISECONDS.sleep(1000);
        MESSAGES_SENT = 0;
      } catch (InterruptedException e) {
        System.out.println("ExecuteMessage error is sleeping");
        e.printStackTrace();
      }
    } else if (System.currentTimeMillis() - LAST_MESSAGE_SENT >= 1000) {
      MESSAGES_SENT = 1;
    }
  }

}
