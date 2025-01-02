// CreateGeoMap_Command - this class initializes, checks, and runs the CreateGeoMap() command

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

import org.openwaterfoundation.geoprocessor.core.GeoLayer;
import org.openwaterfoundation.geoprocessor.core.GeoLayerCategorizedSymbol;
import org.openwaterfoundation.geoprocessor.core.GeoLayerFormatType;
import org.openwaterfoundation.geoprocessor.core.GeoLayerGraduatedSymbol;
import org.openwaterfoundation.geoprocessor.core.GeoLayerSingleSymbol;
import org.openwaterfoundation.geoprocessor.core.GeoLayerSymbol;
import org.openwaterfoundation.geoprocessor.core.GeoLayerType;
import org.openwaterfoundation.geoprocessor.core.GeoLayerView;
import org.openwaterfoundation.geoprocessor.core.GeoLayerViewGroup;
import org.openwaterfoundation.geoprocessor.core.GeoMap;
import org.openwaterfoundation.geoprocessor.core.GeoMapProject;

import rti.tscommandprocessor.core.TSCommandProcessor;
import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.ObjectListProvider;
import RTi.Util.IO.PropList;

/**
This class initializes, checks, and runs the CreateGeoMap() command.
*/
public class GeoMap_Command extends AbstractCommand implements Command, CommandDiscoverable, ObjectListProvider
{

	/**
	 * GeoMap that is created.
	 */
	GeoMap map = null;

	/**
	Constructor.
	*/
	public GeoMap_Command () {
		super();
		setCommandName ( "GeoMap" );
	}
	
	/**
	Check the command parameter for valid values, combination, etc.
	@param parameters The parameters for the command.
	@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
	@param warningLevel The warning level to use when printing parse warnings
	(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
	*/
	public void checkCommandParameters ( PropList parameters, String command_tag, int warningLevel )
	throws InvalidCommandParameterException {
		// General.
		String MapCommand = parameters.getValue ( "MapCommand" );
		String GeoMapProjectID = parameters.getValue ( "GeoMapProjectID" );
		// New.
		String GeoMapID = parameters.getValue ( "GeoMapID" );
		//String NewGeoMapID = parameters.getValue ( "NewGeoMapID" );
		String GeoMapName = parameters.getValue ( "GeoMapName" );
		// Layer.
		String GeoLayerCrs = parameters.getValue ( "GeoLayerCrs" );
		String GeoLayerGeometryType = parameters.getValue ( "GeoLayerGeometryType" );
		String GeoLayerLayerType = parameters.getValue ( "GeoLayerLayerType" );
		String GeoLayerSourceFormat = parameters.getValue ( "GeoLayerSourceFormat" );
		String warning = "";
	    String message;
	
	    CommandStatus status = getCommandStatus();
	    status.clearLog(CommandPhaseType.INITIALIZATION);

    	GeoMapCommandType commandType = null;
   		if ( (MapCommand == null) || MapCommand.isEmpty() ) {
    		message = "The map command must be specified.";
    		warning += "\n" + message;
    		status.addToLog ( CommandPhaseType.INITIALIZATION,
    			new CommandLogRecord(CommandStatusType.FAILURE,
    				message, "Specify the map command." ) );
   		}
   		else {
   			commandType = GeoMapCommandType.valueOfIgnoreCase(MapCommand);
   		}

    	if ( commandType == null ) {
        	message = "The map command is invalid.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
            	new CommandLogRecord(CommandStatusType.FAILURE,
                	message, "Specify a valid map command." ) );
    	}

    	// The project ID must always be specified.

   		if ( (GeoMapProjectID == null) || GeoMapProjectID.isEmpty() ) {
        	message = "The project identifier must be specified.";
        	warning += "\n" + message;
        	status.addToLog ( CommandPhaseType.INITIALIZATION,
            	new CommandLogRecord(CommandStatusType.FAILURE,
                	message, "Specify the project identifier." ) );
    	}
	
    	if ( (commandType != null) && (commandType == GeoMapCommandType.NEW_MAP) ) {
    		// Map identifier is required for a new map.
    		if ( (GeoMapProjectID == null) || GeoMapProjectID.isEmpty() ) {
	        	message = "The new map identifier must be specified.";
	        	warning += "\n" + message;
	        	status.addToLog ( CommandPhaseType.INITIALIZATION,
	            	new CommandLogRecord(CommandStatusType.FAILURE,
	                	message, "Specify the new map identifier." ) );
	    	}

    		if ( (GeoMapName == null) || GeoMapName.isEmpty() ) {
	        	message = "The new map name must be specified.";
	        	warning += "\n" + message;
	        	status.addToLog ( CommandPhaseType.INITIALIZATION,
	            	new CommandLogRecord(CommandStatusType.FAILURE,
	                	message, "Specify the new map name." ) );
	    	}
    	}

    	if ( (commandType != null) && (commandType != GeoMapCommandType.NEW_MAP) ) {
    		// Existing map identifier must be specified.
    		if ( (GeoMapID == null) || GeoMapID.isEmpty() ) {
	        	message = "The map identifier must be specified.";
	        	warning += "\n" + message;
	        	status.addToLog ( CommandPhaseType.INITIALIZATION,
	            	new CommandLogRecord(CommandStatusType.FAILURE,
	                	message, "Specify the map identifier." ) );
	    	}
    	}

    	// Check specific values in order of the tabs.
    	
