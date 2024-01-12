package v10;

import battlecode.common.*;

/*
    This class defines the behavior of bots without a specialized role. They should
    - If given the opportunity to attack, attack!
    - If given the opportunity to heal a very damaged bot, heal!
    - Dig territory close to friendly flags
    - Fill territory far away from friendly flags
    - Explore

 */
public class Normal {

    private static final int MIN_DIST_FROM_SPAWN_TO_FILL = 50;

    private static final int CRUMBS_TO_RESERVE = 2000;

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

    private static int getDistFromSpawn(RobotController rc)
    {
        int minDist = 9999;
        MapLocation[] spawnPoints = rc.getAllySpawnLocations();
        for (int i = 0; i < spawnPoints.length; i++)
        {
            int dist = spawnPoints[i].distanceSquaredTo(rc.getLocation());
            if (dist < minDist)
            {
                minDist = dist;
            }
        }
        return minDist;
    }

    public static void runNormal(RobotController rc) throws GameActionException {
        Combat.micro(rc); //Attack and heal (and move strategically)

        if (rc.getMovementCooldownTurns() < 10)
        {
            //If close to a friendly flag and we have enough crumbs, dig
            FlagInfo[] friendlyFlags = rc.senseNearbyFlags(-1, rc.getTeam());
            if (friendlyFlags.length > 0 && rc.getCrumbs() >= CRUMBS_TO_RESERVE)
            {
                for (int i = 0; i < directions.length; i++)
                {
                    MapLocation digLoc = rc.getLocation().add(directions[i]);
                    if (rc.canDig(digLoc))
                    {
                        rc.dig(digLoc);
                        break;
                    }
                }
            }
            //If far away from spawn points, fill if able
            else if (getDistFromSpawn(rc) > MIN_DIST_FROM_SPAWN_TO_FILL && rc.getCrumbs() >= CRUMBS_TO_RESERVE)
            {
                for (int i = 0; i < directions.length; i++)
                {
                    MapLocation fillLoc = rc.getLocation().add(directions[i]);
                    if (rc.canFill(fillLoc))
                    {
                        rc.fill(fillLoc);
                        break;
                    }
                }
            }
        }

        //If we can explore, explore (fill if needed)
        if (rc.getMovementCooldownTurns() < 10)
        {
            Pathing.moveTowards(rc, Exploration.getExploreTarget(rc));
        }
    }
}
