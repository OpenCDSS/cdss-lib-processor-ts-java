// TSCommandProcessorUtil - This class contains static utility methods to support TSCommandProcessor.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

CDSS Time Series Processor Java Library is free software:  you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

CDSS Time Series Processor Java Library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

You should have received a copy of the GNU General Public License
    along with CDSS Time Series Processor Java Library.  If not, see <https://www.gnu.org/licenses/>.

NoticeEnd */

package rti.tscommandprocessor.core;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.StringBuffer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import rti.tscommandprocessor.commands.ts.ReadTimeSeries_Command;
import RTi.DMI.DatabaseDataStore;
import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSIdent;
import RTi.TS.TSUtil_SortTimeSeries;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusProvider;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatusUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ProcessRunner;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.IO.WarningCount;
import RTi.Util.JSON.JSONObject;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.DateTimeRange;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleSequence;
import freemarker.template.Template;
import freemarker.template.Version;
import riverside.datastore.DataStore;
import riverside.datastore.WebServiceDataStore;

/**
This class contains static utility methods to support TSCommandProcessor.
These methods are here to prevent the processor from getting too large and in some cases because code is being migrated.
*/
public abstract class TSCommandProcessorUtil
{

/**
Output file for regression test results report.
*/
private static String __regression_test_file = null;

/**
PrintWriter for regression test results report.
*/
private static PrintWriter __regression_test_fp = null;

/**
Count of regression tests that fail (does not include disabled tests).
*/
private static int __regressionTestFailCount = 0;

/**
Count of regression tests that pass (does not include disabled tests).
*/
private static int __regressionTestPassCount = 0;

/**
Count of regression tests that are disabled (not included in pass/fail tests).
*/
private static int __regressionTestDisabledCount = 0;

/**
Table to contain regression test results.
*/
private static DataTable __regressionTestTable = null;

/**
Append a time series to the processor time series results list.
@param processor the CommandProcessor to use to get data.
@param command Command for which to get the working directory.
@param ts Time series to append.
@return return the number of warnings generated.
*/
public static int appendEnsembleToResultsEnsembleList ( CommandProcessor processor, Command command, TSEnsemble tsensemble ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".appendEnsembleToResultsEnsembleList";
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "TSEnsemble", tsensemble );
    int warning_level = 3;
    int warning_count = 0;
    CommandStatus status = null;
    if ( command instanceof CommandStatusProvider ) {
        status = ((CommandStatusProvider)command).getCommandStatus();
    }
    //CommandProcessorRequestResultsBean bean = null;
    try { //bean =
        processor.processRequest( "AppendEnsemble", request_params );
    }
    catch ( Exception e ) {
        String message = "Error requesting AppendEnsemble(TSEnsemble=\"...\") from processor).";
        // This is a low-level warning that the user should not see.
        // A problem would indicate a software defect so return the warning count as a trigger.
        Message.printWarning(warning_level, routine, e);
        Message.printWarning(warning_level, routine, message );
        if ( status != null ) {
            status.addToLog(CommandPhaseType.RUN,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Check the log file for details - report the problem to software support."));
        }
        ++warning_count;
    }
    return warning_count;
}

/**
Count of output lines in regression output report body (basically a count of the tests).
*/
private static int __regressionTestLineCount = 0;

/**
Add a record to the regression test results report and optionally results table.
The report is a simple text file that indicates whether a test passed.
The data table is a table maintained by the processor to report on test results.
@param processor CommandProcessor that is being run.
@param isEnabled whether the command file is enabled (it is useful to list all tests even if not
enabled in order to generate an inventory of disabled tests that need cleanup)
@param runTimeMs run time for the command in milliseconds
@param testPassFail whether the test was a success or failure
(it is possible for the test to be a successful even if the command file failed, if failure was expected)
@param expectedStatus the expected status (as a string)
@param maxSeverity the maximum severity from the command file that was run.
@param testCommandFile the full path to the command file that was run.
*/
public static void appendToRegressionTestReport(CommandProcessor processor, boolean isEnabled, long runTimeMs,
    String testPassFail, String expectedStatus, CommandStatusType maxSeverity,
    String testCommandFile ) {
    ++__regressionTestLineCount;
    String indicator = " ";
    String enabled = "TRUE   ";
    if ( isEnabled ) {
	    if ( testPassFail.toUpperCase().indexOf("FAIL") >= 0 ) {
	        indicator = "*";
	        ++__regressionTestFailCount;
	    }
	    else {
	        ++__regressionTestPassCount;
	    }
    }
    else {
        ++__regressionTestDisabledCount;
        enabled = "FALSE  ";
        testPassFail = "    ";
    }
    String lineCount = StringUtil.formatString(__regressionTestLineCount,"%5d");
    //String runTime = "        ";
    //runTime = StringUtil.formatString(runTimeMs,"%7d");
    String delim = "|";
    if ( __regression_test_fp != null ) {
        __regression_test_fp.println (
            lineCount + delim +
            enabled + delim +
            // Moved the runTime to the table because in the report it makes it difficult to "diff" previous and current reports.
            //runTime + delim +
            indicator + StringUtil.formatString(testPassFail,"%-4.4s") + indicator + delim +
            StringUtil.formatString(expectedStatus,"%-10.10s") + delim +
            StringUtil.formatString(maxSeverity,"%-10.10s") + " " + delim + testCommandFile);
    }
    if ( __regressionTestTable != null ) {
    	TableRecord rec = __regressionTestTable.emptyRecord();
    	// Look up the column numbers using the names from the table initialization - make sure they agree.
    	int col = -1;
    	try {
    		col = __regressionTestTable.getFieldIndex("Num");
    		rec.setFieldValue(col, new Integer(__regressionTestLineCount));
    		col = __regressionTestTable.getFieldIndex("Enabled");
    		rec.setFieldValue(col, enabled.trim());
    		col = __regressionTestTable.getFieldIndex("Run Time (ms)");
    		rec.setFieldValue(col, runTimeMs);
    		col = __regressionTestTable.getFieldIndex("Test Pass/Fail");
    		rec.setFieldValue(col, testPassFail.trim());
    		col = __regressionTestTable.getFieldIndex("Commands Expected Status");
    		rec.setFieldValue(col, expectedStatus.trim());
    		col = __regressionTestTable.getFieldIndex("Commands Actual Status");
    		rec.setFieldValue(col, ""+maxSeverity);
    		col = __regressionTestTable.getFieldIndex("Command File");
    		rec.setFieldValue(col, testCommandFile);
    		__regressionTestTable.addRecord(rec);
    	}
    	catch ( Exception e ) {
    		// Just ignore adding the test record to the table.
    	}
    }
}

/**
Append a time series list to the processor time series results list.
Errors should not result and are logged in the log file and command status, indicating a software problem.
@param processor the CommandProcessor to use to get data.
@param command Command for which to get the working directory.
@param tslist List of time series to append.
@return return the number of warnings generated.
*/
public static int appendTimeSeriesListToResultsList ( CommandProcessor processor, Command command, List<TS> tslist ) {
    int wc = 0;
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    for ( int i = 0; i < size; i++ ) {
        wc += appendTimeSeriesToResultsList ( processor, command, tslist.get(i) );
    }
    return wc;
}

/**
Append a time series to the processor time series results list.
@param processor the CommandProcessor to use to get data.
@param command Command for which to get the working directory.
@param ts Time series to append.
@return return the number of warnings generated.
*/
public static int appendTimeSeriesToResultsList ( CommandProcessor processor, Command command, TS ts ) {
	String routine = TSCommandProcessor.class.getSimpleName() + ".appendTimeSeriesToResultsList";
	PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "TS", ts );
    int warning_level = 3;
    int warning_count = 0;
    CommandStatus status = null;
    if ( command instanceof CommandStatusProvider ) {
        status = ((CommandStatusProvider)command).getCommandStatus();
    }
	//CommandProcessorRequestResultsBean bean = null;
	try { //bean =
		processor.processRequest( "AppendTimeSeries", request_params );
	}
	catch ( Exception e ) {
		String message = "Error requesting AppendTimeSeries(TS=\"...\") from processor).";
        // This is a low-level warning that the user should not see.
        // A problem would indicate a software defect so return the warning count as a trigger.
		Message.printWarning(warning_level, routine, e);
		Message.printWarning(warning_level, routine, message );
        if ( status != null ) {
            status.addToLog(CommandPhaseType.RUN,
                    new CommandLogRecord(
                    CommandStatusType.FAILURE, message,
                    "Check the log file for details - report the problem to software support."));
        }
        ++warning_count;
	}
    return warning_count;
}

/**
Close the regression test report file.
Also create a file that is the same but with name including '.nonum' at the end that
does not have test numbers, which facilitates comparing the results, for example in KDiff3.
*/
public static void closeRegressionTestReportFile () {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".closeRegressionTestReportFile";
    if ( __regression_test_fp == null ) {
        return;
    }
    __regression_test_fp.println ( "#----+-------+------+----------+-----------+" +
        "---------------------------------------------------------------------------------------------");
    int totalCount = getRegressionTestFailCount() + getRegressionTestPassCount() + getRegressionTestDisabledCount();
    __regression_test_fp.println ( "FAIL count     = " + StringUtil.formatString(getRegressionTestFailCount(), "%5d") +
        ", " + StringUtil.formatString(100.0*(double)getRegressionTestFailCount()/(double)totalCount,"%7.3f")+ "%");
    __regression_test_fp.println ( "PASS count     = " + StringUtil.formatString(getRegressionTestPassCount(), "%5d") +
        ", " + StringUtil.formatString(100.0*(double)getRegressionTestPassCount()/(double)totalCount,"%7.3f")+ "%");
    __regression_test_fp.println ( "Disabled count = " + StringUtil.formatString(getRegressionTestDisabledCount(), "%5d") +
    	", " + StringUtil.formatString(100.0*(double)getRegressionTestDisabledCount()/(double)totalCount,"%7.3f")+ "%");
    __regression_test_fp.println ( "#--------------------------------" );
    __regression_test_fp.println ( "Total          = " + StringUtil.formatString(totalCount, "%5d") );

    // Close the normal results file.
    __regression_test_fp.close();

    // Create the report file with no test numbers.
    // The 'sed' command that works is as follows but is cumbersome because
    // have to determine a bash shell to run and the report file is not closed and therefore the following output is incomplete:
    //   RunProgram(CommandLine="C:\Users\${UserName}\AppData\Local\Programs\Git\
    //     usr\bin\bash.exe --login -c \"sed 's/^ [ 0-9][ 0-9][ 0-9][ 0-9]/     /'
    //     ${WorkingDirPosix}/Results/RunRegressionTest_commands_general_Windows_out_${NowString}.txt >
    //     ${WorkingDirPosix}/Results/RunRegressionTest_commands_general_Windows_out_${NowString}.nonum.txt\"")
    //
    // Need to replace numbers in lines like the following with spaces:
    //     1|

    // Create a filename that has extension: .nonum.ext
    String ext = IOUtil.getFileExtension(__regression_test_file);
    String outputFile = null;
    if ( ext == null ) {
    	outputFile = __regression_test_file + ".nonum.txt";
    }
    else {
    	outputFile = __regression_test_file.substring(0,__regression_test_file.length() - ext.length( )) + "nonum." + ext;
    }
   	PrintWriter ofp = null;
   	BufferedReader ifp = null;
    try {
    	// Read the normal report file, which contains the test number in the left column.
	    ifp = new BufferedReader ( new InputStreamReader(IOUtil.getInputStream( __regression_test_file) ));
    	// Open a second report file for 'nonum' output.
    	ofp = new PrintWriter ( new FileOutputStream ( outputFile, false ) );
    	String line = null;
		while ( true ) {
			line = ifp.readLine();
			if ( line == null ) {
				break;
			}
    		line = line.replaceAll("^[ 0-9][ 0-9][ 0-9][ 0-9][ 0-9]", "     ");
    		ofp.println(line);
    	}
    }
    catch ( IOException e ) {
    	Message.printWarning(2, routine, "Error creating 'nonum' report file: " + outputFile );
    }
    finally {
    	if ( ifp != null ) {
    		ofp.close();
    	}
    	if ( ofp != null ) {
    		ofp.close();
    	}
    }
}

/**
Convert a time series identifier to a read command.
@param processor the time series command processor that manages data, needed to retrieve data stores, etc.
@param tsid a time series identifier to control the conversion to a read command.
@param requireSpecific if true then a specific ReadXXX() command is required; if false then it is OK
to convert to the more general ReadTimeSeries() command.
*/
public static Command convertTSIDToReadCommand ( TSCommandProcessor processor, String tsid, boolean requireSpecific )
throws Exception {
    // First create a TSIdent object.
    //TSIdent tsident = new TSIdent ( tsid );
    // Figure out if there is an input type and name.
    //String inputType = tsident.getInputType();
    //String inputName = tsident.getInputName();
    // TODO SAM 2011-04-04 Here need to check for matching data stores, etc. to know the command to use for the TSID.
    boolean specificCreated = false; // Whether specific read command was created.
    if ( requireSpecific ) {
        // Try to create a specific read command as requested.
    }
    if ( !specificCreated ) {
        if ( requireSpecific ) {
            // Could not create a specific command but it was requested.
            throw new RuntimeException ( "Conversion of TSID to specific read command for TSID=\"" + tsid +
                 "\" has not been implemented.  Use ReadTimeSeries() instead." );
        }
        else {
            // Create a ReadTimeSeriesCommand and let it do the rest of the error handling during discovery and run modes.
            ReadTimeSeries_Command readCommand = new ReadTimeSeries_Command();
            readCommand.getCommandParameters().set("TSID",tsid);
            // TODO SAM 2011-04-04 Later need to phase out alias when commands have been converted to.
            // Version 10 syntax.
            readCommand.getCommandParameters().set("Alias","SpecifyAlias");
            readCommand.setCommandProcessor(processor);
            Message.printStatus(2, "", "Created new command \"" + readCommand + "\"" );
            return readCommand;
        }
    }
    return null;
}

// TODO SAM 2016-03-24 May move this to class that focuses on UI
// and make generic so as to not hard-code TSTool documentation path.
// Maybe have some configuration/hooks on the processor to define more properties like ${CommandDocRootURL}.
/**
Display the command documentation.  This will use the default web browser.
@param command command to display documentation.
*/
public static void displayCommandDocumentation ( Command command ) {
	try {
		// TODO SAM 2016-03-23 This is a prototype of how to do interactive documentation - put in utility code.
		String docURL = null;
		boolean isCommandPlugin = command.getIsCommandPlugin();
		if ( isCommandPlugin ) {
			// Envision that command documentation would be in:
			// $home/.tstool/plugin-command/CommandName/doc/CommandName.html or CommandName.pdf
			Prop prop = command.getCommandProcessor().getProp("UserTstoolDirURL");
			if ( prop != null ) {
				docURL = prop.getValue() + "/plugin-command/" + command.getCommandName() + "/doc/" +
				command.getCommandName() + ".html";
			}
		}
		else {
			Prop prop = command.getCommandProcessor().getProp("InstallDirURL");
			if ( prop != null ) {
				docURL = prop.getValue() + "/doc/UserManual/html/TSTool-Vol2-CommandReference/" +
				command.getCommandName() + "/" + command.getCommandName() + ".html";
			}
		}
		if ( docURL != null ) {
			Desktop desktop = Desktop.getDesktop();
		    desktop.browse ( new URI(docURL) );
		}
	}
	catch ( Exception err ) {
		Message.printWarning(1,"","Error displaying documentation (" + err + ")");
	}
}

