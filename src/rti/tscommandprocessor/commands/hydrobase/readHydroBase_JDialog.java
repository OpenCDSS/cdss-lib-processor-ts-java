// ----------------------------------------------------------------------------
// readHydroBase_JDialog - editor for TS Alias = readHydroBase() and
//					readHydroBase().
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History: 
//
// 2004-08-25	Steven A. Malers, RTi	Initial version (copy and modify
//					TSreadDateValue).
// 2004-08-26	SAM, RTi		Combine the TS X = read... and read...
//					versions of the command editors into
//					this dialog.
// 2004-08-29	SAM, RTi		* Add all the input filter panels but
//					  still only enable the structure with
//					  SFUT.
//					* Change "SheetName" to
//					  "SheetNameWISFormat" to reflect the
//					  objects being used.
// 2004-08-31	SAM, RTi		* Increase the number of where clauses
//					  to 5.
// 2005-04-12	SAM, RTi		* Add InputName parameter to allow
//					  reading from a specific HydroBase
//					  connection.
//					* Convert the command JTextField to a
//					  scrolled JTextArea because of the
//					  length of the command.
// 2006-04-21	SAM, RTi		* Update to use command class.
//					* Add parameters to fill with diversion
//					  comments and fill daily diversions.
// 2006-04-27	SAM, RTi		* Add support for RelTotal and RelClass
//					  filling with carry forward and
//					  comments.
//					* As per Ray Bennett, filling with
//					  daily carry forward should ALWAYS
//					  occur (no user option) - comment out
//					  code in case he changes his mind.
//					* As per Ray Bennett, filling with
//					  diversion comments should NOT be the
//					  default.
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-02-26	SAM, RTi		Clean up code based on Eclipse feedback.
// ----------------------------------------------------------------------------

package rti.tscommandprocessor.commands.hydrobase;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.TS.TSIdent;

import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel;

/**
The readHydroBase_JDialog edits the readHydroBase() and
TS Alias = readHydroBase() command.
*/
public class readHydroBase_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private SimpleJButton	__cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private readHydroBase_Command __command = null; // Command to edit
private JTextField	__Alias_JTextField=null,// Alias for time series, alias version
			__Location_JTextField,	// Location part of TSID, non-alias version
			__DataSource_JTextField,// Data source part of TSID, non-alias version
			__DataType_JTextField,	// Data type part of TSID, non-alias version
			__Interval_JTextField,	// Interval part of TSID, non-alias version
			__InputName_JTextField,	// Input name part of TSID, non-alias version
			__TSID_JTextField,	// Full TSID, non-alias version
			__InputStart_JTextField,// Text fields for query period, both versions.
			__InputEnd_JTextField,	
			/* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
			__FillDailyDivFlag_JTextField,
						// Flag to use indicating
						// daily diversion values filled
						// with carry forward.
			*/
			__FillUsingDivCommentsFlag_JTextField;
						// Flag to use indicating
						// diversion values filled with
						// diversion comments.
			/* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
private SimpleJComboBox	__FillDailyDiv_JComboBox,
						// Indicate whether to fill
						// daily diversions using the
						// HydroBase carry forward
						// algorithm.
			*/
private SimpleJComboBox	__FillUsingDivComments_JComboBox;
						// Indicate whether to fill
						// diversion time series with
						// diversion comments.
			
private JTextArea	__command_JTextArea = null;
						// Command as JTextArea
private Vector __input_filter_JPanel_Vector = new Vector();
//TODO SAM 2007-02-17 Need to enable CASS when resources allow
//private InputFilter_JPanel __input_filter_HydroBase_CASS_JPanel = null;
						// InputFilter_JPanel for
						// HydroBase CASS agricultural
						// crop statistics time series.
// TODO SAM 2007-02-17 Need to enable NASS when resources allow
//private InputFilter_JPanel __input_filter_HydroBase_NASS_JPanel = null;
						// InputFilter_JPanel for
						// HydroBase NASS agricultural
						// crop statistics time series.
//TODO SAM 2007-02-17 Need to enable irrig acres when resources allow
//private InputFilter_JPanel __input_filter_HydroBase_irrigts_JPanel = null;
						// InputFilter_JPanel for
						// HydroBase structure
						// irrig_summary_ts time series.
