// ARMA_JDialog - Editor for ARMA() command.

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
Editor for ARMA() command.
*/
@SuppressWarnings("serial")
public class ARMA_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ARMA_Command __command = null;
private JTextArea __command_JTextArea = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTextField __a_JTextField = null;
private JTextField __b_JTextField = null;
private SimpleJComboBox	__RequireCoefficientsSumTo1_JComboBox = null;
private SimpleJComboBox __ARMAInterval_JComboBox = null;
private JTextField __InputPreviousValues_JTextField = null;
private JTextField __OutputPreviousValues_JTextField = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextArea __NewTSID_JTextArea = null;
private SimpleJButton __edit_JButton = null;
private SimpleJButton __clear_JButton = null;
private JTextField __Description_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private JTextField __OutputMinimum_JTextField = null;
private JTextField __OutputMaximum_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.
private JFrame __parent_JFrame = null;

/**
ARMA_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ARMA_JDialog ( JFrame parent, ARMA_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();
	String routine = getClass().getSimpleName() + ".actionPerformed";

	if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __clear_JButton ) {
		__NewTSID_JTextArea.setText ( "" );
        refresh();
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
				__NewTSID_JTextArea.setText (tsident2.toString(true) );
				refresh();
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 1, routine, "Error creating time series identifier from \"" + NewTSID + "\"." );
			Message.printWarning ( 3, routine, e );
		}
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ARMA");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __clear_JButton ) {
		__NewTSID_JTextArea.setText ( "" );
        refresh();
	}
	else {
	    // Change in choice
	    refresh();
	}
}

//Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{
 refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{
 refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{
 refresh();
}

//...End event handlers for DocumentListener

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
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String ARMAInterval = __ARMAInterval_JComboBox.getSelected();
    String a = __a_JTextField.getText().trim();
    String b = __b_JTextField.getText().trim();
    String RequireCoefficientsSumTo1 = __RequireCoefficientsSumTo1_JComboBox.getSelected().trim();
    String InputPreviousValues = __InputPreviousValues_JTextField.getText().trim();
    String OutputPreviousValues = __OutputPreviousValues_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Description = __Description_JTextField.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String OutputMinimum = __OutputMinimum_JTextField.getText().trim();
    String OutputMaximum = __OutputMaximum_JTextField.getText().trim();
    
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
    if ( ARMAInterval.length() > 0 ) {
        parameters.set ( "ARMAInterval", ARMAInterval );
    }
    if ( a.length() > 0 ) {
        parameters.set ( "a", a );
    }
    if ( b.length() > 0 ) {
        parameters.set ( "b", b );
    }
    if ( RequireCoefficientsSumTo1.length() > 0 ) {
        parameters.set ( "RequireCoefficientsSumTo1", RequireCoefficientsSumTo1 );
    }
    if ( InputPreviousValues.length() > 0 ) {
        parameters.set ( "InputPreviousValues", InputPreviousValues );
    }
    if ( OutputPreviousValues.length() > 0 ) {
        parameters.set ( "OutputPreviousValues", OutputPreviousValues );
    }
	if ( Alias.length() > 0 ) {
		parameters.set ( "Alias", Alias );
	}
	if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
		parameters.set ( "NewTSID", NewTSID );
	}
	if ( (Description != null) && (Description.length() > 0) ) {
		parameters.set ( "Description", Description );
	}
	if ( OutputStart.length() > 0 ) {
		parameters.set ( "OutputStart", OutputStart );
	}
	if ( OutputEnd.length() > 0 ) {
		parameters.set ( "OutputEnd", OutputEnd );
	}
    if ( OutputMinimum.length() > 0 ) {
        parameters.set ( "OutputMinimum", OutputMinimum );
    }
    if ( OutputMaximum.length() > 0 ) {
        parameters.set ( "OutputMaximum", OutputMaximum );
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
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();   
    String ARMAInterval = __ARMAInterval_JComboBox.getSelected();
    String a = __a_JTextField.getText().trim();
    String b = __b_JTextField.getText().trim();
    String RequireCoefficientsSumTo1 = __RequireCoefficientsSumTo1_JComboBox.getSelected().trim();
    String InputPreviousValues = __InputPreviousValues_JTextField.getText().trim();
    String OutputPreviousValues = __OutputPreviousValues_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
	String Description = __Description_JTextField.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String OutputMinimum = __OutputMinimum_JTextField.getText().trim();
    String OutputMaximum = __OutputMaximum_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "ARMAInterval", ARMAInterval );
    __command.setCommandParameter ( "a", a );
    __command.setCommandParameter ( "b", b );
    __command.setCommandParameter ( "RequireCoefficientsSumTo1", RequireCoefficientsSumTo1 );
    __command.setCommandParameter ( "InputPreviousValues", InputPreviousValues );
    __command.setCommandParameter ( "OutputPreviousValues", OutputPreviousValues );
	__command.setCommandParameter ( "Alias", Alias );
	__command.setCommandParameter ( "NewTSID", NewTSID );
	__command.setCommandParameter ( "Description", Description );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
    __command.setCommandParameter ( "OutputMinimum", OutputMinimum );
    __command.setCommandParameter ( "OutputMaximum", OutputMaximum );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ARMA_Command command )
{	__command = command;
	__parent_JFrame = parent;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Apply the ARMA (AutoRegressive Moving Average) method to predict future values from past values." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"An example of ARMA application is to lag and attenuate a time series." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The output time series O is computed from the original input time series I using:"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    O[t] = a_1*O[t-1] + a_2*O[t-2] + ... + a_p*O[t-p] + b_0*I[t] + b_1*I[t-1] + ... + b_q*I[t-q]" ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"where t = time, p = number of previous output values to consider, and q = number of previous input values to consider."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for input
    int yInput = -1;
    JPanel input_JPanel = new JPanel();
    input_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input", input_JPanel );

    JGUIUtil.addComponent(input_JPanel, new JLabel (
		"Specify the input time series to process.  The input time series will be modified unless a NewTSID parameter is specified for output (see \"Output\" tab)."),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(input_JPanel, new JLabel (
		"The output value is set to missing if one or more input values are missing "
		+ "(typically only filled data should be used).  The period will not automatically be extended."),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(input_JPanel, new JLabel (
		"Specify previous input time series values to avoid missing values in output "
		+ "(leftmost value is earliest in time, rightmost is value for interval prior to input time series start)."),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(input_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yInput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    yInput = CommandEditorUtil.addTSListToEditorDialogPanel ( this, input_JPanel, __TSList_JComboBox, yInput );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInput = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, input_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yInput );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select a time series ensemble ID from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInput = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, input_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yInput );
    
    JGUIUtil.addComponent(input_JPanel, new JLabel ( "Input previous values:" ), 
		0, ++yInput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputPreviousValues_JTextField = new JTextField ( 35 );
	__InputPreviousValues_JTextField.setToolTipText("Specify values separated by commas, earliest value first, can use ${Property}.");
	__InputPreviousValues_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(input_JPanel, __InputPreviousValues_JTextField,
		1, yInput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(input_JPanel, new JLabel(
        "Optional - input previous values (default - limited by input time series)."), 
        3, yInput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for ARMA parameters
    int yARMA = -1;
    JPanel ARMA_JPanel = new JPanel();
    ARMA_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "ARMA", ARMA_JPanel );
    
    JGUIUtil.addComponent(ARMA_JPanel, new JLabel (
		"ARMA a and b coefficients must be computed externally.  The values for p and q will be determined from the number of coefficients."),
		0, ++yARMA, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ARMA_JPanel, new JLabel (
		"Specify the order of the coefficients as time t-1, t-2, etc."),
		0, ++yARMA, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ARMA_JPanel, new JLabel (
		"The ARMA interval must be <= the time series interval.  If the ARMA interval is different from the input time series each input time series value is repeated."),
		0, ++yARMA, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ARMA_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yARMA, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ARMA_JPanel, new JLabel ( "ARMA interval:" ), 
        0, ++yARMA, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ARMAInterval_JComboBox = new SimpleJComboBox ( true ); // Allow to be editable so property notation can be used
    __ARMAInterval_JComboBox.setToolTipText("Specify ARMA interval, can use ${Property}.");
    __ARMAInterval_JComboBox.setData (TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE, TimeInterval.YEAR,false,-1));
    __ARMAInterval_JComboBox.select(0);
    __ARMAInterval_JComboBox.addItemListener ( this );
    __ARMAInterval_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(ARMA_JPanel, __ARMAInterval_JComboBox,
        1, yARMA, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ARMA_JPanel, new JLabel("Required (e.g., 2Hour, 15Minute)."),
        3, yARMA, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ARMA_JPanel, new JLabel ( "\"a\" coefficients:" ), 
		0, ++yARMA, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__a_JTextField = new JTextField ( 35 );
	__a_JTextField.setToolTipText("Specify coefficients for t-1, t-2, etc. separated by commas, can use ${Property}.");
	__a_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ARMA_JPanel, __a_JTextField,
		1, yARMA, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ARMA_JPanel, new JLabel(
        "Required - \"a\" coefficients to multiply output values."), 
        3, yARMA, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ARMA_JPanel, new JLabel ( "\"b\" coefficients:" ), 
		0, ++yARMA, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__b_JTextField = new JTextField ( 35 );
	__b_JTextField.setToolTipText("Specify coefficients for t, t-1, t-2, etc. separated by commas, can use ${Property}.");
	__b_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(ARMA_JPanel, __b_JTextField,
		1, yARMA, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ARMA_JPanel, new JLabel(
        "Required - \"b\" coefficients to multiply input values."), 
        3, yARMA, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(ARMA_JPanel, new JLabel("Require coefficients to sum to 1:"),
		0, ++yARMA, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	List<String> bChoices = new ArrayList<String>(3);
	bChoices.add("");
	bChoices.add(__command._False);
	bChoices.add(__command._True);
	__RequireCoefficientsSumTo1_JComboBox = new SimpleJComboBox(bChoices);
	__RequireCoefficientsSumTo1_JComboBox.select(0);
	__RequireCoefficientsSumTo1_JComboBox.addActionListener(this);
	JGUIUtil.addComponent(ARMA_JPanel, __RequireCoefficientsSumTo1_JComboBox,
		1, yARMA, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(ARMA_JPanel, new JLabel (
		"Optional - require \"a\" and \"b\" coefficients to sum to 1 (default=" + __command._True + ")."),
		3, yARMA, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for output parameters
    int yOutput = -1;
    JPanel output_JPanel = new JPanel();
    output_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", output_JPanel );

    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"Specify a new TSID and optionally alias to create a new output time series - otherwise original input time series will be modified."),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"The output period will default to the input time series period unless specified below."),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"Specify output time series previous values to avoid missing values in output "
		+ "(leftmost value is earliest in time, rightmost is value for interval prior to input start)."),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"Output previous values will be considered in ARMA calculations."),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"The output minimum and maximum value checks occur during the ARMA calculations and will impact subsequent output values."),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "New time series ID:" ),
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTSID_JTextArea = new JTextArea ( 3, 25 );
	__NewTSID_JTextArea.setToolTipText("Specify new time series ID, can include ${Property} notation for TSID parts");
	__NewTSID_JTextArea.setEditable(false); // Force users to use the custom editor
	__NewTSID_JTextArea.setLineWrap ( true );
	__NewTSID_JTextArea.setWrapStyleWord ( true );
	__NewTSID_JTextArea.addKeyListener ( this );
	// Make 3-high to fit in the edit button...
    JGUIUtil.addComponent(output_JPanel, new JScrollPane(__NewTSID_JTextArea),
		1, yOutput, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel("Required if new output time series - unique TSID."), 
		3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    yOutput += 2;
    JGUIUtil.addComponent(output_JPanel, (__edit_JButton =
		new SimpleJButton ( "Edit", "Edit", this ) ),
		3, yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(output_JPanel, (__clear_JButton =
		new SimpleJButton ( "Clear", "Clear", this ) ),
		4, yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(output_JPanel, new JLabel("Alias to assign:"),
        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.getTextField().setToolTipText("Specify alias using % format specifiers, ${ts:Property} or ${Property} notation");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(output_JPanel, __Alias_JTextField,
        1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Optional if new output time series - use %L for location, etc."),
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Description/Name:" ), 
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Description_JTextField = new JTextField ( "", 10 );
	__Description_JTextField.setToolTipText("Specify description or use ${Property} notation");
	JGUIUtil.addComponent(output_JPanel, __Description_JTextField,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__Description_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, new JLabel(
        "Optional - description for time series."),
        3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(output_JPanel, new JLabel ("Output start:"), 
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputStart_JTextField = new JTextField (20);
	__OutputStart_JTextField.setToolTipText("Specify the output start using a date/time string or ${Property} notation");
	__OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(output_JPanel, __OutputStart_JTextField,
		1, yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"Optional - output period start (default=input period)."),
		3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output end:"), 
		0, ++yOutput, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputEnd_JTextField = new JTextField (20);
	__OutputEnd_JTextField.setToolTipText("Specify the output end using a date/time string or ${Property} notation");
	__OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(output_JPanel, __OutputEnd_JTextField,
		1, yOutput, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel (
		"Optional - output period end (default=input period)."),
		3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output previous values:" ), 
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputPreviousValues_JTextField = new JTextField ( 35 );
	__OutputPreviousValues_JTextField.setToolTipText("Specify values separated by commas, earliest value first, can use ${Property}.");
	__OutputPreviousValues_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, __OutputPreviousValues_JTextField,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel(
        "Optional - output previous values (default - calculated from input time series)."), 
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output minimum value:" ), 
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputMinimum_JTextField = new JTextField ( 20 );
	__OutputMinimum_JTextField.setToolTipText("Specify the output minimum value, can use ${Property} notation");
	__OutputMinimum_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, __OutputMinimum_JTextField,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel(
        "Optional - output minimum value (default - no minimum limit)."), 
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(output_JPanel, new JLabel ( "Output maximum value:" ), 
		0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputMaximum_JTextField = new JTextField ( 20 );
	__OutputMaximum_JTextField.setToolTipText("Specify the output maximum value, can use ${Property} notation");
	__OutputMaximum_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(output_JPanel, __OutputMaximum_JTextField,
		1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(output_JPanel, new JLabel(
        "Optional - output maximum value (default - no maximum limit)."), 
        3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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
public void itemStateChanged ( ItemEvent e )
{	checkGUIState();
    refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

    refresh ();
	if ( code == KeyEvent.VK_ENTER ) {
	    checkInput();
		if ( !__error_wait ) {
			response ( false );
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = getClass().getSimpleName() + ".refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String ARMAInterval = "";
    String a = "";
    String b = "";
    String RequireCoefficientsSumTo1 = "";
    String InputPreviousValues = "";
    String OutputPreviousValues = "";
    String Alias = "";
	String NewTSID = "";
	String Description = "";
	String OutputStart = "";
	String OutputEnd = "";
    String OutputMinimum = "";
    String OutputMaximum = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        ARMAInterval = props.getValue ( "ARMAInterval" );
        a = props.getValue ( "a" );
        b = props.getValue ( "b" );
        RequireCoefficientsSumTo1 = props.getValue ( "RequireCoefficientsSumTo1" );
        InputPreviousValues = props.getValue ( "InputPreviousValues" );
        OutputPreviousValues = props.getValue ( "OutputPreviousValues" );
		Alias = props.getValue ( "Alias" );
		NewTSID = props.getValue ( "NewTSID" );
		Description = props.getValue ( "Description" );
		OutputStart = props.getValue ( "OutputStart" );
		OutputEnd = props.getValue ( "OutputEnd" );
        OutputMinimum = props.getValue ( "OutputMinimum" );
        OutputMaximum = props.getValue ( "OutputMaximum" );
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
                "Existing command references an invalid\nTSList value \"" + TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (    JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
                JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {  // Select the blank...
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
		if ( ARMAInterval == null || ARMAInterval.equals("") ) {
			// Select a default...
			__ARMAInterval_JComboBox.select ( 0 );
		} 
		else {
			if ( JGUIUtil.isSimpleJComboBoxItem( __ARMAInterval_JComboBox, ARMAInterval, JGUIUtil.NONE, null, null ) ) {
				__ARMAInterval_JComboBox.select ( ARMAInterval );
			}
			else {
				// Allow property
				if ( ARMAInterval.indexOf("${") >= 0 ) {
				    // Add to list at top and select
				    __ARMAInterval_JComboBox.insert(ARMAInterval, 0);
				    __ARMAInterval_JComboBox.select(0);
				}
				else {
					// For legacy reasons, allow exact interval to be added if it parses
					// TSTool 11.08.00 added combo box to select
					boolean oldOk = false;
					try {
					    TimeInterval.parseInterval(ARMAInterval);
					    oldOk = true;
					    // Add to list at top and select
					    __ARMAInterval_JComboBox.insert(ARMAInterval, 0);
					    __ARMAInterval_JComboBox.select(0);
					}
					catch ( Exception e ) {
						oldOk = false;
					}
					if ( !oldOk ) {
						Message.printWarning ( 1, routine,
							"Existing command references an invalid\nARMAInterval \"" + ARMAInterval + "\".  "
							+"Select a different choice or Cancel." );
						__error_wait = true;
					}
				}
			}
		}
        if ( a != null ) {
            __a_JTextField.setText( a );
        }
        if ( b != null ) {
            __b_JTextField.setText( b );
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(__RequireCoefficientsSumTo1_JComboBox, RequireCoefficientsSumTo1, JGUIUtil.NONE, null, null ) ) {
            __RequireCoefficientsSumTo1_JComboBox.select ( RequireCoefficientsSumTo1 );
        }
        else {
            if ( (RequireCoefficientsSumTo1 == null) || RequireCoefficientsSumTo1.equals("") ) {
                // New command...select the default...
                __RequireCoefficientsSumTo1_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "RequireCoefficientsSumTo1 parameter \"" + RequireCoefficientsSumTo1 + "\".  Select a different value or Cancel." );
            }
        }
        if ( InputPreviousValues != null ) {
            __InputPreviousValues_JTextField.setText( InputPreviousValues );
        }
        if ( OutputPreviousValues != null ) {
            __OutputPreviousValues_JTextField.setText( OutputPreviousValues );
        }
		if ( Alias != null ) {
			__Alias_JTextField.setText ( Alias );
		}
		if ( NewTSID != null ) {
			__NewTSID_JTextArea.setText ( NewTSID );
		}
		if ( Description != null ) {
			__Description_JTextField.setText ( Description );
		}
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText (OutputStart);
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText (OutputEnd);
		}
        if ( OutputMinimum != null ) {
            __OutputMinimum_JTextField.setText( OutputMinimum );
        }
        if ( OutputMaximum != null ) {
            __OutputMaximum_JTextField.setText( OutputMaximum );
        }
    }
    // Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    ARMAInterval = __ARMAInterval_JComboBox.getSelected();
    a = __a_JTextField.getText().trim();
    b = __b_JTextField.getText().trim();
    RequireCoefficientsSumTo1 = __RequireCoefficientsSumTo1_JComboBox.getSelected().trim();
    InputPreviousValues = __InputPreviousValues_JTextField.getText().trim();
    OutputPreviousValues = __OutputPreviousValues_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	NewTSID = __NewTSID_JTextArea.getText().trim();
	Description = __Description_JTextField.getText().trim();
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
    OutputMinimum = __OutputMinimum_JTextField.getText().trim();
    OutputMaximum = __OutputMaximum_JTextField.getText().trim();
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "ARMAInterval=" + ARMAInterval );
    props.add ( "a=" + a );
    props.add ( "b=" + b );
    props.add ( "RequireCoefficientsSumTo1=" + RequireCoefficientsSumTo1 );
    props.add ( "InputPreviousValues=" + InputPreviousValues );
    props.add ( "OutputPreviousValues=" + OutputPreviousValues );
	props.add ( "Alias=" + Alias );
	props.add ( "NewTSID=" + NewTSID );
	props.add ( "Description=" + Description );
    props.add ( "OutputStart=" + OutputStart );
    props.add ( "OutputEnd=" + OutputEnd );
    props.add ( "OutputMinimum=" + OutputMinimum );
    props.add ( "OutputMaximum=" + OutputMaximum );
    __command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{   __ok = ok;  // Save to be returned by ok()
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

public void windowActivated( WindowEvent evt )
{
}

public void windowClosed( WindowEvent evt )
{
}

public void windowDeactivated( WindowEvent evt )
{
}

public void windowDeiconified( WindowEvent evt )
{
}

public void windowIconified( WindowEvent evt )
{
}

public void windowOpened( WindowEvent evt )
{
}

}