/**
Expand a string containing command discovery properties.  For example, a parameter value like
"${DataStore}" will be expanded to include the datastore name set using a property.
The characters \" will be replaced by a literal quote (").  Properties that cannot be expanded will remain.
This method may be called with an incomplete ${Property} string,
such as if called from an editor while characters are typed.
@param props the a list of Prop set in discovery mode by commands
@param command the command that is being processed (may be used later for context sensitive values).
@param parameterValue the parameter value being expanded, containing literal substrings and optionally ${Property} properties.
@return the expanded string or null if the input string was null
*/
public static String expandParameterDiscoveryValue( List<Prop> props, Command command, String parameterValue ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".expandParameterValue";
    if ( (parameterValue == null) || parameterValue.isEmpty() || (parameterValue.indexOf("${") < 0) ) { // }
    	// Nothing to expand:
    	// - null and empty string can't expand
    	// - if no ${Property} notation can't expand
        // - just return the input
        return parameterValue;
    }
   	for ( Prop prop : props ) {
     	// Print the properties, for troubleshooting.
        if ( Message.isDebugOn ) {
        	Message.printStatus(2, routine, "Discovery property list includes " + prop.getKey() + "=\"" + prop.getValue() +"\"");
        }
    }
    // First replace escaped characters.
    // Evaluate whether to write a general method for this - for now only handle // \" and \' replacement.
    parameterValue = parameterValue.replace("\\\"", "\"" );
    parameterValue = parameterValue.replace("\\'", "'" );
    // Else see if the parameter value can be expanded to replace ${} symbolic references with other values.
    // Search the parameter string for $ until all processor parameters have been resolved.
    int searchPos = 0; // Position in the "parameter_val" string to search for ${} references.
    int foundPos; // Position when leading ${ is found.
    int foundPosEnd; // Position when ending } is found.
    String propname = null; // Whether a property is found that matches the $ symbol.
    String delimStart = "${";
    String delimEnd = "}";
    while ( searchPos < parameterValue.length() ) {
        foundPos = parameterValue.indexOf(delimStart, searchPos);
        foundPosEnd = parameterValue.indexOf(delimEnd, (searchPos + delimStart.length()));
       	if ( Message.isDebugOn ) {
       		Message.printStatus ( 2, routine, "" + delimStart + " position=" + foundPos);
        	Message.printStatus ( 2, routine, "" + delimEnd + " position=" + foundPosEnd);
       	}
        if ( (foundPos < 0) || (foundPosEnd < 0) ) {
            // No more $ property names (or an incomplete property), so return what have:
        	// - for example, could be typing ${abx into an editor field and have not yet typed closing }
        	if ( Message.isDebugOn ) {
        		Message.printStatus ( 2, routine, "Don't have surrounding ${ and } so returning \"" + parameterValue + "\".");
        	}
            return parameterValue;
        }
        // Else found the delimiter so continue with the replacement.
        // Get the name of the property.
        propname = parameterValue.substring((foundPos+2),foundPosEnd);
        if ( Message.isDebugOn ) {
        	Message.printStatus ( 2, routine, "Trying to match property \"" + propname + "\" in property list.");
        }
        // Try to get the property from the processor.
        // TODO SAM 2007-12-23 Evaluate whether to skip null.  For now show the unexpanded property name in the result.
        Object propval = null;
        String propvalString = "";
        boolean foundProp = false;
       	// Loop through the props to find a matching property name.
       	for ( Prop prop : props ) {
       		if ( prop.getKey().equals(propname) ) {
       			// The following should work for all representations as long as the toString() does not truncate.
       			//propval = prop.getContents();
       			//propvalString = "" + propval;
       			propvalString = prop.getValue();
       			foundProp = true;
       			break;
       		}
        }
       	if ( !foundProp ) {
            // Keep the original literal value to alert user that property could not be expanded.
       		if ( Message.isDebugOn ) {
       			Message.printStatus ( 2, routine, "Did not match property \"" + propname + "\" in property list - not expanding property.");
       		}
            propvalString = delimStart + propname + delimEnd;
        }
        if ( propvalString == null ) {
            // Keep the original literal value to alert user that property could not be expanded.
       		if ( Message.isDebugOn ) {
       			Message.printStatus ( 2, routine, "Null value for property \"" + propname + "\" in property list - not expanding property.");
       		}
            propvalString = delimStart + propname + delimEnd;
        }
        // If here have a property.
   		if ( Message.isDebugOn ) {
   			Message.printStatus ( 2, routine, "Found property \"" + propname + "\" in property list, value=\"" + propvalString + "\".");
   		}
        StringBuffer b = new StringBuffer();
        // Append the start of the string.
        if ( foundPos > 0 ) {
            b.append ( parameterValue.substring(0,foundPos) );
        }
        // Now append the value of the property.
        b.append ( propvalString );
        // Now append the end of the original string if anything is at the end.
        if ( parameterValue.length() > (foundPosEnd + 1) ) {
            b.append ( parameterValue.substring(foundPosEnd + 1) );
        }
        // Now reset the search position to finish evaluating whether to expand the string.
        parameterValue = b.toString();
        searchPos = foundPos + propvalString.length(); // Expanded so no need to consider start and end delimiters.
        if ( Message.isDebugOn ) {
            Message.printDebug( 1, routine, "Expanded parameter discovery value is \"" + parameterValue +
                "\" searchpos is now " + searchPos + " in string \"" + parameterValue + "\"" );
        }
    }
    return parameterValue;
}

/**
Expand a string containing processor-level properties.  For example, a parameter value like
"${WorkingDir}/morepath" will be expanded to include the working directory.
The characters \" will be replaced by a literal quote (").  Properties that cannot be expanded will remain.
@param processor the CommandProcessor that has a list of named properties.
@param command the command that is being processed (may be used later for context sensitive values).
@param parameterValue the parameter value being expanded, containing literal substrings and optionally ${Property} properties.
@return the expanded string or null if the input string was null
*/
public static String expandParameterValue( CommandProcessor processor, Command command, String parameterValue ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".expandParameterValue";
    if ( (parameterValue == null) || (parameterValue.length() == 0) ||
    	(parameterValue.indexOf("${") < 0) ) { // }
    	// Nothing to expand:
    	// - null and empty string can't expand
    	// - if no ${Property} notation can't expand
        // - just return the input
        return parameterValue;
    }
    // First replace escaped characters.
    // Evaluate whether to write a general method for this - for now only handle // \" and \' replacement.
    parameterValue = parameterValue.replace("\\\"", "\"" );
    parameterValue = parameterValue.replace("\\'", "'" );
    // Else see if the parameter value can be expanded to replace ${} symbolic references with other values.
    // Search the parameter string for $ until all processor parameters have been resolved.
    int searchPos = 0; // Position in the "parameter_val" string to search for ${} references.
    int foundPos; // Position when leading ${ is found.
    int foundPosEnd; // Position when ending } is found.
    String propname = null; // Whether a property is found that matches the $ symbol.
    String delimStart = "${";
    String delimEnd = "}";
    while ( searchPos < parameterValue.length() ) {
        foundPos = parameterValue.indexOf(delimStart, searchPos);
        foundPosEnd = parameterValue.indexOf(delimEnd, (searchPos + delimStart.length()));
        if ( (foundPos < 0) || (foundPosEnd < 0)  ) {
            // No more properly formed $ property names, so return what have.
            return parameterValue;
        }
        // Else found the delimiter so continue with the replacement.
        //Message.printStatus ( 2, routine, "Found " + delimStart + " at position [" + foundPos + "]");
        // Get the name of the property.
        propname = parameterValue.substring((foundPos+2),foundPosEnd);
        // Try to get the property from the processor.
        // TODO SAM 2007-12-23 Evaluate whether to skip null.  For now show the unexpanded property name in result.
        Object propval = null;
        String propvalString = "";
        try {
            propval = processor.getPropContents ( propname );
            // The following should work for all representations as long as the toString() does not truncate.
            propvalString = "" + propval;
        }
        catch ( Exception e ) {
            // Keep the original literal value to alert user that property could not be expanded.
            propvalString = delimStart + propname + delimEnd;
        }
        if ( propval == null ) {
            // Keep the original literal value to alert user that property could not be expanded.
            propvalString = delimStart + propname + delimEnd;
        }
        // If here have a property.
        StringBuffer b = new StringBuffer();
        // Append the start of the string.
        if ( foundPos > 0 ) {
            b.append ( parameterValue.substring(0,foundPos) );
        }
        // Now append the value of the property.
        b.append ( propvalString );
        // Now append the end of the original string if anything is at the end.
        if ( parameterValue.length() > (foundPosEnd + 1) ) {
            b.append ( parameterValue.substring(foundPosEnd + 1) );
        }
        // Now reset the search position to finish evaluating whether to expand the string.
        parameterValue = b.toString();
        searchPos = foundPos + propvalString.length(); // Expanded so no need to consider start and end delimiters.
        if ( Message.isDebugOn ) {
            Message.printDebug( 1, routine, "Expanded parameter value is \"" + parameterValue +
                "\" searchpos is now " + searchPos + " in string \"" + parameterValue + "\"" );
        }
    }
    return parameterValue;
}

/**
 * Expand a template file into output file.
 * This code was taken from ExpandTemplateFile().runCommand() but is simpler for more basic use.
 * A template file uses FreeMarker markup.  This method passes the following to the FreeMarker template engine:
 * <ul>
 * <li>Any processor properties are passed using the property name and object contents</li>
 * <li>Each one-column table is passed with table name and list of object contents</li>
 * </ul>
 * @param processor the command processor that is processing commands
 * @param inputFile the name of the input file to process
 * @param outputFile the output file to create by expanding the template
 * @param useTables indicates if one-column tables should be passed to FreeMarker
 * @param status CommandStatus instance to accumulate command logging
 * @param commandTag command tag generated in command to indicate command position in command list
 * @param warningLevel the warning level for logging messages
 * @param warningCount starting warning count, will be added to and returned if any warnings
 * @return the warningCount, incremented with warnings generated in the method
 */
