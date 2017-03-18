package rti.tscommandprocessor.commands.spatial;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.GIS.GeoView.WKTGeometryParser;
import RTi.GR.GRPoint;
import RTi.GR.GRPointZM;
import RTi.GR.GRPolygon;
import RTi.GR.GRShape;
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
import RTi.Util.IO.HTMLUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the WriteTableToKml() command.
*/
public class WriteTableToKml_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteTableToKml_Command ()
{   super();
    setCommandName ( "WriteTableToKml" );
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
{   String TableID = parameters.getValue ( "TableID" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String LongitudeColumn = parameters.getValue ( "LongitudeColumn" );
    String LatitudeColumn = parameters.getValue ( "LatitudeColumn" );
    String WKTGeometryColumn = parameters.getValue ( "WKTGeometryColumn" );
    String StyleFile = parameters.getValue ( "StyleFile" );
    String warning = "";
    String routine = getCommandName() + ".checkCommandParameters";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the table identifier." ) );
    }
    
    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file must be specified.";
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
    
    if ( ((LongitudeColumn == null) || (LongitudeColumn.length() == 0)) &&
        ((WKTGeometryColumn == null) || (WKTGeometryColumn.length() == 0)) ) {
        message = "The longitude column OR WKT geometry column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the longitude OR WKT geometry column." ) );
    }
    
    if ( ((LongitudeColumn != null) && (LongitudeColumn.length() != 0)) &&
        ((WKTGeometryColumn != null) && (WKTGeometryColumn.length() != 0)) ) {
        message = "The longitude column OR WKT geometry column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the longitude OR WKT geometry column." ) );
    }
    
    if ( ((LatitudeColumn == null) || (LatitudeColumn.length() == 0)) &&
        ((WKTGeometryColumn == null) || (WKTGeometryColumn.length() == 0)) ) {
        message = "The latitude column OR WKT geometry column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the latitude column OR WKT geometry column." ) );
    }
    
    if ( ((LatitudeColumn != null) && (LatitudeColumn.length() != 0)) &&
        ((WKTGeometryColumn != null) && (WKTGeometryColumn.length() != 0)) ) {
        message = "The latitude column OR WKT geometry column must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the latitude column OR WKT geometry column." ) );
    }
    
    String StyleFile_full = null;
    if ( (StyleFile != null) && (StyleFile.length() != 0) ) {
        String working_dir = null;
        try {
            Object o = processor.getPropContents ( "WorkingDir" );
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
                        message, "Specify an existing style file." ) );
            }
    
        try {
            StyleFile_full = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,StyleFile)));
            File f = new File ( StyleFile_full );
            if ( !f.exists() ) {
                message = "The style file does not exist:  \"" + StyleFile_full + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the style file exists - may be OK if created at run time." ) );
            }
        }
        catch ( Exception e ) {
            message = "The style file:\n" + "    \"" + StyleFile +
            "\"\ncannot be adjusted using the working directory:\n" + "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                     message, "Verify that style file and working directory paths are compatible." ) );
        }
    }
    
    // Check for invalid parameters...
    List<String> validList = new ArrayList<String>(14);
    validList.add ( "TableID" );
    validList.add ( "OutputFile" );
    validList.add ( "Name" );
    validList.add ( "Description" );
    validList.add ( "PlacemarkNameColumn" );
    validList.add ( "PlacemarkDescriptionColumn" );
    validList.add ( "LongitudeColumn" );
    validList.add ( "LatitudeColumn" );
    validList.add ( "ElevationColumn" );
    validList.add ( "WKTGeometryColumn" );
    validList.add ( "GeometryInsert" );
    validList.add ( "StyleInsert" );
    validList.add ( "StyleFile" );
    validList.add ( "StyleUrl" );
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
{   List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
    // The command will be modified if changed...
    return (new WriteTableToKml_JDialog ( parent, this, tableIDChoices )).ok();
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
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{   String routine = "WriteTableToKml_Command.runCommand", message;
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    
    // Clear the output file
    
    setOutputFile ( null );
    
    // Check whether the processor wants output files to be created...

    CommandProcessor processor = getCommandProcessor();
    if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
            Message.printStatus ( 2, routine,
            "Skipping \"" + toString() + "\" because output is not being created." );
    }
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.RUN);

    PropList parameters = getCommandParameters();
    String TableID = parameters.getValue ( "TableID" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String Name = parameters.getValue ( "Name" );
    String Description = parameters.getValue ( "Description" );
    String PlacemarkNameColumn = parameters.getValue ( "PlacemarkNameColumn" );
    String PlacemarkDescriptionColumn = parameters.getValue ( "PlacemarkDescriptionColumn" );
    String LongitudeColumn = parameters.getValue ( "LongitudeColumn" );
    String LatitudeColumn = parameters.getValue ( "LatitudeColumn" );
    String ElevationColumn = parameters.getValue ( "ElevationColumn" );
    String WKTGeometryColumn = parameters.getValue ( "WKTGeometryColumn" );
    String GeometryInsert = parameters.getValue ( "GeometryInsert" );
    String StyleInsert = parameters.getValue ( "StyleInsert" );
    String StyleFile = parameters.getValue ( "StyleFile" );
    String StyleUrl = parameters.getValue ( "StyleUrl" );
    
    // Get the table to process.

    DataTable table = null;
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    if ( commandPhase == CommandPhaseType.RUN ) {
        PropList request_params = null;
        CommandProcessorRequestResultsBean bean = null;
        if ( (TableID != null) && !TableID.equals("") ) {
            // Get the table to be updated
            request_params = new PropList ( "" );
            request_params.set ( "TableID", TableID );
            try {
                bean = processor.processRequest( "GetTable", request_params);
            }
            catch ( Exception e ) {
                message = "Error requesting GetTable(TableID=\"" + TableID + "\") from processor.";
                Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Report problem to software support." ) );
            }
            PropList bean_PropList = bean.getResultsPropList();
            Object o_Table = bean_PropList.getContents ( "Table" );
            if ( o_Table == null ) {
                message = "Unable to find table to process using TableID=\"" + TableID + "\".";
                Message.printWarning ( warning_level,
                MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Verify that a table exists with the requested ID." ) );
            }
            else {
                table = (DataTable)o_Table;
            }
        }
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings for command parameters.";
        Message.printWarning ( 2,
        MessageUtil.formatMessageTag(command_tag, ++warning_count),
        routine,message);
        throw new InvalidCommandParameterException ( message );
    }

    String OutputFile_full = OutputFile;
    try {
        // Convert to an absolute path...
        OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
        Message.printStatus ( 2, routine, "Writing KML file \"" + OutputFile_full + "\"" );
        List<String> errors = new Vector<String>();
        String StyleFile_full = null;
        if ( (StyleFile != null) && !StyleFile.equals("") ) {
            StyleFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                    TSCommandProcessorUtil.expandParameterValue(processor,this,StyleFile)));
            if ( !IOUtil.fileReadable(StyleFile_full) || !IOUtil.fileExists(StyleFile_full)) {
                message = "Style file \"" + StyleFile_full + "\" is not found or accessible.";
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count ), routine, message );
                status.addToLog(commandPhase,
                    new CommandLogRecord( CommandStatusType.FAILURE, message,
                        "Verify that the file exists and is readable."));
                throw new CommandException ( message );
            }
        }
        writeTableToKml ( table, OutputFile_full, Name, Description,
            GeometryInsert,
            StyleInsert, StyleFile_full, StyleUrl,
            PlacemarkNameColumn, PlacemarkDescriptionColumn,
            LongitudeColumn, LatitudeColumn, ElevationColumn, WKTGeometryColumn, errors );
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
        message = "Unexpected error writing time series to KML file \"" + OutputFile_full + "\" (" + e + ")";
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
    String TableID = parameters.getValue( "TableID" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String Name = parameters.getValue ( "Name" );
    String Description = parameters.getValue ( "Description" );
    String PlacemarkNameColumn = parameters.getValue ( "PlacemarkNameColumn" );
    String PlacemarkDescriptionColumn = parameters.getValue ( "PlacemarkDescriptionColumn" );
    String LongitudeColumn = parameters.getValue ( "LongitudeColumn" );
    String LatitudeColumn = parameters.getValue ( "LatitudeColumn" );
    String ElevationColumn = parameters.getValue ( "ElevationColumn" );
    String WKTGeometryColumn = parameters.getValue ( "WKTGeometryColumn" );
    String GeometryInsert = parameters.getValue ( "GeometryInsert" );
    String StyleInsert = parameters.getValue ( "StyleInsert" );
    String StyleFile = parameters.getValue ( "StyleFile" );
    String StyleUrl = parameters.getValue ( "StyleUrl" );
    StringBuffer b = new StringBuffer ();
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }
    if ( (Name != null) && (Name.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Name=\"" + Name + "\"" );
    }
    if ( (Description != null) && (Description.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Description=\"" + Description + "\"" );
    }
    if ( (PlacemarkNameColumn != null) && (PlacemarkNameColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PlacemarkNameColumn=\"" + PlacemarkNameColumn + "\"" );
    }
    if ( (PlacemarkDescriptionColumn != null) && (PlacemarkDescriptionColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PlacemarkDescriptionColumn=\"" + PlacemarkDescriptionColumn + "\"" );
    }
    if ( (LongitudeColumn != null) && (LongitudeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LongitudeColumn=\"" + LongitudeColumn + "\"" );
    }
    if ( (LatitudeColumn != null) && (LatitudeColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "LatitudeColumn=\"" + LatitudeColumn + "\"" );
    }
    if ( (ElevationColumn != null) && (ElevationColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ElevationColumn=\"" + ElevationColumn + "\"" );
    }
    if ( (WKTGeometryColumn != null) && (WKTGeometryColumn.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "WKTGeometryColumn=\"" + WKTGeometryColumn + "\"" );
    }
    if ( (GeometryInsert != null) && (GeometryInsert.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "GeometryInsert=\"" + GeometryInsert + "\"" );
    }
    if ( (StyleInsert != null) && (StyleInsert.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StyleInsert=\"" + StyleInsert + "\"" );
    }
    if ( (StyleFile != null) && (StyleFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StyleFile=\"" + StyleFile + "\"" );
    }
    if ( (StyleUrl != null) && (StyleUrl.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "StyleUrl=\"" + StyleUrl + "\"" );
    }
    return getCommandName() + "(" + b.toString() + ")";
}

