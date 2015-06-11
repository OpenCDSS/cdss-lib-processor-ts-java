package rti.tscommandprocessor.commands.template;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleSequence;
import freemarker.template.Template;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;

/**
This class initializes, checks, and runs the ExpandTemplateFile() command.
*/
public class ExpandTemplateFile_Command extends AbstractCommand
implements Command, CommandDiscoverable, FileGenerator, ObjectListProvider
{
    
protected final String _False = "False";
protected final String _True = "True";

/**
Property set during discovery.
*/
private Prop __discoveryProp = null;
    
/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public ExpandTemplateFile_Command ()
{	super();
	setCommandName ( "ExpandTemplateFile" );
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
{	String InputFile = parameters.getValue ( "InputFile" );
    String InputText = parameters.getValue ( "InputText" );
    String OutputFile = parameters.getValue ( "OutputFile" );
    String OutputProperty = parameters.getValue ( "OutputProperty" );
    String UseTables = parameters.getValue ( "UseTables" );
    String ListInResults = parameters.getValue ( "ListInResults" );
	//String IfNotFound = parameters.getValue ( "IfNotFound" );
	String warning = "";
	String message;

	CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to remove is not checked during initialization
	// because files may be created dynamically at runtime.
	
    if ( (InputFile != null) && !InputFile.isEmpty() && (InputFile.indexOf("${") < 0) ) {
        String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
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
        try {
            String adjusted_path = IOUtil.verifyPathForOS (IOUtil.adjustPath ( working_dir, InputFile) );
            File f = new File ( adjusted_path );
            if ( !f.exists() ) {
                message = "The input file does not exist:  \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Verify that the input file exists - may be OK if created at run time." ) );
            }
            f = null;
        }
        catch ( Exception e ) {
            message = "The input file:\n" +
            "    \"" + InputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that input file and working directory paths are compatible." ) );
        }
        if ( (InputText != null) && !InputText.isEmpty() ) {
            message = "The input template file and input text cannot both be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an existing template file or provide template text." ) );
        }
    }

    if ( ((InputFile == null) || InputFile.isEmpty()) &&
        ((InputText == null) || InputText.isEmpty()) ) {
        message = "The input template file or text must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify an input template file or text." ) );
    }

    if ( (OutputFile == null) || OutputFile.isEmpty() ) {
        if ( (OutputProperty == null) || OutputProperty.isEmpty() ) {
            message = "The output file and/or property must be specified.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Specify an output file and/or property." ) );
        }
    }
    else if ( OutputFile.indexOf("${") < 0 ){
        String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
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

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does " +
                "not exist: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
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
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that output file and working directory paths are compatible." ) );
        }
    }
    
    if ( (UseTables != null) && !UseTables.isEmpty() &&
        !UseTables.equalsIgnoreCase(_True) &&
        !UseTables.equalsIgnoreCase(_False) ) {
        message = "The UseTables parameter \"" + UseTables + "\" must be " + _True + " or " + _False + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Correct the UseTables parameter to be blank, " + _True + ", or " + _False + "." ) );
    }
    
    if ( (ListInResults != null) && !ListInResults.isEmpty() &&
        !ListInResults.equalsIgnoreCase(_True) &&
        !ListInResults.equalsIgnoreCase(_False) ) {
        message = "The ListInResults parameter \"" + ListInResults + "\" must be " + _True + " or " + _False + ".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message,
                "Correct the ListInResults parameter to be blank, " + _True + ", or " + _False + "." ) );
    }
	
	/*
	if ( (IfNotFound != null) && !IfNotFound.equals("") ) {
		if ( !IfNotFound.equalsIgnoreCase(_Ignore) && !IfNotFound.equalsIgnoreCase(_Warn) ) {
			message = "The IfNoutFound parameter \"" + IfNotFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + " or (default) " + _Warn + "."));
		}
	}
	*/
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(6);
	validList.add ( "InputFile" );
	validList.add ( "InputText" );
	validList.add ( "OutputFile" );
	validList.add ( "OutputProperty" );
	validList.add ( "UseTables" );
	validList.add ( "ListInResults" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ExpandTemplateFile_JDialog ( parent, this )).ok();
}

/**
Format the template for a warning message.  Add line numbers before.
*/
private StringBuffer formatTemplateForWarning ( List<String> templateLines, String nl )
{   StringBuffer templateFormatted = new StringBuffer();
    new StringBuffer();
    int lineNumber = 1;
    // Don't use space after number because HTML viewer may split and make harder to read
    templateFormatted.append ( StringUtil.formatString(lineNumber++,"%d") + ":<@normalizeNewlines>" + nl );
    for ( String line : templateLines ) {
        templateFormatted.append ( StringUtil.formatString(lineNumber++,"%d") + ":" + line + nl );
    }
    templateFormatted.append ( StringUtil.formatString(lineNumber,"%d") + ":</@normalizeNewlines>" + nl );
    return templateFormatted;
}

/**
Return the property defined in discovery phase.
*/
private Prop getDiscoveryProp ()
{
    return __discoveryProp;
}

