// GeoMap_JDialog - editor for GeoMap command

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

import org.openwaterfoundation.geoprocessor.core.GeoLayerFormatType;
import org.openwaterfoundation.geoprocessor.core.GeoLayerGeometryType;
import org.openwaterfoundation.geoprocessor.core.GeoLayerType;

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
import java.util.ArrayList;
import java.util.List;

import RTi.Util.GUI.DictionaryJDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

@SuppressWarnings("serial")
public class GeoMap_JDialog extends JDialog
implements ActionListener, ItemListener, KeyListener, WindowListener
{
	// Tab positions.
	private final int newTabIndex = 0;
	private final int addLayerTabIndex = 1;
	private final int addLayerViewGroupTabIndex = 2;
	private final int addLayerViewTabIndex = 3;

	private boolean errorWait = false; // To track errors.
	private boolean firstTime = true; // Indicate first time display.
	private JTextArea command_JTextArea = null;
	private JTabbedPane main_JTabbedPane = null;
	// General.
	private SimpleJComboBox MapCommand_JComboBox = null;
	private JTextField GeoMapProjectID_JTextField = null;
	private JTextField GeoMapID_JTextField = null;
	// New.
	private JTextField NewGeoMapID_JTextField = null;
	private JTextField GeoMapName_JTextField = null;
	private JTextField GeoMapDescription_JTextField = null;
	private JTextArea GeoMapProperties_JTextArea = null;
	// Layer.
	private JTextField GeoLayerID_JTextField = null;
	private JTextField GeoLayerName_JTextField = null;
	private JTextField GeoLayerDescription_JTextField = null;
	private SimpleJComboBox GeoLayerCrs_JComboBox = null;
	private SimpleJComboBox GeoLayerGeometryType_JComboBox = null;
	private SimpleJComboBox GeoLayerLayerType_JComboBox = null;
	private JTextArea GeoLayerProperties_JTextArea = null;
	private SimpleJComboBox GeoLayerSourceFormat_JComboBox = null;
	private JTextField GeoLayerSourcePath_JTextField = null;
	// Layer View Group.
	private JTextField GeoLayerViewGroupID_JTextField = null;
	private JTextField GeoLayerViewGroupName_JTextField = null;
	private JTextField GeoLayerViewGroupDescription_JTextField = null;
	private JTextArea GeoLayerViewGroupProperties_JTextArea = null;
	private JTextField GeoLayerViewGroupInsertPosition_JTextField = null;
	private JTextField GeoLayerViewGroupInsertBefore_JTextField = null;
	private JTextField GeoLayerViewGroupInsertAfter_JTextField = null;
	// Layer View.
	private JTextField GeoLayerViewID_JTextField = null;
	private JTextField GeoLayerViewName_JTextField = null;
	private JTextField GeoLayerViewDescription_JTextField = null;
	private JTextArea GeoLayerViewProperties_JTextArea = null;
	private JTextField GeoLayerViewInsertPosition_JTextField = null;
	private JTextField GeoLayerViewInsertBefore_JTextField = null;
	private JTextField GeoLayerViewInsertAfter_JTextField = null;
	private JTextField GeoLayerViewLayerID_JTextField = null;
	private JTabbedPane symbol_JTabbedPane = null;
	// Layer View - Layer.
	// Layer View - Single symbol.
	private JTextField SingleSymbolID_JTextField = null;
	private JTextField SingleSymbolName_JTextField = null;
	private JTextField SingleSymbolDescription_JTextField = null;
	private JTextArea SingleSymbolProperties_JTextArea = null;
	// Layer View - Categorized symbol.
	private JTextField CategorizedSymbolID_JTextField = null;
	private JTextField CategorizedSymbolName_JTextField = null;
	private JTextField CategorizedSymbolDescription_JTextField = null;
	private JTextArea CategorizedSymbolProperties_JTextArea = null;
	// Layer View - Graduated symbol.
	private JTextField GraduatedSymbolID_JTextField = null;
	private JTextField GraduatedSymbolName_JTextField = null;
	private JTextField GraduatedSymbolDescription_JTextField = null;
	private JTextArea GraduatedSymbolProperties_JTextArea = null;
	
	private SimpleJButton cancel_JButton = null;
	private SimpleJButton ok_JButton = null;
	private SimpleJButton help_JButton = null;
	private GeoMap_Command command = null;
	private JFrame parent = null;
	private boolean ok = false;
	
	/**
	Command dialog constructor.
	@param parent JFrame class instantiating this class.
	@param command Command to edit.
	@param tableIDChoices list of table identifiers to provide as choices
	*/
	public GeoMap_JDialog ( JFrame parent, GeoMap_Command command, List<String> tableIDChoices ) {
		super(parent, true);
		initialize ( parent, command, tableIDChoices );
	}
	
	/**
	Responds to ActionEvents.
	@param event ActionEvent object
	*/
	public void actionPerformed(ActionEvent event) {
		Object o = event.getSource();
	
	    if ( o == this.MapCommand_JComboBox ) {
	    	setTabForMapCommand();
	    	refresh();
	    }
	    else if ( o == this.cancel_JButton ) {
			response ( false );
		}
	    else if ( event.getActionCommand().equalsIgnoreCase("EditCategorizedSymbolProperties") ) {
	        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
	        String Properties = this.CategorizedSymbolProperties_JTextArea.getText().trim();
	        String [] notes = {
	            "Categorized symbol properties provide configuration data for a layer's symbol."
	        };
	        String dict = (new DictionaryJDialog ( this.parent, true, Properties,
	            "Edit Properties Parameter", notes, "Property Name", "Property Value",10)).response();
	        if ( dict != null ) {
	            this.CategorizedSymbolProperties_JTextArea.setText ( dict );
	            refresh();
	        }
	    }
	    else if ( event.getActionCommand().equalsIgnoreCase("EditGeoLayerProperties") ) {
	        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
	        String Properties = this.GeoLayerProperties_JTextArea.getText().trim();
	        String [] notes = {
	            "GeoLayer properties provide configuration data for a layer."
	        };
	        String dict = (new DictionaryJDialog ( this.parent, true, Properties,
	            "Edit Properties Parameter", notes, "Property Name", "Property Value",10)).response();
	        if ( dict != null ) {
	            this.GeoLayerProperties_JTextArea.setText ( dict );
	            refresh();
	        }
	    }
	    else if ( event.getActionCommand().equalsIgnoreCase("EditGeoLayerViewProperties") ) {
	        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
	        String Properties = this.GeoLayerViewProperties_JTextArea.getText().trim();
	        String [] notes = {
	            "GeoLayerView properties provide configuration data for a layer view."
	        };
	        String dict = (new DictionaryJDialog ( this.parent, true, Properties,
	            "Edit Properties Parameter", notes, "Property Name", "Property Value",10)).response();
	        if ( dict != null ) {
	            this.GeoLayerViewProperties_JTextArea.setText ( dict );
	            refresh();
	        }
	    }
	    else if ( event.getActionCommand().equalsIgnoreCase("EditGeoLayerViewGroupProperties") ) {
	        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
	        String Properties = this.GeoLayerViewGroupProperties_JTextArea.getText().trim();
	        String [] notes = {
	            "GeoLayerViewGroup properties provide configuration data for a layer view group."
	        };
	        String dict = (new DictionaryJDialog ( this.parent, true, Properties,
	            "Edit Properties Parameter", notes, "Property Name", "Property Value",10)).response();
	        if ( dict != null ) {
	            this.GeoLayerViewGroupProperties_JTextArea.setText ( dict );
	            refresh();
	        }
	    }
	    else if ( event.getActionCommand().equalsIgnoreCase("EditGeoMapProperties") ) {
	        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
	        String Properties = this.GeoMapProperties_JTextArea.getText().trim();
	        String [] notes = {
	            "GeoMap properties provide map configuration data."
	        };
	        String dict = (new DictionaryJDialog ( this.parent, true, Properties,
	            "Edit Properties Parameter", notes, "Property Name", "Property Value",10)).response();
	        if ( dict != null ) {
	            this.GeoMapProperties_JTextArea.setText ( dict );
	            refresh();
	        }
	    }
	    else if ( event.getActionCommand().equalsIgnoreCase("EditGraduatedSymbolProperties") ) {
	        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
	        String Properties = this.GraduatedSymbolProperties_JTextArea.getText().trim();
	        String [] notes = {
	            "Graduated symbol properties provide configuration data for a layer's symbol."
	        };
	        String dict = (new DictionaryJDialog ( this.parent, true, Properties,
	            "Edit Properties Parameter", notes, "Property Name", "Property Value",10)).response();
	        if ( dict != null ) {
	            this.GraduatedSymbolProperties_JTextArea.setText ( dict );
	            refresh();
	        }
	    }
	    else if ( event.getActionCommand().equalsIgnoreCase("EditSingleSymbolProperties") ) {
	        // Edit the dictionary in the dialog.  It is OK for the string to be blank.
	        String Properties = this.SingleSymbolProperties_JTextArea.getText().trim();
	        String [] notes = {
	            "Single symbol properties provide configuration data for a layer's symbol."
	        };
	        String dict = (new DictionaryJDialog ( this.parent, true, Properties,
	            "Edit Properties Parameter", notes, "Property Name", "Property Value",10)).response();
	        if ( dict != null ) {
	            this.SingleSymbolProperties_JTextArea.setText ( dict );
	            refresh();
	        }
	    }
		else if ( o == this.help_JButton ) {
			HelpViewer.getInstance().showHelp("command", "GeoMap");
		}
		else if ( o == this.ok_JButton ) {
			refresh ();
			checkInput ();
			if ( !this.errorWait ) {
				// Command has been edited.
				response ( true );
			}
		}
	}
	
	/**
	Check the input.  If errors exist, warn the user and set the this.errorWait flag to true.
	This should be called before response() is allowed to complete.
	*/
	private void checkInput () {
		// Create a list of parameters to check.
		PropList props = new PropList ( "" );
		// General.
		String MapCommand = this.MapCommand_JComboBox.getSelected();
		String GeoMapProjectID = this.GeoMapProjectID_JTextField.getText().trim();
		String GeoMapID = this.GeoMapID_JTextField.getText().trim();
		// New.
		String NewGeoMapID = this.NewGeoMapID_JTextField.getText().trim();
		//String NewGeoMapID = this.NewGeoMapID_JTextField.getText().trim();
		String GeoMapName = this.GeoMapName_JTextField.getText().trim();
		String GeoMapDescription = this.GeoMapDescription_JTextField.getText().trim();
		String GeoMapProperties = this.GeoMapProperties_JTextArea.getText().trim().replace("\n"," ");
		// Layer.
		String GeoLayerID = this.GeoLayerID_JTextField.getText().trim();
		String GeoLayerName = this.GeoLayerName_JTextField.getText().trim();
		String GeoLayerDescription = this.GeoLayerDescription_JTextField.getText().trim();
		String GeoLayerCrs = this.GeoLayerCrs_JComboBox.getSelected();
		String GeoLayerGeometryType = this.GeoLayerGeometryType_JComboBox.getSelected();
		String GeoLayerLayerType = this.GeoLayerLayerType_JComboBox.getSelected();
		String GeoLayerProperties = this.GeoLayerProperties_JTextArea.getText().trim().replace("\n"," ");
		String GeoLayerSourceFormat = this.GeoLayerSourceFormat_JComboBox.getSelected();
		String GeoLayerSourcePath = this.GeoLayerSourcePath_JTextField.getText().trim();
	    // Layer View Group.
		String GeoLayerViewGroupID = this.GeoLayerViewGroupID_JTextField.getText().trim();
		String GeoLayerViewGroupName = this.GeoLayerViewGroupName_JTextField.getText().trim();
		String GeoLayerViewGroupDescription = this.GeoLayerViewGroupDescription_JTextField.getText().trim();
		String GeoLayerViewGroupProperties = this.GeoLayerViewGroupProperties_JTextArea.getText().trim().replace("\n"," ");
		String GeoLayerViewGroupInsertPosition = this.GeoLayerViewGroupInsertPosition_JTextField.getText().trim();
		String GeoLayerViewGroupInsertBefore = this.GeoLayerViewGroupInsertBefore_JTextField.getText().trim();
		String GeoLayerViewGroupInsertAfter = this.GeoLayerViewGroupInsertAfter_JTextField.getText().trim();
	    // Layer View.
		String GeoLayerViewID = this.GeoLayerViewID_JTextField.getText().trim();
		String GeoLayerViewName = this.GeoLayerViewName_JTextField.getText().trim();
		String GeoLayerViewDescription = this.GeoLayerViewDescription_JTextField.getText().trim();
		String GeoLayerViewProperties = this.GeoLayerViewProperties_JTextArea.getText().trim().replace("\n"," ");
		String GeoLayerViewInsertPosition = this.GeoLayerViewInsertPosition_JTextField.getText().trim();
		String GeoLayerViewInsertBefore = this.GeoLayerViewInsertBefore_JTextField.getText().trim();
		String GeoLayerViewInsertAfter = this.GeoLayerViewInsertAfter_JTextField.getText().trim();
		String GeoLayerViewLayerID = this.GeoLayerViewLayerID_JTextField.getText().trim();
		// Single symbol.
		String SingleSymbolID = this.SingleSymbolID_JTextField.getText().trim();
		String SingleSymbolName = this.SingleSymbolName_JTextField.getText().trim();
		String SingleSymbolDescription = this.SingleSymbolDescription_JTextField.getText().trim();
		String SingleSymbolProperties = this.SingleSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
		// Categorized symbol.
		String CategorizedSymbolID = this.CategorizedSymbolID_JTextField.getText().trim();
		String CategorizedSymbolName = this.CategorizedSymbolName_JTextField.getText().trim();
		String CategorizedSymbolDescription = this.CategorizedSymbolDescription_JTextField.getText().trim();
		String CategorizedSymbolProperties = this.CategorizedSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
		// Graduated symbol.
		String GraduatedSymbolID = this.GraduatedSymbolID_JTextField.getText().trim();
		String GraduatedSymbolName = this.GraduatedSymbolName_JTextField.getText().trim();
		String GraduatedSymbolDescription = this.GraduatedSymbolDescription_JTextField.getText().trim();
		String GraduatedSymbolProperties = this.GraduatedSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
	
		this.errorWait = false;
	
		// General.
	    if ( !MapCommand.isEmpty() ) {
	        props.set ( "MapCommand", MapCommand );
	    }
	    if ( !GeoMapProjectID.isEmpty() ) {
	        props.set ( "GeoMapProjectID", GeoMapProjectID );
	    }
	    if ( !GeoMapID.isEmpty() ) {
	        props.set ( "GeoMapID", GeoMapID );
	    }
	    // New.
	    if ( !NewGeoMapID.isEmpty() ) {
	        props.set ( "NewGeoMapID", NewGeoMapID );
	    }
	    //if ( !NewGeoMapID.isEmpty() ) {
	    //    props.set ( "NewGeoMapID", NewGeoMapID );
	    //}
	    if ( !GeoMapName.isEmpty() ) {
	        props.set ( "GeoMapName", GeoMapName );
	    }
	    if ( !GeoMapDescription.isEmpty() ) {
	        props.set ( "GeoMapDescription", GeoMapDescription );
	    }
	    if ( !GeoMapProperties.isEmpty() ) {
	        props.set ( "GeoMapProperties", GeoMapProperties );
	    }
	    // Layer 
	    if ( !GeoLayerID.isEmpty() ) {
	        props.set ( "GeoLayerID", GeoLayerID );
	    }
	    if ( !GeoLayerName.isEmpty() ) {
	        props.set ( "GeoLayerName", GeoLayerName );
	    }
	    if ( !GeoLayerDescription.isEmpty() ) {
	        props.set ( "GeoLayerDescription", GeoLayerDescription );
	    }
	    if ( !GeoLayerCrs.isEmpty() ) {
	        props.set ( "GeoLayerCrs", GeoLayerCrs );
	    }
	    if ( !GeoLayerGeometryType.isEmpty() ) {
	        props.set ( "GeoLayerGeometryType", GeoLayerGeometryType );
	    }
	    if ( !GeoLayerLayerType.isEmpty() ) {
	        props.set ( "GeoLayerLayerType", GeoLayerLayerType );
	    }
	    if ( !GeoLayerProperties.isEmpty() ) {
	        props.set ( "GeoLayerProperties", GeoLayerProperties );
	    }
	    if ( !GeoLayerSourceFormat.isEmpty() ) {
	        props.set ( "GeoLayerSourceFormat", GeoLayerSourceFormat );
	    }
	    if ( !GeoLayerSourcePath.isEmpty() ) {
	        props.set ( "GeoLayerSourcePath", GeoLayerSourcePath );
	    }
	    // Layer View Group.
	    if ( !GeoLayerViewGroupID.isEmpty() ) {
	        props.set ( "GeoLayerViewGroupID", GeoLayerViewGroupID );
	    }
	    if ( !GeoLayerViewGroupName.isEmpty() ) {
	        props.set ( "GeoLayerViewGroupName", GeoLayerViewGroupName );
	    }
	    if ( !GeoLayerViewGroupDescription.isEmpty() ) {
	        props.set ( "GeoLayerViewGroupDescription", GeoLayerViewGroupDescription );
	    }
	    if ( !GeoLayerViewGroupProperties.isEmpty() ) {
	        props.set ( "GeoLayerViewGroupProperties", GeoLayerViewGroupProperties );
	    }
	    if ( !GeoLayerViewGroupInsertPosition.isEmpty() ) {
	        props.set ( "GeoLayerViewGroupInsertPosition", GeoLayerViewGroupInsertPosition );
	    }
	    if ( !GeoLayerViewGroupInsertBefore.isEmpty() ) {
	        props.set ( "GeoLayerViewGroupInsertBefore", GeoLayerViewGroupInsertBefore );
	    }
	    if ( !GeoLayerViewGroupInsertAfter.isEmpty() ) {
	        props.set ( "GeoLayerViewGroupInsertAfter", GeoLayerViewGroupInsertAfter );
	    }
	    // Layer View.
	    if ( !GeoLayerViewID.isEmpty() ) {
	        props.set ( "GeoLayerViewID", GeoLayerViewID );
	    }
	    if ( !GeoLayerViewName.isEmpty() ) {
	        props.set ( "GeoLayerViewName", GeoLayerViewName );
	    }
	    if ( !GeoLayerViewDescription.isEmpty() ) {
	        props.set ( "GeoLayerViewDescription", GeoLayerViewDescription );
	    }
	    if ( !GeoLayerViewProperties.isEmpty() ) {
	        props.set ( "GeoLayerViewProperties", GeoLayerViewProperties );
	    }
	    if ( !GeoLayerViewInsertPosition.isEmpty() ) {
	        props.set ( "GeoLayerViewInsertPosition", GeoLayerViewInsertPosition );
	    }
	    if ( !GeoLayerViewInsertBefore.isEmpty() ) {
	        props.set ( "GeoLayerViewInsertBefore", GeoLayerViewInsertBefore );
	    }
	    if ( !GeoLayerViewInsertAfter.isEmpty() ) {
	        props.set ( "GeoLayerViewInsertAfter", GeoLayerViewInsertAfter );
	    }
	    if ( !GeoLayerViewLayerID.isEmpty() ) {
	        props.set ( "GeoLayerViewLayerID", GeoLayerViewLayerID );
	    }
	    // Single Symbol
	    if ( !SingleSymbolID.isEmpty() ) {
	        props.set ( "SingleSymbolID", SingleSymbolID );
	    }
	    if ( !SingleSymbolName.isEmpty() ) {
	        props.set ( "SingleSymbolName", SingleSymbolName );
	    }
	    if ( !SingleSymbolDescription.isEmpty() ) {
	        props.set ( "SingleSymbolDescription", SingleSymbolDescription );
	    }
	    if ( !SingleSymbolProperties.isEmpty() ) {
	        props.set ( "SingleSymbolProperties", SingleSymbolProperties );
	    }
	    // Categorized Symbol
	    if ( !CategorizedSymbolID.isEmpty() ) {
	        props.set ( "CategorizedSymbolID", CategorizedSymbolID );
	    }
	    if ( !CategorizedSymbolName.isEmpty() ) {
	        props.set ( "CategorizedSymbolName", CategorizedSymbolName );
	    }
	    if ( !CategorizedSymbolDescription.isEmpty() ) {
	        props.set ( "CategorizedSymbolDescription", CategorizedSymbolDescription );
	    }
	    if ( !CategorizedSymbolProperties.isEmpty() ) {
	        props.set ( "CategorizedSymbolProperties", CategorizedSymbolProperties );
	    }
	    // Graduated Symbol
	    if ( !GraduatedSymbolID.isEmpty() ) {
	        props.set ( "GraduatedSymbolID", GraduatedSymbolID );
	    }
	    if ( !GraduatedSymbolName.isEmpty() ) {
	        props.set ( "GraduatedSymbolName", GraduatedSymbolName );
	    }
	    if ( !GraduatedSymbolDescription.isEmpty() ) {
	        props.set ( "GraduatedSymbolDescription", GraduatedSymbolDescription );
	    }
	    if ( !GraduatedSymbolProperties.isEmpty() ) {
	        props.set ( "GraduatedSymbolProperties", GraduatedSymbolProperties );
	    }
		try {
		    // This will warn the user.
			this.command.checkCommandParameters ( props, null, 1 );
		}
		catch ( Exception e ) {
	        Message.printWarning(2,"", e);
			// The warning would have been printed in the check code.
			this.errorWait = true;
		}
	}
	
	/**
	Commit the edits to the command.
	In this case the command parameters have already been checked and no errors were detected.
	*/
	private void commitEdits () {
		// General.
		String MapCommand = this.MapCommand_JComboBox.getSelected();
		String GeoMapProjectID = this.GeoMapProjectID_JTextField.getText().trim();
		String GeoMapID = this.GeoMapID_JTextField.getText().trim();
		// New.
		String NewGeoMapID = this.NewGeoMapID_JTextField.getText().trim();
		//String NewGeoMapID = this.NewGeoMapID_JTextField.getText().trim();
		String GeoMapName = this.GeoMapName_JTextField.getText().trim();
		String GeoMapDescription = this.GeoMapDescription_JTextField.getText().trim();
		String GeoMapProperties = this.GeoMapProperties_JTextArea.getText().trim().replace("\n"," ");
		// Layer.
		String GeoLayerID = this.GeoLayerID_JTextField.getText().trim();
		String GeoLayerName = this.GeoLayerName_JTextField.getText().trim();
		String GeoLayerDescription = this.GeoLayerDescription_JTextField.getText().trim();
		String GeoLayerCrs = this.GeoLayerCrs_JComboBox.getSelected();
		String GeoLayerGeometryType = this.GeoLayerGeometryType_JComboBox.getSelected();
		String GeoLayerLayerType = this.GeoLayerLayerType_JComboBox.getSelected();
		String GeoLayerProperties = this.GeoLayerProperties_JTextArea.getText().trim().replace("\n"," ");
		String GeoLayerSourceFormat = this.GeoLayerSourceFormat_JComboBox.getSelected();
		String GeoLayerSourcePath = this.GeoLayerSourcePath_JTextField.getText().trim();
	    // Layer View Group.
		String GeoLayerViewGroupID = this.GeoLayerViewGroupID_JTextField.getText().trim();
		String GeoLayerViewGroupName = this.GeoLayerViewGroupName_JTextField.getText().trim();
		String GeoLayerViewGroupDescription = this.GeoLayerViewGroupDescription_JTextField.getText().trim();
		String GeoLayerViewGroupProperties = this.GeoLayerViewGroupProperties_JTextArea.getText().trim().replace("\n"," ");
		String GeoLayerViewGroupInsertPosition = this.GeoLayerViewGroupInsertPosition_JTextField.getText().trim();
		String GeoLayerViewGroupInsertBefore = this.GeoLayerViewGroupInsertBefore_JTextField.getText().trim();
		String GeoLayerViewGroupInsertAfter = this.GeoLayerViewGroupInsertAfter_JTextField.getText().trim();
	    // Layer View.
		String GeoLayerViewID = this.GeoLayerViewID_JTextField.getText().trim();
		String GeoLayerViewName = this.GeoLayerViewName_JTextField.getText().trim();
		String GeoLayerViewDescription = this.GeoLayerViewDescription_JTextField.getText().trim();
		String GeoLayerViewProperties = this.GeoLayerViewProperties_JTextArea.getText().trim().replace("\n"," ");
		String GeoLayerViewInsertPosition = this.GeoLayerViewInsertPosition_JTextField.getText().trim();
		String GeoLayerViewInsertBefore = this.GeoLayerViewInsertBefore_JTextField.getText().trim();
		String GeoLayerViewInsertAfter = this.GeoLayerViewInsertAfter_JTextField.getText().trim();
		String GeoLayerViewLayerID = this.GeoLayerViewLayerID_JTextField.getText().trim();
		// Single symbol.
		String SingleSymbolID = this.SingleSymbolID_JTextField.getText().trim();
		String SingleSymbolName = this.SingleSymbolName_JTextField.getText().trim();
		String SingleSymbolDescription = this.SingleSymbolDescription_JTextField.getText().trim();
		String SingleSymbolProperties = this.SingleSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
		// Categorized symbol.
		String CategorizedSymbolID = this.CategorizedSymbolID_JTextField.getText().trim();
		String CategorizedSymbolName = this.CategorizedSymbolName_JTextField.getText().trim();
		String CategorizedSymbolDescription = this.CategorizedSymbolDescription_JTextField.getText().trim();
		String CategorizedSymbolProperties = this.CategorizedSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
		// Graduated symbol.
		String GraduatedSymbolID = this.GraduatedSymbolID_JTextField.getText().trim();
		String GraduatedSymbolName = this.GraduatedSymbolName_JTextField.getText().trim();
		String GraduatedSymbolDescription = this.GraduatedSymbolDescription_JTextField.getText().trim();
		String GraduatedSymbolProperties = this.GraduatedSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
	
		// General.
	    this.command.setCommandParameter ( "MapCommand", MapCommand );
	    this.command.setCommandParameter ( "GeoMapProjectID", GeoMapProjectID );
	    this.command.setCommandParameter ( "GeoMapID", GeoMapID );
	    // New.
	    this.command.setCommandParameter ( "NewGeoMapID", NewGeoMapID );
	    //this.command.setCommandParameter ( "NewGeoMapID", NewGeoMapID );
	    this.command.setCommandParameter ( "GeoMapName", GeoMapName );
	    this.command.setCommandParameter ( "GeoMapDescription", GeoMapDescription );
	    this.command.setCommandParameter ( "GeoMapProperties", GeoMapProperties );
	    // Layer.
	    this.command.setCommandParameter ( "GeoLayerID", GeoLayerID );
	    this.command.setCommandParameter ( "GeoLayerName", GeoLayerName );
	    this.command.setCommandParameter ( "GeoLayerDescription", GeoLayerDescription );
	    this.command.setCommandParameter ( "GeoLayerCrs", GeoLayerCrs );
	    this.command.setCommandParameter ( "GeoLayerGeometryType", GeoLayerGeometryType );
	    this.command.setCommandParameter ( "GeoLayerLayerType", GeoLayerLayerType );
	    this.command.setCommandParameter ( "GeoLayerProperties", GeoLayerProperties );
	    this.command.setCommandParameter ( "GeoLayerSourceFormat", GeoLayerSourceFormat );
	    this.command.setCommandParameter ( "GeoLayerSourcePath", GeoLayerSourcePath );
	    // Layer View Group.
	    this.command.setCommandParameter ( "GeoLayerViewGroupID", GeoLayerViewGroupID );
	    this.command.setCommandParameter ( "GeoLayerViewGroupName", GeoLayerViewGroupName );
	    this.command.setCommandParameter ( "GeoLayerViewGroupDescription", GeoLayerViewGroupDescription );
	    this.command.setCommandParameter ( "GeoLayerViewGroupProperties", GeoLayerViewGroupProperties );
	    this.command.setCommandParameter ( "GeoLayerViewGroupInsertPosition", GeoLayerViewGroupInsertPosition );
	    this.command.setCommandParameter ( "GeoLayerViewGroupInsertBefore", GeoLayerViewGroupInsertBefore );
	    this.command.setCommandParameter ( "GeoLayerViewGroupInsertAfter", GeoLayerViewGroupInsertAfter );
	    // Layer View.
	    this.command.setCommandParameter ( "GeoLayerViewID", GeoLayerViewID );
	    this.command.setCommandParameter ( "GeoLayerViewName", GeoLayerViewName );
	    this.command.setCommandParameter ( "GeoLayerViewDescription", GeoLayerViewDescription );
	    this.command.setCommandParameter ( "GeoLayerViewProperties", GeoLayerViewProperties );
	    this.command.setCommandParameter ( "GeoLayerViewInsertPosition", GeoLayerViewInsertPosition );
	    this.command.setCommandParameter ( "GeoLayerViewInsertBefore", GeoLayerViewInsertBefore );
	    this.command.setCommandParameter ( "GeoLayerViewInsertAfter", GeoLayerViewInsertAfter );
	    this.command.setCommandParameter ( "GeoLayerViewLayerID", GeoLayerViewLayerID );
	    // Single Symbol.
	    this.command.setCommandParameter ( "SingleSymbolID", SingleSymbolID );
	    this.command.setCommandParameter ( "SingleSymbolName", SingleSymbolName );
	    this.command.setCommandParameter ( "SingleSymbolDescription", SingleSymbolDescription );
	    this.command.setCommandParameter ( "SingleSymbolProperties", SingleSymbolProperties );
	    // Categorized Symbol.
	    this.command.setCommandParameter ( "CategorizedSymbolID", CategorizedSymbolID );
	    this.command.setCommandParameter ( "CategorizedSymbolName", CategorizedSymbolName );
	    this.command.setCommandParameter ( "CategorizedSymbolDescription", CategorizedSymbolDescription );
	    this.command.setCommandParameter ( "CategorizedSymbolProperties", CategorizedSymbolProperties );
	    // Graduated Symbol.
	    this.command.setCommandParameter ( "GraduatedSymbolID", GraduatedSymbolID );
	    this.command.setCommandParameter ( "GraduatedSymbolName", GraduatedSymbolName );
	    this.command.setCommandParameter ( "GraduatedSymbolDescription", GraduatedSymbolDescription );
	    this.command.setCommandParameter ( "GraduatedSymbolProperties", GraduatedSymbolProperties );
	}
	
	/**
	Return the selected command type enumeration.
	@return the selected command type
	*/
	private GeoMapCommandType getSelectedMapCommandType () {
	    // The combo box is not null so can get the value.
	    String mapCommand = MapCommand_JComboBox.getSelected();
	   	int pos = mapCommand.indexOf(" -");
	   	if ( pos > 0 ) {
	   		// Have a description.
	   		mapCommand = mapCommand.substring(0,pos).trim();
	   	}
	   	else {
	   		// No description.
	       	mapCommand = mapCommand.trim();
	   	}
	   	return GeoMapCommandType.valueOfIgnoreCase(mapCommand);
	}
	
	/**
	Instantiates the GUI components.
	@param parent JFrame class instantiating this class.
	@param command Command to edit and possibly run.
	*/
	private void initialize ( JFrame parent, GeoMap_Command command, List<String> tableIDChoices ) {
		this.parent = parent;
		this.command = command;
	
		addWindowListener(this);
	
	    Insets insetsTLBR = new Insets(2,2,2,2);
	
		// Main panel.
	
		JPanel main_JPanel = new JPanel();
		main_JPanel.setLayout(new GridBagLayout());
		getContentPane().add ("North", main_JPanel);
		int y = -1;
	
		JPanel paragraph = new JPanel();
		paragraph.setLayout(new GridBagLayout());
		int yy = -1;
	
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "<html><b>This command is under development.</b></html>"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "This command creates and/or adds data to a GeoMap, which defines a map configuration, organized as shown below (* indicates data that this command processes)."),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "Currently, a GeoProject can only have a single GeoMap."),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (""),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("GeoMapProject"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("   * GeoMap[ ] - each project includes 1+ maps"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("       * GeoLayer[ ] - each map includes 1+ layers"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("       * GeoLayerViewGroup[ ] - each map includes 1+ layer view groups"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("           * GeoLayerView[ ] - each layer view group includes 1+ layer views"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel ("               * GeoLayer + GeoLayerSymbol (each layer view includes a layer and symbology)"),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (""),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "A new map can be created, or an existing map can be modified."),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "One command can define a combination of map, layer, layer view group, and layer view."),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	   	JGUIUtil.addComponent(paragraph, new JLabel (
	        "Use multiple commands to add multiple layers, layer view groups, and layer views to a map."),
	        0, ++yy, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	
		JGUIUtil.addComponent(main_JPanel, paragraph,
			0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.NONE, GridBagConstraints.WEST);
		JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Map command:"),
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.MapCommand_JComboBox = new SimpleJComboBox ( false );
		this.MapCommand_JComboBox.setToolTipText("Project command to execute.");
		boolean alphabetize = false;
		boolean addNotes = false;
		List<String> commandChoices = GeoMapCommandType.getChoicesAsStrings ( alphabetize, addNotes );
		this.MapCommand_JComboBox.setData(commandChoices);
		this.MapCommand_JComboBox.select ( 0 );
		this.MapCommand_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(main_JPanel, this.MapCommand_JComboBox,
			1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - map command to run (see tabs below)."),
			3, y, 2, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(main_JPanel, new JLabel ("GeoMap Project ID:"),
	        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoMapProjectID_JTextField = new JTextField (15);
	    this.GeoMapProjectID_JTextField.setToolTipText("GeoMap project ID associated with the map.");
	    this.GeoMapProjectID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(main_JPanel, this.GeoMapProjectID_JTextField,
	        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - map project identifier."),
	        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(main_JPanel, new JLabel ("GeoMap ID:"),
	        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoMapID_JTextField = new JTextField (15);
	    this.GeoMapID_JTextField.setToolTipText("GeoMap ID for an existing map.");
	    this.GeoMapID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(main_JPanel, this.GeoMapID_JTextField,
	        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required (except for new map) - existing map identifier."),
	        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    this.main_JTabbedPane = new JTabbedPane ();
	    //this.main_JTabbedPane.addChangeListener(this);
	    JGUIUtil.addComponent(main_JPanel, this.main_JTabbedPane,
	        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    // Tab for a new map.
	    int yNew = -1;
	    JPanel new_JPanel = new JPanel();
	    new_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "New", new_JPanel );
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel (
	        "Use these parameters to create a new map for the specified project."),
	        0, ++yNew, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel (
	        "A layer, layer view group, and/or layer view can optionally be added to the map in the same command."),
	        0, ++yNew, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(new_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yNew, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("New GeoMap ID:"),
	        0, ++yNew, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.NewGeoMapID_JTextField = new JTextField (15);
	    this.NewGeoMapID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(new_JPanel, this.NewGeoMapID_JTextField,
	        1, yNew, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Required - new map identifier."),
	        3, yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Map name:"),
	        0, ++yNew, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoMapName_JTextField = new JTextField (10);
	    this.GeoMapName_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(new_JPanel, this.GeoMapName_JTextField,
	        1, yNew, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Required (if new) - map name."),
	        3, yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Map description:"),
	        0, ++yNew, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoMapDescription_JTextField = new JTextField (10);
	    this.GeoMapDescription_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(new_JPanel, this.GeoMapDescription_JTextField,
	        1, yNew, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Optional - map description."),
	        3, yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Map properties:"),
	        0, ++yNew, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoMapProperties_JTextArea = new JTextArea (6,35);
	    this.GeoMapProperties_JTextArea.setToolTipText("Properties for the map.  See the command documentation.");
	    this.GeoMapProperties_JTextArea.setLineWrap ( true );
	    this.GeoMapProperties_JTextArea.setWrapStyleWord ( true );
	    this.GeoMapProperties_JTextArea.setToolTipText("OriginalColumn1:NewColumn1,OriginalColumn2:NewColumn2");
	    this.GeoMapProperties_JTextArea.addKeyListener (this);
	    JGUIUtil.addComponent(new_JPanel, new JScrollPane(this.GeoMapProperties_JTextArea),
	        1, yNew, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(new_JPanel, new JLabel ("Optional - map properties."),
	        3, yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    JGUIUtil.addComponent(new_JPanel, new SimpleJButton ("Edit","EditGeoMapProperties",this),
	        3, ++yNew, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	    // Tab for layer.
	    int yLayer = -1;
	    JPanel layer_JPanel = new JPanel();
	    layer_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "Layer", layer_JPanel );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel (
	        "Use these parameters add a layer for the map."),
	        0, ++yLayer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel (
	        "A layer is used in a layer view.  A layer can be used in multiple layer views."),
	        0, ++yLayer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel (
	        "TSTool does not keep layers in memory so specify information needed to find the layer when processing the map."),
	        0, ++yLayer, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(layer_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yLayer, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("GeoLayer ID:"),
	        0, ++yLayer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerID_JTextField = new JTextField (15);
	    this.GeoLayerID_JTextField.setToolTipText("Unique identifier for the GeoLayer.");
	    this.GeoLayerID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layer_JPanel, this.GeoLayerID_JTextField,
	        1, yLayer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Required - layer identifier."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Layer name:"),
	        0, ++yLayer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerName_JTextField = new JTextField (10);
	    this.GeoLayerName_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layer_JPanel, this.GeoLayerName_JTextField,
	        1, yLayer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Required - layer name."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Layer description:"),
	        0, ++yLayer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerDescription_JTextField = new JTextField (10);
	    this.GeoLayerDescription_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layer_JPanel, this.GeoLayerDescription_JTextField,
	        1, yLayer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Optional - layer description."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Layer CRS:"),
	        0, ++yLayer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.GeoLayerCrs_JComboBox = new SimpleJComboBox ( false ); // Editing is disabled.
		this.GeoLayerCrs_JComboBox.setToolTipText("Coordinate Reference System (CRS) (e.g., EPSG:4326).");
		List<String> crsChoices = new ArrayList<>();
		crsChoices.add ( "" );
		crsChoices.add ( "EPSG:4326" );
		this.GeoLayerCrs_JComboBox.setData(crsChoices);
		this.GeoLayerCrs_JComboBox.select ( 0 );
		this.GeoLayerCrs_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(layer_JPanel, this.GeoLayerCrs_JComboBox,
	        1, yLayer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Required - layer CRS."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Layer geometry type:"),
	        0, ++yLayer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.GeoLayerGeometryType_JComboBox = new SimpleJComboBox ( false ); // Editing is disabled.
		this.GeoLayerGeometryType_JComboBox.setToolTipText("Geometry type (e.g., WKT:Polygon, Raster)");
		boolean alphabetical = false;
		boolean includeNote = false;
		List<String> geometryTypeChoices = GeoLayerGeometryType.getChoicesAsStrings(alphabetical, includeNote);
		geometryTypeChoices.add ( 0, "" );
		this.GeoLayerGeometryType_JComboBox.setData(geometryTypeChoices);
		this.GeoLayerGeometryType_JComboBox.select ( 0 );
		this.GeoLayerGeometryType_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(layer_JPanel, this.GeoLayerGeometryType_JComboBox,
	        1, yLayer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Required - layer geometry type."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Layer type:"),
	        0, ++yLayer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.GeoLayerLayerType_JComboBox = new SimpleJComboBox ( false ); // Editing is disabled.
		this.GeoLayerLayerType_JComboBox.setToolTipText( "Layer type (e.g., Vector, Raster).");
		List<String> layerTypeChoices = new ArrayList<>();
		layerTypeChoices.add ( "" );
		layerTypeChoices.add ( "" + GeoLayerType.RASTER );
		layerTypeChoices.add ( "" + GeoLayerType.VECTOR );
		this.GeoLayerLayerType_JComboBox.setData(layerTypeChoices);
		this.GeoLayerLayerType_JComboBox.select ( 0 );
		this.GeoLayerLayerType_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(layer_JPanel, this.GeoLayerLayerType_JComboBox,
	        1, yLayer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Required - layer type."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Layer properties:"),
	        0, ++yLayer, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerProperties_JTextArea = new JTextArea (6,35);
	    this.GeoLayerProperties_JTextArea.setToolTipText("Properties for the layer .  See the command documentation.");
	    this.GeoLayerProperties_JTextArea.setLineWrap ( true );
	    this.GeoLayerProperties_JTextArea.setWrapStyleWord ( true );
	    this.GeoLayerProperties_JTextArea.setToolTipText("OriginalColumn1:NewColumn1,OriginalColumn2:NewColumn2");
	    this.GeoLayerProperties_JTextArea.addKeyListener (this);
	    JGUIUtil.addComponent(layer_JPanel, new JScrollPane(this.GeoLayerProperties_JTextArea),
	        1, yLayer, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Optional - layer  properties."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    JGUIUtil.addComponent(layer_JPanel, new SimpleJButton ("Edit","EditGeoLayerProperties",this),
	        3, ++yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Layer source format:"),
	        0, ++yLayer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.GeoLayerSourceFormat_JComboBox = new SimpleJComboBox ( false ); // Editing is disabled.
		this.GeoLayerSourceFormat_JComboBox.setToolTipText("Source format (e.g., GeoJSON, WMTS).");
		List<String> sourceFormatChoices = new ArrayList<>();
		sourceFormatChoices.add ( "" );
		sourceFormatChoices.add ( "" + GeoLayerFormatType.GEOJSON );
		sourceFormatChoices.add ( "" + GeoLayerFormatType.WMTS );
		this.GeoLayerSourceFormat_JComboBox.setData(sourceFormatChoices);
		this.GeoLayerSourceFormat_JComboBox.select ( 0 );
		this.GeoLayerSourceFormat_JComboBox.addActionListener ( this );
	    JGUIUtil.addComponent(layer_JPanel, this.GeoLayerSourceFormat_JComboBox,
	        1, yLayer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Required - source path."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Layer source path:"),
	        0, ++yLayer, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerSourcePath_JTextField = new JTextField (40);
	    this.GeoLayerSourcePath_JTextField.setToolTipText("Source path to the GeoLayer data, local path or a URL.");
	    this.GeoLayerSourcePath_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layer_JPanel, this.GeoLayerSourcePath_JTextField,
	        1, yLayer, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layer_JPanel, new JLabel ("Required - source path."),
	        3, yLayer, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    // Tab for layer view group.
	    int yLayerViewGroup = -1;
	    JPanel layerViewGroup_JPanel = new JPanel();
	    layerViewGroup_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "Layer View Group", layerViewGroup_JPanel );
	
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel (
	        "Use these parameters to match an existing or create a new layer view group, which is used to organize layer views."),
	        0, ++yLayerViewGroup, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel (
	        "A layer view can be added by using the 'Layer View' tab."),
	        0, ++yLayerViewGroup, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(layerViewGroup_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yLayerViewGroup, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("GeoLayerViewGroup ID:"),
	        0, ++yLayerViewGroup, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewGroupID_JTextField = new JTextField (30);
	    this.GeoLayerViewGroupID_JTextField.setToolTipText("Unique identifier for the GeoLayerViewGroup.");
	    this.GeoLayerViewGroupID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerViewGroup_JPanel, this.GeoLayerViewGroupID_JTextField,
	        1, yLayerViewGroup, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Required - layer view group identifier."),
	        3, yLayerViewGroup, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Layer view group name:"),
	        0, ++yLayerViewGroup, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewGroupName_JTextField = new JTextField (10);
	    this.GeoLayerViewGroupName_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerViewGroup_JPanel, this.GeoLayerViewGroupName_JTextField,
	        1, yLayerViewGroup, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Required (if new) - layer view group name."),
	        3, yLayerViewGroup, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Layer view group description:"),
	        0, ++yLayerViewGroup, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewGroupDescription_JTextField = new JTextField (10);
	    this.GeoLayerViewGroupDescription_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerViewGroup_JPanel, this.GeoLayerViewGroupDescription_JTextField,
	        1, yLayerViewGroup, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Optional - layer view group description."),
	        3, yLayerViewGroup, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Layer view group properties:"),
	        0, ++yLayerViewGroup, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewGroupProperties_JTextArea = new JTextArea (6,35);
	    this.GeoLayerViewGroupProperties_JTextArea.setToolTipText("Properties for the map.  See the command documentation.");
	    this.GeoLayerViewGroupProperties_JTextArea.setLineWrap ( true );
	    this.GeoLayerViewGroupProperties_JTextArea.setWrapStyleWord ( true );
	    this.GeoLayerViewGroupProperties_JTextArea.setToolTipText("OriginalColumn1:NewColumn1,OriginalColumn2:NewColumn2");
	    this.GeoLayerViewGroupProperties_JTextArea.addKeyListener (this);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JScrollPane(this.GeoLayerViewGroupProperties_JTextArea),
	        1, yLayerViewGroup, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Optional - layer view group properties."),
	        3, yLayerViewGroup, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new SimpleJButton ("Edit","EditGeoLayerViewGroupProperties",this),
	        3, ++yLayerViewGroup, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Layer view group insert position:"),
	        0, ++yLayerViewGroup, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewGroupInsertPosition_JTextField = new JTextField (10);
	    this.GeoLayerViewGroupInsertPosition_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerViewGroup_JPanel, this.GeoLayerViewGroupInsertPosition_JTextField,
	        1, yLayerViewGroup, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Optional - layer view group insert position (default=at end)."),
	        3, yLayerViewGroup, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Layer view group to insert before:"),
	        0, ++yLayerViewGroup, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewGroupInsertBefore_JTextField = new JTextField (10);
	    this.GeoLayerViewGroupInsertBefore_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerViewGroup_JPanel, this.GeoLayerViewGroupInsertBefore_JTextField,
	        1, yLayerViewGroup, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Optional - layer view group ID to insert before (default=at end)."),
	        3, yLayerViewGroup, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Layer view group to insert after:"),
	        0, ++yLayerViewGroup, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewGroupInsertAfter_JTextField = new JTextField (10);
	    this.GeoLayerViewGroupInsertAfter_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerViewGroup_JPanel, this.GeoLayerViewGroupInsertAfter_JTextField,
	        1, yLayerViewGroup, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerViewGroup_JPanel, new JLabel ("Optional - layer view group ID to insert after (default=at end)."),
	        3, yLayerViewGroup, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    // Tab for layer view.
	    int yLayerView = -1;
	    JPanel layerView_JPanel = new JPanel();
	    layerView_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "Layer View", layerView_JPanel );
	
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel (
	        "Use these parameters to match an existing or create a new layer view, which includes a layer"
	        + " and symbol configuration (see the 'Layer View (Symbol)' tab."),
	        0, ++yLayerView, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(layerView_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yLayerView, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("GeoLayerView ID:"),
	        0, ++yLayerView, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewID_JTextField = new JTextField (30);
	    this.GeoLayerViewID_JTextField.setToolTipText("Unique identifier for the GeoLayerView.");
	    this.GeoLayerViewID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerView_JPanel, this.GeoLayerViewID_JTextField,
	        1, yLayerView, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Required - layer view identifier."),
	        3, yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Layer view name:"),
	        0, ++yLayerView, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewName_JTextField = new JTextField (10);
	    this.GeoLayerViewName_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerView_JPanel, this.GeoLayerViewName_JTextField,
	        1, yLayerView, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Required (if new) - layer view name."),
	        3, yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Layer view description:"),
	        0, ++yLayerView, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewDescription_JTextField = new JTextField (10);
	    this.GeoLayerViewDescription_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerView_JPanel, this.GeoLayerViewDescription_JTextField,
	        1, yLayerView, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Optional - layer view description."),
	        3, yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Layer view properties:"),
	        0, ++yLayerView, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewProperties_JTextArea = new JTextArea (6,35);
	    this.GeoLayerViewProperties_JTextArea.setToolTipText("Properties for the layer view.  See the command documentation.");
	    this.GeoLayerViewProperties_JTextArea.setLineWrap ( true );
	    this.GeoLayerViewProperties_JTextArea.setWrapStyleWord ( true );
	    this.GeoLayerViewProperties_JTextArea.setToolTipText("OriginalColumn1:NewColumn1,OriginalColumn2:NewColumn2");
	    this.GeoLayerViewProperties_JTextArea.addKeyListener (this);
	    JGUIUtil.addComponent(layerView_JPanel, new JScrollPane(this.GeoLayerViewProperties_JTextArea),
	        1, yLayerView, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Optional - layer view properties."),
	        3, yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    JGUIUtil.addComponent(layerView_JPanel, new SimpleJButton ("Edit","EditGeoLayerViewProperties",this),
	        3, ++yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Layer view insert position:"),
	        0, ++yLayerView, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewInsertPosition_JTextField = new JTextField (10);
	    this.GeoLayerViewInsertPosition_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerView_JPanel, this.GeoLayerViewInsertPosition_JTextField,
	        1, yLayerView, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Optional - layer view insert position (default=at end)."),
	        3, yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Layer view to insert before:"),
	        0, ++yLayerView, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewInsertBefore_JTextField = new JTextField (10);
	    this.GeoLayerViewInsertBefore_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerView_JPanel, this.GeoLayerViewInsertBefore_JTextField,
	        1, yLayerView, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Optional - layer view ID to insert before (default=at end)."),
	        3, yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Layer view to insert after:"),
	        0, ++yLayerView, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewInsertAfter_JTextField = new JTextField (10);
	    this.GeoLayerViewInsertAfter_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerView_JPanel, this.GeoLayerViewInsertAfter_JTextField,
	        1, yLayerView, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Optional - layer view ID to insert after (default=at end)."),
	        3, yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("GeoLayer ID:"),
	        0, ++yLayerView, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GeoLayerViewLayerID_JTextField = new JTextField (45);
	    this.GeoLayerViewLayerID_JTextField.setToolTipText("Unique identifier for the layer to use in the view.");
	    this.GeoLayerViewLayerID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(layerView_JPanel, this.GeoLayerViewLayerID_JTextField,
	        1, yLayerView, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(layerView_JPanel, new JLabel ("Required - layer identifier."),
	        3, yLayerView, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

	    // Tab for layer view symbol.
	    int ySymbol = -1;
	    JPanel symbol_JPanel = new JPanel();
	    symbol_JPanel.setLayout( new GridBagLayout() );
	    this.main_JTabbedPane.addTab ( "Layer View (Symbol)", symbol_JPanel );
	
	    JGUIUtil.addComponent(symbol_JPanel, new JLabel (
	        "Use these parameters to define the symbol for a layer view (indicated by the 'Layer View' tab)."),
	        0, ++ySymbol, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(symbol_JPanel, new JLabel (
	        "The symbol for a layer can be either a single symbol, categorized (distinct values), or graduated (values)."),
	        0, ++ySymbol, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(symbol_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++ySymbol, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    // Tabbed pane for layer's symbol.
	    this.symbol_JTabbedPane = new JTabbedPane ();
	    JGUIUtil.addComponent(symbol_JPanel, this.symbol_JTabbedPane,
	        0, ++ySymbol, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    // Panel for layer's single symbol.
	    int ySingleSymbol = -1;
	    JPanel singleSymbol_JPanel = new JPanel();
	    singleSymbol_JPanel.setLayout( new GridBagLayout() );
	    this.symbol_JTabbedPane.addTab ( "Single Symbol", singleSymbol_JPanel );
	
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel (
	        "Use these parameters to define a single symbol for a layer view."),
	        0, ++ySingleSymbol, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel (
	        "The same symbol will be used for all layer features."),
	        0, ++ySingleSymbol, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(singleSymbol_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++ySingleSymbol, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel ("Single symbol ID:"),
	        0, ++ySingleSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.SingleSymbolID_JTextField = new JTextField (30);
	    this.SingleSymbolID_JTextField.setToolTipText("Unique identifier for the single symbol.");
	    this.SingleSymbolID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(singleSymbol_JPanel, this.SingleSymbolID_JTextField,
	        1, ySingleSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel ("Required - single symbol identifier."),
	        3, ySingleSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel ("Single symbol name:"),
	        0, ++ySingleSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.SingleSymbolName_JTextField = new JTextField (10);
	    this.SingleSymbolName_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(singleSymbol_JPanel, this.SingleSymbolName_JTextField,
	        1, ySingleSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel ("Required - single symbol name."),
	        3, ySingleSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel ("Single symbol description:"),
	        0, ++ySingleSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.SingleSymbolDescription_JTextField = new JTextField (10);
	    this.SingleSymbolDescription_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(singleSymbol_JPanel, this.SingleSymbolDescription_JTextField,
	        1, ySingleSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel ("Optional - single symbol description."),
	        3, ySingleSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel ("Single symbol properties:"),
	        0, ++ySingleSymbol, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.SingleSymbolProperties_JTextArea = new JTextArea (6,35);
	    this.SingleSymbolProperties_JTextArea.setToolTipText("Properties for the single symbol.  See the command documentation.");
	    this.SingleSymbolProperties_JTextArea.setLineWrap ( true );
	    this.SingleSymbolProperties_JTextArea.setWrapStyleWord ( true );
	    this.SingleSymbolProperties_JTextArea.setToolTipText("OriginalColumn1:NewColumn1,OriginalColumn2:NewColumn2");
	    this.SingleSymbolProperties_JTextArea.addKeyListener (this);
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JScrollPane(this.SingleSymbolProperties_JTextArea),
	        1, ySingleSymbol, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(singleSymbol_JPanel, new JLabel ("Optional - single symbol properties."),
	        3, ySingleSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    JGUIUtil.addComponent(singleSymbol_JPanel, new SimpleJButton ("Edit","EditSingleSymbolProperties",this),
	        3, ++ySingleSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    // Panel for layer's categorized symbol.
	    int yCategorizedSymbol = -1;
	    JPanel categorizedSymbol_JPanel = new JPanel();
	    categorizedSymbol_JPanel.setLayout( new GridBagLayout() );
	    this.symbol_JTabbedPane.addTab ( "Categorized Symbol", categorizedSymbol_JPanel );
	
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel (
	        "Use these parameters to define a categorized symbol for a layer view."),
	        0, ++yCategorizedSymbol, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel (
	        "The symbol will be determined using feature attribute values specified as categories."),
	        0, ++yCategorizedSymbol, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(categorizedSymbol_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yCategorizedSymbol, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel ("Categorized symbol ID:"),
	        0, ++yCategorizedSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.CategorizedSymbolID_JTextField = new JTextField (30);
	    this.CategorizedSymbolID_JTextField.setToolTipText("Unique identifier for the categorized symbol.");
	    this.CategorizedSymbolID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, this.CategorizedSymbolID_JTextField,
	        1, yCategorizedSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel ("Required - categorized symbol identifier."),
	        3, yCategorizedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel ("Categorized symbol name:"),
	        0, ++yCategorizedSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.CategorizedSymbolName_JTextField = new JTextField (10);
	    this.CategorizedSymbolName_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, this.CategorizedSymbolName_JTextField,
	        1, yCategorizedSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel ("Required - categorized symbol name."),
	        3, yCategorizedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel ("Categorized symbol description:"),
	        0, ++yCategorizedSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.CategorizedSymbolDescription_JTextField = new JTextField (10);
	    this.CategorizedSymbolDescription_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, this.CategorizedSymbolDescription_JTextField,
	        1, yCategorizedSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel ("Optional - categorized symbol description."),
	        3, yCategorizedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel ("Categornized symbol properties:"),
	        0, ++yCategorizedSymbol, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.CategorizedSymbolProperties_JTextArea = new JTextArea (6,35);
	    this.CategorizedSymbolProperties_JTextArea.setToolTipText("Properties for the categorized symbol.  See the command documentation.");
	    this.CategorizedSymbolProperties_JTextArea.setLineWrap ( true );
	    this.CategorizedSymbolProperties_JTextArea.setWrapStyleWord ( true );
	    this.CategorizedSymbolProperties_JTextArea.setToolTipText("OriginalColumn1:NewColumn1,OriginalColumn2:NewColumn2");
	    this.CategorizedSymbolProperties_JTextArea.addKeyListener (this);
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JScrollPane(this.CategorizedSymbolProperties_JTextArea),
	        1, yCategorizedSymbol, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new JLabel ("Optional - categorized symbol properties."),
	        3, yCategorizedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    JGUIUtil.addComponent(categorizedSymbol_JPanel, new SimpleJButton ("Edit","EditCategorizedSymbolProperties",this),
	        3, ++yCategorizedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    // Panel for layer's graduated symbol.
	    int yGraduatedSymbol = -1;
	    JPanel graduatedSymbol_JPanel = new JPanel();
	    graduatedSymbol_JPanel.setLayout( new GridBagLayout() );
	    this.symbol_JTabbedPane.addTab ( "Graduated Symbol", graduatedSymbol_JPanel );
	
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel (
	        "Use these parameters to define a graduated symbol for a layer view."),
	        0, ++yGraduatedSymbol, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel (
	        "The symbol will be determined using a graduated scale for feature attribute data."),
	        0, ++yGraduatedSymbol, 8, 1, 0, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);
		JGUIUtil.addComponent(graduatedSymbol_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
			0, ++yGraduatedSymbol, 8, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel ("Graduated symbol ID:"),
	        0, ++yGraduatedSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GraduatedSymbolID_JTextField = new JTextField (30);
	    this.GraduatedSymbolID_JTextField.setToolTipText("Unique identifier for the graduated symbol.");
	    this.GraduatedSymbolID_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, this.GraduatedSymbolID_JTextField,
	        1, yGraduatedSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel ("Required - graduated symbol identifier."),
	        3, yGraduatedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel ("Graduated symbol name:"),
	        0, ++yGraduatedSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GraduatedSymbolName_JTextField = new JTextField (10);
	    this.GraduatedSymbolName_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, this.GraduatedSymbolName_JTextField,
	        1, yGraduatedSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel ("Required - graduated symbol name."),
	        3, yGraduatedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel ("Graduated symbol description:"),
	        0, ++yGraduatedSymbol, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GraduatedSymbolDescription_JTextField = new JTextField (10);
	    this.GraduatedSymbolDescription_JTextField.addKeyListener ( this );
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, this.GraduatedSymbolDescription_JTextField,
	        1, yGraduatedSymbol, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel ("Optional - graduated symbol description."),
	        3, yGraduatedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel ("Graduated symbol properties:"),
	        0, ++yGraduatedSymbol, 1, 2, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	    this.GraduatedSymbolProperties_JTextArea = new JTextArea (6,35);
	    this.GraduatedSymbolProperties_JTextArea.setToolTipText("Properties for the graduated symbol.  See the command documentation.");
	    this.GraduatedSymbolProperties_JTextArea.setLineWrap ( true );
	    this.GraduatedSymbolProperties_JTextArea.setWrapStyleWord ( true );
	    this.GraduatedSymbolProperties_JTextArea.setToolTipText("OriginalColumn1:NewColumn1,OriginalColumn2:NewColumn2");
	    this.GraduatedSymbolProperties_JTextArea.addKeyListener (this);
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JScrollPane(this.GraduatedSymbolProperties_JTextArea),
	        1, yGraduatedSymbol, 2, 2, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new JLabel ("Optional - graduated symbol properties."),
	        3, yGraduatedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	    JGUIUtil.addComponent(graduatedSymbol_JPanel, new SimpleJButton ("Edit","EditGraduatedSymbolProperties",this),
	        3, ++yGraduatedSymbol, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
	
	    JGUIUtil.addComponent(main_JPanel, new JLabel ("Command:"),
			0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
		this.command_JTextArea = new JTextArea (7,60);
		this.command_JTextArea.setLineWrap ( true );
		this.command_JTextArea.setWrapStyleWord ( true );
		this.command_JTextArea.setEditable (false);
		JGUIUtil.addComponent(main_JPanel, new JScrollPane(this.command_JTextArea),
			1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
		// Refresh the contents.
		refresh ();
	
		// Panel for buttons.
		JPanel button_JPanel = new JPanel();
		button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	        JGUIUtil.addComponent(main_JPanel, button_JPanel,
			0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);
	
		this.ok_JButton = new SimpleJButton("OK", this);
		this.ok_JButton.setToolTipText("Save changes to command");
		button_JPanel.add (this.ok_JButton);
		this.cancel_JButton = new SimpleJButton("Cancel", this);
		button_JPanel.add (this.cancel_JButton);
		this.cancel_JButton.setToolTipText("Cancel without saving changes to command");
		button_JPanel.add ( this.help_JButton = new SimpleJButton("Help", this) );
		this.help_JButton.setToolTipText("Show command documentation in web browser");
	
		setTitle ( "Edit " + this.command.getCommandName() + " Command");
	    pack();
	    JGUIUtil.center(this);
		refresh();	// Sets the __path_JButton status.
		setResizable (false);
	    super.setVisible(true);
	}
	
	/**
	Handle ItemEvent events.
	@param event ItemEvent to handle.
	*/
	public void itemStateChanged ( ItemEvent event ) {
		Object o = event.getSource();
	    if ( o == this.MapCommand_JComboBox ) {
	    	setTabForMapCommand();
	    }
		refresh();
	}
	
	/**
	Respond to KeyEvents.
	*/
	public void keyPressed (KeyEvent event) {
		int code = event.getKeyCode();
	
		if (code == KeyEvent.VK_ENTER) {
			refresh ();
			checkInput();
			if (!this.errorWait) {
				response ( true );
			}
		}
	}
	
	public void keyReleased (KeyEvent event) {
		refresh();
	}
	
	public void keyTyped (KeyEvent event) {
	}
	
	/**
	Indicate if the user pressed OK (cancel otherwise).
	@return true if the edits were committed, false if the user canceled.
	*/
	public boolean ok () {
		return this.ok;
	}
	
	/**
	Refresh the command from the other text field contents.
	*/
	private void refresh () {
		String routine = getClass().getSimpleName() + ".refresh";
		// General.
		String MapCommand = "";
		String GeoMapProjectID = "";
		String GeoMapID = "";
		// New.
		String NewGeoMapID = "";
		String GeoMapName = "";
		String GeoMapDescription = "";
		String GeoMapProperties = "";
		// Layer.
		String GeoLayerID = "";
		String GeoLayerName = "";
		String GeoLayerDescription = "";
		String GeoLayerCrs = "";
		String GeoLayerGeometryType = "";
		String GeoLayerLayerType = "";
		String GeoLayerProperties = "";
		String GeoLayerSourceFormat = "";
		String GeoLayerSourcePath = "";
	    // Layer View Group.
		String GeoLayerViewGroupID = "";
		String GeoLayerViewGroupName = "";
		String GeoLayerViewGroupDescription = "";
		String GeoLayerViewGroupProperties = "";
		String GeoLayerViewGroupInsertPosition = "";
		String GeoLayerViewGroupInsertBefore = "";
		String GeoLayerViewGroupInsertAfter = "";
	    // Layer View.
		String GeoLayerViewID = "";
		String GeoLayerViewName = "";
		String GeoLayerViewDescription = "";
		String GeoLayerViewProperties = "";
		String GeoLayerViewInsertPosition = "";
		String GeoLayerViewInsertBefore = "";
		String GeoLayerViewInsertAfter = "";
		String GeoLayerViewLayerID = "";
		// Single symbol.
		String SingleSymbolID = "";
		String SingleSymbolName = "";
		String SingleSymbolDescription = "";
		String SingleSymbolProperties = "";
		// Categorized symbol.
		String CategorizedSymbolID = "";
		String CategorizedSymbolName = "";
		String CategorizedSymbolDescription = "";
		String CategorizedSymbolProperties = "";
		// Graduated symbol.
		String GraduatedSymbolID = "";
		String GraduatedSymbolName = "";
		String GraduatedSymbolDescription = "";
		String GraduatedSymbolProperties = "";
		PropList props = this.command.getCommandParameters();
		if ( this.firstTime ) {
			this.firstTime = false;
			// General.
			MapCommand = props.getValue ( "MapCommand" );
			GeoMapProjectID = props.getValue ( "GeoMapProjectID" );
			GeoMapID = props.getValue ( "GeoMapID" );
			// New.
			NewGeoMapID = props.getValue ( "NewGeoMapID" );
			GeoMapName = props.getValue ( "GeoMapName" );
			GeoMapDescription = props.getValue ( "GeoMapDescription" );
			GeoMapProperties = props.getValue ( "GeoMapProperties" );
			// Layer.
			GeoLayerID = props.getValue("GeoLayerID");
			GeoLayerName = props.getValue("GeoLayerName");
			GeoLayerDescription = props.getValue("GeoLayerDescription");
			GeoLayerCrs = props.getValue("GeoLayerCrs");
			GeoLayerGeometryType = props.getValue("GeoLayerGeometryType");
			GeoLayerLayerType = props.getValue("GeoLayerLayerType");
			GeoLayerProperties = props.getValue("GeoLayerProperties");
			GeoLayerSourceFormat = props.getValue("GeoLayerSourceFormat");
			GeoLayerSourcePath = props.getValue("GeoLayerSourcePath");
			// Layer View Group.
			GeoLayerViewGroupID = props.getValue("GeoLayerViewGroupID");
			GeoLayerViewGroupName = props.getValue("GeoLayerViewGroupName");
			GeoLayerViewGroupDescription = props.getValue("GeoLayerViewGroupDescription");
			GeoLayerViewGroupProperties = props.getValue("GeoLayerViewGroupProperties");
			GeoLayerViewGroupInsertPosition = props.getValue("GeoLayerViewGroupInsertPosition");
			GeoLayerViewGroupInsertBefore = props.getValue("GeoLayerViewGroupInsertBefore");
			GeoLayerViewGroupInsertAfter = props.getValue("GeoLayerViewGroupInsertAfter");
	    	// Layer View.
			GeoLayerViewID = props.getValue("GeoLayerViewID");
			GeoLayerViewName = props.getValue("GeoLayerViewName");
			GeoLayerViewDescription = props.getValue("GeoLayerViewDescription");
			GeoLayerViewProperties = props.getValue("GeoLayerViewProperties");
			GeoLayerViewInsertPosition = props.getValue("GeoLayerViewInsertPosition");
			GeoLayerViewInsertBefore = props.getValue("GeoLayerViewInsertBefore");
			GeoLayerViewInsertAfter = props.getValue("GeoLayerViewInsertAfter");
			GeoLayerViewLayerID = props.getValue("GeoLayerViewLayerID");
	    	// Single Symbol.
			SingleSymbolID = props.getValue("SingleSymbolID");
			SingleSymbolName = props.getValue("SingleSymbolName");
			SingleSymbolDescription = props.getValue("SingleSymbolDescription");
			SingleSymbolProperties = props.getValue("SingleSymbolProperties");
	    	// Categorized Symbol.
			CategorizedSymbolID = props.getValue("CategorizedSymbolID");
			CategorizedSymbolName = props.getValue("CategorizedSymbolName");
			CategorizedSymbolDescription = props.getValue("CategorizedSymbolDescription");
			CategorizedSymbolProperties = props.getValue("CategorizedSymbolProperties");
	    	// Graduated Symbol.
			GraduatedSymbolID = props.getValue("GraduatedSymbolID");
			GraduatedSymbolName = props.getValue("GraduatedSymbolName");
			GraduatedSymbolDescription = props.getValue("GraduatedSymbolDescription");
			GraduatedSymbolProperties = props.getValue("GraduatedSymbolProperties");
			// General.
			if ( JGUIUtil.isSimpleJComboBoxItem(this.MapCommand_JComboBox, MapCommand,JGUIUtil.NONE, null, null ) ) {
				this.MapCommand_JComboBox.select ( MapCommand );
			}
			else {
	            if ( (MapCommand == null) ||	MapCommand.equals("") ) {
					// New command...select the default.
					this.MapCommand_JComboBox.select ( 0 );
				}
				else {
					// Bad user command.
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"MapCommand parameter \"" + MapCommand + "\".  Select a value or Cancel." );
				}
			}
	        if ( GeoMapProjectID != null ) {
	            this.GeoMapProjectID_JTextField.setText ( GeoMapProjectID );
	        }
	        if ( GeoMapID != null ) {
	            this.GeoMapID_JTextField.setText ( GeoMapID );
	        }
	        // New.
	        if ( NewGeoMapID != null ) {
	            this.NewGeoMapID_JTextField.setText ( NewGeoMapID );
	        }
	        if ( GeoMapName != null ) {
	            this.GeoMapName_JTextField.setText ( GeoMapName );
	        }
	        if ( GeoMapDescription != null ) {
	            this.GeoMapDescription_JTextField.setText ( GeoMapDescription );
	        }
	        if ( GeoMapProperties != null ) {
	            this.GeoMapProperties_JTextArea.setText ( GeoMapProperties );
	        }
	        // Layer.
	        if ( GeoLayerID != null ) {
	            this.GeoLayerID_JTextField.setText ( GeoLayerID );
	        }
	        if ( GeoLayerName != null ) {
	            this.GeoLayerName_JTextField.setText ( GeoLayerName );
	        }
	        if ( GeoLayerDescription != null ) {
	            this.GeoLayerDescription_JTextField.setText ( GeoLayerDescription );
	        }
			if ( JGUIUtil.isSimpleJComboBoxItem(this.GeoLayerCrs_JComboBox, GeoLayerCrs,JGUIUtil.NONE, null, null ) ) {
				this.GeoLayerCrs_JComboBox.select ( GeoLayerCrs );
			}
			else {
	            if ( (GeoLayerCrs == null) ||	GeoLayerCrs.equals("") ) {
					// New command...select the default.
					this.GeoLayerCrs_JComboBox.select ( 0 );
				}
				else {
					// Bad user command.
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"GeoLayerCrs parameter \"" + GeoLayerCrs + "\".  Select a value or Cancel." );
				}
			}
			if ( JGUIUtil.isSimpleJComboBoxItem(this.GeoLayerGeometryType_JComboBox, GeoLayerGeometryType,JGUIUtil.NONE, null, null ) ) {
				this.GeoLayerGeometryType_JComboBox.select ( GeoLayerGeometryType );
			}
			else {
	            if ( (GeoLayerGeometryType == null) ||	GeoLayerGeometryType.equals("") ) {
					// New command...select the default.
					this.GeoLayerGeometryType_JComboBox.select ( 0 );
				}
				else {
					// Bad user command.
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"GeoLayerGeometryType parameter \"" + GeoLayerGeometryType + "\".  Select a value or Cancel." );
				}
			}
			if ( JGUIUtil.isSimpleJComboBoxItem(this.GeoLayerLayerType_JComboBox, GeoLayerLayerType,JGUIUtil.NONE, null, null ) ) {
				this.GeoLayerLayerType_JComboBox.select ( GeoLayerLayerType );
			}
			else {
	            if ( (GeoLayerLayerType == null) ||	GeoLayerLayerType.equals("") ) {
					// New command...select the default.
					this.GeoLayerLayerType_JComboBox.select ( 0 );
				}
				else {
					// Bad user command.
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"GeoLayerLayerType parameter \"" + GeoLayerLayerType + "\".  Select a value or Cancel." );
				}
			}
	        if ( GeoLayerProperties != null ) {
	            this.GeoLayerProperties_JTextArea.setText ( GeoLayerProperties );
	        }
			if ( JGUIUtil.isSimpleJComboBoxItem(this.GeoLayerSourceFormat_JComboBox, GeoLayerSourceFormat,JGUIUtil.NONE, null, null ) ) {
				this.GeoLayerSourceFormat_JComboBox.select ( GeoLayerSourceFormat );
			}
			else {
	            if ( (GeoLayerSourceFormat == null) ||	GeoLayerSourceFormat.equals("") ) {
					// New command...select the default.
					this.GeoLayerSourceFormat_JComboBox.select ( 0 );
				}
				else {
					// Bad user command.
					Message.printWarning ( 1, routine,
					"Existing command references an invalid\n"+
					"GeoLayerSourceFormat parameter \"" + GeoLayerSourceFormat + "\".  Select a value or Cancel." );
				}
			}
	        if ( GeoLayerSourcePath != null ) {
	            this.GeoLayerSourcePath_JTextField.setText ( GeoLayerSourcePath );
	        }
	        // Layer View Group.
	        if ( GeoLayerViewGroupID != null ) {
	            this.GeoLayerViewGroupID_JTextField.setText ( GeoLayerViewGroupID );
	        }
	        if ( GeoLayerViewGroupName != null ) {
	            this.GeoLayerViewGroupName_JTextField.setText ( GeoLayerViewGroupName );
	        }
	        if ( GeoLayerViewGroupDescription != null ) {
	            this.GeoLayerViewGroupDescription_JTextField.setText ( GeoLayerViewGroupDescription );
	        }
	        if ( GeoLayerViewGroupProperties != null ) {
	            this.GeoLayerViewGroupProperties_JTextArea.setText ( GeoLayerViewGroupProperties );
	        }
	        if ( GeoLayerViewGroupInsertPosition != null ) {
	            this.GeoLayerViewGroupInsertPosition_JTextField.setText ( GeoLayerViewGroupInsertPosition );
	        }
	        if ( GeoLayerViewGroupInsertBefore != null ) {
	            this.GeoLayerViewGroupInsertBefore_JTextField.setText ( GeoLayerViewGroupInsertBefore );
	        }
	        if ( GeoLayerViewGroupInsertAfter != null ) {
	            this.GeoLayerViewGroupInsertAfter_JTextField.setText ( GeoLayerViewGroupInsertAfter );
	        }
	        // Layer View.
	        if ( GeoLayerViewID != null ) {
	            this.GeoLayerViewID_JTextField.setText ( GeoLayerViewID );
	        }
	        if ( GeoLayerViewName != null ) {
	            this.GeoLayerViewName_JTextField.setText ( GeoLayerViewName );
	        }
	        if ( GeoLayerViewDescription != null ) {
	            this.GeoLayerViewDescription_JTextField.setText ( GeoLayerViewDescription );
	        }
	        if ( GeoLayerViewProperties != null ) {
	            this.GeoLayerViewProperties_JTextArea.setText ( GeoLayerViewProperties );
	        }
	        if ( GeoLayerViewInsertPosition != null ) {
	            this.GeoLayerViewInsertPosition_JTextField.setText ( GeoLayerViewInsertPosition );
	        }
	        if ( GeoLayerViewInsertBefore != null ) {
	            this.GeoLayerViewInsertBefore_JTextField.setText ( GeoLayerViewInsertBefore );
	        }
	        if ( GeoLayerViewInsertAfter != null ) {
	            this.GeoLayerViewInsertAfter_JTextField.setText ( GeoLayerViewInsertAfter );
	        }
	        if ( GeoLayerViewLayerID != null ) {
	            this.GeoLayerViewLayerID_JTextField.setText ( GeoLayerViewLayerID );
	        }
	        // Single Symbol.
	        if ( SingleSymbolID != null ) {
	            this.SingleSymbolID_JTextField.setText ( SingleSymbolID );
	        }
	        if ( SingleSymbolName != null ) {
	            this.SingleSymbolName_JTextField.setText ( SingleSymbolName );
	        }
	        if ( SingleSymbolDescription != null ) {
	            this.SingleSymbolDescription_JTextField.setText ( SingleSymbolDescription );
	        }
	        if ( SingleSymbolProperties != null ) {
	            this.SingleSymbolProperties_JTextArea.setText ( SingleSymbolProperties );
	        }
	        // Categorized Symbol.
	        if ( CategorizedSymbolID != null ) {
	            this.CategorizedSymbolID_JTextField.setText ( CategorizedSymbolID );
	        }
	        if ( CategorizedSymbolName != null ) {
	            this.CategorizedSymbolName_JTextField.setText ( CategorizedSymbolName );
	        }
	        if ( CategorizedSymbolDescription != null ) {
	            this.CategorizedSymbolDescription_JTextField.setText ( CategorizedSymbolDescription );
	        }
	        if ( CategorizedSymbolProperties != null ) {
	            this.CategorizedSymbolProperties_JTextArea.setText ( CategorizedSymbolProperties );
	        }
	        // Graduated Symbol.
	        if ( GraduatedSymbolID != null ) {
	            this.GraduatedSymbolID_JTextField.setText ( GraduatedSymbolID );
	        }
	        if ( GraduatedSymbolName != null ) {
	            this.GraduatedSymbolName_JTextField.setText ( GraduatedSymbolName );
	        }
	        if ( GraduatedSymbolDescription != null ) {
	            this.GraduatedSymbolDescription_JTextField.setText ( GraduatedSymbolDescription );
	        }
	        if ( GraduatedSymbolProperties != null ) {
	            this.GraduatedSymbolProperties_JTextArea.setText ( GraduatedSymbolProperties );
	        }
			// Set the tab for selected project command.
			setTabForMapCommand();
		}
		// Regardless, reset the command from the fields.
		// General.
		MapCommand = MapCommand_JComboBox.getSelected();
		GeoMapProjectID = this.GeoMapProjectID_JTextField.getText().trim();
		GeoMapID = this.GeoMapID_JTextField.getText().trim();
		// New.
		NewGeoMapID = this.NewGeoMapID_JTextField.getText().trim();
		GeoMapName = this.GeoMapName_JTextField.getText().trim();
		GeoMapDescription = this.GeoMapDescription_JTextField.getText().trim();
		GeoMapProperties = this.GeoMapProperties_JTextArea.getText().trim();
		// Layer.
		GeoLayerID = this.GeoLayerID_JTextField.getText().trim();
		GeoLayerName = this.GeoLayerName_JTextField.getText().trim();
		GeoLayerDescription = this.GeoLayerDescription_JTextField.getText().trim();
		GeoLayerCrs = this.GeoLayerCrs_JComboBox.getSelected();
		GeoLayerGeometryType = this.GeoLayerGeometryType_JComboBox.getSelected();
		GeoLayerLayerType = this.GeoLayerLayerType_JComboBox.getSelected();
		GeoLayerProperties = this.GeoLayerProperties_JTextArea.getText().trim().replace("\n"," ");
		GeoLayerSourceFormat = this.GeoLayerSourceFormat_JComboBox.getSelected();
		GeoLayerSourcePath = this.GeoLayerSourcePath_JTextField.getText().trim();
	    // Layer View Group.
		GeoLayerViewGroupID = this.GeoLayerViewGroupID_JTextField.getText().trim();
		GeoLayerViewGroupName = this.GeoLayerViewGroupName_JTextField.getText().trim();
		GeoLayerViewGroupDescription = this.GeoLayerViewGroupDescription_JTextField.getText().trim();
		GeoLayerViewGroupProperties = this.GeoLayerViewGroupProperties_JTextArea.getText().trim().replace("\n"," ");
		GeoLayerViewGroupInsertPosition = this.GeoLayerViewGroupInsertPosition_JTextField.getText().trim();
		GeoLayerViewGroupInsertBefore = this.GeoLayerViewGroupInsertBefore_JTextField.getText().trim();
		GeoLayerViewGroupInsertAfter = this.GeoLayerViewGroupInsertAfter_JTextField.getText().trim();
	    // Layer View.
		GeoLayerViewID = this.GeoLayerViewID_JTextField.getText().trim();
		GeoLayerViewName = this.GeoLayerViewName_JTextField.getText().trim();
		GeoLayerViewDescription = this.GeoLayerViewDescription_JTextField.getText().trim();
		GeoLayerViewProperties = this.GeoLayerViewProperties_JTextArea.getText().trim().replace("\n"," ");
		GeoLayerViewInsertPosition = this.GeoLayerViewInsertPosition_JTextField.getText().trim();
		GeoLayerViewInsertBefore = this.GeoLayerViewInsertBefore_JTextField.getText().trim();
		GeoLayerViewInsertAfter = this.GeoLayerViewInsertAfter_JTextField.getText().trim();
		GeoLayerViewLayerID = this.GeoLayerViewLayerID_JTextField.getText().trim();
	    // Single Symbol.
		SingleSymbolID = this.SingleSymbolID_JTextField.getText().trim();
		SingleSymbolName = this.SingleSymbolName_JTextField.getText().trim();
		SingleSymbolDescription = this.SingleSymbolDescription_JTextField.getText().trim();
		SingleSymbolProperties = this.SingleSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
	    // Categorized Symbol.
		CategorizedSymbolID = this.CategorizedSymbolID_JTextField.getText().trim();
		CategorizedSymbolName = this.CategorizedSymbolName_JTextField.getText().trim();
		CategorizedSymbolDescription = this.CategorizedSymbolDescription_JTextField.getText().trim();
		CategorizedSymbolProperties = this.CategorizedSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
	    // Graduated Symbol.
		GraduatedSymbolID = this.GraduatedSymbolID_JTextField.getText().trim();
		GraduatedSymbolName = this.GraduatedSymbolName_JTextField.getText().trim();
		GraduatedSymbolDescription = this.GraduatedSymbolDescription_JTextField.getText().trim();
		GraduatedSymbolProperties = this.GraduatedSymbolProperties_JTextArea.getText().trim().replace("\n"," ");
	
		props = new PropList ( this.command.getCommandName() );
		// General.
		props.add ( "MapCommand=" + MapCommand );
		props.add ( "GeoMapProjectID=" + GeoMapProjectID );
		props.add ( "GeoMapID=" + GeoMapID );
		// New.
		props.add ( "NewGeoMapID=" + NewGeoMapID );
		props.add ( "GeoMapName=" + GeoMapName );
		props.add ( "GeoMapDescription=" + GeoMapDescription );
		props.add ( "GeoMapProperties=" + GeoMapProperties );
		// Layer.
		props.add("GeoLayerID=" + GeoLayerID);
		props.add("GeoLayerName=" + GeoLayerName);
		props.add("GeoLayerDescription=" + GeoLayerDescription);
		props.add("GeoLayerCrs=" + GeoLayerCrs);
		props.add("GeoLayerGeometryType=" + GeoLayerGeometryType);
		props.add("GeoLayerLayerType=" + GeoLayerLayerType);
		props.add("GeoLayerProperties=" + GeoLayerProperties);
		props.add("GeoLayerSourceFormat=" + GeoLayerSourceFormat);
		props.add("GeoLayerSourcePath=" + GeoLayerSourcePath);
	    // Layer View Group.
		props.add("GeoLayerViewGroupID=" + GeoLayerViewGroupID);
		props.add("GeoLayerViewGroupName=" + GeoLayerViewGroupName);
		props.add("GeoLayerViewGroupDescription=" + GeoLayerViewGroupDescription);
		props.add("GeoLayerViewGroupProperties=" + GeoLayerViewGroupProperties);
		props.add("GeoLayerViewGroupInsertPosition=" + GeoLayerViewGroupInsertPosition);
		props.add("GeoLayerViewGroupInsertBefore=" + GeoLayerViewGroupInsertBefore);
		props.add("GeoLayerViewGroupInsertAfter=" + GeoLayerViewGroupInsertAfter);
	    // Layer View.
		props.add("GeoLayerViewID=" + GeoLayerViewID);
		props.add("GeoLayerViewName=" + GeoLayerViewName);
		props.add("GeoLayerViewDescription=" + GeoLayerViewDescription );
		props.add("GeoLayerViewProperties=" + GeoLayerViewProperties);
		props.add("GeoLayerViewInsertInsertPosition=" + GeoLayerViewInsertPosition);
		props.add("GeoLayerViewInsertInsertBefore=" + GeoLayerViewInsertBefore);
		props.add("GeoLayerViewInsertInsertAfter=" + GeoLayerViewInsertAfter);
	    // Single Symbol.
		props.add("SingleSymbolID=" + SingleSymbolID);
		props.add("SingleSymbolName=" + SingleSymbolName);
		props.add("SingleSymbolDescription=" + SingleSymbolDescription );
		props.add("SingleSymbolProperties=" + SingleSymbolProperties);
	    // Categorized Symbol.
		props.add("CategorizedSymbolID=" + CategorizedSymbolID);
		props.add("CategorizedSymbolName=" + CategorizedSymbolName);
		props.add("CategorizedSymbolDescription=" + CategorizedSymbolDescription );
		props.add("CategorizedSymbolProperties=" + CategorizedSymbolProperties);
	    // Graduated Symbol.
		props.add("GraduatedSymbolID=" + GraduatedSymbolID);
		props.add("GraduatedSymbolName=" + GraduatedSymbolName);
		props.add("GraduatedSymbolDescription=" + GraduatedSymbolDescription );
		props.add("GraduatedSymbolProperties=" + GraduatedSymbolProperties);
		this.command_JTextArea.setText( this.command.toString ( props ).trim() );
	}
	
	/**
	React to the user response.
	@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
	*/
	private void response ( boolean ok ) {
		this.ok = ok;	// Save to be returned by ok().
		if ( ok ) {
			// Commit the changes.
			commitEdits ();
			if ( this.errorWait ) {
				// Not ready to close out.
				return;
			}
		}
		// Now close out.
		setVisible( false );
		dispose();
	}
	
	/**
	 * Set the parameter tab based on the selected command.
	 */
	private void setTabForMapCommand() {
		GeoMapCommandType commandType = getSelectedMapCommandType();
		if ( commandType == GeoMapCommandType.NEW_MAP ) {
			this.main_JTabbedPane.setSelectedIndex(this.newTabIndex);
		}
		else if ( commandType == GeoMapCommandType.ADD_LAYER ) {
			this.main_JTabbedPane.setSelectedIndex(this.addLayerTabIndex);
		}
		else if ( commandType == GeoMapCommandType.ADD_LAYER_VIEW_GROUP ) {
			this.main_JTabbedPane.setSelectedIndex(this.addLayerViewGroupTabIndex);
		}
		else if ( commandType == GeoMapCommandType.ADD_LAYER_VIEW ) {
			this.main_JTabbedPane.setSelectedIndex(this.addLayerViewTabIndex);
		}
	}
	
	/**
	Responds to WindowEvents.
	@param event WindowEvent object
	*/
	public void windowClosing(WindowEvent event) {
		response ( false );
	}
	
	// The following methods are all necessary because this class implements WindowListener.
	
	public void windowActivated(WindowEvent evt) {
	}
	
	public void windowClosed(WindowEvent evt) {
	}
	
	public void windowDeactivated(WindowEvent evt) {
	}
	
	public void windowDeiconified(WindowEvent evt) {
	}
	
	public void windowIconified(WindowEvent evt) {
	}
	
	public void windowOpened(WindowEvent evt) {
	}

}