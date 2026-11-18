# Non-null collaborators round

Baseline: develop 0476fe58. Every equality/inequality comparison against the null literal is counted using the Java compiler AST (multiple comparisons on one line count separately). Owned main packages only; tests and off-limits packages are excluded. Classifications describe the current contract, including contracts to be removed—not endorsements of nullable collaborators.

| Package | a: collaborator | b: domain | c: boundary | d: lazy | e: leftover |
|---|---:|---:|---:|---:|---:|
| entity | 75 | 45 | 25 | 1 | 1 |
| equipment | 12 | 4 | 2 | 0 | 0 |
| state | 0 | 1 | 0 | 0 | 0 |
| dialog | 0 | 6 | 3 | 1 | 0 |
| ui | 18 | 13 | 0 | 1 | 0 |
| cutscene | 0 | 29 | 0 | 4 | 0 |
| startup | 0 | 0 | 0 | 0 | 0 |
| scene | 0 | 0 | 0 | 0 | 0 |
| physics | 2 | 7 | 0 | 1 | 0 |
| vfx | 0 | 6 | 1 | 0 | 0 |
| audio | 9 | 31 | 1 | 1 | 0 |
| save | 0 | 2 | 1 | 0 | 0 |
| config | 0 | 5 | 0 | 0 | 0 |
| input | 0 | 1 | 0 | 0 | 0 |
| rom | 0 | 0 | 0 | 0 | 0 |
| testing | 0 | 0 | 0 | 0 | 0 |

## Baseline site ledger

