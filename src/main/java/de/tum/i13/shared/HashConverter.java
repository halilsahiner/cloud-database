package de.tum.i13.shared;

import org.javatuples.Pair;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class HashConverter {

    public static Logger logger = Logger.getLogger(HashConverter.class.getName());

    /*
     * Since message digest packet is not thread-safe,
     * we need to define a new instance in every thread we use.
     */
    public static String getMd5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] messageDigest = md.digest(input.getBytes());
            BigInteger no = new BigInteger(1, messageDigest);

            StringBuilder hashText = new StringBuilder(no.toString(16));
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }
            return hashText.toString();
        }
        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isInKeyRange(Pair<String, String> range, String key) {
        String keyInMD5 = getMd5(key);
        if (range.getValue0().compareTo(range.getValue1()) == 0) {
            return true;
        } else if (range.getValue0().compareTo(range.getValue1()) > 0) {
            return (keyInMD5.compareTo(range.getValue0())) < 0 && (keyInMD5.compareTo(range.getValue1()) < 0) ||
                    (keyInMD5.compareTo(range.getValue0())) > 0 && (keyInMD5.compareTo(range.getValue1()) > 0);
        }

        return (keyInMD5.compareTo(range.getValue0())) > 0 && (keyInMD5.compareTo(range.getValue1()) < 0);
    }

}
