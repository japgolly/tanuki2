require 'java'
require "#$TANUKI2/helpers/test_helpers"

module Tanuki2
  class RubyTest
    include TestHelpers
    #include golly.tanuki2.ruby.IRubyTest
    STATIC_IMPORTS= [org.junit.Assert, golly.tanuki2.TestHelper]
    
    def beforeClass
    end
    
    def afterClass
    end
    
    def setup
    end
    
    def teardown
    end
    
    def getTestNames
      methods.select{|m| m=~/^test_.+/}.sort.to_java :String
    end
    
    def runTest(name)
      send name
    end
    
    def toString
      self.class.to_s
    end

    # Try to delegate uncaught messages to static imports
    def method_missing(sym,*args)
      STATIC_IMPORTS.each{|si|
        if si.respond_to?(sym)
          return si.send(sym,*args)
        end
      }
      super(sym,*args)
    end
  end
end
