package rti.tscommandprocessor.commands.custom;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import rti.tscommandprocessor.core.TSCommandProcessorUtil;

import RTi.TS.DayTS;
import RTi.TS.TS;
import RTi.TS.TSUtil;

import RTi.Util.IO.AbstractCommand;
import RTi.Util.Message.Message;
import RTi.Util.Message.MessageUtil;
import RTi.Util.Time.DateTime;
import RTi.Util.Time.TimeInterval;
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
import RTi.Util.IO.IOUtil;
import RTi.Util.IO.InvalidCommandParameterException;
import RTi.Util.IO.PropList;
import RTi.Util.String.StringUtil;

/**
<p>
This class initializes, checks, and runs the CustomCommand() command.
</p>
*/
public class CustomCommand_Command extends AbstractCommand implements Command, FileGenerator
{
    
/**
Values for VerboseMetrics parameter.
*/
protected final String _False = "False";
protected final String _True = "True";
    
/**
Tests being performed.
*/
private final int __TEST_VALUE = 0;
private final int __TEST_CHANGE = 1;
private final int __TEST_FINAL = 2;
private final int __TEST_SIZE = 3;

//Test names

String [] __TEST_NAMES = {
    "Value pass?",
    "Change pass?",
    "Overall pass?"
};

/**
Bins to receive the results of the analysis.
<pre>
__BIN_BAL_WEEK = Wednesday through Saturday of the first week
__BIN_WEEK_AHEAD = Monday through Saturday of the next week
__BIN_BAL_MONTH = Wednesday of this week through the last day of the month, but no Sundays.
__BIN_MONTH1 = Month + 1 (all days in month except Sundays)
__BIN_MONTH2 = Month + 2 (all days in month except Sundays)
__BIN_MONTH3 = Month + 3 (all days in month except Sundays)
<pre>
*/
private final int __BIN_BAL_WEEK = 0;
private final int __BIN_WEEK_AHEAD = 1;
private final int __BIN_BAL_MONTH = 2;
private final int __BIN_MONTH1 = 3;
private final int __BIN_MONTH2 = 4;
private final int __BIN_MONTH3 = 5;
private final int __BIN_SIZE = 6;

// Bin names, used in output and headings

private final String [] __BIN_NAMES = {
        "Bal week",
        "Week ahead",
        "Bal month",
        "Month + 1",
        "Month + 2",
        "Month + 3"
        };
// Used for headings
private final String [] __BIN_NAMES2 = {
        "(Wed-Sat)",
        "(Mon-Sat)",
        "(all but Sun)",
        "(all but Sun)",
        "(all but Sun)",
        "(all but Sun)"
        };

/**
Holiday dates to be omitted from bins.
*/
private final String [] __HOLIDAYS_TO_OMIT_FROM_BINS = {
        "2007-01-01",
        "2007-05-28",
        "2007-07-04",
        "2007-09-03",
        "2007-11-22",
        "2007-12-25",
        "2008-01-01",
        "2008-05-26",
        "2008-07-04",
        "2008-09-01",
        "2008-11-27",
        "2008-12-25"
};
private DateTime [] __HOLIDAYS_TO_OMIT_FROM_BINS_DateTime = null;
  
/**
Output file that is created by this command.
*/
private File __AdvanceAnalysisOutputFile_File = null;
private File __MetricsOutputFile_File = null;

/**
Constructor.
*/
public CustomCommand_Command ()
{	super();
	setCommandName ( "CustomCommand" );
}

/**
Check the command parameter for valid values, combination, etc.
@param parameters The parameters for the command.
@param command_tag an indicator to be used when printing messages, to allow a
cross-reference to the original commands.
@param warning_level The warning level to use when printing parse warnings
(recommended is 2 for initialization, and 1 for interactive command editor
dialogs).
*/
public void checkCommandParameters ( PropList parameters, String command_tag, int warning_level )
throws InvalidCommandParameterException
{	String warning = "";
	String routine = getCommandName() + ".checkCommandParameters";
	String message;

	CommandStatus status = getCommandStatus();
	status.clearLog(CommandPhaseType.INITIALIZATION);
    CommandProcessor processor = getCommandProcessor();
    
    String Title = parameters.getValue("Title");
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String STPDate = parameters.getValue ( "STPDate" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String AdvanceAnalysisOutputFile = parameters.getValue("AdvanceAnalysisOutputFile");
    String MetricsOutputFile = parameters.getValue ( "MetricsOutputFile" );
    
    if ( (Title == null) || (Title.length() == 0) ) {
        message = "A title must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a title for report headings." ) );
    }
    // FIXME SAM 2008-01-11 Need to implement checks like the following for all commands, as appropriate
    String special = "()";
    if ( (Title != null) && StringUtil.containsAny(Title,special,false) ) {
        message = "The title cannot contain any of the characters \"" + special + "\".";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a title for report headings." ) );
    }
    if ( (CurrentRForecastTSID == null) || (CurrentRForecastTSID.length() == 0) ) {
        message = "A CurrentRForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
    if ( (CurrentNForecastTSID == null) || (CurrentNForecastTSID.length() == 0) ) {
        message = "A CurrentNForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
    if ( (PreviousNForecastTSID == null) || (PreviousNForecastTSID.length() == 0) ) {
        message = "A PreviousNForecast time series identifier must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a time series identifier." ) );
    }
   
    if ( (STPDate != null) && !STPDate.equals("") ) {
        try {
            DateTime date = DateTime.parse(STPDate);
            if ( date.getWeekDay() != 2 ) {
                message = "The STP start date/time \"" + STPDate + "\" must be a Tuesday.";
                warning += "\n" + message;
                status.addToLog ( CommandPhaseType.INITIALIZATION,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                                message, "Specify an STP date that is a Tuesday." ) );
            }
        }
        catch ( Exception e ) {
            message = "The STP date \"" + STPDate + "\" is not a valid date.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Specify a valid date." ) );
        }
    }
    
    if ( (ChangeCriteria == null) || (ChangeCriteria.length() == 0) ) {
        message = "An change criteria must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value for the change criteria." ) );
    }
    else if ( !StringUtil.isDouble(ChangeCriteria ) ) {
        message = "The change criteria (" + ChangeCriteria + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a percent value for the change criteria." ) );
    }
    
    if ( (ValueCriteria == null) || (ValueCriteria.length() == 0) ) {
        message = "An value criteria must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a value for the change criteria." ) );
    }
    else if ( !StringUtil.isDouble(ValueCriteria ) ) {
        message = "The value criteria (" + ValueCriteria + ") is invalid.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a percent value for the value criteria." ) );
    }
    
    if ( (AdvanceAnalysisOutputFile == null) || (AdvanceAnalysisOutputFile.length() == 0) ) {
        message = "The advance analysis output file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify an advance analysis output file." ) );
    }
    else {
        String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, AdvanceAnalysisOutputFile));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does " +
                "not exist: \"" + adjusted_path + "\".";
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
            "    \"" + AdvanceAnalysisOutputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that output file and working directory paths are compatible." ) );
        }
    }
    
    if ( (MetricsOutputFile == null) || (MetricsOutputFile.length() == 0) ) {
        message = "The verification output file must be specified.";
        warning += "\n" + message;
        status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Specify a verification output file." ) );
    }
    else {
        String working_dir = null;
        try { Object o = processor.getPropContents ( "WorkingDir" );
            if ( o != null ) {
                working_dir = (String)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting WorkingDir from processor.";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Software error - report the problem to support." ) );
        }

        try {
            String adjusted_path = IOUtil.verifyPathForOS(IOUtil.adjustPath (working_dir, MetricsOutputFile));
            File f = new File ( adjusted_path );
            File f2 = new File ( f.getParent() );
            if ( !f2.exists() ) {
                message = "The output file parent directory does not exist: \"" + adjusted_path + "\".";
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
            "    \"" + MetricsOutputFile +
            "\"\ncannot be adjusted using the working directory:\n" +
            "    \"" + working_dir + "\".";
            warning += "\n" + message;
            status.addToLog ( CommandPhaseType.INITIALIZATION,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify that output file and working directory paths are compatible." ) );
        }
    }

	// Check for invalid parameters...
	Vector valid_Vector = new Vector();
	valid_Vector.add ( "Title" );
    valid_Vector.add ( "CurrentRForecastTSID" );
    valid_Vector.add ( "CurrentNForecastTSID" );
    valid_Vector.add ( "PreviousNForecastTSID" );
    valid_Vector.add ( "STPDate" );
	valid_Vector.add ( "ChangeCriteria" );
	valid_Vector.add ( "ValueCriteria" );
    valid_Vector.add ( "AdvanceAnalysisOutputFile" );
    valid_Vector.add ( "MetricsOutputFile" );
    valid_Vector.add ( "VerboseMetrics" );
	warning = TSCommandProcessorUtil.validateParameterNames ( valid_Vector, this, warning );

	if ( warning.length() > 0 ) {
		Message.printWarning ( warning_level,
		MessageUtil.formatMessageTag(command_tag,warning_level), routine, warning );
		throw new InvalidCommandParameterException ( warning );
	}
	status.refreshPhaseSeverity(CommandPhaseType.INITIALIZATION,CommandStatusType.SUCCESS);
}

