package v8;

import battlecode.common.*;

import java.util.Random;

/*
    This class defines how a bot with the Builder role behaves. They should
    - Build traps near friendly flags
    - Build traps on land near water
    - Fill near enemy territory
    - Dig near friendly territory
    - Don't go too far into enemy territory

 */
public class Builder {

    private static final int CRUMBS_TO_RESERVE = 2000;
    private static final int MAX_DIST_TO_BUILD_NEAR_FLAGS = 16;
    private static final int MIN_DIST_TO_FILL_ENEMY_TERRITORY = 80;

    private static final int MAX_DIST_TO_TRAVEL_FROM_HOME = 110;
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

    static Random rng = null;

    private static MapLocation closestFlag = null;

    private static int getDistFromFlags(RobotController rc)
    {
        int minDist = 9999;
        MapLocation[] flags = Communication.getFriendlyFlagLocations();
        for (int i = 0; i < flags.length; i++)
        {
            int dist = flags[i].distanceSquaredTo(rc.getLocation());
            if (dist < minDist)
            {
                closestFlag = flags[i];
                minDist = dist;
            }
        }
        return minDist;
    }

    private static boolean isNearWater(RobotController rc, MapLocation loc) throws GameActionException {
        for (int i = 0; i < directions.length; i++)
        {
            Direction waterDir = directions[i];
            MapLocation waterLoc = loc.add(waterDir);
            if (rc.onTheMap(waterLoc) && rc.senseMapInfo(waterLoc).isWater())
            {
                return true;
            }
        }
        return false;
    }

    public static void runBuilder(RobotController rc) throws GameActionException {
        if (rng == null)
        {
            rng = new Random(rc.getID());
        }
        int crumbs = rc.getCrumbs();
        int distToFlags = getDistFromFlags(rc);

        if (rc.getActionCooldownTurns() < 10 && crumbs >= CRUMBS_TO_RESERVE)
        {
            //If we're close to a friendly flag and have crumbs to spare, build explosive traps
            if (distToFlags <= MAX_DIST_TO_BUILD_NEAR_FLAGS)
            {
                for (int i = 0; i < directions.length; i++)
                {
                    Direction trapDir = directions[(i + rng.nextInt(20)) % directions.length];
                    MapLocation trapLoc = rc.getLocation().add(trapDir);
                    if (rc.canBuild(TrapType.EXPLOSIVE, trapLoc))
                    {
                        rc.build(TrapType.EXPLOSIVE, trapLoc);
                        break;
                    }
                }
            }
            //Fill near enemy territory
            else if (distToFlags >= MIN_DIST_TO_FILL_ENEMY_TERRITORY)
            {
                for (int i = 0; i < directions.length; i++)
                {
                    Direction fillDir = directions[(i + rng.nextInt(20)) % directions.length];
                    MapLocation fillLoc = rc.getLocation().add(fillDir);
                    if (rc.canFill(fillLoc))
                    {
                        rc.fill(fillLoc);
                        break;
                    }
                }
            }
            //Build traps on land near water
            if (rc.getActionCooldownTurns() < 10)
            {
                for (int i = 0; i < directions.length; i++)
                {
                    Direction trapDir = directions[(i + rng.nextInt(20)) % directions.length];
                    MapLocation trapLoc = rc.getLocation().add(trapDir);
                    if (rc.canBuild(TrapType.EXPLOSIVE, trapLoc) && isNearWater(rc, trapLoc))
                    {
                        rc.build(TrapType.EXPLOSIVE, trapLoc);
                        break;
                    }
                }
            }
        }

        //Standard micro
        Combat.micro(rc);

        //Explore, but not too close to enemy territory
        if (rc.getMovementCooldownTurns() < 10)
        {
            MapLocation exploreTarget = Exploration.getExploreTarget(rc);
            if (!closestFlag.isWithinDistanceSquared(exploreTarget, MAX_DIST_TO_TRAVEL_FROM_HOME))
            {
                Pathing.lingerTowards(rc, closestFlag, 40);
            }
            else
            {
                Pathing.moveTowards(rc, exploreTarget);
            }
        }
    }
}
