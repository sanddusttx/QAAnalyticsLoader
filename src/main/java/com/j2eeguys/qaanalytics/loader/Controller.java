/*
 * Copyright (c) 2019 This code is licensed under the GPLv2. Please contact
 * Sanddust at sanddust@j2eeguys.com for additional licenses.
 */
package com.j2eeguys.qaanalytics.loader;

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
    // Copy Template file to Output file
    // Invoke Processor with Date & Output file handle.
    // end run
  }

}
