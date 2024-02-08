package me.nologic.vs;

import lombok.Getter;
import co.aikar.commands.PaperCommandManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.ipvp.canvas.MenuFunctionListener;

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