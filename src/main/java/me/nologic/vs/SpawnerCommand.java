package me.nologic.vs;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.Subcommand;
import lombok.SneakyThrows;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;

@CommandAlias("vspawners|vsp|spawner")
public class SpawnerCommand extends BaseCommand {

    @Subcommand("add")
    @CommandCompletion("") @SneakyThrows
    private void add(final Player sender, final EntityType entityType, final String range) {
        final ItemStack item = sender.getInventory().getItemInMainHand();
        vSpawners.getInstance().getConfig().set("item.item.item." + entityType.translationKey(), item.serialize());
        vSpawners.getInstance().getConfig().save(new File(vSpawners.getInstance().getDataFolder(), "govno.yml"));
    }

}