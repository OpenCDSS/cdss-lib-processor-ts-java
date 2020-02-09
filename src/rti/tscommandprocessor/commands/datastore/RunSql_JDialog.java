// RunSql_JDialog - editor for RunSql_JDialog command

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
import java.util.ArrayList;
import java.util.List;

import RTi.DMI.DMI;
import RTi.DMI.DMIStatement;
import RTi.DMI.DMIStoredProcedureData;
import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command parameter editor.
*/
@SuppressWarnings("serial")
public class RunSql_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
    
private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __DataStore_JComboBox = null;
private JTabbedPane __sql_JTabbedPane = null;
private JTextArea __Sql_JTextArea = null;
private JTextField __SqlFile_JTextField = null;
private SimpleJComboBox __DataStoreProcedure_JComboBox = null;
private JTextArea __ProcedureParameters_JTextArea = null;
private JTextField __ProcedureReturnProperty_JTextField = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private RunSql_Command __command = null;
private boolean __ok = false;
private String __working_dir = null;
private JFrame __parent = null;

private boolean __ignoreItemEvents = false; // Used to ignore cascading events when working with choices

private DatabaseDataStore __dataStore = null; // selected data store
private DMI __dmi = null; // DMI to do queries.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public RunSql_JDialog ( JFrame parent, RunSql_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	String routine = getClass().getSimpleName() + ".actionPerformed";
	Object o = event.getSource();

    if ( o == __browse_JButton ) {
        // Browse for the file to read...
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
					Message.printWarning ( 1,"RunSql_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory( directory);
                refresh();
            }
        }
    }
    else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", __command.getCommandName());
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited...
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
    else if ( event.getActionCommand().equalsIgnoreCase("EditProcedureParameters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ProcedureParameters = __ProcedureParameters_JTextArea.getText().trim();
        List<String> notesList = new ArrayList<>();
        String procedureName = __DataStoreProcedure_JComboBox.getSelected();
        if ( (procedureName == null) || procedureName.isEmpty() ) {
        	notesList.add("Procedure to run:  NOT SPECIFIED" );
        }
        else {
        	notesList.add("Procedure to run:  " + procedureName );
        	// Get the parameter names and types for the procedure
        	// - Need to declare a procedure
        	// - Use a generic statement that can handle any type of statement
        	DMIStatement q = new DMIStatement(__dmi);
        	DMIStoredProcedureData procedureData = null;
        	try {
    	    	procedureData = new DMIStoredProcedureData(__dmi,procedureName);
    	    	q.setStoredProcedureData(procedureData);
    	    	if ( procedureData.hasReturnValue() ) {
    	    		notesList.add("Procedure has return value of type: " + procedureData.getReturnTypeString() );
    	    	}
    	    	else {
    	    		notesList.add("Procedure does not return a value." );
    	    	}
        		int nParams = procedureData.getNumParameters();
        		if ( nParams > 0 ) {
        			notesList.add("Procedure has " + nParams + " parameters.");
        			notesList.add("Specify procedure parameter name and values, with parameter values that match the type, as follows:");
    	    		// Get the procedure information
        			for ( int i = 0; i < nParams; i++ ) {
    	    			notesList.add("    Parameter " + (i + 1) + ": name = \"" + procedureData.getParameterName(i) + "\", type=\"" + procedureData.getParameterTypeString(i) + "\"");
        			}
        			notesList.add("The parameter names provided to the command are for information.");
        			notesList.add("The parameter order and data type when calling the procedure are critical.");
        			notesList.add("For example, make sure to provide properly-formatted numbers if type indicates numeric input.");
        			notesList.add("Format date/times using syntax YYYY-MM-DD hh:mm:ss to appropriate precision for date and date/time.");
        		}
        		else {
        			notesList.add("The procedure does not have any input parameters.");
        			notesList.add("Therefore this command parameter (ProcedureParameters) is not used.");
        		}
        	}
        	catch ( Exception e ) {
    	    	Message.printWarning(3, routine, "Unable to created procedure object for \"" + procedureName + "\"" );
    	    	notesList.add("Unable to determine procedure metadata.  Check log file for errors.");
        	}
        }
        String [] notes = notesList.toArray(new String[0]);
        String dict = (new DictionaryJDialog ( __parent, true, ProcedureParameters,
            "Edit ProcedureParameters parameter", notes, "Parameter Name", "Parameter Value",10)).response();
        if ( dict != null ) {
            __ProcedureParameters_JTextArea.setText ( dict );
            refresh();
        }
    }
}

