package me.nologic.vs;

import co.aikar.commands.PaperCommandManager;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
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
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class vSpawners extends JavaPlugin {

    @Getter
    private PaperCommandManager commandManager;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.commandManager = new PaperCommandManager(this);
        super.getServer().getPluginManager().registerEvents(new MenuFunctionListener(), this);
    }

    @Getter
    private static vSpawners instance;

}