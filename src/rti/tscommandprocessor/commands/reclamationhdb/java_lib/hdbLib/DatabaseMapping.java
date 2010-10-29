//package java_lib.hdbLib;
// Provided by Dave King, Reclamation
// this class processes database mapping

// import classes stored in libraries

package rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.util.StringTokenizer;
import java.util.Vector;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ModelDateTime;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ReadNodeControlFile;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib.ReadNodeMap;


/**
* A class to process database node mapping
*/

public class DatabaseMapping
{
	// public Variables

	/**
	* A buffer for reading
	*/

	public BufferedReader readBuffer = null;

	/**
	* A variable to determine the end of the file.
	*/

	public boolean outOfData = true;

	/**
	* A variable to store contents of database information file.
	*/

	public Vector attributesList = new Vector (10, 10);

	public String cFile = null;

	/**
	* Database attributes
	*/

	public String sdtidField = null;			// site datatype id to be mapped to (site_datatype_id)
	public String sidField = null;
	public String dtField = null;
	public String sdtTable = null;
	public String objField = null;
	public String slotField = null;
	public String valuesTable = null;
	public String midField = null;
	public String dateField = null;
	public String valueField = null;
	public String secondSdtidField = null;			// site datatype id specified in control file (siteDatatypeId)
	public String sMapType = null;
	public String mapTable = null;
	public char mapType = 'T';
	public char cfType = 'E';

	String line = null;
	StringTokenizer st;

	// Non HDB Constructor - Constructor opens and stores database information file contents

	/**
	* Constructor opens and stores database information file contents
	* @param file Database information file.
	* @param logBuffer Dmi output log.
	*/

	public DatabaseMapping(boolean rwFlag, String dbInfoFile, PrintWriter logBuffer)
	{
		try
		{
			readBuffer = new BufferedReader(new FileReader (dbInfoFile));
			this.readContents(logBuffer);
			if (this.readStandardAttributes(logBuffer))
			{
				if (rwFlag)cfType = 'M';
				outOfData = false;
			}
			readBuffer.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open node control file " + dbInfoFile);
			logBuffer.println("Unable to open node control file " + dbInfoFile);
		}

	}	// end constructor

	// HDB Constructor with mapping to site_datatype_id

	/**
	* Constructor sets default values as a function of RiverWare involvement
	* @param logBuffer Dmi output log.
	* @param rwFlag RiverWare flag.
	*/

	public DatabaseMapping(PrintWriter logBuffer, boolean rwFlag)
	{
		if (rwFlag)
		{
			mapType = 'L';
			cfType = 'M';
		}
		else
		{
			mapType = 'J';
			cfType = 'E';
		}

		// set hdb tables and fields

		sdtidField = "site_datatype_id";
		sidField = "site_id";
		dtField = "datatype_id";
		sdtTable = "hdb_site_datatype";
		objField = "object_name";
		slotField = "data_name";
		midField = "model_id";
		outOfData = false;

	}	// end constructor

	// Original HDB Constructor with mapping to sid and dtid

	/**
	* Constructor sets default values as a function of RiverWare involvement
	* @param rwFlag RiverWare flag.
	* @param logBuffer Dmi output log.
	*/

	public DatabaseMapping(boolean rwFlag, PrintWriter logBuffer)
	{
		if (rwFlag)
		{
			mapType = 'A';
			cfType = 'M';
		}
		else
		{
			mapType = 'I';
			cfType = 'E';
		}

		// set hdb tables and fields

		sdtidField = "site_datatype_id";
		sidField = "site_id";
		dtField = "datatype_id";
		sdtTable = "hdb_site_datatype";
		objField = "object_name";
		slotField = "data_name";
		midField = "model_id";
		outOfData = false;

	}	// end constructor

	// RDB/HDB to HDB Constructor

	/**
	* Constructor sets default values for RDB to HDB case
	* @param logBuffer Dmi output log.
	*/

