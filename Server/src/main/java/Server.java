import protocol.Packet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class Server extends Thread {

    private Socket socket;
    private InputStream in;
    private OutputStream out;


    public void printStory(OutputStream writer, int size) {
        try {
            Packet packet = Packet.create(2);
            packet.setValue(1, (size+1));
            writer.write(packet.toByteArray());
            writer.flush();
        } catch (IOException ignored) {
        }
    }

    public Server(Socket socket) throws IOException {
        this.socket = socket;
        in = socket.getInputStream();
        out = socket.getOutputStream();
        printStory(out, ServerApp.serverList.size());
        start();
    }

    @Override
    public void run() {
        String word;
        try {
            try {
                while (true) {
                    byte[] data = readInput(in);
                    Packet packet = Packet.parse(data);
                    if (packet.getType()==1) {
                        word = packet.getValue(1, String.class);
                        if (word.equals("pause")) {
                            for (Server vr : ServerApp.serverList) {
                                vr.send(word);
                            }
                        } else {
                            for (Server vr : ServerApp.serverList) {
                                if (this == vr) {
                                    continue;
                                }
                                vr.send(word);
                            }
                        }
                    }
                }
            } catch (NullPointerException ignored) {
            }


        } catch (IOException e) {
            this.downService();
        }
    }

    private void send(String msg) {
        try {
            Packet packet = Packet.create(1);
            packet.setValue(1, msg);
            out.write(packet.toByteArray());
            out.flush();
        } catch (IOException ignored) {
        }
    }

    private void downService() {
        try {
            if (!socket.isClosed()) {
                socket.close();
                in.close();
                out.close();
                for (Server vr : ServerApp.serverList) {
                    if (vr.equals(this)) vr.interrupt();
                    ServerApp.serverList.remove(this);
                }
            }
        } catch (IOException ignored) {
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

