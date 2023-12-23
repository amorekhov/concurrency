package course.concurrency.m3_shared.collections;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;

public class RestaurantService {

    private Map<String, Restaurant> restaurantMap = new ConcurrentHashMap<>() {{
        put("A", new Restaurant("A"));
        put("B", new Restaurant("B"));
        put("C", new Restaurant("C"));
    }};

    private final Map<String, LongAdder> stat = new ConcurrentHashMap<>();

    public Restaurant getByName(String restaurantName) {
        addToStat(restaurantName);
        return restaurantMap.get(restaurantName);
    }

    public void addToStat(String restaurantName) {
        LongAdder adder = stat.putIfAbsent(restaurantName, new LongAdder());
        if (adder == null) {
            stat.get(restaurantName).increment();
        } else {
            adder.increment();
        }
    }

    public Set<String> printStat() {
        return stat.entrySet().stream().map(value -> value.getKey() + " - " + value.getValue().toString()).collect(Collectors.toSet());
    }
}
