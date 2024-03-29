// ChangeTimeZone_JDialog - editor for ChangeTimeZone command

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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
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

@SuppressWarnings("serial")
public class ChangeTimeZone_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ChangeTimeZone_Command __command = null;
private JTextArea __command_JTextArea = null;
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJComboBox __OldTimeZone_JComboBox = null;
private SimpleJComboBox __NewTimeZone_JComboBox = null;
private SimpleJComboBox __ShiftTime_JComboBox = null;
private boolean __first_time = true;
private boolean __error_wait = false;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ChangeTimeZone_JDialog ( JFrame parent, ChangeTimeZone_Command command ) {
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
		HelpViewer.getInstance().showHelp("command", "ChangeTimeZone");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
		checkGUIState();
		refresh ();
	}
}

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState () {
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
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Create a list of parameters to check.
	PropList props = new PropList ( "" );
	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String OldTimeZone = getSelectedNewTimeZone();
	String NewTimeZone = getSelectedNewTimeZone();
    String ShiftTime = __ShiftTime_JComboBox.getSelected();
	__error_wait = false;

	if ( TSList.length() > 0 ) {
		props.set ( "TSList", TSList );
	}
	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
    }
	if ( OldTimeZone.length() > 0 ) {
		props.set ( "OldTimeZone", OldTimeZone );
	}
	if ( NewTimeZone.length() > 0 ) {
		props.set ( "NewTimeZone", NewTimeZone );
	}
	if ( ShiftTime.length() > 0 ) {
		props.set ( "ShiftTime", ShiftTime );
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
	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String OldTimeZone = getSelectedOldTimeZone();
	String NewTimeZone = getSelectedNewTimeZone();
	String ShiftTime = __ShiftTime_JComboBox.getSelected();
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "OldTimeZone", OldTimeZone );
	__command.setCommandParameter ( "NewTimeZone", NewTimeZone );
	__command.setCommandParameter ( "ShiftTime", ShiftTime );
}

/**
 * Get a list of valid time zones.
 */
private List<String> getZoneIds () {
	// Useful example:  https://www.mkyong.com/java8/java-display-all-zoneid-and-its-utc-offset/
	// TODO sam 2017-04-11 would be nice to indicate here whether a zone has daylight savings or is a standard time.
	LocalDateTime dt = LocalDateTime.now();
	List<String> zoneIds = new ArrayList<>();
	zoneIds.add("");
	for ( String zoneId: ZoneId.getAvailableZoneIds() ) { // These come back in order of numerical zone.
		ZoneId zone = ZoneId.of(zoneId);
		ZonedDateTime zdt = dt.atZone(zone);
		ZoneOffset zos = zdt.getOffset();
		// Replace "Z" with "+00:00"
		zoneIds.add(zos.getId().replace("Z","+00:00") + ", " + zoneId);
	}
	// Hard-code some known additions that are available by java.util.ZoneId.SHORT_IDS that are often encountered:
	// - Will need to look these up via the map later
	// - TODO smalers 2023-07-21 MST, etc. are deprecated because they are ambiguous
	//zoneIds.add("-05:00, EST");
	// CST is America/Chicago.
	//zoneIds.add("-07:00, MST");
	// PST is America/Los_Angeles.
	// Also add just the numeric versions to represent straight standard time.
	for ( int i = 1; i <= 12; i++ ) {
		zoneIds.add(String.format("-%02d:00, -%02d:00", i, i) );
	}
	for ( int i = 0; i <= 12; i++ ) {
		if ( i > 0 ) {
			zoneIds.add(String.format("+%02d:00, +%02d:00", i, i) );
		}
		else {
			zoneIds.add("+00:00, +00:00");
		}
	}
	return zoneIds;
}

/**
 * Get the selected new time zone.
 * The displayed list uses:  "+01:00, TimeZone_Name" or "TimeZone" if user-entered
 * @return the selected time zone as the name, without numeric equivalent
 */
private String getSelectedNewTimeZone () {
	String tz = __NewTimeZone_JComboBox.getSelected();
	int pos = tz.indexOf(",");
	if ( pos > 0 ) {
		return tz.substring(pos + 1).trim();
	}
	else {
		return tz.trim();
	}
}

/**
 * Get the selected old time zone.
 * The displayed list uses:  "+01:00, TimeZone_Name" or "TimeZone" if user-entered
 * @return the selected time zone as the name, without numeric equivalent
 */
