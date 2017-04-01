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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Algorithm {
    private List<Color> colors;
    private Color[][] canvas;
    private Set<Coordinate> availableCoordinates;
    private Map<Integer, Integer> imageCheckpoints;

    public Algorithm() {
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
        Collections.shuffle(colors);

        // temporary place where we work (faster than all that many GetPixel calls)
        canvas = new Color[Config.Image.HEIGHT][Config.Image.WIDTH];
        assert colors.size() == Config.Image.HEIGHT * Config.Image.WIDTH;

        // constantly changing list of available coordinates (empty pixels which have non-empty neighbors)
        availableCoordinates = new HashSet<>();

        // calculate the checkpoints in advance
        imageCheckpoints = IntStream.range(0, Config.Image.AMOUNT).boxed()
                .collect(Collectors.toMap(i -> (i + 1) * colors.size() / Config.Image.AMOUNT - 1, i -> i));
    }

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

    public void run() {
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
                                (c1, c2) -> (int) Math.signum(inverseFitness(canvas, c1, colors.get(finalI))
                                        - inverseFitness(canvas, c2, colors.get(finalI))))
                        .findFirst().orElse(null);
            }
            // put the pixel where it belongs
            assert canvas[bestFit.y][bestFit.x] == null;
            canvas[bestFit.y][bestFit.x] = colors.get(i);

            // adjust the available list
            availableCoordinates.remove(bestFit);
            for (Coordinate neighbour : getNeighbours(bestFit)) {
                if (canvas[neighbour.y][neighbour.x] == null) {
                    availableCoordinates.add(neighbour);
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
