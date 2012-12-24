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
	// "бесшумные ошибки". Сюда можно "свалить" все ошибки, по которым не надо
	// печать стек-трейс
	final static String SILENT_ERRORS = "Connection reset";
	private Socket s; // сокет входящего вызова
	private String name = null; // имя клиента
	// две очереди - входящих и исходящих сообщений.
	// первая используется во время установки коннекта,
	// вторая постоянно.
	private BlockingQueue<Message> incomeMessages = new LinkedBlockingQueue<Message>(
			256); // max 256 incomeMessages in queue;
	private BlockingQueue<Message> outgoingMessages = new LinkedBlockingQueue<Message>(
			256); // max 256 incomeMessages in queue

	private Server server; // это сервер, который создал наш экземпляр
							// PeerController'a.
	private Thread pr; // нить "читателя" из сокета.
	private Thread pw; // нить "писателя" в сокет.

	/**
	 * единственный конструктор объекта
	 * 
	 * @param serverParam
	 *            - сервер, который создал этот объект
	 * @param socket
	 *            - входящее подключение.
	 */
	public PeerController(Server serverParam, Socket socket) {
		s = socket;
		server = serverParam;
	}

	/**
	 * Уникальное имя клиента
	 * 
	 * @return имя клиента
	 */
	public String getName() {
		return name;
	}

	/**
	 * метод, который будет вызван в момент запуска нити (из сервера)
	 */
	public void run() {
		// создаем читателя сокета, передаем ему ссылку на очередь входящих
		// сообщений,
		// сохраняем его в локальной переменной
		final PeerReader target = new PeerReader(this, incomeMessages);
		// создаем нить на базе читателя сокета.
		(pr = new Thread(target, "PeerReader")).start();
		// создаем нить писателя в сокет, передавая ему ссылку на очередь
		// сообщений на отправку клиенту.
		(pw = new Thread(new PeerWriter(this, outgoingMessages), "PeerWriter"))
				.start();

		try {
			// это первичный цикл опроса сокета в ожидании предложения имени
			// входящего клиента.
			while (!isClosed() && !Thread.currentThread().isInterrupted()) {
				// к этому моменту читатель запущен и мы ждем строку с
				// предложением имени 1 секунду.
				// читатель кинет строку в очередь,
				Message m = incomeMessages.poll(1, TimeUnit.SECONDS);
				if (m != null) { // если строка была передана и читатель положил
									// в очередь,
					// то мы ее прочли оператором выше

					String messageStr = m.getMessage(); // получаем саму строку
					if (messageStr.startsWith(OFFER_NAME)) { // проверяем, что
																// начинается с
																// кодовой фразы
																// предложения
																// имени
						name = messageStr.substring(OFFER_NAME.length());// отрезаем
																			// кодовую
																			// фразу,
																			// оставляем
																			// само
																			// имя.
						if (!server.tryRegister(this)) { // пробуем
															// зарегистрировать
															// себя на сервере.
							// если не получилось - отошлем сообщение писателю,
							// чтобы он отправил клиенту.
							// то есть мы создаем экземпляр Message и ложим в
							// очередь.
							// писатель, который караулит очередь, извлечет из
							// очереди и отправит.
							outgoingMessages.put(new Message(this, null,
									"Wrong name, try new one"));
						} else { // иначе - сервер успешно нас зарегистрировал.
							// проинформируем клиента об успешной регистрации
							outgoingMessages.put(new Message(this, null,
									REGISTER_SUCCESSFULL));
							// получим единую очередь входящих сообщений
							// сервера.
							final BlockingQueue<Message> serverQueue = server
									.getIncomeQueue();
							// перенаправим читателя на полученную очередь.
							// то есть дальше мы ничего читать не собираемся,
							// читатель будет писать сообщения
							// в очередь циклического рассылателя, наряду со
							// всеми другими читателями других клиентов
							target.setNewQueue(serverQueue);
							break; // выходим из цикла while, заодно - наша нить
									// останавливается и удаляется из памяти
						}

					} else { // hackers online!! hideaway!!
						// до успешной регистрации, все полученные команды
						// должны начинаться с OFFER NAME
						// если это не так - то приконнектился не наш клиент.
						// выходим, закрываем коннект.
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
	 * получить входной поток сокета. Если это невозможно - PeerController
	 * закрывается и возвращает null
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
	 * получить выходной поток сокета. Если это невозможно - PeerController
	 * закрывается и возвращает null
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
	 * закрываем сокет, клиента, снимаемся с регистрации
	 */
	public synchronized void close() {
		// if we got close, then...
		pr.interrupt(); // прерываем ожидание на очереди читателя
		pw.interrupt(); // прерываем ожидание на очереди писателя
		if (server.isRegistered(this)) { // если мы зарегистрированы на сервере
			server.unregister(this); // снимаемся с регистрации
		}
		if (s != null && !s.isClosed()) { // если сокет успешно был создан и не
											// закрыт
			try {
				s.close(); // закрываем
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Метод send вызывается из циклического рассылателя, чтобы отослать некое
	 * сообщение клиенту
	 * 
	 * @param m
	 *            - сообщение на отсылку
	 */
	public void send(Message m) {
		outgoingMessages.offer(m); // тупо пробуем положить в очередь писателю.
	}

	/**
	 * проверяем, что сокет закрыт
	 * 
	 * @return состояие закрытия сокета
	 */
	public boolean isClosed() {
		return s.isClosed();
	}

}