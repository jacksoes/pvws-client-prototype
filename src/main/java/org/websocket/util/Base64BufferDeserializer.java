package org.websocket.util;

import com.fasterxml.jackson.databind.JsonNode;
import org.websocket.models.PV;

import java.nio.*;
import java.util.Base64;

public class Base64BufferDeserializer {

    private static final ByteOrder BYTE_ORDER = ByteOrder.LITTLE_ENDIAN;

    public static short[] decodeShorts(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        ShortBuffer shortBuffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER).asShortBuffer();

        short[] array = new short[shortBuffer.remaining()];
        shortBuffer.get(array);
        return array;
    }

    public static int[] decodeInts(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        IntBuffer intBuffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER).asIntBuffer();

        int[] array = new int[intBuffer.remaining()];
        intBuffer.get(array);
        return array;
    }

    public static float[] decodeFloats(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        FloatBuffer floatBuffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER).asFloatBuffer();

        float[] array = new float[floatBuffer.remaining()];
        floatBuffer.get(array);
        return array;
    }

    public static double[] decodeDoubles(String base64) {
        byte[] bytes = Base64.getDecoder().decode(base64);
        DoubleBuffer doubleBuffer = ByteBuffer.wrap(bytes).order(BYTE_ORDER).asDoubleBuffer();

        double[] array = new double[doubleBuffer.remaining()];
        doubleBuffer.get(array);
        return array;
    }



    public static void decodeArrValue(JsonNode node, PV pvObj) {

        //check for encoded array field and if there is set value to decoded array.
        if (node.has("b64dbl")) {
            String base64Encoded = node.get("b64dbl").asText();
            double[] doubles = Base64BufferDeserializer.decodeDoubles(base64Encoded);
            pvObj.setValue(doubles);
        }
        else if (node.has("b64flt")) {
            String base64Encoded = node.get("b64flt").asText();
            float[] floats = Base64BufferDeserializer.decodeFloats(base64Encoded);
            pvObj.setValue(floats);
        }
        else if (node.has("b64int")) {
            String base64Encoded = node.get("b64int").asText();
            int[] ints = Base64BufferDeserializer.decodeInts(base64Encoded);
            pvObj.setValue(ints);
        }
        else if (node.has("b64srt")) {
            String base64Encoded = node.get("b64srt").asText();
            short[] shorts = Base64BufferDeserializer.decodeShorts(base64Encoded);
            pvObj.setValue(shorts);
        }
        else if (node.has("b64byt")) {
            String base64Encoded = node.get("b64byt").asText();
            byte[] bytes = Base64.getDecoder().decode(base64Encoded);
            pvObj.setValue(bytes);
        }




    }
}