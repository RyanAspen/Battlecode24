package v17;

import battlecode.common.Direction;
import battlecode.common.GameConstants;

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

    //ATTACKER KITING
    public static final int MAX_UNSAFE_DIST = 9;

    //ATTACKER RETREATING
    public static final double MIN_HEALTH_FRACTION_BEFORE_RETREAT = 0.3;

    //ATTACKER DAMS
    private static final int TIME_TO_LEAVE_DAMS_BEFORE = 30;
    private static final int TIME_TO_LEAVE_DAMS_AFTER = 30;
    public static final int MIN_ROUND_RANGE_FROM_DAMS = GameConstants.SETUP_ROUNDS - TIME_TO_LEAVE_DAMS_BEFORE;

    //DEFENSE
    public static final int maxNumberOfDistressCalls = 1;
    public static final int MAX_DISTANCE_TO_FOLLOW_DISTRESS = 100;
    public static final int TIME_FLAG_UNSEEN_UNTIL_LOST = 3;

    //COMMS
    public static final int FRIENDLY_FLAG_LOC_IDX = 0;
    public static final int ENEMY_FLAG_LOC_IDX = GameConstants.NUMBER_FLAGS;
    public static final int ATTACKER_ROLE_IDX = ENEMY_FLAG_LOC_IDX + GameConstants.NUMBER_FLAGS;

    public static final int DEFENDER_ROLE_IDX = ATTACKER_ROLE_IDX+1;
    public static final int CLUSTER_POINT_IDX = DEFENDER_ROLE_IDX+1;
    public static final int MAX_DEFENDERS_PER_FLAG = 4;
    public static final int DISTRESS_IDX = CLUSTER_POINT_IDX + NUM_CLUSTER_POINTS;
    public static final int MARKED_ENEMIES_IDX = DISTRESS_IDX + maxNumberOfDistressCalls;
    public static final int ROUND_NUM_IDX = MARKED_ENEMIES_IDX + Constants.MAX_MARKED_ENEMIES;
    public static final int CRUMBS_PATHING_IDX = ROUND_NUM_IDX + 1;
    public static final int CRUMBS_DEFENDING_IDX = CRUMBS_PATHING_IDX + 1;
    public static final int CRUMBS_ATTACKING_IDX = CRUMBS_DEFENDING_IDX + GameConstants.NUMBER_FLAGS;
    public static final int CRUMBS_FREE_IDX = CRUMBS_ATTACKING_IDX + 1;
    public static final int PREFERED_FLAG_IDX = CRUMBS_FREE_IDX + 1;
    public static final int PREFERED_SPAWN_IDX = PREFERED_FLAG_IDX + 1;
    public static final int PREFERED_FLAG_ROUND_END_IDX = PREFERED_SPAWN_IDX + 1;
    public static final int FRIENDLY_FLAG_COUNT_IDX = PREFERED_FLAG_ROUND_END_IDX + 1;
    public static final int ENEMY_FLAG_COUNT_IDX = FRIENDLY_FLAG_COUNT_IDX + 1;
    public static final int FRIENDLY_FLAG_IDS_IDX = ENEMY_FLAG_COUNT_IDX + 1;
    public static final int FRIENDLY_FLAGS_LAST_SEEN_IDX = FRIENDLY_FLAG_IDS_IDX + GameConstants.NUMBER_FLAGS;
    public static final int STRATEGY_IDX = FRIENDLY_FLAGS_LAST_SEEN_IDX + GameConstants.NUMBER_FLAGS;
    public static final int FLAG_CARRIER_LOC_IDX = STRATEGY_IDX + 1;
    public static final int FLAG_CARRIER_LAST_SENT_IDX = FLAG_CARRIER_LOC_IDX + 1;
    public static final int LIVE_DEFENDER_COUNTS_IDX = FLAG_CARRIER_LAST_SENT_IDX + 1;
    public static final int TEMP_LIVE_DEFENDER_COUNTS_IDX = LIVE_DEFENDER_COUNTS_IDX + GameConstants.NUMBER_FLAGS;
    //CRUMBS
    public static final double CRUMB_PROPORTION_ATTACK = 0.2;
    public static final double CRUMB_PROPORTION_DEFENSE_PER_FLAG = 0.15;
    public static final double CRUMB_PROPORTION_PATHING = 0.05;
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
    public static final int TURTLE_STRATEGY = 0;
    public static final int MIN_DEFENDERS_TURTLE = 50;
    public static final int MIN_ATTACKERS_TURTLE = 0;
    public static final int BALANCED_STRATEGY = 1;
    public static final int MIN_DEFENDERS_BALANCED = 25;
    public static final int MIN_ATTACKERS_BALANCED = 25;
    public static final int AGGRESSIVE_STRATEGY = 2;
    public static final int MIN_DEFENDERS_AGGRESSIVE = 15;
    public static final int MIN_ATTACKERS_AGGRESSIVE = 35;
}
