package edu.stanford.smi.protegex.chatPlugin;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Date;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import edu.stanford.smi.protege.event.ClsAdapter;
import edu.stanford.smi.protege.event.ClsEvent;
import edu.stanford.smi.protege.event.ClsListener;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.ui.FrameRenderer;
import edu.stanford.smi.protege.util.ComponentUtilities;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;

/**
 * @author Tania Tudorache <tudorache@stanford.edu>
 *
 */
public class ChatComponent extends JPanel{
	
	private KnowledgeBase kb;	
	private ChatProjectManager chatProjectManager;

	private JTextComponent chatInputField;
	private JList chatList;
	
	private ClsListener clsListener;
	
	
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
			}
		};
		
		if (messageCls != null) {
			messageCls.addClsListener(clsListener);
		}
	}

	protected void buildGUI(){
		setLayout(new BorderLayout());
		
		chatList = new JList();
		chatList.setCellRenderer(new FrameRenderer());		
		chatInputField = new JTextField();		
		LabeledComponent labelComponent = new LabeledComponent("Chat", new JScrollPane(chatList));
		
		JButton sendButton = new JButton("Send");
		sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onSend();					
			}			
		});
		
		chatInputField.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent arg0) {
				if (arg0.getKeyCode() == KeyEvent.VK_ENTER) {
					onSend();
				}
			}			
		});
		
		JPanel footerPanel = new JPanel();
		footerPanel.setLayout(new BoxLayout(footerPanel, BoxLayout.X_AXIS));
		footerPanel.add(chatInputField);
		footerPanel.add(sendButton);		
		
		labelComponent.setFooterComponent(footerPanel);
		
		add(labelComponent, BorderLayout.CENTER);
	}


	protected void onSend() {
		String message = chatInputField.getText();
		message = message.trim();
		if (message != null || message.length() > 0) {
			chatProjectManager.createMessageInstance(getKb().getUserName(), message, new Date());
			chatInputField.setText("");
		}
	}

	protected void displayChatMessage(Instance messageInstance) {
		String message = ((String)messageInstance.getOwnSlotValue(chatProjectManager.getUserSlot()));
		message = message + " (" +  ((String)messageInstance.getOwnSlotValue(chatProjectManager.getTimestampSlot())) + "): ";
		message = message + ((String)messageInstance.getOwnSlotValue(chatProjectManager.getMessageSlot())) + "\n";

		ComponentUtilities.addListValue(chatList, messageInstance);
		chatList.setSelectedValue(messageInstance, true);		
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

	public JList getChatList() {
		return chatList;
	}
}
