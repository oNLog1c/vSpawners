package me.nologic.vs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.ipvp.canvas.slot.ClickOptions;
import org.ipvp.canvas.type.ChestMenu;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;

@Getter
public class JsonInventory {

    private final CreatureSpawner spawner;

    private final ChestMenu chestMenu;
    private final Inventory inventory;

    public JsonInventory(final CreatureSpawner spawner) {

        final String data = spawner.getPersistentDataContainer().get(vSpawners.getItemsKey(), PersistentDataType.STRING);
        this.spawner = spawner;

        this.chestMenu = this.buildChestGUI();

        if (data != null) {
            this.inventory = this.parseInventory(data);
        } else this.inventory = Bukkit.createInventory(null, vSpawners.getInstance().getConfig().getInt("inventory.rows") * 9, Component.text(Objects.requireNonNull(vSpawners.getInstance().getConfig().getString("inventory.title"))));

    }

    private ChestMenu buildChestGUI() {
        final ChestMenu menu = ChestMenu.builder(1).title(spawner.getSpawnedType() + " " + vSpawners.getInstance().getConfig().getString("inventory.title")).build();;
        menu.forEach(slot -> slot.setClickOptions(ClickOptions.DENY_ALL));
        menu.getSlot(11).setItem(new ItemStack(Material.ROTTEN_FLESH));
        menu.getSlot(11).setClickHandler((player, info) -> player.openInventory(inventory));
        menu.getSlot(15).setItem(new ItemStack(Material.EXPERIENCE_BOTTLE));
        return  menu;
    }

    public JsonInventory add(final List<ItemStack> items) {
        for (ItemStack item : items) {
            this.inventory.addItem(item);
        }
        return this;
    }

    @Override
    public String toString() {

        JsonObject obj = new JsonObject();

        obj.addProperty("type", inventory.getType().name());
        obj.addProperty("size", inventory.getSize());

        JsonArray items = new JsonArray();
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null) {
                JsonObject jsonItem = new JsonObject();
                jsonItem.addProperty("slot", i);
                String itemData = serializeItemStack(item);
                jsonItem.addProperty("data", itemData);
                items.add(jsonItem);
            }
        }
        obj.add("items", items);

        return obj.toString();
    }

    private Inventory parseInventory(final String inventoryJson) {
        JsonObject obj = JsonParser.parseString(inventoryJson).getAsJsonObject();
        Inventory inv = Bukkit.createInventory(null, vSpawners.getInstance().getConfig().getInt("inventory.rows") * 9, Component.text(Objects.requireNonNull(vSpawners.getInstance().getConfig().getString("inventory.title"))));
        JsonArray items = obj.get("items").getAsJsonArray();
        for (final JsonElement element : items) {
            JsonObject jsonItem = element.getAsJsonObject();
            ItemStack item = deserializeItemStack(jsonItem.get("data").getAsString());
            inv.setItem(jsonItem.get("slot").getAsInt(), item);
        }
        return inv;
    }

    @SneakyThrows
    protected final String serializeItemStack(final ItemStack item) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeObject(item);
        dataOutput.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

    @SneakyThrows
    private ItemStack deserializeItemStack(final String base64) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();
        return item;
    }

}