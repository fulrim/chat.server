    package ru.javatalks.chat.server;
     
    import java.util.concurrent.BlockingQueue;
    import java.util.concurrent.LinkedBlockingQueue;
    import java.util.Collection;
     
    /**
    * �����, �������������� ������ ������� �������� ���������, � ����������� ���������� ��������� �� ���� ��������.
    */
    public class CyclicSender implements Runnable{
    // ������� �������� ���������
    private BlockingQueue<Message> generalQueue = new LinkedBlockingQueue<Message>(256);
    // ��������� ��������� ����������� ������
    private Server server;
     
    /**
    * �����������
    * @param s - ������, ��� ������ �����������
    */
    public CyclicSender (Server s) {
    server = s;
    }
     
    /**
    * �����, ���������� ���������� ����� ������� ���� �����������
    */
    public void run() {
    while (!Thread.currentThread().isInterrupted()) { // ������� ����
    Message m = null;
    try {
    m = generalQueue.take(); // ������� ����������� ��������� �� ������ �� ��������
    } catch (InterruptedException ignored) {} // ���� ��������, �� �����������, ��� ��������� � �� �����.
    if (m != null) { // ���� ��������� ��������, ��...
    Collection<PeerController> peers = server.getPeers(); //... �������� ���� ��������...
    for(PeerController pc:peers) { // ... �� ����� �������...
    pc.send(m); //... ���������� ���������� ���������
    }
    }
     
    }
    }
     
    /**
    * �������� ������ ������� �� ��������� ������ ������� �������� ���������
    * @return - ������ ������� �������� ���������.
    */
    public BlockingQueue<Message> getQueue() {
    return generalQueue;
    }
    }