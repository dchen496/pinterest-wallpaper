package com.example.quart;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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
	Cache cache;
	int loadFailures = 0;
	int outstandingRequests = 0;
	Timer loadTimer;

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		prefs.registerOnSharedPreferenceChangeListener(this);
		duration = Integer.parseInt((prefs.getString("list_pinterest_frequency", getString(R.string.default_frequency))));
		source = (prefs.getString("edittext_pinterest_board_url", getString(R.string.default_board)));
		cache = new Cache(40);
		loadTimer = new Timer();
		(new CacheLoadTask()).run();
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

	class Cache {
		private ArrayList<Entry> list;
		private int capacity;
		
		public Cache(int _capacity) {
			list = new ArrayList<Entry>();
			capacity = _capacity;
		}
		
		class Entry {
			long timestamp;
			Bitmap bitmap;
		}
		
		public void expire() {
			long mintime = System.currentTimeMillis() - 1000*3600;
			for(int i = list.size() - 1; i >= 0; i--) {
				if(list.get(i).timestamp < mintime) {
					list.remove(i);
				}
			}
		}
		
		public Bitmap getRandom() {
			if(list.size() == 0)
				return null;
			return list.get((int)(Math.random() * list.size())).bitmap; 
		}
		
		public int cap() {
			return capacity;
		}
		
		public void setCap(int _capacity) {
			if(_capacity < 1)
				return;
			capacity = _capacity;
		}
		
		public int size() {
			return list.size();
		}
		
		public boolean add(Bitmap b) {
			if(list.size() >= capacity)
				return false;
			for(int i = 0; i < list.size(); i++) {
				if(list.get(i).bitmap.equals(b)) {
					return false;
				}
			}
			Entry e = new Entry();
			e.bitmap = b;
			e.timestamp = System.currentTimeMillis();
			list.add(e);
			return true;
		}
	}
	// accepted parameters:
	// url=[a pinterest board url] - must be URL escaped
	// id=[a pinterest board id]
	// user=[user name]&slug=[the board's name, in a format like stairs-and-storage]
	// query=[a search query]
	class CacheLoadTask extends TimerTask {
		public void run() {
			cache.expire();
			Log.e("k", "loading");
			Picasso.with(RefreshingWallpaper.this)
			.load("http://quart.herokuapp.com/board_images?" + processSource() + "&no-cache="+Double.toString(Math.random()))
			.into(new CacheTarget());
		}
	}

	class CacheTarget implements Target {
		@Override
		public void onBitmapFailed(final Drawable d) {
			outstandingRequests--;
			Log.e("k", "load failure");
			loadFailures++;
			next();
		}

		@Override
		public void onBitmapLoaded(final Bitmap b, final LoadedFrom l) {
			outstandingRequests--;
			Log.e("k", "loaded");
			if(!cache.add(b))
				cache.setCap(cache.cap()-1);
			loadFailures = 0;
			next();
		}
		
		private void next() {
			for(int i = 0; i < 5; i++) {
				outstandingRequests++;
				Log.i("k", Integer.toString(outstandingRequests) + " " + Integer.toString(cache.cap()) + " " + Integer.toString(cache.size()));
				if(cache.size() + outstandingRequests > cache.cap())
					loadTimer.schedule(new CacheLoadTask(), 900000);
				else
					loadTimer.schedule(new CacheLoadTask(), 500+1000*Math.min(loadFailures,3));
			}
		}

		@Override
		public void onPrepareLoad(final Drawable d) {}
	}


	class RefreshingWallpaperEngine extends Engine {
		Fader fader;

		RefreshingWallpaperEngine() {
			setTouchEventsEnabled(true);
			fader = new Fader();
		}

		class RefreshTask extends TimerTask {
			@Override
			public void run() {
				if(!fader.busy()) {
					Bitmap b = cache.getRandom();
					if(b != null)
						draw(b);
				}
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
		public void onVisibilityChanged(final boolean visible) {
			if(visible) {
				if(timer != null) {
					timer = new Timer();
					timer.scheduleAtFixedRate(new RefreshTask(), 0, duration*1000);
				}
			} else {
				if(timer != null)
					timer.cancel();
				timer = null;
			}
		}

		class Fader {
			private final int frames = 51;
			private final int ms = 30;
			private Bitmap current, prev;
			private Timer fadetimer;
			private int frame;

			class RedrawTask extends TimerTask {
				private Bitmap a, b;

				RedrawTask(final Bitmap _current, final Bitmap _prev) {
					a = _current;
					b = _prev;
				}
				@Override
				public void run() {
					redraw(a, b);
				}
			}

			public void update(final Bitmap b) {
				Log.e("update", b.toString());
				SurfaceHolder sh = getSurfaceHolder();
				if(current == null) {
					Canvas c = null;
					try {
						c = sh.lockCanvas();
						if(c != null)
							c.drawBitmap(b, 0, 0, new Paint());
					} finally {
						if(c != null)
							sh.unlockCanvasAndPost(c);
					}
					current = b;
				} else {
					frame = 0;
					prev = current;
					current = b;
					if(fadetimer != null)
						fadetimer.cancel();
					fadetimer = new Timer();
					fadetimer.scheduleAtFixedRate(new RedrawTask(current, prev), 0, ms);
				}
			}

			private void redraw(final Bitmap current, final Bitmap prev) {
				frame++;

				Paint paint1 = new Paint();
				Paint paint2 = new Paint();

				int alpha = frame * 5;
				if(frame >= frames) {
					fadetimer.cancel();
					fadetimer = null;
					return;
				} else {
					paint1.setAlpha(alpha);
					paint2.setAlpha(255-alpha);
				}

				SurfaceHolder sh = getSurfaceHolder();
				Canvas c = null;
				try {
					c = sh.lockCanvas();
					if(c != null) {
						c.drawBitmap(prev, 0, 0, paint2);
						c.drawBitmap(current, 0, 0, paint1);
					}
				} finally {
					if(c != null)
						sh.unlockCanvasAndPost(c);
				}
			}

			public boolean busy() {
				return fadetimer != null;
			}
		}

		private void draw(final Bitmap b) {
			SurfaceHolder sh = getSurfaceHolder();
			Canvas c = null;
			int srcwidth = b.getWidth();
			int srcheight = b.getHeight();
			int dstwidth = 0, dstheight = 0;
			boolean success = false;
			try {
				c = sh.lockCanvas();
				if(c != null) {
					dstwidth = c.getWidth();
					dstheight = c.getHeight();
					success = true;
				}
			} finally {
				if(c != null)
					sh.unlockCanvasAndPost(c);
			}
			if(!success)
				return;

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
			fader.update(cropped);
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

	@SuppressLint("DefaultLocale")
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
		} else try {
			return "query=" + URLEncoder.encode(source.toLowerCase().trim(), "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			Log.e("k", "help", e);
		}
		return "url=" + getString(R.string.default_board);
	}
}
