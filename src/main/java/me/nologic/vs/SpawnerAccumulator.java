package me.nologic.vs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.SneakyThrows;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.ipvp.canvas.type.ChestMenu;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;

@Getter
public final class SpawnerAccumulator {

    private final SpawnerManager  manager;
    private final CreatureSpawner spawner;

    private final ChestMenu menu;
    private final Inventory inventory;

    private final String title;
    private final int    inventorySize;

    private int accumulatedExperience;

    public SpawnerAccumulator(final SpawnerManager spawnerManager, final CreatureSpawner spawner) {

        this.manager = spawnerManager;

        final String data   = spawner.getPersistentDataContainer().get(manager.getItemsKey(), PersistentDataType.STRING);
        this.spawner        = spawner;
        this.title          = Objects.requireNonNull(vSpawners.getInstance().getConfig().getString("inventory.title"));
        this.inventorySize  = vSpawners.getInstance().getConfig().getInt("inventory.rows") * 9;
        this.menu = ChestMenu.builder(1).title(String.format(title, spawner.getSpawnedType())).build();
        this.accumulatedExperience = spawner.getPersistentDataContainer().getOrDefault(manager.getExperienceKey(), PersistentDataType.INTEGER, 0);

        if (data != null) {
            this.inventory = this.parseInventory(data);
        } else this.inventory = Bukkit.createInventory(null, inventorySize, Component.text(String.format(title, spawner.getSpawnedType())));
    }

    public void openChestMenu(final Player viewer) {

        // Item button
        menu.getSlot(2).setItem(Button.DROP_BUTTON.update(this));
        menu.getSlot(2).setClickHandler((player, info) -> {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 2F);

            if (info.getClickType().isLeftClick()) {
                player.openInventory(inventory);
            } else if (info.getClickType().isRightClick()) {

                final ItemStack[] items = Arrays.stream(inventory.getContents()).filter(Objects::nonNull).toArray(ItemStack[]::new); // Removing null array elements.
                if (items.length > 0) {

                    final HashMap<Integer, ItemStack> fitBack = player.getInventory().addItem(items);
                    if (!fitBack.isEmpty()) {
                        player.sendMessage(Component.text(Objects.requireNonNull(vSpawners.getInstance().getConfig().getString("message.not-enough-space"))));
                        player.playSound(player, Sound.ENTITY_VILLAGER_NO, 1F, 2F);
                    }

                    inventory.setContents(fitBack.values().toArray(new ItemStack[0])); // To prevent some items from getting lost, we return those that do not fit back.
                    player.playSound(player, Sound.BLOCK_BARREL_OPEN, 1F, 0.3F);
                }
            }
        });

        // Experience button
        menu.getSlot(6).setItem(Button.EXP_BUTTON.update(this));
        menu.getSlot(6).setClickHandler((player, info) -> {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 1F, 2F);

            if (accumulatedExperience == 0)
                return;

            player.giveExp(accumulatedExperience);
            this.setAccumulatedExperience(0);
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1F, 1F);
            menu.update();
        });

        menu.update();
        menu.open(viewer);
    }

    public void setAccumulatedExperience(final int exp) {
        this.accumulatedExperience = exp;
        spawner.getPersistentDataContainer().set(manager.getExperienceKey(), PersistentDataType.INTEGER, accumulatedExperience);
        menu.getSlot(6).setItem(Button.EXP_BUTTON.update(this));
    }

    public SpawnerAccumulator addItems(final List<ItemStack> items) {
        for (ItemStack item : items) {
            this.inventory.addItem(item);
        }
        return this;
    }

    private enum Button {

        DROP_BUTTON(Material.valueOf(vSpawners.getInstance().getConfig().getString("inventory.buttons.inspect-drop-button.material-type")),
                vSpawners.getInstance().getConfig().getString("inventory.buttons.inspect-drop-button.title"),
                vSpawners.getInstance().getConfig().getStringList("inventory.buttons.inspect-drop-button.lore")
        ),
        EXP_BUTTON(Material.valueOf(vSpawners.getInstance().getConfig().getString("inventory.buttons.exp-button.material-type")),
                vSpawners.getInstance().getConfig().getString("inventory.buttons.exp-button.title"),
                vSpawners.getInstance().getConfig().getStringList("inventory.buttons.exp-button.lore")
        );

        private final ItemStack    button;
        private final String       title;
        private final List<String> lore;
        private final Map<String, Object> placeholders = new HashMap<>();

        Button(final Material type, final String title, final List<String> lore) {
            this.button = new ItemStack(type);
            this.title  = title;
            this.lore   = lore;
        }

        @SuppressWarnings("deprecation")
        public ItemStack update(SpawnerAccumulator spawner) {

            final ItemMeta meta = button.getItemMeta();
            placeholders.put("exp", spawner.getAccumulatedExperience());

            final List<TextComponent> loreComponent = new ArrayList<>();
            lore.forEach(line -> loreComponent.add(Component.text(StrSubstitutor.replace(line, placeholders, "${", "}"))));

            meta.displayName(Component.text(title));
            meta.lore(loreComponent);
            button.setItemMeta(meta);
            return this.button;
        }

    }

    /* --- JSON --- */

    public String serializeInventory() {

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
        Inventory inv = Bukkit.createInventory(null, inventorySize, Component.text(String.format(title, spawner.getSpawnedType())));
        JsonArray items = obj.get("items").getAsJsonArray();
        for (final JsonElement element : items) {
            JsonObject jsonItem = element.getAsJsonObject();
            ItemStack item = deserializeItemStack(jsonItem.get("data").getAsString());
            inv.setItem(jsonItem.get("slot").getAsInt(), item);
        }
        return inv;
    }

    @SneakyThrows
    public String serializeItemStack(final ItemStack item) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);
        dataOutput.writeObject(item);
        dataOutput.close();
        return Base64Coder.encodeLines(outputStream.toByteArray());
    }

    @SneakyThrows
    public ItemStack deserializeItemStack(final String base64) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(base64));
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
        ItemStack item = (ItemStack) dataInput.readObject();
        dataInput.close();
        return item;
    }

}