package vg.civcraft.mc.citadel.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import vg.civcraft.mc.citadel.Citadel;
import vg.civcraft.mc.citadel.reinforcement.MultiBlockReinforcement;
import vg.civcraft.mc.citadel.reinforcement.NaturalReinforcement;
import vg.civcraft.mc.citadel.reinforcement.PlayerReinforcement;
import vg.civcraft.mc.citadel.reinforcement.Reinforcement;
import vg.civcraft.mc.namelayer.GroupManager;
import vg.civcraft.mc.namelayer.NameLayerPlugin;
import vg.civcraft.mc.namelayer.group.Group;

public class CitadelReinforcementData {

	private Database db;
	private Citadel plugin = Citadel.getInstance();
	public CitadelReinforcementData(Database db){
		this.db = db;
		if (db.connect()){
			initalizePreparedStatements();
			createTables();
			intitializeProcedures();
		}
	}

	private void intitializeProcedures(){
		db.execute("drop procedure if exists insertReinID;");
		db.execute("create definer=current_user procedure insertReinID(" +
				"in x int,"
				+ "in y int,"
				+ "in z int,"
				+ "in chunk_id varchar(255),"
				+ "in world varchar(255)" +
				") sql security invoker begin "
				+ "insert into reinforcement_id(x, y, z, chunk_id, world) values (x, y, z, chunk_id, world);"
				+ "select LAST_INSERT_ID() as id;"
				+ "end;");
		db.execute("drop procedure if exists insertCustomReinID;");
		db.execute("create definer=current_user procedure insertCustomReinID("
				+ "in rein_id int," +
				"in x int,"
				+ "in y int,"
				+ "in z int,"
				+ "in chunk_id varchar(255),"
				+ "in world varchar(255)" +
				") sql security invoker begin "
				+ "insert into reinforcement_id(rein_id, x, y, z, chunk_id, world) values (rein_id, x, y, z, chunk_id, world);"
				+ "select LAST_INSERT_ID() as id;"
				+ "end;");
	}
	/**
	 * Creates the required mysql tables and updates the db if needed.
	 */
	private void createTables(){
		int ver = checkVersion(plugin.getName());
		long begin_time = System.currentTimeMillis();
		db.execute("create table if not exists db_version (db_version int not null," +
				"update_time varchar(24),"
				+ "plugin_name varchar(40));");
		if (ver == 0){
			db.execute(String.format("update db_version set plugin_name = '%s' " +
					"where plugin_name is null", plugin.getName()));
			ver = checkVersion(plugin.getName());
			if (ver == 0){
				Citadel.Log("Creating tables from scratch, this is a new " +
						"instance of citadel.");
			}
			else{
				Citadel.Log("Detected previous version of Citadel, " +
						"updating to new db.\n" +
						"You should have backed up your data, " +
						"if you havent already you have ten seconds to" +
						" shut down the server and perform a backup.");
				try {
					Thread.sleep(10000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				long first_time = System.currentTimeMillis();
				db.execute("alter table reinforcement drop security_level, drop version, add group_id int not null;");
				/*
				Citadel.Log("Remapping from material id to material name " +
						"in process.");
				db.execute("create table if not exists material_mapping("
						+ "" +
						"material varchar(40) not null," +
						"material_id int not null);");
				db.execute("create index x on material_mapping (material_id);");
				for (Material mat: Material.values()){
					PreparedStatement insert = db.prepareStatement
							("insert into material_mapping(" +
							"material, material_id) values(?,?);");
					try {
						insert.setString(1, mat.name());
						insert.setInt(2, mat.getId());
						insert.execute();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				db.execute("alter table reinforcement add " +
						"material varchar(10)");
				db.execute("update reinforcement r " +
						"inner join material_mapping m on m.material_id = r.material_id " +
						"set r.material = m.material;");
				db.execute("alter table reinforcement drop material_id;");
				db.execute("drop table material_mapping;");
				*/
				db.execute("insert into faction_id (group_name) values (null);"); // For natural reinforcements
				db.execute("delete from reinforcement where `name` is null;");
				db.execute("update ignore reinforcement r inner join faction_id f on f.group_name = r.`name` "
						+ "set r.group_id = f.group_id;");
				db.execute("alter table reinforcement drop `name`, add rein_type_id int not null default 1, "
						+ "add lore varchar(255);");
				db.execute("drop table citadel_account_id_map;");
				Citadel.Log("The update to new format took " + (System.currentTimeMillis() - first_time) / 1000 + " seconds.");
			}
		}
		if (ver == 5 || ver == 0){
			long first_time = System.currentTimeMillis();
			Citadel.Log("Updating to Citadel Version 6.");
			db.execute("create table if not exists reinforcement(" +
					"x int not null," +
					"y int not null," +
					"z int not null," +
					"world varchar(10) not null," +
					"material_id int not null," +
					"durability varchar(10) not null," +
					"chunk_id varchar(255) not null," +
					"insecure tinyint(1) not null," +
					"group_id int not null," +
					"maturation_time int not null," +
					"rein_type_id int not null," +
					"lore varchar(255),"
					+ "primary key (x,y,z,world));");
			db.execute("create table if not exists reinforcement_type("
					+ "rein_type_id int not null auto_increment,"
					+ "rein_type varchar(30) not null,"
					+ "primary key rein_type_key (rein_type_id));");
			String[] types = {"PlayerReinforcement", "NaturalReinforcement", "MultiBlockReinforcement"};
			for (String x: types)
				db.execute(String.format("insert into reinforcement_type(rein_type)"
						+ "values('%s');", x));
			updateVersion(5, plugin.getName());
			ver = checkVersion(plugin.getName());
			Citadel.Log("The update to Version 6 took " + (System.currentTimeMillis() - first_time) / 1000 + " seconds.");
		}
		if (ver == 6){
			long first_time = System.currentTimeMillis();
			Citadel.Log("Updating to version 7. No fun message for you :(");
			db.execute("create table if not exists reinforcement_id("
					+ "rein_id int not null auto_increment,"
					+ "x int not null,"
					+ "y int not null,"
					+ "z int not null,"
					+ "chunk_id varchar(255),"
					+ "world varchar (255) not null,"
					+ "primary key rein_id_key (rein_id),"
					+ "unique key x_y_z_world(x,y,z,world));-- Your mother is a whore and sleeps with banjos"); 
			// I like turtles mother fucker. Never program because then you get turtles.
			db.execute("insert into reinforcement_id (x, y, z, chunk_id, world) select x, y, z, chunk_id, world from reinforcement;"); // populate that bitch.
			db.execute("alter table reinforcement add rein_id int not null, drop chunk_id;");
			db.execute("update reinforcement r inner join reinforcement_id ri on "
					+ "ri.x = r.x and ri.y = r.y and ri.z = r.z and ri.world = r.world "
					+ "set r.rein_id = ri.rein_id");
			db.execute("alter table reinforcement DROP PRIMARY KEY, "
					+ "add primary key rein_id_key(rein_id), "
					+ "drop x,"
					+ "drop y,"
					+ "drop z,"
					+ "drop world;");
			db.execute("alter table reinforcement_id add index `chunk_id_index` (chunk_id);");
			updateVersion(ver, plugin.getName());
			ver = checkVersion(plugin.getName());
			Citadel.Log("The update to Version 7 took " + (System.currentTimeMillis() / first_time) / 1000 + " seconds.");
		}
		if (ver == 7){
			long first_time = System.currentTimeMillis();
			db.execute("alter table reinforcement_id drop primary key,"
					+ " add primary key (rein_id, x, y, z, world);");
			updateVersion(ver, plugin.getName());
			ver = checkVersion(plugin.getName());
			Citadel.Log("The update to Version 8 took " + (System.currentTimeMillis() / first_time) / 1000 + " seconds.");
		}
		Citadel.Log("The total time it took Citadel to update was " + 
				(System.currentTimeMillis() - begin_time) / 1000 + " seconds.");
	}
	/**
	 * Reconnects and reinitializes the mysql connection and preparedstatements.
	 */
	private void reconnectAndReinitialize(){
		if (db.isConnected())
			return;
		db.connect();
		initalizePreparedStatements();
	}
	
	private String version, updateVersion;
	private String getRein, getReins, addRein, removeRein, updateRein;
	//private PreparedStatement deleteGroup, insertDeleteGroup, removeDeleteGroup, getDeleteGroup;
	private String insertReinID, insertCustomReinID, getCordsbyReinID, selectReinCountForGroup, selectReinCount;
	/**
	 * Initializes the PreparedStatements. Gets called on db connect or
	 * reconnect.
	 */
	private void initalizePreparedStatements(){
		getRein = "select r.material_id, r.durability, " +
				"r.insecure, r.maturation_time, rt.rein_type, "
				+ "r.lore, r.group_id, r.rein_id from reinforcement r "
				+ "inner join reinforcement_id ri on r.rein_id = ri.rein_id "
				+ "inner join reinforcement_type rt on rt.rein_type_id = r.rein_type_id "
				+ "where ri.x = ? and ri.y = ? and ri.z = ? and ri.chunk_id = ? and ri.world = ?";
		getReins = "select ri.x, ri.y, ri.z, ri.world, r.material_id, r.durability, " +
				"r.insecure, r.maturation_time, rt.rein_type, "
				+ "r.lore, r.group_id, r.rein_id from reinforcement r "
				+ "inner join reinforcement_id ri on r.rein_id = ri.rein_id "
				+ "inner join reinforcement_type rt on rt.rein_type_id = r.rein_type_id "
				+ "where ri.chunk_id = ?";
		addRein = "insert into reinforcement ("
				+ "material_id, durability, "
				+ "insecure, group_id, maturation_time, rein_type_id,"
				+ "lore, rein_id) select ?, ?, ?, ?, ?, rt.rein_type_id, ?, ? "
				+ "from reinforcement_type rt where rt.rein_type = ?";
		removeRein = "delete r.*, ri.* from reinforcement r "
				+ "left join reinforcement_id ri on r.rein_id = ri.rein_id "
				+ "where ri.x = ? and ri.y = ? and ri.z = ? and ri.world = ?";
		updateRein = "update reinforcement r "
				+ "inner join reinforcement_id ri on ri.rein_id = r.rein_id "
				+ "set r.durability = ?, r.insecure = ?, r.group_id = ?, "
				+ "maturation_time = ? "
				+ "where ri.x = ? and ri.y = ? and ri.z = ? and ri.world =?";
		/*
		deleteGroup = db.prepareStatement("call deleteGroup(?)");
		insertDeleteGroup = db.prepareStatement("insert into toDeleteReinforecments(group_id) select g.group_id from faction_id g "
				+ "where g.group_name = ?");
		removeDeleteGroup = db.prepareStatement("delete from toDeleteReinforecments where group_id = (select f.group_id from "
				+ "faction_id f where f.group_name = ?)");
		getDeleteGroup = db.prepareStatement("select f.group_name from faction_id f "
				+ "inner join toDeleteReinforecments d on f.group_id = d.group_id");
		*/
		
		insertReinID = "call insertReinID(?,?,?,?,?)";
		insertCustomReinID = "call insertCustomReinID(?,?,?,?,?,?)";
		getCordsbyReinID = "select x, y, z, world from reinforcement_id where rein_id = ?";
		selectReinCountForGroup = "select count(*) as count from reinforcement r "
				+ "inner join faction_id f on f.group_id = r.group_id "
				+ "where f.group_name = ?";
		selectReinCount = "select count(*) as count from reinforcement r";
		
		version = "select max(db_version) as db_version from db_version where plugin_name=?";
		updateVersion = "insert into db_version (db_version, update_time, plugin_name) values (?,?,?)";
	}
	
	/**
	 * Checks the version of a specific plugin's db.
	 * @param name- The name of the plugin.
	 * @return Returns the version of the plugin or 0 if none was found.
	 */
	public int checkVersion(String name){
		reconnectAndReinitialize();
		PreparedStatement version = db.prepareStatement(this.version);
		try {
			version.setString(1, name);
			ResultSet set = version.executeQuery();
			if (!set.next()) 
				return 0;
			return set.getInt("db_version");
		} catch (SQLException e) {
			// table doesnt exist
			return 0;
		}
	}
	
	/**
	 * Updates the version number for a plugin. You must specify what 
	 * the current version number is.
	 * @param version- The current version of the plugin.
	 * @param pluginname- The plugin name.
	 * @return Returns the new version of the db.
	 */
	public synchronized int updateVersion(int version, String pluginname){
		reconnectAndReinitialize();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		PreparedStatement updateVersion = db.prepareStatement(this.updateVersion);
		try {
			updateVersion.setInt(1, version+ 1);
			updateVersion.setString(2, sdf.format(new Date()));
			updateVersion.setString(3, pluginname);
			updateVersion.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ++version;
	}
	
	/**
	 * Is used to grab the reinforcement from the mysql db.
	 * If there isn't a reinforcement at the location it
	 * returns null.
	 * @param The Location of the wanted Reinforcement.
	 * @return Returns the Reinforcement of the location.
	 * @return Returns null if there is no reinforcement.
	 */
	public Reinforcement getReinforcement(Location loc){
		reconnectAndReinitialize();
		PreparedStatement getRein = db.prepareStatement(this.getRein);
		try {
			int x = loc.getBlockX();
			int y = loc.getBlockY();
			int z = loc.getBlockZ();
			getRein.setInt(1, x);
			getRein.setInt(2, y);
			getRein.setInt(3, z);
			String formatChunk = formatChunk(loc);
			getRein.setString(4, formatChunk);
			getRein.setString(5, loc.getWorld().getName());
			ResultSet set = getRein.executeQuery();
			if (!set.next()){
				set.close();
				return null;
			}
			Material mat = Material.getMaterial(set.getInt(1));
			int durability = set.getInt(2);
			boolean inSecure = set.getBoolean(3);
			int mature = set.getInt(4);
			String rein_type = set.getString(5);
			String lore = set.getString(6);
			int group_id = set.getInt(7);
			// Check for what type of reinforcement and return the one
			// needed.
			if (rein_type.equals("PlayerReinforcement")){
				ItemStack stack = new ItemStack(mat);
				if (lore != null){
					ItemMeta meta = stack.getItemMeta();
					List<String> array = Arrays.asList(lore.split("\n"));
					meta.setLore(array);
					stack.setItemMeta(meta);
				}
				PlayerReinforcement rein =
						new PlayerReinforcement(loc, durability,
								mature, GroupManager.getGroup(group_id),
								stack);
				rein.setInsecure(inSecure);
				set.close();
				return rein;
			}
			else if(rein_type.equals("NaturalReinforcement")){
				NaturalReinforcement rein = 
						new NaturalReinforcement(loc.getBlock(), durability);
				set.close();
				return rein;
			}
			else if (rein_type.equals("MultiBlockReinforcement")){
				int id = set.getInt(8);
				set.close();
				MultiBlockReinforcement rein = MultiBlockReinforcement.getMultiRein(id);
				if (rein != null)
					return rein;
				PreparedStatement getCordsbyReinID = db.prepareStatement(this.getCordsbyReinID);
				getCordsbyReinID.setInt(1, id);
				set = getCordsbyReinID.executeQuery();
				List<Location> locs = new ArrayList<Location>();
				while (set.next()){
					int xx = set.getInt(1), yy = set.getInt(2), zz = set.getInt(3);
					String world = set.getString(4);
					locs.add(new Location(Bukkit.getWorld(world), xx, yy, zz));
				}
				set.close();
				
				rein = new MultiBlockReinforcement(locs, GroupManager.getGroup(group_id), durability, mature, id);
				return rein;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns a list of reinforcements in a given chunk.
	 * @param The chunk you want the reinforcements about.
	 * @return A list of reinforcements in a chunk
	 */
	public List<Reinforcement> getReinforcements(Chunk chunk){
		reconnectAndReinitialize();
		PreparedStatement getReins = db.prepareStatement(this.getReins);
		String formatChunk = formatChunk(chunk);
		List<Reinforcement> reins = new ArrayList<Reinforcement>();
		try {
			getReins.setString(1, formatChunk);
			ResultSet set = getReins.executeQuery();
			while (set.next()){
				int x = set.getInt(1), y = set.getInt(2), z = set.getInt(3);
				String world = set.getString(4);
				Material mat = Material.getMaterial(set.getInt(5));
				int durability = set.getInt(6);
				boolean inSecure = set.getBoolean(7);
				int mature = set.getInt(8);
				String rein_type = set.getString(9);
				String lore = set.getString(10);
				int group_id = set.getInt(11);
				Group g = GroupManager.getGroup(group_id);
				
				Location loc = new Location(Bukkit.getWorld(world), x, y, z);
				
				if (rein_type.equals("PlayerReinforcement")){
					ItemStack stack = new ItemStack(mat);
					if (lore != null){
						ItemMeta meta = stack.getItemMeta();
						List<String> array = Arrays.asList(lore.split("\n"));
						meta.setLore(array);
						stack.setItemMeta(meta);
					}
					PlayerReinforcement rein =
							new PlayerReinforcement(loc, durability,
									mature, g,
									stack);
					rein.setInsecure(inSecure);
					reins.add(rein);
				}
				else if(rein_type.equals("NaturalReinforcement")){
					NaturalReinforcement rein = 
							new NaturalReinforcement(loc.getBlock(), durability);
					reins.add(rein);
				}
				else if (rein_type.equals("MultiBlockReinforcement")){
					int id = set.getInt(12);
					MultiBlockReinforcement rein = MultiBlockReinforcement.getMultiRein(id);
					if (rein != null)
						reins.add(rein);
					PreparedStatement getCordsbyReinID = db.prepareStatement(this.getCordsbyReinID);
					getCordsbyReinID.setInt(1, id);
					ResultSet multi = getCordsbyReinID.executeQuery();
					List<Location> locs = new ArrayList<Location>();
					while (multi.next()){
						int xx = multi.getInt(1), yy = multi.getInt(2), zz = multi.getInt(3);
						String w = multi.getString(4);
						locs.add(new Location(Bukkit.getWorld(w), xx, yy, zz));
					}
					multi.close();
					
					rein = new MultiBlockReinforcement(locs, g, durability, mature, id);
					reins.add(rein);
				}
			}
			set.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return reins;
	}
	
	/**
	 * Inserts a reinforcement into the Database.  Should only be called from
	 * SaveManager.
	 * @param The Reinforcement to save.
	 */
	public void insertReinforcement(Reinforcement rein){
		reconnectAndReinitialize();
		
		if (rein instanceof PlayerReinforcement){
			Location loc = rein.getLocation();
			int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
			String world = loc.getWorld().getName();
			Material mat = rein.getMaterial();
			int dur = rein.getDurability();
			int maturationTime = rein.getMaturationTime();
			boolean insecure = false;
			String group = null;
			String reinType = "";
			String lore = "";
			PlayerReinforcement pRein = (PlayerReinforcement) rein;
			insecure = pRein.isInsecure();
			ItemMeta meta = pRein.getStackRepresentation().getItemMeta();
			if (meta.hasLore())
				for (String xx: meta.getLore())
					lore += xx + "\n";
			else
				lore = null;
			group = pRein.getGroup().getName();
			reinType = "PlayerReinforcement";
			try {
				PreparedStatement insertReinID = db.prepareStatement(this.insertReinID);
				insertReinID.setInt(1, x);
				insertReinID.setInt(2, y);
				insertReinID.setInt(3, z);
				String formatChunk = formatChunk(loc);
				insertReinID.setString(4, formatChunk);
				insertReinID.setString(5, world);
				ResultSet set = insertReinID.executeQuery();
				set.next();
				
				int id = set.getInt("id");
				
				PreparedStatement addRein = db.prepareStatement(this.addRein);
				addRein.setInt(1, mat.getId());
				addRein.setInt(2, dur);
				addRein.setBoolean(3, insecure);
				addRein.setInt(4, pRein.getGroupId());
				addRein.setInt(5, maturationTime);
				addRein.setString(6, lore);
				addRein.setInt(7, id);
				addRein.setString(8, reinType);
				addRein.execute();
			} catch (SQLException e) {
				Citadel.Log("Citadel has detected a reinforcement that should not be there. Deleting it and trying again. "
						+ "Including the stack incase it is useful.\n" + e.getStackTrace());
				// Let's delete the reinforcement because if a user 
				//is able to place one then the db is messed up some how.
				deleteReinforcement(rein);
				// Now lets try again.
				insertReinforcement(rein);
			}
		}
		else if (rein instanceof NaturalReinforcement){
			/* 
			 * We don't need to worry about saving this right now.
			 * 
			Location loc = rein.getLocation();
			int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
			String world = loc.getWorld().getName();
			Material mat = rein.getMaterial();
			int dur = rein.getDurability();
			String chunk_id = loc.getChunk().toString();
			int maturationTime = rein.getMaturationTime();
			boolean insecure = false;
			String group = NameLayerPlugin.getSpecialAdminGroup();
			String reinType = "NaturalReinforcement";
			String lore = "";
			lore = null;
			try {
				PreparedStatement insertReinID = db.prepareStatement(this.insertReinID);
				insertReinID.setInt(1, x);
				insertReinID.setInt(2, y);
				insertReinID.setInt(3, z);
				String formatChunk = formatChunk(loc);
				insertReinID.setString(4, formatChunk);
				insertReinID.setString(5, world);
				insertReinID.execute();
				
				int id = getLastReinId();
				
				PreparedStatement addRein = db.prepareStatement(this.addRein);
				addRein.setInt(1, mat.getId());
				addRein.setInt(2, dur);
				addRein.setBoolean(3, insecure);
				addRein.setInt(4, maturationTime);
				addRein.setString(5, lore);
				addRein.setInt(6, id);
				addRein.setString(7, reinType);
				addRein.setString(8, group);
				addRein.execute();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			*/
		}
		else if (rein instanceof MultiBlockReinforcement){
			MultiBlockReinforcement mbRein = (MultiBlockReinforcement) rein;

			int id = -1; // We add one because we haven't added it yet.
			try {
				PreparedStatement insertCustomReinID = db.prepareStatement(this.insertCustomReinID);
				// add all the locations into the db.
				int count = 0;
				for (Location lo: mbRein.getLocations()){
					if (count == 0) {
						PreparedStatement insertReinID = db.prepareStatement(this.insertReinID);
						insertReinID.setInt(1, lo.getBlockX());
						insertReinID.setInt(2, lo.getBlockY());
						insertReinID.setInt(3, lo.getBlockZ());
						String formatChunk = formatChunk(lo);
						insertReinID.setString(4, formatChunk);
						insertReinID.setString(5, lo.getWorld().getName());
						ResultSet set = insertReinID.executeQuery();
						set.next();
						
						id = set.getInt("id");
						mbRein.setReinId(id);
						continue;
					}
					insertCustomReinID.setInt(1, id);
					insertCustomReinID.setInt(2, lo.getBlockX());
					insertCustomReinID.setInt(3, lo.getBlockY());
					insertCustomReinID.setInt(4, lo.getBlockZ());
					String formatChunk = formatChunk(lo);
					insertCustomReinID.setString(5, formatChunk);
					insertCustomReinID.setString(6, lo.getWorld().getName());
					insertCustomReinID.addBatch();
				}
				insertCustomReinID.executeBatch();
				
				PreparedStatement addRein = db.prepareStatement(this.addRein);
				addRein.setInt(1, -1);
				addRein.setInt(2, mbRein.getDurability());
				addRein.setBoolean(3, false);
				addRein.setInt(4, mbRein.getGroupId());
				addRein.setInt(5, mbRein.getMaturationTime());
				addRein.setString(6, null);
				addRein.setInt(7, id);
				addRein.setString(8, "MultiBlockReinforcement");
				addRein.execute();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	/**
	 * Deletes a Reinforcement from the database. Should only be called
	 * within SaveManager
	 * @param The Reinforcement to delete.
	 */
	public void deleteReinforcement(Reinforcement rein){
		reconnectAndReinitialize();
		
		Location loc = rein.getLocation();
		int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
		String world = loc.getWorld().getName();
		try {
			PreparedStatement removeRein = db.prepareStatement(this.removeRein);
			removeRein.setInt(1, x);
			removeRein.setInt(2, y);
			removeRein.setInt(3, z);
			removeRein.setString(4, world);
			removeRein.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Saves the Reinforcement to the Database. Should only be called
	 * from SaveManager.
	 * @param The Reinforcement to save.
	 */
	public void saveReinforcement(Reinforcement rein){
		reconnectAndReinitialize();
		
		int dur = rein.getDurability();
		boolean insecure = false;
		int groupId = -1;
		int mature = rein.getMaturationTime();
		Location loc = rein.getLocation();
		int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
		String world = loc.getWorld().getName();
		if (rein instanceof PlayerReinforcement){
			PlayerReinforcement pRein = (PlayerReinforcement) rein;
			insecure = pRein.isInsecure();
			groupId = pRein.getGroup().getGroupId();
		}
		if (rein instanceof MultiBlockReinforcement){
			MultiBlockReinforcement mbRein = (MultiBlockReinforcement) rein;
			insecure = false;
			groupId = mbRein.getGroup().getGroupId();
		}
		try {
			PreparedStatement updateRein = db.prepareStatement(this.updateRein);
			updateRein.setInt(1, dur);
			updateRein.setBoolean(2, insecure);
			updateRein.setInt(3, groupId);
			updateRein.setInt(4, mature);
			updateRein.setInt(5, x);
			updateRein.setInt(6, y);
			updateRein.setInt(7, z);
			updateRein.setString(8, world);
			updateRein.execute();
		} catch (SQLException e) {
			Citadel.Log(String.format("The Null Group Exception that is being followed has to deal with the group id: %s,"
					+ " at location: %d, %d, %d, at world: %s", groupId, x, y, z, world));
			e.printStackTrace();
		}
	}
	
	/*
	/**
	 * Begins deleting the reinforcements at a group. Once this is executed
	 * the specified group loses all reinforcements. They are removed from map,
	 * no longer able to be bypassed or broken.
	 * @param The Group name that is being removed.
	 * @return Returns true if there are more records to remove, false otherwise.
	 *
	public boolean deleteGroup(String group){
		reconnectAndReinitialize();
		
		try {
			deleteGroup.setString(1, group);
			ResultSet set = deleteGroup.executeQuery();
			set.next();
			return set.getInt(1) > 0;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	
	public void insertDeleteGroup(String group){
		reconnectAndReinitialize();
		
		try {
			insertDeleteGroup.setString(1, group);
			insertDeleteGroup.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public List<Group> getDeleteGroups(){
		reconnectAndReinitialize();
		
		List<Group> groups = new ArrayList<Group>();
		try {
			ResultSet set = getDeleteGroup.executeQuery();
			while (set.next())
				groups.add(GroupManager.getGroup(set.getString(1)));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return groups;
	}
	
	public void removeDeleteGroup(String group){
		reconnectAndReinitialize();
		
		try {
			removeDeleteGroup.setString(1, group);
			removeDeleteGroup.execute();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	*/
	
	private String formatChunk(Location loc){
		String chunk = loc.getWorld().getName();
		Chunk c = loc.getChunk();
		chunk += ":" + c.getX() + ":" + c.getZ();
		return chunk;
	}
	
	private String formatChunk(Chunk c){
		String chunk = c.getWorld().getName();
		chunk += ":" + c.getX() + ":" + c.getZ();
		return chunk;
	}
	
	public int getReinCountForGroup(String group){
		try {
			PreparedStatement selectReinCountForGroup = db.prepareStatement(this.selectReinCountForGroup);
			selectReinCountForGroup.setString(1, group);
			ResultSet set = selectReinCountForGroup.executeQuery();
			set.next();
			set.close();
			return set.getInt(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	public int getReinCountForAllGroups(){
		try {
			PreparedStatement selectReinCount = db.prepareStatement(this.selectReinCount);
			ResultSet set = selectReinCount.executeQuery();
			set.next();
			set.close();
			return set.getInt(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
}
