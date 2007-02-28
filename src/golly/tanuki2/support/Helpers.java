package golly.tanuki2.support;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 16/02/2007
 */
public final class Helpers {
	public static class OptimisibleDirTreeNode {
		public Map<String, OptimisibleDirTreeNode> children= new HashMap<String, OptimisibleDirTreeNode>();
		public boolean hasFiles= false;
	}

	public static String addPathElement(final String path, final String name) {
		return path.length() == 0 ? name : path + File.separator + name;
	}

	public static <T> Set<T> arrayToSet(T[] array) {
		final Set<T> r= new HashSet<T>(array.length);
		for (T e : array)
			r.add(e);
		return r;
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
			while (i-- > 0) {
				fields[i]= cls.getDeclaredField(fieldNames[i]);
				fields[i].setAccessible(true);
			}
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

	public static final Pattern pROMAL_NUMERAL= Pattern.compile("^(?:M{0,3})(?:D?C{0,3}|C[DM])(?:L?X{0,3}|X[LC])(?:V?I{0,3}|I[VX])$", Pattern.CASE_INSENSITIVE); //$NON-NLS-1$

	/**
	 * Returns <code>true</code> if a given string is a valid roman numeral.
	 */
	public static boolean isRomanNumeral(String text) {
		return pROMAL_NUMERAL.matcher(text).matches();
	}

	/**
	 * Works like <code>Array.join()</code> in Ruby.
	 */
	public static String join(final String[] array, final String joinWith) {
		StringBuilder sb= new StringBuilder();
		for (String a : array) {
			if (sb.length() != 0 && joinWith != null)
				sb.append(joinWith);
			sb.append(a);
		}
		return sb.toString();
	}

	private static final Pattern pTITLECASE_B= Pattern.compile("\\b"); //$NON-NLS-1$
	private static final Pattern pTITLECASE_HASW= Pattern.compile(".*\\w.*"); //$NON-NLS-1$
	private static final Pattern pTITLECASE_ICAP= Pattern.compile("^['\"\\(\\[']*(\\w).*"); //$NON-NLS-1$
	private static final Pattern pTITLECASE_PREPOST= Pattern.compile("^(\\W*)(.*?)(\\W*)$"); //$NON-NLS-1$
	private static final Set<String> ARTICLES, COORDINATING_CONJUNCTIONS, COMMON_PREPOSITIONS;
	private static final Set<String> TITLECASE_EXCEPTIONS;
	static {
		final String[] saARTICLES= new String[] {"a", "an", "the"};
		final String[] saCOORDINATING_CONJUNCTIONS= new String[] {"and", "but", "for", "nor", "or", "so", "yet"};
		final String[] saCOMMON_PREPOSITIONS= new String[] {"about", "beneath", "in", "regarding", "above", "beside",
				"inside", "round", "across", "between", "into", "since", "after", "beyond", "like", "through",
				"against", "by", "near", "to", "among", "concerning", "of", "toward", "around", "despite", "off",
				"under", "as", "down", "on", "unlike", "at", "during", "out", "until", "before", "except", "outside",
				"up", "behind", "for", "over", "upon", "below", "from", "past", "with"};
		ARTICLES= arrayToSet(saARTICLES);
		COORDINATING_CONJUNCTIONS= arrayToSet(saCOORDINATING_CONJUNCTIONS);
		COMMON_PREPOSITIONS= arrayToSet(saCOMMON_PREPOSITIONS);
		TITLECASE_EXCEPTIONS= new HashSet<String>();
		TITLECASE_EXCEPTIONS.addAll(ARTICLES);
		TITLECASE_EXCEPTIONS.addAll(COORDINATING_CONJUNCTIONS);
		TITLECASE_EXCEPTIONS.addAll(COMMON_PREPOSITIONS);
	}

	public static String makeTitleCase(String text) {
		final Matcher m= pTITLECASE_PREPOST.matcher(text);
		if (!m.matches())
			return text;
		String[] b= pTITLECASE_B.split(m.group(2).toLowerCase());
		if (b[0].length() == 0) {
			String[] b2= new String[b.length - 1];
			System.arraycopy(b, 1, b2, 0, b.length - 1);
			b= b2;
		}
		final int blen= b.length;
		final int lastIndex= blen - 1;
		String[] t= new String[blen];
		int i= -1;
		for (String w : b) {
			i++;
			if ((i != lastIndex && w.length() == 1 && ".".equals(b[i + 1])) || isRomanNumeral(w)) //$NON-NLS-1$
				t[i]= w.toUpperCase();
			else if (i == 0 || i == lastIndex)
				t[i]= makeTitleCase_iCap(w);
			else if (TITLECASE_EXCEPTIONS.contains(w) || ((i > 1) && "'".equals(b[i - 1]) && pTITLECASE_HASW.matcher(b[i - 2]).matches())) //$NON-NLS-1$
				t[i]= w;
			else
				t[i]= makeTitleCase_iCap(w);
		}
		return m.group(1) + join(t, null) + m.group(3);
	}

	private static String makeTitleCase_iCap(String w) {
		final Matcher m= pTITLECASE_ICAP.matcher(w);
		if (m.matches()) {
			char[] chars= w.toCharArray();
			chars[m.start(1)]= Character.toUpperCase(chars[m.start(1)]);
			return new String(chars);
		} else
			return w;
	}

	/**
	 * Works like <code>Array.map()</code> in Ruby although instead of using a closure, strings are simply added to
	 * the beginning and end of array elements.
	 */
	public static String[] map(final String[] array, final String pre, final String post) {
		int i= array.length;
		String[] x= new String[i];
		while (i-- > 0)
			x[i]= pre + array[i] + post;
		return x;
	}

	public static <K, V> void mergeCollectionMap(Map<K, Collection<V>> main, Map<K, Collection<V>> newContent) {
		if (newContent != null)
			for (K k : newContent.keySet())
				if (main.containsKey(k))
					main.get(k).addAll(newContent.get(k));
				else
					main.put(k, newContent.get(k));
	}

	public static <K, V> void mergeListMap(Map<K, List<V>> main, Map<K, List<V>> newContent) {
		if (newContent != null)
			for (K k : newContent.keySet())
				if (main.containsKey(k))
					main.get(k).addAll(newContent.get(k));
				else
					main.put(k, newContent.get(k));
	}

	/**
	 * @see #normalizeText(String, List)
	 */
	public static final String normalizeText(final String input) {
		return normalizeText(input, null);
	}

	private static final String KATAKANA_HALF= "ｱｲｳｴｵｧｨｩｪｫｶｷｸｹｺｻｼｽｾｿﾀﾁﾂｯﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖｬｭｮﾗﾘﾙﾚﾛﾜｦﾝｰﾞﾟ"; //$NON-NLS-1$
	private static final String KATAKANA_FULL= "アイウエオァィゥェォカキクケコサシスセソタチツッテトナニヌネノハヒフヘホマミムメモヤユヨャュョラリルレロワヲンー゛゜"; //$NON-NLS-1$
	private static final String KATAKANA_TENTEN= "カキクケコサシスセソタチツテトハヒフヘホ"; //$NON-NLS-1$

	/**
	 * Performs the following conversions:
	 * <ul>
	 * <li>Converts all 全角英字/数字 to ASCII</li>
	 * <li>Converts ひらがな and 半角カタカナ to 全角カタカナ</li>
	 * <li>Converts to uppercase</li>
	 * <li>Removes all whitespace</li>
	 * <li>Converts all whitespace to ASCII spaces</li>
	 * </ul>
	 */
	public static final String normalizeText(final String input, List<Integer> map) {
		final StringBuilder sb= new StringBuilder();
		final int inputLength= input.length() - 1;
		boolean ignoreNextMapChar= false;
		int lastNonWhitespacePos= 0;
		char c;
		for (int i= 0; i <= inputLength; i++) {
			c= input.charAt(i);

			if (!Character.isWhitespace(c)) {

				if (c >= 'Ａ' && c <= 'Ｚ') // 全角大文字
					c-= ('Ａ' - 'A');

				else if (c >= 'ａ' && c <= 'ｚ') // 全角小文字
					c-= ('ａ' - 'A');

				else if (c >= '０' && c <= '９') // 全角数字
					c-= ('０' - '0');

				else if (c >= 0x3041 && c <= 0x3096) // ひらがな
					c-= (0x3041 - 0x30A1);

				else if (c > 0xFF60 && c < 0xFFA0) { // 半角ｶﾀｶﾅ
					// 全角ｶﾀｶﾅに変換
					int index= KATAKANA_HALF.indexOf(c);
					if (index >= 0)
						c= KATAKANA_FULL.charAt(index);
					// ﾃﾝﾃﾝ
					if (c == '゛') {
						final int sblen= sb.length() - 1;
						c= sb.charAt(sblen);
						if (KATAKANA_TENTEN.indexOf(c) != -1)
							c++;
						else if (c == 'ウ')
							c= 'ヴ';
						else if (c == 'ワ')
							c= 0x30f7;
						else
							continue;
						sb.deleteCharAt(sblen);
						if (map != null)
							ignoreNextMapChar= true;
					}
					// ﾏﾙ
					else if (c == '゜') {
						final int sblen= sb.length() - 1;
						c= sb.charAt(sblen);
						if (c == 'ハ' || c == 'ヒ' || c == 'フ' || c == 'ヘ' || c == 'ホ') {
							c++;
							c++;
							sb.deleteCharAt(sblen);
							if (map != null)
								ignoreNextMapChar= true;
						} else
							continue;
					}

				} else
					c= Character.toUpperCase(c);

				sb.append(c);
				if (map != null) {
					lastNonWhitespacePos= i;
					if (ignoreNextMapChar)
						ignoreNextMapChar= false;
					else
						map.add(i);
				}
			}
		}

		if (map != null)
			map.add(lastNonWhitespacePos);

		assert (map == null ? true : map.size() == sb.length() + 1);
		return sb.toString();
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
	public static Map<String, Map> optimiseDirTree(final Map<String, OptimisibleDirTreeNode> tree) {
		final Map<String, Map> root= new HashMap<String, Map>();
		Map<String, Map> r= root;
		optimiseDirTree("", r, tree); //$NON-NLS-1$
		if (r.size() == 1 && r.keySet().iterator().next().length() == 0) {
			r= r.get(""); //$NON-NLS-1$
			if (r == null) {
				r= root;
				r.clear();
			}
		}
		return r;
	}

	@SuppressWarnings("unchecked")
	private static void optimiseDirTree(String path, Map<String, Map> target, Map<String, OptimisibleDirTreeNode> sourceNodes) {
		switch (sourceNodes.size()) {
		case 0: {
			// Has no children
			// Add to target with no child-node
			target.put(path, null);
			break;
		}
		case 1: {
			// Has one child
			// Don't add to target, extend path and optimise child
			final String name= sourceNodes.keySet().iterator().next();
			optimiseDirTree_add(Helpers.addPathElement(path, name), target, sourceNodes.get(name));
			break;
		}
		default: {
			// Has more than one child
			// Create node in target and add children to it
			Map<String, Map> currentNode;
			if (path.length() == 0)
				currentNode= target;
			else {
				currentNode= target.get(path);
				if (currentNode == null)
					target.put(path, currentNode= new HashMap<String, Map>());
			}
			for (String name : sourceNodes.keySet())
				optimiseDirTree_add(name, currentNode, sourceNodes.get(name));
			break;
		}
		}
	}

	@SuppressWarnings("unchecked")
	private static void optimiseDirTree_add(String path, Map<String, Map> target, OptimisibleDirTreeNode node) {
		if (node.hasFiles) {
			if (node.children.size() == 0)
				target.put(path, null);
			else {
				Map<String, Map> currentNode= target.get(path);
				if (currentNode == null)
					target.put(path, currentNode= new HashMap<String, Map>());
				optimiseDirTree("", currentNode, node.children); //$NON-NLS-1$
			}
		} else
			optimiseDirTree(path, target, node.children);
	}

	/**
	 * Creates a regex string that is an OR combination of the arguents passed.<br>
	 * eg. <code>regexOr("abc","def","asd")</code> returns <code>"(?:(?:abc)|(?:def)|(?:asd))"</code>.
	 */
	@SuppressWarnings("nls")
	public static String regexOr(String... array) {
		return "(?:" + join(map(array, "(?:", ")"), "|") + ")";
	}

	public static void removeEmptyCollections(Map<?, ? extends Collection<?>> map) {
		Object[] keys= map.keySet().toArray();
		for (Object k : keys)
			if (map.get(k) == null || map.get(k).isEmpty())
				map.remove(k);
	}

	public static void removeEmptyMaps(Map<?, ? extends Map<?, ?>> map) {
		Object[] keys= map.keySet().toArray();
		for (Object k : keys)
			if (map.get(k) == null || map.get(k).isEmpty())
				map.remove(k);
	}

	/**
	 * Takes a <code>Set</code>, and returns a sorted array.
	 */
	public static String[] sort(final Set<String> data) {
		String[] r= data.toArray(new String[data.size()]);
		Arrays.sort(r);
		return r;
	}

	public static final String whitespaceChars= "\u0020\u3000\n\r\u0009\u000b\u000c\u001c\u001d\u001e\u001f\u1680\u180e\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2008\u2009\u200a\u200b\u2028\u2029\u205f"; //$NON-NLS-1$
	private static final Pattern ptnUnicodeTrim= Pattern.compile("^[" + whitespaceChars + "]+|[" + whitespaceChars + "]+$"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

	/**
	 * Removes whitespace from the beginning and end of a string.<br>
	 * Unicode-aware.
	 */
	public static String unicodeTrim(String text) {
		return ptnUnicodeTrim.matcher(text).replaceAll(""); //$NON-NLS-1$
	}
}
