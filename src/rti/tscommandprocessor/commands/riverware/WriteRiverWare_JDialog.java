package rti.tscommandprocessor.commands.riverware;

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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
 * Editor for WriteRiverWare command.
 * @author sam
 *
 */
@SuppressWarnings("serial")
public class WriteRiverWare_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
	
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";
	
private SimpleJButton __browse_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJComboBox	__TSList_JComboBox = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __WriteHeaderComments_JComboBox = null;
private JTextField __Units_JTextField = null;
private JTextField __Scale_JTextField = null;
private JTextField __SetUnits_JTextField = null;
private JTextField __SetScale_JTextField = null;
private JTextField __Precision_JTextField = null;
private JTextArea __command_JTextArea=null;
private String __working_dir = null; // Working directory.
private boolean __error_wait = false; // Is there an error that we are waiting to be cleared up or Cancel?
private boolean __first_time = true;
private WriteRiverWare_Command __command = null;
private boolean __ok = false; // Indicates whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteRiverWare_JDialog (	JFrame parent, WriteRiverWare_Command command )
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
		else {	fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle("Select RiverWare Time Series File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("txt", "RiverWare Time Series File");
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
					Message.printWarning ( 1,"WriteRiverWare_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "WriteRiverWare");
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
			__OutputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory ) ) {
			try {
				__OutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "WriteRiverWare_JDialog", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else if ( (__TSList_JComboBox != null) && (o == __TSList_JComboBox) ) {
		checkGUIState();
		refresh ();
	}
}

/**
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{	// If "AllMatchingTSID", enable the list.
	// Otherwise, clear and disable...
	String TSList = __TSList_JComboBox.getSelected();
    if ( TSListType.ALL_MATCHING_TSID.equals(TSList) ||
            TSListType.FIRST_MATCHING_TSID.equals(TSList) ||
            TSListType.LAST_MATCHING_TSID.equals(TSList) ) {
		__TSID_JComboBox.setEnabled(true);
	}
	else {	__TSID_JComboBox.setEnabled(false);
		// Set the the first choice, which is blank...
		__TSID_JComboBox.select ( 0 );
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
    String WriteHeaderComments = __WriteHeaderComments_JComboBox.getSelected();
	String Units = __Units_JTextField.getText().trim();
	String Scale = __Scale_JTextField.getText().trim();
	String SetUnits = __SetUnits_JTextField.getText().trim();
	String SetScale = __SetScale_JTextField.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
	__error_wait = false;
	if ( TSList.length() > 0 ) {
		props.set ( "TSList", TSList );
	}
	if ( TSID.length() > 0 ) {
		props.set ( "TSID", TSID );
	}
	if ( OutputFile.length() > 0 ) {
		props.set ( "OutputFile", OutputFile );
	}
    if ( (WriteHeaderComments != null) && (WriteHeaderComments.length() > 0) ) {
    	props.set ( "WriteHeaderComments", WriteHeaderComments );
    }
	if ( Units.length() > 0 ) {
		props.set ( "Units", Units );
	}
	if ( Scale.length() > 0 ) {
		props.set ( "Scale", Scale );
	}
	if ( SetUnits.length() > 0 ) {
		props.set ( "SetUnits", SetUnits );
	}
	if ( SetScale.length() > 0 ) {
		props.set ( "SetScale", SetScale );
	}
	if ( Precision.length() > 0 ) {
		props.set ( "Precision", Precision );
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
private void commitEdits ()
{	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String WriteHeaderComments = __WriteHeaderComments_JComboBox.getSelected();
	String Units = __Units_JTextField.getText().trim();
	String Scale = __Scale_JTextField.getText().trim();
	String SetUnits = __SetUnits_JTextField.getText().trim();
	String SetScale = __SetScale_JTextField.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "WriteHeaderComments", WriteHeaderComments );
	__command.setCommandParameter ( "Units", Units );
	__command.setCommandParameter ( "Scale", Scale );
	__command.setCommandParameter ( "SetUnits", SetUnits );
	__command.setCommandParameter ( "SetScale", SetScale );
	__command.setCommandParameter ( "Precision", Precision );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteRiverWare_Command command )
{	__command = command;
    CommandProcessor processor = __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );
	
	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write time series to a RiverWare format file," +
		" which can be specified using a full or relative path (relative to the working directory)." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the file name follow the convention ObjectName.SlotName." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time series to process are indicated using the TS list."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If TS list is \"" + TSListType.ALL_MATCHING_TSID + "\", "+
		"pick a single time series, or enter a wildcard time series identifier pattern."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Only the first time series will be written (limitation of file format)."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
   	JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ("TS list:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> tslistChoices = new ArrayList<String>(3);
	tslistChoices.add ( "" + TSListType.ALL_MATCHING_TSID );
	tslistChoices.add ( "" + TSListType.ALL_TS );
	tslistChoices.add ( "" + TSListType.SELECTED_TS );
	__TSList_JComboBox = new SimpleJComboBox(false);
	__TSList_JComboBox.setData ( tslistChoices );
	__TSList_JComboBox.select ( 0 );
	__TSList_JComboBox.addActionListener (this);
	JGUIUtil.addComponent(main_JPanel, __TSList_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"How to get the time series to write (default=AllTS)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Identifier (TSID) to match:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);

	// Allow edits...
        
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
    			(TSCommandProcessor)__command.getCommandProcessor(), __command );

	__TSID_JComboBox = new SimpleJComboBox ( true );
	__TSID_JComboBox.setToolTipText("Specify the time series identifier pattern to match or use ${Property} notation");
	int size = 0;
	if ( tsids == null ) {
		tsids = new Vector<String> ();
	}
	size = tsids.size();
	// Blank for default
	if ( size > 0 ) {
		tsids.add ( 0, "" );
	}
	else {	tsids.add ( "" );
	}
	// Always allow a "*" to let all time series be filled (put at end)...
	tsids.add ( "*" );
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addActionListener ( this );
	__TSID_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "RiverWare file to write:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.setToolTipText("Specify the path to the output file or use ${Property} notation");
	__OutputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
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
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Write header comments?:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WriteHeaderComments_JComboBox = new SimpleJComboBox ( false );
    List<String> writeHeaderCommentsList = new Vector<String>();
    writeHeaderCommentsList.add("");
    writeHeaderCommentsList.add(__command._False);
    writeHeaderCommentsList.add(__command._True);
    __WriteHeaderComments_JComboBox.setData ( writeHeaderCommentsList );
    __WriteHeaderComments_JComboBox.select(0);
    __WriteHeaderComments_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __WriteHeaderComments_JComboBox,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - should header comments be written? (default=" + __command._True + ")."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Units:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Units_JTextField = new JTextField ( "", 20 );
	__Units_JTextField.setToolTipText("Specify the units or use ${Property} notation");
	__Units_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Units_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Default is time series units. Data are not converted."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Scale:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Scale_JTextField = new JTextField ( "", 20 );
	__Scale_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Scale_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - divide values by this to scale (default=1)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Set_units:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetUnits_JTextField = new JTextField ( "", 20 );
	__SetUnits_JTextField.setToolTipText("Specify the set_units or use ${Property} notation.");
	__SetUnits_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SetUnits_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - see RiverWare documentation (default=not output)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Set_scale:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SetScale_JTextField = new JTextField ( "", 20 );
	__SetScale_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SetScale_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - see RiverWare documentation (default=not output)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Precision:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Precision_JTextField = new JTextField ( "", 20 );
	__Precision_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Precision_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Number of digits after decimal (default=4)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 50 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add (__cancel_JButton = new SimpleJButton("Cancel",this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
    pack();
    JGUIUtil.center( this );
	refresh();	// Sets the __path_JButton status
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e)
{
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
	else {	// One of the combo boxes...
		refresh();
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
{	String routine = getClass().getSimpleName() + ".refresh";
	String TSList = "";
	String TSID = "";
	String OutputFile = "";
	String WriteHeaderComments = "";
	String Units = "";
	String Scale = "";
	String SetUnits = "";
	String SetScale = "";
	String Precision = "";
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		TSList = parameters.getValue ( "TSList" );
		TSID = parameters.getValue ( "TSID" );
		OutputFile = parameters.getValue("OutputFile");
		WriteHeaderComments = parameters.getValue ( "WriteHeaderComments" );
		Units = parameters.getValue("Units");
		Scale = parameters.getValue("Scale");
		SetUnits = parameters.getValue("SetUnits");
		SetScale = parameters.getValue("SetScale");
		Precision = parameters.getValue("Precision");
		if ( TSList == null ) {
			// Select default...
			__TSList_JComboBox.select ( "" + TSListType.ALL_TS );
		}
		else {
			if ( JGUIUtil.isSimpleJComboBoxItem(__TSList_JComboBox, TSList, JGUIUtil.NONE, null, null ) ) {
				__TSList_JComboBox.select ( TSList );
			}
			else {
				Message.printWarning ( 1, routine,
				"Existing command references an invalid TSList value \"" + TSList +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
			}
		}
		if (	JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
				JGUIUtil.NONE, null, null ) ) {
				__TSID_JComboBox.select ( TSID );
		}
		else {	// Automatically add to the list after the blank...
			if ( (TSID != null) && (TSID.length() > 0) ) {
				__TSID_JComboBox.insertItemAt ( TSID, 1 );
				// Select...
				__TSID_JComboBox.select ( TSID );
			}
			else {	// Select the blank...
				__TSID_JComboBox.select ( 0 );
			}
		}
		// Check the GUI state to make sure that components are
		// enabled as expected (mainly enable/disable the TSID).  If
		// disabled, the TSID will not be added as a parameter below.
		checkGUIState();
		if ( !__TSID_JComboBox.isEnabled() ) {
			// Not needed because some other method of specifying
			// the time series is being used...
			TSID = null;
		}
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText( OutputFile );
		}
        if ( JGUIUtil.isSimpleJComboBoxItem(__WriteHeaderComments_JComboBox, WriteHeaderComments, JGUIUtil.NONE, null, null ) ) {
            __WriteHeaderComments_JComboBox.select ( WriteHeaderComments );
        }
        else {
            if ( (WriteHeaderComments == null) || WriteHeaderComments.equals("") ) {
                // New command...select the default...
                if ( __WriteHeaderComments_JComboBox.getItemCount() > 0 ) {
                    __WriteHeaderComments_JComboBox.select ( 0 );
                }
            }
            else {
                // Bad user command...
                Message.printWarning ( 1, routine, "Existing command references an invalid "+
                  "WriteHeaderComments parameter \"" + WriteHeaderComments + "\".  Select a different value or Cancel." );
            }
        }
		if ( Units != null ) {
			__Units_JTextField.setText ( Units );
		}
		if ( Scale != null ) {
			__Scale_JTextField.setText ( Scale );
		}
		if ( SetUnits != null ) {
			__SetUnits_JTextField.setText ( SetUnits );
		}
		if ( SetScale != null ) {
			__SetScale_JTextField.setText ( SetScale );
		}
		if ( Precision != null ) {
			__Precision_JTextField.setText ( Precision );
		}
	}
	// Regardless, reset the command from the fields...
	TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
	WriteHeaderComments = __WriteHeaderComments_JComboBox.getSelected();
	Units = __Units_JTextField.getText().trim();
	Scale = __Scale_JTextField.getText().trim();
	SetUnits = __SetUnits_JTextField.getText().trim();
	SetScale = __SetScale_JTextField.getText().trim();
	Precision = __Precision_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
	parameters.add ( "TSID=" + TSID );
	parameters.add ( "OutputFile=" + OutputFile );
    parameters.add ( "WriteHeaderComments=" + WriteHeaderComments );
	parameters.add ( "Units=" + Units );
	parameters.add ( "Scale=" + Scale );
	parameters.add ( "SetUnits=" + SetUnits );
	parameters.add ( "SetScale=" + SetScale );
	parameters.add ( "Precision=" + Precision );
	__command_JTextArea.setText( __command.toString ( parameters ) );
	// Check the path and determine what the label on the path button should
	// be...
	if ( __path_JButton != null ) {
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
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
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

} // end writeRiverWare_JDialog