/**
Create the Advance Analysis report (modified STP report)
*/
private void createAdvanceAnalysisReport ( String title, String AdvanceAnalysisOutputFile, DateTime STPDate_DateTime,
        TS Np, TS Nc, TS R,
        double [] Np_mean, double [] Nc_mean, double [] R_mean, DateTime [] bin_end_DateTime )
throws Exception
{
    //String comment = "#";
    CommandProcessor processor = getCommandProcessor();
    String AdvanceAnalysisOutputFile_full = IOUtil.verifyPathForOS(
            IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),AdvanceAnalysisOutputFile));
    PrintWriter fout = new PrintWriter ( new FileOutputStream ( AdvanceAnalysisOutputFile_full ) );
    //IOUtil.printCreatorHeader( fout, comment, 120, 0);
    String format_param = "%-12.12s";
    //String format_percent = "%16.2f";
    String format_text = "%16.16s";
    String format_value = "%16.0f";
    //String space = "  ";
    String delim = ","; // Delimiter between data columns (headers are saved in column 1)
    //String delim2 = ",,";
    String delim3 = ",,,";
    //String format_int = "%16d";
    /*
    fout.println ( comment );
    fout.println ( comment + "--------------------------------------------------------------------------------------");
    fout.println ( comment + " STP date:  " + STPDate_DateTime );
    fout.println ( comment + " Np period: " + Np_TS.getDate1() + " to " + Np_TS.getDate2());
    fout.println ( comment + " Nc period: " + Nc_TS.getDate1() + " to " + Nc_TS.getDate2());
    fout.println ( comment + " R period:  " + R_TS.getDate1() + " to " + R_TS.getDate2());
    fout.println ( comment );
    fout.println ( comment + " Units of output are mean " + R_TS.getDataUnits() + " over bin.");
    fout.println ( comment );
    fout.println ( comment + " Change criteria :        " +
            StringUtil.formatString(ChangeCriteria_double,"%.2f") + "%");
    fout.println ( comment + " Absolute value criteria: " +
            StringUtil.formatString(ValueCriteria_double,"%.1f") + " " + R_TS.getDataUnits() );
    fout.println ( comment );
    fout.println ( comment + "--------------------------------------------------------------------------------------");
    fout.println ( comment );
    */
    
    fout.println ( "" + delim3 );
    fout.println ( title + delim3 );
    fout.println ( "" + delim3 );
    fout.println ( "STP date:  " + STPDate_DateTime + delim3 );
    DateTime now = new DateTime ( DateTime.DATE_CURRENT );
    fout.println ( "Creation time:  " + now + delim3 );
    fout.println ( "" + delim3 );
    
    fout.println ( "" + delim3 );
    fout.println ( "Average CFS" + delim3 );
    fout.println ( "" + delim3 );
    
    fout.println (
            StringUtil.formatString("Bin",format_param) + delim +
            StringUtil.formatString("NWS Prev (Np)",format_text) + delim +
            StringUtil.formatString("RTi (R)",format_text) + delim +
            StringUtil.formatString("NWS Current (Nc)",format_text) );
           
    fout.println ( "" + delim3 );
    for ( int i = 0; i < __BIN_SIZE; i++ ) {
        fout.println (
                StringUtil.formatString(__BIN_NAMES[i],format_param) + delim +
                StringUtil.formatString(Np_mean[i],format_value) + delim +
                StringUtil.formatString(R_mean[i],format_value) + delim +
                StringUtil.formatString(Nc_mean[i],format_value) );
                
    }
    
    fout.println ( "" + delim3 );
    fout.println ( "Daily Data (CFS)" + delim3 );
    fout.println ( "" + delim3 );
    fout.println (
            StringUtil.formatString("Date",format_param ) + delim +
            StringUtil.formatString("NWS Prev (Np)",format_text) + delim +
            StringUtil.formatString("RTi (R)",format_text) + delim +
            StringUtil.formatString("NWS Current (Nc)",format_text) );

    fout.println ( "" + delim3 );
    
    DateTime end = null;
    if ( Nc != null ) {
        end = new DateTime(Nc.getDate2());
    }
    else {
        // Take from RTi forecast
        end = new DateTime(R.getDate2());
    }
    DateTime date = new DateTime(STPDate_DateTime);
    // Add one day since STP report is issued on Tuesday but first bin starts on following Wednesday
    date.addDay ( 1 );
    double Np_value;
    double R_value;
    double Nc_value;
    double missing = -999.0;
    for ( ; date.lessThanOrEqualTo(end); date.addDay(1) ) {
        // Allow any of the time series to be missing, in particular Nc will be missing during daily forecasts
        if ( Np == null ) {
            Np_value = missing;
        }
        else {
            Np_value = Np.getDataValue(date);
        }
        if ( R == null ) {
            R_value = missing;
        }
        else {
            R_value = R.getDataValue(date);
        }
        if ( Nc == null ) {
            Nc_value = missing;
        }
        else {
            Nc_value = Nc.getDataValue(date);
        }
        fout.println (
        StringUtil.formatString(date.toString(), format_param) + delim +
        StringUtil.formatString(Np_value, format_value) + delim +
        StringUtil.formatString(R_value, format_value) + delim +
        StringUtil.formatString(Nc_value, format_value) );
        // Brute force print blank lines after each bin
        for ( int j = 0; j < bin_end_DateTime.length; j++ ) {
            if ( (bin_end_DateTime[j] != null) && bin_end_DateTime[j].equals(date) ) {
                fout.println ( "" + delim3 );
                break;
            }
        }
    }
    
    fout.println ( "" + delim3 );
    fout.println ( "Generated by Riverside Technology inc." + delim3 );
    
    fout.close();
}

