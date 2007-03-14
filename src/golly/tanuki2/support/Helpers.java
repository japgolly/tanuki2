package golly.tanuki2.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
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
@SuppressWarnings("nls")
public final class Helpers {
	public static class OptimisibleDirTreeNode {
		public Map<String, OptimisibleDirTreeNode> children= new HashMap<String, OptimisibleDirTreeNode>();
		public boolean hasFiles= false;
	}

	public static final String whitespaceChars= "\u0020\u3000\n\r\u0009\u000b\u000c\u001c\u001d\u001e\u001f\u1680\u180e\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2008\u2009\u200a\u200b\u2028\u2029\u205f";
	private static final String KATAKANA_FULL= "アイウエオァィゥェォカキクケコサシスセソタチツッテトナニヌネノハヒフヘホマミムメモヤユヨャュョラリルレロワヲンー゛゜";
	private static final String KATAKANA_HALF= "ｱｲｳｴｵｧｨｩｪｫｶｷｸｹｺｻｼｽｾｿﾀﾁﾂｯﾃﾄﾅﾆﾇﾈﾉﾊﾋﾌﾍﾎﾏﾐﾑﾒﾓﾔﾕﾖｬｭｮﾗﾘﾙﾚﾛﾜｦﾝｰﾞﾟ";
	private static final String KATAKANA_TENTEN= "カキクケコサシスセソタチツテトハヒフヘホ";
	private static final Pattern pGetFileExtention= Pattern.compile("^(?:.+[\\\\/])*[^\\\\/]+\\.([^\\\\/.]*)$");
	private static final Pattern pMakeFilenameSafe_naughtyChars= Pattern.compile("[\\\\/:*?<>|]");
	private static final Pattern pPathSeperator= Pattern.compile("[\\/\\\\]"); //$NON-NLS-1$
	private static final Pattern pRemoveFileExt= Pattern.compile("\\.[^.]*$");
	private static final Pattern pRomanNumeral= Pattern.compile("^(?:M{0,3})(?:D?C{0,3}|C[DM])(?:L?X{0,3}|X[LC])(?:V?I{0,3}|I[VX])$", Pattern.CASE_INSENSITIVE);
	private static final Pattern pTitleCase_b= Pattern.compile("\\b");
	private static final Pattern pTitleCase_hasw= Pattern.compile(".*\\w.*");
	private static final Pattern pTitleCase_icap= Pattern.compile("^['\"\\(\\[']*(\\w).*");
	private static final Pattern pTitleCase_prepost= Pattern.compile("^(\\W*)(.*?)(\\W*)$");
	private static final Pattern pUnicodeTrim= Pattern.compile("^[" + whitespaceChars + "]+|[" + whitespaceChars + "]+$");
	private static final Set<String> setTitleCase_articles, setTitleCase_coordinatingConjunctions,
			setTitleCase_commonPrepositions, setTitleCase_exceptions;

	static {
		final String[] saARTICLES= new String[] {"a", "an", "the"};
		final String[] saCOORDINATING_CONJUNCTIONS= new String[] {"and", "but", "for", "nor", "or", "so", "yet"};
		final String[] saCOMMON_PREPOSITIONS= new String[] {"about", "beneath", "in", "regarding", "above", "beside",
				"inside", "round", "across", "between", "into", "since", "after", "beyond", "like", "through",
				"against", "by", "near", "to", "among", "concerning", "of", "toward", "around", "despite", "off",
				"under", "as", "down", "on", "unlike", "at", "during", "out", "until", "before", "except", "outside",
				"up", "behind", "for", "over", "upon", "below", "from", "past", "with"};
		setTitleCase_articles= arrayToSet(saARTICLES);
		setTitleCase_coordinatingConjunctions= arrayToSet(saCOORDINATING_CONJUNCTIONS);
		setTitleCase_commonPrepositions= arrayToSet(saCOMMON_PREPOSITIONS);
		setTitleCase_exceptions= new HashSet<String>();
		setTitleCase_exceptions.addAll(setTitleCase_articles);
		setTitleCase_exceptions.addAll(setTitleCase_coordinatingConjunctions);
		setTitleCase_exceptions.addAll(setTitleCase_commonPrepositions);
	}

	public static OptimisibleDirTreeNode addDirToUnoptimisedDirTree(final Map<String, OptimisibleDirTreeNode> unoptimisedDirTree, String dir, boolean hasFiles) {
		Map<String, OptimisibleDirTreeNode> t= unoptimisedDirTree;
		OptimisibleDirTreeNode latestNode= null;
		for (String dirElement : splitDir(dir)) {
			latestNode= t.get(dirElement);
			if (latestNode == null) {
				latestNode= new OptimisibleDirTreeNode();
				t.put(dirElement, latestNode);
			}
			t= latestNode.children;
		}
		if (latestNode != null && hasFiles)
			latestNode.hasFiles= true;
		return latestNode;
	}

