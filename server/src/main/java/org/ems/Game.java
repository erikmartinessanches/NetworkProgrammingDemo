package org.ems;
enum Result {
    Ok,
    No,
    PickedUpB,
    PickedUpC,
    Won
}

public class Game implements GameInterface{
    private boolean gameIsOn;
    private final static int SIZE = 10; //Height and width of the map.
    private int turn=0;
    String mapString = new StringBuilder(
                    " ####     " +
                    " #op#     " +
                    " #  #     " +
                    "##  ######" +
                    "#        #" +
                    "#1 b#    #" +
                    "#####    #" +
                    "#        #" +
                    "# 2 c    #" +
                    "##########").toString();
    private Player p1;
    private Player p2;
    
    public Game(){
    }

    private Result checkKeyPresenceInDirection(String blockInDirection, Player p){
        if (p.getPlayerNumber() == 1){
            if (blockInDirection.equals("b")){
                p1.setKey(new Key("b", "o"));
                //Possibly remove the key from the map to prevent the player from picking it up multiple times.
                int keyX = mapString.indexOf(p1.getKey().name) % 10;
                int keyY = (int)Math.floor(mapString.indexOf(p1.getKey().name) / 10.0);
                System.out.println("blockInDirection: " + blockInDirection);
                System.out.println("KeyX:" + keyX);
                System.out.println("KeyY:" + keyY);
                String firstPartOfNewMap = mapString.substring(0, Math.abs(keyY*10+keyX));
                String secondPartOfNewMap = mapString.substring(Math.abs(keyY*10+keyX+1));
                mapString = firstPartOfNewMap + " " + secondPartOfNewMap;
                System.out.println("Player " + p.getPlayerNumber() + " picked up key " + p1.getKey().name + ".");
                return Result.PickedUpB;
            }
        }
        if (p.getPlayerNumber()==2){
            if (blockInDirection.equals("c")){
                p2.setKey(new Key("c", "p"));
                //Possibly remove the key from the map to prevent the player from picking it up multiple times.
                int keyX = mapString.indexOf(p2.getKey().name) % 10;
                int keyY = (int)Math.floor(mapString.indexOf(p2.getKey().name) / 10.0);
                System.out.println("blockInDirection: " + blockInDirection);
                System.out.println("KeyX:" + keyX);
                System.out.println("KeyY:" + keyY);
                String firstPartOfNewMap = mapString.substring(0, Math.abs(keyY*10+keyX));
                String secondPartOfNewMap = mapString.substring(Math.abs(keyY*10+keyX+1));
                mapString = firstPartOfNewMap + " " + secondPartOfNewMap;
                System.out.println("Player " + p.getPlayerNumber() + " picked up key " + p2.getKey().name + ".");
                return Result.PickedUpC;
            }
        }
        return null;
    }

    private boolean checkForWinningState() {
        int door1X = mapString.indexOf("o") % 10;
        int door1Y = (int)-Math.floor(mapString.indexOf("o") / 10.0);
        int door2X = mapString.indexOf("p") % 10;
        int door2Y = (int)-Math.floor(mapString.indexOf("p") / 10.0);
        if (door1X==p1.getXPos() && door1Y==p1.getyPos() && door2X == p2.getXPos() && door2Y == p2.getyPos() && p1.getKey().getName().equals("b") && p2.getKey().getName().equals("c")){
            return true;
        }
        return false;
    }
    