//TODO SAM 2007-02-17 Need to enable station when resources allow
//private InputFilter_JPanel __input_filter_HydroBase_station_JPanel = null;
						// InputFilter_JPanel for
						// HydroBase station time
						// series.
//SAM 2007-02-17 Need to enable structures when resources allow
//private InputFilter_JPanel __input_filter_HydroBase_structure_JPanel = null;
						// InputFilter_JPanel for
						// HydroBase structure time
						// series - those that do not
						// use SFUT.
private InputFilter_JPanel __input_filter_HydroBase_structure_sfut_JPanel =null;
						// InputFilter_JPanel for
						// HydroBase structure time
						// series - those that do use
						// SFUT.
// SAM 2007-02-17 Need to enable WIS when resources allow
//private InputFilter_JPanel __input_filter_HydroBase_WIS_JPanel =null;
						// InputFilter_JPanel for
						// HydroBase WIS time
						// series.
private HydroBaseDMI	__hbdmi = null;		// HydroBaseDMI to do queries.
private boolean		__error_wait = false;	// Is there an error that we
						// are waiting to be cleared up
						// or Cancel?
private boolean		__first_time = true;
private boolean		__use_alias = false;	// Indicates if one time series
						// is being read (TX Alias =...)
						// or multiple time series.
private boolean		__ok = false;		// Indicates whether OK was
						// pressed when closing the
						// dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public readHydroBase_JDialog ( JFrame parent, Command command )
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
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if (
		/* TODO SAM 2006-04-28 Review code
		As per Ray Bennett always do the fill
		(o == __FillDailyDiv_JComboBox) ||
		*/
		(o == __FillUsingDivComments_JComboBox) ) {
		// combo boxes...
		refresh();
	}
}

