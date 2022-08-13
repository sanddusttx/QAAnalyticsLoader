/*
 * Copyright 2019
 */
package com.j2eeguys.qaanalytics.loader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * @author Sanddust sanddust@j2eeguys.com
 * @author Gorky gorky@j2eeguys.com
 */
class ControllerTest {

  /**
   * Test method for {@link Controller#run()}.
   */
  @SuppressWarnings("static-method")
  @Test
  void testRun() {
    //BUG - This needs to be in the build/test dir
    final File testSheetFile = new File(".", "2018-12-QCAnalysis.xls");
    if (!testSheetFile.getParentFile().exists() && !testSheetFile.getParentFile().mkdirs()) {
      throw new RuntimeException(
          "Unable to create working directory: " + testSheetFile.getParentFile().getAbsolutePath());
    }
    assertTrue(!testSheetFile.exists() || testSheetFile.delete());
    final Controller controller = new Controller("12", "2018");
    controller.run();
    assertEquals(365056, testSheetFile.length());
    //end testRun
  }

}
