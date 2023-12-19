package pack;

import entities.Board;
import entities.Point;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;

public class ClientApp extends Application {
    static Rectangle[] bar1 = new Rectangle[3];
    Group barPlayer1 = new Group();
    static Rectangle[] bar2 = new Rectangle[3];
    Group barPlayer2 = new Group();
    
    
    private static int positionPlayer;
    private static Client client;
    private static final int SPEED_1_P = 3;
    private static final int SPEED_2_P = 3;
    private static final int JUMP_LENGTH = 21;
    private static final Color FIRST_COLOR = Color.YELLOW;
    private static final Color SECOND_COLOR = Color.ORANGE;
    private static final double[] COORDS = new double[]{100.0, 260.0,400.0,260.0};


    @Override
    public void start(Stage stage) throws Exception {
        String ipAddr = "localhost";
        int port = 8080;
        client = new Client(ipAddr, port);

        StackPane stack = new StackPane();

        GridPane board = new GridPane();
        Pane[][] grid = new Pane[25][25];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {
                grid[i][j] = new Pane();
                grid[i][j].setPrefSize(20, 20);
                grid[i][j].setStyle("-fx-background-color: black;");
                board.add(grid[i][j], i, j);
            }
        }

        stack.setStyle("-fx-background-color: #03d32c;" + "-fx-border-color: grey;");
        stack.setPadding(new Insets(2));
        stack.setMinSize(board.getMinWidth(), board.getMinHeight());

        int xOffset = 10;
        for (int i = 0; i < 3; i++) {

            Rectangle r1 = new Rectangle(xOffset, 15, 20, 5);
            r1.setFill(Color.web("#262626"));
            bar1[i] = r1;
            barPlayer1.getChildren().add(r1);

            Rectangle r2 = new Rectangle(xOffset, 15, 20, 5);
            r2.setFill(Color.web("#262626"));
            bar2[i] = r2;
            barPlayer2.getChildren().add(r2);

            xOffset += 22;
        }


        GamePane game = new GamePane(1);
        game.setMinSize(500, 500);

        stack.getChildren().addAll(board, game);

        VBox player1info = new VBox(5);

        VBox player2info = new VBox(5);

        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: #03d32c;");
        pane.setCenter(stack);