/**
Process the custom command.
*/
private int customCommand (
        String Title,
        DayTS R_TS,
        DayTS Nc_TS,
        DayTS Np_TS,
        DateTime STPDate_DateTime,
        double ChangeCriteria_double,
        double ValueCriteria_double,
        String AdvanceAnalysisOutputFile_full,
        String MetricsOutputFile_full,
        boolean VerboseMetrics_boolean,
        CommandStatus status )
{   String routine = "CustomCommand.customCommand";
    int warning_count = 0;
    String message;
    CommandPhaseType command_phase = CommandPhaseType.RUN;
    
    // Start on the forecast date and loop through the data to fill bins of data to fill the
    // following bins.

    // Single values used during iteration...
    
    double R_value = 0.0;
    double Nc_value = 0.0;
    double Np_value = 0.0;
    
    // Values in the bins, used to evaluate accuracy of forecast

    double [] R_total = new double[__BIN_SIZE];
    double [] Nc_total = new double[__BIN_SIZE];
    double [] Np_total = new double[__BIN_SIZE];
    double [] R_mean = new double[__BIN_SIZE];
    double [] Nc_mean = new double[__BIN_SIZE];
    double [] Np_mean = new double[__BIN_SIZE];
    // Count of not missing in each bin
    int [] R_notmissing = new int[__BIN_SIZE];
    int [] Nc_notmissing = new int[__BIN_SIZE];
    int [] Np_notmissing = new int[__BIN_SIZE];
    // Count of missing values in each bin
    int [] R_missing = new int[__BIN_SIZE];
    int [] Nc_missing = new int[__BIN_SIZE];
    int [] Np_missing = new int[__BIN_SIZE];
    // Also save the start and end of each bin so that time series ends can be checked
    DateTime [] bin_start_DateTime = new DateTime[__BIN_SIZE];
    DateTime [] bin_end_DateTime = new DateTime[__BIN_SIZE];
    
    for ( int i = 0; i < __BIN_SIZE; i++ ) {
        R_total[i] = 0;
        Nc_total[i] = 0;
        Np_total[i] = 0;
        R_mean[i] = Double.NaN;
        Nc_mean[i] = Double.NaN;
        Np_mean[i] = Double.NaN;
        R_missing[i] = 0;
        Nc_missing[i] = 0;
        Np_missing[i] = 0;
        R_notmissing[i] = 0;
        Nc_notmissing[i] = 0;
        Np_notmissing[i] = 0;
        bin_start_DateTime[i] = null;
        bin_end_DateTime[i] = null;
    }
    
    // Indicate which bin the iterator is in.
    
    boolean in_balweek = true;     // Always true to start
    boolean in_between_balweek_and_weekahead = false;
    boolean in_weekahead = false;
    boolean in_balmonth = true;    // Always true to start
    boolean in_month1 = false;
    boolean in_month2 = false;
    boolean in_month3 = false;
    
    // Day of week, used to determine some bins.
    
    int day;    // Day of month
    int day_of_week;    // See Date class, corresponding to values below
    final int day_sunday = 0;
    final int day_monday = 1;
    
    // Loop from the STP start to the end of available data.
    
    DateTime date = new DateTime(STPDate_DateTime);
    // Add one day to the date because the data to be processed start on Wednesday
    date.addDay ( 1 );
    DateTime end = null;
    if ( Nc_TS != null ) {
        end = new DateTime(Nc_TS.getDate2());
    }
    else {
        end = new DateTime(R_TS.getDate2());
    }
    Message.printStatus ( 2, routine, "Generating report starting on " + STPDate_DateTime + " through " + end );
    double missing = -999.0;    // Use this if time series are missing
    for ( int i = 0; date.lessThanOrEqualTo(end); date.addDay(1), i++ ) {
        // Determine which bin the values should go into...
        day = date.getDay();
        day_of_week = date.getWeekDay();
        Message.printStatus ( 2, routine, "Processing date " + date + " day of week " + day_of_week );
        // TODO SAM 2007-12-21 May need to move this below to avoid spurious messages at end of period
        // Get the current values corresponding to the date iterator
        if ( R_TS == null ) {
            R_value = missing;
        }
        else {
            R_value = R_TS.getDataValue ( date );
        }
        if ( R_TS.isDataMissing(R_value) ) {
            message = "CurrentRForecast(" + date + ") is missing.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        else if ( R_value <= 0.0 ) {
            message = "CurrentRForecast(" + date + ") is negative (" +
            R_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        if ( Nc_TS == null ) {
            Nc_value = missing;
        }
        else {
            Nc_value = Nc_TS.getDataValue ( date );
        }
        if ( (Nc_TS == null) || Nc_TS.isDataMissing(Nc_value) ) {
            message = "CurrentNForecast(" + date + ") is missing.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        else if ( Nc_value <= 0.0 ) {
            message = "CurrentNForecast(" + date + ") is negative (" +
            Nc_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        if ( Np_TS == null ) {
            Np_value = missing;
        }
        else {
            Np_value = Np_TS.getDataValue ( date );
        }
        if ( Np_TS.isDataMissing(Np_value) ) {
            message = "PreviousNForecast(" + date + ") is missing.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        else if ( Np_value <= 0.0 ) {
            message = "PreviousNForecast(" + date + ") is negative (" +
            Np_value + ") - will decrease total.";
            Message.printWarning( 3, routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.WARNING,
                            message, "Verify input time series." ) );
            ++warning_count;
        }
        // Adjust the bin that the value should go into, starting with figuring out the week.
        if ( i == 0 ) {
            // Always in this week with no further adjustments necessary
            in_balweek = true;
            bin_start_DateTime[__BIN_BAL_WEEK] = new DateTime(date);
        }
        else if ( in_balweek ) {
            // Switch to next week if Saturday and not the first day being processed
            if ( day_of_week == day_sunday ) {
                in_balweek = false;
                bin_end_DateTime[__BIN_BAL_WEEK] = new DateTime(date);
                bin_end_DateTime[__BIN_BAL_WEEK].addDay ( -1 );
                in_between_balweek_and_weekahead = true;
            }
        }
        else if ( in_between_balweek_and_weekahead ) {
            if ( day_of_week == day_monday ) {
                // Need to start "next week"...
                in_between_balweek_and_weekahead = false;
                bin_start_DateTime[__BIN_WEEK_AHEAD] = new DateTime(date);
                in_weekahead = true;
            }
        }
        else if ( in_weekahead ) {
            // Done with week when get to sunday
            if ( day_of_week == day_sunday ) {
                in_weekahead = false;
                bin_end_DateTime[__BIN_WEEK_AHEAD] = new DateTime(date);
                bin_end_DateTime[__BIN_WEEK_AHEAD].addDay ( -1 );
            }
        }
        // Now determine the monthly bin.  The "thismonth" bin can overlap the weeks, but includes
        // all days of the month (not just weekdays).
        if ( i == 0 ) {
            // Always in this month with no further adjustments necessary
            in_balmonth = true;
            bin_start_DateTime[__BIN_BAL_MONTH] = new DateTime(date);
        }
        else if ( in_balmonth ) {
            // Switch to next month if not the first day being processed and the day is 1
            if ( day == 1 ) {
                in_balmonth = false;
                bin_end_DateTime[__BIN_BAL_MONTH] = new DateTime(date);
                bin_end_DateTime[__BIN_BAL_MONTH].addDay ( -1 );
                in_month1 = true;
                bin_start_DateTime[__BIN_MONTH1] = new DateTime(date);
            }
        }
        else if ( in_month1 ) {
            if ( day == 1 ) {
                in_month1 = false;
                bin_end_DateTime[__BIN_MONTH1] = new DateTime(date);
                bin_end_DateTime[__BIN_MONTH1].addDay ( -1 );
                in_month2 = true;
                bin_start_DateTime[__BIN_MONTH2] = new DateTime(date);
            }
        }
        else if ( in_month2 ) {
            if ( day == 1 ) {
                in_month2 = false;
                bin_end_DateTime[__BIN_MONTH2] = new DateTime(date);
                bin_end_DateTime[__BIN_MONTH2].addDay ( -1 );
                in_month3 = true;
                bin_start_DateTime[__BIN_MONTH3] = new DateTime(date);
            }
        }
        else if ( in_month3 ) {
            if ( day == 1 ) {
                // Done processing...
                in_month3 = false;
                bin_end_DateTime[__BIN_MONTH3] = new DateTime(date);
                bin_end_DateTime[__BIN_MONTH3].addDay ( -1 );
            }
        }
        // Make sure that months have ends.  Leave as null for bins with no data.
        if ( date.equals(end) ) {
            for ( int ibin = 0; ibin < __BIN_SIZE; ibin++ ) {
                if ( (bin_start_DateTime[ibin] != null) && (bin_end_DateTime[ibin] == null) ) {
                    bin_end_DateTime[ibin] = new DateTime(end);
                }
            }
        }
        // Now actually do the processing
        // Weekly metrics...
        int bin_week = -1;
        if ( isHoliday(date) ) {
            // Just skip it
            Message.printStatus( 2, routine, "Not adding " + date + " to a week bin because it is a holiday." );
        }
        else if ( in_balweek ) {
            bin_week = __BIN_BAL_WEEK;
        }
        else if ( in_weekahead ) {
            bin_week = __BIN_WEEK_AHEAD;
        }
        if ( bin_week >= 0 ) {
            // Have a weekly bin to put data in so do it (if data is not missing).
            if ( Np_TS.isDataMissing(Np_value) ) {
                ++Np_missing[bin_week];
            }
            else {
                ++Np_notmissing[bin_week];
                Np_total[bin_week] += Np_value;
            }
            if ( (Nc_TS == null) || Nc_TS.isDataMissing(Nc_value) ) {
                ++Nc_missing[bin_week];
            }
            else {
                ++Nc_notmissing[bin_week];
                Nc_total[bin_week] += Nc_value;
            }
            if ( R_TS.isDataMissing(R_value) ) {
                ++R_missing[bin_week];
            }
            else {
                R_total[bin_week] += R_value;
                ++R_notmissing[bin_week];
            }
        }
        // Monthly metrics are computed separately...
        int bin_month = -1;
        if ( isHoliday(date) ) {
            // Just skip it
            Message.printStatus( 2, routine, "Not adding " + date + " to a month bin because it is a holiday." );
        }
        else if ( in_balmonth && (day_of_week != day_sunday) ) {   // All days except Sunday
            bin_month = __BIN_BAL_MONTH;
        }
        else if ( in_month1 && (day_of_week != day_sunday)) {
            bin_month = __BIN_MONTH1;
        }
        else if ( in_month2 && (day_of_week != day_sunday)) {
            bin_month = __BIN_MONTH2;
        }
        else if ( in_month3 && (day_of_week != day_sunday)) {
            bin_month = __BIN_MONTH3;
        }
        if ( bin_month >= 0 ) {
            // Have a monthly bin identified so put data in it (if data not missing).
            if ( Np_TS.isDataMissing(Np_value) ) {
                ++Np_missing[bin_month];
            }
            else {
                Np_total[bin_month] += Np_value;
                ++Np_notmissing[bin_month];
            }
            if ( (Nc_TS == null) || Nc_TS.isDataMissing(Nc_value) ) {
                ++Nc_missing[bin_month];
            }
            else {
                Nc_total[bin_month] += Nc_value;
                ++Nc_notmissing[bin_month];
            }
            if ( R_TS.isDataMissing(R_value) ) {
                ++R_missing[bin_month];
            }
            else {
                R_total[bin_month] += R_value;
                ++R_notmissing[bin_month];
            }
        }
    }
    
    // Process the means to convert totals to means..
    
    for ( int i = 0; i < __BIN_SIZE; i++ ) {
        if ( R_notmissing[i] > 0 ) {
            R_mean[i] = R_total[i]/R_notmissing[i];
        }
        if ( Nc_notmissing[i] > 0 ) {
            Nc_mean[i] = Nc_total[i]/Nc_notmissing[i];
        }
        if ( Np_notmissing[i] > 0 ) {
            Np_mean[i] = Np_total[i]/Np_notmissing[i];
        }
    }
    
    // Write the results with the pass fail results.
    
    PrintWriter fout = null;
    String comment = "#";
    String delim = ",";
    try {
         fout = new PrintWriter ( new FileOutputStream ( MetricsOutputFile_full ) );
         //IOUtil.printCreatorHeader( fout, comment, 120, 0);
         String format_param = "%-26.26s";
         String format_percent = "%16.2f";
         String format_text = "%16.16s";
         String format_value = "%16.0f";
         String format_int = "%16d";
         String delim6 = ",,,,,,";
         fout.println ( comment + delim6);
         fout.println ( comment + "--------------------------------------------------------------------------------------" + delim6 );
         fout.println ( comment + " " + Title + delim6 );
         fout.println ( comment );
         fout.println ( comment + " STP date:       " + STPDate_DateTime );
         DateTime now = new DateTime ( DateTime.DATE_CURRENT );
         fout.println ( comment + " Creation date:  " + now + delim6 );
         fout.println ( comment + " Np period:      " + Np_TS.getDate1() + " to " + Np_TS.getDate2() + delim6 );
         if ( Nc_TS == null ) {
             fout.println ( comment + " Nc period:      " + "NA" + delim6 );
         }
         else {
             fout.println ( comment + " Nc period:      " + Nc_TS.getDate1() + " to " + Nc_TS.getDate2() + delim6 );
         }
         fout.println ( comment + " R period:       " + R_TS.getDate1() + " to " + R_TS.getDate2() + delim6 );
         fout.println ( comment + delim6 );
         fout.println ( comment + " Units of output are mean " + R_TS.getDataUnits() + " over bin." + delim6 );
         fout.println ( comment + delim6 );
         fout.println ( comment + " Change criteria :        " +
                 StringUtil.formatString(ChangeCriteria_double,"%.2f") + "%" + delim6 );
         fout.println ( comment + " Absolute value criteria: " +
                 StringUtil.formatString(ValueCriteria_double,"%.1f") + " " + R_TS.getDataUnits() + delim6 );
         fout.println ( comment + delim6 );
         fout.println ( comment + "--------------------------------------------------------------------------------------"+ delim6);
         fout.println ( comment + delim6 );
         printMetricsReportDividerLine ( fout, format_param, delim, format_text, true );
         printMetricsReportResultsLine ( fout, "Parameter", format_param, delim, __BIN_NAMES, format_text );
         printMetricsReportResultsLine ( fout, "", format_param, delim, __BIN_NAMES2, format_text );
         printMetricsReportDividerLine ( fout, format_param, delim, format_text, true );
         printMetricsReportResultsLine ( fout, "Bin start", format_param, delim, bin_start_DateTime, format_value );
         printMetricsReportResultsLine ( fout, "Bin end", format_param, delim, bin_end_DateTime, format_value );
         printMetricsReportDividerLine ( fout, format_param, delim, format_text, true );
         // Previous N forecast values....
         printMetricsReportDataLine ( fout, "Previous NWS Forecast (Np)", format_param, delim, Np_mean, format_value );

         // R forecast...
         printMetricsReportDataLine ( fout, "Current RTi forecast (R)", format_param, delim, R_mean, format_value );

         // Current N forecast values....
         printMetricsReportDataLine ( fout, "Current NWS Forecast (Nc)", format_param, delim, Nc_mean, format_value );
         
         // R minus Nc, difference and percent difference of Nc values...
         double [] RNc_diff = new double[__BIN_SIZE];
         double [] RNc_diff_percent = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             if ( (Nc_missing[i] == 0) && (R_missing[i] == 0) ) {
                 RNc_diff[i] = R_mean[i] - Nc_mean[i];
                 RNc_diff_percent[i] = (RNc_diff[i]/Nc_mean[i])*100.0;
             }
         }
         
         // Difference and percent difference of Nc - Np values...
         double [] NcNp_diff = new double[__BIN_SIZE];
         double [] NcNp_diff_percent = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             if ( (Nc_missing[i] == 0) && (Np_missing[i] == 0) ) {
                 NcNp_diff[i] = Nc_mean[i] - Np_mean[i];
                 NcNp_diff_percent[i] = (NcNp_diff[i]/Np_mean[i])*100.0;
             }
         }
         
         // R minus Np, difference and percent, difference and percent difference of Np values...
         double [] RNp_diff = new double[__BIN_SIZE];
         double [] RNp_diff_percent = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             if ( (Np_missing[i] == 0) && (R_missing[i] == 0) ) {
                 RNp_diff[i] = R_mean[i] - Np_mean[i];
                 RNp_diff_percent[i] = (RNp_diff[i]/Np_mean[i])*100.0;
             }
         }
         
         // Value bound (high), based on Nc
         double [] value_high = new double[__BIN_SIZE];
         double [] value_low = new double[__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             if ( Nc_missing[i] == 0 ) {
                 value_high[i] = Nc_mean[i] + ValueCriteria_double;
                 value_low[i] = Nc_mean[i] - ValueCriteria_double;
             }
         }
         
         // Compute the change value and bounds
         
         double [] change = new double[__BIN_SIZE]; // Computed metric
         double [] change_delta = new double[__BIN_SIZE];   // Allowed delta on each side of abs(Nc - Np)
         double [] change_low = new double[__BIN_SIZE]; // Low bound on pass
         double [] change_high = new double[__BIN_SIZE]; // High bound on pass
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             if ( (Nc_missing[i] == 0) && (Np_missing[i] == 0) && (R_missing[i] == 0) ) {
                     change[i] = Math.abs((RNp_diff[i] - NcNp_diff[i])/NcNp_diff[i])*100.0;
                     change_delta[i] = Math.abs(NcNp_diff[i])*(ChangeCriteria_double/100.0);
                     change_low[i] = Nc_mean[i] - change_delta[i];
                     change_high[i] = Nc_mean[i] + change_delta[i];
             }
         }

         fout.println ( delim6 );
         printMetricsReportDataLine ( fout, "R - Nc", format_param, delim, RNc_diff, format_value );
         fout.println ( delim6 );
         
         printMetricsReportDataLine ( fout, "Nc - Np", format_param, delim, NcNp_diff, format_value );
         printMetricsReportDataLine ( fout, "Change Criteria", format_param, delim, change_delta, format_value );
         fout.println ( delim6 );
         
         // Print optional metrics for troubleshooting...
         
         if ( VerboseMetrics_boolean ) {
             printMetricsReportDataLine ( fout, "Nc - Np (% of Np)", format_param, delim, NcNp_diff_percent, format_percent );
             printMetricsReportDataLine ( fout, "R - Np", format_param, delim, RNp_diff, format_value );
             printMetricsReportDataLine ( fout, "R - Np (% of Np)", format_param, delim, RNp_diff_percent, format_percent );
             printMetricsReportDataLine ( fout, "Np # missing", format_param, delim, Np_missing, format_int );
             printMetricsReportDataLine ( fout, "Np # not missing", format_param, delim, Np_notmissing, format_int );
             printMetricsReportDataLine ( fout, "R # missing", format_param, delim, R_missing, format_int );
             printMetricsReportDataLine ( fout, "R # not missing", format_param, delim, R_notmissing, format_int );
             printMetricsReportDataLine ( fout, "Nc # missing", format_param, delim, Nc_missing, format_int );
             printMetricsReportDataLine ( fout, "Np # not missing", format_param, delim, Np_notmissing, format_int );
             printMetricsReportDataLine ( fout, "R - Nc (% of Nc)", format_param, delim, RNc_diff_percent, format_percent );
         }

         // Do the final tests, looping through each bin
         String [][] test_results = new String[__TEST_SIZE][__BIN_SIZE];
         for ( int i = 0; i < __BIN_SIZE; i++ ) {
             test ( test_results, i,
                     ValueCriteria_double, ChangeCriteria_double,
                     Np_mean[i], Nc_mean[i], R_mean[i], NcNp_diff[i], RNp_diff[i], RNc_diff[i],
                     change[i],
                     Np_missing[i], Nc_missing[i], R_missing[i],
                     Np_notmissing[i], Nc_notmissing[i], R_notmissing[i]);
         }
         
         // Print important results and whether pass

         printMetricsReportDividerLine ( fout, format_param, delim, format_text, true );
         printMetricsReportDataLine ( fout, "Value bound (high)", format_param, delim, value_high, format_value );
         printMetricsReportDataLine ( fout, "Current RTi forecast (R)", format_param, delim, R_mean, format_value );

         printMetricsReportDataLine ( fout, "Value bound (low)", format_param, delim, value_low, format_value );
         printMetricsReportResultsLine ( fout,
                 __TEST_NAMES[__TEST_VALUE], format_param, delim, test_results[__TEST_VALUE], format_text );
         printMetricsReportDividerLine ( fout, format_param, delim, format_text, false );
         printMetricsReportDataLine ( fout, "Change bound (high)", format_param, delim, change_high, format_value );
         printMetricsReportDataLine ( fout, "Current RTi forecast (R)", format_param, delim, R_mean, format_value );
         printMetricsReportDataLine ( fout, "Change bound (low)", format_param, delim, change_low, format_value );
         printMetricsReportResultsLine ( fout,
                 __TEST_NAMES[__TEST_CHANGE], format_param, delim, test_results[__TEST_CHANGE], format_text );
         printMetricsReportDividerLine ( fout, format_param, delim, format_text, true );
         printMetricsReportResultsLine (
                 fout, __TEST_NAMES[__TEST_FINAL], format_param, delim, test_results[__TEST_FINAL], format_text );
         printMetricsReportDividerLine ( fout, format_param, delim, format_text, true );
         
         fout.println ( delim6 );
         fout.println ( "Generated by Riverside Technology inc." + delim6 );
                 
         fout.close();
         // Save the output file name...
         setMetricsOutputFile ( new File(MetricsOutputFile_full));
         
         // Create the Advance Analysis report
         createAdvanceAnalysisReport ( Title, AdvanceAnalysisOutputFile_full, STPDate_DateTime,
                 Np_TS, Nc_TS, R_TS, Np_mean, Nc_mean, R_mean, bin_end_DateTime );
         setAdvanceAnalysisOutputFile ( new File(AdvanceAnalysisOutputFile_full));
    }
    catch ( Exception e ) {
        message = "Unexpected error processing data.";
        Message.printWarning( 3, routine, message );
        Message.printWarning ( 3, routine, e);
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }

    return warning_count;
}

