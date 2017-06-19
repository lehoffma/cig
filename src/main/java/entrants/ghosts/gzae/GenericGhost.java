package entrants.ghosts.gzae;

import entrants.pacman.gzae.Util;
import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;
import pacman.game.comms.BasicMessage;
import pacman.game.comms.Message;
import pacman.game.comms.Messenger;


public class GenericGhost extends IndividualGhostController {
    private int[] ghostPos = {-1, -1, -1, -1};
    private int[] messageTick = {-1, -1, -1, -1};

    private int TICK_THRESHOLD = 20;
    private int lastPacmanIndex = -1;
    private int tickSeen = -1;

    private int currentJunctionIndex = -1;

    public GenericGhost(Constants.GHOST ghostType) {
        super(ghostType);
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

        //reset pacman info if nobody has seen him in a while
        if (currentTick <= 2 || currentTick - tickSeen >= TICK_THRESHOLD) {
            lastPacmanIndex = -1;
            tickSeen = -1;
        }

        //setup patrolling indices if not initialized yet
        if (this.currentJunctionIndex == -1) {
            int amountOfJunctions = game.getCurrentMaze().junctionIndices.length - 1;
            this.currentJunctionIndex = amountOfJunctions / (Util.getGhostIndex(ghost) + 1);
            System.out.println(this.currentJunctionIndex);
        }

        // Can we see PacMan? If so tell people and update our info
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);
        Messenger messenger = game.getMessenger();
        if (messenger != null) {
            // Send my Pos
            messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.I_AM, currentIndex,
                    game.getCurrentLevelTime()));
            ghostPos[Util.getGhostIndex(ghost)] = currentIndex;

            //we see pacman
            if (pacmanIndex != -1) {
                lastPacmanIndex = pacmanIndex;
                tickSeen = game.getCurrentLevelTime();
                messenger.addMessage(new BasicMessage(ghost, null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex,
                        game.getCurrentLevelTime()));
            } else {
                // Has anybody else seen PacMan if we haven't?
                for (Message message : messenger.getMessages(ghost)) {
                    switch (message.getType()) {
                        case I_AM:
                            // receive positions
                            int sender = Util.getGhostIndex(message.getSender());
                            if (message.getTick() > messageTick[sender] && message.getTick() < currentTick)
                                messageTick[sender] = message.getTick();
                            ghostPos[sender] = message.getData();
                            break;
                        case PACMAN_SEEN:
                            // Only update pacman info if it is newer information
                            if (message.getTick() > tickSeen && message.getTick() < currentTick) {
                                lastPacmanIndex = message.getData();
                                tickSeen = message.getTick();
                            }
                            break;
                    }
                }
            }
        }
        //todo?
        if (pacmanIndex == -1) {
            pacmanIndex = lastPacmanIndex;
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
            return game.getNextMoveAwayFromTarget(this.getOwnNodeIndex(), lastPacmanIndex, Constants.DM.PATH);
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
        return game.getNextMoveTowardsTarget(this.getOwnNodeIndex(), nextJunctionPosition, Constants.DM.PATH);
    }

    /**
     * @return
     */
    private int getOwnNodeIndex() {
        return ghostPos[Util.getGhostIndex(ghost)];
    }
}


