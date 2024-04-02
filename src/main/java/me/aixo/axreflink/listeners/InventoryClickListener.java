package me.aixo.axreflink.listeners;

import me.aixo.axreflink.database.DatabaseManager;
import me.aixo.axreflink.gui.ReferralGUI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Map;

public class InventoryClickListener implements Listener {

    private Plugin plugin;
    private DatabaseManager databaseManager;

    public InventoryClickListener(Plugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {

        if (event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        String playerName = player.getName();
        Inventory inventory = event.getInventory();
        Material emptySlotItem = Material.getMaterial(plugin.getConfig().getString("empty-slot-item"));


        if (inventory.getHolder() instanceof ReferralGUI) {
            event.setCancelled(true);

            int slot = event.getRawSlot();
            ItemStack item = inventory.getItem(slot);

            if (item == null || item.getType() == emptySlotItem) {
                return;
            }

            List<Map<?, ?>> rewardsList = plugin.getConfig().getMapList("rewards");

            for (Map<?, ?> rewardMap : rewardsList) {
                int requiredReferrals = (int) rewardMap.get("requiredReferrals");
                int rewardSlot = (int) rewardMap.get("slot");
                List<String> commands = (List<String>) rewardMap.get("commands");

                if (rewardSlot == slot) {
                    int referralsCount = databaseManager.getReferralsCount(playerName);

                    List<Integer> claimedRewards = databaseManager.getClaimedRewards(playerName);
                    if (claimedRewards.contains(slot)) {
                        player.sendMessage(ChatColor.RED + "Odebrales już tą nagrodę");
                        return;
                    }
                    if (referralsCount >= requiredReferrals) {
                        for (String cmd : commands) {
                            String formattedCommand = cmd.replace("%player%", playerName);
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), formattedCommand);
                        }

                        player.sendMessage(ChatColor.GREEN + "Odebraleś nagrodę!");

                        databaseManager.claimReward(playerName, slot);

                        claimedRewards.add(slot);

                        ReferralGUI gui = new ReferralGUI(player, referralsCount, databaseManager, plugin, claimedRewards);
                        gui.open();
                    } else {
                        player.sendMessage(ChatColor.RED + "Nie masz wystarczającej liczby zaproszonych osób, aby odebrać nagrodę");
                    }
                }
            }
        }
    }
}