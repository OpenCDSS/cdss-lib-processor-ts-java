// WriteHecDss_JDialog - Command editor dialog for the WriteHecDss() command.

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

package rti.tscommandprocessor.commands.hecdss;

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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import java.io.File;
import java.util.List;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.Util.GUI.JFileChooserFactory;
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
Command editor dialog for the WriteHecDss() command.
*/
@SuppressWarnings("serial")
public class WriteHecDss_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private WriteHecDss_Command __command = null;
private String __working_dir = null; // Working directory.
private JTextArea __command_JTextArea=null;
private JTextField __OutputFile_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTextField __Precision_JTextField = null;
private JTextField __A_JTextField = null;
private JTextField __B_JTextField = null;
private JTextField __C_JTextField = null;
private JTextField __E_JTextField = null;
private JTextField __F_JTextField = null;
private SimpleJComboBox __Type_JComboBox = null;
private SimpleJComboBox __Replace_JComboBox = null;
private SimpleJComboBox __Close_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog?

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteHecDss_JDialog ( JFrame parent, WriteHecDss_Command command )
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
		fc.setDialogTitle("Select Existing HEC-DSS Time Series File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("dss", "HEC-DSS File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				// Convert path to relative path by default.
				try {
					__OutputFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"WriteHecdss_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory );
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "WriteHecDss");
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
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __OutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"WriteHecDss_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
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
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Precision = __Precision_JTextField.getText().trim();
    String A = __A_JTextField.getText().trim();
    String B = __B_JTextField.getText().trim();
    String C = __C_JTextField.getText().trim();
    String E = __E_JTextField.getText().trim();
    String F = __F_JTextField.getText().trim();
    String Type = __Type_JComboBox.getSelected();
    String Replace = __Replace_JComboBox.getSelected();
    String Close = __Close_JComboBox.getSelected();

	__error_wait = false;
	
	if ( TSList.length() > 0 ) {
		parameters.set ( "TSList", TSList );
	}
    if ( Type.length() > 0 ) {
        parameters.set ( "Type", Type );
    }
    if ( TSID.length() > 0 ) {
        parameters.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
	if ( OutputFile.length() > 0 ) {
		parameters.set ( "OutputFile", OutputFile );
    }
	if ( OutputStart.length() > 0 ) {
		parameters.set ( "OutputStart", OutputStart );
	}
	if ( OutputEnd.length() > 0 ) {
		parameters.set ( "OutputEnd", OutputEnd );
	}
    if ( Precision.length() > 0 ) {
        parameters.set ( "Precision", Precision );
    }
    if ( A.length() > 0 ) {
        parameters.set ( "A", A );
    }
    if ( B.length() > 0 ) {
        parameters.set ( "B", B );
    }
    if ( C.length() > 0 ) {
        parameters.set ( "C", C );
    }
    if ( E.length() > 0 ) {
        parameters.set ( "E", E );
    }
    if ( F.length() > 0 ) {
        parameters.set ( "F", F );
    }
    if ( Replace.length() > 0 ) {
        parameters.set ( "Replace", Replace );
    }
    if ( Close.length() > 0 ) {
        parameters.set ( "Close", Close );
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
    String Type = __Type_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();  
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
    String A = __A_JTextField.getText().trim();
    String B = __B_JTextField.getText().trim();
    String C = __C_JTextField.getText().trim();
    String E = __E_JTextField.getText().trim();
    String F = __F_JTextField.getText().trim();
    String Replace = __Replace_JComboBox.getSelected();
    String Close = __Close_JComboBox.getSelected();
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
	__command.setCommandParameter ( "Precision", Precision );
	__command.setCommandParameter ( "Type", Type );
	__command.setCommandParameter ( "A", A );
	__command.setCommandParameter ( "B", B );
	__command.setCommandParameter ( "C", C );
	__command.setCommandParameter ( "E", E );
	__command.setCommandParameter ( "F", F );
	__command.setCommandParameter ( "Replace", Replace );
	__command.setCommandParameter ( "Close", Close );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteHecDss_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write time series to a HEC-DSS format file," +
		" which can be specified using a full or relative path (relative to the working directory)." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Specifying a DSS file that does not exist will create a new file."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"Enter date/times to a precision appropriate for output time series."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
    	"HEC-DSS parts will be taken from the time series identifier as follows: Apart:Bpart.*.Cpart.Epart.Fpart"),
    	0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The defaults can be overridden with the optional part fields."),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "HEC-DSS file to write:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
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
	JGUIUtil.addComponent(main_JPanel, OutputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Type:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Type_JComboBox = new SimpleJComboBox ( false );
    __Type_JComboBox.add ( __command._PerAver );
    __Type_JComboBox.add ( __command._PerCum );
    __Type_JComboBox.add ( __command._InstVal );
    __Type_JComboBox.add ( __command._InstCum );
    __Type_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Type_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Required - HEC-DSS time series type." ), 
        2, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select a time series ensemble ID from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField (20);
    __OutputStart_JTextField.setToolTipText("Specify the output start using a date/time string or ${Property} notation");
    __OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html>Optional - override the global output start (default=write all data).  <b>NOT FUNCTIONAL</b>.</html>"),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output end:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField (20);
    __OutputEnd_JTextField.setToolTipText("Specify the output end using a date/time string or ${Property} notation");
    __OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "<html>Optional - override the global output end (default=write all data).  <b>NOT FUNCTIONAL</b>.</html>"),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Precision:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Precision_JTextField = new JTextField (20);
    __Precision_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __Precision_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - number of digits after decimal (default=HEC-DSS default)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("A:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __A_JTextField = new JTextField (20);
    __A_JTextField.setToolTipText("Specify the A part, can use ${Property} notation");
    __A_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __A_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - A part to write (default=TSID location type)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("B:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __B_JTextField = new JTextField (20);
    __B_JTextField.setToolTipText("Specify the B part, can use ${Property} notation");
    __B_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __B_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - B part to write (default=TSID location identifier)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("C:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __C_JTextField = new JTextField (20);
    __C_JTextField.setToolTipText("Specify the C part, can use ${Property} notation");
    __C_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __C_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - C part to write (default=TSID data type)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("E:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __E_JTextField = new JTextField (20);
    __E_JTextField.setToolTipText("Specify the E part, can use ${Property} notation");
    __E_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __E_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - E part to write (default=TSID interval)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("F:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __F_JTextField = new JTextField (20);
    __F_JTextField.setToolTipText("Specify the F part, can use ${Property} notation");
    __F_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __F_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - F part to write (default=TSID scenario)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JLabel label = new JLabel ( "Replace entire time series?:" );
    label.setEnabled(false);
    JGUIUtil.addComponent(main_JPanel, label, 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Replace_JComboBox = new SimpleJComboBox ( false );
    __Replace_JComboBox.setEnabled(false); // Under development
    __Replace_JComboBox.add ( "" );
    __Replace_JComboBox.add ( __command._False );
    __Replace_JComboBox.add ( __command._True );
    __Replace_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Replace_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "<html>Optional - replace the entire time series (default="+
        __command._True + "). <b>NOT FUNCTIONAL.</b></html>" ), 
        2, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Close HEC-DSS files after?:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Close_JComboBox = new SimpleJComboBox ( false );
    __Close_JComboBox.add ( "" );
    __Close_JComboBox.add ( __command._False );
    __Close_JComboBox.add ( __command._True );
    __Close_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Close_JComboBox,
        1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - close HEC-DSS file after write (default="+
        __command._False + ")." ), 
        2, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
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

	__ok_JButton = new SimpleJButton("OK", "OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
	
	// Refresh the contents...
    checkGUIState();
    refresh ();
    
    pack();
    JGUIUtil.center( this );
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e)
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
}

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
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = "WriteHecDss_JDialog.refresh";
	String OutputFile = "";
	String OutputStart = "";
	String OutputEnd = "";
	String TSList = "";
    String TSID = "";
    String EnsembleID = "";
    String Precision = "";
    String Type = "";
    String A = "";
    String B = "";
    String C = "";
    String E = "";
    String F = "";
    String Replace = "";
    String Close = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		OutputFile = parameters.getValue ( "OutputFile" );
		OutputStart = parameters.getValue ( "OutputStart" );
		OutputEnd = parameters.getValue ( "OutputEnd" );
		TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        Precision = parameters.getValue ( "Precision" );
        Type = parameters.getValue ( "Type" );
        A = parameters.getValue ( "A" );
        B = parameters.getValue ( "B" );
        C = parameters.getValue ( "C" );
        E = parameters.getValue ( "E" );
        F = parameters.getValue ( "F" );
        Replace = parameters.getValue ( "Replace" );
        Close = parameters.getValue ( "Close" );
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText (OutputFile);
		}
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText (OutputStart);
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText (OutputEnd);
		}
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
        if ( Precision != null ) {
            __Precision_JTextField.setText (Precision);
        }
        if ( Type == null ) {
            // Select default...
            __Type_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Type_JComboBox,Type, JGUIUtil.NONE, null, null ) ) {
                __Type_JComboBox.select ( Type );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nType value \"" + Type +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( A != null ) {
            __A_JTextField.setText (A);
        }
        if ( B != null ) {
            __B_JTextField.setText (B);
        }
        if ( C != null ) {
            __C_JTextField.setText (C);
        }
        if ( E != null ) {
            __E_JTextField.setText (E);
        }
        if ( F != null ) {
            __F_JTextField.setText (F);
        }
        if ( Replace == null ) {
            // Select default...
            __Replace_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Replace_JComboBox,Replace, JGUIUtil.NONE, null, null ) ) {
                __Replace_JComboBox.select ( Replace );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nReplace value \"" + Replace +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Close == null ) {
            // Select default...
            __Close_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Close_JComboBox,Close, JGUIUtil.NONE, null, null ) ) {
                __Close_JComboBox.select ( Close );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nClose value \"" + Close +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	// Regardless, reset the command from the fields...
	OutputFile = __OutputFile_JTextField.getText().trim();
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    Precision = __Precision_JTextField.getText().trim();
    Type = __Type_JComboBox.getSelected();
    A = __A_JTextField.getText().trim();
    B = __B_JTextField.getText().trim();
    C = __C_JTextField.getText().trim();
    E = __E_JTextField.getText().trim();
    F = __F_JTextField.getText().trim();
    Replace = __Replace_JComboBox.getSelected();
    Close = __Close_JComboBox.getSelected();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
	parameters.add ( "OutputFile=" + OutputFile );
	parameters.add ( "OutputStart=" + OutputStart );
	parameters.add ( "OutputEnd=" + OutputEnd );
	parameters.add ( "Precision=" + Precision );
	parameters.add ( "Type=" + Type );
	parameters.add ( "A=" + A );
	parameters.add ( "B=" + B );
	parameters.add ( "C=" + C );
	parameters.add ( "E=" + E );
	parameters.add ( "F=" + F );
	parameters.add ( "Replace=" + Replace );
	parameters.add ( "Close=" + Close );
	__command_JTextArea.setText( __command.toString ( parameters ).trim() );
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
@param ok if false, then the edit is cancelled.  If true, the edit is committed and the dialog is closed.
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
