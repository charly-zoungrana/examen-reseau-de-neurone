package net.zoungrana;

import java.util.List;

record Layer(LayerType type, List<Neuron> neurons) {
    Layer {
        neurons = List.copyOf(neurons);
    }
}
