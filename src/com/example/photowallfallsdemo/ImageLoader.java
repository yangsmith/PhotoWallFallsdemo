package com.example.photowallfallsdemo;

import android.R.integer;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

public class ImageLoader {

	private static LruCache<String, Bitmap> mMemoryCache;
	
	private static ImageLoader mImageLoader = null;
	
	private  ImageLoader() {
		
		int nMaxMemory = (int)Runtime.getRuntime().maxMemory();
		int nCacheSize = nMaxMemory/8;
		mMemoryCache = new LruCache<String, Bitmap>(nCacheSize){
			
			@Override
			protected int sizeOf(String key, Bitmap value) {
				// TODO Auto-generated method stub
				return value.getByteCount();
			}
		};
	}
	
	public static ImageLoader getInstance(){
		if (mImageLoader == null) {
			mImageLoader = new ImageLoader();
		} 
		return mImageLoader;
	}
	
	public Bitmap getBitmapFromMemoryCache(String key){
		return mMemoryCache.get(key);
	}
	
	public void addBitmapToMemoryCache(String key,Bitmap bitmap){
		if (getBitmapFromMemoryCache(key) == null) {
			mMemoryCache.put(key, bitmap);
		}
	}
	
	public static int calculateInSampleSize(BitmapFactory.Options options,  
            int reqWidth) {  
        // 源图片的宽度  
        final int width = options.outWidth;  
        int inSampleSize = 1;  
        if (width > reqWidth) {  
            // 计算出实际宽度和目标宽度的比率  
            final int widthRatio = Math.round((float) width / (float) reqWidth);  
            inSampleSize = widthRatio;  
        }  
        return inSampleSize;  
    }  
  
    public static Bitmap decodeSampledBitmapFromResource(String pathName,  
            int reqWidth) {  
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小  
        final BitmapFactory.Options options = new BitmapFactory.Options();  
        options.inJustDecodeBounds = true;  
        BitmapFactory.decodeFile(pathName, options);  
        // 调用上面定义的方法计算inSampleSize值  
        options.inSampleSize = calculateInSampleSize(options, reqWidth);  
        // 使用获取到的inSampleSize值再次解析图片  
        options.inJustDecodeBounds = false;  
        return BitmapFactory.decodeFile(pathName, options);  
    }  
}
