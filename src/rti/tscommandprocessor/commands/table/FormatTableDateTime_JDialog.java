package rti.tscommandprocessor.commands.table;

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
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTimeFormatterSpecifiersJPanel;
import RTi.Util.Time.DateTimeFormatterType;
import RTi.Util.Time.YearType;

public class FormatTableDateTime_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private FormatTableDateTime_Command __command = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __InputColumn_JTextField = null;
private DateTimeFormatterSpecifiersJPanel __DateTimeFormat_JPanel = null;
private SimpleJComboBox __OutputYearType_JComboBox = null;
private SimpleJComboBox __OutputColumn_JComboBox = null;
private SimpleJComboBox __OutputType_JComboBox = null;
private JTextField __InsertBeforeColumn_JTextField = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public FormatTableDateTime_JDialog ( JFrame parent, FormatTableDateTime_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String TableID = __TableID_JComboBox.getSelected();
    String InputColumn = __InputColumn_JTextField.getText().trim();
    String FormatterType = __DateTimeFormat_JPanel.getSelectedFormatterType().trim();
    String DateTimeFormat =__DateTimeFormat_JPanel.getText().trim();
    String OutputYearType = __OutputYearType_JComboBox.getSelected();
    String OutputColumn = __OutputColumn_JComboBox.getSelected();
    String OutputType = __OutputType_JComboBox.getSelected();
    String InsertBeforeColumn = __InsertBeforeColumn_JTextField.getText().trim();

	__error_wait = false;

    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( InputColumn.length() > 0 ) {
        parameters.set ( "InputColumn", InputColumn );
    }
    if ( FormatterType.length() > 0 ) {
        parameters.set ( "FormatterType", FormatterType );
    }
    if ( DateTimeFormat.length() > 0 ) {
        parameters.set ( "DateTimeFormat", DateTimeFormat );
    }
    if ( OutputYearType.length() > 0 ) {
        parameters.set ( "OutputYearType", OutputYearType );
    }
    if ( OutputColumn.length() > 0 ) {
        parameters.set ( "OutputColumn", OutputColumn );
    }
    if ( OutputType.length() > 0 ) {
        parameters.set ( "OutputType", OutputType );
    }
    if ( InsertBeforeColumn.length() > 0 ) {
        parameters.set ( "InsertBeforeColumn", InsertBeforeColumn );
    }

	try {
	    // This will warn the user...
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
private void commitEdits ()
{	String TableID = __TableID_JComboBox.getSelected();
    String InputColumn = __InputColumn_JTextField.getText().trim();
    String FormatterType = __DateTimeFormat_JPanel.getSelectedFormatterType().trim();
    String DateTimeFormat =__DateTimeFormat_JPanel.getText().trim();
    String OutputYearType = __OutputYearType_JComboBox.getSelected();
    String OutputColumn = __OutputColumn_JComboBox.getSelected();
    String OutputType = __OutputType_JComboBox.getSelected();
    String InsertBeforeColumn = __InsertBeforeColumn_JTextField.getText().trim();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "InputColumn", InputColumn );
    __command.setCommandParameter ( "FormatterType", FormatterType );
    __command.setCommandParameter ( "DateTimeFormat", DateTimeFormat );
    __command.setCommandParameter ( "OutputYearType", OutputYearType );
    __command.setCommandParameter ( "OutputColumn", OutputColumn );
    __command.setCommandParameter ( "OutputType", OutputType );
    __command.setCommandParameter ( "InsertBeforeColumn", InsertBeforeColumn );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, FormatTableDateTime_Command command, List<String> tableIDChoices )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Format the contents of a date/time input column to create values in the output column." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The input column must have a type of Date, DateTime, or a string that can be parsed to a date/time." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The output type can be set to:" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "    DateTime - if the resulting formatted string can be parsed to a date/time (e.g., with less precision than original)" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "    (note TSTool by default displays all DateTime objets using ISO YYYY-MM-DD, etc. notation in tables)" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "    Double - if the resulting formatted string can be parsed to a floating point number (e.g., year only)" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "    Integer - if the resulting formatted string can be parsed to an integer (e.g., year only)" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "    String - if the resulting formatted string should be treated as a literal string" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __TableID_JComboBox.setToolTipText("Specify the table ID to process or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table to process."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input column" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputColumn_JTextField = new JTextField ( 30 );
    __InputColumn_JTextField.setToolTipText("Specify the input column name or use ${Property} notation");
    __InputColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - name of date/time column to process."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JLabel DateTimeFormat_JLabel = new JLabel ("Date/time format:");
    JGUIUtil.addComponent(main_JPanel, DateTimeFormat_JLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeFormat_JPanel = new DateTimeFormatterSpecifiersJPanel(20,true,true,null,true,true);
    __DateTimeFormat_JPanel.getTextField().setToolTipText("Specify the date/time format or use ${Property} notation");
    __DateTimeFormat_JPanel.addKeyListener (this);
    __DateTimeFormat_JPanel.addFormatterTypeItemListener (this); // Respond to changes in formatter choice
    __DateTimeFormat_JPanel.getDocument().addDocumentListener(this); // Respond to changes in text field contents
    JGUIUtil.addComponent(main_JPanel, __DateTimeFormat_JPanel,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Required - to specify output format."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "OutputYearType:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputYearType_JComboBox = new SimpleJComboBox ( false );
    List<String> choices = YearType.getYearTypeChoicesAsStrings();
    choices.add(0,"");
    __OutputYearType_JComboBox.setData(choices);
    JGUIUtil.addComponent(main_JPanel, __OutputYearType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - year type to interpret ${dt:YearForTypeYear} (default=" + YearType.CALENDAR + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output column:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputColumn_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __OutputColumn_JComboBox.setToolTipText("Specify the output column name or use ${Property} notation");
    List<String> outputChoices = new ArrayList<String>();
    outputChoices.add("");
    __OutputColumn_JComboBox.setData ( outputChoices ); // TODO SAM 2010-09-13 Need to populate via discovery
    __OutputColumn_JComboBox.addItemListener ( this );
    __OutputColumn_JComboBox.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputColumn_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - output column name."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output type:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputType_JComboBox = new SimpleJComboBox ( false );
    __OutputType_JComboBox.addItem ( "" );
    __OutputType_JComboBox.addItem ( __command._DateTime );
    __OutputType_JComboBox.addItem ( __command._Double );
    __OutputType_JComboBox.addItem ( __command._Integer );
    __OutputType_JComboBox.addItem ( __command._String );
    __OutputType_JComboBox.select ( __command._String );
    __OutputType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - specify output column type (default=" + __command._String + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Insert before column:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InsertBeforeColumn_JTextField = new JTextField ( 30 );
    __InsertBeforeColumn_JTextField.setToolTipText("Specify the column name to insert before or use ${Property} notation");
    __InsertBeforeColumn_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InsertBeforeColumn_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - column to insert before (default=at end)."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
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

	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

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
public void itemStateChanged ( ItemEvent e )
{	checkGUIState();
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
	else {
	    // Combo box...
		refresh();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

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
    String InputColumn = "";
    String FormatterType = "";
    String DateTimeFormat = "";
    String OutputYearType = "";
    String OutputColumn = "";
    String OutputType = "";
    String InsertBeforeColumn = "";

	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
	    TableID = props.getValue ( "TableID" );
        InputColumn = props.getValue ( "InputColumn" );
        FormatterType = props.getValue ( "FormatterType" );
        DateTimeFormat = props.getValue ( "DateTimeFormat" );
        OutputYearType = props.getValue ( "OutputYearType" );
		OutputColumn = props.getValue ( "OutputColumn" );
		OutputType = props.getValue ( "OutputType" );
		InsertBeforeColumn = props.getValue ( "InsertBeforeColumn" );
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
        if ( InputColumn != null ) {
            __InputColumn_JTextField.setText ( InputColumn );
        }
        if ( (FormatterType == null) || FormatterType.equals("") ) {
            // Select default...
            __DateTimeFormat_JPanel.selectFormatterType(null);
        }
        else {
            try {
                __DateTimeFormat_JPanel.selectFormatterType(DateTimeFormatterType.valueOfIgnoreCase(FormatterType));
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nFormatterType value \"" + FormatterType +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (DateTimeFormat != null) {
            // The front part of the string may match a formatter type (e.g., "C:") in which case
            // only the latter part of the string should be displayed.
            int pos = DateTimeFormat.indexOf(":");
            if ( pos > 0 ) {
                try {
                    __DateTimeFormat_JPanel.selectFormatterType(
                        DateTimeFormatterType.valueOfIgnoreCase(DateTimeFormat.substring(0,pos)));
                    Message.printStatus(2, routine, "Selecting format \"" + DateTimeFormat.substring(0,pos) + "\"");
                    if ( DateTimeFormat.length() > pos ) {
                        __DateTimeFormat_JPanel.setText(DateTimeFormat.substring(pos + 1));
                    }
                }
                catch ( IllegalArgumentException e ) {
                    __DateTimeFormat_JPanel.setText(DateTimeFormat);
                }
            }
            else {
                __DateTimeFormat_JPanel.setText(DateTimeFormat);
            }
        }
        if ( OutputYearType == null ) {
            // Select default...
            __OutputYearType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputYearType_JComboBox,OutputYearType, JGUIUtil.NONE, null, null ) ) {
                __OutputYearType_JComboBox.select ( OutputYearType );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nOutputYearType value \"" + OutputYearType +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( OutputColumn == null ) {
            // Select default...
            __OutputColumn_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputColumn_JComboBox,OutputColumn, JGUIUtil.NONE, null, null ) ) {
                __OutputColumn_JComboBox.select ( OutputColumn );
            }
            else {
                // Just set the user-specified value
                __OutputColumn_JComboBox.setText( OutputColumn );
            }
        }
        if ( OutputType == null ) {
            // Select default...
            __OutputType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputType_JComboBox,OutputType, JGUIUtil.NONE, null, null ) ) {
                __OutputType_JComboBox.select ( OutputType );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nOutputType value \"" + OutputType +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( InsertBeforeColumn != null ) {
            __InsertBeforeColumn_JTextField.setText ( InsertBeforeColumn );
        }
	}
	// Regardless, reset the command from the fields...
	TableID = __TableID_JComboBox.getSelected();
	InputColumn = __InputColumn_JTextField.getText();
    FormatterType = __DateTimeFormat_JPanel.getSelectedFormatterType().trim();
	DateTimeFormat = __DateTimeFormat_JPanel.getText().trim();
	OutputYearType = __OutputYearType_JComboBox.getSelected();
    OutputColumn = __OutputColumn_JComboBox.getSelected();
    OutputType = __OutputType_JComboBox.getSelected();
    InsertBeforeColumn = __InsertBeforeColumn_JTextField.getText();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
    props.add ( "InputColumn=" + InputColumn );
    props.add ( "FormatterType=" + FormatterType );
    props.add ( "DateTimeFormat=" + DateTimeFormat );
    props.add ( "OutputYearType=" + OutputYearType );
    props.add ( "OutputColumn=" + OutputColumn );
    props.add ( "OutputType=" + OutputType );
    props.add ( "InsertBeforeColumn=" + InsertBeforeColumn );
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