package obdd.logic

class LutGraph(val root: Lut) {

}

interface LutAtom

class Lut(val inputs: Array<LutAtom>, val emulateFormula: Formula, val inputOrder: Array<String>) : LutAtom