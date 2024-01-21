package v19;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

//Controls where a jailed duck respawns
public class Spawning {

    private static boolean attemptToSpawnNear(RobotController rc, MapLocation loc) throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        int minDist = 9999;
        MapLocation bestSpawn = null;
        for (int i = 0; i < spawnLocs.length; i++)
        {
            if (rc.canSpawn(loc))
            {
                int dist = spawnLocs[i].distanceSquaredTo(loc);
                if (dist < minDist)
                {
                    minDist = dist;
                    bestSpawn = spawnLocs[i];
                }
            }
        }
        if (bestSpawn != null)
        {
            rc.spawn(bestSpawn);
            return true;
        }
        return false;
    }

    private static boolean attemptToRandomSpawn(RobotController rc) throws GameActionException {
        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
        for (int i = 0; i < spawnLocs.length; i++)
        {
            if (rc.canSpawn(spawnLocs[i]))
            {
                rc.spawn(spawnLocs[i]);
                return true;
            }
        }
        return false;
    }

    public static void spawnIn(RobotController rc) throws GameActionException {
        //Attempt to spawn nearest to your flagToAttack or flagToDefend
        boolean success = false;
        if (SharedVariables.currentRole == null)
        {
            //Try random spawn
            success = attemptToRandomSpawn(rc);
        }
        else if (SharedVariables.currentRole.equals(Role.Attacker) && SharedVariables.flagToProtect != null)
        {
            success = attemptToSpawnNear(rc, SharedVariables.flagToProtect);
        }
        else
        {
            MapLocation preferedLoc = Communication.getPreferedSpawnAttackers(rc);
            if (SharedVariables.currentRole.equals(Role.Defender) && SharedVariables.flagToProtect != null)
            {
                success = attemptToSpawnNear(rc, preferedLoc);
            }
        }
        if (!success)
        {
            success = attemptToRandomSpawn(rc);
        }
        if (rc.isSpawned())
        {
            if (SharedVariables.spawnRound == 0)
                SharedVariables.spawnRound = rc.getRoundNum();
        }
    }
}
