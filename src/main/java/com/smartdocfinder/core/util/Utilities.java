package com.smartdocfinder.core.util;

import java.security.MessageDigest;
import java.util.HexFormat;

public class Utilities {
    public static String computeFileHash(byte[] bytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(bytes);
        return HexFormat.of().formatHex(hash);
    }
}
