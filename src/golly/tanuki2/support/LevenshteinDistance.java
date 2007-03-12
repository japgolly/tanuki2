package golly.tanuki2.support;

/**
 * Class that calculates the Levenshtein distance of two strings.<br>
 * Taken from http://www.merriampark.com/ld.htm
 */
@SuppressWarnings("nls")
public final class LevenshteinDistance {

	/**
	 * Returns the minimum of three values.
	 */
	private static int minimum(final int a, final int b, final int c) {
		int mi= a;
		if (b < mi)
			mi= b;
		if (c < mi)
			mi= c;
		return mi;
	}

	/**
	 * Compute Levenshtein distance. (original implementation)
	 * 
	 * @param s source string.
	 * @param t target string.
	 */
	public static int calculateDistance(final String s, final String t) {
		int d[][]; // matrix
		int n; // length of s
		int m; // length of t
		int i; // iterates through s
		int j; // iterates through t
		char s_i; // ith character of s
		char t_j; // jth character of t
		int cost; // cost

		// Step 1
		n= s.length();
		m= t.length();
		if (n == 0)
			return m;
		if (m == 0)
			return n;
		d= new int[n + 1][m + 1];

		// Step 2
		for (i= 0; i <= n; i++)
			d[i][0]= i;
		for (j= 0; j <= m; j++)
			d[0][j]= j;

		// Step 3
		for (i= 1; i <= n; i++) {
			s_i= s.charAt(i - 1);

			// Step 4
			for (j= 1; j <= m; j++) {
				t_j= t.charAt(j - 1);

				// Step 5
				if (s_i == t_j)
					cost= 0;
				else
					cost= 1;

				// Step 6
				d[i][j]= minimum(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + cost);
			}
		}

		// Step 7
		return d[n][m];
	}

	/**
	 * Compute Levenshtein distance. (alternative implementation by Chas Emerick)
	 * <p>
	 * <em>"While attempting to use it with two very large strings, I encountered an OutOfMemoryError, due to the fact
	 * that a matrix is created with the dimensions of the two strings' lengths. ... I modified your implementation to
	 * use two single-dimensional arrays; this is clearly more memory-friendly (although it probably results in some
	 * very slight performance degradation when comparing smaller strings)."</em>
	 * </p>
	 * 
	 * @param s source string.
	 * @param t target string.
	 */
	public static int calculateDistance2(String s, String t) {
		if (s == null || t == null) {
			throw new IllegalArgumentException("Strings must not be null");
		}

		/*
		 * The difference between this impl. and the previous is that, rather than creating and retaining a matrix of
		 * size s.length()+1 by t.length()+1, we maintain two single-dimensional arrays of length s.length()+1. The
		 * first, d, is the 'current working' distance array that maintains the newest distance cost counts as we
		 * iterate through the characters of String s. Each time we increment the index of String t we are comparing, d
		 * is copied to p, the second int[]. Doing so allows us to retain the previous cost counts as required by the
		 * algorithm (taking the minimum of the cost count to the left, up one, and diagonally up and to the left of the
		 * current cost count being calculated). (Note that the arrays aren't really copied anymore, just
		 * switched...this is clearly much better than cloning an array or doing a System.arraycopy() each time through
		 * the outer loop.) Effectively, the difference between the two implementations is this one does not cause an
		 * out of memory condition when calculating the LD over two very large strings.
		 */

		int n= s.length(); // length of s
		int m= t.length(); // length of t

		if (n == 0)
			return m;
		else if (m == 0)
			return n;

		int p[]= new int[n + 1]; //'previous' cost array, horizontally
		int d[]= new int[n + 1]; // cost array, horizontally
		int _d[]; //placeholder to assist in swapping p and d

		// indexes into strings s and t
		int i; // iterates through s
		int j; // iterates through t
		char t_j; // jth character of t
		int cost; // cost

		for (i= 0; i <= n; i++)
			p[i]= i;

		for (j= 1; j <= m; j++) {
			t_j= t.charAt(j - 1);
			d[0]= j;

			for (i= 1; i <= n; i++) {
				cost= s.charAt(i - 1) == t_j ? 0 : 1;
				// minimum of cell to the left+1, to the top+1, diagonally left and up +cost
				d[i]= minimum(d[i - 1] + 1, p[i] + 1, p[i - 1] + cost);
			}

			// copy current distance counts to 'previous row' distance counts
			_d= p;
			p= d;
			d= _d;
		}

		// our last action in the above loop was to switch d and p, so p now 
		// actually has the most recent cost counts
		return p[n];
	}
}
