package com.company;

import java.util.ArrayDeque;
import java.util.Deque;

class WineCup {
    private Deque<WineGill> gills;
    private final int maximumWineGillCapacity;

    public WineCup(int maximumWineGillCapacity) {
        gills = new ArrayDeque<>(maximumWineGillCapacity);
        this.maximumWineGillCapacity = maximumWineGillCapacity;
    }

    public boolean hasGills() {
        return !gills.isEmpty();
    }

    public WineGill takeGill() {
        return gills.pop();
    }

    public boolean putGill(WineGill gill) {
        if (gills.size() >= maximumWineGillCapacity) {
            return false;
        } else {
            gills.push(gill);
            return true;
        }
    }

    public int missingGillCount() {
        return maximumWineGillCapacity - gills.size();
    }
}
