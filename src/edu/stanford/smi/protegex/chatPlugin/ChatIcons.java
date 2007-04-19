package edu.stanford.smi.protegex.chatPlugin;

import java.util.HashMap;

import javax.swing.Icon;

import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.util.ComponentUtilities;

/**
 * @author Tania Tudorache <tudorache@stanford.edu>
 *
 */
public class ChatIcons {
	private static HashMap<String, Icon> key2icon = new HashMap<String, Icon>();
		
    private static Icon lookupIcon(String key) {
        Icon icon = (Icon) key2icon.get(key);
        if (icon == null || icon.getIconWidth() == -1) {
            String fileName = key.toString() + ".gif";
            icon = ComponentUtilities.loadImageIcon(ChatIcons.class, "images/" + fileName);
            key2icon.put(key, icon);
        }
        return icon;
    }
    	
    public static Icon getIcon(String key) {
        Icon icon = lookupIcon(key);
        if (icon == null) {
            icon = Icons.getUglyIcon();
        }
        return icon;
    }

    public static Icon getSmileyIcon() {
		return getIcon("smiley");
	}
	
}