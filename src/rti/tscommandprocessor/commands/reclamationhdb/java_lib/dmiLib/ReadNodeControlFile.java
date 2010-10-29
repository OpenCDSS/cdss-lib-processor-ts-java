//package java_lib.dmiLib;
//Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.util.Vector;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib.HdbDmiUtils;

//package java_lib.dmiLib;
// Provided by Dave King, Reclamation
// this class reads non RiverWare control file

// import classes stored in libraries

/**
* A class to read non RiverWare control file
*/

public class ReadNodeControlFile

{
	// Public Variables

	/**
	* A buffer for reading
	*/

	public BufferedReader readBuffer = null;

	/**
	* A variable to determine the end of the file.
	*/

	public boolean outOfData = true;

	/**
	* A variable to store the control file user variables.
	*/

	public String restOfLine;

	/**
	* A variable to store the object name.
	*/

	public String object_name;

	/**
	* A variable to store the slot name.
	*/

	public String slot_name;

	/**
	* A variable to store the slot name.
	*/

	public String objectSlot;

	/**
	* A variable to store the object.slot - expects a period as a separator.
	*/

	public String stationName;

	/**
	* A variable to store the slot name.
	*/

	public String parameter;

	/**
	* A variable to store the site number.
	*/

	public int siteNumber;

	/**
	* A variable to store the parameter number.
	*/

	public int paramNumber;

	/**
	* A variable to store the dmi units.
	*/

	public int site_datatype_id;

	/**
	* A variable to store the site datatype id.
	*/

	public String dmiUnits;

	/**
	* A variable to store the dmi scale.
	*/

	public double dmiScale;

	/**
	* Vector to store singular list of stations
	*/

	public Vector stationsList = new Vector (10, 10);

	/**
	* Vector to store singular list of parameters
	*/

	public Vector parametersList = new Vector (10, 10);

	/**
	* Vector to store singular list of units
	*/

	public Vector unitsList = new Vector (10, 10);

	/**
	* Vector to store singular list of files
	*/

	public Vector filesList = new Vector (10, 10);

	String line;
	StringTokenizer st;

	// This constructor just opens file

	/**
	* Constructor to open file.
	* @param file Node control file name.
	* @param logBuffer Dmi output log.
	*/

	public ReadNodeControlFile(String file, PrintWriter logBuffer)
	{
		String controlFileName 	= System.getProperty("qsd")
								+ System.getProperty("file.separator")
								+ file;
		try
		{
			readBuffer = new BufferedReader(new FileReader (controlFileName));
			outOfData = false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open node control file " + controlFileName);
			logBuffer.println("Unable to open node control file " + controlFileName);
		}

	}	// end constructor

	// This constructor opens file for creating vectors of stations, parameters, and units

	/**
	* Constructor to open file for creating vectors of stations, parameters, and units.
	* @param logBuffer Dmi output log.
	* @param file Node control file name.
	*/

	public ReadNodeControlFile(PrintWriter logBuffer, String file)
	{
		String controlFileName 	= System.getProperty("qsd")
								+ System.getProperty("file.separator")
								+ file;
		try
		{
			readBuffer = new BufferedReader(new FileReader (controlFileName));
			outOfData = false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open node control file " + controlFileName);
			logBuffer.println("Unable to open node control file " + controlFileName);
		}

	}	// end constructor

	// read one line of file as a function of node specification type

