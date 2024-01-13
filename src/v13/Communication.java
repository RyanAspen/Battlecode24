package v13;

import battlecode.common.*;

class Communication {

    private static MapLocation[] friendlyFlagLocations = new MapLocation[0];
    private static MapLocation[] enemyFlagLocations = new MapLocation[0];

    private static final int NORMAL_ROLE_IDX = GameConstants.NUMBER_FLAGS*2;
    private static final int ATTACKER_ROLE_IDX = NORMAL_ROLE_IDX+1;

    private static final int DEFENDER_ROLE_IDX = ATTACKER_ROLE_IDX+1;

    private static int normalRoleCount = 0;
    private static int attackerRoleCount = 0;
    private static int defenderRoleCount = 0;
    private static final int CLUSTER_POINT_IDX = DEFENDER_ROLE_IDX+1;
    private static final int MAX_DEFENDERS_PER_FLAG = 4;
    private static final int FLAG_DEFENSE_IDX = CLUSTER_POINT_IDX + Constants.NUM_CLUSTER_POINTS;
    private static final int DISTRESS_IDX = FLAG_DEFENSE_IDX + 3;
    private static final int MARKED_ENEMIES_IDX = DISTRESS_IDX + GameConstants.NUMBER_FLAGS;
    private static final int ROUND_NUM_IDX = MARKED_ENEMIES_IDX + Constants.MAX_MARKED_ENEMIES;
    private static final int CRUMBS_PATHING_IDX = ROUND_NUM_IDX + 1;
    private static final int CRUMBS_DEFENDING_IDX = CRUMBS_PATHING_IDX + 1;
    private static final int CRUMBS_ATTACKING_IDX = CRUMBS_DEFENDING_IDX + GameConstants.NUMBER_FLAGS;
    private static final int CRUMBS_FREE_IDX = CRUMBS_ATTACKING_IDX + 1;
    private static MapLocation[] clusters = new MapLocation[0];
    private static int[] markedIds = new int[0];



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