/**
Edit the command.
@param parent The parent JFrame to which the command dialog will belong.
@return true if the command was edited (e.g., "OK" was pressed), and false if
not (e.g., "Cancel" was pressed.
*/
public boolean editCommand ( JFrame parent )
{	// The command will be modified if changed...
	return (new CustomCommand_JDialog ( parent, this )).ok();
}

/**
Return the list of files that were created by this command.
*/
public List getGeneratedFileList ()
{
    Vector list = new Vector();
    if ( getAdvanceAnalysisOutputFile() != null ) {
        list.addElement ( getAdvanceAnalysisOutputFile() );
    }
    if ( getMetricsOutputFile() != null ) {
        list.addElement ( getMetricsOutputFile() );
    }
    return list;
}

/**
Return the Advance Analysis output file generated by this file.  This method is used internally.
*/
private File getAdvanceAnalysisOutputFile ()
{
    return __AdvanceAnalysisOutputFile_File;
}

/**
Return the verification output file generated by this file.  This method is used internally.
*/
private File getMetricsOutputFile ()
{
    return __MetricsOutputFile_File;
}

/**
Determine whether the date is a holiday, in which case the value should not be
included in totals.
*/
private boolean isHoliday ( DateTime date )
{
    for ( int i = 0; i < __HOLIDAYS_TO_OMIT_FROM_BINS.length; i++ ) {
        if ( date.equals(__HOLIDAYS_TO_OMIT_FROM_BINS_DateTime[i])) {
            return true;
        }
    }
    return false;
}

