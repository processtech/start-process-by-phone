package ru.evgeny.echo.sipbot.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommonUtil {

    public static List<byte[]> split(byte[] source, int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Chunk size must be positive.");
        }
        List<byte[]> result = new ArrayList<>();
        for (int from = 0; from < source.length; from += chunkSize) {
            int to = Math.min(from + chunkSize, source.length);
            byte[] chunk = Arrays.copyOfRange(source, from, to);

            result.add(chunk);
        }
        return result;
    }
}
