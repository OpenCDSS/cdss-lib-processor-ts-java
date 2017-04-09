package rti.tscommandprocessor.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import RTi.TS.TS;
import RTi.TS.TSIdent;
import RTi.TS.TSSupplier;
import RTi.Util.IO.CommandProcessor;
import RTi.Util.IO.IOUtil;
import RTi.Util.Message.Message;
import RTi.Util.String.StringUtil;
import RTi.Util.Table.DataTable;
import RTi.Util.Time.DateTime;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.SimpleSequence;
import freemarker.template.Template;
import freemarker.template.Version;

/**
 * Instances of this class expand a time series product template
 * and then process time series products that are consistent with the product.
 * @author sam
 */
public class GraphTemplateProcessor implements TSSupplier {

	/**
	 * Template that will be used to do processing.
	 */
	private Template template = null;
	
	/**
	 * Template as list of String, for internal processing.
	 */
	private List<String> templateLines = null;
	
	/**
	 * List of time series that were processed when the template was expanded.
	 */
	private List<TS> tslist = new ArrayList<TS>();
	
	/**
	 * Constructor.
	 */
	public GraphTemplateProcessor ( File templateFile ) throws IOException {
		createTemplate ( templateFile );
	}
	
	/**
	 * Create the Freemarker template.
	 */
	private void createTemplate ( File templateFile ) throws IOException {
		String routine = getClass().getSimpleName() + ".createTemplate", message;
        // Call the FreeMarker API...
    	// TODO sam 2017-04-08 figure out whether can re-use a singleton
    	// Configuration is intended to be a shared singleton but templates can exist in many folders.
        Configuration config = new Configuration(new Version(2,3,0));
        // TODO SAM 2009-10-07 Not sure what configuration is needed for TSTool since most
        // templates will be located with command files and user data
        //config.setSharedVariable("shared", "avoid global variables");
        // See comment below on why this is used.
        config.setSharedVariable("normalizeNewlines", new freemarker.template.utility.NormalizeNewlines());
        config.setTemplateLoader(new FileTemplateLoader(new File(".")));

        // In some apps, use config to load templates as it provides caching
        //Template template = config.getTemplate("some-template.ftl");

        // Manipulate the template file into an in-memory string so it can be manipulated...
        StringBuffer b = new StringBuffer();
        // Prepend any extra FreeMarker content that should be handled transparently.
        // "normalizeNewlines" is used to ensure that output has line breaks consistent with the OS (e.g., so that
        // results can be edited in Notepad on Windows).
        String nl = System.getProperty("line.separator");
        b.append("<@normalizeNewlines>" + nl );
        this.templateLines = new ArrayList<String>();
        if ( templateFile != null ) {
            this.templateLines = IOUtil.fileToStringList(templateFile.getAbsolutePath());
        }
        b.append(StringUtil.toString(this.templateLines,nl));
        b.append(nl + "</@normalizeNewlines>" );
        try {
            this.template = new Template("template", new StringReader(b.toString()), config);
        }
        catch ( Exception e1 ) {
            message = "Freemarker error expanding command template file \"" + templateFile +
                "\" + (" + e1 + ") template text (with internal inserts at ends) =" + nl +
                formatTemplateForWarning(this.templateLines,nl);
            Message.printWarning ( 2, routine, message );
            Message.printWarning ( 3, routine, e1 );
        }
	}
	
