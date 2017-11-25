package me.defenestration.competition;

import java.io.*;
import java.net.Socket;

public class Client {
    private Socket client;
    private BufferedReader input;
    private DataOutputStream output;

    public Client(String host, int port) {
        try {
            client = new Socket(host, port);

            DataInputStream is = new DataInputStream(client.getInputStream());
            input = new BufferedReader(new InputStreamReader(is));

            output = new DataOutputStream(client.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(String msg) {
        try {
            output.writeBytes(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String readLine() {
        try {
            return input.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    public void close() {
        try {
            output.close();
            input.close();
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
