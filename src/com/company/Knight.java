package com.company;

import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class Knight implements Runnable {
    private int drunkenCups = 0;
    private int n;
    private List<Knight> knights;
    private Lock partyLock;
    private Condition canTalkNow;
    private Condition canDrinkAndEatNow;
    private int thisKnightId;
    private List<WineCup> wineCups;
    private List<Plate> plates;
    private WineCup centralWine;
    private Queue<CheckAndWaitUnit> knightsWakeQueue;
    private HashSet<Knight> knightsWakeSet;
    private KnightState state;

    Knight(int n,
           int id,
           List<Knight> knights,
           Lock partyLock,
           List<WineCup> wineCups,
           List<Plate> plates,
           WineCup centralWine, Deque<CheckAndWaitUnit> knightsWakeQueue, HashSet<Knight> knightsWakeSet) {
        this.n = n;
        thisKnightId = id;
        this.wineCups = wineCups;
        this.plates = plates;
        this.centralWine = centralWine;
        this.knightsWakeQueue = knightsWakeQueue;
        this.knightsWakeSet = knightsWakeSet;
        state = KnightState.sleeping; // so that knights start sleeping
        this.partyLock = partyLock;
        this.knights = knights;
        canTalkNow = partyLock.newCondition();
        canDrinkAndEatNow = partyLock.newCondition();
    }

    @Override
    public void run() {
        // this mayhem with partyLock is to ensure that a knight changes his state atomically (or awaits some condition)
        // hence all methods drink/talk/sleep assume acquiring the partyLock lock before beginning of execution
        // and they leave the lock locked after finishing execution
        partyLock.lock();
        sleep();
        while (state != KnightState.knockedOut) {
            switch (state) {
                case drinking:
                    state = KnightState.awaitingTalk;

                    if (previousKnight().isReadyToDrinkAndEat() && !knightsWakeSet.contains(previousKnight())) {
                        knightsWakeQueue.add(new CheckAndWaitUnit(
                                Knight::isReadyToDrinkAndEat,
                                previousKnight(),
                                Knight::enableDrinking
                        ));
                    }
                    if (nextKnight().isReadyToDrinkAndEat() && !knightsWakeSet.contains(nextKnight())) {
                        knightsWakeQueue.add(new CheckAndWaitUnit(
                                Knight::isReadyToDrinkAndEat,
                                nextKnight(),
                                Knight::enableDrinking
                        ));
                    }

                    talk();
                    break;
                case sleeping:
                    state = KnightState.awaitingDrinking;
                    drink();
                    break;
                case talking:
                    state = KnightState.sleeping;

                    if (amIKing()) {
                        for (Knight knight : knights) {
                            if (knight.isReadyToTalk() && !knightsWakeSet.contains(knight)) {
                                knightsWakeQueue.add(new CheckAndWaitUnit(
                                        Knight::isReadyToTalk,
                                        knight,
                                        Knight::enableTalking
                                ));
                            }
                        }
                    } else {
                        if (previousKnight().isReadyToTalk() && !knightsWakeSet.contains(previousKnight())) {
                            knightsWakeQueue.add(new CheckAndWaitUnit(
                                    Knight::isReadyToTalk,
                                    previousKnight(),
                                    Knight::enableTalking
                            ));
                        }
                        if (nextKnight().isReadyToTalk() && !knightsWakeSet.contains(nextKnight())) {
                            knightsWakeQueue.add(new CheckAndWaitUnit(
                                    Knight::isReadyToTalk,
                                    nextKnight(),
                                    Knight::enableTalking
                            ));
                        }
                    }
                    sleep();
                    break;
            }
        }

        System.out.printf("%s is knocked out completely.%n", describeOneself());

        partyLock.unlock();
        // knocked out

    }


    private void talk() {
        var time = PartyHelper.getRandomTime(2);

        PartyHelper.signalFirstUnit(knightsWakeQueue, knightsWakeSet);
        // watch out for a spurious wakeup, they really do happen!
        while (!isAbleToTalk()) {
            canTalkNow.awaitUninterruptibly();
            PartyHelper.signalFirstUnit(knightsWakeQueue, knightsWakeSet);
        }
        state = KnightState.talking;
        System.out.printf("%s TALKING for %f seconds.%n",
                describeOneself(), time / 1000d);
        partyLock.unlock();

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Talk interruption exception");
        }

        partyLock.lock();
        System.out.printf("%s finished talking.%n", describeOneself());
    }

    private void sleep() {
        var time = PartyHelper.getRandomTime();

        System.out.printf("%s SLEEP for %f seconds.%n",
                describeOneself(), time / 1000d);

        PartyHelper.signalFirstUnit(knightsWakeQueue, knightsWakeSet);
        partyLock.unlock();

        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Sleep interruption exception");
        }

        partyLock.lock();
        System.out.printf("%s finished sleeping.%n", describeOneself());
    }

    private void drink() {
        PartyHelper.signalFirstUnit(knightsWakeQueue, knightsWakeSet);
        // watch out for a spurious wakeup, they really do happen!
        while (!isAbleToDrinkAndEat()) {
            canDrinkAndEatNow.awaitUninterruptibly();
            PartyHelper.signalFirstUnit(knightsWakeQueue, knightsWakeSet);
        }
        state = KnightState.drinking;
        System.out.printf("%s DRINKING %d'th immediately.%n",
                describeOneself(), drunkenCups + 1);
        getWineCup().putGill(centralWine.takeGill());
        partyLock.unlock();

        // consume pickle
        consume(getWineCup().takeGill());
        ++drunkenCups;
        consume(getPlate().takePickle());
        // drink gill

        partyLock.lock();
        System.out.printf("%s finished drinking.%n", describeOneself());
        if (drunkenCups >= PartyHelper.maximumKnightDrinkingCapacity) {
            state = KnightState.knockedOut;
        }
    }

    private void consume(WineGill gill) {
        //consume
    }

    private void consume(Pickle pickle) {
        //consume
    }

    private WineCup getWineCup() {
        return wineCups.get(thisKnightId / 2);
    }

    private Plate getPlate() {
        int plateId = (thisKnightId + 1) / 2;
        if (plateId >= n / 2) {
            plateId -= n / 2;
        }
        return plates.get(plateId);
    }

    public void enableDrinking() {
        canDrinkAndEatNow.signal();
    }

    public void enableTalking() {
        canTalkNow.signal();
    }

    public boolean isReadyToDrinkAndEat() {
        return state == KnightState.awaitingDrinking && isAbleToDrinkAndEat();
    }

    public boolean isReadyToTalk() {
        return state == KnightState.awaitingTalk && isAbleToTalk();
    }

    private boolean isAbleToDrinkAndEat() {
        return previousKnight().getState() != KnightState.drinking
                && nextKnight().getState() != KnightState.drinking
                && getPlate().hasPickles()
                && centralWine.hasGills();
    }

    private boolean isAbleToTalk() {
        return nextKnight().getState() != KnightState.talking
                && previousKnight().getState() != KnightState.talking
                && knights.get(PartyHelper.kingId).getState() != KnightState.talking;
    }

    private Knight previousKnight() {
        return knights.get(PartyHelper.previousKnightId(thisKnightId, n));
    }

    private Knight nextKnight() {
        return knights.get(PartyHelper.nextKnightId(thisKnightId, n));
    }

    private String describeOneself() {
        if (amIKing()) {
            return String.format("King (id %d)", thisKnightId);
        } else {
            return String.format("Knight (id %d)", thisKnightId);
        }
    }

    private boolean amIKing() {
        return thisKnightId == PartyHelper.kingId;
    }

    public KnightState getState() {
        return state;
    }
}
