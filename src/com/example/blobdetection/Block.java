package com.example.blobdetection;

import org.opencv.core.Point;
import org.opencv.core.Size;

public class Block {

	private Point center;
	private double area;
	private double height, width;
	private Point[] vertices;
	

	private BlockColors type;
	
	public Block() {
		super();
	}
	
	public Block(Point center, Size size, Point[] vertices) {
		super();
		this.center = center;
		this.area = size.area();
		this.height = size.height;
		this.width = size.width;
		this.vertices = vertices;
	}

	public Point getCenter() {
		return center;
	}

	public void setCenter(Point center) {
		this.center = center;
	}

	public double getArea() {
		return area;
	}

	public void setArea(double area) {
		this.area = area;
	}

	public Point[] getVertices() {
		return vertices;
	}

	public void setVertices(Point[] vertices) {
		this.vertices = vertices;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}
}
