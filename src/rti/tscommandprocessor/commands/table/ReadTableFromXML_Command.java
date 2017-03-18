package rti.tscommandprocessor.commands.table;

import javax.swing.JFrame;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.IO.IOUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Table.TableField;
import RTi.Util.Table.TableRecord;

/**
This class initializes, checks, and runs the ReadTableFromXML() command.
*/
public class ReadTableFromXML_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{
    
/**
The table that is read.
*/
private DataTable __table = null;

/**
Constructor.
*/
public ReadTableFromXML_Command ()
{	super();
	setCommandName ( "ReadTableFromXML" );
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
{	String TableID = parameters.getValue ( "TableID" );
    String InputFile = parameters.getValue ( "InputFile" );
    String RowElement = parameters.getValue ( "RowElement" );
    String Top = parameters.getValue ( "Top" );
	String warning = "";
    String message;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

	// If the input file does not exist, warn the user...

	String working_dir = null;
	
	CommandProcessor processor = getCommandProcessor();
	
    if ( (TableID == null) || (TableID.length() == 0) ) {
        message = "The table identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify the table identifier." ) );
    }
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
                        message, "Report the problem to software support." ) );
	}
	
	if ( (InputFile == null) || (InputFile.length() == 0) ) {
        message = "The input file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an existing input file." ) );
	}
	else {
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
	}
    if ( (RowElement == null) || (RowElement.length() == 0) ) {
        message = "The row element must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the row element." ) );
    }
    if ( (Top != null) && (Top.length() != 0) && !StringUtil.isInteger(Top)) {
        message = "The Top value (" + Top +") is not an integer.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the Top parameter as an integer." ) );
    }
 
	// TODO SAM 2005-11-18 Check the format.
    
	//  Check for invalid parameters...
	List<String> validList = new ArrayList<String>(4);
    validList.add ( "TableID" );
    validList.add ( "InputFile" );
    validList.add ( "RowElement" );
    validList.add ( "Top" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );    

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
		throw new InvalidCommandParameterException ( warning );
	}
    
    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Create the table columns.  This is called recursively
@param table data table to process
@param node node to add columns to table
*/
private void createTableColumns ( DataTable table, NodeList nodeList )
{
	Node node;
	for ( int i = 0; i < nodeList.getLength(); i++ ) {
		node = nodeList.item(i);
		if ( node.getNodeType() == Node.ELEMENT_NODE ) {
			// Add the element name for the node - always add because data always gets added later even if blank
			// TODO SAM 2015-04-11 Figure out the data type
			String nodeName = null;
			if ( (node == null) || (node.getNodeName() == null) ) {
				nodeName = "Column" + table.getNumberOfFields() + 1;
			}
			else {
				nodeName = node.getNodeName();
			}
			table.addField(new TableField(TableField.DATA_TYPE_STRING, nodeName, -1, -1), null );
			// Loop through the children and recursively add
			// TODO SAM 2015-04-11 Need to only add one-to-one fields and need a way to deal with required vs. optional
			NodeList nodeList2 = node.getChildNodes();
			if ( nodeList2.getLength() > 0 ) {
				createTableColumns(table,nodeList2);
			}
		}
	}
}

/**
Add a row to the table.  This is called recursively.
@param rec table record to process
@param node node to add data values to record
*/
private void createTableRow ( TableRecord rec, NodeList nodeList )
{	String routine = getClass().getSimpleName() + ".createTableRow";
	Node node;
	for ( int i = 0; i < nodeList.getLength(); i++ ) {
		node = nodeList.item(i);
		Message.printStatus(2,routine,"Processing node \"" + node.getNodeName() + "\"" );
		if ( node.getNodeType() == Node.ELEMENT_NODE ) {
			Message.printStatus(2,routine,"Node is element node." );
			// Add the text for the node
			// TODO SAM 2015-04-11 Figure out the data type
			String nodeContent = null;
			Message.printStatus(2,routine,"Number of child nodes="+node.getChildNodes().getLength());
			if ( node.getChildNodes().getLength() == 0 ) {
				// No children so element should have text content
				nodeContent = node.getTextContent();
			}
			rec.addFieldValue(nodeContent);
			// Loop through the children and recursively add
			// TODO SAM 2015-04-11 Need to only add one-to-one fields and need a way to deal with required vs. optional
			// To be sure, maybe need to lookup column name?
			NodeList nodeList2 = node.getChildNodes();
			if ( nodeList2.getLength() > 0 ) {
				createTableRow(rec,nodeList2);
			}
		}
	}
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed).
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new ReadTableFromXML_JDialog ( parent, this )).ok();
}

/**
Return the table that is read by this class when run in discovery mode.
*/
private DataTable getDiscoveryTable()
{
    return __table;
}

/**
Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
*/
public List getObjectList ( Class c )
{   DataTable table = getDiscoveryTable();
    List<DataTable> v = null;
    if ( (table != null) && (c == table.getClass()) ) {
        v = new Vector<DataTable>();
        v.add ( table );
    }
    return v;
}

// Use base class parseCommand()

