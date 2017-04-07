package allrgb.core;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Algorithm {
    private List<Color> colors;
    private Color[][] canvas;
    private Set<Coordinate> availableCoordinates;
    private Map<Integer, Integer> imageCheckpoints;
    private Random random;
    private final int neighbourhoodHalfWidth = Config.NEIGHBOURHOOD_WIDTH / 2;

    public Algorithm() {
        random = new Random(Config.SEED);

        // create every color once and randomize the order
        colors = new ArrayList<>();
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
        Collections.shuffle(colors, random);

        // temporary place where we work (faster than all that many GetPixel calls)
        canvas = new Color[Config.Image.HEIGHT][Config.Image.WIDTH];
        assert colors.size() == Config.Image.HEIGHT * Config.Image.WIDTH;

        // constantly changing list of available coordinates (empty pixels which have non-empty neighbors)
        availableCoordinates = new HashSet<>();

        // calculate the checkpoints in advance
        imageCheckpoints = IntStream.range(0, Config.Image.AMOUNT).boxed()
                .collect(Collectors.toMap(i -> (i + 1) * colors.size() / Config.Image.AMOUNT - 1, i -> i));
    }

    // calculates how well a color fits at the given coordinates
    private double inverseFitness(Coordinate coordinate, Color color) {
        // get the diffs for each neighbor separately
        List<Double> inverseFitnesses =
                new ArrayList<>(Config.NEIGHBOURHOOD_WIDTH * Config.NEIGHBOURHOOD_WIDTH - 1);
        final int maxY = coordinate.y + neighbourhoodHalfWidth;
        final int maxX = coordinate.x + neighbourhoodHalfWidth;
        for (int y = coordinate.y - neighbourhoodHalfWidth; y <= maxY; y++) {
            if (y < 0 || y >= Config.Image.HEIGHT)
                continue;
            for (int x = coordinate.x - neighbourhoodHalfWidth; x <= maxX; x++) {
                if (x < 0 || x >= Config.Image.WIDTH || canvas[y][x] == null)
                    continue;
                inverseFitnesses.add(Config.Color.DISTANCE.apply(color, canvas[y][x]));
            }
        }

        // average or minimum selection
        if (Config.AVERAGE) {
            double sum = 0;
            for (double d : inverseFitnesses) {
                sum += d;
            }
            return sum / inverseFitnesses.size();
        } else {
            double min = Double.MAX_VALUE;
            for (double d : inverseFitnesses) {
                if (d < min) {
                    min = d;
                }
            }
            return min;
        }
    }

    public void run() {
        // loop through all colors that we want to place
        for (int i = 0; i < colors.size(); i++) {
            Coordinate bestFit;
            if (availableCoordinates.size() == 0) {
                // use the starting point
                bestFit = new Coordinate(Config.Origin.X, Config.Origin.Y);
            } else {
                // find the best place from the list of available coordinates
                // this is the most expensive step
                bestFit = availableCoordinates.iterator().next();
                double minInverseFitness = Double.MAX_VALUE;
                for (Coordinate coordinate : availableCoordinates) {
                    double inverseFitness = inverseFitness(coordinate, colors.get(i));
                    if (inverseFitness < minInverseFitness) {
                        minInverseFitness = inverseFitness;
                        bestFit = coordinate;
                    }
                }
            }
            // put the pixel where it belongs
            assert canvas[bestFit.y][bestFit.x] == null;
            canvas[bestFit.y][bestFit.x] = colors.get(i);

            // adjust the available list
            availableCoordinates.remove(bestFit);
            for (int y = bestFit.y - 1; y <= bestFit.y + 1; y++) {
                if (y == -1 || y == Config.Image.HEIGHT)
                    continue;
                for (int x = bestFit.x - 1; x <= bestFit.x + 1; x++) {
                    if (x == -1 || x == Config.Image.WIDTH || canvas[y][x] != null)
                        continue;
                    availableCoordinates.add(new Coordinate(x, y));
                }
            }

            // save an image
            if (imageCheckpoints.containsKey(i)) {
                System.out.printf("Progress: %2d%%%n", (i + 1) * 100 / colors.size());
                int checkpoint = imageCheckpoints.get(i);
                WritableImage img = new WritableImage(Config.Image.WIDTH, Config.Image.HEIGHT);
                PixelWriter pixelWriter = img.getPixelWriter();
                for (int y = 0; y < Config.Image.HEIGHT; y++) {
                    for (int x = 0; x < Config.Image.WIDTH; x++) {
                        if (canvas[y][x] != null) {
                            pixelWriter.setColor(x, y, canvas[y][x]);
                        }
                    }
                }
                try {
                    ImageIO.write(
                            SwingFXUtils.fromFXImage(img, null),
                            Config.Image.FORMAT,
                            Paths.get(
                                    Config.Image.PATH,
                                    Config.Image.PREFIX + "_" + checkpoint + "." + Config.Image.FORMAT
                            ).toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        assert availableCoordinates.size() == 0;
    }
}
