package linksawakening.world.entity;

/** Shared story slot state, independent of behavior dispatch and construction order. */
final class EntityStoryState {
    private boolean shouldGetLostInMysteriousWoods;
    private final int[] kidPlayTimer = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidPlayState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidZSpeed = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] kidBall = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private int kidHintIndex;
    private final int[] kidKidnapState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidKidnapDelay = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidSpeedXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] kidSpeedYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardDirection = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] richardInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private boolean playerHasToadstool;
    private int playerMagicPowderCount;
    private boolean playerHasTailKey;
    private int houseTarinFlag;
    private int houseTradeItem;
    private int houseInstrument3Flags;
    private int houseWalrusStatus;
    private final OwlEventState owl = new OwlEventState();

    boolean readShouldGetLostInMysteriousWoods() {
        return shouldGetLostInMysteriousWoods;
    }

    boolean writeShouldGetLostInMysteriousWoods(boolean value) {
        return shouldGetLostInMysteriousWoods = value;
    }

    int readKidPlayTimer(int slot) {
        return kidPlayTimer[slot];
    }

    int writeKidPlayTimer(int slot, int value) {
        return kidPlayTimer[slot] = value;
    }

    int[] storageKidPlayTimer() {
        return kidPlayTimer;
    }

    int preKidPlayTimer(int slot, int delta) {
        return kidPlayTimer[slot] += delta;
    }

    int readKidPlayState(int slot) {
        return kidPlayState[slot];
    }

    int writeKidPlayState(int slot, int value) {
        return kidPlayState[slot] = value;
    }

    int[] storageKidPlayState() {
        return kidPlayState;
    }

    int readKidZSpeed(int slot) {
        return kidZSpeed[slot];
    }

    int writeKidZSpeed(int slot, int value) {
        return kidZSpeed[slot] = value;
    }

    int[] storageKidZSpeed() {
        return kidZSpeed;
    }

    int postKidZSpeed(int slot, int delta) {
        int previous = kidZSpeed[slot];
        kidZSpeed[slot] += delta;
        return previous;
    }

    boolean readKidBall(int slot) {
        return kidBall[slot];
    }

    boolean writeKidBall(int slot, boolean value) {
        return kidBall[slot] = value;
    }

    boolean[] storageKidBall() {
        return kidBall;
    }

    int postKidHintIndex(int delta) {
        int previous = kidHintIndex;
        kidHintIndex += delta;
        return previous;
    }

    int readKidKidnapState(int slot) {
        return kidKidnapState[slot];
    }

    int writeKidKidnapState(int slot, int value) {
        return kidKidnapState[slot] = value;
    }

    int[] storageKidKidnapState() {
        return kidKidnapState;
    }

    int writeKidKidnapDelay(int slot, int value) {
        return kidKidnapDelay[slot] = value;
    }

    int[] storageKidKidnapDelay() {
        return kidKidnapDelay;
    }

    int preKidKidnapDelay(int slot, int delta) {
        return kidKidnapDelay[slot] += delta;
    }

    int writeKidSpeedXAccumulator(int slot, int value) {
        return kidSpeedXAccumulator[slot] = value;
    }

    int[] storageKidSpeedXAccumulator() {
        return kidSpeedXAccumulator;
    }

    int writeKidSpeedYAccumulator(int slot, int value) {
        return kidSpeedYAccumulator[slot] = value;
    }

    int[] storageKidSpeedYAccumulator() {
        return kidSpeedYAccumulator;
    }

    int readRichardState(int slot) {
        return richardState[slot];
    }

    int writeRichardState(int slot, int value) {
        return richardState[slot] = value;
    }

    int readRichardDirection(int slot) {
        return richardDirection[slot];
    }

    int writeRichardDirection(int slot, int value) {
        return richardDirection[slot] = value;
    }

    int[] storageRichardXAccumulator() {
        return richardXAccumulator;
    }

    int postRichardInertia(int slot, int delta) {
        int previous = richardInertia[slot];
        richardInertia[slot] += delta;
        return previous;
    }

    boolean readPlayerHasToadstool() {
        return playerHasToadstool;
    }

    boolean writePlayerHasToadstool(boolean value) {
        return playerHasToadstool = value;
    }

    int readPlayerMagicPowderCount() {
        return playerMagicPowderCount;
    }

    int writePlayerMagicPowderCount(int value) {
        return playerMagicPowderCount = value;
    }

    boolean readPlayerHasTailKey() {
        return playerHasTailKey;
    }

    boolean writePlayerHasTailKey(boolean value) {
        return playerHasTailKey = value;
    }

    int readHouseTarinFlag() {
        return houseTarinFlag;
    }

    int writeHouseTarinFlag(int value) {
        return houseTarinFlag = value;
    }

    int readHouseTradeItem() {
        return houseTradeItem;
    }

    int writeHouseTradeItem(int value) {
        return houseTradeItem = value;
    }

    int readHouseInstrument3Flags() {
        return houseInstrument3Flags;
    }

    int writeHouseInstrument3Flags(int value) {
        return houseInstrument3Flags = value;
    }

    int readHouseWalrusStatus() {
        return houseWalrusStatus;
    }

    int writeHouseWalrusStatus(int value) {
        return houseWalrusStatus = value;
    }

    OwlEventState owlState() {
        return owl;
    }
}
