package v22;

import battlecode.common.*;

import java.util.Objects;

class FlagStatus {
    MapLocation location;
    MapLocation spawn;
    boolean inDanger;
    boolean isHeldByAlly;
    boolean isLost;
    int id;
    int numberOfLiveDefenders = 0;
    int idx = -1;


    FlagStatus(){}

}

class Communication {

    // ROLES
    public static void initializeRole(RobotController rc, Role role) throws GameActionException {
        if (Objects.requireNonNull(role) == Role.Defender) {
            rc.writeSharedArray(Constants.DEFENDER_ROLE_IDX, rc.readSharedArray(Constants.DEFENDER_ROLE_IDX) + 1);
        } else {
            rc.writeSharedArray(Constants.ATTACKER_ROLE_IDX, rc.readSharedArray(Constants.ATTACKER_ROLE_IDX) + 1);
        }
    }

    public static void reportRoleSwitch(RobotController rc, Role prevRole, Role newRole) throws GameActionException {
        int offsetPrev, offsetNew;
        if (Objects.requireNonNull(prevRole) == Role.Defender) {
            offsetPrev = Constants.DEFENDER_ROLE_IDX;
        } else {
            offsetPrev = Constants.ATTACKER_ROLE_IDX;
        }
        if (Objects.requireNonNull(newRole) == Role.Defender) {
            offsetNew = Constants.DEFENDER_ROLE_IDX;
        } else {
            offsetNew = Constants.ATTACKER_ROLE_IDX;
        }
        int currentAmountPrev = rc.readSharedArray(offsetPrev);
        int currentAmountNew = rc.readSharedArray(offsetNew);
        rc.writeSharedArray(offsetPrev, currentAmountPrev - 1);
        rc.writeSharedArray(offsetNew, currentAmountNew + 1);
    }

    public static int getRoleCount(RobotController rc, Role role) throws GameActionException {
        switch (role)
        {
            case Attacker:
                return rc.readSharedArray(Constants.ATTACKER_ROLE_IDX);
            case Defender:
                return rc.readSharedArray(Constants.DEFENDER_ROLE_IDX);
            default:
                return 0;
        }
    }

    public static int getDesiredDefenders(RobotController rc) throws GameActionException {
        int strategy = getStrategy(rc);
        switch (strategy)
        {
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
            case Constants.TURTLE_STRATEGY:
                return Constants.MIN_ATTACKERS_TURTLE;
            case Constants.AGGRESSIVE_STRATEGY:
                return Constants.MIN_ATTACKERS_AGGRESSIVE;
            default:
                return Constants.MIN_ATTACKERS_BALANCED;
        }
    }

    //END ROLES

    //DEFENDERS

