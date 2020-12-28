package BOT.Listener;

import BOT.Objects.SQL;
import BOT.Objects.config;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import me.duncte123.botcommons.messaging.EmbedUtils;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.Permission;
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
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideCreateEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideDeleteEvent;
import net.dv8tion.jda.api.events.guild.override.PermissionOverrideUpdateEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceGuildMuteEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.*;
import net.dv8tion.jda.api.events.role.*;
import net.dv8tion.jda.api.events.role.update.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

public class loggerListener extends ListenerAdapter {
    private final SQL sql;
    public loggerListener(SQL sql) {
        this.sql = sql;
    }

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
                List<Message.Attachment> files = message.getAttachments();
                if(!files.isEmpty()) {
                    int i = 0;
                    for(Message.Attachment attachment : files) {
                        if(attachment.isImage()) {
                            i++;
                            File file = attachment.downloadToFile().join();
                            S3UploadObject(file, messageId + "_" + i);
                            file.delete();
                        }
                    }
                }
                final boolean[] temp = {sql.loggingMessageUpLoad(guild.getId(), messageId, messageContent, authorId)};
                String finalMessageContent = messageContent;
                Timer timer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        temp[0] = sql.loggingMessageUpLoad(guild.getId(), messageId, finalMessageContent, authorId);
                        if(temp[0]) {
                            timer.cancel();
                        }
                    }
                };
                if(!temp[0]) {
                    timer.scheduleAtFixedRate(timerTask, 0, 1000);
                }
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
                String[] data = sql.loggingMessageDownLoad(guild.getId(), messageId);
                if(data[0] == null) {
                    return;
                }
                if (data[0].length() < 2) {
                    return;
                }
                final boolean[] temp = {sql.loggingMessageUpdate(guild.getId(), messageId, messageContent)};
                Timer timer = new Timer();
                TimerTask timerTask = new TimerTask() {
                    @Override
                    public void run() {
                        temp[0] = sql.loggingMessageUpdate(guild.getId(), messageId, messageContent);
                        if(temp[0]) {
                            timer.cancel();
                        }
                    }
                };
                timer.scheduleAtFixedRate(timerTask, 0, 1000);

                Member member = event.getGuild().getMemberById(authorId);
                assert member != null;
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");

                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                        .setTitle("수정된 메세지")
                        .setColor(Color.ORANGE)
                        .setDescription("메세지 수정: " + event.getChannel().getAsMention() + "\n" +
                                "[메세지 이동](" + message.getJumpUrl() + ")");
                try {
                    builder.addField("수정전 내용", data[0], false);
                } catch (Exception e) {
                    e.printStackTrace();
                    builder.addField("수정전 내용", "1024자 이상이라서 표현할 수 없습니다.", false);
                }
                try {
                    builder.addField("수정후 내용", messageContent, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    builder.addField("수정후 내용", "1024자 이상이라서 표현할 수 없습니다.", false);
                }
                builder.addField("수정 시간", time2, false)
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
            String[] data = sql.loggingMessageDownLoad(guild.getId(), messageId);
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
                File file = S3DownloadObject(messageId + "_" + 1);
                String[] data = sql.loggingMessageDownLoad(guild.getId(), messageId);
                try {
                    if (data[0].length() < 2) {
                        if(file == null) {
                            return;
                        }
                    }
                } catch (NullPointerException e) {
                    return;
                }
                Member member = event.getGuild().getMemberById(data[1]);
                assert member != null;
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");

                Date time = new Date();
                String time2 = format2.format(time);
                if(file != null) {
                    if(data[0].length() < 2) {
                        data[0] = "사진 파일만 있는 메세지";
                    }
                }
                if(data[0].length() > 1024) {
                    data[0] = "1024자 초과로 인한 처리 불가";
                }
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                        .setTitle("삭제된 메세지")
                        .setColor(Color.RED)
                        .setDescription("메세지 삭제: " + event.getChannel().getAsMention())
                        .addField("내용", data[0], false)
                        .addField("메세지 ID", messageId, false)
                        .addField("삭제 시간", time2, false)
                        .setFooter((member.getEffectiveName() + "(" + member.getEffectiveName() + ")"), member.getUser().getAvatarUrl());
                messageLoggingSend(builder, guild);
                if(file != null) {
                    messageLoggingSend(file, guild);
                }
                if(file != null) {
                    file.delete();
                }
            }
        }
    }
    private void messageLoggingSend(@NotNull File file, @NotNull Guild guild) {
        String channelId = sql.configDownLoad_channel(guild.getId(), SQL.textLogChannel);
        boolean a = false;
        if(!channelId.equals("error")) {
            try {
                Objects.requireNonNull(guild.getTextChannelById(channelId)).sendFile(file).queue();
                a = true;
            } catch (Exception e) {
                e.printStackTrace();
                a = false;
            }
        }
        if(!a) {
            List<TextChannel> channels = guild.getTextChannelsByName("채팅-로그", false);
            if (!channels.isEmpty()) {
                channels.get(0).sendFile(file).queue();
            } else {
                List<TextChannel> channels1 = guild.getTextChannelsByName("chat-logs", false);
                if (!channels.isEmpty()) {
                    channels1.get(0).sendFile(file).queue();
                }
            }
        }
    }
    private void messageLoggingSend(@NotNull EmbedBuilder builder, @NotNull Guild guild) {
        String channelId = sql.configDownLoad_channel(guild.getId(), SQL.textLogChannel);
        boolean a = false;
        if(!channelId.equals("error")) {
            try {
                Objects.requireNonNull(guild.getTextChannelById(channelId)).sendMessage(builder.build()).queue();
                a = true;
            } catch (Exception e) {
                e.printStackTrace();
                a = false;
            }
        }
        if(!a) {
            List<TextChannel> channels = guild.getTextChannelsByName("채팅-로그", false);
            if (!channels.isEmpty()) {
                channels.get(0).sendMessage(builder.build()).queue();
            } else {
                List<TextChannel> channels1 = guild.getTextChannelsByName("chat-logs", false);
                if (!channels.isEmpty()) {
                    channels1.get(0).sendMessage(builder.build()).queue();
                }
            }
        }
    }

    private void channelLoggingSend(@NotNull EmbedBuilder builder, @NotNull Guild guild) {
        String channelId = sql.configDownLoad_channel(guild.getId(), SQL.textLogChannel);
        boolean a = false;
        if(!channelId.equals("error")) {
            try {
                Objects.requireNonNull(guild.getTextChannelById(channelId)).sendMessage(builder.build()).queue();
                a = true;
            } catch (Exception e) {
                e.printStackTrace();
                a = false;
            }
        }
        if(!a) {
            List<TextChannel> channels = guild.getTextChannelsByName("채널-로그", false);
            if (!channels.isEmpty()) {
                channels.get(0).sendMessage(builder.build()).queue();
            } else {
                List<TextChannel> channels1 = guild.getTextChannelsByName("channel-logs", false);
                if (!channels.isEmpty()) {
                    channels1.get(0).sendMessage(builder.build()).queue();
                }
            }
        }

    }

    private void memberLoggingSend(@NotNull EmbedBuilder builder, @NotNull Guild guild) {
        String channelId = sql.configDownLoad_channel(guild.getId(), SQL.textLogChannel);
        boolean a = false;
        if(!channelId.equals("error")) {
            try {
                Objects.requireNonNull(guild.getTextChannelById(channelId)).sendMessage(builder.build()).queue();
                a = true;
            } catch (Exception e) {
                e.printStackTrace();
                a = false;
            }
        }
        if(!a) {
            List<TextChannel> channels = guild.getTextChannelsByName("멤버-로그", false);
            if (!channels.isEmpty()) {
                channels.get(0).sendMessage(builder.build()).queue();
            } else {
                List<TextChannel> channels1 = guild.getTextChannelsByName("member-logs", false);
                if (!channels.isEmpty()) {
                    channels1.get(0).sendMessage(builder.build()).queue();
                }
            }
        }

    }

    @Override
    public void onReconnect(@Nonnull ReconnectedEvent event) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            sql.setLoggingConnection(DriverManager.getConnection(sql.getUrl(), sql.getUser(), sql.getPassword()));
            SQL.reConnection();
        } catch (@NotNull SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void onGuildJoin(@Nonnull GuildJoinEvent event) {
        String guildId = event.getGuild().getId();
        try {
            Statement statement = sql.getConnection().createStatement();
            statement.executeUpdate("INSERT INTO ritobot_config.color_command_guild VALUES (" + guildId + ", 1)");
            statement.executeUpdate("INSERT INTO ritobot_config.filter_guild VALUES (" + guildId + ", 0)");
            statement.executeUpdate("INSERT INTO ritobot_config.kill_filter_guild VALUES (" + guildId + ", 0)");
            statement.executeUpdate("INSERT INTO ritobot_config.lewdneko_command VALUES (" + guildId + ", 0)");
            statement.executeUpdate("INSERT INTO ritobot_config.link_filter_guild VALUES (" + guildId + ", 0)");
            statement.executeUpdate("INSERT INTO ritobot_config.logging_enable VALUES (" + guildId + ", 1, 1, 1)");
            statement.executeUpdate("INSERT INTO ritobot_config.notice VALUES (" + guildId + ", 0, '0')");
            statement.executeUpdate("INSERT INTO ritobot_config.filter_output_channel VALUES (" + guildId + ", 0, '1')");
            statement.executeUpdate("INSERT INTO ritobot_config.bot_channel VALUES (" + guildId + " , 0 , 1)");
            statement.executeUpdate("INSERT INTO ritobot_config.custom_Filter VALUES (" + guildId + ", 1, '{\"data\": [\"none\"]}')");
            statement.executeUpdate("INSERT INTO ritobot_config.log_channel VALUES (" + guildId + ", 0, 0, 0)");
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
    public void onPermissionOverrideCreate(@Nonnull PermissionOverrideCreateEvent event) {
        Guild guild = event.getGuild();
        for(String guildId : config.getChannelLoggingEnable()) {
            if(guild.getId().equals(guildId)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed();
                StringBuilder stringBuilder = new StringBuilder();
                for(Permission permission : event.getPermissionOverride().getAllowed()) {
                    stringBuilder.append("\u2795").append(permission.getName()).append("\n");
                }
                switch(event.getChannelType()) {
                    case CATEGORY:
                        if (event.getPermissionOverride().isRoleOverride()) {
                            builder.setTitle("카테고리 권한 오버라이딩 생성")
                                    .setColor(Color.GREEN)
                                    .addField("카테고리명", event.getCategory().getName(), false)
                                    .addField("변경된 권한(" + Objects.requireNonNull(event.getPermissionOverride().getRole()).getAsMention() + ")", stringBuilder.toString(), false)
                                    .addField("변경 시간", time2, false);
                        } else if(event.getPermissionOverride().isMemberOverride()) {
                            builder.setTitle("카테고리 권한 오버라이딩 생성")
                                    .setColor(Color.GREEN)
                                    .addField("카테고리명", event.getCategory().getName(), false)
                                    .addField("변경된 권한(" + Objects.requireNonNull(event.getPermissionOverride().getMember()).getAsMention() + ")", stringBuilder.toString(), false)
                                    .addField("변경 시간", time2, false);
                        }
                        break;
                    case TEXT:
                        if (event.getPermissionOverride().isRoleOverride()) {
                            builder.setTitle("텍스트 채널 권한 오버라이딩 생성")
                                    .setColor(Color.GREEN)
                                    .addField("채널명", event.getCategory().getName(), false)
                                    .addField("변경된 권한(" + Objects.requireNonNull(event.getPermissionOverride().getRole()).getAsMention() + ")", stringBuilder.toString(), false)
                                    .addField("변경 시간", time2, false);
                        } else if(event.getPermissionOverride().isMemberOverride()) {
                            builder.setTitle("텍스트 채널 권한 오버라이딩 생성")
                                    .setColor(Color.GREEN)
                                    .addField("채널명", event.getCategory().getName(), false)
                                    .addField("변경된 권한(" + Objects.requireNonNull(event.getPermissionOverride().getMember()).getAsMention() + ")", stringBuilder.toString(), false)
                                    .addField("변경 시간", time2, false);
                        }
                        break;
                    case VOICE:
                        if (event.getPermissionOverride().isRoleOverride()) {
                            builder.setTitle("보이스 채널 권한 오버라이딩 생성")
                                    .setColor(Color.GREEN)
                                    .addField("채널명", event.getCategory().getName(), false)
                                    .addField("변경된 권한(" + Objects.requireNonNull(event.getPermissionOverride().getRole()).getAsMention() + ")", stringBuilder.toString(), false)
                                    .addField("변경 시간", time2, false);
                        } else if(event.getPermissionOverride().isMemberOverride()) {
                            builder.setTitle("보이스 채널 권한 오버라이딩 생성")
                                    .setColor(Color.GREEN)
                                    .addField("채널명", event.getCategory().getName(), false)
                                    .addField("변경된 권한(" + Objects.requireNonNull(event.getPermissionOverride().getMember()).getAsMention() + ")", stringBuilder.toString(), false)
                                    .addField("변경 시간", time2, false);
                        }
                        break;
                }
                channelLoggingSend(builder, guild);
            }
        }
    }

    @Override
    public void onPermissionOverrideUpdate(@Nonnull PermissionOverrideUpdateEvent event) {
        super.onPermissionOverrideUpdate(event);
    }

    @Override
    public void onPermissionOverrideDelete(@Nonnull PermissionOverrideDeleteEvent event) {
        Guild guild = event.getGuild();
        for(String guildId : config.getChannelLoggingEnable()) {
            if(guild.getId().equals(guildId)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed();
                switch(event.getChannelType()) {
                    case CATEGORY:
                        if (event.getPermissionOverride().isRoleOverride()) {
                            builder.setTitle("카테고리 권한 오버라이딩 삭제")
                                    .setColor(Color.GREEN)
                                    .addField("카테고리명", event.getCategory().getName(), false)
                                    .addField("변경 시간", time2, false);
                        } else if(event.getPermissionOverride().isMemberOverride()) {
                            builder.setTitle("카테고리 권한 오버라이딩 삭제")
                                    .setColor(Color.GREEN)
                                    .addField("카테고리명", event.getCategory().getName(), false)
                                    .addField("변경 시간", time2, false);
                        }
                        break;
                    case TEXT:
                        if (event.getPermissionOverride().isRoleOverride()) {
                            builder.setTitle("텍스트 채널 권한 오버라이딩 삭제")
                                    .setColor(Color.GREEN)
                                    .addField("채널명", event.getCategory().getName(), false)
                                    .addField("변경 시간", time2, false);
                        } else if(event.getPermissionOverride().isMemberOverride()) {
                            builder.setTitle("텍스트 채널 권한 오버라이딩 삭제")
                                    .setColor(Color.GREEN)
                                    .addField("채널명", event.getCategory().getName(), false)
                                    .addField("변경 시간", time2, false);
                        }
                        break;
                    case VOICE:
                        if (event.getPermissionOverride().isRoleOverride()) {
                            builder.setTitle("보이스 채널 권한 오버라이딩 삭제")
                                    .setColor(Color.GREEN)
                                    .addField("채널명", event.getCategory().getName(), false)
                                    .addField("변경 시간", time2, false);
                        } else if(event.getPermissionOverride().isMemberOverride()) {
                            builder.setTitle("보이스 채널 권한 오버라이딩 삭제")
                                    .setColor(Color.GREEN)
                                    .addField("채널명", event.getCategory().getName(), false)
                                    .addField("변경 시간", time2, false);
                        }
                        break;
                }
                channelLoggingSend(builder, guild);
            }
        }
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
                    builder = EmbedUtils.getDefaultEmbed()
                            .setTitle("후방 주의 채널 해제")
                            .setColor(Color.GREEN)
                            .addField("채널명", event.getChannel().getAsMention(), false)
                            .addField("변경 시간", time2, false);
                } else {
                    builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
    public void onCategoryDelete(@Nonnull CategoryDeleteEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
    public void onCategoryCreate(@Nonnull CategoryCreateEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getChannelLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
    public void onGuildMemberRemove(@Nonnull GuildMemberRemoveEvent event) {
        Guild guild = event.getGuild();
        for(String guild1 : config.getMemberLoggingEnable()) {
            if (guild.getId().equals(guild1)) {
                SimpleDateFormat format2 = new SimpleDateFormat("yyyy년 MM월dd일 HH시mm분ss초");
                Date time = new Date();

                StringBuilder stringBuilder = new StringBuilder();

                for(Role role : Objects.requireNonNull(event.getMember()).getRoles()) {
                    stringBuilder.append(role.getName()).append("\n");
                }

                String time2 = format2.format(time);
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed();
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
        EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
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
                EmbedBuilder builder = EmbedUtils.getDefaultEmbed()
                        .setTitle("서버 이모지 제거")
                        .setColor(Color.GREEN)
                        .addField("이모지명", emote.getName(), false)
                        .setDescription("[이모지 보기](" + emote.getImageUrl() + ")")
                        .addField("변경 시간", time2, false);
                channelLoggingSend(builder, guild);
            }
        }
    }

    private void S3UploadObject(File file, String messageId) {
        Regions clientRegion = Regions.AP_NORTHEAST_2;
        String bucketName = "ritobot-logger";

        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new EnvironmentVariableCredentialsProvider())
                    .build();

            PutObjectRequest request = new PutObjectRequest(bucketName, messageId, file);
            ObjectMetadata metadata = new ObjectMetadata();
            request.setMetadata(metadata);
            request.setStorageClass(StorageClass.StandardInfrequentAccess);
            s3Client.putObject(request);
        } catch (SdkClientException e) {
            e.printStackTrace();
        }
    }

    private File S3DownloadObject(String messageId) {
        Regions clientRegion = Regions.AP_NORTHEAST_2;
        String bucketName = "ritobot-logger";

        try {
            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .withCredentials(new EnvironmentVariableCredentialsProvider())
                    .build();

            GetObjectRequest request = new GetObjectRequest(bucketName, messageId);
            S3Object object = s3Client.getObject(request);
            ObjectMetadata metadata = object.getObjectMetadata();
            InputStream inputStream = object.getObjectContent();
            Path path = Files.createTempFile(messageId, "." + metadata.getContentType().split("/")[1]);
            try (FileOutputStream out = new FileOutputStream(path.toFile())) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } catch (Exception e) {
                // TODO: handle exception
                return null;
            }
            return path.toFile();

        } catch (SdkClientException | IOException e) {
            return null;
        }
    }
}
