package dev.hub.corylib.api.subject;

import java.nio.file.Path;

public final class ClientSubject {
    public static final ClientSubject INSTANCE = new ClientSubject();

    private Path gameFolder;
    private Path worldFolder;

    private ClientSubject() {
    }

    public synchronized void setGameFolder(Path gameFolder) {
        this.gameFolder = gameFolder;
    }

    public synchronized Path gameFolder() {
        return gameFolder;
    }

    public synchronized void setWorldFolder(Path worldFolder) {
        this.worldFolder = worldFolder;
    }

    public synchronized Path worldFolder() {
        return worldFolder;
    }

    public synchronized void invalidate() {
        worldFolder = null;
    }
}
