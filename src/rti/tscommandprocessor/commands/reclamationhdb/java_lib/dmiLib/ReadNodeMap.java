//package java_lib.dmiLib;
//Provided by Dave King, Reclamation
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib;

import rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib.HdbDmiUtils;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib.RdbDmiUtils;
import rti.tscommandprocessor.commands.reclamationhdb.java_lib.hdbLib.VerifyRecords;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.StringTokenizer;
import java.util.Vector;

//package java_lib.dmiLib;
// Provided by Dave King, Reclamation
// Class to process node mappings.

// import classes stored in libraries

/**
* Class to process node mappings.
*/

public class ReadNodeMap
{

	// Public Variables

	/**
	* A variable to determine if end of file.
	*/

	public boolean outOfData = false;

	/**
	* A vector to store nodes data.
	*/

	public Vector nodesData = new Vector (10, 10);

	/**
	* Constructor A - HDB with external mapping.
	* @param ourConn JDBC connection.
	* @param mapType Flag for type of map.
	* @param file Map file name.
	* @param logBuffer Dmi output log.
	*/

	public ReadNodeMap(Connection ourConn, char mapType, String file, PrintWriter logBuffer)
	{
		String mapFileName 	= System.getProperty("qsd")
							+ System.getProperty("file.separator")
							+ file;
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (mapFileName));
			try
			{
				switch (mapType)
				{
					case 'A': case 'a':
						this.textTypeAMap(ourConn, readBuffer, logBuffer);
						break;
					case 'C': case 'c':
						this.textTypeCMap(ourConn, readBuffer, logBuffer);
						break;
					case 'E': case 'e':
						this.textTypeEMap(ourConn, readBuffer, logBuffer);
						break;
					case 'H': case 'h':
						this.textTypeHMap(ourConn, readBuffer, logBuffer);
						break;
					case 'I': case 'i':
						this.textTypeIMap(ourConn, readBuffer, logBuffer);
						break;
					case 'J': case 'j':
						this.textTypeJMap(ourConn, readBuffer, logBuffer);
						break;
					case 'K': case 'k':
						this.textTypeKMap(ourConn, readBuffer, logBuffer);
						break;
					case 'L': case 'l':
						this.textTypeLMap(ourConn, readBuffer, logBuffer);
						break;
					default:
						this.textTypeAMap(ourConn, readBuffer, logBuffer);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read map file " + mapFileName);
				logBuffer.println("DMI: Unable to read map file " + mapFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close map file " + mapFileName);
				logBuffer.println("DMI: Unable to close map file " + mapFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open map file " + mapFileName);
			logBuffer.println("DMI: Unable to open map file " + mapFileName);
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor B - HDB with internal mapping.
	* @param con JDBC connection.
	* @param mapType Flag for map type.
	* @param mid_edsid Model id or external data source id.
	* @param logBuffer Dmi output log.
	*/

	public ReadNodeMap(Connection ourConn, char mapType, int mid_edsid, PrintWriter logBuffer)
	{

		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		int siteNumber = 0;
		int ext_data_source_id = mid_edsid;
		String description = null;
		ResultSet rs = null;
		Statement stmt = null;
		String sqlCommand = null;

		// if mid_edsid is negative, assume that it is a model id and attempt to obtain ext_data_source_id from it

		if (mid_edsid < 0)
		{
			sqlCommand	= "select ext_data_source_id from hdb_ext_data_source where model_id = " + -1 * mid_edsid;
	   		try
			{
				stmt = ourConn.createStatement();
				rs = stmt.executeQuery(sqlCommand);
				if (rs.next())
				{
					ext_data_source_id = rs.getInt(1);
				}
				else
				{
					System.out.println(mid_edsid + " Unable to determine external data source id from model id" + -1 * mid_edsid);
					logBuffer.println(mid_edsid + " Unable to determine external data source id from model id" + -1 * mid_edsid);
				}
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing ReadNodeMap");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
			finally
			{
				try
				{
					rs.close();
					stmt.close();
				}
				catch (SQLException e)
				{
					System.out.println ("SQL error executing ReadNodeMap");
					System.err.println ("SQLState: " + e.getSQLState ());
					System.err.println (" Message: " + e.getMessage ());
					System.err.println ("  Vendor: " + e.getErrorCode ());
					e.printStackTrace();
				}
			}
		}

		// read generic map

		sqlCommand	= "select distinct b.site_id, b.datatype_id, a.hdb_site_datatype_id, "
						+ "a.primary_site_code, a.primary_data_code from ref_ext_site_data_map a, "
						+ "hdb_site_datatype b where a.ext_data_source_id = " + ext_data_source_id
						+ " and a.hdb_site_datatype_id = b.site_datatype_id";
		try
		{
			stmt = ourConn.createStatement();
			rs = stmt.executeQuery(sqlCommand.toString());
			while (rs.next())
			{
				siteNumber = siteNumber + 1;
				site_id = rs.getInt(1);
				datatype_id = rs.getInt(2);
				site_datatype_id = rs.getInt(3);
				description = "Data of " + rs.getString(4) + " - " + rs.getString(5);

				// add node to Vector of nodes as a function of map type

				switch (mapType)
				{
					case 'A': case 'a': case 'L': case 'l':
						nodesData.addElement(new RefDmiModelMap(rs.getString(4) + "." + rs.getString(5), rs.getString(4),
							rs.getString(5), site_id, datatype_id, site_datatype_id,
							description));
						break;
					case 'C': case 'c':
						nodesData.addElement(new RefDmiModelMap(siteNumber, rs.getString(4),
							site_id, datatype_id, site_datatype_id, description));
						break;
					case 'K': case 'k':
						nodesData.addElement(new RefDmiModelMap(site_datatype_id, rs.getInt(4), description));
						break;
					default:
						nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
							site_datatype_id, rs.getString(4), rs.getString(5), description));
						break;
				}
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}
		finally
		{
			try
			{
				rs.close();
				stmt.close();
			}
			catch (SQLException e)
			{
				System.out.println ("SQL error executing ReadNodeMap.");
				System.err.println ("SQLState: " + e.getSQLState ());
				System.err.println (" Message: " + e.getMessage ());
				System.err.println ("  Vendor: " + e.getErrorCode ());
				e.printStackTrace();
			}
		}
		if (nodesData.size() < 1)
		{
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor C - Non database data with external mapping.
	* @param mapType Flag for map type.
	* @param file Map file.
	* @param logBuffer Dmi output log.
	*/

	public ReadNodeMap(char mapType, String file, PrintWriter logBuffer)
	{
		String mapFileName 	= System.getProperty("qsd")
							+ System.getProperty("file.separator")
							+ file;
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (mapFileName));
			try
			{
				switch (mapType)
				{
					case 'B': case 'b':
						this.textTypeBMap(readBuffer, logBuffer);
						break;
					case 'D': case 'd':
						this.textTypeDMap(readBuffer, logBuffer);
						break;
					case 'F': case 'f':
						this.textTypeFMap(readBuffer, logBuffer);
						break;
					case 'G': case 'g':
						this.textTypeGMap(readBuffer, logBuffer);
						break;
					default:
						this.textTypeBMap(readBuffer, logBuffer);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read map file " + mapFileName);
				logBuffer.println("DMI: Unable to read map file " + mapFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close map file " + mapFileName);
				logBuffer.println("DMI: Unable to close map file " + mapFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open map file " + mapFileName);
			logBuffer.println("DMI: Unable to open map file " + mapFileName);
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor D - HDB with control file mapping.
	* @param ourConn JDBC connection.
	* @param mapType Flag for type of map.
	* @param file Map file name.
	* @param logBuffer Dmi output log.
	*/

	public ReadNodeMap(Connection ourConn, String file, PrintWriter logBuffer, char mapType)
	{
		String controlFileName 	= System.getProperty("qsd")
							+ System.getProperty("file.separator")
							+ file;
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (controlFileName));
			try
			{
				switch (mapType)
				{
					case 'A': case 'a':
						this.conFileTypeAMap(ourConn, readBuffer, logBuffer);
						break;
					case 'C': case 'c':
						this.conFileTypeCMap(ourConn, readBuffer, logBuffer);
						break;
					case 'E': case 'e':
						this.conFileTypeEMap(ourConn, readBuffer, logBuffer);
						break;
					case 'H': case 'h':
						this.conFileTypeHMap(ourConn, readBuffer, logBuffer);
						break;
					case 'I': case 'i':
						this.conFileTypeIMap(ourConn, readBuffer, logBuffer);
						break;
					case 'J': case 'j':
						this.conFileTypeJMap(ourConn, readBuffer, logBuffer);
						break;
					case 'K': case 'k':
						this.conFileTypeKMap(ourConn, readBuffer, logBuffer);
						break;
					case 'L': case 'l':
						this.conFileTypeLMap(ourConn, readBuffer, logBuffer);
						break;
					default:
						this.conFileTypeAMap(ourConn, readBuffer, logBuffer);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read control file " + controlFileName);
				logBuffer.println("DMI: Unable to read control file " + controlFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close control file " + controlFileName);
				logBuffer.println("DMI: Unable to close control file " + controlFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open control file " + controlFileName);
			logBuffer.println("DMI: Unable to open control file " + controlFileName);
			outOfData = true;
		}

	} // end constructor

	/**
	* Constructor E - Non database data with control file mapping.
	* @param mapType Flag for map type.
	* @param file Map file.
	* @param logBuffer Dmi output log.
	*/

	public ReadNodeMap(PrintWriter logBuffer, char mapType, String file)
	{
		String controlFileName 	= System.getProperty("qsd")
							+ System.getProperty("file.separator")
							+ file;
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (controlFileName));
			try
			{
				switch (mapType)
				{
					case 'B': case 'b':
						this.conFileTypeBMap(readBuffer, logBuffer);
						break;
					case 'D': case 'd':
						this.conFileTypeDMap(readBuffer, logBuffer);
						break;
					case 'F': case 'f':
						this.conFileTypeFMap(readBuffer, logBuffer);
						break;
					case 'G': case 'g':
						this.conFileTypeGMap(readBuffer, logBuffer);
						break;
					default:
						this.conFileTypeBMap(readBuffer, logBuffer);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read control file " + controlFileName);
				logBuffer.println("DMI: Unable to read control file " + controlFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close control file " + controlFileName);
				logBuffer.println("DMI: Unable to close control file " + controlFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open control file " + controlFileName);
			logBuffer.println("DMI: Unable to open control file " + controlFileName);
			outOfData = true;
		}

	} // end constructor

	/**
	* Constructor F - HDB with meta control file (RW) mapping.
	* @param ourConn JDBC connection.
	* @param mapType Flag for type of map.
	* @param file Map file name.
	* @param logBuffer Dmi output log.
	*/

	public ReadNodeMap(Connection ourConn, char mapType, PrintWriter logBuffer,
								String controlFileName)
	{
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (controlFileName));
			try
			{
				switch (mapType)
				{
					case 'A': case 'a':
						this.metaConFileTypeAMap(ourConn, readBuffer, logBuffer);
						break;
					case 'H': case 'h':
						this.metaConFileTypeHMap(ourConn, readBuffer, logBuffer);
						break;
					case 'L': case 'l':
						this.metaConFileTypeLMap(ourConn, readBuffer, logBuffer);
						break;
					default:
						this.metaConFileTypeAMap(ourConn, readBuffer, logBuffer);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read control file " + controlFileName);
				logBuffer.println("DMI: Unable to read control file " + controlFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close control file " + controlFileName);
				logBuffer.println("DMI: Unable to close control file " + controlFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open control file " + controlFileName);
			logBuffer.println("DMI: Unable to open control file " + controlFileName);
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor G - Non database data with meta control file (RW) mapping.
	* @param mapType Flag for map type.
	* @param file Map file.
	* @param logBuffer Dmi output log.
	*/

	public ReadNodeMap(char mapType, PrintWriter logBuffer, String controlFileName)
	{
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (controlFileName));
			try
			{
				switch (mapType)
				{
					case 'B': case 'b':
						this.metaConFileTypeBMap(readBuffer, logBuffer);
						break;
					case 'D': case 'd':
						this.metaConFileTypeDMap(readBuffer, logBuffer);
						break;
					case 'F': case 'f':
						this.metaConFileTypeFMap(readBuffer, logBuffer);
						break;
					default:
						this.metaConFileTypeBMap(readBuffer, logBuffer);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read control file " + controlFileName);
				logBuffer.println("DMI: Unable to read control file " + controlFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close control file " + controlFileName);
				logBuffer.println("DMI: Unable to close control file " + controlFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open control file " + controlFileName);
			logBuffer.println("DMI: Unable to open control file " + controlFileName);
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor H - RDB with external mapping.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param mapType Flag for type of map.
	* @param file Map file name.
	* @param logBuffer Dmi output log.
	* @param sdtTable Site datatype table name.
	* @param sidField Field that contains site id's.
	* @param dtField Field that contains datatype id's.
	* @param sdtidField Field that contains site datatype id's.
	*/

	public ReadNodeMap(Connection ourConn, String dbType, char mapType, String file, PrintWriter logBuffer,
			String sdtTable, String sidField, String dtField, String sdtidField)
	{
		String mapFileName 	= System.getProperty("qsd")
							+ System.getProperty("file.separator")
							+ file;
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (mapFileName));
			try
			{
				switch (mapType)
				{
					case 'Z': case 'z':
						this.textTypeZMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'Y': case 'y':
						this.textTypeYMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'X': case 'x':
						this.textTypeXMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'V': case 'v':
						this.textTypeVMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'U': case 'u':
						this.textTypeUMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'T': case 't':
						this.textTypeTMap(ourConn, dbType, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'S': case 's':
						this.textTypeSMap(ourConn, dbType, readBuffer, logBuffer, sdtTable, sdtidField);
						break;
					case 'R': case 'r':
						this.textTypeRMap(ourConn, dbType, readBuffer, logBuffer, sdtTable, sdtidField);
						break;
					default:
						this.textTypeZMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read map file " + mapFileName);
				logBuffer.println("DMI: Unable to read map file " + mapFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close map file " + mapFileName);
				logBuffer.println("DMI: Unable to close map file " + mapFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open map file " + mapFileName);
			logBuffer.println("DMI: Unable to open map file " + mapFileName);
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor I - RDB with internal mapping with a model_id.
	* @param con JDBC connection.
	* @param mapType Flag for map type.
	* @param mid Model id.
	* @param logBuffer Dmi output log.
	* @param mapTable Site datatype table name.
	* @param objField Field that contains object name.
	* @param slotField Field that contains slot name.
	* @param sdtidField Field that contains site datatype id's.
	* @param midField Field that contains datatype id's.
	*/

	public ReadNodeMap(Connection ourConn, char mapType, int mid, PrintWriter logBuffer,
			String mapTable, String objField, String slotField, String sdtidField, String midField)
	{
		int site_datatype_id = 0;
		int siteNumber = 0;
		String description = null;

		String sqlCommand	= "Select distinct " + sdtidField + ", " + objField + ", " + slotField
					+ " From " + mapTable + " where " + midField + " = " + mid;
		ResultSet rs = null;
		Statement stmt = null;

		try
		{
			stmt = ourConn.createStatement();
			rs = stmt.executeQuery(sqlCommand.toString());
			while (rs.next())
			{
				siteNumber = siteNumber + 1;
				site_datatype_id = rs.getInt(1);
				description = "Data of " + rs.getString(2) + " - " + rs.getString(3);

				// add node to Vector of nodes as a function of map type

				switch (mapType)
				{
					case 'Z': case 'z': case 'V': case 'v': case 'R': case 'r':
						nodesData.addElement(new RefDmiModelMap(rs.getString(2) + "." + rs.getString(3), rs.getString(2),
							rs.getString(3), siteNumber, siteNumber, site_datatype_id,
							description));
						break;
					case 'Y': case 'y':
						nodesData.addElement(new RefDmiModelMap(siteNumber, rs.getString(2),
							siteNumber, siteNumber, site_datatype_id, description));
						break;
					case 'T': case 't':
						nodesData.addElement(new RefDmiModelMap(site_datatype_id, rs.getString(2), rs.getString(3), description));
						break;
					default:
						nodesData.addElement(new RefDmiModelMap(siteNumber, siteNumber,
							site_datatype_id, rs.getString(2), rs.getString(3), description));
						break;
				}
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}
		if (nodesData.size() < 1)
		{
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor J - RDB with control file mapping.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param mapType Flag for type of map.
	* @param file Map file name.
	* @param logBuffer Dmi output log.
	* @param sdtTable Site datatype table name.
	* @param sidField Field that contains site id's.
	* @param dtField Field that contains datatype id's.
	* @param sdtidField Field that contains site datatype id's.
	*/

	public ReadNodeMap(Connection ourConn, String dbType, String file, PrintWriter logBuffer, char mapType,
			String sdtTable, String sidField, String dtField, String sdtidField)
	{
		String controlFileName 	= System.getProperty("qsd")
							+ System.getProperty("file.separator")
							+ file;
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (controlFileName));
			try
			{
				switch (mapType)
				{
					case 'Z': case 'z':
						this.conFileTypeZMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'Y': case 'y':
						this.conFileTypeYMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'X': case 'x':
						this.conFileTypeXMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'V': case 'v':
						this.conFileTypeVMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'U': case 'u':
						this.conFileTypeUMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'T': case 't':
						this.conFileTypeTMap(ourConn, dbType, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'S': case 's':
						this.conFileTypeSMap(ourConn, dbType, readBuffer, logBuffer, sdtTable, sdtidField);
						break;
					case 'R': case 'r':
						this.conFileTypeRMap(ourConn, dbType, readBuffer, logBuffer, sdtTable, sdtidField);
						break;
					default:
						this.conFileTypeZMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read control file " + controlFileName);
				logBuffer.println("DMI: Unable to read control file " + controlFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close control file " + controlFileName);
				logBuffer.println("DMI: Unable to close control file " + controlFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open control file " + controlFileName);
			logBuffer.println("DMI: Unable to open control file " + controlFileName);
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor K - RDB with meta control file (RW) mapping.
	* @param ourConn JDBC connection.
	* @param dbType Database type.
	* @param mapType Flag for type of map.
	* @param file Map file name.
	* @param logBuffer Dmi output log.
	* @param sdtTable Site datatype table name.
	* @param sidField Field that contains site id's.
	* @param dtField Field that contains datatype id's.
	* @param sdtidField Field that contains site datatype id's.
	*/

	public ReadNodeMap(Connection ourConn, String dbType, char mapType, PrintWriter logBuffer, String controlFileName,
			String sdtTable, String sidField, String dtField, String sdtidField)
	{
		try
		{
			BufferedReader readBuffer = new BufferedReader(new FileReader (controlFileName));
			try
			{
				switch (mapType)
				{
					case 'Z': case 'z':
						this.metaConFileTypeZMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'V': case 'v':
						this.metaConFileTypeVMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
					case 'R': case 'r':
						this.metaConFileTypeRMap(ourConn, dbType, readBuffer, logBuffer, sdtTable, sdtidField);
						break;
					default:
						this.metaConFileTypeZMap(ourConn, readBuffer, logBuffer,
							sdtTable, sidField, dtField, sdtidField);
						break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to read control file " + controlFileName);
				logBuffer.println("DMI: Unable to read control file " + controlFileName);
				outOfData = true;
			}

			try
			{
				readBuffer.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to close control file " + controlFileName);
				logBuffer.println("DMI: Unable to close control file " + controlFileName);
				outOfData = true;
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Unable to open control file " + controlFileName);
			logBuffer.println("DMI: Unable to open control file " + controlFileName);
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor L - RDB with internal mapping without a model_id.
	* @param con JDBC connection.
	* @param mapType Flag for map type.
	* @param logBuffer Dmi output log.
	* @param mapTable Site datatype table name.
	* @param objField Field that contains object name.
	* @param slotField Field that contains slot name.
	* @param sdtidField Field that contains site datatype id's.
	*/

	public ReadNodeMap(Connection ourConn, char mapType, PrintWriter logBuffer,
			String mapTable, String objField, String slotField, String sdtidField)
	{
		int site_datatype_id = 0;
		int siteNumber = 0;
		String description = null;

		String sqlCommand	= "Select distinct " + sdtidField + ", " + objField + ", " + slotField
					+ " From " + mapTable;
		ResultSet rs = null;
		Statement stmt = null;

		try
		{
			stmt = ourConn.createStatement();
			rs = stmt.executeQuery(sqlCommand.toString());
			while (rs.next())
			{
				siteNumber = siteNumber + 1;
				site_datatype_id = rs.getInt(1);
				description = "Data of " + rs.getString(2) + " - " + rs.getString(3);

				// add node to Vector of nodes as a function of map type

				switch (mapType)
				{
					case 'Z': case 'z': case 'V': case 'v': case 'R': case 'r':
						nodesData.addElement(new RefDmiModelMap(rs.getString(2) + "." + rs.getString(3), rs.getString(2),
							rs.getString(3), siteNumber, siteNumber, site_datatype_id,
							description));
						break;
					case 'Y': case 'y':
						nodesData.addElement(new RefDmiModelMap(siteNumber, rs.getString(2),
							siteNumber, siteNumber, site_datatype_id, description));
						break;
					case 'T': case 't':
						nodesData.addElement(new RefDmiModelMap(site_datatype_id, rs.getString(2), rs.getString(3), description));
						break;
					default:
						nodesData.addElement(new RefDmiModelMap(siteNumber, siteNumber,
							site_datatype_id, rs.getString(2), rs.getString(3), description));
						break;
				}
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}
		if (nodesData.size() < 1)
		{
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor M - RDB with internal mapping of sdtid to sdtid with a model_id.
	* @param con JDBC connection.
	* @param mapType Flag for map type.
	* @param mid Model id.
	* @param logBuffer Dmi output log.
	* @param mapTable Site datatype table name.
	* @param firstSdtidField Field that contains source site datatype id's.
	* @param secondSdtidField Field that contains second site datatype id's.
	* @param midField Field that contains datatype id's.
	*/

	public ReadNodeMap(Connection ourConn, char mapType, int mid, PrintWriter logBuffer,
			String mapTable, String firstSdtidField, String secondSdtidField, String midField)
	{
		int site_datatype_id = 0;
		int siteDatatypeId = 0;
		String description = null;

		String sqlCommand	= "Select distinct " + firstSdtidField + ", " + secondSdtidField
					+ " From " + mapTable + " where " + midField + " = " + mid;
		ResultSet rs = null;
		Statement stmt = null;

		try
		{
			stmt = ourConn.createStatement();
			rs = stmt.executeQuery(sqlCommand.toString());
			while (rs.next())
			{
				site_datatype_id = rs.getInt(1);
				siteDatatypeId = rs.getInt(2);
				description = "Data of " + rs.getString(1) + " - " + rs.getString(2);

				// add node to Vector of nodes as a function of map type

				nodesData.addElement(new RefDmiModelMap(site_datatype_id, siteDatatypeId, description));
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}
		if (nodesData.size() < 1)
		{
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}

	}	// end constructor

	/**
	* Constructor N - RDB with internal mapping of sdtid to sdtid without a model_id.
	* @param con JDBC connection.
	* @param mapType Flag for map type.
	* @param logBuffer Dmi output log.
	* @param mapTable Site datatype table name.
	* @param firstSdtidField Field that contains source site datatype id's.
	* @param secondSdtidField Field that contains second site datatype id's.
	*/

	public ReadNodeMap(Connection ourConn, char mapType, PrintWriter logBuffer,
			String mapTable, String firstSdtidField, String secondSdtidField)
	{
		int site_datatype_id = 0;
		int siteDatatypeId = 0;
		String description = null;

		String sqlCommand	= "Select distinct " + firstSdtidField + ", " + secondSdtidField
					+ " From " + mapTable;
		ResultSet rs = null;
		Statement stmt = null;

		try
		{
			stmt = ourConn.createStatement();
			rs = stmt.executeQuery(sqlCommand.toString());
			while (rs.next())
			{
				site_datatype_id = rs.getInt(1);
				siteDatatypeId = rs.getInt(2);
				description = "Data of " + rs.getString(1) + " - " + rs.getString(2);

				// add node to Vector of nodes as a function of map type

				nodesData.addElement(new RefDmiModelMap(site_datatype_id, siteDatatypeId, description));
			}
			rs.close();
			stmt.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			System.err.println ("SQLState: " + e.getSQLState ());
			System.err.println (" Message: " + e.getMessage ());
			System.err.println ("  Vendor: " + e.getErrorCode ());
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}
		if (nodesData.size() < 1)
		{
			System.out.println("Unable to process node mapping.");
			logBuffer.println("DMI: Unable to process node mapping.");
			outOfData = true;
		}

	}	// end constructor

	// Reads Type A text map.

	private void textTypeAMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();

				objectSlot = object_name + "." + slot_name;

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// determine site datatype id

				site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
						slot_name, site_id, datatype_id, site_datatype_id, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type B text map.

	private void textTypeBMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		String object_name = null;
		String slot_name = null;
		String objectSlot = null;
		String stationName = null;
		String parameter = null;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();
				objectSlot = object_name + "." + slot_name;

				// station name

				stationName = st.nextToken();

				// parameter

				parameter = st.nextToken();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(objectSlot, object_name, slot_name,
								stationName, parameter, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type C text map.

	private void textTypeCMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		String stationName = null;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// siteNumber

				siteNumber = Integer.valueOf(st.nextToken()).intValue();

				// stationName

				stationName = st.nextToken();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + stationName;
				}

				// determine site datatype id

				site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(siteNumber, stationName, site_id,
										datatype_id, site_datatype_id, description));
			}
		}

		 // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type D text map.

	private void textTypeDMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		String stationName = null;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();
				objectSlot = object_name + "." + slot_name;

				// site number

				siteNumber = Integer.valueOf(st.nextToken()).intValue();

				// station name

				stationName = st.nextToken();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(objectSlot, object_name, slot_name,
								siteNumber, stationName, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type E text map.

	private void textTypeEMap(Connection ourConn, BufferedReader readBuffer,
							PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// siteNumber

				siteNumber = Integer.valueOf(st.nextToken()).intValue();

				// paramNumber

				paramNumber = Integer.valueOf(st.nextToken()).intValue();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + line;
				}

				// determine site datatype id

				site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(siteNumber, paramNumber, site_id,
											datatype_id, site_datatype_id, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type F text map.

	private void textTypeFMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		String object_name = null;
		String slot_name = null;
		String objectSlot = null;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();
				objectSlot = object_name + "." + slot_name;

				// site number

				siteNumber = Integer.valueOf(st.nextToken()).intValue();

				// param number

				paramNumber = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
								siteNumber, paramNumber, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type G text map.

	private void textTypeGMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		int site_id = 0;
		int datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// site number

				siteNumber = Integer.valueOf(st.nextToken()).intValue();

				// param number

				paramNumber = Integer.valueOf(st.nextToken()).intValue();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + line;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(siteNumber, paramNumber, site_id,
										datatype_id, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type H text map.

	private void textTypeHMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object_name

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// determine site datatype id

				site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
								site_id, datatype_id, site_datatype_id, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type I text map.

	private void textTypeIMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// stationName

				stationName = st.nextToken();

				// parameter

				parameter = st.nextToken();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + stationName + "." + parameter;
				}

				// determine site datatype id

				site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
							site_datatype_id, stationName, parameter, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type J text map.

	private void textTypeJMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// stationName

				stationName = st.nextToken();

				// parameter

				parameter = st.nextToken();

				// site datatype id

				site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + stationName + "." + parameter;
				}

				// verify site_datatype_id and determine site id and datatype id

				site_id = 0;
				datatype_id = 0;
				try
				{
					if (VerifyRecords.verifyHdbSiteDatatype(ourConn, site_datatype_id))
					{
						site_id = HdbDmiUtils.getSiteIdGivenSiteDatatypeId(ourConn, site_datatype_id);
						if (site_id <= 0)
						{
							logBuffer.println("Unable to determine site id for site datatype id " + site_datatype_id);
						}
						datatype_id = HdbDmiUtils.getDatatypeIdGivenSiteDatatypeId(ourConn, site_datatype_id);
						if (datatype_id <= 0)
						{
							logBuffer.println("Unable to determine datatype id for site datatype id " + site_datatype_id);
						}
					}
					else
					{
						logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("Unable to verify site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//					outOfData = true;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
							site_datatype_id, stationName, parameter, description));
			}

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type K text map.

	private void textTypeKMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_datatype_id = 0;
		int siteDatatypeId = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// source site datatype id

				site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// second site datatype id

				siteDatatypeId = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + site_datatype_id + "." + siteDatatypeId;
				}

				// verify source site_datatype_id

				try
				{
					if (!VerifyRecords.verifyHdbSiteDatatype(ourConn, site_datatype_id))
					{
						System.out.println("Unable to verify source site datatype id " + site_datatype_id);
						logBuffer.println("Unable to verify source site datatype id " + site_datatype_id);
//						outOfData = true;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("Unable to verify source site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify source site datatype id " + site_datatype_id);
//					outOfData = true;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(site_datatype_id, siteDatatypeId, description));
			}

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type L text map.

	private void textTypeLMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();

				objectSlot = object_name + "." + slot_name;

				// site datatype id

				site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// verify site_datatype_id and determine site id and datatype id

				site_id = 0;
				datatype_id = 0;
				try
				{
					if (VerifyRecords.verifyHdbSiteDatatype(ourConn, site_datatype_id))
					{
						site_id = HdbDmiUtils.getSiteIdGivenSiteDatatypeId(ourConn, site_datatype_id);
						if (site_id <= 0)
						{
							logBuffer.println("Unable to determine site id for site datatype id " + site_datatype_id);
						}
						datatype_id = HdbDmiUtils.getDatatypeIdGivenSiteDatatypeId(ourConn, site_datatype_id);
						if (datatype_id <= 0)
						{
							logBuffer.println("Unable to determine datatype id for site datatype id " + site_datatype_id);
						}
					}
					else
					{
						logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("Unable to verify site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//					outOfData = true;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
						slot_name, site_id, datatype_id, site_datatype_id, description));
			}

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type Z text map.

	private void textTypeZMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();

				objectSlot = object_name + "." + slot_name;

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// determine site datatype id

				site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
							sdtTable, sidField, dtField, sdtidField);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
						slot_name, site_id, datatype_id, site_datatype_id, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type Y text map.

	private void textTypeYMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		String stationName = null;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// siteNumber

				siteNumber = Integer.valueOf(st.nextToken()).intValue();

				// stationName

				stationName = st.nextToken();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + stationName;
				}

				// determine site datatype id

				site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
							sdtTable, sidField, dtField, sdtidField);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(siteNumber, stationName, site_id,
										datatype_id, site_datatype_id, description));
			}
		}

		 // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type X text map.

	private void textTypeXMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// siteNumber

				siteNumber = Integer.valueOf(st.nextToken()).intValue();

				// paramNumber

				paramNumber = Integer.valueOf(st.nextToken()).intValue();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + line;
				}

				// determine site datatype id

				site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
							sdtTable, sidField, dtField, sdtidField);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(siteNumber, paramNumber, site_id,
											datatype_id, site_datatype_id, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type W text map.

	private void textTypeWMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		int site_id = 0;
		int datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// site number

				siteNumber = Integer.valueOf(st.nextToken()).intValue();

				// param number

				paramNumber = Integer.valueOf(st.nextToken()).intValue();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + line;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(siteNumber, paramNumber, site_id,
										datatype_id, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type V text map.

	private void textTypeVMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object_name

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// determine site datatype id

				site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
							sdtTable, sidField, dtField, sdtidField);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
								site_id, datatype_id, site_datatype_id, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type U text map.

	private void textTypeUMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// stationName

				stationName = st.nextToken();

				// parameter

				parameter = st.nextToken();

				// site id

				site_id = Integer.valueOf(st.nextToken()).intValue();

				// datatype id

				datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + stationName + "." + parameter;
				}

				// determine site datatype id

				site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
							sdtTable, sidField, dtField, sdtidField);

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
							site_datatype_id, stationName, parameter, description));
			}

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type T text map.

	private void textTypeTMap(Connection ourConn, String dbType, BufferedReader readBuffer,
		PrintWriter logBuffer, String sdtTable, String sidField, String dtField,
		String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// stationName

				stationName = st.nextToken();

				// parameter

				parameter = st.nextToken();

				// site datatype id

				site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + stationName + "." + parameter;
				}

				// verify site_datatype_id

				site_id = 0;
				datatype_id = 0;
				try
				{
					if (!VerifyRecords.verifyRdbSiteDatatype(ourConn, site_datatype_id, sdtTable, sdtidField, dbType))
					{
						System.out.println("Unable to verify source site datatype id " + site_datatype_id);
						logBuffer.println("Unable to verify source site datatype id " + site_datatype_id);
//						outOfData = true;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("Unable to verify site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//					outOfData = true;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
							site_datatype_id, stationName, parameter, description));
			}

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type S text map.

	private void textTypeSMap(Connection ourConn, String dbType, BufferedReader readBuffer,
		PrintWriter logBuffer, String sdtTable, String sdtidField) throws IOException
	{
		// NOTE - connection is to "source" database

		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_datatype_id = 0;
		int siteDatatypeId = 0;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// source site datatype id

				site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// second site datatype id

				siteDatatypeId = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + site_datatype_id + "." + siteDatatypeId;
				}

				// verify source site_datatype_id

				try
				{
					if (!VerifyRecords.verifyRdbSiteDatatype(ourConn, siteDatatypeId, sdtTable, sdtidField, dbType))
					{
						System.out.println("Unable to verify source site datatype id " + siteDatatypeId);
						logBuffer.println("Unable to verify source site datatype id " + siteDatatypeId);
//						outOfData = true;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("Unable to verify source site datatype id " + siteDatatypeId);
					logBuffer.println("Unable to verify source site datatype id " + siteDatatypeId);
//					outOfData = true;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(site_datatype_id, siteDatatypeId, description));
			}

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type R text map.

	private void textTypeRMap(Connection ourConn, String dbType, BufferedReader readBuffer,
		PrintWriter logBuffer, String sdtTable, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			len = line.length();
			if((len > 0 &&
				(!line.substring(0, 1).equals(" ")) && (!line.substring(0, 1).equals("#"))))
			{
				st = new StringTokenizer(line, "\t");

				// object

				object_name = st.nextToken();

				// slot

				slot_name = st.nextToken();

				objectSlot = object_name + "." + slot_name;

				// site datatype id

				site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

				// description

				try
				{
					description = st.nextToken();
				}
				catch (Exception e)
				{
					description = "Data of " + objectSlot;
				}

				// verify site_datatype_id

				site_id = 0;
				datatype_id = 0;
				try
				{
					if (!VerifyRecords.verifyRdbSiteDatatype(ourConn, site_datatype_id, sdtTable, sdtidField, dbType))
					{
						System.out.println("Unable to verify source site datatype id " + site_datatype_id);
						logBuffer.println("Unable to verify source site datatype id " + site_datatype_id);
//						outOfData = true;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("Unable to verify site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//					outOfData = true;
				}

				// add node to Vector of nodes

				nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
						slot_name, site_datatype_id, description));
			}

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type A non RiverWare control file map

	private void conFileTypeAMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// object

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();

			objectSlot = object_name + "." + slot_name;

			restOfLine = st.nextToken();

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// determine site datatype id

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
					slot_name, site_id, datatype_id, site_datatype_id, description));

		}	//	end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in control file.");
			logBuffer.println("No nodes found in control file.");
		}
		return;

	}	// end method

	// Reads Type B non RiverWare control file map

	private void conFileTypeBMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		String object_name = null;
		String objectSlot = null;
		String slot_name = null;
		String stationName = null;
		String parameter = null;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// object

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();
			objectSlot = object_name + "." + slot_name;

			restOfLine = st.nextToken();

			// station name

			stationName = ControlFileUtils.getStationName(restOfLine);
			if (stationName == null)stationName = object_name;

			// parameter

			parameter = ControlFileUtils.getParameter(restOfLine);
			if (parameter == null)parameter = slot_name;

			// description

			description = "Data of " + objectSlot;

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name, slot_name,
							stationName, parameter, description));

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type C non RiverWare control file map

	private void conFileTypeCMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		String stationName = null;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// siteNumber

			siteNumber = Integer.valueOf(st.nextToken()).intValue();

			// stationName

			stationName = st.nextToken();

			restOfLine = st.nextToken();

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + line;

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(siteNumber, stationName, site_id,
									datatype_id, site_datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type D non RiverWare control file map

	private void conFileTypeDMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		String stationName = null;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// object

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();
			objectSlot = object_name + "." + slot_name;

			restOfLine = st.nextToken();

			// site number

			siteNumber = ControlFileUtils.getSiteId(restOfLine);

			// station name

			stationName = ControlFileUtils.getStationName(restOfLine);
			if (stationName == null)stationName = object_name;

			// description

			description = "Data of " + objectSlot;

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name, slot_name,
							siteNumber, stationName, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type E non RiverWare control file map

	private void conFileTypeEMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// siteNumber

			siteNumber = ControlFileUtils.getSiteId(restOfLine);

			// paramNumber

			paramNumber = ControlFileUtils.getSiteId(restOfLine);

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + line;

			// determine site datatype id

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(siteNumber, paramNumber, site_id,
										datatype_id, site_datatype_id, description));
		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type F non RiverWare control file map.

	private void conFileTypeFMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		String object_name = null;
		String slot_name = null;
		String objectSlot = null;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// object

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();
			objectSlot = object_name + "." + slot_name;

			// site number

			siteNumber = ControlFileUtils.getSiteId(restOfLine);

			// param number

			paramNumber = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
								siteNumber, paramNumber, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type G non RiverWare control file map.

	private void conFileTypeGMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		int site_id = 0;
		int datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			st = new StringTokenizer(line, "\t");

			// site number

			siteNumber = ControlFileUtils.getSiteId(restOfLine);

			// param number

			paramNumber = ControlFileUtils.getSiteId(restOfLine);

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + line;

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(siteNumber, paramNumber, site_id,
									datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type H non RiverWare control file map.

	private void conFileTypeHMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			st = new StringTokenizer(line, "\t");

			// object_name

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + object_name + "." + slot_name;

			// determine site datatype id

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
							site_id, datatype_id, site_datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type I non RiverWare control file map.

	private void conFileTypeIMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// station name

			stationName = st.nextToken();

			// parameter

			parameter = st.nextToken();

			restOfLine = st.nextToken();

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + stationName + "." + parameter;

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
						site_datatype_id, stationName, parameter, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type J non RiverWare control file map.

	private void conFileTypeJMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// station name

			stationName = st.nextToken();

			// parameter

			parameter = st.nextToken();

			restOfLine = st.nextToken();

			// site datatype id

			site_datatype_id = ControlFileUtils.getSDTID(restOfLine);

			// description

			description = "Data of " + stationName + "." + parameter;

			// verify site_datatype_id and determine site id and datatype id

			site_id = 0;
			datatype_id = 0;
			try
			{
				if (VerifyRecords.verifyHdbSiteDatatype(ourConn, site_datatype_id))
				{
					site_id = HdbDmiUtils.getSiteIdGivenSiteDatatypeId(ourConn, site_datatype_id);
					if (site_id <= 0)
					{
						logBuffer.println("Unable to determine site id for site datatype id " + site_datatype_id);
					}
					datatype_id = HdbDmiUtils.getDatatypeIdGivenSiteDatatypeId(ourConn, site_datatype_id);
					if (datatype_id <= 0)
					{
						logBuffer.println("Unable to determine datatype id for site datatype id " + site_datatype_id);
					}
				}
				else
				{
					System.out.println("Unable to verify site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//					outOfData = true;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to verify site datatype id " + site_datatype_id);
				logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//				outOfData = true;
			}

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
						site_datatype_id, stationName, parameter, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type K non RiverWare control file map.

	private void conFileTypeKMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		int siteDatatypeId = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// source site datatype id

			site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

			restOfLine = st.nextToken();

			// second site datatype id

			siteDatatypeId = ControlFileUtils.getSDTID(restOfLine);

			// description

			try
			{
				description = st.nextToken();
			}
			catch (Exception e)
			{
				description = "Data of " + site_datatype_id + "." + siteDatatypeId;
			}

				// verify source site_datatype_id

			try
			{
				if (!VerifyRecords.verifyHdbSiteDatatype(ourConn, site_datatype_id))
				{
					System.out.println("Unable to verify source site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify source site datatype id " + site_datatype_id);
//					outOfData = true;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to verify source site datatype id " + site_datatype_id);
				logBuffer.println("Unable to verify source site datatype id " + site_datatype_id);
//				outOfData = true;
			}

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(site_datatype_id, siteDatatypeId, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type L non RiverWare control file map

	private void conFileTypeLMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// object

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();

			objectSlot = object_name + "." + slot_name;

			restOfLine = st.nextToken();

			// site datatype id

			site_datatype_id = ControlFileUtils.getSDTID(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// verify site_datatype_id and determine site id and datatype id

			site_id = 0;
			datatype_id = 0;
			try
			{
				if (VerifyRecords.verifyHdbSiteDatatype(ourConn, site_datatype_id))
				{
					site_id = HdbDmiUtils.getSiteIdGivenSiteDatatypeId(ourConn, site_datatype_id);
					if (site_id <= 0)
					{
						logBuffer.println("Unable to determine site id for site datatype id " + site_datatype_id);
					}
					datatype_id = HdbDmiUtils.getDatatypeIdGivenSiteDatatypeId(ourConn, site_datatype_id);
					if (datatype_id <= 0)
					{
						logBuffer.println("Unable to determine datatype id for site datatype id " + site_datatype_id);
					}
				}
				else
				{
					logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to verify site datatype id " + site_datatype_id);
				logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//				outOfData = true;
			}

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
					slot_name, site_id, datatype_id, site_datatype_id, description));

		}	//	end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in control file.");
			logBuffer.println("No nodes found in control file.");
		}
		return;

	}	// end method

	// Reads Type Z control file map.

	private void conFileTypeZMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// object

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();

			objectSlot = object_name + "." + slot_name;

			restOfLine = st.nextToken();

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// determine site datatype id

			site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
						sdtTable, sidField, dtField, sdtidField);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
					slot_name, site_id, datatype_id, site_datatype_id, description));

		}	//	end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in control file.");
			logBuffer.println("No nodes found in control file.");
		}
		return;

	}	// end method

	// Reads Type Y non RiverWare control file map

	private void conFileTypeYMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		String stationName = null;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// siteNumber

			siteNumber = Integer.valueOf(st.nextToken()).intValue();

			// stationName

			stationName = st.nextToken();

			restOfLine = st.nextToken();

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + line;

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(siteNumber, stationName, site_id,
									datatype_id, site_datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type X non RiverWare control file map

	private void conFileTypeXMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// siteNumber

			siteNumber = ControlFileUtils.getSiteId(restOfLine);

			// paramNumber

			paramNumber = ControlFileUtils.getSiteId(restOfLine);

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + line;

			// determine site datatype id

			site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
						sdtTable, sidField, dtField, sdtidField);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(siteNumber, paramNumber, site_id,
										datatype_id, site_datatype_id, description));
		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type W non RiverWare control file map.

	private void conFileTypeWMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		int site_id = 0;
		int datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// site number

			siteNumber = ControlFileUtils.getSiteId(restOfLine);

			// param number

			paramNumber = ControlFileUtils.getSiteId(restOfLine);

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + line;

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(siteNumber, paramNumber, site_id,
									datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type V non RiverWare control file map.

	private void conFileTypeVMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// object_name

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + object_name + "." + slot_name;

			// determine site datatype id

			site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
						sdtTable, sidField, dtField, sdtidField);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
							site_id, datatype_id, site_datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type U non RiverWare control file map.

	private void conFileTypeUMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// station name

			stationName = st.nextToken();

			// parameter

			parameter = st.nextToken();

			restOfLine = st.nextToken();

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + stationName + "." + parameter;

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
						site_datatype_id, stationName, parameter, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type T non RiverWare control file map.

	private void conFileTypeTMap(Connection ourConn, String dbType, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// station name

			stationName = st.nextToken();

			// parameter

			parameter = st.nextToken();

			restOfLine = st.nextToken();

			// site datatype id

			site_datatype_id = ControlFileUtils.getSDTID(restOfLine);

			// description

			description = "Data of " + stationName + "." + parameter;

			// verify site_datatype_id

			site_id = 0;
			datatype_id = 0;
			try
			{
				if (!VerifyRecords.verifyRdbSiteDatatype(ourConn, site_datatype_id, sdtTable, sdtidField, dbType))
				{
					System.out.println("Unable to verify site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to verify site datatype id " + site_datatype_id);
				logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
			}

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(site_id, datatype_id,
						site_datatype_id, stationName, parameter, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type S non RiverWare control file map.

	private void conFileTypeSMap(Connection ourConn, String dbType, BufferedReader readBuffer,
		PrintWriter logBuffer, String sdtTable, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String stationName = null;
		String parameter = null;
		String objectSlot = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		int siteDatatypeId = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// source site datatype id

			site_datatype_id = Integer.valueOf(st.nextToken()).intValue();

			restOfLine = st.nextToken();

			// second site datatype id

			siteDatatypeId = ControlFileUtils.getSDTID(restOfLine);

//			System.out.println("Mapped " + siteDatatypeId + " to " + site_datatype_id);

			// description

			try
			{
				description = st.nextToken();
			}
			catch (Exception e)
			{
				description = "Data of " + siteDatatypeId + "." + site_datatype_id;
			}

			// verify source site datatype id

			try
			{
//				System.out.println("Verifying " + siteDatatypeId + " in table " + sdtTable + " and field " + sdtidField);
				if (!VerifyRecords.verifyRdbSiteDatatype(ourConn, siteDatatypeId, sdtTable, sdtidField, dbType))
				{
					System.out.println("Unable to verify source site datatype id " + siteDatatypeId);
					logBuffer.println("Unable to verify source site datatype id " + siteDatatypeId);
//					outOfData = true;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to verify source site datatype id " + siteDatatypeId);
				logBuffer.println("Unable to verify source site datatype id " + siteDatatypeId);
//				outOfData = true;
			}

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(site_datatype_id, siteDatatypeId, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type R control file map.

	private void conFileTypeRMap(Connection ourConn, String dbType, BufferedReader readBuffer,
			PrintWriter logBuffer, String sdtTable, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			st = new StringTokenizer(line, "\t");

			// object

			object_name = st.nextToken();

			// slot

			slot_name = st.nextToken();

			objectSlot = object_name + "." + slot_name;

			restOfLine = st.nextToken();

			// site datatype id

			site_datatype_id = ControlFileUtils.getSDTID(restOfLine);

			// description

			description = "Data of " + objectSlot;


			// verify site_datatype_id

			site_id = 0;
			datatype_id = 0;
			try
			{
				if (!VerifyRecords.verifyRdbSiteDatatype(ourConn, site_datatype_id, sdtTable, sdtidField, dbType))
				{
					System.out.println("Unable to verify source site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify source site datatype id " + site_datatype_id);
//					outOfData = true;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to verify site datatype id " + site_datatype_id);
				logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//				outOfData = true;
			}

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
					slot_name, site_datatype_id, description));

		}	//	end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in control file.");
			logBuffer.println("No nodes found in control file.");
		}
		return;

	}	// end method

	// Reads Type A RiverWare control file map

	private void metaConFileTypeAMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// determine site datatype id

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
					slot_name, site_id, datatype_id, site_datatype_id, description));

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in control file.");
			logBuffer.println("No nodes found in control file.");
		}
		return;

	}	// end method

	// Reads Type B RiverWare control file map

	private void metaConFileTypeBMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		String object_name = null;
		String objectSlot = null;
		String slot_name = null;
		String stationName = null;
		String parameter = null;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// station name

			stationName = ControlFileUtils.getStationName(restOfLine);
			if (stationName == null)stationName = object_name;

			// parameter

			parameter = ControlFileUtils.getParameter(restOfLine);
			if (parameter == null)parameter = slot_name;

			// description

			description = "Data of " + objectSlot;

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name, slot_name,
							stationName, parameter, description));

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type D RiverWare control file map

	private void metaConFileTypeDMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		String stationName = null;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// site number

			siteNumber = ControlFileUtils.getSiteId(restOfLine);

			// station name

			stationName = ControlFileUtils.getStationName(restOfLine);
			if (stationName == null)stationName = object_name;

			// description

			description = "Data of " + objectSlot;

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name, slot_name,
							siteNumber, stationName, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type F RiverWare control file map.

	private void metaConFileTypeFMap(BufferedReader readBuffer, PrintWriter logBuffer)
														throws IOException
	{
		String line = null;
		StringTokenizer st;
		int len = 0;
		int siteNumber = 0;
		int paramNumber = 0;
		String object_name = null;
		String slot_name = null;
		String objectSlot = null;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// site number

			siteNumber = ControlFileUtils.getSiteId(restOfLine);

			// param number

			paramNumber = ControlFileUtils.getSiteId(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
								siteNumber, paramNumber, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type H RiverWare control file map.

	private void metaConFileTypeHMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// determine site datatype id

			site_datatype_id = HdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
							site_id, datatype_id, site_datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type L RiverWare control file map

	private void metaConFileTypeLMap(Connection ourConn, BufferedReader readBuffer,
								PrintWriter logBuffer) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// site datatype id

			site_datatype_id = ControlFileUtils.getSDTID(restOfLine);

			// description

			description = "Data of " + objectSlot;


			// verify site_datatype_id and determine site id and datatype id

			site_id = 0;
			datatype_id = 0;
			try
			{
				if (VerifyRecords.verifyHdbSiteDatatype(ourConn, site_datatype_id))
				{
					site_id = HdbDmiUtils.getSiteIdGivenSiteDatatypeId(ourConn, site_datatype_id);
					if (site_id <= 0)
					{
						logBuffer.println("Unable to determine site id for site datatype id " + site_datatype_id);
					}
					datatype_id = HdbDmiUtils.getDatatypeIdGivenSiteDatatypeId(ourConn, site_datatype_id);
					if (datatype_id <= 0)
					{
						logBuffer.println("Unable to determine datatype id for site datatype id " + site_datatype_id);
					}
				}
				else
				{
					logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to verify site datatype id " + site_datatype_id);
				logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//				outOfData = true;
			}

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
					slot_name, site_id, datatype_id, site_datatype_id, description));

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in control file.");
			logBuffer.println("No nodes found in control file.");
		}
		return;

	}	// end method

	// Reads Type Z meta control map.

	private void metaConFileTypeZMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// determine site datatype id

			site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
						sdtTable, sidField, dtField, sdtidField);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
					slot_name, site_id, datatype_id, site_datatype_id, description));

		} // end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in control file.");
			logBuffer.println("No nodes found in control file.");
		}
		return;

	}	// end method

	// Reads Type V meta control map.

	private void metaConFileTypeVMap(Connection ourConn, BufferedReader readBuffer, PrintWriter logBuffer,
		String sdtTable, String sidField, String dtField, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id = 0;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{

			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// site id

			site_id = ControlFileUtils.getSiteId(restOfLine);

			// datatype id

			datatype_id = ControlFileUtils.getDatatypeId(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// determine site datatype id

			site_datatype_id = RdbDmiUtils.getSiteDatatypeId(ourConn, site_id, datatype_id,
						sdtTable, sidField, dtField, sdtidField);

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(object_name, slot_name,
							site_id, datatype_id, site_datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in map file.");
			logBuffer.println("No nodes found in map file.");
		}
		return;

	}	// end method

	// Reads Type R RiverWare control file map

	private void metaConFileTypeRMap(Connection ourConn, String dbType, BufferedReader readBuffer,
					PrintWriter logBuffer, String sdtTable, String sdtidField) throws IOException
	{
		String line = null;
		StringTokenizer st;
		String objectSlot = null;
		String object_name = null;
		String slot_name = null;
		int len = 0;
		int site_id = 0;
		int datatype_id = 0;
		int site_datatype_id;
		String description = null;
		String restOfLine = null;

		// read info

		while ((line = readBuffer.readLine()) != null)
		{
			// object and slot

			object_name = line.substring(0, line.indexOf("."));
			slot_name = line.substring((line.indexOf(".") + 1), line.indexOf(": "));
			objectSlot = object_name + "." + slot_name;

			restOfLine = line.substring(line.indexOf(": ") + 2);

			// site datatype id

			site_datatype_id = ControlFileUtils.getSDTID(restOfLine);

			// description

			description = "Data of " + objectSlot;

			// verify site_datatype_id

			site_id = 0;
			datatype_id = 0;
			try
			{
				if (!VerifyRecords.verifyRdbSiteDatatype(ourConn, site_datatype_id, sdtTable, sdtidField, dbType))
				{
					System.out.println("Unable to verify source site datatype id " + site_datatype_id);
					logBuffer.println("Unable to verify source site datatype id " + site_datatype_id);
//					outOfData = true;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				System.out.println("Unable to verify site datatype id " + site_datatype_id);
				logBuffer.println("Unable to verify site datatype id " + site_datatype_id);
//				outOfData = true;
			}

			// add node to Vector of nodes

			nodesData.addElement(new RefDmiModelMap(objectSlot, object_name,
					slot_name, site_datatype_id, description));

		}	// end of loop thru map file

		if (nodesData.size() < 1)
		{
			outOfData = true;
			System.out.println("No nodes found in control file.");
			logBuffer.println("No nodes found in control file.");
		}
		return;

	}	// end method

	// get node map given objectSlot - Case A

	/**
	* Method to get node map given objectSlot - Case A.
	* @param objectSlot Object and slot name.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public RefDmiModelMap getNodeMapGivenObjectSlot(String objectSlot, PrintWriter logBuffer)
											throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{
			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.objectSlot.equals(objectSlot)) break;
			rdmm = null;
		}
		if (rdmm != null) return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate object.slot " + objectSlot + " in map vector.");
		logBuffer.println("Unable to locate object.slot " + objectSlot + " in map vector.");
		return rdmm;

	}	// end method

	/**
	* Method to get node map given object_name and slot_name - Case B
	* @param object Object name.
	* @param slot Slot name.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public RefDmiModelMap getNodeMapGivenObjectAndSlot(String object, String slot,
							PrintWriter logBuffer) throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{

			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.object_name.equals(object) && rdmm.slot_name.equals(slot)) break;
			rdmm = null;

		}
		if (rdmm != null) return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate object and slot "
				+ object + " - " + slot + " in map vector.");
		logBuffer.println("Unable to locate object and slot "
				+ object + " - " + slot + " in map vector.");
		return rdmm;

	}	// end method

	/**
	* Method to get node map given siteNumber and stationName - Case C.
	* @param siteNumber Site number.
	* @param stationName Station name.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public RefDmiModelMap getNodeMapGivenSiteNumberAndStationName(int siteNumber,
				String stationName, PrintWriter logBuffer) throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{
			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.siteNumber == siteNumber && rdmm.stationName.equals(stationName)) break;
			rdmm = null;
		}
		if (rdmm != null)return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate station number and name "
				+ siteNumber + " - " + stationName + " in map vector.");
		logBuffer.println("Unable to locate station number and name "
				+ siteNumber + " - " + stationName + " in map vector.");
		return rdmm;

	}	// end method

	/**
	* Method to get node map given siteNumber and paramNumber - Case D.
	* @param siteNumber Site number.
	* @param paramNumber Parameter number.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public RefDmiModelMap getNodeMapGivenSiteNumberAndParamNumber(int siteNumber,
				int paramNumber , PrintWriter logBuffer) throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{
			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.siteNumber == siteNumber && rdmm.paramNumber == paramNumber)break;
			rdmm = null;
		}
		if (rdmm != null) return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate station number and parameter "
				+ siteNumber + " - " + paramNumber + " in map vector.");
		logBuffer.println("Unable to locate station number and parameter "
				+ siteNumber + " - " + paramNumber + " in map vector.");
		return rdmm;

	}	// end method

	/**
	* Method to get node map given stationName and parameter - Case E
	* @param stationName Station name.
	* @param parameter Parameter.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public RefDmiModelMap getNodeMapGivenStationNameAndParameter(String stationName,
				String parameter, PrintWriter logBuffer) throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{
			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.stationName.equals(stationName) && rdmm.parameter.equals(parameter)) break;
			rdmm = null;
		}
		if (rdmm != null) return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate stationName and parameter "
				+ stationName + " - " + parameter + " in map vector.");
		logBuffer.println("Unable to locate stationName and parameter "
				+ stationName + " - " + parameter + " in map vector.");
		return rdmm;

	}	// end method

	/**
	* Method to get column given objectSlot - Column slot formatting.
	* @param objectSlot Object and slot name.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public RefDmiModelMap getColumnGivenObjectSlot(String objectSlot, PrintWriter logBuffer)
											throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{
			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.objectSlot.equals(objectSlot)) break;
			rdmm = null;
		}
		if (rdmm != null) return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate object.slot " + objectSlot + " in map vector with " + rdmm.objectSlot);
		logBuffer.println("Unable to locate object.slot " + objectSlot + " in map vectorw" + rdmm.objectSlot);
		return rdmm;

	}	// end method

	/**
	* Method to get node map given site datatype id
	* @param sdtid Site datatype id.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public RefDmiModelMap getNodeMapGivenSDTID(int sdtid, PrintWriter logBuffer) throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{
			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.site_datatype_id == sdtid)break;
			rdmm = null;
		}
		if (rdmm != null) return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate site datatype id " + sdtid);
		logBuffer.println("Unable to locate site datatype id " + sdtid);
		return rdmm;

	}	// end method

	/**
	* Method to get node map given second site datatype id
	* @param sdtid Site datatype id.
	* @param logBuffer Dmi output log.
	* @exception IOException
	*/

	public RefDmiModelMap getNodeMapGivenSecSDTID(PrintWriter logBuffer, int sdtid) throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{
			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.siteDatatypeId == sdtid)break;
			rdmm = null;
		}
		if (rdmm != null) return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate site datatype id " + sdtid);
		logBuffer.println("Unable to locate site datatype id " + sdtid);
		return rdmm;

	}	// end method

	/**
	* Method to get node map given site id and datatype id.
	* @param logBuffer Dmi output log.
	* @param sid Site Id.
	* @param dtid Dataype id.
	* @exception IOException
	*/

	public RefDmiModelMap getNodeMapGivenSidAndDtid(PrintWriter logBuffer, int sid, int dtid ) throws IOException
	{
		RefDmiModelMap rdmm = null;
		for (int i = 0; i < nodesData.size(); i++)
		{
			rdmm = (RefDmiModelMap) nodesData.elementAt(i);
			if (rdmm.site_id == sid && rdmm.datatype_id == dtid)break;
			rdmm = null;
		}
		if (rdmm != null) return rdmm;

		// node was not found - holler at them

		System.out.println("Unable to locate site id and datatype id "
				+ sid + " - " + dtid + " in map vector.");
		logBuffer.println("Unable to locate site id and datatype id "
				+ sid + " - " + dtid + " in map vector.");
		return rdmm;

	}	// end method

}	// end class
