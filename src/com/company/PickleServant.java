package com.company;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

class PickleServant implements Runnable {
    private List<Knight> knights;
    private Lock partyLock;
    private List<Plate> plates;

    public PickleServant(List<Knight> knights, Lock partyLock, List<Plate> plates) {
        this.knights = knights;
        this.partyLock = partyLock;
        this.plates = plates;
    }

    @Override
    public void run() {
        partyLock.lock();
        while (true) {
            int time;

            time = PartyHelper.getRandomTime(2);

            PartyHelper.wakeUpAnybody(knights);
            partyLock.unlock();


            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            partyLock.lock();
            if (PartyHelper.areEveryoneKnockedOut(knights))
                break;
            System.out.println("Servant added pickles");
            plates.forEach(plate -> IntStream.range(0, plate.missingPickleCount()).forEach(i ->
                    plate.putPickle(new Pickle())
            ));
        }
        partyLock.unlock();

    }
}
