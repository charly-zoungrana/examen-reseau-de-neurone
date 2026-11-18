package net.zoungrana;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class NeuralNetworkTest {
    private static final double[][] INPUTS = {{1}};
    private static final double[][] TARGETS = {{0.75}};

    @Test
    void learnsAndNotifiesInOrder() {
        NeuralNetwork network = linearNetwork();
        List<NetworkState> visited = new ArrayList<>();
        Dashboard dashboard = new Dashboard();
        network.observe(dashboard);
        network.observe((previous, current) -> {
            assertEquals(current, network.state());
            if (current == NetworkState.TRAINING) {
                assertThrows(IllegalStateException.class, () -> network.predict(INPUTS[0]));
            }
            visited.add(current);
        });

        assertEquals(NetworkState.CONSTRUCTION, network.state());
        train(network);

        assertEquals(List.of(NetworkState.TRAINING, NetworkState.TRAINED), visited);
        assertEquals(1, dashboard.visits(NetworkState.TRAINED));
        assertArrayEquals(new double[]{0.75}, predict(network, INPUTS[0]), 1e-9);
        assertThrows(IllegalStateException.class, () -> train(network));
    }

    @Test
    void propagatesThroughTwoHiddenLayers() {
        NeuralNetwork network = new DenseNetworkBuilder(1, 42)
                .hidden(2, new Identity()).hidden(2, new Identity()).build(1, new Identity());
        train(network);
        assertArrayEquals(new double[]{0.75}, predict(network, INPUTS[0]), 1e-7);
    }

    @Test
    void validatesDimensionsAndTimeOfPrediction() {
        NeuralNetwork network = linearNetwork();
        assertThrows(IllegalStateException.class, () -> predict(network, INPUTS[0]));
        assertThrows(IllegalArgumentException.class, () -> AccessContext.as(AccessContext.Role.TRAINER, () -> {
            network.train(new double[][]{{1, 2}}, TARGETS, 1, 0.1);
            return null;
        }));
        assertEquals(NetworkState.CONSTRUCTION, network.state());
        train(network);
        assertThrows(IllegalArgumentException.class, () -> predict(network, new double[]{Double.NaN}));
    }

    @Test
    void clampsWeights() {
        NeuralNetwork network = linearNetwork();
        AccessContext.as(AccessContext.Role.TRAINER, () -> {
            network.train(INPUTS, new double[][]{{10}}, 1, 1);
            return null;
        });
        assertArrayEquals(new double[]{1}, predict(network, INPUTS[0]), 0);
        assertThrows(IllegalArgumentException.class, () -> new Connection(null, null, -0.1));
    }

    @Test
    void rollsBackPartlyUpdatedWeightsOnFailure() {
        Activation unstable = new Activation() {
            public double apply(double x) { return x; }
            public double derivative(double x) {
                return x > 1 ? Double.NaN : 1.0;
            }
        };
        Neuron input = new Neuron(new Identity());
        Neuron output = new Neuron(unstable);
        output.connectFrom(input, 0.5);
        NeuralNetwork network = new NeuralNetwork(List.of(
                new Layer(LayerType.INPUT, List.of(input)), new Layer(LayerType.OUTPUT, List.of(output))));
        List<NetworkState> visited = new ArrayList<>();
        network.observe((previous, current) -> visited.add(current));

        assertThrows(ArithmeticException.class, () -> AccessContext.as(AccessContext.Role.TRAINER, () -> {
            network.train(new double[][]{{1}, {4}}, new double[][]{{1}, {0}}, 1, 0.5);
            return null;
        }));

        assertEquals(0.5, output.incoming().getFirst().weight());
        assertEquals(List.of(NetworkState.TRAINING, NetworkState.CONSTRUCTION), visited);
        train(network);
        assertEquals(NetworkState.TRAINED, network.state());
    }

    @Test
    void adaptsTheLegacyContract() {
        Activation adapter = new LegacyActivationAdapter(new LegacyActivation() {
            public double evaluate(double value) { return value * 2; }
            public double slopeAt(double value) { return 2; }
        });
        assertEquals(6, adapter.apply(3));
        assertEquals(2, adapter.derivative(3));
        assertEquals(0.5, new Sigmoid().apply(0));
    }

    @Test
    void enforcesRolesEvenOnDirectNetworkCalls() {
        NeuralNetwork network = linearNetwork();
        assertThrows(SecurityException.class, () -> network.predict(INPUTS[0]));
        assertThrows(SecurityException.class, () -> AccessContext.as(AccessContext.Role.READER, () -> {
            network.fit(INPUTS, TARGETS, 1, 0.1);
            return null;
        }));
        train(network);
        assertArrayEquals(new double[]{0.75}, predict(network, INPUTS[0]), 1e-9);
        assertThrows(SecurityException.class, () -> network.predict(INPUTS[0]));
    }

    @Test
    void auditsPrivateMethodsAndDeniedCalls() {
        NeuralNetwork network = linearNetwork();
        Logger audit = Logger.getLogger("net.zoungrana.audit");
        Level previous = audit.getLevel();
        List<LogRecord> events = new ArrayList<>();
        Handler handler = new Handler() {
            public void publish(LogRecord record) { events.add(record); }
            public void flush() {}
            public void close() {}
        };
        audit.setLevel(Level.FINER);
        audit.addHandler(handler);
        try {
            assertThrows(SecurityException.class, () -> network.predict(INPUTS[0]));
            AccessContext.as(AccessContext.Role.TRAINER, () -> {
                network.train(INPUTS, TARGETS, 1, 0.1);
                return null;
            });
            assertTrue(events.stream().anyMatch(event -> event.getMessage().startsWith("SUCCES")
                    && event.getParameters()[0].toString().contains("forward")));
            assertTrue(events.stream().anyMatch(event -> event.getMessage().startsWith("ECHEC")
                    && event.getParameters()[1].equals("SecurityException")));
        } finally {
            audit.removeHandler(handler);
            audit.setLevel(previous);
        }
    }

    private static NeuralNetwork linearNetwork() {
        return new DenseNetworkBuilder(1, 42).build(1, new Identity());
    }

    private static void train(NeuralNetwork network) {
        AccessContext.as(AccessContext.Role.TRAINER, () -> {
            network.fit(INPUTS, TARGETS, 500, 0.2);
            return null;
        });
    }

    private static double[] predict(NeuralNetwork network, double[] input) {
        return AccessContext.as(AccessContext.Role.READER, () -> network.predict(input));
    }
}
