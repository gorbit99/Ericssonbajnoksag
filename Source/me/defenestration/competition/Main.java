package me.defenestration.competition;

import org.capnproto.*;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class Main {

	private static SocketChannel channel;
	private static ArrayList<String> messages = new ArrayList<>();

	public static void main (String[] args) {
		try {
			channel = SocketChannel.open();
			channel.connect(new InetSocketAddress("ecovpn.dyndns.org", 11223));

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

			if (response.getStatus().toString().equals("Congratulation! You fixed all the bugs!")) {
				msg = "I solved a huge amount of bug. I am proud of myself.";
				bugsSolved++;
				bugs = bugsSolved;
			} else {
				msg = "Fixed";
				if (response.getBugfix().getBugs() != 0)
					bugs = response.getBugfix().getBugs();
			}
			messages.add(msg);

			//Checking if connection should be closed------------------------
			if (response.getEnd() || messages.size() == 0) {
				try {
					channel.close();
				} catch (Exception e) {
					System.out.println("channel.close(): " + e);
				}
				break;
			}

			//Send message---------------------------------------------------
			String m = messages.remove(0);
			bugs--;
			bugsSolved++;
			sendRequest(bugs, m);
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

	private static void sendRequest(byte count, String msg) {
		MessageBuilder msgBuilder = new MessageBuilder();
		RequestClass.Request.Builder bugfixRequest = msgBuilder.initRoot(RequestClass.Request.factory);
		BugfixClass.Bugfix.Builder bugfix = bugfixRequest.initBugfix();
		bugfix.setBugs(count);
		bugfix.setMessage(msg);

		System.out.println("Sending request...");
		try {
			Serialize.write(channel, msgBuilder);
		} catch (Exception e) {
			System.out.println("sendRequest: " + e);
		}
	}

	private static void sendLogin() {
		MessageBuilder msgBuilder = new MessageBuilder();
		RequestClass.Request.Builder loginRequest = msgBuilder.initRoot(RequestClass.Request.factory);
		RequestClass.Request.Login.Builder login = loginRequest.getLogin();
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
			   "|End: " + response.getEnd() + '\n' +
			   "|Status: " + response.getStatus() + '\n' +
			   "|Bugfix:\n" +
			   "|  Bugs: " + response.getBugfix().getBugs() + '\n' +
			   "|  Message: " +response.getBugfix().getMessage() + '\n' +
			   "└------------------------------------------------------------------------";
	}
}
