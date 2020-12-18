package redempt.redlib.multiblock;

import static redempt.redlib.RedLib.midVersion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import redempt.redlib.RedLib;
import redempt.redlib.region.Region;

/**
 * A utility class intended to create interactive multi-block structures.
 * Can also be used to store and copy large sections of blocks. 
 * @author Redempt
 *
 */
@SuppressWarnings("deprecation")
public class MultiBlockStructure {
	
	/**
	 * Use this to get the info to construct a multi-block structure.
	 * Should be hard-coded.
	 * You can use the multi-block structure tool (/struct wand) as long as devMode is true
	 * @param start One bounding corner of the region
	 * @param end The other bounding corner of the region
	 * @param skip A block type to be skipped, in case it was used to select the corners
	 * @return A string representing all of the block data for the region
	 * @throws IllegalArgumentException if the specified locations are not in the same world
	 */
	public static String stringify(Location start, Location end, Material skip) {
		if (!start.getWorld().equals(end.getWorld())) {
			throw new IllegalArgumentException("Locations must be in the same  world");
		}
		int minX = Math.min(start.getBlockX(), end.getBlockX());
		int minY = Math.min(start.getBlockY(), end.getBlockY());
		int minZ = Math.min(start.getBlockZ(), end.getBlockZ());
		
		int maxX = Math.max(start.getBlockX(), end.getBlockX());
		int maxY = Math.max(start.getBlockY(), end.getBlockY());
		int maxZ = Math.max(start.getBlockZ(), end.getBlockZ());
		
		String output = (maxX - minX + 1) + "x" + (maxY - minY + 1) + "x" + (maxZ - minZ + 1) + ";";
		StringBuilder builder = new StringBuilder();
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = start.getWorld().getBlockAt(x, y, z);
					if (midVersion >= 13) {
						if (block.getType() == skip) {
							builder.append("air;");
							continue;
						}
						builder.append(block.getBlockData().getAsString()).append(';');
					} else {
						if (block.getType() == skip) {
							builder.append("AIR:0;");
							continue;
						}
						builder.append(block.getType().toString()).append(":").append(block.getData()).append(";");
					}
				}
			}
		}
		output += builder.toString();
		output = output.substring(0, output.length() - 1);
		output = minify(output);
		return output;
	}
	
	/**
	 * Use this to get the info to construct a multi-block structure.
	 * Should be hard-coded.
	 * You can use the multi-block structure tool (/struct wand) as long as devMode is true
	 * @param start One bounding corner of the region
	 * @param end The other bounding corner of the region
	 * @return A string representing all of the block data for the region
	 * @throws IllegalArgumentException if the specified locations are not in the same world
	 */
	public static String stringify(Location start, Location end) {
		return stringify(start, end, null);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(String info, String name) {
		return new MultiBlockStructure(info, name, true, false);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(String info, String name, boolean strictMode) {
		return new MultiBlockStructure(info, name, strictMode, false);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an info string
	 * @param info The info string. Get this from {@link MultiBlockStructure#stringify(Location, Location)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @param ignoreAir If true, air in the original structure is skipped when checking blocks. Defaults to false.
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(String info, String name, boolean strictMode, boolean ignoreAir) {
		return new MultiBlockStructure(info, name, strictMode, ignoreAir);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an input stream containing the info string
	 * @param stream The input stream. Get this from {@link org.bukkit.plugin.java.JavaPlugin#getResource(String)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @param ignoreAir If true, air in the original structure is skipped when checking blocks. Defaults to false.
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(InputStream stream, String name, boolean strictMode, boolean ignoreAir) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line;
			StringBuilder combine = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				combine.append(line);
			}
			return create(combine.toString(), name, strictMode, ignoreAir);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an input stream containing the info string
	 * @param stream The input stream. Get this from {@link org.bukkit.plugin.java.JavaPlugin#getResource(String)}
	 * @param name The name of the multi-block structure
	 * @param strictMode Whether block data is taken into account. Only checks material if false. Defaults to true.
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(InputStream stream, String name, boolean strictMode) {
		return create(stream, name, strictMode, false);
	}
	
	/**
	 * Creates a MultiBlockStructure instance from an input stream containing the info string
	 * @param stream The input stream. Get this from {@link org.bukkit.plugin.java.JavaPlugin#getResource(String)}
	 * @param name The name of the multi-block structure
	 * @return The multi-block structure
	 */
	public static MultiBlockStructure create(InputStream stream, String name) {
		return create(stream, name, true, false);
	}
	
	private static String minify(String data) {
		data = data.replace("minecraft:", "");
		String[] split = data.split(";");
		int same = 0;
		StringBuilder output = new StringBuilder().append(split[0]).append(";");
		for (int i = 1; i < split.length - 1; i++) {
			if (split[i].equals(split[i + 1])) {
				same += same == 0 ? 2 : 1;
				continue;
			} else if (same > 0) {
				output.append(split[i - 1]).append('*').append(same).append(';');
				same = 0;
				continue;
			}
			output.append(split[i]).append(';');
		}
		if (same > 0) {
			output.append(split[split.length - 1]).append('*').append(same).append(';');
		} else {
			output.append(split[split.length - 1]);
		}
		Map<String, Integer> count = new HashMap<>();
		String combine = output.toString();
		split = combine.split(";");
		for (int i = 1; i < split.length; i++) {
			String str = split[i];
			if (str.contains("*")) {
				str = str.substring(0, str.indexOf('*'));
			}
			if (!count.containsKey(str)) {
				count.put(str, 1);
				continue;
			}
			count.put(str, count.get(str) + 1);
		}
		List<String> replace = new ArrayList<>();
		for (Entry<String, Integer> entry : count.entrySet()) {
			if (entry.getValue() >= 2) {
				replace.add(entry.getKey());
			}
		}
		replace.sort(Comparator.comparingInt(String::length));
		StringBuilder prepend = new StringBuilder();
		for (int i = 0; i < replace.size(); i++) {
			String str = replace.get(i);
			prepend.append(str).append(';');
			combine = combine.replaceAll("(?<=;|^)" + Pattern.quote(str) + "(?=[^a-z_]|$)", i + "");
		}
		if (replace.size() > 0) {
			combine = "(" + prepend.substring(0, prepend.length() - 1) + ")" + combine + ";";
		}
		return combine;
	}
	
	private static String expand(String data) {
		String[] replace = null;
		if (data.startsWith("(")) {
			String list = data.substring(1, data.indexOf(')'));
			replace = list.split(";");
			data = data.substring(data.indexOf(')') + 1);
		}
		StringBuilder builder = new StringBuilder();
		for (String str : data.split(";")) {
			String[] split = str.split("\\*");
			String val = "";
			try {
				int index = Integer.parseInt(split[0]);
				val = replace[index];
			} catch (NumberFormatException e) {
				val = split[0];
			}
			if (split.length > 1) {
				int times = Integer.parseInt(split[1]);
				for (int i = 0; i < times; i++) {
					builder.append(val).append(';');
				}
				continue;
			}
			builder.append(val).append(';');
		}
		return builder.toString() + ";";
	}
	
	protected String[][][] data;
	private StructureFinder finder;
	private String dataString;
	private String name;
	protected int dimX;
	protected int dimY;
	protected int dimZ;
	protected boolean strictMode = true;
	protected boolean ignoreAir = false;
	protected EnumSet<Material> strictModeExclude = EnumSet.noneOf(Material.class);
	
	private MultiBlockStructure(String info, String name, boolean strictMode, boolean ignoreAir) {
		info = expand(info);
		this.dataString = info;
		this.name = name;
		this.strictMode = strictMode;
		this.ignoreAir = ignoreAir;
		String[] split = info.split(";");
		String[] dimSplit = split[0].split("x");
		dimX = Integer.parseInt(dimSplit[0]);
		dimY = Integer.parseInt(dimSplit[1]);
		dimZ = Integer.parseInt(dimSplit[2]);
		data = parse(info, dimX, dimY, dimZ);
		finder = new StructureFinder(this);
	}
	
	private static String[][][] parse(String info, int dimX, int dimY, int dimZ) {
		String[] split = info.split(";");
		String[][][] data = new String[dimX][dimY][dimZ];
		
		int pos = 1;
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					data[x][y][z] = split[pos];
					pos++;
				}
			}
		}
		return data;
	}
	
	private void forEachData(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror, BiConsumer<Location, String> callback) {
		loc = loc.getBlock().getLocation();
		Rotator rotator = new Rotator(rotation, mirror);
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					rotator.setLocation(x, z);
					Location l = loc.clone().add(rotator.getRotatedBlockX(), y, rotator.getRotatedBlockZ());
					rotator.setLocation(relX, relZ);
					l.subtract(rotator.getRotatedBlockX(), relY, rotator.getRotatedBlockZ());
					callback.accept(l, rotator.rotate(data[x][y][z]));
				}
			}
		}
	}
	
	/**
	 * Add a list of Materials to be excluded from strict data checks rather than disabling strict mode for all blocks
	 * @param materials The materials to exclude from strict mode checks
	 */
	public void addStrictModeExclusions(Material... materials) {
		Collections.addAll(strictModeExclude, materials);
	}
	
	/**
	 * @return A set of Materials to be excluded from strict data checks rather than disabling strict mode for all blocks
	 */
	public Set<Material> getStrictModeExclusions() {
		return strictModeExclude;
	}
	
	/**
	 * Gets the Region this multi-block structure would occupy, were it built here
	 * @param loc The location of the multi-block structure
	 * @param relX The relative X in the structure to center at
	 * @param relY The relative Y in the structure to center at
	 * @param relZ The relative Z in the structure to center at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @return The Region this multi-block structure would occupy
	 */
	public Region getRegion(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror) {
		loc = loc.getBlock().getLocation();
		Rotator rotator = new Rotator(rotation, mirror);
		rotator.setLocation(relX, relZ);
		Location start = loc.clone().subtract(rotator.getRotatedBlockX(), relY, rotator.getRotatedBlockZ());
		rotator.setLocation(dimX, dimZ);
		Location end = start.clone().add(rotator.getRotatedBlockX(), dimY, rotator.getRotatedBlockZ());
		return new Region(start, end);
	}
	
	/**
	 * Gets the Region this multi-block structure would occupy, were it built here
	 * @param loc The location of the multi-block structure
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @return The Region this multi-block structure would occupy
	 */
	public Region getRegion(Location loc, int rotation, boolean mirror) {
		return getRegion(loc, 0, 0, 0, rotation, mirror);
	}
	
	/**
	 * Gets the Region this multi-block structure would occupy, were it built here
	 * @param loc The location of the multi-block structure
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @return The Region this multi-block structure would occupy
	 */
	public Region getRegion(Location loc, int rotation) {
		return getRegion(loc, 0, 0, 0, rotation, false);
	}
	
	/**
	 * Gets the Region this multi-block structure would occupy, were it built here
	 * @param loc The location of the multi-block structure
	 * @return The Region this multi-block structure would occupy
	 */
	public Region getRegion(Location loc) {
		return getRegion(loc, 0, 0, 0, 0, false);
	}
	
	/**
	 * Iterates each block which would be set if this structure is built
	 * @param loc The location the structure would be built at
	 * @param relX The relative X in the structure to center at
	 * @param relY The relative Y in the structure to center at
	 * @param relZ The relative Z in the structure to center at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param callback The callback to be called, passed the {@link BlockState} which would be set if the structure is built here
	 */
	public void forEachBlock(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror, Consumer<BlockState> callback) {
		forEachData(loc, relX, relY, relZ, rotation, mirror, (l, s) -> {
			callback.accept(getStateToSet(l, s));
		});
	}
	
	/**
	 * Gets the data of this structure at a given relative location, set with a BlockState at the given location
	 * @param relX The relative X of the block within this multi-block structure
	 * @param relY The relative Y of the block within this multi-block structure
	 * @param relZ The relative Z of the block within this multi-block structure
	 * @return A BlockState, with the Location passed, with the data at the specified relative location within this multi-block structure.
	 * This is done for compatibility reasons. For 1.8, MaterialData would make the most sense, while for 1.13+, BlockData would. BlockState can be converted to either.
	 * @throws ArrayIndexOutOfBoundsException if the relative coordinates do not exist within this structure
	 */
	public BlockState getData(Location loc, int relX, int relY, int relZ) {
		if (relX >= dimX || relX < 0 || relY >= dimY || relY < 0 || relZ >= dimZ || relZ < 0) {
			return null;
		}
		return this.getStateToSet(loc, data[relX][relY][relZ]);
	}
	
	/**
	 * Gets the data of this structure at a given relative location
	 * @param relX The relative X of the block within this multi-block structure
	 * @param relY The relative Y of the block within this multi-block structure
	 * @param relZ The relative Z of the block within this multi-block structure
	 * @return A BlockState, with the Location (0, 0, 0) in the default world, with the data at the specified relative location within this multi-block structure.
	 * This is done for compatibility reasons. For 1.8, MaterialData would make the most sense, while for 1.13+, BlockData would. BlockState can be converted to either.
	 * @throws ArrayIndexOutOfBoundsException if the relative coordinates do not exist within this structure
	 */
	public BlockState getData(int relX, int relY, int relZ) {
		return getData(new Location(Bukkit.getWorlds().get(0), 0, 0, 0), relX, relY, relZ);
	}
	
	/**
	 * Gets the type of the data at a given relative location
	 * @param relX The relative X of the block within this multi-block structure
	 * @param relY The relative Y of the block within this multi-block structure
	 * @param relZ The relative Z of the block within this multi-block structure
	 * @return The type of the block at the given relative location
	 * @throws ArrayIndexOutOfBoundsException if the relative coordinates do not exist within this structure
	 */
	public Material getType(int relX, int relY, int relZ) {
		String data = this.data[relX][relY][relZ];
		if (midVersion >= 13) {
			int pos = data.indexOf('[');
			pos = pos == -1 ? data.length() : pos;
			return Material.valueOf(data.substring(0, pos).toUpperCase());
		} else {
			int pos = data.indexOf(':');
			pos = pos == -1 ? data.length() : pos;
			return Material.valueOf(data.substring(0, pos).toUpperCase());
		}
	}
	
	/**
	 * Iterates each block which would be set if this structure is built
	 * @param loc The location the structure would be built at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param callback The callback to be called, passed the {@link BlockState} which would be set if the structure is built here
	 */
	public void forEachBlock(Location loc, int rotation, boolean mirror, Consumer<BlockState> callback) {
		forEachBlock(loc, 0, 0, 0, rotation, mirror, callback);
	}
	
	/**
	 * Iterates each block which would be set if this structure is built
	 * @param loc The location the structure would be built at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param callback The callback to be called, passed the {@link BlockState} which would be set if the structure is built here
	 */
	public void forEachBlock(Location loc, int rotation, Consumer<BlockState> callback) {
		forEachBlock(loc, 0, 0, 0, rotation, false, callback);
	}
	
	/**
	 * Iterates each block which would be set if this structure is built
	 * @param loc The location the structure would be built at
	 * @param callback The callback to be called, passed the {@link BlockState} which would be set if the structure is built here
	 */
	public void forEachBlock(Location loc, Consumer<BlockState> callback) {
		forEachBlock(loc, 0, 0, 0, 0, false, callback);
	}
	
	/**
	 * Uses a Predicate to test each block where this structure would be built
	 * @param loc The location to test the conditions at
	 * @param relX The relative X in the structure to test centered at
	 * @param relY The relative Y in the structure to test centered at
	 * @param relZ The relative Z in the structure to test centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param filter The predicate to check each location
	 * @return Whether every location passed the check
	 */
	public boolean canBuild(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror, Predicate<Location> filter) {
		boolean[] canBuild = {true};
		forEachData(loc, relX, relY, relZ, rotation, mirror, (l, d) -> {
			if (!filter.test(l)) {
				canBuild[0] = false;
			}
		});
		return canBuild[0];
	}
	
	/**
	 * Uses a Predicate to test each block where this structure would be built
	 * @param loc The location to test the conditions at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param filter The predicate to check each location
	 * @return Whether every location passed the check
	 */
	public boolean canBuild(Location loc, int rotation, boolean mirror, Predicate<Location> filter) {
		return canBuild(loc, 0, 0, 0, rotation, mirror, filter);
	}
	
	/**
	 * Uses a Predicate to test each block where this structure would be built
	 * @param loc The location to test the conditions at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param filter The predicate to check each location
	 * @return Whether every location passed the check
	 */
	public boolean canBuild(Location loc, int rotation, Predicate<Location> filter) {
		return canBuild(loc, 0, 0, 0, rotation, false, filter);
	}
	
	/**
	 * Uses a Predicate to test each block where this structure would be built
	 * @param loc The location to test the conditions at
	 * @param filter The predicate to check each location
	 * @return Whether every location passed the check
	 */
	public boolean canBuild(Location loc, Predicate<Location> filter) {
		return canBuild(loc, 0, 0, 0, 0, false, filter);
	}
	
	/**
	 * Sends ghost blocks of this multi-block structure to the given player at the given location
	 * @param player The player to visualize the structure to
	 * @param loc The location to visualize the structure at
	 * @param relX The relative X in the structure to visualize centered at
	 * @param relY The relative Y in the structure to visualize centered at
	 * @param relZ The relative Z in the structure to visualize centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 */
	public void visualize(Player player, Location loc, int relX, int relY, int relZ, int rotation, boolean mirror) {
		forEachData(loc, relX, relY, relZ, rotation, mirror, (l, d) -> {
			sendBlock(player, l, d);
		});
	}
	
	/**
	 * Sends ghost blocks of this multi-block structure to the given player at the given location
	 * @param player The player to visualize the structure to
	 * @param loc The location to visualize the structure at
	 * @param relX The relative X in the structure to visualize centered at
	 * @param relY The relative Y in the structure to visualize centered at
	 * @param relZ The relative Z in the structure to visualize centered at
	 */
	public void visualize(Player player, Location loc, int relX, int relY, int relZ) {
		visualize(player, loc, relX, relY, relZ, 0, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param relX The relative X in the structure to build centered at
	 * @param relY The relative Y in the structure to build centered at
	 * @param relZ The relative Z in the structure to build centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror) {
		forEachData(loc, relX, relY, relZ, rotation, mirror, (l, d) -> {
			BlockState state = getStateToSet(l, d);
			if (state != null) {
				state.update(true, false);
			}
		});
		return assumeAt(loc, relX, relY, relZ, rotation, mirror);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param relX The relative X in the structure to build centered at
	 * @param relY The relative Y in the structure to build centered at
	 * @param relZ The relative Z in the structure to build centered at
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc, int relX, int relY, int relZ) {
		return build(loc, relX, relY, relZ, 0, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param relX The relative X in the structure to build centered at
	 * @param relY The relative Y in the structure to build centered at
	 * @param relZ The relative Z in the structure to build centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc, int relX, int relY, int relZ, int rotation) {
		return build(loc, relX, relY, relZ, rotation, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc) {
		return build(loc, 0, 0, 0, 0, false);
	}
	
	/**
	 * Builds this multi-block structure at the given location
	 * @param loc The location to build the structure at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @return The Structure instance that was created
	 */
	public Structure build(Location loc, int rotation) {
		return build(loc, 0, 0, 0, rotation, false);
	}
	
	/**
	 * Build this multi-block structure over multiple ticks
	 * @param loc The location to build the structure at
	 * @param relX The relative X in the structure to build centered at
	 * @param relY The relative Y in the structure to build centered at
	 * @param relZ The relative Z in the structure to build centered at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param blocksPerTick The number of blocks to build per tick
	 * @param callback A callback to accept the Structure instance that was created when construction is complete
	 * @return The task number for the Bukkit scheduler task created by this method
	 */
	public int buildAsync(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror, int blocksPerTick, Consumer<Structure> callback) {
		Location location = loc.getBlock().getLocation();
		Rotator rotator = new Rotator(rotation, mirror);
		int[] iter = {0, 0, 0};
		int[] task = {0};
		task[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(RedLib.getInstance(), () -> {
			int pos = 0;
			for (; iter[0] < dimX; iter[0]++) {
				for (; iter[1] < dimY; iter[1]++) {
					for (; iter[2] < dimZ; iter[2]++) {
						rotator.setLocation(iter[0], iter[2]);
						Location l = location.clone().add(rotator.getRotatedBlockX(), iter[1], rotator.getRotatedBlockZ());
						rotator.setLocation(relX, relZ);
						l.subtract(rotator.getRotatedBlockX(), relY, rotator.getRotatedBlockZ());
						BlockState state = getStateToSet(l, rotator.rotate(data[iter[0]][iter[1]][iter[2]]));
						if (state != null) {
							state.update(true, false);
							pos++;
						}
						if (pos >= blocksPerTick) {
							return;
						}
					}
					iter[2] = 0;
				}
				iter[1] = 0;
			}
			try {
				callback.accept(assumeAt(location, relX, relY, relZ, rotation, mirror));
			} catch (Exception e) {
				e.printStackTrace();
			}
			Bukkit.getScheduler().cancelTask(task[0]);
		}, 1, 1);
		return task[0];
	}
	
	/**
	 * Build this multi-block structure over multiple ticks
	 * @param loc The location to build the structure at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param mirror Whether to mirror the structure on the X axis
	 * @param blocksPerTick The number of blocks to build per tick
	 * @param callback A callback to accept the Structure instance that was created when construction is complete
	 * @return The task number for the Bukkit scheduler task created by this method
	 */
	public int buildAsync(Location loc, int rotation, boolean mirror, int blocksPerTick, Consumer<Structure> callback) {
		return buildAsync(loc, 0, 0, 0, rotation, mirror, blocksPerTick, callback);
	}
	
	/**
	 * Build this multi-block structure over multiple ticks
	 * @param loc The location to build the structure at
	 * @param rotation The number of 90-degree clockwise rotations to apply
	 * @param blocksPerTick The number of blocks to build per tick
	 * @param callback A callback to accept the Structure instance that was created when construction is complete
	 * @return The task number for the Bukkit scheduler task created by this method
	 */
	public int buildAsync(Location loc, int rotation, int blocksPerTick, Consumer<Structure> callback) {
		return buildAsync(loc, 0, 0, 0, rotation, false, blocksPerTick, callback);
	}
	
	/**
	 * Build this multi-block structure over multiple ticks
	 * @param loc The location to build the structure at
	 * @param blocksPerTick The number of blocks to build per tick
	 * @param callback A callback to accept the Structure instance that was created when construction is complete
	 * @return The task number for the Bukkit scheduler task created by this method
	 */
	public int buildAsync(Location loc, int blocksPerTick, Consumer<Structure> callback) {
		return buildAsync(loc, 0, 0, 0, 0, false, blocksPerTick, callback);
	}
	
	/**
	 * Gets this multi-block structure's name. May be faster to compare this than to use .equals().
	 * @return The name of this multi-block structure
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the dimensions of this multi-block structure. [x, y, z]
	 * @return The dimensions of this multi-block structure
	 */
	public int[] getDimensions() {
		return new int[] {dimX, dimY, dimZ};
	}
	
	/**
	 * @return The volume of this multi-block structure in blocks
	 */
	public int getVolume() {
		return dimX * dimY * dimZ;
	}
	
	/**
	 * @return Whether this structure ignores air in the data when building and checking for presence
	 */
	public boolean ignoresAir() {
		return ignoreAir;
	}
	
	/**
	 * @return Whether this structure ignores data other than block type when checking for presence
	 */
	public boolean isStrictMode() {
		return strictMode;
	}
	
	/**
	 * Gets the Structure at the given block, if it exists.
	 * The given location can be any part of the multi-block structure.
	 * This is very computationally expensive for larger structures, and should be avoided if possible, as it checks every possible rotation and mirroring of the structure.
	 * @param loc The location to check at
	 * @return The structure at this block, or null if it does not exist
	 */
	public Structure getAt(Location loc) {
		return finder.getAt(loc.getBlock());
	}
	
	/**
	 * Gets the Structure at the given block, if it exists. All parameters must be known.
	 * Significantly faster than {@link MultiBlockStructure#getAt(Location)}
	 * @param loc The location to check at
	 * @param relX The relative X in the structure of the location
	 * @param relY The relative Y in the structure of the location
	 * @param relZ The relative Z in the structure of the location
	 * @param rotation The number of clockwise rotations applied to the structure
	 * @param mirror Whether the structure is mirrored
	 * @return The structure at this block, or null if it does not exist
	 */
	public Structure getAt(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror) {
		Structure s;
		Rotator rotator = new Rotator(rotation, mirror);
		if (compare(data[relX][relY][relZ], loc.getBlock(), rotator) && (s = test(loc, relX, relY, relZ, rotator)) != null) {
			return s;
		}
		return null;
	}
	
	/**
	 * Gets the Structure at the given block, performing no checks to ensure it exists.
	 * @param loc The location of the Structure
	 * @param relX The relative X of the location within the Structure
	 * @param relY The relative Y of the location within the Structure
	 * @param relZ The relative Z of the location within the Structure
	 * @param rotation The number of clockwise rotations applied to the structure
	 * @param mirror Whether the structure is mirrored
	 * @return The Structure instance
	 */
	public Structure assumeAt(Location loc, int relX, int relY, int relZ, int rotation, boolean mirror) {
		loc = loc.getBlock().getLocation();
		Rotator rotator = new Rotator(rotation, mirror);
		rotator.setLocation(relX, relZ);
		loc.subtract(rotator.getRotatedBlockX(), relY, rotator.getRotatedBlockZ());
		return new Structure(this, loc, rotator);
	}
	
	/**
	 * Gets the Structure at the given block, performing no checks to ensure it exists.
	 * @param loc The location of the Structure
	 * @param rotation The number of clockwise rotations applied to the structure
	 * @param mirror Whether the structure is mirrored
	 * @return The Structure instance
	 */
	public Structure assumeAt(Location loc, int rotation, boolean mirror) {
		return assumeAt(loc, 0, 0, 0, rotation, mirror);
	}
	
	/**
	 * Gets the Structure at the given block, performing no checks to ensure it exists.
	 * @param loc The location of the Structure
	 * @param rotation The number of clockwise rotations applied to the structure
	 * @return The Structure instance
	 */
	public Structure assumeAt(Location loc, int rotation) {
		return assumeAt(loc, 0, 0, 0, rotation, false);
	}
	
	/**
	 * Gets the Structure at the given block, performing no checks to ensure it exists.
	 * @param loc The location of the Structure
	 * @return The Structure instance
	 */
	public Structure assumeAt(Location loc) {
		return assumeAt(loc, 0, 0, 0, 0, false);
	}
	
	private Structure test(Location loc, int xPos, int yPos, int zPos, Rotator rotator) {
		loc = loc.getBlock().getLocation();
		for (int x = 0; x < dimX; x++) {
			for (int y = 0; y < dimY; y++) {
				for (int z = 0; z < dimZ; z++) {
					rotator.setLocation(x - xPos, z - zPos);
					int xp = rotator.getRotatedBlockX();
					int yp = y - yPos;
					int zp = rotator.getRotatedBlockZ();
					Block block = loc.clone().add(xp, yp, zp).getBlock();
					if (!compare(data[x][y][z], block, rotator)) {
						return null;
					}
				}
			}
		}
		rotator.setLocation(xPos, zPos);
		loc = loc.clone().subtract(rotator.getRotatedBlockX(), yPos, rotator.getRotatedBlockZ());
		return new Structure(this, loc, rotator);
	}
	
	protected boolean compare(String data, Block block, Rotator rotator) {
		Material material = block.getType();
		if (midVersion >= 13) {
			data = rotator.rotate(data);
			data = data.startsWith("minecraft:") ? data : "minecraft:" + data;
			if (ignoreAir && Bukkit.createBlockData(data).getMaterial() == Material.AIR) {
				return true;
			}
			BlockData bdata = Bukkit.createBlockData(data);
			String type = data.substring(0, data.indexOf('[') == -1 ? data.length() : data.indexOf('['));
			String otherBlockData = block.getBlockData().getAsString();
			String otherType = otherBlockData.substring(0, otherBlockData.indexOf('[') == -1 ? otherBlockData.length() : otherBlockData.indexOf('['));
			if (!strictMode || strictModeExclude.contains(material)) {
				return type.equals(otherType);
			}
			return block.getBlockData().matches(bdata);
		} else {
			String[] split = data.split(":");
			if (ignoreAir && split[0].equals("AIR")) {
				return true;
			}
			if (!strictMode || strictModeExclude.contains(material)) {
				return material == Material.valueOf(split[0]);
			}
			return material == Material.valueOf(split[0]) && block.getData() == Byte.parseByte(split[1]);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof MultiBlockStructure) {
			MultiBlockStructure structure = (MultiBlockStructure) o;
			return structure.dataString.equals(dataString) && structure.name.equals(name);
		}
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(dataString, name);
	}
	
	@Override
	public String toString() {
		return dataString;
	}
	
	private BlockState getStateToSet(Location loc, String data) {
		if (midVersion >= 13) {
			BlockData blockData = Bukkit.createBlockData(data);
			if (ignoreAir && blockData.getMaterial() == Material.AIR) {
				return null;
			}
			BlockState state = loc.getBlock().getState();
			state.setBlockData(blockData);
			return state;
		} else {
			String[] split = data.split(":");
			Material type = Material.valueOf(split[0]);
			if (ignoreAir && type == Material.AIR) {
				return null;
			}
			byte dataValue = Byte.parseByte(split[1]);
			BlockState state = loc.getBlock().getState();
			state.setType(type);
			state.setRawData(dataValue);
			return state;
		}
	}
	
	private void sendBlock(Player player, Location loc, String data) {
		if (midVersion >= 13) {
			BlockData blockData = Bukkit.createBlockData(data);
			if (ignoreAir && blockData.getMaterial() == Material.AIR) {
				return;
			}
			player.sendBlockChange(loc, blockData);
		} else {
			String[] split = data.split(":");
			Material type = Material.valueOf(split[0]);
			if (ignoreAir && type == Material.AIR) {
				return;
			}
			byte dataValue = Byte.parseByte(split[1]);
			player.sendBlockChange(loc, type, dataValue);
		}
	}
	
}