	public DatabaseMapping(PrintWriter logBuffer)
	{

		mapType = 'K';
		cfType = 'F';

		// set hdb tables and fields

		secondSdtidField = "site_datatype_id";
		sidField = "site_id";
		dtField = "datatype_id";
		sdtTable = "hdb_site_datatype";
		objField = "object_name";
		slotField = "data_name";
		midField = "model_id";
		outOfData = false;

	}	// end constructor

// method to read contents of file and store data in vector

	private void readContents(PrintWriter logBuffer)
	{
		String attribLine = null;
		try
		{
			while ((attribLine = readBuffer.readLine()) != null)
			{
				attributesList.addElement(attribLine);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to read contents of database information file.");
			logBuffer.println("Unable to read contents of database information file.");
		}
		return;
	}	// end method

// method to pick up standard attributes

	private boolean readStandardAttributes(PrintWriter logBuffer)
	{
		sdtidField = this.getAttribute("SDTID Field");
		if (sdtidField.equals(null))
		{
			System.out.println("Unable to determine site datatype id field.");
			return false;
		}
		valuesTable = this.getAttribute("Values Table");
		if (valuesTable.equals(null))
		{
			System.out.println("Unable to determine values table.");
			return false;
		}
		dateField = this.getAttribute("Date Field");
		if (dateField.equals(null))
		{
			System.out.println("Unable to determine date field.");
			return false;
		}
		valueField = this.getAttribute("Value Field");
		if (valueField.equals(null))
		{
			System.out.println("Unable to determine value field.");
			return false;
		}
		sMapType = this.getAttribute("Map Type");
		if (sMapType.equals(null))
		{
			System.out.println("Unable to determine map type.");
			return false;
		}

		// create a char version of map type

		if (sMapType.equalsIgnoreCase("T"))
		{
			mapType = 'T';
			cfType = 'E';
		}
		else if (sMapType.equalsIgnoreCase("R"))
		{
			mapType = 'R';
			cfType = 'E';
		}
		else if (sMapType.equalsIgnoreCase("S"))
		{
			mapType = 'S';
			cfType = 'F';
		}
		else if (sMapType.equalsIgnoreCase("U"))
		{
			mapType = 'U';
			cfType = 'E';
		}
		else if (sMapType.equalsIgnoreCase("V"))
		{
			mapType = 'V';
			cfType = 'E';
		}
		else if (sMapType.equalsIgnoreCase("Z"))
		{
			mapType = 'Z';
			cfType = 'M';
		}
		else
		{
			System.out.println("Map type " + sMapType + " is invalid.");
			logBuffer.println("Map type " + sMapType + " is invalid.");
			return false;
		}
		// System.out.println("Map type is '" + mapType + "' and control file type is '" + cfType + "'");
		return true;

	}	// end method

// method to get a specified attribute from the list

	public String getAttribute(String attribToRead)
	{
		String attribLine = null;
		String attribute = null;
		String attribValue = null;
		for (int i = 0; i < attributesList.size(); i++)
		{
			try
			{
				attribLine = (String) attributesList.elementAt(i);
				st = new StringTokenizer(attribLine, "\t");
				attribute = st.nextToken();
				attribValue = st.nextToken();
				if (attribToRead.equalsIgnoreCase(attribute))
				{
//					System.out.println("Attribute " + attribToRead + " value is " + attribValue + ".");
					return attribValue;
				}
			}
			catch (Exception e)
			{
				System.out.println("Unable to find attribute " + attribToRead + ".");
				return attribValue;
			}
		}
		System.out.println("Unable to find attribute " + attribToRead + ".");
		return attribValue;
	}	// end method

// method to process node mapping for a database dmi

	public ReadNodeMap readDatabaseMapping (String nodeMapFile, String controlFile, Connection ourConn, String dbType, PrintWriter logBuffer)
	{
		ReadNodeMap rnm = null;

		// resolve node mapping
		// test if first character of nodeMapFile is > 9
		// if so, assume that it really is a file name
		// otherwise, assume that it is model model id to use interal mapping

		// System.out.println("Node map file is " + nodeMapFile);
		// System.out.println("Control file is " + controlFile);
		// System.out.println("Reading mapping with map type '" + mapType + "' and control file type '" + cfType + "'");

		if (nodeMapFile.equalsIgnoreCase("GenericMapping"))
		{

//			For generic mapping, assume that controlFile spec is actually the internal mapping id

			ModelDateTime tDate = new ModelDateTime();
			cFile = "SCFile" + tDate.getYear() + tDate.getMonth() + tDate.getDay() + tDate.getMinute() + ".controlFile";
//			System.out.println("cFile is " + cFile);
			if (!ReadNodeControlFile.makeControlFile(ourConn, mapType, Integer.valueOf(controlFile).intValue(), logBuffer, cFile))
			{
				rnm.outOfData = true;
				return rnm;
			}
			controlFile = cFile;
			nodeMapFile = "ControlFileMapping";
		}

		if (((int) nodeMapFile.charAt(0)) > 57)
		{
			if (nodeMapFile.equalsIgnoreCase("ControlFileMapping"))
			{
				// call constructors that uses control file mapping

				switch (mapType)
				{
					case 'A': case 'a': case 'L': case 'l':
						if (cfType == 'M')
							rnm = new ReadNodeMap(ourConn, mapType, logBuffer, controlFile);
						else
							rnm = new ReadNodeMap(ourConn, controlFile, logBuffer, mapType);
						break;
					case 'I': case 'i': case 'J': case 'j':
						rnm = new ReadNodeMap(ourConn, controlFile, logBuffer, mapType);
						break;
					case 'R': case 'r':
						sdtTable = this.getAttribute("Site datatype Table");
						if (cfType == 'M')
							rnm = new ReadNodeMap(ourConn, dbType, mapType, logBuffer, controlFile, sdtTable,
										sidField, dtField, sdtidField);
						else
							rnm = new ReadNodeMap(ourConn, dbType, controlFile, logBuffer, mapType, sdtTable,
										sidField, dtField, sdtidField);
						break;
					case 'S': case 's':
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, controlFile, logBuffer, mapType, sdtTable,
									null, null, sdtidField);
						break;
					case 'T': case 't':
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, controlFile, logBuffer, mapType, sdtTable,
									sidField, dtField, sdtidField);
						break;
					case 'U': case 'u':
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, controlFile, logBuffer, mapType, sdtTable,
									sidField, dtField, sdtidField);
						break;
					case 'V': case 'v':
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, controlFile, logBuffer, mapType, sdtTable,
									sidField, dtField, sdtidField);
						break;
					case 'Z': case 'z':
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						if (cfType == 'M')
							rnm = new ReadNodeMap(ourConn, dbType, mapType, logBuffer, controlFile, sdtTable, sidField, dtField, sdtidField);
						else
							rnm = new ReadNodeMap(ourConn, dbType, controlFile, logBuffer, mapType, sdtTable, sidField, dtField, sdtidField);
						break;
					default:
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, controlFile, logBuffer, mapType, sdtTable,
									sidField, dtField, sdtidField);
						break;
				}
			}
			else
			{
					// call constructor that uses externally mapped rdb data

				switch (mapType)
				{
					case 'A': case 'a': case 'L': case 'l':
						rnm = new ReadNodeMap(ourConn, mapType, nodeMapFile, logBuffer);
						break;
					case 'I': case 'i': case 'J': case 'j':
						rnm = new ReadNodeMap(ourConn, mapType, nodeMapFile, logBuffer);
						break;
					case 'R': case 'r':
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, mapType, controlFile, logBuffer, sdtTable,
										sidField, dtField, sdtidField);
						break;
					case 'S': case 's':
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, mapType, nodeMapFile, logBuffer, sdtTable,
									null, null, sdtidField);
						break;
					case 'T': case 't':
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, mapType, nodeMapFile, logBuffer, sdtTable,
									sidField, dtField, sdtidField);
						break;
					case 'U': case 'u':
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, mapType, nodeMapFile, logBuffer, sdtTable,
									sidField, dtField, sdtidField);
						break;
					case 'V': case 'v':
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, mapType, nodeMapFile, logBuffer, sdtTable,
									sidField, dtField, sdtidField);
						break;
					case 'Z': case 'z':
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, mapType, nodeMapFile, logBuffer, sdtTable,
									sidField, dtField, sdtidField);
						break;
					default:
						sidField = this.getAttribute("Site id Field");
						dtField = this.getAttribute("Datatype id Field");
						sdtTable = this.getAttribute("Site datatype Table");
						rnm = new ReadNodeMap(ourConn, dbType, mapType, nodeMapFile, logBuffer, sdtTable,
									sidField, dtField, sdtidField);
						break;
				}
			}
		}
		else
		{
			// call constructors that uses internally mapped rdb data

			int modelId = Integer.valueOf(nodeMapFile).intValue();

			switch (mapType)
			{
				case 'A': case 'a': case 'L': case 'l':
					rnm = new ReadNodeMap(ourConn, mapType, Integer.valueOf(nodeMapFile).intValue(), logBuffer);
						break;
				case 'R': case 'r':
					if (modelId > 0)
					{
						mapTable = this.getAttribute("Map Table");
						midField = this.getAttribute("Model Id Field");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, modelId, logBuffer, mapTable,
								objField, slotField, sdtidField, midField);
					}
					else
					{
						mapTable = this.getAttribute("Map Table");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, logBuffer, mapTable,
								objField, slotField, sdtidField);
					}
					break;
				case 'S': case 's':
					if (modelId > 0)
					{
						mapTable = this.getAttribute("Map Table");
						secondSdtidField = this.getAttribute("Second SDTID Field");
						midField = this.getAttribute("Model Id Field");
						rnm = new ReadNodeMap(ourConn, mapType, modelId, logBuffer, mapTable,
								sdtidField, secondSdtidField, midField);
					}
					else
					{
						mapTable = this.getAttribute("Map Table");
						secondSdtidField = this.getAttribute("Second SDTID Field");
						rnm = new ReadNodeMap(ourConn, mapType, logBuffer, mapTable,
								sdtidField, secondSdtidField);
					}
					break;
				case 'T': case 't':
					if (modelId > 0)
					{
						mapTable = this.getAttribute("Map Table");
						midField = this.getAttribute("Model Id Field");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, modelId, logBuffer, mapTable,
								objField, slotField, sdtidField, midField);
					}
					else
					{
						mapTable = this.getAttribute("Map Table");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, logBuffer, mapTable,
								objField, slotField, sdtidField);
					}
					break;
				case 'V': case 'v':
					if (modelId > 0)
					{
						mapTable = this.getAttribute("Map Table");
						midField = this.getAttribute("Model Id Field");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, modelId, logBuffer, mapTable,
								objField, slotField, sdtidField, midField);
					}
					else
					{
						mapTable = this.getAttribute("Map Table");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, logBuffer, mapTable,
								objField, slotField, sdtidField);
					}
					break;
				case 'U': case 'u':
					if (modelId > 0)
					{
						mapTable = this.getAttribute("Map Table");
						midField = this.getAttribute("Model Id Field");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, modelId, logBuffer, mapTable,
								objField, slotField, sdtidField, midField);
					}
					else
					{
						mapTable = this.getAttribute("Map Table");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, logBuffer, mapTable,
								objField, slotField, sdtidField);
					}
					break;
				case 'Z': case 'z':
					if (modelId > 0)
					{
						mapTable = this.getAttribute("Map Table");
						midField = this.getAttribute("Model Id Field");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, modelId, logBuffer, mapTable,
								objField, slotField, sdtidField, midField);
					}
					else
					{
						mapTable = this.getAttribute("Map Table");
						objField = this.getAttribute("Object Field");
						slotField = this.getAttribute("Slot Field");
						rnm = new ReadNodeMap(ourConn, mapType, logBuffer, mapTable,
								objField, slotField, sdtidField);
					}
					break;
				default:
					rnm = new ReadNodeMap(ourConn, mapType, Integer.valueOf(nodeMapFile).intValue(), logBuffer);
					break;
			}
		}
		return rnm;
	}	// end method

}	// end class
