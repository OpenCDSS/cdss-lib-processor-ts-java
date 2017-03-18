package rti.tscommandprocessor.commands.ts;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;

import DWR.StateMod.StateMod_TS;

import RTi.TS.MonthTS;
import RTi.TS.TSLimits;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Math.MathUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.TimeUtil;
import RTi.Util.Time.YearType;
import RTi.TS.TSIdent;
import RTi.TS.StringMonthTS;
import RTi.TS.TS;
import RTi.TS.TSUtil;

public class AnalyzePattern_Command extends AbstractCommand	implements Command, FileGenerator,
CommandDiscoverable, ObjectListProvider
{

// Defines used by this class and its analyzePattern_Dialog counterpart.
protected final String _ANALYSIS_PERCENTILE = "Percentile";

protected final String _False = "False";
protected final String _True = "True";

/**
Array of percentiles that are the upper limits of the "bins".  A value of 1.0 is ensured at the end.
*/
double [] __percentileArray = null;
/**
The user-specified pattern identifiers.
*/
String [] __patternIDArray  = null;

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
The table that is created.
*/
private DataTable __table = null;

/**
Command constructor.
*/
public AnalyzePattern_Command ()
{
	super ();
	setCommandName ( "AnalyzePattern" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String warning = "";
    String message;
	
	String TSList = parameters.getValue ( "TSList"     );
	String TSID = parameters.getValue ( "TSID"       );
	// TODO SAM 2007-02-17 Need to enable Method
	//String Method = parameters.getValue ( "Method"     );
	String Percentile = parameters.getValue ( "Percentile" );
	String PatternID = parameters.getValue ( "PatternID"  );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String Legacy = parameters.getValue ( "Legacy" );
	
	// Get the working_dir from the command processor
	String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
	
	try { Object o = processor.getPropContents ( "WorkingDir" );
		// Working directory is available so use it...
		if ( o != null ) {
			working_dir = (String)o;
		}
	}
	catch ( Exception e ) {
        message = "Error requesting WorkingDir from processor.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Software error - report problem to support." ) );
	}
	
    if ( (TSList != null) && !TSListType.ALL_MATCHING_TSID.equals(TSList) &&
        !TSListType.FIRST_MATCHING_TSID.equals(TSList) &&
        !TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        if ( TSID != null ) {
            message = "TSID should only be specified when TSList=" +
            TSListType.ALL_MATCHING_TSID.toString() + " or " +
            TSListType.FIRST_MATCHING_TSID.toString() + " or " +
            TSListType.LAST_MATCHING_TSID.toString() + ".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Do not specify the TSID parameter." ) );
        }
    }
	
	// Make sure one or more time series are selected when AllMatchingTSID is selected.
	if ( TSList.equalsIgnoreCase ( TSListType.ALL_MATCHING_TSID.toString() ) ) {
		if ( TSID != null ) {
			List<String> selectedV = StringUtil.breakStringList (TSID, ",",StringUtil.DELIM_SKIP_BLANKS );
			if ( (selectedV == null) || (selectedV.size() == 0) ) {
                message = "TSID should should not be empty when TSList=" + TSListType.ALL_MATCHING_TSID + " is specified.";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a TSID." ) );
			}
		}
        else {
            message = "TSID should should not be empty when TSList=" + TSListType.ALL_MATCHING_TSID + " is specified.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a TSID." ) );
		}
	}

	// Make sure Percentile list is given and the values are valid.
	//     0.25, 0.75          Acceptable input values example.
	// In this example there will be three classes:
	// 	1st - From 0.00 to 0.25
	//	2nd - From 0.25 to 0.75
	// 	3nd - From 0.75 to 1
	// i.e if 2 values are given for the percentiles, three values are
	// needed for the patternID, one for each of the three classes
	// mentioned above.
	int percentileCount = 0;
	if ( Percentile == null || Percentile.equals("") ) {
        message = "A list of percentile values is required.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                 new CommandLogRecord(CommandStatusType.FAILURE,
                         message, "Specify a list of percentile values." ) );
        
	}
    else {
    	List<String> percentileV = StringUtil.breakStringList ( Percentile, ",",StringUtil.DELIM_SKIP_BLANKS );
		if ( percentileV == null || percentileV.size()<= 0 ) {
            message = "Error parsing Percentile list \"" + Percentile + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                     new CommandLogRecord(CommandStatusType.FAILURE,
                             message, "Verify that the list of percentile values is numbers separated by commas." ) );
		}
        else {
			percentileCount = percentileV.size();
			__percentileArray = new double [percentileCount];
			String percentileStr;
			double previousP = 0, currentP;
			for ( int n = 0; n < percentileCount; n++ ) {
				percentileStr =(String)percentileV.get(n);
				if ( !StringUtil.isDouble( percentileStr ) ) {
                    message = "Percentile \"" + percentileStr + "\" is not a number.";
					warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                             new CommandLogRecord(CommandStatusType.FAILURE,
                                     message, "Specify the percentile value as a number." ) );
				}
				// Percentile must be between 0 and 1.
				currentP = Double.parseDouble( percentileStr );
				if ( currentP <= 0 || currentP >= 1 ) {
                    message = "Percentile \"" + percentileStr + "\" is not between (0 and 1).";
					warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Specify the percentile value as a number between 0 and 1." ) );
				}
				// Current Percentile should always be < than
				// the previous one
				if ( currentP <= previousP ) {
                    message = "Percentile \"" + percentileStr + "\" must be greater than the previous Percentile \""
                    + previousP + "\".";
					warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Verify that the percentile values are in ascending order." ) );
				}
				__percentileArray[n] = currentP;
				previousP = currentP;
			}
		}
	}

	// Make sure the PatternID list is given and the values are valid.
	// The number of PatternID must be plus one in addition to the
	// number of percentiles.
	int patternCount= 0; 
	if ( PatternID == null || PatternID.equals("") && !StringUtil.isInteger(PatternID) ) {
        message = "The pattern list is required.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a list of pattern values." ) );
	}
    else {
    	List<String> patternV = StringUtil.breakStringList ( PatternID, ",",StringUtil.DELIM_SKIP_BLANKS );
		if ( patternV == null || patternV.size() <= 0 ) {
            message = "Error parsing PatternID list \"" + PatternID + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the list of pattern values contains strings separated by commas." ) );
            
		}
        else {
			patternCount = patternV.size();
			if ( ( patternCount - percentileCount ) != 1 ) {
                message = "The PatternID count is \"" + patternCount +
                "\". It must be equal to \"" + (percentileCount + 1)
                    + "\" (Percentile count + 1).";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the number of pattern values is one greater than the number of percentile values." ) );
                
			}
		}
		__patternIDArray = new String [ patternCount ];
		for ( int n = 0; n < patternCount; n++ ) {
			String pattern = (String) patternV.get(n);
			__patternIDArray[n] = pattern;
		}
	}
	
	// Output file
	if ( OutputFile == null || OutputFile.length() == 0 ) {
        message = "The OutputFile is required but has not been specified.";
		warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an output file for results." ) );
	}
    else {	
		try {
			String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath ( working_dir, OutputFile));
			File f  = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
                message = "The parent directory does not exist for the output file: \"" + adjusted_path + "\".";
				warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Create a folder for the output file." ) );
			}
			f  = null;
			f2 = null;
		}
		catch ( Exception e ) {
            message = "The output file \"" + OutputFile + "\" cannot be adjusted using the working directory \"" +
            working_dir + "\".";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify that the output file path and working directory are compatible." ) );
		}
	}
	
   if ( Legacy!=null && Legacy.length()>0 && !Legacy.equalsIgnoreCase(_False)&&
        !Legacy.equalsIgnoreCase(_True )){
        message = "The Legacy (" + Legacy + ") parameter is invalid.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are " + _False
                    + " (default) and " + _True + "."));
    }
	
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(10);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "Method" );
    validList.add ( "Percentile" );
    validList.add ( "PatternID" );
    validList.add ( "OutputFile" );
    validList.add ( "TableID" );
    validList.add ( "DataRow" );
    validList.add ( "Legacy" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	// Throw an InvalidCommandParameterException in case of errors.
	if ( warning.length() > 0 ) {		
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, warning_level ),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Create the statistics table.
@param TableID identifier for table to create.
@param percentileArray array of percentile cutoffs (e.g., .25, .75).
@param tslist list of time series being processed
@param tsLabelFormat the format for the row labels, for each time series.
@param tsPercentiles the last values placed in a percentile "bin", by time series, month, and percentile
*/
private DataTable createStatisticsTable ( String TableID, double [] percentileArray, List<TS> tslist,
    String tsLabelFormatReq, double [][][] tsPercentiles )
throws Exception
{
    List<TableField> columnList = new ArrayList<TableField>();
    DataTable table = null;
    StringBuffer tsLabelFormat = new StringBuffer();
    
    // Create the table with column data
    // First column is the time series identifiers
    columnList.add ( new TableField(TableField.DATA_TYPE_STRING, "Time Series",30) );
    for ( int month = 1; month <= 12; month++ ) {
        // List of percentiles in output is one less than the pattern identifiers
        for ( int i = 0; i < percentileArray.length; i++ ) {
            if ( i == (percentileArray.length - 1) ) {
                columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE,
                        TimeUtil.monthAbbreviation(month) + " first value > " + percentileArray[i], 12, 2) );
            }
            else {
                columnList.add ( new TableField(TableField.DATA_TYPE_DOUBLE,
                        TimeUtil.monthAbbreviation(month) + " last value < " + percentileArray[i], 12, 2) );
            }
        }
    }
    table = new DataTable( columnList );
    table.setTableID ( TableID );
    // Add rows to the table for every time series
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    TS ts;
    String loc, dataType, units;
    for ( int i = 0; i < size; i++ ) {
        tsLabelFormat.setLength(0);
        ts = tslist.get(i);
        if ( (tsLabelFormatReq == null) || tsLabelFormatReq.equals("") ) {
            // Determine default format
            loc = ts.getLocation();
            dataType = ts.getDataType();
            units = ts.getDataUnits();
            if ( !loc.equals("") ) {
                tsLabelFormat.append ( "%L" );
            }
            if ( !dataType.equals("") ) {
                tsLabelFormat.append ( ", %T" );
            }
            if ( !units.equals("") ) {
                tsLabelFormat.append ( " (%U)" );
            }
        }
        else {
            tsLabelFormat.append(tsLabelFormatReq);
        }
        table.setFieldValue(i, 0, ts.formatExtendedLegend(tsLabelFormat.toString()), true );
        int colCount = 1;
        for ( int month = 1; month <= 12; month++ ) {
            for ( int j = 0; j < percentileArray.length; j++ ) {
                table.setFieldValue(i, colCount, new Double(tsPercentiles[i][month - 1][j]), true );
                ++colCount;
            }
        }
    }
    return table;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	
	// The command will be modified if changed...
	return ( new AnalyzePattern_JDialog ( parent, this ) ).ok();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	super.finalize ();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new Vector<File>();
    if ( getOutputFile() != null ) {
        list.add ( getOutputFile() );
    }
    return list;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector<DataTable>();
        v.add ( table );
    }
    return v;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
    return __OutputFile_File;
}

