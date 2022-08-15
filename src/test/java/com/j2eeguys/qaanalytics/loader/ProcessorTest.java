/*
 * Copyright (c) 2020
 * 
 * This code is licensed under the GPLv2.  Please contact Sanddust at
 * sanddust@j2eeguys.com for additional licenses. 
 */
package com.j2eeguys.qaanalytics.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.Test;

/**
 * @author Sanddust sanddust@j2eeguys.com
 * @author Gorky gorky@j2eeguys.com
 *
 */
class ProcessorTest {

  /**
   * The {@link Processor} to use for testing.
   */
  protected Processor processor;

  /**
   * Setupe the processor for testing.
   * @param fileName the name of the file to load the source file from.
   */
  protected void setupProcessor(final String fileName) {
    try (final InputStream templateStream =
        Thread.currentThread().getContextClassLoader().getResourceAsStream(fileName);) {
      @SuppressWarnings("resource") //Handed off for more processing
      final HSSFWorkbook template = new HSSFWorkbook(templateStream);
      this.processor = new Processor("12", "2018", template, new File("src/test/resources"));
    } catch (IOException e) {
      throw new RuntimeException("Exception initializing workbook", e);
    }
  }
  
  /**
   * Test method for {@link com.j2eeguys.qaanalytics.loader.Processor#process()}.
   * @throws IOException thrown if any exceptions occur during testing.
   */
  @Test
  void testProcess() throws IOException {
    setupProcessor("QCTemplate.xls");
    this.processor.process();
    final File testSheetFile = new File("build/test", "December 2018 TestQC.xls");
    if (!testSheetFile.getParentFile().exists() && !testSheetFile.getParentFile().mkdirs()) {
      throw new RuntimeException(
          "Unable to create working directory: " + testSheetFile.getParentFile().getAbsolutePath());
    }
    try (final OutputStream testOut = new FileOutputStream(testSheetFile)) {
      this.processor.write(testOut);
      testOut.flush();
    }
    assertEquals(461824, testSheetFile.length());
    //end testProcess
  }

  /**
   * Test method for {@link com.j2eeguys.qaanalytics.loader.Processor#write(java.io.OutputStream)}.
   * @throws IOException thrown if any Exceptions occur during testing.
   */
  @Test
  void testWrite() throws IOException {
    setupProcessor("December 2018 QC.xls");
    final File testSheetFile = new File("build/test", "TWDecember 2018 TestQC.xls");
    if (!testSheetFile.getParentFile().exists() && !testSheetFile.getParentFile().mkdirs()) {
      throw new RuntimeException(
          "Unable to create working directory: " + testSheetFile.getParentFile().getAbsolutePath());
    }
    try (final OutputStream testOut = new FileOutputStream(testSheetFile)) {
      this.processor.write(testOut);
      testOut.flush();
    }
    assertTrue(testSheetFile.exists());
    final File demoFile = new File("src/test/resources", "December 2018 QC.xls");
    //Writing sometimes increases the size of the file.
    assertTrue(demoFile.length() <= testSheetFile.length());
    
    //end testWrite
  }

}
