package com.xu.music.player.test;

public class SplitExample {
    public static void main(String[] args) {
        String input = "[00:31.82]陪着我等于死去了无情趣";

        // 使用正则表达式分割字符串
        String[] parts = input.split("(?<=\\])", 2);

        if (parts.length == 2) {
            String timestamp = parts[0];
            // "[00:31.82]"
            String text = parts[1];
            // "陪着我等于死去了无情趣"

            System.out.println("时间戳: " + timestamp);
            System.out.println("文本: " + text);
        } else {
            System.out.println("输入格式不正确");
        }
    }
}
