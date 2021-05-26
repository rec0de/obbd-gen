package obdd

import obdd.gen.FormulaBaseVisitor
import obdd.gen.FormulaLexer
import obdd.gen.FormulaParser
import obdd.logic.*
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer

object FormulaConverter : FormulaBaseVisitor<Formula>() {
    fun parse(formulaString: String): Formula {
        val stream = CharStreams.fromString(formulaString)

        val lexer = FormulaLexer(stream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(ParseErrorListener)

        val tokens = CommonTokenStream(lexer)
        val parser = FormulaParser(tokens)
        parser.removeErrorListeners()
        parser.addErrorListener(ParseErrorListener)

        try {
            return parser.formula().accept(this)
        } catch (e: ParsingException) {
            throw Exception("Could not parse formula '$formulaString':\n${e.message}")
        }
    }

    override fun visitTrue_formula(ctx: FormulaParser.True_formulaContext?) = ConstTrue
    override fun visitFalse_formula(ctx: FormulaParser.False_formulaContext?) = ConstFalse
    override fun visitVar_formula(ctx: FormulaParser.Var_formulaContext?) = Var(ctx!!.VAR().text)

    override fun visitNest_formula(ctx: FormulaParser.Nest_formulaContext?): Formula = ctx!!.formula().accept(this)
    override fun visitNot_formula(ctx: FormulaParser.Not_formulaContext?) = Not(ctx!!.formula().accept(this))

    override fun visitAnd_formula(ctx: FormulaParser.And_formulaContext?): And {
        val left = ctx!!.formula(0).accept(this)
        val right = ctx.formula(1).accept(this)
        return And(left, right)
    }

    override fun visitOr_xor_formula(ctx: FormulaParser.Or_xor_formulaContext?): Formula {
        val left = ctx!!.formula(0).accept(this)
        val right = ctx.formula(1).accept(this)
        return when(ctx.getChild(1)) {
            ctx.OR() -> Or(left, right)
            ctx.XOR() -> Not(Equiv(left, right)) // xor is just syntactic sugar for !(a<=>b)
            else -> throw RuntimeException("Parse error: Unknown operand in or/xor expression")
        }
    }

    override fun visitImpl_equiv_formula(ctx: FormulaParser.Impl_equiv_formulaContext?): Formula {
        val left = ctx!!.formula(0).accept(this)
        val right = ctx.formula(1).accept(this)
        return when(ctx.getChild(1)) {
            ctx.EQUIV() -> Equiv(left, right)
            ctx.IMPL() -> Or(Not(left), right) // impl is just syntactic sugar for (!a | b)
            else -> throw RuntimeException("Parse error: Unknown operand in impl/equiv expression")
        }
    }
}

class ParsingException(msg: String) : Exception(msg)

object ParseErrorListener : BaseErrorListener() {
    override fun syntaxError(recognizer: Recognizer<*, *>, offendingSymbol: Any?, line: Int, charPositionInLine: Int, msg: String, e: RecognitionException?) {
        throw ParsingException("Error at position $line:$charPositionInLine $msg")
    }
}