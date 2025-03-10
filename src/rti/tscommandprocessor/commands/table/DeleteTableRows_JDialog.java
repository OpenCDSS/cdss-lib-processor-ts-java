// DeleteTableRows_JDialog - editor dialog for DeleteTableRows command

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
import java.util.List;

//import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class DeleteTableRows_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private boolean __error_wait = false; // To track errors.
private boolean __first_time = true; // Indicate first time display.
private JTextArea __command_JTextArea = null;
private JTabbedPane __param_JTabbedPane = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __Condition_JTextField = null;
private JTextField __DeleteRowNumbers_JTextField = null;
private JTextField __First_JTextField = null;
private JTextField __Last_JTextField = null;
private JTextField __DeleteCountProperty_JTextField = null;
private JTextField __RowCountProperty_JTextField = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private DeleteTableRows_Command __command = null;
//private JFrame __parent = null;
private boolean __ok = false;

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of table identifiers to provide as choices
*/
public DeleteTableRows_JDialog ( JFrame parent, DeleteTableRows_Command command, List<String> tableIDChoices ) {
	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed(ActionEvent event) {
	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "DeleteTableRows");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			// Command has been edited.
			response ( true );
		}
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Create a list of parameters to check.
	PropList props = new PropList ( "" );
	String TableID = __TableID_JComboBox.getSelected();
	String Condition = __Condition_JTextField.getText().trim();
	String DeleteRowNumbers = __DeleteRowNumbers_JTextField.getText().trim();
	String First = __First_JTextField.getText().trim();
	String Last = __Last_JTextField.getText().trim();
	String DeleteCountProperty = __DeleteCountProperty_JTextField.getText().trim();
	String RowCountProperty = __RowCountProperty_JTextField.getText().trim();
	__error_wait = false;

    if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( Condition.length() > 0 ) {
        props.set ( "Condition", Condition );
    }
    if ( DeleteRowNumbers.length() > 0 ) {
        props.set ( "DeleteRowNumbers", DeleteRowNumbers );
    }
    if ( First.length() > 0 ) {
        props.set ( "First", First );
    }
    if ( Last.length() > 0 ) {
        props.set ( "Last", Last );
    }
    if ( DeleteCountProperty.length() > 0 ) {
        props.set ( "DeleteCountProperty", DeleteCountProperty );
    }
    if ( RowCountProperty.length() > 0 ) {
        props.set ( "RowCountProperty", RowCountProperty );
    }
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
        Message.printWarning(2,"", e);
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String TableID = __TableID_JComboBox.getSelected();
    String Condition = __Condition_JTextField.getText().trim();
    String DeleteRowNumbers = __DeleteRowNumbers_JTextField.getText().trim();
	String First = __First_JTextField.getText().trim();
	String Last = __Last_JTextField.getText().trim();
	String DeleteCountProperty = __DeleteCountProperty_JTextField.getText().trim();
	String RowCountProperty = __RowCountProperty_JTextField.getText().trim();
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "Condition", Condition );
    __command.setCommandParameter ( "DeleteRowNumbers", DeleteRowNumbers );
    __command.setCommandParameter ( "First", First );
    __command.setCommandParameter ( "Last", Last );
    __command.setCommandParameter ( "DeleteCountProperty", DeleteCountProperty );
	__command.setCommandParameter ( "RowCountProperty", RowCountProperty );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit and possibly run.
