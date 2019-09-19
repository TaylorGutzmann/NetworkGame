package Network_Game;

/*
 * Grab.java
 *
 * This is the Client for the Grab game.
 * Two-player, networked game.  Run around and grab coins while being able to smash walls
 *
 * Code modified from mike slattery
 *
 * modified by Taylor Gutzmann April 2018
 * 
 */
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.media.*;
import javafx.stage.Stage;
import java.net.*;
import java.io.*;

public class Grab extends Application implements Runnable {
	final String appName = "Grab";

	GraphicsContext gc; // declare here to use in handlers

	/* the Thread */
	Thread kicker;

	int grid[][] = new int[GameGroup.GWD][GameGroup.GHT]; // Game board
	public static final int CELLSIZE = 64;
	public static final int WIDTH = GameGroup.GWD * CELLSIZE;
	public static final int PLAY_HEIGHT = GameGroup.GHT * CELLSIZE;
	public static final int TOTAL_HEIGHT = (GameGroup.GHT+1) * CELLSIZE;
	public static final int UI_HEIGHT = CELLSIZE;
	boolean setup = false; // record whether we've got the board yet
	Player blue = null, red = null;
	String my_name;
	int winner = -1;
	
	AudioClip coin = new AudioClip(getClass().getResource("Coin.wav").toString());
	AudioClip blast = new AudioClip(getClass().getResource("Blast.wav").toString());
	AudioClip gg = new AudioClip(getClass().getResource("GameOver.wav").toString());
	AudioClip tie = new AudioClip(getClass().getResource("Tie.wav").toString());

	/* the network stuff */
	PrintWriter pw;
	Socket s = null;
	BufferedReader br = null;
	String name, theHost = "192.168.1.76";
	int thePort = 2001;

	void initialize() {
		makeContact();
		/* start a new game */
		/* start the thread */
		kicker = new Thread(this);
		kicker.setPriority(Thread.MIN_PRIORITY);
		kicker.setDaemon(true);
		kicker.start();
		render(gc);
	}

	private void makeContact() {
	// contact the GameServer
		/* ok, now make the socket connection */
		while (s == null)
			try {
				System.out.println("Attempting to make connection:" + theHost + ", " + thePort);
				s = new Socket(theHost, thePort);
				br = new BufferedReader(new InputStreamReader(s.getInputStream()));
				pw = new PrintWriter(s.getOutputStream());
			} catch (Exception e) {
				System.out.println(e);
				try {
					Thread.sleep(7500);
				} catch (Exception ex) {
				}
				;
			}

		System.out.println("Connection established");

	} // end of makeContact()

	/* the main Thread loop */
	public void run() {

		/*
		 * Here is the main network loop Wait for messages from the server
		 */
		while (kicker != null) {
			String input = null;
			
			while (input == null)
				try {
					Thread.sleep(100);
					input = br.readLine();
				} catch (Exception e) {
					input = null;
				}

			System.out.println("Got input:" + input);

			// Chop up the message and see what to do
			String[] words = input.split(",");
			String cmd = words[0];

			/* if we are ready to start a game */
			if (cmd.equals("start")) {
				fillGrid(words[1]);
				setup = true;
				render(gc);
			} else if (cmd.equals("who")) {
				my_name = words[1];
			} else if (cmd.equals("blue")) {
				try {
					if (blue == null)
						blue = new Player(0, 0, 0, Color.BLUE);
					blue.x = Integer.valueOf(words[1]).intValue();
					blue.y = Integer.valueOf(words[2]).intValue();
					blue.dir = Integer.valueOf(words[3]).intValue();
				} catch (Exception e) {
				}
				; // if nonsense message, just ignore it
				render(gc);
			} else if (cmd.equals("red")) {
				try {
					if (red == null)
						red = new Player(0, 0, 0, Color.RED);
					red.x = Integer.valueOf(words[1]).intValue();
					red.y = Integer.valueOf(words[2]).intValue();
					red.dir = Integer.valueOf(words[3]).intValue();
				} catch (Exception e) {
				}
				; // if nonsense message, just ignore it
				render(gc);
			} else if (cmd.equals("grab")) {
				fillGrid(words[2]);
				coin.play();
				String ply = words[1];
				if(ply.equals("blue")) {
					blue.score++;
				}
				if(ply.equals("red")) {
					red.score++;
				}
				winner = checkScore();
				switch(winner) {
				case 0:
					gc.setStroke(Color.BLUE);
					gc.strokeText("BLUE WINS!!!", 350, Grab.TOTAL_HEIGHT-Grab.UI_HEIGHT+50);
					gg.play();
					break;
				case 1:
					gc.setStroke(Color.RED);
					gc.strokeText("RED WINS!!!", 350, Grab.TOTAL_HEIGHT-Grab.UI_HEIGHT+50);
					gg.play();
					break;
				case 2:
					gc.setStroke(Color.BLACK);
					gc.strokeText("TIE!!!", 350, Grab.TOTAL_HEIGHT-Grab.UI_HEIGHT+50);
					break;
				}
				render(gc);
			} else if (cmd.equals("blast")) {
				fillGrid(words[2]);
				blast.play();
				String ply = words[1];
				if(ply.equals("blue")) {
					blue.ability--;
				}
				if(ply.equals("red")) {
					red.ability--;
				}
				render(gc);
			}
		}
	}

