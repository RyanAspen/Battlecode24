package v22;

import battlecode.common.MapLocation;

import java.util.Random;

enum Role {
    Attacker,
    Defender
}
public class SharedVariables
{
    public static Role currentRole = null;
    public static Random rng = null;
    public static int spawnRound = 0;
    public static MapLocation flagToProtect = null;
    public static int flagIdToProtect = -1;
    public static FlagStatus[] flagStatuses = new FlagStatus[0];
    public static boolean verbose = false;

    public static MapLocation goalDestination = null;
    public static int flagIdHeld = -1;
}
