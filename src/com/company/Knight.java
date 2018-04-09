package com.company;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Consumer;
import java.util.function.Predicate;

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
    private Set<Knight> knightsWakeSet;
    private KnightState state;

    Knight(int n,
           int id,
           List<Knight> knights,
           Lock partyLock,
           List<WineCup> wineCups,
           List<Plate> plates,
           WineCup centralWine,
           Queue<CheckAndWaitUnit> knightsWakeQueue,
           Set<Knight> knightsWakeSet) {
        this.n = n;
        thisKnightId = id;
        this.knights = knights;
        this.partyLock = partyLock;
        this.wineCups = wineCups;
        this.plates = plates;
        this.centralWine = centralWine;
        this.knightsWakeQueue = knightsWakeQueue;
        this.knightsWakeSet = knightsWakeSet;

        state = KnightState.sleeping; // so that knights start sleeping
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
                case drinkingAndEating:
                    state = KnightState.awaitingTalk;

                    AddKnightToWakeQueueIfNecessary(
                            previousKnight(),
                            Knight::isAwaitingValidlyToDrinkAndEat,
                            Knight::enableDrinking
                    );
                    AddKnightToWakeQueueIfNecessary(
                            nextKnight(),
                            Knight::isAwaitingValidlyToDrinkAndEat,
                            Knight::enableDrinking
                    );

                    talk();
                    break;
                case sleeping:
                    state = KnightState.awaitingDrinkingAndEating;
                    drink();
                    break;
                case talking:
                    state = KnightState.sleeping;

                    if (amIKing()) {
                        for (Knight knight : knights) {

                            if (knight == this)
                                continue;

                            AddKnightToWakeQueueIfNecessary(
                                    knight,
                                    Knight::isAwaitingValidlyToTalk,
                                    Knight::enableTalking
                            );
                        }
                    } else {

                        AddKnightToWakeQueueIfNecessary(
                                previousKnight(),
                                Knight::isAwaitingValidlyToTalk,
                                Knight::enableTalking
                        );

                        AddKnightToWakeQueueIfNecessary(
                                nextKnight(),
                                Knight::isAwaitingValidlyToTalk,
                                Knight::enableTalking
                        );
                    }

                    sleep();
                    break;
            }
        }

        System.out.printf("%s is knocked out completely.%n", describeOneself());

        partyLock.unlock();
        // knocked out

    }

    private void AddKnightToWakeQueueIfNecessary(Knight knight, Predicate<Knight> predicate, Consumer<Knight> enableDrinking) {
        if (predicate.test(knight) && !knightsWakeSet.contains(knight)) {
            knightsWakeQueue.add(new CheckAndWaitUnit(
                    knight,
                    predicate,
                    enableDrinking
            ));
        }
    }


    private void talk() {
        var time = PartyHelper.getRandomTime(2);

        signalFirstValidUnit();
        // watch out for spurious wake-ups, they really do happen!
        while (!isAbleToTalk()) {
            canTalkNow.awaitUninterruptibly();
            signalFirstValidUnit();
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

        System.out.printf("%s SLEEPING for %f seconds.%n",
                describeOneself(), time / 1000d);

        signalFirstValidUnit();
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
        signalFirstValidUnit();

        // watch out for spurious wake-ups, they really do happen!
        while (!isAbleToDrinkAndEat()) {
            canDrinkAndEatNow.awaitUninterruptibly();
            signalFirstValidUnit();
        }

        state = KnightState.drinkingAndEating;
        System.out.printf("%s DRINKING %d'th immediately.%n",
                describeOneself(), drunkenCups + 1);
        // pour the wine from the central bottle to local cup
        getLocalWineCup().pourGill(centralWine.takeGill());
        partyLock.unlock();

        consume(getLocalWineCup().takeGill());
        ++drunkenCups;
        consume(getLocalPlate().takePickle());

        partyLock.lock();
        System.out.printf("%s finished drinkingAndEating.%n", describeOneself());
        if (drunkenCups == PartyHelper.maximumKnightDrinkingCapacity) {
            state = KnightState.knockedOut;
        }
    }

    private void consume(WineGill gill) {
        //consume wine implementation
    }

    private void consume(Pickle pickle) {
        //consume pickle implementation
    }

    private boolean signalFirstValidUnit() {
        return PartyHelper.signalFirstValidUnit(knightsWakeQueue, knightsWakeSet);
    }

    private WineCup getLocalWineCup() {
        return wineCups.get(thisKnightId / 2);
    }

    private Plate getLocalPlate() {
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

    public boolean isAwaitingValidlyToDrinkAndEat() {
        return state == KnightState.awaitingDrinkingAndEating && isAbleToDrinkAndEat();
    }

    public boolean isAwaitingValidlyToTalk() {
        return state == KnightState.awaitingTalk && isAbleToTalk();
    }

    private boolean isAbleToDrinkAndEat() {
        return previousKnight().getState() != KnightState.drinkingAndEating
                && nextKnight().getState() != KnightState.drinkingAndEating
                && getLocalPlate().hasPickles()
                && centralWine.hasGills();
    }

    private boolean isAbleToTalk() {
        return previousKnight().getState() != KnightState.talking
                && nextKnight().getState() != KnightState.talking
                && (amIKing() || knights.get(PartyHelper.kingId).getState() != KnightState.talking);
    }

    private Knight previousKnight() {
        return knights.get(PartyHelper.previousKnightId(thisKnightId, n));
    }

    private Knight nextKnight() {
        return knights.get(PartyHelper.nextKnightId(thisKnightId, n));
    }

    private String describeOneself() {
        if (amIKing()) {
            return String.format("King   (id %d)", thisKnightId);
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