| Source | Line | Comparison | Class | Reason |
|---|---:|---|---|---|
| audio/apu/GameBoyApu.java | 78 | `observer == null` | a | Legacy nullable observer configuration normalized to no-op |
| audio/browser/AudioBrowserFrame.java | 184 | `selected != null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 196 | `selected != null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 228 | `track == null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 245 | `effect == null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 255 | `message == null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 290 | `message != null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 297 | `track == null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 303 | `effect == null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 316 | `trackList.getSelectedValue() != null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 324 | `soundEffectList.getSelectedValue() != null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 327 | `message == null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserFrame.java | 95 | `message == null` | b | Optional domain value or absent query result |
| audio/browser/AudioBrowserMain.java | 39 | `player != null` | a | Optional initialized playback collaborator |
| audio/browser/AudioBrowserMain.java | 48 | `stream == null` | a | Optional initialized playback collaborator |
| audio/browser/MusicBrowserModel.java | 17 | `filter == null` | b | Optional domain value or absent query result |
| audio/browser/SoundEffectBrowserModel.java | 18 | `filter == null` | b | Optional domain value or absent query result |
| audio/music/FrameAudioMixer.java | 64 | `currentTrack == null` | b | Optional domain value or absent query result |
| audio/music/FrameAudioMixer.java | 76 | `currentTrack != null` | b | Optional domain value or absent query result |
| audio/music/FrameAudioMixer.java | 85 | `soundEffectPlayer != null` | a | Optional sound-effects collaborator |
| audio/music/MusicCatalog.java | 147 | `previous != null` | b | Optional domain value or absent query result |
| audio/music/MusicCatalog.java | 173 | `track == null` | b | Optional domain value or absent query result |
| audio/music/MusicCatalog.java | 222 | `TRACK_NAMES[id] == null` | b | Optional domain value or absent query result |
| audio/openal/NativePcmAlDevice.java | 70 | `pcmStorage == null` | d | First-use PCM scratch |
| audio/openal/OpenAlMusicPlayer.java | 104 | `currentTrack == null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlMusicPlayer.java | 120 | `soundEffectPlayer != null` | a | Optional sound-effects collaborator |
| audio/openal/OpenAlMusicPlayer.java | 184 | `stream.statusMessage == null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlMusicPlayer.java | 185 | `cleanupFailure == null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlMusicPlayer.java | 233 | `soundEffectPlayer != null` | a | Optional sound-effects collaborator |
| audio/openal/OpenAlMusicPlayer.java | 243 | `cleanupFailure != null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlMusicPlayer.java | 257 | `soundEffectPlayer != null` | a | Optional sound-effects collaborator |
| audio/openal/OpenAlPcmSoundOutput.java | 109 | `pcm == null` | c | PCM submission boundary validates input |
| audio/openal/OpenAlPcmSoundOutput.java | 217 | `device == null` | a | Absent device on headless/unavailable output |
| audio/openal/OpenAlPcmSoundOutput.java | 235 | `message == null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlPcmSoundOutput.java | 81 | `lastFailure == null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlPcmSoundOutput.java | 85 | `device == null` | a | Absent device on headless/unavailable output |
| audio/openal/OpenAlStream.java | 206 | `next == null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlStream.java | 209 | `first == null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlStream.java | 226 | `message == null` | b | Optional domain value or absent query result |
| audio/openal/OpenAlStream.java | 70 | `cleanupFailure != null` | b | Optional domain value or absent query result |
| audio/sfx/SoundEffectPlayer.java | 72 | `currentEffect != null` | b | Optional domain value or absent query result |
| audio/sfx/SoundEffectPlayer.java | 94 | `stream == null` | b | Optional domain value or absent query result |
| config/AppConfig.java | 107 | `stream == null` | b | Optional domain value or absent query result |
| config/AppConfig.java | 138 | `value == null` | b | Optional domain value or absent query result |
| config/AppConfig.java | 157 | `value == null` | b | Optional domain value or absent query result |
| config/AppConfig.java | 169 | `value == null` | b | Optional domain value or absent query result |
| config/AppConfig.java | 186 | `value == null` | b | Optional domain value or absent query result |
| cutscene/CutsceneManager.java | 102 | `frameSnapshot != null` | b | Optional domain value or absent query result |
| cutscene/CutsceneManager.java | 126 | `lineScroll == null` | b | Optional domain value or absent query result |
| cutscene/CutsceneManager.java | 23 | `introSequence != null` | d | Intro created after manager construction |
| cutscene/CutsceneManager.java | 52 | `introSequence != null` | d | Intro created after manager construction |
| cutscene/CutsceneManager.java | 68 | `introSequence != null` | d | Intro created after manager construction |
| cutscene/CutsceneManager.java | 72 | `introSequence == null` | d | Intro created after manager construction |
| cutscene/CutsceneManager.java | 86 | `frameSnapshot != null` | b | Optional domain value or absent query result |
| cutscene/CutsceneManager.java | 90 | `frameSnapshot != null` | b | Optional domain value or absent query result |
| cutscene/CutsceneManager.java | 94 | `frameSnapshot != null` | b | Optional domain value or absent query result |
| cutscene/CutsceneManager.java | 98 | `frameSnapshot != null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 112 | `this.visibleTilemap() == null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 116 | `visibleAttrmap == null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 130 | `visibleWave == null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 220 | `scene == null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 233 | `this.fadeBg() != null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 254 | `fadeObj != null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 78 | `snapshot == null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 80 | `transitionFrame != null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 89 | `this.visibleBg() == null` | b | Optional domain value or absent query result |
| cutscene/IntroFramePublisher.java | 90 | `visibleObj == null` | b | Optional domain value or absent query result |
| cutscene/IntroFrameSnapshot.java | 34 | `lineScrollX == null` | b | Optional domain value or absent query result |
| cutscene/IntroFrameSnapshot.java | 36 | `lineScrollY == null` | b | Optional domain value or absent query result |
| cutscene/IntroFrameSnapshot.java | 37 | `animatedTiles == null` | b | Optional domain value or absent query result |
| cutscene/IntroFrameSnapshot.java | 42 | `lineScrollX == null` | b | Optional domain value or absent query result |
| cutscene/IntroFrameSnapshot.java | 47 | `lineScrollY == null` | b | Optional domain value or absent query result |
| cutscene/IntroFrameSnapshot.java | 52 | `animatedTiles == null` | b | Optional domain value or absent query result |
| cutscene/IntroResources.java | 19 | `spec == null` | b | Optional domain value or absent query result |
| cutscene/IntroResources.java | 28 | `stream == null` | b | Optional domain value or absent query result |
| cutscene/IntroSequence.java | 162 | `frames.pendingSprites() == null` | b | Optional domain value or absent query result |
| cutscene/IntroSequence.java | 222 | `frames.snapshot() == null` | b | Optional domain value or absent query result |
| cutscene/IntroSequence.java | 223 | `lineScroll == null` | b | Optional domain value or absent query result |
| cutscene/IntroSequence.java | 230 | `frames.snapshot() == null` | b | Optional domain value or absent query result |
| cutscene/IntroSequence.java | 231 | `lineScroll == null` | b | Optional domain value or absent query result |
| dialog/DialogController.java | 85 | `text == null` | b | Optional domain value or absent query result |
| dialog/DialogController.java | 91 | `boxPosition == null` | b | Optional domain value or absent query result |
| dialog/DialogFont.java | 33 | `stream == null` | b | Optional domain value or absent query result |
| dialog/DialogFont.java | 43 | `romData == null` | c | Input/table shape validation at construction or query boundary |
| dialog/DialogFont.java | 76 | `glyphTiles[tileIndex] != null` | d | First-use decoded glyph cache |
| dialog/DialogRenderer.java | 27 | `buffer == null` | c | Compatibility render boundary accepts absent input |
| dialog/DialogRenderer.java | 27 | `dialog == null` | c | Compatibility render boundary accepts absent input |
| dialog/DialogRenderer.java | 96 | `suppliedPalette == null` | b | Optional domain value or absent query result |
| dialog/DialogRomLayout.java | 25 | `input == null` | b | Optional domain value or absent query result |
| dialog/DialogText.java | 35 | `playerName == null` | b | Optional domain value or absent query result |
| entity/BossSpriteComposer.java | 69 | `segments == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntityEffectSprites.java | 122 | `arrowVariant.second() != null` | b | Optional domain value or absent query result |
| entity/EntityEffectSprites.java | 165 | `pair.second() == null` | b | Optional domain value or absent query result |
| entity/EntitySpriteCatalog.java | 103 | `overworldRoomStatus != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteCatalog.java | 177 | `overworldRoomStatus != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteCatalog.java | 44 | `romData == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteCatalog.java | 94 | `roomTable == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDecoder.java | 17 | `romData == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 129 | `shape == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 130 | `variants == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 131 | `rectangleVariants == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 132 | `dynamicVariants == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 133 | `pairTileSource == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 152 | `s == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 152 | `variant == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 165 | `s == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 165 | `variant == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 27 | `first == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 39 | `oam == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 58 | `oam == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteDefinition.java | 58 | `tileSource == null` | c | Input/table shape validation at construction or query boundary |
| entity/EntitySpriteGeometry.java | 17 | `body.second() != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteGeometry.java | 54 | `pair.second() != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteGeometry.java | 70 | `pair.second() != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteGeometry.java | 92 | `pair.second() != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1277 | `roomTable == null` | c | Registry lookup boundary rejects missing room table |
| entity/EntitySpriteRegistrationData.java | 1296 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1298 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1300 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1302 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1304 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1306 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1308 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1310 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1312 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1314 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1316 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1318 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1320 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1322 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1324 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 1326 | `result != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteRegistrationData.java | 368 | `roomTable == null` | e | Private selector only called with validated room-table values |
| entity/EntitySpriteSelection.java | 104 | `burningSpriteDefinition != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteSelection.java | 184 | `definition != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteSelection.java | 262 | `definition != null` | b | Optional domain value or absent query result |
| entity/EntitySpriteSelection.java | 268 | `source == null` | b | Optional domain value or absent query result |
| entity/EntitySpriteSelection.java | 273 | `source[i] == null` | b | Optional domain value or absent query result |
| entity/EntitySpriteSelection.java | 77 | `spriteOverrides == null` | b | Optional domain value or absent query result |
| entity/EntitySpriteSelection.java | 83 | `entry.getValue() == null` | c | Sprite-definition input validation |
| entity/EntitySpriteSelection.java | 89 | `spriteOverlays == null` | b | Optional domain value or absent query result |
| entity/EntitySpriteSelection.java | 95 | `entry.getValue() == null` | c | Sprite-definition input validation |
| entity/EntitySpriteStates.java | 181 | `status == null` | c | Input/table shape validation at construction or query boundary |
| entity/LinkAirborne.java | 158 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAirborne.java | 207 | `linkServices.get().collision != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAirborne.java | 231 | `linkServices.get().itemRegistry == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAirborne.java | 320 | `linkServices.get().itemRegistry != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAnimator.java | 131 | `linkServices.get().spriteSheet != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAnimator.java | 133 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAnimator.java | 153 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAnimator.java | 168 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAnimator.java | 176 | `linkServices.get().spriteSheet != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAnimator.java | 189 | `item == null` | b | Optional domain value or absent query result |
| entity/LinkAnimator.java | 224 | `palette == null` | c | Presentation palette argument validation |
| entity/LinkAnimator.java | 250 | `linkServices.get().linkMotionTables == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkAnimator.java | 310 | `this.tarinShieldPalette == null` | b | Optional domain value or absent query result |
| entity/LinkComponentRef.java | 10 | `this.value != null` | d | Assembly binding phase encoded as null |
| entity/LinkControls.java | 21 | `linkServices.get().inputState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkControls.java | 27 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkControls.java | 41 | `linkServices.get().inputConfig == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkControls.java | 41 | `linkServices.get().inputState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDamage.java | 142 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDamage.java | 44 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDamage.java | 61 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDamage.java | 63 | `linkServices.get().itemRegistry != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDash.java | 126 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDash.java | 157 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDash.java | 64 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDash.java | 65 | `linkServices.get().inputState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkDash.java | 66 | `linkServices.get().inputConfig == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEntityEffects.java | 222 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 108 | `linkServices.get().itemRegistry == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 108 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 115 | `a != null` | b | Optional domain value or absent query result |
| entity/LinkEquipment.java | 122 | `b != null` | b | Optional domain value or absent query result |
| entity/LinkEquipment.java | 137 | `linkServices.get().itemRegistry == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 137 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 144 | `a != null` | b | Optional domain value or absent query result |
| entity/LinkEquipment.java | 151 | `b != null` | b | Optional domain value or absent query result |
| entity/LinkEquipment.java | 161 | `linkServices.get().itemRegistry == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 161 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 171 | `linkServices.get().itemRegistry == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 171 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 69 | `linkServices.get().inputState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 70 | `linkServices.get().inputConfig == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkEquipment.java | 71 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkGroundMotion.java | 41 | `linkServices.get().collision != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkGroundMotion.java | 66 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkMotion.java | 103 | `linkServices.get().collision != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkMotion.java | 174 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkMotion.java | 176 | `linkServices.get().itemRegistry != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkMotion.java | 209 | `linkServices.get().itemRegistry == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkMotion.java | 214 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkMotion.java | 242 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkPitFall.java | 114 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkPitFall.java | 122 | `pit != null` | b | Optional domain value or absent query result |
| entity/LinkPitFall.java | 199 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkPitFall.java | 70 | `linkServices.get().collision != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkPitFall.java | 80 | `linkServices.get().collision != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkPitFall.java | 90 | `linkServices.get().collision != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkRecovery.java | 85 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkRoomPhysics.java | 77 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkServices.java | 44 | `romTables == null` | a | Nullable ROM table collaborator |
| entity/LinkServices.java | 45 | `romTables == null` | a | Nullable ROM table collaborator |
| entity/LinkSideView.java | 115 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSideView.java | 131 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSideView.java | 132 | `linkServices.get().inputState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSideView.java | 171 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSideView.java | 93 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSpriteComposer.java | 119 | `linkServices.get().itemRegistry == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSpriteComposer.java | 119 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSpriteComposer.java | 126 | `a != null` | b | Optional domain value or absent query result |
| entity/LinkSpriteComposer.java | 139 | `b != null` | b | Optional domain value or absent query result |
| entity/LinkSpriteComposer.java | 154 | `acquisitionSword != null` | b | Optional domain value or absent query result |
| entity/LinkSpriteComposer.java | 186 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSpriteComposer.java | 195 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSpriteComposer.java | 235 | `linkServices.get().itemRegistry == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSpriteComposer.java | 235 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSpriteComposer.java | 242 | `a != null` | b | Optional domain value or absent query result |
| entity/LinkSpriteComposer.java | 255 | `b != null` | b | Optional domain value or absent query result |
| entity/LinkSpriteComposer.java | 270 | `acquisitionSword != null` | b | Optional domain value or absent query result |
| entity/LinkSpriteComposer.java | 34 | `tile == null` | b | Optional domain value or absent query result |
| entity/LinkSpriteComposer.java | 73 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSpriteComposer.java | 82 | `linkServices.get().playerState == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkStateMachine.java | 284 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkStateMachine.java | 299 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkStateMachine.java | 388 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSwimming.java | 130 | `linkServices.get().itemRegistry != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSwimming.java | 147 | `linkServices.get().playerState != null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSwimming.java | 169 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSwimming.java | 199 | `linkServices.get().inputConfig == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSwimming.java | 211 | `linkServices.get().inputConfig == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSwimming.java | 49 | `linkServices.get().collision == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkSwimming.java | 49 | `linkServices.get().linkMotionTables == null` | a | Nullable Link collaborator used by reduced test construction |
| entity/LinkTunicPalette.java | 17 | `objectPalettes == null` | c | Input/table shape validation at construction or query boundary |
| entity/LinkTunicPalette.java | 22 | `palette == null` | c | Input/table shape validation at construction or query boundary |
| entity/LinkTunicPalette.java | 68 | `source[i] == null` | c | Input/table shape validation at construction or query boundary |
| equipment/EquipmentController.java | 66 | `item == null` | b | Optional domain value or absent query result |
| equipment/ItemRegistry.java | 45 | `sword != null` | b | Optional domain value or absent query result |
| equipment/ItemRegistry.java | 67 | `item == null` | b | Optional domain value or absent query result |
| equipment/ItemSpriteRenderer.java | 12 | `tile == null` | b | Optional domain value or absent query result |
| equipment/MagicRod.java | 123 | `equipmentTables == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/MagicRod.java | 157 | `equipmentTables == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/MagicRod.java | 73 | `romTables == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/MagicRod.java | 73 | `romTables == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/MagicRod.java | 73 | `spriteSheet == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/MagicRod.java | 73 | `tunicPalette == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/MagicRod.java | 76 | `romTables == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/Sword.java | 336 | `swordTables == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/Sword.java | 72 | `romTables == null` | a | ROM-free equipment construction uses absent graphics/table collaborators |
| equipment/SwordPalette.java | 16 | `objectPalettes == null` | c | Input/table shape validation at construction or query boundary |
| equipment/SwordPalette.java | 51 | `row == null` | c | Input/table shape validation at construction or query boundary |
| equipment/SwordRenderer.java | 16 | `romTables == null` | a | Nullable ROM table collaborator |
| equipment/SwordSpin.java | 141 | `swordTables == null` | a | Nullable ROM table collaborator |
| equipment/SwordSpin.java | 153 | `swordTables == null` | a | Nullable ROM table collaborator |
| input/InputConfig.java | 131 | `glfwKey == null` | b | Optional domain value or absent query result |
| physics/OverworldCollision.java | 106 | `roomObjectsArea == null` | b | Optional domain value or absent query result |
| physics/OverworldCollision.java | 115 | `roomObjectsArea == null` | b | Optional domain value or absent query result |
| physics/OverworldCollision.java | 128 | `roomObjectsArea == null` | b | Optional domain value or absent query result |
| physics/OverworldCollision.java | 142 | `roomObjectsArea == null` | b | Optional domain value or absent query result |
| physics/OverworldCollision.java | 228 | `roomObjectsArea == null` | b | Optional domain value or absent query result |
| physics/OverworldCollision.java | 250 | `roomObjectsArea == null` | b | Optional domain value or absent query result |
| physics/OverworldCollision.java | 267 | `sample == null` | d | First-use ground sample cache |
| physics/OverworldCollision.java | 321 | `roomObjectsArea == null` | b | Optional domain value or absent query result |
| physics/OverworldCollision.java | 42 | `romTables == null` | a | Nullable table collaborators in collision fixture construction |
| physics/OverworldCollision.java | 43 | `romTables == null` | a | Nullable table collaborators in collision fixture construction |
| save/SaveRamStore.java | 153 | `path == null` | c | Save path parameter validation |
| save/SaveRamStore.java | 162 | `parent != null` | b | Optional domain value or absent query result |
| save/SaveRamStore.java | 75 | `userHome == null` | b | Optional domain value or absent query result |
| state/PlayerSounds.java | 26 | `sound == null` | b | Optional domain value or absent query result |
| ui/FileMenuAction.java | 22 | `nameBytes == null` | b | Optional domain value or absent query result |
| ui/FileMenuController.java | 150 | `names == null` | b | Optional domain value or absent query result |
| ui/FileMenuController.java | 154 | `names[slot] != null` | b | Optional domain value or absent query result |
| ui/FileMenuFrameSnapshot.java | 60 | `palettes == null` | b | Optional domain value or absent query result |
| ui/FileMenuFrameSnapshot.java | 65 | `palettes[index] == null` | b | Optional domain value or absent query result |
| ui/FileSaveController.java | 60 | `scene == null` | b | Optional domain value or absent query result |
| ui/HudOverlay.java | 109 | `attrmap == null` | b | Optional domain value or absent query result |
| ui/InventoryInstrumentPanel.java | 63 | `paletteScratch == null` | d | First-use palette scratch |
| ui/InventoryMenu.java | 243 | `romData != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 252 | `dungeonPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 260 | `instrumentPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 268 | `roomPalettes != null` | b | Optional domain value or absent query result |
| ui/InventoryMenu.java | 271 | `instrumentPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 275 | `dungeonPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 285 | `instrumentPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 290 | `romData != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 295 | `romData != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 315 | `roomPalettes == null` | b | Optional domain value or absent query result |
| ui/InventoryMenu.java | 319 | `romData != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 328 | `dungeonPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 336 | `instrumentPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 344 | `roomPalettes != null` | b | Optional domain value or absent query result |
| ui/InventoryMenu.java | 347 | `instrumentPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 351 | `dungeonPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 361 | `instrumentPanel != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 366 | `romData != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 371 | `romData != null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 66 | `romData == null` | a | Nullable inventory ROM/panel collaborators |
| ui/InventoryMenu.java | 67 | `romData == null` | a | Nullable inventory ROM/panel collaborators |
| ui/TilemapRenderer.java | 23 | `tileAttrs != null` | b | Optional domain value or absent query result |
| ui/TilemapRenderer.java | 29 | `tile == null` | b | Optional domain value or absent query result |
| ui/TilemapRenderer.java | 88 | `attrs == null` | b | Optional domain value or absent query result |
| vfx/TransientVfxSystem.java | 104 | `slot != null` | b | Optional domain value or absent query result |
| vfx/TransientVfxSystem.java | 123 | `type == null` | c | Effect type parameter validation |
| vfx/TransientVfxSystem.java | 128 | `slots[i] == null` | b | Optional domain value or absent query result |
| vfx/TransientVfxSystem.java | 139 | `slot == null` | b | Optional domain value or absent query result |
| vfx/TransientVfxSystem.java | 157 | `slot != null` | b | Optional domain value or absent query result |
| vfx/TransientVfxSystem.java | 176 | `slot != null` | b | Optional domain value or absent query result |
| vfx/TransientVfxSystem.java | 96 | `slot != null` | b | Optional domain value or absent query result |

## Step 2: required collaborators

Owned main comparisons: **309 → 200**. Link's eight nullable service fields are now non-null. Update, motion, animation, damage and equipment paths no longer test for missing services. `LinkDependencies` validates the complete configuration once; the old constructors adapt nullable arguments at one boundary. `TestLinks.link()` constructs real player/input/registry state and explicit inert data. The former null-dependent test assertions are unchanged.

Explicit configurations: `OverworldCollision.openWorld()`, inert motion targets, animation-only sword data/assets, invisible Link/sword/equipment sprites, `InventoryGraphics.overlayOnly()`, `SoundEffectPlayback.none()` and an unavailable PCM device. These do not fabricate a ROM image. The green ROM-free palette and sword-spin tables preserve the existing fallback values. Native output retains device-failure status and diagnostic values. APU diagnostic subscriptions are mutable registration data in a final registry, with one non-null sink at all times.

The baseline ledger classified `AudioBrowserMain`'s missing resource stream as (a); that site is actually (b), an I/O lookup result. Corrected baseline audio counts are **8/32/1/1/0**, and the true collaborator baseline is **115**. Remaining construction-time nullable adapters are category (c), not nullable stored collaborators.

Production construction was checked in `app/wiring/PlayerWiring`, `EquipmentWiring`, `SubsystemWiring` and `UiWiring`: all pass real, non-null dependencies from validated wiring bundles. The browser passes a real sound-effect engine. No off-limits source was edited.

### Construction hand-off

- `app/wiring/PlayerWiring.java`: use `new Link(LinkDependencies)`; its current six-argument constructor remains a thin adapter.
- `app/wiring/EquipmentWiring.java`: use `Sword.Assets` and the typed constructor; the ROM-table overload remains a thin adapter. Magic Rod's production form already requires complete non-null graphics.
- `app/wiring/SubsystemWiring.java`: use the `(PhysicsTables, CollisionTables)` collision constructor. The aggregate-table form remains compatible.
- `app/wiring/UiWiring.java`: use `InventoryGraphics.fromRom(rom)`; the `Rom` overload remains a thin adapter.

External tests still using Link's compatibility constructors can adopt `testing.TestLinks`:
- `world/entity/RoomEntityLiftThrowRuntimeTest.java`
- `world/room/GameplayRouteScenario.java`
- `world/room/KeyCavernEntranceRouteTest.java`
- `world/room/LibraryTreeCollisionTest.java`
- `world/room/MabeWellRegressionTest.java`
- `world/room/RoomTransitionBoundariesTest.java`
- `world/room/RoomTransitionDoorsTest.java`
- `world/room/WarpEntranceRegressionTest.java`
- `app/MabeCollisionSweepTest.java`
- `app/gameplay/entity/EntityBoundaryBehaviorTest.java`
- `app/gameplay/entity/LinkCommandAdapterTest.java`
- `app/wiring/RoomBombPlacementTest.java`
- `gameplay/EnemyProjectileEventConsumerTest.java`
- `render/DialogEmulatorCapture.java`
- `render/DialogGotItemRenderTest.java`
- `render/GameFrameSceneBuilderTest.java`
- `render/HouseEmulatorCapture.java`
- `render/LinkGrassRenderLayerTest.java`
- `render/LinkScrollVisibilityTest.java`
- `render/MarinWakeUpBedRenderLayerTest.java`
- `render/SceneConstructionFixture.java`
- `render/TarinShieldRenderLayerTest.java`

## Step 3: construction and loading phases

Null comparisons: **200 → 183**. All nine originally classified lazy comparisons are removed. Glyph bytes are decoded at font construction; palette and native PCM scratch start with allocated storage and resize only for capacity changes; collision caches start with immutable empty samples and refresh on value/coordinate changes. Link assembly has an explicit unbound sentinel, rejects use before binding, and binds once.

`IntroPlayback` owns explicit unloaded/running phases. The unloaded phase supplies an empty presentation and no work; a running phase owns its sequence and last snapshot. `CutsceneManager.frameSnapshot()` preserves its nullable external contract at one boundary. Initial intro-frame publication is a construction-time operation rather than a first-call null cache. Two snapshot checks after `IntroSequence` construction were redundant and removed. The original ledger's `IntroFramePublisher.snapshot` check was another initialization check rather than a domain value; it is also gone.

Validation: 2,965 tests, zero failures; no assertions removed. New checks cover unloaded playback without a ROM and read-before-bind rejection.

## Step 4: explicit domain absence

Null comparisons: **183 → 131**. No-room collision uses an identity sentinel and `hasRoom()`; caller-supplied empty buffers remain distinct. VFX slots are preallocated and carry an active bit; clearing/reusing a slot preserves immutable historical snapshots. Intro VBlank maps, wave uploads and palettes have named unlatched sentinels; pending sprites and scene-transition snapshots have presence flags. Track/effect selection has explicit presence state. Tarin's absent presentation palette has a named empty sentinel. Single-column entity sprites expose `hasSecond()`.

Optional snapshot scroll/upload values remain nullable at the existing render boundary, with `hasLineScrollX/hasLineScrollY/hasAnimatedTiles` predicates and documented semantics. A missing scroll map remains different from a supplied empty map. Line sampling can now read immutable snapshot values without cloning an array. Optional text/palette inputs normalize to their existing empty/default values at their boundaries.

Intentional optional values retained: Swing selections, resource/property/map lookup results, exception/cleanup diagnostics, save destinations/backups, alternate sprite definitions, palette rows in legacy frame snapshots, and externally consumed event results. These are domain data rather than services. Save persistence and diagnostic failure state have named predicates; field/record documentation identifies absence. Standard-library query results and argument validation remain checked locally, preserving exception and fallback behavior.

Validation: 2,967 tests, zero failures. Added absence-versus-empty scroll checks and room bind/unbind checks; existing scene, sprite, VFX retention and save tests unchanged.

## Step 5: explicit no-result protocols

Null comparisons: **131 → 90**. Sprite registration families return a shared `NO_MATCH`, distinct from a matched-but-unsupported entity. The private duplicate room-table check is removed. Empty inventory slots contain `EquipmentItem.none()`; owned callers use `ItemRegistry.item()` and no longer branch around missing handlers. Nullable `lookup()` and `activeSword()` remain thin external adapters. Resource sounds use a pending bit without adding an enum member that would break external exhaustive switches. Normal-pit queries return an explicit absent `PitCell` with `present()`; the only caller is owned Link motion.

The final audit also removed impossible tile-null checks: Link/sword sheets populate every tile; signed background indices are always 128–383 and VRAM constructs all 384 tiles. The music-name null check was unreachable after rejecting id zero. Palette validation now precedes copying, removing its duplicate row-null branch. HUD and tile-plane absence normalize once to named no-attribute arrays.

Validation: 2,970 tests, zero failures. Added checks distinguish unmatched from explicitly unsupported recipes and verify empty equipment/unregistration semantics. All former assertions remain.

## Step 6: final null-comparison audit

The initial classification was reviewed against the actual construction and call paths. The corrected baseline below retains all **309** sites; final main sources contain **89**. Categories: (a) optional collaborator, (b) optional domain value/result, (c) public-boundary validation/default or legacy constructor adapter, (d) lazy initialization, (e) redundant leftover. Counts are source comparisons, not runtime branch counts. No nullable collaborator is retained behind a renamed predicate.

| Package | a before → after | b before → after | c before → after | d before → after | e before → after | Total before → after |
|---|---:|---:|---:|---:|---:|---:|
| entity | 75 → 0 | 38 → 3 | 31 → 31 | 1 → 0 | 2 → 0 | 147 → 34 |
| equipment | 12 → 0 | 3 → 0 | 2 → 3 | 0 → 0 | 1 → 0 | 18 → 3 |
| state | 0 → 0 | 1 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 1 → 0 |
| dialog | 0 → 0 | 2 → 2 | 7 → 3 | 1 → 0 | 0 → 0 | 10 → 5 |
| ui | 18 → 0 | 11 → 5 | 0 → 1 | 2 → 0 | 1 → 0 | 32 → 6 |
| cutscene | 0 → 0 | 24 → 8 | 2 → 2 | 5 → 0 | 2 → 0 | 33 → 10 |
| startup | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 |
| scene | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 |
| physics | 2 → 0 | 7 → 0 | 0 → 2 | 1 → 0 | 0 → 0 | 10 → 2 |
| vfx | 0 → 0 | 6 → 0 | 1 → 1 | 0 → 0 | 0 → 0 | 7 → 1 |
| audio | 8 → 0 | 29 → 17 | 3 → 3 | 1 → 0 | 1 → 0 | 42 → 20 |
| save | 0 → 0 | 3 → 3 | 0 → 0 | 0 → 0 | 0 → 0 | 3 → 3 |
| config | 0 → 0 | 5 → 5 | 0 → 0 | 0 → 0 | 0 → 0 | 5 → 5 |
| input | 0 → 0 | 1 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 1 → 0 |
| rom | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 |
| testing | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 | 0 → 0 |
| **Total** | 115 → 0 | 130 → 43 | 46 → 46 | 11 → 0 | 7 → 0 | **309 → 89** |

### Classification corrections

These are corrections to the first-commit ledger, not removed sites or hidden comparisons. The final audit also moved FileSaveController’s scene load into its constructor and made that scene final; its original null check was lazy initialization, not optional scene data.

- `audio/browser/AudioBrowserMain.java:48` `stream == null`: a → b; Resource lookup may be absent.
- `audio/music/MusicCatalog.java:147` `previous != null`: b → c; Reject duplicate/unknown catalog identity.
- `audio/music/MusicCatalog.java:173` `track == null`: b → c; Reject duplicate/unknown catalog identity.
- `audio/music/MusicCatalog.java:222` `TRACK_NAMES[id] == null`: b → e; Only null entry is id zero, already rejected.
- `cutscene/IntroFramePublisher.java:220` `scene == null`: b → c; Validate scene-provider result.
- `cutscene/IntroFramePublisher.java:78` `snapshot == null`: b → d; Construction-time first publication/loading.
- `cutscene/IntroResources.java:19` `spec == null`: b → c; Reject unknown scene identifier at provider boundary.
- `cutscene/IntroSequence.java:222` `frames.snapshot() == null`: b → e; Initial snapshot is already published before construction returns.
- `cutscene/IntroSequence.java:230` `frames.snapshot() == null`: b → e; Initial snapshot is already published before construction returns.
- `dialog/DialogController.java:85` `text == null`: b → c; Public argument default normalization.
- `dialog/DialogController.java:91` `boxPosition == null`: b → c; Public argument default normalization.
- `dialog/DialogRenderer.java:96` `suppliedPalette == null`: b → c; Public palette default normalization.
- `dialog/DialogText.java:35` `playerName == null`: b → c; Public argument default normalization.
- `entity/EntitySpriteCatalog.java:103` `overworldRoomStatus != null`: b → c; Input room-status buffer length validation.
- `entity/EntitySpriteSelection.java:104` `burningSpriteDefinition != null`: b → c; Input definition/map validation.
- `entity/EntitySpriteSelection.java:184` `definition != null`: b → c; Input definition/map validation.
- `entity/EntitySpriteSelection.java:262` `definition != null`: b → c; Input definition/map validation.
- `entity/EntitySpriteSelection.java:77` `spriteOverrides == null`: b → c; Input definition/map validation.
- `entity/EntitySpriteSelection.java:89` `spriteOverlays == null`: b → c; Input definition/map validation.
- `entity/LinkSpriteComposer.java:34` `tile == null`: b → e; Proven populated tile and bounded index.
- `equipment/ItemSpriteRenderer.java:12` `tile == null`: b → e; Proven populated tile and bounded index.
- `save/SaveRamStore.java:153` `path == null`: c → b; Explicit in-memory save destination.
- `ui/FileSaveController.java:60` `scene == null`: b → d; Construction-time first publication/loading.
- `ui/TilemapRenderer.java:29` `tile == null`: b → e; Proven populated tile and bounded index.

### Every remaining comparison

| Source | Line | Comparison | Class | Reason |
|---|---:|---|---|---|
| audio/browser/AudioBrowserFrame.java | 185 | `selected != null` | b | Absent Swing list selection |
| audio/browser/AudioBrowserFrame.java | 197 | `selected != null` | b | Absent Swing list selection |
| audio/browser/AudioBrowserFrame.java | 229 | `track == null` | b | Absent Swing list selection |
| audio/browser/AudioBrowserFrame.java | 246 | `effect == null` | b | Absent Swing list selection |
| audio/browser/AudioBrowserFrame.java | 298 | `track == null` | b | Absent Swing list selection |
| audio/browser/AudioBrowserFrame.java | 304 | `effect == null` | b | Absent Swing list selection |
| audio/browser/AudioBrowserFrame.java | 317 | `trackList.getSelectedValue() != null` | b | Absent Swing list selection |
| audio/browser/AudioBrowserFrame.java | 325 | `soundEffectList.getSelectedValue() != null` | b | Absent Swing list selection |
| audio/browser/AudioBrowserMain.java | 48 | `stream == null` | b | Absent classpath resource lookup |
| audio/music/FrameAudioMixer.java | 32 | `track != null` | b | Track-selection presence bit at setter |
| audio/music/MusicCatalog.java | 147 | `previous != null` | c | Duplicate/unknown catalog identity validation |
| audio/music/MusicCatalog.java | 173 | `track == null` | c | Duplicate/unknown catalog identity validation |
| audio/openal/OpenAlMusicPlayer.java | 193 | `cleanupFailure == null` | b | Optional failure/cleanup diagnostic |
| audio/openal/OpenAlMusicPlayer.java | 249 | `cleanupFailure != null` | b | Optional failure/cleanup diagnostic |
| audio/openal/OpenAlPcmSoundOutput.java | 117 | `pcm == null` | c | PCM argument validation |
| audio/openal/OpenAlPcmSoundOutput.java | 64 | `lastFailure != null` | b | Optional failure/cleanup diagnostic |
| audio/openal/OpenAlStream.java | 207 | `next == null` | b | Optional failure/cleanup diagnostic |
| audio/openal/OpenAlStream.java | 210 | `first == null` | b | Optional failure/cleanup diagnostic |
| audio/openal/OpenAlStream.java | 71 | `cleanupFailure != null` | b | Optional failure/cleanup diagnostic |
| audio/sfx/SoundEffectPlayer.java | 102 | `stream == null` | b | Absent classpath resource lookup |
| config/AppConfig.java | 107 | `stream == null` | b | Absent configuration resource/property |
| config/AppConfig.java | 138 | `value == null` | b | Absent configuration resource/property |
| config/AppConfig.java | 157 | `value == null` | b | Absent configuration resource/property |
| config/AppConfig.java | 169 | `value == null` | b | Absent configuration resource/property |
| config/AppConfig.java | 186 | `value == null` | b | Absent configuration resource/property |
| cutscene/CutsceneManager.java | 109 | `lineScroll == null` | b | Documented optional line-scroll/upload data; nullable renderer compatibility |
| cutscene/IntroFramePublisher.java | 232 | `scene == null` | c | Scene-provider result validation |
| cutscene/IntroFrameSnapshot.java | 62 | `lineScrollX != null` | b | Documented optional line-scroll/upload data; nullable renderer compatibility |
| cutscene/IntroFrameSnapshot.java | 66 | `lineScrollY != null` | b | Documented optional line-scroll/upload data; nullable renderer compatibility |
| cutscene/IntroFrameSnapshot.java | 70 | `animatedTiles != null` | b | Documented optional line-scroll/upload data; nullable renderer compatibility |
| cutscene/IntroFrameSnapshot.java | 80 | `scroll == null` | b | Documented optional line-scroll/upload data; nullable renderer compatibility |
| cutscene/IntroFrameSnapshot.java | 84 | `tiles == null` | b | Documented optional line-scroll/upload data; nullable renderer compatibility |
| cutscene/IntroResources.java | 19 | `spec == null` | c | Unknown scene identifier validation |
| cutscene/IntroResources.java | 28 | `stream == null` | b | Absent classpath resource lookup |
| cutscene/IntroSequence.java | 227 | `lineScroll == null` | b | Documented optional line-scroll/upload data; nullable renderer compatibility |
| dialog/DialogFont.java | 37 | `stream == null` | b | Absent classpath resource lookup |
| dialog/DialogFont.java | 47 | `romData == null` | c | ROM argument validation |
| dialog/DialogRenderer.java | 28 | `buffer == null` | c | Legacy nullable render arguments |
| dialog/DialogRenderer.java | 28 | `dialog == null` | c | Legacy nullable render arguments |
| dialog/DialogRomLayout.java | 25 | `input == null` | b | Absent classpath resource lookup |
| entity/BossSpriteComposer.java | 69 | `segments == null` | c | Construction/query validation or legacy construction adapter |
| entity/EntitySpriteCatalog.java | 46 | `romData == null` | c | Construction/query validation or legacy construction adapter |
| entity/EntitySpriteCatalog.java | 96 | `roomTable == null` | c | Construction/query validation or legacy construction adapter |
| entity/EntitySpriteDecoder.java | 17 | `romData == null` | c | Construction/query validation or legacy construction adapter |
| entity/EntitySpriteDefinition.java | 134 | `shape == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 135 | `variants == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 136 | `rectangleVariants == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 137 | `dynamicVariants == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 138 | `pairTileSource == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 157 | `s == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 157 | `variant == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 170 | `s == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 170 | `variant == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 28 | `second != null` | b | Single-column presence predicate |
| entity/EntitySpriteDefinition.java | 32 | `first == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 44 | `oam == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 63 | `oam == null` | c | Display-list metadata validation |
| entity/EntitySpriteDefinition.java | 63 | `tileSource == null` | c | Display-list metadata validation |
| entity/EntitySpriteRegistrationData.java | 1274 | `roomTable == null` | c | Construction/query validation or legacy construction adapter |
| entity/EntitySpriteSelection.java | 107 | `burningSpriteDefinition != null` | c | Input definition/map validation |
| entity/EntitySpriteSelection.java | 187 | `definition != null` | c | Input definition/map validation |
| entity/EntitySpriteSelection.java | 265 | `definition != null` | c | Input definition/map validation |
| entity/EntitySpriteSelection.java | 271 | `source == null` | b | Optional inherited palette data |
| entity/EntitySpriteSelection.java | 276 | `source[i] == null` | b | Optional inherited palette data |
| entity/EntitySpriteSelection.java | 80 | `spriteOverrides == null` | c | Input definition/map validation |
| entity/EntitySpriteSelection.java | 86 | `entry.getValue() == null` | c | Input definition/map validation |
| entity/EntitySpriteSelection.java | 92 | `spriteOverlays == null` | c | Input definition/map validation |
| entity/EntitySpriteSelection.java | 98 | `entry.getValue() == null` | c | Input definition/map validation |
| entity/EntitySpriteStates.java | 181 | `status == null` | c | Construction/query validation or legacy construction adapter |
| entity/LinkAnimator.java | 206 | `palette == null` | c | Construction/query validation or legacy construction adapter |
| entity/LinkDependencies.java | 50 | `tables == null` | c | Construction/query validation or legacy construction adapter |
| entity/LinkDependencies.java | 51 | `tables == null` | c | Construction/query validation or legacy construction adapter |
| entity/LinkTunicPalette.java | 17 | `objectPalettes == null` | c | Construction/query validation or legacy construction adapter |
| entity/LinkTunicPalette.java | 21 | `palette == null` | c | Construction/query validation or legacy construction adapter |
| equipment/Sword.java | 74 | `romTables == null` | c | Construction/query validation or legacy construction adapter |
| equipment/SwordPalette.java | 16 | `objectPalettes == null` | c | Construction/query validation or legacy construction adapter |
| equipment/SwordPalette.java | 51 | `row == null` | c | Construction/query validation or legacy construction adapter |
| physics/OverworldCollision.java | 58 | `romTables == null` | c | Construction/query validation or legacy construction adapter |
| physics/OverworldCollision.java | 59 | `romTables == null` | c | Construction/query validation or legacy construction adapter |
| save/SaveRamStore.java | 156 | `path != null` | b | Optional save destination, parent path or host property |
| save/SaveRamStore.java | 169 | `parent != null` | b | Optional save destination, parent path or host property |
| save/SaveRamStore.java | 77 | `userHome == null` | b | Optional save destination, parent path or host property |
| ui/FileMenuAction.java | 22 | `nameBytes == null` | b | Absent save-name data normalizes to an empty name |
| ui/FileMenuController.java | 151 | `names == null` | b | Absent save-name data normalizes to an empty name |
| ui/FileMenuController.java | 155 | `names[slot] != null` | b | Absent save-name data normalizes to an empty name |
| ui/FileMenuFrameSnapshot.java | 62 | `palettes == null` | b | Optional inherited palette set/rows |
| ui/FileMenuFrameSnapshot.java | 67 | `palettes[index] == null` | b | Optional inherited palette set/rows |
| ui/InventoryMenu.java | 64 | `romData == null` | c | Legacy ROM constructor adapter |
| vfx/TransientVfxSystem.java | 129 | `type == null` | c | Construction/query validation or legacy construction adapter |

