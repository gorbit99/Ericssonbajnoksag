package me.defenestration.competition;

import org.capnproto.*;

import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

public class Main {

	private static SocketChannel channel;
	private static ArrayList<String> messages = new ArrayList<>();

	public static void main (String[] args) {
		try {
			channel = SocketChannel.open();
			channel.connect(new InetSocketAddress("epb2017.dyndns.org", 11224));

		} catch (Exception e) {
			System.out.println("main: " + e);
		}

		sendLogin();

		ResponseClass.Response.Reader response;

		byte bugs = 0;
		byte bugsSolved = 0;

		while (channel.isConnected()) {
			response = getResponse();
			if (response == null)
				continue;

			//Handling response----------------------------------------------
			System.out.println(responseToString(response));

			String msg;
		}
	}

	private static ResponseClass.Response.Reader getResponse() {
		try {
			MessageReader msgReader = Serialize.read(channel);
			return msgReader.getRoot(ResponseClass.Response.factory);
		} catch (Exception e) {
			//System.out.println("getResponse: " + e);
		}
		return null;
	}

	private static void sendCommand(ArrayList<CommandClass.Move.Builder> moves) {
		MessageBuilder msgBuilder = new MessageBuilder();
		CommandClass.Command.Builder command = msgBuilder.initRoot(CommandClass.Command.factory);
		CommandClass.Command.Commands.Builder commands = command.initCommands();
		org.capnproto.StructList.Builder<CommandClass.Move.Builder> moveList = commands.initMoves(moves.size());

		for (int i = 0; i < moves.size(); i++) {
			CommandClass.Move.Builder move = moveList.get(i);
			move.setUnit(0);
			move.setDirection(moves.get(i).getDirection());
		}
	}

	private static void sendLogin() {
		MessageBuilder msgBuilder = new MessageBuilder();
		CommandClass.Command.Builder loginRequest = msgBuilder.initRoot(CommandClass.Command.factory);
		CommandClass.Command.Commands.Builder commands = loginRequest.initCommands();
		CommandClass.Command.Commands.Login.Builder login = commands.initLogin();
		login.setTeam("defenestration");
		login.setHash("6ilwor8e6t3yv2kwvbwu5d5obl1upmu2ywteh");

		System.out.println("Sending login...");
		try {
			Serialize.write(channel, msgBuilder);
		} catch (Exception e) {
			System.out.println("sendLogin: " + e);
		}
	}

	private static String responseToString (ResponseClass.Response.Reader response) {
		return "┌-Response---------------------------------------------------------------\n" +
			   "|Status: " + response.getStatus() + '\n' +
			   "|Bugfix:\n" +
			   "|  Occupied: " + response.getInfo().getOwns() + '\n' +
			   "|  Level: " +response.getInfo().getLevel() + '\n' +
			   "|  Tick: " +response.getInfo().getTick() + '\n' +
			   "└------------------------------------------------------------------------";
	}
}