// TODO SAM 2013-07-01 Evaluate whether a separate package should be created - for now keep the code here
// until there is time to split out
/**
Write the table to a KML file.
*/
private void writeTableToKml ( DataTable table, String outputFile, String name, String description,
    String geometryInsert,
    String styleInsert, String styleFile, String styleUrl,
    String placemarkNameColumn, String placemarkDescriptionColumn,
    String longitudeColumn, String latitudeColumn, String elevationColumn, String wktGeometryColumn, List<String> errors )
{   PrintWriter fout = null;
    try {
        FileOutputStream fos = new FileOutputStream ( outputFile );
        fout = new PrintWriter ( fos );
        // For now just support KML 2
        writeTableToKml02 ( fout, table, name, description,
            geometryInsert,
            styleInsert, styleFile, styleUrl, placemarkNameColumn, placemarkDescriptionColumn,
            longitudeColumn, latitudeColumn, elevationColumn, wktGeometryColumn, errors );
    }
    catch ( FileNotFoundException e ) {
        errors.add ( "Output file \"" + outputFile + "\" could not be created (" + e + ")." );
    }
    finally {
        try {
            fout.close();
        }
        catch ( Exception e ) {
        }
    } 
}

/**
Write the version 02 format KML.
@param fout open PrintWriter to write to
@param table data table to write
@param errors list of error strings to be propagated to calling code
*/
private void writeTableToKml02 ( PrintWriter fout, DataTable table, String name, String description,
    String geometryInsert,
    String styleInsert, String styleFile, String styleUrl,
    String placemarkNameColumn, String placemarkDescriptionColumn, 
    String longitudeColumn, String latitudeColumn, String elevationColumn, String wktGeometryColumn, List<String> errors )
{   // Get the column numbers corresponding to the column names
    int errorCount = 0;
    if ( (name == null) || name.equals("") ) {
        name = table.getTableID();
    }
    if ( description == null ) {
        description = "";
    }
    boolean doPoint = false;
    boolean doWkt = false;
    boolean doElevation = false;
    boolean doPlacemarkName = false;
    boolean doPlacemarkDescription = false;
    boolean doStyleUrl = false;
    // WKT trumps point data
    if ( (wktGeometryColumn != null) && !wktGeometryColumn.equals("") ) {
        doWkt = true;
    }
    else {
        // Rely on point data
        if ( (latitudeColumn != null) && !latitudeColumn.equals("") &&
            (longitudeColumn != null) && !longitudeColumn.equals("")) {
            doPoint = true;
        }
        if ( (elevationColumn != null) && !elevationColumn.equals("") ) {
            doElevation = true;
        }
    }
    if ( (placemarkNameColumn != null) && !placemarkNameColumn.equals("") ) {
        doPlacemarkName = true;
    }
    if ( (placemarkDescriptionColumn != null) && !placemarkDescriptionColumn.equals("") ) {
        doPlacemarkDescription = true;
    }
    if ( (styleUrl != null) && !styleUrl.equals("") ) {
        doStyleUrl = true;
    }
    int placemarkNameColNum = -1;
    try {
        if ( doPlacemarkName ) {
            placemarkNameColNum = table.getFieldIndex(placemarkNameColumn);
            if ( placemarkNameColNum < 0 ) {
                errors.add ( "Placemark column \"" + placemarkNameColumn + "\" not found in table.");
                ++errorCount;
            }
        }
    }
    catch ( Exception e ) {
        errors.add ( "Error determining elevation column number \"" + elevationColumn + "\" (" + e + ").");
    }
    int placemarkDescriptionColNum = -1;
    try {
        if ( doPlacemarkDescription ) {
            placemarkDescriptionColNum = table.getFieldIndex(placemarkDescriptionColumn);
            if ( placemarkDescriptionColNum < 0 ) {
                errors.add ( "Placemark column \"" + placemarkDescriptionColumn + "\" not found in table.");
                ++errorCount;
            }
        }
    }
    catch ( Exception e ) {
        errors.add ( "Error determining elevation column number \"" + elevationColumn + "\" (" + e + ").");
    }
    int longitudeColNum = -1;
    int latitudeColNum = -1;
    if ( doPoint ) {
        try {
            longitudeColNum = table.getFieldIndex(longitudeColumn);
            if ( longitudeColNum < 0 ) {
                errors.add ( "Longitude column \"" + longitudeColumn + "\" not found in table.");
                ++errorCount;
            }
        }
        catch ( Exception e ) {
            errors.add ( "Error determining longitude column number \"" + longitudeColumn + "\" (" + e + ").");
        }
        try {
            latitudeColNum = table.getFieldIndex(latitudeColumn);
            if ( latitudeColNum < 0 ) {
                errors.add ( "Latitude column \"" + latitudeColumn + "\" not found in table.");
                ++errorCount;
            }
        }
        catch ( Exception e ) {
            errors.add ( "Error determining latitude column number \"" + latitudeColumn + "\" (" + e + ").");
        }
    }
    int elevationColNum = -1;
    try {
        if ( doElevation ) {
            elevationColNum = table.getFieldIndex(elevationColumn);
            if ( elevationColNum < 0 ) {
                errors.add ( "Elevation column \"" + elevationColumn + "\" not found in table.");
                ++errorCount;
            }
        }
    }
    catch ( Exception e ) {
        errors.add ( "Error determining elevation column number \"" + elevationColumn + "\" (" + e + ").");
    }
    int wktGeometryColNum = -1;
    if ( doWkt ) {
        try {
            wktGeometryColNum = table.getFieldIndex(wktGeometryColumn);
            if ( wktGeometryColNum < 0 ) {
                errors.add ( "WKT geometry column \"" + wktGeometryColumn + "\" not found in table.");
                ++errorCount;
            }
        }
        catch ( Exception e ) {
            errors.add ( "Error determining WKT geometry column number \"" + wktGeometryColumn + "\" (" + e + ").");
        }
    }
    if ( errorCount > 0 ) {
        // Don't have needed input
        return;
    }
    // Write the header
    String i1 = " ";
    String i2 = "  ";
    String i3 = "   ";
    String i4 = "    ";
    String i5 = "     ";
    String i6 = "      ";
    String i7 = "       ";
    if ( name.startsWith("<") ) {
        name = "<![CDATA[\n" + name + "]]>";
    }
    if ( description.startsWith("<") ) {
        description = "<![CDATA[\n" + description + "]]>";
    }
    fout.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
        "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
        i1 + "<Document>\n" +
        i2 + "<name>" + name + "</name>\n" +
        i2 + "<description>" + description + "</description>\n" );
    // Write the styles based on supplied information
    if ( (styleInsert != null) && !styleInsert.equals("") ) {
        fout.write(i2 + styleInsert + "\n");
    }
    else if ( (styleFile != null) && !styleFile.equals("") ) {
        // Insert styles from the contents of the SQL file
        try {
            List<String> list = IOUtil.fileToStringList(styleFile);
            for ( String line : list ) {
                fout.write(i2 + line + "\n" );
            }
        }
        catch ( Exception e ) {
            errors.add("Error adding style file to KML (" + e + ")." ); 
        }
    }
    int nRows = table.getNumberOfRecords();
    TableRecord rec;
    Object placemarkNameO = null, placemarkDescriptionO = null,
        latitudeO, longitudeO, elevationO = null; // Can be double, int, or string
    WKTGeometryParser wktParser = null;
    String wkt = null;
    if ( doWkt ) {
        wktParser = new WKTGeometryParser();
    }
    for ( int iRow = 0; iRow < nRows; iRow++ ) {
        try {
            rec = table.getRecord(iRow);
            longitudeO = null;
            latitudeO = null;
            if ( doPlacemarkName ) {
                placemarkNameO = rec.getFieldValue(placemarkNameColNum);
            }
            if ( doPlacemarkDescription ) {
                placemarkDescriptionO = rec.getFieldValue(placemarkDescriptionColNum);
            }
            if ( doPoint ) {
                // Table columns can be any type because objects are treated as strings below
                longitudeO = rec.getFieldValue(longitudeColNum);
                if ( longitudeO == null ) {
                    continue;
                }
                latitudeO = rec.getFieldValue(latitudeColNum);
                if ( latitudeO == null ) {
                    continue;
                }
                if ( doElevation ) {
                    elevationO = rec.getFieldValue(elevationColNum);
                }
            }
            if ( doWkt ) {
                // Extract and output KML below
                wkt = rec.getFieldValueString(wktGeometryColNum);  
            }
        }
        catch ( Exception e ) {
            errors.add("Error adding geometry KML (" + e + ")." );
            Message.printWarning(3, "", e);
            continue;
        }
        if ( placemarkNameO == null ) {
            placemarkNameO = "";
        }
        if ( placemarkDescriptionO == null ) {
            placemarkDescriptionO = "";
        }
        // If the description contains HTML (but not elements like "&lt;", surround as CDATA
        if ( ((String)placemarkDescriptionO).startsWith("<") ) {
            placemarkDescriptionO = "<![CDATA[\n" + placemarkDescriptionO + "]]>";
        }
        if ( elevationO == null ) {
            elevationO = "0";
        }
        fout.write(i2 + "<Placemark>\n");
        // TODO SAM 2013-07-01 use formatting strings for name
        fout.write(i3 + "<name>" + HTMLUtil.text2html("" + placemarkNameO, false) + "</name>\n");
        fout.write(i3 + "<description>" + HTMLUtil.text2html("" + placemarkDescriptionO, false) + "</description>\n");
        if ( doStyleUrl ) {
            fout.write(i3 + "<styleUrl>" + styleUrl + "</styleUrl>\n");
        }
        if ( doPoint ) {
            fout.write(i3 + "<Point>\n");
            if ( (geometryInsert != null) && !geometryInsert.equals("") ) {
                fout.write(i4 + geometryInsert);
            }
            fout.write(i4 + "<coordinates>" + longitudeO + "," + latitudeO + "," + elevationO + "</coordinates>\n");
            fout.write(i3 + "</Point>\n");
        }
        else if ( doWkt ) {
            // Parse WKT string needs to extract coordinates
            //Message.printStatus(2, "", "Parsing \"" + wkt + "\"." );
            GRShape shape = wktParser.parseWKT(wkt);
            if ( shape == null ) {
                Message.printStatus(2, "", "Shape from \"" + wkt + "\" is null." );
            }
            if ( shape != null ) {
                if ( shape instanceof GRPoint ) {
                    //Message.printStatus(2, "", "Shape is POINT." );
                    GRPoint pt = (GRPoint)shape;
                    longitudeO = new Double(pt.x);
                    latitudeO = new Double(pt.y);
                    if ( shape instanceof GRPointZM ) {
                        elevationO = new Double(((GRPointZM)pt).z);
                    }
                    fout.write(i3 + "<Point>\n");
                    if ( (geometryInsert != null) && !geometryInsert.equals("") ) {
                        fout.write(i4 + geometryInsert);
                    }
                    fout.write(i4 + "<coordinates>" + longitudeO + "," + latitudeO + "," + elevationO + "</coordinates>\n");
                    fout.write(i3 + "</Point>\n");
                }
                else if ( shape instanceof GRPolygon ) {
                    //Message.printStatus(2, "", "Shape is POLYGON." );
                    GRPolygon p = (GRPolygon)shape;
                    fout.write(i3 + "<Polygon>\n");
                    if ( (geometryInsert != null) && !geometryInsert.equals("") ) {
                        fout.write(i4 + geometryInsert);
                    }
                    fout.write(i4 + "<outerBoundaryIs>\n");
                    fout.write(i5 + "<LinearRing>\n");
                    fout.write(i6 + "<coordinates>\n");
                    Object latitudeO0 = null, longitudeO0 = null, elevationO0 = null;
                    for ( int i = 0; i < p.npts; i++ ) {
                        longitudeO = new Double(p.pts[i].x);
                        latitudeO = new Double(p.pts[i].y);
                        elevationO = null;
                        //if ( shape instanceof GRPolygonZM ) {
                        //    elevationO = new Double(((GRPointZM)p.pts[i]).z);
                        //}
                        if ( i == 0 ) {
                            // Save first point and output at end if needed to close ring
                            longitudeO0 = longitudeO;
                            latitudeO0 = latitudeO;
                            elevationO0 = elevationO;
                        }
                        if ( elevationO == null ) {
                            fout.write(i7 + longitudeO + "," + latitudeO + "\n");
                        }
                        else {
                            fout.write(i7 + longitudeO + "," + latitudeO + "," + elevationO + "\n");
                        }
                    }
                    // Write the first point again if it was not written at the end
                    if ( elevationO == null ) {
                        if ( !longitudeO.equals(longitudeO0) || !latitudeO.equals(latitudeO0) ) {
                            fout.write(i7 + longitudeO0 + "," + latitudeO0 + "\n");
                        }
                    }
                    else {
                        if ( !longitudeO.equals(longitudeO0) || !latitudeO.equals(latitudeO0)
                            || !elevationO.equals(elevationO0) ) {
                            fout.write(i7 + longitudeO0 + "," + latitudeO0 + "," + elevationO0 + "\n");
                        }
                    }
                    fout.write(i6 + "</coordinates>\n");
                    fout.write(i5 + "</LinearRing>\n");
                    fout.write(i4 + "</outerBoundaryIs>\n");
                    fout.write(i3 + "</Polygon>\n");
                }
                else {
                    Message.printStatus(2,"","Unknown shape for \"" + wkt + "\"");
                }
            }
        }
        fout.write(i2 + "</Placemark>\n");
    }
    fout.write( i1 + "</Document>\n" );   
    fout.write( "</kml>\n" );
}

}