package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {
    private final Notifier notifier;
    private final Object lock = new Object();

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private volatile Bid latestBid = new Bid(0L, 0L, Long.MIN_VALUE);

    public boolean propose(Bid bid) {
        if (bid.getPrice() <= latestBid.getPrice()) {
        return false;
        }
        Bid lastValueBid;
        synchronized (lock) {
            if (bid.getPrice() <= latestBid.getPrice()) {
                return false;
        }
          lastValueBid = latestBid = bid;
        }
        notifier.sendOutdatedMessage(lastValueBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
