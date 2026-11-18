# Build hardening

Baseline: `84f65c4a`, JDK 21, Gradle wrapper 9.8.1, Error Prone 2.43.0 / plugin 5.0.0. A full `compileJava compileTestJava --rerun-tasks` produced **1,313 main + 154 test warnings**. Counts are diagnostic occurrences, not files.

The gate remains `spotlessApply test` with the contributor-supplied ROM. No gameplay/ROM semantics may change to satisfy a warning. Potential defects below are preserved, not silently corrected.

## Check decisions

| Check | Main baseline | Test baseline | Decision |
|---|---:|---:|---|
| AlreadyChecked | 1 | 0 | FIX redundant color-dungeon guard (branch returned above). |
| ArrayEquals | 5 | 0 | FIX explicitly with identity comparisons: preserve the old Objects.equals(array,array) contract. |
| ArrayHashCode | 6 | 0 | FIX explicitly with identity hashing: preserve existing identity hash semantics. |
| ArrayRecordComponent | 64 | 3 | DISABLE globally: array payloads are intentional ROM/pixel ownership boundaries; no content-key use found. Audit below. |
| ArrayToString | 6 | 0 | FIX with scoped intent: preserve the existing identity-based diagnostic format. |
| AssignmentExpression | 310 | 0 | FIX: separate assignment and returned assigned value without repeating evaluation. |
| BooleanLiteral | 2 | 0 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| CompareToZero | 1 | 0 | FIX with scoped intent: Integer.compare is specified to return -1, 0, 1; this is signed ROM acceleration. |
| DefaultCharset | 0 | 4 | FIX with explicit defaultCharset to preserve existing diagnostic/test encoding. |
| DuplicateBranches | 0 | 2 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| EmptyCatch | 0 | 1 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| EnumOrdinal | 9 | 0 | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| ImmutableEnumChecker | 1 | 0 | FIX with scoped intent: final RomAddress is an immutable value record. |
| InputStreamSlowMultibyteRead | 0 | 1 | FIX test fake consistently with its single-byte contract. |
| InvalidParam | 2 | 0 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| JavaTimeDefaultTimeZone | 1 | 0 | FIX: explicitly request systemDefault, retaining local recording filenames. |
| MissingOverride | 557 | 65 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| NarrowingCompoundAssignment | 0 | 3 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| NonApiType | 0 | 2 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| NotJavadoc | 1 | 0 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| ObjectToString | 10 | 0 | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| OperatorPrecedence | 18 | 8 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| ProtectedMembersInFinalClass | 0 | 5 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| RedundantControlFlow | 1 | 0 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| ReferenceEquality | 25 | 0 | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| RethrowReflectiveOperationExceptionAsLinkageError | 0 | 1 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| ReturnAtTheEndOfVoidFunction | 4 | 0 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| ReturnValueIgnored | 1 | 0 | FIX with scoped intent: advance RNG at ROM-prescribed point even though its byte is discarded. |
| SelfAssignment | 6 | 0 | FIX redundant compact-constructor assignment; preserve signed-byte truncation in LinkSwimming. |
| ShortCircuitBoolean | 1 | 0 | FIX with scoped intent: retain eager ROM accumulator merge. |
| StatementSwitchToExpressionSwitch | 6 | 3 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| StringCaseLocaleUsage | 14 | 1 | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| StringConcatToTextBlock | 2 | 0 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| StringSplitter | 1 | 21 | FIX with scoped intent: retain JDK regex splitting and trailing-empty-field behavior, without adding Guava as a runtime dependency. |
| UnnecessaryLambda | 8 | 1 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| UnnecessaryParentheses | 97 | 3 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| UnusedMethod | 0 | 5 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| UnusedNestedClass | 0 | 2 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| UnusedVariable | 150 | 14 | FIX after auditing initializer/call effects; retain effectful reads and native GL resource cleanup. |
| cast | 0 | 4 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| lossy-conversions | 0 | 3 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| rawtypes | 2 | 0 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| static | 1 | 0 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |
| try | 0 | 2 | FIX: mechanical syntax/documentation/test cleanup; preserve evaluation order and existing semantics. |

Javac `serial` is excluded because Java serialization is not a persistence contract. `processing` is excluded because there are no annotation processors (Error Prone is a compiler plugin). All other javac checks remain enabled. No Error Prone check other than ArrayRecordComponent is disabled globally.

## Risk review

