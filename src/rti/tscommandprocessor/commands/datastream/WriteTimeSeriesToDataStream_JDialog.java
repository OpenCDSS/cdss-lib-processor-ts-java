// WriteTimeSeriesToDataStream_JDialog - editor dialog for the WriteTimeSeriesToDataStream() command.

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
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

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
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Time.DateTimeFormatterSpecifiersJPanel;
import RTi.Util.Time.DateTimeFormatterType;

/**
Command editor dialog for the WriteTimeSeriesToDataStream() command.
*/
@SuppressWarnings("serial")
public class WriteTimeSeriesToDataStream_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, ItemListener, WindowListener
{

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browseOutput_JButton = null;
private SimpleJButton __browseHeader_JButton = null;
private SimpleJButton __browseFormat_JButton = null;
private SimpleJButton __browseFooter_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __pathOutput_JButton = null;
private SimpleJButton __pathHeader_JButton = null;
private SimpleJButton __pathFormat_JButton = null;
private SimpleJButton __pathFooter_JButton = null;
private WriteTimeSeriesToDataStream_Command __command = null;
private String __working_dir = null;
private JTextArea __command_JTextArea=null;
private JTabbedPane __main_JTabbedPane = null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __Append_JComboBox = null;
private JTextArea __OutputFileHeader_JTextArea = null;
private JTextField __OutputFileHeaderFile_JTextField = null;
private JTextArea __OutputLineFormat_JTextArea = null;
private JTextField __OutputLineFormatFile_JTextField = null;
private JTextArea __LastOutputLineFormat_JTextArea = null;
private DateTimeFormatterSpecifiersJPanel __DateTimeFormat_JPanel = null;
private JTextArea __OutputFileFooter_JTextArea = null;
private JTextField __OutputFileFooterFile_JTextField = null;
private JTextField __Precision_JTextField = null;
private JTextField __OutputStart_JTextField = null;
private JTextField __OutputEnd_JTextField = null;
private JTextField __NonMissingOutputCount_JTextField = null;
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

