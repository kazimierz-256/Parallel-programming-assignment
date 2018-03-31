package com.company;

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
    private KnightState state;

    Knight(int n,
           int id,
           List<Knight> knights,
           Lock partyLock,
           List<WineCup> wineCups,
           List<Plate> plates,
           WineCup centralWine) {
        this.n = n;
        thisKnightId = id;
        this.wineCups = wineCups;
        this.plates = plates;
        this.centralWine = centralWine;
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
                    talk();
                    break;
                case sleeping:
                    drink();
                    break;
                case talking:
                    sleep();
                    break;
            }
        }
        wakeSomeKnightIfNecessary();
        partyLock.unlock();
        // knocked out

    }

    private void talk() {
        var time = PartyHelper.getRandomTime(2);

        state = KnightState.awaitingTalk;
        // watch out for a spurious wakeup, they really do happen!
        while (!isAbleToTalk()) {
            canTalkNow.awaitUninterruptibly();
        }
        state = KnightState.talking;
        System.out.printf("%s TALKING for %f seconds.%n",
                describeOneself(), time / 1000d);
        wakeSomeKnightIfNecessary();
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

        state = KnightState.sleeping;
        System.out.printf("%s SLEEP for %f seconds.%n",
                describeOneself(), time / 1000d);
        wakeSomeKnightIfNecessary();
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
        var time = PartyHelper.getRandomTime();

        state = KnightState.awaitingDrinking;
        // watch out for a spurious wakeup, they really do happen!
        while (!isAbleToDrinkAndEat()) {
            canDrinkAndEatNow.awaitUninterruptibly();
        }
        state = KnightState.drinking;
        System.out.printf("%s DRINKING %d'th for %f seconds.%n",
                describeOneself(), drunkenCups + 1, time / 1000d);
        getWineCup().putGill(centralWine.takeGill());
        var pickle = getPlate().takePickle();
        wakeSomeKnightIfNecessary();
        partyLock.unlock();

        // consume pickle
        consume(getWineCup().takeGill());
        consume(pickle);
        // drink gill
        ++drunkenCups;

        partyLock.lock();
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

    private boolean wakeSomeKnightIfNecessary() {
        if (cycleLeftTillFirst(Knight::isReadyToTalk, Knight::enableTalking)) {
            return true;
        } else return cycleLeftTillFirst(Knight::isReadyToDrinkAndEat, Knight::enableDrinking);
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
