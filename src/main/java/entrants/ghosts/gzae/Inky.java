package entrants.ghosts.gzae;

import pacman.controllers.IndividualGhostController;
import pacman.game.Constants;
import pacman.game.Game;


public class Inky extends IndividualGhostController {

    public Inky() {
        super(Constants.GHOST.INKY);
    }

    @Override
    public Constants.MOVE getMove(Game game, long timeDue) {
        return null;
    }
}