	/**
	* Method to read one line of file as a function of node specification type.
	* @param controlCase Flag to determine type of control file.  Case A is for RiverWare DMI's - use ReadMetaControlFile class.  Case B is node specification as object, slot.  Case C is node specification as siteNumber, stationName.  Case D is node specification as siteNumber, paramNumber.  Case E is node specification as stationName, parameter.  Case F is node specification as site_datatype_id.  Case G is node specification as objectSlot.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public void readOneLine(char controlCase, PrintWriter logBuffer)
	{
		// System.out.println("Processing control file with " + controlCase);
		try
		{
			// read a line and parse it by controlCase

			line = readBuffer.readLine();
			if (line != null)
			{
				switch (controlCase)
				{
					// Case A is for RiverWare DMI's - use ReadMetaControlFile class

					case 'B': case 'b':
						this.readCaseBNodeSpec();
						break;
					case 'C': case 'c':
						this.readCaseCNodeSpec();
						break;
					case 'D': case 'd':
						this.readCaseDNodeSpec();
						break;
					case 'E': case 'e':
						this.readCaseENodeSpec();
						break;
					case 'F': case 'f':
						this.readCaseFNodeSpec();
						break;
					case 'G': case 'g':
						this.readCaseFNodeSpec();
						break;
					default:
						this.readCaseBNodeSpec();
						break;
				}
			}
			else
			{
				outOfData = true;
				readBuffer.close();
			}

		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
			System.out.println("DMI: Unable to read meta control file line.");
			logBuffer.println("DMI: Unable to read meta control file line.");
			outOfData = true;
		}

	}	// end method

	// Reads Case B node specification as object, slot

	private void readCaseBNodeSpec() throws IOException
	{
		outOfData = false;
		dmiUnits = null;
		dmiScale = 1.0;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		objectSlot = null;
		site_datatype_id = 0;

		// parse line

		st = new StringTokenizer(line, "\t");

		// object

		object_name = st.nextToken();

		// slot

		slot_name = st.nextToken();
		objectSlot = object_name + "." + slot_name;

		// remaining tokens

		restOfLine = st.nextToken();

		// scale

		dmiScale = ControlFileUtils.getScale(restOfLine);

		// units

		dmiUnits = ControlFileUtils.getUnits(restOfLine);

	}	// end method

	// Reads Case C node specification as siteNumber, stationName

	private void readCaseCNodeSpec() throws IOException
	{
		outOfData = false;
		dmiUnits = null;
		dmiScale = 1.0;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		objectSlot = null;
		site_datatype_id = 0;

		// parse line

		st = new StringTokenizer(line, "\t");

		// siteNumber

		siteNumber = Integer.valueOf(st.nextToken()).intValue();

		// stationName

		stationName = st.nextToken();

		String restOfLine = st.nextToken();

		// scale

		dmiScale = ControlFileUtils.getScale(restOfLine);

		// units

		dmiUnits = ControlFileUtils.getUnits(restOfLine);

	}	// end method

	// Reads Case D node specification as siteNumber, paramNumber

	private void readCaseDNodeSpec() throws IOException
	{
		outOfData = false;
		dmiUnits = null;
		dmiScale = 1.0;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		objectSlot = null;
		site_datatype_id = 0;

		// parse line

		st = new StringTokenizer(line, "\t");

		// siteNumber

		siteNumber = Integer.valueOf(st.nextToken()).intValue();

		// paramNumber

		paramNumber = Integer.valueOf(st.nextToken()).intValue();

		String restOfLine = st.nextToken();

		// scale

		dmiScale = ControlFileUtils.getScale(restOfLine);

		// units

		dmiUnits = ControlFileUtils.getUnits(restOfLine);

	}	// end method

	// Reads Case E node specification as stationName, parameter

	private void readCaseENodeSpec() throws IOException
	{
		outOfData = false;
		dmiUnits = null;
		dmiScale = 1.0;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		objectSlot = null;
		site_datatype_id = 0;

		// parse line

		st = new StringTokenizer(line, "\t");

		// stationName

		stationName = st.nextToken();

		// paramter

		parameter = st.nextToken();

		restOfLine = st.nextToken();

		// scale

		dmiScale = ControlFileUtils.getScale(restOfLine);

		// units

		dmiUnits = ControlFileUtils.getUnits(restOfLine);

	}	// end method

	// Reads Case F node specification as site_datatype_id

	private void readCaseFNodeSpec() throws IOException
	{
		outOfData = false;
		dmiUnits = null;
		dmiScale = 1.0;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		objectSlot = null;
		site_datatype_id = 0;

		// parse line

		st = new StringTokenizer(line, "\t");

		// siteNumber

		site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

		String restOfLine = st.nextToken();

		// scale

		dmiScale = ControlFileUtils.getScale(restOfLine);

		// units

		dmiUnits = ControlFileUtils.getUnits(restOfLine);

	}	// end method

	// Reads Case G node specification as object.slot

	private void readCaseGNodeSpec() throws IOException
	{
		outOfData = false;
		dmiUnits = null;
		dmiScale = 1.0;
		object_name = null;
		slot_name = null;
		stationName = null;
		parameter = null;
		siteNumber = 0;
		paramNumber = 0;
		objectSlot = null;
		site_datatype_id = 0;

		// parse line

		st = new StringTokenizer(line, "\t");

		// object.slot

		objectSlot = st.nextToken();

		// parse object and slot

		object_name = objectSlot.substring(0, line.indexOf("."));
		slot_name = objectSlot.substring((objectSlot.indexOf(".") + 1), line.indexOf(": "));

		// remaining tokens

		restOfLine = st.nextToken();

		// scale

		dmiScale = ControlFileUtils.getScale(restOfLine);

		// units

		dmiUnits = ControlFileUtils.getUnits(restOfLine);

	}	// end method

	// This method reads entire file and creates vectors of stations, parameters, and units

	/**
	* Method to read file and store singular lists of stations, parameters, and units.
	* @param fileType - control file type.
	* @param logBuffer Dmi output log.
	*/

