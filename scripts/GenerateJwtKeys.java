import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Generates the Ed25519 key pair for auth-service JWT signing, in the format KeyProvider expects:
 * Base64-encoded DER - PKCS#8 private key, X.509 public key.
 *
 * Usage (JDK 21, no compilation needed):
 *   java scripts/GenerateJwtKeys.java                 -> prints the variables
 *   java scripts/GenerateJwtKeys.java .env .env.local -> fills JWT_PRIVATE_KEY / JWT_PUBLIC_KEY in the files
 *
 * Existing non-empty keys are never overwritten (tokens signed with them would stop validating);
 * clear both values in a file to regenerate. All given files get the same key pair.
 */
public class GenerateJwtKeys {

    private static final String PRIVATE_KEY = "JWT_PRIVATE_KEY";
    private static final String PUBLIC_KEY = "JWT_PUBLIC_KEY";

    public static void main(String[] args) throws NoSuchAlgorithmException, IOException {
        KeyPair keyPair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
        Map<String, String> keys = Map.of(
                PRIVATE_KEY, Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded()),
                PUBLIC_KEY, Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded())
        );

        if (args.length == 0) {
            System.out.println(PRIVATE_KEY + "=" + keys.get(PRIVATE_KEY));
            System.out.println(PUBLIC_KEY + "=" + keys.get(PUBLIC_KEY));
            return;
        }
        for (String file : args) {
            fill(Path.of(file), keys);
        }
    }

    private static void fill(Path file, Map<String, String> keys) throws IOException {
        if (!Files.exists(file)) {
            System.out.println("skipped " + file + ": file does not exist");
            return;
        }
        List<String> lines = new ArrayList<>(Files.readAllLines(file));
        for (String name : keys.keySet()) {
            if (lines.stream().anyMatch(line -> line.startsWith(name + "=") && line.length() > name.length() + 1)) {
                System.out.println("skipped " + file + ": " + name + " is already set (clear both keys to regenerate)");
                return;
            }
        }
        for (String name : List.of(PRIVATE_KEY, PUBLIC_KEY)) {
            String entry = name + "=" + keys.get(name);
            int index = indexOf(lines, name + "=");
            if (index >= 0) {
                lines.set(index, entry);
            } else {
                lines.add(entry);
            }
        }
        Files.write(file, lines);
        System.out.println("wrote JWT keys to " + file);
    }

    private static int indexOf(List<String> lines, String prefix) {
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }
}
