package schematic.models.images

import java.awt.image.BufferedImage
import java.awt.Color

case class ARGBColor(a: Int, r: Int, g: Int, b: Int) {
    def toColor = new Color(r, g, b, a)
    def toHex = a << 24 | r << 16 | g << 8 | b
    def max = Math.max(Math.max(Math.max(a, r), g), b)
    def *(multiple: Float) =
        copy(
            r = Math.round(r * multiple),
            g = Math.round(g * multiple),
            b = Math.round(b * multiple))
}

object ARGBColor {
    def apply(hex: Int): ARGBColor = {
        val a = (hex & 0xff000000) >>> 24
        val r = (hex & 0x00ff0000) >>> 16
        val g = (hex & 0x0000ff00) >>> 8
        val b = hex & 0x000000ff

        ARGBColor(a, r, g, b)
    }

    def apply(color: Color): ARGBColor =
        ARGBColor(color.getAlpha, color.getRed, color.getGreen, color.getBlue)
}

/**
 * Converts all mask colors in the image to the specified color.
 * @author tlei (Terence Lei)
 */
object BufferedImageColorFilter {
    final val REPLACE_COLOR = new Color(255, 0, 255)

    private final val COMPARATORS = Seq(
        (c: ARGBColor) => c.r == 0xff && c.b == 0xff && c.g < c.r,
        (c: ARGBColor) => c.g == 0xff && c.b == 0xff && c.r < c.g)

    private def map(image: BufferedImage)
            (func: ARGBColor => Option[ARGBColor]) = {
        for {
            y <- 0 to image.getHeight - 1
            x <- 0 to image.getWidth - 1
        } {
            func(ARGBColor(image.getRGB(x, y))) match {
                case Some(c) => image.setRGB(x, y, c.toHex)
                case None =>
            }
        }
        image
    }

    def processImage(image: BufferedImage, newColors: Seq[Color]) = {
        val replaceConditions = newColors.map(ARGBColor.apply).zip(COMPARATORS)
        map(image) { (c: ARGBColor) =>
            // if the color is any shade of pink, cyan, or yellow, replace it providing a
            // replacement exists
            replaceConditions.find(_._2(c)).map {
                case (replacement, _) =>
                    val shade = c.max.toFloat / 0xff
                    replacement * shade
            }
        }
    }
}
