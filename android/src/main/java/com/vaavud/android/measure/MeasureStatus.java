package com.vaavud.android.measure;

import com.vaavud.android.R;

public enum MeasureStatus {
	MEASURING(R.string.info_measuring), 
	NO_SIGNAL(R.string.info_no_signal),
	KEEP_VERTICAL(R.string.info_keep_steady);
	
	private int id;
	
	private MeasureStatus(int id){
		this.id = id;
	}
	
	public int getResourceId() {
		return id;
	}
}
