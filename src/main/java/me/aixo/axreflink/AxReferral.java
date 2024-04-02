package me.aixo.axreflink;

import me.aixo.axreflink.commands.ReferralCommand;
import me.aixo.axreflink.database.DatabaseManager;
import me.aixo.axreflink.listeners.InventoryClickListener;
import me.aixo.axreflink.listeners.PlayerJoinListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class AxReferral extends JavaPlugin {

    private DatabaseManager databaseManager;


    @Override
    public void onEnable() {
        saveDefaultConfig();
        databaseManager = new DatabaseManager(this);

        getCommand("reflink").setExecutor(new ReferralCommand(this, databaseManager));

        getServer().getPluginManager().registerEvents(new InventoryClickListener(this, databaseManager), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(databaseManager), this);

    }

    @Override
    public void onDisable() {
        databaseManager.close();
    }
}
