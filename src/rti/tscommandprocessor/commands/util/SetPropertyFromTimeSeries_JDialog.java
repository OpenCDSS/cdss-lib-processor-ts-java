// SetPropertyFromTimeSeries_JDialog - Editor for SetPropertyFromTimeSeries() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

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
Editor for SetPropertyFromTimeSeries() command.
*/
@SuppressWarnings("serial")
public class SetPropertyFromTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private JTabbedPane __main_JTabbedPane = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetPropertyFromTimeSeries_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;

private JTextField __PropertyName_JTextField = null;
private TSFormatSpecifiersJPanel __PropertyValue_JTextField = null;

private JTextField __DateTime_JTextField = null;
private JTextField __PropertyNameForValue_JTextField = null;
private JTextField __PropertyNameForFlag_JTextField = null;

private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SetPropertyFromTimeSeries_JDialog ( JFrame parent, SetPropertyFromTimeSeries_Command command ) {
	super(parent, true);
	initialize ( parent, command );
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
		HelpViewer.getInstance().showHelp("command", "SetPropertyFromTimeSeries");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

//Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e ) {
    checkGUIState();
    refresh();
}

//...End event handlers for DocumentListener

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState () {
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
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();
    String DateTime = __DateTime_JTextField.getText().trim();
    String PropertyNameForValue = __PropertyNameForValue_JTextField.getText().trim();
    String PropertyNameForFlag = __PropertyNameForFlag_JTextField.getText().trim();
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
	if ( PropertyName.length() > 0 ) {
	    parameters.set ( "PropertyName", PropertyName );
	}
	if ( PropertyValue.length() > 0 ) {
	    parameters.set ( "PropertyValue", PropertyValue );
	}
	if ( DateTime.length() > 0 ) {
	    parameters.set ( "DateTime", DateTime );
	}
	if ( PropertyNameForValue.length() > 0 ) {
	    parameters.set ( "PropertyNameForValue", PropertyNameForValue );
	}
	if ( PropertyNameForFlag.length() > 0 ) {
	    parameters.set ( "PropertyNameForFlag", PropertyNameForFlag );
	}
	try {
	    // This will warn the user.
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
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();
    String DateTime = __DateTime_JTextField.getText().trim();
    String PropertyNameForValue = __PropertyNameForValue_JTextField.getText().trim();
    String PropertyNameForFlag = __PropertyNameForFlag_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "PropertyName", PropertyName );
    __command.setCommandParameter ( "PropertyValue", PropertyValue );
    __command.setCommandParameter ( "DateTime", DateTime );
    __command.setCommandParameter ( "PropertyNameForValue", PropertyNameForValue );
    __command.setCommandParameter ( "PropertyNameForFlag", PropertyNameForFlag );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
*/
private void initialize ( JFrame parent, SetPropertyFromTimeSeries_Command command ) {
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Set a processor property from a time series property and/or data value." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"One processor property can be set for each \"TS list\" time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command is useful when setting a processor property in a For() loop."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );

    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for property.
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Property", prop_JPanel );

    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"The property name can be set using ${Property}, % specifiere, and ${ts:Property} syntax to use property names specific to a time series."),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"The property value can include a combination of:"),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"    - literal text"),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"    - processor properties using ${Property} syntax"),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"    - built-in time series properties using % syntax"),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
		"    - ${ts:Property} syntax for dynamic time series properties set by previous commands"),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Property name:" ),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyName_JTextField = new JTextField ( 20 );
    __PropertyName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __PropertyName_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel(
        "Required - do not use spaces $, { or } in name."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel("Property value:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyValue_JTextField = new TSFormatSpecifiersJPanel(10);
    __PropertyValue_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval, ${ts:Property}, ${Property}.");
    __PropertyValue_JTextField.addKeyListener ( this );
    __PropertyValue_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(prop_JPanel, __PropertyValue_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, yProp, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for data.
    int yData = -1;
    JPanel data_JPanel = new JPanel();
    data_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Data", data_JPanel );

    JGUIUtil.addComponent(data_JPanel, new JLabel (
		"Properties can be set based on the time series data value and flag for a date/time."),
		0, ++yData, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
		"The property name can be set using ${Property}, % specifiere, and ${ts:Property} syntax to use property names specific to a time series."),
		0, ++yData, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yData, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Date/time:" ),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTime_JTextField = new JTextField ( 30 );
    __DateTime_JTextField.setToolTipText("Date/time for the data, can use ${Property}");
    __DateTime_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(data_JPanel, __DateTime_JTextField,
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel(
        "Required - date/time for the data."),
        3, yData, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Property name for value:" ),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyNameForValue_JTextField = new JTextField ( 50 );
    __PropertyNameForValue_JTextField.setToolTipText("Property name for data value, can use ${Property}");
    __PropertyNameForValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(data_JPanel, __PropertyNameForValue_JTextField,
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel(
        "Optional - can use ${Property}."),
        3, yData, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Property name for flag:" ),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyNameForFlag_JTextField = new JTextField ( 50 );
    __PropertyNameForFlag_JTextField.setToolTipText("Property name for data flag, can use ${Property}");
    __PropertyNameForFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(data_JPanel, __PropertyNameForFlag_JTextField,
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel(
        "Optional - can use ${Property}."),
        3, yData, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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

    button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	setResizable ( false );
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
	    // Combo box.
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
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String PropertyName = "";
    String PropertyValue = "";
    String DateTime = "";
    String PropertyNameForValue = "";
    String PropertyNameForFlag = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
        TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        PropertyName = props.getValue ( "PropertyName" );
        PropertyValue = props.getValue ( "PropertyValue" );
        DateTime = props.getValue ( "DateTime" );
        PropertyNameForValue = props.getValue ( "PropertyNameForValue" );
        PropertyNameForFlag = props.getValue ( "PropertyNameForFlag" );
        if ( TSList == null ) {
            // Select default.
            __TSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSList_JComboBox,TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
			__TSID_JComboBox.select ( TSID );
		}
		else {
		    // Automatically add to the list after the blank.
			if ( (TSID != null) && (TSID.length() > 0) ) {
				__TSID_JComboBox.insertItemAt ( TSID, 1 );
				// Select.
				__TSID_JComboBox.select ( TSID );
			}
			else {
			    // Select the blank.
				__TSID_JComboBox.select ( 0 );
			}
		}
        if ( EnsembleID == null ) {
            // Select default.
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
	    if ( PropertyName != null ) {
	         __PropertyName_JTextField.setText ( PropertyName );
            // Also select tab.
		    __main_JTabbedPane.setSelectedIndex(0);
	    }
	    if ( PropertyValue != null ) {
	         __PropertyValue_JTextField.setText ( PropertyValue );
	    }
	    if ( DateTime != null ) {
	         __DateTime_JTextField.setText ( DateTime );
	    }
	    if ( (PropertyNameForValue != null) && !PropertyNameForValue.isEmpty() ) {
	         __PropertyNameForValue_JTextField.setText ( PropertyNameForValue );
            // Also select tab.
		    __main_JTabbedPane.setSelectedIndex(1);
	    }
	    if ( (PropertyNameForFlag != null) && !PropertyNameForFlag.isEmpty() ) {
	         __PropertyNameForFlag_JTextField.setText ( PropertyNameForFlag );
            // Also select tab.
		    __main_JTabbedPane.setSelectedIndex(1);
	    }
	}
	// Regardless, reset the command from the fields.
    TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    PropertyName = __PropertyName_JTextField.getText().trim();
    PropertyValue = __PropertyValue_JTextField.getText().trim();
    DateTime = __DateTime_JTextField.getText().trim();
    PropertyNameForValue = __PropertyNameForValue_JTextField.getText().trim();
    PropertyNameForFlag = __PropertyNameForFlag_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
	props.add ( "PropertyName=" + PropertyName );
	props.add ( "PropertyValue=" + PropertyValue );
	props.add ( "DateTime=" + DateTime );
	props.add ( "PropertyNameForValue=" + PropertyNameForValue );
	props.add ( "PropertyNameForFlag=" + PropertyNameForFlag );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok ) {
	__ok = ok;	// Save to be returned by ok().
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

public void windowOpened( WindowEvent evt ) {;}

}