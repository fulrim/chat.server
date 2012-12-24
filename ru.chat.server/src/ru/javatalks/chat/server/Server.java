package ru.javatalks.chat.server;

import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Collection;

/**
 * Server - запускаемый класс для сервера. Он создает первичную инициализацию,
 * принимает запросы на подключение, держит на себе список подключившихся
 * клиентов.
 */

public class Server {
	public static final String OFFER_NAME = "OFFER NAME:"; // заголовок команды
															// клиента с
															// предложением
															// имени.
	public static final String CHARSET = "UTF-16"; // кодировка, единая для
													// сервера и клиентов.
	private ServerSocket ss; // серверный сокет, прослушивающий входящие
								// подключения

	// список подключившихся и прошедших авторизацию клиентов
	// String - уникальное имя клиента (то, что демонстрируется в чате)
	// PeerController - экземпляр клиента, хранящий в себе сокет подключения
	// клиента, его имя и т.д.
	private ConcurrentMap<String, PeerController> peers;

	private CyclicSender cs; // экземпляр класса-нити-демона, рассылающий
								// полученное сообщение по всем клиентам
	public static final String REGISTER_SUCCESSFULL = "Register successfull!"; // просто
																				// константа.

	/**
	 * Единственный валидный конструктор. Сделан приватным, чтобы можно было
	 * создать только из main()
	 */

	private Server() {
		// создаем небольшую хеш-мапу на 4 пользователя
		// при необходимости - она вырастет сама.
		peers = new ConcurrentHashMap<String, PeerController>(4);
		try {
			ss = new ServerSocket(45000); // пробуем создать сервер-сокет на
											// прослушивание
			cs = new CyclicSender(this); // создаем экземпляр циклического
											// рассылателя.
			final Thread cyclicSenderThread = new Thread(cs, "CyclicSender"); // создаем
																				// нить
																				// на
																				// нем
			// устанавливаем его в демона (чтобы потом не надо было вручную его
			// останавливать)
			cyclicSenderThread.setDaemon(true);
			cyclicSenderThread.start(); // запускаем рассылателя
		} catch (IOException e) {
			e.printStackTrace(); // если ошибки - печатаем.
		}
	}

	/**
	 * главный цикл ожидания коннекта и обработки поступивших вызовов
	 * 
	 */
	void run() {
		while (!Thread.currentThread().isInterrupted()) { // это заготовка на
															// будущую остановку
															// сервера через
															// админа
			try {
				// слушаем порт, если поступил вызов, создаем экземпляр
				// PeerController, которому передаем ссылку на себя
				// и на поступивший вызов.
				PeerController p = new PeerController(this, ss.accept());
				new Thread(p, "PeerController").start(); // запускаем созданный
															// экземпляр
															// PeerController в
															// отдельной нити

			} catch (IOException e) { // eсли проблемы с сервер-сокетом
				e.printStackTrace(); // печатаем стек-трейс.
				// и пробуем закрыть сервер-сокет (на всякий случай)
				close();
				Thread.currentThread().interrupt(); // и ставим флаг остановки
													// потока.
			}
		}
	}

	/**
	 * Метод вызывается из экземпляра PeerController, когда он получил желаемое
	 * имя пользователя. Метод проверяет свободно ли предлагаемое пользователем
	 * имя, и если да - регистрирует его в списке.
	 * 
	 * @param p
	 *            - PeerController, пытающийся зарегистрироваться
	 * @return Если имя не дублировано - true, иначе - false
	 */
	boolean tryRegister(PeerController p) {
		return peers.putIfAbsent(p.getName(), p) == null;
	}

	/**
	 * Метод вызывается из экземпляра PeerController, когда пользователь
	 * "отвалился" либо был кикнут. Метод снимает пользователя с регистрации в
	 * списке активных пользователей.
	 * 
	 * @param p
	 *            - PeerController, желающий разрегистрироваться
	 * @return - true - успешная разрегистрация, false - в списках не было.
	 */
	boolean unregister(PeerController p) {
		return peers.remove(p.getName()) == p;
	}

	/**
	 * Проверяется, зарегистрирован ли данный PeerController в списке клиентов
	 * 
	 * @param p
	 *            - проверяемый PeerController
	 * @return true - в списке, false - нет.
	 */
	boolean isRegistered(PeerController p) {
		return peers.containsValue(p);
	}

	/**
	 * Получить текущую коллекцию клиентов (для циклической рассылки)
	 * 
	 * @return Коллекция клиентов
	 */
	public Collection<PeerController> getPeers() {
		return peers.values();
	}

	/**
	 * Получить единую входную очередь всех сообщений (от циклического
	 * рассылателя). Запись в эту очередь сообщения (Message) будет генерировать
	 * рассылку по всем клиентам.
	 * 
	 * @return Входная очередь цикличесого рассылателя.
	 */
	public BlockingQueue<Message> getIncomeQueue() {
		return cs.getQueue();
	}

	/**
	 * общий метод закрытия сервер-сокета.
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
	 * Финализатор, если сборщик мусора собрал данный объект. Закрывает
	 * сервер-сокет.
	 * 
	 * @throws Throwable
	 */
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	/**
	 * Входная точка программы
	 * 
	 * @param args
	 *            - не используется
	 */
	public static void main(String[] args) {
		// создаем сервер и стартуем его.
		new Server().run();
	}
}