// Use base class parseCommand()

/**
Print a line in the report.
@param fout PrintWriter to receive output.
@param param_title parameter title for first column.
@param format_param StringUtil.formatString() format for parameter column.
@param delim Delimiter between columns.
@param values Values to output.
@param format_values StringUtil.formtString() format for value columns.
*/
private void printMetricsReportDataLine ( PrintWriter fout, String param_title, String format_param,
        String delim, double [] values, String format_value )
{
    fout.println (
        StringUtil.formatString(param_title,format_param) + delim +
        StringUtil.formatString(values[__BIN_BAL_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_WEEK_AHEAD],format_value) + delim +
        StringUtil.formatString(values[__BIN_BAL_MONTH],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH1],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH2],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH3],format_value) );
}

/**
Print a line in the report.
@param fout PrintWriter to receive output.
@param param_title parameter title for first column.
@param format_param StringUtil.formatString() format for parameter column.
@param delim Delimiter between columns.
@param values Values to output.
@param format_values StringUtil.formtString() format for value columns.
*/
private void printMetricsReportDataLine ( PrintWriter fout, String param_title, String format_param,
        String delim, int [] values, String format_value )
{
    fout.println (
        StringUtil.formatString(param_title,format_param) + delim +
        StringUtil.formatString(values[__BIN_BAL_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_WEEK_AHEAD],format_value) + delim +
        StringUtil.formatString(values[__BIN_BAL_MONTH],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH1],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH2],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH3],format_value) );
}

