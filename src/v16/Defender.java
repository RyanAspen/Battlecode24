package v16;

import battlecode.common.*;

/*
    This class defines how a bot with the Defender role behaves. They should
    - Choose a friendly flag and stay on top of it
    - Place Explosive Traps around and on flag
    - If below half health, send a distress call
 */
public class Defender {

    private static boolean isDistressed = false;
    private static void assignPosition(RobotController rc) throws GameActionException {
        SharedVariables.flagIdToProtect = Communication.getDefenderFlagId(rc, SharedVariables.flagIdToProtect);
    }

    public static void runDefender(RobotController rc) throws GameActionException {
        assignPosition(rc);
        SharedVariables.flagToProtect = Communication.getFriendlyFlagLocationFromId(rc, SharedVariables.flagIdToProtect);
        if (SharedVariables.flagToProtect == null)
        {
            rc.setIndicatorString("Defender - No Targets");
        }
        else
        {
            rc.setIndicatorString("Defender - Targetting " + SharedVariables.flagToProtect);
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

        if (SharedVariables.flagToProtect != null)
        {
            Pathing.moveTowards(rc, SharedVariables.flagToProtect);
        }
        else
        {
            Pathing.moveTowards(rc, Exploration.getExploreTarget(rc));
        }



        if (rc.getLocation().equals(SharedVariables.flagToProtect))
        {
            //Get closest enemy and place an explosive trap between it and the bot
            if (rc.canBuild(TrapType.EXPLOSIVE, SharedVariables.flagToProtect) && Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE), SharedVariables.flagToProtect))
            {
                Communication.logCrumbsDefense(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE), SharedVariables.flagToProtect);
                rc.build(TrapType.EXPLOSIVE, SharedVariables.flagToProtect);
            }
            else
            {
                MapLocation closestLoc = null;
                if (enemies.length > 0)
                {
                    int minDist = 9999;
                    for (int i = 0; i < enemies.length; i++)
                    {
                        int dist = enemies[i].getLocation().distanceSquaredTo(SharedVariables.flagToProtect);
                        if (dist < minDist)
                        {
                            minDist = dist;
                            closestLoc = enemies[i].getLocation();
                        }
                    }
                    Direction closestDir = rc.getLocation().directionTo(closestLoc);
                    if (closestDir != null && Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE), SharedVariables.flagToProtect) && rc.canBuild(TrapType.EXPLOSIVE, rc.getLocation().add(closestDir)))
                    {
                        Communication.logCrumbsDefense(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE), SharedVariables.flagToProtect);
                        rc.build(TrapType.EXPLOSIVE, rc.getLocation().add(closestDir));
                    }
                }
                else if (Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE), SharedVariables.flagToProtect))
                {
                    for (int i = 0; i < Constants.directions.length; i++)
                    {
                        Direction trapDir = Constants.directions[(i + SharedVariables.rng.nextInt(20)) % Constants.directions.length];
                        MapLocation trapLoc = rc.getLocation().add(trapDir);
                        if (rc.canBuild(TrapType.EXPLOSIVE, trapLoc) && Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE), SharedVariables.flagToProtect))
                        {
                            Communication.logCrumbsDefense(rc, Economy.getTrapCost(rc, TrapType.EXPLOSIVE), SharedVariables.flagToProtect);
                            rc.build(TrapType.EXPLOSIVE, trapLoc);
                            break;
                        }
                    }
                }
            }
        }
        /*
        //If we are outnumbered, send a distress call
        boolean outnumbered = rc.senseNearbyRobots(-1, rc.getTeam()).length + 1 < rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length;
        if (outnumbered && !isDistressed)
        {
            Communication.sendDistressSignal(rc, SharedVariables.flagToProtect);
            isDistressed = true;
        }
        else if (!outnumbered && isDistressed)
        {
            Communication.removeDistressSignal(rc, SharedVariables.flagToProtect);
            isDistressed = false;
        }

         */

        Combat.micro(rc);
    }
}
