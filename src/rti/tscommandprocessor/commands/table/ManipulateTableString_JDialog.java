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
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Table.DataTableStringManipulation;
import RTi.Util.Table.DataTableStringOperatorType;

public class ManipulateTableString_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;// Cancel Button
private SimpleJButton __ok_JButton = null;	// Ok Button
private ManipulateTableString_Command __command = null;	// Command to edit
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TableID_JComboBox = null;
private SimpleJComboBox __InputColumn1_JComboBox = null;
private SimpleJComboBox __Operator_JComboBox = null;
private SimpleJComboBox __InputColumn2_JComboBox = null;
private JTextField __InputValue2_JTextField = null;
private JTextField __InputValue3_JTextField = null;
private SimpleJComboBox __OutputColumn_JComboBox = null;
private JTextArea __ColumnIncludeFilters_JTextArea = null;
private JTextArea __ColumnExcludeFilters_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.
private JFrame __parent = null;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public ManipulateTableString_JDialog ( JFrame parent, ManipulateTableString_Command command, List<String> tableIDChoices )
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
    else if ( event.getActionCommand().equalsIgnoreCase("EditColumnIncludeFilters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ColumnIncludeFilters = __ColumnIncludeFilters_JTextArea.getText().trim();
        String [] notes = {
            "Include rows from the output by specifying a pattern to match for a column.",
            "Only string columns can be specified."
        };
        String dict = (new DictionaryJDialog ( __parent, true, ColumnIncludeFilters,
            "Edit ColumnIncludeFilters Parameter", notes, "Table Column", "Pattern to include rows (* allowed)",10)).response();
        if ( dict != null ) {
            __ColumnIncludeFilters_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditColumnExcludeFilters") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String ColumnExcludeFilters = __ColumnExcludeFilters_JTextArea.getText().trim();
        String [] notes = {
            "Exclude rows from the output by specifying a pattern to match for a column.",
            "Only string columns can be specified."
        };
        String dict = (new DictionaryJDialog ( __parent, true, ColumnExcludeFilters,
            "Edit ColumnExcludeFilters Parameter", notes, "Table Column", "Pattern to exclude rows (* allowed)",10)).response();
        if ( dict != null ) {
            __ColumnExcludeFilters_JTextArea.setText ( dict );
            refresh();
        }
    }
}

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
	// Reset the tooltips based on the selected operator.
	__InputColumn1_JComboBox.setToolTipText("");
	__InputColumn2_JComboBox.setToolTipText("");
	__InputValue2_JTextField.setToolTipText("");
	__InputValue3_JTextField.setToolTipText("");
	String operator = __Operator_JComboBox.getSelected();
    if ( operator.equalsIgnoreCase( "" + DataTableStringOperatorType.APPEND) ) {
    	__InputColumn1_JComboBox.setToolTipText("Specify an input column as the input - see also InputColumn2 and InputValue2");
    	__InputColumn2_JComboBox.setToolTipText("Specify an input column that will be appended to InputColumn1... OR use InputValue2");
    	__InputValue2_JTextField.setToolTipText("Specify an input value that will be appended to InputColumn1... OR use InputColumn2");
    }
    else if ( operator.equalsIgnoreCase( "" + DataTableStringOperatorType.PREPEND) ) {
    	__InputColumn1_JComboBox.setToolTipText("Specify an input column as the input - see also InputColumn2 and InputValue2");
    	__InputColumn2_JComboBox.setToolTipText("Specify an input column that will be prepended to InputColumn1... OR use InputValue2");
    	__InputValue2_JTextField.setToolTipText("Specify an input value that will be prepended to InputColumn1... OR use InputColumn2");
    }
    else if ( operator.equalsIgnoreCase( "" + DataTableStringOperatorType.REPLACE) ) {
    	__InputColumn1_JComboBox.setToolTipText("Specify an input column that will have substrings replaced - see also InputValue2 and InputValue3");
    	__InputValue2_JTextField.setToolTipText("Specify the substring from the input to be replaced - see also InputValue3");
    	__InputValue3_JTextField.setToolTipText("Specify the substring to be inserted as the replacement for InputValue2");
    }
    else if ( operator.equalsIgnoreCase( "" + DataTableStringOperatorType.SUBSTRING) ) {
    	__InputColumn1_JComboBox.setToolTipText("Specify an input column that will have a substring extracted - see also InputValue2 and InputValue3");
    	__InputValue2_JTextField.setToolTipText("Specify the starting character position 1+ for the extracted substring - see also InputValue3");
    	__InputValue3_JTextField.setToolTipText("Specify the ending character position 1+ for the extracted substring");
    }
    // TODO SAM 2015-04-29 Need to enable boolean
    //choices.add ( DataTableStringOperatorType.TO_BOOLEAN );
    else if ( operator.equalsIgnoreCase( "" + DataTableStringOperatorType.TO_DATE) ) {
    	__InputColumn1_JComboBox.setToolTipText("Specify a table input column to convert to date value");
    }
    else if ( operator.equalsIgnoreCase( "" + DataTableStringOperatorType.TO_DATE_TIME) ) {
    	__InputColumn1_JComboBox.setToolTipText("Specify a table input column to convert to date/time value");
    }
    else if ( operator.equalsIgnoreCase( "" + DataTableStringOperatorType.TO_DOUBLE) ) {
    	__InputColumn1_JComboBox.setToolTipText("Specify a table input column to convert to double value");
    }
    else if ( operator.equalsIgnoreCase( "" + DataTableStringOperatorType.TO_INTEGER) ) {
    	__InputColumn1_JComboBox.setToolTipText("Specify a table input column to convert to integer value");
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
    String TableID = __TableID_JComboBox.getSelected();
	String ColumnIncludeFilters = __ColumnIncludeFilters_JTextArea.getText().trim();
	String ColumnExcludeFilters = __ColumnExcludeFilters_JTextArea.getText().trim();
    String InputColumn1 = __InputColumn1_JComboBox.getSelected();
    String InputColumn2 = __InputColumn2_JComboBox.getSelected();
    String InputValue2 = __InputValue2_JTextField.getText();
    String InputValue3 = __InputValue3_JTextField.getText();
    String Operator = __Operator_JComboBox.getSelected();
    String OutputColumn = __OutputColumn_JComboBox.getSelected();
	PropList parameters = new PropList ( "" );

	__error_wait = false;

    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( ColumnIncludeFilters.length() > 0 ) {
    	parameters.set ( "ColumnIncludeFilters", ColumnIncludeFilters );
    }
    if ( ColumnExcludeFilters.length() > 0 ) {
    	parameters.set ( "ColumnExcludeFilters", ColumnExcludeFilters );
    }
    if ( InputColumn1.length() > 0 ) {
        parameters.set ( "InputColumn1", InputColumn1 );
    }
    if ( InputColumn2.length() > 0 ) {
        parameters.set ( "InputColumn2", InputColumn2 );
    }
    if ( InputValue2.length() > 0 ) {
        parameters.set ( "InputValue2", InputValue2 );
    }
    if ( InputValue3.length() > 0 ) {
        parameters.set ( "InputValue3", InputValue3 );
    }
    if ( Operator.length() > 0 ) {
        parameters.set ( "Operator", Operator );
    }
    if ( OutputColumn.length() > 0 ) {
        parameters.set ( "OutputColumn", OutputColumn );
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
	String ColumnIncludeFilters = __ColumnIncludeFilters_JTextArea.getText().trim();
	String ColumnExcludeFilters = __ColumnExcludeFilters_JTextArea.getText().trim();
    String InputColumn1 = __InputColumn1_JComboBox.getSelected();
    String InputColumn2 = __InputColumn2_JComboBox.getSelected();
    String InputValue2 = __InputValue2_JTextField.getText();
    String InputValue3 = __InputValue3_JTextField.getText();
    String Operator = __Operator_JComboBox.getSelected();
    String OutputColumn = __OutputColumn_JComboBox.getSelected();
    __command.setCommandParameter ( "TableID", TableID );
	__command.setCommandParameter ( "ColumnIncludeFilters", ColumnIncludeFilters );
	__command.setCommandParameter ( "ColumnExcludeFilters", ColumnExcludeFilters );
    __command.setCommandParameter ( "InputColumn1", InputColumn1 );
    __command.setCommandParameter ( "InputColumn2", InputColumn2 );
    __command.setCommandParameter ( "InputValue2", InputValue2 );
    __command.setCommandParameter ( "InputValue3", InputValue3 );
    __command.setCommandParameter ( "Operator", Operator );
    __command.setCommandParameter ( "OutputColumn", OutputColumn );

}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, ManipulateTableString_Command command, List<String> tableIDChoices )
{	__parent = parent;
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Perform simple manipulation on columns of string data in a table, using one of the following approaches:" ), 
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "   - process input values from two columns to populate the output column" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "   - process input values from a column and a constant to populate the output column" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "   - convert an input string value into an output value (operator ToDate, ToDateTime, etc.)" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The operator defines the number of values that are needed as input - mouse over the input fields for feedback on what is needed." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table to process."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Column filters to include rows:"),
        0, ++y, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ColumnIncludeFilters_JTextArea = new JTextArea (3,35);
    __ColumnIncludeFilters_JTextArea.setLineWrap ( true );
    __ColumnIncludeFilters_JTextArea.setWrapStyleWord ( true );
    __ColumnIncludeFilters_JTextArea.setToolTipText("TableColumn:DatastoreColumn,TableColumn:DataStoreColumn");
    __ColumnIncludeFilters_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__ColumnIncludeFilters_JTextArea),
        1, y, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - column patterns to include rows (default=include all)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditColumnIncludeFilters",this),
        3, ++y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Column filters to exclude rows:"),
        0, ++y, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ColumnExcludeFilters_JTextArea = new JTextArea (3,35);
    __ColumnExcludeFilters_JTextArea.setLineWrap ( true );
    __ColumnExcludeFilters_JTextArea.setWrapStyleWord ( true );
    __ColumnExcludeFilters_JTextArea.setToolTipText("TableColumn:DatastoreColumn,TableColumn:DataStoreColumn");
    __ColumnExcludeFilters_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__ColumnExcludeFilters_JTextArea),
        1, y, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - column patterns to exclude rows (default=include all)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, new SimpleJButton ("Edit","EditColumnExcludeFilters",this),
        3, ++y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input column 1:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputColumn1_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    Vector input1Choices = new Vector();
    input1Choices.add("");
    __InputColumn1_JComboBox.setData ( input1Choices ); // TODO SAM 2010-09-13 Need to populate via discovery
    __InputColumn1_JComboBox.addItemListener ( this );
    __InputColumn1_JComboBox.addKeyListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __InputColumn1_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - first input column name."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "String operator:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Operator_JComboBox = new SimpleJComboBox ( 12, false );// Do not allow edit
    __Operator_JComboBox.setData ( DataTableStringManipulation.getOperatorChoicesAsStrings() );
    __Operator_JComboBox.addItemListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __Operator_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - string manipulation to perform on input."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input column 2:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputColumn2_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    Vector input2Choices = new Vector();
    input2Choices.add("");
    __InputColumn2_JComboBox.setData ( input2Choices ); // TODO SAM 2010-09-13 Need to populate via discovery
    __InputColumn2_JComboBox.addItemListener ( this );
    __InputColumn2_JComboBox.addKeyListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __InputColumn2_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required if no input value 2 - second input column name."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input value 2:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputValue2_JTextField = new JTextField ( 10 ); 
    __InputValue2_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputValue2_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required if no input column 2 - constant string."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input value 3:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputValue3_JTextField = new JTextField ( 10 ); 
    __InputValue3_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputValue3_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - only by some operators."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output column:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputColumn_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
    __OutputColumn_JComboBox.setToolTipText("Specify the column name for the output string.");
    Vector outputChoices = new Vector();
    outputChoices.add("");
    __OutputColumn_JComboBox.setData ( outputChoices ); // TODO SAM 2010-09-13 Need to populate via discovery
    __OutputColumn_JComboBox.addItemListener ( this );
    __OutputColumn_JComboBox.addKeyListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __OutputColumn_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - output column name."), 
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
{	String routine = getClass().getSimpleName() + ".refresh";
    String TableID = "";
	String ColumnIncludeFilters = "";
	String ColumnExcludeFilters = "";
    String InputColumn1 = "";
    String InputColumn2 = "";
    String InputValue2 = "";
    String InputValue3 = "";
    String Operator = "";
    String OutputColumn = "";

	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
	    TableID = props.getValue ( "TableID" );
		ColumnIncludeFilters = props.getValue ( "ColumnIncludeFilters" );
		ColumnExcludeFilters = props.getValue ( "ColumnExcludeFilters" );
        InputColumn1 = props.getValue ( "InputColumn1" );
        InputColumn2 = props.getValue ( "InputColumn2" );
        InputValue2 = props.getValue ( "InputValue2" );
        InputValue3 = props.getValue ( "InputValue3" );
        Operator = props.getValue ( "Operator" );
		OutputColumn = props.getValue ( "OutputColumn" );
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
        if ( ColumnIncludeFilters != null ) {
            __ColumnIncludeFilters_JTextArea.setText ( ColumnIncludeFilters );
        }
        if ( ColumnExcludeFilters != null ) {
            __ColumnExcludeFilters_JTextArea.setText ( ColumnExcludeFilters );
        }
        if ( InputColumn1 == null ) {
            // Select default...
            __InputColumn1_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __InputColumn1_JComboBox,InputColumn1, JGUIUtil.NONE, null, null ) ) {
                __InputColumn1_JComboBox.select ( InputColumn1 );
            }
            else {
                // Just set the user-specified value
                __InputColumn1_JComboBox.setText( InputColumn1 );
            }
        }
        if ( Operator == null ) {
            // Select default...
            __Operator_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Operator_JComboBox,Operator, JGUIUtil.NONE, null, null ) ) {
                __Operator_JComboBox.select ( Operator );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nOperator value \"" + Operator +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( InputColumn2 == null ) {
            // Select default...
            __InputColumn2_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __InputColumn2_JComboBox,InputColumn2, JGUIUtil.NONE, null, null ) ) {
                __InputColumn2_JComboBox.select ( InputColumn2 );
            }
            else {
                // Just set the user-specified value
                __InputColumn2_JComboBox.setText( InputColumn2 );
            }
        }
        if ( InputValue2 != null ) {
            __InputValue2_JTextField.setText ( InputValue2 );
        }
        if ( InputValue3 != null ) {
            __InputValue3_JTextField.setText ( InputValue3 );
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
	}
	// Regardless, reset the command from the fields...
	TableID = __TableID_JComboBox.getSelected();
	ColumnIncludeFilters = __ColumnIncludeFilters_JTextArea.getText().trim();
	ColumnExcludeFilters = __ColumnExcludeFilters_JTextArea.getText().trim();
	InputColumn1 = __InputColumn1_JComboBox.getSelected();
    Operator = __Operator_JComboBox.getSelected();
    InputColumn2 = __InputColumn2_JComboBox.getSelected();
    InputValue2 = __InputValue2_JTextField.getText();
    InputValue3 = __InputValue3_JTextField.getText();
    OutputColumn = __OutputColumn_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
	props.add ( "ColumnIncludeFilters=" + ColumnIncludeFilters );
	props.add ( "ColumnExcludeFilters=" + ColumnExcludeFilters );
    props.add ( "InputColumn1=" + InputColumn1 );
    props.add ( "Operator=" + Operator );
    props.add ( "InputColumn2=" + InputColumn2 );
    props.add ( "InputValue2=" + InputValue2 );
    props.add ( "InputValue3=" + InputValue3 );
    props.add ( "OutputColumn=" + OutputColumn );
	__command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
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