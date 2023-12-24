package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final AtomicMarkableReference<Bid> latestBid = new AtomicMarkableReference<>(new Bid(0L, 0L, Long.MIN_VALUE), false);

    public boolean propose(Bid bid) {
        Bid lastValueBid;
        do {
            if (latestBid.isMarked()) {
                return false;
            }
            lastValueBid = latestBid.getReference();
            if (bid.getPrice() <= lastValueBid.getPrice()) {
                return false;
            }
         } while (!latestBid.compareAndSet(lastValueBid, bid, false, false));


            notifier.sendOutdatedMessage(lastValueBid);

        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        Bid lastValue;
        do {
            lastValue = latestBid.getReference();
        } while (!latestBid.attemptMark(lastValue, true));
        return lastValue;
    }
}
