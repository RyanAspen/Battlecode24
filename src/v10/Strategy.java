package v10;

/*
    This file is meant for macro-level strategy. For example, decide whether to specialize in a
    particular area, whether to mainly be a defender or attacker, etc.
 */

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.SkillType;

enum Role {
    Normal,
    Attacker,
    Defender
}

public class Strategy {

    private static Role currentRole = Role.Normal;
    private static int MIN_DEFENDERS = 6;
    private static int MIN_ATTACKERS = 15;

    private static void changeRole(RobotController rc, Role role) throws GameActionException {
        if (role != currentRole)
        {
            Communication.reportRoleSwitch(rc, currentRole, role);
            currentRole = role;

        }
    }

    public static void changeStrategy(RobotController rc) throws GameActionException {
        /*
        if (rc.canBuyGlobal(GlobalUpgrade.ACTION))
        {
            rc.buyGlobal(GlobalUpgrade.ACTION);
        }
        if (rc.canBuyGlobal(GlobalUpgrade.CAPTURING))
        {
            rc.buyGlobal(GlobalUpgrade.CAPTURING);
        }
         */
        MapLocation[] friendlyFlags = Communication.getFriendlyFlagLocations();
        MapLocation[] enemyFlags = Communication.getEnemyFlagLocations();
        if (friendlyFlags.length == 3)
        {
            //Offensive Strategy
            MIN_DEFENDERS = 12;
            MIN_ATTACKERS = 38;
        }
        else if (friendlyFlags.length > enemyFlags.length)
        {
            //Defensive Strategy (TURTLE)
            MIN_DEFENDERS = 12;
            MIN_ATTACKERS = 38;
        }
        else if (enemyFlags.length > friendlyFlags.length)
        {
            //Very Offensive Strategy
            MIN_DEFENDERS = 12;
            MIN_ATTACKERS = 38;
        }
    }

    public static Role getRole(RobotController rc) throws GameActionException {
        int attackExp = rc.getExperience(SkillType.ATTACK);
        int buildExp = rc.getExperience(SkillType.BUILD);
        int healExp = rc.getExperience(SkillType.HEAL);
        int normalRoleCount = Communication.getRoleCount(rc, Role.Normal);
        int attackerRoleCount = Communication.getRoleCount(rc, Role.Attacker);
        int defenderRoleCount = Communication.getRoleCount(rc, Role.Defender);
        if (rc.getID() == 10799)
        {
            System.out.println("Normal: " + normalRoleCount +
                    " Attackers: " + attackerRoleCount +
                    " Defenders: " + defenderRoleCount
            );
        }

        boolean canSwitchRole;
        switch (currentRole)
        {
            case Normal:
                canSwitchRole = true;
                break;
            case Attacker:
                canSwitchRole = (attackerRoleCount > MIN_ATTACKERS);
                break;
            case Defender:
                canSwitchRole = (defenderRoleCount > MIN_DEFENDERS);
                break;
            default:
                canSwitchRole = false;
        }

        //Only change role from a non-Normal role if global role counts are imbalanced
        /*
        if (currentRole != Role.Normal)
        {
            // Check if there are enough defenders
            if (defenderRoleCount < MIN_DEFENDERS && currentRole != Role.Builder)
            {
                changeRole(rc, Role.Defender);
            }
            //Offense won't be strong enough unless we have enough attackers
            else if (healerRoleCount >= attackerRoleCount)
            {
                changeRole(rc, Role.Attacker);
            }
            else if (builderRoleCount < MIN_BUILDERS)
            {
                changeRole(rc, Role.Builder);
            }
        }

         */
        //else
        //{
            if (rc.getRoundNum() > 150 && canSwitchRole)
            {
                if (defenderRoleCount < MIN_DEFENDERS)
                {
                    changeRole(rc, Role.Defender);
                }
                else if (attackerRoleCount < MIN_ATTACKERS)
                {
                    changeRole(rc, Role.Attacker);
                }
                else
                {
                    changeRole(rc, Role.Attacker);
                }
            }

        //}
        return currentRole;
    }
}
