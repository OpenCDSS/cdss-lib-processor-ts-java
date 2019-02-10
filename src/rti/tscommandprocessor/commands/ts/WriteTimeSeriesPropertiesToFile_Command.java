// WriteTimeSeriesPropertiesToFile_Command - This class initializes, checks, and runs the WriteTimeSeriesPropertiesToFile() command.

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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import rti.tscommandprocessor.core.TSListType;
import RTi.TS.TS;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandProcessorRequestResultsBean;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.FileWriteModeType;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.IO.PropertyFileFormatType;

/**
This class initializes, checks, and runs the WriteTimeSeriesPropertiesToFile() command.
*/
public class WriteTimeSeriesPropertiesToFile_Command extends AbstractCommand implements Command, FileGenerator
{
	
/**
Values for SortOrder property.
*/
protected final String _Ascending = "Ascending";
protected final String _Descending = "Descending";
protected final String _None = "None";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WriteTimeSeriesPropertiesToFile_Command ()
{	super();
	setCommandName ( "WriteTimeSeriesPropertiesToFile" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String OutputFile = parameters.getValue ( "OutputFile" );
	String IncludeProperties = parameters.getValue ( "IncludeProperties" );
	String WriteMode = parameters.getValue ( "WriteMode" );
	String FileFormat = parameters.getValue ( "FileFormat" );
	String SortOrder = parameters.getValue ( "SortOrder" );
	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	if ( (OutputFile == null) || (OutputFile.length() == 0) ) {
		message = "The output file must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify an output file." ) );
	}
	/* Don't check because ${} properties may be used
	 * TODO SAM 2008-07-31 Need to enable check in some form.
	else {
	    String working_dir = null;
		try {
		    Object o = processor.getPropContents ( "WorkingDir" );
			if ( o != null ) {
				working_dir = (String)o;
			}
		}
		catch ( Exception e ) {
			message = "Error requesting WorkingDir from processor.";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Software error - report problem to support." ) );
		}
		// Expand for ${} properties...
		working_dir = TSCommandProcessorUtil.expandParameterValue(processor, this, working_dir);
		try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, OutputFile));
			File f = new File ( adjusted_path );
			File f2 = new File ( f.getParent() );
			if ( !f2.exists() ) {
				message = "The output parent directory does " +
				"not exist for the output file: \"" + adjusted_path + "\".";
				warning += "\n" + message;
				status.addToLog ( CommandPhaseType.INITIALIZATION,
					new CommandLogRecord(CommandStatusType.FAILURE,
							message, "Create the output directory." ) );
			}
			f = null;
			f2 = null;
		}
		catch ( Exception e ) {
			message = "The output file:\n" +
			"    \"" + OutputFile +
			"\"\ncannot be adjusted to an absolute path using the working directory:\n" +
			"    \"" + working_dir + "\".";
			warning += "\n" + message;
			status.addToLog ( CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Verify that output file and working directory paths are compatible." ) );
		}
	}
	*/
		
	if ( (IncludeProperties == null) || (IncludeProperties.length() == 0) ) {
		message = "One or more property names must be specified.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify a property name." ) );
	}
	
	if ( (WriteMode != null) && (WriteMode.length() != 0) &&
	    !WriteMode.equalsIgnoreCase("" + FileWriteModeType.APPEND) &&
	    !WriteMode.equalsIgnoreCase("" + FileWriteModeType.OVERWRITE)) {
		message="WriteMode (" + WriteMode + ") is not a valid value.";
		warning += "\n" + message;
		status.addToLog ( CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify as " + FileWriteModeType.APPEND + " or " +
				FileWriteModeType.OVERWRITE + " (default)." ) );
	}
	
    if ( (FileFormat != null) && (FileFormat.length() != 0) &&
        !FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_VALUE) &&
        !FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_TYPE_VALUE) &&
        !FileFormat.equalsIgnoreCase("" + PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON)) {
        message="FileFormat (" + FileFormat + ") is not a valid value.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify as " + PropertyFileFormatType.NAME_VALUE + ", " +
                PropertyFileFormatType.NAME_TYPE_VALUE + " (default), " +
                PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON + "." ) );
    }

    if ( (SortOrder != null) && !SortOrder.equals("") && !SortOrder.equals(_Ascending) && !SortOrder.equals(_Descending) && !SortOrder.equals(_None) ) {
        message = "The sort order is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
            new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify the sort order as " + _Ascending + ", " + _Descending + ", or " +_None + " (default)." ) );
    }
	
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(8);
	validList.add ( "TSList" );
	validList.add ( "TSID" );
	validList.add ( "EnsembleID" );
	validList.add ( "OutputFile" );
	validList.add ( "IncludeProperties" );
	validList.add ( "WriteMode" );
	validList.add ( "FileFormat" );
	validList.add ( "SortOrder" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new WriteTimeSeriesPropertiesToFile_JDialog ( parent, this )).ok();
}

