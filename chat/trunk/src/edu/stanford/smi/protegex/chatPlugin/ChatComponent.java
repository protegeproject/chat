package edu.stanford.smi.protegex.chatPlugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import edu.stanford.smi.protege.event.ClsAdapter;
import edu.stanford.smi.protege.event.ClsEvent;
import edu.stanford.smi.protege.event.ClsListener;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.resource.ResourceKey;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protegex.widget.editorpane.EditorPaneComponent;
import edu.stanford.smi.protegex.widget.editorpane.EditorPaneLinkDetector;


/**
 * @author Tania Tudorache <tudorache@stanford.edu>
 *
 */

public class ChatComponent extends JPanel{
	
	private KnowledgeBase kb;	
	private ChatProjectManager chatProjectManager;
	private JTextComponent chatInputField;
	private EditorPaneLinkDetector chatList;
	private ClsListener clsListener;
	private JTabbedPane tabbedContainer;
		
	public ChatComponent(KnowledgeBase kb) {
		this.kb = kb;
		
		if (kb.getProject().isMultiUserClient()) {		
			chatProjectManager = new ChatProjectManager(kb);		
			buildGUI();		
			initializeListeners();
						
		} else {
			Log.getLogger().info("ChatComponent only works in client-server mode");
		}
	}
	
	protected void initializeListeners() {
		Cls messageCls = chatProjectManager.getMessageCls();
		
		clsListener = new ClsAdapter() {
			@Override
			public void directInstanceAdded(ClsEvent event) {
				Instance messageInstance = event.getInstance();				
				displayChatMessage(messageInstance);
				changeTabTitleDisplay(true);				
			}
		};
		
		if (messageCls != null) {
			messageCls.addClsListener(clsListener);
		}
		
	}

	public void changeTabTitleDisplay(boolean isNewChatLineAvailabe) {		
		if (tabbedContainer == null) {
			//hack
			tabbedContainer = getContainerComponent();
			if (tabbedContainer == null) {
				return;
			}
		}
		
		try {
			JComponent parent = (JComponent) this.getParent();
			int index = tabbedContainer.indexOfComponent(parent);
			
			if (isNewChatLineAvailabe) {
				tabbedContainer.setForegroundAt(index, Color.RED);			
				tabbedContainer.setIconAt(index, Icons.getIcon(new ResourceKey("warning")));
			} else {
				tabbedContainer.setForegroundAt(index, Color.BLACK);			
				tabbedContainer.setIconAt(index, ChatIcons.getSmileyIcon());				
			}
		} catch (Exception e) {
			//do nothing
		}
		
	}
	
	protected void buildGUI(){
		setLayout(new BorderLayout());
		
		chatList = new EditorPaneLinkDetector(false, false);
		chatList.setAutoscrolls(true);
		EditorPaneComponent epc = new EditorPaneComponent();
		chatInputField = epc.createEditorPaneLinkDetector(true, false);
		chatInputField.setBorder(new TitledBorder(""));
		LabeledComponent labelComponent1 = epc.createUI((EditorPaneLinkDetector) chatInputField);
				
		tabbedContainer = getContainerComponent();
		
		LabeledComponent labelComponent = new LabeledComponent("Chat", new JScrollPane(chatList));
		
		JButton sendButton = new JButton("Send");
		Dimension preferredSize = new Dimension(40, 80);
		sendButton.setPreferredSize(preferredSize);
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					chatInputField.getDocument().insertString(chatInputField.getDocument().getLength(), " ", null);
				} catch (BadLocationException ble) {
					if (Log.getLogger().getLevel() == Level.FINE) {
						Log.getLogger().log(Level.FINE, ble.getMessage(), ble);
					}
					return;
				}				
				onSend();					
			}			
		});

		chatInputField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
					try{
						chatInputField.getDocument().insertString(chatInputField.getCaretPosition(), " ", null);
					} catch (BadLocationException ble) {
						if (Log.getLogger().getLevel() == Level.FINE) {
							Log.getLogger().log(Level.FINE, ble.getMessage(), ble);
						}						
					}
					onSend();
				}
			}			
		});
		
		
		JPanel footerPanel = new JPanel();
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.X_AXIS));
		footerPanel.add(labelComponent1);
		footerPanel.add(sendButton);
		labelComponent.setFooterComponent(footerPanel);
		add(labelComponent, BorderLayout.CENTER);
		
	}


	protected void onSend() {		
		String message = chatInputField.getText();
		if (message == null) {
			return;
		}
		

		message = message.trim();
		
		if (message.length() > 0) {			
			Date timestamp = new Date();
			chatProjectManager.createMessageInstance(getKb().getUserName(), message, timestamp);
						
			String colored = "<FONT COLOR=\"#33cc00\">" + "<b>" + 
				getKb().getUserName() + "</b>" + 
					" (" + getTimeString() + "): " + "</FONT> " ;		
			
			String msg =  colored + message; 
			
			chatList.addText(msg);
			chatInputField.grabFocus();
			chatInputField.setText("");
			changeTabTitleDisplay(false);
		}		
	}

	protected void displayChatMessage(Instance messageInstance) {
		String chatMessage = (String) messageInstance.getOwnSlotValue(chatProjectManager.getMessageSlot());
		if (chatMessage == null) {
			return;
		}
		String message = "<FONT COLOR=\"#0033cc\">" + "<b>" + 
			(String)messageInstance.getOwnSlotValue(chatProjectManager.getUserSlot()) + "</b>" + 
				" (" + getTimeString() + "): " + "</FONT> " ; 

		message = message + chatMessage + "\n";
		
		chatList.addText(message);		
	}
		
	protected String getTimeString(){
		GregorianCalendar cal = new GregorianCalendar(TimeZone.getDefault());
		cal.setTime(new Date());
		
	
		int minute = cal.get(Calendar.MINUTE);
		int hour = cal.get(Calendar.HOUR);
					
		String time = ((hour < 10) ? "0":"") + Integer.toString(hour) + ":" + ((minute < 10) ? "0":"") + Integer.toString(minute);
		
		return time;	
	}
	
   
    public KnowledgeBase getKb() {
		return kb;
	}
	

	public void dispose() {
		Cls messageCls = chatProjectManager.getMessageCls();
		
		if (clsListener != null && messageCls != null) {			
			messageCls.removeClsListener(clsListener);
		}
				
		chatProjectManager.getChatKb(getKb()).dispose();
		Log.getLogger().info("Disposed chat project.");
	}
	
	protected JTabbedPane getContainerComponent() {
		if (tabbedContainer != null) {
			return tabbedContainer;
		}
		
		JTabbedPane parent = null;
		
		try {
			parent = (JTabbedPane) this.getParent().getParent();			
		} catch (Exception e) {
			// do nothing
		}
		
		return parent;
	}

	public EditorPaneLinkDetector getChatList() {
		return chatList;
	}
}