    	if ( (GeoLayerCrs != null) && !GeoLayerCrs.isEmpty() && !GeoLayerCrs.equalsIgnoreCase("EPSG:4326") ) {
   			message = "The GeoLayerCrs parameter (" + GeoLayerCrs + ") is invalid.";
   			warning += "\n" + message;
   			status.addToLog ( CommandPhaseType.INITIALIZATION,
   				new CommandLogRecord(CommandStatusType.FAILURE,
   					message, "Specify the GeoLayerCrs parameter as \"Bare\" or \"Named\" (default)." ) );
    	}

    	if ( (GeoLayerGeometryType != null) && !GeoLayerGeometryType.isEmpty() &&
    		(org.openwaterfoundation.geoprocessor.core.GeoLayerGeometryType.valueOfIgnoreCase(GeoLayerGeometryType) == null) ) {
   			message = "The GeoLayerGeometryType parameter (" + GeoLayerGeometryType + ") is invalid.";
   			warning += "\n" + message;
   			status.addToLog ( CommandPhaseType.INITIALIZATION,
   				new CommandLogRecord(CommandStatusType.FAILURE,
   					message, "Select the GeoLayerGeometryType in the cimmand editor." ) );
    	}

    	if ( (GeoLayerLayerType != null) && !GeoLayerLayerType.isEmpty() && (GeoLayerType.valueOfIgnoreCase(GeoLayerLayerType) == null) ) {
   			message = "The GeoLayerLayerType parameter (" + GeoLayerLayerType + ") is invalid.";
   			warning += "\n" + message;
   			status.addToLog ( CommandPhaseType.INITIALIZATION,
   				new CommandLogRecord(CommandStatusType.FAILURE,
   					message, "Select the GeoLayerLayerType in the cimmand editor." ) );
    	}

    	if ( (GeoLayerSourceFormat != null) && !GeoLayerSourceFormat.isEmpty() && (GeoLayerFormatType.valueOfIgnoreCase(GeoLayerSourceFormat) == null) ) {
   			message = "The GeoLayerSourceFormatType parameter (" + GeoLayerSourceFormat + ") is invalid.";
   			warning += "\n" + message;
   			status.addToLog ( CommandPhaseType.INITIALIZATION,
   				new CommandLogRecord(CommandStatusType.FAILURE,
   					message, "Select the GeoLayerSourceFormatType in the cimmand editor." ) );
    	}
	
		// Check for invalid parameters.
		List<String> validList = new ArrayList<>(6);
		// General.
		validList.add ( "MapCommand" );
		validList.add ( "GeoMapProjectID" );
		validList.add ( "GeoMapID" );
		// New.
		validList.add ( "NewGeoMapID" );
		validList.add ( "GeoMapName" );
		validList.add ( "GeoMapDescription" );
	    validList.add ( "GeoMapProperties" );
		// Layer.
		validList.add ( "GeoLayerID" );
		validList.add ( "GeoLayerName" );
		validList.add ( "GeoLayerDescription" );
		validList.add ( "GeoLayerCrs" );
		validList.add ( "GeoLayerGeometryType" );
		validList.add ( "GeoLayerLayerType" );
		validList.add ( "GeoLayerProperties" );
		validList.add ( "GeoLayerSourceFormat" );
		validList.add ( "GeoLayerSourcePath" );
		// Layer View Group.
		validList.add ( "GeoLayerViewGroupID" );
		validList.add ( "GeoLayerViewGroupName" );
		validList.add ( "GeoLayerViewGroupDescription" );
		validList.add ( "GeoLayerViewGroupProperties" );
		validList.add ( "GeoLayerViewGroupInsertPosition" );
		validList.add ( "GeoLayerViewGroupInsertBefore" );
		validList.add ( "GeoLayerViewGroupInsertAfter" );
		// Layer View.
		validList.add ( "GeoLayerViewID" );
		validList.add ( "GeoLayerViewName" );
		validList.add ( "GeoLayerViewDescription" );
		validList.add ( "GeoLayerViewProperties" );
		validList.add ( "GeoLayerViewInsertPosition" );
		validList.add ( "GeoLayerViewInsertBefore" );
		validList.add ( "GeoLayerViewInsertAfter" );
		validList.add ( "GeoLayerViewLayerID" );
		// Single Symbol.
		validList.add ( "SingleSymbolID" );
		validList.add ( "SingleSymbolName" );
		validList.add ( "SingleSymbolDescription" );
		validList.add ( "SingleSymbolProperties" );
		// Categorized Symbol.
		validList.add ( "CategorizedSymbolID" );
		validList.add ( "CategorizedSymbolName" );
		validList.add ( "CategorizedSymbolDescription" );
		validList.add ( "CategorizedSymbolProperties" );
		// Graduated Symbol.
		validList.add ( "GraduatedSymbolID" );
		validList.add ( "GraduatedSymbolName" );
		validList.add ( "GraduatedSymbolDescription" );
		validList.add ( "GraduatedSymbolProperties" );

		// Parameters grouped by use in sub-commands:
		// - used in checks below

		// General parameters that can be used with any S3 command:
    	// - use this when no GeoMapProjectID is not used
		String [] generalParameters0 = {
			"MapCommand",
			"GeoMapProjectID"
		};
		// Use this when the GeoMapProjectID is used.
		String [] generalParameters1 = {
			"MapCommand",
			"GeoMapProjectID",
			"GeoMapID"
		};

		// 'New' parameters.
		String [] newParameters = {
			"NewGeoMapID",
			"GeoMapName",
			"GeoMapDescription",
			"GeoMapProperties",
		};

