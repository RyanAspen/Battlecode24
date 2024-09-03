package v2_POST;

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
                    //if (rc.getRoundNum() > 500) rc.resign(); //DEBUGGING ONLYs
                    Communication.updateFlagCommunication();
                    NormalUnit.run();
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
}
