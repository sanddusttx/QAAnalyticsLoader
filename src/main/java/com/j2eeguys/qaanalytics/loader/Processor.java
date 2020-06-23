/*
 * Copyright (c) 2019
 * This code is licensed under the GPLv2.  Please contact Sanddust at
 * sanddust@j2eeguys.com for additional licenses. 
 */
package com.j2eeguys.qaanalytics.loader;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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
	protected String month;
	protected String year;
	protected Workbook workbook;

	/**
	 * @param month
	 * @param year
	 * @param workbook
	 */
	public Processor(String month, String year, Workbook workbook) {
		super();
		this.month = month;
		this.year = year;
		this.workbook = workbook;
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
			//Open CSV File			
			try (final InputStream repFile = new FileInputStream(fileName);
			    final HSSFWorkbook repBook = new HSSFWorkbook(repFile)){
	            
	            final Sheet repSheet = repBook.getSheetAt(0);
	            final int rowCount = repSheet.getLastRowNum();
	            for(int j = 0; j < rowCount; j++) {
	              //SPDTODO: Check if a desired value
	              String name = repSheet.getRow(j++).getCell(0).getStringCellValue();
	              String date = repSheet.getRow(j++).getCell(0).getStringCellValue();
	              while(repSheet.getRow(j).getCell(0).getStringCellValue().startsWith("|")) {
					//Will generally be LI on first cycle.  Data is in Column 4;
	                final String element = repSheet.getRow(j).getCell(1).getStringCellValue();
	                final Sheet qcSheet = this.workbook.getSheet(element);
                    //TODO: Check that there was a tab for the element.
	                //If no tab, qcSheet == null.
					//SPDTODO: Get Row number based on Data Type (ex. China Hair) & Deviation Range.
					//SPDTODO: Get Column number based on Rep File/Sample Date
	                Cell currentCell = qcSheet.getRow(6).getCell(6);
					currentCell.setCellValue(repSheet.getRow(j).getCell(4).getNumericCellValue());
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
