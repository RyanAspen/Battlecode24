package v3_POST;

import battlecode.common.FlagInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

class FlagStatus {
    MapLocation location;
    int id;
    boolean notFound = true;
    boolean friendlyTeam = true;

    FlagStatus(){}

    public String toString()
    {
        return "{ID = " + id + " Location = " + location.toString() + " NotFound = " + notFound + " Friendly = " + friendlyTeam + "}";
    }

}

class FlagHolder {
    MapLocation loc = null;
    boolean friendlyCarrier = true;
    int flagId = -1;
}

class Communication {

    /**
     * This function is called once per round per bot. It updates all communications.
     */
    public static void updateAllCommunications() throws GameActionException {
        updateFlagCommunication();
        updateClusterCommunications();
    }

    /**
     * This function is called once per round per bot. If there
     * is a cluster point that has expired, delete it from the global array.
     */
    public static void updateClusterCommunications() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        for (int i = Constants.CLUSTER_IDX; i < Constants.CLUSTER_IDX + 2 * Constants.MAX_NUM_CLUSTERS; i+=2)
        {
            int roundToDelete = rc.readSharedArray(i+1);
            if (roundToDelete != 0)
            {
                if (roundToDelete <= rc.getRoundNum())
                {
                    rc.writeSharedArray(i, 0);
                    rc.writeSharedArray(i+1, 0);
                }
            }
        }
    }

    /**
     * This function adds a cluster point to the global array for a set amount
     * of rounds. Information is stored between the range
     * (CLUSTER_IDX, CLUSTER_IDX+2*MAX_NUM_CLUSTERS-1).
     * Each cluster point is stored with two ints:
     * the first int is formatted 0000 - (x position (6 bits)) - (y position (6 bits));
     * the second int is just the round at which to delete the cluster point.
     * @param cluster the MapLocation of the cluster
     * @param roundsToCluster how many rounds the cluster should last
     */
    public static void addClusterPoint(MapLocation cluster, int roundsToCluster) throws GameActionException {
        RobotController rc = SharedVariables.rc;
        int firstOpenIdx = -1;
        for (int i = Constants.CLUSTER_IDX; i < Constants.CLUSTER_IDX + 2 * Constants.MAX_NUM_CLUSTERS; i+=2)
        {
            int roundToDelete = rc.readSharedArray(i+1);
            if (roundToDelete == 0 && firstOpenIdx == -1)
            {
                firstOpenIdx = i;
            }
            else if (roundToDelete > 0)
            {
                int mapLocationInt = rc.readSharedArray(i);
                int x = (mapLocationInt & 0b0000111111000000) >> 6;
                int y = mapLocationInt & 0b0000000000111111;
                MapLocation existingClusterLoc = new MapLocation(x,y);
                if (cluster.isWithinDistanceSquared(existingClusterLoc, Constants.MIN_DISTANCE_BETWEEN_CLUSTERS))
                {
                    return;
                }
            }
        }
        if (firstOpenIdx != -1)
        {
            //Add cluster point
            int newClusterPointInt = (cluster.x << 6) + cluster.y;
            rc.writeSharedArray(firstOpenIdx, newClusterPointInt);
            rc.writeSharedArray(firstOpenIdx+1, rc.getRoundNum()+roundsToCluster);
        }
    }

    /**
     * This function gets the closest cluster point to the given location.
     * @param location the cluster point chosen is the closest to this MapLocation
     */
    public static MapLocation getClusterPoint(MapLocation location) throws GameActionException {
        RobotController rc = SharedVariables.rc;
        MapLocation closestLoc = null;
        int closestDist = 9999;
        for (int i = Constants.CLUSTER_IDX; i < Constants.CLUSTER_IDX + 2 * Constants.MAX_NUM_CLUSTERS; i+=2)
        {
            if (rc.readSharedArray(i+1) == 0) continue;
            int mapLocationInt = rc.readSharedArray(i);
            int x = (mapLocationInt & 0b0000111111000000) >> 6;
            int y = mapLocationInt & 0b0000000000111111;
            MapLocation existingClusterLoc = new MapLocation(x,y);
            int dist = rc.getLocation().distanceSquaredTo(existingClusterLoc);
            if (dist < closestDist)
            {
                closestDist = dist;
                closestLoc = existingClusterLoc;
            }
        }
        return closestLoc;
    }



    /**
     * This function is called by every bot before they do anything else each turn.
     * Flags that are sensed have their flagStatuses added/updated, while flagStatuses
     * that should be sensed but aren't are logged as notFound.
     */
    public static void updateFlagCommunication() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        FlagInfo[] nearFlags = rc.senseNearbyFlags(-1);
        SharedVariables.friendlyFlagCarrierLocs = null;
        for (FlagInfo nearFlag : nearFlags) {
            FlagStatus status = new FlagStatus();
            status.id = nearFlag.getID();
            status.notFound = false;
            status.location = nearFlag.getLocation();
            status.friendlyTeam = (nearFlag.getTeam().equals(rc.getTeam()));
            if (!status.friendlyTeam && nearFlag.isPickedUp())
            {
                FlagHolder holder = new FlagHolder();
                holder.flagId = status.id;
                holder.friendlyCarrier = true;
                holder.loc = status.location;
                if (SharedVariables.friendlyFlagCarrierLocs == null)
                {
                    SharedVariables.friendlyFlagCarrierLocs = new FlagHolder[1];
                    SharedVariables.friendlyFlagCarrierLocs[0] = holder;
                }
                else
                {
                    boolean found = false;
                    FlagHolder[] finalHolders = new FlagHolder[SharedVariables.friendlyFlagCarrierLocs.length + 1];
                    for (int i = 0; i < SharedVariables.friendlyFlagCarrierLocs.length; i++)
                    {
                        if (SharedVariables.friendlyFlagCarrierLocs[i].flagId == holder.flagId)
                        {
                            SharedVariables.friendlyFlagCarrierLocs[i] = holder;
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        System.arraycopy(SharedVariables.friendlyFlagCarrierLocs, 0, finalHolders, 0, SharedVariables.friendlyFlagCarrierLocs.length);
                        finalHolders[SharedVariables.friendlyFlagCarrierLocs.length] = holder;
                        SharedVariables.friendlyFlagCarrierLocs = finalHolders;
                    }
                }
            }
            addFlag(status);
        }
        SharedVariables.statuses = getAllFlagStatuses();
        for (int i = 0; i < SharedVariables.statuses.length; i++)
        {
            FlagStatus status = SharedVariables.statuses[i];
            if (!status.notFound && rc.canSenseLocation(status.location))
            {
                boolean idFound = false;
                for (FlagInfo nearFlag : nearFlags)
                {
                    if (status.id == nearFlag.getID())
                    {
                        idFound = true;
                        break;
                    }
                }
                if(!idFound)
                {
                    markFlagAsUnknown(status.id);
                    SharedVariables.statuses[i].notFound = true;
                }
            }
        }
        //if (rc.getID() == 13618) printFlagInts(); //For Debugging Only
    }

    /**
     * This function is used to add information about a flag to the global array.
     * Information is stored between the range
     * (Constants.FLAG_IDX, Constants.FLAG_IDX + 4 * Constants.MAX_NUM_FLAGS_PER_TEAM - 1).
     * Each flag is stored with two ints: the first is formatted as
     * 00 - (x position (6 bits)) - (y position (6 bits)) - (notFound (1 bit)) - (friendly (1 bit));
     * the second int just stores the ID of the flag.
     * @param status information about the flag to add to the global array
     */
    public static void addFlag(FlagStatus status) throws GameActionException {
        RobotController rc = SharedVariables.rc;
        for (int i = Constants.FLAG_IDX; i < Constants.FLAG_IDX + 4 * Constants.MAX_NUM_FLAGS_PER_TEAM; i+=2)
        {
            int existingId = rc.readSharedArray(i+1);
            if (existingId == status.id || existingId == 0)
            {
                int newStatusInt = 0;
                if (status.friendlyTeam)
                {
                    newStatusInt = 1;
                }
                //newStatusInt += 2; //For notFound, which is always false
                newStatusInt = newStatusInt + (status.location.y << 2) + (status.location.x << 8);
                rc.writeSharedArray(i, newStatusInt);
                if (existingId == 0)
                {
                    rc.writeSharedArray(i + 1, status.id);
                }
                break;
            }
        }
    }

    /**
     * This function marks a known flag with given id as unknown.
     * @param id the id of the flag queried
     */
    public static void markFlagAsUnknown(int id) throws GameActionException {
        RobotController rc = SharedVariables.rc;
        for (int i = Constants.FLAG_IDX; i < Constants.FLAG_IDX + 4 * Constants.MAX_NUM_FLAGS_PER_TEAM; i+=2)
        {
            int existingId = rc.readSharedArray(i+1);
            if (existingId == id)
            {
                int val = rc.readSharedArray(i);
                if ((val & 0b0000000000000010) == 0)
                {
                    val += 2;
                    rc.writeSharedArray(i, val);
                }
                break;
            }
        }
    }

    /**
     * This function gets known information about a flag with the given id.
     * @param id the id of the flag queried
     * @return FlagStatus - information about the flag with the given id
     */
    public static FlagStatus getFlagStatus(int id) throws GameActionException {
        RobotController rc = SharedVariables.rc;
        for (int i = Constants.FLAG_IDX; i < Constants.FLAG_IDX + 4 * Constants.MAX_NUM_FLAGS_PER_TEAM; i+=2) {
            int existingId = rc.readSharedArray(i + 1);
            if (existingId == id)
            {
                int info = rc.readSharedArray(i);
                FlagStatus status = new FlagStatus();
                status.id = id;
                int x = (info & 0b0011111100000000) >> 8;
                int y = (info & 0b0000000011111100) >> 2;
                status.notFound = ((info & 0b0000000000000010) >> 1) == 1;
                status.friendlyTeam = (info & 0b0000000000000001) == 1;
                status.location = new MapLocation(x,y);
                return status;
            }
        }
        return null;
    }

    /**
     * This function gets known information about all known flags.
     * @return FlagStatus[] - an array of information about each known flag
     */
    public static FlagStatus[] getAllFlagStatuses() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        FlagStatus[] statuses = new FlagStatus[Constants.MAX_NUM_FLAGS_PER_TEAM*2];
        int currentIdx = 0;
        for (int i = Constants.FLAG_IDX; i < Constants.FLAG_IDX + 4 * Constants.MAX_NUM_FLAGS_PER_TEAM; i+=2) {
            int existingId = rc.readSharedArray(i + 1);
            if (existingId != 0)
            {
                int info = rc.readSharedArray(i);
                FlagStatus status = new FlagStatus();
                status.id = existingId;
                int x = (info & 0b0011111100000000) >> 8;
                int y = (info & 0b0000000011111100) >> 2;
                status.notFound = ((info & 0b0000000000000010) >> 1) == 1;
                status.friendlyTeam = (info & 0b0000000000000001) == 1;
                status.location = new MapLocation(x,y);
                statuses[currentIdx] = status;
                currentIdx++;
            }
        }
        FlagStatus[] finalStatuses = new FlagStatus[currentIdx];
        System.arraycopy(statuses, 0, finalStatuses, 0, currentIdx);
        return finalStatuses;
    }

    /**
     * This function prints information about each flag both in raw integer format and
     * a cleaner format. This function should only be used for debugging.
     */
    public static void printFlagInts() throws GameActionException {
        RobotController rc = SharedVariables.rc;
        for (int i = Constants.FLAG_IDX; i < Constants.FLAG_IDX + 4 * Constants.MAX_NUM_FLAGS_PER_TEAM; i+=2)
        {
            if (rc.readSharedArray(i+1) == 0) continue;
            System.out.println("Flag Info Int = "  + rc.readSharedArray(i));
            FlagStatus status = getFlagStatus(rc.readSharedArray(i+1));
            if (status != null) {
                System.out.println("Not Found = " + status.notFound);
                System.out.println("Is Friendly = " + status.friendlyTeam);
                System.out.println("Location = " + status.location.toString());
            }
            else
            {
                System.out.println("No status");
            }
            System.out.println("Flag ID = " + rc.readSharedArray(i+1));
        }
    }
}
