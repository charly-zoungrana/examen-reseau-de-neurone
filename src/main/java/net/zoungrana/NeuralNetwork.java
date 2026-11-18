package net.zoungrana;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NeuralNetwork implements NetworkOperations {
    private static final Logger LOGGER = Logger.getLogger(NeuralNetwork.class.getName());
    private final List<Layer> layers;
    private final List<Connection> connections;
    private final List<NetworkObserver> observers = new ArrayList<>();
    private volatile NetworkState state = NetworkState.CONSTRUCTION;

    NeuralNetwork(List<Layer> layers) {
        this.layers = List.copyOf(layers);
        connections = layers.stream()
                .flatMap(layer -> layer.neurons().stream())
                .flatMap(neuron -> neuron.incoming().stream())
                .toList();
    }

    public NetworkState state() {
        return state;
    }

    public synchronized void observe(NetworkObserver observer) {
        observers.add(Objects.requireNonNull(observer));
    }

    @Override
    public void fit(double[][] inputs, double[][] targets, int epochs, double rate) {
        train(inputs, targets, epochs, rate);
    }

    @Override
    public void train(double[][] inputs, double[][] targets, int epochs, double rate) {
        state.checkTraining();
        synchronized (this) {
            state.checkTraining();
            if (inputs == null || targets == null || inputs.length == 0 || inputs.length != targets.length
                    || epochs < 1 || !Double.isFinite(rate) || rate <= 0) {
                throw new IllegalArgumentException("Paramètres d'entraînement invalides");
            }
            double[][] samples = copyVectors(inputs, layers.getFirst().neurons().size());
            double[][] expected = copyVectors(targets, layers.getLast().neurons().size());
            double[] initialWeights = connections.stream().mapToDouble(Connection::weight).toArray();
            boolean complete = false;
            try {
                transition(NetworkState.TRAINING);
                for (int epoch = 0; epoch < epochs; epoch++) {
                    for (int sample = 0; sample < samples.length; sample++) {
                        forward(samples[sample]);
                        backward(expected[sample]);
                        for (Connection connection : connections) {
                            connection.update(rate);
                        }
                    }
                }
                complete = true;
            } finally {
                if (!complete) {
                    for (int i = 0; i < connections.size(); i++) {
                        connections.get(i).setWeight(initialWeights[i]);
                    }
                    transition(NetworkState.CONSTRUCTION);
                }
            }
            transition(NetworkState.TRAINED);
        }
    }

    @Override
    public double[] predict(double[] input) {
        state.checkPrediction();
        synchronized (this) {
            double[] vector = copyVector(input, layers.getFirst().neurons().size());
            forward(vector);
            return layers.getLast().neurons().stream().mapToDouble(neuron -> neuron.output).toArray();
        }
    }

    private void forward(double[] input) {
        List<Neuron> first = layers.getFirst().neurons();
        for (int i = 0; i < input.length; i++) {
            first.get(i).output = input[i];
        }
        for (int i = 1; i < layers.size(); i++) {
            for (Neuron neuron : layers.get(i).neurons()) {
                neuron.evaluate();
            }
        }
    }

    private void backward(double[] target) {
        List<Neuron> output = layers.getLast().neurons();
        for (int i = 0; i < output.size(); i++) {
            Neuron neuron = output.get(i);
            neuron.backpropagate(neuron.output - target[i]);
        }
        for (int i = layers.size() - 2; i > 0; i--) {
            for (Neuron neuron : layers.get(i).neurons()) {
                neuron.delta = 0;
            }
            for (Neuron next : layers.get(i + 1).neurons()) {
                for (Connection connection : next.incoming()) {
                    connection.source().delta += connection.weight() * next.delta;
                }
            }
            for (Neuron neuron : layers.get(i).neurons()) {
                neuron.backpropagate(neuron.delta);
            }
        }
    }

    private void transition(NetworkState next) {
        NetworkState previous = state;
        state = next;
        for (NetworkObserver observer : List.copyOf(observers)) {
            try {
                observer.onStateChanged(previous, next);
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Échec de notification d’un observateur", exception);
            }
        }
    }

    private static double[][] copyVectors(double[][] vectors, int width) {
        double[][] copy = new double[vectors.length][];
        for (int i = 0; i < vectors.length; i++) {
            copy[i] = copyVector(vectors[i], width);
        }
        return copy;
    }

    private static double[] copyVector(double[] vector, int width) {
        if (vector == null || vector.length != width) {
            throw new IllegalArgumentException("Dimension attendue : " + width);
        }
        double[] copy = vector.clone();
        for (double value : copy) {
            if (!Double.isFinite(value)) {
                throw new IllegalArgumentException("Les valeurs doivent être finies");
            }
        }
        return copy;
    }
}
