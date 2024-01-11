package v9;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

//Goal is to get Bread crumbs and nothing else
public class Economy {

    public static void econ(RobotController rc) throws GameActionException {
        MapLocation[] crumbLocations = rc.senseNearbyCrumbs(20);
        if (crumbLocations.length > 0)
        {
            MapLocation target = crumbLocations[rc.getID() % crumbLocations.length];
            Pathing.moveTowardsFilling(rc, target);
        }
    }
}
