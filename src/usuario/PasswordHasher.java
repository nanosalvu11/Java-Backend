package usuario;

import org.mindrot.jbcrypt.BCrypt;

import java.util.regex.Pattern;

public final class PasswordHasher {
    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private PasswordHasher() {
    }

    public static String hash(String plainPassword) {
        if (plainPassword == null || plainPassword.isBlank()) {
            throw new IllegalArgumentException("El password es obligatorio");
        }
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    public static boolean matches(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        if (!isBcryptHash(hashedPassword)) {
            return false;
        }
        return BCrypt.checkpw(plainPassword, hashedPassword);
    }

    public static boolean isBcryptHash(String value) {
        return value != null && BCRYPT_PATTERN.matcher(value).matches();
    }
}

