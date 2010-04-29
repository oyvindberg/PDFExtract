package org.spaceroots.jarmor;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {
  public static Test suite() { 
    TestSuite suite = new TestSuite("org.spaceroots.jarmor"); 
    suite.addTest(Base64EncoderTest.suite()); 
    suite.addTest(Base64DecoderTest.suite()); 
    suite.addTest(Base32EncoderTest.suite()); 
    suite.addTest(Base32DecoderTest.suite()); 
    suite.addTest(Base16EncoderTest.suite()); 
    suite.addTest(Base16DecoderTest.suite()); 
    suite.addTest(UUEncoderTest.suite()); 
    suite.addTest(UUDecoderTest.suite()); 
    suite.addTest(ASCII85EncoderTest.suite()); 
    suite.addTest(ASCII85DecoderTest.suite()); 
    return suite; 
  }
}
