package org.ems;
public class Player {
        private int playerNumber;
        private Game.Key key;
        private int XPos;
        private int yPos; //Remember, down is negative y.

        public Player(int playerNumber) {
            this.playerNumber = playerNumber;
        }

        public int getXPos() {
                return XPos;
        }

        public void setXPos(int XPos) {
                this.XPos = XPos;
        }

        public int getyPos() {
                return yPos;
        }

        public void setyPos(int yPos) {
                this.yPos = yPos;
        }

        public int getPlayerNumber() {
                return playerNumber;
        }

        public Game.Key getKey() {
                return key;
        }

        public void setKey(Game.Key key) {
                this.key = key;
        }
}
