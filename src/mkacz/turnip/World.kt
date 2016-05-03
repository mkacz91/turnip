package mkacz.turnip

import processing.core.PVector
import java.io.*
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

    fun write(stream: DataOutputStream)
    {
        stream.writeInt(loops.size)
        for (loop in loops)
        {
            stream.writeInt(loop.nodes.count())
            for (position in loop.positions)
            {
                stream.writeFloat(position.x)
                stream.writeFloat(position.y)
            }
        }
    }

    fun read(stream: DataInputStream)
    {
        loops.clear()
        val loopCount = stream.readInt()
        for (i in 1..loopCount)
        {
            var nodeCount = stream.readInt()
            if (nodeCount == 0)
                return
            var node = addLoop(PVector(stream.readFloat(), stream.readFloat())).origin
            for (j in 2..nodeCount)
                node = node.insertSucc(PVector(stream.readFloat(), stream.readFloat()))
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

    val toPred: PVector
        get() = span(position, pred.position)
    val toSucc: PVector
        get() = span(position, succ.position)

    val bisector: PVector
        get() = bisector(toSucc, toPred)

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

class WorldSegment(val start: WorldNode, val end: WorldNode) : WorldItem(), Support
{
    val pred: WorldSegment
        get() = WorldSegment(start.pred, start)
    val succ: WorldSegment
        get() = WorldSegment(end, end.succ)

    val startBound: PVector
        get()
        {
            val u = start.toPred
            val v = start.toSucc
            return if (per(u, v) < 0) bisector(v, u) else lhp(v)
        }

    val endBound: PVector
        get()
        {
            val u = end.toPred
            val v = end.toSucc
            return if (per(u, v) < 0) bisector(v, u) else rhp(u)
        }

    val center: PVector
        get() = mid(start.position, end.position)

    val span: PVector
        get() = span(start.position, end.position)

    val direction: PVector
        get() = span.normalize()

    fun insertNode(position: PVector) = start.insertSucc(position)

    fun distSq(position: PVector) = pointToSegmentDistSq(start.position, end.position, position)

    fun project(position: PVector) = projectToSegment(start.position, end.position, position)

    override fun eval(radius: Float, param: Float) : Support.EvalResult
    {
        val direction = direction
        val position0 = evalBound(startBound, direction, radius)
        val position1 = evalBound(endBound, direction, radius)
        return Support.EvalResult(lerp(position0, position1, param), direction)
    }

    fun eval(param: Float) = lerp(start.position, end.position, param)

    override fun moveBy(translation: PVector)
    {
        start.moveBy(translation)
        end.moveBy(translation)
    }

    override fun encroaches(position: PVector, radius: Float)
        = distSq(position) <= sq(radius) && inBounds(position)

    fun inStartBound(position: PVector) = per(startBound, span(start.position, position)) <= 0

    fun inEndBound(position: PVector) = per(endBound, span(end.position, position)) > 0

    fun inBounds(position: PVector) = inStartBound(position) && inEndBound(position)

    override fun length(radius: Float) : Float
    {
        val direction = span
        var length = length(direction)
        direction.mult(1 / length)
        return length - dot(startBound, direction) + dot(endBound, direction)
    }

    companion object
    {
        fun segmentLoop(nodeLoop: Iterable<WorldNode>)
            = nodeLoop.map { n -> WorldSegment(n, n.succ) }

        fun evalBound(bound: PVector, direction: PVector, radius: Float)
            = sub(bound, mul(dot(bound, direction), direction)).setMag(radius)
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