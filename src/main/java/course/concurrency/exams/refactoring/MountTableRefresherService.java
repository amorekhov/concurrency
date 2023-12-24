package course.concurrency.exams.refactoring;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Supplier;


public class MountTableRefresherService {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */

    private ExecutorService refreshersExecutor;

    protected void setRefreshersExecutor(ExecutorService refreshersExecutor) {
        this.refreshersExecutor = refreshersExecutor;
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh()  {
        List<MountTableRefresherRunnable> refreshers = routerStore.getCachedRecords()
                .stream()
                .map(this::createMountTableRefresherRunnable)
                .filter(Objects::nonNull)
                .toList();
        if (!refreshers.isEmpty()) {
            invokeRefresh(refreshers);
        }
    }

    protected MountTableRefresherRunnable createMountTableRefresherRunnable(Others.RouterState routerState) {
        String adminAddress = routerState.getAdminAddress();
        if (adminAddress == null || adminAddress.isBlank()) {
            return null;
        } else {
            Others.MountTableManager mountTableManager = getMountTableManager(adminAddress);
            return new MountTableRefresherRunnable(mountTableManager, adminAddress);
        }
    }

    protected Others.MountTableManager getMountTableManager(String adminAdress) {
        if (adminAdress.contains("local")) {
            /*
             * Local router's cache update does not require RPC call, so no need for
             * RouterClient
             */
            return new Others.MountTableManager("local");
        } else {
            return new Others.MountTableManager(adminAdress);
        }
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<MountTableRefresherRunnable> refreshers) {
        CompletableFuture[] futures = refreshers.stream()
                .map(this::createCompletableFuture)
                .toArray(CompletableFuture[]::new);

        try {
            /*
             * Wait for all the thread to complete, await method returns false if refresh is
             * not finished within specified time
             */
            CompletableFuture.allOf(futures).get(cacheUpdateTimeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException | InterruptedException | ExecutionException e) {
            log("Mount table cache refresher was interrupted.");
        }

        if (!refreshers.stream().allMatch(MountTableRefresherRunnable::isSuccess)) {
            log("Not all router admins updated their cache");
        }

        logResult(refreshers);
    }

    private CompletableFuture<?> createCompletableFuture(MountTableRefresherRunnable refresher) {
        Supplier<Boolean> asyncSupplier = () -> {
            refresher.run();
            return refresher.isSuccess();
        };
        return CompletableFuture.supplyAsync(asyncSupplier, refreshersExecutor).exceptionally(ex -> false);
    }

    private void logResult(List<MountTableRefresherRunnable> refreshers) {
        int successCount = 0;
        int failureCount = 0;
        for (MountTableRefresherRunnable mountTableRefresherRunnable : refreshers) {
            if (mountTableRefresherRunnable.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                // remove RouterClient from cache so that new client is created
                removeFromCache(mountTableRefresherRunnable.getAdminAddress());
            }
        }
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }
    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}