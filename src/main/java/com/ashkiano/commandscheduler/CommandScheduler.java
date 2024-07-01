package com.ashkiano.commandscheduler;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CommandScheduler extends JavaPlugin {

    @Override
    public void onEnable() {
        this.getCommand("schedule").setExecutor(new CommandSchedulerExecutor());
        saveDefaultConfig();
        startScheduler();
        Metrics metrics = new Metrics(this, 22153);
        this.getLogger().info("Thank you for using the CommandScheduler plugin! If you enjoy using this plugin, please consider making a donation to support the development. You can donate at: https://donate.ashkiano.com");
    }

    private void startScheduler() {
        new BukkitRunnable() {
            @Override
            public void run() {
                scheduleConfiguredCommands();
            }
        }.runTaskTimer(this, 0L, 20 * 60); // Check every minute
    }

    private void scheduleConfiguredCommands() {
        FileConfiguration config = getConfig();
        List<String> commands = config.getStringList("scheduled-commands");

        for (String entry : commands) {
            String[] parts = entry.split(" ", 2);
            String time = parts[0];
            String command = parts[1];

            LocalTime scheduledTime = LocalTime.parse(time, DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime now = LocalTime.now();

            if (shouldRunCommand(scheduledTime, now, config.getString("last-run." + command))) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                config.set("last-run." + command, now.toString());
                saveConfig();
            }
        }
    }

    private boolean shouldRunCommand(LocalTime scheduledTime, LocalTime now, String lastRun) {
        if (lastRun == null) {
            return true; // Never run before
        }

        LocalTime lastRunTime = LocalTime.parse(lastRun);
        return now.isAfter(scheduledTime) && (lastRunTime.isBefore(scheduledTime) || lastRunTime.isAfter(now));
    }

    class CommandSchedulerExecutor implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
            if (!sender.hasPermission("commandscheduler.schedule")) {
                sender.sendMessage("You do not have permission to use this command.");
                return true;
            }

            if (args.length < 2) {
                sender.sendMessage("Usage: /schedule <delay> <command>");
                return false;
            }

            try {
                long delay = Long.parseLong(args[0]) * 20; // Convert seconds to ticks
                String command = String.join(" ", args).substring(args[0].length()).trim();

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }.runTaskLater(CommandScheduler.getPlugin(CommandScheduler.class), delay);

                sender.sendMessage("Scheduled command: " + command + " to run in " + args[0] + " seconds.");
            } catch (NumberFormatException e) {
                sender.sendMessage("Invalid delay value. Please enter a number.");
                return false;
            }

            return true;
        }
    }
}