public static int expandTemplateFile ( CommandProcessor processor, String inputFile, String outputFile, boolean useTables,
	CommandStatus status, String commandTag, int warningLevel, int warningCount ) throws FileNotFoundException, IOException, Exception {
	String message, routine = "TSCommandProcessorUtil.expandTemplateFile";
	// Call the FreeMarker API.
	// TODO sam 2017-04-08 figure out whether can re-use a singleton.
	// Configuration is intended to be a shared singleton but templates can exist in many folders.
    Configuration config = new Configuration(new Version(2,3,0));
    // TODO SAM 2009-10-07 Not sure what configuration is needed for TSTool since most
    // templates will be located with command files and user data.
    //config.setSharedVariable("shared", "avoid global variables");
    // See comment below on why this is used.
    config.setSharedVariable("normalizeNewlines", new freemarker.template.utility.NormalizeNewlines());
    config.setTemplateLoader(new FileTemplateLoader(new File(".")));

    // In some applications, use config to load templates as it provides caching.
    //Template template = config.getTemplate("some-template.ftl");

    // Manipulate the template file into an in-memory string so it can be manipulated.
    StringBuffer b = new StringBuffer();
    // Prepend any extra FreeMarker content that should be handled transparently.
    // "normalizeNewlines" is used to ensure that output has line breaks consistent with the operating system
    // (e.g., so that results can be edited in Notepad on Windows).
    // Setting the number_format to "computer" ensures that output of an integer such as 1234 does not include comma like 1,234.
    String nl = System.getProperty("line.separator");
    b.append("<#setting number_format=\"computer\">" + nl );
    b.append("<@normalizeNewlines>" + nl );
    List<String> templateLines = new ArrayList<>();
    if ( inputFile != null ) {
        templateLines = IOUtil.fileToStringList(inputFile);
    }
    b.append(StringUtil.toString(templateLines,nl));
    b.append(nl + "</@normalizeNewlines>" );
    Template template = null;
    boolean error = false;
    try {
        template = new Template("template", new StringReader(b.toString()), config);
    }
    catch ( Exception e1 ) {
        message = "Freemarker error expanding command template file \"" + inputFile +
            "\" + (" + e1 + ") template text (with internal inserts at ends) =" + nl +
            formatTemplateForWarning(templateLines,nl);
        Message.printWarning ( warningLevel,
        MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
        Message.printWarning ( 3, routine, e1 );
        status.addToLog(CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check template file syntax for Freemarker markup errors."));
        error = true;
    }
    if ( !error ) {
        // Create a model.
        Map<String,Object> model = new HashMap<String,Object>();
        TSCommandProcessor tsprocessor = (TSCommandProcessor)processor;
        if ( processor instanceof TSCommandProcessor ) {
            // Add properties from the processor.
            Collection<String> propertyNames = tsprocessor.getPropertyNameList(true,true);
            for ( String propertyName : propertyNames ) {
                model.put(propertyName, tsprocessor.getPropContents(propertyName) );
            }
            // Add single column tables from the processor, using the table ID as the object key.
            if ( useTables ) {
                @SuppressWarnings("unchecked")
				List<DataTable> tables = (List<DataTable>)tsprocessor.getPropContents ( "TableResultsList" );
                Object tableVal;
                for ( DataTable table: tables ) {
                    if ( table.getNumberOfFields() == 1 ) {
                        // One-column table so add as a hash (list) property in the data model.
                        int numRecords = table.getNumberOfRecords();
                        SimpleSequence list = new SimpleSequence();
                        for ( int irec = 0; irec < numRecords; irec++ ) {
                            // Check for null because this fouls up the template.
                            tableVal = table.getFieldValue(irec, 0);
                            if ( tableVal == null ) {
                                tableVal = "";
                            }
                            list.add ( tableVal );
                        }
                        if ( Message.isDebugOn ) {
                            Message.printStatus(2, routine, "Passing 1-column table \"" + table.getTableID() +
                                "\" (" + numRecords + " rows) to template model.");
                        }
                        model.put(table.getTableID(), list );
                    }
                }
            }
        }
        if ( outputFile != null ) {
            // Expand the template to the output file.
            FileOutputStream fos = new FileOutputStream( outputFile );
            PrintWriter out = new PrintWriter ( fos );
            try {
                template.process (model, out);
            }
            catch ( Exception e1 ) {
                message = "Freemarker error expanding command template file \"" + inputFile +
                    "\" + (" + e1 + ") template text (with internal inserts at ends) =\n" +
                    formatTemplateForWarning(templateLines,nl);
                Message.printWarning ( warningLevel,
                MessageUtil.formatMessageTag(commandTag, ++warningCount),routine, message );
                Message.printWarning ( 3, routine, e1 );
                status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check template file syntax for Freemarker markup errors."));
            }
            finally {
                out.close();
            }
        }
    }
    return warningCount;
}

/**
Format the template for a warning message.  Add line numbers before.
@param templateLines template file as a list of String
@param nl newline character
*/
private static StringBuffer formatTemplateForWarning ( List<String> templateLines, String nl ) {
    StringBuffer templateFormatted = new StringBuffer();
    new StringBuffer();
    int lineNumber = 1;
    // Don't use space after number because HTML viewer may split and make harder to read.
    templateFormatted.append ( StringUtil.formatString(lineNumber++,"%d") + ":<@normalizeNewlines>" + nl );
    for ( String line : templateLines ) {
        templateFormatted.append ( StringUtil.formatString(lineNumber++,"%d") + ":" + line + nl );
    }
    templateFormatted.append ( StringUtil.formatString(lineNumber,"%d") + ":</@normalizeNewlines>" + nl );
    return templateFormatted;
}

/**
Expand a string using:
<ol>
<li> time series processor ${Property} strings</li>
<li> time series ensemble ${tsensemble:Property} strings</li>
</ol>
If a property string is not found, it will remain without being replaced.
@param processor The processor that is being used, if a ${property} needs to be expanded (if passed as null,
the processor property won't be expanded)
@param ensemble Time series ensemble to be used for metadata string.
@param s String to expand, which includes format specifiers and literal strings.
@param status CommandStatus to add messages to if problems occur, or null to ignore.
@param commandPhase command phase (for logging), can be null to ignore logging.
*/
public static String expandTimeSeriesEnsembleMetadataString ( CommandProcessor processor, TSEnsemble ensemble, String s,
    CommandStatus status, CommandPhaseType commandPhase ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".expandTimeSeriesEnsembleMetadataString";
    if ( s == null ) {
        return "";
    }
    //Message.printStatus(2, routine, "After formatLegend(), string is \"" + s2 + "\"" );
    // Now replace ${tsensemble:Property} and ${Property} strings with properties from the processor.
    // Put the most specific first so it is matched first.
    String [] startStrings = { "${tsensemble:", "${" };
    int [] startStringsLength = { 13, 2 };
    String [] endStrings = { "}", "}" };
    boolean isTsProp = false;
    Object propO;
    // Loop through and expand the string, first by expanding the time series properties, which have a more specific
    // ${tsensemble: starting pattern and then the processor properties starting with ${.
    for ( int ipat = 0; ipat < startStrings.length; ipat++ ) {
        int start = 0; // Start at the beginning of the string.
        int pos2 = 0;
        isTsProp = false;
        if ( ipat == 0 ) {
            // Time series property corresponding to startStrings[0] for loop below.
            // The fundamental logic is the same but getting the property is different whether from TS or processor.
            isTsProp = true;
        }
        while ( pos2 < s.length() ) {
            int pos1 = StringUtil.indexOfIgnoreCase(s, startStrings[ipat], start );
            if ( pos1 >= 0 ) {
                // Find the end of the property.
                pos2 = s.indexOf( endStrings[ipat], pos1 );
                if ( pos2 > 0 ) {
                    // Get the property.
                    String propname = s.substring(pos1+startStringsLength[ipat],pos2);
                    //Message.printStatus(2, routine, "Property=\"" + propname + "\" isTSProp=" + isTsProp + " pos1=" + pos1 + " pos2=" + pos2 );
                    // By convention if the property is not found, keep the original string so can troubleshoot property issues
                    String propvalString = s.substring(pos1,(pos2 + 1));
                    if ( isTsProp ) {
                        // Get the property out of the time series.
                        propO = ensemble.getProperty(propname);
                        if ( propO == null ) {
                            if ( status != null ) {
                                String message = "Time series ensemble \"" + ensemble.getEnsembleID() + "\" property=\"" +
                                propname + "\" has a null value.";
                                Message.printWarning ( 3,routine, message );
                                status.addToLog ( commandPhase,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Verify that the property is set for the time series ensemble." ) );
                            }
                        }
                        else {
                            // This handles conversion of integers to strings.
                            propvalString = "" + propO;
                        }
                    }
                    else if ( processor != null ) {
                        // Not a time series property so this is a processor property.
                        // Get the property from the processor properties.
                        PropList request_params = new PropList ( "" );
                        request_params.set ( "PropertyName", propname );
                        CommandProcessorRequestResultsBean bean = null;
                        boolean processorError = false;
                        try {
                            bean = processor.processRequest( "GetProperty", request_params);
                        }
                        /* TODO SAM 2015-07-05 Need to evaluate whether error should be absorbed and ${property} remain unexpanded, as javadoc'ed.
                        catch ( UnrecognizedRequestException e ) {
                        	// Property is not set - OK
                        	processorError = true;
                        }
                        */
                        catch ( Exception e ) {
                        	// Unexpected exception.
                            if ( status != null ) {
                                String message = "Error requesting GetProperty(Property=\"" + propname + "\") from processor.";
                                Message.printWarning ( 3,routine, message );
                                status.addToLog ( commandPhase,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
                            }
                            processorError = true;
                        }
                        if ( !processorError ) {
                            if ( bean == null ) {
                                // Not an exception but the property was not found in the processor.
                                if ( status != null ) {
                                    String message = "Unable to find property from processor using GetProperty(Property=\"" + propname + "\").";
                                    Message.printWarning ( 3,routine, message );
                                    status.addToLog ( commandPhase,
                                        new CommandLogRecord(CommandStatusType.FAILURE,
                                            message, "Verify that the property name is valid - must match case." ) );
                                }
                            }
                            else {
                                // Have a property, but still need to check for null value.
                                // TODO SAM 2013-09-09 should this be represented as "null" in output?
                                PropList bean_PropList = bean.getResultsPropList();
                                Object o_PropertyValue = bean_PropList.getContents ( "PropertyValue" );
                                if ( o_PropertyValue == null ) {
                                    if ( status != null ) {
                                        String message = "Null PropertyValue returned from processor for GetProperty(PropertyName=\"" + propname + "\").";
                                        Message.printWarning ( 3, routine, message );
                                        status.addToLog ( commandPhase,
                                            new CommandLogRecord(CommandStatusType.FAILURE, message,
                                                "Verify that the property name is valid - must match case." ) );
                                    }
                                }
                                else {
                                    // This handles conversion of integers and dates to strings.
                                    propvalString = "" + o_PropertyValue;
                                }
                            }
                        }
                    }
                    // Replace the string and continue to evaluate s2.
                    s = s.substring ( 0, pos1 ) + propvalString + s.substring (pos2 + 1);
                    // Next search will be at the end of the expanded string (end delimiter will be skipped in any case).
                    start = pos1 + propvalString.length();
                }
                else {
                    // No closing character so leave the property string as is and march on.
                    start = pos1 + startStringsLength[ipat];
                    if ( start > s.length() ) {
                        break;
                    }
                }
            }
            else {
                // No more ${} property strings so done processing properties.
                // If checking time series properties will then check global properties in next loop.
                break;
            }
        }
    }
    return s;
}

/**
Expand a string using:
<ol>
<li> time series % formatting strings using TS.formatLegend()</li>
<li> time series processor ${Property} strings</li>
<li> time series  ${ts:Property} strings</li>
</ol>
If a property string is not found, it will remain without being replaced.
@param processor The processor that is being used, if a ${property} needs to be expanded (if passed as null,
the processor property won't be expanded)
@param ts Time series to be used for metadata string.
@param s String to expand, which includes format specifiers and literal strings.
@param status CommandStatus to add messages to if problems occur, or null to ignore.
@param commandPhase command phase (for logging), can be null to ignore logging.
*/
public static String expandTimeSeriesMetadataString ( CommandProcessor processor, TS ts, String s,
    CommandStatus status, CommandPhaseType commandPhase ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".expandTimeSeriesMetadataString";
    if ( s == null ) {
        return "";
    }
    // First expand using the % characters.
    String s2 = ts.formatLegend ( s );
    // TODO SAM 2014-04-05 Remove the ${ts:Property} handling from this method since it is now in TS.formatLegend().
    //Message.printStatus(2, routine, "After formatLegend(), string is \"" + s2 + "\"" );
    // Now replace ${ts:Property} and ${Property} strings with properties from the processor.
    // Put the most specific first so it is matched first.
    String [] startStrings = { "${ts:", "${" };
    int [] startStringsLength = { 5, 2 };
    String [] endStrings = { "}", "}" };
    boolean isTsProp = false;
    Object propO;
    // Loop through and expand the string, first by expanding the time series properties, which have a more specific
    // ${ts: starting pattern and then the processor properties starting with ${.
    for ( int ipat = 0; ipat < startStrings.length; ipat++ ) {
        int start = 0; // Start at the beginning of the string
        int pos2 = 0;
        isTsProp = false;
        if ( ipat == 0 ) {
            // Time series property corresponding to startStrings[0] for loop below.
            // The fundamental logic is the same but getting the property is different whether from TS or processor.
            isTsProp = true;
        }
        while ( pos2 < s2.length() ) {
            int pos1 = StringUtil.indexOfIgnoreCase(s2, startStrings[ipat], start );
            if ( pos1 >= 0 ) {
                // Find the end of the property.
                pos2 = s2.indexOf( endStrings[ipat], pos1 );
                if ( pos2 > 0 ) {
                    // Get the property.
                    String propname = s2.substring(pos1+startStringsLength[ipat],pos2);
                    //Message.printStatus(2, routine, "Property=\"" + propname + "\" isTSProp=" + isTsProp + " pos1=" + pos1 + " pos2=" + pos2 );
                    // By convention if the property is not found, keep the original string so can troubleshoot property issues.
                    String propvalString = s2.substring(pos1,(pos2 + 1));
                    if ( isTsProp ) {
                        // Get the property out of the time series.
                        propO = ts.getProperty(propname);
                        if ( propO == null ) {
                            if ( status != null ) {
                                String message = "Time series \"" + ts.getIdentifierString() + "\" property=\"" +
                                propname + "\" (case-specific) has a null value.";
                                Message.printWarning ( 3,routine, message );
                                status.addToLog ( commandPhase,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Verify that the property is set for the time series." ) );
                            }
                        }
                        else {
                            // This handles conversion of objects to strings.
                            propvalString = "" + propO;
                        }
                    }
                    else if ( processor != null ) {
                        // Not a time series property so this is a processor property.
                        // Get the property from the processor properties.
                        PropList request_params = new PropList ( "" );
                        request_params.set ( "PropertyName", propname );
                        CommandProcessorRequestResultsBean bean = null;
                        boolean processorError = false;
                        try {
                            bean = processor.processRequest( "GetProperty", request_params);
                        }
                        /* TODO SAM 2015-07-05 Need to evaluate whether error should be absorbed and ${property} remain unexpanded, as javadoc'ed.
                        catch ( UnrecognizedRequestException e ) {
                        	// Property is not set - OK.
                        	processorError = true;
                        }
                        */
                        catch ( Exception e ) {
                        	// Unexpected exception.
                            if ( status != null ) {
                                String message = "Error requesting GetProperty(Property=\"" + propname + "\") from processor.";
                                Message.printWarning ( 3,routine, message );
                                status.addToLog ( commandPhase,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Report the problem to software support." ) );
                            }
                            processorError = true;
                        }
                        if ( !processorError ) {
                            if ( bean == null ) {
                                // Not an exception but the property was not found in the processor.
                                if ( status != null ) {
                                    String message = "Unable to find property from processor using GetProperty(Property=\"" + propname + "\").";
                                    Message.printWarning ( 3,routine, message );
                                    status.addToLog ( commandPhase,
                                        new CommandLogRecord(CommandStatusType.FAILURE,
                                            message, "Verify that the property name is valid - must match case." ) );
                                }
                            }
                            else {
                                // Have a property, but still need to check for null value.
                                // Show the value as the property name in output.
                                PropList bean_PropList = bean.getResultsPropList();
                                Object o_PropertyValue = bean_PropList.getContents ( "PropertyValue" );
                                if ( o_PropertyValue == null ) {
                                    if ( status != null ) {
                                        String message = "Null PropertyValue returned from processor for GetProperty(PropertyName=\"" + propname + "\").";
                                        Message.printWarning ( 3, routine, message );
                                        status.addToLog ( commandPhase,
                                            new CommandLogRecord(CommandStatusType.FAILURE, message,
                                                "Verify that the property name is valid - must match case." ) );
                                    }
                                }
                                else {
                                    // This handles conversion of integers and dates to strings.
                                    propvalString = "" + o_PropertyValue;
                                }
                            }
                        }
                    }
                    // Replace the string and continue to evaluate s2.
                    s2 = s2.substring ( 0, pos1 ) + propvalString + s2.substring (pos2 + 1);
                    // Next search will be at the end of the expanded string (end delimiter will be skipped in any case).
                    start = pos1 + propvalString.length();
                }
                else {
                    // No closing character so leave the property string as is and march on.
                    start = pos1 + startStringsLength[ipat];
                    if ( start > s2.length() ) {
                        break;
                    }
                }
            }
            else {
                // No more ${} property strings so done processing properties.
                // If checking time series properties will then check global properties in next loop.
                break;
            }
        }
    }
    return s2;
}

/**
 * Get the annotation command parameter from a "#@annotation ..." command, or return null if not found.
 * The following case is matched:
 * <pre>
 * #@annotation ...
 * # @annotation ...
 * </pre>
 * The following is not matched because it is often used in comments for automated tests:
 * <pre>
 * # Some text #@annotation ...
 * </pre>
 * @param command Annotation command to parse
 * @param index position of the parameter 0 is the annotation and 1 is the first parameter
 * @return the parameter or null if not available (command string too short);
 * the parameter name is returned without the leading @
 */
public static String getAnnotationCommandParameter ( Command command, int index ) {
	// Trim leading spaces.
	String commandString = command.toString().trim();
	if ( !commandString.startsWith("#") ) {
		// Not in an annotation comment.
		return null;
	}
	int pos1 = commandString.indexOf("@");
	if ( pos1 < 0 ) {
		// Not in an annotation comment.
		return null;
	}
	
	// Initialize the annotation string to null.
	String annotation = null;
	// Advance to the space after the annotation name.
	int pos2 = commandString.indexOf(" ",pos1);
	if ( pos2 < 0 ) {
		// No space after the annotation name so no parameters, something like "# @readOnly"
		if ( index == 0 ) {
			// Requested the annotation name so return it without the leading @.
			annotation = commandString.substring(pos1 + 1).trim();
		}
	}
	else if ( index == 0 ) {
		// Have annotation name and parameters but want the parameter name.
		annotation = commandString.substring(pos1 + 1,pos2).trim();
	}
	else {
		// Have a space after the annotation name so parse the remaining tokens.
		List<String> params = StringUtil.breakStringList(commandString.substring(pos2).trim(), " ", 0);
		if ( params.size() >= index ) {
			// Have the requested parameter.
			annotation = params.get(index - 1);
		}
	}
	
	// Check for '# ..... #@abc' and '# ..... # @abc', which are normal comments.
	if ( annotation != null ) {
		// If there is a '#' before the annotation comment in the original string, then it is a true comment.
		// First find the '#' immediately before the annotation.
		for ( int pos = pos1; pos >= 0; --pos ) {
			if ( commandString.charAt(pos) == '#' ) {
				// Found the first '#':
				// - search for a preceding '#':
				for ( int pos3 = (pos - 1); pos3 >= 0; --pos3 ) {
					if ( commandString.charAt(pos3) == '#' ) {
						// Found a preceding '#' so the command is a comment that is intended to NOT be an annotation.
						annotation = null;
						break;
					}
				}
			}
			if ( annotation == null ) {
				break;
			}
		}
	}
	
	// Return the final result, may be null.
	return annotation;
}

/**
 * Get commands that match an annotation like:
 * <pre>
 * #@annotation
 * </pre>
 * The following cases are ignored because they are typically used in documentation comments, including tests:
 * <pre>
 * # Some text @annotation
 * </pre>
 * @param processing command processor
 * @param annotation the annotation of interest (e.g., "author" or "@author")
 * @return a list of commands that match the requested annotation
 */
public static List<Command> getAnnotationCommands ( TSCommandProcessor processor, String annotation ) {
	List<Command> annotationCommands = new ArrayList<>();
	// Add the leading @ if not present.
	if ( !annotation.startsWith("@") ) {
		annotation = "@" + annotation;
	}
	boolean doAdd;
    for ( Command command : processor.getCommands() ) {
        String commandString = command.toString().trim();
        // The following might match @version for @versionDate.
        if ( isAnnotationCommand(command) ) {
       		// Make sure that the annotation is exactly matched (e.g., @versionDate) so have to check:
       		// - the following returns the annotation name without the leading @ but should not be null.
       		String annotationName = getAnnotationCommandParameter(command,0);
       		annotationName = "@" + annotationName;
       		if ( (annotationName != null) && annotationName.equalsIgnoreCase(annotation) ) {
       			// Have a matching annotation name.
        		// Make sure that # and @ only have spaces in between because the annotation string could
        		// be used in a comment, for example automated tests for the annotations.
        		doAdd = true;
        		int pos1 = commandString.indexOf("#");
        		int pos2 = commandString.indexOf(annotation);
        		if ( (pos2 - pos1) > 1 ) {
        			// Have characters between # and @ so check those characters.
        			for ( int i = (pos1 + 1); i < pos2; i++ ) {
        				if ( !Character.isWhitespace(commandString.charAt(i)) ) {
        					// Not an annotation. Assume it is a normal comment, for example an automated test explaining the annotation.
        					doAdd = false;
        					break;
        				}
        			}
        		}
        		if ( doAdd ) {
        			annotationCommands.add(command);
        		}
       		}
        }
    }
    return annotationCommands;
}

/**
Get the commands before the indicated index position.
Only the requested commands are returned.
Use this, for example, to get the SetWorkingDir() commands above the insert position for a readXXX() command,
so the working directory can be defined and used in the editor dialog.
@return List of commands before the index that match the commands in the neededCommandsList.
This will always return a non-null list, even if no commands are found.
@param index The index in the command list before which to search for other commands.
@param processor A TSCommandProcessor with commands to search.
@param neededCommandsList List of commands (as String) that need to be processed
(e.g., "SetWorkingDir").  Only the main command name should be defined.
@param last_only if true, only the last item above the insert point is returned.
If false, all matching commands above the point are returned in the order from top to bottom.
*/
public static List<Command> getCommandsBeforeIndex ( int index, TSCommandProcessor processor,
	List<String> neededCommandsList, boolean last_only ) {
	// Now search backwards matching commands for each of the requested commands.
	int size = 0;
	if ( neededCommandsList != null ) {
		size = neededCommandsList.size();
	}
	String needed_command_string;
	List<Command> found_commands = new ArrayList<>();
	// Get the commands from the processor.
	List<Command> commands = processor.getCommands();
	Command command;
	// Now loop up through the command list.
	for ( int ic = (index - 1); ic >= 0; ic-- ) {
		command = commands.get(ic);
		for ( int i = 0; i < size; i++ ) {
			needed_command_string = neededCommandsList.get(i);
			//((String)_command_List.getItem(ic)).trim() );
			if ( needed_command_string.regionMatches(true,0,command.toString().trim(),0,
				needed_command_string.length() ) ) {
				found_commands.add ( command );
				if ( last_only ) {
					// Don't need to search any more.
					break;
				}
			}
		}
	}
	// Reverse the commands so they are listed in the order of the list.
	size = found_commands.size();
	if ( size <= 1 ) {
		return found_commands;
	}
	List<Command> found_commands_sorted = new ArrayList<>(size);
	for ( int i = size - 1; i >= 0; i-- ) {
		found_commands_sorted.add ( found_commands.get(i));
	}
	return found_commands_sorted;
}

/**
Get the commands above an index position.
@param processor The processor that is managing commands.
@param pos Index (0+) before which to get commands.
The command at the indicated position is NOT included in the search.
*/
private static List<Command> getCommandsBeforeIndex ( TSCommandProcessor processor, int pos ) {
	List<Command> commands = new ArrayList<>();
	int size = processor.size();
	if ( pos > size ) {
		pos = size;
	}
	for ( int i = 0; i < pos; i++ ) {
		commands.add ( processor.get(i));
	}
	return commands;
}

/**
Get the maximum command status severity for the processor.
This is used, for example, when determining an overall status for a runCommands() command.
@param processor Command processor to check status.
@return most severe command status from all commands in a processor.
*/
public static CommandStatusType getCommandStatusMaxSeverity ( TSCommandProcessor processor ) {
	int size = processor.size();
	Command command;
	CommandStatusType most_severe = CommandStatusType.UNKNOWN;
	CommandStatusType from_command;
	for ( int i = 0; i < size; i++ ) {
		command = processor.get(i);
		if ( command instanceof CommandStatusProvider ) {
			from_command = CommandStatusUtil.getHighestSeverity((CommandStatusProvider)command);
			//Message.printStatus (2,"", "Highest severity \"" + command.toString() + "\"=" + from_command.toString());
			most_severe = CommandStatusType.maxSeverity(most_severe,from_command);
		}
	}
	return most_severe;
}

/**
Determine whether commands should create output by checking the CreateOutput parameter.
This is a processor level property.  If there is a problem, return true (create output).
@param processor the CommandProcessor to use to get data.
@return true if output should be created when processing commands, false if not.
*/
public static boolean getCreateOutput ( CommandProcessor processor ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getCreateOutput";
	try {
		Object o = processor.getPropContents ( "CreateOutput" );
		if ( o != null ) {
			return ((Boolean)o).booleanValue();
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		String message = "Error requesting CreateOutput from processor - will create output.";
		Message.printWarning(3, routine, message );
		Message.printWarning(3, routine, e );
	}
	return true;
}

/**
Return the datastore names for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of names to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which datastore names are needed.
@param includeDatabases indicate whether to include DatabaseDataStore names.
@param includeWebServices indicate whether to include WebServiceDataStore names.
@return a list of String containing the datastore names, or an empty list.
*/
public static List<String> getDataStoreNamesFromCommandsBeforeCommand(
	TSCommandProcessor processor, Command command, boolean includeDatabases, boolean includeWebServices ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDataStoreNamesFromCommandsBeforeCommand";
	// Get the position of the command in the list.
	int pos = processor.indexOf(command);
	if ( Message.isDebugOn ) {
	    Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
	}
	if ( pos < 0 ) {
		// Just return an empty list.
		return new ArrayList<>();
	}
    // Find the commands above the position.
	List<Command> commands = getCommandsBeforeIndex ( processor, pos );
	// Get the datastore names from the commands.
	return getDataStoreNamesFromCommands ( commands, includeDatabases, includeWebServices, true );
}

/**
Get a list of datastore names from a list of commands.
These strings are suitable for drop-down lists, etc.
Datastore names are determined as follows:
<ol>
<li>    Commands that implement ObjectListProvider have their getObjectList(DataStore) method called.
        The datastore names from the datastore list are returned.</li>
</ol>
@param commands commands to search, in order of first command to process to last.
@param includeDatabases indicate whether to include DatabaseDataStore names.
@param includeWebServices indicate whether to include WebServiceDataStore names.
@param sort Should output be sorted by names.
@return list of datastore names or an empty non-null list if nothing found.
*/
protected static List<String> getDataStoreNamesFromCommands ( List<Command> commands,
	boolean includeDatabases, boolean includeWebServices, boolean sort ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDataStoreNamesFromCommands";
	if ( commands == null ) {
		return new ArrayList<>();
	}
	List<String> namesFromCommands = new ArrayList<>(); // The DataStore names.
	List<DataStore> dsFromCommands = new ArrayList<>(); // The DataStores.
	int size = commands.size();
	String commandString = null;
	boolean in_comment = false;
	Object command_o = null; // Command as object.
	for ( int i = 0; i < size; i++ ) {
		command_o = commands.get(i);
		if ( command_o instanceof Command ) {
			commandString = command_o.toString().trim();
		}
		if ( (commandString == null) || commandString.startsWith("#") || (commandString.length() == 0) ) {
			// Make sure comments are ignored.
			continue;
		}
		if ( commandString.startsWith("/*") ) {
			in_comment = true;
			continue;
		}
		else if ( commandString.startsWith("*/") ) {
			in_comment = false;
			continue;
		}
		else if ( in_comment ) {
			continue;
		}
        if ( (command_o != null) && (command_o instanceof ObjectListProvider) ) {
           	if ( Message.isDebugOn ) {
           		Message.printDebug(1, routine, "Command is an ObjectListProvider: " + command_o );
           	}
            // Try to get the list of identifiers using the interface method.
            // TODO SAM 2007-12-07 Evaluate the automatic use of the alias (takes priority over TSID) - probably good.
        	Object o = ((ObjectListProvider)command_o).getObjectList ( DataStore.class );
            List<DataStore> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<DataStore> list0 = (List<DataStore>)o;
            	list = list0;
            }
            if ( list == null ) {
            	if ( Message.isDebugOn ) {
            		Message.printDebug(1, routine, "  ObjectListProvider returned no objects." );
            	}
            }
            else {
                int dssize = list.size();
            	if ( Message.isDebugOn ) {
            		Message.printDebug(1, routine, "  ObjectListProvider returned " + dssize + " objects." );
            	}
                DataStore ds;
                for ( int ids = 0; ids < dssize; ids++ ) {
                    ds = list.get(ids);
                    if ( ds == null ) {
                    	// This should not happen and is symptomatic of a command not fully handling a datastore in discovery mode,
                    	// more of an issue with ${property} use in parameters.
                    	// Log a message so the issue can be tracked down.
                    	Message.printWarning(3, routine,
                    		"Null datastore in discovery mode - need to check code for command to improve handling: " + commandString);
                    }
                    else {
                    	boolean doInclude = false;
                    	if ( !includeDatabases && !includeWebServices ) {
                    		// Default is to always include, otherwise would never return anything.
                    		doInclude = true;
                    	}
                    	if ( includeDatabases ) {
                    		if ( ds instanceof DatabaseDataStore ) {
                    			doInclude = true;
                    		}
                    		else {
                    			if ( Message.isDebugOn ) {
            		       			Message.printDebug(1, routine, "  Including databases but datastore \"" +
            		       				ds.getName() + "\" is not a database so ignore." );
            	       			}
                    		}
                    	}
                    	if ( includeWebServices ) {
                    		if ( ds instanceof WebServiceDataStore ) {
                    			doInclude = true;
                    		}
                    		else {
                    			if ( Message.isDebugOn ) {
            		       			Message.printDebug(1, routine, "  Including web services but datastore \"" +
            		       				ds.getName() + "\" is not a web service so ignore." );
            	       			}
                    		}
                    	}
                    	// If a match occurred above, include the name in the returned list.
                    	if ( doInclude ) {
                    		// Use the name for the returned identifier.
                    		namesFromCommands.add ( ds.getName() );
                    		dsFromCommands.add( ds );
                    	}
                    }
                }
            }
        }
	}
	return namesFromCommands;
}

/**
 * Get a list of database datastores for command editors that need datastore instances.
 * This includes datastores configured at startup in the processor and datastores from discovery.
 * @param tsProcessor TSCommandProcessor to use to retrieve the datastores
 * @param command the command being edited
 * @return the list of DatabaseDataStore that can be used in editors.
 */
public static List<DatabaseDataStore> getDatabaseDataStoresForEditors ( TSCommandProcessor tsProcessor, Command command ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDatabaseDataStoresForEditors";
	// Get the list of database datastores, to list as choices and allow listing tables and procedures if necessary.
    // Datastores could be those active at startup and opened with OpenDataStore commands:
    // - since runtime datastores may not known when editing the command, merge the two lists
    // - any datastores opened with OpenDataStore commands could be closed later with CloseDataStore,
    //   in which case the corresponding discovery datastore can be used for editing
    List<DataStore> dataStoreList0 = tsProcessor.getDataStoresByType( DatabaseDataStore.class );
    List<DatabaseDataStore> dataStoreList = new ArrayList<>();
    for ( DataStore dataStore : dataStoreList0 ) {
		if ( DatabaseDataStore.class.isInstance(dataStore) ) {
			dataStoreList.add((DatabaseDataStore)dataStore);
		}
    }
	if ( Message.isDebugOn ) {
		Message.printDebug(1,routine,"There are " + dataStoreList.size() + " matching datastores that are active.");
	}
    // Get the datastores from discovery mode:
    // - only get the database datastores, not web services
	// - these are discovery datastores from OpenDataStore and NewSQLiteDatabase commands
	List<DataStore> dataStoreList2 =
        TSCommandProcessorUtil.getDataStoresFromCommandsBeforeCommand( tsProcessor, command, true, false);
	if ( Message.isDebugOn ) {
		Message.printDebug(1,routine,"There are " + dataStoreList2.size() + " datastores from discovery before checking for matches.");
	}
    // Merge the lists:
	// - sort alphabetically in the editor
	for ( DataStore dataStore2 : dataStoreList2 ) {
		boolean found = false;
		if ( DatabaseDataStore.class.isInstance(dataStore2) ) {
			// Is a DatabaseDataStore so can continue processing below.
			if ( Message.isDebugOn ) {
				Message.printDebug(1,routine,"  Datastore \"" + dataStore2.getName() + "\" is a DatabaseDataStore.");
			}
		}
		else {
			// Is not a DatabaseDataStore so skip processing below.
			if ( Message.isDebugOn ) {
				Message.printDebug(1,routine,"  Datastore \"" + dataStore2.getName() + "\" is not a DatabaseDataStore - not checking.");
			}
			continue;
		}
		// Loop through the the list from active datastores and add if not already in the list.
		for ( DataStore dataStore : dataStoreList ) {
			if ( Message.isDebugOn ) {
				Message.printDebug(1,routine,"Comparing discovery datastore \"" + dataStore2.getName() +
					"\" with active datastore \"" + dataStore.getName() + "\"");
			}
			if ( dataStore2.getName().equals(dataStore.getName()) ) {
				if ( Message.isDebugOn ) {
					Message.printDebug(1,routine,"  Datastore \"" + dataStore2.getName() +
						"\" exists in active datastores list - don't add again.");
				}
				found = true;
				break;
			}
		}
		if ( !found ) {
			if ( Message.isDebugOn ) {
				Message.printDebug(1,routine,"  Discovery datastore \"" + dataStore2.getName() +
					"\" does not exist in active datastores list - adding to list, status=" + dataStore2.getStatus() );
			}
			dataStoreList.add((DatabaseDataStore)dataStore2);
		}
	}
	return dataStoreList;
}

/**
 * Get a list of datastores for command editors that need datastore instances.
 * This includes datastores configured at startup in the processor and datastores from discovery.
 * @param tsProcessor TSCommandProcessor to use to retrieve the datastores
 * @param command the command being edited
 * @return the list of DatabaseDataStore that can be used in editors.
 */
public static List<DataStore> getDataStoresForEditors ( TSCommandProcessor tsProcessor, Command command ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDataStoresForEditors";
	// Get the list of datastores, to list as choices in command editors.
    // Datastores could be those active at startup and opened with OpenDataStore commands:
    // - since runtime datastores may not known when editing the command, merge the two lists
    // - any datastores opened with OpenDataStore commands could be closed later with CloseDataStore,
    //   in which case the corresponding discovery datastore can be used for editing
	boolean debug = true;
	//boolean debug = Message.isDebugOn;
	// Get all the datastores:
	// - can't call getDataStoresByType because it ignores the base class so that it can check for specific types
    List<DataStore> dataStoreList0 = tsProcessor.getDataStores();
    List<DataStore> dataStoreList = new ArrayList<>();
    for ( DataStore dataStore : dataStoreList0 ) {
		dataStoreList.add(dataStore);
    }
	if ( debug ) {
		Message.printDebug(1,routine,"There are " + dataStoreList.size() + " matching datastores that are active.");
	}
    // Get the datastores from discovery mode:
    // - get all datastores
	// - these are discovery datastores from OpenDataStore and NewSQLiteDatabase commands
	List<DataStore> dataStoreList2 =
        TSCommandProcessorUtil.getDataStoresFromCommandsBeforeCommand( tsProcessor, command, true, false);
	if ( debug ) {
		Message.printDebug(1,routine,"There are " + dataStoreList2.size() + " datastores from discovery before checking for matches.");
	}
    // Merge the lists:
	// - sort alphabetically in the editor
	for ( DataStore dataStore2 : dataStoreList2 ) {
		boolean found = false;
		// Loop through the the list from active datastores and add if not already in the list.
		for ( DataStore dataStore : dataStoreList ) {
			if ( debug ) {
				Message.printDebug(1,routine,"Comparing discovery datastore \"" + dataStore2.getName() +
					"\" with active datastore \"" + dataStore.getName() + "\"");
			}
			if ( dataStore2.getName().equals(dataStore.getName()) ) {
				if ( debug ) {
					Message.printDebug(1,routine,"  Datastore \"" + dataStore2.getName() +
						"\" exists in active datastores list - don't add again.");
				}
				found = true;
				break;
			}
		}
		if ( !found ) {
			if ( debug ) {
				Message.printDebug(1,routine,"  Discovery datastore \"" + dataStore2.getName() +
					"\" does not exist in active datastores list - adding to list, status=" + dataStore2.getStatus() );
			}
			dataStoreList.add(dataStore2);
		}
	}
	return dataStoreList;
}

/**
Get a list of datastores from a list of commands.
The datastores are suitable for editors that need datastore metadata.
Datastores are determined as follows:
<ol>
<li>    Commands that implement ObjectListProvider have their getObjectList(DataStore) method called.
        The datastores from the datastore list are returned.</li>
</ol>
@param commands commands to search, in order of first command to process to last.
@param includeDatabases indicate whether to include DatabaseDataStore instances.
@param includeWebServices indicate whether to include WebServiceDataStore instances.
@param sort Should output be sorted by names.
@return list of datastores or an empty list if nothing found.
*/
public static List<DataStore> getDataStoresFromCommands ( List<Command> commands,
	boolean includeDatabases, boolean includeWebServices, boolean sort ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDataStoreNamesFromCommands";
	if ( commands == null ) {
		return new ArrayList<>();
	}
	List<DataStore> dsFromCommands = new ArrayList<>(); // The DataStores.
	int size = commands.size();
	String commandString = null;
	boolean in_comment = false;
	Object command_o = null; // Command as object.
	for ( int i = 0; i < size; i++ ) {
		command_o = commands.get(i);
		if ( command_o instanceof Command ) {
			commandString = command_o.toString().trim();
		}
		if ( (commandString == null) || commandString.startsWith("#") || (commandString.length() == 0) ) {
			// Make sure comments are ignored.
			continue;
		}
		if ( commandString.startsWith("/*") ) {
			in_comment = true;
			continue;
		}
		else if ( commandString.startsWith("*/") ) {
			in_comment = false;
			continue;
		}
		else if ( in_comment ) {
			continue;
		}
        if ( (command_o != null) && (command_o instanceof ObjectListProvider) ) {
           	if ( Message.isDebugOn ) {
           		Message.printDebug(1, routine, "Command is an ObjectListProvider: " + command_o );
           	}
            // Try to get the list of identifiers using the interface method.
            // TODO SAM 2007-12-07 Evaluate the automatic use of the alias (takes priority over TSID) - probably good.
        	Object o = ((ObjectListProvider)command_o).getObjectList ( DataStore.class );
            List<DataStore> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<DataStore> list0 = (List<DataStore>)o;
            	list = list0;
            }
            if ( list == null ) {
            	if ( Message.isDebugOn ) {
            		Message.printDebug(1, routine, "  ObjectListProvider returned no objects." );
            	}
            }
            else {
                int dssize = list.size();
            	if ( Message.isDebugOn ) {
            		Message.printDebug(1, routine, "  ObjectListProvider returned " + dssize + " objects." );
            	}
                DataStore ds;
                for ( int ids = 0; ids < dssize; ids++ ) {
                    ds = list.get(ids);
                    if ( ds == null ) {
                    	// This should not happen and is symptomatic of a command not fully handling a
                    	// datastore in discovery mode, more of an issue with ${property} use in parameters.
                    	// Log a message so the issue can be tracked down.
                    	Message.printWarning(3, routine,
                    		"Null datastore in discovery mode - need to check code for command to improve handling: " + commandString);
                    }
                    else {
                    	boolean doInclude = false;
                    	if ( !includeDatabases && !includeWebServices ) {
                    		// Default is to always include, otherwise would never return anything.
                    		doInclude = true;
                    	}
                    	if ( includeDatabases ) {
                    		if ( ds instanceof DatabaseDataStore ) {
                    			doInclude = true;
                    		}
                    		else {
                    			if ( Message.isDebugOn ) {
            		       			Message.printDebug(1, routine, "  Including databases but datastore \"" +
            		       				ds.getName() + "\" is not a database so ignore." );
            	       			}
                    		}
                    	}
                    	if ( includeWebServices ) {
                    		if ( ds instanceof WebServiceDataStore ) {
                    			doInclude = true;
                    		}
                    		else {
                    			if ( Message.isDebugOn ) {
            		       			Message.printDebug(1, routine, "  Including web services but datastore \"" +
            		       				ds.getName() + "\" is not a web service so ignore." );
            	       			}
                    		}
                    	}
                    	// If a match occurred above, include the name in the returned list.
                    	if ( doInclude ) {
                    		// Use the name for the returned identifier.
                    		dsFromCommands.add( ds );
                    	}
                    }
                }
            }
        }
	}
	return dsFromCommands;
}

/**
Return the datastores for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of names to editor dialogs and browse datastore metadata.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which datastore names are needed.
@param includeDatabases indicate whether to include DatabaseDataStore names.
@param includeWebServices indicate whether to include WebServiceDataStore names.
@return a list of String containing the datastore names, or an empty list.
*/
public static List<DataStore> getDataStoresFromCommandsBeforeCommand(
	TSCommandProcessor processor, Command command, boolean includeDatabases, boolean includeWebServices ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDataStoreNamesFromCommandsBeforeCommand";
	// Get the position of the command in the list.
	int pos = processor.indexOf(command);
	if ( Message.isDebugOn ) {
	    Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
	}
	if ( pos < 0 ) {
		// Just return an empty list.
		return new ArrayList<>();
	}
    // Find the commands above the position.
	List<Command> commands = getCommandsBeforeIndex ( processor, pos );
	// Get the datastore names from the commands.
	return getDataStoresFromCommands ( commands, includeDatabases, includeWebServices, true );
}

/**
Get a date/time property from the processor, recognizing normal date/time strings like "YYYY-MM-DD",
processor properties "${Property}", and special strings including:
<ol>
<li>	"OutputPeriod"</li>
</ol>
If an error occurs, the command log messages and status will be updated.
Additionally, if the parameter string is invalid an exception will be thrown
(calling code in command can then increment the command's warning count).
@param dtString date/time string to process.
@param parameterName name for parameter for messages.
@param processor command processor from which to retrieve the date.
@param status command status, to receive logging information.
@param int warningLevel level at which to log information.
@param commandTag string tag for logging.
@exception InvalidCommandParameterException if the parameter is not valid.
*/
public static DateTime getDateTime ( String dtString, String parameterName, CommandProcessor processor,
    CommandStatus status, int warningLevel, String commandTag )
throws InvalidCommandParameterException {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDateTime", message;
    DateTime dt = null;
    int logLevel = 3;
    int warningCount = 0; // Only has local scope and limited meaning.
    if ( (dtString == null) || dtString.equals("") ) {
        return null;
    }
    try {
        PropList request_params = new PropList ( "" );
        request_params.set ( "DateTime", dtString );
        CommandProcessorRequestResultsBean bean = null;
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting " + parameterName + " DateTime(DateTime=" + dtString + "\") from processor (" + e + ").";
            Message.printWarning(logLevel,
                MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report the problem to software support." ) );
            throw new InvalidCommandParameterException ( message );
        }

        // Requesting a "DateTime" will check processor properties matching the name.
        PropList bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
        	// Have to take special care for built-in properties that are allowed to be null.
        	// See the similar handling in TSEngine.getDateTime().
        	// Newer code will focus on ${Property} whereas legacy uses the version without ${}.
        	if ( dtString.equalsIgnoreCase("OutputStart") || dtString.equalsIgnoreCase("OutputEnd") ||
        		dtString.equalsIgnoreCase("${OutputStart}") || dtString.equalsIgnoreCase("${OutputEnd}") ||
        		dtString.equalsIgnoreCase("InputStart") || dtString.equalsIgnoreCase("InputEnd") ||
        		dtString.equalsIgnoreCase("${InputStart}") || dtString.equalsIgnoreCase("${InputEnd}") ) {
        		// OK to return null.
        		return null;
        	}
        	else {
	            message = "Null value for " + parameterName + " DateTime(DateTime=" + dtString + "\") returned from processor.";
	            Message.printWarning(logLevel,
	                MessageUtil.formatMessageTag( commandTag, ++warningCount),
	                routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
	                new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Specify a valid date/time string, or a recognized internal property such as ${OutputEnd}." ) );
	            throw new InvalidCommandParameterException ( message );
        	}
        }
        else {
            dt = (DateTime)prop_contents;
        }
    }
    catch ( Exception e ) {
        message = parameterName + " \"" + dtString + "\" is invalid.";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( commandTag, ++warningCount),
            routine, message );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid date/time string, or a recognized internal property such as ${OutputEnd}." ) );
        throw new InvalidCommandParameterException ( message );
    }
    return dt;
}

/**
Get a list of TSEnsemble (ensemble) from a list of commands.
These ensemble are suitable for passing to code run in discovery mode,
which itself produces new ensembles or time series with identifiers that are dynamically determined from the input time series.
Commands that implement ObjectListProvider have their getObjectList(TS) method called and
the returned time series are added to the list.
@param commands time series commands to search.
@param sort Should output time series be sorted by identifier - currently not enabled
@return list of time series or an empty non-null list if nothing found.
*/
protected static List<TSEnsemble> getDiscoveryEnsembleFromCommands ( List<Command> commands, boolean sort ) {
    if ( commands == null ) {
        return new ArrayList<>();
    }
    List<TSEnsemble> tsEnsembleList = new ArrayList<>();
    for ( Command command: commands ) {
        if ( (command != null) && (command instanceof ObjectListProvider) ) {
        	Object o = ((ObjectListProvider)command).getObjectList ( TSEnsemble.class );
        	List<TSEnsemble> list = null;
        	if ( o != null ) {
        		@SuppressWarnings("unchecked")
				List<TSEnsemble> list0 = (List<TSEnsemble>)o;
        		list = list0;
        	}
            if ( list != null ) {
                for ( TSEnsemble tsEnsemble : list ) {
                    if ( tsEnsemble != null ) {
                        tsEnsembleList.add( tsEnsemble );
                    }
                }
            }
        }
    }
    /*
    if ( sort ) {
        TSUtil.sort(tsEnsembleList);
    }
    */
    return tsEnsembleList;
}

/**
Return the time series ensemble (TSEnsemble) for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@return a list of time series ensemble, or an empty list.
*/
public static List<TSEnsemble> getDiscoveryEnsembleFromCommandsBeforeCommand(
    TSCommandProcessor processor, Command command ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDiscoveryEnsembleFromCommandsBeforeCommand";
    // Get the position of the command in the list.
    int pos = processor.indexOf(command);
    if ( Message.isDebugOn ) {
        Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
    }
    if ( pos < 0 ) {
        // Just return a blank list.
        return new Vector<>();
    }
    // Find the commands above the position.
    List<Command> commands = getCommandsBeforeIndex ( processor, pos );
    // Get the time series from the commands (sort on the identifiers).
    return getDiscoveryEnsembleFromCommands ( commands, true );
}

/**
Get a list of Prop (properties) from a list of commands.
These properties are suitable for passing to code run in discovery mode.
Commands that implement ObjectListProvider have their getObjectList(Prop) method
called and the returned time series are added to the list.
@param commands commands to search.
@param sort Should properties be sorted by property name - currently not enabled
@return list of properties an empty non-null list if nothing found.
*/
protected static List<Prop> getDiscoveryPropFromCommands ( List<Command> commands, boolean sort ) {
    if ( commands == null ) {
        return new ArrayList<>();
    }
    List<Prop> proplist = new ArrayList<>();
    for ( Command command: commands ) {
        if ( (command != null) && (command instanceof ObjectListProvider) ) {
        	//TODO sam 2017-03-17 figure out how to do generics but for now the old way works.
			ObjectListProvider objectListProvider = (ObjectListProvider)command;
        	Object o = objectListProvider.getObjectList ( Prop.class );
            List<Prop> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<Prop> list0 = (List<Prop>)o;
            	list = list0;
            }
            if ( list != null ) {
                int listsize = list.size();
                Prop prop;
                for ( int iprop = 0; iprop < listsize; iprop++ ) {
                    prop = list.get(iprop);
                    if ( prop != null ) {
                        proplist.add( prop );
                    }
                }
            }
        }
    }
    if ( sort ) {
    	Collections.sort(proplist);
    }
    return proplist;
}

/**
Return the properties for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of property names to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which properties are needed.
@return a list of time series (containing only information populated during discovery), or an empty list.
*/
public static List<Prop> getDiscoveryPropFromCommandsBeforeCommand( TSCommandProcessor processor, Command command ) {
    String routine = "getDiscoveryPropFromCommandsBeforeCommand";
    // Get the position of the command in the list.
    int pos = processor.indexOf(command);
    if ( Message.isDebugOn ) {
        Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
    }
    if ( pos < 0 ) {
        // Just return a blank list.
        return new Vector<>();
    }
    // Find the commands above the position.
    List<Command> commands = getCommandsBeforeIndex ( processor, pos );
    // Get the time series from the commands (sort on the property names).
    List<Prop> availableProp = getDiscoveryPropFromCommands ( commands, true );
    return availableProp;
}

/**
Get a list of TS (time series) from a list of commands.
These time series are suitable for passing to code run in discovery mode,
which itself produces new time series with identifiers that are dynamically determined from the input time series.
Commands that implement ObjectListProvider have their getObjectList(TS) method
called and the returned time series are added to the list.
@param commands time series commands to search.
@param sort Should output time series be sorted by identifier - currently not enabled
@return list of time series or an empty non-null list if nothing found.
*/
protected static List<TS> getDiscoveryTSFromCommands ( List<Command> commands, boolean sort ) {
    if ( commands == null ) {
        return new ArrayList<>();
    }
    List<TS> tslist = new ArrayList<>();
    for ( Command command: commands ) {
        if ( (command != null) && (command instanceof ObjectListProvider) ) {
        	//TODO sam 2017-03-17 figure out how to do generics but for now the old way works.
			ObjectListProvider objectListProvider = (ObjectListProvider)command;
        	Object o = objectListProvider.getObjectList ( TS.class );
            List<TS> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<TS> list0 = (List<TS>)o;
            	list = list0;
            }
        	// Following is attempt at generics.
        	//@SuppressWarnings("unchecked")
			//ObjectListProvider<TS> objectListProvider = (ObjectListProvider<TS>)command;
        	//List<TS> list = objectListProvider.getObjectList ( TS.class );
            //List<TS> list = null;
            //if ( o != null ) {
            //	@SuppressWarnings("unchecked")
			//	List<TS> list0 = (List<TS>)o;
            //	list = list0;
            //}
            if ( list != null ) {
                int tssize = list.size();
                TS ts;
                for ( int its = 0; its < tssize; its++ ) {
                    ts = list.get(its);
                    if ( ts != null ) {
                        tslist.add( ts );
                    }
                }
            }
        }
        else if ( command.getCommandName().equalsIgnoreCase("Free") ) {
            // Need to remove matching time series identifiers that are in the list
            // (otherwise editing commands will show extra time series as of that point in the workflow,
        	// which will be confusing and may lead to errors, e.g., if consistent units are expected but the units are not consistent).
            // First get the matching time series for the Free() command parameters.
            PropList parameters = command.getCommandParameters();
            // TODO SAM 2011-04-04 Need to get ensembles above command.
            List<TSEnsemble> ensemblesFromCommands = new ArrayList<>();
            TimeSeriesToProcess tsToProcess = getTSMatchingTSListParameters(tslist, ensemblesFromCommands,
                parameters.getValue("TSList"), parameters.getValue("TSID"),
                parameters.getValue("TSPosition"), parameters.getValue("EnsembleID") );
            // Loop through the list of matching time series and remove identifiers at the matching positions
            // (the time series list and identifier lists should match in position).
            int [] pos = tsToProcess.getTimeSeriesPositions();
            // Loop backwards so that position values don't need to be adjusted.
            for ( int ipos = pos.length - 1; ipos >= 0; ipos--  ) {
                //Message.printStatus(2,"", "Removing time series " + pos[ipos] + ": " + tslist.get(pos[ipos]));
                tslist.remove(pos[ipos]);
            }
        }
    }
    /*
    if ( sort ) {
        TSUtil.sort(tslist);
    }
    */
    return tslist;
}

/**
Return the time series identifiers for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@param tsList string value matching TSListType, which controls how a list of time series is determined
@param tsid time series identifier (or alias) to be matched
@param tsPosition string of form "1,3,5-6" indicating time series positions to match (dangerous except for specific cases)
@param ensembleId time series ensemble identifier from which to match time series
@return a list of time series (containing only information populated during discovery), or an empty list.
*/
public static List<TS> getDiscoveryTSFromCommandsBeforeCommand( TSCommandProcessor processor, Command command,
    String tsList, String tsid, String tsPosition, String ensembleId ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getDiscoveryTSFromCommandsBeforeCommand";
    // Get the position of the command in the list.
    int pos = processor.indexOf(command);
    if ( Message.isDebugOn ) {
        Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
    }
    if ( pos < 0 ) {
        // Just return a blank list.
        return new Vector<>();
    }
    // Find the commands above the position.
    List<Command> commands = getCommandsBeforeIndex ( processor, pos );
    // Get the time series from the commands (sort on the identifiers).
    List<TS> availableTS = getDiscoveryTSFromCommands ( commands, true );
    // Get the ensembles from the commands (sort on the identifiers).
    List<TSEnsemble> availableEnsembles = getDiscoveryEnsembleFromCommands ( commands, true );
    // Now filter out the ones that match the TSList criteria.
    // This code is essentially the same as TSEngine.getTimeSeriesToProcess();
    // however, the TSEngine code works on instances in the processor
    // whereas the code below works on skeleton time series from the commands.
    TimeSeriesToProcess tsToProcess = getTSMatchingTSListParameters ( availableTS,
        availableEnsembles, tsList, tsid, tsPosition, ensembleId );
    return tsToProcess.getTimeSeriesList();
}

/**
Get a list of ensemble identifiers from a list of commands.
See documentation for fully loaded method.  The output list is not sorted.
@param commands Commands to search.
@return list of table identifiers or an empty non-null list if nothing found.
*/
private static List<String> getEnsembleIdentifiersFromCommands ( List<Command> commands ) {
    // Default behavior.
    return getEnsembleIdentifiersFromCommands ( commands, false );
}

/**
Get a list of ensemble identifiers from a list of commands.
The returned strings are suitable for drop-down lists, etc.  Ensemble identifiers are determined as follows:
Commands that implement ObjectListProvider have their getObjectList(TSEnsemble) method called.
The getEnsembleID() method on the TSEnsemble is then returned.
@param commands Commands to search.
@param sort Should output be sorted by identifier.
@return list of ensemble identifiers or an empty non-null list if nothing found.
*/
protected static List<String> getEnsembleIdentifiersFromCommands ( List<Command> commands, boolean sort ) {
    if ( commands == null ) {
        return new ArrayList<>();
    }
    List<String> v = new ArrayList<>(10);
    int size = commands.size();
    boolean in_comment = false;
    Command command = null;
    String command_string = null;
    for ( int i = 0; i < size; i++ ) {
        command = commands.get(i);
        command_string = command.toString();
        if ( command_string.startsWith("/*") ) {
            in_comment = true;
            continue;
        }
        else if ( command_string.startsWith("*/") ) {
            in_comment = false;
            continue;
        }
        if ( in_comment ) {
            continue;
        }
        if ( command instanceof ObjectListProvider ) {
        	Object o = ((ObjectListProvider)command).getObjectList ( new TSEnsemble().getClass() );
            List<TSEnsemble> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<TSEnsemble> list0 = (List<TSEnsemble>)o;
            	list = list0;
            }
            String id;
            if ( list != null ) {
                int listsize = list.size();
                TSEnsemble tsensemble;
                for ( int its = 0; its < listsize; its++ ) {
                    tsensemble = list.get(its);
                    id = tsensemble.getEnsembleID();
                    if ( (id != null) && !id.equals("") ) {
                        v.add( id );
                    }
                }
            }
        }
    }
    return v;
}

/**
Return the ensemble identifiers for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@return a list of String containing the ensemble identifiers, or an empty list.
*/
public static List<String> getEnsembleIdentifiersFromCommandsBeforeCommand( TSCommandProcessor processor, Command command ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getEnsembleIdentifiersFromCommandsBeforeCommand";
    // Get the position of the command in the list.
    int pos = processor.indexOf(command);
    if ( Message.isDebugOn ) {
        Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
    }
    if ( pos < 0 ) {
        // Just return a blank list.
        return new ArrayList<>();
    }
    // Find the commands above the position.
    List<Command> commands = getCommandsBeforeIndex ( processor, pos );
    // Get the time series identifiers from the commands.
    return getEnsembleIdentifiersFromCommands ( commands );
}

/**
Get a list of object identifiers from a list of commands.  See documentation for fully loaded method.
@param commands commands to search
@return list of object identifiers or an empty non-null list if nothing found.
*/
private static List<String> getObjectIdentifiersFromCommands ( List<Command> commands ) {
    // Default behavior.
    return getObjectIdentifiersFromCommands ( commands, false );
}

/**
Get a list of object identifiers from a list of commands.
The returned strings are suitable for drop-down lists, etc.  Object identifiers are determined as follows:
Commands that implement ObjectListProvider have their getObjectList(JSONObject) method called.
The getObjectID() method on the JSONObject is then returned.
@param commands Commands to search.
@param sort Should output be sorted by identifier.
@return list of object identifiers or an empty non-null list if nothing found.
*/
protected static List<String> getObjectIdentifiersFromCommands ( List<Command> commands, boolean sort ) {
    if ( commands == null ) {
        return new ArrayList<>();
    }
    List<String> objectIDList = new ArrayList<> ();
    int size = commands.size();
    boolean in_comment = false;
    Command command = null;
    String commandString = null;
    String commandName = null;
    for ( int i = 0; i < size; i++ ) {
        command = commands.get(i);
        commandString = command.toString();
        commandName = command.getCommandName();
        if ( commandString.startsWith("/*") ) {
            in_comment = true;
            continue;
        }
        else if ( commandString.startsWith("*/") ) {
            in_comment = false;
            continue;
        }
        if ( in_comment ) {
            continue;
        }
        // Commands that provide a list of objects (so add to the list).
        if ( command instanceof ObjectListProvider ) {
        	Object o = ((ObjectListProvider)command).getObjectList ( new JSONObject().getClass() );
            List<JSONObject> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<JSONObject> list0 = (List<JSONObject>)o;
            	list = list0;
            }
            String id;
            if ( list != null ) {
                int listsize = list.size();
                JSONObject object;
                for ( int its = 0; its < listsize; its++ ) {
                    object = list.get(its);
                    id = object.getObjectID();
                    if ( (id != null) && !id.isEmpty() ) {
                    	// Don't add if already in the list.
                    	boolean found = false;
                    	for ( String objectID : objectIDList ) {
                    		if ( id.equalsIgnoreCase(objectID) ) {
                    			found = true;
                    			break;
                    		}
                    	}
                    	if ( !found ) {
                    		objectIDList.add( id );
                    	}
                    }
                }
            }
        }
        else if ( commandName.equalsIgnoreCase("FreeObject") ) {
            // Need to remove matching object identifiers that are in the list
            // (otherwise editing commands will show extra objects as of that point in the workflow,
        	// which will be confusing and may lead to errors).
            // First get the matching objects for the FreeObject() command parameters.
            PropList parameters = command.getCommandParameters();
            String ObjectID = parameters.getValue("ObjectID");
            for ( int iObject = 0; iObject < objectIDList.size(); iObject++ ) {
                if ( objectIDList.get(iObject).equalsIgnoreCase(ObjectID) ) {
                    //Message.printStatus(2,"", "Removing object " + ObjectID );
                    objectIDList.remove(iObject--);
                }
            }
        }
    }
    if ( sort ) {
        java.util.Collections.sort(objectIDList);
    }
    return objectIDList;
}

/**
Return the object identifiers for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@return a list of String containing the object identifiers, or an empty list.
*/
public static List<String> getObjectIdentifiersFromCommandsBeforeCommand( TSCommandProcessor processor, Command command ) {
	return getObjectIdentifiersFromCommandsBeforeCommand( processor, command, false );
}

/**
Return the object identifiers for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@param inclusive if true also search the current specified command.
@return a list of String containing the object identifiers, or an empty list.
*/
public static List<String> getObjectIdentifiersFromCommandsBeforeCommand( TSCommandProcessor processor, Command command, boolean inclusive ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getObjectIdentifiersFromCommandsBeforeCommand";
    // Get the position of the command in the list.
    int pos = processor.indexOf(command);
    if ( Message.isDebugOn ) {
        Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
    }
    if ( pos < 0 ) {
        // Just return a blank list.
        return new ArrayList<>();
    }
    // If inclusive increment the index.
    if ( inclusive ) {
   		++pos;
    }
    // Find the commands above the position.
    List<Command> commands = getCommandsBeforeIndex ( processor, pos );
    // Get the time series identifiers from the commands.
    return getObjectIdentifiersFromCommands ( commands );
}

/**
Determine the output period for the command, first by evaluating the string properties that are passed in and
second by optionally using the global output period.  This method is used to shorten parameter-handling code
in the "run" method of many commands and can be used to determine the set period, analysis period, output period,
etc., for a command.
@param command the command requesting the period
@param commandPhase the processing phase - discovery phase examines SetOutputPeriod() commands.
@param startParameter the name of the start parameter, for messaging (e.g, "OutputStart", "AnalysisStart")
@param periodStart the value of the start parameter (e.g., YYYY-MM-DD or "OutputStart" to use the global value)
@param endParameter the name of the end parameter, for messaging (e.g, "OutputEnd", "AnalysisEnd")
@param periodEnd the value of the end parameter (e.g., YYYY-MM-DD or "OutputEnd" to use the global value)
@param defaultToGlobalOutputPeriod if false, only the parameter value will be interpreted; if true and the
parameter value cannot be evaluated to a date/time, then the global output period is determined and returned
@param logLevel for logging messages
@param commandTag for logging messages
@param warningLevel for warning messages
@param warningCount to count warnings in this code - commands will check the count and generate an overall
warning if > 0
@return the date/time range for the requested period - date/times will be null if unable to determine or the
global output period is not set and is used as a default
*/
public static DateTimeRange getOutputPeriodForCommand ( Command command, CommandPhaseType commandPhase,
    String startParameter, String periodStart, String endParameter, String periodEnd,
    boolean defaultToGlobalOutputPeriod,
    int logLevel, String commandTag, int warningLevel, WarningCount warningCount )
throws InvalidCommandParameterException {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getOutputPeriodForCommand";
    String message;
    CommandStatus status = null;
    if ( command instanceof AbstractCommand ) {
        status = ((AbstractCommand)command).getCommandStatus();
    }
    else {
        status = new CommandStatus();
    }
    TSCommandProcessor processor = (TSCommandProcessor)command.getCommandProcessor();
    DateTime setStartProcessor_DateTime = null;
    DateTime setEndProcessor_DateTime = null;
    DateTime start_DateTime = null;
    DateTime end_DateTime = null;
    // TODO SAM 2011-04-20 Need to figure out how to handle run-time properties and special values like OutputStart.
    if ( (commandPhase == CommandPhaseType.DISCOVERY) && defaultToGlobalOutputPeriod ) {
        // Need to walk the commands for SetOutputPeriod() commands to get the global period.
        // Also do this if the start or end rely on OutputStart or OutputEnd because the global period will be needed.
        // If command phase is RUN below, this information will be regenerated for each property.
        try {
            PropList requestParams = new PropList ( "" );
            requestParams.setUsingObject ( "Command", command );
            CommandProcessorRequestResultsBean bean =
                processor.processRequest( "GetOutputPeriodForCommand", requestParams);
            PropList bean_PropList = bean.getResultsPropList();
            Object propContents = bean_PropList.getContents ( "OutputStart" );
            if ( propContents != null ) {
                setStartProcessor_DateTime = (DateTime)propContents;
            }
            propContents = bean_PropList.getContents ( "OutputEnd" );
            if ( propContents != null ) {
                setEndProcessor_DateTime = (DateTime)propContents;
            }
        }
        catch ( Exception e ) {
            message = "Error getting request GetOutputPeriodForCommand.";
            Message.printWarning(warningLevel,
                MessageUtil.formatMessageTag( commandTag, warningCount.incrementCount()),
                routine, message );
            Message.printWarning(2, routine, e);
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "This is likely a software error - contact software support." ) );
        }
    }

    try {
        if ( (periodStart == null) || periodStart.equals("") ) {
            // Parameter is empty.
            if ( defaultToGlobalOutputPeriod ) {
                // Try to determine start from global OutputStart.
                if ( commandPhase == CommandPhaseType.RUN ) {
                    PropList request_params = new PropList ( "" );
                    request_params.set ( "DateTime", "OutputStart" );
                    CommandProcessorRequestResultsBean bean = null;
                    bean = processor.processRequest( "DateTime", request_params);
                    PropList bean_PropList = bean.getResultsPropList();
                    Object prop_contents = bean_PropList.getContents ( "DateTime" );
                    if ( prop_contents != null ) {
                        setStartProcessor_DateTime = (DateTime)prop_contents;
                    }
                }
                // OK to set to null.
                start_DateTime = setStartProcessor_DateTime;
            }
        }
        else {
            // Try to set from what user specified.
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", periodStart );
            CommandProcessorRequestResultsBean bean = null;
            bean = processor.processRequest( "DateTime", request_params);
            PropList bean_PropList = bean.getResultsPropList();
            Object prop_contents = bean_PropList.getContents ( "DateTime" );
            if ( (prop_contents == null) && (commandPhase == CommandPhaseType.RUN) ) {
                message = "Null value for " + startParameter + " DateTime(DateTime=" +periodStart +  ") returned from processor.";
                Message.printWarning(logLevel,
                    MessageUtil.formatMessageTag( commandTag, warningCount.incrementCount()),
                    routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Use a SetOutputPeriod() command or specify the " + startParameter + "." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                start_DateTime = (DateTime)prop_contents;
            }
        }
    }
    catch ( Exception e ) {
        message = startParameter + " \"" + periodStart + "\" is invalid.";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( commandTag, warningCount.incrementCount()),
            routine, message );
        Message.printWarning(2, routine, e);
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid " + startParameter + " date/time, or as OutputStart or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }

    try {
        if ( (periodEnd == null) || periodEnd.equals("") ) {
            if ( defaultToGlobalOutputPeriod ) {
                // Try to determine end from global OutputEnd.
                if ( commandPhase == CommandPhaseType.RUN ) {
                    PropList request_params = new PropList ( "" );
                    request_params.set ( "DateTime", "OutputEnd" );
                    CommandProcessorRequestResultsBean bean = null;
                    bean = processor.processRequest( "DateTime", request_params);
                    PropList bean_PropList = bean.getResultsPropList();
                    Object prop_contents = bean_PropList.getContents ( "DateTime" );
                    if ( prop_contents != null ) {
                        setEndProcessor_DateTime = (DateTime)prop_contents;
                    }
                }
                // OK to set to null.
                end_DateTime = setEndProcessor_DateTime;
            }
        }
        else {
            // Try to set from what user specified.
            PropList request_params = new PropList ( "" );
            request_params.set ( "DateTime", periodEnd );
            CommandProcessorRequestResultsBean bean = null;
            bean = processor.processRequest( "DateTime", request_params);
            PropList bean_PropList = bean.getResultsPropList();
            Object propContents = bean_PropList.getContents ( "DateTime" );
            if ( (propContents == null) && (commandPhase == CommandPhaseType.RUN) ) {
                message = "Null value for " + endParameter + " DateTime(DateTime=" + periodEnd + "\") returned from processor.";
                Message.printWarning(logLevel,
                    MessageUtil.formatMessageTag( commandTag, warningCount.incrementCount()),
                    routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Use a SetOutputPeriod() command or specify the " + endParameter + "." ) );
                throw new InvalidCommandParameterException ( message );
            }
            else {
                end_DateTime = (DateTime)propContents;
            }
        }
    }
    catch ( Exception e ) {
        message = endParameter + " \"" + periodEnd + "\" is invalid.";
        Message.printWarning(warningLevel,
            MessageUtil.formatMessageTag( commandTag, warningCount.incrementCount()),
            routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a valid " + endParameter + " date/time, or as OutputStart or OutputEnd." ) );
        throw new InvalidCommandParameterException ( message );
    }
    return new DateTimeRange ( start_DateTime, end_DateTime );
}

/**
Return the pattern time series for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of time identifiers to editor dialogs,
using information determined during discovery.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series are needed.
@return a List of pattern time series.
*/
public static List<TS> getPatternTSListFromCommandsBeforeCommand( TSCommandProcessor processor, Command command ) {
    //String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getPatternTSFromCommandsBeforeCommand";
    // Get the position of the command in the list.
    int pos = processor.indexOf(command);
    //Message.printStatus ( 2, routine, "Position in list is " + pos + " for command:" + command );
    if ( pos < 0 ) {
        // Just return a blank list.
        return new Vector<>();
    }
    // Find the commands above the position.
    List<Command> commands = getCommandsBeforeIndex ( processor, pos );
    // Get the time series identifiers from the commands.
    return getPatternTSListFromCommands ( commands );
}

/**
Get a list of pattern time series from a list of commands.
The time series can be used to extract identifiers for drop-down lists, etc.
Time series are determined as follows:
<ol>
<li>    Commands that implement ObjectListProvider have their getObjectList(TS) method called.
        The time series identifiers from the time series list are examined and those with alias
        will have the alias returned.  Otherwise, the full time series identifier is returned with or
        with input path as requested.</li>
</ol>
@param commands Commands to search.
@param List of pattern time series provided by commands.
*/
protected static List<TS> getPatternTSListFromCommands ( List<Command> commands ) {
    if ( commands == null ) {
        return new ArrayList<>();
    }
    List<TS> v = new ArrayList<>(10);
    int size = commands.size();
    Object command_o = null;    // Command as object.
    for ( int i = 0; i < size; i++ ) {
        command_o = commands.get(i);
        if ( (command_o != null) && (command_o instanceof ObjectListProvider) ) {
            // Try to get the list of identifiers using the interface method.
            // TODO SAM 2007-12-07 Evaluate the automatic use of the alias.
        	Object o = ((ObjectListProvider)command_o).getObjectList ( new TS().getClass() );
            List<TS> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<TS> list0 = (List<TS>)o;
            	list = list0;
            }
            if ( list != null ) {
                int tssize = list.size();
                TS ts;
                for ( int its = 0; its < tssize; its++ ) {
                    ts = list.get(its);
                    v.add( ts );
                }
            }
        }
    }
    // Sort the time series by identifier.
    TSUtil_SortTimeSeries tsu = new TSUtil_SortTimeSeries(v, null, null, null, 1 );
    try {
        return tsu.sortTimeSeries();
    }
    catch ( Exception e ) {
        // Return original order below.
    }
    return v;
}

