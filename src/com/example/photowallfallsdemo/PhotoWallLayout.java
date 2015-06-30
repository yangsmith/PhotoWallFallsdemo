package com.example.photowallfallsdemo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.AttributedCharacterIterator.Attribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public class PhotoWallLayout extends ScrollView implements OnTouchListener {
	/**
	 * ÿҳҪ���ص�ͼƬ����
	 */
	public static final int PAGE_SIZE = 15;

	/**
	 * ��¼��ǰ�Ѽ��ص��ڼ�ҳ
	 */
	private int page;

	/**
	 * ÿһ�еĿ��
	 */
	private int columnWidth;

	/**
	 * ��ǰ��һ�еĸ߶�
	 */
	private int firstColumnHeight;

	/**
	 * ��ǰ�ڶ��еĸ߶�
	 */
	private int secondColumnHeight;

	/**
	 * ��ǰ�����еĸ߶�
	 */
	private int thirdColumnHeight;

	/**
	 * �Ƿ��Ѽ��ع�һ��layout������onLayout�еĳ�ʼ��ֻ�����һ��
	 */
	private boolean loadOnce = false;

	/**
	 * ��ͼƬ���й���Ĺ�����
	 */
	private ImageLoader imageLoader;

	/**
	 * ��һ�еĲ���
	 */
	private LinearLayout firstColumn;

	/**
	 * �ڶ��еĲ���
	 */
	private LinearLayout secondColumn;

	/**
	 * �����еĲ���
	 */
	private LinearLayout thirdColumn;

	/**
	 * ��¼�����������ػ�ȴ����ص�����
	 */
	private static Set<LoadImageTask> taskCollection;

	/**
	 * MyScrollView�µ�ֱ���Ӳ��֡�
	 */
	private static View scrollLayout;

	/**
	 * MyScrollView���ֵĸ߶ȡ�
	 */
	private static int scrollViewHeight;

	/**
	 * ��¼�ϴ�ֱ����Ĺ������롣
	 */
	private static int lastScrollY = -1;

	/**
	 * ��¼���н����ϵ�ͼƬ�����Կ�����ʱ���ƶ�ͼƬ���ͷš�
	 */
	private List<ImageView> imageViewList = new ArrayList<ImageView>();
	
	private static Handler handler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			PhotoWallLayout photoWallLayout = (PhotoWallLayout)msg.obj;
			int nsrollY = photoWallLayout.getScrollY();
			if (nsrollY == lastScrollY) {
				if (scrollViewHeight + nsrollY >= scrollLayout.getHeight() && taskCollection.isEmpty()) {
					
					photoWallLayout.loadMoreImages();  
                }  
                photoWallLayout.checkVisibility();  
			}else {
				lastScrollY = nsrollY;
				Message message = new Message();
				message.obj = photoWallLayout;
				handler.sendMessageDelayed(message, 5);
			}
		};
	};
	
    public PhotoWallLayout(Context context ,AttributeSet attrs){
    	super(context,attrs);
    	imageLoader = ImageLoader.getInstance();
    	taskCollection = new HashSet<LoadImageTask>();
    	setOnTouchListener(this);
    }
    
    
    
    @Override
    public boolean onTouch(View v, MotionEvent event) {
    	// TODO Auto-generated method stub
    	if (event.getAction() == MotionEvent.ACTION_UP) {
    		Message message = new Message();  
            message.obj = this;  
            handler.sendMessageDelayed(message, 5);  
		}
    	return false;
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    	// TODO Auto-generated method stub
    	super.onLayout(changed, l, t, r, b);
    	if(changed && !loadOnce){
    		scrollViewHeight = getHeight();  
            scrollLayout = getChildAt(0);  
            firstColumn = (LinearLayout) findViewById(R.id.first_column);  
            secondColumn = (LinearLayout) findViewById(R.id.second_column);  
            thirdColumn = (LinearLayout) findViewById(R.id.third_column);  
            columnWidth = firstColumn.getWidth();  
            loadOnce = true;  
            loadMoreImages();  
    	}
    }
    
    /** 
     * ��ʼ������һҳ��ͼƬ��ÿ��ͼƬ���Ὺ��һ���첽�߳�ȥ���ء� 
     */  
    public void loadMoreImages() {  
        if (hasSDCard()) {  
            int startIndex = page * PAGE_SIZE;  
            int endIndex = page * PAGE_SIZE + PAGE_SIZE;  
            if (startIndex < ImageSet.imageThumUrls.length) {  
                Toast.makeText(getContext(), "���ڼ���...", Toast.LENGTH_SHORT)  
                        .show();  
                if (endIndex > ImageSet.imageThumUrls.length) {  
                    endIndex = ImageSet.imageThumUrls.length;  
                }  
                for (int i = startIndex; i < endIndex; i++) {  
                    LoadImageTask task = new LoadImageTask();  
                    taskCollection.add(task);  
                    task.execute(ImageSet.imageThumUrls[i]);  
                }  
                page++;  
            } else {  
                Toast.makeText(getContext(), "��û�и���ͼƬ", Toast.LENGTH_SHORT)  
                        .show();  
            }  
        } else {  
            Toast.makeText(getContext(), "δ����SD��", Toast.LENGTH_SHORT).show();  
        }  
    }

	/**
	 * ����imageViewList�е�ÿ��ͼƬ����ͼƬ�Ŀɼ��Խ��м�飬���ͼƬ�Ѿ��뿪��Ļ�ɼ���Χ����ͼƬ�滻��һ�ſ�ͼ��
	 */
	public void checkVisibility() {
		for (int i = 0; i < imageViewList.size(); i++) {
			ImageView imageView = imageViewList.get(i);
			int borderTop = (Integer) imageView.getTag(R.string.border_top);
			int borderBottom = (Integer) imageView
					.getTag(R.string.border_bottom);
			if (borderBottom > getScrollY()
					&& borderTop < getScrollY() + scrollViewHeight) {
				String imageUrl = (String) imageView.getTag(R.string.image_url);
				Bitmap bitmap = imageLoader.getBitmapFromMemoryCache(imageUrl);
				if (bitmap != null) {
					imageView.setImageBitmap(bitmap);
				} else {
					LoadImageTask task = new LoadImageTask(imageView);
					task.execute(imageUrl);
				}
			} else {
				imageView.setImageResource(R.drawable.empty_photo);
			}
		}
	}

	/**
	 * �ж��ֻ��Ƿ���SD����
	 * 
	 * @return ��SD������true��û�з���false��
	 */
	private boolean hasSDCard() {
		return Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
	}

	/**
	 * �첽����ͼƬ������
	 * 
	 */

	class LoadImageTask extends AsyncTask<String, Void, Bitmap> {

		private String mImageUrl;

		/**
		 * ���ظ�ʹ�õ�ImageView
		 */
		private ImageView mImageView;

		public LoadImageTask() {
		}

		/**
		 * �����ظ�ʹ�õ�ImageView����
		 * 
		 * @param imageView
		 */
		public LoadImageTask(ImageView imageView) {
			mImageView = imageView;
		}

		@Override
		protected Bitmap doInBackground(String... params) {
			// TODO Auto-generated method stub
			mImageUrl = params[0];
			Bitmap bitmap = imageLoader.getBitmapFromMemoryCache(mImageUrl);
			if (bitmap == null) {
				bitmap = loadImage(mImageUrl);
			}

			return bitmap;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap) {
			// TODO Auto-generated method stub
			if (bitmap != null) {
				double ratio = bitmap.getWidth() / (columnWidth * 1.0);
				int scaledHeight = (int) (bitmap.getHeight() / ratio);
				addImage(bitmap, columnWidth, scaledHeight);
			}
			taskCollection.remove(this);
		}

		private Bitmap loadImage(String imageUrl) {
			File imageFile = new File(getImagePath(imageUrl));
			if (!imageFile.exists()) {
				downloadImage(imageUrl);
			}

			if (imageUrl != null) {
				Bitmap bitmap = imageLoader.decodeSampledBitmapFromResource(
						imageFile.getPath(), columnWidth);
				if (bitmap != null) {
					imageLoader.addBitmapToMemoryCache(imageUrl, bitmap);
					return bitmap;
				}
			}

			return null;
		}

		private void addImage(Bitmap bitmap, int imageWidth, int imageHeight) {
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
					imageWidth, imageHeight);
			if (mImageView != null) {
				mImageView.setImageBitmap(bitmap);
			} else {
				ImageView imageView = new ImageView(getContext());
				imageView.setLayoutParams(params);
				imageView.setImageBitmap(bitmap);
				imageView.setScaleType(ScaleType.FIT_XY);
				imageView.setPadding(5, 5, 5, 5);
				imageView.setTag(R.string.image_url, mImageUrl);
				imageView.setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						Intent intent = new Intent(getContext(),ImageDetailsActivity.class);
						intent.putExtra("image_path", getImagePath(mImageUrl));
						getContext().startActivity(intent);
					}
				});
				findColumnToAdd(imageView, imageHeight).addView(imageView);
				imageViewList.add(imageView);
			}
		}

		private LinearLayout findColumnToAdd(ImageView imageView,
				int imageHeight) {
			if (firstColumnHeight <= secondColumnHeight) {
				if (firstColumnHeight <= thirdColumnHeight) {
					imageView.setTag(R.string.border_top, firstColumnHeight);
					firstColumnHeight += imageHeight;
					imageView.setTag(R.string.border_bottom, firstColumnHeight);
					return firstColumn;
				}
				imageView.setTag(R.string.border_top, thirdColumnHeight);
				thirdColumnHeight += imageHeight;
				imageView.setTag(R.string.border_bottom, thirdColumnHeight);
				return thirdColumn;
			} else {
				if (secondColumnHeight <= thirdColumnHeight) {
					imageView.setTag(R.string.border_top, secondColumnHeight);
					secondColumnHeight += imageHeight;
					imageView
							.setTag(R.string.border_bottom, secondColumnHeight);
					return secondColumn;
				}
				imageView.setTag(R.string.border_top, thirdColumnHeight);
				thirdColumnHeight += imageHeight;
				imageView.setTag(R.string.border_bottom, thirdColumnHeight);
				return thirdColumn;
			}
		}

		/**
		 * ��ͼƬ���ص�SD������������
		 * 
		 * @param imageUrl
		 *            ͼƬ��URL��ַ��
		 */
		private void downloadImage(String imageUrl) {
			HttpURLConnection con = null;
			FileOutputStream fos = null;
			BufferedOutputStream bos = null;
			BufferedInputStream bis = null;
			File imageFile = null;
			try {
				URL url = new URL(imageUrl);
				con = (HttpURLConnection) url.openConnection();
				con.setConnectTimeout(50 * 1000);
				con.setReadTimeout(100 * 1000);
				con.setDoInput(true);
				con.setDoOutput(true);
				con.connect();
				
				bis = new BufferedInputStream(con.getInputStream());
				imageFile = new File(getImagePath(imageUrl));
				fos = new FileOutputStream(imageFile);
				bos = new BufferedOutputStream(fos);
				byte[] b = new byte[1024];
				int length;
				while ((length = bis.read(b)) != -1) {
					bos.write(b, 0, length);
					bos.flush();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (bis != null) {
						bis.close();
					}
					if (bos != null) {
						bos.close();
					}
					if (con != null) {
						con.disconnect();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (imageFile != null) {
				Bitmap bitmap = ImageLoader.decodeSampledBitmapFromResource(
						imageFile.getPath(), columnWidth);
				if (bitmap != null) {
					imageLoader.addBitmapToMemoryCache(imageUrl, bitmap);
				}
			}
		}

		/**
		 * ��ȡͼƬ�ı��ش洢·����
		 * 
		 * @param imageUrl
		 *            ͼƬ��URL��ַ��
		 * @return ͼƬ�ı��ش洢·����
		 */
		private String getImagePath(String imageUrl) {
			int lastSlashIndex = imageUrl.lastIndexOf("/");
			String imageName = imageUrl.substring(lastSlashIndex + 1);
			String imageDir = Environment.getExternalStorageDirectory()
					.getPath() + "/PhotoWallFalls/";
			File file = new File(imageDir);
			if (!file.exists()) {
				file.mkdirs();
			}
			String imagePath = imageDir + imageName;
			return imagePath;
		}

	}
}
