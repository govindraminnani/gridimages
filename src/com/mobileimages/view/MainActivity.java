package com.mobileimages.view;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Transformation;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;

class ImageAdapter extends BaseAdapter	{

	private Context context;
	private List<HashMap<String, String>> listOfImages;
	private HashMap<Integer, Bitmap> imagesCache = new HashMap<Integer, Bitmap>();
	
	public ImageAdapter(Context context, List<HashMap<String, String>> listOfImages)	{
		this.context = context;
		this.listOfImages = listOfImages;
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return listOfImages.size();
	}

	@Override
	public HashMap<String, String> getItem(int position) {
		// TODO Auto-generated method stub
		return listOfImages.get(position);
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		// TODO Auto-generated method stub
		View v = convertView;
		if (v == null) {
			v = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
			v.setTag(R.id.image_name, v.findViewById(R.id.image_name));
			v.setTag(R.id.image_desc, v.findViewById(R.id.image_desc));
        }
		
		final HashMap<String, String> item = getItem(position);
		ImageView imageview = (ImageView) v.findViewById(R.id.image_view);
		if(imagesCache.containsKey(position))	{
			imageview.setImageBitmap(imagesCache.get(position));
		} else	{
			new LoadImage(position, imageview).executeOnExecutor(Executors.newCachedThreadPool(), item.get("image_url"));
		}
		
		((TextView) v.getTag(R.id.image_name)).setText(item.get("image_name"));
		((TextView) v.getTag(R.id.image_desc)).setText(item.get("image_desc"));
		
		final ViewFlipper imageFlipper = (ViewFlipper) v.findViewById(R.id.image_flip);
		imageFlipper.setDisplayedChild(Integer.parseInt(item.get("displayed_child")));
		imageFlipper.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				int displayedChild = rotateAnimation(imageFlipper);
				item.put("displayed_child", "" + displayedChild);
				listOfImages.set(position, item);
			}
		});
		
		return v;
	}
	
	private int rotateAnimation(ViewFlipper imageFlipper)	{
		View fromView = imageFlipper.getCurrentView();
		
		final float centerX = fromView.getWidth() / 2.0f, centerY = fromView.getHeight() / 2.0f;
		FlipAnimation outFlip = new FlipAnimation(0, -90, centerX, centerY, 0);
		outFlip.setDuration(300);
		outFlip.setFillAfter(true);
		outFlip.setInterpolator(new AccelerateInterpolator());
		AnimationSet outAnimation = new AnimationSet(true);
		outAnimation.addAnimation(outFlip);		
		imageFlipper.setOutAnimation(outAnimation);
		
		FlipAnimation inFlip = new FlipAnimation(90, 0, centerX, centerY, 1);
		inFlip.setDuration(300);
		inFlip.setFillAfter(true);
		inFlip.setInterpolator(new AccelerateInterpolator());
		inFlip.setStartOffset(300);
		AnimationSet inAnimation = new AnimationSet(true); 
		inAnimation.addAnimation(inFlip); 
		imageFlipper.setInAnimation(inAnimation);
		
		imageFlipper.showNext();
		
		return imageFlipper.getDisplayedChild();
	}
	
	private class FlipAnimation extends Animation	{
		
		private float fromDegrees, toDegrees, centerX, centerY;
		private int scale;
		
		public FlipAnimation(float fromDegrees, float toDegrees, float centerX, float centerY, int scale)	{
			this.fromDegrees = fromDegrees;
			this.toDegrees = toDegrees;
			this.centerX = centerX;
			this.centerY = centerY;
			this.scale = scale;
		}
		
		@Override
		protected void applyTransformation(float interpolatedTime, Transformation t) {
			// TODO Auto-generated method stub
			float degrees = fromDegrees + ((toDegrees - fromDegrees) * interpolatedTime);

			final Camera camera = new Camera();

			final Matrix matrix = t.getMatrix();

			camera.save();
	        camera.rotateY(degrees);

			camera.getMatrix(matrix);
			camera.restore();

			matrix.preTranslate(-centerX, -centerY);
			matrix.postTranslate(centerX, centerY);
			
			float tempScale = (1 - 0.75f) * interpolatedTime;
			switch(scale)	{
				case 0:
					tempScale = 1 - tempScale;					
					break;
				case 1:
					tempScale = 0.75f + tempScale;
					break;
			}
			matrix.preScale(tempScale, tempScale, centerX, centerY);
		}
	}
	
	private class LoadImage extends AsyncTask<String, Void, Bitmap> {

		private int position;
		private ImageView imageView;
		
		public LoadImage(int position, ImageView imageView)	{
			this.position = position;
			this.imageView = imageView;
		}
		
		@Override
		protected Bitmap doInBackground(String... params) {
			// TODO Auto-generated method stub
			Bitmap bmp = null;
			HttpURLConnection connection = null;
			
			try	{
				String url = params[0];
				URL urlObject = new URL(url);
				connection = (HttpURLConnection) urlObject.openConnection();
				connection.connect();
				if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
					Log.e("URL not responded. Getting " + connection.getResponseCode(), url);
				} else	{
					Log.i("Image from URL", url);
					bmp = BitmapFactory.decodeStream(urlObject.openConnection().getInputStream());
				}
			} catch (Exception e)	{
				e.printStackTrace();
			} finally {
				if (connection != null) 
					connection.disconnect();
			}
			
			return bmp;
		}

		@Override
		protected void onPostExecute(Bitmap result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			if(result != null)	{
				imageView.setImageBitmap(result);
				imagesCache.put(position, result);
			}
		}
	}
}


