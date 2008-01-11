package rti.tscommandprocessor.commands.custom;

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
import java.io.File;

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

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.Vector;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the CustomCommand() command.
*/
public class CustomCommand_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, ListSelectionListener, WindowListener
{
    
private final String __AddWorkingDirectory = "Add Working Directory";
private final String __RemoveWorkingDirectory = "Remove Working Directory";

private SimpleJButton	__browse_JButton = null,
            __cancel_JButton = null,// Cancel Button
			__ok_JButton = null,
            __path_JButton = null;
private CustomCommand_Command __command = null;// Command to edit
private JTextArea	__command_JTextArea=null;
private JTextField  __Title_JTextField;
private SimpleJComboBox __CurrentRForecastTSID_JComboBox = null;
private SimpleJComboBox __CurrentNForecastTSID_JComboBox = null;
private SimpleJComboBox __PreviousNForecastTSID_JComboBox = null;
private JTextField  __STPDate_JTextField;
private JTextField  __ChangeCriteria_JTextField;
private JTextField  __ValueCriteria_JTextField;
private JTextField	__AdvanceAnalysisOutputFile_JTextField = null;
private JTextField  __MetricsOutputFile_JTextField = null;
private String      __working_dir = null;   // Working directory.
private boolean		__error_wait = false;
private boolean		__first_time = true;
private boolean     __ok = false;  // Has the users pressed OK to close the dialog.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public CustomCommand_JDialog ( JFrame parent, Command command )
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
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Output Report File to Write");
        SimpleFileFilter sff = new SimpleFileFilter("txt", "Report File");
        fc.addChoosableFileFilter(sff);
        
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
                __AdvanceAnalysisOutputFile_JTextField.setText(path );
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
    else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __ok_JButton ) {
		refresh();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
    else if ( o == __path_JButton ) {
        if (    __path_JButton.getText().equals(__AddWorkingDirectory) ) {
            __AdvanceAnalysisOutputFile_JTextField.setText ( IOUtil.toAbsolutePath(__working_dir,
            __AdvanceAnalysisOutputFile_JTextField.getText() ) );
        }
        else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __AdvanceAnalysisOutputFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                __AdvanceAnalysisOutputFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,
                "CustomCommand_JDialog", "Error converting file to relative path." );
            }
        }
        refresh ();
    }
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList parameters = new PropList ( "" );
    String Title = __Title_JTextField.getText().trim();
    String CurrentRForecastTSID = __CurrentRForecastTSID_JComboBox.getSelected();
    String CurrentNForecastTSID = __CurrentNForecastTSID_JComboBox.getSelected();
    String PreviousNForecastTSID = __PreviousNForecastTSID_JComboBox.getSelected();
    String STPDate = __STPDate_JTextField.getText().trim();
    String ChangeCriteria = __ChangeCriteria_JTextField.getText().trim();
    String ValueCriteria = __ValueCriteria_JTextField.getText().trim();
    String AdvanceAnalysisOutputFile = __AdvanceAnalysisOutputFile_JTextField.getText().trim();
    String MetricsOutputFile = __MetricsOutputFile_JTextField.getText().trim();

    __error_wait = false;

    if ( Title.length() > 0 ) {
        parameters.set ( "Title", Title );
    }
    if ( CurrentRForecastTSID.length() > 0 ) {
        parameters.set ( "CurrentRForecastTSID", CurrentRForecastTSID );
    }
    if ( CurrentNForecastTSID.length() > 0 ) {
        parameters.set ( "CurrentNForecastTSID", CurrentNForecastTSID );
    }
    if ( PreviousNForecastTSID.length() > 0 ) {
        parameters.set ( "PreviousNForecastTSID", PreviousNForecastTSID );
    }
    if ( STPDate.length() > 0 ) {
        parameters.set ( "STPDate", STPDate );
    }
    if ( ChangeCriteria.length() > 0 ) {
        parameters.set ( "ChangeCriteria", ChangeCriteria );
    }
    if ( ValueCriteria.length() > 0 ) {
        parameters.set ( "ValueCriteria", ValueCriteria );
    }
    if ( AdvanceAnalysisOutputFile.length() > 0 ) {
        parameters.set ( "AdvanceAnalysisOutputFile", AdvanceAnalysisOutputFile );
    }
    if ( MetricsOutputFile.length() > 0 ) {
        parameters.set ( "MetricsOutputFile", MetricsOutputFile );
    }
    try {   // This will warn the user...
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
{   String Title = __Title_JTextField.getText().trim();
    String CurrentRForecastTSID = __CurrentRForecastTSID_JComboBox.getSelected();
    String CurrentNForecastTSID = __CurrentNForecastTSID_JComboBox.getSelected();
    String PreviousNForecastTSID = __PreviousNForecastTSID_JComboBox.getSelected();
    String STPDate = __STPDate_JTextField.getText().trim();
    String ChangeCriteria = __ChangeCriteria_JTextField.getText().trim();
    String ValueCriteria = __ValueCriteria_JTextField.getText().trim();
    String AdvanceAnalysisOutputFile = __AdvanceAnalysisOutputFile_JTextField.getText().trim();
    String MetricsOutputFile = __MetricsOutputFile_JTextField.getText().trim();
    
    __command.setCommandParameter ( "Title", Title );
    __command.setCommandParameter ( "CurrentRForecastTSID", CurrentRForecastTSID );
    __command.setCommandParameter ( "CurrentNForecastTSID", CurrentNForecastTSID );
    __command.setCommandParameter ( "PreviousNForecastTSID", PreviousNForecastTSID );
    __command.setCommandParameter ( "STPDate", STPDate );
    __command.setCommandParameter ( "ChangeCriteria", ChangeCriteria  );
    __command.setCommandParameter ( "ValueCriteria", ValueCriteria );
    __command.setCommandParameter ( "AdvanceAnalysisOutputFile", AdvanceAnalysisOutputFile );
    __command.setCommandParameter ( "MetricsOutputFile", MetricsOutputFile );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__cancel_JButton = null;
	__command_JTextArea = null;
	__AdvanceAnalysisOutputFile_JTextField = null;
	__command = null;
	__CurrentRForecastTSID_JComboBox = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, Command command )
{	__command = (CustomCommand_Command)command;
    CommandProcessor processor = __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (	"This is a custom command to create forecast products."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel ( "The working directory is: " + __working_dir ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Title:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Title_JTextField = new JTextField ( "", 50 );
    __Title_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Title_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Title for reports."), 
            3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
 
    Vector tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    if ( tsids == null ) {
        // User will not be able to select anything.
        tsids = new Vector();
    }
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Current R forecast:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CurrentRForecastTSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    __CurrentRForecastTSID_JComboBox.setData ( tsids );
    __CurrentRForecastTSID_JComboBox.addKeyListener ( this );
    __CurrentRForecastTSID_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CurrentRForecastTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Current N forecast:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CurrentNForecastTSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    __CurrentNForecastTSID_JComboBox.setData ( tsids );
    __CurrentNForecastTSID_JComboBox.addKeyListener ( this );
    __CurrentNForecastTSID_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CurrentNForecastTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Previous N forecast:"),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PreviousNForecastTSID_JComboBox = new SimpleJComboBox ( true );    // Allow edit
    __PreviousNForecastTSID_JComboBox.setData ( tsids );
    __PreviousNForecastTSID_JComboBox.addKeyListener ( this );
    __PreviousNForecastTSID_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(main_JPanel, __PreviousNForecastTSID_JComboBox,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "STP date:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __STPDate_JTextField = new JTextField ( "", 10 );
    __STPDate_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __STPDate_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "STP date (a Tuesday)."), 
            3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Percent of change criteria (%):" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ChangeCriteria_JTextField = new JTextField ( "", 10 );
    __ChangeCriteria_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ChangeCriteria_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required (suggest 30)."), 
            3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Absolute value criteria:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ValueCriteria_JTextField = new JTextField ( "", 10 );
    __ValueCriteria_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __ValueCriteria_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Required, specific to location."), 
            3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Advance analysis report file to write:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AdvanceAnalysisOutputFile_JTextField = new JTextField ( 50 );
    __AdvanceAnalysisOutputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __AdvanceAnalysisOutputFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browse_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browse_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Metrics report file to write:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MetricsOutputFile_JTextField = new JTextField ( 50 );
    __MetricsOutputFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __MetricsOutputFile_JTextField,
        1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    __browse_JButton = new SimpleJButton ( "Browse", this );
        JGUIUtil.addComponent(main_JPanel, __browse_JButton,
        6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 5, 65 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();

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
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", this);
	button_JPanel.add ( __ok_JButton );

	// Visualize it...

    setTitle ( "Edit " + __command.getCommandName() + "() command" );
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	// Any change needs to refresh the command...
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
@return true if the edits were committed, false if the user cancelled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String Title = "";
    String CurrentRForecastTSID = "";
    String CurrentNForecastTSID = "";
    String PreviousNForecastTSID = "";
    String STPDate = "";
    String ChangeCriteria = "";
    String ValueCriteria = "";
    String AdvanceAnalysisOutputFile = "";
    String MetricsOutputFile = "";
    PropList parameters = null;      // Parameters as PropList.
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        parameters = __command.getCommandParameters ();
        Title = parameters.getValue("Title");
        CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
        CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
        PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
        STPDate = parameters.getValue ( "STPDate" );
        ChangeCriteria  = parameters.getValue("ChangeCriteria");
        ValueCriteria = parameters.getValue("ValueCriteria");
        AdvanceAnalysisOutputFile = parameters.getValue("AdvanceAnalysisOutputFile");
        MetricsOutputFile = parameters.getValue ( "MetricsOutputFile" );
        if ( Title != null ) {
            __Title_JTextField.setText ( Title );
        }
        // Now select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem(
                __CurrentRForecastTSID_JComboBox, CurrentRForecastTSID, JGUIUtil.NONE, null, null ) ) {
                __CurrentRForecastTSID_JComboBox.select ( CurrentRForecastTSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (CurrentRForecastTSID != null) && (CurrentRForecastTSID.length() > 0) ) {
                __CurrentRForecastTSID_JComboBox.insertItemAt ( CurrentRForecastTSID, 1 );
                // Select...
                __CurrentRForecastTSID_JComboBox.select ( CurrentRForecastTSID );
            }
            else {  // Do not select anything...
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(
                __CurrentNForecastTSID_JComboBox, CurrentNForecastTSID, JGUIUtil.NONE, null, null ) ) {
                __CurrentNForecastTSID_JComboBox.select ( CurrentNForecastTSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (CurrentNForecastTSID != null) && (CurrentNForecastTSID.length() > 0) ) {
                __CurrentNForecastTSID_JComboBox.insertItemAt ( CurrentNForecastTSID, 1 );
                // Select...
                __CurrentNForecastTSID_JComboBox.select ( CurrentNForecastTSID );
            }
            else {  // Do not select anything...
            }
        }
        if ( JGUIUtil.isSimpleJComboBoxItem(
                __PreviousNForecastTSID_JComboBox, PreviousNForecastTSID, JGUIUtil.NONE, null, null ) ) {
                __PreviousNForecastTSID_JComboBox.select ( PreviousNForecastTSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (PreviousNForecastTSID != null) && (PreviousNForecastTSID.length() > 0) ) {
                __PreviousNForecastTSID_JComboBox.insertItemAt ( PreviousNForecastTSID, 1 );
                // Select...
                __PreviousNForecastTSID_JComboBox.select ( PreviousNForecastTSID );
            }
            else {  // Do not select anything...
            }
        }
        if ( STPDate != null ) {
            __STPDate_JTextField.setText ( STPDate );
        }
        if ( ChangeCriteria != null ) {
            __ChangeCriteria_JTextField.setText ( ChangeCriteria );
        }
        if ( ValueCriteria != null ) {
            __ValueCriteria_JTextField.setText ( ValueCriteria );
        }
        if ( AdvanceAnalysisOutputFile != null ) {
            __AdvanceAnalysisOutputFile_JTextField.setText ( AdvanceAnalysisOutputFile );
        }
        if ( MetricsOutputFile != null ) {
            __MetricsOutputFile_JTextField.setText ( MetricsOutputFile );
        }
	}
	// Regardless, reset the command from the fields...
    Title = __Title_JTextField.getText().trim();
    CurrentRForecastTSID = __CurrentRForecastTSID_JComboBox.getSelected();
    CurrentNForecastTSID = __CurrentNForecastTSID_JComboBox.getSelected();
    PreviousNForecastTSID = __PreviousNForecastTSID_JComboBox.getSelected();
    STPDate = __STPDate_JTextField.getText().trim();
    ChangeCriteria = __ChangeCriteria_JTextField.getText().trim();
    ValueCriteria = __ValueCriteria_JTextField.getText().trim();
    AdvanceAnalysisOutputFile = __AdvanceAnalysisOutputFile_JTextField.getText().trim();
    MetricsOutputFile = __MetricsOutputFile_JTextField.getText().trim();
    parameters = new PropList ( __command.getCommandName() );
    parameters.add ( "Title=" + Title );
    parameters.add ( "CurrentRForecastTSID=" + CurrentRForecastTSID );
    parameters.add ( "CurrentNForecastTSID=" + CurrentNForecastTSID );
    parameters.add ( "PreviousNForecastTSID=" + PreviousNForecastTSID );
    parameters.add ( "STPDate=" + STPDate );
    parameters.add ( "ChangeCriteria=" + ChangeCriteria );
    parameters.add ( "ValueCriteria=" + ValueCriteria );
    parameters.add ( "AdvanceAnalysisOutputFile=" + AdvanceAnalysisOutputFile );
    parameters.add ( "MetricsOutputFile=" + MetricsOutputFile  );
    __command_JTextArea.setText( __command.toString ( parameters ) );
    if ( (AdvanceAnalysisOutputFile == null) || (AdvanceAnalysisOutputFile.length() == 0) ) {
        if ( __path_JButton != null ) {
            __path_JButton.setEnabled ( false );
        }
    }
    if ( __path_JButton != null ) {
        __path_JButton.setEnabled ( true );
        File f = new File ( AdvanceAnalysisOutputFile );
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
Handle ListSelectionListener events.
*/
public void valueChanged ( ListSelectionEvent e )
{	refresh ();
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
