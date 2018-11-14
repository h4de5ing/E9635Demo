package com.android.cvbsdemo;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private TextureView[] mPreviewList = {null, null, null, null};
    private Camera[] mCameraList = {null, null, null, null};
    private int[] mViewID = {R.id.camera_preview0, R.id.camera_preview1, R.id.camera_preview2, R.id.camera_preview3};
    private int mNumberOfCameras = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera_layout);
        super.onCreate(savedInstanceState);
        Log.d("llx", "onCreate");

        mNumberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < mNumberOfCameras && i < mPreviewList.length; i++) {
            mPreviewList[i] = (TextureView) findViewById(mViewID[i]);
        }

        if (mNumberOfCameras < 3) {
            findViewById(R.id.camera_group1).setVisibility(View.GONE);
            if (mNumberOfCameras < 2) {
                findViewById(R.id.camera_preview1).setVisibility(View.GONE);
            }
        } else if (CommonUtils.is7Inch()) {
            findViewById(R.id.camera_group0).setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("llx", "onResume");
        for (int i = 0; i < mNumberOfCameras && i < mPreviewList.length; i++) {
            try {
                if (mCameraList[i] == null) {
                    mCameraList[i] = Camera.open(mNumberOfCameras - i - 1);
                }
            } catch (Exception e) {
                Toast.makeText(this, "打开摄像头出错" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            mPreviewList[i].setVisibility(View.VISIBLE);
        }

        for (int i = 0; i < mPreviewList.length; i++) {
            if (mPreviewList[i] != null) {
                mPreviewList[i].setSurfaceTextureListener(textureListener[i]);
            }
        }
    }

    @Override
    protected void onPause() {
        for (int i = 0; i < mCameraList.length; i++) {
            if (mCameraList[i] != null) {
                mCameraList[i].stopPreview();
            }
            mPreviewList[i].setVisibility(View.GONE);
        }
        Log.d("llx", "onPause");
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        for (int i = 0; i < mCameraList.length; i++) {
            if (mCameraList[i] != null) {
                mCameraList[i].release();
                mCameraList[i] = null;
            }
        }
        Log.d("llx", "onDestroy");
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d("llx", "onBackPressed");
        for (int i = 0; i < mCameraList.length; i++) {
            if (mCameraList[i] != null) {
                mCameraList[i].stopPreview();
            }
        }
        finish();
    }


    SurfaceTextureListener[] textureListener = {
            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    if (mCameraList[0] != null) {
                        try {
                            mCameraList[0].startPreview();
                            mCameraList[0].setPreviewTexture(surface);
                        } catch (Exception e) {
                            Log.d("llx", e.toString());
                        }
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    try {
                        if (mCameraList[0] != null) {
                            mCameraList[0].stopPreview();
                        }
                    } catch (Exception e) {
                        Log.d("llx", e.toString());
                    }
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            },

            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    if (mCameraList[1] != null) {
                        try {
                            mCameraList[1].startPreview();
                            mCameraList[1].setPreviewTexture(surface);
                        } catch (Exception e) {
                            Log.d("llx", e.toString());
                        }
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    try {
                        if (mCameraList[1] != null) {
                            mCameraList[1].stopPreview();
                        }
                    } catch (Exception e) {
                        Log.d("llx", e.toString());
                    }
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            },

            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    if (mCameraList[2] != null) {
                        try {
                            mCameraList[2].startPreview();
                            mCameraList[2].setPreviewTexture(surface);
                        } catch (Exception e) {
                            Log.d("llx", e.toString());
                        }
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    try {
                        if (mCameraList[2] != null) {
                            mCameraList[2].stopPreview();
                        }
                    } catch (Exception e) {
                        Log.d("llx", e.toString());
                    }
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            },

            new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    if (mCameraList[3] != null) {
                        try {
                            mCameraList[3].startPreview();
                            mCameraList[3].setPreviewTexture(surface);
                        } catch (Exception e) {
                            Log.d("llx", e.toString());
                        }
                    }
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    try {
                        if (mCameraList[3] != null) {
                            mCameraList[3].stopPreview();
                        }
                    } catch (Exception e) {
                        Log.d("llx", e.toString());
                    }
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            }
    };

}
