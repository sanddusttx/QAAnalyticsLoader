/*
 * Copyright (c) 2019
 * This code is licensed under the GPLv2.  Please contact Sanddust at
 * sanddust@j2eeguys.com for additional licenses. 
 */
package com.j2eeguys.qaanalytics.loader;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Iterator;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

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
    protected File repDir;
	protected String month;
	protected String year;
	protected Workbook workbook;
	
	/**
	 * @param month
	 * @param year
	 * @param workbook
	 */
	public Processor(String month, String year, Workbook workbook, File repDir) {
		super();
		this.month = month;
		this.year = year;
		this.workbook = workbook;
		this.repDir = repDir;
		//end <init>
	}
	
	/**
	 * Process the Rep files
	 */
	public void process() {
	    //SPDTODO
		//Load Maps
	    //SPDTODO
		//Calculate number of days in the month!
		final int days = 31;
		//Loop through the rep files
		for (int i = 1; i <= days; i++) {
			final String fileName = month + (i < 10 ? "0" + i : i) + year.substring(2) + ".rep";
            //TODO: Check that the REP file is present!
			final File repFile = new File(repDir, fileName);
			if (!repFile.exists()) {
			  continue;
			}
			//Open CSV File			
            try (final CSVParser parser =
                CSVParser.parse(repFile, Charset.forName("UTF-8"), CSVFormat.RFC4180)) {
              final Iterator<CSVRecord> iter = parser.iterator();
              for(CSVRecord rec = iter.next();iter.hasNext();) {
	              //SPDTODO: Check if a desired value
                  if (rec.size()==0) {
                    //Empty record;
                    rec = iter.next();
                    continue;
                  }
	              String name = rec.get(0);
	              iter.next();
	              String date = rec.get(0);
	              for(; iter.hasNext() && (rec = iter.next()).size()>0 && rec.get(0).startsWith("|");) {
					//Will generally be LI on first cycle.  Data is in Column 4;
	                final String element = rec.get(1);
	                final Sheet qcSheet = this.workbook.getSheet(element);
                    //TODO: Check that there was a tab for the element.
	                //If no tab, qcSheet == null.
                    final String rawRepValue = rec.get(4);
                    if (rawRepValue == null || rawRepValue.isEmpty()) {
                      //Blank Value, skip!
                      continue;
                    }//else
                    final double repValue = Double.parseDouble(rawRepValue);
                    //SPDTODO: Get Row number based on Data Type (ex. China Hair) & Deviation Range.
                    //SPDTODO: Get Column number based on Rep File/Sample Date
                    final Cell currentCell = qcSheet.getRow(6).getCell(6);
                    currentCell.setCellValue(repValue);
	              }
	            }
			} catch (IOException e) {
			  throw new RuntimeException("Error reading file: " + fileName, e);
			}
			
		}
		//end process
	}
	
	/**
	 * Save the Workbook
	 * @throws IOException thrown if an exception occurs writing to the output file.
	 */
	public void write(final OutputStream out) throws IOException {
	  this.workbook.write(out);
	}

}
