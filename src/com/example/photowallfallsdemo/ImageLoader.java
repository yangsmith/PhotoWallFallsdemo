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
        // ԴͼƬ�Ŀ��  
        final int width = options.outWidth;  
        int inSampleSize = 1;  
        if (width > reqWidth) {  
            // �����ʵ�ʿ�Ⱥ�Ŀ���ȵı���  
            final int widthRatio = Math.round((float) width / (float) reqWidth);  
            inSampleSize = widthRatio;  
        }  
        return inSampleSize;  
    }  
  
    public static Bitmap decodeSampledBitmapFromResource(String pathName,  
            int reqWidth) {  
        // ��һ�ν�����inJustDecodeBounds����Ϊtrue������ȡͼƬ��С  
        final BitmapFactory.Options options = new BitmapFactory.Options();  
        options.inJustDecodeBounds = true;  
        BitmapFactory.decodeFile(pathName, options);  
        // �������涨��ķ�������inSampleSizeֵ  
        options.inSampleSize = calculateInSampleSize(options, reqWidth);  
        // ʹ�û�ȡ����inSampleSizeֵ�ٴν���ͼƬ  
        options.inJustDecodeBounds = false;  
        return BitmapFactory.decodeFile(pathName, options);  
    }  
}
