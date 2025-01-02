// CreateGeoMapProject_Command - this class initializes, checks, and runs the CreateGeoMapProject() command

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

import javax.swing.JFrame;

import org.openwaterfoundation.geoprocessor.core.GeoMapProject;
import org.openwaterfoundation.geoprocessor.core.GeoMapProjectResponse;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandDiscoverable;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;
import RTi.Util.JSON.JSONObject;

/**
This class initializes, checks, and runs the CreateGeoMapProject() command.
*/
public class GeoMapProject_Command extends AbstractCommand implements Command, CommandDiscoverable, FileGenerator, ObjectListProvider
{

	/**
	 * GeoMapProject that is created.
	 */
	GeoMapProject project = null;

	/**
	Output file that is created by this command.
	 */
	private File OutputFile_File = null;

	/**
	Constructor.
	*/
	public GeoMapProject_Command () {
		super();
		setCommandName ( "GeoMapProject" );
	}

	/**
	Check the command parameter for valid values, combination, etc.
	@param parameters The parameters for the command.
	@param commandTag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
	@param warningLevel The warning level to use when printing parse warnings
	(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
	*/
	public void checkCommandParameters ( PropList parameters, String commandTag, int warningLevel )
	throws InvalidCommandParameterException {
		// General.
		String ProjectCommand = parameters.getValue ( "ProjectCommand" );
		String GeoMapProjectID = parameters.getValue ( "GeoMapProjectID" );
		// New.
		String NewGeoMapProjectID = parameters.getValue ( "NewGeoMapProjectID" );
		String Name = parameters.getValue ( "Name" );
		//String Description = parameters.getValue ( "Description" );
		// Input.
		String InputFile = parameters.getValue ( "InputFile" );
		// Output.
		//String OutputFile = parameters.getValue ( "OutputFile" );
		String JsonFormat = parameters.getValue ( "JsonFormat" );

		String warning = "";
    	String message;

    	CommandStatus status = getCommandStatus();
    	status.clearLog(CommandPhaseType.INITIALIZATION);
    
    	GeoMapProjectCommandType commandType = null;
   		if ( (ProjectCommand == null) || ProjectCommand.isEmpty() ) {
    		message = "The project command must be specified.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
    			new CommandLogRecord(CommandStatusType.FAILURE,
    				message, "Specify the project command." ) );
   		}
   		else {
   			commandType = GeoMapProjectCommandType.valueOfIgnoreCase(ProjectCommand);
   		}

