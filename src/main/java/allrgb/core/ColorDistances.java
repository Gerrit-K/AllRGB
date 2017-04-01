package allrgb.core;

import javafx.scene.paint.Color;

import java.util.function.BiFunction;

public interface ColorDistances {
    BiFunction<Color, Color, Double> SQUARED_EUCLIDEAN =
            (c1, c2) -> (c1.getRed() - c2.getRed()) * (c1.getRed() - c2.getRed())
                    + (c1.getGreen() - c2.getGreen()) * (c1.getGreen() - c2.getGreen())
                    + (c1.getBlue() - c2.getBlue()) * (c1.getBlue() - c2.getBlue());

    BiFunction<Color, Color, Double> SQUARED_HUE_DIFFERENCE =
            (c1, c2) -> (c1.getHue() - c2.getHue()) * (c1.getHue() - c2.getHue());

    BiFunction<Color, Color, Double> SQUARED_BRIGHTNESS_DIFFERENCE =
            (c1, c2) -> (c1.getBrightness() - c2.getBrightness()) * (c1.getBrightness() - c2.getBrightness());
}
