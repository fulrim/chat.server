package ru.javatalks.chat.server;

import static ru.javatalks.chat.server.Server.CHARSET;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.SocketException;

/**
 *  ласс читател€ из сокета. —идит на сокете, читает полученные данные, ложит в
 * очередь вход€щих сообщений. ≈сли получает ошибку сокета - пробует закрыть
 * PeerController, дабы сн€ть клиента из списка клиентов сервера.
 */

public class PeerReader implements Runnable {
	final static String SILENT_ERRORS = "Connection reset"; // "бесшумные ошибки",
															// на которые не
															// надо печатать
															// стек-трейс
	private BufferedReader br; // буфер чтени€ из сокета.
	private PeerController pc; // ссылка на PeerController, который создал
								// данного читател€
	// очередь сообщений на отправку.
	// сначала ее будут читать PeerController, затем CyclicSender
	private BlockingQueue<Message> messages;

	/**
	 * конструктор
	 * 
	 * @param pcP
	 *            - экземпл€р PeerController, который нас создал.
	 * @param messagesP
	 *            - очередь, куда будем писать вход€щие сообщени€
	 */
	PeerReader(PeerController pcP, BlockingQueue<Message> messagesP) {
		pc = pcP;
		messages = messagesP;
		try {
			// конструируем буфер чтени€, использу€ единый charset
			br = new BufferedReader(new InputStreamReader(pc.getInputStream(),
					CHARSET));
		} catch (IOException e) {
			// если что=то не получилось..
			if (!pc.isClosed()) {
				pc.close(); // закрываем PeerController;
			}
			e.printStackTrace();
		}
	}

	/**
	 * метод, который будет вызван после запуска нити читател€
	 */
	public void run() {
		String line = "";
		try {
			do { // самый главный цикл прослушивани€ вход€щих сообщений
				line = br.readLine(); // читаем строку
				if (line != null && line.length() != 0) { // если это нормальна€
															// строка...
					Message m = new Message(pc, null, line); // ... создаем
																// экземпл€р
																// сообщени€ ...
					messages.offer(m); // пробуем положить в очередь сообщений,
										// чтобы прочли
										// PeerController(CyclicSender)
				}
				// если получили кривую или пустую строку, а также если получили
				// уставнленный флаг прерывани€ нити
			} while (line != null && line.length() != 0
					&& !Thread.currentThread().isInterrupted());
			// ... то выходим из цикла

		} catch (SocketException e) {
			// если получили ошибку сокета (а это, обычно, отвалилс€ клиент)
			String message = e.getMessage();
			if (SILENT_ERRORS.indexOf(message) == -1) { // если не отвал, а
														// что-то не обычное -
														// то отпишемс€
				e.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// по выходу из цикла, вне зависимости от нормально, или
			// ненормально.
			if (!pc.isClosed()) { // пробуем закрыть PeerClient
				pc.close();
			}
		}

	}

	/**
	 * метод замены очереди на другую. »спользуетс€ из PeerController'a, который
	 * подставл€ет единую очередь вход€щих сообщений CyclicSender'a
	 * 
	 * @param newQueue
	 */
	public synchronized void setNewQueue(BlockingQueue<Message> newQueue) {
		// может случитьс€ ситуаци€, что от момента успешной регистрации до
		// момента подмены очереди
		// пришли новые сообщени€ от клиента, поэтому...
		messages.drainTo(newQueue); // ...сбросим, если есть, сообщени€ в новую
									// очередь...
		messages = newQueue; // сохраним ссылку на новую очередь.
	}
}