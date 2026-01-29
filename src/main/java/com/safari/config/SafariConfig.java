package com.safari.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class SafariConfig {
    private static SafariConfig INSTANCE;
    private static File currentConfigFile;

    // Session
    public int sessionTimeMinutes = 30;
    public int initialSafariBalls = 25;
    public boolean carryOverSafariBalls = false;

    // Economy
    public int entrancePrice = 2500;
    public int pack16BallsPrice = 400;
    public int pack32BallsPrice = 750;
    public int pack64BallsPrice = 1400;
    public int maxBallsPurchasable = 20;
    public int timePurchaseMinutes = 30;
    public int timePurchasePrice = 1000;
    
    // Capture Rates
    public double commonCatchRate = 0.45;
    public double uncommonCatchRate = 0.18;
    public double rareCatchRate = 0.1;
    public double ultraRareCatchRate = 0.05;
    
    // Dimension
    public int dimensionSize = 2000;
    public boolean forceCustomSpawn = false;
    public double customSpawnX = 0.5;
    public double customSpawnY = 160.0;
    public double customSpawnZ = 0.5;
    public int safariSpawnY = 160;
    public int safariSpawnOffsetY = 3;
    public List<String> allowedBiomes = Arrays.asList("safari:safari_biome");
    public int safariMinLevel = 5;
    public int safariMaxLevel = 30;

    // Entry title
    public String entryTitle = "Safari Warning";
    public String entrySubtitle = "Leaving ends your session and remaining time";
    public int entryTitleFadeInTicks = 10;
    public int entryTitleStayTicks = 60;
    public int entryTitleFadeOutTicks = 10;

    public static SafariConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new SafariConfig(); // Default fallback
        }
        return INSTANCE;
    }

    public static void load() {
        if (currentConfigFile != null) {
            loadFromFile(currentConfigFile);
        }
    }

    public static void load(File worldDir) {
        currentConfigFile = new File(worldDir, "safari-config.json");
        loadFromFile(currentConfigFile);
    }

    private static void loadFromFile(File configFile) {
        if (configFile == null) {
            return;
        }

        if (!configFile.exists()) {
            INSTANCE = new SafariConfig();
            save();
            return;
        }

        try (FileReader reader = new FileReader(configFile)) {
            Gson gson = new Gson();
            JsonObject raw = JsonParser.parseReader(reader).getAsJsonObject();
            boolean shouldRewrite = raw.has("resetTimezone")
                    || raw.has("enableDailyReset")
                    || raw.has("spawnStructureId")
                    || raw.has("starterBoostRadius")
                    || raw.has("starterCullChance")
                    || raw.has("starterSpecies")
                    || raw.has("safariBallItem")
                    || raw.has("logoutClearInventory")
                    || raw.has("allowMultiplayerSessions")
                    || raw.has("coreRadius")
                    || raw.has("resetOffsetRange")
                    || raw.has("spawnRateMultiplier")
                    || !raw.has("entryTitle")
                    || !raw.has("entrySubtitle")
                    || !raw.has("entryTitleFadeInTicks")
                    || !raw.has("entryTitleStayTicks")
                    || !raw.has("entryTitleFadeOutTicks")
                    || !raw.has("forceCustomSpawn")
                    || !raw.has("customSpawnX")
                    || !raw.has("customSpawnY")
                    || !raw.has("customSpawnZ")
                    || !raw.has("guideNpcSpawned")
                    || !raw.has("spawnInitialized");
            INSTANCE = gson.fromJson(raw, SafariConfig.class);
            boolean normalized = normalizeDefaults();
            if (shouldRewrite || normalized) {
                save();
            }
        } catch (IOException e) {
            e.printStackTrace();
            INSTANCE = new SafariConfig();
        }
    }

    private static boolean normalizeDefaults() {
        boolean updated = false;
        if (INSTANCE.sessionTimeMinutes <= 0) {
            INSTANCE.sessionTimeMinutes = 30;
            updated = true;
        }
        if (INSTANCE.timePurchaseMinutes <= 0) {
            INSTANCE.timePurchaseMinutes = 30;
            updated = true;
        }
        if (INSTANCE.timePurchasePrice <= 0) {
            INSTANCE.timePurchasePrice = 1000;
            updated = true;
        }
        if (INSTANCE.allowedBiomes == null || INSTANCE.allowedBiomes.isEmpty()) {
            INSTANCE.allowedBiomes = Arrays.asList("safari:safari_biome");
            updated = true;
        }
        if (INSTANCE.ultraRareCatchRate <= 0) {
            INSTANCE.ultraRareCatchRate = 0.05;
            updated = true;
        }
        if (INSTANCE.maxBallsPurchasable < 0) {
            INSTANCE.maxBallsPurchasable = 0;
            updated = true;
        }
        if (INSTANCE.entryTitle == null) {
            INSTANCE.entryTitle = "Safari Warning";
            updated = true;
        }
        if (INSTANCE.entrySubtitle == null) {
            INSTANCE.entrySubtitle = "Leaving ends your session and remaining time";
            updated = true;
        }
        if (INSTANCE.entryTitleFadeInTicks < 0) {
            INSTANCE.entryTitleFadeInTicks = 10;
            updated = true;
        }
        if (INSTANCE.entryTitleStayTicks < 0) {
            INSTANCE.entryTitleStayTicks = 60;
            updated = true;
        }
        if (INSTANCE.entryTitleFadeOutTicks < 0) {
            INSTANCE.entryTitleFadeOutTicks = 10;
            updated = true;
        }
        return updated;
    }

    public static void save() {
        if (currentConfigFile == null) return;
        try (FileWriter writer = new FileWriter(currentConfigFile)) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            gson.toJson(INSTANCE, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
