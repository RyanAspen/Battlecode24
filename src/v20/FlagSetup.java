package v20;

import battlecode.common.*;

//File for flag moving at the beginning of the game
public class FlagSetup {

    private static int startAtX = -6;
    private static int startAtY = -6;
    private static int startAtStatusId = -1;
    private static MapLocation startAtBestLoc = null;
    private static MapLocation locationForFlag = null;
    private static int timeUntilRecalculation = 0;
    private static int flagIdCarrying = -1;

    public static void stayCloseToSpawn(RobotController rc) throws GameActionException {
        int minClose;
        if (rc.getRoundNum() >= GameConstants.SETUP_ROUNDS)
        {
            minClose = 0;
        }
        else
        {
            minClose = 3;
        }
        //Don't try to move early on if we are near a spawn and only a few other bot are near that spawn
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        for (int i = 0; i < spawnLocs.length; i++)
        {
            if (spawnLocs[i].isWithinDistanceSquared(rc.getLocation(), 9))
            {
                if (rc.senseNearbyRobots(spawnLocs[i], 9, rc.getTeam()).length < minClose)
                {
                    rc.setIndicatorString("Staying close to " + spawnLocs[i]);
                    Direction directionToSpawn = rc.getLocation().directionTo(spawnLocs[i]);
                    if (rc.canMove(directionToSpawn))
                    {
                        rc.move(directionToSpawn);
                    }
                    Clock.yield();
                }
            }
        }
    }

    public static FlagStatus getFlagToMove(RobotController rc) throws GameActionException {
        FlagStatus status = null;
        int minDist = 9999;
        int maxDist = -1;
        for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
        {
            if (!SharedVariables.flagStatuses[i].isHeldByAlly)
            {
                if (Communication.wasIdMoved(rc, SharedVariables.flagStatuses[i].id)) continue;
                int dist = Communication.getApproxDistToDams(rc, SharedVariables.flagStatuses[i].location);
                if (dist < minDist)
                {
                    minDist = dist;
                    status = SharedVariables.flagStatuses[i];
                }
                if (dist > maxDist)
                {
                    maxDist = dist;
                }
            }
        }
        if (maxDist == minDist)
        {
            return null;
        }
        return status;
    }

    private static int distToFlags(RobotController rc, MapLocation loc)
    {
        int minDist = 9999;
        for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
        {
            if (!SharedVariables.flagStatuses[i].isHeldByAlly)
            {
                int dist = SharedVariables.flagStatuses[i].location.distanceSquaredTo(loc);
                if (dist < minDist)
                {
                    minDist = dist;
                }
            }
        }
        return minDist;
    }

    //Very bytecode intense, but should only be used during setup
    public static MapLocation getSpotForFlag(RobotController rc) throws GameActionException {
        MapLocation bestLoc = null;
        if (startAtBestLoc != null)
        {
            bestLoc = startAtBestLoc;
        }
        int maxDamDist = 0;
        for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
        {
            if (startAtStatusId != -1 && SharedVariables.flagStatuses[i].id != startAtStatusId)
            {
                continue;
            }
            if (!SharedVariables.flagStatuses[i].isHeldByAlly)
            {
                for (int dx = -6; dx < 7; dx++)
                {
                    if (dx < startAtX)
                    {
                        continue;
                    }
                    for (int dy = -6; dy < 7; dy++)
                    {
                        if (dy < startAtY)
                        {
                            continue;
                        }
                        if (Clock.getBytecodesLeft() < 1000)
                        {
                            startAtX = dx;
                            startAtY = dy;
                            Clock.yield();
                        }
                        if (dx*dx + dy*dy < 36) continue;
                        MapLocation currLoc = new MapLocation(SharedVariables.flagStatuses[i].location.x + dx, SharedVariables.flagStatuses[i].location.y + dy);
                        if (rc.onTheMap(currLoc))
                        {
                            boolean tooClose = false;
                            for (int j = 0; j < SharedVariables.flagStatuses.length; j++)
                            {
                                if (i == j) continue;
                                if (SharedVariables.flagStatuses[j].isHeldByAlly) continue;
                                int flagDist = SharedVariables.flagStatuses[j].location.distanceSquaredTo(currLoc);
                                if (flagDist < 36)
                                {
                                    tooClose = true;
                                    break;
                                }
                            }
                            if (tooClose) continue;
                            int damDist = Communication.getApproxDistToDams(rc, currLoc);
                            if (damDist > maxDamDist)
                            {
                                maxDamDist = damDist;
                                bestLoc = currLoc;
                            }
                        }
                        if (startAtY != -6)
                        {
                            startAtY = -6;
                        }
                    }
                    if (startAtX != -6)
                    {
                        startAtX = -6;
                    }
                }
            }
        }
        startAtBestLoc = null;
        startAtStatusId = -1;
        return bestLoc;
    }

    public static void moveFlags(RobotController rc) throws GameActionException {
        if (rc.hasFlag())
        {
            if (locationForFlag == null || timeUntilRecalculation <= 0) {
                int temp = Clock.getBytecodeNum();
                timeUntilRecalculation = 16;
                locationForFlag = getSpotForFlag(rc);
                rc.setIndicatorString("Moving to place flag near " + locationForFlag);
                System.out.println("Best Loc = " + locationForFlag + " took " + (Clock.getBytecodeNum() - temp) + " bytecode to find");
            }
            timeUntilRecalculation--;
            if (locationForFlag != null && rc.getLocation().isWithinDistanceSquared(locationForFlag, 10))
            {

                //Check if we can place the flag
                boolean canPlace = rc.canDropFlag(rc.getLocation());
                for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
                {
                    if (!SharedVariables.flagStatuses[i].isHeldByAlly)
                    {
                        if (SharedVariables.flagStatuses[i].location.isWithinDistanceSquared(rc.getLocation(), 36))
                        {
                            canPlace = false;
                            break;
                        }
                    }
                }
                if (canPlace)
                {
                    rc.dropFlag(rc.getLocation());
                    Communication.addToFlagsThatMoved(rc, flagIdCarrying);
                    flagIdCarrying = -1;
                }
                else
                {
                    Pathing.lingerTowards(rc, locationForFlag, 4);
                }
            }
            else if (locationForFlag != null)
            {
                Pathing.moveTowards(rc, locationForFlag);
            }
        }
        else if (rc.getRoundNum() < 150)
        {
            FlagStatus flagToMove = getFlagToMove(rc);

            if (flagToMove != null) {
                rc.setIndicatorString("Going to move flag at " + flagToMove.location);
                if (rc.canPickupFlag(flagToMove.location)) {
                    flagIdCarrying = flagToMove.id;
                    rc.pickupFlag(flagToMove.location);
                } else {
                    Pathing.moveTowards(rc, flagToMove.location);
                }
            }
        }
    }

}
