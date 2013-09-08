package rti.tscommandprocessor.commands.table;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

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

import java.util.List;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class CreateTimeSeriesEventTable_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private boolean __error_wait = false; // To track errors
private boolean __first_time = true; // Indicate first time display
private JTextArea __command_JTextArea = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __NewTableID_JTextField = null;
private JTextField __IncludeColumns_JTextField = null;
private JTextField __InputTableEventIDColumn_JTextField = null;
private JTextField __InputTableEventTypeColumn_JTextField = null;
private JTextField __InputTableEventStartColumn_JTextField = null;
private JTextField __InputTableEventEndColumn_JTextField = null;
private JTextField __InputTableEventLocationTypeColumn_JTextField = null;
private JTextField __InputTableEventLocationIDColumn_JTextField = null;
private JTextField __InputTableEventLabelColumn_JTextField = null;
private JTextField __InputTableEventDescriptionColumn_JTextField = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private CreateTimeSeriesEventTable_Command __command = null;
private boolean __ok = false;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public CreateTimeSeriesEventTable_JDialog ( JFrame parent, CreateTimeSeriesEventTable_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TableID = __TableID_JComboBox.getSelected();
    String NewTableID = __NewTableID_JTextField.getText().trim();
	String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	String InputTableEventIDColumn = __InputTableEventIDColumn_JTextField.getText().trim();
	String InputTableEventTypeColumn = __InputTableEventTypeColumn_JTextField.getText().trim();
	String InputTableEventStartColumn = __InputTableEventStartColumn_JTextField.getText().trim();
	String InputTableEventEndColumn = __InputTableEventEndColumn_JTextField.getText().trim();
	String InputTableEventLocationTypeColumn = __InputTableEventLocationTypeColumn_JTextField.getText().trim();
	String InputTableEventLocationIDColumn = __InputTableEventLocationIDColumn_JTextField.getText().trim();
	String InputTableEventLabelColumn = __InputTableEventLabelColumn_JTextField.getText().trim();
	String InputTableEventDescriptionColumn = __InputTableEventDescriptionColumn_JTextField.getText().trim();
	__error_wait = false;

    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( NewTableID.length() > 0 ) {
        props.set ( "NewTableID", NewTableID );
    }
	if ( IncludeColumns.length() > 0 ) {
		props.set ( "IncludeColumns", IncludeColumns );
	}
    if ( InputTableEventIDColumn.length() > 0 ) {
        props.set ( "InputTableEventIDColumn", InputTableEventIDColumn );
    }
    if ( InputTableEventTypeColumn.length() > 0 ) {
        props.set ( "InputTableEventTypeColumn", InputTableEventTypeColumn );
    }
    if ( InputTableEventStartColumn.length() > 0 ) {
        props.set ( "InputTableEventStartColumn", InputTableEventStartColumn );
    }
    if ( InputTableEventEndColumn.length() > 0 ) {
        props.set ( "InputTableEventEndColumn", InputTableEventEndColumn );
    }
    if ( InputTableEventLocationTypeColumn.length() > 0 ) {
        props.set ( "InputTableEventLocationTypeColumn", InputTableEventLocationTypeColumn );
    }
    if ( InputTableEventLocationIDColumn.length() > 0 ) {
        props.set ( "InputTableEventLocationIDColumn", InputTableEventLocationIDColumn );
    }
    if ( InputTableEventLabelColumn.length() > 0 ) {
        props.set ( "InputTableEventLabelColumn", InputTableEventLabelColumn );
    }
    if ( InputTableEventDescriptionColumn.length() > 0 ) {
        props.set ( "InputTableEventDescriptionColumn", InputTableEventDescriptionColumn );
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
{	String TableID = __TableID_JComboBox.getSelected();
    String NewTableID = __NewTableID_JTextField.getText().trim();
    String IncludeColumns = __IncludeColumns_JTextField.getText().trim();
    String InputTableEventIDColumn = __InputTableEventIDColumn_JTextField.getText().trim();
    String InputTableEventTypeColumn = __InputTableEventTypeColumn_JTextField.getText().trim();
    String InputTableEventStartColumn = __InputTableEventStartColumn_JTextField.getText().trim();
    String InputTableEventEndColumn = __InputTableEventEndColumn_JTextField.getText().trim();
    String InputTableEventLocationTypeColumn = __InputTableEventLocationTypeColumn_JTextField.getText().trim();
    String InputTableEventLocationIDColumn = __InputTableEventLocationIDColumn_JTextField.getText().trim();
    String InputTableEventLabelColumn = __InputTableEventLabelColumn_JTextField.getText().trim();
    String InputTableEventDescriptionColumn = __InputTableEventDescriptionColumn_JTextField.getText().trim();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "NewTableID", NewTableID );
    __command.setCommandParameter ( "IncludeColumns", IncludeColumns );
	__command.setCommandParameter ( "InputTableEventIDColumn", InputTableEventIDColumn );
	__command.setCommandParameter ( "InputTableEventTypeColumn", InputTableEventTypeColumn );
	__command.setCommandParameter ( "InputTableEventStartColumn", InputTableEventStartColumn );
	__command.setCommandParameter ( "InputTableEventEndColumn", InputTableEventEndColumn );
	__command.setCommandParameter ( "InputTableEventLocationTypeColumn", InputTableEventLocationTypeColumn );
	__command.setCommandParameter ( "InputTableEventLocationIDColumn", InputTableEventLocationIDColumn );
	__command.setCommandParameter ( "InputTableEventLabelColumn", InputTableEventLabelColumn );
	__command.setCommandParameter ( "InputTableEventDescriptionColumn", InputTableEventDescriptionColumn );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__IncludeColumns_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, CreateTimeSeriesEventTable_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = 0;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = 0;
    
   	JGUIUtil.addComponent(paragraph, new JLabel (
        "This command creates a new time series event table, which associates time series with events " +
        "that have temporal and spatial properties."),
        0, yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(paragraph, new JLabel (
        "Events may be based on data extremes (e.g., drought, flood) or other data (e.g., political, legal events and decisions)."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    __main_JTabbedPane = new JTabbedPane ();
    __main_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify how to create the time series event table" ));
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for separate options for creating the event table
    int yTable = -1;
    JPanel table_JPanel = new JPanel();
    table_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Create from existing table", table_JPanel );

    JGUIUtil.addComponent(table_JPanel, new JLabel ( "Table ID:" ), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(table_JPanel, __TableID_JComboBox,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel( "Required - original table."), 
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(table_JPanel, new JLabel ("New table ID:"),
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewTableID_JTextField = new JTextField (10);
    __NewTableID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(table_JPanel, __NewTableID_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - unique identifier for the new table."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Column names to copy:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeColumns_JTextField = new JTextField (10);
    __IncludeColumns_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __IncludeColumns_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Optional - names of columns to copy (default=copy all)."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event ID column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventIDColumn_JTextField = new JTextField (10);
    __InputTableEventIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event ID."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event type column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventTypeColumn_JTextField = new JTextField (10);
    __InputTableEventTypeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventTypeColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event type."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event start column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventStartColumn_JTextField = new JTextField (10);
    __InputTableEventStartColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventStartColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event start."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event end column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventEndColumn_JTextField = new JTextField (10);
    __InputTableEventEndColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventEndColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event end."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event location type column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventLocationTypeColumn_JTextField = new JTextField (10);
    __InputTableEventLocationTypeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventLocationTypeColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for location type."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event location ID column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventLocationIDColumn_JTextField = new JTextField (10);
    __InputTableEventLocationIDColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventLocationIDColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for location ID."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event label column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventLabelColumn_JTextField = new JTextField (10);
    __InputTableEventLabelColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventLabelColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event label."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Event description column:"), 
        0, ++yTable, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputTableEventDescriptionColumn_JTextField = new JTextField (10);
    __InputTableEventDescriptionColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(table_JPanel, __InputTableEventDescriptionColumn_JTextField,
        1, yTable, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(table_JPanel, new JLabel ("Required - input table column name for event description."),
        3, yTable, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,40);
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
 
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText ( "Close window without saving changes." );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add (__ok_JButton);
	__ok_JButton.setToolTipText ( "Close window and save changes to command." );

	setTitle ( "Edit " + __command.getCommandName() + "() Command");
	setResizable (false);
    pack();
    JGUIUtil.center(this);
	refresh();
    super.setVisible(true);
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e) {
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
{	String routine = getClass().getName() + ".refresh";
    String TableID = "";
    String NewTableID = "";
    String IncludeColumns = "";
    String InputTableEventIDColumn = "";
    String InputTableEventTypeColumn = "";
    String InputTableEventStartColumn = "";
    String InputTableEventEndColumn = "";
    String InputTableEventLocationTypeColumn = "";
    String InputTableEventLocationIDColumn = "";
    String InputTableEventLabelColumn = "";
    String InputTableEventDescriptionColumn = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
        TableID = props.getValue ( "TableID" );
        NewTableID = props.getValue ( "NewTableID" );
        IncludeColumns = props.getValue ( "IncludeColumns" );
        InputTableEventIDColumn = props.getValue ( "InputTableEventIDColumn" );
        InputTableEventTypeColumn = props.getValue ( "InputTableEventTypeColumn" );
        InputTableEventStartColumn = props.getValue ( "InputTableEventStartColumn" );
        InputTableEventEndColumn = props.getValue ( "InputTableEventEndColumn" );
        InputTableEventLocationTypeColumn = props.getValue ( "InputTableEventLocationTypeColumn" );
        InputTableEventLocationIDColumn = props.getValue ( "InputTableEventLocationIDColumn" );
        InputTableEventLabelColumn = props.getValue ( "InputTableEventLabelColumn" );
        InputTableEventDescriptionColumn = props.getValue ( "InputTableEventDescriptionColumn" );
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
        if ( NewTableID != null ) {
            __NewTableID_JTextField.setText ( NewTableID );
        }
		if ( IncludeColumns != null ) {
			__IncludeColumns_JTextField.setText ( IncludeColumns );
		}
        if ( InputTableEventIDColumn != null ) {
            __InputTableEventIDColumn_JTextField.setText ( InputTableEventIDColumn );
        }
        if ( InputTableEventTypeColumn != null ) {
            __InputTableEventTypeColumn_JTextField.setText ( InputTableEventTypeColumn );
        }
        if ( InputTableEventStartColumn != null ) {
            __InputTableEventStartColumn_JTextField.setText ( InputTableEventStartColumn );
        }
        if ( InputTableEventEndColumn != null ) {
            __InputTableEventEndColumn_JTextField.setText ( InputTableEventEndColumn );
        }
        if ( InputTableEventLocationTypeColumn != null ) {
            __InputTableEventLocationTypeColumn_JTextField.setText ( InputTableEventLocationTypeColumn );
        }
        if ( InputTableEventLocationIDColumn != null ) {
            __InputTableEventLocationIDColumn_JTextField.setText ( InputTableEventLocationIDColumn );
        }
        if ( InputTableEventLabelColumn != null ) {
            __InputTableEventLabelColumn_JTextField.setText ( InputTableEventLabelColumn );
        }
        if ( InputTableEventDescriptionColumn != null ) {
            __InputTableEventDescriptionColumn_JTextField.setText ( InputTableEventDescriptionColumn );
        }
	}
	// Regardless, reset the command from the fields...
	TableID = __TableID_JComboBox.getSelected();
    NewTableID = __NewTableID_JTextField.getText().trim();
	IncludeColumns = __IncludeColumns_JTextField.getText().trim();
	InputTableEventIDColumn = __InputTableEventIDColumn_JTextField.getText().trim();
	InputTableEventTypeColumn = __InputTableEventTypeColumn_JTextField.getText().trim();
	InputTableEventStartColumn = __InputTableEventStartColumn_JTextField.getText().trim();
	InputTableEventEndColumn = __InputTableEventEndColumn_JTextField.getText().trim();
	InputTableEventLocationTypeColumn = __InputTableEventLocationTypeColumn_JTextField.getText().trim();
	InputTableEventLocationIDColumn = __InputTableEventLocationIDColumn_JTextField.getText().trim();
	InputTableEventLabelColumn = __InputTableEventLabelColumn_JTextField.getText().trim();
	InputTableEventDescriptionColumn = __InputTableEventDescriptionColumn_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
    props.add ( "NewTableID=" + NewTableID );
    props.add ( "IncludeColumns=" + IncludeColumns );
	props.add ( "InputTableEventIDColumn=" + InputTableEventIDColumn );
	props.add ( "InputTableEventTypeColumn=" + InputTableEventTypeColumn );
	props.add ( "InputTableEventStartColumn=" + InputTableEventStartColumn );
	props.add ( "InputTableEventEndColumn=" + InputTableEventEndColumn );
	props.add ( "InputTableEventLocationTypeColumn=" + InputTableEventLocationTypeColumn );
	props.add ( "InputTableEventLocationIDColumn=" + InputTableEventLocationIDColumn );
	props.add ( "InputTableEventLabelColumn=" + InputTableEventLabelColumn );
	props.add ( "InputTableEventDescriptionColumn=" + InputTableEventDescriptionColumn );
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