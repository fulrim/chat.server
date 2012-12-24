package ru.javatalks.chat.server;

import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static ru.javatalks.chat.server.Server.*;

public class PeerController implements Runnable {
	// "��������� ������". ���� ����� "�������" ��� ������, �� ������� �� ����
	// ������ ����-�����
	final static String SILENT_ERRORS = "Connection reset";
	private Socket s; // ����� ��������� ������
	private String name = null; // ��� �������
	// ��� ������� - �������� � ��������� ���������.
	// ������ ������������ �� ����� ��������� ��������,
	// ������ ���������.
	private BlockingQueue<Message> incomeMessages = new LinkedBlockingQueue<Message>(
			256); // max 256 incomeMessages in queue;
	private BlockingQueue<Message> outgoingMessages = new LinkedBlockingQueue<Message>(
			256); // max 256 incomeMessages in queue

	private Server server; // ��� ������, ������� ������ ��� ���������
							// PeerController'a.
	private Thread pr; // ���� "��������" �� ������.
	private Thread pw; // ���� "��������" � �����.

	/**
	 * ������������ ����������� �������
	 * 
	 * @param serverParam
	 *            - ������, ������� ������ ���� ������
	 * @param socket
	 *            - �������� �����������.
	 */
	public PeerController(Server serverParam, Socket socket) {
		s = socket;
		server = serverParam;
	}

	/**
	 * ���������� ��� �������
	 * 
	 * @return ��� �������
	 */
	public String getName() {
		return name;
	}

	/**
	 * �����, ������� ����� ������ � ������ ������� ���� (�� �������)
	 */
	public void run() {
		// ������� �������� ������, �������� ��� ������ �� ������� ��������
		// ���������,
		// ��������� ��� � ��������� ����������
		final PeerReader target = new PeerReader(this, incomeMessages);
		// ������� ���� �� ���� �������� ������.
		(pr = new Thread(target, "PeerReader")).start();
		// ������� ���� �������� � �����, ��������� ��� ������ �� �������
		// ��������� �� �������� �������.
		(pw = new Thread(new PeerWriter(this, outgoingMessages), "PeerWriter"))
				.start();

		try {
			// ��� ��������� ���� ������ ������ � �������� ����������� �����
			// ��������� �������.
			while (!isClosed() && !Thread.currentThread().isInterrupted()) {
				// � ����� ������� �������� ������� � �� ���� ������ �
				// ������������ ����� 1 �������.
				// �������� ����� ������ � �������,
				Message m = incomeMessages.poll(1, TimeUnit.SECONDS);
				if (m != null) { // ���� ������ ���� �������� � �������� �������
									// � �������,
					// �� �� �� ������ ���������� ����

					String messageStr = m.getMessage(); // �������� ���� ������
					if (messageStr.startsWith(OFFER_NAME)) { // ���������, ���
																// ���������� �
																// ������� �����
																// �����������
																// �����
						name = messageStr.substring(OFFER_NAME.length());// ��������
																			// �������
																			// �����,
																			// ���������
																			// ����
																			// ���.
						if (!server.tryRegister(this)) { // �������
															// ����������������
															// ���� �� �������.
							// ���� �� ���������� - ������� ��������� ��������,
							// ����� �� �������� �������.
							// �� ���� �� ������� ��������� Message � ����� �
							// �������.
							// ��������, ������� �������� �������, �������� ��
							// ������� � ��������.
							outgoingMessages.put(new Message(this, null,
									"Wrong name, try new one"));
						} else { // ����� - ������ ������� ��� ���������������.
							// �������������� ������� �� �������� �����������
							outgoingMessages.put(new Message(this, null,
									REGISTER_SUCCESSFULL));
							// ������� ������ ������� �������� ���������
							// �������.
							final BlockingQueue<Message> serverQueue = server
									.getIncomeQueue();
							// ������������ �������� �� ���������� �������.
							// �� ���� ������ �� ������ ������ �� ����������,
							// �������� ����� ������ ���������
							// � ������� ������������ �����������, ������ ��
							// ����� ������� ���������� ������ ��������
							target.setNewQueue(serverQueue);
							break; // ������� �� ����� while, ������ - ���� ����
									// ��������������� � ��������� �� ������
						}

					} else { // hackers online!! hideaway!!
						// �� �������� �����������, ��� ���������� �������
						// ������ ���������� � OFFER NAME
						// ���� ��� �� ��� - �� �������������� �� ��� ������.
						// �������, ��������� �������.
						System.out.println(m.getMessage());
						this.close();
					}
				}
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	/**
	 * �������� ������� ����� ������. ���� ��� ���������� - PeerController
	 * ����������� � ���������� null
	 * 
	 * @return InputStream or null (if unable to obtain)
	 */
	public InputStream getInputStream() {
		try {
			return s.getInputStream();
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}
		return null;
	}

	/**
	 * �������� �������� ����� ������. ���� ��� ���������� - PeerController
	 * ����������� � ���������� null
	 * 
	 * @return OutputStream or null (if unable to obtain)
	 */

	public OutputStream getOutputStream() {
		try {
			return s.getOutputStream();
		} catch (IOException e) {
			e.printStackTrace();
			close();
		}
		return null;

	}

	/**
	 * ��������� �����, �������, ��������� � �����������
	 */
	public synchronized void close() {
		// if we got close, then...
		pr.interrupt(); // ��������� �������� �� ������� ��������
		pw.interrupt(); // ��������� �������� �� ������� ��������
		if (server.isRegistered(this)) { // ���� �� ���������������� �� �������
			server.unregister(this); // ��������� � �����������
		}
		if (s != null && !s.isClosed()) { // ���� ����� ������� ��� ������ � ��
											// ������
			try {
				s.close(); // ���������
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * ����� send ���������� �� ������������ �����������, ����� �������� �����
	 * ��������� �������
	 * 
	 * @param m
	 *            - ��������� �� �������
	 */
	public void send(Message m) {
		outgoingMessages.offer(m); // ���� ������� �������� � ������� ��������.
	}

	/**
	 * ���������, ��� ����� ������
	 * 
	 * @return �������� �������� ������
	 */
	public boolean isClosed() {
		return s.isClosed();
	}

}