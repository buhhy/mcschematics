package schematic.models.blocktypes.redstone

import schematic.models.blocktypes.{Block, DirectionalBlock}
import java.awt.image.BufferedImage
import schematic.models.images.{ImageProvider, RedstoneImageProvider}
import schematic.models.blocktypes.redstone.RedstoneComparator.{Compare, Subtract}

trait Redstone {
    val name: String
    val tooltip: String
    val strength: Short
}

trait RedstoneEntity extends Redstone {
    val isOn: Boolean
    override val strength: Short = if (isOn) Redstone.MAX_STRENGTH else 0
}

case class RedstoneWire(
    override val strength: Short,
    directions: Set[CardinalDirection]
) extends Redstone {
    override val name: String = "Redstone Wire"
    override val tooltip: String = s"$name - strength: $strength"
}

case class RedstoneRepeater(
    override val isOn: Boolean,
    direction: CardinalDirection,
    delay: Short
) extends RedstoneEntity {
    override val name: String = "Redstone Repeater"
    override val tooltip: String = s"$name - on: $isOn, delay: $delay"
}

case class RedstoneTorch(
    override val isOn: Boolean,
    direction: Option[CardinalDirection]
) extends RedstoneEntity {
    override val name: String = "Redstone Torch"
    override val tooltip: String = s"$name - on: $isOn"
}

case class RedstoneComparator(
    override val isOn: Boolean,
    direction: CardinalDirection,
    state: RedstoneComparator.State
) extends RedstoneEntity {
    override val name: String = "Redstone Comparator"
    override val tooltip: String = s"$name - on: $isOn, delay: ${state.str}"
}

object Redstone {
    val MAX_STRENGTH: Byte = 0xF

    import DirectionalBlock.Direction._

    def javaWire(dir: DirectionalBlock.Direction, isLine: Boolean, strength: Byte) = {
        val dirs: Set[CardinalDirection] = if (isLine) {
            dir match {
                case N | S => Set(North, South)
                case E | W => Set(East, West)
                case _ => throw new AssertionError(dir)
            }
        } else {
            dir match {
                case N => Set(East, South, West)
                case S => Set(West, North, East)
                case E => Set(South, West, North)
                case W => Set(North, East, South)
                case NW => Set(East, South)
                case NE => Set(South, West)
                case SW => Set(North, East)
                case SE => Set(West, North)
                case NONE => Set(North, East, South, West)
                case _ => throw new AssertionError(dir)
            }
        }

        RedstoneWire(strength, dirs)
    }

    def javaRepeater(dir: DirectionalBlock.Direction, isOn: Boolean, delay: Short) = {
        val newDir: CardinalDirection = dir match {
            case N => North
            case S => South
            case E => East
            case W => West
            case _ => throw new AssertionError(dir)
        }
        RedstoneRepeater(isOn, newDir, delay)
    }

    def javaTorch(dir: DirectionalBlock.Direction, isOn: Boolean) = {
        val newDir: Option[CardinalDirection] = dir match {
            case N => Some(North)
            case S => Some(South)
            case E => Some(East)
            case W => Some(West)
            case NONE => None
            case _ => throw new AssertionError(dir)
        }
        RedstoneTorch(isOn, newDir)
    }
}

object RedstoneComparator {
    trait State {
        val str: String
    }
    case object Compare extends State {
        override val str: String = "compare"
    }
    case object Subtract extends State {
        override val str: String = "subtract"
    }
    
    def apply(data: Byte): RedstoneComparator = {
        val mode = if (((data >>> 2) & 0x1) == 1) Subtract else Compare
        val dir = CardinalDirection.fromByte((data & 0x3).toByte)
        val isOn = if (((data >>> 3) & 0x1) == 1) true else false

        RedstoneComparator(isOn, dir, mode)
    }
}

class JavaRedstoneBlockAdapter(id: Short, model: Redstone) extends Block(id) {
    override def getToolTipText: String = model.tooltip
    override def toString: String = model.toString
    override def getImage(zoom: Float): BufferedImage =
        ImageProvider.zoom(zoom, RedstoneImageProvider().getImage(model))
}
