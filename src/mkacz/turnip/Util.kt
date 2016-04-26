package mkacz.turnip

import processing.core.PApplet
import processing.core.PVector

fun vec(x : Float, y: Float) : PVector
{
    return PVector(x, y);
}

fun vec(x: Int, y: Int) = PVector(x.toFloat(), y.toFloat())

fun add(u: PVector, v: PVector) : PVector
{
    return PVector(u.x + v.x, u.y + v.y);
}

fun sub(u : PVector, v: PVector) : PVector
{
    return PVector(u.x - v.x, u.y - v.y);
}

fun mul(s: Float, u: PVector) : PVector
{
    return PVector(s * u.x, s * u.y);
}

fun mul(u: PVector, s: Float) : PVector
{
    return mul(s, u);
}

fun dot(u: PVector, v: PVector) : Float
{
    return u.x * v.x + u.y * v.y;
}

fun per(u: PVector, v: PVector) : Float
{
    return u.x * v.y - u.y * v.x;
}

fun span(u: PVector, v: PVector) : PVector
{
    return PVector(v.x - u.x, v.y - u.y);
}

fun mid(u: PVector, v: PVector) : PVector
{
    return PVector(0.5f * (u.x + v.x), 0.5f * (u.y + v.y));
}

fun clearDirection(u: PVector, direction: PVector)
{
    u.sub(mul(dot(u, direction), direction));
}

fun lengthSq(u: PVector) : Float
{
    return u.x * u.x + u.y * u.y;
}

fun distSq(u: PVector, v: PVector) : Float
{
    val dx = v.x - u.x;
    val dy = v.y - u.y;
    return dx * dx + dy * dy;
}

fun clamp(minVal: Float, maxVal: Float, x: Float) : Float
{
    if (x <= minVal)
        return minVal;
    if (x >= maxVal)
        return maxVal;
    return x;
}

fun angle(u: PVector) : Float
{
    return PApplet.atan2(u.y, u.x);
}

fun sq(x: Float) = x * x

fun min(x: Float, y: Float) = if (x < y) x else y

fun pointToSegmentDistSq(a: PVector, b: PVector, p: PVector) : Float
{
    val ab = span(a, b)
    val pa = span(p, a)
    val pb = span(p, b)
    return if (dot(ab, pa) * dot(ab, pb) < 0)
        sq(per(pa, pb)) / lengthSq(ab)
    else
        min(lengthSq(pa), lengthSq(pb))
}

fun pointInPolygon(poly: Iterable<PVector>, point: PVector) : Boolean
{
    var result = false
    fun processEdge(start: PVector, end: PVector)
    {
        if ((point.y - start.y) * (point.y - end.y) >= 0)
            return
        val t = (point.y - start.y) / (end.y - start.y)
        if (t.isNaN() || t.isInfinite())
            return
        if (point.x <= start.x + t * (end.x - start.x))
            result = !result
    }

    val it = poly.iterator()
    if (!it.hasNext())
        return false
    val first = it.next()
    var end = first
    while (it.hasNext())
    {
        val start = end
        end = it.next()
        processEdge(start, end)
    }
    processEdge(end, first)
    return result
}