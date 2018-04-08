package com.company;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class Main {

    public static void main(String[] args) {
        System.out.println("Please enter an even integer greater than or equal to 4.");
        var scanner = new Scanner(System.in);
        int n = scanner.nextInt();
        while (n < 4 || n % 2 != 0) {
            System.out.println("Invalid integer, please try again");
            n = scanner.nextInt();
        }
        // now variable n is correct

        // generate necessary arrays and threads
        List<Thread> threads;
        List<Plate> plates;
        List<WineCup> wineCups;
        var knights = new ArrayList<Knight>(n);
        var centralWine = new WineCup(PartyHelper.maximumCentralWineGillCapacity);
        var partyLock = new ReentrantLock();
        var knightsToCheckAndWakeQueue = new ArrayDeque<CheckAndWaitUnit>();
        var knightsToCheckAndWakeSet = new HashSet<Knight>();
        final var nFinal = n;
        final var nHalf = n / 2;

        plates = IntStream.range(0, nHalf).
                mapToObj(i -> new Plate(PartyHelper.maximumPlateCucumberCapacity)).
                collect(Collectors.toList());

        wineCups = IntStream.range(0, nHalf).
                mapToObj(i -> new WineCup(PartyHelper.maximumLocalWineGillCapacity)).
                collect(Collectors.toList());

        for (int i = 0; i < n; i++) {
            knights.add(
                    new Knight(
                            nFinal,
                            i,
                            knights,
                            partyLock,
                            wineCups,
                            plates,
                            centralWine,
                            knightsToCheckAndWakeQueue,
                            knightsToCheckAndWakeSet
                    )
            );
        }

        // create threads
        threads = knights.stream().map(Thread::new).collect(Collectors.toList());

        threads.add(new Thread(new PickleServant(
                knights, partyLock, plates, knightsToCheckAndWakeQueue, knightsToCheckAndWakeSet
        )));

        threads.add(new Thread(new WineGillServant(
                knights, partyLock, centralWine, knightsToCheckAndWakeQueue, knightsToCheckAndWakeSet
        )));

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
