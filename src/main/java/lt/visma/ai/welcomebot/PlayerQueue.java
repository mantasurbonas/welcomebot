package lt.visma.ai.welcomebot;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.util.*;

public class PlayerQueue {

    private Player player = new Player();

    private final Map<String, List<String>> queue = Collections.synchronizedMap(new LinkedHashMap<String, List<String>>());


    PlayerQueue() {
        Runnable runable = this::startListener;
        new Thread(runable).start();
    }

    public void addToQueue(String name, List<String> audioFiles) {
		queue.putIfAbsent(name, audioFiles);
    }


    private void startListener() {

        while (true) {
            Map.Entry<String, List<String>> nextEntry = null;

            {
                synchronized (queue) {
                    Iterator<Map.Entry<String, List<String>>> i = queue.entrySet().iterator();
                    if (i.hasNext()) {
                        nextEntry = i.next();
                        i.remove();
                    }
                }

                if (nextEntry != null) {
                    try {
                        for (String audios : nextEntry.getValue()) {
                            player.play(audios);
                        }
                    } catch (IOException | UnsupportedAudioFileException | LineUnavailableException e) {
                        e.printStackTrace();
                    }
                }

                //TODO: ugly hack to clear up the polluted queue for the same name
                synchronized (queue) {
                    if (nextEntry != null) {
                        queue.remove(nextEntry.getKey());
                    }
                }
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}


