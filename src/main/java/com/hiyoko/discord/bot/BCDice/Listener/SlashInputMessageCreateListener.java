package com.hiyoko.discord.bot.BCDice.Listener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.interaction.SlashCommandCreateEvent;
import org.javacord.api.interaction.SlashCommand;
import org.javacord.api.interaction.SlashCommandInteraction;
import org.javacord.api.interaction.SlashCommandInteractionOption;
import org.javacord.api.interaction.SlashCommandOption;
import org.javacord.api.interaction.SlashCommandOptionType;
import org.javacord.api.listener.interaction.SlashCommandCreateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hiyoko.discord.bot.BCDice.BCDiceCLI;
import com.hiyoko.discord.bot.BCDice.AdminCommand.*;
import com.hiyoko.discord.bot.BCDice.ChatTool.ChatToolClient;
import com.hiyoko.discord.bot.BCDice.ChatTool.DiscordClientV2;
import com.hiyoko.discord.bot.BCDice.ConfigCommand.*;
import com.hiyoko.discord.bot.BCDice.DiceClient.DiceClient;
import com.hiyoko.discord.bot.BCDice.DiceResultFormatter.DiceResultFormatter;
import com.hiyoko.discord.bot.BCDice.DiceResultFormatter.DiceResultFormatterFactory;
import com.hiyoko.discord.bot.BCDice.NameIndicator.NameIndicator;
import com.hiyoko.discord.bot.BCDice.NameIndicator.NameIndicatorFactory;
import com.hiyoko.discord.bot.BCDice.dto.DicerollResult;

public class SlashInputMessageCreateListener implements SlashCommandCreateListener {
	private final Logger logger = LoggerFactory.getLogger(StandardInputMessageCreateListener.class);
	private final DiscordApi api;
	private final BCDiceCLI bcDice;
	private final NameIndicator nameIndicator;
	private final DiceResultFormatter diceResultFormatter;
	private final User admin;
	private final ChatToolClient chatToolClient;

	private final String prefix;
	private final String shortPrefix;

	private final Map<String, AdminCommand> adminCommands = AdminCommandsMapFactory.getAdminCommands();
	private final Map<String, ConfigCommand> configCommands;

	public SlashInputMessageCreateListener(DiscordApi api, BCDiceCLI cli) throws InterruptedException, ExecutionException {
		this.api = api;
		this.bcDice = cli;
		this.nameIndicator = NameIndicatorFactory.getNameIndicator();
		this.diceResultFormatter = DiceResultFormatterFactory.getDiceResultFormatter();
		this.admin = api.getOwner().get();
		this.chatToolClient = new DiscordClientV2(api);
		this.prefix = "bcdice";
		this.shortPrefix = "br";
		defineSlashCommand();
		this.configCommands = bcDice.getConfigCommands();
	}

	public SlashInputMessageCreateListener(DiscordApi api, BCDiceCLI cli, String prefix, String shortPrefix) throws InterruptedException, ExecutionException {
		this.api = api;
		this.bcDice = cli;
		this.nameIndicator = NameIndicatorFactory.getNameIndicator();
		this.diceResultFormatter = DiceResultFormatterFactory.getDiceResultFormatter();
		this.admin = api.getOwner().get();
		this.chatToolClient = new DiscordClientV2(api);
		this.prefix = prefix.startsWith("/") ? prefix.substring(1) : prefix;
		this.shortPrefix = shortPrefix.startsWith("/") ? shortPrefix.substring(1) : shortPrefix;
		defineSlashCommand();
		this.configCommands = bcDice.getConfigCommands();
	}

