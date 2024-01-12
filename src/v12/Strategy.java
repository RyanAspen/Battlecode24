package v12;

/*
    This file is meant for macro-level strategy. For example, decide whether to specialize in a
    particular area, whether to mainly be a defender or attacker, etc.
 */

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.SkillType;

public class Strategy {

    private static void changeRole(RobotController rc, Role role) throws GameActionException {
        if (role != SharedVariables.currentRole)
        {
            Communication.reportRoleSwitch(rc, SharedVariables.currentRole, role);
            SharedVariables.currentRole = role;
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
            SharedVariables.desiredDefenders = Constants.MIN_DEFENDERS_BALANCED;
            SharedVariables.desiredAttackers = Constants.MIN_ATTACKERS_BALANCED;
        }
        else if (friendlyFlags.length > enemyFlags.length)
        {
            //Defensive Strategy (TURTLE)
            SharedVariables.desiredDefenders = Constants.MIN_DEFENDERS_TURTLE;
            SharedVariables.desiredAttackers = Constants.MIN_ATTACKERS_TURTLE;
        }
        else if (enemyFlags.length > friendlyFlags.length)
        {
            //Very Offensive Strategy
            SharedVariables.desiredDefenders = Constants.MIN_DEFENDERS_AGGRESSIVE;
            SharedVariables.desiredAttackers = Constants.MIN_ATTACKERS_AGGRESSIVE;
        }
    }

    public static void updateRole(RobotController rc) throws GameActionException {
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
        switch (SharedVariables.currentRole)
        {
            case Normal:
                canSwitchRole = true;
                break;
            case Attacker:
                canSwitchRole = (attackerRoleCount > SharedVariables.desiredAttackers);
                break;
            case Defender:
                canSwitchRole = (defenderRoleCount > SharedVariables.desiredDefenders);
                break;
            default:
                canSwitchRole = false;
        }
        if (rc.getRoundNum() > 0 && canSwitchRole)
        {
            if (defenderRoleCount < SharedVariables.desiredDefenders)
            {
                changeRole(rc, Role.Defender);
            }
            else if (attackerRoleCount < SharedVariables.desiredAttackers)
            {
                changeRole(rc, Role.Attacker);
            }
            else
            {
                changeRole(rc, Role.Attacker);
            }
        }
    }
}
