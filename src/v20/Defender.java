package v20;

import battlecode.common.*;

public class Defender {

    private static boolean isDistressed = false;
    private static void assignPosition(RobotController rc) throws GameActionException {
        SharedVariables.flagIdToProtect = Communication.getDefenderFlagId(rc, SharedVariables.flagIdToProtect);
    }

    public static void runDefender(RobotController rc) throws GameActionException {
        assignPosition(rc);
        SharedVariables.flagToProtect = Communication.getFriendlyFlagLocationFromId(rc, SharedVariables.flagIdToProtect);

        rc.setIndicatorString("Defender - Targetting ID = " + SharedVariables.flagIdToProtect + " at " + SharedVariables.flagToProtect);

        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS)
        {
            FlagSetup.moveFlags(rc);
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

        boolean dontMove = Combat.interceptFlagStealing(rc); //Try to intercept a possible steal
        if (SharedVariables.verbose)
        {
            System.out.println("Dont Move = " + dontMove);
        }

        //If nobody is holding the flag, never move to attack
        if (!dontMove) {
            if (SharedVariables.flagToProtect != null) {
                Pathing.moveTowards(rc, SharedVariables.flagToProtect);
            } else {
                Exploration.explore(rc);
            }
        }

        if (rc.getLocation().equals(SharedVariables.flagToProtect))
        {
            //Get closest enemy and place a stun trap between it and the bot
            if (rc.canBuild(TrapType.STUN, SharedVariables.flagToProtect) && Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.STUN), SharedVariables.flagToProtect))
            {
                Communication.logCrumbsDefense(rc, Economy.getTrapCost(rc, TrapType.STUN), SharedVariables.flagToProtect);
                rc.build(TrapType.STUN, SharedVariables.flagToProtect);
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
                    if (closestDir != null && Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.STUN), SharedVariables.flagToProtect) && rc.canBuild(TrapType.STUN, rc.getLocation().add(closestDir)))
                    {
                        Communication.logCrumbsDefense(rc, Economy.getTrapCost(rc, TrapType.STUN), SharedVariables.flagToProtect);
                        rc.build(TrapType.STUN, rc.getLocation().add(closestDir));
                    }
                }
                else if (Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.STUN), SharedVariables.flagToProtect))
                {
                    for (int i = 0; i < Constants.directions.length; i++)
                    {
                        Direction trapDir = Constants.directions[(i + SharedVariables.rng.nextInt(20)) % Constants.directions.length];
                        MapLocation trapLoc = rc.getLocation().add(trapDir);
                        if (rc.canBuild(TrapType.STUN, trapLoc) && Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.STUN), SharedVariables.flagToProtect))
                        {
                            Communication.logCrumbsDefense(rc, Economy.getTrapCost(rc, TrapType.STUN), SharedVariables.flagToProtect);
                            rc.build(TrapType.STUN, trapLoc);
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
        if (!dontMove)
        {
            FlagInfo[] nearFlags = rc.senseNearbyFlags(-1, rc.getTeam());
            for (int i = 0; i < nearFlags.length; i++)
            {
                if (nearFlags[i].getID() == SharedVariables.flagIdToProtect && !nearFlags[i].isPickedUp())
                {
                    dontMove = true;
                    break;
                }
            }
        }
        Combat.micro(rc, dontMove);
    }
}
