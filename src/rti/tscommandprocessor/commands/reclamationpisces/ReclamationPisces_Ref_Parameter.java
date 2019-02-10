// ReclamationPisces_Ref_Parameter - Hold data from the Reclamation Pisces database that for the ref_parameter table.

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

package rti.tscommandprocessor.commands.reclamationpisces;

/**
Hold data from the Reclamation Pisces database that for the ref_parameter table.
*/
public class ReclamationPisces_Ref_Parameter
{

private String id = "";
private String description = "";

/**
Constructor.
*/
public ReclamationPisces_Ref_Parameter ()
{   super();
}

public String getDescription ()
{
	return this.description;
}

public String getID ()
{
	return this.id;
}

public void setDescription ( String description )
{
    this.description = description;
}

public void setID ( String id )
{
    this.id = id;
}

}
