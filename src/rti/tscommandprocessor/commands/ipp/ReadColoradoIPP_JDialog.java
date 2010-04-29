package rti.tscommandprocessor.commands.ipp;

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
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.InputFilter_JPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for he ReadColoradoIPP() command.
*/
public class ReadColoradoIPP_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private ReadColoradoIPP_Command __command = null;
private SimpleJComboBox __Subject_JComboBox;
private JTextField __SubjectName_JTextField; // Location part of TSID
private JTextField __DataSource_JTextField;// Data source part of TSID
private JTextField __DataType_JTextField;	// Data type part of TSID
private JTextField __SubDataType_JTextField;
private JTextField __Method_JTextField;
private JTextField __SubMethod_JTextField;
private JTextField __Scenario_JTextField;
private JTextField __InputName_JTextField;	// Input name part of TSID
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
			
private JTextArea __command_JTextArea = null; // Command as JTextArea
private List __input_filter_JPanel_Vector = new Vector();
private InputFilter_JPanel __input_filter_HydroBase_structure_sfut_JPanel =null;
						// InputFilter_JPanel for
						// HydroBase structure time
						// series - those that do use
						// SFUT.
private IppDMI __ippdmi = null; // Colorado IPP DMI to do queries.
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK was pressed when closing the dialog.
private int __numWhere = 0;//(HydroBaseDMI.getSPFlexMaxParameters() - 2); // Number of visible where fields

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadColoradoIPP_JDialog ( JFrame parent, ReadColoradoIPP_Command command )
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
	else {
		refresh();
	}
}

