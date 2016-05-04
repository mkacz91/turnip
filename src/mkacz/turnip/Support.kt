package mkacz.turnip

import processing.core.PVector


data class SupportEval(val position: PVector, val direcion: PVector)

interface Support
{
    fun encroaches(position: PVector, radius: Float) : Boolean
    fun length(radius: Float) : Float
    fun activate(radius: Float) : ActiveSupport
}

abstract class ActiveSupport(val length: Float, val radius: Float)
{
    var position: Float = 0f
    var velocity: Float = 0f
    abstract val pred: Support
    abstract val succ: Support
    abstract fun projectParam(position: PVector) : Float
    abstract fun eval(param: Float) : SupportEval
}