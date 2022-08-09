/*
 * Copyright (c) 2019 This code is licensed under the GPLv2. Please contact
 * Sanddust at sanddust@j2eeguys.com for additional licenses.
 */
package com.j2eeguys.qaanalytics;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.swing.JOptionPane;

import org.apache.poi.util.IOUtils;

import com.j2eeguys.qaanalytics.loader.Controller;

/**
 * Main class for running the QA Analytics Loader. Handles any required User
 * Interaction.
 * 
 * @author Sanddust sanddust@j2eeguys.com
 */
public class Loader {

  protected static final int CURRENT_YEAR = Calendar.getInstance().get(Calendar.YEAR);
  
  protected String month;
  protected String year;

  /**
   * Constructor.
   */
  public Loader() {
    // init
  }
  
  protected String[] getYears() {
    final int size = CURRENT_YEAR - 2018;
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
    //end queryDate
  }

  protected static String getUsage() {
    return "Usage: java -jar QAAnalysticsLoader.jar <options>\n"
        + "-h this help message\n"
        + "-t create template Spreadsheet and config files\n";
    //end getUsage
  }
  
  protected static void writeTemplates() throws IOException {
    final String[] files = { "QCTemplate.xls", "loader.config" };
    for (final String currentFile : files) {
      try (final InputStream inStream = 
        Thread.currentThread().getContextClassLoader().getResourceAsStream(currentFile);){
        IOUtils.copy(inStream, new File(currentFile));
        
      }
    }
    //end writeTemplates
  }
  
  /**
   * Main method for Command Line/Application Execution.
   * @param args Command line arguments.
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
    final Controller controller = new Controller(loader.month, loader.year);
    controller.run();
    //end main
  }

}
