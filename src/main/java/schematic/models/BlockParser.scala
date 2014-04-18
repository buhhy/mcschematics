package schematic.models

import schematic.common.CachedObject
import schematic.models.blocktypes.redstone.{RedstoneComparator, JavaRedstoneBlockAdapter}
import schematic.models.blocktypes.Block

/**
 * @author tlei (Terence Lei)
 */
object BlockParser extends CachedObject[BlockParser] {
    override protected def create: BlockParser = new BlockParser()
    override def apply(): BlockParser = super.apply()       // silly Java
}

class BlockParser {
    def toBlock(blockId: Short, data: Byte): Block = {
        blockId match {
            case 149 => new JavaRedstoneBlockAdapter(blockId, RedstoneComparator(data))
            case _ => Block.getInstance(blockId, data)
        }
    }
}