private String getSelectedOldTimeZone () {
	String tz = __OldTimeZone_JComboBox.getSelected();
	int pos = tz.indexOf(",");
	if ( pos > 0 ) {
		return tz.substring(pos + 1).trim();
	}
	else {
		return tz.trim();
	}
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ChangeTimeZone_Command command ) {
	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Change the times for the time series to have a new time zone "
    	+ "and optionally shift the time values." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (	"The time zone for regular interval time series is saved with the time series period start and end." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (	"The time zone for irregular interval time series is also saved with each data value's date/time." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	    "It is OK if time series don't use a time zone, and time zone is typically not used for day interval or larger (dates)."),
	    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	    "The time zone is primarily used for reading/writing to databases."),
	    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	    "TSTool does not automatically shift data to align time zones because the date/time contents are assumed to align."),
	    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	    "Some commands require a time zone and therefore this command can be used to assign a time zone if it was not previously specified."),
	    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	    "Or, specify the time zone as a parameter for later commands that provide a parameter option."),
	    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
	    "Time zones that are listed have unique characteristics, such as daylight savings parameters for a region."),
	    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
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
    __EnsembleID_JComboBox = new SimpleJComboBox (); // true ); // Allow edits only if necessary.  Added MST from the SHORT_IDS map to hopefully meet most requirements.
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

    List<String> zoneIds = getZoneIds();
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Old time zone:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OldTimeZone_JComboBox = new SimpleJComboBox (true); // Allow edits, in particular for property.
	__OldTimeZone_JComboBox.setToolTipText("Specify the old time zone, can use ${Property}");
	// Sort to make it easy to find the time zone.
	Collections.sort(zoneIds);
	__OldTimeZone_JComboBox.setData(zoneIds);
	__OldTimeZone_JComboBox.select(0);
	__OldTimeZone_JComboBox.addActionAndKeyListeners(this, this);
    JGUIUtil.addComponent(main_JPanel, __OldTimeZone_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - specify the old time zone (default=from time series data)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time zone:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewTimeZone_JComboBox = new SimpleJComboBox (true); // Allow edits, in particular for property.
	__NewTimeZone_JComboBox.setToolTipText("Specify the new time zone, can use ${Property}");
	// Sort to make it easy to find the time zone.
	Collections.sort(zoneIds);
	__NewTimeZone_JComboBox.setData(zoneIds);
	__NewTimeZone_JComboBox.select(0);
	__NewTimeZone_JComboBox.addActionAndKeyListeners ( this, this );
    JGUIUtil.addComponent(main_JPanel, __NewTimeZone_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - specify to change the time zone (blank will remove the time zone)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Shift time?:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ShiftTime_JComboBox = new SimpleJComboBox (true); // Allow edits, in particular for property.
	__ShiftTime_JComboBox.setToolTipText("Whether to shift the time values to the new time zone");
	__ShiftTime_JComboBox.add("");
	__ShiftTime_JComboBox.add(this.__command._False);
	__ShiftTime_JComboBox.add(this.__command._True);
	__ShiftTime_JComboBox.select(0);
	__ShiftTime_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ShiftTime_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - shift times (default=" + this.__command._False + ")."),
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
	refresh ();

	// Panel for buttons.
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add (__cancel_JButton = new SimpleJButton("Cancel",this));
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
		// One of the combo boxes.
		refresh();
	}
}

public void keyReleased ( KeyEvent event ) {
	refresh();
}

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
	String TSList = "";
	String TSID = "";
    String EnsembleID = "";
	String OldTimeZone = "";
	String NewTimeZone = "";
	String ShiftTime = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command.
		TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
		EnsembleID = props.getValue ( "EnsembleID" );
		OldTimeZone = props.getValue("OldTimeZone");
		NewTimeZone = props.getValue("NewTimeZone");
		ShiftTime = props.getValue("ShiftTime");
        int [] index = new int[1]; // Used to select choice based on a token.
		if ( TSList == null ) {
			// Select default...
			__TSList_JComboBox.select ( 0 );
		}
		else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__TSList_JComboBox,	TSList, JGUIUtil.NONE, null, null ) ) {
				__TSList_JComboBox.select ( TSList );
			}
			else {
                Message.printWarning ( 1, routine,
				"Existing command references an invalid\nTSList value \"" +	TSList +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
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
        if ( JGUIUtil.isSimpleJComboBoxItem(__OldTimeZone_JComboBox, OldTimeZone, JGUIUtil.CHECK_SUBSTRINGS, ",", 1, index, true ) ) {
        	__OldTimeZone_JComboBox.select ( index[0] );
        }
		else {
	        // Automatically add to the list after the blank.
			if ( (OldTimeZone != null) && (OldTimeZone.length() > 0) ) {
				__OldTimeZone_JComboBox.insertItemAt ( OldTimeZone, 1 );
				// Select.
				__OldTimeZone_JComboBox.select ( OldTimeZone );
			}
			else {
	            // Select the blank.
				__OldTimeZone_JComboBox.select ( 0 );
			}
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__NewTimeZone_JComboBox, NewTimeZone, JGUIUtil.CHECK_SUBSTRINGS, ",", 1, index, true ) ) {
        	__NewTimeZone_JComboBox.select ( index[0] );
        }
		else {
	        // Automatically add to the list after the blank.
			if ( (NewTimeZone != null) && (NewTimeZone.length() > 0) ) {
				__NewTimeZone_JComboBox.insertItemAt ( NewTimeZone, 1 );
				// Select.
				__NewTimeZone_JComboBox.select ( NewTimeZone );
			}
			else {
	            // Select the blank.
				__NewTimeZone_JComboBox.select ( 0 );
			}
		}
        if ( ShiftTime == null ) {
            // Select default.
            __ShiftTime_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __ShiftTime_JComboBox, ShiftTime, JGUIUtil.NONE, null, null )) {
                __ShiftTime_JComboBox.select ( ShiftTime );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n" +
                "ShiftTime value \"" + ShiftTime + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		// Check the GUI state to make sure that components are enabled as expected (mainly enable/disable the TSID).
        // If disabled, the TSID will not be added as a parameter below.
		checkGUIState();
		if ( !__TSID_JComboBox.isEnabled() ) {
			// Not needed because some other method of specifying the time series is being used.
			TSID = null;
		}
	}
	// Regardless, reset the command from the fields.
	TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    OldTimeZone = getSelectedOldTimeZone();
    NewTimeZone = getSelectedNewTimeZone();
    ShiftTime = __ShiftTime_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
	props.add ( "EnsembleID=" + EnsembleID );
	props.add ( "OldTimeZone=" + OldTimeZone );
	props.add ( "NewTimeZone=" + NewTimeZone );
	props.add ( "ShiftTime=" + ShiftTime );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed and the dialog is closed.
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