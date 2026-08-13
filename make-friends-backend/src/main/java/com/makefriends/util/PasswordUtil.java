package com.makefriends.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.security.MessageDigest;

public class PasswordUtil {

    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    public static String encode(String rawPassword) {
        return ENCODER.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        // 支持 BCrypt 格式（$2a$ $2b$ $2y$ 开头）
        if (encodedPassword.startsWith("$2")) {
            return ENCODER.matches(rawPassword, encodedPassword);
        }
        // 兼容旧版 MD5 格式（32位hex）
        if (encodedPassword.length() == 32) {
            return md5(rawPassword).equalsIgnoreCase(encodedPassword);
        }
        return false;
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
