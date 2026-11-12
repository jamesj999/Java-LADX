package linksawakening.world.room;

import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

final class RoomEntityWorldAdapters {
    static RoomEntityBackgroundInteraction background(java.util.function.Supplier<RoomCollisionQueries> collision) {
        return new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                return collision.get().entityBackgroundCollisionResult(entity, direction, nextX, nextY, 0);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
                return collision
                        .get()
                        .entityBackgroundCollisionResult(entity, direction, nextX, nextY, ignoreHitsCountdown);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                return collision
                        .get()
                        .entityBackgroundCollisionResult(
                                entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter);
            }
        };
    }

    static ColorShellWorld colorShell(
            java.util.function.Supplier<RoomObjectMutations> objects,
            RoomEntityFrameState entityFrame,
            TransientVfxSystem transientVfxSystem) {
        return new ColorShellWorld() {
            @Override
            public int objectAt(RoomEntity entity, int relativeOffset) {
                return objects.get().colorShellObjectAt(entity, relativeOffset);
            }

            @Override
            public void writeObject(RoomEntity entity, int objectId) {
                objects.get().writeColorShellObject(entity, objectId);
            }

            @Override
            public void playJingle(int id) {
                if ((id & 0xFF) == 0x1D) {
                    entityFrame.colorShellSoundSink.play(GameplaySoundEvent.WRONG_ANSWER);
                }
            }

            @Override
            public void playNoise(int id) {
                if ((id & 0xFF) == 0x04) {
                    entityFrame.colorShellSoundSink.play(GameplaySoundEvent.DOOR_UNLOCKED);
                }
            }

            @Override
            public void spawnPoof(int x, int y) {
                if (transientVfxSystem != null) {
                    transientVfxSystem.spawn(TransientVfxType.POOF, x, y);
                }
            }
        };
    }
}
