package entrants.pacman.gzae;

import java.util.Arrays;

import pacman.controllers.PacmanController;
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

/*
 * This is the class you need to modify for your entry. In particular, you need to
 * fill in the getMove() method. Any additional classes you write should either
 * be placed in this package or sub-packages (e.g., entrants.pacman.username).
 */
public class MyPacMan extends PacmanController {
    private MOVE myMove = MOVE.NEUTRAL;

    private static final byte SEEN_THRESHOLD = 10;
    private static final int GAME_SCORE_THRESHOLD = 20;


    private int[] ghostPos = {-1,-1,-1,-1};
    private byte[] lastSeen = {0,0,0,0};
    private int[] edibleTime = {0,0,0,0};

    public MOVE getMove(Game game, long timeDue) {
        // we will only simulate the next moves in a junction,
        // otherwise we will walk straight along the hallway
        int myNodeIndex = game.getPacmanCurrentNodeIndex();


        for (int i=0;i<ghostPos.length;++i) {
            int pos = game.getGhostCurrentNodeIndex(getGhost(i));
            int edible = game.getGhostEdibleTime(getGhost(i));

            lastSeen[i]++;
            if (edibleTime[i]>0) edibleTime[i]--;

            if (pos!=-1) {
                ghostPos[i] = pos;
                edibleTime[i]=edible;
                lastSeen[i]= 0;
            }

            if(lastSeen[i]>SEEN_THRESHOLD) {
                ghostPos[i] = -1;
            }

        }


        // choose random direction at junction
        if (game.isJunction(myNodeIndex))
        {
            // return best direction determined through MCTS
            return mcts(game, timeDue);
        } else {
            // follow along the path
            return nonJunctionSim(game);
        }

    }

    public static MOVE nonJunctionSim(Game game){
        // get the current position of PacMan (returns -1 in case you can't see PacMan)
        int myNodeIndex = game.getPacmanCurrentNodeIndex();

        // get all possible moves at the queried position
        MOVE[] myMoves = game.getPossibleMoves(myNodeIndex);

        MOVE lastMove = game.getPacmanLastMoveMade();
        if (Arrays.asList(myMoves).contains(lastMove)){
            return lastMove;
        }

        // don't go back (corner)
        for (MOVE move : myMoves){
            if (move != lastMove.opposite()){
                return move;
            }
        }

        // default
        return lastMove.opposite();
    }

    public MOVE atJunctionSim(){

        return MOVE.NEUTRAL;
    }


    public MOVE mcts(Game game, long timeDue){
        // create MCTSTree object for simulation
        MCTSTree tree = new MCTSTree(game,ghostPos,edibleTime);
        tree.simulate(timeDue);
        System.out.println("Tree: "+ tree.getBestScore()+" Game:" + (game.getScore() + game.getPacmanNumberOfLivesRemaining()*1000));
        if (tree.getBestScore()> (GAME_SCORE_THRESHOLD + game.getScore() + game.getPacmanNumberOfLivesRemaining()*1000))
            return tree.getBestMove();
        else {
            System.out.println("NO RESULT!");
            // TODO: Do fancy Stuff
            return tree.getBestMove();
        }
    }

    public static Constants.GHOST getGhost(int i) {
        switch (i) {
            case 0:
                return Constants.GHOST.BLINKY;
            case 1:
                return Constants.GHOST.INKY;
            case 2:
                return Constants.GHOST.PINKY;
            case 3:
                return Constants.GHOST.SUE;
            default:
                return null;
        }
    }
}