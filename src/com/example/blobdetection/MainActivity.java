package com.example.blobdetection;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.example.topcode.TopCode;
import com.example.topcode.TopCodeDetectorAndroid;

public class MainActivity extends ActionBarActivity implements OnTouchListener, CvCameraViewListener2  {

	private static final String  TAG              = "OCVSample::Activity";


	private static final int MAX_MARKERS = 40;


    private CameraBridgeViewBase mOpenCvCameraView;
    
    //Menu items
	private MenuItem mScreenshot;
	
	
	private boolean showBinaryMode = false;	
	private boolean front_camera = true;

	private int count = 0;
	private boolean saveScreenshot = false;
	private Mat mRgba;

	private TopCodeDetectorAndroid topCodeDetector;
	
	Set<TopCode> topCodeList = new HashSet<TopCode>();
	Set<Block> blocks;
	private int minWidth = 640, minHeight = 480;
    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setMinimumWidth(minWidth);
                    mOpenCvCameraView.setMinimumHeight(minHeight);
                    mOpenCvCameraView.setMaxFrameSize(minWidth, minHeight);
                    
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


	private MenuItem mShowInfo;


	private boolean showInfo = true;

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

        int maxDiameter = 70;
        int size_cache = 5;
        boolean cacheEnabeld = true;
        boolean allow_different_spot_distance = false;
        this.topCodeDetector = new TopCodeDetectorAndroid(MAX_MARKERS, true, maxDiameter,size_cache, cacheEnabeld, allow_different_spot_distance);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        
        if(front_camera){
        	mOpenCvCameraView.setCameraIndex(1);
        }
        mOpenCvCameraView.setCvCameraViewListener(this);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i(TAG, "called onCreateOptionsMenu");
        mScreenshot = menu.add("Screenshot");
        mShowInfo = menu.add("Show Info");
        return true;
    }
    
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if(item==mScreenshot){
        	saveScreenshot = true;
        }else if(item==mShowInfo){
        	showInfo  = !showInfo;
        	mShowInfo.setTitle(showInfo? "Hide Info":"Show Info");
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
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
    	//TODO do something?
        return false; // don't need subsequent touch events
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        if(front_camera){
        	Mat m = inputFrame.rgba();
        	Core.flip(m, mRgba, 1);
        }else{
        	mRgba = inputFrame.rgba();
        }
        if(saveScreenshot){
        	saveImage(mRgba);
        	saveScreenshot=false;
        }
        
        this.blocks = this.topCodeDetector.detectBlocks(mRgba);
        
        for(Iterator<Block> iter = this.blocks.iterator();iter.hasNext();){
        	Block block = iter.next();
        	//TODO do something with the markers?
        
        	drawBlockContour(mRgba, block);
        	drawText(mRgba, ".", new Point(block.getCenter().x, block.getCenter().y) , false, 2);
        }
        
        if(showBinaryMode){
        	//TODO show topcode binary mode
        	return mRgba;
        }else{
        	return mRgba;
        }
    }

    private void drawBlockContour(Mat img, Block block) {
    	double width = block.getWidth();
    	double height = block.getHeight();
    	RotatedRect rRect = new RotatedRect(block.getCenter(), new Size(width, height), Math.toDegrees(block.getOrientation()));
    	Point vertices[] = new Point[4];
    	rRect.points(vertices);
    	for (int i = 0; i < 4; i++)
    	    Core.line(img, vertices[i], vertices[(i+1)%4], new Scalar(0,255,0));
	}

	private void drawText(Mat img, String text, Point textOrg, boolean drawRectangle, int thickness){
    	int fontFace = Core.FONT_HERSHEY_PLAIN;
    	double fontScale = 1.5;
    	int baseline[]={0};
    	Size textSize = Core.getTextSize(text, fontFace, fontScale, thickness, baseline);
    	baseline[0] += thickness;
    	if(drawRectangle){
	    	// draw the box
	    	Core.rectangle(img, new Point(textOrg.x, textOrg.y + baseline[0]),
	    	          new Point(textOrg.x + textSize.width,textOrg.y -textSize.height),
	    	          new Scalar(0,0,255));
    	}
    	// then put the text itself
    	Core.putText(img, text, textOrg, fontFace, fontScale,
    			Scalar.all(255), thickness, 8, false);
    }
    
    private void saveImage(Mat image){
    	File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
    	File file = new File(path, "screenshot-"+count+".png");
    	String filename = file.toString();
    	
    	Mat bgrImage = new Mat();
    	Imgproc.cvtColor(image, bgrImage, Imgproc.COLOR_RGB2BGR);
    	if(!Highgui.imwrite(filename,bgrImage)){
    		Context	context	=	getApplicationContext();
			CharSequence	text	=	"Failed to save the image!";
			int	duration	=	Toast.LENGTH_SHORT;
			Toast	toast	=	Toast.makeText(context,	text,	duration);
			toast.show();
    	}else{
        	count++;	
        	//sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
        	MediaScannerConnection.scanFile(this, new String[] { file.getPath() }, new String[] { "image/png" }, null);

    	}
    }
}
