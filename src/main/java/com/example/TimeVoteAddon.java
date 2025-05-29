package com.example.timevote;

import com.andrei1058.bedwars.api.BedWars;
import com.andrei1058.bedwars.api.arena.GameState;
import com.andrei1058.bedwars.api.arena.IArena;
import com.andrei1058.bedwars.api.events.gameplay.GameStateChangeEvent;
import com.andrei1058.bedwars.api.events.player.PlayerLeaveArenaEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class TimeVoteAddon extends JavaPlugin implements Listener {

    private final Map<UUID, Integer> playerVotes = new HashMap<>(); // UUID игрока -> время
    private final Map<IArena, List<Integer>> arenaVotes = new HashMap<>(); // Арена -> варианты времени
    private final Map<IArena, Map<Integer, Integer>> voteCounts = new HashMap<>(); // Арена -> (время -> количество голосов)
    private final Map<Integer, String> timeOptions = new HashMap<>(); // Время -> строка отображения
    private BedWars bedWarsAPI;

    private final String VOTE_INVENTORY_TITLE = ChatColor.DARK_GREEN + "Голосование за время суток";

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("BedWars1058") == null) {
            getLogger().severe("BedWars1058 не найден! Отключение TimeVoteAddon.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        RegisteredServiceProvider<BedWars> rsp = Bukkit.getServicesManager().getRegistration(BedWars.class);
        if (rsp == null) {
            getLogger().severe("Не удалось получить API BedWars1058. Отключение.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        bedWarsAPI = rsp.getProvider();

        timeOptions.put(0, "Утро (6:00)");
        timeOptions.put(6000, "День (12:00)");
        timeOptions.put(12000, "Вечер (18:00)");
        timeOptions.put(18000, "Ночь (0:00)");

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TimeVoteAddon включен!");
    }

    private boolean canVote(GameState state) {
        return state == GameState.waiting || state == GameState.starting;
    }

    @EventHandler
    public void onGameStart(GameStateChangeEvent e) {
        IArena arena = e.getArena();
        GameState newState = e.getNewState();

        if (arena == null) return;

        if (newState == GameState.waiting || newState == GameState.starting) {  // waiting или starting
            if (!arenaVotes.containsKey(arena)) {
                arenaVotes.put(arena, new ArrayList<>(Arrays.asList(0, 6000, 12000, 18000)));
                voteCounts.put(arena, new HashMap<>());

                broadcastToArena(arena, ChatColor.GOLD + "Голосование за время суток началось! В вашем инвентаре появились часы.");

                // Выдаём часы всем игрокам в арене, если их нет
                for (Player player : arena.getPlayers()) {
                    if (!player.getInventory().contains(Material.CLOCK)) {
                        player.getInventory().addItem(createVoteClock());
                        player.updateInventory();
                    }
                }
            }
        } else if (newState == GameState.playing) {
            applyVotedTime(arena);
        }
    }

    private void broadcastToArena(IArena arena, String message) {
        for (Player player : arena.getPlayers()) {
            player.sendMessage(message);
        }
    }

    private ItemStack createVoteClock() {
        ItemStack clock = new ItemStack(Material.CLOCK);
        ItemMeta meta = clock.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Голосование за время суток");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.YELLOW + "Кликните, чтобы выбрать время суток");
            clock.setItemMeta(meta);
        }
        return clock;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getItem() == null) return;
        if (e.getItem().getType() != Material.CLOCK) return;

        ItemMeta meta = e.getItem().getItemMeta();
        if (meta == null || !meta.hasDisplayName()) return;
        if (!meta.getDisplayName().equals(ChatColor.GOLD + "Голосование за время суток")) return;

        Player player = e.getPlayer();

        IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);
        if (arena == null) {
            player.sendMessage(ChatColor.RED + "Вы должны быть в лобби арены для голосования.");
            return;
        }

        if (!canVote(arena.getStatus())) {
            player.sendMessage(ChatColor.RED + "Голосование доступно только в лобби перед игрой!");
            return;
        }

        openVoteMenu(player, arena);
        e.setCancelled(true);
    }

    private void openVoteMenu(Player player, IArena arena) {
        Inventory inv = Bukkit.createInventory(null, 9, VOTE_INVENTORY_TITLE);

        List<Integer> options = arenaVotes.get(arena);
        if (options == null) {
            player.sendMessage(ChatColor.RED + "Голосование еще не началось.");
            return;
        }

        for (int i = 0; i < options.size(); i++) {
            int time = options.get(i);
            ItemStack item = new ItemStack(Material.CLOCK);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.GREEN + timeOptions.get(time));
                item.setItemMeta(meta);
            }
            inv.setItem(i, item);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        HumanEntity clicker = e.getWhoClicked();

        if (!(clicker instanceof Player)) return;

        Player player = (Player) clicker;

        if (!e.getView().getTitle().equals(VOTE_INVENTORY_TITLE)) return;

        e.setCancelled(true); // запретить брать предметы из меню

        IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);
        if (arena == null || !canVote(arena.getStatus())) {
            player.closeInventory();
            player.sendMessage(ChatColor.RED + "Голосование доступно только в лобби перед игрой!");
            return;
        }

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() != Material.CLOCK || !clicked.hasItemMeta()) {
            return;
        }

        String displayName = clicked.getItemMeta().getDisplayName();
        if (displayName == null) return;

        // Найдем время по названию
        Integer chosenTime = null;
        for (Map.Entry<Integer, String> entry : timeOptions.entrySet()) {
            if ((ChatColor.GREEN + entry.getValue()).equals(displayName)) {
                chosenTime = entry.getKey();
                break;
            }
        }

        if (chosenTime == null) {
            player.sendMessage(ChatColor.RED + "Неизвестный вариант голосования!");
            return;
        }

        // Обновляем голос игрока
        Integer oldVote = playerVotes.get(player.getUniqueId());

        if (oldVote != null) {
            // Уменьшаем счетчик старого голоса
            Map<Integer, Integer> counts = voteCounts.get(arena);
            if (counts != null) {
                counts.merge(oldVote, -1, Integer::sum);
                if (counts.get(oldVote) <= 0) {
                    counts.remove(oldVote);
                }
            }
        }

        // Сохраняем новый голос
        playerVotes.put(player.getUniqueId(), chosenTime);
        voteCounts.putIfAbsent(arena, new HashMap<>());
        voteCounts.get(arena).merge(chosenTime, 1, Integer::sum);

        player.sendMessage(ChatColor.GREEN + "Ваш голос за " + timeOptions.get(chosenTime) + " принят!");
        player.closeInventory();
    }

    private void applyVotedTime(IArena arena) {
        Map<Integer, Integer> counts = voteCounts.get(arena);

        World world = arena.getWorld();
        if (counts == null || counts.isEmpty() || world == null) {
            if (world != null) {
                world.setTime(6000);
                broadcastToArena(arena, ChatColor.GREEN + "Время по умолчанию: День (12:00)");
            }
            cleanupArenaData(arena);
            return;
        }

        int winningTime = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(6000);

        world.setTime(winningTime);
        broadcastToArena(arena, ChatColor.GREEN + "Установлено время: " + timeOptions.get(winningTime));
        cleanupArenaData(arena);
    }

    private void cleanupArenaData(IArena arena) {
        arenaVotes.remove(arena);
        voteCounts.remove(arena);
        for (Player player : arena.getPlayers()) {
            playerVotes.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerLeaveArenaEvent e) {
        playerVotes.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        playerVotes.remove(e.getPlayer().getUniqueId());
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Команда доступна только игрокам.");
            return true;
        }

        Player player = (Player) sender;

        if (bedWarsAPI == null) {
            player.sendMessage(ChatColor.RED + "API BedWars недоступно.");
            return true;
        }

        IArena arena = bedWarsAPI.getArenaUtil().getArenaByPlayer(player);
        if (arena == null || !canVote(arena.getStatus())) {
            player.sendMessage(ChatColor.RED + "Голосование доступно только в лобби перед игрой.");
            return true;
        }

        openVoteMenu(player, arena);
        return true;
    }
}
