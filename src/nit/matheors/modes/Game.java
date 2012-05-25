package nit.matheors.modes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PFont;
import processing.core.PImage;
import nit.matheors.CanTidyUp;
import nit.matheors.Coordinates;
import nit.matheors.GameComponent;
import nit.matheors.Matheors;
import nit.matheors.MatheorsConstants;
import nit.matheors.controls.Controller;
import nit.matheors.controls.KeyboardController;
import nit.matheors.controls.TUIOController;
import nit.matheors.model.Vector;
import nit.matheors.model.objects.Matheor;
import nit.matheors.model.objects.MatheorSize;
import nit.matheors.model.objects.Qbject;
import nit.matheors.model.objects.Shot;
import nit.matheors.model.objects.ShotType;
import nit.matheors.model.objects.Spacecraft;

import static processing.core.PApplet.round;

public class Game extends GameComponent implements MatheorsConstants, PConstants, MatheorsMode, CanTidyUp {

	private static List<CanTidyUp> tidyUps = new ArrayList<CanTidyUp>();
	
	public Game(Matheors parent, int noOfPlayers) {
		super(parent);
		this.noOfPlayers = noOfPlayers;
	}
	
	private int noOfPlayers;
	private int targetNumber;

	public int getNoOfPlayers() {
		return noOfPlayers;
	}

	public int getTargetNumber() {
		return targetNumber;
	}

	public int getP1Score() {
		return p1Score;
	}

	public int getP2Score() {
		return p2Score;
	}

	private int p1Score;
	private int p2Score;
	private int ticker;

	private PImage p1Scoreboard;
	private PImage p2Scoreboard;
	private PImage additionp1;
	private PImage subtractionp1;
	private PImage additionp2;
	private PImage subtractionp2;
	private PFont scoreFont;
	private PFont timerFont;
	private PFont targetNumberFont;

	private Spacecraft player1;
	private Spacecraft player2;
	
	private List<Qbject> qbjects = new CopyOnWriteArrayList<Qbject>();

	public void addQbject(Qbject o) {
		qbjects.add(o);
		if (o instanceof CanTidyUp)
			tidyUps.add((CanTidyUp) o);
	}
	
	public boolean mayBeAddQbject(Qbject o) {
		if (o != null) {
			addQbject(o);
			return true;
		}
		return false;
	}

	private PImage background = null;

	private void loadImages() {		
		background = getParent().loadImage("images\\background.jpg");
		additionp1 = getParent().loadImage("images\\shot\\additionp1.png");
		additionp2 = getParent().loadImage("images\\shot\\additionp2.png");
		subtractionp1 = getParent().loadImage("images\\shot\\subtractionp1.png");
		subtractionp2 = getParent().loadImage("images\\shot\\subtractionp2.png");
	}
	
	private void loadResources() {
		loadImages();
		scoreFont = getParent().loadFont("Arial-BoldMT-42.vlw");
		timerFont = getParent().loadFont("Arial-BoldMT-48.vlw");
		targetNumberFont = getParent().loadFont("Arial-BoldMT-38.vlw");
	}

	public void setup() {
		loadResources();
		
		getParent().textFont(getParent().getDefaultFont(), 12);

		targetNumber = round(getParent().random(LOWEST_TARGET_NUMBER, HIGHEST_TARGET_NUMBER));

		Coordinates p1coor = null;
		if (noOfPlayers == 1) {
			p1coor = new Coordinates(HALF_WIDTH, HALF_HEIGHT);
		} else {
			p1coor = new Coordinates(QUARTER_WIDTH, HALF_HEIGHT);
		}
		
		player1 = new Spacecraft(getParent(), this, PLAYER1_SPACECRAFT_TYPE, p1coor, new Vector(0, 0), SPACECRAFT_MAX_VELOCITY);
		Controller controllerPlayer1 = new KeyboardController(getParent(), UP, DOWN, RIGHT, LEFT, ENTER, 33, 34);
		Controller tuioP1Controller = new TUIOController(getParent(), this, 24, 29);
		controllerPlayer1.control(player1);
		tuioP1Controller.control(player1);
		tidyUps.add((CanTidyUp) controllerPlayer1);
		tidyUps.add((CanTidyUp) tuioP1Controller);
		p1Scoreboard = getParent().loadImage("images\\header_p1.png");
		p1Score = 0;
		
		addQbject(player1);
		
		if (noOfPlayers == 2) {
			player2 = new Spacecraft(getParent(), this, PLAYER2_SPACECRAFT_TYPE, new Coordinates(THREEQUARTERS_WIDTH, HALF_HEIGHT), new Vector(0, 0), SPACECRAFT_MAX_VELOCITY);
			Controller controllerPlayer2 = new KeyboardController(getParent(), 81, 65, 88, 90, SHIFT, 49, 50);
			controllerPlayer2.control(player2);			
			p2Scoreboard = getParent().loadImage("images\\header_p2.png");
			p2Score = 0;

			addQbject(player2);
		}
			
	}

