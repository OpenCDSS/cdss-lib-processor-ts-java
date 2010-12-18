package rti.tscommandprocessor.commands.ts;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
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

import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSUtil;
import RTi.Util.GUI.GUIUtil;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.ReportJFrame;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandListUI;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Math.BestFitIndicatorType;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Math.RegressionType;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

public class FillMixedStation_JDialog extends JDialog
implements ActionListener,
	 	   ItemListener,
	 	   KeyListener,
	 	   ListSelectionListener,
		   WindowListener
{

// Controls are defined in logical order -- The order they appear in the dialog box and documentation.

/**
Command object used in command editing mode to edit FillMixedStationCommand().  Or an instance
created to store command parameter information if the tool is being used.
*/
private FillMixedStation_Command __command = null;
/**
Used in tool mode to transfer tool commands to processor, recognizing main UI state.
*/
private CommandListUI __commandUI = null;
/**
Used with tool mode to access time series results and in command mode to access command information (such
as the list of time series from previous commands).
*/
private CommandProcessor __processor = null;
/**
Working directory for output report.
*/
private String __working_dir = null;

// Members controlling the execution mode. This class can run as a command or as a tool from the tool menu.
private JTextArea __Command_JTextArea = null; // Command as JTextArea

private SimpleJComboBox	__DependentTSList_JComboBox = null;
private JLabel __DependentTSID_JLabel = null;
private SimpleJComboBox __DependentTSID_JComboBox = null;

private SimpleJComboBox	__IndependentTSList_JComboBox = null;
private JLabel __IndependentTSID_JLabel = null;
private SimpleJComboBox __IndependentTSID_JComboBox = null;

private SimpleJComboBox __BestFitIndicator_JComboBox = null;
private JCheckBox[]	__AnalysisMethod_JCheckBox  = null;
private SimpleJComboBox	__NumberOfEquations_JComboBox = null;
private JCheckBox[] __Transformation_JCheckBox = null;

private JTextField __MinimumDataCount_JTextField;
private JTextField __MinimumR_JTextField;

private JTextField __Intercept_JTextField = null;
private JTextField __ConfidenceInterval_JTextField = null;
private JTextField __AnalysisStart_JTextField = null;
private JTextField __AnalysisEnd_JTextField = null;
private JTextField __FillStart_JTextField = null;
private JTextField __FillEnd_JTextField = null;
private JTextField  __FillFlag_JTextField = null;
private JTextField __OutputFile_JTextField = null;						

private SimpleJButton __browse_JButton = null;
private SimpleJButton __viewOutputFile_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __close_JButton = null;
private SimpleJButton __analyze_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __fillDependents_JButton = null;

private String __view_String = "View Output File";
private String __view_Tip = "View output file containing the analysis results.";

private JTextField __statusJTextField = null;

// Cancel button: used when running as a TSTool command.

private String __cancel_String = "Cancel";
private String __cancel_Tip = "Close the window, whitout returning the command to TSTool.";

// Close button: used when running as a TSTool tool.

private String __close_String = "Close";
private String __close_Tip = "Do not perform the analysis and close the window.";

// OK button: used only when running as a TSTool command.

private String __ok_String = "OK";
private String __ok_Tip = "Close the window, returning the command to TSTool.";

// Analyze button: used only when running as a TSTool tool.

private String __analyze_String = "Analyze";
private String __analyze_Tip = "Perform the analysis and create the output file";

// FIXME SAM 2009-08-26 Evaluate whether should be a checkbox/button to create separate FillRegression,
// FillMOVE2() commands - currently default to using single FillMixedStation() command.
// Used only when running as a TSTool tool.
//private SimpleJButton __createFillMixedStationCommand_JButton = null;
//private String __createFillMixedStationCommand_String = "Create FillMixedStation Command";
//private String __createFillMixedStationCommand_Tip = "Create fill commands using the best fit.";

// Used only when running as a TSTool tool.
private SimpleJButton __copyCommandsToTSTool_JButton = null;
private String __copyCommandsToTSTool_String = "Copy Command to TSTool";
private String __copyCommandsToTSTool_Tip = "Copy command to TSTool.";

// fill button: used only when running as a TSTool tool.

//private String __fillDependents_String = "Fill Dependents";
//private String __fillDependents_Tip = "Fill dependents using best fit.";

// Member initialized by the createFillCommands() method and used by the
// the updateFillCommandsControl() and copyCommandsToTSTool() methods.
// The current design produces only one command since the tool is designed to
// fill StateMod natural flow time series.
List __fillCommands_Vector = null;

// Member flag Used to prevent ValueChange method to execute refresh()
boolean ignoreValueChanged = false;

private boolean	__error_wait = false;
private boolean	__first_time = true;
private boolean	__ok = false;

/**
Constructor when calling as a TSTool command.
In this case the Dialog is modal: super( parent, true ).
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public FillMixedStation_JDialog ( JFrame parent, FillMixedStation_Command command )
{
    // Modal dialog
	super( parent, true );
	__command = command;
	__processor = __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__processor, __command );
	// Initialize the dialog.
	initialize ( parent );
}

/**
Constructor when calling as a TSTool tool.  In this case the Dialog is non-modal, although it may still
obscure the main window.
@param parent JFrame class instantiating this class.
@param processor time series processor, needed by the tool to access time series for analyzing
@param ui interface between main UI and other code.
*/
public FillMixedStation_JDialog ( JFrame parent, TSCommandProcessor processor, CommandListUI ui )
{  
	super( parent, false );
	String routine = getClass().getName();

	// Initialize the dialog
	__commandUI = ui;
	__processor = processor;
	// TODO SAM 2009-08-31 The following is for troubleshooting and can be removed later
	if ( processor == null ) {
	    Message.printStatus ( 2, routine, "Processor is null for temporary command used with tool" );
	}
	else {
	    Object o = null;
	    try {
	        o = processor.getPropContents("TSResultsList");
	    }
	    catch ( Exception e ) {
	        Message.printWarning ( 3, routine,
	            "Error getting results time series list from tool processor (" + e + ").");
	    }
	    if ( o == null ) {
	        Message.printWarning ( 3, routine, "Time series list from tool processor is null.");
	    }
	    else {
	        List tslist = (List)o;
	        Message.printStatus ( 2, routine, "Processor in tool mode has " + tslist.size() + " time series.");
	    }
	}
	// Create a command instance to save command parameters, etc.
	Message.printStatus ( 2, routine, "Creating FillMixedStation command to hold tool data.");
	__command = new FillMixedStation_Command(false); // to hold parameters
	__command.setCommandProcessor ( processor ); // to get working directory, etc., time series are in main GUI
	__working_dir = processor.getInitialWorkingDir();
	initialize ( parent );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{
	String mthd = "FillMixedStation_JDialog.actionPerformed", mssg;
	Object o = event.getSource();

	if ( o == __browse_JButton ) {
	    // Select output file
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select output file");
		SimpleFileFilter sff;
		sff = new SimpleFileFilter("txt","Output file");
		fc.addChoosableFileFilter(sff);

		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName();
			String path = fc.getSelectedFile().getPath();

			if (filename == null || filename.equals("")) {
				return;
			}

			if ( path != null ) {
				__OutputFile_JTextField.setText( path );
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
	else if ( o == __viewOutputFile_JButton ) {
	    // View analysis results
        String outputFile = __OutputFile_JTextField.getText();
        String outputFileFull = outputFile;
	    try {
	        outputFileFull = IOUtil.verifyPathForOS(IOUtil.adjustPath ( __working_dir, outputFile));
    		PropList reportProp = new PropList("ReportJFrame.props");
    		reportProp.set("TotalWidth=750");
    		reportProp.set("TotalHeight=550");
    		reportProp.set("Title=" + __command.getCommandName() );
    		reportProp.set("DisplayFont=Courier");
    		reportProp.set("DisplayStyle=" + Font.PLAIN);
    		reportProp.set("DisplaySize=11");
    		reportProp.set("PrintFont=Courier");
    		reportProp.set("PrintStyle=" + Font.PLAIN);
    		reportProp.set("PrintSize=7");
    		//reportProp.set("PageLength=100");
    		reportProp.set("PageLength=50000");
    		reportProp.set("Title = " + outputFile);
    
    		List strings = null;
    		if ( !outputFile.equals("") ) {
    		    strings = IOUtil.fileToStringList(outputFileFull);
    		}
    
    		// End instantiate the Report viewer.
    		new ReportJFrame(strings, reportProp);
	    }
	    catch ( Exception e ) {
	        Message.printWarning(1, mthd, "Error displaying analysis results file \"" +
	            outputFileFull + "\" (" + e + ").");
	    }
	}

	// Cancel button - valid only under the command mode
	else if ( o == __cancel_JButton ) {
		response ( false );
	}

	// Close button - valid only under the tool mode
	else if ( o == __close_JButton ) {
		response ( false );
	}

	// OK button - Active only under the command mode
	else if ( o == __ok_JButton ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}

	// Analyze button - Active only under the tool mode
	else if ( o == __analyze_JButton ) {
	    refresh();
        checkInput();
        Message.printStatus(2, mthd, "Analyze button pushed.  error_wait=" + __error_wait );
        if ( !__error_wait ) {
            // Commit to local command used to manage parameters
    		commitEdits ();
			try {
			    mssg = "Running Mixed Station Analysis...";
			    Message.printStatus(2, mthd, mssg );
			    setStatusText ( mssg );
			    GUIUtil.setWaitCursor(this, true);
				__command.runCommand( -1 );
				GUIUtil.setWaitCursor(this, false);
				mssg = "...done running Mixed Station Analysis.";
				Message.printStatus(2, mthd, mssg );
				setStatusText ( mssg );
			}
			catch ( Exception e ) {
				Message.printWarning ( 2, mthd, e );
				mssg = "Error executing the analysis.  Please check the log file for details (" + e + ").";
				Message.printWarning ( 1, mthd, mssg );
				GUIUtil.setWaitCursor(this, false);
			}	
			// Enable the runCommand dependent buttons, even if an error occurred, but only if output exists.
			if ( (__OutputFile_JTextField.getText() != null) && !__OutputFile_JTextField.getText().equals("") &&
			    IOUtil.fileExists(getOutputFileFromInterface(true))) {
			    __viewOutputFile_JButton.setEnabled ( true );
			}
			else {
			    __viewOutputFile_JButton.setEnabled ( false );
			}
			//__createFillMixedStationCommand_JButton.setEnabled ( true );
			//__fillDependents_JButton.setEnabled ( true );
		}
	}

	// Create fill Commands button - Active only under the tool mode
	/* FIXME SAM 2009-08-26 See comments for data members
	else if ( o == __createFillMixedStationCommand_JButton ) {
		createFillCommands();
		updateFillCommandsControl();
		// Enable the fill __copyFillCommandsToTSTool_JButton.
		__copyFillCommandsToTSTool_JButton.setEnabled( true );
	}
	*/

	// Copy Commands To TSTool button - Active only under the tool mode
	// TODO [LT 2005-06-01] There may be room for improvements here:
	// Either warn the user that the results of the analysis in memory
	// may not reflect the setting in the interface (if the setting are
	// changed after the analysis, or disable all the runCommand
	// dependent buttons if the settings are changed. 
	else if ( o == __copyCommandsToTSTool_JButton ) {
		copyCommandsToTSTool();
	}

	// Fill dependents button - Active only under the tool mode
	// TODO [LT 2005-06-01] There may be room for improvements here:
	// Either warn the user that the results of the analysis in memory
	// may not reflect the setting in the interface (if the setting are
	// changed after the analysis, or disable all the runCommand
	// dependent buttons if the settings are changed. 
	else if ( o == __fillDependents_JButton ) {
		fillDependents();
	}
	
    else if ( (__DependentTSList_JComboBox != null) && (o == __DependentTSList_JComboBox) ) {
        checkGUIState();
        refresh ();
    }
    else if ( (__IndependentTSList_JComboBox != null) && (o == __IndependentTSList_JComboBox) ) {
        checkGUIState();
        refresh ();
    }
}

/**
Check the GUI state to make sure that appropriate components are enabled/disabled.
*/
private void checkGUIState ()
{
    String TSList = __DependentTSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
        TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
        TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __DependentTSID_JComboBox.setEnabled(true);
        __DependentTSID_JLabel.setEnabled ( true );
    }
    else {
        __DependentTSID_JComboBox.setEnabled(false);
        __DependentTSID_JLabel.setEnabled ( false );
    }

    TSList = __IndependentTSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
        TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
        TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
        __IndependentTSID_JComboBox.setEnabled(true);
        __IndependentTSID_JLabel.setEnabled ( true );
    }
    else {
        __IndependentTSID_JComboBox.setEnabled(false);
        __IndependentTSID_JLabel.setEnabled ( false );
    }
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{   String routine = getClass().getName() + ".checkInput";
	// Get the values from the interface.
	String DependentTSList = __DependentTSList_JComboBox.getSelected();
	String DependentTSID = __DependentTSID_JComboBox.getSelected();
	String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
	String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
	String AnalysisMethod = getAnalysisMethodFromInterface();
	String Transformation = getTransformationFromInterface();
	String NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String MinimumDataCount = __MinimumDataCount_JTextField.getText().trim();
	String MinimumR = __MinimumR_JTextField.getText().trim();
	String BestFitIndicator = __BestFitIndicator_JComboBox.getSelected();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String Intercept = __Intercept_JTextField.getText().trim();
	String ConfidenceInterval = __ConfidenceInterval_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();

	// Put together the list of parameters to check...
	PropList props = new PropList ( "" );
	// DependentTSList
	if ( DependentTSList != null && DependentTSList.length() > 0 ) {
		props.set( "DependentTSList", DependentTSList );
	}
	// DependentTSID
	if ( DependentTSID != null && DependentTSID.length() > 0 ) {
		props.set( "DependentTSID", DependentTSID );
	}
	// IndependentTSList
	if ( IndependentTSList != null && IndependentTSList.length() > 0 ) {
		props.set( "IndependentTSList", IndependentTSList );
	}
	// IndependentTSID
	if ( IndependentTSID != null && IndependentTSID.length() > 0 ) {
		props.set( "IndependentTSID", IndependentTSID );
	}
	// AnalysisMethod
	if ( AnalysisMethod != null && AnalysisMethod.length() > 0 ) {
		props.set( "AnalysisMethod", AnalysisMethod );
	}
	// NumberOfEquations
	if ( NumberOfEquations != null && NumberOfEquations.length() > 0 ) {
		props.set( "NumberOfEquations", NumberOfEquations );
	}
	// Transformation
	if ( Transformation != null && Transformation.length() > 0 ) {
		props.set( "Transformation", Transformation );
	}
	// AnalysisStart
	if ( AnalysisStart != null && AnalysisStart.length() > 0 ) {
		props.set( "AnalysisStart", AnalysisStart );
	}
	// AnalysisEnd
	if ( AnalysisEnd != null && AnalysisEnd.length() > 0 ) {
		props.set( "AnalysisEnd", AnalysisEnd );
	}
	// MinimumDataCount
	if ( MinimumDataCount != null && MinimumDataCount.length() > 0 ) {
		props.set( "MinimumDataCount", MinimumDataCount );
	}
	// MinimumR
	if ( MinimumR != null && MinimumR.length() > 0 ) {
		props.set( "MinimumR", MinimumR );
	}
	// BestFitIndicator
	if ( BestFitIndicator != null && BestFitIndicator.length() > 0 ) {
		props.set( "BestFitIndicator", BestFitIndicator );
	}
	// FillStart
	if ( FillStart != null && FillStart.length() > 0 ) {
		props.set( "FillStart", FillStart );
	}
	// FillEnd
	if ( FillEnd != null && FillEnd.length() > 0 ) {
		props.set( "FillEnd", FillEnd );
	}
	// Intercept
	if ( Intercept != null && Intercept.length() > 0 ) {
		props.set( "Intercept", Intercept );
	}
    if ( ConfidenceInterval != null && ConfidenceInterval.length() > 0 ) {
        props.set( "ConfidenceInterval", ConfidenceInterval );
    }
    if ( FillFlag.length() > 0 ) {
        props.set ( "FillFlag", FillFlag );
    }
	// OutputFile
	if ( OutputFile != null && OutputFile.length() > 0 ) {
		props.set( "OutputFile", OutputFile );
	}

	// Check the list of Command Parameters.
    __error_wait = false;
	try {
	    // This will warn the user...
	    __command.checkCommandParameters ( props, null, 1 );
	} catch ( Exception e ) {
		// The warning would have been printed in the check code.
	    Message.printWarning ( 3, routine, "Checking input exception: " + e );
	    Message.printWarning ( 3, routine, e );
		__error_wait = true;
		this.requestFocus();	
	}
}

/**
Commit the edits to the command.  Make sure the command parameters have
already been checked and no errors were detected (check input).
*/
private void commitEdits ()
{
	// Get the values from the interface.
	String DependentTSList = __DependentTSList_JComboBox.getSelected();
	String DependentTSID = __DependentTSID_JComboBox.getSelected();
	String IndependentTSList= __IndependentTSList_JComboBox.getSelected();
	String IndependentTSID = __IndependentTSID_JComboBox.getSelected();
	String AnalysisMethod = getAnalysisMethodFromInterface();
	String Transformation = getTransformationFromInterface();
	String NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String MinimumDataCount = __MinimumDataCount_JTextField.getText().trim();
	String MinimumR = __MinimumR_JTextField.getText().trim();
	String BestFitIndicator = __BestFitIndicator_JComboBox.getSelected();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String Intercept = __Intercept_JTextField.getText().trim();
	String ConfidenceInterval = __ConfidenceInterval_JTextField.getText().trim();
	String FillFlag = __FillFlag_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();

	__command.setCommandParameter ("DependentTSList", DependentTSList);
	__command.setCommandParameter ("DependentTSID", DependentTSID);
	__command.setCommandParameter ("IndependentTSList", IndependentTSList);
	__command.setCommandParameter ("IndependentTSID", IndependentTSID);
	__command.setCommandParameter ("AnalysisMethod", AnalysisMethod);
	__command.setCommandParameter ("Transformation", Transformation);
	__command.setCommandParameter ("NumberOfEquations", NumberOfEquations);
	__command.setCommandParameter ("AnalysisStart", AnalysisStart);
	__command.setCommandParameter ("AnalysisEnd", AnalysisEnd);
	__command.setCommandParameter ("MinimumDataCount", MinimumDataCount);
	__command.setCommandParameter ("MinimumR", MinimumR);
	__command.setCommandParameter ("BestFitIndicator", BestFitIndicator);
	__command.setCommandParameter ("FillStart", FillStart);
	__command.setCommandParameter ("FillEnd", FillEnd);
	__command.setCommandParameter ("Intercept", Intercept);
	__command.setCommandParameter ("ConfidenceInterval", ConfidenceInterval);
	__command.setCommandParameter ("FillFlag", FillFlag );
	__command.setCommandParameter ("OutputFile", OutputFile);
}

// FIXME SAM 2009-08-26 Evaluate whether this is needed.
/**
Create the commands needed to fill the dependent time series using the best fit
among the independent time series.  This method 
*/
private void createFillCommands ()
{
	__fillCommands_Vector = __command.createFillCommands ();
}

/**
Add the vector of FillRegression and FillMOVE2 commands to the TSTool.
*/
private void copyCommandsToTSTool()
{   // For now just copy the single in-memory command.  Make a copy because the TSTool command should reflect
    // the parameters at the time of the copy, not later edits that may occur
    refresh();
    checkInput();
    if ( !__error_wait ) {
        commitEdits(); // Need to commit for parameters to be updated for copy
    }
    Command copy = (Command)__command.clone();
    __commandUI.insertCommand( copy );
}

/**
Fill the dependent time series using the best fit among the independent time series.
*/
private void fillDependents()
{
	__command.fillDependents();
	this.requestFocus();
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	// Dependent time series
	__DependentTSList_JComboBox	= null;
	__DependentTSID_JComboBox	= null;

	// Independent time series
	__IndependentTSList_JComboBox = null;
	__IndependentTSID_JComboBox = null;

	__AnalysisMethod_JCheckBox = null;
	__NumberOfEquations_JComboBox = null;
	__Transformation_JCheckBox = null;

	__AnalysisStart_JTextField = null;
	__AnalysisEnd_JTextField = null;
	__BestFitIndicator_JComboBox = null;
	__FillStart_JTextField = null;
	__FillEnd_JTextField = null;
	__Intercept_JTextField = null;

	__OutputFile_JTextField	 = null;

	// Command Buttons
	__browse_JButton = null;
	__cancel_JButton = null;
	__ok_JButton = null;
	__analyze_JButton = null;
	__viewOutputFile_JButton = null;
	__copyCommandsToTSTool_JButton = null;
	__fillDependents_JButton = null;

	// Member initialized by the createFillCommands() method and used by the
	// the update FillCommandsControl() and copyCommandsToTSTool() methods.
	__fillCommands_Vector = null;

	super.finalize ();
}

/**
Return a comma-delimited string containing the AnalysisMethods, built from the selected items. 
*/
private String getAnalysisMethodFromInterface()
{
	StringBuffer AnalysisMethod = new StringBuffer();

	int size = 0;
	if ( __AnalysisMethod_JCheckBox != null ) {
	    size = __AnalysisMethod_JCheckBox.length;
	}
	int countAdded = 0;
	for ( int i = 0; i < size; i++ ) {
		if ( __AnalysisMethod_JCheckBox[i].isSelected() ) {
	        if ( countAdded > 0 ) {
	            AnalysisMethod.append ( ",");
	        }
		    AnalysisMethod.append ( __AnalysisMethod_JCheckBox[i].getText() );
		    ++countAdded;
		}
	}
	return AnalysisMethod.toString();
}

/**
Get the output file from the interface.
*/
private String getOutputFileFromInterface ( boolean fullPath )
{
    String outputFile = __OutputFile_JTextField.getText().trim();
    if ( fullPath ) {
        String workingDir = TSCommandProcessorUtil.getWorkingDirForCommand ( __processor, __command );
        outputFile = IOUtil.verifyPathForOS(IOUtil.toAbsolutePath(workingDir,
            TSCommandProcessorUtil.expandParameterValue(__processor,__command,outputFile)));
    }
    return outputFile;
}

/**
Return a comma-delimited string containing the Transformation, built from the selected items. 
*/
private String getTransformationFromInterface()
{
    StringBuffer Transformation = new StringBuffer();

    int size = 0;
    if ( __Transformation_JCheckBox != null ) {
        size = __Transformation_JCheckBox.length;
    }
    int countAdded = 0;
    for ( int i = 0; i < size; i++ ) {
        if ( __Transformation_JCheckBox[i].isSelected() ) {
            if ( countAdded > 0 ) {
                Transformation.append ( ",");
            }
            Transformation.append ( __Transformation_JCheckBox[i].getText() );
            ++countAdded;
        }
    }
    return Transformation.toString();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
*/
private void initialize ( JFrame parent )
{   String routine = getClass().getName() + ".initialize";
	if ( __commandUI == null ) {
	    // Have a command to edit
		setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	}
	else {
	    // Running the tool
		setTitle ( "Mixed Station Analysis" );
	}

	addWindowListener( this );

	Insets insetsTLBR = new Insets(2,2,2,2);

	// Panel encompassing the full dialog for all technical content,
	// including analysis, review, transfer to commands.
	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int yMain = 0;
	
	// Top comments
	JPanel mainNotes_JPanel = new JPanel();
	mainNotes_JPanel.setLayout( new GridBagLayout() );
	int yNotes = 0;
    if ( __commandUI == null ) {
        JGUIUtil.addComponent( mainNotes_JPanel, new JLabel (
            "This command determines the best fit to fill the dependent time"
            + " series with data from the independent time series, and performs the filling."),
            0, yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    else {
        JGUIUtil.addComponent( mainNotes_JPanel, new JLabel (
            "This tool determines the best fit to fill the dependent time"
            + " series with data from the independent time series, and generates a command to perform the filling."),
            0, yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }

    JGUIUtil.addComponent(mainNotes_JPanel, new JLabel (
        "The dependent and independent time series can be selected using the TS list parameters."),
        0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(mainNotes_JPanel, new JLabel ( "The working directory for files is: " + __working_dir ),
        0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent( main_JPanel, mainNotes_JPanel,
        0, yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Panel for notes at the top (no border title needed)
	
	// Panel for analysis
    int yAnalysis = 0;
	JPanel mainAnalysis_JPanel = new JPanel();
	mainAnalysis_JPanel.setLayout( new GridBagLayout() );
	if ( __commandUI != null ) {
    	mainAnalysis_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),"Edit Mixed Station Analysis Parameters" ));
	}
    JGUIUtil.addComponent( main_JPanel, mainAnalysis_JPanel,
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __DependentTSList_JComboBox = new SimpleJComboBox(false);
    yAnalysis = CommandEditorUtil.addTSListToEditorDialogPanel ( this, mainAnalysis_JPanel,
        new JLabel("Dependent TS list:"), __DependentTSList_JComboBox, yAnalysis );
    // Remove the EnsembleID from the list...
    __DependentTSList_JComboBox.remove(TSListType.ENSEMBLE_ID.toString());
    
    // The list of time series identifiers depends on whether the tool is being used or the command edited.
    List<String> tsids = null;

    __DependentTSID_JLabel = new JLabel ("Dependent TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __DependentTSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    if ( __commandUI != null ) {
        try {
            tsids = TSUtil.getTimeSeriesIdentifiers((List)__processor.getPropContents("TSResultsList"),false);
        }
        catch ( Exception e ) {
            Message.printStatus(2, routine, "Error getting time series results for Mixed Station Analysis tool (" +
                e + ")." );
        }
    }
    else {
        // Editing a command...
        tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    }
    yAnalysis = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, mainAnalysis_JPanel, __DependentTSID_JLabel,
        __DependentTSID_JComboBox, tsids, yAnalysis );

	// How to get the independent time series.
    __IndependentTSList_JComboBox = new SimpleJComboBox(false);
    yAnalysis = CommandEditorUtil.addTSListToEditorDialogPanel ( this, mainAnalysis_JPanel,
        new JLabel("Independent TS list:"), __IndependentTSList_JComboBox, yAnalysis,
        new JLabel("Optional - time series used to fill (default=" + TSListType.ALL_TS + ")."));
    __IndependentTSList_JComboBox.remove(TSListType.ENSEMBLE_ID.toString());

    __IndependentTSID_JLabel = new JLabel ("Independent TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __IndependentTSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    yAnalysis = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, mainAnalysis_JPanel, __IndependentTSID_JLabel,
        __IndependentTSID_JComboBox, tsids, yAnalysis );
	
	// Best fit indicator
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Best Fit:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __BestFitIndicator_JComboBox = new SimpleJComboBox ( false );
    __BestFitIndicator_JComboBox.addItem ( "" + BestFitIndicatorType.R );
    __BestFitIndicator_JComboBox.addItem ( "" + BestFitIndicatorType.SEP );
    // FIXME SAM 2010-06-10 Does not seem to get computed for monthly and just confusing
    //__BestFitIndicator_JComboBox.addItem ( "" + BestFitIndicatorType.SEP_TOTAL );
    __BestFitIndicator_JComboBox.select ( "" + BestFitIndicatorType.SEP );
    __BestFitIndicator_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(mainAnalysis_JPanel, __BestFitIndicator_JComboBox,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel( "Required - best fit indicator, for ranking output."),
    3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Analysis method
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Analysis method(s):"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	// With only two choices, list horizontally to save vertical space
	JPanel analysisMethodJPanel = new JPanel(new GridLayout(1,2));
	analysisMethodJPanel.setBorder ( new LineBorder(Color.black,1));
	__AnalysisMethod_JCheckBox = new JCheckBox[2];
	__AnalysisMethod_JCheckBox[0] = new JCheckBox("" + RegressionType.MOVE2);
	__AnalysisMethod_JCheckBox[1] = new JCheckBox("" + RegressionType.OLS_REGRESSION);
	for ( int i = 0; i < __AnalysisMethod_JCheckBox.length; i++ ) {
	    analysisMethodJPanel.add( __AnalysisMethod_JCheckBox[i] );
	    __AnalysisMethod_JCheckBox[i].addItemListener ( this );
	}
	JGUIUtil.addComponent( mainAnalysis_JPanel,
		analysisMethodJPanel,
		1, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel("Optional - method(s) to use in analysis (default=" +
        RegressionType.OLS_REGRESSION + ")."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Number of equation (Cyclicity in the original Multiple Station Model)
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Number of equations:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NumberOfEquations_JComboBox = new SimpleJComboBox ( false );
    __NumberOfEquations_JComboBox.addItem ( "" );
    __NumberOfEquations_JComboBox.addItem ( "" + NumberOfEquationsType.ONE_EQUATION );
    __NumberOfEquations_JComboBox.addItem ( "" + NumberOfEquationsType.MONTHLY_EQUATIONS );
    __NumberOfEquations_JComboBox.select ( "" + NumberOfEquationsType.ONE_EQUATION );
    __NumberOfEquations_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(mainAnalysis_JPanel, __NumberOfEquations_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel("Optional - number of equations to use in the analysis (default=" +
        NumberOfEquationsType.ONE_EQUATION + ")."),
       3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Transformation
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Transformation(s):" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	// With only two choices, list horizontally to save vertical space
    JPanel transformationJPanel = new JPanel(new GridLayout(1,2));
    transformationJPanel.setBorder ( new LineBorder(Color.black,1));
    __Transformation_JCheckBox = new JCheckBox[2];
    __Transformation_JCheckBox[0] = new JCheckBox("" + DataTransformationType.LOG);
    __Transformation_JCheckBox[1] = new JCheckBox("" + DataTransformationType.NONE);
    for ( int i = 0; i < __Transformation_JCheckBox.length; i++ ) {
        transformationJPanel.add( __Transformation_JCheckBox[i] );
        __Transformation_JCheckBox[i].addItemListener ( this );
    }
	JGUIUtil.addComponent(mainAnalysis_JPanel, transformationJPanel,
		1, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
       "Optional - transformation(s) to use in analysis (default=" + DataTransformationType.NONE + ")."),
       3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Intercept
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Intercept:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Intercept_JTextField = new JTextField ( 10 );
	__Intercept_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __Intercept_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
		"Optional - 0.0 is allowed with Transformation=None (default=no fixed intercept)."),
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Confidence interval
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Confidence interval:" ),
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ConfidenceInterval_JTextField = new JTextField ( 10 );
    __ConfidenceInterval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(mainAnalysis_JPanel, __ConfidenceInterval_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
        "Optional - confidence interval (%) for line slope (default=do not check interval)."),
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Analysis period
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Analysis period:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AnalysisStart_JTextField = new JTextField ( "", 25 );
	__AnalysisStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __AnalysisStart_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "to" ),
		3, yAnalysis, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__AnalysisEnd_JTextField = new JTextField ( "", 25 );
	__AnalysisEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __AnalysisEnd_JTextField,
		5, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Fill Period
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Fill period:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillStart_JTextField = new JTextField ( "", 25 );
	__FillStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __FillStart_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "to" ),
		3, yAnalysis, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__FillEnd_JTextField = new JTextField ( "", 25 );
	__FillEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __FillEnd_JTextField,
		5, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Minimum Data Count
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Minimum data count:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MinimumDataCount_JTextField = new JTextField ( 10 );
	__MinimumDataCount_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __MinimumDataCount_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
		"Optional - minimum number of overlapping points required for analysis (default=10)."),
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Minimum R
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Minimum R:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MinimumR_JTextField = new JTextField ( 10 );
	__MinimumR_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __MinimumR_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
		"Optional - minimum correlation coefficient R required for a best fit (default = 0.5)."),
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Fill flag:" ), 
        0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillFlag_JTextField = new JTextField ( 5 );
    __FillFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(mainAnalysis_JPanel, __FillFlag_JTextField,
        1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainAnalysis_JPanel,
        new JLabel("Optional - string (or \"Auto\") to indicate filled values (default=no flag)."), 
        3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// File to save results.
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Output file:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __OutputFile_JTextField,
		1, yAnalysis, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
	__browse_JButton.setToolTipText( "Browse to select analysis output file." );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __browse_JButton,
		6, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);

    if ( __commandUI != null ) {
        // Panel to perform analysis and review results
        int yReview = 0;
        JPanel mainReview_JPanel = new JPanel();
        mainReview_JPanel.setLayout( new GridBagLayout() );
        mainReview_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),"Perform Mixed Station Analysis and Review Results" ));
        JGUIUtil.addComponent( main_JPanel, mainReview_JPanel,
            0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
        JPanel analysis_JPanel = new JPanel();
        analysis_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainReview_JPanel, analysis_JPanel,
            0, yReview, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
        // Analyze button: used only when running as a TSTool tool.
        __analyze_JButton = new SimpleJButton(__analyze_String, this);
        __analyze_JButton.setToolTipText( __analyze_Tip );
        analysis_JPanel.add ( __analyze_JButton );
        
         // View button: used only when running as a TSTool tool.
        __viewOutputFile_JButton = new SimpleJButton ( __view_String, this );
        __viewOutputFile_JButton.setToolTipText( __view_Tip );
        __viewOutputFile_JButton.setEnabled( false ); // enabled as soon as a run is made, even if fails
        analysis_JPanel.add ( __viewOutputFile_JButton );

        /* FIXME SAM 2009-08-26 Evaluate use
        // fillDependents button: used only when running as a tool.
        // TODO SAM 2009-06-15 Evaluate what this does
        __fillDependents_JButton = new SimpleJButton(__fillDependents_String, this);
        __fillDependents_JButton.setToolTipText( __fillDependents_Tip );
        __fillDependents_JButton.setEnabled( false );
        buttonAnalyze_JPanel.add ( __fillDependents_JButton );
        */
    }
    
    // Panel for transfer to commands - will have simple command "toString" if command editor
    int yTransfer = 0;
    JPanel mainTransfer_JPanel = new JPanel();
    mainTransfer_JPanel.setLayout( new GridBagLayout() );
    if ( __commandUI != null ) {
        mainTransfer_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),__copyCommandsToTSTool_String ));
    }
    JGUIUtil.addComponent( main_JPanel, mainTransfer_JPanel,
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

    /* FIXME SAM 2009-08-26 See previous comment for the data members
    if ( __commandUI != null ) {
        JPanel buttonTransfer1_JPanel = new JPanel();
        buttonTransfer1_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainTransfer_JPanel, buttonTransfer1_JPanel,
            0, yTransfer, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
        // createFillCommands button: used only when running as a tool.
        __createFillMixedStationCommand_JButton = new SimpleJButton(__createFillMixedStationCommand_String, this);
        __createFillMixedStationCommand_JButton.setToolTipText(__createFillMixedStationCommand_Tip );
        __createFillMixedStationCommand_JButton.setEnabled( false );
        buttonTransfer1_JPanel.add ( __createFillMixedStationCommand_JButton );
    }
    */
    
    // Command - Currently showing only under the command mode
    JGUIUtil.addComponent(mainTransfer_JPanel, new JLabel ( "Fill command(s):" ),
        0, ++yTransfer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Command_JTextArea = new JTextArea (4,55);
    __Command_JTextArea.setLineWrap ( true );
    __Command_JTextArea.setWrapStyleWord ( true );
    __Command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(mainTransfer_JPanel, new JScrollPane( __Command_JTextArea ),
        1, yTransfer, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    if ( __commandUI != null ) {
        JPanel buttonTransfer2_JPanel = new JPanel();
        buttonTransfer2_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainTransfer_JPanel, buttonTransfer2_JPanel,
            0, ++yTransfer, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
        // copyCommandsToTSTool button: used only when running as a tool.
        __copyCommandsToTSTool_JButton = new SimpleJButton(__copyCommandsToTSTool_String, this);
        __copyCommandsToTSTool_JButton.setToolTipText( __copyCommandsToTSTool_Tip );
        buttonTransfer2_JPanel.add ( __copyCommandsToTSTool_JButton );
    }
    
    // Main buttons.
    
    JPanel buttonMain_JPanel = new JPanel();
    buttonMain_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, buttonMain_JPanel,
        0, ++yMain, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    if ( __commandUI == null ) {
        // Cancel button: used when running as a command
        __cancel_JButton = new SimpleJButton( __cancel_String, this);
        __cancel_JButton.setToolTipText( __cancel_Tip );
        buttonMain_JPanel.add ( __cancel_JButton );
        // OK button: used only when running as a TSTool command.
        __ok_JButton = new SimpleJButton(__ok_String, this);
        __ok_JButton.setToolTipText( __ok_Tip );
        buttonMain_JPanel.add ( __ok_JButton );

    }
    else {
        // Close button: used when running as a tool
        __close_JButton = new SimpleJButton( __close_String, this);
        __close_JButton.setToolTipText( __close_Tip );
        buttonMain_JPanel.add ( __close_JButton );
    }

	// Set up the status bar.
	__statusJTextField = new JTextField();
	__statusJTextField.setEditable(false);
	JPanel statusJPanel = new JPanel();
	statusJPanel.setLayout(new GridBagLayout());
	JGUIUtil.addComponent(statusJPanel, __statusJTextField,
		0, 0, 1, 1, 1, 1,
		GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
	getContentPane().add ( "South", statusJPanel);
	
	// Refresh the contents...
    checkGUIState();
    refresh();

	// Visualize it...
	setResizable ( true );
	pack();
	JGUIUtil.center ( this );
	super.setVisible( true );

	__statusJTextField.setText ( " Ready" );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{
    checkGUIState();
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{
	int code = event.getKeyCode();

	if ( (__commandUI == null) && (code == KeyEvent.VK_ENTER) ) {
		// This is the same as the ActionPerformed code for the ok_JButton, command editor only
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
//	else {	// One of the combo boxes...
//		refresh();
//	}
}

/**
Handle mouse released event.
*/
public void keyReleased ( KeyEvent event )
{	
	refresh();
}

/**
*/
public void keyTyped ( KeyEvent event )
{	
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{
    return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh()
{
	String mthd = __command.getCommandName() + "_JDialog.refresh";

	String DependentTSList = "";
	String DependentTSID = "";
	String IndependentTSList = "";
	String IndependentTSID = "";
	String AnalysisMethod = "";
	String NumberOfEquations = "";
	String Transformation = "";
	String AnalysisStart = "";
	String AnalysisEnd = "";
	String MinimumDataCount	= "";
	String MinimumR = "";
	String BestFitIndicator = "";
	String FillStart = "";
	String FillEnd = "";
	String Intercept = "";
	String ConfidenceInterval = "";
	String FillFlag = "";
	String OutputFile = "";

	PropList props = null;
	List<String> v = null;

	if ( __first_time ) {
		__first_time = false;

		// Get the properties from the command
		props = __command.getCommandParameters();
		DependentTSList = props.getValue ( "DependentTSList" );
		DependentTSID = props.getValue ( "DependentTSID" );
		IndependentTSList = props.getValue ( "IndependentTSList");
		IndependentTSID = props.getValue ( "IndependentTSID" );
		AnalysisMethod = props.getValue ( "AnalysisMethod" );
		NumberOfEquations = props.getValue ( "NumberOfEquations");
		Transformation = props.getValue ( "Transformation" );
		AnalysisStart = props.getValue ( "AnalysisStart" );
		AnalysisEnd = props.getValue ( "AnalysisEnd" );
		MinimumDataCount = props.getValue ( "MinimumDataCount" );
		MinimumR = props.getValue ( "MinimumR" );
		BestFitIndicator = props.getValue ( "BestFitIndicator" );
		FillStart = props.getValue ( "FillStart" );
		FillEnd = props.getValue ( "FillEnd" );
		Intercept = props.getValue ( "Intercept" );
		ConfidenceInterval = props.getValue ( "ConfidenceInterval" );
		FillFlag = props.getValue( "FillFlag" );
		OutputFile = props.getValue ( "OutputFile" );

        if ( DependentTSList == null ) {
            // Select default...
            __DependentTSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__DependentTSList_JComboBox, DependentTSList, JGUIUtil.NONE, null, null ) ) {
                __DependentTSList_JComboBox.select ( DependentTSList );
            }
            else {
                Message.printWarning ( 1, mthd, "Existing command references an invalid\nDependentTSList value \"" +
                DependentTSList + "\".  Select a different value or Cancel.");
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __DependentTSID_JComboBox, DependentTSID, JGUIUtil.NONE, null, null ) ) {
            __DependentTSID_JComboBox.select ( DependentTSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (DependentTSID != null) && (DependentTSID.length() > 0) ) {
                __DependentTSID_JComboBox.insertItemAt ( DependentTSID, 1 );
                // Select...
                __DependentTSID_JComboBox.select ( DependentTSID );
            }
            else {
                // Select the blank...
                __DependentTSID_JComboBox.select ( 0 );
            }
        }
        
        if ( IndependentTSList == null ) {
            // Select default...
            __IndependentTSList_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__IndependentTSList_JComboBox, IndependentTSList, JGUIUtil.NONE, null, null ) ) {
                __IndependentTSList_JComboBox.select ( IndependentTSList );
            }
            else {
                Message.printWarning ( 1, mthd, "Existing command references an invalid\nIndependentTSList value \"" +
                IndependentTSList + "\".  Select a different value or Cancel.");
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem( __IndependentTSID_JComboBox, IndependentTSID, JGUIUtil.NONE, null, null ) ) {
            __IndependentTSID_JComboBox.select ( IndependentTSID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (IndependentTSID != null) && (IndependentTSID.length() > 0) ) {
                __IndependentTSID_JComboBox.insertItemAt ( IndependentTSID, 1 );
                // Select...
                __IndependentTSID_JComboBox.select ( IndependentTSID );
            }
            else {
                // Select the blank...
                __IndependentTSID_JComboBox.select ( 0 );
            }
        }
		
	    // Check the GUI state to make sure that components are
        // enabled as expected (mainly enable/disable the TSID).  If
        // disabled, the TSID will not be added as a parameter below.
        checkGUIState();
        /*
        if ( !__IndependentTSID_JComboBox.isEnabled() ) {
            // Not needed because some other method of specifying
            // the time series is being used...
            IndependentTSID = null;
        }*/
        
        if ( BestFitIndicator == null ) {
            // Select default...
            __BestFitIndicator_JComboBox.select ( "" + BestFitIndicatorType.SEP );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __BestFitIndicator_JComboBox, BestFitIndicator, JGUIUtil.NONE, null, null )) {
                __BestFitIndicator_JComboBox.select ( BestFitIndicator);
            }
            else {
                Message.printWarning ( 1, mthd, "Existing command references an invalid\n" +
                "BestFitIndicator value \"" + BestFitIndicator +
                "\".  Select a different value or Cancel.");
                this.requestFocus();
            }
        }

		// Check AnalysisMethod and highlight the one that match the command being edited
        for ( int icb = 0; icb < __AnalysisMethod_JCheckBox.length; icb++ ) {
            __AnalysisMethod_JCheckBox[icb].setSelected(false);
        }
        if ( AnalysisMethod != null ) {
            v = StringUtil.breakStringList ( AnalysisMethod, ",", StringUtil.DELIM_SKIP_BLANKS );
            String analysisMethod = "";
            for ( int i = 0; i < v.size(); i++ ) {
                analysisMethod = v.get(i).trim();
                boolean found = false;
                for ( int icb = 0; icb < __AnalysisMethod_JCheckBox.length; icb++ ) {
                    if ( analysisMethod.equalsIgnoreCase(__AnalysisMethod_JCheckBox[icb].getText())) {
                        __AnalysisMethod_JCheckBox[icb].setSelected(true);
                        found = true;
                        break;
                    }
                }
                if ( !found ){
                    Message.printWarning ( 1, mthd, "Existing command references a non-existent\n"
                        + " AnalysisMethod \"" + AnalysisMethod
                        + "\".  Select a\ndifferent AnalysisMethod or Cancel.");
                    this.requestFocus();
                }
            }
        }

        if ( NumberOfEquations == null ) {
            // Select default...
            __NumberOfEquations_JComboBox.select ( "" + NumberOfEquationsType.ONE_EQUATION );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __NumberOfEquations_JComboBox, NumberOfEquations, JGUIUtil.NONE, null, null )) {
                __NumberOfEquations_JComboBox.select ( NumberOfEquations);
            }
            else {
                Message.printWarning ( 1, mthd, "Existing command references an invalid\n" +
                "NumberOfEquations value \"" + NumberOfEquations +
                "\".  Select a different value or Cancel.");
                this.requestFocus();
            }
        }

		// Check Transformation and highlight the one that match the command being edited
		for ( int icb = 0; icb < __Transformation_JCheckBox.length; icb++ ) {
            __Transformation_JCheckBox[icb].setSelected(false);
        }
		if ( Transformation != null ) {
			v = StringUtil.breakStringList ( Transformation, ",", StringUtil.DELIM_SKIP_BLANKS );
			String transformation = "";
			for ( int i = 0; i < v.size(); i++ ) {
				transformation = v.get(i);
				boolean found = false;
				for ( int icb = 0; icb < __Transformation_JCheckBox.length; icb++ ) {
				    if ( transformation.equalsIgnoreCase(__Transformation_JCheckBox[icb].getText())) {
				        __Transformation_JCheckBox[icb].setSelected(true);
                        found = true;
                        break;
				    }
				}
				if ( !found ){
					Message.printWarning ( 1, mthd, "Existing command references a non-existent\n"
						+ " Transformation \"" + transformation
						+ "\".  Select a\ndifferent Transformation or Cancel.");
					this.requestFocus();
				}
			}
		}

		// Check AnalysisStart and update the text field
		if ( AnalysisStart == null ) {
			__AnalysisStart_JTextField.setText ( "" );
		}
		else {
			__AnalysisStart_JTextField.setText ( AnalysisStart );
		}

		// Check AnalysisEnd and update the text field
		if ( AnalysisEnd == null ) {
			__AnalysisEnd_JTextField.setText ( "" );
		}
		else {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}

		// Check MinimumDataCount and update the text field
		if ( MinimumDataCount == null || MinimumDataCount.equals("") ) {
			__MinimumDataCount_JTextField.setText ( "" );
		}
		else {
			__MinimumDataCount_JTextField.setText(MinimumDataCount);
		}

		// Check MinimumR and update the text field
		if ( MinimumR == null || MinimumR.equals("") ) {
			__MinimumR_JTextField.setText( "" );
		}
		else {
			__MinimumR_JTextField.setText( MinimumR );
		}

		// Check FillStart and and update the text field
		if ( FillStart == null ) {
			__FillStart_JTextField.setText ( "" );
		}
		else {
			__FillStart_JTextField.setText ( FillStart );
		}

		// Check FillEnd and update the text field
		if ( FillEnd == null ) {
			__FillEnd_JTextField.setText ( "" );
		}
		else {
			__FillEnd_JTextField.setText ( FillEnd );
		}

		// Check Intercept and update the text field
		if ( Intercept == null ) {
			__Intercept_JTextField.setText ( "" );
		}
		else {
			__Intercept_JTextField.setText ( Intercept );
		}
		
        if ( ConfidenceInterval == null ) {
            __ConfidenceInterval_JTextField.setText ( "" );
        }
        else {
            __ConfidenceInterval_JTextField.setText ( ConfidenceInterval );
        }
		
	    if ( FillFlag != null ) {
	        __FillFlag_JTextField.setText ( FillFlag );
	    }

		// Check OutputFile and update the text field
		if ( OutputFile == null ) {
			__OutputFile_JTextField.setText ( "" );
		}
		else {
			__OutputFile_JTextField.setText ( OutputFile );
		}
	}

	// Regardless, reset the command from the interface fields...
	DependentTSList = __DependentTSList_JComboBox.getSelected();
	DependentTSID = __DependentTSID_JComboBox.getSelected();
	IndependentTSList = __IndependentTSList_JComboBox.getSelected();
	IndependentTSID = __IndependentTSID_JComboBox.getSelected();
	BestFitIndicator = __BestFitIndicator_JComboBox.getSelected();
	AnalysisMethod = getAnalysisMethodFromInterface();
	Transformation = getTransformationFromInterface();
	NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	MinimumDataCount = __MinimumDataCount_JTextField.getText().trim();
	MinimumR = __MinimumR_JTextField.getText().trim();
	FillStart = __FillStart_JTextField.getText().trim();
	FillEnd = __FillEnd_JTextField.getText().trim();
	Intercept = __Intercept_JTextField.getText().trim();
	ConfidenceInterval = __ConfidenceInterval_JTextField.getText().trim();
    FillFlag = __FillFlag_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();

	// And set the command properties.
	props = new PropList ( __command.getCommandName() );
	props.add ( "DependentTSList=" + DependentTSList );
	props.add ( "DependentTSID=" + DependentTSID );
	props.add ( "IndependentTSList=" + IndependentTSList);
	props.add ( "IndependentTSID=" + IndependentTSID );
	props.add ( "BestFitIndicator=" + BestFitIndicator );
	props.add ( "AnalysisMethod=" + AnalysisMethod );
	props.add ( "Transformation=" + Transformation );
	props.add ( "NumberOfEquations=" + NumberOfEquations);
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "MinimumDataCount=" + MinimumDataCount );
	props.add ( "MinimumR=" + MinimumR );
	props.add ( "FillStart=" + FillStart );
	props.add ( "FillEnd=" + FillEnd );
	props.add ( "Intercept=" + Intercept );
	props.add ( "ConfidenceInterval=" + ConfidenceInterval );
	props.add ( "FillFlag=" + FillFlag );
	props.add ( "OutputFile=" + OutputFile );

	// FIXME SAM 2009-08-26 Evaluate whether FillMixedStation() command should always be used or
	// add a checkbox to allow individual FillRegression(), etc. commands to be used (see createFillCommands() method).
	// Update the __Command_JTextArea if running under the command mode. 
	__Command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{   __ok = ok;  // Save to be returned by ok()
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
*/
public void setStatusText(String text)
{
	__statusJTextField.setText ( text );
	JGUIUtil.forceRepaint(__statusJTextField);
}

/**
Updates the fill the __Command_JTextArea with FillCommands. 
*/
private void updateFillCommandsControl ()
{

	String s, commandList = "";
	int nCommands = __fillCommands_Vector.size();
	for ( int n=0; n<nCommands; n++ ) {
		s = (String) __fillCommands_Vector.get(n) + "\n";
		commandList = commandList + s;
	}
	__Command_JTextArea.setText( commandList );

	// Once fill commands are available, these controls should be made visible.
	__Command_JTextArea.setVisible(true);

	setResizable ( true );
	pack();
	JGUIUtil.center ( this );
}

/**
Handle ListSelectionListener events.
*/
public void valueChanged ( ListSelectionEvent e )
{
	if ( ignoreValueChanged ) {
		return;
	}
	refresh ();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event )
{
	response ( false );
}

public void windowActivated	( WindowEvent evt ){;}
public void windowClosed ( WindowEvent evt ){;}
public void windowDeactivated ( WindowEvent evt ){;}
public void windowDeiconified ( WindowEvent evt ){;}
public void windowIconified	( WindowEvent evt ){;}
public void windowOpened ( WindowEvent evt ){;}

}