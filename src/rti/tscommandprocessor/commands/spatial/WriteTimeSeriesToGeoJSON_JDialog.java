// WriteTimeSeriesToGeoJSON_JDialog - Command editor dialog for the WriteTimeSeriesToGeoJSON() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.spatial;

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

/**
Command editor dialog for the WriteTimeSeriesToGeoJSON() command.
*/
@SuppressWarnings("serial")
public class WriteTimeSeriesToGeoJSON_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, ItemListener, WindowListener
{

private final String __AddWorkingDirectory = "Abs";
private final String __RemoveWorkingDirectory = "Rel";

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __browse_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private SimpleJButton __path_JButton = null;
private WriteTimeSeriesToGeoJSON_Command __command = null;
private String __working_dir = null;
private JTextArea __command_JTextArea=null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __TSList_JComboBox = null;
private JLabel __TSID_JLabel = null;
private SimpleJComboBox __TSID_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTextField __OutputFile_JTextField = null;
private SimpleJComboBox __Append_JComboBox = null;
private JTextField __LongitudeProperty_JTextField = null;
private JTextField __LatitudeProperty_JTextField = null;
private JTextField __CoordinatePrecision_JTextField = null;
private JTextField __ElevationProperty_JTextField = null;
private JTextField __WKTGeometryProperty_JTextField = null;
private JTextField __IncludeProperties_JTextField = null;
private JTextField __ExcludeProperties_JTextField = null;
private JTextField __JavaScriptVar_JTextField = null;
private JTextField __PrependText_JTextField = null;
private JTextField __AppendText_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false; // Has user pressed OK to close the dialog.

/**
Dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public WriteTimeSeriesToGeoJSON_JDialog ( JFrame parent, WriteTimeSeriesToGeoJSON_Command command )
{   super(parent, true);
    initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{   Object o = event.getSource();

    if ( o == __browse_JButton ) {
        String last_directory_selected = JGUIUtil.getLastFileDialogDirectory();
        JFileChooser fc = null;
        if ( last_directory_selected != null ) {
            fc = JFileChooserFactory.createJFileChooser( last_directory_selected );
        }
        else {
            fc = JFileChooserFactory.createJFileChooser( __working_dir );
        }
        fc.setDialogTitle("Select GeoJSON File to Write");
        SimpleFileFilter sff = new SimpleFileFilter("json", "GeoJSON");
        fc.addChoosableFileFilter(sff);
        fc.addChoosableFileFilter (new SimpleFileFilter("js", "GeoJSON JavaScript"));
        
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
					Message.printWarning ( 1,"WriteTimeSeriesToGeoJSON_JDialog", "Error converting file to relative path." );
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
		HelpViewer.getInstance().showHelp("command", "WriteTimeSeriesToGeoJSON");
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
                Message.printWarning ( 1, "WriteTimeSeriesToGeoJSON_JDialog", "Error converting file to relative path." );
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
private void checkGUIState () {
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
{   // Create a list of parameters to check.
    PropList parameters = new PropList ( "" );
    String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String Append = __Append_JComboBox.getSelected();
    String LongitudeProperty = __LongitudeProperty_JTextField.getText().trim();
    String LatitudeProperty = __LatitudeProperty_JTextField.getText().trim();
    String CoordinatePrecision = __CoordinatePrecision_JTextField.getText().trim();
    String ElevationProperty = __ElevationProperty_JTextField.getText().trim();
    String WKTGeometryProperty = __WKTGeometryProperty_JTextField.getText().trim();
    String IncludeProperties = __IncludeProperties_JTextField.getText().trim();
    String ExcludeProperties = __ExcludeProperties_JTextField.getText().trim();
    String JavaScriptVar = __JavaScriptVar_JTextField.getText().trim();
    String PrependText = __PrependText_JTextField.getText().trim();
    String AppendText = __AppendText_JTextField.getText().trim();

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
    if ( LongitudeProperty.length() > 0 ) {
        parameters.set ( "LongitudeProperty", LongitudeProperty );
    }
    if ( LatitudeProperty.length() > 0 ) {
        parameters.set ( "LatitudeProperty", LatitudeProperty );
    }
    if ( CoordinatePrecision.length() > 0 ) {
        parameters.set ( "CoordinatePrecision", CoordinatePrecision );
    }
    if ( ElevationProperty.length() > 0 ) {
        parameters.set ( "ElevationProperty", ElevationProperty );
    }
    if ( WKTGeometryProperty.length() > 0 ) {
        parameters.set ( "WKTGeometryProperty", WKTGeometryProperty );
    }
    if ( IncludeProperties.length() > 0 ) {
        parameters.set ( "IncludeProperties", IncludeProperties );
    }
    if ( ExcludeProperties.length() > 0 ) {
        parameters.set ( "ExcludeProperties", ExcludeProperties );
    }
    if ( JavaScriptVar.length() > 0 ) {
        parameters.set ( "JavaScriptVar", JavaScriptVar );
    }
    if ( PrependText.length() > 0 ) {
        parameters.set ( "PrependText", PrependText );
    }
    if ( AppendText.length() > 0 ) {
        parameters.set ( "AppendText", AppendText );
    }
    try {
        // This will warn the user.
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
{   String TSList = __TSList_JComboBox.getSelected();
    String TSID = __TSID_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();  
    String OutputFile = __OutputFile_JTextField.getText().trim();
    String Append = __Append_JComboBox.getSelected();
    String LongitudeProperty = __LongitudeProperty_JTextField.getText().trim();
    String LatitudeProperty = __LatitudeProperty_JTextField.getText().trim();
    String CoordinatePrecision = __CoordinatePrecision_JTextField.getText().trim();
    String ElevationProperty = __ElevationProperty_JTextField.getText().trim();
    String WKTGeometryProperty = __WKTGeometryProperty_JTextField.getText().trim();
    String IncludeProperties = __IncludeProperties_JTextField.getText().trim();
    String ExcludeProperties = __ExcludeProperties_JTextField.getText().trim();
    String JavaScriptVar = __JavaScriptVar_JTextField.getText().trim();
    String PrependText = __PrependText_JTextField.getText().trim();
    String AppendText = __AppendText_JTextField.getText().trim();
    __command.setCommandParameter ( "TSList", TSList );
    __command.setCommandParameter ( "TSID", TSID );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "OutputFile", OutputFile );
    __command.setCommandParameter ( "Append", Append );
    __command.setCommandParameter ( "LongitudeProperty", LongitudeProperty );
    __command.setCommandParameter ( "LatitudeProperty", LatitudeProperty );
    __command.setCommandParameter ( "CoordinatePrecision", CoordinatePrecision );
    __command.setCommandParameter ( "ElevationProperty", ElevationProperty );
    __command.setCommandParameter ( "WKTGeometryProperty", WKTGeometryProperty );
    __command.setCommandParameter ( "IncludeProperties", IncludeProperties );
    __command.setCommandParameter ( "ExcludeProperties", ExcludeProperties );
    __command.setCommandParameter ( "JavaScriptVar", JavaScriptVar );
    __command.setCommandParameter ( "PrependText", PrependText );
    __command.setCommandParameter ( "AppendText", AppendText );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WriteTimeSeriesToGeoJSON_Command command )
{   __command = command;
    CommandProcessor processor = __command.getCommandProcessor();
    __working_dir = TSCommandProcessorUtil.getWorkingDirForCommand ( processor, __command );

    addWindowListener( this );

    Insets insetsTLBR = new Insets(1,2,1,2);

    JPanel main_JPanel = new JPanel();
    main_JPanel.setLayout( new GridBagLayout() );
    getContentPane().add ( "North", main_JPanel );
    int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Write time series to a GeoJSON file, for use in spatial data processing and visualization." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Longitude, latitude, elevation, and other GeoJSON values are taken from time series properties." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    if ( __working_dir != null ) {
        JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The working directory is: " + __working_dir ), 
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    }
    JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    __TSList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addTSListToEditorDialogPanel ( this, main_JPanel, __TSList_JComboBox, y );

    __TSID_JLabel = new JLabel ("TSID (for TSList=" + TSListType.ALL_MATCHING_TSID.toString() + "):");
    __TSID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __TSID_JComboBox.setToolTipText("Select a time series TSID/alias from the list or specify with ${Property} notation");
    List<String> tsids = TSCommandProcessorUtil.getTSIdentifiersNoInputFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel ( this, this, main_JPanel, __TSID_JLabel, __TSID_JComboBox, tsids, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for TSList=" + TSListType.ENSEMBLE_ID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select a time series ensemble ID from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "GeoJSON file to write:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputFile_JTextField = new JTextField ( 50 );
    __OutputFile_JTextField.setToolTipText("Specify the output file or specify with ${Property} notation");
    __OutputFile_JTextField.addKeyListener ( this );
    // Output file layout fights back with other rows so put in its own panel.
	JPanel OutputFile_JPanel = new JPanel();
	OutputFile_JPanel.setLayout(new GridBagLayout());
    JGUIUtil.addComponent(OutputFile_JPanel, __OutputFile_JTextField,
		0, 0, 1, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST );
	__browse_JButton = new SimpleJButton ( "...", this );
	__browse_JButton.setToolTipText("Browse for file");
    JGUIUtil.addComponent(OutputFile_JPanel, __browse_JButton,
		1, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	if ( __working_dir != null ) {
		// Add the button to allow conversion to/from relative path.
		__path_JButton = new SimpleJButton(	__RemoveWorkingDirectory,this);
		JGUIUtil.addComponent(OutputFile_JPanel, __path_JButton,
			2, 0, 1, 1, 0.0, 0.0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.CENTER);
	}
	JGUIUtil.addComponent(main_JPanel, OutputFile_JPanel,
		1, y, 6, 1, 1.0, 0.0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Append to GeoJSON file?:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Append_JComboBox = new SimpleJComboBox ( false ); // Allow edit.
    __Append_JComboBox.add ( "" );
    __Append_JComboBox.add ( __command._False );
    __Append_JComboBox.add ( __command._True );
    __Append_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __Append_JComboBox,
        1, ++y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel( "Optional - append content to file? (default=" + __command._False + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    __main_JTabbedPane = new JTabbedPane ();
    //__main_JTabbedPane.setBorder(
    //    BorderFactory.createTitledBorder ( BorderFactory.createLineBorder(Color.black),
    //    "Specify SQL" ));
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel for point data in separate properties.
    int yPoint = -1;
    JPanel point_JPanel = new JPanel();
    point_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Point Data", point_JPanel );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel (
        "If the time series are associated with a point layer, then spatial information can be specified from time series properties (below)."),
        0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel (
        "The property names are case-specific."),
        0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel (
        "Otherwise, specify shape data using parameters in the Geometry Data tab."),
        0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yPoint, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Longitude (X) property:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LongitudeProperty_JTextField = new JTextField ( "", 20 );
    __LongitudeProperty_JTextField.setToolTipText("Specify the longitude property, can use ${Property} notation");
    __LongitudeProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __LongitudeProperty_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Required - time series property containing longitude."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Latitude (Y) property:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __LatitudeProperty_JTextField = new JTextField ( "", 20 );
    __LatitudeProperty_JTextField.setToolTipText("Specify the latitude property, can use ${Property} notation");
    __LatitudeProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __LatitudeProperty_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Required - time series property containing latitude."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Coordinate precision:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __CoordinatePrecision_JTextField = new JTextField ( "", 20 );
    __CoordinatePrecision_JTextField.setToolTipText("Coordinate precision, digits after decimal point.");
    __CoordinatePrecision_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __CoordinatePrecision_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Optional - digits after decimal (default=data precision)."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Elevation (Z) property:" ),
        0, ++yPoint, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ElevationProperty_JTextField = new JTextField ( "", 20 );
    __ElevationProperty_JTextField.setToolTipText("Specify the elevation property, can use ${Property} notation");
    __ElevationProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(point_JPanel, __ElevationProperty_JTextField,
        1, yPoint, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(point_JPanel, new JLabel ( "Optional - time series property containing elevation (default=X,Y)."),
        3, yPoint, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for geometry data in WKT property.
    int yGeom = -1;
    JPanel geom_JPanel = new JPanel();
    geom_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Geometry Data", geom_JPanel );
    
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Geometry (shape) data can be specified using Well Known Text (WKT) strings in a time series property."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Currently only POINT and POLYGON geometry are recognized but support for other geometry types will be added in the future."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel (
        "Coordinates in the WKT strings must be geographic (longitude and latitude decimal degrees)."),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
        0, ++yGeom, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(geom_JPanel, new JLabel ( "WKT geometry property:" ),
        0, ++yGeom, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __WKTGeometryProperty_JTextField = new JTextField ( "", 20 );
    __WKTGeometryProperty_JTextField.setToolTipText("Specify the WKT geometry property, can use ${Property} notation");
    __WKTGeometryProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(geom_JPanel, __WKTGeometryProperty_JTextField,
        1, yGeom, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(geom_JPanel, new JLabel ( "Optional - time series property WKT geometry."),
        3, yGeom, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for properties.
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Properties", prop_JPanel );
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "Specify time series properties to be output in the GeoJSON feature \"properties\" list."),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Include properties:" ),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IncludeProperties_JTextField = new JTextField ( "", 30 );
    __IncludeProperties_JTextField.setToolTipText("Names of time series properties to include, can use ${Property}");
    __IncludeProperties_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __IncludeProperties_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Optional - properties to include (default=include all)."),
        3, yProp, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Exclude properties:" ),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __ExcludeProperties_JTextField = new JTextField ( "", 30 );
    __ExcludeProperties_JTextField.setToolTipText("Names of time series properties to exclude, can use ${Property}");
    __ExcludeProperties_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(prop_JPanel, __ExcludeProperties_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ( "Optional - properties to exclude (default=exclude none)."),
        3, yProp, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for JavaScript.
    int yJs = -1;
    JPanel js_JPanel = new JPanel();
    js_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "JavaScript", js_JPanel );
    
    JGUIUtil.addComponent(js_JPanel, new JLabel (
        "The default is to output GeoJSon in a format similar to the following:"),
        0, ++yJs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(js_JPanel, new JLabel (
        "<html><pre>{\n" +
    	"  \"type\": \"FeatureCollection\",\n" +
    	"  \"features\": [\n" +
    	"    {\n" +
    	"      \"type\": \"Feature\",\n" +
    	"      \"properties\": {\n" +
      	"      }\n" +
      	"      \"geometry\": {\n" +
      	"        \"type\": \"Point\",\n" +
      	"        \"coordinates\": [-105.89194, 38.99333]\n" +
      	"      }\n" +
      	"    }, { repeat for each feature },...\n" +
      	"  ]\n" +
    	"}</pre></html>"),
        0, ++yJs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(js_JPanel, new JLabel (
        "The entire output will correspond to one JavaScript object."),
        0, ++yJs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(js_JPanel, new JLabel (
        "However, if a JavaScript variable is specified, the object will be assigned to a JavaScript variable.  "
        + "This allows direct use of the file in a website"),
        0, ++yJs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(js_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yJs, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(js_JPanel, new JLabel ( "JavaScript variable:" ),
        0, ++yJs, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __JavaScriptVar_JTextField = new JTextField ( "", 20 );
    __JavaScriptVar_JTextField.setToolTipText("JavaScript variable, can use ${Property}");
    __JavaScriptVar_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(js_JPanel, __JavaScriptVar_JTextField,
        1, yJs, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(js_JPanel, new JLabel ( "Optional - JavaScript variable for GeoJSON object (default=none)."),
        3, yJs, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel for inserts.
    int yInsert = -1;
    JPanel insert_JPanel = new JPanel();
    insert_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Inserts", insert_JPanel );
    
    JGUIUtil.addComponent(insert_JPanel, new JLabel (
        "Specify text to insert before and after the GeoJSON.  For example, use the following to initialize the object in an array:"),
        0, ++yInsert, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JLabel ( "prepend:  var stationData = []; stationData['Org1'] = "),
        0, ++yInsert, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JLabel ( "append:  ;"),
        0, ++yInsert, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
        0, ++yInsert, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(insert_JPanel, new JLabel ( "Prepend text:" ),
        0, ++yInsert, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PrependText_JTextField = new JTextField ( "", 35 );
    __PrependText_JTextField.setToolTipText("Text to prepend before GeoJSON - can include ${Property}");
    __PrependText_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(insert_JPanel, __PrependText_JTextField,
        1, yInsert, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JLabel ( "Optional - text to prepend before GeoJSON object (default=none)."),
        3, yInsert, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(insert_JPanel, new JLabel ( "Append text:" ),
        0, ++yInsert, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __AppendText_JTextField = new JTextField ( "", 35 );
    __AppendText_JTextField.setToolTipText("Text to append at end of GeoJSON - can include ${Property}");
    __AppendText_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(insert_JPanel, __AppendText_JTextField,
        1, yInsert, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(insert_JPanel, new JLabel ( "Optional - text to append after GeoJSON object (default=none)."),
        3, yInsert, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

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
    
    // Refresh the contents.
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
{   int code = event.getKeyCode();

    if ( code == KeyEvent.VK_ENTER ) {
        refresh ();
        checkInput();
        if ( !__error_wait ) {
            response ( true );
        }
    }
}

public void keyReleased ( KeyEvent event )
{   refresh();
}

public void keyTyped ( KeyEvent event )
{
}

/**
Indicate if the user pressed OK (cancel otherwise).
@return true if the edits were committed, false if the user canceled.
*/
public boolean ok ()
{   return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{   String routine = getClass().getSimpleName() + ".refresh";
	String TSList = "";
	String TSID = "";
	String EnsembleID = "";
    String OutputFile = "";
    String Append = "";
    String LongitudeProperty = "";
    String LatitudeProperty = "";
    String CoordinatePrecision = "";
    String ElevationProperty = "";
    String WKTGeometryProperty = "";
    String IncludeProperties = "";
    String ExcludeProperties = "";
    String JavaScriptVar = "";
    String PrependText = "";
    String AppendText = "";
    __error_wait = false;
    PropList parameters = null;
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command.
        parameters = __command.getCommandParameters();
        TSList = parameters.getValue ( "TSList" );
        TSID = parameters.getValue ( "TSID" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        OutputFile = parameters.getValue ( "OutputFile" );
        Append = parameters.getValue ( "Append" );
        LongitudeProperty = parameters.getValue ( "LongitudeProperty" );
        LatitudeProperty = parameters.getValue ( "LatitudeProperty" );
        CoordinatePrecision = parameters.getValue ( "CoordinatePrecision" );
        ElevationProperty = parameters.getValue ( "ElevationProperty" );
        WKTGeometryProperty = parameters.getValue ( "WKTGeometryProperty" );
        IncludeProperties = parameters.getValue ( "IncludeProperties" );
        ExcludeProperties = parameters.getValue ( "ExcludeProperties" );
        JavaScriptVar = parameters.getValue ( "JavaScriptVar" );
        PrependText = parameters.getValue ( "PrependText" );
        AppendText = parameters.getValue ( "AppendText" );
        if ( TSList == null ) {
            // Select default.
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
            // Automatically add to the list after the blank.
            if ( (TSID != null) && (TSID.length() > 0) ) {
                __TSID_JComboBox.insertItemAt ( TSID, 1 );
                // Select.
                __TSID_JComboBox.select ( TSID );
            }
            else {
                // Select the blank.
                __TSID_JComboBox.select ( 0 );
            }
        }
        if ( EnsembleID == null ) {
            // Select default.
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
            // Select default.
            __Append_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Append_JComboBox, Append, JGUIUtil.NONE, null, null ) ) {
                __Append_JComboBox.select ( Append );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nAppend value \"" + Append +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( LongitudeProperty != null ) {
            __LongitudeProperty_JTextField.setText (LongitudeProperty);
        }
        if ( LatitudeProperty != null ) {
            __LatitudeProperty_JTextField.setText (LatitudeProperty);
        }
        if ( CoordinatePrecision != null ) {
            __CoordinatePrecision_JTextField.setText (CoordinatePrecision);
        }
        if ( ElevationProperty != null ) {
            __ElevationProperty_JTextField.setText (ElevationProperty);
        }
        if ( WKTGeometryProperty != null ) {
            __WKTGeometryProperty_JTextField.setText (WKTGeometryProperty);
        }
        if ( IncludeProperties != null ) {
            __IncludeProperties_JTextField.setText (IncludeProperties);
        }
        if ( ExcludeProperties != null ) {
            __ExcludeProperties_JTextField.setText (ExcludeProperties);
        }
        if ( JavaScriptVar != null ) {
            __JavaScriptVar_JTextField.setText (JavaScriptVar);
        }
        if ( PrependText != null ) {
            __PrependText_JTextField.setText (PrependText);
        }
        if ( AppendText != null ) {
            __AppendText_JTextField.setText (AppendText);
        }
    }
    // Regardless, reset the command from the fields.
    TSList = __TSList_JComboBox.getSelected();
    TSID = __TSID_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    OutputFile = __OutputFile_JTextField.getText().trim();
    Append = __Append_JComboBox.getSelected();
    LongitudeProperty = __LongitudeProperty_JTextField.getText().trim();
    LatitudeProperty = __LatitudeProperty_JTextField.getText().trim();
    CoordinatePrecision = __CoordinatePrecision_JTextField.getText().trim();
    ElevationProperty = __ElevationProperty_JTextField.getText().trim();
    WKTGeometryProperty = __WKTGeometryProperty_JTextField.getText().trim();
    IncludeProperties = __IncludeProperties_JTextField.getText().trim();
    ExcludeProperties = __ExcludeProperties_JTextField.getText().trim();
    JavaScriptVar = __JavaScriptVar_JTextField.getText().trim();
    PrependText = __PrependText_JTextField.getText().trim();
    AppendText = __AppendText_JTextField.getText().trim();
    parameters = new PropList ( __command.getCommandName() );
    parameters.add ( "TSList=" + TSList );
    parameters.add ( "TSID=" + TSID );
    parameters.add ( "EnsembleID=" + EnsembleID );
    parameters.add ( "OutputFile=" + OutputFile );
    parameters.add ( "Append=" + Append );
    parameters.add ( "LongitudeProperty=" + LongitudeProperty );
    parameters.add ( "LatitudeProperty=" + LatitudeProperty );
    parameters.add ( "CoordinatePrecision=" + CoordinatePrecision );
    parameters.add ( "ElevationProperty=" + ElevationProperty );
    parameters.add ( "WKTGeometryProperty=" + WKTGeometryProperty );
    parameters.add ( "IncludeProperties=" + IncludeProperties );
    parameters.add ( "ExcludeProperties=" + ExcludeProperties );
    parameters.add ( "JavaScriptVar=" + JavaScriptVar );
    parameters.add ( "PrependText=" + PrependText );
    parameters.add ( "AppendText=" + AppendText );
    __command_JTextArea.setText( __command.toString ( parameters ).trim() );
	// Check the path and determine what the label on the path button should be.
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
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
*/
private void response ( boolean ok )
{   __ok = ok;  // Save to be returned by ok().
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
public void windowClosing( WindowEvent event )
{   response ( false );
}

public void windowActivated( WindowEvent evt ){;}
public void windowClosed( WindowEvent evt ){;}
public void windowDeactivated( WindowEvent evt ){;}
public void windowDeiconified( WindowEvent evt ){;}
public void windowIconified( WindowEvent evt ){;}
public void windowOpened( WindowEvent evt ){;}

}