Identity comparisons below are cache generation or explicit sentinel identity. Enum ordinals index private arrays created from the same enum, with no persistence/ROM byte coupling. Existing identity diagnostics and hashes are preserved. Signed narrowing in LinkSwimming is essential byte arithmetic, not a self-assignment. RoomTileLoader already returns for the color dungeon. StalfosAggressiveMotion intentionally advances the random source before its countdown gate. The boolean OR accumulates clinging slot state eagerly. RomAddress consists only of final primitive record fields.

| Site | Check | Existing expression | Disposition |
|---|---|---|---|
| `java/src/main/java/linksawakening/ui/FileMenuAction.java:21` | SelfAssignment | `type = Objects.requireNonNull(type, "type");` | FIX redundant compact-constructor assignment; preserve signed-byte truncation in LinkSwimming. |
| `java/src/main/java/linksawakening/gpu/RoomTileLoader.java:87` | AlreadyChecked | `if (mapId != MAP_COLOR_DUNGEON) {` | FIX redundant color-dungeon guard (branch returned above). |
| `java/src/main/java/linksawakening/input/InputConfig.java:131` | StringCaseLocaleUsage | `int glfwKey = KEY_NAME_TO_CODE.getOrDefault(name.toUpperCase(), UNKNOWN_KEY);` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/ui/FileSaveAction.java:15` | SelfAssignment | `type = Objects.requireNonNull(type, "type");` | FIX redundant compact-constructor assignment; preserve signed-byte truncation in LinkSwimming. |
| `java/src/main/java/linksawakening/physics/OverworldCollision.java:49` | ReferenceEquality | `return this != NONE;` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/entity/LinkTunicPalette.java:33` | InvalidParam | `/** Returns the exact four RGB colors selected by {@code wTunicType}. */` | FIX: ROM register names are code, not Javadoc parameter references. |
| `java/src/main/java/linksawakening/entity/LinkAirborne.java:135` | CompareToZero | `return (speed + Integer.compare(target, speed)) & 0xFF;` | FIX with scoped intent: Integer.compare is specified to return -1, 0, 1; this is signed ROM acceleration. |
| `java/src/main/java/linksawakening/entity/LinkSwimming.java:269` | SelfAssignment | `current = (byte) current;` | FIX redundant compact-constructor assignment; preserve signed-byte truncation in LinkSwimming. |
| `java/src/main/java/linksawakening/entity/LinkSwimming.java:270` | SelfAssignment | `target = (byte) target;` | FIX redundant compact-constructor assignment; preserve signed-byte truncation in LinkSwimming. |
| `java/src/main/java/linksawakening/dialog/DialogController.java:274` | ReferenceEquality | `if (cachedSource == text` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/dialog/DialogController.java:278` | ReferenceEquality | `&& cachedRetainedLine == retainedLine` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:49` | SelfAssignment | `sceneId = Objects.requireNonNull(sceneId, "sceneId");` | FIX redundant compact-constructor assignment; preserve signed-byte truncation in LinkSwimming. |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:50` | SelfAssignment | `substate = Objects.requireNonNull(substate, "substate");` | FIX redundant compact-constructor assignment; preserve signed-byte truncation in LinkSwimming. |
| `java/src/main/java/linksawakening/dialog/DialogTextLayout.java:14` | ReferenceEquality | `if (source == text) return;` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/render/GameFrameState.java:339` | ArrayEquals | `&& Objects.equals(background.tilemap.value, other.background.tilemap.value)` | FIX explicitly with identity comparisons: preserve the old Objects.equals(array,array) contract. |
| `java/src/main/java/linksawakening/render/GameFrameState.java:340` | ArrayEquals | `&& Objects.equals(background.attrmap.value, other.background.attrmap.value)` | FIX explicitly with identity comparisons: preserve the old Objects.equals(array,array) contract. |
| `java/src/main/java/linksawakening/render/GameFrameState.java:341` | ArrayEquals | `&& Objects.equals(background.bgPalettes.value, other.background.bgPalettes.value)` | FIX explicitly with identity comparisons: preserve the old Objects.equals(array,array) contract. |
| `java/src/main/java/linksawakening/render/GameFrameState.java:342` | ArrayEquals | `&& Objects.equals(background.objPalettes.value, other.background.objPalettes.value)` | FIX explicitly with identity comparisons: preserve the old Objects.equals(array,array) contract. |
| `java/src/main/java/linksawakening/render/GameFrameState.java:351` | ArrayEquals | `&& Objects.equals(overworld.transientVfxPalette.value, other.overworld.transientVfxPalette.value)` | FIX explicitly with identity comparisons: preserve the old Objects.equals(array,array) contract. |
| `java/src/main/java/linksawakening/world/entity/EntityRoomInspection.java:612` | ReferenceEquality | `if (entity == previousSource && previousPresentation.spriteVariant() == -1) return previousPresentation;` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/world/entity/EntityRoomLoader.java:39` | ImmutableEnumChecker | `private final RomAddress pointerTable;` | FIX with scoped intent: final RomAddress is an immutable value record. |
| `java/src/main/java/linksawakening/entity/EntitySpriteRecipe.java:16` | ReferenceEquality | `return this != NO_MATCH;` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/entity/EntitySpriteRegistrationData.java:1264` | EnumOrdinal | `recipes[room.ordinal()][map][entity] = select(entity, room, MAPS[map]);` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/entity/EntitySpriteRegistrationData.java:1286` | EnumOrdinal | `return recipes[roomTable.ordinal()][map][entityType];` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/world/entity/EntitySpriteViews.java:36` | EnumOrdinal | `return get(0, type, table.ordinal(), map);` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/world/entity/EntitySpriteViews.java:40` | EnumOrdinal | `return get(1, type, state, status.ordinal());` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/world/entity/EntitySpriteViews.java:48` | EnumOrdinal | `return get(3, state, table.ordinal(), 0);` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/world/entity/EntitySpriteViews.java:52` | EnumOrdinal | `return get(4, table.ordinal(), state, 0);` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/world/entity/event/EntityCombatEvent.java:167` | StringCaseLocaleUsage | `throw new IllegalArgumentException("A sound id requires a " + label.toLowerCase() + " sound channel");` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/world/entity/event/EntityCombatEvent.java:170` | StringCaseLocaleUsage | `throw new IllegalArgumentException("A " + label.toLowerCase() + " sound channel requires a sound id");` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/world/entity/RoomEntityObjectSample.java:22` | ReferenceEquality | `return this != UNSAMPLED;` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/world/room/RoomCollisionQueries.java:215` | ReferenceEquality | `&#124;&#124; previous.entity() != updated` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:106` | ArrayHashCode | `hash = 31 * hash + java.util.Objects.hashCode(objects.roomObjectsArea());` | FIX explicitly with identity hashing: preserve existing identity hash semantics. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:107` | ArrayHashCode | `hash = 31 * hash + java.util.Objects.hashCode(objects.gbcOverlay());` | FIX explicitly with identity hashing: preserve existing identity hash semantics. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:108` | ArrayHashCode | `hash = 31 * hash + java.util.Objects.hashCode(objects.renderValues());` | FIX explicitly with identity hashing: preserve existing identity hash semantics. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:109` | ArrayHashCode | `hash = 31 * hash + java.util.Objects.hashCode(graphics.tileIds());` | FIX explicitly with identity hashing: preserve existing identity hash semantics. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:110` | ArrayHashCode | `hash = 31 * hash + java.util.Objects.hashCode(graphics.tileAttrs());` | FIX explicitly with identity hashing: preserve existing identity hash semantics. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:111` | ArrayHashCode | `hash = 31 * hash + java.util.Objects.hashCode(graphics.palettes());` | FIX explicitly with identity hashing: preserve existing identity hash semantics. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:129` | ObjectToString | `+ ", " + "entities=" + entities + "]";` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:126` | ArrayToString | `+ "tileAttrs=" + graphics.tileAttrs() + ", " + "palettes=" + graphics.palettes() + ", " + "warps="` | FIX with scoped intent: preserve the existing identity-based diagnostic format. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:126` | ArrayToString | `+ "tileAttrs=" + graphics.tileAttrs() + ", " + "palettes=" + graphics.palettes() + ", " + "warps="` | FIX with scoped intent: preserve the existing identity-based diagnostic format. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:125` | ArrayToString | `+ "renderValues=" + objects.renderValues() + ", " + "tileIds=" + graphics.tileIds() + ", "` | FIX with scoped intent: preserve the existing identity-based diagnostic format. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:125` | ArrayToString | `+ "renderValues=" + objects.renderValues() + ", " + "tileIds=" + graphics.tileIds() + ", "` | FIX with scoped intent: preserve the existing identity-based diagnostic format. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:124` | ArrayToString | `+ "roomObjectsArea=" + objects.roomObjectsArea() + ", " + "gbcOverlay=" + objects.gbcOverlay() + ", "` | FIX with scoped intent: preserve the existing identity-based diagnostic format. |
| `java/src/main/java/linksawakening/world/room/LoadedRoom.java:124` | ArrayToString | `+ "roomObjectsArea=" + objects.roomObjectsArea() + ", " + "gbcOverlay=" + objects.gbcOverlay() + ", "` | FIX with scoped intent: preserve the existing identity-based diagnostic format. |
| `java/src/main/java/linksawakening/world/entity/EntitySlotLifecycle.java:409` | ShortCircuitBoolean | `this.motions.get(ZolGelMotion.class, ZolGelMotion::new).readGelClingingToLink() &#124; (clinging));` | FIX with scoped intent: retain eager ROM accumulator merge. |
| `java/src/main/java/linksawakening/world/entity/EntityPickupController.java:41` | ReferenceEquality | `if (previous.entity() != entity &#124;&#124; previous.hidden() != hidden) {` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/entity/EntitySpriteCatalog.java:61` | InvalidParam | `* {@code wOverworldRoomStatus} table; it is only consulted for overworld` | FIX: ROM register names are code, not Javadoc parameter references. |
| `java/src/main/java/linksawakening/audio/music/AreaMusicResolver.java:86` | StringCaseLocaleUsage | `+ Integer.toHexString(map).toUpperCase());` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/audio/music/AreaMusicResolver.java:95` | StringCaseLocaleUsage | `+ Integer.toHexString(SELECT_MUSIC_BANK).toUpperCase()` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/audio/music/AreaMusicResolver.java:96` | StringCaseLocaleUsage | `+ " address 0x" + Integer.toHexString(address).toUpperCase());` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/audio/music/MusicCatalog.java:229` | StringCaseLocaleUsage | `return "0x" + Integer.toHexString(value).toUpperCase();` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/app/gameplay/entity/EntityInputCache.java:64` | ReferenceEquality | `&#124;&#124; linkInputs.position() != position` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/app/gameplay/entity/EntityInputCache.java:65` | ReferenceEquality | `&#124;&#124; linkInputs.motion() != motion` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/app/gameplay/entity/EntityInputCache.java:131` | ReferenceEquality | `&#124;&#124; playerInputs.progress() != progress` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/app/gameplay/entity/EntityInputCache.java:132` | ReferenceEquality | `&#124;&#124; playerInputs.inventory() != inventory` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/app/gameplay/entity/EntityInputCache.java:133` | ReferenceEquality | `&#124;&#124; playerInputs.levels() != levels` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/app/gameplay/entity/EntityInputCache.java:134` | ReferenceEquality | `&#124;&#124; playerInputs.health() != health) {` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/app/GameSession.java:55` | JavaTimeDefaultTimeZone | `+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))` | FIX: explicitly request systemDefault, retaining local recording filenames. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:124` | ObjectToString | `+ dialogController + ", equipmentController=" + equipmentController + ", link=" + link + "]";` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:124` | ObjectToString | `+ dialogController + ", equipmentController=" + equipmentController + ", link=" + link + "]";` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:124` | ObjectToString | `+ dialogController + ", equipmentController=" + equipmentController + ", link=" + link + "]";` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:123` | ObjectToString | `+ ", playerState=" + playerState + ", inventoryMenu=" + inventoryMenu + ", dialogController="` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:123` | ObjectToString | `+ ", playerState=" + playerState + ", inventoryMenu=" + inventoryMenu + ", dialogController="` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:122` | ObjectToString | `+ transitionController + ", frameCounter=" + frameCounter + ", scrollController=" + scrollController` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:122` | ObjectToString | `+ transitionController + ", frameCounter=" + frameCounter + ", scrollController=" + scrollController` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:121` | ObjectToString | `return "Diagnostics[" + "gpu=" + gpu + ", roomSession=" + roomSession + ", transitionController="` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/app/headless/SessionDiagnosticSnapshot.java:121` | ObjectToString | `return "Diagnostics[" + "gpu=" + gpu + ", roomSession=" + roomSession + ", transitionController="` | FIX with scoped intent: existing diagnostics intentionally show object identities. |
| `java/src/main/java/linksawakening/render/RoomRenderLayer.java:76` | ReferenceEquality | `if (previousRoom != previousSnapshot) {` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/render/StoryLayer.java:31` | ReferenceEquality | `if (kind == Kind.HEART && state.roomInputs.heart != heart) {` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/render/WarpLayers.java:28` | ReferenceEquality | `if (visual != room.warp &#124;&#124; sourceBg != room.palettes &#124;&#124; sourceObj != room.objectPalettes) {` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/render/BackgroundLayers.java:81` | ReferenceEquality | `if (value == capturedMenu) return;` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/render/BackgroundLayers.java:90` | ReferenceEquality | `if (value == capturedIntro) return;` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/render/VfxPoseCache.java:24` | EnumOrdinal | `poses[type.ordinal()] = new List[size];` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/render/VfxPoseCache.java:26` | EnumOrdinal | `poses[type.ordinal()][i] = switch (type) {` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/render/VfxPoseCache.java:50` | EnumOrdinal | `return poses[slot.type().ordinal()][index];` | FIX with scoped intent: private in-memory enum-indexed tables, never serialized ROM values. |
| `java/src/main/java/linksawakening/world/entity/motion/MoldormTailSnapshots.java:29` | ReferenceEquality | `if (first == previous.get(0)` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/world/entity/motion/MoldormTailSnapshots.java:30` | ReferenceEquality | `&& second == previous.get(1)` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/world/entity/motion/MoldormTailSnapshots.java:31` | ReferenceEquality | `&& third == previous.get(2)` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/world/entity/motion/MoldormTailSnapshots.java:32` | ReferenceEquality | `&& fourth == previous.get(3)) return previous;` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/world/entity/motion/SideViewPlatformMotion.java:55` | ReferenceEquality | `if (frame.entity() != entity) {` | FIX with scoped intent: cache generation/sentinel/slot identity is deliberate; value equality changes invalidation semantics. |
| `java/src/main/java/linksawakening/world/entity/motion/StalfosAggressiveMotion.java:24` | ReturnValueIgnored | `randomByteSupplier.getAsInt();` | FIX with scoped intent: advance RNG at ROM-prescribed point even though its byte is discarded. |
| `java/src/main/java/linksawakening/audio/apu/GameBoyApu.java:258` | StringCaseLocaleUsage | `+ Integer.toHexString(register).toUpperCase());` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/audio/music/MusicDriver.java:298` | StringCaseLocaleUsage | `return "0x" + Integer.toHexString(value).toUpperCase();` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/audio/sfx/RomSoundEffectTables.java:54` | StringCaseLocaleUsage | `+ Integer.toHexString(offset).toUpperCase());` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/audio/music/MusicOpcode.java:91` | StringCaseLocaleUsage | `"Unsupported music opcode 0x" + Integer.toHexString(opcode).toUpperCase() + " on channel " + channel);` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/audio/openal/OpenAlStream.java:221` | StringCaseLocaleUsage | `+ Integer.toHexString(error).toUpperCase());` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/gameplay/GameplaySoundEffectMap.java:117` | StringCaseLocaleUsage | `+ " 0x" + Integer.toHexString(id).toUpperCase())));` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/main/java/linksawakening/gameplay/DialogSoundEffectMap.java:46` | StringCaseLocaleUsage | `+ Integer.toHexString(id).toUpperCase())));` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |
| `java/src/test/java/linksawakening/app/host/DesktopHostViewportCheck.java:65` | StringCaseLocaleUsage | `if (System.getProperty("os.name").toLowerCase().contains("mac")) {` | FIX hex case conversions with ROOT; preserve key-parser locale behavior with a suspected-defect suppression. |

## Suspected defects (behavior retained)

* `input/InputConfig.java:134`: key-name uppercasing uses the ambient locale. Under Turkish, `right` can fail to match the ASCII key table. Proposed separate behavior fix: `Locale.ROOT` plus a Turkish-locale regression test. This round leaves parsing unchanged with a scoped suppression and `Suspected defect` comment.

## Array-record audit

Each row is one warned array component. Call-site review found no array-bearing record used as a content key in a map/set. Consumers read components, copy owned buffers or use explicit cache identity. Snapshot equality is not a content-comparison API: GameFrameState retains array identity, and parity tests compare rasterized planes/hashes. Save round trips compare components/bytes. No existing caller relies on equality between independently copied array payloads; replacing record equality with deep equality would add behavior and mutable hash risks. Keep the arrays and disable this one heuristic globally. A future content-equality API must be explicit and tested.

| Component site | Declaration | Usage decision |
|---|---|---|
| `java/src/main/java/linksawakening/ui/FileMenuAction.java:6` | `public record FileMenuAction(Type type, int selectedSlot, int targetSlot, int[] nameBytes) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/render/RenderContext.java:6` | `record RenderContext(byte[] buffer, GPU gpu, OamSink oam) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/save/SaveSlotComponents.java:12` | `int itemA, int itemB, int[] subscreen, int powerBraceletLevel, int shieldLevel, int swordLevel) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/save/SaveSlotState.java:17` | `int[] nameBytes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/save/SaveSlotState.java:23` | `byte[] rawSlot) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/ui/FileMenuFrameSnapshot.java:18` | `int[] nameBytes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/ui/FileMenuFrameSnapshot.java:19` | `int[] tilemap,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/ui/FileMenuFrameSnapshot.java:20` | `int[] attrmap,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/ui/FileMenuFrameSnapshot.java:21` | `int[][] bgPalettes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/ui/FileMenuFrameSnapshot.java:22` | `int[][] objectPalettes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/scene/BackgroundScene.java:3` | `public record BackgroundScene(int[] tilemap, int[] attrmap, int[][] palettes, int[][] objectPalettes) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/scene/BackgroundScene.java:3` | `public record BackgroundScene(int[] tilemap, int[] attrmap, int[][] palettes, int[][] objectPalettes) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/scene/BackgroundScene.java:3` | `public record BackgroundScene(int[] tilemap, int[] attrmap, int[][] palettes, int[][] objectPalettes) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/scene/BackgroundScene.java:3` | `public record BackgroundScene(int[] tilemap, int[] attrmap, int[][] palettes, int[][] objectPalettes) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/ui/InventoryMenu.java:313` | `public record ClosedWindow(int[] tiles, int[] attributes, int[][] palettes, int y) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/ui/InventoryMenu.java:313` | `public record ClosedWindow(int[] tiles, int[] attributes, int[][] palettes, int y) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/ui/InventoryMenu.java:313` | `public record ClosedWindow(int[] tiles, int[] attributes, int[][] palettes, int y) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/app/story/OpeningActions.java:10` | `public record OpeningActions(int[] shieldPalette, DialogBlockers blockers, MusicRequests music) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:18` | `int[] lineScrollX,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:20` | `int[] tilemap,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:21` | `int[] attrmap,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:22` | `int[][] bgPalettes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:23` | `int[][] objPalettes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:26` | `int[] lineScrollY,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroFrameSnapshot.java:27` | `byte[] animatedTiles,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroRomData.java:271` | `public record TitleRow(int tileTargetAddress, int attributeTargetAddress, int[] tileBytes, int[] attributeBytes) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroRomData.java:271` | `public record TitleRow(int tileTargetAddress, int attributeTargetAddress, int[] tileBytes, int[] attributeBytes) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroRomData.java:288` | `public record PaletteBlock(int[][] rows) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroSpriteComposer.java:28` | `int[] shipHeave,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/cutscene/IntroFramePublisher.java:58` | `record PaletteSources(int[] colorModifiers, IntroRomData.PaletteBlock dxFadeInPalette) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/entity/RoomEntitySnapshot.java:432` | `int[] visualYOffsetBySlot,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/entity/event/InstrumentWarpVisual.java:8` | `int[] tiles,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/entity/event/InstrumentWarpVisual.java:9` | `int[] attributes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/entity/event/InstrumentWarpVisual.java:12` | `int[] swirl) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/entity/event/HeartPieceVisual.java:3` | `public record HeartPieceVisual(int pieces, int[] spritePairs) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomRenderSnapshot.java:8` | `int[] tileIds,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomRenderSnapshot.java:9` | `int[] tileAttrs,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomRenderSnapshot.java:10` | `int[][] palettes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/object/BushCutInputs.java:7` | `int[] roomObjectsArea,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/object/BushCutInputs.java:8` | `int[] mutableRenderValues,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/object/BushCutInputs.java:9` | `int[] fallbackOverlay,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/object/BushCutInputs.java:10` | `int[] roomTileIds,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/object/BushCutInputs.java:11` | `int[] roomTileAttrs) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/LoadedRoomSpec.java:11` | `public record Objects(int[] roomObjectsArea, int[] gbcOverlay, int[] renderValues) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/LoadedRoomSpec.java:11` | `public record Objects(int[] roomObjectsArea, int[] gbcOverlay, int[] renderValues) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/LoadedRoomSpec.java:11` | `public record Objects(int[] roomObjectsArea, int[] gbcOverlay, int[] renderValues) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/LoadedRoomSpec.java:13` | `public record Graphics(int[] tileIds, int[] tileAttrs, int[][] palettes) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/LoadedRoomSpec.java:13` | `public record Graphics(int[] tileIds, int[] tileAttrs, int[][] palettes) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/LoadedRoomSpec.java:13` | `public record Graphics(int[] tileIds, int[] tileAttrs, int[][] palettes) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomTilemap.java:3` | `record RoomTilemap(int[] tileIds, int[] tileAttrs, int[] gbcOverlay, int[] renderValues) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomTilemap.java:3` | `record RoomTilemap(int[] tileIds, int[] tileAttrs, int[] gbcOverlay, int[] renderValues) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomTilemap.java:3` | `record RoomTilemap(int[] tileIds, int[] tileAttrs, int[] gbcOverlay, int[] renderValues) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomTilemap.java:3` | `record RoomTilemap(int[] tileIds, int[] tileAttrs, int[] gbcOverlay, int[] renderValues) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomLoadCoordinator.java:376` | `record PreviousPalettes(int[][] background, int[][] objects) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomLoadCoordinator.java:376` | `record PreviousPalettes(int[][] background, int[][] objects) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomPaletteLoader.java:97` | `public record TransitionPalettes(int[][] background, int[][] objects) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomPaletteLoader.java:97` | `public record TransitionPalettes(int[][] background, int[][] objects) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/IndoorRoomLoadInputs.java:6` | `int[][] fallbackPalettes,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/IndoorRoomLoadInputs.java:9` | `byte[] indoorRoomStatus,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomEntityLoadInputs.java:10` | `byte[] roomStatus,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/world/room/RoomObjectParseResult.java:6` | `record RoomObjectParseResult(int[] roomObjectsArea, List<Warp> warps, int shutterDoorMask, int staircaseLocation) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/app/services/VisualServices.java:16` | `int[] newGameTarinShieldPalette,` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/app/services/VisualServices.java:17` | `byte[] indexedDisplayBuffer) {` | Keep array; component/identity consumer, no content-key use |
| `java/src/main/java/linksawakening/app/wiring/RomWiring.java:19` | `record Graphics(BackgroundSceneLoader backgrounds, SceneState scene, byte[] framebuffer) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/test/java/linksawakening/entity/LinkLedgeParityTest.java:181` | `private record Fixture(Link link, byte[] rom, int[] room) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/test/java/linksawakening/entity/LinkLedgeParityTest.java:181` | `private record Fixture(Link link, byte[] rom, int[] room) {}` | Keep array; component/identity consumer, no content-key use |
| `java/src/test/java/linksawakening/world/room/WorldRomEntryPointsTest.java:26` | `private record Entry(Class<?> owner, String method, Class<?>[] parameters) {` | Keep array; component/identity consumer, no content-key use |

