package com.example.quart;

import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;
import android.graphics.Canvas;
import android.view.MotionEvent;
import java.util.Timer;
import java.util.TimerTask;

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
				draw();
			}
		}
		
		@Override
		public void onSurfaceChanged(SurfaceHolder sh,
				int format, int w, int h) {
			timer = new Timer();
			timer.scheduleAtFixedRate(new RefreshTask(), 0, 5000);
		}
		
		@Override
		public void onTouchEvent(MotionEvent ev) {
			draw();
		}
		
		@Override
		public void onVisibilityChanged(boolean visible) {
			if(visible) {
				
			} else {
				
			}
		}
		
		private void draw() {
			SurfaceHolder sh = getSurfaceHolder();
			int r = (int) (Math.random() * 255);
			int g = (int) (Math.random() * 255);
			int b = (int) (Math.random() * 255);
			Canvas c = null;
			try {
				c = sh.lockCanvas();
				if(c != null)
					c.drawARGB(255,r,g,b);
			} finally {
				if(c != null)
					sh.unlockCanvasAndPost(c);
			}
		}
	}
}
