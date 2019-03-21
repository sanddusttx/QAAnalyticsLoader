/*
 * Copyright 2019
 */
package com.j2eeguys.qaanalytics.loader;

import javax.swing.JOptionPane;

/**
 * @author Sanddust
 *
 */
public class Controller implements Runnable{

	protected String month;
	protected String year;
	
	/**
	 * 
	 */
	public Controller() {
		// TODO Auto-generated constructor stub
	}

	public void queryDate() {
		//Year
		String[] years = {"2018", "2019"};
		this.year = (String)JOptionPane.showInputDialog(null, 
				"Please select year", "Select", 
				JOptionPane.QUESTION_MESSAGE, 
				null, years, "2019");
		//Month
		String[] months = new String[12];
		for(int i=0; i<12;i++) {
			if ( i < 9 ) {
				months[i] = "0" + (i +1);
			} else {
				months[i] = Integer.toString(i+ 1);
			}
		}
		this.month = (String)JOptionPane.showInputDialog(null, 
				"Please select month", "Select", 
				JOptionPane.QUESTION_MESSAGE, 
				null, months
				, "01");
	}
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		//Query user for month(s) and year to process.
		queryDate();
		//Copy Template file to Output file
		//Invoke Processor with Date & Output file handle.
	}

}
