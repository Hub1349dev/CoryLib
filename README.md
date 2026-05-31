# CoryLib

CoryLib is a unified data storage and synchronization library for Minecraft mods.

Designed for mods that need a clean way to store, retrieve, version, and synchronize data across server, dimension, player, and client contexts.

## Why CoryLib?

Minecraft mods often reimplement the same data systems over and over:

- Player data persistence
- World or server-wide state
- Dimension-specific state
- Client preferences
- Server-to-client sync
- Runtime-only transient data

CoryLib provides one fluent API for all of those cases.

```java
public static final CoryContext DATA = CoryData.init("my_mod");

public static final DataEntry<ServerPlayer, Integer> KILLS =
        DATA.player("kills", Codec.INT)
                .defaultValue(0)
                .storage(Storage.DISK)
                .version(1)
                .sync(SyncTrigger.IMMEDIATE)
                .build();
```

## Features

- **Four built-in scopes:** server, dimension, player, and client.
- **Two storage modes:** JSON-backed disk storage and runtime memory storage.
- **Codec-based serialization:** values are encoded and decoded with Minecraft `Codec<T>`.
- **Versioned JSON files:** persistent entries use a `_version` envelope.
- **Migration support:** upgrade older JSON values before decoding.
- **Server-to-client sync:** immediate, tick-batched, or manual sync triggers.
- **Client-side synced mirror:** read the latest received synced value with `getSynced()`.
- **Architectury lifecycle integration:** data loads/saves through common Fabric and NeoForge events.

## Core Concepts

### Scopes

Each entry belongs to exactly one scope. The scope determines what object owns the value.

| Scope | Subject type | Meaning | Can sync |
| --- | --- | --- | --- |
| `server` | `MinecraftServer` | One value per save/server | Yes |
| `dimension` | `ServerLevel` | One value per loaded dimension | Yes |
| `player` | `ServerPlayer` | One value per player UUID | Yes |
| `client` | `ClientSubject` | One local client value | No |

### Storage

```java
.storage(Storage.DISK)
```

Stores the value as JSON under the save or game directory.

```java
.storage(Storage.MEMORY)
```

Keeps the value in memory only. It is discarded when the subject is flushed or the game shuts down.

## Disk Layout

Persistent entries are stored as JSON files.

```text
server:    <save>/corylib/<modid>/<key>.json
dimension: <save>/corylib/<modid>/dim/<namespace>/<path>/<key>.json
player:    <save>/corylib/<modid>/player/<uuid>/<key>.json
client:    .minecraft/corylib/<modid>/<key>.json
client per-world:
  singleplayer: <save>/corylib/<modid>/<key>.json
  multiplayer:  .minecraft/corylib-worlds/servers/<server-hash>/corylib/<modid>/<key>.json
```

Each file uses a stable envelope:

```json
{
  "_version": 1,
  "value": {}
}
```

## Examples

### Player Data

```java
public static final DataEntry<ServerPlayer, Integer> KILLS =
        DATA.player("kills", Codec.INT)
                .defaultValue(0)
                .storage(Storage.DISK)
                .version(1)
                .sync(SyncTrigger.IMMEDIATE)
                .build();

int kills = KILLS.get(player);
KILLS.set(player, kills + 1);
KILLS.modify(player, value -> value + 1);
```

### Runtime-Only Player Data

```java
public static final DataEntry<ServerPlayer, Integer> COMBO =
        DATA.player("combo", Codec.INT)
                .defaultValue(0)
                .storage(Storage.MEMORY)
                .build();
```

### Dimension Data

```java
public static final DataEntry<ServerLevel, List<BlockPos>> PORTALS =
        DATA.dimension("portals", BlockPos.CODEC.listOf())
                .defaultValue(ArrayList::new)
                .storage(Storage.DISK)
                .version(1)
                .sync(SyncTrigger.MANUAL)
                .build();

PORTALS.scheduleSync(level);
```

### Loaded Subjects

```java
Map<ServerPlayer, Integer> loadedKills = KILLS.getLoaded();
KILLS.setLoaded(player -> 0);
```

`getLoaded()` and `setLoaded(...)` operate only on subjects currently present in CoryLib's memory cache. They do not scan unloaded players, offline saves, or dimensions that are not loaded.

### Client Data

```java
import dev.hub.corylib.api.subject.ClientSubject;

public static final DataEntry<ClientSubject, Float> HUD_SCALE =
        DATA.client("hud_scale", Codec.FLOAT)
                .defaultValue(1.0f)
                .storage(Storage.DISK)
                .version(1)
                .build();

float scale = HUD_SCALE.get(CoryData.client());
HUD_SCALE.set(CoryData.client(), 1.25f);
```

Client entries are loaded and saved by CoryLib's client lifecycle. Use `.perWorld()` for preferences that should be scoped to the active singleplayer save or multiplayer server.

### Reading Synced Data on the Client

Synced server, dimension, and player entries are mirrored on the client as JSON and decoded through the same `Codec<T>`.

```java
Optional<Integer> syncedKills = KILLS.getSynced();
```

## Sync Triggers

| Trigger | Behavior |
| --- | --- |
| `IMMEDIATE` | Sends a packet as soon as `set` is called. |
| `ON_TICK` | Marks the value dirty and sends it at the end of the server tick. |
| `MANUAL` | Sends only when `scheduleSync(subject)` is called. |

Recipients are selected by scope:

| Scope | Packet recipients |
| --- | --- |
| `player` | The specific player |
| `dimension` | Players in that dimension |
| `server` | All connected players |

## Versioning and Migration

Disk entries must declare a version.

```java
DATA.player("kills", KillData.CODEC)
        .defaultValue(KillData::empty)
        .storage(Storage.DISK)
        .version(2, Migration.from(1, json -> {
            JsonObject obj = new JsonObject();
            obj.addProperty("count", json.getAsInt());
            obj.addProperty("lastKill", "");
            return obj;
        }))
        .build();
```

If loading fails because of corrupt JSON or a codec error, CoryLib logs the failure and falls back to the entry default.

## Building

```powershell
.\gradlew.bat build
```

Build outputs are generated for both Fabric and NeoForge.

The build also runs `:common:contractTest`, executable contract tests for CoryLib behavior such as scope/registry rules, builder validation, entry memory lifecycle, loaded-subject APIs, client disk storage, migration rules, sync trigger semantics, client sync storage copying, and registration closure. A smaller `:common:staticBoundaryCheck` catches source-boundary mistakes that runtime tests cannot see, such as client-only imports outside the client lifecycle boundary.

## Project Structure

```text
common/   Shared CoryLib API and implementation
fabric/   Fabric loader entrypoint
neoforge/ NeoForge loader entrypoint
```

## Status

CoryLib is currently an early v1 implementation. The core API, disk storage, memory storage, lifecycle hooks, versioned JSON, migrations, and S2C sync transport are implemented and compile for Fabric and NeoForge.

Future work may include custom storage backends, entity scope, chunk scope, compression, and debug UI tooling.
