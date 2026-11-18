package net.zoungrana;

import java.util.Arrays;
import java.util.Locale;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        NeuralNetwork network = new DenseNetworkBuilder(3, 42)
                .hidden(new Sigmoid(), new Sigmoid(), new Identity(), new Identity(), new Identity())
                .build(2, new Identity());
        Dashboard dashboard = new Dashboard();
        network.observe(dashboard);
        network.observe((previous, current) -> System.out.printf("État : %s → %s%n", previous, current));

        double[][] inputs = {{0, 0, 0}, {1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
        double[][] targets = {{0, 0}, {0.4, 0.1}, {0.2, 0.6}, {0.1, 0.3}};

        System.out.println("Réseau dense 3 → 5 → 2 · graine 42");
        System.out.println("État initial : " + network.state());
        AccessContext.as(AccessContext.Role.TRAINER, () -> {
            network.fit(inputs, targets, 8_000, 0.15);
            return null;
        });

        try {
            network.predict(inputs[1]);
        } catch (SecurityException expected) {
            System.out.println("Accès anonyme : refusé");
        }

        AccessContext.as(AccessContext.Role.READER, () -> {
            System.out.println("Prédictions après entraînement");
            for (int i = 0; i < inputs.length; i++) {
                double[] result = network.predict(inputs[i]);
                System.out.printf(Locale.ROOT, "%s → [%.4f, %.4f]   cible %s%n",
                        Arrays.toString(inputs[i]), result[0], result[1], Arrays.toString(targets[i]));
            }
            return null;
        });

        System.out.printf("Tableau de bord : %d passage en TRAINING, %d passage en TRAINED%n",
                dashboard.visits(NetworkState.TRAINING), dashboard.visits(NetworkState.TRAINED));
    }
}
