// ReadDelftFewsPiXml_JDialog - Editor for the ReadDelftFewsPiXml() command.

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

package rti.tscommandprocessor.commands.delftfews;

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
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
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
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor for the ReadDelftFewsPiXml() command.
*/
@SuppressWarnings("serial")
public class ReadDelftFewsPiXml_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, WindowListener
{
private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";
	
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadDelftFewsPiXml_Command __command = null;
private String __working_dir = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __InputFile_JTextField = null;
private SimpleJComboBox __Output_JComboBox = null;
private JTextField __InputStart_JTextField;
private JTextField __InputEnd_JTextField;
private JTextField __TimeZoneOffset_JTextField;
private JTextField __TimeZone_JTextField;
private JTextField __DataSource_JTextField;
private JTextField __DataType_JTextField;
private JTextField __Description_JTextField;
private SimpleJComboBox	__Read24HourAsDay_JComboBox = null;
private JTextField __Read24HourAsDayCutoff_JTextField;
//private JTextField __NewUnits_JTextField = null; // Units to convert to at read
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __EnsembleID_JTextField;
private JTextField __EnsembleName_JTextField;
private JTextArea __Command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadDelftFewsPiXml_JDialog ( JFrame parent, ReadDelftFewsPiXml_Command command )
{   super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browse_JButton ) {
		// Browse for the file to read...
		JFileChooser fc = new JFileChooser();
        fc.setDialogTitle( "Select FEWS PI XML Time Series File");
        SimpleFileFilter sff = new SimpleFileFilter("xml","PI XML Time Series File");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("gz","PI XML Time Series File (gzipped)");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("zip","PI XML Time Series File (zipped)");
        fc.addChoosableFileFilter(sff);
		
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		if ( last_directory_selected != null ) {
			fc.setCurrentDirectory(	new File(last_directory_selected));
		}
		else {
            fc.setCurrentDirectory(new File(__working_dir));
		}
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
					Message.printWarning ( 1,"ReadDelftFewsPiXml_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadDelftFewsPiXml");
	}
	else if ( o == __cancel_JButton ) {
		response(false);
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput ();
		if (!__error_wait) {
			response(true);
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__InputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,__InputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
                __InputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,	__InputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "ReadDelfFewsPiXml_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
}

// Start event handlers for DocumentListener...

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

// ...End event handlers for DocumentListener

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput () {
	
	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String InputFile = __InputFile_JTextField.getText().trim();
    String Output = __Output_JComboBox.getSelected();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String TimeZoneOffset = __TimeZoneOffset_JTextField.getText().trim();
	String TimeZone = __TimeZone_JTextField.getText().trim();
	String DataSource = __DataSource_JTextField.getText().trim();
	String DataType = __DataType_JTextField.getText().trim();
	String Description = __Description_JTextField.getText().trim();
	String Read24HourAsDay = __Read24HourAsDay_JComboBox.getSelected().trim();
	String Read24HourAsDayCutoff = __Read24HourAsDayCutoff_JTextField.getText().trim();
	//String NewUnits = __NewUnits_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String EnsembleID = __EnsembleID_JTextField.getText().trim();
	String EnsembleName = __EnsembleName_JTextField.getText().trim();
	
	__error_wait = false;

	if (InputFile.length() > 0) {
		props.set("InputFile", InputFile);
	}
    if (Output.length() > 0) {
        props.set("Output", Output);
    }
	if (InputStart.length() > 0 && !InputStart.equals("*")) {
		props.set("InputStart", InputStart);
	}
	if (InputEnd.length() > 0 && !InputEnd.equals("*")) {
		props.set("InputEnd", InputEnd);
	}
	if (TimeZoneOffset.length() > 0 && !TimeZoneOffset.equals("*")) {
		props.set("TimeZoneOffset", TimeZoneOffset);
	}
	if (TimeZone.length() > 0 && !TimeZone.equals("*")) {
		props.set("TimeZone", TimeZone);
	}
	if (DataSource.length() > 0 && !DataSource.equals("*")) {
		props.set("DataSource", DataSource);
	}
	if (DataType.length() > 0 && !DataType.equals("*")) {
		props.set("DataType", DataType);
	}
	if (Description.length() > 0 && !Description.equals("*")) {
		props.set("Description", Description);
	}
	if (Read24HourAsDay.trim().length() > 0) {
		props.set("Read24HourAsDay", Read24HourAsDay);
	}
	if (Read24HourAsDayCutoff.trim().length() > 0) {
		props.set("Read24HourAsDayCutoff", Read24HourAsDayCutoff);
	}
	//if (NewUnits.length() > 0 && !NewUnits.equals("*")) {
	//	props.set("NewUnits", NewUnits);
	//}
    if (Alias != null && Alias.length() > 0) {
        props.set("Alias", Alias);
    }
    if (EnsembleID != null && EnsembleID.length() > 0) {
        props.set("EnsembleID", EnsembleID);
    }
    if (EnsembleName != null && EnsembleName.length() > 0) {
        props.set("EnsembleName", EnsembleName);
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
private void commitEdits() {
	String InputFile = __InputFile_JTextField.getText().trim();
    String Output = __Output_JComboBox.getSelected();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String TimeZoneOffset = __TimeZoneOffset_JTextField.getText().trim();
	String TimeZone = __TimeZone_JTextField.getText().trim();
	String DataSource = __DataSource_JTextField.getText().trim();
	String DataType = __DataType_JTextField.getText().trim();
	String Description = __Description_JTextField.getText().trim();
	String Read24HourAsDay = __Read24HourAsDay_JComboBox.getSelected().trim();
	String Read24HourAsDayCutoff = __Read24HourAsDayCutoff_JTextField.getText().trim();
	//String NewUnits = __NewUnits_JTextField.getText().trim();
    String Alias = __Alias_JTextField.getText().trim();
	String EnsembleID = __EnsembleID_JTextField.getText().trim();
	String EnsembleName = __EnsembleName_JTextField.getText().trim();

	__command.setCommandParameter("InputFile", InputFile);
	__command.setCommandParameter("Output", Output);
	__command.setCommandParameter("InputStart", InputStart);
	__command.setCommandParameter("InputEnd", InputEnd);
	__command.setCommandParameter("TimeZoneOffset", TimeZoneOffset);
	__command.setCommandParameter("TimeZone", TimeZone);
	__command.setCommandParameter("DataSource", DataSource);
	__command.setCommandParameter("DataType", DataType);
	__command.setCommandParameter("Description", Description);
	__command.setCommandParameter("Read24HourAsDay", Read24HourAsDay);
	__command.setCommandParameter("Read24HourAsDayCutoff", Read24HourAsDayCutoff);
	//__command.setCommandParameter("NewUnits", NewUnits);
    __command.setCommandParameter("Alias", Alias);
    __command.setCommandParameter("EnsembleID", EnsembleID);
    __command.setCommandParameter("EnsembleName", EnsembleName);
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param app_PropList Properties from application.
@param command Command to edit.
*/
private void initialize(JFrame parent, ReadDelftFewsPiXml_Command command) {
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Read time series from a Delft FEWS PI XML file."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Single time series and optionally ensembles will be read (specific individual traces from an ensemble cannot be extracted by this command)."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full path or relative path (relative to the working directory) for a PI XML file to read." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The Output command parameter indicates whether individual time series and optionally ensembles (groups of time series) are output."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"PI XML file to read:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText("Specify the path to the input file or use ${Property} notation");
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
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Output_JComboBox = new SimpleJComboBox ( false );
    List<String> outputChoices = new ArrayList<String>();
    outputChoices.add ( "" );
    outputChoices.add ( __command._TimeSeries );
    outputChoices.add ( __command._TimeSeriesAndEnsembles );
    __Output_JComboBox.setData(outputChoices);
    __Output_JComboBox.select ( 0 );
    __Output_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Output_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output to generate (default=" + __command._TimeSeriesAndEnsembles + ")."), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for time series
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series", ts_JPanel );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "All time series in the PI XML file are read as time series according to these parameters."),
        0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    /*
    JGUIUtil.addComponent(main_JPanel, new JLabel("Units to convert to:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewUnits_JTextField = new JTextField ( "", 10 );
	__NewUnits_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(main_JPanel, __NewUnits_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - request units different from input."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        */

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Time zone offset:"), 
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeZoneOffset_JTextField = new JTextField (20);
    __TimeZoneOffset_JTextField.setToolTipText("Time series output will have time zone offset from GMT such as -7 for Mountain Standard Time");
    __TimeZoneOffset_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __TimeZoneOffset_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - hours from GMT for output (default=file time zone)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Time zone:"), 
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TimeZone_JTextField = new JTextField (20);
    __TimeZone_JTextField.setToolTipText("Output time zone text to assign to time series date/times, e.g., MST.");
    __TimeZone_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __TimeZone_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - output time zone as string (default=GMT+/-TimeZoneOffset)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Input start (output time zone):"), 
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.setToolTipText("Specify the input start using a date/time string or ${Property} notation");
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __InputStart_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - date/time for start of data (default=global input start)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Input end (output time zone):"), 
        0, ++yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.setToolTipText("Specify the input end using a date/time string or ${Property} notation");
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __InputEnd_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - date/time for end of data (default=global input end)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Data source:"), 
        0, ++yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataSource_JTextField = new JTextField (20);
    __DataSource_JTextField.setToolTipText("Data source to override default, can use time serie % specifiers or ${ts:Property} notation");
    __DataSource_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __DataSource_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - data source for time series ID (default=FEWS)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Data type:"), 
        0, ++yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataType_JTextField = new JTextField (20);
    __DataType_JTextField.setToolTipText("Data type to override default, can use time serie % specifiers or ${ts:Property} notation");
    __DataType_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __DataType_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - data type (default=read from file)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Description:"), 
        0, ++yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Description_JTextField = new JTextField (20);
    __Description_JTextField.setToolTipText("Description to override default, can use time serie % specifiers or ${ts:Property} notation");
    __Description_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __Description_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - description (default=station name)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
	JGUIUtil.addComponent(ts_JPanel, new JLabel("Read 24 hour as day:"),
		0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	List<String> v = new Vector<String>();
	v.add("");
	v.add(__command._False);
	v.add(__command._True);
	__Read24HourAsDay_JComboBox = new SimpleJComboBox(v);
	__Read24HourAsDay_JComboBox.select(0);
	__Read24HourAsDay_JComboBox.addActionListener(this);
	JGUIUtil.addComponent(ts_JPanel, __Read24HourAsDay_JComboBox,
		1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Optional - convert 24Hour interval to Day interval (default=" + __command._False + ")."),
		3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "24 Hour to day cutoff:"), 
        0, ++yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Read24HourAsDayCutoff_JTextField = new JTextField (20);
    __Read24HourAsDayCutoff_JTextField.setToolTipText("If hour is <= this value, decrement day.");
    __Read24HourAsDayCutoff_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __Read24HourAsDayCutoff_JTextField,
        1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - decrement day if hour is <= this value (default=0)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
    JGUIUtil.addComponent(ts_JPanel, new JLabel("Alias to assign:"),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(ts_JPanel, __Alias_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Optional - alias for time series use %L for location, etc."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for ensembles
    int yEns = -1;
    JPanel ens_JPanel = new JPanel();
    ens_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Ensembles", ens_JPanel );
    
    JGUIUtil.addComponent(ens_JPanel, new JLabel (
        "Ensembles are created by grouping time series with matching <ensembleId> property in the PI XML file."),
        0, ++yEns, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ens_JPanel, new JLabel (
        "The TSTool EnsembleID will default to the locationId_DataType_ensembleId (DataType can be specified as parameter)."),
        0, ++yEns, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ens_JPanel, new JLabel (
        "Relevant elements from the PI XML file are saved as properties on the ensemble and can be accessed with ${tsensemble:property}."),
        0, ++yEns, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ens_JPanel, new JLabel (
        "Important:  TSTool EnsembleID can be different from the ensembleId value (property names are case-specific)."),
        0, ++yEns, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ens_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yEns, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ens_JPanel, new JLabel ("Ensemble ID:"), 
        0, ++yEns, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleID_JTextField = new JTextField (20);
    __EnsembleID_JTextField.setToolTipText("Specify the ensemble ID using text and ${Property} notation");
    __EnsembleID_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ens_JPanel, __EnsembleID_JTextField,
        1, yEns, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ens_JPanel, new JLabel ( "Optional - ensemble ID (default=locationId_DataType_ensembleId)."),
        3, yEns, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ens_JPanel, new JLabel ("Ensemble name:"), 
        0, ++yEns, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleName_JTextField = new JTextField (20);
    __EnsembleName_JTextField.setToolTipText("Specify the ensemble name using text and ${Property} notation");
    __EnsembleName_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ens_JPanel, __EnsembleName_JTextField,
        1, yEns, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ens_JPanel, new JLabel ( "Optional - ensemble name (default=EnsembleID)."),
        3, yEns, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Command_JTextArea = new JTextArea(4, 55);
	__Command_JTextArea.setLineWrap ( true );
	__Command_JTextArea.setWrapStyleWord ( true );	
	__Command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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

	setTitle("Edit " + __command.getCommandName() +" Command");
	
	// Refresh the contents...
    refresh ();

    pack();
    JGUIUtil.center( this );
	refresh(); // Sets the __path_JButton status
	setResizable ( false );
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed(KeyEvent event) {
	int code = event.getKeyCode();
	if (code == KeyEvent.VK_ENTER) {
		refresh();
		checkInput();
		if (!__error_wait) {
			response(true);
		}
	}
}

/**
Need this to properly capture key events, especially deletes.
*/
public void keyReleased(KeyEvent event) {
	refresh();
}

public void keyTyped(KeyEvent event) {
	refresh();
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok() {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh() {
	String routine = getClass().getSimpleName() + ".refresh";
	String InputFile = "";
	String Output = "";
	String InputStart = "";
	String InputEnd = "";
	String TimeZoneOffset = "";
	String TimeZone = "";
	String DataSource = "";
	String DataType = "";
	String Description = "";
	String Read24HourAsDay = "";
	String Read24HourAsDayCutoff = "";
	String NewUnits = "";
	String Alias = "";
	String EnsembleID = "";
	String EnsembleName = "";

	PropList props = null;

	if (__first_time) {
		__first_time = false;

		// Get the properties from the command
		props = __command.getCommandParameters();
        Alias = props.getValue("Alias");
		InputFile = props.getValue("InputFile");
		Output = props.getValue("Output");
		InputStart = props.getValue("InputStart");
		InputEnd = props.getValue("InputEnd");
		TimeZoneOffset = props.getValue("TimeZoneOffset");
		TimeZone = props.getValue("TimeZone");
		DataSource = props.getValue("DataSource");
		DataType = props.getValue("DataType");
		Description = props.getValue("Description");
		Read24HourAsDay = props.getValue("Read24HourAsDay");
		Read24HourAsDayCutoff = props.getValue("Read24HourAsDayCutoff");
		NewUnits = props.getValue("NewUnits");
		EnsembleID = props.getValue("EnsembleID");
		EnsembleName = props.getValue("EnsembleName");
		// Set the control fields
		if (InputFile != null) {
			__InputFile_JTextField.setText(InputFile);
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__Output_JComboBox, Output, JGUIUtil.NONE, null, null ) ) {
            __Output_JComboBox.select ( Output );
        }
        else {
            if ( (Output == null) || Output.equals("") ) {
                // New command...select the default...
                __Output_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "Output parameter \"" + Output + "\".  Select a\ndifferent value or Cancel." );
            }
        }
		if (InputStart != null) {
			__InputStart_JTextField.setText(InputStart);
		}
		if (InputEnd != null) {
			__InputEnd_JTextField.setText(InputEnd);
		}
		if (TimeZoneOffset != null) {
			__TimeZoneOffset_JTextField.setText(TimeZoneOffset);
		}
		if (TimeZone != null) {
			__TimeZone_JTextField.setText(TimeZone);
		}
		if (DataSource != null) {
			__DataSource_JTextField.setText(DataSource);
		}
		if (DataType != null) {
			__DataType_JTextField.setText(DataType);
		}
		if (Description != null) {
			__Description_JTextField.setText(Description);
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__Read24HourAsDay_JComboBox, Read24HourAsDay, JGUIUtil.NONE, null, null ) ) {
            __Read24HourAsDay_JComboBox.select ( Read24HourAsDay );
        }
        else {
            if ( (Read24HourAsDay == null) || Read24HourAsDay.equals("") ) {
                // New command...select the default...
                __Read24HourAsDay_JComboBox.select ( 0 );
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
                "Read24HourAsDay parameter \"" + Read24HourAsDay + "\".  Select a\ndifferent value or Cancel." );
            }
        }
        if (Read24HourAsDayCutoff != null) {
			__Read24HourAsDayCutoff_JTextField.setText(Read24HourAsDayCutoff);
		}
		//if (NewUnits != null) {
		//	__NewUnits_JTextField.setText(NewUnits);
		//}
		if (Alias != null) {
			__Alias_JTextField.setText(Alias.trim());
		}
		if (EnsembleID != null) {
			__EnsembleID_JTextField.setText(EnsembleID);
		}
		if (EnsembleName != null) {
			__EnsembleName_JTextField.setText(EnsembleName);
		}
	}

	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
	Output = __Output_JComboBox.getSelected();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();
	TimeZoneOffset = __TimeZoneOffset_JTextField.getText().trim();
	TimeZone = __TimeZone_JTextField.getText().trim();
	DataSource = __DataSource_JTextField.getText().trim();
	DataType = __DataType_JTextField.getText().trim();
	Read24HourAsDay = __Read24HourAsDay_JComboBox.getSelected().trim();
	Read24HourAsDayCutoff = __Read24HourAsDayCutoff_JTextField.getText().trim();
	//NewUnits = __NewUnits_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	EnsembleID = __EnsembleID_JTextField.getText().trim();
	EnsembleName = __EnsembleName_JTextField.getText().trim();

	props = new PropList(__command.getCommandName());
	props.add("InputFile=" + InputFile);
	props.add("Output=" + Output);
	props.add("InputStart=" + InputStart);
	props.add("InputEnd=" + InputEnd);
	props.add("TimeZoneOffset=" + TimeZoneOffset);
	props.add("TimeZone=" + TimeZone);
	props.add("DataSource=" + DataSource);
	props.add("DataType=" + DataType);
	props.add("Read24HourAsDay=" + Read24HourAsDay);
	props.add("Read24HourAsDayCutoff=" + Read24HourAsDayCutoff);
	props.add("NewUnits=" + NewUnits);
	props.add("Alias=" + Alias);
	props.add("EnsembleID=" + EnsembleID);
	props.add("EnsembleName=" + EnsembleName);
	__Command_JTextArea.setText( __command.toString(props).trim() );

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
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok ) {
	__ok = ok;
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
{	response(false);
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
