package v3;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.List;

class Message {
    public int idx;
    public int value;
    public int turnAdded;

    Message (int idx, int value, int turnAdded) {
        this.idx = idx;
        this.value = value;
        this.turnAdded = turnAdded;
    }
}

class Communication {

    private static final int OUTDATED_TURNS_AMOUNT = 30;
    private static final int AREA_RADIUS = 20;
    private static final int STARTING_ENEMY_IDX = 13;
    private static List<Message> messagesQueue = new ArrayList<>();
    private static MapLocation[] enemyLocations = new MapLocation[0];

    private static MapLocation[] friendlyFlagLocations = new MapLocation[0];

    private static MapLocation[] enemyFlagLocations = new MapLocation[0];

    private static void tryWriteMessages(RobotController rc) throws GameActionException {
        messagesQueue.removeIf(msg -> msg.turnAdded + OUTDATED_TURNS_AMOUNT < RobotPlayer.turnCount);
        // Can always write (0, 0), so just checks are we in range to write
        if (rc.canWriteSharedArray(0, 0)) {
            while (!messagesQueue.isEmpty()) {
                Message msg = messagesQueue.remove(0); // Take from front or back?
                if (rc.canWriteSharedArray(msg.idx, msg.value)) {
                    rc.writeSharedArray(msg.idx, msg.value);
                }
            }
        }
    }

    private static void clearObsoleteEnemies(RobotController rc) {
        for (int i = STARTING_ENEMY_IDX; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
            try {
                MapLocation mapLoc = intToLocation(rc, rc.readSharedArray(i));
                if (mapLoc == null) {
                    continue;
                }
                if (rc.canSenseLocation(mapLoc) && rc.senseNearbyRobots(mapLoc, AREA_RADIUS, rc.getTeam().opponent()).length == 0) {
                    Message msg = new Message(i, locationToInt(rc, null), RobotPlayer.turnCount);
                    messagesQueue.add(msg);
                }
            } catch (GameActionException e) {
                continue;
            }

        }
    }

    private static void reportEnemy(RobotController rc, MapLocation enemy) {
        int slot = -1;
        for (int i = STARTING_ENEMY_IDX; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
            try {
                MapLocation prevEnemy = intToLocation(rc, rc.readSharedArray(i));
                if (prevEnemy == null) {
                    slot = i;
                    break;
                } else if (prevEnemy.distanceSquaredTo(enemy) < AREA_RADIUS) {
                    return;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        if (slot != -1) {
            Message msg = new Message(slot, locationToInt(rc, enemy), RobotPlayer.turnCount);
            messagesQueue.add(msg);
        }
    }

    private static void updateEnemies(RobotController rc) {
        MapLocation[] mapLocations = new MapLocation[50];
        int j = 0;
        for (int i = STARTING_ENEMY_IDX; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
            final int value;
            try {
                value = rc.readSharedArray(i);
                final MapLocation m = intToLocation(rc, value);
                if (m != null) {
                    mapLocations[j] = m;
                    j++;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        enemyLocations = new MapLocation[j];
        System.arraycopy(mapLocations, 0, enemyLocations, 0, j);
    }

    private static void reportFlag(RobotController rc, FlagInfo flag) {
        int slot = -1;
        int offset = 0;
        if (!flag.getTeam().equals(rc.getTeam()))
        {
            offset = 3;
        }
        for (int i = offset; i < 3 + offset; i++) {
            try {
                MapLocation prevFlag = intToLocation(rc, rc.readSharedArray(i));
                if (prevFlag == null) {
                    slot = i;
                    break;
                } else if (prevFlag.distanceSquaredTo(flag.getLocation()) < AREA_RADIUS) {
                    return;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        if (slot != -1) {
            Message msg = new Message(slot, locationToInt(rc, flag.getLocation()), RobotPlayer.turnCount);
            messagesQueue.add(msg);
        }
    }

    //Should probably change/remove this
    private static void reportFlagPriority(RobotController rc, MapLocation flag) {
        int slot = -1;
        int offset = 3;
        for (int i = offset; i < 3 + offset; i++) {
            try {
                MapLocation prevFlag = intToLocation(rc, rc.readSharedArray(i));
                slot = 3 + rc.getID() % 3;
                if (prevFlag != null && prevFlag.distanceSquaredTo(flag) < AREA_RADIUS) {
                    return;
                }
            } catch (GameActionException e) {
                continue;
            }
        }
        if (slot != -1) {
            Message msg = new Message(slot, locationToInt(rc, flag), RobotPlayer.turnCount);
            messagesQueue.add(msg);
        }
    }

    private static void updateFlags(RobotController rc) {
        MapLocation[] mapLocations1 = new MapLocation[3];
        MapLocation[] mapLocations2 = new MapLocation[3];
        int j = 0;
        int k = 0;
        for (int i = 0; i < GameConstants.SHARED_ARRAY_LENGTH; i++) {
            final int value;
            try {
                value = rc.readSharedArray(i);
                final MapLocation m = intToLocation(rc, value);
                if (m != null) {
                    if (i < 3)
                    {
                        mapLocations1[j] = m;
                        j++;
                    }
                    else
                    {
                        mapLocations2[k] = m;
                        k++;
                    }

                }
            } catch (GameActionException e) {
                continue;
            }
        }
        friendlyFlagLocations = new MapLocation[j];
        enemyFlagLocations = new MapLocation[k];
        System.arraycopy(mapLocations1, 0, friendlyFlagLocations, 0, j);
        System.arraycopy(mapLocations2, 0, enemyFlagLocations, 0, k);
    }

    public static MapLocation[] getEnemyLocations()
    {
        return enemyLocations;
    }

    public static MapLocation[] getFriendlyFlagLocations()
    {
        return friendlyFlagLocations;
    }

    public static MapLocation[] getEnemyFlagLocations()
    {
        return enemyFlagLocations;
    }

    public static void updateComms(RobotController rc) throws GameActionException {
        clearObsoleteEnemies(rc);
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), AREA_RADIUS, rc.getTeam());
        for (int i = 0; i < enemies.length; i++)
        {
            reportEnemy(rc, enemies[i].getLocation());
        }
        FlagInfo[] flags = rc.senseNearbyFlags(AREA_RADIUS);
        for (int i = 0; i < flags.length; i++)
        {
            reportFlag(rc, flags[i]);
        }
        MapLocation[] flags2 = rc.senseBroadcastFlagLocations();
        for (int i = 0; i < flags2.length; i++)
        {
            reportFlagPriority(rc, flags2[i]);
        }
        tryWriteMessages(rc);
        updateEnemies(rc);
    }

    private static int locationToInt(RobotController rc, MapLocation m) {
        if (m == null) {
            return 0;
        }
        return 1 + m.x + m.y * rc.getMapWidth();
    }

    private static MapLocation intToLocation(RobotController rc, int m) {
        if (m == 0) {
            return null;
        }
        m--;
        return new MapLocation(m % rc.getMapWidth(), m / rc.getMapWidth());
    }
}