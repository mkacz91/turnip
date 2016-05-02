package mkacz.turnip

import processing.core.PApplet
import processing.core.PVector

fun rgb(value: Int) = -16777216 or value
fun red(color: Int) = (0xFF0000 and color) shr 16
fun green(color: Int) = (0x00FF00 and color) shr 8
fun blue(color: Int) = 0x0000FF and color
fun rgbus(red: Int, green: Int, blue: Int) = rgb((red shl 16) or (green shl 8) or blue)
fun clampch(value: Int) = if (value < 0) 0 else (if (255 < value) 255 else value)
fun rgb(red: Int, green: Int, blue: Int) = rgbus(clampch(red), clampch(green), clampch(blue))

fun lighten(color: Int, factor: Float) = rgb(
    lerp(red(color), 255, factor),
    lerp(green(color), 255, factor),
    lerp(blue(color), 255, factor))

fun darken(color: Int, factor: Float) = rgb(
    (red(color) * (1.0f - factor)).toInt(),
    (green(color) * (1.0f - factor)).toInt(),
    (blue(color) * (1.0f - factor)).toInt())

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

fun lhp(u: PVector) = PVector(-u.y, u.x)
fun rhp(u: PVector) = PVector(u.y, -u.x)

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

fun length(u: PVector) = Math.sqrt(lengthSq(u).toDouble()).toFloat()

fun normalized(u: PVector) = mul(u, 1 / length(u))

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

fun pow(x: Float, y: Float) = Math.pow(x.toDouble(), y.toDouble()).toFloat()

fun min(x: Float, y: Float) = if (x < y) x else y

fun lerp(x: Int, y: Int, a: Float) = x + (a * (y - x)).toInt()

fun lerp(p0: PVector, p1: PVector, a: Float) : PVector
{
    val b = 1 - a
    return PVector(b * p0.x + a * p1.x, b * p0.y + a * p1.y)
}

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

fun projectToSegmentParam(a: PVector, b: PVector, p: PVector) : Float
{
    val u = span(a, b)
    val v = span(a, p)
    return dot(u, v) / lengthSq(u)
}

fun projectToSegment(a: PVector, b: PVector, p: PVector)
    = lerp(a, b, projectToSegmentParam(a, b, p))

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

fun bisector(u: PVector, v: PVector) = bisectorOfNormalized(normalized(u), normalized(v))

fun bisectorOfNormalized(u: PVector, v: PVector) : PVector
{
    if (dot(u, v) <= 0)
        return PVector(v.y - u.y, u.x - v.x)
    if (per(u, v) <= 0)
        return PVector(-u.x - v.x, -u.y - v.y)
    return PVector(u.x + v.x, u.y + v.y)
}