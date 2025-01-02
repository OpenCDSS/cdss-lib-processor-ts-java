// GeoMapCommandType - GeoMap sub-command enumeration

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
GeoMap sub-commands, as enumeration to simplify code.
*/
public enum GeoMapCommandType {
	/**
	Add a layer to a GeoMap.
	*/
	ADD_LAYER ( "AddLayer", "Add a layer to a map." ),

	/**
	Add a layer view (and group) to a GeoMap.
	*/
	ADD_LAYER_VIEW ( "AddLayerView", "Add a layer view to a map." ),

	/**
	Add a layer view group (without the layer view) to a GeoMap.
	*/
	ADD_LAYER_VIEW_GROUP ( "AddLayerViewGroup", "Add a layer view group to a map." ),

	/**
	Copy an existing GeoMap.
	*/
	//COPY ( "Copy", "Copy an existing map." ),

	/**
	Delete an existing GeoMap.
	*/
	//DELETE ( "Delete", "Delete an existing map." ),

	/**
	Create a new GeoMap.
	*/
	NEW_MAP ( "NewMap", "Create a new map for the project." );

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
	private GeoMapCommandType(String name, String description ) {
    	this.name = name;
    	this.description = description;
	}

	/**
	Get the list of command types, in appropriate order.
	@param alphabetize whether to alphabetize choices (true) or use logical order (false)
	@return the list of command types.
	*/
	public static List<GeoMapCommandType> getChoices ( boolean alphabetize ) {
    	List<GeoMapCommandType> choices = new ArrayList<>();
    	if ( alphabetize ) {
    		choices.add ( GeoMapCommandType.ADD_LAYER );
    		choices.add ( GeoMapCommandType.ADD_LAYER_VIEW );
    		choices.add ( GeoMapCommandType.ADD_LAYER_VIEW_GROUP );
    		choices.add ( GeoMapCommandType.NEW_MAP );
    	}
    	else {
    		// Use the order that would be logical.
    		choices.add ( GeoMapCommandType.NEW_MAP );
    		choices.add ( GeoMapCommandType.ADD_LAYER );
    		choices.add ( GeoMapCommandType.ADD_LAYER_VIEW_GROUP );
    		choices.add ( GeoMapCommandType.ADD_LAYER_VIEW );
    	}
    	return choices;
	}

	/**
	Get the list of command type as strings.
	@param alphabetize whether to alphabetize choices (true) or use logical order (false)
	@param includeNote Currently not implemented.
	@return the list of command types as strings.
	*/
	public static List<String> getChoicesAsStrings ( boolean alphabetize, boolean includeNote ) {
    	List<GeoMapCommandType> choices = getChoices ( alphabetize );
    	List<String> stringChoices = new ArrayList<>();
    	for ( int i = 0; i < choices.size(); i++ ) {
        	GeoMapCommandType choice = choices.get(i);
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
	public static GeoMapCommandType valueOfIgnoreCase ( String name ) {
	    if ( name == null ) {
        	return null;
    	}
    	GeoMapCommandType [] values = values();
    	for ( GeoMapCommandType t : values ) {
        	if ( name.equalsIgnoreCase(t.toString()) )  {
            	return t;
        	}
    	}
    	return null;
	}

}