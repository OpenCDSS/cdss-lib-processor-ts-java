// WebGet_Command - This class initializes, checks, and runs the WebGet() command.

/* NoticeStart

CDSS Time Series Processor Java Library
CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2022 Colorado Department of Natural Resources

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
import java.io.OutputStreamWriter;
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
import java.util.Map;
import java.util.TreeMap;

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
import RTi.Util.String.MultiKeyStringDictionary;
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

protected final String DELETE = "DELETE";
protected final String GET = "GET";
protected final String OPTIONS = "OPTIONS";
protected final String POST = "POST";
protected final String PUT = "PUT";

/**
Output file that is created by this command.
*/
private File __OutputFile_File = null;

/**
Constructor.
*/
public WebGet_Command () {
	super();
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
throws InvalidCommandParameterException {
	String URI = parameters.getValue ( "URI" );
	String EncodeURI = parameters.getValue ( "EncodeURI" );
	String RequestMethod = parameters.getValue ( "RequestMethod" );
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

	if ( (EncodeURI != null) && !EncodeURI.isEmpty() && !EncodeURI.equalsIgnoreCase(this._False) && !EncodeURI.equalsIgnoreCase(this._True) ) {
		message = "The EncodeURI parameter is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
						message, "Specify the EncodeURI parameter as " + this._False + " or " + this._True + " (default)."));
	}

	if ( (RequestMethod != null) && !RequestMethod.isEmpty() &&
		!RequestMethod.equalsIgnoreCase(this.DELETE) &&
		!RequestMethod.equalsIgnoreCase(this.GET) &&
		!RequestMethod.equalsIgnoreCase(this.OPTIONS) &&
		!RequestMethod.equalsIgnoreCase(this.POST) &&
		!RequestMethod.equalsIgnoreCase(this.PUT)) {
		message = "The request method (" + RequestMethod + ") is invalid.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the request method as " + this.DELETE + ", " + this.GET + " (default), " + this.OPTIONS +
					", " + this.POST + " or " + this.PUT + "."));
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

	// LocalFile is not required given that output property can be specified.
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

	// Check for invalid parameters.
	List<String> validList = new ArrayList<>(14);
	validList.add ( "URI" );
	validList.add ( "EncodeURI" );
	validList.add ( "RequestMethod" );
	validList.add ( "PayloadFile" );
	validList.add ( "HttpHeaders" );
	validList.add ( "Cookies" );
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
@return true if the command was edited (e.g., "OK" was pressed), and false if not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent ) {
	// The command will be modified if changed.
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
			// The parameter may contain multiple = (such as if a complex value like a query) so split on the first = character.
			int posEqual = param.indexOf("=");
			//String [] vParts = param.split("=");
			if ( paramCount > 1 ) {
				b.append("&");
			}
			//b.append(vParts[0]);
			// TODO smalers 2022-05-10 should the parameter name and equals also be encoded?
			b.append(param.substring(0,posEqual));
			b.append("=");
			//b.append(URLEncoder.encode(vParts[1], StandardCharsets.UTF_8.toString()));
			b.append(URLEncoder.encode(param.substring(posEqual+1), StandardCharsets.UTF_8.toString()));
		}
		uriEncoded = b.toString();
	}
	Message.printStatus(2, "", "URL before encoding=\"" + uri + "\", after=\"" + uriEncoded + "\"");
	return uriEncoded;
}

/**
Return the list of files that were created by this command.
*/
public List<File> getGeneratedFileList () {
    List<File> list = new ArrayList<>();
    if ( getOutputFile() != null ) {
        list.add ( getOutputFile() );
    }
    return list;
}

/**
Return the output file generated by this file.  This method is used internally.
*/
private File getOutputFile () {
    return __OutputFile_File;
}

/**
 * Determine whether an HTTP response code is a redirect.
 * @return true if a redirect, false if not
 */
private boolean isRedirect ( int responseCode ) {
    if ( (responseCode == HttpURLConnection.HTTP_MOVED_TEMP) ||
		(responseCode == HttpURLConnection.HTTP_MOVED_PERM) ||
		(responseCode == HttpURLConnection.HTTP_SEE_OTHER) ) {
    	return true;
    }
    else {
    	return false;
    }
}

// Use base class parseCommand.

