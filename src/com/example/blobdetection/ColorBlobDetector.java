package com.example.blobdetection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import android.util.Log;

public class ColorBlobDetector {

   
    
    private List<Scalar> hsvValues = new LinkedList<Scalar>();
    
    
    private ArrayList<RotatedRect> minRectangles = new ArrayList<RotatedRect>();
    private List<Block> detectedBlocks = new LinkedList<Block>();
    
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25,50,50);
    private Mat mSpectrum = new Mat();

    // Cache
    Mat mPyrDownMat = new Mat();
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mAccumulatedMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();

    private List<DetectionData> detectionData =  new CopyOnWriteArrayList<DetectionData>();
    
	private boolean keepBinary = false;

	private Mat dilatedMaskCopy;

	private boolean onlyLowerBoundMode = true;

	//FIXME make it configurable
	private double image_downsample_factor = 4;

	//FIXME make it configurable
	private double MIN_AREA = 100;

	private double minSaturation = 80;
	private double minBright = 40;
	
    ArrayList<MatOfPoint> approxContoursList = new ArrayList<MatOfPoint>();


	private boolean polyApproxMode = true;

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    } 
    
    private double calculateMinH(double h, double radius){
    	return (h >= radius) ? h -radius : 0;
    }
    
    private double calculateMaxH(double h, double radius){
    	return (h+radius <= 255) ? h+radius : 255;
    }
    
    public void loadCETAPresetColors(){
    	this.hsvValues.clear();
    	this.detectionData.clear();
    	
//    	Scalar hsvRedColor = new Scalar(9,150,100);
//    	this.hsvValues.add(hsvRedColor);
//    	DetectionData redDetectionData = new DetectionData(BlockColors.RED, hsvRedColor, computeUpperBounds(hsvRedColor, mColorRadius, onlyLowerBoundMode), 
//    										computeLowerBounds(hsvRedColor, mColorRadius, onlyLowerBoundMode));
//    	
//    	Scalar hsvBlueColor = new Scalar(167,150,100);
//    	this.hsvValues.add(hsvBlueColor);
//    	DetectionData blueDetectionData = new DetectionData(BlockColors.BLUE, hsvBlueColor,computeUpperBounds(hsvBlueColor, mColorRadius, onlyLowerBoundMode), 
//											computeLowerBounds(hsvBlueColor, mColorRadius, onlyLowerBoundMode));
//    	
//    	Scalar hsvGreenColor = new Scalar(85,150,100);   	
//    	this.hsvValues.add(hsvGreenColor);
//    	DetectionData greenDetectionData = new DetectionData(BlockColors.GREEN, hsvGreenColor, computeUpperBounds(hsvGreenColor, mColorRadius, onlyLowerBoundMode), 
//											computeLowerBounds(hsvGreenColor, mColorRadius, onlyLowerBoundMode));
//    	
//    	this.detectionData.add(redDetectionData);
//    	this.detectionData.add(blueDetectionData);
//    	this.detectionData.add(greenDetectionData);
    	    
    	
    	
    	
    	
    	Scalar hsvRedColor = new Scalar(9,150,100);
    	this.hsvValues.add(hsvRedColor);
    	Scalar redLower = new Scalar(0.5,minSaturation,minBright);
    	Scalar redUpper = new Scalar(10,255,255);
    	DetectionData redDetectionData = new DetectionData(BlockColors.RED, hsvRedColor,redUpper,redLower);
    	
    	Scalar hsvBlueColor = new Scalar(167,150,100);
    	this.hsvValues.add(hsvBlueColor);
    	Scalar blueLower = new Scalar(157,minSaturation,minBright);
    	Scalar blueUpper = new Scalar(173,255,255);
    	DetectionData blueDetectionData = new DetectionData(BlockColors.BLUE, hsvBlueColor,blueUpper, blueLower);
    	
    	Scalar hsvGreenColor = new Scalar(84,150,100);   	
    	Scalar greenLower = new Scalar(84,minSaturation,minBright);
    	Scalar greenUpper = new Scalar(99,255,255);
    	this.hsvValues.add(hsvGreenColor);
    	DetectionData greenDetectionData = new DetectionData(BlockColors.GREEN, hsvGreenColor, greenUpper, greenLower);

    	Scalar orange = new Scalar(0,0,0);
    	Scalar lower = new Scalar(12,minSaturation,minBright);
    	Scalar upper = new Scalar(25,255,255);
    	this.hsvValues.add(orange);
    	DetectionData orangeDetectionData = new DetectionData(BlockColors.ORANGE, orange, upper, lower);
    	
    	Scalar pinky = new Scalar(240,195,86);
    	Scalar lowerPink = new Scalar(235,minSaturation,minBright);
    	Scalar upperPink = new Scalar(250,255,255);
    	this.hsvValues.add(orange);
    	DetectionData pinkDetectionData = new DetectionData(BlockColors.PINK, pinky, upperPink, lowerPink);
    	
    	this.detectionData.add(orangeDetectionData);
    	this.detectionData.add(redDetectionData);
    	this.detectionData.add(blueDetectionData);
    	this.detectionData.add(greenDetectionData);
    	this.detectionData.add(pinkDetectionData);
    	
    }
    
    public void addHsvColor(Scalar hsvColor){
        
        this.hsvValues.add(hsvColor);      
        
        Scalar upper = computeUpperBounds(hsvColor, mColorRadius, onlyLowerBoundMode);
        Scalar lower = computeLowerBounds(hsvColor, mColorRadius, onlyLowerBoundMode);
        computeSpectrumHsv(upper, lower);
        
        this.detectionData.add(new DetectionData(BlockColors.UNKNOWN, hsvColor, upper, lower));
    }
    
    
    private Scalar computeUpperBounds(Scalar hsvColor, Scalar colorRadio, boolean onlyLowerBoundSV){
    	Scalar upper = new Scalar(0);
        double maxH = calculateMaxH(hsvColor.val[0], colorRadio.val[0]);
        upper.val[0] = maxH;
    	if(onlyLowerBoundSV ){
            upper.val[1] = 255;
            upper.val[2] = 255;
        }else{
        	upper.val[1] = hsvColor.val[1] + colorRadio.val[1];
        	upper.val[2] = hsvColor.val[2] + colorRadio.val[2];
        }
    	return upper;
    }
    
    private Scalar computeLowerBounds(Scalar hsvColor, Scalar colorRadio,boolean onlyLowerBoundSV){
    	Scalar lower = new Scalar(0);
    	double minH = calculateMinH(hsvColor.val[0], colorRadio.val[0]);

    	lower.val[0] = minH;
       	lower.val[1] = hsvColor.val[1] - colorRadio.val[1];
        lower.val[2] = hsvColor.val[2] - colorRadio.val[2];

    	return lower;
    }
    
	private void computeSpectrumHsv(Scalar upper, Scalar lower) {
		Mat spectrumHsv = new Mat(1, (int)(upper.val[0]-lower.val[0]), CvType.CV_8UC3);
        for (int j = 0; j < upper.val[0]-lower.val[0]; j++) {
            byte[] tmp = {(byte)(lower.val[0]+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }
        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4); //Four channels because this image will be combined with the capture frame
	}

    public Mat getSpectrum() {
        return mSpectrum;
    }

    public void setMinContourArea(double area) {
        mMinContourArea = area;
    }

    public void process(Mat rgbaImage) {
    	//se reduce el tamaño a la 4ta parte. Si falta precision se puede hacer solo a la mitad.
        int iterations = (int)image_downsample_factor/2;
    	for(int i=0;i<iterations;i++){
    		if(i==0){
    			Imgproc.pyrDown(rgbaImage, mPyrDownMat);
    		}else{
    			Imgproc.pyrDown(mPyrDownMat, mPyrDownMat);
    		}
    	}
        
        Imgproc.cvtColor(mPyrDownMat, mHsvMat, Imgproc.COLOR_RGB2HSV_FULL);

        doImageSegmentationByColor();
        this.approxContoursList.clear();
        for(Iterator<DetectionData> iter = detectionData.iterator();iter.hasNext();){
        	ArrayList<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        	DetectionData data =  iter.next();
        	Imgproc.findContours(data.getMask(), contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        	
        	
        	//FIXME este max area debería considerar todas las areas, no solo la del data actual
        	// Find max contour area
            double maxArea = 0;
            Iterator<MatOfPoint> each = contours.iterator();
            while (each.hasNext()) {
                MatOfPoint wrapper = each.next();
                double area = Imgproc.contourArea(wrapper);
                if(area > MIN_AREA ){
	              //Polygon approximation
	                aproxPoly(wrapper);
	                
	                if (area > maxArea){
	                    maxArea = area;
	                }
                }
            }

            // Filter contours by area and resize to fit the original image size
            ArrayList<MatOfPoint> contoursList = new ArrayList<MatOfPoint>();
            each = contours.iterator();
            while (each.hasNext()) {
                MatOfPoint contour = each.next();
                double area  = Math.abs(Imgproc.contourArea(contour));
                if (area>MIN_AREA && area > mMinContourArea*maxArea) {
                	if(polyApproxMode ){
                		MatOfPoint2f approx = aproxPoly(contour);
                		MatOfPoint points = new MatOfPoint(approx.toArray());
            	        Log.e("detector", "#points=" + points.total());
            	        //FIXME core.multiply mover adentro del if cuando saque el debagguing de approxcountourlist
            	        Core.multiply(contour, new Scalar(image_downsample_factor,image_downsample_factor ), contour);
                        approxContoursList.add(contour);
                        if (points.total() == 4 && Math.abs(Imgproc.contourArea(points)) > MIN_AREA && Imgproc.isContourConvex(points)) {
                            //TODO También se pueden chequar los cosenos de los angulos para ver si son rectos. 
                        	//ver squares.cpp o http://www.codesend.com/view/28f1b97e61a435f30765c0a1ba32d2a0/
                        	contoursList.add(contour);
                        }
                	}else{
                		Core.multiply(contour, new Scalar(image_downsample_factor,image_downsample_factor ), contour);
                		contoursList.add(contour);
                	}
                }
            }        	
        	data.setContours((List<MatOfPoint>)contoursList.clone());
        }
        
        //Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        
    }

    private Mat cleanBinaryImage(Mat binaryImage){
    	  Size size = new Size(5,5);
    	  //morphological opening (remove small objects from the foreground)
    	  Imgproc.erode(binaryImage, binaryImage, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, size));
    	  Imgproc.dilate( binaryImage, binaryImage, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, size)); 

    	  //morphological closing (fill small holes in the foreground)
    	  Imgproc.dilate( binaryImage, binaryImage, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, size)); 
    	  Imgproc.erode(binaryImage, binaryImage, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, size));
    	  return binaryImage;
    }
    
    private void doImageSegmentationByColor(){
    	mAccumulatedMask = Mat.zeros(mHsvMat.size(), CvType.CV_8UC1);
    	for(Iterator<DetectionData> iter = this.detectionData.iterator();iter.hasNext();){
    		DetectionData data = iter.next();
    		Core.inRange(mHsvMat, data.getHsvLower(), data.getHsvUpper(), mMask);
    		Core.add(mAccumulatedMask, mMask, mAccumulatedMask);
    	
    		
    		mMask = cleanBinaryImage(mMask);
    		data.setMask(mMask.clone());
//    		Imgproc.dilate(mMask, mDilatedMask, new Mat());
//    		data.setMask(mDilatedMask.clone());
    	}
    	
//    	Imgproc.dilate(mAccumulatedMask, mDilatedMask, new Mat());
    	
        if(keepBinary){
//        	this.dilatedMaskCopy = mDilatedMask.clone();
        	this.dilatedMaskCopy = cleanBinaryImage(mAccumulatedMask);
        }
    }
    
   

	public void setPolyApproxMode(boolean polyApproxMode) {
		this.polyApproxMode = polyApproxMode;
	}

	public void setOnlyLowerBoundMode(boolean onlyLowerBoundMode) {
		this.onlyLowerBoundMode = onlyLowerBoundMode;	
		for(Iterator<DetectionData> iter = this.detectionData.iterator();iter.hasNext();){
			DetectionData data = iter.next();
			Scalar upper = data.getHsvUpper();
			if(onlyLowerBoundMode){
				upper.val[1] = 255;
				upper.val[2] = 255;
			}else{
				upper.val[1] = data.getHsvValues().val[1] + mColorRadius.val[1];
				upper.val[2] = data.getHsvValues().val[2] + mColorRadius.val[2];
			}
		}
	}


    
	public void computeMinRectangles(){
		for(Iterator<DetectionData> iter = detectionData.iterator();iter.hasNext();){
			DetectionData data = iter.next();
			this.minRectangles.clear();
			for(Iterator<MatOfPoint> iterContours = data.getContours().iterator();iterContours.hasNext();){
				MatOfPoint2f mat = new MatOfPoint2f(iterContours.next().toArray());
				this.minRectangles.add(Imgproc.minAreaRect(mat));
			}
			data.setRotatedRects((List<RotatedRect>)this.minRectangles.clone());
		}
	}
	
	public List<Block> detectBlocks(Mat rgbaImage){
		this.process(rgbaImage);
		this.computeMinRectangles();
		
		for(Iterator<DetectionData> iter = detectionData.iterator();iter.hasNext();){
			DetectionData data = iter.next();
			List<Block> detectedByColor = new ArrayList<Block>();
			for(Iterator<RotatedRect> iterRect = data.getRotatedRects().iterator();iter.hasNext();){
	    		RotatedRect rect = iterRect.next();
	    		Point[] vertices = new Point[4];  
	            rect.points(vertices);  
	            Block block = new Block(rect.center,rect.size,vertices);
	            this.detectedBlocks.add(block);  //here we accumulate all blocks
	            detectedByColor.add(block);		 //just the blocks of one color
	    	}
			data.setBlocks(detectedByColor);
		}
    	return this.detectedBlocks;
	}
	
	public List<RotatedRect> getMinRectangles(){
		List<RotatedRect> allContours = new LinkedList<RotatedRect>();
    	for(Iterator<DetectionData> iter = detectionData.iterator();iter.hasNext();){
    		DetectionData data = iter.next();
    		allContours.addAll(data.getRotatedRects());
    	}
        return allContours;

	}
	
	public void computeMoments(){
		//TODO
	}
    
	public MatOfPoint2f aproxPoly(MatOfPoint contour){
	    MatOfPoint2f contour2f = new MatOfPoint2f();
	    MatOfPoint2f approxPoly = new MatOfPoint2f();
	    
		contour.convertTo(contour2f, CvType.CV_32FC2);
		double contourLength = Imgproc.arcLength(contour2f, true);
    	Imgproc.approxPolyDP(contour2f, approxPoly, contourLength*0.06, true);
    	return approxPoly;
    }
	
	
    public List<MatOfPoint> getContours() {
    	List<MatOfPoint> allContours = new LinkedList<MatOfPoint>();
    	for(Iterator<DetectionData> iter = detectionData.iterator();iter.hasNext();){
    		DetectionData data = iter.next();
    		allContours.addAll(data.getContours());
    	}
        return allContours;
    }
    
    
    
    
    /*------------- getters and setters*/
    public Mat getMask(){
    	return this.mMask;
    }
    
    public Mat getDilatedBinaryMask(){
    	return this.dilatedMaskCopy;
    }
    
    
    public boolean isKeepBinary() {
		return keepBinary;
	}

	public void setKeepBinary(boolean keepBinary) {
		this.keepBinary = keepBinary;
	}

	
	public boolean isOnlyLowerBoundMode() {
		return onlyLowerBoundMode;
	}

	
	public boolean isPolyApproxMode() {
		return polyApproxMode;
	}

	public ArrayList<MatOfPoint> getApproxContoursList() {
		return approxContoursList;
	}

	public void setApproxContoursList(ArrayList<MatOfPoint> approxContoursList) {
		this.approxContoursList = approxContoursList;
	}
	
	
}