/**
Return the list of data objects read by this object in discovery mode.
*/
public List getObjectList ( Class c )
{
    Prop discovery_Prop = getDiscoveryProp ();
    if ( discovery_Prop == null ) {
        return null;
    }
    Prop prop = new Prop();
    // Check for TS request or class that matches the data...
    if ( c == prop.getClass() ) {
        List v = new Vector (1);
        v.add ( discovery_Prop );
        return v;
    }
    else {
        return null;
    }
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

// parseCommand from parent

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
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
@param commandPhase The command phase that is being run (RUN or DISCOVERY).
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3; // Level for non-user messages for log file.
	
	PropList parameters = getCommandParameters();
	
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
		status.clearLog(CommandPhaseType.RUN);
	}
	
	String InputFile = parameters.getValue ( "InputFile" );
	String InputText = parameters.getValue ( "InputText" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String OutputProperty = parameters.getValue ( "OutputProperty" );
	String UseTables = parameters.getValue ( "UseTables" );
    boolean UseTables_boolean = true;
    if ( (UseTables != null) && UseTables.equalsIgnoreCase(_False) ) {
        UseTables_boolean = false;
    }
	String ListInResults = parameters.getValue ( "ListInResults" );
	boolean ListInResults_boolean = true;
	if ( (ListInResults != null) && ListInResults.equalsIgnoreCase(_False) ) {
	    ListInResults_boolean = false;
	}
	/*
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    IfNotFound = _Warn; // Default
	}
	*/

	String InputFile_full = null;
	if ( (InputFile != null) && !InputFile.equals("") ) {
	    InputFile_full = IOUtil.verifyPathForOS(
	        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        	TSCommandProcessorUtil.expandParameterValue(processor, this,InputFile)) );
        File file = new File ( InputFile_full );
    	if ( !file.exists() ) {
            message = "Template command file \"" + InputFile_full + "\" does not exist.";
            /*
            if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that the file exists at the time the command is run."));
            }
            else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {*/
                Message.printWarning ( warning_level,
                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
                status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                    message, "Verify that the file exists at the time the command is run."));
                /*
            }
            else {
                Message.printStatus( 2, routine, message + "  Ignoring.");
            }*/
    	}
	}
    String OutputFile_full = null;
    if ( (OutputFile != null) && !OutputFile.equals("") ) {
        OutputFile_full = IOUtil.verifyPathForOS(
        	IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
        		TSCommandProcessorUtil.expandParameterValue(processor, this,OutputFile)) );
        File file = new File ( OutputFile_full );
        if ( !file.getParentFile().exists() ) {
           message = "Output file parent folder \"" + file.getParentFile() + "\" does not exist.";
               Message.printWarning ( warning_level,
                   MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
           status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
               message, "Verify that the output folder exists at the time the command is run."));
        }
    }
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}

    try {
        // Initialize the output file to null...
        setOutputFile ( null );
        setDiscoveryProp(null);
        if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            if ( (OutputProperty != null) && !OutputProperty.equals("") ) {
                // Just set the property name
                setDiscoveryProp(new Prop(OutputProperty,null,""));
            }
        }
        else if ( commandPhase == CommandPhaseType.RUN ) {
            // Call the FreeMarker API...
            Configuration config = new Configuration();
            // TODO SAM 2009-10-07 Not sure what configuration is needed for TSTool since most
            // templates will be located with command files and user data
            //config.setSharedVariable("shared", "avoid global variables");
            // See comment below on why this is used.
            config.setSharedVariable("normalizeNewlines", new freemarker.template.utility.NormalizeNewlines());
            config.setTemplateLoader(new FileTemplateLoader(new File(".")));
    
            // In some apps, use config to load templates as it provides caching
            //Template template = config.getTemplate("some-template.ftl");
    
            // Manipulate the template file into an in-memory string so it can be manipulated...
            StringBuffer b = new StringBuffer();
            // Prepend any extra FreeMarker content that should be handled transparently.
            // "normalizeNewlines" is used to ensure that output has line breaks consistent with the OS (e.g., so that
            // results can be edited in Notepad on Windows).
            String nl = System.getProperty("line.separator");
            b.append("<@normalizeNewlines>" + nl );
            List<String> templateLines = new ArrayList<String>();
            if ( InputFile_full != null ) {
                templateLines = IOUtil.fileToStringList(InputFile_full);
            }
            else if ( (InputText != null) && !InputText.equals("") ) {
                templateLines.add(InputText);
            }
            b.append(StringUtil.toString(templateLines,nl));
            b.append(nl + "</@normalizeNewlines>" );
            Template template = null;
            boolean error = false;
            try {
                template = new Template("template", new StringReader(b.toString()), config);
            }
            catch ( Exception e1 ) {
                message = "Freemarker error expanding command template file \"" + InputFile_full +
                    "\" + (" + e1 + ") template text (with internal inserts at ends) =" + nl +
                    formatTemplateForWarning(templateLines,nl);
                Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
                Message.printWarning ( 3, routine, e1 );
                status.addToLog(CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Check template file syntax for Freemarker markup errors."));
                error = true;
            }
            if ( !error ) {
                // Create a model
                Map model = new HashMap();
                TSCommandProcessor tsprocessor = (TSCommandProcessor)processor;
                if ( processor instanceof TSCommandProcessor ) {
                    // Add properties from the processor
                    Collection<String> propertyNames = tsprocessor.getPropertyNameList(true,true);
                    for ( String propertyName : propertyNames ) {
                        model.put(propertyName, tsprocessor.getPropContents(propertyName) );
                    }
                    // Add single column tables from the processor, using the table ID as the object key
                    if ( UseTables_boolean ) {
                        List<DataTable> tables = (List<DataTable>)tsprocessor.getPropContents ( "TableResultsList" );
                        Object tableVal;
                        for ( DataTable table: tables ) {
                            if ( table.getNumberOfFields() == 1 ) {
                                // One-column table so add as a hash (list) property in the data model
                                int numRecords = table.getNumberOfRecords();
                                SimpleSequence list = new SimpleSequence();
                                for ( int irec = 0; irec < numRecords; irec++ ) {
                                    // Check for null because this fouls up the template
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
                if ( OutputFile_full != null ) {
                    // Expand the template to the output file
                    FileOutputStream fos = new FileOutputStream( OutputFile_full );
                    PrintWriter out = new PrintWriter ( fos );
                    try {
                        template.process (model, out);
                        // Set the output file
                        if ( ListInResults_boolean ) {
                            setOutputFile ( new File(OutputFile_full));
                        }
                    }
                    catch ( Exception e1 ) {
                        message = "Freemarker error expanding command template file \"" + InputFile_full +
                            "\" + (" + e1 + ") template text (with internal inserts at ends) =\n" +
                            formatTemplateForWarning(templateLines,nl);;
                        Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
                        Message.printWarning ( 3, routine, e1 );
                        status.addToLog(CommandPhaseType.RUN,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Check template file syntax for Freemarker markup errors."));
                    }
                    finally {
                        out.close();
                    }
                }
                if ( (OutputProperty != null) && !OutputProperty.equals("") ) {
                    // Expand the template to a property - run and discovery mode
                    StringWriter out = new StringWriter ();
                    try {
                        template.process (model, out);
                        // Set the property in the processor                   
                        PropList request_params = new PropList ( "" );
                        request_params.setUsingObject ( "PropertyName", OutputProperty );
                        // Trim off trailing newline
                        String propValue = out.getBuffer().toString().trim();
                        request_params.setUsingObject ( "PropertyValue", propValue );
                        try {
                            processor.processRequest( "SetProperty", request_params); 
                            if ( commandPhase == CommandPhaseType.DISCOVERY ) {
                                setDiscoveryProp ( new Prop(OutputProperty,propValue,"" + propValue ) );
                            }
                        }
                        catch ( Exception e ) {
                            message = "Error requesting SetProperty(Property=\"" + OutputProperty + "\") from processor.";
                            Message.printWarning(log_level,
                                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                                    routine, message );
                            status.addToLog ( CommandPhaseType.RUN,
                                    new CommandLogRecord(CommandStatusType.FAILURE,
                                            message, "Report the problem to software support." ) );
                        }
                    }
                    catch ( Exception e1 ) {
                        message = "Freemarker error expanding command template file \"" + InputFile_full +
                            "\" + (" + e1 + ") template text to property (with internal inserts at ends) =\n" +
                            formatTemplateForWarning(templateLines,nl);;
                        Message.printWarning ( warning_level, 
                        MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
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
        }
	}
	catch ( Exception e ) {
		message = "Unexpected error expanding command template file \"" + InputFile_full + "\" to \"" +
		    OutputFile_full + " (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "See the log file for details."));
		throw new CommandException ( message );
	}

	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the property defined in discovery phase.
@param prop Property set during discovery phase.
*/
private void setDiscoveryProp ( Prop prop )
{
    __discoveryProp = prop;
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
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String InputFile = parameters.getValue("InputFile");
	String InputText = parameters.getValue("InputText");
	String OutputFile = parameters.getValue("OutputFile");
	String OutputProperty = parameters.getValue("OutputProperty");
	String UseTables = parameters.getValue("UseTables");
	String ListInResults = parameters.getValue("ListInResults");
	//String IfNotFound = parameters.getValue("IfNotFound");
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
    if ( (InputText != null) && (InputText.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append(",");
        }
        b.append ( "InputText=\"" + InputText + "\"" );
    }
    if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append(",");
        }
        b.append ( "OutputFile=\"" + OutputFile + "\"" );
    }
    if ( (OutputProperty != null) && (OutputProperty.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append(",");
        }
        b.append ( "OutputProperty=\"" + OutputProperty + "\"" );
    }
    if ( (UseTables != null) && (UseTables.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append(",");
        }
        b.append ( "UseTables=" + UseTables );
    }   
    if ( (ListInResults != null) && (ListInResults.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append(",");
        }
        b.append ( "ListInResults=" + ListInResults );
    }
	/*
	if ( (IfNotFound != null) && (IfNotFound.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfNotFound=" + IfNotFound );
	}
	*/
	return getCommandName() + "(" + b.toString() + ")";
}

}