### Compiler-recorded off-limits callers

Read from compiled method references after the full suite (main and test outputs); nested classes are grouped by source file. Constructors below retain their legacy overloads.

`cutscene/CutsceneManager.frameSnapshot`:

- `main/linksawakening/app/presentation/RenderStateAssembler.java`

`entity/Link.<init>`:

- `main/linksawakening/app/wiring/PlayerWiring.java`
- `test/linksawakening/app/MabeCollisionSweepTest.java`
- `test/linksawakening/app/gameplay/entity/EntityBoundaryBehaviorTest.java`
- `test/linksawakening/app/gameplay/entity/LinkCommandAdapterTest.java`
- `test/linksawakening/app/wiring/RoomBombPlacementTest.java`
- `test/linksawakening/gameplay/EnemyProjectileEventConsumerTest.java`
- `test/linksawakening/render/DialogEmulatorCapture.java`
- `test/linksawakening/render/DialogGotItemRenderTest.java`
- `test/linksawakening/render/GameFrameSceneBuilderTest.java`
- `test/linksawakening/render/HouseEmulatorCapture.java`
- `test/linksawakening/render/LinkGrassRenderLayerTest.java`
- `test/linksawakening/render/LinkScrollVisibilityTest.java`
- `test/linksawakening/render/MarinWakeUpBedRenderLayerTest.java`
- `test/linksawakening/render/SceneConstructionFixture.java`
- `test/linksawakening/render/TarinShieldRenderLayerTest.java`
- `test/linksawakening/world/entity/RoomEntityLiftThrowRuntimeTest.java`
- `test/linksawakening/world/room/GameplayRouteScenario.java`
- `test/linksawakening/world/room/KeyCavernEntranceRouteTest.java`
- `test/linksawakening/world/room/LibraryTreeCollisionTest.java`
- `test/linksawakening/world/room/MabeWellRegressionTest.java`
- `test/linksawakening/world/room/RoomTransitionBoundariesTest.java`
- `test/linksawakening/world/room/RoomTransitionDoorsTest.java`
- `test/linksawakening/world/room/WarpEntranceRegressionTest.java`