    public Result processCommand(String command, int playerNumber){
        Result answer = Result.Ok; //start by allowing movement.
        if (playerNumber == 1){
            if (command.equals("up")){
                int lookAtPosition = Math.abs(p1.getyPos()+1)*10 + p1.getXPos();
                String blockInDirection = mapString.substring(lookAtPosition, lookAtPosition + 1);
                if (blockInDirection.equals("#") || (p1.getXPos() == p2.getXPos() && p2.getyPos()==p1.getyPos()+1)) return Result.No;
                if (checkKeyPresenceInDirection(blockInDirection, p1) == Result.PickedUpB){
                    p1.setyPos(p1.getyPos() + 1);
                    return Result.PickedUpB;
                }
                p1.setyPos(p1.getyPos() + 1); //Player is allowed to move, update its position.
            }
            if (command.equals("down")){
                int lookAtPosition = Math.abs(p1.getyPos()-1)*10 + p1.getXPos();
                String blockInDirection = mapString.substring(lookAtPosition, lookAtPosition + 1);
                if (blockInDirection.equals("#") || (p1.getXPos() == p2.getXPos() && p2.getyPos()==p1.getyPos()-1)) return Result.No;
                if (checkKeyPresenceInDirection(blockInDirection, p1) == Result.PickedUpB){
                    p1.setyPos(p1.getyPos() - 1);
                    return Result.PickedUpB;
                }
                p1.setyPos(p1.getyPos() - 1); //Player is allowed to move, update its position.
            }
            if (command.equals("left")){
                int lookAtPosition = Math.abs(p1.getyPos())*10 + p1.getXPos() - 1;
                String blockInDirection = mapString.substring(lookAtPosition, lookAtPosition + 1);
                if (blockInDirection.equals("#") || (p1.getyPos() == p2.getyPos() && p2.getXPos()==p1.getXPos()-1)) return Result.No;
                if (checkKeyPresenceInDirection(blockInDirection, p1) == Result.PickedUpB) {
                    p1.setXPos(p1.getXPos() - 1);
                    return Result.PickedUpB;
                }
                p1.setXPos(p1.getXPos() - 1); //Player is allowed to move, update its position.
            }
            if (command.equals("right")){
                int lookAtPosition = Math.abs(p1.getyPos())*10 + p1.getXPos() + 1;
                String blockInDirection = mapString.substring(lookAtPosition, lookAtPosition + 1);
                if (blockInDirection.equals("#") || (p1.getyPos() == p2.getyPos() && p2.getXPos()==p1.getXPos()+1)) return Result.No;
                if (checkKeyPresenceInDirection(blockInDirection, p1) == Result.PickedUpB) {
                    p1.setXPos(p1.getXPos() + 1);
                    return Result.PickedUpB;
                }
                p1.setXPos(p1.getXPos() + 1); //Player is allowed to move, update its position.
            }
        }
        if (playerNumber == 2){
            if (command.equals("up")){
                int lookAtPosition = Math.abs(p2.getyPos()+1)*10 + p2.getXPos();
                String blockInDirection = mapString.substring(lookAtPosition, lookAtPosition + 1);
                if (blockInDirection.equals("#") || (p2.getXPos() == p1.getXPos() && p1.getyPos()==p2.getyPos()+1)) return Result.No;
                if (checkKeyPresenceInDirection(blockInDirection, p2) == Result.PickedUpC){
                    p2.setyPos(p2.getyPos() + 1);
                    return Result.PickedUpC;
                }
                p2.setyPos(p2.getyPos() + 1); //Player is allowed to move, update its position.
            }
            if (command.equals("down")){
                int lookAtPosition = Math.abs(p2.getyPos()-1)*10 + p2.getXPos();
                String blockInDirection = mapString.substring(lookAtPosition, lookAtPosition + 1);
                if (blockInDirection.equals("#") || (p2.getXPos() == p1.getXPos() && p1.getyPos()==p2.getyPos()-1)) return Result.No;
                if (checkKeyPresenceInDirection(blockInDirection, p2) == Result.PickedUpC){
                    p2.setyPos(p2.getyPos() - 1);
                    return Result.PickedUpC;
                }
                p2.setyPos(p2.getyPos() - 1); //Player is allowed to move, update its position.
            }
            if (command.equals("left")){
                int lookAtPosition = Math.abs(p2.getyPos())*10 + p2.getXPos() - 1;
                String blockInDirection = mapString.substring(lookAtPosition, lookAtPosition + 1);
                if (blockInDirection.equals("#") || (p2.getyPos() == p1.getyPos() && p1.getXPos()==p2.getXPos()-1)) return Result.No;
                if (checkKeyPresenceInDirection(blockInDirection, p2) == Result.PickedUpC) {
                    p2.setXPos(p2.getXPos() - 1);
                    return Result.PickedUpC;
                }
                p2.setXPos(p2.getXPos() - 1); //Player is allowed to move, update its position.
            }
            if (command.equals("right")){
                int lookAtPosition = Math.abs(p2.getyPos())*10 + p2.getXPos() + 1;
                String blockInDirection = mapString.substring(lookAtPosition, lookAtPosition + 1);
                if (blockInDirection.equals("#") || (p2.getyPos() == p1.getyPos() && p1.getXPos()==p2.getXPos()+1)) return Result.No;
                if (checkKeyPresenceInDirection(blockInDirection, p2) == Result.PickedUpC) {
                    p2.setXPos(p2.getXPos() + 1);
                    return Result.PickedUpC;
                }
                p2.setXPos(p2.getXPos() + 1); //Player is allowed to move, update its position.
            }
        }
        if (checkForWinningState()) return Result.Won; //The the end, not matter who moved, check for the winning state.
        return answer;
    }

    public void shutdown(){
        gameIsOn=false;
    }

    public boolean isOn(){
        return gameIsOn;
    }

    private void positionPlayer(Player p /*String s*/) {
        String temp = String.valueOf(p.getPlayerNumber());
        int xPos = mapString.indexOf(temp) % 10;
        int yPos = (int)Math.floor(mapString.indexOf(temp) / 10.0);
        p.setXPos(xPos);
        //Negative due to "down" being y-negative direction:
        p.setyPos(-yPos); 
    }
    
    public void play(){
        positionPlayer(p1);
        positionPlayer(p2);
        this.gameIsOn = true;
        System.out.println("Playing the game...");
    }

    public Player getP1() {
        return p1;
    }

    public void setP1(Player p1) {
        this.p1 = p1;
    }

    public Player getP2() {
        return p2;
    }

    public void setP2(Player p2) {
        this.p2 = p2;
    }

    protected class Key {
        private String name;
        private String opensDoor;

        private Key(String name, String opensDoor) {
            this.name = name;
            this.opensDoor = opensDoor;
        }

        public String getOpensDoor() {
            return opensDoor;
        }

        public void setOpensDoor(String opensDoor) {
            this.opensDoor = opensDoor;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}

