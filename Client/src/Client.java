import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ListNode {
    int val;
    ListNode left;
    ListNode right;
    ListNode(int x) { val = x; }
}

public class Client {
    String[] quorumIps = new String[] { "dc01.utdallas.edu", "dc02.utdallas.edu", "dc03.utdallas.edu" , "dc04.utdallas.edu", "dc05.utdallas.edu", "dc06.utdallas.edu", "dc07.utdallas.edu"};
    int[] quorumPorts = new int[] { 30501, 30502, 30503, 30504, 30505, 30506, 30507 };
    int[] clientPorts = new int[] { 30000, 30001, 30002, 30003, 30004 };
    int msgCounter = 0;
    int clientID;
    String serverIps = "dc08.utdallas.edu";
    int serverPort = 30500;
    private ArrayList<ClientListener> listeners = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(500);
    private ArrayList<Boolean> token= new ArrayList<>();
    private ArrayList<Boolean> activatedToken= new ArrayList<>();//get permission from those
    private String fileName = "0.txt";
    ListNode quorumTree = new ListNode(1);

    ArrayList<Integer> quorums;
    void setMsgCounter(int i){msgCounter = i;}
    int getMsgCounter(){return msgCounter;}
    void setToken(int index,boolean t){
        token.set(index,t);
    }
    private Message socketRead(Socket socket){

        Message message = new Message(0, "",0,0,"");;
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            message =(Message) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("socketRead error");
            message.setContent("fail");
        }
        return  message;
    }
    private boolean socketWrite(Socket socket, Message message){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());

            oos.writeObject(message);

            oos.flush();
            return true;
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("socketWrite error");
            return false;
        }
    }
    void buildTree(int layer,ListNode src){

        ListNode node1 = new ListNode(src.val*2);
        ListNode node2 = new ListNode(src.val*2+1);
        src.left = node1;
        src.right = node2;
        if(layer == 1)
            return;
        else{
            buildTree(layer-1,node1);
            buildTree(layer-1,node2);
        }
    }
    private String getTimeStamp(){
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        String strTime = String.format("%s", timestamp);
        strTime = strTime.substring(0,19);
        return strTime;
    }
    String request(int quorumID,String timeStamp) throws IOException {
        Socket socket = new Socket(quorumIps[quorumID], quorumPorts[quorumID]);
        Message request = new Message(0, "request", clientID, quorumID, "");//release
        //set the message to request
        request.setContent(timeStamp);
        //sending (success is a flag of sock etWrite result)
        socketWrite(socket, request);
        //read form client
        Message ServerMessage = socketRead(socket);
        System.out.println(ServerMessage.getContent()+" "+ServerMessage.getFrom());
        msgCounter++;
        return ServerMessage.getContent();
    }
    void release(int quorumID) throws IOException {
        Socket socket = new Socket(quorumIps[quorumID], quorumPorts[quorumID]);
        Message request = new Message(0, "release", clientID, quorumID, "");//release
        //set the message to request
        request.setContent(getTimeStamp());
        //sending (success is a flag of sock etWrite result)
        socketWrite(socket, request);
        //read form client
        Message ServerMessage = socketRead(socket);
        switch (ServerMessage.getContent()){
            case "keep":
                msgCounter++;
                break;
            case "released":
                token.set(quorumID,false);
                msgCounter++;
                break;
            default:
                System.out.println("release error");
                break;
        }
        System.out.println(ServerMessage.getContent());
    }
    void severWrite(String timeStamp) throws IOException {
        Socket socket = new Socket(serverIps, serverPort);
        Message request = new Message(0, "write", clientID, 0, fileName);//release
        //set the message to request
        request.setContent(clientID+": "+timeStamp);
        //sending (success is a flag of sock etWrite result)
        socketWrite(socket, request);
        //read form client
        Message ServerMessage = socketRead(socket);
        System.out.println(ServerMessage.getContent());
    }

    ArrayList<Integer> genQuorum(ListNode tree) {
        ArrayList<Integer> dst = new ArrayList<Integer>();
        Random r = new Random();
        int randPick = r.nextInt(3);
        //System.out.print(randPick);
        switch (randPick) {
            case 0:
                dst.add(tree.val);
                if (tree.left!=null)
                    dst.addAll(genQuorum(tree.left));
                break;
            case 1:
                dst.add(tree.val);
                if (tree.right!=null)
                    dst.addAll(genQuorum(tree.right));
                break;
            case 2:
                if (tree.left !=null)
                    dst.addAll(genQuorum(tree.left));
                else
                    dst.add(tree.val);
                if (tree.right!=null)
                    dst.addAll(genQuorum(tree.right));
                break;
            default:
                System.out.println("error");
                break;
        }
        return dst;
    }
    boolean checkToken(){

        for (int i = 0;i<quorumIps.length;i++){
            if (activatedToken.get(i)){
                if(!token.get(i))
                    return false;
            }
        }
        return true;
    }
    private void broadcastRequest(String timeStamp) throws InterruptedException {
        quorums =  genQuorum(quorumTree);
        for (int quorum:quorums){//tree 1~7 array 0~6
            activatedToken.set(quorum-1,true);
            System.out.println(quorum-1);
        }

        for (int i = 0;i<quorums.size();i++) {
            int finalI = i;
            new Thread() {
                @Override
                public void run() {
                    try {
                        //request(quorums.get(finalI));

                        String result =  request(quorums.get(finalI)-1,timeStamp);

                        switch (result){
                            case "requested":
                                token.set(quorums.get(finalI)-1, true);
                                break;
                            case "wait":
                                break;
                            default:
                                System.out.println("request error");
                                break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }


    }
    private void broadcastRelease(){
        for (int i = 0;i<quorumIps.length;i++){//reset the activated token
            activatedToken.set(i,false);
        }
        for(int quorum:quorums){
            new Thread() {
                public void run() {
                    try {
                        //System.out.println(quorum-1);
                        release(quorum-1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }
        quorums.clear();
    }
    private void ini(String id) throws IOException, InterruptedException {
        clientID = Integer.valueOf(id);

        try {
            Properties p = new Properties();
            p.load(new FileInputStream("ip.ini"));
            serverIps = p.getProperty("server0");
            for (int i = 0;i<quorumIps.length;i++){
                quorumIps[i] = p.getProperty("quorum"+i);
            }
        } catch (IOException e) {
            System.out.println("no ip.ini file");
        }
        for (int i = 0;i<quorumIps.length;i++){
            token.add(false);
        }

        for (int i = 0;i<quorumIps.length;i++){
            activatedToken.add(false);
        }
        //genQuorum(quorumTree);
        buildTree(2,quorumTree);
    }
    private void listening(){
        Client client = this;
        new Thread(){
            public void run() {
                Socket socket = null;
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(clientPorts[clientID]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                while (true){
                    try{
                    socket = ss.accept();
                    ClientListener listener = new ClientListener(socket,client);
                    listeners.add(listener);
                    pool.execute((listener));
                    }catch (SocketTimeoutException e) {
                        System.out.println("time out");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }
        }.start();

    }
    public static void main(String[] args) throws IOException, InterruptedException {
        Client client = new Client();
        Random random = new Random();

        if(args.length!=0)
            client.ini(args[0]);
        else
            client.ini("0");

        client.listening();
        double time;


        for(int i = 0;i<20;i++){
            System.out.println(client.clientID+" times: "+i);
            String timeStamp = client.getTimeStamp();

            if(i == 19)
                timeStamp += "end";
            Thread.sleep(random.nextInt(3000)+2000);
            client.setMsgCounter(0);
            time = System.currentTimeMillis();
            client.broadcastRequest(timeStamp);
            client.waitToken();
            System.out.println(String.format("The Latency: %f", (System.currentTimeMillis() -time)/1000));
            client.severWrite(timeStamp);
            Thread.sleep(random.nextInt(2000)+1000);
            client.broadcastRelease();
            client.waitRelease();
            System.out.println("Message counter: "+client.getMsgCounter());
        }

    }

    private void waitToken() throws InterruptedException {
        while (!checkToken()){
            Thread.sleep(100);
        }
    }
    private void waitRelease() throws InterruptedException {
        while (true){
            if (quorums.size()==0)
                break;
        }
    }

}
