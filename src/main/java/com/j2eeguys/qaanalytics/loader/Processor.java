/*
 * Copyright (c) 2019
 * This code is licensed under the GPLv2.  Please contact Sanddust at
 * sanddust@j2eeguys.com for additional licenses. 
 */
package com.j2eeguys.qaanalytics.loader;

import java.io.File;

/**
 * Processes the selected Month. Reads the .rep files from the input (or current
 * working) directory, and writes to the output spreadsheet.  The output
 * spreadsheet can be either the default template or a user supplied template.
 * Note that in the case of a user supplied template, a conforming layout is
 * required.
 * 
 * @author Sanddust sanddust@j2eeguys.com
 */
public class Processor {
	protected String month;
	protected String year;
	protected File outputFile;

	/**
	 * @param month
	 * @param year
	 * @param outputFile
	 */
	public Processor(String month, String year, File outputFile) {
		super();
		this.month = month;
		this.year = year;
		this.outputFile = outputFile;
	}
	
	public void process() {
		//Open Outputfile for writing (wrap with Poi!)
		//Calculate number of days in the month!
		final int days = 0;
		//Loop through the rep files
		for (int i = 0; i < days; i++) {
			
		}
	}

}
