package protocol;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
public class Packet {

    private static final byte HEADER_1 = (byte) 0xb2;
    private static final byte HEADER_2 = (byte) 0x1f;
    private static final byte FOOTER_1 = (byte) 0xf7;
    private static final byte FOOTER_2 = (byte) 0xcf;

    private byte type;

    private List<PacketField> fields =new ArrayList<>();

    private Packet(){}

    public static boolean compareEOP(byte[] arr, int lastItem) {
        return arr[lastItem - 1] == FOOTER_1 && arr [lastItem] == FOOTER_2;
    }

    public byte[] toByteArray() {
        try (ByteArrayOutputStream writer = new ByteArrayOutputStream()) {
            writer.write(new byte[] {HEADER_1, HEADER_2});
            writer.write(type);

            for (PacketField field: fields) {
                writer.write(new byte[] {field.getId(), field.getSize()});
                writer.write(field.getContent());
            }

            writer.write(new byte[] {FOOTER_1, FOOTER_2});
            return writer.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Packet parse(byte[] data) {
        if (data[0] != HEADER_1 && data[1] != HEADER_2
                || data[data.length - 1] != FOOTER_2 && data[data.length - 2] != FOOTER_1) {
            throw new IllegalArgumentException("Unknown packet format");
        }

        byte type = data[2];
        Packet packet = Packet.create(type);
        int offset = 3;
        while (true) {
            if (data.length - 2 <= offset) {
                return packet;
            }

            byte fieldId = data[offset];
            byte fieldSize = data[offset + 1];

            byte[] content = new byte[Byte.toUnsignedInt(fieldSize)];
            if (fieldSize != 0) {
                System.arraycopy(data, offset + 2, content, 0, Byte.toUnsignedInt(fieldSize));
            }

            PacketField field = new PacketField(fieldId, fieldSize, content);
            packet.getFields().add(field);

            offset += 2 + fieldSize;
        }
    }

    public PacketField getField(int id) {
        Optional<PacketField> field = getFields().stream()
                .filter(f -> f.getId() == (byte) id)
                .findFirst();
        if (!field.isPresent()) {
            throw new IllegalArgumentException("No field with that id");
        }
        return field.get();
    }

    public <T> T getValue(int id, Class<T> clazz) {
        PacketField field = getField(id);
        try (ByteArrayInputStream bis = new ByteArrayInputStream(field.getContent());
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public void setValue(int id, Object value) {
        PacketField field;
        try {
            field = getField(id);
        } catch (IllegalArgumentException e) {
            field = new PacketField((byte) id);
        }
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(value);
            byte[] data = bos.toByteArray();
            if (data.length > 255) {
                throw new IllegalArgumentException("Too much data sent");
            }
            field.setSize((byte) data.length);
            field.setContent(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        getFields().add(field);
    }

    public static Packet create(int type) {
        Packet packet = new Packet();
        packet.type = (byte) type;
        return packet;
    }


    @Data
    @AllArgsConstructor
    public static class PacketField{
        private byte id;
        private byte size;
        private byte[] content;

        public PacketField(byte id) {
            this.id = id;
        }
    }
}