/**
Lookup a property by its name and return the value as a Double.
This is useful for ${Property} lookups for floating point data.
The normal expandParameterValue() method does not work well because floating point numbers are
converted to scientific notation in some cases, which may truncate the value that is expected.
@param processor command processor from which to retrieve properties.
If it is ${Property} syntax the surrounding characters will be stripped.
@paramName parameter name to look up
@return the parameter value as a Double or null
@exception NumberFormatException if the property has a value but cannot be converted to a Double.
*/
public static Double getPropertyValueAsDouble ( TSCommandProcessor processor, String propName )
throws NumberFormatException {
	propName = propName.replace("${", "").replace("}","");
	Object o = null;
	try {
		o = processor.getPropContents ( propName );
	}
	catch ( Exception e ) {
		throw new NumberFormatException("Requested property \"" + propName + "\" is not found in processor");
	}
	if ( o == null ) {
		return null;
	}
	if ( o instanceof Double ) {
		return (Double)o;
	}
	else if ( o instanceof Float ) {
		return new Double((Float)o);
	}
	else {
		// Integers, strings, etc. try to parse Double and throw exception if an error.
		return Double.parseDouble(""+o);
	}
}

/**
Return the list of property names available from the processor.
These properties can be requested using getPropContents().
@return the list of property names available from the processor.
*/
public static Collection<String> getPropertyNameList( CommandProcessor processor ) {
	// This could use reflection.
	if ( processor instanceof TSCommandProcessor ) {
		return ((TSCommandProcessor)processor).getPropertyNameList(true,true);
	}
	return new Vector<>();
}

