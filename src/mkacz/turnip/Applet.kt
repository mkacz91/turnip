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
        const val NODE_PICK_RADIUS = 10.0f
        const val ACTIVE_NODE_RADIUS = 11.0f
        const val SEGMENT_PICK_RADIUS = 5.0f;
        val SELECT_BUTTON = PApplet.LEFT
        val CLEAR_BUTTON = PApplet.RIGHT

        fun pickLoop(world: World, position: PVector)
            = world.loops.find { l -> l.contains(position) }



        fun pickSegment(loop: WorldLoop, position: PVector)
            = loop.segments.minBy { s -> s.distSq(position) }!!

        data class NodeLoopPair(val node: WorldNode, val loop: WorldLoop)
        fun pickNodeWithLoop(world: World, position: PVector, radius: Float) : NodeLoopPair?
        {
            val radiusSq = radius * radius
            for (loop in world.loops)
            {
                for (node in loop.nodes)
                {
                    if (node.distSq(position) <= radiusSq)
                        return NodeLoopPair(node, loop)
                }
            }
            return null
        }

        fun pickNode(world: World, position: PVector, radius: Float)
            = world.nodes.find { n -> n.distSq(position) <= radius * radius }

        fun pickSegment(world: World, position: PVector, radius: Float)
            = world.segments.find { s -> s.distSq(position) <= radius * radius }
    }

    val guy = Guy()
    val world = World()
    var dt = 0.0f
    var hoverNode: WorldNode? = null
    var grabNode: WorldNode? = null
    var insertSegment: WorldSegment? = null
    var hoverSegment: WorldSegment? = null
    var grabSegment: WorldSegment? = null
    var hoverLoop: WorldLoop? = null
    var activeLoop: WorldLoop? = null
    var grabLoop: WorldLoop? = null

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

        // Loops
        noStroke()
        fill(color(229, 117, 99))
        for (loop in world.loops)
        {
            val fillColor = when (loop)
            {
                hoverLoop -> color(255, 0, 0)
                activeLoop -> color(0, 255, 0)
                grabLoop -> color(0, 0, 255)
                else -> color(255, 255, 0)
            }
            fill(fillColor)
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
                val fillColor = when (node)
                {
                    hoverNode -> color(255, 0, 0)
                    grabNode -> color(0, 255, 0)
                    else -> null
                }
                if (fillColor != null)
                {
                    fill(fillColor)
                    ellipse(node.position, NODE_RADIUS)
                    noFill()
                }
                else
                {
                    ellipse(node.position, NODE_RADIUS)
                }
            }
        }

        strokeWeight(3.0f)
        if (hoverSegment != null)
        {
            stroke(0)
            line(hoverSegment!!.start.position, hoverSegment!!.end.position)
        }
        if (grabSegment != null)
        {
            stroke(128)
            line(grabSegment!!.start.position, grabSegment!!.end.position)
        }
        if (insertSegment != null)
        {
            stroke(128)
            line(insertSegment!!.start.position, insertSegment!!.end.position)
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
            updateHover()
            if (hoverNode != null)
            {
                grabNode = hoverNode
            }
            else if (hoverSegment != null)
            {
                grabSegment = hoverSegment
                insertSegment = null
            }
            else if (insertSegment != null)
            {
                hoverNode = insertSegment!!.insertNode(m)
                grabNode = hoverNode
                insertSegment = null
            }
            else if (hoverLoop != null)
            {
                activeLoop = hoverLoop
                grabLoop = hoverLoop
            }
            else
            {

            }
//
//
//            activeSegment = null
//            val m = vec(mouseX, mouseY)
//            val nl = pickNode(world, m, NODE_RADIUS)
//            if (nl != null)
//            {
//                activeLoop = nl.first
//                activeNode = nl.second
//                return
//            }
//            val loop = pickLoop(world, m)
//            if (loop != null && loop != activeLoop)
//            {
//                activeLoop = loop
//                return
//            }
//            if (activeLoop == null)
//            {
//                activeLoop = world.addLoop(m)
//                activeNode = activeLoop!!.origin
//                return
//            }
//            activeNode = pickSegment(activeLoop!!, m).insertNode(m)
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
            //activeNode?.position?.set(mouseX.toFloat(), mouseY.toFloat())
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
        hoverNode = null
        hoverSegment = null
        hoverLoop = null

        hoverNode = pickNode(world, m, NODE_PICK_RADIUS)
        if (hoverNode != null)
            return

        hoverSegment = pickSegment(world, m, SEGMENT_PICK_RADIUS)
        if (hoverSegment != null)
            return

        hoverLoop = pickLoop(world, m)
    }

    fun line(p0: PVector, p1: PVector) { line(p0.x, p0.y, p1.x, p1.y); }

    fun ellipse(p: PVector, r: Float) { ellipse(p.x, p.y, r, r); }

    fun vertex(p: PVector) { vertex(p.x, p.y); }
}