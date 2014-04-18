package schematic.models.images

import org.apache.commons.io.{FilenameUtils, FileUtils}
import java.io.File
import scala.collection.JavaConverters._
import javax.imageio.ImageIO
import schematic.models.blocktypes.redstone._
import java.awt.image.BufferedImage
import java.awt._
import schematic.common.CachedObject
import schematic.models.blocktypes.redstone.RedstoneComparator.{Compare, Subtract}
import java.awt.RenderingHints._
import schematic.models.blocktypes.redstone.RedstoneWire
import schematic.models.blocktypes.redstone.RedstoneTorch
import scala.Some
import schematic.models.blocktypes.redstone.RedstoneRepeater

object RedstoneImageProvider extends CachedObject[RedstoneImageProvider] {
    final val REDSTONE_IMAGES_DIRECTORY = s"${ImageProvider.IMAGE_DIRECTORY}/redstone/"
    final val IMAGE_EXTENSIONS = Array("png", "jpg")

    // image resources are 128x128 by default
    final val DEFAULT_IMAGE_SIZE = new Dimension(128, 128)

    final val COLOR_OFF_STATE = new Color(0x560600)
    final val COLOR_ON_STATE = new Color(0xff0003)

    override protected def create: RedstoneImageProvider = new RedstoneImageProvider()
    override def apply(): RedstoneImageProvider = super.apply()     // Java shenanigans

    def createBlankImage() =
        new BufferedImage(
            DEFAULT_IMAGE_SIZE.width, DEFAULT_IMAGE_SIZE.height, BufferedImage.TYPE_4BYTE_ABGR)

    private def interpolate(start: Color, end: Color, amount: Float) = {
        def in(c1: Int, c2: Int) = c1 + Math.round(amount * (c2 - c1))

        new Color(
            in(start.getRed, end.getRed),
            in(start.getGreen, end.getGreen),
            in(start.getBlue, end.getBlue),
            in(start.getAlpha, end.getAlpha))
    }

    private def withGraphics(img: BufferedImage)(func: Graphics2D => Unit) {
        val g2d = img.createGraphics()
        g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY)
        func(g2d)
        g2d.dispose()
    }

    private def drawDirImage(dir: CardinalDirection, dest: BufferedImage, source: Image) =
        withGraphics(dest) { g2d =>
            val rotate = dir match {
                case North => Math.toRadians(180)
                case South => 0.0
                case East => Math.toRadians(-90)
                case West => Math.toRadians(90)
            }

            val tempG2d = g2d.create().asInstanceOf[Graphics2D]

            if (rotate != 0.0)
                tempG2d.rotate(rotate, dest.getWidth(null) / 2.0, dest.getHeight(null) / 2.0)

            tempG2d.drawImage(source, 0, 0, dest.getWidth(null), dest.getHeight(null), null)
            tempG2d.dispose()
        }

    private def drawCenteredFont(str: String, dest: BufferedImage) =
        withGraphics(dest) { g2d =>
            val fontSize = (DEFAULT_IMAGE_SIZE.getHeight * 0.4).toFloat
            val newFont = g2d.getFont.deriveFont(fontSize)

            g2d.setFont(newFont)
            g2d.setColor(BufferedImageColorFilter.REPLACE_COLOR)

            val visualBounds = newFont.createGlyphVector(g2d.getFontRenderContext, str)
                .getVisualBounds.getBounds
            val textBounds = g2d.getFontMetrics.getStringBounds(str, g2d).getBounds
            val xpos = Math.round((DEFAULT_IMAGE_SIZE.width - textBounds.width) * 0.5f)
            val ypos = Math.round(
                (DEFAULT_IMAGE_SIZE.height - visualBounds.height) * 0.5f - visualBounds.y)

            g2d.drawString(str, xpos, ypos)
        }
}

sealed class RedstoneImageProvider {
    import RedstoneImageProvider._
    import RenderingHints._

    private val redstoneImages = fetchImagesFromDir(REDSTONE_IMAGES_DIRECTORY)

    private def fetchImagesFromDir(dir: String) = {
        val classLoader = Thread.currentThread().getContextClassLoader
        val directory = new File(classLoader.getResource(dir).toURI)
        val imgListByName = FileUtils.listFiles(directory, IMAGE_EXTENSIONS, true).asScala.collect {
            case imageFile: File =>
                println(s"Loading image '${imageFile.getName}' found in '$dir'")
                FilenameUtils.getBaseName(imageFile.getName) -> ImageIO.read(imageFile)
        }

        imgListByName.toMap
    }

    private def getImageByName(str: String) = {
        redstoneImages.get(str).getOrElse {
            throw new IllegalArgumentException(s"Image named '$str' not found")
        }
    }

    def getImage(block: Redstone) = {
        val newImage = RedstoneImageProvider.createBlankImage()
        val g2d = newImage.createGraphics()

        def drawImage(img: Image) =
            g2d.drawImage(img, 0, 0, newImage.getWidth, newImage.getHeight, null)

        g2d.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
        g2d.setRenderingHint(KEY_ALPHA_INTERPOLATION, VALUE_ALPHA_INTERPOLATION_QUALITY)

        // draw image onto new image
        block match {
            case RedstoneWire(_, dirs) =>
                val hub = getImageByName("redstone-wire-hub")
                val line = getImageByName("redstone-wire-line")

                // draw the hub if redstone isn't a line
                dirs.size match {
                    case 2 if dirs == Set(East, West) || dirs == Set(North, South) =>
                    case _ => drawImage(hub)
                }

                dirs.foreach(drawDirImage(_, newImage, line))

            case RedstoneRepeater(_, dir, delay) =>
                drawDirImage(dir, newImage, getImageByName("redstone-repeater"))
                drawCenteredFont((delay + 1).toString, newImage)

            case RedstoneTorch(_, Some(dir)) =>
                drawDirImage(dir, newImage, getImageByName("redstone-torch-horizontal"))

            case RedstoneTorch(_, None) =>
                drawImage(getImageByName("redstone-torch-vertical"))

            case RedstoneComparator(_, dir, state) =>
                drawDirImage(dir, newImage, getImageByName("redstone-comparator"))
                state match {
                    case Compare => drawCenteredFont(">", newImage)
                    case Subtract => drawCenteredFont("-", newImage)
                }
        }

        val secondaryColors = block match {
            case RedstoneComparator(_, dir, Compare) => Seq(COLOR_OFF_STATE)
            case RedstoneComparator(_, dir, Subtract) => Seq(COLOR_ON_STATE)
            case _ => Nil
        }

        // tint the image based on strength
        val percent = block.strength.toFloat / Redstone.MAX_STRENGTH
        val color = interpolate(COLOR_OFF_STATE, COLOR_ON_STATE, percent)
        g2d.dispose()

        BufferedImageColorFilter.processImage(newImage, color +: secondaryColors)
    }
}
