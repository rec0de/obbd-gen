package obdd.logic

class Lut(val inputWires: Array<String>, val outputWire: String, val emulateFormula: Formula) {
    override fun toString(): String {
        return "LUT(${inputWires.joinToString(", ")})->$outputWire"
    }
}