	if ( o == __browseOutput_JButton ) {
		String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
		JFileChooser fc = null;
		if ( last_directory_selected != null ) {
			fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
		}
		else {
		    fc = JFileChooserFactory.createJFileChooser( __working_dir );
		}
		fc.setDialogTitle("Select Data Stream Time Series File to Write");
		SimpleFileFilter sff = new SimpleFileFilter("txt", "Data Stream Text Time Series File");
		fc.addChoosableFileFilter(sff);
		sff = new SimpleFileFilter("xml", "Data Stream XML Time Series File");
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
					Message.printWarning ( 1,"WriteTimeSeriesToDataStream_JDialog", "Error converting file to relative path." );
				}
				JGUIUtil.setLastFileDialogDirectory(directory );
				refresh();
			}
		}
	}
	else if ( o == __browseHeader_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Header File");
        SimpleFileFilter sff = new SimpleFileFilter("txt", "Header File (text)");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("xml", "Header File (XML)");
        fc.addChoosableFileFilter(sff);
        
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
					__OutputFileHeaderFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"WriteTimeSeriesToDataStream_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
	else if ( o == __browseFormat_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Data Line Format File");
        SimpleFileFilter sff = new SimpleFileFilter("txt", "Data Line Format File (text)");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("xml", "Data Line Format File (XML)");
        fc.addChoosableFileFilter(sff);
        
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
					__OutputLineFormatFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"WriteTimeSeriesToDataStream_JDialog", "Error converting file to relative path." );
				}
                JGUIUtil.setLastFileDialogDirectory(directory );
                refresh();
            }
        }
    }
	else if ( o == __browseFooter_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select Footer File");
        SimpleFileFilter sff = new SimpleFileFilter("txt", "Footer File (text)");
        fc.addChoosableFileFilter(sff);
        sff = new SimpleFileFilter("xml", "Footer File (XML)");
        fc.addChoosableFileFilter(sff);
        
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
					__OutputFileFooterFile_JTextField.setText(IOUtil.toRelativePath(__working_dir, path));
				}
				catch ( Exception e ) {
					Message.printWarning ( 1,"WriteTimeSeriesToDataStream_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "WriteTimeSeriesToDataStream");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
	else if ( o == __pathOutput_JButton ) {
		if ( __pathOutput_JButton.getText().equals(__AddWorkingDirectory) ) {
			__OutputFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __OutputFile_JTextField.getText() ) );
		}
		else if ( __pathOutput_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
			    __OutputFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "WriteTimeSeriesToDataStream", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
	else if ( o == __pathHeader_JButton ) {
		if ( __pathHeader_JButton.getText().equals(__AddWorkingDirectory) ) {
			__OutputFileHeaderFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __OutputFileHeaderFile_JTextField.getText() ) );
		}
		else if ( __pathHeader_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
				__OutputFileHeaderFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputFileHeaderFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "WriteTimeSeriesToDataStream", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
    else if ( o == __pathFormat_JButton ) {
        if ( __pathFormat_JButton.getText().equals(__AddWorkingDirectory) ) {
            __OutputLineFormatFile_JTextField.setText (
            IOUtil.toAbsolutePath(__working_dir, __OutputLineFormatFile_JTextField.getText() ) );
        }
        else if ( __pathFormat_JButton.getText().equals(__RemoveWorkingDirectory) ) {
            try {
                __OutputLineFormatFile_JTextField.setText (
                IOUtil.toRelativePath ( __working_dir, __OutputLineFormatFile_JTextField.getText() ) );
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, "WriteTimeSeriesToDataStream", "Error converting file to relative path." );
            }
        }
        refresh ();
    }
	else if ( o == __pathFooter_JButton ) {
		if ( __pathFooter_JButton.getText().equals(__AddWorkingDirectory) ) {
			__OutputFileFooterFile_JTextField.setText (
			IOUtil.toAbsolutePath(__working_dir, __OutputFileFooterFile_JTextField.getText() ) );
		}
		else if ( __pathFooter_JButton.getText().equals(__RemoveWorkingDirectory) ) {
			try {
				__OutputFileFooterFile_JTextField.setText (
				IOUtil.toRelativePath ( __working_dir, __OutputFileFooterFile_JTextField.getText() ) );
			}
			catch ( Exception e ) {
				Message.printWarning ( 1, "WriteTimeSeriesToDataStream", "Error converting file to relative path." );
			}
		}
		refresh ();
	}
}

//Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{   checkGUIState();
    refresh();
}

