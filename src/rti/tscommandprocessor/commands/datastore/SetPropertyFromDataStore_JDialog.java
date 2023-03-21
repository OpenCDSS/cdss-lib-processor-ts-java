// SetPropertyFromDataStore_JDialog - editor for the SetPropertyFromDataStore command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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
import java.util.List;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import riverside.datastore.DataStore;
import riverside.datastore.DataStoreSubstitute;
import rti.tscommandprocessor.core.TSCommandProcessor;

/**
Editor for the SetPropertyFromDataStore command.
*/
@SuppressWarnings("serial")
public class SetPropertyFromDataStore_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener {
	private SimpleJButton __cancel_JButton = null;
	private SimpleJButton __ok_JButton = null;
	private SimpleJButton __help_JButton = null;
	private SetPropertyFromDataStore_Command __command = null;
	private JTextArea __command_JTextArea=null;
	private SimpleJComboBox __DataStore_JComboBox = null;
	private JTextField  __DataStoreProperty_JTextField = null;
	private JTextField  __PropertyName_JTextField = null;
	//private SimpleJComboBox __PropertyType_JComboBox = null;
	//private JTextField __PropertyValue_JTextField = null;
	private SimpleJComboBox __AllowNull_JComboBox = null;
	private boolean __error_wait = false;	// Is there an error to be cleared up or Cancel?
	private boolean __first_time = true;
	private boolean __ok = false;		// Indicates whether OK button has been pressed.

	private List<DataStore> datastores = new ArrayList<>();

	/**
	Command dialog constructor.
	@param parent JFrame class instantiating this class.
	@param command Command to edit.
	@param datastores list of database datastores
	*/
	public SetPropertyFromDataStore_JDialog ( JFrame parent, SetPropertyFromDataStore_Command command, List<DataStore> datastores ) {
		super(parent, true);
		initialize ( parent, command, datastores );
	}

	/**
	Responds to ActionEvents.
	@param event ActionEvent object
	*/
	public void actionPerformed( ActionEvent event ) {
		Object o = event.getSource();

		if ( o == __cancel_JButton ) {
			response ( false );
		}
		else if ( o == __help_JButton ) {
			HelpViewer.getInstance().showHelp("command", "SetPropertyFromDatastore");
		}
		else if ( o == __ok_JButton ) {
			refresh ();
			checkInput();
			if ( !__error_wait ) {
				response ( true );
			}
		}
	}

	/**
	Check the GUI state to make sure that appropriate components are enabled/disabled.
	*/
	private void checkGUIState () {
	}

	/**
	Check the input.
	If errors exist, warn the user and set the __error_wait flag to true.
	This should be called before response() is allowed to complete.
	*/
	private void checkInput () {
		// Put together a list of parameters to check.
		PropList parameters = new PropList ( "" );
		String DataStore = __DataStore_JComboBox.getSelected();
		String DataStoreProperty = __DataStoreProperty_JTextField.getText().trim();
		String PropertyName = __PropertyName_JTextField.getText().trim();
    	//String PropertyType = __PropertyType_JComboBox.getSelected();
		//String PropertyValue = __PropertyValue_JTextField.getText().trim();
		String AllowNull = __AllowNull_JComboBox.getSelected();

		__error_wait = false;

		if ( (DataStore != null) && !DataStore.isEmpty() ) {
			parameters.set ( "DataStore", DataStore );
		}
		else {
			parameters.set ( "DataStore", "" );
		}
		if ( DataStoreProperty.length() > 0 ) {
	    	parameters.set ( "DataStoreProperty", DataStoreProperty );
		}
		if ( PropertyName.length() > 0 ) {
	    	parameters.set ( "PropertyName", PropertyName );
		}
		/*
    	if ( PropertyType.length() > 0 ) {
        	parameters.set ( "PropertyType", PropertyType );
    	}
		if ( PropertyValue.length() > 0 ) {
			parameters.set ( "PropertyValue", PropertyValue );
		}
		*/
		if ( AllowNull.length() > 0 ) {
			parameters.set ( "AllowNull", AllowNull );
		}

		try {	// This will warn the user...
			__command.checkCommandParameters ( parameters, null, 1 );
		}
		catch ( Exception e ) {
			// The warning would have been printed in the check code.
			__error_wait = true;
		}
	}

