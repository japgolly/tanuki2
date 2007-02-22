package golly.tanuki2.support;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 16/02/2007
 */
public final class Helpers {

	public static String addPathElement(final String path, final String name) {
		return path.length() == 0 ? name : path + File.separator + name;
	}

	/**
	 * Checks whether an array contains a certain object.
	 */
	public static boolean contains(Object[] collection, Object x) {
		if (x == null) {
			for (Object c : collection)
				if (c == null)
					return true;
		} else {
			for (Object c : collection)
				if (x.equals(c))
					return true;
		}
		return false;
	}

	/**
	 * Takes a String array of field names and returns an array of {@link Field}s.
	 * 
	 * @throws RuntimeException if any excxeption occurs.
	 */
	public static Field[] getFields(final Object obj, String... fieldNames) {
		final Class<?> cls= obj.getClass();
		int i= fieldNames.length;
		final Field[] fields= new Field[i];
		try {
			while (i-- > 0)
				fields[i]= cls.getDeclaredField(fieldNames[i]);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return fields;
	}

	/**
	 * Same as {@link #inspect(Object, boolean, Field...)} except all instance variables will be displayed.
	 * 
	 * @see #inspect(Object, boolean, Field...)
	 */
	@SuppressWarnings("nls")
	public static String inspect(final Object obj, boolean includeObjectId) {
		return inspect(obj, includeObjectId, obj.getClass().getDeclaredFields());
	}

	/**
	 * @see #inspect(Object, boolean, Field...)
	 */
	public static String inspect(final Object obj, boolean includeObjectId, String... fieldNames) {
		return inspect(obj, includeObjectId, getFields(obj, fieldNames));
	}

	/**
	 * Similar to Ruby's inspect() method. Returns a string representation of an object that also shows values of
	 * instance variables.
	 * 
	 * @param obj the object to inspect.
	 * @param includeObjectId whether or not the <code>hashCode()</code> should be included in the string.
	 * @param fields the fields to inspect.
	 * @throws RuntimeException if any excxeption occurs.
	 */
	@SuppressWarnings("nls")
	public static String inspect(final Object obj, boolean includeObjectId, Field... fields) {
		if (obj == null)
			return "null";
		try {
			final HashMap<String, Field> fieldMap= new HashMap<String, Field>();
			for (Field f : fields)
				fieldMap.put(f.getName(), f);

			StringBuilder sb= new StringBuilder();
			sb.append('{');
			sb.append(obj.getClass().getSimpleName());
			if (includeObjectId) {
				sb.append(':');
				sb.append(Integer.toHexString(obj.hashCode()));
			}
			sb.append(' ');

			boolean first= true;
			for (String fn : Helpers.sort(fieldMap.keySet())) {
				final Field f= fieldMap.get(fn);
				if (first)
					first= false;
				else {
					sb.append(',');
					sb.append(' ');
				}
				sb.append('@');
				sb.append(fn);
				sb.append('=');
				sb.append(' ');

				f.setAccessible(true);
				final Class<?> cls= f.getType();
				final Object o= f.get(obj);
				if (o == null)
					sb.append("null");
				else if (String.class.equals(cls)) {
					sb.append('\"');
					sb.append(((String) o).replace("\\", "\\\\").replace("\"", "\\\""));
					sb.append('\"');
				} else
					sb.append(o.toString());
			}
			sb.append('}');
			return sb.toString();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Same as {@link #inspect(Object, boolean, Field...)} except instead of specifying which instance variables to
	 * inspect, all instance variables are inspected except for those passed as arguments here.
	 * 
	 * @see #inspect(Object, boolean, Field...)
	 */
	public static String inspectExcept(final Object obj, boolean includeObjectId, String... fieldNames) {
		Set<Field> fields= new HashSet<Field>();
		for (Field f : obj.getClass().getDeclaredFields())
			if (!contains(fieldNames, f.getName()))
				fields.add(f);
		return inspect(obj, includeObjectId, fields.toArray(new Field[fields.size()]));
	}

	/**
	 * Takes a virtual directory tree in the following format:<br>
	 * 
	 * <pre>
	 * {
	 *   'c:' =&gt; {
	 *     'music' =&gt; {
	 *       'new stuff' =&gt; {
	 *         'napster' =&gt; {}, 
	 *         'winmx' =&gt; {},
	 *       },
	 *       'old stuff' =&gt; {
	 *         '2006' =&gt; {
	 *           'burnt' =&gt; {},
	 *         },
	 *       },
	 *     },
	 *   },
	 * }
	 * </pre>
	 * 
	 * and optimises it so that it becomes:<br>
	 * 
	 * <pre>
	 * {
	 *   'c:\music' =&gt; {
	 *     'new stuff' =&gt; {
	 *       'napster' =&gt; {}, 
	 *       'winmx' =&gt; {},
	 *     },
	 *     'old stuff\2006\burnt' =&gt; {},
	 *   },
	 * }
	 * </pre>
	 */
	@SuppressWarnings("unchecked")
	public static HashMap<String, HashMap> optimiseDirTree(HashMap<String, HashMap> tree) {
		// FIXME This reduces tree to c:/2/nevermore/xxxx but wot if there r mp3s in c:/2 ??
		HashMap<String, HashMap> r= new HashMap<String, HashMap>();
		optimiseDirTree(tree, "", r); //$NON-NLS-1$
		if (r.size() == 1 && r.keySet().iterator().next().length() == 0)
			r= r.get(""); //$NON-NLS-1$
		return r;
	}

	@SuppressWarnings("unchecked")
	private static void optimiseDirTree(HashMap<String, HashMap> tree, String path, HashMap<String, HashMap> target) {
		switch (tree.size()) {
		case 0: {
			target.put(path, null);
			break;
		}
		case 1: {
			final String name= tree.keySet().iterator().next();
			optimiseDirTree(tree.get(name), Helpers.addPathElement(path, name), target);
			break;
		}
		default: {
			HashMap<String, HashMap> subTarget= target.get(path);
			if (subTarget == null)
				target.put(path, subTarget= new HashMap<String, HashMap>());
			for (String name : tree.keySet())
				optimiseDirTree(tree.get(name), name, subTarget);
			break;
		}
		}
	}

	/**
	 * Takes a <code>Set</code>, and returns a sorted array.
	 */
	public static String[] sort(final Set<String> data) {
		String[] r= data.toArray(new String[data.size()]);
		Arrays.sort(r);
		return r;
	}

	private static final String whitespaceChars= "\u0020\u3000\n\r\u0009\u000b\u000c\u001c\u001d\u001e\u001f\u1680\u180e\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2008\u2009\u200a\u200b\u2028\u2029\u205f"; //$NON-NLS-1$
	private static final Pattern ptnUnicodeTrim= Pattern.compile("^[" + whitespaceChars + "]+|[" + whitespaceChars + "]+$"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	/**
	 * Removes whitespace from the beginning and end of a string.<br>
	 * Unicode-aware.
	 */
	public static String unicodeTrim(String text) {
		return ptnUnicodeTrim.matcher(text).replaceAll(""); //$NON-NLS-1$
	}
}
