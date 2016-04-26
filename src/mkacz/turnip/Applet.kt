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
        const val ACTIVE_NODE_RADIUS = 11.0f
        val SELECT_BUTTON = PApplet.LEFT
        val CLEAR_BUTTON = PApplet.RIGHT

        fun pickLoop(world: World, position: PVector)
            = world.loops.find { l -> l.contains(position) }

        fun pickSegment(loop: WorldLoop, position: PVector)
            = loop.segments.minBy { s -> s.distSq(position) }!!

        fun pickNode(world: World, position: PVector, radius: Float) : Pair<WorldLoop, WorldNode>?
        {
            val radiusSq = radius * radius
            for (loop in world.loops)
            {
                for (node in loop.nodes)
                {
                    if (node.distSq(position) <= radiusSq)
                        return Pair(loop, node)
                }
            }
            return null
        }
    }

    val guy = Guy()
    val world = World()
    var dt = 0.0f
    var activeNode: WorldNode? = null
    var activeSegment: WorldSegment? = null
    var activeLoop: WorldLoop? = null

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

        // Draw world

        noStroke()
        fill(color(229, 117, 99))
        for (loop in world.loops)
        {
            fill(if (loop == activeLoop) color(215, 100, 80) else color(229, 117, 99))
            beginShape()
            for (position in loop.positions)
                vertex(position)
            endShape(CLOSE)
        }

        strokeWeight(1.0f)
        stroke(color(87, 34, 77))
        if (mode == Mode.EDIT_WORLD)
        {
            ellipseMode(RADIUS)
            noFill()
            for (node in world.nodes)
            {
                if (node == activeNode)
                {
                    fill(color(168, 86, 72))
                    ellipse(node.position, ACTIVE_NODE_RADIUS)
                    noFill()
                }
                else
                {
                    ellipse(node.position, NODE_RADIUS)
                }
            }
        }

        if (activeSegment != null)
        {
            strokeWeight(3.0f)
            line(activeSegment!!.start.position, activeSegment!!.end.position)
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
                Mode.EDIT_WORLD -> activeNode = null
                else -> { }
            }
            field = value
        }

    override fun mousePressed()
    {
        if (mode == Mode.EDIT_WORLD && mouseButton == SELECT_BUTTON)
        {
            activeSegment = null
            val m = vec(mouseX, mouseY)
            val nl = pickNode(world, m, NODE_RADIUS)
            if (nl != null)
            {
                activeLoop = nl.first
                activeNode = nl.second
                return
            }
            val loop = pickLoop(world, m)
            if (loop != null && loop != activeLoop)
            {
                activeLoop = loop
                return
            }
            if (activeLoop == null)
            {
                activeLoop = world.addLoop(m)
                activeNode = activeLoop!!.origin
                return
            }
            activeNode = pickSegment(activeLoop!!, m).insertNode(m)
        }
        else if (mode == Mode.EDIT_WORLD && mouseButton == CLEAR_BUTTON)
        {
            activeLoop = null
        }
    }

    override fun mouseDragged()
    {
        if (mode == Mode.EDIT_WORLD && mouseButton == SELECT_BUTTON)
        {
            activeNode?.position?.set(mouseX.toFloat(), mouseY.toFloat())
        }
    }

    override fun mouseMoved()
    {
        if (mode == Mode.EDIT_WORLD)
        {
            activeNode = null
            activeSegment = null
            val m = vec(mouseX, mouseY)
            val nl = pickNode(world, m, NODE_RADIUS)
            if (nl != null)
            {
                activeLoop = nl.first
                activeNode = nl.second
                activeSegment = null
                return
            }
            if (activeLoop == null)
            {
                activeLoop = pickLoop(world, m)
                return
            }
            activeSegment = pickSegment(activeLoop!!, m)
        }
    }

    fun line(p0: PVector, p1: PVector) { line(p0.x, p0.y, p1.x, p1.y); }

    fun ellipse(p: PVector, r: Float) { ellipse(p.x, p.y, r, r); }

    fun vertex(p: PVector) { vertex(p.x, p.y); }
}