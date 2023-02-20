// FillMixedStation_JDialog - editor dialog for FillMixedStation command

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

import java.awt.Color;
import java.awt.FlowLayout;
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
import java.util.ArrayList;
import java.util.List;

import javax.swing.border.LineBorder;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
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
import RTi.TS.TS;
import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSRegression;
import RTi.TS.TSUtil;
import RTi.Util.GUI.GUIUtil;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandListUI;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Math.BestFitIndicatorType;
import RTi.Util.Math.DataTransformationType;
import RTi.Util.Math.NumberOfEquationsType;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

@SuppressWarnings("serial")
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
Since there is no command in tool mode, this can't just be obtained from the com
	 */
	private CommandProcessor __processor = null;

	// Members controlling the execution mode. This class can run as a command or as a tool from the tool menu.
	private JTextArea __Command_JTextArea = null; // Command as JTextArea

	private JTabbedPane __main_JTabbedPane = null;
	
	//labels are used to easily add TSID's to list of selectable ones
	private SimpleJComboBox	__DependentTSList_JComboBox = null;
	private JLabel __DependentTSID_JLabel = null;
	private SimpleJComboBox __DependentTSID_JComboBox = null;

	private SimpleJComboBox	__IndependentTSList_JComboBox = null;
	private JLabel __IndependentTSID_JLabel = null;
	private SimpleJComboBox __IndependentTSID_JComboBox = null;

	private JCheckBox[]	__NumberOfEquations_JCheckBox = null;
	private SimpleJComboBox	__AnalysisMonth_JComboBox = null;
	private JCheckBox[] __Transformation_JCheckBox = null;
	private JTextField __LEZeroLogValue_JTextField = null;
	private JTextField __Intercept_JTextField = null;
	//reimplement if move2 is reimplemented
	//private JCheckBox[]	__AnalysisMethod_JCheckBox  = null;
	private JTextField __AnalysisStart_JTextField = null;
	private JTextField __AnalysisEnd_JTextField = null;

	private SimpleJComboBox __BestFitIndicator_JComboBox = null;
	private JTextField __MinimumDataCount_JTextField;
	private JTextField __MinimumR_JTextField;
	private JTextField __ConfidenceInterval_JTextField = null;

	private SimpleJComboBox __Fill_JComboBox = null;
	private JTextField __FillStart_JTextField = null;
	private JTextField __FillEnd_JTextField = null;
	private JTextField  __FillFlag_JTextField = null;
	private JTextField __FillFlagDesc_JTextField;

	private SimpleJComboBox __TableID_JComboBox = null;
	private JTextField __TableTSIDColumn_JTextField = null;
	private TSFormatSpecifiersJPanel __TableTSIDFormat_JTextField = null;

	private SimpleJButton __cancel_JButton = null;
	private SimpleJButton __close_JButton = null;
	private SimpleJButton __analyze_JButton = null;
	private SimpleJButton __ok_JButton = null;
	private SimpleJButton __help_JButton = null;
	//private SimpleJButton __fillDependents_JButton = null;

	private JTextField __statusJTextField = null;

	// Cancel button: used when running as a TSTool command.

	private String __cancel_String = "Cancel";
	private String __cancel_Tip = "Close the window, whitout returning the command to TSTool.";

	// Close button: used when running as a TSTool tool.

	private String __close_String = "Close";

	// OK button: used only when running as a TSTool command.

	private String __ok_String = "OK";

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
	List<String> __fillCommands_Vector = null;

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
	public FillMixedStation_JDialog ( JFrame parent, FillMixedStation_Command command, List<String> tableIDList )
	{
		// Modal dialog
		super( parent, true );
		__command = command;
		__processor = __command.getCommandProcessor();
		// Initialize the dialog.
		initialize ( parent, tableIDList );
	}

	/**
Constructor when calling as a TSTool tool.  In this case the Dialog is non-modal, although it may still
obscure the main window.
@param parent JFrame class instantiating this class.
@param processor time series processor, needed by the tool to access time series for analyzing
@param ui interface between main UI and other code.
	 */
