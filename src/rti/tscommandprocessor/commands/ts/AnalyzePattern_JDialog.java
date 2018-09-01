package rti.tscommandprocessor.commands.ts;

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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJList;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class AnalyzePattern_JDialog extends JDialog
	implements ActionListener,
	 	   ItemListener,
	 	   KeyListener,
	 	   ListSelectionListener,
		   MouseListener,
		   WindowListener
{
// Controls are defined in logical order -- The order they appear in the dialog
// box and documentation.
private AnalyzePattern_Command __command = null; // Command object.
private String __working_dir   = null;	// Working directory.
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox	__TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private SimpleJList __Method_SimpleJList = null;
private JTextField __Percentile_JTextField = null;
private JTextField __PatternID_JTextField = null;
private JTextField __OutputFile_JTextField = null;
private JTextField __TableID_JTextField = null;
private TSFormatSpecifiersJPanel __DataRow_JTextField = null;
private SimpleJComboBox __Legacy_JComboBox = null;
private SimpleJButton __browse_JButton = null;
private String __browse_String = "Browse";
private String __browse_Tip = "Open the file browser.";
private JTextArea __Command_JTextArea = null;
private JLabel __Command_JLabel = null;
private JScrollPane __Command_JScrollPane = null;

// Cancel button
private SimpleJButton __cancel_JButton = null;
private String __cancel_String  = "Cancel";

// OK button
private SimpleJButton __ok_JButton = null;
private String __ok_String  = "OK";

private SimpleJButton __help_JButton = null;
						
private JTextField __statusJTextField = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false;

boolean ignoreValueChanged = false; // Used to prevent ValueChange method to execute refresh()

/**
Editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public AnalyzePattern_JDialog ( JFrame parent, AnalyzePattern_Command command )
{
	super( parent, true );
	
	// Initialize the dialog.
	initialize ( parent, command );		 
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{
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
		sff = new SimpleFileFilter("pat","Output file");
		fc.addChoosableFileFilter(sff);
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

	// Cancel button - valid only under the command mode
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "AnalyzePattern");
	}

	// OK button - Active only under the command mode
	else if ( o == __ok_JButton ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}

	else if ( (__TSList_JComboBox != null) && (o == __TSList_JComboBox) ) {
		checkGUIState();
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
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{
	// Get the values from the interface.
	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String Method = (String) __Method_SimpleJList.getSelectedItem();
	String Percentile = __Percentile_JTextField.getText().trim();
	String PatternID = __PatternID_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String TableID = __TableID_JTextField.getText().trim();
	String DataRow = __DataRow_JTextField.getText().trim();
	String Legacy = __Legacy_JComboBox.getSelected();
	
	// Put together the list of parameters to check...
	PropList props = new PropList ( "" );
	if ( TSList != null && TSList.length() > 0 ) {
		props.set( "TSList", TSList );
	}
	if ( TSID != null && TSID.length() > 0 ) {
		props.set( "TSID", TSID );
	}
    if ( EnsembleID != null && EnsembleID.length() > 0 ) {
        props.set( "EnsembleID", EnsembleID );
    }
	if ( Method != null && Method.length() > 0 ) {
		props.set( "Method", Method );
	}
	if ( Percentile != null && Percentile.length() > 0 ) {
		props.set( "Percentile", Percentile );
	}
	if ( PatternID != null && PatternID.length() > 0 ) {
		props.set( "PatternID", PatternID );
	}	
	if ( OutputFile != null && OutputFile.length() > 0 ) {
		props.set( "OutputFile", OutputFile );
	}
    if ( TableID != null && TableID.length() > 0 ) {
        props.set( "TableID", TableID );
    }
    if ( DataRow != null && DataRow.length() > 0 ) {
        props.set( "DataRow", DataRow );
    }
    if ( Legacy != null && Legacy.length() > 0 ) {
        props.set( "Legacy", Legacy );
    }
	
	// Check the list of Command Parameters.
	try {
	    // This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
		__error_wait = false;
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
{
	// Get the values from the interface.
	String TSList = __TSList_JComboBox.getSelected();
	String TSID = __TSID_JComboBox.getSelected();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String Method = (String)__Method_SimpleJList.getSelectedItem();
	String Percentile = __Percentile_JTextField.getText().trim();
	String PatternID = __PatternID_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String TableID = __TableID_JTextField.getText().trim();
	String DataRow = __DataRow_JTextField.getText().trim();
	String Legacy = __Legacy_JComboBox.getSelected();

	// Commit the values to the command object.
	__command.setCommandParameter ( "TSList", TSList );
	__command.setCommandParameter ( "TSID", TSID );
	__command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "Method", Method );
	__command.setCommandParameter ( "Percentile", Percentile );
	__command.setCommandParameter ( "PatternID", PatternID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "TableID", TableID );
	__command.setCommandParameter ( "DataRow", DataRow );
	__command.setCommandParameter ( "Legacy", Legacy );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	__command = null;

	// Time series
	__TSList_JComboBox = null;
	__TSID_JComboBox = null;
	__Method_SimpleJList = null;
	__Percentile_JTextField = null;
	__PatternID_JTextField = null;
	__OutputFile_JTextField = null;

	// Command Buttons
	__browse_JButton = null;
	__cancel_JButton = null;
	__ok_JButton = null;

	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Vector of String containing the command.
*/
private void initialize ( JFrame parent, AnalyzePattern_Command command )
{
	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	
	// GUI Title
	String title = "Edit " + __command.getCommandName() + "() Command";
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	// Top comments
	JGUIUtil.addComponent( main_JPanel,
		new JLabel ( "This command creates the pattern file for use with the FillPattern() command."),
			0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Only monthly time series can be processed." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Example percentiles are .25,.75, with corresponding pattern identifiers DRY,AVG,WET." ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
		
	// Method
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Method:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	List<String> av = new ArrayList<String>();
	av.add( __command._ANALYSIS_PERCENTILE  );
	__Method_SimpleJList = new SimpleJList (av);
	av = null;
	__Method_SimpleJList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
	__Method_SimpleJList.setVisibleRowCount       ( 1 );
	// Make sure to set the flag ignoreValueChanged to false and
	// then back to true when executing the select() methods.
	// Maybe not needed here (modal dialog)
	ignoreValueChanged = true;
	__Method_SimpleJList.select ( 0 );
	ignoreValueChanged = false;
	__Method_SimpleJList.addListSelectionListener ( this );
	__Method_SimpleJList.addKeyListener ( this );
	__Method_SimpleJList.addMouseListener ( this );

    JGUIUtil.addComponent( main_JPanel, new JScrollPane(__Method_SimpleJList),
	1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	// Percentile
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Percentile:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Percentile_JTextField = new JTextField ( 10 );
	__Percentile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Percentile_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Required - comma-separated list of fractions (0 to 1) for cutoffs."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// PatternID
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "PatternID:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PatternID_JTextField = new JTextField ( 10 );
	__PatternID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __PatternID_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - pattern identifiers corresponding to the fractions."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// File to save results.
	JGUIUtil.addComponent(main_JPanel, new JLabel ("Output file:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( __browse_String, this );
	__browse_JButton.setToolTipText( __browse_Tip );
	JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Table ID:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TableID_JTextField = new JTextField ( 10 );
    __TableID_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __TableID_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - identifier for table to create, containing statistics."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel,new JLabel( "Row(s) for data:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DataRow_JTextField = new TSFormatSpecifiersJPanel(10);
    __DataRow_JTextField.setToolTipText("Use %L for location, %T for data type, %I for interval.");
    __DataRow_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __DataRow_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - data row name(s) for 1+ time series."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Legacy behavior:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Legacy_JComboBox = new SimpleJComboBox ( 10, false );
    List<String> legacyChoices = new ArrayList<String>(3);
    legacyChoices.add ( "" );
    legacyChoices.add ( __command._False );
    legacyChoices.add ( __command._True );
    __Legacy_JComboBox.setData ( legacyChoices );
    __Legacy_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Legacy_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - use legacy logic (error with some edge values shifted to lower percentile)."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Command
	__Command_JLabel = new JLabel ( "Command:" );
        JGUIUtil.addComponent(main_JPanel, __Command_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Command_JTextArea = new JTextArea (4,55);
	__Command_JTextArea.setLineWrap ( true );
	__Command_JTextArea.setWrapStyleWord ( true );
	__Command_JTextArea.setEditable ( false );
	__Command_JScrollPane = new JScrollPane( __Command_JTextArea );
	JGUIUtil.addComponent(main_JPanel, __Command_JScrollPane,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
        
	// OK button: used only when running as a TSTool command.
	__ok_JButton = new SimpleJButton(__ok_String, this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );

	// Cancel button: used when running as a command
	__cancel_JButton = new SimpleJButton( __cancel_String, this);
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __cancel_JButton );

	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	// Set up the status bar.
	__statusJTextField = new JTextField();
	__statusJTextField.setEditable(false);
	JPanel statusJPanel = new JPanel();
	statusJPanel.setLayout(new GridBagLayout());
	JGUIUtil.addComponent(statusJPanel, __statusJTextField,
		0, 0, 1, 1, 1, 1,
		GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
	getContentPane().add ( "South", statusJPanel);

	// Visualize it...
	if ( title != null ) {
		setTitle ( title );
	}

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
{   checkGUIState();
	refresh();
}

/**
Respond to KeyEvents.
@param e ItemEvent to handle.
*/
public void keyPressed ( KeyEvent event )
{	
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else {
	    // One of the combo boxes...
		refresh();
	}
}

/**
*/
public void keyReleased ( KeyEvent event )
{	
	refresh();
}

/**
*/
public void keyTyped ( KeyEvent event ) {
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
}

/**
Handle mouse released event.
*/
public void mouseReleased ( MouseEvent event )
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
Refresh the command from the other text field contents:
*/
private void refresh()
{
	String mthd = "analyzePattern_JDialog.refresh", mssg;

	String TSList = "";
	String TSID = "";
	String EnsembleID = "";
	String Method = "";
	String Percentile = "";
	String PatternID = "";
	String OutputFile = "";
	String TableID = "";
	String DataRow = "";
	String Legacy = "";

	__error_wait = false;

	PropList props 	= null;

	if ( __first_time ) {
		
		__first_time = false;
		
		// Get the properties from the command
		props = __command.getCommandParameters();
		TSList = props.getValue ( "TSList" );
		TSID = props.getValue ( "TSID" );
		EnsembleID = props.getValue ( "EnsembleID" );
		Method = props.getValue ( "Method" );
		Percentile = props.getValue ( "Percentile");
		PatternID = props.getValue ( "PatternID" );
		OutputFile = props.getValue ( "OutputFile");
		TableID = props.getValue ( "TableID");
		DataRow = props.getValue ( "DataRow");
		Legacy = props.getValue ( "Legacy");
		
		// Make sure the TSList option is valid
		if ( TSList == null || TSList.equals("") ) {
			// Select default...
			if (__TSList_JComboBox.getItemCount() > 0) {
				__TSList_JComboBox.select ( 0 );
			}
		}
		else {
			if ( JGUIUtil.isSimpleJComboBoxItem(__TSList_JComboBox,
				TSList, JGUIUtil.NONE, null, null ) ) {
				__TSList_JComboBox.select ( TSList );
			}
			else {
				mssg = "Existing command references an invalid\nTSList value \"" + TSList + "\".";
				Message.printWarning ( 1, mthd, mssg );
				this.requestFocus();
				__error_wait = true;
			}
		}

		// TSID
		if ( JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID, JGUIUtil.NONE, null, null ) ) {
			__TSID_JComboBox.select ( TSID );
		}
		else {
		    // Automatically add to the list after the first "*"...
			__TSID_JComboBox.insertItemAt ( TSID, 1 );
			// Select...
			__TSID_JComboBox.select ( TSID );
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
                Message.printWarning ( 1, mthd,
                "Existing command references an invalid\nEnsembleID value \"" + EnsembleID +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
		
		// Check the GUI state to make sure that components are
		// enabled as expected (mainly enable/disable the TSID).  If
		// disabled, the TSID will not be added as a parameter below.
		checkGUIState();
		if ( !__TSID_JComboBox.isEnabled() ) {
			// Not needed because some other method of specifying the time series is being used...
			TSID = null;
		}

		// Check Method and highlight the one that match the command being edited
		if ( Method != null ) {
			int pos = 0;
			if ( (pos = JGUIUtil.indexOf(__Method_SimpleJList,Method, false, true)) >= 0 ) {
				// It in the command and the list...
				__Method_SimpleJList.select(pos);	
			}
			else {
				mssg = "Existing command references a non-existent Method \""+ Method + "\".";
				Message.printWarning ( 1, mthd, mssg );
				this.requestFocus();
				__error_wait = true;
			}
		}
		if ( Percentile != null ) {
			__Percentile_JTextField.setText ( Percentile );
		}
		if ( PatternID != null ) {
			__PatternID_JTextField.setText ( PatternID );
		}
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText ( OutputFile );
		}
        if ( TableID != null ) {
            __TableID_JTextField.setText ( TableID );
        }
        if ( DataRow != null ) {
            __DataRow_JTextField.setText ( DataRow );
        }
        if ( Legacy == null ) {
            // Select default...
            __Legacy_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Legacy_JComboBox,Legacy, JGUIUtil.NONE, null, null ) ) {
                __Legacy_JComboBox.select ( Legacy );
            }
            else {
                Message.printWarning ( 1, mthd,
                "Existing command references an invalid\nLegacy value \"" + Legacy +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	}

	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	props = __command.getCommandParameters();
	TSList = __TSList_JComboBox.getSelected();
	TSID = __TSID_JComboBox.getSelected();
	EnsembleID = __EnsembleID_JComboBox.getSelected();
	Method = (String) __Method_SimpleJList.getSelectedItem();
	Percentile = __Percentile_JTextField.getText().trim();
	PatternID = __PatternID_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	TableID = __TableID_JTextField.getText().trim();
	DataRow = __DataRow_JTextField.getText().trim();
	Legacy = __Legacy_JComboBox.getSelected();
	
	// And set the command properties.
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSList=" + TSList );
	props.add ( "TSID=" + TSID );
	props.add ( "Method=" + Method );
	props.add ( "Percentile=" + Percentile );
	props.add ( "PatternID=" + PatternID );
	props.add ( "OutputFile=" + OutputFile );
	props.add ( "TableID=" + TableID );
	props.add ( "DataRow=" + DataRow );
	props.add ( "Legacy=" + Legacy );
	
	__Command_JTextArea.setText( __command.toString(props) );
}

/**
React to the user response.
@param ok if false, then the edit is cancelled.  If true, the edit is committed
and the dialog is closed.
*/
public void response ( boolean ok )
{	
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
public void windowIconified ( WindowEvent evt ){;}
public void windowOpened ( WindowEvent evt ){;}

}