/**
Return the regression test disabled count.
@return the regression test disabled count.
*/
private static int getRegressionTestDisabledCount () {
    return __regressionTestDisabledCount;
}

/**
Return the regression test fail count.
@return the regression test fail count.
*/
private static int getRegressionTestFailCount () {
    return __regressionTestFailCount;
}

/**
Return the regression test pass count.
@return the regression test pass count.
*/
private static int getRegressionTestPassCount () {
    return __regressionTestPassCount;
}

/**
Get the total run time for the commands.  This is used, for example, by the RunCommands() command.
@param commands list of commands to determine total run time.
@return total run time for all commands, in milliseconds.
*/
public static long getRunTimeTotal ( List<Command> commands ) {
    long runTimeTotal = 0;
    if ( commands == null ) {
        return runTimeTotal;
    }
    for ( Command command : commands ) {
        runTimeTotal += command.getCommandProfile(CommandPhaseType.RUN).getRunTime();
    }
    return runTimeTotal;
}

// FIXME SAM 2008-01-31 Need to sort the column names.
/**
Return the table column names, searching commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of column names to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@param sort Indicates whether column names should be sorted (NOT YET IMPLEMENTED).
@return a list of String containing the ensemble identifiers, or an empty list.
*/
public static List<String> getTableColumnNamesFromCommandsBeforeCommand(
        TSCommandProcessor processor, Command command, String table_id, boolean sort ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getTableColumnNamesFromCommandsBeforeCommand";
    // Get the position of the command in the list.
    int pos = processor.indexOf(command);
    if ( Message.isDebugOn ) {
        Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
    }
    // Loop backwards because tables may be modified and we want the column names from
    // the table as close previous to the command in question.
    DataTable table;
    for ( int i = (pos - 1); i >= 0; i-- ) {
        command = processor.get(i);
        if ( command instanceof ObjectListProvider ) {
            // Request table objects.
        	Object o = ((ObjectListProvider)command).getObjectList(DataTable.class);
        	List<DataTable> tables = null;
        	if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<DataTable> tables0 = (List<DataTable>)o;
            	tables = tables0;
        	}
            int ntables = 0;
            if ( tables != null ) {
                ntables = tables.size();
            }
            for ( int it = 0; it < ntables; it++ ) {
                table = tables.get(it);
                if ( !table.getTableID().equalsIgnoreCase(table_id) ) {
                    continue;
                }
                // Found the table.  Get its column names.
                String [] field_names = table.getFieldNames();
                List<String> fieldNamesList = new Vector<>();
                for ( int in = 0; in < field_names.length; in++ ) {
                    fieldNamesList.add ( field_names[in] );
                }
                return fieldNamesList;
            }
        }
    }
    // Nothing found.
    return new Vector<>();
}

