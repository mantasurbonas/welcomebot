package lt.visma.ai.welcomebot;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class GreetingsWindow extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private static final String GREETING_IMAGE = "data/images/minion.gif";
	private JLabel lblWelcome;
	
	private PlayerQueue playerQueue = new PlayerQueue();
	private Timer timer;

	private ImageIcon icnFace;

	private JLabel lblConf;

	public GreetingsWindow() {
		setTitle("WELCOME TO VISMA LIETUVA");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		getContentPane().setLayout(new BorderLayout());
		
		lblConf = new JLabel("0.00");
		lblConf.setFont(new Font("Comic Sans", Font.BOLD, 38));
		getContentPane().add(lblConf, BorderLayout.LINE_START);
		
		Icon icnAnimation = new ImageIcon(GREETING_IMAGE);
        JLabel lblAnimation = new JLabel(icnAnimation);
        getContentPane().add(lblAnimation, BorderLayout.CENTER);
        
        icnFace = new ImageIcon();
        JLabel lblFace = new JLabel(icnFace);
        getContentPane().add(lblFace, BorderLayout.LINE_END);
        
		lblWelcome = new JLabel("HELLO DEAR UNKNOWN GUEST");
		lblWelcome.setFont(new Font("Comic Sans", Font.BOLD, 58));
        getContentPane().add(lblWelcome, BorderLayout.PAGE_END);
        
        setSize(1000, 600);
        
		timer = null;
		
	}

	public void greet(String name, List<String> list, BufferedImage capturedImage, double confidentiality){
		lblWelcome.setText("Hello "+name.toUpperCase());
		lblConf.setText(String.format("%5.02f", confidentiality));
		
		this.icnFace.setImage(doubleImage(capturedImage));
		
		if (timer != null)
			timer.cancel();
		
		timer = new Timer();
		timer.schedule(new TimerTask(){
			public void run() {
				setVisible(false);
				timer = null;
			}
		}, 15000);
		
		setVisible(true);
		
		if (list !=null && !list.isEmpty())
			playerQueue.addToQueue(name, list);		

	}
	
	public void ungreet(){
		setVisible(false);
	}
	
	private BufferedImage doubleImage(BufferedImage srcImg){
		int w = srcImg.getWidth() *2;
		int h = srcImg.getHeight()*2;
		
	    BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
	    
	    Graphics2D g2 = resizedImg.createGraphics();
	    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
	    g2.drawImage(srcImg, 0, 0, w, h, null);
	    g2.dispose();

	    return resizedImg;
	}
}
