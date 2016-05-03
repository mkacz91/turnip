package mkacz.turnip

import processing.core.PVector

interface Support
{
    data class EvalResult(val position: PVector, val direcion: PVector)

    fun projectParam(position: PVector)
    fun eval(radius: Float, param: Float) : EvalResult
    fun encroaches(position: PVector, radius: Float) : Boolean
    fun length(radius: Float) : Float
}
