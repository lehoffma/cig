package entrants.pacman.gzae;

/**
 * Created by Gzae on 17.06.2017.
 */
import pacman.game.Constants;
import pacman.game.Constants.MOVE;
import pacman.game.info.GameInfo;
import pacman.game.internal.Ghost;

import java.util.LinkedList;

import pacman.controllers.MASController;
import pacman.controllers.examples.po.POCommGhosts;
import pacman.game.Game;

public class MCTSTree {

    private Game game;
    private MASController ghosts;
    private LinkedList<MCTSNode> simulatedMoves;

    private static final int MaxLookahead = 3;
    private static final int SimulationsPerMove = 3;

    private static final boolean Debug = false;

    public MCTSTree(Game game,int[] ghostPos,int[] edibleTime){
        Game coGame;
        GameInfo info = game.getPopulatedGameInfo();
       /* info.fixGhosts((ghost) -> new Ghost(
                ghost,
                game.getCurrentMaze().lairNodeIndex,
                -1,
                -1,
                MOVE.NEUTRAL
        )); */
        for (int i=0;i<Constants.GHOST.values().length;++i)
        {
            if (Debug)
                System.out.println("i: "+i+" Pos: "+ ghostPos[i]+" edibleTime: "+ edibleTime[i]);


            Ghost g = new Ghost(MyPacMan.getGhost(i),ghostPos[i]>=0 ? ghostPos[i]:game.getCurrentMaze().lairNodeIndex,edibleTime[i],-1,MOVE.NEUTRAL);
            info.setGhost(MyPacMan.getGhost(i),g.copy());
        }
        coGame = game.getGameFromInfo(info);

        this.game = coGame;

        // Make some ghosts for simulation purposes
        this.ghosts = new POCommGhosts(50);
        simulatedMoves = new LinkedList<MCTSNode>();
    }


    public void simulate(double timeDue){
        if (Debug)
            System.out.println("Start simulation");

        // get all possible moves at the queried position
        int myNodeIndex = game.getPacmanCurrentNodeIndex();
        MOVE[] myMoves = game.getPossibleMoves(myNodeIndex);
        simulatedMoves.clear();

        for (MOVE move : myMoves) {

            // Create a node in the game tree and perform simulations on it
            MCTSNode node = new MCTSNode(move);
            simulatedMoves.add(node);
            node.doSimulations(game, ghosts, SimulationsPerMove, MaxLookahead);

            if (Debug)
                System.out.println(node.toString());
        }

    }



    public MOVE getBestMove(){
        double bestScore = -1;
        MOVE bestMove = MOVE.NEUTRAL;

        for (MCTSNode node : this.simulatedMoves){
            if (node.score > bestScore){
                bestMove = node.move;
                bestScore = node.score;
            }
        }
        if (Debug)
            System.out.println("best Move: " + bestMove.name());
        return bestMove;
    }



    public double getBestScore(){
        double bestScore = -1;

        for (MCTSNode node : this.simulatedMoves){
            if (node.score > bestScore){
                bestScore = node.score;
            }
        }
        if (Debug)
            System.out.println("best Score: " + bestScore);
        return bestScore;
    }

}
