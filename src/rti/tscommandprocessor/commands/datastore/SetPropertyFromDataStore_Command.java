// SetPropertyFromDataStore_Command - this class initializes, checks, and runs the SetPropertyFromDataStore() command.

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

package rti.tscommandprocessor.commands.datastore;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.List;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.Prop;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;
import riverside.datastore.DataStore;

/**
This class initializes, checks, and runs the SetPropertyFromDataStore() command.
*/
public class SetPropertyFromDataStore_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

	/**
	Possible value for PropertyType.
	*/
	//protected final String _DateTime = "DateTime";
	//protected final String _Double = "Double";
	//protected final String _Integer = "Integer";
	//protected final String _String = "String";

	/**
	 * Values for AllowNull.
	 */
	protected final String _False = "False";
	protected final String _True = "True";

	/**
	Property set during discovery.
	*/
	private Prop __discovery_Prop = null;

	/**
	Constructor.
	*/
	public SetPropertyFromDataStore_Command () {
		super();
		setCommandName ( "SetPropertyFromDataStore" );
	}

	/**
	Check the command parameter for valid values, combination, etc.
	@param parameters The parameters for the command.
	@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
	@param warning_level The warning level to use when printing parse warnings
	(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
	*/
	public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
	throws InvalidCommandParameterException {
		String DataStore = parameters.getValue ( "DataStore" );
		String DataStoreProperty = parameters.getValue ( "DataStoreProperty" );
		String PropertyName = parameters.getValue ( "PropertyName" );
		//String PropertyType = parameters.getValue ( "PropertyType" );
		//String PropertyValue = parameters.getValue ( "PropertyValue" );
		String AllowNull = parameters.getValue ( "AllowNull" );
		String warning = "";
    	String message;

    	CommandStatus status = getCommandStatus();
    	status.clearLog(CommandPhaseType.INITIALIZATION);

    	if ( (DataStore == null) || DataStore.equals("") ) {
        	message = "The datastore must be specified.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
               	new CommandLogRecord(CommandStatusType.FAILURE,
               		message, "Specify the datastore." ) );
    	}

    	if ( (DataStoreProperty == null) || DataStoreProperty.equals("") ) {
        	message = "The datastore property name must be specified.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
               	new CommandLogRecord(CommandStatusType.FAILURE,
                   	message, "Provide a datastore property name." ) );
    	}
    	else {
        	// Check for allowed characters.
        	if ( StringUtil.containsAny(DataStoreProperty,"${}() \t", true)) {
            	message = "The datastore property name cannot contains invalid characters.";
            	warning += "\n" + message;
            	status.addToLog ( CommandPhaseType.INITIALIZATION,
                	new CommandLogRecord(CommandStatusType.FAILURE, message,
                    	"Specify a datastore property name that does not include the characters $(){}, space, or tab." ) );
        	}
    	}

    	if ( (PropertyName == null) || PropertyName.equals("") ) {
        	message = "The property name must be specified.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
               	new CommandLogRecord(CommandStatusType.FAILURE,
                   	message, "Provide a property name." ) );
    	}
    	else {
        	// Check for allowed characters.
        	if ( StringUtil.containsAny(PropertyName,"${}() \t", true)) {
            	message = "The property name cannot contains invalid characters.";
            	warning += "\n" + message;
            	status.addToLog ( CommandPhaseType.INITIALIZATION,
                	new CommandLogRecord(CommandStatusType.FAILURE, message,
                    	"Specify a property name that does not include the characters $(){}, space, or tab." ) );
        	}
    	}

    	/*
    	if ( (PropertyType == null) || PropertyType.equals("") ) {
        	message = "The property type must be specified.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
              	new CommandLogRecord(CommandStatusType.FAILURE,
                   	message, "Provide a property type." ) );
        	// Set to blank to be able to do checks below
        	PropertyType = "";
    	}
		if ( (PropertyValue == null) || PropertyValue.equals("") ) {
        	message = "The property value must be specified.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
               	new CommandLogRecord(CommandStatusType.FAILURE,
                   	message, "Provide a property value." ) );
		}
		/ * TODO SAM 2008-02-14 Should this be checked?  Only in run phase?
		else {
	    	// Check the value given the type.
	    	if ( PropertyType.equalsIgnoreCase(_DateTime) && !TimeUtil.isDateTime(PropertyValue) ) {
	        	message = "The property value \"" + PropertyValue + "\" is not a valid date/time.";
	        	warning += "\n" + message;
	        	status.addToLog ( CommandPhaseType.INITIALIZATION,
	            	new CommandLogRecord(CommandStatusType.FAILURE,
	                	message, "Specify the property value as a date/time (e.g., YYYY-MM-DD if a date)" ));
	    	}
	    	else if ( PropertyType.equalsIgnoreCase(_Double) && !StringUtil.isDouble(PropertyValue) ) {
    			message = "The property value \"" + PropertyValue + "\" is not a number.";
            	warning += "\n" + message;
            	status.addToLog ( CommandPhaseType.INITIALIZATION,
                   	new CommandLogRecord(CommandStatusType.FAILURE,
                       	message, "Specify the property value as a number" ));
			}
			else if ( PropertyType.equalsIgnoreCase(_Integer) && !StringUtil.isInteger(PropertyValue) ) {
            	message = "The property value \"" + PropertyValue + "\" is not an integer";
            	warning += "\n" + message;
            	status.addToLog ( CommandPhaseType.INITIALIZATION,
                   	new CommandLogRecord(CommandStatusType.FAILURE,
                      	message, "Specify the property value as an integer." ));
			}
		}
		* /
    	*/

    	if ( (AllowNull != null) && !AllowNull.equals("") ) {
		   	if ( !AllowNull.equalsIgnoreCase(_False) && !AllowNull.equalsIgnoreCase(_True) ) {
			   	message = "The AllowNull parameter \"" + AllowNull + "\" is invalid.";
			   	warning += "\n" + message;
			   	status.addToLog(CommandPhaseType.INITIALIZATION,
				   	new CommandLogRecord(CommandStatusType.FAILURE,
					   	message, "Specify the parameter as " + _False + " (default) or " + _True + "."));
		   	}
	   	}

    	// Check for invalid parameters.
		List<String> validList = new ArrayList<>(3);
    	validList.add ( "DataStore" );
    	validList.add ( "DataStoreProperty" );
    	validList.add ( "PropertyName" );
    	//validList.add ( "PropertyType" );
    	//validList.add ( "PropertyValue" );
    	validList.add ( "AllowNull" );
    	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

    	if ( warning.length() > 0 ) {
		   	Message.printWarning ( warning_level,
		   	MessageUtil.formatMessageTag(command_tag,warning_level),
		   	warning );
		   	throw new InvalidCommandParameterException ( warning );
	   	}

       	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
   	}

   	/**
   	Edit the command.
   	@param parent The parent JFrame to which the command dialog will belong.
   	@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
   	*/
   	public boolean editCommand ( JFrame parent ) {
	   	// The command will be modified if changed.
   		List<DataStore> dataStoreList =
		  	TSCommandProcessorUtil.getDataStoresForEditors ( (TSCommandProcessor)this.getCommandProcessor(), this );
	   	return (new SetPropertyFromDataStore_JDialog ( parent, this, dataStoreList )).ok();
   	}

   	/**
   	Return the property defined in discovery phase.
   	*/
   	private Prop getDiscoveryProp () {
       	return __discovery_Prop;
   	}

   	/**
   	Return the list of data objects read by this object in discovery mode.
   	The following classes can be requested:  Prop
   	*/
   	@SuppressWarnings("unchecked")
   	public <T> List<T> getObjectList ( Class<T> c ) {
       	Prop discovery_Prop = getDiscoveryProp ();
       	if ( discovery_Prop == null ) {
           	return null;
       	}
       	Prop prop = new Prop();
       	// Check for TS request or class that matches the data.
       	if ( c == prop.getClass() ) {
    	   	List<T> v = new ArrayList<>(1);
           	v.add ( (T)discovery_Prop );
           	return v;
       	}
       	else {
           	return null;
       	}
   	}

   	// Use the base class parseCommand() method.

   	/**
   	Run the command.
   	@param command_number Command number in sequence.
   	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
   	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
   	*/
   	public void runCommand ( int command_number )
   	throws InvalidCommandParameterException, CommandWarningException, CommandException {
       	runCommandInternal ( command_number, CommandPhaseType.RUN );
   	}

   	/**
   	Run the command in discovery mode.
   	@param command_number Command number in sequence.
   	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
   	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
   	*/
   	public void runCommandDiscovery ( int command_number )
   	throws InvalidCommandParameterException, CommandWarningException, CommandException {
       	runCommandInternal ( command_number, CommandPhaseType.DISCOVERY );
   	}

   	/**
   	Run the command.
   	@param commandNumber Number of command in sequence.
   	@param commandPhase The command phase that is being run (RUN or DISCOVERY).
   	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
   	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
   	@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
   	*/
   	public void runCommandInternal ( int commandNumber, CommandPhaseType commandPhase )
   	throws InvalidCommandParameterException,
   	CommandWarningException, CommandException {
	   	String routine = getClass().getSimpleName() + ".runCommand", message;
	   	int warning_count = 0;
	   	int warning_level = 2;
	   	String command_tag = "" + commandNumber;
	   	int log_level = 3;  // Level for non-use messages for log file.

	   	// Make sure there are time series available to operate on.

       	CommandStatus status = getCommandStatus();
       	status.clearLog(CommandPhaseType.RUN);

	   	PropList parameters = getCommandParameters();
	   	CommandProcessor processor = getCommandProcessor();

	   	String DataStore = parameters.getValue ( "DataStore" );
	   	String DataStoreProperty = parameters.getValue ( "DataStoreProperty" );
	   	String PropertyName = parameters.getValue ( "PropertyName" );
       	//String PropertyType = parameters.getValue ( "PropertyType" );
	   	//String PropertyValue = parameters.getValue ( "PropertyValue" );

	   	String AllowNull = parameters.getValue ( "AllowNull" );
    	boolean allowNull = false; // Default.
    	if ( (AllowNull != null) && AllowNull.equalsIgnoreCase(_True)  ) {
    		allowNull = true;
    	}

	   	// Find the data store to use:
	   	// - pass null for the class since getting all datastores
    	DataStore dataStore = ((TSCommandProcessor)processor).getDataStoreForName ( DataStore, null );
    	if ( dataStore == null ) {
    		if ( commandPhase == CommandPhaseType.RUN ) {
    			message = "Could not get datastore for name \"" + DataStore + "\" to retrive properties.";
    			Message.printWarning ( 2, routine, message );
    			status.addToLog ( commandPhase,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "Verify that a datastore has been configured with name \"" + DataStore + "\"." ) );
    		}
    	}

		if ( warning_count > 0 ) {
			message = "There were " + warning_count + " warnings for command parameters.";
			Message.printWarning ( 2,
			MessageUtil.formatMessageTag(command_tag, ++warning_count),
			routine,message);
			throw new InvalidCommandParameterException ( message );
		}

		// Run the command.
	   	try {

    	   	// Set the property in the processor.

    	   	PropList request_params = new PropList ( "" );
    	   	request_params.setUsingObject ( "PropertyName", PropertyName );

    	   	if ( commandPhase == CommandPhaseType.RUN ) {
    	   		// Get the property value from the indicated datastore:
    	   		// - all datastore properties are currently strings

    	   		String propertyValue = dataStore.getProperty(DataStoreProperty);

    	   		// TODO smalers 2023-03-19 in the future may allow the property type to be changed.
    	   		/*
	       		else {
    	       		// Convert to the proper object type.
    	       		Object Property_Object = null;
	           		if ( PropertyType.equalsIgnoreCase(_DateTime) ) {
	               		try {
	                   		Property_Object = DateTime.parse(app_default_value);
	               		}
	               		catch ( Exception e ) {
                       		message = "Error converting \"" + app_default_value + "\" to a DateTime.";
                       		Message.printWarning(log_level,
                           		MessageUtil.formatMessageTag( command_tag, ++warning_count),
                           		routine, message );
                      		status.addToLog ( command_phase,
                           		new CommandLogRecord(CommandStatusType.FAILURE,
                               		message, "Verify that the App Default value is a standard DateTime format." ) );
                       		Property_Object = null;
	               		}
	           		}
	        		else if ( PropertyType.equalsIgnoreCase(_Double) ) {
	            		try {
	                		Property_Object = Double.valueOf(app_default_value);
	            		}
                		catch ( Exception e ) {
                    		message = "Error converting \"" + app_default_value + "\" to a double.";
                    		Message.printWarning(log_level,
                           		MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            		routine, message );
                    		status.addToLog ( command_phase,
                           		new CommandLogRecord(CommandStatusType.FAILURE,
                               		message, "Verify that the App Default value is a double value." ) );
                    		Property_Object = null;
                		}
	        		}
	        		else if ( PropertyType.equalsIgnoreCase(_Integer) ) {
	            		try {
	                		Property_Object = Integer.valueOf(app_default_value);
	            		}
                		catch ( Exception e ) {
                    		message = "Error converting \"" + app_default_value + "\" to an integer.";
                    		Message.printWarning(log_level,
                           		MessageUtil.formatMessageTag( command_tag, ++warning_count),
                            		routine, message );
                    		status.addToLog ( command_phase,
                          		new CommandLogRecord(CommandStatusType.FAILURE,
                               		message, "Verify that the App Default value is an integer." ) );
                    		Property_Object = null;
                		}
	        		}
	        		else if ( PropertyType.equalsIgnoreCase(_String) ) {
	            		Property_Object = app_default_value;
	        		}
	    		}
	       		*/

    	   		// Set the property value in the processor.
    			if ( (propertyValue == null) && !allowNull ) {
        			message = "Datastore property \"" + DataStoreProperty + "\" has null value.  Not allowed to set null value.";
        			Message.printWarning ( warning_level,
           				MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        			status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
           				message, "Confirm that the property name is correct and has a non-null value, or specify AllowNull=True." ) );
    			}
	       		else {
            		request_params.setUsingObject ( "PropertyValue", propertyValue );
            		try {
                   		processor.processRequest( "SetProperty", request_params);
                   		Message.printStatus(2, routine, "Set property \"" + PropertyName + "\" = \"" + propertyValue + "\"");
            		}
               		catch ( Exception e ) {
                   		message = "Error requesting SetProperty(Property=\"" + PropertyName + "\") from processor.";
                   		Message.printWarning(log_level,
                         		MessageUtil.formatMessageTag( command_tag, ++warning_count), routine, message );
                   		status.addToLog ( commandPhase,
                          		new CommandLogRecord(CommandStatusType.FAILURE,
                              		message, "Report the problem to software support." ) );
               		}
	       		}
           	}
    	   	/*
    	   	// TODO smalers 2023-03-20 evaluate whether to support in discovery mode.
   			else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
          		// Set the property for use in discovery mode.
               	setDiscoveryProp ( new Prop(PropertyName,propertyValue, propertyValue) );
           	}
           	*/
		}
		catch ( Exception e ) {
			message = "Unexpected error setting property \""+ PropertyName + "\" from the the datastore (" + e + ").";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag(
				command_tag,++warning_count),routine,message );
			Message.printWarning(3,routine,e);
        	status.addToLog ( commandPhase,
            	new CommandLogRecord(CommandStatusType.FAILURE,
                	message, "See the log file for details - report the problem to software support." ) );
		}

		if ( warning_count > 0 ) {
			message = "There were " + warning_count + " warnings processing the command.";
			Message.printWarning ( warning_level,
				MessageUtil.formatMessageTag( command_tag, ++warning_count), routine,message);
			throw new CommandWarningException ( message );
		}
    	status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
	}

	/**
	Set the property defined in discovery phase.
	@param prop Property set during discovery phase.
	*/
	private void setDiscoveryProp ( Prop prop ) {
    	__discovery_Prop = prop;
	}

	/**
	Return the string representation of the command.
	@param parameters parameters to include in the command
	@return the string representation of the command
	*/
	public String toString ( PropList parameters ) {
		String [] parameterOrder = {
    		"DataStore",
    		"DataStoreProperty",
    		"PropertyName",
    		"AllowNull"
		};
		return this.toString(parameters, parameterOrder);
	}

}