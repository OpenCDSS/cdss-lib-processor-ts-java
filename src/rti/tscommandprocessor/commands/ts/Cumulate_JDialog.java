// Cumulate_JDialog - Editor for the Cumulate command.

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
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Editor for the Cumulate command.
*/
@SuppressWarnings("serial")
public class Cumulate_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private Cumulate_Command __command = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox	__HandleMissingHow_JComboBox = null;
private SimpleJComboBox	__Reset_JComboBox = null;
private SimpleJComboBox __ResetValue_JComboBox = null;
private JTextField __AllowMissingCount_JTextField = null;
private JTextField __MinimumSampleSize_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Cumulate_JDialog ( JFrame parent, Cumulate_Command command )
{	super(parent, true);
	initialize ( parent, command );
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
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "Cumulate");
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
{	// Put together a list of parameters to check...
	PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();  
	String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
	String Reset = __Reset_JComboBox.getSelected();
	String ResetValue = __ResetValue_JComboBox.getSelected();
    String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
    String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
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
	if ( HandleMissingHow.length() > 0 ) {
        parameters.set ( "HandleMissingHow", HandleMissingHow );
	}
	if ( Reset.length() > 0 ) {
        parameters.set ( "Reset", Reset );
	}
    if ( ResetValue.length() > 0 ) {
        parameters.set ( "ResetValue", ResetValue );
    }
    if ( (AllowMissingCount != null) && (AllowMissingCount.length() > 0) ) {
        parameters.set ( "AllowMissingCount", AllowMissingCount );
    }
    if ( (MinimumSampleSize != null) && (MinimumSampleSize.length() > 0) ) {
        parameters.set ( "MinimumSampleSize", MinimumSampleSize );
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
{	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();  
	String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
	String Reset = __Reset_JComboBox.getSelected();
	String ResetValue = __ResetValue_JComboBox.getSelected();
    String AllowMissingCount = __AllowMissingCount_JTextField.getText().trim();
    String MinimumSampleSize = __MinimumSampleSize_JTextField.getText().trim();
	if ( (Reset != null) && (Reset.length() > 0) ) {
		// Use the first token...
		Reset = StringUtil.getToken(Reset,"(",0,0).trim();
	}
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "HandleMissingHow", HandleMissingHow );
	__command.setCommandParameter ( "Reset", Reset );
	__command.setCommandParameter ( "ResetValue", ResetValue );
    __command.setCommandParameter ( "AllowMissingCount", AllowMissingCount);
    __command.setCommandParameter ( "MinimumSampleSize", MinimumSampleSize);
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JComboBox = null;
	__HandleMissingHow_JComboBox = null;
	__Reset_JComboBox = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Cumulate_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The selected time series will be converted to cumulative values over the period." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "The units remain the original." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specify a Reset value to reset the total for each year.  The date/time should be specified to " +
        "a precision matching the time series." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "   Use a format MM for month, MM-DD for day, MM-DD hh for hour, and MM-DD hh:mm for minute data." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "   The year's data will be set to missing if AllowingMissingCount and " +
        "MinimumSampleSize criteria are not met." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select a time series ensemble ID from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Handle missing data how?:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__HandleMissingHow_JComboBox = new SimpleJComboBox ();
	List<String> missingChoices = new ArrayList<>();
	missingChoices.add ( "" );
	missingChoices.add ( __command._CarryForwardIfMissing );
	missingChoices.add ( __command._SetMissingIfMissing );
	__HandleMissingHow_JComboBox.setData(missingChoices);
	__HandleMissingHow_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __HandleMissingHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional (default=" + __command._SetMissingIfMissing + ")."), 
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    int yReset = -1;
    JPanel reset_JPanel = new JPanel();
    reset_JPanel.setLayout( new GridBagLayout() );
    reset_JPanel.setBorder( BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),"Parameters to reset value every year" ));
    JGUIUtil.addComponent( main_JPanel, reset_JPanel,
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(reset_JPanel, new JLabel ( "Reset date/time:" ), 
		0, ++yReset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Reset_JComboBox = new SimpleJComboBox (true); // Editable
	__Reset_JComboBox.setToolTipText("Reset in format MM, MM-DD, etc. consistent with time series precision, can use ${Property}.");
	List<String> resetChoices = new ArrayList<>();
	resetChoices.add ( "" );  // No reset
    /*
    for ( int i = 1; i <= 12; i++ ) {
        resetChoices.add ( "Date " + i + "-1 (reset matching MM-DD for day interval)" );
    }
    for ( int i = 1; i <= 12; i++ ) {
        resetChoices.add ( "Date " + i + "-1 (reset matching MM-DD for month interval)" );
    }
	for ( int i = 1; i <= 31; i++ ) {
		resetChoices.add ( "Day " + i + " (reset every specified day for day interval)" );
	}
	for ( int i = 1; i <= 12; i++ ) {
		resetChoices.add ( "Month " + i + " (reset every specified month for month interval)" );
	}
    */
	__Reset_JComboBox.setData(resetChoices);
	__Reset_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(reset_JPanel, __Reset_JComboBox,
		1, yReset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(reset_JPanel, new JLabel(
		"Optional - date/time on which to reset total (default=no reset)."), 
		3, yReset, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(reset_JPanel, new JLabel ( "Reset value:" ), 
        0, ++yReset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ResetValue_JComboBox = new SimpleJComboBox (true); // Allow edit
    List<String> resetValueChoices = new ArrayList<>();
    resetValueChoices.add ( "" );
    resetValueChoices.add ( __command._DataValue );
    resetValueChoices.add ( __command._Zero );
    __ResetValue_JComboBox.addItemListener ( this );
    __ResetValue_JComboBox.setData(resetValueChoices);
    JGUIUtil.addComponent(reset_JPanel, __ResetValue_JComboBox,
        1, yReset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(reset_JPanel, new JLabel(
        "Optional - value for reset (default=" + __command._Zero + ")."), 
        3, yReset, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(reset_JPanel, new JLabel ("Allow missing count:"),
        0, ++yReset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AllowMissingCount_JTextField = new JTextField (10);
    __AllowMissingCount_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(reset_JPanel, __AllowMissingCount_JTextField,
        1, yReset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(reset_JPanel, new JLabel (
        "Optional - number of missing values allowed in year (default=no limit)."),
        3, yReset, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(reset_JPanel, new JLabel ("Minimum sample size:"),
        0, ++yReset, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MinimumSampleSize_JTextField = new JTextField (10);
    __MinimumSampleSize_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(reset_JPanel, __MinimumSampleSize_JTextField,
        1, yReset, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(reset_JPanel, new JLabel (
        "Optional - minimum required sample size in year (default=no minimum)."),
        3, yReset, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 40 );
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
{	String routine = "Cumulate_JDialog.refresh";
    String TSList = "";
    String TSID = "";
    String EnsembleID = "";
	String HandleMissingHow = "";
	String Reset = "";
	String ResetValue = "";
    String AllowMissingCount = "";
    String MinimumSampleSize = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        TSList = props.getValue ( "TSList" );
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
		HandleMissingHow = props.getValue ( "HandleMissingHow" );
		Reset = props.getValue ( "Reset" );
		ResetValue = props.getValue ( "ResetValue" );
        AllowMissingCount = props.getValue ( "AllowMissingCount" );
        MinimumSampleSize = props.getValue ( "MinimumSampleSize" );
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
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
            __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the blank...
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
		if ( JGUIUtil.isSimpleJComboBoxItem( __HandleMissingHow_JComboBox, HandleMissingHow, JGUIUtil.NONE, null, null ) ) {
		    __HandleMissingHow_JComboBox.select ( HandleMissingHow );
		}
		else {
            // Automatically add to the list after the blank...
			if ( (HandleMissingHow != null) && (HandleMissingHow.length() > 0) ) {
				__HandleMissingHow_JComboBox.insertItemAt ( HandleMissingHow, 1 );
				// Select...
				__HandleMissingHow_JComboBox.select ( HandleMissingHow );
			}
			else {	// Select the blank...
				__HandleMissingHow_JComboBox.select ( 0 );
			}
		}
		try {
            JGUIUtil.selectTokenMatches ( __Reset_JComboBox, true, "(", 0, 0, Reset, "", true );
		}
		catch ( Exception e ) {
			// Automatically add to the list after the blank...
			if ( (Reset != null) && (Reset.length() > 0) ) {
				__Reset_JComboBox.insertItemAt ( Reset, 1 );
				// Select...
				__Reset_JComboBox.select ( Reset );
			}
			else {
			    // Select the blank...
				__Reset_JComboBox.select ( 0 );
			}
		}
        if ( JGUIUtil.isSimpleJComboBoxItem( __ResetValue_JComboBox, ResetValue, JGUIUtil.NONE, null, null ) ) {
            __ResetValue_JComboBox.select ( ResetValue );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (ResetValue != null) && (ResetValue.length() > 0) ) {
                __ResetValue_JComboBox.insertItemAt ( ResetValue, 1 );
                // Select...
                __ResetValue_JComboBox.select ( ResetValue );
            }
            else {
                // Select the blank...
                __ResetValue_JComboBox.select ( 0 );
            }
        }
        if ( AllowMissingCount != null ) {
            __AllowMissingCount_JTextField.setText ( AllowMissingCount );
        }
        if ( MinimumSampleSize != null ) {
            __MinimumSampleSize_JTextField.setText ( MinimumSampleSize );
        }
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
	HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
	Reset = __Reset_JComboBox.getSelected();
	ResetValue = __ResetValue_JComboBox.getSelected();
    AllowMissingCount = __AllowMissingCount_JTextField.getText();
    MinimumSampleSize = __MinimumSampleSize_JTextField.getText();
	if ( (Reset != null) && (Reset.length() > 0) ) {
		// Use the first token...
		Reset = StringUtil.getToken(Reset,"(",0,0).trim();
	}
	props = new PropList ( __command.getCommandName() );
    props.add ( "TSList=" + TSList );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
	props.add ( "HandleMissingHow=" + HandleMissingHow );
	props.add ( "Reset=" + Reset );
	props.add ( "ResetValue=" + ResetValue );
    props.add ( "AllowMissingCount=" + AllowMissingCount );
    props.add ( "MinimumSampleSize=" + MinimumSampleSize );
	__command_JTextArea.setText( __command.toString ( props ) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
*/
private void response ( boolean ok )
{	__ok = ok; // Save to be returned by ok()
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
{	response ( true );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}