/**
Get a list of table identifiers from a list of commands.  See documentation for fully loaded method.
@param commands Time series commands to search.
@return list of table identifiers or an empty non-null list if nothing found.
*/
private static List<String> getTableIdentifiersFromCommands ( List<Command> commands ) {
    // Default behavior.
    return getTableIdentifiersFromCommands ( commands, false );
}

/**
Get a list of table identifiers from a list of commands.
The returned strings are suitable for drop-down lists, etc.  Table identifiers are determined as follows:
Commands that implement ObjectListProvider have their getObjectList(DataTable) method called.
The getTableID() method on the DataTable is then returned.
@param commands Commands to search.
@param sort Should output be sorted by identifier.
@return list of table identifiers or an empty non-null list if nothing found.
*/
protected static List<String> getTableIdentifiersFromCommands ( List<Command> commands, boolean sort ) {
    if ( commands == null ) {
        return new ArrayList<>();
    }
    List<String> tableIDList = new ArrayList<> ( 10 );
    int size = commands.size();
    boolean in_comment = false;
    Command command = null;
    String commandString = null;
    String commandName = null;
    for ( int i = 0; i < size; i++ ) {
        command = commands.get(i);
        commandString = command.toString();
        commandName = command.getCommandName();
        if ( commandString.startsWith("/*") ) {
            in_comment = true;
            continue;
        }
        else if ( commandString.startsWith("*/") ) {
            in_comment = false;
            continue;
        }
        if ( in_comment ) {
            continue;
        }
        // Commands that provide a list of tables (so add to the list).
        if ( command instanceof ObjectListProvider ) {
        	Object o = ((ObjectListProvider)command).getObjectList ( new DataTable().getClass() );
            List<DataTable> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<DataTable> list0 = (List<DataTable>)o;
            	list = list0;
            }
            String id;
            if ( list != null ) {
                int tablesize = list.size();
                DataTable table;
                for ( int its = 0; its < tablesize; its++ ) {
                    table = list.get(its);
                    id = table.getTableID();
                    if ( (id != null) && !id.isEmpty() ) {
                    	// Don't add if already in the list.
                    	boolean found = false;
                    	for ( String tableID : tableIDList ) {
                    		if ( id.equalsIgnoreCase(tableID) ) {
                    			found = true;
                    			break;
                    		}
                    	}
                    	if ( !found ) {
                    		tableIDList.add( id );
                    	}
                    }
                }
            }
        }
        else if ( commandName.equalsIgnoreCase("FreeTable") ) {
            // Need to remove matching table identifiers that are in the list
            // (otherwise editing commands will show extra tables as of that point in the workflow,
        	// which will be confusing and may lead to errors, e.g., if consistent units are expected but the units are not consistent).
            // First get the matching tables for the FreeTable() command parameters.
            PropList parameters = command.getCommandParameters();
            String TableID = parameters.getValue("TableID");
            for ( int iTable = 0; iTable < tableIDList.size(); iTable++ ) {
                if ( tableIDList.get(iTable).equalsIgnoreCase(TableID) ) {
                    //Message.printStatus(2,"", "Removing table " + TableID );
                    tableIDList.remove(iTable--);
                }
            }
        }
    }
    if ( sort ) {
        java.util.Collections.sort(tableIDList);
    }
    return tableIDList;
}

/**
Return the table identifiers for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@return a list of String containing the table identifiers, or an empty list.
*/
public static List<String> getTableIdentifiersFromCommandsBeforeCommand( TSCommandProcessor processor, Command command ) {
	return getTableIdentifiersFromCommandsBeforeCommand( processor, command, false );
}

/**
Return the table identifiers for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@param inclusive if true also search the current specified command.
@return a list of String containing the table identifiers, or an empty list.
*/
public static List<String> getTableIdentifiersFromCommandsBeforeCommand( TSCommandProcessor processor, Command command, boolean inclusive ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getTableIdentifiersFromCommandsBeforeCommand";
    // Get the position of the command in the list.
    int pos = processor.indexOf(command);
    if ( Message.isDebugOn ) {
        Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
    }
    if ( pos < 0 ) {
        // Just return a blank list.
        return new ArrayList<>();
    }
    // If inclusive increment the index.
    if ( inclusive ) {
   		++pos;
    }
    // Find the commands above the position.
    List<Command> commands = getCommandsBeforeIndex ( processor, pos );
    // Get the time series identifiers from the commands.
    return getTableIdentifiersFromCommands ( commands );
}

/**
Get values for a tag in command comments.
Tags are strings like "@tagName" or "@tagName value" (without the quotes).
@param processor CommandProcessor to evaluate.
@param tag Tag to search for, without the leading "@".
@return a list of tag values, which are either Strings for the value or True if the tag has no value.
Return an empty list if the tag was not found.
*/
public static List<Object> getTagValues ( CommandProcessor processor, String tag ) {
    List<Object> tagValues = new ArrayList<>();
    // Loop through the commands and check comments for the special string.
    List<Command> commandList = ((TSCommandProcessor)processor).getCommands();
    int size = commandList.size();
    Command c;
    String searchTag = "@" + tag;
    for ( int i = 0; i < size; i++ ) {
        c = commandList.get(i);
        String commandString = c.toString();
        if ( !commandString.trim().startsWith("#") ) {
            continue;
        }
        // Check the comment.
        int pos = StringUtil.indexOfIgnoreCase(commandString,searchTag,0);
        if ( pos >= 0 ) {
            List<String> parts = StringUtil.breakStringList(
                commandString.substring(pos)," \t", StringUtil.DELIM_SKIP_BLANKS);
            if ( parts.size() == 1 ) {
                // No value to the tag so.
                tagValues.add ( new Boolean(true) );
            }
            else {
                // Add as a string - note that this value may contain multiple values separated by
                // commas or some other encoding.  The calling code needs to handle.
                tagValues.add ( parts.get(1) );
            }
        }
    }
    return tagValues;
}

