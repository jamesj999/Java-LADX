package linksawakening.world.entity.motion;

/** Focused behavior using the state owned by HookshotChainMotion. */
final class HookshotChainGeometry {
    static HookshotChainMotion.Vector vectorTowardsLink(
            int entityX, int entityY, int entityZ, int linkX, int linkY, int length) {
        int distanceX = HookshotChainMotion.signedByte(linkX - entityX);
        int distanceY = HookshotChainMotion.signedByte(linkY - entityY + entityZ);
        int absoluteX = Math.abs(distanceX);
        int absoluteY = Math.abs(distanceY);
        boolean swapped = absoluteX < absoluteY;
        int smaller = Math.min(absoluteX, absoluteY);
        int larger = Math.max(absoluteX, absoluteY);
        int result = HookshotChainMotion.romDivide(length, smaller, larger);
        int x = swapped ? result : length;
        int y = swapped ? length : result;
        if (distanceX < 0) {
            x = -x;
        }
        if (distanceY < 0) {
            y = -y;
        }
        return new HookshotChainMotion.Vector(x & 0xFF, y & 0xFF);
    }

    static int romDivide(int length, int smallerDistance, int largerDistance) {
        if (length == 0 || largerDistance == 0) {
            return largerDistance == 0 ? length : 0;
        }
        int result = 0;
        int remainder = 0;
        for (int count = 0; count < length; count++) {
            int sum = remainder + smallerDistance;
            if (sum >= largerDistance) {
                sum -= largerDistance;
                result++;
            }
            remainder = sum & 0xFF;
        }
        return result;
    }
}
