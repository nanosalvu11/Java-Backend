package http;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
    public Map<String, Object> parseObject(String json) {
        Object value = new Parser(json).parseValue();
        if (value == null) {
            return new LinkedHashMap<>();
        }
        if (!(value instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON");
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            result.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return result;
    }

    public List<Object> parseArray(String json) {
        Object value = new Parser(json).parseValue();
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalArgumentException("Se esperaba un array JSON");
        }
        return new ArrayList<>(list);
    }

    public String stringify(Object value) {
        StringBuilder sb = new StringBuilder();
        writeValue(sb, value);
        return sb.toString();
    }

    private void writeValue(StringBuilder sb, Object value) {
        if (value == null) {
            sb.append("null");
            return;
        }
        if (value instanceof String string) {
            sb.append('"').append(escape(string)).append('"');
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(value);
            return;
        }
        if (value instanceof Map<?, ?> map) {
            sb.append('{');
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                sb.append('"').append(escape(String.valueOf(entry.getKey()))).append('"').append(':');
                writeValue(sb, entry.getValue());
            }
            sb.append('}');
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            sb.append('[');
            boolean first = true;
            for (Object item : iterable) {
                if (!first) {
                    sb.append(',');
                }
                first = false;
                writeValue(sb, item);
            }
            sb.append(']');
            return;
        }
        if (value.getClass().isArray()) {
            sb.append('[');
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                writeValue(sb, Array.get(value, i));
            }
            sb.append(']');
            return;
        }
        writeValue(sb, value.toString());
    }

    private String escape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> escaped.append("\\\"");
                case '\\' -> escaped.append("\\\\");
                case '\b' -> escaped.append("\\b");
                case '\f' -> escaped.append("\\f");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> {
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }

    private static final class Parser {
        private final String input;
        private int index;

        private Parser(String input) {
            this.input = input == null ? "" : input.trim();
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEnd()) {
                return null;
            }
            char c = peek();
            return switch (c) {
                case '{' -> parseObject();
                case '[' -> parseArray();
                case '"' -> parseString();
                case 't' -> parseLiteral("true", Boolean.TRUE);
                case 'f' -> parseLiteral("false", Boolean.FALSE);
                case 'n' -> parseLiteral("null", null);
                default -> parseNumber();
            };
        }

        private Map<String, Object> parseObject() {
            expect('{');
            Map<String, Object> map = new LinkedHashMap<>();
            skipWhitespace();
            if (match('}')) {
                return map;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                map.put(key, value);
                skipWhitespace();
                if (match('}')) {
                    return map;
                }
                expect(',');
            }
        }

        private List<Object> parseArray() {
            expect('[');
            List<Object> list = new ArrayList<>();
            skipWhitespace();
            if (match(']')) {
                return list;
            }
            while (true) {
                list.add(parseValue());
                skipWhitespace();
                if (match(']')) {
                    return list;
                }
                expect(',');
            }
        }

        private String parseString() {
            expect('"');
            StringBuilder sb = new StringBuilder();
            while (!isEnd()) {
                char c = next();
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (isEnd()) {
                        throw error("Secuencia de escape incompleta");
                    }
                    char escaped = next();
                    switch (escaped) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> sb.append(parseUnicode());
                        default -> throw error("Escape JSON invalido: " + escaped);
                    }
                } else {
                    sb.append(c);
                }
            }
            throw error("String JSON sin cerrar");
        }

        private char parseUnicode() {
            if (index + 4 > input.length()) {
                throw error("Unicode escape incompleto");
            }
            String hex = input.substring(index, index + 4);
            index += 4;
            return (char) Integer.parseInt(hex, 16);
        }

        private Object parseNumber() {
            int start = index;
            if (peek() == '-') {
                index++;
            }
            while (!isEnd() && Character.isDigit(peek())) {
                index++;
            }
            if (!isEnd() && peek() == '.') {
                index++;
                while (!isEnd() && Character.isDigit(peek())) {
                    index++;
                }
            }
            if (!isEnd() && (peek() == 'e' || peek() == 'E')) {
                index++;
                if (!isEnd() && (peek() == '+' || peek() == '-')) {
                    index++;
                }
                while (!isEnd() && Character.isDigit(peek())) {
                    index++;
                }
            }
            String number = input.substring(start, index);
            if (number.isBlank()) {
                throw error("Numero JSON invalido");
            }
            return new BigDecimal(number);
        }

        private Object parseLiteral(String literal, Object value) {
            if (input.startsWith(literal, index)) {
                index += literal.length();
                return value;
            }
            throw error("Literal JSON invalido: " + literal);
        }

        private void skipWhitespace() {
            while (!isEnd() && Character.isWhitespace(peek())) {
                index++;
            }
        }

        private boolean match(char expected) {
            if (!isEnd() && peek() == expected) {
                index++;
                return true;
            }
            return false;
        }

        private void expect(char expected) {
            skipWhitespace();
            if (isEnd() || peek() != expected) {
                throw error("Se esperaba '" + expected + "'");
            }
            index++;
        }

        private char peek() {
            return input.charAt(index);
        }

        private char next() {
            return input.charAt(index++);
        }

        private boolean isEnd() {
            return index >= input.length();
        }

        private IllegalArgumentException error(String message) {
            return new IllegalArgumentException(message + " en posicion " + index);
        }
    }
}



