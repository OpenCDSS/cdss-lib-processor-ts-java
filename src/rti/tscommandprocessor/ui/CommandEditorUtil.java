package rti.tscommandprocessor.ui;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Vector;

import java.awt.event.ItemListener;
import java.awt.event.KeyListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import rti.tscommandprocessor.core.TSListType;
import RTi.Util.GUI.JGUIUtil;
import RTi.Util.GUI.SimpleJComboBox;

/**
This class provides methods for manipulating UI components of command editors.
Its intent is to centralize commonly used UI code.
*/
public abstract class CommandEditorUtil
{

/**
Add the EnsembleID parameter components to a command dialog.
It is assumed that GridBagLayout is used for the layout.
@param dialog The dialog that is being added to.
@param panel The JPanel to which the controls are being added.
@param label Label for the EnsembleID component (typically can be enabled/disabled elsewhere).
@param choices Choices for the EnsembleID, as String.
@param y The GridBagLayout vertical position.
@return Incremented y reflecting the addition of a new vertical component group.
*/
public static int addEnsembleIDToEditorDialogPanel ( ItemListener itemlistener, KeyListener keylistener,
        JPanel panel, JLabel label, SimpleJComboBox choices, Vector EnsembleIDs, int y )
{
    Insets insetsTLBR = new Insets(2,2,2,2);
    JGUIUtil.addComponent(panel, label,
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    int size = 0;
    if ( EnsembleIDs == null ) {
        EnsembleIDs = new Vector ();
    }
    size = EnsembleIDs.size();
    // Blank for default (not used)
    if ( size > 0 ) {
        EnsembleIDs.insertElementAt ( "", 0 );
    }
    else {
        EnsembleIDs.addElement ( "" );
    }
    choices.setData ( EnsembleIDs );
    choices.addItemListener ( itemlistener );
    choices.addKeyListener ( keylistener );
        JGUIUtil.addComponent(panel, choices,
        1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    return y;
}

/**
Add the TSID parameter components to a command dialog.  A "*" will automatically be added.
It is assumed that GridBagLayout is used for the layout.
@param dialog The dialog that is being added to.
@param panel The JPanel to which the controls are being added.
@param label Label for the TSID component (typically can be enabled/disabled elsewhere).
@param choices Choices for the TSID, as String.
@param y The GridBagLayout vertical position.
@return Incremented y reflecting the addition of a new vertical component group.
*/
public static int addTSIDToEditorDialogPanel ( ItemListener itemlistener, KeyListener keylistener,
        JPanel panel, JLabel label, SimpleJComboBox choices, Vector tsids, int y )
{
    return addTSIDToEditorDialogPanel ( itemlistener, keylistener,
            panel, label, choices, tsids, y, true );
}
    
/**
Add the TSID parameter components to a command dialog.
It is assumed that GridBagLayout is used for the layout.
@param dialog The dialog that is being added to.
@param panel The JPanel to which the controls are being added.
@param label Label for the TSID component (typically can be enabled/disabled elsewhere).
@param choices Choices for the TSID, as String.
@param y The GridBagLayout vertical position.
@param add_asterisk If true, a "*" will be added to the list.
@return Incremented y reflecting the addition of a new vertical component group.
*/
public static int addTSIDToEditorDialogPanel ( ItemListener itemlistener, KeyListener keylistener,
        JPanel panel, JLabel label, SimpleJComboBox choices, Vector tsids, int y, boolean add_asterisk )
{
    Insets insetsTLBR = new Insets(2,2,2,2);
    JGUIUtil.addComponent(panel, label,
            0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    int size = 0;
    if ( tsids == null ) {
        tsids = new Vector ();
    }
    size = tsids.size();
    // Blank for default
    if ( size > 0 ) {
        tsids.insertElementAt ( "", 0 );
    }
    else {
        tsids.addElement ( "" );
    }
    if ( add_asterisk ) {
        // Add a "*" to let all time series be filled (put at end)...
        tsids.addElement ( "*" );
    }
    choices.setData ( tsids );
    choices.addItemListener ( itemlistener );
    choices.addKeyListener ( keylistener );
    JGUIUtil.addComponent(panel, choices,
    1, y, 6, 1, 1, 0, insetsTLBR, GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
    return y;
}

/**
Add the TSList parameter components to a command dialog.
It is assumed that GridBagLayout is used for the layout.
@param dialog The dialog that is being added to.
@param panel The JPanel to which the controls are being added.
@param y The GridBagLayout vertical position.
@return Incremented y reflecting the addition of a new vertical component group.
*/
public static int addTSListToEditorDialogPanel (
        ItemListener dialog, JPanel panel, SimpleJComboBox choices, int y )
{
    return addTSListToEditorDialogPanel ( dialog, panel, null, choices, y );
}

/**
Add the TSList parameter components to a command dialog.
It is assumed that GridBagLayout is used for the layout.
@param dialog The dialog that is being added to.
@param panel The JPanel to which the controls are being added.
@param label Label for the row.  If null a default will be provided.
@param y The GridBagLayout vertical position.
@return Incremented y reflecting the addition of a new vertical component group.
*/
public static int addTSListToEditorDialogPanel (
        ItemListener dialog, JPanel panel, JLabel label, SimpleJComboBox choices, int y )
{
    Insets insetsTLBR = new Insets(2,2,2,2);
    if ( label == null ) {
        label = new JLabel ("TS list:");
    }
    JGUIUtil.addComponent(panel, label, 0, ++y, 1, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.EAST);
    Vector TSList_Vector = new Vector();
    TSList_Vector.addElement ( "" );
    TSList_Vector.addElement ( TSListType.ALL_MATCHING_TSID.toString() );
    TSList_Vector.addElement ( TSListType.ALL_TS.toString() );
    TSList_Vector.addElement ( TSListType.ENSEMBLE_ID.toString() );
    TSList_Vector.addElement ( TSListType.LAST_MATCHING_TSID.toString() );
    TSList_Vector.addElement ( TSListType.SELECTED_TS.toString() );
    
    choices.setData ( TSList_Vector );
    choices.addItemListener (dialog);
    JGUIUtil.addComponent(panel, choices,
        1, y, 2, 1, 1, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST);
    JGUIUtil.addComponent(panel, new JLabel (
        "Indicates the time series to process (default=AllTS)."),
        3, y, 3, 1, 0, 0, insetsTLBR, GridBagConstraints.NONE, GridBagConstraints.WEST );
    return y;
}

}
