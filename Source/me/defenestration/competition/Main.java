package me.defenestration.competition;

import org.capnproto.*;

import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class Main {

	static SocketChannel channel;
	static ArrayList<String> messages = new ArrayList<String>();

	public static void main (String[] args) {
		try {
			channel = SocketChannel.open();
			channel.connect(new InetSocketAddress("ecovpn.dyndns.org", 11223));

		} catch (Exception e) {
			System.out.println("main: " + e);
		}

		sendLogin("defenestration", "6ilwor8e6t3yv2kwvbwu5d5obl1upmu2ywteh");

		while (channel.isConnected()) {
			ResponseClass.Response.Reader response = getResponse();
			if (response != null) {

				System.out.println(response.getEnd());
				System.out.println(response.getStatus().toString());
				System.out.println("Bugfix:");
				System.out.println("  " + response.getBugfix().getBugs());
				System.out.println("  " + response.getBugfix().getMessage());

				byte bugs = response.getBugfix().getBugs();
				String msg = "Fixed";

				if (response.getEnd() || response.getBugfix().getBugs() == 0)
					break;
				//for (int i = 0; i < bugs; i++)
					messages.add("Fixed");
			}

			if (messages.size() > 0) {
				String msg = messages.remove(0);
				sendRequest((byte)10, msg);
			}
		}
	}

	static ResponseClass.Response.Reader getResponse() {
		try {
			MessageReader msgReader = Serialize.read(channel);
			ResponseClass.Response.Reader response = msgReader.getRoot(ResponseClass.Response.factory);
			return response;
		} catch (Exception e) {
			//System.out.println("getResponse: " + e);
		}
		return null;
	}

	static void sendRequest(byte count, String msg) {
		MessageBuilder msgBuilder = new MessageBuilder();
		RequestClass.Request.Builder bugfixRequest = msgBuilder.initRoot(RequestClass.Request.factory);

		bugfixRequest.getBugfix().setBugs(count);
		bugfixRequest.getBugfix().setMessage(msg);

		System.out.println("Sending...");
		try {
			Serialize.write(channel, msgBuilder);
		} catch (Exception e) {
			System.out.println("sendRequest: " + e);
		}
	}

	static void sendLogin(String team, String hash) {
		MessageBuilder msgBuilder = new MessageBuilder();
		RequestClass.Request.Builder loginRequest = msgBuilder.initRoot(RequestClass.Request.factory);
		loginRequest.getLogin().setTeam(team);
		loginRequest.getLogin().setHash(hash);

		System.out.println("Sending...");
		try {
			Serialize.write(channel, msgBuilder);
		} catch (Exception e) {
			System.out.println("sendLogin: " + e);
		}
	}
}
