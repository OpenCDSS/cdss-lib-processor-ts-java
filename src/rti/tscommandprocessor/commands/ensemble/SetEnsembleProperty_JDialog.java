package rti.tscommandprocessor.commands.ensemble;

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

import java.util.ArrayList;
import java.util.List;

import rti.tscommandprocessor.core.EnsembleListType;
import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.ui.CommandEditorUtil;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

/**
Command editor dialog for the SetEnsembleProperty() command.
*/
@SuppressWarnings("serial")
public class SetEnsembleProperty_JDialog extends JDialog
implements ActionListener, DocumentListener, KeyListener, ItemListener, WindowListener
{

private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SetEnsembleProperty_Command __command = null;
private JTextArea __command_JTextArea=null;
private SimpleJComboBox	__EnsembleList_JComboBox = null;
private JLabel __EnsembleID_JLabel = null;
private SimpleJComboBox __EnsembleID_JComboBox = null;
private JTabbedPane __props_JTabbedPane = null;
private JTextField __Name_JTextField = null;
private SimpleJComboBox __PropertyType_JComboBox = null;
private JTextField __PropertyValue_JTextField = null;
private JTextField __PropertyName_JTextField = null;
private boolean __error_wait = false; // Is there an error to be cleared up or Cancel?
private boolean __first_time = true;
private boolean __ok = false; // Has user has pressed OK to close the dialog?

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SetEnsembleProperty_JDialog ( JFrame parent, SetEnsembleProperty_Command command )
{	super(parent, true);
	initialize ( parent, command );
}

/**
Responds to ActionEvents.
@param event ActionEvent object
*/
public void actionPerformed( ActionEvent event )
{	Object o = event.getSource();

	if ( o == __cancel_JButton ) {
		response ( false );
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
    String EnsembleList = __EnsembleList_JComboBox.getSelected();
    if ( EnsembleListType.ALL_MATCHING_ENSEMBLEID.equals(EnsembleList) ||
    	EnsembleListType.FIRST_MATCHING_ENSEMBLEID.equals(EnsembleList) ||
    	EnsembleListType.LAST_MATCHING_ENSEMBLEID.equals(EnsembleList) ) {
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
	String EnsembleList = __EnsembleList_JComboBox.getSelected();
    String EnsembleID = __EnsembleID_JComboBox.getSelected();
    String Name = __Name_JTextField.getText().trim();
    String PropertyName = __PropertyName_JTextField.getText().trim();
    String PropertyType = __PropertyType_JComboBox.getSelected();
    String PropertyValue = __PropertyValue_JTextField.getText().trim();

	__error_wait = false;
	
	if ( EnsembleList.length() > 0 ) {
		parameters.set ( "TSList", EnsembleList );
	}
    if ( EnsembleID.length() > 0 ) {
        parameters.set ( "EnsembleID", EnsembleID );
    }
    if ( Name.length() > 0 ) {
        parameters.set ( "Name", Name );
    }
    if ( PropertyName.length() > 0 ) {
        parameters.set ( "PropertyName", PropertyName );
    }
    if ( PropertyType.length() > 0 ) {
        parameters.set ( "PropertyType", PropertyType );
    }
    if ( PropertyValue.length() > 0 ) {
        parameters.set ( "PropertyValue", PropertyValue );
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
{	String EnsembleList = __EnsembleList_JComboBox.getSelected();
	String EnsembleID = __EnsembleID_JComboBox.getSelected();
	String Name = __Name_JTextField.getText().trim();
	String PropertyName = __PropertyName_JTextField.getText().trim();
	String PropertyType = __PropertyType_JComboBox.getSelected();
	String PropertyValue = __PropertyValue_JTextField.getText().trim();
	__command.setCommandParameter ( "EnsembleList", EnsembleList );
    __command.setCommandParameter ( "EnsembleID", EnsembleID );
    __command.setCommandParameter ( "Name", Name );
    __command.setCommandParameter ( "PropertyName", PropertyName );
    __command.setCommandParameter ( "PropertyType", PropertyType );
    __command.setCommandParameter ( "PropertyValue", PropertyValue );
}

/**
Instantiates the GUI components.
@param parent Frame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, SetEnsembleProperty_Command command )
{	__command = command;
	
    addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Set time series ensemble properties (metadata)." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"The time series ensemble identifier (EnsembleID) cannot be changed because it is fundamental to ensembles during processing."),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    __EnsembleList_JComboBox = new SimpleJComboBox(false);
    y = CommandEditorUtil.addEnsembleListToEditorDialogPanel ( this, main_JPanel, __EnsembleList_JComboBox, y );
    
    __EnsembleID_JLabel = new JLabel ("EnsembleID (for EnsembleList=" + EnsembleListType.ALL_MATCHING_ENSEMBLEID.toString() + "):");
    __EnsembleID_JComboBox = new SimpleJComboBox ( true ); // Allow edits
    __EnsembleID_JComboBox.setToolTipText("Select an ensemble identifier from the list or specify with ${Property} notation");
    List<String> EnsembleIDs = TSCommandProcessorUtil.getEnsembleIdentifiersFromCommandsBeforeCommand(
        (TSCommandProcessor)__command.getCommandProcessor(), __command );
    y = CommandEditorUtil.addTSIDToEditorDialogPanel (
        this, this, main_JPanel, __EnsembleID_JLabel, __EnsembleID_JComboBox, EnsembleIDs, y );
    
    __props_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __props_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
     
    // Panel for built-in properties
    int yBuiltIn = -1;
    JPanel builtIn_JPanel = new JPanel();
    builtIn_JPanel.setLayout( new GridBagLayout() );
    __props_JTabbedPane.addTab ( "Built-in properties", builtIn_JPanel );

    JGUIUtil.addComponent(builtIn_JPanel, new JLabel ( "Built-in properties are core to the time series ensemble design." ), 
        0, ++yBuiltIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yBuiltIn, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(builtIn_JPanel, new JLabel("Name:"),
        0, ++yBuiltIn, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Name_JTextField = new JTextField(10);
    //__Name_JTextField.getTextField().setToolTipText("Use %L for location, %T for data type, %I for interval, also ${ts:Property} and ${Property}.");
    __Name_JTextField.addKeyListener ( this );
    __Name_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(builtIn_JPanel, __Name_JTextField,
        1, yBuiltIn, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(builtIn_JPanel,
        new JLabel ("Optional - ensemble name."),
        3, yBuiltIn, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
        
    // Panel for user-defined (not built-in) properties
    int yUser = -1;
    JPanel user_JPanel = new JPanel();
    user_JPanel.setLayout( new GridBagLayout() );
    __props_JTabbedPane.addTab ( "User-defined properties", user_JPanel );

    JGUIUtil.addComponent(user_JPanel, new JLabel ( "User-defined properties can be referenced later with ${tsensemble:Property} notation." ), 
        0, ++yUser, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JLabel (
        "User-defined properties require that all three of the following parameters are specified." ), 
        0, ++yUser, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JSeparator (SwingConstants.HORIZONTAL),
		0, ++yUser, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(user_JPanel, new JLabel ( "Property name:" ), 
        0, ++yUser, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyName_JTextField = new JTextField ( 20 );
    __PropertyName_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(user_JPanel, __PropertyName_JTextField,
        1, yUser, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JLabel(
        "Required - name of property (case-specific)."), 
        3, yUser, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(user_JPanel, new JLabel ( "Property type:" ), 
        0, ++yUser, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyType_JComboBox = new SimpleJComboBox ( false );
    List<String> typeChoices = new ArrayList<String>();
    typeChoices.add ( "" );
    typeChoices.add ( __command._DateTime );
    typeChoices.add ( __command._Double );
    typeChoices.add ( __command._Integer );
    typeChoices.add ( __command._String );
    __PropertyType_JComboBox.setData(typeChoices);
    __PropertyType_JComboBox.select ( __command._String );
    __PropertyType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(user_JPanel, __PropertyType_JComboBox,
        1, yUser, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JLabel(
        "Required - to ensure proper property object initialization."), 
        3, yUser, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(user_JPanel, new JLabel ( "Property value:" ), 
        0, ++yUser, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyValue_JTextField = new JTextField ( 20 );
    __PropertyValue_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(user_JPanel, __PropertyValue_JTextField,
        1, yUser, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(user_JPanel, new JLabel( "Required - property value as string."), 
        3, yUser, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
    		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __command_JTextArea = new JTextArea ( 4, 50 );
    __command_JTextArea.setLineWrap ( true );
    __command_JTextArea.setWrapStyleWord ( true );
    __command_JTextArea.setEditable ( false );
    JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
    		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
    checkGUIState();
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	__cancel_JButton = new SimpleJButton("Cancel", "Cancel", this);
	button_JPanel.add ( __cancel_JButton );
	__ok_JButton = new SimpleJButton("OK", "OK", this);
	button_JPanel.add ( __ok_JButton );

	setTitle ( "Edit " + __command.getCommandName() + "() Command" );
	setResizable ( true );
    pack();
    JGUIUtil.center( this );
	refresh();
    super.setVisible( true );
}

/**
Handle ItemEvent events.
@param e ItemEvent to handle.
*/
public void itemStateChanged (ItemEvent e) {
    checkGUIState();
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
	String EnsembleList = "";
    String EnsembleID = "";
    String Name = "";
    String PropertyName = "";
    String PropertyType = "";
    String PropertyValue = "";
	__error_wait = false;
	PropList parameters = null;
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
		parameters = __command.getCommandParameters();
		EnsembleList = parameters.getValue ( "EnsembleList" );
        EnsembleID = parameters.getValue ( "EnsembleID" );
        Name = parameters.getValue ( "Name" );
        PropertyName = parameters.getValue ( "PropertyName" );
        PropertyType = parameters.getValue ( "PropertyType" );
        PropertyValue = parameters.getValue ( "PropertyValue" );
		if ( EnsembleList == null ) {
			// Select default...
			__EnsembleList_JComboBox.select ( 0 );
		}
		else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __EnsembleList_JComboBox,EnsembleList, JGUIUtil.NONE, null, null ) ) {
            	__EnsembleList_JComboBox.select ( EnsembleList );
			}
			else {
                Message.printWarning ( 1, routine,
				"Existing command references an invalid\nEnsembleList value \"" + EnsembleList +
				"\".  Select a different value or Cancel.");
				__error_wait = true;
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
        if ( Name != null ) {
            __Name_JTextField.setText(Name);
        }
        if ( PropertyName != null ) {
            __PropertyName_JTextField.setText ( PropertyName );
            if ( !PropertyName.equals("") ) {
                __props_JTabbedPane.setSelectedIndex(1);
            }
        }
        if ( PropertyType == null ) {
            // Select default...
            __PropertyType_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __PropertyType_JComboBox,PropertyType, JGUIUtil.NONE, null, null ) ) {
                __PropertyType_JComboBox.select ( PropertyType );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nPropertyType value \"" + PropertyType +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( PropertyValue != null ) {
            __PropertyValue_JTextField.setText ( PropertyValue );
        }
	}
	// Regardless, reset the command from the fields...
	EnsembleList = __EnsembleList_JComboBox.getSelected();
    EnsembleID = __EnsembleID_JComboBox.getSelected();
    Name = __Name_JTextField.getText().trim();
    PropertyName = __PropertyName_JTextField.getText().trim();
    PropertyType = __PropertyType_JComboBox.getSelected();
    PropertyValue = __PropertyValue_JTextField.getText().trim();
	parameters = new PropList ( __command.getCommandName() );
	parameters.add ( "EnsembleList=" + EnsembleList );
    parameters.add ( "EnsembleID=" + EnsembleID );
    parameters.add ( "Name=" + Name );
    parameters.add ( "PropertyName=" + PropertyName );
    parameters.add ( "PropertyType=" + PropertyType );
    parameters.add ( "PropertyValue=" + PropertyValue );
	__command_JTextArea.setText( __command.toString ( parameters ) );
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