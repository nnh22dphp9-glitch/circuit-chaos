package de.phlup.circuitchaos.server.scheduler;

import de.phlup.circuitchaos.common.model.GameItem;
import de.phlup.circuitchaos.common.settings.CleanGameListSchedulerSettings;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.Game;
import de.phlup.circuitchaos.server.service.PollingService;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@AllArgsConstructor
@SuppressWarnings("unused")
public class CleanGameListScheduler {

    private CleanGameListSchedulerSettings settings;
    private PollingService                 pollingService;

    @Scheduled(initialDelayString = "${circuit-chaos.server.clean-game-list-scheduler.start-delay-seconds}000",
            fixedRateString = "${circuit-chaos.server.clean-game-list-scheduler.fixed-rate-seconds}000")
    public void cleanOldGames() {
        cleanupPreparingGames(settings.getEvictTimePreparingMinutes());
        cleanupRunningGames(settings.getEvictTimeRunningMinutes());
        pollingService.cleanupRegistrations();
    }

    private void cleanupPreparingGames(int evictTime) {
        ZonedDateTime       zonedDateTime = ZonedDateTime.now().minusSeconds(evictTime);
        Map<GameItem, Game> copy          = new ConcurrentHashMap<>(GlobalServerAttributes.PREPARING_GAMES);
        for (Map.Entry<GameItem, Game> e : copy.entrySet()) {
            if (zonedDateTime.isAfter(e.getValue().getLastAccessed()) || e.getValue().isGameAborted()) {
                GlobalServerAttributes.PREPARING_GAMES.remove(e.getKey());
            }
        }
    }

    private void cleanupRunningGames(int evictTime) {
        ZonedDateTime     zonedDateTime = ZonedDateTime.now().minusSeconds(evictTime);
        Map<String, Game> copy          = new ConcurrentHashMap<>(GlobalServerAttributes.RUNNING_GAMES);
        for (Map.Entry<String, Game> e : copy.entrySet()) {
            if (zonedDateTime.isAfter(e.getValue().getLastAccessed()) || e.getValue().isGameAborted()) {
                GlobalServerAttributes.RUNNING_GAMES.remove(e.getKey());
            }
        }
    }

}
