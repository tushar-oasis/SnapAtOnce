package com.oasis.snapatonce.views;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.oasis.snapatonce.MainActivity;

import java.io.IOException;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
	private SurfaceHolder mHolder;
	private Camera mCamera;
	private Activity mActivity;
	private int mCameraSelected;
	private List<Camera.Size> mAllSizes;
	private Camera.Size mOptimalSize;

	public CameraPreview(Activity activity, Context context, Camera camera, Camera.Size optimalSize) {
		super(context);
		mCamera = camera;
		mActivity = activity;
		mOptimalSize = optimalSize;

		Camera.Parameters parameters = mCamera.getParameters();
		mAllSizes = parameters.getSupportedPreviewSizes();

		// Install a SurfaceHolder.Callback so we get notified when the
		// underlying surface is created and destroyed.
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		mHolder.setFixedSize(mOptimalSize.height, mOptimalSize.width);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		setMeasuredDimension(mOptimalSize.height, mOptimalSize.width);
		Log.d("tushar: ", "In onMeasure() optimal marginHeight: " + mOptimalSize.height + " optimal width: "+ mOptimalSize.width);
	}

	public void surfaceCreated(SurfaceHolder holder) {
		try {
			int orientation = getCameraOrientation(mActivity, mCamera);
			mCamera.setPreviewDisplay(holder);
			mCamera.setDisplayOrientation(orientation);
			mCamera.startPreview();


		} catch (IOException e) {
			Log.d("DG_DEBUG", "Error setting camera preview: " + e.getMessage());
		}

	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (mHolder.getSurface() == null) {
			// preview surface does not exist
			return;
		}

		// stop preview before making changes
		try {
			mCamera.stopPreview();
		} catch (Exception e) {
			// ignore: tried to stop a non-existent preview
		}

		// make any resize, rotate or reformatting changes here

		// start preview with new settings
		try {
			mCamera.setPreviewDisplay(mHolder);
			int orientation = getCameraOrientation(mActivity, mCamera);

			mCamera.setDisplayOrientation(orientation);

			mCamera.startPreview();

		} catch (Exception e) {
			Log.d("DG_DEBUG", "Error starting camera preview: " + e.getMessage());
		}
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		// empty. Take care of releasing the Camera preview in your activity.
	}

	public int getCameraOrientation(Activity activity, Camera camera ){
		Camera.CameraInfo info =
				new Camera.CameraInfo();
		Camera.getCameraInfo(MainActivity.currentSelectedCameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		switch (rotation) {
			case Surface.ROTATION_0: degrees = 0; break;
			case Surface.ROTATION_90: degrees = 90; break;
			case Surface.ROTATION_180: degrees = 180; break;
			case Surface.ROTATION_270: degrees = 270; break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360;  // compensate the mirror
		} else {  // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}

		return result;
	}


}
