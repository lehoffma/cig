package entrants.pacman.gzae;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getMove() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., entrants.pacman.username).
 */
public class MyPacMan extends PacmanController {
    private MOVE myMove = MOVE.NEUTRAL;

    private static final byte SEEN_THRESHOLD = 10;
    private static final int GAME_SCORE_THRESHOLD = 20;
    private static final int DISTANCE_THRESHOLD = 10;


    private int[] ghostPos = {-1, -1, -1, -1};
    private byte[] lastSeen = {0, 0, 0, 0};
    private int[] edibleTime = {0, 0, 0, 0};

    public MOVE getMove(Game game, long timeDue) {
        // we will only simulate the next moves in a junction,
        // otherwise we will walk straight along the hallway
        int myNodeIndex = game.getPacmanCurrentNodeIndex();


        // for every ghost
        for (int ghostIndex = 0; ghostIndex < ghostPos.length; ghostIndex++) {

            // Try to get Ghost Information
            int ghostPosition = game.getGhostCurrentNodeIndex(Util.getGhostByIndex(ghostIndex));
            int edibleTime = game.getGhostEdibleTime(Util.getGhostByIndex(ghostIndex));

            // One Timestep later, so increase the last seen and decrease the edibleTime
            lastSeen[ghostIndex]++;
            if (this.edibleTime[ghostIndex] > 0) {
                this.edibleTime[ghostIndex]--;
            }

            // if we found a ghost, update infos
            if (ghostPosition != -1) {
                ghostPos[ghostIndex] = ghostPosition;
                this.edibleTime[ghostIndex] = edibleTime;
                lastSeen[ghostIndex] = 0;
            }

            // if not seen for a long time
            if (lastSeen[ghostIndex] > SEEN_THRESHOLD) {
                ghostPos[ghostIndex] = -1;
            }

        }

        if (game.isJunction(myNodeIndex)) {
            // return best direction determined through MCTS
            return mcts(game, timeDue);
        }
        // follow along the path
        return nonJunctionSim(game, this.ghostPos, this.edibleTime);
    }

    /**
     * @param game
     * @param ghostPositions
     * @param edibleTime
     * @return
     */
    public static MOVE nonJunctionSim(Game game, int[] ghostPositions, int[] edibleTime) {
        // get the current position of PacMan (returns -1 in case you can't see PacMan)
        int currentPacmanIndex = game.getPacmanCurrentNodeIndex();

        // get all possible moves at the queried position
        List<MOVE> possibleMoves = Arrays.asList(game.getPossibleMoves(currentPacmanIndex));
        List<MOVE> safeMoves = possibleMoves;

        //check if any ghosts are in the way if we are given that information
        if (ghostPositions.length > 0 && edibleTime.length > 0) {
            safeMoves = possibleMoves.stream()
                    //remove moves that would put us in danger (i.e. too close to a non-edible ghost)
                    .filter(move -> {
                        int neighbourIndex = game.getNeighbour(currentPacmanIndex, move);

                        //compute the minimum distance between pacman and any non-edible ghost
                        int distance = IntStream.range(0, ghostPositions.length)
                                .filter(ghostIndex -> ghostPositions[ghostIndex] >= 0)
                                .filter(ghostIndex -> edibleTime[ghostIndex] == 0)
                                .mapToObj(ghostIndex ->
                                        (int) game.getDistance(neighbourIndex, ghostPositions[ghostIndex], Constants.DM.PATH))
                                .reduce(Integer.MAX_VALUE, Math::min);

                        //move is only safe if the nearest ghost is far away enough
                        return distance > DISTANCE_THRESHOLD;
                    })
                    .collect(Collectors.toList());
        }


        if (safeMoves.isEmpty()) {
            safeMoves.addAll(possibleMoves);
        }

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
     * @return
     */
    public MOVE atJunctionSim() {
        return MOVE.NEUTRAL;
    }


    /**
     * @param game
     * @param timeDue
     * @return
     */
    public MOVE mcts(Game game, long timeDue) {
        // create MCTSTree object for simulation
        MCTSTree tree = new MCTSTree(game, ghostPos, edibleTime);
        tree.simulate(timeDue);

        System.out.println("Tree: " + tree.getBestScore() + " Game:" + (game.getScore() + game.getPacmanNumberOfLivesRemaining() * 1000));

        // if sim result is better than current situation
        if (tree.getBestScore() > (GAME_SCORE_THRESHOLD + game.getScore() + game.getPacmanNumberOfLivesRemaining() * 1000))
            return tree.getBestMove();
        else {
            System.out.println("NO RESULT!");
            // TODO: Do fancy Stuff
            return tree.getBestMove();
        }
    }
}