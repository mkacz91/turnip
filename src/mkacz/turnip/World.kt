package mkacz.turnip

import processing.core.PVector
import java.util.*
import kotlin.collections.*
import kotlin.collections.Iterator

class World
{
    val origins = ArrayList<WorldNode>()

    val nodes = object : Iterable<WorldNode> { override operator fun iterator() = object : Iterator<WorldNode>
    {
        var origIt = origins.iterator()
        var loopIt = Collections.emptyIterator<WorldNode>()
        override operator fun hasNext() = loopIt.hasNext() || origIt.hasNext()
        override operator fun next() : WorldNode
        {
            if (!loopIt.hasNext())
                loopIt = origIt.next().nodeLoop.iterator()
            return loopIt.next()
        }
    }}

    fun addOrigin(position: PVector) : WorldNode
    {
        val origin = WorldNode(position)
        origins.add(origin)
        return origin
    }

    fun pickNode(position: PVector, radius: Float)
        = nodes.find { n -> distSq(n.position, position) <= radius * radius }
}

class WorldNode(var position: PVector)
{
    var pred: WorldNode = this
    var succ: WorldNode = this

    constructor(position: PVector, pred: WorldNode, succ: WorldNode) : this(position)
    {
        this.pred = pred
        this.succ = succ
    }

    private constructor(succ: WorldNode) : this(PVector(), succ, succ) { }

    val nodeLoop = object : Iterable<WorldNode> { override operator fun iterator() = object : Iterator<WorldNode> {
        val last = pred
        var node = WorldNode(this@WorldNode)
        override operator fun hasNext() = node != last
        override operator fun next() : WorldNode { node = node.succ; return node }
    }}

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

    fun insertAdaptive(position: PVector) : WorldNode
    {
        if (distSq(position, pred.position) < distSq(position, succ.position))
            return insertPred(position)
        else
            return insertSucc(position)
    }
}