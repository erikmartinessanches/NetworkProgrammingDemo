package org.ems;

import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class Client {
    interface Observer{
        void onObservableChanged(String commandReceived);
    }
    private static PrintStream out;
    private static NetworkDetector networkDetector;
    private static Socket sckt;
    private static ClientWindow clientWindow;
    private static boolean listenForTerminalInput = true;
    private static int playerNumber = 0;
    private static String map;
    public Client() {
    }

    static Observer detectorObs = new Observer() {
        @Override
        public void onObservableChanged(String commandReceived) {
            System.out.println("Detected " + commandReceived + " from the network.");
            actOnCommand(commandReceived);
        }
    };

    static Observer uiObs = new Observer() {
        @Override
        public void onObservableChanged(String commandReceived) {
            if (commandReceived.startsWith("quit")){
                try {
                    Disconnect();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    Send(commandReceived);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private static void actOnCommand(String commandReceived) {
        if (commandReceived.startsWith("init")){
            playerNumber = Integer.parseInt(commandReceived.substring(5,6));
            System.out.println("client " + playerNumber + " init reached.");
            map = commandReceived.substring(7);
            ShowWindow();
            //Perhaps wait for the other player to connect until 'start', somehow.
        }
        if (commandReceived.startsWith("start")){
            System.out.println("Start command received, both players are connected."); //Get out of the waiting mode on 'start'.
        }
        if (commandReceived.startsWith("left") || commandReceived.startsWith("right") || commandReceived.startsWith("up") || commandReceived.startsWith("down")){
            String[] temp = commandReceived.split(" ");
            String direction = temp[0];
            int playerToMove = Integer.parseInt(temp[1]);
            if (playerNumber==playerToMove){
                moveGeometry(clientWindow.player, direction);
            } else {
                moveGeometry(clientWindow.opponent, direction);
            }
        }
        if (commandReceived.startsWith("lift")){
            String[] temp = commandReceived.split(" ");
            if (temp[1].equals("1") && temp[2].equals("b")) {
                removeGeometry("key1");
            }
            if (temp[1].equals("2") && temp[2].equals("c")) {
                removeGeometry("key2");
            }
        }
        if (commandReceived.equals("loser")){
            System.out.println("The other player disconnected. (Losing state.)");
        }
    }

    private static void ShowWindow() {
        clientWindow = new ClientWindow(map, playerNumber); //Make window AFTER getting playerNumber and map from server.
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Super Game 64 (player " + playerNumber + ")");
        settings.setFrameRate(25);
        settings.setAlphaBits(0);
        settings.setAudioRenderer(null);
        settings.setVSync(true);
        settings.setGammaCorrection(true);
        clientWindow.setSettings(settings);
        clientWindow.start();

        clientWindow.registerObserver(uiObs); //Register observer AFTER making a clientWindow...
    }

    private static void Send(String textForTransmission) throws IOException {
        out = new PrintStream(sckt.getOutputStream());
        out.println(textForTransmission);
    }

//    private static void setStatus(String playing) {
//        final Node tempNode = (Node) clientWindow.getGuiNode().getChild("myUiElements");
//        final Label tempLabel = tempNode.getChild("myLabel");
//
//    }

    private static void removeGeometry(String keyName) {
        final Node tempNode = (Node) clientWindow.getRootNode().getChild("walls");//Obtain the geometry to remove, by using the name "key1" or "key2".
        clientWindow.enqueue(new Callable<Integer>() {
            public Integer call() throws Exception {
                return tempNode.detachChildNamed(keyName);
            }
        });
    }

    public static void moveGeometry(final Geometry geoToMove, String direction/*, float value*/){
        clientWindow.enqueue(new Callable<Spatial>() {
            public Spatial call() throws Exception {
                float value=1f;//arbitrary offset for now.
                if(direction.startsWith("up")){
                    return geoToMove.move(0f,value, 0f);
                }
                if(direction.startsWith("down")){
                    return geoToMove.move(0f,-value, 0f);
                }
                if(direction.startsWith("left")){
                    return geoToMove.move(-value,0, 0f);
                }
                if(direction.startsWith("right")){
                    return geoToMove.move(+value,0, 0f);
                }
                return null;
            }
        });
    }

    public static void main(String[] args){
        try{
            sckt = new Socket(InetAddress.getLocalHost(),52222); //Duplex.
            networkDetector = new NetworkDetector(sckt);
            networkDetector.registerObserver(detectorObs);
            networkDetector.start();

            //Must come AFTER networkDetector and BEFORE clientWindow.
            Send("conn"); //Request playerNumber and map from server.
            //We have to have a window before registering observer on it.
            //We have to get the playerNumber from server before creating a window.

            //Read keyboard input and send.
            BufferedReader indata = new BufferedReader(new InputStreamReader(System.in));
            String textForTransmission = "";
            while(listenForTerminalInput){
                if(indata.ready()){ //True if data has been input from the terminal into BufferedReader!
                    if (listenForTerminalInput && textForTransmission.startsWith("quit")){
                        Disconnect();
                    } else if(listenForTerminalInput) {
                        Send(textForTransmission);
                    }
                } else { //If not, wait for data to be inserted from the terminal (into the BufferedReader).
                    Thread.sleep(100);
                }
            }
        } catch(Exception e){System.err.println("A problem in the client:" + e.getMessage());
            e.printStackTrace();}
    }

    private static void Disconnect() throws IOException, InterruptedException {
        Send("quit");

        clientWindow.unregisterObserver(uiObs);
        clientWindow.stop();
        networkDetector.listenForNetworkInput = false;
        listenForTerminalInput = false;
        networkDetector.unregisterObserver(detectorObs);
        networkDetector.socket.shutdownInput();
        //sckt.shutdownOutput();
    }
}

//Listen for incoming messages from the network.
class NetworkDetector extends Thread {
    Socket socket;
    String textReceived;
    public boolean listenForNetworkInput;
    private final Set<Client.Observer> mObservers = Collections.newSetFromMap(new ConcurrentHashMap<Client.Observer, Boolean>(0));

    public NetworkDetector(Socket s){
        this.socket=s;
        this.textReceived="";
        this.listenForNetworkInput = true;
    }

    public void registerObserver(Client.Observer observer){
        if (observer==null) return;
        mObservers.add(observer);
    }
    public void unregisterObserver(Client.Observer observer){
        if (observer!=null) mObservers.remove(observer);
    }

    private void notifyObservers(){
        for(Client.Observer observer: mObservers){
            observer.onObservableChanged(this.textReceived);
        }
    }
    private void setTextReceived(String text) {
        if(this.textReceived == text) return;
        this.textReceived = text;
        notifyObservers();
    }
    @Override
    public void run() {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String textReceived = null;
        while(listenForNetworkInput){
            try {
                if((textReceived = in.readLine()) != null){
                    setTextReceived(textReceived);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

