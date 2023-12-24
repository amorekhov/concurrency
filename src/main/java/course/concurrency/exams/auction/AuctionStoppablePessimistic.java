package course.concurrency.exams.auction;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private Notifier notifier;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private Bid latestBid = new Bid(Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE);
    private boolean isStopped = false;

    public boolean propose(Bid bid) {
        lock.writeLock().lock();
        try {
            if (!isStopped && bid.getPrice().compareTo(latestBid.getPrice()) > 0) {
                notifier.sendOutdatedMessage(latestBid);
                latestBid = bid;
                return true;
            }
        } finally {
            lock.writeLock().unlock();
        }

        return false;
    }

    public Bid getLatestBid() {
        lock.readLock().lock();
        try {
            return latestBid;
        } finally {
            lock.readLock().unlock();
        }
    }

    public Bid stopAuction() {
        lock.writeLock().lock();
        try {
            isStopped = true;
            return latestBid;
        } finally {
            lock.writeLock().unlock();
        }
    }
}