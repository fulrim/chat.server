package ru.javatalks.chat.server;

/**
 * Immutable-class. Каждое сообщение порождает создание объекта этого класса,
 * который затем ложится в очереди читателей/писателей
 */
public class Message {
	private PeerController p; // клиент, который прислал это сообщение
	private String header; // заголовок задумывался для отдельных команд серверу
							// от админа. не используется
	private String message; // само сообщение

	public Message(PeerController pP, String headerP, String messageP) {
		p = pP;
		header = headerP;
		message = messageP;
	}

	public PeerController getP() {
		return p;
	}

	public String getHeader() {
		return header;
	}

	public String getMessage() {
		return message;
	}

}