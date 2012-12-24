package ru.javatalks.chat.server;

/**
 * Immutable-class. ������ ��������� ��������� �������� ������� ����� ������,
 * ������� ����� ������� � ������� ���������/���������
 */
public class Message {
	private PeerController p; // ������, ������� ������� ��� ���������
	private String header; // ��������� ����������� ��� ��������� ������ �������
							// �� ������. �� ������������
	private String message; // ���� ���������

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