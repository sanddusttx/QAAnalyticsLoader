/*
 * Copyright (c) 2019 This code is licensed under the GPLv2. Please contact
 * Sanddust at sanddust@j2eeguys.com for additional licenses.
 */
package com.j2eeguys.qaanalytics.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.ini4j.Profile;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;

/**
 * Processes the selected Month. Reads the .rep files from the input (or current
 * working) directory, and writes to the output spreadsheet. The output
 * spreadsheet can be either the default template or a user supplied template.
 * Note that in the case of a user supplied template, a conforming layout is
 * required.
 * 
 * @author Sanddust sanddust@j2eeguys.com
 * @author Gorky gorky@j2eeguys.com
 */
//TODO: Support multiple machines
public class Processor {
  protected File repDir;
  protected String month;
  protected String year;
  protected Workbook workbook;

  protected Map<String, String> mappings;
  protected Map<String, Integer> rangeStarts;
  
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
    // end <init>
  }

  /**
   * Load the ini style configuration file loader.config.  If not found in the local directory,
   * the default configuration will be loaded.
   * @return The Profile wrapping the configuration file.
   * @throws IOException if the configuration file can not be loaded.
   */
  protected Profile loadConfig() throws IOException {
    final File loaderConfig = new File(".", "loader.config");
    try (final InputStream configStream = loaderConfig.exists() ? new FileInputStream(loaderConfig):
      Thread.currentThread().getContextClassLoader().getResourceAsStream("loader.config");
      ){
      final Wini config = new Wini(configStream);
      this.mappings = config.get("Mappings");
      loadRangeStarts(config);
      return config;
    }
    //end loadConfig
  }
  
  protected void loadRangeStarts(final Profile config) {
    final Set<String> rangeNames = new HashSet<>(this.mappings.values());
    this.rangeStarts = new HashMap<>(rangeNames.size());
    final Section templateSection = config.get("Template");
    //Row Numbers are 1-Offset, but we need to use 0-Offset.
    final int rowFirstName = Integer.valueOf(templateSection.get("section.firstRow")) - 1
        + Integer.valueOf(templateSection.get("section.nameRow")) - 1;
    final int startDiff = Integer.valueOf(templateSection.get("section.nameRow"))
        - Integer.valueOf(templateSection.get("section.firstRow")) 
        + 1; //Skip the "Flag" row and start with the actual range row.
    final int rangeSize = Integer.valueOf(templateSection.get("section.numRows"));
    final Sheet firstSheet = this.workbook.getSheetAt(0);
    for(int row = rowFirstName; this.rangeStarts.size() < rangeNames.size() &&
        row < firstSheet.getLastRowNum();row += rangeSize) {
      final String rangeName = firstSheet.getRow(row).getCell(0).getStringCellValue();
      if (rangeNames.contains(rangeName)) {
        this.rangeStarts.put(rangeName, Integer.valueOf(row - startDiff));
      }
    }
    //loadRangeStarts
  }
  
  protected int getRangeTopRow(final String name) {
    final String rangeName = this.mappings.get(name);
    if (rangeName == null) {
      //Not a valid range.  Might be a sample.
      return -1;
    }//else
    final Integer rangeStart = this.rangeStarts.get(rangeName);
    if (rangeStart == null) {
      throw new NullPointerException("Mapped Range not defined in Template Sheet: " + rangeName);
    }
    return rangeStart.intValue();
    //end getRangeTopRow
  }
  
  /**
   * Process the Rep files
   * @throws IOException thrown if the configuration file can not be loaded.
   */
  public void process() throws IOException {
    // Load Maps and Configs
    final Profile config = loadConfig();
    final int colDayStart = 
        (int)config.get("Template", "column.day1").trim().charAt(0) - 'A';
    // TODO: Calculate number of days in the month! - SPD
 // Get the number of days in that month
    YearMonth yearMonthObject = YearMonth.of(Integer.valueOf(this.year).intValue(), Integer.valueOf(this.month));
    int days = yearMonthObject.lengthOfMonth();  
    // Loop through the rep files
    for (int day = 1; day <= days; day++) {
      final String fileName = month + (day < 10 ? "0" + day : day) + year.substring(2) + ".rep";
      final File repFile = new File(repDir, fileName);
      if (!repFile.exists()) {
        continue;
      }
      // Open CSV File
      try (final CSVParser parser = CSVParser.parse(repFile, Charset.forName("UTF-8"), CSVFormat.RFC4180)) {
        final Iterator<CSVRecord> iter = parser.iterator();
        for (CSVRecord rec = iter.next(); iter.hasNext();) {
          // SPDTODO: Check if a desired value
          if (rec.size() == 0) {
            // Empty record;
            rec = iter.next();
            continue;
          }
          String name = rec.get(0);
          final int rangeTopRow = getRangeTopRow(name);
          //Skip Date Row
          iter.next();
          for (; iter.hasNext() && (rec = iter.next()).size() > 0 && rec.get(0).startsWith("|");) {
            if (rangeTopRow > 0) {
              // Will generally be LI on first cycle. Data is in Column 4;
              final String element = rec.get(1);
              final Sheet qcSheet = this.workbook.getSheet(element);
              // If no tab, qcSheet == null.
              if (qcSheet == null) {
                // No Tab, skip!
                continue;
              }
              final String rawRepValue = rec.get(4);
              if (rawRepValue == null || rawRepValue.isEmpty()) {
                // Blank Value, skip!
                continue;
              } // else
              final double repValue = Double.parseDouble(rawRepValue);
              // TODO: Get Row number based on Deviation Range. - SPD
              // number of days added to the column subtracting 1 for 0-Offset of POI
              final Cell medianCell = qcSheet.getRow(rangeTopRow + 2).getCell(5);
              final double median = medianCell.getNumericCellValue();
              int trackNumber = 0;
              if (repValue == median) {
                trackNumber = rangeTopRow + 2;
              } else if (repValue > median) {
                final Cell rangeCell = qcSheet.getRow(rangeTopRow + 1).getCell(5);
                final double rangeValue = rangeCell.getNumericCellValue();
                if (repValue <= rangeValue) {
                  trackNumber = 1; 
                }
                  else {
                    trackNumber =0;
                  }
              } else {
                final Cell rangeCell = qcSheet.getRow(rangeTopRow + 3).getCell(5);
                final double rangeValue = rangeCell.getNumericCellValue();
                if (repValue >= rangeValue) {
                  trackNumber=3; 
                } else {
                    trackNumber=4;
                }
              }
              Cell currentCell = qcSheet.getRow(rangeTopRow + trackNumber).getCell(day + colDayStart - 1);
              if (currentCell == null) {
                currentCell = qcSheet.getRow(rangeTopRow + trackNumber).createCell(day + colDayStart - 1);
              }
              currentCell.setCellValue(repValue);
            }//else, not a mapped QC Row.
          }
          //TODO: Don't process the entire REP File!  Once all QC's are loaded, skip to next file! - SPD.
        }
      } catch (IOException e) {
        throw new RuntimeException("Error reading file: " + fileName, e);
      }

    }
    // end process
  }

  /**
   * Save the Workbook.
   * 
   * @throws IOException thrown if an exception occurs writing to the output file.
   */
  protected void write(final OutputStream out) throws IOException {
    this.workbook.write(out);
  }

}
