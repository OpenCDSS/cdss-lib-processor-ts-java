// SetTimeSeriesProperty_JDialog - Command editor dialog for the SetTimeSeriesProperty() command.

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

package rti.tscommandprocessor.commands.ts;

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

import javax.swing.BorderFactory;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.util.ArrayList;
import java.util.List;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the SetTimeSeriesProperty() command.
*/
@SuppressWarnings("serial")
public class SetTimeSeriesProperty_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, ItemListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetTimeSeriesProperty_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __Editable_JComboBox = null;
private JTabbedPane __props_JTabbedPane = null;
private TSFormatSpecifiersJPanel __Description_JTextField = null; // Allows expansion of % specifiers.
private JTextField __Units_JTextField = null;
private JTextField __Precision_JTextField = null;
private JTextField __MissingValue_JTextField = null;
private SimpleJComboBox __PropertyType_JComboBox = null;
private JTextField __PropertyValue_JTextField = null;
private JTextField __PropertyName_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Has user has pressed OK to close the dialog?

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SetTimeSeriesProperty_JDialog ( JFrame parent, SetTimeSeriesProperty_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "SetTimeSeriesProperty");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
    String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
        TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
        TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __TSID_JComboBox.setEnabled(true);
        __TSID_JLabel.setEnabled ( true );
    }
    else {
        __TSID_JComboBox.setEnabled(false);
        __TSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(TSList)) {
        __EnsembleID_JComboBox.setEnabled(true);
        __EnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __EnsembleID_JComboBox.setEnabled(false);
        __EnsembleID_JLabel.setEnabled ( false );
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList parameters = new PropList ( "" );
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Editable = __Editable_JComboBox.getSelected();
    String Description = __Description_JTextField.getText().trim();
    String Units = __Units_JTextField.getText().trim();
    String Precision = __Precision_JTextField.getText().trim();
    String MissingValue = __MissingValue_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyType = __PropertyType_JComboBox.getSelected();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();

	__error_wait = false;
	
	if ( TSList.length() > 0 ) {
		parameters.set ( "TSList", TSList );
	}
	if ( TSID.length() > 0 ) {
		parameters.set ( "TSID", TSID );
	}
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
	if ( Editable.length() > 0 ) {
		parameters.set ( "Editable", Editable );
	}
    if ( Description.length() > 0 ) {
        parameters.set ( "Description", Description );
    }
    if ( Units.length() > 0 ) {
        parameters.set ( "Units", Units );
    }
    if ( Precision.length() > 0 ) {
        parameters.set ( "Precision", Precision );
    }
    if ( MissingValue.length() > 0 ) {
        parameters.set ( "MissingValue", MissingValue );
    }
    if ( PropertyName.length() > 0 ) {
        parameters.set ( "PropertyName", PropertyName );
    }
    if ( PropertyType.length() > 0 ) {
        parameters.set ( "PropertyType", PropertyType );
    }
    if ( PropertyValue.length() > 0 ) {
        parameters.set ( "PropertyValue", PropertyValue );
    }
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( parameters, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		Message.printWarning(2,"",e);
		__error_wait = true;
	}
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Editable = __Editable_JComboBox.getSelected();
    String Description = __Description_JTextField.getText().trim();
    String Units = __Units_JTextField.getText().trim();
    String Precision = __Precision_JTextField.getText().trim();
    String MissingValue = __MissingValue_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyType = __PropertyType_JComboBox.getSelected();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "Editable", Editable );
    __command.setCommandParameter ( "Description", Description );
    __command.setCommandParameter ( "Units", Units );
    __command.setCommandParameter ( "Precision", Precision );
    __command.setCommandParameter ( "MissingValue", MissingValue );
    __command.setCommandParameter ( "PropertyName", PropertyName );
    __command.setCommandParameter ( "PropertyType", PropertyType );
    __command.setCommandParameter ( "PropertyValue", PropertyValue );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, SetTimeSeriesProperty_Command command )
{	__command = command;
	
    addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Set time series properties (metadata)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Time series identifier information cannot be changed because it is fundamental to locating time series during processing."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    __props_JTabbedPane = new JTabbedPane ();
    __props_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify time series properties" ));
    JGUIUtil.addComponent(main_JPanel, __props_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for built-in properties
    int yBuiltIn = -1;
    JPanel builtIn_JPanel = new JPanel();
    builtIn_JPanel.setLayout( new GridBagLayout() );
    __props_JTabbedPane.addTab ( "Built-in properties", builtIn_JPanel );

    JGUIUtil.addComponent(builtIn_JPanel, new JLabel ( "Built-in properties are core to the time series design." ), 
        0, ++yBuiltIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel (
        "Some built-in properties can be referenced later with % specifier notation (e.g., %D for description)." ), 
        0, ++yBuiltIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yBuiltIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel("Description:"),
        0, ++yBuiltIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Description_JTextField = new TSFormatSpecifiersJPanel(10);
    __Description_JTextField.getTextField().setToolTipText("Use %L for location, %T for data type, %I for interval, also ${ts:Property} and ${Property}.");
    __Description_JTextField.addKeyListener ( this );
    __Description_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(builtIn_JPanel, __Description_JTextField,
        1, yBuiltIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel,
        new JLabel ("Optional - use %L for location, etc."),
        3, yBuiltIn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel ("Data units:"),
        0, ++yBuiltIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Units_JTextField = new JTextField (10);
    __Units_JTextField.setToolTipText("Specify units or use ${Property} notation");
    __Units_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(builtIn_JPanel, __Units_JTextField,
        1, yBuiltIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel (
        "Optional - data units (does not change data values)."),
        3, yBuiltIn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(builtIn_JPanel, new JLabel("Precision of data:"),
		0, ++yBuiltIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Precision_JTextField = new JTextField ( "", 10 );
	__Precision_JTextField.setToolTipText("Number of digits after the decimal point for output.");
	__Precision_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(builtIn_JPanel, __Precision_JTextField,
		1, yBuiltIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel (
        "Optional - data precision for output (default=from units)."),
        3, yBuiltIn, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel ( "Missing value:" ),
        0, ++yBuiltIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField ( "", 20 );
    __MissingValue_JTextField.setToolTipText("Specify missing value as number, NaN or use ${Property} notation");
    __MissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(builtIn_JPanel, __MissingValue_JTextField,
        1, yBuiltIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel ( "Optional - missing data value (does not change data values)."),
        3, yBuiltIn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel ("Are data editable?:"),
        0, ++yBuiltIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    __Editable_JComboBox = new SimpleJComboBox ( true );
    // No blank (default) or wildcard is allowed.
    __Editable_JComboBox.add ( "" );
    __Editable_JComboBox.add ( __command._False );
    __Editable_JComboBox.add ( __command._True );
    __Editable_JComboBox.addItemListener ( this );
    __Editable_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(builtIn_JPanel, __Editable_JComboBox,
        1, yBuiltIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel (
        "Optional - for interactive edit tools (default=" + __command._False + ")."),
        3, yBuiltIn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for user-defined (not built-in) properties
    int yUser = -1;
    JPanel user_JPanel = new JPanel();
    user_JPanel.setLayout( new GridBagLayout() );
    __props_JTabbedPane.addTab ( "User-defined properties", user_JPanel );

    JGUIUtil.addComponent(user_JPanel, new JLabel ( "User-defined properties can be referenced later with ${ts:Property} notation." ), 
        0, ++yUser, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JLabel (
        "User-defined properties require that all three of the following parameters are specified." ), 
        0, ++yUser, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yUser, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(user_JPanel, new JLabel ( "Property name:" ), 
        0, ++yUser, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyName_JTextField = new JTextField ( 20 );
    __PropertyName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(user_JPanel, __PropertyName_JTextField,
        1, yUser, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JLabel(
        "Required - name of property (case-specific)."), 
        3, yUser, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(user_JPanel, new JLabel ( "Property type:" ), 
        0, ++yUser, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyType_JComboBox = new SimpleJComboBox ( false );
    List<String> propType = new ArrayList<String>();
    propType.add ( "" );
    propType.add( __command._DateTime );
    propType.add ( __command._Double );
    propType.add ( __command._Integer );
    propType.add ( __command._String );
    __PropertyType_JComboBox.setData(propType);
    __PropertyType_JComboBox.select ( __command._String );
    __PropertyType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(user_JPanel, __PropertyType_JComboBox,
        1, yUser, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JLabel(
        "Required - to ensure proper property object initialization."), 
        3, yUser, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(user_JPanel, new JLabel ( "Property value:" ), 
        0, ++yUser, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyValue_JTextField = new JTextField ( 20 );
    __PropertyValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(user_JPanel, __PropertyValue_JTextField,
        1, yUser, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JLabel( "Required - property value as string, can use % and ${ts:property}."), 
        3, yUser, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
    		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", "OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	refresh();
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e) {
    checkGUIState();
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{
}

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
{	String routine = "SetTimeSeriesProperty_JDialog.refresh";
	String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String Description = "";
    String Units = "";
    String Precision = "";
    String MissingValue = "";
    String Editable = "";
    String PropertyName = "";
    String PropertyType = "";
    String PropertyValue = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        Description = parameters.getValue ( "Description" );
        Units = parameters.getValue ( "Units" );
        Precision = parameters.getValue ( "Precision" );
        MissingValue = parameters.getValue("MissingValue");
        Editable = parameters.getValue ( "Editable" );
        PropertyName = parameters.getValue ( "PropertyName" );
        PropertyType = parameters.getValue ( "PropertyType" );
        PropertyValue = parameters.getValue ( "PropertyValue" );
		if ( TSList == null ) {
			// Select default...
			__TSList_JComboBox.select ( 0 );
		}
		else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
				__TSList_JComboBox.select ( TSList );
			}
			else {
                Message.printWarning ( 1, routine,
				"Existing command references an invalid\nTSList value \"" +	TSList +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
            __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the blank...
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default...
            __EnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox,EnsembleID, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID_JComboBox.select ( EnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Description != null ) {
            __Description_JTextField.setText(Description);
        }
        if ( Units != null ) {
            __Units_JTextField.setText(Units);
        }
        if ( Precision != null ) {
            __Precision_JTextField.setText(Precision);
        }
        if ( MissingValue != null ) {
            __MissingValue_JTextField.setText ( MissingValue );
        }
        if ( Editable == null ) {
            // Select default...
            if ( __Editable_JComboBox.getItemCount() > 0 ) {
                __Editable_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Editable_JComboBox,Editable, JGUIUtil.NONE, null, null ) ) {
                __Editable_JComboBox.select ( Editable );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid Editable value \"" + Editable +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( PropertyName != null ) {
            __PropertyName_JTextField.setText ( PropertyName );
            if ( !PropertyName.equals("") ) {
                __props_JTabbedPane.setSelectedIndex(1);
            }
        }
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
	}
	// Regardless, reset the command from the fields...
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    Description = __Description_JTextField.getText().trim();
    Units = __Units_JTextField.getText().trim();
    Precision = __Precision_JTextField.getText().trim();
    MissingValue = __MissingValue_JTextField.getText().trim();
    Editable = __Editable_JComboBox.getSelected();
    PropertyName = __PropertyName_JTextField.getText().trim();
    PropertyType = __PropertyType_JComboBox.getSelected();
    PropertyValue = __PropertyValue_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
	parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
    parameters.add ( "Description=" + Description );
    parameters.add ( "Units=" + Units );
    parameters.add ( "Precision=" + Precision );
    parameters.add ( "MissingValue=" + MissingValue );
    parameters.add ( "Editable=" + Editable );
    parameters.add ( "PropertyName=" + PropertyName );
    parameters.add ( "PropertyType=" + PropertyType );
    parameters.add ( "PropertyValue=" + PropertyValue );
	__command_JTextArea.setText( __command.toString ( parameters ) );
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
public void windowClosing( WindowEvent event )
{	response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}
