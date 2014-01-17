package be.bendem.OreBroadcast;

import be.bendem.chatformatter.ChatFormatter;

import org.bukkit.block.Block;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.GameMode;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.HashSet;

public class BlockBreakListener implements Listener {

    public OreBroadcast plugin;

    BlockBreakListener(OreBroadcast plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // Reject creative users and users without ob.broadcast permission
        if(event.getPlayer().getGameMode() != GameMode.SURVIVAL
                || !event.getPlayer().hasPermission("ob.broadcast")) {
            return;
        }

        Block block = event.getBlock();
        // Don't broadcast the blocks which has already been broadcasted!
        if(plugin.alreadyBroadcastedBlocks.contains(block)) {
            return;
        }

        // Measuring event time
        long timer = System.currentTimeMillis();

        // Create the list of blocks to broadcast from the file
        ArrayList<String> blocksToBroadcast = new ArrayList<String>(plugin.getConfig().getStringList("ores"));
        for (int i = 0; i < blocksToBroadcast.size(); ++i) {
            blocksToBroadcast.set(i, blocksToBroadcast.get(i).toUpperCase() + "_ORE");
            // Handle glowing redstone ore (id 74) and redstone ore (id 73)
            if(blocksToBroadcast.get(i).equals("REDSTONE_ORE")) {
                blocksToBroadcast.add("GLOWING_REDSTONE");
            }
        }

        String blockName = (block.getType() == Material.GLOWING_REDSTONE_ORE ? "redstone" :
            block.getType().name().toLowerCase().replace("_ore", ""));

        if(blocksToBroadcast.contains(block.getType().name())) {
            int veinSize = getVeinSize(block);
            String color = plugin.getConfig().getString("colors." + blockName, "white").toUpperCase();

            broadcast(format(
                plugin.getConfig().getString("message", "{player} just found {count} block{plural} of {ore}"),
                event.getPlayer().getDisplayName(),
                Integer.toString(veinSize),
                blockName,
                color,
                veinSize > 1
            ));
        }

        plugin.logger.finer("Event duration : " + (System.currentTimeMillis() - timer) + "ms");
    }

    public final int getVeinSize(Block block) {
        HashSet<Block> vein = new HashSet<Block>();
        vein.add(block);
        vein = getVein(block, vein);
        plugin.alreadyBroadcastedBlocks.addAll(vein);

        return vein.size();
    }

    public final HashSet<Block> getVein(Block block, HashSet<Block> vein) {
        int i, j, k;
        for (i = -1; i < 2; ++i) {
            for (j = -1; j < 2; ++j) {
                for (k = -1; k < 2; ++k) {
                    if(vein.contains(block.getRelative(i, j, k))       // block already found
                       || !compare(block, block.getRelative(i, j, k))  // block has not the same type
                       || (i == 0 && j == 0 && k == 0)) {              // comparing block to itself
                        // Recursion end!
                        continue;
                    }
                    vein.add(block.getRelative(i, j, k));
                    vein = getVein(block.getRelative(i, j, k), vein);
                }
            }
        }

        return vein;
    }

    public boolean compare(Block block1, Block block2) {
        return block1.getType().equals(block2.getType())
            || block1.getType() == Material.GLOWING_REDSTONE_ORE && block2.getType() == Material.REDSTONE_ORE
            || block1.getType() == Material.REDSTONE_ORE && block2.getType() == Material.GLOWING_REDSTONE_ORE;
    }

    public void broadcast(String message) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if(player.hasPermission("ob.receive")) {
                player.sendMessage(message);
            }
        }
    }

    public final String format(String msg, String player, String count, String ore, String color, boolean plural) {
        return msg
            .replace("{player}", ChatFormatter.bold(player))
            .replace("{count}",  ChatFormatter.bold(count))
            .replace("{ore}",    ChatFormatter.format(ChatFormatter.bold(ore), ChatColor.valueOf(color)))
            .replace("{plural}", plural ? plugin.getConfig().getString("plural", "s") : "");
    }

}
