package edu.stanford.smi.protegex.chatPlugin;

import java.util.Collection;

import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.widget.AbstractTabWidget;

/**
 * @author Tania Tudorache <tudorache@stanford.edu>
 *
 */
public class ChatTab extends AbstractTabWidget {
	private ChatComponent chatComponent;
	
	
	public void initialize() {
		setLabel("ChatTab");
		setIcon(Icons.getFormIcon());
				
		if (!getProject().isMultiUserClient()) {
			Log.getLogger().info("You are not in client server. You cannot use the chat tab.");
			return;
		}
	
		chatComponent = new ChatComponent(getKnowledgeBase());		
		add(chatComponent);
	}
	

    public static boolean isSuitable(Project p, Collection errors) {
        if (p.isMultiUserClient()) {
        	return true;
        }        
        errors.add("Works only in client-server mode.");
        return false;
    }
    
    	
	@Override
	public void dispose() {
		chatComponent.dispose();		
		super.dispose();
	}
}
