package v11;

import battlecode.common.Direction;

public class Constants {

    //ATTACKER CLUSTERING
    public static final int MAX_DIST_TO_CLUSTER = 150;
    public static final int CLUSTER_LINGER_DIST = 20;

    public static final int MIN_CLUSTER_SIZE = 6;
    public static final int CLUSTER_CALL_COOLDOWN = 150;
    public static final int MAX_CLUSTER_TIME = 50;
    public static final int NUM_CLUSTER_POINTS = 2;
    public static final int MIN_DIST_BETWEEN_CLUSTERS = 50;


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
