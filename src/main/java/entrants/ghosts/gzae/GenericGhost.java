package entrants.ghosts.gzae;

import entrants.pacman.gzae.Util;
import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Messenger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class GenericGhost extends IndividualGhostController {
    private Map<Constants.GHOST, Integer> ghostPositions = new HashMap<>();

    private final int TICK_THRESHOLD = 30;
    private int lastPacmanIndex = -1;
    private int tickSeen = -1;
    private int pacmanLives = -1;

    private int currentJunctionIndex = -1;

    public GenericGhost(Constants.GHOST ghostType) {
        super(ghostType);
    }

    /**
     * @param game
     * @param ownNodeIndex
     * @param pacmanIndex
     * @return
     */
    private List<BasicMessage> getMessages(Game game, int ownNodeIndex, int pacmanIndex) {
        List<BasicMessage> messages = new ArrayList<>();
        //send own position
        messages.add(new BasicMessage(ghost, null, BasicMessage.MessageType.I_AM, ownNodeIndex,
                game.getCurrentLevelTime()));

        //we see pacman
        if (pacmanIndex != -1) {
            messages.add(new BasicMessage(ghost, null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex,
                    game.getCurrentLevelTime()));
        }

        return messages;
    }

    /**
     * @param messenger
     * @param currentTick
     */
    private void readMessages(Messenger messenger, int currentTick) {
        messenger.getMessages(ghost).forEach(message -> {
            switch (message.getType()) {
                case I_AM:
                    // receive positions
                    ghostPositions.put(message.getSender(), message.getData());
                    break;
                // Has anybody else seen PacMan if we haven't?
                case PACMAN_SEEN:
                    // Only update pacman info if it is newer information
                    if (message.getTick() > tickSeen && message.getTick() < currentTick) {
                        lastPacmanIndex = message.getData();
                        tickSeen = message.getTick();
                    }
                    break;
            }
        });
    }

    /**
     * @param game
     * @param timeDue
     * @return
     */
    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        // Housekeeping - throw out old info
        int edibleTime = game.getGhostEdibleTime(ghost);
        int currentTick = game.getCurrentLevelTime();

        //reset pacman info if nobody has seen him in a while or if pacman died since we last called this method
        if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD
                || pacmanLives > game.getPacmanNumberOfLivesRemaining()) {
            pacmanLives = game.getPacmanNumberOfLivesRemaining();
            lastPacmanIndex = -1;
            tickSeen = -1;
        }

        //setup patrolling indices if not initialized yet
        if (this.currentJunctionIndex == -1) {
            int amountOfJunctions = game.getCurrentMaze().junctionIndices.length - 1;
            this.currentJunctionIndex = amountOfJunctions / (Util.getGhostIndex(ghost) + 1);
        }

        // Can we see PacMan? If so tell people and update our info
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);
        Messenger messenger = game.getMessenger();

        //update our own and pacman's position
        ghostPositions.put(ghost, currentIndex);
        if (pacmanIndex != -1) {
            lastPacmanIndex = pacmanIndex;
            tickSeen = game.getCurrentLevelTime();
        }

        //handle messages
        if (messenger != null) {
            //send messages
            this.getMessages(game, currentIndex, pacmanIndex).forEach(messenger::addMessage);
            this.readMessages(messenger, currentTick);
        }

        Boolean requiresAction = game.doesGhostRequireAction(ghost);
        //if ghost requires an action, i.e. the ghost is at a junction
        if (requiresAction != null && requiresAction) {
            //run away from pacman if he can eat the ghost
            if (edibleTime > 0) {
                return runAway(game);
            }
            //we know where he is hiding
            if (lastPacmanIndex >= 0) {
                return chasePacman(game);
            }
            return findPacman(game);
        }
        return null;
    }

    /**
     * @param game
     * @return
     */
    private Constants.MOVE runAway(Game game) {
        if (lastPacmanIndex >= 0) {
            return game.getApproximateNextMoveAwayFromTarget(this.getOwnNodeIndex(), lastPacmanIndex,
                    game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
        }
        //move towards the lair if not already there
        if (game.getDistance(this.getOwnNodeIndex(), game.getCurrentMaze().lairNodeIndex, Constants.DM.PATH) > 10) {
            return game.getApproximateNextMoveAwayFromTarget(this.getOwnNodeIndex(), game.getCurrentMaze().lairNodeIndex,
                    game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
        }
        return Constants.MOVE.NEUTRAL;
    }

    /**
     * @param game
     * @return
     */
    private Constants.MOVE chasePacman(Game game) {
        return game.getApproximateNextMoveTowardsTarget(this.getOwnNodeIndex(), lastPacmanIndex,
                game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
    }

    /**
     * @param game
     * @return
     */
    private Constants.MOVE findPacman(Game game) {
        //patrols from junction to junction
        //we have reached the junction we wanted to move towards -> move towards next junction
        if (this.getOwnNodeIndex() == game.getJunctionIndices()[currentJunctionIndex]) {
            currentJunctionIndex = (currentJunctionIndex + 1) % game.getJunctionIndices().length;
        }
        int nextJunctionPosition = game.getJunctionIndices()[currentJunctionIndex];
        return game.getApproximateNextMoveTowardsTarget(this.getOwnNodeIndex(), nextJunctionPosition,
                game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
    }

    /**
     * @return
     */
    private int getOwnNodeIndex() {
        return ghostPositions.get(ghost);
    }
}