	/**
	 * Expand the template given a model.
	 * @param tslist list of time series available to the template
	 * @param processor1 main command processor, for example original TSTool command processor
	 * @param processor2 secondary command processor, for example if the template contains TemplatePreprocessCommandFile
	 * @param outputFile output file for the expanded template
	 */
	public void expandTemplate ( List<TS> tslist, CommandProcessor processor1, CommandProcessor processor2, File outputFile )
		throws FileNotFoundException {
		this.tslist = tslist;
		String message, routine = getClass().getSimpleName() + ".expandTemplate";
        // Expand the template to the output file
        FileOutputStream fos = new FileOutputStream( outputFile.getAbsolutePath() );
        PrintWriter out = new PrintWriter ( fos );
        CommandProcessor [] processorArray = new CommandProcessor[2];
        processorArray[0] = processor1;
        processorArray[1] = processor2;
        try {
            Map<String,Object> model = new HashMap<String,Object>();
            for ( int iproc = 0; iproc < 2; iproc++ ) {
            	CommandProcessor processor = processorArray[iproc];
            	if ( processor != null ) {
		            if ( processor instanceof TSCommandProcessor ) {
		                // Add properties from the processor
		            	TSCommandProcessor tsprocessor = (TSCommandProcessor)processor;
		                Collection<String> propertyNames = tsprocessor.getPropertyNameList(true,true);
		                for ( String propertyName : propertyNames ) {
		                    model.put(propertyName, tsprocessor.getPropContents(propertyName) );
		                }
		                // Add single column tables from the processor, using the table ID as the object key
		                boolean useProcessorTables = false;
		                if ( useProcessorTables ) {
		                    @SuppressWarnings("unchecked")
							List<DataTable> tables = (List<DataTable>)tsprocessor.getPropContents ( "TableResultsList" );
		                    Object tableVal;
		                    for ( DataTable table: tables ) {
		                        if ( table.getNumberOfFields() == 1 ) {
		                            // One-column table so add as a hash (list) property in the data model
		                            int numRecords = table.getNumberOfRecords();
		                            SimpleSequence list = new SimpleSequence();
		                            for ( int irec = 0; irec < numRecords; irec++ ) {
		                                // Check for null because this fouls up the template
		                                tableVal = table.getFieldValue(irec, 0);
		                                if ( tableVal == null ) {
		                                    tableVal = "";
		                                }
		                                list.add ( tableVal );
		                            }
		                            if ( Message.isDebugOn ) {
		                                Message.printStatus(2, routine, "Passing 1-column table \"" + table.getTableID() +
		                                    "\" (" + numRecords + " rows) to template model.");
		                            }
		                            model.put(table.getTableID(), list );
		                        }
		                    }
		                }
		            }
	            }
            }
            // Always add one-column tables for "TemplateTSIDList" and "TemplateTSAliasList", for use in the template
            List<String> TSIDList = new ArrayList<String>();
            List<String> TSIDShortList = new ArrayList<String>();
            List<String> TSAliasList = new ArrayList<String>();
            List<String> TSDescriptionList = new ArrayList<String>();
            List<String> TSLocationIDList = new ArrayList<String>();
            List<String> TSUnitsList = new ArrayList<String>();
            for ( TS ts : tslist ) {
            	TSIDList.add(ts.getIdentifierString() ); // Includes input type or datastore
            	TSIDShortList.add(ts.getIdentifier().toString(false) ); // No input type or datastore (just in memor TSID)
            	TSAliasList.add(ts.getAlias() );
            	TSLocationIDList.add(ts.getLocation() );
            	TSDescriptionList.add(ts.getDescription() );
            	TSUnitsList.add(ts.getDataUnits() );
            }
            model.put("TemplateTSIDList", TSIDList);
            model.put("TemplateTSIDShortList", TSIDShortList);
            model.put("TemplateTSAliasList", TSAliasList);
            model.put("TemplateTSLocationIDList", TSLocationIDList);
            model.put("TemplateTSDescriptionList", TSDescriptionList);
            model.put("TemplateTSUnitsList", TSUnitsList);
            // Now process the template with the model to create the output file.
            template.process (model, out);
        }
        catch ( Exception e ) {
            String nl = System.getProperty("line.separator");
            message = "Freemarker error expanding command template file \"" + outputFile.getAbsolutePath() +
                "\" + (" + e + ") template text (with internal inserts at ends) =\n" +
                formatTemplateForWarning(templateLines,nl);
            Message.printWarning ( 2, routine, message );
            Message.printWarning ( 3, routine, e );
            Message.printWarning ( 1, routine, "Error processing the graph template.  See the log file for more information." );
        }
        finally {
            out.close();
        }
	}
	
	/**
	Format the template for a warning message.  Add line numbers before.
	*/
	private StringBuffer formatTemplateForWarning ( List<String> templateLines, String nl )
	{   StringBuffer templateFormatted = new StringBuffer();
	    new StringBuffer();
	    int lineNumber = 1;
	    // Don't use space after number because HTML viewer may split and make harder to read
	    templateFormatted.append ( StringUtil.formatString(lineNumber++,"%d") + ":<@normalizeNewlines>" + nl );
	    for ( String line : templateLines ) {
	        templateFormatted.append ( StringUtil.formatString(lineNumber++,"%d") + ":" + line + nl );
	    }
	    templateFormatted.append ( StringUtil.formatString(lineNumber,"%d") + ":</@normalizeNewlines>" + nl );
	    return templateFormatted;
	}

	// === Start TSSupplier methods ===
	
