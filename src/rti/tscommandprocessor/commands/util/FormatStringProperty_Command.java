// FormatStringProperty_Command - This class initializes, checks, and runs the FormatStringProperty() command.

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

package rti.tscommandprocessor.commands.util;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.IO.AbstractCommand;
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
import RTi.Util.Time.DateTime;

/**
This class initializes, checks, and runs the FormatStringProperty() command.
*/
public class FormatStringProperty_Command extends AbstractCommand implements CommandDiscoverable, ObjectListProvider
{

/**
Possible value for IntegerFormat.
*/
protected final String _Binary = "Binary";
protected final String _HexBytes = "HexBytes";
protected final String _HexBytesUpperCase = "HexBytesUpperCase";

/**
Possible value for Endianness.
*/
protected final String _Big = "Big";
protected final String _Little = "Little";

/**
Possible value for PropertyType.
*/
protected final String _DateTime = "DateTime";
protected final String _Double = "Double";
protected final String _Integer = "Integer";
protected final String _String = "String";

/**
Property set during discovery.
*/
private Prop __discovery_Prop = null;

/**
Constructor.
*/
public FormatStringProperty_Command () {
    super();
    setCommandName ( "FormatStringProperty" );
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
    String Format = parameters.getValue ( "Format" );
    String IntegerFormat = parameters.getValue ( "IntegerFormat" );
    String Endianness = parameters.getValue ( "Endianness" );
    String NumBytes = parameters.getValue ( "NumBytes" );
    String OutputProperty = parameters.getValue ( "OutputProperty" );
    String PropertyType = parameters.getValue ( "PropertyType" );
    String warning = "";
    String message;

    CommandStatus status = getCommandStatus();
    status.clearLog(CommandPhaseType.INITIALIZATION);

    if ( ((Format == null) || Format.isEmpty()) && ((IntegerFormat == null) || IntegerFormat.isEmpty()) ) {
        message = "The Format or IntegerFormat must be specified.";
        // TODO smalers 2022-01-30 could implement more complex checks to make sure that integer values are only allowed to use IntegerFormat.
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a format to process input." ) );
    }

    if ( (IntegerFormat != null) && !IntegerFormat.isEmpty() ) {
    	if ( !IntegerFormat.equalsIgnoreCase(_Binary) &&
    		!IntegerFormat.equalsIgnoreCase(_HexBytes) &&
    		!IntegerFormat.equalsIgnoreCase(_HexBytesUpperCase)
    		//&&
    		//!IntegerFormat.equalsIgnoreCase(_Hex0x) &&
    		//!IntegerFormat.equalsIgnoreCase(_Hex0xWithSpace)
    		) {
    		message = "The property type is invalid.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
           		new CommandLogRecord(CommandStatusType.FAILURE,
               		message, "Specify the integer format as " + _Binary + ", " + _HexBytes +
              	  		", or " + _HexBytesUpperCase + " (default)." ) );
    	}
    	// Output property type must be a string.
    	if ( (PropertyType != null) && !PropertyType.equalsIgnoreCase(_String) ) {
			message = "The property type must be " + _String + " when IntegerFormat is specified.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
            	new CommandLogRecord(CommandStatusType.FAILURE,
                	message, "Specify the property type as " + _String + " (default if not specified)." ) );
    	}
    }

    if ( (Endianness != null) && !Endianness.isEmpty() && !Endianness.equalsIgnoreCase(_Big) &&
    	!_Little.equalsIgnoreCase(_Little) ) {
		message = "The endianness is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the property type as " + _Big + " (default) or " + _Little + "." ) );
    }

    if ( (NumBytes != null) && !NumBytes.isEmpty() && !StringUtil.isInteger(NumBytes) ) {
        message = "The number of bytes is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Specify the number of bytes as an integer." ) );
    }

    if ( (OutputProperty == null) || OutputProperty.equals("") ) {
        message = "The output property must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Provide a property name for output." ) );
    }

    if ( (PropertyType != null) && !PropertyType.isEmpty() && !PropertyType.equalsIgnoreCase(_DateTime) &&
    	!PropertyType.equalsIgnoreCase(_Double) && !PropertyType.equalsIgnoreCase(_Integer) && !PropertyType.equalsIgnoreCase(_String) ) {
		message = "The property type is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the property type as " + _DateTime + ", " + _Double + ", " +
                	_Integer + ", or " + _String + " (default)." ) );
    }

    // Check for invalid parameters.
    List<String> validList = new ArrayList<>(8);
    validList.add ( "InputProperties" );
    validList.add ( "Format" );
    validList.add ( "IntegerFormat" );
    validList.add ( "Endianness" );
    validList.add ( "Delimiter" );
    validList.add ( "NumBytes" );
    validList.add ( "OutputProperty" );
    validList.add ( "PropertyType" );
    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

    if ( warning.length() > 0 ) {
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(command_tag,warning_level), warning );
        throw new InvalidCommandParameterException ( warning );
    }

    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
    return (new FormatStringProperty_JDialog ( parent, this )).ok();
}

