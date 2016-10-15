package com.example.blobdetection;

import java.util.Iterator;
import java.util.List;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import com.example.blobdetection.*;

import android.support.v7.app.ActionBarActivity;
import android.hardware.camera2.params.MeteringRectangle;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnTouchListener;

public class MainActivity extends ActionBarActivity implements OnTouchListener, CvCameraViewListener2  {

	private static final String  TAG              = "OCVSample::Activity";

    private boolean              mIsColorSelected = false;
    private Mat                  mRgba;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private ColorBlobDetector    mDetector;
    private Mat                  mSpectrum;
    private Size                 SPECTRUM_SIZE;
    private Scalar               CONTOUR_COLOR;

    private CameraBridgeViewBase mOpenCvCameraView;

    
    
    //Menu items
    private MenuItem	mItemClear;
    private MenuItem	mItemShowBinary;
	private MenuItem mItemOnlyLowerBound;
	private MenuItem mInteractiveMode;
	private MenuItem mMinRectangle;
	private MenuItem mAproxPoly;
	
	
	private boolean showBinaryMode = false;
	private boolean onlyLowerBound = false;
	private boolean interactiveMode = true;
	private boolean minRectangleMode = false;
	private boolean aproxPolyMode = true;
    
	
	private boolean front_camera = true;

	
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
//                    mOpenCvCameraView.setMinimumWidth(800);
//                    mOpenCvCameraView.setMinimumHeight(480);
//                    mOpenCvCameraView.setMaxFrameSize(800, 480);
                    
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                    mOpenCvCameraView.enableFpsMeter();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

	


;


    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
               
        setContentView(R.layout.activity_main);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        if(front_camera){
        	mOpenCvCameraView.setCameraIndex(1);
        }mOpenCvCameraView.setCvCameraViewListener(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mItemClear= menu.add("Clear");
        mItemShowBinary= menu.add("Binary/Color");
        mItemOnlyLowerBound= menu.add("Set radius mode");
        mInteractiveMode = menu.add("Load CETA preset");
        mMinRectangle = menu.add("Show min Rectangles");
        mAproxPoly = menu.add("Se approx poly OFF");

        return true;
    }
    

