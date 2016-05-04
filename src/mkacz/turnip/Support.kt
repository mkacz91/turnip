package mkacz.turnip

import processing.core.PVector

interface Support
{
    data class EvalResult(val position: PVector, val direcion: PVector)

    fun projectParam(position: PVector, radius: Float) : Float
    fun eval(radius: Float, param: Float) : EvalResult
    fun encroaches(position: PVector, radius: Float) : Boolean
    fun length(radius: Float) : Float
}

abstract class ActiveSupport(val length: Float)
{
    var param: Float = 0f
    abstract val pred: Support
    abstract val succ: Support
    abstract fun eval(param: Float) : Support.EvalResult
}

class ActiveSegmentSupport(val segment: WorldSegment, val radius: Float)
    : ActiveSupport(segment.length(radius))
{
    private val support = segment.supportSegment(radius)

    override val pred: Support
        get() = segment.succ
    override val succ: Support
        get() = segment.pred

    override fun eval(param: Float) = support.eval(param)
}