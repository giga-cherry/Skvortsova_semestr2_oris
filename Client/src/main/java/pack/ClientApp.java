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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class ClientApp extends Application {
    static Rectangle[] bar1 = new Rectangle[3];
    Group barPlayer1 = new Group();
    static Rectangle[] bar2 = new Rectangle[3];
    Group barPlayer2 = new Group();
    private static int positionPlayer;
    private static Client client;

    private static final int speed1P = 3;
    private static final int speed2P = 3;
    private static final int jumpLength = 12;


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

        stack.setStyle("-fx-background-color: #67C8FF;" + "-fx-border-color: grey;");
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

        BorderPane head = new BorderPane();
        InputStream is = Files.newInputStream(Paths.get("Client/src/main/java/resources/TronLogo.jpg"));
        Image logo = new Image(is, 200, 100, true, true);
        ImageView iv = new ImageView(logo);

        VBox player1info = new VBox(5);

        VBox player2info = new VBox(5);

        head.setStyle("-fx-background-color: rgba(0,0,0,1);");
        head.setLeft(player1info);
        head.setCenter(iv);
        head.setRight(player2info);
        head.setPadding(new Insets(20));

        BorderPane pane = new BorderPane();
        pane.setStyle("-fx-background-color: blue;");
        pane.setTop(head);
        pane.setCenter(stack);

        Scene scene = new Scene(pane);
        scene.setOnKeyPressed(game::processKeyPress);
        positionPlayer = client.getPositionPlayer();
        stage.setTitle("Tron");
        stage.setScene(scene);
        stage.show();
    }

    public static class GamePane extends Pane {
        public ArrayList<Line> linesP1 = new ArrayList<Line>();
        public ArrayList<Line> linesP2 = new ArrayList<Line>();

        public boolean crash1 = false;
        public boolean crash2 = false;

        public int player1wins = 0;
        private int player2wins = 0;

        private final Board game;
        private Circle p1, p2;
        private double p1x, p1y, p2x, p2y;
        private Line tailP1, tailP2;

        private final Timeline animation;
        private boolean isPaused = true;

        Label press;

        Label winnerText;

        public GamePane(int i) {

            press = new Label("Press Space Bar to Start");
            String winner = "";
            winnerText = new Label(winner);

            p1x = 100.0;
            p1y = 260.0;
            p2x = 400.0;
            p2y = 260.0;


            winnerText.setFont(new Font("Arial", 20));

            game = new Board();
            reset();
            press.setStyle("-fx-background-color: black;" + "-fx-border-color: cyan;" + "-fx-text-fill: white;");
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

        public void play() {
            animation.play();
            isPaused = false;
        }

        public void pause() {
            animation.pause();
            winnerText.setText("PAUSE");
            isPaused = true;
            winnerText.setVisible(true);
        }

        private boolean playGame = false;

        protected void movePlayer() throws IOException {
            String s = client.read();
            if (s.equals("pause")) {
                pause();
                playGame = false;
            }
            if (playGame) {
                String[] coordinate;
                String cor;
                if (positionPlayer == 1) {
                    if (game.getPlayer(positionPlayer).getDir().equals("l")) {
                        p1x -= speed1P;
                    } else if (game.getPlayer(positionPlayer).getDir().equals("u")) {
                        p1y -= speed1P;
                    } else if (game.getPlayer(positionPlayer).getDir().equals("r")) {
                        p1x += speed1P;
                    } else if (game.getPlayer(positionPlayer).getDir().equals("d")) {
                        p1y += speed1P;
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
                        p2x -= speed2P;
                    } else if (game.getPlayer(positionPlayer).getDir().equals("u")) {
                        p2y -= speed2P;
                    } else if (game.getPlayer(positionPlayer).getDir().equals("r")) {
                        p2x += speed2P;
                    } else if (game.getPlayer(positionPlayer).getDir().equals("d")) {
                        p2y += speed2P;
                    }
                    client.send(p2x, p2y);
                    cor = client.read();
                    if (!cor.equals("null") && !cor.equals("play")) {
                        coordinate = client.read().split(",");
                        p1x = Double.parseDouble(coordinate[0]);
                        p1y = Double.parseDouble(coordinate[1]);
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

                tailP1.setEndX(p1x);
                tailP1.setEndY(p1y);
                tailP2.setEndX(p2x);
                tailP2.setEndY(p2y);


                Point point1 = new Point(p1x, p1y);
                Point point2 = new Point(p2x, p2y);

                crash1 = checkCrash1(point1);
                if (crash1) {
                    crash(1);
                }
                crash2 = checkCrash1(point2);
                if (crash2) {
                    crash(2);
                }

                crash2 = check(linesP2, p2);
                crash1 = check(linesP1, p1);

                if (crash1) {
                    crash(1);
                }
                if (crash2) {
                    crash(2);
                }

                crash1 = check2(linesP2, p1);
                crash2 = check2(linesP1, p2);
                if (crash1) {
                    crash(1);
                }
                if (crash2) {
                    crash(2);
                }
            } else if (s.equals("play")) {
                playGame = true;
                isPaused = false;
            }
        }

        protected void jumpPlayer() {
            if (positionPlayer == 1) {
                if (game.getPlayer(positionPlayer).getDir().equals("l")) {
                    p1x -= jumpLength;
                } else if (game.getPlayer(positionPlayer).getDir().equals("u")) {
                    p1y -= jumpLength;
                } else if (game.getPlayer(positionPlayer).getDir().equals("r")) {
                    p1x += jumpLength;
                } else if (game.getPlayer(positionPlayer).getDir().equals("d")) {
                    p1y += jumpLength;
                }
            } else {
                if (game.getPlayer(positionPlayer).getDir().equals("l")) {
                    p2x -= jumpLength;
                } else if (game.getPlayer(positionPlayer).getDir().equals("u")) {
                    p2y -= jumpLength;
                } else if (game.getPlayer(positionPlayer).getDir().equals("r")) {
                    p2x += jumpLength;
                } else if (game.getPlayer(positionPlayer).getDir().equals("d")) {
                    p2y += jumpLength;
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
                l.setStroke(Color.CYAN);
            else
                l.setStroke(Color.HOTPINK);
            return l;
        }
        public void crash(int i) {
            reset();
            isPaused = true;
            client.pause();
            pause();
            playGame = false;
            if (i == 1) {
                winnerText.setText("PLAYER 2 WINS");
                bar2[player2wins].setFill(Color.HOTPINK);
                player2wins++;
                win();
            } else if (i == 2) {
                winnerText.setText("PLAYER 1 WINS");
                bar1[player1wins].setFill(Color.CYAN);
                player1wins++;
                win();
            } else if (i == 3) {
                winnerText.setText("TIE");
            }
        }

        public void win() {
            reset();
            p1x = 100.0;
            p1y = 260.0;
            p2x = 400.0;
            p2y = 260.0;
            if (player1wins == 3) {
                winnerText.setText("CONGRATULATIONS PLAYER 1! YOU WON!");
                player1wins = 0;
                player2wins = 0;
            } else if (player2wins == 3) {
                winnerText.setText("CONGRATULATIONS PLAYER 2! YOU WON!");
                player2wins = 0;
                player1wins = 0;
            }

            winnerText.setVisible(true);
        }

        public void reset() {
            getChildren().clear();
            game.getPlayer(1).setDir("r");
            game.getPlayer(2).setDir("l");

            p1x = 100;
            p1y = 260;
            p2x = 400;
            p2y = 260;

            if (positionPlayer == 1) {
                client.send(p2x, p2y);
            } else {
                client.send(p1x, p1y);
            }

            tailP1 = new Line(p1x, p1y, p1x, p1y);
            tailP2 = new Line(p2x, p2y, p2x, p2y);

            tailP1.setStrokeWidth(0.5);
            tailP1.setStroke(Color.CYAN);
            tailP1.setStrokeLineCap(StrokeLineCap.ROUND);
            tailP2.setStrokeWidth(0.5);
            tailP2.setStroke(Color.HOTPINK);
            tailP2.setStrokeLineCap(StrokeLineCap.ROUND);

            p1 = new Circle(p1x, p1y, 2.5);
            p2 = new Circle(p2x, p2y, 2.5);
            p1.setFill(Color.CYAN);
            p2.setFill(Color.HOTPINK);

            crash1 = false;
            crash2 = false;
            linesP1.clear();
            linesP2.clear();

            getChildren().addAll(p1, p2, tailP1, tailP2, press, winnerText);
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
}