// OpenDataStore_JDialog - editor for OpenDataStore command

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
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

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
import java.util.List;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class OpenDataStore_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
    
private boolean __error_wait = false;
private boolean __first_time = true;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea=null;
private JTextField __DataStoreName_JTextField = null;
private JTextField __DataStoreDescription_JTextField = null;
private SimpleJComboBox __DataStoreType_JComboBox = null;
private SimpleJComboBox __DatabaseEngine_JComboBox = null;
private JTextField __ServerName_JTextField = null;
private JTextField __DatabaseName_JTextField = null;
private JTextField __Login_JTextField = null;
private JTextField __Password_JTextField = null;
private SimpleJComboBox	__IfFound_JComboBox =null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private OpenDataStore_Command __command = null;
private boolean __ok = false;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public OpenDataStore_JDialog ( JFrame parent, OpenDataStore_Command command )
{	super(parent, true);
	initialize ( parent, command );
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
		HelpViewer.getInstance().showHelp("command", "OpenDataStore");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited...
			response ( true );
		}
	}
	else {
		// Other event
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );

    String DataStoreName = __DataStoreName_JTextField.getText().trim();
    String DataStoreDescription = __DataStoreDescription_JTextField.getText().trim();
    String DataStoreType = __DataStoreType_JComboBox.getSelected();
    String DatabaseEngine = __DatabaseEngine_JComboBox.getSelected();
    String ServerName = __ServerName_JTextField.getText().trim();
    String DatabaseName = __DatabaseName_JTextField.getText().trim();
    String Login = __Login_JTextField.getText().trim();
    String Password = __Password_JTextField.getText().trim();
	String IfFound = __IfFound_JComboBox.getSelected();
	__error_wait = false;

    if ( DataStoreName.length() > 0 ) {
        props.set ( "DataStoreName", DataStoreName );
    }
    if ( DataStoreDescription.length() > 0 ) {
        props.set ( "DataStoreDescription", DataStoreDescription );
    }
    if ( (DataStoreType != null) && DataStoreType.length() > 0 ) {
        props.set ( "DataStoreType", DataStoreType );
    }
    if ( (DatabaseEngine != null) && DatabaseEngine.length() > 0 ) {
        props.set ( "DatabaseEngine", DatabaseEngine );
    }
    if ( (ServerName != null) && ServerName.length() > 0 ) {
        props.set ( "ServerName", ServerName );
    }
    if ( (DatabaseName != null) && DatabaseName.length() > 0 ) {
        props.set ( "DatabaseName", DatabaseName );
    }
    if ( (Login != null) && Login.length() > 0 ) {
        props.set ( "Login", Login );
    }
    if ( (Password != null) && Password.length() > 0 ) {
        props.set ( "Password", Password );
    }
	if ( IfFound.length() > 0 ) {
		props.set ( "IfFound", IfFound );
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
{	String DataStoreName = __DataStoreName_JTextField.getText().trim();
    String DataStoreDescription = __DataStoreDescription_JTextField.getText().trim();
    String DataStoreType = __DataStoreType_JComboBox.getSelected();
    String DatabaseEngine = __DatabaseEngine_JComboBox.getSelected();
    String ServerName = __ServerName_JTextField.getText().trim();
    String DatabaseName = __DatabaseName_JTextField.getText().trim();
    String Login = __Login_JTextField.getText().trim();
    String Password = __Password_JTextField.getText().trim();
	String IfFound = __IfFound_JComboBox.getSelected();
    __command.setCommandParameter ( "DataStoreName", DataStoreName );
    __command.setCommandParameter ( "DataStoreDescription", DataStoreDescription );
    __command.setCommandParameter ( "DataStoreType", DataStoreType );
    __command.setCommandParameter ( "DatabaseEngine", DatabaseEngine );
    __command.setCommandParameter ( "ServerName", ServerName );
    __command.setCommandParameter ( "DatabaseName", DatabaseName );
    __command.setCommandParameter ( "Login", Login );
    __command.setCommandParameter ( "Password", Password );
	__command.setCommandParameter ( "IfFound", IfFound );
}

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
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, OpenDataStore_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

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
        "This command opens a database datastore connection."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "This command is used to manage datastores that are dynamically created, such as SQLite in-memory and file databases."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Datastore metadata are available for command editing but editing functionality may be limited."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Configuration information can be provided via command parameters or by reading a datastore configuration file."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "The database connection will be active until modified further when other commands are run."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "See the View / Datastores menu for a list of active datastores."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for datastore configuration specified with properties.
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Properties", prop_JPanel );

    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "Specify the datastore configuration using properties."),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(prop_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yProp, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Datastore name:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreName_JTextField = new JTextField (10);
    __DataStoreName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, __DataStoreName_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Required - datastore name."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Datastore description:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataStoreDescription_JTextField = new JTextField (10);
    __DataStoreDescription_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, __DataStoreDescription_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Optional - datastore description (default=datastore name)."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

   JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Datastore type:"),
		0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataStoreType_JComboBox = new SimpleJComboBox ( false );
	List<String> typeChoices = new ArrayList<>();
	// TODO smalers 2020-10-18 need to figure out how to get all types
	typeChoices.add ( "" );	// Default
	typeChoices.add ( "GenericDatabaseDataStore" );
	__DataStoreType_JComboBox.setData(typeChoices);
	__DataStoreType_JComboBox.select ( 0 );
	__DataStoreType_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(prop_JPanel, __DataStoreType_JComboBox,
		1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
		"Optional - datastore type (default=GenericDatabaseDataStore)."), 
		3, yProp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

   JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Database engine:"),
		0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DatabaseEngine_JComboBox = new SimpleJComboBox ( false );
	List<String> dbChoices = new ArrayList<>();
	// TODO smalers 2020-10-18 these should be provided by an enumeration or other code
	dbChoices.add ( "" );	// Default
	dbChoices.add ( "Access" );
	dbChoices.add ( "Derby" );
	// H2
	// Informix
	dbChoices.add ( "MySQL" );
	dbChoices.add ( "Oracle" );
	dbChoices.add ( "PostgreSQL" );
	dbChoices.add ( "SQLite" );
	dbChoices.add ( "SQLServer" );
	__DatabaseEngine_JComboBox.setData(dbChoices);
	__DatabaseEngine_JComboBox.select ( 0 );
	__DatabaseEngine_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(prop_JPanel, __DatabaseEngine_JComboBox,
		1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
		"Optional - database engine (default=SQLite)."), 
		3, yProp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Server name:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ServerName_JTextField = new JTextField (10);
    __ServerName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, __ServerName_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Required - database server name."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Database name:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DatabaseName_JTextField = new JTextField (10);
    __DatabaseName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, __DatabaseName_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Usually required - database name."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Login:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Login_JTextField = new JTextField (10);
    __Login_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, __Login_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Usually required - database login."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Password:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Password_JTextField = new JTextField (10);
    __Password_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, __Password_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Usually required - database password."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

   JGUIUtil.addComponent(prop_JPanel, new JLabel ( "If found?:"),
		0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<String>();
	notFoundChoices.add ( "" );	// Default
	notFoundChoices.add ( __command._Close );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfFound_JComboBox.setData(notFoundChoices);
	__IfFound_JComboBox.select ( 0 );
	__IfFound_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(prop_JPanel, __IfFound_JComboBox,
		1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
		"Optional - action if datastore is found (default=" + __command._Close + ")."), 
		3, yProp, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for configuration file
    int yConfig = -1;
    JPanel config_JPanel = new JPanel();
    config_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Configuration File", config_JPanel );

    JGUIUtil.addComponent(config_JPanel, new JLabel (
        "Functionality to specify a datastore configuration file similar to startup configuration will be added in the future."),
        0, ++yConfig, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(config_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String DataStoreName = "";
    String DataStoreDescription = "";
    String DataStoreType = "";
    String DatabaseEngine = "";
    String ServerName = "";
    String DatabaseName = "";
    String Login = "";
    String Password = "";
	String IfFound = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
		DataStoreName = props.getValue ( "DataStoreName" );
    	DataStoreDescription = props.getValue ( "DataStoreDescription" );
    	DataStoreType = props.getValue ( "DataStoreType" );
    	DatabaseEngine = props.getValue ( "DatabaseEngine" );
    	ServerName = props.getValue ( "ServerName" );
    	DatabaseName = props.getValue ( "DatabaseName" );
    	Login = props.getValue ( "Login" );
    	Password = props.getValue ( "Password" );
		IfFound = props.getValue ( "IfFound" );
        if ( DataStoreName != null ) {
            __DataStoreName_JTextField.setText ( DataStoreName );
        }
        if ( DataStoreDescription != null ) {
            __DataStoreDescription_JTextField.setText ( DataStoreDescription );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__DataStoreType_JComboBox, DataStoreType, JGUIUtil.NONE, null, null ) ) {
            __DataStoreType_JComboBox.select ( null ); // To ensure that following causes an event
            __DataStoreType_JComboBox.select ( DataStoreType ); // This will trigger getting the DMI for use in the editor
        }
        else {
            if ( (DataStoreType == null) || DataStoreType.equals("") ) {
                // New command...select the default...
                __DataStoreType_JComboBox.select ( null ); // To ensure that following causes an event
                if ( __DataStoreType_JComboBox.getItemCount() > 0 ) {
                	__DataStoreType_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DataStoreType parameter \"" + DataStoreType + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__DatabaseEngine_JComboBox, DatabaseEngine, JGUIUtil.NONE, null, null ) ) {
            __DatabaseEngine_JComboBox.select ( null ); // To ensure that following causes an event
            __DatabaseEngine_JComboBox.select ( DatabaseEngine );
        }
        else {
            if ( (DatabaseEngine == null) || DatabaseEngine.equals("") ) {
                // New command...select the default...
                __DatabaseEngine_JComboBox.select ( null ); // To ensure that following causes an event
                if ( __DatabaseEngine_JComboBox.getItemCount() > 0 ) {
                	__DatabaseEngine_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                  "DatabaseEngine parameter \"" + DatabaseEngine + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if ( ServerName != null ) {
            __ServerName_JTextField.setText ( ServerName );
        }
        if ( DatabaseName != null ) {
            __DatabaseName_JTextField.setText ( DatabaseName );
        }
        if ( Login != null ) {
            __Login_JTextField.setText ( Login );
        }
        if ( Password != null ) {
            __Password_JTextField.setText ( Password );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfFound_JComboBox, IfFound,JGUIUtil.NONE, null, null ) ) {
			__IfFound_JComboBox.select ( IfFound );
		}
		else {
            if ( (IfFound == null) || IfFound.equals("") ) {
				// New command...select the default...
				__IfFound_JComboBox.select ( 0 );
			}
			else {
				// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfFound parameter \"" +	IfFound +
				"\".  Select a\n value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields...
    DataStoreName = __DataStoreName_JTextField.getText().trim();
    DataStoreDescription = __DataStoreDescription_JTextField.getText().trim();
    DataStoreType = __DataStoreType_JComboBox.getSelected();
    DatabaseEngine = __DatabaseEngine_JComboBox.getSelected();
    ServerName = __ServerName_JTextField.getText().trim();
    DatabaseName = __DatabaseName_JTextField.getText().trim();
    Login = __Login_JTextField.getText().trim();
    Password = __Password_JTextField.getText().trim();
	IfFound = __IfFound_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "DataStoreName=" + DataStoreName );
	props.add ( "DataStoreDescription=" + DataStoreDescription );
    props.add ( "DataStoreType=" + DataStoreType );
    props.add ( "DatabaseEngine=" + DatabaseEngine );
    props.add ( "ServerName=" + ServerName );
    props.add ( "DatabaseName=" + DatabaseName );
    props.add ( "Login=" + Login );
    props.add ( "Password=" + Password );
	props.add ( "IfFound=" + IfFound );
	__command_JTextArea.setText( __command.toString ( props ) );
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