	void fillGrid(String board) {
		// Fill in the grid array with the values
		// in the String board.
		int x, y, i = 0;
		char c;

		for (y = 0; y < GameGroup.GHT; y++)
			for (x = 0; x < GameGroup.GWD; x++) {
				c = board.charAt(i);
				i++;
				switch (c) {
				case '0':
					grid[x][y] = 0;
					break;
				case '1':
					grid[x][y] = 1;
					break;
				case '2':
					grid[x][y] = 2;
					break;
				}
			}
	}

	/* if the Thread stops, be sure to clean up! */
	public void finalize() {

		try {
			br.close();
			pw.close();
			s.close();
		} catch (Exception e) {
		}
		;
	}

	public void render(GraphicsContext gc) {
		int x, y;
		
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, WIDTH, TOTAL_HEIGHT);
		if (!setup) {
			gc.setFill(Color.BLACK);
			gc.fillText("Waiting...", 50, 50);
		} else {
			// Draw board
			for (x = 0; x < GameGroup.GWD; x++)
				for (y = 0; y < GameGroup.GHT; y++) {
					if (grid[x][y] == 1) {
						gc.setFill(Color.GRAY);
						gc.fillRect(CELLSIZE * x, CELLSIZE * y, CELLSIZE - 1, CELLSIZE - 1);
					} else if (grid[x][y] == 2) {
						gc.setFill(Color.ORANGE);
						gc.fillOval(CELLSIZE * x + 2, CELLSIZE * y + 2, CELLSIZE - 4, CELLSIZE - 4);
					}
				}
			gc.setStroke(Color.BLACK);
			gc.strokeRect(0, 0, WIDTH, PLAY_HEIGHT);
			// Add the players if they're there
			if (blue != null) {
				gc.setStroke(Color.BLUE);
				gc.strokeText("Blue player score: " + blue.score, 450, Grab.TOTAL_HEIGHT-Grab.UI_HEIGHT+10);
				gc.setStroke(Color.BLUE);
				gc.strokeText("Blue player blast charges: " + blue.ability, 450, Grab.TOTAL_HEIGHT-Grab.UI_HEIGHT+20);
				blue.render(gc);
			}
			if (red != null) {
				gc.setStroke(Color.RED);
				gc.strokeText("Red player score: " + red.score, 150, Grab.TOTAL_HEIGHT-Grab.UI_HEIGHT+10);
				gc.setStroke(Color.RED);
				gc.strokeText("Red player blast charges: " + red.ability, 150, Grab.TOTAL_HEIGHT-Grab.UI_HEIGHT+20);
				red.render(gc);
			}
		}
	}

	public void tellServer(String msg) {
		/* send a message to the server */
		boolean flag = false;
		while (!flag) // we keep trying until it's sent
			try {
				pw.println(msg);
				pw.flush();
				flag = true;
			} catch (Exception e1) {
				flag = false;
			}
	}
	int checkScore() {
		try {
			if(blue.score + red.score == 8) {
				if(blue.score > red.score) {
					return 0;
				}
				if(blue.score < red.score) {
					return 1;
				}
				if(blue.score == red.score) {
					return 2;
				}
			}
		} catch (Exception e) {
			
		}
		return -1;
	}

	void setHandlers(Scene scene) {
		scene.setOnKeyPressed(e -> {
			KeyCode c = e.getCode();
			switch (c) {
			case J:
			case LEFT:
				tellServer("turnleft," + my_name);
				break;
			case L:
			case RIGHT:
				tellServer("turnright," + my_name);
				break;
			case K:
			case UP:
				tellServer("step," + my_name);
				break;
			case G:
				tellServer("grab," + my_name);
				break;
			case B:
				if(my_name.equals("red") && red.ability > 0)
					tellServer("blast," + my_name);
				if(my_name.equals("blue") && blue.ability > 0)
					tellServer("blast," + my_name);
					break;
			default: /* Do Nothing */
				break;
			}
		});
	}

	/*
	 * Begin boiler-plate code... [Events with initialization]
	 */
	public static void main(String[] args) {
		launch(args);
	}

	public void start(Stage theStage) {
		theStage.setTitle(appName);

		Group root = new Group();
		Scene theScene = new Scene(root);
		theStage.setScene(theScene);

		Canvas canvas = new Canvas(WIDTH, TOTAL_HEIGHT);
		root.getChildren().add(canvas);

		gc = canvas.getGraphicsContext2D();

		// Initial setup
		initialize();

		setHandlers(theScene);

		theStage.show();
	}
	/*
	 * ... End boiler-plate code
	 */
}
