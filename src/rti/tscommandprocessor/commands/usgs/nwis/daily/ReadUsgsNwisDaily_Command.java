package rti.tscommandprocessor.commands.usgs.nwis.daily;

import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.TS;

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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the ReadUsgsNwisDaily() command.
*/
public class ReadUsgsNwisDaily_Command extends AbstractCommand
implements Command, CommandDiscoverable, ObjectListProvider, FileGenerator
{

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discovery_TS_Vector = null;

/**
Bounding box coordinate WestLon, SouthLat, EastLon, NorthLat
*/
double [] __boundingBox = null;

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public ReadUsgsNwisDaily_Command ()
{	super();
	setCommandName ( "ReadUsgsNwisDaily" );
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
    
    String DataStore = parameters.getValue ( "DataStore" );
    String Sites = parameters.getValue ( "Sites" );
    String States = parameters.getValue ( "States" );
    String HUCs = parameters.getValue ( "HUCs" );
    String BoundingBox = parameters.getValue ( "BoundingBox" );
    String Counties = parameters.getValue ( "Counties" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );
    String OutputFile = parameters.getValue ( "OutputFile" );

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || DataStore.equals("") ) {
        message = "The data store must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store." ) );
    }

    int locCount = 0; // Count of location parameters (only 1 allowed)
    if ( (Sites != null) && !Sites.equals("") ) {
        ++locCount;
    }
    if ( (States != null) && !States.equals("") ) {
        ++locCount;
    }
    if ( (HUCs != null) && !HUCs.equals("") ) {
        ++locCount;
    }
    if ( (BoundingBox != null) && !BoundingBox.equals("") ) {
        // Make sure that 4 numbers are specified
        ++locCount;
        String [] parts = BoundingBox.split(",");
        if ( parts == null ) {
            message = "The bounding box (" + BoundingBox + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the bounding box as WestLonDeg,SouthLatDeg,EastLonDeg,NorthLatDeg." ) );
        }
        else {
            if ( parts.length != 4 ) {
                message = "The bounding box (" + BoundingBox + ") is invalid.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the bounding box as WestLonDeg,SouthLatDeg,EastLonDeg,NorthLatDeg." ) );
            }
            for ( int i = 0; i < parts.length; i++ ) {
                try {
                    __boundingBox[i] = Double.parseDouble(parts[i].trim());
                }
                catch ( NumberFormatException e ) {
                    message = "The bounding box (" + BoundingBox + ") part " + (i + 1) + " is not a number.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the bounding box as WestLonDeg,SouthLatDeg,EastLonDeg,NorthLatDeg." ) );
                }
                if ( ((i == 0) || (i == 2)) && ((__boundingBox[i] < -180.0) || (__boundingBox[i] > 180.0)) ) {
                    message = "The bounding box longitude (" + __boundingBox[i] + ") is not in range -180 to 180.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the longitude in range -180 to 180." ) );
                }
                if ( ((i == 1) || (i == 3)) && ((__boundingBox[i] < -90.0) || (__boundingBox[i] > 90.0)) ) {
                    message = "The bounding box latitude (" + __boundingBox[i] + ") is not in range -90 to 90.";
                    warning += "\n" + message;
                    status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify the latitude in range -90 to 90." ) );
                }
            }
        }
    }
    if ( (Counties != null) && !Counties.equals("") ) {
        ++locCount;
    }
    if ( locCount != 1 ) {
        message = "Only one location constraint can be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify one location constraint." ) );
    }

	// TODO SAM 2006-04-24 Need to check the WhereN parameters.

	if ( (InputStart != null) && !InputStart.equals("") &&
		!InputStart.equalsIgnoreCase("InputStart") && !InputStart.equalsIgnoreCase("InputEnd") ) {
		try {
		    DateTime.parse(InputStart);
		}
		catch ( Exception e ) {
            message = "The input start date/time \"" + InputStart + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a date/time or InputStart." ) );
		}
	}
	if ( (InputEnd != null) && !InputEnd.equals("") &&
		!InputEnd.equalsIgnoreCase("InputStart") && !InputEnd.equalsIgnoreCase("InputEnd") ) {
		try {
		    DateTime.parse( InputEnd );
		}
		catch ( Exception e ) {
            message = "The input end date/time \"" + InputEnd + "\" is not a valid date/time.";
			warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a date/time or InputEnd." ) );
		}
	}
	
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file: \"" + OutputFile + "\" must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an output file." ) );
    }
    else {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                    TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does not exist for: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Create the output directory." ) );
            }
            f = null;
            f2 = null;
        }
        catch ( Exception e ) {
            message = "The output file:\n" +
            "    \"" + OutputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that output file and working directory paths are compatible." ) );
        }
    }

    // Check for invalid parameters...
    List<String> valid_Vector = new Vector();
    valid_Vector.add ( "DataStore" );
    valid_Vector.add ( "Sites" );
    valid_Vector.add ( "States" );
    valid_Vector.add ( "HUCs" );
    valid_Vector.add ( "BoundingBox" );
    valid_Vector.add ( "Counties" );
    valid_Vector.add ( "Parameters" );
    valid_Vector.add ( "Statistics" );
    valid_Vector.add ( "SiteStatus" );
    valid_Vector.add ( "SiteTypes" );
    valid_Vector.add ( "Agency" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    valid_Vector.add ( "Alias" );
    valid_Vector.add ( "Format" );
    valid_Vector.add ( "OutputFile" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{   // The command will be modified if changed...
    return (new ReadUsgsNwisDaily_JDialog ( parent, this )).ok();
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
    List<File> list = new Vector();
    if ( getOutputFile() != null ) {
        list.add ( getOutputFile() );
    }
    return list;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
	List<TS> discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
    TS datats = discovery_TS_Vector.get(0);
    // Also check the base class
    if ( (c == TS.class) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
    return __OutputFile_File;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   
    runCommandInternal ( command_number, CommandPhaseType.RUN );
}

/**
Run the command in discovery mode.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
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
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadUsgsNwisDaily_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(commandPhase);
    
    // Clear the output file
    
    setOutputFile ( null );
    
    boolean readData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        readData = false;
    }
    
    String dataStoreName = parameters.getValue("DataStore");
    String Sites = parameters.getValue("Sites");
    List<String> siteList = new Vector();
    if ( (Sites != null) && !Sites.equals("") ) {
        if ( Sites.indexOf(",") < 0 ) {
            siteList.add(Sites.trim());
        }
        else {
            String [] siteArray = Sites.split(",");
            for ( int i = 0; i < siteArray.length; i++ ) {
                siteList.add(siteArray[i].trim());
            }
        }
    }
    String States = parameters.getValue("States");
    List<String> stateList = new Vector();
    if ( (States != null) && !States.equals("") ) {
        if ( States.indexOf(",") < 0 ) {
            stateList.add(States.trim());
        }
        else {
            String [] stateArray = States.split(",");
            for ( int i = 0; i < stateArray.length; i++ ) {
                stateList.add(stateArray[i].trim());
            }
        }
    }
    String HUCs = parameters.getValue("HUCs");
    List<String> hucList = new Vector();
    if ( (HUCs != null) && !HUCs.equals("") ) {
        if ( HUCs.indexOf(",") < 0 ) {
            hucList.add(HUCs.trim());
        }
        else {
            String [] hucArray = HUCs.split(",");
            for ( int i = 0; i < hucArray.length; i++ ) {
                hucList.add(hucArray[i].trim());
            }
        }
    }
    String Counties = parameters.getValue("Counties");
    List<String> countyList = new Vector();
    if ( (Counties != null) && !Counties.equals("") ) {
        if ( Counties.indexOf(",") < 0 ) {
            countyList.add(Counties.trim());
        }
        else {
            String [] countyArray = Counties.split(",");
            for ( int i = 0; i < countyArray.length; i++ ) {
                countyList.add(countyArray[i].trim());
            }
        }
    }
    String Parameters = parameters.getValue("Parameters");
    List<UsgsNwisParameterType> parameterList = new Vector();
    if ( (Parameters != null) && !Parameters.equals("") ) {
        if ( Parameters.indexOf(",") < 0 ) {
            parameterList.add(new UsgsNwisParameterType(Parameters.trim(), "", "", "", "", ""));
        }
        else {
            String [] parameterArray = Parameters.split(",");
            for ( int i = 0; i < parameterArray.length; i++ ) {
                parameterList.add(new UsgsNwisParameterType(parameterArray[i].trim(), "", "", "", "", ""));
            }
        }
    }
    String Statistics = parameters.getValue("Statistics");
    List<UsgsNwisStatisticType> statisticList = new Vector();
    if ( (Statistics != null) && !Statistics.equals("") ) {
        if ( Statistics.indexOf(",") < 0 ) {
            statisticList.add(new UsgsNwisStatisticType(Statistics.trim(), "", ""));
        }
        else {
            String [] statisticArray = Statistics.split(",");
            for ( int i = 0; i < statisticArray.length; i++ ) {
                statisticList.add(new UsgsNwisStatisticType(statisticArray[i].trim(), "", ""));
            }
        }
    }
    String SiteStatus = parameters.getValue("SiteStatus");
    UsgsNwisSiteStatusType siteStatus = UsgsNwisSiteStatusType.valueOfIgnoreCase(SiteStatus);
    if ( siteStatus == null ) {
        siteStatus = UsgsNwisSiteStatusType.ALL;
    }
    String SiteTypes = parameters.getValue("SiteTypes");
    List<UsgsNwisSiteType> siteTypeList = new Vector();
    if ( (SiteTypes != null) && !SiteTypes.equals("") ) {
        if ( SiteTypes.indexOf(",") < 0 ) {
            siteTypeList.add(new UsgsNwisSiteType(SiteTypes.trim(), "", ""));
        }
        else {
            String [] siteTypeArray = SiteTypes.split(",");
            for ( int i = 0; i < siteTypeArray.length; i++ ) {
                siteTypeList.add(new UsgsNwisSiteType(siteTypeArray[i].trim(), "", ""));
            }
        }
    }
    String Agency = parameters.getValue("Agency");
    String Alias = parameters.getValue("Alias");
    String Format = parameters.getValue("Format");
    UsgsNwisFormatType format = UsgsNwisFormatType.valueOfIgnoreCase(Format);
    if ( format == null ) {
        format = UsgsNwisFormatType.WATERML;
    }
    String OutputFile = parameters.getValue("OutputFile");
    
	String InputStart = parameters.getValue ( "InputStart" );
	DateTime InputStart_DateTime = null;
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputStart );
		CommandProcessorRequestResultsBean bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + InputStart + ") from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                   message, "Report problem to software support." ) );
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + InputStart + "\") returned from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a valid InputStart string has been specified." ) );
		}
		else {
		    InputStart_DateTime = (DateTime)prop_contents;
		}
	}
	else {
	    // Get from the processor...
		try {
            Object o = processor.getPropContents ( "InputStart" );
			if ( o != null ) {
				InputStart_DateTime = (DateTime)o;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting InputStart from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
		}
	}
	String InputEnd = parameters.getValue ( "InputEnd" );
	DateTime InputEnd_DateTime = null;
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		PropList request_params = new PropList ( "" );
		request_params.set ( "DateTime", InputEnd );
		CommandProcessorRequestResultsBean bean = null;
		try {
            bean = processor.processRequest( "DateTime", request_params);
		}
		catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + InputEnd + ") from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
		}
		PropList bean_PropList = bean.getResultsPropList();
		Object prop_contents = bean_PropList.getContents ( "DateTime" );
		if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + InputEnd + ") returned from processor.";
			Message.printWarning(warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a valid InputEnd has been specified." ) );
		}
		else {
		    InputEnd_DateTime = (DateTime)prop_contents;
		}
	}
	else {
	    // Get from the processor...
		try { Object o = processor.getPropContents ( "InputEnd" );
			if ( o != null ) {
				InputEnd_DateTime = (DateTime)o;
			}
		}
		catch ( Exception e ) {
			// Not fatal, but of use to developers.
			message = "Error requesting InputEnd from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
                status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
		}
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	// Now try to read...

	List<TS> tslist = new Vector();	// List for time series results.
					// Will be added to for one time series
					// read or replaced if a list is read.
	try {
		// Find the data store to use...
		DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName (
		    dataStoreName, UsgsNwisDailyDataStore.class );
		if ( dataStore == null ) {
			message = "Could not get data store for name \"" + dataStoreName + "\" to query data.";
			Message.printWarning ( 2, routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the USGS NWIS daily web service has been configured with name \"" +
                    dataStoreName + "\" and is available." ) );
			throw new Exception ( message );
		}
		UsgsNwisDailyDataStore usgsNwisDailyDataStore = (UsgsNwisDailyDataStore)dataStore;

		String OutputFile_full = OutputFile;
		if ( (OutputFile != null) && !OutputFile.equals("") ) {
		    OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
		}
        tslist = usgsNwisDailyDataStore.readTimeSeriesList ( siteList, stateList,
            hucList, __boundingBox, countyList,
            parameterList, statisticList,
            siteStatus, siteTypeList, Agency,
            format, OutputFile_full,
            InputStart_DateTime, InputEnd_DateTime, readData );
		// Make sure that size is set...
		int size = 0;
		if ( tslist != null ) {
			size = tslist.size();
		}
	
   		if ( (tslist == null) || (size == 0) ) {
			Message.printStatus ( 2, routine,"No USGS NWIS daily time series were found." );
	        // Warn if nothing was retrieved (can be overridden to ignore).
            message = "No time series were read from the USGS NWIS web service.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database." +
                    	"  Previous messages may provide more information." ) );
   		}
   		else {
			// Else, further process each time series...
			for ( TS ts: tslist ) {
			    // Set the alias to the desired string - this is impacted by the Location parameter
                String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, ts, Alias, status, commandPhase);
                ts.setAlias ( alias );
			}
			
            // Save the output file name...
            setOutputFile ( new File(OutputFile_full));
		}
    
        Message.printStatus ( 2, routine, "Read " + size + " USGS NWIS daily time series." );

        if ( commandPhase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing USGS NWIS daily time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    // Don't throw an exception - probably due to missing data.
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding USGS NWIS daily time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( commandPhase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
        }
        else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            setDiscoveryTSList ( tslist );
        }
        // Warn if nothing was retrieved (can be overridden to ignore).
        if ( (tslist == null) || (size == 0) ) {
            message = "No time series were read from the USGS NWIS daily web service.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( commandPhase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database.  See previous messages." ) );
        }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from USGS NWIS daily web service (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
               message, "Report the problem to software support - also see the log file." ) );
		throw new CommandException ( message );
	}

	// Throw CommandWarningException in case of problems.
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(
				command_tag, ++warning_count ),
			routine, message );
		throw new CommandWarningException ( message );
	}
    
    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
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
{	StringBuffer b = new StringBuffer ();
	if ( props == null ) {
	    return getCommandName() + "()";
	}
    String DataStore = props.getValue("DataStore");
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    String Sites = props.getValue("Sites");
    if ( (Sites != null) && (Sites.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Sites=\"" + Sites + "\"" );
    }
	String States = props.getValue("States");
	if ( (States != null) && (States.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "States=\"" + States + "\"" );
	}
    String HUCs = props.getValue("HUCs");
    if ( (HUCs != null) && (HUCs.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "HUCs=\"" + HUCs + "\"" );
    }
    String BoundingBox = props.getValue("BoundingBox");
    if ( (BoundingBox != null) && (BoundingBox.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "BoundingBox=\"" + BoundingBox + "\"" );
    }
    String Counties = props.getValue("Counties");
    if ( (Counties != null) && (Counties.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Counties=\"" + Counties + "\"" );
    }
    String Parameters = props.getValue("Parameters");
    if ( (Parameters != null) && (Parameters.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Parameters=\"" + Parameters + "\"" );
    }
    String Statistics = props.getValue("Statistics");
    if ( (Statistics != null) && (Statistics.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Statistics=\"" + Statistics + "\"" );
    }
    String SiteStatus = props.getValue("SiteStatus");
    if ( (SiteStatus != null) && (SiteStatus.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SiteStatus=\"" + SiteStatus + "\"" );
    }
    String SiteTypes = props.getValue("SiteTypes");
    if ( (SiteTypes != null) && (SiteTypes.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SiteTypes=\"" + SiteTypes + "\"" );
    }
    String Agency = props.getValue("Agency");
    if ( (Agency != null) && (Agency.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Agency=\"" + Agency + "\"" );
    }
	String InputStart = props.getValue("InputStart");
	if ( (InputStart != null) && (InputStart.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputStart=\"" + InputStart + "\"" );
	}
	String InputEnd = props.getValue("InputEnd");
	if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputEnd=\"" + InputEnd + "\"" );
	}
    String Alias = props.getValue("Alias");
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
    String Format = props.getValue("Format");
    if ( (Format != null) && (Format.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Format=\"" + Format + "\"" );
    }
    String OutputFile = props.getValue("OutputFile");
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }

    return getCommandName() + "(" + b.toString() + ")";
}

}