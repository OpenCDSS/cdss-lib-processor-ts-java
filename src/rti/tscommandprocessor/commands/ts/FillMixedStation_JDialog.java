// ----------------------------------------------------------------------------
// fillMixedStation_JDialog - editor for fillMixedStation()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// History:
// 2004-04-05	Luiz Teixeira, RTi	Converted from the add_JDialog class to
//					the fill MixedStation_JDialog class.
// 2005-04-11	Luiz Teixeira, RTi	Adding code to support the analysis.
// 2005-04-22	Luiz Teixeira, RTi	Clean up
// 2005-05-26	Luiz Teixeira, RTi	Copied the original class
//					fillMixedStation_JDialog() from TSTool
//					and split the code into the new
//					fillMixedStation_JDialog() and
//					fillMixedStation_Command().
// 2007-02-16	Steven A. Malers, RTi	Update to new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-03-09	SAM, RTi			Work on code so an operational release can
//					be made.
// ----------------------------------------------------------------------------
package rti.tscommandprocessor.commands.ts;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Vector;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.ReportJFrame;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJMenuItem;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJList;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandListUI;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.TS.TS;

public class FillMixedStation_JDialog extends JDialog
implements ActionListener,
	 	   ItemListener,
	 	   KeyListener,
	 	   ListSelectionListener,
		   MouseListener,
		   WindowListener
{

// As currently coded the dependent and independent string must be different,
private String __SELECT_ALL_DEPENDENT = "Dependent - Select all";
private String __DESELECT_ALL_DEPENDENT = "Dependent - Deselect all";
private String __SELECT_ALL_INDEPENDENT = "Independent - Select all";
private String __DESELECT_ALL_INDEPENDENT = "Independent - Deselect all";

// Controls are defined in logical order -- The order they appear in the dialog
// box and documentation.

private FillMixedStation_Command __command = null; // Command object used in command mode
private CommandListUI __commandUI = null; // Used in tool mode to transfer tool commands to processor,
    // recognizing main UI state
private CommandProcessor __processor = null; // Used with tool mode to access time series results
private String __working_dir = null; // Working directory.

// Members controlling the execution mode. This class can run as a command or as a tool from the tool menu.
private JTextArea __Command_JTextArea = null; // Command as JTextArea

private SimpleJComboBox	__DependentTSList_JComboBox = null; // Indicate how to get time series list.
private SimpleJList __DependentTSID_SimpleJList = null;
private JPopupMenu __DependentTS_JPopupMenu = null; // Fields for the dependent time series identifiers

private SimpleJComboBox	__IndependentTSList_JComboBox = null; // Indicate how to get time series list.
private SimpleJList __IndependentTSID_SimpleJList = null;
private JPopupMenu __IndependentTS_JPopupMenu = null; // Field for independent time series identifiers

private SimpleJList	__AnalysisMethod_SimpleJList  = null; // Field for AnalysisMethod

private SimpleJComboBox	__NumberOfEquations_JComboBox = null; // OneEquation, MonthlyEquations

private SimpleJList __Transformation_SimpleJList = null; // Field for Transformation

private JTextField __MinimumDataCount_JTextField; // Field to the MinimumDataCount
private JTextField __MinimumR_JTextField; // Field to the MinimumR
private SimpleJComboBox	__BestFitIndicator_JComboBox = null; // SEP

private JTextField __Intercept_JTextField = null; // Intercept value as JTextField
private JTextField	__AnalysisStart_JTextField = null, // Text fields for dependent time series analysis period.
			__AnalysisEnd_JTextField = null,
			__FillStart_JTextField = null,
			__FillEnd_JTextField = null; // Text fields for fill period.

private JTextField	__OutputFile_JTextField = null; // File to save output						

private SimpleJButton __browse_JButton = null;
private SimpleJButton __view_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __close_JButton = null;
private SimpleJButton __analyze_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __fillDependents_JButton = null;

private String __view_String = "View Results";
private String __view_Tip = "View output containing the analysis results.";

private JTextField	__statusJTextField = null;

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

// createFillCommands button: used only when running as a TSTool tool.
private SimpleJButton __createFillCommands_JButton = null;
private String __createFillCommands_String = "Create FillMixedStation Commands";
private String __createFillCommands_Tip = "Create fill commands using the best fit.";

// copyCommandsToTSTool button: used only when running as a TSTool tool.
private SimpleJButton __copyFillCommandsToTSTool_JButton = null;
private String __copyCommandsToTSTool_String = "Copy Commands to TSTool";
private String __copyCommandsToTSTool_Tip = "Copy fill commands using best fit to TSTool.";

// fill button: used only when running as a TSTool tool.

private String __fillDependents_String = "Fill Dependents";
private String __fillDependents_Tip = "Fill dependents using best fit.";

// Member initialized by the createFillCommands() method and used by the
// the update FillCommandsControl() and copyCommandsToTSTool() methods.
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
Constructor when calling as a TSTool tool.  In this case the Dialog is non-modal.
@param parent JFrame class instantiating this class.
@param processor time series processor, needed by the tool to access time series for analyzing
@param ui interface between main UI and other code.
*/
public FillMixedStation_JDialog ( JFrame parent, TSCommandProcessor processor, CommandListUI ui )
{
	super( parent, false );

	// Initialize the dialog - note that there is no command instance so initialize using other info
	__commandUI = ui;
	__processor = processor;
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
	String s = event.getActionCommand();
	Object o = event.getSource();

	if ( o == __browse_JButton ) {
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

	if ( o == __view_JButton ) {
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
		commitEdits ();
		refresh();
		checkInput();
		if ( !__error_wait ) {
			// TODO LT [2005-06-01] What is the logic of command tag?
			try {
				__command.runCommand( -1 );
			}
			catch ( Exception e ) {
				Message.printWarning ( 2, mthd, e );
				mssg = "Error executing the analysis.  Please check the log file for details.";
				Message.printWarning ( 1, mthd, mssg );	
			}	
			// Unable the runCommand dependent buttons.
			__view_JButton.setEnabled ( true );
			__createFillCommands_JButton.setEnabled ( true );
			__fillDependents_JButton.setEnabled ( true );
		}
	}

	// Create fill Commands button - Active only under the tool mode
	// TODO [LT 2005-06-01] There may be room for improvements here:
	// Either warn the user that the results of the analysis in memory
	// may not reflect the setting in the interface (if the setting are
	// changed after the analysis, or disable all the runCommand
	// dependent buttons if the settings are changed. 
	else if ( o == __createFillCommands_JButton ) {
	//	refresh();
	//	checkInput();
	//	if ( !__error_wait ) {
			createFillCommands();
			updateFillCommandsControl();
			// Unable the fill __copyFillCommandsToTSTool_JButton.
			__copyFillCommandsToTSTool_JButton.setEnabled( true );
	//	}
	}

	// Copy Commands To TSTool button - Active only under the tool mode
	// TODO [LT 2005-06-01] There may be room for improvements here:
	// Either warn the user that the results of the analysis in memory
	// may not reflect the setting in the interface (if the setting are
	// changed after the analysis, or disable all the runCommand
	// dependent buttons if the settings are changed. 
	else if ( o == __copyFillCommandsToTSTool_JButton ) {
		copyCommandsToTSTool();
	}

	// Fill dependents button - Active only under the tool mode
	// TODO [LT 2005-06-01] There may be room for improvements here:
	// Either warn the user that the results of the analysis in memory
	// may not reflect the setting in the interface (if the setting are
	// changed after the analysis, or disable all the runCommand
	// dependent buttons if the settings are changed. 
	else if ( o == __fillDependents_JButton ) {
	//	refresh();
	//	checkInput();
	//	if ( !__error_wait ) {
			fillDependents();
	//	}
	}

	// Select all time series in the dependent time series list
	else if ( s.equals( __SELECT_ALL_DEPENDENT  ) ) {
		// Make sure to set the flag ignoreValueChanged to false and
		// then back to true when executing the selectAll() or select()
		// methods.  These methods cause the ValueChange method to run,
		// which run the refresh() and resetTimeSeriesID_JLists() which
		// would interfere with what we are trying to do here.
		ignoreValueChanged = true;
		__DependentTSID_SimpleJList.selectAll();
		ignoreValueChanged = false;

		int i = __DependentTSID_SimpleJList.indexOf("*");
		__DependentTSID_SimpleJList.deselectRow(i);
		refresh();
	}

	// Select all time series in the independent time series list
	else if ( s.equals( __SELECT_ALL_INDEPENDENT  ) ) {
		// Here we do not want the ValueChange method to run, because
		// it would run resetTimeSeriesID_JLists which would interfere
		// with what we are trying to do here. So set the flag
		// ignoreValueChanged to false and then back to true when
		// executing the selectAll() method.
		ignoreValueChanged = true;
		__IndependentTSID_SimpleJList.selectAll();
		ignoreValueChanged = false;
		int i = __IndependentTSID_SimpleJList.indexOf("*");
		__IndependentTSID_SimpleJList.deselectRow(i);
		refresh();
	}

	// Unselect all time series in the dependent time series list
	else if ( s.equals( __DESELECT_ALL_DEPENDENT ) ) {
		__DependentTSID_SimpleJList.clearSelection();
		refresh();
	}

	// Unselect all time series in the independent time series list
	else if ( s.equals( __DESELECT_ALL_INDEPENDENT ) ) {
		__IndependentTSID_SimpleJList.clearSelection();
		refresh();
	}
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{
	resetTimeSeriesID_JLists();

	// Get the values from the interface.
	String DependentTSList = __DependentTSList_JComboBox.getSelected();
	String DependentTSID = getDependentTSIDFromInterface();
	String IndependentTSList = __IndependentTSList_JComboBox.getSelected();
	String IndependentTSID = getIndependentTSIDFromInterface();
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
	// OutputFile
	if ( OutputFile != null && OutputFile.length() > 0 ) {
		props.set( "OutputFile", OutputFile );
	}

	// Check the list of Command Parameters.
	try {
	    // This will warn the user...
	    if ( __command != null ) {
	        __command.checkCommandParameters ( props, null, 1 );
	    }
	    else {
	        // Create a temporary command and check the parameters.
	        FillMixedStation_Command command = new FillMixedStation_Command();
	        command.checkCommandParameters ( props, null, 1 );
	    }
		__error_wait = false;
	} catch ( Exception e ) {
		// The warning would have been printed in the check code.
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
	String DependentTSID = getDependentTSIDFromInterface();
	String IndependentTSList= __IndependentTSList_JComboBox.getSelected();
	String IndependentTSID = getIndependentTSIDFromInterface();
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
	String OutputFile = __OutputFile_JTextField.getText().trim();

	// Commit the values to the command object.
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
	__command.setCommandParameter ("OutputFile", OutputFile);
}

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
{
    // Loop through the results and pick the highest ranking fill combination.  Then define a corresponding
    // fill command and pass to TSTool.
    // FIXME SAM 2009-06-15 Need to finish enabling
    Command command = null;
    __commandUI.insertCommand( command );
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
	__DependentTSID_SimpleJList	= null;
	__DependentTS_JPopupMenu = null;

	// Independent time series
	__IndependentTSList_JComboBox = null;
	__IndependentTSID_SimpleJList = null;
	__IndependentTS_JPopupMenu	= null;

	__AnalysisMethod_SimpleJList = null;
	__NumberOfEquations_JComboBox = null;
	__Transformation_SimpleJList = null;

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
	__view_JButton = null;
	__copyFillCommandsToTSTool_JButton = null;
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
	String AnalysisMethod = "";

	List analysis = __AnalysisMethod_SimpleJList.getSelectedItems();
	StringBuffer buffer = new StringBuffer();
	for ( int i = 0; i < analysis.size(); i++ ) {
		if ( i > 0 ) buffer.append ( ",");
		buffer.append ( analysis.get( i ) );
	}
	AnalysisMethod = buffer.toString();
	buffer = null;

	return AnalysisMethod;
}

/**
Return a comma-delimited string containing the DependentTSIDs, built from the selected items. 
*/
private String getDependentTSIDFromInterface()
{
	String DependentTSID = "";

	String DependentTSList = __DependentTSList_JComboBox.getSelected();

	if ( DependentTSList.equalsIgnoreCase(FillMixedStation_Command._AllTS) ||
		DependentTSList.equalsIgnoreCase(FillMixedStation_Command._SelectedTS) ) {
		// Don't need...
		DependentTSID = "";
	}
	else if ( DependentTSList.equalsIgnoreCase(FillMixedStation_Command._AllMatchingTSID) ) {
		// Format from the selected identifiers...
		DependentTSID = "";
		if ( JGUIUtil.selectedSize(__DependentTSID_SimpleJList) > 0 ) {
			// Get the selected and format...
			List dependent = __DependentTSID_SimpleJList.getSelectedItems();
			StringBuffer buffer = new StringBuffer();
			for ( int i = 0; i < dependent.size(); i++ ) {
				if ( i > 0 ) buffer.append ( ",");
				buffer.append ( dependent.get( i ) );
			}
			DependentTSID = buffer.toString();
		}
	}
	
	return DependentTSID;
}

/**
Return a comma-delimited string containing the IndependentTSIDs, built from the selected items. 
*/
private String getIndependentTSIDFromInterface()
{
	String IndependentTSID = "";

	String IndependentTSList = __IndependentTSList_JComboBox.getSelected();

	if ( IndependentTSList.equalsIgnoreCase(FillMixedStation_Command._AllTS) ||
		IndependentTSList.equalsIgnoreCase(FillMixedStation_Command._SelectedTS) ) {
		// Don't need...
		IndependentTSID = "";
	}
	else if ( IndependentTSList.equalsIgnoreCase(FillMixedStation_Command._AllMatchingTSID) ) {
		// Format from the selected identifiers...
		IndependentTSID = "";
		if ( JGUIUtil.selectedSize(__IndependentTSID_SimpleJList) > 0 ) {
			// Get the selected and format...
			List independent = __IndependentTSID_SimpleJList.getSelectedItems();
			StringBuffer buffer = new StringBuffer();
			for ( int i = 0; i < independent.size(); i++ ) {
				if ( i > 0 ) buffer.append ( ",");
				buffer.append ( independent.get( i ) );
			}
			IndependentTSID = buffer.toString();
		}
	}
	
	return IndependentTSID;
}

/**
Return a comma-delimited string containing the Transformation, built from the selected items. 
*/
private String getTransformationFromInterface()
{
	String Transformation = "";

	List transformation = __Transformation_SimpleJList.getSelectedItems();
	StringBuffer buffer = new StringBuffer();
	for ( int i = 0; i < transformation.size(); i++ ) {
		if ( i > 0 ) buffer.append ( ",");
		buffer.append ( transformation.get( i ) );
	}
	Transformation = buffer.toString();
	buffer = null;

	return Transformation;
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
*/
private void initialize ( JFrame parent )
{	String mthd = getClass().getName() + ".initialize";

	// GUI Title
	if ( __command != null ) {
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
    if ( __command != null ) {
        JGUIUtil.addComponent( mainNotes_JPanel, new JLabel ( "This command finds the best fit to fill the dependent time"
            + " series with data from the dependent time series, and performs the filling."),
            0, yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    else {
        JGUIUtil.addComponent( mainNotes_JPanel, new JLabel (
            "This tool finds the best fit to fill the dependent time"
            + " series with data from the independent time series, and generates commands to perform the filling."),
            0, yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }

    JGUIUtil.addComponent(mainNotes_JPanel, new JLabel (
        "The dependent and independent time series can be selected using the TS list parameters:"),
        0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(mainNotes_JPanel, new JLabel ( "The working directory is: " + __working_dir ),
        0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent( main_JPanel, mainNotes_JPanel,
        0, yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	// Panel for notes at the top (no border title needed)
	
	// Panel for analysis
    int yAnalysis = 0;
	JPanel mainAnalysis_JPanel = new JPanel();
	mainAnalysis_JPanel.setLayout( new GridBagLayout() );
	if ( __command == null ) {
    	mainAnalysis_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),"Analyze" ));
	}
    JGUIUtil.addComponent( main_JPanel, mainAnalysis_JPanel,
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Create the vector containing the identifiers needed to populate the 
	// dependent and independent time series controls. 
	// TODO [LT 2006-06-01] Allow edits? Maybe in the future, if reimplemented as AnalyzePattern_JDialog
	// LT1 - TODO [LT 2006-06-01] The "" and "* can be added here, if we decide
	// to replace the SimpleJList by JComboBox.
	// (see AnalyzePattern_JDialog)
	List tsids = null;
	if ( __command != null ) {
		tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	}
	else {
	    // Full tool - get the identifiers from the results...
		List tsObjects = null;
		try {
		    Object o = __processor.getPropContents ( "TSResultsList" );
			tsObjects = (List)o;
		}
		catch ( Exception e ) {
			String message = "Cannot get time series list to process (" + e + ").";
			Message.printWarning ( 1, mthd, message );
		}
		// Create a vector containing the ts identifiers.
		if ( tsObjects != null ) {
			int size = tsObjects.size();
			tsids = new Vector( size );
			for ( int i = 0; i < size; i++ ) {
				TS ts = (TS)tsObjects.get(i);
				// Don't need the input type, only the main information
				if ( ts.getAlias().equals("") ) {
				    tsids.add ( ts.getAlias() );
				}
				else {
				    tsids.add ( ts.getIdentifier().toString(false) );
				}
			}
		}
	}

	// List of options for both the dependent and independent TSList
	List tslist_Vector = new Vector();
	if ( __command != null ) {
		tslist_Vector.add ( FillMixedStation_Command._AllTS );
		tslist_Vector.add ( FillMixedStation_Command._SelectedTS );
	}
	tslist_Vector.add ( FillMixedStation_Command._AllMatchingTSID );

	// How to get the dependent time series list to fill.
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ("Dependent TS list:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DependentTSList_JComboBox = new SimpleJComboBox(false);
	__DependentTSList_JComboBox.setData ( tslist_Vector );
	__DependentTSList_JComboBox.addItemListener (this);
	JGUIUtil.addComponent(mainAnalysis_JPanel, __DependentTSList_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel (
		"How to get the dependent time series to fill."),
		3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	// Dependent time series list.
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ("Dependent time series:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);

	List dts = new Vector();
	if ( tsids == null ) {
	    tsids = new Vector();
	}
	int size = tsids.size();
	for ( int i = 0; i < size; i++ ) {
		dts.add( (String) tsids.get(i) );
	}
	dts.add( (String) "*" ); // See LT1 TODO above.

	__DependentTSID_SimpleJList = new SimpleJList (dts);
	__DependentTSID_SimpleJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
	__DependentTSID_SimpleJList.setVisibleRowCount ( 2 );
	// Make sure to set the flag ignoreValueChanged to false and
	// then back to true when executing the select() methods.
	ignoreValueChanged = true;
	__DependentTSID_SimpleJList.select ( 0 );
	ignoreValueChanged = false;
	__DependentTSID_SimpleJList.addListSelectionListener ( this );
	__DependentTSID_SimpleJList.addKeyListener ( this );
	__DependentTSID_SimpleJList.addMouseListener ( this );
	__DependentTSID_SimpleJList.setEnabled(false);
	JGUIUtil.addComponent( mainAnalysis_JPanel,
		new JScrollPane(__DependentTSID_SimpleJList),
		1, yAnalysis, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );

	// How to get the independent time series.
	++yAnalysis;
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ("Independent TS list:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IndependentTSList_JComboBox = new SimpleJComboBox(false);
	__IndependentTSList_JComboBox.setData ( tslist_Vector );
	__IndependentTSList_JComboBox.addItemListener (this);
	JGUIUtil.addComponent(mainAnalysis_JPanel, __IndependentTSList_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel (
		"How to get the independent time series."),
		3, yAnalysis, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	// Independent time series list
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ("Independent time series:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);

	List its = new Vector();
	for ( int i = 0; i < size; i++ ) {
		its.add( (String) tsids.get(i) );
	}
	its.add( (String) "*" ); // See LT1 REVISIT above.

	__IndependentTSID_SimpleJList = new SimpleJList (its);
	__IndependentTSID_SimpleJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
	__IndependentTSID_SimpleJList.setVisibleRowCount ( 2 );
	// Make sure to set the flag ignoreValueChanged to false and
	// then back to true when executing the select() methods.
	ignoreValueChanged = true;
	if ( its.size() == 1 ) {
		__IndependentTSID_SimpleJList.select ( 0 );
	}
	else {
		__IndependentTSID_SimpleJList.select ( 1 );
	}
	ignoreValueChanged = false;
	__IndependentTSID_SimpleJList.addListSelectionListener ( this );
	__IndependentTSID_SimpleJList.addKeyListener ( this );
	__IndependentTSID_SimpleJList.addMouseListener ( this );
	__IndependentTSID_SimpleJList.setEnabled(false);
	JGUIUtil.addComponent(mainAnalysis_JPanel,
		new JScrollPane(__IndependentTSID_SimpleJList),
		1, yAnalysis, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );

	// Analysis method
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Analysis method(s):"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	List av = new Vector();
	av.add( FillMixedStation_Command._ANALYSIS_OLS );
	av.add( FillMixedStation_Command._ANALYSIS_MOVE2 );
	__AnalysisMethod_SimpleJList = new SimpleJList (av);
	__AnalysisMethod_SimpleJList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
	__AnalysisMethod_SimpleJList.setVisibleRowCount ( 2 );
	__AnalysisMethod_SimpleJList.select ( 0 );
	__AnalysisMethod_SimpleJList.addListSelectionListener ( this );
	JGUIUtil.addComponent( mainAnalysis_JPanel,
		new JScrollPane(__AnalysisMethod_SimpleJList),
		1, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel("Optional - methods to use in analysis (default=" +
        FillMixedStation_Command._ANALYSIS_OLS + ")."),
            3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Number of equation (Cyclicity in the original Multiple Station Model
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Number of equations:"),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__NumberOfEquations_JComboBox = new SimpleJComboBox ( false );
	__NumberOfEquations_JComboBox.addItem ( FillMixedStation_Command._NUM_OF_EQUATIONS_ONE_EQUATION );
	__NumberOfEquations_JComboBox.addItem ( FillMixedStation_Command._NUM_OF_EQUATIONS_MONTHLY_EQUATIONS );
	__NumberOfEquations_JComboBox.select ( FillMixedStation_Command._NUM_OF_EQUATIONS_ONE_EQUATION );
	__NumberOfEquations_JComboBox.addItemListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __NumberOfEquations_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel("Optional - number of equations to use in the analysis."),
       3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Transformation
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Transformation(s):" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	List tv = new Vector();
	tv.add( FillMixedStation_Command._TRANSFORMATION_NONE );
	tv.add( FillMixedStation_Command._TRANSFORMATION_LOG );
	__Transformation_SimpleJList = new SimpleJList (tv);
	__Transformation_SimpleJList.setSelectionMode( ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
	__Transformation_SimpleJList.setVisibleRowCount ( 2 );
	__Transformation_SimpleJList.select ( 0 );
	__Transformation_SimpleJList.addListSelectionListener ( this );
	__Transformation_SimpleJList.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JScrollPane(__Transformation_SimpleJList),
		1, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
       "Optional - transformations to use in analysis (default=" + FillMixedStation_Command._TRANSFORMATION_NONE + "."),
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
		"Optional - minimum number of overlapping points required for analysis (default=1)."),
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Minimum R
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Minimum R:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MinimumR_JTextField = new JTextField ( 10 );
	__MinimumR_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __MinimumR_JTextField,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
		"Minimum correlation required for a best fit. (default = 0.5)"),
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Best fit indicator
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Best Fit:" ),
		0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__BestFitIndicator_JComboBox = new SimpleJComboBox ( false );
	__BestFitIndicator_JComboBox.addItem ( FillMixedStation_Command._BEST_FIT_R );
	__BestFitIndicator_JComboBox.addItem ( FillMixedStation_Command._BEST_FIT_SEP );
	__BestFitIndicator_JComboBox.addItem ( FillMixedStation_Command._BEST_FIT_SEPTOTAL );
	__BestFitIndicator_JComboBox.select ( FillMixedStation_Command._BEST_FIT_SEP );
	__BestFitIndicator_JComboBox.addItemListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __BestFitIndicator_JComboBox,
		1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel( "Best fit indicator, for ranking output."),
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
		6, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.EAST);

	// South Panel: North
	JPanel buttonAnalyze_JPanel = new JPanel();
	buttonAnalyze_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	JGUIUtil.addComponent(mainAnalysis_JPanel, buttonAnalyze_JPanel,
		0, ++yAnalysis, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	if ( __command == null ) {
		// Analyze button: used only when running as a TSTool tool.
		__analyze_JButton = new SimpleJButton(__analyze_String, this);
		__analyze_JButton.setToolTipText( __analyze_Tip );
		buttonAnalyze_JPanel.add ( __analyze_JButton );
	}

    if ( __command == null ) {
        // Panel for review of analysis
        int yReview = 0;
        JPanel mainReview_JPanel = new JPanel();
        mainReview_JPanel.setLayout( new GridBagLayout() );
        mainReview_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),"Review Analysis Results" ));
        JGUIUtil.addComponent( main_JPanel, mainReview_JPanel,
            0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
        JPanel buttonReview_JPanel = new JPanel();
        buttonReview_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainReview_JPanel, buttonReview_JPanel,
            0, yReview, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
         // View button: used only when running as a TSTool tool.
        __view_JButton = new SimpleJButton ( __view_String, this );
        __view_JButton.setToolTipText( __view_Tip );
        __view_JButton.setEnabled( false );
        buttonReview_JPanel.add ( __view_JButton );

        // fillDependents button: used only when running as a tool.
        // TODO SAM 2009-06-15 Evaluate what this does
        __fillDependents_JButton = new SimpleJButton(__fillDependents_String, this);
        __fillDependents_JButton.setToolTipText( __fillDependents_Tip );
        __fillDependents_JButton.setEnabled( false );
        buttonReview_JPanel.add ( __fillDependents_JButton );
    }
    
    // Panel for transfer to commands - will have simple command "toString" if command editor
    int yTransfer = 0;
    JPanel mainTransfer_JPanel = new JPanel();
    mainTransfer_JPanel.setLayout( new GridBagLayout() );
    if ( __command == null ) {
        mainTransfer_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),"Transfer Analysis Parameters/Results to Commands" ));
    }
    JGUIUtil.addComponent( main_JPanel, mainTransfer_JPanel,
        0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

    if ( __command == null ) {
        JPanel buttonTransfer1_JPanel = new JPanel();
        buttonTransfer1_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainTransfer_JPanel, buttonTransfer1_JPanel,
            0, yTransfer, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
        // createFillCommands button: used only when running as a tool.
        __createFillCommands_JButton = new SimpleJButton(__createFillCommands_String, this);
        __createFillCommands_JButton.setToolTipText(__createFillCommands_Tip );
        __createFillCommands_JButton.setEnabled( false );
        buttonTransfer1_JPanel.add ( __createFillCommands_JButton );
    }
    
    // Command - Currently showing only under the command mode
    if ( __command != null ) {
        JGUIUtil.addComponent(mainTransfer_JPanel, new JLabel ( "Command:" ),
            0, ++yTransfer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    }
    else {
        JGUIUtil.addComponent(mainTransfer_JPanel, new JLabel ( "Fill Commands:" ),
            0, ++yTransfer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    }
    __Command_JTextArea = new JTextArea (4,55);
    __Command_JTextArea.setLineWrap ( true );
    __Command_JTextArea.setWrapStyleWord ( true );
    __Command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(mainTransfer_JPanel, new JScrollPane( __Command_JTextArea ),
        1, yTransfer, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    if ( __command == null ) {
        JPanel buttonTransfer2_JPanel = new JPanel();
        buttonTransfer2_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainTransfer_JPanel, buttonTransfer2_JPanel,
            0, ++yTransfer, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
        // copyCommandsToTSTool button: used only when running as a tool.
        __copyFillCommandsToTSTool_JButton = new SimpleJButton(__copyCommandsToTSTool_String, this);
        __copyFillCommandsToTSTool_JButton.setToolTipText( __copyCommandsToTSTool_Tip );
        __copyFillCommandsToTSTool_JButton.setEnabled( false );
        buttonTransfer2_JPanel.add ( __copyFillCommandsToTSTool_JButton );
    }
    
    // Main buttons.
    
    JPanel buttonMain_JPanel = new JPanel();
    buttonMain_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, buttonMain_JPanel,
        0, ++yMain, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

    if ( __command != null ) {
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

	// Add the Dependent Pop-up menu to manipulate time series...
	__DependentTS_JPopupMenu = new JPopupMenu( "Dependent TS Actions");
	__DependentTS_JPopupMenu.add( new SimpleJMenuItem ( __SELECT_ALL_DEPENDENT, this) );
	__DependentTS_JPopupMenu.add( new SimpleJMenuItem ( __DESELECT_ALL_DEPENDENT, this) );

	// Add the Independent Pop-up menu to manipulate time series...
	__IndependentTS_JPopupMenu = new JPopupMenu("Independent TS Actions");
	__IndependentTS_JPopupMenu.add( new SimpleJMenuItem (__SELECT_ALL_INDEPENDENT, this));
	__IndependentTS_JPopupMenu.add( new SimpleJMenuItem (__DESELECT_ALL_INDEPENDENT, this));
	
	// Refresh the contents...
    if ( __command != null ) {
        refresh();
    }

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
	if (e.getStateChange() == ItemEvent.SELECTED) {
		resetTimeSeriesList();
	}
	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		// This is the same as the ActionPerformed code for the ok_JButton
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
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{
	return __ok;
}

/**
*/
public void keyTyped ( KeyEvent event )
{	
}

/**
Handle mouse clicked event.
*/
public void mouseClicked ( MouseEvent event )
{
}

/**
Handle mouse entered event.
*/
public void mouseEntered ( MouseEvent event )
{
}

/**
Handle mouse exited event.
*/
public void mouseExited ( MouseEvent event )
{
}

/**
Handle mouse pressed event.
*/
public void mousePressed ( MouseEvent event )
{	
	int mods = event.getModifiers();
	if ( (mods & MouseEvent.BUTTON3_MASK) != 0 ) {

		// Dependent time series
		if (event.getComponent() == __DependentTSID_SimpleJList) {
			// Show this menu only if applicable (_AllMatchingTSID)
			if ( __DependentTSList_JComboBox.getSelected().equalsIgnoreCase(FillMixedStation_Command._AllMatchingTSID)) {
				__DependentTS_JPopupMenu.show ( event.getComponent(), event.getX(), event.getY() );
			}	
		} 
		
		// Independent time series
		else if (event.getComponent()==__IndependentTSID_SimpleJList) {
			// Show this menu only if applicable (_AllMatchingTSID)
			if ( __IndependentTSList_JComboBox.getSelected().equalsIgnoreCase (FillMixedStation_Command._AllMatchingTSID)) {
				__IndependentTS_JPopupMenu.show ( event.getComponent(), event.getX(), event.getY() );
			}
		}
	}
}

/**
Handle mouse released event.
*/
public void mouseReleased ( MouseEvent event )
{
}

/**
Refresh the command from the other text field contents.
*/
private void refresh()
{
    if ( __command == null ) {
        // This does not apply when in tool mode
        return;
    }
	String mthd = __command.getCommandName() + "_JDialog.refresh";

	String DependentTSList = "";  // How to get list of depend.  time series
	String DependentTSID = "";  // Dependent Time series.

	String IndependentTSList = ""; // How to get list of independent time series
	String IndependentTSID = ""; // Independent Time series.

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
	String OutputFile = "";

	__error_wait = false;

	PropList props 	= null;
	List v	= null;

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
		OutputFile = props.getValue ( "OutputFile" );

		// Make sure the DependentTSList option is valid
		if ( DependentTSList == null ) {
			// Select default...
			__DependentTSList_JComboBox.select ( 0 );
		}
		else {
			if ( JGUIUtil.isSimpleJComboBoxItem( __DependentTSList_JComboBox,
				DependentTSList, JGUIUtil.NONE, null, null ) ) {
				__DependentTSList_JComboBox.select ( DependentTSList );
			}
			else {
				Message.printWarning ( 1, mthd, "Existing command references an invalid\nDependentTSList value \""
				+ DependentTSList + "\".  Select a different value or Cancel.");
				this.requestFocus();
				__error_wait = true;
			}
		}

		// Make sure the IndependentTSList option is valid
		if ( IndependentTSList == null ) {
			// Select default ...
			__IndependentTSList_JComboBox.select ( 0 );
		}
		else {
			if ( JGUIUtil.isSimpleJComboBoxItem(__IndependentTSList_JComboBox,
				IndependentTSList, JGUIUtil.NONE, null, null ) ) {
				__IndependentTSList_JComboBox.select ( IndependentTSList );
			}
			else {
				Message.printWarning ( 1, mthd, "Existing command references an invalid\nIndependentTSList value \""
				+ IndependentTSList + "\".  Select a different value or Cancel.");
				this.requestFocus();
				__error_wait = true;
			}
		}

		// Enable or disable the selection of dependent time series
		// according to the settings of DependentTSList parameter.
		// Enable or disable the selection of independent time series
		// according to the settings of IndependentTSList parameter.
		resetTimeSeriesList();

		// Check all the items in the Dependent time series list and
		// highlight the ones that match the command being edited...
		if ( (DependentTSList != null) && DependentTSList.equalsIgnoreCase(FillMixedStation_Command._AllMatchingTSID) &&
			(DependentTSID != null) ) {
			v = StringUtil.breakStringList ( DependentTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
			int size = v.size();
			int pos = 0;
			List selected = new Vector();
			String dependent = "";
			for ( int i = 0; i < size; i++ ) {
				dependent = (String)v.get(i);
				if ( (pos = JGUIUtil.indexOf(__DependentTSID_SimpleJList,dependent, false, true))>= 0 ) {
					// Select it because it is in the command and the list...
					selected.add ( "" + pos );
				}
				else {
					Message.printWarning ( 1, mthd, "Existing command references a non-existent\n"+
					"time series \"" + dependent + "\".  Select a\ndifferent time series or Cancel." );
					this.requestFocus();
					__error_wait = true;
				}
			}

			// Select the matched time series...
			// Make sure to use setSelectedIndices to select multiply rows.
			if ( selected.size() > 0  ) {
				int [] iselected = new int[selected.size()];
				for ( int is = 0; is < iselected.length; is++ ){
					iselected[is] = StringUtil.atoi ( (String)selected.get(is));
				}
				__DependentTSID_SimpleJList.setSelectedIndices(iselected );
			}
		}

		// Check all the items in the Independent time series list and
		// highlight the ones that match the command being edited...
		if ( (IndependentTSList != null) && IndependentTSList.equalsIgnoreCase(FillMixedStation_Command._AllMatchingTSID) &&
			(IndependentTSID != null) ) {
			v = StringUtil.breakStringList (IndependentTSID, ",",StringUtil.DELIM_SKIP_BLANKS );
			int size = v.size();
			int pos = 0;
			List selected = new Vector();
			String independent = "";
			for ( int i = 0; i < size; i++ ) {
				independent = (String)v.get(i);
				if ( (pos = JGUIUtil.indexOf( __IndependentTSID_SimpleJList, independent, false, true))>= 0 ) {
					// Select it because it is in the command and the list...
					selected.add ( "" + pos );
				}
				else {
					Message.printWarning ( 1, mthd, "Existing command references a non-existent\n"+
					"time series \"" + independent + "\".  Select a\ndifferent time series or Cancel." );
					this.requestFocus();
					__error_wait = true;
				}
			}

			// Select the matched time series...
			// Make sure to use setSelectedIndices to select multiply rows.
			if ( selected.size() > 0  ) {
				int [] iselected = new int[selected.size()];
				for ( int is = 0; is < iselected.length; is++ ){
					iselected[is] = StringUtil.atoi ((String)selected.get(is));
				}
				__IndependentTSID_SimpleJList.setSelectedIndices(iselected );
			}
		}

		// Check AnalysisMethod and highlight the one that match the command being edited
		if ( AnalysisMethod != null ) {
			v = StringUtil.breakStringList (AnalysisMethod, ",",StringUtil.DELIM_SKIP_BLANKS );
			int pos = 0;
			List selected = new Vector();
			String analysis = "";
			for ( int i = 0; i < v.size(); i++ ) {
				analysis = (String) v.get(i);
				if ( (pos = JGUIUtil.indexOf(__AnalysisMethod_SimpleJList,analysis, false, true)) >= 0 ) {
					// It in the command and the list...
					selected.add ( "" + pos );
				}
				else {
					Message.printWarning ( 1, mthd, "Existing command references a non-existent\n"
						+ " Analysis Method\"" + analysis
						+ "\".  Select a\ndifferent Analysis Method or Cancel.");
					this.requestFocus();
					__error_wait = true;
				}
			}

			// Select the matched AnalysisMethod...
			// Make sure to use setSelectedIndices to select multiply rows.
			if ( selected.size() > 0  ) {
				int [] iselected = new int[selected.size()];
				for ( int j = 0; j < selected.size(); j++ ){
					iselected[j] = StringUtil.atoi ((String)selected.get(j));
					__AnalysisMethod_SimpleJList.setSelectedIndices(iselected );
				}
			}
		}

		// Check NumberOfEquations and highlight the one that match the command being edited
		if ( NumberOfEquations == null ) {
			// Select default...
			__NumberOfEquations_JComboBox.select ( 0 );
		}
		else {
		    if ( JGUIUtil.isSimpleJComboBoxItem(
				__NumberOfEquations_JComboBox, NumberOfEquations, JGUIUtil.NONE, null, null )) {
				__NumberOfEquations_JComboBox.select (NumberOfEquations);
			}
			else {
			    Message.printWarning ( 1, mthd,
				"Existing command references an invalid\n" +
				"NumberOfEquations value \"" + NumberOfEquations +
				"\".  Select a different value or Cancel.");
				this.requestFocus();
				__error_wait = true;
			}
		}

		// Check Transformation and highlight the one that match the command being edited
		if ( Transformation != null ) {
			v = StringUtil.breakStringList ( Transformation, ",", StringUtil.DELIM_SKIP_BLANKS );
			int pos = 0;
			List selected = new Vector();
			String transformation = "";
			for ( int i = 0; i < v.size(); i++ ) {
				transformation = (String) v.get(i);
				if ( (pos = JGUIUtil.indexOf(__Transformation_SimpleJList,transformation, false, true)) >= 0 ) {
					// It in the command and the list...
					selected.add ( "" + pos );
				} else {
					Message.printWarning ( 1, mthd, "Existing command references a non-existent\n"
						+ " Transformation \"" + transformation
						+ "\".  Select a\ndifferent Transformation or Cancel.");
					this.requestFocus();
					__error_wait = true;
				}
			}

			// Select the matched Transformation...
			// Make sure to use setSelectedIndices to select multiply rows.
			if ( selected.size() > 0  ) {
				int [] iselected = new int[selected.size()];
				for ( int j = 0; j < selected.size(); j++ ){
					iselected[j] = StringUtil.atoi ((String)selected.get(j));
					__Transformation_SimpleJList.setSelectedIndices(iselected );
				}
			}
		}

		// Check AnalysisStart and update the text field
		if ( AnalysisStart == null ) {
			__AnalysisStart_JTextField.setText ( "" );
		} else {
			__AnalysisStart_JTextField.setText ( AnalysisStart );
		}

		// Check AnalysisEnd and update the text field
		if ( AnalysisEnd == null ) {
			__AnalysisEnd_JTextField.setText ( "" );
		} else {
			__AnalysisEnd_JTextField.setText ( AnalysisEnd );
		}

		// Check MinimumDataCount and update the text field
		if ( MinimumDataCount == null || MinimumDataCount.equals("") ) {
			__MinimumDataCount_JTextField.setText ( "" );
		} else {
			__MinimumDataCount_JTextField.setText(MinimumDataCount);
		}

		// Check MinimumR and update the text field
		if ( MinimumR == null || MinimumR.equals("") ) {
			__MinimumR_JTextField.setText( "" );
		} else {
			__MinimumR_JTextField.setText( MinimumR );
		}

		// Check BestFitIndicator
		// highlight the one that match the command being edited
		if ( BestFitIndicator == null ) {
			// Select default...
			__BestFitIndicator_JComboBox.select ( 0 );
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
				__error_wait = true;
			}
		}

		// Check FillStart and and update the text field
		if ( FillStart == null ) {
			__FillStart_JTextField.setText ( "" );
		} else {
			__FillStart_JTextField.setText ( FillStart );
		}

		// Check FillEnd and update the text field
		if ( FillEnd == null ) {
			__FillEnd_JTextField.setText ( "" );
		} else {
			__FillEnd_JTextField.setText ( FillEnd );
		}

		// Check Intercept and update the text field
		if ( Intercept == null ) {
			__Intercept_JTextField.setText ( "" );
		} else {
			__Intercept_JTextField.setText ( Intercept );
		}

		// Check OutputFile and update the text field
		if ( OutputFile == null ) {
			__OutputFile_JTextField.setText ( "" );
		} else {
			__OutputFile_JTextField.setText ( OutputFile );
		}

	} else {
		// Enable or disable the selection of dependent time series
		// according to the settings of DependentTSList parameter.
		// Enable or disable the selection of independent time series
		// according to the settings of IndependentTSList parameter.
		resetTimeSeriesList();
	}

	// Update the __DependentTSID_SimpleJList and
	// __IndependentTSID_SimpleJList to have only the * selected, it the * is selected by the user.
	resetTimeSeriesID_JLists();

	// Regardless, reset the command from the interface fields...
	DependentTSList = __DependentTSList_JComboBox.getSelected();
	DependentTSID = getDependentTSIDFromInterface();
	IndependentTSList = __IndependentTSList_JComboBox.getSelected();
	DependentTSID = getDependentTSIDFromInterface();
	AnalysisMethod = getAnalysisMethodFromInterface();
	Transformation = getTransformationFromInterface();
	NumberOfEquations = __NumberOfEquations_JComboBox.getSelected();
	AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	MinimumDataCount = __MinimumDataCount_JTextField.getText().trim();
	MinimumR = __MinimumR_JTextField.getText().trim();
	BestFitIndicator = __BestFitIndicator_JComboBox.getSelected();
	FillStart = __FillStart_JTextField.getText().trim();
	FillEnd = __FillEnd_JTextField.getText().trim();
	Intercept = __Intercept_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();

	// And set the command properties.
	props = new PropList ( __command.getCommandName() );
	props.add ( "DependentTSList=" + DependentTSList );
	props.add ( "DependentTSID=" + DependentTSID );
	props.add ( "IndependentTSList=" + IndependentTSList);
	props.add ( "DependentTSID=" + DependentTSID );
	props.add ( "AnalysisMethod=" + AnalysisMethod );
	props.add ( "Transformation=" + Transformation );
	props.add ( "NumberOfEquations=" + NumberOfEquations);
	props.add ( "AnalysisStart=" + AnalysisStart );
	props.add ( "AnalysisEnd=" + AnalysisEnd );
	props.add ( "MinimumDataCount=" + MinimumDataCount );
	props.add ( "MinimumR=" + MinimumR );
	props.add ( "BestFitIndicator=" + BestFitIndicator );
	props.add ( "FillStart=" + FillStart );
	props.add ( "FillEnd=" + FillEnd );
	props.add ( "Intercept=" + Intercept );
	props.add ( "OutputFile=" + OutputFile );

	// Update the __Command_JTextArea if running under the command mode. 
	if ( __command != null ) {
		__Command_JTextArea.setText( __command.toString(props) );
	}
}

/**
Update the __DependentTSID_SimpleJList and __IndependentTSID_SimpleJList to
have only the * selected, it the * is selected by the user.
*/
private void resetTimeSeriesID_JLists()
{
	int size;

	List dependent = __DependentTSID_SimpleJList.getSelectedItems();
	size = dependent.size();
	if ( size > 1 ) {
		for ( int i = 0; i < size; i++ ) {
			if ( dependent.get(i).equals("*") ) {
				__DependentTSID_SimpleJList.clearSelection();
				__DependentTSID_SimpleJList.select(i);
				break;
			}
		}
	}

	List independent = __IndependentTSID_SimpleJList.getSelectedItems();
	size = independent.size();
	if ( size > 1 ) {
		for ( int i = 0; i < size; i++ ) {
			if ( independent.get(i).equals("*") ) {
				__IndependentTSID_SimpleJList.clearSelection();
				__IndependentTSID_SimpleJList.select(i);
				break;
			}
		}
	}
}

/**
Enable or disable the selection of dependent time series according to
the settings of DependentTSList parameter.
Enable or disable the selection of independent time series according to
the settings of IndependentTSList parameter.
Enabled only if _AllMatchingTSID.
*/
private void resetTimeSeriesList()
{
	// Dependent time series list
	if ( __DependentTSList_JComboBox.getSelected().equalsIgnoreCase
		( FillMixedStation_Command._AllMatchingTSID ) ) {
		__DependentTSID_SimpleJList.setEnabled( true );
	}
	else {
		__DependentTSID_SimpleJList.setEnabled( false );
	}

	// Independent time series list
	if ( __IndependentTSList_JComboBox.getSelected().equalsIgnoreCase
		( FillMixedStation_Command._AllMatchingTSID ) ) {
		__IndependentTSID_SimpleJList.setEnabled( true );
	}
	else {
		__IndependentTSID_SimpleJList.setEnabled( false );
	}
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
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