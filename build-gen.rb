INPUT_FILE= 'build-gen.xml'
OUTPUT_FILE= 'build.xml'

# Read input
x= File.read(INPUT_FILE)

# Read builds
require 'rexml/document'
doc= REXML::Document.new(x)
builds= nil
doc.elements.each('project/property') {|e| builds= e.attributes['value'] if e.attributes['name'] == 'tanuki.builds' }
raise 'Builds not defined.' unless builds
builds= builds.split ','
puts "Builds:\n" + builds.map{|b|"   #{b}"}.join("\n")

# Process targets
puts "\nTargets:"
x.gsub! %r|[ \t]*<target .+?</target>[ \t]*|m do |target_xml|
  raise unless target_xml =~ /<target( +.+?=".+?")? +name="(.+?)"/
  name= $2
  puts "   #{name}"
  t= builds.map{|b|
    a= target_xml.gsub "${build.", "${#{b}."
    a.gsub! "${build}", b
    a.sub! /<target( +.+?=".+?")? +name="(.+?)"/, "<target\\1 name=\"\\2-#{b}\""
    a
  }
  target_xml =~ /^([ \t]*).+?([ \t]*)$/
  t<< "#{$1}<target name=\"#{name}-all\" depends=\"#{builds.map{|b|"#{name}-#{b}"}.join ','}\" />#{$2}"
  t.join("\n\n")
end

# Output to file
fout= File.new(OUTPUT_FILE,'w')
fout<< x
fout.close
