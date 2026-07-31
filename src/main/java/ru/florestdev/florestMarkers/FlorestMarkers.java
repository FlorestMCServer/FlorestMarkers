package ru.florestdev.florestMarkers;

import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.POIMarker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

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

            Bukkit.getLogger().info("[BlueMapMarkers] BlueMap подключен");

            loadMarkers();
        });
    }


    @Override
    public boolean onCommand(CommandSender sender,
                             Command command,
                             String label,
                             String[] args) {


        if (!(sender instanceof Player player)) {
            return true;
        }

        if (!sender.hasPermission("markers.add")) {
            sender.sendMessage(ChatColor.RED + "У вас нет разрешения markers.add!");
            return true;
        }

        if (args.length < 2) {
            player.sendMessage("§c/map add <название>");
            return true;
        }


        if (!args[0].equalsIgnoreCase("add")) {
            return true;
        }


        String name = String.join(" ",
                Arrays.copyOfRange(args, 1, args.length));


        Location loc = player.getLocation();


        data.set(name + ".world", loc.getWorld().getName());
        data.set(name + ".x", loc.getX());
        data.set(name + ".y", loc.getY());
        data.set(name + ".z", loc.getZ());

        save();


        addMarker(
                name,
                loc.getWorld().getName(),
                loc.getX(),
                loc.getY(),
                loc.getZ()
        );


        player.sendMessage(
                "§aМетка создана: §f" + name
        );


        return true;
    }



    private void addMarker(String name, String world, double x, double y, double z) {

        if (blueMapAPI == null)
            return;

        blueMapAPI.getWorld(world).ifPresent(bWorld -> {

            bWorld.getMaps()
                    .stream()
                    .findFirst()
                    .ifPresent(map -> {

                        MarkerSet set = map.getMarkerSets()
                                .computeIfAbsent(
                                        "player_markers",
                                        id -> MarkerSet.builder()
                                                .label("Метки игроков")
                                                .build()
                                );


                        POIMarker marker = POIMarker.builder()
                                .label(name)
                                .position(new Vector3d(x, y, z))
                                .build();


                        set.getMarkers().put(name, marker);

                    });

        });
    }



    private void loadMarkers() {

        for (String name : data.getKeys(false)) {

            String world =
                    data.getString(name + ".world");


            double x =
                    data.getDouble(name + ".x");

            double y =
                    data.getDouble(name + ".y");

            double z =
                    data.getDouble(name + ".z");


            addMarker(
                    name,
                    world,
                    x,
                    y,
                    z
            );
        }
    }



    private void save() {

        try {
            data.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}