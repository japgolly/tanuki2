package golly.tanuki2.qa;

import golly.tanuki2.data.AlbumData;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("nls")
public class DataTest extends Assert {

	@Test
	public void testAlbumData() {
		AlbumData ad1= new AlbumData(), ad2= new AlbumData();
		assertTrue(ad1.equals(ad2));
		assertTrue(ad1.hashCode() == ad2.hashCode());
		ad1.setAlbum("album");
		assertFalse(ad1.equals(ad2));
		assertTrue(ad1.hashCode() != ad2.hashCode());
		ad1.setAlbum("abc");
		ad1.setYear(1980);
		ad1.setArtist("qwe");
		ad2.setAlbum("abc");
		ad2.setYear(1980);
		ad2.setArtist("qwe");

		assertTrue(ad1.equals(ad2));
		assertTrue(ad1.hashCode() == ad2.hashCode());
		ad2.setArtist(null);
		assertFalse(ad1.equals(ad2));
		assertTrue(ad1.hashCode() != ad2.hashCode());
		ad1.setArtist(null);
		assertTrue(ad1.equals(ad2));
		assertTrue(ad1.hashCode() == ad2.hashCode());

		ad2.setYear((Integer)null);
		assertFalse(ad1.equals(ad2));
		assertTrue(ad1.hashCode() != ad2.hashCode());
		ad1.setYear((Integer)null);
		assertTrue(ad1.equals(ad2));
		assertTrue(ad1.hashCode() == ad2.hashCode());

		ad2.setAlbum(null);
		assertFalse(ad1.equals(ad2));
		assertTrue(ad1.hashCode() != ad2.hashCode());
		ad1.setAlbum(null);
		assertTrue(ad1.equals(ad2));
		assertTrue(ad1.hashCode() == ad2.hashCode());

		ad2.setArtist("1");
		assertFalse(ad1.equals(ad2));
		assertTrue(ad1.hashCode() != ad2.hashCode());
		ad1.setArtist("2");
		assertFalse(ad1.equals(ad2));
		assertTrue(ad1.hashCode() != ad2.hashCode());

		Set<AlbumData> set= new HashSet<AlbumData>();
		set.add(new AlbumData());
		assertEquals(1, set.size());
		set.add(new AlbumData());
		assertEquals(1, set.size());
		set.add(ad1);
		assertEquals(2, set.size());
		set.add(ad1);
		assertEquals(2, set.size());
	}
}
