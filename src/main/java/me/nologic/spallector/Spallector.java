package me.nologic.spallector;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

public final class Spallector extends JavaPlugin implements Listener {

    @Getter
    private static NamespacedKey namespacedKey;
    private HashMap<Entity, CreatureSpawner>    spawners;
    private HashMap<Inventory, CreatureSpawner> inventories;

    @Getter
    private static Spallector instance;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        namespacedKey = new NamespacedKey(this, "spallector");
        spawners = new HashMap<>();
        inventories = new HashMap<>();
        super.getServer().getPluginManager().registerEvents(this, this);
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
            this.spawners.get(event.getEntity()).getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, new JsonInventory(spawners.get(event.getEntity())).add(event.getDrops()).toString());
            this.spawners.get(event.getEntity()).update();
            event.setCancelled(true);
            event.getEntity().remove();
        }
    }

    @EventHandler
    private void onPlayerInteract(final PlayerInteractEvent event) {
        if (event.getAction().isRightClick() && event.getClickedBlock() != null && event.getClickedBlock().getType().equals(Material.SPAWNER)) {

            if (event.getPlayer().isSneaking())
                return;

            final Inventory inventory = new JsonInventory((CreatureSpawner) event.getClickedBlock().getState()).getInventory();
            this.inventories.put(inventory, (CreatureSpawner) event.getClickedBlock().getState());
            event.getPlayer().openInventory((inventory));
        }
    }

    @EventHandler
    private void onInventoryClose(final InventoryCloseEvent event) {

        final Inventory inventory = event.getInventory();

        if (inventories.containsKey(inventory)) {
            final CreatureSpawner spawner = inventories.get(inventory);
            final JsonInventory jsonInventory = new JsonInventory(inventory);
            spawner.getPersistentDataContainer().set(namespacedKey, PersistentDataType.STRING, jsonInventory.toString());
            spawner.update();
            inventories.remove(inventory);
        }
    }

}