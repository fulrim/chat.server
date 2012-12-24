package ru.javatalks.chat.server;

import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Collection;

/**
 * Server - ����������� ����� ��� �������. �� ������� ��������� �������������,
 * ��������� ������� �� �����������, ������ �� ���� ������ ��������������
 * ��������.
 */

public class Server {
	public static final String OFFER_NAME = "OFFER NAME:"; // ��������� �������
															// ������� �
															// ������������
															// �����.
	public static final String CHARSET = "UTF-16"; // ���������, ������ ���
													// ������� � ��������.
	private ServerSocket ss; // ��������� �����, �������������� ��������
								// �����������

	// ������ �������������� � ��������� ����������� ��������
	// String - ���������� ��� ������� (��, ��� ��������������� � ����)
	// PeerController - ��������� �������, �������� � ���� ����� �����������
	// �������, ��� ��� � �.�.
	private ConcurrentMap<String, PeerController> peers;

	private CyclicSender cs; // ��������� ������-����-������, �����������
								// ���������� ��������� �� ���� ��������
	public static final String REGISTER_SUCCESSFULL = "Register successfull!"; // ������
																				// ���������.

	/**
	 * ������������ �������� �����������. ������ ���������, ����� ����� ����
	 * ������� ������ �� main()
	 */

	private Server() {
		// ������� ��������� ���-���� �� 4 ������������
		// ��� ������������� - ��� �������� ����.
		peers = new ConcurrentHashMap<String, PeerController>(4);
		try {
			ss = new ServerSocket(45000); // ������� ������� ������-����� ��
											// �������������
			cs = new CyclicSender(this); // ������� ��������� ������������
											// �����������.
			final Thread cyclicSenderThread = new Thread(cs, "CyclicSender"); // �������
																				// ����
																				// ��
																				// ���
			// ������������� ��� � ������ (����� ����� �� ���� ���� ������� ���
			// �������������)
			cyclicSenderThread.setDaemon(true);
			cyclicSenderThread.start(); // ��������� �����������
		} catch (IOException e) {
			e.printStackTrace(); // ���� ������ - ��������.
		}
	}

	/**
	 * ������� ���� �������� �������� � ��������� ����������� �������
	 * 
	 */
	void run() {
		while (!Thread.currentThread().isInterrupted()) { // ��� ��������� ��
															// ������� ���������
															// ������� �����
															// ������
			try {
				// ������� ����, ���� �������� �����, ������� ���������
				// PeerController, �������� �������� ������ �� ����
				// � �� ����������� �����.
				PeerController p = new PeerController(this, ss.accept());
				new Thread(p, "PeerController").start(); // ��������� ���������
															// ���������
															// PeerController �
															// ��������� ����

			} catch (IOException e) { // e��� �������� � ������-�������
				e.printStackTrace(); // �������� ����-�����.
				// � ������� ������� ������-����� (�� ������ ������)
				close();
				Thread.currentThread().interrupt(); // � ������ ���� ���������
													// ������.
			}
		}
	}

	/**
	 * ����� ���������� �� ���������� PeerController, ����� �� ������� ��������
	 * ��� ������������. ����� ��������� �������� �� ������������ �������������
	 * ���, � ���� �� - ������������ ��� � ������.
	 * 
	 * @param p
	 *            - PeerController, ���������� ������������������
	 * @return ���� ��� �� ����������� - true, ����� - false
	 */
	boolean tryRegister(PeerController p) {
		return peers.putIfAbsent(p.getName(), p) == null;
	}

	/**
	 * ����� ���������� �� ���������� PeerController, ����� ������������
	 * "���������" ���� ��� ������. ����� ������� ������������ � ����������� �
	 * ������ �������� �������������.
	 * 
	 * @param p
	 *            - PeerController, �������� �������������������
	 * @return - true - �������� ��������������, false - � ������� �� ����.
	 */
	boolean unregister(PeerController p) {
		return peers.remove(p.getName()) == p;
	}

	/**
	 * �����������, ��������������� �� ������ PeerController � ������ ��������
	 * 
	 * @param p
	 *            - ����������� PeerController
	 * @return true - � ������, false - ���.
	 */
	boolean isRegistered(PeerController p) {
		return peers.containsValue(p);
	}

	/**
	 * �������� ������� ��������� �������� (��� ����������� ��������)
	 * 
	 * @return ��������� ��������
	 */
	public Collection<PeerController> getPeers() {
		return peers.values();
	}

	/**
	 * �������� ������ ������� ������� ���� ��������� (�� ������������
	 * �����������). ������ � ��� ������� ��������� (Message) ����� ������������
	 * �������� �� ���� ��������.
	 * 
	 * @return ������� ������� ����������� �����������.
	 */
	public BlockingQueue<Message> getIncomeQueue() {
		return cs.getQueue();
	}

	/**
	 * ����� ����� �������� ������-������.
	 */
	public void close() {
		if (ss != null && !ss.isClosed()) {
			try {
				ss.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * �����������, ���� ������� ������ ������ ������ ������. ���������
	 * ������-�����.
	 * 
	 * @throws Throwable
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	/**
	 * ������� ����� ���������
	 * 
	 * @param args
	 *            - �� ������������
	 */
	public static void main(String[] args) {
		// ������� ������ � �������� ���.
		new Server().run();
	}
}