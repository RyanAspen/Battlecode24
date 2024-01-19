package v18;

import battlecode.common.Clock;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.util.ArrayList;
import java.util.Random;

public class Exploration {

    //If we have no particular target, we need to explore.

    private static ArrayList<MapLocation> exploredLocations = new ArrayList<>();
    private static MapLocation currentExploreLocation = null;
    private static int timeExploring = 0;
    private static Random rng = null;

    public static MapLocation getExploreTarget(RobotController rc)
    {
        if (rng == null)
        {
            rng = new Random(rc.getID());
        }

        if (currentExploreLocation == null || currentExploreLocation.isWithinDistanceSquared(rc.getLocation(), 20) || timeExploring >= Constants.maxTimeExploring)
        {
            timeExploring = 0;
            if (currentExploreLocation != null)
            {
                if (exploredLocations.size() >= Constants.maxExplorationListLength)
                {
                    exploredLocations.remove(0);
                }
                exploredLocations.add(currentExploreLocation);
            }

            //Generate a new target not close to existing explored locations
            MapLocation newLoc = null;
            while (Clock.getBytecodesLeft() > Constants.maxBytecodeToLeave)
            {
                int x = rng.nextInt(rc.getMapWidth());
                int y = rng.nextInt(rc.getMapHeight());
                newLoc = new MapLocation(x,y);
                boolean tooClose = false;
                for (int i = 0; i < exploredLocations.size(); i++)
                {
                    if (exploredLocations.get(i).isWithinDistanceSquared(newLoc, Constants.minDistFromExistingLocation))
                    {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose)
                {
                    break;
                }
            }
            currentExploreLocation = newLoc;
            return currentExploreLocation;
        }
        else
        {
            timeExploring++;
            return currentExploreLocation;
        }
    }
}
