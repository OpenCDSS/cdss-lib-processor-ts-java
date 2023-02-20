// WeightTraces_JDialog - Editor for WeightTraces command.

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

package rti.tscommandprocessor.commands.ts;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.ui.CommandEditorUtil;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.TS.TSIdent;
import RTi.TS.TSIdent_JDialog;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;

/**
Editor for WeightTraces command.
*/
@SuppressWarnings("serial")
public class WeightTraces_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{

private JFrame __parent_JFrame = null;
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __edit_JButton = null;
private SimpleJButton __clear_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private WeightTraces_Command __command = null;
private JTextArea __command_JTextArea=null;
private TSFormatSpecifiersJPanel __Alias_JTextField = null;// Field for time series alias
private SimpleJComboBox	__EnsembleID_JComboBox = null; // Time series available to operate on.
private SimpleJComboBox	__SpecifyWeightsHow_JComboBox = null;// Indicates how weights are specified
private JTextArea __Year_JTextArea = null; // Field for traces
private JTextArea __Weight_JTextArea = null;// Weights for traces.
private JTextArea __NewTSID_JTextArea = null;
private boolean __error_wait = false;	// Is there an error to be cleared up?
private boolean __first_time = true;
private boolean __ok = false;       // Whether OK has been pressed.

/**
Editor dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to parse.
*/
public WeightTraces_JDialog ( JFrame parent, WeightTraces_Command command )
{	super(parent, true);
    __parent_JFrame = parent;
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	String routine = "WeightTraces_JDialog.actionPerformed";
    Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
	}
    else if ( o == __clear_JButton ) {
        __NewTSID_JTextArea.setText ( "" );
        refresh();
    }
    else if ( o == __edit_JButton ) {
        // Edit the NewTSID in the dialog.  It is OK for the string to be blank.
        String NewTSID = __NewTSID_JTextArea.getText().trim();
        TSIdent tsident;
        try {
            if ( NewTSID.length() == 0 ) {
                tsident = new TSIdent();
            }
            else {
                tsident = new TSIdent ( NewTSID );
            }
            PropList idprops = new PropList("NewTSIDProps" );
            TSIdent tsident2=(new TSIdent_JDialog ( __parent_JFrame, true, tsident, idprops )).response();
            if ( tsident2 != null ) {
                __NewTSID_JTextArea.setText ( tsident2.toString(true) );
                refresh();
            }
        }
        catch ( Exception e ) {
            Message.printWarning ( 1, routine, "Error creating time series identifier from \"" + NewTSID + "\"." );
            Message.printWarning ( 3, routine, e );
        }
    }
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "WeightTraces");
	}
	else if ( o == __ok_JButton ) {
		refresh ();
		checkInput();
		if ( !__error_wait ) {
			response ( true );
		}
	}
}

// Start event handlers for DocumentListener...

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void changedUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void insertUpdate ( DocumentEvent e )
{
    refresh();
}

/**
Handle DocumentEvent events.
@param e DocumentEvent to handle.
*/
public void removeUpdate ( DocumentEvent e )
{
    refresh();
}

// ...End event handlers for DocumentListener

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{   // Put together a list of parameters to check...
    PropList props = new PropList ( "" );
	String Alias = __Alias_JTextField.getText().trim();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String SpecifyWeightsHow = __SpecifyWeightsHow_JComboBox.getSelected();
	String Weights = getWeights();
	String NewTSID = __NewTSID_JTextArea.getText().trim();
    __error_wait = false;

    if ( Alias.length() > 0 ) {
        props.set ( "Alias", Alias );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        props.set ( "EnsembleID", EnsembleID );
    }
    if ( (SpecifyWeightsHow != null) && (SpecifyWeightsHow.length() > 0) ) {
        props.set ( "SpecifyWeightsHow", SpecifyWeightsHow );
    }
    if ( (Weights != null) && (Weights.length() > 0) ) {
        props.set ( "Weights", Weights );
    }
    if ( (NewTSID != null) && (NewTSID.length() > 0) ) {
        props.set ( "NewTSID", NewTSID );
    }
    try {
        // This will warn the user...
        __command.checkCommandParameters ( props, null, 1 );
    }
    catch ( Exception e ) {
        // The warning would have been printed in the check code.
        __error_wait = true;
        Message.printWarning ( 2, "", e );
    }
}

