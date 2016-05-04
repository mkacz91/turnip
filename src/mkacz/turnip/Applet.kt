package mkacz.turnip

import processing.core.PApplet
import processing.core.PVector
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.file.FileSystems
import java.nio.file.Files

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
        const val SEGMENT_HOVER_RADIUS = 10.0f
        const val SEGMENT_INSERT_RADIUS = 120.0f
        val BACKGROUND_COLOR = rgb(0xE39794)
        val LOOP_COLOR = rgb(0x91615F)
        val LOOP_ACTIVE_COLOR = rgb(0x694644)
        val NODE_HOVER_COLOR = rgb(0xE3D8D8)
        val NODE_ACTIVE_COLOR = rgb(0xD4B9B8)
        val G_ACCEL = vec(0, -40)

        val SELECT_BUTTON = LEFT
        val SPAWN_BUTTON = RIGHT

        fun pickLoop(world: World, position: PVector)
            = world.loops.find { l -> l.contains(position) }

        fun nearestSegment(world: World, position: PVector)
            = world.segments.minBy { s -> s.distSq(position) }

        fun pickNode(world: World, position: PVector, radius: Float)
            = world.nodes.find { n -> n.distSq(position) <= radius * radius }
    }

    val guy = Guy()
    val world = World()
    var dt = 0.0f
    var insertSegment: WorldSegment? = null

    var hoverItem: WorldItem? = null
    var activeItem: WorldItem? = null
    var showBisectors = true
    var showBoundaries = false
    var showSegmentDirection = false

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

        if (mode == Mode.PLAY)
        {
            var activeSupport = guy.support
            if (activeSupport != null)
            {
                    var accel = 0f
                if (leftPressed)
                    accel -= 2000f
                if (rightPressed)
                    accel += 2000f
                activeSupport.velocity += dt * accel / activeSupport.length
                activeSupport.position += dt * activeSupport.velocity

                while (activeSupport!!.position < 0)
                {
                    val newSupport = activeSupport.pred.activate(guy.radius)
                    val f = activeSupport.length / newSupport.length
                    newSupport.velocity = f * activeSupport.velocity
                    newSupport.position = f * activeSupport.position + 1
                    activeSupport = newSupport
                }
                while (activeSupport!!.position > 1)
                {
                    val newSupport = activeSupport.succ.activate(guy.radius)
                    val f = activeSupport.length / newSupport.length
                    newSupport.velocity = f * activeSupport.velocity
                    newSupport.position = f * (activeSupport.position - 1)
                    activeSupport = newSupport
                }

                val se = activeSupport.eval(activeSupport.position)
                guy.velocity = mul(activeSupport.velocity * activeSupport.length, se.direcion)
                guy.position = se.position
                activeSupport.velocity *= pow(0.001f, dt)
                guy.support = activeSupport
            }
            else
            {
                guy.velocity.add(mul(dt, G_ACCEL))
                guy.position.add(mul(dt, guy.velocity))

                val support = world.segments.find { it.encroaches(guy.position, guy.radius) }
                if (support != null)
                {
                    activeSupport = support.activate(guy.radius)
                    val param = activeSupport.projectParam(guy.position)
                    activeSupport.position = param
                    activeSupport.velocity = 0f
                    val se = activeSupport.eval(param)
                    guy.support = activeSupport
                    guy.position = se.position
                    guy.velocity = mul(dot(guy.velocity, se.direcion), se.direcion)
                }
            }
        }

        background(BACKGROUND_COLOR)

        noStroke()
        ellipseMode(RADIUS)
        fill(color(255))
        ellipse(guy.position, guy.radius)

        strokeWeight(2.0f)
        for (loop in world.loops)
        {
            var fillColor = if (loop == activeItem) LOOP_ACTIVE_COLOR else LOOP_COLOR
            if (loop == hoverItem)
                fillColor = lighten(fillColor, 0.1f)
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
            if (insertSegment != null)
            {
                strokeWeight(1.0f)
                stroke(128)
                noFill()
                line(mouse, insertSegment!!.start.position)
                line(mouse, insertSegment!!.end.position)
                ellipse(mouse, NODE_RADIUS)
            }

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

            noStroke()
            val hoverNode = hoverItem as? WorldNode
            val activeNode = activeItem as? WorldNode
            if (hoverNode == activeNode)
            {
                if (hoverNode != null)
                {
                    fill(rgb(darken(NODE_ACTIVE_COLOR, 0.1f)))
                    ellipse(hoverNode.position, NODE_RADIUS)
                }
            }
            else
            {
                if (hoverNode != null)
                {
                    fill(NODE_HOVER_COLOR)
                    ellipse(hoverNode.position, NODE_RADIUS)
                }
                if (activeNode != null)
                {
                    fill(NODE_ACTIVE_COLOR)
                    ellipse(activeNode.position, NODE_RADIUS)
                }
            }

            noFill()
            strokeWeight(1.0f)
            stroke(color(87, 34, 77))
            for (node in world.nodes)
                ellipse(node.position, NODE_RADIUS)
        }

        strokeWeight(1.0f)
        stroke(color(0))
        if (showBisectors)
        {
            for (node in world.nodes)
            {
                val bisector = node.bisector
                bisector.setMag(70f)
                line(node.position, add(node.position, bisector))
            }
        }

        if (showBoundaries)
        {
            for (segment in world.segments)
            {
                val startBoundary = segment.startBound.setMag(70f)
                val endBoundary = segment.endBound.setMag(70f)
                line(segment.start.position, add(segment.start.position, startBoundary))
                line(segment.end.position, add(segment.end.position, endBoundary))
            }
        }

        if (showSegmentDirection)
        {
            for (segment in world.segments)
            {
                val u = mul(5f, segment.direction)
                val v = lhp(u)
                val c = segment.center
                val t = add(c, u)
                val b = sub(c, u)
                line(t, add(b, v))
                line(t, sub(b, v))
            }
        }

        if (mode == Mode.PLAY && guy.support != null)
        {
            strokeWeight(2f)
            stroke(rgb(0xaa1100))
            line(guy.support!!.eval(0f).position, guy.support!!.eval(1f).position)
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

    fun spawnGuy(position: PVector)
    {
        guy.position = position
        guy.velocity = vec(0, 0)
        guy.support = null
    }

    override fun mousePressed()
    {
        if (mode == Mode.EDIT_WORLD && mouseButton == SELECT_BUTTON)
        {
            updateHover()
            activeItem = hoverItem
            if (insertSegment != null)
            {
                hoverItem = insertSegment!!.insertNode(mouse)
                insertSegment = null
            }
            else if (hoverItem == null)
            {
                hoverItem = world.addLoop(mouse).origin
            }
        }
        if (mouseButton == SPAWN_BUTTON)
        {
            spawnGuy(mouse)
        }
    }

    val mouse: PVector
        get() = vec(mouseX, height - mouseY)

    val dmouse: PVector
        get() = vec(mouseX - pmouseX, pmouseY - mouseY)

    override fun mouseDragged()
    {
        if (mode == Mode.EDIT_WORLD && mouseButton == SELECT_BUTTON)
        {
            hoverItem?.moveBy(dmouse)
        }
        if (mouseButton == SPAWN_BUTTON)
        {
            spawnGuy(mouse)
        }
    }

    override fun mouseMoved()
    {
        if (mode == Mode.EDIT_WORLD)
            updateHover()
    }

    fun updateHover()
    {
        hoverItem = null
        insertSegment = null

        val node = pickNode(world, mouse, NODE_HOVER_RADIUS)
        if (node != null)
        {
            hoverItem = node
            return
        }

        val segment = nearestSegment(world, mouse)
        val segmentDistSq = segment?.distSq(mouse) ?: Float.POSITIVE_INFINITY

        val loop = pickLoop(world, mouse)
        if (loop != null)
        {
            hoverItem = if (segmentDistSq <= SEGMENT_HOVER_RADIUS * SEGMENT_HOVER_RADIUS)
                segment else loop
            return
        }

        if (segmentDistSq <= SEGMENT_INSERT_RADIUS * SEGMENT_INSERT_RADIUS)
        {
            insertSegment = segment
            return
        }
    }

    var leftPressed = false
    var rightPressed = false

    override fun keyPressed() = when (key)
    {
        '1' -> mode = Mode.EDIT_WORLD
        '2' -> mode = Mode.PLAY
        's' ->
        {
            val path = FileSystems.getDefault().getPath("world.trp")
            val stream = Files.newOutputStream(path)
            world.write(DataOutputStream(stream))
            stream.close()
        }
        'l' ->
        {
            val path = FileSystems.getDefault().getPath("world.trp")
            val stream = Files.newInputStream(path)
            world.read(DataInputStream(stream))
            stream.close()
        }
        'b' ->
        {
            if (showBisectors)
            {
                showBisectors = false
                showBoundaries = true
            }
            else if (showBoundaries)
            {
                showBoundaries = false
            }
            else
            {
                showBisectors = true
            }
        }
        'd' -> showSegmentDirection = !showSegmentDirection
        CODED.toChar() -> when (keyCode)
        {
            LEFT -> leftPressed = true
            RIGHT -> rightPressed =  true
            else -> Unit
        }
        else -> Unit
    }

    override fun keyReleased() = when (key)
    {
        CODED.toChar() -> when (keyCode)
        {
            LEFT -> leftPressed = false
            RIGHT -> rightPressed = false
            else -> Unit
        }
        else -> Unit
    }

    fun line(p0: PVector, p1: PVector) { line(p0.x, height - p0.y, p1.x, height - p1.y); }

    fun ellipse(p: PVector, r: Float) { ellipse(p.x, height - p.y, r, r); }

    fun vertex(p: PVector) { vertex(p.x, height - p.y); }
}