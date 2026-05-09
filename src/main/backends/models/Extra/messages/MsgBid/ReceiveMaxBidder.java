package models.Extra.messages.MsgBid;

public class ReceiveMaxBidder {
    public String type = "RECEIVE_BID";
    public ServerBidRespond maxBidder;
    public ReceiveMaxBidder() {}
    public ReceiveMaxBidder(ServerBidRespond maxBidder) {
        this.maxBidder = maxBidder;
    }
}
