package golly.tanuki2.qa;

import static golly.tanuki2.support.Helpers.ensureCorrectDirSeperators;
import golly.tanuki2.support.Helpers;
import golly.tanuki2.support.Helpers.OptimisibleDirTreeNode;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

@SuppressWarnings( {"nls", "unchecked"})
public class HelperTest extends TestHelper {

	@Test
	public void testOptimiseDirTree1() {
		Map<String, OptimisibleDirTreeNode> s= new HashMap<String, OptimisibleDirTreeNode>();
		OptimisibleDirTreeNode sC= addOptimisibleDirTreeNode(s, "c:");
		OptimisibleDirTreeNode sC_music= addOptimisibleDirTreeNode(sC, "music");
		OptimisibleDirTreeNode sC_music_newStuff= addOptimisibleDirTreeNode(sC_music, "new stuff");
		OptimisibleDirTreeNode sC_music_newStuff_napster= addOptimisibleDirTreeNode(sC_music_newStuff, "napster");
		sC_music_newStuff_napster.hasFiles= true;
		OptimisibleDirTreeNode sC_music_newStuff_winmx= addOptimisibleDirTreeNode(sC_music_newStuff, "winmx");
		sC_music_newStuff_winmx.hasFiles= true;
		OptimisibleDirTreeNode sC_music_oldStuff= addOptimisibleDirTreeNode(sC_music, "old stuff");
		OptimisibleDirTreeNode sC_music_oldStuff_2006= addOptimisibleDirTreeNode(sC_music_oldStuff, "2006");
		OptimisibleDirTreeNode sC_music_oldStuff_2006_burnt= addOptimisibleDirTreeNode(sC_music_oldStuff_2006, "burnt");
		sC_music_oldStuff_2006_burnt.hasFiles= true;

		Map<String, Map> r= Helpers.optimiseDirTree(s);
		//		debugDirTree(r);

		assertDirTreeNode(r, "c:/music");
		Map<String, Map> p= r.get(ensureCorrectDirSeperators("c:/music"));
		assertDirTreeNode(p, "new stuff", "old stuff/2006/burnt");
		assertNull(p.get(ensureCorrectDirSeperators("old stuff/2006/burnt")));
		p= p.get(ensureCorrectDirSeperators("new stuff"));
		assertDirTreeNode(p, "napster", "winmx");
		assertNull(p.get("napster"));
		assertNull(p.get("winmx"));
	}