/*	public FillMixedStation_JDialog ( JFrame parent, TSCommandProcessor processor, CommandListUI ui )
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
		__command = new FillMixedStation_Command(); // to hold parameters
		__command.setCommandProcessor ( processor ); // to get working directory, etc., time series are in main GUI
		__working_dir = processor.getInitialWorkingDir();
		initialize ( parent );
	}*/

	/**
Responds to ActionEvents.
@param event ActionEvent object
	 */
	public void actionPerformed( ActionEvent event )
	{
		String mthd = "FillMixedStation_JDialog.actionPerformed", mssg;
		Object o = event.getSource();
		
		// Cancel button - valid only under the command mode
		if ( o == __cancel_JButton ) {
			response ( false );
		}

		// Close button - valid only under the tool mode
		else if ( o == __close_JButton ) {
			response ( false );
		}
		else if ( o == __help_JButton ) {
			HelpViewer.getInstance().showHelp("command", "FillMixedStation");
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
		//else if ( o == __fillDependents_JButton ) {
		//	fillDependents();
		//}

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
	//String AnalysisMethod = getAnalysisMethodFromInterface();
	String Transformation = getTransformationFromInterface();
	String LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
	String NumberOfEquations = getNumberEquationsFromInterface();
	String AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
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
	String FillFlagDesc = __FillFlagDesc_JTextField.getText().trim();
	String TableID = __TableID_JComboBox.getSelected();
    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();
	
	// Put together the list of parameters to check...
	PropList props = new PropList ( "" );
	// Fill
	if ( __commandUI == null ) {
		//in command mode. Fill only shows up there
		String Fill = __Fill_JComboBox.getSelected();
		if ( Fill != null && Fill.length() > 0 ) {
			props.set( "Fill", Fill );
		}
	}
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
	/*if ( AnalysisMethod != null && AnalysisMethod.length() > 0 ) {
		props.set( "AnalysisMethod", AnalysisMethod );
	}*/
	// NumberOfEquations
	if ( NumberOfEquations != null && NumberOfEquations.length() > 0 ) {
		props.set( "NumberOfEquations", NumberOfEquations );
	}
	// AnalysisMonth
	if ( AnalysisMonth != null && AnalysisMonth.length() > 0 ) {
		props.set( "AnalysisMonth", AnalysisMonth );
	}
	// Transformation
	if ( Transformation != null && Transformation.length() > 0 ) {
		props.set( "Transformation", Transformation );
	}
	// LEZeroLogValue
	if ( LEZeroLogValue != null && LEZeroLogValue.length() > 0 ) {
		props.set( "LEZeroLogValue", LEZeroLogValue );
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
	// ConfidenceInterval
	if ( ConfidenceInterval != null && ConfidenceInterval.length() > 0 ) {
		props.set( "ConfidenceInterval", ConfidenceInterval );
	}
	// FillFlag
	if ( FillFlag.length() > 0 ) {
		props.set ( "FillFlag", FillFlag );
	}
	// FillFlagDesc
	if ( FillFlagDesc != null && FillFlagDesc.length() > 0 ) {
		props.set( "FillFlagDesc", FillFlagDesc );
	}
	// TableID
	if ( TableID.length() > 0 ) {
        props.set ( "TableID", TableID );
    }
    if ( TableTSIDColumn.length() > 0 ) {
        props.set ( "TableTSIDColumn", TableTSIDColumn );
    }
    if ( TableTSIDFormat.length() > 0 ) {
        props.set ( "TableTSIDFormat", TableTSIDFormat );
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
		//String AnalysisMethod = getAnalysisMethodFromInterface();
		String Transformation = getTransformationFromInterface();
		String LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
		String NumberOfEquations = getNumberEquationsFromInterface();
		String AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
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
		String FillFlagDesc = __FillFlagDesc_JTextField.getText().trim();
		String TableID = __TableID_JComboBox.getSelected();
	    String TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
	    String TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();

		__command.setCommandParameter ("DependentTSList", DependentTSList);
		__command.setCommandParameter ("DependentTSID", DependentTSID);
		__command.setCommandParameter ("IndependentTSList", IndependentTSList);
		__command.setCommandParameter ("IndependentTSID", IndependentTSID);
		//__command.setCommandParameter ("AnalysisMethod", AnalysisMethod);
		__command.setCommandParameter ("Transformation", Transformation);
		__command.setCommandParameter ("LEZeroLogValue", LEZeroLogValue);
		__command.setCommandParameter ("NumberOfEquations", NumberOfEquations);
		__command.setCommandParameter ("AnalysisMonth", AnalysisMonth);
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
		__command.setCommandParameter ("FillFlagDesc", FillFlagDesc);
	    __command.setCommandParameter ( "TableID", TableID );
	    __command.setCommandParameter ( "TableTSIDColumn", TableTSIDColumn );
	    __command.setCommandParameter ( "TableTSIDFormat", TableTSIDFormat );
		
		if ( __commandUI == null ) {
			//command mode. Fill only shows up here
			String Fill = __Fill_JComboBox.getSelected();
			__command.setCommandParameter ("Fill", Fill);
		}
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
Free memory for garbage collection.
	 */
/*	protected void finalize ()
	throws Throwable
	{	// Dependent time series
		__DependentTSList_JComboBox	= null;
		__DependentTSID_JComboBox	= null;

		// Independent time series
		__IndependentTSList_JComboBox = null;
		__IndependentTSID_JComboBox = null;

		//__AnalysisMethod_JCheckBox = null;
		__NumberOfEquations_JCheckBox = null;
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
	}*/

	/**
Return a comma-delimited string containing the AnalysisMethods, built from the selected items. 
	 */
	//reimplement if move2 is reimplemented
	/*private String getAnalysisMethodFromInterface()
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
	}*/

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
	 * Return a comma-delimited string containing the number of equations, built from the selected items.
	 * @return a list of the number of equations
	 */
	private String getNumberEquationsFromInterface() {
		StringBuffer NumberOfEquations = new StringBuffer();

		int size = 0;
		if ( __NumberOfEquations_JCheckBox != null ) {
			size = __NumberOfEquations_JCheckBox.length;
		}
		int countAdded = 0;
		for ( int i = 0; i < size; i++ ) {
			if ( __NumberOfEquations_JCheckBox[i].isSelected() ) {
				if ( countAdded > 0 ) {
					NumberOfEquations.append ( ",");
				}
				NumberOfEquations.append ( __NumberOfEquations_JCheckBox[i].getText() );
				++countAdded;
			}
		}
		return NumberOfEquations.toString();
	}

	/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param tableIDChoices The tables that already exist.
	 */
	private void initialize ( JFrame parent, List<String> tableIDChoices )
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
	int yNotes = -1;
	if ( __commandUI == null ) {
		JGUIUtil.addComponent( mainNotes_JPanel, new JLabel (
			"This command implements the \"mixed station\" approach to filling time series using regression relationships."),
			0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
		JGUIUtil.addComponent( mainNotes_JPanel, new JLabel (
			"The command determines the best fit to fill the dependent time"
			+ " series with data from the independent time series, and optionally also fills missing values in the dependent time series."),
			0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	}
	else {
		JGUIUtil.addComponent( mainNotes_JPanel, new JLabel (
				"<html><b>THIS TOOL IS UNDER DEVELOPMENT - USE THE FillMixedStation() COMMAND." +
		"  Contact the developers if you want to help.</b></html>"),
		0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
		JGUIUtil.addComponent( mainNotes_JPanel, new JLabel (
				"This tool determines the best fit to fill the dependent time"
				+ " series with data from the independent time series, and generates a command to perform the filling."),
				0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	}

	JGUIUtil.addComponent(mainNotes_JPanel, new JLabel (
	"The dependent and independent time series can be selected using the TS list parameters."),
	0, ++yNotes, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent( main_JPanel, mainNotes_JPanel,
			0, yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yMain, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	// Tabbed pane for parameters
	
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++yMain, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.LINE_START);

	// Panel for data for analysis
	int yAnalysis = 0;
	JPanel mainAnalysis_JPanel = new JPanel();
	mainAnalysis_JPanel.setLayout( new GridBagLayout() );
	__main_JTabbedPane.addTab ( "Data for Analysis", mainAnalysis_JPanel );

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
			@SuppressWarnings("unchecked")
			List<TS> dataList = (List<TS>)__processor.getPropContents("TSResultsList");
			tsids = TSUtil.getTimeSeriesIdentifiers(dataList,false);
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

	// Analysis method
	//reimplement if move2 is reimplemented
	/*JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Analysis method(s):"),
			0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
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
			1, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START );
	JGUIUtil.addComponent( mainAnalysis_JPanel, new JLabel("Optional - method(s) to use in analysis (default=" +
			RegressionType.OLS_REGRESSION + ")."),
			3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START );*/

	// Number of equation (Cyclicity in the original Multiple Station Model)
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Number of equations:"),
			0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	JPanel equationsJPanel = new JPanel(new GridLayout(1,2));
	equationsJPanel.setBorder( new LineBorder(Color.black,1));
	__NumberOfEquations_JCheckBox = new JCheckBox[2];
	__NumberOfEquations_JCheckBox[0] = new JCheckBox(""+NumberOfEquationsType.MONTHLY_EQUATIONS);
	__NumberOfEquations_JCheckBox[1] = new JCheckBox(""+NumberOfEquationsType.ONE_EQUATION);
	for ( int i = 0; i < __NumberOfEquations_JCheckBox.length; i++) {
		equationsJPanel.add(__NumberOfEquations_JCheckBox[i]);
		__NumberOfEquations_JCheckBox[i].addItemListener(this);
	}
	JGUIUtil.addComponent(mainAnalysis_JPanel, equationsJPanel,
			1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel("Optional - number of equations to use in the analysis (default=" +
			NumberOfEquationsType.ONE_EQUATION + ")."),
			3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	//Analyze which month?
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ("Analysis month: "),
			0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__AnalysisMonth_JComboBox = new SimpleJComboBox ( false );
	__AnalysisMonth_JComboBox.setMaximumRowCount ( 13 );
	List<String> monthChoices = new ArrayList<String>();
	monthChoices.add ( "" );
	for ( int i = 1; i <= 12; i++ ) {
		monthChoices.add ( "" + i );
	}
	__AnalysisMonth_JComboBox.setData(monthChoices);
	__AnalysisMonth_JComboBox.select ( 0 );
	__AnalysisMonth_JComboBox.addActionListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __AnalysisMonth_JComboBox,
			1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel (
	"Optional - use with monthly equations (default=process all months)."),
	3, yAnalysis, 4,1,0,0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	// Transformation
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Transformation(s):" ),
			0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
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
			1, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START );
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
			"Optional - transformation(s) to use in analysis (default=" + DataTransformationType.NONE + ")."),
			3, yAnalysis, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	//Value to replace zero with if log transform
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ("Value to use when log and <=0:" ),
			0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__LEZeroLogValue_JTextField = new JTextField (10);
	__LEZeroLogValue_JTextField.addKeyListener( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __LEZeroLogValue_JTextField,
			1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
			"Optional - value to substitute when original is <=0 and log transform (default="+
			TSRegression.getDefaultLEZeroLogValue() + ")."),
			3, yAnalysis, 5, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	// Intercept
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Intercept:" ),
			0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__Intercept_JTextField = new JTextField ( 10 );
	__Intercept_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __Intercept_JTextField,
			1, yAnalysis, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel(
		"Optional - 0.0 is allowed with Transformation=None (default=no fixed intercept)."),
		3, yAnalysis, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	
	// Analysis period
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Analysis period:" ),
			0, ++yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__AnalysisStart_JTextField = new JTextField ( "", 25 );
	__AnalysisStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __AnalysisStart_JTextField,
			1, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "to" ),
			2, yAnalysis, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__AnalysisEnd_JTextField = new JTextField ( "", 25 );
	__AnalysisEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __AnalysisEnd_JTextField,
			3, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Optional - range of dates to analyze (default=all time)" ),
			4, yAnalysis, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	//valid relationship controls
	int yRelate = -1;
	JPanel mainRelate_JPanel = new JPanel();
	mainRelate_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Criteria for Valid Relationships", mainRelate_JPanel );
    
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel(
			"These parameters specify the best fit criteria and constraints on valid data set for analysis."),
			0, ++yRelate, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel(""),
			0, ++yRelate, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	// Best fit indicator
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel ( "Best Fit:" ),
			0, ++yRelate, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__BestFitIndicator_JComboBox = new SimpleJComboBox ( false );
	List<String> fitChoices = new ArrayList<String>();
	fitChoices.add ( "" + BestFitIndicatorType.R );
	fitChoices.add ( "" + BestFitIndicatorType.SEP );
	// FIXME SAM 2010-06-10 Does not seem to get computed for monthly and just confusing
	//fitChoices.add ( "" + BestFitIndicatorType.SEP_TOTAL );
	__BestFitIndicator_JComboBox.setData(fitChoices);
	__BestFitIndicator_JComboBox.select ( "" + BestFitIndicatorType.SEP );
	__BestFitIndicator_JComboBox.addItemListener ( this );
	JGUIUtil.addComponent(mainRelate_JPanel, __BestFitIndicator_JComboBox,
			1, yRelate, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel( "Required - best fit indicator, for ranking output."),
			3, yRelate, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	// Minimum Data Count
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel ( "Minimum data count:" ),
			0, ++yRelate, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__MinimumDataCount_JTextField = new JTextField ( 10 );
	__MinimumDataCount_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainRelate_JPanel, __MinimumDataCount_JTextField,
			1, yRelate, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel(
	"Optional - minimum number of overlapping points required for analysis (default=10)."),
	3, yRelate, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	// Minimum R
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel ( "Minimum R:" ),
			0, ++yRelate, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__MinimumR_JTextField = new JTextField ( 10 );
	__MinimumR_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainRelate_JPanel, __MinimumR_JTextField,
			1, yRelate, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel(
	"Optional - minimum correlation coefficient R required for a best fit (default = do not check)."),
	3, yRelate, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);

	// Confidence interval
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel ( "Confidence interval:" ),
			0, ++yRelate, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__ConfidenceInterval_JTextField = new JTextField ( 10 );
	__ConfidenceInterval_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainRelate_JPanel, __ConfidenceInterval_JTextField,
			1, yRelate, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainRelate_JPanel, new JLabel(
	"Optional - confidence interval (%) for line slope (default=do not check interval)."),
	3, yRelate, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	//fill controls
	int yFill = -1;
	JPanel mainFill_JPanel = new JPanel();
	mainFill_JPanel.setLayout( new GridBagLayout() );
	__main_JTabbedPane.addTab ( "Control Filling", mainFill_JPanel );
	
	JGUIUtil.addComponent(mainFill_JPanel, new JLabel(
			"These parameters control whether to fill dependent time series after the regression relationships are computed."),
			0, ++yFill, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainFill_JPanel, new JLabel(""),
			0, ++yFill, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	//Fill or not?
	if (__commandUI == null) {
		//only show if using command, not tool
		JGUIUtil.addComponent(mainFill_JPanel, new JLabel ("Fill:"),
				0, ++yFill, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
		__Fill_JComboBox = new SimpleJComboBox ( false );
		List<String> fill2Choices = new ArrayList<String>();
		fill2Choices.add( "" );
		fill2Choices.add( "" + __command._False );
		fill2Choices.add( "" + __command._True );
		__Fill_JComboBox.setData(fill2Choices);
		__Fill_JComboBox.select ( 0 );
		__Fill_JComboBox.setToolTipText("Use False to calculate statistics but do not fill.");
		__Fill_JComboBox.addActionListener( this );
		JGUIUtil.addComponent(mainFill_JPanel, __Fill_JComboBox,
				1, yFill, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
		JGUIUtil.addComponent(mainFill_JPanel, new JLabel(
				"Optional - fill missing values in dependent time series (blank=" + __command._True + ", " +
				__command._False + "=analyze only)."),
				3, yFill, 5, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	}

	// Fill Period
    JGUIUtil.addComponent(mainFill_JPanel,new JLabel( "Fill start date/time:"),
        0, ++yFill, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillStart_JTextField = new JTextField ( "", 10 );
    __FillStart_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(mainFill_JPanel, __FillStart_JTextField,
        1, yFill, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainFill_JPanel, new JLabel(
        "Optional - fill start (default=fill all)."), 
        3, yFill, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(mainFill_JPanel,new JLabel("Fill end date/time:"),
        0, ++yFill, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __FillEnd_JTextField = new JTextField ( "", 10 );
    __FillEnd_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(mainFill_JPanel, __FillEnd_JTextField,
        1, yFill, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainFill_JPanel, new JLabel(
        "Optional - fill end (default=fill all)."), 
        3, yFill, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	//Fill Flag
	JGUIUtil.addComponent(mainFill_JPanel, new JLabel ( "Fill flag:" ), 
			0, ++yFill, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__FillFlag_JTextField = new JTextField ( 5 );
	__FillFlag_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainFill_JPanel, __FillFlag_JTextField,
			1, yFill, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainFill_JPanel,
			new JLabel("Optional - single character (or \"Auto\") to indicate filled values (default=no flag)."), 
			3, yFill, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	//description for fill flag
	JGUIUtil.addComponent(mainFill_JPanel, new JLabel ("Fill flag description:"),
			0, ++yFill, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__FillFlagDesc_JTextField = new JTextField ( 25 );
	__FillFlagDesc_JTextField.addKeyListener( this );
	JGUIUtil.addComponent(mainFill_JPanel, __FillFlagDesc_JTextField,
			1, yFill, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainFill_JPanel, new JLabel ("Optional - description for fill flag used in report legends."),
			3, yFill, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);

	//panel for outputs
	int yOutput = -1;
	JPanel mainOutput_JPanel = new JPanel();
	mainOutput_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Table", mainOutput_JPanel );
    
	JGUIUtil.addComponent(mainOutput_JPanel, new JLabel(
		"These parameters specify how to save the analysis results in a table."),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainOutput_JPanel, new JLabel(""),
		0, ++yOutput, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	
	// Table to save statistics
	JGUIUtil.addComponent(mainOutput_JPanel, new JLabel ( "Table ID for output:" ), 
	        0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__TableID_JComboBox = new SimpleJComboBox ( 12, true );    // Allow edit
	tableIDChoices.add(0,""); // Add blank to ignore table
	__TableID_JComboBox.setData ( tableIDChoices );
	__TableID_JComboBox.addItemListener ( this );
	JGUIUtil.addComponent(mainOutput_JPanel, __TableID_JComboBox,
	    1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainOutput_JPanel, new JLabel(
	    "Optional - specify to output statistics to table."), 
	    3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	    
	//column that represents time series
	JGUIUtil.addComponent(mainOutput_JPanel, new JLabel ( "Table TSID column:" ), 
	    0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__TableTSIDColumn_JTextField = new JTextField ( 10 );
	__TableTSIDColumn_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainOutput_JPanel, __TableTSIDColumn_JTextField,
	    1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainOutput_JPanel, new JLabel( "Required if using table - column name for dependent TSID."), 
	    3, yOutput, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START);
	   
	JGUIUtil.addComponent(mainOutput_JPanel, new JLabel("Format of TSID:"),
	    0, ++yOutput, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
	__TableTSIDFormat_JTextField = new TSFormatSpecifiersJPanel(10);
	__TableTSIDFormat_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
	__TableTSIDFormat_JTextField.addKeyListener ( this );
	__TableTSIDFormat_JTextField.setToolTipText("%L for location, %T for data type.");
	JGUIUtil.addComponent(mainOutput_JPanel, __TableTSIDFormat_JTextField,
	    1, yOutput, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.LINE_START);
	JGUIUtil.addComponent(mainOutput_JPanel, new JLabel ("Optional - use %L for location, etc. (default=alias or TSID)."),
	    3, yOutput, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_START );

	// Command
	if (__commandUI == null) {
		JGUIUtil.addComponent(main_JPanel, new JLabel ( "Fill command(s):" ),
				0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
		__Command_JTextArea = new JTextArea (4,55);
		__Command_JTextArea.setLineWrap ( true );
		__Command_JTextArea.setWrapStyleWord ( true );
		__Command_JTextArea.setEditable ( false );
		JGUIUtil.addComponent(main_JPanel, new JScrollPane( __Command_JTextArea ),
				1, yMain, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.LINE_START);
	}
	else {
		// Panel for transfer to commands - will have simple command "toString" if command editor
		int yTransfer = 0;
		JPanel mainTransfer_JPanel = new JPanel();
		mainTransfer_JPanel.setLayout( new GridBagLayout() );
		if ( __commandUI != null ) {
			mainTransfer_JPanel.setBorder( BorderFactory.createTitledBorder (
					BorderFactory.createLineBorder(Color.black),__copyCommandsToTSTool_String ));
		}
		JGUIUtil.addComponent( main_JPanel, mainTransfer_JPanel,
				0, ++yMain, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.LINE_START);

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
		JGUIUtil.addComponent(mainTransfer_JPanel, new JLabel ( "Fill command(s):" ),
				0, ++yTransfer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.LINE_END);
		__Command_JTextArea = new JTextArea (4,55);
		__Command_JTextArea.setLineWrap ( true );
		__Command_JTextArea.setWrapStyleWord ( true );
		__Command_JTextArea.setEditable ( false );
		JGUIUtil.addComponent(mainTransfer_JPanel, new JScrollPane( __Command_JTextArea ),
				1, yTransfer, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.LINE_START);

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

	if ( __commandUI == null ) {
		JPanel buttonMain_JPanel = new JPanel();
		buttonMain_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JGUIUtil.addComponent(main_JPanel, buttonMain_JPanel,
				0, ++yMain, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

		// OK button: used only when running as a TSTool command.
		__ok_JButton = new SimpleJButton(__ok_String, this);
		__ok_JButton.setToolTipText("Save changes to command");
		buttonMain_JPanel.add ( __ok_JButton );
		// Cancel button: used when running as a command
		__cancel_JButton = new SimpleJButton( __cancel_String, this);
		__cancel_JButton.setToolTipText( __cancel_Tip );
		buttonMain_JPanel.add ( __cancel_JButton );
		__cancel_JButton.setToolTipText("Cancel without saving changes to command");
		buttonMain_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
		__help_JButton.setToolTipText("Show command documentation in web browser");

	}
	else {
		// Panel to perform analysis and review results
		int yReview = 0;
		JPanel mainReview_JPanel = new JPanel();
		mainReview_JPanel.setLayout( new GridBagLayout() );
		mainReview_JPanel.setBorder( BorderFactory.createTitledBorder (
				BorderFactory.createLineBorder(Color.black),"Perform Mixed Station Analysis and Review Results" ));
		JGUIUtil.addComponent( main_JPanel, mainReview_JPanel,
				0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.LINE_START);

		JPanel analysis_JPanel = new JPanel();
		analysis_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
		JGUIUtil.addComponent(mainReview_JPanel, analysis_JPanel,
				0, yReview, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

		// Analyze button: used only when running as a TSTool tool.
		__analyze_JButton = new SimpleJButton(__analyze_String, this);
		__analyze_JButton.setToolTipText( __analyze_Tip );
		analysis_JPanel.add ( __analyze_JButton );

		/* FIXME SAM 2009-08-26 Evaluate use
        // fillDependents button: used only when running as a tool.
        // TODO SAM 2009-06-15 Evaluate what this does
        __fillDependents_JButton = new SimpleJButton(__fillDependents_String, this);
        __fillDependents_JButton.setToolTipText( __fillDependents_Tip );
        __fillDependents_JButton.setEnabled( false );
        buttonAnalyze_JPanel.add ( __fillDependents_JButton );
		 */

		// Close button: used when running as a tool
		__close_JButton = new SimpleJButton( __close_String, this);
		analysis_JPanel.add ( __close_JButton );
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
	pack();
	JGUIUtil.center ( this );
	setResizable ( false );
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
		String AnalysisMonth = "";
		String Transformation = "";
		String LEZeroLogValue = "";
		String Intercept = "";
		String AnalysisStart = "";
		String AnalysisEnd = "";
		String MinimumDataCount	= "";
		String MinimumR = "";
		String BestFitIndicator = "";
		String Fill = "";
		String FillStart = "";
		String FillEnd = "";
		String ConfidenceInterval = "";
		String FillFlag = "";
		String FillFlagDesc = "";
	    String TableID = "";
	    String TableTSIDColumn = "";
	    String TableTSIDFormat = "";

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
			NumberOfEquations = props.getValue ( "NumberOfEquations" );
			AnalysisMonth = props.getValue ( "AnalysisMonth" );
			Transformation = props.getValue ( "Transformation" );
			LEZeroLogValue = props.getValue ( "LEZeroLogValue" );
			AnalysisStart = props.getValue ( "AnalysisStart" );
			AnalysisEnd = props.getValue ( "AnalysisEnd" );
			MinimumDataCount = props.getValue ( "MinimumDataCount" );
			MinimumR = props.getValue ( "MinimumR" );
			BestFitIndicator = props.getValue ( "BestFitIndicator" );
			Fill = props.getValue ( "Fill" );
			FillStart = props.getValue ( "FillStart" );
			FillEnd = props.getValue ( "FillEnd" );
			Intercept = props.getValue ( "Intercept" );
			ConfidenceInterval = props.getValue ( "ConfidenceInterval" );
			FillFlag = props.getValue( "FillFlag" );
			FillFlagDesc = props.getValue ( "FillFlagDesc" );
	        TableID = props.getValue ( "TableID" );
	        TableTSIDColumn = props.getValue ( "TableTSIDColumn" );
	        TableTSIDFormat = props.getValue ( "TableTSIDFormat" );

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
			//reimplement if move2 is reimplemented
			/*for ( int icb = 0; icb < __AnalysisMethod_JCheckBox.length; icb++ ) {
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
			}*/

			//check number of equations
			for ( int icb = 0; icb < __NumberOfEquations_JCheckBox.length; icb++) {
				__NumberOfEquations_JCheckBox[icb].setSelected(false);
			}
			if ( NumberOfEquations != null ) {
				v = StringUtil.breakStringList(NumberOfEquations, ",", StringUtil.DELIM_SKIP_BLANKS);
				String numberOfEquations = "";
				for ( int i = 0; i < v.size(); i++ ) {
					numberOfEquations = v.get(i);
					boolean found = false;
					for ( int icb = 0; icb < __NumberOfEquations_JCheckBox.length; icb++) {
						if ( numberOfEquations.equalsIgnoreCase(__NumberOfEquations_JCheckBox[icb].getText())) {
							__NumberOfEquations_JCheckBox[icb].setSelected(true);
							found = true;
							break;
						}
					}
					if (!found) {
						Message.printWarning(1, mthd, "Existing command references a non-existant\n"
								+ " NumberOfEquations \"" + numberOfEquations
								+ "\". Select a\ndifferent number of equations or cancel." );
						this.requestFocus();
					}
				}
			}

			// Check Analysis Month
			if ( JGUIUtil.isSimpleJComboBoxItem( __AnalysisMonth_JComboBox, AnalysisMonth, JGUIUtil.NONE, null, null ) ) {
				__AnalysisMonth_JComboBox.select ( AnalysisMonth );
			}
			else {
				if ( (AnalysisMonth == null) ||	AnalysisMonth.equals("") ) {
					// New command...select the default...
					__AnalysisMonth_JComboBox.select ( 0 );
				}
				else {
					// Bad user command...
					Message.printWarning ( 1, mthd, "Existing command references an invalid analysis month \"" +
							AnalysisMonth + "\".  Select a different value or Cancel." );
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

			// Check Fill
			if ( __commandUI == null ) {
				if ( JGUIUtil.isSimpleJComboBoxItem( __Fill_JComboBox, Fill, JGUIUtil.NONE, null, null ) ) {
					__Fill_JComboBox.select ( Fill );
				}
				else {
					if ( (Fill == null) || Fill.equals("") ) {
						// Set default...
						__Fill_JComboBox.select ( 0 );
					}
					else {
						Message.printWarning ( 1, mthd, "Existing command references an invalid\n"+
								"Fill \"" + Fill + "\".  Select a different type or Cancel." );
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

			// Check LEZeroLogValue and update the text field
			if ( LEZeroLogValue == null || LEZeroLogValue.equals("") ) {
				__LEZeroLogValue_JTextField.setText( "" );
			}
			else {
				__LEZeroLogValue_JTextField.setText(LEZeroLogValue);
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

			if ( FillFlagDesc != null ) {
				__FillFlagDesc_JTextField.setText ( FillFlagDesc );
			}
			
			//check table stuff
			if ( TableID == null ) {
	            // Select default...
	            __TableID_JComboBox.select ( 0 );
	        }
	        else {
	            if ( JGUIUtil.isSimpleJComboBoxItem( __TableID_JComboBox,TableID, JGUIUtil.NONE, null, null ) ) {
	                __TableID_JComboBox.select ( TableID );
	            }
	            else {
	                // User can specify new table so add to the end of the list
	                __TableID_JComboBox.add(TableID);
	                __TableID_JComboBox.select(TableID);
	            }
	        }
	        if ( TableTSIDColumn != null ) {
	            __TableTSIDColumn_JTextField.setText ( TableTSIDColumn );
	        }
	        if (TableTSIDFormat != null ) {
	            __TableTSIDFormat_JTextField.setText(TableTSIDFormat.trim());
	        }
		}

		// Regardless, reset the command from the interface fields...
		DependentTSList = __DependentTSList_JComboBox.getSelected();
		DependentTSID = __DependentTSID_JComboBox.getSelected();
		IndependentTSList = __IndependentTSList_JComboBox.getSelected();
		IndependentTSID = __IndependentTSID_JComboBox.getSelected();
		BestFitIndicator = __BestFitIndicator_JComboBox.getSelected();
		//AnalysisMethod = getAnalysisMethodFromInterface();
		Transformation = getTransformationFromInterface();
		LEZeroLogValue = __LEZeroLogValue_JTextField.getText().trim();
		NumberOfEquations = getNumberEquationsFromInterface();
		AnalysisMonth = __AnalysisMonth_JComboBox.getSelected();
		AnalysisStart = __AnalysisStart_JTextField.getText().trim();
		AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
		MinimumDataCount = __MinimumDataCount_JTextField.getText().trim();
		MinimumR = __MinimumR_JTextField.getText().trim();
		FillStart = __FillStart_JTextField.getText().trim();
		FillEnd = __FillEnd_JTextField.getText().trim();
		Intercept = __Intercept_JTextField.getText().trim();
		ConfidenceInterval = __ConfidenceInterval_JTextField.getText().trim();
		FillFlag = __FillFlag_JTextField.getText().trim();
		FillFlagDesc = __FillFlagDesc_JTextField.getText().trim();
		TableID = __TableID_JComboBox.getSelected();
		TableTSIDColumn = __TableTSIDColumn_JTextField.getText().trim();
		TableTSIDFormat = __TableTSIDFormat_JTextField.getText().trim();

		// And set the command properties.
		props = new PropList ( __command.getCommandName() );
		props.add ( "DependentTSList=" + DependentTSList );
		props.add ( "DependentTSID=" + DependentTSID );
		props.add ( "IndependentTSList=" + IndependentTSList);
		props.add ( "IndependentTSID=" + IndependentTSID );
		props.add ( "BestFitIndicator=" + BestFitIndicator );
		props.add ( "AnalysisMethod=" + AnalysisMethod );
		props.add ( "Transformation=" + Transformation );
		props.add ( "LEZeroLogValue=" + LEZeroLogValue );
		props.add ( "NumberOfEquations=" + NumberOfEquations);
		props.add ( "AnalysisMonth=" + AnalysisMonth );
		props.add ( "AnalysisStart=" + AnalysisStart );
		props.add ( "AnalysisEnd=" + AnalysisEnd );
		props.add ( "MinimumDataCount=" + MinimumDataCount );
		props.add ( "MinimumR=" + MinimumR );
		props.add ( "FillStart=" + FillStart );
		props.add ( "FillEnd=" + FillEnd );
		props.add ( "Intercept=" + Intercept );
		props.add ( "ConfidenceInterval=" + ConfidenceInterval );
		props.add ( "FillFlag=" + FillFlag );
		props.add ( "FillFlagDesc=" + FillFlagDesc );
		props.add ( "TableID=" + TableID );
	    props.add ( "TableTSIDColumn=" + TableTSIDColumn );
	    props.add ( "TableTSIDFormat=" + TableTSIDFormat );
		
		if ( __commandUI == null ) {
			Fill = __Fill_JComboBox.getSelected();
			props.add ( "Fill=" + Fill );
		}

		// FIXME SAM 2009-08-26 Evaluate whether FillMixedStation() command should always be used or
		// add a checkbox to allow individual FillRegression(), etc. commands to be used (see createFillCommands() method).
		// Update the __Command_JTextArea if running under the command mode. 
		__Command_JTextArea.setText( __command.toString(props).trim() );
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
/*	private void updateFillCommandsControl ()
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
	}*/

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
