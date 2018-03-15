package lt.visma.ai.welcomebot;

import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.IntBuffer;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_face.LBPHFaceRecognizer;
import org.openimaj.image.FImage;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.HaarCascadeDetector;
import org.openimaj.math.geometry.shape.Rectangle;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

/**
 */
public class MainFrame extends JFrame implements Runnable, WebcamPanel.Painter {

	private static final String DETECTOR_CONFIG = "data/haarcascade_frontalface_default.xml";

	private static final String TRAINING_IMAGES_LOCATION = "data/training";

	private static final String LOGITECH_WEBCAM = "Logitech HD Pro Webcam C920 1";

	private static final long serialVersionUID = 1L;

	private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();
	private static final HaarCascadeDetector detector = new HaarCascadeDetector();
	
	private static final Stroke STROKE = new BasicStroke(1.0f, 
														BasicStroke.CAP_BUTT, 
														BasicStroke.JOIN_MITER, 
														1.0f, 
														new float[] { 1.0f }, 
														0.0f);

	private Webcam webcam = null;
	private WebcamPanel.Painter painter = null;
	private List<DetectedFace> faces = null;

	private FaceRecognizer faceRecognizer;

	private MatVector images;

	private Mat labels;
	
	private FacesRepository facesRepo = new FacesRepository(TRAINING_IMAGES_LOCATION);
	private FaceTracker faceTracker;
	private GreetingsWindow greetingsWindow;

	private BufferedImage capturedImage;

	private boolean recognize = true;

	private boolean saveCPU = true;

	private JComboBox<String> cmbNameList;

	private JTextField txtNewName;

	JButton btnRecord = new JButton("record");
	
