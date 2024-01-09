package v6;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.RobotInfo;

/*
    This class defines how a bot with the Healer role behaves. They should
    - Stay near other bots, especially flag carriers
    - Prioritize healing flag carriers, but always heal if given the chance

 */
public class Healer {
    public static void runHealer(RobotController rc) throws GameActionException {

        //Linger toward friendly flag carriers if able, healing if needed
        RobotInfo[] allies = rc.senseNearbyRobots(-1, rc.getTeam());
        for (int i = 0; i < allies.length; i++)
        {
            if (allies[i].hasFlag())
            {
                Pathing.lingerTowards(rc, allies[i].getLocation(), 9);
                if (rc.canHeal(allies[i].getLocation()))
                {
                    rc.heal(allies[i].getLocation());
                }
                break;
            }
        }

        //If we haven't healed yet, try to heal the weakest bot
        if (rc.getActionCooldownTurns() < 10)
        {
            int minHealth = 99999;
            MapLocation botLoc = null;
            for (int i = 0; i < allies.length; i++)
            {
                int health = allies[i].getHealth();
                if (health < minHealth)
                {
                    minHealth = health;
                    botLoc = allies[i].getLocation();
                }
            }
            if (botLoc != null && rc.canHeal(botLoc))
            {
                rc.heal(botLoc);
            }
        }

        //Otherwise, attack/move carefully
        Combat.micro(rc);

        //Keep close to other allies (Try going toward the furthest ally that is within view)
        if (rc.getMovementCooldownTurns() < 10 && allies.length > 0)
        {
            int maxDist = -1;
            MapLocation botLoc = null;
            for (int i = 0; i < allies.length; i++)
            {
                int dist = allies[i].getLocation().distanceSquaredTo(rc.getLocation());
                if (dist > maxDist)
                {
                    maxDist = dist;
                    botLoc = allies[i].getLocation();
                }
            }
            Pathing.moveTowards(rc, botLoc);
        }
        //Otherwise, explore if able
        else if (rc.getMovementCooldownTurns() < 10)
        {
            Pathing.moveTowards(rc, Exploration.getExploreTarget(rc));
        }
    }
}
