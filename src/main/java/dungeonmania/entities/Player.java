package dungeonmania.entities;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import dungeonmania.Game;
import dungeonmania.battles.BattleStatistics;
import dungeonmania.battles.Battleable;
import dungeonmania.entities.collectables.Bomb;
import dungeonmania.entities.collectables.Key;
import dungeonmania.entities.collectables.Treasure;
import dungeonmania.entities.collectables.potions.InvincibilityPotion;
import dungeonmania.entities.collectables.potions.Potion;
import dungeonmania.entities.enemies.Enemy;
import dungeonmania.entities.enemies.Mercenary;
import dungeonmania.entities.inventory.Inventory;
import dungeonmania.entities.inventory.InventoryItem;
import dungeonmania.entities.playerState.BaseState;
import dungeonmania.entities.playerState.PlayerState;
import dungeonmania.entities.playerState.InvincibleState;
import dungeonmania.entities.playerState.InvisibleState;
import dungeonmania.map.GameMap;
import dungeonmania.util.Direction;
import dungeonmania.util.Position;

public class Player extends PlaceableEntity implements Battleable {
    public static final double DEFAULT_ATTACK = 5.0;
    public static final double DEFAULT_HEALTH = 5.0;
    private BattleStatistics battleStatistics;
    private Inventory inventory;
    private Queue<Potion> potionQueue = new LinkedList<>();
    private int nextTrigger = 0;

    private int collectedTreasureCount = 0;
    private int defeatedEnemyCount = 0;

    private PlayerState state;
    private InvincibleState invincibleState = new InvincibleState();
    private InvisibleState invisibleState = new InvisibleState();
    private BaseState baseState = new BaseState();

    public Player(Position position, double health, double attack) {
        super(position);
        battleStatistics = new BattleStatistics(health, attack, 0, BattleStatistics.DEFAULT_DAMAGE_MAGNIFIER,
                BattleStatistics.DEFAULT_PLAYER_DAMAGE_REDUCER);
        inventory = new Inventory();
        changeState(baseState);
    }

    public int getCollectedTreasureCount() {
        return collectedTreasureCount;
    }

    public int getDefeatedEnemyCount() {
        return defeatedEnemyCount;
    }

    public boolean hasWeapon() {
        return inventory.hasWeapon();
    }

    public BattleItem getWeapon() {
        return inventory.getWeapon();
    }

    public List<String> getBuildables() {
        return inventory.getBuildables();
    }

    public void wonBattle() {
        defeatedEnemyCount++;
    }

    public boolean build(String entity, EntityFactory factory) {
        InventoryItem item = inventory.checkBuildCriteria(this, true, entity.equals("shield"), factory);
        if (item == null)
            return false;
        return inventory.add(item);
    }

    public void move(GameMap map, Direction direction) {
        this.setFacing(direction);
        map.moveTo(this, Position.translateBy(this.getPosition(), direction));
    }

    @Override
    public void onOverlap(GameMap map, PlaceableEntity entity) {
        if (entity instanceof Enemy && !state.isInvisible()) {
            if (entity instanceof Mercenary) {
                if (((Mercenary) entity).isAllied())
                    return;
            }
            map.getGame().battle(this, (Enemy) entity);
        }
    }

    @Override
    public boolean canMoveOnto(GameMap map, PlaceableEntity entity) {
        return true;
    }

    public Entity getEntity(String itemUsedId) {
        return inventory.getEntity(itemUsedId);
    }

    public boolean pickUp(PlaceableEntity item) {
        if (item instanceof Treasure)
            collectedTreasureCount++;
        return inventory.add((InventoryItem) item);
    }

    public Inventory getInventory() {
        return inventory;
    }

    public <T extends InventoryItem> void use(Class<T> itemType) {
        T item = inventory.getFirst(itemType);
        if (item != null)
            inventory.remove(item);
    }

    public void use(Bomb bomb, GameMap map) {
        inventory.remove(bomb);
        bomb.onPutDown(map, getPosition());
    }

    public void triggerNext(int currentTick) {
        if (potionQueue.isEmpty()) {
            changeState(baseState);
            return;
        }
        Potion inEffect = potionQueue.remove();
        if (inEffect instanceof InvincibilityPotion) {
            changeState(invincibleState);
        } else {
            changeState(invisibleState);
        }
        nextTrigger = currentTick + inEffect.getDuration();
    }

    public void changeState(PlayerState playerState) {
        state = playerState;
    }

    public void use(Potion potion, int tick) {
        inventory.remove(potion);
        potionQueue.add(potion);
        if (state.isBaseState()) {
            triggerNext(tick);
        }
    }

    public void onTick(int tick) {
        if (state.isBaseState() || tick == nextTrigger) {
            triggerNext(tick);
        }
    }

    public boolean isInvincible() {
        return state.isInvincible();
    }

    public boolean isInvisible() {
        return state.isInvisible();
    }

    public boolean isBaseState() {
        return state.isBaseState();
    }

    public void remove(InventoryItem item) {
        inventory.remove(item);
    }

    @Override
    public BattleStatistics getBattleStatistics() {
        return battleStatistics;
    }

    public <T extends InventoryItem> int countEntityOfType(Class<T> itemType) {
        return inventory.count(itemType);
    }

    public BattleStatistics applyBuff(BattleStatistics origin) {
        if (state.isInvincible()) {
            return BattleStatistics.applyBuff(origin, new BattleStatistics(0, 0, 0, 1, 1, true, true));
        } else if (state.isInvisible()) {
            return BattleStatistics.applyBuff(origin, new BattleStatistics(0, 0, 0, 1, 1, false, false));
        }
        return origin;
    }

    public boolean hasKey(Door door) {
        Inventory inventory = getInventory();
        Key key = inventory.getFirst(Key.class);

        return (key != null && key.getnumber() == door.getNumber());
    }

    public void zombieToastSpawnerInteract(Game game) {
        this.getInventory().getWeapon().use(game);
    }
}