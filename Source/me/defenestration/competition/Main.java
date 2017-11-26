package me.defenestration.competition;

import com.sun.deploy.util.StringUtils;
import org.capnproto.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.io.*;
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

        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setVisible(true);
        frame.setFocusable(true);

        Canvas canvas = new Canvas();
        canvas.setSize(400, 500);
        canvas.setBackground(Color.WHITE);
        canvas.setVisible(true);
        canvas.setFocusable(true);

        frame.add(canvas);
        frame.pack();

        canvas.addKeyListener(new KeyListener() {
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
        canvas.createBufferStrategy(2);

        BufferStrategy bufferStrategy;
        Graphics graphics;
        ResponseClass.Response.Reader response;

        int level = -1;
        boolean replaying = false;
        ArrayList<CommonClass.Direction> replay = new ArrayList<>();

        while (channel.isConnected()) {
            response = getResponse();
            if (response == null)
                continue;

            //Handling response----------------------------------------------

            try {
                if (level < response.getInfo().getLevel()) {

                    if (!replaying && level > -1) {
                        File out = new File( level + ".txt");
                        out.createNewFile();
                        BufferedWriter writer = new BufferedWriter(new FileWriter(out));

                        StringBuilder builder = new StringBuilder();
                        int count = 0;
                        for (CommonClass.Direction d : replay) {
                            if (count != 0)
                                builder.append(",");
                            builder.append(d.toString());
                            count++;
                        }

                        writer.write(builder.toString());
                        writer.flush();
                        writer.close();
                    }

                    level++;
                    File file = new File(level + ".txt");

                    replaying = false;
                    replay.clear();
                    if (file.exists()) {
                        replaying = true;
                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        String[] data = reader.readLine().split(",");
                        for (String d : data) {
                            replay.add(CommonClass.Direction.parse(Integer.parseInt(d)));
                        }

                        reader.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                frame.setTitle(e.getMessage());
            }

            bufferStrategy = canvas.getBufferStrategy();
            graphics = bufferStrategy.getDrawGraphics();

            int filled = draw(graphics, response);

            bufferStrategy.show();
            graphics.dispose();

            CommonClass.Position.Reader p = response.getUnits().get(0).getPosition();

            if (direction == CommonClass.Direction.DOWN && p.getX() == 79)
                direction = CommonClass.Direction.UP;
            if (direction == CommonClass.Direction.UP && p.getX() == 0)
                direction = CommonClass.Direction.DOWN;
            if (direction == CommonClass.Direction.LEFT && p.getY() == 0)
                direction = CommonClass.Direction.RIGHT;
            if (direction == CommonClass.Direction.RIGHT && p.getY() == 99)
                direction = CommonClass.Direction.LEFT;

            MessageBuilder message = new MessageBuilder();
            CommandClass.Command.Builder commandBuilder = message.initRoot(CommandClass.Command.factory);
            CommandClass.Command.Commands.Builder command = commandBuilder.initCommands();
            StructList.Builder<CommandClass.Move.Builder> moves = command.initMoves(1);

            if (replaying) {
                direction = replay.get(response.getInfo().getTick() - 1);
            } else {
                replay.add(direction);
            }

            moves.get(0).setDirection(direction);
            moves.get(0).setUnit(0);

            try {
                SerializePacked.writeToUnbuffered(channel, message);
            } catch (IOException e) {
                e.printStackTrace();
            }

            frame.setTitle(response.getInfo().getTick() + "/" + (1000 + (response.getInfo().getLevel() % 500) * 10) + " " + (100 - filled / 80f) + "%");

            if (!replaying) {
                try {
                    Thread.sleep(80);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private static int draw(Graphics graphics, ResponseClass.Response.Reader response) {
        int filled = 0;

        graphics.clearRect(0, 0, 400, 500);
        ListList.Reader<StructList.Reader<ResponseClass.Cell.Reader>> cells = response.getCells();
        for (int x = 0; x < 80; x++) {
            StructList.Reader<ResponseClass.Cell.Reader> row = cells.get(x);
            for (int y = 0; y < 100; y++) {
                int brightness = row.get(y).getOwner() * 20;
                graphics.setColor(new Color(brightness, brightness, brightness));
                if (brightness == 0)
                    filled++;
                if (row.get(y).getAttack().isUnit())
                    graphics.setColor(Color.pink);
                graphics.fillRect(x * 5, y * 5, 5, 5);
            }
        }


        StructList.Reader<ResponseClass.Enemy.Reader> enemies = response.getEnemies();
        for (ResponseClass.Enemy.Reader enemy : enemies) {
            CommonClass.Position.Reader pos = enemy.getPosition();
            graphics.setColor(Color.RED);
            graphics.fillRect(pos.getX() * 5, pos.getY() * 5, 5, 5);
        }

        graphics.setColor(Color.GREEN);

        StructList.Reader<ResponseClass.Unit.Reader> units = response.getUnits();
        for (ResponseClass.Unit.Reader unit : units) {
            CommonClass.Position.Reader pos = unit.getPosition();
            graphics.fillRect(pos.getX() * 5, pos.getY() * 5, 5, 5);
        }

        graphics.setColor(Color.GREEN);

        for (ResponseClass.Unit.Reader unit : units) {
            CommonClass.Position.Reader pos = unit.getPosition();
            graphics.fillRect(pos.getX() * 5, pos.getY() * 5, 5, 5);
        }

        return filled;
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