/**
Format a string property using input from other properties.
@param processor command processor from which to get input property values
@param inputProperties the name of the first column to use as input
@param format the format to use
@param outputProperty the name of the output column
@param problems a list of strings indicating problems during processing
@return the formatted string property
*/
public String format ( TSCommandProcessor processor, String [] inputProperties, String format,
	String outputProperty, List<String> problems ) {
    //String routine = getClass().getSimpleName() + ".format" ;

    // Loop through the records, get the input column objects, and format for output.
    String outputVal = null;
    List<Object> values = new ArrayList<>();
    // Get the input values.
    values.clear();
    for ( int iProp = 0; iProp < inputProperties.length; iProp++ ) {
        try {
            values.add(processor.getPropContents(inputProperties[iProp]));
        }
        catch ( Exception e ) {
            problems.add ( "Error getting property value for \"" + inputProperties[iProp] + "\" (" + e + ")." );
            values.clear();
            break;
        }
    }
    if ( inputProperties.length != values.size() ) {
        // Don't have the right number of values from the number of specified input properties.
    	problems.add ( "Have " + inputProperties.length + " input properties but only found " +
    		values.size() + " corresponding values.  Cannot format string." );
        outputVal = null;
    }
    else {
        //Message.printStatus(2, routine, "format=\"" + format + "\"" );
        //for ( int i = 0; i < values.size(); i++ ) {
        //    Message.printStatus(2, routine, "value=\"" + values.get(i) + "\"" );
        //}
        outputVal = StringUtil.formatString(values,format);
    }
    return outputVal;
}

