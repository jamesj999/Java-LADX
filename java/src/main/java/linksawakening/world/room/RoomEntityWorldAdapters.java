package linksawakening.world.room;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

final class RoomEntityWorldAdapters {
    static RoomEntityBackgroundInteraction background(RoomCollisionQueries collision, RoomRuntime loaded) {
        return new RoomEntityBackgroundInteraction() {
            @Override
            public EntityBackgroundCollisionResult probe(RoomEntity entity, int direction, int nextX, int nextY) {
                return collision.entityBackgroundCollisionResult(loaded, entity, direction, nextX, nextY, 0);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown) {
                return collision.entityBackgroundCollisionResult(
                        loaded, entity, direction, nextX, nextY, ignoreHitsCountdown);
            }

            @Override
            public EntityBackgroundCollisionResult probe(
                    RoomEntity entity, int direction, int nextX, int nextY, int ignoreHitsCountdown, int frameCounter) {
                return collision.entityBackgroundCollisionResult(
                        loaded, entity, direction, nextX, nextY, ignoreHitsCountdown, frameCounter);
            }
        };
    }

    static ColorShellWorld colorShell(
            RoomObjectMutations objects,
            RoomEntitySessionState entityFrame,
            TransientVfxSystem transientVfxSystem,
            RoomRuntime loaded) {
        return new ColorShellWorld() {
            @Override
            public int objectAt(RoomEntity entity, int relativeOffset) {
                return objects.colorShellObjectAt(loaded, entity, relativeOffset);
            }

            @Override
            public void writeObject(RoomEntity entity, int objectId) {
                objects.writeColorShellObject(loaded, entity, objectId);
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
