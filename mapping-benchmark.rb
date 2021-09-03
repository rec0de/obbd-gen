require 'benchmark'
require 'json'

results = {}
abc = {}

def countLUTs(filename)
	lutsizes = File.readlines(filename).filter{ |l| l.start_with?(".names") }.map { |l| (l.split(" ").length - 2) < 4 ? 3 : 5 }
	tally = lutsizes.uniq.map { |s| [s, lutsizes.count(s)] }.to_h
	tally[5] + tally[3] / 2
end

Dir.each_child("benchmark/mapping/blif/MCNC-big20"){|file| 
	next if (file.split(".")[-1] != 'blif')
	title = file.split(".")[0...-1].join('.')
	fullpath = "benchmark/mapping/blif/MCNC-big20/#{file}"
	
	puts "Running benchmark for #{title}..."
	output = ""
	endToEnd = Benchmark.measure {
		output = `java -jar build/libs/obdd-gen-1.0-SNAPSHOT-all.jar --blif-map --loglevel=5 --out=benchmark/mapping/fusemap/#{title}.blif #{fullpath}`
	}

 	verification = `abc -c "cec benchmark/mapping/fusemap/#{title}.blif #{fullpath}"`
 	mapTime = output.chomp.split("|")[0]
 	lutCount = countLUTs("benchmark/mapping/fusemap/#{title}.blif")

 	puts "#{title} completed, #{lutCount} LUTs, #{mapTime}ms map time, #{(endToEnd.real * 1000).round}ms end to end"
 	puts verification =~ /Networks are equivalent/ ? "VERIFICATION OK" : "VERIFICATION FAILURE"

 	puts "Running abc reference mapping for #{title}..."
 	output = ""
 	abcEndToEnd = Benchmark.measure {
  		output = `abc -c "read_lut benchmark/mapping/lutlibrary.lut; read #{fullpath}; if; lutpack; write benchmark/mapping/abc-if/#{title}.blif"`
 	}
 	abcLUTs = countLUTs("benchmark/mapping/abc-if/#{title}.blif")
 	puts "abc ref for #{title} completed, #{abcLUTs} LUTs, #{(abcEndToEnd.real * 1000).round}ms end to end"

 	results[title] = {:e2e => (endToEnd.real * 1000).round, :luts => lutCount, :map => mapTime.to_i}
 	abc[title] = {:e2e => (abcEndToEnd.real * 1000).round, :luts => abcLUTs}
}

puts JSON.dump(results)
puts JSON.dump(abc)