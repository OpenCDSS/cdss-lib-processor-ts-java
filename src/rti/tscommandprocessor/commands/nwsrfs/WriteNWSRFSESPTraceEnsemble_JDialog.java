package rti.tscommandprocessor.commands.nwsrfs;

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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import java.io.File;

import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Editor dialog for WriteNwsrfsEspTraceEnsemble() command.
*/
@SuppressWarnings("serial")
public class WriteNWSRFSESPTraceEnsemble_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{

private final String __RemoveWorkingDirectory = "Rel";
private final String __AddWorkingDirectory = "Abs";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private WriteNWSRFSESPTraceEnsemble_Command	__command = null;
private String __working_dir = null;	// Working directory.
private JTextArea __Command_JTextArea=null;// Command as TextField
private JTextField __OutputFile_JTextField = null;
private JTextField __CarryoverGroup_JTextField = null;
private JTextField __ForecastGroup_JTextField = null;// Field for forecast group
private JTextField __Segment_JTextField = null;// Field for segment ID
private JTextField __SegmentDescription_JTextField = null;	// Field for segment description
private JTextField __Latitude_JTextField = null;// Field for latitude
private JTextField __Longitude_JTextField = null;	// Field for longitude
private JTextField __RFC_JTextField = null;// Field for RFC
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;			

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteNWSRFSESPTraceEnsemble_JDialog ( JFrame parent, Command command )
{
	super(parent, true);
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
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select NWSRFS ESP Trace Ensemble File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("CS", "ESP Conditional Trace Ensemble File");
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
					Message.printWarning ( 1,"WriteNWSRFSESPTraceEnsemble_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory );
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response(false);
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "WriteNWSRFSESPTraceEnsemble");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response(true);
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__OutputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
                __OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,__OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "writeNWSRFSESPTraceEnsemble_JDialog", "Error converting file to relative path." );
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
    PropList props = new PropList ( "" );
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String CarryoverGroup = __CarryoverGroup_JTextField.getText().trim();
    String ForecastGroup = __ForecastGroup_JTextField.getText().trim();
    String Segment = __Segment_JTextField.getText().trim();
    String SegmentDescription = __SegmentDescription_JTextField.getText().trim();
    String Latitude = __Latitude_JTextField.getText().trim();
    String Longitude = __Longitude_JTextField.getText().trim();
    String RFC = __RFC_JTextField.getText().trim();
    String TSList = __TSList_JComboBox.getSelected().trim();
    String EnsembleID = __EnsembleID_JComboBox.getSelected().trim();
    __error_wait = false;

    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( CarryoverGroup.length() > 0 ) {
        props.set ( "CarryoverGroup", CarryoverGroup );
    }
    if ( ForecastGroup.length() > 0 ) {
        props.set ( "ForecastGroup", ForecastGroup );
    }
    if ( Segment.length() > 0 ) {
        props.set ( "Segment", Segment );
    }
    if ( SegmentDescription.length() > 0 ) {
        props.set ( "SegmentDescription", SegmentDescription );
    }
    if ( Latitude.length() > 0 ) {
        props.set ( "Latitude", Latitude );
    }
    if ( Longitude.length() > 0 ) {
        props.set ( "Longitude", Longitude );
    }
    if ( RFC.length() > 0 ) {
        props.set ( "RFC", RFC );
    }
    if ( TSList.length() > 0 ) {
        props.set ( "TSList", TSList );
    }
    if ( EnsembleID.length() > 0 ) {
        props.set ( "EnsembleID", EnsembleID );
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
private void commitEdits()
{
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String CarryoverGroup = __CarryoverGroup_JTextField.getText().trim();
	String ForecastGroup = __ForecastGroup_JTextField.getText().trim();
	String Segment = __Segment_JTextField.getText().trim();
	String SegmentDescription = __SegmentDescription_JTextField.getText().trim();
	String Latitude = __Latitude_JTextField.getText().trim();
	String Longitude = __Longitude_JTextField.getText().trim();
	String RFC = __RFC_JTextField.getText().trim();
	String TSList = __TSList_JComboBox.getSelected().trim();
    String EnsembleID = __EnsembleID_JComboBox.getSelected().trim();

	__command.setCommandParameter("OutputFile", OutputFile);
	__command.setCommandParameter("CarryoverGroup", CarryoverGroup);
	__command.setCommandParameter("ForecastGroup", ForecastGroup);
	__command.setCommandParameter("Segment", Segment);
	__command.setCommandParameter("SegmentDescription", SegmentDescription);
	__command.setCommandParameter("Latitude", Latitude);
	__command.setCommandParameter("Longitude", Longitude);
	__command.setCommandParameter("RFC", RFC);
	__command.setCommandParameter("TSList", TSList);
    __command.setCommandParameter("EnsembleID", EnsembleID);
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__Command_JTextArea = null;
	__OutputFile_JTextField = null;
	__CarryoverGroup_JTextField = null;
	__ForecastGroup_JTextField = null;
	__Segment_JTextField = null;
	__SegmentDescription_JTextField = null;
	__Latitude_JTextField = null;
	__Longitude_JTextField = null;
	__RFC_JTextField = null;
	__TSList_JComboBox = null;
	__ok_JButton = null;
	__path_JButton = null;
	__browse_JButton = null;
	__working_dir = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (WriteNWSRFSESPTraceEnsemble_Command)command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command writes time series to an NWSRFS ESP trace ensemble file."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Currently, only conditional simulation (CS) trace files can be written."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If a time series list is specified (NOT using an ensemble ID), the time series to write " +
        "must be traces having a consistent period, and the sequence numbers must be defined."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The file can be specified using a full or relative path (relative to the working directory)."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (	"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"To be understood by the NWS ESPADP program, the file name should adhere to the format:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"   Segment.Location.DataType.HH.CS"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"where the DataType is an NWSRFS data type and the interval HH is padded with zeros."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "File to write:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(__RemoveWorkingDirectory,this);
	    JGUIUtil.addComponent(main_JPanel, __path_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Carryover group:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__CarryoverGroup_JTextField = new JTextField(10);
	__CarryoverGroup_JTextField.addKeyListener(this);
	JGUIUtil.addComponent(main_JPanel, __CarryoverGroup_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (	"Optional - carryover group."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Forecast group:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__ForecastGroup_JTextField = new JTextField(10);
	__ForecastGroup_JTextField.addKeyListener(this);
	JGUIUtil.addComponent(main_JPanel, __ForecastGroup_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - forecast group."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Segment:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Segment_JTextField = new JTextField(10);
	__Segment_JTextField.addKeyListener(this);
	JGUIUtil.addComponent(main_JPanel, __Segment_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - segment (default is 1st part of file name)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Segment description:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SegmentDescription_JTextField = new JTextField(10);
	__SegmentDescription_JTextField.addKeyListener(this);
	JGUIUtil.addComponent(main_JPanel, __SegmentDescription_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - segment description (default is from first time series)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

     JGUIUtil.addComponent(main_JPanel, new JLabel ("Latitude:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Latitude_JTextField = new JTextField(10);
	__Latitude_JTextField.addKeyListener(this);
	JGUIUtil.addComponent(main_JPanel, __Latitude_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - Latitude, decimal degrees."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Longitude:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Longitude_JTextField = new JTextField(10);
	__Longitude_JTextField.addKeyListener(this);
	JGUIUtil.addComponent(main_JPanel, __Longitude_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - Longitude, decimal degrees."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("RFC:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RFC_JTextField = new JTextField(10);
	__RFC_JTextField.addKeyListener(this);
	JGUIUtil.addComponent(main_JPanel, __RFC_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - River Forecast Center abbreviation."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("TS list:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> tslist_Vector = new Vector<String>();
	tslist_Vector.add ( "" );
	tslist_Vector.add ( TSListType.ALL_TS.toString() );
    tslist_Vector.add ( TSListType.ENSEMBLE_ID.toString() );
    tslist_Vector.add ( TSListType.SELECTED_TS.toString() );
	__TSList_JComboBox = new SimpleJComboBox(false);
	__TSList_JComboBox.setData ( tslist_Vector );
	__TSList_JComboBox.addItemListener (this);
	JGUIUtil.addComponent(main_JPanel, __TSList_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - indicates the time series to output (default=" + TSListType.ALL_TS + ")."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

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

	__ok_JButton = new SimpleJButton("OK", "OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

    setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
    // Refresh the contents...
    checkGUIState();
	refresh();	// Sets the __path_JButton status
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
			response(true);
		}
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event )
{	refresh();
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok() {
	return __ok;
}

/**
Refresh the command from the other text field contents.  The command is of the
form:
<pre>
writeNWSRFSESPTraceEnsemble(OutputFile="X",CarryoverGroup="X",
ForecastGroup="X",Segment="X",SegmentDescription="X",Latitude=X,Longitude=X,
RFC="X",TSList="X")
</pre>
*/
private void refresh ()
{	String routine = "writeNWSRFSESPTraceEnsemble_JDialog.refresh";
	String OutputFile = "";
	String CarryoverGroup = "";
	String ForecastGroup = "";
	String Segment = "";
	String SegmentDescription = "";
	String Latitude = "";
	String Longitude = "";
	String RFC = "";
	String TSList = "";
    String EnsembleID = "";
	PropList props = null;
	__error_wait = false;
	if ( __first_time ) {
		__first_time = false;
		props = __command.getCommandParameters();
		OutputFile = props.getValue("OutputFile");
		CarryoverGroup = props.getValue("CarryoverGroup");
		ForecastGroup = props.getValue("ForecastGroup");
		Segment = props.getValue("Segment");
		SegmentDescription = props.getValue("SegmentDescription");
		Latitude = props.getValue("Latitude");
		Longitude = props.getValue("Longitude");
		RFC = props.getValue("RFC");
		TSList = props.getValue("TSList");
        EnsembleID = props.getValue ( "EnsembleID" );
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText ( OutputFile );
		}
		if ( CarryoverGroup != null ) {
			__CarryoverGroup_JTextField.setText ( CarryoverGroup );
		}
		if ( ForecastGroup != null ) {
			__ForecastGroup_JTextField.setText ( ForecastGroup );
		}
		if ( Segment != null ) {
			__Segment_JTextField.setText ( Segment );
		}
		if ( SegmentDescription != null ) {
			__SegmentDescription_JTextField.setText (
				SegmentDescription );
		}
		if ( Latitude != null ) {
			__Latitude_JTextField.setText ( Latitude );
		}
		if ( Longitude != null ) {
			__Longitude_JTextField.setText ( Longitude );
		}
		if ( RFC != null ) {
			__RFC_JTextField.setText ( RFC );
		}
		if ( TSList == null ) {
			// Select default...
			__TSList_JComboBox.select ( 0 );
		}
		else {	if (	JGUIUtil.isSimpleJComboBoxItem(
				__TSList_JComboBox,
				TSList, JGUIUtil.NONE, null, null ) ) {
				__TSList_JComboBox.select ( TSList );
			}
			else {	Message.printWarning ( 1, routine,
				"Existing writeDateValue() " +
				"references an invalid\nTSList value \"" +
				TSList +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
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
	}
	// Regardless, reset the command from the fields...
	OutputFile = __OutputFile_JTextField.getText().trim();
	CarryoverGroup = __CarryoverGroup_JTextField.getText().trim();
	ForecastGroup = __ForecastGroup_JTextField.getText().trim();
	Segment = __Segment_JTextField.getText().trim();
	SegmentDescription = __SegmentDescription_JTextField.getText().trim();
	Latitude = __Latitude_JTextField.getText().trim();
	Longitude = __Longitude_JTextField.getText().trim();
	RFC = __RFC_JTextField.getText().trim();
	TSList = __TSList_JComboBox.getSelected().trim();
    EnsembleID = __EnsembleID_JComboBox.getSelected();

	props = new PropList(__command.getCommandName());
	props.add("OutputFile=" + OutputFile);
	props.add("CarryoverGroup=" + CarryoverGroup);
	props.add("ForecastGroup=" + ForecastGroup);
	props.add("Segment=" + Segment);
	props.add("SegmentDescription=" + SegmentDescription);
	props.add("Latitude=" + Latitude);
	props.add("Longitude=" + Longitude);
	props.add("RFC=" + RFC);
	props.add("TSList=" + TSList);
    props.add("EnsembleID=" + EnsembleID );
	
	__Command_JTextArea.setText(__command.toString(props));

	refreshPathControl();
}

/**
Refresh the PathControl text based on the contents of the input text field contents.
*/
private void refreshPathControl()
{
	String InputFile = __OutputFile_JTextField.getText().trim();
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
			__path_JButton.setText( __RemoveWorkingDirectory);
			__path_JButton.setToolTipText("Change path to relative to command file");
		}
		else {	
			__path_JButton.setText(__AddWorkingDirectory);
			__path_JButton.setToolTipText("Change path to absolute");
		}
	}
}

/**
Return the time series command as a Vector of String.
@return returns the command text or null if no command.
*/
public void response (boolean ok) {
	__ok = ok;
	if (ok) {
		// commit the changes
		commitEdits();
		if (__error_wait) {
			// not ready to close
			return;
		}
	}
	// Now close
	setVisible(false);
	dispose();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event )
{	response(false);
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

} // end writeNWSRFSESPTraceEnsemble_JDialog
