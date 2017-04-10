package entrants.ghosts.gzae;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;


public class Pinky extends IndividualGhostController {

    public Pinky() {
        super(Constants.GHOST.PINKY);
    }

    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        return null;
    }
}
