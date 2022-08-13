# QAAnalyticsLoader
Loads Report Data into a Spreadsheet (or other) template for QA Analytics usage.  Initially for Mass 
Spectrometry with data from a Perkin Elmer Nexion. 


## Known Issues
### Only first sample is read
Currently only the first set of QC Samples are read.  If the first set have out-of-range values, see section
on "Out of Range values".

### Out of Range values
Currently out of Range Values are not handled.  Any out-of-range values need to be manually handled.
