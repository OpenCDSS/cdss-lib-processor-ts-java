// ResequenceTimeSeriesData_JDialog - Command editor dialog for the ResequenceTimeSeriesData() command.

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

import java.util.List;
import java.util.Vector;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.YearType;

/**
Command editor dialog for the ResequenceTimeSeriesData() command.
*/
@SuppressWarnings("serial")
public class ResequenceTimeSeriesData_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, ItemListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ResequenceTimeSeriesData_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __TableID_JComboBox = null;
private SimpleJComboBox __TableColumn_JComboBox = null;
private SimpleJComboBox __TableRowStart_JComboBox = null;
private SimpleJComboBox __TableRowEnd_JComboBox = null;
private JTextField __OutputStart_JTextField = null;// Start of period for output
private SimpleJComboBox __OutputYearType_JComboBox = null;
private JTextField __NewScenario_JTextField = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private boolean	__error_wait = false;	// Is there an error to be cleared up?
private boolean	__first_time = true;
private boolean	__ok = false; // Indicates whether the user has pressed OK to close the dialog.

/**
WriteDateValue_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ResequenceTimeSeriesData_JDialog ( JFrame parent, ResequenceTimeSeriesData_Command command )
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
		HelpViewer.getInstance().showHelp("command", "ResequenceTimeSeriesData");
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
    String TableID = __TableID_JComboBox.getSelected();
    String TableColumn = __TableColumn_JComboBox.getSelected();
    String TableRowStart = __TableRowStart_JComboBox.getSelected();
    String TableRowEnd = __TableRowEnd_JComboBox.getSelected();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputYearType = __OutputYearType_JComboBox.getSelected();
    String NewScenario = __NewScenario_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();

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
	if ( TableID.length() > 0 ) {
		parameters.set ( "TableID", TableID );
	}
    if ( TableColumn.length() > 0 ) {
        parameters.set ( "TableColumn", TableColumn );
    }
    if ( TableRowStart.length() > 0 ) {
        parameters.set ( "TableRowStart", TableRowStart );
    }
    if ( TableRowEnd.length() > 0 ) {
        parameters.set ( "TableRowEnd", TableRowEnd );
    }
    if ( OutputStart.length() > 0 ) {
        parameters.set ( "OutputStart", OutputStart );
    }
    if ( OutputYearType.length() > 0 ) {
        parameters.set ( "OutputYearType", OutputYearType );
    }
    if ( NewScenario.length() > 0 ) {
        parameters.set ( "NewScenario", NewScenario );
    }
    if (Alias.length() > 0) {
        parameters.set("Alias", Alias);
    }
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( parameters, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		Message.printWarning(2,"",e);
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
    String TableID = __TableID_JComboBox.getSelected();
    String TableColumn = __TableColumn_JComboBox.getSelected();
    String TableRowStart = __TableRowStart_JComboBox.getSelected();
    String TableRowEnd = __TableRowEnd_JComboBox.getSelected();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputYearType = __OutputYearType_JComboBox.getSelected();
    String NewScenario = __NewScenario_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "TableColumn", TableColumn );
    __command.setCommandParameter ( "TableRowStart", TableRowStart );
    __command.setCommandParameter ( "TableRowEnd", TableRowEnd );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputYearType", OutputYearType );
    __command.setCommandParameter ( "NewScenario", NewScenario );
    __command.setCommandParameter("Alias", Alias);
}

/**
Put together a list of row/column numbers (a list of numbers) for use in choices.
*/
private List<String> getRowNumberList ()
{
	List<String> v = new Vector<String>();
    v.add( "");
    for ( int i = 1; i <= 200; i++ ) {
        v.add("" + i);
    }
    return v;
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ResequenceTimeSeriesData_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Resequence time series data by \"shuffling\" the original years of data, creating new time series." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "An identifier for the table with the new year sequence must be specified, and each sequence of " +
        "years must be provided in a column with a unique column heading."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The resulting time series will start in the indicated year and have a time series identifier that is " +
        "the same as the original time series, additionally with the indicated scenario." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "If not specified, the output start is taken from the global output start or the time series." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Table ID for year sequence:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    List<String> tableids = TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    __TableID_JComboBox = new SimpleJComboBox ( true );
    if ( tableids == null ) {
        tableids = new Vector<String>();
    }
    // No blank (default) or wildcard is allowed.
    __TableID_JComboBox.setData ( tableids );
    if ( tableids.size() > 0 ) {
        __TableID_JComboBox.select ( 0 );
    }
    __TableID_JComboBox.addItemListener ( this );
    __TableID_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TableID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Column name in table for year sequence:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    __TableColumn_JComboBox = new SimpleJComboBox ( true );
    // Use discovery data to fill in column names
    setTableColumnData ( __TableID_JComboBox.getSelected() );
    __TableColumn_JComboBox.addItemListener ( this );
    __TableColumn_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TableColumn_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - column name for year sequence."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("New scenario:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewScenario_JTextField = new JTextField (10);
    __NewScenario_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __NewScenario_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - for TSID of new time series."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("First row number in table for year sequence:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    __TableRowStart_JComboBox = new SimpleJComboBox ( true );
    __TableRowStart_JComboBox.setData ( getRowNumberList() );
    __TableRowStart_JComboBox.select( 0 ); // Default
    __TableRowStart_JComboBox.addItemListener ( this );
    __TableRowStart_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TableRowStart_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - default is first row in column."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
            
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Last row number in table for year sequence:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    // Allow edits...
    __TableRowEnd_JComboBox = new SimpleJComboBox ( true );
    __TableRowEnd_JComboBox.setData ( getRowNumberList() );
    __TableRowEnd_JComboBox.select( 0 ); // Default
    __TableRowEnd_JComboBox.addItemListener ( this );
    __TableRowEnd_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TableRowEnd_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - default is last row in column."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output year type:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputYearType_JComboBox = new SimpleJComboBox ( false );
    __OutputYearType_JComboBox.add ( "" );
    __OutputYearType_JComboBox.add ( "" + YearType.CALENDAR );
    __OutputYearType_JComboBox.add ( "" + YearType.NOV_TO_OCT );
    __OutputYearType_JComboBox.add ( "" + YearType.WATER );
    __OutputYearType_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputYearType_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - output year type (default=" +
        YearType.CALENDAR + ")."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output start:"), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField (20);
    __OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - starting year of resequenced time series, consistent with output year type."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
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

	__ok_JButton = new SimpleJButton("OK", "OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	refresh();
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e) {
    checkGUIState();
    if ( (e.getSource() == __TableID_JComboBox) && (e.getStateChange() == ItemEvent.SELECTED) ) {
        // Update the list
        setTableColumnData ( __TableID_JComboBox.getSelected() );
    }
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
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{
}

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
{	String routine = "ResequenceTimeSeriesData_JDialog.refresh";
	String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String TableID = "";
    String TableColumn = "";
    String TableRowStart = "";
    String TableRowEnd = "";
    String OutputYearType = "";
    String OutputStart = "";
    String NewScenario = "";
    String Alias = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        TableID = parameters.getValue ( "TableID" );
        TableColumn = parameters.getValue ( "TableColumn" );
        TableRowStart = parameters.getValue ( "TableRowStart" );
        TableRowEnd = parameters.getValue ( "TableRowEnd" );
        OutputYearType = parameters.getValue ( "OutputYearType" );
        OutputStart = parameters.getValue("OutputStart");
        NewScenario = parameters.getValue ( "NewScenario" );
        Alias = parameters.getValue("Alias");
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
				"Existing command references an invalid\nTSList value \"" +	TSList +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
        if ( TSID == null ) {
            // Select default...
            __TSID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox,TSID, JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTSID value \"" +
                TSID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
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
        if ( TableID == null ) {
            // Select default...
            if ( __TableID_JComboBox.getItemCount() > 0 ) {
                __TableID_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableID value \"" +
                TableID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TableColumn == null ) {
            // Select default...
            if ( __TableColumn_JComboBox.getItemCount() > 0 ) {
                __TableColumn_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableColumn_JComboBox,TableColumn, JGUIUtil.NONE, null, null ) ) {
                __TableColumn_JComboBox.select ( TableColumn );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableColumn value \"" + TableColumn +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TableRowStart == null ) {
            // Select default...
            if ( __TableRowStart_JComboBox.getItemCount() > 0 ) {
                __TableRowStart_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableRowStart_JComboBox,TableRowStart, JGUIUtil.NONE, null, null ) ) {
                __TableRowStart_JComboBox.select ( TableRowStart );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableRowStart value \"" +
                TableRowStart +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( TableRowEnd == null ) {
            // Select default...
            if ( __TableRowEnd_JComboBox.getItemCount() > 0 ) {
                __TableRowEnd_JComboBox.select ( 0 );
            }
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableRowEnd_JComboBox,TableRowEnd, JGUIUtil.NONE, null, null ) ) {
                __TableRowEnd_JComboBox.select ( TableRowEnd );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nTableRowEnd value \"" +
                TableRowEnd +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
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
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText ( OutputStart );
        }
        if ( NewScenario != null ) {
            __NewScenario_JTextField.setText(NewScenario);
        }
        if (Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
	}
	// Regardless, reset the command from the fields...
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    TableID = __TableID_JComboBox.getSelected();
    TableColumn = __TableColumn_JComboBox.getSelected();
    TableRowStart = __TableRowStart_JComboBox.getSelected();
    TableRowEnd = __TableRowEnd_JComboBox.getSelected();
    OutputYearType = __OutputYearType_JComboBox.getSelected();
    OutputStart = __OutputStart_JTextField.getText().trim();
    NewScenario = __NewScenario_JTextField.getText().trim();
    Alias = __Alias_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
	parameters.add ( "TSID=" + TSID );
	parameters.add ( "TableID=" + TableID );
    parameters.add ( "TableColumn=" + TableColumn );
    parameters.add ( "TableRowStart=" + TableRowStart );
    parameters.add ( "TableRowEnd=" + TableRowEnd );
    parameters.add ( "OutputStart=" + OutputStart );
    parameters.add ( "OutputYearType=" + OutputYearType );
    parameters.add ( "NewScenario=" + NewScenario );
    parameters.add("Alias=" + Alias);
	__command_JTextArea.setText( __command.toString ( parameters ).trim() );
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
Set the table column data based on the selected table.
*/
private void setTableColumnData ( String selected_tableid )
{   List<String> column_names = TSCommandProcessorUtil.getTableColumnNamesFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command, __TableID_JComboBox.getSelected(), false );
    __TableColumn_JComboBox.setData ( column_names );
    if ( column_names.size() > 0 ) {
        __TableColumn_JComboBox.select( 0 );
    }
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
