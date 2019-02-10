#Get Date
Query user for month(s) and year to process.
(NOTE: For the initial version, stick to a single calendar year)

#Process Records:
QC Std [1|2]
China Hair
Hair Pool

##Mapping Table
QC Std 1 -> Low Check QC
QC Std 2 -> High Check QC

##Rules
##Rep file (CSV formatted) -> XLS file
1. 1 Spreadsheet covers one month
2. 1 .rep file covers 1 day
##Month & Year - Available in Rep filename
##Process Date
- Available in Rep filename
- Available in Spreadsheet Row 2 (? - Does template need to be updated?)
##Element Name:
- Rep File (1-Offset): (Col 2/B)
- Spreadsheet: Sheet Name
##Concentration 
- Rep File (1-Offset): (Col 5/E)
- Spreadsheet: Matching Date Column
 

##Pick first in Range
- Limit values go in Limit Row
- Acceptable values go in appropriate "Range", start with -2SD to max of +2SD

- No Data - Column is Blue Highlight

**Special Handing for China Hair and Hair Pool not in Range**
Only ONE needs to be in Range.
Out of Range values need to be highlighted in Range
Red Block needs to be added to top of Column Section.

Read (1-Offset):
Element (Col 2/B)
Concentration (Col 5/E)

NOTE: Some items such as Column/Cell colors can be and possibly should be handled by Spreadsheet formulas and Macros.
