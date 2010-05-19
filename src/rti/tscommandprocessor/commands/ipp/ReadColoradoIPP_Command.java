package rti.tscommandprocessor.commands.ipp;

import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;

/**
<p>
This class initializes, checks, and runs the ReadColoradoIPP() command.
</p>
*/
public class ReadColoradoIPP_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

/**
Number of where clauses shown in the editor and available as parameters - the HydroBase SPFlex maximum minus
2 (data type and interval).
*/
private int __numWhere = (HydroBaseDMI.getSPFlexMaxParameters() - 2);

/**
Data values for boolean parameters.
*/
protected String _False = "False";
protected String _True = "True";

/**
Data values for IfMissing parameter.
*/
protected String _Ignore = "Ignore";
protected String _Warn = "Warn";

/**
List of time series read during discovery.  These are TS objects but with mainly the
metadata (TSIdent) filled in.
*/
private List __discovery_TS_Vector = null;

/**
Constructor.
*/
public ReadColoradoIPP_Command ()
{	super();
	setCommandName ( "ReadColoradoIPP" );
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
    
    String Subject = parameters.getValue ( "Subject" );
    String InputStart = parameters.getValue ( "InputStart" );
    String InputEnd = parameters.getValue ( "InputEnd" );

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
   
    if ( (Subject == null) || Subject.equals("") ) {
        message = "The subject must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the subject." ) );
    }
    else {
        boolean found = false;
        for ( IPPSubjectType subject: IPPSubjectType.values() ) {
            if ( Subject.equalsIgnoreCase(""+subject)) {
                found = true;
                break;
            }
        }
        if ( !found ) {
            message = "The subject (" + Subject + ") is invalid.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the subject as " + IPPSubjectType.COUNTY + ", " +
                    IPPSubjectType.PROJECT + ", or " + IPPSubjectType.PROVIDER + "." ) );
        }
    }

	// InputName is optional.
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

    // Check for invalid parameters...
    List valid_Vector = new Vector();
    valid_Vector.add ( "Subject" );
    valid_Vector.add ( "SubjectID" );
    valid_Vector.add ( "SubjectName" );
    valid_Vector.add ( "DataSource" );
    valid_Vector.add ( "DataType" );
    valid_Vector.add ( "SubDataType" );
    valid_Vector.add ( "Interval" );
    int numFilters = HydroBaseDMI.getSPFlexMaxParameters() - 2; // Maximum minus data type and interval
    for ( int i = 1; i <= numFilters; i++ ) { 
        valid_Vector.add ( "Where" + i );
    }
    valid_Vector.add ( "Method" );
    valid_Vector.add ( "SubMethod" );
    valid_Vector.add ( "Scenario" );
    valid_Vector.add ( "InputName" );
    valid_Vector.add ( "InputStart" );
    valid_Vector.add ( "InputEnd" );
    valid_Vector.add ( "Alias" );
    warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Return the list of time series read in discovery phase.
