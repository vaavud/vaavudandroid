package com.vaavud.android.ui.history.cache;


import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader.ImageCache;

/**
* Basic LRU Memory cache.
*
* @author Trey Robinson
*
*/
public class BitmapLruImageCache extends LruCache<String, Bitmap> implements ImageCache{

	private final String TAG = this.getClass().getSimpleName();
	
	public BitmapLruImageCache(int maxSize) {
		super(maxSize);
	}
	
	@Override
	protected int sizeOf(String key, Bitmap value) {
		return value.getRowBytes() * value.getHeight();
	}
	
	@Override
	public Bitmap getBitmap(String url) {
//		Log.v(TAG, "Retrieved item from Mem Cache: " +url);
		return get(url);
	}
	 
	@Override
	public void putBitmap(String url, Bitmap bitmap) {
//		Log.v(TAG, "Added item to Mem Cache: " +url);
		put(url, bitmap);
	}
}