	public static String addPathElements(final String path, final String... elements) {
		StringBuilder sb= new StringBuilder();
		if (path != null)
			sb.append(path);
		for (String e : elements) {
			if (sb.length() != 0)
				sb.append(File.separatorChar);
			sb.append(e);
		}
		return sb.toString();
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
	 * Copies/renames a file.
	 */
	public static void cp(String srcFile, String destFile, boolean overwrite) throws FileNotFoundException, IOException {
		if (!overwrite)
			if (new File(destFile).isFile())
				return;
		FileChannel srcChannel= new FileInputStream(srcFile).getChannel();
		FileChannel dstChannel= new FileOutputStream(destFile).getChannel();
		dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
		srcChannel.close();
		dstChannel.close();
	}

	/**
	 * Recursively copies the contents of a whole directory to another directory.<br>
	 * If the target directory does not already exist it will be created.
	 * 
	 * @param exceptions A list of filenames that will not be copied. <b>NOTE:</b> wildcards are not supported and the
	 *            list is currently case-sensitive.
	 */
	public static void cp_r(File srcDir, File destDir, boolean overwrite, Set<String> exceptions) throws IOException {
		final String destDirPrefix= destDir.getPath() + File.separator;
		mkdir_p(destDir);
		for (File f : srcDir.listFiles())
			if (!exceptions.contains(f.getName())) {
				if (f.isFile())
					cp(f.toString(), destDirPrefix + f.getName(), overwrite);
				else if (f.isDirectory())
					cp_r(f, new File(destDirPrefix + f.getName()), overwrite, exceptions);
			}
	}

	/**
	 * @see #cp_r(File, File, boolean, Set)
	 */
	public static void cp_r(File srcDir, File destDir, boolean overwrite, String... exceptions) throws IOException {
		cp_r(srcDir, destDir, overwrite, arrayToSet(exceptions));
	}

	/**
	 * Replaces all forward-slashes with backward-slashes or vice-versa depending on the OS.
	 */
	public static String ensureCorrectDirSeperators(String filename) {
		if (File.separatorChar == '/')
			return filename.replace('\\', '/');
		else
			return filename.replace('/', '\\');
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

	public static String getFileExtention(String filename, boolean returnDotToo) {
		final Matcher m= pGetFileExtention.matcher(filename);
		if (!m.matches())
			return "";
		else
			return returnDotToo ? "." + m.group(1) : m.group(1);
	}

	/**
	 * Returns the system temp dir.
	 * 
	 * @throws RuntimeException if the temp dir could not be determined, doesn't exist or isn't a directory.
	 */
	public static String getSystemTempDir() {
		String tmpDir= System.getenv("TEMP");
		if (tmpDir == null)
			tmpDir= System.getenv("TMP");
		if (tmpDir == null)
			throw new RuntimeException("Could determine temp dir.");
		if (!(new File(tmpDir).isDirectory()))
			throw new RuntimeException(tmpDir + " either doesn't exist or is not a directory.");
		return tmpDir;
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
	 * @see #inspect(Object, boolean, Field...)
	 */
	public static String inspect(final Object obj, boolean includeObjectId, String... fieldNames) {
		return inspect(obj, includeObjectId, getFields(obj, fieldNames));
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
	 * Returns <code>true</code> if a given string is a valid roman numeral.
	 */
	public static boolean isRomanNumeral(String text) {
		return pRomanNumeral.matcher(text).matches();
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

	/**
	 * Replaces all file-system-unsafe characters with safe alternatives.
	 */
	public static String makeFilenameSafe(String filename) {
		// TODO makeFilenameSafe is win32 specific
		filename= filename.replace("\"", "''");
		return pMakeFilenameSafe_naughtyChars.matcher(filename).replaceAll("_");
	}

	public static String makeTitleCase(String text) {
		final Matcher m= pTitleCase_prepost.matcher(text);
		if (!m.matches())
			return text;
		String[] b= pTitleCase_b.split(m.group(2).toLowerCase());
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
			if ((i != lastIndex && w.length() == 1 && ".".equals(b[i + 1])) || isRomanNumeral(w))
				t[i]= w.toUpperCase();
			else if (i == 0 || i == lastIndex)
				t[i]= makeTitleCase_iCap(w);
			else if (setTitleCase_exceptions.contains(w) || ((i > 1) && "'".equals(b[i - 1]) && pTitleCase_hasw.matcher(b[i - 2]).matches()))
				t[i]= w;
			else
				t[i]= makeTitleCase_iCap(w);
		}
		return m.group(1) + join(t, null) + m.group(3);
	}

	private static String makeTitleCase_iCap(String w) {
		final Matcher m= pTitleCase_icap.matcher(w);
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
	 * Creates a directory and if neccessary all parent directories.
	 * 
	 * @return <code>true</code> if the path was created successfully; <code>false</code> if the directory already
	 *         exists.
	 * @throws IOException if <code>File.mkdirs()</code> fails, or if the dir already exists but isn't a directory.
	 */
	public static boolean mkdir_p(File dir) throws IOException {
		if (!dir.exists()) {
			if (!dir.mkdirs())
				throw new IOException("File.mkdirs() failed.");
			return true;
		} else if (!dir.isDirectory())
			throw new IOException("Cannot create path. " + dir.toString() + " already exists and is not a directory.");
		else
			return false;
	}

	/**
	 * @See #mkdir_p(File)
	 */
	public static boolean mkdir_p(String dir) throws IOException {
		return mkdir_p(new File(dir));
	}

	/**
	 * Renames/moves a file, overwriting the target if neccessary.
	 * 
	 * @throws IOException if anything goes wrong.
	 */
	public static void mv(File source, File target) throws IOException {
		if (source.equals(target))
			return;
		if (target.exists() && !target.canWrite())
			throw new IOException("Cannot move file. Target is read-only. (\"" + target.toString() + "\")");
		File tmp= new File(addPathElements(target.getParent(), "golly_java_mvhelper_tempfile_7816452937.tmp"));
		if (tmp.exists())
			tmp.delete();

		boolean normalRenameWorked= false;
		try {
			if (source.renameTo(tmp))
				normalRenameWorked= true;
			else
				cp(source.toString(), tmp.toString(), true);
			if (target.exists())
				if (!target.delete())
					throw new IOException("Failed to delete " + target.toString());
			tmp.renameTo(target);
			if (!normalRenameWorked)
				source.delete();
		} finally {
			if (tmp.exists()) {
				if (normalRenameWorked)
					tmp.renameTo(source);
				else
					tmp.delete();
			}
		}
	}

	/**
	 * @see #mv(File, File)
	 */
	public static void mv(String source, String target) throws IOException {
		mv(new File(source), new File(target));
	}

	/**
	 * @see #normalizeText(String, List)
	 */
	public static final String normalizeText(final String input) {
		return normalizeText(input, null);
	}

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
		optimiseDirTree("", r, tree);
		if (r.size() == 1 && r.keySet().iterator().next().length() == 0) {
			r= r.get("");
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
			optimiseDirTree_add(Helpers.addPathElements(path, name), target, sourceNodes.get(name));
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
				optimiseDirTree("", currentNode, node.children);
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

	public static String removeFilenameExtension(String filename) {
		return pRemoveFileExt.matcher(filename).replaceFirst("");
	}

	/**
	 * Removes a directory and all its contents.
	 */
	public static void rm_rf(File dir) throws IOException {
		for (File f : dir.listFiles())
			if (f.isFile()) {
				if (!f.delete())
					throw new IOException("Delete failed. (\"" + f + "\")");
			} else if (f.isDirectory())
				rm_rf(f);
		if (!dir.delete())
			throw new IOException("Delete failed. (\"" + dir + "\")");
	}

	/**
	 * @see #rm_rf(File)
	 */
	public static void rm_rf(String dir) throws IOException {
		rm_rf(new File(dir));
	}

	/**
	 * @see #rmdirPath(File, List)
	 */
	public static void rmdirPath(File path) throws IOException {
		rmdirPath(path, null);
	}

	/**
	 * Removes a directory if it's empty, and then its parent directory if that's empty and so on and so forth.
	 * 
	 * @throws IOException if for some reason an empty directory fails to be removed.
	 */
	public static void rmdirPath(File path, List<File> removedFiles) throws IOException {
		if (path.isDirectory() && path.list().length == 0) {
			if (!path.delete())
				throw new IOException("rmdir failed. (\"" + path + "\")");
			if (removedFiles != null)
				removedFiles.add(path);

			final File parent= path.getParentFile();
			if (parent != null)
				rmdirPath(parent, removedFiles);
		}
	}

	/**
	 * @see #rmdirPath(File, List)
	 */
	public static void rmdirPath(String path) throws IOException {
		rmdirPath(new File(path));
	}

	/**
	 * @see #rmdirPath(File, List)
	 */
	public static void rmdirPath(String path, List<File> removedFiles) throws IOException {
		rmdirPath(new File(path), null);
	}

	public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public static Integer[] sort(final Collection<Integer> data) {
		Integer[] r= data.toArray(new Integer[data.size()]);
		Arrays.sort(r);
		return r;
	}

	public static String[] sort(final Collection<String> data) {
		String[] r= data.toArray(new String[data.size()]);
		Arrays.sort(r);
		return r;
	}

	public static String[] splitDir(final String dir) {
		return pPathSeperator.split(dir);
	}

	/**
	 * Removes whitespace from the beginning and end of a string.<br>
	 * Unicode-aware.
	 */
	public static String unicodeTrim(String text) {
		return pUnicodeTrim.matcher(text).replaceAll("");
	}
}