	/**
	Commit the edits to the command.
	In this case the command parameters have already been checked and no errors were detected.
	*/
	private void commitEdits () {
		String DataStore = __DataStore_JComboBox.getSelected();
		String DataStoreProperty = __DataStoreProperty_JTextField.getText().trim();
		String PropertyName = __PropertyName_JTextField.getText().trim();
    	//String PropertyType = __PropertyType_JComboBox.getSelected();
		//String PropertyValue = __PropertyValue_JTextField.getText().trim();
		String AllowNull = __AllowNull_JComboBox.getSelected();
		__command.setCommandParameter ( "DataStore", DataStore);
		__command.setCommandParameter ( "DataStoreProperty", DataStoreProperty );
		__command.setCommandParameter ( "PropertyName", PropertyName );
    	//__command.setCommandParameter ( "PropertyType", PropertyType );
		//__command.setCommandParameter ( "PropertyValue", PropertyValue );
		__command.setCommandParameter ( "AllowNull", AllowNull );
	}

	/**
	Instantiates the UI components.
	@param parent JFrame class instantiating this class.
	@param title Dialog title.
	@param command The command to edit.
	@param datastores list of database datastores
	*/
	private void initialize ( JFrame parent, SetPropertyFromDataStore_Command command, List<DataStore> datastores ) {
		__command = command;
		this.datastores = datastores;

		TSCommandProcessor processor = (TSCommandProcessor)__command.getCommandProcessor();

		addWindowListener( this );

    	Insets insetsTLBR = new Insets(2,2,2,2);

		JPanel main_JPanel = new JPanel();
		main_JPanel.setLayout( new GridBagLayout() );
		getContentPane().add ( "North", main_JPanel );
		int y = 0;

    	JGUIUtil.addComponent(main_JPanel, new JLabel (
			"Set a processor property from a datastore configuration file property." ),
			0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"The property can be referenced in parameters using ${Property} notation." ),
        	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"This allows dynamic data to be handled without hard-coding in workflows."),
        	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"For example, use a datastore property for web service API token in a WebGet command request." ),
        	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
        	"Currently, all datastore configuration properties are strings."),
        	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
			0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    	// List all datastores:
    	// - don't need to worry about whether of a specific type since configuration properties are general
    
    	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore:"),
        	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    	__DataStore_JComboBox = new SimpleJComboBox ( false );
    	// Copy the list of datastore names to an internal list.
    	List<String> datastoreChoices = new ArrayList<>();
    	for ( DataStore dataStore : this.datastores ) {
    		datastoreChoices.add(dataStore.getName());
    	}
    	// Also list any substitute datastore names so the original or substitute can be used.
    	List<DataStoreSubstitute> datastoreSubstituteList = processor.getDataStoreSubstituteList();
    	for ( DataStoreSubstitute dssub : datastoreSubstituteList ) {
    		boolean found = false;
    		for ( String choice : datastoreChoices ) {
    			if ( choice.equals(dssub.getDatastoreNameToUse()) ) {
    				// The substitute original name matches a datastore name so also add the alias.
    				found = true;
    				break;
    			}
    		}
    		if ( found ) {
    			datastoreChoices.add(dssub.getDatastoreNameInCommands());
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
    	JGUIUtil.addComponent(main_JPanel, new JLabel("Required - datastore containing properities."), 
        	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Datastore property name:" ),
        	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    	__DataStoreProperty_JTextField = new JTextField ( 20 );
    	__DataStoreProperty_JTextField.setToolTipText("Datastore property name to use.");
    	__DataStoreProperty_JTextField.addKeyListener ( this );
    	JGUIUtil.addComponent(main_JPanel, __DataStoreProperty_JTextField,
        	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel(
        	"Do not use spaces $, { or } in name."),
        	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Property name:" ),
        	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    	__PropertyName_JTextField = new JTextField ( 20 );
    	__PropertyName_JTextField.setToolTipText("Property name to set.");
    	__PropertyName_JTextField.addKeyListener ( this );
    	JGUIUtil.addComponent(main_JPanel, __PropertyName_JTextField,
        	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel(
        	"Do not use spaces $, { or } in name."),
        	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    	/*
    	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Property type:" ),
        	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    	__PropertyType_JComboBox = new SimpleJComboBox ( false );
    	List<String> typeChoices = new ArrayList<String>();
    	typeChoices.add ( __command._DateTime );
    	typeChoices.add ( __command._Double );
    	typeChoices.add ( __command._Integer );
    	typeChoices.add ( __command._String );
    	__PropertyType_JComboBox.setData(typeChoices);
    	__PropertyType_JComboBox.select ( __command._String );
    	__PropertyType_JComboBox.addItemListener ( this );
        	JGUIUtil.addComponent(main_JPanel, __PropertyType_JComboBox,
        	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel( "Used to check property value."),
        	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    	JGUIUtil.addComponent(main_JPanel, new JLabel ( "NWSRFS App Default:" ),
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__PropertyValue_JTextField = new JTextField ( 20 );
		__PropertyValue_JTextField.addKeyListener ( this );
    	JGUIUtil.addComponent(main_JPanel, __PropertyValue_JTextField,
			1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel( ""),
			3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
			*/

    	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Allow Null?:" ),
        	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    	__AllowNull_JComboBox = new SimpleJComboBox ( false );
    	List<String> allowNullChoices = new ArrayList<>();
    	allowNullChoices.add("");
    	allowNullChoices.add(__command._False);
    	allowNullChoices.add(__command._True);
    	__AllowNull_JComboBox.setData ( allowNullChoices );
    	__AllowNull_JComboBox.addItemListener ( this );
    	JGUIUtil.addComponent(main_JPanel, __AllowNull_JComboBox,
        	1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - allow null property value (default=" + __command._False + ")?"),
        	3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);


    	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__command_JTextArea = new JTextArea ( 4, 55 );
		__command_JTextArea.setLineWrap ( true );
		__command_JTextArea.setWrapStyleWord ( true );
		__command_JTextArea.setEditable ( false );
		JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
			1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

		// Refresh the contents.
    	checkGUIState();
		refresh ();

		// South Panel: North
		JPanel button_JPanel = new JPanel();
		button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        	JGUIUtil.addComponent(main_JPanel, button_JPanel,
			0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

		button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
		button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

		button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
		__help_JButton.setToolTipText("Show command documentation in web browser");

		setTitle ( "Edit " + __command.getCommandName() + "() Command" );
		setResizable ( true );
    	pack();
    	JGUIUtil.center( this );
    	super.setVisible( true );
	}

	/**
	Handle ItemEvent events.
	@param e ItemEvent to handle.
	*/
	public void itemStateChanged ( ItemEvent e ) {
		checkGUIState();
    	refresh();
	}

	/**
	Respond to KeyEvents.
	*/
	public void keyPressed ( KeyEvent event ) {
		int code = event.getKeyCode();

		if ( code == KeyEvent.VK_ENTER ) {
			refresh ();
			checkInput();
			if ( !__error_wait ) {
				response ( true );
			}
		}
		else {
			// Combo box events.
			refresh();
		}
	}

	public void keyReleased ( KeyEvent event ) {
		refresh();
	}

	public void keyTyped ( KeyEvent event ) {
	}

	/**
	Indicate if the user pressed OK (cancel otherwise).
	@return true if the edits were committed, false if the user cancelled.
	*/
	public boolean ok () {
		return __ok;
	}

	/**
	Refresh the command from the other text field contents.
	*/
	private void refresh () {
		String routine = getClass().getSimpleName() + ".refresh";
		String DataStore = "";
    	String DataStoreProperty = "";
    	String PropertyName = "";
    	//String PropertyType = "";
		//String PropertyValue = "";
    	String AllowNull = "";
		PropList props = __command.getCommandParameters();
		if ( __first_time ) {
			__first_time = false;
			// Get the parameters from the command.
			DataStore = props.getValue ( "DataStore" );
			DataStoreProperty = props.getValue ( "DataStoreProperty" );
			PropertyName = props.getValue ( "PropertyName" );
        	//PropertyType = props.getValue ( "PropertyType" );
			//PropertyValue = props.getValue ( "PropertyValue" );
			AllowNull = props.getValue ( "AllowNull" );

			// The datastore list is set up in initialize() but is selected here.
			if ( JGUIUtil.isSimpleJComboBoxItem(__DataStore_JComboBox, DataStore, JGUIUtil.NONE, null, null ) ) {
				__DataStore_JComboBox.select ( null ); // To ensure that following causes an event.
				__DataStore_JComboBox.select ( DataStore ); // This will trigger getting the DMI for use in the editor.
			}
			else {
				if ( (DataStore == null) || DataStore.equals("") ) {
					// New command...select the default.
					__DataStore_JComboBox.select ( null ); // To ensure that following causes an event.
					__DataStore_JComboBox.select ( 0 );
				}
				else {
					// Bad user command.
					Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
						"DataStore parameter \"" + DataStore + "\".  Select a\ndifferent value or Cancel." );
				}
			}
	    	if ( DataStoreProperty != null ) {
	         	__DataStoreProperty_JTextField.setText ( DataStoreProperty );
	    	}
	    	if ( PropertyName != null ) {
	         	__PropertyName_JTextField.setText ( PropertyName );
	    	}
	    	/*
        	if ( PropertyType == null ) {
            	// Select default...
            	__PropertyType_JComboBox.select ( 0 );
        	}
        	else {
            	if ( JGUIUtil.isSimpleJComboBoxItem( __PropertyType_JComboBox,PropertyType, JGUIUtil.NONE, null, null ) ) {
                	__PropertyType_JComboBox.select ( PropertyType );
            	}
            	else {
                	Message.printWarning ( 1, routine,
                	"Existing command references an invalid\nPropertyType value \"" + PropertyType +
                	"\".  Select a different value or Cancel.");
                	__error_wait = true;
            	}
        	}
			if ( PropertyValue != null ) {
		    	__PropertyValue_JTextField.setText ( PropertyValue );
			}
			*/
	    	if ( AllowNull == null ) {
	    		// Select default.
	    		__AllowNull_JComboBox.select ( 0 );
	    	}
	    	else {
	    		if ( JGUIUtil.isSimpleJComboBoxItem( __AllowNull_JComboBox,AllowNull, JGUIUtil.NONE, null, null ) ) {
	    			__AllowNull_JComboBox.select ( AllowNull );
	    		}
	    		else {
	    			Message.printWarning ( 1, routine,
	    				"Existing command references an invalid\nAllowNull value \"" + AllowNull +
	    				"\".  Select a different value or Cancel.");
	    			__error_wait = true;
	    		}
	    	}
		}
		// Regardless, reset the command from the fields.
		DataStore = __DataStore_JComboBox.getSelected();
		if ( DataStore == null ) {
			DataStore = "";
		}
		DataStoreProperty = __DataStoreProperty_JTextField.getText().trim();
		PropertyName = __PropertyName_JTextField.getText().trim();
    	//PropertyType = __PropertyType_JComboBox.getSelected();
		//PropertyValue = __PropertyValue_JTextField.getText().trim();
		AllowNull = __AllowNull_JComboBox.getSelected();
		props = new PropList ( __command.getCommandName() );
		props.add ( "DataStore=" + DataStore);
		props.add ( "DataStoreProperty=" + DataStoreProperty );
		props.add ( "PropertyName=" + PropertyName );
    	//props.add ( "PropertyType=" + PropertyType );
		//props.add ( "PropertyValue=" + PropertyValue );
		props.add ( "AllowNull=" + AllowNull );
		__command_JTextArea.setText( __command.toString ( props ).trim() );
	}

	/**
	React to the user response.
	@param ok if false, then the edit is cancelled.
	If true, the edit is committed and the dialog is closed.
	*/
	private void response ( boolean ok ) {
		__ok = ok;	// Save to be returned by ok()
		if ( ok ) {
			// Commit the changes.
			commitEdits ();
			if ( __error_wait ) {
				// Not ready to close out.
				return;
			}
		}
		// Now close out.
		setVisible( false );
		dispose();
	}

	/**
	Responds to WindowEvents.
	@param event WindowEvent object
	*/
	public void windowClosing( WindowEvent event ) {
		response ( false );
	}

	public void windowActivated( WindowEvent evt ) {
	}

	public void windowClosed( WindowEvent evt ) {
	}

	public void windowDeactivated( WindowEvent evt ) {
	}

	public void windowDeiconified( WindowEvent evt ) {
	}

	public void windowIconified( WindowEvent evt ) {
	}

	public void windowOpened( WindowEvent evt ) {
	}

}