	public boolean queueStationsParamsUnits(char controlCase, PrintWriter logBuffer,
			String dataFile)
	{
		String cFile = null;
		try
		{
			while((line = readBuffer.readLine()) != null)
			{
				switch (controlCase)
				{
					// Case A is for RiverWare DMI's - use ReadMetaControlFile class

					case 'B': case 'b':
						this.readCaseBNodeSpec();
						if (stationsList.indexOf(object_name) < 0)
						{
							stationsList.addElement(object_name);
							cFile = ControlFileUtils.getFilename(restOfLine);
							if(cFile != null && !cFile.equals(dataFile))
								filesList.addElement(cFile);
							else
								filesList.addElement(dataFile);
						}
						if (parametersList.indexOf(slot_name) < 0)
						{
							parametersList.addElement(slot_name);
							unitsList.addElement(dmiUnits);
						}
						break;
					case 'C': case 'c':
						this.readCaseCNodeSpec();
						if (stationsList.indexOf(new Integer(siteNumber)) < 0)
						{
							stationsList.addElement(new Integer(siteNumber));
							cFile = ControlFileUtils.getFilename(restOfLine);
							if(cFile != null && !cFile.equals(dataFile))
								filesList.addElement(cFile);
							else
								filesList.addElement(dataFile);
						}
						if (parametersList.indexOf(stationName) < 0)
						{
							parametersList.addElement(stationName);
							unitsList.addElement(dmiUnits);
						}
						break;
					case 'D': case 'd':
						this.readCaseDNodeSpec();
						if (stationsList.indexOf(new Integer(siteNumber)) < 0)
						{
							stationsList.addElement(new Integer(siteNumber));
							cFile = ControlFileUtils.getFilename(restOfLine);
							if(cFile != null && !cFile.equals(dataFile))
								filesList.addElement(cFile);
							else
								filesList.addElement(dataFile);
						}
						if (parametersList.indexOf(new Integer(paramNumber)) < 0)
						{
							parametersList.addElement(new Integer(paramNumber));
							unitsList.addElement(dmiUnits);
						}
					break;
					case 'E': case 'e':
						this.readCaseENodeSpec();
						if (stationsList.indexOf(stationName) < 0)
						{
							stationsList.addElement(stationName);
							cFile = ControlFileUtils.getFilename(restOfLine);
							if(cFile != null && !cFile.equals(dataFile))
								filesList.addElement(cFile);
							else
								filesList.addElement(dataFile);
						}
						if (parametersList.indexOf(parameter) < 0)
						{
							parametersList.addElement(parameter);
							unitsList.addElement(dmiUnits);
						}
						break;
					case 'F': case 'f':
						this.readCaseFNodeSpec();
						if (stationsList.indexOf(new Integer(site_datatype_id)) < 0)
						{
							stationsList.addElement(new Integer(site_datatype_id));
							parametersList.addElement(new Integer(site_datatype_id));
							unitsList.addElement(dmiUnits);
							cFile = ControlFileUtils.getFilename(restOfLine);
							if(cFile != null && !cFile.equals(dataFile))
								filesList.addElement(cFile);
							else
								filesList.addElement(dataFile);
						}
						break;
					case 'G': case 'g':
						this.readCaseGNodeSpec();
						if (stationsList.indexOf(objectSlot) < 0)
						{
							stationsList.addElement(object_name);
							cFile = ControlFileUtils.getFilename(restOfLine);
							if(cFile != null && !cFile.equals(dataFile))
								filesList.addElement(cFile);
							else
								filesList.addElement(dataFile);
						}
						if (parametersList.indexOf(slot_name) < 0)
						{
							parametersList.addElement(parameter);
							unitsList.addElement(dmiUnits);
						}
						break;
					default:
						this.readCaseBNodeSpec();
						if (stationsList.indexOf(object_name) < 0)
						{
							stationsList.addElement(object_name);
							cFile = ControlFileUtils.getFilename(restOfLine);
							if(cFile != null && !cFile.equals(dataFile))
								filesList.addElement(cFile);
							else
								filesList.addElement(dataFile);
						}
						if (parametersList.indexOf(slot_name) < 0)
						{
							parametersList.addElement(slot_name);
							unitsList.addElement(dmiUnits);
						}
						break;
				}
			}
			readBuffer.close();
			return true;
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
			System.out.println("DMI: Unable to read meta control file line.");
			logBuffer.println("DMI: Unable to read meta control file line.");
			outOfData = true;
			try
			{
				readBuffer.close();
			}
			catch (java.io.IOException c)
			{
				return false;
			}
			return false;
		}
	}	// end method

