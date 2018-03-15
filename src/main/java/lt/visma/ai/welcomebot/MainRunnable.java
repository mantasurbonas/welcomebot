package lt.visma.ai.welcomebot;

import static org.bytedeco.javacpp.helper.opencv_objdetect.cvHaarDetectObjects;
import static org.bytedeco.javacpp.opencv_core.CV_32SC1;
import static org.bytedeco.javacpp.opencv_core.IPL_DEPTH_8U;
import static org.bytedeco.javacpp.opencv_core.cvClearMemStorage;
import static org.bytedeco.javacpp.opencv_core.cvCopy;
import static org.bytedeco.javacpp.opencv_core.cvCreateImage;
import static org.bytedeco.javacpp.opencv_core.cvGetSeqElem;
import static org.bytedeco.javacpp.opencv_core.cvGetSize;
import static org.bytedeco.javacpp.opencv_core.cvLoad;
import static org.bytedeco.javacpp.opencv_core.cvPoint;
import static org.bytedeco.javacpp.opencv_core.cvResetImageROI;
import static org.bytedeco.javacpp.opencv_core.cvSetImageROI;
import static org.bytedeco.javacpp.opencv_imgcodecs.CV_LOAD_IMAGE_GRAYSCALE;
import static org.bytedeco.javacpp.opencv_imgcodecs.imread;
import static org.bytedeco.javacpp.opencv_imgproc.CV_AA;
import static org.bytedeco.javacpp.opencv_imgproc.CV_FONT_HERSHEY_SIMPLEX;
import static org.bytedeco.javacpp.opencv_imgproc.CV_INTER_AREA;
import static org.bytedeco.javacpp.opencv_imgproc.cvInitFont;
import static org.bytedeco.javacpp.opencv_imgproc.cvRectangle;
import static org.bytedeco.javacpp.opencv_imgproc.cvResize;
import static org.bytedeco.javacpp.opencv_objdetect.CV_HAAR_DO_CANNY_PRUNING;

import java.nio.IntBuffer;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.opencv_core.CvMemStorage;
import org.bytedeco.javacpp.opencv_core.CvPoint;
import org.bytedeco.javacpp.opencv_core.CvRect;
import org.bytedeco.javacpp.opencv_core.CvScalar;
import org.bytedeco.javacpp.opencv_core.CvSeq;
import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacpp.opencv_core.Mat;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_face.FaceRecognizer;
import org.bytedeco.javacpp.opencv_face.LBPHFaceRecognizer;
import org.bytedeco.javacpp.opencv_imgproc.CvFont;
import org.bytedeco.javacpp.opencv_objdetect.CvHaarClassifierCascade;
import org.bytedeco.javacv.CanvasFrame;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameGrabber.ImageMode;
import org.bytedeco.javacv.OpenCVFrameConverter;

public class MainRunnable implements Runnable{

    private static final int CAMERA_NUMBER = 0;

	public static void main(String[] args) {
    	MainRunnable cot = new MainRunnable();
        Thread th = new Thread(cot);
        th.start();
    }
	
    CanvasFrame canvas;
    CvHaarClassifierCascade classifier;
    FaceRecognizer faceRecognizer;
	MatVector images;
	Mat labels;

	FacesRepository facesRepo;
	private CvFont font;
	
    public MainRunnable(){
    	canvas = new CanvasFrame("Web Cam Live");
    	canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
    	
    	classifier = new CvHaarClassifierCascade(cvLoad("E:/deleteme/faces/haarcascade_frontalface_default.xml"));
        if (classifier.isNull())
        	throw new RuntimeException("classifier not loaded");    
        
        facesRepo = new FacesRepository("e:/deleteme/faces/training");
        loadTrainingData();
        
        faceRecognizer = LBPHFaceRecognizer.create();        
        faceRecognizer.train(images, labels);
    }
    
