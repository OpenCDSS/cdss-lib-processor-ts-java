// NewPatternTimeSeries_JDialog - Editor for the NewPatternTimeSeries() command.

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

package rti.tscommandprocessor.commands.ts;

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

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;

/**
Editor for the NewPatternTimeSeries() command.
*/
@SuppressWarnings("serial")
public class NewPatternTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JFrame __parent_JFrame = null;
private NewPatternTimeSeries_Command __command = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextArea __NewTSID_JTextArea=null;
private SimpleJButton __edit_JButton = null;
private SimpleJButton __clear_JButton = null;
private SimpleJComboBox __IrregularInterval_JComboBox = null;
private JTextField __Description_JTextField = null;
private JTextField __SetStart_JTextField = null;
private JTextField __SetEnd_JTextField = null;
private JTextField __Units_JTextField = null;
private JTextField __MissingValue_JTextField = null;
private JTextArea __PatternValues_JTextArea=null;
private JTextArea __PatternFlags_JTextArea=null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK has been pressed.

/**
newPatternTimeSeries_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public NewPatternTimeSeries_JDialog ( JFrame parent, NewPatternTimeSeries_Command command ) {
	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	String routine = getClass().getSimpleName() + ".actionPerformed";
	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __clear_JButton ) {
		__NewTSID_JTextArea.setText ( "" );
	}
	else if ( o == __edit_JButton ) {
		// Edit the NewTSID in the dialog.  It is OK for the string to be blank.
		String NewTSID = __NewTSID_JTextArea.getText().trim();
		TSIdent tsident;
		try {
		    if ( NewTSID.length() == 0 ) {
				tsident = new TSIdent();
			}
			else {
			    tsident = new TSIdent ( NewTSID );
			}
			TSIdent tsident2=(new TSIdent_JDialog ( __parent_JFrame, true, tsident, null )).response();
			if ( tsident2 != null ) {
				__NewTSID_JTextArea.setText ( tsident2.toString(true) );
				refresh();
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error creating time series identifier from \"" + NewTSID + "\"." );
			Message.printWarning ( 3, routine, e );
		}
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "NewPatternTimeSeries");
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
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e ) {
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e ) {
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the UI state and adjust settings based on user selections.
*/
private void checkUIState() {
    String NewTSID = __NewTSID_JTextArea.getText().trim().toUpperCase();
    if ( NewTSID.indexOf("IRR") >= 0 ) {
        // Irregular time series.
        __IrregularInterval_JComboBox.setEnabled(true);
    }
    else {
        __IrregularInterval_JComboBox.setEnabled(false);
    }
}

