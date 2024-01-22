package v21;

import battlecode.common.*;

public class Defender {

    private static boolean isDistressed = false;
    private static void assignPosition(RobotController rc) throws GameActionException {
        SharedVariables.flagIdToProtect = Communication.getDefenderFlagId(rc, SharedVariables.flagIdToProtect);
        SharedVariables.flagToProtect = Communication.getFriendlyFlagLocationFromId(rc, SharedVariables.flagIdToProtect);
    }

    public static void runDefender(RobotController rc) throws GameActionException {
        //If I'm near a friendly flag, don't move for any reason
        boolean canMove = true;
        assignPosition(rc);

        /*
        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS)
        {
            FlagSetup.moveFlags(rc);
        }
         */
        rc.setIndicatorString("Defender - Targetting ID = " + SharedVariables.flagIdToProtect + " at " + SharedVariables.flagToProtect);

        //canMove = Combat.micro(rc, canMove);
        canMove = !Micro.micro(rc);

        //ALWAYS GO TOWARDS ALLIES provided that there aren't already many nearby allies
        Micro.goNearAllies(rc, canMove);

        if (rc.getLocation().equals(SharedVariables.flagToProtect))
        {
            RobotInfo[] enemies = rc.senseNearbyRobots(-1, rc.getTeam().opponent());
            //Get closest enemy and place a stun trap between it and the bot
            if (rc.canBuild(TrapType.STUN, SharedVariables.flagToProtect) && Economy.canSpendDefense(rc, Economy.getTrapCost(rc, TrapType.STUN)))
            {
                //Communication.logCrumbsDefense(rc, Economy.getTrapCost(rc, TrapType.STUN), SharedVariables.flagToProtect);
                rc.build(TrapType.STUN, SharedVariables.flagToProtect);
            }
            /*
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

             */
        }

        if (canMove) {
            if (SharedVariables.flagToProtect != null) {
                Pathing.moveTowards(rc, SharedVariables.flagToProtect);
            } else {
                Exploration.explore(rc);
            }
        }
    }
}
