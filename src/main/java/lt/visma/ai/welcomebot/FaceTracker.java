package lt.visma.ai.welcomebot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FaceTracker {

	private static final long FORGET_FACE_TIMEOUT = 700;

	private static final long RECOGNIZED_FOR_SURE_TIMEOUT = 1500;
	
	Map<String, List<Spotting>> spottingHistory = new HashMap<>();
	
	private static class Spotting{
		int x;
		int y;
		int w;
		int h;
		
		long firstSpotted;
		long lastSpotted;
		
		boolean recognized = false;
		
		public Spotting(int x, int y, int w, int h){
			firstSpotted = lastSpotted = System.currentTimeMillis();
			
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}

		public boolean intersects(int x2, int y2, int w2, int h2) {
		    // If one rectangle is on left side of other
		    if (this.x > (x2+w2) || (x2) > (this.x+this.w) )
		        return false;
		 
		    // If one rectangle is below other
		    if (this.y > (y2+h2) || (y2) > (this.y+this.h))
		        return false;
		 
		    return true;
		}

		public long getDuration() {
			return this.lastSpotted - this.firstSpotted;
		}
	}
	
	boolean faceDetected(String name, int x, int y, int w, int h){
		long current = System.currentTimeMillis();
		
		Spotting spotting = getSpotting(name, x, y, w, h);
		
		long lastTimeSpotted = current - spotting.lastSpotted;
		
		if (lastTimeSpotted > FORGET_FACE_TIMEOUT){
			System.out.println("last time "+name
									+" was spotted was too long ago: "+lastTimeSpotted
									+ " (it was around for " +spotting.getDuration()+") "
									+" " + (spotting.recognized?"SURE":"NOISE"));
			
			removeSpotting(name, spotting);
			addSpotting(name, x, y, w, h);
			return false;
		}
		
		spotting.lastSpotted = current;
		spotting.x = x;
		spotting.y = y;
		spotting.w = w;
		spotting.h = h;
		
		boolean recognizedForSure = (current - spotting.firstSpotted) >= RECOGNIZED_FOR_SURE_TIMEOUT;
		
		spotting.recognized = recognizedForSure;
		
		return recognizedForSure;
	}

	private Spotting getSpotting(String name, int x, int y, int w, int h) {
		List<Spotting> spottings = spottingHistory.get(name);
		if (spottings == null)
			return addSpotting(name, x, y, w, h);

		long current = System.currentTimeMillis();
		
		Iterator<Spotting> it = spottings.iterator();
		while(it.hasNext()){
			Spotting spotting = it.next();
			if (spotting.intersects(x, y, w, h))
				return spotting;
			
			long lastTimeSpotted = current - spotting.lastSpotted;
			
			if (lastTimeSpotted > FORGET_FACE_TIMEOUT){
				System.out.println("forgetting "+name
									+ " who was last seen too long ago: "+lastTimeSpotted
									+ " (it was around for " +spotting.getDuration()+") "
									+ " " + (spotting.recognized?"SURE":"NOISE"));
				it.remove();
			}
		}
		
		return addSpotting(name, x, y, w, h);
	}

	private Spotting addSpotting(String name, int x, int y, int w, int h) {
		Spotting s = new Spotting(x, y, w, h);
		
		List<Spotting> spottings = spottingHistory.get(name);
		if (spottings == null){
			spottings = new ArrayList<>();
			spottingHistory.put(name, spottings);
		}
		
		spottings.add(s);
		
		return s;
	}

	private void removeSpotting(String name, Spotting spotting) {
		List<Spotting> spottings = spottingHistory.get(name);
		
		for (Spotting s: spottings)
			if (s.equals(spotting)){
				spottings.remove(s);
				return;
			}
	}

}
