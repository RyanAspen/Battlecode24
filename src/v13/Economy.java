package v13;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

public class Economy {

    public static int getCrumbsForAttack(RobotController rc) throws GameActionException {
        return (int)(Constants.CRUMB_PROPORTION_ATTACK * Communication.getCrumbsEarned(rc)) - Communication.getCrumbsAttacking(rc);
    }
    public static int getCrumbsForDefense(RobotController rc, MapLocation base) throws GameActionException {
        return (int)(Constants.CRUMB_PROPORTION_DEFENSE_PER_FLAG * Communication.getCrumbsEarned(rc)) - Communication.getCrumbsDefense(rc, base);
    }
    public static int getCrumbsForPathing(RobotController rc) throws GameActionException {
        return (int)(Constants.CRUMB_PROPORTION_PATHING * Communication.getCrumbsEarned(rc)) - Communication.getCrumbsPathing(rc);
    }
    public static int getCrumbsForFree(RobotController rc) throws GameActionException {
        return (int)(Constants.CRUMB_PROPORTION_FREE * Communication.getCrumbsEarned(rc)) - Communication.getCrumbsFree(rc);
    }
    public static boolean canSpendAttack(RobotController rc, int crumbsToSpend) throws GameActionException {
        int crumbsAvailable = getCrumbsForAttack(rc) + getCrumbsForFree(rc);
        return crumbsToSpend <= crumbsAvailable;
    }
    public static boolean canSpendDefense(RobotController rc, int crumbsToSpend, MapLocation base) throws GameActionException {
        int crumbsAvailable = getCrumbsForDefense(rc, base) + getCrumbsForFree(rc);
        return crumbsToSpend <= crumbsAvailable;
    }
    public static boolean canSpendPathing(RobotController rc, int crumbsToSpend) throws GameActionException {
        int crumbsAvailable = getCrumbsForPathing(rc) + getCrumbsForFree(rc);;
        return crumbsToSpend <= crumbsAvailable;
    }

    public static void printBudgets(RobotController rc) throws GameActionException {
        MapLocation[] friendlyFlags = Communication.getFriendlyFlagLocations();
        System.out.println("--------");
        System.out.println("Current Crumbs = " + rc.getCrumbs());
        int fullBudget = 0;
        System.out.println("Attacking Budget = " + getCrumbsForAttack(rc));
        fullBudget += getCrumbsForAttack(rc);
        for (int i = 0; i < friendlyFlags.length; i++)
        {
            System.out.println("Defense Budget for Flag " + (i+1) + " = " + getCrumbsForDefense(rc, friendlyFlags[i]));
            fullBudget += getCrumbsForDefense(rc, friendlyFlags[i]);
        }
        System.out.println("Pathing Budget = " + getCrumbsForPathing(rc));
        fullBudget += getCrumbsForPathing(rc);
        System.out.println("Free Budget = " + getCrumbsForFree(rc));
        fullBudget += getCrumbsForFree(rc);
        System.out.println("Attacking Crumbs Spent = " + Communication.getCrumbsAttacking(rc));
        for (int i = 0; i < friendlyFlags.length; i++)
        {
            System.out.println("Defense Crumbs Spent for Flag " + (i+1) + " = " + Communication.getCrumbsDefense(rc, friendlyFlags[i]));
        }
        System.out.println("Pathing Crumbs Spent = " + Communication.getCrumbsPathing(rc));
        System.out.println("Free Crumbs Spent = " + Communication.getCrumbsFree(rc));
        System.out.println("Total Crumbs Spent = " + Communication.getTotalCrumbsSpent(rc));
        int error = fullBudget - rc.getCrumbs();
        System.out.println("Budget Error = " + error);
    }

}
