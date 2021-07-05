// SendEmailMessage_Command - This class initializes, checks, and runs the SendEmailMessage() command.

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

package rti.tscommandprocessor.commands.email;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
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
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.PropList;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;


/**
This class initializes, checks, and runs the SendEmailMessage() command.
*/
public class SendEmailMessage_Command extends AbstractCommand implements Command
{

/**
Data members used for parameter values.
*/
protected final String _Ignore = "Ignore";
protected final String _Warn = "Warn";
protected final String _Fail = "Fail";
protected final String _JavaAPI = "JavaAPI";
protected final String _Sendmail = "Sendmail";
protected final String _WindowsMail = "WindowsMail";

protected String _SMTPConfigPath = System.getenv("APPDATA") + "\\tstool\\.smtp.cfg";

/**
Constructor.
*/
public SendEmailMessage_Command ()
{	super();
	setCommandName ( "SendEmailMessage" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String MailProgram = parameters.getValue("MailProgram");
	String To = parameters.getValue ( "To" );
	String Subject = parameters.getValue ( "Subject" );
	String Message0 = parameters.getValue ( "Message" );
	String MessageFile = parameters.getValue ( "MessageFile" );   
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	String SMTPServer = parameters.getValue ( "SMTPServer" );
	String SMTPAccount = parameters.getValue ( "SMTPAccount" );
	String SMTPPassword = parameters.getValue ( "SMTPPassword" );
	String warning = "";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
	
	// The existence of the file to append is not checked during initialization
	// because files may be created dynamically at runtime.
	// If the To field is not given, warn the user.
	if ( (To == null) || To.isEmpty() ) {
		message = "The \"To\" addresses must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the To addresses."));
	}
	// If the Subject is not given, warn the user.
	if ( (Subject == null) || Subject.isEmpty() ) {
		message = "The subject must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the subject."));
	}
	if ( (IfNotFound != null) && !IfNotFound.isEmpty() ) {
		if ( !IfNotFound.equalsIgnoreCase(_Ignore) && !IfNotFound.equalsIgnoreCase(_Warn)
		    && !IfNotFound.equalsIgnoreCase(_Fail) ) {
			message = "The IfNotFound parameter \"" + IfNotFound + "\" is invalid.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Specify the parameter as " + _Ignore + ", " + _Warn + " (default), or " +
					_Fail + "."));
		}
	}
	// If neither a message or message file is given, warn the user.
	if ( (Message0 == null) && (MessageFile == null)) {
		message = "The Message or Message file must be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify the message or message file."));
	}
	// If both a message and message file 
	if ( ((Message0 != null) && (MessageFile != null)) ) {
		message = "The Message and Message file cannot both be specified.";
		warning += "\n" + message;
		status.addToLog(CommandPhaseType.INITIALIZATION,
			new CommandLogRecord(CommandStatusType.FAILURE,
				message, "Specify either the message or message file."));
	}
	// JavaAPI (default) chosen and one of the SMTP text fields is empty
	if ( (MailProgram == null) || (MailProgram.equals(_JavaAPI)) ) {
		if (!SMTPPassword.equalsIgnoreCase("smtp.cfg")) {
			// Don't do anything, don't need to check if all three fields are null. ONLY if the password is smtp.cfg.
			message = "SMTPPassword must be smtp.cfg.";
			warning += "\n" + message;
			status.addToLog(CommandPhaseType.INITIALIZATION,
				new CommandLogRecord(CommandStatusType.FAILURE,
					message, "SMTPPassword must be smtp.cfg."));
		}
//		else if ( (SMTPServer == null) || (SMTPAccount == null) || (SMTPPassword == null) ) {
//			message = "All SMTP fields must be filled out when using JavaAPI";
//			warning += "\n" + message;
//			status.addToLog(CommandPhaseType.INITIALIZATION,
//				new CommandLogRecord(CommandStatusType.FAILURE,
//					message, "Specify either the message or message file."));
//		}
		
	}
	// Check for invalid parameters...
	List<String> validList = new ArrayList<String>(13);
	validList.add ( "MailProgram" );
	validList.add ( "From" );
	validList.add ( "To" );
	validList.add ( "CC" );
	validList.add ( "BCC" );
	validList.add ( "Subject" );
	validList.add ( "Message" );
	validList.add ( "MessageFile" );
	validList.add ( "AttachmentFiles" );
	validList.add ( "IfNotFound" );
	validList.add( "SMTPServer" );
	validList.add( "SMTPAccount" );
	validList.add( "SMTPPassword" );
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
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new SendEmailMessage_JDialog ( parent, this )).ok();
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException, CommandWarningException, CommandException
{	String routine = getClass().getSimpleName() + ".runCommand", message;
	int warning_level = 2;
	String command_tag = "" + command_number;
	int warning_count = 0;
	
	PropList parameters = getCommandParameters();
	
    CommandProcessor processor = getCommandProcessor();
    CommandPhaseType commandPhase = CommandPhaseType.RUN;
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
		status.clearLog(commandPhase);
	}
	
    String MailProgram = parameters.getValue( "MailProgram" );
    if ( (MailProgram == null) || (MailProgram.equals("")) ) {
    	MailProgram = _JavaAPI; // Default
    }
    String From = parameters.getValue ( "From" );
	String To = parameters.getValue ( "To" );
	String CC = parameters.getValue ( "CC" );
	String BCC = parameters.getValue ( "BCC" );
    String Subject = parameters.getValue ( "Subject" );
    String Message0 = parameters.getValue ( "Message" );
