package rti.tscommandprocessor.commands.hydrojson;

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
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the WriteTimeSeriesToHydroJSON() command.
*/
public class WriteTimeSeriesToHydroJSON_Dialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private final String __AddWorkingDirectory = "Add Working Directory";
private final String __RemoveWorkingDirectory = "Remove Working Directory";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __path_JButton = null;
private WriteTimeSeriesToHydroJSON_Command __command = null;
private JTabbedPane __main_JTabbedPane = null;
private String __working_dir = null;
private JTextArea __command_JTextArea=null;
// Time series parameters...
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private JTextField __MissingValue_JTextField = null;
private JTextField __Precision_JTextField = null;
// Time series parameters...
private JTextField __TIdentifier_JTextField = null;
private JTextField __THash_JTextField = null;
private JTextField __TQualityType_JTextField = null;
private JTextField __TParameter_JTextField = null;
private JTextField __TDuration_JTextField = null;
private JTextField __TInterval_JTextField = null;
private JTextField __TUnits_JTextField = null;
// Station parameters...
private JTextField __SName_JTextField = null;
private JTextField __SResponsibility_JTextField = null;
private JTextField __SCoordLatitude_JTextField = null;
private JTextField __SCoordLongitude_JTextField = null;
private JTextField __SCoordDatum_JTextField = null;
private JTextField __SHUC_JTextField = null;
private JTextField __SElevValue_JTextField = null;
private JTextField __SElevAccuracy_JTextField = null;
private JTextField __SElevDatum_JTextField = null;
private JTextField __SElevMethod_JTextField = null;
private JTextField __STimeZone_JTextField = null;
private JTextField __STimeZoneOffset_JTextField = null;
private JTextField __STimeFormat_JTextField = null;
private JTextField __SActiveFlag_JTextField = null;
private JTextField __SLocationType_JTextField = null;
// File parameters...
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __PrintNice_JComboBox = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog?

