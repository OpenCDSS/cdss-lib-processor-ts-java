// ReadStateMod_JDialog - Editor for ReadStateMod() command.

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

package rti.tscommandprocessor.commands.statemod;

import java.awt.Color;
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

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
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

import java.io.File;
import java.util.List;
import java.util.Vector;

import DWR.StateMod.StateMod_DiversionRight;
import DWR.StateMod.StateMod_GUIUtil;
import DWR.StateMod.StateMod_InstreamFlowRight;
import DWR.StateMod.StateMod_ReservoirRight;
import DWR.StateMod.StateMod_WellRight;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.TimeInterval;

/**
Editor for ReadStateMod() command.
*/
@SuppressWarnings("serial")
public class ReadStateMod_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, WindowListener
{
	
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null;// File browse button
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null; // Convert input file between relative and absolute paths.
private ReadStateMod_Command __command = null;	// Command to edit
private JTextArea __command_JTextArea=null;// Command as TextField
private String __working_dir = null;	// Working directory.
private JTextField __InputFile_JTextField = null;
private JTextField __InputStart_JTextField = null;
private JTextField __InputEnd_JTextField = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null; // Alias for time series.
private JTabbedPane __file_JTabbedPane = null;
private JLabel __Interval_JLabel = null;
private SimpleJComboBox __Interval_JComboBox = null; // Interval for water rights output.
private JLabel __SpatialAggregation_JLabel = null;
private SimpleJComboBox	__SpatialAggregation_JComboBox = null; // aggregation for water rights output.
private JLabel __ParcelYear_JLabel = null;
private JTextField __ParcelYear_JTextField = null; // Parcel year for parcel total water rights
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Whether OK was pressed when closing the dialog.

/**
Command editor constructor.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
public ReadStateMod_JDialog ( JFrame parent, ReadStateMod_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select StateMod Time Series or Right File");
		StateMod_GUIUtil.addTimeSeriesFilenameFilters(fc, TimeInterval.UNKNOWN, true);
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				// Convert path to relative path by default.
				try {
					__InputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, __command.getCommandName() + "_JDialog", "Error converting file to relative path." );
				}
		        checkGUIState(); // To enable/disable parameters
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadStateMod");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals(__AddWorkingDirectory) ) {
			__InputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir, __InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __InputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, __command.getCommandName() + "_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else if ( (__Interval_JComboBox != null) && (o == __Interval_JComboBox) ) {
		refresh ();
	}
	else if ( (__SpatialAggregation_JComboBox != null) && (o == __SpatialAggregation_JComboBox) ) {
		refresh ();
	}
	else {
	    // Other combo boxes, etc...
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
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{   String inputFile = __InputFile_JTextField.getText().trim();
    if ( StateMod_DiversionRight.isDiversionRightFile(inputFile) ||
        StateMod_InstreamFlowRight.isInstreamFlowRightFile(inputFile) ||
        StateMod_ReservoirRight.isReservoirRightFile(inputFile) ||
        StateMod_WellRight.isWellRightFile(inputFile) ) {
        JGUIUtil.setEnabled(__Interval_JLabel, true);
        JGUIUtil.setEnabled(__Interval_JComboBox, true);
        JGUIUtil.setEnabled(__SpatialAggregation_JLabel, true);
        JGUIUtil.setEnabled(__SpatialAggregation_JComboBox, true);
    }
    else {
        JGUIUtil.setEnabled(__Interval_JLabel, false);
        JGUIUtil.setEnabled(__Interval_JComboBox, false);
        JGUIUtil.setEnabled(__SpatialAggregation_JLabel, false);
        JGUIUtil.setEnabled(__SpatialAggregation_JComboBox, false);
    }
    if ( StateMod_WellRight.isWellRightFile(inputFile) ) {
        JGUIUtil.setEnabled(__ParcelYear_JLabel, true);
        JGUIUtil.setEnabled(__ParcelYear_JTextField, true);
    }
    else {
        JGUIUtil.setEnabled(__ParcelYear_JLabel, false);
        JGUIUtil.setEnabled(__ParcelYear_JTextField, false); 
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String Interval = __Interval_JComboBox.getSelected();
	String SpatialAggregation = __SpatialAggregation_JComboBox.getSelected();
	String ParcelYear = __ParcelYear_JTextField.getText().trim();
	__error_wait = false;
	if ( InputFile.length() > 0 ) {
		props.set ( "InputFile", InputFile );
	}
	if ( InputStart.length() > 0 ) {
		props.set ( "InputStart", InputStart );
	}
	if ( InputEnd.length() > 0 ) {
		props.set ( "InputEnd", InputEnd );
	}
    if (Alias.length() > 0) {
        props.set("Alias", Alias);
    }
	if ( Interval.length() > 0 ) {
		props.set ( "Interval", Interval );
	}
	if ( SpatialAggregation.length() > 0 ) {
		props.set ( "SpatialAggregation", SpatialAggregation );
	}
	if ( ParcelYear.length() > 0 ) {
		props.set ( "ParcelYear", ParcelYear );
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
{	String InputFile = __InputFile_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String Interval = __Interval_JComboBox.getSelected();
	String SpatialAggregation = __SpatialAggregation_JComboBox.getSelected();
	String ParcelYear = __ParcelYear_JTextField.getText().trim();
	__command.setCommandParameter ( "InputFile", InputFile );
	__command.setCommandParameter ( "InputStart", InputStart );
	__command.setCommandParameter ( "InputEnd", InputEnd );
	__command.setCommandParameter ( "Interval", Interval );
	__command.setCommandParameter ( "SpatialAggregation", SpatialAggregation );
	__command.setCommandParameter ( "ParcelYear", ParcelYear );
	__command.setCommandParameter ( "Alias", Alias );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, ReadStateMod_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	// TODO SAM 2007-02-18 Evaluate whether to support
	//__use_alias = false;

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );
	
	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Read all the time series from a StateMod text input or output file (use ReadStateModB() to read binary output file)."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The data source and data type will be blank in the resulting time series identifier (TSID)."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full or relative path (relative to working directory)." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
      	JGUIUtil.addComponent(main_JPanel, new JLabel ("The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("StateMod file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("StateMod file to read.  Can use ${Property} notation.");
	__InputFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel InputFile_JPanel = new JPanel();
	InputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(InputFile_JPanel, __InputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(InputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(InputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(main_JPanel, InputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.getTextField().setToolTipText(
         "Use %L for location, %T for data type, ${ts:property} for time series property.");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - use %L for location, ${ts:property}, etc. (default=no alias)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    __file_JTabbedPane = new JTabbedPane ();
    __file_JTabbedPane.setBorder(
        BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
        "Specify parameters specific to file type" ));
    JGUIUtil.addComponent(main_JPanel, __file_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for STM files
    int yStm = -1;
    JPanel stm_JPanel = new JPanel();
    stm_JPanel.setLayout( new GridBagLayout() );
    __file_JTabbedPane.addTab ( "Standard Time Series", stm_JPanel );

    JGUIUtil.addComponent(stm_JPanel, new JLabel (
        "Standard time series files are used for daily, monthly, and average monthly time series."),
        0, ++yStm, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(stm_JPanel, new JLabel (
        "This format includes most of the StateMod input time series files (*.ddh, *.rih, *.ddm, etc.) and *.stm files."),
        0, ++yStm, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(stm_JPanel, new JLabel (
        "Use the ReadStateModB() command to read time series from the binary output files."),
        0, ++yStm, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for water right files
    int yRight = -1;
    JPanel right_JPanel = new JPanel();
    right_JPanel.setLayout( new GridBagLayout() );
    __file_JTabbedPane.addTab ( "Water Right Input File", right_JPanel );

    JGUIUtil.addComponent(right_JPanel, new JLabel (
        "Water right files can be read to create a cumulative step-function of decrees for a location."),
        0, ++yRight, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(right_JPanel, new JLabel (
        "The following parameters are enabled if the StateMod file has a file extension for water right files (*.wer, *.ddr, etc.)."),
        0, ++yRight, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(right_JPanel, new JLabel (
        "Specify the desired interval for the output water right time series."),
        0, ++yRight, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(right_JPanel, new JLabel (
        "Water rights can be aggregated by location, for example well rights associated with parcel."),
        0, ++yRight, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(right_JPanel, new JLabel (
        "Well right/parcel relationships can change every year.  " +
        "Specify a single year to extract rights for that year."),
        0, ++yRight, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    __Interval_JLabel = new JLabel ("Interval:");
    JGUIUtil.addComponent(right_JPanel, __Interval_JLabel,
      	0, ++yRight, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> Interval_Vector = new Vector<String>();
   	Interval_Vector.add ( "" );
   	Interval_Vector.add ( __command._Day );
   	Interval_Vector.add ( __command._Month );
   	Interval_Vector.add ( __command._Year );
	// TODO SAM 2007-05-16 Evaluate whether needed
   	//Interval_Vector.addElement ( __command._Irregular );
   	__Interval_JComboBox = new SimpleJComboBox(false);
   	__Interval_JComboBox.setData ( Interval_Vector );
   	__Interval_JComboBox.select ( 0 );
   	__Interval_JComboBox.addActionListener (this);
    JGUIUtil.addComponent(right_JPanel, __Interval_JComboBox,
    	1, yRight, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(right_JPanel, new JLabel (
    	"Optional for rights - interval for resulting time series (default=" + __command._Year + ")."),
    	3, yRight, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    __SpatialAggregation_JLabel = new JLabel ("Spatial aggregation:");
    JGUIUtil.addComponent(right_JPanel, __SpatialAggregation_JLabel,
       0, ++yRight, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> SpatialAggregation_Vector = new Vector<String>();
   	SpatialAggregation_Vector.add ( "" );
   	SpatialAggregation_Vector.add ( __command._Location );
   	SpatialAggregation_Vector.add ( __command._Parcel );
   	SpatialAggregation_Vector.add ( __command._None );
   	__SpatialAggregation_JComboBox = new SimpleJComboBox(false);
   	__SpatialAggregation_JComboBox.setData ( SpatialAggregation_Vector );
   	__SpatialAggregation_JComboBox.select ( 0 );
   	__SpatialAggregation_JComboBox.addActionListener (this);
    JGUIUtil.addComponent(right_JPanel, __SpatialAggregation_JComboBox,
    	1, yRight, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(right_JPanel, new JLabel (
    	"Optional for rights - spatial aggregation (default=" + __command._Location + ")."),
    	3, yRight, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    __ParcelYear_JLabel = new JLabel ( "Parcel year:");
    JGUIUtil.addComponent(right_JPanel, __ParcelYear_JLabel, 
   		0, ++yRight, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
   	__ParcelYear_JTextField = new JTextField (20);
   	__ParcelYear_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(right_JPanel, __ParcelYear_JTextField,
        1, yRight, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(right_JPanel, new JLabel (
	    "Optional for well rights - read a single irrigated lands year (default=read all)."),
	    3, yRight, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for StateMod output files
    int yOut = -1;
    JPanel out_JPanel = new JPanel();
    out_JPanel.setLayout( new GridBagLayout() );
    __file_JTabbedPane.addTab ( "Output (.x*) File", out_JPanel );

    JGUIUtil.addComponent(out_JPanel, new JLabel (
        "StateMod output files have standard file extensions, which are used to determine how to read the file."),
        0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
        "Currently only the .xop file is handled.  " +
        "Use the ReadStateModB() command to read from the binary output files."),
        0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
        "Periods in the location ID will be replaced with underscores so as to not corrupt the TSID."),
        0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);  
   
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
	0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
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
	
    checkGUIState();
    refresh();  // Sets the __path_JButton status

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable...
	setResizable ( false );
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
		checkInput ();
		if ( !__error_wait ) {
			response ( false );
		}
	}
}

public void keyReleased ( KeyEvent event )
{	if ( event.getSource() == __InputFile_JTextField ) {
        checkGUIState(); // To enable/disable parameters based on filename
    }
    refresh();
}

public void keyTyped ( KeyEvent event )
{	refresh();
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
{	String routine = "ReadStateMod_JDialog.refresh";
	String InputFile = "";
	String InputStart = "";
	String InputEnd = "";
    String Alias = "";
	String Interval = "";
	String SpatialAggregation = "";
	String ParcelYear = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		InputFile = props.getValue ( "InputFile" );
		InputStart = props.getValue ( "InputStart" );
		InputEnd = props.getValue ( "InputEnd" );
	    Alias = props.getValue ( "Alias" );
		Interval = props.getValue ( "Interval" );
		SpatialAggregation = props.getValue ( "SpatialAggregation" );
		ParcelYear = props.getValue ( "ParcelYear" );
		if ( InputFile != null ) {
			__InputFile_JTextField.setText ( InputFile );
		}
		if ( InputStart != null ) {
			__InputStart_JTextField.setText ( InputStart );
		}
		if ( InputEnd != null ) {
			__InputEnd_JTextField.setText ( InputEnd );
		}
        if (Alias != null ) {
            __Alias_JTextField.setText(Alias.trim());
        }
		if ( (Interval == null) || Interval.equals("") ) {
			// Select the first item
			__Interval_JComboBox.select ( 0 );
		}
		else {
		    __file_JTabbedPane.setSelectedIndex(1);
		    if ( JGUIUtil.isSimpleJComboBoxItem(__Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
				__Interval_JComboBox.select ( Interval );
			}
			else {
			    Message.printWarning ( 1, routine,
					"Existing command references an invalid\nInterval value \"" +
					Interval + "\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( (SpatialAggregation == null) || SpatialAggregation.equals("") ) {
			// Select the first item
			__SpatialAggregation_JComboBox.select ( 0 );
		}
		else {
		    __file_JTabbedPane.setSelectedIndex(1);
		    if ( JGUIUtil.isSimpleJComboBoxItem(
				__SpatialAggregation_JComboBox, SpatialAggregation, JGUIUtil.NONE, null, null ) ) {
				__SpatialAggregation_JComboBox.select ( SpatialAggregation );
			}
			else {
			    Message.printWarning ( 1, routine,
					"Existing command references an invalid\nSpatialAggregation value \"" +
					SpatialAggregation + "\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if ( ParcelYear != null ) {
			__ParcelYear_JTextField.setText ( ParcelYear );
			if ( !ParcelYear.equals("") ) {
			    __file_JTabbedPane.setSelectedIndex(1);
			}
		}
		// Ensure that components are properly enabled/disabled...
		checkGUIState();
	}
	// Regardless, reset the command from the fields...
	InputFile = __InputFile_JTextField.getText().trim();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	Interval = __Interval_JComboBox.getSelected();
	SpatialAggregation = __SpatialAggregation_JComboBox.getSelected();
	ParcelYear = __ParcelYear_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "InputFile=" + InputFile );
	props.add ( "InputStart=" + InputStart );
	props.add ( "InputEnd=" + InputEnd );
	props.add ( "Alias=" + Alias );
	props.add ( "Interval=" + Interval );
	props.add ( "SpatialAggregation=" + SpatialAggregation );
	props.add ( "ParcelYear=" + ParcelYear );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		if ( (InputFile != null) && !InputFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( InputFile );
			if ( f.isAbsolute() ) {
				__path_JButton.setText ( __RemoveWorkingDirectory );
				__path_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__path_JButton.setText ( __AddWorkingDirectory );
		    	__path_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path_JButton.setEnabled(false);
		}
	}
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
