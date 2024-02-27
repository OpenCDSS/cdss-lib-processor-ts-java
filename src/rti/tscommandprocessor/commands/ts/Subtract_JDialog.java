// Subtract_JDialog - Editor dialog for the Subtract() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Editor dialog for the Subtract() command.
*/
@SuppressWarnings("serial")
public class Subtract_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, MouseListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private Subtract_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox	__TSID_JComboBox = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox	__SubtractTSList_JComboBox = null;
private JLabel __SubtractTSID_JLabel = null;
private SimpleJComboBox __SubtractTSID_JComboBox = null;
private JLabel __SubtractEnsembleID_JLabel = null;
private SimpleJComboBox __SubtractEnsembleID_JComboBox = null;
private JLabel __SubtractSpecifiedTSID_JLabel = null;
private DefaultListModel<String> __SubtractSpecifiedTSID_JListModel = null;
private JList<String> __SubtractSpecifiedTSID_JList = null;
private SimpleJComboBox	__HandleMissingHow_JComboBox = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private boolean	__error_wait = false;
private boolean	__first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public Subtract_JDialog ( JFrame parent, Subtract_Command command ) {
    super(parent, true);
    initialize ( parent, command );
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
		HelpViewer.getInstance().showHelp("command", "Subtract");
	}
	else if ( o == __ok_JButton ) {
		refresh();
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
    String TSList = __SubtractTSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) || TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __SubtractTSID_JComboBox.setEnabled(true);
        __SubtractTSID_JLabel.setEnabled ( true );
    }
    else {
        __SubtractTSID_JComboBox.setEnabled(false);
        __SubtractTSID_JLabel.setEnabled ( false );
    }
    if ( TSListType.ENSEMBLE_ID.equals(TSList)) {
        __SubtractEnsembleID_JComboBox.setEnabled(true);
        __SubtractEnsembleID_JLabel.setEnabled ( true );
    }
    else {
        __SubtractEnsembleID_JComboBox.setEnabled(false);
        __SubtractEnsembleID_JLabel.setEnabled ( false );
    }
    if ( TSListType.SPECIFIED_TSID.equals(TSList)) {
        __SubtractSpecifiedTSID_JList.setEnabled(true);
        __SubtractSpecifiedTSID_JLabel.setEnabled ( true );
    }
    else {
        __SubtractSpecifiedTSID_JList.setEnabled(false);
        __SubtractSpecifiedTSID_JLabel.setEnabled ( false );
    }
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput () {
	// Put together a list of parameters to check.
    PropList props = new PropList ( "" );
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String SubtractTSList = __SubtractTSList_JComboBox.getSelected();
    String SubtractTSID = __SubtractTSID_JComboBox.getSelected();
    String SubtractSpecifiedTSID = getSubtractSpecifiedTSIDFromList();
    String SubtractEnsembleID = __SubtractEnsembleID_JComboBox.getSelected();
    String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    __error_wait = false;

    // TSID is used for several variations of SubtractTSList.
    if ( TSListType.SPECIFIED_TSID.equals(SubtractTSList) ) {
        SubtractTSID = SubtractSpecifiedTSID;
    }

    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( SubtractTSList.length() > 0 ) {
        props.set ( "SubtractTSList", SubtractTSList );
    }
    if ( SubtractTSID.length() > 0 ) {
        props.set ( "SubtractTSID", SubtractTSID );
    }
    if ( SubtractEnsembleID.length() > 0 ) {
        props.set ( "SubtractEnsembleID", SubtractEnsembleID );
    }
    if ( HandleMissingHow.length() > 0 ) {
        props.set ( "HandleMissingHow", HandleMissingHow );
    }
	if ( AnalysisStart.length() > 0 ) {
		props.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		props.set ( "AnalysisEnd", AnalysisEnd );
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
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String SubtractTSList = __SubtractTSList_JComboBox.getSelected();
    String SubtractTSID = __SubtractTSID_JComboBox.getSelected();
    String SubtractSpecifiedTSID = getSubtractSpecifiedTSIDFromList();
    String SubtractEnsembleID = __SubtractEnsembleID_JComboBox.getSelected();
    String HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
    //String TransferHow = __TransferHow_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();

    // TSID is used for several variations of SubtractTSList.
    if ( TSListType.SPECIFIED_TSID.equals(SubtractTSList) ) {
        SubtractTSID = SubtractSpecifiedTSID;
    }

    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "SubtractTSList", SubtractTSList );
    __command.setCommandParameter ( "SubtractTSID", SubtractTSID );
    __command.setCommandParameter ( "SubtractEnsembleID", SubtractEnsembleID );
    __command.setCommandParameter ( "HandleMissingHow", HandleMissingHow );
    //__command.setCommandParameter ( "TransferHow", TransferHow );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
}

