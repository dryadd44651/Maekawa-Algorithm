
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Quorum {
    int quorumID = 0;
    int[] quorumPorts = new int[] { 30501, 30502, 30503, 30504, 30505, 30506, 30507 };
    int[] clientPorts = new int[] { 30000, 30001, 30002, 30003, 30004 };
    String[] clientIps = new String[] { "dc09.utdallas.edu", "dc10.utdallas.edu", "dc11.utdallas.edu" , "dc12.utdallas.edu", "dc13.utdallas.edu"};
    //private ArrayList<QuorumListener> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newCachedThreadPool();

	private String pathName;
	private ArrayList<Message> queue = new ArrayList<>();
    boolean token = true;
    Socket socket = null;
    ServerSocket serverSocket = null;

    public boolean getToken(){
        return token;
    }
    public void setToken(boolean t){
        token = t;
    }
    private void setID(int src){
        quorumID = src%quorumPorts.length;
    }
    private Message socketRead(Socket socket) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
        Message message = new Message(0,"",0,0,"");;
        try {
            message =(Message) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return  message;
    }
    private void socketWrite(Socket socket ,Message message) throws IOException {
        ObjectOutputStream oos;
        oos= new ObjectOutputStream(socket.getOutputStream());
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    void reply(int clientID) throws IOException {
        Socket replySocket = new Socket(clientIps[clientID], clientPorts[clientID]);
        Message request = new Message(0, "reply", quorumID,clientID, "fileName");
        System.out.println(clientID);
        //set the message to request


        //sending (success is a flag of sock etWrite result)
        socketWrite(replySocket, request);
        //read form client
        Message ServerMessage = socketRead(replySocket);
        if (ServerMessage.getContent().compareTo("replied")==0)
            token = false;
        System.out.println(ServerMessage.getContent());
    }
    public void addToQueue(Message src) {
        this.queue.add(src);
    }
    public int firstList(){
        long time ,min;
        int minIdx,qIdx = 0;
        if(this.queue.size() == 0)
            return -1;
        min = Long.valueOf(this.queue.get(0).getContent().replaceAll("-|:| |end",""));
        minIdx = this.queue.get(0).getFrom();
        //for(Message message:this.queue){
        for(int i = 0;i<queue.size();i++){
            Message message = queue.get(i);
            time = Long.valueOf(message.getContent().replaceAll("-|:| |end",""));
            if(time<min){
                min = time;
                minIdx = message.getFrom();
                qIdx = i;
            }
        }
        queue.remove(qIdx);
        return minIdx;
    }
    public ArrayList<Message> getQueue(){
        return queue;
    }
    private int getID(){
        return	quorumID;
    }
	private void setPathName(){
        pathName = "./files"+quorumID;
    }
	private String getPathName(){
        return	pathName;
    }
    private  void run() throws IOException {
		System.out.println("Quorum"+quorumID+" is runing...");

        try {
            socket = serverSocket.accept();
            //QuorumListener clientThread = new QuorumListener(socket,this);
            //clients.add(clientThread);
            pool.execute(new QuorumListener(socket,this));



        }catch (SocketTimeoutException e) {
            System.out.println("time out");
        } catch (IOException e) {
            e.printStackTrace();
        }finally{

            //socket close in handler
        }
    }
    private void release(){
        new Thread(){
            @Override
            public void run() {
                while (true){
                    if (token == true && queue.size()>0){
                        int dst = firstList();
                        try {
                            System.out.println(dst);
                            reply(dst);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }.start();

    }
    private  void ini(int id){
        setID(id);
        try {
            Properties p = new Properties();
            p.load(new FileInputStream("ip.ini"));

            for (int i = 0;i<clientIps.length;i++){
                clientIps[i] = p.getProperty("client"+i);
            }
            serverSocket = new ServerSocket(quorumPorts[quorumID]);
        } catch (IOException e) {
            System.out.println("no ip.ini file");
        }
    }
    public static void main(String[] args) throws IOException {

        Quorum quorum = new Quorum();
        if(args.length!=0)
            quorum.ini(Integer.valueOf(args[0]));
        else
            quorum.ini(0);

        quorum.release();
        while (true){
            quorum.run();
        }


    }
}