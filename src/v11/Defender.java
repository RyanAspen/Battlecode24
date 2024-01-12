package v11;

import battlecode.common.*;

import java.util.Random;

/*
    This class defines how a bot with the Defender role behaves. They should
    - Choose a friendly flag and stay on top of it
    - Place Explosive Traps around and on flag
    - If below half health, send a distress call
 */
public class Defender {

    private static MapLocation flagToProtect = null;
    private static boolean distressed = false;
    private static void assignPosition(RobotController rc) throws GameActionException {
        if (flagToProtect != null)
        {
            Communication.removeDefenderPosition(rc, flagToProtect);
        }
        MapLocation newFlag = Communication.getDefenderPosition(rc);
        if (flagToProtect != null && !flagToProtect.equals(newFlag) && distressed) // Get rid of old distress call
        {
            distressed = false;
            Communication.removeDistressSignal(rc, flagToProtect);
        }
        flagToProtect = newFlag;
    }

    public static void runDefender(RobotController rc) throws GameActionException {

        if (flagToProtect == null || !flagToProtect.isWithinDistanceSquared(rc.getLocation(), 20))
        {
            assignPosition(rc);
        }

        //If an enemy flag carrier is close, don't let them get away!
        RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
        for (int i = 0; i < enemies.length; i++)
        {
            if (enemies[i].hasFlag())
            {
                Pathing.moveTowards(rc, enemies[i].getLocation());
            }
        }

        if (flagToProtect != null)
        {
            Pathing.moveTowards(rc, flagToProtect);
        }
        else
        {
            Pathing.moveTowards(rc, Exploration.getExploreTarget(rc));
        }



        if (rc.getLocation().equals(flagToProtect))
        {
            //Get closest enemy and place an explosive trap between it and the bot
            if (rc.canBuild(TrapType.EXPLOSIVE, flagToProtect))
            {
                rc.build(TrapType.EXPLOSIVE, flagToProtect);
            }
            else
            {
                MapLocation closestLoc = null;
                if (enemies.length > 0)
                {
                    int minDist = 9999;
                    for (int i = 0; i < enemies.length; i++)
                    {
                        int dist = enemies[i].getLocation().distanceSquaredTo(flagToProtect);
                        if (dist < minDist)
                        {
                            minDist = dist;
                            closestLoc = enemies[i].getLocation();
                        }
                    }
                    Direction closestDir = rc.getLocation().directionTo(closestLoc);
                    if (closestDir != null && rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation().add(closestDir)))
                    {
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation().add(closestDir));
                    }
                }
                else if (rc.getCrumbs() >= Constants.CRUMBS_TO_RESERVE_LOW)
                {
                    for (int i = 0; i < Constants.directions.length; i++)
                    {
                        Direction trapDir = Constants.directions[(i + SharedVariables.rng.nextInt(20)) % Constants.directions.length];
                        MapLocation trapLoc = rc.getLocation().add(trapDir);
                        if (rc.canBuild(TrapType.EXPLOSIVE, trapLoc))
                        {
                            rc.build(TrapType.EXPLOSIVE, trapLoc);
                            break;
                        }
                    }
                }
            }
        }
        //If below half health, send a distress call
        if (rc.getHealth() < 500)
        {
            Communication.sendDistressSignal(rc, flagToProtect);
            distressed = true;
        }

        Combat.micro(rc);
    }
}
