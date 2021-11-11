// DeleteDataStoreTableRows_JDialog - editor for DeleteDataStoreTableRows command

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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import riverside.datastore.DataStore;
import rti.tscommandprocessor.core.TSCommandProcessor;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import RTi.DMI.DMI;
import RTi.DMI.DMIUtil;
import RTi.DMI.DatabaseDataStore;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class DeleteDataStoreTableRows_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private boolean __error_wait = false;
private boolean __first_time = true;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __DataStore_JComboBox = null;
private SimpleJComboBox __DataStoreTable_JComboBox = null;
//private SimpleJComboBox __TableID_JComboBox = null;
private SimpleJComboBox __DeleteAllRows_JComboBox = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private DeleteDataStoreTableRows_Command __command = null;
private boolean __ok = false;

private boolean __ignoreItemEvents = false; // Used to ignore cascading events when working with choices

private DatabaseDataStore __dataStore = null; // selected data store
private DMI __dmi = null; // DMI to do queries.
private List<DatabaseDataStore> datastores = new ArrayList<>();

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
@param datastores list of databases
*/
public DeleteDataStoreTableRows_JDialog ( JFrame parent, DeleteDataStoreTableRows_Command command,
    List<String> tableIDChoices, List<DatabaseDataStore> datastores )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices, datastores );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event)
{	Object o = event.getSource();

    if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "DeleteDataStoreTableRows");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited...
			response ( true );
		}
	}
}