	private List<Qbject> checkForCollisions() throws Exception {
		List<Qbject> resQbjects = new ArrayList<Qbject>();
		Map<Qbject, List<Qbject>> processed = new HashMap<Qbject, List<Qbject>>();
		for (Qbject o : qbjects) {
			if (o.getMassKg() == 0) {
				continue;
			}
			if (!processed.containsKey(o)) {
				processed.put(o, new ArrayList<Qbject>());
			}
			for (Qbject o2 : qbjects) {
				if (o != o2) {
					if (o2.getMassKg() == 0) {
						continue;							
					}
					// If the two objects have already been processed, break out of this iteration 
					if (processed.get(o).contains(o2)) {
						continue;
					}
					if (!processed.containsKey(o2)) {
						processed.put(o2, new ArrayList<Qbject>());
					}
					if (o.hasCollidedWith(o2)) {
						// They are in collision
						
						// First, we take care of object 1
						Qbject oc = o.createClone();
						boolean o1exploded = o.collideAndMaybeExplodeWith(o2);
						if (o1exploded && o instanceof Matheor && o2 instanceof Shot) {
							updateScores((Matheor) o, (Shot) o2);
						}
						// Now it's object 2's turn
						boolean o2exploded = o2.collideAndMaybeExplodeWith(oc);
						if (o2exploded && o2 instanceof Matheor && o instanceof Shot) {
							updateScores((Matheor) o2, (Shot) o);
						}
						
					}
					processed.get(o2).add(o);
				}
			}
		}
		for (Qbject o : qbjects) {
			if (!o.isDead())
				resQbjects.add(o);
		}
		return new CopyOnWriteArrayList<Qbject>(resQbjects);		
	}

	public void updateScores(Matheor m, Shot s) {
		if (s.getFiredBy() == 1) {
			p1Score += (m.getNumber() * (s.getType() == ShotType.ADDITION ? 1 : -1));
		} else {
			p2Score += (m.getNumber() * (s.getType() == ShotType.ADDITION ? 1 : -1));
		}
	}
	
	private int nextMatheorParams = 0;
	private float[][] matheorSpawnParams = new float[][] {
			new float[] {-50, -50, 280, 350},
			new float[] {SCREEN_WIDTH + 50, -50, 190, 260},
			new float[] {SCREEN_WIDTH + 50, SCREEN_HEIGHT + 50, 100, 170},
			new float[] {- 50, SCREEN_HEIGHT + 50, 10, 80},
	};
	
	public Matheor createNewMatheor() {
		nextMatheorParams = round(getParent().random(0, matheorSpawnParams.length - 1));
		float x = matheorSpawnParams[nextMatheorParams][0];
		float y = matheorSpawnParams[nextMatheorParams][1];
		float a = getParent().random(
				matheorSpawnParams[nextMatheorParams][2],
				matheorSpawnParams[nextMatheorParams][3]);
		float v = getParent().random(5, 10);
		return new Matheor(getParent(), this, getParent().random(MATHEAOR_MASS_LOW, MATHEAOR_MASS_HIGH), new Coordinates(x, y), new Vector(a, v), MatheorSize.BIG);		
	}
	
	public void draw() throws Exception {
		//getParent().background(background);
		getParent().imageMode(CORNERS);
		getParent().image(background, 0, 0);
		
		ticker++;
				
		for (Qbject o : qbjects) {
			o.move();
			o.draw();
		}
		qbjects = checkForCollisions();
		
		getParent().textMode(CENTER);

		// Paint scoreboards
		
		getParent().imageMode(CORNERS);
		getParent().image(p1Scoreboard, 10, 10);
		getParent().textFont(scoreFont);
		getParent().text(p1Score, 150, 115);
		
		getParent().textFont(targetNumberFont);
		getParent().text(targetNumber, 57, 68);
		
		if (player1.getGun() == ShotType.ADDITION) {
			getParent().image(additionp1, 206, 106);
		} else {
			getParent().image(subtractionp1, 206, 106);			
		}
		
		if (noOfPlayers == 2) {
			getParent().imageMode(CORNERS);
			getParent().image(p2Scoreboard, SCREEN_WIDTH - 260, 10);
			getParent().text(p2Score, SCREEN_WIDTH - 125, 115);			

			getParent().textFont(targetNumberFont);
			getParent().text(targetNumber, SCREEN_WIDTH - 57, 68);
		
			if (player2.getGun() == ShotType.ADDITION) {
				getParent().image(additionp2, SCREEN_WIDTH - 236, 106);
			} else {
				getParent().image(subtractionp2, SCREEN_WIDTH - 236, 106);			
			}
		}
		
		// Paint timer
		getParent().textFont(timerFont);
		int secRemaining = PApplet.floor(GAME_TIMER_SECONDS - (ticker / FPS) );		
		getParent().text(secRemaining, HALF_WIDTH, 50);
		
		if ((GAME_TIMER_SECONDS * FPS) == ticker) {
			getParent().endCurrentGame();
		}
		/*
		if (ticker % MATHEOR_SPAWN_INTERVAL == 0) {
			addQbject(createNewMatheor());
		}
		*/
		
	}

	@Override
	public void tidyUp() {		
		try {
			for (CanTidyUp t : tidyUps)
				t.tidyUp();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
