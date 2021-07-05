// SendEmailMessage_JDialog - editor for SendEmailMessage command

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
import javax.swing.JTabbedPane;
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
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browseInput_JButton = null;
private SimpleJButton __pathInput_JButton = null;
private SimpleJButton __browseMessage_JButton = null;
private SimpleJButton __pathMessage_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __MailProgram_JComboBox = null;
private JTextField __From_JTextField = null;
private JTextField __To_JTextField = null;
private JTextField __CC_JTextField = null;
private JTextField __BCC_JTextField = null;
private JTextField __SMTPServer_JTextField = null;
private JTextField __SMTPAccount_JTextField = null;
private JTextField __SMTPPassword_JTextField = null;
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
		if ( __pathInput_JButton.getText().equals(__AddWorkingDirectory) ) {
			__AttachmentFiles_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__AttachmentFiles_JTextField.getText() ) );
		}
		else if ( __pathInput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
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
        if ( __pathMessage_JButton.getText().equals(__AddWorkingDirectory) ) {
        	__MessageFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__MessageFile_JTextField.getText() ) );
        }
        else if ( __pathMessage_JButton.getText().equals(__RemoveWorkingDirectory) ) {
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
	String MailProgram = __MailProgram_JComboBox.getSelected();
	String To = __To_JTextField.getText().trim();
	String CC = __CC_JTextField.getText().trim();
	String BCC = __BCC_JTextField.getText().trim();
	String Subject = __Subject_JTextField.getText().trim();
	String Message = __Message_JTextArea.getText().trim();
	String MessageFile = __MessageFile_JTextField.getText().trim();
	String AttachmentFiles = __AttachmentFiles_JTextField.getText().trim();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
	String SMTPServer = __SMTPServer_JTextField.getText().trim();
	String SMTPAccount = __SMTPAccount_JTextField.getText().trim();
	String SMTPPassword = __SMTPPassword_JTextField.getText().trim();
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
	if ( SMTPServer.length() > 0 ) {
		props.set( "SMTPServer", SMTPServer );
	}
	if ( MailProgram.length() > 0 ) {
		props.set( "MailProgram", MailProgram );
	}
	if ( SMTPAccount.length() > 0 ) {
		props.set( "SMTPAccount", SMTPAccount );
	}
	if ( SMTPPassword.length() > 0 ) {
		props.set( "SMTPPassword", SMTPPassword );
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
{	String MailProgram = __MailProgram_JComboBox.getSelected();
	String From = __From_JTextField.getText().trim();
	String To = __To_JTextField.getText().trim();
	String CC = __CC_JTextField.getText().trim();
	String BCC = __BCC_JTextField.getText().trim();
	String Subject = __Subject_JTextField.getText().trim();
	String Message = __Message_JTextArea.getText().trim();
	String MessageFile = __MessageFile_JTextField.getText().trim();
	String AttachmentFiles = __AttachmentFiles_JTextField.getText().trim();
	String IfNotFound = __IfNotFound_JComboBox.getSelected();
	String SMTPServer = __SMTPServer_JTextField.getText().trim();
	String SMTPAccount = __SMTPAccount_JTextField.getText().trim();
	String SMTPPassword = __SMTPPassword_JTextField.getText().trim();
	__command.setCommandParameter ( "MailProgram", MailProgram );
	__command.setCommandParameter ( "From", From );
	__command.setCommandParameter ( "To", To );
	__command.setCommandParameter ( "CC", CC );
	__command.setCommandParameter ( "BCC", BCC );
	__command.setCommandParameter ( "Subject", Subject );
	__command.setCommandParameter ( "Message", Message );
	__command.setCommandParameter ( "MessageFile", MessageFile );
	__command.setCommandParameter ( "AttachmentFiles", AttachmentFiles );
	__command.setCommandParameter ( "IfNotFound", IfNotFound );
	__command.setCommandParameter ( "SMTPServer", SMTPServer );
	__command.setCommandParameter ( "SMTPAccount", SMTPAccount );
	__command.setCommandParameter ( "SMTPPassword", SMTPPassword );
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
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for Message
    int yMessage = -1;
    JPanel message_JPanel = new JPanel();
    message_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Message", message_JPanel );
    
    JGUIUtil.addComponent(message_JPanel, new JLabel ( "Mail Program:"),
		0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MailProgram_JComboBox = new SimpleJComboBox ( false );
	List<String> mailProgramChoices = new ArrayList<String>();
	mailProgramChoices.add ( "" );	// Default
	mailProgramChoices.add ( __command._JavaAPI );
	mailProgramChoices.add ( __command._Sendmail );
	mailProgramChoices.add ( __command._WindowsMail );
	__MailProgram_JComboBox.setData(mailProgramChoices);
	__MailProgram_JComboBox.select ( 0 );
	__MailProgram_JComboBox.addActionListener ( this );
   JGUIUtil.addComponent(message_JPanel, __MailProgram_JComboBox,
		1, yMessage, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(message_JPanel, new JLabel(
		"Required - method for sending the email message (default=" + __command._JavaAPI + ")"), 
		3, yMessage, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(message_JPanel, new JLabel ( "From:"),
        0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __From_JTextField = new JTextField ( 40 );
    __From_JTextField.setToolTipText("Recipient email addresses, separated by commas, can include ${Property}");
    __From_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(message_JPanel, __From_JTextField,
        1, yMessage, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(message_JPanel, new JLabel(
        "Required - from email address"), 
        3, yMessage, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(message_JPanel, new JLabel ( "To:"),
        0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __To_JTextField = new JTextField ( 40 );
    __To_JTextField.setToolTipText("Recipient email addresses, separated by commas, can include ${Property}");
    __To_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(message_JPanel, __To_JTextField,
        1, yMessage, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(message_JPanel, new JLabel(
        "Required - recipient email addresses"), 
        3, yMessage, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(message_JPanel, new JLabel ( "CC:"),
        0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CC_JTextField = new JTextField ( 40 );
    __CC_JTextField.setToolTipText("CC email addresses, separated by commas, can include ${Property}");
    __CC_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(message_JPanel, __CC_JTextField,
        1, yMessage, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(message_JPanel, new JLabel(
        "Optional - CC email addresses"), 
        3, yMessage, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(message_JPanel, new JLabel ( "BCC:"),
        0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __BCC_JTextField = new JTextField ( 40 );
    __BCC_JTextField.setToolTipText("CC email addresses, separated by commas, can include ${Property}");
    __BCC_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(message_JPanel, __BCC_JTextField,
        1, yMessage, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(message_JPanel, new JLabel(
        "Optional - BCC email addresses"), 
        3, yMessage, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(message_JPanel, new JLabel ( "Subject:"),
        0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Subject_JTextField = new JTextField ( 40 );
    __Subject_JTextField.setToolTipText("Subject for email, can include ${Property}");
    __Subject_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(message_JPanel, __Subject_JTextField,
        1, yMessage, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(message_JPanel, new JLabel(
        "Required - email subject"), 
        3, yMessage, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(message_JPanel, new JLabel ( "Message:" ),
        0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Message_JTextArea = new JTextArea ( 10, 80 );
    __Message_JTextArea.setToolTipText("Email message, can include ${Property}");
    __Message_JTextArea.setLineWrap ( true );
    __Message_JTextArea.setWrapStyleWord ( true );
    __Message_JTextArea.addKeyListener ( this );
    JGUIUtil.addComponent(message_JPanel, new JScrollPane(__Message_JTextArea),
        1, yMessage, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
    JGUIUtil.addComponent(message_JPanel, new JLabel(
        "Required - email message"), 
        3, yMessage, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(message_JPanel, new JLabel ("Message file:" ), 
		0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__MessageFile_JTextField = new JTextField ( 50 );
	__MessageFile_JTextField.setToolTipText("Specify the message file or use ${Property} notation");
	__MessageFile_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel MessageFile_JPanel = new JPanel();
	MessageFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(MessageFile_JPanel, __MessageFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseMessage_JButton = new SimpleJButton ( "...", this );
	__browseMessage_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(MessageFile_JPanel, __browseMessage_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathMessage_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(MessageFile_JPanel, __pathMessage_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(message_JPanel, MessageFile_JPanel,
		1, yMessage, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(message_JPanel, new JLabel ("Attachment file(s):" ), 
		0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__AttachmentFiles_JTextField = new JTextField ( 50 );
	__AttachmentFiles_JTextField.setToolTipText("Specify the attachment file(s) or use ${Property} notation");
	__AttachmentFiles_JTextField.addKeyListener ( this );
    // Input file layout fights back with other rows so put in its own panel
	JPanel AttachmentFiles_JPanel = new JPanel();
	AttachmentFiles_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(AttachmentFiles_JPanel, __AttachmentFiles_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	__browseInput_JButton = new SimpleJButton ( "...", this );
	__browseInput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(AttachmentFiles_JPanel, __browseInput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathInput_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(AttachmentFiles_JPanel, __pathInput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
	JGUIUtil.addComponent(message_JPanel, AttachmentFiles_JPanel,
		1, yMessage, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
   JGUIUtil.addComponent(message_JPanel, new JLabel ( "If not found?:"),
		0, ++yMessage, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfNotFound_JComboBox = new SimpleJComboBox ( false );
	List<String> notFoundChoices = new ArrayList<String>();
	notFoundChoices.add ( "" );	// Default
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfNotFound_JComboBox.setData(notFoundChoices);
	__IfNotFound_JComboBox.select ( 0 );
	__IfNotFound_JComboBox.addActionListener ( this );
	JGUIUtil.addComponent(message_JPanel, __IfNotFound_JComboBox,
		1, yMessage, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(message_JPanel, new JLabel(
		"Optional - action if input file is not found (default=" + __command._Warn + ")"), 
		3, yMessage, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // Panel for JavaAPI
    int yJavaAPI = -1;
    JPanel javaAPI_JPanel = new JPanel();
    javaAPI_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "JavaAPI", javaAPI_JPanel );
    
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel (
    		"<html><b>The JavaAPI functionality has not yet been fully implemented.</b></html>"),
    		0, ++yJavaAPI, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel (
    		"An SMTP server is needed along with the account through which to send the email. For example:"),
    		0, ++yJavaAPI, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel (
    		"    smtp.gmail.com          - Gmail"),
    		0, ++yJavaAPI, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel (
    		"    smtp.live.com             - Outlook"),
    		0, ++yJavaAPI, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel (
    		"    smtp.mail.yahoo.com - Yahoo"),
    		0, ++yJavaAPI, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel ( "SMTP Server:" ), 
        0, ++yJavaAPI, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SMTPServer_JTextField = new JTextField ( "", 40 );
	__SMTPServer_JTextField.setToolTipText("Set the SMTP server to connect to.");
	__SMTPServer_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(javaAPI_JPanel, __SMTPServer_JTextField,
        1, yJavaAPI, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel(
        "Required - the SMTP server to connect to"), 
        3, yJavaAPI, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel ( "SMTP Account:" ), 
        0, ++yJavaAPI, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SMTPAccount_JTextField = new JTextField ( "", 40 );
    __SMTPAccount_JTextField.setToolTipText("Set the SMTP account username / ID.");
    __SMTPAccount_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(javaAPI_JPanel, __SMTPAccount_JTextField,
        1, yJavaAPI, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel(
        "<html>Required - the SMTP account to connect to the server. <b>Not yet implemented.</b></html>"), 
        3, yJavaAPI, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel ( "SMTP Password:" ), 
        0, ++yJavaAPI, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SMTPPassword_JTextField = new JTextField ( "", 40 );
    __SMTPPassword_JTextField.setToolTipText("Set the SMTP account password for authentication.");
    __SMTPPassword_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(javaAPI_JPanel, __SMTPPassword_JTextField,
        1, yJavaAPI, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(javaAPI_JPanel, new JLabel(
        "Required - the SMTP password for authentication"), 
        3, yJavaAPI, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    // PANEL FOR WINDOWS MAIL
    int yWindowsMail = -1;
    JPanel windowsMail_JPanel = new JPanel();
    windowsMail_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Windows Mail", windowsMail_JPanel );
    
    JGUIUtil.addComponent(windowsMail_JPanel, new JLabel (
    		"<html><b>The Windows Mail functionality has not yet been fully implemented.</b></html>"),
    		0, ++yWindowsMail, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for Sendmail
    int ySendMail = -1;
    JPanel sendMail_JPanel = new JPanel();
    sendMail_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Sendmail", sendMail_JPanel );
    
    JGUIUtil.addComponent(sendMail_JPanel, new JLabel (
    		"<html><b>The Sendmail functionality has not yet been fully implemented.</b></html>"),
    		0, ++ySendMail, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

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
	String MailProgram = "";
	String From = "";
	String To = "";
	String CC = "";
	String BCC = "";
	String Subject = "";
	String Message0 = "";
	String MessageFile = "";
	String AttachmentFiles = "";
	String IfNotFound = "";
	String SMTPServer = "";
	String SMTPAccount = "";
	String SMTPPassword = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
        MailProgram = parameters.getValue( "MailProgram" );
        From = parameters.getValue ( "From" );
		To = parameters.getValue ( "To" );
		CC = parameters.getValue ( "CC" );
		BCC = parameters.getValue ( "BCC" );
		Subject = parameters.getValue ( "Subject" );
		Message0 = parameters.getValue ( "Message" );
		MessageFile = parameters.getValue ( "MessageFile" );
		AttachmentFiles = parameters.getValue ( "AttachmentFiles" );
		IfNotFound = parameters.getValue ( "IfNotFound" );
		SMTPServer = parameters.getValue ( "SMTPServer" );
		SMTPAccount = parameters.getValue ( "SMTPAccount" );
		SMTPPassword = parameters.getValue ( "SMTPPassword" );
		
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
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfNotFound_JComboBox, IfNotFound, JGUIUtil.NONE, null, null ) ) {
			__IfNotFound_JComboBox.select ( IfNotFound );
		}
		else {
            if ( (IfNotFound == null) || IfNotFound.equals("") ) {
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
		if ( JGUIUtil.isSimpleJComboBoxItem(__MailProgram_JComboBox, MailProgram, JGUIUtil.NONE, null, null ) ) {
			__MailProgram_JComboBox.select ( MailProgram );
		}
		else {
            if ( (MailProgram == null) || MailProgram.equals("") ) {
				// New command...select the default...
            	__MailProgram_JComboBox.select ( 0 );
			}
			else {	// Bad user command...
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"MailProgram parameter \"" + MailProgram +
				"\". Select a\n value or Cancel." );
			}
		}
		if ( SMTPServer != null ) {
			__SMTPServer_JTextField.setText(SMTPServer);
		}
		if ( SMTPAccount != null ) {
			__SMTPAccount_JTextField.setText(SMTPAccount);
		}
		if ( SMTPPassword != null ) {
			__SMTPPassword_JTextField.setText(SMTPPassword);
		}
	}
	// Regardless, reset the command from the fields. This is only visible information that has not been committed in the command.
	MailProgram = __MailProgram_JComboBox.getSelected();
	From = __From_JTextField.getText().trim();
	To = __To_JTextField.getText().trim();
	CC = __CC_JTextField.getText().trim();
	BCC = __BCC_JTextField.getText().trim();
	Subject = __Subject_JTextField.getText().trim();
	Message0 = __Message_JTextArea.getText().trim();
	MessageFile = __MessageFile_JTextField.getText().trim();
	AttachmentFiles = __AttachmentFiles_JTextField.getText().trim();
	IfNotFound = __IfNotFound_JComboBox.getSelected();
	SMTPServer = __SMTPServer_JTextField.getText().trim();
	SMTPAccount = __SMTPAccount_JTextField.getText().trim();
	SMTPPassword = __SMTPPassword_JTextField.getText().trim();
	PropList props = new PropList ( __command.getCommandName() );
	props.add ( "MailProgram=" + MailProgram );
	props.add ( "From=" + From );
	props.add ( "To=" + To );
	props.add ( "CC=" + CC );
	props.add ( "BCC=" + BCC );
	props.add ( "Subject=" + Subject );
	props.add ( "Message=" + Message0 );
	props.add ( "MessageFile=" + MessageFile );
	props.add ( "AttachmentFiles=" + AttachmentFiles );
	props.add ( "IfNotFound=" + IfNotFound );
	props.add ( "SMTPServer=" + SMTPServer );
	props.add ( "SMTPAccount=" + SMTPAccount);
	props.add ("SMTPPassword=" + SMTPPassword);
	__command_JTextArea.setText( __command.toString(props) );
	// Check the path and determine what the label on the path button should be...
	if ( __pathMessage_JButton != null ) {
		if ( (MessageFile != null) && !MessageFile.isEmpty() ) {
			__pathMessage_JButton.setEnabled ( true );
			File f = new File ( MessageFile );
			if ( f.isAbsolute() ) {
				__pathMessage_JButton.setText ( __RemoveWorkingDirectory );
				__pathMessage_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__pathMessage_JButton.setText ( __AddWorkingDirectory );
		    	__pathMessage_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathMessage_JButton.setEnabled(false);
		}
	}
	// Check the path and determine what the label on the path button should be...
	if ( __pathInput_JButton != null ) {
		if ( (AttachmentFiles != null) && !AttachmentFiles.isEmpty() ) {
			__pathInput_JButton.setEnabled ( true );
			File f = new File ( AttachmentFiles );
			if ( f.isAbsolute() ) {
				__pathInput_JButton.setText ( __RemoveWorkingDirectory );
				__pathInput_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
		    	__pathInput_JButton.setText ( __AddWorkingDirectory );
		    	__pathInput_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathInput_JButton.setEnabled(false);
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