		// 'Layer' parameters.
		String [] layerParameters = {
			"GeoLayerID",
			"GeoLayerName",
			"GeoLayerDescription",
			"GeoLayerCrs",
			"GeoLayerGeometryType",
			"GeoLayerLayerType",
			"GeoLayerProperties",
			"GeoLayerSourceFormat",
			"GeoLayerSourcePath"
		};

		// 'Layer View Group' parameters.
		String [] layerViewGroupParameters = {
			"GeoLayerViewGroupID",
			"GeoLayerViewGroupName",
			"GeoLayerViewGroupDescription",
			"GeoLayerViewGroupProperties",
			"GeoLayerViewGroupInsertPosition",
			"GeoLayerViewGroupInsertBefore",
			"GeoLayerViewGroupInsertAfter",
		};

		// 'Layer View' parameters.
		String [] layerViewParameters = {
			"GeoLayerViewID",
			"GeoLayerViewName",
			"GeoLayerViewDescription",
			"GeoLayerViewProperties",
			"GeoLayerViewInsertPosition",
			"GeoLayerViewInsertBefore",
			"GeoLayerViewInsertAfter",
			"GeoLayerViewLayerID"
		};

		// 'Single Symbol' parameters.
		String [] singleSymbolParameters = {
			"SingleSymbolID",
			"SingleSymbolName",
			"SingleSymbolDescription",
			"SingleSymbolProperties"
		};

		// 'Categorized Symbol' parameters.
		String [] categorizedSymbolParameters = {
			"CategorizedSymbolID",
			"CategorizedSymbolName",
			"CategorizedSymbolDescription",
			"CategorizedSymbolProperties"
		};

		// 'Graduated Symbol' parameters.
		String [] graduatedSymbolParameters = {
			"GraduatedSymbolID",
			"GraduatedSymbolName",
			"GraduatedSymbolDescription",
			"GraduatedSymbolProperties"
		};

		// Checks for sub-command parameters:
		// - the above arrays of valid parameters are passed and checked
		// - some parameter lists are independent of the command
		// - others, including 'List Objects',  'Output', and 'CloudFront' are used in conjunction with a command

		// Pointers to arrays, to make the code more obvious.
		String [] generalp = null;
		String [] commandp = null;
		String [] newmapp = null;
		String [] layerp = null;
		String [] layerviewgroupp = null;
		String [] layerviewp = null;
		String [] singlesymbolp = null;
		String [] categorizedsymbolp = null;
		String [] graduatedsymbolp = null;

		if ( commandType == GeoMapCommandType.NEW_MAP ) {
			// No specific parameters.
			// General for new map used.
			// New is used.
			// All others are not used (can only create the new map).
			commandp = newParameters;
			generalp = generalParameters0;
			newmapp = newParameters;
			layerp = layerParameters;
			layerviewgroupp = layerViewGroupParameters;
			layerviewp = layerViewParameters;
			singlesymbolp = singleSymbolParameters;
			categorizedsymbolp = categorizedSymbolParameters;
			graduatedsymbolp = graduatedSymbolParameters;
		}
		else if ( commandType == GeoMapCommandType.ADD_LAYER ) {
			// Layer specific parameters.
			// General for existing map used.
			// New is not used.
			// Layer is used.
			// All others are optional to add a layer view at the same time.
			commandp = layerParameters;
			generalp = generalParameters1;
			newmapp = null;
			layerp = layerParameters;
			layerviewgroupp = layerViewGroupParameters;
			layerviewp = layerViewParameters;
			singlesymbolp = singleSymbolParameters;
			categorizedsymbolp = categorizedSymbolParameters;
			graduatedsymbolp = graduatedSymbolParameters;
		}
		else if ( commandType == GeoMapCommandType.ADD_LAYER_VIEW_GROUP ) {
			// Layer view group specific parameters.
			// General for existing map used.
			// New is not used.
			// Layer view group is used.
			// All others are optional to add a layer view at the same time.
			commandp = layerViewGroupParameters;
			generalp = generalParameters1;
			newmapp = null;
			layerp = null;
			layerviewgroupp = layerViewGroupParameters;
			layerviewp = layerViewParameters;
			singlesymbolp = singleSymbolParameters;
			categorizedsymbolp = categorizedSymbolParameters;
			graduatedsymbolp = graduatedSymbolParameters;
		}
		else if ( commandType == GeoMapCommandType.ADD_LAYER_VIEW ) {
			// Layer view group specific parameters.
			// General for existing map used.
			// New is not used.
			// Layer view group is used.
			// All others are optional to add a layer view at the same time.
			commandp = layerViewParameters;
			generalp = generalParameters1;
			newmapp = null;
			layerp = null;
			layerviewgroupp = layerViewGroupParameters;
			layerviewp = layerViewParameters;
			singlesymbolp = singleSymbolParameters;
			categorizedsymbolp = categorizedSymbolParameters;
			graduatedsymbolp = graduatedSymbolParameters;
		}

		// Do the checks in a single call.
		warning = checkSubcommandParameters ( commandType, parameters, validList, generalp, commandp, newmapp,
			layerp, layerviewgroupp, layerviewp,
			singlesymbolp, categorizedsymbolp, graduatedsymbolp,
			status, warning );

		// Also check for redundant symbol parameters.
		warning = checkSubcommandSymbolParameters ( parameters, validList,
			singlesymbolp, categorizedsymbolp, graduatedsymbolp,
			status, warning );
		
