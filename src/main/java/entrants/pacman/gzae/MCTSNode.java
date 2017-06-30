package entrants.pacman.gzae;

import pacman.controllers.MASController;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;


public class MCTSNode {
    /**
     * The move the simulation is based on (i.e. the first move in the simulation)
     */
    public MOVE move;
    /**
     * The score of the simulation (based on the game's score after simulating)
     */
    public double score = -1;


    private static Random random = new Random();

    /**
     * @param move the move the simulation is based on (i.e. the first move in the simulation)
     */
    public MCTSNode(MOVE move) {
        this.move = move;
    }

    /**
     * @return A string representation of the monte carlo node
     */
    @Override
    public String toString() {
        return "<Node: previous_move = " + this.move.name() +
                "; score = " + this.score;
    }

    /**
     * Performs a simulation "simulation" times. For every simulation, we look ahead until we reach the given amount
     * of junctions. Every time we reach a junction inside these simulations, we choose a random move.
     * After all simulations are done and their scores are calculated, the final resulting score will be calculated
     * by calculating the mean of all the simulation's scores.
     * @param game the game the simulations will be based on
     * @param ghosts a map containing observed infos (positions, edible times) about the ghosts in
     *                   the currently active game
     * @param simulations how many iterations should be performed
     * @param maximalLookahead how many junctions we want to look ahead
     * @return this
     */
    public MCTSNode doSimulations(Game game, MASController ghosts, int simulations, int maximalLookahead) {
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
        return this;
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