    /*
    - For now, just defend the flag with the least defenders, unless the current defense is not null or lost and has no more than 1 defender more than the minimum
     */
    public static int getDefenderFlagId(RobotController rc, int currentFlagId) throws GameActionException {
        FlagStatus[] flagStatuses = SharedVariables.flagStatuses;
        int minDefenderCount = 9999;
        int newIdx = -1;
        int currentIdx = -1;
        int newId = -1;

        //Hold info about current defense location
        boolean currentIsLost = false;
        int currentNumberOfDefenders = 9999;
        MapLocation currentLoc = null;

        //Hold info about a lost flag
        int lostNumberOfDefenders = 0;
        MapLocation lostLoc = null;
        int lostIdx = -1;

        //Get the flag with the least defenders that isn't lost

        for (int i = 0; i < flagStatuses.length; i++)
        {
            int defenders = flagStatuses[i].numberOfLiveDefenders;
            if (!flagStatuses[i].isLost)
            {

                if (defenders < minDefenderCount) {
                    minDefenderCount = defenders;
                    newIdx = flagStatuses[i].idx;
                    newId = flagStatuses[i].id;
                }
            }
            else
            {
                lostLoc = flagStatuses[i].spawn;
                lostNumberOfDefenders = flagStatuses[i].numberOfLiveDefenders;
                lostIdx = flagStatuses[i].idx;
            }
            if (flagStatuses[i].id == currentFlagId)
            {
                //Store info on current defense
                currentIdx = flagStatuses[i].idx;
                currentFlagId = flagStatuses[i].id;
                currentIsLost = flagStatuses[i].isLost;
                currentLoc = getFriendlyFlagLocationFromId(rc, flagStatuses[i].id);
                currentNumberOfDefenders = flagStatuses[i].numberOfLiveDefenders;
            }
        }

        //Don't swap IF we have a flag already that isn't lost, AND we have fewer than the minimum number of defenders at the current flag OR we have no more than 1 defender more than the minimum
        if ((newId == -1) || (currentLoc != null && !currentIsLost && currentFlagId != -1 && (currentNumberOfDefenders <= Constants.MIN_DEFENDERS_PER_FLAG || currentNumberOfDefenders <= minDefenderCount + 1)))
        {
            return currentFlagId;
        }
        if(newIdx == currentIdx)
        {
            return currentFlagId;
        }

        /*
            If our currentFlag has at least 1 more than the
            minimum number of defenders and a lost flag has no defenders,
            swap to the lost flag to check its spawn.
         */
        if (currentIsLost && currentLoc != null && !rc.canSenseLocation(currentLoc))
        {
            return currentFlagId;
        }
        if (!currentIsLost && currentNumberOfDefenders > Constants.MIN_DEFENDERS_PER_FLAG && lostLoc != null && lostNumberOfDefenders == 0)
        {
            newIdx = lostIdx;
        }

        //Add to new spot
        rc.writeSharedArray(newIdx + Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX, rc.readSharedArray(newIdx + Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX) + 1);
        rc.writeSharedArray(newIdx + Constants.FRIENDLY_FLAG_ADD_IDX, rc.readSharedArray(newIdx + Constants.FRIENDLY_FLAG_ADD_IDX) + 1);
        if (currentFlagId != -1)
        {
            rc.writeSharedArray(currentIdx + Constants.FRIENDLY_FLAG_REMOVE_IDX, rc.readSharedArray(currentIdx + Constants.FRIENDLY_FLAG_REMOVE_IDX) + 1);
            int currentTempDefenders = rc.readSharedArray(currentIdx + Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX);
            rc.writeSharedArray(currentIdx + Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX, currentTempDefenders - 1);
        }
        return newId;
    }

    public static void removeDefenderPosition(RobotController rc, int flagId) throws GameActionException {
        FlagStatus currentStatus = getFlagStatus(flagId);
        if (currentStatus == null) return;
        rc.writeSharedArray(currentStatus.idx + Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX, rc.readSharedArray(currentStatus.idx + Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX) - 1);
        rc.writeSharedArray(currentStatus.idx + Constants.FRIENDLY_FLAG_REMOVE_IDX, rc.readSharedArray(currentStatus.idx + Constants.FRIENDLY_FLAG_REMOVE_IDX) + 1);
        for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
        {
            if (SharedVariables.flagStatuses[i].id == flagId)
            {
                SharedVariables.flagStatuses[i].numberOfLiveDefenders--;
                break;
            }
        }
    }

    //END DEFENDERS

    //FLAG MOVEMENT

    public static void addToFlagsThatMoved(RobotController rc, int id) throws GameActionException {
        int idx = -1;
        for (int i = Constants.FLAGS_MOVED_IDX; i < Constants.FLAGS_MOVED_IDX + GameConstants.NUMBER_FLAGS; i++)
        {
            if (rc.readSharedArray(i) == 0)
            {
                idx = i;
            }
            else if (rc.readSharedArray(i) == id)
            {
                return;
            }
        }
        if (idx != -1)
        {
            rc.writeSharedArray(idx, id);
        }
    }

    public static boolean wasIdMoved(RobotController rc, int id) throws GameActionException {
        for (int i = Constants.FLAGS_MOVED_IDX; i < Constants.FLAGS_MOVED_IDX + GameConstants.NUMBER_FLAGS; i++)
        {
            if (rc.readSharedArray(i) == id)
            {
                return true;
            }
        }
        return false;
    }

