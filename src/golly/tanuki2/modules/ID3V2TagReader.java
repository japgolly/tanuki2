package golly.tanuki2.modules;

import golly.tanuki2.data.TrackPropertyMap;
import golly.tanuki2.data.TrackPropertyType;
import golly.tanuki2.support.AbstractTagReader;
import golly.tanuki2.support.RichRandomAccessFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 23/07/2007
 */
public class ID3V2TagReader extends AbstractTagReader {
	private static final class StringToTrackPropertyTypeMap extends HashMap<String, TrackPropertyType> {
		private static final long serialVersionUID= -1261757065727482970L;
	}

	private static final Charset ASCII= Charset.forName("ASCII"); //$NON-NLS-1$
	private static final Charset UTF8= Charset.forName("UTF-8"); //$NON-NLS-1$
	private static final Charset UTF16BE= Charset.forName("UTF-16BE"); //$NON-NLS-1$
	private static final Charset UTF16LE= Charset.forName("UTF-16LE"); //$NON-NLS-1$
	private static final Charset ISO_8859_1= Charset.forName("ISO-8859-1"); //$NON-NLS-1$

	private static final Map<TrackPropertyType, String[]> TPT2TAG;
	private static final StringToTrackPropertyTypeMap[] TAG2TPT;
	private static final Map<TrackPropertyType, String> TPT2TAGXXX;
	private static final Map<String, TrackPropertyType> TPTXXX2SYM;
	private static final char[] TXX= new char[] {'T', 'X', 'X'};
	private static final char[] TXXX= new char[] {'T', 'X', 'X', 'X'};
	private static final Pattern ID3V2_RTRIM= Pattern.compile("[\0 ]+$"); //$NON-NLS-1$
	private static final Pattern MERGED_VALUES_PATTERN= Pattern.compile("^(.+)/(.+)$"); //$NON-NLS-1$

	static {
		TPT2TAG= new HashMap<TrackPropertyType, String[]>();
		TPT2TAGXXX= new HashMap<TrackPropertyType, String>();
		initStatic();

		int i= TPT2TAG.values().iterator().next().length;
		TAG2TPT= new StringToTrackPropertyTypeMap[i];
		while (i-- > 0) {
			TAG2TPT[i]= new StringToTrackPropertyTypeMap();
			for (Map.Entry<TrackPropertyType, String[]> e : TPT2TAG.entrySet())
				TAG2TPT[i].put(e.getValue()[i], e.getKey());
		}

		TPTXXX2SYM= new HashMap<String, TrackPropertyType>();
		for (Map.Entry<TrackPropertyType, String> e : TPT2TAGXXX.entrySet())
			TPTXXX2SYM.put(e.getValue(), e.getKey());
	}

	@SuppressWarnings("nls")
	private static void initStatic() {
		TPT2TAG.put(TrackPropertyType.ARTIST, new String[] {"TP1", "TPE1", "TPE1"});
		TPT2TAG.put(TrackPropertyType.ALBUM, new String[] {"TAL", "TALB", "TALB"});
		TPT2TAG.put(TrackPropertyType.TN, new String[] {"TRK", "TRCK", "TRCK"});
		TPT2TAG.put(TrackPropertyType.TRACK, new String[] {"TT2", "TIT2", "TIT2"});
		TPT2TAG.put(TrackPropertyType.YEAR, new String[] {"TYE", "TYER", "TDRC"});
	}