## Final results

After B3 the only emitted diagnostics were the audited 64 main / 3 test array components. B4 disables that heuristic and enables `-Werror` for every JavaCompile task, with Error Prone errors no longer downgraded. Final enabled warning counts: **0 main / 0 test**, for every check in the baseline table. `check` includes formatting, both linted compilations and the full suite. `SuppressionReasonTest` enforces an adjacent reason for every source suppression.

Suppression counts below count annotated declarations per check (an annotation with two check names contributes once to each). These are explicit intent exceptions, not global disabling.

| Check | Main | Test |
|---|---:|---:|
| ArrayToString | 1 | 0 |
| CompareToZero | 1 | 0 |
| EnumOrdinal | 8 | 0 |
| ImmutableEnumChecker | 1 | 0 |
| ObjectToString | 2 | 0 |
| ReferenceEquality | 17 | 0 |
| RethrowReflectiveOperationExceptionAsLinkageError | 0 | 1 |
| ReturnValueIgnored | 1 | 0 |
| SelfAssignment | 1 | 0 |
| ShortCircuitBoolean | 1 | 0 |
| StringCaseLocaleUsage | 1 | 0 |
| StringSplitter | 1 | 15 |
| UnnecessaryLambda | 8 | 0 |
| UnusedNestedClass | 0 | 2 |
| UnusedVariable | 0 | 1 |
| try | 0 | 2 |
| unchecked | 3 | 5 |

