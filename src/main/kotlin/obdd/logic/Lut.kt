package obdd.logic

class Lut(private val inputWires: Array<String>, private val outputWire: String, private val emulateFormula: Formula) {
    override fun toString(): String {
        return "LUT(${inputWires.joinToString(", ")})->$outputWire"
    }

    @OptIn(ExperimentalUnsignedTypes::class)
    fun toBLIF(lutSizes: List<Int>, placeholder: String): String {
        val paddedSize = lutSizes.filter { it >= inputWires.size }.minOrNull()
            ?: throw Exception("No suitable LUT type available for ${inputWires.size} inputs")

        if(inputWires.size > 16)
            throw Exception("LUT is too large to be converted to cubes (max 16 inputs, got ${inputWires.size})")

        val paddedInputs = inputWires.joinToString(" ") + " $placeholder".repeat(paddedSize - inputWires.size)
        val header = ".names $paddedInputs $outputWire\n"
        val onLines = formulaToCubeSet().map { cubeToBlifLine(it, paddedSize) }

        return header + onLines.joinToString("\n")
    }

    @kotlin.ExperimentalUnsignedTypes
    private fun formulaToCubeSet(): List<UInt> {
        val queue: MutableList<Triple<Formula,Int,UInt>> = mutableListOf(Triple(emulateFormula, 0, UInt.MAX_VALUE))
        val onSet: MutableList<UInt> = mutableListOf()

        while (queue.isNotEmpty()) {
            val item = queue.removeFirst()
            val formula = item.first
            val nextVarIndex = item.second
            val cube = item.third

            when(formula) {
                is ConstFalse -> continue
                is ConstTrue -> onSet.add(cube)
                else -> {
                    val nextVar = inputWires[nextVarIndex]
                    val oneCube = cube and ((0b01u shl nextVarIndex*2) xor UInt.MAX_VALUE)
                    val zeroCube = cube and ((0b10u shl nextVarIndex*2) xor UInt.MAX_VALUE)
                    queue.add(Triple(formula.simplify(nextVar, true), nextVarIndex + 1, oneCube))
                    queue.add(Triple(formula.simplify(nextVar, false), nextVarIndex + 1, zeroCube))
                }
            }
        }

        return onSet
    }

    @kotlin.ExperimentalUnsignedTypes
    private fun cubeToBlifLine(cube: UInt, padToSize: Int) : String {
        var line = ""
        for(i in inputWires.indices) {
            line += when((cube shr i*2) and 0b11u) {
                0b10u -> "1"
                0b01u -> "0"
                0b11u -> "-"
                else -> throw Exception("Illegal cube $cube")
            }
        }

        line += "-".repeat (padToSize - inputWires.size)

        return "$line 1"
    }
}