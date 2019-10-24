import com.sun.tools.javac.Main;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Handler implements Runnable {
    private Socket client;
    private Message ClientMessage;
    private Message ServerMessage;
    ObjectInputStream ois;
    ObjectOutputStream oos;
    Quorum quorum;
    private Message socketRead(){
        //ObjectInputStream ois
        Message message = new Message(0,"",0,0,"");;
        try {
            message =(Message) ois.readObject();

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return  message;
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
    private void socketWrite(Message message){
        //ObjectOutputStream oos
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void socketWrite(Socket socket ,Message message) throws IOException {
        //ObjectOutputStream oos
        this.oos= new ObjectOutputStream(socket.getOutputStream());
        try {
            oos.writeObject(message);
            oos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Message getReturnMessage(Message src){
        Message dst = new Message(1,src.getType(),src.getTo(),src.getFrom(),src.getFileName());
        return dst;
    }

    void reply(int clientID) throws IOException {
        Socket socket = new Socket(quorum.clientIps[clientID], quorum.clientPorts[clientID]);
        Message request = new Message(0, "request", clientID, quorum.quorumID, "fileName");//release
        System.out.println(clientID);
        //set the message to request
        request.setContent("reply");
        //sending (success is a flag of sock etWrite result)
        socketWrite(socket, request);
        //read form client
        Message ServerMessage = socketRead(socket);
        System.out.println(ServerMessage.getContent());
    }
    public Handler(Socket clientSocket, Quorum quorum) throws IOException {
        this.client = clientSocket;
        this.ois = new ObjectInputStream(clientSocket.getInputStream());
        this.oos= new ObjectOutputStream(clientSocket.getOutputStream());
        this.quorum = quorum;

    }
    @Override
    public void run() {
        //read message from client and analyse it

        ClientMessage = (Message) socketRead();
        ServerMessage = getReturnMessage(ClientMessage);
        //System.out.println("client"+ClientMessage.getFrom()+"message : "+ClientMessage.getContent());



        switch (ClientMessage.getType()){
            case "request":
                if(quorum.getToken()==true){
                    ServerMessage.setContent("requested");
                    quorum.setToken(false);
                }else{
                    ServerMessage.setContent("wait");
                    quorum.addToQueue(ClientMessage);
                }
                System.out.println("request");
//                while (!quorum.getToken()){
//                    try {
//                        Thread.sleep(100);
//                        //System.out.println("wait");
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }

                System.out.println(ServerMessage.getContent());
                quorum.setToken(false);
                break;
            case "release":
                int releaseDst;
                System.out.println("release");
                if(quorum.getQueue().size()>0){
                    releaseDst = quorum.firstList();
                    //System.out.println(releaseDst+" "+ClientMessage.getFrom());

                    if (releaseDst == ClientMessage.getFrom()){
                        ServerMessage.setContent("keep");
                        System.out.println(ClientMessage.getFrom()+" keep");
                    }else {
//                        new Thread(){
//                            @Override
//                            public void run() {
//                                try {
//                                    System.out.println(releaseDst+" "+ClientMessage.getFrom());
//                                    reply(releaseDst);
//                                } catch (IOException e) {
//                                    //e.printStackTrace();
//                                    System.out.println("Error!! release "+releaseDst+" release from"+ClientMessage.getFrom());
//                                }
//                            }
//                        }.start();
                        try {
                            reply(releaseDst);//reply token to other
                        } catch (IOException e) {
                            //e.printStackTrace();
                            System.out.println(quorum.clientIps[ClientMessage.getFrom()]+" "+ quorum.clientPorts[ClientMessage.getFrom()]);
                            System.out.println(quorum.clientIps[releaseDst]+" "+ quorum.clientPorts[releaseDst]);
                            System.out.println("release "+releaseDst+" release from"+ClientMessage.getFrom());
                        }
                        ServerMessage.setContent("released");
                        System.out.println(ClientMessage.getFrom()+"keep");
                    }







                }else {
                    quorum.setToken(true);
                    ServerMessage.setContent("released");
                }

                break;

            default://unknown command
                System.out.println(ClientMessage.getType());
                ServerMessage.setContent("Error");
                System.out.println(ClientMessage.getFrom() + ": Wrong type");
                break;
        }
        socketWrite(ServerMessage);

        try {//close the socket connection
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