/**
Print a line in the report.
@param fout PrintWriter to receive output.
@param format_param StringUtil.formatString() format for parameter column.
@param delim Delimiter between columns.
@param format_text StringUtil.formtString() format for text columns.
@param wide Indicates if wide line should be used.
*/
private void printMetricsReportDividerLine (
        PrintWriter fout, String format_param, String delim, String format_text, boolean wide )
{
    String thinline = "-----------------------------------------";   // Make long - will be truncated
    String wideline = "=========================================";   // Make long - will be truncated
    String divider = thinline;
    if ( wide ) {
        divider = wideline;
    }
    fout.println (
        StringUtil.formatString(divider,format_param) + delim +
        StringUtil.formatString(divider,format_text) + delim +
        StringUtil.formatString(divider,format_text) + delim +
        StringUtil.formatString(divider,format_text) + delim +
        StringUtil.formatString(divider,format_text) + delim +
        StringUtil.formatString(divider,format_text) + delim +
        StringUtil.formatString(divider,format_text) );
}

/**
Print a line in the report.
@param fout PrintWriter to receive output.
@param param_title parameter title for first column.
@param format_param StringUtil.formatString() format for parameter column.
@param delim Delimiter between columns.
@param values Values to output (test results strings).
@param format_values StringUtil.formtString() format for value columns.
*/
private void printMetricsReportResultsLine ( PrintWriter fout, String param_title, String format_param,
        String delim, Object [] values, String format_value )
{
    fout.println (
        StringUtil.formatString(param_title,format_param) + delim +
        StringUtil.formatString(values[__BIN_BAL_WEEK],format_value) + delim +
        StringUtil.formatString(values[__BIN_WEEK_AHEAD],format_value) + delim +
        StringUtil.formatString(values[__BIN_BAL_MONTH],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH1],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH2],format_value) + delim +
        StringUtil.formatString(values[__BIN_MONTH3],format_value) );
}