    private void reset(){
    	mDetector = new ColorBlobDetector();
    	mDetector.setKeepBinary(showBinaryMode);
    	mDetector.setOnlyLowerBoundMode(onlyLowerBound);
    	if(!interactiveMode){
    		mDetector.loadCETAPresetColors();
    	}
    }
    
    
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemClear) {
        	mBlobColorHsv = new Scalar(0,0,0,0);
        	mIsColorSelected = false;
        	reset();
        } else if (item == mItemShowBinary) {
        	showBinaryMode = !showBinaryMode;
        	minRectangleMode = false;
        	mDetector.setKeepBinary(showBinaryMode);
        } else if (item == mItemOnlyLowerBound) {
        	onlyLowerBound=!onlyLowerBound;
        	mDetector.setOnlyLowerBoundMode(onlyLowerBound);
        	if(onlyLowerBound){
        		mItemOnlyLowerBound.setTitle("Set radius mode");
        	}else{
        		mItemOnlyLowerBound.setTitle("Set Lower bound mode");
        	}
        } else if (item == mInteractiveMode) {
        	interactiveMode=!interactiveMode;
        	if(!interactiveMode){
        		mInteractiveMode.setTitle("Interactive Mode");
        		mDetector.loadCETAPresetColors();
        		mIsColorSelected=true;
        	}else{
        		mInteractiveMode.setTitle("Load CETA preset");
        		reset();
        		mIsColorSelected=false;
        	}
        } else if (item == mMinRectangle) {
        	minRectangleMode=!minRectangleMode;
        	showBinaryMode = false;
        } else if (item == mAproxPoly){
        	aproxPolyMode  = !aproxPolyMode;
        	if(aproxPolyMode){
        		mAproxPoly.setTitle("Se approx poly OFF");
        	}else{
        		mAproxPoly.setTitle("Se approx poly ON");
        	}
        	mDetector.setPolyApproxMode(aproxPolyMode);
        }
        return true;
    }
    

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        mSpectrum = new Mat();
        mBlobColorRgba = new Scalar(255);
        mBlobColorHsv = new Scalar(255);
        SPECTRUM_SIZE = new Size(200, 64);
        CONTOUR_COLOR = new Scalar(255,0,0,255);
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
    	if(interactiveMode){
	        int cols = mRgba.cols();
	        int rows = mRgba.rows();
	
	        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
	        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;
	
	        int x = (int)event.getX() - xOffset;
	        int y = (int)event.getY() - yOffset;
	
	        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");
	
	        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;
	
	        Rect touchedRect = new Rect();
	
	        touchedRect.x = (x>4) ? x-4 : 0;
	        touchedRect.y = (y>4) ? y-4 : 0;
	
	        touchedRect.width = (x+4 < cols) ? x + 4 - touchedRect.x : cols - touchedRect.x;
	        touchedRect.height = (y+4 < rows) ? y + 4 - touchedRect.y : rows - touchedRect.y;
	
	        Mat touchedRegionRgba = mRgba.submat(touchedRect);
	
	        Mat touchedRegionHsv = new Mat();
	        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);
	
	        // Calculate average color of touched region
	        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
	        int pointCount = touchedRect.width*touchedRect.height;
	        for (int i = 0; i < mBlobColorHsv.val.length; i++)
	            mBlobColorHsv.val[i] /= pointCount;
	
	        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);
	
	        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
	                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");
	
	        mDetector.addHsvColor(mBlobColorHsv);
	
	        Mat m = mDetector.getSpectrum(); 
	        Imgproc.resize(m, mSpectrum, SPECTRUM_SIZE);
	
	        mIsColorSelected = true;
	
	        touchedRegionRgba.release();
	        touchedRegionHsv.release();
    	}
        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if(front_camera){
        	Mat m = inputFrame.rgba();
        	Core.flip(m, mRgba, 1);
        }else{
        	mRgba = inputFrame.rgba();
        }
        
        if (mIsColorSelected) {
            mDetector.process(mRgba);
            List<MatOfPoint> contours = mDetector.getContours();
            //Log.e(TAG, "Contours count: " + contours.size());
            if(contours.size()>1){
            	System.out.println("Sfdsf");
            }
            Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR,3);
            Imgproc.drawContours(mRgba, mDetector.getApproxContoursList(), -1, new Scalar(0,255,0,100),1);
            

            Mat colorLabel = mRgba.submat(4, 68, 4, 68);
            colorLabel.setTo(mBlobColorRgba);

            Mat spectrumLabel = mRgba.submat(4, 4 + mSpectrum.rows(), 70, 70 + mSpectrum.cols());
            mSpectrum.copyTo(spectrumLabel);
            
            String hsvText = "(" + mBlobColorHsv.val[0] + ", " + mBlobColorHsv.val[1] +
                    ", " + mBlobColorHsv.val[2] + ", " + mBlobColorHsv.val[3] + ")";
            
            drawText(mRgba, hsvText, new Point(4, 80), false);
        }

        if(showBinaryMode){
        	Mat resized = new Mat(); 
        	Mat mask = mDetector.getDilatedBinaryMask();
        	Imgproc.resize(mask, resized, mRgba.size());
        	return resized;
        }else if(minRectangleMode){
        	mDetector.computeMinRectangles();
        	List<RotatedRect> minRectangles = mDetector.getMinRectangles();
        	for(Iterator<RotatedRect> iter = minRectangles.iterator();iter.hasNext();){
        		RotatedRect rect = iter.next();
        		Point[] vertices = new Point[4];  
                rect.points(vertices);  
                for (int j = 0; j < 4; j++){  
                    Core.line(mRgba, vertices[j], vertices[(j+1)%4], new Scalar(0,255,0));
                }
                
        		//this draws the bounding rect, without rotation
        		//Core.rectangle(mRgba, rect.boundingRect().tl(), rect.boundingRect().br(), new Scalar(255, 0, 0), 2);
        	}
        	return mRgba;
        }else{
        	return mRgba;
        }
    }

    private Scalar converScalarHsv2Rgba(Scalar hsvColor) {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }
    
    
    
    private void drawText(Mat img, String text, Point textOrg, boolean drawRectangle){
    
    	int fontFace = Core.FONT_HERSHEY_SCRIPT_SIMPLEX;
    	double fontScale = 0.5;
    	int thickness = 1;

    	int baseline[]={0};
    	
    	Size textSize = Core.getTextSize(text, fontFace,
    	                            fontScale, thickness, baseline);
    	baseline[0] += thickness;

    	
    	// center the text
//    	Point textOrg = new Point((img.cols() - textSize.width)/2,
//    	              (img.rows() + textSize.height)/2);

    	if(drawRectangle){
	    	// draw the box
	    	Core.rectangle(img, new Point(textOrg.x, textOrg.y + baseline[0]),
	    	          new Point(textOrg.x + textSize.width,textOrg.y -textSize.height),
	    	          new Scalar(0,0,255));
    	}
    	
    	// ... and the baseline first
    	/*Core.line(img, new Point(textOrg.x,textOrg.y + thickness),
    	     new Point(textOrg.x + textSize.width,textOrg.y + thickness),
    	     new Scalar(0, 0, 255));
    	 */
    	
    	
    	// then put the text itself
    	Core.putText(img, text, textOrg, fontFace, fontScale,
    			Scalar.all(255), thickness, 8, false);
    }
}
