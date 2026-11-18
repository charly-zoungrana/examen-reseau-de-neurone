package net.zoungrana;

public final class Sigmoid implements Activation {
    public double apply(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }

    public double derivative(double x) {
        double y = apply(x);
        return y * (1.0 - y);
    }
}
