package com.alivc.live.pusher.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.alivc.live.pusher.AlivcLivePushConfig;
import com.alivc.live.pusher.AlivcLivePushStatsInfo;
import com.alivc.live.pusher.AlivcLivePusher;
import com.alivc.live.pusher.AlivcPreviewOrientationEnum;
import com.alivc.live.pusher.LogUtil;

import java.util.ArrayList;
import java.util.List;

import static com.alivc.live.pusher.AlivcPreviewOrientationEnum.ORIENTATION_LANDSCAPE_HOME_LEFT;
import static com.alivc.live.pusher.AlivcPreviewOrientationEnum.ORIENTATION_LANDSCAPE_HOME_RIGHT;
import static com.alivc.live.pusher.AlivcPreviewOrientationEnum.ORIENTATION_PORTRAIT;

public class LivePushActivity extends AppCompatActivity{
    private static final String TAG = "LivePushActivity";
    private static final int FLING_MIN_DISTANCE = 50;
    private static final int FLING_MIN_VELOCITY = 0;
    private final long REFRESH_INTERVAL = 1000;
    private static final String URL_KEY = "url_key";
    private static final String ASYNC_KEY = "async_key";
    private static final String AUDIO_ONLY_KEY = "audio_only_key";
    private static final String VIDEO_ONLY_KEY = "video_only_key";
    private static final String ORIENTATION_KEY = "orientation_key";
    private static final String CAMERA_ID = "camera_id";
    private static final String FLASH_ON = "flash_on";
    public static final int REQ_CODE_PUSH = 0x1112;

    public SurfaceView mPreviewView;
    private ViewPager mViewPager;

    private List<Fragment> mFragmentList = new ArrayList<>();
    private FragmentAdapter mFragmentAdapter;

    private GestureDetector mDetector;
    private ScaleGestureDetector mScaleDetector;
    private LivePushFragment mLivePushFragment;
    private PushTextStatsFragment mPushTextStatsFragment;
    private PushDiagramStatsFragment mPushDiagramStatsFragment;
    private AlivcLivePushConfig mAlivcLivePushConfig;

    private AlivcLivePusher mAlivcLivePusher = null;
    private String mPushUrl = null;

    private boolean mAsync = false;
    private boolean mAudioOnly = false;
    private boolean mVideoOnly = false;
    private int mOrientation = ORIENTATION_PORTRAIT.ordinal();

    private SurfaceStatus mSurfaceStatus = SurfaceStatus.UNINITED;
//    private Handler mHandler = new Handler();
    private boolean isPause = false;

    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private boolean mFlash = false;
    AlivcLivePushStatsInfo alivcLivePushStatsInfo = null;

//    private ConnectivityChangedReceiver mChangedReceiver = new ConnectivityChangedReceiver();

    private int mNetWork = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        mPushUrl = getIntent().getStringExtra(URL_KEY);
        mAsync = getIntent().getBooleanExtra(ASYNC_KEY, false);
        mAudioOnly = getIntent().getBooleanExtra(AUDIO_ONLY_KEY, false);
        mVideoOnly = getIntent().getBooleanExtra(VIDEO_ONLY_KEY, false);
        mOrientation = getIntent().getIntExtra(ORIENTATION_KEY, ORIENTATION_PORTRAIT.ordinal());
        mCameraId = getIntent().getIntExtra(CAMERA_ID, Camera.CameraInfo.CAMERA_FACING_FRONT);
        mFlash = getIntent().getBooleanExtra(FLASH_ON, false);
        setOrientation(mOrientation);
        setContentView(R.layout.activity_push);
        initView();
        mAlivcLivePushConfig = (AlivcLivePushConfig) getIntent().getSerializableExtra(AlivcLivePushConfig.Config);
        mAlivcLivePusher = new AlivcLivePusher();

        try {
            mAlivcLivePusher.init(getApplicationContext(),mAlivcLivePushConfig);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            showDialog(this, e.getMessage());
        } catch (IllegalStateException e) {
            e.printStackTrace();
            showDialog(this, e.getMessage());
        }

        mLivePushFragment = new LivePushFragment().newInstance(mPushUrl, mAsync, mAudioOnly, mVideoOnly, mCameraId, mFlash, mAlivcLivePushConfig.getQualityMode().getQualityMode());
        mLivePushFragment.setAlivcLivePusher(mAlivcLivePusher);
        mLivePushFragment.setStateListener(mStateListener);
        mPushTextStatsFragment = new PushTextStatsFragment();
        mPushDiagramStatsFragment = new PushDiagramStatsFragment();

