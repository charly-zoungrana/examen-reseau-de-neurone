package net.zoungrana;

public enum NetworkState {
    CONSTRUCTION {
        @Override void checkTraining() {}
    },
    TRAINING,
    TRAINED {
        @Override void checkPrediction() {}
    };

    void checkTraining() {
        throw new IllegalStateException("Entraînement interdit dans l'état " + this);
    }

    void checkPrediction() {
        throw new IllegalStateException("Prédiction interdite dans l'état " + this);
    }
}
