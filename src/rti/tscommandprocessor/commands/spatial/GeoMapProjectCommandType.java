// GeoMapProjectCommandType - GeoMapProject sub-command enumeration

/* NoticeStart

CDSS Time Series Processor Java Library is a part of Colorado's Decision Support Systems (CDSS)
Copyright (C) 1994-2024 Colorado Department of Natural Resources

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

package rti.tscommandprocessor.commands.spatial;

import java.util.ArrayList;
import java.util.List;

/**
GeoMapProject sub-commands, as enumeration to simplify code.
*/
public enum GeoMapProjectCommandType {
	/**
	Copy an existing GeoMap project.
	*/
	COPY ( "Copy", "Copy an existing project." ),

	/**
	Delete an existing GeoMap project.
	*/
	DELETE ( "Delete", "Delete an existing project." ),

	/**
	Create a new GeoMap project.
	*/
	NEW_PROJECT ( "NewProject", "Create a new project." ),

	/**
	Read an existing GeoMap project from a JSON file.
	*/
	READ ( "Read", "Read an existing project from a JSON file." ),

	/**
	Write an existing GeoMap project to a JSON file.
	*/
	WRITE ( "Write", "Write an existing project to a JSON file." );

	/**
	The name that is used for choices and other technical code (terse).
	*/
	private final String name;

	/**
	The description, useful for UI notes.
	*/
	private final String description;

	/**
	Construct an enumeration value.
	@param name name that should be displayed in choices, etc.
	@param descritpion command description.
	*/
	private GeoMapProjectCommandType(String name, String description ) {
    	this.name = name;
    	this.description = description;
	}

	/**
	Get the list of command types, in appropriate order.
	@param alphabetical return alphabetical choices (true) or logical order (false)
	@return the list of command types.
	*/
	public static List<GeoMapProjectCommandType> getChoices ( boolean alphabetical ) {
    	List<GeoMapProjectCommandType> choices = new ArrayList<>();
    	if ( alphabetical ) {
    		choices.add ( GeoMapProjectCommandType.COPY );
    		choices.add ( GeoMapProjectCommandType.DELETE );
    		choices.add ( GeoMapProjectCommandType.NEW_PROJECT );
    		choices.add ( GeoMapProjectCommandType.READ );
    		choices.add ( GeoMapProjectCommandType.WRITE );
    	}
    	else {
    		choices.add ( GeoMapProjectCommandType.NEW_PROJECT );
    		choices.add ( GeoMapProjectCommandType.COPY );
    		choices.add ( GeoMapProjectCommandType.READ );
    		choices.add ( GeoMapProjectCommandType.DELETE );
    		choices.add ( GeoMapProjectCommandType.WRITE );
    	}
    	return choices;
	}

	/**
	Get the list of command type as strings.
	@param alphabetical return alphabetical choices (true) or logical order (false)
	@param includeNote return choices with note (e.g., "Copy - copy a project") - currently not implemented.
	@return the list of command types as strings.
	*/
	public static List<String> getChoicesAsStrings ( boolean alphabetical, boolean includeNote ) {
    	List<GeoMapProjectCommandType> choices = getChoices ( alphabetical );
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	GeoMapProjectCommandType choice = choices.get(i);
        	String choiceString = "" + choice;
        	//if ( includeNote ) {
            //	choiceString = choiceString + " - " + choice.toStringVerbose();
        	//}
        	stringChoices.add ( choiceString );
    	}
    	return stringChoices;
	}

	/**
	Return the command name for the type.  This is the same as the value.
	@return the display name.
	*/
	@Override
	public String toString() {
    	return this.name;
	}

	/**
	Return the enumeration value given a string name (case-independent).
	@param name the name to match
	@return the enumeration value given a string name (case-independent), or null if not matched.
	*/
	public static GeoMapProjectCommandType valueOfIgnoreCase ( String name ) {
	    if ( name == null ) {
        	return null;
    	}
    	GeoMapProjectCommandType [] values = values();
    	for ( GeoMapProjectCommandType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}