	@Test
	public void testOptimiseDirTree2() {
		Map<String, OptimisibleDirTreeNode> s= new HashMap<String, OptimisibleDirTreeNode>();
		OptimisibleDirTreeNode sC= addOptimisibleDirTreeNode(s, "c:");
		OptimisibleDirTreeNode sC_music= addOptimisibleDirTreeNode(sC, "music");
		OptimisibleDirTreeNode sC_music_newStuff= addOptimisibleDirTreeNode(sC_music, "new stuff");
		OptimisibleDirTreeNode sC_music_newStuff_napster= addOptimisibleDirTreeNode(sC_music_newStuff, "napster");
		sC_music_newStuff_napster.hasFiles= true;
		OptimisibleDirTreeNode sC_music_newStuff_winmx= addOptimisibleDirTreeNode(sC_music_newStuff, "winmx");
		sC_music_newStuff_winmx.hasFiles= true;
		OptimisibleDirTreeNode sC_music_oldStuff= addOptimisibleDirTreeNode(sC_music, "old stuff");
		sC_music_oldStuff.hasFiles= true;
		OptimisibleDirTreeNode sC_music_oldStuff_2006= addOptimisibleDirTreeNode(sC_music_oldStuff, "2006");
		OptimisibleDirTreeNode sC_music_oldStuff_2006_burnt= addOptimisibleDirTreeNode(sC_music_oldStuff_2006, "burnt");
		sC_music_oldStuff_2006_burnt.hasFiles= true;
		OptimisibleDirTreeNode sC_music_oldStuff_2006_burnt_asd= addOptimisibleDirTreeNode(sC_music_oldStuff_2006_burnt, "asd");
		sC_music_oldStuff_2006_burnt_asd.hasFiles= true;
		OptimisibleDirTreeNode sC_music_blah= addOptimisibleDirTreeNode(sC_music, "blah");
		sC_music_blah.hasFiles= true;
		OptimisibleDirTreeNode sC_music_blah_blah1= addOptimisibleDirTreeNode(sC_music_blah, "blah1");
		sC_music_blah_blah1.hasFiles= true;
		OptimisibleDirTreeNode sC_music_blah_blah2= addOptimisibleDirTreeNode(sC_music_blah, "blah2");
		OptimisibleDirTreeNode sC_music_blah_zxc= addOptimisibleDirTreeNode(sC_music_blah_blah2, "zxc");
		sC_music_blah_zxc.hasFiles= true;
		OptimisibleDirTreeNode sC_music_blah_zxc2= addOptimisibleDirTreeNode(sC_music_blah_zxc, "zxc2");
		sC_music_blah_zxc2.hasFiles= true;
		OptimisibleDirTreeNode sC_music_blah_qwe= addOptimisibleDirTreeNode(sC_music_blah_blah2, "qwe");
		OptimisibleDirTreeNode sC_music_blah_qwe2= addOptimisibleDirTreeNode(sC_music_blah_qwe, "qwe2");
		sC_music_blah_qwe2.hasFiles= true;

		Map<String, Map> r= Helpers.optimiseDirTree(s);
		//		debugDirTree(r);

		assertDirTreeNode(r, "c:/music");
		Map<String, Map> p, pMusic= r.get(ensureCorrectDirSeperators("c:/music"));
		assertDirTreeNode(pMusic, "new stuff", "old stuff", "blah");

		p= pMusic.get("new stuff");
		assertDirTreeNode(p, "napster", "winmx");
		assertNull(p.get("napster"));
		assertNull(p.get("winmx"));

		p= pMusic.get("old stuff");
		assertDirTreeNode(p, "2006/burnt");
		p= p.get(ensureCorrectDirSeperators("2006/burnt"));
		assertDirTreeNode(p, "asd");
		assertNull(p.get("asd"));

		p= pMusic.get("blah");
		assertDirTreeNode(p, "blah1", "blah2");
		assertNull(p.get("blah1"));
		p= p.get("blah2");
		assertDirTreeNode(p, "zxc", ensureCorrectDirSeperators("qwe/qwe2"));
		assertNull(p.get(ensureCorrectDirSeperators("qwe/qwe2")));
		p= p.get("zxc");
		assertDirTreeNode(p, "zxc2");
		assertNull(p.get("zxc2"));
	}

	private void assertDirTreeNode(Map<String, ?> map, String... keys) {
		assertEquals(keys.length, map.size());
		for (String key : keys)
			assertTrue(map.containsKey(ensureCorrectDirSeperators(key)));
	}

	private OptimisibleDirTreeNode addOptimisibleDirTreeNode(OptimisibleDirTreeNode parent, String name) {
		return addOptimisibleDirTreeNode(parent.children, name);
	}

	private OptimisibleDirTreeNode addOptimisibleDirTreeNode(Map<String, OptimisibleDirTreeNode> parent, String name) {
		OptimisibleDirTreeNode n= new OptimisibleDirTreeNode();
		parent.put(name, n);
		return n;
	}

	public static void debugDirTree(Map<String, Map> tree) {
		System.out.println("-------------------------------------------------------------------------");
		System.out.println("{");
		for (String s : Helpers.sort(tree.keySet())) {
			System.out.println("  \"" + s + "\" => {");
			debugDirTree("  ", tree.get(s));
			System.out.println("  },");
		}
		System.out.println("}");
	}

	private static void debugDirTree(String prefix, Map<String, Map> node) {
		prefix+= "| ";
		if (node == null)
			System.out.println(prefix + "null");
		else
			for (String s : Helpers.sort(node.keySet())) {
				Map<String, Map> x= node.get(s);
				System.out.print(prefix + "\"" + s + "\" => ");
				if (x == null)
					System.out.println("null");
				else {
					System.out.println("{");
					debugDirTree(prefix, x);
					System.out.println(prefix + "},");
				}
			}
	}
}
