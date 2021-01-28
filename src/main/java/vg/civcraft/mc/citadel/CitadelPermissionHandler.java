package vg.civcraft.mc.citadel;

import vg.civcraft.mc.namelayer.core.DefaultPermissionLevel;
import vg.civcraft.mc.namelayer.core.PermissionTracker;
import vg.civcraft.mc.namelayer.core.PermissionType;
import vg.civcraft.mc.namelayer.mc.GroupAPI;

public class CitadelPermissionHandler {

	private final static String CHEST_PERM = "CONTAINER_ACCESS";
	private final static String BYPASS_PERM = "BYPASS_REINFORCEMENT";
	private final static String CROPS_PERM = "HARVEST_CROPS";
	private final static String INSECURE_PERM = "INSECURE_REINFORCEMENT";
	private final static String REINFORCE_PERM = "REINFORCE";
	private final static String DOOR_PERM = "USE_DOORS";
	private final static String ACID_PERM = "USE_ACID_BLOCKS";
	private final static String INFO_PERM = "USE_INFORMATION_MODE";
	private final static String REPAIR_PERM = "ALLOW_REPAIR_REINFORCEMENT";
	private final static String MODIFY_PERM = "ALLOW_MODIFYING_BLOCKS";
	private final static String BEACON_PERM = "USE_BEACONS";
	private final static String HANGING_PLACE_BREAK_PERM = "ALLOWS_CHANGING_HANGING_BLOCKS";
	private final static String ITEM_FRAME_HANDLE_PERM = "ALLOWS_HANDLING_ITEM_FRAMES";
	private final static String ITEM_FRAME_ROTATE_PERM = "ALLOWS_ROTATING_ITEM_FRAMES";

	private PermissionTracker permTracker;

	public CitadelPermissionHandler(PermissionTracker permTracker) {
		this.permTracker = permTracker;
		setup();
	}

	private static void setup() {
		GroupAPI.registerPermission(REINFORCE_PERM, DefaultPermissionLevel.MOD, "Allows reinforcing blocks on this group");
		GroupAPI.registerPermission(ACID_PERM, DefaultPermissionLevel.MOD, "Allows activating acid blocks reinforced on this group");		
		GroupAPI.registerPermission(INFO_PERM, DefaultPermissionLevel.MEMBER, "Allows viewing information on reinforcements reinforced on this group");		
		GroupAPI.registerPermission(BYPASS_PERM, DefaultPermissionLevel.MOD, "Allows bypassing reinforcements reinforced on this group");		
		GroupAPI.registerPermission(REPAIR_PERM, DefaultPermissionLevel.MOD, "Allows repairing reinforcements reinforced on this group");		
		GroupAPI.registerPermission(DOOR_PERM, DefaultPermissionLevel.MEMBER, "Allows opening doors reinforced on this group");		
		GroupAPI.registerPermission(CHEST_PERM, DefaultPermissionLevel.MEMBER, "Allows opening containers like chests reinforced on this group");		
		GroupAPI.registerPermission(CROPS_PERM, DefaultPermissionLevel.MEMBER, "Allows harvesting crops growing on soil reinforced on this group");		
		GroupAPI.registerPermission(INSECURE_PERM, DefaultPermissionLevel.MEMBER, "Allows toggling the insecure flag on reinforcements");		
		GroupAPI.registerPermission(MODIFY_PERM, DefaultPermissionLevel.MOD, "Allows modifying reinforced blocks like flipping levers, stripping logs etc");		
		GroupAPI.registerPermission(BEACON_PERM, DefaultPermissionLevel.MEMBER, "Allow changing beacon effects");		
		GroupAPI.registerPermission(HANGING_PLACE_BREAK_PERM, DefaultPermissionLevel.MOD, "Allows placing/breaking hanging entities on reinforced blocks");		
		GroupAPI.registerPermission(ITEM_FRAME_HANDLE_PERM, DefaultPermissionLevel.MOD, "Allows the placing/removal of items into/from Item Frames");		
		GroupAPI.registerPermission(ITEM_FRAME_ROTATE_PERM, DefaultPermissionLevel.MEMBER, "Allows the rotation of items placed within Item Frames");		
	}

	public PermissionType getModifyBlocks() {
		return permTracker.getPermission(MODIFY_PERM);
	}

	public PermissionType getChests() {
		return permTracker.getPermission(CHEST_PERM);
	}

	public PermissionType getDoors() {
		return permTracker.getPermission(DOOR_PERM);
	}

	public PermissionType getBypass() {
		return permTracker.getPermission(BYPASS_PERM);
	}

	public PermissionType getReinforce() {
		return permTracker.getPermission(REINFORCE_PERM);
	}

	public PermissionType getAcidblock() {
		return permTracker.getPermission(ACID_PERM);
	}

	public PermissionType getCrops() {
		return permTracker.getPermission(CROPS_PERM);
	}

	public PermissionType getInsecure() {
		return permTracker.getPermission(INSECURE_PERM);
	}

	public PermissionType getInfo() {
		return permTracker.getPermission(INFO_PERM);
	}

	public PermissionType getRepair() {
		return permTracker.getPermission(REPAIR_PERM);
	}

	public PermissionType getBeacon() {
		return permTracker.getPermission(BEACON_PERM);
	}

	public PermissionType getHangingPlaceBreak() {
		return permTracker.getPermission(HANGING_PLACE_BREAK_PERM);
	}

	public PermissionType getItemFramePutTake() {
		return permTracker.getPermission(ITEM_FRAME_HANDLE_PERM);
	}

	public PermissionType getItemFrameRotate() {
		return permTracker.getPermission(ITEM_FRAME_ROTATE_PERM);
	}

}
