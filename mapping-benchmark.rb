require 'benchmark'
require 'json'

results = {}
abc = {}

def countLUTs(filename)
	lutsizes = File.readlines(filename).filter{ |l| l.start_with?(".names") }.map { |l| (l.split(" ").length - 2) < 4 ? 3 : 5 }
	tally = lutsizes.uniq.map { |s| [s, lutsizes.count(s)] }.to_h
	five = tally[5] ? tally[5] : 0
	three = tally[3] ? tally[3] : 0
	five + three / 2
end

def getDepth(filename)
	abcOut = `abc -c "read #{filename}; print_level;"`
	/Level = \s*([0-9]+)\./.match(abcOut.split("\n")[-1]).captures[0].to_i
end

def getBenchmarkFiles
	Dir.each_child("benchmark/mapping/blif/MCNC-big20").filter{ |file| file.split(".")[-1] == 'blif' }.map{ |file|
		title = file.split(".")[0...-1].join('.')
		fullpath = "benchmark/mapping/blif/MCNC-big20/#{file}"
		[title, fullpath]
	}
end

def genABCregular
	results = {}
	outpath = "benchmark/mapping/abc/"
	getBenchmarkFiles.each { |title, path|
		puts "Running abc reference mapping for #{title}..."
		time = Benchmark.measure {
			`abc -c "read_lut benchmark/mapping/library.lut; read #{path}; if; lutpack; write #{outpath}#{title}.blif"`
		}
		luts = countLUTs("#{outpath}#{title}.blif")
		depth = getDepth("#{outpath}#{title}.blif")

		puts "#{title} completed, #{luts} LUTs, depth #{depth}, #{(time.real * 1000).round}ms end to end"
		results[title] = {:e2e => (time.real * 1000).round, :luts => luts, :depth => depth}
	}
	results
end

def genABCdelay
	results = {}
	outpath = "benchmark/mapping/abc-delay/"
	getBenchmarkFiles.each { |title, path|
		puts "Running abc-delay reference mapping for #{title}..."
		time = Benchmark.measure {
			`abc -c "read_lut benchmark/mapping/library-delay.lut; read #{path}; if; lutpack; write #{outpath}#{title}.blif"`
		}
		luts = countLUTs("#{outpath}#{title}.blif")
		depthOutput = `abc -c "read #{outpath}#{title}.blif; print_level;"`
		depth = getDepth("#{outpath}#{title}.blif")

		puts "#{title} completed, #{luts} LUTs, depth #{depth}, #{(time.real * 1000).round}ms end to end"
		results[title] = {:e2e => (time.real * 1000).round, :luts => luts, :depth => depth}
	}
	results
end

def genABCisolated
	results = {}
	outpath = "benchmark/mapping/abc-isolated/"
	getBenchmarkFiles.each { |title, path|
		puts "Running abc-isolated reference mapping for #{title}..."
		blif = File.read(path)
		outputLine = /\.outputs(( |\n)(?<wire>[^\s\\\.]+)( \\)?)+/.match(blif)[0]
		outputs = outputLine.gsub("\\", "").gsub("\n", " ").sub(".outputs ", "").split(" ")
		luts = 0

		outputs.each { |out|
		 	isolated = blif.sub(outputLine, ".outputs #{out}")
		 	File.open("/tmp/isolated.blif", "w") { |file| file.puts(isolated) }
		 	`abc -c "read_lut benchmark/mapping/library.lut; read /tmp/isolated.blif; if; lutpack; write #{outpath}#{title}-#{out}.blif"`
		 	luts += countLUTs("#{outpath}#{title}-#{out}.blif")
		}
		results[title] = {:e2e => 0, :luts => luts, :depth => 0}
	}
	results
end

def genFusemapRegular
	results = {}
	outpath = "benchmark/mapping/fusemap/"
	`rm #{outpath}*.blif`

	getBenchmarkFiles.each { |title, path|
		puts "Running fusemap mapping for #{title}..."
		output = ""
		time = Benchmark.measure {
			output = `java -jar build/libs/obdd-gen-1.0-SNAPSHOT-all.jar --blif-map --loglevel=5 --out=#{outpath}#{title}.blif #{path}`
		}
		luts = countLUTs("#{outpath}#{title}.blif")
		depth = getDepth("#{outpath}#{title}.blif")
		mapTime = output.chomp.split("|")[0].to_i

		verification = `abc -c "cec #{outpath}#{title}.blif #{path}"`
		puts verification =~ /Networks are equivalent/ ? "VERIFICATION OK" : "VERIFICATION FAILURE"

		puts "#{title} completed, #{luts} LUTs, depth #{depth}, #{(time.real * 1000).round}ms end to end"
		results[title] = {:e2e => (time.real * 1000).round, :luts => luts, :depth => depth, :map => mapTime}
	}
	results
end

def genFusemapAgressive
	results = {}
	outpath = "benchmark/mapping/fusemap-agressive/"
	getBenchmarkFiles.each { |title, path|
		puts "Running fusemap-agressive mapping for #{title}..."
		output = ""
		time = Benchmark.measure {
			output = `java -jar fusemap-agressive.jar --blif-map --loglevel=5 --out=#{outpath}#{title}.blif #{path}`
		}
		luts = countLUTs("#{outpath}#{title}.blif")
		depth = getDepth("#{outpath}#{title}.blif")
		mapTime = output.chomp.split("|")[0].to_i

		verification = `abc -c "cec #{outpath}#{title}.blif #{path}"`
		puts verification =~ /Networks are equivalent/ ? "VERIFICATION OK" : "VERIFICATION FAILURE"

		puts "#{title} completed, #{luts} LUTs, depth #{depth}, #{(time.real * 1000).round}ms end to end"
		results[title] = {:e2e => (time.real * 1000).round, :luts => luts, :depth => depth, :map => mapTime}
	}
	results
end

#abc = genABCregular()
#abcDelay = genABCdelay()
#abcIsolated = genABCisolated()
fusemap = genFusemapRegular()
#fusemapAgressive = genFusemapAgressive()

#puts JSON.dump(abc)
#puts JSON.dump(abcDelay)
#puts JSON.dump(abcIsolated)
puts JSON.dump(fusemap)