package dev.truewinter.minebroadcast;

import org.bukkit.Material;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/* package */ class Config {

    private final MineBroadcast plugin;
    private final Set<SafeBlock> broadcastBlacklist   = new HashSet<>();
    private final Set<Material>  blocksToBroadcast    = new HashSet<>();
    private final Set<String>    worldWhitelist       = new HashSet<>();
    private final Set<UUID>      optOutPlayers        = new HashSet<>();
    private final File           playerFile;
    private       boolean        worldWhitelistActive = false;

    /* package */ Config(MineBroadcast plugin) {
        this.plugin = plugin;
        playerFile = new File(plugin.getDataFolder(), "players.dat");
        plugin.saveDefaultConfig();
    }

    /* package */ void loadConfig() {
        plugin.reloadConfig();
        // Create the list of materials to broadcast from the file
        List<String> configList = plugin.getConfig().getStringList("blocks");
        blocksToBroadcast.clear();

        for(String item : configList) {
            Material material = Material.getMaterial(item.toUpperCase());
            blocksToBroadcast.add(material);
        }

        // Load world whitelist
        worldWhitelist.clear();
        worldWhitelistActive = plugin.getConfig().getBoolean("active-per-worlds", true);
        if(worldWhitelistActive) {
            worldWhitelist.addAll(plugin.getConfig().getStringList("active-worlds"));
        }

        // Load opt out players
        if(!playerFile.exists()) {
            return;
        }
        optOutPlayers.clear();
        try (ObjectInputStream stream = new ObjectInputStream(new FileInputStream(playerFile))) {
            @SuppressWarnings("unchecked")
            Set<UUID> uuids = (Set<UUID>) stream.readObject();
            optOutPlayers.addAll(uuids);
        } catch(IOException | ClassNotFoundException e) {
            plugin.getLogger().severe("Failed to read opt out players from file");
            e.printStackTrace(System.err);
        } catch(ClassCastException e) {
            plugin.getLogger().severe("Invalid players.dat file");
            e.printStackTrace(System.err);
        }
    }

    /* package */ boolean isOptOut(UUID uuid) {
        return optOutPlayers.contains(uuid);
    }

    /* package */ void optOutPlayer(UUID uuid) {
        optOutPlayers.add(uuid);
        saveOptOutPlayers();
    }

    /* package */ void unOptOutPlayer(UUID uuid) {
        optOutPlayers.remove(uuid);
        saveOptOutPlayers();
    }

    private void saveOptOutPlayers() {
        try (ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(playerFile))) {
            stream.writeObject(optOutPlayers);
        } catch(IOException e) {
            plugin.getLogger().severe("Failed to write opt out players to file");
            e.printStackTrace(System.err);
        }
    }

    /* package */ Set<SafeBlock> getBroadcastBlacklist() {
        return broadcastBlacklist;
    }

    /* package */ Set<Material> getBlocksToBroadcast() {
        return blocksToBroadcast;
    }

    /* package */ Set<String> getWorldWhitelist() {
        return worldWhitelist;
    }

    /* package */ boolean isWorldWhitelistActive() {
        return worldWhitelistActive;
    }

}
