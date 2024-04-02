package me.aixo.axreflink.commands;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.User;
import me.aixo.axreflink.database.DatabaseManager;
import me.aixo.axreflink.gui.ReferralGUI;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ReferralCommand implements CommandExecutor {

    private Plugin plugin;
    private DatabaseManager databaseManager;


    public ReferralCommand(Plugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Tylko gracze mogą używać tego polecenia");
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();

        if (args.length == 0) {
            int referralsCount = databaseManager.getReferralsCount(playerName);
            List<Integer> claimedRewards = databaseManager.getClaimedRewards(playerName);

            ReferralGUI gui = new ReferralGUI(player, referralsCount, databaseManager, plugin, claimedRewards);
            gui.open();
            return true;
        }

        // Sprawdzenie czy gracz ma synchronizowane konto z Discordem
        if (!databaseManager.isAccountSyncedWithDiscord(player.getUniqueId())) {
            player.sendMessage("§cMusisz mieć synchronizowane konto z Discordem, aby skorzystać z reflinka! §7(/discord)");
            return true;
        }

        Essentials essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        User user = essentials.getUser(player.getUniqueId());
        long playTime = user.getBase().getStatistic(Statistic.PLAY_ONE_MINUTE);
        long playTimeSeconds = playTime / 20;
        if (playTimeSeconds < 60 * 60) {
            long remainingTime = 60 * 60 - playTimeSeconds;
            long remainingMinutes = remainingTime / 60;
            long remainingSeconds = remainingTime % 60;
            player.sendMessage("§cAby skorzystać z reflinka musisz rozegrać jeszcze " + remainingMinutes + " minut i " + remainingSeconds + " sekund na serwerze");
            return true;
        }

        if (args.length == 1) {
            String argument = args[0];

            if (argument.equalsIgnoreCase("reload")) {
                if (player.hasPermission("reflink.reload")) {
                    plugin.reloadConfig();
                    player.sendMessage("§2 Plugin zreloadowany");
                } else {
                    player.sendMessage("§cNie masz permisji.");
                }
                return true;
            }

            String referrerName = argument;

            if (databaseManager.hasUsedReferralLink(playerName)) {
                player.sendMessage("§cSkorzystałeś już z linku polecającego");
                return true;
            }

            if (playerName.equalsIgnoreCase(referrerName)) {
                player.sendMessage("§cNie możesz użyć własnego linku polecającego");
                return true;
            }

            if (!databaseManager.isValidReferralLink(referrerName)) {
                player.sendMessage("§cNieprawidłowy nick polecającego");
                return true;
            }


            databaseManager.useReferralLink(referrerName, playerName);
            String rewardCommand = plugin.getConfig().getString("referral-reward-command").replace("%player%", playerName);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rewardCommand);
            player.sendMessage("§2Pomyślnie użyłeś reflinka gracza " + referrerName);

            return true;
        }

        player.sendMessage("§7Nieprawidłowe użycie, /reflink <nick gracza>");
        return true;
    }
}