/*
 * Copyright (c) 2019, 2020, 2021, 2022 This code is licensed under the GPLv2. Please contact
 * Sanddust at sanddust@j2eeguys.com for additional licenses.
 */
package com.j2eeguys.qaanalytics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.swing.JOptionPane;

import org.apache.commons.io.IOUtils;

import com.j2eeguys.qaanalytics.loader.Controller;

/**
 * Main class for running the QA Analytics Loader. Handles any required User
 * Interaction.
 * 
 * @author Sanddust sanddust@j2eeguys.com
 */
public class Loader {

  /**
   * The Current Year for the System.
   */
  protected static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);

  /**
   * The Month to process.
   */
  protected String month;
  
  /**
   * The year to process.
   */
  protected String year;

  /**
   * Constructor.
   */
  public Loader() {
    // init
  }
  
  /**
   * @return List of years from 2018 to the current year.
   */
  protected static String[] getYears() {
    final int size = CURRENT_YEAR - 2018 + 1; //Include current year.
    final String[] yearList = new String[size];
    for(int i = 0, y = 2018; i < size; i++, y++) {
      yearList[i] = String.valueOf(y);
    }
    return yearList;
    //end getYears
  }

  /**
   * Query the user for month/year to process.
   */
  public void queryDate() {
    // Year
    final String[] years = getYears();
    this.year = (String) JOptionPane.showInputDialog(null, "Please select year", "Select",
        JOptionPane.QUESTION_MESSAGE, null, years, String.valueOf(CURRENT_YEAR));
    if (this.year == null) {
      //User Cancelled.
      return;
    }
    // Month
    String[] months = new String[12];
    for (int i = 0; i < 12; i++) {
      if (i < 9) {
        months[i] = "0" + (i + 1);
      } else {
        months[i] = Integer.toString(i + 1);
      }
    }
    //Get Month is 0-Offset, but user and files are 1-Offset
    final int currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
    final String defaultMonth = currentMonth < 10 ? "0" + currentMonth : String.valueOf(currentMonth);
    this.month = (String) JOptionPane.showInputDialog(null, "Please select month", "Select",
        JOptionPane.QUESTION_MESSAGE, null, months, defaultMonth);
    if (this.month == null) {
      //User Cancelled.
      return;
    }
    //end queryDate
  }

  /**
   * @return The CLI Help for the QAAnalyticsLoader.
   */
  protected static String getUsage() {
    return "Usage: bin/QAAnalyticsLoader <options>\n"
        + "-h this help message\n"
        + "-t create template Spreadsheet and config files\n"
        + "\n"
        + "If a Template and Config file are not available, defaults will be used."
        ;
    //end getUsage
  }

  /**
   * Writes the Template files into the local directory so that they can be modified for the data to be run.
   * @throws IOException if the Template files can not be accessed or written.
   */
  protected static void writeTemplates() throws IOException {
    final String[] files = { "QCTemplate.xls", "loader.config" };
    for (final String currentFile : files) {
      try (final InputStream inStream = 
        Thread.currentThread().getContextClassLoader().getResourceAsStream(currentFile);
          final FileOutputStream outStream = new FileOutputStream(new File(currentFile))){
        IOUtils.copy(inStream, outStream);
        outStream.flush();
      }
    }
    final File reportDir = new File("reports");
    if (!(reportDir.exists() || reportDir.mkdir())) {
      throw new IOException("Unable to create REP Directory: " + reportDir.getAbsolutePath());
    }
    //end writeTemplates
  }
  
  /**
   * Main method for Command Line/Application Execution.
   * @param args Command line arguments.
   * @throws Throwable thrown if any issues occur during processing.
   */
  public static void main(String[] args) throws Throwable{
    if (args != null && args.length > 0) {
      for (final String arg : args) {
        if ("-h".equals(arg)) {
          System.out.println(getUsage()); //NOSONAR -- Main Method
        } else if ("-t".equals(arg)) {
          writeTemplates();
        }
      }//end for
      return; //Done
    }//else
    final Loader loader = new Loader();
    //Query user for month(s) and year to process.
    loader.queryDate();
    if (loader.year == null || loader.month == null) {
      System.out.println("User Cancelled.");
      //User Cancelled.
      return;
    }
    final Controller controller = new Controller(loader.month, loader.year);
    System.out.println("Running " + loader.month + '-' +  loader.year);
    controller.run();
    System.out.println("Done.");
    //end main
  }

}
