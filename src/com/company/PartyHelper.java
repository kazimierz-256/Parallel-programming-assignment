package com.company;

import java.util.List;
import java.util.Random;

class PartyHelper {
    static final int maximumKnightDrinkingCapacity = 10;
    static final int kingId = 0;
    static final int maximumPlateCucumberCapacity = 3;
    static final int maximumCentralWineGillCapacity = 4;
    private static final int minimumTimeMs = 10;
    private static final int maximumTimeMs = 2 * minimumTimeMs;

    private static Random random = new Random();

    private static int getRandomInt(int minInclusive, int maxExclusive) {
        return minInclusive + random.nextInt(maxExclusive - minInclusive);
    }

    static int getRandomTime() {
        return getRandomInt(minimumTimeMs, maximumTimeMs);
    }

    static int getRandomTime(int factor) {
        return getRandomTime() * factor;
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

    static boolean wakeUpAnybody(List<Knight> knights) {
        for (Knight knight : knights) {
            if (knight.isReadyToDrinkAndEat()) {
                knight.enableDrinking();
                return true;
            } else if (knight.isReadyToTalk()) {
                knight.enableTalking();
                return true;
            }
        }
        return false;
    }
}
