package me.aixo.axreflink.gui;

import me.aixo.axreflink.database.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReferralGUI implements InventoryHolder {

    private final List<Integer> claimedRewards;
    private Player player;
    private Inventory inventory;
    private int referralsCount;
    private ConfigurationSection rewards;

    private DatabaseManager databaseManager;

    public ReferralGUI(Player player, int referralsCount, DatabaseManager databaseManager, Plugin plugin, List<Integer> claimedRewards) {
        this.player = player;
        this.referralsCount = referralsCount;
        this.claimedRewards = claimedRewards;
        this.databaseManager = databaseManager;

        int inventorySize = 27; // wielkosc gui
        this.inventory = Bukkit.createInventory(this, inventorySize,"§7System Partnerski");
        

        Material emptySlotItem = Material.getMaterial(plugin.getConfig().getString("empty-slot-item"));
        ItemStack emptyItem = new ItemStack(emptySlotItem);
        ItemMeta emptyMeta = emptyItem.getItemMeta();
        emptyMeta.setDisplayName(" ");
        emptyItem.setItemMeta(emptyMeta);
        for (int i = 0; i < inventorySize; i++) {
            inventory.setItem(i, emptyItem);
        }


        // dodawanie itemow do gui
        List<Map<?, ?>> rewardsList = plugin.getConfig().getMapList("rewards");

        for (Map<?, ?> rewardMap : rewardsList) {
            int requiredReferrals = (int) rewardMap.get("requiredReferrals");
            int slot = (int) rewardMap.get("slot");
            String name = (String) rewardMap.get("name");
            String lore = (String) rewardMap.get("lore");
            ItemStack item;
            ItemMeta meta;

            if (claimedRewards.contains(slot)) {
                // Szara szyba (Odebrana nagroda)
                item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                meta = item.getItemMeta();
                meta.setDisplayName(String.format("§8%s", name));
                List<String> loreList = new ArrayList<>();
                loreList.add(String.format("§7%s", lore ));
                loreList.add("");
                loreList.add("§7Odbierz nagrodę: §8Odebrane");
                meta.setLore(loreList);
                item.setItemMeta(meta);

            } else if (referralsCount >= requiredReferrals) {
                // Zielona szyba (dostępna nagroda)
                item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.GREEN + name);
                List<String> loreList = new ArrayList<>();
                loreList.add(ChatColor.GRAY + lore);
                loreList.add("");
                loreList.add("§7Odbierz nagrodę: §2Dostępne");
                meta.setLore(loreList);
                item.setItemMeta(meta);
            } else {
                // Czerwona szyba (niedostępna nagroda)
                item = new ItemStack(Material.RED_STAINED_GLASS_PANE);
                meta = item.getItemMeta();
                meta.setDisplayName(ChatColor.RED + name);
                List<String> loreList = new ArrayList<>();
                loreList.add(ChatColor.GRAY + lore);
                loreList.add("");
                loreList.add(String.format("§7Odbierz nagrodę: §cNiedostępne §7(§a%s§7/§c%s§7)", referralsCount, requiredReferrals));
                meta.setLore(loreList);
                item.setItemMeta(meta);
            }

            inventory.setItem(slot, item);
        }

        ItemStack infoItem = new ItemStack(Material.PAPER);
        ItemMeta infoMeta = infoItem.getItemMeta();
        infoMeta.setDisplayName("§7Zaproszenia");
        List<String> infoLore = new ArrayList<>();
        infoLore.add(ChatColor.GRAY + "§7Liczba zaproszeń: " + referralsCount);
        infoMeta.setLore(infoLore);
        infoItem.setItemMeta(infoMeta);
        inventory.setItem(26, infoItem);


        ItemStack topItem = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta topMeta = topItem.getItemMeta();
        topMeta.setDisplayName(ChatColor.GOLD + "Top 5");
        List<String> topLore = new ArrayList<>();
        List<Map.Entry<String, Integer>> topReferrals = databaseManager.getTopReferrals();
        int rank = 1;
        for (Map.Entry<String, Integer> entry : topReferrals) {
            topLore.add(ChatColor.GRAY.toString() + rank + ". " + entry.getKey() + ": " + entry.getValue());
            rank++;
        }
        topLore.add("");
        topLore.add(ChatColor.YELLOW + "Twoja pozycja: " + databaseManager.getPlayerRank(player.getName()));
        topMeta.setLore(topLore);
        topItem.setItemMeta(topMeta);
        inventory.setItem(25, topItem);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open() {
        player.openInventory(inventory);
    }
}