/**
Refresh the database choices in response to the currently selected datastore.
*/
private void actionPerformedDataStoreSelected ( )
{
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    __dataStore = getSelectedDataStore();
    __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    // Update list of procedures
    populateDataStoreProcedureChoices(getDMI() );
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
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
	String Sql = __Sql_JTextArea.getText().trim();
	String SqlFile = __SqlFile_JTextField.getText().trim();
	String DataStoreProcedure = __DataStoreProcedure_JComboBox.getSelected();
	String ProcedureParameters = __ProcedureParameters_JTextArea.getText().trim().replace("\n"," ");
	String ProcedureReturnProperty = __ProcedureReturnProperty_JTextField.getText().trim();
	__error_wait = false;

    if ( Sql.length() > 0 ) {
        props.set ( "Sql", Sql );
    }
    if ( SqlFile.length() > 0 ) {
        props.set ( "SqlFile", SqlFile );
    }
    if ( DataStoreProcedure.length() > 0 ) {
        props.set ( "DataStoreProcedure", DataStoreProcedure );
    }
    if ( ProcedureParameters.length() > 0 ) {
        props.set ( "ProcedureParameters", ProcedureParameters );
    }
    if ( ProcedureReturnProperty.length() > 0 ) {
        props.set ( "ProcedureReturnProperty", ProcedureReturnProperty );
    }
	try {
	    // This will warn the user...
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
    String Sql = __Sql_JTextArea.getText().trim().replace('\n', ' ').replace('\t', ' ');
    String SqlFile = __SqlFile_JTextField.getText().trim();
    String DataStoreProcedure = __DataStoreProcedure_JComboBox.getSelected();
	String ProcedureParameters = __ProcedureParameters_JTextArea.getText().trim().replace("\n"," ");
	String ProcedureReturnProperty = __ProcedureReturnProperty_JTextField.getText().trim();
    __command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "Sql", Sql );
	__command.setCommandParameter ( "SqlFile", SqlFile );
	__command.setCommandParameter ( "DataStoreProcedure", DataStoreProcedure );
	__command.setCommandParameter ( "ProcedureParameters", ProcedureParameters );
	__command.setCommandParameter ( "ProcedureReturnProperty", ProcedureReturnProperty );
}

/**
Return the DMI that is currently being used for database interaction, based on the selected data store.
*/
private DMI getDMI ()
{
    return __dmi;
}

