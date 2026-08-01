package ru.florestdev.florestMarkers;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class FlorestMarkers extends JavaPlugin implements CommandExecutor {

    private File file;
    private FileConfiguration data;
    private BlueMapAPI blueMapAPI;

    @Override
    public void onEnable() {
        file = new File(getDataFolder(), "markers.yml");

        if (!file.exists()) {
            try {
                getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        data = YamlConfiguration.loadConfiguration(file);

        getCommand("map").setExecutor(this);

        BlueMapAPI.onEnable(api -> {
            blueMapAPI = api;
            Bukkit.getLogger().info("[FlorestMarkers] BlueMap подключен");
            loadAllMarkers();
        });
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Только для игроков");
            return true;
        }

        if (!player.hasPermission("markers.add")) {
            player.sendMessage(ChatColor.RED + "У вас нет разрешения markers.add!");
            return true;
        }

        if (args.length < 2 || !args[0].equalsIgnoreCase("add")) {
            player.sendMessage("§cИспользование: /map add <название>");
            return true;
        }

        String name = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Location loc = player.getLocation();

        String path = name;
        data.set(path + ".world", loc.getWorld().getName());
        data.set(path + ".x", loc.getX());
        data.set(path + ".y", loc.getY());
        data.set(path + ".z", loc.getZ());
        save();

        addMarker(name, loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ());

        player.sendMessage("§aМетка создана: §f" + name);
        return true;
    }

    // ======================= ЗАГРУЗКА ВСЕХ МАРКЕРОВ (РЕКУРСИЯ) =======================

    private void loadAllMarkers() {
        if (blueMapAPI == null) return;

        ConfigurationSection root = data.getRoot();
        if (root == null) return;

        // Проходим по всем корневым ключам
        for (String key : root.getKeys(false)) {
            Object obj = root.get(key);
            if (obj instanceof ConfigurationSection section) {
                // Проверяем, является ли сама секция конечной точкой
                if (isMarkerSection(section)) {
                    addMarkerFromSection(key, section);
                } else {
                    // Иначе идём глубже
                    traverseSection(section, key);
                }
            }
        }
    }

    private void traverseSection(ConfigurationSection section, String currentPath) {
        for (String key : section.getKeys(false)) {
            String fullPath = currentPath + " → " + key;
            Object obj = section.get(key);

            if (obj instanceof ConfigurationSection subSection) {
                if (isMarkerSection(subSection)) {
                    addMarkerFromSection(fullPath, subSection);
                } else {
                    traverseSection(subSection, fullPath);
                }
            }
        }
    }

    private boolean isMarkerSection(ConfigurationSection section) {
        return section.contains("world") && section.contains("x") && section.contains("y") && section.contains("z");
    }

    private void addMarkerFromSection(String name, ConfigurationSection section) {
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        addMarker(name, world, x, y, z);
    }

    // ======================= ДОБАВЛЕНИЕ МАРКЕРА В BLUEMAP =======================

    private void addMarker(String name, String world, double x, double y, double z) {
        if (blueMapAPI == null) return;

        blueMapAPI.getWorld(world).ifPresent(bWorld -> {
            bWorld.getMaps().stream().findFirst().ifPresent(map -> {
                MarkerSet set = map.getMarkerSets().computeIfAbsent(
                        "player_markers",
                        id -> MarkerSet.builder()
                                .label("Метки игроков")
                                .build()
                );

                POIMarker marker = POIMarker.builder()
                        .label(name)
                        .position(new Vector3d(x, y, z))
                        .build();

                set.getMarkers().put(UUID.randomUUID().toString(), marker);
            });
        });
    }

    // ======================= СОХРАНЕНИЕ =======================

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}