/**
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{
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
    String Subject = __Subject_JComboBox.getSelected();
    if ( Subject.length() > 0 ) {
        props.set ( "Subject", Subject );
    }
    String SubjectName = __SubjectName_JTextField.getText().trim();
    if ( SubjectName.length() > 0 ) {
        props.set ( "SubjectName", SubjectName );
    }
    String DataSource = __DataSource_JTextField.getText().trim();
    if ( DataSource.length() > 0 ) {
        props.set ( "DataSource", DataSource );
    }
    String DataType = __DataType_JTextField.getText().trim();
	if ( DataType.length() > 0 ) {
		props.set ( "DataType", DataType );
	}
    String SubDataType = __SubDataType_JTextField.getText().trim();
    if ( SubDataType.length() > 0 ) {
        props.set ( "SubDataType", SubDataType );
    }
	String Method = __Method_JTextField.getText().trim();
	if ( Method.length() > 0 ) {
		props.set ( "Method", Method );
	}
    String SubMethod = __SubMethod_JTextField.getText().trim();
    if ( SubMethod.length() > 0 ) {
        props.set ( "SubMethod", SubMethod );
    }
    String Scenario = __Scenario_JTextField.getText().trim();
    if ( Scenario.length() > 0 ) {
        props.set ( "Scenario", Scenario );
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
	String InputStart = __InputStart_JTextField.getText().trim();
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	String InputEnd = __InputEnd_JTextField.getText().trim();
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
    String Alias = __Alias_JTextField.getText().trim();
    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
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
{	String Subject = __Subject_JComboBox.getSelected();
    __command.setCommandParameter ( "Subject", Subject );
    String SubjectName = __SubjectName_JTextField.getText().trim();
    __command.setCommandParameter ( "SubjectName", SubjectName );
    String DataSource = __DataSource_JTextField.getText().trim();
    __command.setCommandParameter ( "DataSource", DataSource );
    String DataType = __DataType_JTextField.getText().trim();
	__command.setCommandParameter ( "DataType", DataType );
	String SubDataType = __SubDataType_JTextField.getText().trim();
    __command.setCommandParameter ( "SubDataType", SubDataType );
	String Method = __Method_JTextField.getText().trim();
	__command.setCommandParameter ( "Method", Method );
    String SubMethod = __SubMethod_JTextField.getText().trim();
    __command.setCommandParameter ( "SubMethod", SubMethod );
    String Scenario = __Scenario_JTextField.getText().trim();
    __command.setCommandParameter ( "Scenario", Scenario );
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
	String Alias = __Alias_JTextField.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__SubjectName_JTextField = null;
	__DataSource_JTextField = null;
	__DataType_JTextField = null;
	__Method_JTextField = null;
	__InputName_JTextField = null;
	__InputStart_JTextField = null;
	__InputEnd_JTextField = null;
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
private void initialize ( JFrame parent, ReadColoradoIPP_Command command )
{	String routine = "ReadColoradoIPP_JDialog.initialize";
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();

	try {
	    Object o = processor.getPropContents("ColoradoIppDMIList");
		if ( o != null ) {
			// Use the first ColoradoIppDMI instance, since input filter
			// information should be relatively consistent...
			List v = (List)o;
			if ( v.size() > 0 ) {
				__ippdmi = (IppDMI)v.get(0);
			}
			else {
				String message = "No Colorado IPP connection is available to use with command editing.\n" +
					"Make sure that Colorado IPP is open.";
				Message.printWarning(1, routine, message );
			}
		}
	}
	catch ( Exception e ){
		// Not fatal, but of use to developers.
		String message = "No Colorado IPP connection is available to use with command editing.\n" +
			"Make sure that Colorado IPP is open.";
		Message.printWarning(1, routine, message );
	}

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Read one or more time series from the State of Colorado's IPP database."),
    	0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Constrain the query by specifying time series metadata." ), 
    	0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Refer to the Colorado IPP Database Input Type documentation for possible values." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the period will limit data that are available " +
		"for fill commands but can increase performance." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If not specified, the period defaults to the query period."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Subject:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> subjectList = null;
    if ( __ippdmi == null ) {
        subjectList = new Vector();
    }
    else {
        subjectList = __ippdmi.getSubjectList();
    }
    subjectList.add ( 0, "" );
    __Subject_JComboBox = new SimpleJComboBox( subjectList );
    __Subject_JComboBox.select(0);
    __Subject_JComboBox.addActionListener(this);
    JGUIUtil.addComponent(main_JPanel, __Subject_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - main data object (Provider, Project, County)."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Subject name:"),
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SubjectName_JTextField = new JTextField ( 20 );
	__SubjectName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SubjectName_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - for example, provider, project, county name."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Data source:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataSource_JTextField = new JTextField ( 20 );
	__DataSource_JTextField.addKeyListener ( this );
   	JGUIUtil.addComponent(main_JPanel, __DataSource_JTextField,
   	    1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - source of data."),
   	    3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data type:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DataType_JTextField = new JTextField ( 20 );
	__DataType_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DataType_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - for example: WaterDemand."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data subtype:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SubDataType_JTextField = new JTextField ( 20 );
    __SubDataType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SubDataType_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - for example: Total."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Method:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Method_JTextField = new JTextField ( 20 );
    __Method_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Method_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - for example: observed, estimated."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Submethod:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SubMethod_JTextField = new JTextField ( 20 );
    __SubMethod_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SubMethod_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - ."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Scenario:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Scenario_JTextField = new JTextField ( 20 );
    __Scenario_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Scenario_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - for example low, middle, high."),
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
/*
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data interval:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Interval_JTextField = new JTextField ( "" );
	__Interval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Interval_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - currently always assumed to be Year."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		*/

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input name:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputName_JTextField = new JTextField ( "" );
	__InputName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputName_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - Colorado IPP connection name (blank for default)."),
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	int buffer = 3;
	Insets insets = new Insets(0,buffer,0,0);
	/* TODO SAM 2004-08-29 - enable later - right now it slows things down
	try {
	    // Add input filters for stations...

		__input_filter_HydroBase_station_JPanel = new
			HydroBase_GUI_StationGeolocMeasType_InputFilter_JPanel (__hbdmi );
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
		"Unable to initialize input filter for HydroBase stations." );
		Message.printWarning ( 2, routine, e );
	}

	try {
	    // Structure total (no SFUT)...

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
/*
	try {
	    // Structure with SFUT...

		PropList filter_props = new PropList ( "" );
		// Number of filters is the maximum - 2 (data type and interval)
		filter_props.set ( "NumFilterGroups=" + __numWhere );
		__input_filter_HydroBase_structure_sfut_JPanel = new
			HydroBase_GUI_StructureGeolocStructMeasType_InputFilter_JPanel (
			__ippdmi, true, filter_props );
   			JGUIUtil.addComponent(main_JPanel,
			__input_filter_HydroBase_structure_sfut_JPanel,
			0, ++y, 7, 1, 0.0, 0.0, insets, GridBagConstraints.HORIZONTAL,
			GridBagConstraints.WEST );
		__input_filter_JPanel_Vector.add ( __input_filter_HydroBase_structure_sfut_JPanel);
		__input_filter_HydroBase_structure_sfut_JPanel.
			addEventListeners ( this );
	}
	catch ( Exception e ) {
		Message.printWarning ( 2, routine,
		"Unable to initialize input filter for HydroBase structures with SFUT." );
		Message.printWarning ( 2, routine, e );
	}
	*/

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Input start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - YYYY, override the global input start."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __InputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - YYYY, override the global input end."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.setToolTipText("%L for location, %T for data type.");
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
{	//String routine = "ReadColoradoIPP_JDialog.refresh";
	__error_wait = false;
	String Subject = "";
	String SubjectName = "";
    String DataSource = "";
	String DataType = "";
	String SubDataType = "";
    String Method = "";
    String SubMethod = "";
    String Scenario = "";
	//String Interval = "";
	String InputName = "";
	String filter_delim = ";";
	String InputStart = "";
	String InputEnd = "";
	String Alias = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		Subject = props.getValue ( "Subject" );
		SubjectName = props.getValue ( "SubjectName" );
		DataSource = props.getValue ( "DataSource" );
		DataType = props.getValue ( "DataType" );
		SubDataType = props.getValue ( "SubDataType" );
		Method = props.getValue ( "Method" );
        SubMethod= props.getValue ( "SubMethod" );
        Scenario= props.getValue ( "Scenario" );
		//Interval = props.getValue ( "Interval" );
		InputName = props.getValue ( "InputName" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
		Alias = props.getValue ( "Alias" );
        if ( JGUIUtil.isSimpleJComboBoxItem( __Subject_JComboBox, Subject, JGUIUtil.NONE, null, null ) ) {
            __Subject_JComboBox.select ( Subject );
        }
        else {
            // Select the blank...
            __Subject_JComboBox.select ( 0 );
        }
        if ( SubjectName != null ) {
            __SubjectName_JTextField.setText(SubjectName);
        }
        if ( DataSource != null ) {
            __DataSource_JTextField.setText(DataSource);
        }
		if ( DataType != null ) {
			__DataType_JTextField.setText(DataType);
		}
        if ( SubDataType != null ) {
            __SubDataType_JTextField.setText(SubDataType);
        }
        if ( Method != null ) {
            __Method_JTextField.setText(Method);
        }
        if ( SubMethod != null ) {
            __SubMethod_JTextField.setText(SubMethod);
        }
        if ( Scenario != null ) {
            __Scenario_JTextField.setText(Scenario);
        }
//		if ( Interval != null ) {
//			__Interval_JTextField.setText(Interval);
//		}
		if ( InputName != null ) {
			__InputName_JTextField.setText (InputName );
		}
		/*
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
		*/
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
	}
	// Regardless, reset the command from the fields...
	InputName = __InputName_JTextField.getText().trim();
	Subject = __Subject_JComboBox.getSelected();
	SubjectName = __SubjectName_JTextField.getText().trim();
	DataSource = __DataSource_JTextField.getText().trim();
    DataType = __DataType_JTextField.getText().trim();
    SubDataType = __SubDataType_JTextField.getText().trim();
    Method = __Method_JTextField.getText().trim();
    SubMethod = __SubMethod_JTextField.getText().trim();
    Scenario = __Scenario_JTextField.getText().trim();
	//Interval = __Interval_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	// Regardless, reset the command from the fields...
	props = new PropList ( __command.getCommandName() );
	props.add ( "Subject=" + Subject );
	props.add ( "SubjectName=" + SubjectName );
	props.add ( "DataSource=" + DataSource );
    props.add ( "DataType=" + DataType );
    props.add ( "SubDataType=" + SubDataType );
    props.add ( "Method=" + Method );
    props.add ( "SubMethod=" + SubMethod );
    props.add ( "Scenario=" + Scenario );
	//props.add ( "Interval=" + Interval );
	props.add ( "InputName=" + InputName );
	/*
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
			props.add ( "Where" + (ifg + 1) + "=" + where );
		}
	}
	*/
	InputStart = __InputStart_JTextField.getText().trim();
	props.add ( "InputStart=" + InputStart );
	InputEnd = __InputEnd_JTextField.getText().trim();
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Alias=" + Alias );
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