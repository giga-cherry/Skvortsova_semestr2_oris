package entities;

public class Board {
    //Attributes
    private Car player1, player2;

    public Board() {
        player1 = new Car(1);
        player2 = new Car(2);
    }

    public Car getPlayer(int player) {
        if (player == 1)
            return player1;
        else
            return player2;
    }
}