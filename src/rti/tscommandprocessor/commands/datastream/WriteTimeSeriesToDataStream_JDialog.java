package rti.tscommandprocessor.commands.datastream;

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
import javax.swing.JTextArea;
import javax.swing.JTextField;

import java.io.File;
import java.util.List;
import java.util.Vector;

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
Command editor dialog for the WriteTimeSeriesToDataStream() command.
*/
public class WriteTimeSeriesToDataStream_JDialog extends JDialog
implements ActionListener, KeyListener, ItemListener, WindowListener
{

private final String __AddWorkingDirectory = "Add Working Directory";
private final String __RemoveWorkingDirectory = "Remove Working Directory";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __path_JButton = null;
private WriteTimeSeriesToDataStream_Command __command = null;
private String __working_dir = null;
private JTextArea __command_JTextArea=null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __Append_JComboBox = null;
private JTextArea __OutputFileHeader_JTextArea = null;
private JTextArea __OutputLineFormat_JTextArea = null;
private JTextArea __OutputFileFooter_JTextArea = null;
private JTextField __Precision_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private SimpleJComboBox	__TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTextField __MissingValue_JTextField = null;// Missing value for output
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;		// Has user pressed OK to close the dialog.

/**
WriteDateValue_JDialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteTimeSeriesToDataStream_JDialog (	JFrame parent, WriteTimeSeriesToDataStream_Command command )
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
		fc.setDialogTitle("Select DateValue Time Series File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("txt", "DateValue Time Series File");
		fc.addChoosableFileFilter(sff);
		sff = new SimpleFileFilter("dv", "DateValue Time Series File");
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
				Message.printWarning ( 1, "WriteDateValue_JDialog", "Error converting file to relative path." );
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
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Append = __Append_JComboBox.getSelected();
	String OutputFileHeader = __OutputFileHeader_JTextArea.getText().trim();
	String OutputLineFormat = __OutputLineFormat_JTextArea.getText().trim();
	String OutputFileFooter = __OutputFileFooter_JTextArea.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
    String MissingValue = __MissingValue_JTextField.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();

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
	if ( OutputFile.length() > 0 ) {
		parameters.set ( "OutputFile", OutputFile );
	}
    if ( Append.length() > 0 ) {
        parameters.set ( "Append", Append );
    }
    if ( OutputFileHeader.length() > 0 ) {
        parameters.set ( "OutputFileHeader", OutputFileHeader );
    }
    if ( OutputLineFormat.length() > 0 ) {
        parameters.set ( "OutputLineFormat", OutputLineFormat );
    }
    if ( OutputFileFooter.length() > 0 ) {
        parameters.set ( "OutputFileFooter", OutputFileFooter );
    }
    if (Precision.length() > 0) {
        parameters.set("Precision", Precision);
    }
    if ( MissingValue.length() > 0 ) {
        parameters.set ( "MissingValue", MissingValue );
    }
	if ( OutputStart.length() > 0 ) {
		parameters.set ( "OutputStart", OutputStart );
	}
	if ( OutputEnd.length() > 0 ) {
		parameters.set ( "OutputEnd", OutputEnd );
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
	String OutputFile = __OutputFile_JTextField.getText().trim();
	String Append = __Append_JComboBox.getSelected();
	String OutputFileHeader = __OutputFileHeader_JTextArea.getText().trim();
	String OutputLineFormat = __OutputLineFormat_JTextArea.getText().trim();
	String OutputFileFooter = __OutputFileFooter_JTextArea.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	String MissingValue = __MissingValue_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "Append", Append );
	__command.setCommandParameter ( "OutputFileHeader", OutputFileHeader );
	__command.setCommandParameter ( "OutputLineFormat", OutputLineFormat );
	__command.setCommandParameter ( "OutputFileFooter", OutputFileFooter );
	__command.setCommandParameter ( "Precision", Precision );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
	__command.setCommandParameter ( "MissingValue", MissingValue );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteTimeSeriesToDataStream_Command command )
{	__command = command;
	CommandProcessor processor = __command.getCommandProcessor();
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write time series to a data stream format file," +
		" which consists of a simple header, data records for each value, and a simple footer." ),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The output filename can be specified using ${Property} notation to utilize global properties."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Enter date/times to a precision appropriate for output time series."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
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

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Data stream file to write:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browse_JButton = new SimpleJButton ( "Browse", this );
    JGUIUtil.addComponent(main_JPanel, __browse_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Append to file?:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> Append_Vector = new Vector<String>();
    Append_Vector.add ( "" );
    Append_Vector.add ( __command._False );
    Append_Vector.add ( __command._True );
    __Append_JComboBox = new SimpleJComboBox(false);
    __Append_JComboBox.setData ( Append_Vector );
    __Append_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(main_JPanel, __Append_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - whether to append to output file (default=" + __command._True + ")."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output file header:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFileHeader_JTextArea = new JTextArea (3,35);
    __OutputFileHeader_JTextArea.setLineWrap ( true );
    __OutputFileHeader_JTextArea.setWrapStyleWord ( true );
    __OutputFileHeader_JTextArea.setToolTipText("Will be inserted at top of file");
    __OutputFileHeader_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__OutputFileHeader_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - content to add at top of output file."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Data line format:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputLineFormat_JTextArea = new JTextArea (3,35);
    __OutputLineFormat_JTextArea.setLineWrap ( true );
    __OutputLineFormat_JTextArea.setWrapStyleWord ( true );
    __OutputLineFormat_JTextArea.setToolTipText("Format used for each output line.");
    __OutputLineFormat_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__OutputLineFormat_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - format for each data line."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output file footer:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFileFooter_JTextArea = new JTextArea (3,35);
    __OutputFileFooter_JTextArea.setLineWrap ( true );
    __OutputFileFooter_JTextArea.setWrapStyleWord ( true );
    __OutputFileFooter_JTextArea.setToolTipText("Will be inserted at top of file");
    __OutputFileFooter_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__OutputFileFooter_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Optional - content to add at bottom of output file."),
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output precision:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Precision_JTextField = new JTextField ( "", 10 );
    __Precision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Precision_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Optional - digits after decimal (default=4)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Missing value:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField ( "", 10 );
    __MissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __MissingValue_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Optional - value to write for missing data (default=initial missing value)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output start:"), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputStart_JTextField = new JTextField (10);
	__OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
		1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - override the global output start (default=write all data)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output end:"), 
		0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputEnd_JTextField = new JTextField (10);
	__OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
		1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Optional - override the global output end (default=write all data)."),
		3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
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
{	String routine = "WriteTimeSeriesToDataStream_JDialog.refresh";
	String OutputFile = "";
	String Append = "";
	String OutputFileHeader = "";
	String OutputLineFormat = "";
	String OutputFileFooter = "";
	String Precision = "";
	String MissingValue = "";
	String OutputStart = "";
	String OutputEnd = "";
	String TSList = "";
    String TSID = "";
    String EnsembleID = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
        TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
		OutputFile = parameters.getValue ( "OutputFile" );
		Append = parameters.getValue ( "Append" );
		OutputFileHeader = parameters.getValue ( "OutputFileHeader" );
		OutputLineFormat = parameters.getValue ( "OutputLineFormat" );
		OutputFileFooter = parameters.getValue ( "OutputFileFooter" );
	    Precision = parameters.getValue("Precision");
	    MissingValue = parameters.getValue("MissingValue");
		OutputStart = parameters.getValue ( "OutputStart" );
		OutputEnd = parameters.getValue ( "OutputEnd" );
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
		if ( OutputFile != null ) {
			__OutputFile_JTextField.setText (OutputFile);
		}
        if ( Append == null ) {
            // Select default...
            __Append_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(__Append_JComboBox, Append, JGUIUtil.NONE, null, null ) ) {
                __Append_JComboBox.select ( Append );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\nAppend value \"" + Append +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
	    if (OutputFileHeader != null) {
	         __OutputFileHeader_JTextArea.setText(OutputFileHeader);
	    }
        if (OutputLineFormat != null) {
            __OutputLineFormat_JTextArea.setText(OutputLineFormat);
       }
        if (OutputFileFooter != null) {
             __OutputFileFooter_JTextArea.setText(OutputFileFooter);
        }
	    if ( Precision != null ) {
	        __Precision_JTextField.setText ( Precision );
	    }
        if ( MissingValue != null ) {
            __MissingValue_JTextField.setText ( MissingValue );
        }
		if ( OutputStart != null ) {
			__OutputStart_JTextField.setText (OutputStart);
		}
		if ( OutputEnd != null ) {
			__OutputEnd_JTextField.setText (OutputEnd);
		}
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
	Append = __Append_JComboBox.getSelected();
	OutputFileHeader = __OutputFileHeader_JTextArea.getText().trim();
	OutputLineFormat = __OutputLineFormat_JTextArea.getText().trim();
	OutputFileFooter = __OutputFileFooter_JTextArea.getText().trim();
	Precision = __Precision_JTextField.getText().trim();
	MissingValue = __MissingValue_JTextField.getText().trim();
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
	parameters.add ( "OutputFile=" + OutputFile );
	parameters.add ( "Append=" + Append );
	parameters.add ( "OutputFileHeader=" + OutputFileHeader );
	parameters.add ( "OutputLineFormat=" + OutputLineFormat );
	parameters.add ( "OutputFileFooter=" + OutputFileFooter );
	parameters.add ( "Precision=" + Precision );
	parameters.add ( "MissingValue=" + MissingValue );
	parameters.add ( "OutputStart=" + OutputStart );
	parameters.add ( "OutputEnd=" + OutputEnd );
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