/**
Refresh the data type choices in response to the currently selected RiversideDB data store.
@param value if non-null, then the selection is from the command initialization, in which case the
specified data type should be selected
*/
private void actionPerformedDataStoreSelected ( )
{
    if ( __DataStore_JComboBox.getSelected() == null ) {
        // Startup initialization
        return;
    }
    __dataStore = getSelectedDataStore();
    __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    //Message.printStatus(2, "", "Selected data store " + __dataStore + " __dmi=" + __dmi );
    // Now populate the data type choices corresponding to the data store
    populateDataStoreTableChoices ( __dmi );
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
    String DataStore = __DataStore_JComboBox.getSelected();
	String DataStoreTable = __DataStoreTable_JComboBox.getSelected();
	String DeleteAllRows = __DeleteAllRows_JComboBox.getSelected();
	//String TableID = __TableID_JComboBox.getSelected();
	//String DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
	__error_wait = false;

    if ( (DataStore != null) && DataStore.length() > 0 ) {
        props.set ( "DataStore", DataStore );
        __dataStore = getSelectedDataStore();
        __dmi = ((DatabaseDataStore)__dataStore).getDMI();
    }
    else {
        props.set ( "DataStore", "" );
    }
	if ( DataStoreTable.length() > 0 ) {
		props.set ( "DataStoreTable", DataStoreTable );
	}
    if ( DeleteAllRows.length() > 0 ) {
        props.set ( "DeleteAllRows", DeleteAllRows );
    }
    //if ( TableID.length() > 0 ) {
    //    props.set ( "TableID", TableID );
    //}
    //if ( DataStoreColumns.length() > 0 ) {
    //    props.set ( "DataStoreColumns", DataStoreColumns );
    //}
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
{	
    String DataStore = __DataStore_JComboBox.getSelected();
    String DataStoreTable = __DataStoreTable_JComboBox.getSelected();
    //String DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
    //String TableID = __TableID_JComboBox.getSelected();
    String DeleteAllRows = __DeleteAllRows_JComboBox.getSelected();
    __command.setCommandParameter ( "DataStore", DataStore );
	__command.setCommandParameter ( "DataStoreTable", DataStoreTable );
    //__command.setCommandParameter ( "TableID", TableID );
	//__command.setCommandParameter ( "DataStoreColumns", DataStoreColumns );
	__command.setCommandParameter ( "DeleteAllRows", DeleteAllRows );
}

/**
Return the DMI that is currently being used for database interaction, based on the selected data store.
*/
private DMI getDMI ()
{
    return __dmi;
}

// TODO smalers 2021-10-24 remove when other code tests out.
/**
Get the selected data store.
*/
/*
private DatabaseDataStore getSelectedDataStore ()
{   String routine = getClass().getName() + ".getSelectedDataStore";
    String DataStore = __DataStore_JComboBox.getSelected();
    DatabaseDataStore dataStore = (DatabaseDataStore)((TSCommandProcessor)
        __command.getCommandProcessor()).getDataStoreForName(
        DataStore, DatabaseDataStore.class );
    if ( dataStore != null ) {
        Message.printStatus(2, routine, "Selected data store is \"" + dataStore.getName() + "\"." );
    }
    else {
        Message.printStatus(2, routine, "Cannot get data store for \"" + DataStore + "\"." );
    }
    return dataStore;
}
*/

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
@param tableIDChoices list of table identifiers to provide as choices
@param datastores list of database datastores
*/
private void initialize ( JFrame parent, DeleteDataStoreTableRows_Command command, List<String> tableIDChoices, List<DatabaseDataStore> datastores )
{	this.__command = command;
    this.datastores = datastores;
	TSCommandProcessor processor = (TSCommandProcessor)__command.getCommandProcessor();

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
        "This command deletes rows (records) from a database datastore table."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Currently the DeleteAllRows parameter is the only way to control the deletion."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "In the future the command will be updated to include a WHERE capability."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    // List available data stores of the correct type
    
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
        // Add an empty item so users can at least bring up the editor
    	datastoreChoices.add ( "" );
    }
    __DataStore_JComboBox.setData(datastoreChoices);
    __DataStore_JComboBox.select ( 0 );
    __DataStore_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStore_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - database datastore to delete rows."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Data tables are particular to the data store...
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore table/view:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreTable_JComboBox = new SimpleJComboBox ( false );
    __DataStoreTable_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DataStoreTable_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - database table/view to delete rows."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - original table."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table columns to write:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreColumns_JTextField = new JTextField (10);
    __DataStoreColumns_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __DataStoreColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - table/view columns, separated by commas (default=all)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Delete all rows?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteAllRows_JComboBox = new SimpleJComboBox ( false );
    List<String> deleteAllRowsChoices = new Vector<String>();
    deleteAllRowsChoices.add("");
    deleteAllRowsChoices.add(__command._False);
    deleteAllRowsChoices.add(__command._True);
    deleteAllRowsChoices.add(__command._Truncate);
    __DeleteAllRows_JComboBox.setData ( deleteAllRowsChoices );
    __DeleteAllRows_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __DeleteAllRows_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - remove all rows (default=" + __command._False + ")?"), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (6,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
            // User has selected a data store.
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
Populate the data type list based on the selected database.
@param dmi DMI to use when selecting table list
*/
private void populateDataStoreTableChoices ( DMI dmi )
{   String routine = getClass().getName() + "populateDataStoreTableChoices";
    //__TableID_JTextField.removeAll ();
    List<String> tableList = null;
    List<String> notIncluded = new Vector<String>(); // TODO SAM 2012-01-31 need to omit system tables
    if ( dmi == null ) {
        tableList = new Vector<String>();
    }
    else {
        try {
            tableList = DMIUtil.getDatabaseTableNames(dmi, null, null, true, notIncluded);
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error getting tables table list (" + e + ")." );
            Message.printWarning ( 3, routine, e );
            tableList = null;
        }
    }
    if ( tableList == null ) {
        tableList = new Vector<String>();
    }
    // Always add a blank option at the start to help with initialization
    tableList.add ( 0, "" );
    __DataStoreTable_JComboBox.removeAll();
    for ( String table : tableList ) {
        __DataStoreTable_JComboBox.add( table );
    }
    // Set large so that new table list from selected datastore does not found up layout
    String longest = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    __DataStoreTable_JComboBox.setPrototypeDisplayValue(longest);
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
{	String routine = getClass().getName() + ".refresh";
try{
    String DataStore = "";
    String DataStoreTable = "";
    //String TableID = "";
    //String DataStoreColumns = "";
    String DeleteAllRows = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
		DataStore = props.getValue ( "DataStore" );
		DataStoreTable = props.getValue ( "DataStoreTable" );
        //TableID = props.getValue ( "TableID" );
		//DataStoreColumns = props.getValue ( "DataStoreColumns" );
		DeleteAllRows = props.getValue ( "DeleteAllRows" );
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
        // First populate the data type choices...
        populateDataStoreTableChoices(getDMI() );
        // Now select what the command had previously (if specified)...
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreTable_JComboBox, DataStoreTable, JGUIUtil.NONE, null, null ) ) {
            __DataStoreTable_JComboBox.select ( DataStoreTable );
        }
        else {
            if ( (DataStoreTable == null) || DataStoreTable.equals("") ) {
                // New command...select the default...
                __DataStoreTable_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStoreTable parameter \"" + DataStoreTable + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        /*
        if ( TableID == null ) {
            // Select default...
            __TableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableID value \"" + TableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( DataStoreColumns != null ) {
            __DataStoreColumns_JTextField.setText ( DataStoreColumns );
        }
        */
        if ( JGUIUtil.isSimpleJComboBoxItem(__DeleteAllRows_JComboBox, DeleteAllRows, JGUIUtil.NONE, null, null ) ) {
            __DeleteAllRows_JComboBox.select ( DeleteAllRows );
        }
        else {
            if ( (DeleteAllRows == null) || DeleteAllRows.equals("") ) {
                // New command...select the default...
                __DeleteAllRows_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DeleteAllRows parameter \"" + DeleteAllRows + "\".  Select a\ndifferent value or Cancel." );
            }
        }
	}
	// Regardless, reset the command from the fields...
    DataStore = __DataStore_JComboBox.getSelected();
    if ( DataStore == null ) {
        DataStore = "";
    }
    DataStoreTable = __DataStoreTable_JComboBox.getSelected();
    if ( DataStoreTable == null ) {
        DataStoreTable = "";
    }
    //TableID = __TableID_JComboBox.getSelected();
	//DataStoreColumns = __DataStoreColumns_JTextField.getText().trim();
    DeleteAllRows = __DeleteAllRows_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "DataStore=" + DataStore );
	props.add ( "DataStoreTable=" + DataStoreTable );
    //props.add ( "TableID=" + TableID );
	//props.add ( "DataStoreColumns=" + DataStoreColumns );
	props.add ( "DeleteAllRows=" + DeleteAllRows );
	__command_JTextArea.setText( __command.toString ( props ) );
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
