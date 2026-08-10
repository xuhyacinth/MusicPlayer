package com.xu.music.player.lyric;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * LRC 时间标签解析。
 */
public final class LrcParser {

    private static final Pattern LINE = Pattern.compile("^\\[(\\d+):(\\d+(?:\\.\\d+)?)\\](.*)$");

    private LrcParser() {
    }

    public static List<LrcLine> parse(List<String> source) {
        return source.stream()
                .map(LrcParser::parseLine)
                .flatMap(Optional::stream)
                .sorted(Comparator.comparingDouble(LrcLine::seconds))
                .toList();
    }

    private static Optional<LrcLine> parseLine(String value) {
        var matcher = LINE.matcher(value);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            var seconds = Double.parseDouble(matcher.group(1)) * 60
                    + Double.parseDouble(matcher.group(2));
            var tag = value.substring(0, value.indexOf(']') + 1);
            return Optional.of(new LrcLine(seconds, tag, matcher.group(3)));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }
}
