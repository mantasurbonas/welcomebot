package lt.visma.ai.welcomebot;

import java.io.File;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.JButton;

public class Recorder {
    TargetDataLine line;
    AudioFormat format = new AudioFormat(16000, 8, 2, true, true);
    JButton jbutton;

    public Recorder(String filename, JButton jbutton) {
        Thread stopper = new Thread(() -> {
            jbutton.setText("Recording");
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            line.stop();
            line.close();
            jbutton.setText("Record");

        });

        stopper.start();
        write(filename);
    }

    private void write(String filename) {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            line = (TargetDataLine) AudioSystem.getLine(info);
            line.open(format);
            line.start();
            AudioInputStream ais = new AudioInputStream(line);
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, new File(filename));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        new Recorder(null, null);
    }

}
