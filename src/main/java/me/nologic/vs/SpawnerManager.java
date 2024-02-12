package me.nologic.vs;

import lombok.Getter;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SpawnerManager implements Listener {

    private final vSpawners plugin;

    @Getter
    private final NamespacedKey itemsKey;

    @Getter
    private final NamespacedKey experienceKey;

    @Getter
    private final HashMap<CreatureSpawner, SpawnerAccumulator> workingSpawners;

    private final HashMap<Entity, CreatureSpawner> entitiesFromSpawners;

    private final Random random;

    public SpawnerManager(final vSpawners plugin) {
        this.plugin = plugin;
        this.random   = new Random();
        itemsKey      = new NamespacedKey(plugin, "items");
        experienceKey = new NamespacedKey(plugin, "exp");
        workingSpawners      = new HashMap<>();
        entitiesFromSpawners = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private @NotNull SpawnerAccumulator getSpawnerAccumulator(final CreatureSpawner spawner) {
        if (!workingSpawners.containsKey(spawner)) workingSpawners.put(spawner, new SpawnerAccumulator(this, spawner));
        return workingSpawners.get(spawner);
    }

    private @Nullable SpawnerAccumulator getSpawnerAccumulator(final Inventory inventory) {
        return workingSpawners.values().stream().filter(ji -> ji.getInventory().equals(inventory)).findAny().orElse(null);
    }

    /* --- Listeners ---*/

    @EventHandler
    private void onChunkUnload(final ChunkUnloadEvent event) {
        final List<CreatureSpawner> toDelete = new ArrayList<>();
        this.workingSpawners.forEach((spawner, accumulator) -> {
            if (event.getChunk().contains(spawner.getBlockData())) {
                toDelete.add(spawner);
            }
        });
        toDelete.forEach(workingSpawners::remove);
    }

    @EventHandler
    private void onSpawnerSpawn(final SpawnerSpawnEvent event) {
        if (event.getSpawner() == null) return;
        this.entitiesFromSpawners.put(event.getEntity(), event.getSpawner());
        ((LivingEntity) event.getEntity()).damage(((LivingEntity) event.getEntity()).getHealth());
    }

    @EventHandler
    private void onBlockDestroy(final BlockBreakEvent event) {
        if (event.getBlock().getType().equals(Material.SPAWNER)) {

            final CreatureSpawner spawner = (CreatureSpawner) event.getBlock().getState();
            if (event.getPlayer().getInventory().getItemInMainHand().getEnchantments().containsKey(Enchantment.SILK_TOUCH)) {
                final ItemStack dropped = spawner.getWorld().dropItem(spawner.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.SPAWNER)).getItemStack();
                final BlockStateMeta meta    = (BlockStateMeta) dropped.getItemMeta();
                meta.setBlockState(spawner);
                dropped.setItemMeta(meta);
                workingSpawners.remove(spawner);
                spawner.getBlock().setType(Material.AIR);
                event.setCancelled(true);
                return;
            }

            final SpawnerAccumulator accumulator = this.getSpawnerAccumulator(spawner);
            accumulator.getInventory().forEach(item -> {
                if (item != null) spawner.getWorld().dropItem(spawner.getLocation().clone().add(0.5, 0.5, 0.5), item);
                spawner.getWorld().spawn(spawner.getLocation(), ExperienceOrb.class, orb -> orb.setExperience(accumulator.getAccumulatedExperience()));
            });
            workingSpawners.remove(spawner);
        }
    }

    @EventHandler
    private void onEntityDeath(final EntityDeathEvent event) {
        if (event.getEntity().fromMobSpawner()) {


            final CreatureSpawner    spawner     = this.entitiesFromSpawners.get(event.getEntity());
            final SpawnerAccumulator accumulator = this.getSpawnerAccumulator(spawner);

            /* Drop modification. */
            final ConfigurationSection section = plugin.getLootConfig().getConfigurationSection("spawner-drops.%s".formatted(event.getEntity().getType().toString()));
            final int                  dice    = random.nextInt(plugin.getLootConfig().getInt("spawner-drops.dice"));

            if (plugin.getLootConfig().getBoolean("spawner-drops.overwrite")) {
                event.getDrops().clear();
            }

            if (section != null) {
                for (final String key : section.getKeys(false)) {
                    final int[] range = { Integer.parseInt(key.split("-")[0]), Integer.parseInt(key.split("-")[1]) };
                    if (dice >= range[0] && dice <= range[1]) {

                        final ItemStack item = accumulator.deserializeItemStack(plugin.getLootConfig().getString("spawner-drops.%s.%s".formatted(event.getEntityType().toString(), key)));

                        if (item != null) {
                            event.getDrops().add(item);
                            plugin.getLogger().info("item:" + item);
                        }
                    }
                }
            }

            /* Experience and persistent data. */
            int exp = ((int) (Math.random() * plugin.getConfig().getInt("xp-drop")));
            spawner.getPersistentDataContainer().set(itemsKey, PersistentDataType.STRING, accumulator.addItems(event.getDrops()).serializeInventory());
            spawner.update();

            accumulator.setAccumulatedExperience(accumulator.getAccumulatedExperience() + exp);
            accumulator.getMenu().update();

            event.setCancelled(true);
            event.getEntity().remove();
            this.entitiesFromSpawners.remove(event.getEntity());
        }
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && event.getClickedBlock() != null && event.getClickedBlock().getType().equals(Material.SPAWNER)) {

            final CreatureSpawner spawner = ((CreatureSpawner) event.getClickedBlock().getState());

            // Spawn egg detection
            if (event.getPlayer().getInventory().getItemInMainHand().getType().toString().contains("SPAWN_EGG"))
                return;

            final SpawnerAccumulator jsonInventory = getSpawnerAccumulator(spawner);
            jsonInventory.openChestMenu(event.getPlayer());
        }
    }

    @EventHandler
    private void onInventoryClose(final InventoryCloseEvent event) {
        final SpawnerAccumulator accumulator = this.getSpawnerAccumulator(event.getInventory());
        if (accumulator != null)
            accumulator.getSpawner().getPersistentDataContainer().set(itemsKey, PersistentDataType.STRING, accumulator.serializeInventory());
    }

}