/**
Check the input.
If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String Alias = __Alias_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String IrregularInterval = __IrregularInterval_JComboBox.getSelected();
	String Description = __Description_JTextField.getText().trim();
	String SetStart = __SetStart_JTextField.getText().trim();
	String SetEnd = __SetEnd_JTextField.getText().trim();
	String Units = __Units_JTextField.getText().trim();
	String MissingValue = __MissingValue_JTextField.getText().trim();
	String PatternValues = __PatternValues_JTextArea.getText().trim();
	String PatternFlags = __PatternFlags_JTextArea.getText().trim();
	__error_wait = false;

	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		props.set ( "NewTSID", NewTSID );
	}
	if ( (IrregularInterval != null) && (IrregularInterval.length() > 0) ) {
	    props.set ( "IrregularInterval", IrregularInterval );
	}
	if ( (Description != null) && (Description.length() > 0) ) {
		props.set ( "Description", Description );
	}
	if ( SetStart.length() > 0 ) {
		props.set ( "SetStart", SetStart );
	}
	if ( SetEnd.length() > 0 ) {
		props.set ( "SetEnd", SetEnd );
	}
	if ( Units.length() > 0 ) {
		props.set ( "Units", Units );
	}
    if ( MissingValue.length() > 0 ) {
        props.set ( "MissingValue", MissingValue );
    }
	if ( (PatternValues != null) && (PatternValues.length() > 0) ) {
		props.set ( "PatternValues", PatternValues );
	}
    if ( (PatternFlags != null) && (PatternFlags.length() > 0) ) {
        props.set ( "PatternFlags", PatternFlags );
    }
	try {
		// This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
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
	String Alias = __Alias_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String IrregularInterval = __IrregularInterval_JComboBox.getSelected();
	String Description = __Description_JTextField.getText().trim();
	String SetStart = __SetStart_JTextField.getText().trim();
	String SetEnd = __SetEnd_JTextField.getText().trim();
	String Units = __Units_JTextField.getText().trim();
	String MissingValue = __MissingValue_JTextField.getText().trim();
	String PatternValues = __PatternValues_JTextArea.getText().trim();
	String PatternFlags = __PatternFlags_JTextArea.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "IrregularInterval", IrregularInterval );
	__command.setCommandParameter ( "Description", Description );
	__command.setCommandParameter ( "SetStart", SetStart );
	__command.setCommandParameter ( "SetEnd", SetEnd );
	__command.setCommandParameter ( "Units", Units );
	__command.setCommandParameter ( "MissingValue", MissingValue );
	__command.setCommandParameter ( "PatternValues", PatternValues );
	__command.setCommandParameter ( "PatternFlags", PatternFlags );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, NewPatternTimeSeries_Command command ) {
	__parent_JFrame = parent;
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a new time series, which can be referenced using the alias or TSID, using a repeating pattern of values."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify period start and end date/times using a precision consistent with the data interval."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If the start and end for the period are not set, then a SetOutputPeriod() command must be specified before the command."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If the time series has an interval of irregular, provide the interval to define data (more options" +
        " for irregular data may be added later)."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(20);
    __Alias_JTextField.getTextField().setToolTipText("Specify alias using % format specifiers, ${ts:Property} or ${Property} notation");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time series ID:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTSID_JTextArea = new JTextArea ( 3, 25 );
	__NewTSID_JTextArea.setToolTipText("Specify new time series ID, can include ${Property} notation for TSID parts");
	__NewTSID_JTextArea.setEditable ( false );
	__NewTSID_JTextArea.setLineWrap ( true );
	__NewTSID_JTextArea.setWrapStyleWord ( true );
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button.
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, y, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required - specify unique TSID information to define time series."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	y += 2;
    JGUIUtil.addComponent(main_JPanel, (__edit_JButton = new SimpleJButton ( "Edit", "Edit", this ) ),
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, (__clear_JButton = new SimpleJButton ( "Clear", "Clear", this ) ),
		4, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Interval for irregular time series:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IrregularInterval_JComboBox = new SimpleJComboBox ( false );
    boolean padZeroes = true;
    boolean includeIrregular = false;
    List<String> intervalChoices = TimeInterval.getTimeIntervalChoices(
        TimeInterval.MINUTE, TimeInterval.YEAR, padZeroes, 1, includeIrregular);
    // Add a blank.
    intervalChoices.add(0,"");
    __IrregularInterval_JComboBox.setData ( intervalChoices );
    __IrregularInterval_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __IrregularInterval_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required for irregular time series - used to initialize data."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Description/name:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Description_JTextField = new JTextField ( "", 20 );
	__Description_JTextField.setToolTipText("Specify description or use ${Property} notation");
	JGUIUtil.addComponent(main_JPanel, __Description_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__Description_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - description for time series."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Start:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetStart_JTextField = new JTextField ( "", 20 );
	__SetStart_JTextField.setToolTipText("Specify the set start using a date/time string or ${Property} notation");
	__SetStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __SetStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
            "Optional - starting date/time for data (default=global start)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "End:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetEnd_JTextField = new JTextField ( "", 20 );
	__SetEnd_JTextField.setToolTipText("Specify the set end using a date/time string or ${Property} notation");
	__SetEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __SetEnd_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
            "Optional - ending date/time for data (default=global end)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data units:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Units_JTextField = new JTextField ( "", 10 );
	__Units_JTextField.setToolTipText("Specify the data units or use ${Property} notation");
	JGUIUtil.addComponent(main_JPanel, __Units_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__Units_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - for example:  ACFT, CFS, IN."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Missing value:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField ( "", 20 );
    __MissingValue_JTextField.setToolTipText("Specify the missing value number, NaN, or use ${Property} notation");
    JGUIUtil.addComponent(main_JPanel, __MissingValue_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __MissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - missing data value (default=-999, recommended=NaN)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Pattern values:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PatternValues_JTextArea = new JTextArea (4,60);
	__PatternValues_JTextArea.setLineWrap ( true );
	__PatternValues_JTextArea.setWrapStyleWord ( true );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__PatternValues_JTextArea),
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	__PatternValues_JTextArea.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, new JLabel("Required - separate by spaces or commas, blank for missing."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Pattern flags:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PatternFlags_JTextArea = new JTextArea (4,60);
    __PatternFlags_JTextArea.setLineWrap ( true );
    __PatternFlags_JTextArea.setWrapStyleWord ( true );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__PatternFlags_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    __PatternFlags_JTextArea.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - annotates data (default=no flags)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,60);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents.
    checkUIState();
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
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
	checkUIState();
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( (code == KeyEvent.VK_ENTER) || (code == KeyEvent.VK_TAB) ) {
		refresh();
	}
	if ( code == KeyEvent.VK_ENTER ) {
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
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
	String Alias = "";
	String NewTSID = "";
	String IrregularInterval = "";
	String Description = "";
	String SetStart = "";
	String SetEnd = "";
	String Units = "";
	String MissingValue = "";
	String PatternValues = "";
	String PatternFlags = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		Alias = props.getValue ( "Alias" );
		NewTSID = props.getValue ( "NewTSID" );
		IrregularInterval = props.getValue ( "IrregularInterval" );
		Description = props.getValue ( "Description" );
		SetStart = props.getValue ( "SetStart" );
		SetEnd = props.getValue ( "SetEnd" );
		Units = props.getValue ( "Units" );
		MissingValue = props.getValue ( "MissingValue" );
		PatternValues = props.getValue ( "PatternValues" );
		PatternFlags = props.getValue ( "PatternFlags" );
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
		if ( NewTSID != null ) {
			__NewTSID_JTextArea.setText ( NewTSID );
		}
        if ( JGUIUtil.isSimpleJComboBoxItem( __IrregularInterval_JComboBox, IrregularInterval, JGUIUtil.NONE, null, -1, null, true ) ) {
            __IrregularInterval_JComboBox.select ( IrregularInterval );
        }
        else {
            // Automatically add to the list after the blank (might be a multiple).
            if ( (IrregularInterval != null) && !IrregularInterval.isEmpty() ) {
                __IrregularInterval_JComboBox.insertItemAt ( IrregularInterval, 1 );
                // Select...
                __IrregularInterval_JComboBox.select ( IrregularInterval );
            }
            else {
                // Select the blank.
                __IrregularInterval_JComboBox.select ( 0 );
            }
        }
		if ( Description != null ) {
			__Description_JTextField.setText ( Description );
		}
		if ( SetStart != null ) {
			__SetStart_JTextField.setText( SetStart );
		}
		if ( SetEnd != null ) {
			__SetEnd_JTextField.setText ( SetEnd );
		}
		if ( Units != null ) {
			__Units_JTextField.setText( Units );
		}
        if ( MissingValue != null ) {
            __MissingValue_JTextField.setText( MissingValue );
        }
		if ( PatternValues != null ) {
			__PatternValues_JTextArea.setText ( PatternValues );
		}
        if ( PatternFlags != null ) {
            __PatternFlags_JTextArea.setText ( PatternFlags );
        }
	}
	// Regardless, reset the command from the fields.
	checkUIState(); // Enable/disable irregular interval.
	Alias = __Alias_JTextField.getText().trim();
	NewTSID = __NewTSID_JTextArea.getText().trim();
	IrregularInterval = __IrregularInterval_JComboBox.getSelected();
	Description = __Description_JTextField.getText().trim();
	SetStart = __SetStart_JTextField.getText().trim();
	SetEnd = __SetEnd_JTextField.getText().trim();
	Units = __Units_JTextField.getText().trim();
	MissingValue = __MissingValue_JTextField.getText().trim();
	PatternValues = __PatternValues_JTextArea.getText().trim();
	PatternFlags = __PatternFlags_JTextArea.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "Alias=" + Alias );
	props.add ( "NewTSID=" + NewTSID );
	props.add ( "IrregularInterval=" + IrregularInterval );
	props.add ( "Description=" + Description );
	props.add ( "SetStart=" + SetStart );
	props.add ( "SetEnd=" + SetEnd );
	props.add ( "Units=" + Units );
	props.add ( "MissingValue=" + MissingValue );
	props.add ( "PatternValues=" + PatternValues );
	props.add ( "PatternFlags=" + PatternFlags );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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