package net.zoungrana;

import java.util.Objects;
import java.util.function.Supplier;

// Point d'intégration réservé au code de confiance qui établit la session.
final class AccessContext {
    enum Role { TRAINER, READER }

    private static final ThreadLocal<Role> CURRENT = new ThreadLocal<>();

    private AccessContext() {}

    static <T> T as(Role role, Supplier<T> action) {
        Role previous = CURRENT.get();
        CURRENT.set(Objects.requireNonNull(role));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    static void requireTraining() {
        if (CURRENT.get() != Role.TRAINER) {
            throw new SecurityException("Entraînement réservé au rôle TRAINER");
        }
    }

    static void requirePrediction() {
        if (CURRENT.get() == null) {
            throw new SecurityException("Prédiction réservée à une session autorisée");
        }
    }
}
