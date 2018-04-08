package com.company;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class CheckAndWaitUnit implements java.util.function.BooleanSupplier {
    private final Knight knight;
    private final Predicate<Knight> predicate;
    private final Consumer<Knight> consumer;

    CheckAndWaitUnit(Knight knight, Predicate<Knight> predicate, Consumer<Knight> consumer) {

        this.knight = knight;
        this.predicate = predicate;
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

    public Knight getKnight() {
        return knight;
    }
}
