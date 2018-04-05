package com.company;

import java.util.Deque;
import java.util.List;
import java.util.Random;

class PartyHelper {
    static final int maximumKnightDrinkingCapacity = 10;
    static final int kingId = 0;
    static final int maximumPlateCucumberCapacity = 5;
    static final int maximumCentralWineGillCapacity = 8;
    private static final int minimumTimeMs = 10;
    private static final int maximumTimeMs = 2 * minimumTimeMs;

    private static Random random = new Random();

    private static int getRandomInt(int minInclusive, int maxExclusive) {
        return minInclusive + random.nextInt(maxExclusive - minInclusive);
    }

    static int getRandomTime() {
        return getRandomInt(minimumTimeMs, maximumTimeMs);
    }

    static int getRandomTime(double factor) {
        return (int) (getRandomTime() * factor);
    }

    static int previousKnightId(int id, int n) {
        int tmp = id - 1;
        if (tmp < 0) {
            tmp += n;
        }
        return tmp;
    }

    static int nextKnightId(int id, int n) {
        int tmp = id + 1;
        if (tmp >= n) {
            tmp -= n;
        }
        return tmp;
    }

    static boolean areEveryoneKnockedOut(List<Knight> knights) {
        for (Knight knight : knights) {
            if (knight.getState() != KnightState.knockedOut) {
                return false;
            }
        }
        return true;
    }

    public static boolean signalFirstUnit(Deque<CheckAndWaitUnit> checkAndWaitUnits) {
        while (checkAndWaitUnits.size() > 0) {
            if (checkAndWaitUnits.pollFirst().getAsBoolean())
                return true;
        }
        return false;
    }

}
