package net.zoungrana;

public aspect SecurityAspect {
    before(): execution(* NetworkOperations+.train(..)) || execution(* NetworkOperations+.fit(..)) {
        AccessContext.requireTraining();
    }

    before(): execution(* NetworkOperations+.predict(..)) {
        AccessContext.requirePrediction();
    }
}
