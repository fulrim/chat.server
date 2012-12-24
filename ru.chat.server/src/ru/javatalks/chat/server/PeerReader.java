package ru.javatalks.chat.server;

import static ru.javatalks.chat.server.Server.CHARSET;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.SocketException;

/**
 * ����� �������� �� ������. ����� �� ������, ������ ���������� ������, ����� �
 * ������� �������� ���������. ���� �������� ������ ������ - ������� �������
 * PeerController, ���� ����� ������� �� ������ �������� �������.
 */

public class PeerReader implements Runnable {
	final static String SILENT_ERRORS = "Connection reset"; // "��������� ������",
															// �� ������� ��
															// ���� ��������
															// ����-�����
	private BufferedReader br; // ����� ������ �� ������.
	private PeerController pc; // ������ �� PeerController, ������� ������
								// ������� ��������
	// ������� ��������� �� ��������.
	// ������� �� ����� ������ PeerController, ����� CyclicSender
	private BlockingQueue<Message> messages;

	/**
	 * �����������
	 * 
	 * @param pcP
	 *            - ��������� PeerController, ������� ��� ������.
	 * @param messagesP
	 *            - �������, ���� ����� ������ �������� ���������
	 */
	PeerReader(PeerController pcP, BlockingQueue<Message> messagesP) {
		pc = pcP;
		messages = messagesP;
		try {
			// ������������ ����� ������, ��������� ������ charset
			br = new BufferedReader(new InputStreamReader(pc.getInputStream(),
					CHARSET));
		} catch (IOException e) {
			// ���� ���=�� �� ����������..
			if (!pc.isClosed()) {
				pc.close(); // ��������� PeerController;
			}
			e.printStackTrace();
		}
	}

	/**
	 * �����, ������� ����� ������ ����� ������� ���� ��������
	 */
	public void run() {
		String line = "";
		try {
			do { // ����� ������� ���� ������������� �������� ���������
				line = br.readLine(); // ������ ������
				if (line != null && line.length() != 0) { // ���� ��� ����������
															// ������...
					Message m = new Message(pc, null, line); // ... �������
																// ���������
																// ��������� ...
					messages.offer(m); // ������� �������� � ������� ���������,
										// ����� ������
										// PeerController(CyclicSender)
				}
				// ���� �������� ������ ��� ������ ������, � ����� ���� ��������
				// ������������ ���� ���������� ����
			} while (line != null && line.length() != 0
					&& !Thread.currentThread().isInterrupted());
			// ... �� ������� �� �����

		} catch (SocketException e) {
			// ���� �������� ������ ������ (� ���, ������, ��������� ������)
			String message = e.getMessage();
			if (SILENT_ERRORS.indexOf(message) == -1) { // ���� �� �����, �
														// ���-�� �� ������� -
														// �� ���������
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// �� ������ �� �����, ��� ����������� �� ���������, ���
			// �����������.
			if (!pc.isClosed()) { // ������� ������� PeerClient
				pc.close();
			}
		}

	}

	/**
	 * ����� ������ ������� �� ������. ������������ �� PeerController'a, �������
	 * ����������� ������ ������� �������� ��������� CyclicSender'a
	 * 
	 * @param newQueue
	 */
	public synchronized void setNewQueue(BlockingQueue<Message> newQueue) {
		// ����� ��������� ��������, ��� �� ������� �������� ����������� ��
		// ������� ������� �������
		// ������ ����� ��������� �� �������, �������...
		messages.drainTo(newQueue); // ...�������, ���� ����, ��������� � �����
									// �������...
		messages = newQueue; // �������� ������ �� ����� �������.
	}
}