/**
Dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteTimeSeriesToHydroJSON_Dialog ( JFrame parent, WriteTimeSeriesToHydroJSON_Command command )
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
		fc.setDialogTitle("Select HydroJSON File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("json", "JSON File");
		fc.addChoosableFileFilter(sff);
		
		if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				__OutputFile_JTextField.setText(path );
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
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __OutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "WriteTimeSeriesToHydroJSON_JDialog", "Error converting file to relative path." );
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
	String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
    String MissingValue = __MissingValue_JTextField.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
	String TIdentifier = __TIdentifier_JTextField.getText().trim();
	String THash = __THash_JTextField.getText().trim();
	String TQualityType = __TQualityType_JTextField.getText().trim();
	String TParameter = __TParameter_JTextField.getText().trim();
	String TDuration = __TDuration_JTextField.getText().trim();
	String TInterval = __TInterval_JTextField.getText().trim();
	String TUnits = __TUnits_JTextField.getText().trim();
	String SName = __SName_JTextField.getText().trim();
	String SResponsibility = __SResponsibility_JTextField.getText().trim();
	String SCoordLatitude = __SCoordLatitude_JTextField.getText().trim();
	String SCoordLongitude = __SCoordLongitude_JTextField.getText().trim();
	String SCoordDatum = __SCoordDatum_JTextField.getText().trim();
	String SHUC = __SHUC_JTextField.getText().trim();
	String SElevValue = __SElevValue_JTextField.getText().trim();
	String SElevAccuracy = __SElevAccuracy_JTextField.getText().trim();
	String SElevDatum = __SElevDatum_JTextField.getText().trim();
	String SElevMethod = __SElevMethod_JTextField.getText().trim();
	String STimeZone = __STimeZone_JTextField.getText().trim();
	String STimeZoneOffset = __STimeZoneOffset_JTextField.getText().trim();
	String STimeFormat = __STimeFormat_JTextField.getText().trim();
	String SActiveFlag = __SActiveFlag_JTextField.getText().trim();
	String SLocationType = __SLocationType_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String PrintNice = __PrintNice_JComboBox.getSelected();

	__error_wait = false;
	
	if ( TSList.length() > 0 ) {
		parameters.set ( "TSList", TSList );
	}
    if ( TSID.length() > 0 ) {
        parameters.set ( "TSID", TSID );
    }
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
	if ( OutputStart.length() > 0 ) {
		parameters.set ( "OutputStart", OutputStart );
	}
	if ( OutputEnd.length() > 0 ) {
		parameters.set ( "OutputEnd", OutputEnd );
	}
    if ( MissingValue.length() > 0 ) {
        parameters.set ( "MissingValue", MissingValue );
    }
    if (!Precision.isEmpty() ) {
        parameters.set("Precision", Precision);
    }
    if (!TIdentifier.isEmpty() ) {
        parameters.set("TIdentifier", TIdentifier);
    }
    if (!THash.isEmpty() ) {
        parameters.set("THash", THash);
    }
    if (!TQualityType.isEmpty() ) {
        parameters.set("TQualityType", TQualityType);
    }
    if (!TParameter.isEmpty() ) {
        parameters.set("TParameter", TParameter);
    }
    if (!TDuration.isEmpty() ) {
        parameters.set("TDuration", TDuration);
    }
    if (!TInterval.isEmpty() ) {
        parameters.set("TInterval", TInterval);
    }
    if (!TUnits.isEmpty() ) {
        parameters.set("TUnits", TUnits);
    }
    // Station parameters...
    if (!SName.isEmpty() ) {
        parameters.set("SName", SName);
    }
    if (!SResponsibility.isEmpty() ) {
        parameters.set("SResponsibility", SResponsibility);
    }
    if (!SCoordLatitude.isEmpty() ) {
        parameters.set("SCoordLatitude", SCoordLatitude);
    }
    if (!SCoordLongitude.isEmpty() ) {
        parameters.set("SCoordLongitude", SCoordLongitude);
    }
    if (!SCoordDatum.isEmpty() ) {
        parameters.set("SCoordDatum", SCoordDatum);
    }
    if (!SHUC.isEmpty() ) {
        parameters.set("SHUC", SHUC);
    }
    if (!SElevValue.isEmpty() ) {
        parameters.set("SElevValue", SElevValue);
    }
    if (!SElevAccuracy.isEmpty() ) {
        parameters.set("SElevAccuracy", SElevAccuracy);
    }
    if (!SElevDatum.isEmpty() ) {
        parameters.set("SElevDatum", SElevDatum);
    }
    if (!SElevMethod.isEmpty() ) {
        parameters.set("SElevMethod", SElevMethod);
    }
    if (!STimeZone.isEmpty() ) {
        parameters.set("STimeZone", STimeZone);
    }
    if (!STimeZoneOffset.isEmpty() ) {
        parameters.set("STimeZoneOffset", STimeZoneOffset);
    }
    if (!STimeFormat.isEmpty() ) {
        parameters.set("STimeFormat", STimeFormat);
    }
    if (!SActiveFlag.isEmpty() ) {
        parameters.set("SActiveFlag", SActiveFlag);
    }
    if (!SLocationType.isEmpty() ) {
        parameters.set("SLocationType", SLocationType);
    }
	if ( OutputFile.length() > 0 ) {
		parameters.set ( "OutputFile", OutputFile );
	}
	if ( PrintNice.length() > 0 ) {
		parameters.set ( "PrintNice", PrintNice );
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
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();  
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	String MissingValue = __MissingValue_JTextField.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
	String TIdentifier = __TIdentifier_JTextField.getText().trim();
	String THash = __THash_JTextField.getText().trim();
	String TQualityType = __TQualityType_JTextField.getText().trim();
	String TParameter = __TParameter_JTextField.getText().trim();
	String TDuration = __TDuration_JTextField.getText().trim();
	String TInterval = __TInterval_JTextField.getText().trim();
	String SName = __SName_JTextField.getText().trim();
	String SResponsibility = __SResponsibility_JTextField.getText().trim();
	String SCoordLatitude = __SCoordLatitude_JTextField.getText().trim();
	String SCoordLongitude = __SCoordLongitude_JTextField.getText().trim();
	String SCoordDatum = __SCoordDatum_JTextField.getText().trim();
	String SHUC = __SHUC_JTextField.getText().trim();
	String SElevValue = __SElevValue_JTextField.getText().trim();
	String SElevAccuracy = __SElevAccuracy_JTextField.getText().trim();
	String SElevDatum = __SElevDatum_JTextField.getText().trim();
	String SElevMethod = __SElevMethod_JTextField.getText().trim();
	String STimeZone = __STimeZone_JTextField.getText().trim();
	String STimeZoneOffset = __STimeZoneOffset_JTextField.getText().trim();
	String STimeFormat = __STimeFormat_JTextField.getText().trim();
	String SActiveFlag = __SActiveFlag_JTextField.getText().trim();
	String SLocationType = __SLocationType_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String PrintNice = __PrintNice_JComboBox.getSelected();
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
	__command.setCommandParameter ( "MissingValue", MissingValue );
	__command.setCommandParameter ( "Precision", Precision );
	__command.setCommandParameter ( "TIdentifier", TIdentifier );
	__command.setCommandParameter ( "THash", THash );
	__command.setCommandParameter ( "TQualityType", TQualityType );
	__command.setCommandParameter ( "TParameter", TParameter );
	__command.setCommandParameter ( "TDuration", TDuration );
	__command.setCommandParameter ( "TInterval", TInterval );
	__command.setCommandParameter ( "SName", SName );
	__command.setCommandParameter ( "SResponsibility", SResponsibility );
	__command.setCommandParameter ( "SCoordLatitude", SCoordLatitude );
	__command.setCommandParameter ( "SCoordLongitude", SCoordLongitude );
	__command.setCommandParameter ( "SCoordDatum", SCoordDatum );
	__command.setCommandParameter ( "SHUC", SHUC );
	__command.setCommandParameter ( "SElevValue", SElevValue );
	__command.setCommandParameter ( "SElevAccuracy", SElevAccuracy );
	__command.setCommandParameter ( "SElevDatum", SElevDatum );
	__command.setCommandParameter ( "SElevMethod", SElevMethod );
	__command.setCommandParameter ( "STimeZone", STimeZone );
	__command.setCommandParameter ( "STimeZoneOffset", STimeZoneOffset );
	__command.setCommandParameter ( "STimeFormat", STimeFormat );
	__command.setCommandParameter ( "SActiveFlag", SActiveFlag );
	__command.setCommandParameter ( "SLocationType", SLocationType );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "PrintNice", PrintNice );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteTimeSeriesToHydroJSON_Command command )
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
        "<html><b>This command is under development.  The HydroJSON specification is under development.</b></html>." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write time series to a HydroJSON format file, which can be used for website integration." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The HydroJSON format is an open standard documented here: https://github.com/gunnarleffler/hydroJSON" ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for time series data
    int yts = -1;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series Data", ts_JPanel );

    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Specify time series to output and how time series properties should be mapped to HydroJSON elements."),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Enter date/times to a precision appropriate for output time series."),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yts, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    yts = CommandEditorUtil.addTSListToEditorDialogPanel ( this, ts_JPanel, __TSList_JComboBox, yts );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yts = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, ts_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yts );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yts = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, ts_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yts );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ("Output start:"), 
		0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputStart_JTextField = new JTextField (20);
	__OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __OutputStart_JTextField,
		1, yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Optional - override the global output start (default=write all data)."),
		3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Output end:"), 
		0, ++yts, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputEnd_JTextField = new JTextField (20);
	__OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(ts_JPanel, __OutputEnd_JTextField,
		1, yts, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
		"Optional - override the global output end (default=write all data)."),
		3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Missing value:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField ( "", 20 );
    __MissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __MissingValue_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Optional - value to write for missing data (default=initial missing value)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Output precision:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Precision_JTextField = new JTextField ( "", 20 );
    __Precision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __Precision_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Optional - digits after decimal (default=4)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Time series identifier:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TIdentifier_JTextField = new JTextField ( "", 20 );
    __TIdentifier_JTextField.setToolTipText("Time series identifier string, ${Property}, ${ts:Property}.");
    __TIdentifier_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TIdentifier_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Optional - time series identifier (default=alias or TSID)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Hash:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __THash_JTextField = new JTextField ( "", 20 );
    __THash_JTextField.setToolTipText("Time series hash code, ${Property}, ${ts:Property}.");
    __THash_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __THash_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Optional - unique hash code (default=blank)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Quality type:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TQualityType_JTextField = new JTextField ( "", 20 );
    __TQualityType_JTextField.setToolTipText("Time series quality type, ${Property}, ${ts:Property}.");
    __TQualityType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TQualityType_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Optional - quality type (default=blank)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Parameter:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TParameter_JTextField = new JTextField ( "", 20 );
    __TParameter_JTextField.setToolTipText("Time series parameter, ${Property}, ${ts:Property}.");
    __TParameter_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TParameter_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Optional - parameter (default=data type)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Duration:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TDuration_JTextField = new JTextField ( "", 20 );
    __TDuration_JTextField.setToolTipText("Time series duration, ${Property}, ${ts:Property}.");
    __TDuration_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TDuration_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Optional - duration (default=blank)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Interval:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TInterval_JTextField = new JTextField ( "", 20 );
    __TInterval_JTextField.setToolTipText("Time series interval, ${Property}, ${ts:Property}.");
    __TInterval_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TInterval_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Optional - interval (default=time series interval)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(ts_JPanel, new JLabel ( "Units:" ),
        0, ++yts, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TUnits_JTextField = new JTextField ( "", 20 );
    __TUnits_JTextField.setToolTipText("Time series data units, ${Property}, ${ts:Property}.");
    __TUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(ts_JPanel, __TUnits_JTextField,
        1, yts, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(ts_JPanel, new JLabel (
        "Optional - data units (default=from time series)."),
        3, yts, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for station data
    int yStation = -1;
    JPanel station_JPanel = new JPanel();
    station_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Station Data", station_JPanel );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Specify how station properties should be mapped to HydroJSON elements."),
		0, ++yStation, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Stations are determined from the first time series associated with time series."),
		0, ++yStation, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yStation, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station name:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SName_JTextField = new JTextField ( "", 20 );
    __SName_JTextField.setToolTipText("Station name, ${Property}, ${ts:Property}.");
    __SName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SName_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station name (default=time series location ID)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station responsibility:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SResponsibility_JTextField = new JTextField ( "", 20 );
    __SResponsibility_JTextField.setToolTipText("Station name, ${Property}, ${ts:Property}.");
    __SResponsibility_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SResponsibility_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station responsibility (default=blank)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station latitude:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SCoordLatitude_JTextField = new JTextField ( "", 20 );
    __SCoordLatitude_JTextField.setToolTipText("Station coordinate latitude, ${Property}, ${ts:Property}.");
    __SCoordLatitude_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SCoordLatitude_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station latitude (default=null)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station longitude:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SCoordLongitude_JTextField = new JTextField ( "", 20 );
    __SCoordLongitude_JTextField.setToolTipText("Station coordinate longitude, ${Property}, ${ts:Property}.");
    __SCoordLongitude_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SCoordLongitude_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station longitude (default=null)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station coordinate datum:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SCoordDatum_JTextField = new JTextField ( "", 20 );
    __SCoordDatum_JTextField.setToolTipText("Station coordinate datum, ${Property}, ${ts:Property}.");
    __SCoordDatum_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SCoordDatum_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station coordinate datum (default=null)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station HUC:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SHUC_JTextField = new JTextField ( "", 20 );
    __SHUC_JTextField.setToolTipText("Station hydrologic unit code (HUC), ${Property}, ${ts:Property}.");
    __SHUC_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SHUC_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station HUC (default=null)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station elevation:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SElevValue_JTextField = new JTextField ( "", 20 );
    __SElevValue_JTextField.setToolTipText("Station elevation, ${Property}, ${ts:Property}.");
    __SElevValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SElevValue_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station HUC (default=null)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station elevation accuracy:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SElevAccuracy_JTextField = new JTextField ( "", 20 );
    __SElevAccuracy_JTextField.setToolTipText("Station elevation accuracy, ${Property}, ${ts:Property}.");
    __SElevAccuracy_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SElevAccuracy_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station elevation accuracy (default=null)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station elevation datum:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SElevDatum_JTextField = new JTextField ( "", 20 );
    __SElevDatum_JTextField.setToolTipText("Station elevation datum, ${Property}, ${ts:Property}.");
    __SElevDatum_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SElevDatum_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station elevation datum (default=blank)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station elevation method:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SElevMethod_JTextField = new JTextField ( "", 20 );
    __SElevMethod_JTextField.setToolTipText("Station elevation method, ${Property}, ${ts:Property}.");
    __SElevMethod_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SElevMethod_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station elevation method (default=blank)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station time zone:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __STimeZone_JTextField = new JTextField ( "", 20 );
    __STimeZone_JTextField.setToolTipText("Station time zone, ${Property}, ${ts:Property}.");
    __STimeZone_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __STimeZone_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station time zone (default=blank)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station time zone offset:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __STimeZoneOffset_JTextField = new JTextField ( "", 20 );
    __STimeZoneOffset_JTextField.setToolTipText("Station time zone offset, ${Property}, ${ts:Property}.");
    __STimeZoneOffset_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __STimeZoneOffset_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station time zone offset (default=blank)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station time format:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __STimeFormat_JTextField = new JTextField ( "", 20 );
    __STimeFormat_JTextField.setToolTipText("Station time format, ${Property}, ${ts:Property}.");
    __STimeFormat_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __STimeFormat_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - station time format (default=blank)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station active?:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SActiveFlag_JTextField = new JTextField ( "", 20 );
    __SActiveFlag_JTextField.setToolTipText("Station active flag, ${Property}, ${ts:Property}.");
    __SActiveFlag_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SActiveFlag_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - active flag T or F (default=blank)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(station_JPanel, new JLabel ( "Station location type:" ),
        0, ++yStation, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SLocationType_JTextField = new JTextField ( "", 20 );
    __SLocationType_JTextField.setToolTipText("Station location type, ${Property}, ${ts:Property}.");
    __SLocationType_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(station_JPanel, __SLocationType_JTextField,
        1, yStation, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(station_JPanel, new JLabel (
        "Optional - location type (default=blank)."),
        3, yStation, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for output file
    int yFile = -1;
    JPanel file_JPanel = new JPanel();
    file_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output File", file_JPanel );
    
    JGUIUtil.addComponent(file_JPanel, new JLabel (
        "Specify the HydroJSON output file as a full path or relative to the working directory."),
		0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(file_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(file_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yFile, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(file_JPanel, new JLabel ( "JSON file to write:" ), 
		0, ++yFile, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.setToolTipText("Specify the path to the output file or use ${Property} notation");
	__OutputFile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(file_JPanel, __OutputFile_JTextField,
		1, yFile, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(file_JPanel, __browse_JButton,
		6, yFile, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    
    JGUIUtil.addComponent(file_JPanel, new JLabel ( "Print nice?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PrintNice_JComboBox = new SimpleJComboBox ( false );
	__PrintNice_JComboBox.addItem ( "" ); // Default
	__PrintNice_JComboBox.addItem ( __command._False );
	__PrintNice_JComboBox.addItem ( __command._True );
	__PrintNice_JComboBox.addItemListener(this);
   JGUIUtil.addComponent(file_JPanel, __PrintNice_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(file_JPanel, new JLabel(
		"Optional - format to be human-readable (default=" + __command._False + ")"), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        
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

	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton( __RemoveWorkingDirectory, __RemoveWorkingDirectory, this);
		button_JPanel.add ( __path_JButton );
	}
	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", "OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	
	// Refresh the contents...
    checkGUIState();
    refresh ();
    
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
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
@return true if the edits were committed, false if the user canceled.
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
	String EnsembleID = "";
	String OutputStart = "";
	String OutputEnd = "";
	String MissingValue = "";
	String Precision = "";
	String TIdentifier = "";
	String THash = "";
	String TQualityType = "";
	String TParameter = "";
	String TDuration = "";
	String TInterval = "";
	String TUnits = "";
	String SName = "";
	String SResponsibility = "";
	String SCoordLatitude = "";
	String SCoordLongitude = "";
	String SCoordDatum = "";
	String SHUC = "";
	String SElevValue = "";
	String SElevAccuracy = "";
	String SElevDatum = "";
	String SElevMethod = "";
	String STimeZone = "";
	String STimeZoneOffset = "";
	String STimeFormat = "";
	String SActiveFlag = "";
	String SLocationType = "";
	String OutputFile = "";
	String PrintNice = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
		OutputStart = parameters.getValue ( "OutputStart" );
		OutputEnd = parameters.getValue ( "OutputEnd" );
	    MissingValue = parameters.getValue("MissingValue");
	    Precision = parameters.getValue("Precision");
	    TIdentifier = parameters.getValue("TIdentifier");
	    THash = parameters.getValue("THash");
	    TQualityType = parameters.getValue("TQualityType");
	    TParameter = parameters.getValue("TParameter");
	    TDuration = parameters.getValue("TDuration");
	    TInterval = parameters.getValue("TInterval");
	    TUnits = parameters.getValue("TUnits");
	    SName = parameters.getValue("SName");
	    SResponsibility = parameters.getValue("SResponsibility");
	    SCoordLatitude = parameters.getValue("SCoordLatitude");
	    SCoordLongitude = parameters.getValue("SCoordLongitude");
	    SCoordDatum = parameters.getValue("SCoordDatum");
	    SHUC = parameters.getValue("SHUC");
	    SElevValue = parameters.getValue("SElevValue");
	    SElevAccuracy = parameters.getValue("SElevAccuracy");
	    SElevDatum = parameters.getValue("SElevDatum");
		SElevMethod = parameters.getValue("SElevMethod");
		STimeZone = parameters.getValue("STimeZone");
		STimeZoneOffset = parameters.getValue("STimeZoneOffset");
		STimeFormat = parameters.getValue("STimeFormat");
		SActiveFlag = parameters.getValue("SActiveFlag");
		SLocationType = parameters.getValue("SLocationType");
		OutputFile = parameters.getValue ( "OutputFile" );
		PrintNice = parameters.getValue ( "PrintNice" );
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
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText (OutputStart);
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText (OutputEnd);
		}
        if ( MissingValue != null ) {
            __MissingValue_JTextField.setText ( MissingValue );
        }
	    if ( Precision != null ) {
	        __Precision_JTextField.setText ( Precision );
	    }
	    if ( TIdentifier != null ) {
	        __TIdentifier_JTextField.setText ( TIdentifier );
	    }
	    if ( THash != null ) {
	        __THash_JTextField.setText ( THash );
	    }
	    if ( TQualityType != null ) {
	        __TQualityType_JTextField.setText ( TQualityType );
	    }
	    if ( TParameter != null ) {
	        __TParameter_JTextField.setText ( TParameter );
	    }
	    if ( TDuration != null ) {
	        __TDuration_JTextField.setText ( TDuration );
	    }
	    if ( TInterval != null ) {
	        __TInterval_JTextField.setText ( TInterval );
	    }
	    if ( TUnits != null ) {
	        __TUnits_JTextField.setText ( TUnits );
	    }
	    if ( SName != null ) {
	        __SName_JTextField.setText ( SName );
	    }
	    if ( SResponsibility != null ) {
	        __SResponsibility_JTextField.setText ( SResponsibility );
	    }
	    if ( SCoordLatitude != null ) {
	        __SCoordLatitude_JTextField.setText ( SCoordLatitude );
	    }
	    if ( SCoordLongitude != null ) {
	        __SCoordLongitude_JTextField.setText ( SCoordLongitude );
	    }
	    if ( SCoordDatum != null ) {
	        __SCoordDatum_JTextField.setText ( SCoordDatum );
	    }
	    if ( SHUC != null ) {
	        __SHUC_JTextField.setText ( SHUC );
	    }
	    if ( SElevValue != null ) {
	        __SElevValue_JTextField.setText ( SElevValue );
	    }
	    if ( SElevAccuracy != null ) {
	        __SElevAccuracy_JTextField.setText ( SElevAccuracy );
	    }
	    if ( SElevDatum != null ) {
	        __SElevDatum_JTextField.setText ( SElevDatum );
	    }
	    if ( SElevMethod != null ) {
	        __SElevMethod_JTextField.setText ( SElevMethod );
	    }
	    if ( STimeZone != null ) {
	        __STimeZone_JTextField.setText ( STimeZone );
	    }
	    if ( STimeZoneOffset != null ) {
	        __STimeZoneOffset_JTextField.setText ( STimeZoneOffset );
	    }
	    if ( STimeFormat != null ) {
	        __STimeFormat_JTextField.setText ( STimeFormat );
	    }
	    if ( SActiveFlag != null ) {
	        __SActiveFlag_JTextField.setText ( SActiveFlag );
	    }
	    if ( SLocationType != null ) {
	        __SLocationType_JTextField.setText ( SLocationType );
	    }
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText (OutputFile);
		}
        if ( PrintNice == null ) {
            // Select default...
            __PrintNice_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __PrintNice_JComboBox,PrintNice, JGUIUtil.NONE, null, null ) ) {
                __PrintNice_JComboBox.select ( PrintNice );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nPrintNice value \"" + PrintNice +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}
	// Regardless, reset the command from the fields...
	TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
	MissingValue = __MissingValue_JTextField.getText().trim();
	Precision = __Precision_JTextField.getText().trim();
	TIdentifier = __TIdentifier_JTextField.getText().trim();
	THash = __THash_JTextField.getText().trim();
	TQualityType = __TQualityType_JTextField.getText().trim();
	TParameter = __TParameter_JTextField.getText().trim();
	TDuration = __TDuration_JTextField.getText().trim();
	TInterval = __TInterval_JTextField.getText().trim();
	TUnits = __TUnits_JTextField.getText().trim();
	SName = __SName_JTextField.getText().trim();
	SResponsibility = __SResponsibility_JTextField.getText().trim();
	SCoordLatitude = __SCoordLatitude_JTextField.getText().trim();
	SCoordLongitude = __SCoordLongitude_JTextField.getText().trim();
	SCoordDatum = __SCoordDatum_JTextField.getText().trim();
	SHUC = __SHUC_JTextField.getText().trim();
	SElevValue = __SElevValue_JTextField.getText().trim();
	SElevAccuracy = __SElevAccuracy_JTextField.getText().trim();
	SElevDatum = __SElevDatum_JTextField.getText().trim();
	SElevMethod = __SElevMethod_JTextField.getText().trim();
	STimeZone = __STimeZone_JTextField.getText().trim();
	STimeZoneOffset = __STimeZoneOffset_JTextField.getText().trim();
	STimeFormat = __STimeFormat_JTextField.getText().trim();
	SActiveFlag = __SActiveFlag_JTextField.getText().trim();
	SLocationType = __SLocationType_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	PrintNice = __PrintNice_JComboBox.getSelected();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
	parameters.add ( "OutputStart=" + OutputStart );
	parameters.add ( "OutputEnd=" + OutputEnd );
	parameters.add ( "MissingValue=" + MissingValue );
	parameters.add ( "Precision=" + Precision );
	parameters.add ( "TIdentifier=" + TIdentifier );
	parameters.add ( "THash=" + THash );
	parameters.add ( "TQualityType=" + TQualityType );
	parameters.add ( "TParameter=" + TParameter );
	parameters.add ( "TDuration=" + TDuration );
	parameters.add ( "TInterval=" + TInterval );
	parameters.add ( "TUnits=" + TUnits );
	parameters.add ( "SName=" + SName );
	parameters.add ( "SResponsibility=" + SResponsibility );
	parameters.add ( "SCoordLatitude=" + SCoordLatitude );
	parameters.add ( "SCoordLongitude=" + SCoordLongitude );
	parameters.add ( "SCoordDatum=" + SCoordDatum );
	parameters.add ( "SHUC=" + SHUC );
	parameters.add ( "SElevValue=" + SElevValue );
	parameters.add ( "SElevAccuracy=" + SElevAccuracy );
	parameters.add ( "SElevDatum=" + SElevDatum );
	parameters.add ( "SElevMethod=" + SElevMethod );
	parameters.add ( "STimeZone=" + STimeZone );
	parameters.add ( "STimeZoneOffset=" + STimeZoneOffset );
	parameters.add ( "STimeFormat=" + STimeFormat );
	parameters.add ( "SActiveFlag=" + SActiveFlag );
	parameters.add ( "SLocationType=" + SLocationType );
	parameters.add ( "OutputFile=" + OutputFile );
	parameters.add ( "PrintNice=" + PrintNice );
	__command_JTextArea.setText( __command.toString ( parameters ) );
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		if ( __path_JButton != null ) {
			__path_JButton.setEnabled ( false );
		}
	}
	if ( __path_JButton != null ) {
		__path_JButton.setEnabled ( true );
		File f = new File ( OutputFile );
		if ( f.isAbsolute() ) {
			__path_JButton.setText ( __RemoveWorkingDirectory );
		}
		else {
            __path_JButton.setText ( __AddWorkingDirectory );
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