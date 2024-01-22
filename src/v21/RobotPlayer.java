package v21;

import battlecode.common.*;

import java.util.Random;

//TODO: Issue with spawning in the right location


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

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
    public static void run(RobotController rc) throws GameActionException {
        SharedVariables.rng = new Random(rc.getID());
        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                SharedVariables.updatedTempDefender = false;
                if (!rc.isSpawned()){
                    rc.setIndicatorString("Just Spawned");
                    Spawning.spawnIn(rc);
                }
                else{
                    if (SharedVariables.spawnRound == rc.getRoundNum() + 1)
                    {
                        Communication.initializeFlagCounts(rc);
                    }
                    TrapExploiter.updateTrapInfo(rc);
                    Communication.updateComms(rc);
                    Strategy.updateRole(rc);
                    Strategy.changeStrategy(rc);

                    SharedVariables.flagStatuses = Communication.getFriendlyFlagStatuses(rc);
                    FlagSetup.stayCloseToSpawn(rc);
                    FlagInfo nearFlags[] = rc.senseNearbyFlags(-1);
                    for (int i = 0; i < nearFlags.length; i++)
                    {
                        if (nearFlags[i].getTeam().equals(rc.getTeam().opponent()))
                        {
                            if (rc.canPickupFlag(nearFlags[i].getLocation()))
                            {
                                rc.pickupFlag(nearFlags[i].getLocation());
                            }
                        }
                    }

                    // If we are holding an enemy flag, singularly focus on moving towards
                    // an ally spawn zone to capture it! We use the check roundNum >= SETUP_ROUNDS
                    // to make sure setup phase has ended.
                    if (rc.hasFlag() && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS){
                        FlagCarrier.runFlagCarrier(rc);
                        continue;
                    }

//                    //If an enemy flag is close, go for it regardless of role
//                    FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
//                    if (enemyFlags.length > 0)
//                    {
//                        Pathing.moveTowards(rc, enemyFlags[0].getLocation());
//                    }

                    //For now on, all role specific movement is here
                    if (SharedVariables.currentRole != null) {
                        switch (SharedVariables.currentRole) {
                            case Attacker:
                                Attacker.runAttacker(rc);
                                break;
                            case Defender:
                                Defender.runDefender(rc);
                                break;
                        }
                    }
                    else
                    {
                        Attacker.runAttacker(rc);
                        rc.setIndicatorString("Default  (Attacker)");
                    }
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();
                System.out.println("IsSpawned = " + rc.isSpawned());
                System.out.println("Bytecode Left = " + Clock.getBytecodesLeft());

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();
                System.out.println("IsSpawned = " + rc.isSpawned());
                System.out.println("Bytecode Left = " + Clock.getBytecodesLeft());

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }
}