`equipment/ItemRegistry.lookup`:

- `main/linksawakening/app/gameplay/entity/LinkWriteApplier.java`
- `test/linksawakening/world/room/RoomNavigationFixture.java`

`equipment/Sword.<init>`:

- `main/linksawakening/app/wiring/EquipmentWiring.java`
- `test/linksawakening/world/room/GameplayRouteScenario.java`
- `test/linksawakening/world/room/RoomNavigationFixture.java`

`physics/OverworldCollision.<init>`:

- `main/linksawakening/app/wiring/SubsystemWiring.java`
- `test/linksawakening/app/MabeCollisionSweepTest.java`
- `test/linksawakening/app/MusicDirectorTest.java`
- `test/linksawakening/gameplay/EnemyProjectileEventConsumerTest.java`
- `test/linksawakening/render/DialogEmulatorCapture.java`
- `test/linksawakening/render/HouseEmulatorCapture.java`
- `test/linksawakening/render/LinkGrassRenderLayerTest.java`
- `test/linksawakening/world/entity/SlimeEyeRuntimeTest.java`
- `test/linksawakening/world/entity/TarinRaccoonRuntimeTest.java`
- `test/linksawakening/world/room/CrackedFloorGroundPhysicsTest.java`
- `test/linksawakening/world/room/GameplayRouteScenario.java`
- `test/linksawakening/world/room/KeyCavernEntranceRouteTest.java`
- `test/linksawakening/world/room/LibraryTreeCollisionTest.java`
- `test/linksawakening/world/room/MabeWellRegressionTest.java`
- `test/linksawakening/world/room/OpeningHouseRegressionTest.java`
- `test/linksawakening/world/room/RoomNavigationFixture.java`
- `test/linksawakening/world/room/RoomRenderAllocationTest.java`
- `test/linksawakening/world/room/RoomSessionFloatingItemTest.java`
- `test/linksawakening/world/room/RoomSessionTestSupport.java`
- `test/linksawakening/world/room/RoomTransitionBoundariesTest.java`
- `test/linksawakening/world/room/RoomTransitionDoorsTest.java`
- `test/linksawakening/world/room/RoomTransitionFollowersTest.java`
- `test/linksawakening/world/room/WarpEntranceRegressionTest.java`
- `test/linksawakening/world/room/WellHeartEmulatorTrajectoryTest.java`
- `test/linksawakening/world/room/WorldRomEntryPointsTest.java`

