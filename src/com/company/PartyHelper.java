package com.company;

import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

class PartyHelper {
    static final int maximumKnightDrinkingCapacity = 10;
    static final int kingId = 0;
    static final int maximumPlateCucumberCapacity = 5;
    static final int maximumCentralWineGillCapacity = 8;
    static final int maximumLocalWineGillCapacity = 1;
    private static final int minimumTimeMs = 5000;
    private static final int maximumTimeMs = 2 * minimumTimeMs;

    private static Random random = new Random();

    private static int getRandomInt(int minInclusive, int maxExclusive) {
        return minInclusive + random.nextInt(maxExclusive - minInclusive);
    }

    static int getRandomTime() {
        return getRandomInt(minimumTimeMs, maximumTimeMs);
    }

    static int getRandomTime(double slowdownFactor) {
        return (int) (getRandomTime() * slowdownFactor);
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
        return knights.stream().map(Knight::getState).allMatch(state -> state == KnightState.knockedOut);
    }

    public static boolean signalFirstValidUnit(Queue<CheckAndWaitUnit> checkAndWaitUnits, Set<Knight> knightsInQueue) {
        while (checkAndWaitUnits.size() > 0) {
            var checkAndWaitUnit = checkAndWaitUnits.remove();
            knightsInQueue.remove(checkAndWaitUnit.getKnight());
            if (checkAndWaitUnit.getAsBoolean())
                return true;

        }
        return false;
    }

}