/**
Read the XML table.
@param inputFile the path to the input XML file to read
@param rowElement the name of the element indicating row scope
@param top the number of rows to read, or -1 to read all
*/
private DataTable readXmlTable ( String inputFile, String rowElement, int top, List<String> problems )
throws ParserConfigurationException, IOException, SAXException
{	String routine = getClass().getSimpleName() + ".readXmlTable";
	String message;
	DataTable table = new DataTable();
	// Get Document Builder
	Message.printStatus(2,routine,"Creating document builder factory...");
	DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	Message.printStatus(2,routine,"Creating document builder...");
	DocumentBuilder builder = factory.newDocumentBuilder();
	// Build the document
	Message.printStatus(2,routine,"Parsing the XML document...");
	Document document = builder.parse(new File(inputFile));
	// Normalize the document
	Message.printStatus(2,routine,"Normalizing the XML document...");
	document.getDocumentElement().normalize();
	// Get the root node
	Element root = document.getDocumentElement();
	Message.printStatus(2,routine,"Root node name=\"" + root.getNodeName() + "\"");
	// Get the elements to use for the table, or children under the root if not specified
	NodeList nodeList = null;
	nodeList = document.getElementsByTagName(rowElement);
	if ( nodeList.getLength() == 0 ) {
		problems.add("No elements named \"" + rowElement + "\" were read.");
	}
	// Loop through the nodes.  For the first element, create the columns in the table
	Message.printStatus(2, routine, "XML file has " + nodeList.getLength() + " elements named \"" +rowElement + "\" to process into rows.");
	NodeList nodeList2;
	for ( int i = 0; i < nodeList.getLength(); i++ ) {
		Node node = nodeList.item(i);
		// Process the children of the element used for rows
		nodeList2 = node.getChildNodes();
		if ( i == 0 ) {
			// Create the columns in the table using the children of the element
			createTableColumns(table,nodeList2);
		}
		// Create the data in the record
		TableRecord rec = new TableRecord();
		createTableRow(rec,nodeList2);
		try {
			table.addRecord(rec);
		}
		catch ( Exception e ) {
			message = "Error adding row " + (i + 1) + " (" + e + ")";
			Message.printWarning(3, routine, message );
			problems.add ( message );
		}
		if ( (top >= 0) && (i == (top - 1)) ) {
			break;
		}
	}
	return table;
}

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
private void runCommandInternal ( int command_number, CommandPhaseType command_phase )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "ReadTableFromXML_Command.runCommand",message = "";
	int warning_level = 2;
	String command_tag = "" + command_number;	
	int warning_count = 0;
    
    CommandStatus status = getCommandStatus();
    status.clearLog(command_phase);
    if ( command_phase == CommandPhaseType.DISCOVERY ) {
        setDiscoveryTable ( null );
    }

	// Make sure there are time series available to operate on...
	
	PropList parameters = getCommandParameters();
	CommandProcessor processor = getCommandProcessor();

    String TableID = parameters.getValue ( "TableID" );
	String InputFile = parameters.getValue ( "InputFile" );
    String RowElement = parameters.getValue ( "RowElement" );
    String Top = parameters.getValue ( "Top" );
    Integer top = 0;
    if ( (Top != null) && !Top.equals("") ) {
        top = Integer.parseInt(Top);
    }

	String InputFile_full = IOUtil.verifyPathForOS(
        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),InputFile) );
	if ( !IOUtil.fileExists(InputFile_full) ) {
		message += "\nThe XML file \"" + InputFile_full + "\" does not exist.";
		++warning_count;
        status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that the XML file exists." ) );
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings for command parameters.";
		Message.printWarning ( 2,
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine,message);
		throw new InvalidCommandParameterException ( message );
	}

	// Now process the file...

	DataTable table = null;
	try {
	    if ( command_phase == CommandPhaseType.RUN ) {
	    	List<String> problems = new ArrayList<String>();
	        table = readXmlTable ( InputFile_full, RowElement, top, problems );
	        int errorMax = 50;
	        int errorCount = 0;
	        for ( String problem : problems ) {
	    		++warning_count;
	    		++errorCount;
	    		Message.printWarning(3,routine,problem);
	            status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
	                problem, "Check command input." ) );
	            if ( errorCount > errorMax ) {
	            	break;
	            }
	        }
	        if ( table == null ) {
	    		message = "No table created from XML file \"" + InputFile_full + "\".";
	    		++warning_count;
	    		Message.printWarning(3,routine,message);
	            status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify input to read the XML file." ) );
	        	table = new DataTable();
	        }
	        table.setTableID ( TableID );
	    }
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message = "Unexpected error reading table from XML file \"" + InputFile_full + "\" (" + e + ").";
		Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warning_count), routine,message );
		Message.printWarning(3,routine,e);
        status.addToLog ( command_phase, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Verify that the file exists and is readable." ) );
		throw new CommandWarningException ( message );
	}
	
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings processing the command.";
		Message.printWarning ( warning_level,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
		throw new CommandWarningException ( message );
	}
    
    // Set the table in the processor...
    
    if ( command_phase == CommandPhaseType.RUN ) {
        PropList request_params = new PropList ( "" );
        request_params.setUsingObject ( "Table", table );
        try {
            processor.processRequest( "SetTable", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting SetTable(Table=...) from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
    }
    else if ( command_phase == CommandPhaseType.DISCOVERY ) {
        table = new DataTable ();
        table.setTableID ( TableID );
        setDiscoveryTable ( table );
    }

    status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the table that is read by this class in discovery mode.
*/
private void setDiscoveryTable ( DataTable table )
{
    __table = table;
}

/**
Return the string representation of the command.
*/
public String toString ( PropList props )
{	if ( props == null ) {
		return getCommandName() + "()";
	}
    String TableID = props.getValue( "TableID" );
	String InputFile = props.getValue( "InputFile" );
	String RowElement = props.getValue( "RowElement" );
	String Top = props.getValue( "Top" );
	StringBuffer b = new StringBuffer ();
	if ( (InputFile != null) && (InputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "InputFile=\"" + InputFile + "\"" );
	}
	if ( (RowElement != null) && (RowElement.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RowElement=\"" + RowElement + "\"" );
	}
    if ( (Top != null) && (Top.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Top=" + Top );
    }
    if ( (TableID != null) && (TableID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TableID=\"" + TableID + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}