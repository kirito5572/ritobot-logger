package BOT.Listener;

import BOT.App;
import BOT.Objects.SQL;
import BOT.Objects.config;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.channel.category.*;
import net.dv8tion.jda.api.events.channel.category.update.*;
import net.dv8tion.jda.api.events.channel.text.*;
import net.dv8tion.jda.api.events.channel.text.update.*;
import net.dv8tion.jda.api.events.channel.voice.*;
import net.dv8tion.jda.api.events.channel.voice.update.*;
import net.dv8tion.jda.api.events.emote.*;
import net.dv8tion.jda.api.events.guild.*;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildMuteEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.*;
import net.dv8tion.jda.api.events.role.*;
import net.dv8tion.jda.api.events.role.update.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class loggerListener extends ListenerAdapter {

    @Override
    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        Message message = event.getMessage();
        Guild guild = event.getGuild();
        String messageId = message.getId();
        String messageContent = message.getContentDisplay();
        String authorId = event.getAuthor().getId();
        for(String guild1 : config.getTextLoggingEnable()) {
            if(guild.getId().equals(guild1)) {
                if (event.getAuthor().isBot()) {
                    return;
                }
                if (message.isWebhookMessage() || message.isTTS()) {
                    return;
                }
                while(messageContent.contains("'")) {
                    messageContent = messageContent.replaceFirst("'", "");
                }
                while(messageContent.contains("\"")) {
                    messageContent = messageContent.replaceFirst("\"", "");
                }
                while(messageContent.contains("\\")) {
                    messageContent = messageContent.replace("\\", "");
                }
                if (messageContent.contains("\\'")) {
                    messageContent = messageContent.replace("\\'", "\\\\'");
                }
                final boolean[] temp = {SQL.loggingMessageUpLoad(guild.getId(), messageId, messageContent, authorId)};
                String finalMessageContent = messageContent;
                new Thread(() -> {
                    while (!temp[0]) {
                        temp[0] = SQL.loggingMessageUpLoad(guild.getId(), messageId, finalMessageContent, authorId);
                        try {
                            System.out.println(temp[0]);
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }



    @Override
    public void onGuildMessageUpdate(@Nonnull GuildMessageUpdateEvent event) {
        Message message = event.getMessage();
        Guild guild = event.getGuild();
        String messageId = message.getId();
        String messageContent = message.getContentDisplay();
        String authorId = event.getAuthor().getId();
        for(String guild1 : config.getTextLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                if (message.isWebhookMessage() || message.isTTS()) {
                    return;
                }
                if (event.getAuthor().isBot()) {
                    return;
                }
                while (messageContent.contains("\\")) {
                    messageContent = messageContent.replace("\\", "");
                }
                while (messageContent.contains("'")) {
                    messageContent = messageContent.replaceFirst("'", "");
                }
                while (messageContent.contains("\"")) {
                    messageContent = messageContent.replaceFirst("\"", "");
                }
                String finalMessageContent = messageContent;
                String[] data = SQL.loggingMessageDownLoad(guild.getId(), messageId);
                if(data[0] == null) {
                    return;
                }
                if (data[0].length() < 2) {
                    return;
                }
                final boolean[] temp = {SQL.loggingMessageUpdate(guild.getId(), messageId, messageContent)};
                new Thread(() -> {
                    while (!temp[0]) {
                        temp[0] = SQL.loggingMessageUpdate(guild.getId(), messageId, finalMessageContent);
                        try {
                            System.out.println(temp[0]);
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
                Member member = event.getGuild().getMemberById(authorId);
                assert member != null;
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");

                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("수정된 메세지")
                        .setColor(Color.ORANGE)
                        .setDescription("메세지 수정: " + event.getChannel().getAsMention() + "\n" +
                                "[메세지 이동](" + message.getJumpUrl() + ")")
                        .addField("수정전 내용", data[0], false)
                        .addField("수정후 내용", messageContent, false)
                        .addField("수정 시간", time2, false)
                        .setFooter((member.getEffectiveName() + "(" + member.getEffectiveName() + ")"), member.getUser().getAvatarUrl());
                messageLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onMessageBulkDelete(@Nonnull MessageBulkDeleteEvent event) {
        List<String> ids = event.getMessageIds();
        Guild guild = event.getGuild();
        StringBuilder stringBuilder = new StringBuilder();
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");

        Date time = new Date();

        String time2 = format2.format(time);
        for (String messageId : ids) {
            String[] data = SQL.loggingMessageDownLoad(guild.getId(), messageId);
            if (data[0].length() < 2) {
                return;
            } else {
                stringBuilder.append(data[0]).append("\n");
            }
        }

    }


    @Override
    public void onGuildMessageDelete(@Nonnull GuildMessageDeleteEvent event) {
        String messageId = event.getMessageId();
        Guild guild = event.getGuild();
        for(String guild1 : config.getTextLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                String[] data = SQL.loggingMessageDownLoad(guild.getId(), messageId);
                try {
                    if (data[0].length() < 2) {
                        return;
                    }
                } catch (NullPointerException e) {
                    return;
                }
                Member member = event.getGuild().getMemberById(data[1]);
                assert member != null;
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");

                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("삭제된 메세지")
                        .setColor(Color.RED)
                        .setDescription("메세지 삭제: " + event.getChannel().getAsMention())
                        .addField("내용", data[0], false)
                        .addField("메세지 ID", messageId, false)
                        .addField("삭제 시간", time2, false)
                        .setFooter((member.getEffectiveName() + "(" + member.getEffectiveName() + ")"), member.getUser().getAvatarUrl());
                messageLoggingSend(builder, guild);
            }
        }
    }
    private void messageLoggingSend(@NotNull EmbedBuilder builder, @NotNull Guild guild) {
        List<TextChannel> channels = guild.getTextChannelsByName("채팅-로그", false);
        if(!channels.isEmpty()) {
            channels.get(0).sendMessage(builder.build()).queue();
        } else {
            List<TextChannel> channels1 = guild.getTextChannelsByName("chat-logs", false);
            if(!channels.isEmpty()) {
                channels1.get(0).sendMessage(builder.build()).queue();
            }
        }

    }

    private void channelLoggingSend(@NotNull EmbedBuilder builder, @NotNull Guild guild) {
        List<TextChannel> channels = guild.getTextChannelsByName("채널-로그", false);
        if(!channels.isEmpty()) {
            channels.get(0).sendMessage(builder.build()).queue();
        } else {
            List<TextChannel> channels1 = guild.getTextChannelsByName("channel-logs", false);
            if(!channels.isEmpty()) {
                channels1.get(0).sendMessage(builder.build()).queue();
            }
        }

    }

    private void memberLoggingSend(@NotNull EmbedBuilder builder, @NotNull Guild guild) {
        List<TextChannel> channels = guild.getTextChannelsByName("멤버-로그", false);
        if(!channels.isEmpty()) {
            channels.get(0).sendMessage(builder.build()).queue();
        } else {
            List<TextChannel> channels1 = guild.getTextChannelsByName("member-logs", false);
            if(!channels.isEmpty()) {
                channels1.get(0).sendMessage(builder.build()).queue();
            }
        }

    }

    @Override
    public void onReconnect(@Nonnull ReconnectedEvent event) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            SQL.setLoggingConnection(DriverManager.getConnection(SQL.getUrl(), SQL.getUser(), SQL.getPassword()));
            SQL.setConnection(DriverManager.getConnection(SQL.getUrl(), SQL.getUser(), SQL.getPassword()));
        } catch (@NotNull SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
        String guildId = event.getGuild().getId();
        try {
            Statement statement = SQL.getConnection().createStatement();
            statement.executeUpdate("INSERT INTO ritobot_config.color_command_guild VALUES (" + guildId + ", 1)");
            statement.executeUpdate("INSERT INTO ritobot_config.filter_guild VALUES (" + guildId + ", 0)");
            statement.executeUpdate("INSERT INTO ritobot_config.kill_filter_guild VALUES (" + guildId + ", 0)");
            statement.executeUpdate("INSERT INTO ritobot_config.lewdneko_command VALUES (" + guildId + ", 0)");
            statement.executeUpdate("INSERT INTO ritobot_config.link_filter_guild VALUES (" + guildId + ", 0)");
            statement.executeUpdate("INSERT INTO ritobot_config.logging_enable VALUES (" + guildId + ", 1, 1, 1)");
            statement.executeUpdate("INSERT INTO ritobot_config.notice VALUES (" + guildId + ", 0, '0')");
            statement.executeUpdate("INSERT INTO ritobot_config.filter_output_channel VALUES (" + guildId + ", 0, '1')");
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Objects.requireNonNull(event.getGuild().getDefaultChannel()).sendMessage("&설정 명령어로 봇 설정 해두시기 바랍니다.\n 설정을 하지 않아 발생한 문제는 제작자가 책임지지 않습니다.").queue();
    }

    @Override
    public void onTextChannelUpdateName(@Nonnull TextChannelUpdateNameEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("텍스트 채널 이름 변경")
                        .setColor(Color.GREEN)
                        .addField("이전 이름", event.getOldName(), false)
                        .addField("변경된 이름", event.getNewName(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onTextChannelUpdateTopic(@Nonnull TextChannelUpdateTopicEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                String topic = event.getOldTopic();
                if(topic == null) {
                    topic = "없음";
                }
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("텍스트 채널 토픽 변경")
                        .setColor(Color.GREEN)
                        .addField("채널명", event.getChannel().getName(), false)
                        .addField("이전 토픽", topic, false)
                        .addField("변경된 토픽", event.getNewTopic(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onTextChannelUpdatePermissions(@Nonnull TextChannelUpdatePermissionsEvent event) {
        super.onTextChannelUpdatePermissions(event);
        /*
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);

                StringBuilder builder1 = new StringBuilder();
                StringBuilder builder2 = new StringBuilder();
                for(int i = 0; i < event.getChangedRoles().size(); i++) {
                    builder1.append(event.getChangedRoles().get(i).getName()).append("\n");
                    builder1.append(event.getChangedPermissionHolders().get(i).getPermissions(event.getChannel()).toString()).append("\n\n");
                }
                for(int i = 0; i < event.getChangedMembers().size(); i++) {
                    builder2.append(event.getChangedMembers().get(i).getEffectiveName()).append("\n");
                    builder2.append(event.getChangedPermissionHolders().get(i).getPermissions(event.getChannel()).toString()).append("\n\n");
                }

                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("텍스트 채널 권한 변경")
                        .setColor(Color.GREEN)
                        .addField("채널명", event.getChannel().getName(), false)
                        .addField("변경된 권한(역할)", builder1.toString(), false)
                        .addField("변경된 권한(멤버)", builder2.toString(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }

         */
    }

    @Override
    public void onTextChannelUpdateNSFW(@Nonnull TextChannelUpdateNSFWEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder;
                if(event.getOldNSFW()) {
                    builder = EmbedUtils.defaultEmbed()
                            .setTitle("후방 주의 채널 해제")
                            .setColor(Color.GREEN)
                            .addField("채널명", event.getChannel().getAsMention(), false)
                            .addField("변경 시간", time2, false);
                } else {
                    builder = EmbedUtils.defaultEmbed()
                            .setTitle("후방 주의 채널 지정")
                            .setColor(Color.RED)
                            .addField("채널명", event.getChannel().getAsMention(), false)
                            .addField("변경 시간", time2, false);
                }
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onTextChannelUpdateSlowmode(@Nonnull TextChannelUpdateSlowmodeEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                            .setTitle("텍스트 채널 슬로우 모드 지정")
                            .setColor(Color.YELLOW)
                            .addField("채널명", event.getChannel().getAsMention(), false)
                            .addField("이전 슬로우 모드 시간", String.valueOf(event.getOldValue()), false)
                            .addField("현재 슬로우 모드 시간", String.valueOf(event.getNewSlowmode()), false)
                            .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onTextChannelCreate(@Nonnull TextChannelCreateEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("텍스트 채널 생성")
                        .setColor(Color.GREEN)
                        .addField("채널명", event.getChannel().getAsMention(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onTextChannelDelete(@Nonnull TextChannelDeleteEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("텍스트 채널 삭제")
                        .setColor(Color.RED)
                        .addField("채널명", event.getChannel().getName(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onVoiceChannelCreate(@Nonnull VoiceChannelCreateEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("보이스 채널 생성")
                        .setColor(Color.GREEN)
                        .addField("채널명", event.getChannel().getName(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onVoiceChannelDelete(@Nonnull VoiceChannelDeleteEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("보이스 채널 삭제")
                        .setColor(Color.RED)
                        .addField("채널명", event.getChannel().getName(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onVoiceChannelUpdateName(@Nonnull VoiceChannelUpdateNameEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("보이스 채널 이름 변경")
                        .setColor(Color.YELLOW)
                        .addField("이전 이름", event.getOldName(), false)
                        .addField("변경된 이름", event.getNewName(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onVoiceChannelUpdateUserLimit(@Nonnull VoiceChannelUpdateUserLimitEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("보이스 채널 유저 제한 수 변경")
                        .setColor(Color.YELLOW)
                        .addField("채널명", event.getChannel().getName(), false)
                        .addField("이전 제한 수", String.valueOf(event.getOldUserLimit()), false)
                        .addField("변경 제한 수", String.valueOf(event.getNewUserLimit()), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onVoiceChannelUpdateBitrate(@Nonnull VoiceChannelUpdateBitrateEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("보이스 채널 비트레이트 변경")
                        .setColor(Color.YELLOW)
                        .addField("채널명", event.getChannel().getName(), false)
                        .addField("이전 비트레이트", String.valueOf(event.getOldBitrate()), false)
                        .addField("변경 비트레이트", String.valueOf(event.getNewBitrate()), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onVoiceChannelUpdatePermissions(@Nonnull VoiceChannelUpdatePermissionsEvent event) {
        super.onVoiceChannelUpdatePermissions(event);
    }

    @Override
    public void onCategoryDelete(@Nonnull CategoryDeleteEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("카테고리 삭제")
                        .setColor(Color.RED)
                        .addField("카테고리명", event.getCategory().getName(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onCategoryUpdateName(@Nonnull CategoryUpdateNameEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("카테고리 이름 변경")
                        .setColor(Color.ORANGE)
                        .addField("이전 이름", event.getOldName(), false)
                        .addField("변경된 이름", event.getNewName(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onCategoryUpdatePermissions(@Nonnull CategoryUpdatePermissionsEvent event) {
        super.onCategoryUpdatePermissions(event);
    }

    @Override
    public void onCategoryCreate(@Nonnull CategoryCreateEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("카테고리 생성")
                        .setColor(Color.GREEN)
                        .addField("카테고리명", event.getCategory().getName(), false)
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onGuildMemberJoin(@Nonnull GuildMemberJoinEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("유저 입장")
                        .setDescription(event.getMember().getAsMention() + "유저가 서버에 들어왔습니다.")
                        .setColor(Color.GREEN)
                        .addField("유저명", event.getMember().getEffectiveName() + "(" + event.getMember().getUser().getAsTag() + ") ", false)
                        .addField("유저 가입일", event.getMember().getTimeCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())), false)
                        .addField("입장 시간", time2, false);
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onGuildMemberLeave(@Nonnull GuildMemberLeaveEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                StringBuilder stringBuilder = new StringBuilder();

                for(Role role : event.getMember().getRoles()) {
                    stringBuilder.append(role.getName()).append("\n");
                }

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("유저 퇴장")
                        .setDescription(event.getMember().getAsMention() + "유저가 서버에서 나갔습니다.")
                        .setColor(Color.RED)
                        .addField("유저명", event.getMember().getEffectiveName() + "(" + event.getMember().getUser().getAsTag() + ") ", false)
                        .addField("유저 가입일", event.getMember().getTimeCreated().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())), false)
                        .addField("유저 서버 입장일", event.getMember().getTimeJoined().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault())), false)
                        .addField("역할", stringBuilder.toString(), false)
                        .addField("퇴장 시간", time2, false);
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onGuildMemberRoleAdd(@Nonnull GuildMemberRoleAddEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                StringBuilder stringBuilder = new StringBuilder();

                for(Role role : event.getRoles()) {
                    stringBuilder.append(role.getAsMention()).append("\n");
                }

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("유저 역할 추가")
                        .setDescription("대상유저:" + event.getMember().getAsMention())
                        .setColor(Color.GREEN)
                        .addField("유저명", event.getMember().getEffectiveName() + "(" + event.getMember().getUser().getAsTag() + ") ", false)
                        .addField("추가된 역할", stringBuilder.toString(), false)
                        .addField("시간", time2, false);
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(@Nonnull GuildMemberRoleRemoveEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                StringBuilder stringBuilder = new StringBuilder();

                for(Role role : event.getRoles()) {
                    stringBuilder.append(role.getAsMention()).append("\n");
                }

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("유저 역할 삭제")
                        .setDescription("대상유저:" + event.getMember().getAsMention())
                        .setColor(Color.RED)
                        .addField("유저명", event.getMember().getEffectiveName() + "(" + event.getMember().getUser().getAsTag() + ") ", false)
                        .addField("삭제된 역할", stringBuilder.toString(), false)
                        .addField("시간", time2, false);
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onGuildMemberUpdateNickname(@Nonnull GuildMemberUpdateNicknameEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                String nickname = event.getOldNickname();
                if(nickname == null) {
                    nickname = "없음";
                }
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("유저 닉네임 변경")
                        .setDescription("대상유저:" + event.getMember().getAsMention())
                        .setColor(Color.GREEN)
                        .addField("이전 이름", nickname, false)
                        .addField("현재 이름", event.getNewNickname(), false)
                        .addField("시간", time2, false);
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onGuildBan(@Nonnull GuildBanEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("유저 밴")
                        .setDescription("대상유저:" + event.getUser().getAsTag())
                        .setColor(Color.RED)
                        .addField("시간", time2, false);
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onGuildUnban(@Nonnull GuildUnbanEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("유저 밴 헤제")
                        .setDescription("대상유저:" + event.getUser().getAsTag())
                        .setColor(Color.GREEN)
                        .addField("시간", time2, false);
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onGuildVoiceGuildMute(@Nonnull GuildVoiceGuildMuteEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed();
                if(event.isGuildMuted()) {
                    builder.setTitle("유저 강제 뮤트")
                            .setDescription("대상유저:" + event.getMember().getEffectiveName() + "(" + event.getMember().getAsMention() + ")")
                            .setColor(Color.RED)
                            .addField("시간", time2, false);
                } else {
                    builder.setTitle("유저 강제 뮤트 해제")
                            .setDescription("대상유저:" + event.getMember().getEffectiveName() + "(" + event.getMember().getAsMention() + ")")
                            .setColor(Color.RED)
                            .addField("시간", time2, false);
                }
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onRoleCreate(@Nonnull RoleCreateEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();
                Role role = event.getRole();

                String time2 = format2.format(time);
                StringBuilder stringBuilder = new StringBuilder();
                for(Permission permission : role.getPermissions()) {
                    stringBuilder.append(permission.getName()).append("\n");
                }
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("역할 생성")
                        .setColor(role.getColor())
                        .addField("역할명", role.getName() + "(" + role.getAsMention() + ")", false)
                        .addField("권한", stringBuilder.toString(), false)
                        .addField("멘션 가능", role.isMentionable() ? "예" : "아니오", true)
                        .addField("유저 분리 표시", role.isHoisted() ? "예" : "아니오", true)
                        .addField("시간", time2, false);
                List<AuditLogEntry> list = event.getGuild().retrieveAuditLogs().complete();
                AuditLogEntry log = list.get(list.size() - 1);
                Map<String, AuditLogChange> changelog = log.getChanges();
                memberLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onRoleDelete(@Nonnull RoleDeleteEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();
                Role role = event.getRole();

                String time2 = format2.format(time);
                StringBuilder stringBuilder = new StringBuilder();
                for(Permission permission : role.getPermissions()) {
                    stringBuilder.append(permission.getName()).append("\n");
                }
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("역할 삭제")
                        .setColor(role.getColor())
                        .addField("역할명", role.getName(), false)
                        .addField("권한", stringBuilder.toString(), false)
                        .addField("멘션 가능", role.isMentionable() ? "예" : "아니오", true)
                        .addField("유저 분리 표시", role.isHoisted() ? "예" : "아니오", true)
                        .addField("시간", time2, false);
                memberLoggingSend(builder, guild);
            }
        }
    }

    private void onRoleUpdate(@Nonnull Role role, @Nonnull Guild guild) {
        SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
        Date time = new Date();

        String time2 = format2.format(time);
        StringBuilder stringBuilder = new StringBuilder();
        for(Permission permission : role.getPermissions()) {
            stringBuilder.append(permission.getName()).append("\n");
        }
        EmbedBuilder builder = EmbedUtils.defaultEmbed()
                .setTitle("역할 변경")
                .setColor(role.getColor())
                .addField("역할명", role.getName() +  "(" + role.getAsMention() + ")", false)
                .addField("권한", stringBuilder.toString(), false)
                .addField("멘션 가능", role.isMentionable() ? "예" : "아니오", true)
                .addField("유저 분리 표시", role.isHoisted() ? "예" : "아니오", true)
                .addField("시간", time2, false);
        memberLoggingSend(builder, guild);
    }

    @Override
    public void onRoleUpdateColor(@Nonnull RoleUpdateColorEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                onRoleUpdate(event.getRole(), guild);
            }
        }
    }

    @Override
    public void onRoleUpdateHoisted(@Nonnull RoleUpdateHoistedEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                onRoleUpdate(event.getRole(), guild);
            }
        }
    }

    @Override
    public void onRoleUpdateMentionable(@Nonnull RoleUpdateMentionableEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                onRoleUpdate(event.getRole(), guild);
            }
        }
    }

    @Override
    public void onRoleUpdateName(@Nonnull RoleUpdateNameEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                onRoleUpdate(event.getRole(), guild);
            }
        }
    }

    @Override
    public void onRoleUpdatePermissions(@Nonnull RoleUpdatePermissionsEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                onRoleUpdate(event.getRole(), guild);
            }
        }
    }



    @Override
    public void onEmoteAdded(@Nonnull EmoteAddedEvent event) {
        Guild guild = event.getGuild();
        Emote emote = event.getEmote();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("서버 이모지 추가")
                        .setColor(Color.GREEN)
                        .addField("이모지명", emote.getName(), false)
                        .setDescription("[이모지 보기](" + emote.getImageUrl() + ")")
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onEmoteRemoved(@Nonnull EmoteRemovedEvent event) {
        Guild guild = event.getGuild();
        Emote emote = event.getEmote();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.defaultEmbed()
                        .setTitle("서버 이모지 제거")
                        .setColor(Color.GREEN)
                        .addField("이모지명", emote.getName(), false)
                        .setDescription("[이모지 보기](" + emote.getImageUrl() + ")")
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }
}
