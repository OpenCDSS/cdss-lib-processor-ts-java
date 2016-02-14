package rti.tscommandprocessor.commands.spatial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import com.google.gson.Gson;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.GIS.GeoView.GeoJSONGeometryFormatter;
import RTi.GIS.GeoView.WKTGeometryParser;
import RTi.GR.GRPoint;
import RTi.GR.GRPointZM;
import RTi.GR.GRShape;
import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.Command;
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
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the WriteTimeSeriesToGeoJSON() command.
*/
public class WriteTimeSeriesToGeoJSON_Command extends AbstractCommand implements Command, FileGenerator
{
	
/**
Possible values for Append.
*/
protected final String _False = "False";
protected final String _True = "True";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteTimeSeriesToGeoJSON_Command ()
{   super();
    setCommandName ( "WriteTimeSeriesToGeoJSON" );
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
{   String OutputFile = parameters.getValue ( "OutputFile" );
	String Append = parameters.getValue ( "Append" );
    String LongitudeProperty = parameters.getValue ( "LongitudeProperty" );
    String LatitudeProperty = parameters.getValue ( "LatitudeProperty" );
    String WKTGeometryProperty = parameters.getValue ( "WKTGeometryProperty" );
    String warning = "";
    String routine = getCommandName() + ".checkCommandParameters";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);
    
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an output file." ) );
    }
    else if ( OutputFile.indexOf ("${") < 0 ) {
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
    
    if ( Append != null && !Append.equalsIgnoreCase(_True) && 
        !Append.equalsIgnoreCase(_False) && !Append.isEmpty() ) {
        message = "Append is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Append must be specified as " + _False + " (default) or " + _True ) );
    }
    
    if ( ((LongitudeProperty == null) || (LongitudeProperty.length() == 0)) &&
        ((WKTGeometryProperty == null) || (WKTGeometryProperty.length() == 0)) ) {
        message = "The longitude property OR WKT geometry property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the longitude OR WKT geometry property." ) );
    }
    
    if ( ((LongitudeProperty != null) && (LongitudeProperty.length() != 0)) &&
        ((WKTGeometryProperty != null) && (WKTGeometryProperty.length() != 0)) ) {
        message = "The longitude property OR WKT geometry property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the longitude OR WKT geometry property." ) );
    }
    
    if ( ((LatitudeProperty == null) || (LatitudeProperty.length() == 0)) &&
        ((WKTGeometryProperty == null) || (WKTGeometryProperty.length() == 0)) ) {
        message = "The latitude property OR WKT geometry property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the latitude column OR WKT geometry property." ) );
    }
    
    if ( ((LatitudeProperty != null) && (LatitudeProperty.length() != 0)) &&
        ((WKTGeometryProperty != null) && (WKTGeometryProperty.length() != 0)) ) {
        message = "The latitude property OR WKT geometry property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the latitude column OR WKT geometry property." ) );
    }

    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(14);
    validList.add ( "TSList" );
    validList.add ( "TSID" );
    validList.add ( "EnsembleID" );
    validList.add ( "OutputFile" );
    validList.add ( "Append" );
    validList.add ( "LongitudeProperty" );
    validList.add ( "LatitudeProperty" );
    validList.add ( "ElevationProperty" );
    validList.add ( "WKTGeometryProperty" );
    validList.add ( "IncludeProperties" );
    validList.add ( "ExcludeProperties" );
    validList.add ( "JavaScriptVar" );
    validList.add ( "PrependText" );
    validList.add ( "AppendText" );
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
{   // The command will be modified if changed...
    return (new WriteTimeSeriesToGeoJSON_JDialog ( parent, this )).ok();
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
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
    return __OutputFile_File;
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{   String routine = getClass().getSimpleName() + ".runCommand", message;
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    
    // Clear the output file
    
    setOutputFile ( null );
    
    // Check whether the processor wants output files to be created...
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    CommandProcessor processor = getCommandProcessor();
    if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
        Message.printStatus ( 2, routine,
        "Skipping \"" + toString() + "\" because output is not being created." );
    }
    
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

    PropList parameters = getCommandParameters();
    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
    String TSID = parameters.getValue ( "TSID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (TSID != null) && (TSID.indexOf("${") >= 0) ) {
		TSID = TSCommandProcessorUtil.expandParameterValue(processor, this, TSID);
	}
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	if ( (commandPhase == CommandPhaseType.RUN) && (EnsembleID != null) && (EnsembleID.indexOf("${") >= 0) ) {
		EnsembleID = TSCommandProcessorUtil.expandParameterValue(processor, this, EnsembleID);
	}
    String OutputFile = parameters.getValue ( "OutputFile" );
    String Append = parameters.getValue ( "Append" );
    boolean append = false;
    if ( (Append != null) && Append.equalsIgnoreCase(_True) ) {
    	append = true;
    }
    String LongitudeProperty = parameters.getValue ( "LongitudeProperty" );
    String LatitudeProperty = parameters.getValue ( "LatitudeProperty" );
    String ElevationProperty = parameters.getValue ( "ElevationProperty" );
    String WKTGeometryProperty = parameters.getValue ( "WKTGeometryProperty" );
    String IncludeProperties = parameters.getValue ( "IncludeProperties" );
    String [] includeProperties = null;
    if ( (IncludeProperties != null) && !IncludeProperties.isEmpty() ) {
        // Use the provided columns
        includeProperties = IncludeProperties.split(",");
        for ( int i = 0; i < includeProperties.length; i++ ) {
            // Convert glob notation to Java
            includeProperties[i] = includeProperties[i].trim();
        }
    }
    String ExcludeProperties = parameters.getValue ( "ExcludeProperties" );
    String [] excludeProperties = null;
    if ( (ExcludeProperties != null) && !ExcludeProperties.isEmpty() ) {
        // Use the provided columns
        excludeProperties = ExcludeProperties.split(",");
        for ( int i = 0; i < excludeProperties.length; i++ ) {
        	// Convert glob notation to Java
            excludeProperties[i] = excludeProperties[i].trim().replace("*",".*");
        }
    }
    String JavaScriptVar = parameters.getValue ( "JavaScriptVar" );
    String PrependText = parameters.getValue ( "PrependText" );
	if ( (PrependText != null) && (PrependText.indexOf("${") >= 0) ) {
		PrependText = TSCommandProcessorUtil.expandParameterValue(processor, this, PrependText);
	}
    String AppendText = parameters.getValue ( "AppendText" );
	if ( (AppendText != null) && (AppendText.indexOf("${") >= 0) ) {
		AppendText = TSCommandProcessorUtil.expandParameterValue(processor, this, AppendText);
	}

    // Get the time series to process...
    PropList request_params = new PropList ( "" );
    request_params.set ( "TSList", TSList );
    request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report problem to software support." ) );
    }
    List tslist = (List)o_TSList;
    if ( tslist.size() == 0 ) {
        message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Confirm that time series are available (may be OK for partial run)." ) );
    } 
    
    String OutputFile_full = OutputFile;
    try {
        // Convert to an absolute path...
        OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
        Message.printStatus ( 2, routine, "Writing GeoJSON file \"" + OutputFile_full + "\"" );
        List<String> errors = new ArrayList<String>();
        writeTimeSeriesToGeoJSON ( tslist, OutputFile_full, append, includeProperties, excludeProperties,
            LongitudeProperty, LatitudeProperty, ElevationProperty, WKTGeometryProperty, JavaScriptVar, PrependText, AppendText, errors );
        for ( String error : errors ) {
            Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, error );
            status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    error, "Check log file for details." ) );
        }
        // Save the output file name...
        setOutputFile ( new File(OutputFile_full));
    }
    catch ( Exception e ) {
        message = "Unexpected error writing time series to GeoJSON file \"" + OutputFile_full + "\" (" + e + ")";
        Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN,
            new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
    
    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
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
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{   if ( parameters == null ) {
        return getCommandName() + "()";
    }
    String TSList = parameters.getValue ( "TSList" );
    String TSID = parameters.getValue( "TSID" );
    String EnsembleID = parameters.getValue( "EnsembleID" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String Append = parameters.getValue ( "Append" );
    String LongitudeProperty = parameters.getValue ( "LongitudeProperty" );
    String LatitudeProperty = parameters.getValue ( "LatitudeProperty" );
    String ElevationProperty = parameters.getValue ( "ElevationProperty" );
    String WKTGeometryProperty = parameters.getValue ( "WKTGeometryProperty" );
    String IncludeProperties = parameters.getValue ( "IncludeProperties" );
    String ExcludeProperties = parameters.getValue ( "ExcludeProperties" );
    String JavaScriptVar = parameters.getValue ( "JavaScriptVar" );
    String PrependText = parameters.getValue ( "PrependText" );
    String AppendText = parameters.getValue ( "AppendText" );
    StringBuffer b = new StringBuffer ();
    if ( (TSList != null) && (TSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSList=" + TSList );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID=\"" + TSID + "\"" );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }
    if ( (Append != null) && (Append.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Append=\"" + Append + "\"" );
    }
    if ( (LongitudeProperty != null) && (LongitudeProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LongitudeProperty=\"" + LongitudeProperty + "\"" );
    }
    if ( (LatitudeProperty != null) && (LatitudeProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LatitudeProperty=\"" + LatitudeProperty + "\"" );
    }
    if ( (ElevationProperty != null) && (ElevationProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ElevationProperty=\"" + ElevationProperty + "\"" );
    }
    if ( (WKTGeometryProperty != null) && (WKTGeometryProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WKTGeometryProperty=\"" + WKTGeometryProperty + "\"" );
    }
    if ( (IncludeProperties != null) && (IncludeProperties.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "IncludeProperties=\"" + IncludeProperties + "\"" );
    }
    if ( (ExcludeProperties != null) && (ExcludeProperties.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExcludeProperties=\"" + ExcludeProperties + "\"" );
    }
    if ( (JavaScriptVar != null) && (JavaScriptVar.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "JavaScriptVar=\"" + JavaScriptVar + "\"" );
    }
    if ( (PrependText != null) && (PrependText.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PrependText=\"" + PrependText + "\"" );
    }
    if ( (AppendText != null) && (AppendText.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AppendText=\"" + AppendText + "\"" );
    }
    return getCommandName() + "(" + b.toString() + ")";
}

// TODO SAM 2013-7-01 Evaluate whether a separate package should be created - for now keep the code here
// until there is time to split out
private void writeTimeSeriesToGeoJSON ( List<TS> tslist, String outputFile, boolean append, String [] includeProperties, String [] excludeProperties,
    String longitudeProperty, String latitudeProperty, String elevationProperty, String wktGeometryProperty, String javaScriptVar,
    String prependText, String appendText, List<String> errors )
throws IOException
{   PrintWriter fout = null;
	if ( appendText == null ) {
		appendText = "";
	}
	try {
		// Open the output file
		FileOutputStream fos = new FileOutputStream ( outputFile, append );
		if ( includeProperties == null ) {
			includeProperties = new String[1]; // Simplifies error handling
			includeProperties[0] = ".*"; // match all
		}
		if ( excludeProperties == null ) {
			excludeProperties = new String[0]; // Simplifies error handling
		}
	    fout = new PrintWriter ( fos );
	    // Indentations
	    String i1 = "  ";
	    String i2 = "    ";
	    String i3 = "      ";
	    String i4 = "        ";
	    boolean doPoint = false;
	    boolean doWkt = false;
	    boolean doElevation = false;
	    // WKT trumps point data
	    if ( (wktGeometryProperty != null) && !wktGeometryProperty.equals("") ) {
	        doWkt = true;
	    }
	    else {
	        if ( (latitudeProperty != null) && !latitudeProperty.equals("") &&
	            (longitudeProperty != null) && !longitudeProperty.equals("")) {
	            doPoint = true;
	        }
	    }
   
	    // Output GeoJSON intro
	    if ( (prependText != null) && !prependText.isEmpty() ) {
	    	fout.print(prependText);
	    }
	    if ( (javaScriptVar != null) && !javaScriptVar.isEmpty() ) {
	    	fout.print( "var " + javaScriptVar + " = {\n" );
	    }
	    else {
	     	fout.print("{\n" );
	    }
	    fout.print( i1 + "\"type\": \"FeatureCollection\",\n");
	    fout.print( i1 + "\"features\": [\n");
	
	    Object latitudeO = null, longitudeO = null, elevationO = null; // Can be double, int, or string
	    Object wkt0 = null;
	    WKTGeometryParser wktParser = null;
	    GeoJSONGeometryFormatter geoJSONFormatter = new GeoJSONGeometryFormatter(2);
	    Gson gson = new Gson();
	    String wkt = null;
	    if ( doWkt ) {
	        wktParser = new WKTGeometryParser();
	    }
	    GRShape shape = null; // Shape instances to add below
	    GRPoint point = null;
	    GRPointZM pointzm = null;
	    Double x = 0.0, y = 0.0, z = 0.0;
	    int nts = tslist.size();
	    int nts0 = nts - 1;
	    Object o = null; // Object from t
	    TS ts;
	    boolean haveElevation = false;
	    for ( int its = 0; its < nts; its++ ) {
	        try {
	            ts = tslist.get(its);
	            longitudeO = null;
	            latitudeO = null;
	            haveElevation = false;
	            if ( doPoint ) {
	                // Property columns can be any type because objects are treated as strings below
	                longitudeO = ts.getProperty(longitudeProperty);
	                if ( longitudeO == null ) {
	                    continue;
	                }
	                else if ( longitudeO instanceof Double ) {
	                	x = (Double)longitudeO;
	                }
	                else if ( longitudeO instanceof Float ) {
	                	x = 0.0 + (Float)longitudeO;
	                }
	                else if ( latitudeO instanceof Integer ) {
	                	x = 0.0 + (Integer)latitudeO;
	                }
	                latitudeO = ts.getProperty(latitudeProperty);
	                if ( latitudeO == null ) {
	                    continue;
	                }
	                else if ( latitudeO instanceof Double ) {
	                	y = (Double)latitudeO;
	                }
	                else if ( latitudeO instanceof Float ) {
	                	y = 0.0 + (Float)latitudeO;
	                }
	                else if ( latitudeO instanceof Integer ) {
	                	y = 0.0 + (Integer)latitudeO;
	                }
	                if ( doElevation ) {
	                    elevationO = ts.getProperty(elevationProperty);
	                    if ( latitudeO instanceof Double ) {
	                    	z = (Double)elevationO;
	                    	haveElevation = true;
	                    }
	                    else if ( latitudeO instanceof Float ) {
	                    	z = 0.0 + (Float)elevationO;
	                    	haveElevation = true;
	                    }
	                    else if ( latitudeO instanceof Integer ) {
	                    	z = 0.0 + (Integer)elevationO;
	                    	haveElevation = true;
	                    }
	                }
	                if ( haveElevation ) {
	                	shape = pointzm = new GRPointZM();
	                	pointzm.x = x;
	                	pointzm.y = y;
	                	pointzm.z = z;
	                }
	                else {
	                	shape = point = new GRPoint ();
	                	point.x = x;
	                	point.y = y;
	                }
	            }
	            if ( doWkt ) {
	                // Extract shape from WKT
	                wkt0 = ts.getProperty(wktGeometryProperty);
	                if ( wkt0 != null ) {
	                	wkt = (String)wkt0;
		                // Parse WKT string needs to extract coordinates
		                //Message.printStatus(2, "", "Parsing \"" + wkt + "\"." );
		                shape = wktParser.parseWKT(wkt);
		                if ( shape == null ) {
		                    //Message.printStatus(2, "", "Shape from \"" + wkt + "\" is null." );
		                    continue;
		                }
	                }
	            }
	            // If get to here it is OK to output the feature and table columns as related properties.
	    	    fout.print( i2 + "{\n");
	    	    fout.print( i3 + "\"type\": \"Feature\",\n");
	    	    fout.print( i3 + "\"properties\": {\n");
	    		// Get all the properties.  Then extract the properties that match the IncludeProperties list
	    	    // - Do not output WKT property but do output latitude and longitude
	    		HashMap<String,Object> props = ts.getProperties();
	    		List<String> matchedProps = new ArrayList<String>();
	    		for ( int iprop = 0; iprop < includeProperties.length; iprop++ ) {
	    			for ( String key: props.keySet() ) {
	    				if ( key.matches(includeProperties[iprop]) ) {
	    					// Don't include WKT property
	    					if ( doWkt && key.equalsIgnoreCase(wktGeometryProperty) ) {
	    						continue;
	    					}
	    					// Make sure the property is not already in the list to include
	    					boolean match = false;
	    					for ( String p : matchedProps ) {
	    						if ( p.equals(key) ) {
	    							match = true;
	    							break;
	    						}
	    					}
	    					if ( !match ) {
	    						matchedProps.add(key);
	    					}
	    				}
	    			}
	    		}
	    		// Remove if in the exclude list
	    		for ( int iex = 0; iex < excludeProperties.length; iex++ ) {
		    		for ( int iprop = matchedProps.size(); iprop >= 0; --iprop ) {
		    			if ( excludeProperties[iex].matches(matchedProps.get(iprop)) ) {
		    				matchedProps.remove(iprop--);
		    			}
		    		}
	    		}
	    		// Output the properties that remain
	    		int iprop = -1;
	    		int nprop0 = matchedProps.size() - 1;
	    		for ( String prop : matchedProps ) {
	    	    	try {
	    	    		// Gson will properly output with quotes, etc.
	    	    		o = ts.getProperty(prop);
		    	    	fout.print( i4 + "\"" + prop + "\": " + gson.toJson(o) );
			    	    if ( iprop != nprop0 ) {
			    	    	fout.print ( ",\n" );
			    	    }
			    	    else {
			    	    	fout.print ( "\n" );
			    	    }
	    	    	}
	    	    	catch ( Exception e ) {
	    	    		continue;
	    	    	}
	    	    }
	    	    fout.print( i3 + "},\n");
	    	    // Output the geometry based on the shape type
	    	    fout.print( i3 + "\"geometry\": " + geoJSONFormatter.format(shape, true, i3) );
	    	    if ( its == nts0 ) {
	    	    	fout.print( i2 + "}\n");
	    	    }
	    	    else {
	    	    	fout.print( i2 + "},\n");
	    	    }
	        }
	        catch ( Exception e ) {
	            errors.add("Error adding shape (" + e + ")." );
	            Message.printWarning(3, "", e);
	            continue;
	        }
	    }
	    fout.print( i1 + "]\n"); // End features
	    if ( (javaScriptVar != null) && !javaScriptVar.isEmpty() ) {
	    	fout.print( "};" + appendText + "\n"); // End GeoJSON
	    }
	    else {
	    	fout.print( "}" + appendText + "\n"); // End GeoJSON
	    }
	}
	finally {
		try {
			fout.close();
		}
		catch ( Exception e ) {
		}
	}
}

}