*/
private void initialize ( JFrame parent, DeleteTableRows_Command command, List<String> tableIDChoices ) {
	//__parent = parent;
    __command = command;

	addWindowListener(this);

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout(new GridBagLayout());
	getContentPane().add ("North", main_JPanel);
	int y = -1;

	JPanel paragraph = new JPanel();
	paragraph.setLayout(new GridBagLayout());
	int yy = -1;

   	JGUIUtil.addComponent(paragraph, new JLabel ( "This command deletes rows from a table."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(paragraph, new JLabel (
        "Specify the rows to delete using a condition or by specifying row numbers."),
        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	JGUIUtil.addComponent(main_JPanel, paragraph,
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit.
    __TableID_JComboBox.setToolTipText("Specify the table ID for the table to modify, can use ${Property}");
    tableIDChoices.add(0,""); // Add blank to ignore table.
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.getJTextComponent().addKeyListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - table to process."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Create a tabbed pane for ways to indicate rows to delete.
    __param_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __param_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for deleting rows by condition.
    int yCond = -1;
    JPanel cond_JPanel = new JPanel();
    cond_JPanel.setLayout( new GridBagLayout() );
    __param_JTabbedPane.addTab ( "Condition", cond_JPanel );

   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "Specify a condition to match rows to delete using syntax:"),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "    ColumnName Operator Value"),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "where:"),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "    'ColumnName' can be a column name or ${Property}."),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "    'Operator' can be:  <, <=, >, >=, ==, !="),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "    'Operator' for strings also can be:  contains, !contains, isempty, !isempty"),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "    'Operator' for all types to check for null values can be:  isempty"),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "    String comparisons are case-specific and a null value is treated as an empty string."),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "    'Value' should agree with column type, for example don't specify text characters for numeric column type."),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(cond_JPanel, new JLabel (
        "Currently only one condition can be evaluated.  Use multiple commands to evaluate multiple independent conditions."),
        0, ++yCond, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	JGUIUtil.addComponent(cond_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yCond, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(cond_JPanel, new JLabel("Condition:"),
        0, ++yCond, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Condition_JTextField = new JTextField ( 50 );
    __Condition_JTextField.setToolTipText("Specify condition to evaluate for each row.  If true, delete the row.");
    __Condition_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(cond_JPanel, __Condition_JTextField,
        1, yCond, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(cond_JPanel, new JLabel ( "Optional - condition to match rows." ),
        3, yCond, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for deleting rows by number.
    int yRowNum = -1;
    JPanel rowNum_JPanel = new JPanel();
    rowNum_JPanel.setLayout( new GridBagLayout() );
    __param_JTabbedPane.addTab ( "Row Number", rowNum_JPanel );

   	JGUIUtil.addComponent(rowNum_JPanel, new JLabel (
        "Row number can be specified as value 1+, * for all rows, or \"last\"."),
        0, ++yRowNum, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(rowNum_JPanel, new JLabel (
        "Separate multiple row values by commas."),
        0, ++yRowNum, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	JGUIUtil.addComponent(rowNum_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yRowNum, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(rowNum_JPanel, new JLabel("Delete row numbers:"),
        0, ++yRowNum, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteRowNumbers_JTextField = new JTextField ( "", 25 );
    __DeleteRowNumbers_JTextField.setToolTipText("Specify row number (1+) to delete, separated by commas, can use ${Property}, * to delete all, or use \"last\" to delete the last row.");
    __DeleteRowNumbers_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(rowNum_JPanel, __DeleteRowNumbers_JTextField,
        1, yRowNum, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(rowNum_JPanel, new JLabel ( "Optional - row numbers to delete (default=none)." ),
        3, yRowNum, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(rowNum_JPanel, new JLabel("Delete first rows:"),
        0, ++yRowNum, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __First_JTextField = new JTextField ( "", 10 );
    __First_JTextField.setToolTipText("Postive N deletes the first N rows, -N deletes all but the first N rows.");
    __First_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(rowNum_JPanel, __First_JTextField,
        1, yRowNum, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(rowNum_JPanel, new JLabel ( "Optional - first rows to delete (default=none)." ),
        3, yRowNum, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(rowNum_JPanel, new JLabel("Delete last rows:"),
        0, ++yRowNum, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Last_JTextField = new JTextField ( "", 10 );
    __Last_JTextField.setToolTipText("Postive N deletes the last N rows, -N deletes all but the last N rows.");
    __Last_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(rowNum_JPanel, __Last_JTextField,
        1, yRowNum, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(rowNum_JPanel, new JLabel ( "Optional - last rows to delete (default=none)." ),
        3, yRowNum, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for output properties.
    int yProps = -1;
    JPanel props_JPanel = new JPanel();
    props_JPanel.setLayout( new GridBagLayout() );
    __param_JTabbedPane.addTab ( "Output Properties", props_JPanel );

   	JGUIUtil.addComponent(props_JPanel, new JLabel (
        "The number of rows deleted and final table row count can be set as processor properties."),
        0, ++yProps, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	JGUIUtil.addComponent(props_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yProps, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(props_JPanel, new JLabel("Delete count property:"),
        0, ++yProps, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DeleteCountProperty_JTextField = new JTextField ( "", 25 );
    __DeleteCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(props_JPanel, __DeleteCountProperty_JTextField,
        1, yProps, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel ( "Optional - processor property to set as number of rows deleted." ),
        3, yProps, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(props_JPanel, new JLabel("Row count property:"),
        0, ++yProps, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RowCountProperty_JTextField = new JTextField ( "", 25 );
    __RowCountProperty_JTextField.setToolTipText("Specify the property name for the copied table row count, can use ${Property} notation");
    __RowCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(props_JPanel, __RowCountProperty_JTextField,
        1, yProps, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(props_JPanel, new JLabel ( "Optional - processor property to set as output table row count." ),
        3, yProps, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,40);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable (false);
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents.
	refresh ();

	// Panel for buttons.
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add (__ok_JButton);
	__ok_JButton.setToolTipText ( "Close window and save changes to command." );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add (__cancel_JButton);
	__cancel_JButton.setToolTipText ( "Close window without saving changes." );
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command");
    pack();
    JGUIUtil.center(this);
	refresh();	// Sets the __path_JButton status.
	setResizable (false);
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

public void keyTyped (KeyEvent event) {
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
    String TableID = "";
    String Condition = "";
    String DeleteRowNumbers = "";
    String First = "";
    String Last = "";
    String DeleteCountProperty = "";
    String RowCountProperty = "";
	PropList props = __command.getCommandParameters();
	if (__first_time) {
		__first_time = false;
        TableID = props.getValue ( "TableID" );
        Condition = props.getValue ( "Condition" );
        DeleteRowNumbers = props.getValue ( "DeleteRowNumbers" );
        First = props.getValue ( "First" );
        Last = props.getValue ( "Last" );
        DeleteCountProperty = props.getValue ( "DeleteCountProperty" );
        RowCountProperty = props.getValue ( "RowCountProperty" );
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
            if ( !Condition.isEmpty() ) {
            	__param_JTabbedPane.setSelectedIndex(0);
            }
        }
        if ( DeleteRowNumbers != null ) {
            __DeleteRowNumbers_JTextField.setText ( DeleteRowNumbers );
            if ( !DeleteRowNumbers.isEmpty() ) {
            	__param_JTabbedPane.setSelectedIndex(1);
            }
        }
        if ( First != null ) {
            __First_JTextField.setText ( First );
            if ( !First.isEmpty() ) {
            	__param_JTabbedPane.setSelectedIndex(1);
            }
        }
        if ( Last != null ) {
            __Last_JTextField.setText ( Last );
            if ( !Last.isEmpty() ) {
            	__param_JTabbedPane.setSelectedIndex(1);
            }
        }
        if ( DeleteCountProperty != null ) {
            __DeleteCountProperty_JTextField.setText ( DeleteCountProperty );
        }
        if ( RowCountProperty != null ) {
            __RowCountProperty_JTextField.setText ( RowCountProperty );
        }
	}
	// Regardless, reset the command from the fields.
	TableID = __TableID_JComboBox.getSelected();
    Condition = __Condition_JTextField.getText().trim();
    DeleteRowNumbers = __DeleteRowNumbers_JTextField.getText().trim();
    First = __First_JTextField.getText().trim();
    Last = __Last_JTextField.getText().trim();
    DeleteCountProperty = __DeleteCountProperty_JTextField.getText().trim();
    RowCountProperty = __RowCountProperty_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
    props.add ( "TableID=" + TableID );
    // Use the following to handle equal sign in the property.
    props.set ( "Condition", Condition );
    props.add ( "DeleteRowNumbers=" + DeleteRowNumbers );
    props.add ( "First=" + First );
    props.add ( "Last=" + Last );
    props.add ( "DeleteCountProperty=" + DeleteCountProperty );
    props.add ( "RowCountProperty=" + RowCountProperty );
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
public void windowClosing(WindowEvent event) {
	response ( false );
}

// The following methods are all necessary because this class implements WindowListener.
public void windowActivated(WindowEvent evt) {
}

public void windowClosed(WindowEvent evt) {
}

public void windowDeactivated(WindowEvent evt) {
}

public void windowDeiconified(WindowEvent evt) {
}

public void windowIconified(WindowEvent evt) {
}

public void windowOpened(WindowEvent evt) {
}

}