/**
Run the command.
@param command_number Command number in sequence.
@exception CommandWarningException Thrown if non-fatal warnings occur (the
command could produce some results).
@exception CommandException Thrown if fatal warnings occur (the command could
not produce output).
*/
public void runCommand ( int command_number )
throws InvalidCommandParameterException,
CommandWarningException, CommandException
{	String routine = "CustomCommand_Command.runCommandInternal", message;
	int warning_level = 2;
    int log_level = 3;  // Non-user warning level
	String command_tag = "" + command_number;
	int warning_count = 0;
	
    CommandPhaseType command_phase = CommandPhaseType.RUN;
    CommandProcessor processor = getCommandProcessor();
	CommandStatus status = getCommandStatus();
	status.clearLog(command_phase);
    
	PropList parameters = getCommandParameters();
	String Title = parameters.getValue("Title");
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String STPDate = parameters.getValue ( "STPDate" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String AdvanceAnalysisOutputFile = parameters.getValue("AdvanceAnalysisOutputFile");
    String MetricsOutputFile = parameters.getValue ( "MetricsOutputFile" );
    String VerboseMetrics = parameters.getValue ( "VerboseMetrics" );

    // Initialize the holiday dates to check
    
    __HOLIDAYS_TO_OMIT_FROM_BINS_DateTime = new DateTime[__HOLIDAYS_TO_OMIT_FROM_BINS.length];
    for ( int i = 0; i < __HOLIDAYS_TO_OMIT_FROM_BINS.length; i++ ) {
        try {
            __HOLIDAYS_TO_OMIT_FROM_BINS_DateTime[i] = DateTime.parse ( __HOLIDAYS_TO_OMIT_FROM_BINS[i] );
        }
        catch ( Exception e ) {
            message = "Error converting holiday string \"" + __HOLIDAYS_TO_OMIT_FROM_BINS[i] + "\" to DateTime.";
            Message.printWarning(log_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                            message, "Report the problem to software support." ) );
        }
    }
    // Get the time series to process.  The time series list is searched backwards until the first match...

    PropList request_params = new PropList ( "" );
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", CurrentRForecastTSID );
    CommandProcessorRequestResultsBean bean = null;
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + CurrentRForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    PropList bean_PropList = bean.getResultsPropList();
    Object o_TS = bean_PropList.getContents ( "TS");
    TS CurrentRForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + CurrentRForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        CurrentRForecastTSID_TS = (TS)o_TS;
    }
     
    if ( CurrentRForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + CurrentRForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    // CurrentNForecastTSID...
    
    request_params = new PropList ( "" );
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", CurrentNForecastTSID );
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + CurrentNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    o_TS = bean_PropList.getContents ( "TS");
    TS CurrentNForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + CurrentNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.WARNING,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." +
                        		"  OK for daily products." ) );
    }
    else {
        CurrentNForecastTSID_TS = (TS)o_TS;
    }
     
    if ( CurrentNForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + CurrentNForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.WARNING,
                message, "Verify the time series identifier.  A previous error may also cause this problem." +
                		"  OK for daily products." ) );
        // Don't throw an exception because the time series will be missing for daily forecasts
        //throw new CommandWarningException ( message );
    }
    
    // PreviousNForecastTSID...
    
    request_params.set ( "CommandTag", command_tag );
    request_params.set ( "TSID", PreviousNForecastTSID );
    try {
        bean = processor.processRequest( "GetTimeSeriesForTSID", request_params);
    }
    catch ( Exception e ) {
        message = "Error requesting GetTimeSeriesForTSID(TSID=\"" + PreviousNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Report the problem to software support." ) );
    }
    bean_PropList = bean.getResultsPropList();
    o_TS = bean_PropList.getContents ( "TS");
    TS PreviousNForecastTSID_TS = null;
    if ( o_TS == null ) {
        message = "Null TS requesting GetTimeSeriesForTSID(TSID=\"" + PreviousNForecastTSID +
        "\") from processor.";
        Message.printWarning(log_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
        status.addToLog ( command_phase,
                new CommandLogRecord(CommandStatusType.FAILURE,
                        message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
    }
    else {
        PreviousNForecastTSID_TS = (TS)o_TS;
    }
     
    if ( PreviousNForecastTSID_TS == null ) {
        message = "Unable to find time series to analyze using TSID \"" + PreviousNForecastTSID + "\".";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Verify the time series identifier.  A previous error may also cause this problem." ) );
        throw new CommandWarningException ( message );
    }
    
    // Check the intervals...
    
    if ( CurrentRForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "CurrentRForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    if ( (CurrentNForecastTSID_TS) != null &&
            CurrentNForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "CurrentNForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    if ( PreviousNForecastTSID_TS.getDataIntervalBase() != TimeInterval.DAY ) {
        message = "PreviousNForecast time series does not have interval of Day";
        Message.printWarning ( warning_level,
        MessageUtil.formatMessageTag(
        command_tag,++warning_count), routine, message );
        status.addToLog ( command_phase,
        new CommandLogRecord(CommandStatusType.FAILURE,
                message, "Specify a day interval time series." ) );
        throw new CommandWarningException ( message );
    }
    
    // STP date
    
    DateTime STPDate_DateTime = null;
    if ( (STPDate != null) && (STPDate.length() > 0) ) {
        request_params = new PropList ( "" );
        request_params.set ( "DateTime", STPDate );
        try {
            bean = processor.processRequest( "DateTime", request_params);
        }
        catch ( Exception e ) {
            message = "Error requesting DateTime(DateTime=" + STPDate + ") from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Report problem to software support." ) );
        }
        bean_PropList = bean.getResultsPropList();
        Object prop_contents = bean_PropList.getContents ( "DateTime" );
        if ( prop_contents == null ) {
            message = "Null value for DateTime(DateTime=" + STPDate + "\") returned from processor.";
            Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Verify that a valid InputStart string has been specified." ) );
        }
        else {
            STPDate_DateTime = (DateTime)prop_contents;
        }
    }
    else {  // Get from the processor...
        try {
            Object o = processor.getPropContents ( "STPDate" );
            if ( o != null ) {
                STPDate_DateTime = (DateTime)o;
            }
        }
        catch ( Exception e ) {
            message = "Error requesting STPDate from processor.";
            Message.printWarning(warning_level,
                    MessageUtil.formatMessageTag( command_tag, ++warning_count),
                    routine, message );
                status.addToLog ( command_phase,
                        new CommandLogRecord(CommandStatusType.FAILURE,
                           message, "Report problem to software support." ) );
        }
    }
    
    // Check units...
    
    Vector tslist = new Vector(3);
    tslist.addElement ( CurrentRForecastTSID_TS );
    if ( CurrentNForecastTSID_TS != null ) {
        tslist.addElement ( CurrentNForecastTSID_TS );
    }
    tslist.addElement ( PreviousNForecastTSID_TS );
    if ( !TSUtil.areUnitsCompatible(tslist,true) ) {
        message = "Units for time series are different.";
        Message.printWarning(warning_level,
                MessageUtil.formatMessageTag( command_tag, ++warning_count),
                routine, message );
            status.addToLog ( command_phase,
                    new CommandLogRecord(CommandStatusType.FAILURE,
                       message, "Verify that the time series units are the same." ) );
            throw new CommandException ( message );
    }
    
    // Parameters for the analysis...
    
    double ChangeCriteria_double = StringUtil.atod ( ChangeCriteria );
    double ValueCriteria_double = StringUtil.atod ( ValueCriteria );
    boolean VerboseMetrics_boolean = false;
    if ( (VerboseMetrics != null) && VerboseMetrics.equalsIgnoreCase("True")) {
        VerboseMetrics_boolean = true;
    }
    
	// Now try to process.
    
    try {
        String AdvanceAnalysisOutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),AdvanceAnalysisOutputFile));
        String MetricsOutputFile_full = IOUtil.verifyPathForOS(
                IOUtil.toAbsolutePath(TSCommandProcessorUtil.getWorkingDir(processor),MetricsOutputFile));
        warning_count += customCommand (
                Title,
                (DayTS)CurrentRForecastTSID_TS,
                (DayTS)CurrentNForecastTSID_TS,
                (DayTS)PreviousNForecastTSID_TS,
                STPDate_DateTime,
                ChangeCriteria_double,
                ValueCriteria_double,
                AdvanceAnalysisOutputFile_full,
                MetricsOutputFile_full,
                VerboseMetrics_boolean,
                status
                );
    }
    catch ( Exception e ) {
        message = "Unexpected error processing command";
        Message.printWarning ( warning_level, 
                MessageUtil.formatMessageTag(command_tag, ++warning_count),routine, message );
        Message.printWarning ( 3, routine, e );
        status.addToLog ( command_phase,
			new CommandLogRecord(CommandStatusType.FAILURE,
					message, "Check log file for details." ) );
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
	
	status.refreshPhaseSeverity(command_phase,CommandStatusType.SUCCESS);
}

/**
Set the Advance Analysis output file that is created by this command.  This is only used internally.
*/
private void setAdvanceAnalysisOutputFile ( File file )
{
    __AdvanceAnalysisOutputFile_File = file;
}

/**
Set the verification output file that is created by this command.  This is only used internally.
*/
private void setMetricsOutputFile ( File file )
{
    __MetricsOutputFile_File = file;
}