`ui/InventoryMenu.<init>`:

- `main/linksawakening/app/wiring/UiWiring.java`
- `test/linksawakening/render/DialogEmulatorCapture.java`
- `test/linksawakening/render/HouseEmulatorCapture.java`
- `test/linksawakening/render/RenderPipelineGpuUploadsTest.java`
- `test/linksawakening/render/SceneConstructionFixture.java`

### Final allocation verification

All allocation guard tests pass with and without escape analysis. The EA-off argument came only from `/tmp/r7-ea-off.gradle`; no Gradle/build configuration was edited.

| Scenario | EA on B/frame | EA off B/frame |
|---|---:|---:|
| Whole frame: opening dialog | 0 | 0 |
| Whole frame: inventory | 0 | 0 |
| Whole frame: walking with entities | 532.33 | 580.93 |
| Link walking and sword | 0 | 0 |
| Dialog printing | 0 | 0 |
| HUD/inventory submission | 0 | 0 |
| Frame audio | 0 | 0 |
| Reusable render assembly + scene | 0 | 0 |

Walking's remaining whole-frame allocations are outside the changed collaborator setup; its 747 B guard remains unchanged. Owned per-frame measurements remain zero. Full default suite: **2,970 tests, zero failures/errors, one pre-existing skip**, versus 2,960 at the merged baseline. Off-limits source files, fixtures and build files were not edited.

## Step 7: architecture rule

The architecture design rules now require final, constructor-validated collaborators, explicit no-op wiring, non-null test builders, named domain absence, and explicit loading phases. They distinguish mutable diagnostic subscriptions from required services and keep legacy nullable data translations at public boundaries. Cohesion and the no-implementation-inheritance rule remain unchanged.

The final documentation commit was preceded by `LA_ROM`-configured `./gradlew spotlessApply test`: 2,970 tests, zero failures/errors, one pre-existing skip. No off-limits files or fixtures changed across this round.
