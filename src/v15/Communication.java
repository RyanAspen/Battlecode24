package v15;

import battlecode.common.*;

import java.util.Arrays;

class FlagStatus {
    MapLocation location;
    boolean inDanger;
    boolean isLost;
    boolean isInitialized;
    int id;
    int numberOfDefenders = 0;
    int idx;


    FlagStatus(){}

}

class Communication {


    private static int attackerRoleCount = 0;
    private static int defenderRoleCount = 0;
    private static MapLocation[] clusters = new MapLocation[0];
    private static int[] markedIds = new int[0];

    //Used only at the very beginning, always
    public static void initializeRole(RobotController rc, Role role) throws GameActionException {
        switch (role)
        {
            case Attacker:
                rc.writeSharedArray(Constants.ATTACKER_ROLE_IDX, rc.readSharedArray(Constants.ATTACKER_ROLE_IDX) + 1);
                break;
            case Defender:
                rc.writeSharedArray(Constants.DEFENDER_ROLE_IDX, rc.readSharedArray(Constants.DEFENDER_ROLE_IDX) + 1);
                break;
            default:
                rc.writeSharedArray(Constants.ATTACKER_ROLE_IDX, rc.readSharedArray(Constants.ATTACKER_ROLE_IDX) + 1);
                break;
        }
    }

    public static void reportRoleSwitch(RobotController rc, Role prevRole, Role newRole) throws GameActionException {
        int offsetPrev, offsetNew;
        switch (prevRole)
        {
            case Attacker:
                offsetPrev = Constants.ATTACKER_ROLE_IDX;
                break;
            case Defender:
                offsetPrev = Constants.DEFENDER_ROLE_IDX;
                break;
            default:
                offsetPrev = Constants.ATTACKER_ROLE_IDX;
                break;
        }
        switch (newRole)
        {
            case Attacker:
                offsetNew = Constants.ATTACKER_ROLE_IDX;
                break;
            case Defender:
                offsetNew = Constants.DEFENDER_ROLE_IDX;
                break;
            default:
                offsetNew = Constants.ATTACKER_ROLE_IDX;
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
            case Attacker:
                return attackerRoleCount;
            case Defender:
                return defenderRoleCount;
            default:
                return 0;
        }
    }

    /*
    - For now, just defend the flag with the least defenders, unless the current defense is not null or lost and has no more than 1 defender more than the minimum
     */
    //TODO:: This method is bugged
    public static int getDefenderFlagId(RobotController rc, int currentFlagId) throws GameActionException {
        FlagStatus[] flagStatuses = Communication.getFriendlyFlagStatuses(rc);
        int minDefenderCount = 9999;
        int nextIdx = -1;
        int currentIdx = -1;
        int newId = -1;

        //Hold info about current defense location
        boolean currentIsLost = false;
        int currentNumberOfDefenders = 9999;
        MapLocation currentLoc = null;

        //Get the flag with the least defenders that isn't lost
        for (int i = 0; i < flagStatuses.length; i++)
        {
            if (!flagStatuses[i].isLost)
            {
                int currentDefenders = flagStatuses[i].numberOfDefenders;
                if (currentDefenders < minDefenderCount)
                {
                    minDefenderCount = currentDefenders;
                    nextIdx = flagStatuses[i].idx;
                    newId = flagStatuses[i].id;
                }
            }
            if (flagStatuses[i].id == currentFlagId)
            {
                //Store info on current defense
                currentIdx = flagStatuses[i].idx;
                currentIsLost = flagStatuses[i].isLost;
                currentNumberOfDefenders = flagStatuses[i].numberOfDefenders;
                currentLoc = flagStatuses[i].location;
            }
        }

        //If current defense is not lost AND currentLoc != null AND currentNumberOfDefenders < minDefenderCount + 1, just return the existing id
        if (!currentIsLost && currentLoc != null && currentNumberOfDefenders <= minDefenderCount + 1)
        {
            return currentFlagId;
        }

        //If no new location was found, just return the existing id
        if (nextIdx == -1)
        {
            return currentFlagId;
        }

        //Otherwise, add to new defense id
        int currentAmount = rc.readSharedArray(nextIdx + Constants.FLAG_DEFENSE_IDX);
        rc.writeSharedArray(nextIdx + Constants.FLAG_DEFENSE_IDX,currentAmount  + 1);

        //If the existing id exists, remove from it
        if (currentIdx != -1) {
            int existingAmount = rc.readSharedArray(currentIdx + Constants.FLAG_DEFENSE_IDX);
            rc.writeSharedArray(currentIdx + Constants.FLAG_DEFENSE_IDX,  existingAmount - 1);
        }
        return newId;
    }

