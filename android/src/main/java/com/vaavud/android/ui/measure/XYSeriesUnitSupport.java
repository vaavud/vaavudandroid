package com.vaavud.android.ui.measure;

import com.vaavud.android.model.entity.SpeedUnit;

import org.achartengine.model.XYSeries;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

public class XYSeriesUnitSupport extends XYSeries {
	
	private SpeedUnit currentUnit;
	
	public XYSeriesUnitSupport(String title, SpeedUnit unit) {
		super(title);
		this.currentUnit = unit;
	}

	public XYSeriesUnitSupport(String title, int scaleNumber, SpeedUnit unit) {
		super(title, scaleNumber);
		this.currentUnit = unit;
	}	
	
	public void setUnit(SpeedUnit _currentUnit) {
		this.currentUnit = _currentUnit;
	}
	
	@Override
	public double getY(int index) {
		return currentUnit.toDisplayValue(super.getY(index));
	}
	
	@Override 
	public double getMinY() {
		return currentUnit.toDisplayValue(super.getMinY());
	}
	
	@Override 
	public double getMaxY() {
		return currentUnit.toDisplayValue(super.getMaxY());
	}
	
	public java.util.SortedMap<java.lang.Double,java.lang.Double> getRange(double start,
            double stop,
            boolean beforeAfterPoints) {
		
		SortedMap<Double, Double> dataMap =  super.getRange(start, stop, beforeAfterPoints);
		SortedMap<Double, Double> newDataMap = new TreeMap<Double, Double>();
		
		for (Iterator<Double> iterator = dataMap.keySet().iterator(); iterator.hasNext();) {
			Double key = (Double) iterator.next();
			newDataMap.put(key, currentUnit.toDisplayValue(dataMap.get(key)));
		}
		
		return newDataMap;
	}	
}
