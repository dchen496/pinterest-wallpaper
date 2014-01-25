package com.example.quart;

import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import java.util.Timer;
import java.util.TimerTask;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

public class RefreshingWallpaper extends WallpaperService {
	@Override
	public void onCreate() {
		super.onCreate();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}
	
	@Override
	public Engine onCreateEngine() {
		return new RefreshingWallpaperEngine();
	}
	
	class RefreshingWallpaperEngine extends Engine {		
		Timer timer;
		
		RefreshingWallpaperEngine() {
			 setTouchEventsEnabled(true);
		}
		
		class RefreshTask extends TimerTask {
			@Override
			public void run() {
				load();
			}
		}
		
		@Override
		public void onSurfaceChanged(SurfaceHolder sh,
				int format, int w, int h) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new RefreshTask(), 0, 10000);
		}
		
		@Override
		public void onTouchEvent(MotionEvent ev) {
		}
		
		@Override
		public void onVisibilityChanged(boolean visible) {
			if(visible) {
				
			} else {
				
			}
		}
		
		public void load() {
			Log.e("k", "loading");
			Picasso.with(RefreshingWallpaper.this)
			.load("http://quart.herokuapp.com/board_images?user=marialetteboer&slug=stairs-and-storage&no-cache="+Double.toString(Math.random()))
			.into(new CanvasTarget(this));
		}
		
		class CanvasTarget implements Target {
			private RefreshingWallpaperEngine owner;
			
			public CanvasTarget(RefreshingWallpaperEngine _owner) {
				owner = _owner;
			}

			@Override
			public void onBitmapFailed(Drawable d) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onBitmapLoaded(Bitmap b, LoadedFrom l) {
				Log.e("k", "loaded");

				// TODO Auto-generated method stub
				owner.draw(b);
			}

			@Override
			public void onPrepareLoad(Drawable d) {
				// TODO Auto-generated method stub
				
			}
		}
		
		private void draw(Bitmap b) {
			SurfaceHolder sh = getSurfaceHolder();
			Canvas c = null;
			try {
				c = sh.lockCanvas();
				if(c != null) {
					int srcwidth = b.getWidth();
					int srcheight = b.getHeight();
					int dstwidth = c.getWidth();
					int dstheight = c.getHeight();
					int cropwidth = dstwidth;
					int cropheight = dstheight;
					if((double)dstheight / dstwidth * srcwidth > srcheight) {
						cropwidth = (int) ((double)dstheight / srcheight * srcwidth);
					} else if ((double)dstwidth / dstheight * srcheight > srcwidth) {
						cropheight = (int) ((double)dstwidth / srcwidth * srcheight);
					}
					Rect target = new Rect(cropheight + srcheight/2, cropwidth - srcwidth/2,
							cropwidth + srcwidth/2, cropheight - srcheight / 2);

					Bitmap scaled = Bitmap.createScaledBitmap(b, cropwidth, cropheight, true);
					c.drawBitmap(scaled, null, target, new Paint());
				}
			} finally {
				if(c != null)
					sh.unlockCanvasAndPost(c);
			}
		}
	}
}
