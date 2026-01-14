package doom.util;

import java.security.MessageDigest;

public class HWIDUtil {
    public static String getHWID() {
        try {
            String toHash = System.getenv("PROCESSOR_IDENTIFIER") +
                    System.getenv("COMPUTERNAME") +
                    System.getProperty("user.name");

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(toHash.getBytes());
            StringBuffer hexString = new StringBuffer();

            byte byteData[] = md.digest();
            for (byte aByteData : byteData) {
                String hex = Integer.toHexString(0xff & aByteData);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "Error";
        }
    }
}