		// Basic check for valid parameters.
	    warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );
	
		if ( warning.length() > 0 ) {
			Message.printWarning ( warningLevel,
			MessageUtil.formatMessageTag(command_tag,warningLevel),warning );
			throw new InvalidCommandParameterException ( warning );
		}
	
	    status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
	}

	/**
	 * Check that a sub-command's parameters are used (but no others).
	 * @param projectCommandType project command type
	 * @param parameters list of all command parameters and values
	 * @param validList list of all valid parameters
	 * @param generalParameters general parameters used by all sub-commands
	 * @param subcommandParameters all parameters that are valid for a sub-command
	 * @param newMapParameters new map parameters that are valid for a sub-command
	 * @param layerParameters layer parameters that are valid for a sub-command
	 * @param layerViewGroupParameters layer view group parameters that are valid for a sub-command
	 * @param layerViewParameters layer view parameters that are valid for a sub-command
	 * @param singelSymbolParameters single symbol parameters that are valid for a sub-command
	 * @param categorizedSymbolParameters categorized symbol parameters that are valid for a sub-command
	 * @param graduatedSymbolParameters graduated symbol parameters that are valid for a sub-command
	 * @param status the command status to receive check messages
	 * @param warning cumulative warning string to append to
	 */
	private String checkSubcommandParameters ( GeoMapCommandType projectCommandType, PropList parameters,
		List<String> validList,
		// List in the order of the UI.
		String [] generalParameters,
		String [] subcommandParameters,
		String [] newMapParameters,
		String [] layerParameters,
		String [] layerViewGroupParameters,
		String [] layerViewParameters,
		String [] singleSymbolParameters,
		String [] categorizedSymbolParameters,
		String [] graduatedSymbolParameters,
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
				if ( (newMapParameters != null) && Arrays.stream(newMapParameters).anyMatch(param::equals) ) {
					// Check new map sub-command parameters only if used for a command.
					++validCount;
				}
				if ( (layerParameters != null) && Arrays.stream(layerParameters).anyMatch(param::equals) ) {
					// Check layer sub-command parameters only if used for a command.
					++validCount;
				}
				if ( (layerViewGroupParameters != null) && Arrays.stream(layerViewGroupParameters).anyMatch(param::equals) ) {
					// Check layer view group sub-command parameters only if used for a command.
					++validCount;
				}
				if ( (layerViewParameters != null) && Arrays.stream(layerViewParameters).anyMatch(param::equals) ) {
					// Check layer view sub-command parameters only if used for a command.
					++validCount;
				}
				if ( (singleSymbolParameters != null) && Arrays.stream(singleSymbolParameters).anyMatch(param::equals) ) {
					// Check single symbol sub-command parameters only if used for a command.
					++validCount;
				}
				if ( (categorizedSymbolParameters != null) && Arrays.stream(categorizedSymbolParameters).anyMatch(param::equals) ) {
					// Check categorized symbol sub-command parameters only if used for a command.
					++validCount;
				}
				if ( (graduatedSymbolParameters != null) && Arrays.stream(graduatedSymbolParameters).anyMatch(param::equals) ) {
					// Check graduated symbol sub-command parameters only if used for a command.
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
	 * Check that only one set of a sub-command's symbol parameters are used.
	 * @param parameters command parameters
	 * @param singelSymbolParameters single symbol parameters that are valid for a sub-command
	 * @param categorizedSymbolParameters categorized symbol parameters that are valid for a sub-command
	 * @param graduatedSymbolParameters graduated symbol parameters that are valid for a sub-command
	 * @param status the command status to receive check messages
	 * @param warning cumulative warning string to append to
	 */
	private String checkSubcommandSymbolParameters (
		PropList parameters,
		List<String> validList,
		String [] singleSymbolParameters,
		String [] categorizedSymbolParameters,
		String [] graduatedSymbolParameters,
		CommandStatus status, String warning ) {
		int singleCount = 0;
		int categorizedCount = 0;
		int graduatedCount = 0;
		for ( String param : validList ) {
			String value = parameters.getValue(param);
			if ( (value == null) || value.isEmpty() ) {
				continue;
			}
			if ( singleSymbolParameters != null ) {
				// See if the parameter matches a single symbol parameter.
				for ( String s : singleSymbolParameters ) {
					if ( s.equals(param) ) {
						++singleCount;
					}
				}
			}
			if ( categorizedSymbolParameters != null ) {
				// See if the parameter matches a categorized symbol parameter.
				for ( String s : categorizedSymbolParameters ) {
					if ( s.equals(param) ) {
						++categorizedCount;
					}
				}
			}
			if ( graduatedSymbolParameters != null ) {
				// See if the parameter matches a graduated symbol parameter.
				for ( String s : graduatedSymbolParameters ) {
					if ( s.equals(param) ) {
						++graduatedCount;
					}
				}
			}
		}
		int arrayCount = 0;
		if ( singleCount > 0 ) {
			++arrayCount;
		}
		if ( categorizedCount > 0 ) {
			++categorizedCount;
		}
		if ( graduatedCount > 0 ) {
			++graduatedCount;
		}
		if ( arrayCount > 1 ) {
			String message = "One or different symbol parameters are specified.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.WARNING,
					message, "Specify parameters for single, categorized, or graduated symbol."));
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
		return (new GeoMap_JDialog ( parent, this, tableIDChoices )).ok();
	}
	
	/**
	Return the map that is read by this class when run in discovery mode.
	@return the map that is read by this class when run in discovery mode
	*/
	private GeoMap getDiscoveryMap() {
	    return this.map;
	}
	
	/**
	Return a list of objects of the requested type.  This class only keeps a list of GeoMap objects.
	*/
	@SuppressWarnings("unchecked")
	public <T> List<T> getObjectList ( Class<T> c ) {
	    GeoMap map = getDiscoveryMap();
	    if ( (map != null) && (c == map.getClass()) ) {
	        // Map request.
	        List<T> v = new ArrayList<>();
	        v.add ( (T)map );
	        return v;
	    }
		return null;
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
	@param commandNumber number of the command in the command list
	@param commandPhase command phase
	@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
	@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
	@exception InvalidCommandParameterException Thrown if parameter one or more parameter values are invalid.
	*/
	private void runCommandInternal ( int commandNumber, CommandPhaseType commandPhase )
	throws InvalidCommandParameterException,
	CommandWarningException, CommandException {
		String routine = getClass().getSimpleName() + ".runCommandInternal", message = "";
		int warningLevel = 2;
		String command_tag = "" + commandNumber;
		int warningCount = 0;
	
	    CommandStatus status = getCommandStatus();
	    status.clearLog(commandPhase);
	    if ( commandPhase == CommandPhaseType.DISCOVERY ) {
	        setDiscoveryMap ( null );
	    }
	
		// Make sure there are time series available to operate on.
	
		PropList parameters = getCommandParameters();
		CommandProcessor processor = getCommandProcessor();
	
		boolean doNewMap = false;
		boolean doLayer = false;
		boolean doLayerViewGroup = false;
		boolean doLayerView = false;
	    boolean doSingleSymbol = false;
	    boolean doCategorizedSymbol = false;
	    boolean doGraduatedSymbol = false;

	    // General.
		String MapCommand = parameters.getValue ( "MapCommand" );
		GeoMapCommandType commandType = GeoMapCommandType.valueOfIgnoreCase(MapCommand);
		String GeoMapProjectID = parameters.getValue ( "GeoMapProjectID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoMapProjectID = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoMapProjectID);
		}
		String GeoMapID = parameters.getValue ( "GeoMapID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoMapID = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoMapID);
		}
		// New.
		String NewGeoMapID = parameters.getValue ( "NewGeoMapID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			NewGeoMapID = TSCommandProcessorUtil.expandParameterValue(processor, this, NewGeoMapID);
		}
		if ( (NewGeoMapID != null) && !NewGeoMapID.isEmpty() ) {
			doNewMap = true;
		}
		String GeoMapName = parameters.getValue ( "GeoMapName" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoMapName = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoMapName);
		}
		String GeoMapDescription = parameters.getValue ( "GeoMapDescription" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoMapDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoMapDescription);
		}
	    String GeoMapProperties = parameters.getValue ( "GeoMapProperties" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoMapProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoMapProperties);
		}
	    SortedMap<String,Object> geoMapProperties = new TreeMap<>();
	    if ( (GeoMapProperties != null) && (GeoMapProperties.length() > 0) && (GeoMapProperties.indexOf(":") > 0) ) {
	        // First break map pairs by comma.
	        List<String>pairs = StringUtil.breakStringList(GeoMapProperties, ",", 0 );
	        // Now break pairs and put in hashtable.
	        for ( String pair : pairs ) {
	            String [] parts = pair.split(":");
	            geoMapProperties.put(parts[0].trim(), parts[1].trim() );
	        }
	    }

		// Layer.
	
		String GeoLayerID = parameters.getValue ( "GeoLayerID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerID = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerID);
		}
		String GeoLayerName = parameters.getValue ( "GeoLayerName" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerName = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerName);
		}
		if ( (GeoLayerName != null) && !GeoLayerName.isEmpty() ) {
			doLayer = true;
		}
		String GeoLayerDescription = parameters.getValue ( "GeoLayerDescription" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerDescription);
		}
		String GeoLayerCrs = parameters.getValue ( "GeoLayerCrs" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerCrs = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerCrs);
		}
		String GeoLayerGeometryType = parameters.getValue ( "GeoLayerGeometryType" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerGeometryType = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerGeometryType);
		}
		String GeoLayerLayerType = parameters.getValue ( "GeoLayerLayerType" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerLayerType = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerLayerType);
		}
	    String GeoLayerProperties = parameters.getValue ( "GeoLayerProperties" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerProperties);
		}
	    SortedMap<String,Object> geoLayerProperties = new TreeMap<>();
	    if ( (GeoLayerProperties != null) && (GeoLayerProperties.length() > 0) && (GeoLayerProperties.indexOf(":") > 0) ) {
	        // First break map pairs by comma.
	        List<String>pairs = StringUtil.breakStringList(GeoLayerProperties, ",", 0 );
	        // Now break pairs and put in hashtable.
	        for ( String pair : pairs ) {
	            String [] parts = pair.split(":");
	            geoLayerProperties.put(parts[0].trim(), parts[1].trim() );
	        }
	    }
	    String GeoLayerSourceFormat = parameters.getValue ( "GeoLayerSourceFormat" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerSourceFormat = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerSourceFormat);
		}
	    String GeoLayerSourcePath = parameters.getValue ( "GeoLayerSourcePath" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerSourcePath = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerSourcePath);
		}
	    
	    // Layer View Group.
	
		String GeoLayerViewGroupID = parameters.getValue ( "GeoLayerViewGroupID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewGroupID = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewGroupID);
		}
		String GeoLayerViewGroupName = parameters.getValue ( "GeoLayerViewGroupName" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewGroupName = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewGroupName);
		}
		if ( (GeoLayerViewGroupName != null) && !GeoLayerViewGroupName.isEmpty() ) {
			doLayerViewGroup = true;
		}
		String GeoLayerViewGroupDescription = parameters.getValue ( "GeoLayerViewGroupDescription" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewGroupDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewGroupDescription);
		}
	    String GeoLayerViewGroupProperties = parameters.getValue ( "GeoLayerViewGroupProperties" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewGroupProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewGroupProperties);
		}
	    SortedMap<String,Object> geoLayerViewGroupProperties = new TreeMap<>();
	    if ( (GeoLayerViewGroupProperties != null) && (GeoLayerViewGroupProperties.length() > 0) && (GeoLayerViewGroupProperties.indexOf(":") > 0) ) {
	        // First break map pairs by comma.
	        List<String>pairs = StringUtil.breakStringList(GeoLayerViewGroupProperties, ",", 0 );
	        // Now break pairs and put in hashtable.
	        for ( String pair : pairs ) {
	            String [] parts = pair.split(":");
	            geoLayerViewGroupProperties.put(parts[0].trim(), parts[1].trim() );
	        }
	    }
		String GeoLayerViewGroupInsertPosition = parameters.getValue ( "GeoLayerViewGroupInsertPosition" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewGroupInsertPosition = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewGroupInsertPosition);
		}
		String GeoLayerViewGroupInsertBefore = parameters.getValue ( "GeoLayerViewGroupInsertBefore" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewGroupInsertBefore = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewGroupInsertBefore);
		}
		String GeoLayerViewGroupInsertAfter = parameters.getValue ( "GeoLayerViewGroupInsertAfter" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewGroupInsertAfter = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewGroupInsertAfter);
		}
	
	    // Layer View.
	
		String GeoLayerViewID = parameters.getValue ( "GeoLayerViewID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewID = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewID);
		}
		String GeoLayerViewName = parameters.getValue ( "GeoLayerViewName" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewName = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewName);
		}
		if ( (GeoLayerViewName != null) && !GeoLayerViewName.isEmpty() ) {
			doLayerView = true;
		}
		String GeoLayerViewDescription = parameters.getValue ( "GeoLayerViewDescription" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewDescription);
		}
	    String GeoLayerViewProperties = parameters.getValue ( "GeoLayerViewProperties" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewProperties);
		}
	    SortedMap<String,Object> geoLayerViewProperties = new TreeMap<>();
	    if ( (GeoLayerViewProperties != null) && (GeoLayerViewProperties.length() > 0) && (GeoLayerViewProperties.indexOf(":") > 0) ) {
	        // First break map pairs by comma.
	        List<String>pairs = StringUtil.breakStringList(GeoLayerViewProperties, ",", 0 );
	        // Now break pairs and put in hashtable.
	        for ( String pair : pairs ) {
	            String [] parts = pair.split(":");
	            geoLayerViewProperties.put(parts[0].trim(), parts[1].trim() );
	        }
	    }
		String GeoLayerViewInsertPosition = parameters.getValue ( "GeoLayerViewInsertPosition" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewInsertPosition = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewInsertPosition);
		}
		String GeoLayerViewInsertBefore = parameters.getValue ( "GeoLayerViewInsertBefore" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewInsertBefore = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewInsertBefore);
		}
		String GeoLayerViewInsertAfter = parameters.getValue ( "GeoLayerViewInsertAfter" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewInsertAfter = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewInsertAfter);
		}
		String GeoLayerViewLayerID = parameters.getValue ( "GeoLayerViewLayerID" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GeoLayerViewID = TSCommandProcessorUtil.expandParameterValue(processor, this, GeoLayerViewLayerID);
		}
		
	    // Single Symbol.
	
		String SingleSymbolName = parameters.getValue ( "SingleSymbolName" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			SingleSymbolName = TSCommandProcessorUtil.expandParameterValue(processor, this, SingleSymbolName);
		}
		String SingleSymbolDescription = parameters.getValue ( "SingleSymbolDescription" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			SingleSymbolDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, SingleSymbolDescription);
		}
	    String SingleSymbolProperties = parameters.getValue ( "SingleSymbolProperties" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			SingleSymbolProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, SingleSymbolProperties);
		}
	    SortedMap<String,Object> singleSymbolProperties = new TreeMap<>();
	    if ( (SingleSymbolProperties != null) && (SingleSymbolProperties.length() > 0) && (SingleSymbolProperties.indexOf(":") > 0) ) {
	    	doSingleSymbol = true;
	        // First break map pairs by comma.
	        List<String>pairs = StringUtil.breakStringList(SingleSymbolProperties, ",", 0 );
	        // Now break pairs and put in hashtable.
	        for ( String pair : pairs ) {
	            String [] parts = pair.split(":");
	            singleSymbolProperties.put(parts[0].trim(), parts[1].trim() );
	        }
	    }
	
	    // Categorized Symbol.
	
		String CategorizedSymbolName = parameters.getValue ( "CategorizedSymbolName" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			CategorizedSymbolName = TSCommandProcessorUtil.expandParameterValue(processor, this, CategorizedSymbolName);
		}
		String CategorizedSymbolDescription = parameters.getValue ( "CategorizedSymbolDescription" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			CategorizedSymbolDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, CategorizedSymbolDescription);
		}
	    String CategorizedSymbolProperties = parameters.getValue ( "CategorizedSymbolProperties" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			CategorizedSymbolProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, CategorizedSymbolProperties);
		}
	    SortedMap<String,Object> categorizedSymbolProperties = new TreeMap<>();
	    if ( (CategorizedSymbolProperties != null) && (CategorizedSymbolProperties.length() > 0) && (CategorizedSymbolProperties.indexOf(":") > 0) ) {
	    	doCategorizedSymbol = true;
	        // First break map pairs by comma.
	        List<String>pairs = StringUtil.breakStringList(CategorizedSymbolProperties, ",", 0 );
	        // Now break pairs and put in hashtable.
	        for ( String pair : pairs ) {
	            String [] parts = pair.split(":");
	            categorizedSymbolProperties.put(parts[0].trim(), parts[1].trim() );
	        }
	    }
	
	    // Graduated Symbol.
	
		String GraduatedSymbolName = parameters.getValue ( "GraduatedSymbolName" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GraduatedSymbolName = TSCommandProcessorUtil.expandParameterValue(processor, this, GraduatedSymbolName);
		}
		String GraduatedSymbolDescription = parameters.getValue ( "GraduatedSymbolDescription" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GraduatedSymbolDescription = TSCommandProcessorUtil.expandParameterValue(processor, this, GraduatedSymbolDescription);
		}
	    String GraduatedSymbolProperties = parameters.getValue ( "GraduatedSymbolProperties" );
		if ( commandPhase == CommandPhaseType.RUN ) {
			GraduatedSymbolProperties = TSCommandProcessorUtil.expandParameterValue(processor, this, GraduatedSymbolProperties);
		}
	    SortedMap<String,Object> graduatedSymbolProperties = new TreeMap<>();
	    if ( (GraduatedSymbolProperties != null) && (GraduatedSymbolProperties.length() > 0) && (GraduatedSymbolProperties.indexOf(":") > 0) ) {
	    	doGraduatedSymbol = true;
	        // First break map pairs by comma.
	        List<String>pairs = StringUtil.breakStringList(GraduatedSymbolProperties, ",", 0 );
	        // Now break pairs and put in hashtable.
	        for ( String pair : pairs ) {
	            String [] parts = pair.split(":");
	            graduatedSymbolProperties.put(parts[0].trim(), parts[1].trim() );
	        }
	    }
	
	    // Get the GeoViewProject for the map (from a previous GeoMap command).
	
	    GeoMapProject project = null;
		GeoMap map = null;
		GeoLayerViewGroup layerViewGroup = null;
		GeoLayerView layerView = null;
	    if ( commandPhase == CommandPhaseType.RUN ) {
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
	                    MessageUtil.formatMessageTag( command_tag, ++warningCount), routine, message );
	                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Report problem to software support." ) );
	            }
	            PropList beanPropList = bean.getResultsPropList();
	            Object oProject = beanPropList.getContents ( "GeoMapProject" );
	            if ( oProject == null ) {
	                message = "Unable to find GeoMap project to process using GeoMapProjectID=\"" + GeoMapProjectID + "\".";
	                Message.printWarning ( warningLevel,
	                MessageUtil.formatMessageTag( command_tag,++warningCount), routine, message );
	                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Verify that a map project exists with the requested ID." ) );
	            }
	            else {
	                project = (GeoMapProject)oProject;
	            }
	        }
	    }
	
		if ( warningCount > 0 ) {
			message = "There were " + warningCount + " warnings for command parameters.";
			Message.printWarning ( 2,
			MessageUtil.formatMessageTag(command_tag, ++warningCount),
			routine,message);
			throw new InvalidCommandParameterException ( message );
		}
	
		try {
			if ( doNewMap && (project != null) ) {
				// Create a new map in the project.
				if ( commandPhase == CommandPhaseType.RUN ) {
					// Create the project.
					//map = new GeoMap ( NewGeoMapID, GeoMapName, GeoMapDescription, geoMapProperties );
					map = new GeoMap ( NewGeoMapID, GeoMapName, GeoMapDescription, geoMapProperties );

					// Add the map to the project.
					project.addMap ( map );
				}
				else if ( commandPhase == CommandPhaseType.DISCOVERY ) {
					// Create an empty project and set the ID:
					// - currently the processor does not maintain a list of maps at runtime (maps are included in projects)
					map = new GeoMap(NewGeoMapID, GeoMapName, GeoMapDescription, geoMapProperties);
					setDiscoveryMap ( map );
				}
			}
			else if ( !doNewMap && (project != null) ) {
				// Try to find an existing map in the project.
				map = project.getMapForId ( GeoMapID );
				if ( map == null ) {
	                message = "Unable to find existing GeoMap to process using GeoMapID=\"" + GeoMapID + "\".";
	                Message.printWarning ( warningLevel,
	                MessageUtil.formatMessageTag( command_tag,++warningCount), routine, message );
	                status.addToLog ( CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Verify that a map project exists with the requested ID." ) );
				}
			}

			// If a layer is defined, create it so that it can be used in the layer view.
			
			GeoLayer layer = null;
			if ( doLayer && (map != null) ) {
				// Try to find the existing layer in the map.
				layer = map.getLayerForLayerId ( GeoLayerID );
			}
			if ( doLayer && (layer == null) ) {
				// Create the layer and add to the map.
				layer = new GeoLayer ( GeoLayerID, GeoLayerName, GeoLayerDescription, geoLayerProperties );
				layer.setCrs ( GeoLayerCrs );
				layer.setGeometryType ( GeoLayerGeometryType );
				layer.setLayerType ( GeoLayerLayerType );
				layer.setSourceFormat ( GeoLayerSourceFormat );
				layer.setSourcePath ( GeoLayerSourcePath );
			}
			
			// Set the layer in the map, independent of the layer view.
			if ( (map != null) && (layer != null) ) {
				map.addGeoLayer ( layer );
			}
			
			// Get the existing layer view group and layer view, needed below to search for the layer.
			
			if ( doLayerViewGroup && (map != null) ) {
				// Try to find the existing layer view group in the map.
				layerViewGroup = map.getLayerViewGroupForLayerViewGroupId ( GeoLayerViewGroupID );
			}
	
			if ( doLayerView && (layerViewGroup != null) ) {
				// Try to find the existing layer view in the layer view group.
				layerView = layerViewGroup.getLayerViewForLayerViewId ( GeoLayerViewID );
			}
	
			// If a layer symbol is defined, create it so that it can be used in the layer view.
	
			GeoLayerSymbol symbol = null;
			if ( doSingleSymbol ) {
				// Always define new.
				symbol = new GeoLayerSingleSymbol(SingleSymbolName, SingleSymbolDescription, singleSymbolProperties);
			}
			else if ( doCategorizedSymbol ) {
				// Always define new.
				symbol = new GeoLayerCategorizedSymbol(CategorizedSymbolName, CategorizedSymbolDescription, categorizedSymbolProperties);
			}
			else if ( doGraduatedSymbol ) {
				// Always define new.
				symbol = new GeoLayerGraduatedSymbol(GraduatedSymbolName, GraduatedSymbolDescription, graduatedSymbolProperties);
			}
			
			// If a layer view group is defined, process it.
			
			if ( doLayerViewGroup && (map != null) ) {
				if ( layerViewGroup == null ) {
					// LayerViewGroup was not found so create it.
					layerViewGroup = new GeoLayerViewGroup(GeoLayerViewGroupID, GeoLayerViewGroupName, GeoLayerViewGroupDescription, geoLayerViewGroupProperties);
					map.addGeoLayerViewGroup(layerViewGroup);
				}
			}
	
			// If a layer view is defined, process it.
	
			if ( doLayerView && (layerViewGroup != null) ) {
				if ( layerView == null ) {
					// No existing layer view found so create a new one.
					layerView = new GeoLayerView(GeoLayerViewGroupID, GeoLayerViewGroupName, GeoLayerViewGroupDescription, geoLayerViewGroupProperties);
					layerViewGroup.addGeoLayerView(layerView);
				}
				
				// Set the layer in the layer view.
				if ( layer != null ) {
					layerView.setGeoLayerId ( layer.getGeoLayerId() );
				}
	
				// Set the symbol in the layer view.
				if ( symbol != null ) {
					layerView.setGeoLayerSymbol ( symbol );
				}
			}
		}
		catch ( Exception e ) {
			Message.printWarning ( 3, routine, e );
			message = "Unexpected error creating the GeoMap project (" + e + ").";
			Message.printWarning ( 2, MessageUtil.formatMessageTag(command_tag, ++warningCount), routine,message );
	        status.addToLog ( commandPhase, new CommandLogRecord(CommandStatusType.FAILURE,
	            message, "Check log file for details." ) );
			throw new CommandWarningException ( message );
		}
	
		if ( warningCount > 0 ) {
			message = "There were " + warningCount + " warnings processing the command.";
			Message.printWarning ( warningLevel,
				MessageUtil.formatMessageTag(command_tag, ++warningCount),routine,message);
			throw new CommandWarningException ( message );
		}
	
	    status.refreshPhaseSeverity(commandPhase,CommandStatusType.SUCCESS);
	}
	
	/**
	Set the map that is created by this class in discovery mode (empty project with identifier and name).
	@param map the project to set
	*/
	private void setDiscoveryMap ( GeoMap map ) {
	    this.map = map;
	}
	
	/**
	Return the string representation of the command.
	@param parameters to include in the command
	@return the string representation of the command
	*/
	public String toString ( PropList parameters ) {
		String [] parameterOrder = {
			// General.
			"MapCommand",
			"GeoMapProjectID",
			"GeoMapID",
			// New.
			"NewGeoMapID",
			"GeoMapName",
			"GeoMapDescription",
			"GeoMapProperties",
			// Layer.
			"GeoLayerID",
			"GeoLayerName",
			"GeoLayerDescription",
			"GeoLayerCrs",
			"GeoLayerGeometryType",
			"GeoLayerLayerType",
			"GeoLayerProperties",
			"GeoLayerSourceFormat",
			"GeoLayerSourcePath",
			// New Layer View Group.
			"GeoLayerViewGroupID",
			"GeoLayerViewGroupName",
			"GeoLayerViewGroupDescription",
			"GeoLayerViewGroupProperties",
			"GeoLayerViewGroupInsertPosition",
			"GeoLayerViewGroupInsertBefore",
			"GeoLayerViewGroupInsertAfter",
			// New Layer View.
			"GeoLayerViewID",
			"GeoLayerViewName",
			"GeoLayerViewDescription",
			"GeoLayerViewProperties",
			"GeoLayerViewInsertPosition",
			"GeoLayerViewInsertBefore",
			"GeoLayerViewInsertAfter",
			"GeoLayerViewLayerID",
			// Single Symbol
			"SingleSymbolID",
			"SingleSymbolName",
			"SingleSymbolDescription",
			"SingleSymbolProperties",
			// Categorized Symbol
			"CategorizedSymbolID",
			"CategorizedSymbolName",
			"CategorizedSymbolDescription",
			"CategorizedSymbolProperties",
			// Graduated Symbol
			"GraduatedSymbolID",
			"GraduatedSymbolName",
			"GraduatedSymbolDescription",
			"GraduatedSymbolProperties"
		};
		return this.toString(parameters, parameterOrder);
	}

}