	private void defineSlashCommand() {
		SlashCommand.with(prefix, "BCDice のダイスボットを利用します", Arrays.asList(
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "roll", String.format("ダイスを振ります。/%s というショートカットもあります", shortPrefix), Arrays.asList(
				SlashCommandOption.create(SlashCommandOptionType.STRING, "diceCommand", "振りたいダイスのコマンドです", true)
			)),
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "config", "ダイスボットの設定を確認・実施します", Arrays.asList(
				SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "status", "ダイスボットが利用しているダイスサーバやバージョン、使用しているシステムを確認します"),
				SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "list", "ダイスボットで利用できるシステムを一覧します"),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "set", "ダイスボットで使用するシステムを設定します", Arrays.asList(
						SlashCommandOption.create(SlashCommandOptionType.STRING, "system", "ダイスボットで使用するシステムです。Cthulhu7th や DoubleCross、SwordWorld2.5　等", true)
				)),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "help", "ダイスボットで使用するシステムのヘルプメッセージを参照します", Arrays.asList(
						SlashCommandOption.create(SlashCommandOptionType.STRING, "system", "ダイスボットで使用するシステムです。Cthulhu7th や DoubleCross、SwordWorld2.5　等", true)
				)),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "load", "シークレットダイスの結果を呼び出します", Arrays.asList(
						SlashCommandOption.create(SlashCommandOptionType.STRING, "key", "シークレットダイスを振った際にDMに送られてくる値です", true)
				))
			)),
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "admin", "（管理者向け）ダイスボットを管理します", Arrays.asList(
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "setServer", "（管理者向け）ダイスサーバを変更します", Arrays.asList(
					SlashCommandOption.create(SlashCommandOptionType.STRING, "serverURL", "新しく利用するダイスサーバの URL です", true)
				)),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "removeServer", "（管理者向け）ダイスサーバを削除します", Arrays.asList(
					SlashCommandOption.create(SlashCommandOptionType.STRING, "serverURL", "削除するダイスサーバの URL です", true)
				)),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "listServer", "（管理者向け）ダイスサーバを一覧します"),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "refreshSecretDice", "（管理者向け）保存してあるシークレットダイスの結果をリセットします"),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "updateDiceRollPreFix", "（管理者向け）ダイスコマンドを再読み込みします"),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "addOriginalTable", "（管理者向け）スラッシュコマンドでのオリジナル表の追加は未対応です"),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "removeOriginalTable", "（管理者向け）オリジナル表を削除します", Arrays.asList(
					SlashCommandOption.create(SlashCommandOptionType.STRING, "originalTable", "（管理者向け）削除するオリジナル表の名前です", true)
				)),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "listOriginalTable", "（管理者向け）利用可能なオリジナル表を一覧します"),
				SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND, "reloadOriginalTable", "（管理者向け）利用可能なオリジナル表を再読み込みして一覧します")
			)),
			SlashCommandOption.createWithOptions(SlashCommandOptionType.SUB_COMMAND_GROUP, "Discord", "（管理者向け）ダイスボットが導入されているDiscordサーバについて確認します", Arrays.asList(
				SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "listRoomIds", "（管理者向け）チャンネルの ID を一覧します"),
				SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "listRooms", "（管理者向け）チャンネルの情報を一覧します"),
				SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "listServers", "（管理者向け）サーバを一覧します"),
				SlashCommandOption.create(SlashCommandOptionType.SUB_COMMAND, "removeSlashCommands", "（管理者向け）スラッシュコマンドを全削除し、再起動するまでスラッシュコマンドが使えなくなります。スラッシュコマンドを再設定したい場合はこれを実行した後にダイスボットを停止・再起動してください")
			))
		)).createGlobal(api).join();
		SlashCommand.with(shortPrefix, "ダイスを振ります", Arrays.asList(
			SlashCommandOption.create(SlashCommandOptionType.STRING, "diceCommand", "振りたいダイスのコマンドです", true)
		)).createGlobal(api).join();
	}

	private List<String> handleRoll(String diceCommand, TextChannel channel, User user) throws IOException {
		List<DicerollResult> rollResults = bcDice.rolls(bcDice.getRollCommand() + " " + diceCommand, channel.getIdAsString());
		if( rollResults.size() > 0 && rollResults.get(0).isRolled() ) {
			logger.debug("Dice command request for dice server is done");
			List<String> sb = new ArrayList<String>();
			for(DicerollResult rollResult : rollResults) {
				if(rollResult.isError()) {
					throw new IOException(rollResult.getText());
				}
				if( rollResult.isRolled() ) {
					sb.add(diceResultFormatter.getText(rollResult));
				}
			}
			List<String> resultMessage = bcDice.separateStringWithLengthLimitation(
				String.format("＞%s\n> %s\n%s", nameIndicator.getName(user), diceCommand, sb.stream().collect(Collectors.joining("\n\n"))), 1000);
			DicerollResult firstOne = rollResults.get(0);
			if( firstOne.isSecret() ) {
				String index = bcDice.saveMessage(user.getIdAsString(), resultMessage);
				List<String> secretResult = new ArrayList<String>();
				secretResult.add(diceResultFormatter.getText(new DicerollResult(
					String.format("[Secret Dice] Key: %s", index),
					firstOne.getSystem(),
					true, true
				)));
				for(String post : resultMessage) {
					user.sendMessage(chatToolClient.formatMessage(post));
				}
				user.sendMessage(String.format("この結果を呼び出すには次のようにしてください。\n> bcdice load %s\nこのコマンドは最短72時間後には無効になります\nその後も必要であればそのままコピー&ペーストするか、スクリーンショットなどで共有してください", index));
				return secretResult;
			} else {
				return resultMessage;
			}
		} else {
			String message = String.format("ダイスを振るのに失敗しました。ダイスコマンドとして恐らく無効です\n%s", diceCommand); 
			logger.warn(message);
			List<String> empty = new ArrayList<String>();
			empty.add(message);
			return empty;
		}
	}

	private List<String> handleAdmin(SlashCommandInteractionOption option, TextChannel channel, User user) {
		DiceClient client = bcDice.getDiceClient();
		String subCommand = option.getName();
		AdminCommand command = adminCommands.get(subCommand);
		if(command != null) {
			return bcDice.separateStringWithLengthLimitation(command.exec(option, client), 1000);
		} else {
			logger.warn(String.format("無効な管理コマンド %s が実行されました", subCommand));
		}
		return null;
	}

	private List<String> handleDiscord(SlashCommandInteractionOption option, TextChannel channel, User user) {
		return bcDice.separateStringWithLengthLimitation(chatToolClient.input(option.getName()), 1000);
	}

	private List<String> handleConfig(SlashCommandInteractionOption option, TextChannel channel, User user) {
		DiceClient client = bcDice.getDiceClient();
		String subCommand = option.getName();
		ConfigCommand command = configCommands.get(subCommand);
		if(command != null) {
			return command.exec(option, client, user, channel);
		} else {
			logger.warn(String.format("無効なコマンド %s が実行されました", subCommand));
		}
		return null;
	}

	private List<String> getSingleMessage(String message) {
		List<String> msg = new ArrayList<String>();
		msg.add(message);
		return msg;
	}

	@Override
	public void onSlashCommandCreate(SlashCommandCreateEvent event) {
		SlashCommandInteraction interaction = event.getSlashCommandInteraction();
		User user = interaction.getUser();
		TextChannel channel = interaction.getChannel().get();

		List<String> responseMessage = null;
		SlashCommandInteractionOption firstOption = interaction.getOptionByIndex(0).get();
		if(interaction.getCommandName().equals(shortPrefix)) {
			String diceCommand = firstOption.getStringValue().orElse("");
			try {
				responseMessage = handleRoll(diceCommand, channel, user);
			} catch (IOException ioe) {
				responseMessage = getSingleMessage(String.format("[ERROR]%s", ioe.getMessage()));
				logger.warn(String.format("USERID: %s MESSAGE: %s", user.getIdAsString() , diceCommand));
				logger.warn("Failed to reply to user request", ioe);
			}
		}
		if(interaction.getCommandName().equals(prefix)) {
			String firstOptionAsString = firstOption.getName();

			SlashCommandInteractionOption secondOption = firstOption.getOptionByIndex(0).get();

			if(firstOptionAsString.equals("roll")) {
				String diceCommand = secondOption.getStringValue().orElse("");
				try {
					responseMessage = handleRoll(diceCommand, channel, user);
				} catch (IOException ioe) {
					responseMessage = getSingleMessage(String.format("[ERROR]%s", ioe.getMessage()));
					logger.warn(String.format("USERID: %s MESSAGE: %s", user.getIdAsString() , diceCommand));
					logger.warn("Failed to reply to user request", ioe);
				}
			} else if(firstOptionAsString.equals("config")) {
				responseMessage = handleConfig(secondOption, channel, user);
			} else if(firstOptionAsString.equals("admin")) {
				if(user.getIdAsString().equals(admin.getIdAsString())) {
					responseMessage = handleAdmin(secondOption, channel, user);
				} else {
					responseMessage = getSingleMessage("Bot の管理者以外が admin コマンドを使うことはできません");
				}
			} else if(firstOptionAsString.equals("discord")) {
				if(user.getIdAsString().equals(admin.getIdAsString())) {
					responseMessage = handleDiscord(secondOption, channel, user);
				} else {
					responseMessage = getSingleMessage("Bot の管理者以外が discord コマンドを使うことはできません");
				}
			}
		}

		if(responseMessage == null || responseMessage.size() == 0) {
			responseMessage = getSingleMessage("無効なコマンドっぽいです。未実装なのかもしれない。");
		}
		try {
			interaction.createImmediateResponder().setContent(chatToolClient.formatMessage(responseMessage.get(0))).respond();
			if(responseMessage.size() > 1) {
				responseMessage.subList(1, responseMessage.size()).forEach((post)->{
					channel.sendMessage(chatToolClient.formatMessage(post));
				});
			}
		} catch (Exception e) {
			logger.warn("結果の送信に失敗しました", e);
		}
	}
}