/**
Get the selected data store.
*/
private DatabaseDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    DatabaseDataStore dataStore = (DatabaseDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, DatabaseDataStore.class );
    if ( dataStore != null ) {
        Message.printStatus(2, routine, "Selected data store is \"" + dataStore.getName() + "\"." );
        // Make sure database connection is open
        dataStore.checkDatabaseConnection();
    }
    else {
        Message.printStatus(2, routine, "Cannot get data store for \"" + DataStore + "\"." );
    }
    return dataStore;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, RunSql_Command command )
{	__command = command;
	__parent = parent;
	CommandProcessor processor = __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = -1;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = -1;

   	JGUIUtil.addComponent(paragraph, new JLabel (
        "This command runs an SQL statement, procedure, or function on the specified database datastore."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The SQL statement can be specified in one of three ways - use the tabs below to do so."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "An SQL statement specified with ${property} notation will be updated to use processor property values before executing the SQL statement."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "SQL syntax may vary between database engines.  See http://www.w3schools.com/sql for an SQL reference."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Use the ReadTableFromDataStore(Top=...) command to view table columns.  " +
        "Right click on the table ID in the TSTool results to display table properties."),
        0, ++yy, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    // List available data stores of the correct type
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStore_JComboBox = new SimpleJComboBox ( false );
    TSCommandProcessor tsProcessor = (TSCommandProcessor)processor;
    List<DataStore> dataStoreList = tsProcessor.getDataStoresByType( DatabaseDataStore.class );
    List<String>dataStoreChoices = new ArrayList<String>();
    for ( DataStore dataStore: dataStoreList ) {
    	dataStoreChoices.add ( dataStore.getName() );
    }
    if ( dataStoreList.size() == 0 ) {
        // Add an empty item so users can at least bring up the editor
    	dataStoreChoices.add ( "" );
    }
    __DataStore_JComboBox.setData(dataStoreChoices);
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - datastore containing data to read."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __sql_JTabbedPane = new JTabbedPane ();
    __sql_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify SQL" ));
    JGUIUtil.addComponent(main_JPanel, __sql_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for SQL via SQL statement
    int ySql = -1;
    JPanel sql_JPanel = new JPanel();
    sql_JPanel.setLayout( new GridBagLayout() );
    __sql_JTabbedPane.addTab ( "SQL string", sql_JPanel );
    
    JGUIUtil.addComponent(sql_JPanel, new JLabel (
        "Specify the SQL statement string as a command parameter below."),
        0, ++ySql, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(sql_JPanel, new JLabel (
        "If special characters conflict with TSTool conventions, then save the SQL in a file and specify the file as input."),
        0, ++ySql, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(sql_JPanel, new JLabel (
        "Because the SQL statement is in a parameter, newlines are not allowed."),
        0, ++ySql, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(sql_JPanel, new JLabel ("SQL String:"), 
        0, ++ySql, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Sql_JTextArea = new JTextArea (9,50);
    __Sql_JTextArea.setLineWrap ( true );
    __Sql_JTextArea.setWrapStyleWord ( true );
    __Sql_JTextArea.addKeyListener(this);
    JGUIUtil.addComponent(sql_JPanel, new JScrollPane(__Sql_JTextArea),
        1, ySql, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for SQL via SQL file
    int yFile = -1;
    JPanel file_JPanel = new JPanel();
    file_JPanel.setLayout( new GridBagLayout() );
    __sql_JTabbedPane.addTab ( "SQL file", file_JPanel );

    JGUIUtil.addComponent(file_JPanel, new JLabel (
        "Read the SQL statement from a file."),
        0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(file_JPanel, new JLabel (
        "This is useful for a complex SQL statement and when the SQL will be shared with other tools)."),
        0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(file_JPanel, new JLabel (
        "The working directory is: " + __working_dir ), 
        0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(file_JPanel, new JLabel ( "SQL file to read:" ), 
        0, ++yFile, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SqlFile_JTextField = new JTextField ( 50 );
    __SqlFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel SqlFile_JPanel = new JPanel();
	SqlFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(SqlFile_JPanel, __SqlFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(SqlFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(SqlFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(file_JPanel, SqlFile_JPanel,
		1, yFile, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    // Panel for procedure
    int yProc = -1;
    JPanel proc_JPanel = new JPanel();
    proc_JPanel.setLayout( new GridBagLayout() );
    __sql_JTabbedPane.addTab ( "Procedure", proc_JPanel );

    JGUIUtil.addComponent(proc_JPanel, new JLabel ("Run a stored procedure)."),
        0, ++yProc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(proc_JPanel, new JLabel (
        "Under development - use the Edit button to see return value and parameters for procedures."),
        0, ++yProc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(proc_JPanel, new JLabel ( "Datastore procedure:"),
        0, ++yProc, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreProcedure_JComboBox = new SimpleJComboBox ( false );
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

    // Panel for function

    int yFunc = -1;
    JPanel func_JPanel = new JPanel();
    func_JPanel.setLayout( new GridBagLayout() );
    __sql_JTabbedPane.addTab ( "Function", func_JPanel );

    JGUIUtil.addComponent(func_JPanel, new JLabel ("Run a function."),
        0, ++yFunc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(func_JPanel, new JLabel (
        "Under development - databases vary in how they define functions and procedures.  They may be equivalent."),
        0, ++yFunc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(func_JPanel, new JLabel (
        "Additional features will be added to run functions, as necessary."),
        0, ++yFunc, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (6,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South JPanel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add (__ok_JButton);
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command");
    pack();
    JGUIUtil.center(this);
	refresh();
	setResizable (false);
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
    }
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed (KeyEvent event) {
	int code = event.getKeyCode();

	if (code == KeyEvent.VK_ENTER) {
		refresh ();
		checkInput();
		if (!__error_wait) {
			response ( true );
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
Populate the procedure list based on the selected database.
@param dmi DMI to use when selecting procedure list
*/
private void populateDataStoreProcedureChoices ( DMI dmi )
{   String routine = getClass().getSimpleName() + "populateDataStoreProcedureChoices";
    List<String> procList = null;
    List<String> notIncluded = new ArrayList<>(); // TODO SAM 2012-01-31 need to omit system procedures
    if ( dmi == null ) {
        procList = new ArrayList<>();
    }
    else {
        try {
        	boolean returnSpecificName = true; // Needed for overloaded functions
            procList = DMIUtil.getDatabaseProcedureNames(dmi, true, notIncluded, returnSpecificName );
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error getting procedure list (" + e + ")." );
            Message.printWarning ( 3, routine, e );
            procList = null;
        }
    }
    if ( procList == null ) {
        procList = new ArrayList<String>();
    }
    // Always add a blank option at the start to help with initialization
    procList.add ( 0, "" );
    __DataStoreProcedure_JComboBox.removeAll();
    for ( String proc : procList ) {
        __DataStoreProcedure_JComboBox.add( proc );
    }
    // Set large so that new procedure list from selected datastore does not foul up layout
    String longest = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    __DataStoreProcedure_JComboBox.setPrototypeDisplayValue(longest);
    // Select first choice (may get reset from existing parameter values).
    __DataStoreProcedure_JComboBox.select ( null );
    if ( __DataStoreProcedure_JComboBox.getItemCount() > 0 ) {
        __DataStoreProcedure_JComboBox.select ( 0 );
    }
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
try{
    String DataStore = "";
    String Sql = "";
    String SqlFile = "";
    String DataStoreProcedure = "";
    String ProcedureParameters = "";
    String ProcedureReturnProperty = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
		DataStore = props.getValue ( "DataStore" );
		Sql = props.getValue ( "Sql" );
		SqlFile = props.getValue ( "SqlFile" );
		DataStoreProcedure = props.getValue ( "DataStoreProcedure" );
		ProcedureParameters = props.getValue ( "ProcedureParameters" );
		ProcedureReturnProperty = props.getValue ( "ProcedureReturnProperty" );
        // The data store list is set up in initialize() but is selected here
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
            __DataStore_JComboBox.select ( null ); // To ensure that following causes an event
            __DataStore_JComboBox.select ( DataStore ); // This will trigger getting the DMI for use in the editor
        }
        else {
            if ( (DataStore == null) || DataStore.equals("") ) {
                // New command...select the default...
                __DataStore_JComboBox.select ( null ); // To ensure that following causes an event
                __DataStore_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( (Sql != null) && !Sql.equals("") ) {
            __Sql_JTextArea.setText ( Sql );
            __sql_JTabbedPane.setSelectedIndex(0);
        }
        if ( (SqlFile != null) && !SqlFile.equals("") ) {
            __SqlFile_JTextField.setText(SqlFile);
            __sql_JTabbedPane.setSelectedIndex(1);
        }
        // First populate the procedure choices...
        populateDataStoreProcedureChoices(getDMI() );
        // Now select what the command had previously (if specified)...
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreProcedure_JComboBox, DataStoreProcedure, JGUIUtil.NONE, null, null ) ) {
            __DataStoreProcedure_JComboBox.select ( DataStoreProcedure );
            if ( (DataStoreProcedure != null) && !DataStoreProcedure.equals("") ) {
                __sql_JTabbedPane.setSelectedIndex(2);
            }
        }
        else {
            if ( (DataStoreProcedure == null) || DataStoreProcedure.equals("") ) {
                // New command...select the default...
                __DataStoreProcedure_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStoreProcedure parameter \"" + DataStoreProcedure + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( ProcedureParameters != null ) {
            __ProcedureParameters_JTextArea.setText ( ProcedureParameters );
        }
        if ( ProcedureReturnProperty != null ) {
            __ProcedureReturnProperty_JTextField.setText ( ProcedureReturnProperty );
        }
	}
	// Regardless, reset the command from the fields...
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
	Sql = __Sql_JTextArea.getText().trim();
	SqlFile = __SqlFile_JTextField.getText().trim();
    DataStoreProcedure = __DataStoreProcedure_JComboBox.getSelected();
    if ( DataStoreProcedure == null ) {
        DataStoreProcedure = "";
    }
	ProcedureParameters = __ProcedureParameters_JTextArea.getText().trim().replace("\n"," ");
	ProcedureReturnProperty = __ProcedureReturnProperty_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "DataStore=" + DataStore );
	props.add ( "Sql=" + Sql );
	props.add ( "SqlFile=" + SqlFile);
	props.add ( "DataStoreProcedure=" + DataStoreProcedure );
	props.add ( "DataStoreProcedure=" + DataStoreProcedure );
	props.add ( "ProcedureParameters=" + ProcedureParameters );
	__command_JTextArea.setText( __command.toString ( props ) );
	// Check the path and determine what the label on the path button should be...
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
{	__ok = ok;	// Save to be returned by ok()
	if ( ok ) {
		// Commit the changes...
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out!
			return;
		}
	}
	// Now close out...
	setVisible( false );
	dispose();
}

/**
Responds to WindowEvents.
@param event WindowEvent object 
*/
public void windowClosing(WindowEvent event) {
	response ( false );
}

// The following methods are all necessary because this class implements WindowListener
public void windowActivated(WindowEvent evt)	{}
public void windowClosed(WindowEvent evt)	{}
public void windowDeactivated(WindowEvent evt)	{}
public void windowDeiconified(WindowEvent evt)	{}
public void windowIconified(WindowEvent evt)	{}
public void windowOpened(WindowEvent evt)	{}

}
