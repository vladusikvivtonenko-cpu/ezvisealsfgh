# EzVisuals — Fabric 1.21.4

Client-side Fabric mod source project. Requires Java 21.

## Build

Open this folder in IntelliJ IDEA with the Gradle plugin, or install Gradle 8.10+ and run:

```bash
gradle build
```

The resulting mod JAR is placed in `build/libs/`.

## Controls

- Right Shift: open menu (default; configurable in Binds)
- C: hold to zoom (default; configurable in Binds)
- Bind buttons: click a key field, press a key; Delete/Backspace clears it

## Current features

- Blood Moon is the sole GUI theme (red/pink accent with dark panels)
- HUD: XYZ, FPS, estimated TPS, armor durability percentages, potion timers, cooldowns, totem count and nearby-player summary
- Potion HUD options: MM:SS or seconds, beneficial-only filter, optional background
- No-fire overlay and no hurt-camera shake
- Hotbar sword/pickaxe/axe highlight
- Projectile trajectory preview and configurable FT radius overlays
- PvP-safe command binds for `/home`, `/darena`, `/hub`
- Rain/snow/space lightweight 2D overlay effects and particle reduction
- Three JSON config slots
- Four custom picture resource slots (`pic1.png` to `pic4.png`) and an anime menu illustration

## Important limitations

This is a source project, not a verified compiled release. Java 21 is required; Gradle is not installed in the preparation environment, so a Minecraft compile could not be run here.

Some requested features are visual helpers or partial implementations rather than full gameplay automation: Full Bridge displays a placement guide, Swimming Animation currently provides a configurable HUD indicator rather than changing the player model, and FT Helper recognizes some custom server items by display-name keywords. Exact FantasticTime item IDs/names are needed for reliable server-specific detection. Rain/snow/space are lightweight HUD overlays, not GPU shader programs.

Optional GUI images are located at:

- `src/main/resources/assets/ezvisuals/textures/gui/girl.png`
- `src/main/resources/assets/ezvisuals/textures/gui/pic1.png` through `pic4.png`
