package golly.tanuki2.support;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.bind.DataBindingException;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Golly
 * @since 17/02/2007
 */
@XmlRootElement(name= "settings")
@SuppressWarnings("nls")
public class RuntimeConfig {

	public int appwndX= -1, appwndY= -1, appwndWidth= -1, appwndHeight= -1;
	public int voodooWndWidth= -1, voodooWndHeight= -1;
	public boolean appwndMaximised= false;
	public String targetDir= "";
	public String lastAddedDir= null;
	public boolean autoTitleCase= true;
	public boolean intelligentTitleCase= false;
	public String targetDirFormat= Helpers.ensureCorrectDirSeperators("[:artist:]/[:year:] - [:album:]");
	public String targetAudioFileFormat= "[:tn:]. [:track:]";

	/**
	 * This is an optional regex pattern that is used to filter out metadata with illegal artist names. In other words,
	 * when attempting to detect metadata for new files, any artists that match will pattern will be excluded.
	 * 
	 * @since 13/04/2009
	 */
	public String artistBlacklist= null;

	public boolean checkVersionOnStartup= true;

	// =============================================================================================== //
	private static RuntimeConfig INSTANCE= new RuntimeConfig();

	public static RuntimeConfig getInstance() {
		return INSTANCE;
	}

	public static JAXBContext getJAXBContext() throws JAXBException {
		final JAXBContext jaxbContext= JAXBContext.newInstance(RuntimeConfig.class);
		return jaxbContext;
	}

	private static final String DIRECTORY= OSSpecific.getTanukiSettingsDirectory();
	private static final String FILENAME= "settings.xml";
	private static final String FULL_FILENAME= Helpers.addPathElements(DIRECTORY, FILENAME);

	/**
	 * Loads the saved configuration if it is available.
	 */
	public static void load() throws IOException {
		try {
			final File file= new File(FULL_FILENAME);
			if (file.exists()) {
				INSTANCE= JAXB.unmarshal(file, RuntimeConfig.class);
			}
		} catch (Throwable e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * Saves the current configuration so that it can be restored on next load.
	 */
	public static void save() throws DataBindingException, JAXBException, IOException {
		final FileWriter fw= new FileWriter(FULL_FILENAME);
		final Marshaller marshaller= getJAXBContext().createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		marshaller.marshal(getInstance(), fw);
		fw.close();
	}

	/**
	 * Just a helper function that calls {@link #save()} with error-handling.
	 */
	public static void tryToSave() {
		try {
			RuntimeConfig.save();
		} catch (Throwable e) {
			if (e instanceof DataBindingException && e.getCause() != null) {
				e= e.getCause();
			}
//			if (e instanceof MarshalException && ((MarshalException) e).getLinkedException() != null) {
//				e= ((MarshalException) e).getLinkedException();
//			}
			new TanukiException(e).showErrorDialog();
		}
	}
}
