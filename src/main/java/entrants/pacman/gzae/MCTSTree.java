package entrants.pacman.gzae;

import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.info.GameInfo;
import pacman.game.internal.Ghost;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MCTSTree {
    /**
     * The game the simulations will be performed on (completely observable)
     */
    private Game game;
    /**
     * A controller for the ghosts which will be initialized with the observed infos from the pacman
     */
    private MASController ghosts = new POCommGhosts(50);

    /**
     * How many junctions we want to look ahead
     */
    private static final int MAX_LOOKAHEAD = 3;
    /**
     * How often we want to simulate a move
     */
    private static final int SIMULATIONS_PER_MOVE = 5;


    private final Logger LOGGER = Logger.getLogger(MCTSTree.class.getName());

    /**
     * @param game       the game the simulation should take place in
     * @param ghostInfos a map containing observed infos (positions, edible times) about the ghosts in
     *                   the currently active game
     */
    public MCTSTree(Game game, Map<Constants.GHOST, GhostInfo> ghostInfos) {
        GameInfo info = game.getPopulatedGameInfo();

        // initialize ghosts with the information we have
        Arrays.stream(Constants.GHOST.values()).forEach(ghost -> {
            int ghostPos = Optional.ofNullable(ghostInfos.get(ghost))
                    .map(GhostInfo::getPosition)
                    .filter(pos -> pos >= 0)
                    .orElse(game.getCurrentMaze().lairNodeIndex);
            int edibleTime = Optional.ofNullable(ghostInfos.get(ghost)).map(GhostInfo::getEdibleTime).orElse(0);

            info.setGhost(ghost, new Ghost(ghost, ghostPos, edibleTime, -1, MOVE.NEUTRAL));
        });

        this.game = game.getGameFromInfo(info);
    }


    /**
     * Performs a simulation for every possible move pacman can perform.
     *
     * @param timeDue how much time is left in this tick
     * @return the best move according to the simulations performed inside this method
     */
    public MCTSNode simulate(double timeDue) {
        LOGGER.log(Level.INFO, "Start simulation");

        return Arrays
                // get all possible moves at the queried position
                .stream(game.getPossibleMoves(game.getPacmanCurrentNodeIndex()))
                .map(MCTSNode::new)
                //simulate the game
                .map(mctsNode -> mctsNode.doSimulations(game, ghosts, SIMULATIONS_PER_MOVE, MAX_LOOKAHEAD))
                .peek(mctsNode -> LOGGER.info(mctsNode.toString()))
                //return the simulated move with the highest score
                .reduce(new MCTSNode(MOVE.NEUTRAL),
                        (bestMoveYet, node) -> bestMoveYet.score > node.score ? bestMoveYet : node);
    }
}
