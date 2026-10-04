package de.phlup.circuitchaos.server.game;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class Sleep {

    private static final List<Object> queue = new ArrayList<>();

    public static void sleepForMillis(long millis) {
        long startTime = System.currentTimeMillis();
        long counter   = 0;
        long counter2  = 0;
        while (startTime + millis > System.currentTimeMillis()) {
            try {
                //noinspection BusyWait
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                counter++;
                Thread.currentThread().interrupt();
            }
            counter2++;
        }
        if (counter2 > 2) {
            log.error("Got interrupted {} times in {} loops. Why is that?!?", counter, counter2);
        }
    }

    public static void waitForWakeUp(Game game) {
        try {
            while (queue.contains(game)) {
                Thread.currentThread().wait();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            queue.remove(game);
        }
    }

}
