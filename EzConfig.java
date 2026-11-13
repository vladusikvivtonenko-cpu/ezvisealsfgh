package com.ezvisuals;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Files;
import java.nio.file.Path;

public class EzConfig {
    public static EzConfig I = new EzConfig();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // {accent, background, sidebar}
    public static final String[] THEME_NAMES = {"Blood Moon"};
    public static final int[][] THEMES = {{0xFFFF3158, 0xFF10070D, 0xFF24101A}};

    // Visuals
    public int theme = 0;
    public boolean noFire = true, noShake = true, itemGlow = true;
    // HUD
    public boolean topBar = true, armorHud = true, potionHud = true, potionBeneficialOnly = false, potionBackground = true, cooldownsHud = true,
            totemHelper = true, nearbyPlayers = true, lookSlots = true, fullBridge = false, armorNameTags = true, swimAnimation = false;
    public int potionTimeFormat = 0; // 0 = MM:SS, 1 = seconds
    public float swimAmount = 0.5f;
    public int pictureSlot = 1;
    // FT helper / predictions
    public boolean ftHelper = true, predictions = true, pvpSafe = true;
    public float radDisorient = 10f, radPlast = 5f, radSnowball = 5f, radCrossbow = 3f, radPotion = 4f;
    // Effects
    public int weather = 0; // 0 off, 1 rain, 2 snow, 3 space
    public boolean bloodVisuals = false;
    public float weatherDensity = 100f;
    // Optimize
    public boolean reduceParticles = true, lowEnd = false;
    public float particleKeep = 40f; // percent
    // Binds (GLFW key code, -1 = none)
    public int keyMenu = GLFW.GLFW_KEY_RIGHT_SHIFT, keyZoom = GLFW.GLFW_KEY_C,
            keyHome = -1, keyArena = -1, keyHub = -1, keyBridge = GLFW.GLFW_KEY_B, keyLookSlots = GLFW.GLFW_KEY_L,
            keyTrap = -1, keyDisorient = -1, keyWeb = -1, keyGapple = -1, keyTotem = -1;
    public float zoomFactor = 0.25f;

    public static int accent() { return THEMES[Math.floorMod(I.theme, THEMES.length)][0]; }
    public static int bg() { return THEMES[Math.floorMod(I.theme, THEMES.length)][1]; }
    public static int side() { return THEMES[Math.floorMod(I.theme, THEMES.length)][2]; }

    private static Path path(int slot) {
        return FabricLoader.getInstance().getConfigDir().resolve(slot == 0 ? "ezvisuals.json" : "ezvisuals_" + slot + ".json");
    }

    public static void save(int slot) {
        try { Files.writeString(path(slot), GSON.toJson(I)); } catch (Exception ignored) {}
    }

    public static boolean load(int slot) {
        try {
            Path p = path(slot);
            if (!Files.exists(p)) return false;
            EzConfig c = GSON.fromJson(Files.readString(p), EzConfig.class);
            if (c != null) { I = c; return true; }
        } catch (Exception ignored) {}
        return false;
    }
}
