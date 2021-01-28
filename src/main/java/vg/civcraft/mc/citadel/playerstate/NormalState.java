package vg.civcraft.mc.citadel.playerstate;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.CitadelUtility;
import vg.civcraft.mc.citadel.reinforcementtypes.ReinforcementType;
import vg.civcraft.mc.namelayer.core.Group;
import vg.civcraft.mc.namelayer.mc.GroupAPI;

public class NormalState extends AbstractPlayerState {

	public NormalState(Player p) {
		super(p);
	}

	@Override
	public String getName() {
		return ChatColor.GREEN + "Normal mode";
	}

	@Override
	public void handleBlockPlace(BlockPlaceEvent e) {
		Player player = e.getPlayer();
		if (!Citadel.getInstance().getSettingManager().isInEasyMode(player.getUniqueId())) {
			return;
		}
		ItemStack offHand = player.getInventory().getItemInOffHand();
		ReinforcementType type = Citadel.getInstance().getReinforcementTypeManager().getByItemStack(offHand);
		if (type == null) {
			return;
		}
		Group defaultGroup = GroupAPI.getDefaultGroup(player);
		if (defaultGroup == null) {
			return;
		}
		CitadelUtility.attemptReinforcementCreation(e.getBlock(), type, defaultGroup, player);
	}

	@Override
	public void handleInteractBlock(PlayerInteractEvent e) {
		if (e.getAction() != Action.LEFT_CLICK_BLOCK) {
			return;
		}
		Player player = e.getPlayer();
		if (!Citadel.getInstance().getSettingManager().isInEasyMode(player.getUniqueId())) {
			return;
		}
		ItemStack hand = player.getInventory().getItemInMainHand();
		ReinforcementType type = Citadel.getInstance().getReinforcementTypeManager().getByItemStack(hand);
		if (type == null) {
			return;
		}
		Group defaultGroup = GroupAPI.getDefaultGroup(player);
		if (defaultGroup == null) {
			return;
		}
		CitadelUtility.attemptReinforcementCreation(e.getClickedBlock(), type, defaultGroup, player);
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof NormalState;
	}

	@Override
	public String getOverlayText() {
		return null;
	}

}
