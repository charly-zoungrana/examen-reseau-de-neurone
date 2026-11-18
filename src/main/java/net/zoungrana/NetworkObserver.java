package net.zoungrana;

@FunctionalInterface
public interface NetworkObserver {
    void onStateChanged(NetworkState previous, NetworkState current);
}
