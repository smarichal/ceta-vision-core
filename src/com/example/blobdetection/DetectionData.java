package com.example.blobdetection;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;

public class DetectionData {

	private BlockColors color;
	private  List<MatOfPoint> contours;
	private Mat mask;
	private List<Block> blocks;
	private List<RotatedRect> rotatedRects;
	
	private Scalar hsvValues, hsvUpper, hsvLower;
	
	public DetectionData(BlockColors color, Scalar hsvValues, Scalar hsvUpper, Scalar hsvLower){
		this.contours = new ArrayList<MatOfPoint>();
		this.mask = new Mat();
		this.blocks = new LinkedList<Block>();
		this.color = color;
		this.hsvValues = hsvValues;
		this.hsvUpper = hsvUpper;
		this.hsvLower = hsvLower;
	}
	
	public BlockColors getColor() {
		return color;
	}
	
	public void setColor(BlockColors color) {
		this.color = color;
	}
	
	public List<MatOfPoint> getContours() {
		return contours;
	}
	
	public void setContours(List<MatOfPoint> contours) {
		this.contours = contours;
	}
	
	public Mat getMask() {
		return mask;
	}
	
	public void setMask(Mat mask) {
		this.mask = mask;
	}
	
	public List<Block> getBlocks() {
		return blocks;
	}
	
	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	public Scalar getHsvValues() {
		return hsvValues;
	}

	public void setHsvValues(Scalar hsvValues) {
		this.hsvValues = hsvValues;
	}

	public Scalar getHsvUpper() {
		return hsvUpper;
	}

	public void setHsvUpper(Scalar hsvUpper) {
		this.hsvUpper = hsvUpper;
	}

	public Scalar getHsvLower() {
		return hsvLower;
	}

	public void setHsvLower(Scalar hsvLower) {
		this.hsvLower = hsvLower;
	}

	public List<RotatedRect> getRotatedRects() {
		return rotatedRects;
	}

	public void setRotatedRects(List<RotatedRect> rotatedRects) {
		this.rotatedRects = rotatedRects;
	}
	
	
}
