package com.lipasquide.cekilis;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Plugin(
        id = "cekilis",
        name = "Cekilis",
        version = "1.0.0",
        description = "Velocity cekilis (giveaway) plugin",
        authors = {"Lipasquide"}
)
public class CekilisPlugin implements SimpleCommand {

    private static final String DISCORD_LINK = "https://discord.gg/SENIN_LINKIN";
    private static final String LINE = "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━";
    private static final Pattern DURATION_PATTERN =
            Pattern.compile("^(\\d+)\\s*(saniye|sn|s|dakika|dk|saat|sa|gun|gün|g)$");

    private final ProxyServer server;
    private final Logger logger;

    private final Map<UUID, CreationState> creationStates = new ConcurrentHashMap<>();
    private final Set<UUID> participants = ConcurrentHashMap.newKeySet();

    private String giveawayName;
    private String giveawaySecret;
    private Instant giveawayEnd;
    private boolean giveawayActive;

    private UUID winnerUuid;
    private String winnerDisplayName;
    private boolean winnerAcknowledged;
    private ScheduledTask endTask;
    private ScheduledTask reminderTask;

    private enum Step { NAME, SECRET, DURATION }

    private static final class CreationState {
        Step step;
        String name;
        String secret;

        CreationState() {
            this.step = Step.NAME;
        }
    }