/**
Perform the test on the forecast to determine whether it passes or fails the criteria.
The returned values are "YES" (pass), "NO" (fail), or " - " (not applicable).
@param test_results Array that is filled with String test results.
@param bin The bin that is being processed.
@param ValueCriteria_double The value criteria threshold, in the units of the data.
@param ChangeCriteria_double The change criteria threshold, in the units of the data.
@param Np_mean Mean of previous NWS forecast for the bin.
@param Nc_mean Mean of current NWS forecast for the bin.
@param R_mean Mean of RTi forecast for the bin.
@param NcNp_diff (Nc - Np) for the bin.
@param RNp_diff (R - Np) for the bin.
@param RNc_diff (R - Nc) for the bin.
@param change Value of change metric.
@param Np_missing The number of Np missing values for the bin.
@param Nc_missing The number of Nc missing values for the bin.
@param R_missing The number of R missing values for the bin.
@param Np_notmissing The number of Np not missing values for the bin.
@param Nc_notmissing The number of Nc not missing values for the bin.
@param R_notmissing The number of R not missing values for the bin.
*/
private void test ( String [][] test_results, int bin,
        double ValueCriteria_double, double ChangeCriteria_double,
        double Np_mean, double Nc_mean, double R_mean, double NcNp_diff, double RNp_diff, double RNc_diff,
        double change,
        int Np_missing, int Nc_missing, int R_missing, int Np_notmissing, int Nc_notmissing, int R_notmissing )
{   String routine = "CustomCommand_test";
    // OK to divide by zero since shown as NaN in reports
    // Keep around to see if people wan
    //double NcNp_diff_percent = (NcNp_diff/Np_mean)*100.0;
    //double RNp_diff_percent = (RNp_diff/Np_mean)*100.0;
    //double RNc_diff_percent = (RNc_diff/Nc_mean)*100.0;

    String YES = "YES";
    String NO = "NO";
    String NA = "-";
    String NO_DATA = "NO DATA";
    // Initialize to unknown
    for ( int i = 0; i < __TEST_SIZE; i++ ) {
        test_results[i][bin] = NA;
    }

    // Value test
    if ( R_missing > 0 || Nc_missing > 0 || (R_notmissing == 0) || (Nc_notmissing == 0) ) {
        test_results[__TEST_VALUE][bin] = NO_DATA;
        Message.printStatus ( 2, routine, "Value test not enough data for bin " + __BIN_NAMES[bin] );
    }
    else if ( R_notmissing != Nc_notmissing ) {
        test_results[__TEST_VALUE][bin] = NA;
        Message.printStatus ( 2, routine, "Value test different number of nonmissing for bin " + __BIN_NAMES[bin] );
    }
    else if ( Math.abs(RNc_diff) <= ValueCriteria_double ) {
        // RTi forecast is within the magnitude tolerance 
        Message.printStatus ( 2, routine, "Value test passed for bin " + __BIN_NAMES[bin] );
        test_results[__TEST_VALUE][bin] = YES;
    }
    else {
        test_results[__TEST_VALUE][bin] = NO;
        Message.printStatus ( 2, routine, "Value test failed for bin " + __BIN_NAMES[bin] );
    }
    
    // Change test
    if ( (Np_missing > 0) || (Nc_missing > 0) || (R_missing > 0) ||
            (Np_notmissing == 0) || (Nc_notmissing == 0) || (R_notmissing == 0) ) {
        Message.printStatus ( 2, routine, "Change test not enough data for bin " + __BIN_NAMES[bin] );
        test_results[__TEST_CHANGE][bin] = NO_DATA;
    }
    else if ( (Np_notmissing != Nc_notmissing) || (Np_notmissing != R_notmissing) ) {
        Message.printStatus ( 2, routine, "Change test different number of nonmissing for bin " + __BIN_NAMES[bin] );
        test_results[__TEST_CHANGE][bin] = NA;
    }
    else if ( change <= ChangeCriteria_double ) {
        // RTi forecast is within the magnitude tolerance.
        Message.printStatus ( 2, routine, "Change test passed for bin " + __BIN_NAMES[bin] );
        test_results[__TEST_CHANGE][bin] = YES;
    }
    else {
        Message.printStatus ( 2, routine, "Change test failed for bin " + __BIN_NAMES[bin] );
        test_results[__TEST_CHANGE][bin] = NO;
    }
 
    // Final test - need to pass one of the above.
    if (    (test_results[__TEST_CHANGE][bin].equals(YES) ||
            test_results[__TEST_CHANGE][bin].equals(NO_DATA) ||
            test_results[__TEST_CHANGE][bin].equals(NA))
            ||
            (test_results[__TEST_VALUE][bin].equals(YES) ||
            test_results[__TEST_VALUE][bin].equals(NO_DATA) ||
            test_results[__TEST_VALUE][bin].equals(NA)) ) {
        // Overall success
        test_results[__TEST_FINAL][bin] = YES;
    }
    else {
        test_results[__TEST_FINAL][bin] = NO;
    }
}

/**
Return the string representation of the command.
@param parameters Command parameters as strings.
*/
public String toString ( PropList parameters )
{	if ( parameters == null ) {
		return getCommandName() + "()";
	}
    String Title = parameters.getValue("Title");
    String CurrentRForecastTSID = parameters.getValue("CurrentRForecastTSID");
    String CurrentNForecastTSID = parameters.getValue ( "CurrentNForecastTSID" );
    String PreviousNForecastTSID = parameters.getValue ( "PreviousNForecastTSID" );
    String STPDate = parameters.getValue ( "STPDate" );
    String ChangeCriteria  = parameters.getValue("ChangeCriteria");
    String ValueCriteria = parameters.getValue("ValueCriteria");
    String AdvanceAnalysisOutputFile = parameters.getValue("AdvanceAnalysisOutputFile");
    String MetricsOutputFile = parameters.getValue ( "MetricsOutputFile" );
    String VerboseMetrics = parameters.getValue ( "VerboseMetrics" );
    
	StringBuffer b = new StringBuffer ();
	if ( (Title != null) && (Title.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "Title=\"" + Title + "\"" );
    }
	if ( (CurrentRForecastTSID != null) && (CurrentRForecastTSID.length() > 0) ) {
		if ( b.length() > 0 ) {
			b.append ( "," );
		}
		b.append ( "CurrentRForecastTSID=\"" + CurrentRForecastTSID + "\"" );
	}
    if ( (CurrentNForecastTSID != null) && (CurrentNForecastTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "CurrentNForecastTSID=\"" + CurrentNForecastTSID + "\"" );
    }
    if ( (PreviousNForecastTSID != null) && (PreviousNForecastTSID.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "PreviousNForecastTSID=\"" + PreviousNForecastTSID + "\"" );
    }
    if ( (STPDate != null) && (STPDate.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "STPDate=\"" + STPDate + "\"" );
    }
    if ( (ChangeCriteria != null) && (ChangeCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ChangeCriteria=" + ChangeCriteria );
    }
    if ( (ValueCriteria != null) && (ValueCriteria.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "ValueCriteria=" + ValueCriteria );
    }
    if ( (AdvanceAnalysisOutputFile != null) && (AdvanceAnalysisOutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "AdvanceAnalysisOutputFile=\"" + AdvanceAnalysisOutputFile + "\"");
    }
    if ( (MetricsOutputFile != null) && (MetricsOutputFile.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "MetricsOutputFile=\"" + MetricsOutputFile + "\"");
    }
    if ( (VerboseMetrics != null) && (VerboseMetrics.length() > 0) ) {
        if ( b.length() > 0 ) {
            b.append ( "," );
        }
        b.append ( "VerboseMetrics=" + VerboseMetrics );
    }
	return getCommandName() + "(" + b.toString() + ")";
}

}
