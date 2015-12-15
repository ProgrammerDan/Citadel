package vg.civcraft.mc.citadel.command.commands;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.PlayerState;
import vg.civcraft.mc.citadel.ReinforcementManager;
import vg.civcraft.mc.citadel.ReinforcementMode;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.GroupManager.PlayerType;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.command.TabCompleters.GroupTabCompleter;
import vg.civcraft.mc.namelayer.group.Group;
import vg.civcraft.mc.namelayer.permission.GroupPermission;
import vg.civcraft.mc.namelayer.permission.PermissionType;

public class Fortification extends PlayerCommand{
	private ReinforcementManager rm = Citadel.getReinforcementManager();
	private GroupManager gm = NameAPI.getGroupManager();

	public Fortification(String name) {
		super(name);
		setIdentifier("ctf");
		setDescription("Allows you to place already reinforced blocks. Use /ctf groupname with a reinforcement material in your hand, then place down blocks and they will automatically be reinforced to groupname.");
		setUsage("/ctf <group>");
		setArguments(0,1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)){
			sender.sendMessage("Must be a player to perform this command.");
			return true;
		}
		Player p = (Player) sender;
		UUID uuid = NameAPI.getUUID(p.getName());
		String groupName = null;
		if(args.length == 0){
			groupName = gm.getDefaultGroup(uuid);
			if(groupName == null){
				p.sendMessage(ChatColor.RED + "You need to fortify to a group! Try /ctf groupname. \n Or use /nlcg groupname if you don't have a group yet.");
				return true;
			}
		}
		else{
			groupName = args[0];
		}
		Group g = gm.getGroup(groupName);	
		if (g == null){
			p.sendMessage(ChatColor.RED + "That group does not exist.");
			return true;
		}
		
		PlayerType type = g.getPlayerType(uuid);
		if (!p.hasPermission("citadel.admin") && !p.isOp() && type == null){
			p.sendMessage(ChatColor.RED + "You are not on this group.");
			return true;
		}
		GroupPermission gPerm = gm.getPermissionforGroup(g);
		if (!p.hasPermission("citadel.admin") && !p.isOp() && !gPerm.isAccessible(type, PermissionType.BLOCKS)){
			p.sendMessage(ChatColor.RED + "You do not have permission to "
					+ "place a reinforcement on this group.");
			return true;
		}
		ItemStack stack = p.getItemInHand();
		PlayerState state = PlayerState.get(p);
		ReinforcementType reinType = ReinforcementType.getReinforcementType(stack);
		if (state.getMode() == ReinforcementMode.REINFOREMENT_FORTIFICATION){
			p.sendMessage(ChatColor.GREEN + state.getMode().name() + " has been disabled");
			state.reset();
		}
		else{
			if (stack.getType() == Material.AIR){
				p.sendMessage(ChatColor.RED + "You need to be holding something to fortify with, try holding a stone block in your hand.");
				return true;
			}
			else if (reinType == null){
				p.sendMessage(ChatColor.RED + "You can't use the item in your hand to reinforce. Try using a stone block.");
				return true;
			}
			p.sendMessage(ChatColor.GREEN + "You are now in Fortification mode, place blocks down and they will be secured with the material in your hand. \n Type /ctf or /cto to turn this off when you are done.");
			state.setMode(ReinforcementMode.REINFOREMENT_FORTIFICATION);
			state.setFortificationItemStack(reinType.getItemStack());
			state.setGroup(g);
		}
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		if (!(sender instanceof Player))
			return null;

		if (args.length == 0)
			return GroupTabCompleter.complete(null, PermissionType.BLOCKS, (Player)sender);
		else if (args.length == 1)
			return GroupTabCompleter.complete(args[0], PermissionType.BLOCKS, (Player)sender);
		else{
			return new ArrayList<String>();
		}
	}

}
