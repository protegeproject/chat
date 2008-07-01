package edu.stanford.smi.protegex.chatPlugin;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Date;

import edu.stanford.smi.protege.model.BrowserSlotPattern;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.DefaultKnowledgeBase;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.model.Slot;
import edu.stanford.smi.protege.model.framestore.FrameStoreManager;
import edu.stanford.smi.protege.server.RemoteProjectManager;
import edu.stanford.smi.protege.server.RemoteServer;
import edu.stanford.smi.protege.server.RemoteServerProject;
import edu.stanford.smi.protege.server.RemoteSession;
import edu.stanford.smi.protege.server.framestore.RemoteClientFrameStore;
import edu.stanford.smi.protege.util.Log;

/**
 * @author Tania Tudorache <tudorache@stanford.edu>
 *
 */
public class ChatProjectManager {
	public final static String SERVER_PROJ_NAME = "ChatProject";
	
	public final static String CLS_MESSAGE = "Message";
	public final static String SLOT_MESSAGE = "message";
	public final static String SLOT_USER = "user";
	public final static String SLOT_TIMESTAMP = "timestamp";
	
	private KnowledgeBase chatKb;

	private Cls  messageCls;
	private Slot userSlot;
	private Slot messageSlot;
	private Slot timestampSlot;
		
	
	public ChatProjectManager(KnowledgeBase kb) {		
		this.chatKb = getChatKb(kb);
	}
	
	protected void initializeChatKb(){
		
		messageCls = createCls(CLS_MESSAGE);
		userSlot = createSlot(SLOT_USER);
		messageSlot = createSlot(SLOT_MESSAGE);
		timestampSlot = createSlot(SLOT_TIMESTAMP);
		
		attachSlotToClass(messageCls, userSlot);
		attachSlotToClass(messageCls, messageSlot);
		attachSlotToClass(messageCls, timestampSlot);
		
		fixBrowserSlots();
	}
	
	@SuppressWarnings("unchecked")
	private void fixBrowserSlots() {
		ArrayList browserSlotElems = new ArrayList();
		browserSlotElems.add(getUserSlot());
		browserSlotElems.add(" (");
		browserSlotElems.add(getTimestampSlot());
		browserSlotElems.add("): ");
		browserSlotElems.add(getMessageSlot());
		
		getMessageCls().setDirectBrowserSlotPattern(new BrowserSlotPattern(browserSlotElems));		
	}

	private Cls createCls(String name) {
		Cls cls = chatKb.getCls(name);
		
		if (cls != null) {
			return cls;
		}
		
		return chatKb.createCls(name, chatKb.getRootClses());
	}
	
	private Slot createSlot(String name) {
		Slot slot = chatKb.getSlot(name);
		
		if (slot != null) {
			return slot;
		}
		
		return chatKb.createSlot(name);
	}
	
	private void attachSlotToClass(Cls cls, Slot slot) {
		if (!cls.hasTemplateSlot(slot)) {
			cls.addDirectTemplateSlot(slot);
		}
	}
	
	public Cls getMessageCls() {
		if (messageCls == null) {
			messageCls = chatKb.getCls(CLS_MESSAGE); 
		}
		
		return messageCls;
	}
	
	public Slot getUserSlot() {
		if (userSlot == null) {
			userSlot = chatKb.getSlot(SLOT_USER); 
		}
		return userSlot;
	}

	public Slot getMessageSlot() {
		if (messageSlot == null) {
			messageSlot = chatKb.getSlot(SLOT_MESSAGE); 
		}
		return messageSlot;
	}

	public Slot getTimestampSlot() {
		if (timestampSlot == null) {
			timestampSlot = chatKb.getSlot(SLOT_TIMESTAMP); 
		}
		return timestampSlot;
	}
	
	public Instance createMessageInstance(String user, String message, Date timestamp) {		
		Instance messageInstance = getMessageCls().createDirectInstance(null);		
		messageInstance.setOwnSlotValue(getMessageSlot(), message);
		messageInstance.setOwnSlotValue(getUserSlot(), user);
		messageInstance.setOwnSlotValue(getTimestampSlot(), timestamp.toString());
		
		return messageInstance;
	}
	
	

	public KnowledgeBase getChatKb(KnowledgeBase kb) {
		if (chatKb != null) {
			return chatKb;
		}
		
        FrameStoreManager fsmanager = ((DefaultKnowledgeBase) kb).getFrameStoreManager();
        RemoteClientFrameStore rcfs = (RemoteClientFrameStore) fsmanager.getFrameStoreFromClass(RemoteClientFrameStore.class);
        RemoteServer server = rcfs.getRemoteServer();
        
        RemoteSession session = rcfs.getSession();
        try {
            session = server.cloneSession(session);
        } catch (RemoteException e) {
            Log.getLogger().warning("Error at creating session clone "+ rcfs.getSession() + ". Error: " + e);
            return null;
        }
        
        Project chatProject = RemoteProjectManager.getInstance().connectToProject(server, session, SERVER_PROJ_NAME);
        
        if (chatProject != null) {
        	chatKb = chatProject.getKnowledgeBase();
        	fixBrowserSlots();
        	
        	Log.getLogger().info("Connected to server chat project on server.");
        	return chatKb;
        }

        
		
		try {
			//Try to reuse the cloned session, because it was not really used before
			//server.closeSession(session);
			
			session = server.cloneSession(rcfs.getSession());
			
			RemoteServerProject serverProject = server.createProject(SERVER_PROJ_NAME, session, null, false);		
			chatProject = RemoteProjectManager.getInstance().connectToProject(server, session, SERVER_PROJ_NAME);
			//Log.getLogger().info("Created successfully chat project on server");
		} catch (Throwable e) {
			Log.getLogger().warning("Error at creating chat project on server. Project name: " + SERVER_PROJ_NAME);
		}
		
		if (chatProject != null) {
			chatKb = chatProject.getKnowledgeBase();
			initializeChatKb();
		}
        
		return chatKb;
			
	}


}
