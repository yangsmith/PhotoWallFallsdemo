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
	 * 每页要加载的图片数量
	 */
	public static final int PAGE_SIZE = 15;

	/**
	 * 记录当前已加载到第几页
	 */
	private int page;

	/**
	 * 每一列的宽度
	 */
	private int columnWidth;

	/**
	 * 当前第一列的高度
	 */
	private int firstColumnHeight;

	/**
	 * 当前第二列的高度
	 */
	private int secondColumnHeight;

	/**
	 * 当前第三列的高度
	 */
	private int thirdColumnHeight;

	/**
	 * 是否已加载过一次layout，这里onLayout中的初始化只需加载一次
	 */
	private boolean loadOnce = false;

	/**
	 * 对图片进行管理的工具类
	 */
	private ImageLoader imageLoader;

	/**
	 * 第一列的布局
	 */
	private LinearLayout firstColumn;

	/**
	 * 第二列的布局
	 */
	private LinearLayout secondColumn;

	/**
	 * 第三列的布局
	 */
	private LinearLayout thirdColumn;

	/**
	 * 记录所有正在下载或等待下载的任务。
	 */
	private static Set<LoadImageTask> taskCollection;

	/**
	 * MyScrollView下的直接子布局。
	 */
	private static View scrollLayout;

	/**
	 * MyScrollView布局的高度。
	 */
	private static int scrollViewHeight;

	/**
	 * 记录上垂直方向的滚动距离。
	 */
	private static int lastScrollY = -1;

	/**
	 * 记录所有界面上的图片，用以可以随时控制对图片的释放。
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
     * 开始加载下一页的图片，每张图片都会开启一个异步线程去下载。 
     */  
    public void loadMoreImages() {  
        if (hasSDCard()) {  
            int startIndex = page * PAGE_SIZE;  
            int endIndex = page * PAGE_SIZE + PAGE_SIZE;  
            if (startIndex < ImageSet.imageThumUrls.length) {  
                Toast.makeText(getContext(), "正在加载...", Toast.LENGTH_SHORT)  
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
                Toast.makeText(getContext(), "已没有更多图片", Toast.LENGTH_SHORT)  
                        .show();  
            }  
        } else {  
            Toast.makeText(getContext(), "未发现SD卡", Toast.LENGTH_SHORT).show();  
        }  
    }

	/**
	 * 遍历imageViewList中的每张图片，对图片的可见性进行检查，如果图片已经离开屏幕可见范围，则将图片替换成一张空图。
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
	 * 判断手机是否有SD卡。
	 * 
	 * @return 有SD卡返回true，没有返回false。
	 */
	private boolean hasSDCard() {
		return Environment.MEDIA_MOUNTED.equals(Environment
				.getExternalStorageState());
	}

	/**
	 * 异步下载图片的任务。
	 * 
	 */

	class LoadImageTask extends AsyncTask<String, Void, Bitmap> {

		private String mImageUrl;

		/**
		 * 可重复使用的ImageView
		 */
		private ImageView mImageView;

		public LoadImageTask() {
		}

		/**
		 * 将可重复使用的ImageView传入
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
		 * 将图片下载到SD卡缓存起来。
		 * 
		 * @param imageUrl
		 *            图片的URL地址。
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
		 * 获取图片的本地存储路径。
		 * 
		 * @param imageUrl
		 *            图片的URL地址。
		 * @return 图片的本地存储路径。
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
