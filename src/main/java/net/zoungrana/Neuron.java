package net.zoungrana;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class Neuron {
    private final Activation activation;
    private final List<Connection> incoming = new ArrayList<>();
    double sum;
    double output;
    double delta;

    Neuron(Activation activation) {
        this.activation = Objects.requireNonNull(activation);
    }

    List<Connection> incoming() { return List.copyOf(incoming); }

    void connectFrom(Neuron source, double weight) {
        incoming.add(new Connection(source, this, weight));
    }

    void evaluate() {
        sum = 0.0;
        for (Connection connection : incoming) {
            sum += connection.source().output * connection.weight();
        }
        output = activation.apply(sum);
        if (!Double.isFinite(output)) {
            throw new ArithmeticException("Sortie neuronale non finie");
        }
    }

    void backpropagate(double error) {
        delta = error * activation.derivative(sum);
        if (!Double.isFinite(delta)) {
            throw new ArithmeticException("Gradient non fini");
        }
    }
}
