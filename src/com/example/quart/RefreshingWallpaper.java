package com.example.quart;

import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.SurfaceHolder;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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
			.load("http://quart.herokuapp.com/board_images?user=marialetteboer&slug=stairs-and-storage")
			.into(new CanvasTarget(this));
		}
		
		class CanvasTarget implements Target {
			private RefreshingWallpaperEngine owner;
			
			public CanvasTarget(RefreshingWallpaperEngine _owner) {
				owner = _owner;
			}
			
			public boolean equals(Object other) {
				if(!(other instanceof CanvasTarget)) {
					return false;
				}
				return ((CanvasTarget) other).owner == owner;
			}
			
			public int hashCode() {
				return owner.hashCode();
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
					c.drawBitmap(b, 0, 0, new Paint());
				}
			} finally {
				if(c != null)
					sh.unlockCanvasAndPost(c);
			}
		}
	}
}
