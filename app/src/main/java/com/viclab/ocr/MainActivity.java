package com.viclab.ocr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.agog.mathdisplay.MTMathView;
import com.viclab.ocr.mathpix.MathpixOCR;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

	private static final String TAG = "VIC OCR";

	private LinearLayout loader;
	private LinearLayout canvasHolder;
	private EditText codeEditor;
	private TextView statusView;
	private Button buttonClear;
	private Button buttonOCR;
	private Button buttonReset;
	private Button buttonSubmit;
	private MTMathView mathPreview;
	private TextView mathView;

	private CanvasView canvasView;
	private Paint mPaint;
	private int mColor;
	private final int STROKE_PAINT = 12;
	private final int STROKE_ERASE = 120;

	private HttpConnection httpConnection = HttpConnection.getInstance();
	private String currentLatex = null;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		initializeViews();
		initializeCanvas();
	}

	private void initializeViews() {
		loader = findViewById(R.id.loader);
		canvasHolder = findViewById(R.id.canvas_holder);
		codeEditor = findViewById(R.id.codeEditor);
		statusView = findViewById(R.id.statusView);
		buttonClear = findViewById(R.id.button_clear);
		buttonOCR = findViewById(R.id.button_ocr);
		buttonReset = findViewById(R.id.button_reset);
		buttonSubmit = findViewById(R.id.button_submit);
		mathPreview = findViewById(R.id.mathPreview);
		mathView = findViewById(R.id.mathView);

		buttonClear.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				canvasView.clearCanvas();
			}
		});

		buttonOCR.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				processOCR();
			}
		});

		buttonReset.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				codeEditor.setText(null);
				canvasView.clearCanvas();
				statusView.setText(null);
				statusView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorWhite));
				mathView.setText(null);
				mathPreview.setLatex("");
				statusView.setText(null);
				statusView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorWhite));
				currentLatex = null;
			}
		});

		buttonSubmit.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				submitLatex();
			}
		});
	}

	private void initializeCanvas() {
		canvasView = new CanvasView(this);
		canvasHolder.addView(canvasView);
		mColor = Color.BLACK;
		mPaint = new Paint();
		mPaint.setAntiAlias(true);
		mPaint.setDither(true);
		mPaint.setColor(mColor);
		mPaint.setStyle(Paint.Style.STROKE);
		mPaint.setStrokeJoin(Paint.Join.ROUND);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStrokeWidth(STROKE_PAINT);
	}

	private void processOCR() {
		loader.setVisibility(View.VISIBLE);
		statusView.setText(null);
		statusView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorWhite));
		mathView.setText(null);
		mathPreview.setLatex("");
		currentLatex = null;
		Bitmap bitmap = Bitmap.createBitmap(canvasView.getWidth(), canvasView.getHeight(), Bitmap.Config.ARGB_8888);
		bitmap.eraseColor(Color.WHITE);
		Canvas canvas = new Canvas(bitmap);
		canvasView.draw(canvas);
		callServiceAPI(bitmap);
	}

	@SuppressLint("SetTextI18n")
	private void callServiceAPI(final Bitmap bitmap) {
		try {
			MathpixOCR.UploadParams mathpixParams = new MathpixOCR.UploadParams(bitmap);
			MathpixOCR mathpixOCR = new MathpixOCR(new MathpixOCR.ResultListener() {
				@Override
				public void onError(String message) {
					recognizeFinished(false, message);
				}

				@Override
				public void onSuccess(String latex) {
					recognizeFinished(true, latex);
				}
			});
			mathpixOCR.execute(mathpixParams);
		} catch (Exception e) {
			e.printStackTrace();
			statusView.setText("OCR 에러 : " + e.getMessage());
			loader.setVisibility(View.GONE);
		}
	}

	private void recognizeFinished(boolean success, String result) {
		loader.setVisibility(View.GONE);
		Log.w(TAG, "Recognize Result : " + (success ? "" : "FAIL : ") + result);
		mathView.setText(result);
		currentLatex = result;
		mathPreview.setLatex(result);
		mathPreview.setFontSize(100);
	}

	private void submitLatex() {
		loader.setVisibility(View.VISIBLE);
		if (currentLatex == null) {
			statusView.setText(R.string.message_latex_empty);
			return;
		}
		if (codeEditor.getText() == null || codeEditor.getText().toString().trim().equals("")) {
			statusView.setText(R.string.message_index_empty);
			return;
		}
		new Thread() {
			public void run() {
				httpConnection.requestWebServer(codeEditor.getText().toString(), currentLatex, callback);
			}
		}.start();
	}

	private final Callback callback = new Callback() {
		@Override
		public void onFailure(Call call, IOException e) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					loader.setVisibility(View.GONE);
					Log.d(TAG, "콜백 오류:" + e.getMessage());
					statusView.setText(getString(R.string.message_submit_error) + e.getMessage());
					statusView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorDanger));
				}
			});
		}
		@Override
		public void onResponse(Call call, Response response) throws IOException {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					loader.setVisibility(View.GONE);
					String body = null;
					try {
						body = response.body().string().trim();
					} catch (Exception e) {
						e.printStackTrace();
					}
					Log.d(TAG, "서버에서 응답한 Body:" + body);
					if (response.code() == 200) {
						statusView.setText(getString(R.string.message_submit_result) + body);
						statusView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorSuccess));
					} else {
						statusView.setText(getString(R.string.message_submit_error) + body);
						statusView.setBackgroundColor(ContextCompat.getColor(MainActivity.this, R.color.colorWarning));
					}
				}
			});
		}
	};

	public class CanvasView extends View {

		private Bitmap mBitmap;
		private Canvas mCanvas;
		private Path mPath;
		private Paint mBitmapPaint;
		private Paint mErasePaint;
		private float lastEraseX = 0, lastEraseY = 0;
		private boolean eraseMode;
		private boolean drawMode;

		private float mX, mY;
		private static final float TOUCH_TOLERANCE = 4;

		public CanvasView(Context c) {
			super(c);
			mPath = new Path();
			mBitmapPaint = new Paint(Paint.DITHER_FLAG);
			mErasePaint = new Paint();
			mErasePaint.setColor(Color.GRAY);
			mErasePaint.setStrokeWidth(5);
			mErasePaint.setStyle(Paint.Style.STROKE);
		}

		@Override
		protected void onSizeChanged(int w, int h, int oldw, int oldh) {
			super.onSizeChanged(w, h, oldw, oldh);
			mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			mCanvas = new Canvas(mBitmap);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			canvas.drawColor(Color.TRANSPARENT);
			canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);
			canvas.drawPath(mPath, mPaint);
		}

		public void setModeDraw() {
			eraseMode = false;
			drawMode = true;
			lastEraseX = 0;
			lastEraseY = 0;
			mPaint.setXfermode(null);
			mPaint.setStrokeWidth(STROKE_PAINT);
		}

		public void setModeErase() {
			drawMode = false;
			eraseMode = true;
			mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
			mPaint.setStrokeWidth(STROKE_ERASE);
		}

		private void touch_start(float x, float y) {
			mPath.reset();
			mPath.moveTo(x, y);
			mX = x;
			mY = y;
			if (eraseMode) {
				mCanvas.drawCircle(x, y, STROKE_ERASE / 2, mErasePaint);
				lastEraseX = x;
				lastEraseY = y;
			}
		}

		private void touch_move(float x, float y) {
			if (eraseMode) {
				mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
				mCanvas.drawCircle(lastEraseX, lastEraseY, STROKE_ERASE / 2, mErasePaint);
				mErasePaint.setXfermode(null);
				mCanvas.drawCircle(x, y, STROKE_ERASE / 2, mErasePaint);
				lastEraseX = x;
				lastEraseY = y;
				mPath.lineTo(x, y);
				mCanvas.drawPath(mPath, mPaint);
				mPath.reset();
				mPath.moveTo(x, y);
				mX = x;
				mY = y;
			} else {
				float dx = Math.abs(x - mX);
				float dy = Math.abs(y - mY);
				if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
					mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
					mX = x;
					mY = y;
				}
			}
		}

		private void touch_up(float x, float y) {
			if (eraseMode) {
				mErasePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
				mCanvas.drawCircle(lastEraseX, lastEraseY, STROKE_ERASE / 2, mErasePaint);
				mErasePaint.setXfermode(null);
				lastEraseX = x;
				lastEraseY = y;
			}
			mPath.lineTo(mX, mY);
			mCanvas.drawPath(mPath, mPaint);
			mPath.reset();
		}

		@SuppressLint("ClickableViewAccessibility")
		@Override
		public boolean onTouchEvent(MotionEvent event) {
			float x = event.getX();
			float y = event.getY();
			switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					touch_start(x, y);
					invalidate();
					break;
				case MotionEvent.ACTION_MOVE:
					touch_move(x, y);
					invalidate();
					break;
				case MotionEvent.ACTION_UP:
					touch_up(x, y);
					invalidate();
					break;
			}
			return true;
		}

		public void clearCanvas() {
			if (mCanvas == null) return;
			Paint clearPaint  = new Paint();
			Xfermode xMode = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
			clearPaint.setXfermode(xMode);
			int iCnt = mCanvas.save();
			mCanvas.drawBitmap(mBitmap, 0,0, clearPaint);
			mCanvas.restoreToCount(iCnt);
			invalidate();
		}

	}

}
