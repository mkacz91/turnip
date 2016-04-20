package mkacz.turnip

import processing.core.PApplet
import processing.core.PVector

fun vec(x : Float, y: Float) : PVector
{
    return PVector(x, y);
}

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