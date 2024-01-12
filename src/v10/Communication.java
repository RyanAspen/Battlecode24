package v10;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

class Communication {

    private static final int AREA_RADIUS = 20;
    private static MapLocation[] friendlyFlagLocations = new MapLocation[0];
    private static MapLocation[] enemyFlagLocations = new MapLocation[0];

    private static final int NORMAL_ROLE_IDX = 6;
    private static final int ATTACKER_ROLE_IDX = 7;

    private static final int DEFENDER_ROLE_IDX = 8;

    private static int normalRoleCount = 0;
    private static int attackerRoleCount = 0;
    private static int defenderRoleCount = 0;

    private static final int CLUSTER_POINT_IDX = 9;
    private static final int NUM_CLUSTER_POINTS = 2;

    private static final int MIN_DIST_BETWEEN_CLUSTERS = 50;

    private static final int MAX_DEFENDERS_PER_FLAG = 4;
    private static final int FLAG_DEFENSE_IDX = CLUSTER_POINT_IDX + NUM_CLUSTER_POINTS;
    private static final int DISTRESS_IDX = FLAG_DEFENSE_IDX + 3;

    private static MapLocation[] clusters = new MapLocation[0];



    //Used only at the very beginning, always
    public static void initializeRole(RobotController rc) throws GameActionException {
        int currentAmount = rc.readSharedArray(NORMAL_ROLE_IDX);
        rc.writeSharedArray(NORMAL_ROLE_IDX, currentAmount + 1);
    }

    public static void reportRoleSwitch(RobotController rc, Role prevRole, Role newRole) throws GameActionException {
        int offsetPrev, offsetNew;
        switch (prevRole)
        {
            case Attacker:
                offsetPrev = ATTACKER_ROLE_IDX;
                break;
            case Defender:
                offsetPrev = DEFENDER_ROLE_IDX;
                break;
            default:
                offsetPrev = NORMAL_ROLE_IDX;
                break;
        }
        switch (newRole)
        {
            case Attacker:
                offsetNew = ATTACKER_ROLE_IDX;
                break;
            case Defender:
                offsetNew = DEFENDER_ROLE_IDX;
                break;
            default:
                offsetNew = NORMAL_ROLE_IDX;
                break;
        }
        int currentAmountPrev = rc.readSharedArray(offsetPrev);
        int currentAmountNew = rc.readSharedArray(offsetNew);
        rc.writeSharedArray(offsetPrev, currentAmountPrev - 1);
        rc.writeSharedArray(offsetNew, currentAmountNew + 1);
    }

    public static int getRoleCount(RobotController rc, Role role)
    {
        switch (role)
        {
            case Normal:
                return normalRoleCount;
            case Attacker:
                return attackerRoleCount;
            case Defender:
                return defenderRoleCount;
            default:
                return 0;
        }
    }

    public static MapLocation getDefenderPosition(RobotController rc) throws GameActionException {
        int minDist = 9999;
        MapLocation bestPos = null;
        int bestIdx = -1;
        for (int i = 0; i < 3; i++)
        {
            int amount = rc.readSharedArray(i + FLAG_DEFENSE_IDX);
            if (amount >= MAX_DEFENDERS_PER_FLAG) {}
            else
            {
                MapLocation defenderPos = intToLocation(rc, rc.readSharedArray(i));
                if (defenderPos == null)
                {
                    continue;
                }
                int dist = defenderPos.distanceSquaredTo(rc.getLocation());
                if (dist < minDist)
                {
                    minDist = dist;
                    bestPos = defenderPos;
                    bestIdx = i + FLAG_DEFENSE_IDX;
                }
            }
        }
        if (bestPos != null)
        {
            rc.writeSharedArray(bestIdx, rc.readSharedArray(bestIdx) + 1);
        }
        return bestPos;
    }

    public static void removeDefenderPosition(RobotController rc, MapLocation loc) throws GameActionException {
        for (int i = 0; i < 3; i++)
        {
            MapLocation defenderPos = intToLocation(rc, rc.readSharedArray(i));
            if (defenderPos != null && defenderPos.equals(loc))
            {
                int amount = rc.readSharedArray(i + FLAG_DEFENSE_IDX);
                rc.writeSharedArray(i + FLAG_DEFENSE_IDX, amount - 1);
            }
        }
    }

