package su.binance_bot.Model;

import java.time.LocalDateTime;

public class CoinChanged {
  
  private boolean positionChanged;
  private float price;
  private LocalDateTime date;


  public CoinChanged(boolean positionChanged, float price, LocalDateTime date) {
    this.positionChanged = positionChanged;
    this.price = price;
    this.date = date;
  }

  public boolean isPositionChanged() {
    return this.positionChanged;
  }

  public boolean getPositionChanged() {
    return this.positionChanged;
  }

  public void setPositionChanged(boolean positionChanged) {
    this.positionChanged = positionChanged;
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
