package golly.tanuki2.support;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Properties;

/**
 * @author Golly
 * @since 17/02/2007
 */
@SuppressWarnings("nls")
public class Config {

	public static int appwndX= -1, appwndY= -1, appwndWidth= -1, appwndHeight= -1;
	public static boolean appwndMaximised= false;

	// =============================================================================================== //
	private static final String FILENAME= "settings.xml";
	private static final String SETTINGS_DESC= "Tanuki2 Settings";

	/**
	 * Loads the saved configuration if it is available.
	 */
	public static void load() throws IOException {
		Properties prop= new Properties();
		try {
			prop.loadFromXML(new FileInputStream(FILENAME));
		} catch (FileNotFoundException e) {
			return;
		}
		for (Field f : Config.class.getFields())
			readProperty(prop, f.getName());
	}

	/**
	 * Saves the current configuration so that it can be restored on next load.
	 */
	public static void save() throws IOException {
		Properties prop= new Properties();
		for (Field f : Config.class.getFields())
			saveProperty(prop, f.getName());

		FileOutputStream fos= new FileOutputStream(FILENAME);
		prop.storeToXML(fos, SETTINGS_DESC);
		fos.close();
	}

	/**
	 * Just a helper function that calls {@link #save()} with error-handling.
	 */
	public static void tryToSave() {
		try {
			Config.save();
		} catch (IOException e) {
			new TanukiException(e).showErrorDialog();
		}
	}

	@SuppressWarnings("unchecked")
	private static void readProperty(Properties p, final String name) {
		try {
			if (p.containsKey(name)) {
				final String v= p.getProperty(name);
				final Field f= Config.class.getDeclaredField(name);
				final Class type= f.getType();
				// String
				if (type.equals(String.class)) {
					f.set(null, v);
				}
				// ArrayList<String> 
				else if (type.equals(ArrayList.class)) {
					f.set(null, stringToArray(v));
				}
				// int
				else if (type.equals(int.class)) {
					try {
						f.setInt(null, Integer.parseInt(v));
					} catch (NumberFormatException e) {
					}
				}
				// Integer
				else if (type.equals(Integer.class)) {
					try {
						f.set(null, new Integer(v));
					} catch (NumberFormatException e) {
					}
				}
				// boolean
				else if (type.equals(boolean.class)) {
					f.setBoolean(null, "1".equals(v));
				}
				// unsupported
				else
					throw new RuntimeException("Unsupported type: " + type.toString());

			}
			// Field f= Config.class.getDeclaredField(name);System.out.println(name+": "+f.get(null));
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	@SuppressWarnings("unchecked")
	private static void saveProperty(Properties p, final String name) {
		try {
			final Field f= Config.class.getDeclaredField(name);
			final Class type= f.getType();
			final Object v= f.get(null);
			if (v != null) {
				if (type.equals(String.class)) {
					// String
					p.setProperty(name, v.toString());
				} else if (type.equals(ArrayList.class)) {
					// ArrayList<String>
					ArrayList<String> v2= (ArrayList<String>) v;
					if (!v2.isEmpty())
						p.setProperty(name, arrayToString(v2));
				} else if (type.equals(Integer.class) || type.equals(int.class)) {
					// Integer + int
					p.setProperty(name, ((Integer) v).toString());
				} else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
					// Boolean + boolean
					p.setProperty(name, ((Boolean) v) ? "1" : "0");
				} else
					throw new RuntimeException("Unsupported type: " + type.toString());
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	private static String arrayToString(ArrayList<String> a) {
		StringBuilder sb= new StringBuilder();
		for (String s : a) {
			sb.append((char) 9);
			sb.append(s);
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}

	private static ArrayList<String> stringToArray(String s) {
		ArrayList<String> a= new ArrayList<String>();
		for (String s2 : s.split("\u0009"))
			a.add(s2);
		return a;
	}
}