    public static void logCrumbsDefense(RobotController rc, int crumbsSpent, MapLocation base) throws GameActionException {
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i));
            if (flagLoc != null && flagLoc.equals(base))
            {
                int crumbsForDefense = Economy.getCrumbsForDefense(rc, base);
                if (crumbsForDefense >= crumbsSpent)
                {
                    rc.writeSharedArray(CRUMBS_DEFENDING_IDX+i, rc.readSharedArray(CRUMBS_DEFENDING_IDX+i) + crumbsSpent);
                }
                else
                {
                    rc.writeSharedArray(CRUMBS_DEFENDING_IDX+i, rc.readSharedArray(CRUMBS_DEFENDING_IDX+i) + crumbsForDefense);
                    rc.writeSharedArray(CRUMBS_FREE_IDX, rc.readSharedArray(CRUMBS_FREE_IDX) + (crumbsSpent - crumbsForDefense));
                }
                break;
            }
        }
    }
    public static void logCrumbsAttack(RobotController rc, int crumbsSpent) throws GameActionException {
        int crumbsForAttack = Economy.getCrumbsForAttack(rc);
        if (crumbsForAttack >= crumbsSpent)
        {
            rc.writeSharedArray(CRUMBS_ATTACKING_IDX, rc.readSharedArray(CRUMBS_ATTACKING_IDX) + crumbsSpent);
        }
        else
        {
            rc.writeSharedArray(CRUMBS_ATTACKING_IDX, rc.readSharedArray(CRUMBS_ATTACKING_IDX) + crumbsForAttack);
            rc.writeSharedArray(CRUMBS_FREE_IDX, rc.readSharedArray(CRUMBS_FREE_IDX) + (crumbsSpent - crumbsForAttack));
        }
    }
    public static void logCrumbsPathing(RobotController rc, int crumbsSpent) throws GameActionException {
        int crumbsForPathing = Economy.getCrumbsForPathing(rc);
        if (crumbsForPathing >= crumbsSpent)
        {
            rc.writeSharedArray(CRUMBS_PATHING_IDX, rc.readSharedArray(CRUMBS_PATHING_IDX) + crumbsSpent);
        }
        else
        {
            rc.writeSharedArray(CRUMBS_PATHING_IDX, rc.readSharedArray(CRUMBS_PATHING_IDX) + crumbsForPathing);
            rc.writeSharedArray(CRUMBS_FREE_IDX, rc.readSharedArray(CRUMBS_FREE_IDX) + (crumbsSpent - crumbsForPathing));
        }
    }

    public static void logCrumbsFree(RobotController rc, int crumbsSpent) throws GameActionException {
        rc.writeSharedArray(CRUMBS_FREE_IDX, rc.readSharedArray(CRUMBS_FREE_IDX) + crumbsSpent);
    }

    public static int getTotalCrumbsSpent(RobotController rc) throws GameActionException {
        int crumbsSpent = getCrumbsAttacking(rc) + getCrumbsPathing(rc) + getCrumbsFree(rc);
        for (int i = CRUMBS_DEFENDING_IDX; i < CRUMBS_DEFENDING_IDX + GameConstants.NUMBER_FLAGS; i++)
        {
            crumbsSpent += rc.readSharedArray(CRUMBS_DEFENDING_IDX+i);
        }
        return crumbsSpent;
    }

    public static int getCrumbsDefense(RobotController rc, MapLocation base) throws GameActionException {
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i));
            if (flagLoc != null && flagLoc.equals(base))
            {
                return rc.readSharedArray(CRUMBS_DEFENDING_IDX+i);
            }
        }
        return 0;
    }
    public static int getCrumbsAttacking(RobotController rc) throws GameActionException {
        return rc.readSharedArray(CRUMBS_ATTACKING_IDX);
    }

    public static int getCrumbsPathing(RobotController rc) throws GameActionException {
        return rc.readSharedArray(CRUMBS_PATHING_IDX);
    }

    public static int getCrumbsFree(RobotController rc) throws GameActionException {
        return rc.readSharedArray(CRUMBS_FREE_IDX);
    }

    public static int getCrumbsEarned(RobotController rc) throws GameActionException {
        return rc.getCrumbs() + getTotalCrumbsSpent(rc);
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
                } else if (prevFlag.distanceSquaredTo(flag.getLocation()) < GameConstants.VISION_RADIUS_SQUARED) {
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
                } else if (prevFlag.distanceSquaredTo(loc) < GameConstants.VISION_RADIUS_SQUARED) {
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
        for (int i = CLUSTER_POINT_IDX; i < CLUSTER_POINT_IDX + Constants.NUM_CLUSTER_POINTS; i++)
        {
            MapLocation prevClusterLoc = intToLocation(rc, rc.readSharedArray(i));
            if (prevClusterLoc == null)
            {
                idx = i;
            }
            else if (prevClusterLoc.isWithinDistanceSquared(loc, Constants.MIN_DIST_BETWEEN_CLUSTERS))
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
        for (int i = CLUSTER_POINT_IDX; i < CLUSTER_POINT_IDX + Constants.NUM_CLUSTER_POINTS; i++)
        {
            MapLocation prevClusterLoc = intToLocation(rc, rc.readSharedArray(i));
            if (loc.equals(prevClusterLoc))
            {
                rc.writeSharedArray(i, locationToInt(rc, null));
            }
        }
    }

    public static void markEnemy(RobotController rc, RobotInfo enemy) throws GameActionException {
        int id = enemy.getID();
        int idx = -1;
        for (int i = MARKED_ENEMIES_IDX; i < MARKED_ENEMIES_IDX+ Constants.MAX_MARKED_ENEMIES; i++)
        {
            int val = rc.readSharedArray(i);
            if (val == 0)
            {
                idx = i;
                continue;
            }
            int savedID = val % (2^14);
            if (val == savedID)
            {
                return;
            }
        }
        if (idx != -1)
        {
            rc.writeSharedArray(idx, 2^14 + id);
        }
    }

    public static void checkMarkedEnemies(RobotController rc) throws GameActionException {

        //Only run once per round
        if (rc.getRoundNum() > rc.readSharedArray(ROUND_NUM_IDX))
        {
            rc.writeSharedArray(ROUND_NUM_IDX, rc.getRoundNum());
            for (int i = MARKED_ENEMIES_IDX; i < MARKED_ENEMIES_IDX + Constants.MAX_MARKED_ENEMIES; i++)
            {
                int val = rc.readSharedArray(i);
                if (val == 0) {}
                else if ((val >> 14) == 0)
                {
                    //Marked bot wasn't seen last round, reset its index
                    rc.writeSharedArray(i, 0);
                }
                else
                {
                    //Reset "seen" bit
                    rc.writeSharedArray(i,val % 2^14);
                }
            }
        }

        //Update "seen" for this bot
        for (int i = MARKED_ENEMIES_IDX; i < MARKED_ENEMIES_IDX+ Constants.MAX_MARKED_ENEMIES; i++)
        {
            int val = rc.readSharedArray(i);
            int id = val % 2^14;
            if (rc.canSenseRobot(id))
            {
                rc.writeSharedArray(i, 2^14 + id);
            }
        }
    }

    private static void updateMarkedEnemies(RobotController rc) throws GameActionException {
        checkMarkedEnemies(rc);
        int[] markIds = new int[Constants.MAX_MARKED_ENEMIES];
        int j = 0;
        for (int i = MARKED_ENEMIES_IDX; i < MARKED_ENEMIES_IDX + Constants.MAX_MARKED_ENEMIES; i++) {
            int id = rc.readSharedArray(i) % 2^14;
            if (id != 0) {
                markIds[j] = id;
                j++;
            }
        }
        markedIds = new int[j];
        System.arraycopy(markIds, 0, markedIds, 0, j);
    }

    private static void updateClusters(RobotController rc) throws GameActionException {
        MapLocation[] clusterPoints = new MapLocation[Constants.NUM_CLUSTER_POINTS];
        int j = 0;
        for (int i = CLUSTER_POINT_IDX; i < CLUSTER_POINT_IDX + Constants.NUM_CLUSTER_POINTS; i++) {
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

    public static int[] getMarkedIds()
    {
        return markedIds;
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
        updateMarkedEnemies(rc);
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