/**
Get values for a tag in command file comments.
Tags are strings like "@tagName" or "@tagName value" (without the quotes).
@param processor CommandProcessor to evaluate.
@param tag Tag to search for, without the leading "@".
@return a list of tag values, which are either Strings for the value or True if the tag has
no value.  Return an empty list if the tag was not found.
*/
public static List<Object> getTagValues ( String commandFile, String tag )
throws IOException, FileNotFoundException {
	// There is no need for initial properties so pass null.
    TSCommandProcessor processor = new TSCommandProcessor(null);
    // TODO SAM 2013-02-17 This might be an expensive way to parse tags because
    // command objects are heavier than simple strings.
    processor.readCommandFile(commandFile, true, false, false);
    return getTagValues ( processor, tag );
}

/**
Get a list of time series identifiers from a list of commands.  See documentation for fully loaded method.
The output list is not sorted and does NOT contain the input type or name.
@param commands Time series commands to search.
@return list of time series identifiers or an empty non-null list if nothing found.
*/
private static List<String> getTSIdentifiersFromCommands ( List<Command> commands ) {
	// Default behavior.
	return getTSIdentifiersFromCommands ( commands, false, false );
}

/**
Get a list of identifiers from a list of commands.
See documentation for fully loaded method.
The output list does NOT contain the input type or name.
@param commands Time series commands to search.
@param sort Should output be sorted by identifier.
@return list of time series identifiers or an empty non-null list if nothing found.
*/
protected static List<String> getTSIdentifiersFromCommands ( List<Command> commands, boolean sort ) {
	// Return the identifiers without the input type and name.
	return getTSIdentifiersFromCommands ( commands, false, sort );
}

/**
Get a list of time series identifiers from a list of commands.
These strings are suitable for drop-down lists, etc.
Time series identifiers are determined as follows:
<ol>
<li>    Commands that implement ObjectListProvider have their getObjectList(TS) method called.
        The time series identifiers from the time series list are examined and those with alias will have the alias returned.
        Otherwise, the full time series identifier is returned with or with input path as requested.</li>
<li>    Command strings that start with "TS ? = " have the alias (?) returned.</li>
<li>    Lines that are time series identifiers are returned, including the full path as requested.</li>
</ol>
@param commands commands to search, in order of first command to process to last.
@param include_input If true, include the input type and name in the returned values.
If false, only include the 5-part TSID information.
If an alias is returned, it is not impacted by this parameter.
@param sort Should output be sorted by identifier (currently ignored)
@return list of time series identifiers or an empty non-null list if nothing found.
*/
protected static List<String> getTSIdentifiersFromCommands ( List<Command> commands, boolean include_input, boolean sort ) {
	if ( commands == null ) {
		return new ArrayList<>();
	}
	List<String> tsidsFromCommands = new ArrayList<>(); // The String TSID or alias.
	List<TS> tsFromCommands = new ArrayList<>(); // The ts for available TS, used to store each TSIdent.
	int size = commands.size();
	String commandString = null;
	List<String> tokens = null;
	boolean in_comment = false;
	Object command_o = null; // Command as object.
	String commandName; // Command name.
	Command command; // Command as Command instance.
	for ( int i = 0; i < size; i++ ) {
		command_o = commands.get(i);
		command = null;
		commandName = ""; // Only care about instances of Free() commands below.
		if ( command_o instanceof Command ) {
			commandString = command_o.toString().trim();
			command = (Command)command_o;
			commandName = command.getCommandName();
		}
		// TODO SAM 2017-03-17 seems like the following can be removed because
		// code update to generics does not indicate any use of string version.
		else if ( command_o instanceof String ) {
			commandString = ((String)command_o).trim();
		}
		if ( (commandString == null) || commandString.startsWith("#") || (commandString.length() == 0) ) {
			// Make sure comments are ignored.
			continue;
		}
		if ( commandString.startsWith("/*") ) {
			in_comment = true;
			continue;
		}
		else if ( commandString.startsWith("*/") ) {
			in_comment = false;
			continue;
		}
		if ( in_comment ) {
			continue;
		}
        if ( (command_o != null) && (command_o instanceof ObjectListProvider) ) {
            // Try to get the list of identifiers using the interface method.
            // TODO SAM 2007-12-07 Evaluate the automatic use of the alias (takes priority over TSID) - probably good.
        	Object o = ((ObjectListProvider)command_o).getObjectList ( new TS().getClass() );
            List<TS> list = null;
            if ( o != null ) {
            	@SuppressWarnings("unchecked")
				List<TS> list0 = (List<TS>)o;
            	list = list0;
            }
            if ( list != null ) {
                int tssize = list.size();
                TS ts;
                for ( int its = 0; its < tssize; its++ ) {
                    ts = list.get(its);
                    if ( ts == null ) {
                    	// This should not happen and is symptomatic of a command not fully handling a
                    	// time series in discovery mode, more of an issue with ${property} use in parameters.
                    	// Log a message so the issue can be tracked down.
                    	String routine = "TSCommandProcessorUtil.getTSIdentifiersFromCommands";
                    	Message.printWarning(3, routine, "Null time series in discovery mode - need to check code for command to improve handling: " + commandString);
                    	continue;
                    }
                    if ( !ts.getAlias().equals("") ) {
                        // Use the alias if it is available.
                        tsidsFromCommands.add( ts.getAlias() );
                    }
                    else {
                        // Use the identifier.
                        tsidsFromCommands.add ( ts.getIdentifier().toString(include_input) );
                    }
                    tsFromCommands.add( ts );
                }
            }
        }
		else if ( StringUtil.startsWithIgnoreCase(commandString,"TS ") ) {
		    // TODO SAM 2011-04-04 Remove this code after some period - TSTool version 10.00.00 removed the
		    // TS Alias syntax, which should be migrated as commands are parsed.
			// Use the alias.
			tokens = StringUtil.breakStringList( commandString.substring(3)," =", StringUtil.DELIM_SKIP_BLANKS);
			if ( (tokens != null) && (tokens.size() > 0) ) {
			    String alias = tokens.get(0);
				tsidsFromCommands.add ( alias );
				//+ " (alias)" );
				// Treat as an alias.
				TS ts = new TS();
				TSIdent tsident = new TSIdent();
				tsident.setAlias(alias);
				try {
				    ts.setIdentifier(tsident);
				}
				catch ( Exception e ) {
				    // This code should be phased out so don't worry about this issue.
				}
                tsFromCommands.add( ts );
			}
		}
		else if ( isTSID(commandString) ) {
			// Reasonably sure it is an identifier.
			// Only add the 5-part TSID and not the trailing input type and name.
			int pos = commandString.indexOf("~");
			String tsid;
			if ( (pos < 0) || include_input ) {
				// Add the whole thing.
				tsid = commandString;
			}
			else {
			    // Add the part before the input fields.
				tsid = commandString.substring(0,pos);
			}
			tsidsFromCommands.add ( tsid );
			// For the purpose of handling TSIdents for Free(), treat as an alias.
            TS ts = new TS();
			TSIdent tsident = null;
			try {
			    tsident = new TSIdent(tsid);
			}
			catch ( Exception e ) {
			    // Should not happen because isTSID() called at start of code block.
			}
			// Also set the identifier as the alias.
            tsident.setAlias(tsid);
            try {
                ts.setIdentifier(tsident);
            }
            catch ( Exception e ) {
                // Should not happen because isTSID() called at start of code block.
            }
            tsFromCommands.add( ts );
		}
		else if ( commandName.equalsIgnoreCase("Free") ) {
		    // Need to remove matching time series identifiers that are in the list
		    // (otherwise editing commands will show extra time series as of that point in the workflow, which will
		    // be confusing and may lead to errors, e.g., if consistent units are expected but the units are not consistent).
		    // First get the matching time series for the Free() command parameters.
		    Command commandInst = (Command)command_o;
		    PropList parameters = commandInst.getCommandParameters();
		    // TODO SAM 2011-04-04 Need to get ensembles above command.
		    List<TSEnsemble> ensemblesFromCommands = new ArrayList<>();
		    TimeSeriesToProcess tsToProcess = getTSMatchingTSListParameters(tsFromCommands, ensemblesFromCommands,
	            parameters.getValue("TSList"), parameters.getValue("TSID"),
	            parameters.getValue("TSPosition"), parameters.getValue("EnsembleID") );
		    // Loop through the list of matching time series and remove identifiers at the matching positions
		    // (the time series list and identifier lists should match in position).
		    int [] pos = tsToProcess.getTimeSeriesPositions();
		    //Message.printStatus(2,"", "Detected Free() command, have " + pos.length + " time series to check." );
		    // Loop backwards so that position values don't need to be adjusted.
		    for ( int ipos = pos.length - 1; ipos >= 0; ipos--  ) {
	            //Message.printStatus(2,"", "Removing time series " + pos[ipos] + ": " + tsidsFromCommands.get(pos[ipos]));
		        tsFromCommands.remove(pos[ipos]);
		        tsidsFromCommands.remove(pos[ipos]);
		    }
		}
	}
	return tsidsFromCommands;
}

/**
Return the time series identifiers for commands before a specific command in the TSCommandProcessor.
This is used, for example, to provide a list of identifiers to editor dialogs.
@param processor a TSCommandProcessor that is managing commands.
@param command the command above which time series identifiers are needed.
@return a list of String containing the time series identifiers, or an empty list.
*/
public static List<String> getTSIdentifiersNoInputFromCommandsBeforeCommand( TSCommandProcessor processor, Command command ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getTSIdentifiersNoInputFromCommandsBeforeCommand";
	// Get the position of the command in the list.
	int pos = processor.indexOf(command);
	if ( Message.isDebugOn ) {
	    Message.printDebug ( 1, routine, "Position in list is " + pos + " for command:" + command );
	}
	if ( pos < 0 ) {
		// Just return a blank list.
		return new ArrayList<>();
	}
    // Find the commands above the position.
	List<Command> commands = getCommandsBeforeIndex ( processor, pos );
	// Get the time series identifiers from the commands.
	return getTSIdentifiersFromCommands ( commands );
}

/**
Get time series that match the TSList and related input.
This method is used to evaluate the list of time series from the time series processor,
and the list of discovery time series extracted from commands
(that maintain their own discovery information).
@param tsCandidateList list of time series to check for matching time series identifier
@param ensembleCandidateList list of ensembles to check for matching ensemble identifier
@param TSList string value for TSList (should match TSListType enumeration) - if null or a blank string,
TSListType.ALL_TS will be used by default
@param TSPosition string value of TSPosition range notation
@param EnsembleID ensemble identifier to match
*/
public static TimeSeriesToProcess getTSMatchingTSListParameters ( List<TS> tsCandidateList,
    List<TSEnsemble> ensembleCandidateList, String TSList, String TSID, String TSPosition, String EnsembleID ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getTimeSeriesToProcess";
    if ( (TSList == null) || TSList.equals("") ) {
        // Default is to match all.
        TSList = "" + TSListType.ALL_TS;
    }
    List<TS> tslist = new ArrayList<>(); // List of time series to process.
    List<String> errorList = new ArrayList<>(); // List of error messages finding time series.
    if ( (tsCandidateList == null) || (tsCandidateList.size() == 0) ) {
        // Return an empty list.
        return new TimeSeriesToProcess(tslist, new int[0], errorList);
    }
    int nts = tsCandidateList.size();
    // Positions of time series to process.
    // Size to match the full list but may not be filled initially - trim before returning.
    int [] tspos = new int[nts];
    // Loop through the time series in memory.
    int count = 0;
    TS ts = null;
    if ( Message.isDebugOn ) {
        Message.printDebug( 1, routine, "Getting list of time series to process using TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\", TSPosition=\"" + TSPosition + "\"" );
    }
    if ( TSList.equalsIgnoreCase(TSListType.FIRST_MATCHING_TSID.toString()) ) {
        // Search forward for the first single matching time series.
        for ( int its = 0; its < nts; its++ ) {
            try {
                ts = tsCandidateList.get ( its );
                if ( ts == null ) {
                    continue;
                }
            }
            catch ( Exception e ) {
                // Don't add.
                continue;
            }
            if ( TSID.indexOf("~") > 0 ) {
                // Include the input type.
                if (ts.getIdentifier().matches(TSID,true,true)){
                    tslist.add ( ts );
                    tspos[count++] = its;
                    // Only return the single index.
                    int [] tspos2 = new int[1];
                    tspos2[0] = tspos[0];
                    // Only want one match.
                    return new TimeSeriesToProcess(tslist, tspos2, errorList);
                }
            }
            else {
                // Just check the main information.
                if(ts.getIdentifier().matches(TSID,true,false)){
                    tslist.add ( ts );
                    tspos[count++] = its;
                    // Only return the single index.
                    int [] tspos2 = new int[1];
                    tspos2[0] = tspos[0];
                    // Only want one match.
                    return new TimeSeriesToProcess(tslist, tspos2, errorList);
                }
            }
        }
        // Return empty list since no match.
        errorList.add("Unable to find first matched TSID \"" + TSID + "\"" );
        return new TimeSeriesToProcess(tslist, new int[0], errorList);
    }
    else if ( TSList.equalsIgnoreCase(TSListType.LAST_MATCHING_TSID.toString()) ) {
        // Search backwards for the last single matching time series.
        for ( int its = (nts - 1); its >= 0; its-- ) {
            try {
                ts = tsCandidateList.get ( its );
            }
            catch ( Exception e ) {
                // Don't add.
                continue;
            }
            if ( TSID.indexOf("~") > 0 ) {
                // Include the input type.
                if (ts.getIdentifier().matches(TSID,true,true)){
                    tslist.add ( ts );
                    tspos[count++] = its;
                    // Only return the single index.
                    int [] tspos2 = new int[1];
                    tspos2[0] = tspos[0];
                    // Only want one match.
                    return new TimeSeriesToProcess(tslist, tspos2, errorList);
                }
            }
            else {
                // Just check the main information.
                if(ts.getIdentifier().matches(TSID,true,false)){
                    tslist.add ( ts );
                    tspos[count++] = its;
                    // Only return the single index.
                    int [] tspos2 = new int[1];
                    tspos2[0] = tspos[0];
                    // Only want one match.
                    return new TimeSeriesToProcess(tslist, tspos2, errorList);
                }
            }
        }
        // Return empty list since no match.
        errorList.add("Unable to find last matched TSID \"" + TSID + "\"" );
        return new TimeSeriesToProcess(tslist, new int[0], errorList);
    }
    else if ( TSList.equalsIgnoreCase(TSListType.ENSEMBLE_ID.toString()) ) {
        // Return a list of all time series in an ensemble.
        // FIXME SAM 2009-10-10 Need to fix issue of index positions being found in ensemble but
        // then not matching the main TS list (in case where time series were copied to ensemble).
        // For now, do not allow time series to be copied to ensemble (e.g., in NewEnsemble() command).
        TSEnsemble ensemble = null;
        if ( ensembleCandidateList != null ) {
            for ( TSEnsemble tsensemble : ensembleCandidateList ) {
                if ( tsensemble.getEnsembleID().equalsIgnoreCase(EnsembleID) ) {
                    ensemble = tsensemble;
                    break;
                }
            }
        }
        if ( ensemble == null ) {
            errorList.add ("Unable to find ensemble \"" + EnsembleID + "\" to get time series.");
            return new TimeSeriesToProcess(tslist, new int[0], errorList);
        }
        else {
            int esize = ensemble.size();
            for ( int ie = 0; ie < esize; ie++ ) {
                // Set the time series instance (always what is included in the ensemble).
                ts = ensemble.get (ie);
                tslist.add ( ts );
                // Figure out the index in the processor time series list by comparing the instance.
                TS ts2; // Time series in main list to compare against.
                boolean found = false;
                // Loop through the main list.
                for ( int its = 0; its < nts; its++ ) {
                    try {
                        ts2 = tsCandidateList.get ( its );
                    }
                    catch ( Exception e ) {
                        continue;
                    }
                    if ( ts == ts2 ) {
                        found = true;
                        tspos[count++] = its;
                        break;
                    }
                }
                if ( !found ) {
                    // This will happen when time series are copied to ensembles in order to protect the
                    // data from further modification.  Time series will then always need to be accessed via the ensemble.
                    Message.printStatus( 3, routine, "Unable to find ensemble \"" + EnsembleID +
                        "\" time series \"" + ts.getIdentifier() + "\" - setting index to -1.");
                    tspos[count++] = -1; // TODO SAM 2009-10-08 - will this impact other code?
                }
            }
        }
        // Trim down the "tspos" array to only include matches so that other
        // code does not mistakenly iterate through a longer array.
        int [] tspos2 = new int[count];
        for ( int i = 0; i < count; i++ ) {
            tspos2[i] = tspos[i];
        }
        return new TimeSeriesToProcess(tslist, tspos2, errorList);
    }
    else if ( TSList.equalsIgnoreCase(TSListType.SPECIFIED_TSID.toString()) ) {
        // Return a list of time series that match the provided identifiers.
        List<String> tsid_List = StringUtil.breakStringList ( TSID, ",", StringUtil.DELIM_SKIP_BLANKS );
        int size_tsid = 0;
        if ( tsid_List != null ) {
            size_tsid = tsid_List.size();
        }
        for ( int itsid = 0; itsid < size_tsid; itsid++ ) {
            String tsid = tsid_List.get(itsid);
            Message.printStatus( 2, routine, "Trying to match \"" + tsid + "\"" );
            // Loop through the available time series and see if any match.
            boolean found = false;
            for ( int its = 0; its < nts; its++ ) {
                try {
                    ts = tsCandidateList.get ( its );
                }
                catch ( Exception e ) {
                    // Don't add.
                    continue;
                }
                // Compare the requested TSID with that in the time series list.
                if ( tsid.indexOf("~") > 0 ) {
                    // Include the input type.
                    if (ts.getIdentifier().matches(tsid,true,true)){
                        //Message.printStatus( 2, routine,
                        //        "Matched using input with TSID=\"" + ts.getIdentifier() + "\"" +
                        //        " Alias=\"" + ts.getAlias() + "\"");
                        found = true;
                    }
                }
                else {
                    // Just check the main information.
                    if(ts.getIdentifier().matches(tsid,true,false)){
                        //Message.printStatus( 2, routine,
                        //        "Matched not using input with TSID=\"" + ts.getIdentifier() + "\"" +
                        //        " Alias=\"" + ts.getAlias() + "\"");
                        found = true;
                    }
                }
                if ( found ) {
                    // Add the time series and increment the count.
                    tslist.add ( ts );
                    tspos[count++] = its;
                    // Found the specific time series so break out of the list.
                    // FIXME SAM 2008-02-05 What if user has the same ID more than once?
                    break;
                }
            }
            if ( !found ) {
                // Did not find a specific time series, which is a problem.
                errorList.add ( "Did not match requested (specified) time series \"" + tsid + "\"" );
            }
        }
        // Trim down the "tspos" array to only include matches so that other
        // code does not mistakenly iterate through a longer array.
        Message.printStatus( 2, routine, "Matched " + count + " time series." );
        int [] tspos2 = new int[count];
        for ( int i = 0; i < count; i++ ) {
            tspos2[i] = tspos[i];
        }
        return new TimeSeriesToProcess(tslist, tspos2, errorList);
    }
    else if ( TSList.equalsIgnoreCase(TSListType.TSPOSITION.toString()) ) {
        // Process the position string.
        List<String> tokens = StringUtil.breakStringList ( TSPosition,",", StringUtil.DELIM_SKIP_BLANKS );
        int npos = 0;
        if ( tokens != null ) {
            npos = tokens.size();
        }
        int tsposStart, tsposEnd;
        for ( int i = 0; i < npos; i++ ) {
            String token = tokens.get(i);
            if ( token.indexOf("-") >= 0 ) {
                // Range.
                String posString = StringUtil.getToken(token, "-",0,0).trim();
                tsposStart = Integer.parseInt( posString ) - 1;
                posString = StringUtil.getToken(token, "-",0,1).trim();
                tsposEnd = Integer.parseInt( posString ) - 1;
            }
            else {
                // Single value.  Treat as a range of 1.
                tsposStart = Integer.parseInt(token) - 1;
                tsposEnd = tsposStart;
            }
            for ( int itspos = tsposStart; itspos <= tsposEnd; itspos++ ) {
                try {
                    tslist.add ( tsCandidateList.get(itspos) );
                }
                catch ( Exception e ) {
                    // Don't add.
                    // FIXME SAM 2008-07-07 Evaluate whether exception needs to be thrown for out of range index.
                }
                tspos[count++] = itspos;
            }
        }
        // Trim down the "tspos" array to only include matches so that other
        // code does not mistakenly iterate through a longer array.
        int [] tspos2 = new int[count];
        for ( int i = 0; i < count; i++ ) {
            tspos2[i] = tspos[i];
        }
        return new TimeSeriesToProcess(tslist, tspos2, errorList);
    }
    else {
        // Else loop through all the time series from first to last and find matches.  This is for:
        // TSList = AllTS
        // TSList = SELECTED_TS
        // TSList = ALL_MATCHING_TSID
        boolean found = false;
        for ( int its = 0; its < nts; its++ ) {
            found = false;
            try {
                ts = tsCandidateList.get ( its );
            }
            catch ( Exception e ) {
                // Don't add.
                continue;
            }
            if ( TSList.equalsIgnoreCase(TSListType.ALL_TS.toString()) ) {
                found = true;
            }
            else if( TSList.equalsIgnoreCase(TSListType.SELECTED_TS.toString()) && ts.isSelected() ) {
                found = true;
            }
            else if ( TSList.equalsIgnoreCase(TSListType.ALL_MATCHING_TSID.toString()) ) {
                if ( TSID.indexOf("~") > 0 ) {
                    // Include the input type.
                    if (ts.getIdentifier().matches(TSID,true,true)){
                        found = true;
                    }
                }
                else {
                    // Just check the main information.
                    if(ts.getIdentifier().matches(TSID,true,false)){
                        found = true;
                    }
                }
            }
            if ( found ) {
                // Add the time series and increment the count.
                tslist.add ( ts );
                tspos[count++] = its;
            }
        }
        // Trim down the "tspos" array to only include matches so that other
        // code does not mistakenly iterate through a longer array.
        int [] tspos2 = new int[count];
        for ( int i = 0; i < count; i++ ) {
            tspos2[i] = tspos[i];
        }
        //Message.printStatus( 2, routine, tslist.toString() );
        return new TimeSeriesToProcess(tslist, tspos2, errorList);
    }
}

