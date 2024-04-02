package me.aixo.axreflink.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class DatabaseManager {

    private Plugin plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(Plugin plugin) {
        this.plugin = plugin;
        setupDataSource();
        createTable();
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + plugin.getConfig().getString("database.host") + ":" + plugin.getConfig().getInt("database.port") + "/" + plugin.getConfig().getString("database.name") + "?useSSL=true");
        config.setUsername(plugin.getConfig().getString("database.username"));
        config.setPassword(plugin.getConfig().getString("database.password"));

        try {
            dataSource = new HikariDataSource(config);
            if (dataSource != null && !dataSource.isClosed()) {
                plugin.getLogger().info("Połączono z bazą danych!");
            } else {
                plugin.getLogger().severe("Nie udało się nawiązać połączenia z bazą danych!");
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Błąd podczas próby połączenia z bazą danych: " + e.getMessage());
        }
    }

    private void createTable() {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS referrals (" +
                            "player_name VARCHAR(16) PRIMARY KEY," +
                            "referrals_count INT," +
                            "has_used_referral_link BOOLEAN," +
                            "referrer_name VARCHAR(16)," +
                            "claimed_rewards VARCHAR(255)" +
                            ");"
            );
            statement.executeUpdate();
            plugin.getLogger().info("Table created");
        } catch (SQLException e) {
            e.printStackTrace();
            plugin.getLogger().severe("Could not create table");
        }
    }


    public void addPlayer(String playerName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO referrals (player_name, referrals_count, has_used_referral_link, claimed_rewards) VALUES (?, 0, FALSE, '');"
            );
            statement.setString(1, playerName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public int getReferralsCount(String playerName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT referrals_count FROM referrals WHERE player_name = ?;"
            );
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("referrals_count");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public boolean hasUsedReferralLink(String playerName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT has_used_referral_link FROM referrals WHERE player_name = ?;"
            );
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getBoolean("has_used_referral_link");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean isValidReferralLink(String referrerName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM referrals WHERE player_name = ?;"
            );
            statement.setString(1, referrerName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void useReferralLink(String referrerName, String playerName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE referrals SET referrals_count = referrals_count + 1 WHERE player_name = ?;"
            );
            statement.setString(1, referrerName);
            statement.executeUpdate();

            statement = connection.prepareStatement(
                    "UPDATE referrals SET has_used_referral_link = TRUE, referrer_name = ? WHERE player_name = ?;"
            );
            statement.setString(1, referrerName);
            statement.setString(2, playerName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Integer> getClaimedRewards(String playerName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT claimed_rewards FROM referrals WHERE player_name = ?;"
            );
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                String claimedRewards = resultSet.getString("claimed_rewards");
                if (claimedRewards != null && !claimedRewards.isEmpty()) {
                    return Arrays.stream(claimedRewards.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    public void claimReward(String playerName, int slot) {
        try (Connection connection = dataSource.getConnection()) {
            List<Integer> claimedRewards = getClaimedRewards(playerName);
            claimedRewards.add(slot);
            String claimedRewardsString = claimedRewards.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(","));

            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE referrals SET claimed_rewards = ? WHERE player_name = ?;"
            );
            statement.setString(1, claimedRewardsString);
            statement.setString(2, playerName);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Map.Entry<String, Integer>> getTopReferrals() {
        List<Map.Entry<String, Integer>> topReferrals = new ArrayList<>();
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT player_name, referrals_count FROM referrals ORDER BY referrals_count DESC LIMIT 5;"
            );
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                topReferrals.add(new AbstractMap.SimpleEntry<>(resultSet.getString("player_name"), resultSet.getInt("referrals_count")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return topReferrals;
    }

    public int getPlayerRank(String playerName) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT COUNT(*) FROM referrals WHERE referrals_count > (SELECT referrals_count FROM referrals WHERE player_name = ?);"
            );
            statement.setString(1, playerName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt(1) + 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public boolean isAccountSyncedWithDiscord(UUID playerUUID) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "SELECT * FROM discord_sync WHERE uuid = ?;"
            );
            statement.setString(1, playerUUID.toString());
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }




    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection closed");
        }
    }
}
