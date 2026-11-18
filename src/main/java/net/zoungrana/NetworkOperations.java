package net.zoungrana;

public interface NetworkOperations {
    void fit(double[][] inputs, double[][] targets, int epochs, double rate);
    void train(double[][] inputs, double[][] targets, int epochs, double rate);
    double[] predict(double[] input);
}
