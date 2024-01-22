package v21;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.ArrayList;

public class Exploration {

    //If we have no particular target, we need to explore.

    private static ArrayList<MapLocation> exploredLocations = new ArrayList<>();
    private static MapLocation currentExploreLocation = null;
    private static int minDistToExplore = 9999;
    private static final int MAX_TIME_NOT_STRICTLY_IMPROVING = 10;
    private static int timeNotImproving = 0;

    //TODO: BUGGY
    private static void generateNewExploreTarget(RobotController rc)
    {
        if (currentExploreLocation != null)
        {
            exploredLocations.add(currentExploreLocation);
            currentExploreLocation = null;
        }

        for (int i = 0; i < 3; i++)
        {
            //Generate a point 7 units away from the current position using trig
            int angleDegrees = SharedVariables.rng.nextInt(360);
            int x = (int) (rc.getLocation().x + 7*Math.sin(angleDegrees));
            int y = (int) (rc.getLocation().x + 7*Math.cos(angleDegrees));
            MapLocation newLoc = new MapLocation(x,y);
            if (!rc.onTheMap(newLoc)) //Don't count locations outside the map
            {
                continue;
            }
            //If the new location is too close to an existing explored location, skip it
            for (int j = 0; j < exploredLocations.size(); j++)
            {
                if (exploredLocations.get(j).isWithinDistanceSquared(newLoc, 40))
                {
                    newLoc = null;
                    break;
                }
            }
            if (newLoc != null) {
                currentExploreLocation = newLoc;
                break;
            }
        }
        if (currentExploreLocation != null)
        {
            return;
        }

        //Try again with further points
        for (int i = 0; i < 3; i++)
        {
            //Generate a point 7 units away from the current position using trig
            int angleDegrees = SharedVariables.rng.nextInt(360);
            int x = (int) (rc.getLocation().x + 13*Math.sin(angleDegrees));
            int y = (int) (rc.getLocation().x + 13*Math.cos(angleDegrees));
            MapLocation newLoc = new MapLocation(x,y);
            if (!rc.onTheMap(newLoc)) //Don't count locations outside the map
            {
                continue;
            }
            //If the new location is too close to an existing explored location, skip it
            for (int j = 0; j < exploredLocations.size(); j++)
            {
                if (exploredLocations.get(j).isWithinDistanceSquared(newLoc, 40))
                {
                    newLoc = null;
                    break;
                }
            }
            if (newLoc != null) {
                currentExploreLocation = newLoc;
                break;
            }
        }
        if (currentExploreLocation != null)
        {
            return;
        }

        //Try once more with an even further point
        for (int i = 0; i < 1; i++)
        {
            //Generate a point 7 units away from the current position using trig
            int angleDegrees = SharedVariables.rng.nextInt(360);
            int x = (int) (rc.getLocation().x + 20*Math.sin(angleDegrees));
            int y = (int) (rc.getLocation().x + 20*Math.cos(angleDegrees));
            MapLocation newLoc = new MapLocation(x,y);
            if (!rc.onTheMap(newLoc)) //Don't count locations outside the map
            {
                continue;
            }
            //If the new location is too close to an existing explored location, skip it
            for (int j = 0; j < exploredLocations.size(); j++)
            {
                if (exploredLocations.get(j).isWithinDistanceSquared(newLoc, 40))
                {
                    newLoc = null;
                    break;
                }
            }
            if (newLoc != null) {
                currentExploreLocation = newLoc;
                break;
            }
        }
    }

    public static void explore(RobotController rc) throws GameActionException {
        //If we have an explore target, recalculate minDistToExplore
        if (currentExploreLocation != null && currentExploreLocation.distanceSquaredTo(rc.getLocation()) < minDistToExplore)
        {
            minDistToExplore = currentExploreLocation.distanceSquaredTo(rc.getLocation());
            timeNotImproving = 0;
        }
        else if (currentExploreLocation != null)
        {
            timeNotImproving++;
        }

        //If we don't have an explore target, timeNotImproving is too high, or we are at our explore target, generate a new one
        if (currentExploreLocation == null || timeNotImproving >= MAX_TIME_NOT_STRICTLY_IMPROVING || rc.getLocation().equals(currentExploreLocation))
        {
            generateNewExploreTarget(rc);
            timeNotImproving = 0;
            minDistToExplore = 9999;
        }

        if (currentExploreLocation != null)
        {
            Pathing.moveTowards(rc, currentExploreLocation);
        }
        else
        {
            //Randomly walk
            int randomStart = SharedVariables.rng.nextInt(100);
            for (int i = randomStart; i < Constants.directions.length + randomStart; i++)
            {
                if (rc.canMove(Constants.directions[i % Constants.directions.length]))
                {
                    rc.move(Constants.directions[i % Constants.directions.length]);
                    break;
                }
            }
        }
    }
}
