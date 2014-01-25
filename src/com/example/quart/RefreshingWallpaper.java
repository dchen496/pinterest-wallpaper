package com.example.quart;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Timer;
import java.util.TimerTask;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

public class RefreshingWallpaper extends WallpaperService implements OnSharedPreferenceChangeListener {
	int duration;
	String source;
	Timer timer;
	RefreshingWallpaperEngine engine;
	Bitmap previousFrame;

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		duration = Integer.parseInt((prefs.getString("list_pinterest_frequency", getString(R.string.default_frequency))));
		source = (prefs.getString("edittext_pinterest_board_url", getString(R.string.default_board)));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine() {
		engine = new RefreshingWallpaperEngine();
		return engine;
	}

	class RefreshingWallpaperEngine extends Engine {

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
		public void onSurfaceChanged(final SurfaceHolder sh,
				final int format, final int w, final int h) {
			if (timer != null)
				timer.cancel();
			timer = new Timer();
			timer.scheduleAtFixedRate(new RefreshTask(), 0, duration*1000);
		}

		@Override
		public void onTouchEvent(final MotionEvent ev) {}

		@Override
		public void onVisibilityChanged(final boolean visible) {}

		// accepted parameters:
		// url=[a pinterest board url] - must be URL escaped
		// id=[a pinterest board id]
		// user=[user name]&slug=[the board's name, in a format like stairs-and-storage]
		public void load() {
			Log.e("k", "loading");
			Picasso.with(RefreshingWallpaper.this)
			.load("http://quart.herokuapp.com/board_images?" + processSource() + "&no-cache="+Double.toString(Math.random()))
			.into(new CanvasTarget(this));
		}

		class CanvasTarget implements Target {
			private RefreshingWallpaperEngine owner;

			public CanvasTarget(final RefreshingWallpaperEngine _owner) {
				owner = _owner;
			}

			@Override
			public void onBitmapFailed(final Drawable d) {}

			@Override
			public void onBitmapLoaded(final Bitmap b, final LoadedFrom l) {
				Log.e("k", "loaded");
				owner.draw(b);
			}

			@Override
			public void onPrepareLoad(final Drawable d) {}
		}

		class Fader {
			private final int frames = 51;
			private final int ms = 30;
			private Bitmap begin, end;
			private Timer fadetimer;
			private int frame;
			
			Fader(Bitmap _begin, Bitmap _end) {
				begin = _begin;
				end = _end;
				fadetimer = new Timer();
				fadetimer.scheduleAtFixedRate(new RedrawTask(), 0, ms);
				frame = 0;
			}
			
			class RedrawTask extends TimerTask {
				@Override
				public void run() {
					redraw();
				}
			}
			
			private void redraw() {
				frame++;
				Paint paint = new Paint();
				if(frame >= frames) {
					fadetimer.cancel();
				} else {
					paint.setAlpha(255 / frames);
				}
				
				SurfaceHolder sh = getSurfaceHolder();
				Canvas c = null;
				try {
					c = sh.lockCanvas();
					if(c != null) {
						c.drawBitmap(end, 0, 0, paint);
					}
				} finally {
					if(c != null)
						sh.unlockCanvasAndPost(c);
				}
			}
		}
		
		private void draw(final Bitmap b) {
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
					Log.e("k", Integer.toString(cropwidth));
					Log.e("k", Integer.toString(cropheight));

					Bitmap scaled = Bitmap.createScaledBitmap(b, cropwidth, cropheight, true);
					Bitmap cropped = Bitmap.createBitmap(scaled, 
							(cropwidth - dstwidth)/2, (cropheight - dstheight)/2,
							dstwidth, dstheight);
					new Fader(previousFrame, cropped);
					previousFrame = cropped;
				}
			} finally {
				if(c != null)
					sh.unlockCanvasAndPost(c);
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
		if (key.equals("list_pinterest_frequency")) {
			duration = Integer.parseInt((prefs.getString("list_pinterest_frequency", getString(R.string.default_frequency))));
		} else if (key.equals("edittext_pinterest_board_url")) {
			source = (prefs.getString("edittext_pinterest_board_url", getString(R.string.default_board)));
		}
		engine.onSurfaceChanged(null, 0, 0, 0);
	}

	public String processSource() {
		if (source.trim().startsWith("http")) try {
			return "url=" + URLEncoder.encode(source.trim(), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			Log.e("k", "help", e);
		} else if (source.trim().matches("\\b\\d+\\b")) {
			return "id=" + source.trim();
		} else if (source.contains(",")) {
			int comma = source.indexOf(',');
			return "user=" + source.substring(0, comma).toLowerCase().trim() + "&slug=" + source.substring(comma + 1).toLowerCase().trim();
		}
		return getString(R.string.default_board);
	}
}
