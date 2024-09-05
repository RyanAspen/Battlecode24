package v5_POST;

import battlecode.common.Direction;

public class Constants {

    static final int FLAG_IDX = 0;
    static final int MAX_NUM_FLAGS_PER_TEAM = 4;
    static final int CLUSTER_IDX = FLAG_IDX + MAX_NUM_FLAGS_PER_TEAM*4;
    static final int MAX_NUM_CLUSTERS = 3;
    static final int MIN_DISTANCE_BETWEEN_CLUSTERS = 20;
    static final int TIME_FOR_CLUSTER = 50;
    static final int TIME_TO_RESET_TRAP_REGION = 30;
    static final int STANDARD_ID_IDX = CLUSTER_IDX + MAX_NUM_CLUSTERS*2;
    static final int IN_DANGER_HEALTH = 400;

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

    public static final Direction[] MOVEABLE_DIRECTIONS = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };
}
