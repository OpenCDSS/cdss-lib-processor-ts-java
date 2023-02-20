// FormatStringProperty_JDialog - editor for FormatStringProperty command

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

package rti.tscommandprocessor.commands.util;

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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

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

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.GUI.SimpleJComboBox;
import RTi.Util.Help.HelpViewer;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.String.StringFormatterSpecifiersJPanel;

@SuppressWarnings("serial")
public class FormatStringProperty_JDialog extends JDialog
implements ActionListener, DocumentListener, ItemListener, KeyListener, WindowListener
{
private SimpleJButton __cancel_JButton = null;
private SimpleJButton __ok_JButton = null;
private SimpleJButton __help_JButton = null;
private JTabbedPane __main_JTabbedPane = null;
private JTextArea __command_JTextArea = null;
private JTextField __InputProperties_JTextField = null;
private StringFormatterSpecifiersJPanel __Format_JPanel = null;
private SimpleJComboBox __IntegerFormat_JComboBox = null;
private SimpleJComboBox __Endianness_JComboBox = null;
private JTextField __Delimiter_JTextField = null;
private JTextField __NumBytes_JTextField = null;
private JTextField __OutputProperty_JTextField = null;
private SimpleJComboBox __PropertyType_JComboBox = null;
private FormatStringProperty_Command __command = null;
private boolean __error_wait = false;
private boolean __first_time = true;
private boolean __ok = false; // Indicates whether OK button has been pressed.

/**
Command dialog constructor.
@param parent JFrame class instantiating this class.
@param command Command to edit.
*/
public FormatStringProperty_JDialog ( JFrame parent, FormatStringProperty_Command command )
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
	else if ( o == __help_JButton ) {
		HelpViewer.getInstance().showHelp("command", "FormatStringProperty");
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
}

/**
Check the input.  If errors exist, warn the user and set the __error_wait flag
to true.  This should be called before response() is allowed to complete.
*/
private void checkInput ()
{	// Put together a list of parameters to check...
    String InputProperties = __InputProperties_JTextField.getText().trim();
    String Format = __Format_JPanel.getText().trim();
    String IntegerFormat = __IntegerFormat_JComboBox.getSelected();
    String Endianness = __Endianness_JComboBox.getSelected();
    String Delimiter = __Delimiter_JTextField.getText().trim();
    String NumBytes = __NumBytes_JTextField.getText().trim();
    String OutputProperty = __OutputProperty_JTextField.getText().trim();
	String PropertyType = __PropertyType_JComboBox.getSelected();
	PropList parameters = new PropList ( "" );

	__error_wait = false;

    if ( InputProperties.length() > 0 ) {
        parameters.set ( "InputProperties", InputProperties );
    }
    if ( Format.length() > 0 ) {
        parameters.set ( "Format", Format );
    }
    if ( IntegerFormat.length() > 0 ) {
        parameters.set ( "IntegerFormat", IntegerFormat );
    }
    if ( Endianness.length() > 0 ) {
        parameters.set ( "Endianness", Endianness );
    }
    if ( Delimiter.length() > 0 ) {
        parameters.set ( "Delimiter", Delimiter );
    }
    if ( NumBytes.length() > 0 ) {
        parameters.set ( "NumBytes", NumBytes );
    }
    if ( OutputProperty.length() > 0 ) {
        parameters.set ( "OutputProperty", OutputProperty );
    }
    if ( PropertyType.length() > 0 ) {
        parameters.set ( "PropertyType", PropertyType );
    }

	try {
	    // This will warn the user...
		__command.checkCommandParameters ( parameters, null, 1 );
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
{	String InputProperties = __InputProperties_JTextField.getText().trim();
    String Format = __Format_JPanel.getText().trim();
	String IntegerFormat = __IntegerFormat_JComboBox.getSelected();
    String Endianness = __Endianness_JComboBox.getSelected();
    String Delimiter = __Delimiter_JTextField.getText().trim();
    String NumBytes = __NumBytes_JTextField.getText().trim();
    String OutputProperty = __OutputProperty_JTextField.getText().trim();
	String PropertyType = __PropertyType_JComboBox.getSelected();
    __command.setCommandParameter ( "InputProperties", InputProperties );
    __command.setCommandParameter ( "Format", Format );
    __command.setCommandParameter ( "IntegerFormat", IntegerFormat );
    __command.setCommandParameter ( "Endianness", Endianness );
    __command.setCommandParameter ( "Delimiter", Delimiter );
    __command.setCommandParameter ( "NumBytes", NumBytes );
    __command.setCommandParameter ( "OutputProperty", OutputProperty );
    __command.setCommandParameter ( "PropertyType", PropertyType );
}

/**
Instantiates the GUI components.
@param parent JFrame class instantiating this class.
@param title Dialog title.
@param command The command to edit.
*/
private void initialize ( JFrame parent, FormatStringProperty_Command command )
{	__command = command;

	addWindowListener( this );

    Insets insetsTLBR = new Insets(2,2,2,2);

	JPanel main_JPanel = new JPanel();
	main_JPanel.setLayout( new GridBagLayout() );
	getContentPane().add ( "North", main_JPanel );
	int y = -1;

	JGUIUtil.addComponent(main_JPanel, new JLabel (
		"Format processor properties to set the value of another processor property." ), 
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(main_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++y, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Input properties:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __InputProperties_JTextField = new JTextField ( 30 );
    __InputProperties_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __InputProperties_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - name(s) of properties to process, separated by commas, no ${ }."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    __main_JTabbedPane = new JTabbedPane ();
    JGUIUtil.addComponent(main_JPanel, __main_JTabbedPane,
        0, ++y, 7, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    // Panel for general choices.
    int yGeneral = -1;
    JPanel general_JPanel = new JPanel();
    general_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "General", general_JPanel );

	JGUIUtil.addComponent(general_JPanel, new JLabel (
        "The following properties are used to format a list one or more of properties into a string." ), 
        0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JLabel (
        "The resulting string can optionally be converted to a different data type."),
        0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JLabel (
        "General formatting uses C-style format specifiers, including literal strings and format specifiers:" ), 
        0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JLabel (
       "  %% - literal percent character" ), 
       0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JLabel (
       "  %c - single character" ), 
       0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JLabel (
       "  %s, %-20.20s - include entire string, fit to 20 characters left-justified" ), 
       0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JLabel (
       "  %d, %4d, %04d, %-04d - include integer, pad with spaces for 4 digits, pad with zeros for 4 digits, left-justify" ), 
       0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JLabel (
       "  %f, %8.2f, %#8.2f, %-8.0f, %08.1f - include float, use width of 8 and 2 decimals, force decimal point, left-justify, pad with zeros on left" ), 
       0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JLabel ("  \\n - newline" ), 
	    0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(general_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yGeneral, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
    // TODO SAM 2012-04-10 Evaluate whether the formatter should just be the first part of the format, which
    // is supported by the panel
    JGUIUtil.addComponent(general_JPanel, new JLabel ( "Format:" ), 
        0, ++yGeneral, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Format_JPanel = new StringFormatterSpecifiersJPanel ( 35, true, true, null );
    __Format_JPanel.addKeyListener ( this );
    __Format_JPanel.addFormatterTypeItemListener (this); // Respond to changes in formatter choice
    __Format_JPanel.getDocument().addDocumentListener ( this );
    JGUIUtil.addComponent(general_JPanel, __Format_JPanel,
        1, yGeneral, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(general_JPanel, new JLabel( "Required if no IntegerFormat - format for string."),
        3, yGeneral, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    // Panel for integer choices.
    int yInteger = -1;
    JPanel integer_JPanel = new JPanel();
    integer_JPanel.setLayout( new GridBagLayout() );
    __main_JTabbedPane.addTab ( "Integer", integer_JPanel );

	JGUIUtil.addComponent(integer_JPanel, new JLabel (
        "The following properties are used to format integer input properties, which formats each byte." ), 
        0, ++yInteger, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
	JGUIUtil.addComponent(integer_JPanel, new JSeparator(SwingConstants.HORIZONTAL),
		0, ++yInteger, 7, 1, 0, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

    JGUIUtil.addComponent(integer_JPanel, new JLabel ( "Integer format:" ), 
        0, ++yInteger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __IntegerFormat_JComboBox = new SimpleJComboBox ( false );
    __IntegerFormat_JComboBox.setToolTipText("Format to use for integer input - all input must be integers.");
    List<String> formatChoices = new ArrayList<>();
    formatChoices.add ( "" ); // Default is to not do special integer formatting
    // TODO smalers 2022-01-30 - enable when have time
    //formatChoices.add ( __command._Binary );
    formatChoices.add ( __command._HexBytes );
    formatChoices.add ( __command._HexBytesUpperCase );
    __IntegerFormat_JComboBox.setData(formatChoices);
    __IntegerFormat_JComboBox.select ( __command._String );
    __IntegerFormat_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(integer_JPanel, __IntegerFormat_JComboBox,
        1, yInteger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(integer_JPanel, new JLabel(
        "Optional - format for integer input."), 
        3, yInteger, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(integer_JPanel, new JLabel ( "Endianness:" ), 
        0, ++yInteger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Endianness_JComboBox = new SimpleJComboBox ( false );
    __Endianness_JComboBox.setToolTipText("Format to use for integer input - all input must be integers.");
    List<String> endChoices = new ArrayList<>();
    endChoices.add ( "" ); // Default is to not do special integer formatting
    endChoices.add ( __command._Big );
    endChoices.add ( __command._Little );
    __Endianness_JComboBox.setData(endChoices);
    __Endianness_JComboBox.select ( __command._String );
    __Endianness_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(integer_JPanel, __Endianness_JComboBox,
        1, yInteger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(integer_JPanel, new JLabel(
        "Optional - endianness (default=" + __command._Big + ")."), 
        3, yInteger, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(integer_JPanel, new JLabel ( "Delimiter:" ), 
        0, ++yInteger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __Delimiter_JTextField = new JTextField ( 10 );
    __Delimiter_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(integer_JPanel, __Delimiter_JTextField,
        1, yInteger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(integer_JPanel, new JLabel("Optional - delimiter between bytes, \\s for space."), 
        3, yInteger, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(integer_JPanel, new JLabel ( "Number of bytes:" ), 
        0, ++yInteger, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __NumBytes_JTextField = new JTextField ( 10 );
    __NumBytes_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(integer_JPanel, __NumBytes_JTextField,
        1, yInteger, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(integer_JPanel, new JLabel("Optional - number of bytes per integer to output."), 
        3, yInteger, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output property:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __OutputProperty_JTextField = new JTextField ( 30 );
    __OutputProperty_JTextField.addKeyListener ( this );
    JGUIUtil.addComponent(main_JPanel, __OutputProperty_JTextField,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel("Required - name of output property."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    
    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Output property type:" ), 
        0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    __PropertyType_JComboBox = new SimpleJComboBox ( false );
    List<String> typeChoices = new ArrayList<>();
    typeChoices.add ( "" ); // Default is string
    typeChoices.add ( __command._DateTime );
    typeChoices.add ( __command._Double );
    typeChoices.add ( __command._Integer );
    typeChoices.add ( __command._String );
    __PropertyType_JComboBox.setData(typeChoices);
    __PropertyType_JComboBox.select ( __command._String );
    __PropertyType_JComboBox.addItemListener ( this );
    JGUIUtil.addComponent(main_JPanel, __PropertyType_JComboBox,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(main_JPanel, new JLabel(
        "Optional - output property type (default=" + __command._String + ")."), 
        3, y, 4, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);

    JGUIUtil.addComponent(main_JPanel, new JLabel ( "Command:" ), 
		0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
	__command_JTextArea = new JTextArea ( 4, 55 );
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

    button_JPanel.add ( __ok_JButton = new SimpleJButton("OK", this) );
	__ok_JButton.setToolTipText("Save changes to command");
	button_JPanel.add(__cancel_JButton = new SimpleJButton("Cancel", this));
	__cancel_JButton.setToolTipText("Cancel without saving changes to command");
	button_JPanel.add ( __help_JButton = new SimpleJButton("Help", this) );
	__help_JButton.setToolTipText("Show command documentation in web browser");

	setTitle ( "Edit " + __command.getCommandName() + " Command" );
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
{	checkGUIState();
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
	else {
	    // Combo box...
		refresh();
	}
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
{	return __ok;
}

/**
Refresh the command from the other text field contents.
*/
private void refresh ()
{	String routine = getClass().getSimpleName() + ".refresh";
    String InputProperties = "";
    String Format = "";
    String IntegerFormat = "";
    String Endianness = "";
    String Delimiter = "";
    String NumBytes = "";
    String OutputProperty = "";
    String PropertyType = "";

	PropList props = __command.getCommandParameters();
	if ( __first_time ) {
		__first_time = false;
		// Get the parameters from the command...
        InputProperties = props.getValue ( "InputProperties" );
        Format = props.getValue ( "Format" );
		IntegerFormat = props.getValue ( "IntegerFormat" );
		Endianness = props.getValue ( "Endianness" );
		Delimiter = props.getValue ( "Delimiter" );
		NumBytes = props.getValue ( "NumBytes" );
		OutputProperty = props.getValue ( "OutputProperty" );
		PropertyType = props.getValue ( "PropertyType" );
        if ( InputProperties != null ) {
            __InputProperties_JTextField.setText ( InputProperties );
        }
        if ( Format != null ) {
            __Format_JPanel.setText ( Format );
            __main_JTabbedPane.setSelectedIndex(0);
        }
        if ( IntegerFormat == null ) {
            // Select default...
            __IntegerFormat_JComboBox.select ( 0 );
        }
        else {
            __main_JTabbedPane.setSelectedIndex(1);
            if ( JGUIUtil.isSimpleJComboBoxItem( __IntegerFormat_JComboBox,IntegerFormat, JGUIUtil.NONE, null, null ) ) {
                __IntegerFormat_JComboBox.select ( IntegerFormat );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nIntegerFormat value \"" + IntegerFormat +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Endianness == null ) {
            // Select default...
            __Endianness_JComboBox.select ( 0 );
        }
        else {
            if ( JGUIUtil.isSimpleJComboBoxItem( __Endianness_JComboBox,Endianness, JGUIUtil.NONE, null, null ) ) {
                __Endianness_JComboBox.select ( Endianness );
            }
            else {
                Message.printWarning ( 1, routine,
                "Existing command references an invalid\nEndianness value \"" + Endianness +
                "\".  Select a different value or Cancel.");
                __error_wait = true;
            }
        }
        if ( Delimiter != null ) {
            __Delimiter_JTextField.setText ( Delimiter );
        }
        if ( NumBytes != null ) {
            __NumBytes_JTextField.setText ( NumBytes );
        }
        if ( OutputProperty != null ) {
            __OutputProperty_JTextField.setText ( OutputProperty );
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
	}
	// Regardless, reset the command from the fields...
	InputProperties = __InputProperties_JTextField.getText();
	Format = __Format_JPanel.getText().trim();
    IntegerFormat = __IntegerFormat_JComboBox.getSelected();
    Endianness = __Endianness_JComboBox.getSelected();
    Delimiter = __Delimiter_JTextField.getText();
    NumBytes = __NumBytes_JTextField.getText();
    OutputProperty = __OutputProperty_JTextField.getText();
    PropertyType = __PropertyType_JComboBox.getSelected();
	props = new PropList ( __command.getCommandName() );
    props.add ( "InputProperties=" + InputProperties );
    props.add ( "Format=" + Format );
    props.add ( "IntegerFormat=" + IntegerFormat );
    props.add ( "Endianness=" + Endianness );
    props.add ( "Delimiter=" + Delimiter );
    props.add ( "NumBytes=" + NumBytes );
    props.add ( "OutputProperty=" + OutputProperty );
    props.add ( "PropertyType=" + PropertyType );
	__command_JTextArea.setText( __command.toString ( props ).trim() );
}

/**
React to the user response.
@param ok if false, then the edit is canceled.  If true, the edit is committed
and the dialog is closed.
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
