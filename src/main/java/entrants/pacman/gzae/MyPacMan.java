package entrants.pacman.gzae;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MyPacMan extends PacmanController {
    /**
     * The amount of ticks that have to elapse until the ghost's positions will be reset
     */
    private static final byte SEEN_THRESHOLD = 10;
    /**
     * A move is only considered safe if the distance to the nearest ghost exceeds this value
     */
    private static final int DISTANCE_THRESHOLD = 7;

    /**
     * contains infos (positions, edible times) about the ghosts in the currently active game
     */
    private Map<Constants.GHOST, GhostInfo> ghostInfos = new HashMap<>();


    private final Logger LOGGER = Logger.getLogger(MyPacMan.class.getName());

    /**
     *
     */
    public MyPacMan() {
        Arrays.stream(Constants.GHOST.values()).forEach(ghost -> ghostInfos.put(ghost, new GhostInfo()));
    }

    /**
     * Updates the ghosts' positions and edible times.
     * If the ghost is currently visible, we get the data directly from the game, otherwise we use the old data.
     * If enough time has passed without seeing the ghost, the internal state is reset (since it will probably be
     * somewhere else)
     *
     * @param game the currently active game
     * @param ghostInfos a map containing infos (positions, edible times) about the ghosts in the currently active game
     * @return an updated ghostInfo map (with updated or reset positions/edible times)
     */
    private Map<Constants.GHOST, GhostInfo> updateGhostInfo(Game game, Map<Constants.GHOST, GhostInfo> ghostInfos) {
        return ghostInfos.entrySet().stream()
                .map(ghostInfoEntry -> {
                    GhostInfo ghostInfo = ghostInfoEntry.getValue();
                    Constants.GHOST ghost = ghostInfoEntry.getKey();
                    // Try to get Ghost Information
                    int ghostPosition = game.getGhostCurrentNodeIndex(ghost);
                    int edibleTime = game.getGhostEdibleTime(ghost);

                    // if we found a ghost, update infos
                    if (edibleTime != -1 && ghostPosition != -1) {
                        ghostInfo.setPosition(ghostPosition)
                                .setEdibleTime(edibleTime)
                                .setLastSeen((byte) 0);
                    }
                    // we can't see the ghost => update internal ghost state
                    else {
                        ghostInfo.incrementLastSeen();
                        if (ghostInfo.getEdibleTime() > 0) {
                            ghostInfo.setEdibleTime(ghostInfo.getEdibleTime() - 1);
                        }
                        // reset the position if we haven't seen the ghost in a long time
                        if (ghostInfo.getLastSeen() > SEEN_THRESHOLD) {
                            ghostInfo.setPosition(-1);
                        }
                    }

                    ghostInfoEntry.setValue(ghostInfo);
                    return ghostInfoEntry;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     *
     * @param game the currently active game
     * @param timeDue how much time we have for generating the move
     * @return the best move according to the monte carlo tree search, if at a junction.
     * Otherwise, the next move along the corridor (if it's safe).
     */
    public MOVE getMove(Game game, long timeDue) {
        // we will only simulate the next moves in a junction,
        // otherwise we will walk straight along the hallway
        int myNodeIndex = game.getPacmanCurrentNodeIndex();

        //update the ghost's information stored in the map
        ghostInfos = this.updateGhostInfo(game, ghostInfos);


        if (game.isJunction(myNodeIndex)) {
            // return best direction determined through MCTS
            return this.mcts(game, timeDue);
        }
        // follow along the path
        return nonJunctionSim(game, ghostInfos);
    }

    /**
     * @param game the currently active game
     * @param ghostInfos a map containing infos (positions, edible times) about the ghosts in the currently active game
     * @return the next move along the corridor, if it's safe. Otherwise, returns the next best safe move.
     */
    public static MOVE nonJunctionSim(Game game, Map<Constants.GHOST, GhostInfo> ghostInfos) {
        // get the current position of PacMan (returns -1 in case you can't see PacMan)
        int currentPacmanIndex = game.getPacmanCurrentNodeIndex();

        // get all possible moves at the queried position
        List<MOVE> possibleMoves = Arrays.asList(game.getPossibleMoves(currentPacmanIndex));
        List<MOVE> safeMoves = possibleMoves;

        //check if any ghosts are in the way if we are given that information
        if (ghostInfos.size() > 0) {
            safeMoves = possibleMoves.stream()
                    //remove moves that would put us in danger (i.e. too close to a non-edible ghost)
                    .filter(move -> {
                        int neighbourIndex = game.getNeighbour(currentPacmanIndex, move);

                        //compute the minimum distance between pacman and any non-edible ghost
                        int distance = ghostInfos.values().stream()
                                .filter(info -> info.getPosition() >= 0 && info.getEdibleTime() == 0)
                                .map(info -> (int) game.getDistance(neighbourIndex, info.getPosition(), Constants.DM.PATH))
                                .reduce(Integer.MAX_VALUE, Math::min);

                        //move is only safe if the nearest ghost is far away enough
                        return distance > DISTANCE_THRESHOLD;
                    })
                    .collect(Collectors.toList());
        }

        //if there are now safe moves, just use the non safe moves (we don't really have any other choice)
        if (safeMoves.isEmpty()) {
            safeMoves.addAll(possibleMoves);
        }

        //move along the corridor if it's safe
        MOVE lastMove = game.getPacmanLastMoveMade();
        if (safeMoves.contains(lastMove)) {
            return lastMove;
        }

        return safeMoves.stream()
                //don't go back (corner)
                .filter(move -> move != lastMove.opposite())
                .findFirst()
                //default value
                .orElse(lastMove.opposite());
    }

    /**
     * Performs a monte carlo tree search to find the best move available to the pacman.
     * @param game the currently active game
     * @param timeDue how much time is left to calculate the move
     * @return the best move according to the monte carlo tree search's value function (which depends on the simulated game score
     * and the amount of lives left)
     */
    private MOVE mcts(Game game, long timeDue) {
        // create MCTSTree object for simulation
        MCTSTree tree = new MCTSTree(game, ghostInfos);
        MCTSNode bestMove = tree.simulate(timeDue);

        LOGGER.log(Level.INFO, "Tree: " + bestMove.score + " Game:" +
                (game.getScore() + game.getPacmanNumberOfLivesRemaining() * 1000));

        return bestMove.move;
    }
}