    public static void removeDefenderPosition(RobotController rc, int flagId) throws GameActionException {
        if (flagId == -1)
            return;
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            int id = rc.readSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + i);
            if (id != -1 && flagId == id)
            {
                int amount = rc.readSharedArray(i + Constants.FLAG_DEFENSE_IDX);
                rc.writeSharedArray(i + Constants.FLAG_DEFENSE_IDX, amount - 1);
            }
        }
    }

    public static void sendDistressSignal(RobotController rc, MapLocation loc) throws GameActionException {
        for (int i = 0; i < 3; i++)
        {
            MapLocation defenderPos = intToLocation(rc, rc.readSharedArray(i));
            if (defenderPos != null && defenderPos.equals(loc))
            {
                rc.writeSharedArray(i + Constants.DISTRESS_IDX, 1);
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
                rc.writeSharedArray(i + Constants.DISTRESS_IDX, 0);
                break;
            }
        }
    }

    public static MapLocation getDistressLoc(RobotController rc) throws GameActionException {
        for (int i = 0; i < 3; i++)
        {
            boolean distress = rc.readSharedArray(i+Constants.DISTRESS_IDX) > 0;
            if (distress)
            {
                return intToLocation(rc, rc.readSharedArray(i));
            }
        }
        return null;
    }

    public static void initializeFlagCounts(RobotController rc) throws GameActionException {
        rc.writeSharedArray(Constants.FRIENDLY_FLAG_COUNT_IDX, GameConstants.NUMBER_FLAGS);
        rc.writeSharedArray(Constants.ENEMY_FLAG_COUNT_IDX, GameConstants.NUMBER_FLAGS);
    }

    public static void capturedFlag(RobotController rc) throws GameActionException {
        rc.writeSharedArray(Constants.ENEMY_FLAG_COUNT_IDX, rc.readSharedArray(Constants.ENEMY_FLAG_COUNT_IDX) - 1);
    }

    public static int getFriendlyFlagsCount(RobotController rc) throws GameActionException {
        FlagStatus[] flagStatuses = getFriendlyFlagStatuses(rc);
        int flagCount = 0;
        for (int i = 0; i < flagStatuses.length; i++)
        {
            if (!flagStatuses[i].isLost)
            {
                flagCount++;
            }
        }
        return flagCount;
    }
    public static int getEnemyFlagsCount(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.ENEMY_FLAG_COUNT_IDX);
    }

    /*
        A flag is marked IF
        - it's location isn't null
        AND
        - the new location isn't the same as the existing location
        AND
        (the new location is near the existing location OR roundNum >= roundToStop OR the existing location is not near a real flag)
     */
    public static void focusFlag(RobotController rc, MapLocation flagLoc, MapLocation preferedSpawn) throws GameActionException {
        boolean markFlag = false;
        MapLocation recentFlag = getAttackerFlag(rc);
        if (flagLoc != null && (recentFlag == null || !recentFlag.equals(flagLoc)))
        {
            int roundToStop = rc.readSharedArray(Constants.PREFERED_FLAG_ROUND_END_IDX) + 2000; //TODO
            if (rc.getRoundNum() >= roundToStop || recentFlag == null || flagLoc.isWithinDistanceSquared(recentFlag, GameConstants.VISION_RADIUS_SQUARED))
            {
                markFlag = true;
            }
            else
            {
                //If the existing location is not near a real flag
                if (rc.getLocation().isWithinDistanceSquared(recentFlag, 4))
                {
                    //We can check if we can see a flag. If not, mark the flag.
                    FlagInfo[] nearEnemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                    markFlag = nearEnemyFlags.length == 0;
                }
            }
        }

        if (markFlag)
        {
            rc.writeSharedArray(Constants.PREFERED_FLAG_ROUND_END_IDX, rc.getRoundNum());
            rc.writeSharedArray(Constants.PREFERED_FLAG_IDX, locationToInt(rc, flagLoc));
            rc.writeSharedArray(Constants.PREFERED_SPAWN_IDX, locationToInt(rc, preferedSpawn));
        }
    }

    public static MapLocation getAttackerFlag(RobotController rc) throws GameActionException {
        return intToLocation(rc, rc.readSharedArray(Constants.PREFERED_FLAG_IDX));
    }

    public static MapLocation getPreferedSpawnAttackers(RobotController rc) throws GameActionException {
        return intToLocation(rc, rc.readSharedArray(Constants.PREFERED_SPAWN_IDX));
    }

    public static void logCrumbsDefense(RobotController rc, int crumbsSpent, MapLocation base) throws GameActionException {
        int crumbsForDefense = Economy.getCrumbsForDefense(rc, base);
        int crumbsForDefenseSpent = getCrumbsSpentDefense(rc, base);
        int crumbsForFreeSpent = getCrumbsSpentFree(rc);
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i));
            if (flagLoc != null && flagLoc.equals(base))
            {
                if (crumbsForDefense >= crumbsSpent)
                {
                    rc.writeSharedArray(Constants.CRUMBS_DEFENDING_IDX+i, crumbsForDefenseSpent + crumbsSpent);
                }
                else
                {
                    int amountForDefenseSpent = crumbsSpent - crumbsForDefense;
                    rc.writeSharedArray(Constants.CRUMBS_DEFENDING_IDX+i, crumbsForDefenseSpent + (crumbsSpent - amountForDefenseSpent));
                    rc.writeSharedArray(Constants.CRUMBS_FREE_IDX, crumbsForFreeSpent + amountForDefenseSpent);
                    //crumbsForDefenseSpent + amountForDefenseSpent
                }
            }
        }
    }
    public static void logCrumbsAttack(RobotController rc, int crumbsSpent) throws GameActionException {
        int crumbsForAttack = Economy.getCrumbsForAttack(rc);
        int crumbsForFree = Economy.getCrumbsForFree(rc);
        if (crumbsForAttack >= crumbsSpent)
        {
            rc.writeSharedArray(Constants.CRUMBS_ATTACKING_IDX, crumbsForAttack - crumbsSpent);
        }
        else
        {
            rc.writeSharedArray(Constants.CRUMBS_ATTACKING_IDX, 0);
            rc.writeSharedArray(Constants.CRUMBS_FREE_IDX, crumbsForFree - (crumbsSpent - crumbsForAttack));
        }
    }
    public static void logCrumbsPathing(RobotController rc, int crumbsSpent) throws GameActionException {
        int crumbsForPathing = Economy.getCrumbsForPathing(rc);
        int crumbsForFree = Economy.getCrumbsForFree(rc);
        if (crumbsForPathing >= crumbsSpent)
        {
            rc.writeSharedArray(Constants.CRUMBS_PATHING_IDX, crumbsForPathing - crumbsSpent);
        }
        else
        {
            rc.writeSharedArray(Constants.CRUMBS_PATHING_IDX, 0);
            rc.writeSharedArray(Constants.CRUMBS_FREE_IDX, crumbsForFree - (crumbsSpent - crumbsForPathing));
        }
    }

    public static int getCrumbsSpentDefense(RobotController rc, MapLocation base) throws GameActionException {
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i));
            if (flagLoc != null && flagLoc.equals(base))
            {
                return rc.readSharedArray(Constants.CRUMBS_DEFENDING_IDX+i);
            }
        }
        return 0;
    }
    public static int getCrumbsSpentAllDefense(RobotController rc) throws GameActionException {
        int totalSpent = 0;
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            totalSpent += rc.readSharedArray(Constants.CRUMBS_DEFENDING_IDX+i);
        }
        return totalSpent;
    }
    public static int getCrumbsSpentAttacking(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.CRUMBS_ATTACKING_IDX);
    }

    public static int getCrumbsSpentPathing(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.CRUMBS_PATHING_IDX);
    }

    public static int getCrumbsSpentFree(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.CRUMBS_FREE_IDX);
    }

    public static int getCurrentDefenders(RobotController rc, int flagId) throws GameActionException {
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            int id = rc.readSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + i);
            if (id == flagId)
            {
                return rc.readSharedArray(Constants.FLAG_DEFENSE_IDX + i);
            }
        }
        return 9999;
    }

    public static MapLocation getFriendlyFlagLocationFromId(RobotController rc, int flagId) throws GameActionException {
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            int id = rc.readSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + i);
            if (id == flagId)
            {
                return intToLocation(rc, rc.readSharedArray(Constants.FRIENDLY_FLAG_LOC_IDX + i));
            }
        }
        return null;
    }

    /*
        Friendly flag information is stored as follows:
        - Friendly flag ids are stored in the last 14 bits of FRIENDLY_FLAG_IDS_IDX
        - Whether a particular flag is in danger is stored in the 2nd bit of FRIENDLY_FLAG_IDS_IDX
        - The last round in which a particular flag was seen is stored in FRIENDLY_FLAGS_LAST_SEEN_IDX
        - The last seen location in which a particular flag was seen is stored in FRIENDLY_FLAG_LOC_IDX
        - The number of defenders assigned to defend a particular flag is stored in FLAG_DEFENSE_IDX
     */
    public static FlagStatus[] getFriendlyFlagStatuses(RobotController rc) throws GameActionException {
        FlagStatus[] friendlyFlagStatuses = new FlagStatus[GameConstants.NUMBER_FLAGS];
        int numStatuses = 0;
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            //Check if the id at i=0 exists
            int existingVal = rc.readSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + i);
            int existingId = existingVal & ((int) Math.pow(2, 14) - 1);
            boolean existingInDanger = (existingVal >> 14) > 0;

            if (existingVal != 0)
            {
                //Get location information
                MapLocation existingLoc = intToLocation(rc, rc.readSharedArray(Constants.FRIENDLY_FLAG_LOC_IDX + i));

                //Get Last Seen information
                int lostTime = rc.getRoundNum()-rc.readSharedArray(Constants.FRIENDLY_FLAGS_LAST_SEEN_IDX + i);
                boolean isLost = lostTime >= 3 && rc.getRoundNum() > GameConstants.SETUP_ROUNDS;

                //Get Number of Defenders Information
                int numDefenders = rc.readSharedArray(Constants.FLAG_DEFENSE_IDX + i);

                //Add a new flag status
                FlagStatus status = new FlagStatus();
                status.id = existingId;
                status.inDanger = existingInDanger;
                status.isLost = isLost;
                status.location = existingLoc;
                status.numberOfDefenders = numDefenders;
                status.idx = i;
                friendlyFlagStatuses[numStatuses] = status;
                numStatuses++;
            }
        }
        FlagStatus[] finalStatuses = new FlagStatus[numStatuses];
        System.arraycopy(friendlyFlagStatuses, 0, finalStatuses, 0, numStatuses);
        return finalStatuses;
    }

    public static void printFriendlyFlagStatuses(RobotController rc) throws GameActionException {
        FlagStatus statuses[] = getFriendlyFlagStatuses(rc);
        for (int i = 0; i < statuses.length; i++)
        {
            System.out.println("Status of Flag ID = " + statuses[i].id);
            System.out.println("Is Lost = " + statuses[i].isLost);
            if (statuses[i].location == null)
            {
                System.out.println("Location = Unknown");
            }
            else
            {
                System.out.println("Location = " + statuses[i].location);
            }
        }
    }

    public static void printFlagDefenses(RobotController rc) throws GameActionException {
        FlagStatus[] flagStatuses = getFriendlyFlagStatuses(rc);
        int totalBots = 0;
        for (int i = 0; i < flagStatuses.length; i++)
        {
            if (!flagStatuses[i].isLost) {
                totalBots += flagStatuses[i].numberOfDefenders;
                System.out.println("There are " + flagStatuses[i].numberOfDefenders + " bots defending ID = " + flagStatuses[i].id + " at " + flagStatuses[i].location);
            }
        }
        System.out.println("Total Bots Defending = " + totalBots);
    }

    /*
        Friendly flag information is stored as follows:
        - Friendly flag ids are stored in the last 14 bits of FRIENDLY_FLAG_IDS_IDX
        - Whether a particular flag is in danger is stored in the 2nd bit of FRIENDLY_FLAG_IDS_IDX
        - The last round in which a particular flag was seen is stored in FRIENDLY_FLAGS_LAST_SEEN_IDX
        - The last seen location in which a particular flag was seen is stored in FRIENDLY_FLAG_LOC_IDX
        - The number of defenders assigned to defend a particular flag is stored in FLAG_DEFENSE_IDX
     */
    public static void reportFriendlyFlag(RobotController rc, FlagInfo flag) throws GameActionException
    {
        int id = flag.getID();
        MapLocation flagLoc = flag.getLocation();
        int idx = -1;
        boolean inDanger = flag.isPickedUp();

        //Check if there are more enemies near the flag than allies
        if (!inDanger)
        {
            inDanger = rc.senseNearbyRobots(flagLoc, -1, rc.getTeam()).length < rc.senseNearbyRobots(flagLoc, -1, rc.getTeam().opponent()).length;
        }

        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            int existingVal = rc.readSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + i);
            int existingId = existingVal & ((int)Math.pow(2,14) - 1);
            if (existingId == 0)
            {
                idx = i;
            }
            else if (existingId == id)
            {
                idx = i;
                break;
            }
        }
        if (idx != -1)
        {
            if (rc.readSharedArray(idx + Constants.FRIENDLY_FLAGS_LAST_SEEN_IDX) < rc.getRoundNum()) {
                rc.writeSharedArray(idx + Constants.FRIENDLY_FLAGS_LAST_SEEN_IDX, rc.getRoundNum());
            }

            rc.writeSharedArray(Constants.FRIENDLY_FLAG_LOC_IDX + idx, locationToInt(rc, flagLoc));
            if (inDanger)
            {
                rc.writeSharedArray(idx + Constants.FRIENDLY_FLAG_IDS_IDX, id + (int)Math.pow(2,14));
            }
            else
            {
                rc.writeSharedArray(idx + Constants.FRIENDLY_FLAG_IDS_IDX, id);
            }
        }
    }

    private static void reportEnemyFlag(RobotController rc, FlagInfo flag) throws GameActionException {
        int slot = -1;
        int minDist = 9999;
        for (int i = Constants.ENEMY_FLAG_LOC_IDX; i < Constants.ENEMY_FLAG_LOC_IDX + GameConstants.NUMBER_FLAGS; i++) {
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

    public static int getDesiredDefenders(RobotController rc) throws GameActionException {
        int strategy = getStrategy(rc);
        switch (strategy)
        {
            case Constants.BALANCED_STRATEGY:
                return Constants.MIN_DEFENDERS_BALANCED;
            case Constants.TURTLE_STRATEGY:
                return Constants.MIN_DEFENDERS_TURTLE;
            case Constants.AGGRESSIVE_STRATEGY:
                return Constants.MIN_DEFENDERS_AGGRESSIVE;
            default:
                return Constants.MIN_DEFENDERS_BALANCED;
        }
    }
    public static int getDesiredAttackers(RobotController rc) throws GameActionException {
        int strategy = getStrategy(rc);
        switch (strategy)
        {
            case Constants.BALANCED_STRATEGY:
                return Constants.MIN_ATTACKERS_BALANCED;
            case Constants.TURTLE_STRATEGY:
                return Constants.MIN_ATTACKERS_TURTLE;
            case Constants.AGGRESSIVE_STRATEGY:
                return Constants.MIN_ATTACKERS_AGGRESSIVE;
            default:
                return Constants.MIN_ATTACKERS_BALANCED;
        }
    }

    public static boolean createClusterPoint(RobotController rc, MapLocation loc) throws GameActionException {
        int idx = -1;
        for (int i = Constants.CLUSTER_POINT_IDX; i < Constants.CLUSTER_POINT_IDX + Constants.NUM_CLUSTER_POINTS; i++)
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
        for (int i = Constants.CLUSTER_POINT_IDX; i < Constants.CLUSTER_POINT_IDX + Constants.NUM_CLUSTER_POINTS; i++)
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
        for (int i = Constants.MARKED_ENEMIES_IDX; i < Constants.MARKED_ENEMIES_IDX+ Constants.MAX_MARKED_ENEMIES; i++)
        {
            int val = rc.readSharedArray(i);
            if (val == 0)
            {
                idx = i;
                continue;
            }
            int savedID = val & ((int)Math.pow(2,14) - 1);
            if (val == savedID)
            {
                return;
            }
        }
        if (idx != -1)
        {
            rc.writeSharedArray(idx, (int)Math.pow(2,14) + id);
        }
    }

    public static void checkMarkedEnemies(RobotController rc) throws GameActionException {

        //Only run once per round
        if (rc.getRoundNum() > rc.readSharedArray(Constants.ROUND_NUM_IDX))
        {
            rc.writeSharedArray(Constants.ROUND_NUM_IDX, rc.getRoundNum());
            for (int i = Constants.MARKED_ENEMIES_IDX; i < Constants.MARKED_ENEMIES_IDX + Constants.MAX_MARKED_ENEMIES; i++)
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
                    rc.writeSharedArray(i,val & ((int)Math.pow(2,14) - 1));
                }
            }
        }

        //Update "seen" for this bot
        for (int i = Constants.MARKED_ENEMIES_IDX; i < Constants.MARKED_ENEMIES_IDX+ Constants.MAX_MARKED_ENEMIES; i++)
        {
            int val = rc.readSharedArray(i);
            int id = val & ((int)Math.pow(2,14) - 1);
            if (rc.canSenseRobot(id))
            {
                rc.writeSharedArray(i, (int)Math.pow(2,14) + id);
            }
        }
    }

    private static void updateMarkedEnemies(RobotController rc) throws GameActionException {
        checkMarkedEnemies(rc);
        int[] markIds = new int[Constants.MAX_MARKED_ENEMIES];
        int j = 0;
        for (int i = Constants.MARKED_ENEMIES_IDX; i < Constants.MARKED_ENEMIES_IDX + Constants.MAX_MARKED_ENEMIES; i++) {
            int id = rc.readSharedArray(i) % (int)Math.pow(2,14);
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
        for (int i = Constants.CLUSTER_POINT_IDX; i < Constants.CLUSTER_POINT_IDX + Constants.NUM_CLUSTER_POINTS; i++) {
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
        attackerRoleCount = rc.readSharedArray(Constants.ATTACKER_ROLE_IDX);
        defenderRoleCount = rc.readSharedArray(Constants.DEFENDER_ROLE_IDX);
    }
    public static MapLocation[] getEnemyFlagLocations(RobotController rc) throws GameActionException {
        MapLocation[] enemyFlags = new MapLocation[3];
        int j = 0;
        for (int i = Constants.ENEMY_FLAG_LOC_IDX; i < Constants.ENEMY_FLAG_LOC_IDX+GameConstants.NUMBER_FLAGS; i++) {
            MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i));
            if (flagLoc != null)
            {
                enemyFlags[j] = flagLoc;
                j++;
            }
        }
        MapLocation[] enemyFlagLocations = new MapLocation[j];
        System.arraycopy(enemyFlags, 0, enemyFlagLocations, 0, j);
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
                if (flags[i].getTeam().isPlayer())
                {
                    reportFriendlyFlag(rc, flags[i]);
                }
                else
                {
                    reportEnemyFlag(rc, flags[i]);
                }
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
        if (rc.getRoundNum() < 5)
        {
            initializeFlagCounts(rc);
        }
        updateRoleCounts(rc);
        updateClusters(rc);
        updateMarkedEnemies(rc);
        //If there are no flags close but comms says otherwise, remove from comms
        if (flags.length == 0)
        {
            for (int i = Constants.ENEMY_FLAG_LOC_IDX; i < Constants.ENEMY_FLAG_LOC_IDX + GameConstants.NUMBER_FLAGS; i++)
            {
                MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i));
                if (flagLoc != null && flagLoc.isWithinDistanceSquared(rc.getLocation(), 13))
                {
                    reportFlagGone(rc, flagLoc);
                }
            }
        }
    }

    public static int getStrategy(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.STRATEGY_IDX);
    }

    public static void setStrategy(RobotController rc, int strategyType) throws GameActionException {
        rc.writeSharedArray(Constants.STRATEGY_IDX, strategyType);
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