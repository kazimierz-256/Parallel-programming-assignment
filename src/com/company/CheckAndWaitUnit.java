package com.company;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class CheckAndWaitUnit implements java.util.function.BooleanSupplier {
    private final Predicate<Knight> predicate;
    private final Knight knight;
    private Consumer consumer;

    CheckAndWaitUnit(Predicate<Knight> predicate, Knight knight, Consumer<Knight> consumer) {

        this.predicate = predicate;
        this.knight = knight;
        this.consumer = consumer;
    }

    @Override
    public boolean getAsBoolean() {
        if (predicate.test(knight)) {
            consumer.accept(knight);
            return true;
        }
        return false;
    }
}
