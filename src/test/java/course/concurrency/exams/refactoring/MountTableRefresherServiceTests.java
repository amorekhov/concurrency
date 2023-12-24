package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceTests {

    private static final long CACHE_UPDATE_TIMEOUT = 1000L;

    private MountTableRefresherService service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache<?, ?> routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountTableRefresherService();
        service.setCacheUpdateTimeout(CACHE_UPDATE_TIMEOUT);
        service.setRefreshersExecutor(Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
        routerStore = mock(Others.RouterStore.class);
        manager = mock(Others.MountTableManager.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
//         service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(true);

        List<Others.RouterState> states = addresses.stream().map(Others.RouterState::new).toList();
        when(routerStore.getCachedRecords()).thenReturn(states);
        when(mockedService.getMountTableManager(anyString())).thenReturn(manager);

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(false);

        List<Others.RouterState> states = addresses.stream().map(Others.RouterState::new).toList();
        when(routerStore.getCachedRecords()).thenReturn(states);
        when(mockedService.getMountTableManager(anyString())).thenReturn(manager);

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        verify(routerClientsCache, times(addresses.size())).invalidate(anyString());
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        AtomicInteger counter = new AtomicInteger(1);
        when(manager.refresh()).thenAnswer(invocation -> counter.getAndIncrement() % 2 == 0);

        List<Others.RouterState> states = addresses.stream().map(Others.RouterState::new).toList();
        when(routerStore.getCachedRecords()).thenReturn(states);
        when(mockedService.getMountTableManager(anyString())).thenReturn(manager);

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache, times(2)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        AtomicInteger counter = new AtomicInteger(1);
        when(manager.refresh()).thenAnswer(invocation -> {
            int index = counter.getAndIncrement();
            if (index == 1) {
                throw new UnsupportedOperationException();
            }
            return true;
        });

        List<Others.RouterState> states = addresses.stream().map(Others.RouterState::new).toList();
        when(routerStore.getCachedRecords()).thenReturn(states);
        when(mockedService.getMountTableManager(anyString())).thenReturn(manager);

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        AtomicInteger counter = new AtomicInteger(1);
        when(manager.refresh()).thenAnswer(invocation -> {
            int index = counter.getAndIncrement();
            if (index == 1) {
                Thread.sleep(CACHE_UPDATE_TIMEOUT * 2);
            }
            return true;
        });

        List<Others.RouterState> states = addresses.stream().map(Others.RouterState::new).toList();
        when(routerStore.getCachedRecords()).thenReturn(states);
        when(mockedService.getMountTableManager(anyString())).thenReturn(manager);

        // when
        mockedService.refresh();

        // then
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache, times(1)).invalidate(anyString());
    }

    @Test
    @DisplayName("Refresher is interrupted")
    public void interupptRefresher() {
        // given
        MountTableRefresherService mockedService = Mockito.spy(service);
        List<String> addresses = List.of("123", "local6", "789", "local");

        when(manager.refresh()).thenReturn(true);

        List<Others.RouterState> states = addresses.stream().map(Others.RouterState::new).toList();
        when(routerStore.getCachedRecords()).thenReturn(states);
        when(mockedService.getMountTableManager(anyString())).thenReturn(manager);

        // when
        Thread.currentThread().interrupt();
        mockedService.refresh();

        // then
        verify(mockedService).log("Mount table cache refresher was interrupted.");
    }
}