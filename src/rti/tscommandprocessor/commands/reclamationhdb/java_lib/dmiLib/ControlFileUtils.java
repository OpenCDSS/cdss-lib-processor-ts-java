// Provided by Dave King, Reclamation
//package java_lib.dmiLib;
package rti.tscommandprocessor.commands.reclamationhdb.java_lib.dmiLib;

import java.util.StringTokenizer;

//package java_lib.dmiLib;

// control file parsing utilities

public class ControlFileUtils
{

	// no constructor necessary

	/*
	* Method getUnits to return units from control file.
	*/

	public static String getUnits(String line)
	{
		String units = null;
		int loc1 = 0;
		String capsLine = line.toUpperCase();
		int j = -1;

		try
		{
			j = capsLine.indexOf("UNITS");
			if (j > -1)
			{
				loc1 = j + 6;
				if(line.substring(loc1, loc1 + 1).equals("\""))
					units = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
				else if(line.substring(loc1, loc1 + 1).equals("'"))
				{
					units = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
				}
				else
				{
					if (line.indexOf(" ", loc1) >= 0)
					{
						units = line.substring(loc1, line.indexOf(" ", loc1));
					}
					else
					{
						// end of line case

						units = line.substring(loc1);
					}
				}
			}
		}
		finally
		{
		}
		return units;

	}	// end method

	/*
	* Method getFilename to return file name from control file.
	*/

	public static String getFilename(String line)
	{
		String fileName = null;
		int loc1 = 0;
		String capsLine = line.toUpperCase();
		int j = -1;

		try
		{
			j = capsLine.indexOf("FILE");
			if (j > -1)
			{
				loc1 = j + 5;
				if(line.substring(loc1, loc1 + 1).equals("\""))
					fileName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
				else if(line.substring(loc1, loc1 + 1).equals("'"))
				{
					fileName = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
				}
				else
				{
					if (line.indexOf(" ", loc1) >= 0)
					{
						fileName = line.substring(loc1, line.indexOf(" ", loc1));
					}
					else
					{
						// end of line case

						fileName = line.substring(loc1);
					}
				}
			}
		}
		finally
		{

		}
		return fileName;

	}	// end method

	/*
	* Method getObjectType to return object type from control file.
	*/

	public static String getObjectType(String line)
	{
		String objType = null;
		int loc1 = 0;
		String capsLine = line.toUpperCase();
		int j = -1;

		try
		{
			j = capsLine.indexOf("OBJ");
			if (j > -1)
			{
				loc1 = j + 4;
				if(line.substring(loc1, loc1 + 1).equals("\""))
					objType = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
				else if(line.substring(loc1, loc1 + 1).equals("'"))
				{
					objType = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
				}
				else
				{
					if (line.indexOf(" ", loc1) >= 0)
					{
						objType = line.substring(loc1, line.indexOf(" ", loc1));
					}
					else
					{
						// end of line case

						objType = line.substring(loc1);
					}
				}
			}
		}
		finally
		{

		}
		return objType;

	}	// end method

	/*
	* Method getAccount to return object type from control file.
	*/

	public static String getAccount(String line)
	{
		String account = null;
		int loc1 = 0;
		String capsLine = line.toUpperCase();
		int j = -1;

		try
		{
			j = capsLine.indexOf("ACCOUNT");
			if (j > -1)
			{
				loc1 = j + 8;
				if(line.substring(loc1, loc1 + 1).equals("\""))
					account = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
				else if(line.substring(loc1, loc1 + 1).equals("'"))
				{
					account = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
				}
				else
				{
					if (line.indexOf(" ", loc1) >= 0)
					{
						account = line.substring(loc1, line.indexOf(" ", loc1));
					}
					else
					{
						// end of line case

						account = line.substring(loc1);
					}
				}
			}
		}
		finally
		{

		}
		return account;

	}	// end method

	/*
	* Method getScale to return scale from control file.
	*/

	public static double getScale(String line)
	{
		double scale = 1.0;
		StringTokenizer st = new StringTokenizer(line, " ");
		String token = null;

		while (st.hasMoreTokens())
		{
			token = st.nextToken();

			if (token.length() > 5 && token.substring(0, 6).equalsIgnoreCase("scale="))
			{
				scale = Double.valueOf(token.substring(6)).doubleValue();
				return scale;
			}
		}
		return scale;

	}	// end method

	/*
	* Method getSiteId to get HDB site id or other numeric site specification.
	*/

