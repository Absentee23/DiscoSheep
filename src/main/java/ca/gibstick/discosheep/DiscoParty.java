package ca.gibstick.discosheep;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.*;
import org.bukkit.FireworkEffect.Builder;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

import static org.bukkit.EntityEffect.SHEEP_EAT;

public class DiscoParty {

    static int defaultDuration = 300; // ticks for entire party
    static int defaultPeriod = 10; // ticks per state change
    static int defaultRadius = 5;
    static int defaultSheep = 10;
    static float defaultSheepJump = 0.35f;
    static int maxFloorSize = 5;
    static int maxDuration = 2400; // 120 seconds
    static int maxSheep = 100;
    static int maxRadius = 100;
    static int minPeriod = 5;	// 0.25 seconds
    static int maxPeriod = 40;	// 2.0 seconds
    private static HashMap<String, Integer> defaultGuestNumbers = new HashMap<>();
    private static HashMap<String, Integer> maxGuestNumbers = new HashMap<>();
    private static final EnumSet<Material> floorExceptions = EnumSet.of(
            //Material.STAINED_GLASS,
            Material.FURNACE,
            Material.CHEST,
            Material.ENDER_CHEST,
            Material.BURNING_FURNACE,
            Material.ENDER_PORTAL,
            Material.ENDER_PORTAL_FRAME,
            Material.OBSIDIAN,
            Material.BED,
            Material.BED_BLOCK,
            Material.SOIL
    );

    private static final DyeColor[] discoColours = {
        DyeColor.RED,
        DyeColor.ORANGE,
        DyeColor.YELLOW,
        DyeColor.GREEN,
        DyeColor.BLUE,
        DyeColor.LIGHT_BLUE,
        DyeColor.PINK,
        DyeColor.MAGENTA,
        DyeColor.LIME,
        DyeColor.CYAN,
        DyeColor.PURPLE,
        DyeColor.BLACK,
        DyeColor.WHITE
    };

    private static final float[] pentatonicNotes = {
        1.0f,
        1.125f,
        1.25f,
        1.5f,
        1.667f,
        2.0f
    };
    // Instance properties
    private Random r = new Random();
    private PartyEvents partyEvents;
    private final DiscoSheep plugin = DiscoSheep.getInstance();
    private Player player;
    private final ArrayList<Sheep> sheepList = new ArrayList<>();
    private final HashSet<Sheep> sheepSet = new HashSet<>();
    private final ArrayList<Entity> guestList = new ArrayList<>();
    private final HashSet<Entity> guestSet = new HashSet<>();
    private final ArrayList<BlockState> floorBlockCache = new ArrayList<>();
    private final ArrayList<Block> floorBlocks = new ArrayList<>();
    private HashMap<String, Integer> guestNumbers = new HashMap<>();
    private boolean doFireworks = false;
    private final boolean doJump = true;
    private boolean doLightning = false;
    private boolean doFloor = false;
    private int duration, period, radius, sheep;
    private int state = 0; // basically our own tick system
    private float volumeMultiplier;
    private Location partyLocation;
    private DiscoUpdater updater;

    public DiscoParty(Player player) {
        this();
        this.player = player;
        this.partyLocation = player.getLocation();
    }

    public DiscoParty() {
        this.duration = DiscoParty.defaultDuration;
        this.period = DiscoParty.defaultPeriod;
        this.radius = DiscoParty.defaultRadius;
        this.sheep = DiscoParty.defaultSheep;
        this.guestNumbers = new HashMap<>(DiscoParty.defaultGuestNumbers);
        r = new Random();
    }

    // copy but with new player
    // used for /ds other and /ds all
    public DiscoParty clone(Player player) {
        DiscoParty newParty;
        newParty = new DiscoParty(player);
        newParty.doFireworks = this.doFireworks;
        newParty.duration = this.duration;
        newParty.period = this.period;
        newParty.radius = this.radius;
        newParty.sheep = this.sheep;
        newParty.doLightning = this.doLightning;
        newParty.guestNumbers = this.getGuestNumbers();
        return newParty;
    }

    HashSet<Sheep> getSheepSet() {
        return sheepSet;
    }

    HashSet<Entity> getGuestSet() {
        return guestSet;
    }

    ArrayList<Sheep> getSheepList() {
        return sheepList;
    }

    ArrayList<Entity> getGuestList() {
        return guestList;
    }

    ArrayList<BlockState> getFloorCache() {
        return this.floorBlockCache;
    }

    ArrayList<Block> getFloorBlocks() {
        return this.floorBlocks;
    }

    public int getRadius() {
        return radius;
    }

    public static HashMap<String, Integer> getDefaultGuestNumbers() {
        return defaultGuestNumbers;
    }

