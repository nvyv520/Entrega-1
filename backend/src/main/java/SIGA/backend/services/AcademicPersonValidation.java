package SIGA.backend.services;

import java.util.Objects;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class AcademicPersonValidation {

    private AcademicPersonValidation() {}

    public static String compactRut(String value) {
        return value.replace(".", "").replace("-", "").toUpperCase(java.util.Locale.ROOT);
    }

    public static String normalizeRut(String value, String current) {
        // Preserve legacy identifiers when another attribute is being edited.
        if (current != null && Objects.equals(value, current)) return current;
        if (current != null && current.startsWith("SEED-2026-")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador del docente de prueba no se puede modificar");
        }
        if (value == null || !value.matches("(?:[1-9][0-9]{0,7}-?[0-9Kk]|[1-9][0-9]{0,2}(?:\\.[0-9]{3}){1,2}-[0-9Kk])")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa un RUN con formato válido, sin espacios y terminado en un número o K");
        }
        String compact = compactRut(value);
        if (!compact.matches("[1-9][0-9]{0,7}[0-9K]")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El RUN no tiene un formato válido");
        }
        String body = compact.substring(0, compact.length() - 1);
        String digit = compact.substring(compact.length() - 1);
        return body.replaceAll("\\B(?=(?:[0-9]{3})+(?![0-9]))", ".") + "-" + digit;
    }

    public static String normalizeEmail(String value, String current) {
        if (current != null && Objects.equals(value, current)) return current;
        String email = value == null ? "" : value.trim();
        String[] parts = email.split("@", -1);
        boolean valid = parts.length == 2 && email.length() <= 254 && parts[0].length() <= 64
                && parts[0].matches("[A-Za-z0-9!#$%&'*+/=?^_`{|}~.-]+")
                && !parts[0].startsWith(".") && !parts[0].endsWith(".") && !email.contains("..");
        if (valid) {
            String[] labels = parts[1].split("\\.", -1);
            valid = parts[1].length() <= 253 && labels.length >= 2
                    && labels[labels.length - 1].matches("[A-Za-z]{2,63}");
            for (String label : labels) {
                valid &= label.length() <= 63 && label.matches("[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?");
            }
        }
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresa un correo válido, por ejemplo nombre@institucion.cl");
        return email;
    }

    public static String normalizeName(String value, String current) {
        if (current != null && Objects.equals(value, current)) return current;
        String name = value == null ? "" : Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS).matcher(value.strip()).replaceAll(" ").strip();
        if (name.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre y apellido son obligatorios");
        // Capitalization supplied by the user is preserved.
        return name;
    }
}
