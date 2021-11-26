// CompareTimeSeries_JDialog - Editor dialog for CompareTimeSeries() command.

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

package rti.tscommandprocessor.commands.ts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
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
Editor dialog for CompareTimeSeries() command.
*/
@SuppressWarnings("serial")
public class CompareTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __browseDiff_JButton = null;
private SimpleJButton __browseSummary_JButton = null;
private SimpleJButton __pathDiff_JButton = null;
private SimpleJButton __pathSummary_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox	__TSID1_JComboBox = null;
private SimpleJComboBox	__TSID2_JComboBox = null;
private SimpleJComboBox	__EnsembleID1_JComboBox = null;
private SimpleJComboBox	__EnsembleID2_JComboBox = null;
private SimpleJComboBox	__MatchLocation_JComboBox = null;
private SimpleJComboBox	__MatchDataType_JComboBox = null;
private SimpleJComboBox	__MatchAlias_JComboBox = null;
private JTextField __Precision_JTextField = null;
private JTextField __Tolerance_JTextField = null;
private SimpleJComboBox	__CompareFlags_JComboBox = null;
private JTextField __AnalysisStart_JTextField;
private JTextField __AnalysisEnd_JTextField;
private JTextField __DiffFlag_JTextField = null;
private SimpleJComboBox	__CreateDiffTS_JComboBox = null;
private JTextField __DifferenceFile_JTextField = null;
private JTextField __SummaryFile_JTextField = null;
private SimpleJComboBox __TableID_JComboBox = null;
private JTextField __DiffCountProperty_JTextField = null;
private JTextField __AllowedDiff_JTextField = null;
// TODO smalers 2021-11-23 add later
//private JTextField __AllowedDiffPerTS_JTextField = null;
private SimpleJComboBox __IfDifferent_JComboBox = null;
private SimpleJComboBox __IfSame_JComboBox = null;
// TODO smalers 2021-08-26 old properties
//private SimpleJComboBox __WarnIfDifferent_JComboBox = null;
//private SimpleJComboBox __WarnIfSame_JComboBox = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private CompareTimeSeries_Command __command = null;
private boolean __ok = false; // Indicates whether user has pressed OK to close the dialog.
private String __working_dir = null;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices choices for TableID value.
*/
public CompareTimeSeries_JDialog ( JFrame parent, CompareTimeSeries_Command command, List<String> tableIDChoices )
{	super(parent, true);
	initialize ( parent, command, tableIDChoices );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browseDiff_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle("Select Difference File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("txt", "Difference File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				try {
					__DifferenceFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"CompareTimeSeries_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory );
				refresh();
			}
		}
	}
	else if ( o == __browseSummary_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle("Select Summary File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("txt", "Summary File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				try {
					__SummaryFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"CompareTimeSeries_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "CompareTimeSeries");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathDiff_JButton ) {
		if ( __pathDiff_JButton.getText().equals(__AddWorkingDirectory) ) {
			__DifferenceFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __DifferenceFile_JTextField.getText() ) );
		}
		else if ( __pathDiff_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __DifferenceFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __DifferenceFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "CompareTimeSeries_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __pathSummary_JButton ) {
		if ( __pathSummary_JButton.getText().equals(__AddWorkingDirectory) ) {
			__SummaryFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __SummaryFile_JTextField.getText() ) );
		}
		else if ( __pathSummary_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __SummaryFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __SummaryFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "CompareTimeSeries_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else {
		// Choices...
		refresh();
	}
}

//Start event handlers for DocumentListener...

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

