package dev.hub.corylib.contract;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import dev.hub.corylib.CoryContext;
import dev.hub.corylib.CoryData;
import dev.hub.corylib.api.entry.DataEntry;
import dev.hub.corylib.api.migration.DataVersion;
import dev.hub.corylib.api.migration.Migration;
import dev.hub.corylib.api.scope.DataScope;
import dev.hub.corylib.api.scope.Scopes;
import dev.hub.corylib.api.storage.Storage;
import dev.hub.corylib.api.storage.StorageType;
import dev.hub.corylib.api.subject.ClientSubject;
import dev.hub.corylib.api.sync.SyncTrigger;
import dev.hub.corylib.impl.registry.DataRegistry;
import dev.hub.corylib.impl.registry.ScopeRegistry;
import dev.hub.corylib.impl.sync.ClientSyncStorage;
import dev.hub.corylib.impl.sync.SyncSchedulerContract;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public final class CoryLibContractTests {
    private static final AtomicInteger IDS = new AtomicInteger();

    private CoryLibContractTests() {
    }

    public static void main(String[] args) throws Exception {
        scopesExposeStableBuiltInContracts();
        scopeRegistryRejectsDuplicatesAndUnknownInstances();
        storageTypesCompareByIdAndValidateIds();
        buildersValidateKeysDefaultsStorageAndSyncTriggers();
        dataRegistryRejectsDuplicateContextsAndEntries();
        dataEntryMemoryLifecycleCallbacksAndLoadedSubjectsWork();
        dataEntryRejectsUnknownEntriesAndWrongSubjects();
        dataRegistryClientLifecycleRoutesGlobalAndPerWorldEntries();
        diskClientStorageSavesLoadsMigratesAndFallsBackToDefault();
        migrationsValidateAndRunInOrder();
        clientSyncStorageDefensivelyCopiesJson();
        clientSubjectTracksGameAndWorldFolders();
        SyncSchedulerContract.run();
        registrationCloseIsEnforcedAtTheEnd();
    }

    private static void scopesExposeStableBuiltInContracts() {
        ContractAssertions.equals("server", Scopes.SERVER.id(), "Server scope id must be stable.");
        ContractAssertions.equals("dimension", Scopes.DIMENSION.id(), "Dimension scope id must be stable.");
        ContractAssertions.equals("player", Scopes.PLAYER.id(), "Player scope id must be stable.");
        ContractAssertions.equals("client", Scopes.CLIENT.id(), "Client scope id must be stable.");
        ContractAssertions.isTrue(Scopes.SERVER.syncSupported(), "Server scope must support sync.");
        ContractAssertions.isTrue(Scopes.DIMENSION.syncSupported(), "Dimension scope must support sync.");
        ContractAssertions.isTrue(Scopes.PLAYER.syncSupported(), "Player scope must support sync.");
        ContractAssertions.isTrue(!Scopes.CLIENT.syncSupported(), "Client scope must not support server-to-client sync.");
        ContractAssertions.equals("client", Scopes.CLIENT.toString(), "DataScope.toString must return its id.");
        ContractAssertions.throwsException(IllegalArgumentException.class, () -> new DataScope<>(" ", String.class, false),
                "Blank scope ids must fail.");
        ContractAssertions.throwsException(NullPointerException.class, () -> new DataScope<String>(null, String.class, false),
                "Null scope ids must fail.");
    }

    private static void scopeRegistryRejectsDuplicatesAndUnknownInstances() {
        DataScope<String> scope = new DataScope<>(id("scope"), String.class, true);
        ScopeRegistry.INSTANCE.register(scope);

        ContractAssertions.equals(Optional.of(scope), ScopeRegistry.INSTANCE.byId(scope.id()),
                "Registered scopes must be discoverable by id.");
        ContractAssertions.isTrue(ScopeRegistry.INSTANCE.all().contains(scope),
                "Registered scopes must appear in all().");
        ContractAssertions.throwsException(UnsupportedOperationException.class,
                () -> ScopeRegistry.INSTANCE.all().clear(),
                "ScopeRegistry.all() must be immutable.");
        ContractAssertions.throwsException(IllegalStateException.class, () -> ScopeRegistry.INSTANCE.register(scope),
                "Registering the same scope twice must fail.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> ScopeRegistry.INSTANCE.assertRegistered(new DataScope<>(id("unknown_scope"), String.class, true)),
                "Unregistered scopes must fail registry validation.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> ScopeRegistry.INSTANCE.assertRegistered(new DataScope<>(scope.id(), String.class, true)),
                "A different DataScope instance with an already registered id must fail.");
    }

    private static void storageTypesCompareByIdAndValidateIds() {
        ContractAssertions.equals(Storage.DISK, new StorageType("disk"), "StorageType equality must be id-based.");
        ContractAssertions.equals(Storage.DISK.hashCode(), new StorageType("disk").hashCode(),
                "Equal storage types must have equal hash codes.");
        ContractAssertions.notEquals(Storage.MEMORY, new StorageType("disk"), "Different storage ids must not compare equal.");
        ContractAssertions.equals("memory", Storage.MEMORY.toString(), "StorageType.toString must return its id.");
        ContractAssertions.throwsException(IllegalArgumentException.class, () -> new StorageType(" "),
                "Blank storage ids must fail.");
        ContractAssertions.throwsException(NullPointerException.class, () -> new StorageType(null),
                "Null storage ids must fail.");
    }

    private static void buildersValidateKeysDefaultsStorageAndSyncTriggers() {
        CoryContext context = CoryData.init(id("builder_mod"));

        ContractAssertions.throwsException(IllegalArgumentException.class,
                () -> context.client("Bad Key", Codec.INT),
                "Builder keys must be lowercase and path-safe.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> context.client(id("missing_default"), Codec.INT).build(),
                "Entries must require a default value.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> context.client(id("disk_without_version"), Codec.INT)
                        .defaultValue(1)
                        .storage(Storage.DISK)
                        .build(),
                "Disk entries must require a version.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> context.client(id("unsupported_storage"), Codec.INT)
                        .defaultValue(1)
                        .storage(new StorageType("remote"))
                        .build(),
                "Unsupported storage types must fail until a backend exists.");
        ContractAssertions.throwsException(NullPointerException.class,
                () -> context.player(id("null_sync"), Codec.INT)
                        .defaultValue(0)
                        .sync(null),
                "sync(null) must fail immediately.");

        DataEntry<ClientSubject, Integer> globalClient = context.client(id("client_global"), Codec.INT)
                .defaultValue(1)
                .storage(new StorageType("memory"))
                .build();
        ContractAssertions.equals(Scopes.CLIENT, globalClient.scope(), "Client builder must use the client scope.");
        ContractAssertions.isTrue(!globalClient.perWorld(), "Client entries must default to global client storage.");

        DataEntry<ClientSubject, Integer> perWorldClient = context.client(id("client_world"), Codec.INT)
                .defaultValue(1)
                .perWorld()
                .build();
        ContractAssertions.isTrue(perWorldClient.perWorld(), "ClientDataBuilder.perWorld() must mark the entry as per-world.");

        DataEntry<?, Integer> server = context.server(id("server"), Codec.INT).defaultValue(0).sync(SyncTrigger.MANUAL).build();
        DataEntry<?, Integer> dimension = context.dimension(id("dimension"), Codec.INT).defaultValue(0).sync(SyncTrigger.ON_TICK).build();
        DataEntry<?, Integer> player = context.player(id("player"), Codec.INT).defaultValue(0).sync(SyncTrigger.IMMEDIATE).build();
        ContractAssertions.equals(Scopes.SERVER, server.scope(), "Server builder must use the server scope.");
        ContractAssertions.equals(Scopes.DIMENSION, dimension.scope(), "Dimension builder must use the dimension scope.");
        ContractAssertions.equals(Scopes.PLAYER, player.scope(), "Player builder must use the player scope.");

        ContractAssertions.throwsException(IllegalStateException.class,
                () -> new DataEntry<>("contract", id("client_sync_forbidden"), Scopes.CLIENT, Codec.INT, () -> 0,
                        Storage.MEMORY, null, null, null, SyncTrigger.ON_TICK, false),
                "Client scope entries must reject server-to-client sync triggers.");
    }

    private static void dataRegistryRejectsDuplicateContextsAndEntries() {
        String modId = id("registry_mod");
        CoryData.init(modId);
        ContractAssertions.throwsException(IllegalStateException.class, () -> CoryData.init(modId),
                "Calling CoryData.init twice for the same mod id must fail.");

        DataScope<String> scope = registeredStringScope();
        DataEntry<String, String> first = memoryEntry("registry_dup", "same_key", scope, "default");
        DataRegistry.INSTANCE.register(first);
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> DataRegistry.INSTANCE.register(memoryEntry("registry_dup", "same_key", scope, "default")),
                "DataRegistry must reject duplicate entry ids.");
        ContractAssertions.equals(first, DataRegistry.INSTANCE.entryById(first.id()),
                "DataRegistry.entryById must return registered entries.");
        ContractAssertions.equals(null, DataRegistry.INSTANCE.entryById(id("missing_entry")),
                "DataRegistry.entryById must return null for missing entries.");
        ContractAssertions.throwsException(UnsupportedOperationException.class,
                () -> DataRegistry.INSTANCE.entries().clear(),
                "DataRegistry.entries() must be immutable.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> DataRegistry.INSTANCE.register(memoryEntry("unregistered_scope", id("entry"), new DataScope<>(id("unregistered"), String.class, false), "default")),
                "Entries with unregistered scopes must fail registration.");
    }

    private static void dataEntryMemoryLifecycleCallbacksAndLoadedSubjectsWork() {
        DataScope<String> scope = registeredStringScope();
        AtomicInteger loads = new AtomicInteger();
        AtomicInteger saves = new AtomicInteger();
        DataEntry<String, Integer> entry = new DataEntry<>(
                id("memory_mod"),
                id("counter"),
                scope,
                Codec.INT,
                () -> 1,
                Storage.MEMORY,
                null,
                ignored -> loads.incrementAndGet(),
                ignored -> saves.incrementAndGet()
        );
        DataRegistry.INSTANCE.register(entry);

        ContractAssertions.equals(entry.modId() + ":" + entry.key(), entry.id(), "Entry id must be modid:key.");
        ContractAssertions.equals(Codec.INT, entry.codec(), "Entry codec accessor must return the configured codec.");
        ContractAssertions.equals(Storage.MEMORY, entry.storage(), "Entry storage accessor must return the configured storage.");
        ContractAssertions.equals(null, entry.version(), "Memory entries without version must expose null version.");
        ContractAssertions.isTrue(!entry.synced(), "Entries without sync trigger must not be synced.");

        ContractAssertions.equals(1, entry.get("alpha"), "Memory get must materialize the default value.");
        ContractAssertions.equals(2, entry.modify("alpha", value -> value + 1), "modify must return the new value.");
        ContractAssertions.equals(2, entry.get("alpha"), "modify must persist the new value.");
        entry.clear("alpha");
        ContractAssertions.equals(1, entry.get("alpha"), "clear must restore a fresh default value.");
        entry.set("beta", 5);
        ContractAssertions.equals(Map.of("alpha", 1, "beta", 5), entry.getLoaded(), "getLoaded must return cached subjects.");
        ContractAssertions.throwsException(UnsupportedOperationException.class,
                () -> entry.getLoaded().clear(),
                "getLoaded must return an immutable map.");
        entry.setLoaded(subject -> subject.length());
        ContractAssertions.equals(5, entry.get("alpha"), "setLoaded must update already loaded subjects.");
        ContractAssertions.equals(4, entry.get("beta"), "setLoaded must update every loaded subject.");
        entry.load("gamma");
        entry.save("gamma");
        ContractAssertions.equals(1, loads.get(), "onLoad must run after load.");
        ContractAssertions.equals(1, saves.get(), "onSave must run before save.");
        entry.flush("gamma");
        ContractAssertions.equals(1, entry.get("gamma"), "flush must remove cached values so default materializes again.");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void dataEntryRejectsUnknownEntriesAndWrongSubjects() {
        DataScope<String> scope = registeredStringScope();
        DataEntry<String, String> unregistered = memoryEntry(id("unknown_entry_mod"), id("value"), scope, "default");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> unregistered.get("subject"),
                "Unregistered DataEntry instances must not be usable.");

        DataEntry<String, String> registered = memoryEntry(id("wrong_subject_mod"), id("value"), scope, "default");
        DataRegistry.INSTANCE.register(registered);
        ContractAssertions.throwsException(IllegalArgumentException.class,
                () -> ((DataEntry) registered).get(123),
                "DataEntry must reject subjects outside its scope subject type.");
        ContractAssertions.throwsException(NullPointerException.class,
                () -> registered.get(null),
                "DataEntry must reject null subjects.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> registered.scheduleSync("subject"),
                "scheduleSync must reject non-manual entries.");
    }

    private static void dataRegistryClientLifecycleRoutesGlobalAndPerWorldEntries() {
        AtomicInteger globalLoads = new AtomicInteger();
        AtomicInteger globalSaves = new AtomicInteger();
        AtomicInteger worldLoads = new AtomicInteger();
        AtomicInteger worldSaves = new AtomicInteger();

        DataEntry<ClientSubject, Integer> global = new DataEntry<>(
                id("client_lifecycle_mod"),
                id("global"),
                Scopes.CLIENT,
                Codec.INT,
                () -> 1,
                Storage.MEMORY,
                null,
                ignored -> globalLoads.incrementAndGet(),
                ignored -> globalSaves.incrementAndGet(),
                null,
                false
        );
        DataEntry<ClientSubject, Integer> world = new DataEntry<>(
                id("client_lifecycle_mod"),
                id("world"),
                Scopes.CLIENT,
                Codec.INT,
                () -> 2,
                Storage.MEMORY,
                null,
                ignored -> worldLoads.incrementAndGet(),
                ignored -> worldSaves.incrementAndGet(),
                null,
                true
        );
        DataRegistry.INSTANCE.register(global);
        DataRegistry.INSTANCE.register(world);

        DataRegistry.INSTANCE.loadClientGlobal();
        ContractAssertions.equals(1, globalLoads.get(), "loadClientGlobal must load global client entries.");
        ContractAssertions.equals(0, worldLoads.get(), "loadClientGlobal must not load per-world client entries.");

        DataRegistry.INSTANCE.loadClientWorld();
        ContractAssertions.equals(1, worldLoads.get(), "loadClientWorld must load per-world client entries.");

        DataRegistry.INSTANCE.saveClientGlobal();
        ContractAssertions.equals(1, globalSaves.get(), "saveClientGlobal must save global client entries.");
        ContractAssertions.equals(0, worldSaves.get(), "saveClientGlobal must not save per-world client entries.");

        DataRegistry.INSTANCE.saveClientWorld();
        ContractAssertions.equals(1, worldSaves.get(), "saveClientWorld must save per-world client entries.");

        world.set(ClientSubject.INSTANCE, 5);
        DataRegistry.INSTANCE.flushClientWorld();
        ContractAssertions.equals(2, world.get(ClientSubject.INSTANCE), "flushClientWorld must remove cached per-world client values.");
    }

    private static void diskClientStorageSavesLoadsMigratesAndFallsBackToDefault() throws IOException {
        Path root = Files.createTempDirectory("corylib-contract");
        ClientSubject.INSTANCE.setGameFolder(root);
        ClientSubject.INSTANCE.setWorldFolder(root.resolve("world"));

        DataEntry<ClientSubject, Integer> global = clientDiskEntry(id("disk_global"), false, 1, List.of(), () -> 7);
        global.set(ClientSubject.INSTANCE, 42);
        global.save(ClientSubject.INSTANCE);
        global.flush(ClientSubject.INSTANCE);
        ContractAssertions.equals(42, global.get(ClientSubject.INSTANCE), "Disk client entries must load saved values.");

        DataEntry<ClientSubject, Integer> world = clientDiskEntry(id("disk_world"), true, 1, List.of(), () -> 3);
        world.set(ClientSubject.INSTANCE, 9);
        world.save(ClientSubject.INSTANCE);
        world.flush(ClientSubject.INSTANCE);
        ContractAssertions.equals(9, world.get(ClientSubject.INSTANCE), "Per-world client entries must use the configured world folder.");

        String migratedKey = id("disk_migrated");
        DataEntry<ClientSubject, Integer> migrated = clientDiskEntry(migratedKey, false, 2,
                List.of(Migration.from(1, json -> JsonParser.parseString(String.valueOf(json.getAsInt() + 10)))),
                () -> 0);
        Path migratedPath = root.resolve("corylib").resolve(migrated.modId()).resolve(migratedKey + ".json");
        Files.createDirectories(migratedPath.getParent());
        Files.writeString(migratedPath, "{\"_version\":1,\"value\":5}");
        ContractAssertions.equals(15, migrated.get(ClientSubject.INSTANCE), "Disk loads must migrate old values before decoding.");

        String corruptKey = id("disk_corrupt");
        DataEntry<ClientSubject, Integer> corrupt = clientDiskEntry(corruptKey, false, 1, List.of(), () -> 99);
        Path corruptPath = root.resolve("corylib").resolve(corrupt.modId()).resolve(corruptKey + ".json");
        Files.createDirectories(corruptPath.getParent());
        Files.writeString(corruptPath, "not json");
        ContractAssertions.equals(99, corrupt.get(ClientSubject.INSTANCE), "Corrupt disk data must fall back to the default value.");
    }

    private static void migrationsValidateAndRunInOrder() {
        DataVersion version = new DataVersion(3, List.of(
                Migration.from(1, json -> {
                    JsonObject object = new JsonObject();
                    object.addProperty("value", json.getAsInt() + 1);
                    return object;
                }),
                Migration.from(2, json -> {
                    JsonObject object = json.getAsJsonObject();
                    object.addProperty("value", object.get("value").getAsInt() * 2);
                    return object;
                })
        ));

        ContractAssertions.equals(4, version.migrate(1, JsonParser.parseString("1")).getAsJsonObject().get("value").getAsInt(),
                "Migrations must run one version step at a time.");
        JsonObject current = new JsonObject();
        current.addProperty("value", 12);
        ContractAssertions.equals(current, version.migrate(3, current), "Current-version values must not be migrated.");
        ContractAssertions.throwsException(IllegalStateException.class, () -> version.migrate(4, new JsonObject()),
                "Loading data from a future version must fail.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> new DataVersion(3, List.of(Migration.from(1, json -> json))).migrate(1, new JsonObject()),
                "Missing migration steps must fail.");
        ContractAssertions.throwsException(IllegalArgumentException.class,
                () -> new DataVersion(-1, List.of()),
                "Negative current versions must fail.");
        ContractAssertions.throwsException(IllegalArgumentException.class,
                () -> Migration.from(-1, json -> json),
                "Negative migration versions must fail.");
        ContractAssertions.throwsException(IllegalArgumentException.class,
                () -> new DataVersion(2, List.of(Migration.from(1, json -> json), Migration.from(1, json -> json))),
                "Duplicate migrations from the same version must fail.");
        ContractAssertions.throwsException(IllegalArgumentException.class,
                () -> new DataVersion(2, List.of(Migration.from(2, json -> json))),
                "Migrations from the current version must fail.");
        ContractAssertions.throwsException(NullPointerException.class,
                () -> Migration.from(1, null),
                "Migration upgraders must not be null.");
    }

    private static void clientSyncStorageDefensivelyCopiesJson() {
        JsonObject original = new JsonObject();
        original.addProperty("value", 1);
        ClientSyncStorage.INSTANCE.put("contract:test", original);
        original.addProperty("value", 2);

        JsonObject stored = ClientSyncStorage.INSTANCE.get("contract:test").getAsJsonObject();
        ContractAssertions.equals(1, stored.get("value").getAsInt(), "Client sync storage must copy values on write.");

        stored.addProperty("value", 3);
        ContractAssertions.equals(1, ClientSyncStorage.INSTANCE.get("contract:test").getAsJsonObject().get("value").getAsInt(),
                "Client sync storage must copy values on read.");
        ContractAssertions.isTrue(ClientSyncStorage.INSTANCE.snapshot().containsKey("contract:test"),
                "Client sync snapshots must include stored entries.");
        ContractAssertions.throwsException(UnsupportedOperationException.class,
                () -> ClientSyncStorage.INSTANCE.snapshot().clear(),
                "Client sync snapshots must be immutable.");
        ClientSyncStorage.INSTANCE.clear();
        ContractAssertions.equals(null, ClientSyncStorage.INSTANCE.get("contract:test"), "Client sync clear must remove values.");
    }

    private static void clientSubjectTracksGameAndWorldFolders() {
        Path game = Path.of("contract-game");
        Path world = Path.of("contract-world");
        ClientSubject.INSTANCE.setGameFolder(game);
        ClientSubject.INSTANCE.setWorldFolder(world);
        ContractAssertions.equals(game, ClientSubject.INSTANCE.gameFolder(), "ClientSubject must expose its game folder.");
        ContractAssertions.equals(world, ClientSubject.INSTANCE.worldFolder(), "ClientSubject must expose its world folder.");
        ContractAssertions.equals(ClientSubject.INSTANCE, CoryData.client(), "CoryData.client() must return the singleton client subject.");
        ClientSubject.INSTANCE.invalidate();
        ContractAssertions.equals(null, ClientSubject.INSTANCE.worldFolder(), "ClientSubject.invalidate must clear world folder.");
        ContractAssertions.equals(game, ClientSubject.INSTANCE.gameFolder(), "ClientSubject.invalidate must not clear game folder.");
    }

    private static void registrationCloseIsEnforcedAtTheEnd() {
        DataRegistry.INSTANCE.closeRegistration();
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> CoryData.init(id("closed_mod")),
                "CoryData.init must fail after registration closes.");
        ContractAssertions.throwsException(IllegalStateException.class,
                () -> DataRegistry.INSTANCE.register(memoryEntry(id("closed_registry_mod"), id("entry"), registeredStringScope(), "default")),
                "DataRegistry.register must fail after registration closes.");
    }

    private static DataEntry<ClientSubject, Integer> clientDiskEntry(String key, boolean perWorld, int version, List<Migration> migrations, java.util.function.Supplier<Integer> defaultValue) {
        DataEntry<ClientSubject, Integer> entry = new DataEntry<>(
                id("disk_mod"),
                key,
                Scopes.CLIENT,
                Codec.INT,
                defaultValue,
                Storage.DISK,
                new DataVersion(version, migrations),
                null,
                null,
                null,
                perWorld
        );
        DataRegistry.INSTANCE.register(entry);
        return entry;
    }

    private static DataEntry<String, String> memoryEntry(String modId, String key, DataScope<String> scope, String defaultValue) {
        return new DataEntry<>(modId, key, scope, Codec.STRING, () -> defaultValue, Storage.MEMORY, null, null, null);
    }

    private static DataScope<String> registeredStringScope() {
        DataScope<String> scope = new DataScope<>(id("string_scope"), String.class, true);
        ScopeRegistry.INSTANCE.register(scope);
        return scope;
    }

    private static String id(String prefix) {
        return prefix + "_" + IDS.incrementAndGet();
    }
}
