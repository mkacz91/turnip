package mkacz.turnip

import processing.core.PVector

class Guy
{
    var position = PVector()
    var velocity = PVector()
    val radius = 20f
    var support: Support? = null
}