	private void loadTrainingData(){
		
        this.images = new MatVector(facesRepo.getImageCount());
        this.labels = new Mat(facesRepo.getImageCount(), 1, CV_32SC1);
        
        IntBuffer labelsBuf = labels.createBuffer();

        int counter = 0;

        for (String name: facesRepo.getNames()) {
        	for (String imageFile: facesRepo.getImageFiles(name)){
        	    images.put(counter, 	imread(imageFile, CV_LOAD_IMAGE_GRAYSCALE));
        	    
				labelsBuf.put(counter, 	facesRepo.getId(name));
	            
	            System.out.println("file "+name+" "+ imageFile+" read ok");
	            
	            counter++;
        	}
        }
	}
    
	@Override
	public void run() {
		try{
			FrameGrabber grabber = FrameGrabber.createDefault(CAMERA_NUMBER);
			grabber.setImageMode(ImageMode.GRAY);
			grabber.setBitsPerPixel(org.bytedeco.javacpp.opencv_core.CV_8U);
			
	        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
	        grabber.start();
	        
	        CvMemStorage storage = CvMemStorage.create();
	        
	        font = new CvFont();
			cvInitFont(font, CV_FONT_HERSHEY_SIMPLEX, 0.4d, 0.4d);
			
			while (true) {
				Frame grabbed = grabber.grab();
				
				IplImage img = converter.convert(grabbed);
                if (img == null) 
                	continue;
                
                try(CvSeq faces = detectFaces(img, storage)){

	                int faces_num = faces.total();
	                for(int i = 0; i < faces_num; i++)
	                    drawFace(img, cvGetSeqElem(faces, i));
                }                
                
                Frame toDisplay = converter.convert(img);
				canvas.showImage(toDisplay);
                
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	private CvSeq detectFaces(IplImage img, CvMemStorage storage) {
		try(IplImage smallImage = resizeImage(img)){
	
	        cvClearMemStorage(storage);
	        return cvHaarDetectObjects(smallImage, classifier, storage, 1.1, 3, CV_HAAR_DO_CANNY_PRUNING);
		}
	}

	private void drawFace(IplImage iplImg, BytePointer face) {
		try(CvRect r = new CvRect(face)){
			
			r.x(r.x() * 2);
			r.y(r.y() * 2);
			r.height(r.height() * 2);
			r.width(r.width() * 2);
			
			CvPoint topLeftPoint = cvPoint(r.x(), r.y());
			cvRectangle (
			        iplImg,
			        topLeftPoint,
			        cvPoint(r.width() + r.x(), r.height() + r.y()),
			        CvScalar.RED,
			        2,
			        CV_AA,
			        0);
	        
			IplImage cropped = cropImage(iplImg, r);
			cropped.release();
			
			//	int id = recognize(cropped);
		        
			//	if (id < 0)
			//		return;
				
		     //   String text = String.format("%5.02f", confPtr.get()) 
			//	        					+" "+ id
			//	        					+" "+facesRepo.getName(id);
		        	        
			//	cvPutText(iplImg, text, topLeftPoint, font, CvScalar.RED);
			//}
		}
	}

	IntPointer    labelPtr = null;
    DoublePointer confPtr  = null;
    
	private int recognize(IplImage cropped) {
		try(Mat mat = new Mat(cropped)){
		
			if (labelPtr == null)
				labelPtr = new IntPointer(1);
			
			if (confPtr == null)
				confPtr = new DoublePointer(1);
			
			faceRecognizer.predict(mat, labelPtr, confPtr);
			
			int predictedLabel = labelPtr.get(0);
			
	        if (confPtr.get() < 20d)
	        	return -1;
	        
			return predictedLabel;
		}
	}

	private IplImage resizeImage(IplImage img) {
		IplImage smallImage = IplImage.create(img.width()/2, img.height()/2, IPL_DEPTH_8U, 1);
	    cvResize(img, smallImage, CV_INTER_AREA);
		return smallImage;
	}
	
	private static IplImage cropImage(IplImage iplImg, CvRect r) {
		cvSetImageROI(iplImg, r);
		
		IplImage cropped = cvCreateImage(cvGetSize(iplImg), iplImg.depth(), iplImg.nChannels());
		cvCopy(iplImg, cropped);
		cvResetImageROI(iplImg);
		return cropped;
	}
	
}
