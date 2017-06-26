package entrants.pacman.gzae;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final int DISTANCE_THRESHOLD = 7;

    private Map<Constants.GHOST, GhostInfo> ghostInfos = new HashMap<>();

    public MyPacMan(){
        Arrays.stream(Constants.GHOST.values()).forEach(ghost -> ghostInfos.put(ghost, new GhostInfo()));
    }

    public MOVE getMove(Game game, long timeDue) {
        // we will only simulate the next moves in a junction,
        // otherwise we will walk straight along the hallway
        int myNodeIndex = game.getPacmanCurrentNodeIndex();

        //update the ghost's information stored in the map
        ghostInfos = ghostInfos.entrySet().stream()
                .map(ghostInfoEntry -> {
                    GhostInfo ghostInfo = ghostInfoEntry.getValue();
                    // Try to get Ghost Information
                    int ghostPosition = game.getGhostCurrentNodeIndex(ghostInfoEntry.getKey());
                    int edibleTime = game.getGhostEdibleTime(ghostInfoEntry.getKey());

                    ghostInfo.incrementLastSeen();
                    if(ghostInfo.getEdibleTime() > 0){
                        ghostInfo.setEdibleTime(ghostInfo.getEdibleTime()-1);
                    }

                    // if we found a ghost, update infos
                    if (ghostPosition != -1) {
                        ghostInfo.setGhostPos(ghostPosition)
                                .setEdibleTime(edibleTime)
                                .setLastSeen((byte) 0);
                    }

                    // if not seen for a long time
                    if(ghostInfo.getLastSeen() > SEEN_THRESHOLD){
                        ghostInfo.setGhostPos(-1);
                    }
                    ghostInfoEntry.setValue(ghostInfo);
                    return ghostInfoEntry;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        if (game.isJunction(myNodeIndex)) {
            // return best direction determined through MCTS
            return mcts(game, timeDue);
        }
        // follow along the path
        return nonJunctionSim(game, ghostInfos);
    }

    /**
     *
     * @param game
     * @param ghostInfos
     * @return
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
                                .filter(info -> info.getGhostPos() >= 0 && info.getEdibleTime() == 0)
                                .map(info -> (int) game.getDistance(neighbourIndex, info.getGhostPos(), Constants.DM.PATH))
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
        MCTSTree tree = new MCTSTree(game, ghostInfos);
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