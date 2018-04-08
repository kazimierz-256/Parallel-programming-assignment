package com.company;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class WineGillServant implements Runnable {
    private List<Knight> knights;
    private Lock partyLock;
    private WineCup centralWine;
    private Queue<CheckAndWaitUnit> knightsWakeQueue;
    private Set<Knight> knightsWakeSet;

    public WineGillServant(
            List<Knight> knights,
            Lock partyLock,
            WineCup centralWine,
            Queue<CheckAndWaitUnit> knightsWakeQueue,
            Set<Knight> knightsWakeSet) {
        this.knights = knights;
        this.partyLock = partyLock;
        this.centralWine = centralWine;
        this.knightsWakeQueue = knightsWakeQueue;
        this.knightsWakeSet = knightsWakeSet;
    }


    @Override
    public void run() {
        partyLock.lock();
        while (true) {
            int time;
            time = PartyHelper.getRandomTime(0.5);

            PartyHelper.signalFirstValidUnit(knightsWakeQueue, knightsWakeSet);
            partyLock.unlock();

            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            partyLock.lock();
            if (PartyHelper.areEveryoneKnockedOut(knights))
                break;

            boolean missingGills = centralWine.missingGillCount() > 0;
            if (missingGills)
                System.out.format("WineServant added %d wine gills to central wine%n", centralWine.missingGillCount());
            else
                System.out.printf("WineServant realised nobody drank the central wine%n");

            while (centralWine.missingGillCount() > 0) {
                centralWine.pourGill(new WineGill());
            }

            if (missingGills) {
                knightsWakeQueue.addAll(
                        knights.stream().
                                filter(Knight::isAwaitingValidlyToDrinkAndEat).
                                filter(((Predicate<Knight>) knightsWakeSet::contains).negate()).
                                map(knight -> new CheckAndWaitUnit(
                                        knight,
                                        Knight::isAwaitingValidlyToDrinkAndEat,
                                        Knight::enableDrinking
                                )).
                                collect(Collectors.toList())
                );
            }

        }
        partyLock.unlock();
        System.out.println("WineServant has done his job");
    }
}