public class MainActivity extends AppCompatActivity {

	private String[][] imagesList = {
		{"http://farm9.staticflickr.com/8616/28010532893_5f1a3f8f3e_n.jpg", "Hiking in GAP", "jaminjan96 posted a photo"},
		{"http://farm9.staticflickr.com/8599/28626826535_ba45082ea6_n.jpg", "DSC_0284", "bainesthomas posted a photo"},
		{"http://farm9.staticflickr.com/8728/28547615621_486b80a5ec_n.jpg", "Kirche", "germancute posted a photo"},
		{"http://farm9.staticflickr.com/8010/28593842736_92d4a690b0_n.jpg", "DSC_0271-001", "bainesthomas posted a photo"},
		{"http://farm9.staticflickr.com/8729/28009504814_4f20d814e5_n.jpg", "DSC_0308-001", "nobody@flickr.com posted a photo"},
		{"http://farm9.staticflickr.com/8575/28342158590_2f49859e38_n.jpg", "Alpine Asphodel", "aniko posted a photo"},
		{"http://farm9.staticflickr.com/8668/28520087132_db7ea2ce1b_n.jpg", "West side", "lorenzoviolone posted a photo"},
		{"http://farm9.staticflickr.com/8789/28342153710_6028e8d2db_n.jpg", "We hiked today!", "North2 posted a photo"},
		{"http://farm9.staticflickr.com/8734/28520047552_a0fc65a940_n.jpg", "Monte Pelmo", "annalisabianchetti posted a photo"},
		{"http://farm9.staticflickr.com/8773/28010170403_4b2bfc2c86_n.jpg", "Fabrikruinen Walim", "germancute posted a photo"}		
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		GridView gridView = (GridView) findViewById(R.id.gridview);
		
		List<HashMap<String, String>> listOfImages = new ArrayList<HashMap<String,String>>();
		for(int i=0; i<imagesList.length; i++)	{
			HashMap<String, String> tempImage = new HashMap<String, String>();			
			tempImage.put("image_url", imagesList[i][0]);
			tempImage.put("image_name", imagesList[i][1]);
			tempImage.put("image_desc", imagesList[i][2]);
			tempImage.put("displayed_child", "0");
			
			listOfImages.add(tempImage);
		}
		
		gridView.setAdapter(new ImageAdapter(MainActivity.this, listOfImages));
	}
}
