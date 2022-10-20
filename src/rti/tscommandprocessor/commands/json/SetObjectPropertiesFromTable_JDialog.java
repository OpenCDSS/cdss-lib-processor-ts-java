// SetObjectPropertiesFromTable_JDialog - editor dialog for SetObjectPropertiesFromTable command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class SetObjectPropertiesFromTable_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SetObjectPropertiesFromTable_Command __command = null;
private SimpleJComboBox __ObjectID_JComboBox = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextArea __MatchMap_JTextArea = null;
private JTextField __IncludeColumns_JTextField = null;
private JTextArea __PropertyMap_JTextArea = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.
private JFrame __parent = null;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param objectIDChoices choices for ObjectID value.
@param tableIDChoices choices for TableID value.
*/
public SetObjectPropertiesFromTable_JDialog ( JFrame parent, SetObjectPropertiesFromTable_Command command,
	List<String> objectIDChoices, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, objectIDChoices, tableIDChoices );
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
    else if ( event.getActionCommand().equalsIgnoreCase("EditMatchMap") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String PropertyMap = __MatchMap_JTextArea.getText().trim();
        String [] notes = {
            "Specify one or more table columns and object property names used to match records.",
            "For example, match a location identifier in table column value with an object property.",
            "Use a.b.c to indicate a property in the object's data hierarchy.",
            "Lists in the object will be iterated over to check property values."
        };
        String columnFilters = (new DictionaryJDialog ( __parent, true, PropertyMap, "Edit MatchMap Parameter",
            notes, "Table Column Name", "Object Property Name",10)).response();
        if ( columnFilters != null ) {
            __MatchMap_JTextArea.setText ( columnFilters );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditPropertyMap") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String PropertyMap = __PropertyMap_JTextArea.getText().trim();
        String [] notes = {
            "By default, object property names are the same as the table column names.",
            "However, object property names can be changed by specifying information below."
        };
        String columnFilters = (new DictionaryJDialog ( __parent, true, PropertyMap, "Edit PropertyMap Parameter",
            notes, "Table Column Name", "Object Property Name",10)).response();
        if ( columnFilters != null ) {
            __PropertyMap_JTextArea.setText ( columnFilters );
            refresh();
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "SetObjectPropertiesFromTable");
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
    String ObjectID = __ObjectID_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
    String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
    String MatchMap = __MatchMap_JTextArea.getText().trim().replace("\n"," ");
    String PropertyMap = __PropertyMap_JTextArea.getText().trim().replace("\n"," ");
	PropList parameters = new PropList ( "" );

	__error_wait = false;

    if ( ObjectID.length() > 0 ) {
        parameters.set ( "ObjectID", ObjectID );
    }
    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( IncludeColumns.length() > 0 ) {
        parameters.set ( "IncludeColumns", IncludeColumns );
    }
    if ( MatchMap.length() > 0 ) {
        parameters.set ( "MatchMap", MatchMap );
    }
    if ( PropertyMap.length() > 0 ) {
        parameters.set ( "PropertyMap", PropertyMap );
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
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits () {
    String ObjectID = __ObjectID_JComboBox.getSelected();
    String TableID = __TableID_JComboBox.getSelected();
    String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
    String MatchMap = __MatchMap_JTextArea.getText().trim().replace("\n"," ");
    String PropertyMap = __PropertyMap_JTextArea.getText().trim().replace("\n"," ");
    __command.setCommandParameter ( "ObjectID", ObjectID );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "IncludeColumns", IncludeColumns );
    __command.setCommandParameter ( "MatchMap", MatchMap );
    __command.setCommandParameter ( "PropertyMap", PropertyMap );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
@param objectIDChoices list of choices for object identifiers
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, SetObjectPropertiesFromTable_Command command,
	List<String> objectIDChoices, List<String> tableIDChoices )
{	__command = command;
    __parent = parent;

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
		"Set object properties using matching input from a table." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The table value is determined by matching object property with table column value."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "For example, set properties for a location." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
       "The properties type from the table is retained in the object property (e.g., float retained as float, string as string)." ), 
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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table to process."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Include table columns:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeColumns_JTextField = new JTextField ( 10 );
    __IncludeColumns_JTextField.setToolTipText("List of table column names to include, can use ${Property}, use * for all.");
    __IncludeColumns_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IncludeColumns_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - table column names from which to set properties (default=*)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Match map:" ),
        0, ++y, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MatchMap_JTextArea = new JTextArea (3,35);
    __MatchMap_JTextArea.setLineWrap ( true );
    __MatchMap_JTextArea.setWrapStyleWord ( true );
    __MatchMap_JTextArea.setToolTipText("ColumnName1:Property1,ColumnName2:Property2");
    __MatchMap_JTextArea.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__MatchMap_JTextArea),
        1, y, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - how to match table rows to object property names."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditMatchMap",this),
        3, ++y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Property map:" ),
        0, ++y, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyMap_JTextArea = new JTextArea (3,35);
    __PropertyMap_JTextArea.setLineWrap ( true );
    __PropertyMap_JTextArea.setWrapStyleWord ( true );
    __PropertyMap_JTextArea.setToolTipText("ColumnName1:Property1,ColumnName2:Property2");
    __PropertyMap_JTextArea.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__PropertyMap_JTextArea),
        1, y, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - map of table rows to object property names (default=table columns)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditPropertyMap",this),
        3, ++y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
    String TableID = "";
    String IncludeColumns = "";
    String MatchMap = "";
    String PropertyMap = "";

	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
        ObjectID = props.getValue ( "ObjectID" );
        TableID = props.getValue ( "TableID" );
        IncludeColumns = props.getValue ( "IncludeColumns" );
        MatchMap = props.getValue ( "MatchMap" );
        PropertyMap = props.getValue ( "PropertyMap" );
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
        if ( TableID == null ) {
            // Select default.
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
        if ( IncludeColumns != null ) {
            __IncludeColumns_JTextField.setText ( IncludeColumns );
        }
        if ( MatchMap != null ) {
            __MatchMap_JTextArea.setText ( MatchMap );
        }
        if ( PropertyMap != null ) {
            __PropertyMap_JTextArea.setText ( PropertyMap );
        }
	}
	// Regardless, reset the command from the fields.
	ObjectID = __ObjectID_JComboBox.getSelected();
	TableID = __TableID_JComboBox.getSelected();
    IncludeColumns = __IncludeColumns_JTextField.getText().trim();
    MatchMap = __MatchMap_JTextArea.getText().trim().replace("\n"," ");
    PropertyMap = __PropertyMap_JTextArea.getText().trim().replace("\n"," ");
	props = new PropList ( __command.getCommandName() );
    props.add ( "ObjectID=" + ObjectID );
    props.add ( "TableID=" + TableID );
    props.add ( "IncludeColumns=" + IncludeColumns );
    props.add ( "MatchMap=" + MatchMap );
    props.add ( "PropertyMap=" + PropertyMap );
	__command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok;	// Save to be returned by ok().
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
public void windowClosing( WindowEvent event )
{	response ( false );
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