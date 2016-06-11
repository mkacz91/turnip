package mkacz.turnip

import processing.core.PVector
import java.io.*
import java.util.*

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

    val supports: Iterable<Support>
        get() = loops.flatMap { it.supports }

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

class WorldNode(var position: PVector) : WorldItem(), Support
{
    var pred = this
    var succ = this

    val predSegment: WorldSegment
        get() = WorldSegment(pred, this)
    val succSegment: WorldSegment
        get() = WorldSegment(this, succ)

    val toPred: PVector
        get() = span(position, pred.position)
    val toSucc: PVector
        get() = span(position, succ.position)

    override val predSupport: Support
        get() = predSegment
    override val succSupport: Support
        get() = succSegment

    val bisector: PVector
        get() = bisector(toSucc, toPred)

    val startBound: PVector
        get() = rhn(toPred)
    val endBound: PVector
        get() = lhn(toSucc)

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

    fun inBounds(position: PVector) : Boolean
    {
        val u = span(this.position, position)
        return per(startBound, u) >= 0f && per(endBound, u) < 0f
    }

    override fun encroaches(position: PVector, radius: Float)
        = distSq(position) < sq(radius) && inBounds(position)

    override fun length(radius: Float) = unitAngle(startBound, endBound) * radius

    override fun activate(radius: Float) = object : ActiveSupport(this, radius)
    {
        val startBound = this@WorldNode.startBound
        val alpha = unitAngle(startBound, endBound)
        val origin = this@WorldNode.position
        override fun projectParam(position: PVector)
            = unitAngle(startBound, dir(origin, position)) / alpha
        override fun eval(param: Float) : SupportEval
        {
            val u = rot(startBound, -alpha * param)
            return SupportEval(add(origin, mul(radius, u)), rhp(u))
        }
    }
}

class WorldSegment(val start: WorldNode, val end: WorldNode) : WorldItem(), Support
{
    val pred: WorldSegment
        get() = WorldSegment(start.pred, start)
    val succ: WorldSegment
        get() = WorldSegment(end, end.succ)
    override val predSupport: Support
        get() = if (per(start.toPred, span) > 0f) start else pred
    override val succSupport: Support
        get() = if (per(end.toSucc, span) > 0f) end else succ

    val startBound: PVector
        get()
        {
            val u = start.toPred
            val v = start.toSucc
            return if (per(u, v) < 0) bisector(v, u).normalize() else lhp(v).normalize()
        }

    val endBound: PVector
        get()
        {
            val u = end.toPred
            val v = end.toSucc
            return if (per(u, v) < 0) bisector(v, u).normalize() else rhp(u).normalize()
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

    fun eval(param: Float) = lerp(start.position, end.position, param)

    override fun moveBy(translation: PVector)
    {
        start.moveBy(translation)
        end.moveBy(translation)
    }

    override fun encroaches(position: PVector, radius: Float)
        = distSq(position) <= sq(radius) && inBounds(position)

    override fun activate(radius: Float) = object : ActiveSupport(this, radius)
    {
        val direction = this@WorldSegment.direction
        val start = add(this@WorldSegment.start.position, evalBound(startBound, direction, radius))
        val end = add(this@WorldSegment.end.position, evalBound(endBound, direction, radius))
        override fun eval(param: Float) = SupportEval(lerp(start, end, param), direction)
        override fun projectParam(position: PVector) = projectToSegmentParam(start, end, position)
    }

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
            = mul(radius / abs(per(bound, direction)), bound)
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

    val supports = object : Iterable<Support>
    {
        override operator fun iterator() = object : Iterator<Support>
        {
            var support: Support = DummySupport(origin.succSupport)
            val last = origin.succSupport.predSupport
            override operator fun hasNext()
                = support != last && (support as? WorldSegment)?.end != origin
            override operator fun next() : Support { support = support.succSupport; return support }
        }
    }

    fun contains(position: PVector) = pointInPolygon(positions, position)

    override fun moveBy(translation: PVector)
    {
        for (node in nodes)
            node.moveBy(translation)
    }
}