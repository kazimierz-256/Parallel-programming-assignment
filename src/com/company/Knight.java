package com.company;

import java.util.Deque;
import java.util.List;
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
    private Deque<CheckAndWaitUnit> knightsToCheckAndWake;
    private KnightState state;

    Knight(int n,
           int id,
           List<Knight> knights,
           Lock partyLock,
           List<WineCup> wineCups,
           List<Plate> plates,
           WineCup centralWine, Deque<CheckAndWaitUnit> knightsToCheckAndWake) {
        this.n = n;
        thisKnightId = id;
        this.wineCups = wineCups;
        this.plates = plates;
        this.centralWine = centralWine;
        this.knightsToCheckAndWake = knightsToCheckAndWake;
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

                    if (previousKnight().isReadyToDrinkAndEat()) {
                        knightsToCheckAndWake.addLast(new CheckAndWaitUnit(
                                Knight::isReadyToDrinkAndEat,
                                previousKnight(),
                                Knight::enableDrinking
                        ));
                    }
                    if (nextKnight().isReadyToDrinkAndEat()) {
                        knightsToCheckAndWake.addLast(new CheckAndWaitUnit(
                                Knight::isReadyToDrinkAndEat,
                                nextKnight(),
                                Knight::enableDrinking
                        ));
                    }

                    PartyHelper.signalFirstUnit(knightsToCheckAndWake);

                    talk();
                    break;
                case sleeping:
                    state = KnightState.awaitingDrinking;

                    PartyHelper.signalFirstUnit(knightsToCheckAndWake);

                    drink();
                    break;
                case talking:
                    state = KnightState.sleeping;

                    if (amIKing()) {
                        for (Knight knight : knights) {
                            if (knight.isReadyToTalk()) {
                                knightsToCheckAndWake.push(new CheckAndWaitUnit(
                                        Knight::isReadyToTalk,
                                        knight,
                                        Knight::enableTalking
                                ));
                            }
                        }
                    } else {
                        if (previousKnight().isReadyToTalk()) {
                            knightsToCheckAndWake.addLast(new CheckAndWaitUnit(
                                    Knight::isReadyToTalk,
                                    previousKnight(),
                                    Knight::enableTalking
                            ));
                        }
                        if (nextKnight().isReadyToTalk()) {
                            knightsToCheckAndWake.addLast(new CheckAndWaitUnit(
                                    Knight::isReadyToTalk,
                                    nextKnight(),
                                    Knight::enableTalking
                            ));
                        }
                    }

                    PartyHelper.signalFirstUnit(knightsToCheckAndWake);

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

        // watch out for a spurious wakeup, they really do happen!
        while (!isAbleToTalk()) {
            canTalkNow.awaitUninterruptibly();
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
        // watch out for a spurious wakeup, they really do happen!
        while (!isAbleToDrinkAndEat()) {
            canDrinkAndEatNow.awaitUninterruptibly();
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
        if (drunkenCups >= PartyHelper.maximumKnightDrinkingCapacity) {
            state = KnightState.knockedOut;
        }
        wakeSomeKnightToDrinkAndEat();
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

//    private boolean wakeSomeKnightIfNecessary() {
//        if (cycleLeftTillFirst(Knight::isReadyToTalk, Knight::enableTalking)) {
//            return true;
//        } else return cycleLeftTillFirst(Knight::isReadyToDrinkAndEat, Knight::enableDrinking);
//    }

    private boolean wakeSomeKnightToTalk() {
        return cycleLeftTillFirst(Knight::isReadyToTalk, Knight::enableTalking);
    }

    private boolean wakeSomeKnightToDrinkAndEat() {
        return cycleLeftTillFirst(Knight::isReadyToDrinkAndEat, Knight::enableDrinking);
    }

    private boolean cycleLeftTillFirst(Predicate<Knight> predicate, Consumer<Knight> consumer) {
        for (int i = PartyHelper.previousKnightId(thisKnightId, n);
             i != thisKnightId;
             i = PartyHelper.previousKnightId(i, n)) {
            if (predicate.test(knights.get(i))) {
                consumer.accept(knights.get(i));
                return true;
            }
        }
        return false;
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
            return String.format("King (id %d).", thisKnightId);
        } else {
            return String.format("Knight (id %d).", thisKnightId);
        }
    }

    private boolean amIKing() {
        return thisKnightId == PartyHelper.kingId;
    }

//    private Knight getCurrentKnight() {
//        return knights.get(thisKnightId);
//    }

    private Knight getKing() {
        return knights.get(PartyHelper.kingId);
    }

    public KnightState getState() {
        return state;
    }

    public void setState(KnightState state) {
        this.state = state;
    }
}
