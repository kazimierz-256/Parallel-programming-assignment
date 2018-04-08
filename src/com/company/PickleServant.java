package com.company;

import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class PickleServant implements Runnable {
    private List<Knight> knights;
    private Lock partyLock;
    private List<Plate> plates;
    private Queue<CheckAndWaitUnit> knightsWakeQueue;
    private Set<Knight> knightsWakeSet;

    public PickleServant(
            List<Knight> knights,
            Lock partyLock,
            List<Plate> plates,
            Queue<CheckAndWaitUnit> knightsWakeQueue,
            Set<Knight> knightsWakeSet) {
        this.knights = knights;
        this.partyLock = partyLock;
        this.plates = plates;
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

            boolean missingPickles = plates.stream().map(Plate::missingPickleCount).anyMatch(count -> count > 0);

            if (missingPickles)
                System.out.println("PickleServant added some pickles");
            else
                System.out.println("PickleServant realised nobody ate any pickles");

            for (var plate : plates) {
                while (plate.missingPickleCount() > 0) {
                    plate.putPickle(new Pickle());
                }
            }

            if (missingPickles) {
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
        System.out.println("PickleServant has done his job");
    }
}
