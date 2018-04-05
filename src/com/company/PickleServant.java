package com.company;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class PickleServant implements Runnable {
    private List<Knight> knights;
    private Lock partyLock;
    private List<Plate> plates;
    private Queue<CheckAndWaitUnit> knightsWakeQueue;
    private Set<Knight> knightsWakeSet;

    public PickleServant(List<Knight> knights, Lock partyLock, List<Plate> plates, ArrayDeque<CheckAndWaitUnit> knightsWakeQueue, Set<Knight> knightsWakeSet) {
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

            PartyHelper.signalFirstUnit(knightsWakeQueue, knightsWakeSet);
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


            plates.forEach(plate -> IntStream.range(0, plate.missingPickleCount()).forEach(i ->
                    plate.putPickle(new Pickle())
            ));


            if (missingPickles) {
                knightsWakeQueue.addAll(
                        knights.stream().
                                filter(Knight::isReadyToDrinkAndEat).
                                filter(knight -> !knightsWakeSet.contains(knight)).
                                map(knight -> new CheckAndWaitUnit(
                                        Knight::isReadyToDrinkAndEat,
                                        knight,
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