*/
private List getDiscoveryTSList ()
{
    return __discovery_TS_Vector;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
	List discovery_TS_Vector = getDiscoveryTSList ();
    if ( (discovery_TS_Vector == null) || (discovery_TS_Vector.size() == 0) ) {
        return null;
    }
    TS datats = (TS)discovery_TS_Vector.get(0);
    // Use the most generic for the base class...
    TS ts = new TS();
    if ( (c == ts.getClass()) || (c == datats.getClass()) ) {
        return discovery_TS_Vector;
    }
    else {
        return null;
    }
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ReadColoradoIPP_JDialog ( parent, this )).ok();
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
@param command_number Number of command in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadColoradoIpp_Command.runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;

	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    
    boolean readData = true;
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTSList ( null );
        readData = false;
    }
    
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
            status.addToLog ( command_phase,
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
                status.addToLog ( command_phase,
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

	List tslist = new Vector();	// Vector for time series results.
					// Will be added to for one time series
					// read or replaced if a list is read.
	try {
        // Read 1+ time series...
		// Get the input needed to process the file...
	    String Subject = parameters.getValue ( "Subject" );
	    String SubjectName = parameters.getValue ( "SubjectName" );
	    String Source = parameters.getValue ( "Source" );
		String DataType = parameters.getValue ( "DataType" );
		String SubDataType = parameters.getValue ( "SubDataType" );
		String Method = parameters.getValue ( "Method" );
		String SubMethod = parameters.getValue ( "SubMethod" );
		String Scenario = parameters.getValue ( "Scenario" );
		String InputName = parameters.getValue ( "InputName" );
		if ( InputName == null ) {
			InputName = "";
		}
		List WhereN_Vector = new Vector ( 6 );
		String WhereN;
		int nfg = 0;	// Used below.
		for ( nfg = 0; nfg < 1000; nfg++ ) {
			WhereN = parameters.getValue ( "Where" + (nfg + 1) );
			if ( WhereN == null ) {
				break;	// No more where clauses
			}
			WhereN_Vector.add ( WhereN );
		}
	
		// Find the HydroBaseDMI to use...
		Object o = processor.getPropContents ( "ColoradoIppDMIList" );
		if ( o == null ) {
			message = "Could not get list of Colorado IPP connections to query data.";
			Message.printWarning ( 2, routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a Colorado IPP database connection has been opened." ) );
			throw new Exception ( message );
		}
		IppDMI ippdmi = (IppDMI)((List)o).get(0);
		/*
		List ippdmi_Vector = (List)o;
		IppDMI ippdmi = HydroBase_Util.lookupHydroBaseDMI ( ippdmi_Vector, InputName );
		if ( ippdmi == null ) {
			message ="Could not find HydroBase connection with input name \"" + InputName + "\" to query data.";
			Message.printWarning ( 2, routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a HydroBase database connection has been opened." ) );
			throw new Exception ( message );
		}
		*/

		// Initialize an input filter based on the data type...
/*
		InputFilter_JPanel filter_panel = null;
		boolean is_CASS = false;
		boolean is_NASS = false;
		boolean is_Station = false;
		boolean is_Structure = false;
		boolean is_StructureSFUT = false;
		boolean is_StructureIrrigSummaryTS = false;
		boolean is_SheetName = false;

		int wdid_length = HydroBase_Util.getPreferredWDIDLength();

		// Create the input filter panel...

		if ( HydroBase_Util.isStationTimeSeriesDataType ( ippdmi, DataType ) ){
			// Stations...
			is_Station = true;
			filter_panel = new HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel ( ippdmi );
			Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for station." );
		}
		else if ( HydroBase_Util.isStructureSFUTTimeSeriesDataType ( ippdmi, DataType ) ) {
			// Structures (with SFUT)...
			is_StructureSFUT = true;
			PropList filter_props = new PropList ( "" );
			filter_props.set ( "NumFilterGroups=6" );
			Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for structure SFUT." );
			filter_panel = new HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
				ippdmi, true, filter_props );
		}
		else if ( HydroBase_Util.isStructureTimeSeriesDataType (ippdmi, DataType ) ) {
			// Structures (no SFUT)...
			is_Structure = true;
			PropList filter_props = new PropList ( "" );
			filter_props.set ( "NumFilterGroups=6" );
			filter_panel = new HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
				ippdmi, false, filter_props );
			Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for structure (no SFUT)." );
		}
		else if ( HydroBase_Util.isIrrigSummaryTimeSeriesDataType(ippdmi, DataType ) ) {
			// Irrig summary TS...
			is_StructureIrrigSummaryTS = true;
			filter_panel = new HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel ( ippdmi );
			Message.printStatus ( 2, routine, "Data type \"" + DataType + "\" is for structure irrig summary ts." );
		}
		else if ( HydroBase_Util.isAgriculturalCASSCropStatsTimeSeriesDataType ( ippdmi, DataType) ) {
			is_CASS = true;
			filter_panel = new
			HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel (ippdmi );
			Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for CASS." );
		}
		else if ( HydroBase_Util.isAgriculturalNASSCropStatsTimeSeriesDataType (	ippdmi, DataType ) ) {
			// Data from agricultural_CASS_crop_statistics
			// or agricultural_NASS_crop_statistics...
			is_NASS = true;
			filter_panel = new
			HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel (ippdmi );
			Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for NASS." );
		}
		else if( HydroBase_Util.isWISTimeSeriesDataType (ippdmi, DataType ) ) {
			// Sheet name...
			is_SheetName = true;
			filter_panel = new
			HydroBase_GUI_SheetNameWISFormat_InputFilter_JPanel ( ippdmi );
			Message.printStatus ( 2, routine,"Data type \"" + DataType +"\" is for WIS." );
		}
		else {
            message = "Data type \"" + DataType + "\" is not recognized as a HydroBase data type.";
			Message.printWarning ( 2, routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that the datatype can be used with HydroBase (see documentation)." ) );
			throw new Exception ( message );
		}

		// Populate with the where information from the command...

		String filter_delim = ";";
		for ( int ifg = 0; ifg < nfg; ifg ++ ) {
			WhereN = (String)WhereN_Vector.get(ifg);
            if ( WhereN.length() == 0 ) {
                continue;
            }
			// Set the filter...
			try {
                filter_panel.setInputFilter( ifg, WhereN, filter_delim );
			}
			catch ( Exception e ) {
                message = "Error setting where information using \""+WhereN+"\"";
				Message.printWarning ( 2, routine,message);
				Message.printWarning ( 3, routine, e );
				++warning_count;
                status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support - also see the log file." ) );
			}
		}
	*/
		// Read the list of objects from which identifiers can be obtained.  This code is similar to that in
		// TSTool_JFrame.readHydroBaseHeaders...
	
		Message.printStatus ( 2, routine, "Getting the list of time series..." );
	
		List tslist0 = null;
		if ( Subject.equalsIgnoreCase("County")) {
		    tslist0 = ippdmi.readCountyDataMetaDataList( SubjectName, DataType, SubDataType,
                Method, SubMethod, Source, Scenario );
		}
		else if ( Subject.equalsIgnoreCase("Project")) {
		    tslist0 = ippdmi.readProjectDataMetaDataList( SubjectName, DataType, SubDataType,
                Method, SubMethod, Source, Scenario );
		}
		else if ( Subject.equalsIgnoreCase("Provider")) {
		    tslist0 = ippdmi.readProviderDataMetaDataList( SubjectName, DataType, SubDataType,
	            Method, SubMethod, Source, Scenario );
		}
		// Make sure that size is set...
		int size = 0;
		if ( tslist0 != null ) {
			size = tslist0.size();
		}
	
   		if ( (tslist0 == null) || (size == 0) ) {
			Message.printStatus ( 2, routine,"No Colorado IPP time series were found." );
	        // Warn if nothing was retrieved (can be overridden to ignore).
            message = "No time series were read from the Colorado IPP database.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database." +
                    	"  Previous messages may provide more information." ) );
   		}
   		else {
			// Else, convert each header object to a TSID string and read the time series...

			Message.printStatus ( 2, "", "Reading " + size + " time series..." );

			//String tsident_string = null; // TSIdent string
			TS ts; // Time series to read.
			IPP_DataMetaData meta = null;
			for ( int i = 0; i < size; i++ ) {
				meta = (IPP_DataMetaData)tslist0.get(i);
				//tsident_string = 
					//Subject + ":" + SubjectName
					//+ "." + Source
					//+ "." + DataType + "-" + SubDataType + "." + Interval
					//+ "." + Method + "-" + SubMethod + "-" + Scenario
					//+ "~ColoradoIPP" + input_name;
	
				//Message.printStatus ( 2, routine, "Reading time series for \"" + tsident_string + "\"..." );
				Message.printStatus ( 2, routine, "Reading time series for subject=\"" + meta.getSubject() +
				    "\" name=\"" + meta.getName() + "\"..." );
				try {
				    ts = ippdmi.readTimeSeries ( meta.getSubject(), meta.getID(), meta.getName(),
			            meta.getSource(), meta.getDataType(), meta.getSubType(), meta.getUnits(), meta.getMethod(),
			            meta.getSubMethod(), meta.getScenario(), InputStart_DateTime, InputEnd_DateTime, readData );
				    /*
				    It gets a bit complicated with the ID so deal with parsing later
				    ts = ippdmi.readTimeSeries (
						tsident_string,
						InputStart_DateTime,
						InputEnd_DateTime, null, read_data );
						*/
					// Add the time series to the temporary list.  It will be further processed below...
					tslist.add ( ts );
				}
				catch ( Exception e ) {
					message = "Unexpected error reading Colorado IPP time series (" + e + ").";
					Message.printWarning ( 2, routine, message );
					Message.printWarning ( 2, routine, e );
					++warning_count;
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report the problem to software support - also see the log file." ) );
				}
			}
		}
    
        size = 0;
        if ( tslist != null ) {
            size = tslist.size();
        }
        Message.printStatus ( 2, routine, "Read " + size + " Colorado IPP time series." );

        if ( command_phase == CommandPhaseType.RUN ) {
            if ( tslist != null ) {
                // Further process the time series...
                // This makes sure the period is at least as long as the output period...

                int wc = TSCommandProcessorUtil.processTimeSeriesListAfterRead( processor, this, tslist );
                if ( wc > 0 ) {
                    message = "Error post-processing Colorado IPP time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    // Don't throw an exception - probably due to missing data.
                }
    
                // Now add the list in the processor...
                
                int wc2 = TSCommandProcessorUtil.appendTimeSeriesListToResultsList ( processor, this, tslist );
                if ( wc2 > 0 ) {
                    message = "Error adding Colorado IPP time series after read.";
                    Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                    status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
                    throw new CommandException ( message );
                }
            }
        }
        else if ( command_phase == CommandPhaseType.DISCOVERY ) {
            setDiscoveryTSList ( tslist );
        }
        // Warn if nothing was retrieved (can be overridden to ignore).
        if ( (tslist == null) || (size == 0) ) {
            message = "No time series were read from HydroBase.";
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Data may not be in database.  See previous messages." ) );
    }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error reading time series from Colorado IPP (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( command_phase,
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
    
    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the list of time series read in discovery phase.
*/
private void setDiscoveryTSList ( List discovery_TS_Vector )
{
    __discovery_TS_Vector = discovery_TS_Vector;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	StringBuffer b = new StringBuffer ();
	if ( props == null ) {
	    return getCommandName() + "()";
	}

    String Subject = props.getValue("Subject");
    if ( (Subject != null) && (Subject.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Subject=" + Subject );
    }
    String SubjectName = props.getValue("SubjectName");
    if ( (SubjectName != null) && (SubjectName.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SubjectName=\"" + SubjectName + "\"" );
    }
    String DataSource = props.getValue("DataSource");
    if ( (DataSource != null) && (DataSource.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataSource=\"" + DataSource + "\"" );
    }
    String DataType = props.getValue("DataType");
	if ( (DataType != null) && (DataType.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "DataType=\"" + DataType + "\"" );
	}
    String SubDataType = props.getValue("SubDataType");
    if ( (SubDataType != null) && (SubDataType.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SubDataType=\"" + SubDataType + "\"" );
    }
    String Method = props.getValue("Method");
    if ( (Method != null) && (Method.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Method=\"" + Method + "\"" );
    }
    String SubMethod = props.getValue("SubMethod");
    if ( (SubMethod != null) && (SubMethod.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SubMethod=\"" + SubMethod + "\"" );
    }
    String Scenario = props.getValue("Scenario");
    if ( (Scenario != null) && (Scenario.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Scenario=\"" + Scenario + "\"" );
    }
	String Interval = props.getValue("Interval");
	if ( (Interval != null) && (Interval.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "Interval=\"" + Interval + "\"" );
	}
	String InputName = props.getValue("InputName");
	if ( (InputName != null) && (InputName.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputName=\"" + InputName + "\"" );
	}
	String delim = ";";
    for ( int i = 1; i <= __numWhere; i++ ) {
    	String where = props.getValue("Where" + i);
    	if ( (where != null) && (where.length() > 0) && !where.startsWith(delim) ) {
    		if ( b.length() > 0 ) {
    			b.append ( "," );
    		}
    		b.append ( "Where" + i + "=\"" + where + "\"" );
    	}
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

    return getCommandName() + "(" + b.toString() + ")";
}

}
