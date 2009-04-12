require "#$TANUKI2/helpers/swt_test"

class AppWindowInputTreeTest < Tanuki2::SwtTest
  include_package 'golly.tanuki2.ui'
  import 'golly.tanuki2.core.Engine'
  
  def setup
    super
    @engine= Engine.new
    @wnd= AppWindow.new(display, @engine)
    @inputTree= get_field_value @wnd, :inputTree
    @inputTree_tree= get_field_value @inputTree, :tree
    @inputTree_contextMenu= get_field_value @inputTree, :contextMenu
  end
  
  def test_context_menu_with_nothing_selected
    add_files "sample_data"
    select_items_in_tree @inputTree_tree
    assert_context_menu @inputTree_contextMenu, { 
      :main_contextMenu_editAlbum => :disabled,
      :main_contextMenu_editArtist => :disabled,
      :main_contextMenu_launchFile => :disabled,
      :main_contextMenu_openFolder => :disabled,
      :main_contextMenu_openPrompt => :disabled,
      :main_contextMenu_removeItems => :disabled,
    }
  end
  
  def test_context_menu_with_single_mp3_selected
    add_files "sample_data"
    select_items_in_tree @inputTree_tree, [/.*sample_data$/, /complete.blah/, /01\.mp3/]
    assert_context_menu @inputTree_contextMenu, { 
      :main_contextMenu_editAlbum => :enabled,
      :main_contextMenu_editArtist => :enabled,
      :main_contextMenu_launchFile => :enabled,
      :main_contextMenu_openFolder => :enabled,
      :main_contextMenu_openPrompt => :enabled,
      :main_contextMenu_removeItems => :enabled,
    }
  end
  
  def test_context_menu_with_mp3s_across_albums_selected
    add_files "sample_data"
    select_items_in_tree @inputTree_tree, *[
      [/.*sample_data$/, /complete.blah/, /01\.mp3/],
      [/.*sample_data$/, /incomplete/, /1/, /01\.mp3/],
    ]
    assert_context_menu @inputTree_contextMenu, { 
      :main_contextMenu_editAlbum => :disabled,
      :main_contextMenu_editArtist => :enabled,
      :main_contextMenu_launchFile => :disabled,
      :main_contextMenu_openFolder => :disabled,
      :main_contextMenu_openPrompt => :disabled,
      :main_contextMenu_removeItems => :enabled,
    }
  end
  
  #################################################################################################
  private
    def add_files(*files)
      paths= files.map{|f| getTestResourcePath f}
      @wnd.add paths.to_java(:String)
      dispatch_swt_tasks!
    end
    
    def assert_context_menu(menu, checks)
      raise "Hash expected but was #{checks.inspect}" unless checks.is_a?(Hash)
      
      # Send event to show menu
      e= org.eclipse.swt.widgets.Event.new
      e.widget= @inputTree_tree
      menu.notifyListeners(SWT::Show, e)
      
      # Cache menu items
      items= {}
      menu.getItems.each {|mi|
        items[mi.getText.sub(/\t.+/,'')]= mi
      }
  
      # Perform each declared check
      checks.each {|k,v|
        # Find the menu item to test
        item= case k
          when Fixnum then menu.getItem(k)
          when Symbol then items[items.keys.select{|n| n == I18n.l(k.to_s)}.first]
          when String then items[items.keys.select{|n| n == k}.first]
          when Regexp then items[items.keys.select{|n| n =~ k}.first]
          else raise "Unrecognised menu item identifier: #{k.inspect}"
        end
        raise "Menu item not found for #{k.inspect}\nAvailable items are:\n  #{items.keys.join "\n  "}" unless item
        
        # Perform single check
        v= [v] unless v.is_a?(Array)
        name= "Menu item '#{item.getText.gsub '&',''}'"
        v.each {|check|
          case check
            when :enabled  then assertTrue "#{name} should be enabled.", item.isEnabled
            when :disabled then assertFalse "#{name} should be disabled.", item.isEnabled
            else raise "Unrecognised check: #{check.inspect}"
          end
        }
      }
    end
end

AppWindowInputTreeTest.new
