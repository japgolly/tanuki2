require "#$TANUKI2/ruby_test"

module Tanuki2
  class SwtTest < RubyTest
    import org.eclipse.swt.SWT
    import golly.tanuki2.support.I18n
    
    attr_reader :shell, :display
    
    def beforeClass
      @display= $SWT_DISPLAY
      I18n.setLocale java.util.Locale::ENGLISH
      golly.tanuki2.support.TanukiImage.setDisplay @display
    end
    
    def setup
      super
      @shell= org.eclipse.swt.widgets.Shell.new(display)
      shell.setLayout(org.eclipse.swt.layout.FillLayout.new)
    end
    
    def teardown
      if shell
        shell.dispose unless shell.isDisposed
        @shell= nil
      end
      super
    end
    
    protected
      def dispatch_swt_tasks!
        while display.readAndDispatch; end
      end

      # Finds a single TreeItem in an SWT Tree.      
      def find_treeitem(parent, *elements)
        raise if elements.empty?
        elements.each{|e|
          return nil unless parent
          found= nil
          children= parent.getItems
          for child in children
            if child && e =~ child.getText
              found= child
              break
            end
          end if children
          parent= found
        }
        parent
      end
      
      # Selects items in an SWT Tree widget.
      # See find_treeitem
      def select_items_in_tree(tree, *criteria)
        selection= criteria.map{|elements|
          raise "Array expected but was #{elements.inspect}." unless elements.is_a?(Array)
          ti= find_treeitem(tree, *elements)
          raise "TreeItem not found for #{elements.inspect}" unless ti
          ti
        }
        tree.setSelection selection.to_java(org.eclipse.swt.widgets.TreeItem)
      end
    
  end
end