    public HashMap<String, Integer> getGuestNumbers() {
        return guestNumbers;
    }

    public static HashMap<String, Integer> getMaxGuestNumbers() {
        return maxGuestNumbers;
    }

    public int getSheep() {
        return this.sheep;
    }

    public DiscoParty setPlayer(Player player) {
        this.player = player;
        this.partyLocation = player.getLocation();
        return this;
    }

    public DiscoParty setDuration(int duration) throws IllegalArgumentException {
        if (duration <= DiscoParty.maxDuration && duration > 0) {
            this.duration = duration;
            return this;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public DiscoParty setPeriod(int period) throws IllegalArgumentException {
        if (period >= DiscoParty.minPeriod && period <= DiscoParty.maxPeriod) {
            this.period = period;
            return this;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public DiscoParty setRadius(int radius) throws IllegalArgumentException {
        if (radius <= DiscoParty.maxRadius && radius > 0) {
            this.radius = radius;
            return this;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public DiscoParty setDenseRadius(int sheepNo) throws IllegalArgumentException {
        Integer rand = (int) Math.floor(Math.sqrt(sheep / Math.PI));
        if (rand > DiscoParty.maxRadius) {
            rand = DiscoParty.maxRadius;
        }
        if (rand < 1) {
            rand = 1;
        }

        this.setRadius(rand);
        return this;
    }

    public DiscoParty setSheep(int sheep) throws IllegalArgumentException {
        if (sheep <= DiscoParty.maxSheep && sheep > 0) {
            this.sheep = sheep;
            return this;
        } else {
            throw new IllegalArgumentException();
        }
    }

    public DiscoParty setDoFireworks(boolean doFireworks) {
        this.doFireworks = doFireworks;
        return this;
    }

    public DiscoParty setDoLightning(boolean doLightning) {
        this.doLightning = doLightning;
        return this;
    }

    public void setDoFloor(boolean doFloor) {
        this.doFloor = doFloor;
    }

    public DiscoParty setGuestNumber(String key, int n) throws IllegalArgumentException {
        if (getMaxGuestNumbers().containsKey(key.toUpperCase())) {
            if (n <= getMaxGuestNumbers().get(key.toUpperCase()) && n >= 0) { // so that /ds defaults can take 0 as arg
                getGuestNumbers().put(key, n);

                return this;
            }
        }
        throw new IllegalArgumentException();
    }

    // use current settings as new defaults
    public DiscoParty setDefaultsFromCurrent() {
        DiscoParty.defaultDuration = this.duration;
        DiscoParty.defaultPeriod = this.period;
        DiscoParty.defaultRadius = this.radius;
        DiscoParty.defaultSheep = this.sheep;
        DiscoParty.defaultGuestNumbers = new HashMap<>(this.getGuestNumbers());
        return this;
    }

    Location getRandomSpawnLocation(double x, double z, World world, int spawnRadius) {
        Location loc;

        double y;

        /* random point on circle with polar coordinates
         * random number must be square rooted to obtain uniform distribution
         * otherwise the sheep are biased toward the centre */
        double rand = Math.sqrt(r.nextDouble()) * spawnRadius;
        double azimuth = r.nextDouble() * 2 * Math.PI; // radians
        x += rand * Math.cos(azimuth);
        z += rand * Math.sin(azimuth);
        y = partyLocation.getY();

        loc = new Location(world, x, y, z);
        loc.setPitch(r.nextFloat() * 360 - 180);
        loc.setYaw(r.nextFloat() * 360 - 180);

        return loc;
    }

    // Spawn some number of guests next to given player
    void spawnAll(int sheep, int spawnRadius) {
        Location loc;
        World world = player.getWorld();

        double x = partyLocation.getX();
        double z = partyLocation.getZ();
        for (int i = 0; i < sheep; i++) {
            loc = getRandomSpawnLocation(x, z, world, spawnRadius);
            if (plugin.useWG) {
                if (allowSpawn(loc)) {
                    spawnSheep(world, loc);
                    denySpawn(loc);
                } else {
                    spawnSheep(world, loc);
                }
            } else {
                spawnSheep(world,loc);
            }
        }

        // loop through hashmap of other guests and spawn accordingly
        for (Map.Entry entry : guestNumbers.entrySet()) {
            EntityType ent = EntityType.valueOf((String) entry.getKey());
            int num = (Integer) entry.getValue();

            for (int i = 0; i < num; i++) {
                loc = getRandomSpawnLocation(x, z, world, spawnRadius);
                if (plugin.useWG) {
                    if (allowSpawn(loc)) {
                        spawnGuest(world, loc, ent);
                        denySpawn(loc);
                    } else {
                        spawnGuest(world, loc, ent);
                    }
                } else {
                    spawnGuest(world,loc,ent);
                }
            }
        }

        if (doFloor) {
            this.spawnFloor(world, new Location(world, partyLocation.getBlockX(), partyLocation.getBlockY() - 1, partyLocation.getBlockZ()));
        }
    }

    void spawnSheep(World world, Location loc) {
        Sheep newSheep = (Sheep) world.spawnEntity(loc, EntityType.SHEEP);
        newSheep.setColor(discoColours[(r.nextInt(discoColours.length))]);
        newSheep.setBreed(false);	// this prevents breeding - no event listener required
        newSheep.teleport(loc);	    // teleport is needed to set orientation
        getSheepList().add(newSheep);
        getSheepSet().add(newSheep);
        if (doLightning) {
            world.strikeLightningEffect(loc);
        }
        //newSheep.setCustomName("jeb_");
        //newSheep.setCustomNameVisible(false);
        newSheep.setRemoveWhenFarAway(true);
    }

    void spawnGuest(World world, Location loc, EntityType type) {
        LivingEntity newGuest = (LivingEntity) loc.getWorld().spawnEntity(loc, type);
        newGuest.setRemoveWhenFarAway(true);
        getGuestList().add(newGuest);
        getGuestSet().add(newGuest);
        if (doLightning) {
            world.strikeLightningEffect(loc);
        }
    }

    void spawnFloor(World world, Location loc) {
        // First we'll save the floor state
        for (int x = loc.getBlockX() - Math.min(this.radius, DiscoParty.maxFloorSize); x < loc.getX() + Math.min(this.radius, DiscoParty.maxFloorSize); ++x) {
            for (int z = loc.getBlockZ() - Math.min(this.radius, DiscoParty.maxFloorSize); z < loc.getZ() + Math.min(this.radius, DiscoParty.maxFloorSize); ++z) {
                Block block = world.getBlockAt(x, loc.getBlockY(), z);
                if (!DiscoParty.floorExceptions.contains(block.getType())
                        && block.getRelative(BlockFace.UP).getType() == Material.AIR
                        && (block.getType().isSolid() || block.getType() == Material.AIR)) {
                    this.getFloorCache().add(block.getState());
                    //block.setType(Material.STAINED_GLASS);
                    this.getFloorBlocks().add(block);
                }
            }
        }
    }

    // Mark all guests for removal, then clear the array
    void removeAll() {
        for (Sheep sheeple : getSheepList()) {
            sheeple.remove();
        }
        for (Entity guest : getGuestList()) {
            guest.remove();
        }

        for (BlockState block : this.floorBlockCache) {
            block.update(true);
        }
        getSheepList().clear();
        getGuestList().clear();
        getSheepSet().clear();
        getGuestSet().clear();
        floorBlockCache.clear();
    }

    // Set a random colour for all sheep in array
    void randomizeSheepColour(Sheep sheep) {
        sheep.setColor(discoColours[(r.nextInt(discoColours.length))]);
    }

    void randomizeFloor(Block block, int index) {
        int to_color = (index + state) % discoColours.length;
        block.setData(discoColours[to_color].getData());
    }

    void jump(Entity entity) {
        Vector orgVel = entity.getVelocity();
        Vector newVel = (new Vector()).copy(orgVel);
        newVel.add(new Vector(0, defaultSheepJump, 0));
        entity.setVelocity(newVel);
    }

    Color[] getColor = {
        Color.AQUA,
        Color.BLACK,
        Color.BLUE,
        Color.FUCHSIA,
        Color.GRAY,
        Color.GREEN,
        Color.LIME,
        Color.MAROON,
        Color.NAVY,
        Color.OLIVE,
        Color.ORANGE,
        Color.PURPLE,
        Color.RED,
        Color.SILVER,
        Color.TEAL,
        Color.WHITE,
        Color.YELLOW
    };

    void updateAll() {
        for (Sheep sheeple : getSheepList()) {
            randomizeSheepColour(sheeple);

            if (state % 8 == 0) {
                if (r.nextDouble() < 0.50 && doFireworks) {
                    spawnRandomFireworkAtSheep(sheeple);
                }
                sheeple.playEffect(SHEEP_EAT);
            }

            if (doJump) {
                if (state % 2 == 0 && r.nextDouble() < 0.5) {
                    jump(sheeple);
                }
            }

        }

        for (Entity guest : getGuestList()) {
            if (doJump) {
                if (state % 2 == 0 && r.nextDouble() < 0.5) {
                    jump(guest);
                }
            }
        }

//        for (int i = 0; i < this.floorBlocks.size(); i++) {
//            this.randomizeFloor(floorBlocks.get(i), i);
//        }
    }

    float getPentatonicNote() {
        return DiscoParty.pentatonicNotes[r.nextInt(pentatonicNotes.length)];
    }

    void playSounds() {

        /*for (Sheep sheep : this.getSheepList()) {
         sheep.getWorld().playSound(sheep.getLocation(), Sound.NOTE_BASS_DRUM, 0.75f, 1.0f);

         if (this.state % 2 == 0) {
         sheep.getWorld().playSound(sheep.getLocation(), Sound.NOTE_SNARE_DRUM, 0.8f, 1.0f);
         }

         if ((this.state + 1) % 8 == 0) {
         sheep.getWorld().playSound(sheep.getLocation(), Sound.NOTE_STICKS, 1.0f, 1.0f);
         }

         }*/
        partyLocation.getWorld().playSound(partyLocation, Sound.NOTE_BASS_DRUM, volumeMultiplier * 0.75f, 1.0f);
        if (this.state % 2 == 0) {
            partyLocation.getWorld().playSound(partyLocation, Sound.NOTE_SNARE_DRUM, volumeMultiplier * 0.8f, 1.0f);
        }

        if ((this.state + 1) % 8 == 0) {
            partyLocation.getWorld().playSound(partyLocation, Sound.NOTE_STICKS, volumeMultiplier * 1.0f, 1.0f);
        }
        player.playSound(player.getLocation(), Sound.BURP, 0.5f, r.nextFloat() + 1);
    }

    void randomizeFirework(Firework firework) {
        Builder effect = FireworkEffect.builder();
        FireworkMeta meta = firework.getFireworkMeta();

        // construct [1, 3] random colours
        int numColours = r.nextInt(3) + 1;
        Color[] colourArray = new Color[numColours];
        for (int i = 0; i < numColours; i++) {
            colourArray[i] = getColor[r.nextInt(17)];
        }

        // randomize effects
        effect.withColor(colourArray);
        effect.flicker(r.nextDouble() < 0.5);
        effect.trail(r.nextDouble() < 0.5);
        effect.with(FireworkEffect.Type.values()[r.nextInt(FireworkEffect.Type.values().length)]);

        // set random effect and randomize power
        meta.addEffect(effect.build());
        meta.setPower(r.nextInt(2) + 1);

        // apply it to the given firework
        firework.setFireworkMeta(meta);
    }

    void spawnRandomFireworkAtSheep(Sheep sheep) {
        Firework firework = (Firework) sheep.getWorld().spawnEntity(sheep.getEyeLocation(), EntityType.FIREWORK);
        randomizeFirework(firework);
    }

    void update() {
        if (duration > 0) {
            updateAll();
            playSounds();
            duration -= period;
            this.scheduleUpdate();
            this.state = (this.state + 1) % 10000;
        } else {
            this.stopDisco();
        }
    }

    void scheduleUpdate() {
        updater = new DiscoUpdater();
        updater.runTaskLater(plugin, this.period);
    }

    void startDisco() {
        this.volumeMultiplier = Math.max(this.radius / 10, 1.0f);
        this.spawnAll(sheep, radius);
        this.scheduleUpdate();
        plugin.getPartyMap().put(this.player.getName(), this);
        // start listening
        this.partyEvents = new PartyEvents(this);
        plugin.getServer().getPluginManager().registerEvents(this.partyEvents, this.plugin);
    }

    void stopDisco() {
        removeAll();
        this.duration = 0;
        if (updater != null) {
            updater.cancel();
        }
        updater = null;
        plugin.getPartyMap().remove(this.player.getName());
        // stop listening
        HandlerList.unregisterAll(this.partyEvents);
    }

    boolean allowSpawn(Location loc) {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        ApplicableRegionSet regions = ((WorldGuardPlugin) plugin).getRegionManager(loc.getWorld()).getApplicableRegions(loc);

        if (!regions.testState(null,DefaultFlag.MOB_SPAWNING)) {
            plugin.getLogger().info("Mob-spawning denied, allowing...");
            for (ProtectedRegion r : regions) {
                r.setFlag(DefaultFlag.MOB_SPAWNING, StateFlag.State.ALLOW);
            }
            return true;
        } else {
            return false;
        }
    }

    void denySpawn(Location loc) {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("WorldGuard");
        ApplicableRegionSet regions = ((WorldGuardPlugin) plugin).getRegionManager(loc.getWorld()).getApplicableRegions(loc);

        if (regions.testState(null,DefaultFlag.MOB_SPAWNING)) {
            for (ProtectedRegion r : regions) {
                r.setFlag(DefaultFlag.MOB_SPAWNING, StateFlag.State.DENY);
            }
        }
    }

    class DiscoUpdater extends BukkitRunnable {

        @Override
        public void run() {
            update();
        }
    }
}