	/**
	Return the name of the TSSupplier.  This is used for messages.
	*/
	public String getTSSupplierName() {
		return "GraphTemplateProcessor";
	}

	/**
	Read a time series given a time series identifier string.  The string may be
	a file name if the time series are stored in files, or may be a true identifier
	string if the time series is stored in a database.  The specified period is
	read.  The data are converted to the requested units.
	@param tsidentString Time series identifier or file name to read.
	@param date1 First date to query.  If specified as null the entire period will be read.
	@param date2 Last date to query.  If specified as null the entire period will be read.
	@param reqUnits Requested units to return data.  If specified as null or an
	empty string the units will not be converted.
	@param readData if true, the data will be read.  If false, only the time series header will be read.
	@return Time series of appropriate type (e.g., MonthTS, HourTS).
	@exception Exception if an error occurs during the read.
	*/
	public TS readTimeSeries ( String tsidentString, DateTime date1, DateTime date2,
		String reqUnits, boolean readData ) throws Exception {
		// Loop through and find the time series that matches the tsidentString
		for ( TS ts : this.tslist ) {
			if ( ts.getAlias().equalsIgnoreCase(tsidentString) ) {
				// Match alias
				return ts;
			}
			else if ( ts.getIdentifierString().equalsIgnoreCase(tsidentString) ) {
				// Match file TSID with datastore/input type
				return ts;
			}
			else if ( ts.getIdentifier().toString(false).equalsIgnoreCase(tsidentString) ) {
				// Match without datastore/input type
				return ts;
			}
		}
		return null;
	}

	/**
	Read a time series given an existing time series and a file name.
	The specified period is read.
	The data are converted to the requested units.
	@param req_ts Requested time series to fill.  If null, return a new time series.
	If not null, all data are reset, except for the identifier, which is assumed
	to have been set in the calling code.  This can be used to query a single
	time series from a file that contains multiple time series.
	@param fname File name to read.
	@param date1 First date to query.  If specified as null the entire period will
	be read.
	@param date2 Last date to query.  If specified as null the entire period will
	be read.
	@param req_units Requested units to return data.  If specified as null or an
	empty string the units will not be converted.
	@param read_data if true, the data will be read.  If false, only the time series
	header will be read.
	@return Time series of appropriate type (e.g., MonthTS, HourTS).
	@exception Exception if an error occurs during the read.
	*/
	public TS readTimeSeries (	TS req_ts, String fname,
						DateTime date1, DateTime date2,
						String req_units,
						boolean read_data ) throws Exception {
		return null;
	}

	/**
	Read a time series list from a file (this is typically used used where a time
	series file can contain one or more time series).
	The specified period is
	read.  The data are converted to the requested units.
	@param fname File to read.
	@param date1 First date to query.  If specified as null the entire period will
	be read.
	@param date2 Last date to query.  If specified as null the entire period will
	be read.
	@param req_units Requested units to return data.  If specified as null or an
	empty string the units will not be converted.
	@param read_data if true, the data will be read.  If false, only the time series
	header will be read.
	@return List of time series of appropriate type (e.g., MonthTS, HourTS).
	@exception Exception if an error occurs during the read.
	*/
	public List<TS> readTimeSeriesList ( String fname,
							DateTime date1, DateTime date2,
							String req_units,
							boolean read_data ) throws Exception {
		return null;
	}

	/**
	Read a time series list from a file or database using the time series identifier
	information as a query pattern.
	The specified period is read.  The data are converted to the requested units.
	@param tsident A TSIdent instance that indicates which time series to query.
	If the identifier parts are empty, they will be ignored in the selection.  If
	set to "*", then any time series identifier matching the field will be selected.
	If set to a literal string, the identifier field must match exactly to be selected.
	@param fname File to read.
	@param date1 First date to query.  If specified as null the entire period will be read.
	@param date2 Last date to query.  If specified as null the entire period will be read.
	@param req_units Requested units to return data.  If specified as null or an
	empty string the units will not be converted.
	@param read_data if true, the data will be read.  If false, only the time series header will be read.
	@return List of time series of appropriate type (e.g., MonthTS, HourTS).
	@exception Exception if an error occurs during the read.
	*/
	public List<TS> readTimeSeriesList ( TSIdent tsident, String fname,
		DateTime date1, DateTime date2, String req_units, boolean read_data ) throws Exception {
		// Loop through the time series in the list and find the matching time series
		return null;
	}
	
	// === End TSSupplier methods ===
}