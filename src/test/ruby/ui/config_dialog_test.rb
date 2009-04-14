require "#$TANUKI2/helpers/swt_test"

class ConfigDialogTest < Tanuki2::SwtTest
  include_package 'golly.tanuki2.ui'
  import 'golly.tanuki2.support.RuntimeConfig'
  
  def setup
    super
    @cfg= RuntimeConfig.new
  end
  
  def teardown
    @cfg= @dlg= @iwAlbumBlacklist= @btnOk= nil
    super
  end
  
  #################################################################################################
  
  def test_albumBlacklist_setting
    # Init and open
    @cfg.artistBlacklist= nil
    create_dialog!
    assertEquals '', @iwAlbumBlacklist.getText

    # Set and save
    @iwAlbumBlacklist.setText '^(a|bc)$'
    click_ok!
    assertTrue "Updated flag should be set.", @dlg.isUpdated
    assertEquals '^(a|bc)$', @cfg.artistBlacklist
  end
  
  def test_albumBlacklist_unsetting
    # Init and open
    @cfg.artistBlacklist= 'asd.+'
    create_dialog!
    assertEquals 'asd.+', @iwAlbumBlacklist.getText
    
    # Set and save
    @iwAlbumBlacklist.setText ''
    click_ok!
    assertTrue "Updated flag should be set.", @dlg.isUpdated
    assertNull "Artist blacklist should be null.", @cfg.artistBlacklist
  end
  
  def test_albumBlacklist_validates_regex
    old_value= @cfg.artistBlacklist
    create_dialog!
    @iwAlbumBlacklist.setText 'asd('
    click_ok!
    assertFalse "Config shouldn't have been updated with incorrect artist blacklist regex.", @dlg.isUpdated
    assertEquals old_value, @cfg.artistBlacklist
  end
  
  #################################################################################################
  private
    def create_dialog!
      @dlg= ConfigDialog.new(shell, @cfg)
      @iwAlbumBlacklist= get_field_value @dlg, :iwAlbumBlacklist
      @btnOk= get_field_value @dlg, :btnOk
    end
    
    def click_ok!
      e= org.eclipse.swt.widgets.Event.new
      e.widget= @btnOk
      @btnOk.notifyListeners(SWT::Selection, e)
    end
end

ConfigDialogTest.new
