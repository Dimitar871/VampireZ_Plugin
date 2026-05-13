package com.vampirez.discord;

import com.vampirez.VampireZPlugin;
import com.vampirez.config.PluginConfig;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.bukkit.Bukkit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Owns the JDA connection lifecycle and exposes safe, fire-and-forget operations
 * for sending presence updates / embeds. All public methods are no-ops when the
 * bot isn't ready, so callers never crash on a misconfigured Discord setup.
 */
public class DiscordBot {

    private static final Logger log = LoggerFactory.getLogger(DiscordBot.class);

    private final VampireZPlugin plugin;
    private volatile JDA jda;
    private final AtomicBoolean ready = new AtomicBoolean(false);
    /** Tracks if a permanent failure on the status channel has been logged so we don't spam. */
    private final AtomicBoolean statusChannelDisabled = new AtomicBoolean(false);

    public DiscordBot(VampireZPlugin plugin) {
        this.plugin = plugin;
    }

    private PluginConfig.DiscordSection cfg() { return plugin.getPluginConfig().discord; }

    /** Bootstrap JDA off the main thread so plugin enable isn't delayed by gateway connect. */
    public void startAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::startBlocking);
    }

    private void startBlocking() {
        String token = cfg().token;
        if (token == null || token.isBlank()) {
            log.warn("Discord enabled but token is empty — bot will not connect");
            return;
        }
        try {
            JDA jdaLocal = JDABuilder.createLight(token, EnumSet.noneOf(GatewayIntent.class))
                    .disableCache(EnumSet.allOf(CacheFlag.class))
                    .setActivity(Activity.playing("VampireZ"))
                    .build();
            jdaLocal.awaitReady();
            this.jda = jdaLocal;
            this.ready.set(true);
            log.info("Discord bot connected as {}", jdaLocal.getSelfUser().getAsTag());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.warn("Discord bot startup interrupted");
        } catch (Throwable t) {
            log.warn("Discord bot failed to connect: {}", t.getMessage());
        }
    }

    public void shutdown() {
        ready.set(false);
        JDA local = this.jda;
        if (local == null) return;
        try {
            local.shutdown();
            // Bounded wait so onDisable doesn't hang if Discord is unreachable.
            Thread t = new Thread(() -> {
                try { local.awaitShutdown(); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            });
            t.start();
            t.join(5_000);
            if (t.isAlive()) {
                local.shutdownNow();
            }
        } catch (Throwable e) {
            log.warn("Error during Discord bot shutdown: {}", e.getMessage());
        } finally {
            this.jda = null;
        }
    }

    public boolean isReady() { return ready.get(); }

    public void setPresence(String text) {
        if (!isReady()) return;
        try {
            jda.getPresence().setActivity(Activity.playing(text));
        } catch (Throwable t) {
            log.debug("Failed to set Discord presence: {}", t.getMessage());
        }
    }

    /**
     * Send a fresh status embed and persist its ID, OR edit the existing one.
     * Re-sends automatically if the previously-stored message has been deleted.
     */
    public void sendOrEditStatusEmbed(MessageEmbed embed) {
        if (!isReady() || statusChannelDisabled.get()) return;
        TextChannel channel = resolveStatusChannel();
        if (channel == null) return;

        String existingId = cfg().statusMessageId;
        if (existingId == null || existingId.isBlank()) {
            channel.sendMessageEmbeds(embed).queue(
                    msg -> persistStatusMessageId(msg.getId()),
                    err -> handleStatusError("send", err));
            return;
        }
        channel.editMessageEmbedsById(existingId, embed).queue(
                ok -> { /* updated in place */ },
                new ErrorHandler()
                        .handle(ErrorResponse.UNKNOWN_MESSAGE, e -> {
                            // Stored message gone — clear ID and try fresh send next tick.
                            persistStatusMessageId("");
                        })
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            if (statusChannelDisabled.compareAndSet(false, true)) {
                                log.warn("Discord bot lacks permission to edit messages in status channel — disabled for this session");
                            }
                        }));
    }

    /** One-shot announcement to the announce channel. */
    public void postAnnouncement(MessageEmbed embed) {
        if (!isReady()) return;
        TextChannel channel = resolveAnnounceChannel();
        if (channel == null) return;
        channel.sendMessageEmbeds(embed).queue(
                ok -> { /* posted */ },
                err -> log.debug("Failed to post Discord announcement: {}", err.getMessage()));
    }

    private TextChannel resolveStatusChannel() {
        String id = cfg().statusChannelId;
        if (id == null || id.isBlank()) return null;
        TextChannel channel = jda.getTextChannelById(id);
        if (channel == null && statusChannelDisabled.compareAndSet(false, true)) {
            log.warn("Discord status channel {} not found — check the ID + bot has access", id);
        }
        return channel;
    }

    private TextChannel resolveAnnounceChannel() {
        // Fall back to status channel if announce channel not separately configured.
        String id = cfg().announceChannelId;
        if (id == null || id.isBlank()) id = cfg().statusChannelId;
        if (id == null || id.isBlank()) return null;
        return jda.getTextChannelById(id);
    }

    /** ConfigLib YAML write must run on the main thread. */
    private void persistStatusMessageId(String id) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            cfg().statusMessageId = id;
            plugin.savePluginConfig();
        });
    }

    private void handleStatusError(String op, Throwable err) {
        if (statusChannelDisabled.compareAndSet(false, true)) {
            log.warn("Discord status embed {} failed — disabled for this session: {}", op, err.getMessage());
        }
    }
}
