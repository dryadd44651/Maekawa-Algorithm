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

    private void socketWrite(Message message){
        //ObjectOutputStream oos
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
                quorum.setToken(false);
                break;
            case "release":
                quorum.setToken(true);
                ServerMessage.setContent("released");
                break;

            default://unknown command
                ServerMessage.setContent("Error");
                break;
        }
        System.out.println(ServerMessage.getContent());
        socketWrite(ServerMessage);

        try {//close the socket connection
            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
