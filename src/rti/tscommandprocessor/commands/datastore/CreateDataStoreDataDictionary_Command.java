package rti.tscommandprocessor.commands.datastore;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.DMI.ERDiagram_JFrame;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PrintUtil;
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the CreateDataStoreDataDictionary() command.
*/
public class CreateDataStoreDataDictionary_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Possible values for ViewERDiagram.
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
public CreateDataStoreDataDictionary_Command ()
{	super();
	setCommandName ( "CreateDataStoreDataDictionary" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{   String DataStore = parameters.getValue ( "DataStore" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String ERDiagramLayoutTableID = parameters.getValue ( "ERDiagramLayoutTableID" );
    String ERDiagramLayoutTableNameColumn = parameters.getValue ( "ERDiagramLayoutTableNameColumn" );
    String ERDiagramLayoutTableXColumn = parameters.getValue ( "ERDiagramLayoutTableXColumn" );
    String ERDiagramLayoutTableYColumn = parameters.getValue ( "ERDiagramLayoutTableYColumn" );
    String ERDiagramOrientation = parameters.getValue ( "ERDiagramOrientation" );
    String ViewERDiagram = parameters.getValue ( "ViewERDiagram" );

	String warning = "";
    String message;

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( (DataStore == null) || (DataStore.length() == 0) ) {
        message = "The data store must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the data store." ) );
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
    
    if ( (ERDiagramLayoutTableID != null) && !ERDiagramLayoutTableID.isEmpty() ) {
    	// Make sure the coordinate columns and page size are specified
    	if ( (ERDiagramLayoutTableNameColumn == null) || ERDiagramLayoutTableNameColumn.isEmpty() ) {
            message = "The layout table name column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the layout table name column." ) );
        }
    	if ( (ERDiagramLayoutTableXColumn == null) || ERDiagramLayoutTableXColumn.isEmpty() ) {
            message = "The layout table X column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the layout table X column." ) );
        }
    	if ( (ERDiagramLayoutTableYColumn == null) || ERDiagramLayoutTableYColumn.isEmpty() ) {
            message = "The layout table Y column must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the layout table Y column." ) );
        }
    	String landscape = PrintUtil.getOrientationAsString(PageFormat.LANDSCAPE);
    	String portrait = PrintUtil.getOrientationAsString(PageFormat.PORTRAIT);
        if ( (ERDiagramOrientation != null) && !ERDiagramOrientation.isEmpty() ) {
            if ( !ERDiagramOrientation.equalsIgnoreCase(landscape) && !ERDiagramOrientation.equalsIgnoreCase(portrait) ) {
                message = "The Orientation parameter \"" + ERDiagramOrientation + "\" is invalid.";
                warning += "\n" + message;
                status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the parameter as " + landscape + " or " + portrait + " (default)."));
            }
        }
        if ( (ViewERDiagram != null) && !ViewERDiagram.isEmpty() ) {
            if ( !ViewERDiagram.equalsIgnoreCase(_False) && !ViewERDiagram.equalsIgnoreCase(_True) ) {
                message = "The ViewERDiagram parameter \"" + ViewERDiagram + "\" is invalid.";
                warning += "\n" + message;
                status.addToLog(CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
            }
        }
    }
    
	//  Check for invalid parameters...
	List<String> validList = new ArrayList<String>(3);
    validList.add ( "DataStore" );
    validList.add ( "ReferenceTables" );
    validList.add ( "OutputFile" );
    validList.add ( "ERDiagramLayoutTableID" );
    validList.add ( "ERDiagramLayoutTableNameColumn" );
    validList.add ( "ERDiagramLayoutTableXColumn" );
    validList.add ( "ERDiagramLayoutTableYColumn" );
    validList.add ( "ERDiagramPageSize" );
    validList.add ( "ERDiagramOrientation" );
    validList.add ( "ViewERDiagram" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
	// The command will be modified if changed...
	return (new CreateDataStoreDataDictionary_JDialog ( parent, this, tableIDChoices )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
    List<File> list = new ArrayList<File>(1);
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

// Use base class parseCommand()

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
    status.clearLog(commandPhase);

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String DataStore = parameters.getValue ( "DataStore" );
    String ReferenceTables = parameters.getValue ( "ReferenceTables" );
    String [] referenceTables = null;
    List<String> referenceTablesList = new ArrayList<String>();
    if ( (ReferenceTables != null) && !ReferenceTables.equals("") ) {
    	referenceTables = ReferenceTables.split(",");
        for ( int i = 0; i < referenceTables.length; i++ ) {
        	referenceTablesList.add(referenceTables[i]);
        }
    }
    // TODO SAM 2015-05-10 Enable exclude tables
    List<String> excludeTables = null;
    String OutputFile = parameters.getValue("OutputFile");
    String ERDiagramLayoutTableID = parameters.getValue("ERDiagramLayoutTableID");
    String ERDiagramLayoutTableNameColumn = parameters.getValue("ERDiagramLayoutTableNameColumn");
    String ERDiagramLayoutTableXColumn = parameters.getValue("ERDiagramLayoutTableXColumn");
    String ERDiagramLayoutTableYColumn = parameters.getValue("ERDiagramLayoutTableYColumn");
    String ERDiagramPageSize = parameters.getValue("ERDiagramPageSize");
    String ERDiagramOrientation = parameters.getValue("ERDiagramOrientation");
    String ViewERDiagram = parameters.getValue("ViewERDiagram");
    boolean viewERDiagram = false;
    if ( (ViewERDiagram != null) && ViewERDiagram.equalsIgnoreCase("True") ) {
    	viewERDiagram = true;
    }
    
    // Find the data store to use...
    DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName ( DataStore, DatabaseDataStore.class );
    DMI dmi = null;
    if ( dataStore == null ) {
        message = "Could not get data store for name \"" + DataStore + "\" to query data.";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a database connection has been opened with name \"" +
                DataStore + "\"." ) );
    }
    else {
        dmi = ((DatabaseDataStore)dataStore).getDMI();
    }
    
    // Get the table to process for the ER diagram.

    DataTable layoutTable = null;
    PropList request_params = null;
    CommandProcessorRequestResultsBean bean = null;
    if ( (ERDiagramLayoutTableID != null) && !ERDiagramLayoutTableID.equals("") ) {
        // Get the table to be updated
        request_params = new PropList ( "" );
        request_params.set ( "TableID", ERDiagramLayoutTableID );
        try {
            bean = processor.processRequest( "GetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting GetTable(TableID=\"" + ERDiagramLayoutTableID + "\") from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Report problem to software support." ) );
        }
        PropList bean_PropList = bean.getResultsPropList();
        Object o_Table = bean_PropList.getContents ( "Table" );
        if ( o_Table == null ) {
            message = "Unable to find table to process using ERDiagramLayoutTableID=\"" + ERDiagramLayoutTableID + "\".";
            Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that a table exists with the requested ID." ) );
        }
        else {
            layoutTable = (DataTable)o_Table;
        }
    }
    
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}
	
    try {
        setOutputFile ( null );
        String OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile)));
        OutputFile_full = IOUtil.enforceFileExtension(OutputFile_full, "html");
        // Create the data dictionary.
        DMIUtil.createHTMLDataDictionary(dmi, OutputFile_full, referenceTables, excludeTables);
        // Save the output file name...
        setOutputFile ( new File(OutputFile_full));
        
        // TODO SAM 2015-05-09 Figure out how to create ER diagram output file
        // View the ER diagram
        if ( viewERDiagram ) {
        	String tableNameField = ERDiagramLayoutTableNameColumn;
        	String erdXField = ERDiagramLayoutTableXColumn;
        	String erdYField = ERDiagramLayoutTableYColumn;
        	PageFormat pageFormat = new PageFormat();
        	pageFormat.setOrientation(PageFormat.LANDSCAPE);
        	if ( ERDiagramOrientation.equalsIgnoreCase("" + PrintUtil.getOrientationAsString(PageFormat.PORTRAIT)) ) {
        		pageFormat.setOrientation(PageFormat.PORTRAIT);
        	}
        	Paper paper = new Paper();
        	//paper.
        	pageFormat.setPaper(paper);
        	boolean debug = false;
        	Message.printStatus(2, routine, "Creating ER diagram");
        	new ERDiagram_JFrame ( dmi, layoutTable, tableNameField,
        		erdXField, erdYField, referenceTablesList, pageFormat, debug );
        }
    }
    catch ( Exception e ) {
        message = "Error creating data dictionary for datastore \"" + DataStore + "\" (" + e + ").";
        Message.printWarning ( 2, routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that the database for data store \"" + DataStore +
                "\" has metadata defined and that database software supports Java metadata retrieval." ) );
        Message.printWarning ( 3, routine, e );
    }
    
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
    }

    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
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
{	if ( props == null ) {
		return getCommandName() + "()";
	}
	String DataStore = props.getValue( "DataStore" );
	String ReferenceTables = props.getValue( "ReferenceTables" );
	String OutputFile = props.getValue( "OutputFile" );
	String ERDiagramLayoutTableID = props.getValue( "ERDiagramLayoutTableID" );
	String ERDiagramLayoutTableNameColumn = props.getValue( "ERDiagramLayoutTableNameColumn" );
	String ERDiagramLayoutTableXColumn = props.getValue( "ERDiagramLayoutTableXColumn" );
	String ERDiagramLayoutTableYColumn = props.getValue( "ERDiagramLayoutTableYColumn" );
	String ERDiagramPageSize = props.getValue( "ERDiagramPageSize" );
	String ERDiagramOrientation = props.getValue( "ERDiagramOrientation" );
	String ViewERDiagram = props.getValue( "ViewERDiagram" );
	StringBuffer b = new StringBuffer ();
    if ( (DataStore != null) && (DataStore.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStore=\"" + DataStore + "\"" );
    }
    if ( (ReferenceTables != null) && (ReferenceTables.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ReferenceTables=\"" + ReferenceTables + "\"" );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }
    if ( (ERDiagramLayoutTableID != null) && !ERDiagramLayoutTableID.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ERDiagramLayoutTableID=\"" + ERDiagramLayoutTableID + "\"" );
    }
    if ( (ERDiagramLayoutTableNameColumn != null) && !ERDiagramLayoutTableNameColumn.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ERDiagramLayoutTableNameColumn=\"" + ERDiagramLayoutTableNameColumn + "\"" );
    }
    if ( (ERDiagramLayoutTableXColumn != null) && !ERDiagramLayoutTableXColumn.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ERDiagramLayoutTableXColumn=\"" + ERDiagramLayoutTableXColumn + "\"" );
    }
    if ( (ERDiagramLayoutTableYColumn != null) && !ERDiagramLayoutTableYColumn.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ERDiagramLayoutTableYColumn=\"" + ERDiagramLayoutTableYColumn + "\"" );
    }
    if ( (ERDiagramPageSize != null) && !ERDiagramPageSize.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ERDiagramPageSize=\"" + ERDiagramPageSize + "\"" );
    }
    if ( (ERDiagramOrientation != null) && !ERDiagramOrientation.isEmpty() ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ERDiagramOrientation=" + ERDiagramOrientation );
    }
    if ( (ViewERDiagram != null) && (ViewERDiagram.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ViewERDiagram=" + ViewERDiagram );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}