    	if ( commandType == null ) {
        	message = "The project command is invalid.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
            	new CommandLogRecord(CommandStatusType.FAILURE,
                	message, "Specify a valid project command." ) );
    	}

    	if ( (commandType != null) && (commandType == GeoMapProjectCommandType.COPY) ||
    		(commandType == GeoMapProjectCommandType.NEW_PROJECT) ) {
    		if ( (NewGeoMapProjectID == null) || NewGeoMapProjectID.isEmpty() ) {
    			message = "The NewGeoMapProjectID must be specified.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "Specify the NewGeoMapProjectID." ) );
    		}

    		if ( (Name == null) || Name.isEmpty() ) {
    			message = "The project name must be specified.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "Specify the project name." ) );
    		}
    	}

    	if ( (commandType != null) && (commandType == GeoMapProjectCommandType.COPY) ) {
    		if ( (GeoMapProjectID == null) || GeoMapProjectID.isEmpty() ) {
    			message = "The GeoMapProjectID must be specified.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "Specify the GeoMapProjectID." ) );
    		}
    	}

    	if ( (commandType != null) && (commandType == GeoMapProjectCommandType.READ) ) {
    		if ( (InputFile == null) || InputFile.isEmpty() ) {
    			message = "The input file must be specified.";
    			warning += "\n" + message;
    			status.addToLog ( CommandPhaseType.INITIALIZATION,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "Specify the input file." ) );
    		}
    	}
    	
    	// Check specific values in order of the tabs.
    	
    	if ( (JsonFormat != null) && !JsonFormat.isEmpty() && !JsonFormat.equalsIgnoreCase("Bare") && !JsonFormat.equalsIgnoreCase("Named") ) {
   			message = "The JsonFormat parameter (" + JsonFormat + ") is invalid.";
   			warning += "\n" + message;
   			status.addToLog ( CommandPhaseType.INITIALIZATION,
   				new CommandLogRecord(CommandStatusType.FAILURE,
   					message, "Specify the JsonFormat parameter as \"Bare\" or \"Named\" (default)." ) );
    	}

		// Check for invalid parameters.
		List<String> validList = new ArrayList<>(8);
		// General.
		validList.add ( "ProjectCommand" );
		validList.add ( "GeoMapProjectID" );
		// New.
		validList.add ( "NewGeoMapProjectID" );
		validList.add ( "Name" );
		validList.add ( "Description" );
    	validList.add ( "Properties" );
    	// Read.
    	validList.add ( "InputFile" );
    	// Output.
    	validList.add ( "OutputFile" );
    	validList.add ( "JsonFormat" );

		// Parameters grouped by use in sub-commands:
		// - used in checks below

		// General parameters that can be used with any S3 command:
    	// - use this when no GeoMapProjectID is not used
		String [] generalParameters0 = {
			"ProjectCommand"
		};
		// Use this when the GeoMapProjectID is used.
		String [] generalParameters1 = {
			"ProjectCommand",
			"GeoMapProjectID"
		};

		// 'New' parameters.
		String [] newParameters = {
			"NewGeoMapProjectID",
			"Name",
			"Description",
			"Properties",
		};

		// 'Copy' parameters:
		// - uses the general and new parameters

		// 'Read' parameters.
		String [] readParameters = {
			"InputFile"
		};

		// Write parameters:
		// - can be used at any time and specifically for the write command
		String [] writeParameters = {
			"OutputFile",
			"JsonFormat"
		};
	
		// Checks for sub-command parameters:
		// - the above arrays of valid parameters are passed and checked

		// Pointers to arrays, to make the code more obvious.
		String [] generalp = null;
		String [] commandp = null;
		String [] newp = null;
		String [] readp = null;
		String [] writep = null;
		if ( commandType == GeoMapProjectCommandType.COPY ) {
			// No specific parameters.
			// General is used (source of copy).
			// New is used (destination of copy).
			// Output is used (optional write the copy).
			commandp = null;
			generalp = generalParameters1;
			newp = newParameters;
			readp = null;
			writep = writeParameters;
		}
		else if ( commandType == GeoMapProjectCommandType.DELETE ) {
			// No specific parameters.
			// General is used (project to delete).
			// New is not used.
			// Output is not used.
			commandp = null;
			generalp = generalParameters1;
			newp = null;
			readp = null;
			writep = null;
		}
		else if ( commandType == GeoMapProjectCommandType.NEW_PROJECT ) {
			// No specific parameters.
			// General is not used.
			// New is not used (is the specific command parameters).
			// Read is not used.
			// Output is optional.
			commandp = newParameters;
			generalp = generalParameters0;
			newp = newParameters;
			readp = null;
			writep = writeParameters;
		}
		else if ( commandType == GeoMapProjectCommandType.READ ) {
			// General is not used (since all are read).
			// New is not used.
			// Read is not used (since the specific parameters).
			// Output is optional.
			commandp = readParameters;
			generalp = generalParameters0;
			newp = newParameters;
			readp = readParameters;
			writep = writeParameters;
		}
		else if ( commandType == GeoMapProjectCommandType.WRITE ) {
			// General is not used (since all are read).
			// New is not used.
			// Read could be used.
			// Output is used.
			// Other parameters are not used.
			commandp = writeParameters;
			generalp = generalParameters1;
			newp = null;
			readp = readParameters;
			writep = writeParameters;
		}

		// Do the checks in a single call.
		warning = checkSubcommandParameters ( commandType, parameters, validList, generalp, commandp, newp, readp, writep, status, warning );
		
		// Basic check for valid parameters.
    	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

		if ( warning.length() > 0 ) {
			Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(commandTag,warningLevel),warning );
			throw new InvalidCommandParameterException ( warning );
		}

    	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
	}

	/**
	 * Check that a sub-command's parameters are used (but no others).
	 * @param projectCommandType project command type
	 * @param parameters list of all command parameters and values
	 * @param validLIst list of all valid parameters
	 * @param generalParameters general parameters used by all sub-commands
	 * @param subcommandParameters all parameters that are valid for a sub-command
	 * @param newParameters New parameters that are valid for a sub-command
	 * @param readParameters Read parameters that are valid for a sub-command
	 * @param writeParameters output parameters that are valid for a sub-command
	 * @param status the command status to receive check messages
	 * @param warning cumulative warning string to append to
	 */
	private String checkSubcommandParameters ( GeoMapProjectCommandType projectCommandType, PropList parameters,
		List<String> validList,
		// List in the order of the UI.
		String [] generalParameters,
		String [] subcommandParameters,
		String [] newParameters,
		String [] readParameters,
		String [] writeParameters,
		CommandStatus status, String warning ) {
		// Loop through the list of all valid parameters:
		// - get the value for a parameter
		// - if a value is specified, check that the parameter name was expected
		for ( String param : validList ) {
			String value = parameters.getValue(param);
			// Count of when a parameter matches an array of expected parameters:
			// - a parameter must match one of the smaller arrays for it to be valid
			int validCount = 0;
			if ( (value != null) && !value.isEmpty() ) {
				if ( (generalParameters != null) && Arrays.stream(generalParameters).anyMatch(param::equals) ) {
					// Always check general parameters (should never be an issue).
					++validCount;
				}
				if ( (subcommandParameters != null) && Arrays.stream(subcommandParameters).anyMatch(param::equals) ) {
					// Always check sub-command parameters.
					++validCount;
				}
				if ( (newParameters != null) && Arrays.stream(newParameters).anyMatch(param::equals) ) {
					// Check new project sub-command parameters only if used for a command.
					++validCount;
				}
				if ( (readParameters != null) && Arrays.stream(readParameters).anyMatch(param::equals) ) {
					// Check read sub-command parameters only if used for a command.
					++validCount;
				}
				if ( (writeParameters != null) && Arrays.stream(writeParameters).anyMatch(param::equals) ) {
					// Check write sub-command parameters only if used for a command.
					++validCount;
				}

				if ( validCount == 0 ) {
					// No valid parameters were matched.
					String message = "The " + param + " parameter is not used with the " + projectCommandType + " project command.";
					warning += "\n" + message;
					status.addToLog(CommandPhaseType.INITIALIZATION,
						new CommandLogRecord(CommandStatusType.WARNING,
							message, "Don't set the " + param + " parameter."));
				}
			}
		}
		return warning;
	}

	/**
	Edit the command.
	@param parent The parent JFrame to which the command dialog will belong.
	@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed).
	*/
	public boolean editCommand ( JFrame parent ) {
		List<String> tableIDChoices =
        	TSCommandProcessorUtil.getTableIdentifiersFromCommandsBeforeCommand(
            	(TSCommandProcessor)getCommandProcessor(), this);
    	// The command will be modified if changed.
		return (new GeoMapProject_JDialog ( parent, this, tableIDChoices )).ok();
	}

	/**
	Return the project that is read by this class when run in discovery mode.
	*/
	private GeoMapProject getDiscoveryProject() {
    	return this.project;
	}

	/**
	Return the list of files that were created by this command.
	@return the list of files that were created by this command
	*/
	public List<File> getGeneratedFileList () {
		List<File> list = new ArrayList<>();
		if ( getOutputFile() != null ) {
			list.add ( getOutputFile() );
		}
		return list;
	}

	/**
	Return a list of objects of the requested type.  This class only keeps a list of DataTable objects.
	*/
	@SuppressWarnings("unchecked")
	public <T> List<T> getObjectList ( Class<T> c ) {
    	GeoMapProject project = getDiscoveryProject();
    	if ( (project != null) && (c == project.getClass()) ) {
        	// GeoMapProject request.
        	List<T> v = new ArrayList<>();
        	v.add ( (T)project );
        	return v;
    	}
		return null;
	}

	/**
	Return the output file generated by this file.  This method is used internally.
	*/
	private File getOutputFile () {
		return this.OutputFile_File;
	}

	// Use base class parseCommand()

	/**
	Run the command.
	@param commandNumber Command number in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	*/
	public void runCommand ( int commandNumber )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
    	runCommandInternal ( commandNumber, CommandPhaseType.RUN );
	}
	
	/**
	Run the command in discovery mode.
	@param commandNumber Command number in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	*/
	public void runCommandDiscovery ( int commandNumber )
	throws InvalidCommandParameterException, CommandWarningException, CommandException {
    	runCommandInternal ( commandNumber, CommandPhaseType.DISCOVERY );
	}

	/**
	Run the command.
	@param commandNumber Number of command in sequence.
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
	*/
	private void runCommandInternal ( int commandNumber, CommandPhaseType commandPhase )
	throws InvalidCommandParameterException,
	CommandWarningException, CommandException {
		String routine = getClass().getSimpleName() + ".runCommandInternal", message = "";
		int warningLevel = 2;
		String commandTag = "" + commandNumber;
		int warningCount = 0;

    	CommandStatus status = getCommandStatus();
    	status.clearLog(commandPhase);
    	if ( commandPhase == CommandPhaseType.DISCOVERY ) {
        	setDiscoveryProject ( null );
    	}

		PropList parameters = getCommandParameters();
		CommandProcessor processor = getCommandProcessor();

		// General.
		String ProjectCommand = parameters.getValue ( "ProjectCommand" );
		GeoMapProjectCommandType commandType = GeoMapProjectCommandType.valueOfIgnoreCase(ProjectCommand);

		String GeoMapProjectID = parameters.getValue ( "GeoMapProjectID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoMapProjectID = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoMapProjectID);
		}
		String GeoMapID = parameters.getValue ( "GeoMapID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoMapID = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoMapID);
		}

		// New.
		String NewGeoMapProjectID = parameters.getValue ( "NewGeoMapProjectID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			NewGeoMapProjectID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewGeoMapProjectID);
		}
		String Name = parameters.getValue ( "Name" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			Name = TSCommandProcessorUtil.expandParameterValue(processor, this, Name);
		}
		String Description = parameters.getValue ( "Description" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			Description = TSCommandProcessorUtil.expandParameterValue(processor, this, Description);
		}
    	String Properties = parameters.getValue ( "Properties" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			Properties = TSCommandProcessorUtil.expandParameterValue(processor, this, Properties);
		}
    	SortedMap<String,Object> properties = new TreeMap<>();
    	if ( (Properties != null) && (Properties.length() > 0) && (Properties.indexOf(":") > 0) ) {
        	// First break map pairs by comma.
        	List<String>pairs = StringUtil.breakStringList(Properties, ",", 0 );
        	// Now break pairs and put in hashtable.
        	for ( String pair : pairs ) {
            	String [] parts = pair.split(":");
            	properties.put(parts[0].trim(), parts[1].trim() );
        	}
    	}

    	// Read.
		String InputFile = parameters.getValue ( "InputFile" ); // Expanded below.

    	// Output.
		String OutputFile = parameters.getValue ( "OutputFile" ); // Expanded below.
		String JsonFormat = parameters.getValue ( "JsonFormat" );

		if ( warningCount > 0 ) {
			message = "There were " + warningCount + " warnings for command parameters.";
			Message.printWarning ( 2,
			MessageUtil.formatMessageTag(commandTag, ++warningCount),
			routine,message);
			throw new InvalidCommandParameterException ( message );
		}

		// Process the project.
		
		try {
			GeoMapProject project = null;
       		if ( commandPhase == CommandPhaseType.RUN ) {
       			if ( (commandType == GeoMapProjectCommandType.COPY) ||
       				(commandType == GeoMapProjectCommandType.WRITE) ) {
       				// Get the existing project for commands that need it.
       				PropList requestParams = null;
	        		CommandProcessorRequestResultsBean bean = null;
	        		if ( (GeoMapProjectID != null) && !GeoMapProjectID.equals("") ) {
	            		// Get the project for the map.
	            		requestParams = new PropList ( "" );
	            		requestParams.set ( "GeoMapProjectID", GeoMapProjectID );
	            		try {
	                		bean = processor.processRequest( "GetGeoMapProject", requestParams);
	            		}
	            		catch ( Exception e ) {
	                		message = "Error requesting GetGeoMapProject(GeoMapProjectID=\"" + GeoMapProjectID + "\") from processor.";
	                		Message.printWarning(warningLevel,
	                    		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	                		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                    		message, "Report problem to software support." ) );
	            		}
	            		PropList beanPropList = bean.getResultsPropList();
	            		Object oProject = beanPropList.getContents ( "GeoMapProject" );
	            		if ( oProject == null ) {
	                		message = "Unable to find GeoMap project to process using GeoMapProjectID=\"" + GeoMapProjectID + "\".";
	                		Message.printWarning ( warningLevel,
	                		MessageUtil.formatMessageTag( commandTag,++warningCount), routine, message );
	                		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                    		message, "Verify that a map project exists with the requested ID." ) );
	            		}
	            		else {
	                		project = (GeoMapProject)oProject;
	            		}
	        		}
       			}

       			// Process specific commands.
       			if ( commandType == GeoMapProjectCommandType.NEW_PROJECT ) {
       				// Create the project.
       				Message.printStatus(2, routine, "Creating new GeoMap project ID=\"" + NewGeoMapProjectID + "\".");
        			project = new GeoMapProject ( NewGeoMapProjectID, Name, Description, properties );
       			}
       			else if ( commandType == GeoMapProjectCommandType.COPY ) {
       				// Copy the project.
        			project = (GeoMapProject)project.clone();
       				// Override the copied project data with that specified.
       				if ( (NewGeoMapProjectID != null) && !NewGeoMapProjectID.isEmpty() ) {
       					project.setGeoMapProjectId(NewGeoMapProjectID);
       				}
       				if ( (Name != null) && !Name.isEmpty() ) {
       					project.setName(Name);
       				}
       				if ( (Description != null) && !Description.isEmpty() ) {
       					project.setDescription(Description);
       				}
       			}
       			else if ( commandType == GeoMapProjectCommandType.DELETE ) {
       				// Delete the existing project.
       				PropList requestParams = null;
	        		CommandProcessorRequestResultsBean bean = null;
	        		if ( (GeoMapProjectID != null) && !GeoMapProjectID.equals("") ) {
	            		// Remove the map project.
	            		requestParams = new PropList ( "" );
	            		requestParams.set ( "GeoMapProjectID", GeoMapProjectID );
	            		try {
	                		bean = processor.processRequest( "RemoveGeoMapProjectFromResultsList", requestParams);
	            		}
	            		catch ( Exception e ) {
	                		message = "Error requesting RemoveGeoMapProjectFromResultsList(GeoMapProjectID=\"" + GeoMapProjectID + "\") from processor.";
	                		Message.printWarning(warningLevel,
	                    		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	                		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
	                    		message, "Report problem to software support." ) );
	            		}
	            		/* TODO smalers 2024-2-31 add a check to see if removed?  Also allow ignoring if not matched?
	            		PropList beanPropList = bean.getResultsPropList();
	            		Object oProject = beanPropList.getContents ( "GeoMapProject" );
	            		if ( oProject == null ) {
	                		message = "Unable to remove GeoMap project using GeoMapProjectID=\"" + GeoMapProjectID + "\".";
	                		Message.printWarning ( warningLevel,
	                		MessageUtil.formatMessageTag( commandTag,++warningCount), routine, message );
	                		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
	                    		message, "Verify that a map project exists with the requested ID." ) );
	            		}
	            		else {
	                		project = (GeoMapProject)oProject;
	            		}
	            		*/
	            		
	            		// Also remove the object.
	            		requestParams = new PropList ( "" );
	            		requestParams.set ( "ObjectID", GeoMapProjectID );
	            		try {
	                		bean = processor.processRequest( "RemoveObjectFromResultsList", requestParams);
	            		}
	            		catch ( Exception e ) {
	                		message = "Error requesting RemoveObjectFromResultsList(ObjectID=\"" + GeoMapProjectID + "\") from processor.";
	                		Message.printWarning(warningLevel,
	                    		MessageUtil.formatMessageTag( commandTag, ++warningCount), routine, message );
	                		status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
	                    		message, "Report problem to software support." ) );
	            		}
	        		}
       			}
       			else if ( commandType == GeoMapProjectCommandType.READ ) {
       				// Create the object from JSON file.
       				if ( commandPhase == CommandPhaseType.RUN ) {
       					String InputFile_full = InputFile;
        				InputFile_full = IOUtil.verifyPathForOS(
           					IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
               					TSCommandProcessorUtil.expandParameterValue(processor,this,InputFile)));
        				ObjectMapper mapper = new ObjectMapper();
        				// TODO smalers 2017-05-14 figure out if the following can be implemented:
        				// - for now using a custom deserializer on the Station.lastTimePolled
        				mapper.registerModule(new JavaTimeModule());
        				//mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        				Message.printStatus(2, routine, "Creating map project by deserializing the JSON file.");
        				// Read the NovaStar time series, which maps the JSON to Java object.
        				GeoMapProjectResponse response = mapper.readValue(new File(InputFile_full), GeoMapProjectResponse.class);
        				project = response.getGeoMapProject();
        				// Override the read data with that specified.
        				if ( (NewGeoMapProjectID != null) && !NewGeoMapProjectID.isEmpty() ) {
        					project.setGeoMapProjectId(NewGeoMapProjectID);
        				}
        				if ( (Name != null) && !Name.isEmpty() ) {
        					project.setName(Name);
        				}
        				if ( (Description != null) && !Description.isEmpty() ) {
        					project.setDescription(Description);
        				}
       				}
       			}
       			else if ( commandType == GeoMapProjectCommandType.WRITE ) {
       				// Write the project:
       				// - the project is requested from the processor above or is created from new, copy, or read
       			}

       			if ( (project != null) && (OutputFile != null) && !OutputFile.isEmpty() ) {
       				// Output the project to a JSON file.
       				String OutputFile_full = OutputFile;
       				// Convert to an absolute path.
		    		OutputFile_full = IOUtil.verifyPathForOS(
            			IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
               				TSCommandProcessorUtil.expandParameterValue(processor, this, OutputFile) ) );
       				Message.printStatus(2, routine, "Writing GeoMap project to \"" + OutputFile_full + "\".");

		    		// Probably always do pretty print?
		    		boolean prettyPrint = true;
		    		if ( prettyPrint ) {
		    			// Write using pretty printing with indent.
		    			//mapper.writerWithDefaultPrettyPrinter().writeValue(Paths.get(OutputFile_full).toFile(), object);
		    			ObjectMapper mapper = new ObjectMapper();
		    			mapper.registerModule(new JavaTimeModule());
		    			mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		    			mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
		    			// Configure pretty printing:
		    			// - default is 2-space indentation
		    			mapper.configure(SerializationFeature.INDENT_OUTPUT,prettyPrint);
		    			//int indent = 2;
		    			//String indentSpaces = "";
		    			//for ( int i = 0; i < indent; i++ ) {
		    			//	indentSpaces += " ";
		    			//}
		    			// Configure so that arrays using newlines so not one long line:
		    			// - see:  https://stackoverflow.com/questions/14938667/jackson-json-deserialization-array-elements-in-each-line
		    			DefaultPrettyPrinter prettyPrinter = new DefaultPrettyPrinter();
		    			prettyPrinter.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);

		    			//DefaultPrettyPrinter printer = new DefaultPrettyPrinter().withObjectIndenter(
		    			//	new DefaultIndenter(indentSpaces, "\n"));
		    			if ( (JsonFormat == null) || ((JsonFormat != null) && JsonFormat.equalsIgnoreCase("Named")) ) {
		    				// JSON has "geoMapProject" as top-level element.
		    				HashMap<String,GeoMapProject> hashMap = new HashMap<>();
		    				hashMap.put("geoMapProject", project);
		    				mapper.writer(prettyPrinter).writeValue(Paths.get(OutputFile_full).toFile(), hashMap);
		    			}
		    			else {
		    				// Put the object parts in a map for output.
		    				// Output the bare object:
		    				// - no single top-level object
		    				// - output the project's data members
		    				mapper.writer(prettyPrinter).writeValue(Paths.get(OutputFile_full).toFile(), project);
		    			}
		    		}
		    		else {
		    			// Write with default formatting (not pretty).
		    			ObjectMapper mapper = new ObjectMapper();
		    			mapper.registerModule(new JavaTimeModule());
		    			mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		    			mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
		    			if ( (JsonFormat == null) || ((JsonFormat != null) && JsonFormat.equalsIgnoreCase("Named")) ) {
		    				// JSON has "geoMapProject" as top-level element.
		    				mapper.writeValue(Paths.get(OutputFile_full).toFile(), project);
		    			}
		    		}
		    		// Save the output file name.
		    		setOutputFile ( new File(OutputFile_full));
       			}

       			if ( (project != null) &&
       				((commandType == GeoMapProjectCommandType.NEW_PROJECT) ||
       				(commandType == GeoMapProjectCommandType.COPY)|| 
       				(commandType == GeoMapProjectCommandType.READ)) ) {

       				// Add the new project instance to the processor so that it can be retrieved for other commands.

       				PropList requestParams = new PropList ( "" );
       				requestParams.setUsingObject ( "GeoMapProject", project );
       				try {
       					processor.processRequest( "SetGeoMapProject", requestParams);
       				}
       				catch ( Exception e ) {
       					message = "Error processing request SetGeoMapProject.";
       					Message.printWarning(warningLevel,
       						MessageUtil.formatMessageTag( commandTag, ++warningCount),
       						routine, message );
       					status.addToLog ( commandPhase,
       						new CommandLogRecord(CommandStatusType.FAILURE,
       							message, "Report problem to software support." ) );
       				}

       				// Set the project as an object in the processor so that it can be viewed as JSON:
       				// - use an object name "geoMapProject" for the object instance

       				requestParams = new PropList ( "" );
       				JSONObject projectObject = new JSONObject(NewGeoMapProjectID);
       				Map<String,Object> objectMap = new TreeMap<>();
       				objectMap.put("geoMapProject", project);
       				projectObject.setObjectMap(objectMap);
       				requestParams.setUsingObject ( "Object", projectObject );
       				try {
       					processor.processRequest( "SetObject", requestParams);
       				}
       				catch ( Exception e ) {
       					message = "Error processing request SetObject(Object=...).";
       					Message.printWarning(warningLevel,
       						MessageUtil.formatMessageTag( commandTag, ++warningCount),
       						routine, message );
       					Message.printWarning(3, routine, e);
       					status.addToLog ( commandPhase,
       							new CommandLogRecord(CommandStatusType.FAILURE,
       							message, "Report problem to software support." ) );
       				}
            	}
        	}
       		else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
            	// Create an empty project and set the ID.
       			if ( (commandType == GeoMapProjectCommandType.NEW_PROJECT) || (commandType == GeoMapProjectCommandType.COPY) ) {
       				// Use the new map identifier.
       				if ( (NewGeoMapProjectID != null) && !NewGeoMapProjectID.equals("") ) {
       					project = new GeoMapProject(NewGeoMapProjectID, Name, Description, properties);
       					setDiscoveryProject ( project );
       				}
            	}
       			else if ( commandType == GeoMapProjectCommandType.READ ) {
       				// Use the existing identifier.
       				if ( (GeoMapProjectID != null) && !GeoMapProjectID.equals("") ) {
       					project = new GeoMapProject(GeoMapProjectID, Name, Description, properties);
       					setDiscoveryProject ( project );
       				}
       			}
        	}
		}
		catch ( Exception e ) {
			Message.printWarning ( 3, routine, e );
			message = "Unexpected error creating the GeoMap project (" + e + ").";
			Message.printWarning ( 2, MessageUtil.formatMessageTag(commandTag, ++warningCount), routine,message );
        	status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
            	message, "Check log file for details." ) );
			throw new CommandWarningException ( message );
		}

		if ( warningCount > 0 ) {
			message = "There were " + warningCount + " warnings processing the command.";
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(commandTag, ++warningCount),routine,message);
			throw new CommandWarningException ( message );
		}

    	status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
	}

	/**
	Set the project that is created by this class in discovery mode (empty project with identifier and name).
	@param project the project to set
	*/
	private void setDiscoveryProject ( GeoMapProject project ) {
    	this.project = project;
	}

	/**
	Set the output file that is created by this command.  This is only used internally.
	@param file the output file that is created by this command
	*/
	private void setOutputFile ( File file ) {
		this.OutputFile_File = file;
	}

	/**
	Return the string representation of the command.
	@param parameters to include in the command
	@return the string representation of the command
	*/
	public String toString ( PropList parameters ) {
		String [] parameterOrder = {
			// General.
			"ProjectCommand",
			"GeoMapProjectID",
			// New.
			"NewGeoMapProjectID",
			"Name",
			"Description",
			"Properties",
			// Read.
			"InputFile",
			// Output.
			"OutputFile",
			"JsonFormat"
		};
		return this.toString(parameters, parameterOrder);
	}

}