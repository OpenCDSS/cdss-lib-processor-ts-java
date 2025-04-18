// TableMath_JDialog - editor dialog for TableMath command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2025 Colorado Department of Natural Resources

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

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Table.DataTableMath;
import RTi.Util.Table.TableField;

@SuppressWarnings("serial")
public class TableMath_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private TableMath_Command __command = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __Condition_JTextField = null;
private JTextField __ProcessRows_JTextField = null;
private SimpleJComboBox __Input1_JComboBox = null;
private SimpleJComboBox __Operator_JComboBox = null;
private SimpleJComboBox __Input2_JComboBox = null;
private SimpleJComboBox __Output_JComboBox = null;
private SimpleJComboBox __OutputType_JComboBox = null;
private SimpleJComboBox __NonValue_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public TableMath_JDialog ( JFrame parent, TableMath_Command command, List<String> tableIDChoices ) {
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
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
		HelpViewer.getInstance().showHelp("command", "TableMath");
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
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
    String TableID = __TableID_JComboBox.getSelected();
	String Condition = __Condition_JTextField.getText().trim();
	String ProcessRows = __ProcessRows_JTextField.getText().trim();
    String Input1 = __Input1_JComboBox.getSelected();
    String Input2 = __Input2_JComboBox.getSelected();
    String Operator = __Operator_JComboBox.getSelected();
    String Output = __Output_JComboBox.getSelected();
    String OutputType = __OutputType_JComboBox.getSelected();
    String NonValue = __NonValue_JComboBox.getSelected();
	PropList parameters = new PropList ( "" );

	__error_wait = false;

    if ( TableID.length() > 0 ) {
        parameters.set ( "TableID", TableID );
    }
    if ( Condition.length() > 0 ) {
        parameters.set ( "Condition", Condition );
    }
    if ( ProcessRows.length() > 0 ) {
        parameters.set ( "ProcessRows", ProcessRows );
    }
    if ( Input1.length() > 0 ) {
        parameters.set ( "Input1", Input1 );
    }
    if ( Input2.length() > 0 ) {
        parameters.set ( "Input2", Input2 );
    }
    if ( Operator.length() > 0 ) {
        parameters.set ( "Operator", Operator );
    }
    if ( Output.length() > 0 ) {
        parameters.set ( "Output", Output );
    }
    if ( OutputType.length() > 0 ) {
        parameters.set ( "OutputType", OutputType );
    }
    if ( NonValue.length() > 0 ) {
        parameters.set ( "NonValue", NonValue );
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
Commit the edits to the command.  In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String TableID = __TableID_JComboBox.getSelected();
	String Condition = __Condition_JTextField.getText().trim();
	String ProcessRows = __ProcessRows_JTextField.getText().trim();
    String Input1 = __Input1_JComboBox.getSelected();
    String Input2 = __Input2_JComboBox.getSelected();
    String Operator = __Operator_JComboBox.getSelected();
    String Output = __Output_JComboBox.getSelected();
    String OutputType = __OutputType_JComboBox.getSelected();
    String NonValue = __NonValue_JComboBox.getSelected();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "Condition", Condition );
    __command.setCommandParameter ( "ProcessRows", ProcessRows );
    __command.setCommandParameter ( "Input1", Input1 );
    __command.setCommandParameter ( "Input2", Input2 );
    __command.setCommandParameter ( "Operator", Operator );
    __command.setCommandParameter ( "Output", Output );
    __command.setCommandParameter ( "OutputType", OutputType );
    __command.setCommandParameter ( "NonValue", NonValue );

}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, TableMath_Command command, List<String> tableIDChoices ) {
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Perform a simple math operation on columns of data in a table, using one of the following approaches:" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "   - process input from one column to populate the output column" ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "   - process input from two columns to populate the output column" ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
        "   - process input from a column and a constant to populate the output column" ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The output column type, width, and precision are determined automatically from input, or use OutputType to specify." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __TableID_JComboBox.setToolTipText("Specify the table ID or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table to process."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Condition:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Condition_JTextField = new JTextField ( "", 25 );
    __Condition_JTextField.setToolTipText("Specify condition to evaluate for each row.  If true, process the row.");
    __Condition_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Condition_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - condition to match rows (default=all)." ),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Process rows:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ProcessRows_JTextField = new JTextField ( "", 25 );
    //__ProcessRows_JTextField.setToolTipText("Specify row numbers (1+) to process, separated by commas, can use ${Property}, \"first\" for the first row, \"last\" for the row.");
    __ProcessRows_JTextField.setToolTipText("Specify rows to process, separated by commas, can use ${Property}, currently only allow \"first\" for the first row, \"last\" for the last row.");
    __ProcessRows_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ProcessRows_JTextField,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - row numbers to process (default=all)." ),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input 1:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Input1_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit.
    __Input1_JComboBox.setToolTipText("Input column name, can use ${Property} notation");
    List<String> input1Choices = new ArrayList<>();
    input1Choices.add("");
    __Input1_JComboBox.setData ( input1Choices ); // TODO SAM 2010-09-13 Need to populate via discovery.
    __Input1_JComboBox.addItemListener ( this );
    __Input1_JComboBox.getJTextComponent().addKeyListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __Input1_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - first input column name."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Math operator:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Operator_JComboBox = new SimpleJComboBox ( 12, false );// Do not allow edit.
    __Operator_JComboBox.setData ( DataTableMath.getOperatorChoicesAsStrings() );
    __Operator_JComboBox.addItemListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __Operator_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - math calculation to perform on input."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input 2:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Input2_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit.
    __Input2_JComboBox.setToolTipText("Input column name or constant, can use ${Property} notation");
    List<String> input2Choices = new ArrayList<>();
    input2Choices.add("");
    __Input2_JComboBox.setData ( input2Choices ); // TODO SAM 2010-09-13 Need to populate via discovery.
    __Input2_JComboBox.addItemListener ( this );
    __Input2_JComboBox.getJTextComponent().addKeyListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __Input2_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - second input column name, or constant."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output column:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Output_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit.
    __Output_JComboBox.setToolTipText("Output column name, can use ${Property} notation");
    List<String> outputChoices = new ArrayList<>();
    outputChoices.add("");
    __Output_JComboBox.setData ( outputChoices ); // TODO SAM 2010-09-13 Need to populate via discovery.
    __Output_JComboBox.addItemListener ( this );
    __Output_JComboBox.getJTextComponent().addKeyListener ( this );
    //__Statistic_JComboBox.setMaximumRowCount(statisticChoices.size());
    JGUIUtil.addComponent(main_JPanel, __Output_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - output column name."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output type:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputType_JComboBox = new SimpleJComboBox ( 12, false );  // Do not allow edit.
    __OutputType_JComboBox.setToolTipText("Output column type. Use if automatic type determination is not correct");
    List<String> outputTypeChoices = TableField.getDataTypeChoices(false);
    outputTypeChoices.add(0,"");
    __OutputType_JComboBox.setData ( outputTypeChoices );
    __OutputType_JComboBox.addItemListener ( this );
    __OutputType_JComboBox.getJTextComponent().addKeyListener ( this );
    __OutputType_JComboBox.setMaximumRowCount(outputTypeChoices.size());
    JGUIUtil.addComponent(main_JPanel, __OutputType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - output column type (default=auto detect from input)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Non-value:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NonValue_JComboBox = new SimpleJComboBox ( 12, false );    // Allow edit.
    List<String> nonValueChoices = new ArrayList<>();
    nonValueChoices.add("");
    nonValueChoices.add(__command._NaN );
    nonValueChoices.add(__command._Null );
    __NonValue_JComboBox.setData ( nonValueChoices ); // TODO SAM 2010-09-13 Need to populate via discovery.
    __NonValue_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __NonValue_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel,
        new JLabel("Optional - non-value for missing, unable to compute (default=" + __command._Null + ")."),
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

	// Panel for buttons.
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
@param event event to handle.
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

/**
 * Handle key released event.
 * @param event event to handle.
 */
public void keyReleased ( KeyEvent event ) {
	refresh();
}

/**
 * Handle key typed event.
 * @param event event to handle.
 */
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
    String TableID = "";
    String Condition = "";
    String ProcessRows = "";
    String Input1 = "";
    String Input2 = "";
    String Operator = "";
    String Output = "";
    String OutputType = "";
    String NonValue = "";

	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
	    TableID = props.getValue ( "TableID" );
        Condition = props.getValue ( "Condition" );
        ProcessRows = props.getValue ( "ProcessRows" );
        Input1 = props.getValue ( "Input1" );
        Input2 = props.getValue ( "Input2" );
        Operator = props.getValue ( "Operator" );
		Output = props.getValue ( "Output" );
		OutputType = props.getValue ( "OutputType" );
		NonValue = props.getValue ( "NonValue" );
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
        if ( Condition != null ) {
            __Condition_JTextField.setText ( Condition );
        }
        if ( ProcessRows != null ) {
            __ProcessRows_JTextField.setText ( ProcessRows );
        }
        if ( Input1 == null ) {
            // Select default.
            __Input1_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Input1_JComboBox,Input1, JGUIUtil.NONE, null, null ) ) {
                __Input1_JComboBox.select ( Input1 );
            }
            else {
                // Just set the user-specified value.
                __Input1_JComboBox.setText( Input1 );
            }
        }
        if ( Operator == null ) {
            // Select default.
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
        if ( Input2 == null ) {
            // Select default.
            __Input2_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Input2_JComboBox,Input2, JGUIUtil.NONE, null, null ) ) {
                __Input2_JComboBox.select ( Input2 );
            }
            else {
                // Just set the user-specified value.
                __Input2_JComboBox.setText( Input2 );
            }
        }
        if ( Output == null ) {
            // Select default.
            __Output_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Output_JComboBox,Output, JGUIUtil.NONE, null, null ) ) {
                __Output_JComboBox.select ( Output );
            }
            else {
                // Just set the user-specified value.
                __Output_JComboBox.setText( Output );
            }
        }
        if ( OutputType == null ) {
            // Select default.
            __OutputType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __OutputType_JComboBox,OutputType, JGUIUtil.NONE, null, null ) ) {
                __OutputType_JComboBox.select ( OutputType );
            }
            else {
                // Just set the user-specified value.
                __OutputType_JComboBox.setText( OutputType );
            }
        }
        if ( NonValue == null ) {
            // Select default.
            __NonValue_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __NonValue_JComboBox,NonValue, JGUIUtil.NONE, null, null ) ) {
                __NonValue_JComboBox.select ( NonValue );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nNonValue value \"" + NonValue +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	// Regardless, reset the command from the fields.
	TableID = __TableID_JComboBox.getSelected();
    Condition = __Condition_JTextField.getText().trim();
    ProcessRows = __ProcessRows_JTextField.getText().trim();
	Input1 = __Input1_JComboBox.getSelected();
    Operator = __Operator_JComboBox.getSelected();
    Input2 = __Input2_JComboBox.getSelected();
    Output = __Output_JComboBox.getSelected();
    OutputType = __OutputType_JComboBox.getSelected();
    NonValue = __NonValue_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
    // Use the following to handle equal sign in the property.
    props.set ( "Condition", Condition );
    props.set ( "ProcessRows", ProcessRows );
    props.add ( "Input1=" + Input1 );
    props.add ( "Operator=" + Operator );
    props.add ( "Input2=" + Input2 );
    props.add ( "Output=" + Output );
    props.add ( "OutputType=" + OutputType );
    props.add ( "NonValue=" + NonValue );
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
			// Not ready to close.
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