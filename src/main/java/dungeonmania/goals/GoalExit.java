package dungeonmania.goals;

import java.util.List;

import dungeonmania.Game;
import dungeonmania.entities.PlaceableEntity;
import dungeonmania.entities.Exit;
import dungeonmania.entities.Player;
import dungeonmania.util.Position;

public class GoalExit implements Goal {
    public boolean achieved(Game game) {
        if (game.getPlayer() == null) {
            return false;
        }

        Player character = game.getPlayer();
        Position pos = character.getPosition();
        List<Exit> es = game.getMap().getEntities(Exit.class);
        if (es == null || es.size() == 0)
            return false;
        return es.stream().map(PlaceableEntity::getPosition).anyMatch(pos::equals);
    }

    public String toString(Game game) {
        if (this.achieved(game)) {
            return "";
        }

        return ":exit";
    }
}
