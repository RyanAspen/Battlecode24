package v8;

import battlecode.common.*;

import java.util.Random;

//Our imports


/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    static Random rng = null;

    static MapLocation spawnPoint = null;

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };



    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc  The RobotController object. You use it to perform actions from this robot, and to get
     *            information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        rng = new Random(rc.getID());

        Communication.initializeRole(rc);

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // Make sure you spawn your robot in before you attempt to take any actions!
                // Robots not spawned in do not have vision of any tiles and cannot perform any actions.
                if (!rc.isSpawned()){
                    MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                    // Pick a random spawn location to attempt spawning in.
                    MapLocation randomLoc = spawnLocs[rng.nextInt(spawnLocs.length)];
                    if (rc.canSpawn(randomLoc)) {
                        rc.spawn(randomLoc);
                        spawnPoint = randomLoc;
                    }
                }
                else{
                    Communication.updateComms(rc);
                    Role currentRole = Strategy.getRole(rc);
                    Strategy.changeStrategy(rc);
                    if (turnCount < GameConstants.SETUP_ROUNDS)
                    {
                        Economy.econ(rc);
                    }
                    else {
                        if (rc.canPickupFlag(rc.getLocation()))
                        {
                            rc.pickupFlag(rc.getLocation());
                        }
                        else
                        {
                            for (int i = 0; i < directions.length; i++)
                            {
                                if (rc.canPickupFlag(rc.getLocation().add(directions[i])))
                                {
                                    rc.pickupFlag(rc.getLocation().add(directions[i]));
                                }
                            }
                        }
                    }
                    // If we are holding an enemy flag, singularly focus on moving towards
                    // an ally spawn zone to capture it! We use the check roundNum >= SETUP_ROUNDS
                    // to make sure setup phase has ended.
                    if (rc.hasFlag() && rc.getRoundNum() >= GameConstants.SETUP_ROUNDS){
                        MapLocation[] spawnLocs = rc.getAllySpawnLocations();
                        int minDist = spawnLocs[0].distanceSquaredTo(rc.getLocation());
                        MapLocation closestSpawnLoc = spawnLocs[0];
                        for (int i = 1; i < spawnLocs.length; i++)
                        {
                            int dist = spawnLocs[i].distanceSquaredTo(rc.getLocation());
                            if (dist < minDist)
                            {
                                minDist = dist;
                                closestSpawnLoc = spawnLocs[i];
                            }
                        }
                        Pathing.moveTowardsFlee(rc, closestSpawnLoc, 13);
                        rc.setIndicatorString("Fleeing with Flag towards " + closestSpawnLoc.toString());
                        continue;
                    }

                    //If an enemy flag is close, go for it regardless of role
                    FlagInfo[] enemyFlags = rc.senseNearbyFlags(-1, rc.getTeam().opponent());
                    if (enemyFlags.length > 0)
                    {
                        Pathing.moveTowardsFilling(rc, enemyFlags[0].getLocation());
                    }

                    //For now on, all role specific movement is here
                    switch (currentRole)
                    {
                        case Attacker:
                            Attacker.runAttacker(rc);
                            rc.setIndicatorString("Attacker");
                            break;
                        case Defender:
                            Defender.runDefender(rc);
                            rc.setIndicatorString("Defender");
                            break;
                        case Healer:
                            Healer.runHealer(rc);
                            rc.setIndicatorString("Healer");
                            break;
                        case Builder:
                            Builder.runBuilder(rc);
                            rc.setIndicatorString("Builder");
                            break;
                        default:
                            Normal.runNormal(rc);
                            rc.setIndicatorString("Not Specialized");
                            break;
                    }
                }

            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println("GameActionException");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println("Exception");
                e.printStackTrace();
                rc.resign();

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
