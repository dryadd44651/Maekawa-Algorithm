import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientListener implements Runnable {
    private Socket socket;
    private Message ClientMessage;
    private Message ServerMessage;
    ObjectInputStream ois;
    ObjectOutputStream oos;
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
    Client client;

    private Message getReturnMessage(Message src){
        Message dst = new Message(1,src.getType(),src.getTo(),src.getFrom(),src.getFileName());
        return dst;
    }
    public ClientListener(Socket socket,Client client) throws IOException {
        this.socket = socket;
        this.client = client;
        this.ois = new ObjectInputStream(socket.getInputStream());
        this.oos= new ObjectOutputStream(socket.getOutputStream());
    }
    @Override
    public void run() {
        ClientMessage = (Message) socketRead();
        ServerMessage = getReturnMessage(ClientMessage);
        switch (ClientMessage.getType()){
            case "reply":
                client.setToken(ClientMessage.getFrom(),true);
                ServerMessage.setContent("replied");
                System.out.println("replied");
                break;
            default:
                ServerMessage.setContent("error");
                System.out.println("client listener error");
                break;
        }

        socketWrite(ServerMessage);
        try {//close the socket connection
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
