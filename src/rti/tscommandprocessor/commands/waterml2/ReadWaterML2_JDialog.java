// ReadWaterML2_JDialog - Editor for the ReadWaterML2() command.

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

package rti.tscommandprocessor.commands.waterml2;

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
import java.io.File;
import java.util.List;

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
import RTi.Util.Time.TimeInterval;

/**
Editor for the ReadWaterML2() command.
*/
@SuppressWarnings("serial")
public class ReadWaterML2_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __browse_JButton = null;
private SimpleJButton __path_JButton = null; // Convert between relative and absolute path.
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private ReadWaterML2_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private String __working_dir = null;
private JTextField __InputFile_JTextField = null;
private SimpleJComboBox __ReadMethod_JComboBox = null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;
private JTextField __InputStart_JTextField = null;
private JTextField __InputEnd_JTextField = null;
private SimpleJComboBox __Interval_JComboBox = null;
private SimpleJComboBox __RequireDataToMatchInterval_JComboBox = null;
private JTextField __OutputTimeZoneOffset_JTextField = null;
private JTextField __OutputTimeZone_JTextField = null;
//private JTextField __NewUnits_JTextField = null;
private JTextArea __Command_JTextArea = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;			
private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public ReadWaterML2_JDialog ( JFrame parent, ReadWaterML2_Command command )
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
        fc.setDialogTitle( "Select WaterML2 Time Series File");
        SimpleFileFilter sff = new SimpleFileFilter("xml","WaterML2 Time Series File");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("waterml","WaterML2 Time Series File");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("wml","WaterML2 Time Series File");
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
					Message.printWarning ( 1,"ReadWaterML2_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response(false);
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "ReadWaterML2");
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
				Message.printWarning ( 1, "ReadWaterML_JDialog", "Error converting file to relative path." );
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
	String ReadMethod  = __ReadMethod_JComboBox.getSelected();
	String OutputTimeZoneOffset = __OutputTimeZoneOffset_JTextField.getText().trim();
	String OutputTimeZone = __OutputTimeZone_JTextField.getText().trim();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	//String NewUnits = __NewUnits_JTextField.getText().trim();
	String Alias = __Alias_JTextField.getText().trim();
	String Interval  = __Interval_JComboBox.getSelected();
	String RequireDataToMatchInterval  = __RequireDataToMatchInterval_JComboBox.getSelected();
	
	__error_wait = false;

    if (Alias != null && Alias.length() > 0) {
        props.set("Alias", Alias);
    }
	if (InputFile.length() > 0) {
		props.set("InputFile", InputFile);
	}
	if (ReadMethod.length() > 0) {
		props.set("ReadMethod", ReadMethod);
	}
	if (InputStart.length() > 0) {
		props.set("InputStart", InputStart);
	}
	if (InputEnd.length() > 0) {
		props.set("InputEnd", InputEnd);
	}
    if (Interval.length() > 0) {
        props.set("Interval", Interval);
    }
    if (RequireDataToMatchInterval.length() > 0) {
        props.set("RequireDataToMatchInterval", RequireDataToMatchInterval);
    }
	if (OutputTimeZoneOffset.length() > 0) {
		props.set("OutputTimeZoneOffset", OutputTimeZoneOffset);
	}
	if (OutputTimeZone.length() > 0) {
		props.set("OutputTimeZone", OutputTimeZone);
	}
	//if (NewUnits.length() > 0 && !NewUnits.equals("*")) {
	//	props.set("NewUnits", NewUnits);
	//}

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
private void commitEdits() {
    String Alias = __Alias_JTextField.getText().trim();
	String InputFile = __InputFile_JTextField.getText().trim();
	String ReadMethod  = __ReadMethod_JComboBox.getSelected();
	String InputStart = __InputStart_JTextField.getText().trim();
	String InputEnd = __InputEnd_JTextField.getText().trim();
	String Interval  = __Interval_JComboBox.getSelected();
	String RequireDataToMatchInterval  = __RequireDataToMatchInterval_JComboBox.getSelected();
	String OutputTimeZoneOffset = __OutputTimeZoneOffset_JTextField.getText().trim();
	String OutputTimeZone = __OutputTimeZone_JTextField.getText().trim();
	//String NewUnits = __NewUnits_JTextField.getText().trim();

    __command.setCommandParameter("Alias", Alias);
	__command.setCommandParameter("InputFile", InputFile);
	__command.setCommandParameter("ReadMethod", ReadMethod);
	__command.setCommandParameter("InputStart", InputStart);
	__command.setCommandParameter("InputEnd", InputEnd);
	__command.setCommandParameter("Interval", Interval);
	__command.setCommandParameter("RequireDataToMatchInterval", RequireDataToMatchInterval);
	__command.setCommandParameter("OutputTimeZoneOffset", OutputTimeZoneOffset);
	__command.setCommandParameter("OutputTimeZone", OutputTimeZone);
	//__command.setCommandParameter("NewUnits", NewUnits);
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param app_PropList Properties from application.
@param command Command to edit.
*/
private void initialize(JFrame parent, ReadWaterML2_Command command) {
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
        "Read all the time series from a WaterML 2 file, using " +
        "information in the file to assign the time series identifier.  WaterML 2.0 is currently supported."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "It is assumed that data in the file have the same interval so that command parameters can be specified for one interval."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "WaterML 2.0 implementations for different data providers vary and software changes may be needed to support a WaterML 2 \"flavor\"."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specify a full path or relative path (relative to the working directory) for a WaterML 2 file to read." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Specifying the input period will limit data that are available for other commands but can increase performance." ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for general parameters
    int yGeneral = -1;
    JPanel general_JPanel = new JPanel();
    general_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "General", general_JPanel );

    JGUIUtil.addComponent(general_JPanel, new JLabel (	"WaterML 2 file to read:" ), 
		0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__InputFile_JTextField = new JTextField ( 50 );
	__InputFile_JTextField.setToolTipText ("Specify path to input file, can use ${Property}");
	__InputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(general_JPanel, __InputFile_JTextField,
		1, yGeneral, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(general_JPanel, __browse_JButton,
		6, yGeneral, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
	    JGUIUtil.addComponent(general_JPanel, __path_JButton,
	    	7, yGeneral, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
        
    JGUIUtil.addComponent(general_JPanel, new JLabel( "Read method:"),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ReadMethod_JComboBox = new SimpleJComboBox ( false );
    __ReadMethod_JComboBox.setToolTipText(__command._API + " will use objects generated from WaterML 2 XML schema, " +
    	__command._ParseDOM + " will parse XML document directly.");
    __ReadMethod_JComboBox.add("");
    __ReadMethod_JComboBox.add(__command._API);
    __ReadMethod_JComboBox.add(__command._ParseDOM);
    // Select a default...
    __ReadMethod_JComboBox.select ( 0 );
    __ReadMethod_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(general_JPanel, __ReadMethod_JComboBox,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Optional - how to read (default=" + __command._ParseDOM + ")."),
        3, yGeneral, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(general_JPanel, new JLabel("Alias to assign:"),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(10);
    __Alias_JTextField.setToolTipText("Specify the time series alias, can use %L, ${Property}, ${ts:property}");
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(general_JPanel, __Alias_JTextField,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ("Optional - use %L for location, etc."),
        3, yGeneral, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(general_JPanel, new JLabel( "Interval:"),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Interval_JComboBox = new SimpleJComboBox ( false );
    __Interval_JComboBox.setToolTipText("Specify the time series interval (see tabs for more information).");
    List<String> intervalChoices = TimeInterval.getTimeIntervalChoices(TimeInterval.MINUTE, TimeInterval.YEAR,false,-1);
    intervalChoices.add ( "Irregular" );
    __Interval_JComboBox.setData ( intervalChoices );
    
    // Select a default...
    __Interval_JComboBox.select ( 0 );
    __Interval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(general_JPanel, __Interval_JComboBox,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Required - data interval for data."),
        3, yGeneral, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(general_JPanel, new JLabel( "Require data to match interval?:"),
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RequireDataToMatchInterval_JComboBox = new SimpleJComboBox ( false );
    __RequireDataToMatchInterval_JComboBox.setToolTipText("Indicate whether data timesteps should align with interval, helps with instantaneous measurements.");
    __RequireDataToMatchInterval_JComboBox.add("");
    __RequireDataToMatchInterval_JComboBox.add(__command._False);
    __RequireDataToMatchInterval_JComboBox.add(__command._True);
    // Select a default...
    __RequireDataToMatchInterval_JComboBox.select ( 0 );
    __RequireDataToMatchInterval_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(general_JPanel, __RequireDataToMatchInterval_JComboBox,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Optional - require data/interval alignment (default=" + __command._True + ")."),
        3, yGeneral, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    /*
    JGUIUtil.addComponent(general_JPanel, new JLabel("Units to convert to:"),
		0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NewUnits_JTextField = new JTextField ( "", 10 );
	__NewUnits_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(general_JPanel, __NewUnits_JTextField,
		1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Optional - request units different from input."),
        3, yGeneral, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    */

    JGUIUtil.addComponent(general_JPanel, new JLabel ("Input start:"), 
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputStart_JTextField = new JTextField (20);
    __InputStart_JTextField.setToolTipText("Specify the input start using YYYY-MM-DD or similar, can use ${Property}");
    __InputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(general_JPanel, __InputStart_JTextField,
        1, yGeneral, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Optional - overrides the global input start."),
        3, yGeneral, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Input end:"), 
        0, ++yGeneral, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputEnd_JTextField = new JTextField (20);
    __InputEnd_JTextField.setToolTipText("Specify the input end using YYYY-MM-DD or similar, can use ${Property}");
    __InputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(general_JPanel, __InputEnd_JTextField,
        1, yGeneral, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Optional - overrides the global input end."),
        3, yGeneral, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(general_JPanel, new JLabel ("Output time zone as offset:"), 
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTimeZoneOffset_JTextField = new JTextField (20);
    __OutputTimeZoneOffset_JTextField.setToolTipText("Specify time zone offset such as -07:00 for all data, can use ${Property}");
    __OutputTimeZoneOffset_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(general_JPanel, __OutputTimeZoneOffset_JTextField,
        1, yGeneral, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Optional - shift all times to offset."),
        3, yGeneral, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(general_JPanel, new JLabel ("Output time zone (text):"), 
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputTimeZone_JTextField = new JTextField (20);
    __OutputTimeZone_JTextField.setToolTipText("Specify time zone label to use (after shift), \\b for blank, can use ${Property}");
    __OutputTimeZone_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(general_JPanel, __OutputTimeZone_JTextField,
        1, yGeneral, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Optional - time zone to set (after shifting to offset)."),
        3, yGeneral, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for instantaneous interval parameters
    int yInst = -1;
    JPanel inst_JPanel = new JPanel();
    inst_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Instantaneous Data", inst_JPanel );
    
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"If the WaterML 2 file contains instantaneous data (e.g., USGS NWIS instantaneous value web service),"), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"the interval can be specified as " + TimeInterval.getName(TimeInterval.IRREGULAR, 0) + " or a regular interval, as appropriate."), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"For example, USGS NWIS instantaneous data are typically published with an interval of 15" + TimeInterval.getName(TimeInterval.MINUTE, 0) + "."), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"Using an irregular interval limits how data can be processed with other commands because timesteps may not align and timezone can vary."), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"The RequireDataToMatchInterval parameter can be used to ensure that instantaneous data do in fact align with regular interval boundary."), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"Time zone will be set in the internal date/time objects and the date/time parts from the WaterML 2 content will be used for time series data values."), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"The WaterML 2 date/time parts may be for local time, GMT, +HH:MM offset, or a standard time zone, depending on how the file was generated."), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"TSTool date/times for regular interval data should be set to a time zone that applies to entire period to avoid time alignment issues."), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(inst_JPanel, new JLabel (
		"Therefore, if necessary, specify an output time zone offset consistent with other time series being processed (\\b to set to blank)."), 
		0, ++yInst, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for daily web service parameters
    int yDay = -1;
    JPanel day_JPanel = new JPanel();
    day_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Daily Data", day_JPanel );
    
    JGUIUtil.addComponent(day_JPanel, new JLabel (
		"If the WaterML 2 file contains daily data (e.g., USGS NWIS daily value web service), specify the interval as " +
			TimeInterval.getName(TimeInterval.DAY, 0)+ "." ), 
		0, ++yDay, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

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

	setTitle("Edit " + __command.getCommandName() + " Command");
	
	// Refresh the contents...
    refresh ();

    pack();
    JGUIUtil.center( this );
	refreshPathControl();	// Sets the __path_JButton status
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{
    refresh();
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
private void refresh()
{   String routine = getClass().getSimpleName() + ".refresh";
	String InputFile = "";
	String ReadMethod = "";
	//String NewUnits = "";
	String Alias = "";
	String Interval = "";
	String RequireDataToMatchInterval = "";
	String OutputTimeZoneOffset = "";
	String OutputTimeZone = "";
	String InputStart = "";
	String InputEnd = "";

	PropList props = null;

	if (__first_time) {
		__first_time = false;

		// Get the properties from the command
		props = __command.getCommandParameters();
        Alias = props.getValue("Alias");
		InputFile = props.getValue("InputFile");
		ReadMethod = props.getValue("ReadMethod");
		InputStart = props.getValue("InputStart");
		InputEnd = props.getValue("InputEnd");
		Interval = props.getValue("Interval");
		RequireDataToMatchInterval = props.getValue("RequireDataToMatchInterval");
		OutputTimeZoneOffset = props.getValue("OutputTimeZoneOffset");
		OutputTimeZone = props.getValue("OutputTimeZone");
		//NewUnits = props.getValue("NewUnits");
		// Set the control fields
		if (Alias != null) {
			__Alias_JTextField.setText(Alias.trim());
		}
		if (InputFile != null) {
			__InputFile_JTextField.setText(InputFile);
		}
        if ( ReadMethod == null || ReadMethod.equals("") ) {
            // Select a default...
            __ReadMethod_JComboBox.select ( 0 );
        } 
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __ReadMethod_JComboBox, ReadMethod, JGUIUtil.NONE, null, null ) ) {
                __ReadMethod_JComboBox.select ( ReadMethod );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid ReadMethod \"" +
                	ReadMethod + "\".  Select a different choice or Cancel." );
            }
        }
        if ( Interval == null || Interval.equals("") ) {
            // Select a default...
            __Interval_JComboBox.select ( 0 );
        } 
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Interval_JComboBox, Interval, JGUIUtil.NONE, null, null ) ) {
                __Interval_JComboBox.select ( Interval );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nInterval \"" +
                    Interval + "\".  Select a different choice or Cancel." );
            }
        }
        if ( RequireDataToMatchInterval == null || RequireDataToMatchInterval.equals("") ) {
            // Select a default...
            __RequireDataToMatchInterval_JComboBox.select ( 0 );
        } 
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __RequireDataToMatchInterval_JComboBox, RequireDataToMatchInterval, JGUIUtil.NONE, null, null ) ) {
                __RequireDataToMatchInterval_JComboBox.select ( RequireDataToMatchInterval );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nRequireDataToMatchInterval \"" +
                    RequireDataToMatchInterval + "\".  Select a different choice or Cancel." );
            }
        }
		if ( OutputTimeZoneOffset != null) {
			__OutputTimeZoneOffset_JTextField.setText(OutputTimeZoneOffset);
		}
		if ( OutputTimeZone != null) {
			__OutputTimeZone_JTextField.setText(OutputTimeZone);
		}
		if (InputStart != null) {
			__InputStart_JTextField.setText(InputStart);
		}
		if (InputEnd != null) {
			__InputEnd_JTextField.setText(InputEnd);
		}
		//if (NewUnits != null) {
		//	__NewUnits_JTextField.setText(NewUnits);
		//}
	}

	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	InputFile = __InputFile_JTextField.getText().trim();
	ReadMethod = __ReadMethod_JComboBox.getSelected();
	//NewUnits = __NewUnits_JTextField.getText().trim();
	Alias = __Alias_JTextField.getText().trim();
	Interval = __Interval_JComboBox.getSelected();
	RequireDataToMatchInterval = __RequireDataToMatchInterval_JComboBox.getSelected();
	OutputTimeZoneOffset = __OutputTimeZoneOffset_JTextField.getText().trim();
	OutputTimeZone = __OutputTimeZone_JTextField.getText().trim();
	InputStart = __InputStart_JTextField.getText().trim();
	InputEnd = __InputEnd_JTextField.getText().trim();

	props = new PropList(__command.getCommandName());
	props.add("InputFile=" + InputFile);
	props.add("ReadMethod=" + ReadMethod);
	//props.add("NewUnits=" + NewUnits);
	props.add("Alias=" + Alias);
	props.add("Interval=" + Interval);
	props.add("RequireDataToMatchInterval=" + RequireDataToMatchInterval);
	props.add("OutputTimeZoneOffset=" + OutputTimeZoneOffset);
	props.add("OutputTimeZone=" + OutputTimeZone);
	props.add("InputStart=" + InputStart);
	props.add("InputEnd=" + InputEnd);
	__Command_JTextArea.setText( __command.toString(props) );

	// Refresh the Path Control text.
	refreshPathControl();
}

/**
Refresh the PathControl text based on the contents of the input text field contents.
*/
private void refreshPathControl()
{
	String InputFile = __InputFile_JTextField.getText().trim();
	if ( (InputFile == null) || (InputFile.trim().length() == 0) ) {
		if ( __path_JButton != null ) {
			__path_JButton.setEnabled ( false );
		}
		return;
	}

	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		__path_JButton.setEnabled ( true );
		File f = new File ( InputFile );
		if ( f.isAbsolute() ) {
			__path_JButton.setText(	__RemoveWorkingDirectory );
			__path_JButton.setToolTipText("Change path to relative to command file");
		}
		else {
            __path_JButton.setText(	__AddWorkingDirectory );
			__path_JButton.setToolTipText("Change path to absolute");
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
