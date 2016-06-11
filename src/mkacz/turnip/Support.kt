package mkacz.turnip

import processing.core.PVector

data class SupportEval(val position: PVector, val direcion: PVector)

interface Support
{
    val predSupport: Support
    val succSupport: Support
    fun encroaches(position: PVector, radius: Float) : Boolean
    fun length(radius: Float) : Float
    fun activate(radius: Float) : ActiveSupport
}

abstract class ActiveSupport(val support: Support, val radius: Float)
{
    val length = support.length(radius)
    var position = 0f
    var velocity = 0f
    abstract fun projectParam(position: PVector) : Float
    abstract fun eval(param: Float) : SupportEval
}

class DummySupport(override val succSupport: Support) : Support
{
    override val predSupport = this
    override fun encroaches(position: PVector, radius: Float) = false
    override fun length(radius: Float) = 0f
    override fun activate(radius: Float) = object : ActiveSupport(this, radius)
    {
        override fun projectParam(position: PVector) = 0f
        override fun eval(param: Float) = SupportEval(vec(0f, 0f), vec(0f, 0f))
    }
}