class MockFormula

	@@varCount = 4
	@@vars = "abcdefghijklmnopqrstuvwxyz"

	def genFormula()
		generatePureExpr()
	end

	def generatePureExpr()
		generateImpl()
	end

	def generateImpl()
		if chance(0.3)
			op = ["->", "<=>", "=>", "<->"].sample
			return "#{generateOr()} #{op} #{generateOr()}"
		else
			return generateOr()
		end
	end

	def generateOr()
		if chance(0.35)
			op = ["|", "|", "^"].sample
			return "#{generateAnd()} #{op} #{generateOr()}"
		else
			return generateAnd()
		end
	end

	def generateAnd()
		if chance(0.35)
			return "#{generateNot()} & #{generateAnd()}"
		else
			return generateNot()
		end
	end

	def generateNot()
		if chance(0.3)
			return "!#{generateAtom()}"
		else
			return generateAtom()
		end
	end


	def generateAtom()
		if chance(0.2)
			"(#{generatePureExpr()})"
		elsif chance(0.85)
			@@vars.split('').sample
		else
			["true", "false"].sample
		end
	end

	def chance(prob)
		Random::rand() < prob
	end

	def exponentialRandomValue(lambda)
		(-Math.log(Random::rand())) / lambda
	end

	def exponentialRandomInt(expected)
		exponentialRandomValue(1.to_f/expected).ceil.to_i
	end
end

gen = MockFormula.new()

500.times do 
	formula = gen.genFormula()
	while(formula.length < 6)
		formula = gen.genFormula()
	end

	puts formula
end