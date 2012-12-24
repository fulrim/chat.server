package ru.javatalks.chat.server;

import java.util.concurrent.BlockingQueue;
import java.nio.charset.Charset;
import java.io.OutputStreamWriter;
import java.io.IOException;

import static ru.javatalks.chat.server.Server.CHARSET;

/**
 * Класс, экземпляр которого прослушивает очередь сообщений на отправку и при
 * получении сообщения - кидает его клиенту Писатель, другими словами.
 */
public class PeerWriter implements Runnable {
	final static String SILENT_ERRORS = "Connection reset";
	private PeerController pc; // контроллер, который нас создал
	private Charset ch = Charset.forName(CHARSET); // единый метод кодирования
													// для сервера и клиента.
	private StringBuilder sb = new StringBuilder(); // буфер, где формируется
													// строка на отправку
	private BlockingQueue<Message> queueToSend; // очередь исходящих сообщений.
												// Ее и слушаем.

	/**
	 * Конструктор объекта
	 * 
	 * @param pcParam
	 *            контроллер, что создал данный объект
	 * @param queueParam
	 *            очередь сообещений для прослушивания
	 */
	public PeerWriter(PeerController pcParam, BlockingQueue<Message> queueParam) {
		pc = pcParam;
		queueToSend = queueParam;
	}

	/**
	 * метод, который получит управление после запуска нити писателя
	 */
	public void run() {
		try {
			// создаем писателя потока на базе сокета.
			OutputStreamWriter osw = new OutputStreamWriter(
					pc.getOutputStream(), ch);
			// цикл, пока нас не прервали и пока сокет не закрыт
			while (!Thread.currentThread().isInterrupted() && !pc.isClosed()) {
				Message m = queueToSend.take(); // курим в ожидании сообещния на
												// отправку.
				sb.setLength(0); // обнуляем буфер строки.
				// далее конструируем новую строку...
				sb.append('[').append(m.getP().getName()).append("] ")
						.append(m.getMessage()).append('\n');
				osw.write(sb.toString());// которую и отправляем
				osw.flush(); // заставляем сокет отправить то, что мы ему
								// передали на отправку
			}
		} catch (IOException e) {
			// при любой ошибке пробуем закрыть PeerController
			if (!pc.isClosed()) {
				pc.close();
			}
			e.printStackTrace();
		} catch (InterruptedException ignored) {

		}
	}
}