	public static int getSiteId(String line)
	{
		int site_id = 0;
		StringTokenizer st = new StringTokenizer(line, " ");
		String token = null;

		while (st.hasMoreTokens())
		{
			token = st.nextToken();

			if (token.length() > 7 && token.substring(0, 8).equalsIgnoreCase("site_id="))
			{
				site_id = Integer.valueOf(token.substring(8)).intValue();
				return site_id;
			}
			if (token.length() > 10 && token.substring(0, 11).equalsIgnoreCase("siteNumber="))
			{
				site_id = Integer.valueOf(token.substring(11)).intValue();
				return site_id;
			}
		}
		return site_id;

	}	// end method

	/*
	* Method getDataTypeId to get HDB datatype id or other numeric parameter specification.
	*/

	public static int getDatatypeId(String line)
	{
		int datatype_id = 0;
		StringTokenizer st = new StringTokenizer(line, " ");
		String token = null;

		while (st.hasMoreTokens())
		{
			token = st.nextToken();

			if (token.length() > 11 && token.substring(0, 12).equalsIgnoreCase("datatype_id="))
			{
				datatype_id = Integer.valueOf(token.substring(12)).intValue();
				return datatype_id;
			}
			if (token.length() > 11 && token.substring(0, 12).equalsIgnoreCase("paramNumber="))
			{
				datatype_id = Integer.valueOf(token.substring(12)).intValue();
				return datatype_id;
			}
		}
		return datatype_id;

	}	// end method

	/*
	* Method getSDTID to get HDB site datatype id or other numeric unique specification.
	*/

	public static int getSDTID(String line)
	{
		int site_datatype_id = 0;
		StringTokenizer st = new StringTokenizer(line, " ");
		String token = null;

		while (st.hasMoreTokens())
		{
			token = st.nextToken();

			if (token.length() > 5 && token.substring(0, 6).equalsIgnoreCase("sdtid="))
			{
				site_datatype_id = Integer.valueOf(token.substring(6)).intValue();
//				System.out.println("Site datatype id is " + site_datatype_id);
				return site_datatype_id;
			}
			if (token.length() > 16 && token.substring(0, 17).equalsIgnoreCase("site_datatype_id="))
			{
				site_datatype_id = Integer.valueOf(token.substring(17)).intValue();
				return site_datatype_id;
			}
			if (token.length() > 3 && token.substring(0, 4).equalsIgnoreCase("sdi="))
			{
				site_datatype_id = Integer.valueOf(token.substring(4)).intValue();
				return site_datatype_id;
			}
		}
		return site_datatype_id;

	}	// end method

	/*
	* Method getStationName to return stationName
	*/

	public static String getStationName(String line)
	{
		String stationName = null;
		String capsLine = line.toUpperCase();
		int loc1 = 0;
		int j = -1;

		try
		{
			j = capsLine.indexOf("PARTB");
			if (j > -1)
			{
				loc1 = j + 6;
				if(line.substring(loc1, loc1 + 1).equals("\""))
					stationName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
				else if(line.substring(loc1, loc1 + 1).equals("'"))
				{
					stationName = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
				}
				else
				{
					if (line.indexOf(" ", loc1) >= 0)
					{
						stationName = line.substring(loc1, line.indexOf(" ", loc1));
					}
					else
					{
						// end of line case

						stationName = line.substring(loc1);
					}
				}
			}
			else
			{
				j = capsLine.indexOf("BPART");
				if (j > -1)
				{
					loc1 = j + 6;
					if(line.substring(loc1, loc1 + 1).equals("\""))
						stationName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
					if(line.substring(loc1, loc1 + 1).equals("\""))
					{
						stationName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
					}
					else if(line.substring(loc1, loc1 + 1).equals("'"))
					{
						stationName = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
					}
					else
					{
						if (line.indexOf(" ", loc1) >= 0)
						{
							stationName = line.substring(loc1, line.indexOf(" ", loc1));
						}
						else
						{
							// end of line case

							stationName = line.substring(loc1);
						}
					}
				}
				else
				{
					j = capsLine.indexOf("STATIONNAME");
					if (j > -1)
					{
						loc1 = j + 12;
						if(line.substring(loc1, loc1 + 1).equals("\""))
							stationName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
						else if(line.substring(loc1, loc1 + 1).equals("'"))
						{
							stationName = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
						}
						else
						{
							if (line.indexOf(" ", loc1) >= 0)
							{
								stationName = line.substring(loc1, line.indexOf(" ", loc1));
							}
							else
							{
								// end of line case

								stationName = line.substring(loc1);
							}
						}
					}
					else
					{
						j = capsLine.indexOf("OBJECT_NAME");
						if (j > -1)
						{
							loc1 = j + 12;
							if(line.substring(loc1, loc1 + 1).equals("\""))
								stationName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
							if(line.substring(loc1, loc1 + 1).equals("\""))
							{
							stationName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
							}
							else if(line.substring(loc1, loc1 + 1).equals("'"))
							{
								stationName = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
							}
							else
							{
								if (line.indexOf(" ", loc1) >= 0)
								{
									stationName = line.substring(loc1, line.indexOf(" ", loc1));
								}
								else
								{
									// end of line case

									stationName = line.substring(loc1);
								}
							}
						}
						else
						{
							j = capsLine.indexOf("STATION");
							if (j > -1)
							{
								loc1 = j + 8;
								if(line.substring(loc1, loc1 + 1).equals("\""))
									stationName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
								if(line.substring(loc1, loc1 + 1).equals("\""))
								{
								stationName = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
								}
								else if(line.substring(loc1, loc1 + 1).equals("'"))
								{
									stationName = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
								}
								else
								{
									if (line.indexOf(" ", loc1) >= 0)
									{
										stationName = line.substring(loc1, line.indexOf(" ", loc1));
									}
									else
									{
										// end of line case

									stationName = line.substring(loc1);
									}
								}
							}
						}
					}
				}
			}
		}
		finally
		{
//			System.out.println("Station is " + stationName);

		}
		return stationName;

	}	// end method