    public static boolean areFlagsCloseTogether(RobotController rc) throws GameActionException {
        int maxDist = 0;
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            MapLocation flag1Loc = intToLocation(rc, rc.readSharedArray(Constants.FRIENDLY_FLAG_LOC_IDX + i));
            if (flag1Loc == null)
            {
                continue;
            }

            for (int j = 1; j < GameConstants.NUMBER_FLAGS; j++)
            {
                MapLocation flag2Loc = intToLocation(rc, rc.readSharedArray(Constants.FRIENDLY_FLAG_LOC_IDX + j));
                if (flag2Loc == null)
                {
                    continue;
                }

                int dist = flag1Loc.distanceSquaredTo(flag2Loc);
                if (dist > maxDist)
                {
                    maxDist = dist;
                }
            }
        }
        return maxDist < 120;
    }

    private static void reportDam(RobotController rc, MapLocation damLoc) throws GameActionException {
        int idx = -1;
        for (int i = Constants.DAM_LOCATIONS_IDX; i < Constants.DAM_LOCATIONS_IDX + Constants.NUM_DAM_LOCS; i++)
        {
            MapLocation currentDamLoc = intToLocation(rc, rc.readSharedArray(i));
            if (currentDamLoc != null && damLoc.isWithinDistanceSquared(currentDamLoc, Constants.MIN_DIST_BETWEEN_DAM_LOCS))
            {
                return;
            }
            else if (currentDamLoc == null)
            {
                idx = i;
            }
        }
        if (idx != -1)
        {
            rc.writeSharedArray(idx, locationToInt(rc, damLoc));
        }
    }

    public static void updateDamLocs(RobotController rc) throws GameActionException {
        MapInfo[] nearMapInfos = rc.senseNearbyMapInfos();
        for (int i = 0; i < nearMapInfos.length; i++)
        {
            if (nearMapInfos[i].isDam())
            {
                reportDam(rc, nearMapInfos[i].getMapLocation());
            }
        }
    }

    public static int getApproxDistToDams(RobotController rc, MapLocation loc) throws GameActionException {
        int closestDist = 9999;
        MapInfo[] nearMapInfos = rc.senseNearbyMapInfos();
        for (int i = 0; i < nearMapInfos.length; i++)
        {
            if (nearMapInfos[i].isDam())
            {
                int dist = loc.distanceSquaredTo(nearMapInfos[i].getMapLocation());
                if (dist < closestDist)
                {
                    closestDist = dist;
                }
            }
        }

        //Go through recorded dam locations
        for (int i = Constants.DAM_LOCATIONS_IDX; i < Constants.DAM_LOCATIONS_IDX + Constants.NUM_DAM_LOCS; i++)
        {
            MapLocation damLoc = intToLocation(rc, rc.readSharedArray(i));
            if (damLoc != null) {
                int dist = loc.distanceSquaredTo(damLoc);
                if(dist < closestDist)
                {
                    closestDist = dist;
                }
            }
        }
        return closestDist;
    }

    //END OF FLAG MOVING

    //FLAG CARRIERS

    public static MapLocation getFlagCarrierLoc(RobotController rc) throws GameActionException {
        return intToLocation(rc, rc.readSharedArray(Constants.FLAG_CARRIER_LOC_IDX));
    }

    public static void sendFlagCarrierSignal(RobotController rc) throws GameActionException {
        rc.writeSharedArray(Constants.FLAG_CARRIER_LOC_IDX, locationToInt(rc, rc.getLocation()));
        rc.writeSharedArray(Constants.FLAG_CARRIER_LAST_SENT_IDX, rc.getRoundNum());
    }

    public static void updateFlagCarrierSignal(RobotController rc) throws GameActionException {
        MapLocation flagCarrierLocation = intToLocation(rc, rc.readSharedArray(Constants.FLAG_CARRIER_LOC_IDX));
        if (flagCarrierLocation != null) {
            int lastSeenTime = rc.getRoundNum() - rc.readSharedArray(Constants.FLAG_CARRIER_LAST_SENT_IDX);
            if (lastSeenTime > 5)
            {
                rc.writeSharedArray(Constants.FLAG_CARRIER_LOC_IDX, locationToInt(rc, null));
                rc.writeSharedArray(Constants.FLAG_CARRIER_LAST_SENT_IDX, 0);
            }
        }
    }

    public static void capturedFlag(RobotController rc) throws GameActionException {
        int flagId = SharedVariables.flagIdHeld;
        for (int i = 0; i < 3; i++)
        {
            if (rc.readSharedArray(Constants.ENEMY_FLAG_IDS_IDX + i) == flagId)
            {
                rc.writeSharedArray(Constants.ENEMY_FLAG_IDS_IDX + i, 19999);
                rc.writeSharedArray(Constants.ENEMY_FLAG_SPAWNS_IDX + i, 0);
                rc.writeSharedArray(Constants.ENEMY_FLAG_LOC_IDX + i, locationToInt(rc, null));
                break;
            }
        }
        rc.writeSharedArray(Constants.ENEMY_FLAG_COUNT_IDX, rc.readSharedArray(Constants.ENEMY_FLAG_COUNT_IDX) - 1);
    }

    //END OF FLAG CARRIERS




    //FRIENDLY FLAGS

    public static void updateLiveDefenderCounts(RobotController rc) throws GameActionException {
        if (rc.getRoundNum() > rc.readSharedArray(Constants.ROUND_NUM_IDX))
        {
            for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
            {
                int idx = SharedVariables.flagStatuses[i].idx;
                SharedVariables.flagStatuses[i].numberOfLiveDefenders = rc.readSharedArray(Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX + idx);
                rc.writeSharedArray(Constants.LIVE_DEFENDER_COUNTS_IDX + idx, rc.readSharedArray(Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX + idx));
                rc.writeSharedArray(Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX + idx, 0);
                rc.writeSharedArray(Constants.FRIENDLY_FLAG_ADD_IDX + idx, 0);
                rc.writeSharedArray(Constants.FRIENDLY_FLAG_REMOVE_IDX + idx, 0);
            }

        }

        //If we are a defender with a flag, mark that we are defending it
        if (SharedVariables.currentRole != null && SharedVariables.currentRole.equals(Role.Defender) && SharedVariables.flagIdToProtect != -1)
        {
            FlagStatus currentStatus = getFlagStatus(SharedVariables.flagIdToProtect);
            if (currentStatus == null || currentStatus.idx == -1) return;
            rc.writeSharedArray(Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX + currentStatus.idx, rc.readSharedArray(Constants.TEMP_LIVE_DEFENDER_COUNTS_IDX + currentStatus.idx) + 1);
        }
    }
    public static int getFriendlyFlagsCount() throws GameActionException {
        FlagStatus[] flagStatuses = SharedVariables.flagStatuses;
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
    public static MapLocation getFriendlyFlagLocationFromId(RobotController rc, int flagId) throws GameActionException {
        FlagStatus currentStatus = getFlagStatus(flagId);
        if (currentStatus != null)
        {
            if (currentStatus.isLost)
            {
                return currentStatus.spawn;
            }
            return currentStatus.location;
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
    public static FlagStatus getFlagStatus(int id)
    {
        if (id == -1) return null;
        for (int i = 0; i < SharedVariables.flagStatuses.length; i++)
        {
            if (SharedVariables.flagStatuses[i].id == id)
            {
                return SharedVariables.flagStatuses[i];
            }
        }
        return null;
    }
    public static FlagStatus[] getFriendlyFlagStatuses(RobotController rc) throws GameActionException {
        FlagStatus[] friendlyFlagStatuses = new FlagStatus[GameConstants.NUMBER_FLAGS];
        int numStatuses = 0;
        for (int i = 0; i < GameConstants.NUMBER_FLAGS; i++)
        {
            //Check if the id at i=0 exists
            int existingVal = rc.readSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + i);
            int existingId = existingVal & ((int) Math.pow(2, 14) - 1);
            boolean existingInDanger = (existingVal >> 14) == 1 || (existingVal >> 14) == 3;
            boolean heldByAlly = (existingVal >> 15) == 1;

            if (existingVal != 0)
            {
                //Get location information
                MapLocation existingLoc = intToLocation(rc, rc.readSharedArray(Constants.FRIENDLY_FLAG_LOC_IDX + i));
                MapLocation existingSpawn = intToLocation(rc, rc.readSharedArray(Constants.FRIENDLY_FLAG_SPAWNS_IDX + i));
                //Get Last Seen information
                int lostTime = rc.getRoundNum()-rc.readSharedArray(Constants.FRIENDLY_FLAGS_LAST_SEEN_IDX + i);
                boolean isLost = lostTime >= Constants.TIME_FLAG_UNSEEN_UNTIL_LOST && rc.getRoundNum() > GameConstants.SETUP_ROUNDS;

                //Get Number of Defenders Information
                int numLiveDefenders = rc.readSharedArray(Constants.LIVE_DEFENDER_COUNTS_IDX + i) - rc.readSharedArray(Constants.FRIENDLY_FLAG_REMOVE_IDX + i) + rc.readSharedArray(Constants.FRIENDLY_FLAG_ADD_IDX + i);

                //Add a new flag status
                FlagStatus status = new FlagStatus();
                status.id = existingId;
                status.inDanger = existingInDanger;
                status.isHeldByAlly = heldByAlly;
                status.isLost = isLost;
                status.location = existingLoc;
                status.spawn = existingSpawn;
                status.numberOfLiveDefenders = numLiveDefenders;
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
        FlagStatus[] statuses = SharedVariables.flagStatuses;
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
            System.out.println("Number of live defenders = " + statuses[i].numberOfLiveDefenders);
        }
    }

    public static void printFlagDefenses(RobotController rc) throws GameActionException {
        FlagStatus[] flagStatuses = SharedVariables.flagStatuses;
        int totalLiveBots = 0;
        for (int i = 0; i < flagStatuses.length; i++)
        {
            totalLiveBots += flagStatuses[i].numberOfLiveDefenders;
            System.out.println("There are " + flagStatuses[i].numberOfLiveDefenders + " live bots defending ID = " + flagStatuses[i].id + " at " + flagStatuses[i].location + " isLost = " + flagStatuses[i].isLost);
        }
        System.out.println("Total Live Bots Defending = " + totalLiveBots);
        System.out.println("Total bots with Defender Role = " + rc.readSharedArray(Constants.DEFENDER_ROLE_IDX));
    }

    public static void updateFriendlyFlags(RobotController rc) throws GameActionException {
        FlagInfo[] nearFlags = rc.senseNearbyFlags(-1, rc.getTeam());
        for (int i = 0; i < nearFlags.length; i++)
        {
            FlagStatus currentStatus = getFlagStatus(nearFlags[i].getID());
            if (currentStatus == null)
            {
                //Add new flagStatus
                System.out.println("Adding flagStatus for ID = " + nearFlags[i].getID());
                //See where we could add a near status
                int idx = -1;
                for (int j = 0; j < GameConstants.NUMBER_FLAGS; j++)
                {
                    if (rc.readSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + j) == 0)
                    {
                        //Write it here!
                        idx = j;
                        break;
                    }
                }
                if (idx == -1) break;
                currentStatus = new FlagStatus();
                currentStatus.idx = idx;
                currentStatus.id = nearFlags[i].getID();
                rc.writeSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + idx, nearFlags[i].getID());
                MapLocation flagLoc = nearFlags[i].getLocation();
                //Update existing flagStatus

                currentStatus.spawn = flagLoc;
                rc.writeSharedArray(Constants.FRIENDLY_FLAG_SPAWNS_IDX + idx, locationToInt(rc, flagLoc));

                //LOCATION
                currentStatus.location = nearFlags[i].getLocation();
                rc.writeSharedArray(Constants.FRIENDLY_FLAG_LOC_IDX + idx, locationToInt(rc, flagLoc));

                //inDanger + isHeldByAlly
                boolean heldByAlly = (nearFlags[i].isPickedUp() && (rc.senseRobotAtLocation(flagLoc).getTeam().isPlayer())) || (rc.hasFlag() && rc.getLocation().equals(flagLoc));
                boolean inDanger = nearFlags[i].isPickedUp() && (!rc.senseRobotAtLocation(flagLoc).getTeam().isPlayer());
                if (!inDanger)
                {
                    inDanger = rc.senseNearbyRobots(flagLoc, -1, rc.getTeam().opponent()).length >= Constants.MIN_ENEMIES_FOR_DANGER;
                }
                currentStatus.inDanger = inDanger;
                currentStatus.isHeldByAlly = heldByAlly;
                int valToWrite = currentStatus.id;
                if (inDanger)
                {
                    valToWrite += (int)Math.pow(2, 14);
                }
                if (heldByAlly)
                {
                    valToWrite += (int)Math.pow(2, 15);
                }
                rc.writeSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + idx, valToWrite);
                //LAST SEEN
                if (rc.readSharedArray(idx + Constants.FRIENDLY_FLAGS_LAST_SEEN_IDX) < rc.getRoundNum()) {
                    rc.writeSharedArray(idx + Constants.FRIENDLY_FLAGS_LAST_SEEN_IDX, rc.getRoundNum());
                }
                FlagStatus[] allStatuses = new FlagStatus[SharedVariables.flagStatuses.length + 1];
                System.arraycopy(SharedVariables.flagStatuses, 0, allStatuses, 0, SharedVariables.flagStatuses.length);
                allStatuses[SharedVariables.flagStatuses.length] = currentStatus;
                SharedVariables.flagStatuses = allStatuses;
            }
            else
            {
                int idx = currentStatus.idx;
                MapLocation flagLoc = nearFlags[i].getLocation();
                //Update existing flagStatus

                //SPAWN isn't updated unless location is null
                MapLocation currLoc = SharedVariables.flagStatuses[idx].location;
                if (currLoc == null)
                {
                    SharedVariables.flagStatuses[idx].spawn = flagLoc;
                    rc.writeSharedArray(Constants.FRIENDLY_FLAG_SPAWNS_IDX + idx, locationToInt(rc, flagLoc));
                }

                //LOCATION
                SharedVariables.flagStatuses[currentStatus.idx].location = nearFlags[i].getLocation();
                rc.writeSharedArray(Constants.FRIENDLY_FLAG_LOC_IDX + idx, locationToInt(rc, flagLoc));

                //inDanger + isHeldByAlly
                boolean heldByAlly = (nearFlags[i].isPickedUp() && (rc.senseRobotAtLocation(flagLoc).getTeam().isPlayer())) || (rc.hasFlag() && rc.getLocation().equals(flagLoc));
                boolean inDanger = nearFlags[i].isPickedUp() && (!rc.senseRobotAtLocation(flagLoc).getTeam().isPlayer());
                if (!inDanger)
                {
                    inDanger = rc.senseNearbyRobots(flagLoc, -1, rc.getTeam().opponent()).length >= Constants.MIN_ENEMIES_FOR_DANGER;
                }
                SharedVariables.flagStatuses[currentStatus.idx].inDanger = inDanger;
                SharedVariables.flagStatuses[currentStatus.idx].isHeldByAlly = heldByAlly;
                int valToWrite = currentStatus.id;
                if (inDanger)
                {
                    valToWrite += (int)Math.pow(2, 14);
                }
                if (heldByAlly)
                {
                    valToWrite += (int)Math.pow(2, 15);
                }
                rc.writeSharedArray(Constants.FRIENDLY_FLAG_IDS_IDX + idx, valToWrite);
                //LAST SEEN
                if (rc.readSharedArray(idx + Constants.FRIENDLY_FLAGS_LAST_SEEN_IDX) < rc.getRoundNum()) {
                    rc.writeSharedArray(idx + Constants.FRIENDLY_FLAGS_LAST_SEEN_IDX, rc.getRoundNum());
                }

            }
        }
    }

    //END OF FRIENDLY FLAGS

    //ENEMY FLAGS

    public static int getEnemyFlagsCount(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.ENEMY_FLAG_COUNT_IDX);
    }

    public static void printEnemyFlagLocs(RobotController rc) throws GameActionException {
        System.out.println("Enemy Flag Locs");
        for (int i = 0; i < 3; i++)
        {
            MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i + Constants.ENEMY_FLAG_LOC_IDX));
            MapLocation flagSpawn = intToLocation(rc, rc.readSharedArray(i + Constants.ENEMY_FLAG_SPAWNS_IDX));
            int flagId = rc.readSharedArray(i + Constants.ENEMY_FLAG_IDS_IDX);
            System.out.println(flagLoc + ", id = " + flagId + ", spawn = " + flagSpawn);
        }
    }

    public static MapLocation getClosestEnemyFlagLoc(RobotController rc) throws GameActionException {
        int minDist = 9999;
        MapLocation bestFlagLoc = null;
        for (int i = 0; i < 3; i++)
        {
            MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i + Constants.ENEMY_FLAG_LOC_IDX));
            if (flagLoc != null)
            {
                int dist = flagLoc.distanceSquaredTo(rc.getLocation());
                if (dist < minDist)
                {
                    minDist = dist;
                    bestFlagLoc = flagLoc;
                }
            }
        }
        return bestFlagLoc;
    }

    public static void updateEnemyFlags(RobotController rc) throws GameActionException {
        FlagInfo[] nearFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
        MapLocation[] broadcastedFlags = rc.senseBroadcastFlagLocations();
        boolean[] known = new boolean[3];
        for (int i = 0; i < nearFlags.length; i++)
        {
            int idx = -1;
            int existingIdx = -1;
            for (int j = 0; j < GameConstants.NUMBER_FLAGS; j++)
            {
                if (rc.readSharedArray(j + Constants.ENEMY_FLAG_IDS_IDX) == nearFlags[i].getID())
                {
                    existingIdx = j;
                    break;
                }
                else if (rc.readSharedArray(j + Constants.ENEMY_FLAG_IDS_IDX) == 0)
                {
                    idx = j;
                }
            }
            if (existingIdx == -1 && idx != -1)
            {
                //Update id and spawn
                known[i] = true;
                rc.writeSharedArray(idx + Constants.ENEMY_FLAG_IDS_IDX, nearFlags[i].getID());
                rc.writeSharedArray(idx + Constants.ENEMY_FLAG_SPAWNS_IDX, locationToInt(rc, nearFlags[i].getLocation()));
            }
            else if (existingIdx != -1)
            {
                //Update loc
                known[i] = true;
                rc.writeSharedArray(existingIdx + Constants.ENEMY_FLAG_LOC_IDX, locationToInt(rc, nearFlags[i].getLocation()));
            }
        }

        for (int i = 0; i < 3; i++)
        {
            int flagId = rc.readSharedArray(i + Constants.ENEMY_FLAG_IDS_IDX);
            if (flagId == 19999) continue;
            MapLocation flagLoc = intToLocation(rc, rc.readSharedArray(i + Constants.ENEMY_FLAG_LOC_IDX));
            if (!known[i] && rc.readSharedArray(i + Constants.ENEMY_FLAG_IDS_IDX) == 0)
            {
                int minDist = 9999;
                MapLocation bestLoc = null;
                if (flagLoc == null)
                {
                    rc.writeSharedArray(Constants.ENEMY_FLAG_LOC_IDX + i, locationToInt(rc, broadcastedFlags[0]));
                    return;
                }
                for (int j = 0; j < broadcastedFlags.length; j++)
                {
                    int dist = flagLoc.distanceSquaredTo(broadcastedFlags[j]);
                    if (dist < minDist)
                    {
                        minDist = dist;
                        bestLoc = broadcastedFlags[j];
                    }
                }
                if (bestLoc != null)
                {
                    rc.writeSharedArray(Constants.ENEMY_FLAG_LOC_IDX + i, locationToInt(rc, bestLoc));
                }
            }
        }
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

    //END OF ENEMY FLAGS

    //RENDEZVOUS

    public static void updateRendezvousCounts(RobotController rc) throws GameActionException {
        MapLocation currentRendezvous = intToLocation(rc, rc.readSharedArray(Constants.RENDEZVOUS_IDX));
        if (currentRendezvous != null)
        {
            if (rc.getRoundNum() > rc.readSharedArray(Constants.ROUND_NUM_IDX))
            {
                rc.writeSharedArray(Constants.RENDEZVOUS_COUNT_IDX, 0);
            }
            if (rc.getLocation().isWithinDistanceSquared(currentRendezvous, 30))
            {
                rc.writeSharedArray(Constants.RENDEZVOUS_COUNT_IDX, rc.readSharedArray(Constants.RENDEZVOUS_COUNT_IDX ) + 1);
            }
        }
    }
    public static MapLocation getRendezvous(RobotController rc) throws GameActionException {
        return null;
        /*
        if (rc.getRoundNum() < GameConstants.SETUP_ROUNDS) return null;
        MapLocation currentRendezvous = intToLocation(rc, rc.readSharedArray(Constants.RENDEZVOUS_IDX));
        int rendezvousRound = rc.readSharedArray(Constants.RENDEZVOUS_ROUND_IDX);
        int rendezvousCount = rc.readSharedArray(Constants.RENDEZVOUS_COUNT_IDX);
        if (currentRendezvous != null && (rendezvousCount >= 15 || rc.getRoundNum() >= rendezvousRound))
        {
            //Remove current rendezvous
            System.out.println("Removed Rendezvous = " + currentRendezvous);
            rc.writeSharedArray(Constants.RENDEZVOUS_ROUND_IDX, rc.getRoundNum() + 100);
            rc.writeSharedArray(Constants.RENDEZVOUS_COUNT_IDX, 0);
            rc.writeSharedArray(Constants.RENDEZVOUS_IDX, locationToInt(rc, null));
            currentRendezvous = intToLocation(rc, rc.readSharedArray(Constants.RENDEZVOUS_IDX));
            rendezvousRound = rc.readSharedArray(Constants.RENDEZVOUS_ROUND_IDX);
        }

        if (currentRendezvous == null && Communication.getFlagCarrierLoc(rc) == null && rc.getRoundNum() >= rendezvousRound)
        {
            //Generate a new rendezvous
            MapLocation bestFlagLoc = getClosestEnemyFlagLoc(rc);
            if(bestFlagLoc != null)
            {
                //Add rendezvous point near the bestFlagLoc
                Direction dirToFlag = rc.getLocation().directionTo(bestFlagLoc);
                MapLocation loc = bestFlagLoc;
                for (int i = 0; i < 7; i++)
                {
                    if (!rc.onTheMap(loc.subtract(dirToFlag))) break;
                    loc = loc.subtract(dirToFlag);
                }
                currentRendezvous = loc;
                rc.writeSharedArray(Constants.RENDEZVOUS_ROUND_IDX, rc.getRoundNum() + 300);
                rc.writeSharedArray(Constants.RENDEZVOUS_COUNT_IDX, 0);
                rc.writeSharedArray(Constants.RENDEZVOUS_IDX, locationToInt(rc, currentRendezvous));
                System.out.println("New Rendezvous = " + currentRendezvous);
            }
        }
        //System.out.println("Rendezvous = " + currentRendezvous);
        return currentRendezvous;

         */
    }

    //END OF RENDEZVOUS



    //STRATEGY
    public static void initializeFlagCounts(RobotController rc) throws GameActionException {
        rc.writeSharedArray(Constants.FRIENDLY_FLAG_COUNT_IDX, GameConstants.NUMBER_FLAGS);
        rc.writeSharedArray(Constants.ENEMY_FLAG_COUNT_IDX, GameConstants.NUMBER_FLAGS);
    }
    public static int getStrategy(RobotController rc) throws GameActionException {
        return rc.readSharedArray(Constants.STRATEGY_IDX);
    }

    public static void setStrategy(RobotController rc, int strategyType) throws GameActionException {
        rc.writeSharedArray(Constants.STRATEGY_IDX, strategyType);
    }

    //END OF STRATEGY

    //UTILITY

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

    //END OF UTILITY

    //COMMS UPDATE

    public static void updateComms(RobotController rc) throws GameActionException {
        SharedVariables.flagStatuses = Communication.getFriendlyFlagStatuses(rc);
        updateLiveDefenderCounts(rc);
        updateFriendlyFlags(rc);
        updateEnemyFlags(rc);
        updateFlagCarrierSignal(rc);
        if (rc.getRoundNum() < 5)
        {
            initializeFlagCounts(rc);
        }
        updateRendezvousCounts(rc);
        updateDamLocs(rc);
        if (rc.getRoundNum() > rc.readSharedArray(Constants.ROUND_NUM_IDX))
        {
            rc.writeSharedArray(Constants.ROUND_NUM_IDX, rc.getRoundNum());
        }
    }

    //END OF COMMS UPDATE
}