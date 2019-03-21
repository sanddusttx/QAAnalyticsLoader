/*
 * Copyright 2019
 */
package com.j2eeguys.qaanalytics.loader;

import java.io.File;

/**
 * @author honor
 *
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
