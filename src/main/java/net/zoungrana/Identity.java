package net.zoungrana;

public final class Identity implements Activation {
    public double apply(double x) { return x; }
    public double derivative(double x) { return 1.0; }
}