	/*
	* Method getParameter to return parameter
	*/

	public static String getParameter(String line)
	{
		String parameter = null;
		String capsLine = line.toUpperCase();
		int loc1 = 0;
		int j = -1;

		try
		{
			j = capsLine.indexOf("PARTC");
			if (j > -1)
			{
				loc1 = j + 6;
				if(line.substring(loc1, loc1 + 1).equals("\""))
					parameter = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
				else if(line.substring(loc1, loc1 + 1).equals("'"))
				{
					parameter = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
				}
				else
				{
					if (line.indexOf(" ", loc1) >= 0)
					{
						parameter = line.substring(loc1, line.indexOf(" ", loc1));
					}
					else
					{
						// end of line case

						parameter = line.substring(loc1);
					}
				}
			}
			else
			{
				j = capsLine.indexOf("CPART");
				if (j > -1)
				{
					loc1 = j + 6;
					if(line.substring(loc1, loc1 + 1).equals("\""))
						parameter = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
					if(line.substring(loc1, loc1 + 1).equals("\""))
					{
						parameter = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
					}
					else if(line.substring(loc1, loc1 + 1).equals("'"))
					{
						parameter = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
					}
					else
					{
						if (line.indexOf(" ", loc1) >= 0)
						{
							parameter = line.substring(loc1, line.indexOf(" ", loc1));
						}
						else
						{
							// end of line case

							parameter = line.substring(loc1);
						}
					}
				}
				else
				{
					j = capsLine.indexOf("PARAMETER");
					if (j > -1)
					{
						loc1 = j + 10;
						if(line.substring(loc1, loc1 + 1).equals("\""))
							parameter = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
						else if(line.substring(loc1, loc1 + 1).equals("'"))
						{
							parameter = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
						}
						else
						{
							if (line.indexOf(" ", loc1) >= 0)
							{
								parameter = line.substring(loc1, line.indexOf(" ", loc1));
							}
							else
							{
								// end of line case

								parameter = line.substring(loc1);
							}
						}
					}
					else
					{
						j = capsLine.indexOf("SLOT_NAME");
						if (j > -1)
						{
							loc1 = j + 10;
							if(line.substring(loc1, loc1 + 1).equals("\""))
								parameter = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
							if(line.substring(loc1, loc1 + 1).equals("\""))
							{
							parameter = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
							}
							else if(line.substring(loc1, loc1 + 1).equals("'"))
							{
								parameter = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
							}
							else
							{
								if (line.indexOf(" ", loc1) >= 0)
								{
									parameter = line.substring(loc1, line.indexOf(" ", loc1));
								}
								else
								{
									// end of line case

									parameter = line.substring(loc1);
								}
							}
						}
					}
				}
			}
		}
		finally
		{
//			System.out.println("Parameter is " + parameter);

		}
		return parameter;

	}	// end method

	/*
	* Method getSlotPartA to return partA
	*/

	public static String getSlotPartA(String partA, String line)
	{
		String Apart = partA;
		String capsLine = line.toUpperCase();
		int loc1 = 0;
		int j = -1;

		try
		{
			j = capsLine.indexOf("PARTA");
			if (j > -1)
			{
				loc1 = j + 6;
				if(line.substring(loc1, loc1 + 1).equals("\""))
					Apart = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
				else if(line.substring(loc1, loc1 + 1).equals("'"))
				{
					Apart = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
				}
				else
				{
					if (line.indexOf(" ", loc1) >= 0)
					{
						Apart = line.substring(loc1, line.indexOf(" ", loc1));
					}
					else
					{
						// end of line case

						Apart = line.substring(loc1);
					}
				}
			}
			else
			{
				j = capsLine.indexOf("APART");
				if (j > -1)
				{
					loc1 = j + 6;
					if(line.substring(loc1, loc1 + 1).equals("\""))
						Apart = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
					if(line.substring(loc1, loc1 + 1).equals("\""))
					{
						Apart = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
					}
					else if(line.substring(loc1, loc1 + 1).equals("'"))
					{
						Apart = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
					}
					else
					{
						if (line.indexOf(" ", loc1) >= 0)
						{
							Apart = line.substring(loc1, line.indexOf(" ", loc1));
						}
						else
						{
							// end of line case

							Apart = line.substring(loc1);
						}
					}
				}
			}
		}
		finally
		{
//			System.out.println("Part A is " + Apart);

		}
		return Apart;

	}	// end method

