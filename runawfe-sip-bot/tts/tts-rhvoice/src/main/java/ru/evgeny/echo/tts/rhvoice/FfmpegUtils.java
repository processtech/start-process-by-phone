package ru.evgeny.echo.tts.rhvoice;

import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;

public class FfmpegUtils {

    @SneakyThrows
    public static byte[] convertToPCM(byte[] inputAudio, String inputFormat) {
        Path inputFile = Files.createTempFile("input", "." + inputFormat);
        Path outputFile = Files.createTempFile("output", ".pcm");

        Files.write(inputFile, inputAudio);

        // ffmpeg -i test.wav -ar 8000 -ac 1 -f s16le test.pcm
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-i", inputFile.toString(),
                "-ar", "8000",
                "-ac", "1",
                "-f", "s16le",
                "-y",
                outputFile.toString()
        );

        Process process = pb.start();
        int exitCode = process.waitFor();

        Files.delete(inputFile);

        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code " + exitCode);
        }

        byte[] webmData = Files.readAllBytes(outputFile);
        Files.delete(outputFile);

        return webmData;
    }
}
