package models.Extra.messages;

import java.time.Duration;

public class StartAuctionMessage {
    public String type = "START_AUCTION";
    public long endTimeEpoch;
    public String itemName;
    public String auctionId;
    public double startingPrice;
    public double bidIncrement;
    public StartAuctionMessage() {

    }
    public StartAuctionMessage(long endTimeEpoch, String itemName,  String auctionId,  double startingPrice, double bidIncrement ) {
        this.endTimeEpoch = endTimeEpoch;
        this.itemName = itemName;
        this.auctionId = auctionId;
        this.startingPrice = startingPrice;
        this.bidIncrement = bidIncrement;
    }
}
