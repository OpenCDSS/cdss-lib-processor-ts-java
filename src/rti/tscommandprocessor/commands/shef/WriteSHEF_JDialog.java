// WriteSHEF_JDialog - editor for WriteSHEF command

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

package rti.tscommandprocessor.commands.shef;

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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class WriteSHEF_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
    
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __path_JButton = null;
private WriteSHEF_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JTextField __LocationID_JTextField = null;
private JTextArea __DataTypePELookup_JTextArea = null;
private JTextField __TimeZone_JTextField = null;
private JTextField __ObservationTime_JTextField = null;
private JTextField __CreationDate_JTextField = null;
private JTextField __Duration_JTextField = null;
private JTextField __Precision_JTextField = null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __Append_JComboBox = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
// TODO SAM 2007-12-10 Evaluate if other parameters are needed like the following
//private JTextField __MissingValue_JTextField = null; // Missing value for output
private JTextArea __command_JTextArea=null;
private String __working_dir = null; // Working directory.
private boolean __error_wait = false; // Is there an error that needs to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether the user has pressed OK to close the dialog.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteSHEF_JDialog ( JFrame parent, WriteSHEF_Command command )
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
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle("Select SHEF File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("shef",	"SHEF File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String path = fc.getSelectedFile().getPath(); 
	
			if (path != null) {
				// Convert path to relative path by default.
				try {
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, __command.getCommandName() + "_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory );
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
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
			__OutputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,
			__OutputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
                __OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
				__OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "WriteSHEF_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else if ( (__TSList_JComboBox != null) && (o == __TSList_JComboBox) ) {
		checkGUIState();
		refresh ();
	}
	else {
        // Other combo boxes, etc...
		refresh();
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
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String LocationID = __LocationID_JTextField.getText().trim();
	String DataTypePELookup = __DataTypePELookup_JTextArea.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Append = __Append_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	String TimeZone = __TimeZone_JTextField.getText().trim();
	String ObservationTime = __ObservationTime_JTextField.getText().trim();
	String CreationDate = __CreationDate_JTextField.getText().trim();
	String Duration = __Duration_JTextField.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
	__error_wait = false;
	if ( TSList.length() > 0 ) {
		props.set ( "TSList", TSList );
	}
	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
	if ( LocationID.length() > 0 ) {
		props.set ( "LocationID", LocationID );
	}
	if ( OutputFile.length() > 0 ) {
		props.set ( "OutputFile", OutputFile );
	}
    if ( Append.length() > 0 ) {
        props.set ( "Append", Append );
    }
	if ( OutputStart.length() > 0 ) {
		props.set ( "OutputStart", OutputStart );
	}
	if ( OutputEnd.length() > 0 ) {
		props.set ( "OutputEnd", OutputEnd );
	}
    if ( DataTypePELookup.length() > 0 ) {
        props.set ( "DataTypePELookup", DataTypePELookup );
    }
	if ( TimeZone.length() > 0 ) {
		props.set ( "TimeZone", TimeZone );
	}
    if ( ObservationTime.length() > 0 ) {
        props.set ( "ObservationTime", ObservationTime );
    }
    if ( CreationDate.length() > 0 ) {
        props.set ( "CreationDate", CreationDate );
    }
    if ( Duration.length() > 0 ) {
        props.set ( "Duration", Duration );
    }
	if ( Precision.length() > 0 ) {
		props.set ( "Precision", Precision );
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
{	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String LocationID = __LocationID_JTextField.getText().trim();
	String DataTypePELookup = __DataTypePELookup_JTextArea.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Append = __Append_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String TimeZone = __TimeZone_JTextField.getText().trim();
    String ObservationTime = __ObservationTime_JTextField.getText().trim();
    String CreationDate = __CreationDate_JTextField.getText().trim();
    String Duration = __Duration_JTextField.getText().trim();
    String Precision = __Precision_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "LocationID", LocationID );
	__command.setCommandParameter ( "DataTypePELookup", DataTypePELookup );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "Append", Append );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
	__command.setCommandParameter ( "TimeZone", TimeZone );
    __command.setCommandParameter ( "ObservationTime", ObservationTime );
    __command.setCommandParameter ( "CreationDate", CreationDate );
    __command.setCommandParameter ( "Duration", Duration );
	__command.setCommandParameter ( "Precision", Precision );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteSHEF_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write time series to a Standard Hydrologic Exchange Format (SHEF) .A format file - " +
		"refer to SHEF documentation for data format and nomenclature details."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"A sample record is as follows:"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    .A loc1 20071031 PS DH2400/DUE/QI 5.00"),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
	    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for input time series
    int yInputTS = -1;
    JPanel inputTS_JPanel = new JPanel();
    inputTS_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Input Time Series", inputTS_JPanel );

   	JGUIUtil.addComponent(inputTS_JPanel, new JLabel (
		"The time series to process are indicated using the TS list."),
		0, ++yInputTS, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(inputTS_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yInputTS, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
   	
    __TSList_JComboBox = new SimpleJComboBox(false);
    yInputTS = CommandEditorUtil.addTSListToEditorDialogPanel ( this, inputTS_JPanel, __TSList_JComboBox, yInputTS );
    // Remove Ensemble because not supported
    __TSList_JComboBox.remove(TSListType.ENSEMBLE_ID.toString());

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
	__TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yInputTS = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, inputTS_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yInputTS );
   	
    // Panel for SHEF data
    int ySHEF = -1;
    JPanel shef_JPanel = new JPanel();
    shef_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "SHEF Data", shef_JPanel );
    
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
	    "The SHEF physical element (PE) code will normally be determined from the operational environment; " +
	    "however, specify the data type to PE lookup information if necessary."),
	    0, ++ySHEF, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
        "The observation time, if specified, will be used for all data - specify as an integer or " +
        "include the character prefix (e.g., DH1200)."),
        0, ++ySHEF, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
        "The creation time, if specified, will be used for all data - specify as an integer or " +
        "include the character prefix (e.g., CD20091231)."),
        0, ++ySHEF, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
        "The duration, if specified, will be used for all data - specify as an integer or " +
        "include the character prefix (e.g., DVH06)."),
        0, ++ySHEF, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(shef_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++ySHEF, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(shef_JPanel, new JLabel ( "DataType,PE;DataType,PE;...:"),
        0, ++ySHEF, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataTypePELookup_JTextArea = new JTextArea ( 4, 55 );
    __DataTypePELookup_JTextArea.setToolTipText("Specify time series data type and SHEF PE code as DataType,PE;DataType,PE, can use ${Property} notation");
    __DataTypePELookup_JTextArea.setLineWrap ( true );
    __DataTypePELookup_JTextArea.setWrapStyleWord ( true );
    JGUIUtil.addComponent(shef_JPanel, new JScrollPane(__DataTypePELookup_JTextArea),
        1, ySHEF, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(shef_JPanel, new JLabel ( "Location ID:" ),
		0, ++ySHEF, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__LocationID_JTextField = new JTextField ( "", 20 );
	__LocationID_JTextField.setToolTipText("Specify the location ID for output, can use ${Property} and ${ts:Property} notation");
	__LocationID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(shef_JPanel, __LocationID_JTextField,
		1, ySHEF, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
		"Optional - location ID for output (default=from time series)."),
		3, ySHEF, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(shef_JPanel, new JLabel ( "Time zone:" ),
		0, ++ySHEF, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TimeZone_JTextField = new JTextField ( "", 20 );
	__TimeZone_JTextField.setToolTipText("Specify the time zone string for output (no conversion occurs), can use ${Property} notation");
	__TimeZone_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(shef_JPanel, __TimeZone_JTextField,
		1, ySHEF, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
		"Optional - time zone for output (default=from time series or Z)."),
		3, ySHEF, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(shef_JPanel, new JLabel ( "Observation time:" ),
        0, ++ySHEF, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ObservationTime_JTextField = new JTextField ( "", 20 );
    __ObservationTime_JTextField.setToolTipText("Specify the observation time, can use ${Property} notation");
    __ObservationTime_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(shef_JPanel, __ObservationTime_JTextField,
        1, ySHEF, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
        "Optional - observation time (default=from data)."),
        3, ySHEF, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(shef_JPanel, new JLabel ( "Creation date:" ),
        0, ++ySHEF, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CreationDate_JTextField = new JTextField ( "", 20 );
    __CreationDate_JTextField.setToolTipText("Specify the creation date, can use ${Property} notation");
    __CreationDate_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(shef_JPanel, __CreationDate_JTextField,
        1, ySHEF, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
        "Optional - creation date (default=not used)."),
        3, ySHEF, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(shef_JPanel, new JLabel ( "Duration:" ),
        0, ++ySHEF, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Duration_JTextField = new JTextField ( "", 20 );
    __Duration_JTextField.setToolTipText("Specify the duration, can use ${Property} notation");
    __Duration_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(shef_JPanel, __Duration_JTextField,
        1, ySHEF, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
        "Optional - duration (default=determined from time series if irregular)."),
        3, ySHEF, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(shef_JPanel, new JLabel ("Output precision:" ),
		0, ++ySHEF, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Precision_JTextField = new JTextField ( "", 20 );
	__Precision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(shef_JPanel, __Precision_JTextField,
		1, ySHEF, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(shef_JPanel, new JLabel (
		"Optional - digits after decimal (default=from units, or 2)."),
		3, ySHEF, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for output
    int yOut = -1;
    JPanel out_JPanel = new JPanel();
    out_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", out_JPanel );

    JGUIUtil.addComponent(out_JPanel, new JLabel (
	    "Specify the output file and period to write."),
	    0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"It is recommended that the file name be relative to the working directory."),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(out_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(out_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel (	"SHEF file to write:" ), 
		0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.setToolTipText("Specify the output file, can use ${Property} notation");
	__OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(out_JPanel, OutputFile_JPanel,
		1, yOut, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Append to output?:"),
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Append_JComboBox = new SimpleJComboBox ( false );
    List<String> appendChoices = new ArrayList<String>();
    appendChoices.add ( "" );  // Default
    appendChoices.add ( __command._False );
    appendChoices.add ( __command._True );
    __Append_JComboBox.setData(appendChoices);
    __Append_JComboBox.select ( 0 );
    __Append_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(out_JPanel, __Append_JComboBox,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Optional - append to command file? (default=" + __command._False + ")."), 
        3, yOut, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel ("Output start:"), 
		0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputStart_JTextField = new JTextField (20);
    __OutputStart_JTextField.setToolTipText("Specify the output start using a date/time string or ${Property} notation");
	__OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(out_JPanel, __OutputStart_JTextField,
		1, yOut, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"Optional - default is all data or global output start."),
		3, yOut, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Output end:"), 
		0, ++yOut, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputEnd_JTextField = new JTextField (20);
	__OutputEnd_JTextField.setToolTipText("Specify the output end using a date/time string or ${Property} notation");
	__OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(out_JPanel, __OutputEnd_JTextField,
		1, yOut, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"Optional - default is all data or global output end."),
		3, yOut, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add ( __cancel_JButton = new SimpleJButton( "Cancel", this) );
	button_JPanel.add ( __ok_JButton = new SimpleJButton( "OK", this) );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
    checkGUIState();
	refresh();	// Sets the __path_JButton status
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{   checkGUIState();
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
	    // Other character entered...
		refresh();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String TSList = "";
	String TSID = "";
	String LocationID = "";
	String DataTypePELookup = "";
	String OutputFile = "";
	String Append = "";
	String OutputStart = "";
	String OutputEnd = "";
	String TimeZone = "";
	String ObservationTime = "";
	String CreationDate = "";
	String Duration = "";
	String Precision = "";
	PropList props = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		props = __command.getCommandParameters();
		TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
		LocationID = props.getValue ( "LocationID" );
		DataTypePELookup = props.getValue ( "DataTypePELookup" );
		OutputFile = props.getValue("OutputFile");
		Append = props.getValue ( "Append" );
		OutputStart = props.getValue("OutputStart");
		OutputEnd = props.getValue("OutputEnd");
		TimeZone = props.getValue("TimeZone");
		ObservationTime = props.getValue("ObservationTime");
		CreationDate = props.getValue("CreationDate");
		Duration = props.getValue("Duration");
		Precision = props.getValue("Precision");
		if ( TSList == null ) {
			// Select default...
			__TSList_JComboBox.select ( 0 );
		}
		else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
				__TSList_JComboBox,	TSList, JGUIUtil.NONE, null, null ) ) {
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
		if ( !__TSID_JComboBox.isEnabled() ) {
			// Not needed because some other method of specifying
			// the time series is being used...
			TSID = null;
		}
        if ( LocationID != null ) {
            __LocationID_JTextField.setText( LocationID );
        }
        if ( DataTypePELookup != null ) {
            __DataTypePELookup_JTextArea.setText( DataTypePELookup );
        }
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText( OutputFile );
		}
        if ( JGUIUtil.isSimpleJComboBoxItem( __Append_JComboBox, Append, JGUIUtil.NONE, null, null ) ) {
            __Append_JComboBox.select ( Append );
        }
        else {
            if ( (Append == null) || Append.equals("") ) {
                // New command...select the default...
                __Append_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAppend parameter \"" +
                Append + "\".  Select a\ndifferent value or Cancel." );
            }
        }
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText ( OutputStart );
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText ( OutputEnd );
		}
		if ( TimeZone != null ) {
			__TimeZone_JTextField.setText ( TimeZone );
		}
        if ( ObservationTime != null ) {
            __ObservationTime_JTextField.setText ( ObservationTime );
        }
        if ( CreationDate != null ) {
            __CreationDate_JTextField.setText ( CreationDate );
        }
        if ( Duration != null ) {
            __Duration_JTextField.setText ( Duration );
        }
		if ( Precision != null ) {
			__Precision_JTextField.setText ( Precision );
		}
	}
	// Regardless, reset the command from the fields...
	TSList = __TSList_JComboBox.getSelected();
	if ( __TSID_JComboBox.isEnabled() ) {
		TSID = __TSID_JComboBox.getSelected();
	}
	else {
	    TSID = "";
	}
	LocationID = __LocationID_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	Append = __Append_JComboBox.getSelected();
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
	TimeZone = __TimeZone_JTextField.getText().trim();
    ObservationTime = __ObservationTime_JTextField.getText().trim();
    CreationDate = __CreationDate_JTextField.getText().trim();
    Duration = __Duration_JTextField.getText().trim();
	Precision = __Precision_JTextField.getText().trim();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
	props.add ( "LocationID=" + LocationID );
	props.add ( "DataTypePELookup=" + DataTypePELookup );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "Append=" + Append );
	props.add ( "OutputStart=" + OutputStart );
	props.add ( "OutputEnd=" + OutputEnd );
	props.add ( "TimeZone=" + TimeZone );
    props.add ( "ObservationTime=" + ObservationTime );
    props.add ( "CreationDate=" + CreationDate );
    props.add ( "Duration=" + Duration );
	props.add ( "Precision=" + Precision );
	__command_JTextArea.setText( __command.toString ( props ) );
	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
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

}
