package su.binance_bot.Model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import su.binance_bot.Enum.CoinSymbolEnum;
import su.binance_bot.Enum.PositionEnum;

@Document(collection = "coin_position")
public class CoinPosition {
  @Id
  private String id;

  private CoinSymbolEnum symbol;
  private PositionEnum position;
  private float price;
  private LocalDateTime date;

  public CoinPosition(CoinSymbolEnum symbol, PositionEnum position, float price, LocalDateTime date) {
    this.symbol = symbol;
    this.position = position;
    this.price = price;
    this.date = date;
  }

  public CoinSymbolEnum getSymbol() {
    return this.symbol;
  }

  public void setSymbol(CoinSymbolEnum symbol) {
    this.symbol = symbol;
  }

  public PositionEnum getPosition() {
    return this.position;
  }

  public void setPosition(PositionEnum position) {
    this.position = position;
  }

  public float getPrice() {
    return this.price;
  }

  public void setPrice(float price) {
    this.price = price;
  }

  public LocalDateTime getDate() {
    return this.date;
  }

  public void setDate(LocalDateTime date) {
    this.date = date;
  }
  
}
