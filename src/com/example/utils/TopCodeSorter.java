package com.example.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.example.topcode.TopCode;



public class TopCodeSorter{

	/**
	 * Returns spots ordered by x position
	 * @param spots
	 * @return
	 */
	public static List<TopCode> sortHorizontally(List<TopCode> spots){
		TopCodeXComparator comparator = new TopCodeXComparator();
		 Collections.sort(spots, comparator);
		 return spots;
	}
}
