package org.ah.minecraft.blockshuffle;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import net.md_5.bungee.api.ChatColor;

public class BlockShuffle extends JavaPlugin {

	public int rounds = 0;
	public HashMap<Player, Material> blocks = new HashMap<Player, Material>();

	public java.util.List<Player> found = new ArrayList<Player>();

	private java.util.List<Material> blocklist;

	private long endOfRound = 0;
	private int taskID = 0;
	private BukkitScheduler scheduler;

	@Override
	public void onEnable() {
		getLogger().info("onEnable has been invoked!");
		scheduler = getServer().getScheduler();

		blocklist = new ArrayList<Material>();
		for (Material block : Material.values()) {
			if (block.isBlock() && block.isSolid()) {
				if (block != Material.BARRIER && block != Material.CHAIN_COMMAND_BLOCK
						&& block != Material.COMMAND_BLOCK && block != Material.REPEATING_COMMAND_BLOCK) {
					blocklist.add(block);
				}
			}
		}
	}

	@Override
	public void onDisable() {
		getLogger().info("onDisable has been invoked!");
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("start")) {
			if ((sender instanceof Player && sender.isOp())) {
				sender.sendMessage(ChatColor.DARK_RED + "Starting!");
				start();
			}
			return true;
		}
		return false;
	}

	public void startRound() {

		int playing = 0;
		Player winner = null;
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (p.getGameMode() == GameMode.SURVIVAL) {
				playing++;
				winner = p;
			}
		}

		if (playing == 1) {
			if (Bukkit.getServer().getOnlinePlayers().size() > 1) {
				endOfRound = Long.MAX_VALUE;
				Bukkit.getServer().broadcastMessage(ChatColor.GOLD + winner.getName() + " wins!");
				scheduler.cancelTask(taskID);
				return;
			}
		} else if (playing == 0) {
			endOfRound = Long.MAX_VALUE;
			Bukkit.getServer().broadcastMessage(ChatColor.RED + "Nobody wins!");
			scheduler.cancelTask(taskID);
			return;

		}
		Bukkit.getServer().broadcastMessage(ChatColor.LIGHT_PURPLE + "" + playing + ChatColor.WHITE + " players remain...");

		giveBlocks();
		endOfRound = System.currentTimeMillis() + 1000 * 60 * 5;
		found.clear();

	}

	public void giveBlocks() {
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (p.getGameMode() == GameMode.SURVIVAL) {
				Material material = blocklist.get((int) Math.floor(Math.random() * blocklist.size()));
				blocks.put(p, material);

				p.sendMessage(ChatColor.GREEN + "Your next block is: " + ChatColor.GOLD + getFancyName(material));
			}
		}

	}

	public void endRound() {
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			if (!found.contains(p)) {
				p.setGameMode(GameMode.SPECTATOR);
				if (blocks.containsKey(p)) {
					Material material = blocks.get(p);
					Bukkit.getServer().broadcastMessage(ChatColor.RED + p.getName() + ChatColor.WHITE + " did not find "
							+ ChatColor.GOLD + getFancyName(material) + ChatColor.WHITE + " in time!");
				}
			}
		}

		startRound();
	}

	public void start() {
		rounds = 0;
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			p.setGameMode(GameMode.SURVIVAL);
		}
		Bukkit.getWorlds().forEach(w -> w.setTime(0));

		startRound();

		taskID  = scheduler.scheduleSyncRepeatingTask(this, () -> {

			int playing = 0;
			for (Player p : Bukkit.getServer().getOnlinePlayers()) {
				if (blocks.containsKey(p) && p.getGameMode() == GameMode.SURVIVAL) {
					playing++;
					Material material = blocks.get(p);
					Block block = p.getLocation().subtract(0, 1, 0).getBlock();
					if (block.getType() == material) {
						found.add(p);
						Bukkit.getServer().broadcastMessage(ChatColor.GREEN + p.getName() + ChatColor.WHITE + " found "
								+ ChatColor.GOLD + getFancyName(material));
						p.getWorld().playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
					}
				}
			}

			if (System.currentTimeMillis() > endOfRound || found.size() == playing) {
				endRound();
			}
		}, 0L, 20L);
	}

	public static String getFancyName(Material m) {
		return m.name().toLowerCase().replace("_", " ");
	}
}