// ...End event handlers for DocumentListener

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
	String OutputFileHeaderFile = __OutputFileHeaderFile_JTextField.getText().trim();
	String OutputLineFormat = __OutputLineFormat_JTextArea.getText().trim();
	String OutputLineFormatFile = __OutputLineFormatFile_JTextField.getText().trim();
	String LastOutputLineFormat = __LastOutputLineFormat_JTextArea.getText().trim();
    String DateTimeFormatterType = __DateTimeFormat_JPanel.getSelectedFormatterType().trim();
    String DateTimeFormat = __DateTimeFormat_JPanel.getText().trim();
	String OutputFileFooter = __OutputFileFooter_JTextArea.getText().trim();
	String OutputFileFooterFile = __OutputFileFooterFile_JTextField.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
    String MissingValue = __MissingValue_JTextField.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	String NonMissingOutputCount = __NonMissingOutputCount_JTextField.getText().trim();

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
    if ( OutputFileHeaderFile.length() > 0 ) {
        parameters.set ( "OutputFileHeaderFile", OutputFileHeaderFile );
    }
    if ( OutputLineFormat.length() > 0 ) {
        parameters.set ( "OutputLineFormat", OutputLineFormat );
    }
    if ( OutputLineFormatFile.length() > 0 ) {
        parameters.set ( "OutputLineFormatFile", OutputLineFormatFile );
    }
    if ( LastOutputLineFormat.length() > 0 ) {
        parameters.set ( "LastOutputLineFormat", LastOutputLineFormat );
    }
    if ( DateTimeFormatterType.length() > 0 ) {
        parameters.set ( "DateTimeFormatterType", DateTimeFormatterType );
    }
    if ( DateTimeFormat.length() > 0 ) {
        parameters.set ( "DateTimeFormat", DateTimeFormat );
    }
    if ( OutputFileFooter.length() > 0 ) {
        parameters.set ( "OutputFileFooter", OutputFileFooter );
    }
    if ( OutputFileFooterFile.length() > 0 ) {
        parameters.set ( "OutputFileFooterFile", OutputFileFooterFile );
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
    if ( NonMissingOutputCount.length() > 0 ) {
        parameters.set ( "NonMissingOutputCount", NonMissingOutputCount );
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
	String OutputFileHeaderFile = __OutputFileHeaderFile_JTextField.getText().trim();
	String OutputLineFormat = __OutputLineFormat_JTextArea.getText().trim();
	String OutputLineFormatFile = __OutputLineFormatFile_JTextField.getText().trim();
	String LastOutputLineFormat = __LastOutputLineFormat_JTextArea.getText().trim();
    String DateTimeFormatterType = __DateTimeFormat_JPanel.getSelectedFormatterType().trim();
    String DateTimeFormat = __DateTimeFormat_JPanel.getText().trim();
	String OutputFileFooter = __OutputFileFooter_JTextArea.getText().trim();
	String OutputFileFooterFile = __OutputFileFooterFile_JTextField.getText().trim();
	String Precision = __Precision_JTextField.getText().trim();
	String OutputStart = __OutputStart_JTextField.getText().trim();
	String OutputEnd = __OutputEnd_JTextField.getText().trim();
	String NonMissingOutputCount = __NonMissingOutputCount_JTextField.getText().trim();
	String MissingValue = __MissingValue_JTextField.getText().trim();
	__command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
	__command.setCommandParameter ( "OutputFile", OutputFile );
	__command.setCommandParameter ( "Append", Append );
	OutputFileHeader.replace("\n", "\\n");
	OutputFileHeader.replace("\r", "");
	__command.setCommandParameter ( "OutputFileHeader", OutputFileHeader );
	__command.setCommandParameter ( "OutputFileHeaderFile", OutputFileHeaderFile );
	__command.setCommandParameter ( "OutputLineFormat", OutputLineFormat );
	__command.setCommandParameter ( "OutputLineFormatFile", OutputLineFormatFile );
	__command.setCommandParameter ( "LastOutputLineFormat", LastOutputLineFormat );
	__command.setCommandParameter ( "DateTimeFormatterType", DateTimeFormatterType );
	__command.setCommandParameter ( "DateTimeFormat", DateTimeFormat );
	__command.setCommandParameter ( "OutputFileFooter", OutputFileFooter );
	OutputFileFooter.replace("\n", "\\n");
	OutputFileFooter.replace("\r", "");
	__command.setCommandParameter ( "OutputFileFooterFile", OutputFileFooterFile );
	__command.setCommandParameter ( "Precision", Precision );
	__command.setCommandParameter ( "OutputStart", OutputStart );
	__command.setCommandParameter ( "OutputEnd", OutputEnd );
	__command.setCommandParameter ( "NonMissingOutputCount", NonMissingOutputCount );
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
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Write one or more time series to a data stream format file," +
		" which consists of an optional header, a \"stream\" of time series value data records, and an optional footer." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Time series values are written one per line, NOT multiple columns." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for time series
    int yTS = 0;
    JPanel ts_JPanel = new JPanel();
    ts_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Time Series", ts_JPanel );
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    yTS = CommandEditorUtil.addTSListToEditorDialogPanel ( this, ts_JPanel, __TSList_JComboBox, yTS );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true );  // Allow edits
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTS = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, ts_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, yTS );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    yTS = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, ts_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, yTS );
    
    // Panel for output period
    // TODO SAM 2015-08-03 add output window at some point
    int yPeriod = -1;
    JPanel period_JPanel = new JPanel();
    period_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output Period", period_JPanel );
    
    JGUIUtil.addComponent(period_JPanel, new JLabel (
        "Specify output period date/times to a precision appropriate for time series."),
		0, ++yPeriod, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yPeriod, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(period_JPanel, new JLabel ("Output start:"), 
		0, ++yPeriod, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputStart_JTextField = new JTextField (10);
	__OutputStart_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(period_JPanel, __OutputStart_JTextField,
		1, yPeriod, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JLabel (
		"Optional - override the global output start (default=write all data)."),
		3, yPeriod, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(period_JPanel, new JLabel ( "Output end:"), 
		0, ++yPeriod, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputEnd_JTextField = new JTextField (10);
	__OutputEnd_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(period_JPanel, __OutputEnd_JTextField,
		1, yPeriod, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(period_JPanel, new JLabel (
		"Optional - override the global output end (default=write all data)."),
		3, yPeriod, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for output file
    int yFile = -1;
    JPanel file_JPanel = new JPanel();
    file_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Output File", file_JPanel );

    JGUIUtil.addComponent(file_JPanel, new JLabel (
		"The output filename can be specified using ${Property} notation to utilize global properties."),
		0, ++yFile, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	if ( __working_dir != null ) {
        JGUIUtil.addComponent(file_JPanel, new JLabel (
		"The working directory is: " + __working_dir ), 
		0, ++yFile, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	}
    JGUIUtil.addComponent(file_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yFile, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(file_JPanel, new JLabel ( "Data stream file to write:" ), 
		0, ++yFile, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__OutputFile_JTextField = new JTextField ( 45 );
	__OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseOutput_JButton = new SimpleJButton ( "...", this );
	__browseOutput_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browseOutput_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathOutput_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __pathOutput_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(file_JPanel, OutputFile_JPanel,
		1, yFile, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(file_JPanel, new JLabel ("Append to file?:"),
        0, ++yFile, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    List<String> Append_Vector = new Vector<String>();
    Append_Vector.add ( "" );
    Append_Vector.add ( __command._False );
    Append_Vector.add ( __command._True );
    __Append_JComboBox = new SimpleJComboBox(false);
    __Append_JComboBox.setData ( Append_Vector );
    __Append_JComboBox.addItemListener (this);
    JGUIUtil.addComponent(file_JPanel, __Append_JComboBox,
        1, yFile, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(file_JPanel, new JLabel (
        "Optional - whether to append to output file (default=" + __command._False + ")."),
        3, yFile, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for header
    int yHeader = -1;
    JPanel header_JPanel = new JPanel();
    header_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Header", header_JPanel );
    
    JGUIUtil.addComponent(header_JPanel, new JLabel (
        "Header content can be added to the top of the file."),
		0, ++yHeader, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(header_JPanel, new JLabel (
        "Use ${Property} notation to include processor properties in the header."),
		0, ++yHeader, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(header_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yHeader, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
    JGUIUtil.addComponent(header_JPanel, new JLabel ("Output file header (text):"),
        0, ++yHeader, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFileHeader_JTextArea = new JTextArea (3,35);
    __OutputFileHeader_JTextArea.setLineWrap ( true );
    __OutputFileHeader_JTextArea.setWrapStyleWord ( true );
    __OutputFileHeader_JTextArea.setToolTipText("Will be inserted at top of file");
    __OutputFileHeader_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(header_JPanel, new JScrollPane(__OutputFileHeader_JTextArea),
        1, yHeader, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(header_JPanel, new JLabel ("Optional - content to add at top of output file."),
        3, yHeader, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(header_JPanel, new JLabel ( "OR output file header (file):" ), 
        0, ++yHeader, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFileHeaderFile_JTextField = new JTextField ( 45 );
    __OutputFileHeaderFile_JTextField.setToolTipText("Specify a file that provides the header.");
    __OutputFileHeaderFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFileHeaderFile_JPanel = new JPanel();
	OutputFileHeaderFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFileHeaderFile_JPanel, __OutputFileHeaderFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseHeader_JButton = new SimpleJButton ( "...", this );
	__browseHeader_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFileHeaderFile_JPanel, __browseHeader_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathHeader_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFileHeaderFile_JPanel, __pathHeader_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(header_JPanel, OutputFileHeaderFile_JPanel,
		1, yHeader, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for time series data
    int yData = -1;
    JPanel data_JPanel = new JPanel();
    data_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Data", data_JPanel );
    
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "The data line format can contain literal text (commas, text, etc.) and the following:"),
		0, ++yData, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "   ${Property} - processor property"),
		0, ++yData, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "   ${ts:Property} - time series property"),
		0, ++yData, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "   %L, etc. - built-in time series properties (%L is location ID)"),
		0, ++yData, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "   ${tsdata:datetime} - date/time for data value, will be formatted using the date/time format specified below"),
		0, ++yData, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "   ${tsdata:value} - data value, formatted to precision specified below"),
		0, ++yData, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "   ${tsdata:flag} - data flag"),
		0, ++yData, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yData, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(data_JPanel, new JLabel ("Data line format:"),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputLineFormat_JTextArea = new JTextArea (3,35);
    __OutputLineFormat_JTextArea.setLineWrap ( true );
    __OutputLineFormat_JTextArea.setWrapStyleWord ( true );
    __OutputLineFormat_JTextArea.setToolTipText(
    	"Format used for each output line, including % TS specifiers, ${property}, ${ts:property}, ${tsdata:datetime}, ${tsdata:value}, ${tsdata:flag}.");
    __OutputLineFormat_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(data_JPanel, new JScrollPane(__OutputLineFormat_JTextArea),
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel ("Required (if format file not specified) - format for each data line."),
        3, yData, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "OR data line format file:" ), 
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputLineFormatFile_JTextField = new JTextField ( 45 );
    __OutputLineFormatFile_JTextField.setToolTipText("Specify a file that provides a template for the output line.");
    __OutputLineFormatFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputLineFormatFile_JPanel = new JPanel();
	OutputLineFormatFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputLineFormatFile_JPanel, __OutputLineFormatFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseFormat_JButton = new SimpleJButton ( "...", this );
	__browseFormat_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputLineFormatFile_JPanel, __browseFormat_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathFormat_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputLineFormatFile_JPanel, __pathFormat_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(data_JPanel, OutputLineFormatFile_JPanel,
		1, yData, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
    JGUIUtil.addComponent(data_JPanel, new JLabel ("Data line format (last line):"),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LastOutputLineFormat_JTextArea = new JTextArea (3,35);
    __LastOutputLineFormat_JTextArea.setLineWrap ( true );
    __LastOutputLineFormat_JTextArea.setWrapStyleWord ( true );
    __LastOutputLineFormat_JTextArea.setToolTipText(
    	"Format used for the last output line, including % TS specifiers, ${property}, ${ts:property}, ${tsdata:datetime}, ${tsdata:value}, ${tsdata:flag}.");
    __LastOutputLineFormat_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(data_JPanel, new JScrollPane(__LastOutputLineFormat_JTextArea),
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel ("Optional - format for last data line."),
        3, yData, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // TODO SAM 2012-04-10 Evaluate whether the formatter should just be the first part of the format, which
    // is supported by the panel
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Date/time format:" ), 
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __DateTimeFormat_JPanel = new DateTimeFormatterSpecifiersJPanel ( 20, true, true, null, true, false );
    __DateTimeFormat_JPanel.addKeyListener ( this );
    __DateTimeFormat_JPanel.addFormatterTypeItemListener (this); // Respond to changes in formatter choice
    __DateTimeFormat_JPanel.getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(data_JPanel, __DateTimeFormat_JPanel,
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel( "Required - format string for data date/time formatter."), 
        3, yData, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Output precision:" ),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Precision_JTextField = new JTextField ( "", 10 );
    __Precision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(data_JPanel, __Precision_JTextField,
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Optional - digits after decimal (default=4)."),
        3, yData, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Missing value:" ),
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __MissingValue_JTextField = new JTextField ( "", 10 );
    __MissingValue_JTextField.setToolTipText("Specify Blank to output a blank.");
    __MissingValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(data_JPanel, __MissingValue_JTextField,
        1, yData, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "Optional - value to write for missing data (default=initial missing value)."),
        3, yData, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(data_JPanel, new JLabel ( "Non-missing output count:"), 
        0, ++yData, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NonMissingOutputCount_JTextField = new JTextField (10);
    __NonMissingOutputCount_JTextField.setToolTipText ( "Useful to output last value.  Specify a negative number to output values at end.");
    __NonMissingOutputCount_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(data_JPanel, __NonMissingOutputCount_JTextField,
        1, yData, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(data_JPanel, new JLabel (
        "Optional - property to set for number of non-missing values output."),
        3, yData, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for footer
    int yFooter = -1;
    JPanel footer_JPanel = new JPanel();
    footer_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Footer", footer_JPanel );
    
    JGUIUtil.addComponent(footer_JPanel, new JLabel (
        "Footer content can be added to the bottom of the file."),
		0, ++yFooter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(footer_JPanel, new JLabel (
        "Use ${Property} notation to include processor properties in the footer."),
		0, ++yFooter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(footer_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yFooter, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(footer_JPanel, new JLabel ("Output file footer:"),
        0, ++yFooter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFileFooter_JTextArea = new JTextArea (3,35);
    __OutputFileFooter_JTextArea.setLineWrap ( true );
    __OutputFileFooter_JTextArea.setWrapStyleWord ( true );
    __OutputFileFooter_JTextArea.setToolTipText("Will be inserted at top of file");
    __OutputFileFooter_JTextArea.addKeyListener (this);
    JGUIUtil.addComponent(footer_JPanel, new JScrollPane(__OutputFileFooter_JTextArea),
        1, yFooter, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(footer_JPanel, new JLabel ("Optional - content to add at bottom of output file."),
        3, yFooter, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(footer_JPanel, new JLabel ( "OR output file footer (file):" ), 
        0, ++yFooter, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFileFooterFile_JTextField = new JTextField ( 45 );
    __OutputFileFooterFile_JTextField.setToolTipText("Specify a file that provides the header.");
    __OutputFileFooterFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel
	JPanel OutputFileFooterFile_JPanel = new JPanel();
	OutputFileFooterFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFileFooterFile_JPanel, __OutputFileFooterFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browseFooter_JButton = new SimpleJButton ( "...", this );
	__browseFooter_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFileFooterFile_JPanel, __browseFooter_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path...
		__pathFooter_JButton = new SimpleJButton( __RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFileFooterFile_JPanel, __pathFooter_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(footer_JPanel, OutputFileFooterFile_JPanel,
		1, yFooter, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
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

	__ok_JButton = new SimpleJButton("OK", "OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
	
	// Refresh the contents...
    checkGUIState();
    refresh ();
    pack();
    JGUIUtil.center( this );
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
{	String routine = getClass().getSimpleName() + ".refresh";
	String OutputFile = "";
	String Append = "";
	String OutputFileHeader = "";
	String OutputFileHeaderFile = "";
	String OutputLineFormat = "";
	String OutputLineFormatFile = "";
	String LastOutputLineFormat = "";
    String dateTimeFormatterType = "";
    String DateTimeFormat = "";
	String OutputFileFooter = "";
	String OutputFileFooterFile = "";
	String Precision = "";
	String MissingValue = "";
	String OutputStart = "";
	String OutputEnd = "";
	String NonMissingOutputCount = "";
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
		OutputFileHeaderFile = parameters.getValue ( "OutputFileHeaderFile" );
		OutputLineFormat = parameters.getValue ( "OutputLineFormat" );
		dateTimeFormatterType = parameters.getValue ( "DateTimeFormatterType" );
        DateTimeFormat = parameters.getValue ( "DateTimeFormat" );
		OutputLineFormatFile = parameters.getValue ( "OutputLineFormatFile" );
		LastOutputLineFormat = parameters.getValue ( "LastOutputLineFormat" );
		OutputFileFooter = parameters.getValue ( "OutputFileFooter" );
		OutputFileFooterFile = parameters.getValue ( "OutputFileFooterFile" );
	    Precision = parameters.getValue("Precision");
	    MissingValue = parameters.getValue("MissingValue");
		OutputStart = parameters.getValue ( "OutputStart" );
		OutputEnd = parameters.getValue ( "OutputEnd" );
		NonMissingOutputCount = parameters.getValue ( "NonMissingOutputCount" );
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
	    	// Replace \n in file with actual newline
	    	OutputFileHeader.replace("\\n","\n");
	         __OutputFileHeader_JTextArea.setText(OutputFileHeader);
	    }
	    if (OutputFileHeaderFile != null) {
	         __OutputFileHeaderFile_JTextField.setText(OutputFileHeaderFile);
	    }
        if (OutputLineFormat != null) {
            __OutputLineFormat_JTextArea.setText(OutputLineFormat);
        }
        if (OutputLineFormatFile != null) {
            __OutputLineFormatFile_JTextField.setText(OutputLineFormatFile);
        }
        if (LastOutputLineFormat != null) {
            __LastOutputLineFormat_JTextArea.setText(LastOutputLineFormat);
        }
        if ( (dateTimeFormatterType == null) || dateTimeFormatterType.equals("") ) {
            // Select default...
            __DateTimeFormat_JPanel.selectFormatterType(null);
        }
        else {
            try {
                __DateTimeFormat_JPanel.selectFormatterType(DateTimeFormatterType.valueOfIgnoreCase(dateTimeFormatterType));
            }
            catch ( Exception e ) {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nDateTimeFormatterType value \"" + dateTimeFormatterType +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( DateTimeFormat != null ) {
            __DateTimeFormat_JPanel.setText ( DateTimeFormat );
        }
        if (OutputFileFooter != null) {
        	OutputFileFooter.replace("\\n","\n");
             __OutputFileFooter_JTextArea.setText(OutputFileFooter);
        }
	    if (OutputFileFooterFile != null) {
	         __OutputFileFooterFile_JTextField.setText(OutputFileFooterFile);
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
		if ( NonMissingOutputCount != null ) {
            __NonMissingOutputCount_JTextField.setText (NonMissingOutputCount);
        }
	}
	// Regardless, reset the command from the fields...
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
	OutputFile = __OutputFile_JTextField.getText().trim();
	Append = __Append_JComboBox.getSelected();
	OutputFileHeader = __OutputFileHeader_JTextArea.getText().trim();
	OutputFileHeaderFile = __OutputFileHeaderFile_JTextField.getText().trim();
	OutputLineFormat = __OutputLineFormat_JTextArea.getText().trim();
	OutputLineFormatFile = __OutputLineFormatFile_JTextField.getText().trim();
	LastOutputLineFormat = __LastOutputLineFormat_JTextArea.getText().trim();
    dateTimeFormatterType = __DateTimeFormat_JPanel.getSelectedFormatterType().trim();
    DateTimeFormat = __DateTimeFormat_JPanel.getText().trim();
	OutputFileFooter = __OutputFileFooter_JTextArea.getText().trim();
	OutputFileFooterFile = __OutputFileFooterFile_JTextField.getText().trim();
	Precision = __Precision_JTextField.getText().trim();
	MissingValue = __MissingValue_JTextField.getText().trim();
	OutputStart = __OutputStart_JTextField.getText().trim();
	OutputEnd = __OutputEnd_JTextField.getText().trim();
	NonMissingOutputCount = __NonMissingOutputCount_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
	parameters.add ( "OutputFile=" + OutputFile );
	parameters.add ( "Append=" + Append );
	parameters.add ( "OutputFileHeader=" + OutputFileHeader );
	parameters.add ( "OutputFileHeaderFile=" + OutputFileHeaderFile );
	parameters.add ( "OutputLineFormat=" + OutputLineFormat );
	parameters.add ( "OutputLineFormatFile=" + OutputLineFormatFile );
	parameters.add ( "LastOutputLineFormat=" + LastOutputLineFormat );
	parameters.add ( "DateTimeFormatterType=" + dateTimeFormatterType );
	parameters.add ( "DateTimeFormat=" + DateTimeFormat );
	parameters.add ( "OutputFileFooter=" + OutputFileFooter );
	parameters.add ( "OutputFileFooterFile=" + OutputFileFooterFile );
	parameters.add ( "Precision=" + Precision );
	parameters.add ( "MissingValue=" + MissingValue );
	parameters.add ( "OutputStart=" + OutputStart );
	parameters.add ( "OutputEnd=" + OutputEnd );
	parameters.add ( "NonMissingOutputCount=" + NonMissingOutputCount );
	__command_JTextArea.setText( __command.toString ( parameters ).trim() );
	// Check the path and determine what the label on the path button should be...
	if ( __pathOutput_JButton != null ) {
		if ( (OutputFile != null) && !OutputFile.isEmpty() ) {
			__pathOutput_JButton.setEnabled ( true );
			File f = new File ( OutputFile );
			if ( f.isAbsolute() ) {
				__pathOutput_JButton.setText ( __RemoveWorkingDirectory );
				__pathOutput_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathOutput_JButton.setText ( __AddWorkingDirectory );
            	__pathOutput_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathOutput_JButton.setEnabled(false);
		}
	}
	// Check the path and determine what the label on the path button should be...
	if ( __pathHeader_JButton != null ) {
		if ( (OutputFileHeaderFile != null) && !OutputFileHeaderFile.isEmpty() ) {
			__pathHeader_JButton.setEnabled ( true );
			File f = new File ( OutputFileHeaderFile );
			if ( f.isAbsolute() ) {
				__pathHeader_JButton.setText ( __RemoveWorkingDirectory );
				__pathHeader_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathHeader_JButton.setText ( __AddWorkingDirectory );
            	__pathHeader_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathHeader_JButton.setEnabled(false);
		}
	}
	// Check the path and determine what the label on the path button should be...
	if ( __pathFormat_JButton != null ) {
		if ( (OutputLineFormatFile != null) && !OutputLineFormatFile.isEmpty() ) {
			__pathFormat_JButton.setEnabled ( true );
			File f = new File ( OutputLineFormatFile );
			if ( f.isAbsolute() ) {
				__pathFormat_JButton.setText ( __RemoveWorkingDirectory );
				__pathFormat_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathFormat_JButton.setText ( __AddWorkingDirectory );
            	__pathFormat_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathFormat_JButton.setEnabled(false);
		}
	}
	// Check the path and determine what the label on the path button should be...
	if ( __pathFooter_JButton != null ) {
		if ( (OutputFileFooterFile != null) && !OutputFileFooterFile.isEmpty() ) {
			__pathFooter_JButton.setEnabled ( true );
			File f = new File ( OutputFileFooterFile );
			if ( f.isAbsolute() ) {
				__pathFooter_JButton.setText ( __RemoveWorkingDirectory );
				__pathFooter_JButton.setToolTipText("Change path to relative to command file");
			}
			else {
            	__pathFooter_JButton.setText ( __AddWorkingDirectory );
            	__pathFooter_JButton.setToolTipText("Change path to absolute");
			}
		}
		else {
			__pathFooter_JButton.setEnabled(false);
		}
	}
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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
