package linksawakening.world.room;

import java.util.Objects;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.vfx.TransientVfxSystem;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.ColorShellWorld;
import linksawakening.world.entity.EntityBackgroundCollisionResult;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;

final class RoomEntityWorldAdapters {
    static RoomEntityBackgroundInteraction background(RoomCollisionQueries collision, RoomRuntime loaded) {
        return new Background(collision, loaded);
    }

    static ColorShellWorld colorShell(
            RoomObjectMutations objects,
            RoomEntitySessionState entityFrame,
            TransientVfxSystem transientVfxSystem,
            RoomRuntime loaded) {
        return new ShellWorld(objects, entityFrame, transientVfxSystem, loaded);
    }

    private static final class Background implements RoomEntityBackgroundInteraction {
        private final RoomCollisionQueries collision;
        private final RoomRuntime loaded;

        private Background(RoomCollisionQueries collision, RoomRuntime loaded) {
            this.collision = Objects.requireNonNull(collision, "collision");
            this.loaded = Objects.requireNonNull(loaded, "loaded");
        }

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
    }

    private static final class ShellWorld implements ColorShellWorld {
        private final RoomObjectMutations objects;
        private final RoomEntitySessionState entityFrame;
        private final TransientVfxSystem transientVfxSystem;
        private final RoomRuntime loaded;

        private ShellWorld(
                RoomObjectMutations objects,
                RoomEntitySessionState entityFrame,
                TransientVfxSystem transientVfxSystem,
                RoomRuntime loaded) {
            this.objects = Objects.requireNonNull(objects, "objects");
            this.entityFrame = Objects.requireNonNull(entityFrame, "entityFrame");
            this.transientVfxSystem = Objects.requireNonNull(transientVfxSystem, "transientVfxSystem");
            this.loaded = Objects.requireNonNull(loaded, "loaded");
        }

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
                entityFrame.colorShellSoundSink().play(GameplaySoundEvent.WRONG_ANSWER);
            }
        }

        @Override
        public void playNoise(int id) {
            if ((id & 0xFF) == 0x04) {
                entityFrame.colorShellSoundSink().play(GameplaySoundEvent.DOOR_UNLOCKED);
            }
        }

        @Override
        public void spawnPoof(int x, int y) {

            transientVfxSystem.spawn(TransientVfxType.POOF, x, y);
        }
    }
}
