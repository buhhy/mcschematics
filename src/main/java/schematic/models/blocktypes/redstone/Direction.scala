package schematic.models.blocktypes.redstone

sealed trait Direction

sealed trait VerticalDirection extends Direction

sealed trait CardinalDirection extends Direction

object CardinalDirection {
    def fromByteWithNone(byte: Byte): Option[CardinalDirection] = {
        None
    }

    def fromByte(byte: Byte): CardinalDirection =
        byte match {
            case 0 => North
            case 1 => East
            case 2 => South
            case 3 => West
            case _ => throw new IllegalArgumentException(s"Illegal directional state: $byte")
        }
}

case object Up extends VerticalDirection
case object Down extends VerticalDirection
case object Middle extends VerticalDirection

case object East extends CardinalDirection
case object West extends CardinalDirection
case object North extends CardinalDirection
case object South extends CardinalDirection
