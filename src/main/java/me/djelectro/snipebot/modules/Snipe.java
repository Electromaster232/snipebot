package me.djelectro.snipebot.modules;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookMessageBuilder;
import me.djelectro.snipebot.Database;
import me.djelectro.snipebot.annotations.SlashCommand;
import me.djelectro.snipebot.annotations.SlashCommandOption;
import me.djelectro.snipebot.types.SnipeGuild;
import me.djelectro.snipebot.types.SnipePlayer;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;

import javax.xml.crypto.Data;

public class Snipe extends Module {

    @SlashCommand(name = "snipe", description = "Snipe a player", options = {
            @SlashCommandOption(name="member", description = "The player you sniped", required = true, option = OptionType.MENTIONABLE),
            @SlashCommandOption(name = "attachment", option = OptionType.ATTACHMENT, description = "Attach your image", required = true),
            @SlashCommandOption(name="message", option = OptionType.STRING, description = "An optional message", required = false)
    })
    public void snipeCmd(SlashCommandInteractionEvent event) throws Exception {
        event.deferReply().queue();
        me.djelectro.snipebot.types.Snipe s = me.djelectro.snipebot.types.Snipe.createSnipe(event);
        if(s == null) {
            event.getHook().sendMessage("Snipe failed to process, code 3").queue();;
            return;
        }
        if(!s.process()){
            event.getHook().sendMessage("Snipe failed to process, code 1").queue();
            return;
        }

        long guildId = event.getGuild().getIdLong();
        if(guildId == 0){
            event.getHook().sendMessage("Snipe failed to process, code 2").queue();
            return;
        }

        try(SnipeGuild g = s.getGuild()){
            // Make sure that this guild is valid in DB

            WebhookClient client = WebhookClient.withUrl(g.getSnipeHook().getUrl());
            WebhookMessageBuilder builder = new WebhookMessageBuilder();
            String msg = s.getMessage();
            if(msg == null)
                msg = "";
            builder.setContent(STR."\{s.getSniped().getDiscordMember().getAsMention()}\n\{msg}\n\{s.getAttachUrl()}");
            builder.setUsername(event.getMember().getEffectiveName());
            builder.setAvatarUrl(event.getMember().getEffectiveAvatarUrl());
            client.send(builder.build()).join();
        };
        event.getHook().sendMessage("Snipe recorded.").queue();
//        String channelString = Database.getInstance().executeAndReturnData("SELECT channelid FROM guild_config WHERE guildid = ?", String.valueOf(guildId)).entrySet().iterator().next().getValue()[0];
//        long channelID = Long.parseLong(channelString);
//
//        event.getGuild().getTextChannelById(channelID).sendMessage(s.getSniper().getDiscordMember().getEffectiveName() + " sniped " + s.getSniped().getDiscordMember().getEffectiveName()).queue();
    }

    @SlashCommand(name = "getsnipes", description = "Show how many snipes a player has", options = {
            @SlashCommandOption(name="user", option = OptionType.MENTIONABLE, description = "The user to lookup", required = true)
    })
    public void showSnipes(SlashCommandInteractionEvent e){
        e.deferReply().queue();
        OptionMapping m = e.getOption("user");
        if(m == null) {
            e.reply("Error locating user").queue();
            return;
        }
        User u = m.getAsUser();
        SnipePlayer sp = new SnipePlayer(u);
        e.getHook().sendMessage(STR."Sniped \{sp.getSnipeCount(new SnipeGuild(e.getGuild()))} players in this guild.").queue();
    }

    @SlashCommand(name="snipeconfig", description = "Set the configuration for this guild", options = {
            @SlashCommandOption(name="snipechannel", option=OptionType.CHANNEL, description = "The channel to send snipes to", required = true)
    })
    public void guildConfig(SlashCommandInteractionEvent e){
        // Need to check if this guild exists in the DB
        e.deferReply().queue();
        SnipeGuild sg = new SnipeGuild(e.getGuild());
        OptionMapping c = e.getOption("snipechannel");
        if(c == null){
            e.getHook().sendMessage("Cannot locate channel").queue();
            return;
        }
        try {
            TextChannel tc = c.getAsChannel().asTextChannel();
            if(sg.updateSnipeChannel(tc))
                e.getHook().sendMessage("Guild configuration saved.").queue();
            else
                e.getHook().sendMessage("There was an error updating the guild config").queue();
        }catch (IllegalStateException ise){
            e.getHook().sendMessage("The channel specified is not a text channel!").queue();
        }
    }
}
