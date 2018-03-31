package com.company;

import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

class Main {

    public static void main(String[] args) {
        System.out.println("Please enter an even integer greater than or equal to 4.");
        var scanner = new Scanner(System.in);
        int n;
        do {
            n = scanner.nextInt();
        } while (n < 4 || n % 2 == 1);
        // n is correct

        // generate necessary arrays and threads
        var threads = new ArrayList<Thread>(n);
        var knights = new ArrayList<Knight>(n);
        var plates = new ArrayList<Plate>(n / 2);
        var wineCups = new ArrayList<WineCup>(n / 2);
        var centralWine = new WineCup(PartyHelper.maximumCentralWineGillCapacity);
        var partyLock = new ReentrantLock();
        final var nFinal = n;
        IntStream.range(0, n / 2).forEach(i -> {
            plates.add(new Plate(PartyHelper.maximumPlateCucumberCapacity));
            wineCups.add(new WineCup(1));
        });

        IntStream.range(0, n).forEach(i -> knights.add(
                new Knight(nFinal, i, knights, partyLock, wineCups, plates, centralWine)
        ));

        // create threads
        knights.forEach(knight -> threads.add(new Thread(knight)));

        threads.add(new Thread(new PickleServant(knights, partyLock, plates)));
        threads.add(new Thread(new WineGillServant(knights, partyLock, centralWine)));

        threads.forEach(Thread::start);

        // join on threads and end the party
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Party ended successfully");
    }


}
