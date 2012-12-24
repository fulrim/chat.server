package ru.javatalks.chat.server;

import java.util.concurrent.BlockingQueue;
import java.nio.charset.Charset;
import java.io.OutputStreamWriter;
import java.io.IOException;

import static ru.javatalks.chat.server.Server.CHARSET;

/**
 * �����, ��������� �������� ������������ ������� ��������� �� �������� � ���
 * ��������� ��������� - ������ ��� ������� ��������, ������� �������.
 */
public class PeerWriter implements Runnable {
	final static String SILENT_ERRORS = "Connection reset";
	private PeerController pc; // ����������, ������� ��� ������
	private Charset ch = Charset.forName(CHARSET); // ������ ����� �����������
													// ��� ������� � �������.
	private StringBuilder sb = new StringBuilder(); // �����, ��� �����������
													// ������ �� ��������
	private BlockingQueue<Message> queueToSend; // ������� ��������� ���������.
												// �� � �������.

	/**
	 * ����������� �������
	 * 
	 * @param pcParam
	 *            ����������, ��� ������ ������ ������
	 * @param queueParam
	 *            ������� ���������� ��� �������������
	 */
	public PeerWriter(PeerController pcParam, BlockingQueue<Message> queueParam) {
		pc = pcParam;
		queueToSend = queueParam;
	}

	/**
	 * �����, ������� ������� ���������� ����� ������� ���� ��������
	 */
	public void run() {
		try {
			// ������� �������� ������ �� ���� ������.
			OutputStreamWriter osw = new OutputStreamWriter(
					pc.getOutputStream(), ch);
			// ����, ���� ��� �� �������� � ���� ����� �� ������
			while (!Thread.currentThread().isInterrupted() && !pc.isClosed()) {
				Message m = queueToSend.take(); // ����� � �������� ��������� ��
												// ��������.
				sb.setLength(0); // �������� ����� ������.
				// ����� ������������ ����� ������...
				sb.append('[').append(m.getP().getName()).append("] ")
						.append(m.getMessage()).append('\n');
				osw.write(sb.toString());// ������� � ����������
				osw.flush(); // ���������� ����� ��������� ��, ��� �� ���
								// �������� �� ��������
			}
		} catch (IOException e) {
			// ��� ����� ������ ������� ������� PeerController
			if (!pc.isClosed()) {
				pc.close();
			}
			e.printStackTrace();
		} catch (InterruptedException ignored) {

		}
	}
}