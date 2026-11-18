package net.zoungrana;

final class Connection {
    private final Neuron source;
    private final Neuron destination;
    private double weight;

    Connection(Neuron source, Neuron destination, double weight) {
        this.source = source;
        this.destination = destination;
        setWeight(weight);
    }

    Neuron source() { return source; }
    Neuron destination() { return destination; }
    double weight() { return weight; }

    void setWeight(double weight) {
        if (!(weight >= 0.0 && weight <= 1.0)) {
            throw new IllegalArgumentException("Le poids doit appartenir à [0, 1]");
        }
        this.weight = weight;
    }

    void update(double rate) {
        double next = weight - rate * destination.delta * source.output;
        setWeight(Math.clamp(next, 0.0, 1.0));
    }
}
