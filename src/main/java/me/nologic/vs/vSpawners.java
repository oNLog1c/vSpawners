package me.nologic.vs;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;

public final class vSpawners extends JavaPlugin implements Listener {

    @Getter
    private static NamespacedKey itemsKey;

    @Getter
    private static NamespacedKey expKey;

    private HashMap<Entity, CreatureSpawner>        spawners;
    private HashMap<CreatureSpawner, JsonInventory> inventories;

    @Getter
    private static vSpawners instance;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        itemsKey = new NamespacedKey(this, "items");
        expKey   = new NamespacedKey(this, "exp");
        spawners = new HashMap<>();
        inventories = new HashMap<>();
        super.getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        this.inventories.keySet().forEach(BlockState::update);
    }

    @EventHandler
    private void onSpawnerSpawn(final SpawnerSpawnEvent event) {
        if (event.getSpawner() == null) return;
        this.spawners.put(event.getEntity(), event.getSpawner());
        ((LivingEntity) event.getEntity()).damage(((LivingEntity) event.getEntity()).getHealth());
    }

    @EventHandler
    private void onEntityDeath(final EntityDeathEvent event) {
        if (event.getEntity().fromMobSpawner()) {

            final CreatureSpawner spawner = this.spawners.get(event.getEntity());

            // LAZY
            final JsonInventory jsonInventory;
            if (inventories.get(spawner) != null) {
                jsonInventory = inventories.get(spawner);
            } else {
                jsonInventory = new JsonInventory(spawner);
                inventories.put(spawner, jsonInventory);
            }

            spawner.getPersistentDataContainer().set(itemsKey, PersistentDataType.STRING, jsonInventory.add(event.getDrops()).toString());
            spawner.getPersistentDataContainer().set(expKey, PersistentDataType.INTEGER, spawner.getPersistentDataContainer().getOrDefault(expKey, PersistentDataType.INTEGER, 0) + ((int) (Math.random() * this.getConfig().getInt("xp-drop"))));
            spawner.update();

            event.setCancelled(true);
            event.getEntity().remove();
        }
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && event.getClickedBlock() != null && event.getClickedBlock().getType().equals(Material.SPAWNER)) {

            final CreatureSpawner spawner = ((CreatureSpawner) event.getClickedBlock().getState());

            // EXP
            if (event.getPlayer().isSneaking()) {
                final int exp = spawner.getPersistentDataContainer().getOrDefault(expKey, PersistentDataType.INTEGER, 0);
                spawner.getPersistentDataContainer().set(expKey, PersistentDataType.INTEGER, 0);
                event.getPlayer().giveExp(exp);
                spawner.update();
                return;
            }

            // LAZY
            final JsonInventory jsonInventory;
            if (inventories.get(spawner) != null) {
                jsonInventory = inventories.get(spawner);
            } else {
                jsonInventory = new JsonInventory(spawner);
                inventories.put(spawner, jsonInventory);
            }

            event.getPlayer().openInventory((jsonInventory.getInventory()));
        }
    }

    @EventHandler
    private void onInventoryClose(final InventoryCloseEvent event) {

        final Inventory     inventory     = event.getInventory();
        final JsonInventory jsonInventory = inventories.values().stream().filter(ji -> ji.getInventory().equals(inventory)).findAny().orElse(null);

        if (jsonInventory == null)
            return;

        final CreatureSpawner spawner = jsonInventory.getSpawner();
        spawner.getPersistentDataContainer().set(itemsKey, PersistentDataType.STRING, jsonInventory.toString());

        // spawner.update() вызывает провоцирует тик спавна мобов, что добавляет потенциальный абуз, ломающий баланс
    }

}