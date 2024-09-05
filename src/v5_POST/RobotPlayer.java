package v5_POST;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.Random;

public strictfp class RobotPlayer {
    public static void run(RobotController rc) throws GameActionException {
        SharedVariables.rc = rc;
        SharedVariables.rng = new Random(rc.getID());

        while (true) {
            try {
                if (!rc.isSpawned())
                {
                    //Try to spawn
                    MapLocation spawnLocs[] = rc.getAllySpawnLocations();
                    for (int i = 0; i < spawnLocs.length; i++)
                    {
                        if (rc.canSpawn(spawnLocs[i]))
                        {
                            rc.spawn(spawnLocs[i]);
                        }
                    }
                }
                else
                {
                    //if (rc.getRoundNum() > 500) rc.resign(); //DEBUGGING ONLY
                    Communication.updateAllCommunications();
                    determineRole(rc);
                }

            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();

            } finally {
                Clock.yield();
            }
        }
    }

    /**
     * Determines when to be an attacker or defender and start up
     * role-specific code. A bot should be a defender if it senses enemy bots near
     * a friendly flag. A bot should be an attacker if it senses no enemy bots.
     * Otherwise, the existing role doesn't change.
     *
     * @param rc The RobotController
     */
    public static void determineRole(RobotController rc) throws GameActionException {
        if (Role.isAttacker())
        {
            rc.setIndicatorString("Attacker - ID = " + SharedVariables.standardID);
        }
        else if (Role.isDefender())
        {
            rc.setIndicatorString("Defender - ID = " + SharedVariables.standardID);
        }
        Micro.fullMicro();
    }
}
