// WriteStateCU_JDialog - This class provides an editor dialog for the WriteStateCU() command.

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

package rti.tscommandprocessor.commands.statecu;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import java.io.File;

import RTi.Util.GUI.JFileChooserFactory;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleFileFilter;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
This class provides an editor dialog for the WriteStateCU() command.
*/
@SuppressWarnings("serial")
public class WriteStateCU_JDialog extends JDialog
implements ActionListener, KeyListener, WindowListener
{
private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __browse_JButton = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private WriteStateCU_Command __command = null;
private JTextArea __command_JTextArea=null;// Command as JTextField
private String __working_dir = null;	// Working directory.
private JTextField __OutputFile_JTextField = null;// Field for time series identifier
private JTextField __OutputStart_JTextField = null;// Start of period for output
private JTextField __OutputEnd_JTextField = null;// End of period for output
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has the user pressed OK to close the dialog?

/**
Command editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteStateCU_JDialog ( JFrame parent, WriteStateCU_Command command )
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
		fc.setDialogTitle("Select StateCU Time Series File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("stm", "StateCU Frost Dates File");
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
					Message.printWarning ( 1,"WriteStateCU_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory( directory);
				refresh();
			}
		}
	}
	else if ( o == __cancel_JButton ) {
		response ( false );
	}
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "WriteStateCU");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __path_JButton ) {
		if ( __path_JButton.getText().equals( __AddWorkingDirectory) ) {
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
		}
		else if ( __path_JButton.getText().equals( __RemoveWorkingDirectory) ) {
			try {
			    __OutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1,
				"WriteStateCU_JDialog", "Error converting file to relative path." );
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
    PropList props = new PropList ( "" );
    //String TSList = __TSList_JComboBox.getSelected();
    //String TSID = __TSID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    //String MissingValue = __MissingValue_JTextField.getText().trim();
    //String Precision = __Precision_JTextField.getText().trim();
    __error_wait = false;
    /*
    if ( TSList.length() > 0 ) {
        props.set ( "TSList", TSList );
    }
    if ( TSID.length() > 0 ) {
        props.set ( "TSID", TSID );
    }
    */
    if ( OutputFile.length() > 0 ) {
        props.set ( "OutputFile", OutputFile );
    }
    if ( OutputStart.length() > 0 ) {
        props.set ( "OutputStart", OutputStart );
    }
    if ( OutputEnd.length() > 0 ) {
        props.set ( "OutputEnd", OutputEnd );
    }
    /*
    if ( MissingValue.length() > 0 ) {
        props.set ( "MissingValue", MissingValue );
    }
    if ( Precision.length() > 0 ) {
        props.set ( "Precision", Precision );
    }
    */
    try {   // This will warn the user...
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
{   //String TSList = __TSList_JComboBox.getSelected();
    //String TSID = __TSID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String OutputStart = __OutputStart_JTextField.getText().trim();
    String OutputEnd = __OutputEnd_JTextField.getText().trim();
    //String MissingValue = __MissingValue_JTextField.getText().trim();
    //String Precision = __Precision_JTextField.getText().trim();
    //__command.setCommandParameter ( "TSList", TSList );
    //__command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "OutputFile", OutputFile );
    __command.setCommandParameter ( "OutputStart", OutputStart );
    __command.setCommandParameter ( "OutputEnd", OutputEnd );
    //__command.setCommandParameter ( "MissingValue", MissingValue );
    //__command.setCommandParameter ( "Precision", Precision );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteStateCU_Command command )
{	__command = command;
	__working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( (TSCommandProcessor)__command.getCommandProcessor(), __command );

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"THIS COMMAND CURRENTLY ONLY WRITES StateCU FROST DATE FILES."),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Time series with data types FrostDateL28S, FrostDateL32S," +
        " FrostDateF32F, and FrostDateF28F are processed." ),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write frost date time series to a StateCU format file," +
		" which can be specified using a full or relative path (relative to the working directory)." ),
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "StateCU file to write:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST );
	__OutputFile_JTextField = new JTextField ( 50 );
	__OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(main_JPanel, OutputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Output start:"), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputStart_JTextField = new JTextField (20);
    __OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputStart_JTextField,
        1, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Overrides the global output start, specify as YYYY."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output end:"), 
        0, ++y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputEnd_JTextField = new JTextField (20);
    __OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(main_JPanel, __OutputEnd_JTextField,
        1, y, 6, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Overrides the global output end, specify as YYYY."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 55 );
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

	__ok_JButton = new SimpleJButton( "OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton( "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
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

public void keyTyped ( KeyEvent event ) {;}

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
{   //String routine = "WriteStateCU_JDialog.refresh";
    //String TSList = "";
    //String TSID = "";
    String OutputFile = "";
    String OutputStart = "";
    String OutputEnd = "";
    //String MissingValue = "";
    //String Precision = "";
    PropList props = null;
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        props = __command.getCommandParameters();
        //TSList = props.getValue ( "TSList" );
        //TSID = props.getValue ( "TSID" );
        OutputFile = props.getValue("OutputFile");
        OutputStart = props.getValue("OutputStart");
        OutputEnd = props.getValue("OutputEnd");
        //MissingValue = props.getValue("MissingValue");
        //Precision = props.getValue("Precision");
        /*
        if ( TSList == null ) {
            // Select default...
            __TSList_JComboBox.select ( __command._AllTS );
        }
        else {  if (    JGUIUtil.isSimpleJComboBoxItem(
                __TSList_JComboBox,
                TSList, JGUIUtil.NONE, null, null ) ) {
                __TSList_JComboBox.select ( TSList );
            }
            else {  Message.printWarning ( 1, routine,
                "Existing command " +
                "references an invalid\nTSList value \"" +
                TSList +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (    JGUIUtil.isSimpleJComboBoxItem( __TSID_JComboBox, TSID,
                JGUIUtil.NONE, null, null ) ) {
                __TSID_JComboBox.select ( TSID );
        }
        else {  // Automatically add to the list after the blank...
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select...
                __TSID_JComboBox.select ( TSID );
            }
            else {  // Select the blank...
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( !__TSID_JComboBox.isEnabled() ) {
            // Not needed because some other method of specifying
            // the time series is being used...
            TSID = null;
        }*/
        if ( OutputFile != null ) {
            __OutputFile_JTextField.setText( OutputFile );
        }
        if ( OutputStart != null ) {
            __OutputStart_JTextField.setText ( OutputStart );
        }
        if ( OutputEnd != null ) {
            __OutputEnd_JTextField.setText ( OutputEnd );
        }
        /*
        if ( MissingValue != null ) {
            __MissingValue_JTextField.setText ( MissingValue );
        }
        if ( Precision != null ) {
            __Precision_JTextField.setText ( Precision );
        }
        */
    }
    // Regardless, reset the command from the fields...
    /*
    TSList = __TSList_JComboBox.getSelected();
    if ( __TSID_JComboBox.isEnabled() ) {
        TSID = __TSID_JComboBox.getSelected();
    }
    else {  TSID = "";
    }
    */
    OutputFile = __OutputFile_JTextField.getText().trim();
    OutputStart = __OutputStart_JTextField.getText().trim();
    OutputEnd = __OutputEnd_JTextField.getText().trim();
    /*
    MissingValue = __MissingValue_JTextField.getText().trim();
    Precision = __Precision_JTextField.getText().trim();
    */
    props = new PropList ( __command.getCommandName() );
    //props.add ( "TSList=" + TSList );
    //props.add ( "TSID=" + TSID );
    props.add ( "OutputFile=" + OutputFile );
    props.add ( "OutputStart=" + OutputStart );
    props.add ( "OutputEnd=" + OutputEnd );
    //props.add ( "MissingValue=" + MissingValue );
    //props.add ( "Precision=" + Precision );
    __command_JTextArea.setText( __command.toString ( props ).trim() );
	// Check the path and determine what the label on the path button should be...
	if ( __path_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
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
		else {
			__path_JButton.setEnabled(false);
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
