package net.zoungrana;

import java.util.Objects;

public final class LegacyActivationAdapter implements Activation {
    private final LegacyActivation legacy;

    public LegacyActivationAdapter(LegacyActivation legacy) {
        this.legacy = Objects.requireNonNull(legacy);
    }

    public double apply(double x) { return legacy.evaluate(x); }
    public double derivative(double x) { return legacy.slopeAt(x); }
}
