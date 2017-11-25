package me.defenestration.competition;

import org.capnproto.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;

public class Main {

    private static SocketChannel channel;
    private static ArrayList<String> messages = new ArrayList<>();

    public static CommonClass.Direction direction = CommonClass.Direction.DOWN;

    public static void main(String[] args) {
        try {
            channel = SocketChannel.open();
            channel.connect(new InetSocketAddress("epb2017.dyndns.org", 11224));

        } catch (IOException e) {
            e.printStackTrace();
        }

        sendLogin();

        ResponseClass.Response.Reader response;

        byte bugs = 0;
        byte bugsSolved = 0;

        JFrame frame = new JFrame();
        frame.setSize(500, 600);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);

        Canvas canvas = new Canvas();
        canvas.setSize(500, 600);
        canvas.setBackground(Color.WHITE);
        canvas.setVisible(true);
        canvas.setFocusable(true);

        frame.add(canvas);

        canvas.createBufferStrategy(2);

        BufferStrategy bufferStrategy;
        Graphics graphics;

        frame.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {

            }

            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'w':
                        direction = CommonClass.Direction.LEFT;
                        break;
                    case 's':
                        direction = CommonClass.Direction.RIGHT;
                        break;
                    case 'a':
                        direction = CommonClass.Direction.UP;
                        break;
                    case 'd':
                        direction = CommonClass.Direction.DOWN;
                        break;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        while (channel.isConnected()) {
            response = getResponse();
            if (response == null)
                continue;

            //Handling response----------------------------------------------
            //System.out.println(response);

            bufferStrategy = canvas.getBufferStrategy();
            graphics = bufferStrategy.getDrawGraphics();
            graphics.clearRect(0, 0, 500, 600);

            ListList.Reader<StructList.Reader<ResponseClass.Cell.Reader>> cells = response.getCells();
            for (int x = 0; x < 80; x++) {
                StructList.Reader<ResponseClass.Cell.Reader> row = cells.get(x);
                for (int y = 0; y < 100; y++) {
                    int brightness = row.get(y).getOwner() * 20;
                    graphics.setColor(new Color(brightness, brightness, brightness));
                    graphics.fillRect(x * 5, y * 5, 5, 5);
                }
            }

            graphics.setColor(Color.RED);

            StructList.Reader<ResponseClass.Enemy.Reader> enemies = response.getEnemies();
            for (ResponseClass.Enemy.Reader enemy : enemies) {
                CommonClass.Position.Reader pos = enemy.getPosition();
                graphics.fillRect(pos.getX() * 5, pos.getY() * 5, 5, 5);
            }

            graphics.setColor(Color.GREEN);

            StructList.Reader<ResponseClass.Unit.Reader> units = response.getUnits();
            for (ResponseClass.Unit.Reader unit : units) {
                CommonClass.Position.Reader pos = unit.getPosition();
                graphics.fillRect(pos.getX() * 5, pos.getY() * 5, 5, 5);
            }

            //ArrayList<CommandClass.Move.Builder> moves = new ArrayList<>();

            MessageBuilder message = new MessageBuilder();

            CommandClass.Command.Builder commandBuilder = message.initRoot(CommandClass.Command.factory);

            CommandClass.Command.Commands.Builder command = commandBuilder.initCommands();

            StructList.Builder<CommandClass.Move.Builder> moves = command.initMoves(1);
            moves.get(0).setDirection(direction);
            moves.get(0).setUnit(0);

            try {
                SerializePacked.writeToUnbuffered(channel, message);
            } catch (IOException e) {
                e.printStackTrace();
            }

            bufferStrategy.show();
            graphics.dispose();
        }
    }

    private static ResponseClass.Response.Reader getResponse() {
        try {
            MessageReader msgReader = SerializePacked.readFromUnbuffered(channel);
            return msgReader.getRoot(ResponseClass.Response.factory);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        return null;
    }

    private static void sendCommand(ArrayList<CommandClass.Move.Builder> moves) {
        MessageBuilder msgBuilder = new MessageBuilder();
        CommandClass.Command.Builder command = msgBuilder.initRoot(CommandClass.Command.factory);
        CommandClass.Command.Commands.Builder commands = command.initCommands();
        StructList.Builder<CommandClass.Move.Builder> moveList = commands.initMoves(moves.size());

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
            SerializePacked.writeToUnbuffered(channel, msgBuilder);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
