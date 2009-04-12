require 'java'

def golly
  Java::Golly
end
  
module Tanuki2
  module TestHelpers

    # Returns the value of a field on a java object
    def get_field_value(obj, name)
      c= obj.java_class
      name= name.to_s
      while c
        if f= c.declared_field(name) rescue nil
          f.accessible= true
          return f.value(obj)
        end
        c= c.superclass
      end
      raise "Field #{name} not found in class #{obj.java_class.to_s}."
    end

  end
end

# Borrowed from Rails
unless :to_proc.respond_to?(:to_proc)
  class Symbol
    # Turns the symbol into a simple proc, which is especially useful for enumerations. Examples:
    #
    #   # The same as people.collect { |p| p.name }
    #   people.collect(&:name)
    #
    #   # The same as people.select { |p| p.manager? }.collect { |p| p.salary }
    #   people.select(&:manager?).collect(&:salary)
    def to_proc
      Proc.new { |*args| args.shift.__send__(self, *args) }
    end
  end
end