//parseCommand is base class

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws CommandWarningException, CommandException
{	String routine = "analyzePattern_Command.runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;		// Level for non-user messages
	int dl = 2; // Debug level - for now be verbose
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);
	
	CommandProcessor processor = getCommandProcessor();
	PropList parameters = getCommandParameters();

	// Make sure there are time series available to operate on...
	String TSList = parameters.getValue ( "TSList" );
 	String TSID = parameters.getValue ( "TSID"   );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String TableID = parameters.getValue ( "TableID" );
    String DataRow = parameters.getValue ( "DataRow" );
	
	// Get the method. Currently only Percentile method is supported.
	String Method = parameters.getValue ( "Method" );
	if ( Method == null || Method.equals("") ) {
		Method = "Percentile";
	}

	// Percentile list
	// The percentileArray[] is populated while checking the Command
	// Parameters (checkCommandParameters() method)
	
	// Pattern list
	// The patternIDArray[] is populated while checking the Command
	// Parameters (checkCommandParameters() method)
	
	// OutputFile
	String OutputFile = parameters.getValue ("OutputFile");
	
    String Legacy = parameters.getValue ( "Legacy" );
    if ( Legacy == null || Legacy.equals("") ) {
        Legacy = _False;
    }
    boolean Legacy_boolean = false;
    if ( Legacy.equalsIgnoreCase(_True) ) {
        Legacy_boolean = true;
    }
	
	if ( warning_count > 0 ) {
		// Input error (e.g., missing time series)...
		message = "Command parameter data has errors.  Unable to run command.";
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(
		command_tag,++warning_count), routine, message );
		throw new CommandException ( message );
	}

    try {
        // Clear the output file
        setOutputFile ( null );
        if ( commandPhase == CommandPhaseType.RUN ) {
            // Get the time series to process...
            
            PropList request_params = new PropList ( "" );
            request_params.set ( "TSList", TSList );
            request_params.set ( "TSID", TSID );
            request_params.set ( "EnsembleID", EnsembleID );
            CommandProcessorRequestResultsBean bean = null;
            try { bean =
                processor.processRequest( "GetTimeSeriesToProcess", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
                "\", TSID=\"" + TSID + "\") from processor.";
                Message.printWarning(log_level,
                        MessageUtil.formatMessageTag( command_tag, ++warning_count),
                        routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
            List<TS> tslist = null;
            if ( o_TSList == null ) {
                message = "Unable to find time series to process using TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\".";
                Message.printWarning ( log_level,
                MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
            }
            else {
            	@SuppressWarnings("unchecked")
				List<TS> tslist0 = (List<TS>)o_TSList;
                tslist = tslist0;
                if ( tslist.size() == 0 ) {
                    message = "Unable to find time series to process using TSList=\"" + TSList +
                    "\" TSID=\"" + TSID + "\".";
                    Message.printWarning ( log_level,
                            MessageUtil.formatMessageTag(
                                    command_tag,++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.WARNING,
                                    message, "Verify the TSID (pattern) - may be OK if a partial run." ) );
                }
            }
            int [] tspos = null;
            Object o_Indices = bean_PropList.getContents ( "Indices" );
            if ( o_Indices == null ) {
                message = "Unable to find positions for time series to process using TSList=\"" + TSList +
                "\" TSID=\"" + TSID + "\".";
                Message.printWarning ( log_level,
                MessageUtil.formatMessageTag(
                command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Report the problem to software support." ) );
            }
            else {
                tspos = (int [])o_Indices;
                if ( tspos.length == 0 ) {
                    message = "Unable to find positions for time series to process using TSList=\"" + TSList +
                    "\" TSID=\"" + TSID + "\".";
                    Message.printWarning ( log_level,
                            MessageUtil.formatMessageTag(
                                    command_tag,++warning_count), routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
                }
            }
            
            int tsCount = tslist.size();
            if ( tsCount == 0 ) {
                message = "No time series were found to processing using TSList=\"" + TSList + "\" TSID=\"" + TSID + "\".";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(
                        command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Verify how the list of time series is specified - may be OK if a partial run." ) );
            }
            
        	// Get the start and end dates among the time series.
        	DateTime startDate = null;
        	DateTime endDate   = null;
        	TSLimits limits = new TSLimits ();
        	try {	
        		limits = TSUtil.getPeriodFromTS ( tslist, TSUtil.MAX_POR );
        	} catch ( Exception e ) {
        	}
        	startDate = new DateTime ( limits.getDate1() );
        	endDate = new DateTime ( limits.getDate2() );
        
        	// Monthly Time series
        	int smBase = TimeInterval.MONTH;
        	int smMult = 1;
        	
        	TS analysisTS;
            String OutputFile_full = OutputFile;
    		// Looping through each time series creating a new StringMonthTS
    		// And keep the StringMonthTS in a vector of time series
    		List<StringMonthTS> StringMonthTS_List = new ArrayList<StringMonthTS>(tsCount);
    		StringMonthTS stringMonthTS = null;
    		// Array to hold cutoff values for each bin, for outputting statistics to the table
    		// Each time series has monthly values.  The values are the last values put into a bin,
    		// except that the last statistic value is the first value in the bin.  This allows other
    		// applications to bracket the percentiles.
    		double [][][] tsPercentiles = new double[tsCount][12][__percentileArray.length];
    		for ( int nTS = 0; nTS < tsCount; nTS++ ) {
    			
    			// Get the time series object.
    			analysisTS = null;
    			request_params = new PropList ( "" );
    			request_params.setUsingObject ( "Index", new Integer(tspos[nTS]) );
    			bean = null;
    			try {
    			    bean = processor.processRequest( "GetTimeSeries", request_params);
    			}
    			catch ( Exception e ) {
                    message = "Error requesting GetTimeSeries(Index=" + tspos[nTS] + ") from processor.";
    				Message.printWarning(log_level,
    						MessageUtil.formatMessageTag( command_tag, ++warning_count),
    						routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
    			}
    			bean_PropList = bean.getResultsPropList();
    			Object prop_contents = bean_PropList.getContents ( "TS" );
    			if ( prop_contents == null ) {
                    message = "Null value for GetTimeSeries(Index=" + tspos[nTS] +
                    ") returned from processor - skipping.";
    				Message.printWarning(warning_level,
    					MessageUtil.formatMessageTag( command_tag, ++warning_count),
    					routine, message );
                    status.addToLog ( CommandPhaseType.RUN,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                                    message, "Report the problem to software support." ) );
    					// Skip the time series...
    					continue;
    			}
    			else {
    			    analysisTS = (TS)prop_contents;
    			}
    		    notifyCommandProgressListeners ( nTS, tsCount, (float)-1.0, "Analyzing pattern for " +
    		        analysisTS.getIdentifier().toStringAliasAndTSID() );
                
                if ( !(analysisTS instanceof MonthTS) ) {
                    message = "Time series does not monthly data (skipping):  " + analysisTS.getIdentifierString();
                    Message.printWarning(warning_level,
                            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, message );
                        status.addToLog ( CommandPhaseType.RUN,
                                new CommandLogRecord(CommandStatusType.FAILURE,
                                        message, "Verify that the TSList parameter specifies only monthly time series." ) );
                        continue;
                }
    
    		   	// Create stringMonthTS to receive the analysis results
    			TSIdent smIdent  = new TSIdent(	analysisTS.getIdentifier() );
    			smIdent.setType ( "Pattern");
    			smIdent.setInterval ( smBase, smMult );
    
    			// Create the new time series using the new identifier.
    			stringMonthTS = new StringMonthTS(smIdent.getIdentifier(), startDate, endDate );
    			// Update the stringMonth time series properties with
    			// information from the input analysis time series.
    			// Notice: CopyHeader() overwrites, among several other
    			//	   things, the Identifier, the DataInterval
    			//	   (Base and Multiplier).
    			//         It also set the dates, from the old time
    			//	   series. Make sure to reset these properties
    			//	   to the values needed by the new time series.
    			stringMonthTS.copyHeader ( analysisTS );
    		// TODO What dataType?			
    		//	stringMonthTS.setDataType ( NewDataType );
    			stringMonthTS.setIdentifier ( smIdent  );
    			stringMonthTS.setDataInterval ( smBase, smMult );
    			// Finally allocate data space.
    			stringMonthTS.allocateDataSpace	();
    
    			// Get the actual startDate end the endDate for this
    			// time series. These may different from the dates used to create the stringMonthTS.
    			DateTime _startDate = new DateTime( analysisTS.getDate1() );
    			DateTime _endDate   = new DateTime( analysisTS.getDate2() );
    			double [] values = null;
    			int [] sortedOrder = null;
    			int nValues;
    			double bigNegative = -1.0e50;
    
    			for ( int month = 1; month <= 12; month++ ) {
    			    // Data values include missing values because the year position in the array must
    			    // be maintained in order to process below.  Missing values of -999 and NaN will sort to
    			    // the front of the list and can be skipped over
    			    //
    			    // DO NOT get only the non-missing values because the sort position is used to determine
    			    // the year in order to properly determine the date for setting values below.
    			    // TODO SAM 2009-10-28 May be issues with some missing values not sorting to the front.
    				values = TSUtil.toArrayByMonth ( analysisTS, _startDate, _endDate, month);
    				// Reset missing values to have very small value so that they sort to the front.  This
    				// ensures that negative values don't get interpreted incorrectly.
    				for ( int i = 0; i < values.length; i++ ) {
    				    if ( analysisTS.isDataMissing(values[i]) ) {
    				        values[i] = bigNegative;
    				    }
    				}
    
    				// Size of the returning array.
    				nValues = values.length;
    				sortedOrder = new int[nValues];
    				MathUtil.sort ( values, MathUtil.SORT_QUICK, MathUtil.SORT_ASCENDING, sortedOrder, true );
    
    				// Find the first year, to use as offset.
    				int yearOffset = _startDate.getYear();
    				if ( _startDate.getMonth() > month ) {
    					yearOffset += 1;
    				}
    
    				// Loop through the sorted data until the first non-missing is found. Then set the counters,
    				// the currentUpperLimit, the currentPatternID and break out.
    				int missingCount = 0;
    				int non_missingCount = 0;
    				double currentUpperLimit = -999.9;
    				String currentPatternID = "";
    				int percentileIndex = 0;
    				boolean found = false;
    				if ( Message.isDebugOn ) {
    				    Message.printDebug( dl, routine, analysisTS.getIdentifierString() +
    				        " sorted " + TimeUtil.monthAbbreviation(month)+
    				        " values with missing at front (using to find non-mising values)..." );
    				}
    				for( int n=0; n<nValues && found==false; n++ ) {
    					// Build the dateTime
    					DateTime _date = new DateTime(DateTime.PRECISION_MONTH);
    					_date.setMonth( month );
    					_date.setYear( yearOffset +	sortedOrder[n] );
    
    					if ( analysisTS.isDataMissing(analysisTS.getDataValue(_date) ) ) {
					    	if ( Message.isDebugOn ) {
					    		message = "(nTotal=" + (n + 1) + ") " + _date + " " + analysisTS.getDataValue(_date);
    							Message.printDebug(	dl, routine, message );
    						}
    						
    						continue;
    					}
                        else {
    						missingCount = n;
    						non_missingCount = nValues-n;
    						currentUpperLimit =	missingCount + non_missingCount *__percentileArray[percentileIndex ];
    						currentPatternID = __patternIDArray[percentileIndex ];
    						found = true;
    					}
    				}
    				
    				if ( Message.isDebugOn ) {
    					message = "missing: " + missingCount + " non-missing: " + non_missingCount +
    					    ", nTotal cutoff=" + currentUpperLimit + ", nNonMissing cutoff=" +
    					    (currentUpperLimit - missingCount);
    					Message.printDebug (2, routine, message );
    				}
    
    				// Populate the stringMonthTS time series based
    				// on the sorted values and using the petternIDs
    				if ( Message.isDebugOn ) {
    				    Message.printDebug( dl, routine, "Sorted " + TimeUtil.monthAbbreviation(month)+
    				        " values analyzed for pattern..." );
    				}
    				for( int n = missingCount; n<nValues; n++ ){
    					// Build the dateTime
    					DateTime date = new DateTime(DateTime.PRECISION_MONTH);
    					date.setMonth( month );
    					date.setYear( yearOffset +	sortedOrder[n] );
    
    					// If beyond the current limits, compute the next
    					// FIXME SAM 2009-10-28 Evaluate whether need to use n + 1 because n is zero-referenced
    					//Legacy code... (wrong because of zero offset?)
    					// For now use n + 1 and allow command parameter to duplicate legacy behavior
    					int check = n + 1;
    					if ( Legacy_boolean ) {
    					    check = n;
    					}
    					if ( check >= currentUpperLimit ) {
    					    // Save the bin values for the statistic.
    					    if ( percentileIndex == __percentileArray.length - 1 ) {
                                // Last bin so save the first value
                                tsPercentiles[nTS][month - 1][percentileIndex] = values[n];
    					    }
    					    else if ( percentileIndex < __percentileArray.length - 1) {
                                // Save the last value in the bin
                                tsPercentiles[nTS][month - 1][percentileIndex] = values[n - 1];
    					    }
    					    // Increment because it is used for the pattern ID in the last bin (and have one
    					    // more patterns than percentiles, but don't need to increment the cutoff
    					    percentileIndex += 1;
    					    if ( percentileIndex == __percentileArray.length ) {
    					        // Artificial upper bound (was not set by command parameter)
    					        currentUpperLimit = nValues + 1;
    					    }
    					    else if ( percentileIndex < __percentileArray.length ){
    					        currentUpperLimit = missingCount + non_missingCount * __percentileArray[percentileIndex];
    					    }
    						currentPatternID = __patternIDArray[percentileIndex];
    						if ( Message.isDebugOn ) {
    	                        Message.printDebug( dl, routine, "nTotal=" + n + ", new nTotal cutoff= " +
    	                            currentUpperLimit + ", nNonMissing cutoff=" +
    	                            (currentUpperLimit - missingCount) + ", percentileIndex=" + percentileIndex);
    	                    }
    					}
    
    					// Set the date and value in the TS
    					stringMonthTS.setDataValue (date, currentPatternID );
    					
    					if ( Message.isDebugOn ) {
    						message = "(nTotal=" + (n + 1) + ", nNonMissing=" + (n + 1 - missingCount) +
    						") " + date + " " + analysisTS.getDataValue ( date )
    		   				+ " " + stringMonthTS.getDataValueAsString ( date );
    						Message.printDebug (dl, routine, message );
    					}
    				}
    			}
    			// Add this time series to the list.
    			StringMonthTS_List.add( stringMonthTS );
    		}
    
    		// Saving results in the output file.
    		PropList props = new PropList ( "AnalyzePattern" );
    		YearType outputYearType = YearType.CALENDAR; // Default
    	   	try {
    	   	    Object o = processor.getPropContents ( "OutputYearType" );
    	   		if ( o != null ) {
    	   			outputYearType = (YearType)o; // Default to whatever is coming in
    	   		}
    	   	}
    	   	catch ( Exception e ) {
    	   		// Not fatal, but of use to developers.
    	   	    message = "Error requesting OutputYearType from processor - using calendar (" + e + ").";
    	   	    status.addToLog ( CommandPhaseType.RUN,
                       new CommandLogRecord(CommandStatusType.WARNING,
                               message, "Report the problem to software support." ) );
    	   	}
            props.add ( "CalendarType=" + outputYearType );
                    
            // Write the results file using the available data, or the global
            // output period if it was specified.
                    
            DateTime OutputStart_DateTime = startDate;
            DateTime OutputEnd_DateTime = endDate;
            
        	try {
                Object o = processor.getPropContents ( "OutputStart" );
    			if ( o != null ) {
    				OutputStart_DateTime = (DateTime)o;
    			}
        	}
        	catch ( Exception e ) {
        		// Not fatal, but of use to developers.
        		message = "Error requesting OutputStart from processor.";
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Report the problem to software support." ) );
        	}
        	try {
        	    Object o = processor.getPropContents ( "OutputEnd" );
    			if ( o != null ) {
    				OutputEnd_DateTime = (DateTime)o;
    			}
        	}
        	catch ( Exception e ) {
        		// Not fatal, but of use to developers.
        		message = "Error requesting OutputEnd from processor - not using.";
                status.addToLog ( CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.WARNING,
                                message, "Report the problem to software support." ) );
        	}
            // Get the comments to add to the top of the file.

            List<String> outputCommentsList = null;
            try {
                Object o = processor.getPropContents ( "OutputComments" );
                // Comments are available so use them...
                if ( o != null ) {
                	@SuppressWarnings("unchecked")
					List<String> outputCommentsList0 = (List<String>)o;
                    outputCommentsList = outputCommentsList0;
                }
            }
            catch ( Exception e ) {
                // Not fatal, but of use to developers.
                message = "Error requesting OutputComments from processor - not using.";
                Message.printDebug(10, routine, message );
            }
            OutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),OutputFile));
    		StateMod_TS.writePatternTimeSeriesList(OutputFile_full, outputCommentsList,
				StringMonthTS_List, OutputStart_DateTime, OutputEnd_DateTime, props );
    		// Save the output file name...
            setOutputFile ( new File(OutputFile_full));
            
            if ( (TableID != null) && !TableID.equals("") ) {
                DataTable table = createStatisticsTable ( TableID, __percentileArray, tslist, DataRow,
                    tsPercentiles );
                
                // Set the table in the processor...
                
                request_params = new PropList ( "" );
                request_params.setUsingObject ( "Table", table );
                try {
                    processor.processRequest( "SetTable", request_params);
                }
                catch ( Exception e ) {
                    message = "Error requesting SetTable(Table=...) from processor (" + e + ").";
                    Message.printWarning(warning_level,
                            MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            routine, message );
                    status.addToLog ( commandPhase,
                            new CommandLogRecord(CommandStatusType.FAILURE,
                               message, "Report problem to software support." ) );
                }
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            // Create an empty table and set the ID
            if ( (TableID != null) && !TableID.equals("") ) {
                DataTable table = new DataTable();
                table.setTableID ( TableID );
                setDiscoveryTable ( table );
            }
        }
	} catch ( Exception e ) {
		Message.printWarning ( log_level, routine, e );
		message = "Unexpected error performing the pattern analysis (" + e + ").";
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Report the problem to software support - see the log file for details." ) );
		throw new CommandWarningException ( message);
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
    __OutputFile_File = file;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{
	if ( props == null ) {
		return getCommandName() + "()";
	}

	// Get the properties
	String TSList = props.getValue ( "TSList" );
	String TSID = props.getValue ( "TSID" );
	String EnsembleID = props.getValue ( "EnsembleID" );
	String Method = props.getValue ( "Method" );
	String Percentile = props.getValue ( "Percentile");
	String PatternID = props.getValue ( "PatternID" );
	String OutputFile = props.getValue ( "OutputFile");
    String TableID = props.getValue ( "TableID");
    String DataRow = props.getValue ( "DataRow");
    String Legacy = props.getValue ( "Legacy");

	// Creating the command string
	// This StringBuffer will contain all parameters for the command.
	StringBuffer b = new StringBuffer();
	if ( TSList != null && TSList.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "TSList=" + TSList );
	}
	if ( TSID != null && TSID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "TSID=\"" + TSID + "\"" );
	}
    if ( EnsembleID != null && EnsembleID.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
	if ( Method != null && Method.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "Method=" + Method );
	}
	if ( Percentile != null && Percentile.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "Percentile=\"" + Percentile + "\"" );
	}
	if ( PatternID != null && PatternID.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "PatternID=\"" + PatternID + "\""  );
	}
	if ( OutputFile != null && OutputFile.length() > 0 ) {
		if ( b.length() > 0 ) b.append ( "," );
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
    if ( TableID != null && TableID.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( DataRow != null && DataRow.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "DataRow=\"" + DataRow + "\"" );
    }
    if ( Legacy != null && Legacy.length() > 0 ) {
        if ( b.length() > 0 ) b.append ( "," );
        b.append ( "Legacy=\"" + Legacy + "\"" );
    }
	
	return getCommandName() + "(" + b.toString() + ")";
}

}