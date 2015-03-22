package rti.tscommandprocessor.commands.reclamationhdb;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJButton;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import rti.tscommandprocessor.commands.reclamationhdb.ReclamationHDBConnectionUI;

/**
User interface for the ReclamationHDB connection dialog.
@author sam
*/
public class ReclamationHDBConnectionUI extends JDialog implements ActionListener
{
	
/**
Datastore factory.
*/
private ReclamationHDBDataStoreFactory factory = null;

/**
Datastore properties read from configuration file.
*/
private PropList datastoreProps = null;

/**
Datastore opened and returned by this class.
*/
private ReclamationHDBDataStore dataStore = null;

// UI components
private JTextField loginJTextField = null;
private JPasswordField passwordJPasswordField = null;
private SimpleJButton okJButton = null;
private SimpleJButton cancelJButton = null;
private JTextField statusJTextField = null;

/**
Create the editor dialog.
*/
public ReclamationHDBConnectionUI ( ReclamationHDBDataStoreFactory factory, PropList props, JFrame parent )
{	super(parent, true); // This is important - it prevents the main UI from continuing
	this.factory = factory;
	if ( props == null ) {
		props = new PropList("");
	}
	this.datastoreProps = props;
	initUI();
}

/**
Responds to action events.
@param event the event that happened.
*/
public void actionPerformed(ActionEvent event) {
	String command = event.getActionCommand();
	if (command.equals("OK")) {
		okClicked();
	}  
	else if (command.equals("Cancel")) {
	    cancelClicked();
	}
}

/**
Close the dialog without transferring any settings to the internal data.
*/
private void cancelClicked()
{	// If the datastore is non-null, then OK was tried but failed,
	// use the existing information since the status and message are helpful
	if ( getDataStore() == null ) {
		// Create datastore but do not initialize the database connection (similar to configuration file with errors).
		// Use the factory method to do the heavy lifting in creating the datastore, with properties updated from this UI
		String systemLogin = this.loginJTextField.getText().trim();
		this.datastoreProps.set("SystemLogin",systemLogin);
		String systemPassword = new String(this.passwordJPasswordField.getPassword());
		this.datastoreProps.set("SystemPassword",systemPassword);
		this.dataStore = (ReclamationHDBDataStore)this.factory.create(this.datastoreProps);
		// Factory will have set the status=1 and error to an internal exception message - be more specific here
		this.dataStore.setStatus(2);
		this.dataStore.setStatusMessage("Canceled login dialog.");
	}
	Message.printStatus(2, "", "In cancelClicked and dataStore is " + dataStore + " getDataStore=" + getDataStore());
	closeDialog();
}

/**
Close the dialog and dispose of graphical resources.
*/
private void closeDialog()
{
	setVisible(false);
	dispose();
}

/**
Return the datastore opened by the UI, or null.
*/
public ReclamationHDBDataStore getDataStore()
{
	return this.dataStore;
}

/**
Initialize the user interface.
*/
private void initUI()
{
	// used in the GridBagLayouts
	Insets LTB_insets = new Insets(7,7,0,0);
	Insets RTB_insets = new Insets(7,0,0,7);
	GridBagLayout gbl = new GridBagLayout();

	// North JPanel for the data components
	JPanel northJPanel = new JPanel();
	northJPanel.setLayout(gbl);
	getContentPane().add("North", northJPanel);

	int y = -1;	// Vertical position of components in grid bag layout
    JGUIUtil.addComponent(northJPanel, new JLabel("Specify the Reclamation HDB database user information to initialize the database connection for the datastore."), 
		0, ++y, 7, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(northJPanel, new JLabel("The datastore server and database name cannot be changed here - change in the datastore configuration file."), 
		0, ++y, 7, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

	// Server
    JGUIUtil.addComponent(northJPanel, new JLabel("Server:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    String server = this.datastoreProps.getValue("DatabaseServer");
    if ( server == null ) {
    	server = "";
    }
    JTextField serverJTextField = new JTextField(20);
    serverJTextField.setEnabled(false);
	//this.loginJTextField.addKeyListener(this);
	serverJTextField.setText(server);
    JGUIUtil.addComponent(northJPanel, serverJTextField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
	// Database
    JGUIUtil.addComponent(northJPanel, new JLabel("Database:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    String database = datastoreProps.getValue("DatabaseName");
    if ( database == null ) {
    	database = "";
    }
    JTextField databaseJTextField = new JTextField(20);
    databaseJTextField.setEnabled(false);
	//this.loginJTextField.addKeyListener(this);
    databaseJTextField.setText(database);
    JGUIUtil.addComponent(northJPanel, databaseJTextField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    
	// Login
    JGUIUtil.addComponent(northJPanel, new JLabel("Login:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
    String systemLogin = datastoreProps.getValue("SystemLogin");
    if ( (systemLogin == null) || systemLogin.equalsIgnoreCase("prompt") ) {
    	systemLogin = "";
    }
    this.loginJTextField = new JTextField(20);
	//this.loginJTextField.addKeyListener(this);
	this.loginJTextField.setText(systemLogin);
    JGUIUtil.addComponent(northJPanel, this.loginJTextField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

	// Password
    JGUIUtil.addComponent(northJPanel, new JLabel("Password:"), 
		0, ++y, 1, 1, 0, 0, LTB_insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
	String systemPassword = datastoreProps.getValue("SystemPassword");
    if ( (systemPassword == null) || systemPassword.equalsIgnoreCase("prompt") ) {
    	systemPassword = "";
    }
	passwordJPasswordField = new JPasswordField(25);
	passwordJPasswordField.setEchoChar('*');
	passwordJPasswordField.setText(systemPassword);
	//passwordJPasswordField.addKeyListener(this);
	JGUIUtil.addComponent(northJPanel, passwordJPasswordField, 
		1, y, 1, 1, 0, 0, RTB_insets, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
	
	// South JPanel
	JPanel southJPanel = new JPanel();
	southJPanel.setLayout(new BorderLayout());
	getContentPane().add("South", southJPanel);

	// South North JPanel
	JPanel southNJPanel = new JPanel();
	southNJPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
	southJPanel.add("North", southNJPanel);

	this.okJButton = new SimpleJButton("OK", "OK",this);
	southNJPanel.add(this.okJButton);

	this.cancelJButton = new SimpleJButton("Cancel", "Cancel", this);
	southNJPanel.add(this.cancelJButton);
	
	// South South JPanel
	JPanel southSJPanel = new JPanel();
	southSJPanel.setLayout(gbl);
	southJPanel.add("South", southSJPanel);

	this.statusJTextField = new JTextField();
	this.statusJTextField.setEditable(false);
	JGUIUtil.addComponent(southSJPanel, this.statusJTextField, 
		0, 0, 1, 1, 1, 1, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        
	// Dialog settings        
	setTitle("Connect to Reclamation HDB Database");
	this.statusJTextField.setText( "Enter login information for Reclamation HDB and press OK to open the database connection.");

	pack();
	JGUIUtil.center(this);
	setResizable(false);
	setVisible(true);
}

/**
Use the information in the dialog to try to instantiate a new datastore and DMI instance.
*/
private void okClicked()
{	String routine = getClass().getSimpleName() + ".okClicked";
	// Use the factory method to do the heavy lifting in creating the datastore, with properties updated from this UI
	String systemLogin = this.loginJTextField.getText().trim();
	this.datastoreProps.set("SystemLogin",systemLogin);
	String systemPassword = new String(this.passwordJPasswordField.getPassword());
	this.datastoreProps.set("SystemPassword",systemPassword);
	JGUIUtil.setWaitCursor(this, true);
	try {
		this.dataStore = (ReclamationHDBDataStore)this.factory.create(this.datastoreProps);
	}
	finally {
		JGUIUtil.setWaitCursor(this, false);
	}
	// Check whether the database connection is open.  Change the status message.
	if ( (this.dataStore.getDMI() != null) && this.dataStore.getDMI().isOpen() ) {
		// OK to close
		Message.printStatus(2, routine, "Opened datastore \"" + this.dataStore.getName() + "\" using prompt.");
		closeDialog();
	}
	else {
		this.statusJTextField.setText("Unable to connect to database with provided user information.  Check information and try again or Cancel.");
		this.statusJTextField.setBackground(Color.red);
		// Factory will have set the status=1 and error to an internal exception message - be more specific here
		this.dataStore.setStatus(2);
		this.dataStore.setStatusMessage("Invalid user information provided in login dialog.");
	}
}

}