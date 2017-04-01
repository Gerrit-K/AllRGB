package allrgb;

import allrgb.core.Algorithm;
import allrgb.core.Config;

import java.io.IOException;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
        Config.load(Arrays.stream(args).findFirst().orElse(""));
        new Algorithm().run();
    }
}