	protected void readTags(final List<TrackPropertyMap> results) throws IOException {
		fin.seekTo(0).readFully(10);
		if (!compare(buf, 0, 'I', 'D', '3'))
			return;
		final byte version= buf[3];
		if (version < (byte) 2 || version > (byte) (2 + TAG2TPT.length - 1))
			return;
		final boolean ver2= (version == (byte) 2);
		final boolean useSynchsafe= (version >= (byte) 4);
		// Doesn't support extended headers
		final long totalTagSize= readInt(true, buf, 6);

		long pos= 0;
		final byte[] zeroId= ver2 ? new byte[] {0, 0, 0} : new byte[] {0, 0, 0, 0};
		final byte[] id= new byte[zeroId.length];
		final byte[] flags= new byte[2];
		final TrackPropertyMap tpm= new TrackPropertyMap();

		// Read all fields in tag
		while (true) {

			// Read key (ie. tag field type)
			fin.readFully(id);
			final long size;
			if (ver2) {
				size= readInt3(fin);
				pos+= 6;
			} else {
				size= readInt(useSynchsafe, fin);
				fin.readFully(flags);
				pos+= 10;
			}
			if (compare(id, 0, zeroId) || pos > totalTagSize)
				break;
			pos+= size;

			// Read value
			fin.readFully(size);
			final String value= readString(buf, (int) size, id[0] == (byte) 'T');
			if (compare(buf, 0, tagxxx(version))) {
				//            if frame[:value] =~ /^(.+?)\0(.+)$/
				//              self[tag2sym($1,true)]= $2
				//            else
				//              self[frame[:id]]= frame[:value]
				//            end
				//          else
			} else {
				final TrackPropertyType type= tpt2sym(version, id, false);
				if (type != null)
					tpm.put(type, value);
			}
		}

		// Unmerge merged values
		unmerge(tpm, TrackPropertyType.TN);

		// Save result
		results.add(tpm);
	}

	private static void unmerge(TrackPropertyMap tpm, TrackPropertyType type) {
		if (tpm.containsKey(type))
			tpm.put(type, MERGED_VALUES_PATTERN.matcher(tpm.get(type)).replaceFirst("$1")); //$NON-NLS-1$
	}

	private static String readString(byte[] buf, int len, boolean checkEncoding) {
		String str= null;
		if (!checkEncoding) {
			str= new String(buf, 0, len, UTF8);
		} else {
			final byte encoding= buf[0];
			int offset= 1;
			len-= 1;
			switch (encoding) {
			case 0:
				// ISO-8859-1 [ISO-8859-1]. Terminated with $00.
				str= new String(buf, offset, len, ISO_8859_1);
				break;
			case 1:
				// UTF-16 [UTF-16] encoded Unicode [UNICODE] with BOM. All strings in the same frame SHALL have the same byteorder. Terminated with $00 00.
				// Unicode strings must begin with the Unicode BOM ($FF FE or $FE FF) to identify the byte order.
				offset+= 2;
				len-= 2;
				if (buf[1] == 254 && buf[2] == 255)
					str= new String(buf, offset, len, UTF16BE);
				else
					str= new String(buf, offset, len, UTF16LE);
				break;
			case 2:
				// UTF-16BE [UTF-16] encoded Unicode [UNICODE] without BOM. Terminated with $00 00.
				// Untested
				str= new String(buf, offset, len, UTF16BE);
				break;
			case 3:
				// UTF-8 [UTF-8] encoded Unicode [UNICODE]. Terminated with $00.
				str= new String(buf, offset, len, UTF8);
				break;
			}
		}
		if (str != null)
			str= ID3V2_RTRIM.matcher(str).replaceFirst(""); //$NON-NLS-1$
		return str;
	}

	private static char[] tagxxx(final byte version) throws IOException {
		switch (version) {
		case 2:
			return TXX;
		case 3:
		case 4:
			return TXXX;
		default:
			throw new RuntimeException();
		}
	}

	private static TrackPropertyType tpt2sym(byte version, byte[] tag, boolean extendedTag) {
		String tagStr= new String(tag, ASCII);
		if (extendedTag)
			return TPTXXX2SYM.get(tagStr);
		else
			return TAG2TPT[(version) - 2].get(tagStr);
	}

	private static long readInt3(RichRandomAccessFile fin) throws IOException {
		final byte[] tmp= new byte[] {0, 0, 0, 0};
		fin.readFully(tmp, 1, 3);
		return readInt(false, tmp, 0);
	}

	private static long readInt(boolean synchsafe, RichRandomAccessFile fin) throws IOException {
		final byte[] tmp= new byte[4];
		fin.readFully(tmp);
		return readInt(synchsafe, tmp, 0);
	}

	private static long readInt(boolean synchsafe, byte[] buffer, int from) {
		final long x0= (long) buffer[from];
		final long x1= (long) buffer[from + 1];
		final long x2= (long) buffer[from + 2];
		final long x3= (long) buffer[from + 3];
		if (synchsafe)
			return (x3 & 127) | ((x2 & 127) << 7) | ((x1 & 127) << 14) | ((x0 & 127) << 21);
		else
			return x3 | (x2 << 8) | (x1 << 16) | (x0 << 24);
	}
}
