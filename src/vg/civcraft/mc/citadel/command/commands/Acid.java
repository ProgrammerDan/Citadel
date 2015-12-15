package vg.civcraft.mc.citadel.command.commands;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelConfigManager;
import vg.civcraft.mc.citadel.ReinforcementManager;
import vg.civcraft.mc.citadel.Utility;
import vg.civcraft.mc.citadel.events.AcidBlockEvent;
import vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement;
import vg.civcraft.mc.citadel.reinforcement.Reinforcement;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.NameAPI;

public class Acid extends PlayerCommand {
	private Random rand = new Random();
	private ReinforcementManager rm = Citadel.getReinforcementManager();
	
	public Acid(String name) {
		super(name);
		setIdentifier("ctacid");
		setDescription("Removes the block above it if using an acid block.");
		setUsage("/ctacid");
		setArguments(0, 0);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage("You must be a player to perform this command.");
			return true;
		}
		Player p = (Player) sender;
		Iterator<Block> itr = new BlockIterator(p, 40); // Within 2.5 chunks
		Material acidBlock = CitadelConfigManager.getAcidBlock();
		while (itr.hasNext()) {
			Block block = itr.next();
			if (block.getType() != acidBlock) {
				continue;
			}
			Reinforcement rein = rm.getReinforcement(block);
			if (rein == null) {
				p.sendMessage(ChatColor.RED + "That block is not reinforced.");
				return true;
			}
			if (!(rein instanceof PlayerReinforcement)) { // Just in case.
				// Most chance it is a PlayerReinforcement but otherwise.
				p.sendMessage(ChatColor.RED
						+ "An acid block cannot be a natural reinforcement.");
				return true;
			}
			PlayerReinforcement pRein = (PlayerReinforcement) rein;
			UUID uuid = NameAPI.getUUID(p.getName());
			if (!pRein.getGroup().isMember(uuid)) {
				p.sendMessage(ChatColor.RED
						+ "You do not belong on that group.");
				return true;
			}
			if (!pRein.isBypassable(p)) {
				p.sendMessage(ChatColor.RED
						+ "You do not have sufficient permission "
						+ "to use acid blocks.");
				return true;
			}
			int time = Utility.timeUntilAcidMature(pRein);
			if (time != 0) {
				p.sendMessage(ChatColor.RED + "That acid block is not mature yet.");
				return true;
			}
			Block topFace = block.getRelative(BlockFace.UP);
			if (topFace.getType() == Material.AIR) {
				p.sendMessage(ChatColor.RED
						+ "There is no block above to acid block.");
				return true;
			}
			Reinforcement topRein = rm.getReinforcement(topFace);
			if (topRein == null) {
				p.sendMessage(ChatColor.RED
						+ "That block doesn't have a reinforcement.");
				return true;
			}
			if (!(topRein instanceof PlayerReinforcement)) {
				continue;
			}
			PlayerReinforcement pTopRein = (PlayerReinforcement) topRein;
			ReinforcementType acidBlockType = ReinforcementType
					.getReinforcementType(pRein.getStackRepresentation());
			ReinforcementType topReinType = ReinforcementType
					.getReinforcementType(pTopRein.getStackRepresentation());
			if (acidBlockType.getAcidTime() < topReinType.getAcidTime()) {
				p.sendMessage(ChatColor.RED
						+ "This acid block is too weak for that reinforcement.");
				return true;
			}
			AcidBlockEvent event = new AcidBlockEvent(p, pRein, pTopRein);
			Bukkit.getPluginManager().callEvent(event);
			if (event.isCancelled())
				return true;
			topFace.setType(Material.AIR);

			block.breakNaturally();
			
			// Consider if should simply be an AcidBlockEvent listener. This will do for now.
			Utility.reinforcementBroken(p, pRein);
			rm.deleteReinforcement(pTopRein);
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return new ArrayList<String>();
	}

}
