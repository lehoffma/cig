package entrants.ghosts.gzae;

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

    /**
     * This threshold controls when we reset the pacman's position. If the last time we saw him was
     * more than TICK_TRESHOLD ticks ago, we reset the values since he is probably already gone.
     */
    private final int TICK_THRESHOLD = 30;

    /**
     * The info about pacman we have currently stored (includes his position, when we last saw him and how many lives
     * he has left)
     */
    private PacmanInfo pacmanInfo = new PacmanInfo();

    /**
     * The junction we are currently headed towards (when patrolling)
     */
    private int currentJunctionIndex = -1;

    public GenericGhost(Constants.GHOST ghostType) {
        super(ghostType);
    }

    /**
     * Initializes the ghost if it didn't happen already.
     * At the moment, this includes only the patrolling mechanism.
     * @param game the currently active game
     */
    private void initialize(Game game) {
        //setup patrolling indices if not initialized yet
        if (this.currentJunctionIndex == -1) {
            int amountOfJunctions = game.getCurrentMaze().junctionIndices.length - 1;
            this.currentJunctionIndex = (int) (Math.random() * amountOfJunctions);
        }
    }

    /**
     * @param ownNodeIndex     our own position
     * @param pacmanIndex      the position of the pacman (-1 if we don't see him at the moment)
     * @param currentLevelTime the time elapsed in the game (needed for the messages)
     * @return a list of messages consisting of the "I_AM" message and, if the given pacman index is not -1 (i.e. the ghost
     * can see him), the "PACMAN_SEEN" message.
     */
    private List<BasicMessage> getMessages(int ownNodeIndex, int pacmanIndex, int currentLevelTime) {
        List<BasicMessage> messages = new ArrayList<>();
        //send own position
        messages.add(new BasicMessage(ghost, null, BasicMessage.MessageType.I_AM, ownNodeIndex,
                currentLevelTime));

        //we see pacman
        if (pacmanIndex != -1) {
            messages.add(new BasicMessage(ghost, null, BasicMessage.MessageType.PACMAN_SEEN, pacmanIndex,
                    currentLevelTime));
        }

        return messages;
    }


    /**
     * Reads the unread messages of the messenger and handles them accordingly. If the message is an I_AM message,
     * we update our internal ghostPositions map. If the message is an PACMAN_SEEN message and the information is newer
     * than the one we have stored, we update the pacman position.
     *
     * @param messenger   the messenger object containing the messenges
     * @param currentTick the current timestamp of the game
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
                    if (message.getTick() > pacmanInfo.getTickSeen() && message.getTick() < currentTick) {
                        pacmanInfo.setLastIndex(message.getData())
                                .setTickSeen(message.getTick());
                    }
                    break;
            }
        });
    }

    /**
     * Updates the information stored about pacman and the other ghosts.
     * This includes updating the pacmanInfo object if needed, updating our own position, and reading and sending messages.
     *
     * @param game the currently active game
     */
    private void updateGameInfo(Game game) {
        int currentTick = game.getCurrentLevelTime();
        int pacmanIndex = game.getPacmanCurrentNodeIndex();
        int currentIndex = game.getGhostCurrentNodeIndex(ghost);
        Messenger messenger = game.getMessenger();

        //update our own position
        ghostPositions.put(ghost, currentIndex);

        //reset pacman info if nobody has seen him in a while or if pacman died since we last called this method
        if (currentTick <= 2 || currentTick - pacmanInfo.getTickSeen() >= TICK_THRESHOLD
                || pacmanInfo.getLivesLeft() > game.getPacmanNumberOfLivesRemaining()) {
            pacmanInfo.setLastIndex(-1)
                    .setTickSeen(-1)
                    .setLivesLeft(game.getPacmanNumberOfLivesRemaining());
        }

        //update the pacman info if we can see him
        if (pacmanIndex != -1) {
            pacmanInfo.setLastIndex(pacmanIndex)
                    .setTickSeen(game.getCurrentLevelTime());
        }

        //handle messages
        if (messenger != null) {
            //send messages
            this.getMessages(currentIndex, pacmanIndex, game.getCurrentLevelTime()).forEach(messenger::addMessage);
            this.readMessages(messenger, currentTick);
        }
    }

    /**
     * Handles the movement when the ghost is edible. Tries to move away from pacman's position if we know where he is.
     * Otherwise the ghost tries to lure pacman towards the lair, where the already eaten ghosts are respawned and
     * might be able to eat pacman or at least chase him away.
     *
     * @param game the currently active game
     * @return either a move away from pacman (if we saw him), a move towards the lair (if we aren't already there) or
     * a neutral move.
     */
    private Constants.MOVE runAway(Game game) {
        if (pacmanInfo.getLastIndex() >= 0) {
            return game.getApproximateNextMoveAwayFromTarget(this.getOwnNodeIndex(game), pacmanInfo.getLastIndex(),
                    game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
        }
        //move towards the lair if not already there
        if (game.getDistance(this.getOwnNodeIndex(game), game.getCurrentMaze().lairNodeIndex, Constants.DM.PATH) > 10) {
            return game.getApproximateNextMoveAwayFromTarget(this.getOwnNodeIndex(game), game.getCurrentMaze().lairNodeIndex,
                    game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
        }
        return Constants.MOVE.NEUTRAL;
    }

    /**
     * @param game the currently active game
     * @return the approximate next move towards the pacman's position
     */
    private Constants.MOVE chasePacman(Game game) {
        return game.getApproximateNextMoveTowardsTarget(this.getOwnNodeIndex(game), pacmanInfo.getLastIndex(),
                game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
    }

    /**
     * This function handles the junction patrolling of the ghost, i.e. once it reaches the destination junction,
     * a new one is selected (the next one in the list given by the game object).
     *
     * @param game the currently active game
     * @return a move towards the next junction
     */
    private Constants.MOVE findPacman(Game game) {
        //patrols from junction to junction
        //we have reached the junction we wanted to move towards -> move towards next junction
        if (this.getOwnNodeIndex(game) == game.getJunctionIndices()[currentJunctionIndex]) {
            currentJunctionIndex = (currentJunctionIndex + 1) % game.getJunctionIndices().length;
        }
        int nextJunctionPosition = game.getJunctionIndices()[currentJunctionIndex];
        return game.getApproximateNextMoveTowardsTarget(this.getOwnNodeIndex(game), nextJunctionPosition,
                game.getGhostLastMoveMade(ghost), Constants.DM.PATH);
    }

    /**
     * Calls the appropriate movement functions, depending on the state of the game.
     *
     * @param game the currently active game
     * @return either a move (which move depends on the state of the game) if the ghost has to return an action
     * or null otherwise
     */
    private Constants.MOVE getGhostMove(Game game) {
        Boolean requiresAction = game.doesGhostRequireAction(ghost);
        int edibleTime = game.getGhostEdibleTime(ghost);

        //if ghost requires an action, i.e. the ghost is at a junction
        if (requiresAction != null && requiresAction) {
            //run away from pacman if he can eat the ghost
            if (edibleTime > 0) {
                return runAway(game);
            }
            //we know where he is hiding
            if (pacmanInfo.getLastIndex() >= 0) {
                return chasePacman(game);
            }
            return findPacman(game);
        }
        return null;
    }

    /**
     * Handles updating and initializing the internal game state (i.e. the ghost's and the pacman's position, etc.).
     * Based on this updated game state, an appropriate move is selected (either finding, chasing or running away from pacman)
     * @param game the currently active game
     * @param timeDue how much time we can spend at most
     * @return a move depending on the state of the game (or null if the ghost doesn't require an action)
     */
    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        this.initialize(game);
        this.updateGameInfo(game);
        return this.getGhostMove(game);
    }


    /**
     * @return our own node index (i.e. where the ghost is positioned)
     */
    private int getOwnNodeIndex(Game game) {
        return game.getGhostCurrentNodeIndex(ghost);
    }
}


