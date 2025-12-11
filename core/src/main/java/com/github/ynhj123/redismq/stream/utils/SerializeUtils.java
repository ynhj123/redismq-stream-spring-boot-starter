package com.github.ynhj123.redismq.stream.utils;

import java.io.*;

public class SerializeUtils {
    // Java原生序列化
    public static <V> String serialize(V obj) throws IOException {
        if (!(obj instanceof Serializable)) {
            throw new IllegalArgumentException("Object must implement Serializable");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(obj);
        oos.close();
        return java.util.Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    // Java原生反序列化
    public static <V> V deserialize(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = java.util.Base64.getDecoder().decode(data);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ObjectInputStream ois = new ObjectInputStream(bais);
        V obj = (V) ois.readObject();
        ois.close();
        return obj;
    }
}
