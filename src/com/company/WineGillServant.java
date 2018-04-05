package com.company;

import java.util.ArrayDeque;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

class WineGillServant implements Runnable {
    private List<Knight> knights;
    private Lock partyLock;
    private WineCup centralWine;
    private ArrayDeque<CheckAndWaitUnit> knightsToCheckAndWake;

    public WineGillServant(List<Knight> knights, Lock partyLock, WineCup centralWine, ArrayDeque<CheckAndWaitUnit> knightsToCheckAndWake) {
        this.knights = knights;
        this.partyLock = partyLock;
        this.centralWine = centralWine;
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

            if (centralWine.missingGillCount() > 0)
                System.out.format("Servant added %d wine gills to central wine%n",
                        centralWine.missingGillCount());
            else
                System.out.printf("Servant realised nobody drank the central wine%n");

            IntStream.range(0, centralWine.missingGillCount()).forEach(i ->
                    centralWine.putGill(new WineGill())
            );


        }
        partyLock.unlock();
    }
}
