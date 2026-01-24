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
    public String safariBallItem = "safari:safari_ball";
    public boolean carryOverSafariBalls = false;
    public boolean logoutClearInventory = true;
    public boolean allowMultiplayerSessions = true;

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
    
    // Dimension
    public int dimensionSize = 2000;
    public int coreRadius = 350; 
    public int resetOffsetRange = 100000;
    public int safariSpawnY = 160;
    public int safariSpawnOffsetY = 3;
    public List<String> allowedBiomes = Arrays.asList("safari:safari_biome");
    public double spawnRateMultiplier = 1.5;
    public int safariMinLevel = 5;
    public int safariMaxLevel = 30;
    public int starterBoostRadius = 120;
    public double starterCullChance = 0.45;
    public List<String> starterSpecies = Arrays.asList(
            "bulbasaur",
            "charmander",
            "squirtle",
            "chikorita",
            "cyndaquil",
            "totodile",
            "treecko",
            "torchic",
            "mudkip",
            "turtwig",
            "chimchar",
            "piplup",
            "snivy",
            "tepig",
            "oshawott",
            "chespin",
            "fennekin",
            "froakie",
            "rowlet",
            "litten",
            "popplio",
            "grookey",
            "scorbunny",
            "sobble",
            "sprigatito",
            "fuecoco",
            "quaxly"
    );

    public static SafariConfig get() {
        if (INSTANCE == null) {
            INSTANCE = new SafariConfig(); // Default fallback
        }
        return INSTANCE;
    }

    public static void load() {
        if (currentConfigFile != null) load(currentConfigFile);
    }

    public static void load(File worldDir) {
        File configFile = new File(worldDir, "safari-config.json");
        currentConfigFile = configFile;
        
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
                    || raw.has("spawnStructureId");
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
        if (INSTANCE.maxBallsPurchasable < 0) {
            INSTANCE.maxBallsPurchasable = 0;
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
