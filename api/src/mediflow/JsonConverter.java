package mediflow;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON ringkas. Angka utuh tidak ditulis dengan ".0". */
final class JsonConverter {
    static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        SimpleModule modul = new SimpleModule();
        modul.addSerializer(Double.class, new JsonSerializer<>() {
            @Override
            public void serialize(Double value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null || value.isNaN() || value.isInfinite()) {
                    gen.writeNull();
                    return;
                }
                if (Math.abs(value) < 1e15 && value == Math.rint(value)) {
                    gen.writeNumber(value.longValue());
                } else {
                    gen.writeNumber(value);
                }
            }
        });
        MAPPER.registerModule(modul);
    }

    private JsonConverter() {}

    static final class Rusak extends Exception {
        Rusak(String message) {
            super(message);
        }
    }

    static Map<String, Object> objek(byte[] tubuh) throws Rusak {
        if (tubuh == null || tubuh.length == 0) {
            throw new Rusak("Body JSON tidak valid.");
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(tubuh);
        } catch (IOException error) {
            throw new Rusak("Body JSON tidak valid.");
        }
        if (node.isObject() && node.isEmpty()) {
            throw new Rusak("Data tidak boleh kosong");
        }
        if (!node.isObject()) {
            if (node.isArray() && node.isEmpty()) {
                throw new Rusak("Data tidak boleh kosong");
            }
            throw new Rusak("Body JSON tidak valid.");
        }
        Object hasil = dari(node);
        if (!(hasil instanceof Map<?, ?> peta)) {
            throw new Rusak("Body JSON tidak valid.");
        }
        Map<String, Object> objek = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entri : peta.entrySet()) {
            objek.put(String.valueOf(entri.getKey()), entri.getValue());
        }
        return objek;
    }

    /** DatabaseSnapshot menerima objek tabel. Objek kosong gagal di pemeriksaan tabel, bukan di parser. */
    static Map<String, Object> objekSnapshot(byte[] tubuh) throws Rusak {
        if (tubuh == null || tubuh.length == 0) {
            throw new Rusak("Body JSON tidak valid.");
        }
        JsonNode node;
        try {
            node = MAPPER.readTree(tubuh);
        } catch (IOException error) {
            throw new Rusak("Body JSON tidak valid.");
        }
        if (!node.isObject()) {
            throw new Rusak("Body JSON tidak valid.");
        }
        Object hasil = dari(node);
        if (!(hasil instanceof Map<?, ?> peta)) {
            throw new Rusak("Body JSON tidak valid.");
        }
        Map<String, Object> objek = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entri : peta.entrySet()) {
            objek.put(String.valueOf(entri.getKey()), entri.getValue());
        }
        return objek;
    }

    private static Object dari(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isIntegralNumber()) {
            return node.longValue();
        }
        if (node.isFloatingPointNumber()) {
            return node.doubleValue();
        }
        if (node.isTextual()) {
            return node.asText();
        }
        if (node.isArray()) {
            List<Object> daftar = new ArrayList<>();
            node.forEach(item -> daftar.add(dari(item)));
            return daftar;
        }
        if (node.isObject()) {
            Map<String, Object> peta = new LinkedHashMap<>();
            node.fields().forEachRemaining(entri -> peta.put(entri.getKey(), dari(entri.getValue())));
            return peta;
        }
        return node.asText();
    }
}