/**
Run the command.
@param command_line Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException {
	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	int log_level = 3; // Level for non-user messages for log file.
	String command_tag = "" + command_number;
	int warning_count = 0;
	
    // Clear the output file.

    setOutputFile ( null );
	
	PropList parameters = getCommandParameters();
	
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
		status.clearLog(CommandPhaseType.RUN);
	}
	
	String URI = parameters.getValue ( "URI" );
	URI = TSCommandProcessorUtil.expandParameterValue(processor,this,URI);
	Message.printStatus(2, routine, "URI after expanding is \"" + URI + "\"");
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
	if ( (EncodeURI != null) && !EncodeURI.isEmpty() ) {
		if ( EncodeURI.equalsIgnoreCase("false") ) {
			encodeUri = false;
		}
	}
	String RequestMethod = parameters.getValue ( "RequestMethod" );
	if ( (RequestMethod == null) || RequestMethod.isEmpty() ) {
		RequestMethod = this.GET; // Default.
	}
	/*
	if ( RequestMethod.equalsIgnoreCase(this.GET)) {
		message = "Only the GET request method is currently supported.  Cannot process: " + RequestMethod;
		Message.printWarning(log_level,
			MessageUtil.formatMessageTag( command_tag, ++warning_count),
				routine, message );
		status.addToLog ( CommandPhaseType.RUN,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Contact support - the software needs to be enhanced." ) );
	}
	*/

    String PayloadFile = parameters.getValue ( "PayloadFile" );
	PayloadFile = TSCommandProcessorUtil.expandParameterValue(processor,this,PayloadFile);
	String payloadFile_full = PayloadFile;
	File payloadFile = null;
	if ( (PayloadFile != null) && !PayloadFile.isEmpty() ) {
		payloadFile_full = IOUtil.verifyPathForOS(
	        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),PayloadFile) );
		payloadFile = new File(payloadFile_full);
	}

	// Headers are a dictionary:
	//   key1:value,key2:value2,...
	// - if the value contains a colon, surround the value with single quotes.
	// - the value cannot contain duplicates
    String HttpHeaders = parameters.getValue ( "HttpHeaders" );
	HttpHeaders = TSCommandProcessorUtil.expandParameterValue(processor,this,HttpHeaders);
   	MultiKeyStringDictionary httpHeaders = null;
    if ( (HttpHeaders != null) && !HttpHeaders.isEmpty() ) {
    	// Parse the headers.
    	httpHeaders = new MultiKeyStringDictionary ( HttpHeaders, ":", "," );
    }
	// Cookies are a dictionary:
	//   key1:value,key2:value2,...
	// - if the value contains a colon, surround the value with single quotes.
	// - the value cannot contain duplicates
    String Cookies = parameters.getValue ( "Cookies" );
	Cookies = TSCommandProcessorUtil.expandParameterValue(processor,this,Cookies);
   	MultiKeyStringDictionary cookies = null;
    if ( (Cookies != null) && !Cookies.isEmpty() ) {
    	// Parse the headers.
    	cookies = new MultiKeyStringDictionary ( Cookies, ":", "," );
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
		IfHttpError = _Warn; // Default.
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
	
	// Run the HTTP request.
	// Creating an HttpURLConnection object does not actually perform the request.
	// Once the connection is created, properties can be set on the object.
	// The request is fired when the following methods are called:
	//
	// Explicit firing:
	//
	// connect()
	//
	// Implicit firing:
	//
	// getContentLength()
	// getOutputStream()
	//
	// Also, connections automatically follow redirects, except not between http and https.
	// Therefore, return status of 3xx need to attempt the action on the redirect link.

	try {
		// Encode the URI if requested.
		if ( encodeUri ) {
			URI = encode ( URI );
		}
    	if ( retryMax <= 0 ) {
    		// Do at least one try.
    		retryMax = 1;
    	}
    	boolean requestSuccessful = false;
		int responseCode = -1;
    	//for ( int iRetry = 1; iRetry <= retryMax; ++iRetry ) {
		// Number of retries, corresponding to RetryMax parameter:
		// - redirects do not increment the retry count
		int iRetry = 0;
		// The redirect count:
		// - probably won't be a large number of redirects but limit just in case
		//   there is a circular redirect
		int redirectMax = 100;
		int iRedirect = 0;
		// Response code from the HTTP request:
		// - initialized to -1 for first call and after that should be 3xx for redirect,
		//   200 for success, etc.
   		responseCode = -1;
   		// The connection is initially null but retries will result in a non-null connection that needs to be disconnected.
    	HttpURLConnection urlConnection = null;
		while ( true ) {
			// First iteration is retry 1.
			++iRetry;
    		if ( iRetry > 1 ) {
    			// Second and later retries.  Apply the wait.
   				Message.printStatus(2, routine, "Attempt number " + iRetry + ", waiting " + retryWait + " milliseconds first.");
    			if ( retryWait > 0 ) {
    				Thread.sleep(retryWait);
    			}
    		}
    		if ( iRetry > retryMax ) {
    			Message.printStatus(2, routine, "Maximum retry (" + retryMax + ") reached.");
    			break;
    		}
    		if ( iRedirect > redirectMax ) {
    			Message.printStatus(2, routine, "Maximum redirect (" + redirectMax + ") reached.");
    			break;
    		}
    		FileOutputStream fos = null;
	    	InputStream is = null;
 			OutputStreamWriter outputStream = null;
   			BufferedInputStream isr = null;
    		StringBuilder content = null;
    		if ( doOutputProperty ) {
    			content = new StringBuilder();
    		}
    		try {
    			// Do global setup if the first try.
    			if ( iRetry == 1 ) {
    				// Some sites need cookie manager:
    				// (see http://stackoverflow.com/questions/11022934/getting-java-net-protocolexception-server-redirected-too-many-times-error)
    				CookieHandler.setDefault(new CookieManager(null,CookiePolicy.ACCEPT_ALL));
    			}

    			// Check for redirects:
    			// - initial try 'responseCode' will be -1 so is not a redirect
    			// - subsequent tries may result in a 3xx code, which indicates a redirect
    			if ( isRedirect(responseCode) ) {
    				// Previous loop detected a redirect and therefore could not complete:
    				// - the default is to follow redirects but if the schema is different (e.g, http, https),
    				//   the redirect is not automatically followed and the following handles
    				// - different technologies have maximum on redirects but loop for 100, which is unlikely to be reached.

    				// Decrement the try count since redirects don't count.
    				--iRetry;
    				// URL is a redirect so need to reopen the connection with the redirect.
    				if ( urlConnection != null ) {
    					// First close the old connection to free resources.
    					urlConnection.disconnect();
    				}
    				String newUrl = urlConnection.getHeaderField("Location");
    				Message.printStatus(2,routine,"Executing " + RequestMethod + " for redirect URL \"" + newUrl + "\" (redirect count=" + iRedirect + ")." );
    				URL url = new URL(newUrl);
    				urlConnection = (HttpURLConnection)url.openConnection();
    			}
    			else {
    				// Create the connection:
    				// - could be the first attempt
    				// - could be the final link of a redirect list
    				if ( urlConnection != null ) {
    					// First close the old connection to free resources.
    					urlConnection.disconnect();
    				}
    				// Open the input connection.
    				Message.printStatus(2,routine,"Executing " + RequestMethod + " request for URI \"" + URI + "\" (try " + iRetry + ")." );
    				URL url = new URL(URI);
    				urlConnection = (HttpURLConnection)url.openConnection();
    			}

   				// Add headers.
    			if ( httpHeaders == null ) {
    				Message.printStatus(2,routine,"Have 0 request headers from HttpHeaders parameter." );
    			}
    			else {
    				Message.printStatus(2,routine,"Have " + httpHeaders.size() + " request headers from HttpHeaders parameter." );
					for ( int i = 0; i < httpHeaders.size(); i++ ) {
						String key = httpHeaders.getKey(i);
						String value = httpHeaders.getValue(i);
  						Message.printStatus(2,routine,"  Adding request header: " + key + " = " + value );
  						urlConnection.setRequestProperty(key, value);
   					}
    			}

   				// Add cookies.
    			if ( cookies == null ) {
    				Message.printStatus(2,routine,"Have 0 cookies from Cookies parameter." );
    			}
    			else {
    				Message.printStatus(2,routine,"Have " + cookies.size() + " request cookies from Cookies parameter." );
    				StringBuilder cookieBuilder = new StringBuilder();
					for ( int i = 0; i < cookies.size(); i++ ) {
						String key = cookies.getKey(i);
						String value = cookies.getValue(i);
  						Message.printStatus(2,routine,"  Adding request cookie: " + key + " = " + value );
  						if ( cookieBuilder.length() > 0 ) {
  							cookieBuilder.append("; ");
  						}
  						cookieBuilder.append(key + "=" + value);
   					}
					// The header name is "Cookie" and the value is a list of 1+ cookies.
					urlConnection.setRequestProperty("Cookie", cookieBuilder.toString());
    			}

    			// Print default connection information the first time.
    			if ( iRetry == 1 ) {
    				Message.printStatus(2,routine,"Connect timeout default is: " + urlConnection.getConnectTimeout() );
   					Message.printStatus(2,routine,"Read timeout default is: " + urlConnection.getReadTimeout() );
    			}

    			// Set the timeout information for the current connection attempt.
    			if ( connectTimeout > 0 ) {
    				Message.printStatus(2,routine,"Setting the connect to: " + connectTimeout );
    				urlConnection.setConnectTimeout(connectTimeout);
    			}
    			if ( readTimeout > 0 ) {
    				Message.printStatus(2,routine,"Setting the read to: " + readTimeout );
    				urlConnection.setReadTimeout(readTimeout);
    			}
    			
    			if ( RequestMethod.equalsIgnoreCase(this.DELETE) ) {
    				// TODO smalers 2022-05-18 need to complete the implementation.
    				// See:  https://stackoverflow.com/questions/1051004/how-to-send-put-delete-http-request-in-httpurlconnection
    				urlConnection.setRequestMethod("DELETE");

    			    // Call the 'connect' method to explicitly fire the request.
    			    // Check the response code immediately in case it was a redirect.
   				    urlConnection.connect();
   				    // Get the response code:
   				    // - if 3xx, loop again to follow the redirect
   				    responseCode = urlConnection.getResponseCode();
   				    if ( isRedirect(responseCode) ) {
   					    // The above code will check the response code and follow the redirect.
   					    continue;
   				    }
    			
    				requestSuccessful = false;
    			}
    			else if ( RequestMethod.equalsIgnoreCase(this.GET) ) {
    				urlConnection.setRequestMethod("GET");

    			    // Call the 'connect' method to explicitly fire the request.
    			    // Check the response code immediately in case it was a redirect.
   				    urlConnection.connect();
   				    // Get the response code:
   				    // - if 3xx, loop again to follow the redirect
   				    responseCode = urlConnection.getResponseCode();
   				    if ( isRedirect(responseCode) ) {
   					    // The above code will check the response code and follow the redirect.
   					    continue;
   				    }

    				// Read the response:
    				// - it is a bit confusing as to what is input and output stream but follow examples
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
    				requestSuccessful = true;
    			}
    			else if ( RequestMethod.equalsIgnoreCase(this.OPTIONS) ) {
    				// TODO smalers 2022-05-18 need to complete the implementation.
    				// See:  https://stackoverflow.com/questions/1051004/how-to-send-put-delete-http-request-in-httpurlconnection
    				urlConnection.setRequestMethod("OPTIONS");
    				// The following should be added with HttpHeaders command parameter:
    				//urlConnection.setRequestProperty("Access-Control-Request-Method", "POST");
    				//urlConnection.setRequestProperty("Access-Control-Request-Headers", "content-type");
    				//urlConnection.setRequestProperty("Origin", "https://poudre.openwaterfoundation.org");

    			    // Call the 'connect' method to explicitly fire the request.
    			    // Check the response code immediately in case it was a redirect.
   				    urlConnection.connect();
   				    // Get the response code:
   				    // - if 3xx, loop again to follow the redirect
   				    responseCode = urlConnection.getResponseCode();
   				    if ( isRedirect(responseCode) ) {
   					    // The above code will check the response code and follow the redirect.
   					    continue;
   				    }

   				    // Response headers are handled below.

    				requestSuccessful = true;
    			}
    			else if ( RequestMethod.equalsIgnoreCase(this.POST) ) {
    				urlConnection.setRequestMethod("POST");
    				urlConnection.setDoOutput(true);

    			    // Call the 'connect' method to explicitly fire the request.
    			    // Check the response code immediately in case it was a redirect.
   				    urlConnection.connect();
   				    // Get the response code:
   				    // - if 3xx, loop again to follow the redirect
   				    responseCode = urlConnection.getResponseCode();
   				    if ( isRedirect(responseCode) ) {
   					    // The above code will check the response code and follow the redirect.
   					    continue;
   				    }

    				if ( (payloadFile != null) && payloadFile.exists() ) {
    					outputStream = new OutputStreamWriter(urlConnection.getOutputStream());
    					StringBuilder payloadBuilder = IOUtil.fileToStringBuilder(payloadFile.getAbsolutePath());
    					outputStream.write(payloadBuilder.toString());
    					outputStream.flush();
    					outputStream.close();
    				}
    				requestSuccessful = true;
    			}
    			else if ( RequestMethod.equalsIgnoreCase(this.PUT) ) {
    				urlConnection.setRequestMethod("PUT");
    				urlConnection.setDoOutput(true);

    			    // Call the 'connect' method to explicitly fire the request.
    			    // Check the response code immediately in case it was a redirect.
   				    urlConnection.connect();
   				    // Get the response code:
   				    // - if 3xx, loop again to follow the redirect
   				    responseCode = urlConnection.getResponseCode();
   				    if ( isRedirect(responseCode) ) {
   					    // The above code will check the response code and follow the redirect.
   					    continue;
   				    }

    				if ( (payloadFile != null) && payloadFile.exists() ) {
    					outputStream = new OutputStreamWriter(urlConnection.getOutputStream());
    					StringBuilder payloadBuilder = IOUtil.fileToStringBuilder(payloadFile.getAbsolutePath());
    					outputStream.write(payloadBuilder.toString());
    					outputStream.flush();
    					outputStream.close();
    				}
    				requestSuccessful = true;
    			}
    			else {
    				message = "Request method \"" + RequestMethod + "\" is not recognized.";
    				Message.printWarning ( warning_level,
                   	MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    				status.addToLog(CommandPhaseType.RUN,
    					new CommandLogRecord(CommandStatusType.FAILURE,
    						message, "See the log file for details."));
    				break;
    			}

    			// Get the response headers and print to the log file:
    			// - the map is unmodifiable so have to make a copy before sorting
    			// - also handle null Map key, which indicates the response code
    			Map<String, List<String>> responseHeaders = urlConnection.getHeaderFields();
    			if ( responseHeaders != null ) {
    				Message.printStatus(2, routine, "Have " + responseHeaders.size() + " response headers.");
    				// Can't just create a TreeMap from the map object because it can't handle null,
    				// so iterate to transfer objects.
    				TreeMap<String, List<String>> sorted = new TreeMap<>();
   					for ( Map.Entry<String, List<String>> entry : responseHeaders.entrySet() ) {
   						if ( entry.getKey() == null ) {
   							// A key may be null:
   							// - this seems to be used for the response code (null = [HTTP/1.1 200 OK])
    						// - replace with a string so sort on keys will work
   							sorted.put("null", entry.getValue() );
   						}
   						else {
   							sorted.put(entry.getKey(), entry.getValue() );
   						}
   					}
    				for ( Map.Entry<String, List<String>> entry : sorted.entrySet() ) {
    					Message.printStatus(2, routine, "  Response header " + entry.getKey() + " = " + entry.getValue());
    				}
    			}
    			else {
    				Message.printStatus(2, routine, "Have 0 response headers.");
    			}
    		} // Try for executing the request.
    		catch (MalformedURLException me) {
    			// A bad URI will not retry since the same error will be generated each time.
    			message = "URI \"" + URI + "\" is malformed (" + me + ")";
    			Message.printWarning ( warning_level,
                   MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
    			Message.printWarning ( 3, routine, me );
    			status.addToLog(CommandPhaseType.RUN,
    				new CommandLogRecord(CommandStatusType.FAILURE,
    					message, "See the log file for details."));
    			break;
    		}
    		catch ( SocketTimeoutException te ) {
    			// This exception allows the full number of retries.
    			if ( iRetry <= 5 ) {
    				// Only print the message the first 5 times.
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
    			// For now... this exception allows the full number of retries.
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
    			// For now... this exception allows the full number of retries.
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
    			// Close the streams and connection if open.
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
   			if ( requestSuccessful ) {
   				// The request was successful so no need to retry.
   				break;
   			}
    	} // End retry loop.

    	// Check whether the response code needs to be set as a processor property.
    	if ( requestSuccessful ) {
    		if ( responseCode != 200 ) {
    			message = "Response code (" + responseCode + ") is not 200, which indicates an error retrieving the resource.";
        	    if ( IfHttpError.equalsIgnoreCase(_Fail) ) {
            	    Message.printWarning ( warning_level,
                	    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	    status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
                    	    message, "Verify that the URI is correct, for example try in a web browser."));
        	    }
        	    else if ( IfHttpError.equalsIgnoreCase(_Warn) ) {
            	    Message.printWarning ( warning_level,
                	    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
            	    status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
                    	    message, "Verify that the URI is correct, for example try in a web browser."));
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
private void setOutputFile ( File file ) {
    __OutputFile_File = file;
}

/**
Return the string representation of the command.
@param parameters to include in the command
@return the string representation of the command
*/
public String toString ( PropList parameters ) {
	String [] parameterOrder = {
    	"URI",
    	"EncodeURI",
    	"RequestMethod",
    	"PayloadFile",
    	"HttpHeaders",
    	"ConnectTimeout",
    	"ReadTimeout",
    	"RetryMax",
    	"RetryWait",
    	"LocalFile",
    	"OutputProperty",
    	"IfHttpError",
    	"ResponseCodeProperty"
	};
	return this.toString(parameters, parameterOrder);
}

}