package rti.tscommandprocessor.commands.hydrobase;

import java.awt.Color;
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
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;

import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

import DWR.DMI.HydroBaseDMI.HydroBaseDMI;
import DWR.DMI.HydroBaseDMI.HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel;

/**
Editor for the ReadHydroBase() command.
*/
public class ReadHydroBase_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null,// Cancel Button
			__ok_JButton = null;	// Ok Button
private ReadHydroBase_Command __command = null; // Command to edit
private TSFormatSpecifiersJPanel __Alias_JTextField = null; // Alias for time series, alias version
private JTextField __Location_JTextField, // Location part of TSID, TSID version
			__DataSource_JTextField, // Data source part of TSID, TSID version
			__DataType_JTextField, // Data type part of TSID, both versions
			__Interval_JTextField, // Interval part of TSID, both versions
			__InputName_JTextField, // Input name part of TSID, both versions
			__TSID_JTextField, // Full TSID, TSID version
			__InputStart_JTextField, // Text fields for query period, both versions.
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
private SimpleJComboBox __IfMissing_JComboBox;
			
private JTextArea __command_JTextArea = null;
private List __input_filter_JPanel_Vector = new Vector();
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
private HydroBaseDMI __hbdmi = null; // HydroBaseDMI to do queries.
private boolean __error_wait = false; // Is there an error to be cleared up
private boolean __first_time = true;
private boolean __ok = false; // Was OK pressed when closing the dialog.
private int __numWhere = (HydroBaseDMI.getSPFlexMaxParameters() - 2); // Number of visible where fields

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadHydroBase_JDialog ( JFrame parent, ReadHydroBase_Command command )
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
		if ( DataType.equalsIgnoreCase("DivTotal") ||
			DataType.equalsIgnoreCase("DivClass") ||
			DataType.equalsIgnoreCase("RelTotal") ||
			DataType.equalsIgnoreCase("RelClass") ) {
			/* TODO SAM 2006-04-28 Review code
			As per Ray Bennett always do the fill
			if ( Interval.equalsIgnoreCase("Day") ) {
				JGUIUtil.setEnabled ( __FillDailyDiv_JComboBox, true );
				JGUIUtil.setEnabled ( __FillDailyDivFlag_JTextField, true );
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
	String Alias = __Alias_JTextField.getText().trim();
	if ( Alias.length() > 0 ) {
		props.set ( "Alias", Alias );
	}
	String TSID = __TSID_JTextField.getText().trim();
	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
    String DataType = __DataType_JTextField.getText().trim();
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
	for ( int i = 1; i <= __numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.length() > 0 ) {
	        props.set ( "Where" + i, where );
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
	String FillUsingDivComments = __FillUsingDivComments_JComboBox.getSelected();
	if ( FillUsingDivComments.length() > 0 ) {
		props.set ( "FillUsingDivComments", FillUsingDivComments );
	}
	String FillUsingDivCommentsFlag = __FillUsingDivCommentsFlag_JTextField.getText().trim();
	if ( FillUsingDivCommentsFlag.length() > 0 ) {
		props.set ("FillUsingDivCommentsFlag",FillUsingDivCommentsFlag);
	}
    String IfMissing = __IfMissing_JComboBox.getSelected();
    if ( IfMissing.length() > 0 ) {
        props.set ("IfMissing",IfMissing);
    }
	try {
	    // This will warn the user...
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
{
	String Alias = __Alias_JTextField.getText().trim();
	__command.setCommandParameter ( "Alias", Alias );
	String TSID = __TSID_JTextField.getText().trim();
	__command.setCommandParameter ( "TSID", TSID );
	String DataType = __DataType_JTextField.getText().trim();
	__command.setCommandParameter ( "DataType", DataType );
	String Interval = __Interval_JTextField.getText().trim();
	__command.setCommandParameter ( "Interval", Interval );
	String InputName = __InputName_JTextField.getText().trim();
	__command.setCommandParameter ( "InputName", InputName );
	String delim = ";";
	for ( int i = 1; i <= __numWhere; i++ ) {
	    String where = getWhere ( i - 1 );
	    if ( where.startsWith(delim) ) {
	        where = "";
	    }
	    __command.setCommandParameter ( "Where" + i, where );
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
    String IfMissing = __IfMissing_JComboBox.getSelected();
    __command.setCommandParameter ( "IfMissing", IfMissing );
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
private void initialize ( JFrame parent, ReadHydroBase_Command command )
{	String routine = "readHydroBase_JDialog.initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	try { Object o = processor.getPropContents("HydroBaseDMIList");
		if ( o != null ) {
			// Use the first HydroBaseDMI instance, since input filter
			// information should be relatively consistent...
			List<HydroBaseDMI> v = (List)o;
			if ( v.size() > 0 ) {
				__hbdmi = v.get(0);
			}
			else {
				String message = "No HydroBase connection is available to use with command editing.\n" +
					"Make sure that HydroBase is open.";
				Message.printWarning(1, routine, message );
			}
		}
	}
	catch ( Exception e ){
		// Not fatal, but of use to developers.
		String message = "No HydroBase connection is available to use with command editing.\n" +
			"Make sure that HydroBase is open.";
		Message.printWarning(1, routine, message );
	}

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read 1+ time series from a HydroBase database, using options from the parameter groups below."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html><b>\"Where\" choices have only been fully implemented for structure time series " +
        "(e.g., DivTotal, DivClass, RelTotal, RelClass).</b></html>" ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to the HydroBase Input Type documentation for possible values." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for fill commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Filling with diversion comments applies only to diversion and reservoir records."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	
   	JPanel tsidJPanel = new JPanel();
   	tsidJPanel.setLayout(new GridBagLayout());
   	JPanel inputFilterJPanel = new JPanel();
   	inputFilterJPanel.setLayout(new GridBagLayout());
   	
    //JGUIUtil.addComponent(main_JPanel, inputFilterJPanel,
    //    0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JTextField = new JTextField ( "" );
	__DataType_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required (e.g., Streamflow, DivTotal)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Interval_JTextField = new JTextField ( "" );
	__Interval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required (e.g., Day, Month, Year)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input name:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputName_JTextField = new JTextField ( "" );
	__InputName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputName_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - HydroBase connection name (blank for default)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    tsidJPanel.setBorder(BorderFactory.createTitledBorder (
        BorderFactory.createLineBorder(Color.black),"Match a single time series..."));
    JGUIUtil.addComponent(main_JPanel, tsidJPanel,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    int y2 = 0;
    JGUIUtil.addComponent(tsidJPanel, new JLabel ( "Location:"),
        0, y2, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __Location_JTextField = new JTextField ( "" );
        __Location_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsidJPanel, __Location_JTextField,
        1, y2, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsidJPanel, new JLabel ( "For example, station or structure WDID."),
        3, y2, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(tsidJPanel, new JLabel ( "Data source:"),
        0, ++y2, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
        __DataSource_JTextField = new JTextField ( "" );
        __DataSource_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsidJPanel, __DataSource_JTextField,
        1, y2, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsidJPanel, new JLabel ( "For example: USGS, NWS."),
        3, y2, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(tsidJPanel, new JLabel ( "TSID (full):"),
        0, ++y2, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID_JTextField = new JTextField ( "" );
    __TSID_JTextField.setEditable ( false );
    __TSID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(tsidJPanel, __TSID_JTextField,
        1, y2, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(tsidJPanel, new JLabel ( "Created from above parameters."),
        3, y2, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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

	try {
	    // Structure with SFUT...
		// Number of filters is the maximum - 2 (data type and interval)
		__input_filter_HydroBase_structure_sfut_JPanel = new
			HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
			__hbdmi, true, __numWhere, -1 );
		__input_filter_HydroBase_structure_sfut_JPanel.setBorder(BorderFactory.createTitledBorder (
	        BorderFactory.createLineBorder(Color.black),"Or, match 1+ time series by specifying criteria below..."));
   		JGUIUtil.addComponent(main_JPanel,
			__input_filter_HydroBase_structure_sfut_JPanel,
			0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST );
		__input_filter_JPanel_Vector.add ( __input_filter_HydroBase_structure_sfut_JPanel);
		__input_filter_HydroBase_structure_sfut_JPanel.addEventListeners ( this );
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine, "Unable to initialize input filter for HydroBase structures with SFUT." );
		Message.printWarning ( 3, routine, e );
	}

	/* TODO SAM 2004-08-29 enable later
	try {
	    // Structure irrig summary TS...
		__input_filter_HydroBase_irrigts_JPanel = new
			HydroBase_GUI_StructureIrrigSummaryTS_InputFilter_JPanel ( __hbdmi );
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
	
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill using diversion comments:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> FillUsingDivComments_Vector = new Vector ( 3 );
	FillUsingDivComments_Vector.add ( "" );
	FillUsingDivComments_Vector.add ( __command._False );
	FillUsingDivComments_Vector.add ( __command._True );
	__FillUsingDivComments_JComboBox = new SimpleJComboBox ( false );
	__FillUsingDivComments_JComboBox.setData ( FillUsingDivComments_Vector);
	__FillUsingDivComments_JComboBox.select ( 0 );
	__FillUsingDivComments_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __FillUsingDivComments_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - whether to use diversion comments to fill more zero values (default=" + __command._False + ")."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill using diversion comments flag:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillUsingDivCommentsFlag_JTextField = new JTextField ( "" );
	__FillUsingDivCommentsFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel,
		__FillUsingDivCommentsFlag_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - string to flag filled diversion comment values."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "If missing:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> IfMissing_Vector = new Vector ( 3 );
    IfMissing_Vector.add ( "" );
    IfMissing_Vector.add ( __command._Ignore );
    IfMissing_Vector.add ( __command._Warn );
    __IfMissing_JComboBox = new SimpleJComboBox ( false );
    __IfMissing_JComboBox.setData ( IfMissing_Vector);
    __IfMissing_JComboBox.select ( 0 );
    __IfMissing_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __IfMissing_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - how to handle missing time series (blank=" + __command._Warn + ")."),
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

	setTitle ( "Edit " + __command.getCommandName() + " Command" );

	// Dialogs do not need to be resizable...
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Respond to ItemEvents.
*/
public void itemStateChanged ( ItemEvent event )
{
    refresh();
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
@return true if the edits were committed, false if the user canceled.
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
	String IfMissing = "";
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
		IfMissing = props.getValue ( "IfMissing" );
	    if ( Alias != null ) {
		    __Alias_JTextField.setText ( Alias );
	    }
		if ( TSID != null ) {
			try {
			    TSIdent tsident = new TSIdent ( TSID );
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
		if ( DataType != null ) {
			__DataType_JTextField.setText(DataType);
		}
		if ( Interval != null ) {
			__Interval_JTextField.setText(Interval);
		}
		if ( InputName != null ) {
			__InputName_JTextField.setText (
				InputName );
		}
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
		else {
		    if (	JGUIUtil.isSimpleJComboBoxItem(
				__FillUsingDivComments_JComboBox,
				FillUsingDivComments, JGUIUtil.NONE, null,
				null ) ) {
				__FillUsingDivComments_JComboBox.select (
				FillUsingDivComments);
			}
			else {
			    Message.printWarning ( 1, routine,
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
        if ( IfMissing == null ) {
            // Select default...
            __IfMissing_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __IfMissing_JComboBox, IfMissing, JGUIUtil.NONE, null, null ) ) {
                __IfMissing_JComboBox.select ( IfMissing);
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\n" +
                "IfMissing value \"" + IfMissing +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	// Regardless, reset the command from the fields...
	InputName = __InputName_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	String Location = __Location_JTextField.getText().trim();
	String DataSource = __DataSource_JTextField.getText().trim();
	StringBuffer b = new StringBuffer();
	b.append ( Location );
	b.append ( "." );
	b.append ( DataSource );
	b.append ( "." );
	b.append ( __DataType_JTextField.getText().trim() );
	b.append ( "." );
	b.append ( __Interval_JTextField.getText().trim() );
	b.append ( "~HydroBase" );
	if ( InputName.length() > 0 ) {
	    b.append ( "~" );
	    b.append ( InputName );
	}
	TSID = b.toString();
	if ( Location.equals("") || DataSource.equals("") ) {
	    // Not enough information so assume using the where filters
	    TSID = "";
	}
	__TSID_JTextField.setText ( TSID );
    DataType = __DataType_JTextField.getText().trim();
	Interval = __Interval_JTextField.getText().trim();
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	props.add ( "Alias=" + Alias );
	props.add ( "TSID=" + TSID );
    props.add ( "DataType=" + DataType );
	props.add ( "Interval=" + Interval );
	props.add ( "InputName=" + InputName );
	// Add the where clause...
	// TODO SAM 2004-08-26 eventually allow filter panels similar
	// to main GUI - right now only do water class...
	InputFilter_JPanel filter_panel = __input_filter_HydroBase_structure_sfut_JPanel;
	int nfg = filter_panel.getNumFilterGroups();
	String where;
	String delim = ";";	// To separate input filter parts
	for ( int ifg = 0; ifg < nfg; ifg ++ ) {
		where = filter_panel.toString(ifg,delim).trim();
		// Make sure there is a field that is being checked in a where clause...
		if ( (where.length() > 0) && !where.startsWith(delim) ) {
            // FIXME SAM 2010-11-01 The following discards '=' in the quoted string
            //props.add ( "Where" + (ifg + 1) + "=" + where );
            props.set ( "Where" + (ifg + 1), where );
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
	IfMissing = __IfMissing_JComboBox.getSelected();
    props.add ( "IfMissing=" + IfMissing );
	__command_JTextArea.setText( __command.toString ( props ) );

	// Check the GUI state to determine whether some controls should be disabled.

	checkGUIState();
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