//    String MessageFile = parameters.getValue ( "MessageFile" );
    String AttachmentFiles = parameters.getValue ( "AttachmentFiles" );
	String IfNotFound = parameters.getValue ( "IfNotFound" );
	if ( (IfNotFound == null) || IfNotFound.equals("")) {
	    IfNotFound = _Warn; // Default
	}
	String[] SMTPParameters = new String[3];
	if (MailProgram.equals(_JavaAPI)) {
		SMTPParameters[0] = parameters.getValue("SMTPServer");
//		SMTPParameters[1] = parameters.getValue("SMTPAccount");
		SMTPParameters[2] = parameters.getValue("SMTPPassword");
		
//		System.out.println(IOUtil.verifyPathForOS(_SMTPConfigPath));
	}
	
	//String MessageFile_full = IOUtil.verifyPathForOS(
    //   IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
    //    	TSCommandProcessorUtil.expandParameterValue(processor,this,MessageFile) ) );
	
	// Check if AttachmentFiles is null. If not, then process.
	List<File> fileList = null;
	
	if (AttachmentFiles != null) {
		String AttachmentFiles_full = IOUtil.verifyPathForOS(
	        IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        	TSCommandProcessorUtil.expandParameterValue(processor,this,AttachmentFiles) ) );
		// Expand to a list of files...
		File f = new File(AttachmentFiles_full);
		String ext = null;
		fileList = new ArrayList<File>();
		if ( AttachmentFiles_full.indexOf("*") < 0 ) {
		    // Processing a single file
		    fileList.add(new File(AttachmentFiles_full));
		}
		else if ( f.getName().equals("*") ) {
		    // Process all files in folder
		    fileList = Arrays.asList(f.getParentFile().listFiles());
		}
		else if ( f.getName().startsWith("*.") ) {
		    // Process all files in the folder with the matching extension
		    ext = IOUtil.getFileExtension(f.getName());
		    // TODO SAM 2016-02-08 Need to enable parameter for case
		    fileList = IOUtil.getFilesMatchingPattern(f.getParent(),ext,false);
		}
		if ( fileList.size() == 0 ) {
		    message = "Unable to match any files using AttachmentFiles=\"" + AttachmentFiles + "\"";
		    if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
	            Message.printWarning ( warning_level,
	                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	            status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                message, "Verify that the input file(s) exist(s) at the time the command is run."));
	        }
	        else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
	            Message.printWarning ( warning_level,
	                MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	            status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
	                message, "Verify that the input file(s) exist(s) at the time the command is run."));
	        }
		}
		// Print warnings depending on what was picked from the drop-down list.
		for ( File file : fileList ) {
	    	if ( !file.exists() ) {
	            message = "Attachment file \"" + file + "\" does not exist.";
	            if ( IfNotFound.equalsIgnoreCase(_Fail) ) {
	                Message.printWarning ( warning_level,
	                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	                status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.FAILURE,
	                    message, "Verify that the attachment file exists at the time the command is run."));
	            }
	            else if ( IfNotFound.equalsIgnoreCase(_Warn) ) {
	                Message.printWarning ( warning_level,
	                    MessageUtil.formatMessageTag(command_tag,++warning_count), routine, message );
	                status.addToLog(CommandPhaseType.RUN, new CommandLogRecord(CommandStatusType.WARNING,
	                    message, "Verify that the attachment file exists at the time the command is run."));
	            }
	            else {
	                Message.printStatus( 2, routine, message + "  Ignoring.");
	            }
	    	}
		}
	}
	if ( warning_count > 0 ) {
		message = "There were " + warning_count + " warnings about command parameters.";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count), routine, message );
		throw new InvalidCommandParameterException ( message );
	}
	
	// Process the files.  Each input file is opened to scan the file.  The output file is opened once in append mode.
