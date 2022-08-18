/*
 * Copyright (c) 2019, 2020, 2021, 2022 This code is licensed under the GPLv2.
 * Please contact Sanddust at sanddust@j2eeguys.com for additional licenses.
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
import java.util.Collection;
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
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.ini4j.Profile;
import org.ini4j.Profile.Section;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class Processor {

  private static final Logger LOGGER = LoggerFactory.getLogger(Processor.class);

  /**
   * Holds the status of a QC Check being processed.
   * 
   * @author Gorky gorky@j2eeguys.com
   */
  protected class QCStatus {
    
    /**
     * Holds the details of the Out-of-Range value for the element in the Range.
     * @author Gorky gorky@j2eeguys.com
     */
    private class Diff {
      protected final double value;
      protected final double diff;
      protected final boolean above;

      public Diff(final double value, final double diff, final boolean above) {
        this.diff = diff;
        this.value = value;
        this.above = above;
        // end <init>
      }
    }

    /**
     * Number of attempts to process QC Check.
     */
    private int count = 0;
    /**
     * If the QC Check processing has completed.
     */
    private boolean done = false;

    /**
     * Map of elements to be processed and the value closest to the range limits
     * found.
     */
    private Map<String, Diff> elements = new HashMap<>();

    /**
     * Increment the number of attempts to process the current QC Check.
     * 
     * @return the number of attempts made to process the current QC Check.
     */
    public int increment() {
      this.count++;
      return this.count;
      // end increment
    }

    /**
     * @return the number of attempts made to process the current QC Check.
     */
    public int getCount() {
      return this.count;
      // end getCount
    }

    /**
     * @param element the Element that is Out Of Range
     * @return true if the Element is Out of Range High.
     */
    public boolean isHigh(final String element) {
      final Diff diff = this.elements.get(element);
      if (diff == null) {
        throw new IllegalArgumentException("Element not found: " + element);
      } // else
      return diff.above;
      // end isHigh
    }

    /**
     * @return if this QC Check is done processing.
     */
    public boolean isDone() {
      return this.done;
      // end isDone
    }

    /**
     * Set the Status to "Done".
     */
    public void setDone() {
      this.done = true;
      // end setDone
    }

    /**
     * @return if this status has any Out of Range values.
     */
    public boolean hasOutOfRange() {
      return !this.elements.isEmpty();
      // end hasOutOfRange
    }

    /**
     * Clear an element from the out of range list.
     * 
     * @param elementName the name of the element that was out of range.
     * @return true if there are no additional elements that are out of range, false
     *         if there are still out of range elements.
     */
    public boolean clearOutOfRange(final String elementName) {
      this.elements.remove(elementName);
      return this.elements.isEmpty();
      // end clearOutOfRange
    }

    /**
     * @param elementName the name of the element to check if out of range.
     * @return if the current element is out of range.
     */
    public boolean isOutOfRange(final String elementName) {
      return this.elements.containsKey(elementName);
      // end isOutOfRange
    }

    /**
     * Add or update an out of range value. Accepts the value that is closest to the
     * ranges. If a stored value is closer to an acceptable range value, the new
     * value is rejected.
     * 
     * @param elementName the name of the element out of range.
     * @param maxCell     the Cell with the max value for the range.
     * @param lowCell     the Cell with the lowest value for the range.
     * @param value       the out of range value.
     * @return
     *         <ul>
     *         <li>+1 - if the value was accepted and higher than the max value</li>
     *         <li>0 - if the value was not accepted</li>
     *         <li>-1 - if the value was accepted and lower than the min value</li>
     */
    public int outOfRange(final String elementName, final Cell maxCell, final Cell lowCell,
        final double value) {
      final double maxValue = getCellValue(maxCell);
      final boolean above = value > maxValue;
      final Diff oldDiff = this.elements.get(elementName);
      if (oldDiff == null) {
        // Value not previously present
        this.elements.put(elementName,
            new Diff(value, above ? value - maxValue : getCellValue(lowCell) - value, above));
        return above ? 1 : -1;
      } // else

      // Generally reads are always high or low
      if (oldDiff.above) {
        final double maxDiff = value - maxValue;
        if (maxDiff > 0) {
          if (maxDiff < oldDiff.diff) {
            this.elements.put(elementName, new Diff(value, maxDiff, above));
            return 1;
          } // else
          return 0;
        } // else
        // Low Diff, check diff with low range value.
        final double minValue = getCellValue(lowCell);
        final double minDiff = minValue - value;
        if (minDiff < maxDiff) {
          // Closer value;
          this.elements.put(elementName, new Diff(value, minDiff, !above));
          return -1;
        } // else
        return 0;
      } // else previous read was a low value
      final double minValue = getCellValue(lowCell);
      final double minDiff = minValue - value;
      if (minDiff > 0) {
        if (minDiff < oldDiff.diff) {
          this.elements.put(elementName, new Diff(value, minDiff, !above));
          return -1;
        } // else
        return 0;
      } // else a high value, check diff with high range value.
      final double maxDiff = value - maxValue;
      if (maxDiff < minDiff) {
        this.elements.put(elementName, new Diff(value, maxDiff, above));
        return 1;
      }
      return 0;
      // end outOfRange
    }

    /**
     * @param element the Element to get the value for.
     * @return the value for the Element that is closest to the Range Limit.
     */
    public double getValue(final String element) {
      return this.elements.get(element).value;
      // end getValue
    }

    // end QCStatus
  }

  /**
   * The directory with the reports and configuration files.
   */
  protected File workingDir;
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

  /**
   * Map of Report Sample names to QC Section Names.
   */
  protected Map<String, String> mappings;

  /**
   * Map of Range Names and first row in the Range.
   */
  protected Map<String, Integer> rangeStarts;

  /**
   * The column with the ranges.
   */
  protected int rangeColumn;

  /**
   * The number of rows in a section.
   */
  protected int sectionSize;

  /**
   * The directory with the Reports.
   */
  protected File reportDir;

  /**
   * @param month      the month being processed
   * @param year       the year being processed
   * @param workbook   the workbook to write the data to.
   * @param workingDir The directory with the reports and configuration files.
   */
  public Processor(final String month, final String year, final Workbook workbook, final File workingDir) {
    super();
    this.month = month;
    this.year = year;
    this.workbook = workbook;
    this.workingDir = workingDir;
    // end <init>
  }

  /**
   * Load the ini style configuration file loader.config. If not found in the
   * local directory, the default configuration will be loaded.
   * 
   * @return The Profile wrapping the configuration file.
   * @throws IOException if the configuration file can not be loaded.
   */
  protected Profile loadConfig() throws IOException {
    final File loaderConfig = new File(".", "loader.config");
    try (final InputStream configStream = loaderConfig.exists() ? new FileInputStream(loaderConfig)
        : Thread.currentThread().getContextClassLoader().getResourceAsStream("loader.config");) {
      final Wini config = new Wini(configStream);
      this.mappings = config.get("Mappings");
      loadRangeStarts(config);
      this.rangeColumn = getCharValue(config.get("Template"), "column.ranges") - 'A';
      this.firstRow = getIntValue(config.get("Template"), "section.firstRow");
      this.reportDir =
          new File(this.workingDir, System.getProperty("sample.dir", config.get("General", "sample.dir")));
      LOGGER.info("Loading Configs from: {}", this.reportDir.getCanonicalPath());
      return config;
    }
    // end loadConfig
  }

  /**
   * Get the char value of a configuration item.
   * 
   * @param section   The section with the configuration settings containing the
   *                    key.
   * @param configKey The lookup key for the configuration item
   * @return the character value of the configuration item.
   * @throws NullPointerException thrown if the configKey is not defined in the
   *                                configuration file.
   */
  protected static char getCharValue(final Section section, final String configKey) {
    return section.get(configKey).trim().charAt(0);
    // end getCharValue
  }

  /**
   * Get the char value of a configuration item.
   * 
   * @param section   The section with the configuration settings containing the
   *                    key.
   * @param configKey The lookup key for the configuration item
   * @return the character value of the configuration item.
   * @throws NullPointerException thrown if the configKey is not defined in the
   *                                configuration file.
   */
  protected static int getIntValue(final Section section, final String configKey) {
    return Integer.parseInt(section.get(configKey).trim());
    // end getIntValue
  }

  /**
   * Round the report value to the precision specified in the range cell style.
   * 
   * @param style    The CellStyle with the precision.
   * @param repValue The Report Value.
   */
  protected static double roundRepValue(final CellStyle style, final String repValue) {
    final String format = style.getDataFormatString().trim();
    final int precision = format.length() - format.indexOf('.') - 1;
    final BigDecimal bd = new BigDecimal(repValue).setScale(precision, RoundingMode.HALF_UP);
    final double value =  bd.doubleValue();
    return value < 0 ? 0 : value;
    // end roundRepValue
  }

  /**
   * Compare two double values within a tolerance range.
   * 
   * @param style      the CellStyle with the precision.
   * @param repValue   the Value from the Report File.
   * @param rangeValue the range limit value from the QC Spreadsheet.
   * @return
   *         <ul>
   *         <li>+1 if the repValue > rangeValue + tolerance</li>
   *         <li>0 if the values are equal within range</li>
   *         <li>-1 otherwise</li>
   */
  protected static int valueCompare(final CellStyle style, final double repValue, final double rangeValue) {
    final String formatString = style.getDataFormatString();
    try {
    final double threshold = formatString.length() <= 2 ? 0 : // HACK for Template format not set.
        Double.parseDouble(formatString + "1");
    return Math.abs(repValue - rangeValue) < threshold ? 0 :
      repValue > rangeValue + threshold ? 1 : -1
    ;
} catch (NumberFormatException e) {
  LOGGER.debug("Exception processing Range Format: {}", e.getMessage(), e);
  LOGGER.debug("Range Cell Format set to {}", formatString);
  return Math.abs(repValue - rangeValue) == 0 ? 0 :
    repValue > rangeValue ? 1 : -1
  ;
}
    // end valueCompare
  }

  /**
   * Load the starting row for each of the ranges in to the map.
   * 
   * @param config the config file wrapper.
   */
  protected void loadRangeStarts(final Profile config) {
    final Set<String> rangeNames = new HashSet<>(this.mappings.values());
    final int rangeCount = rangeNames.size();
    this.rangeStarts = new HashMap<>(rangeCount);
    final Section templateSection = config.get("Template");
    // Row Numbers are 1-Offset, but we need to use 0-Offset for the worksheet.
    final int rowFirstName = getIntValue(templateSection, "section.firstRow") - 1
        + getIntValue(templateSection, "section.nameRow") - 1;
    final int startDiff = getIntValue(templateSection, "section.nameRow")
        - getIntValue(templateSection, "section.firstRow") + 1; // Skip the "Flag" row and start with the
                                                                // actual range row.
    final int rangeSize = getIntValue(templateSection, "section.numRows");
    final Sheet firstSheet = this.workbook.getSheetAt(0);
    for (int row = rowFirstName; this.rangeStarts.size() < rangeCount && row < firstSheet.getLastRowNum();
        row += rangeSize) {
      final String rangeName = firstSheet.getRow(row).getCell(0).getStringCellValue();
      if (rangeNames.contains(rangeName)) {
        this.rangeStarts.put(rangeName, Integer.valueOf(row - startDiff));
      }
    }
    this.sectionSize = rangeSize * rangeCount;
    // loadRangeStarts
  }

  /**
   * Get the first row for the range.
   * 
   * @param name    The name of the range to get the first row for.
   * @param machine the number of the machine being processed.
   * @return The first row of the range.
   */
  protected int getRangeTopRow(final String name, final int machine) {
    final String rangeName = this.mappings.get(name);
    if (rangeName == null) {
      // Not a valid range. Might be a sample.
      return -1;
    } // else
    final Integer rangeStart = this.rangeStarts.get(rangeName);
    if (rangeStart == null) {
      throw new NullPointerException("Mapped Range not defined in Template Sheet: " + rangeName);
    }
    return rangeStart.intValue() + ((machine - 1) * this.sectionSize); // Machine is 0-Offset
    // end getRangeTopRow
  }

  /**
   * Style to use for the Date Cells.
   */
  protected CellStyle dateStyle;
  
  /**
   * Style to use for the Flag Cells.
   */
  protected CellStyle flagStyle;
  
  /**
   * First Row with data.
   */
  protected int firstRow;

  /**
   * Format the column with the same format as and precision as the range. Also
   * formats the Date Cell to match the Range Background.
   * 
   * @param colRanges The column with the Ranges.
   * @param dateCol The column with the current day being processed.
   * @param sectionFirstRow The first row of data in the section.
   * @param sectionLastRow The last row of data in the section.
   */
  protected void formatColumn(final int colRanges, final int dateCol, final int sectionFirstRow,
      final int sectionLastRow) {
    for (final Iterator<Sheet> it = this.workbook.sheetIterator(); it.hasNext();) {
      final Sheet currentSheet = it.next();
      // Do Date Row
      final Row dateRow = currentSheet.getRow(this.firstRow - 1);
      if (dateRow == null) {
        // Not a Data Sheet
        continue;
      }
      final Cell dateCell = dateRow.getCell(dateCol);
      if (this.dateStyle == null) {
        this.dateStyle = this.workbook.createCellStyle();
        this.dateStyle.cloneStyleFrom(dateCell.getCellStyle());
        final CellStyle rangeStyle = dateRow.getCell(colRanges).getCellStyle();
        this.dateStyle.setFillPattern(rangeStyle.getFillPattern());
        this.dateStyle.setFillBackgroundColor(rangeStyle.getFillBackgroundColor());
        this.dateStyle.setFillForegroundColor(rangeStyle.getFillForegroundColor());
      }
      dateCell.setCellStyle(this.dateStyle);

      // Do Data Rows
      for(int i = sectionFirstRow; i < sectionLastRow;i++) {
        final Row currentRow = currentSheet.getRow(i);
        if (currentRow != null) {
          final Cell rangeCell = currentRow.getCell(colRanges);
          if (rangeCell != null) {
            final CellStyle rangeStyle = rangeCell.getCellStyle();
            if (rangeCell.getCellType() != CellType.NUMERIC && rangeCell.getCellType() != CellType.BLANK) {
              LOGGER.warn("NonNumber Range Cell defined - {}:{}:{} = {}|{}", currentSheet.getSheetName(),
                  Character.valueOf((char) ('A' + colRanges)), Integer.valueOf(i + 1),
                  rangeCell.getCellType(), rangeStyle.getDataFormatString());

            }
            final Cell currentCell = currentRow.getCell(dateCol, MissingCellPolicy.CREATE_NULL_AS_BLANK);
            currentCell.setCellStyle(rangeStyle);
          } else {
            LOGGER.warn("Range Cell not defined - {}:{}:{}", currentSheet.getSheetName(),
                Character.valueOf((char) ('A' + colRanges)), Integer.valueOf(i + 1));
          }
        }
      }
    }
    // end formatColumn
  }

  /**
   * @param cell the cell to get the value for.
   * @return the value of the cell if numeric, 0 if not a numeric cell.
   */
  protected static double getCellValue(final Cell cell) {
    return
    // HACK - If median not a numeric range Type, use 0 until Template is fixed.
    cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : 0;
    // end getCellValue
  }

  /**
   * Process the Rep files
   * 
   * @throws IOException thrown if the configuration file can not be loaded.
   */
  @SuppressWarnings("null") // False positive on qcStatus access.
  public void process() throws IOException {
    // Load Maps and Configs
    final Profile config = loadConfig();
    final Section templateSection = config.get("Template");
    final int colDayStart = getCharValue(templateSection, "column.day1") - 'A';
    final int colRanges = getCharValue(templateSection, "column.ranges") - 'A';
    final Section generalSection = config.get("General");
    final int maxTry = getIntValue(generalSection, "sample.try");
    final int machineCount = getIntValue(generalSection, "machine.count");
    final String machineBaseName = config.get("General", "machine.base.name");
    // Get the number of days in that month
    final YearMonth yearMonthObject =
        YearMonth.of(Integer.valueOf(this.year).intValue(), Integer.valueOf(this.month).intValue());
    final int days = yearMonthObject.lengthOfMonth();
    for (int machine = 1; machine <= machineCount; machine++) {
      final File machineDir = new File(this.reportDir, machineBaseName + machine);
      LOGGER.info("Loading Reports from: {}", machineDir.getCanonicalPath());

      // Loop through the rep files
      for (int day = 1; day <= days; day++) {
        final String fileName =
            this.month + (day < 10 ? "0" + day : Integer.toString(day)) + this.year.substring(2) + ".rep";
        final File repFile = new File(machineDir, fileName);
        if (!repFile.exists()) {
          continue;
        }
        final Map<String, QCStatus> processedChecks = new HashMap<>();

        //Have a file, format the columns
        //Day is 1-Offset, Columns are 0-Offset.
        //Machine is 1-Offset, Rows are 0-Offset.
        final int currentSectionStart = this.firstRow - 1 //FirstRow is 1-Offset, Rows are 0-Offset
            + ((machine - 1) * this.sectionSize);
        final int currentSectionEnd = currentSectionStart + this.sectionSize;
        formatColumn(colRanges, colDayStart + day - 1, currentSectionStart, currentSectionEnd); 
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
            final String sampleName = rec.get(0);
            final int rangeTopRow = getRangeTopRow(sampleName, machine);
            final boolean processSample;
            QCStatus qcStatus;
            if (rangeTopRow > 0) {
              // If a Range to process, track the attempts, don't process if exceeded
              qcStatus = processedChecks.get(sampleName);
              if (qcStatus == null) {
                qcStatus = new QCStatus();
                processedChecks.put(sampleName, qcStatus);
              }
              processSample = !qcStatus.isDone() && qcStatus.increment() <= maxTry;
            } else {
              // not a range to process.
              processSample = false;
              qcStatus = null;
            }

            // Skip Date Row
            iter.next();
            final boolean firstAttempt = processSample && qcStatus.getCount() == 1;
            for (; iter.hasNext() && (rec = iter.next()).size() > 0 && rec.get(0).startsWith("|");) {
              if (processSample) {
                // Will generally be LI on first cycle. Data is in Column 4;
                final String element = rec.get(1);
                // If not on first record, only process element that was previously OutOfRange
                if (!firstAttempt && !qcStatus.isOutOfRange(element)) {
                  continue;
                } // else
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
                CellStyle alertStyle = null; //One AlertStyle per machine/tab combination.
                // number of days added to the column subtracting 1 for 0-Offset of POI
                final Cell medianCell = qcSheet.getRow(rangeTopRow + 2).getCell(colRanges);
                final double median = getCellValue(medianCell);
                int trackNumber = 0;

                final CellStyle medianStyle = medianCell.getCellStyle();
                // Compute Report Value to appropriate precision.
                final double repValue = roundRepValue(medianStyle, rawRepValue);
                boolean ooRange = false;
                switch (valueCompare(medianStyle, repValue, median)) {
                case 1: {
                  final Cell maxCell = qcSheet.getRow(rangeTopRow).getCell(colRanges);
                  final double maxValue = getCellValue(maxCell);
                  if (repValue <= maxValue) {
                    final Cell rangeCell = qcSheet.getRow(rangeTopRow + 1).getCell(colRanges);
                    final double rangeValue = getCellValue(rangeCell);
                    trackNumber = valueCompare(rangeCell.getCellStyle(), repValue, rangeValue) > 0 ? 0 : 1;
                    qcStatus.clearOutOfRange(element);
                  } else {
                    // Out of Range Value
                    qcStatus.outOfRange(element, maxCell, qcSheet.getRow(rangeTopRow + 4).getCell(colRanges),
                        repValue);
                    ooRange = true;
                    trackNumber = 0;
                  }
                  break;
                }
                case 0:
                  trackNumber = 2;
                  qcStatus.clearOutOfRange(element);
                  break;
                default: {
                  final Cell minCell = qcSheet.getRow(rangeTopRow + 4).getCell(colRanges);
                  final double minValue = getCellValue(minCell);
                  if (repValue >= minValue) {
                    final Cell rangeCell = qcSheet.getRow(rangeTopRow + 3).getCell(colRanges);
                    final double rangeValue = getCellValue(rangeCell);
                    trackNumber = valueCompare(rangeCell.getCellStyle(), repValue, rangeValue) < 0 ? 4 : 3;
                    qcStatus.clearOutOfRange(element);
                  } else {
                    // Out of Range Value
                    qcStatus.outOfRange(element, qcSheet.getRow(rangeTopRow).getCell(colRanges), minCell,
                        repValue);
                    ooRange = true;
                    trackNumber = 4;
                  }
                  break;
                }
                }
                if (ooRange == false) {
                  final Cell currentCell =
                      qcSheet.getRow(rangeTopRow + trackNumber).getCell(day + colDayStart - 1);
                  currentCell.setCellValue(repValue);
                } else {
                  if (qcStatus.getCount() >= maxTry) {
                    // Write last value and set Color to Red.
                    alertStyle =
                        processOoR(qcSheet, element, qcStatus, alertStyle, rangeTopRow, day, colDayStart);
                  }
                }
              } // else, not a mapped QC Row, or done processing QC Rows
            } // end for elements in CVS Rec
            if (qcStatus != null) {
              if (qcStatus.hasOutOfRange()) {
                if (qcStatus.getCount() >= maxTry) {
                  qcStatus.setDone();
                } // else, still have items to process so don't set done.
              } else {
                qcStatus.setDone();
              }
            } // else, not a range check
              // Don't process the entire REP File! Once all QC's are loaded, skip to next
              // file!
            if (allStatusDone(processedChecks.values())) {
              break;
            }
          } // end for CSVRecord rec
        } catch (IOException e) {
          throw new RuntimeException("Error reading file: " + fileName, e);
        }
        //Do any leftover OoR
        processLeftOverOOR(processedChecks, machine, day, colDayStart);
      } // end for day
    } // end for machine

    // end process
  }
  
  /**
   * Process and Out Of Range (OoR) value.  This routine will write the OoR value to the appropriate
   * cell and the value and flag cell fonts, background, and formatting appropriately.
   * @param qcSheet     The Sheet for the element being written to.
   * @param element     The Element being processed.
   * @param qcStatus    The Status of the QC Sample with the OoR values being processed.
   * @param alertStyle  The style to use for the alert cell.  Includes precision from the range.  Can be null.
   * @param rangeTopRow The first row in the range for the value.
   * @param day         The day of the month being processed.  1-Offset value.
   * @param colDayStart The column that the days start in. 0-Offset value
   * @return            New {@link CellStyle} for alerts if alertStyle was null (not yet defined).
   */
  protected CellStyle processOoR(final Sheet qcSheet, final String element, final QCStatus qcStatus,
      CellStyle alertStyle, final int rangeTopRow, final int day, final int colDayStart) {

    // Write last value and set Color to Red.
    final Cell currentCell = qcSheet.getRow(rangeTopRow + (qcStatus.isHigh(element) ? 0 : 4))
        .getCell(day + colDayStart - 1);
    currentCell.setCellValue(qcStatus.getValue(element));
    if (alertStyle == null) {
      // Assign previously null value.
      alertStyle = this.workbook.createCellStyle();  //NOSONAR - Intentional assignment.
      alertStyle.cloneStyleFrom(currentCell.getCellStyle());
      final Font valueFont = this.workbook.getFontAt(alertStyle.getFontIndexAsInt());
      final Font alertFont = this.workbook.createFont();
      alertFont.setFontHeight(valueFont.getFontHeight());
      alertFont.setColor(Font.COLOR_RED);
      alertStyle.setFont(alertFont);
    }
    currentCell.setCellStyle(alertStyle);
    //Set Flag Cell (the one above or below the Range) to red.
    final Cell flagCell = 
        qcSheet.getRow(rangeTopRow + (qcStatus.isHigh(element) ? -1 : 5))
        .getCell(day + colDayStart - 1);
    if (this.flagStyle == null) {
      this.flagStyle = this.workbook.createCellStyle();
      this.flagStyle.cloneStyleFrom(flagCell.getCellStyle());
      this.flagStyle.setFillForegroundColor(IndexedColors.RED1.getIndex());
      this.flagStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      
    }
    flagCell.setCellStyle(this.flagStyle);
    //Return current working value.  Allow caller to capture reference if needed.
    //Note that LibreOffice only supports up to 4K Styles defined in the entire spreadsheet, so
    //need to share styles where possible.
    return alertStyle;
    //end processOoR
  }

  /**
   * Process and Out of Range values where there we less than Max Try QC Samples.
   * @param processedChecks The QC Checks that were processed.
   * @param machine Index of the Machine being processed.  1-Offset value.
   * @param day The day being processed.  1-Offset value.
   * @param colDayStart The column that the days start in. 0-Offset value.
   */
  protected void processLeftOverOOR(final Map<String, QCStatus> processedChecks, final int machine,
      final int day, final int colDayStart) {
    for (final Map.Entry<String, QCStatus> currentCheck : processedChecks.entrySet()) {
      final String sampleName = currentCheck.getKey();
      final QCStatus qcStatus = currentCheck.getValue();
      if (!qcStatus.isDone() && qcStatus.hasOutOfRange()) {
        //Insufficient QC Checks to find an in-range value.  Go with the current value.
        for (final String element : qcStatus.elements.keySet()) {
          final Sheet qcSheet = this.workbook.getSheet(element);
          processOoR(qcSheet, element, qcStatus, null, getRangeTopRow(sampleName, machine), day, colDayStart);
        }//end for element
      }//end OoR value
    }//end for currentCheck
    //end processLeftOverOOR
  }

  /**
   * @param processedChecks The Checks that have been processed.
   * @return false if any {@link QCStatus} values are not done, true otherwise.
   */
  protected boolean allStatusDone(final Collection<QCStatus> processedChecks) {
    if (processedChecks.size() < this.mappings.size()) {
      return false;
    } // else
    for (final QCStatus currentStatus : processedChecks) {
      if (!currentStatus.isDone()) {
        return false;
      }
    }
    return true;
    // end allStatusDone
  }

  /**
   * Save the Workbook.
   * 
   * @param out The OutputStream to write the Workbook to.
   * @throws IOException thrown if an exception occurs writing to the output file.
   */
  protected void write(final OutputStream out) throws IOException {
    this.workbook.write(out);
    out.flush();
    // end write
  }

}
