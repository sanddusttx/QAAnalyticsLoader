/*
 * Copyright (c) 2019
 * This code is licensed under the GPLv2.  Please contact Sanddust at
 * sanddust@j2eeguys.com for additional licenses. 
 */
/**
 * This package contains the code to setup and run the QA Analytics Loader.
 * <ul>
 * <li>{@link com.j2eeguys.qaanalytics.loader.Controller} handles:
 * <ul>
 * <li>opening up the template files for reading and writing.</li>
 * <li>building the list of files for processing</li>
 * <li>building the directory handles for reading/writing</li>
 * </ul>
 * <li>{@link com.j2eeguys.qaanalytics.loader.Processor} reads the input files
 * and generates the output QC Spreadsheet based on the supplied or default
 * template.</li>
 * </ul>
 * 
 * @author Sanddust sanddust@j2eeguys.com
 *
 */
package com.j2eeguys.qaanalytics.loader;