/**
Get the SubtractSpecifiedTSID parameter from the JList and put into a string.
@return a String containing the selected specified time series, separated by commas.
*/
private String getSubtractSpecifiedTSIDFromList() {
    StringBuffer buffer = new StringBuffer();
    if ( JGUIUtil.selectedSize(__SubtractSpecifiedTSID_JList) > 0 ) {
        // Get the selected and format.
        int selected[] = __SubtractSpecifiedTSID_JList.getSelectedIndices();
        int size = JGUIUtil.selectedSize(__SubtractSpecifiedTSID_JList);
        for ( int i = 0; i < size; i++ ) {
            if ( i > 0 ) {
                buffer.append ( ",");
            }
            buffer.append ( __SubtractSpecifiedTSID_JListModel.elementAt( selected[i]) );
        }
    }
    return buffer.toString();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Subtract_Command command ) {
   __command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

   	JGUIUtil.addComponent(main_JPanel,
		new JLabel ( "Subtract one or more time series from a time series (or ensemble of time series)." +
		"  The receiving time series (or ensemble) is modified."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time series to be subtracted are selected using the TS list parameter:"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "  " + TSListType.ALL_MATCHING_TSID + " - subtract all previous time series with matching identifiers."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  " + TSListType.ALL_TS + " - subtract all previous time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  " + TSListType.SELECTED_TS + " - subtract time series selected with selectTimeSeries() commands"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"  " + TSListType.SPECIFIED_TSID + " - subtract time series selected from the list below"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Time series to be subtracted to.

    JLabel TSID_JLabel = new JLabel ("Time series to receive results:");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel (
            this, this, main_JPanel, TSID_JLabel, __TSID_JComboBox, tsids, y, false );

   JLabel EnsembleID_JLabel = new JLabel ("Ensemble to receive results:");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

    // The time series to supply values (time series to subtract).

    __SubtractTSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel (
            this, main_JPanel, new JLabel ("Time series to subtract (SubtractTSList):"), __SubtractTSList_JComboBox, y );
    // Default is not to add SelectedTSID so add it here.
    __SubtractTSList_JComboBox.add(TSListType.SPECIFIED_TSID.toString());

    __SubtractTSID_JLabel = new JLabel ("Subtract TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __SubtractTSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __SubtractTSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    // Automatically adds "*".
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __SubtractTSID_JLabel, __SubtractTSID_JComboBox, tsids, y );

    __SubtractEnsembleID_JLabel = new JLabel ("Add EnsembleID (for SubtractTSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __SubtractEnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits.
    __SubtractEnsembleID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> SubtractEnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __SubtractEnsembleID_JLabel, __SubtractEnsembleID_JComboBox, SubtractEnsembleIDs, y );

    __SubtractSpecifiedTSID_JLabel =
        new JLabel ("Subtract specified TSID (for SubtractTSList=" + TSListType.SPECIFIED_TSID.toString() + "):");
    JGUIUtil.addComponent(main_JPanel, __SubtractSpecifiedTSID_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SubtractSpecifiedTSID_JListModel = new DefaultListModel<String>();
    // Get the list again because above list will have "*" which don't want here.
	List<String> tsids2 = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    int size = tsids2.size();
	for ( int i = 0; i < size; i++ ) {
		__SubtractSpecifiedTSID_JListModel.addElement( (String)tsids2.get(i));
	}
	__SubtractSpecifiedTSID_JList = new JList<String> ( __SubtractSpecifiedTSID_JListModel );
    __SubtractSpecifiedTSID_JList.setVisibleRowCount(Math.min(5,size));
	__SubtractSpecifiedTSID_JList.addListSelectionListener ( this );
	__SubtractSpecifiedTSID_JList.addKeyListener ( this );
	__SubtractSpecifiedTSID_JList.addMouseListener ( this );
	__SubtractSpecifiedTSID_JList.clearSelection();
	DefaultListSelectionModel sm = new DefaultListSelectionModel();
	sm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
	__SubtractSpecifiedTSID_JList.setSelectionModel ( sm );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__SubtractSpecifiedTSID_JList),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Handle missing data how?:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__HandleMissingHow_JComboBox = new SimpleJComboBox ( false );
	List<String> missingChoices = new ArrayList<>();
	missingChoices.add ( "" );
	missingChoices.add ( __command._IgnoreMissing );
	missingChoices.add ( __command._SetMissingIfOtherMissing );
	missingChoices.add ( __command._SetMissingIfAnyMissing );
	__HandleMissingHow_JComboBox.addItemListener ( this );
	__HandleMissingHow_JComboBox.setData(missingChoices);
    JGUIUtil.addComponent(main_JPanel, __HandleMissingHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - how to handle missing values in time series (default=" + __command._IgnoreMissing + ")."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis start:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.setToolTipText("Specify the analysis start using a date/time string or ${Property} notation");
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisStart_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Analysis end:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.setToolTipText("Specify the analysis end using a date/time string or ${Property} notation");
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __AnalysisEnd_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents.
    checkGUIState();
	refresh();

	// Panel for buttons.
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

	// Visualize it.

    setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	setResizable ( true );
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

    refresh();
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
Handle mouse clicked event.
*/
public void mouseClicked ( MouseEvent event ) {
}

/**
Handle mouse entered event.
*/
public void mouseEntered ( MouseEvent event ) {
}

/**
Handle mouse exited event.
*/
public void mouseExited ( MouseEvent event ) {
}

/**
Handle mouse pressed event.
*/
public void mousePressed ( MouseEvent event ) {
	int mods = event.getModifiers();
	if ( (mods & MouseEvent.BUTTON3_MASK) != 0 ) {
		//__ts_JPopupMenu.show (
		//event.getComponent(), event.getX(), event.getY() );
	}
}

/**
Handle mouse released event.
*/
public void mouseReleased ( MouseEvent event ) {
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
	String routine = __command + "_JDialog.refresh";
	String TSID = "";
    String EnsembleID = "";
	String SubtractTSList = "";
	String SubtractTSID = "";
    String SubtractEnsembleID = "";
    //String SubtractSpecifiedTSID = "";
    String HandleMissingHow = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command.
        TSID = props.getValue ( "TSID" );
        EnsembleID = props.getValue ( "EnsembleID" );
        SubtractTSList = props.getValue ( "SubtractTSList" );
        SubtractTSID = props.getValue ( "SubtractTSID" );
        //SubtractSpecifiedTSID = props.getValue ( "SubtractSpecifiedTSID" );
        SubtractEnsembleID = props.getValue ( "SubtractEnsembleID" );
        HandleMissingHow = props.getValue ( "HandleMissingHow" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
            __TSID_JComboBox.select ( TSID );
        }
        else {
            // Automatically add to the list after the blank.
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select.
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the blank.
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default.
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
        if ( SubtractTSList == null ) {
            // Select default.
            __SubtractTSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SubtractTSList_JComboBox,SubtractTSList, JGUIUtil.NONE, null, null ) ) {
                __SubtractTSList_JComboBox.select ( SubtractTSList );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSubtractTSList value \"" + SubtractTSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (    JGUIUtil.isSimpleJComboBoxItem( __SubtractTSID_JComboBox, SubtractTSID,
                JGUIUtil.NONE, null, null ) ) {
                __SubtractTSID_JComboBox.select ( SubtractTSID );
        }
        else {
        	// Automatically add to the list after the blank.
            if ( (SubtractTSID != null) && (SubtractTSID.length() > 0) ) {
                if ( !TSListType.SPECIFIED_TSID.equals(SubtractTSList) ) {
                    __SubtractTSID_JComboBox.insertItemAt ( SubtractTSID, 1 );
                    // Select.
                    __SubtractTSID_JComboBox.select ( SubtractTSID );
                }
                else {
                    // Select the blank.
                    __SubtractTSID_JComboBox.select ( 0 );
                }
            }
            else {
                // Select the blank.
                __SubtractTSID_JComboBox.select ( 0 );
            }
        }
        if ( SubtractEnsembleID == null ) {
            // Select default.
            __SubtractEnsembleID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SubtractEnsembleID_JComboBox,SubtractEnsembleID, JGUIUtil.NONE, null, null ) ) {
                __SubtractEnsembleID_JComboBox.select ( SubtractEnsembleID );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nSubtractEnsembleID value \"" + SubtractEnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        // Setup the list of time series to subtract, which will be a list of available time series,
        // selecting the ones that were in the previous command.
        //setupSubtractSpecifiedTSID ( SubtractTSList, SubtractSpecifiedTSID );
        setupSubtractSpecifiedTSID ( SubtractTSList, SubtractTSID );
        if ( HandleMissingHow == null ) {
            // Select default.
            __HandleMissingHow_JComboBox.select ( 0 );
        }
        else {
        	if ( JGUIUtil.isSimpleJComboBoxItem( __HandleMissingHow_JComboBox,
                HandleMissingHow, JGUIUtil.NONE, null, null )) {
                __HandleMissingHow_JComboBox.select (
                HandleMissingHow );
            }
            else {
            	Message.printWarning ( 1, routine,
                "Existing command " +
                "references an invalid\n" +
                "HandleMissingHow value \"" + HandleMissingHow +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}
	}
    // Regardless, reset the command from the fields.
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    SubtractTSList = __SubtractTSList_JComboBox.getSelected();
    SubtractTSID = __SubtractTSID_JComboBox.getSelected();
    String SubtractSpecifiedTSID = getSubtractSpecifiedTSIDFromList();
    SubtractEnsembleID = __SubtractEnsembleID_JComboBox.getSelected();
    HandleMissingHow = __HandleMissingHow_JComboBox.getSelected();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
    // Use the list of specified TSID instead of the __SubtractTSID_JComboBox above.
    if ( TSListType.SPECIFIED_TSID.equals(SubtractTSList) ) {
        SubtractTSID = SubtractSpecifiedTSID;
    }
    props = new PropList ( __command.getCommandName() );
    props.add ( "TSID=" + TSID );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "SubtractTSList=" + SubtractTSList );
    props.add ( "SubtractTSID=" + SubtractTSID );
    props.add ( "SubtractEnsembleID=" + SubtractEnsembleID );
    props.add ( "HandleMissingHow=" + HandleMissingHow );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
    __command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.
If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok ) {
    __ok = ok;  // Save to be returned by ok().
    if ( ok ) {
        // Commit the changes.
        commitEdits ();
        if ( __error_wait ) {
            // Not ready to close out!
            return;
        }
    }
    // Now close out.
    setVisible( false );
    dispose();
}

/**
Setup the SubtractSpecifiedTSID list at initialization,
selecting items in the list that match the SubtractTSID parameter.
@param SubtractSpecifiedTSID The value of the parameter, of form "TSID,TSID,TSID,...".
*/
private void setupSubtractSpecifiedTSID ( String SubtractTSList, String SubtractSpecifiedTSID ) {
    String routine = getClass().getSimpleName() + ".setupSubtractSelectedTSID";
    // Check all the items in the list and highlight the ones that match the command being edited.
    if ( (SubtractTSList != null) && TSListType.SPECIFIED_TSID.equals(SubtractTSList) && (SubtractSpecifiedTSID != null) ) {
        // Break list by commas since identifiers may have spaces and other "special" characters (but no commas)
    	List<String> v = StringUtil.breakStringList ( SubtractSpecifiedTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
        int size = v.size();
        int pos = 0;
        List<String> selected = new ArrayList<>();
        String independent = "";
        for ( int i = 0; i < size; i++ ) {
            independent = v.get(i);
            if ( (pos = JGUIUtil.indexOf( __SubtractSpecifiedTSID_JList, independent, false, true))>= 0 ) {
                // Select it because it is in the command and the list.
                selected.add ( "" + pos );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references a non-existent\n"+
                "specified time series \"" + independent +
                "\".  Select a\n" + "different time series or Cancel." );
            }
        }
        // Select the matched time series.
        if ( selected.size() > 0  ) {
            int [] iselected = new int[selected.size()];
            for ( int is = 0; is < iselected.length; is++ ){
                iselected[is] = StringUtil.atoi ( selected.get(is));
            }
            __SubtractSpecifiedTSID_JList.setSelectedIndices( iselected );
        }
    }
}

/**
Handle ListSelectionListener events.
*/
public void valueChanged ( ListSelectionEvent e ) {
	refresh ();
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