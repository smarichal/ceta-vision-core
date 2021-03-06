package com.example.topcode;

import java.util.Set;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import android.graphics.Bitmap;

import com.example.blobdetection.Block;

public class TopCodeDetectorAndroid extends TopCodeDetector {


	protected Bitmap bmp;
	
	public TopCodeDetectorAndroid(int max_markers, boolean probMode, int max_marker_diameter, 
								int size_cache, boolean cacheEnabled, boolean allow_different_spot_distance){
		super(max_markers, probMode, max_marker_diameter, size_cache, cacheEnabled, allow_different_spot_distance);
		this.scanner = new ScannerAndroid();
		if(max_marker_diameter>0){
			this.scanner.setMaxCodeDiameter(max_marker_diameter);
		}
	}
	
	public Set<TopCode> update(Mat rgbaImage){
		bmp = Bitmap.createBitmap(rgbaImage.cols(), rgbaImage.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(rgbaImage, bmp);
		this.markers = ((ScannerAndroid)this.scanner).scan(this.bmp);
		if(probMode){
        	this.stateMatrix.updateAllProbs(this.markers, step);
        }else{
        	this.stateMatrix.addAll(this.markers);
        }
		bmp.recycle();
        
		return stateMatrix.getStateMarkers();
	}
	
	@Deprecated
	public Set<TopCode> update(Bitmap bitmapImg){
		bmp = bitmapImg;
		this.markers = ((ScannerAndroid)this.scanner).scan(this.bmp);
		if(probMode){
        	this.stateMatrix.updateAllProbs(this.markers, step);
        }else{
        	this.stateMatrix.addAll(this.markers);
        }
		return stateMatrix.getStateMarkers();
	}
	public void dummy(){
		
	}
	
	public Set<Block> detectBlocks(Mat rgbaImage){
		bmp = Bitmap.createBitmap(rgbaImage.cols(), rgbaImage.rows(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(rgbaImage, bmp);
		this.markers = ((ScannerAndroid)this.scanner).scan(this.bmp);
		bmp.recycle();
        groupMarkers();
        computeBlocks();
        return this.blocks;
	}
	
	public ScannerAndroid getScanner(){
		return ((ScannerAndroid)this.scanner);
	}
	
	public Mat getBinaryImage(){
		Bitmap preview = ((ScannerAndroid)this.scanner).getPreview();
		Mat previewMat = new Mat();
		Utils.bitmapToMat(preview, previewMat);
		
		return previewMat;
	}
}
