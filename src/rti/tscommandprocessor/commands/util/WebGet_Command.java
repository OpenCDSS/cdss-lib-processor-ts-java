// WebGet_Command - This class initializes, checks, and runs the WebGet() command.

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

// FIXME SAM 2008-06-25 Need to clean up exception handling and command status in runCommand().

package rti.tscommandprocessor.commands.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;
import RTi.Util.IO.AbstractCommand;
import RTi.Util.IO.Command;
import RTi.Util.IO.CommandException;
import RTi.Util.IO.CommandLogRecord;
import RTi.Util.IO.CommandPhaseType;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.CommandStatusType;
import RTi.Util.IO.CommandStatus;
import RTi.Util.IO.CommandWarningException;
import RTi.Util.IO.FileGenerator;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.String.StringUtil;

/**
This class initializes, checks, and runs the WebGet() command.
*/
public class WebGet_Command extends AbstractCommand implements Command, FileGenerator
{

/**
Data members used for parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";

protected final String _False = "False";
protected final String _True = "True";
    
/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;
    
/**
Constructor.
*/
public WebGet_Command ()
{	super();
	setCommandName ( "WebGet" );
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
{	String URI = parameters.getValue ( "URI" );
	String ConnectTimeout = parameters.getValue ( "ConnectTimeout" );
	String ReadTimeout = parameters.getValue ( "ReadTimeout" );
	String RetryMax = parameters.getValue ( "RetryMax" );
	String RetryWait = parameters.getValue ( "RetryWait" );
	String IfHttpError = parameters.getValue ( "IfHttpError" );
	String LocalFile = parameters.getValue ( "LocalFile" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	CommandProcessor processor = getCommandProcessor();
	
	// The existence of the file to remove is not checked during initialization
	// because files may be created dynamically at runtime.

	if ( (URI == null) || (URI.length() == 0) ) {
		message = "The URI (Uniform Resource Identifier) must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the URI."));
	}

	if ( (ConnectTimeout != null) && !ConnectTimeout.isEmpty() && !StringUtil.isDouble(ConnectTimeout)) {
		message = "The ConnectTimeout (" + ConnectTimeout + ") is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the timeout as the number of ms."));
	}

	if ( (ReadTimeout != null) && !ReadTimeout.isEmpty() && !StringUtil.isDouble(ReadTimeout)) {
		message = "The ReadTimeout (" + ReadTimeout + ") is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the timeout as the number of ms."));
	}

	if ( (RetryMax != null) && !RetryMax.isEmpty() && !StringUtil.isInteger(RetryMax)) {
		message = "The RetryMax (" + RetryMax + ") is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the timeout as an integer."));
	}

	if ( (RetryWait != null) && !RetryWait.isEmpty() && !StringUtil.isDouble(RetryWait)) {
		message = "The RetryWait (" + RetryWait + ") is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the retry wait as the number of ms."));
	}

	// LocalFile is not required given that output property can be specified
    if ( (LocalFile != null) && !LocalFile.isEmpty() && (LocalFile.indexOf("${") < 0) ) {
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
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir,
                TSCommandProcessorUtil.expandParameterValue(processor,this,LocalFile)));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The local file parent directory does not exist for: \"" + adjusted_path + "\".";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                    message, "Create the output directory." ) );
            }
            f = null;
            f2 = null;
        }
        catch ( Exception e ) {
            message = "The local file:\n" +
            "    \"" + LocalFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify that local file and working directory paths are compatible." ) );
        }
    }

	if ( (IfHttpError != null) && !IfHttpError.isEmpty() && !IfHttpError.equalsIgnoreCase(_Ignore) &&
		!IfHttpError.equalsIgnoreCase(_Fail) && !IfHttpError.equalsIgnoreCase(_Warn) ) {
		message = "The IfHttpError (" + IfHttpError + ") parameter is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the IfHttpError parameter as " + _Ignore + ", " + _Warn + " (default), or " + _Fail + "."));
	}

	// Check for invalid parameters...
	List<String> validList = new ArrayList<>(9);
	validList.add ( "URI" );
	validList.add ( "EncodeURI" );
	validList.add ( "ConnectTimeout" );
	validList.add ( "ReadTimeout" );
	validList.add ( "RetryMax" );
	validList.add ( "RetryWait" );
	validList.add ( "LocalFile" );
	validList.add ( "OutputProperty" );
	validList.add ( "IfHttpError" );
	validList.add ( "ResponseCodeProperty" );
	warning = TSCommandProcessorUtil.validateParameterNames ( validList, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level),warning );
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
	return (new WebGet_JDialog ( parent, this )).ok();
}