        Scene scene = new Scene(pane);
        scene.setOnKeyPressed(game::processKeyPress);
        positionPlayer = client.getPositionPlayer();
        stage.setScene(scene);
        stage.show();
    }

    public static class GamePane extends Pane {
        public ArrayList<Line> linesP1 = new ArrayList<Line>();
        public ArrayList<Line> linesP2 = new ArrayList<Line>();

        private int player1wins = 0;
        private int player2wins = 0;

        private final Board game;
        private Circle p1, p2;
        private double p1x, p1y, p2x, p2y;
        private Line tailP1, tailP2;
        private boolean skipNext = false;
        private final Timeline animation;
        private boolean isPaused = true;
        private boolean playGame = false;

        Label press;

        Label winnerText;

        public GamePane(int i) {

            press = new Label("Press Space Bar to Start");
            String winner = "";
            winnerText = new Label(winner);

            coordsToStart();


            winnerText.setFont(new Font("Arial", 20));

            game = new Board();
            reset();
            press.setStyle("-fx-background-color: black;" + "-fx-background-color: #03d32c;" + "-fx-text-fill: white;");
            press.layoutXProperty().bind(this.widthProperty().subtract(press.widthProperty()).divide(2));
            press.layoutYProperty().bind(this.heightProperty().subtract(press.heightProperty()).divide(2));
            press.setPadding(new Insets(5));

            winnerText.setStyle("-fx-background-color: black;" + "-fx-border-color: white;" + "-fx-text-fill: white;");
            winnerText.layoutXProperty().bind(this.widthProperty().subtract(winnerText.widthProperty()).divide(2));
            winnerText.layoutYProperty().bind(this.heightProperty().subtract(winnerText.heightProperty()).divide(2));
            winnerText.setVisible(false);

            reset();

            animation = new Timeline(
                    new KeyFrame(Duration.millis(50), e -> {
                        try {
                            movePlayer();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }));
            animation.setCycleCount(Timeline.INDEFINITE);
            animation.pause();
        }
        
        private void coordsToStart(){
            p1x = COORDS[0];
            p1y = COORDS[1];
            p2x = COORDS[2];
            p2y = COORDS[3];
        }

        public void play() {
            animation.play();
            isPaused = false;
        }

        public void pause() {
            animation.pause();
            isPaused = true;
        }

        public void pauseManually() {
            animation.pause();
            winnerText.setText("PAUSE");
            isPaused = true;
            winnerText.setVisible(true);
            client.pause();
        }

        protected void movePlayer() throws IOException {
            String s = client.read();
            if ("pause".equals(s)) {
                System.out.println(positionPlayer + "paused");
                pause();
                playGame = false;
            }
            if (client.isTie()) {
                endGame(4);
            } else{
                if (playGame) {
                    if (client.isJump()) {
                        skipNext = true;
                    } else {
                        String[] coordinate;
                        String cor;
                        if (positionPlayer == 1) {
                            if (game.getPlayer(positionPlayer).getDir().equals("l")) {
                                p1x -= SPEED_1_P;
                            } else if (game.getPlayer(positionPlayer).getDir().equals("u")) {
                                p1y -= SPEED_1_P;
                            } else if (game.getPlayer(positionPlayer).getDir().equals("r")) {
                                p1x += SPEED_1_P;
                            } else if (game.getPlayer(positionPlayer).getDir().equals("d")) {
                                p1y += SPEED_1_P;
                            }
                            client.send(p1x, p1y);
                            cor = client.read();
                            if (!cor.equals("null") && !cor.equals("play")) {
                                coordinate = client.read().split(",");
                                p2x = Double.parseDouble(coordinate[0]);
                                p2y = Double.parseDouble(coordinate[1]);
                            }
                        } else {
                            if (game.getPlayer(positionPlayer).getDir().equals("l")) {
                                p2x -= SPEED_2_P;
                            } else if (game.getPlayer(positionPlayer).getDir().equals("u")) {
                                p2y -= SPEED_2_P;
                            } else if (game.getPlayer(positionPlayer).getDir().equals("r")) {
                                p2x += SPEED_2_P;
                            } else if (game.getPlayer(positionPlayer).getDir().equals("d")) {
                                p2y += SPEED_2_P;
                            }
                            client.send(p2x, p2y);
                            cor = client.read();
                            if (!cor.equals("null") && !cor.equals("play")) {
                                coordinate = client.read().split(",");
                                p1x = Double.parseDouble(coordinate[0]);
                                p1y = Double.parseDouble(coordinate[1]);
                            }
                        }
                    }

                    if (skipNext) {

                        if (positionPlayer == 1 && distanceFromPrevious(linesP2, p2x, p2y)) {
                            linesP2.add(tailP2);
                            tailP2 = tail(
                                    p2x,
                                    p2y,
                                    p2x, p2y, 2);
                            getChildren().add(tailP2);
                            skipNext = false;
                        }
                        if (positionPlayer == 2 && distanceFromPrevious(linesP1, p1x, p1y)) {
                            linesP1.add(tailP1);
                            tailP1 = tail(
                                    p1x,
                                    p1y,
                                    p1x, p1y, 1);
                            getChildren().add(tailP1);
                            skipNext = false;
                        }
                    }


                    linesP1.add(tailP1);
                    tailP1 = tail(
                            linesP1.get(linesP1.size() - 1).endXProperty().doubleValue(),
                            linesP1.get(linesP1.size() - 1).endYProperty().doubleValue(),
                            p1x, p1y, 1);
                    p1.setCenterX(p1x);
                    p1.setCenterY(p1y);
                    getChildren().add(tailP1);

                    linesP2.add(tailP2);
                    tailP2 = tail(
                            linesP2.get(linesP2.size() - 1).endXProperty().doubleValue(),
                            linesP2.get(linesP2.size() - 1).endYProperty().doubleValue(),
                            p2x, p2y, 2);
                    p2.setCenterX(p2x);
                    p2.setCenterY(p2y);
                    getChildren().add(tailP2);


                    Point point1 = new Point(p1x, p1y);
                    Point point2 = new Point(p2x, p2y);

                    if (checkTie(point1, point2)) {
                        endGame(3);
                    } else {
                        if (checkCrash1(point1)) {
                            endGame(1);
                        }
                        if (checkCrash1(point2)) {
                            endGame(2);
                        }


                        if (check(linesP1, p1)) {
                            endGame(1);
                        }
                        if (check(linesP2, p2)) {
                            endGame(2);
                        }


                        if (check2(linesP2, p1)) {
                            endGame(1);
                        }
                        if (check2(linesP1, p2)) {
                            endGame(2);
                        }
                    }
                } else if ("play".equals(s)) {
                    playGame = true;
                    isPaused = false;
                }
        }
        }

        protected void jumpPlayer() {
            if (positionPlayer == 1) {
                if (game.getPlayer(positionPlayer).getDir().equals("l")) {
                    p1x -= JUMP_LENGTH;
                } else if (game.getPlayer(positionPlayer).getDir().equals("u")) {
                    p1y -= JUMP_LENGTH;
                } else if (game.getPlayer(positionPlayer).getDir().equals("r")) {
                    p1x += JUMP_LENGTH;
                } else if (game.getPlayer(positionPlayer).getDir().equals("d")) {
                    p1y += JUMP_LENGTH;
                }
            } else {
                if (game.getPlayer(positionPlayer).getDir().equals("l")) {
                    p2x -= JUMP_LENGTH;
                } else if (game.getPlayer(positionPlayer).getDir().equals("u")) {
                    p2y -= JUMP_LENGTH;
                } else if (game.getPlayer(positionPlayer).getDir().equals("r")) {
                    p2x += JUMP_LENGTH;
                } else if (game.getPlayer(positionPlayer).getDir().equals("d")) {
                    p2y += JUMP_LENGTH;
                }
            }
            linesP1.add(tailP1);
            tailP1 = tail(
                    p1x,
                    p1y,
                    p1x, p1y, 1);
            getChildren().add(tailP1);
            linesP2.add(tailP2);
            tailP2 = tail(
                    p2x,
                    p2y,
                    p2x, p2y, 2);
            getChildren().add(tailP2);
            client.sendJump();
        }

        public void processKeyPress(KeyEvent e) {
            if (e.getCode() == KeyCode.SPACE && isPaused) {
                client.unpause();
                client.sendPlay();
                play();
                isPaused = false;
                press.setVisible(false);
                winnerText.setVisible(false);
            } else if (e.getCode() == KeyCode.SPACE) {
                jumpPlayer();
            } else if (e.getCode() == KeyCode.P) {
                pauseManually();
            } else if (positionPlayer == 1) {
                if (e.getCode() == KeyCode.W && !game.getPlayer(positionPlayer).getDir().equals("d")) {
                    game.getPlayer(1).setDir("u");
                } else if (e.getCode() == KeyCode.A && !game.getPlayer(positionPlayer).getDir().equals("r")) {
                    game.getPlayer(1).setDir("l");
                } else if (e.getCode() == KeyCode.S && !game.getPlayer(positionPlayer).getDir().equals("u")) {
                    game.getPlayer(1).setDir("d");
                } else if (e.getCode() == KeyCode.D && !game.getPlayer(positionPlayer).getDir().equals("l")) {
                    game.getPlayer(1).setDir("r");
                }
            } else {
                if (e.getCode() == KeyCode.W && !game.getPlayer(positionPlayer).getDir().equals("d")) {
                    game.getPlayer(2).setDir("u");
                } else if (e.getCode() == KeyCode.A && !game.getPlayer(positionPlayer).getDir().equals("r")) {
                    game.getPlayer(2).setDir("l");
                } else if (e.getCode() == KeyCode.S && !game.getPlayer(positionPlayer).getDir().equals("u")) {
                    game.getPlayer(2).setDir("d");
                } else if (e.getCode() == KeyCode.D && !game.getPlayer(positionPlayer).getDir().equals("l")) {
                    game.getPlayer(2).setDir("r");
                }
            }
        }

        public Line tail(double sx, double sy, double ex, double ey, int player) {
            Line l = new Line(sx, sy, ex, ey);
            l.setStrokeWidth(0.5);
            l.setStrokeLineCap(StrokeLineCap.ROUND);
            if (player == 1)
                l.setStroke(FIRST_COLOR);
            else
                l.setStroke(SECOND_COLOR);
            return l;
        }

        public void endGame(int i) {
            reset();
            isPaused = true;
            client.pause();
            pause();
            playGame = false;

            if (i == 1) {
                winnerText.setText("PLAYER 2 WINS");
                winnerText.setTextFill(SECOND_COLOR);
//                bar2[player2wins].setFill(SECOND_COLOR);
                player2wins++;
                reset();
            } else if (i == 2) {
                winnerText.setText("PLAYER 1 WINS");
                winnerText.setTextFill(FIRST_COLOR);
                player1wins++;
                reset();
            } else if (i == 3) {
                winnerText.setText("TIE");
                System.out.println("own");
                client.sendTie();
                reset();
            }
            else if (i == 4) {
                winnerText.setText("TIE");
                System.out.println("checked");
                reset();
            }
            winnerText.setVisible(true);
        }

        public void reset() {
            getChildren().clear();
            game.getPlayer(1).setDir("r");
            game.getPlayer(2).setDir("l");

            coordsToStart();

            if (positionPlayer == 1) {
                client.send(p2x, p2y);
            } else {
                client.send(p1x, p1y);
            }

            tailP1 = new Line(p1x, p1y, p1x, p1y);
            tailP2 = new Line(p2x, p2y, p2x, p2y);

            tailP1.setStrokeWidth(0.5);
            tailP1.setStroke(FIRST_COLOR);
            tailP1.setStrokeLineCap(StrokeLineCap.ROUND);
            tailP2.setStrokeWidth(0.5);
            tailP2.setStroke(SECOND_COLOR);
            tailP2.setStrokeLineCap(StrokeLineCap.ROUND);

            p1 = new Circle(p1x, p1y, 2.5);
            p2 = new Circle(p2x, p2y, 2.5);
            p1.setFill(FIRST_COLOR);
            p2.setFill(SECOND_COLOR);

            linesP1.clear();
            linesP2.clear();

            getChildren().addAll(p1, p2, tailP1, tailP2, press, winnerText);

            player2wins = 0;
            player1wins = 0;
        }

        public boolean checkTie(Point p1, Point p2){
            boolean a1=false;
            boolean a2=false;
            for (int i = (int) p1.getX()-2; i < p1.getX()+2; i++) {
                for (int j = (int) p2.getX()-2; j < p2.getX()+2; j++) {
                    if(i==j){
                        a1=true;
                    }
                }
            }
            for (int i = (int) p1.getY()-2; i < p1.getY()+2; i++) {
                for (int j = (int) p2.getY()-2; j < p2.getY()+2; j++) {
                    if(i==j){
                        a2=true;
                    }
                }
            }
            return a1&&a2;
        }

        public boolean check(ArrayList<Line> list, Circle l) {
            for (int i = 0; i < list.size() - 10; i++) {
                if (l.getBoundsInLocal().intersects(list.get(i).getBoundsInLocal())) {
                    return true;
                }
            }
            return false;
        }


        public boolean check2(ArrayList<Line> list, Circle l) {
            for (Line line : list) {
                if (l.getBoundsInLocal().intersects(line.getBoundsInLocal())) {
                    return true;
                }
            }
            return false;
        }

        public boolean checkCrash1(Point p) {
            return p.getX() < 0 || p.getX() > 500 || p.getY() < 0 || p.getY() > 500;
        }

    }

    public static void main(String[] args) {
        launch();
    }

    private static boolean distanceFromPrevious(ArrayList<Line> list, double x, double y){
        return Math.abs(list.get(list.size() -1).endXProperty().doubleValue() - x) > 4 ||
                Math.abs(list.get(list.size() -1).endYProperty().doubleValue() - y) > 4;
    }
}