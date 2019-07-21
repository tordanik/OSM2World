package org.osm2world.viewer.model;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * receives and stores the messages that are displayed to the user
 */
public class MessageManager { //TODO: isn't this a *model* class...?

	public static final long DEFAULT_MILLISECONDS_TO_LIVE = 3000;

	public static class Message {
		public final String messageString;
		public final long expiration;
		public Message(String messageString, long expiration) {
			this.messageString = messageString;
			this.expiration = expiration;
		}
	}

	private LinkedList<Message> messages = new LinkedList<Message>();

	private void addMessage(String message, long millisecondsToLive) {
		long expiration = System.currentTimeMillis() + millisecondsToLive;
		messages.add(new Message(message, expiration ));
	}

	public void addMessage(String message) {
		addMessage(message, DEFAULT_MILLISECONDS_TO_LIVE);
	}

	public LinkedList<Message> getLiveMessages() {
		removeExpiredMessages();
		return messages;
	}

	private void removeExpiredMessages() {
		long now = System.currentTimeMillis();
		for (Iterator<Message> messageIterator = messages.iterator(); messageIterator.hasNext();) {
			Message message = messageIterator.next();
			if (now > message.expiration) {
				messageIterator.remove();
			}
		}
	}

}