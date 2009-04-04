package golly.tanuki2.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Golly
 * @since 13/08/2007
 */
public class Version implements Comparable<Version> {
	private final static Pattern VERSION_STRING_PATTERN= Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)\\.(\\d+)$"); //$NON-NLS-1$
	public final int major, minor, tiny, tiny2;
	private final String toString;

	public Version(int major, int minor, int tiny, int tiny2) {
		this.major= major;
		this.minor= minor;
		this.tiny= tiny;
		this.tiny2= tiny2;
		this.toString= genToString();
	}

	public Version(final String versionStr) {
		Matcher m= VERSION_STRING_PATTERN.matcher(versionStr.trim());
		if (!m.matches()) {
			throw new IllegalArgumentException();
		}
		this.major= Integer.parseInt(m.group(1));
		this.minor= Integer.parseInt(m.group(2));
		this.tiny= Integer.parseInt(m.group(3));
		this.tiny2= Integer.parseInt(m.group(4));
		this.toString= genToString();
	}

	private String genToString() {
		return "" + major + "." + minor + "." + tiny + "." + tiny2; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public int compareTo(Version v2) {
		if (major != v2.major) {
			return major > v2.major ? 1 : -1;
		}

		if (minor != v2.minor) {
			return minor > v2.minor ? 1 : -1;
		}

		if (tiny != v2.tiny) {
			return tiny > v2.tiny ? 1 : -1;
		}

		if (tiny2 != v2.tiny2) {
			return tiny2 > v2.tiny2 ? 1 : -1;
		}

		return 0;
	}

	@Override
	public int hashCode() {
		final int prime= 31;
		int result= 1;
		result= prime * result + major;
		result= prime * result + minor;
		result= prime * result + tiny;
		result= prime * result + tiny2;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final Version other= (Version) obj;
		if (major != other.major) {
			return false;
		}
		if (minor != other.minor) {
			return false;
		}
		if (tiny != other.tiny) {
			return false;
		}
		if (tiny2 != other.tiny2) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return toString;
	}
}
