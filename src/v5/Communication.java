package v5;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

class Message {
    public int idx;
    public int value;
    public int turnAdded;

    Message (int idx, int value, int turnAdded) {
        this.idx = idx;
        this.value = value;
        this.turnAdded = turnAdded;
    }
}

class Communication {

    private static final int AREA_RADIUS = 20;
    private static MapLocation[] friendlyFlagLocations = new MapLocation[0];
    private static MapLocation[] enemyFlagLocations = new MapLocation[0];

    private static void reportFlag(RobotController rc, FlagInfo flag) throws GameActionException {
        int slot = -1;
        int offset = 0;
        if (!flag.getTeam().equals(rc.getTeam()))
        {
            offset = 3;
        }
        int minDist = 9999;
        for (int i = offset; i < 3 + offset; i++) {
            try {
                MapLocation prevFlag = intToLocation(rc, rc.readSharedArray(i));
                if (prevFlag == null) {
                    slot = i;
                    break;
                } else if (prevFlag.distanceSquaredTo(flag.getLocation()) < AREA_RADIUS) {
                    slot = -1;
                    return;
                }
                else
                {
                    int dist = prevFlag.distanceSquaredTo(flag.getLocation());
                    if (dist < minDist)
                    {
                        minDist = dist;
                        slot = i;
                    }
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        if (slot != -1) {
            rc.writeSharedArray(slot, locationToInt(rc, flag.getLocation()));
        }
    }

    private static void reportFlagBroadcast(RobotController rc, MapLocation loc) throws GameActionException {
        int slot = -1;
        int offset = 3;
        int minDist = 9999;
        for (int i = offset; i < 3 + offset; i++) {
            try {
                MapLocation prevFlag = intToLocation(rc, rc.readSharedArray(i));
                if (prevFlag == null) {
                    slot = i;
                    break;
                } else if (prevFlag.distanceSquaredTo(loc) < AREA_RADIUS) {
                    slot = -1;
                    return;
                }
                else
                {
                    int dist = prevFlag.distanceSquaredTo(loc);
                    if (dist < minDist)
                    {
                        minDist = dist;
                        slot = i;
                    }
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        if (slot != -1) {
            rc.writeSharedArray(slot, locationToInt(rc, loc));
        }
    }

    private static void reportFlagGone(RobotController rc, MapLocation loc) throws GameActionException {
        int offset = 3;
        for (int i = offset; i < 3 + offset; i++) {
            try {
                MapLocation prevFlag = intToLocation(rc, rc.readSharedArray(i));
                if (prevFlag != null && prevFlag.distanceSquaredTo(loc) < 10) {
                    rc.writeSharedArray(i, locationToInt(rc, null));
                    return;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
    }

    private static void updateFlags(RobotController rc) {
        MapLocation[] friendlyFlags = new MapLocation[3];
        MapLocation[] enemyFlags = new MapLocation[3];
        int j = 0;
        int k = 0;
        for (int i = 0; i < 6; i++) {
            final int value;
            try {
                value = rc.readSharedArray(i);
                final MapLocation m = intToLocation(rc, value);
                if (m != null) {
                    if (i < 3)
                    {
                        friendlyFlags[j] = m;
                        j++;
                    }
                    else
                    {
                        enemyFlags[k] = m;
                        k++;
                    }

                }
            } catch (GameActionException e) {
                continue;
            }
        }
        friendlyFlagLocations = new MapLocation[j];
        enemyFlagLocations = new MapLocation[k];
        System.arraycopy(friendlyFlags, 0, friendlyFlagLocations, 0, j);
        System.arraycopy(enemyFlags, 0, enemyFlagLocations, 0, k);
    }

    public static MapLocation[] getFriendlyFlagLocations()
    {
        return friendlyFlagLocations;
    }

    public static MapLocation[] getEnemyFlagLocations()
    {
        return enemyFlagLocations;
    }

    public static void updateComms(RobotController rc) throws GameActionException {
        FlagInfo[] flags = rc.senseNearbyFlags(-1);
        if (rc.getID() == 12041)
        {
            System.out.print("Sensed Flags = ");
            for (int i = 0; i < flags.length; i++)
            {
                System.out.print(flags[i].getLocation() + " " + flags[i].getTeam() + " ");
            }
            System.out.println();
        }

        if (flags.length > 0)
        {
            for (int i = 0; i < flags.length; i++)
            {
                reportFlag(rc, flags[i]);
            }
        }
        else
        {
            MapLocation[] broadcastFlagLocs = rc.senseBroadcastFlagLocations();
            if (rc.getID() == 12041)
            {
                System.out.print("Broadcasted Flags = ");
                for (int i = 0; i < broadcastFlagLocs.length; i++)
                {
                    System.out.print(broadcastFlagLocs[i] + " ");
                }
                System.out.println();
            }
            for (int i = 0; i < broadcastFlagLocs.length; i++)
            {
                reportFlagBroadcast(rc, broadcastFlagLocs[i]);
            }
        }
        updateFlags(rc);
        //If there are no flags close but comms says otherwise, remove from comms
        if (flags.length == 0)
        {
            for (int i = 0; i < friendlyFlagLocations.length; i++)
            {
                if (friendlyFlagLocations[i].isWithinDistanceSquared(rc.getLocation(), 13))
                {
                    reportFlagGone(rc, friendlyFlagLocations[i]);
                }
            }
            for (int i = 0; i < enemyFlagLocations.length; i++)
            {
                if (enemyFlagLocations[i].isWithinDistanceSquared(rc.getLocation(), 13))
                {
                    reportFlagGone(rc, enemyFlagLocations[i]);
                }
            }
        }

        if (rc.getID() == 12041)
        {
            System.out.print("Comms Flags = ");
            System.out.print("Friendly Flags = ");
            for (int i = 0; i < friendlyFlagLocations.length; i++)
            {
                System.out.print(friendlyFlagLocations[i] + " ");
            }
            System.out.print("Enemy Flags = ");
            for (int i = 0; i < enemyFlagLocations.length; i++)
            {
                System.out.print(enemyFlagLocations[i] + " ");
            }
            System.out.println();
        }
    }

    private static int locationToInt(RobotController rc, MapLocation m) {
        if (m == null) {
            return 0;
        }
        return 1 + m.x + m.y * rc.getMapWidth();
    }

    private static MapLocation intToLocation(RobotController rc, int m) {
        if (m == 0) {
            return null;
        }
        m--;
        return new MapLocation(m % rc.getMapWidth(), m / rc.getMapWidth());
    }
}