	public MainFrame(boolean trainingMode) throws Exception {

		super();

		this.saveCPU = !trainingMode;

		faceRecognizer = LBPHFaceRecognizer.create();
		
		System.out.println("loading training images");
		
		loadTrainingData();
		
        System.out.println("face training started");
        
        faceRecognizer.train(images, labels);
        
        System.out.println("face training done");
        
        detector.setCascade(DETECTOR_CONFIG);
        
        System.out.println("face detection configured");
        
        faceTracker = new FaceTracker();
		
        for (Webcam w: Webcam.getWebcams())
        	System.out.println("["+w.getName() + "] ");
        
        try{
        	webcam = Webcam.getWebcamByName(LOGITECH_WEBCAM);
        	//webcam = Webcam.getDefault();
        }catch(Exception e){
        	webcam = null;
        }
        
        if (webcam == null){
        	System.err.println("failed connecting to HD webcam, using default");
        	webcam = Webcam.getDefault();
        }
        
		webcam.setViewSize(WebcamResolution.VGA.getSize());
		webcam.open(true);

		WebcamPanel webcamPanel = new WebcamPanel(webcam, false);
		webcamPanel.setPreferredSize(WebcamResolution.VGA.getSize());
		webcamPanel.setPainter(this);
		webcamPanel.setFPSDisplayed(true);
		webcamPanel.setFPSLimited(true);
		webcamPanel.setFPSLimit(20);
		webcamPanel.setPainter(this);
		webcamPanel.start();

		painter = webcamPanel.getDefaultPainter();

		
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

		JPanel textPanel = new JPanel();
		
		cmbNameList = new JComboBox<String>(facesRepo.getNames().toArray(new String[]{}));
		cmbNameList.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				onNameSelected();
			}});
		textPanel.add(cmbNameList);

		txtNewName = new JTextField();
		txtNewName.setColumns(20);
		textPanel.add(txtNewName);
		txtNewName.setVisible(true);
		textPanel.add(txtNewName);

		JButton btnTrain = new JButton("train");
		btnTrain.addActionListener(e -> onTrain());
		textPanel.add(btnTrain);
		
		btnRecord.addActionListener(e -> onRecord());
		textPanel.add(btnRecord);

		mainPanel.add(webcamPanel);
		mainPanel.add(textPanel);
		
		add(mainPanel);
		

		setTitle("Welcome Bot");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);

		greetingsWindow = new GreetingsWindow();
		
		EXECUTOR.execute(this);
	}

	private void onNameSelected(){
		String selectedName = cmbNameList.getSelectedItem().toString();
		boolean newPerson = (selectedName.startsWith("<"));
		txtNewName.setVisible(newPerson);			
	}
	
	private void onTrain(){
		if (capturedImage == null)
			return;
		
		String name = getNameToAddTrainingTo();
		
		if (name == null || name.trim().isEmpty())
			return;
		
		synchronized(this){
			recognize = false;
			
			try {
				facesRepo.save(capturedImage, name);
			} finally{
				recognize = true;
			}
			
		}
	}

	private void onRecord() {
		String name = getNameToAddTrainingTo();
		if (name == null)
			return;
		
		Integer id = facesRepo.getId(name);

		Runnable runnable = () -> new Recorder(TRAINING_IMAGES_LOCATION+"/"+id+". "+name+"/hello.wav", btnRecord);
		new Thread(runnable).start();
	}
	
	private String getNameToAddTrainingTo() {
		String name = cmbNameList.getSelectedItem().toString().trim();
		
		if (name.startsWith("<")){
			if (! txtNewName.isVisible())
				return null; 
			
			name = txtNewName.getText().trim();
		}
		
		if (name.isEmpty())
			return null;
		
		return name;
	}

	private boolean hasFaces(){
		return faces != null && !faces.isEmpty();
	}
	
	@Override
	public void run() {
		while (true) {
			if (!webcam.isOpen()) 
				return;
			
			faces = detector.detectFaces(ImageUtilities.createFImage(webcam.getImage()));
			
			if (!hasFaces())
				try { Thread.sleep(100); } catch (Exception e) { throw new RuntimeException(e); } 
		}
	}

	@Override
	public void paintPanel(WebcamPanel panel, Graphics2D g2) {
		if (painter != null && hasFaces()) {
			painter.paintPanel(panel, g2);
			return;
		}
		
		try { Thread.sleep(100); } catch (Exception e) { throw new RuntimeException(e); }
	}
		
	@Override
	public void paintImage(WebcamPanel panel, BufferedImage image, Graphics2D g2) {

		if (saveCPU && (painter == null || !hasFaces()))
			return;
		
		painter.paintImage(panel, image, g2);

		for(DetectedFace face : faces) {			
			if (!recognize)
				return;
			
			Rectangle bounds = face.getBounds();

			int dx = (int) (0.1 * bounds.width);
			int dy = (int) (0.2 * bounds.height);
			int x = (int) bounds.x - dx;
			int y = (int) bounds.y - dy;
			int w = (int) bounds.width + 2 * dx;
			int h = (int) bounds.height + dy;

			float confidence = face.getConfidence();
			if (confidence < 4)
				continue;

			g2.setStroke(STROKE);
			g2.setColor(Color.RED);
			g2.drawRect(x, y, w, h);
			
			
			FImage facePatch = face.getFacePatch();
			synchronized(this){
				capturedImage = ImageUtilities.createBufferedImage(facePatch);
			}
			
			byte[] data = ((DataBufferByte) capturedImage.getRaster().getDataBuffer()).getData();
			
			try(Mat faceMat1 = new Mat(data, false)){
				Mat faceMat2 = faceMat1.reshape(capturedImage.getColorModel().getNumComponents(), capturedImage.getHeight());
			
		        IntPointer label = new IntPointer(1);
		        DoublePointer conf = new DoublePointer(1);
		        
		        faceRecognizer.predict(faceMat2, label, conf);
		        
		        int predictedLabel = label.get(0);
			        
	        	String name = facesRepo.getName(predictedLabel);
	        	if (name == null)
	        		continue;
	        	
		        if (conf.get() < 20d){
		        	System.out.println("confidence < 20 :  "+conf.get());
		        	continue;
		        }
		        
		        if (conf.get()>100d){
		        	System.out.println("confidence > 100 : "+conf.get());
		        	continue;
		        }
	        	
	        	boolean recognizedForSure = faceTracker.faceDetected(name, x, y, w, h);
	        	
				g2.drawString(String.format("%5.02f", conf.get())
    							+" " +name
						        +(recognizedForSure?" GREETINGS":"..."),
	        				  x, y);

				if (recognizedForSure && !facesRepo.isGreeted(name)){
					greetingsWindow.greet(name, facesRepo.getAudioFiles(name), capturedImage, conf.get());
					facesRepo.setGreeted(name);
				}
			}
			
		}
	}

	private void loadTrainingData(){

        this.images = new MatVector(facesRepo.getImageCount());
        this.labels = new Mat(facesRepo.getImageCount(), 1, CV_32SC1);
        
        IntBuffer labelsBuf = labels.createBuffer();

        int counter = 0;

        for (String name: facesRepo.getNames()) {
        	if (name.startsWith("<"))
        		continue;
        	
        	for (String imageFile: facesRepo.getImageFiles(name)){
        	    images.put(counter, 	imread(imageFile, CV_LOAD_IMAGE_GRAYSCALE));
        	    
				labelsBuf.put(counter, 	facesRepo.getId(name));
	            
	            System.out.println("file "+name+" "+ imageFile+" read ok");
	            
	            counter++;
        	}
        }
	}
}
