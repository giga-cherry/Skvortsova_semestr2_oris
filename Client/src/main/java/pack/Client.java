package pack;

import protocol.Packet;

import java.net.*;
import java.io.*;

class Client {

    private Socket socket;
    private InputStream in;
    private OutputStream out;
    private String str;
    private String sendMessage = null;


    private Integer positionPlayer;

    public Client(String addr, int port) {
        // ip адрес клиента
        // порт соединения
        try {
            this.socket = new Socket(addr, port);
        } catch (IOException e) {
            System.err.println("Socket failed");
        }
        try {
            in = socket.getInputStream();
            out = socket.getOutputStream();
            new ReadMsg().start(); // нить читающая сообщения из сокета в бесконечном цикле
            new WriteMsg().start(); // нить пишущая сообщения в сокет приходящие с консоли в бесконечном цикле
        } catch (IOException e) {
            Client.this.downService();
        }
    }

    /**
     * закрытие сокета
     */
    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
            }
        } catch (IOException ignored) {
        }
    }

    private boolean needToPause = true;
    private boolean needToSend = true;
    private double oldX = -1.0;
    private double oldY = -1.0;

    public synchronized void pausePoint() throws InterruptedException {
        while (needToPause) {
            wait();
        }
    }

    private int type;

    public synchronized void pause() {
        needToPause = true;
    }

    public synchronized void unpause() {
        needToPause = false;
        this.notifyAll();
    }

    public synchronized void send(double x, double y) {
        type = 1;
        sendMessage = x + "," + y + "";
        if (x == oldX && y == oldY) {
            needToSend = false;
        } else {
            oldX = x;
            oldY = y;
            needToSend = true;
        }
    }

    public synchronized void sendPlay() {
        type = 1;
        sendMessage = "play";
        needToSend = true;
    }

    private class ReadMsg extends Thread {
        @Override
        public void run() {
            try {
                while (true) {
                    byte[] data = readInput(in);
                    Packet packet = Packet.parse(data);
                    switch (packet.getType()) {
                        case (1):
                            str = packet.getValue(1, String.class);
                            break;
                        case (2):
                            positionPlayer = packet.getValue(1, Integer.class);
                            break;

                    }
                }
            } catch (IOException e) {
                Client.this.downService();
            }
        }
    }

    public synchronized String read() {
        return str;
    }

    public int getPositionPlayer() {
        while (positionPlayer == null) {
        }
        return positionPlayer;
    }

    // нить отправляющая сообщения приходящие с консоли на сервер
    public class WriteMsg extends Thread {

        @Override
        public void run() {
            while (true) {
                try {
                    pausePoint();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    if (needToSend) {
                        Packet packet = Packet.create(type);
                        packet.setValue(1, sendMessage);
                        out.write(packet.toByteArray());
                        out.flush();
                        needToSend = false;
                    }
                } catch (IOException e) {
                    Client.this.downService();

                }

            }
        }
    }

    private byte[] extendArray(byte[] oldArray) {
        int oldSize = oldArray.length;
        byte[] newArray = new byte[oldSize * 2];
        System.arraycopy(oldArray, 0, newArray, 0, oldSize);
        return newArray;
    }

    private byte[] readInput(InputStream stream) throws IOException {
        int b;
        byte[] buffer = new byte[10];
        int counter = 0;
        while ((b = stream.read()) > -1) {
            buffer[counter++] = (byte) b;
            if (counter >= buffer.length) {
                buffer = extendArray(buffer);
            }
            if (counter > 1 && Packet.compareEOP(buffer, counter - 1)) {
                break;
            }
        }
        byte[] data = new byte[counter];
        System.arraycopy(buffer, 0, data, 0, counter);
        return data;
    }
}