//...End event handlers for DocumentListener

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TSID1 = __TSID1_JComboBox.getSelected();
	String TSID2 = __TSID2_JComboBox.getSelected();
	String EnsembleID1 = __EnsembleID1_JComboBox.getSelected();
	String EnsembleID2 = __EnsembleID2_JComboBox.getSelected();
	String MatchLocation = __MatchLocation_JComboBox.getSelected();
	String MatchDataType = __MatchDataType_JComboBox.getSelected();
	String MatchAlias = __MatchAlias_JComboBox.getSelected();
	String Precision = __Precision_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
	String CompareFlags = __CompareFlags_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String DiffFlag = __DiffFlag_JTextField.getText().trim();
	String CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
	String DifferenceFile = __DifferenceFile_JTextField.getText().trim();
	String SummaryFile = __SummaryFile_JTextField.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
	String DiffCountProperty = __DiffCountProperty_JTextField.getText().trim();
	String AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	//String AllowedDiffPerTS = __AllowedDiffPerTS_JTextField.getText().trim();
	String IfDifferent = __IfDifferent_JComboBox.getSelected();
	String IfSame = __IfSame_JComboBox.getSelected();
	//String WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	//String WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	__error_wait = false;
	if ( TSID1.length() > 0 ) {
		props.set ( "TSID1", TSID1 );
	}
	if ( TSID2.length() > 0 ) {
		props.set ( "TSID2", TSID2 );
	}
	if ( EnsembleID1.length() > 0 ) {
		props.set ( "EnsembleID1", EnsembleID1 );
	}
	if ( EnsembleID2.length() > 0 ) {
		props.set ( "EnsembleID2", EnsembleID2 );
	}
	if ( MatchLocation.length() > 0 ) {
		props.set ( "MatchLocation", MatchLocation );
	}
	if ( MatchDataType.length() > 0 ) {
		props.set ( "MatchDataType", MatchDataType );
	}
	if ( MatchAlias.length() > 0 ) {
		props.set ( "MatchAlias", MatchAlias );
	}
	if ( Precision.length() > 0 ) {
		props.set ( "Precision", Precision );
	}
	if ( Tolerance.length() > 0 ) {
		props.set ( "Tolerance", Tolerance );
	}
	if ( CompareFlags.length() > 0 ) {
		props.set ( "CompareFlags", CompareFlags );
	}
	if ( AnalysisStart.length() > 0 ) {
		props.set ( "AnalysisStart", AnalysisStart );
	}
	if ( AnalysisEnd.length() > 0 ) {
		props.set ( "AnalysisEnd", AnalysisEnd );
	}
	if ( DiffFlag.length() > 0 ) {
		props.set ( "DiffFlag", DiffFlag );
	}
	if ( CreateDiffTS.length() > 0 ) {
		props.set ( "CreateDiffTS", CreateDiffTS );
	}
    if ( DifferenceFile.length() > 0 ) {
    	props.set ( "DifferenceFile", DifferenceFile );
    }
    if ( SummaryFile.length() > 0 ) {
    	props.set ( "SummaryFile", SummaryFile );
    }
    if ( TableID.length() > 0 ) {
    	props.set ( "TableID", TableID );
    }
    if ( DiffCountProperty.length() > 0 ) {
    	props.set ( "DiffCountProperty", DiffCountProperty );
    }
    if ( AllowedDiff.length() > 0 ) {
        props.set ( "AllowedDiff", AllowedDiff );
    }
    //if ( AllowedDiffPerTS.length() > 0 ) {
    //    props.set ( "AllowedDiffPerTS", AllowedDiffPerTS );
    //}
	if ( IfDifferent.length() > 0 ) {
		props.set ( "IfDifferent", IfDifferent );
	}
	if ( IfSame.length() > 0 ) {
		props.set ( "IfSame", IfSame );
	}
	/*
	if ( WarnIfDifferent.length() > 0 ) {
		props.set ( "WarnIfDifferent", WarnIfDifferent );
	}
	if ( WarnIfSame.length() > 0 ) {
		props.set ( "WarnIfSame", WarnIfSame );
	}
	*/
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
{	String TSID1 = __TSID1_JComboBox.getSelected();
	String TSID2 = __TSID2_JComboBox.getSelected();
	String EnsembleID1 = __EnsembleID1_JComboBox.getSelected();
	String EnsembleID2 = __EnsembleID2_JComboBox.getSelected();
	String MatchLocation = __MatchLocation_JComboBox.getSelected();
	String MatchDataType = __MatchDataType_JComboBox.getSelected();
	String MatchAlias = __MatchAlias_JComboBox.getSelected();
	String Precision = __Precision_JTextField.getText().trim();
	String Tolerance = __Tolerance_JTextField.getText().trim();
	String CompareFlags = __CompareFlags_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String DiffFlag = __DiffFlag_JTextField.getText().trim();
	String CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
	String DifferenceFile = __DifferenceFile_JTextField.getText().trim();
	String SummaryFile = __SummaryFile_JTextField.getText().trim();
    String TableID = __TableID_JComboBox.getSelected();
	String DiffCountProperty = __DiffCountProperty_JTextField.getText().trim();
	String AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	//String AllowedDiffPerTS = __AllowedDiffPerTS_JTextField.getText().trim();
	String IfDifferent = __IfDifferent_JComboBox.getSelected();
	String IfSame = __IfSame_JComboBox.getSelected();
	//String WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	//String WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	__command.setCommandParameter ( "TSID1", TSID1 );
	__command.setCommandParameter ( "TSID2", TSID2 );
	__command.setCommandParameter ( "EnsembleID1", EnsembleID1 );
	__command.setCommandParameter ( "EnsembleID2", EnsembleID2 );
	__command.setCommandParameter ( "MatchLocation", MatchLocation );
	__command.setCommandParameter ( "MatchDataType", MatchDataType );
	__command.setCommandParameter ( "MatchAlias", MatchAlias );
	__command.setCommandParameter ( "Precision", Precision );
	__command.setCommandParameter ( "Tolerance", Tolerance );
	__command.setCommandParameter ( "CompareFlags", CompareFlags );
	__command.setCommandParameter ( "AnalysisStart", AnalysisStart );
	__command.setCommandParameter ( "AnalysisEnd", AnalysisEnd );
	__command.setCommandParameter ( "DiffFlag", DiffFlag );
	__command.setCommandParameter ( "CreateDiffTS", CreateDiffTS );
    __command.setCommandParameter ( "DifferenceFile", DifferenceFile );
    __command.setCommandParameter ( "SummaryFile", SummaryFile );
    __command.setCommandParameter ( "TableID", TableID );
    __command.setCommandParameter ( "DiffCountProperty", DiffCountProperty );
	__command.setCommandParameter ( "AllowedDiff", AllowedDiff );
	//__command.setCommandParameter ( "AllowedDiffPerTS", AllowedDiffPerTS );
	__command.setCommandParameter ( "IfDifferent", IfDifferent );
	__command.setCommandParameter ( "IfSame", IfSame );
	//__command.setCommandParameter ( "WarnIfDifferent", WarnIfDifferent );
	//__command.setCommandParameter ( "WarnIfSame", WarnIfSame );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
@param tableIDChoices list of choices for table identifiers
*/
private void initialize ( JFrame parent, CompareTimeSeries_Command command, List<String> tableIDChoices )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command compares time series, in particular to detect differences for matching pairs of time series." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST); 
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel specifying two time series
    int yts2 = -1;
    JPanel ts2_JPanel = new JPanel();
    ts2_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series (2)", ts2_JPanel );
    
    JGUIUtil.addComponent(ts2_JPanel, new JLabel (
		"Use these parameters to specify two time series to compare." ),
		0, ++yts2, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts2_JPanel, new JLabel (
	    "For example, compare two time series to validate software or a procedure." ),
	    0, ++yts2, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts2_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yts2, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts2_JPanel, new JLabel ( "First time series to compare:" ), 
		0, ++yts2, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID1_JComboBox = new SimpleJComboBox ( true ); // Allow edit
    __TSID1_JComboBox.setToolTipText("Specify the TSID for the first time series, can use ${Property}");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    if ( tsids.size() == 0 ) {
    	tsids.add("");
    }
    else {
    	tsids.add(0, "");
    }
    __TSID1_JComboBox.setData ( tsids );
    __TSID1_JComboBox.addItemListener ( this );
    __TSID1_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(ts2_JPanel, __TSID1_JComboBox,
        1, yts2, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts2_JPanel, new JLabel ( "Second time series compare:" ), 
		0, ++yts2, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSID2_JComboBox = new SimpleJComboBox ( true ); // Allow edit
    __TSID2_JComboBox.setToolTipText("Specify the TSID for the second time series, can use ${Property}");
    __TSID2_JComboBox.setData ( tsids );
    __TSID2_JComboBox.addItemListener ( this );
    __TSID2_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(ts2_JPanel, __TSID2_JComboBox,
        1, yts2, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel specifying two ensembles
    int yEnsemble = -1;
    JPanel ensemble_JPanel = new JPanel();
    ensemble_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Ensembles (2)", ensemble_JPanel );
    
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
		"Use these parameters to specify two ensembles to compare." ),
		0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
	    "For example, compare two ensembles to validate software or a procedure." ),
	    0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JLabel (
	    "The time series in the ensembles will be compared in sequence." ),
	    0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ensemble_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yEnsemble, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JLabel ensemble1Label = new JLabel ( "First ensemble to compare:" );
    JGUIUtil.addComponent(ensemble_JPanel, ensemble1Label, 
		0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleID1_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID1_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> ensembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    ensembleIDs.add(0,""); // Always add default
    __EnsembleID1_JComboBox.setData ( ensembleIDs );
    __EnsembleID1_JComboBox.addItemListener ( this );
    __EnsembleID1_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleID1_JComboBox,
        1, yEnsemble, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JLabel ensemble2Label = new JLabel ( "Second ensemble to compare:" );
    JGUIUtil.addComponent(ensemble_JPanel, ensemble2Label, 
		0, ++yEnsemble, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __EnsembleID2_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID2_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    __EnsembleID2_JComboBox.setData ( ensembleIDs );
    __EnsembleID2_JComboBox.addItemListener ( this );
    __EnsembleID2_JComboBox.getJTextComponent().getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(ensemble_JPanel, __EnsembleID2_JComboBox,
        1, yEnsemble, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel specifying many time series
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series (many)", ts_JPanel );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Use these parameters to specify information to pair time series from the full time series list." ),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
	    "For example, compare time series from from two model runs to determine changes." ),
	    0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Currently all available time series are evaluated, comparing time series that have the same" ),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"time series identifier location and/or data type and/or alias." ),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"The alias can be used to ensure unique identifiers, in which case the location and data type may be ignored." ),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Match location:"),
		0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MatchLocation_JComboBox = new SimpleJComboBox ( false );
	__MatchLocation_JComboBox.setToolTipText("Match the time series location identifier.");
	List<String> matchChoices = new ArrayList<>();
	matchChoices.add ( "" );	// Default
	matchChoices.add ( __command._False );
	matchChoices.add ( __command._True );
	__MatchLocation_JComboBox.setData(matchChoices);
	__MatchLocation_JComboBox.select ( 0 );
	__MatchLocation_JComboBox.addActionListener ( this );
        JGUIUtil.addComponent(ts_JPanel, __MatchLocation_JComboBox,
		1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(ts_JPanel, new JLabel(
		"Optional - match location to find time series pair? (default=" + __command._True + ")."), 
		3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Match data type:"),
		0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MatchDataType_JComboBox = new SimpleJComboBox ( false );
	__MatchDataType_JComboBox.setToolTipText("Match the time series data types.");
	List<String> matchDataTypeChoices = new ArrayList<>();
	matchDataTypeChoices.add ( "" );	// Default
	matchDataTypeChoices.add ( __command._False );
	matchDataTypeChoices.add ( __command._True );
	__MatchDataType_JComboBox.setData(matchDataTypeChoices);
	__MatchDataType_JComboBox.select ( 0 );
	__MatchDataType_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __MatchDataType_JComboBox,
		1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel(
		"Optional - match data type to find time series pair? (default=" + __command._False + ")."), 
		3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Match alias:"),
		0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MatchAlias_JComboBox = new SimpleJComboBox ( false );
	__MatchAlias_JComboBox.setToolTipText("Match the time series aliases.");
	List<String> matchAliasChoices = new ArrayList<>();
	matchAliasChoices.add ( "" ); // Default
	matchAliasChoices.add ( __command._False );
	matchAliasChoices.add ( __command._True );
	__MatchAlias_JComboBox.setData(matchAliasChoices);
	__MatchAlias_JComboBox.select ( 0 );
	__MatchAlias_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __MatchAlias_JComboBox,
		1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel(
		"Optional - match alias to find time series pair? (default=" + __command._False + ")."), 
		3, yts, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel specifying analysis parameters
    int yAnalysis = -1;
    JPanel analysis_JPanel = new JPanel();
    analysis_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Analysis", analysis_JPanel );
    
    JGUIUtil.addComponent(analysis_JPanel, new JLabel (
		"Specify one or more tolerances, separated by commas.  Differences greater than these values will be noted." ),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yAnalysis, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);    

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Precision:" ), 
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Precision_JTextField = new JTextField ( 5 );
	__Precision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Precision_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - digits after decimal to compare (default=available digits are used)."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Tolerance:" ), 
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Tolerance_JTextField = new JTextField ( 15 );
	__Tolerance_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __Tolerance_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - tolerance(s) to indicate difference (e.g., .01, .1, default=exact comparison)."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Compare flags?:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CompareFlags_JComboBox = new SimpleJComboBox ( false );
	__CompareFlags_JComboBox.setToolTipText("Should data value flags be compared?  The comparison is case-specific.");
	List<String> flagChoices = new ArrayList<>();
	flagChoices.add ( "" );	// Default
	flagChoices.add ( __command._False );
	flagChoices.add ( __command._True );
	__CompareFlags_JComboBox.setData(flagChoices);
	__CompareFlags_JComboBox.select ( 0 );
	__CompareFlags_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __CompareFlags_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - compare flags? (default=" + __command._False + ")."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Analysis start:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisStart_JTextField = new JTextField ( "", 20 );
    __AnalysisStart_JTextField.setToolTipText("Specify the analysis start using a date/time string or ${Property} notation");
    __AnalysisStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisStart_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis start date/time (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Analysis end:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AnalysisEnd_JTextField = new JTextField ( "", 20 );
    __AnalysisEnd_JTextField.setToolTipText("Specify the analysis end using a date/time string or ${Property} notation");
    __AnalysisEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AnalysisEnd_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
        "Optional - analysis end date/time (default=full time series period)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Difference flag:" ), 
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DiffFlag_JTextField = new JTextField ( 15 );
	__DiffFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __DiffFlag_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - 1-character flag to use for values that are different."),
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Allowed # of differences (total):"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AllowedDiff_JTextField = new JTextField ( 5 );
    __AllowedDiff_JTextField.setToolTipText("Number of differences allowed for all time series.");
    __AllowedDiff_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AllowedDiff_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel( "Optional - when checking for differences (default=0)"), 
        3, yAnalysis, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Allowed # of differences (per TS):"),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AllowedDiffPerTS_JTextField = new JTextField ( 5 );
    __AllowedDiffPerTS_JTextField.setToolTipText("Number of differences allowed per time series.");
    __AllowedDiffPerTS_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __AllowedDiffPerTS_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel( "Optional - when checking for differences (default=0)"), 
        3, yAnalysis, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        */

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Action if different:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfDifferent_JComboBox = new SimpleJComboBox ( false );
	List<String> diffChoices = new ArrayList<>();
	diffChoices.add ( "" );	// Default
	diffChoices.add ( __command._Ignore );
	diffChoices.add ( __command._Warn );
	diffChoices.add ( __command._Fail );
	__IfDifferent_JComboBox.setData(diffChoices);
	__IfDifferent_JComboBox.select ( 0 );
	__IfDifferent_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __IfDifferent_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - action if time series are different (default=" + __command._Ignore + ")."),
		3, yAnalysis, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Action if same:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfSame_JComboBox = new SimpleJComboBox ( false );
	List<String> sameChoices = new ArrayList<>();
	sameChoices.add ( "" );	// Default
	sameChoices.add ( __command._Ignore );
	sameChoices.add ( __command._Warn );
	sameChoices.add ( __command._Fail );
	__IfSame_JComboBox.setData(sameChoices);
	__IfSame_JComboBox.select ( 0 );
	__IfSame_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __IfSame_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - action if time series are the same (default=" + __command._Ignore + ")."),
		3, yAnalysis, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    /*
    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Warn if different?:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WarnIfDifferent_JComboBox = new SimpleJComboBox ( false );
	List<String> diffChoices = new ArrayList<>();
	diffChoices.add ( "" );	// Default
	diffChoices.add ( __command._False );
	diffChoices.add ( __command._True );
	__WarnIfDifferent_JComboBox.setData(diffChoices);
	__WarnIfDifferent_JComboBox.select ( 0 );
	__WarnIfDifferent_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(analysis_JPanel, __WarnIfDifferent_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - generate a warning if different? (default=" + __command._False + ")."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(analysis_JPanel, new JLabel ( "Warn if same?:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__WarnIfSame_JComboBox = new SimpleJComboBox ( false );
	List<String> sameChoices = new ArrayList<>();
	sameChoices.add ( "" );	// Default
	sameChoices.add ( __command._False );
	sameChoices.add ( __command._True );
	__WarnIfSame_JComboBox.setData(sameChoices);
	__WarnIfSame_JComboBox.select ( 0 );
	__WarnIfSame_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(analysis_JPanel, __WarnIfSame_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(analysis_JPanel, new JLabel(
		"Optional - generate a warning if same? (default=" + __command._False + ")."), 
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	*/
    
    // Panel specifying output parameters
    int yOut = -1;
    JPanel out_JPanel = new JPanel();
    out_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output", out_JPanel );

    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"Indicate whether output time series should be created indicating the difference between time series." ),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel (
		"An output table containing time series differences can also be created (or appended to)." ),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yOut, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST); 
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Create difference time series?:"),
		0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
  	__CreateDiffTS_JComboBox = new SimpleJComboBox ( false );
  	List<String> createChoices = new ArrayList<>();
  	createChoices.add ( "" );	// Default
  	createChoices.add ( __command._False );
  	createChoices.add ( __command._True );
  	createChoices.add ( __command._IfDifferent );
  	__CreateDiffTS_JComboBox.setData(createChoices);
   	__CreateDiffTS_JComboBox.select ( 0 );
   	__CreateDiffTS_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(out_JPanel, __CreateDiffTS_JComboBox,
    		1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
		"Optional - create a time series TS2 - TS1? (default=" + __command._False + ")."), 
		3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Difference file:" ), 
		0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DifferenceFile_JTextField = new JTextField ( 50 );
	__DifferenceFile_JTextField.setToolTipText("Specify the path to the difference file for all time series, can use ${Property} notation");
	__DifferenceFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel DifferenceFile_JPanel = new JPanel();
	DifferenceFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(DifferenceFile_JPanel, __DifferenceFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseDiff_JButton = new SimpleJButton ( "...", this );
	__browseDiff_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(DifferenceFile_JPanel, __browseDiff_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathDiff_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(DifferenceFile_JPanel, __pathDiff_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(out_JPanel, DifferenceFile_JPanel,
		1, yOut, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Summary file:" ), 
		0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SummaryFile_JTextField = new JTextField ( 50 );
	__SummaryFile_JTextField.setToolTipText("Specify the path to the summary file for differences, can use ${Property} notation");
	__SummaryFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel SummaryFile_JPanel = new JPanel();
	SummaryFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(SummaryFile_JPanel, __SummaryFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseSummary_JButton = new SimpleJButton ( "...", this );
	__browseSummary_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(SummaryFile_JPanel, __browseSummary_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathSummary_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(SummaryFile_JPanel, __pathSummary_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(out_JPanel, SummaryFile_JPanel,
		1, yOut, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Table ID:" ), 
        0, ++yOut, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JComboBox = new SimpleJComboBox ( 12, true ); // Allow edit
    __TableID_JComboBox.setToolTipText("Specify the table ID for comparison output or use ${Property} notation");
    tableIDChoices.add(0,""); // Add blank to ignore table
    __TableID_JComboBox.setData ( tableIDChoices );
    __TableID_JComboBox.addItemListener ( this );
    //__TableID_JComboBox.setMaximumRowCount(tableIDChoices.size());
    JGUIUtil.addComponent(out_JPanel, __TableID_JComboBox,
        1, yOut, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel( "Optional - table to receive difference output."), 
        3, yOut, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(out_JPanel, new JLabel ( "Difference count property:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DiffCountProperty_JTextField = new JTextField ( "", 20 );
    __DiffCountProperty_JTextField.setToolTipText("Property name to set difference count (use to check non-zero count).");
    __DiffCountProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(out_JPanel, __DiffCountProperty_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(out_JPanel, new JLabel(
        "Optional - property to set to difference count (default=don't set)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " command" );

    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable...
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String TSID1 = "";
	String TSID2 = "";
	String EnsembleID1 = "";
	String EnsembleID2 = "";
	String MatchLocation = "";
	String MatchDataType = "";
	String MatchAlias = "";
	String Precision = "";
	String Tolerance = "";
	String CompareFlags = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String DiffFlag = "";
	String CreateDiffTS = "";
	String DifferenceFile = "";
	String SummaryFile = "";
	String TableID = "";
	String DiffCountProperty = "";
	String AllowedDiff = "";
	//String AllowedDiffPerTS = "";
	String IfDifferent = "";
	String IfSame = "";
	//String WarnIfDifferent = "";
	//String WarnIfSame = "";
	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		TSID1 = props.getValue ( "TSID1" );
		TSID2 = props.getValue ( "TSID2" );
		EnsembleID1 = props.getValue ( "EnsembleID1" );
		EnsembleID2 = props.getValue ( "EnsembleID2" );
		MatchLocation = props.getValue ( "MatchLocation" );
		MatchDataType = props.getValue ( "MatchDataType" );
		MatchAlias = props.getValue ( "Alias" );
		Precision = props.getValue ( "Precision" );
		Tolerance = props.getValue ( "Tolerance" );
		CompareFlags = props.getValue ( "CompareFlags" );
		AnalysisStart = props.getValue("AnalysisStart");
		AnalysisEnd = props.getValue("AnalysisEnd");
		DiffFlag = props.getValue ( "DiffFlag" );
		CreateDiffTS = props.getValue ( "CreateDiffTS" );
        DifferenceFile = props.getValue ( "DifferenceFile" );
        SummaryFile = props.getValue ( "SummaryFile" );
        TableID = props.getValue ( "TableID" );
        DiffCountProperty = props.getValue ( "DiffCountProperty" );
		AllowedDiff = props.getValue ( "AllowedDiff" );
		//AllowedDiffPerTS = props.getValue ( "AllowedDiffPerTS" );
		IfDifferent = props.getValue ( "IfDifferent" );
		IfSame = props.getValue ( "IfSame" );
		//WarnIfDifferent = props.getValue ( "WarnIfDifferent" );
		//WarnIfSame = props.getValue ( "WarnIfSame" );
        // Select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID1_JComboBox, TSID1, JGUIUtil.NONE, null, null ) ) {
            __TSID1_JComboBox.select ( TSID1 );
        }
        else {
            // Automatically add to the list...
            if ( (TSID1 != null) && (TSID1.length() > 0) ) {
                __TSID1_JComboBox.insertItemAt ( TSID1, 0 );
                // Select...
                __TSID1_JComboBox.select ( TSID1 );
            }
            else {
                // Select the first choice...
                if ( __TSID1_JComboBox.getItemCount() > 0 ) {
                    __TSID1_JComboBox.select ( 0 );
                }
            }
        }
        // Select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __TSID2_JComboBox, TSID2, JGUIUtil.NONE, null, null ) ) {
            __TSID2_JComboBox.select ( TSID2 );
        }
        else {
            // Automatically add to the list...
            if ( (TSID2 != null) && (TSID2.length() > 0) ) {
                __TSID2_JComboBox.insertItemAt ( TSID2, 0 );
                // Select...
                __TSID2_JComboBox.select ( TSID2 );
            }
            else {
                // Select the first choice...
                if ( __TSID2_JComboBox.getItemCount() > 0 ) {
                    __TSID2_JComboBox.select ( 0 );
                }
            }
        }
        if ( EnsembleID1 == null ) {
            // Select default...
            __EnsembleID1_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID1_JComboBox, EnsembleID1, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID1_JComboBox.select ( EnsembleID1 );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID1 value \"" + EnsembleID1 +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( EnsembleID2 == null ) {
            // Select default...
            __EnsembleID2_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID2_JComboBox, EnsembleID2, JGUIUtil.NONE, null, null ) ) {
                __EnsembleID2_JComboBox.select ( EnsembleID2 );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEnsembleID2 value \"" + EnsembleID2 +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__MatchLocation_JComboBox, MatchLocation,JGUIUtil.NONE, null, null ) ) {
			__MatchLocation_JComboBox.select ( MatchLocation );
		}
		else {
		    if ( (MatchLocation == null) || MatchLocation.equals("") ) {
				// New command...select the default...
				__MatchLocation_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"MatchLocation parameter \"" + MatchLocation +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __MatchDataType_JComboBox, MatchDataType, JGUIUtil.NONE, null, null ) ) {
			__MatchDataType_JComboBox.select ( MatchDataType );
		}
		else {
		    if ( (MatchDataType == null) || MatchDataType.equals("") ) {
				// New command...select the default...
				__MatchDataType_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"MatchDataType parameter \"" + MatchDataType +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem( __MatchAlias_JComboBox, MatchAlias, JGUIUtil.NONE, null, null ) ) {
			__MatchAlias_JComboBox.select ( MatchAlias );
		}
		else {
		    if ( (MatchAlias == null) || MatchAlias.equals("") ) {
				// New command...select the default...
				__MatchAlias_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"MatchAlias parameter \"" + MatchAlias +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( Precision != null ) {
			__Precision_JTextField.setText ( Precision );
		}
		if ( Tolerance != null ) {
			__Tolerance_JTextField.setText ( Tolerance );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__CompareFlags_JComboBox, CompareFlags, JGUIUtil.NONE, null, null ) ) {
			__CompareFlags_JComboBox.select ( CompareFlags );
		}
		else {
		    if ( (CompareFlags == null) || CompareFlags.equals("") ) {
				// New command...select the default...
				__CompareFlags_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"CompareFlags parameter \"" + CompareFlags + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( AnalysisStart != null ) {
			__AnalysisStart_JTextField.setText ( AnalysisStart );
		}
		if ( AnalysisEnd != null ) {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}
		if ( DiffFlag != null ) {
			__DiffFlag_JTextField.setText ( DiffFlag );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__CreateDiffTS_JComboBox, CreateDiffTS, JGUIUtil.NONE, null, null ) ) {
				__CreateDiffTS_JComboBox.select ( CreateDiffTS );
		}
		else {
		    if ( (CreateDiffTS == null) || CreateDiffTS.equals("") ) {
				// New command...select the default...
				__CreateDiffTS_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"CreateDiffTS parameter \"" + CreateDiffTS +
				"\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( DifferenceFile != null ) {
			__DifferenceFile_JTextField.setText (DifferenceFile);
		}
		if ( SummaryFile != null ) {
			__SummaryFile_JTextField.setText (SummaryFile);
		}
        if ( TableID == null ) {
            // Select default...
            __TableID_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
                __TableID_JComboBox.select ( TableID );
            }
            else {
            	// OK to add to list since does not need to exist
            	__TableID_JComboBox.add(TableID);
            	__TableID_JComboBox.select(TableID);
                //Message.printWarning ( 1, routine,
                //"Existing command references an invalid\nTableID value \"" + TableID +
                //"\".  Select a different value or Cancel.");
                //__error_wait = true;
            }
        }
		if ( DiffCountProperty != null ) {
			__DiffCountProperty_JTextField.setText ( DiffCountProperty );
		}
        if ( AllowedDiff != null ) {
            __AllowedDiff_JTextField.setText ( AllowedDiff );
        }
        //if ( AllowedDiffPerTS != null ) {
        //    __AllowedDiffPerTS_JTextField.setText ( AllowedDiffPerTS );
        //}
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfDifferent_JComboBox, IfDifferent, JGUIUtil.NONE, null, null ) ) {
			__IfDifferent_JComboBox.select ( IfDifferent );
		}
		else {
		    if ( (IfDifferent == null) || IfDifferent.equals("") ) {
				// New command...select the default...
				__IfDifferent_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"IfDifferent parameter \"" +
				IfDifferent + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfSame_JComboBox, IfSame, JGUIUtil.NONE, null, null ) ) {
			__IfSame_JComboBox.select ( IfSame );
		}
		else {
		    if ( (IfSame == null) || IfSame.equals("") ) {
				// New command...select the default...
				__IfSame_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"IfSame parameter \"" + IfSame + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		/*
		if ( JGUIUtil.isSimpleJComboBoxItem(__WarnIfDifferent_JComboBox, WarnIfDifferent, JGUIUtil.NONE, null, null ) ) {
			__WarnIfDifferent_JComboBox.select ( WarnIfDifferent );
		}
		else {
		    if ( (WarnIfDifferent == null) || WarnIfDifferent.equals("") ) {
				// New command...select the default...
				__WarnIfDifferent_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"WarnIfDifferent parameter \"" +
				WarnIfDifferent + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__WarnIfSame_JComboBox, WarnIfSame, JGUIUtil.NONE, null, null ) ) {
			__WarnIfSame_JComboBox.select ( WarnIfSame );
		}
		else {
		    if ( (WarnIfSame == null) || WarnIfSame.equals("") ) {
				// New command...select the default...
				__WarnIfSame_JComboBox.select ( 0 );
			}
			else {
			    // Bad user command...
				Message.printWarning ( 1, routine, "Existing command references an invalid\n"+
				"WarnIfSame parameter \"" + WarnIfSame + "\".  Select a\ndifferent value or Cancel." );
			}
		}
		*/
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	TSID1 = __TSID1_JComboBox.getSelected();
	TSID2 = __TSID2_JComboBox.getSelected();
	EnsembleID1 = __EnsembleID1_JComboBox.getSelected();
	EnsembleID2 = __EnsembleID2_JComboBox.getSelected();
	MatchLocation = __MatchLocation_JComboBox.getSelected();
	MatchDataType = __MatchDataType_JComboBox.getSelected();
	MatchAlias = __MatchAlias_JComboBox.getSelected();
	Precision = __Precision_JTextField.getText().trim();
	Tolerance = __Tolerance_JTextField.getText().trim();
	CompareFlags = __CompareFlags_JComboBox.getSelected();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	DiffFlag = __DiffFlag_JTextField.getText().trim();
	CreateDiffTS = __CreateDiffTS_JComboBox.getSelected();
	DifferenceFile = __DifferenceFile_JTextField.getText().trim();
	SummaryFile = __SummaryFile_JTextField.getText().trim();
    TableID = __TableID_JComboBox.getSelected();
    DiffCountProperty = __DiffCountProperty_JTextField.getText().trim();
	AllowedDiff = __AllowedDiff_JTextField.getText().trim();
	//AllowedDiffPerTS = __AllowedDiffPerTS_JTextField.getText().trim();
	IfDifferent = __IfDifferent_JComboBox.getSelected();
	IfSame = __IfSame_JComboBox.getSelected();
	//WarnIfDifferent = __WarnIfDifferent_JComboBox.getSelected();
	//WarnIfSame = __WarnIfSame_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSID1=" + TSID1 );
	props.add ( "TSID2=" + TSID2 );
	props.add ( "EnsembleID1=" + EnsembleID1 );
	props.add ( "EnsembleID2=" + EnsembleID2 );
	props.add ( "MatchLocation=" + MatchLocation );
	props.add ( "MatchDataType=" + MatchDataType );
	props.add ( "MatchAlias=" + MatchAlias );
	props.add ( "Precision=" + Precision );
	props.add ( "Tolerance=" + Tolerance );
	props.add ( "CompareFlags=" + CompareFlags );
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "DiffFlag=" + DiffFlag );
	props.add ( "CreateDiffTS=" + CreateDiffTS );
    props.add ( "DifferenceFile=" + DifferenceFile );
    props.add ( "SummaryFile=" + SummaryFile );
    props.add ( "TableID=" + TableID );
    props.add ( "DiffCountProperty=" + DiffCountProperty );
	props.add ( "AllowedDiff=" + AllowedDiff );
	//props.add ( "AllowedDiffPerTS=" + AllowedDiffPerTS );
	props.add ( "IfDifferent=" + IfDifferent );
	props.add ( "IfSame=" + IfSame );
	//props.add ( "WarnIfDifferent=" + WarnIfDifferent );
	//props.add ( "WarnIfSame=" + WarnIfSame );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( __pathDiff_JButton != null ) {
		if ( (DifferenceFile != null) && !DifferenceFile.isEmpty() ) {
			__pathDiff_JButton.setEnabled ( true );
			File f = new File ( DifferenceFile );
			if ( f.isAbsolute() ) {
				__pathDiff_JButton.setText ( __RemoveWorkingDirectory );
				__pathDiff_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathDiff_JButton.setText ( __AddWorkingDirectory );
            	__pathDiff_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathDiff_JButton.setEnabled(false);
		}
	}
	if ( __pathSummary_JButton != null ) {
		if ( (SummaryFile != null) && !SummaryFile.isEmpty() ) {
			__pathSummary_JButton.setEnabled ( true );
			File f = new File ( SummaryFile );
			if ( f.isAbsolute() ) {
				__pathSummary_JButton.setText ( __RemoveWorkingDirectory );
				__pathSummary_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathSummary_JButton.setText ( __AddWorkingDirectory );
            	__pathSummary_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathSummary_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok )
{	__ok = ok;
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
