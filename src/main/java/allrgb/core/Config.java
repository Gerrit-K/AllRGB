package allrgb.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.function.BiFunction;

public final class Config {
    public static boolean AVERAGE;
    public static long SEED;
    public static int NEIGHBOURHOOD_WIDTH;

    public static final class Color {
        public static int DEPTH;
        public static BiFunction<javafx.scene.paint.Color, javafx.scene.paint.Color, Double> DISTANCE;
    }

    public static final class Image {
        public static int AMOUNT;
        public static int WIDTH;
        public static int HEIGHT;
        public static String PATH;
        public static String PREFIX;
        public static String FORMAT;
    }

    public static final class Origin {
        public static int X;
        public static int Y;
    }

    public static void load(String file) {
        Properties properties = new Properties();

        // property precedence:
        // 1 (lowest): default properties
        try (InputStream inputStream = Config.class.getClassLoader().getResourceAsStream("default.properties")) {
            properties.load(inputStream);
        } catch (IOException ignored) {
        }

        // 2: custom file properties
        try (InputStream inputStream = new FileInputStream(file)) {
            properties.load(inputStream);
        } catch (IOException ignored) {
        }

        // 3: system properties (provided as JVM arguments with -D)
        properties.putAll(System.getProperties());

        AVERAGE = Boolean.parseBoolean(properties.getProperty("allrgb.average"));
        SEED = Long.parseLong(properties.getProperty("allrgb.seed"));
        NEIGHBOURHOOD_WIDTH = Integer.parseInt(properties.getProperty("allrgb.neighbourhood_width"));
        Color.DEPTH = Integer.parseInt(properties.getProperty("allrgb.color.depth"));
        Color.DISTANCE = getColorDistanceByName(properties.getProperty("allrgb.color.distance"));
        Image.AMOUNT = Integer.parseInt(properties.getProperty("allrgb.image.amount"));
        Image.WIDTH = Integer.parseInt(properties.getProperty("allrgb.image.width"));
        Image.HEIGHT = Integer.parseInt(properties.getProperty("allrgb.image.height"));
        Image.PATH = properties.getProperty("allrgb.image.path");
        Image.PREFIX = properties.getProperty("allrgb.image.prefix");
        Image.FORMAT = properties.getProperty("allrgb.image.format");
        Origin.X = Integer.parseInt(properties.getProperty("allrgb.start.x"));
        Origin.Y = Integer.parseInt(properties.getProperty("allrgb.start.y"));
    }

    private static BiFunction<javafx.scene.paint.Color, javafx.scene.paint.Color, Double> getColorDistanceByName(String name) {
        Field[] distanceFields = ColorDistances.class.getDeclaredFields();
        for (Field distanceField : distanceFields) {
            if (distanceField.getName().equalsIgnoreCase(name)) {
                try {
                    @SuppressWarnings("unchecked")
                    BiFunction<javafx.scene.paint.Color, javafx.scene.paint.Color, Double> distance =
                            (BiFunction<javafx.scene.paint.Color, javafx.scene.paint.Color, Double>) distanceField.get(null);
                    return distance;
                } catch (IllegalAccessException ignored) {
                }
            }
        }
        return null;
    }
}
