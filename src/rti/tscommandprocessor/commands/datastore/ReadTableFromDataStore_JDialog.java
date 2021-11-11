// ReadTableFromDataStore_JDialog - editor dialog for ReadTableFromDataStore command

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

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import RTi.DMI.DMI;
import RTi.DMI.DMIDatabaseType;
import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class ReadTableFromDataStore_JDialog extends JDialog
implements ActionListener, ChangeListener, ItemListener, KeyListener, WindowListener
{
    
private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __DataStore_JComboBox = null;
private JTextField __TableID_JTextField = null;
private JTextField __RowCountProperty_JTextField = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __DataStoreCatalog_JComboBox = null;
private SimpleJComboBox __DataStoreSchema_JComboBox = null;
private SimpleJComboBox __DataStoreTable_JComboBox = null;
private JTextField __DataStoreColumns_JTextField = null;
private JTextField __OrderBy_JTextField = null;
private JTextField __Top_JTextField = null;
private JTextArea __Sql_JTextArea = null;
private JTextField __SqlFile_JTextField = null;
private SimpleJComboBox __DataStoreFunction_JComboBox = null;
private JTextArea __FunctionParameters_JTextArea = null;
private SimpleJComboBox __DataStoreProcedure_JComboBox = null;
private JTextArea __ProcedureParameters_JTextArea = null;
private JTextField __ProcedureReturnProperty_JTextField = null;
private JTextArea __OutputProperties_JTextArea = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadTableFromDataStore_Command __command = null;
private boolean __ok = false;
private String __working_dir = null;
private JFrame __parent = null;
//private List<String> datastoreNames = new ArrayList<>();
private List<DatabaseDataStore> datastores = new ArrayList<>();

private boolean __ignoreItemEvents = false; // Used to ignore cascading events when working with choices.

private DatabaseDataStore __dataStore = null; // Selected datastore.
private DMI __dmi = null; // DMI to do queries.

// Last datastore for which procedures were populated, so can avoid repopulating the procedures.
private String lastDataStoreForProcedures = "";
// Last datastore for which functions were populated, so can avoid repopulating the functions.
private String lastDataStoreForFunctions = "";

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param datastores list of database datastores
*/
public ReadTableFromDataStore_JDialog ( JFrame parent, ReadTableFromDataStore_Command command, List<DatabaseDataStore> datastores ) {
	super(parent, true);
	initialize ( parent, command, datastores );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	//String routine = getClass().getSimpleName() + ".actionPerformed";
	Object o = event.getSource();

    if ( o == __browse_JButton ) {
        // Browse for the file to read.
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle( "Select SQL File");
        SimpleFileFilter sff = new SimpleFileFilter("sql","SQL File");
        fc.addChoosableFileFilter(sff);
        
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        if ( last_directory_selected != null ) {
            fc.setCurrentDirectory( new File(last_directory_selected));
        }
        else {
            fc.setCurrentDirectory(new File(__working_dir));
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
				// Convert path to relative path by default.
				try {
					__SqlFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"CompareFiles_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory( directory);
                refresh();
            }
        }
    }
    else if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditOutputProperties") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String OutputProperties = __OutputProperties_JTextArea.getText().trim();
        String [] notes = {
            "One row result can be set as processor properties."
        };
        String dict = (new DictionaryJDialog ( __parent, true, OutputProperties,
            "Edit OutputProperties Parameter", notes, "Table Column", "Processor Property Name",10)).response();
        if ( dict != null ) {
            __OutputProperties_JTextArea.setText ( dict );
            refresh();
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadTableFromDataStore");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited.
			response ( true );
		}
	}
    else if ( o == __path_JButton ) {
        if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
            __SqlFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,__SqlFile_JTextField.getText() ) );
        }
        else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
            try {
                __SqlFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __SqlFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "ReadTableFromDataStore_JDialog", "Error converting file to relative path." );
            }
        }
        refresh ();
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditFunctionParameters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String FunctionParameters = __FunctionParameters_JTextArea.getText().trim();
        List<String> notesList = new ArrayList<>();
        String functionName = __DataStoreFunction_JComboBox.getSelected();
        int nparams = 10; // Will be reset below.
        if ( (functionName == null) || functionName.isEmpty() ) {
        	notesList.add("Function to run:  NOT SPECIFIED" );
        }
        else {
        	notesList.add("Function to run:  " + functionName );
        	// Parse the parameter names from the function name.
        	List<String> parameterNames = this.__command.parseFunctionParameterNames(functionName);
        	nparams = parameterNames.size();
        	if ( nparams > 0 ) {
        		notesList.add("Function has " + nparams + " parameters.");
        		notesList.add("Specify function parameter values using appropriate type.");
        		notesList.add("The parameter names that are listed are based on the function declaration.");
        		notesList.add("The parameter order and data type must be correct.");
        		notesList.add("For example, make sure to provide properly-formatted numbers if type indicates numeric input.");
        		notesList.add("Format date/times using syntax YYYY-MM-DD hh:mm:ss to appropriate precision for date and date/time.");
        	}
        	else {
        		notesList.add("The function does not have any input parameters.");
        		notesList.add("Therefore this command parameter (FunctionParameters) is not used.");
        	}
        }
        String [] notes = notesList.toArray(new String[0]);
        String dict = (new DictionaryJDialog ( __parent, true, FunctionParameters,
            "Edit FunctionParameters parameter", notes, "Parameter Name", "Parameter Value", -nparams)).response();
        if ( dict != null ) {
            __FunctionParameters_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditProcedureParameters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ProcedureParameters = __ProcedureParameters_JTextArea.getText().trim();
        List<String> notesList = new ArrayList<>();
        String procedureName = __DataStoreProcedure_JComboBox.getSelected();
        int nparams = 10; // Will be reset below.
        if ( (procedureName == null) || procedureName.isEmpty() ) {
        	notesList.add("Procedure to run:  NOT SPECIFIED" );
        }
        else {
        	notesList.add("Procedure to run:  " + procedureName );
        	// Parse the parameter names from the function name.
        	List<String> parameterNames = this.__command.parseFunctionParameterNames(procedureName);
        	nparams = parameterNames.size();
        	if ( nparams > 0 ) {
        		notesList.add("Procedure has " + nparams + " parameters.");
        		notesList.add("Specify procedure parameter values using appropriate type.");
        		notesList.add("The parameter names that are listed are based on the procedure declaration.");
        		notesList.add("The parameter order and data type must be correct.");
        		notesList.add("For example, make sure to provide properly-formatted numbers if type indicates numeric input.");
        		notesList.add("Format date/times using syntax YYYY-MM-DD hh:mm:ss to appropriate precision for date and date/time.");
        	}
        	else {
        		notesList.add("The procedure does not have any input parameters.");
        		notesList.add("Therefore this command parameter (ProcedureParameters) is not used.");
        	}
        }
        String [] notes = notesList.toArray(new String[0]);
        String dict = (new DictionaryJDialog ( __parent, true, ProcedureParameters,
            "Edit ProcedureParameters parameter", notes, "Parameter Name", "Parameter Value", -nparams)).response();
        if ( dict != null ) {
            __ProcedureParameters_JTextArea.setText ( dict );
            refresh();
        }
    }
}

