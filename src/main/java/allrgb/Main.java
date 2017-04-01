package allrgb;

import allrgb.core.Config;
import allrgb.core.Coordinate;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Main {
    // gets the difference between two colors
    static double colorDistance(Color c1, Color c2) {
        return (c1.getRed() - c2.getRed()) * (c1.getRed() - c2.getRed())
                + (c1.getGreen() - c2.getGreen()) * (c1.getGreen() - c2.getGreen())
                + (c1.getBlue() - c2.getBlue()) * (c1.getBlue() - c2.getBlue());
    }

    // gets the neighbors (3..8) of the given coordinate
    static List<Coordinate> getNeighbours(Coordinate coordinate) {
        List<Coordinate> neighbours = new ArrayList<>();
        for (int y = coordinate.y - 1; y <= coordinate.y + 1; y++) {
            if (y == -1 || y == Config.Image.HEIGHT)
                continue;
            for (int x = coordinate.x - 1; x <= coordinate.x + 1; x++) {
                if (x == -1 || x == Config.Image.WIDTH)
                    continue;
                neighbours.add(new Coordinate(x, y));
            }
        }
        return neighbours;
    }

    // calculates how well a color fits at the given coordinates
    static double inverseFitness(Color[][] pixels, Coordinate coordinate, Color color) {
        // get the diffs for each neighbor separately
        List<Double> inverseFitnesses = new ArrayList<>(8);
        for (Coordinate neighbour : getNeighbours(coordinate)) {
            Color pixel = pixels[neighbour.y][neighbour.x];
            if (pixel != null) {
                inverseFitnesses.add(colorDistance(color, pixel));
            }
        }

        // average or minimum selection
        if (Config.AVERAGE)
            return inverseFitnesses.stream().mapToDouble(Double::doubleValue).average().orElse(1);
        else
            return inverseFitnesses.stream().mapToDouble(Double::doubleValue).min().orElse(1);
    }

    public static void main(String[] args) throws IOException {
        Config.load(Arrays.stream(args).findFirst().orElse(""));

        // create every color once and randomize the order
        List<Color> colors = new ArrayList<>();
        for (int r = 0; r < Config.Color.DEPTH; r++) {
            double red = (double) r / Config.Color.DEPTH;
            for (int g = 0; g < Config.Color.DEPTH; g++) {
                double green = (double) g / Config.Color.DEPTH;
                for (int b = 0; b < Config.Color.DEPTH; b++) {
                    double blue = (double) b / Config.Color.DEPTH;
                    colors.add(Color.color(red, green, blue));
                }
            }
        }
        Collections.shuffle(colors);

        // temporary place where we work (faster than all that many GetPixel calls)
        Color[][] pixels = new Color[Config.Image.HEIGHT][Config.Image.WIDTH];
        assert colors.size() == Config.Image.HEIGHT * Config.Image.WIDTH;

        // constantly changing list of available coordinates (empty pixels which have non-empty neighbors)
        Set<Coordinate> availableCoordinates = new HashSet<>();

        // calculate the checkpoints in advance
        Map<Integer, Integer> checkpoints =
                IntStream.range(0, Config.Image.AMOUNT)
                        .boxed()
                        .collect(Collectors.toMap(i -> (i + 1) * colors.size() / Config.Image.AMOUNT - 1, i -> i));

        // loop through all colors that we want to place
        for (int i = 0; i < colors.size(); i++) {
            Coordinate bestFit;
            if (availableCoordinates.size() == 0) {
                // use the starting point
                bestFit = new Coordinate(Config.Origin.X, Config.Origin.Y);
            } else {
                // find the best place from the list of available coordinates
                // uses parallel processing, this is the most expensive step
                int finalI = i;
                bestFit = availableCoordinates
                        .parallelStream()
                        .sorted(
                                (c1, c2) -> (int) Math.signum(inverseFitness(pixels, c1, colors.get(finalI))
                                        - inverseFitness(pixels, c2, colors.get(finalI))))
                        .findFirst().orElse(null);
            }
            // put the pixel where it belongs
            assert pixels[bestFit.y][bestFit.x] == null;
            pixels[bestFit.y][bestFit.x] = colors.get(i);

            // adjust the available list
            availableCoordinates.remove(bestFit);
            for (Coordinate neighbour : getNeighbours(bestFit)) {
                if (pixels[neighbour.y][neighbour.x] == null) {
                    availableCoordinates.add(neighbour);
                }
            }

            // save a checkpoint
            if (checkpoints.containsKey(i)) {
                System.out.printf("Progress: %2d%%%n", (i + 1) * 100 / colors.size());
                int checkpoint = checkpoints.get(i);
                WritableImage img = new WritableImage(Config.Image.WIDTH, Config.Image.HEIGHT);
                PixelWriter pixelWriter = img.getPixelWriter();
                for (int y = 0; y < Config.Image.HEIGHT; y++) {
                    for (int x = 0; x < Config.Image.WIDTH; x++) {
                        if (pixels[y][x] != null) {
                            pixelWriter.setColor(x, y, pixels[y][x]);
                        }
                    }
                }
                ImageIO.write(
                        SwingFXUtils.fromFXImage(img, null),
                        "png",
                        new File("build/result_" + checkpoint + ".png"));

            }
        }

        assert availableCoordinates.size() == 0;
    }
}
