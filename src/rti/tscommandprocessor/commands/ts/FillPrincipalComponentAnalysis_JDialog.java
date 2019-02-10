// PrincipalComponentAnalysis_JDialog - editor for PrincipalComponentAnalysis()

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

// ----------------------------------------------------------------------------
// PrincipalComponentAnalysis_JDialog - editor for PrincipalComponentAnalysis()
// ----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// ----------------------------------------------------------------------------
// ----------------------------------------------------------------------------
package rti.tscommandprocessor.commands.ts;

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
import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
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
import RTi.Util.IO.CommandListUI;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.InvalidCommandSyntaxException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.TS.TS;
import java.awt.Color;
import java.awt.GridLayout;
import javax.swing.BoxLayout;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

public class FillPrincipalComponentAnalysis_JDialog extends JDialog
	implements ActionListener,
	 	   ItemListener,
	 	   KeyListener,
	 	   ListSelectionListener,
		   MouseListener,
		   WindowListener
{

// As currently coded the dependent and independent string must be different,
private String __SELECT_ALL_DEPENDENT     = "Dependent - Select all";
private String __DESELECT_ALL_DEPENDENT   = "Dependent - Deselect all";
private String __SELECT_ALL_INDEPENDENT   = "Independent - Select all";
private String __DESELECT_ALL_INDEPENDENT = "Independent - Deselect all";

// Controls are defined in logical order -- The order they appear in the dialog
// box and documentation.

private FillPrincipalComponentAnalysis_Command __command = null; // Command object.
private CommandListUI __commandUI = null;  // Used in tool mode to transfer tool commands to processor
private TSCommandProcessor __processor = null;  // Used with tool mode to access time series results
private String	__working_dir		   = null; // Working directory.

// Members controlling the execution mode. This class can run as a command or
// as a tool from the tool menu.
private JTextArea	__Command_JTextArea   = null; // Command as JTextArea
private JLabel		__Command_JLabel      = null; // JLabel for Command line
private JScrollPane	__Command_JScrollPane = null; // ScrollPane

private SimpleJComboBox	__DependentTSList_JComboBox = null;
						// Indicate how to get time
						// series list.
private SimpleJList	 __DependentTSID_SimpleJList= null;
private JPopupMenu	 __DependentTS_JPopupMenu   = null;
						// Fields for the dependent time
						// series identifiers

private SimpleJComboBox	__IndependentTSList_JComboBox = null;
						// Indicate how to get time
						// series list.
// creating both a SimpleJComboBox and List for the dependent tsid to accommodate
// selection made in __independentTSList_JComboBox
private JLabel      __IndependentTSID_ComboBoxLabel = null;
private SimpleJComboBox __IndependentTSID_SimpleJComboBox = null;
private JLabel      __IndependentTSID_ComboBoxNote = null;
private JLabel      __IndependentTSID_ListLabel = null;
private SimpleJList	 __IndependentTSID_SimpleJList= null;
private JLabel      __IndependentTSID_ListNote = null;
private JPopupMenu	 __IndependentTS_JPopupMenu   = null;
						// Field for independent time
						// series identifiers

private JTextField	__AnalysisStart_JTextField = null,
			__AnalysisEnd_JTextField   = null,
						// Text fields for dependent
						// time series analysis period.
			__FillStart_JTextField = null,
			__FillEnd_JTextField   = null;
						// Text fields for fill period.
private JTextField __MaxCombinations_JTextField = null;
                        // Indicates number of combinations to calculate
private SimpleJComboBox __RegressionEquationFill_SimpleJComboBox = null;
                        // Indicates number of regression equation to use for fill
private JTextField	__PCAOutputFile_JTextField	 = null;
						// File to save PCA output
private JTextField	__FilledTSOutputFile_JTextField	 = null;
						// File to save filled time series output

private SimpleJButton	__browse_JButton = null;
private SimpleJButton	__browseTS_JButton = null;
private SimpleJButton	__view_JButton = null;
private SimpleJButton	__viewTS_JButton = null;
private SimpleJButton	__cancel_JButton = null;
private SimpleJButton	__close_JButton = null;
private SimpleJButton   __analyze_JButton    = null;
private SimpleJButton   __ok_JButton = null;
private SimpleJButton   __fillDependents_JButton = null;

private String 		__view_String  = "View Results";
private String 		__viewTS_String  = "View TS Results";
private String		__view_Tip =
	"View output containing the analysis results.";

private JTextField	__statusJTextField = null;

// Cancel button: used when running as a TSTool command.

private String 		__cancel_String  = "Cancel";
private String		__cancel_Tip =
	"Close the window, whitout returning the command to TSTool.";

// Close button: used when running as a TSTool tool.

private String 		__close_String  = "Close";
private String		__close_Tip =
	"Do not perform the analysis and close the window.";

// OK button: used only when running as a TSTool command.

private String 		__ok_String  = "OK";
private String		__ok_Tip =
	"Close the window, returning the command to TSTool.";

// Analyze button: used only when running as a TSTool tool.

private String		__analyze_String = "Analyze";
private String		__analyze_Tip =
	"Perform the analysis and create the output file";

// createFillCommands button: used only when running as a TSTool tool.
private SimpleJButton   __createFillCommands_JButton = null;
private String		__createFillCommands_String =
	 "Create FillPrincipalComponentAnalysis() commands";
private String		__createFillCommands_Tip =
	"Create fill commands using the best fit.";

// copyCommandsToTSTool button: used only when running as a TSTool tool.
private SimpleJButton   __copyFillCommandsToTSTool_JButton = null;
private String		__copyCommandsToTSTool_String =
	 "Copy commands to TSTool";
private String		__copyCommandsToTSTool_Tip =
	"Copy fill commands using best fit to TSTool.";

// fill button: used only when running as a TSTool tool.

private String		__fillDependents_String =
	"Fill Dependents";
private String		__fillDependents_Tip =
	"Fill dependents using best fit.";

private String      __AddWorkingDirectoryPCAFile_String = "Add Working Directory (PCA File)";
private String      __AddWorkingDirectoryFillTSFile_String = "Add Working Directory (Filled TS File)";
private String      __RemoveWorkingDirectoryPCAFile_String = "Remove Working Directory (PCA File)";
private String      __RemoveWorkingDirectoryFillTSFile_String = "Remove Working Directory (Filled TS File)";
private SimpleJButton __pathPCA_JButton = null,
            __pathFillTS_JButton = null;

// Member initialized by the createFillCommands() method and used by the
// the update FillCommandsControl() and copyCommandsToTSTool() methods.
List __fillCommands_Vector = null;

// Member flag Used to prevent ValueChange method to execute refresh()
boolean ignoreValueChanged = false;

private boolean	__error_wait = false;
private boolean	__first_time = true;
private boolean	__ok         = false;

/**
Constructor when calling as a TSTool command.
In this case the Dialog is modal: super( parent, true ).
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public FillPrincipalComponentAnalysis_JDialog ( JFrame parent, FillPrincipalComponentAnalysis_Command command )
{
	super( parent, true );
    __command = command;
	__processor = (TSCommandProcessor) __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( __processor, __command );
	// Initialize the dialog.
	initialize ( parent );
}

/**
Constructor when calling as a TSTool tool.  In this case the Dialog is non-modal
@param parent JFrame class instantiating this class.
@param processor time series processor, needed by the tool to access time series for analyzing
@param ui interface between main UI and other code.
*/
public FillPrincipalComponentAnalysis_JDialog ( JFrame parent, TSCommandProcessor processor, CommandListUI ui )
{
	super( parent, false );
    
    __commandUI = ui;
    __processor = processor;
    __working_dir = processor.getInitialWorkingDir();
    __command = new FillPrincipalComponentAnalysis_Command(false);
        try {
            __command.initializeCommand(null, __processor, false);
        } catch (InvalidCommandSyntaxException ex) {
            Message.printWarning(1, "FillPrincipalComponentAnalysis_JDialog", "Problems creating FillPrincipalComponentAnalysis_JDialog");
        } catch (InvalidCommandParameterException ex) {
            Message.printWarning(1, "FillPrincipalComponentAnalysis_JDialog", "Problems creating FillPrincipalComponentAnalysis_JDialog");
        }
	// Initialize the dialog.
	initialize ( parent );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{
	String mthd = "fillPrincipalComponentAnalysis_JDialog.actionPerformed", mssg;
	String s = event.getActionCommand();
	Object o = event.getSource();

	if ( o == __browse_JButton || o == __browseTS_JButton) {

		String last_directory_selected =
			JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(
				last_directory_selected );
		}
		else {	fc = JFileChooserFactory.createJFileChooser(
				__working_dir );
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
                if ( o == __browse_JButton) {
                    __PCAOutputFile_JTextField.setText( path );
                    __pathPCA_JButton.setEnabled(true);
                } else {
                    __FilledTSOutputFile_JTextField.setText( path );
                    __pathFillTS_JButton.setEnabled(true);
                }
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}

	if ( o == __view_JButton || o == __viewTS_JButton ) {

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
		String outputFile = o ==__view_JButton? __PCAOutputFile_JTextField.getText() : __FilledTSOutputFile_JTextField.getText();
		reportProp.set("Title = " + outputFile);

		// First add the content of the Output file, if any.
		List strings = new Vector();
		if ( !outputFile.equals("") ) {
			strings = readTextFile ( outputFile );
		}

		// End instantiate the Report viewer.
		new ReportJFrame(strings, reportProp);
		strings = null;
	}

	// Cancel button - valid only under the command mode
	else if ( o == __cancel_JButton ) {
		response ( false );
	}

	// Close button - valid only under the tool mode
	else if ( o == __close_JButton ) {
        // Set command mode to true in case user immediately selects to edit
        // this command by double clicking it in the command list in tstool_JFrame.
        __command.setCommandMode(true);
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
			try {
				__command.runAnalysis( "tool mode" );
			} catch ( Exception e ) {
				Message.printWarning ( 2, mthd, e );
				mssg = "Error executing the analysis."
				 + " Please check the log file for details.";
				Message.printWarning ( 1, mthd, mssg );	
			}	
			// Enable the runCommand dependent buttons.
            __FillEnd_JTextField.setEnabled        ( true );
            __FillStart_JTextField.setEnabled      ( true );
			__view_JButton.setEnabled              ( true );
			__createFillCommands_JButton.setEnabled( true );
			__fillDependents_JButton.setEnabled    ( true );
			__FilledTSOutputFile_JTextField.setEnabled    ( true );
			__browseTS_JButton.setEnabled    ( true );

            // check how many regression equations are available and fill list...
            __RegressionEquationFill_SimpleJComboBox.setEnabled(true);
            __RegressionEquationFill_SimpleJComboBox.removeAll();
            int nEq = __command.getPrincipalComponentAnalysis().getNumberOfAvailableCombinations();
            for ( int i=1; i<=nEq; i++ )
                    __RegressionEquationFill_SimpleJComboBox.add(""+i);
            if ( nEq > 0 )
                __RegressionEquationFill_SimpleJComboBox.select( 0 );
		}

	}

	// Create fill Commands button - Active only under the tool mode
	// REVISIT [LT 2005-06-01] There may be room for improvements here:
	// Either warn the user that the results of the analysis in memory
	// may not reflect the setting in the interface (if the setting are
	// changed after the analysis, or disable all the runCommand
	// dependent buttons if the settings are changed. 
	else if ( o == __createFillCommands_JButton ) {
        commitEdits();
	  	refresh();
	  	checkInput();
	  	if ( !__error_wait ) {
			createFillCommands();
			updateFillCommandsControl();
			// Unable the fill __copyFillCommandsToTSTool_JButton.
			__copyFillCommandsToTSTool_JButton.setEnabled( true );
	  	}

	}

	// Copy Commands To TSTool button - Active only under the tool mode
	// REVISIT [LT 2005-06-01] There may be room for improvements here:
	// Either warn the user that the results of the analysis in memory
	// may not reflect the setting in the interface (if the setting are
	// changed after the analysis, or disable all the runCommand
	// dependent buttons if the settings are changed. 
	else if ( o == __copyFillCommandsToTSTool_JButton ) {
        commitEdits();
	  	refresh();
	  	checkInput();
	  	if ( !__error_wait ) {
			copyCommandsToTSTool();
	  	}
	} else if ( o == __pathPCA_JButton ) {
        if ( __pathPCA_JButton.getText().equals(__AddWorkingDirectoryPCAFile_String) ) {
			__PCAOutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__PCAOutputFile_JTextField.getText() ) );
            __pathPCA_JButton.setText(__RemoveWorkingDirectoryPCAFile_String);
		}
		else if ( __pathPCA_JButton.getText().equals(__RemoveWorkingDirectoryPCAFile_String) ) {
			try {
			    __PCAOutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __PCAOutputFile_JTextField.getText() ) );
                __pathPCA_JButton.setText(__AddWorkingDirectoryPCAFile_String);
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"compareFiles_JDialog",
				"Error converting first file name to relative path." );
			}
		}
        commitEdits();
		refresh ();
	} else if ( o == __pathFillTS_JButton ) {
        if ( __pathFillTS_JButton.getText().equals(__AddWorkingDirectoryFillTSFile_String) ) {
			__FilledTSOutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir,__FilledTSOutputFile_JTextField.getText() ) );
            __pathFillTS_JButton.setText(__RemoveWorkingDirectoryFillTSFile_String);
		}
		else if ( __pathFillTS_JButton.getText().equals(__RemoveWorkingDirectoryFillTSFile_String) ) {
			try {
			    __FilledTSOutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __FilledTSOutputFile_JTextField.getText() ) );
                __pathFillTS_JButton.setText(__AddWorkingDirectoryFillTSFile_String);
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"compareFiles_JDialog",
				"Error converting first file name to relative path." );
			}
		}
        commitEdits();
		refresh ();
    }

	// Fill dependents button - Active only under the tool mode
	// REVISIT [LT 2005-06-01] There may be room for improvements here:
	// Either warn the user that the results of the analysis in memory
	// may not reflect the setting in the interface (if the setting are
	// changed after the analysis, or disable all the runCommand
	// dependent buttons if the settings are changed. 
	else if ( o == __fillDependents_JButton ) {
        commitEdits ();
	 	refresh();
	 	checkInput();
	 	if ( !__error_wait ) {
			fillDependents();
	 	}
        __viewTS_JButton.setEnabled( true );
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
	else if ( s.equals( __DESELECT_ALL_DEPENDENT  ) ) {
		__DependentTSID_SimpleJList.clearSelection();
		refresh();
	}

	// Unselect all time series in the independent time series list
	else if ( s.equals( __DESELECT_ALL_INDEPENDENT  ) ) {
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
	String DependentTSList  = __DependentTSList_JComboBox.getSelected();
	String DependentTSID    = getDependentTSIDFromInterface();
	String IndependentTSList= __IndependentTSList_JComboBox.getSelected();
	String IndependentTSID  = getIndependentTSIDFromInterface();
	String AnalysisStart    = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd      = __AnalysisEnd_JTextField.getText().trim();
	String FillStart        = __FillStart_JTextField.getText().trim();
	String FillEnd          = __FillEnd_JTextField.getText().trim();
	String MaxCombinations  = __MaxCombinations_JTextField.getText().trim();
    String RegressionEquationFill = __RegressionEquationFill_SimpleJComboBox.getSelected();
	String PCAOutputFile       = __PCAOutputFile_JTextField.getText().trim();
    String FilledTSOutputFile = __FilledTSOutputFile_JTextField.getText().trim();

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
	// AnalysisStart
	if ( AnalysisStart != null && AnalysisStart.length() > 0 ) {
		props.set( "AnalysisStart", AnalysisStart );
	}
	// AnalysisEnd
	if ( AnalysisEnd != null && AnalysisEnd.length() > 0 ) {
		props.set( "AnalysisEnd", AnalysisEnd );
	}
	// FillStart
	if ( FillStart != null && FillStart.length() > 0 ) {
		props.set( "FillStart", FillStart );
	}
	// FillEnd
	if ( FillEnd != null && FillEnd.length() > 0 ) {
		props.set( "FillEnd", FillEnd );
	}
	// MaxCombinations
	if ( MaxCombinations != null && MaxCombinations.length() > 0 ) {
		props.set( "MaxCombinations", MaxCombinations );
	}
	// RegressionEquationFill
	if ( RegressionEquationFill != null && RegressionEquationFill.length() > 0 ) {
		props.set( "RegressionEquationFill", RegressionEquationFill );
	}
	// PCAOutputFile
	if ( PCAOutputFile != null && PCAOutputFile.length() > 0 ) {
		props.set( "PCAOutputFile", PCAOutputFile );
	}
	// FilledTSOutputFile
	if ( FilledTSOutputFile != null && FilledTSOutputFile.length() > 0 ) {
		props.set( "FilledTSOutputFile", FilledTSOutputFile );
	}

	// Check the list of Command Parameters.
	try {	// This will warn the user...
        __command.checkCommandParameters ( props, null, 1 );
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
	String AnalysisStart = __AnalysisStart_JTextField.getText().trim();
	String AnalysisEnd = __AnalysisEnd_JTextField.getText().trim();
	String FillStart = __FillStart_JTextField.getText().trim();
	String FillEnd = __FillEnd_JTextField.getText().trim();
	String MaxCombinations = __MaxCombinations_JTextField.getText().trim();
    String RegressionEquationFill = __RegressionEquationFill_SimpleJComboBox.getSelected();
	String PCAOutputFile = __PCAOutputFile_JTextField.getText().trim();
	String FilledTSOutputFile = __FilledTSOutputFile_JTextField.getText().trim();

	// Commit the values to the command object.
	__command.setCommandParameter ("DependentTSList"  , DependentTSList  );
	__command.setCommandParameter ("DependentTSID"    , DependentTSID    );
	__command.setCommandParameter ("IndependentTSList", IndependentTSList);
	__command.setCommandParameter ("IndependentTSID"  , IndependentTSID  );
	__command.setCommandParameter ("AnalysisStart"    , AnalysisStart    );
	__command.setCommandParameter ("AnalysisEnd"      , AnalysisEnd      );
	__command.setCommandParameter ("FillStart"        , FillStart 	     );
	__command.setCommandParameter ("FillEnd"          , FillEnd          );
	__command.setCommandParameter ("MaxCombinations"  , MaxCombinations  );
	__command.setCommandParameter ("RegressionEquationFill"  , RegressionEquationFill  );
	__command.setCommandParameter ("PCAOutputFile"    , PCAOutputFile       );
	__command.setCommandParameter ("FilledTSOutputFile"      , FilledTSOutputFile       );
}

/**
Add the vector of FillRegression and FillMOVE1 commands to the TSTool.
*/
private void copyCommandsToTSTool()
{
    __commandUI.insertCommand( __command );
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
Fill the dependent time series using the best fit among the independent
time series.
*/
private void fillDependents()
{
    try {
        __command.fillDependents();
    } catch (InvalidCommandParameterException ex) {

    }
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

	__AnalysisStart_JTextField = null;
	__AnalysisEnd_JTextField = null;
	__FillStart_JTextField = null;
	__FillEnd_JTextField = null;

	__MaxCombinations_JTextField = null;
    __RegressionEquationFill_SimpleJComboBox = null;

	__PCAOutputFile_JTextField	 = null;
	__FilledTSOutputFile_JTextField	 = null;

	// Command Buttons
	__browse_JButton = null;
    __browseTS_JButton = null;
	__cancel_JButton = null;
	__ok_JButton = null;
	__analyze_JButton = null;
	__view_JButton = null;
	__viewTS_JButton = null;
	__copyFillCommandsToTSTool_JButton = null;
	__fillDependents_JButton = null;

	// Member initialized by the createFillCommands() method and used by the
	// the update FillCommandsControl() and copyCommandsToTSTool() methods.
	__fillCommands_Vector = null;

	super.finalize ();
}

/**
Return a comma-delimited string containing the DependentTSIDs, built from the
selected items. 
*/
private String getDependentTSIDFromInterface()
{
	String DependentTSID = "";

	String DependentTSList = __DependentTSList_JComboBox.getSelected();

	if ( DependentTSList.equalsIgnoreCase(TSListType.ALL_TS.toString()) ||
		DependentTSList.equalsIgnoreCase(TSListType.SELECTED_TS.toString()) ) {
		// Don't need...
		DependentTSID = "";
	}
	else if ( DependentTSList.equalsIgnoreCase( TSListType.ALL_MATCHING_TSID.toString()) ||
            DependentTSList.equalsIgnoreCase( TSListType.FIRST_MATCHING_TSID.toString()) ||
            DependentTSList.equalsIgnoreCase( TSListType.LAST_MATCHING_TSID.toString()) ||
            DependentTSList.equalsIgnoreCase( TSListType.SPECIFIED_TSID.toString())
            ) {
		// Format from the selected identifiers...
		DependentTSID = "";
		if ( JGUIUtil.selectedSize(__DependentTSID_SimpleJList) > 0 ) {
			// Get the selected and format...
			List dependent =
				__DependentTSID_SimpleJList.getSelectedItems();
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
Return a comma-delimited string containing the IndependentTSIDs, built from the
selected items. 
*/
private String getIndependentTSIDFromInterface()
{
	String IndependentTSID = "";

	String IndependentTSList = __IndependentTSList_JComboBox.getSelected();

	if (	IndependentTSList.equalsIgnoreCase(TSListType.ALL_TS.toString()) ||
		IndependentTSList.equalsIgnoreCase(TSListType.SELECTED_TS.toString()) ) {
		// Don't need...
		IndependentTSID = "";
	}
	else if (IndependentTSList.equalsIgnoreCase( TSListType.SPECIFIED_TSID.toString())) {
		// Format from the selected identifiers...
		IndependentTSID = "";
		if ( JGUIUtil.selectedSize(__IndependentTSID_SimpleJList) > 0 ) {
			// Get the selected and format...
			List independent =
				__IndependentTSID_SimpleJList.getSelectedItems();
			StringBuffer buffer = new StringBuffer();
			for ( int i = 0; i < independent.size(); i++ ) {
				if ( i > 0 ) buffer.append ( ",");
				buffer.append ( independent.get( i ) );
			}
			IndependentTSID = buffer.toString();
		}
	}
    else if ( IndependentTSList.equalsIgnoreCase(TSListType.ALL_MATCHING_TSID.toString()) ||
            IndependentTSList.equalsIgnoreCase( TSListType.FIRST_MATCHING_TSID.toString()) ||
            IndependentTSList.equalsIgnoreCase( TSListType.LAST_MATCHING_TSID.toString())) {
        IndependentTSID = __IndependentTSID_SimpleJComboBox.getSelected();
    }
	
	return IndependentTSID;
}


/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Vector of String containing the command.
*/
private void initialize ( JFrame parent )
{	String mthd = "fillPrincipalComponentAnalysis_JDialog.initialize", mssg;

	// GUI Title
	if ( __command.isCommandMode() ) {
		setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	} else {
		setTitle ( "Principal Component Analysis" );
	}
	
	addWindowListener( this );

	Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "Center", main_JPanel );
	int y = -1, yMain=0;

	// Top comments
    JPanel mainNotes_JPanel = new JPanel();
    mainNotes_JPanel.setLayout ( new GridBagLayout());
	if ( __command.isCommandMode() ) {
		JGUIUtil.addComponent( mainNotes_JPanel,
			new JLabel ( "This command finds"
				+ " the best fit to fill the dependent time"
				+ " series with data from the dependent time series."),
				0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	else {
        JGUIUtil.addComponent( mainNotes_JPanel, new JLabel (
            "<html><b>THIS TOOL IS UNDER DEVELOPMENT." +
            "  Contact the developers if you want to help.</b></html>"),
            0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
		JGUIUtil.addComponent( mainNotes_JPanel,
			new JLabel (
			    "This tool finds the best fit to fill the dependent time"
				+ " series with data from the independent time series."),
				0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

	   /*JGUIUtil.addComponent(mainNotes_JPanel, new JLabel (
    "The dependent and independent time series " +
    "can be selected using the TS list parameters:"),
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainNotes_JPanel, new JLabel ( "     "
    + TSListType.ALL_MATCHING_TSID.toString()
    + " - time series that have identifiers matching the given TSID parameter "
    + "(* will analyze all listed time series)."),
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainNotes_JPanel, new JLabel ( "     "
    + TSListType.ALL_TS.toString()
    + " - all time series."),
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainNotes_JPanel, new JLabel ( "     "
    + TSListType.FIRST_MATCHING_TSID.toString()
    + " - only the first time series matching the given TSID parameter."),
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainNotes_JPanel, new JLabel ( "     "
    + TSListType.LAST_MATCHING_TSID.toString()
    + " - only the last time series matching the given TSID parameter."),
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainNotes_JPanel, new JLabel ( "     "
    + TSListType.SELECTED_TS.toString()
    + " - time series selected with selectTimeSeries() commands."),
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainNotes_JPanel, new JLabel ( "     "
    + TSListType.SPECIFIED_TSID.toString()
    + " - time series selected from the list."),
    0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);*/
	JGUIUtil.addComponent(mainNotes_JPanel, new JLabel (
		"Right-click on the time series area to select or deselect all."
		+ "  Active only if the TS list selection is \"SpecifiedTSID\""),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
		JGUIUtil.addComponent(mainNotes_JPanel, new JLabel (
		"The working directory is: " + __working_dir ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent( main_JPanel, mainNotes_JPanel,
    0, yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for analysis
    y=0;
    JPanel mainAnalysis_JPanel = new JPanel();
    mainAnalysis_JPanel.setLayout( new GridBagLayout());
    mainAnalysis_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),"Analyze" ));
    JGUIUtil.addComponent( main_JPanel, mainAnalysis_JPanel,
        0, ++yMain, 1, 1, 0, .5, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Create a list containing the identifiers needed to populate the
	// dependent and independent time series controls. 
	List tsids = null;
	if ( __command.isCommandMode() ) {
		
		tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
				(TSCommandProcessor)__command.getCommandProcessor(), __command );
		
	} else {
		List tsObjects = null;
		try { Object o = __processor.getPropContents ( "TSResultsList" );
			tsObjects = (List)o;
		}
		catch ( Exception e ) {
				String message = "Cannot get time series list to process.";
				Message.printWarning ( 3, mthd, message );
		}
		// Create a vector containing the ts identifiers.
		if ( tsObjects != null ) {
			int size = tsObjects.size();
			tsids = new Vector( size );
			for ( int i = 0; i < size; i++ ) {
				TS ts = (TS) tsObjects.get(i);
                String display_name = ts.getAlias();
				if ( display_name != null && display_name.length()>0 ) {
                    tsids.add ( ts.getAlias() );
                }
                else {
                    tsids.add ( ts.getIdentifier().toString(false) );
                }
			}
		}
    }

	// Check if we have anything to display.			
	int size = 0;
	if ( tsids != null ) {
		size = tsids.size();
	}
	if ( size == 0 ) {
		mssg = "You may need to define time series before inserting the "
			+ __command.getCommandName() + "() command.";
		Message.printWarning ( 1, mthd, mssg );
		this.requestFocus();
		response ( false );
	}

	// How to get the dependent time series list to fill.
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ("Dependent TS list:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__DependentTSList_JComboBox = new SimpleJComboBox(false);
    List DepTSList_Vector = new Vector();
    DepTSList_Vector.add ( TSListType.FIRST_MATCHING_TSID.toString());
	__DependentTSList_JComboBox.setData ( DepTSList_Vector );
	__DependentTSList_JComboBox.addItemListener (this);
	JGUIUtil.addComponent(mainAnalysis_JPanel, __DependentTSList_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel (
		"Required - How to get the dependent time series to fill."),
		7, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	// Dependent time series list.
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel (
		"Dependent time series:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);

	List dts = new Vector();
	for ( int i = 0; i < size; i++ ) {
		dts.add( (String) tsids.get(i) );
	}
	dts.add( (String) "*" );

	__DependentTSID_SimpleJList = new SimpleJList (dts);
	__DependentTSID_SimpleJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
	__DependentTSID_SimpleJList.setVisibleRowCount( 2 );
	// Make sure to set the flag ignoreValueChanged to false and
	// then back to true when executing the select() methods.
	ignoreValueChanged = true;
	__DependentTSID_SimpleJList.select( 0 );
	ignoreValueChanged = false;
	__DependentTSID_SimpleJList.addListSelectionListener ( this );
	__DependentTSID_SimpleJList.addKeyListener ( this );
	__DependentTSID_SimpleJList.addMouseListener ( this );
	__DependentTSID_SimpleJList.setEnabled(false);
	JGUIUtil.addComponent( mainAnalysis_JPanel,
		new JScrollPane(__DependentTSID_SimpleJList),
		1, y, 6, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Required - Select one."),
		7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// How to get the independent time series.
	++y;
	__IndependentTSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel(
            this, mainAnalysis_JPanel, new JLabel ("Independent TS list:"), __IndependentTSList_JComboBox, y);
    // Default is not to add SpecifiedTSID so add it here...
    __IndependentTSList_JComboBox.add(TSListType.SPECIFIED_TSID.toString());
    __IndependentTSList_JComboBox.remove(TSListType.ENSEMBLE_ID.toString());
    

	List its = new Vector();
	for ( int i = 0; i < size; i++ ) {
		its.add( (String) tsids.get(i) );
	}
    // "*" is automatically added
    
    __IndependentTSID_SimpleJComboBox = new SimpleJComboBox(true);
    __IndependentTSID_ComboBoxLabel = new JLabel ("Independent time series (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    y = CommandEditorUtil.addTSIDToEditorDialogPanel (
            this, this, mainAnalysis_JPanel,
            __IndependentTSID_ComboBoxLabel,
            __IndependentTSID_SimpleJComboBox, tsids, y );
    __IndependentTSID_ComboBoxNote = new JLabel ( "Required - Select or enter identifier.");
    JGUIUtil.addComponent(mainAnalysis_JPanel, __IndependentTSID_ComboBoxNote,
		7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __IndependentTSID_ListLabel = new JLabel ("Independent time series (for TSList=" + TSListType.SPECIFIED_TSID.toString() + "):");
	__IndependentTSID_SimpleJList = new SimpleJList(its);
	__IndependentTSID_SimpleJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION );
	__IndependentTSID_SimpleJList.setVisibleRowCount( 5 );
	// Make sure to set the flag ignoreValueChanged to false and
	// then back to true when executing the select()
	// methods.
	ignoreValueChanged = true;
	if ( its.size() == 1 ) {
		__IndependentTSID_SimpleJList.select ( 0 );
	} else {
		__IndependentTSID_SimpleJList.select ( 1 );
	}
	ignoreValueChanged = false;
	__IndependentTSID_SimpleJList.addListSelectionListener ( this );
	__IndependentTSID_SimpleJList.addKeyListener           ( this );
	__IndependentTSID_SimpleJList.addMouseListener         ( this );
	__IndependentTSID_SimpleJList.setEnabled(false);
    JGUIUtil.addComponent(mainAnalysis_JPanel, __IndependentTSID_ListLabel,
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IndependentTSID_ListNote = new JLabel ( "Required - Select all to include in analysis.");
	JGUIUtil.addComponent(mainAnalysis_JPanel,
		new JScrollPane(__IndependentTSID_SimpleJList),
		1, y, 6, 1, 1, 1, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST );
    JGUIUtil.addComponent(mainAnalysis_JPanel, __IndependentTSID_ListNote,
		7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    resetTimeSeriesList();

	// Analysis period
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Analysis period:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AnalysisStart_JTextField = new JTextField ( "", 25 );
	__AnalysisStart_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel,
		__AnalysisStart_JTextField,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "to" ),
		2, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__AnalysisEnd_JTextField = new JTextField ( "", 25 );
	__AnalysisEnd_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __AnalysisEnd_JTextField,
		3, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Optional.  Period to analyze (default=analyze all)."),
		7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	
	// MaxCombinations
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Maximum combinations:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MaxCombinations_JTextField = new JTextField ( "", 25 );
	__MaxCombinations_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __MaxCombinations_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Optional - Number of equations returned (default=20)."),
		7, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	
	// File to save PCA results.
	JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel (
		"PCA output file:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PCAOutputFile_JTextField = new JTextField ( 50 );
	__PCAOutputFile_JTextField.addKeyListener ( this );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __PCAOutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
	__browse_JButton.setToolTipText( "Browse to select analysis output file." );
	JGUIUtil.addComponent(mainAnalysis_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    JGUIUtil.addComponent(mainAnalysis_JPanel, new JLabel ( "Required - PCA analysis results."),
		7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    if ( !__command.isCommandMode() ) {
        JPanel buttonAnalyze_JPanel = new JPanel();
        buttonAnalyze_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainAnalysis_JPanel, buttonAnalyze_JPanel,
            0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

		// Analyze button: used only when running as a TSTool tool.
		__analyze_JButton = new SimpleJButton(__analyze_String, this);
		__analyze_JButton .setToolTipText( __analyze_Tip );
		buttonAnalyze_JPanel.add ( __analyze_JButton );

		// View button: used only when running as a TSTool tool.
		__view_JButton = new SimpleJButton ( __view_String, this );
		__view_JButton.setToolTipText( __view_Tip );
		__view_JButton.setEnabled( false );
		buttonAnalyze_JPanel.add ( __view_JButton );
    }

    JPanel mainFill_JPanel = new JPanel();
    mainFill_JPanel.setLayout( new GridBagLayout() );
    mainFill_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),"Fill Using Analysis Results" ));
    JGUIUtil.addComponent( main_JPanel, mainFill_JPanel,
            0, ++yMain, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // regression equation to use for fill
    JGUIUtil.addComponent(mainFill_JPanel, new JLabel ("Regression Equation:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List tmp_Vector = new Vector();
    tmp_Vector.add ( "XXX");
	__RegressionEquationFill_SimpleJComboBox = new SimpleJComboBox (false);
	__RegressionEquationFill_SimpleJComboBox.addItemListener(this);
	__RegressionEquationFill_SimpleJComboBox.addKeyListener( this );
	__RegressionEquationFill_SimpleJComboBox.addMouseListener( this );
	__RegressionEquationFill_SimpleJComboBox.setEnabled(false);
    __RegressionEquationFill_SimpleJComboBox.setData(tmp_Vector);
	JGUIUtil.addComponent(mainFill_JPanel,
		new JScrollPane(__RegressionEquationFill_SimpleJComboBox),
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(mainFill_JPanel, new JLabel ( "Required to fill - Select index of desired equation to use for filling missing data."),
		7, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Fill Period
	JGUIUtil.addComponent(mainFill_JPanel, new JLabel ( "Fill period:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FillStart_JTextField = new JTextField ( "", 25 );
	__FillStart_JTextField.addKeyListener ( this );
	__FillStart_JTextField.setEnabled ( false );
	JGUIUtil.addComponent(mainFill_JPanel, __FillStart_JTextField,
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(mainFill_JPanel, new JLabel ( "to" ),
		2, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	__FillEnd_JTextField = new JTextField ( "", 25 );
	__FillEnd_JTextField.addKeyListener ( this );
	__FillEnd_JTextField.setEnabled ( false );
	JGUIUtil.addComponent(mainFill_JPanel, __FillEnd_JTextField,
		3, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(mainFill_JPanel, new JLabel ( "Optional.  Period to fill (default=fill all)."),
		7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// File to save TS results.
	JGUIUtil.addComponent(mainFill_JPanel, new JLabel (
		"Filled TS output file:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__FilledTSOutputFile_JTextField = new JTextField ( 50 );
	__FilledTSOutputFile_JTextField.addKeyListener ( this );
	__FilledTSOutputFile_JTextField.setEnabled ( false );
	JGUIUtil.addComponent(mainFill_JPanel, __FilledTSOutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseTS_JButton = new SimpleJButton ( "Browse", this );
	__browseTS_JButton.setToolTipText( "Browse to select filled time series output file." );
	JGUIUtil.addComponent(mainFill_JPanel, __browseTS_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __browseTS_JButton.setEnabled ( false );
    JGUIUtil.addComponent(mainFill_JPanel, new JLabel ( "Required to fill - filled time series results."),
		7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    if ( !__command.isCommandMode() ) {
        JPanel buttonFill_JPanel = new JPanel();
        buttonFill_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainFill_JPanel, buttonFill_JPanel,
            0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

        // fillDependents button: used only when running as a tool.
		__fillDependents_JButton = new SimpleJButton(__fillDependents_String, this);
		__fillDependents_JButton .setToolTipText( __fillDependents_Tip );
		__fillDependents_JButton.setEnabled( false );
		buttonFill_JPanel.add ( __fillDependents_JButton );

        // View button: used only when running as a TSTool tool.
		__viewTS_JButton = new SimpleJButton ( __viewTS_String, this );
		__viewTS_JButton.setToolTipText( __view_Tip );
		__viewTS_JButton.setEnabled( false );
		buttonFill_JPanel.add ( __viewTS_JButton );

    }

    JPanel mainTransfer_JPanel = new JPanel();
    mainTransfer_JPanel.setLayout( new GridBagLayout() );
    mainTransfer_JPanel.setBorder( BorderFactory.createTitledBorder (
            BorderFactory.createLineBorder(Color.black),"Transfer Analysis Parameters/Results to Commands" ));
    JGUIUtil.addComponent( main_JPanel, mainTransfer_JPanel,
            0, ++yMain, 1, 1, 0, 1, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

    y=0;
	// Command - Currently showing only under the command mode
	if ( __command.isCommandMode() ) {
		__Command_JLabel = new JLabel ( "Command:" );
		JGUIUtil.addComponent(mainTransfer_JPanel, __Command_JLabel,
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__Command_JTextArea = new JTextArea (7,80);
		__Command_JTextArea.setLineWrap ( true );
		__Command_JTextArea.setWrapStyleWord ( true );
		__Command_JTextArea.setEditable ( false );
		__Command_JScrollPane = new JScrollPane( __Command_JTextArea );
		JGUIUtil.addComponent(mainTransfer_JPanel, __Command_JScrollPane,
			1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        // add buttons to add/remove working directory from PCA and TS output file names
        JPanel buttonTransfer_JPanel = new JPanel();
        buttonTransfer_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainTransfer_JPanel, buttonTransfer_JPanel,
            0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        JPanel path_JPanel = new JPanel();
        path_JPanel.setLayout ( new FlowLayout (FlowLayout.CENTER));
        __pathPCA_JButton = new SimpleJButton(__RemoveWorkingDirectoryPCAFile_String, this);
        __pathPCA_JButton.setEnabled ( false );
        path_JPanel.add ( __pathPCA_JButton );
        __pathFillTS_JButton = new SimpleJButton(__RemoveWorkingDirectoryFillTSFile_String, this);
        __pathFillTS_JButton.setEnabled ( false );
        path_JPanel.add ( __pathFillTS_JButton );
        buttonTransfer_JPanel.add ( path_JPanel );
	} else {
		// These controls will initialially be invisible. Only when commands
		// are available they will be set visible.
        JPanel buttonTransferTop_JPanel = new JPanel();
        buttonTransferTop_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainTransfer_JPanel, buttonTransferTop_JPanel,
            0, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

        // createFillCommands button: used only when running as a tool.
		__createFillCommands_JButton = new SimpleJButton(__createFillCommands_String, this);
		__createFillCommands_JButton .setToolTipText(__createFillCommands_Tip );
		__createFillCommands_JButton.setEnabled( false );
		buttonTransferTop_JPanel.add ( __createFillCommands_JButton );

		__Command_JLabel = new JLabel ( "Fill Commands:" );
		JGUIUtil.addComponent(mainTransfer_JPanel, __Command_JLabel,
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		__Command_JLabel.setVisible ( false );
		__Command_JTextArea = new JTextArea (7,80);
		__Command_JTextArea.setLineWrap ( true );
		__Command_JTextArea.setWrapStyleWord ( true );
		__Command_JTextArea.setEditable ( false );
		__PCAOutputFile_JTextField.setEditable ( false );
		__Command_JTextArea.setBackground (__PCAOutputFile_JTextField.getBackground());
		__PCAOutputFile_JTextField.setEditable ( true );
		__Command_JScrollPane = new JScrollPane( __Command_JTextArea );
		JGUIUtil.addComponent(mainTransfer_JPanel, __Command_JScrollPane,
			1, y, 9, 4, 1, 1, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		__Command_JScrollPane.setVisible (false );
		__Command_JTextArea.setVisible   ( false );

        y += 4;
        JPanel buttonTransfer_JPanel = new JPanel();
        buttonTransfer_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(mainTransfer_JPanel, buttonTransfer_JPanel,
            0, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

        JPanel path_JPanel = new JPanel();
        path_JPanel.setLayout ( new FlowLayout (FlowLayout.CENTER));
        __pathPCA_JButton = new SimpleJButton(__RemoveWorkingDirectoryPCAFile_String, this);
        __pathPCA_JButton.setEnabled ( false );
        path_JPanel.add ( __pathPCA_JButton );
        __pathFillTS_JButton = new SimpleJButton(__RemoveWorkingDirectoryFillTSFile_String, this);
        __pathFillTS_JButton.setEnabled ( false );
        path_JPanel.add ( __pathFillTS_JButton );
        buttonTransfer_JPanel.add ( path_JPanel );

		// copyCommandsToTSTool button: used only when running as a tool.
		__copyFillCommandsToTSTool_JButton = new SimpleJButton(__copyCommandsToTSTool_String, this);
		__copyFillCommandsToTSTool_JButton.setToolTipText( __copyCommandsToTSTool_Tip );
		__copyFillCommandsToTSTool_JButton.setEnabled( false );
        JGUIUtil.addComponent(mainTransfer_JPanel, __copyFillCommandsToTSTool_JButton,
            0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
		// mainTransfer_JPanel.add ( __copyFillCommandsToTSTool_JButton );
	}

	// Refresh the contents...
    __IndependentTSList_JComboBox.select(TSListType.ALL_TS.toString());
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    y++;
	JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++yMain, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.SOUTH);

	if ( __command.isCommandMode() ) {
		// Cancel button: used when running as a command
		__cancel_JButton = new SimpleJButton( __cancel_String, this);
		__cancel_JButton.setToolTipText( __cancel_Tip );
		button_JPanel.add ( __cancel_JButton );

	} else {
		// Close button: used when running as a tool
		__close_JButton = new SimpleJButton( __close_String, this);
		__close_JButton.setToolTipText( __close_Tip );
		button_JPanel.add ( __close_JButton );
	}

	if ( __command.isCommandMode() ) {
		// OK button: used only when running as a TSTool command.
		__ok_JButton = new SimpleJButton(__ok_String, this);
		__ok_JButton .setToolTipText( __ok_Tip );
		button_JPanel.add ( __ok_JButton );
	}


	// Set up the status bar.
	__statusJTextField = new JTextField();
	__statusJTextField.setEditable(false);
	JPanel statusJPanel = new JPanel();
	statusJPanel.setLayout(new GridBagLayout());
	JGUIUtil.addComponent(statusJPanel, __statusJTextField, 0, 0, 1, 1, 1, 1,
		GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
	getContentPane().add ( "South", statusJPanel);

	// Add the Dependent Pop-up menu to manipulate time series...
	__DependentTS_JPopupMenu = new JPopupMenu("Dependent TS Actions");
	__DependentTS_JPopupMenu.add( new SimpleJMenuItem (__SELECT_ALL_DEPENDENT,   this) );
	__DependentTS_JPopupMenu.add( new SimpleJMenuItem (__DESELECT_ALL_DEPENDENT, this) );

	// Add the Independent Pop-up menu to manipulate time series...
	__IndependentTS_JPopupMenu = new JPopupMenu("Independent TS Actions");
	__IndependentTS_JPopupMenu.add( new SimpleJMenuItem (__SELECT_ALL_INDEPENDENT, this));
	__IndependentTS_JPopupMenu.add( new SimpleJMenuItem (__DESELECT_ALL_INDEPENDENT, this));

	// Refresh the contents...
	if ( __command.isCommandMode() ) {
		refresh();
	}

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
			// Show this menu only if applicable (SPECIFIED_TSID)
            String dependentList = __DependentTSList_JComboBox.getSelected();
			if ( dependentList.equalsIgnoreCase(TSListType.SPECIFIED_TSID.toString())) {
				__DependentTS_JPopupMenu.show (
					event.getComponent(),
					event.getX(), event.getY() );
			}	
		} 
		
		// Independent time series
		else if (event.getComponent()==__IndependentTSID_SimpleJList) {
			// Show this menu only if applicable (SPECIFIED_TSID)
            String independentList = __IndependentTSList_JComboBox.getSelected();
			if ( independentList.equalsIgnoreCase (TSListType.SPECIFIED_TSID.toString())) {
				__IndependentTS_JPopupMenu.show (
					event.getComponent(),
					event.getX(), event.getY() );
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
Read the content of a text file, returning a vector of strings,
one string per line.
@param fileName - The file (path and file) to read.
*/
List readTextFile ( String fileName )
{
	List 	_fileContent = new Vector();	// The return vector.
	FileReader _fileReader;			// The actual file stream.
	BufferedReader	_bufferedReader ;	// Used to read the file line by line.
	String line;

	try {
		String adjusted_path = IOUtil.adjustPath (
				 __working_dir, fileName);
		_fileReader     = new FileReader ( new File( adjusted_path ) );
		_bufferedReader = new BufferedReader( _fileReader );

		do {
			line = _bufferedReader.readLine();
			if( line!=null ) _fileContent.add( line );
		} while( line!=null  );

	} catch( Exception e ) {
		Message.printWarning ( 1, "", "Error reading file." );
		this.requestFocus();
		return null;
	}

	return _fileContent;
}

/**
Refresh the command from the other text field contents:
<pre>
fillPrincipalComponentAnalysis ( DependentTSList="...",
		   DependentTSList="X,Y,...",
		   IndependentTSList="...",
		   IndependentTSList="X,Y,...",
		   AnalysisStart="...",
		   AnalysisEnd="...",
		   FillStart="...",
		   FillEnd="...",
		   MaxCombinations="...",
           RegressionEquation="...",
		   PCAOutputFile="...",
           FilledTSOutputFile="...")
</pre>
*/
private void refresh()
{
    if (  !__command.isCommandMode() ) {
        // This does not apply when in tool mode
        return;
    }
	String mthd = __command.getCommandName() + "_JDialog.refresh";

	String DependentTSList  = "";  // How to get list of depend.  time series
	String DependentTSID    = "";  // Dependent Time series.

	String IndependentTSList= ""; // How to get list of independ time series
	String IndependentTSID  = ""; // Independent Time series.

	String AnalysisStart	= "";
	String AnalysisEnd	= "";
	String FillStart 	= "";
	String FillEnd 		= "";
	String MaxCombinations = "";
	String RegressionEquationFill = "";
	String PCAOutputFile	= "";
	String FilledTSOutputFile	= "";

	__error_wait = false;

	PropList props 	= null;
	List v	= null;

	if ( __first_time ) {
		__first_time = false;

		// Get the properties from the command
		props = __command.getCommandParameters();
		DependentTSList   = props.getValue ( "DependentTSList"  );
		DependentTSID     = props.getValue ( "DependentTSID"    );
		IndependentTSList = props.getValue ( "IndependentTSList");
		IndependentTSID   = props.getValue ( "IndependentTSID"  );
		AnalysisStart	  = props.getValue ( "AnalysisStart"    );
		AnalysisEnd	  = props.getValue ( "AnalysisEnd"      );
		FillStart	  = props.getValue ( "FillStart"        );
		FillEnd		  = props.getValue ( "FillEnd"          );
		MaxCombinations = props.getValue ( "MaxCombinations"  );
		RegressionEquationFill = props.getValue ( "RegressionEquationFill"  );
		PCAOutputFile	  = props.getValue ( "PCAOutputFile"       );
		FilledTSOutputFile	  = props.getValue ( "FilledTSOutputFile"       );

		// Make sure the DependentTSList option is valid
		if ( DependentTSList == null ) {
			// Select default...
			__DependentTSList_JComboBox.select ( 0 );
		} else {
			if (	JGUIUtil.isSimpleJComboBoxItem(
				__DependentTSList_JComboBox,
				DependentTSList, JGUIUtil.NONE, null, null ) ) {
					__DependentTSList_JComboBox.select (
						DependentTSList );
			}
			else {
				Message.printWarning ( 1, mthd,
					  "Existing command references an "
					+ "invalid\nDependentTSList value \""
					+ DependentTSList + "\".  "
					+ "Select a different value or Cancel.");
				this.requestFocus();
				__error_wait = true;
			}
		}

		// Make sure the IndependentTSList option is valid
		if ( IndependentTSList == null ) {
			// Select default ...
			__IndependentTSList_JComboBox.select ( 0 );
		} else {
			if (	JGUIUtil.isSimpleJComboBoxItem(
				__IndependentTSList_JComboBox,
				IndependentTSList, JGUIUtil.NONE, null, null ) ) {
					__IndependentTSList_JComboBox.select (
						IndependentTSList );
			}
			else {
				Message.printWarning ( 1, mthd,
					  "Existing command references an "
					+ "invalid\nIndependentTSList value \""
					+ IndependentTSList + "\".  "
					+ "Select a different value or Cancel.");
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
		if ((DependentTSList != null) &&
			(DependentTSList.equalsIgnoreCase(TSListType.FIRST_MATCHING_TSID.toString())) &&
			(DependentTSID != null) ) {
			v = StringUtil.breakStringList (
				DependentTSID, ",", StringUtil.DELIM_SKIP_BLANKS );
			int size = v.size();
			int pos = 0;
			List selected = new Vector();
			String dependent = "";
			for ( int i = 0; i < size; i++ ) {
				dependent = (String)v.get(i);
				if ( (pos = JGUIUtil.indexOf(
					__DependentTSID_SimpleJList,
					dependent, false, true))>= 0 ) {
					// Select it because it is in the
					// command and the list...
					selected.add ( "" + pos );
				} else {
					Message.printWarning ( 1, mthd,
					"Existing " +
					"command references a non-existent\n"+
					"time series \"" + dependent +
					"\".  Select a\n" +
					"different time series or Cancel." );
					this.requestFocus();
					__error_wait = true;
				}
			}

			// Select the matched time series...
			// Make sure to use setSelectedIndices to select multiple rows.
			if ( selected.size() > 0  ) {
				int [] iselected = new int[selected.size()];
				for ( int is = 0; is < iselected.length; is++ ){
					iselected[is] = StringUtil.atoi (
					(String)selected.get(is));
				}
				__DependentTSID_SimpleJList.setSelectedIndices(iselected );
			}
		}

		// Check all the items in the Independent time series list and
		// highlight the ones that match the command being edited...
		if ((IndependentTSList != null) &&
			(IndependentTSID != null) ) {
            if ( IndependentTSList.equalsIgnoreCase(TSListType.SPECIFIED_TSID.toString())) {
                v = StringUtil.breakStringList (
                    IndependentTSID, ",",
                    StringUtil.DELIM_SKIP_BLANKS );
                int size = v.size();
                int pos = 0;
                List selected = new Vector();
                String independent = "";
                for ( int i = 0; i < size; i++ ) {
                    independent = (String)v.get(i);
                    if ( (pos = JGUIUtil.indexOf(
                        __IndependentTSID_SimpleJList,
                        independent, false, true))>= 0 ) {
                        // Select it because it is in the
                        // command and the list...
                        selected.add ( "" + pos );
                    } else {
                        Message.printWarning ( 1, mthd,
                        "Existing " +
                        "command references a non-existent\n"+
                        "time series \"" + independent +
                        "\".  Select a\n" +
                        "different time series or Cancel." );
                        this.requestFocus();
                        __error_wait = true;
                    }
                }

                // Select the matched time series...
                // Make sure to use setSelectedIndices to select multiply
                // rows.
                if ( selected.size() > 0  ) {
                    int [] iselected = new int[selected.size()];
                    for ( int is = 0; is < iselected.length; is++ ){
                        iselected[is] = StringUtil.atoi (
                        (String)selected.get(is));
                    }
                    __IndependentTSID_SimpleJList.setSelectedIndices(
                        iselected );
                }
            } else if ( IndependentTSList.equalsIgnoreCase(TSListType.ALL_MATCHING_TSID.toString()) ||
                    IndependentTSList.equalsIgnoreCase(TSListType.FIRST_MATCHING_TSID.toString()) ||
                    IndependentTSList.equalsIgnoreCase(TSListType.LAST_MATCHING_TSID.toString())) {
                    __IndependentTSID_SimpleJComboBox.add(IndependentTSID);
                    __IndependentTSID_SimpleJComboBox.select(IndependentTSID);
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

		// Check FillStart and and update the text field
		if ( FillStart == null ) {
			__FillStart_JTextField.setText ( "" );
		} else {
			__FillStart_JTextField.setText ( FillStart );
		}
        __FillStart_JTextField.setEnabled(true);

		// Check FillEnd and update the text field
		if ( FillEnd == null ) {
			__FillEnd_JTextField.setText ( "" );
		} else {
			__FillEnd_JTextField.setText ( FillEnd );
		}
        __FillEnd_JTextField.setEnabled(true);

		// Check MaxCombinations and update the text field
        __RegressionEquationFill_SimpleJComboBox.removeAll();
		if ( MaxCombinations == null || MaxCombinations.length()==0) {
			__MaxCombinations_JTextField.setText ( "" );
            for ( int i=1; i<=FillPrincipalComponentAnalysis_Command._maxCombinationsDefault; i++ ) {
                __RegressionEquationFill_SimpleJComboBox.add(""+i);
            }
		} else {
			__MaxCombinations_JTextField.setText ( MaxCombinations );
            for ( int i=1; i<=Integer.parseInt(MaxCombinations); i++ ) {
                __RegressionEquationFill_SimpleJComboBox.add(""+i);
            }
		}
        __RegressionEquationFill_SimpleJComboBox.setEnabled(true);

        // Check RegressionEquationFill and update the text field
        if ( RegressionEquationFill != null ) {
            __RegressionEquationFill_SimpleJComboBox.select(RegressionEquationFill);
        }

		// Check PCAOutputFile and update the text field
		if ( PCAOutputFile == null ) {
			__PCAOutputFile_JTextField.setText ( "" );
		} else {
			__PCAOutputFile_JTextField.setText ( PCAOutputFile );
		}

		// Check FilledTSOutputFile and update the text field
		if ( FilledTSOutputFile == null ) {
			__FilledTSOutputFile_JTextField.setText ( "" );
		} else {
			__FilledTSOutputFile_JTextField.setText ( FilledTSOutputFile );
		}
        __FilledTSOutputFile_JTextField.setEnabled( true );

        __browseTS_JButton.setEnabled(true);

	} else {
		// Enable or disable the selection of dependent time series
		// according to the settings of DependentTSList parameter.
		// Enable or disable the selection of independent time series
		// according to the settings of IndependentTSList parameter.
		resetTimeSeriesList();
	}

	// Update the __DependentTSID_SimpleJList and
	// __IndependentTSID_SimpleJList to have only the * selected, it the
	// * is selected by the user.
	resetTimeSeriesID_JLists();

	// Regardless, reset the command from the interface fields...
	DependentTSList  = __DependentTSList_JComboBox.getSelected();
	DependentTSID    = getDependentTSIDFromInterface();
	IndependentTSList= __IndependentTSList_JComboBox.getSelected();
	IndependentTSID    = getIndependentTSIDFromInterface();
	AnalysisStart    = __AnalysisStart_JTextField.getText().trim();
	AnalysisEnd      = __AnalysisEnd_JTextField.getText().trim();
	FillStart        = __FillStart_JTextField.getText().trim();
	FillEnd          = __FillEnd_JTextField.getText().trim();
	MaxCombinations  = __MaxCombinations_JTextField.getText().trim();
    RegressionEquationFill = __RegressionEquationFill_SimpleJComboBox.getSelected();
	PCAOutputFile       = __PCAOutputFile_JTextField.getText().trim();
	FilledTSOutputFile  = __FilledTSOutputFile_JTextField.getText().trim();

	// And set the command properties.
	props = new PropList ( __command.getCommandName() );
	props.add ( "DependentTSList="   + DependentTSList  );
	props.add ( "DependentTSID="     + DependentTSID    );
	props.add ( "IndependentTSList=" + IndependentTSList);
	props.add ( "IndependentTSID="   + IndependentTSID  );
	props.add ( "AnalysisStart="     + AnalysisStart    );
	props.add ( "AnalysisEnd="       + AnalysisEnd      );
	props.add ( "FillStart="         + FillStart        );
	props.add ( "FillEnd="           + FillEnd          );
	props.add ( "MaxCombinations="   + MaxCombinations  );
	props.add ( "RegressionEquationFill="   + RegressionEquationFill  );
	props.add ( "PCAOutputFile="        + PCAOutputFile       );
	props.add ( "FilledTSOutputFile="   + FilledTSOutputFile  );

	// Update the __Command_JTextArea if running under the command mode. 
	if ( __command.isCommandMode() ) {
		__Command_JTextArea.setText( __command.toString(props) );
	}
    // Check the path and determine what the label on the path buttons should be...
        if ( __pathPCA_JButton != null && PCAOutputFile != null ) {
            __pathPCA_JButton.setEnabled ( true );
            File f = new File ( PCAOutputFile );
            if ( f.isAbsolute() ) {
                __pathPCA_JButton.setText (__RemoveWorkingDirectoryPCAFile_String);
            }
            else {
                __pathPCA_JButton.setText (__AddWorkingDirectoryPCAFile_String );
            }
        }
        if ( __pathFillTS_JButton != null && FilledTSOutputFile != null ) {
            __pathFillTS_JButton.setEnabled ( true );
            File f = new File ( FilledTSOutputFile );
            if ( f.isAbsolute() ) {
                __pathFillTS_JButton.setText (__RemoveWorkingDirectoryFillTSFile_String);
            }
            else {
                __pathFillTS_JButton.setText (__AddWorkingDirectoryFillTSFile_String );
            }
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
Enabled only if ALL_MATCHING_TSID, FIRST_MATCHING_TSID, LAST_MATCHING_TSID or SPECIFIED_TSID.
*/
private void resetTimeSeriesList()
{
	// Dependent time series list
    String dependentIDList = __DependentTSList_JComboBox.getSelected();
	if ( dependentIDList.equalsIgnoreCase( TSListType.ALL_MATCHING_TSID.toString()) ||
         dependentIDList.equalsIgnoreCase( TSListType.FIRST_MATCHING_TSID.toString()) ||
         dependentIDList.equalsIgnoreCase( TSListType.LAST_MATCHING_TSID.toString()) ||
         dependentIDList.equalsIgnoreCase( TSListType.SPECIFIED_TSID.toString())
            ) {
		__DependentTSID_SimpleJList.setEnabled( true );
	} else {
		__DependentTSID_SimpleJList.setEnabled( false );
	}

    __IndependentTSID_ListLabel.setEnabled(false);
    __IndependentTSID_SimpleJList.setEnabled( false );
    __IndependentTSID_ListNote.setEnabled( false );

    __IndependentTSID_ComboBoxLabel.setEnabled(false);
    __IndependentTSID_SimpleJComboBox.setEnabled( false );
    __IndependentTSID_ComboBoxNote.setEnabled( false );

	// Independent time series list
    String independentIDList = __IndependentTSList_JComboBox.getSelected();
    if ( independentIDList.equalsIgnoreCase(TSListType.ALL_TS.toString()) ||
            independentIDList.equalsIgnoreCase(TSListType.SELECTED_TS.toString())) {
        // no action needed
    }
    else if ( independentIDList.equalsIgnoreCase( TSListType.ALL_MATCHING_TSID.toString()) ||
         independentIDList.equalsIgnoreCase( TSListType.FIRST_MATCHING_TSID.toString()) ||
         independentIDList.equalsIgnoreCase( TSListType.LAST_MATCHING_TSID.toString())
            ) {
        __IndependentTSID_ComboBoxLabel.setEnabled(true);
		__IndependentTSID_SimpleJComboBox.setEnabled( true );
		__IndependentTSID_ComboBoxNote.setEnabled( true );
	} else { //independentIDList.equalsIgnoreCase( TSListType.SPECIFIED_TSID.toString())
        __IndependentTSID_ListLabel.setEnabled(true);
		__IndependentTSID_SimpleJList.setEnabled( true );
		__IndependentTSID_ListNote.setEnabled( true );
	}
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed, if there is no error waiting, otherwise we are not
ready to close, so simple return.
*/
public void response (  boolean ok  )
{
// REVISIT [LT 2005-06-01] Why commit the changes if there are errors? Why check
// for errors anyway? Is't it true that the only way to get here with ok=true
// is if the checkInput was called, succeded and the _error_wait flag was set 
// to false (see ActionPerformed method)?
// Make consistend, if modified, in all the other commands using the new scheme 
// (JDialog and Command classes). 	
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
	__Command_JLabel.setVisible(true);
	__Command_JScrollPane.setVisible(true);
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
public void windowClosed	( WindowEvent evt ){;}
public void windowDeactivated	( WindowEvent evt ){;}
public void windowDeiconified	( WindowEvent evt ){;}
public void windowIconified	( WindowEvent evt ){;}
public void windowOpened	( WindowEvent evt ){;}

} // end fillPrincipalComponentAnalysis_JDialog