/**
Format a string property using a list of integers and input from other properties.
@param processor command processor from which to get input property values
@param inputProperties the name of the first column to use as input
@param integerFormat the format to use
@param endianness endianness parameter
@param delimiter delimiter between bytes
@param numBytes number of bytes to output (if 0 default from integer size)
@param outputProperty the name of the output column
@param problems a list of strings indicating problems during processing
@return the formatted string property
*/
public String formatIntegers ( TSCommandProcessor processor, String [] inputProperties,
	String integerFormat, String endianness, String delimiter, int numBytes,
	String outputProperty, List<String> problems ) {
    //String routine = getClass().getSimpleName() + ".format" ;

    // Loop through the records, get the input column objects, and format for output.
    List<Object> values = new ArrayList<>();
    // Get the input values.
    values.clear();
    for ( int iProp = 0; iProp < inputProperties.length; iProp++ ) {
    	Object value = null;
        try {
            value = processor.getPropContents(inputProperties[iProp]);
            if ( value instanceof Integer ) {
        	    // OK.
            	values.add(value);
            }
            else if ( value instanceof Long ) {
            	// TODO smalers 2022-01-30 enable in the future.
        	    // OK.
            	values.add(value);
            }
            else if ( value instanceof String ) {
        	    if ( StringUtil.isInteger((String)value) ) {
        	    	// OK.
        	    }
        	    else {
        	    	problems.add ( "Input property \"" + inputProperties[iProp] + " is a string but cannot be converted to an integer." );
        	    	values.clear();
        	    	break;
        	    }
            }
            else {
            	problems.add ( "Input property \"" + inputProperties[iProp] + " is not an integer or string that can be converted to an integer." );
            	values.clear();
            	break;
            }
        }
        catch ( Exception e ) {
            problems.add ( "Error getting property value for \"" + inputProperties[iProp] + "\" (" + e + ")." );
            values.clear();
            break;
        }
    }
    // Format 1+ integers according to the format.
    StringBuilder s = new StringBuilder();
    ByteBuffer bb = null;
    byte[] bytes;
    int numBytesOutput = 0;
    int numBytesValue = 0;
    // Java is big endian with bits ordered most significant to least significant.
    // Therefore big ending output is in the same order as in-memory byte order.
    for ( int ivalue = 0; ivalue < values.size(); ivalue++ ) {
    	Object value = values.get(ivalue);
   		if ( delimiter != null ) {
    		if ( ivalue > 0 ) {
    			// Append a space to separate the integers.
    			s.append(delimiter);
    		}
   		}
   		// Format the value.
    	if ( value instanceof Integer ) {
    		numBytesValue = 4;
    		numBytesOutput = numBytesValue; // Default.
    		// Allocate memory for the actual size.
    		bb = ByteBuffer.allocate(numBytesOutput);
    		// Adjust output for the requested number of bytes, but only to less than the memory size.
    		if ( (numBytes > 0) && (numBytes <= numBytesValue) ) {
    			numBytesOutput = numBytes;
    		}
    		bb.putInt((Integer)value );
    	}
    	else if ( value instanceof Long ) {
    		numBytesValue = 8;
    		numBytesOutput = numBytesValue; // Default.
    		// Allocate memory for the actual size.
    		bb = ByteBuffer.allocate(numBytesOutput);
    		// Adjust output for the requested number of bytes, but only to less than the memory size.
    		if ( (numBytes > 0) && (numBytes <= numBytesValue) ) {
    			numBytesOutput = numBytes;
    		}
    		bb.putLong((Integer)value );
    	}
    	// Get the bytes from the array.
    	bytes = bb.array();
    	if ( endianness.equalsIgnoreCase(_Big) ) {
    		// Big endian:
    		// - same as Java so loop in the normal order
    		// - output to include least significant bytes when less than full number of bytes
    		for ( int i = (numBytesValue - numBytesOutput); i < numBytesValue; i++ ) {
    			if ( delimiter != null ) {
    				if ( i > 0 ) {
    					// Append a space to separate the hex byte values within an integer.
    					s.append(" ");
    				}
    			}
    			if ( integerFormat.equalsIgnoreCase(_HexBytes) ) {
    				s.append(String.format("%02x", bytes[i]));
    			}
    			else if ( integerFormat.equalsIgnoreCase(_HexBytesUpperCase) ) {
    				s.append(String.format("%02X", bytes[i]));
    			}
    		}
    	}
    	else {
    		// Little endian:
    		// - output in reverse order from memory when less than full number of bytes
    		for ( int i = (numBytesValue - 1); i >= (numBytesValue - numBytesOutput); i-- ) {
    			if ( delimiter != null ) {
    				if ( i != (numBytesOutput - 1) ) {
    					// Append a space to separate the hex byte values within an integer.
    					s.append(" ");
    				}
    			}
    			if ( integerFormat.equalsIgnoreCase(_HexBytes) ) {
    				s.append(String.format("%02x", bytes[i]));
    			}
    			else if ( integerFormat.equalsIgnoreCase(_HexBytesUpperCase) ) {
    				s.append(String.format("%02X", bytes[i]));
    			}
    		}
    	}
    }
    return s.toString();
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
        List<T> v = new ArrayList<> (1);
        v.add ( (T)discovery_Prop );
        return v;
    }
    else {
        return null;
    }
}