/**
Return the list of supported FileFormat parameter choices.
@return the list of supported FileFormat parameter choices.
*/
protected List<PropertyFileFormatType> getFileFormatChoices ()
{
    List<PropertyFileFormatType> fileFormatChoices = new ArrayList<PropertyFileFormatType>(3);
    fileFormatChoices.add ( PropertyFileFormatType.NAME_TYPE_VALUE );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON );
    fileFormatChoices.add ( PropertyFileFormatType.NAME_VALUE );
    return fileFormatChoices;
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList ()
{
	List<File> list = new Vector<File>();
	if ( getOutputFile() != null ) {
		list.add ( getOutputFile() );
	}
	return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile ()
{
	return __OutputFile_File;
}

/**
Return the list of supported WriteMode parameter choices.
@return the list of supported WriteMode parameter choices.
*/
protected List<FileWriteModeType> getWriteModeChoices ()
{
    List<FileWriteModeType> writeModeChoices = new ArrayList<FileWriteModeType>(2);
    writeModeChoices.add ( FileWriteModeType.APPEND );
    writeModeChoices.add ( FileWriteModeType.OVERWRITE );
    // TODO SAM 2012-07-27 Need to update
    //writeModeChoices.add ( FileWriteModeType.UPDATE );
    return writeModeChoices;
}

// parseCommand is base class

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	int log_level = 3;  // Level for non-use messages for log file.
	
	// Clear the output file
	
	setOutputFile ( null );
	
	// Check whether the processor wants output files to be created...

	CommandProcessor processor = getCommandProcessor();
	if ( !TSCommandProcessorUtil.getCreateOutput(processor) ) {
			Message.printStatus ( 2, routine,
			"Skipping \"" + toString() + "\" because output is not being created." );
	}

	PropList parameters = getCommandParameters();
    String TSList = parameters.getValue ( "TSList" );
    if ( (TSList == null) || TSList.equals("") ) {
        TSList = TSListType.ALL_TS.toString();
    }
    String TSID = parameters.getValue ( "TSID" );
    String EnsembleID = parameters.getValue ( "EnsembleID" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String IncludeProperties = parameters.getValue ( "IncludeProperties" );
	String [] includeProperties = new String[0];
	if ( (IncludeProperties != null) && !IncludeProperties.equals("") ) {
	    if ( IncludeProperties.indexOf(",") > 0 ) {
	        includeProperties = IncludeProperties.split(",");
	    }
	    else {
	        includeProperties = new String[1];
	        includeProperties[0] = IncludeProperties.trim();
	    }
        // Also convert glob-style wildcard * to internal Java wildcard
	    if ( IncludeProperties.indexOf('*') >= 0 ) {
	    	for ( int i = 0; i < includeProperties.length; i++ ) {
	    		includeProperties[i] = includeProperties[i].replace("*", ".*");
	    	}
	    }
	}
	String WriteMode = parameters.getValue ( "WriteMode" );
    FileWriteModeType writeMode = FileWriteModeType.valueOfIgnoreCase(WriteMode);
    if ( writeMode == null ) {
        writeMode = FileWriteModeType.OVERWRITE; // Default
    }
    String FileFormat = parameters.getValue ( "FileFormat" );
    PropertyFileFormatType fileFormat = PropertyFileFormatType.valueOfIgnoreCase(FileFormat);
    if ( fileFormat == null ) {
        fileFormat = PropertyFileFormatType.NAME_TYPE_VALUE; // Default
    }
    String SortOrder = parameters.getValue ( "SortOrder" );
    int sortOrder = 0; // No sort
    if ( SortOrder != null ) {
    	if ( SortOrder.equalsIgnoreCase(_Descending) ) {
    		sortOrder = -1;
    	}
    	else if ( SortOrder.equalsIgnoreCase(_Ascending) ) {
    		sortOrder = 1;
    	}
    }
	
	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.RUN);

	// Get the time series to process
	
    PropList request_params = new PropList ( "" );
    request_params.set ( "TSList", TSList );
    request_params.set ( "TSID", TSID );
    request_params.set ( "EnsembleID", EnsembleID );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeriesToProcess", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\", TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    if ( bean == null ) {
        Message.printStatus ( 2, routine, "Bean is null.");
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TSList = bean_PropList.getContents ( "TSToProcessList" );
    List<TS> tslist = null;
    if ( o_TSList == null ) {
        message = "Null TSToProcessList returned from processor for GetTimeSeriesToProcess(TSList=\"" + TSList +
        "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
        Message.printWarning ( log_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    }
    else {
    	@SuppressWarnings("unchecked")
		List<TS> tslist0 = (List<TS>)o_TSList;
        tslist = tslist0;
        if ( tslist.size() == 0 ) {
            message = "No time series are available from processor GetTimeSeriesToProcess (TSList=\"" + TSList +
            "\" TSID=\"" + TSID + "\", EnsembleID=\"" + EnsembleID + "\").";
            Message.printWarning ( log_level,
                    MessageUtil.formatMessageTag(
                            command_tag,++warning_count), routine, message );
            status.addToLog ( CommandPhaseType.RUN,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message,
                            "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
        }
    }
    
    int nts = tslist.size();
    if ( nts == 0 ) {
        message = "Unable to find time series to process using TSList=\"" + TSList + "\" TSID=\"" + TSID +
            "\", EnsembleID=\"" + EnsembleID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( CommandPhaseType.RUN,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message,
                        "Verify that the TSID parameter matches one or more time series - may be OK for partial run." ) );
    }

	// Now try to write...

	String OutputFile_full = OutputFile;
	PrintWriter fout = null;
	try {
		// Convert to an absolute path...
		OutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
                TSCommandProcessorUtil.expandParameterValue(processor, this, OutputFile) ) );
	    List<String> problems = new ArrayList<String>();
		writePropertyFile ( processor, tslist, OutputFile_full, includeProperties, writeMode, fileFormat, sortOrder, problems );
		for ( String problem : problems ) {
			Message.printWarning ( warning_level, 
				MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, problem );
			status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE, problem, "Check log file for details." ) );
		}
		// Save the output file name...
		setOutputFile ( new File(OutputFile_full));
	}
	catch ( Exception e ) {
		message = "Unexpected error writing time series properties to file \"" + OutputFile_full + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE, message, "Check log file for details." ) );
		throw new CommandException ( message );
	}
	finally {
	    // Close the file...
	    if ( fout != null ) {
	        fout.close();
	    }
	}
	
	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

