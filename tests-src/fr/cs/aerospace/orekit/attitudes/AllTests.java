package fr.cs.aerospace.orekit.attitudes;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class AllTests extends TestCase {
  public static Test suite() { 

    TestSuite suite = new TestSuite("fr.cs.aerospace.orekit.attitudes"); 

    suite.addTest(SunPointingAttitudeTest.suite());

    return suite; 

  }
}
