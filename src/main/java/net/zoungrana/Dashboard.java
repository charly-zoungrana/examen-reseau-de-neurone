package net.zoungrana;

import java.util.EnumMap;
import java.util.Map;

public final class Dashboard implements NetworkObserver {
    private final Map<NetworkState, Integer> transitions = new EnumMap<>(NetworkState.class);

    @Override
    public synchronized void onStateChanged(NetworkState previous, NetworkState current) {
        transitions.merge(current, 1, Integer::sum);
    }

    public synchronized int visits(NetworkState state) {
        return transitions.getOrDefault(state, 0);
    }
}
