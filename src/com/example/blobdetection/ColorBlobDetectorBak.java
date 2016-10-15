package com.example.blobdetection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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

public class ColorBlobDetectorBak {
	
	private static int n_classes = 3;  //Amount of objects colors to detect (red, blue, green, etc)
	
    // Lower and Upper bounds for range checking in HSV color space
    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);
    
    
    /*
     *   hsvBoundaries: For channels to store Scalar datatype		
     *   				
     *   							lower	|   upper
     *   
     *   color1			 -->    [h,s,v,a]       [h,s,v,a]
     *   color2			 -->	[h,s,v,a]       [h,s,v,a]
     *   ...
     *   ...
     *   color_n_classes -->    [h,s,v,a]       [h,s,v,a]
     * 
     * */
    private Mat hsvBoundaries = new Mat(n_classes, 2, CvType.CV_8UC4);
    private List<Scalar> hsvValues = new LinkedList<Scalar>();
    private int classes_count_index = 0;
    
    
    private List<RotatedRect> minRectangles = new LinkedList<RotatedRect>();
    private List<Block> detectedBlocks = new LinkedList<Block>();
    
    // Minimum contour area in percent for contours filtering
    private static double mMinContourArea = 0.1;
    // Color radius for range checking in HSV color space
    private Scalar mColorRadius = new Scalar(25,50,50,0);
    private Mat mSpectrum = new Mat();
    private List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();

    // Cache
    Mat mPyrDownMat = new Mat();
    Mat mHsvMat = new Mat();
    Mat mMask = new Mat();
    Mat mAccumulatedMask = new Mat();
    Mat mDilatedMask = new Mat();
    Mat mHierarchy = new Mat();

    private List<DetectionData> detectionData =  new LinkedList<DetectionData>();
    
	private boolean keepBinary = false;

	private Mat dilatedMaskCopy;

	private int count=0;

	private boolean onlyLowerBoundMode = false;

	private double image_downsample_factor = 2;

    public void setColorRadius(Scalar radius) {
        mColorRadius = radius;
    }

    public void setHsvColor(Scalar hsvColor) {
        computeUpperAndLowerBounds(hsvColor);
        this.hsvValues.add(hsvColor);
        computeSpectrumHsv();
    }
    
    
    private double calculateMinH(double h, double radius){
    	return (h >= radius) ? h -radius : 0;
    }
    
    private double calculateMaxH(double h, double radius){
    	return (h+radius <= 255) ? h+radius : 255;
    }
    
    public void loadCETAPresetColors(){
    	this.hsvValues.clear();
    	int index;
    	count=0;
    	
    	Scalar hsvRedColor = new Scalar(9,200,90);
    	Scalar hsvBlueColor = new Scalar(167,200,90);
    	Scalar hsvGreenColor = new Scalar(85,200,90);

    	this.hsvValues.add(hsvGreenColor);
    	this.hsvValues.add(hsvBlueColor);
    	this.hsvValues.add(hsvRedColor);
    	
    	
    	for(Iterator<Scalar> iter = this.hsvValues.iterator();iter.hasNext();){
    		Scalar color = iter.next();
        	computeUpperAndLowerBounds(color);
    	
        	index = classes_count_index%n_classes;
            classes_count_index++;
            if(count<n_classes){
            	count++;
            }
            hsvBoundaries.put(index, 0, mLowerBound.val);
            hsvBoundaries.put(index, 1, mUpperBound.val);
    	}        
    }
    
    public void addHsvColor(Scalar hsvColor){
        computeUpperAndLowerBounds(hsvColor);
        computeSpectrumHsv();
        
        int index = classes_count_index%n_classes;
        classes_count_index++;
        if(count<n_classes){
        	count++;
        }
        
        this.hsvValues.add(hsvColor);
        hsvBoundaries.put(index, 0, mLowerBound.val);
        hsvBoundaries.put(index, 1, mUpperBound.val);
    }
    
    private void computeUpperAndLowerBounds(Scalar hsvColor){
    	double minH = calculateMinH(hsvColor.val[0], mColorRadius.val[0]);
        double maxH = calculateMaxH(hsvColor.val[0], mColorRadius.val[0]);

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;
    	if(onlyLowerBoundMode ){
            mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
            mUpperBound.val[1] = 255;

            mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
            mUpperBound.val[2] = 255;
        }else{
            mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
            mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

            mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
            mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];
        }
        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;
    }
    
    

	private void computeSpectrumHsv() {
		Mat spectrumHsv = new Mat(1, (int)(mUpperBound.val[0]-mLowerBound.val[0]), CvType.CV_8UC3);
        for (int j = 0; j < mUpperBound.val[0]-mLowerBound.val[0]; j++) {
            byte[] tmp = {(byte)(mLowerBound.val[0]+j), (byte)255, (byte)255};
            spectrumHsv.put(0, j, tmp);
        }
        Imgproc.cvtColor(spectrumHsv, mSpectrum, Imgproc.COLOR_HSV2RGB_FULL, 4);
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

        doImageSegmentation();

        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();

        Imgproc.findContours(mDilatedMask, contours, mHierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        // Find max contour area
        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        // Filter contours by area and resize to fit the original image size
        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea*maxArea) {
                Core.multiply(contour, new Scalar(image_downsample_factor,image_downsample_factor ), contour);
                mContours.add(contour);
            }
        }
    }

    
    private void doImageSegmentation(){
    	//Core.inRange(mHsvMat, mLowerBound, mUpperBound, mMask);
    	mAccumulatedMask = Mat.zeros(mHsvMat.size(), CvType.CV_8UC1);
    	for(int i = 0;i<count;i++){
    		Core.inRange(mHsvMat, new Scalar(hsvBoundaries.get(i, 0)), new Scalar(hsvBoundaries.get(i, 1)), mMask);
    		//List<Mat> channels = new LinkedList<Mat>();
    		//Core.split(mHsvMat,channels);
    		//Core.inRange(channels.get(0), new Scalar(hsvBoundaries.get(i, 0)[0]),new Scalar( hsvBoundaries.get(i, 1)[0]), mMask);
    		Core.add(mAccumulatedMask, mMask, mAccumulatedMask);
    	}
        //Imgproc.dilate(mMask, mDilatedMask, new Mat());
    	Imgproc.dilate(mAccumulatedMask, mDilatedMask, new Mat());
        if(keepBinary){
        	this.dilatedMaskCopy = mDilatedMask.clone();
        }
    }
    
    private void doImageSegmentationByColor(){
    	mAccumulatedMask = Mat.zeros(mHsvMat.size(), CvType.CV_8UC1);
    	for(int i = 0;i<count;i++){
    		Core.inRange(mHsvMat, new Scalar(hsvBoundaries.get(i, 0)), new Scalar(hsvBoundaries.get(i, 1)), mMask);
    		//List<Mat> channels = new LinkedList<Mat>();
    		//Core.split(mHsvMat,channels);
    		//Core.inRange(channels.get(0), new Scalar(hsvBoundaries.get(i, 0)[0]),new Scalar( hsvBoundaries.get(i, 1)[0]), mMask);
    		Core.add(mAccumulatedMask, mMask, mAccumulatedMask);
    	}
        //Imgproc.dilate(mMask, mDilatedMask, new Mat());
    	Imgproc.dilate(mAccumulatedMask, mDilatedMask, new Mat());
        if(keepBinary){
        	this.dilatedMaskCopy = mDilatedMask.clone();
        }
    }
    
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

	public void setOnlyLowerBoundMode(boolean onlyLowerBoundMode) {
		this.onlyLowerBoundMode = onlyLowerBoundMode;
		if(onlyLowerBoundMode){
			for(int i = 0;i<count;i++){
				double[] dataUpper = hsvBoundaries.get(i, 1);
				dataUpper[1] = 255;
				dataUpper[2] = 255;
				hsvBoundaries.put(i, 1, dataUpper);
			}
		}else{
			for(int i = 0;i<count;i++){
				double[] dataUpper = hsvBoundaries.get(i, 1);
				dataUpper[1] = this.hsvValues.get(i).val[1] + mColorRadius.val[1];
				dataUpper[2] = this.hsvValues.get(i).val[2] + mColorRadius.val[2];    
				hsvBoundaries.put(i, 1, dataUpper);
			}
		}
	}

	public void aproxPoly(){
    	//Imgproc.
    }
    
	public void computeMinRectangles(){
		this.minRectangles.clear();
		for(Iterator<MatOfPoint> iter = mContours.iterator();iter.hasNext();){
			MatOfPoint2f mat = new MatOfPoint2f(iter.next().toArray());
			this.minRectangles.add(Imgproc.minAreaRect(mat));
		}
	}
	
	public List<Block> detectBlocks(Mat rgbaImage){
		this.process(rgbaImage);
		this.computeMinRectangles();
    	for(Iterator<RotatedRect> iter = minRectangles.iterator();iter.hasNext();){
    		RotatedRect rect = iter.next();
    		Point[] vertices = new Point[4];  
            rect.points(vertices);  
            this.detectedBlocks.add(new Block(rect.center,rect.size,vertices));
    	}
    	return this.detectedBlocks;
	}
	
	public List<RotatedRect> getMinRectangles(){
		return this.minRectangles;
	}
	
	public void computeMoments(){

	}
    
    public List<MatOfPoint> getContours() {
        return mContours;
    }
}
