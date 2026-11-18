package net.zoungrana;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public final class DenseNetworkBuilder {
    private final List<List<Activation>> hiddenLayers = new ArrayList<>();
    private final int inputs;
    private final long seed;

    public DenseNetworkBuilder(int inputs, long seed) {
        if (inputs < 1) {
            throw new IllegalArgumentException("Il faut au moins une entrée");
        }
        this.inputs = inputs;
        this.seed = seed;
    }

    public DenseNetworkBuilder hidden(int size, Activation activation) {
        hiddenLayers.add(repeat(size, activation));
        return this;
    }

    public DenseNetworkBuilder hidden(Activation... activations) {
        hiddenLayers.add(requireNonEmpty(List.of(activations)));
        return this;
    }

    public NeuralNetwork build(int outputs, Activation activation) {
        return buildWith(repeat(outputs, activation));
    }

    public NeuralNetwork build(Activation... activations) {
        return buildWith(requireNonEmpty(List.of(activations)));
    }

    private NeuralNetwork buildWith(List<Activation> outputs) {
        List<Layer> layers = new ArrayList<>();
        Random random = new Random(seed);
        layers.add(new Layer(LayerType.INPUT,
                repeat(inputs, new Identity()).stream().map(Neuron::new).toList()));
        for (List<Activation> hidden : hiddenLayers) {
            append(layers, LayerType.HIDDEN, hidden, random);
        }
        append(layers, LayerType.OUTPUT, outputs, random);
        return new NeuralNetwork(layers);
    }

    private static void append(List<Layer> layers, LayerType type,
                               List<Activation> activations, Random random) {
        List<Neuron> previous = layers.getLast().neurons();
        List<Neuron> neurons = activations.stream().map(Neuron::new).toList();
        for (Neuron neuron : neurons) {
            for (Neuron source : previous) {
                neuron.connectFrom(source, random.nextDouble() / previous.size());
            }
        }
        layers.add(new Layer(type, neurons));
    }

    private static List<Activation> repeat(int size, Activation activation) {
        return requireNonEmpty(Collections.nCopies(size, Objects.requireNonNull(activation)));
    }

    private static List<Activation> requireNonEmpty(List<Activation> activations) {
        if (activations.isEmpty()) {
            throw new IllegalArgumentException("Une couche doit contenir des neurones");
        }
        return activations;
    }
}
