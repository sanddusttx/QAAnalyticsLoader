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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

  protected void setSheetDate(final HSSFWorkbook template) throws ParseException {
    final Date date = new SimpleDateFormat("MM-YYYY").parse(this.year + "-" + this.month);
    template.getSheetAt(0).getRow(0).getCell(2).setCellValue(date);
    //end setSheetDate
  }
  /*
   * (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    // Load Template
    //TODO: Load Template from Filesystem if available, otherwise use embedded default.
    try (final InputStream templateStream = 
        Thread.currentThread().getContextClassLoader().getResourceAsStream("QCTemplate.xls");){
      final HSSFWorkbook template = new HSSFWorkbook(templateStream);
      setSheetDate(template);
      // Invoke Processor with Date & Workbook.
      final Processor proccessor = new Processor(month, year, template, new File("."));
      proccessor.process();
      //Save Processed Data!
      final OutputStream out = new FileOutputStream(new File("QCAnalytic.xls"));
      proccessor.write(out);
    } catch (IOException e) {
      throw new RuntimeException("Exception accessing Template", e);
    } catch (ParseException e) {
      throw new RuntimeException("Exception setting Template date", e);
    }
  }

}