        initViewPager();
        mScaleDetector = new ScaleGestureDetector(getApplicationContext(), mScaleGestureDetector);
        mDetector = new GestureDetector(getApplicationContext(), mGestureDetector);
        mNetWork = NetWorkUtils.getAPNType(this);
//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
//        registerReceiver(mChangedReceiver, filter);
    }

    public void initView() {
        mPreviewView = (SurfaceView) findViewById(R.id.preview_view);
        mPreviewView.getHolder().addCallback(mCallback);
    }

    private void initViewPager() {
        mViewPager = (ViewPager) findViewById(R.id.tv_pager);
//        mFragmentList.add(mPushTextStatsFragment);
        mFragmentList.add(mLivePushFragment);
//        mFragmentList.add(mPushDiagramStatsFragment);
        mFragmentAdapter = new FragmentAdapter(this.getSupportFragmentManager(), mFragmentList) ;
        mViewPager.setAdapter(mFragmentAdapter);
//        mViewPager.setCurrentItem(1);
//        mViewPager.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View view, MotionEvent motionEvent) {
//                if(((ViewPager)view).getCurrentItem() == 1) {
//                    if (motionEvent.getPointerCount() >= 2) {
//                        mScaleDetector.onTouchEvent(motionEvent);
//                    } else if (motionEvent.getPointerCount() == 1) {
//                        mDetector.onTouchEvent(motionEvent);
//                    }
//                }
//                return false;
//            }
//        });

//        mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//
//            }
//
//            @Override
//            public void onPageSelected(final int arg0) {
//                if(arg0 == 1) {
//                    mHandler.removeCallbacks(mRunnable);
//                } else {
//                    mHandler.post(mRunnable);
//                }
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int state) {
//
//            }
//        });
    }

    private void setOrientation(int orientation) {
        if(orientation == ORIENTATION_PORTRAIT.ordinal()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if(orientation == ORIENTATION_LANDSCAPE_HOME_RIGHT.ordinal()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if(orientation == ORIENTATION_LANDSCAPE_HOME_LEFT.ordinal()) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
        }
    }

    private GestureDetector.OnGestureListener mGestureDetector = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onSingleTapUp(MotionEvent motionEvent) {
            if (mPreviewView.getWidth() > 0 && mPreviewView.getHeight() > 0) {
                float x = motionEvent.getX() / mPreviewView.getWidth();
                float y = motionEvent.getY() / mPreviewView.getHeight();
                try{
                    mAlivcLivePusher.focusCameraAtAdjustedPoint(x, y, true);
                } catch (IllegalStateException e) {

                }
            }
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent motionEvent) {

        }

        @Override
        public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
            if(motionEvent == null || motionEvent1 == null) {
                return false;
            }
            if (motionEvent.getX() - motionEvent1.getX() > FLING_MIN_DISTANCE
                    && Math.abs(v) > FLING_MIN_VELOCITY) {
                // Fling left
            } else if (motionEvent1.getX() - motionEvent.getX() > FLING_MIN_DISTANCE
                    && Math.abs(v) > FLING_MIN_VELOCITY) {
                // Fling right
            }
            return false;
        }
    };

    private float scaleFactor = 1.0f;
    private ScaleGestureDetector.OnScaleGestureListener mScaleGestureDetector = new ScaleGestureDetector.OnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
            if(scaleGestureDetector.getScaleFactor() > 1) {
                scaleFactor += 0.5;
            } else {
                scaleFactor -= 2;
            }
            if(scaleFactor <= 1) scaleFactor = 1;
            try{
                if(scaleFactor >= mAlivcLivePusher.getMaxZoom()) scaleFactor = mAlivcLivePusher.getMaxZoom();
                mAlivcLivePusher.setZoom((int)scaleFactor);

            } catch (IllegalStateException e) {

            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {

        }
    };

    SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            if(mSurfaceStatus == SurfaceStatus.UNINITED) {
                mSurfaceStatus = SurfaceStatus.CREATED;
                if(mAlivcLivePusher != null) {
                    try {
                        if(mAsync) {
                            mAlivcLivePusher.startPreviewAysnc(mPreviewView);
                        } else {
                            mAlivcLivePusher.startPreview(mPreviewView);
                        }
                    } catch (IllegalArgumentException e) {
                        e.toString();
                    } catch (IllegalStateException e) {
                        e.toString();
                    }
                }
            } else if(mSurfaceStatus == SurfaceStatus.DESTROYED) {
                mSurfaceStatus = SurfaceStatus.RECREATED;
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            mSurfaceStatus = SurfaceStatus.CHANGED;
            if(mLivePushFragment != null) {
                mLivePushFragment.setSurfaceView(mPreviewView);
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            mSurfaceStatus = SurfaceStatus.DESTROYED;
        }
    };
    public static void startActivity(Activity activity, AlivcLivePushConfig alivcLivePushConfig, String url, boolean async, boolean audioOnly, boolean videoOnly, AlivcPreviewOrientationEnum orientation, int cameraId, boolean isFlash) {
        Intent intent = new Intent(activity, LivePushActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable(AlivcLivePushConfig.Config, alivcLivePushConfig);
        bundle.putString(URL_KEY, url);
        bundle.putBoolean(ASYNC_KEY, async);
        bundle.putBoolean(AUDIO_ONLY_KEY, audioOnly);
        bundle.putBoolean(VIDEO_ONLY_KEY, videoOnly);
        bundle.putInt(ORIENTATION_KEY, orientation.ordinal());
        bundle.putInt(CAMERA_ID, cameraId);
        bundle.putBoolean(FLASH_ON, isFlash);
        intent.putExtras(bundle);
        activity.startActivityForResult(intent, REQ_CODE_PUSH);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mAlivcLivePusher != null) {
            try {
                if(!isPause) {
                    if(mAsync) {
                        mAlivcLivePusher.resumeAsync();
                    } else {
                        mAlivcLivePusher.resume();
                    }
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
//        if(mViewPager.getCurrentItem() != 1) {
//            mHandler.post(mRunnable);
//        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAlivcLivePusher != null) {
            try {
                if(mAlivcLivePusher != null/*.isPushing()*/) {
                    mAlivcLivePusher.pause();
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
//        if(mHandler != null) {
//            mHandler.removeCallbacks(mRunnable);
//        }
    }

    @Override
    protected void onDestroy() {
        if(mAlivcLivePusher != null) {
            try {
                mAlivcLivePusher.destroy();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
//        if(mHandler != null) {
//            mHandler.removeCallbacks(mRunnable);
//            mHandler = null;
//        }
//        unregisterReceiver(mChangedReceiver);
        mFragmentList = null;
        mPreviewView = null;
        mViewPager = null;
        mFragmentAdapter = null;
        mDetector = null;
        mScaleDetector = null;
        mLivePushFragment = null;
        mPushTextStatsFragment = null;
        mPushDiagramStatsFragment = null;
        mAlivcLivePushConfig = null;

        mAlivcLivePusher = null;

//        mHandler = null;
        alivcLivePushStatsInfo = null;
        super.onDestroy();
    }

    public class FragmentAdapter extends FragmentPagerAdapter {

        List<Fragment> fragmentList = new ArrayList<>();
        public FragmentAdapter(FragmentManager fm, List<Fragment> fragmentList) {
            super(fm);
            this.fragmentList = fragmentList;
        }

        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        @Override
        public int getCount() {
            return fragmentList.size();
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        AlivcPreviewOrientationEnum orientationEnum;
        if(mAlivcLivePusher != null) {
            switch (rotation) {
                case Surface.ROTATION_0:
                    orientationEnum = ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                    orientationEnum = ORIENTATION_LANDSCAPE_HOME_RIGHT;
                    break;
                case Surface.ROTATION_270:
                    orientationEnum = ORIENTATION_LANDSCAPE_HOME_LEFT;
                    break;
                default:
                    orientationEnum = ORIENTATION_PORTRAIT;
                    break;
            }
            mAlivcLivePusher.setPreviewOrientation(orientationEnum);
        }
    }

    public AlivcLivePusher getLivePusher() {
        return this.mAlivcLivePusher;
    }

    public SurfaceView getPreviewView() {
        return this.mPreviewView;
    }

    private void showDialog(Context context, String message) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle(getString(R.string.dialog_title));
        dialog.setMessage(message);
        dialog.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        dialog.show();
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            LogUtil.d(TAG, "====== mRunnable run ======");

            new AsyncTask<AlivcLivePushStatsInfo, Void, AlivcLivePushStatsInfo>() {
                @Override
                protected AlivcLivePushStatsInfo doInBackground(AlivcLivePushStatsInfo... alivcLivePushStatsInfos) {
                    try {
                        alivcLivePushStatsInfo = mAlivcLivePusher.getLivePushStatsInfo();
                    } catch (IllegalStateException e) {

                    }
                    return alivcLivePushStatsInfo;
                }

                @Override
                protected void onPostExecute(AlivcLivePushStatsInfo alivcLivePushStatsInfo) {
                    super.onPostExecute(alivcLivePushStatsInfo);
                    if(mPushTextStatsFragment != null && mViewPager.getCurrentItem() == 0) {
                        mPushTextStatsFragment.updateValue(alivcLivePushStatsInfo);
                    } else if (mPushDiagramStatsFragment != null && mViewPager.getCurrentItem() == 2) {
                        mPushDiagramStatsFragment.updateValue(alivcLivePushStatsInfo);
                    }
//                    mHandler.postDelayed(mRunnable, REFRESH_INTERVAL);
                }
            }.execute();
        }
    };

    public interface PauseState {
        void updatePause(boolean state);
    }

    private PauseState mStateListener = new PauseState() {
        @Override
        public void updatePause(boolean state) {
            isPause = state;
        }
    };

    class ConnectivityChangedReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                if(mNetWork != NetWorkUtils.getAPNType(context)) {
                    mNetWork = NetWorkUtils.getAPNType(context);
                    if(mAlivcLivePusher != null) {
                        if(mAlivcLivePusher.isPushing()) {
                            try {
                                mAlivcLivePusher.reconnectPushAsync();
                            } catch (IllegalStateException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

        }
    }

}