    public static void sendDistressSignal(RobotController rc, MapLocation loc) throws GameActionException {
        for (int i = 0; i < 3; i++)
        {
            MapLocation defenderPos = intToLocation(rc, rc.readSharedArray(i));
            if (defenderPos != null && defenderPos.equals(loc))
            {
                rc.writeSharedArray(i +DISTRESS_IDX, 1);
                break;
            }
        }
    }

    public static void removeDistressSignal(RobotController rc, MapLocation loc) throws GameActionException {
        for (int i = 0; i < 3; i++)
        {
            MapLocation defenderPos = intToLocation(rc, rc.readSharedArray(i));
            if (defenderPos != null && defenderPos.equals(loc))
            {
                rc.writeSharedArray(i +DISTRESS_IDX, 0);
                break;
            }
        }
    }

    public static MapLocation getDistressLoc(RobotController rc) throws GameActionException {
        for (int i = 0; i < 3; i++)
        {
            boolean distress = rc.readSharedArray(i+DISTRESS_IDX) > 0;
            if (distress)
            {
                return intToLocation(rc, rc.readSharedArray(i));
            }
        }
        return null;
    }

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

    public static boolean createClusterPoint(RobotController rc, MapLocation loc) throws GameActionException {
        int idx = -1;
        for (int i = CLUSTER_POINT_IDX; i < CLUSTER_POINT_IDX + NUM_CLUSTER_POINTS; i++)
        {
            MapLocation prevClusterLoc = intToLocation(rc, rc.readSharedArray(i));
            if (prevClusterLoc == null)
            {
                idx = i;
            }
            else if (prevClusterLoc.isWithinDistanceSquared(loc, MIN_DIST_BETWEEN_CLUSTERS))
            {
                idx = i;
                break;
            }
        }
        if (idx != -1)
        {
            rc.writeSharedArray(idx, locationToInt(rc, loc));
            return true;
        }
        return false;
    }

    public static void removeClusterPoint(RobotController rc, MapLocation loc) throws GameActionException {
        for (int i = CLUSTER_POINT_IDX; i < CLUSTER_POINT_IDX + NUM_CLUSTER_POINTS; i++)
        {
            MapLocation prevClusterLoc = intToLocation(rc, rc.readSharedArray(i));
            if (loc.equals(prevClusterLoc))
            {
                rc.writeSharedArray(i, locationToInt(rc, null));
            }
        }
    }

    private static void updateClusters(RobotController rc) throws GameActionException {
        MapLocation[] clusterPoints = new MapLocation[NUM_CLUSTER_POINTS];
        int j = 0;
        for (int i = CLUSTER_POINT_IDX; i < CLUSTER_POINT_IDX + NUM_CLUSTER_POINTS; i++) {
            MapLocation m = intToLocation(rc, rc.readSharedArray(i));
            if (m != null) {
                clusterPoints[j] = m;
                j++;
            }
        }
        clusters = new MapLocation[j];
        System.arraycopy(clusterPoints, 0, clusters, 0, j);
    }

    private static void updateRoleCounts(RobotController rc) throws GameActionException {
        normalRoleCount = rc.readSharedArray(NORMAL_ROLE_IDX);
        attackerRoleCount = rc.readSharedArray(ATTACKER_ROLE_IDX);
        defenderRoleCount = rc.readSharedArray(DEFENDER_ROLE_IDX);
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

    public static MapLocation[] getClusters()
    {
        return clusters;
    }

    public static void updateComms(RobotController rc) throws GameActionException {
        FlagInfo[] flags = rc.senseNearbyFlags(-1);
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
            for (int i = 0; i < broadcastFlagLocs.length; i++)
            {
                reportFlagBroadcast(rc, broadcastFlagLocs[i]);
            }
        }
        updateFlags(rc);
        updateRoleCounts(rc);
        updateClusters(rc);
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