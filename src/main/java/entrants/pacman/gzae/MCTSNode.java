package entrants.pacman.gzae;

import pacman.controllers.MASController;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class MCTSNode {
    public MOVE move;
    public double score;
    private static Random random = new Random();

    public MCTSNode(MOVE move) {
        this.move = move;
        this.score = -1;
    }

    @Override
    public String toString() {
        return "<Node: previous_move = " + this.move.name() +
                "; score = " + this.score;
    }

    public void doSimulations(Game game, MASController ghosts, int simulations, int maximalLookahead) {
        List<Double> scores = new ArrayList<>();
        for (int i = 0; i < simulations; i++) {
            Game gameCopy = game.copy();
            // Have to forward once before the loop - so that we aren't on a junction
            gameCopy.advanceGame(move, ghosts.getMove(gameCopy.copy(), 40));

            for (int j = 0; j < maximalLookahead; j++) {
                // Repeat simulation till we find the next junction
                while (!gameCopy.isJunction(gameCopy.getPacmanCurrentNodeIndex())) {
                    gameCopy.advanceGame(MyPacMan.nonJunctionSim(gameCopy, new HashMap<>()),
                            ghosts.getMove(gameCopy.copy(), 40));
                }

                // once again leave the junction before extending the simulation
                MOVE[] possibleMoves = gameCopy.getPossibleMoves(gameCopy.getPacmanCurrentNodeIndex());
                gameCopy.advanceGame(possibleMoves[random.nextInt(possibleMoves.length)],
                        ghosts.getMove(gameCopy.copy(), 40));
            }

            scores.add(this.getValue(gameCopy));
        }

        //calculate mean of all scores
        this.score = scores.stream().reduce(0.0, (acc, score) -> acc + score) / scores.size();
    }

    /**
     * The value function for the monte carlo tree search.
     *
     * @param game
     * @return
     */
    public double getValue(Game game) {
        return game.getScore() + game.getPacmanNumberOfLivesRemaining() * 1000;
    }


}
