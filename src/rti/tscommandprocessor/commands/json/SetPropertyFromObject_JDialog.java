// SetPropertyFromObject_JDialog - editor dialog for SetPropertyFromObject command

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

package rti.tscommandprocessor.commands.json;

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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class SetPropertyFromObject_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetPropertyFromObject_Command __command = null;
private SimpleJComboBox __ObjectID_JComboBox = null;
private JTextField __ObjectProperty_JTextField = null;
private JTextField __PropertyName_JTextField = null;
private SimpleJComboBox __PropertyType_JComboBox = null;
private SimpleJComboBox __AllowNull_JComboBox = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.
//private JFrame __parent = null;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param objectIDChoices choices for ObjectID value.
*/
public SetPropertyFromObject_JDialog ( JFrame parent, SetPropertyFromObject_Command command, List<String> objectIDChoices ) {
	super(parent, true);
	initialize ( parent, command, objectIDChoices );
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
		HelpViewer.getInstance().showHelp("command", "SetPropertyFromObject");
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

// ...End event handlers for DocumentListener

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState () {
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
    String ObjectID = __ObjectID_JComboBox.getSelected();
    String ObjectProperty = __ObjectProperty_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyType = __PropertyType_JComboBox.getSelected();
    String AllowNull = __AllowNull_JComboBox.getSelected();
	PropList parameters = new PropList ( "" );

	__error_wait = false;

    if ( ObjectID.length() > 0 ) {
        parameters.set ( "ObjectID", ObjectID );
    }
    if ( ObjectProperty.length() > 0 ) {
        parameters.set ( "ObjectProperty", ObjectProperty );
    }
    if ( PropertyName.length() > 0 ) {
        parameters.set ( "PropertyName", PropertyName );
    }
    if ( PropertyType.length() > 0 ) {
        parameters.set ( "PropertyType", PropertyType );
    }
    if ( AllowNull.length() > 0 ) {
        parameters.set ( "AllowNull", AllowNull );
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
    String ObjectID = __ObjectID_JComboBox.getSelected();
    String ObjectProperty = __ObjectProperty_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyType = __PropertyType_JComboBox.getSelected();
    String AllowNull = __AllowNull_JComboBox.getSelected();
    __command.setCommandParameter ( "ObjectID", ObjectID );
    __command.setCommandParameter ( "ObjectProperty", ObjectProperty );
    __command.setCommandParameter ( "PropertyName", PropertyName );
    __command.setCommandParameter ( "PropertyType", PropertyType );
    __command.setCommandParameter ( "AllowNull", AllowNull );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
@param objectIDChoices list of choices for object identifiers
*/
private void initialize ( JFrame parent, SetPropertyFromObject_Command command,
	List<String> objectIDChoices ) {
	__command = command;
    //__parent = parent;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"<html><b>This command is under development.</b></html>"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Set a processor object from an object's property value." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The object's property name is specified using levels separated by periods, for example:" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    level1.level2.level3.name" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Currently, the object property must be a single property, not an object in an array." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
       "The property type defaults from the object property (e.g., integer retained as integer, string as string)." ),
       0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
       "Or, set the type to convert from the object property type."),
       0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Object ID:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ObjectID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    objectIDChoices.add(0,""); // Add blank to ignore object.
    __ObjectID_JComboBox.setData ( objectIDChoices );
    __ObjectID_JComboBox.addItemListener ( this );
    //__ObjectID_JComboBox.setMaximumRowCount(objectIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __ObjectID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - object process."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Object Property:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ObjectProperty_JTextField = new JTextField ( 20 );
    __ObjectProperty_JTextField.setToolTipText("Property to set using period-delimited name.");
    __ObjectProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ObjectProperty_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - object property name."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Property Name:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyName_JTextField = new JTextField ( 20 );
    __PropertyName_JTextField.setToolTipText("Property name for property to set.");
    __PropertyName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __PropertyName_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - property name."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Property type:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyType_JComboBox = new SimpleJComboBox ( false );
    List<String> typeChoices = new ArrayList<>();
    typeChoices.add ( "" ); // Use when setting special values or removing.
    typeChoices.add ( __command._Boolean );
    typeChoices.add ( __command._DateTime );
    typeChoices.add ( __command._Double );
    typeChoices.add ( __command._Integer );
    typeChoices.add ( __command._String );
    __PropertyType_JComboBox.setData(typeChoices);
    __PropertyType_JComboBox.select ( __command._String );
    __PropertyType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __PropertyType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - set the property type (default=from object property)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
    String ObjectID = "";
    String ObjectProperty = "";
    String PropertyName = "";
    String PropertyType = "";
    String AllowNull = "";

	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
        ObjectID = props.getValue ( "ObjectID" );
        ObjectProperty = props.getValue ( "ObjectProperty" );
        PropertyName = props.getValue ( "PropertyName" );
        PropertyType = props.getValue ( "PropertyType" );
        AllowNull = props.getValue ( "AllowNull" );
        if ( ObjectID == null ) {
            // Select default.
            __ObjectID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ObjectID_JComboBox,ObjectID, JGUIUtil.NONE, null, null ) ) {
                __ObjectID_JComboBox.select ( ObjectID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nObjectID value \"" + ObjectID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( ObjectProperty != null ) {
            __ObjectProperty_JTextField.setText ( ObjectProperty );
        }
        if ( PropertyName != null ) {
            __PropertyName_JTextField.setText ( PropertyName );
        }
        if ( PropertyType == null ) {
            // Select default.
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
	ObjectID = __ObjectID_JComboBox.getSelected();
    ObjectProperty = __ObjectProperty_JTextField.getText().trim();
    PropertyName = __PropertyName_JTextField.getText().trim();
	PropertyType = __PropertyType_JComboBox.getSelected();
	AllowNull = __AllowNull_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
    props.add ( "ObjectID=" + ObjectID );
    props.add ( "ObjectProperty=" + ObjectProperty );
    props.add ( "PropertyName=" + PropertyName );
    props.add ( "PropertyType=" + PropertyType );
    props.add ( "AllowNull=" + AllowNull );
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

public void windowOpened( WindowEvent evt ) {
}

}