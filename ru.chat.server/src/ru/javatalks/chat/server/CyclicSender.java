    package ru.javatalks.chat.server;
     
    import java.util.concurrent.BlockingQueue;
    import java.util.concurrent.LinkedBlockingQueue;
    import java.util.Collection;
     
    /**
    * Класс, прослушивающий единую очередь входящих сообщений, и рассылающий полученное сообещние по всем клиентам.
    */
    public class CyclicSender implements Runnable{
    // очередь входящих сообщений
    private BlockingQueue<Message> generalQueue = new LinkedBlockingQueue<Message>(256);
    // создавший экземпляр рассылателя сервер
    private Server server;
     
    /**
    * Конструктор
    * @param s - сервер, что создал рассылателя
    */
    public CyclicSender (Server s) {
    server = s;
    }
     
    /**
    * метод, получивший управление после запуска нити рассылателя
    */
    public void run() {
    while (!Thread.currentThread().isInterrupted()) { // главный цикл
    Message m = null;
    try {
    m = generalQueue.take(); // ожидаем поступившее сообщение от любого из клиентов
    } catch (InterruptedException ignored) {} // если прервали, то помалкиваем, это нестрашно и не важно.
    if (m != null) { // если сообщение получено, то...
    Collection<PeerController> peers = server.getPeers(); //... получаем всех клиентов...
    for(PeerController pc:peers) { // ... по циклу каждому...
    pc.send(m); //... отправляем полученное сообщение
    }
    }
     
    }
    }
     
    /**
    * Передает ссылку серверу на созданную единую очередь входящих сообщений
    * @return - единая очередь входящих сообщений.
    */
    public BlockingQueue<Message> getQueue() {
    return generalQueue;
    }
    }