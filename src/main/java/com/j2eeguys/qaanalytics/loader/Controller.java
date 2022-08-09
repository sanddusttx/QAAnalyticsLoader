/*
 * Copyright (c) 2019 This code is licensed under the GPLv2. Please contact
 * Sanddust at sanddust@j2eeguys.com for additional licenses.
 */
package com.j2eeguys.qaanalytics.loader;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;

/**
 * Sets up the files, streams, templates, etc. for processing.
 * 
 * @author Sanddust sanddust@j2eeguys.com
 */
public class Controller implements Runnable {

  protected String month;
  protected String year;

  /**
   * Constructor.
   */
  public Controller() {
    // end <init>
  }

  /**
   * Constructor for Controller.
   * 
   * @param month
   * @param year
   */
  public Controller(String month, String year) {
    super();
    this.month = month;
    this.year = year;
    // end <init>
  }

  /*
   * (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    // Load Template
    try (final InputStream templateStream = 
        Thread.currentThread().getContextClassLoader().getResourceAsStream("QCTemplate.xls");){
      final HSSFWorkbook template = new HSSFWorkbook(templateStream);
      // Invoke Processor with Date & Workbook.
      final Processor proccessor = new Processor(month, year, template, new File("."));
      proccessor.process();
      //Save Processed Data!
      final OutputStream out = new FileOutputStream(new File("QCAnalytic.xls"));
      proccessor.write(out);
    } catch (IOException e) {
      throw new RuntimeException("Exception accessing Template", e);
    }
    // end run
  }

}
