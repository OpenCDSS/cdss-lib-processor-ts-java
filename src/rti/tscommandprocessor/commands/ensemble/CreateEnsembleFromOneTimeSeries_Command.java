// CreateEnsembleFromOneTimeSeries_Command - This class initializes, checks, and runs the CreateEnsembleFromOneTimeSeries() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2019 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.ensemble;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.TS;
import RTi.TS.TSEnsemble;
import RTi.TS.TSUtil_CreateTracesFromTimeSeries;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
import RTi.Util.Time.YearType;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the CreateEnsembleFromOneTimeSeries() command.
*/
public class CreateEnsembleFromOneTimeSeries_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
Protected data members shared with the dialog and other related classes.
*/
protected final String _NoShift = "NoShift";
protected final String _ShiftToReference = "ShiftToReference";

/**
TSEnsemble created in discovery mode (basically to get the identifier for other commands).
*/
private TSEnsemble __discoveryTSEnsemble = null;

/**
List of time series created during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List<TS> __discoveryTSList = null;

/**
Constructor.
*/
public CreateEnsembleFromOneTimeSeries_Command ()
{	super();
	setCommandName ( "CreateEnsembleFromOneTimeSeries" );
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
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    
    String TSID = parameters.getValue ( "TSID" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String TraceLength = parameters.getValue ( "TraceLength" );
    String ReferenceDate = parameters.getValue ( "ReferenceDate" );
    String OutputYearType = parameters.getValue ( "OutputYearType" );
    String ShiftDataHow = parameters.getValue ( "ShiftDataHow" );
    
    if ( (TSID == null) || (TSID.length() == 0) ) {
        message = "A time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
    // Check the trace length...
    try {
        TimeInterval.parseInterval ( TraceLength );
    }
    catch ( Exception e ) {
        message = "Trace length \"" + TraceLength +"\" is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a valid interval (e.g., 1Year)." ) );
    }
   
    if ( (InputStart != null) && !InputStart.equals("") &&
            !InputStart.equalsIgnoreCase("InputStart") &&
            !InputStart.equalsIgnoreCase("InputEnd") && (InputStart.indexOf("${") < 0) ) {
        try {   DateTime.parse(InputStart);
        }
        catch ( Exception e ) {
            message = "The input start date/time \"" +InputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, InputStart, InputEnd, or blank to use the global input start." ) );
        }
    }
    if (    (InputEnd != null) && !InputEnd.equals("") &&
        !InputEnd.equalsIgnoreCase("InputStart") &&
        !InputEnd.equalsIgnoreCase("InputEnd") && (InputEnd.indexOf("${") < 0) ) {
        try {   DateTime.parse( InputEnd );
        }
        catch ( Exception e ) {
            message = "The input end date/time \"" +InputStart + "\" is not a valid date/time.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date/time, InputStart, InputEnd, or blank to use the global input start." ) );
        }
    }
    
    if ( (EnsembleID == null) || (EnsembleID.length() == 0) ) {
        message = "An ensemble identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an ensemble identifier." ) );
    }
    if ( (ReferenceDate != null) && !ReferenceDate.equals("") && (ReferenceDate.indexOf("${") < 0) ) {
        try {
            DateTime.parse ( ReferenceDate, null );
        }
        catch ( Exception e ) {
            message = "The reference date \"" + ReferenceDate + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify a valid date/time (or blank) for the reference date." ) );
        }
    }
    
    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
        try {
            YearType.valueOfIgnoreCase(OutputYearType);
        }
        catch ( Exception e ) {
            message = "The output year type (" + OutputYearType + ") is invalid.";
            warning += "\n" + message;
            StringBuffer b = new StringBuffer();
            List<YearType> values = YearType.getYearTypeChoices();
            for ( YearType t : values ) {
                if ( b.length() > 0 ) {
                    b.append ( ", " );
                }
                b.append ( t.toString() );
            }
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(
                CommandStatusType.FAILURE, message, "Valid values are:  " + b.toString() + "."));
        }
    }

    if ( (ShiftDataHow != null) && (ShiftDataHow.length() != 0) &&
            !ShiftDataHow.equalsIgnoreCase(_ShiftToReference) &&
            !ShiftDataHow.equalsIgnoreCase(_NoShift)) {
        message = "The ShiftDataHow parameter value is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify as blank, " + _NoShift + " (default), or " +
                        _ShiftToReference + "." ) );
    }

	// Check for invalid parameters...
    List<String> validList = new ArrayList<String>(11);
    validList.add ( "TSID" );
    validList.add ( "InputStart" );
    validList.add ( "InputEnd" );
    validList.add ( "EnsembleID" );
    validList.add ( "EnsembleName" );
    validList.add ( "Alias" );
	validList.add ( "TraceLength" );
	validList.add ( "TraceDescription" );
	validList.add ( "ReferenceDate" );
	validList.add ( "OutputYearType" );
    validList.add ( "ShiftDataHow" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
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
{	// The command will be modified if changed...
	return (new CreateEnsembleFromOneTimeSeries_JDialog ( parent, this )).ok();
}

/**
Return the ensemble that is created by this class when run in discovery mode.
*/
private TSEnsemble getDiscoveryEnsemble()
{
    return __discoveryTSEnsemble;
}

/**
Return the list of time series read in discovery phase.
*/
private List<TS> getDiscoveryTSList ()
{
    return __discoveryTSList;
}

/**
Return a list of objects of the requested type.
*/
public List getObjectList ( Class c )
{   TSEnsemble tsensemble = getDiscoveryEnsemble();
    List<TS> discoveryTSList = getDiscoveryTSList ();
    if ( (tsensemble != null) && (c == tsensemble.getClass()) ) {
    	List<TSEnsemble> v = new Vector<TSEnsemble>();
        v.add ( tsensemble );
        return v;
    }
    else if ( (discoveryTSList != null) && (discoveryTSList.size() != 0) ) {
        // Since all time series must be the same interval, check the class for the first one (e.g., MonthTS)
        TS datats = discoveryTSList.get(0);
        if ((c == TS.class) || (c == datats.getClass()) ) {
            return discoveryTSList;
        }
    }
    return null;
}

// Use base class parseCommand()

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
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommandDiscovery ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{
    runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
private void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommandInternal", message;
	int warning_level = 2;
    int log_level = 3;  // Non-user warning level
	String command_tag = "" + command_number;
	int warning_count = 0;

    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // default
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
    
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryEnsemble ( null );
        setDiscoveryTSList( null );
    }

	PropList parameters = getCommandParameters();
	String TSID = parameters.getValue ( "TSID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String EnsembleName = parameters.getValue ( "EnsembleName" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnsembleName != null) && (EnsembleName.indexOf("${") >= 0) ) {
		EnsembleName = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleName);
	}
    if ( EnsembleName == null ) {
        EnsembleName = "";
    }
    String Alias = parameters.getValue ( "Alias" );
    if ( Alias == null ) {
        Alias = ""; // Alias expanded below
    }
	String InputStart = parameters.getValue("InputStart");
	if ( (InputStart == null) || InputStart.isEmpty() ) {
		InputStart = "${InputStart}"; // Global default
	}
	DateTime InputStart_DateTime = null;
	String InputEnd = parameters.getValue("InputEnd");
	if ( (InputEnd == null) || InputEnd.isEmpty() ) {
		InputEnd = "${InputEnd}"; // Global default
	}
	DateTime InputEnd_DateTime = null;
    String TraceLength = parameters.getValue ( "TraceLength" );
    String TraceDescription = parameters.getValue ( "TraceDescription" ); // Expanded below like alias
    String ReferenceDate = parameters.getValue ( "ReferenceDate" );
	if ( (commandPhase == CommandPhaseType.RUN) && (ReferenceDate != null) && (ReferenceDate.indexOf("${") >= 0) ) {
		ReferenceDate = TSCommandProcessorUtil.expandParameterValue(processor, this, ReferenceDate);
	}
    String OutputYearType = parameters.getValue( "OutputYearType" );
    YearType outputYearType = YearType.CALENDAR;
    if ( (OutputYearType != null) && !OutputYearType.equals("") ) {
        outputYearType = YearType.valueOfIgnoreCase(OutputYearType);
    }
    String ShiftDataHow = parameters.getValue ( "ShiftDataHow" );

    TS ts = null;
    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
    PropList bean_PropList = null;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Get the discovery time series list from all time series above this command
        String TSList = "" + TSListType.LAST_MATCHING_TSID;
        List<TS> tslist = TSCommandProcessorUtil.getDiscoveryTSFromCommandsBeforeCommand(
            (TSCommandProcessor)processor, this, TSList, TSID, null, null );
        if ( (tslist != null) && (tslist.size() > 0) ) {
            ts = tslist.get(0);
        }
    }
    else if ( commandPhase == CommandPhaseType.RUN ) {
        // Get the time series to process.  The time series list is searched backwards until the first match...
        request_params = new PropList ( "" );
        request_params.set ( "CommandTag", command_tag );
        request_params.set ( "TSID", TSID );
        try {
            bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\") from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object o_TS = bean_PropList.getContents ( "TS");
        if ( o_TS == null ) {
            message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + TSID + "\" from processor.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( commandPhase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        }
        else {
            ts = (TS)o_TS;
        }
    }
     
    if ( ts == null ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
	        message = "Unable to find time series to analyze using TSID \"" + TSID + "\".";
	        Message.printWarning ( warning_level,
	        MessageUtil.formatMessageTag(
	        command_tag,++warning_count), routine, message );
	        status.addToLog ( commandPhase,
	        new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
	        throw new CommandWarningException ( message );
    	}
    }
    
    DateTime ReferenceDate_DateTime = null;
	if ( commandPhase == CommandPhaseType.RUN ) {
	    if ( (ReferenceDate != null) && !ReferenceDate.equals("") ) {
	        try {
	            // The following call will recognize special values like CurrentToDay
	            ReferenceDate_DateTime = DateTime.parse(ReferenceDate,null);
	        }
	        catch ( Exception e ) {
	            message="Reference date \"" + ReferenceDate + "\" is invalid.";
	            Message.printWarning(log_level,
	                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
	                    routine, message );
	            status.addToLog ( commandPhase,
	                    new CommandLogRecord(CommandStatusType.FAILURE,
	                            message, "Verify that the reference date is valid." ) );
	        }
	    }
	}
    
	// Input period...
    
	if ( commandPhase == CommandPhaseType.RUN ) {
		try {
			InputStart_DateTime = TSCommandProcessorUtil.getDateTime ( InputStart, "InputStart", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
		try {
			InputEnd_DateTime = TSCommandProcessorUtil.getDateTime ( InputEnd, "InputEnd", processor,
				status, warning_level, command_tag );
		}
		catch ( InvalidCommandParameterException e ) {
			// Warning will have been added above...
			++warning_count;
		}
	}
	
	// Now try to process.
    
    List<TS> tslist = null;
    boolean createData = true;
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        createData = false;
    }
    try {
        TSUtil_CreateTracesFromTimeSeries util = new TSUtil_CreateTracesFromTimeSeries();
        tslist = util.getTracesFromTS ( ts, TraceLength, ReferenceDate_DateTime,
            outputYearType, ShiftDataHow, InputStart_DateTime, InputEnd_DateTime, Alias, TraceDescription, createData );
        // The above code does not recognize ${Properties} from the processor so reset the alias if necessary
        if ( (Alias != null) && Alias.indexOf("${") >= 0 ) {
        	for ( TS ts2 : tslist ) {
        		String alias = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, ts2, Alias, status, commandPhase);
                ts2.setAlias ( alias );
        	}
        }
    }
    catch ( Exception e ) {
    	if ( commandPhase == CommandPhaseType.RUN ) {
	    	if ( ts == null ) {
	    		message = "Unexpected error creating traces from time series, time series is null (" + e + ").";
	    	}
	    	else {
	    		message = "Unexpected error creating traces from time series \"" + ts.getIdentifier() + "\" (" + e + ").";
	    	}
	        Message.printWarning ( warning_level, 
	                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
	        Message.printWarning ( 3, routine, e );
	        status.addToLog ( commandPhase,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Check log file for details." ) );
	        throw new CommandException ( message );
    	}
    }
    
    int size = 0;
    if ( tslist != null ) {
        size = tslist.size();
    }
    
    // Update the data to the processor so that appropriate actions are taken...

    if ( tslist != null ) {
        if ( ts != null ) {
        	Message.printStatus ( 2, routine, "Created " + size + " traces from time series \"" + ts.getIdentifier() + "\"" );
        }
        if ( commandPhase == CommandPhaseType.RUN ) {
            TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
            TSCommandProcessorUtil.appendTimeSeriesListToResultsList(processor, this, tslist);
            
            // Create an ensemble and add to the processor...
            String ensembleName = EnsembleName;
            if ( ts != null ) {
            	ensembleName = TSCommandProcessorUtil.expandTimeSeriesMetadataString(
                    processor, ts, EnsembleName, status, commandPhase);
            }
            TSEnsemble ensemble = new TSEnsemble ( EnsembleID, ensembleName, tslist );
            TSCommandProcessorUtil.appendEnsembleToResultsEnsembleList(processor, this, ensemble);
        }
    }
    // Always set the data in discovery mode
    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        // Create an ensemble and add the discovery time series...
        TSEnsemble ensemble = new TSEnsemble ( EnsembleID, EnsembleName, tslist );
        setDiscoveryTSList(tslist);
        setDiscoveryEnsemble ( ensemble );
    }

    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandWarningException ( message );
    }
	
	status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
}

/**
Set the ensemble that is processed by this class in discovery mode.
*/
private void setDiscoveryEnsemble ( TSEnsemble tsensemble )
{
    __discoveryTSEnsemble = tsensemble;
}

/**
Set the list of time series read in discovery phase.
@param discoveryTSList list of time series created during discovery phase
*/
private void setDiscoveryTSList ( List<TS> discoveryTSList )
{
    __discoveryTSList = discoveryTSList;
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
    String TSID = parameters.getValue ( "TSID" );
    String TraceLength = parameters.getValue ( "TraceLength" );
    String TraceDescription = parameters.getValue ( "TraceDescription" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
    String EnsembleName = parameters.getValue ( "EnsembleName" );
    String Alias = parameters.getValue ( "Alias" );
    String ReferenceDate = parameters.getValue ( "ReferenceDate" );
    String OutputYearType = parameters.getValue ( "OutputYearType" );
    String ShiftDataHow = parameters.getValue ( "ShiftDataHow" );
	StringBuffer b = new StringBuffer ();
	if ( (TSID != null) && (TSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TSID=\"" + TSID + "\"" );
	}
    if ( (InputStart != null) && (InputStart.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputStart=" + InputStart );
    }
    if ( (InputEnd != null) && (InputEnd.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "InputEnd=" + InputEnd );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
    if ( (EnsembleName != null) && (EnsembleName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleName=\"" + EnsembleName + "\"" );
    }
    if ( (Alias != null) && (Alias.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Alias=\"" + Alias + "\"" );
    }
	if ( (TraceLength != null) && (TraceLength.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TraceLength=" + TraceLength );
	}
	if ( (TraceDescription != null) && (TraceDescription.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "TraceDescription=\"" + TraceDescription + "\"" );
	}
    if ( (ReferenceDate != null) && (ReferenceDate.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ReferenceDate=\"" + ReferenceDate + "\"");
    }
    if ( (OutputYearType != null) && (OutputYearType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputYearType=" + OutputYearType );
    }
    if ( (ShiftDataHow != null) && (ShiftDataHow.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ShiftDataHow=" + ShiftDataHow );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