// Parse command is in the parent class.

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
@param command_number Number of command in sequence.
@param commandPhase The command phase that is being run (RUN or DISCOVERY).
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
*/
public void runCommandInternal ( int command_number, CommandPhaseType commandPhase )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
    String message, routine = getClass().getSimpleName() + ".runCommandInternal";
    int warning_level = 2;
    String command_tag = "" + command_number;
    int warning_count = 0;
    int log_level = 3;  // Level for non-use messages for log file.

    CommandProcessor processor = getCommandProcessor();
    CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // Default.
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen.
    }
    if ( clearStatus ) {
		status.clearLog(commandPhase);
	}
    PropList parameters = getCommandParameters();

	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
		setDiscoveryProp(null);
	}

    // Get the input parameters.

    String InputProperties = parameters.getValue ( "InputProperties" );
    String [] inputPropertyNames = new String[0];
    if ( (InputProperties != null) && !InputProperties.equals("") ) {
        if ( InputProperties.indexOf(",") < 0 ) {
            inputPropertyNames = new String[1];
            inputPropertyNames[0] = InputProperties.trim();
        }
        else {
            inputPropertyNames = InputProperties.split(",");
            for ( int i = 0; i < inputPropertyNames.length; i++ ) {
                inputPropertyNames[i] = inputPropertyNames[i].trim();
            }
        }
        // Strip ${ } from properties since not used by this command:
        // - otherwise, looking up a property value during parsing will cause issues
        for ( int i = 0; i < inputPropertyNames.length; i++ ) {
        	if ( inputPropertyNames[i].startsWith("$")) {
        		inputPropertyNames[i] = inputPropertyNames[i].substring(2);
        	}
        	if ( inputPropertyNames[i].endsWith("}")) {
        		inputPropertyNames[i] = inputPropertyNames[i].substring(0,(inputPropertyNames[i].length() - 1));
        	}
        }
    }
    String Format = parameters.getValue ( "Format" );
    String IntegerFormat = parameters.getValue ( "IntegerFormat" );
    String Endianness = parameters.getValue ( "Endianness" );
    if ( (Endianness == null) || Endianness.isEmpty() ) {
    	Endianness = _Big; // Default.
    }
    String Delimiter = parameters.getValue ( "Delimiter" );
    String delimiter = null; // Default - no delimiter.
    if ( (Delimiter != null)  && !Delimiter.isEmpty() ) {
    	delimiter = Delimiter;
    	// Replace \s with space.
    	delimiter = delimiter.replace("\\s", " ");
    }
    String NumBytes = parameters.getValue ( "NumBytes" );
    int numBytes = 0; // Default - determine from integer being formatted.
    if ( (NumBytes != null) && !NumBytes.isEmpty() ) {
    	numBytes = Integer.parseInt(NumBytes);
    }
    String OutputProperty = parameters.getValue ( "OutputProperty" );
    String PropertyType = parameters.getValue ( "PropertyType" );
    String propertyType = _String; // Default.
    if ( (PropertyType != null) && !PropertyType.isEmpty() ) {
    	propertyType = PropertyType;
    }

    if ( warning_count > 0 ) {
        // Input error.
        message = "Insufficient input to run command.";
        status.addToLog ( CommandPhaseType.RUN,
        new CommandLogRecord(CommandStatusType.FAILURE, message, "Check input to command." ) );
        Message.printWarning(3, routine, message );
        throw new CommandException ( message );
    }

    // Now process.

    List<String> problems = new ArrayList<>();
    try {
    	if ( commandPhase == CommandPhaseType.RUN ) {
    		String stringProp = null;
    		if ( (IntegerFormat != null) && !IntegerFormat.isEmpty() ) {
    			// Process one or more integers.
    			stringProp = formatIntegers ( (TSCommandProcessor)processor, inputPropertyNames,
    				IntegerFormat, Endianness, delimiter, numBytes, OutputProperty, problems );
    		}
    		else {
    			// Not using IntegerFormat.
    			stringProp = format ( (TSCommandProcessor)processor, inputPropertyNames, Format, OutputProperty, problems );
    		}
    		// Replace "\n" in format with actual newline.
    		if ( stringProp != null ) {
    			stringProp = stringProp.replace("\\n","\n");
    		}
    		// Create an output property of the requested type.
    		Object propObject = null;
    		if ( propertyType.equalsIgnoreCase(_DateTime) ) {
    			propObject = DateTime.parse(stringProp);
    		}
    		else if ( propertyType.equalsIgnoreCase(_Double) ) {
    			propObject = new Double(stringProp);
    		}
    		else if ( propertyType.equalsIgnoreCase(_Integer) ) {
    			propObject = new Integer(stringProp);
    		}
    		else {
    			// Default.
    			propObject = stringProp;
    		}
	    	// Set the new property in the processor.
    	    PropList request_params = new PropList ( "" );
	    	request_params.setUsingObject ( "PropertyName", OutputProperty );
	    	request_params.setUsingObject ( "PropertyValue", propObject );
	    	try {
	            processor.processRequest( "SetProperty", request_params);
	    	}
	    	catch ( Exception e ) {
	    		message = "Error requesting SetProperty(Property=\"" + OutputProperty + "\") from processor.";
	    		Message.printWarning(log_level,
    				MessageUtil.formatMessageTag( command_tag, ++warning_count),
    				routine, message );
	            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
	    	}
    	}
		else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
			// Set a property that will be listed for choices.
			Object propertyObject = null;
    		if ( propertyType.equalsIgnoreCase(_DateTime) ) {
    			propertyObject = new DateTime(DateTime.DATE_CURRENT);
    		}
    		else if ( propertyType.equalsIgnoreCase(_Double) ) {
    			propertyObject = new Double(1.0);
    		}
    		else if ( propertyType.equalsIgnoreCase(_Integer) ) {
    			propertyObject = new Integer(1);
    		}
    		else {
    			propertyObject = "";
    		}
    		Prop prop = new Prop(OutputProperty, propertyObject, "");
            prop.setHowSet(Prop.SET_UNKNOWN);
    		setDiscoveryProp ( prop );
		}
    }
    catch ( Exception e ) {
        message = "Unexpected error formatting string (" + e + ").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
            message, "Check log file for details." ) );
        throw new CommandException ( message );
    }
    finally {
	    int MaxWarnings_int = 500; // Limit the problems to 500 to prevent command overload.
	    int problemsSize = problems.size();
	    int problemsSizeOutput = problemsSize;
	    String ProblemType = "FormatTableString";
	    if ( (MaxWarnings_int > 0) && (problemsSize > MaxWarnings_int) ) {
	        // Limit the warnings to the maximum.
	        problemsSizeOutput = MaxWarnings_int;
	    }
	    if ( problemsSizeOutput < problemsSize ) {
	        message = "Performing string formatting had " + problemsSize + " warnings - only " + problemsSizeOutput + " are listed.";
	        Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
	        // No recommendation since it is a user-defined check.
	        // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
	        status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
	    }
	    for ( int iprob = 0; iprob < problemsSizeOutput; iprob++ ) {
	        message = problems.get(iprob);
	        Message.printWarning ( warning_level,
	            MessageUtil.formatMessageTag(command_tag,++warning_count),routine,message );
	        // No recommendation since it is a user-defined check.
	        // FIXME SAM 2009-04-23 Need to enable using the ProblemType in the log.
	        status.addToLog ( CommandPhaseType.RUN,new CommandLogRecord(CommandStatusType.WARNING, ProblemType, message, "" ) );
	    }
    }

    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(command_tag, ++warning_count),routine,message);
        throw new CommandWarningException ( message );
    }

    status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
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
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"InputProperties",
    	"Format",
    	"IntegerFormat",
    	"Endianness",
    	"Delimiter",
    	"NumBytes",
    	"OutputProperty",
    	"PropertyType"
	};
	return this.toString(parameters, parameterOrder);
}

}