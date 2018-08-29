package rti.tscommandprocessor.commands.email;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

@SuppressWarnings("serial")
public class SendEmailMessage_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectoryFileInput = "Abs";
private final String __AddWorkingDirectoryFileOutput = "Abs";
private final String __RemoveWorkingDirectoryFileInput = "Rel)";
private final String __RemoveWorkingDirectoryFileOutput = "Rel";

private SimpleJButton __browseInput_JButton = null;
private SimpleJButton __pathInput_JButton = null;
private SimpleJButton __browseMessage_JButton = null;
private SimpleJButton __pathMessage_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JTextField __From_JTextField = null;
private JTextField __To_JTextField = null;
private JTextField __CC_JTextField = null;
private JTextField __BCC_JTextField = null;
private JTextField __Subject_JTextField = null;
private JTextArea __Message_JTextArea = null;
private JTextField __MessageFile_JTextField = null;
private JTextField __AttachmentFiles_JTextField = null;
private SimpleJComboBox __IfNotFound_JComboBox = null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private SendEmailMessage_Command __command = null;
private boolean __ok = false; // Whether the user has pressed OK to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SendEmailMessage_JDialog ( JFrame parent, SendEmailMessage_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __browseInput_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser(__working_dir );
		}
		fc.setDialogTitle( "Select Input File");
		
		if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			String directory = fc.getSelectedFile().getParent();
			String filename = fc.getSelectedFile().getName(); 
			String path = fc.getSelectedFile().getPath(); 
	
			if (filename == null || filename.equals("")) {
				return;
			}
	
			if (path != null) {
				// Convert path to relative path by default.
				try {
					__AttachmentFiles_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"SendEmailMessage_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory);
				refresh();
			}
		}
	}
    if ( o == __browseMessage_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser(last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser(__working_dir );
        }
        fc.setDialogTitle( "Select Message File");
        
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String directory = fc.getSelectedFile().getParent();
            String filename = fc.getSelectedFile().getName(); 
            String path = fc.getSelectedFile().getPath(); 
    
            if (filename == null || filename.equals("")) {
                return;
            }
    
            if (path != null) {
				// Convert path to relative path by default.
				try {
					__MessageFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"SendEmailMessage_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory);
                refresh();
            }
        }
    }
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "SendEmailMessage");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathInput_JButton ) {
		if ( __pathInput_JButton.getText().equals(__AddWorkingDirectoryFileInput) ) {
			__AttachmentFiles_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__AttachmentFiles_JTextField.getText() ) );
		}
		else if ( __pathInput_JButton.getText().equals(__RemoveWorkingDirectoryFileInput) ) {
			try {
                __AttachmentFiles_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __AttachmentFiles_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,"AppendFile_JDialog",
				"Error converting input file name to relative path." );
			}
		}
		refresh ();
	}
    else if ( o == __pathMessage_JButton ) {
        if ( __pathMessage_JButton.getText().equals(__AddWorkingDirectoryFileOutput) ) {
        	__MessageFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__MessageFile_JTextField.getText() ) );
        }
        else if ( __pathMessage_JButton.getText().equals(__RemoveWorkingDirectoryFileOutput) ) {
            try {
            	__MessageFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
            			__MessageFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1,"SendEmailMessage_JDialog",
                "Error converting output file name to relative path." );
            }
        }
        refresh ();
    }
	else {	// Choices...
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
	PropList props = new PropList ( "" );
	String To = __To_JTextField.getText().trim();
	String CC = __CC_JTextField.getText().trim();
	String BCC = __BCC_JTextField.getText().trim();
	String Subject = __Subject_JTextField.getText().trim();
	String Message = __Message_JTextArea.getText().trim();
	String MessageFile = __MessageFile_JTextField.getText().trim();
	String AttachmentFiles = __AttachmentFiles_JTextField.getText().trim();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
	__error_wait = false;
    if ( To.length() > 0 ) {
        props.set ( "To", To );
    }
    if ( CC.length() > 0 ) {
        props.set ( "CC", CC );
    }
    if ( BCC.length() > 0 ) {
        props.set ( "BCC", BCC );
    }
    if ( Subject.length() > 0 ) {
        props.set ( "Subject", Subject );
    }
    if ( Message.length() > 0 ) {
        props.set ( "Message", Message );
    }
    if ( MessageFile.length() > 0 ) {
        props.set ( "MessageFile", MessageFile );
    }
	if ( AttachmentFiles.length() > 0 ) {
		props.set ( "AttachmentFiles", AttachmentFiles );
	}
	if ( IfNotFound.length() > 0 ) {
		props.set ( "IfNotFound", IfNotFound );
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
{	String From = __From_JTextField.getText().trim();
	String To = __To_JTextField.getText().trim();
	String CC = __CC_JTextField.getText().trim();
	String BCC = __BCC_JTextField.getText().trim();
	String Subject = __Subject_JTextField.getText().trim();
	String Message = __Message_JTextArea.getText().trim();
	String MessageFile = __MessageFile_JTextField.getText().trim();
	String AttachmentFiles = __AttachmentFiles_JTextField.getText().trim();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
	__command.setCommandParameter ( "From", From );
	__command.setCommandParameter ( "To", To );
	__command.setCommandParameter ( "CC", CC );
	__command.setCommandParameter ( "BCC", BCC );
	__command.setCommandParameter ( "Subject", Subject );
	__command.setCommandParameter ( "Message", Message );
	__command.setCommandParameter ( "MessageFile", MessageFile );
	__command.setCommandParameter ( "AttachmentFiles", AttachmentFiles );
	__command.setCommandParameter ( "IfNotFound", IfNotFound );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, SendEmailMessage_Command command )
{	__command = command;
	CommandProcessor processor =__command.getCommandProcessor();
	
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"<html><b>This command is under development - do not use in production.</b></html>" ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Send an email message." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The attachment file can be a single file, all files in a folder (*), or all files matching an extension (e.g., *.csv)." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"It is recommended that the file name be relative to the working directory, which is:"),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"    " + __working_dir),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "From:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __From_JTextField = new JTextField ( 40 );
    __From_JTextField.setToolTipText("Recipient email addresses, separated by commas, can include ${Property}");
    __From_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __From_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - from email address"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "To:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __To_JTextField = new JTextField ( 40 );
    __To_JTextField.setToolTipText("Recipient email addresses, separated by commas, can include ${Property}");
    __To_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __To_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - recipient email addresses"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "CC:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CC_JTextField = new JTextField ( 40 );
    __CC_JTextField.setToolTipText("CC email addresses, separated by commas, can include ${Property}");
    __CC_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __CC_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - CC email addresses"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "BCC:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __BCC_JTextField = new JTextField ( 40 );
    __BCC_JTextField.setToolTipText("CC email addresses, separated by commas, can include ${Property}");
    __BCC_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __BCC_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - BCC email addresses"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Subject:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Subject_JTextField = new JTextField ( 40 );
    __Subject_JTextField.setToolTipText("Subject for email, can include ${Property}");
    __Subject_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Subject_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - email subject"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Message:" ),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Message_JTextArea = new JTextArea ( 10, 80 );
    __Message_JTextArea.setToolTipText("Email message, can include ${Property}");
    __Message_JTextArea.setLineWrap ( true );
    __Message_JTextArea.setWrapStyleWord ( true );
    __Message_JTextArea.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Message_JTextArea),
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Required - email message"), 
        3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Message file:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MessageFile_JTextField = new JTextField ( 50 );
	__MessageFile_JTextField.setToolTipText("Specify the message file or use ${Property} notation");
	__MessageFile_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __MessageFile_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseMessage_JButton = new SimpleJButton ( "...", this );
	__browseMessage_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(main_JPanel, __browseMessage_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathMessage_JButton = new SimpleJButton(__RemoveWorkingDirectoryFileInput,this);
	    JGUIUtil.addComponent(main_JPanel, __pathMessage_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}

    JGUIUtil.addComponent(main_JPanel, new JLabel ("Attachment file(s):" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AttachmentFiles_JTextField = new JTextField ( 50 );
	__AttachmentFiles_JTextField.setToolTipText("Specify the attachment file(s) or use ${Property} notation");
	__AttachmentFiles_JTextField.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, __AttachmentFiles_JTextField,
		1, y, 5, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseInput_JButton = new SimpleJButton ( "...", this );
	__browseInput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(main_JPanel, __browseInput_JButton,
		6, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathInput_JButton = new SimpleJButton(__RemoveWorkingDirectoryFileInput,this);
	    JGUIUtil.addComponent(main_JPanel, __pathInput_JButton,
	    	7, y, 1, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
    
   JGUIUtil.addComponent(main_JPanel, new JLabel ( "If not found?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<String>();
	notFoundChoices.add ( "" );	// Default
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfNotFound_JComboBox.setData(notFoundChoices);
	__IfNotFound_JComboBox.select ( 0 );
	__IfNotFound_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(main_JPanel, __IfNotFound_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
		"Optional - action if input file is not found (default=" + __command._Warn + ")"), 
		3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
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
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " command" );
	
	// Refresh the contents...
    refresh ();

    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable...
	setResizable ( false );
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok ()
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
	String From = "";
	String To = "";
	String CC = "";
	String BCC = "";
	String Subject = "";
	String Message0 = "";
	String MessageFile = "";
	String AttachmentFiles = "";
	String IfNotFound = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
        From = parameters.getValue ( "From" );
		To = parameters.getValue ( "To" );
		CC = parameters.getValue ( "CC" );
		BCC = parameters.getValue ( "BCC" );
		Subject = parameters.getValue ( "Subject" );
		Message0 = parameters.getValue ( "Message" );
		MessageFile = parameters.getValue ( "MessageFile" );
		AttachmentFiles = parameters.getValue ( "AttachmentFiles" );
		IfNotFound = parameters.getValue ( "IfNotFound" );
        if ( From != null ) {
            __From_JTextField.setText ( From );
        }
        if ( To != null ) {
            __To_JTextField.setText ( To );
        }
        if ( CC != null ) {
            __CC_JTextField.setText ( CC );
        }
        if ( BCC != null ) {
            __BCC_JTextField.setText ( BCC );
        }
        if ( Subject != null ) {
            __Subject_JTextField.setText ( Subject );
        }
        if ( Message0 != null ) {
            __Message_JTextArea.setText ( Message0 );
        }
		if ( MessageFile != null ) {
			__MessageFile_JTextField.setText ( MessageFile );
		}
		if ( AttachmentFiles != null ) {
			__AttachmentFiles_JTextField.setText ( AttachmentFiles );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfNotFound_JComboBox, IfNotFound,JGUIUtil.NONE, null, null ) ) {
			__IfNotFound_JComboBox.select ( IfNotFound );
		}
		else {
            if ( (IfNotFound == null) ||	IfNotFound.equals("") ) {
				// New command...select the default...
				__IfNotFound_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfNotFound parameter \"" +	IfNotFound +
				"\".  Select a\n value or Cancel." );
			}
		}
	}
	// Regardless, reset the command from the fields.  This is only  visible
	// information that has not been committed in the command.
	From = __From_JTextField.getText().trim();
	To = __To_JTextField.getText().trim();
	CC = __CC_JTextField.getText().trim();
	BCC = __BCC_JTextField.getText().trim();
	Subject = __Subject_JTextField.getText().trim();
	Message0 = __Message_JTextArea.getText().trim();
	MessageFile = __MessageFile_JTextField.getText().trim();
	AttachmentFiles = __AttachmentFiles_JTextField.getText().trim();
	IfNotFound = __IfNotFound_JComboBox.getSelected();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "From=" + From );
	props.add ( "To=" + To );
	props.add ( "CC=" + CC );
	props.add ( "BCC=" + BCC );
	props.add ( "Subject=" + Subject );
	props.add ( "Message=" + Message0 );
	props.add ( "MessageFile=" + MessageFile );
	props.add ( "AttachmentFiles=" + AttachmentFiles );
	props.add ( "IfNotFound=" + IfNotFound );
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( __pathInput_JButton != null ) {
		__pathInput_JButton.setEnabled ( true );
		File f = new File ( MessageFile );
		if ( f.isAbsolute() ) {
			__pathInput_JButton.setText (__RemoveWorkingDirectoryFileInput);
			__pathInput_JButton.setToolTipText("Change path to relative to command file");
		}
		else {
            __pathInput_JButton.setText (__AddWorkingDirectoryFileInput );
            __pathInput_JButton.setToolTipText("Change path to absolute");
		}
	}
    if ( __pathMessage_JButton != null ) {
        __pathMessage_JButton.setEnabled ( true );
        File f = new File ( To );
        if ( f.isAbsolute() ) {
            __pathMessage_JButton.setText (__RemoveWorkingDirectoryFileOutput);
			__pathMessage_JButton.setToolTipText("Change path to relative to command file");
        }
        else {
            __pathMessage_JButton.setText (__AddWorkingDirectoryFileOutput );
            __pathMessage_JButton.setToolTipText("Change path to absolute");
        }
    }
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok )
{	__ok = ok;
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