package de.phlup.circuitchaos.gamelogic.player.network;

import de.phlup.circuitchaos.gamelogic.Game;
import de.phlup.circuitchaos.gamelogic.GlobalServerAttributes;
import de.phlup.circuitchaos.gamelogic.player.Player;
import de.phlup.circuitchaos.model.NetworkRequest;
import de.phlup.circuitchaos.model.NetworkResponse;
import de.phlup.circuitchaos.model.Registration;
import de.phlup.circuitchaos.model.Robot;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class NetworkPlayer extends Player {

    private final String       id;
    private final Registration registration;

    public NetworkPlayer(Robot robot, String gameId, Registration registration) {
        super(robot, gameId);
        this.registration = registration;
        this.id = UUID.randomUUID().toString();
    }

    public NetworkRequest createRequest(Integer phase) {
        Game           game           = GlobalServerAttributes.getGame(gameId);
        NetworkRequest networkRequest = new NetworkRequest();
        networkRequest.setBoard(game.getBoard());
        networkRequest.setActiveRobot(robot);
        networkRequest.setPhase(phase);
        return createProgramRobotRequest(networkRequest);
    }

    public void handleResponse(NetworkResponse programResponse) {
        Game game = GlobalServerAttributes.getGame(gameId);
        if (programResponse.isFilled() && programResponse.getProgrammedRobot() != null) {
            robot.adjustToNetworkRobot(programResponse.getProgrammedRobot());
        } else {
            game.giveUp(robot, registration.getId());
        }
    }

    private NetworkRequest createProgramRobotRequest(NetworkRequest programRequest) {
        if (robot.isPoweredDown()) {
            return null;
        }
        initTurn();
        return programRequest;
    }

}
