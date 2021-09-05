require 'json'

a = JSON.parse(File.read(ARGV[0]))
b = JSON.parse(File.read(ARGV[1]))

aTotalLut = a.sum{ |key, stats| stats["luts"] }
bTotalLut = b.sum{ |key, stats| stats["luts"] }

puts "#{ARGV[0]} baseline vs #{ARGV[1]}"
puts "LUT comparison by benchmark file"
a.each { |key, stats|
	plusminus = stats["luts"] <= b[key]["luts"] ? "+" : ""
	baseline = stats["luts"]
	lutdiff = b[key]["luts"] - baseline
	puts "#{key}: #{b[key]["luts"]} LUTs (#{plusminus}#{lutdiff}, #{plusminus}#{((lutdiff.to_f/baseline)*100).round}%)"
}

plusminus = aTotalLut <= bTotalLut ? "+" : ""
puts "Total: #{plusminus}#{bTotalLut - aTotalLut} LUTs"
puts "\n"

puts "Depth comparison by benchmark file"
a.each { |key, stats|
	plusminus = stats["depth"] <= b[key]["depth"] ? "+" : ""
	puts "#{key}: #{b[key]["depth"]} levels (#{plusminus}#{b[key]["depth"] - stats["depth"]} levels)"
}
puts "\n"

puts "Runtime comparison by benchmark file"
a.each { |key, stats|
	plusminus = stats["e2e"] <= b[key]["e2e"] ? "+" : ""
	puts "#{key}: #{b[key]["e2e"]} ms (#{plusminus}#{b[key]["e2e"] - stats["e2e"]}ms)"
}