/*
	String OutputFile_full = IOUtil.verifyPathForOS(
	    IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),
	        TSCommandProcessorUtil.expandParameterValue(processor,this,OutputFile) ) );
	PrintWriter fout = null;
	try {
	    fout = new PrintWriter ( new FileOutputStream( OutputFile_full, true ) );
	}
	catch ( Exception e ) {
	    message = "Error opening the output file (" + e + ").";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandException ( message );
	}
	*/
	
	try {
		if (MailProgram.equals(_JavaAPI)) {
			sendEmailMessageViaJavaAPI ( To, From, CC, BCC, Subject, Message0, MailProgram, fileList, SMTPParameters );
		}
		else if (MailProgram.equals(_Sendmail)) {
			sendEmailMessageViaSendmail();
		}
		else if (MailProgram.equals(_WindowsMail)) {
			sendEmailMessageViaWindowsMail();
		}
		
	}
	catch ( Exception e ) {
		Message.printWarning ( 3, routine, e );
		message ="Unexpected error sending email message (" + e + ").";
		Message.printWarning ( warning_level, 
		MessageUtil.formatMessageTag(command_tag, ++warning_count),
		routine, message );
        status.addToLog ( commandPhase,
            new CommandLogRecord(CommandStatusType.FAILURE,
               message, "See the log file." ) );
		throw new CommandException ( message );
	}
	
    if ( warning_count > 0 ) {
        message = "There were " + warning_count + " warnings processing the command.";
        Message.printWarning ( warning_level,
            MessageUtil.formatMessageTag(
            command_tag, ++warning_count),
            routine,message);
        throw new CommandWarningException ( message );
    }

	status.refreshPhaseSeverity(CommandPhaseType.RUN,CommandStatusType.SUCCESS);
}

// See:  http://crunchify.com/java-mailapi-example-send-an-email-via-gmail-smtp/
/**
 * Create the email message to send.
 * @param to
 * @param cc
 * @param bcc
 * @param subject
 * @param message
 */
private MimeMessage createEmailMessage ( Session session, String from, String to, String cc, String bcc, String subject,
		String message, List<File> fileList )
throws AddressException, MessagingException {
	
	MimeMessage emailMessage = new MimeMessage(session);
	InternetAddress toAddress = new InternetAddress(to);
	InternetAddress fromAddress = new InternetAddress(from);
	
	emailMessage.setFrom(fromAddress);
	emailMessage.addRecipient(javax.mail.Message.RecipientType.TO, toAddress);
	emailMessage.setSubject(subject);
	
	// If any attachment files are present, send them along with the message
	if (fileList != null) {
		
		Multipart multipart = new MimeMultipart();
		BodyPart messageBodyPart = new MimeBodyPart();  
	    messageBodyPart.setText(message);
	    // Iterate over each attachment file and add them to the multipart object
		for (File file: fileList) {
			BodyPart attachmentBodyPart = new MimeBodyPart();
			
			DataSource source = new FileDataSource(file);  
			attachmentBodyPart.setDataHandler(new DataHandler(source));
		    // If forward or backward slash.
		    if (file.toString().lastIndexOf("\\") != -1) {
		    	attachmentBodyPart.setFileName(file.toString().substring(file.toString().lastIndexOf("\\") + 1));
		    } else {
		    	attachmentBodyPart.setFileName(file.toString().substring(file.toString().lastIndexOf("/") + 1));
		    }
		    // Add the attached file to the Multipart.
		    multipart.addBodyPart(attachmentBodyPart);
		}
		// Add the message body part to the Multipart
		multipart.addBodyPart(messageBodyPart);
		// Set the message body part and subsequent attachment body parts to the MimeMessage emailMessage.
		emailMessage.setContent(multipart);  
	} else {
		emailMessage.setText(message);
	}
	
	// Add any CC users to recipients.
	if (cc != null) {
		for (String ccRecipient: cc.split(",")) {
            emailMessage.addRecipient(javax.mail.Message.RecipientType.CC, new InternetAddress(ccRecipient));
        }
	}
	// Add any BCC users to recipients.
	if (bcc != null) {
		for (String bccRecipient: bcc.split(",")) {
            emailMessage.addRecipient(javax.mail.Message.RecipientType.BCC, new InternetAddress(bccRecipient));
        }
	}
	
	return emailMessage;
}

/**
 * Reads in the contents of the .mailpass file in the user's home directory with the correct email user name and
 * password. To be used as the user's credentials when trying to send an email.
 * @return An array of length two containing the user's email ID and password.
 */
