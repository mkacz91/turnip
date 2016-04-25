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
        const val HOVER_NODE_RADIUS = 11.0f
        const val ACTIVE_NODE_RADIUS = 12.0f
        const val SEGMENT_RANGE = 40.0f
        val SELECT_BUTTON = PApplet.LEFT
    }

    val guy = Guy()
    val world = World()
    var dt = 0.0f
    var activeNode: WorldNode? = null
    var activeSegment: WorldSegment? = null

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
        for (origin in world.origins)
        {
            beginShape()
            for (node in origin.nodeLoop)
                vertex(node.position)
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
            val m = vec(mouseX, mouseY)
            activeNode = world.pickNode(m, NODE_RADIUS)
                ?: world.pickSegment(m, SEGMENT_RANGE)?.insertNode(m)
                ?: world.addOrigin(m)
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
            val m = vec(mouseX, mouseY)
            activeNode = world.pickNode(m, NODE_RADIUS)
            activeSegment = if (activeNode == null) world.pickSegment(m, SEGMENT_RANGE) else null
        }
    }

    fun line(p0: PVector, p1: PVector) { line(p0.x, p0.y, p1.x, p1.y); }

    fun ellipse(p: PVector, r: Float) { ellipse(p.x, p.y, r, r); }

    fun vertex(p: PVector) { vertex(p.x, p.y); }
}