    @Inject
    public CekilisPlugin(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    // ───────────────────────────── Lifecycle ─────────────────────────────

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("cekilis")
                        .aliases("giveaway")
                        .build(),
                this
        );
        logger.info("Cekilis Plugin aktif!");
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        creationStates.remove(event.getPlayer().getUniqueId());
    }

    // ───────────────────────────── Command ─────────────────────────────

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!(source instanceof Player player)) {
            source.sendMessage(Component.text("Bu komut sadece oyuncular tarafindan kullanilabilir!", NamedTextColor.RED));
            return;
        }

        if (args.length == 0) {
            joinGiveaway(player);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "olustur" -> handleCreate(player);
            case "iptal" -> handleCancel(player);
            case "bilgi" -> handleInfo(player);
            case "yapiyorum" -> handleAcknowledge(player);
            default -> showHelp(player);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length > 1) return List.of();

        List<String> suggestions = new ArrayList<>();
        suggestions.add("bilgi");

        CommandSource source = invocation.source();
        if (source instanceof Player player) {
            if (player.hasPermission("cekilis.admin")) {
                suggestions.add("olustur");
                suggestions.add("iptal");
            }
            if (winnerUuid != null && player.getUniqueId().equals(winnerUuid)) {
                suggestions.add("yapiyorum");
            }
        }

        String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
        return suggestions.stream().filter(s -> s.startsWith(prefix)).toList();
    }

    // ───────────────────────────── Chat Input ─────────────────────────────

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        Player player = event.getPlayer();
        CreationState state = creationStates.get(player.getUniqueId());
        if (state == null) return;

        event.setResult(PlayerChatEvent.ChatResult.denied());
        String message = event.getMessage().trim();

        if (message.equalsIgnoreCase("iptal")) {
            creationStates.remove(player.getUniqueId());
            player.sendMessage(Component.text("Cekilis olusturma iptal edildi.", NamedTextColor.RED));
            return;
        }

        switch (state.step) {
            case NAME -> {
                state.name = message;
                state.step = Step.SECRET;
                player.sendMessage(Component.empty());
                player.sendMessage(colored("  Isim: ", NamedTextColor.GREEN)
                        .append(Component.text(message, NamedTextColor.WHITE)));
                player.sendMessage(Component.empty());
                player.sendMessage(colored("  Simdi gizli kodu yazin:", NamedTextColor.YELLOW));
                player.sendMessage(colored("  (Bu kod sadece sizde kalacak)", NamedTextColor.GRAY));
                player.sendMessage(Component.empty());
            }
            case SECRET -> {
                state.secret = message;
                state.step = Step.DURATION;
                player.sendMessage(Component.empty());
                player.sendMessage(colored("  Gizli Kod: ", NamedTextColor.GREEN)
                        .append(Component.text("********", NamedTextColor.WHITE)));
                player.sendMessage(Component.empty());
                player.sendMessage(colored("  Cekilis suresini yazin:", NamedTextColor.YELLOW));
                player.sendMessage(colored("  Ornekler: 1 dakika, 2 saat, 3 gun", NamedTextColor.GRAY));
                player.sendMessage(colored("  Kisaltmalar: 30s, 5dk, 1sa, 2g", NamedTextColor.GRAY));
                player.sendMessage(Component.empty());
            }
            case DURATION -> {
                Duration duration = parseDuration(message);
                if (duration == null || duration.isZero() || duration.isNegative()) {
                    player.sendMessage(Component.text("Gecersiz sure! Ornekler: 1 dakika, 2 saat, 3 gun", NamedTextColor.RED));
                    return;
                }
                creationStates.remove(player.getUniqueId());
                startGiveaway(player, state.name, state.secret, duration);
            }
        }
    }

    // ───────────────────────────── Handlers ─────────────────────────────

    private void handleCreate(Player player) {
        if (!player.hasPermission("cekilis.admin")) {
            player.sendMessage(Component.text("Bu komutu kullanma yetkin yok!", NamedTextColor.RED));
            return;
        }
        if (giveawayActive) {
            player.sendMessage(Component.text("Zaten aktif bir cekilis var! Once iptal edin: /cekilis iptal", NamedTextColor.RED));
            return;
        }
        if (winnerUuid != null && !winnerAcknowledged) {
            player.sendMessage(Component.text("Onceki cekilsin kazanani henuz onaylamadi!", NamedTextColor.RED));
            return;
        }

        creationStates.put(player.getUniqueId(), new CreationState());

        player.sendMessage(Component.empty());
        player.sendMessage(colored(LINE, NamedTextColor.GOLD));
        player.sendMessage(Component.text("  CEKILIS OLUSTURUCU", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        player.sendMessage(colored(LINE, NamedTextColor.GOLD));
        player.sendMessage(Component.empty());
        player.sendMessage(colored("  Cekilise bir isim verin:", NamedTextColor.YELLOW));
        player.sendMessage(colored("  (Chate yazip Enter'a basin)", NamedTextColor.GRAY));
        player.sendMessage(Component.empty());
    }

    private void handleCancel(Player player) {
        if (!player.hasPermission("cekilis.admin")) {
            player.sendMessage(Component.text("Bu komutu kullanma yetkin yok!", NamedTextColor.RED));
            return;
        }
        if (!giveawayActive && winnerUuid == null) {
            player.sendMessage(Component.text("Aktif bir cekilis yok!", NamedTextColor.RED));
            return;
        }

        String name = giveawayName;
        cancelTasks();
        resetState();

        Component msg = Component.text("\n")
                .append(colored(LINE, NamedTextColor.RED)).append(Component.newline())
                .append(Component.text("  CEKILIS IPTAL EDILDI", NamedTextColor.RED).decoration(TextDecoration.BOLD, true)).append(Component.newline())
                .append(colored("  Cekilis: " + name, NamedTextColor.GRAY)).append(Component.newline())
                .append(colored("  Iptal Eden: " + player.getUsername(), NamedTextColor.GRAY)).append(Component.newline())
                .append(colored(LINE, NamedTextColor.RED));

        broadcast(msg);
        logger.info("Cekilis iptal edildi: {} | Iptal Eden: {}", name, player.getUsername());
    }

    private void handleInfo(Player player) {
        if (!giveawayActive && winnerUuid == null) {
            player.sendMessage(Component.text("Aktif bir cekilis yok!", NamedTextColor.RED));
            return;
        }

        TextComponent.Builder builder = Component.text()
                .append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.text("  CEKILIS BILGISI", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.newline())
                .append(colored("  Isim: ", NamedTextColor.YELLOW))
                .append(Component.text(giveawayName + "\n", NamedTextColor.WHITE))
                .append(colored("  Durum: ", NamedTextColor.YELLOW));

        if (giveawayActive) {
            Duration remaining = Duration.between(Instant.now(), giveawayEnd);
            if (remaining.isNegative()) remaining = Duration.ZERO;
            builder.append(Component.text("Devam ediyor\n", NamedTextColor.GREEN))
                    .append(colored("  Kalan Sure: ", NamedTextColor.YELLOW))
                    .append(Component.text(formatDuration(remaining) + "\n", NamedTextColor.WHITE))
                    .append(colored("  Katilimci: ", NamedTextColor.YELLOW))
                    .append(Component.text(participants.size() + "\n", NamedTextColor.WHITE));

            if (participants.contains(player.getUniqueId())) {
                builder.append(colored("  Durumunuz: ", NamedTextColor.YELLOW))
                        .append(Component.text("Katildiniz\n", NamedTextColor.GREEN));
            } else {
                builder.append(colored("  Durumunuz: ", NamedTextColor.YELLOW))
                        .append(Component.text("Katilmadiniz\n", NamedTextColor.RED));
            }
        } else if (winnerUuid != null) {
            builder.append(Component.text("Sonuclandi\n", NamedTextColor.AQUA))
                    .append(colored("  Kazanan: ", NamedTextColor.YELLOW))
                    .append(Component.text(winnerDisplayName + "\n", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true))
                    .append(colored("  Onay: ", NamedTextColor.YELLOW))
                    .append(winnerAcknowledged
                            ? Component.text("Onaylandi\n", NamedTextColor.GREEN)
                            : Component.text("Bekleniyor...\n", NamedTextColor.RED));
        }

        builder.append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD));

        player.sendMessage(builder.build());
    }

    private void handleAcknowledge(Player player) {
        if (winnerUuid == null || !player.getUniqueId().equals(winnerUuid)) {
            player.sendMessage(Component.text("Bu komutu sadece cekilis kazanani kullanabilir!", NamedTextColor.RED));
            return;
        }
        if (winnerAcknowledged) {
            player.sendMessage(Component.text("Zaten onayladiniz!", NamedTextColor.YELLOW));
            return;
        }

        winnerAcknowledged = true;
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }

        player.sendMessage(Component.empty());
        player.sendMessage(colored(LINE, NamedTextColor.GREEN));
        player.sendMessage(Component.text("  Onaylandi!", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        player.sendMessage(colored("  Discord'da ticket acmayi unutmayin!", NamedTextColor.WHITE));
        player.sendMessage(colored(LINE, NamedTextColor.GREEN));
        player.sendMessage(Component.empty());

        Component adminMsg = colored("[Cekilis] ", NamedTextColor.GOLD)
                .append(Component.text(winnerDisplayName, NamedTextColor.AQUA))
                .append(Component.text(" odul bildirimini onayladi.", NamedTextColor.GREEN));

        for (Player p : server.getAllPlayers()) {
            if (p.hasPermission("cekilis.admin")) {
                p.sendMessage(adminMsg);
            }
        }

        logger.info("Cekilis kazanani {} onayladi.", winnerDisplayName);
    }

    // ───────────────────────────── Giveaway Logic ─────────────────────────────

    private void joinGiveaway(Player player) {
        if (!giveawayActive) {
            player.sendMessage(Component.text("Su anda aktif bir cekilis yok!", NamedTextColor.RED));
            return;
        }
        if (participants.contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Zaten bu cekilise katildiniz!", NamedTextColor.YELLOW));
            return;
        }

        participants.add(player.getUniqueId());

        player.sendMessage(Component.empty());
        player.sendMessage(colored(LINE, NamedTextColor.GREEN));
        player.sendMessage(Component.text("  Cekilise katildiniz!", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
        player.sendMessage(colored("  Cekilis: ", NamedTextColor.GRAY).append(Component.text(giveawayName, NamedTextColor.WHITE)));
        player.sendMessage(colored("  Toplam katilimci: ", NamedTextColor.GRAY).append(Component.text(String.valueOf(participants.size()), NamedTextColor.WHITE)));
        player.sendMessage(colored(LINE, NamedTextColor.GREEN));
        player.sendMessage(Component.empty());

        Component joinMsg = colored("  ", NamedTextColor.GOLD)
                .append(Component.text(player.getUsername(), NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true))
                .append(Component.text(" cekilise katildi! ", NamedTextColor.GOLD))
                .append(Component.text("[" + participants.size() + " katilimci]", NamedTextColor.GRAY));

        for (Player p : server.getAllPlayers()) {
            if (!p.getUniqueId().equals(player.getUniqueId())) {
                p.sendMessage(joinMsg);
            }
        }
    }

    private void startGiveaway(Player creator, String name, String secret, Duration duration) {
        giveawayName = name;
        giveawaySecret = secret;
        giveawayEnd = Instant.now().plus(duration);
        participants.clear();
        giveawayActive = true;
        winnerUuid = null;
        winnerDisplayName = null;
        winnerAcknowledged = false;

        String durationText = formatDuration(duration);

        Component announcement = Component.text("\n")
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.text("  YENI CEKILIS BASLADI!", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.newline())
                .append(colored("  Cekilis: ", NamedTextColor.YELLOW))
                .append(Component.text(name + "\n", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
                .append(colored("  Sure: ", NamedTextColor.YELLOW))
                .append(Component.text(durationText + "\n", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(colored("  Katilmak icin: ", NamedTextColor.GREEN))
                .append(Component.text("/cekilis", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/cekilis"))
                        .hoverEvent(HoverEvent.showText(Component.text("Tikla ve katil!", NamedTextColor.YELLOW))))
                .append(Component.newline()).append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD));

        broadcast(announcement);
        creator.sendMessage(Component.text("Cekilis basariyla olusturuldu! Gizli Kod: " + secret, NamedTextColor.GREEN));
        logger.info("Cekilis olusturuldu: {} | Sure: {} | Olusturan: {}", name, durationText, creator.getUsername());

        endTask = server.getScheduler()
                .buildTask(this, this::endGiveaway)
                .delay(duration.toMillis(), TimeUnit.MILLISECONDS)
                .schedule();
    }

    private void endGiveaway() {
        giveawayActive = false;
        endTask = null;

        if (participants.isEmpty()) {
            Component msg = Component.text("\n")
                    .append(colored(LINE, NamedTextColor.RED)).append(Component.newline())
                    .append(Component.text("  CEKILIS SONA ERDI", NamedTextColor.RED).decoration(TextDecoration.BOLD, true)).append(Component.newline())
                    .append(colored("  Hic katilimci olmadigi icin kazanan yok!", NamedTextColor.GRAY)).append(Component.newline())
                    .append(colored(LINE, NamedTextColor.RED));

            broadcast(msg);
            resetState();
            return;
        }

        List<UUID> list = new ArrayList<>(participants);
        winnerUuid = list.get(new Random().nextInt(list.size()));

        Optional<Player> winnerOpt = server.getPlayer(winnerUuid);
        winnerDisplayName = winnerOpt.map(Player::getUsername).orElse("Bilinmeyen");
        winnerAcknowledged = false;

        Component announcement = Component.text("\n")
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.text("  CEKILIS SONUCLANDI!", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.newline())
                .append(colored("  Cekilis: ", NamedTextColor.YELLOW))
                .append(Component.text(giveawayName + "\n", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
                .append(colored("  Kazanan: ", NamedTextColor.YELLOW))
                .append(Component.text(winnerDisplayName + "\n", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true))
                .append(colored("  Toplam Katilimci: ", NamedTextColor.YELLOW))
                .append(Component.text(participants.size() + "\n", NamedTextColor.WHITE))
                .append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD));

        broadcast(announcement);
        logger.info("Cekilis sonuclandi: {} | Kazanan: {} | Katilimci: {}", giveawayName, winnerDisplayName, participants.size());

        winnerOpt.ifPresent(this::sendWinnerReminder);

        reminderTask = server.getScheduler()
                .buildTask(this, () -> {
                    if (winnerAcknowledged) {
                        if (reminderTask != null) {
                            reminderTask.cancel();
                            reminderTask = null;
                        }
                        return;
                    }
                    server.getPlayer(winnerUuid).ifPresent(this::sendWinnerReminder);
                })
                .delay(10, TimeUnit.MINUTES)
                .repeat(10, TimeUnit.MINUTES)
                .schedule();
    }

    private void sendWinnerReminder(Player winner) {
        Component msg = Component.text("\n")
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.text("  TEBRIKLER! CEKILISI KAZANDINIZ!", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.newline())
                .append(colored("  Cekilis: ", NamedTextColor.YELLOW))
                .append(Component.text(giveawayName + "\n", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
                .append(Component.newline())
                .append(colored("  Odulunuzu almak icin:", NamedTextColor.GREEN)).append(Component.newline())
                .append(colored("  1. Discord sunucumuza gelin", NamedTextColor.WHITE)).append(Component.newline())
                .append(colored("  2. Ticket acin", NamedTextColor.WHITE)).append(Component.newline())
                .append(colored("  3. Zaten sunucudaysaniz da ticket acin!", NamedTextColor.WHITE)).append(Component.newline())
                .append(Component.newline())
                .append(colored("  Discord: ", NamedTextColor.AQUA))
                .append(Component.text(DISCORD_LINK, NamedTextColor.AQUA)
                        .decoration(TextDecoration.UNDERLINED, true)
                        .clickEvent(ClickEvent.openUrl(DISCORD_LINK))
                        .hoverEvent(HoverEvent.showText(Component.text("Discord'a git!", NamedTextColor.YELLOW))))
                .append(Component.newline()).append(Component.newline())
                .append(Component.text("  [YAPIYORUM]", NamedTextColor.GREEN)
                        .decoration(TextDecoration.BOLD, true)
                        .clickEvent(ClickEvent.runCommand("/cekilis yapiyorum"))
                        .hoverEvent(HoverEvent.showText(Component.text("Tiklayarak onaylayin!", NamedTextColor.YELLOW))))
                .append(colored(" <-- Tiklayarak onaylayin", NamedTextColor.GRAY))
                .append(Component.newline()).append(Component.newline())
                .append(colored("  Bu mesaji onaylayana kadar", NamedTextColor.RED)).append(Component.newline())
                .append(colored("  10 dakikada bir hatirlatma alacaksiniz!", NamedTextColor.RED)).append(Component.newline())
                .append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD));

        winner.sendMessage(msg);
    }

    // ───────────────────────────── Help ─────────────────────────────

    private void showHelp(Player player) {
        TextComponent.Builder builder = Component.text()
                .append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.text("  CEKILIS KOMUTLARI", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true)).append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD)).append(Component.newline())
                .append(Component.newline())
                .append(colored("  /cekilis ", NamedTextColor.AQUA))
                .append(colored("- Cekilise katil\n", NamedTextColor.GRAY))
                .append(colored("  /cekilis bilgi ", NamedTextColor.AQUA))
                .append(colored("- Cekilis bilgisi\n", NamedTextColor.GRAY))
                .append(colored("  /cekilis yapiyorum ", NamedTextColor.AQUA))
                .append(colored("- Kazanan onay\n", NamedTextColor.GRAY));

        if (player.hasPermission("cekilis.admin")) {
            builder.append(colored("  /cekilis olustur ", NamedTextColor.AQUA))
                    .append(colored("- Cekilis olustur\n", NamedTextColor.GRAY))
                    .append(colored("  /cekilis iptal ", NamedTextColor.AQUA))
                    .append(colored("- Cekilisi iptal et\n", NamedTextColor.GRAY));
        }

        builder.append(Component.newline())
                .append(colored(LINE, NamedTextColor.GOLD));

        player.sendMessage(builder.build());
    }

    // ───────────────────────────── Utilities ─────────────────────────────

    private Duration parseDuration(String input) {
        input = input.toLowerCase(Locale.ROOT).trim();
        Matcher matcher = DURATION_PATTERN.matcher(input);
        if (!matcher.matches()) return null;

        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        return switch (unit) {
            case "saniye", "sn", "s" -> Duration.ofSeconds(amount);
            case "dakika", "dk" -> Duration.ofMinutes(amount);
            case "saat", "sa" -> Duration.ofHours(amount);
            case "gün", "gun", "g" -> Duration.ofDays(amount);
            default -> null;
        };
    }

    private String formatDuration(Duration duration) {
        long totalSeconds = duration.getSeconds();
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" gun ");
        if (hours > 0) sb.append(hours).append(" saat ");
        if (minutes > 0) sb.append(minutes).append(" dakika ");
        if (seconds > 0 && days == 0) sb.append(seconds).append(" saniye");
        String result = sb.toString().trim();
        return result.isEmpty() ? "0 saniye" : result;
    }

    private void broadcast(Component message) {
        for (Player p : server.getAllPlayers()) {
            p.sendMessage(message);
        }
    }

    private Component colored(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    private void cancelTasks() {
        if (endTask != null) {
            endTask.cancel();
            endTask = null;
        }
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
    }

    private void resetState() {
        giveawayName = null;
        giveawaySecret = null;
        giveawayEnd = null;
        participants.clear();
        giveawayActive = false;
        winnerUuid = null;
        winnerDisplayName = null;
        winnerAcknowledged = false;
    }
}
