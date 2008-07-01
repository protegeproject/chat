package edu.stanford.smi.protegex.chatPlugin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;

import edu.stanford.smi.protege.event.ClsAdapter;
import edu.stanford.smi.protege.event.ClsEvent;
import edu.stanford.smi.protege.event.ClsListener;
import edu.stanford.smi.protege.model.Cls;
import edu.stanford.smi.protege.model.Instance;
import edu.stanford.smi.protege.model.KnowledgeBase;
import edu.stanford.smi.protege.model.Project;
import edu.stanford.smi.protege.resource.Icons;
import edu.stanford.smi.protege.resource.ResourceKey;
import edu.stanford.smi.protege.util.LabeledComponent;
import edu.stanford.smi.protege.util.Log;
import edu.stanford.smi.protege.util.StringUtilities;
import edu.stanford.smi.protegex.widget.editorpane.EditorPaneComponent;
import edu.stanford.smi.protegex.widget.editorpane.EditorPaneLinkDetector;


/**
 * @author Tania Tudorache <tudorache@stanford.edu>
 *
 */

public class ChatComponent extends JPanel{
	
    private static final int DELAY_MSEC = 2000;
	
	private KnowledgeBase kb;	
	private ClsListener clsListener;	
	private Thread usersUpdateThread;	
	private ChatProjectManager chatProjectManager;
	
	private JTextField usersStatusField;
	private JTextComponent chatInputField;
	private EditorPaneLinkDetector chatList;	
	private JTabbedPane tabbedContainer;

		
	public ChatComponent(KnowledgeBase kb) {
		this.kb = kb;
		
		if (kb.getProject().isMultiUserClient()) {		
			chatProjectManager = new ChatProjectManager(kb);		
			buildGUI();		
			initializeListeners();
			createUpdateThread();
						
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
		
		
		JPanel footerPanel = new JPanel(new BorderLayout(5, 0));
		footerPanel.add(labelComponent1, BorderLayout.CENTER);
		
		JPanel sendButtonPanel = new JPanel(new BorderLayout());
		sendButtonPanel.add(sendButton, BorderLayout.SOUTH);
		
		footerPanel.add(sendButtonPanel, BorderLayout.EAST);
		labelComponent.setFooterComponent(footerPanel);
		//add(labelComponent, BorderLayout.CENTER);
		
		usersStatusField = new JTextField();
		usersStatusField.setEditable(false);
		
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(labelComponent, BorderLayout.CENTER);
		mainPanel.add(usersStatusField, BorderLayout.SOUTH);
		
		add(mainPanel);		
	}


	protected void onSend() {		
		String message = chatInputField.getText();
		if (message == null) {
			return;
		}		

		message = message.trim();

		if (message.length() == 0) {
			return;
		}

		Date timestamp = new Date();
		chatProjectManager.createMessageInstance(getKb().getUserName(), message, timestamp);

		String colored = "<FONT COLOR=\"" + getColor(getKb().getUserName()) + "\">" + "<b>" + 
		getKb().getUserName() + "</b>" + 
		" (" + getTimeString() + "): " + "</FONT> " ;		

		String msg =  colored + message; 

		chatList.addText(msg);
		chatInputField.grabFocus();
		chatInputField.setText("");
		changeTabTitleDisplay(false);
	}		


	protected void displayChatMessage(Instance messageInstance) {
		String chatMessage = (String) messageInstance.getOwnSlotValue(chatProjectManager.getMessageSlot());
		if (chatMessage == null) {
			return;
		}
		
		String userName = (String)messageInstance.getOwnSlotValue(chatProjectManager.getUserSlot());
		
		String message = "<FONT COLOR=\"" + getColor(userName) + "\">" + "<b>" + userName +
			"</b>" + " (" + getTimeString() + "): " + "</FONT> " ; 

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
	
   
	protected String getColor(String userName) {
		return (userName.equals(getKb().getUserName())) ? "#993300" : "#0033cc";
	}
	
    public KnowledgeBase getKb() {
		return kb;
	}
	

	public void dispose() {
		usersUpdateThread = null;
		
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
	
	
    private void createUpdateThread() {
        usersUpdateThread = new Thread("Chat Users Status Updater") {
            public void run() {
              try {
                while (usersUpdateThread == this) {
                    try {
                        sleep(DELAY_MSEC);
                        updateUsersStatus();
                    } catch (InterruptedException e) {
                      Log.getLogger().log(Level.INFO, "Exception caught", e);
                    }
                }
              } catch (Throwable t) {
                Log.getLogger().log(Level.INFO, "Exception caught",t);
              }
            }
        };
        usersUpdateThread.setDaemon(true);
        usersUpdateThread.start();
    }

    //TODO: Fix later for race conditions
    protected void updateUsersStatus() {
    	Project prj = kb.getProject();
   	
    	if (prj == null) {
    		return;
    	}
    	
    	Collection users = new ArrayList(prj.getCurrentUsers());
    	users.remove(prj.getLocalUser());
    	String userText = StringUtilities.commaSeparatedList(users);
    	String text;
    	if (userText.length() == 0) {
    		text = "No other users";
    	} else {
    		text = "Other users: " + userText;
    	}
    	usersStatusField.setText(text);

    }
    
    
	
}
