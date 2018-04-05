package com.company;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

class PickleServant implements Runnable {
    private List<Knight> knights;
    private Lock partyLock;
    private List<Plate> plates;
    private ArrayDeque<CheckAndWaitUnit> knightsToCheckAndWake;

    public PickleServant(List<Knight> knights, Lock partyLock, List<Plate> plates, ArrayDeque<CheckAndWaitUnit> knightsToCheckAndWake) {
        this.knights = knights;
        this.partyLock = partyLock;
        this.plates = plates;
        this.knightsToCheckAndWake = knightsToCheckAndWake;
    }

    @Override
    public void run() {
        partyLock.lock();
        while (true) {
            int time;

            time = PartyHelper.getRandomTime(0.5);

            PartyHelper.signalFirstUnit(knightsToCheckAndWake);
            partyLock.unlock();


            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            partyLock.lock();
            if (PartyHelper.areEveryoneKnockedOut(knights))
                break;
            System.out.println("Servant added pickles if necessary");
            plates.forEach(plate -> IntStream.range(0, plate.missingPickleCount()).forEach(i ->
                    plate.putPickle(new Pickle())
            ));
        }
        partyLock.unlock();

    }
}