/**
 * Encode the URL.  Only encode the query parameter values to the right of '='.
 * @param uri full URI
 * @return the encoded URI
 */
private String encode ( String uri ) throws UnsupportedEncodingException {
	int pos = uri.indexOf("?");
	String uriEncoded = uri;
	int paramCount = 0;
	if ( (pos > 0) && (uri.substring(pos).indexOf("=") > 0) ) {
		// URI includes query parameters.
		StringBuilder b = new StringBuilder(uri.substring(0,pos));
		b.append("?");
		String query = uri.substring(pos + 1);
		// Split by &, if only one parameter after ? will return one part.
		String [] paramParts = query.split("&");
		for ( String param : paramParts ) {
			++paramCount;
			// Split by =.
			String [] vParts = param.split("=");
			if ( paramCount > 1 ) {
				b.append("&");
			}
			b.append(vParts[0]);
			b.append("=");
			b.append(URLEncoder.encode(vParts[1], StandardCharsets.UTF_8.toString()));
		}
		uriEncoded = b.toString();
	}
	Message.printStatus(2, "", "URL before encoding=\"" + uri + "\", after=\"" + uriEncoded + "\"");
	return uriEncoded;
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

// Use base class parseCommand

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;
	int warning_count = 0;
	
    // Clear the output file
    
    setOutputFile ( null );
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
    Boolean clearStatus = new Boolean(true); // default
    try {
    	Object o = processor.getPropContents("CommandsShouldClearRunStatus");
    	if ( o != null ) {
    		clearStatus = (Boolean)o;
    	}
    }
    catch ( Exception e ) {
    	// Should not happen
    }
    if ( clearStatus ) {
		status.clearLog(CommandPhaseType.RUN);
	}
	
	String URI = parameters.getValue ( "URI" );
	if ( URI != null ) {
	    URI = TSCommandProcessorUtil.expandParameterValue(processor,this,URI);
	    Message.printStatus(2, routine, "URI after expanding is \"" + URI + "\"");
	}
	if ( URI.indexOf("${") >= 0 ) {
		// The above expansion did not work and will cause problems when doing the request.
		message = "URI after expansion contains property reference (not a valid URI): " + URI;
		Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
			status.addToLog ( CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Check that the property is defined for the command." ) );
	}
	String EncodeURI = parameters.getValue ( "EncodeURI" );
	boolean encodeUri = true; // Default.
	if ( (EncodeURI != null) && ! EncodeURI.isEmpty() ) {
		if ( EncodeURI.equalsIgnoreCase("false") ) {
			encodeUri = false;
		}
	}
    String ConnectTimeout = parameters.getValue ( "ConnectTimeout" );
    int connectTimeout = 60000;
	if ( (ConnectTimeout != null) && StringUtil.isInteger(ConnectTimeout) ) {
		Double d = Double.parseDouble(ConnectTimeout);
		connectTimeout = d.intValue();
	}
    String ReadTimeout = parameters.getValue ( "ReadTimeout" );
    int readTimeout = 60000;
	if ( (ReadTimeout != null) && StringUtil.isInteger(ReadTimeout) ) {
		Double d = Double.parseDouble(ReadTimeout);
		readTimeout = d.intValue();
	}
    String RetryMax = parameters.getValue ( "RetryMax" );
    int retryMax = 0;
	if ( (ReadTimeout != null) && StringUtil.isInteger(RetryMax) ) {
		retryMax = Integer.parseInt(RetryMax);
	}
    String RetryWait = parameters.getValue ( "RetryWait" );
    int retryWait = 0;
	if ( (RetryWait != null) && StringUtil.isInteger(RetryWait) ) {
		retryWait = Integer.parseInt(RetryWait);
	}
    String LocalFile = parameters.getValue ( "LocalFile" );
    boolean doOutputFile = false;
	if ( (LocalFile != null) && !LocalFile.isEmpty() ) {
		LocalFile = TSCommandProcessorUtil.expandParameterValue(processor,this,LocalFile);
		doOutputFile = true;
	}
	String LocalFile_full = LocalFile;
	if ( (LocalFile != null) && !LocalFile.isEmpty() ) {
		LocalFile_full = IOUtil.verifyPathForOS(
	        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),LocalFile) );
	}
	boolean doOutputProperty = false;
	String OutputProperty = parameters.getValue ( "OutputProperty" );
	if ( (OutputProperty != null) && !OutputProperty.isEmpty() ) {
		doOutputProperty = true;
	}
	String IfHttpError = parameters.getValue ( "IfHttpError" );
	if ( IfHttpError == null ) {
		IfHttpError = _Warn; // Default
	}
	boolean doResponseCodeProperty = false;
	String ResponseCodeProperty = parameters.getValue ( "ResponseCodeProperty" );
	if ( (ResponseCodeProperty != null) && !ResponseCodeProperty.isEmpty() ) {
		doResponseCodeProperty = true;
	}

	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}

	try {
		// Encode the URI if requested.
		if ( encodeUri ) {
			URI = encode ( URI );
		}
    	if ( retryMax <= 0 ) {
    		// Do at least one try.
    		retryMax = 1;
    	}
    	boolean readSuccessful = false;
		int responseCode = -1;
    	for ( int iRetry = 1; iRetry <= retryMax; ++iRetry ) {
    		if ( iRetry > 1 ) {
    			// Second and later retries.  Apply the wait.
    			if ( retryWait > 0 ) {
    				Thread.sleep(retryWait);
    			}
    		}
    		FileOutputStream fos = null;
	    	HttpURLConnection urlConnection = null;
	    	InputStream is = null;
   			BufferedInputStream isr = null;
    		StringBuilder content = null;
    		responseCode = -1;
    		if ( doOutputProperty ) {
    			content = new StringBuilder();
    		}
    		try {
    			// Some sites need cookie manager:
    			// (see http://stackoverflow.com/questions/11022934/getting-java-net-protocolexception-server-redirected-too-many-times-error)
    			CookieHandler.setDefault(new CookieManager(null,CookiePolicy.ACCEPT_ALL));
    			// Open the input stream.
    			Message.printStatus(2,routine,"Reading URI \"" + URI + "\" (try " + iRetry + ")." );
    			URL url = new URL(URI);
    			urlConnection = (HttpURLConnection)url.openConnection();
   				Message.printStatus(2,routine,"Connect timeout default is: " + urlConnection.getConnectTimeout() );
   				Message.printStatus(2,routine,"Read timeout default is: " + urlConnection.getReadTimeout() );
    			if ( connectTimeout > 0 ) {
    				urlConnection.setConnectTimeout(connectTimeout);
    			}
    			if ( readTimeout > 0 ) {
    				urlConnection.setReadTimeout(readTimeout);
    			}
    			is = urlConnection.getInputStream();
    			isr = new BufferedInputStream(is);
    			// Open the output file.
    			if ( doOutputFile ) {
    				fos = new FileOutputStream( LocalFile_full );
    			}
    			// Output the characters to the local file.
    			int numCharsRead;
    			// 8K optimal for small files:
    			// - TODO smalers, 2021-08-16 should a command parameter allow changing?
    			int arraySize = 8192;
    			byte[] byteArray = new byte[arraySize];
    			int bytesRead = 0;
    			while ((numCharsRead = isr.read(byteArray, 0, arraySize)) != -1) {
    				if ( doOutputFile ) {
    					fos.write(byteArray, 0, numCharsRead);
    				}
    				if ( doOutputProperty ) {
    					// Also set the content in memory to set property below.
    					if ( numCharsRead == byteArray.length ) {
    						content.append(new String(byteArray));
    					}
    					else {
    						byte [] byteArray2 = new byte[numCharsRead];
    						System.arraycopy(byteArray, 0, byteArray2, 0, numCharsRead);
    						content.append(new String(byteArray2));
    					}
    				}
    				bytesRead += numCharsRead;
    			}
    			// Save the output file name.
    			Message.printStatus(2,routine,"Number of bytes read=" + bytesRead );
    			if ( doOutputFile ) {
    				setOutputFile ( new File(LocalFile_full));
    			}
    			// If requested, also set as a property.
    			if ( doOutputProperty ) {
    				PropList request_params = new PropList ( "" );
    				request_params.setUsingObject ( "PropertyName", OutputProperty );
    				request_params.setUsingObject ( "PropertyValue", content.toString() );
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
    			
    			// If here successful so break out of the retry loop.
    			readSuccessful = true;
    		}
    		catch (MalformedURLException me) {
    			message = "URI \"" + URI + "\" is malformed (" + me + ")";
    			Message.printWarning ( warning_level, 
                   MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    			Message.printWarning ( 3, routine, me );
    			status.addToLog(CommandPhaseType.RUN,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "See the log file for details."));
    		}
    		catch ( SocketTimeoutException te ) {
    			if ( iRetry <= 5 ) {
    				message = "Try " + iRetry + " - connect or read timeout reading URI \"" + URI +
    					"\" (" + te + "), only logging message for retries <= 5 (RetryMax=" + RetryMax + ").";
    				Message.printWarning ( warning_level, 
                    	MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    				Message.printWarning ( 3, routine, te );
    				status.addToLog(CommandPhaseType.RUN,
    					new CommandLogRecord(CommandStatusType.FAILURE,
    						message, "See the log file for details."));
    			}
    		}
    		catch (IOException ioe) {
    			StringBuilder sb = new StringBuilder("Error opening URI \"" + URI + "\" (" + ioe + ").\n" );
    			// Try reading error stream - may only work for some error numbers.
    			if ( urlConnection != null ) {
    				is = urlConnection.getErrorStream(); // Close in finally.
    				if ( is != null ) {
    					sb.append ( " " );
    					BufferedReader br = new BufferedReader(new InputStreamReader(is));
    					// Output the lines to a StringBuilder to improve error handling.
    					String s;
    					while ((s = br.readLine()) != null ) {
    						sb.append(s);
    						// Append the newline to make sure command status output looks nice.
    						sb.append("\n");
    					}
    				}
    			}
    			sb.append ( ")" );
    			Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, sb.toString() );
    			Message.printWarning ( 3, routine, ioe );
    			status.addToLog(CommandPhaseType.RUN,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					sb.toString(), "See the log file for details.  Try using the full URI in a web browser."));
    		}
    		catch (Exception e) {
    			// Catch everything else - should probably add specific handling to troubleshoot.
    			message = "Unexpected error reading URI \"" + URI + "\" (" + e + ")";
    			Message.printWarning ( warning_level, 
                    MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    			Message.printWarning ( 3, routine, e );
    			status.addToLog(CommandPhaseType.RUN,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "See the log file for details."));
    		}
    		finally {
    			// Close the streams and connection.
    			if ( isr != null ) {
    				try {
    					isr.close();
    				}
    				catch ( IOException e ) {
    				}
    			}
    			if ( doOutputFile ) {
    				if ( fos != null ) {
    					fos.close();
    				}
    			}
    			if ( urlConnection != null ) {
    				responseCode = urlConnection.getResponseCode();
    				urlConnection.disconnect();
    				// If requested, set response code as a property:
    				// - this is set in the loop, should be OK
    				if ( doResponseCodeProperty ) {
    					PropList request_params = new PropList ( "" );
    					request_params.setUsingObject ( "PropertyName", ResponseCodeProperty );
    					request_params.setUsingObject ( "PropertyValue", new Integer(responseCode) );
    					try {
    						processor.processRequest( "SetProperty", request_params);
    					}
    					catch ( Exception e ) {
    						message = "Error requesting SetProperty(Property=\"" + ResponseCodeProperty + "\") from processor.";
    						Message.printWarning(log_level,
    							MessageUtil.formatMessageTag( command_tag, ++warning_count),
    							routine, message );
    						status.addToLog ( CommandPhaseType.RUN,
    							new CommandLogRecord(CommandStatusType.FAILURE,
    								message, "Report the problem to software support." ) );
    					}
    				}
    			}
    		} // End 'finally'
   			if ( readSuccessful ) {
   				break;
   			}
    	} // End retry loop.

    	// Check the response code.
    	if ( readSuccessful ) {
    		if ( responseCode != 200 ) {
    			message = "Response code (" + responseCode + ") indicates an error retrieving the resource";
        	    if ( IfHttpError.equalsIgnoreCase(_Fail) ) {
            	    Message.printWarning ( warning_level,
                	    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	    status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    	    message, "Verify that the URI is correct, for example in a web browser."));
        	    }
        	    else if ( IfHttpError.equalsIgnoreCase(_Warn) ) {
            	    Message.printWarning ( warning_level,
                	    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	    status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                    	    message, "Verify that the URI is correct, for example in a web browser."));
        	    }
        	    else {
            	    Message.printStatus( 2, routine, message + "Ignoring HTTP error " + responseCode);
        	    }
    		}
    	}
	}
	catch ( Exception e ) {
		message = "Unexpected error getting resource from \"" + URI + "\" (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
		Message.printWarning ( 3, routine, e );
		status.addToLog(CommandPhaseType.RUN,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "See the log file for details.  Make sure the output file is not open in other software."));
		throw new CommandException ( message );
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
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
    String URI = parameters.getValue ( "URI" );
    String EncodeURI = parameters.getValue ( "EncodeURI" );
    String ConnectTimeout = parameters.getValue ( "ConnectTimeout" );
    String ReadTimeout = parameters.getValue ( "ReadTimeout" );
    String RetryMax = parameters.getValue ( "RetryMax" );
    String RetryWait = parameters.getValue ( "RetryWait" );
    String LocalFile = parameters.getValue ( "LocalFile" );
    String OutputProperty = parameters.getValue ( "OutputProperty" );
    String IfHttpError = parameters.getValue ( "IfHttpError" );
    String ResponseCodeProperty = parameters.getValue ( "ResponseCodeProperty" );
	StringBuffer b = new StringBuffer ();
	if ( (URI != null) && (URI.length() > 0) ) {
		b.append ( "URI=\"" + URI + "\"" );
	}
	if ( (EncodeURI != null) && (EncodeURI.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "EncodeURI=" + EncodeURI );
	}
	if ( (ConnectTimeout != null) && (ConnectTimeout.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ConnectTimeout=" + ConnectTimeout );
	}
	if ( (ReadTimeout != null) && (ReadTimeout.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ReadTimeout=" + ReadTimeout );
	}
	if ( (RetryMax != null) && (RetryMax.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RetryMax=" + RetryMax );
	}
	if ( (RetryWait != null) && (RetryWait.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "RetryWait=" + RetryWait );
	}
	if ( (LocalFile != null) && (LocalFile.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "LocalFile=\"" + LocalFile + "\"" );
	}
	if ( (OutputProperty != null) && (OutputProperty.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "OutputProperty=\"" + OutputProperty + "\"" );
	}
	if ( (IfHttpError != null) && (IfHttpError.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfHttpError=\"" + IfHttpError + "\"" );
	}
	if ( (ResponseCodeProperty != null) && (ResponseCodeProperty.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "ResponseCodeProperty=\"" + ResponseCodeProperty + "\"" );
	}
	return getCommandName() + "(" + b.toString() + ")";
}

}