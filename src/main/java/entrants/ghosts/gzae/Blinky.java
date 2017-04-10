package entrants.ghosts.gzae;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;

public class Blinky extends IndividualGhostController {


    public Blinky() {
        super(Constants.GHOST.BLINKY);
    }

    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        return null;
    }
}