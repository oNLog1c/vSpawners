package me.nologic.vs;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import lombok.SneakyThrows;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandAlias("vspawners|vsp|spawner")
public class SpawnerCommand extends BaseCommand {

    private final vSpawners plugin = vSpawners.getInstance();
    private final List<String> types = new ArrayList<>();

    public SpawnerCommand() {

        for (EntityType type : EntityType.values()) {
            types.add(type.toString());
        }

        plugin.getCommandManager().getCommandCompletions().registerCompletion("entityTypes", c -> types);
    }

    @SneakyThrows
    @Subcommand("add")
    @CommandCompletion("@entityTypes 0-100")
    @CommandPermission("vspawners.command.add")
    private void add(final Player sender, final String entityType, final String range) {

        final ItemStack         item       = sender.getInventory().getItemInMainHand();
        final YamlConfiguration lootConfig = plugin.getLootConfig();
        final File              configPath = new File(plugin.getDataFolder(), "loot.yml");

        try {
            EntityType.valueOf(entityType);
        } catch (final IllegalArgumentException exception) {
            sender.sendMessage(String.format("§c[!] §7Non-existing entity type: §6%s§7!", entityType));
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeObject(item);
        dataOutput.close();

        lootConfig.set("spawner-drops." + entityType + "." + range, Base64Coder.encodeLines(outputStream.toByteArray()));
        lootConfig.save(configPath);
        plugin.setLootConfig(YamlConfiguration.loadConfiguration(configPath));

        sender.sendMessage(String.format("§c[!] §7Now §e%s §7will drop from §6%s §7with a chance of §a%s§7. (dice: §4%s§7)", item.getType(), entityType, range, lootConfig.getInt("spawner-drops.dice")));
        sender.playSound(sender, Sound.ENTITY_CHICKEN_EGG, 1, 2);
    }

}