package linksawakening.world;

public record RoomBoundaryState(int mapCategory,
                                int roomId,
                                boolean indoorHasSouthEntrance,
                                boolean hasWarps,
                                int linkX,
                                int linkY) {
}
