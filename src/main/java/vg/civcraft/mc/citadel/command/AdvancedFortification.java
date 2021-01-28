package vg.civcraft.mc.citadel.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.playerstate.AbstractPlayerState;
import vg.civcraft.mc.citadel.playerstate.AdvancedFortificationState;
import vg.civcraft.mc.citadel.playerstate.PlayerStateManager;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.civmodcore.command.CivCommand;
import vg.civcraft.mc.civmodcore.command.StandaloneCommand;
import vg.civcraft.mc.namelayer.core.Group;
import vg.civcraft.mc.namelayer.mc.GroupAPI;
import vg.civcraft.mc.namelayer.mc.commands.NameLayerTabCompletion;

@CivCommand(id = "cta")
public class AdvancedFortification extends StandaloneCommand {

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Player player = (Player) sender;
		PlayerStateManager stateManager = Citadel.getInstance().getStateManager();
		AbstractPlayerState currentState = stateManager.getState(player);
		AdvancedFortificationState advFortState = null;
		if (currentState instanceof AdvancedFortificationState) {
			advFortState = (AdvancedFortificationState) currentState;
			if (args.length == 0) {
				stateManager.setState(player, null);
				return true;
			}
		}
		ItemStack mainHand = player.getInventory().getItemInMainHand();
		if (mainHand.getType() == Material.AIR) {
			CitadelUtility.sendAndLog(player, ChatColor.RED,
					"You need to hold an item in your main hand to specify the block type to reinforce");
			return true;
		}
		ItemStack offHand = player.getInventory().getItemInOffHand();
		if (offHand.getType() == Material.AIR) {
			CitadelUtility.sendAndLog(player, ChatColor.RED, "You need to hold a reinforcement item in your off hand");
			return true;
		}
		ReinforcementType type = Citadel.getInstance().getReinforcementTypeManager().getByItemStack(offHand);
		if (type == null) {
			CitadelUtility.sendAndLog(player, ChatColor.RED, "You can not reinforce with the item in your off hand");
			return true;
		}
		if (!type.canBeReinforced(mainHand.getType())) {
			CitadelUtility.sendAndLog(player, ChatColor.AQUA,
					type.getName() + ChatColor.RED + " can not reinforce " + mainHand.getType().name());
			return true;
		}
		Group group = null;
		if (args.length == 0) {
			group = GroupAPI.getDefaultGroup(player);
			if (group == null) {
				CitadelUtility.sendAndLog(player, ChatColor.RED,
						"You don't have a default group and can thus not use this command without specifying a group");
				return true;
			}
		} else {
			group = GroupAPI.getGroup(args[0]);
		}
		if (group == null) {
			CitadelUtility.sendAndLog(player, ChatColor.RED, "The group " + args[0] + " does not exist.");
			return true;
		}
		boolean hasAccess = GroupAPI.hasPermission(player.getUniqueId(), group, Citadel.getInstance().getPermissionHandler().getReinforce());
		if (!hasAccess) {
			CitadelUtility.sendAndLog(player, ChatColor.RED, "You do not have permission to reinforce on " + group.getName());
			return true;
		}
		if (advFortState == null) {
			advFortState =  new AdvancedFortificationState(player);
			stateManager.setState(player, advFortState);
		}
		advFortState.addSetup(mainHand, type, group);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (args.length == 0)
			return NameLayerTabCompletion.completeGroupName("", (Player) sender);
		else if (args.length == 1)
			return NameLayerTabCompletion.completeGroupName(args[0], (Player) sender);
		else {
			return new ArrayList<>();
		}
	}

}
