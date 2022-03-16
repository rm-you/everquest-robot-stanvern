package com.edmazur.eqrs.game.listener;

import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.javacord.api.entity.message.Message;

import com.edmazur.eqrs.Logger;
import com.edmazur.eqrs.discord.Discord;
import com.edmazur.eqrs.discord.DiscordChannel;
import com.edmazur.eqrs.game.GameLogEvent;

public class MotdListener implements GameLogListener {

  private static final Logger LOGGER = new Logger();

  private static final Pattern GAME_MOTD_PATTERN =
      Pattern.compile("GUILD MOTD: .+ - .+");
  private static final Pattern DISCORD_MOTD_PATTERN =
      Pattern.compile("`" + GAME_MOTD_PATTERN.pattern() + "`");

  private static final DiscordChannel MOTD_CHANNEL =
      DiscordChannel.RAIDER_GMOTD;

  private final Discord discord;

  public MotdListener(Discord discord) {
    this.discord = discord;
  }

  @Override
  public String getConfig() {
    return "";
  }

  @Override
  public void onGameLogEvent(GameLogEvent gameLogEvent) {
    Matcher matcher = GAME_MOTD_PATTERN.matcher(gameLogEvent.getText());
    if (matcher.matches()) {
      // Avoid repeating the same MotD when you manually /get or login.
      Optional<String> maybeCurrentMotd = getCurrentMotd();
      if (maybeCurrentMotd.isEmpty()) {
        LOGGER.log("Could not read current MotD from Discord. This should not happen.");
      } else {
        if (maybeCurrentMotd.get().equals(gameLogEvent.getText())) {
          return;
        }
      }

      // TODO: Maybe avoid sending multiple MotDs in quick succession (e.g. from
      // fixing typos) by waiting a bit and only sending latest MotD.
      discord.sendMessage(MOTD_CHANNEL, "`" + gameLogEvent.getText() + "`");
    }
  }

  /**
   * Gets the current MotD as reported in the Discord channel.
   */
  public Optional<String> getCurrentMotd() {
    Predicate<Message> predicate = new Predicate<Message>() {
      @Override
      public boolean test(Message message) {
        return message.getAuthor().isYourself() &&
            DISCORD_MOTD_PATTERN.matcher(message.getContent()).matches();
      }
    };

    Optional<String> maybeMotd =
        discord.getLastMessageMatchingPredicated(MOTD_CHANNEL, predicate);
    if (maybeMotd.isEmpty()) {
      return maybeMotd;
    } else {
      String motd = maybeMotd.get();
      return Optional.of(motd.substring(1, motd.length() - 1));
    }
  }

}