/**
Commit the edits to the command.  In this case the command parameters have
already been checked and no errors were detected.
*/
private void commitEdits ()
{   String Alias = __Alias_JTextField.getText().trim();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String SpecifyWeightsHow = __SpecifyWeightsHow_JComboBox.getSelected();
    String Weights = getWeights();
    String NewTSID = __NewTSID_JTextArea.getText().trim();
    __command.setCommandParameter ( "Alias", Alias );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "SpecifyWeightsHow", SpecifyWeightsHow );
    __command.setCommandParameter ( "Weights", Weights );
    __command.setCommandParameter ( "NewTSID", NewTSID );
}

/**
Free memory for garbage collection.
*/
protected void finalize ()
throws Throwable
{	__Alias_JTextField = null;
	__cancel_JButton = null;
	__command_JTextArea = null;
	__command = null;
	__EnsembleID_JComboBox = null;
	__SpecifyWeightsHow_JComboBox = null;
	__Year_JTextArea = null;
	__Weight_JTextArea = null;
	__ok_JButton = null;
	super.finalize ();
}

/**
Get the weights string from the GUI components.  The returned string has the format
"Year,Weight,Year,Weight".
*/
private String getWeights()
{
    StringBuffer weights = new StringBuffer();
    List<String> Year_Vector = StringUtil.breakStringList ( __Year_JTextArea.getText().trim(), ", \n", StringUtil.DELIM_SKIP_BLANKS );
    int Year_size = 0;
    if ( Year_Vector != null ) {
        Year_size = Year_Vector.size();
    }
    List<String> Weight_Vector = StringUtil.breakStringList ( __Weight_JTextArea.getText().trim(), ", \n", StringUtil.DELIM_SKIP_BLANKS );
    int Weight_size = 0;
    if ( Weight_Vector != null ) {
        Weight_size = Weight_Vector.size();
    }
    int size = Year_size;
    if ( Weight_size > size ) {
        size = Weight_size;
    }
    for ( int i = 0; i < size; i++ ) {
        if ( i > 0 ) {
            weights.append ( "," );
        }
        if ( i < Year_size ) {
            weights.append ( "" + Year_Vector.get(i) + ",");
        }
        else {
            weights.append ( "," );
        }
        if ( i < Weight_size ) {
            weights.append ( "" + Weight_Vector.get(i) );
        }
    }
    return weights.toString();
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, WeightTraces_Command command )
{   __parent_JFrame = parent;
    __command = command;  

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = 0;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Create a new time series by weighting traces from an ensemble.  The result is identified by its alias."),
		0, y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Enter trace years and weights (0.0 to 1.0), one value per line."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "Any trace value that is missing will cause the weighted result to be missing."),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, 5, 0, 10, 0, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JLabel EnsembleID_JLabel = new JLabel ("Ensemble to process:");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
            (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addEnsembleIDToEditorDialogPanel (
            this, this, main_JPanel, EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "New time series ID parts:" ),
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NewTSID_JTextArea = new JTextArea ( 3, 25 );
    __NewTSID_JTextArea.setEditable(false);
    __NewTSID_JTextArea.setLineWrap ( true );
    __NewTSID_JTextArea.setWrapStyleWord ( true );
    __NewTSID_JTextArea.addKeyListener ( this );
    // Make 3-high to fit in the edit button...
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__NewTSID_JTextArea),
        1, y, 2, 3, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Specify to avoid confusion with TSID from original TS."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    y += 2;
    JGUIUtil.addComponent(main_JPanel, (__edit_JButton =
        new SimpleJButton ( "Edit", "Edit", this ) ),
        3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    JGUIUtil.addComponent(main_JPanel, (__clear_JButton =
        new SimpleJButton ( "Clear", "Clear", this ) ),
        4, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    JGUIUtil.addComponent(main_JPanel, new JLabel("Alias to assign:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Alias_JTextField = new TSFormatSpecifiersJPanel(15);
    __Alias_JTextField.addKeyListener ( this );
    __Alias_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(main_JPanel, __Alias_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel ("Required - use %L for location, etc."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );

    JGUIUtil.addComponent(main_JPanel, new JLabel("Specify weights how?:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__SpecifyWeightsHow_JComboBox = new SimpleJComboBox ( false );
	List<String> howChoices = new ArrayList<String>();
	howChoices.add ( __command._AbsoluteWeights );
	//__weight_JComboBox.add ( __NORMALIZED_WEIGHTS );
	__SpecifyWeightsHow_JComboBox.setData(howChoices);
	__SpecifyWeightsHow_JComboBox.addItemListener ( this );
        JGUIUtil.addComponent(main_JPanel, __SpecifyWeightsHow_JComboBox,
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel( "Trace years:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Year_JTextArea = new JTextArea ( 5, 10 );
	__Year_JTextArea.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Year_JTextArea),
		1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel( "Weights:"),
		3, y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__Weight_JTextArea = new JTextArea ( 5, 10 );
	__Weight_JTextArea.addKeyListener ( this );
        JGUIUtil.addComponent(main_JPanel, new JScrollPane(__Weight_JTextArea),
		4, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

   JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:"),
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
   __command_JTextArea = new JTextArea (4,50);
   __command_JTextArea.setLineWrap ( true );
   __command_JTextArea.setWrapStyleWord ( true );
   __command_JTextArea.setEditable ( false );
   JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
       1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.BOTH, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__ok_JButton = new SimpleJButton("OK", this);
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add ( __ok_JButton );
	__cancel_JButton = new SimpleJButton("Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

    setTitle ( "Edit " + __command.getCommandName() + " command" );
    pack();
    JGUIUtil.center( this );
	setResizable ( false );
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged ( ItemEvent e )
{	refresh();
}

/**
Respond to KeyEvents.
*/
public void keyPressed ( KeyEvent event )
{	refresh();
}

public void keyReleased ( KeyEvent event )
{	refresh();
}

public void keyTyped ( KeyEvent event ) {;}

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
{	String routine = "WeightTraces_JDialog.refresh";
    String Alias = "";
    String SpecifyWeightsHow = "";
    String Weights = "";
    String EnsembleID = "";
    String NewTSID = "";
    PropList props = __command.getCommandParameters();
    if ( __first_time ) {
        __first_time = false;
        // Get the parameters from the command...
        Alias = props.getValue ( "Alias" );
        EnsembleID = props.getValue ( "EnsembleID" );
        SpecifyWeightsHow = props.getValue ( "SpecifyWeightsHow" );
        Weights = props.getValue ( "Weights" );
        NewTSID = props.getValue ( "NewTSID" );
        if ( Alias != null ) {
            __Alias_JTextField.setText ( Alias );
        }
        // Now select the item in the list.  If not a match, print a warning.
        if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleID_JComboBox, EnsembleID, JGUIUtil.NONE, null, null ) ) {
            __EnsembleID_JComboBox.select ( EnsembleID );
        }
        else {
            // Automatically add to the list after the blank...
            if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
                __EnsembleID_JComboBox.insertItemAt ( EnsembleID, 1 );
                // Select...
                __EnsembleID_JComboBox.select ( EnsembleID );
            }
            else {  // Select the first choice...
                if ( __EnsembleID_JComboBox.getItemCount() > 0 ) {
                    __EnsembleID_JComboBox.select ( 0 );
                }
            }
        }
        if ( SpecifyWeightsHow == null ) {
            // Select the default
            __SpecifyWeightsHow_JComboBox.select (0);
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __SpecifyWeightsHow_JComboBox, SpecifyWeightsHow, JGUIUtil.NONE, null, null ) ) {
    			__SpecifyWeightsHow_JComboBox.select (SpecifyWeightsHow);
    		}
            else {
                Message.printWarning ( 1, routine,
    				"Existing command references an invalid weight type \"" +
    				SpecifyWeightsHow + "\".\nSelect a different type or Cancel." );
    		}
        }
        List<String> v = StringUtil.breakStringList ( Weights, ",", StringUtil.DELIM_SKIP_BLANKS );
        int size = 0;
        if ( v != null ) {
            size = v.size();
        }
		for ( int i = 0; i < size; i++ ) {
		    if ( (i%2) == 0 ) {
		        // Year
    			if ( i != 0 ) {
    				__Year_JTextArea.append ( "\n" );
    			}
    			__Year_JTextArea.append ( (String)v.get(i) );
		    }
		    else {
		        // Weight
				if ( i != 1 ) {
					__Weight_JTextArea.append ( "\n" );
				}
				__Weight_JTextArea.append (	(String)v.get(i) );
			}
		}
        if ( NewTSID != null ) {
            __NewTSID_JTextArea.setText ( NewTSID );
        }
	}
	// Regardless, reset the command from the fields...
	Alias = __Alias_JTextField.getText().trim();
	EnsembleID = __EnsembleID_JComboBox.getSelected();
	SpecifyWeightsHow = __SpecifyWeightsHow_JComboBox.getSelected();
	Weights = getWeights();
    props = new PropList ( __command.getCommandName() );
    props.add ( "Alias=" + Alias );
    props.add ( "EnsembleID=" + EnsembleID );
    props.add ( "SpecifyWeightsHow=" + SpecifyWeightsHow );
    props.add ( "Weights=" + Weights );
    props.add ( "NewTSID=" + NewTSID );
    __command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed and the dialog is closed.
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
