package mkacz.turnip

import processing.core.PApplet
import processing.core.PVector

fun main(args: Array<String>)
{
    PApplet.main(args + arrayOf("mkacz.turnip.Applet"))
}

class Applet : PApplet()
{
    companion object
    {
        const val NODE_RADIUS = 10.0f
        const val NODE_HOVER_RADIUS = 12.0f
        const val SEGMENT_HOVER_RADIUS = 10.0f;
        const val SEGMENT_INSERT_RADIUS = 120.0f;
        val SELECT_BUTTON = PApplet.LEFT

        fun pickLoop(world: World, position: PVector)
            = world.loops.find { l -> l.contains(position) }

        fun nearestSegment(world: World, position: PVector)
            = world.segments.minBy { s -> s.distSq(position) }

        fun pickNode(world: World, position: PVector, radius: Float)
            = world.nodes.find { n -> n.distSq(position) <= radius * radius }
    }

    //val guy = Guy()
    val world = World()
    var dt = 0.0f
    var insertSegment: WorldSegment? = null

    var hoverItem: WorldItem? = null
    var activeItem: WorldItem? = null
    var showBisectors = true

    override fun settings()
    {
        size(800, 600)
    }

    override fun setup()
    {
        prevMillis = millis()
        mode = Mode.EDIT_WORLD
    }

    var prevMillis = 0
    override fun draw()
    {
        val currentMillis = millis()
        dt = (currentMillis - prevMillis) * 0.001f
        prevMillis = currentMillis

        background(color(252, 216, 210))

        strokeWeight(2.0f)
        noStroke()
        fill(color(229, 117, 99))
        for (loop in world.loops)
        {
            val fillColor = when (loop)
            {
                hoverItem -> color(255, 0, 0)
                activeItem -> color(0, 255, 0)
                else -> color(0, 0, 255)
            }
            fill(fillColor)

            var nodeCount = 0
            beginShape()
            for (position in loop.positions)
            {
                vertex(position)
                ++nodeCount
            }
            endShape(CLOSE)

            if (nodeCount == 2)
            {
                stroke(fillColor)
                line(loop.origin.position, loop.origin.succ.position)
                noStroke()
            }
        }

        if (mode == Mode.EDIT_WORLD)
        {
            ellipseMode(RADIUS)

            if (insertSegment != null)
            {
                val m = vec(mouseX, mouseY)
                strokeWeight(1.0f)
                stroke(128)
                noFill()
                line(m, insertSegment!!.start.position)
                line(m, insertSegment!!.end.position)
                ellipse(m, NODE_RADIUS)
            }

            noStroke()
            if (hoverItem is WorldNode)
            {
                fill(color(255, 0, 0))
                ellipse((hoverItem as WorldNode).position, NODE_RADIUS)
            }
            if (activeItem != hoverItem && activeItem is WorldNode)
            {
                fill(color(0, 255, 0))
                ellipse((activeItem as WorldNode).position, NODE_RADIUS)
            }

            noFill()
            strokeWeight(1.0f)
            stroke(color(87, 34, 77))
            for (node in world.nodes)
                ellipse(node.position, NODE_RADIUS)

            strokeWeight(3.0f)
            if (hoverItem is WorldSegment)
            {
                val segment = hoverItem as WorldSegment
                stroke(0)
                line(segment.start.position, segment.end.position)
            }
            if (activeItem != hoverItem && activeItem is WorldSegment)
            {
                val segment = activeItem as WorldSegment
                stroke(255)
                line(segment.start.position, segment.end.position)
            }
        }

        if (showBisectors)
        {
            strokeWeight(1.0f)
            stroke(color(0))
            for (node in world.nodes)
            {
                val bisector = bisector(
                    span(node.position, node.pred.position),
                    span(node.position, node.succ.position))
                bisector.setMag(70f)
                line(node.position, add(node.position, bisector))
            }
        }
    }

    enum class Mode
    {
        NONE,
        EDIT_WORLD,
        PLAY
    }

    var mode = Mode.NONE
        set(value)
        {
            if (mode == value)
                return
            when (mode)
            {
                Mode.EDIT_WORLD -> { }
                else -> { }
            }
            field = value
        }

    override fun mousePressed()
    {
        val m = vec(mouseX, mouseY)
        if (mode == Mode.EDIT_WORLD && mouseButton == SELECT_BUTTON)
        {
            activeItem = null
            updateHover()
            if (insertSegment != null)
            {
                hoverItem = insertSegment!!.insertNode(m)
                insertSegment = null
            }
            else if (hoverItem == null)
            {
                hoverItem = world.addLoop(m).origin
            }
        }
    }

    override fun mouseReleased()
    {
        if (mode == Mode.EDIT_WORLD && mouseButton == SELECT_BUTTON)
        {
            activeItem = hoverItem
        }
    }

    override fun mouseDragged()
    {
        if (mode == Mode.EDIT_WORLD && mouseButton == SELECT_BUTTON)
        {
            val dm = vec(mouseX - pmouseX, mouseY - pmouseY)
            hoverItem?.moveBy(dm)
        }
    }

    override fun mouseMoved()
    {
        if (mode == Mode.EDIT_WORLD)
            updateHover()
    }

    fun updateHover()
    {
        val m = vec(mouseX, mouseY)
        hoverItem = null
        insertSegment = null

        val node = pickNode(world, m, NODE_HOVER_RADIUS)
        if (node != null)
        {
            hoverItem = node
            return
        }

        val segment = nearestSegment(world, m)
        val segmentDistSq = segment?.distSq(m) ?: Float.POSITIVE_INFINITY
        if (segmentDistSq <= SEGMENT_HOVER_RADIUS * SEGMENT_HOVER_RADIUS)
        {
            hoverItem = segment
            return
        }

        val loop = pickLoop(world, m)
        if (loop != null)
        {
            hoverItem = loop
            return
        }

        if (segmentDistSq <= SEGMENT_INSERT_RADIUS * SEGMENT_INSERT_RADIUS)
        {
            insertSegment = segment
            return
        }
    }

    fun line(p0: PVector, p1: PVector) { line(p0.x, p0.y, p1.x, p1.y); }

    fun ellipse(p: PVector, r: Float) { ellipse(p.x, p.y, r, r); }

    fun vertex(p: PVector) { vertex(p.x, p.y); }
}