package nit.matheors;

import static processing.core.PApplet.abs;
import static processing.core.PApplet.sqrt;
import static processing.core.PApplet.pow;

public abstract class Utils {

	public static float calcDistance(Coordinates c1, Coordinates c2) {
		float distX = abs(c1.getX() - c2.getX());
		float distY = abs(c1.getY() - c2.getY());
		return sqrt(pow(distX, 2) + pow(distY, 2));		
	}
	
	public static float calculateMomentum(float velocity, float massKg) {
		return (velocity * massKg);
	}

}
