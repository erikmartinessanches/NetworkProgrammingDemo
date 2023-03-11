package org.ems;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Server{
    ServerSocket serverSckt;
    int playerNumber = 1;
    int numberOfConnections;
    Socket sckt1;
    Socket sckt2;
    Servant svnt1;
    Servant svnt2;
    Game game;
    boolean losingState; //Used if one of the clients disconnect, hence a concern for the Server class...

    
    public Server() throws IOException {
        serverSckt = new ServerSocket(52222);
        numberOfConnections = 0;
        losingState = false;
        game = new Game();
        //makeMapArray(). //Make an array from the string.
    }
    
    Observer obs1 = new Observer() { //Observer for network input from player 1.
        @Override
        public void onObservableChanged() {
            playerNumber = 1;
            System.out.println("Player 1 says: " + svnt1.playersCommand);
            try {
                if(svnt1.playersCommand.equals("quit")){
                    numberOfConnections--; //Should we do something more apart from this?
                   // svnt1.sckt.close();
                    svnt1.sckt.shutdownInput();
                    svnt1.sckt.shutdownOutput();
                    System.out.println("Player 1 has disconnected, both lose.");
                    Send(svnt2, "loser");
                    losingState = true;
                    notifyAll();
                } else if (svnt1.playersCommand.equals("conn")){
                    Send(svnt1, "init " + playerNumber + " " + game.mapString);}
                else if(svnt1.playersCommand.equals("left") || svnt1.playersCommand.equals("right") || svnt1.playersCommand.equals("up") || svnt1.playersCommand.equals("down")){
                    //Check first if this move is permissible prior to remote controlling.
                    Result answer = game.processCommand(svnt1.playersCommand, playerNumber);
                    if (answer == Result.Ok){
                        Send(svnt2, svnt1.playersCommand + " 1");
                        Send(svnt1, svnt1.playersCommand + " 1");
                    }
                    if (answer == Result.PickedUpB){
                        Send(svnt2, svnt1.playersCommand + " 1");
                        Send(svnt1, svnt1.playersCommand + " 1");
                        Send(svnt1, "lift 1 b");
                        Send(svnt2, "lift 1 b");
                    }
                    if (answer == Result.Won){
                        Send(svnt2, svnt1.playersCommand + " 1");
                        Send(svnt1, svnt1.playersCommand + " 1");
                        Send(svnt1, "winner");
                        Send(svnt2, "winner");
                        System.out.println("Winning state achieved.");
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    Observer obs2 = new Observer() { //Observer for network input from player 2.
        @Override
        public void onObservableChanged() {
            playerNumber = 2;
            System.out.println("Player 2 says: " + svnt2.playersCommand);
            try {
                if(svnt2.playersCommand.equals("quit")){
                    numberOfConnections--; //Should we do something more apart from this?
                 //   svnt2.sckt.close();
                    svnt2.sckt.shutdownInput();
                    svnt2.sckt.shutdownOutput();
                    System.out.println("Player 2 has disconnected, both lose.");
                    Send(svnt1, "loser");
                    losingState = true;
                    
                } else if (svnt2.playersCommand.equals("conn")){
                    Send(svnt2, "init " + playerNumber + " " + game.mapString);}
                else if(svnt2.playersCommand.equals("left") || svnt2.playersCommand.equals("right") || svnt2.playersCommand.equals("up") || svnt2.playersCommand.equals("down")){
                    //Obviously check first if this move is permissible prior to remote controlling.
                    Result answer = game.processCommand(svnt2.playersCommand, playerNumber);
                    if (answer == Result.Ok) {
                        Send(svnt1, svnt2.playersCommand + " 2");
                        Send(svnt2, svnt2.playersCommand + " 2");
                    }
                    if (answer == Result.PickedUpC){
                        Send(svnt1, svnt2.playersCommand + " 2");
                        Send(svnt2, svnt2.playersCommand + " 2");
                        Send(svnt1, "lift 2 c");
                        Send(svnt2, "lift 2 c");
                    }
                    if (answer == Result.Won){
                        Send(svnt1, svnt2.playersCommand + " 2");
                        Send(svnt2, svnt2.playersCommand + " 2");
                        Send(svnt1, "winner");
                        Send(svnt2, "winner");
                        System.out.println("Winning state achieved.");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
    
    public void ConnectPlayer1() throws IOException {
        sckt1 = serverSckt.accept();
        numberOfConnections++;
        this.playerNumber = 1;
        svnt1 = new Servant(sckt1, playerNumber);
        svnt1.registerObserver(obs1); //Consider moving to later and outside the function to prevent getting input while the other play is not connected.
        svnt1.start();
        game.setP1(new Player(this.playerNumber)); //Possibly move these to observers.
    }
    public void ConnectPlayer2() throws IOException {
        sckt2 = serverSckt.accept();
        numberOfConnections++;
        this.playerNumber = 2;
        svnt2 = new Servant(sckt2, playerNumber);
        svnt2.registerObserver(obs2); //Consider moving to later and outside the function to prevent getting input while the other play is not connected.
        svnt2.start();
        game.setP2(new Player(this.playerNumber)); //Possibly move these to observers, by "conn".
    }
    
    public void Start() throws IOException, InterruptedException {
        ConnectPlayer1();
        ConnectPlayer2();
        Thread.sleep(250);
        Send(svnt1,"start");
        Send(svnt2,"start");
        game.play();
        while(game.isOn()){ //Playing the game. This shouldn't prevent us from checking the rules in the observers above...
            Thread.onSpinWait(); //TODO Pause this thread here properly.

            //Continue thethread here on some future notification.
            if(losingState) break;
        }
       //     svnt1.unregisterObserver(obs1); //TODO Consider bringing these back, somewhere suitable.
       //     svnt2.unregisterObserver(obs2);
    }

    interface Observer{
        void onObservableChanged();
    }
    
    private void Send(Servant svnt, String othersCommand) throws IOException {
        if(!losingState){ //OR !winningState
            PrintStream out = new PrintStream(svnt.sckt.getOutputStream());
            out.println(othersCommand);
            // System.out.println("Sending command to the other player.");
        }
    }
}

class Servant extends Thread{
    Socket sckt;
    boolean playing;
    int playerNumber;
    String playersCommand;
    private final Set<Server.Observer> mObservers = Collections.newSetFromMap(new ConcurrentHashMap<Server.Observer, Boolean>(0));
    public void registerObserver(Server.Observer observer){
        if (observer==null) return;
        mObservers.add(observer);
    }
    public void unregisterObserver(Server.Observer observer){
        if (observer!=null) mObservers.remove(observer);
    }
    private void notifyObservers(){
        for(Server.Observer observer: mObservers){
            observer.onObservableChanged();
        }
    }
    public Servant(Socket s, int playerNumber){
        super("Player"+playerNumber+"Server");//Passing the name of the thread to superclass constructor.
        this.sckt=s; //'new ServerSocket' in Server.java example.
        this.playing=true;
        this.playerNumber = playerNumber;
        this.playersCommand = "";
    }
    private void setPlayersCommand(String playersCommand) {
        if(this.playersCommand == playersCommand) return;
        this.playersCommand = playersCommand;
        notifyObservers();
    }
    @Override
    public void run(){
        try{
                //System.out.println("Player " + playerNumber + " is playing.");
                BufferedReader indata = new BufferedReader(new InputStreamReader(sckt.getInputStream()));
                String text = null;
                while (((text = indata.readLine()) != null)) {
                    setPlayersCommand(text);
            }
            //sckt.shutdownInput();//Perhaps not needed here if we're shutting it down in the observer function.
        }catch(IOException ioe){
            System.out.println("Något fel inträffade!"); //TODO avoid this when one client disconnects.
            System.err.println(ioe.getMessage());
        }
    }
}
