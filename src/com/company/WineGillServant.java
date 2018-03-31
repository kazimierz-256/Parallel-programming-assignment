package com.company;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.stream.IntStream;

class WineGillServant implements Runnable {
    private List<Knight> knights;
    private Lock partyLock;
    private WineCup centralWine;

    public WineGillServant(List<Knight> knights, Lock partyLock, WineCup centralWine) {
        this.knights = knights;
        this.partyLock = partyLock;
        this.centralWine = centralWine;
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
            System.out.println("Servant added wine gills to central wine");
            IntStream.range(0, centralWine.missingGillCount()).forEach(i ->
                    centralWine.putGill(new WineGill())
            );


        }
        partyLock.unlock();
    }
}
