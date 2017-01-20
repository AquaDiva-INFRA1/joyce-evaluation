import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.aquadiva.joyce.evaluation.services.BiOSSComparator;


public class BiOSSComparatorTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testGenerateTestSettings() {
		
		BiOSSComparator.generateTestSettings(5, new int[] {30,50,100}, new int[] {1,2,3,4,5}, "biosevalsettings.txt");
	}

}
