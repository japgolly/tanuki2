INPUT_FILE= 'build-gen.xml'
OUTPUT_FILE= 'build.xml'
BUILD_TAG= '${build}'

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

# Process properties
puts "\nProperties:"
x.gsub! %r|[ \t]*<property .+?>[ \t]*|m do |target_xml|
  raise unless target_xml =~ /<property( +.+?=".+?")? +name="(.+?)"/
  name= $2
  if target_xml.include?(BUILD_TAG)
    puts "   #{name}"
    builds.map{|b| target_xml.gsub("${build.", "${#{b}.").gsub(BUILD_TAG, b)}.join("\n")
  else
    target_xml
  end
end

# Process targets
puts "\nTargets:"
x.gsub! %r|[ \t]*<target .+?</target>[ \t]*|m do |target_xml|
  raise unless target_xml =~ /<target( +.+?=".+?")? +name="(.+?)"/
  name= $2
  puts "   #{name}"
  if target_xml =~ /^.*?<target [^>]+?\$\{build\}/
    target_xml.sub! %r|^([ \t]*)</target>|, "\\1\t<package-${build}-impl />\n\\1</target>" if name == 'package-${build}'
    t= builds.map{|b| target_xml.gsub("${build.", "${#{b}.").gsub(BUILD_TAG, b)}
    target_xml =~ /^([ \t]*).+?([ \t]*)$/
    t<< "#{$1}<target name=\"#{name.gsub BUILD_TAG, 'all'}\" depends=\"#{builds.map{|b|"#{name.gsub BUILD_TAG, b}"}.join ','}\" />#{$2}"
    t.join("\n\n")
  else
    target_xml
  end
end

# Output to file
fout= File.new(OUTPUT_FILE,'w')
fout<< x
fout.close
