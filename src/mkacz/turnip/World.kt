package mkacz.turnip

import processing.core.PVector
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.*
import kotlin.collections.*
import kotlin.collections.Iterator

class World
{
    val loops = ArrayList<WorldLoop>()

    val nodes = object : Iterable<WorldNode>
    {
        override operator fun iterator() = object : Iterator<WorldNode>
        {
            var loopIt = loops.iterator()
            var nodeIt = Collections.emptyIterator<WorldNode>()
            override operator fun hasNext() = nodeIt.hasNext() || loopIt.hasNext()
            override operator fun next() : WorldNode
            {
                if (!nodeIt.hasNext())
                    nodeIt = loopIt.next().nodes.iterator()
                return nodeIt.next()
            }
        }
    }

    val segments : Iterable<WorldSegment>
        get() = WorldSegment.segmentLoop(nodes)

    fun addLoop(position: PVector) : WorldLoop
    {
        val loop = WorldLoop(WorldNode(position))
        loops.add(loop)
        return loop
    }

    fun write(stream: OutputStream)
    {
        val writer = stream.bufferedWriter()
        for (loop in loops)
        {
            for (position in loop.positions)
            {
                writer.write(position.toString())
                writer.append(' ')
            }
            writer.appendln()
        }
    }
}

abstract class WorldItem()
{
    abstract fun moveBy(translation: PVector)
}

class WorldNode(var position: PVector) : WorldItem()
{
    var pred: WorldNode = this
    var succ: WorldNode = this

    constructor(position: PVector, pred: WorldNode, succ: WorldNode) : this(position)
    {
        this.pred = pred
        this.succ = succ
    }

    private constructor(succ: WorldNode) : this(PVector(), succ, succ) { }

    val nodeLoop = object : Iterable<WorldNode>
    {
        override operator fun iterator() = object : Iterator<WorldNode>
        {
            val last = pred
            var node = WorldNode(this@WorldNode)
            override operator fun hasNext() = node != last
            override operator fun next() : WorldNode { node = node.succ; return node }
        }
    }

    val segmentLoop: Iterable<WorldSegment>
        get() = WorldSegment.segmentLoop(nodeLoop)

    val positionLoop: Iterable<PVector>
        get() = nodeLoop.map { n -> n.position }

    fun insertPred(position: PVector) : WorldNode
    {
        val node = WorldNode(position, pred, this)
        pred.succ = node
        pred = node
        return node
    }

    fun insertSucc(position: PVector) : WorldNode
    {
        val node = WorldNode(position, this, succ)
        succ.pred = node
        succ = node
        return node
    }

    fun distSq(position: PVector) = distSq(this.position, position)

    override fun moveBy(translation: PVector)
    {
        position.add(translation)
    }
}

class WorldSegment(val start: WorldNode, val end: WorldNode) : WorldItem()
{
    fun insertNode(position: PVector) = start.insertSucc(position)

    fun distSq(position: PVector) = pointToSegmentDistSq(start.position, end.position, position)

    override fun moveBy(translation: PVector)
    {
        start.moveBy(translation)
        end.moveBy(translation)
    }

    companion object
    {
        fun segmentLoop(nodeLoop: Iterable<WorldNode>)
            = nodeLoop.map { n -> WorldSegment(n, n.succ) }
    }
}

class WorldLoop(val origin: WorldNode) : WorldItem()
{
    val nodes: Iterable<WorldNode>
        get() = origin.nodeLoop

    val segments: Iterable<WorldSegment>
        get() = origin.segmentLoop

    val positions: Iterable<PVector>
        get() = origin.positionLoop

    fun contains(position: PVector) = pointInPolygon(positions, position)

    override fun moveBy(translation: PVector)
    {
        for (node in nodes)
            node.moveBy(translation)
    }
}