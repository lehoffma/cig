package entrants.pacman.gzae;

import java.util.Arrays;
import java.util.LinkedList;

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
    private static final int DISTANCE_THRESHOLD = 3;


    private int[] ghostPos = {-1,-1,-1,-1};
    private byte[] lastSeen = {0,0,0,0};
    private int[] edibleTime = {0,0,0,0};

    public MOVE getMove(Game game, long timeDue) {
        // we will only simulate the next moves in a junction,
        // otherwise we will walk straight along the hallway
        int myNodeIndex = game.getPacmanCurrentNodeIndex();


        // for every ghost
        for (int i=0;i<ghostPos.length;++i) {

            // Try to get Ghost Information
            int pos = game.getGhostCurrentNodeIndex(getGhost(i));
            int edible = game.getGhostEdibleTime(getGhost(i));

            // One Timestep later, so increase the last seen and decrease the edibleTime
            lastSeen[i]++;
            if (edibleTime[i]>0) edibleTime[i]--;

            // if we found a ghost, update infos
            if (pos!=-1) {
                ghostPos[i] = pos;
                edibleTime[i]=edible;
                lastSeen[i]= 0;
            }

            // if not seen for a long time
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

    private MOVE nonJunctionSim(Game game){
        // get the current position of PacMan (returns -1 in case you can't see PacMan)
        int myNodeIndex = game.getPacmanCurrentNodeIndex();

        // get all possible moves at the queried position
        MOVE[] myMoves = game.getPossibleMoves(myNodeIndex);
        LinkedList<MOVE> myMoves2 = new LinkedList<>();
        int[] distances = new int[myMoves.length];


        for (int i=0;i<myMoves.length;++i) {
            int idx = game.getNeighbour(myNodeIndex,myMoves[i]);
            distances[i]= 9999;
            System.out.println("Pos:" + idx);
            for (int j=0;j<ghostPos.length;++j){

                if (ghostPos[j]>=0&&edibleTime[j]==0)
                    distances[i] = Math.min(distances[i], (int) game.getDistance(idx,ghostPos[j], Constants.DM.PATH));
            }

            if (distances[i]>DISTANCE_THRESHOLD){
                myMoves2.add(myMoves[i]);
            }
            System.out.println(myMoves2);

        }
        if (myMoves2.isEmpty())
        {
            for (MOVE m:myMoves) {
                myMoves2.add(m);

            }
        }
        if (myMoves2.size()==1) return myMoves2.get(0);

        MOVE lastMove = game.getPacmanLastMoveMade();
        if (myMoves2.contains(lastMove)){
            return lastMove;
        }

        // don't go back (corner)
        for (MOVE move : myMoves2){
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

        // if sim result is better than current situation
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


    public static MOVE nonJunctionSimStatic(Game game){
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
}