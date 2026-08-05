package linksawakening.world;

/**
 * Per-slot state for the bank-$04 Beamos parent, Link sensor, and bank-$15
 * laser beam handlers.
 */
final class LaserMotion {
    static final int ENTITY_LASER = 0x2A;
    static final int ENTITY_LASER_BEAM = 0x2B;

    /* LaserLinkSensorYSpeeds followed by LaserLinkSensorXSpeeds in bank $04. */
    private static final int[] SENSOR_Y_SPEEDS = {
        0x10, 0x0E, 0x0C, 0x06, 0x00, 0xFA, 0xF4, 0xF2,
        0xF0, 0xF2, 0xF4, 0xFA, 0x00, 0x06, 0x0C, 0x0E
    };
    private static final int[] SENSOR_X_SPEEDS = {
        0x00, 0xFA, 0xF4, 0xF2, 0xF0, 0xF2, 0xF4, 0xFA,
        0x00, 0x06, 0x0C, 0x0E, 0x10, 0x0E, 0x0C, 0x06
    };

    private enum Role {
        NONE,
        PARENT,
        SENSOR,
        BEAM
    }

    private final Role[] roles = new Role[EntityRoomLoader.MAX_ENTITIES];
    private final int[] parentSlot = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] direction = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] inertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] transitionCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] speedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];

    LaserMotion() {
        java.util.Arrays.fill(roles, Role.NONE);
    }

    void initializeForEntity(RoomEntity entity) {
        if (entity.type() == ENTITY_LASER) {
            if (entity.sourceLoadOrder() == -1) {
                // A dynamically spawned $2A is a sensor. The normal runtime
                // path replaces this with the exact parent slot immediately.
                initializeSensor(entity.slot(), 0, 0);
            } else {
                initializeParent(entity.slot());
            }
        } else if (entity.type() == ENTITY_LASER_BEAM) {
            initializeBeam(entity.slot(), 0, 0, 0);
        }
    }

    void initializeParent(int slot) {
        clearState(slot);
        roles[slot] = Role.PARENT;
    }

    void initializeSensor(int slot, int parent, int sensorDirection) {
        clearState(slot);
        roles[slot] = Role.SENSOR;
        parentSlot[slot] = checkedSlot(parent);
        direction[slot] = sensorDirection & 0x0F;
        speedX[slot] = SENSOR_X_SPEEDS[direction[slot]];
        speedY[slot] = SENSOR_Y_SPEEDS[direction[slot]];
    }

    void initializeBeam(int slot, int beamDirection, int newSpeedX, int newSpeedY) {
        clearState(slot);
        roles[slot] = Role.BEAM;
        direction[slot] = checkedLaserDirection(beamDirection);
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
    }

    boolean isParent(int slot) {
        return roles[checkedSlot(slot)] == Role.PARENT;
    }

    boolean isSensor(int slot) {
        return roles[checkedSlot(slot)] == Role.SENSOR;
    }

    boolean isBeam(int slot) {
        return roles[checkedSlot(slot)] == Role.BEAM;
    }

    ParentUpdate advanceParent(RoomEntity entity) {
        int slot = entity.slot();
        if (!isParent(slot)) {
            initializeParent(slot);
        }

        int countdown = transitionCountdown[slot];
        if (countdown != 0) {
            boolean spawnBeam = countdown == 0x10;
            transitionCountdown[slot] = (countdown - 1) & 0xFF;
            return new ParentUpdate(entity, false, spawnBeam);
        }

        inertia[slot] = (inertia[slot] + 1) & 0xFF;
        if ((inertia[slot] & 0x07) != 0) {
            return new ParentUpdate(entity, false, false);
        }

        direction[slot] = (direction[slot] + 1) & 0x0F;
        return new ParentUpdate(withVariant(entity, direction[slot] >>> 1), true, false);
    }

    SensorUpdate advanceSensor(RoomEntity sensor, RoomEntity parent,
                                int linkX, int linkY, int invincibilityCounter) {
        int slot = sensor.slot();
        if (!isSensor(slot)) {
            initializeSensor(slot, parent == null ? 0 : parent.slot(), 0);
        }
        int owner = parentSlot[slot];
        if (parent == null || !parent.loaded() || parent.slot() != owner) {
            return new SensorUpdate(sensor, true, false);
        }

        if (withinWindow(sensor.x(), linkX) && withinWindow(sensor.y(), linkY)) {
            boolean triggerParent = invincibilityCounter == 0
                && transitionCountdown[owner] == 0;
            if (triggerParent) {
                transitionCountdown[owner] = 0x20;
                Vector vector = vectorTowardsLink(parent.x(), parent.y(), parent.z(),
                    linkX, linkY, 0x40);
                speedX[owner] = vector.x();
                speedY[owner] = vector.y();
            }
            return new SensorUpdate(sensor, true, triggerParent);
        }

        // Bank-$04 writes the sensor speed directly to the position table;
        // unlike the beam it does not call AddEntitySpeedToPos.
        int x = (sensor.x() + signedByte(speedX[slot])) & 0xFF;
        if (x >= 0x9C) {
            return new SensorUpdate(sensor, true, false);
        }
        int y = (sensor.y() + signedByte(speedY[slot])) & 0xFF;
        if (y >= 0x78) {
            return new SensorUpdate(sensor, true, false);
        }
        return new SensorUpdate(withPosition(sensor, x, y), false, false);
    }

    BeamUpdate advanceBeam(RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision) {
        int slot = entity.slot();
        if (!isBeam(slot)) {
            initializeBeam(slot, 0, 0, 0);
        }

        int x = addSpeedToPosition(entity.x(), speedX[slot], speedXAccumulator, slot);
        if (backgroundCollision != null && x != entity.x()
            && backgroundCollision.blocks(entity, horizontalDirection(speedX[slot]), x, entity.y())) {
            return new BeamUpdate(entity, true);
        }
        int y = addSpeedToPosition(entity.y(), speedY[slot], speedYAccumulator, slot);
        if (backgroundCollision != null && y != entity.y()
            && backgroundCollision.blocks(entity, verticalDirection(speedY[slot]), x, y)) {
            return new BeamUpdate(entity, true);
        }
        return new BeamUpdate(withPosition(entity, x, y), false);
    }

    void reflectBeam(int slot, int linkDirection) {
        checkedSlot(slot);
        if ((linkDirection & 0x02) == 0) {
            speedX[slot] = negateByte(speedX[slot]);
        } else {
            speedY[slot] = negateByte(speedY[slot]);
        }
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    int direction(int slot) {
        return direction[checkedSlot(slot)];
    }

    int parentTransitionCountdown(int slot) {
        return transitionCountdown[checkedSlot(slot)];
    }

    int parentSlot(int slot) {
        return parentSlot[checkedSlot(slot)];
    }

    int speedX(int slot) {
        return speedX[checkedSlot(slot)];
    }

    int speedY(int slot) {
        return speedY[checkedSlot(slot)];
    }

    void setParentForTest(int slot, int countdown, int newSpeedX, int newSpeedY) {
        initializeParent(slot);
        transitionCountdown[slot] = countdown & 0xFF;
        speedX[slot] = newSpeedX & 0xFF;
        speedY[slot] = newSpeedY & 0xFF;
    }

    void setBeamForTest(int slot, int newSpeedX, int newSpeedY, int beamDirection) {
        initializeBeam(slot, beamDirection, newSpeedX, newSpeedY);
    }

    void clear(int slot) {
        clearState(checkedSlot(slot));
    }

    private void clearState(int slot) {
        roles[slot] = Role.NONE;
        parentSlot[slot] = 0;
        direction[slot] = 0;
        inertia[slot] = 0;
        transitionCountdown[slot] = 0;
        speedX[slot] = 0;
        speedY[slot] = 0;
        speedXAccumulator[slot] = 0;
        speedYAccumulator[slot] = 0;
    }

    private static boolean withinWindow(int entityPosition, int linkPosition) {
        return ((entityPosition - linkPosition + 0x10) & 0xFF) < 0x20;
    }

    private static Vector vectorTowardsLink(int entityX, int entityY, int entityZ,
                                            int linkX, int linkY, int length) {
        int distanceX = signedByte(linkX - entityX);
        int distanceY = signedByte(linkY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean yIsLargerAxis = absoluteY > absoluteX;
        int smallerDistance = Math.min(absoluteX, absoluteY);
        int largerDistance = Math.max(absoluteX, absoluteY);
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (largerDistance == 0 || sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum;
        }

        int x = yIsLargerAxis ? result : length;
        int y = yIsLargerAxis ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new Vector(x & 0xFF, y & 0xFF);
    }

    private static int addSpeedToPosition(int position, int speed, int[] accumulator,
                                           int slot) {
        speed &= 0xFF;
        if (speed == 0) {
            return position & 0xFF;
        }

        int fractionalSum = accumulator[slot] + ((speed << 4) & 0xF0);
        accumulator[slot] = fractionalSum & 0xFF;
        int signedSpeed = signedByte(speed);
        int delta = signedSpeed >> 4;
        if (fractionalSum > 0xFF) {
            delta++;
        }
        return (position + delta) & 0xFF;
    }

    private static int horizontalDirection(int speed) {
        return signedByte(speed) < 0 ? 1 : 0;
    }

    private static int verticalDirection(int speed) {
        return signedByte(speed) < 0 ? 2 : 3;
    }

    private static int negateByte(int value) {
        return (-signedByte(value)) & 0xFF;
    }

    private static int signedByte(int value) {
        int unsigned = value & 0xFF;
        return unsigned < 0x80 ? unsigned : unsigned - 0x100;
    }

    private static int checkedSlot(int slot) {
        if (slot < 0 || slot >= EntityRoomLoader.MAX_ENTITIES) {
            throw new IllegalArgumentException("Entity slot out of range: " + slot);
        }
        return slot;
    }

    private static int checkedLaserDirection(int value) {
        if (value < 0 || value > 0x0F) {
            throw new IllegalArgumentException("Laser direction out of range: " + value);
        }
        return value;
    }

    private static RoomEntity withPosition(RoomEntity entity, int x, int y) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), x, y,
            entity.status(), entity.spriteDefinition(), entity.spriteVariant(),
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    private static RoomEntity withVariant(RoomEntity entity, int variant) {
        return new RoomEntity(entity.slot(), entity.sourceLoadOrder(), entity.type(), entity.x(),
            entity.y(), entity.status(), entity.spriteDefinition(), variant,
            entity.entityFlipAttribute(), entity.spriteTileOffset(), entity.z());
    }

    record ParentUpdate(RoomEntity entity, boolean spawnSensor, boolean spawnBeam) {
    }

    record SensorUpdate(RoomEntity entity, boolean unloaded, boolean triggeredParent) {
    }

    record BeamUpdate(RoomEntity entity, boolean unloaded) {
    }

    private record Vector(int x, int y) {
    }
}
