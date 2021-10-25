// CreateDataStoreDataDictionary_Command - This class initializes, checks, and runs the CreateDataStoreDataDictionary() command.

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

package rti.tscommandprocessor.commands.datastore;

import javax.swing.JFrame;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import RTi.DMI.DMI;
import RTi.DMI.DataDictionary;
import RTi.DMI.DatabaseDataStore;
import RTi.DMI.ERDiagram_JFrame;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.ResultSetToDataTableFactory;
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
    String DataStoreMetaTableForTables = parameters.getValue ( "DataStoreMetaTableForTables" );
    String DataStoreMetaTableForColumns = parameters.getValue ( "DataStoreMetaTableForColumns" );
    String MetaTableForTables = parameters.getValue ( "MetaTableForTables" );
    String MetaTableForColumns = parameters.getValue ( "MetaTableForColumns" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String SurroundWithPre = parameters.getValue ( "SurroundWithPre" );
    String EncodeHtmlChars = parameters.getValue ( "EncodeHtmlChars" );
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
        message = "The datastore must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the datastore." ) );
    }

    if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
        message = "The output file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify an output file." ) );
    }
    else if ( OutputFile.indexOf("${") < 0 ) {
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
    
    // Make sure that metadata are specified only once
    int count = 0;
    if ( (DataStoreMetaTableForTables != null) && !DataStoreMetaTableForTables.isEmpty() ) {
    	++count;
    }
    if ( (MetaTableForTables != null) && !MetaTableForTables.isEmpty() ) {
    	++count;
    }
    if ( count == 2 ) {
        message = "Specify DataStoreMetaTableForTables or MetaTableForTables, not both.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only one metadata table for tables."));
    }

    count = 0;
    if ( (DataStoreMetaTableForColumns != null) && !DataStoreMetaTableForColumns.isEmpty() ) {
    	++count;
    }
    if ( (MetaTableForColumns != null) && !MetaTableForColumns.isEmpty() ) {
    	++count;
    }
    if ( count == 2 ) {
        message = "Specify DataStoreMetaTableForColumns or MetaTableForColumns, not both.";
        warning += "\n" + message;
        status.addToLog(CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify only one metadata table for columns."));
    }
    
    if ( (SurroundWithPre != null) && !SurroundWithPre.isEmpty() ) {
        if ( !SurroundWithPre.equalsIgnoreCase(_False) && !SurroundWithPre.equalsIgnoreCase(_True) ) {
            message = "The SurroundWithPre parameter \"" + SurroundWithPre + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
        }
    }
    
    if ( (EncodeHtmlChars != null) && !EncodeHtmlChars.isEmpty() ) {
        if ( !EncodeHtmlChars.equalsIgnoreCase(_False) && !EncodeHtmlChars.equalsIgnoreCase(_True) ) {
            message = "The EncodeHtmlChars parameter \"" + EncodeHtmlChars + "\" is invalid.";
            warning += "\n" + message;
            status.addToLog(CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify the parameter as " + _False + " or " + _True + " (default)."));
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
	List<String> validList = new ArrayList<>(18);
    validList.add ( "DataStore" );
    validList.add ( "ReferenceTables" );
    validList.add ( "ExcludeTables" );
    validList.add ( "DataStoreMetaTableForTables" );
    validList.add ( "DataStoreMetaTableForColumns" );
    validList.add ( "MetaTableForTables" );
    validList.add ( "MetaTableForColumns" );
    validList.add ( "OutputFile" );
    validList.add ( "Newline" );
    validList.add ( "SurroundWithPre" );
    validList.add ( "EncodeHtmlChars" );
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

// TODO smalers 2021-10-24 remove when tested out.
/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
*/
/*
public boolean editCommand ( JFrame parent )
{	List<String> tableIDChoices = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)getCommandProcessor(), this);
	// The command will be modified if changed...
	return (new CreateDataStoreDataDictionary_JDialog ( parent, this, tableIDChoices )).ok();
}
*/

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent ) {
	String routine = getClass().getSimpleName() + ".editCommand";
	if ( Message.isDebugOn ) {
		Message.printDebug(1,routine,"Editing the command...getting active and discovery database datastores.");
	}
	List<DatabaseDataStore> dataStoreList =
		TSCommandProcessorUtil.getDatabaseDataStoresForEditors ( (TSCommandProcessor)this.getCommandProcessor(), this );
	List<String> tableIDChoices =
        TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)getCommandProcessor(), this);
	return (new CreateDataStoreDataDictionary_JDialog ( parent, this, tableIDChoices, dataStoreList )).ok();
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
    
	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
		status.clearLog(CommandPhaseType.RUN);
	}

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();

    String DataStore = parameters.getValue ( "DataStore" );
	if ( (DataStore != null) && (DataStore.indexOf("${") >= 0) && !DataStore.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		DataStore = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStore);
	}
    String ReferenceTables = parameters.getValue ( "ReferenceTables" );
	if ( (ReferenceTables != null) && (ReferenceTables.indexOf("${") >= 0) && !ReferenceTables.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		ReferenceTables = TSCommandProcessorUtil.expandParameterValue(processor, this, ReferenceTables);
	}
    String [] referenceTables = null;
    List<String> referenceTablesList = new ArrayList<String>();
    if ( (ReferenceTables != null) && !ReferenceTables.isEmpty() ) {
    	referenceTables = ReferenceTables.split(",");
        for ( int i = 0; i < referenceTables.length; i++ ) {
        	referenceTablesList.add(referenceTables[i]);
        }
    }
    String ExcludeTables = parameters.getValue ( "ExcludeTables" );
	if ( (ExcludeTables != null) && (ExcludeTables.indexOf("${") >= 0) && !ExcludeTables.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		ExcludeTables = TSCommandProcessorUtil.expandParameterValue(processor, this, ExcludeTables);
	}
    String [] excludeTables = null;
    List<String> excludeTablesList = new ArrayList<String>();
    if ( (ExcludeTables != null) && !ExcludeTables.isEmpty() ) {
    	excludeTables = ExcludeTables.split(",");
        for ( int i = 0; i < excludeTables.length; i++ ) {
        	excludeTablesList.add(excludeTables[i]);
        }
    }
    String DataStoreMetaTableForTables = parameters.getValue ( "DataStoreMetaTableForTables" );
	if ( (DataStoreMetaTableForTables != null) && (DataStoreMetaTableForTables.indexOf("${") >= 0) && !DataStoreMetaTableForTables.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		DataStoreMetaTableForTables = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreMetaTableForTables);
	}
    String DataStoreMetaTableForColumns = parameters.getValue ( "DataStoreMetaTableForColumns" );
	if ( (DataStoreMetaTableForColumns != null) && (DataStoreMetaTableForColumns.indexOf("${") >= 0) && !DataStoreMetaTableForColumns.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		DataStoreMetaTableForColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, DataStoreMetaTableForColumns);
	}
    String MetaTableForTables = parameters.getValue ( "MetaTableForTables" );
	if ( (MetaTableForTables != null) && (MetaTableForTables.indexOf("${") >= 0) && !MetaTableForTables.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		MetaTableForTables = TSCommandProcessorUtil.expandParameterValue(processor, this, MetaTableForTables);
	}
    String MetaTableForColumns = parameters.getValue ( "MetaTableForColumns" );
	if ( (MetaTableForColumns != null) && (MetaTableForColumns.indexOf("${") >= 0) && !MetaTableForColumns.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		MetaTableForColumns = TSCommandProcessorUtil.expandParameterValue(processor, this, MetaTableForColumns);
	}
    String OutputFile = parameters.getValue("OutputFile"); // Expanded below
    String Newline = parameters.getValue("Newline");
    String SurroundWithPre = parameters.getValue("SurroundWithPre");
    boolean surroundWithPre = false;
    if ( (SurroundWithPre != null) && SurroundWithPre.equalsIgnoreCase("True") ) {
    	surroundWithPre = true;
    }
    String EncodeHtmlChars = parameters.getValue("EncodeHtmlChars");
    boolean encodeHtmlChars = true;
    if ( (EncodeHtmlChars != null) && EncodeHtmlChars.equalsIgnoreCase("False") ) {
    	encodeHtmlChars = false;
    }
    String ERDiagramLayoutTableID = parameters.getValue("ERDiagramLayoutTableID");
	if ( (ERDiagramLayoutTableID != null) && (ERDiagramLayoutTableID.indexOf("${") >= 0) && !ERDiagramLayoutTableID.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		ERDiagramLayoutTableID = TSCommandProcessorUtil.expandParameterValue(processor, this, ERDiagramLayoutTableID);
	}
    String ERDiagramLayoutTableNameColumn = parameters.getValue("ERDiagramLayoutTableNameColumn");
	if ( (ERDiagramLayoutTableNameColumn != null) && (ERDiagramLayoutTableNameColumn.indexOf("${") >= 0) && !ERDiagramLayoutTableNameColumn.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		ERDiagramLayoutTableNameColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, ERDiagramLayoutTableNameColumn);
	}
    String ERDiagramLayoutTableXColumn = parameters.getValue("ERDiagramLayoutTableXColumn");
	if ( (ERDiagramLayoutTableXColumn != null) && (ERDiagramLayoutTableXColumn.indexOf("${") >= 0) && !ERDiagramLayoutTableXColumn.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		ERDiagramLayoutTableXColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, ERDiagramLayoutTableXColumn);
	}
    String ERDiagramLayoutTableYColumn = parameters.getValue("ERDiagramLayoutTableYColumn");
	if ( (ERDiagramLayoutTableYColumn != null) && (ERDiagramLayoutTableYColumn.indexOf("${") >= 0) && !ERDiagramLayoutTableYColumn.isEmpty() && (commandPhase == CommandPhaseType.RUN)) {
		ERDiagramLayoutTableYColumn = TSCommandProcessorUtil.expandParameterValue(processor, this, ERDiagramLayoutTableYColumn);
	}
    //String ERDiagramPageSize = parameters.getValue("ERDiagramPageSize");
    String ERDiagramOrientation = parameters.getValue("ERDiagramOrientation");
    String ViewERDiagram = parameters.getValue("ViewERDiagram");
    boolean viewERDiagram = false;
    if ( (ViewERDiagram != null) && ViewERDiagram.equalsIgnoreCase("True") ) {
    	viewERDiagram = true;
    }
    
    // Find the datastore to use...
    DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName ( DataStore, DatabaseDataStore.class );
    DMI dmi = null;
    if ( dataStore == null ) {
        message = "Could not get datastore for name \"" + DataStore + "\" to query data.";
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
        // Read the metadata tables
        DataTable metadataForTables = null;
        DataTable metadataForColumns = null;
        if ( (DataStoreMetaTableForTables != null) && !DataStoreMetaTableForTables.isEmpty() ) {
        	// Read the metadata table from the datastore.
        	String queryString = "SELECT * from " + DataStoreMetaTableForTables;
            ResultSet rs = dmi.dmiSelect(queryString);
           	ResultSetToDataTableFactory factory = new ResultSetToDataTableFactory();
           	metadataForTables = factory.createDataTable(dmi.getDatabaseEngineType(), rs, DataStoreMetaTableForTables);
        }
        else if ( (MetaTableForTables != null) && !MetaTableForTables.isEmpty() ) {
        	// Get the metadata table (for tables) from processor.
	        request_params = new PropList ( "" );
	        request_params.set ( "TableID", MetaTableForTables );
	        try {
	            bean = processor.processRequest( "GetTable", request_params);
	        }
	        catch ( Exception e ) {
	            message = "Error requesting GetTable(TableID=\"" + MetaTableForTables + "\") from processor.";
	            Message.printWarning(warning_level,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Report problem to software support." ) );
	        }
	        PropList bean_PropList = bean.getResultsPropList();
	        Object o_Table = bean_PropList.getContents ( "Table" );
	        if ( o_Table == null ) {
	            message = "Unable to find table to process using TableID=\"" + MetaTableForTables + "\".";
	            Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify that a table exists with the requested ID." ) );
	        }
	        else {
	            metadataForTables = (DataTable)o_Table;
	        }
        }
        if ( (DataStoreMetaTableForTables != null) && !DataStoreMetaTableForTables.isEmpty() ) {
        	// Read the metadata table from the datastore.
        	String queryString = "SELECT * from " + DataStoreMetaTableForColumns;
            ResultSet rs = dmi.dmiSelect(queryString);
           	ResultSetToDataTableFactory factory = new ResultSetToDataTableFactory();
           	metadataForColumns = factory.createDataTable(dmi.getDatabaseEngineType(), rs, DataStoreMetaTableForColumns);
        }
        else if ( (MetaTableForColumns != null) && !MetaTableForColumns.isEmpty() ) {
        	// Get the metadata table (for columns) from processor.
	        request_params = new PropList ( "" );
	        request_params.set ( "TableID", MetaTableForColumns );
	        try {
	            bean = processor.processRequest( "GetTable", request_params);
	        }
	        catch ( Exception e ) {
	            message = "Error requesting GetTable(TableID=\"" + MetaTableForColumns + "\") from processor.";
	            Message.printWarning(warning_level,
	                MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Report problem to software support." ) );
	        }
	        PropList bean_PropList = bean.getResultsPropList();
	        Object o_Table = bean_PropList.getContents ( "Table" );
	        if ( o_Table == null ) {
	            message = "Unable to find table to process using TableID=\"" + MetaTableForColumns + "\".";
	            Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag( command_tag,++warning_count), routine, message );
	            status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify that a table exists with the requested ID." ) );
	        }
	        else {
	            metadataForColumns = (DataTable)o_Table;
	        }
        }

        // Create the data dictionary.
        DataDictionary dd = new DataDictionary();
        dd.createHTMLDataDictionary(dmi, OutputFile_full, Newline, surroundWithPre, encodeHtmlChars,
        	referenceTablesList, excludeTablesList, metadataForTables, metadataForColumns);
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
                message, "Verify that the database for datastore \"" + DataStore +
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
	String ExcludeTables = props.getValue( "ExcludeTables" );
	String DataStoreMetaTableForTables = props.getValue( "DataStoreMetaTableForTables" );
	String DataStoreMetaTableForColumns = props.getValue( "DataStoreMetaTableForColumns" );
	String MetaTableForTables = props.getValue( "MetaTableForTables" );
	String MetaTableForColumns = props.getValue( "MetaTableForColumns" );
	String OutputFile = props.getValue( "OutputFile" );
	String Newline = props.getValue( "Newline" );
	String SurroundWithPre = props.getValue( "SurroundWithPre" );
	String EncodeHtmlChars = props.getValue( "EncodeHtmlChars" );
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
    if ( (ExcludeTables != null) && (ExcludeTables.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ExcludeTables=\"" + ExcludeTables + "\"" );
    }
    if ( (DataStoreMetaTableForTables != null) && (DataStoreMetaTableForTables.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreMetaTableForTables=\"" + DataStoreMetaTableForTables + "\"" );
    }
    if ( (DataStoreMetaTableForColumns != null) && (DataStoreMetaTableForColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "DataStoreMetaTableForColumns=\"" + DataStoreMetaTableForColumns + "\"" );
    }
    if ( (MetaTableForTables != null) && (MetaTableForTables.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MetaTableForTables=\"" + MetaTableForTables + "\"" );
    }
    if ( (MetaTableForColumns != null) && (MetaTableForColumns.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MetaTableForColumns=\"" + MetaTableForColumns + "\"" );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }
    if ( (SurroundWithPre != null) && (SurroundWithPre.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SurroundWithPre=" + SurroundWithPre );
    }
    if ( (Newline != null) && (Newline.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Newline=\"" + Newline + "\"" );
    }
    if ( (EncodeHtmlChars != null) && (EncodeHtmlChars.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EncodeHtmlChars=" + EncodeHtmlChars );
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