	/*
	* Method getSlotPartF to return partF
	*/

	public static String getSlotPartF(String partF, String line)
	{
		String Fpart = partF;
		String capsLine = line.toUpperCase();
		int loc1 = 0;
		int j = -1;

		try
		{
			j = capsLine.indexOf("PARTF");
			if (j > -1)
			{
				loc1 = j + 6;
				if(line.substring(loc1, loc1 + 1).equals("\""))
					Fpart = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
				else if(line.substring(loc1, loc1 + 1).equals("'"))
				{
					Fpart = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
				}
				else
				{
					if (line.indexOf(" ", loc1) >= 0)
					{
						Fpart = line.substring(loc1, line.indexOf(" ", loc1));
					}
					else
					{
						// end of line case

						Fpart = line.substring(loc1);
					}
				}
			}
			else
			{
				j = capsLine.indexOf("FPART");
				if (j > -1)
				{
					loc1 = j + 6;
					if(line.substring(loc1, loc1 + 1).equals("\""))
						Fpart = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
					if(line.substring(loc1, loc1 + 1).equals("\""))
					{
						Fpart = line.substring(loc1 + 1, line.indexOf("\"", loc1 + 1));
					}
					else if(line.substring(loc1, loc1 + 1).equals("'"))
					{
						Fpart = line.substring(loc1 + 1, line.indexOf("'", loc1 + 1));
					}
					else
					{
						if (line.indexOf(" ", loc1) >= 0)
						{
							Fpart = line.substring(loc1, line.indexOf(" ", loc1));
						}
						else
						{
							// end of line case

							Fpart = line.substring(loc1);
						}
					}
				}
			}
		}
		finally
		{
//			System.out.println("Part F is " + Fpart);

		}
		return Fpart;

	}	// end method

	/*
	* Method getMethodId to get HDB method id.
	*/

	public static int getMethodId(String line)
	{
		int method_id = 0;
		StringTokenizer st = null;
		String token = null;

		try
		{
			st = new StringTokenizer(line, " ");

			while (st.hasMoreTokens())
			{
				token = st.nextToken();

				if (token.length() > 9 && token.substring(0, 10).equalsIgnoreCase("method_id="))
				{
					method_id = Integer.valueOf(token.substring(10)).intValue();
					break;
				}
			}
		}
		finally
		{

		}
		return method_id;
	}	// end method

	/*
	* Method getValueToPost to get constant value to post
	*/

	public static String getValueToPost(String defaultValue, String line)
	{
		String valueToPost = defaultValue;
		StringTokenizer st = null;
		String token = null;

		try
		{
			st = new StringTokenizer(line, " ");

			while (st.hasMoreTokens())
			{
				token = st.nextToken();

				if (token.length() > 11 && token.substring(0, 12).equalsIgnoreCase("valueToPost="))
				{
					valueToPost = token.substring(12);
					break;
				}
			}
		}
		finally
		{

		}
		return valueToPost;
	}	// end method

	/*
	* Method getColumn to return data column from control file.
	*/

	public static int getColumn(String line)
	{
		int column = 0;
		StringTokenizer st = new StringTokenizer(line, " ");
		String token = null;

		while (st.hasMoreTokens())
		{
			token = st.nextToken();
//			System.out.println("Token is " + token);
			if (token.length() > 6 && token.substring(0, 7).equalsIgnoreCase("column="))
			{
				column = Integer.valueOf(token.substring(7)).intValue();
				return column;
			}
		}
		return column;
	}	// end method

	/*
	* Method getDatum to return datum from control file.
	*/

	public static double getDatum(String line)
	{
		double datum = 0.0;
		StringTokenizer st = new StringTokenizer(line, " ");
		String token = null;

		while (st.hasMoreTokens())
		{
			token = st.nextToken();

			if (token.length() > 5 && token.substring(0, 6).equalsIgnoreCase("datum="))
			{
				datum = Double.valueOf(token.substring(6)).doubleValue();
				return datum;
			}
		}
		return datum;

	}	// end method

}	// end class
