/*
 * Copyright (c) 2019 This code is licensed under the GPLv2. Please contact
 * Sanddust at sanddust@j2eeguys.com for additional licenses.
 */
package com.j2eeguys.qaanalytics;

import javax.swing.JOptionPane;

import com.j2eeguys.qaanalytics.loader.Controller;

/**
 * Main class for running the QA Analytics Loader. Handles any required User
 * Interaction.
 * 
 * @author Sanddust sanddust@j2eeguys.com
 */
public class Loader {

  protected String month;
  protected String year;

  /**
   * Constructor.
   */
  public Loader() {
    // init
  }

  /**
   * Query the user for month/year to process.
   */
  public void queryDate() {
    // Year
    String[] years = { "2018", "2019" };
    this.year = (String) JOptionPane.showInputDialog(null, "Please select year", "Select",
        JOptionPane.QUESTION_MESSAGE, null, years, "2019");
    // Month
    String[] months = new String[12];
    for (int i = 0; i < 12; i++) {
      if (i < 9) {
        months[i] = "0" + (i + 1);
      } else {
        months[i] = Integer.toString(i + 1);
      }
    }
    this.month = (String) JOptionPane.showInputDialog(null, "Please select month", "Select",
        JOptionPane.QUESTION_MESSAGE, null, months, "01");
  }

  /**
   * Main method for Command Line/Application Execution.
   * @param args Command line arguments.
   */
  public static void main(String[] args) {
    final Loader loader = new Loader();
    //Query user for month(s) and year to process.
    loader.queryDate();
    final Controller controller = new Controller(loader.month, loader.year);
    controller.run();
    //end main
  }

}