/**
Set the output file that is created by this command.  This is only used internally.
*/
private void setOutputFile ( File file )
{
	__OutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
    String TSList = parameters.getValue( "TSList" );
    String TSID = parameters.getValue( "TSID" );
    String EnsembleID = parameters.getValue( "EnsembleID" );
	String OutputFile = parameters.getValue ( "OutputFile" );
	String IncludeProperties = parameters.getValue ( "IncludeProperties" );
	String WriteMode = parameters.getValue ( "WriteMode" );
	String FileFormat = parameters.getValue ( "FileFormat" );
	String SortOrder = parameters.getValue( "SortOrder" );
	StringBuffer b = new StringBuffer ();
    if ( (TSList != null) && (TSList.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSList=" + TSList );
    }
    if ( (TSID != null) && (TSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "TSID=\"" + TSID + "\"" );
    }
    if ( (EnsembleID != null) && (EnsembleID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "EnsembleID=\"" + EnsembleID + "\"" );
    }
	if ( (OutputFile != null) && (OutputFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputFile=\"" + OutputFile + "\"" );
	}
	if ( (IncludeProperties != null) && (IncludeProperties.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IncludeProperties=\"" + IncludeProperties + "\"" );
	}
	if ( (WriteMode != null) && (WriteMode.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "WriteMode=" + WriteMode );
	}
    if ( (FileFormat != null) && (FileFormat.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "FileFormat=" + FileFormat );
    }
    if ( (SortOrder != null) && (SortOrder.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SortOrder=" + SortOrder );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

//TODO SAM 2015-05-16 Move this to generic shared code
/**
Write a single property to the output file.
*/
private void writeProperty ( PrintWriter fout, String propertyName, Object propertyObject, PropertyFileFormatType formatType )
{
  // Write the output...
  String quote = "";
  boolean doDateTime = false;
  // Only use double quotes around String and DateTime objects
  if ( propertyObject instanceof DateTime ) {
      doDateTime = true;
  }
  if ( (propertyObject instanceof String) || doDateTime ) {
      quote = "\"";
  }
  if ( formatType == PropertyFileFormatType.NAME_VALUE ) {
      fout.println ( propertyName + "=" + quote + propertyObject + quote );
  }
  else if ( formatType == PropertyFileFormatType.NAME_TYPE_VALUE ) {
      if ( doDateTime ) {
          fout.println ( propertyName + "=DateTime(" + quote + propertyObject + quote + ")" );
      }
      else {
          // Same as NAME_VALUE
          fout.println ( propertyName + "=" + quote + propertyObject + quote );
      }
  }
  else if ( formatType == PropertyFileFormatType.NAME_TYPE_VALUE_PYTHON ) {
      if ( doDateTime ) {
          DateTime dt = (DateTime)propertyObject;
          StringBuffer dtBuffer = new StringBuffer();
          dtBuffer.append("" + dt.getYear() );
          if ( dt.getPrecision() <= DateTime.PRECISION_MONTH ) {
              dtBuffer.append("," + dt.getMonth() );
          }
          if ( dt.getPrecision() <= DateTime.PRECISION_DAY ) {
              dtBuffer.append("," + dt.getDay() );
          }
          if ( dt.getPrecision() <= DateTime.PRECISION_HOUR ) {
              dtBuffer.append("," + dt.getHour() );
          }
          if ( dt.getPrecision() <= DateTime.PRECISION_MINUTE ) {
              dtBuffer.append("," + dt.getMinute() );
          }
          if ( dt.getPrecision() <= DateTime.PRECISION_SECOND ) {
              dtBuffer.append("," + dt.getSecond() );
          }
          // TODO SAM 2012-07-30 Evaluate time zone
          fout.println ( propertyName + "=DateTime(" + dtBuffer + ")" );
      }
      else {
          // Same as NAME_VALUE
          fout.println ( propertyName + "=" + quote + propertyObject + quote );
      }
  }
}

//TODO SAM 2012-07-27 Evaluate putting this in more generic code, perhaps IOUtil.PropList.writePersistent()?
// Much of this code is the same as from the WritePropertiesToFile() command
/**
Write the property file.
@param sortOrder sort order for output: -1=descending, 0=no sort, 1=ascending
*/
private List<String> writePropertyFile ( CommandProcessor processor, List<TS> tslist, String outputFileFull,
  String [] IncludeProperties, FileWriteModeType writeMode, PropertyFileFormatType formatType, int sortOrder, List<String> problems )
{
  PrintWriter fout = null;
  try {
      // Open the file...
      boolean doAppend = false; // Default is overwrite
      if ( writeMode == FileWriteModeType.APPEND ) {
          doAppend = true;
      }
      fout = new PrintWriter ( new FileOutputStream ( outputFileFull, doAppend ) );
      // Process time series in sequence
      TS ts;
      for ( int its = 0; its < tslist.size(); its++ ) {
    	  ts = tslist.get(its);
	      // Get the list of all time series properties
    	  HashMap<String,Object> propMap =  ts.getProperties();
    	  Set<String> keys = propMap.keySet();
	      List<String> propNameList = new ArrayList<String>(keys);
	      if ( sortOrder == 0 ) {
	      	  // Want to output in the order of the properties that were requested, not the order from the processor
	      	  // Rearrange the full list to make sure the requested properties are at the front
	      	  int foundCount = 0;
	      	  for ( int i = 0; i < IncludeProperties.length; i++ ) {
	      		  for ( int j = 0; j < propNameList.size(); j++ ) {
	      			  if ( IncludeProperties[i].equalsIgnoreCase(propNameList.get(j))) {
	      				  // Move to the front of the list and remove the original
	      				  propNameList.add(foundCount++,IncludeProperties[i]);
	      				  propNameList.remove(j + 1);
	      			  }
	      		  }
	      	  }
	      }
	  	  if ( sortOrder != 0 ) {
	  		  // Want to output sorted - first sort ascending
	  		  Collections.sort(propNameList,String.CASE_INSENSITIVE_ORDER);
	  	  }
	  	  if ( sortOrder < 0 ) {
	  		  // Reverse the order if needed
	  		  Collections.reverse(propNameList);
	  	  }
	      // Loop through property names retrieved from the processor
	      // If no specific properties were requested, write them all
	      // TODO SAM 2015-05-05 the list of properties only includes user properties, not built-in properties - need to combine
	  	  boolean doWrite;
	  	  boolean [] IncludePropertiesMatched = new boolean[IncludeProperties.length];
	  	  for ( int i = 0; i < IncludePropertiesMatched.length; i++ ) {
	  		  IncludePropertiesMatched[i] = false;
	  	  }
	      for ( String propName : propNameList ) {
	          doWrite = false;
	          if ( IncludeProperties.length == 0 ) {
	        	  doWrite = true;
	          }
	          else {
	          	  // Loop through the properties to include and see if there is a match
	          	  for ( int i = 0; i < IncludeProperties.length; i++ ) {
	  	              //Message.printStatus(2, "", "Writing property \"" + IncludeProperties[i] + "\"" );
	  	              if ( IncludeProperties[i].indexOf("*") >= 0 ) {
	  	            	  // Includes wildcards.  Check the user-specified properties
	            		  if ( propName.matches(IncludeProperties[i]) ) {
	            			  doWrite = true;
	            			  IncludePropertiesMatched[i] = true;
	            		  }
	  	              }
	  	              else {
	  		              // Match exactly
	  	            	  if ( propName.equals(IncludeProperties[i]) ) {
	            			  doWrite = true;
	            			  IncludePropertiesMatched[i] = true;
	            		  }
	  	              }
	          	  }
	          }
	          if ( doWrite ) {
	          	  try {
	          		  writeProperty(fout,propName,ts.getProperty(propName),formatType);
	          	  }
	          	  catch ( Exception e ) {
	          		  problems.add ( "Error writing property \"" + propName + "\" (" + e + ").");
	          	  }
	      	  }
	      }
	      for ( int i = 0; i < IncludePropertiesMatched.length; i++ ) {
	      	  if ( !IncludePropertiesMatched[i] ) {
	      		  problems.add ( "Unable to match property \"" + IncludeProperties[i] + "\" to write.");
	      	  }
	      }
      }
  }
  catch ( FileNotFoundException e ) {
      problems.add("Error opening output file \"" + outputFileFull + "\" (" + e + ")." );
  }
  finally {
      if ( fout != null ) {
          fout.close();
      }
  }
  return problems;
}

}
