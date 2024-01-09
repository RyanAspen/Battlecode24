package v6;

/*
    This file is meant for macro-level strategy. For example, decide whether to specialize in a
    particular area, whether to mainly be a defender or attacker, etc.
 */

import battlecode.common.GameActionException;
import battlecode.common.RobotController;
import battlecode.common.SkillType;

enum Role {
    Normal,
    Attacker,
    Defender,
    Builder,
    Healer
}

public class Strategy {

    private static Role currentRole = Role.Normal;
    private static final int MIN_ATTACK_EXP_TO_SPECIALIZE = 40;
    private static final int MIN_BUILD_EXP_TO_SPECIALIZE = 10;
    private static final int MIN_HEAL_EXP_TO_SPECIALIZE = 20;
    private static final int MIN_DEFENDERS = 15;
    private static final int MIN_BUILDERS = 0;

    private static void changeRole(RobotController rc, Role role) throws GameActionException {
        if (role != currentRole)
        {
            Communication.reportRoleSwitch(rc, currentRole, role);
            currentRole = role;

        }
    }

    public static Role getRole(RobotController rc) throws GameActionException {
        int attackExp = rc.getExperience(SkillType.ATTACK);
        int buildExp = rc.getExperience(SkillType.BUILD);
        int healExp = rc.getExperience(SkillType.HEAL);
        int normalRoleCount = Communication.getRoleCount(rc, Role.Normal);
        int attackerRoleCount = Communication.getRoleCount(rc, Role.Attacker);
        int defenderRoleCount = Communication.getRoleCount(rc, Role.Defender);
        int builderRoleCount = Communication.getRoleCount(rc, Role.Builder);
        int healerRoleCount = Communication.getRoleCount(rc, Role.Healer);
        if (rc.getID() == 10083)
        {
            System.out.println("Normal: " + normalRoleCount +
                    " Attackers: " + attackerRoleCount +
                    " Defenders: " + defenderRoleCount +
                    " Builders: " + builderRoleCount +
                    " Healers: " + healerRoleCount);
        }


        //Only change role from a non-Normal role if global role counts are imbalanced
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
        else
        {
            // If we have more attacking experience than any other experience, the role should be an Attacker or Defender
            if (attackExp > buildExp && attackExp > healExp && attackExp >= MIN_ATTACK_EXP_TO_SPECIALIZE)
            {
                changeRole(rc, Role.Attacker);
            }
            // If we have more healing experience than any other experience, the role should be a Healer
            else if (healExp > attackExp && healExp > buildExp && healExp >= MIN_HEAL_EXP_TO_SPECIALIZE)
            {
                changeRole(rc, Role.Healer);
            }
            // If we have more building experience than any other experience, the role should be a Builder
            else if (buildExp > attackExp && buildExp > healExp && buildExp >= MIN_BUILD_EXP_TO_SPECIALIZE)
            {
                changeRole(rc, Role.Builder);
            }
            // At the beginning, we have no exp, so we should stay generalist until we get enough exp
        }
        return currentRole;
    }
}
