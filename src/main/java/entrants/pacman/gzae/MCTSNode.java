package entrants.pacman.gzae;

/**
 * Created by Gzae on 17.06.2017.
 */

        import pacman.game.Game;

        import java.util.Random;

        import pacman.controllers.MASController;
        import pacman.game.Constants.MOVE;


public class MCTSNode {

    public MOVE move;
    public double score;
    public static Random random = new Random();

    public MCTSNode(MOVE move){
        this.move = move;
        this.score = -1;
    }

    @Override
    public String toString(){
        return "<Node: previous_move = " + this.move.name() +
                "; score = " + this.score;
    }

    public void doSimulations(Game game, MASController ghosts, int simulations, int maximalLookahead){
        double[] scores = new double[simulations];

        for (int i = 0; i < simulations; i++){
            Game forwardCopy = game.copy();

            // Have to forward once before the loop - so that we aren't on a junction
            forwardCopy.advanceGame(move, ghosts.getMove(forwardCopy.copy(), 40));

            for (int j = 0; j < maximalLookahead; j++){
                // Repeat simulation till we find the next junction
                while(!forwardCopy.isJunction(forwardCopy.getPacmanCurrentNodeIndex())){
                    forwardCopy.advanceGame(MyPacMan.nonJunctionSimStatic(forwardCopy), ghosts.getMove(forwardCopy.copy(), 40));
                }

                // once again leave the junction before extending the simulation
                MOVE[] possibleMoves = forwardCopy.getPossibleMoves(forwardCopy.getPacmanCurrentNodeIndex());
                forwardCopy.advanceGame(possibleMoves[random.nextInt(possibleMoves.length)],
                        ghosts.getMove(forwardCopy.copy(), 40));
            }

            scores[i] = getValue(forwardCopy);
        }

        this.score = mean(scores);
    }


    public static double mean(double[] m) {
        double sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }


    public double getValue(Game game){
        return game.getScore() + game.getPacmanNumberOfLivesRemaining()*1000;
    }



}
