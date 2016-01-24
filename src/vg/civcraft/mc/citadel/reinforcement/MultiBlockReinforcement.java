package vg.civcraft.mc.citadel.reinforcement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.namelayer.NameAPI;
import vg.civcraft.mc.namelayer.group.Group;

public class MultiBlockReinforcement extends Reinforcement{

	List<Location> locs;
	private Group g;
	private static Map<Integer, MultiBlockReinforcement> reins = 
			new HashMap<Integer, MultiBlockReinforcement>();
	
	public MultiBlockReinforcement(List<Location> locs, Group g, int dur, 
			int creation, int multiBlockId) {
		super(null, null, dur, creation);
		this.g = g;
		this.locs = locs;
	}
	
	@Override
	/**
	 * @return Returns the first location in the list of locations.
	 */
	public Location getLocation(){
		return locs.get(0);
	}
	
	public List<Location> getLocations(){
		return locs;
	}
	
	public void setGroup(Group g){
		this.g = g;
		isDirty = true;
	}
	
	public Group getGroup(){
		return g;
	}
	/**
	 * This method is used to get the MultiblockReinforcement from a
	 * particular id. The id is the one from the db.
	 * @param id- The unique identifier for the MultiBlock.
	 * @return Returns the MultiBlockReinforcement.
	 */
	public static MultiBlockReinforcement getMultiRein(int id){
		return reins.get(id);
	}
	
	/**
     * Note this is different to the group id of a Group.
     * If a reinforcement is made with Group 1 and then is merged into Group 0 then this will return the group_id
     * of Group 1.
     * @return Returns the value of the group_id from the group it was created with.
     */
    public int getGroupId(){
    	return g.getGroupId();
    }
    
    public void setReinId(int id) {
		reins.put(id, this);
    }
}
