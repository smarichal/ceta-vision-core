package com.example.utils;

import java.util.logging.Level;

public class Logger {

	private static java.util.logging.Logger logger = java.util.logging.Logger.getLogger(Logger.class.getName());
	public static void info(String msg){
		//TODO
	}
	
	public static void warning(String msg){
		//TODO
	}
	
	public static void error(String msg){
		logger.log(Level.SEVERE, msg);
	}
	
}
