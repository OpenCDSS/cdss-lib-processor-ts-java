// -----------------------------------------------------------------------------
// analyzePattern_JDialog - Editor for analyzePattern() Command
// -----------------------------------------------------------------------------
// Copyright:	See the COPYRIGHT file.
// -----------------------------------------------------------------------------
// History:
// 2004-04-30	Luiz Teixeira, RTi	Derived from the fillMixedStation class
// 2005-05-23	Luiz Teixeira, RTi	Copied the original class 
//					analyzePattern_JDialog() from TSTool and
//					split the code into the new
//					analyzePattern_JDialog() and
//					analyzePattern_Command().
// 2005-05-24	Luiz Teixeira, RTi	Clean up and documentation
// 2007-02-16	SAM, RTi		Use new CommandProcessor interface.
//					Clean up code based on Eclipse feedback.
// 2007-02-27	SAM, RTi		Update the notes to indicate percentile should
//								be fractions.
// -----------------------------------------------------------------------------
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
import java.util.Vector;

import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJList;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class analyzePattern_JDialog extends JDialog
	implements ActionListener,
	 	   ItemListener,
	 	   KeyListener,
	 	   ListSelectionListener,
		   MouseListener,
		   WindowListener
{
// Controls are defined in logical order -- The order they appear in the dialog
// box and documentation.
private analyzePattern_Command __command = null; // Command object.
private String	__working_dir   = null;	// Working directory.

private SimpleJComboBox	__TSList_JComboBox = null;

private SimpleJComboBox	__TSID_JComboBox   = null;// Field for time series ID
					// Time series controls
private SimpleJList	__Method_SimpleJList    = null;
private JTextField	__Percentile_JTextField = null;
private JTextField	__PatternID_JTextField  = null;
					// Method, Percentile and PatternID
private JTextField	__OutputFile_JTextField	 = null;
					// File to save output
private SimpleJButton	__browse_JButton = null;
private String 		__browse_String  = "Browse";
private String		__browse_Tip     = "Open the file browser.";
					// File browser and related controls
private JTextArea	__Command_JTextArea   = null;
private JLabel		__Command_JLabel      = null;
private JScrollPane	__Command_JScrollPane = null;
					// Command_JTextArea and related controls
// Cancel button
private SimpleJButton	__cancel_JButton = null;
private String 		__cancel_String  = "Cancel";
private String		__cancel_Tip =
	"Close the window, without returning the command.";

// OK button
private SimpleJButton   __ok_JButton = null;
private String 		__ok_String  = "OK";
private String		__ok_Tip =
	"Close the window, returning the command.";
						
private JTextField	__statusJTextField = null;
					// Status bar
private boolean		__error_wait = false;
private boolean		__first_time = true;
private boolean		__ok         = false;

boolean ignoreValueChanged = false;
	// Used to prevent ValueChange method to execute refresh()

/**
analyzePattern_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public analyzePattern_JDialog ( JFrame parent,
				Command command )
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
Check the state of the dialog, disabling/enabling components as appropriate.
*/
private void checkGUIState()
{	// If "AllMatchingTSID", enable the list.
	// Otherwise, clear and disable...
	String selected = __TSList_JComboBox.getSelected();
	if ( selected.equals(__command._AllMatchingTSID) ) {
		__TSID_JComboBox.setEnabled(true);
	}
	else {	__TSID_JComboBox.setEnabled(false);
		// Set the the first choice, which is blank...
		__TSID_JComboBox.select ( 0 );
	}
}

/**
Check the user input for errors and set __error_wait accordingly.
*/
private void checkInput ()
{
	// Get the values from the interface.
	String TSList     = __TSList_JComboBox.getSelected();
	String TSID       = __TSID_JComboBox.getSelected();
	String Method     = (String) __Method_SimpleJList.getSelectedItem();
	String Percentile = __Percentile_JTextField.getText().trim();
	String PatternID  = __PatternID_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();
	
	// Put together the list of parameters to check...
	PropList props = new PropList ( "" );
	// TSList
	if ( TSList != null && TSList.length() > 0 ) {
		props.set( "TSList", TSList );
	}
	// TSID 
	if ( TSID != null && TSID.length() > 0 ) {
		props.set( "TSID", TSID );
	}
	// Method
	if ( Method != null && Method.length() > 0 ) {
		props.set( "Method", Method );
	}
	// Percentile
	if ( Percentile != null && Percentile.length() > 0 ) {
		props.set( "Percentile", Percentile );
	}
	// PatternID
	if ( PatternID != null && PatternID.length() > 0 ) {
		props.set( "PatternID", PatternID );
	}	
	// OutputFile	
	if ( OutputFile != null && OutputFile.length() > 0 ) {
		props.set( "OutputFile", OutputFile );
	}		
	
	// Check the list of Command Parameters.
	try {	// This will warn the user...
		__command.checkCommandParameters ( props, null, 1 );
		__error_wait = false;
	} catch ( Exception e ) {
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
	String TSList     = __TSList_JComboBox.getSelected();
	String TSID       = __TSID_JComboBox.getSelected();
	String Method     = (String) __Method_SimpleJList.getSelectedItem();
	String Percentile = __Percentile_JTextField.getText().trim();
	String PatternID  = __PatternID_JTextField.getText().trim();
	String OutputFile = __OutputFile_JTextField.getText().trim();

	// Commit the values to the command object.
	__command.setCommandParameter ( "TSList",     TSList     );
	__command.setCommandParameter ( "TSID",       TSID       );
	__command.setCommandParameter ( "Method",     Method    );
	__command.setCommandParameter ( "Percentile", Percentile );
	__command.setCommandParameter ( "PatternID",  PatternID  );
	__command.setCommandParameter ( "OutputFile", OutputFile );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{
	__command       = null;

	// Time series
	__TSList_JComboBox	= null;
	__TSID_JComboBox        = null;
	__Method_SimpleJList	= null;
	__Percentile_JTextField	= null;
	__PatternID_JTextField	= null;
	__OutputFile_JTextField	= null;

	// Command Buttons
	__browse_JButton	= null;
	__cancel_JButton       	= null;
	__ok_JButton           	= null;

	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Vector of String containing the command.
*/
private void initialize ( JFrame parent,
			  Command command )
{
	String mthd = "analyzePattern_JDialog.initialize";
	
	__command 	= (analyzePattern_Command) command;
	CommandProcessor processor = __command.getCommandProcessor();
	
	// GUI Title
	String title = "Edit " + __command.getCommandName() + "() Command";
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

	// Top comments
	JGUIUtil.addComponent( main_JPanel,
		new JLabel ( "This command creates the pattern file "
			+ "for use with the fillPattern() command."),
			0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Only monthly time series can be processed." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time series to process are indicated using the TS list."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
       	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"If TS list is \"" + __command._AllMatchingTSID + "\", "+
		"pick a single time series, " +
		"or enter a wildcard time series identifier pattern."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}

	// Vector of options for the TSList (set below)
	Vector tslist_Vector = new Vector();
	tslist_Vector.addElement ( __command._AllTS );
	tslist_Vector.addElement ( __command._SelectedTS );
	tslist_Vector.addElement ( __command._AllMatchingTSID );

	// How to get the time series list to fill.
        JGUIUtil.addComponent(main_JPanel, new JLabel ("TS list:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__TSList_JComboBox = new SimpleJComboBox(false);
	__TSList_JComboBox.setData ( tslist_Vector );
	__TSList_JComboBox.addItemListener (this);
	__TSList_JComboBox.addActionListener (this);
	JGUIUtil.addComponent(main_JPanel, __TSList_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"How to get the time series to analyze."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	// Time series list.
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Identifier (TSID) to match:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	// Allow edits...
	__TSID_JComboBox = new SimpleJComboBox ( true );
	
	Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
			(TSCommandProcessor)__command.getCommandProcessor(), __command );
	
	int size = 0;
	if ( tsids == null ) {
		tsids = new Vector ();
	}
	size = tsids.size();
	// Blank for default
	if ( size > 0 ) {
		tsids.insertElementAt ( "", 0 );
	}
	else {	tsids.addElement ( "" );
	}
	// Always allow a "*" to let all time series be filled (put at end)...
	tsids.addElement ( "*" );
	__TSID_JComboBox.setData ( tsids );
	__TSID_JComboBox.addItemListener ( this );
	__TSID_JComboBox.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __TSID_JComboBox,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
		
	// Method
	JGUIUtil.addComponent(main_JPanel, new JLabel ( "Method:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	Vector av = new Vector();
	av.addElement( __command._ANALYSIS_PERCENTILE  );
	__Method_SimpleJList = new SimpleJList (av);
	av = null;
	__Method_SimpleJList.setSelectionMode(
		ListSelectionModel.SINGLE_SELECTION );
	__Method_SimpleJList.setVisibleRowCount       ( 1 );
	// Make sure to set the flag ignoreValueChanged to false and
	// then back to true when executing the select() methods.
	// Maybe not needed here (modal dialog)
	ignoreValueChanged = true;
	__Method_SimpleJList.select 		      ( 0 );
	ignoreValueChanged = false;
	__Method_SimpleJList.addListSelectionListener ( this );
	__Method_SimpleJList.addKeyListener           ( this );
	__Method_SimpleJList.addMouseListener         ( this );

        JGUIUtil.addComponent( main_JPanel,
        	new JScrollPane(__Method_SimpleJList),
		1, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	// Percentile
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "Percentile:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Percentile_JTextField = new JTextField ( 10 );
	__Percentile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __Percentile_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Comma-separated list of percentiles for cutoffs (0 to 1)."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// PatternID
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "PatternID:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PatternID_JTextField = new JTextField ( 10 );
	__PatternID_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __PatternID_JTextField,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JGUIUtil.addComponent(main_JPanel, new JLabel(
		"The pattern identifiers to use, "
		+ "corresponding to the percentiles."),
		3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// File to save results.
	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Output file:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( __browse_String, this );
	__browse_JButton.setToolTipText( __browse_Tip );
	JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);

	// Command
	__Command_JLabel = new JLabel ( "Command:" );
        JGUIUtil.addComponent(main_JPanel, __Command_JLabel,
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Command_JTextArea = new JTextArea (2,55);
	__Command_JTextArea.setLineWrap ( true );
	__Command_JTextArea.setWrapStyleWord ( true );
	__Command_JTextArea.setEditable ( false );
	__Command_JScrollPane = new JScrollPane( __Command_JTextArea );
	JGUIUtil.addComponent(main_JPanel, __Command_JScrollPane,
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel,
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	// Cancel button: used when running as a command
	__cancel_JButton = new SimpleJButton( __cancel_String, this);
	__cancel_JButton.setToolTipText( __cancel_Tip );
	button_JPanel.add ( __cancel_JButton );

	// OK button: used only when running as a TSTool command.
	__ok_JButton = new SimpleJButton(__ok_String, this);
	__ok_JButton .setToolTipText( __ok_Tip );
	button_JPanel.add ( __ok_JButton );

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
	else {	// One of the combo boxes...
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

	String TSList 	  = "";
	String TSID   	  = "";
	String Method	  = "";
	String Percentile = "";
	String PatternID  = "";
	String OutputFile = "";

	__error_wait = false;

	PropList props 	= null;

	if ( __first_time ) {
		
		__first_time = false;
		
		// Get the properties from the command
		props = __command.getCommandParameters();
		TSList     = props.getValue ( "TSList"    );
		TSID       = props.getValue ( "TSID"      );
		Method     = props.getValue ( "Method"    );
		Percentile = props.getValue ( "Percentile");
		PatternID  = props.getValue ( "PatternID" );
		OutputFile = props.getValue ( "OutputFile");
		
		// Make sure the TSList option is valid
		if ( TSList == null || TSList.equals("") ) {
			// Select default...
			if (__TSList_JComboBox.getItemCount() > 0) {
				__TSList_JComboBox.select ( 0 );
			}
		}
		else {
			if (	JGUIUtil.isSimpleJComboBoxItem(
					__TSList_JComboBox,
				TSList, JGUIUtil.NONE, null, null ) ) {
					__TSList_JComboBox.select (
						TSList );
			}
			else {
				mssg = "Existing command references an "
					+ "invalid\nTSList value \""
					+ TSList + "\".";
				Message.printWarning ( 1, mthd, mssg );
				this.requestFocus();
				__error_wait = true;
			}
		}

		// TSID
		if (	JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
				JGUIUtil.NONE, null, null ) ) {
				__TSID_JComboBox.select ( TSID );
		}
		else {	// Automatically add to the list after the first "*"...
			__TSID_JComboBox.insertItemAt ( TSID, 1 );
			// Select...
			__TSID_JComboBox.select ( TSID );
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

		// Check Method and
		// highlight the one that match the command being edited
		if ( Method != null ) {

			int pos = 0;

				if ( (pos = JGUIUtil.indexOf(
					__Method_SimpleJList,
					Method, false, true)) >= 0 ) {
					// It in the command and the list...
					__Method_SimpleJList.select(pos);	
				} else {
					mssg = "Existing command references"
						+ " a non-existent\n"
						+ " Method \""+ Method + "\".";
					Message.printWarning ( 1, mthd, mssg );
					this.requestFocus();
					__error_wait = true;
				}
		}
		
		// Check Percentile and update the text field
		if ( Percentile == null ) {
			__Percentile_JTextField.setText ( "" );
		} else {
			__Percentile_JTextField.setText ( Percentile );
		}

		// Check PatternID and update the text field
		if ( PatternID == null ) {
			__PatternID_JTextField.setText ( "" );
		} else {
			__PatternID_JTextField.setText ( PatternID );
		}

		// Check OutputFile and update the text field
		if ( OutputFile == null ) {
			__OutputFile_JTextField.setText ( "" );
		} else {
			__OutputFile_JTextField.setText ( OutputFile );
		}	
	}

	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	props = __command.getCommandParameters();
	TSList     = __TSList_JComboBox.getSelected();
	TSID       = __TSID_JComboBox.getSelected();
	Method     = (String) __Method_SimpleJList.getSelectedItem();
	Percentile = __Percentile_JTextField.getText().trim();
	PatternID  = __PatternID_JTextField.getText().trim();
	OutputFile = __OutputFile_JTextField.getText().trim();
	
	// And set the command properties.
	props = new PropList ( __command.getCommandName() );
	props.add ( "TSList="     + TSList     );
	props.add ( "TSID="       + TSID       );
	props.add ( "Method="     + Method     );
	props.add ( "Percentile=" + Percentile );
	props.add ( "PatternID="  + PatternID  );
	props.add ( "OutputFile=" + OutputFile );
	
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
public void windowClosed	( WindowEvent evt ){;}
public void windowDeactivated	( WindowEvent evt ){;}
public void windowDeiconified	( WindowEvent evt ){;}
public void windowIconified	( WindowEvent evt ){;}
public void windowOpened	( WindowEvent evt ){;}

} // end analyzePattern_JDialog