/**
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{	// If "AllMatchingTSID", enable the list.
	// Otherwise, clear and disable...
	if ( __DataType_JTextField != null ) {
		String DataType = __DataType_JTextField.getText().trim();
		// TODO SAM 2007-02-17 Add checks for interval
		//String Interval = "";
		//if (  __Interval_JTextField != null ) {
		//	Interval = __Interval_JTextField.getText().trim();
		//}
		// TODO SAM 2006-04-25 Remove hard-coded types
		// Should not need to hard-code these data types but there
		// is no better way to do it at the moment.
		if (	DataType.equalsIgnoreCase("DivTotal") ||
			DataType.equalsIgnoreCase("DivClass") ||
			DataType.equalsIgnoreCase("RelTotal") ||
			DataType.equalsIgnoreCase("RelClass") ) {
			/* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
			if ( Interval.equalsIgnoreCase("Day") ) {
				JGUIUtil.setEnabled (
				__FillDailyDiv_JComboBox, true );
				JGUIUtil.setEnabled (
					__FillDailyDivFlag_JTextField, true );
			}
			else {	JGUIUtil.setEnabled (
				__FillDailyDiv_JComboBox, false );
				JGUIUtil.setEnabled (
					__FillDailyDivFlag_JTextField, false );
			}
			*/
			JGUIUtil.setEnabled ( __FillUsingDivComments_JComboBox, true );
			JGUIUtil.setEnabled ( __FillUsingDivCommentsFlag_JTextField, true );
		}
		else {	/* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
			JGUIUtil.setEnabled ( __FillDailyDiv_JComboBox, false );
			JGUIUtil.setEnabled (
				__FillDailyDivFlag_JTextField, false );
			*/
			JGUIUtil.setEnabled ( __FillUsingDivComments_JComboBox, false );
			JGUIUtil.setEnabled ( __FillUsingDivCommentsFlag_JTextField, false );
		}
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	__error_wait = false;
	// Check parameters for the two command versions...
	if ( __use_alias ) {
		String Alias = __Alias_JTextField.getText().trim();
		if ( Alias.length() > 0 ) {
			props.set ( "Alias", Alias );
		}
		String TSID = __TSID_JTextField.getText().trim();
		if ( TSID.length() > 0 ) {
			props.set ( "TSID", TSID );
		}
	}
	else {	String DataType = __DataType_JTextField.getText().trim();
		if ( DataType.length() > 0 ) {
			props.set ( "DataType", DataType );
		}
		String Interval = __Interval_JTextField.getText().trim();
		if ( Interval.length() > 0 ) {
			props.set ( "Interval", Interval );
		}
		String InputName = __InputName_JTextField.getText().trim();
		if ( InputName.length() > 0 ) {
			props.set ( "InputName", InputName );
		}
		String Where1 = getWhere ( 0 );
		if ( Where1.length() > 0 ) {
			props.set ( "Where1", Where1 );
		}
		String Where2 = getWhere ( 1 );
		if ( Where2.length() > 0 ) {
			props.set ( "Where2", Where2 );
		}
		String Where3 = getWhere ( 2 );
		if ( Where3.length() > 0 ) {
			props.set ( "Where3", Where3 );
		}
		String Where4 = getWhere ( 3 );
		if ( Where4.length() > 0 ) {
			props.set ( "Where4", Where4 );
		}
		String Where5 = getWhere ( 4 );
		if ( Where5.length() > 0 ) {
			props.set ( "Where5", Where5 );
		}
		String Where6 = getWhere ( 5 );
		if ( Where6.length() > 0 ) {
			props.set ( "Where6", Where6 );
		}
	}
	// Both command types use these...
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
	// Additional parameters used to help provide additional data...
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	String FillDailyDiv =__FillDailyDiv_JComboBox.getSelected();
	if ( FillDailyDiv.length() > 0 ) {
		props.set ( "FillDailyDiv", FillDailyDiv );
	}
	String FillDailyDivFlag =__FillDailyDivFlag_JTextField.getText().trim();
	if ( FillDailyDivFlag.length() > 0 ) {
		props.set ( "FillDailyDivFlag", FillDailyDivFlag );
	}
	*/
	String FillUsingDivComments =
		__FillUsingDivComments_JComboBox.getSelected();
	if ( FillUsingDivComments.length() > 0 ) {
		props.set ( "FillUsingDivComments", FillUsingDivComments );
	}
	String FillUsingDivCommentsFlag =
		__FillUsingDivCommentsFlag_JTextField.getText().trim();
	if ( FillUsingDivCommentsFlag.length() > 0 ) {
		props.set ("FillUsingDivCommentsFlag",FillUsingDivCommentsFlag);
	}
	try {	// This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
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
{	if ( __use_alias ) {
		String Alias = __Alias_JTextField.getText().trim();
		__command.setCommandParameter ( "Alias", Alias );
		String TSID = __TSID_JTextField.getText().trim();
		__command.setCommandParameter ( "TSID", TSID );
	}
	else {	String DataType = __DataType_JTextField.getText().trim();
		__command.setCommandParameter ( "DataType", DataType );
		String Interval = __Interval_JTextField.getText().trim();
		__command.setCommandParameter ( "Interval", Interval );
		String InputName = __InputName_JTextField.getText().trim();
		__command.setCommandParameter ( "InputName", InputName );
		String delim = ";";
		String Where1 = getWhere ( 0 );
		if ( Where1.startsWith(delim) ) {
			Where1 = "";
		}
		__command.setCommandParameter ( "Where1", Where1 );
		String Where2 = getWhere ( 1 );
		if ( Where2.startsWith(delim) ) {
			Where2 = "";
		}
		__command.setCommandParameter ( "Where2", Where2 );
		String Where3 = getWhere ( 2 );
		if ( Where3.startsWith(delim) ) {
			Where3 = "";
		}
		__command.setCommandParameter ( "Where3", Where3 );
		String Where4 = getWhere ( 3 );
		if ( Where4.startsWith(delim) ) {
			Where4 = "";
		}
		__command.setCommandParameter ( "Where4", Where4 );
		String Where5 = getWhere ( 4 );
		if ( Where5.startsWith(delim) ) {
			Where5 = "";
		}
		__command.setCommandParameter ( "Where5", Where5 );
		String Where6 = getWhere ( 5 );
		if ( !Where6.startsWith(delim) ) {
			Where6 = "";
		}
		__command.setCommandParameter ( "Where6", Where6 );
	}
	// Both versions of the commands use these...
	String InputStart = __InputStart_JTextField.getText().trim();
	__command.setCommandParameter ( "InputStart", InputStart );
	String InputEnd = __InputEnd_JTextField.getText().trim();
	__command.setCommandParameter ( "InputEnd", InputEnd );
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	String FillDailyDiv = __FillDailyDiv_JComboBox.getSelected();
	__command.setCommandParameter ( "FillDailyDiv", FillDailyDiv );
	String FillDailyDivFlag =__FillDailyDivFlag_JTextField.getText().trim();
	__command.setCommandParameter ( "FillDailyDivFlag", FillDailyDivFlag );
	*/
	String FillUsingDivComments = __FillUsingDivComments_JComboBox.getSelected();
	__command.setCommandParameter (	"FillUsingDivComments", FillUsingDivComments );
	String FillUsingDivCommentsFlag = __FillUsingDivCommentsFlag_JTextField.getText().trim();
	__command.setCommandParameter (	"FillUsingDivCommentsFlag", FillUsingDivCommentsFlag );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__TSID_JTextField = null;

	__Alias_JTextField = null;

	__Location_JTextField = null;
	__DataSource_JTextField = null;
	__DataType_JTextField = null;
	__Interval_JTextField = null;
	__InputName_JTextField = null;
	__InputStart_JTextField = null;
	__InputEnd_JTextField = null;
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	__FillDailyDiv_JComboBox = null;
	__FillDailyDivFlag_JTextField = null;
	*/
	__FillUsingDivComments_JComboBox = null;
	__FillUsingDivCommentsFlag_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Return the "WhereN" parameter for the requested input filter.
@return the "WhereN" parameter for the requested input filter.
@param ifg the Input filter to process (zero index).
*/
private String getWhere ( int ifg )
{
	// TODO SAM 2006-04-24 Need to enable other input filter panels
	String delim = ";";	// To separate input filter parts
	InputFilter_JPanel filter_panel = __input_filter_HydroBase_structure_sfut_JPanel;
	String where = filter_panel.toString(ifg,delim).trim();
	return where;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	String routine = "readHydroBase_JDialog.initialize";
	__command = (readHydroBase_Command)command;
	CommandProcessor processor = __command.getCommandProcessor();

	// Determine whether this is the "TS Alias =" version of the command.
	PropList props = __command.getCommandParameters();
	String Alias = props.getValue("Alias");
	__use_alias = false;
	if (Alias == null || Alias.trim().equalsIgnoreCase("")) {
		if (((readHydroBase_Command)command).getCommandString().trim().toUpperCase().startsWith("TS ")) {
			// This indicates that a new command is being edited
			// and the properties have not been defined yet.  The
			// command string will have been set at initialization
			// but not parsed (because this dialog interactively
			// parses and checks input).
		   	__use_alias = true;
		}
		else {
            // A new command with no alias...
			__use_alias = false;
		}
	}
	else {
        // An existing command that uses the alias.
		__use_alias = true;
	}
	// Tell the new command what version it is because it was not parsed at
	// initialization (doing so might prematurely warn the user)...
	__command.setUseAlias ( __use_alias );

	try { Object o = processor.getPropContents("HydroBaseDMIList");
		if ( o != null ) {
			// Use the first HydroBaseDMI instance, since input filter
			// information should be relatively consistent...
			Vector v = (Vector)o;
			if ( v.size() > 0 ) {
				__hbdmi = (HydroBaseDMI)v.elementAt(0);
			}
			else {
				String message =
					"No HydroBase connection is available to use with command editing.\n" +
					"Make sure that HydroBase is open.";
				Message.printWarning(1, routine, message );
			}
		}
	}
	catch ( Exception e ){
		// Not fatal, but of use to developers.
		String message =
			"No HydroBase connection is available to use with command editing.\n" +
			"Make sure that HydroBase is open.";
		Message.printWarning(1, routine, message );
	}

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	if ( __use_alias ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read a single time series from the HydroBase database."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	else {	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read one or more time series from the HydroBase database."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The data type and interval must be selected.  Constrain the " +
		"query using the \"where\" clauses, if necessary." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command is fully enabled ONLY FOR STRUCTURE TIME SERIES "+
		"(e.g., DivTotal, DivClass, RelTotal, RelClass)."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to the HydroBase Input Type documentation for " +
		"possible values." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for fill commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the period defaults to the query period."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Filling with diversion comments applies only to diversion " +
		"and reservoir release time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	if ( __use_alias ) {
        	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Time series alias:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__Alias_JTextField = new JTextField ( 30 );
		__Alias_JTextField.addKeyListener ( this );
		JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
		1, y, 3, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Location:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__Location_JTextField = new JTextField ( "" );
		__Location_JTextField.addKeyListener ( this );
        	JGUIUtil.addComponent(main_JPanel, __Location_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"For example, station or structure ID."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Data source:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__DataSource_JTextField = new JTextField ( "" );
		__DataSource_JTextField.addKeyListener ( this );
        	JGUIUtil.addComponent(main_JPanel, __DataSource_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"For example: USGS, NWS."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JTextField = new JTextField ( "" );
	__DataType_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"For example: Streamflow."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Interval_JTextField = new JTextField ( "" );
	__Interval_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Interval_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"For example: 6Hour, Day, Month."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ("Input name:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputName_JTextField = new JTextField ( "" );
	__InputName_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __InputName_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"HydroBase connection name (blank for default)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	if ( !__use_alias ) {
		int buffer = 3;
		Insets insets = new Insets(0,buffer,0,0);
		/* TODO SAM 2004-08-29 - enable later -
		right now it slows things down
		try {	// Add input filters for stations...

			__input_filter_HydroBase_station_JPanel = new
				HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel (
				__hbdmi );
       			JGUIUtil.addComponent(main_JPanel,
				__input_filter_HydroBase_station_JPanel,
				0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST );
			__input_filter_JPanel_Vector.addElement (
				__input_filter_HydroBase_station_JPanel );
			__input_filter_HydroBase_station_JPanel.
				addEventListeners ( this );
			__input_filter_HydroBase_station_JPanel.setVisible (
				false );
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine,
			"Unable to initialize input filter for HydroBase" +
			" stations." );
			Message.printWarning ( 2, routine, e );
		}

		try {	// Structure total (no SFUT)...

			PropList filter_props = new PropList ( "" );
			filter_props.set ( "NumFilterGroups=6" );
			__input_filter_HydroBase_structure_JPanel = new
				HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
				__hbdmi, false, filter_props );
       			JGUIUtil.addComponent(main_JPanel,
				__input_filter_HydroBase_structure_JPanel,
				0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST );
			__input_filter_JPanel_Vector.addElement (
				__input_filter_HydroBase_structure_JPanel);
			__input_filter_HydroBase_structure_JPanel.
				addEventListeners ( this );
			__input_filter_HydroBase_structure_JPanel.setVisible (
				false );
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine,
			"Unable to initialize input filter for HydroBase" +
			" structures." );
			Message.printWarning ( 2, routine, e );
		}
		*/

		try {	// Structure with SFUT...

			PropList filter_props = new PropList ( "" );
			filter_props.set ( "NumFilterGroups=6" );
			__input_filter_HydroBase_structure_sfut_JPanel = new
				HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
				__hbdmi, true, filter_props );
       			JGUIUtil.addComponent(main_JPanel,
				__input_filter_HydroBase_structure_sfut_JPanel,
				0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST );
			__input_filter_JPanel_Vector.addElement (
				__input_filter_HydroBase_structure_sfut_JPanel);
			__input_filter_HydroBase_structure_sfut_JPanel.
				addEventListeners ( this );
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine,
			"Unable to initialize input filter for HydroBase" +
			" structures with SFUT." );
			Message.printWarning ( 2, routine, e );
		}

		/* TODO SAM 2004-08-29 enable later
		try {	// Structure irrig summary TS...

			__input_filter_HydroBase_irrigts_JPanel = new
				HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel (
				__hbdmi );
       			JGUIUtil.addComponent(main_JPanel,
				__input_filter_HydroBase_irrigts_JPanel,
				0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST );
			__input_filter_JPanel_Vector.addElement (
				__input_filter_HydroBase_irrigts_JPanel);
			__input_filter_HydroBase_irrigts_JPanel.
				addEventListeners ( this );
			__input_filter_HydroBase_irrigts_JPanel.setVisible (
				false );
		}
		catch ( Exception e ) {
			Message.printWarning ( 2, routine,
			"Unable to initialize input filter for HydroBase" +
			" irrigation summary time series - old database?" );
			Message.printWarning ( 2, routine, e );
		}

		try {	// CASS agricultural statistics...

			__input_filter_HydroBase_CASS_JPanel = new
				HydroBase_GUI_AgriculturalCASSCropStats_InputFilter_JPanel (
				__hbdmi );
       			JGUIUtil.addComponent(main_JPanel,
				__input_filter_HydroBase_CASS_JPanel,
				0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST );
			__input_filter_JPanel_Vector.addElement (
				__input_filter_HydroBase_CASS_JPanel);
			__input_filter_HydroBase_CASS_JPanel.
				addEventListeners ( this );
			__input_filter_HydroBase_CASS_JPanel.setVisible(false );
		}
		catch ( Exception e ) {
			// Agricultural_CASS_crop_stats probably not in
			// HydroBase...
			Message.printWarning ( 2, routine,
			"Unable to initialize input filter for HydroBase" +
			" agricultural_CASS_crop_stats - old database?" );
			Message.printWarning ( 2, routine, e );
		}

		// NASS agricultural statistics...

		try {	__input_filter_HydroBase_NASS_JPanel = new
				HydroBase_GUI_AgriculturalNASSCropStats_InputFilter_JPanel (
				__hbdmi );
       			JGUIUtil.addComponent(main_JPanel,
				__input_filter_HydroBase_NASS_JPanel,
				0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST );
			__input_filter_JPanel_Vector.addElement (
				__input_filter_HydroBase_NASS_JPanel);
			__input_filter_HydroBase_NASS_JPanel.
				addEventListeners ( this );
			__input_filter_HydroBase_NASS_JPanel.setVisible( false);
		}
		catch ( Exception e ) {
			// Agricultural_NASS_crop_stats probably not in
			// HydroBase...
			Message.printWarning ( 2, routine,
			"Unable to initialize input filter for HydroBase" +
			" agricultural_NASS_crop_stats - old database?" );
			Message.printWarning ( 2, routine, e );
		}

		try {	// Water information sheets...

			__input_filter_HydroBase_WIS_JPanel = new
				HydroBase_GUI_SheetNameWISFormat_InputFilter_JPanel (
				__hbdmi );
       			JGUIUtil.addComponent(main_JPanel,
				__input_filter_HydroBase_WIS_JPanel,
				0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
				GridBagConstraints.WEST );
			__input_filter_JPanel_Vector.addElement (
				__input_filter_HydroBase_WIS_JPanel);
			__input_filter_HydroBase_WIS_JPanel.
				addEventListeners ( this );
			__input_filter_HydroBase_WIS_JPanel.
				setVisible ( false );
		}
		catch ( Exception e ) {
			// WIS tables probably not in HydroBase...
			Message.printWarning ( 2, routine,
			"Unable to initialize input filter for HydroBase" +
			" WIS - data tables not in database?" );
			Message.printWarning ( 2, routine, e );
		}

		*/
	}

	if ( __use_alias ) {
        	JGUIUtil.addComponent(main_JPanel, new JLabel ( "TSID (full):"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__TSID_JTextField = new JTextField ( "" );
		__TSID_JTextField.setEditable ( false );
		__TSID_JTextField.addKeyListener ( this );
        	JGUIUtil.addComponent(main_JPanel, __TSID_JTextField,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	}

	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Period to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputStart_JTextField = new JTextField ( 15 );
	__InputStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "to" ), 
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__InputEnd_JTextField = new JTextField ( 15 );
	__InputEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
		4, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	/* TODO SAM 2006-04-27
	As per Ray Bennett this should always be done.
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Fill daily diversions:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	Vector FillDailyDiv_Vector = new Vector ( 3 );
	FillDailyDiv_Vector.addElement ( "" );
	FillDailyDiv_Vector.addElement ( __command._False );
	FillDailyDiv_Vector.addElement ( __command._True );
	__FillDailyDiv_JComboBox = new SimpleJComboBox ( false );
	__FillDailyDiv_JComboBox.setData ( FillDailyDiv_Vector );
	__FillDailyDiv_JComboBox.select ( 0 );
	__FillDailyDiv_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __FillDailyDiv_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Whether to carry forward daily diversions (blank=True)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Fill daily diversions flag:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillDailyDivFlag_JTextField = new JTextField ( "" );
	__FillDailyDivFlag_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __FillDailyDivFlag_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"1-character flag to indicate daily diversion filled values."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	*/

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Fill using diversion comments:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	Vector FillUsingDivComments_Vector = new Vector ( 3 );
	FillUsingDivComments_Vector.addElement ( "" );
	FillUsingDivComments_Vector.addElement ( __command._False );
	FillUsingDivComments_Vector.addElement ( __command._True );
	__FillUsingDivComments_JComboBox = new SimpleJComboBox ( false );
	__FillUsingDivComments_JComboBox.setData ( FillUsingDivComments_Vector);
	__FillUsingDivComments_JComboBox.select ( 0 );
	__FillUsingDivComments_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(main_JPanel, __FillUsingDivComments_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Whether to use diversion comments to fill more zero values " +
		"(blank=False)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Fill using diversion comments flag:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillUsingDivCommentsFlag_JTextField = new JTextField ( "" );
	__FillUsingDivCommentsFlag_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel,
		__FillUsingDivCommentsFlag_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"1-character flag to indicate filled diversion comment " +
		"values."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);


        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea (4,50);
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	if ( __use_alias ) {
		setTitle ( "Edit TS Alias = " + __command.getCommandName() + " Command" );
	}
	else {	setTitle ( "Edit " + __command.getCommandName() + " Command" );
	}

	// Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	refresh();
}

/**
Need this to properly capture key events, especially deletes.
*/
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
{	return __ok;
}

/**
Refresh the command string from the dialog contents.
*/
private void refresh ()
{	String routine = "readHydroBase_JDialog.refresh";
	String Alias = "";
	__error_wait = false;
	String TSID = "";
	String DataType = "";
	String Interval = "";
	String InputName = "";
	String filter_delim = ";";
	String InputStart = "";
	String InputEnd = "";
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	String FillDailyDiv = "";
	String FillDailyDivFlag = "";
	*/
	String FillUsingDivComments = "";
	String FillUsingDivCommentsFlag = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		Alias = props.getValue ( "Alias" );
		TSID = props.getValue ( "TSID" );
		DataType = props.getValue ( "DataType" );
		Interval = props.getValue ( "Interval" );
		InputName = props.getValue ( "InputName" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		/* TODO SAM 2006-04-28 Review code
		As per Ray Bennett always do the fill
		FillDailyDiv = props.getValue ( "FilLDailyDiv" );
		FillDailyDivFlag = props.getValue ( "FilLDailyDivFlag" );
		*/
		FillUsingDivComments = props.getValue ( "FillUsingDivComments" );
		FillUsingDivCommentsFlag = props.getValue ( "FillUsingDivCommentsFlag" );
		if ( __use_alias && (Alias != null) ) {
			__Alias_JTextField.setText ( Alias );
		}
		if ( __use_alias && (TSID != null) ) {
			try {	TSIdent tsident = new TSIdent ( TSID );
				if ( __Location_JTextField != null ) {
					__Location_JTextField.setText (	tsident.getLocation() );
				}
				if ( __DataSource_JTextField != null ) {
					__DataSource_JTextField.setText(tsident.getSource() );
				}
				__DataType_JTextField.setText (	tsident.getType() );
				__Interval_JTextField.setText (	tsident.getInterval() );
				__InputName_JTextField.setText (tsident.getInputName() );
			}
			catch ( Exception e ) {
				// For now do nothing.
			}
		}
		if ( !__use_alias ) {
			if ( DataType != null ) {
				__DataType_JTextField.setText(DataType);
			}
			if ( Interval != null ) {
				__Interval_JTextField.setText(Interval);
			}
		}
		if ( !__use_alias ) {
			if ( InputName != null ) {
				__InputName_JTextField.setText (
					InputName );
			}
		}
		if ( !__use_alias ) {
			InputFilter_JPanel filter_panel = __input_filter_HydroBase_structure_sfut_JPanel;
			int nfg = filter_panel.getNumFilterGroups();
			String where;
			for ( int ifg = 0; ifg < nfg; ifg ++ ) {
				where = props.getValue ( "Where" + (ifg + 1) );
				if ( (where != null) && (where.length() > 0) ) {
					// Set the filter...
					try {	filter_panel.setInputFilter (ifg, where, filter_delim );
					}
					catch ( Exception e ) {
						Message.printWarning ( 1, routine,
						"Error setting where information using \"" + where + "\"" );
						Message.printWarning ( 2, routine, e );
					}
				}
			}
		} // end !__use_alias
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
		/* TODO SAM 2006-04-28 Review code
		As per Ray Bennett always do the fill
		if ( FillDailyDiv == null ) {
			// Select default...
			__FillDailyDiv_JComboBox.select ( 0 );
		}
		else {	if (	JGUIUtil.isSimpleJComboBoxItem(
				__FillDailyDiv_JComboBox,
				FillDailyDiv, JGUIUtil.NONE, null, null ) ) {
				__FillDailyDiv_JComboBox.select ( FillDailyDiv);
			}
			else {	Message.printWarning ( 1, routine,
				"Existing command " +
				"references an invalid\nFillDailyDiv value \"" +
				FillDailyDiv +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( FillDailyDivFlag != null ) {
			__FillDailyDivFlag_JTextField.setText(FillDailyDivFlag);
		}
		*/
		if ( FillUsingDivComments == null ) {
			// Select default...
			__FillUsingDivComments_JComboBox.select ( 0 );
		}
		else {	if (	JGUIUtil.isSimpleJComboBoxItem(
				__FillUsingDivComments_JComboBox,
				FillUsingDivComments, JGUIUtil.NONE, null,
				null ) ) {
				__FillUsingDivComments_JComboBox.select (
				FillUsingDivComments);
			}
			else {	Message.printWarning ( 1, routine,
				"Existing command references an invalid\n" +
				"FillUsingDivComments value \"" +
				FillUsingDivComments +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( FillUsingDivCommentsFlag != null ) {
			__FillUsingDivCommentsFlag_JTextField.setText(FillUsingDivCommentsFlag);
		}
	}
	// Regardless, reset the command from the fields...
	InputName = __InputName_JTextField.getText().trim();
	if ( __use_alias ) {
		Alias = __Alias_JTextField.getText().trim();
		if ( InputName.length() == 0 ) {
			// No input name...
			TSID =	__Location_JTextField.getText().trim() + "." +
				__DataSource_JTextField.getText().trim() + "." +
				__DataType_JTextField.getText().trim() + "." +
				__Interval_JTextField.getText().trim() +
				"~HydroBase";
		}
		else {	// Input name is specified...
			TSID =	__Location_JTextField.getText().trim() + "." +
				__DataSource_JTextField.getText().trim() + "." +
				__DataType_JTextField.getText().trim() + "." +
				__Interval_JTextField.getText().trim() +
				"~HydroBase~" + InputName;
		}
		__TSID_JTextField.setText ( TSID );
	}
	else {	DataType = __DataType_JTextField.getText().trim();
		Interval = __Interval_JTextField.getText().trim();
	}
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	if ( __use_alias ) {
		props.add ( "Alias=" + Alias );
		props.add ( "TSID=" + TSID );
	}
	else {	props.add ( "DataType=" + DataType );
		props.add ( "Interval=" + Interval );
		props.add ( "InputName=" + InputName );
		// Add the where clause...
		// TODO SAM 2004-08-26 eventually allow filter panels similar
		// to main GUI - right now only do water class...
		InputFilter_JPanel filter_panel =
			__input_filter_HydroBase_structure_sfut_JPanel;
		int nfg = filter_panel.getNumFilterGroups();
		String where;
		String delim = ";";	// To separate input filter parts
		for ( int ifg = 0; ifg < nfg; ifg ++ ) {
			where = filter_panel.toString(ifg,delim).trim();
			// Make sure there is a field that is being checked in
			// a where clause...
			if (	(where.length() > 0) &&
				!where.startsWith(delim) ) {
				props.add ( "Where" + (ifg + 1) +
					"=" + where );
			}
		}
	}
	InputStart = __InputStart_JTextField.getText().trim();
	props.add ( "InputStart=" + InputStart );
	InputEnd = __InputEnd_JTextField.getText().trim();
	props.add ( "InputEnd=" + InputEnd );
	/* TODO SAM 2006-04-28 Review code
	As per Ray Bennett always do the fill
	FillDailyDiv = __FillDailyDiv_JComboBox.getSelected();
	props.add ( "FillDailyDiv=" + FillDailyDiv );
	FillDailyDivFlag = __FillDailyDivFlag_JTextField.getText().trim();
	props.add ( "FillDailyDivFlag=" + FillDailyDivFlag );
	*/
	FillUsingDivComments = __FillUsingDivComments_JComboBox.getSelected();
	props.add ( "FillUsingDivComments=" + FillUsingDivComments );
	FillUsingDivCommentsFlag =__FillUsingDivCommentsFlag_JTextField.getText().trim();
	props.add ( "FillUsingDivCommentsFlag=" + FillUsingDivCommentsFlag );
	__command_JTextArea.setText( __command.toString ( props ) );

	// Check the GUI state to determine whether some controls should be disabled.

	checkGUIState();
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
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

} // end readHydroBase_JDialog
