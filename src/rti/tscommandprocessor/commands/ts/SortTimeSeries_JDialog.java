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

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import RTi.TS.TSFormatSpecifiersJPanel;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;

public class SortTimeSeries_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private SimpleJComboBox __TSIDFormat_JComboBox = null;
private JTextField __Property_JTextField = null;
private TSFormatSpecifiersJPanel __PropertyFormat_JTextField = null;
private SimpleJComboBox __SortOrder_JComboBox = null;
private JTextArea __command_JTextArea = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private SortTimeSeries_Command __command = null;
private boolean __ok = false;

/**
Command editor constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public SortTimeSeries_JDialog ( JFrame parent, SortTimeSeries_Command command )
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

//Start event handlers for DocumentListener...

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
{	// Put together a list of parameters to check...
    PropList props = new PropList ( "" );
    String TSIDFormat = __TSIDFormat_JComboBox.getSelected();
    String Property = __Property_JTextField.getText().trim();
    String PropertyFormat = __PropertyFormat_JTextField.getText().trim();
    String SortOrder = __SortOrder_JComboBox.getSelected();
    
    __error_wait = false;

    if (TSIDFormat != null && TSIDFormat.length() > 0) {
        props.set("TSIDFormat", TSIDFormat);
    }
    if (Property.length() > 0) {
        props.set("Property", Property);
    }
    if (PropertyFormat.length() > 0) {
        props.set("PropertyFormat", PropertyFormat);
    }
    if (SortOrder.length() > 0) {
        props.set("SortOrder", SortOrder);
    }

    try {
        // This will warn the user...
        __command.checkCommandParameters ( props, null, 1 );
    } 
    catch ( Exception e ) {
        // The warning would have been printed in the check code.
        __error_wait = true;
    }
}

/**
Commit the edits to the command.
*/
private void commitEdits ()
{   String TSIDFormat = __TSIDFormat_JComboBox.getSelected();
    String Property = __Property_JTextField.getText().trim();
    String PropertyFormat = __PropertyFormat_JTextField.getText().trim();
    String SortOrder = __SortOrder_JComboBox.getSelected();
    
    __command.setCommandParameter("TSIDFormat", TSIDFormat);
    __command.setCommandParameter("Property", Property);
    __command.setCommandParameter("PropertyFormat", PropertyFormat);
    __command.setCommandParameter("SortOrder", SortOrder);
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
private void initialize ( JFrame parent, SortTimeSeries_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	// Main panel...

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

    JGUIUtil.addComponent(main_JPanel, new JLabel (
		"This command sorts time series using one of the following methods." ),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel (
        "The default is to sort based on the full time series identifier." ),
        0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // Panel to sort ID
    int yId = -1;
    JPanel id_JPanel = new JPanel();
    id_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "By Identifier", id_JPanel );
    
    JGUIUtil.addComponent(id_JPanel, new JLabel (
        "Specify how to sort using the alias and/or identifier." ),
        0, ++yId, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(id_JPanel, new JLabel ( "TSID format:"),
        0, ++yId, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __TSIDFormat_JComboBox = new SimpleJComboBox ( false );
    __TSIDFormat_JComboBox.add ( "" );
    __TSIDFormat_JComboBox.add ( __command._AliasTSID );
    __TSIDFormat_JComboBox.add ( __command._TSID );
    __TSIDFormat_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(id_JPanel, __TSIDFormat_JComboBox,
        1, yId, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(id_JPanel, new JLabel("Optional - indicate how TSIDFormat should be sorted (default=" + __command._TSID + ")."), 
        3, yId, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
     
    // Panel for property to sort
    int yProp = -1;
    JPanel prop_JPanel = new JPanel();
    prop_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "By Property", prop_JPanel );
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "Specify the name of a time series property to sort by." ),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "Numerical values are sorted as numbers.  Otherwise, sorts are performed on string equivalents." ),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel (
        "A missing (null) property is treated as a blank string or small number." ),
        0, ++yProp, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Property to sort:"),
        0, ++yProp, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Property_JTextField = new JTextField (10);
    __Property_JTextField.addKeyListener (this);
    JGUIUtil.addComponent(prop_JPanel, __Property_JTextField,
        1, yProp, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(prop_JPanel, new JLabel ("Optional - time series property to sort."),
        3, yProp, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    
    // Panel to sort formatted properties
    int yFormatted = -1;
    JPanel format_JPanel = new JPanel();
    format_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "By Formatted Properties", format_JPanel );
    
    JGUIUtil.addComponent(format_JPanel, new JLabel (
        "Specify how to sort using a string formatted from time series properties." ),
        0, ++yFormatted, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(format_JPanel, new JLabel (
        "The notation ${ts:Property} can also be used to specify a time series property." ),
        0, ++yFormatted, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(format_JPanel, new JLabel ( "Format for properties to sort:"),
        0, ++yFormatted, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyFormat_JTextField = new TSFormatSpecifiersJPanel(30);
    __PropertyFormat_JTextField.addKeyListener ( this );
    __PropertyFormat_JTextField.getDocument().addDocumentListener(this);
    JGUIUtil.addComponent(format_JPanel, __PropertyFormat_JTextField,
        1, yFormatted, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(format_JPanel, new JLabel("Optional - indicate format for property string to sort."), 
        3, yFormatted, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Sort order:"),
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __SortOrder_JComboBox = new SimpleJComboBox ( false );
    __SortOrder_JComboBox.add ( "" );
    __SortOrder_JComboBox.add ( __command._Ascending );
    __SortOrder_JComboBox.add ( __command._Descending );
    __SortOrder_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __SortOrder_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Optional - sort order (default=" + __command._Ascending + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 60 );
	__command_JTextArea.setLineWrap ( true );
	__command_JTextArea.setWrapStyleWord ( true );
	__command_JTextArea.addKeyListener ( this );
	__command_JTextArea.setEditable ( false );
	JGUIUtil.addComponent(main_JPanel, new JScrollPane(__command_JTextArea),
		1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Refresh the contents...
	refresh ();

	// South Panel: North
	JPanel button_JPanel = new JPanel();
	button_JPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        JGUIUtil.addComponent(main_JPanel, button_JPanel, 
		0, ++y, 8, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.CENTER);

	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );

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
{	refresh();
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
Refresh the command from the input field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
    String TSIDFormat = "";
    String Property = "";
    String PropertyFormat = "";
    String SortOrder = "";

    PropList props = null;
    
    if (__first_time) {
        __first_time = false;
        
        // Get the properties from the command
        props = __command.getCommandParameters();
        TSIDFormat = props.getValue("TSIDFormat");
        Property = props.getValue("Property");
        PropertyFormat = props.getValue("PropertyFormat");
        SortOrder = props.getValue("SortOrder");
        // Set the control fields
        if ( TSIDFormat == null ) {
            // Select default...
            __TSIDFormat_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __TSIDFormat_JComboBox, TSIDFormat, JGUIUtil.NONE, null, null )) {
                __TSIDFormat_JComboBox.select ( TSIDFormat );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n" +
                "TSIDFormat value \"" + TSIDFormat + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if (Property != null) {
            __Property_JTextField.setText(Property);
            __main_JTabbedPane.setSelectedIndex(1);
        }
        if (PropertyFormat != null) {
            __PropertyFormat_JTextField.setText(PropertyFormat);
            __main_JTabbedPane.setSelectedIndex(2);
        }
        if ( SortOrder == null ) {
            // Select default...
            __SortOrder_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem(
                __SortOrder_JComboBox, SortOrder, JGUIUtil.NONE, null, null )) {
                __SortOrder_JComboBox.select ( SortOrder );
            }
            else {
                Message.printWarning ( 1, routine, "Existing command references an invalid\n" +
                "SortOrder value \"" + SortOrder + "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
    }
    
    // Regardless, reset the command from the fields.  This is only  visible
    // information that has not been committed in the command.
    TSIDFormat = __TSIDFormat_JComboBox.getSelected();
    Property = __Property_JTextField.getText().trim();
    PropertyFormat = __PropertyFormat_JTextField.getText().trim();
    SortOrder = __SortOrder_JComboBox.getSelected();
    
    props = new PropList(__command.getCommandName());
    props.add("TSIDFormat=" + TSIDFormat);
    props.add("Property=" + Property);
    props.add("PropertyFormat=" + PropertyFormat);
    props.add("SortOrder=" + SortOrder);
    __command_JTextArea.setText( __command.toString(props) );
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