/**
Refresh the schema choices in response to the currently selected datastore.
*/
private void actionPerformedDataStoreCatalogSelected ( )
{
    if ( __DataStoreCatalog_JComboBox.getSelected() == null ) {
        // Startup initialization.
        return;
    }
    __dataStore = getSelectedDataStore();
    __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the schema choices corresponding to the database
    populateDataStoreSchemaChoices ( __dmi );
}

/**
Refresh the database choices in response to the currently selected datastore.
If the datastore is null (such as a discovery-only datastore),
will not be able to populate anything from database metadata.
*/
private void actionPerformedDataStoreSelected ( ) {
    if ( this.__DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization.
        return;
    }
    this.__dataStore = getSelectedDataStore();
    if ( this.__dataStore == null ) {
    	__dmi = null;
    }
    else {
    	__dmi = ((DatabaseDataStore)__dataStore).getDMI();
    }
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Populate the database choices corresponding to the datastore.
    populateDataStoreCatalogChoices ( this.__dmi );
}

/**
Refresh the table choices in response to the currently selected schema.
*/
private void actionPerformedDataStoreSchemaSelected ( )
{
    if ( __DataStoreSchema_JComboBox.getSelected() == null ) {
        // Startup initialization.
        return;
    }
    __dataStore = getSelectedDataStore();
    __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the table choices corresponding to the schema
    populateDataStoreTableChoices ( __dmi );
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
    String DataStore = __DataStore_JComboBox.getSelected();
    if ( (DataStore != null) && DataStore.length() > 0 ) {
        props.set ( "DataStore", DataStore );
        __dataStore = getSelectedDataStore();
        __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    }
    else {
        props.set ( "DataStore", "" );
    }
    String DataStoreCatalog = __DataStoreCatalog_JComboBox.getSelected();
    String DataStoreSchema = __DataStoreSchema_JComboBox.getSelected();
	String DataStoreTable = __DataStoreTable_JComboBox.getSelected();
	String DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
	String OrderBy = __OrderBy_JTextField.getText().trim();
	String Top = __Top_JTextField.getText().trim();
	String Sql = __Sql_JTextArea.getText().trim();
	String SqlFile = __SqlFile_JTextField.getText().trim();
	String DataStoreFunction = __DataStoreFunction_JComboBox.getSelected();
	String FunctionParameters = __FunctionParameters_JTextArea.getText().trim().replace("\n"," ");
	String DataStoreProcedure = __DataStoreProcedure_JComboBox.getSelected();
	String ProcedureParameters = __ProcedureParameters_JTextArea.getText().trim().replace("\n"," ");
	String ProcedureReturnProperty = __ProcedureReturnProperty_JTextField.getText().trim();
	String OutputProperties = __OutputProperties_JTextArea.getText().trim().replace("\n"," ");
    String TableID = __TableID_JTextField.getText().trim();
    String RowCountProperty = __RowCountProperty_JTextField.getText().trim();
	__error_wait = false;

    if ( DataStoreCatalog.length() > 0 ) {
        props.set ( "DataStoreCatalog", DataStoreCatalog );
    }
    if ( DataStoreSchema.length() > 0 ) {
        props.set ( "DataStoreSchema", DataStoreSchema );
    }
	if ( DataStoreTable.length() > 0 ) {
		props.set ( "DataStoreTable", DataStoreTable );
	}
    if ( DataStoreColumns.length() > 0 ) {
        props.set ( "DataStoreColumns", DataStoreColumns );
    }
    if ( OrderBy.length() > 0 ) {
        props.set ( "OrderBy", OrderBy );
    }
    if ( Top.length() > 0 ) {
        props.set ( "Top", Top );
    }
    if ( Sql.length() > 0 ) {
        props.set ( "Sql", Sql );
    }
    if ( SqlFile.length() > 0 ) {
        props.set ( "SqlFile", SqlFile );
    }
    if ( (DataStoreFunction != null) && (DataStoreFunction.length() > 0) ) {
        props.set ( "DataStoreFunction", DataStoreFunction );
    }
    if ( FunctionParameters.length() > 0 ) {
        props.set ( "FunctionParameters", FunctionParameters );
    }
    if ( (DataStoreProcedure != null) && (DataStoreProcedure.length() > 0) ) {
        props.set ( "DataStoreProcedure", DataStoreProcedure );
    }
    if ( ProcedureParameters.length() > 0 ) {
        props.set ( "ProcedureParameters", ProcedureParameters );
    }
    if ( ProcedureReturnProperty.length() > 0 ) {
        props.set ( "ProcedureReturnProperty", ProcedureReturnProperty );
    }
    if ( OutputProperties.length() > 0 ) {
        props.set ( "OutputProperties", OutputProperties );
    }
    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( RowCountProperty.length() > 0 ) {
        props.set ( "RowCountProperty", RowCountProperty );
    }
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
        Message.printWarning(2,"", e);
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String DataStore = __DataStore_JComboBox.getSelected();
    String DataStoreCatalog = __DataStoreCatalog_JComboBox.getSelected();
    String DataStoreSchema = __DataStoreSchema_JComboBox.getSelected();
    String DataStoreTable = __DataStoreTable_JComboBox.getSelected();
    String DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
    String OrderBy = __OrderBy_JTextField.getText().trim();
    String Top = __Top_JTextField.getText().trim();
    // Allow newlines in the dialog to be saved as escaped newlines.
    String Sql = __Sql_JTextArea.getText().trim().replace("\n", "\\n").replace("\t", " ");
    String SqlFile = __SqlFile_JTextField.getText().trim();
	String DataStoreFunction = __DataStoreFunction_JComboBox.getSelected();
	String FunctionParameters = __FunctionParameters_JTextArea.getText().trim().replace("\n"," ");
    String DataStoreProcedure = __DataStoreProcedure_JComboBox.getSelected();
	String ProcedureParameters = __ProcedureParameters_JTextArea.getText().trim().replace("\n"," ");
	String ProcedureReturnProperty = __ProcedureReturnProperty_JTextField.getText().trim();
	String OutputProperties = __OutputProperties_JTextArea.getText().trim();
    String TableID = __TableID_JTextField.getText().trim();
    String RowCountProperty = __RowCountProperty_JTextField.getText().trim();
    __command.setCommandParameter ( "DataStore", DataStore );
    __command.setCommandParameter ( "DataStoreCatalog", DataStoreCatalog );
    __command.setCommandParameter ( "DataStoreSchema", DataStoreSchema );
	__command.setCommandParameter ( "DataStoreTable", DataStoreTable );
	__command.setCommandParameter ( "DataStoreColumns", DataStoreColumns );
	__command.setCommandParameter ( "OrderBy", OrderBy );
	__command.setCommandParameter ( "Top", Top );
	__command.setCommandParameter ( "Sql", Sql );
	__command.setCommandParameter ( "SqlFile", SqlFile );
	__command.setCommandParameter ( "DataStoreFunction", DataStoreFunction );
	__command.setCommandParameter ( "FunctionParameters", FunctionParameters );
	__command.setCommandParameter ( "DataStoreProcedure", DataStoreProcedure );
	__command.setCommandParameter ( "ProcedureParameters", ProcedureParameters );
	__command.setCommandParameter ( "ProcedureReturnProperty", ProcedureReturnProperty );
	__command.setCommandParameter ( "OutputProperties", OutputProperties );
    __command.setCommandParameter ( "TableID", TableID );
	__command.setCommandParameter ( "RowCountProperty", RowCountProperty );
}

