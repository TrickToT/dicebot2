package com.hiyoko.discord.bot.BCDice.ChatTool;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.javacord.api.DiscordApi;

public class DiscordClientV2 implements ChatToolClient {
	private final DiscordApi api;

	public DiscordClientV2(DiscordApi api) {
		this.api = api;
	}

	@Override
	public boolean isRequest(String command) {
		return true;
	}

	@Override
	public List<String> input(String input) {
		String[] tmp = input.split(" ");
		String command = tmp[tmp.length - 1].toLowerCase();
		if(command.equals("listroomids")) {
			return getRoomIds();
		}
		if(command.equals("listrooms")) {
			return getRooms();
		}
		if(command.equals("listservers")) {
			return getServerList();
		}
		return new ArrayList<String>();
	}

	@Override
	public String formatMessage(String input) {
		String result = input.replaceAll("\\*\\*", "\\\\*\\\\*");
		return result;
	}

	private List<String> getRoomIds() {
		return api.getChannels().stream().filter(channel->{
			return channel.getType().isTextChannelType();
		}).map(channel->channel.getIdAsString()).collect(Collectors.toList());
	}

	private List<String> getRooms() {
		return api.getChannels().stream().filter(channel->{
			return channel.getType().isTextChannelType();
		}).map(channel->String.format("%s,%s,%s",
				channel.getIdAsString(),
				channel.asServerTextChannel().get().getName(),
				channel.asServerTextChannel().get().getServer().getName())).collect(Collectors.toList());
	}

	private List<String> getServerList() {
		return api.getServers().stream().map(server->String.format("%s,%s", server.getIdAsString(), server.getName())).collect(Collectors.toList());
	}
}
