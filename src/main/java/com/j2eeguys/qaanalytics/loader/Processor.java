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
import java.math.BigDecimal;
import java.math.RoundingMode;
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
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
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
  /**
   * The directory with the reports and configuration files.
   */
  protected File repDir;
  /**
   * The month being processed
   */
  protected String month;
  
  /**
   * The year being processed
   */
  protected String year;
  
  /**
   * The workbook to write the data to.
   */
  protected Workbook workbook;

  protected Map<String, String> mappings;
  protected Map<String, Integer> rangeStarts;
  
  /**
   * The column with the ranges.
   */
  protected int rangeColumn;

  /**
   * @param month the month being processed
   * @param year the year being processed
   * @param workbook the workbook to write the data to.
   * @param repDir The directory with the reports and configuration files.
   */
  public Processor(final String month, final String year, final Workbook workbook, final File repDir) {
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
      this.rangeColumn = getCharValue(config.get("Template"), "column.ranges") - 'A';
      return config;
    }
    //end loadConfig
  }
  
  /**
   * Get the char value of a configuration item.
   * @param section The section with the configuration settings containing the key.
   * @param configKey The lookup key for the configuration item
   * @return the character value of the configuration item.
   * @throws NullPointerException thrown if the configKey is not defined in the configuration file.
   */
  protected static char getCharValue(final Section section, final String configKey) {
    return section.get(configKey).trim().charAt(0);
    //end getCharValue
  }
  
  /**
   * Get the char value of a configuration item.
   * @param section The section with the configuration settings containing the key.
   * @param configKey The lookup key for the configuration item
   * @return the character value of the configuration item.
   * @throws NullPointerException thrown if the configKey is not defined in the configuration file.
   */
  protected static int getIntValue(final Section section, final String configKey) {
    return Integer.parseInt(section.get(configKey).trim());
    //end getIntValue
  }
  
  /**
   * Load the starting row for each of the ranges in to the map.
   * @param config the config file wrapper.
   */
  protected void loadRangeStarts(final Profile config) {
    final Set<String> rangeNames = new HashSet<>(this.mappings.values());
    this.rangeStarts = new HashMap<>(rangeNames.size());
    final Section templateSection = config.get("Template");
    //Row Numbers are 1-Offset, but we need to use 0-Offset for the worksheet.
    final int rowFirstName = getIntValue(templateSection, "section.firstRow") - 1
        + getIntValue(templateSection, "section.nameRow") - 1;
    final int startDiff = getIntValue(templateSection, "section.nameRow")
        - getIntValue(templateSection, "section.firstRow")
        + 1; //Skip the "Flag" row and start with the actual range row.
    final int rangeSize = getIntValue(templateSection, "section.numRows");
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
  
  /**
   * Get the first row for the range.
   * @param name The name of the range to get the first row for.
   * @return The first row of the range.
   */
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
   * Format the column with the same format as and precision as the range.  Also formats the Date Cell
   * to match the Range Background.
   * @param colRanges The column with the Ranges.
   * @param firstRow The first row of data in the sheet.
   * @param dateCol The column with the current day being processed.
   */
  protected void formatColumn(final int colRanges, final int firstRow, final int dateCol) {
    for(final Iterator<Sheet> it = this.workbook.sheetIterator();it.hasNext();) {
      final Sheet currentSheet = it.next();
      //Do Date Row
      final Row dateRow = currentSheet.getRow(firstRow - 1);
      if (dateRow == null) {
        //Not a Data Sheet
        continue;
      }
      final Cell dateCell = dateRow.getCell(dateCol);
      final CellStyle dateStyle = this.workbook.createCellStyle();
      dateStyle.cloneStyleFrom(dateCell.getCellStyle());
      final CellStyle rangeStyle = dateRow.getCell(colRanges).getCellStyle();
      dateStyle.setFillPattern(rangeStyle.getFillPattern());
      dateStyle.setFillBackgroundColor(rangeStyle.getFillBackgroundColor());
      dateStyle.setFillForegroundColor(rangeStyle.getFillForegroundColor());
      dateCell.setCellStyle(dateStyle);
      
      //Do Data Rows
      for(int i = firstRow,rowCount = currentSheet.getLastRowNum(); i < rowCount;i++) {
        final Row currentRow = currentSheet.getRow(i);
        if (currentRow != null) {
          final Cell rangeCell = currentRow.getCell(colRanges);
          if (rangeCell != null) {
            final CellStyle style = rangeCell.getCellStyle();
            final Cell currentCell = currentRow.getCell(dateCol, MissingCellPolicy.CREATE_NULL_AS_BLANK);
            currentCell.setCellStyle(style);
          }
        }
      }
    }
    //end formatColumn
  }
  
  /**
   * Compare two double values within a tolerance range.
   * @param style the CellStyle with the precision.
   * @param repValue the Value from the Report File.
   * @param rangeValue the range limit value from the QC Spreadsheet.
   * @return <ul>
   * <li>+1 if the repValue > rangeValue + tolerance</li>
   * <li>0 if the values are equal within range</li>
   * <li>-1 otherwise</li>
   * 
   */
  protected static int valueCompare(final CellStyle style, final double repValue,
      final double rangeValue) {
    final String formatString = style.getDataFormatString();
    final double threshold = formatString.length() <= 2 ? 0 : //HACK for Template format not set.
        Double.parseDouble(formatString + "1");
    return Math.abs(repValue - rangeValue) < threshold ? 0 :
      repValue > rangeValue + threshold ? 1 : -1
      ;
    //end valueCompare
  }
  
  /**
   * Round the report value to the precision specified in the range cell style.
   * @param style The CellStyle with the precision.
   * @param repValue The Report Value.
   * @return the report value rounded to the style's precision.
   */
  protected static double roundRepValue(final CellStyle style, final String repValue) {
    final String format = style.getDataFormatString().trim();
    final int precision = format.length() - format.indexOf('.') - 1;
    final BigDecimal bd = new BigDecimal(repValue).setScale(precision, RoundingMode.HALF_UP);
    return bd.doubleValue();
    //end roundRepValue
  }
  
  /**
   * Process the Rep files
   * @throws IOException thrown if the configuration file can not be loaded.
   */
  public void process() throws IOException {
    // Load Maps and Configs
    final Profile config = loadConfig();
    final int colDayStart = getCharValue(config.get("Template"), "column.day1") - 'A';
    final int colRanges = getCharValue(config.get("Template"), "column.ranges") - 'A';
    // TODO: Calculate number of days in the month! - SPD
 // Get the number of days in that month
    YearMonth yearMonthObject = YearMonth.of(Integer.valueOf(this.year).intValue(), Integer.valueOf(this.month));
    int days = yearMonthObject.lengthOfMonth();  
    // Loop through the rep files
    for (int day = 1; day <= days; day++) {
      final String fileName =
          this.month + (day < 10 ? "0" + day : Integer.toString(day)) + this.year.substring(2) + ".rep";
      final File repFile = new File(this.repDir, fileName);
      if (!repFile.exists()) {
        continue;
      }
      //Have a file, format the columns
      formatColumn(colRanges, 2, colDayStart + day - 1); //Day is 1-Offset, Columns are 0-Offset.
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
              // number of days added to the column subtracting 1 for 0-Offset of POI
              final Cell medianCell = qcSheet.getRow(rangeTopRow + 2).getCell(colRanges);
              final double median = 
                  //HACK - If median not a numeric range Type, use 0 until Template is fixed.
                  medianCell.getCellType() ==  CellType.NUMERIC ? 
                      medianCell.getNumericCellValue() : 0;
              int trackNumber = 0;
              
              final CellStyle medianStyle = medianCell.getCellStyle();
              final double repValue = roundRepValue(medianStyle, rawRepValue);
              switch(valueCompare(medianStyle, repValue, median)) {
                case 1: {
                  final Cell rangeCell = qcSheet.getRow(rangeTopRow + 1).getCell(colRanges);
                  final double rangeValue = 
                    //HACK - If not a numeric range Type, use 0 until Template is fixed.
                      rangeCell.getCellType() ==  CellType.NUMERIC ? 
                      rangeCell.getNumericCellValue() : 0;
                  trackNumber = valueCompare(rangeCell.getCellStyle(), repValue, rangeValue) > 0 ? 0 : 1;
                  break;
                }
                case 0:
                  trackNumber = 2;
                  break;
                default: {
                  final Cell rangeCell = qcSheet.getRow(rangeTopRow + 3).getCell(colRanges);
                  final double rangeValue = 
                    //HACK - If not a numeric range Type, use 0 until Template is fixed.
                      rangeCell.getCellType() ==  CellType.NUMERIC ? 
                      rangeCell.getNumericCellValue() : 0;
                  trackNumber = valueCompare(rangeCell.getCellStyle(), repValue, rangeValue) < 0 ? 4 : 3;
                  break;
                  
                }
              }
              final Cell currentCell =
                  qcSheet.getRow(rangeTopRow + trackNumber).getCell(day + colDayStart - 1);
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
   * @param out The OutputStream to write the Workbook to.
   * 
   * @throws IOException thrown if an exception occurs writing to the output file.
   */
  protected void write(final OutputStream out) throws IOException {
    this.workbook.write(out);
  }

}
