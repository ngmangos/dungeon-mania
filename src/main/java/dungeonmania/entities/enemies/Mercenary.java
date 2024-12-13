package dungeonmania.entities.enemies;

import dungeonmania.Game;
import dungeonmania.battles.BattleStatistics;
import dungeonmania.entities.PlaceableEntity;
import dungeonmania.entities.Interactable;
import dungeonmania.entities.Player;
import dungeonmania.entities.collectables.Treasure;
import dungeonmania.entities.enemies.movementbehaviour.FollowPlayer;
import dungeonmania.entities.enemies.movementbehaviour.MoveAwayFromPlayer;
import dungeonmania.entities.enemies.movementbehaviour.MoveTowardPlayer;
import dungeonmania.entities.enemies.movementbehaviour.MovementBehaviour;
import dungeonmania.entities.enemies.movementbehaviour.RandomMovement;
import dungeonmania.map.GameMap;
import dungeonmania.util.Position;

public class Mercenary extends Enemy implements Interactable {
    public static final int DEFAULT_BRIBE_AMOUNT = 1;
    public static final int DEFAULT_BRIBE_RADIUS = 1;
    public static final double DEFAULT_ATTACK = 5.0;
    public static final double DEFAULT_HEALTH = 10.0;

    private int bribeAmount = Mercenary.DEFAULT_BRIBE_AMOUNT;
    private int bribeRadius = Mercenary.DEFAULT_BRIBE_RADIUS;

    private double allyAttack;
    private double allyDefence;
    private boolean allied = false;
    private boolean isAdjacentToPlayer = false;
    private MovementBehaviour movementBehaviour;

    public Mercenary(Position position, double health, double attack, int bribeAmount, int bribeRadius,
            double allyAttack, double allyDefence) {
        super(position, health, attack);
        this.bribeAmount = bribeAmount;
        this.bribeRadius = bribeRadius;
        this.allyAttack = allyAttack;
        this.allyDefence = allyDefence;
    }

    public boolean isAllied() {
        return allied;
    }

    @Override
    public void onOverlap(GameMap map, PlaceableEntity entity) {
        if (allied)
            return;
        super.onOverlap(map, entity);
    }

    /**
     * check whether the current merc can be bribed
     * @param player
     * @return
     */
    private boolean canBeBribed(Player player) {
        return bribeRadius >= 0 && player.countEntityOfType(Treasure.class) >= bribeAmount;
    }

    /**
     * bribe the merc
     */
    private void bribe(Player player) {
        for (int i = 0; i < bribeAmount; i++) {
            player.use(Treasure.class);
        }

    }

    @Override
    public void interact(Player player, Game game) {
        allied = true;
        bribe(player);
        if (!isAdjacentToPlayer && Position.isAdjacent(player.getPosition(), getPosition()))
            isAdjacentToPlayer = true;
    }

    @Override
    public void move(Game game) {
        GameMap map = game.getMap();
        Player player = game.getPlayer();
        if (allied) {
            movementBehaviour = new FollowPlayer(map, this, player);
            if (!isAdjacentToPlayer && Position.isAdjacent(player.getPosition(), movementBehaviour.nextPosition()))
                isAdjacentToPlayer = true;
        } else if (player.isInvisible()) {
            movementBehaviour = new RandomMovement(map, this);
        } else if (player.isInvincible()) {
            movementBehaviour = new MoveAwayFromPlayer(map, this, player);
        } else {
            movementBehaviour = new MoveTowardPlayer(map, this, player);
        }
        map.moveTo(this, movementBehaviour.nextPosition());
    }

    public boolean isAdjacentToPlayer() {
        return isAdjacentToPlayer;
    }

    @Override
    public boolean isInteractable(Player player) {
        return !allied && canBeBribed(player);
    }

    @Override
    public BattleStatistics getBattleStatistics() {
        if (!allied)
            return super.getBattleStatistics();
        return new BattleStatistics(0, allyAttack, allyDefence, 1, 1);
    }
}
