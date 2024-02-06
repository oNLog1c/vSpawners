package me.nologic.vs;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class vSpawners extends JavaPlugin implements Listener {

    @Getter
    private static NamespacedKey itemsKey;

    @Getter
    private static NamespacedKey expKey;

    private HashMap<Entity, CreatureSpawner>             entities;
    private HashMap<CreatureSpawner, SpawnerAccumulator> spawners;

    @Getter
    private static vSpawners instance;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        itemsKey = new NamespacedKey(this, "items");
        expKey   = new NamespacedKey(this, "exp");
        entities = new HashMap<>();
        spawners = new HashMap<>();
        super.getServer().getPluginManager().registerEvents(this, this);
        super.getServer().getPluginManager().registerEvents(new MenuFunctionListener(), this);
    }

    @Override
    public void onDisable() {
        this.spawners.keySet().forEach(BlockState::update);
    }

    @EventHandler
    private void onChunkUnload(final ChunkUnloadEvent event) {
        final List<CreatureSpawner> toDelete = new ArrayList<>();
        this.spawners.forEach((spawner, accumulator) -> {
            if (event.getChunk().contains(spawner.getBlockData())) {
                toDelete.add(spawner);
            }
        });
        toDelete.forEach(spawner -> spawners.remove(spawner));
    }

    private SpawnerAccumulator getSpawnerAccumulator(final CreatureSpawner spawner) {
        if (!spawners.containsKey(spawner)) spawners.put(spawner, new SpawnerAccumulator(spawner));
        return spawners.get(spawner);
    }

    @EventHandler
    private void onSpawnerSpawn(final SpawnerSpawnEvent event) {
        if (event.getSpawner() == null) return;
        this.entities.put(event.getEntity(), event.getSpawner());
        ((LivingEntity) event.getEntity()).damage(((LivingEntity) event.getEntity()).getHealth());
    }

    @EventHandler
    private void onBlockDestroy(final BlockBreakEvent event) {
        if (event.getBlock().getType().equals(Material.SPAWNER)) {
            final CreatureSpawner spawner = (CreatureSpawner) event.getBlock().getState();
            final SpawnerAccumulator accumulator = this.getSpawnerAccumulator(spawner);
            accumulator.getInventory().forEach(item -> {
                if (item != null) spawner.getWorld().dropItem(spawner.getLocation().clone().add(0.5, 0.5, 0.5), item);
                spawner.getWorld().spawn(spawner.getLocation(), ExperienceOrb.class, orb -> orb.setExperience(accumulator.getAccumulatedExperience()));
            });
            spawners.remove(spawner);
        }
    }

    @EventHandler
    private void onEntityDeath(final EntityDeathEvent event) {
        if (event.getEntity().fromMobSpawner()) {

            final CreatureSpawner spawner       = this.entities.get(event.getEntity());
            final SpawnerAccumulator jsonInventory = getSpawnerAccumulator(spawner);

            int exp = ((int) (Math.random() * this.getConfig().getInt("xp-drop")));
            spawner.getPersistentDataContainer().set(itemsKey, PersistentDataType.STRING, jsonInventory.add(event.getDrops()).toString());
            spawner.update();

            jsonInventory.setAccumulatedExperience(jsonInventory.getAccumulatedExperience() + exp);
            jsonInventory.getChestMenu().update();

            event.setCancelled(true);
            event.getEntity().remove();
            this.entities.remove(event.getEntity());
        }
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && event.getClickedBlock() != null && event.getClickedBlock().getType().equals(Material.SPAWNER)) {

            final CreatureSpawner spawner = ((CreatureSpawner) event.getClickedBlock().getState());

            // Spawn egg detection
            if (event.getPlayer().getInventory().getItemInMainHand().getType().toString().contains("SPAWN_EGG")) return;

            final SpawnerAccumulator jsonInventory = getSpawnerAccumulator(spawner);
            jsonInventory.openChestMenu(event.getPlayer());
        }
    }

    @EventHandler
    private void onInventoryClose(final InventoryCloseEvent event) {

        final Inventory     inventory     = event.getInventory();
        final SpawnerAccumulator jsonInventory = spawners.values().stream().filter(ji -> ji.getInventory().equals(inventory)).findAny().orElse(null);

        if (jsonInventory == null)
            return;

        final CreatureSpawner spawner = jsonInventory.getSpawner();
        spawner.getPersistentDataContainer().set(itemsKey, PersistentDataType.STRING, jsonInventory.toString());

        // spawner.update() вызывает провоцирует тик спавна мобов, что добавляет потенциальный абуз, ломающий баланс
    }

}