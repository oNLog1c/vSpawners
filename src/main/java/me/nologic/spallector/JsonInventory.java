package me.nologic.spallector;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Objects;

@Getter
public class JsonInventory {

    private final CreatureSpawner spawner;

    @Setter
    private Inventory inventory;

    public JsonInventory(final CreatureSpawner spawner) {

        final String data = spawner.getPersistentDataContainer().get(Spallector.getItemsKey(), PersistentDataType.STRING);
        this.spawner = spawner;

        if (data != null) {
            this.inventory = this.parseInventory(data);
        } else this.inventory = Bukkit.createInventory(null, 9 * Spallector.getInstance().getConfig().getInt("inventory.rows") * 9, Component.text(Objects.requireNonNull(Spallector.getInstance().getConfig().getString("inventory.title"))));

    }

    public JsonInventory(final CreatureSpawner spawner, final Inventory inventory) {
        this.inventory = inventory;
        this.spawner = spawner;
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
        Inventory inv = Bukkit.createInventory(null, Spallector.getInstance().getConfig().getInt("inventory.rows") * 9, Component.text(Objects.requireNonNull(Spallector.getInstance().getConfig().getString("inventory.title"))));
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