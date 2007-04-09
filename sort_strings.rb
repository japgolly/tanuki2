require 'fileutils'

Dir.glob('src/**/strings_*.properties').each {|f|
  puts f
  content= File.read(f)
  content.gsub!(/[\r\n][ \t]+(?=[\r\n]|$)/,"\n")
  content.gsub!(/[\r\n]{3,}/,"\n\n")
  content= content.split(/[\r\n]{2}/).map{|txt| txt.split(/[\r\n]/).sort.join("\n")}.join("\n\n") + "\n"
  content.sub!(/[\r\n]{2,}$/m,"\n")

  newfile= "#{f}.new"
  File.open(newfile,'w') {|fout| fout<< content}
  FileUtils.mv newfile, f, :force => true
}
