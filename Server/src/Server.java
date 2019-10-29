
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    int serverID = 0;
    int serverPorts = 30500;
    private ArrayList<Handler> clients = new ArrayList<>();
    private ExecutorService pool = Executors.newFixedThreadPool(500);
	private String pathName;
    Socket socket = null;
    ServerSocket serverSocket = null;
    Server() throws IOException {
        serverSocket = new ServerSocket(serverPorts);
    }
	private void setPathName(){
        pathName = "./files"+serverID;
    }
	private String getPathName(){
        return	pathName;
    }
    private  void run() throws IOException {
		System.out.println("Server"+serverID+" is runing...");

        try {

            socket = serverSocket.accept();
            Handler clientThread = new Handler(socket,serverID);
            clients.add(clientThread);
            pool.execute((clientThread));



        }catch (SocketTimeoutException e) {
            System.out.println("time out");
        } catch (IOException e) {
            e.printStackTrace();
        }finally{
            //socket close in handler
        }
    }
    public static void deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if(files!=null) { //some JVMs return null for empty dirs
            for(File f: files) {
                if(f.isDirectory()) {
                    deleteFolder(f);
                } else {
                    f.delete();
                }
            }
        }
        folder.delete();
    }
    public static void main(String[] args) throws IOException {

        Server server = new Server();
		
		//Remove all file and create the folder and file (make sure file is empty)
        server.setPathName();
		deleteFolder(new File(server.getPathName()));//delete folder and content
        new File(server.getPathName()).mkdir();//new a folder
        new File(server.getPathName()+"/"+0+".txt").createNewFile();

        while (true){
            server.run();
        }


    }
}