/**
Return the DMI that is currently being used for database interaction, based on the selected data store.
*/
private DMI getDMI () {
    return __dmi;
}

/**
Get the selected datastore from the processor using the datastore name.
If there is no datastore in the processor based on startup,
it may be a dynamic datastore created with OpenDataStore,
which will have a discovery datastore that is good enough for getting database metadata.
*/
private DatabaseDataStore getSelectedDataStore () {
    String routine = getClass().getSimpleName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    DatabaseDataStore dataStore = null;
   	dataStore = null;
   	for ( DatabaseDataStore dataStore2 : this.datastores ) {
   		if ( dataStore2.getName().equals(DataStore) ) {
   			dataStore = dataStore2;
   		}
   	}
   	if ( dataStore == null ) {
       	Message.printStatus(2, routine, "Cannot get datastore for \"" + DataStore +
       		"\".  Can read with SQL but cannot choose from list of tables or procedures." );
   	}
    else {
    	// Have an active datastore from software startup.
        Message.printStatus(2, routine, "Selected datastore is \"" + dataStore.getName() + "\"." );
        // Make sure database connection is open.
        dataStore.checkDatabaseConnection();
    }
    return dataStore;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
@param datastores list of database datastores
*/
private void initialize ( JFrame parent, ReadTableFromDataStore_Command command, List<DatabaseDataStore> datastores )
{	this.__command = command;
	this.__parent = parent;
	this.datastores = datastores;
	TSCommandProcessor processor = (TSCommandProcessor)__command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, this.__command );

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = -1;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = -1;

   	JGUIUtil.addComponent(paragraph, new JLabel (
        "This command reads a table from a database datastore table, view, function, or procedure."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The query can be specified in one of four ways:"),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "    1)  Specify a single table or view, related columns, and sort order."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "    2a) Specify a free form SQL select statement (allows joins and other SQL constructs " +
        "supported by the database software)."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "    2b) Similar to 2a; however, the SQL statement is read from a file, " +
        "which can be specified relative to the working directory."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(paragraph, new JLabel (
        "        The working directory is: " + __working_dir ), 
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(paragraph, new JLabel (
        "    3) Specify a database function to run (NOTE: embedding the function in an SQL SELECT is often easier)."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "    4) Specify a database procedure to run."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The resulting table columns will have data types based on the query results."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "<html><b>The function and procedure tabs may require a few seconds to display while database metadata are processed.</b></html>"),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
            0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    // List available data stores of the correct type.
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    // Copy the list of datastore names to internal list.
    List<String> datastoreChoices = new ArrayList<>();
    for ( DataStore dataStore : this.datastores ) {
    	datastoreChoices.add(dataStore.getName());
    }
    // Also list any substitute datastore names so the original or substitute can be used.
    HashMap<String,String> datastoreSubstituteMap = processor.getDataStoreSubstituteMap();
    for ( Map.Entry<String,String> set : datastoreSubstituteMap.entrySet() ) {
    	boolean found = false;
    	for ( String choice : datastoreChoices ) {
    		if ( choice.equals(set.getKey()) ) {
    			// The substitute original name matches a datastore name so also add the alias.
    			found = true;
    			break;
    		}
    	}
    	if ( found ) {
    		datastoreChoices.add(set.getValue());
    	}
    }
    Collections.sort(datastoreChoices, String.CASE_INSENSITIVE_ORDER);
    if ( datastoreChoices.size() == 0 ) {
        // Add an empty item so users can at least bring up the editor.
    	datastoreChoices.add ( "" );
    }
    __DataStore_JComboBox.setData(datastoreChoices);
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - data store containing data to read."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.addChangeListener(this);
    __main_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to query the datastore" ));
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for SQL via table choices.
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Table and columns", table_JPanel );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "Specify the catalog and schema as non-blank only if the datastore connection is defined at a higher level and requires " +
        "additional information to locate the table."),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel (
        "Database catalog and schema choices currently do not cascade because database driver metadata features are limited or may " +
        "be disabled and not provide information."),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yTable, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
 
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Datastore catalog (database):"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreCatalog_JComboBox = new SimpleJComboBox ( false );
    __DataStoreCatalog_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(table_JPanel, __DataStoreCatalog_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "<html>Optional - specify if needed for <b>[Database]</b>.[Schema].[Table].</html>"), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Datastore schema:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreSchema_JComboBox = new SimpleJComboBox ( false );
    // Set large so that new database list from selected schema does not foul up layout.
    String longest = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    __DataStoreSchema_JComboBox.setPrototypeDisplayValue(longest);
    __DataStoreSchema_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(table_JPanel, __DataStoreSchema_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel(
        "<html>Optional - specify if needed for [Database].<b>[Schema]</b>.[Table].</html>"), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Datastore table:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreTable_JComboBox = new SimpleJComboBox ( false );
    // Set large so that new table list from selected datastore does not foul up layout
    longest = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    __DataStoreTable_JComboBox.setPrototypeDisplayValue(longest);
    __DataStoreTable_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(table_JPanel, __DataStoreTable_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel("Required - database table/view to read."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Datastore columns:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreColumns_JTextField = new JTextField (10);
    __DataStoreColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, __DataStoreColumns_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - database table/view columns, separated by commas (default=all)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Order by:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OrderBy_JTextField = new JTextField (10);
    __OrderBy_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, __OrderBy_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - columns to sort by, separated by commas (default=no sort)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Top N rows:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Top_JTextField = new JTextField (10);
    __Top_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, __Top_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - return top N rows (default=return all)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for SQL via SQL statement.
    int ySql = -1;
    JPanel sql_JPanel = new JPanel();
    sql_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "SQL string", sql_JPanel );

    JGUIUtil.addComponent(sql_JPanel, new JLabel (
        "Specify SQL as a string. This is useful for simple SQL.  Use 'Enter' to insert a new line, which is shown as \\n in the command parameter."),
        0, ++ySql, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(sql_JPanel, new JLabel (
        "SQL specified with ${property} notation will be updated to use processor property values before executing the query."),
        0, ++ySql, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(sql_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++ySql, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(sql_JPanel, new JLabel ("SQL String:"), 
        0, ++ySql, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Sql_JTextArea = new JTextArea (9,50);
    __Sql_JTextArea.setToolTipText("Specify the SQL string, can use ${Property} notation");
    __Sql_JTextArea.setLineWrap ( true );
    __Sql_JTextArea.setWrapStyleWord ( true );
    __Sql_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(sql_JPanel, new JScrollPane(__Sql_JTextArea),
        1, ySql, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for SQL via SQL file.
    int yFile = -1;
    JPanel file_JPanel = new JPanel();
    file_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "SQL file", file_JPanel );
    
    JGUIUtil.addComponent(file_JPanel, new JLabel (
        "Specify SQL as a file. This is useful for complex SQL containing formatting such as comments and newlines."),
        0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(file_JPanel, new JLabel (
        "SQL specified with ${property} notation will be updated to use processor property values before executing the query."),
        0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(file_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(file_JPanel, new JLabel ( "SQL file to read:" ), 
        0, ++yFile, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SqlFile_JTextField = new JTextField ( 50 );
    __SqlFile_JTextField.setToolTipText("Specify the SQL file or use ${Property} notation");
    __SqlFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel.
	JPanel SqlFile_JPanel = new JPanel();
	SqlFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(SqlFile_JPanel, __SqlFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(SqlFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(SqlFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(file_JPanel, SqlFile_JPanel,
		1, yFile, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    // Panel for functions.

    int yFunc = -1;
    JPanel func_JPanel = new JPanel();
    func_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Function", func_JPanel );

    JGUIUtil.addComponent(func_JPanel, new JLabel ("<html><b>Under development</b> - run a function to return results as a table.</html>"),
        0, ++yFunc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(func_JPanel, new JLabel (
        "Use the Edit button to enter parameter values for the function."),
        0, ++yFunc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(func_JPanel, new JLabel (
        "Databases vary in how they implement functions and procedures."),
        0, ++yFunc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(func_JPanel, new JLabel (
        "Functions return 1+ values and parameters are only input - use the ReadTableFromDataStore command to save output to a table."),
        0, ++yFunc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(func_JPanel, new JLabel (
        "Function calls can be embedded in SQL SELECT statements with parameters in parentheses."),
        0, ++yFunc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(func_JPanel, new JLabel ( "Datastore function:"),
        0, ++yFunc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreFunction_JComboBox = new SimpleJComboBox ( false );
    // Set large so that new function list from selected datastore does not foul up layout.
    longest = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    __DataStoreFunction_JComboBox.setPrototypeDisplayValue(longest);
    __DataStoreFunction_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(func_JPanel, __DataStoreFunction_JComboBox,
        1, yFunc, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(func_JPanel, new JLabel("Optional - database function to run."), 
        3, yFunc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(func_JPanel, new JLabel ("Function parameters:"),
        0, ++yFunc, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FunctionParameters_JTextArea = new JTextArea (6,35);
    __FunctionParameters_JTextArea.setLineWrap ( true );
    __FunctionParameters_JTextArea.setWrapStyleWord ( true );
    __FunctionParameters_JTextArea.setToolTipText("ParameterName1:ParameterValue1,ParameterName2:ParameterValue2");
    __FunctionParameters_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(func_JPanel, new JScrollPane(__FunctionParameters_JTextArea),
        1, yFunc, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(func_JPanel, new JLabel ("Required - if function has parameters."),
        3, yFunc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(func_JPanel, new SimpleJButton ("Edit","EditFunctionParameters",this),
        3, ++yFunc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (6,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for procedure.
    int yProc = -1;
    JPanel proc_JPanel = new JPanel();
    proc_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Procedure", proc_JPanel );

    JGUIUtil.addComponent(proc_JPanel, new JLabel (
        "<html><b>Under development</b> - run a stored procedure to return results as a table.</html>"),
        0, ++yProc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(proc_JPanel, new JLabel (
        "Currently, only procedures that do not require parameters can be run."),
        0, ++yProc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(proc_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yProc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(proc_JPanel, new JLabel ( "Datastore procedure:"),
        0, ++yProc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreProcedure_JComboBox = new SimpleJComboBox ( false );
    // Set large so that new procedure list from selected datastore does not foul up layout.
    longest = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    __DataStoreProcedure_JComboBox.setPrototypeDisplayValue(longest);
    __DataStoreProcedure_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(proc_JPanel, __DataStoreProcedure_JComboBox,
        1, yProc, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(proc_JPanel, new JLabel("Optional - database procedure to run to generate results."), 
        3, yProc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(proc_JPanel, new JLabel ("Procedure parameters:"),
        0, ++yProc, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ProcedureParameters_JTextArea = new JTextArea (6,35);
    __ProcedureParameters_JTextArea.setLineWrap ( true );
    __ProcedureParameters_JTextArea.setWrapStyleWord ( true );
    __ProcedureParameters_JTextArea.setToolTipText("ParameterName1:ParameterValue1,ParameterName2:ParameterValue2");
    __ProcedureParameters_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(proc_JPanel, new JScrollPane(__ProcedureParameters_JTextArea),
        1, yProc, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(proc_JPanel, new JLabel ("Required - if procedure has parameters."),
        3, yProc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(proc_JPanel, new SimpleJButton ("Edit","EditProcedureParameters",this),
        3, ++yProc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(proc_JPanel, new JLabel ("Return value property:"),
        0, ++yProc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ProcedureReturnProperty_JTextField = new JTextField (10);
    __ProcedureReturnProperty_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(proc_JPanel, __ProcedureReturnProperty_JTextField,
        1, yProc, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(proc_JPanel, new JLabel ("Optional - property to set to return value."),
        3, yProc, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for output properties.
    int yProps = -1;
    JPanel props_JPanel = new JPanel();
    props_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Properties", props_JPanel );

    JGUIUtil.addComponent(props_JPanel, new JLabel (
        "If the query results in a single row, the row's column values can be set as properties."),
        0, ++yProps, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel (
        "Specify a list of column names and corresponding property names."),
        0, ++yProps, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yProps, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(props_JPanel, new JLabel ("Output properties:"),
        0, ++yProps, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputProperties_JTextArea = new JTextArea (6,35);
    __OutputProperties_JTextArea.setLineWrap ( true );
    __OutputProperties_JTextArea.setWrapStyleWord ( true );
    __OutputProperties_JTextArea.setToolTipText("TableColumn1:Property1,TableColumn2:Property2");
    __OutputProperties_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(props_JPanel, new JScrollPane(__OutputProperties_JTextArea),
        1, yProps, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel ("Optional - properties if 1-row result."),
        3, yProps, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(props_JPanel, new SimpleJButton ("Edit","EditOutputProperties",this),
        3, ++yProps, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Additional properties below the tabs.

    // Table for results.
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table ID:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JTextField = new JTextField (10);
    __TableID_JTextField.setToolTipText("Specify the table ID or use ${Property} notation");
    __TableID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __TableID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - unique identifier for the output table."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Row count property:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RowCountProperty_JTextField = new JTextField ( "", 20 );
    __RowCountProperty_JTextField.setToolTipText("The property can be referenced in other commands using ${Property}.");
    __RowCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __RowCountProperty_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - processor property to set as output table row count." ),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (6,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents.
	refresh ();

	// South JPanel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add (__ok_JButton);
	__ok_JButton.setToolTipText("Save changes to command");
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + "() Command");
	//setResizable (false);
    pack();
    JGUIUtil.center(this);
	refresh();
    super.setVisible(true);
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent event)
{
    if ( !__ignoreItemEvents ) {
        if ( (event.getSource() == __DataStore_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
            // User has selected a datastore.
            actionPerformedDataStoreSelected ();
        }
        else if ( (event.getSource() == __DataStoreCatalog_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
            // User has selected a catalog.
            actionPerformedDataStoreCatalogSelected ();
        }
        else if ( (event.getSource() == __DataStoreSchema_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
            // User has selected a schema.
            actionPerformedDataStoreSchemaSelected ();
        }
        else if ( (event.getSource() == __DataStoreFunction_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        	// Selected a new datastore function:
        	// - populate the 'FunctionParameters' parameter with parameter names and no values
        	List<String> parameterNames = this.__command.parseFunctionParameterNames(__DataStoreFunction_JComboBox.getSelected());
        	__FunctionParameters_JTextArea.setText(this.__command.formatFunctionParameters(parameterNames));
        }
        else if ( (event.getSource() == __DataStoreProcedure_JComboBox) && (event.getStateChange() == ItemEvent.SELECTED) ) {
        	// Selected a new datastore procedure:
        	// - populate the 'ProcedureParameters' parameter with parameter names and no values
        	List<String> parameterNames = this.__command.parseFunctionParameterNames(__DataStoreProcedure_JComboBox.getSelected());
        	__ProcedureParameters_JTextArea.setText(this.__command.formatFunctionParameters(parameterNames));
        }
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed (KeyEvent event) {
	int code = event.getKeyCode();

	if (code == KeyEvent.VK_ENTER) {
   		if ( event.getSource() == this.__Sql_JTextArea ) {
    		// Do not allow "Enter" in message because newlines in the message are allowed.
    		return;
    	}
   		else {
   			refresh ();
			checkInput();
			if (!__error_wait) {
				response ( true );
			}
   		}
	}
}

public void keyReleased (KeyEvent event) {
	refresh();
}

public void keyTyped (KeyEvent event) {}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{	return __ok;
}

/**
Populate the database list based on the selected datastore.
@param dmi DMI to use when selecting database list
*/
private void populateDataStoreCatalogChoices ( DMI dmi )
{   String routine = getClass().getSimpleName() + ".populateDataStoreDatastoreChoices";
    List<String> catalogList = null;
    List<String> notIncluded = new ArrayList<>();
    if ( dmi == null ) {
        catalogList = new ArrayList<>();
    }
    else {
        // TODO SAM 2013-07-22 Improve this - it should only be shown when the master DB is used.
        if ( (dmi.getDatabaseEngineType() == DMIDatabaseType.SQLSERVER) ) {
            try {
                catalogList = DMIUtil.getDatabaseCatalogNames(dmi, true, notIncluded);
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine, "Error getting database catalog (" + e + ")." );
                Message.printWarning ( 3, routine, e );
                catalogList = null;
            }
        }
    }
    if ( catalogList == null ) {
        catalogList = new ArrayList<>();
    }
    // Always add a blank option at the start to help with initialization.
    catalogList.add ( 0, "" );
    __DataStoreCatalog_JComboBox.removeAll();
    for ( String catalog : catalogList ) {
        __DataStoreCatalog_JComboBox.add( catalog );
    }
    // Select first choice (may get reset from existing parameter values).
    __DataStoreCatalog_JComboBox.select ( null );
    if ( __DataStoreCatalog_JComboBox.getItemCount() > 0 ) {
        __DataStoreCatalog_JComboBox.select ( 0 );
    }
}

/**
Populate the function list based on the selected database.
@param dmi DMI to use when selecting function list
*/
private void populateDataStoreFunctionChoices ( DMI dmi ) {
    String routine = getClass().getSimpleName() + ".populateDataStoreFunctionChoices";
	if ( lastDataStoreForFunctions.equals(__DataStore_JComboBox.getSelected()) ) {
		// Already populated so don't need to do again (it is a bit slow).
		return;
	}
    List<String> funcList = new ArrayList<>();
    //List<String> notIncluded = new ArrayList<>(); // TODO SAM 2012-01-31 need to omit system procedures.
    if ( dmi != null ) {
        try {
        	DatabaseMetaData metadata = dmi.getConnection().getMetaData();
        	// The following will return duplicates for overloaded functions.
            ResultSet rs = DMIUtil.getDatabaseFunctions (dmi);
            // Iterate through the ResultSet and add procedures:
            // - see note below about how function specific name must be used to manage the data
            StringBuilder funcBuilder = null;
            short columnType = 0;
            int pos = 0;
            String typeName = null;
            String specificName = null;
            String columnName = null;
            String funcNamePrev = "";
            while (rs.next()) {
            	String funcName = rs.getString("FUNCTION_NAME");
            	//Message.printStatus(2, routine, "Processing function: " + funcName );
            	if ( funcName.equals(funcNamePrev) ) {
            		// Same function name is being processed:
            		// - this will be the case for overloaded functions
            		// - skip because it will result in redundant processing
            		continue;
            	}
            	// Save the function name so is used in the next loop iteration.
            	funcNamePrev = funcName;
            	// Format the function signature as including the parameter names and type.
            	String returnString = "";
            	// Get the metadata for the function:
            	// - because functions may be overloaded, must format the function string first and then
            	//   add only if not already in the list
            	boolean supportsFunctions = true; // All databases have in metadata?
            	if ( supportsFunctions ) {
            		// If overloaded, duplicate entries can occur:
            		// - the following resultset is the total of columns for all overloaded functions
            		// - therefore, must group by the same "SPECIFIC_NAME"
            		// - use a hashmap with key being the specific name
            		HashMap<String,StringBuilder> funcMap = new HashMap<>();
            		ResultSet rs2 = metadata.getFunctionColumns(dmi.getConnection().getSchema(), null, funcName, null);
            		while (rs2.next()) {
            			// Format the column:
            			// - only list parameters that are passed to the function
            			// - return type does not seem to always be set so also check position
            			columnType = rs2.getShort("COLUMN_TYPE");
            			pos = rs2.getShort("ORDINAL_POSITION");
            			typeName = rs2.getString("TYPE_NAME");
            			specificName = rs2.getString("SPECIFIC_NAME");
            			columnName = rs2.getString("COLUMN_NAME");
            			//Message.printStatus(2, routine, "  Processing " + specificName + " " + funcName + " " + columnName + " " + columnType);
            			funcBuilder = funcMap.get(specificName);
            			if ( funcBuilder == null ) {
            				// Need to create a new StringBuilder:
            				// - save the StringBuilder so it can be used for matching records
            				funcBuilder = new StringBuilder();
            				funcMap.put(specificName, funcBuilder);
            				//Message.printStatus(2, routine, "  Adding HashMap function for " + specificName + " " + funcName);
            			}
            			else {
            				// Else, previously added the builder so use it.
            			}
            			if ( funcBuilder.length() == 0 ) {
            				// First time the function is encountered so add the function name at the beginning.
            				funcBuilder.append( funcName );
           					funcBuilder.append("(");
            			}
            			if ( (columnType == DatabaseMetaData.functionColumnResult) || (pos == 0) ) {
            				// Only want to show columns that need to be provided as parameters to the function:
            				// - save the return type string for later use
            				// - multiple result columns may be skipped
            				returnString = " -> " + typeName;
            				continue;
            			}
            			// Added parameters.
            			if ( funcBuilder.charAt(funcBuilder.length() - 1) != '(' ) {
            				// 2nd or greater parameter so need a comma to separate.
            				funcBuilder.append(",");
            			}
            			// Append the column name and type.
            			funcBuilder.append(columnName);
           				funcBuilder.append(" ");
            			funcBuilder.append(typeName);
           				//Message.printStatus(2, routine, "    Processing column name: " + columnName + " " + typeName + " " + columnType);
            		}
            		// Done with ResultSet of columns for the function.
            		rs2.close();
            		// Add all of the functions in the HashMap if not already added.
            		StringBuilder b;
            		for ( Map.Entry<String,StringBuilder> set : funcMap.entrySet() ) {
            			b = set.getValue();
            			b.append(")" + returnString);
            			// Search for the function signature in the list.
            			boolean found = false;
            			String funcSigString = b.toString();
            			for ( String func : funcList ) {
            				if ( func.equals(b) ) {
            					found = true;
            					break;
            				}
            			}
            			if ( !found ) {
            				// Have not already added the function, so add.
            				funcList.add(funcSigString);
            				//Message.printStatus(2, routine, "  Adding final list function: " + funcSigString);
            			}
            		}
            	}
            }
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error getting function list (" + e + ")." );
            Message.printWarning ( 3, routine, e );
            funcList = null;
        }
    }
    if ( funcList == null ) {
        funcList = new ArrayList<>();
    }
    // Sort the functions.
    Collections.sort(funcList, String.CASE_INSENSITIVE_ORDER);
    // Always add a blank option at the start to help with initialization.
    funcList.add ( 0, "" );
    __DataStoreFunction_JComboBox.removeAll();
    for ( String func : funcList ) {
    	__DataStoreFunction_JComboBox.add( func );
    }
    // Select first choice (may get reset from existing parameter values).
    __DataStoreFunction_JComboBox.select ( null );
    if ( __DataStoreFunction_JComboBox.getItemCount() > 0 ) {
        __DataStoreFunction_JComboBox.select ( 0 );
    }
    // Set the datastore for which functions were populated so don't do it unnecessarily.
    lastDataStoreForFunctions = __DataStore_JComboBox.getSelected();
}

/**
Populate the procedure list based on the selected database.
@param dmi DMI to use when selecting procedure list
*/
private void populateDataStoreProcedureChoices ( DMI dmi ) {
    String routine = getClass().getSimpleName() + ".populateDataStoreProcedureChoices";
	if ( lastDataStoreForProcedures.equals(__DataStore_JComboBox.getSelected()) ) {
		// Already populated so don't need to do again (it is a bit slow).
		return;
	}
    List<String> procList = new ArrayList<>();
    //List<String> notIncluded = new ArrayList<>(); // TODO SAM 2012-01-31 need to omit system procedures.
    if ( dmi != null ) {
        try {
        	DatabaseMetaData metadata = dmi.getConnection().getMetaData();
        	// The following will return duplicates for overloaded procedures.
            ResultSet rs = DMIUtil.getDatabaseProcedures (dmi);
            // Iterate through the ResultSet and add procedures:
            // - see note below about how procedure specific name must be used to manage the data
            StringBuilder procBuilder = null;
            short columnType = 0;
            int pos = 0;
            String typeName = null;
            String specificName = null;
            String columnName = null;
            String procNamePrev = "";
            while (rs.next()) {
            	String procName = rs.getString("PROCEDURE_NAME");
            	if ( procName.equals(procNamePrev) ) {
            		// Same procedure name is being processed:
            		// - this will be the case for overloaded procedures
            		// - skip because it will result in redundant processing
            		continue;
            	}
            	// Save the procedure name so is used in the next loop iteration.
            	procNamePrev = procName;
            	// Format the procedure signature as including the parameter names and type.
            	String returnString = "";
            	// Get the metadata for the procedure:
            	// - because procedures may be overloaded, must format the procedure string first and then
            	//   add only if not already in the list
            	if ( metadata.supportsStoredProcedures() ) {
            		// If overloaded, duplicate entries can occur:
            		// - the following resultset is the total of columns for all overloaded procedures
            		// - therefore, must group by the same "SPECIFIC_NAME"
            		// - use a hashmap with key being the specific name
            		HashMap<String,StringBuilder> procMap = new HashMap<>();
            		ResultSet rs2 = metadata.getProcedureColumns(dmi.getConnection().getSchema(), null, procName, null);
            		while (rs2.next()) {
            			// Format the column:
            			// - only list parameters that are passed to the procedure
            			// - return type does not seem to always be set so also check position
            			columnType = rs2.getShort("COLUMN_TYPE");
            			pos = rs2.getShort("ORDINAL_POSITION");
            			typeName = rs2.getString("TYPE_NAME");
            			specificName = rs2.getString("SPECIFIC_NAME");
            			columnName = rs2.getString("COLUMN_NAME");
            			procBuilder = procMap.get(specificName);
            			if ( procBuilder == null ) {
            				// Need to create a new StringBuilder:
            				// - save the StringBuilder so it can be used for matching records
            				procBuilder = new StringBuilder();
            				procMap.put(specificName, procBuilder);
            			}
            			else {
            				// Else, previously added the builder so use it.
            			}
            			if ( procBuilder.length() == 0 ) {
            				// First time the procedure is encountered so add the procedure name at the beginning.
            				procBuilder.append( procName );
           					procBuilder.append("(");
            			}
            			if ( (columnType == DatabaseMetaData.procedureColumnResult) || (pos == 0) ) {
            				// Only want to show columns that need to be provided as parameters to the procedure:
            				// - save the return type string for later use
            				// - multiple result columns may be skipped
            				returnString = " -> " + typeName;
            				continue;
            			}
            			// Added parameters.
            			if ( procBuilder.charAt(procBuilder.length() - 1) != '(' ) {
            				// 2nd or greater parameter so need a comma to separate.
            				procBuilder.append(",");
            			}
            			// Append the column name and type.
            			procBuilder.append(columnName);
           				procBuilder.append(" ");
            			procBuilder.append(typeName);
           				//Message.printStatus(2, routine, "    Processing column name: " + columnName + " " + typeName + " " + columnType);
            		}
            		// Done with ResultSet of columns for the procedure.
            		rs2.close();
            		// Add all of the procedures in the HashMap if not already added.
            		StringBuilder b;
            		for ( Map.Entry<String,StringBuilder> set : procMap.entrySet() ) {
            			b = set.getValue();
            			b.append(")" + returnString);
            			// Search for the procedure signature in the list.
            			boolean found = false;
            			String procSigString = b.toString();
            			for ( String proc : procList ) {
            				if ( proc.equals(b) ) {
            					found = true;
            					break;
            				}
            			}
            			if ( !found ) {
            				// Have not already added the procedure, so add.
            				procList.add(procSigString);
            			}
            		}
            	}
            }
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error getting procedure list (" + e + ")." );
            Message.printWarning ( 3, routine, e );
            procList = null;
        }
    }
    if ( procList == null ) {
        procList = new ArrayList<>();
    }
    // Sort the procedures.
    Collections.sort(procList, String.CASE_INSENSITIVE_ORDER);
    // Always add a blank option at the start to help with initialization.
    procList.add ( 0, "" );
    __DataStoreProcedure_JComboBox.removeAll();
    for ( String func : procList ) {
    	__DataStoreProcedure_JComboBox.add( func );
    }
    // Select first choice (may get reset from existing parameter values).
    __DataStoreProcedure_JComboBox.select ( null );
    if ( __DataStoreProcedure_JComboBox.getItemCount() > 0 ) {
        __DataStoreProcedure_JComboBox.select ( 0 );
    }
    // Set the datastore for which procedures were populated so don't do it unnecessarily.
    lastDataStoreForProcedures = __DataStore_JComboBox.getSelected();
}

/**
Populate the schema list based on the selected database.
@param dmi DMI to use when selecting schema list
*/
private void populateDataStoreSchemaChoices ( DMI dmi )
{   String routine = getClass().getSimpleName() + "populateDataStoreSchemaChoices";
    List<String> schemaList = null;
    List<String> notIncluded = new ArrayList<>(); // TODO SAM 2012-01-31 need to omit system tables.
    if ( dmi == null ) {
        schemaList = new ArrayList<>();
    }
    else {
        // TODO SAM 2013-07-22 Improve this - it should only be shown when the master DB is used.
        try {
            String catalog = __DataStoreCatalog_JComboBox.getSelected();
            if ( catalog.equals("") ) {
                catalog = null;
            }
            schemaList = DMIUtil.getDatabaseSchemaNames(dmi, catalog, true, notIncluded);
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error getting database schemas (" + e + ")." );
            Message.printWarning ( 3, routine, e );
            schemaList = null;
        }
    }
    if ( schemaList == null ) {
        schemaList = new ArrayList<>();
    }
    // Always add a blank option at the start to help with initialization.
    schemaList.add ( 0, "" );
    __DataStoreSchema_JComboBox.removeAll();
    for ( String schema : schemaList ) {
        __DataStoreSchema_JComboBox.add( schema );
    }
    // Select first choice (may get reset from existing parameter values).
    __DataStoreSchema_JComboBox.select ( null );
    if ( __DataStoreSchema_JComboBox.getItemCount() > 0 ) {
        __DataStoreSchema_JComboBox.select ( 0 );
    }
}

/**
Populate the table list based on the selected database.
@param dmi DMI to use when selecting table list
*/
private void populateDataStoreTableChoices ( DMI dmi )
{   String routine = getClass().getSimpleName() + "populateDataStoreTableChoices";
    List<String> tableList = null;
    List<String> notIncluded = new ArrayList<>(); // TODO SAM 2012-01-31 need to omit system tables.
    if ( dmi == null ) {
        tableList = new ArrayList<>();
    }
    else {
        try {
            String dbName = __DataStoreCatalog_JComboBox.getSelected();
            if ( dbName.equals("")) {
                dbName = null;
            }
            String schema = __DataStoreSchema_JComboBox.getSelected();
            if ( schema.equals("")) {
                schema = null;
            }
            tableList = DMIUtil.getDatabaseTableNames(dmi, dbName, schema, true, notIncluded);
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error getting table list (" + e + ")." );
            Message.printWarning ( 3, routine, e );
            tableList = null;
        }
    }
    if ( tableList == null ) {
        tableList = new ArrayList<>();
    }
    // Always add a blank option at the start to help with initialization.
    tableList.add ( 0, "" );
    __DataStoreTable_JComboBox.removeAll();
    for ( String table : tableList ) {
        __DataStoreTable_JComboBox.add( table );
    }
    // Select first choice (may get reset from existing parameter values).
    __DataStoreTable_JComboBox.select ( null );
    if ( __DataStoreTable_JComboBox.getItemCount() > 0 ) {
        __DataStoreTable_JComboBox.select ( 0 );
    }
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
try{
    String DataStore = "";
    String DataStoreCatalog = "";
    String DataStoreSchema = "";
    String DataStoreTable = "";
    String DataStoreColumns = "";
    String OrderBy = "";
    String Top = "";
    String Sql = "";
    String SqlFile = "";
    String DataStoreFunction = "";
    String FunctionParameters = "";
    String DataStoreProcedure = "";
    String ProcedureParameters = "";
    String ProcedureReturnProperty = "";
    String OutputProperties = "";
    String TableID = "";
    String RowCountProperty = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
		DataStore = props.getValue ( "DataStore" );
		DataStoreCatalog = props.getValue ( "DataStoreCatalog" );
		DataStoreSchema = props.getValue ( "DataStoreSchema" );
		DataStoreTable = props.getValue ( "DataStoreTable" );
		DataStoreColumns = props.getValue ( "DataStoreColumns" );
		OrderBy = props.getValue ( "OrderBy" );
		Top = props.getValue ( "Top" );
		Sql = props.getValue ( "Sql" );
		// Replace escaped newline with actual newline so it will display on multiple lines.
		//Message.printStatus(2,routine,"First time - replacing escaped newline with actual newline.");
		if ( (Sql != null) && !Sql.isEmpty() ) {
			Sql = Sql.replace("\\n","\n");
		}
		SqlFile = props.getValue ( "SqlFile" );
		DataStoreFunction = props.getValue ( "DataStoreFunction" );
		FunctionParameters = props.getValue ( "FunctionParameters" );
		DataStoreProcedure = props.getValue ( "DataStoreProcedure" );
		ProcedureParameters = props.getValue ( "ProcedureParameters" );
		ProcedureReturnProperty = props.getValue ( "ProcedureReturnProperty" );
        OutputProperties = props.getValue ( "OutputProperties" );
		TableID = props.getValue ( "TableID" );
        RowCountProperty = props.getValue ( "RowCountProperty" );
        // The data store list is set up in initialize() but is selected here.
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( null ); // To ensure that following causes an event.
            __DataStore_JComboBox.select ( DataStore ); // This will trigger getting the DMI for use in the editor.
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                __DataStore_JComboBox.select ( null ); // To ensure that following causes an event.
                __DataStore_JComboBox.select ( 0 );
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        // First populate the database choices.
        populateDataStoreCatalogChoices(getDMI() );
        // Now select what the command had previously (if specified).
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreCatalog_JComboBox, DataStoreCatalog, JGUIUtil.NONE, null, null ) ) {
            __DataStoreCatalog_JComboBox.select ( DataStoreCatalog );
        }
        else {
            if ( (DataStoreCatalog == null) || DataStoreCatalog.equals("") ) {
                // New command...select the default.
                __DataStoreCatalog_JComboBox.select ( 0 );
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStoreCatalog parameter \"" + DataStoreCatalog + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        // First populate the database schema.
        populateDataStoreSchemaChoices(getDMI() );
        // Now select what the command had previously (if specified).
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreSchema_JComboBox, DataStoreSchema, JGUIUtil.NONE, null, null ) ) {
            __DataStoreSchema_JComboBox.select ( DataStoreSchema );
        }
        else {
            if ( (DataStoreSchema == null) || DataStoreSchema.equals("") ) {
                // New command...select the default.
                __DataStoreSchema_JComboBox.select ( 0 );
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStoreSchema parameter \"" + DataStoreSchema + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        // First populate the table choices.
        populateDataStoreTableChoices(getDMI() );
        // Now select what the command had previously (if specified).
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreTable_JComboBox, DataStoreTable, JGUIUtil.NONE, null, null ) ) {
            __DataStoreTable_JComboBox.select ( DataStoreTable );
            __main_JTabbedPane.setSelectedIndex(0);
        }
        else {
            if ( (DataStoreTable == null) || DataStoreTable.equals("") ) {
                // New command...select the default.
                __DataStoreTable_JComboBox.select ( 0 );
            }
            else {
                // Bad user command.
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStoreTable parameter \"" + DataStoreTable + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( DataStoreColumns != null ) {
            __DataStoreColumns_JTextField.setText ( DataStoreColumns );
        }
        if ( OrderBy != null ) {
            __OrderBy_JTextField.setText ( OrderBy );
        }
        if ( Top != null ) {
            __Top_JTextField.setText ( Top );
        }
        if ( (Sql != null) && !Sql.equals("") ) {
            __Sql_JTextArea.setText ( Sql );
            __main_JTabbedPane.setSelectedIndex(1);
        }
        if ( (SqlFile != null) && !SqlFile.equals("") ) {
            __SqlFile_JTextField.setText(SqlFile);
            __main_JTabbedPane.setSelectedIndex(2);
        }
        // First populate the function choices:
        // - lazy load since can be slow
        if ( (DataStoreFunction != null) && !DataStoreFunction.isEmpty() ) {
        	populateDataStoreFunctionChoices(getDMI());
        	// Now select what the command had previously (if specified).
        	if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreFunction_JComboBox, DataStoreFunction, JGUIUtil.NONE, null, null ) ) {
        		__DataStoreFunction_JComboBox.select ( DataStoreFunction );
            	if ( (DataStoreFunction != null) && !DataStoreFunction.equals("") ) {
            		__main_JTabbedPane.setSelectedIndex(3);
            	}
        	}
        	else {
        		if ( (DataStoreFunction == null) || DataStoreFunction.equals("") ) {
        			// New command, select the default.
        			__DataStoreFunction_JComboBox.select ( 0 );
        		}
        		else {
        			// Bad user command.
        			Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
       					"DataStoreFunction parameter \"" + DataStoreFunction + "\".  Select a\ndifferent value or Cancel." );
        		}
            }
        }
        // First populate the procedure choices:
        // - lazy load since can be slow
        if ( (DataStoreProcedure != null) && !DataStoreProcedure.isEmpty() ) {
        	populateDataStoreProcedureChoices(getDMI() );
        	// Now select what the command had previously (if specified).
        	if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreProcedure_JComboBox, DataStoreProcedure, JGUIUtil.NONE, null, null ) ) {
            	__DataStoreProcedure_JComboBox.select ( DataStoreProcedure );
            	if ( (DataStoreProcedure != null) && !DataStoreProcedure.equals("") ) {
                	__main_JTabbedPane.setSelectedIndex(4);
            	}
        	}
        	else {
            	if ( (DataStoreProcedure == null) || DataStoreProcedure.equals("") ) {
                	// New command...select the default.
                	__DataStoreProcedure_JComboBox.select ( 0 );
            	}
            	else {
                	// Bad user command.
                	Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  	"DataStoreProcedure parameter \"" + DataStoreProcedure + "\".  Select a\ndifferent value or Cancel." );
            	}
        	}
        }
        if ( ProcedureParameters != null ) {
            __ProcedureParameters_JTextArea.setText ( ProcedureParameters );
        }
        if ( ProcedureReturnProperty != null ) {
            __ProcedureReturnProperty_JTextField.setText ( ProcedureReturnProperty );
        }
        if ( OutputProperties != null ) {
            __OutputProperties_JTextArea.setText ( OutputProperties );
        }
        if ( TableID != null ) {
            __TableID_JTextField.setText ( TableID );
        }
        if ( RowCountProperty != null ) {
            __RowCountProperty_JTextField.setText ( RowCountProperty );
        }
	}
	// Regardless, reset the command from the fields.
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
    DataStoreCatalog = __DataStoreCatalog_JComboBox.getSelected();
    if ( DataStoreCatalog == null ) {
        DataStoreCatalog = "";
    }
    DataStoreSchema = __DataStoreSchema_JComboBox.getSelected();
    if ( DataStoreSchema == null ) {
        DataStoreSchema = "";
    }
    DataStoreTable = __DataStoreTable_JComboBox.getSelected();
    if ( DataStoreTable == null ) {
        DataStoreTable = "";
    }
	DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
	OrderBy = __OrderBy_JTextField.getText().trim();
	Top = __Top_JTextField.getText().trim();
	Sql = __Sql_JTextArea.getText().trim();
    if ( Sql != null ) {
    	// Replace internal newline with escaped string for command text.
		//Message.printStatus(2,routine,"Replacing actual newline with escaped newline in Sql parameter value.");
    	Sql = Sql.replace("\n", "\\n");
    }
	SqlFile = __SqlFile_JTextField.getText().trim();
    DataStoreFunction = __DataStoreFunction_JComboBox.getSelected();
    if ( DataStoreFunction == null ) {
        DataStoreFunction = "";
    }
	FunctionParameters = __FunctionParameters_JTextArea.getText().trim().replace("\n"," ");
    DataStoreProcedure = __DataStoreProcedure_JComboBox.getSelected();
    if ( DataStoreProcedure == null ) {
        DataStoreProcedure = "";
    }
	ProcedureParameters = __ProcedureParameters_JTextArea.getText().trim().replace("\n"," ");
	ProcedureReturnProperty = __ProcedureReturnProperty_JTextField.getText().trim();
	OutputProperties = __OutputProperties_JTextArea.getText().trim().replace("\n"," ");
    TableID = __TableID_JTextField.getText().trim();
	RowCountProperty = __RowCountProperty_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "DataStore=" + DataStore );
	props.add ( "DataStoreCatalog=" + DataStoreCatalog );
	props.add ( "DataStoreSchema=" + DataStoreSchema );
	props.add ( "DataStoreTable=" + DataStoreTable );
	props.add ( "DataStoreColumns=" + DataStoreColumns );
	props.add ( "OrderBy=" + OrderBy );
	props.add ( "Top=" + Top );
	// Surround with double quotes because = in content causes the parse in called code to not work.
	props.add ( "Sql=\"" + Sql + "\"" );
	props.add ( "SqlFile=" + SqlFile);
	props.add ( "DataStoreFunction=" + DataStoreFunction );
	props.add ( "FunctionParameters=" + FunctionParameters );
	props.add ( "DataStoreProcedure=" + DataStoreProcedure );
	props.add ( "ProcedureParameters=" + ProcedureParameters );
	props.add ( "ProcedureReturnProperty=" + ProcedureReturnProperty );
	props.add ( "OutputProperties=" + OutputProperties );
    props.add ( "TableID=" + TableID );
	props.add ( "RowCountProperty=" + RowCountProperty );
    
	__command_JTextArea.setText( __command.toString ( props ) );
	// Check the path and determine what the label on the path button should be.
	if ( __path_JButton != null ) {
		if ( (SqlFile != null) && !SqlFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( SqlFile );
			if ( f.isAbsolute() ) {
				__path_JButton.setText ( __RemoveWorkingDirectory );
				__path_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__path_JButton.setText ( __AddWorkingDirectory );
		    	__path_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path_JButton.setEnabled(false);
		}
	}
}
catch ( Exception e ) {
    Message.printWarning ( 3, routine, e );
}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok().
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out!
			return;
		}
	}
	// Now close out.
	setVisible( false );
	dispose();
}

/**
 * Handle JTabbedPane changes.
 */
public void stateChanged ( ChangeEvent event ) {
	JTabbedPane sourceTabbedPane = (JTabbedPane)event.getSource();
	int index = sourceTabbedPane.getSelectedIndex();
	if ( index == 3 ) {
		// Populate the function list if not done already:
		// - set the index first because otherwise there is a pause
        __main_JTabbedPane.setSelectedIndex(3);
		populateDataStoreFunctionChoices(getDMI());
	}
	else if ( index == 4 ) {
		// Populate the procedure list if not done already:
		// - set the index first because otherwise there is a pause
        __main_JTabbedPane.setSelectedIndex(4);
		populateDataStoreProcedureChoices(getDMI());
	}
}

/**
Responds to WindowEvents.
@param event WindowEvent object 
*/
public void windowClosing(WindowEvent event) {
	response ( false );
}

// The following methods are all necessary because this class implements WindowListener.
public void windowActivated(WindowEvent evt)	{}
public void windowClosed(WindowEvent evt)	{}
public void windowDeactivated(WindowEvent evt)	{}
public void windowDeiconified(WindowEvent evt)	{}
public void windowIconified(WindowEvent evt)	{}
public void windowOpened(WindowEvent evt)	{}

}