private String[] readUserCredentials() throws FileNotFoundException {
	
	String fullSMTPConfigPath = IOUtil.verifyPathForOS(System.getenv("APPDATA") + "\\tstool\\.smtp.cfg");
	String[] credentials = new String[2];
	
	File myObj = new File(fullSMTPConfigPath);
    Scanner myReader = new Scanner(myObj);
    while (myReader.hasNextLine()) {
      String data = myReader.nextLine();
      credentials = data.split(":");
    }
    myReader.close();
	
	return credentials;
}

/**
 * Send an email message.
 * @param to
 * @param from
 * @param cc
 * @param bcc
 * @param subject
 * @param message
 * @param MailProgram
 * @param fileList
 * @param SMTPParameters
 * @throws AddressException
 * @throws MessagingException
 * @throws FileNotFoundException 
 */
private void sendEmailMessageViaJavaAPI ( String to, String from, String cc, String bcc, String subject, String message,
		String MailProgram, List<File> fileList, String[] SMTPParameters )
throws AddressException, MessagingException, FileNotFoundException {
	
	Properties props = new Properties();
	// Port 25 is the default port used, and is considered to not be a great option, as many firewalls will block
	// it. The following 2 ports are suggested, in order of importance. Port 587 - Uses STARTTLS. Port 465- Uses SMTPS
	// Set properties. See: http://crunchify.com/java-mailapi-example-send-an-email-via-gmail-smtp/
	props.put("mail.smtp.port", "587");
	props.put("mail.smtp.auth", "true");
	props.put("mail.smtp.starttls.enable", "true");
	
	// Password can only be smtp.cfg to read from the smtp.cfg file.
	if (SMTPParameters[2].equalsIgnoreCase("smtp.cfg")) {
		// Populate the 2 element sized array userCredentials with accountID and accountPassword
		String[] userCredentials = readUserCredentials();
		String accountId = userCredentials[0];
		// This is recommended to be an app-specific password for Google.
		String accountPassword = userCredentials[1];
		Session session = Session.getInstance(props, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(accountId, accountPassword);
			}
		});
		
		MimeMessage emailMessage = createEmailMessage ( session, from, to, cc, bcc, subject, message, fileList );
		Transport transport = session.getTransport("smtp");
		// Use the smtp server provided from the SMTP Server text field.
		transport.connect(SMTPParameters[0],accountId,accountPassword);
		transport.sendMessage(emailMessage,emailMessage.getAllRecipients());
		transport.close();
	}
	
	
	
}

/**
 * 
 */
private void sendEmailMessageViaSendmail() {
	
}

private void sendEmailMessageViaWindowsMail() {
	
}

/**
Return the string representation of the command.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
	String MailProgram = parameters.getValue("MailProgram");
	String From = parameters.getValue("From");
	String To = parameters.getValue("To");
	String CC = parameters.getValue("CC");
	String BCC = parameters.getValue("BCC");
	String Subject = parameters.getValue("Subject");
	String Message = parameters.getValue("Message");
	String MessageFile = parameters.getValue("MessageFile");
	String AttachmentFiles = parameters.getValue("AttachmentFiles");
	String IfNotFound = parameters.getValue("IfNotFound");
	String SMTPServer = parameters.getValue("SMTPServer");
	String SMTPAccount = parameters.getValue("SMTPAccount");
	String SMTPPassword = parameters.getValue("SMTPPassword");
	StringBuffer b = new StringBuffer ();
	
	if ( (MailProgram != null) && (MailProgram.length() > 0) ) {
		b.append ( "MailProgram=" + MailProgram );
	}
	if ( (From != null) && (From.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "From=\"" + From + "\"" );
	}
	if ( (To != null) && (To.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
		b.append ( "To=\"" + To + "\"" );
	}
    if ( (CC != null) && (CC.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CC=\"" + CC + "\"");
    }
    if ( (BCC != null) && (BCC.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "BCC=\"" + BCC + "\"" );
    }
    if ( (Subject != null) && (Subject.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Subject=\"" + Subject + "\"" );
    }
    if ( (Message != null) && (Message.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Message=\"" + Message + "\"" );
    }
    if ( (MessageFile != null) && (MessageFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MessageFile=\"" + MessageFile + "\"" );
    }
    if ( (AttachmentFiles != null) && (AttachmentFiles.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AttachmentFiles=\"" + AttachmentFiles + "\"" );
    }
	if ( (IfNotFound != null) && (IfNotFound.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "IfNotFound=" + IfNotFound );
	}
	if ( (SMTPServer != null) && (SMTPServer.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SMTPServer=\"" + SMTPServer + "\"" );
    }
	if ( (SMTPAccount != null) && (SMTPAccount.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SMTPAccount=\"" + SMTPAccount + "\"" );
    }
	if ( (SMTPPassword != null) && (SMTPPassword.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "SMTPPassword=\"" + SMTPPassword + "\"" );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
