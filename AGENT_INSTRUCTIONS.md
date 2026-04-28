# Instructions for Implementation Agent (Next Phase)

## 🎯 Objective
Transition from engine initialization to a functional rendering loop that can display the previously generated atlases and tilemaps.

## 🛠️ Core Tasks

### 1. Implement Engine Entry Point (`src/main.ts`)
- Initialize the `Renderer` with Game Boy resolution (160x144).
- Set up a standard `PIXI.Ticker` loop to call `update(delta)` on all active layers.
- Integrate the `Renderer.view` into the DOM (via Vite/index.html).

### 2. Enhance `TilemapLayer` Implementation
- **Asset Loading**: Refine `loadAssets` to use `PIXI.Assets` for loading the `.png` atlases and matching them with the `.json` metadata.
- **Tile Rendering**: Implement logic to iterate through a tilemap array (once format is finalized) and place `PIXI.Sprite` objects using the UV coordinates from the JSON metadata.
- **Layering**: Ensure `TilemapLayer` correctly implements the Game Boy background layer concept.

### 3. Refine `SpriteLayer` & Animation
- Implement a system to handle sprite animations (frame switching) based on timing or game state.
- Use the `SpriteMetadata` from the generated atlases to define animation sequences.

## 🧪 Verification Requirements
- **Visual Check**: Upon running, the canvas should display a non-empty black screen with at least one tile or sprite from the `src/assets/atlases/` directory.
- **Console Logs**: Ensure no WebGL/PixiJS errors appear in the browser console.
- **Type Safety**: All new engine components must be strictly typed and follow the established pattern in `src/engine/`.

## 📂 Reference Files
- `src/pipeline/extract-sprites.ts`: For understanding how atlas metadata is structured.
- `src/assets/atlases/*.json`: The source of truth for sprite UV coordinates.
