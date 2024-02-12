package me.nologic.vs;

import lombok.Getter;
import co.aikar.commands.PaperCommandManager;
import lombok.Setter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

import java.io.File;

public final class vSpawners extends JavaPlugin {

    @Getter
    private PaperCommandManager commandManager;

    @Getter @Setter
    private YamlConfiguration lootConfig;

    @Override
    public void onEnable() {
        instance = this;
        this.manageConfigs();
        this.commandManager = new PaperCommandManager(this);
        this.commandManager.registerCommand(new SpawnerCommand());
        SpawnerManager spawnerManager = new SpawnerManager(this);
        super.getServer().getPluginManager().registerEvents(new MenuFunctionListener(), this);
    }

    private void manageConfigs() {

        super.saveDefaultConfig();
        super.reloadConfig();

        final File lootConfigFile = new File(super.getDataFolder(), "loot.yml");
        if (!lootConfigFile.exists()) {
            super.saveResource("loot.yml", false);
        }

        this.lootConfig = YamlConfiguration.loadConfiguration(lootConfigFile);
    }

    @Getter
    private static vSpawners instance;

}