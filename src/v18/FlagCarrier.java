package v18;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

/*
 * If we are outnumbered, avoid enemies
 * Move towards the closest spawn that isn't near a flag in danger
 * Should send a signal that all attackers should approach

 */
public class FlagCarrier {
    private static MapLocation flagCaptureLoc = null;

    private static void setFlagCaptureLoc(RobotController rc)
    {
        MapLocation[] captureLocs = rc.getAllySpawnLocations();
        MapLocation bestCaptureLoc = null;
        int minDist = 9999;
        for (int i = 0; i < captureLocs.length; i++)
        {
            boolean safe = true;
            for (int j = 0; j < SharedVariables.flagStatuses.length; j++)
            {
                if (SharedVariables.flagStatuses[j].inDanger && SharedVariables.flagStatuses[j].location.isWithinDistanceSquared(captureLocs[i], 2))
                {
                    safe = false;
                    break;
                }
            }
            if (safe)
            {
                int dist = rc.getLocation().distanceSquaredTo(captureLocs[i]);
                if (dist < minDist)
                {
                    minDist = dist;
                    bestCaptureLoc = captureLocs[i];
                }
            }
        }
        flagCaptureLoc = bestCaptureLoc;
    }

    private static void checkForImmediateCapture(RobotController rc) throws GameActionException {
        MapLocation[] captureLocs = rc.getAllySpawnLocations();
        for (int i = 1; i < captureLocs.length; i++)
        {
            if (rc.getLocation().isAdjacentTo(captureLocs[i]) && rc.canMove(rc.getLocation().directionTo(captureLocs[i])))
            {
                rc.move(rc.getLocation().directionTo(captureLocs[i]));
                Communication.capturedFlag(rc);
                break;
            }
        }
    }

    public static void runFlagCarrier(RobotController rc) throws GameActionException {

        setFlagCaptureLoc(rc);
        checkForImmediateCapture(rc);
        boolean outnumbered = rc.senseNearbyRobots(-1, rc.getTeam()).length + 1 < rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length;
        if (outnumbered)
        {
            //Run away from enemy robots
            rc.setIndicatorString("Flag Carrier fleeing");
            Pathing.flee(rc);

        }
        else if (flagCaptureLoc != null)
        {
            //Move Towards flagCaptureLoc
            rc.setIndicatorString("Flag Carrier heading towards " + flagCaptureLoc);
            Pathing.moveTowards(rc, flagCaptureLoc);
        }

        //Send Flag Carrier Signal
        Communication.sendFlagCarrierSignal(rc);
    }
}