	/**
	* Method makeControlFile - create control file from HDB generic map
	* @param con JDBC connection.
	* @param mapType Flag for map type.
	* @param extId External data source id.
	* @param logBuffer Dmi output log.
	*/

	public static boolean makeControlFile(Connection ourConn, char mapType, int extId, PrintWriter logBuffer, String file)
	{
		String controlFileName 	= System.getProperty("qsd")
								+ System.getProperty("file.separator")
								+ file;
		try
		{
			// open dummy control file

			PrintWriter writeBuffer = new PrintWriter(new FileWriter (controlFileName));

			// query generic map

			String sqlCommand	= "select primary_site_code, primary_data_code, hdb_site_datatype_id "
								+ "from ref_ext_site_data_map where ext_data_source_id = " + extId
								+ " and is_active_y_n = 'Y'"
								+ " order by hdb_site_datatype_id";
			ResultSet rs = null;
			Statement stmt = null;

			try
			{
				int count = 0;
				stmt = ourConn.createStatement();
				rs = stmt.executeQuery(sqlCommand.toString());
				while (rs.next())
				{
					count++;

					// write node to control file

					switch (mapType)
					{
						case 'J': case 'j': case 'T': case 't':
							writeBuffer.println(rs.getString(1) + "\t" + rs.getString(2) + "\t"
									+ "scale=1.0 units='"
									+ HdbDmiUtils.getRiverWareUnitsGivenSiteDatatypeId(ourConn, rs.getInt(3))
									+ "' sdtid=" + rs.getString(3));
							break;
						case 'K': case 'k': case 'S': case 's':
							writeBuffer.println(rs.getString(3) + "\t"
									+ "scale=1.0 units='"
									+ HdbDmiUtils.getRiverWareUnitsGivenSiteDatatypeId(ourConn, rs.getInt(3))
									+ "' sdtid=" + rs.getString(1));
							break;
						case 'I': case 'i': case 'U': case 'u':
							writeBuffer.println(rs.getString(1) + "\t" + rs.getString(2) + "\t"
									+ "scale=1.0 units='"
									+ HdbDmiUtils.getRiverWareUnitsGivenSiteDatatypeId(ourConn, rs.getInt(3))
									+ "' sdtid=" + rs.getString(3));
							break;
						default:
							writeBuffer.println(rs.getString(1) + "\t" + rs.getString(2) + "\t"
									+ "scale=1.0 units='"
									+ HdbDmiUtils.getRiverWareUnitsGivenSiteDatatypeId(ourConn, rs.getInt(3))
									+ "' sdtid=" + rs.getString(3));
							break;
					}
				}
				rs.close();
				stmt.close();
				writeBuffer.close();
				if (count < 1)
				{
					System.out.println("DMI: Unable to create control file " + controlFileName);
					logBuffer.println("DMI: Unable to create control file " + controlFileName);
					return false;
				}
				else
				{
					return true;
				}
			}
			catch (SQLException e)
			{
				e.printStackTrace();
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				System.out.println("DMI: Unable to create control file " + controlFileName);
				logBuffer.println("DMI: Unable to create control file " + controlFileName);
				writeBuffer.close();
				return false;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("DMI: Unable to open control file " + controlFileName);
			logBuffer.println("DMI: Unable to open control file " + controlFileName);
			return false;
		}
	}	// end method
}	// end class
