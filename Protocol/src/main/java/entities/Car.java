package entities;


import lombok.Data;

@Data
public class Car {
    private String dir;

    //Constructor
    public Car(int player) {
        if (player == 1) {
            dir = "r";
        } else {
            dir = "l";
        }
    }
}