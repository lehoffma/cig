package entrants.pacman.gzae;

import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;
import pacman.game.info.GameInfo;
import pacman.game.internal.Ghost;

import java.util.*;
import java.util.stream.Collectors;

public class MCTSTree {

    private Game game;
    private MASController ghosts;
    private List<MCTSNode> simulatedMoves;

    private static final int MAX_LOOKAHEAD = 3;
    private static final int SIMULATIONS_PER_MOVE = 5;

    private static final boolean DEBUG = false;

    public MCTSTree(Game game, Map<Constants.GHOST, GhostInfo> ghostInfos) {
        Game completelyObservableGame;
        GameInfo info = game.getPopulatedGameInfo();

        // initialize ghosts with the information we have
        Arrays.stream(Constants.GHOST.values()).forEach(ghost -> {
            int ghostPos = Optional.ofNullable(ghostInfos.get(ghost))
                    .map(GhostInfo::getGhostPos)
                    .filter(pos -> pos >= 0)
                    .orElse(game.getCurrentMaze().lairNodeIndex);
            int edibleTime = Optional.ofNullable(ghostInfos.get(ghost)).map(GhostInfo::getEdibleTime).orElse(0);

            info.setGhost(ghost, new Ghost(ghost, ghostPos, edibleTime, -1, MOVE.NEUTRAL));
        });

        completelyObservableGame = game.getGameFromInfo(info);

        this.game = completelyObservableGame;

        // Make some ghosts for simulation purposes
        this.ghosts = new POCommGhosts(50);
        simulatedMoves = new ArrayList<>();
    }


    public void simulate(double timeDue) {
        if (DEBUG) {
            System.out.println("Start simulation");
        }

        // get all possible moves at the queried position
        int currentPacmanIndex = game.getPacmanCurrentNodeIndex();
        simulatedMoves = Arrays.stream(game.getPossibleMoves(currentPacmanIndex))
                .map(MCTSNode::new)
                .collect(Collectors.toList());

        simulatedMoves.forEach(mctsNode -> {
            mctsNode.doSimulations(game, ghosts, SIMULATIONS_PER_MOVE, MAX_LOOKAHEAD);
            if (DEBUG) {
                System.out.println(mctsNode.toString());
            }
        });
    }

    /**
     * Helper method for getBestMove() and getBestScore()
     * @return the MCTSNode with the best score value
     */
    private MCTSNode getBestMoveNode(){
        MCTSNode bestMoveNode = this.simulatedMoves.stream()
                .reduce(new MCTSNode(MOVE.NEUTRAL),
                        (bestMoveYet, node) -> bestMoveYet.score > node.score ? bestMoveYet : node);

        if(DEBUG){
            System.out.println("best Move: " + bestMoveNode.move.name() + "| score: " + bestMoveNode.score);
        }
        return bestMoveNode;
    }

    /**
     *
     * @return the MOVE enum of the MCTSNode with the best simulated score value
     */
    public MOVE getBestMove() {
        return this.getBestMoveNode().move;
    }

    /**
     * similar to best Move but return the score
     * @return the score value of the MCTSNode with the best simulated score value
     */
    public double getBestScore() {
        return this.getBestMoveNode().score;
    }

}
