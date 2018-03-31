package com.company;

import java.util.ArrayDeque;
import java.util.Queue;

class Plate {

    private final int maximumPlatePickleCapacity;
    private Queue<Pickle> pickles;

    public Plate(int maximumPlatePickleCapacity) {
        pickles = new ArrayDeque<>(maximumPlatePickleCapacity);
        this.maximumPlatePickleCapacity = maximumPlatePickleCapacity;
    }

    public boolean hasPickles() {
        return !pickles.isEmpty();
    }

    public Pickle takePickle() {
        return pickles.remove();
    }

    public boolean putPickle(Pickle pickle) {
        if (pickles.size() >= maximumPlatePickleCapacity) {
            return false;
        } else {
            pickles.add(pickle);
            return true;
        }
    }

    public int missingPickleCount() {
        return maximumPlatePickleCapacity - pickles.size();
    }
}

