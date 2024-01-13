package v13;

import battlecode.common.Direction;
import battlecode.common.GameConstants;
import battlecode.common.SkillType;

public class Constants {

    //ATTACKER CLUSTERING
    public static final int MAX_DIST_TO_CLUSTER = 150;
    public static final int CLUSTER_LINGER_DIST = 20;

    public static final int MIN_CLUSTER_SIZE = 6;
    public static final int CLUSTER_CALL_COOLDOWN = 150;
    public static final int MAX_CLUSTER_TIME = 50;
    public static final int NUM_CLUSTER_POINTS = 2;
    public static final int MIN_DIST_BETWEEN_CLUSTERS = 50;

    //ATTACKER FOCUS FIRE
    public static final int MAX_MARKED_ENEMIES = 5;
    public static final int MIN_ENEMIES_FOR_CLUSTER = 4;
    public static final int ATTACK_FREQUENCY = (int) Math.round(GameConstants.ATTACK_COOLDOWN*(1+.01*SkillType.ATTACK.getCooldown(0))) / 10;

    //ATTACKER KITING
    public static final int MAX_UNSAFE_DIST = 9;

    //ATTACKER RETREATING
    public static final double MIN_HEALTH_FRACTION_BEFORE_RETREAT = 0.3;

    //ATTACKER PROTECT
    public static final int MAX_DIST_FOR_DISTRESS = 150;

    //ATTACKER DAMS
    private static final int TIME_TO_LEAVE_DAMS_BEFORE = 30;
    private static final int TIME_TO_LEAVE_DAMS_AFTER = 30;
    public static final int MIN_ROUND_RANGE_FROM_DAMS = GameConstants.SETUP_ROUNDS - TIME_TO_LEAVE_DAMS_BEFORE;
    public static final int MAX_ROUND_RANGE_FROM_DAMS = GameConstants.SETUP_ROUNDS + TIME_TO_LEAVE_DAMS_AFTER;

    //CRUMBS
    public static final double CRUMB_PROPORTION_ATTACK = 0.1;
    public static final double CRUMB_PROPORTION_DEFENSE_PER_FLAG = 0.2;
    public static final double CRUMB_PROPORTION_PATHING = 0.1;
    public static final double CRUMB_PROPORTION_FREE = 1 - CRUMB_PROPORTION_PATHING - CRUMB_PROPORTION_ATTACK - (CRUMB_PROPORTION_DEFENSE_PER_FLAG*GameConstants.NUMBER_FLAGS);

    //DIRECTIONS
    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
    public static final Direction[] cardinalDirections = {
            Direction.NORTH,
            Direction.EAST,
            Direction.SOUTH,
            Direction.WEST,
    };

    //CRUMB RESERVES
    public static final int CRUMBS_TO_RESERVE_HIGH = 2000;
    public static final int CRUMBS_TO_RESERVE_MEDIUM = 1000;
    public static final int CRUMBS_TO_RESERVE_LOW = 0;

    //EXPLORATION
    public static final int maxTimeExploring = 50;
    public static final int maxExplorationListLength = 12;
    public static final int minDistFromExistingLocation = 25;
    public static final int maxBytecodeToLeave = 25000 - 10000;

    //NORMAL
    public static final int MIN_DIST_FROM_SPAWN_TO_FILL = 50;

    //PATHING
    public static final int MAX_DIST_TO_RESET = 20;
    public static final int MIN_BYTECODE_LEFT_AFTER_SETUP = 1000;

    //STRATEGY
    public static final int MIN_DEFENDERS_TURTLE = 12;
    public static final int MIN_ATTACKERS_TURTLE = 38;
    public static final int MIN_DEFENDERS_BALANCED = 12;
    public static final int MIN_ATTACKERS_BALANCED = 38;
    public static final int MIN_DEFENDERS_AGGRESSIVE = 12;
    public static final int MIN_ATTACKERS_AGGRESSIVE = 38;
}
