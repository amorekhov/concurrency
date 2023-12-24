package course.concurrency.exams.refactoring;

import static course.concurrency.exams.refactoring.Others.MountTableManager;

public class MountTableRefresherRunnable implements Runnable {

    private boolean success;
    private final String adminAddress;
    private final MountTableManager manager;

    public MountTableRefresherRunnable(MountTableManager manager,
                                       String adminAddress) {
        this.manager = manager;
        this.adminAddress = adminAddress;
    }

    @Override
    public void run() {
        success = manager.refresh();
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "MountTableRefreshThread [success=" + success + ", adminAddress="
                + adminAddress + "]";
    }

    public String getAdminAddress() {
        return adminAddress;
    }
}
