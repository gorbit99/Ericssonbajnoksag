package me.defenestration.competition;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
	Socket client;
	BufferedReader input;
	DataOutputStream output;

	public Client (String host, int port) {
		try {
			client = new Socket(host, port);
		} catch (Exception e) {
			System.out.println(e);
		}

		try {
			DataInputStream is = new DataInputStream(client.getInputStream());
			input = new BufferedReader(new InputStreamReader(is));
		} catch (Exception e) {
			System.out.println(e);
		}

		try {
		    output = new DataOutputStream(client.getOutputStream());
        } catch (Exception e) {
            System.out.println(e);
        }
	}

	public void write (String msg) {
	    try {
            output.writeBytes(msg);
        } catch (Exception e) {
	        System.out.println(e);
        }
    }

    public String readLine () {
	    try {
	        return input.readLine();
        } catch (Exception e) {
	        return null;
        }
    }

	public void close () {
	    try {
            output.close();
            input.close();
            client.close();
        } catch (IOException e) {
	        System.out.println(e);
        }
    }
}
