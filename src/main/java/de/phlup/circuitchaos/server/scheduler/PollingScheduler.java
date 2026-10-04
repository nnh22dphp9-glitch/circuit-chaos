package de.phlup.circuitchaos.server.scheduler;

import de.phlup.circuitchaos.client.ClientToServerConnection;
import de.phlup.circuitchaos.client.gui.ClientCourseArranger;
import de.phlup.circuitchaos.client.gui.GameGui;
import de.phlup.circuitchaos.client.service.AsyncService;
import de.phlup.circuitchaos.common.model.PullingData;
import de.phlup.circuitchaos.server.GlobalServerAttributes;
import de.phlup.circuitchaos.server.game.GameAttributes;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
@SuppressWarnings("unused")
public class PollingScheduler {

    private final ClientToServerConnection serverConnection;
    private final AsyncService             asyncService;

    @Scheduled(initialDelayString = "${circuit-chaos.client.poll-scheduler.start-delay-milliseconds}",
            fixedRateString = "${circuit-chaos.client.poll-scheduler.fixed-rate-milliseconds}")
    public void poll() {
        List<GameAttributes> copyOfGames = GlobalServerAttributes.CLIENT_GAMES.values().stream().toList();
        for (GameAttributes reg : copyOfGames) {
            if (reg.getRegistration().isPull()) {
                String        registrationId = reg.getRegistration().getId();
                PullingData[] dataList       = serverConnection.pull(reg.getGameUrl(), registrationId);
                if (dataList != null) {
                    for (PullingData data : dataList) {
                        handleSinglePullingData(reg, data, registrationId);
                    }
                }
            }
        }
    }

    private void handleSinglePullingData(GameAttributes reg, PullingData data, String registrationId) {
        final GameGui gameGui = reg.getGameGui();
        switch (data.getType()) {
            case COURSE_CHANGE -> asyncService.refreshCourse(gameGui, data.getRequest());
            case SHOW_MESSAGE -> asyncService.showMessage(registrationId, data.getMessage());
            case REVEAL_PROGRAMME -> {
                gameGui.setNotStartedYet(false);
                asyncService.revealProgramme(gameGui, data.getRevealProgrammeResponse());
            }
            case PROGRAMME_QUESTION -> {
                gameGui.setNotStartedYet(false);
                String answerUrl = gameGui.getGameAttributes().getGameUrl() + "/answer/" + data.getPurposeId() + "/" + data.getRequestId();
                asyncService.perform(gameGui, answerUrl, data.getRequest());
            }
            case NUMBER_OF_PLAYERS_CHANGE -> {
                ClientCourseArranger courseArranger = gameGui.getCourseArranger();
                if (courseArranger != null) {
                    courseArranger.updatePlayersAndWatchers(data.getNumberOfPlayers(), data.getNumberOfWatchers());
                }
            }
        }
    }

}
