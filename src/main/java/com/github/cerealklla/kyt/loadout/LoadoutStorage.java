package com.github.cerealklla.kyt.loadout;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Global, file-based store of every saved {@link LoadoutRecord} -- one JSON file per Kyt, living
 * outside any world save (under the game instance root) so wiping/recreating a world never loses
 * them, same pattern as Blueprynts' own {@code blueprint.BlueprintStorage}. Flat layout (no
 * Status/Type/Tier nesting -- Kyt loadouts have no such taxonomy): {@code kyt/KytConfigurations/
 * <sanitized name>.json}. Not a cache -- every read/write goes straight to disk.
 */
public final class LoadoutStorage {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String EXTENSION = ".json";

    private static final LoadoutStorage INSTANCE = new LoadoutStorage();

    private final Path root;

    private LoadoutStorage() {
        this.root = FMLPaths.GAMEDIR.get().resolve("kyt").resolve("KytConfigurations");
    }

    public static LoadoutStorage get() {
        return INSTANCE;
    }

    public void save(LoadoutRecord record) {
        delete(record.name());
        try {
            Files.createDirectories(root);
            Path file = root.resolve(fileNameFor(record.name()));
            JsonElement json = LoadoutRecord.CODEC.encodeStart(JsonOps.INSTANCE, record)
                    .getOrThrow(msg -> new IOException("Failed to encode Kyt '" + record.name() + "': " + msg));
            Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save Kyt '" + record.name() + "'", e);
        }
    }

    public Optional<LoadoutRecord> load(String name) {
        Path file = root.resolve(fileNameFor(name));
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        return Optional.of(readRecord(file));
    }

    public List<String> listNames() {
        List<String> names = new ArrayList<>();
        if (!Files.isDirectory(root)) {
            return names;
        }
        try (Stream<Path> walk = Files.list(root)) {
            walk.filter(p -> p.toString().endsWith(EXTENSION))
                    .forEach(p -> names.add(readRecord(p).name()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to list Kyt store", e);
        }
        return names;
    }

    public boolean delete(String name) {
        try {
            return Files.deleteIfExists(root.resolve(fileNameFor(name)));
        } catch (IOException e) {
            throw new RuntimeException("Failed to remove Kyt '" + name + "'", e);
        }
    }

    private static String fileNameFor(String name) {
        return sanitize(name) + EXTENSION;
    }

    /** Kyt names are player-supplied -- keep them filesystem-safe without silently colliding. */
    private static String sanitize(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private LoadoutRecord readRecord(Path file) {
        try {
            String json = Files.readString(file, StandardCharsets.UTF_8);
            JsonElement element = GSON.fromJson(json, JsonElement.class);
            return LoadoutRecord.CODEC.parse(JsonOps.INSTANCE, element)
                    .getOrThrow(msg -> new IOException("Failed to decode " + file + ": " + msg));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read Kyt file " + file, e);
        }
    }
}