/**
Get the current working directory for the processor.
@param processor the CommandProcessor to use to get data.
@return The working directory in effect for a command.
*/
public static String getWorkingDir ( CommandProcessor processor ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getWorkingDir";
	try {
	    Object o = processor.getPropContents ( "WorkingDir" );
		if ( o != null ) {
			return (String)o;
		}
	}
	catch ( Exception e ) {
		// Not fatal, but of use to developers.
		String message = "Error requesting WorkingDir from processor.";
		Message.printWarning(3, routine, message );
	}
	return null;
}

/**
Get the working directory for a command (e.g., for editing).
This processes all SetWorkingDir commands prior to the command,
in case the command file has moved through various folders using relative paths.
TODO smalers 2021-10-24 this code may be phased out since SetWorkingDir is discouraged.
@param processor the CommandProcessor to use to get data.
@param command Command for which to get the working directory.
@return The working directory in effect for a command.
*/
public static String getWorkingDirForCommand ( CommandProcessor processor, Command command ) {
	String routine = TSCommandProcessorUtil.class.getSimpleName() + ".getWorkingDirForCommand";
	PropList request_params = new PropList ( "" );
	request_params.setUsingObject ( "Command", command );
	CommandProcessorRequestResultsBean bean = null;
	try {
		bean = processor.processRequest( "GetWorkingDirForCommand", request_params );
		return bean.getResultsPropList().getValue("WorkingDir");
	}
	catch ( Exception e ) {
		String message = "Error requesting GetWorkingDirForCommand(Command=\"" + command + "\" from processor).";
		Message.printWarning(3, routine, e);
		Message.printWarning(3, routine, message );
	}
	return null;
}

/**
Determine the index of a command in the processor.  A reference comparison occurs.
@param command A command to search for in the processor.
@param startIndex the starting index for processing.
@return the index (0+) of the matching command, or -1 if not found.
*/
public static int indexOf ( CommandProcessor processor, Command command, int startIndex ) {
    List<Command> commands = processor.getCommands();
    int size = commands.size();
    Command c;
    for ( int i = startIndex; i < size; i++ ) {
        c = commands.get(i);
        if ( c == command ) {
            return i;
        }
    }
    return -1;
}

/**
 * Determine whether the command is an annotation command:
 * The following case is matched:
 * <pre>
 * #@annotation ...
 * # @annotation ...
 * </pre>
 * The following is not matched because it is often used in comments for automated tests:
 * <pre>
 * # Some text #@annotation ...
 * </pre>
 * @param command Annotation command to parse
 * @return true of the command is an annotation command, false if not
 */
public static boolean isAnnotationCommand ( Command command ) {
	// Trim leading spaces.
	String commandString = command.toString().trim();
	if ( !commandString.startsWith("#") ) {
		// Not in an annotation comment.
		return false;
	}
	int pos1 = commandString.indexOf("@");
	if ( pos1 < 0 ) {
		// Not in an annotation comment.
		return false;
	}
	
	// Try to parse the annotation:
	// - do so as if requesting the annotation name
	// - can't call getAnnotationCommandParameter because it is more granular and returns null in various situations

	// Initialize the annotation string to null.
	String annotationName = null;
	// Advance to the space after the annotation name.
	int pos2 = commandString.indexOf(" ",pos1);
	if ( pos2 < 0 ) {
		// No space after the annotation name so no parameters, something like "# @readOnly"
		// Requested the annotation name so return it without the leading @.
		annotationName = commandString.substring(pos1 + 1).trim();
	}
	else {
		// Have annotation name and parameters but want the parameter name.
		annotationName = commandString.substring(pos1 + 1,pos2).trim();
	}
	
	// Check for '# ..... #@abc' and '# ..... # @abc', which are normal comments.
	if ( annotationName != null ) {
		// If there is a '#' before the annotation comment in the original string, then it is a true comment.
		// First find the '#' immediately before the annotation.
		for ( int pos = pos1; pos >= 0; --pos ) {
			if ( commandString.charAt(pos) == '#' ) {
				// Found the first '#':
				// - search for a preceding '#':
				for ( int pos3 = (pos - 1); pos3 >= 0; --pos3 ) {
					if ( commandString.charAt(pos3) == '#' ) {
						// Found a preceding '#' so the command is a comment that is intended to NOT be an annotation.
						annotationName = null;
						break;
					}
				}
			}
			if ( annotationName == null ) {
				break;
			}
		}
	}
	
	// Return the final result, may be null.
	if ( (annotationName == null) || annotationName.isEmpty() ) {
		return false;
	}
	else {
		return true;
	}
}

/**
Evaluate whether a command appears to be a pure time series identifier
(not a command that uses a time series identifier).  The string is checked to see if:
<ol>
<li>    it has at least three "."</li>
<li>    parentheses are allowed in any part due to various data sources requirements</li>
</ol>
Multi-line / * * / comment strings should not be passed to this method because it will not
know if the command is in a comment block.
@param command Command to evaluate.
@return true if the command appears to be a pure TSID, false if not.
*/
protected static boolean isTSID ( String command ) {
	String commandTrimmed = command.trim();
    if ( commandTrimmed.startsWith( "TS " ) ) {
	    // TS Alias command.
	    return false;
	}
    if ( commandTrimmed.startsWith( "#" ) || commandTrimmed.startsWith("/*") ||
        commandTrimmed.endsWith("*/") ) {
        // Comment.
        return false;
    }
    // TODO SAM 2014-06-20 Will need to handle escaped periods at some point.
	if ( StringUtil.patternCount(command,".") < 3 ) {
	    // Not enough periods.
	    return false;
	}
	if ( commandTrimmed.endsWith(")") ) {
	    // This cuts out normal commands - TSIDs likely would not have ) at end in the scenario.
	    return false;
	}
	return true;
}

/**
Kill any processes associated with the list of commands.
Any commands that implements the ProcessRunner interface are checked.
@param commandList the list of commands to check.
*/
public static void killCommandProcesses ( List<Command>commandList ) {
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".killCommandProcesses";
    int size = 0;
    if ( commandList != null ) {
        // Use all commands.
        size = commandList.size();
    }
    Command command;
    for ( int i = 0; i < size; i++ ) {
        command = commandList.get(i);
        if ( command instanceof ProcessRunner ) {
            ProcessRunner pr = (ProcessRunner)command;
            List<Process> processList = pr.getProcessList();
            int processListSize = processList.size();
            for ( int iprocess = 0; iprocess < processListSize; iprocess++ ) {
                Process process = processList.get(iprocess);
                Message.printStatus ( 2, routine, "Destroying process for command: " + command.toString() );
                process.destroy();
            }
        }
    }
}

/**
Open a new regression test report file.
@param outputFile Full path to report file to open.
@param table data table to receive report results, or null if no table will be used.
@param append indicates whether the file should be opened in append mode.
*/
public static void openNewRegressionTestReportFile ( String outputFile, DataTable table, boolean append )
throws FileNotFoundException {
    // Initialize the report counts.
    __regressionTestLineCount = 0;
    __regressionTestFailCount = 0;
    __regressionTestPassCount = 0;
    // Save the table to be used for the regression summary.
    __regressionTestTable = table;
    // Print the report headers.
    __regression_test_file = outputFile;
    __regression_test_fp = new PrintWriter ( new FileOutputStream ( __regression_test_file, append ) );
    IOUtil.printCreatorHeader ( __regression_test_fp, "#", 80, 0 );
    __regression_test_fp.println ( "#" );
    __regression_test_fp.println ( "# Command file regression test report from StartRegressionTestResultsReport() and RunCommands()" );
    __regression_test_fp.println ( "#" );
    __regression_test_fp.println ( "# Explanation of columns:" );
    __regression_test_fp.println ( "#" );
    __regression_test_fp.println ( "# Num:" );
    __regression_test_fp.println ( "#   Running count of the tests." );
    __regression_test_fp.println ( "#   Use a text file without numbers to compare differences." );
    __regression_test_fp.println ( "# Enabled:" );
    __regression_test_fp.println ( "#   Default is TRUE.");
    __regression_test_fp.println ( "#   FALSE if \"#@enabled False\" anywhere in the command file." );
    __regression_test_fp.println ( "#   FALSE if any \"#@enabledif ...\" criteria are not met for the command file." );
    __regression_test_fp.println ( "# Run Time:" );
    __regression_test_fp.println ( "#    Run time in milliseconds." );
    __regression_test_fp.println ( "#    Use table output to see this column." );
    __regression_test_fp.println ( "# Test Pass/Fail:" );
    __regression_test_fp.println ( "#    The test status below may be PASS or FAIL (or blank if disabled)." );
    __regression_test_fp.println ( "#    A test will pass if the command file actual status matches the expected status." );
    __regression_test_fp.println ( "#    Disabled tests are not run and do not count as PASS or FAIL." );
    __regression_test_fp.println ( "#    Search for *FAIL* to find failed tests." );
    __regression_test_fp.println ( "# Commands Expected Status:" );
    __regression_test_fp.println ( "#    Default is SUCCESS." );
    __regression_test_fp.println ( "#    \"#@expectedStatus Warning|Failure\" comment in the command file overrides the default." );
    __regression_test_fp.println ( "# Commands Actual Status:" );
    __regression_test_fp.println ( "#    The most severe status (Success|Warning|Failure) for each command file." );
    __regression_test_fp.println ( "#" );
    __regression_test_fp.println ( "#    |       |Test  |Commands  |Commands   |" );
    __regression_test_fp.println ( "#    |       |Pass/ |Expected  |Actual     |" );
    __regression_test_fp.println ( "# Num|Enabled|Fail  |Status    |Status     |Command File" );
    __regression_test_fp.println ( "#----+-------+------+----------+-----------+------------------" +
    		"---------------------------------------------------------------------------" );
}

/**
Process a time series after reading.  This calls the command processor readTimeSeries2() method.
Command status messages will be added if problems arise but exceptions are not thrown.
*/
public static int processTimeSeriesAfterRead( CommandProcessor processor, Command command, TS ts ) {
    List<TS> tslist = new ArrayList<>();
    tslist.add ( ts );
    return processTimeSeriesListAfterRead ( processor, command, tslist );
}

/**
Process a list of time series after reading.  This calls the command processor readTimeSeries2() method.
Command status messages will be added if problems arise but exceptions are not thrown.
*/
public static int processTimeSeriesListAfterRead( CommandProcessor processor, Command command, List<TS> tslist ) {
    int log_level = 3;
    int warning_count = 0;
    String routine = TSCommandProcessorUtil.class.getSimpleName() + ".processTimeSeriesListAfterRead";
    PropList request_params = new PropList ( "" );
    request_params.setUsingObject ( "TSList", tslist );
    CommandStatus status = null;
    if ( command instanceof CommandStatusProvider ) {
        status = ((CommandStatusProvider)command).getCommandStatus();
    }
    try {
        processor.processRequest( "ReadTimeSeries2", request_params);
    }
    catch ( Exception e ) {
        String message = "Error post-processing time series after read using ReadTimeSeries2 processor request.";
        Message.printWarning(log_level, routine, e);
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
        ++warning_count;
    }
    return warning_count;
}

/**
Validate command parameter names and generate standard feedback.
A list of allowed parameter names is provided.
If a name is not recognized, it is removed so as to prevent the user from continuing.
@param validList List of valid parameter names (others will be flagged as invalid).
@param command The command being checked.
@param warning A warning String that is receiving warning messages, for logging.
It will be appended to if there are more issues.
@return the warning string, longer if invalid parameters are detected.
*/
public static String validateParameterNames ( List<String> validList, Command command, String warning ) {
	if ( command == null ) {
		return warning;
	}
	PropList parameters = command.getCommandParameters();
	List<String> warningList = null;
	try {
	    // Validate the properties and discard any that are invalid (a message will be generated)
	    // and will be displayed once.
	    warningList = parameters.validatePropNames (	validList, null, null, "parameter", true );
	}
	catch ( Exception e ) {
		// Ignore.  Should not happen but print out just in case.
		warningList = null;
		Message.printWarning ( 3, "TSCommandProcessorUtil.validateParameterNames",
		    "Error checking parameter names (" + e + ")." );
		Message.printWarning ( 3, "TSCommandProcessorUtil.validateParameterNames", e );
	}
	if ( (warningList != null) && (warningList.size() > 0) ) {
		int size = warningList.size();
		StringBuffer b = new StringBuffer();
		for ( int i = 0; i < size; i++ ) {
			warning += "\n" + warningList.get (i);
			b.append ( warningList.get(i));
		}
		if ( command instanceof CommandStatusProvider ) {
			CommandStatus status = ((CommandStatusProvider)command).getCommandStatus();
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.WARNING, b.toString(),
					"Specify only valid parameters - see documentation."));
		}
	}
	return warning; // Return the original warning string with additional warnings if generated
}

}