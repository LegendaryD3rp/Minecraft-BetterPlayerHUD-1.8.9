package com.yourname.compassmod;

public class KeyCodes {
    // 常用按键代码常量
    public static final int KEY_I = 23;        // I键
    public static final int KEY_F = 34;        // F键
    public static final int KEY_LEFT_SHIFT = 42; // 左Shift
    public static final int KEY_LEFT_CONTROL = 29; // 左Ctrl
    public static final int KEY_CAPS_LOCK = 58; // Caps Lock
    public static final int KEY_R = 19;        // R键
    public static final int KEY_TAB = 15;       // Tab键

    // 获取按键名称的方法
    public static String getKeyName(int keyCode) {
        switch (keyCode) {
            case KEY_I: return "I";
            case KEY_F: return "F";
            case KEY_LEFT_SHIFT: return "左Shift";
            case KEY_LEFT_CONTROL: return "左Ctrl";
            case KEY_CAPS_LOCK: return "Caps Lock";
            case KEY_R: return "R";
            case KEY_TAB: return "Tab";
            default:
                // 尝试获取其他常见按键名称
                if (keyCode >= 2 && keyCode <= 11) {
                    return "F" + (keyCode - 1); // F1-F10
                } else if (keyCode == 59) {
                    return "F11";
                } else if (keyCode == 60) {
                    return "F12";
                } else if (keyCode >= 16 && keyCode <= 25) {
                    return String.valueOf((char)('Q' + (keyCode - 16))); // Q-Z
                } else if (keyCode >= 30 && keyCode <= 38) {
                    return String.valueOf((char)('A' + (keyCode - 30))); // A-H
                } else if (keyCode == 39) {
                    return "J";
                } else if (keyCode == 40) {
                    return "K";
                } else if (keyCode == 41) {
                    return "L";
                } else if (keyCode == 44) {
                    return "Z";
                } else if (keyCode == 45) {
                    return "X";
                } else if (keyCode == 46) {
                    return "C";
                } else if (keyCode == 47) {
                    return "V";
                } else if (keyCode == 48) {
                    return "B";
                } else if (keyCode == 49) {
                    return "N";
                } else if (keyCode == 50) {
                    return "M";
                } else {
                    return "键" + keyCode;
                }
        }
    }
}