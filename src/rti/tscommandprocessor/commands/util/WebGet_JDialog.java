// WebGet_JDialog - editor for WebGet command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2023 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.util;

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

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

@SuppressWarnings("serial")
public class WebGet_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __browsePayload_JButton = null;
private SimpleJButton __path_JButton = null;
private SimpleJButton __pathPayload_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __URI_JTextArea = null;
private SimpleJComboBox	__EncodeURI_JComboBox =null;
private SimpleJComboBox __RequestMethod_JComboBox = null;
private JTextField __PayloadFile_JTextField = null;
private JTextArea __HttpHeaders_JTextArea = null;
private JTextArea __Cookies_JTextArea = null;
private JTextField __ConnectTimeout_JTextField = null;
private JTextField __ReadTimeout_JTextField = null;
private JTextField __RetryMax_JTextField = null;
private JTextField __RetryWait_JTextField = null;
private JTextField __LocalFile_JTextField = null;
private JTextField __OutputProperty_JTextField = null;
private SimpleJComboBox	__IfHttpError_JComboBox =null;
private JTextField __ResponseCodeProperty_JTextField = null;
private JTextArea __command_JTextArea = null;
private String __working_dir = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private WebGet_Command __command = null;
private JFrame __parent = null;
private boolean __ok = false; // Indicates whether OK pressed to close the dialog.

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WebGet_JDialog ( JFrame parent, WebGet_Command command ) {
	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event ) {
	Object o = event.getSource();

    if ( o == __browse_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Local File to Write");
        SimpleFileFilter sff = new SimpleFileFilter("txt", "Text file");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("txt", "Text File");
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
					__LocalFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, "WebGet", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
    else if ( o == __browsePayload_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Payload File for Request");
        //SimpleFileFilter sff = new SimpleFileFilter("txt", "Text file");
        //fc.addChoosableFileFilter(sff);

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
					__PayloadFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1, "WebGet", "Error converting payload file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "WebGet");
	}
    else if ( event.getActionCommand().equalsIgnoreCase("EditHttpHeaders") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String HttpHeaders = __HttpHeaders_JTextArea.getText().trim();
        String [] notes = {
            "HTTP header properties can be set for the request.",
            "The PropertyValue can use ${Property} notation.",
            "Use a property for PropertyValue if special characters generate warnings when used below.",
            "It may also be necessary to surround the property value with single quotes."
        };
        String dict = (new DictionaryJDialog ( __parent, true, HttpHeaders,
            "Edit HttpHeaders Parameter", notes, "Property Name", "Property Value",10)).response();
        if ( dict != null ) {
            __HttpHeaders_JTextArea.setText ( dict );
            refresh();
        }
    }
    else if ( event.getActionCommand().equalsIgnoreCase("EditCookies") ) {
        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
        String HttpHeaders = __Cookies_JTextArea.getText().trim();
        String [] notes = {
            "Cookie header properties can be set for the request and will be inluded in a single header as follows:.",
            "",
            "   Cookie: CookieName1=CookieValue1; CookieName2=CookieValue2"
        };
        String dict = (new DictionaryJDialog ( __parent, true, HttpHeaders,
            "Edit Cookies Parameter", notes, "Cookie Name", "Cookie Value",10)).response();
        if ( dict != null ) {
            __Cookies_JTextArea.setText ( dict );
            refresh();
        }
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
			__LocalFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__LocalFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
                __LocalFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __LocalFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "WebGet_JDialog", "Error converting local file name to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __pathPayload_JButton ) {
		if ( __pathPayload_JButton.getText().equals(__AddWorkingDirectory) ) {
			__PayloadFile_JTextField.setText (IOUtil.toAbsolutePath(__working_dir,__PayloadFile_JTextField.getText() ) );
		}
		else if ( __pathPayload_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
                __PayloadFile_JTextField.setText ( IOUtil.toRelativePath ( __working_dir,
                        __PayloadFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "WebGet_JDialog", "Error converting payload file name to relative path." );
			}
		}
		refresh ();
	}
	else {
	    // Choices.
		refresh();
	}
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag to true.
This should be called before response() is allowed to complete.
*/
private void checkInput () {
	// Put together a list of parameters to check.
	PropList props = new PropList ( "" );
	String URI = __URI_JTextArea.getText().trim();
	String EncodeURI = __EncodeURI_JComboBox.getSelected();
	String RequestMethod = __RequestMethod_JComboBox.getSelected();
	String PayloadFile = __PayloadFile_JTextField.getText().trim();
	String HttpHeaders = __HttpHeaders_JTextArea.getText().trim().replace("\n"," ");
	String Cookies = __Cookies_JTextArea.getText().trim().replace("\n"," ");
	String ConnectTimeout = __ConnectTimeout_JTextField.getText().trim();
	String ReadTimeout = __ReadTimeout_JTextField.getText().trim();
	String RetryMax = __RetryMax_JTextField.getText().trim();
	String RetryWait = __RetryWait_JTextField.getText().trim();
	String LocalFile = __LocalFile_JTextField.getText().trim();
	String OutputProperty = __OutputProperty_JTextField.getText().trim();
	String IfHttpError = __IfHttpError_JComboBox.getSelected();
	String ResponseCodeProperty = __ResponseCodeProperty_JTextField.getText().trim();
	__error_wait = false;
	if ( URI.length() > 0 ) {
		props.set ( "URI", URI );
	}
	if ( EncodeURI.length() > 0 ) {
		props.set ( "EncodeURI", EncodeURI );
	}
	if ( RequestMethod.length() > 0 ) {
		props.set ( "RequestMethod", RequestMethod );
	}
	if ( PayloadFile.length() > 0 ) {
		props.set ( "PayloadFile", PayloadFile );
	}
	if ( HttpHeaders.length() > 0 ) {
		props.set ( "HttpHeaders", HttpHeaders );
	}
	if ( Cookies.length() > 0 ) {
		props.set ( "Cookies", Cookies );
	}
	if ( ConnectTimeout.length() > 0 ) {
		props.set ( "ConnectTimeout", ConnectTimeout );
	}
	if ( ReadTimeout.length() > 0 ) {
		props.set ( "ReadTimeout", ReadTimeout );
	}
	if ( RetryMax.length() > 0 ) {
		props.set ( "RetryMax", RetryMax );
	}
	if ( RetryWait.length() > 0 ) {
		props.set ( "RetryWait", RetryWait );
	}
    if ( LocalFile.length() > 0 ) {
        props.set ( "LocalFile", LocalFile );
    }
    if ( OutputProperty.length() > 0 ) {
        props.set ( "OutputProperty", OutputProperty );
    }
    if ( IfHttpError.length() > 0 ) {
        props.set ( "IfHttpError", IfHttpError );
    }
    if ( ResponseCodeProperty.length() > 0 ) {
        props.set ( "ResponseCodeProperty", ResponseCodeProperty );
    }
	try {
	    // This will warn the user.
		__command.checkCommandParameters ( props, null, 1 );
	}
	catch ( Exception e ) {
		// The warning would have been printed in the check code.
		__error_wait = true;
	}
}

/**
Commit the edits to the command.
In this case the command parameters have already been checked and no errors were detected.
*/
private void commitEdits () {
	String URI = __URI_JTextArea.getText().trim();
	String EncodeURI = __EncodeURI_JComboBox.getSelected();
	String RequestMethod = __RequestMethod_JComboBox.getSelected();
	String PayloadFile = __PayloadFile_JTextField.getText().trim();
	String HttpHeaders = __HttpHeaders_JTextArea.getText().trim().replace("\n"," ");
	String Cookies = __Cookies_JTextArea.getText().trim().replace("\n"," ");
	String ConnectTimeout = __ConnectTimeout_JTextField.getText().trim();
	String ReadTimeout = __ReadTimeout_JTextField.getText().trim();
	String RetryMax = __RetryMax_JTextField.getText().trim();
	String RetryWait = __RetryWait_JTextField.getText().trim();
    String LocalFile = __LocalFile_JTextField.getText().trim();
    String OutputProperty = __OutputProperty_JTextField.getText().trim();
	String IfHttpError = __IfHttpError_JComboBox.getSelected();
    String ResponseCodeProperty = __ResponseCodeProperty_JTextField.getText().trim();
	__command.setCommandParameter ( "URI", URI );
	__command.setCommandParameter ( "EncodeURI", EncodeURI );
	__command.setCommandParameter ( "RequestMethod", RequestMethod );
	__command.setCommandParameter ( "PayloadFile", PayloadFile );
	__command.setCommandParameter ( "HttpHeaders", HttpHeaders );
	__command.setCommandParameter ( "Cookies", Cookies );
	__command.setCommandParameter ( "ConnectTimeout", ConnectTimeout );
	__command.setCommandParameter ( "ReadTimeout", ReadTimeout );
	__command.setCommandParameter ( "RetryMax", RetryMax );
	__command.setCommandParameter ( "RetryWait", RetryWait );
	__command.setCommandParameter ( "LocalFile", LocalFile );
	__command.setCommandParameter ( "OutputProperty", OutputProperty );
	__command.setCommandParameter ( "IfHttpError", IfHttpError );
	__command.setCommandParameter ( "ResponseCodeProperty", ResponseCodeProperty );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WebGet_Command command ) {
	this.__command = command;
	this.__parent = parent;
	CommandProcessor processor =__command.getCommandProcessor();

	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)processor, __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel.

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command performs a web request for a Uniform Resource Identifier (URI, which is a more general term that includes URLs)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"By default, a GET request will occur and the response can be saved to a file or property."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Other request methods (DELETE, OPTIONS, POST, PUT) are being phased in."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
    		"It is recommended that the local file name is relative to the working directory, which is:"),
    		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    	JGUIUtil.addComponent(main_JPanel, new JLabel (
    		"    " + __working_dir),
    		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    //__main_JTabbedPane.addChangeListener(this);
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for request.
    int yRequest = -1;
    JPanel request_JPanel = new JPanel();
    request_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Request", request_JPanel );

    JGUIUtil.addComponent(request_JPanel, new JLabel ("URI:" ),
        0, ++yRequest, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __URI_JTextArea = new JTextArea ( 6, 60 );
    __URI_JTextArea.setToolTipText("Specify the URL from which to read content, can use ${Property}.");
    __URI_JTextArea.setLineWrap ( true );
    __URI_JTextArea.setWrapStyleWord ( true );
    __URI_JTextArea.addKeyListener ( this );
    __URI_JTextArea.setEditable ( true );
        JGUIUtil.addComponent(request_JPanel, new JScrollPane(__URI_JTextArea),
        1, yRequest, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(request_JPanel, new JLabel ("Required - URI for content to download."),
        3, yRequest, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(request_JPanel, new JLabel ( "Encode URI?:"),
		0, ++yRequest, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__EncodeURI_JComboBox = new SimpleJComboBox ( false );
	__EncodeURI_JComboBox.setToolTipText(
		"Should the query part of the URI be encoded, for example space character becomes + and other special charactes are %-encoded.");
	List<String> encodeChoices = new ArrayList<>();
	encodeChoices.add ( "" );	// Default.
	encodeChoices.add ( __command._False );
	encodeChoices.add ( __command._True );
	__EncodeURI_JComboBox.setData(encodeChoices);
	__EncodeURI_JComboBox.select ( 0 );
	__EncodeURI_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(request_JPanel, __EncodeURI_JComboBox,
		1, yRequest, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(request_JPanel, new JLabel(
		"Optional - encode the URI? (default=" + __command._True + ")."),
		3, yRequest, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(request_JPanel, new JLabel ( "Request method:"),
		0, ++yRequest, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__RequestMethod_JComboBox = new SimpleJComboBox ( false );
	__RequestMethod_JComboBox.setToolTipText(
		"HTTP request method.");
	List<String> methodChoices = new ArrayList<>();
	methodChoices.add ( "" );	// Default.
	methodChoices.add ( __command.DELETE );
	methodChoices.add ( __command.GET );
	methodChoices.add ( __command.OPTIONS );
	methodChoices.add ( __command.POST );
	methodChoices.add ( __command.PUT );
	__RequestMethod_JComboBox.setData(methodChoices);
	__RequestMethod_JComboBox.select ( 0 );
	__RequestMethod_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(request_JPanel, __RequestMethod_JComboBox,
		1, yRequest, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(request_JPanel, new JLabel(
		"Optional - request method (default=" + __command.GET + ")."),
		3, yRequest, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(request_JPanel, new JLabel ("Payload file:" ),
		0, ++yRequest, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__PayloadFile_JTextField = new JTextField ( 50 );
	__PayloadFile_JTextField.setToolTipText("Specify the payload file (for PUT and POST requests), can use ${Property}.");
	__PayloadFile_JTextField.addKeyListener ( this );
    // Layout fights back with other rows so put in its own panel.
	JPanel PayloadFile_JPanel = new JPanel();
	PayloadFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(PayloadFile_JPanel, __PayloadFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browsePayload_JButton = new SimpleJButton ( "...", this );
	__browsePayload_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(PayloadFile_JPanel, __browsePayload_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__pathPayload_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(PayloadFile_JPanel, __pathPayload_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(request_JPanel, PayloadFile_JPanel,
		1, yRequest, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(request_JPanel, new JLabel ("HTTP headers:"),
        0, ++yRequest, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __HttpHeaders_JTextArea = new JTextArea (6,35);
    __HttpHeaders_JTextArea.setToolTipText("Each header is added using the specified name and value.");
    __HttpHeaders_JTextArea.setLineWrap ( true );
    __HttpHeaders_JTextArea.setWrapStyleWord ( true );
    __HttpHeaders_JTextArea.setToolTipText("HeaderName1:HeaderValue1,HeaderName2:HeaderValue2,...");
    __HttpHeaders_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(request_JPanel, new JScrollPane(__HttpHeaders_JTextArea),
        1, yRequest, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(request_JPanel, new JLabel ("Optional - HTTP headers."),
        3, yRequest, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(request_JPanel, new SimpleJButton ("Edit","EditHttpHeaders",this),
        3, ++yRequest, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(request_JPanel, new JLabel ("Cookies:"),
        0, ++yRequest, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Cookies_JTextArea = new JTextArea (6,35);
    __Cookies_JTextArea.setToolTipText("All cookies are included in a single 'Cookie' header the specified name and value.");
    __Cookies_JTextArea.setLineWrap ( true );
    __Cookies_JTextArea.setWrapStyleWord ( true );
    __Cookies_JTextArea.setToolTipText("CookieName1:CookieValue1,CookieName2:CookieValue2,...");
    __Cookies_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(request_JPanel, new JScrollPane(__Cookies_JTextArea),
        1, yRequest, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(request_JPanel, new JLabel ("Optional - cookies."),
        3, yRequest, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(request_JPanel, new SimpleJButton ("Edit","EditCookies",this),
        3, ++yRequest, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for timeout and retry.
    int yTimeout = -1;
    JPanel timeout_JPanel = new JPanel();
    timeout_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Timeout & Retry", timeout_JPanel );

    JGUIUtil.addComponent(timeout_JPanel, new JLabel (
		"It is generally a good practice to set an appropriate timeout on requests to streamline workflows." ),
		0, ++yTimeout, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yTimeout, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(timeout_JPanel, new JLabel ("Connection timeout:"),
        0, ++yTimeout, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ConnectTimeout_JTextField = new JTextField (10);
    __ConnectTimeout_JTextField.setToolTipText("Timeout for establishing connection (ms), after which an error will occur.");
    __ConnectTimeout_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(timeout_JPanel, __ConnectTimeout_JTextField,
        1, yTimeout, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JLabel ("Optional - connection timeout, ms (default=60000)."),
        3, yTimeout, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(timeout_JPanel, new JLabel ("Read timeout:"),
        0, ++yTimeout, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ReadTimeout_JTextField = new JTextField (10);
    __ReadTimeout_JTextField.setToolTipText("Timeout for starting read (ms), after which an error will occur.");
    __ReadTimeout_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(timeout_JPanel, __ReadTimeout_JTextField,
        1, yTimeout, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JLabel ("Optional - read timeout, ms (default=60000)."),
        3, yTimeout, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(timeout_JPanel, new JLabel ("Retry maximum:"),
        0, ++yTimeout, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RetryMax_JTextField = new JTextField (10);
    __RetryMax_JTextField.setToolTipText("Maximum number of retries if a connection:w"
    		+ " timeout or error occurs.");
    __RetryMax_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(timeout_JPanel, __RetryMax_JTextField,
        1, yTimeout, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JLabel ("Optional - (default=no retries)."),
        3, yTimeout, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(timeout_JPanel, new JLabel ("Retry wait:"),
        0, ++yTimeout, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __RetryWait_JTextField = new JTextField (10);
    __RetryWait_JTextField.setToolTipText("Wait between retries (ms).");
    __RetryWait_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(timeout_JPanel, __RetryWait_JTextField,
        1, yTimeout, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(timeout_JPanel, new JLabel ("Optional - wait between retries, ms (default=0)."),
        3, yTimeout, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    // Panel for response.
    int yResponse = -1;
    JPanel response_JPanel = new JPanel();
    response_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Response", response_JPanel );

    JGUIUtil.addComponent(response_JPanel, new JLabel (
		"The response can be saved to a file and/or processor property." ),
		0, ++yResponse, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(response_JPanel, new JLabel (
		"A successful response code is 200 and can be set as a property to control workflow logic." ),
		0, ++yResponse, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(response_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yResponse, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(response_JPanel, new JLabel ("Local file:" ),
		0, ++yResponse, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__LocalFile_JTextField = new JTextField ( 50 );
	__LocalFile_JTextField.setToolTipText("Specify the output file (will have same the contents as retrieved from URL), can use ${Property}.");
	__LocalFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel LocalFile_JPanel = new JPanel();
	LocalFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(LocalFile_JPanel, __LocalFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(LocalFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(LocalFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(response_JPanel, LocalFile_JPanel,
		1, yResponse, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(response_JPanel, new JLabel ("Output property:"),
        0, ++yResponse, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputProperty_JTextField = new JTextField (10);
    __OutputProperty_JTextField.setToolTipText("Name of processor property to assign retrieved URI content.");
    __OutputProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(response_JPanel, __OutputProperty_JTextField,
        1, yResponse, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(response_JPanel, new JLabel ("Optional - property name for output (default=not set)."),
        3, yResponse, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(response_JPanel, new JLabel ( "If HTTP error occurs?:"),
		0, ++yResponse, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__IfHttpError_JComboBox = new SimpleJComboBox ( false );
	__IfHttpError_JComboBox.setToolTipText("An HTTP error is any code other than 200, which indicates success.");
	List<String> notFoundChoices = new ArrayList<>();
	notFoundChoices.add ( "" );	// Default.
	notFoundChoices.add ( __command._Ignore );
	notFoundChoices.add ( __command._Warn );
	notFoundChoices.add ( __command._Fail );
	__IfHttpError_JComboBox.setData(notFoundChoices);
	__IfHttpError_JComboBox.select ( 0 );
	__IfHttpError_JComboBox.addActionListener ( this );
    JGUIUtil.addComponent(response_JPanel, __IfHttpError_JComboBox,
		1, yResponse, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(response_JPanel, new JLabel(
		"Optional - action if HTTP error after retries (default=" + __command._Warn + ")."),
		3, yResponse, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(response_JPanel, new JLabel ("Response code property:"),
        0, ++yResponse, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ResponseCodeProperty_JTextField = new JTextField (10);
    __ResponseCodeProperty_JTextField.setToolTipText("Name of processor property to assign retrieved URI content.");
    __ResponseCodeProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(response_JPanel, __ResponseCodeProperty_JTextField,
        1, yResponse, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(response_JPanel, new JLabel ("Optional - property name for response code (default=not set)."),
        3, yResponse, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 6, 60 );
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

    // Refresh the contents.
    refresh ();

	setTitle ( "Edit " + __command.getCommandName() + "() command" );

    pack();
    JGUIUtil.center( this );
	// Dialogs do not need to be resizable.
	setResizable ( false );
    super.setVisible( true );
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event ) {
	int code = event.getKeyCode();

	if ( code == KeyEvent.VK_ENTER ) {
		refresh ();
	}
}

public void keyReleased ( KeyEvent event ) {
	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

/**
Indicate if the user pressed OK (cancel otherwise).
*/
public boolean ok () {
	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh () {
	String routine = getClass().getSimpleName() + ".refresh";
    String URI = "";
    String EncodeURI = "";
    String RequestMethod = "";
    String PayloadFile = "";
    String HttpHeaders = "";
    String Cookies = "";
    String ConnectTimeout = "";
    String ReadTimeout = "";
    String RetryMax = "";
    String RetryWait = "";
    String LocalFile = "";
    String OutputProperty = "";
    String IfHttpError = "";
    String ResponseCodeProperty = "";
    PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
        parameters = __command.getCommandParameters();
        URI = parameters.getValue ( "URI" );
        EncodeURI = parameters.getValue ( "EncodeURI" );
        RequestMethod = parameters.getValue ( "RequestMethod" );
        PayloadFile = parameters.getValue ( "PayloadFile" );
        HttpHeaders = parameters.getValue ( "HttpHeaders" );
        Cookies = parameters.getValue ( "Cookies" );
        ConnectTimeout = parameters.getValue ( "ConnectTimeout" );
        ReadTimeout = parameters.getValue ( "ReadTimeout" );
        RetryMax = parameters.getValue ( "RetryMax" );
        RetryWait = parameters.getValue ( "RetryWait" );
        LocalFile = parameters.getValue ( "LocalFile" );
        OutputProperty = parameters.getValue ( "OutputProperty" );
        IfHttpError = parameters.getValue ( "IfHttpError" );
        ResponseCodeProperty = parameters.getValue ( "ResponseCodeProperty" );
		if ( URI != null ) {
			__URI_JTextArea.setText ( URI );
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__EncodeURI_JComboBox, EncodeURI,JGUIUtil.NONE, null, null ) ) {
			__EncodeURI_JComboBox.select ( EncodeURI );
		}
		else {
            if ( (EncodeURI == null) ||	EncodeURI.equals("") ) {
				// New command...select the default.
				__EncodeURI_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"EncodeURI parameter \"" + EncodeURI +
				"\".  Select a\n value or Cancel." );
			}
		}
		if ( JGUIUtil.isSimpleJComboBoxItem(__RequestMethod_JComboBox, RequestMethod,JGUIUtil.NONE, null, null ) ) {
			__RequestMethod_JComboBox.select ( RequestMethod );
		}
		else {
            if ( (RequestMethod == null) ||	RequestMethod.equals("") ) {
				// New command...select the default.
				__RequestMethod_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"RequestMethod parameter \"" + RequestMethod +
				"\".  Select a\n value or Cancel." );
			}
		}
        if ( PayloadFile != null ) {
            __PayloadFile_JTextField.setText ( PayloadFile );
        }
        if ( HttpHeaders != null ) {
            __HttpHeaders_JTextArea.setText ( HttpHeaders );
        }
        if ( Cookies != null ) {
            __Cookies_JTextArea.setText ( Cookies );
        }
		if ( ConnectTimeout != null ) {
			__ConnectTimeout_JTextField.setText ( ConnectTimeout );
		}
		if ( ReadTimeout != null ) {
			__ReadTimeout_JTextField.setText ( ReadTimeout );
		}
		if ( RetryMax != null ) {
			__RetryMax_JTextField.setText ( RetryMax );
		}
		if ( RetryWait != null ) {
			__RetryWait_JTextField.setText ( RetryWait );
		}
        if ( LocalFile != null ) {
            __LocalFile_JTextField.setText ( LocalFile );
        }
        if ( OutputProperty != null ) {
            __OutputProperty_JTextField.setText ( OutputProperty );
        }
		if ( JGUIUtil.isSimpleJComboBoxItem(__IfHttpError_JComboBox, IfHttpError,JGUIUtil.NONE, null, null ) ) {
			__IfHttpError_JComboBox.select ( IfHttpError );
		}
		else {
            if ( (IfHttpError == null) || IfHttpError.equals("") ) {
				// New command...select the default.
				__IfHttpError_JComboBox.select ( 0 );
			}
			else {
				// Bad user command.
				Message.printWarning ( 1, routine,
				"Existing command references an invalid\n"+
				"IfHttpError parameter \"" + IfHttpError +
				"\".  Select a\n value or Cancel." );
			}
		}
        if ( ResponseCodeProperty != null ) {
            __ResponseCodeProperty_JTextField.setText ( ResponseCodeProperty );
        }
	}
	// Regardless, reset the command from the fields.
	// This is only  visible information that has not been committed in the command.
	URI = __URI_JTextArea.getText().trim();
	EncodeURI = __EncodeURI_JComboBox.getSelected();
	RequestMethod = __RequestMethod_JComboBox.getSelected();
    PayloadFile = __PayloadFile_JTextField.getText().trim();
	HttpHeaders = __HttpHeaders_JTextArea.getText().trim().replace("\n"," ");
	Cookies = __Cookies_JTextArea.getText().trim().replace("\n"," ");
	ConnectTimeout = __ConnectTimeout_JTextField.getText().trim();
	ReadTimeout = __ReadTimeout_JTextField.getText().trim();
	RetryMax = __RetryMax_JTextField.getText().trim();
	RetryWait = __RetryWait_JTextField.getText().trim();
    LocalFile = __LocalFile_JTextField.getText().trim();
    OutputProperty = __OutputProperty_JTextField.getText().trim();
	IfHttpError = __IfHttpError_JComboBox.getSelected();
    ResponseCodeProperty = __ResponseCodeProperty_JTextField.getText().trim();
	PropList props = new PropList ( __command.getCommandName() );
	props.set ( "URI", URI ); // Use 'set' because the URIL may contain equals.
	props.add ( "EncodeURI=" + EncodeURI );
	props.add ( "RequestMethod=" + RequestMethod );
	props.add ( "PayloadFile=" + PayloadFile );
	props.set ( "HttpHeaders", HttpHeaders ); // Use 'set' because headers may contain equals.
	props.set ( "Cookies", Cookies ); // Use 'set' because headers may contain equals.
	props.add ( "ConnectTimeout=" + ConnectTimeout );
	props.add ( "ReadTimeout=" + ReadTimeout );
	props.add ( "RetryMax=" + RetryMax );
	props.add ( "RetryWait=" + RetryWait );
	props.add ( "LocalFile=" + LocalFile );
	props.add ( "OutputProperty=" + OutputProperty );
	props.add ( "IfHttpError=" + IfHttpError );
	props.add ( "ResponseCodeProperty=" + ResponseCodeProperty );
	__command_JTextArea.setText( __command.toString(props).trim() );
	// Check the path and determine what the label on the path button should be.
	if ( __path_JButton != null ) {
		if ( (LocalFile != null) && !LocalFile.isEmpty() ) {
			__path_JButton.setEnabled ( true );
			File f = new File ( LocalFile );
			if ( f.isAbsolute() ) {
				__path_JButton.setText ( __RemoveWorkingDirectory );
				__path_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__path_JButton.setText ( __AddWorkingDirectory );
            	__path_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__path_JButton.setEnabled(false);
		}
	}
	if ( __pathPayload_JButton != null ) {
		if ( (PayloadFile != null) && !PayloadFile.isEmpty() ) {
			__pathPayload_JButton.setEnabled ( true );
			File f = new File ( PayloadFile );
			if ( f.isAbsolute() ) {
				__pathPayload_JButton.setText ( __RemoveWorkingDirectory );
				__pathPayload_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathPayload_JButton.setText ( __AddWorkingDirectory );
            	__pathPayload_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathPayload_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled. If true, the edit is committed and the dialog is closed.
*/
public void response ( boolean ok ) {
	__ok = ok;
	if ( ok ) {
		// Commit the changes.
		commitEdits ();
		if ( __error_wait ) {
			// Not ready to close out.
			return;
		}
	}
	// Now close out.
	setVisible( false );
	dispose();
}

/**
Responds to WindowEvents.
@param event WindowEvent object
*/
public void windowClosing( WindowEvent event ) {
	response ( false );
}

public void windowActivated( WindowEvent evt ) {
}

public void windowClosed( WindowEvent evt ) {
}

public void windowDeactivated( WindowEvent evt ) {
}

public void windowDeiconified( WindowEvent evt ) {
}

public void windowIconified( WindowEvent evt ) {
}

public void windowOpened( WindowEvent evt ) {
}

}