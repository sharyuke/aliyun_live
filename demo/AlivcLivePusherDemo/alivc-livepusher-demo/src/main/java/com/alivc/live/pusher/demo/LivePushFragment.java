package com.alivc.live.pusher.demo;

import android.content.DialogInterface;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.alivc.live.pusher.AlivcLivePushBGMListener;
import com.alivc.live.pusher.AlivcLivePushError;
import com.alivc.live.pusher.AlivcLivePushErrorListener;
import com.alivc.live.pusher.AlivcLivePushInfoListener;
import com.alivc.live.pusher.AlivcLivePushNetworkListener;
import com.alivc.live.pusher.AlivcLivePusher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.alivc.live.pusher.AlivcLivePushCameraTypeEnum.CAMERA_TYPE_BACK;
import static com.alivc.live.pusher.AlivcLivePushCameraTypeEnum.CAMERA_TYPE_FRONT;

public class LivePushFragment extends Fragment implements Runnable{
    public static final String TAG = "LivePushFragment";

    private static final String URL_KEY = "url_key";
    private static final String ASYNC_KEY= "async_key";
    private static final String AUDIO_ONLY_KEY = "audio_only_key";
    private static final String VIDEO_ONLY_KEY = "video_only_key";
    private static final String QUALITY_MODE_KEY = "quality_mode_key";
    private static final String CAMERA_ID = "camera_id";
    private static final String FLASH_ON = "flash_on";
    private final long REFRESH_INTERVAL = 2000;

    private ImageView mExit;
    private ImageView mMusic;
    private ImageView mFlash;
    private ImageView mCamera;
    private ImageView mBeautyButton;
    private LinearLayout mTopBar;
    private TextView mUrl;
    private TextView mIsPushing;
    private LinearLayout mGuide;

    private Button mPreviewButton;
    private Button mPushButton;
    private Button mOperaButton;
    private Button mMore;
    private Button mRestartButton;
    private AlivcLivePusher mAlivcLivePusher = null;
    private String mPushUrl = null;
    private SurfaceView mSurfaceView = null;
    private boolean mAsync = false;

    private boolean mAudio = false;
    private boolean mVideoOnly = false;
    private boolean isPushing = false;
    private Handler mHandler = new Handler();

    private LivePushActivity.PauseState mStateListener = null;
    private int mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private boolean isFlash = false;

    private boolean flashState = true;

    private int mQualityMode = 0;

    private ExecutorService mSingleThreadPool = Executors.newSingleThreadExecutor();

    private MusicDialog mMusicDialog = null;

    public static LivePushFragment newInstance(String url, boolean async, boolean mAudio, boolean mVideoOnly ,int cameraId, boolean isFlash, int mode) {
        LivePushFragment livePushFragment = new LivePushFragment();
        Bundle bundle = new Bundle();
        bundle.putString(URL_KEY, url);
        bundle.putBoolean(ASYNC_KEY, async);
        bundle.putBoolean(AUDIO_ONLY_KEY, mAudio);
        bundle.putBoolean(VIDEO_ONLY_KEY, mVideoOnly);
        bundle.putInt(QUALITY_MODE_KEY,mode);
        bundle.putInt(CAMERA_ID, cameraId);
        bundle.putBoolean(FLASH_ON, isFlash);
        livePushFragment.setArguments(bundle);
        return livePushFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null) {
            mPushUrl = getArguments().getString(URL_KEY);
            mAsync = getArguments().getBoolean(ASYNC_KEY, false);
            mAudio = getArguments().getBoolean(AUDIO_ONLY_KEY, false);
            mVideoOnly = getArguments().getBoolean(VIDEO_ONLY_KEY, false);
            mCameraId = getArguments().getInt(CAMERA_ID);
            isFlash = getArguments().getBoolean(FLASH_ON, false);
            mQualityMode = getArguments().getInt(QUALITY_MODE_KEY);
            flashState = isFlash;
        }
        if(mAlivcLivePusher != null) {
            mAlivcLivePusher.setLivePushInfoListener(mPushInfoListener);
            mAlivcLivePusher.setLivePushErrorListener(mPushErrorListener);
            mAlivcLivePusher.setLivePushNetworkListener(mPushNetworkListener);
            mAlivcLivePusher.setLivePushBGMListener(mPushBGMListener);
            isPushing = mAlivcLivePusher.isPushing();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.push_fragment,container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mExit = (ImageView) view.findViewById(R.id.exit);
        mMusic = (ImageView) view.findViewById(R.id.music);
        mFlash = (ImageView) view.findViewById(R.id.flash);
        mFlash.setSelected(isFlash);
        mCamera = (ImageView) view.findViewById(R.id.camera);
        mCamera.setSelected(true);
        mPreviewButton = (Button) view.findViewById(R.id.preview_button);
        mPreviewButton.setSelected(false);
        mPushButton = (Button) view.findViewById(R.id.push_button);
        mPushButton.setSelected(true);
        mOperaButton = (Button) view.findViewById(R.id.opera_button);
        mOperaButton.setSelected(false);
        mMore = (Button) view.findViewById(R.id.more);
        mBeautyButton = (ImageView) view.findViewById(R.id.beauty_button);
        mBeautyButton.setSelected(SharedPreferenceUtils.isBeautyOn(getActivity().getApplicationContext()));
        mRestartButton = (Button) view.findViewById(R.id.restart_button);
        mTopBar = (LinearLayout) view.findViewById(R.id.top_bar);
        mUrl = (TextView) view.findViewById(R.id.push_url);
        mUrl.setText(mPushUrl);
        mIsPushing = (TextView) view.findViewById(R.id.isPushing);
        mIsPushing.setText(String.valueOf(isPushing));
        mGuide = (LinearLayout) view.findViewById(R.id.guide);
        mExit.setOnClickListener(onClickListener);
        mMusic.setOnClickListener(onClickListener);
        mFlash.setOnClickListener(onClickListener);
        mCamera.setOnClickListener(onClickListener);
        mPreviewButton.setOnClickListener(onClickListener);
        mPushButton.setOnClickListener(onClickListener);
        mOperaButton.setOnClickListener(onClickListener);
        mBeautyButton.setOnClickListener(onClickListener);
        mRestartButton.setOnClickListener(onClickListener);
        mMore.setOnClickListener(onClickListener);
        if(SharedPreferenceUtils.isGuide(getActivity().getApplicationContext())) {
            mGuide.setVisibility(View.VISIBLE);
            mGuide.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    if(mGuide != null) {
                        mGuide.setVisibility(View.GONE);
                        SharedPreferenceUtils.setGuide(getActivity().getApplicationContext(), false);
                    }
                    return false;
                }
            });
        }

        if(mVideoOnly)
        {
            mMusic.setVisibility(View.GONE);
        }
        mMore.setVisibility(mAudio ? View.GONE : View.VISIBLE);
//        mTopBar.setVisibility(mAudio ? View.GONE : View.VISIBLE);
        mBeautyButton.setVisibility(mAudio ? View.GONE : View.VISIBLE);
        mFlash.setVisibility(mAudio ? View.GONE : View.VISIBLE);
        mCamera.setVisibility(mAudio ? View.GONE : View.VISIBLE);
        mFlash.setClickable(mCameraId == CAMERA_TYPE_FRONT.getCameraId() ? false : true);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final int id = view.getId();

            if(mAlivcLivePusher == null) {
                if(getActivity() != null) {
                    mAlivcLivePusher = ((LivePushActivity)getActivity()).getLivePusher();
                }

                if(mAlivcLivePusher == null) {
                    return;
                }
            }

            mSingleThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        switch (id) {
                            case R.id.exit:
                                getActivity().finish();
                                break;
                            case R.id.music:
                                if(mMusicDialog == null) {
                                    mMusicDialog = MusicDialog.newInstance();
                                    mMusicDialog.setAlivcLivePusher(mAlivcLivePusher);
                                }
                                mMusicDialog.show(getFragmentManager(), "beautyDialog");

                                break;
                            case R.id.flash:
                                mAlivcLivePusher.setFlash(!mFlash.isSelected());
                                flashState = !mFlash.isSelected();
                                mFlash.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mFlash.setSelected(!mFlash.isSelected());
                                    }
                                });
                                break;
                            case R.id.camera:
                                if(mCameraId == CAMERA_TYPE_FRONT.getCameraId()) {
                                    mCameraId = CAMERA_TYPE_BACK.getCameraId();
                                } else {
                                    mCameraId = CAMERA_TYPE_FRONT.getCameraId();
                                }
                                mAlivcLivePusher.switchCamera();
                                mFlash.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mFlash.setClickable(mCameraId == CAMERA_TYPE_FRONT.getCameraId() ? false : true);
                                        if(mCameraId == CAMERA_TYPE_FRONT.getCameraId()) {
                                            mFlash.setSelected(false);
                                        } else {
                                            mFlash.setSelected(flashState);
                                        }
                                    }
                                });

                                break;
                            case R.id.preview_button:
                                final boolean isPreview = mPreviewButton.isSelected();
                                if(mSurfaceView == null && getActivity() != null) {
                                    mSurfaceView = ((LivePushActivity)getActivity()).getPreviewView();
                                }
                                if(!isPreview) {
                                    mAlivcLivePusher.stopPreview();
                                } else {
                                    if(mAsync) {
                                        mAlivcLivePusher.startPreviewAysnc(mSurfaceView);
                                    } else {
                                        mAlivcLivePusher.startPreview(mSurfaceView);
                                    }
                                }

                                mPreviewButton.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPreviewButton.setText(isPreview ? getString(R.string.stop_preview_button) : getString(R.string.start_preview_button));
                                        mPreviewButton.setSelected(!isPreview);
                                    }
                                });

                                break;
                            case R.id.push_button:
                                final boolean isPush = mPushButton.isSelected();
                                if(isPush) {
                                    if(mAsync) {
                                        mAlivcLivePusher.startPushAysnc(mPushUrl);
                                    } else {
                                        mAlivcLivePusher.startPush(mPushUrl);
                                    }
                                } else {
                                    mAlivcLivePusher.stopPush();
                                    mOperaButton.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mOperaButton.setText(getString(R.string.pause_button));
                                            mOperaButton.setSelected(false);
                                        }
                                    });
                                }

                                mPushButton.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mPushButton.setText(isPush ? getString(R.string.stop_button) : getString(R.string.start_button));
                                        mPushButton.setSelected(!isPush);
                                    }
                                });

                                break;
                            case R.id.opera_button:
                                final boolean isPause = mOperaButton.isSelected();
                                if(!isPause) {
                                    mAlivcLivePusher.pause();
                                } else {
                                    if(mAsync) {
                                        mAlivcLivePusher.resumeAsync();
                                    } else {
                                        mAlivcLivePusher.resume();
                                    }
                                }

                                if(mStateListener != null) {
                                    mStateListener.updatePause(!isPause);
                                }
                                mOperaButton.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mOperaButton.setText(isPause ? getString(R.string.pause_button) : getString(R.string.resume_button));
                                        mOperaButton.setSelected(!isPause);
                                    }
                                });

                                break;
                            case R.id.beauty_button:
                                PushBeautyDialog pushBeautyDialog = PushBeautyDialog.newInstance(mBeautyButton.isSelected());
                                pushBeautyDialog.setAlivcLivePusher(mAlivcLivePusher);
                                pushBeautyDialog.setBeautyListener(mBeautyListener);
                                pushBeautyDialog.show(getFragmentManager(), "beautyDialog");
                                break;
                            case R.id.restart_button:
                                if(mAsync) {
                                    mAlivcLivePusher.restartPushAync();
                                } else {
                                    mAlivcLivePusher.restartPush();
                                }
                                break;
                            case R.id.more:
                                PushMoreDialog pushMoreDialog = new PushMoreDialog();
                                pushMoreDialog.setAlivcLivePusher(mAlivcLivePusher);
                                pushMoreDialog.setQualityMode(mQualityMode);
                                pushMoreDialog.setPushUrl(mPushUrl);
                                pushMoreDialog.show(getFragmentManager(), "moreDialog");
                                break;
                        }
                    } catch (IllegalArgumentException e) {
                        showDialog(e.getMessage());
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        showDialog(e.getMessage());
                        e.printStackTrace();
                    }
                }
            });

        }
    };


    public void setAlivcLivePusher(AlivcLivePusher alivcLivePusher) {
        this.mAlivcLivePusher = alivcLivePusher;
    }

    public void setStateListener(LivePushActivity.PauseState listener) {
        this.mStateListener = listener;
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        this.mSurfaceView = surfaceView;
    }


    AlivcLivePushInfoListener mPushInfoListener = new AlivcLivePushInfoListener() {
        @Override
        public void onPreviewStarted(AlivcLivePusher pusher) {
            showToast(getString(R.string.start_preview));
        }

        @Override
        public void onPreviewStoped(AlivcLivePusher pusher) {
            showToast(getString(R.string.stop_preview));
        }

        @Override
        public void onPushStarted(AlivcLivePusher pusher) {
            showToast(getString(R.string.start_push));
        }

        @Override
        public void onPushPauesed(AlivcLivePusher pusher) {
            showToast(getString(R.string.pause_push));
        }

        @Override
        public void onPushResumed(AlivcLivePusher pusher) {
            showToast(getString(R.string.resume_push));
        }

        @Override
        public void onPushStoped(AlivcLivePusher pusher) {
            showToast(getString(R.string.stop_push));
        }

        /**
         * 推流重启通知
         *
         * @param pusher AlivcLivePusher实例
         */
        @Override
        public void onPushRestarted(AlivcLivePusher pusher) {
            showToast(getString(R.string.restart_success));
        }

        @Override
        public void onFirstFramePreviewed(AlivcLivePusher pusher) {
            showToast(getString(R.string.first_frame));
        }

        @Override
        public void onDropFrame(AlivcLivePusher pusher, int countBef, int countAft) {
            showToast(getString(R.string.drop_frame) + ", 丢帧前："+countBef+", 丢帧后："+countAft);
        }

        @Override
        public void onAdjustBitRate(AlivcLivePusher pusher, int curBr, int targetBr) {
            showToast(getString(R.string.adjust_bitrate) + ", 当前码率："+curBr+"Kps, 目标码率："+targetBr+"Kps");
        }

        @Override
        public void onAdjustFps(AlivcLivePusher pusher, int curFps, int targetFps) {
            showToast(getString(R.string.adjust_fps) + ", 当前帧率："+curFps+", 目标帧率："+targetFps);
        }
    };

    AlivcLivePushErrorListener mPushErrorListener = new AlivcLivePushErrorListener() {

        @Override
        public void onSystemError(AlivcLivePusher livePusher, AlivcLivePushError error) {
            showDialog(getString(R.string.system_error) +  error.toString());
        }

        @Override
        public void onSDKError(AlivcLivePusher livePusher, AlivcLivePushError error) {
            if(error != null) {
                showDialog(getString(R.string.sdk_error) + error.toString());
            }
        }
    };

    AlivcLivePushNetworkListener mPushNetworkListener = new AlivcLivePushNetworkListener() {
        @Override
        public void onNetworkPoor(AlivcLivePusher pusher) {
            showNetWorkDialog(getString(R.string.network_poor));
        }

        @Override
        public void onNetworkRecovery(AlivcLivePusher pusher) {
            showToast(getString(R.string.network_recovery));
        }

        @Override
        public void onReconnectStart(AlivcLivePusher pusher) {

            showToastShort(getString(R.string.reconnect_start));
        }

        @Override
        public void onReconnectFail(AlivcLivePusher pusher) {

            showDialog(getString(R.string.reconnect_fail));
        }

        @Override
        public void onReconnectSucceed(AlivcLivePusher pusher) {
            showToast(getString(R.string.reconnect_success));
        }

        @Override
        public void onSendDataTimeout(AlivcLivePusher pusher) {
            showDialog(getString(R.string.senddata_timeout));
        }

        @Override
        public void onConnectFail(AlivcLivePusher pusher) {
            showDialog(getString(R.string.connect_fail));
        }
    };

    private AlivcLivePushBGMListener mPushBGMListener = new AlivcLivePushBGMListener() {
        @Override
        public void onStarted() {

        }

        @Override
        public void onStoped() {

        }

        @Override
        public void onPaused() {

        }

        @Override
        public void onResumed() {

        }

        @Override
        public void onProgress(final long progress, final long duration) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(mMusicDialog != null) {
                        mMusicDialog.updateProgress(progress, duration);
                    }
                }
            });
        }

        @Override
        public void onCompleted() {

        }

        @Override
        public void onDownloadTimeout() {

        }

        @Override
        public void onOpenFailed() {

        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void showToast(final String text) {
        if(getActivity() == null || text == null) {
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(getActivity() != null) {
                    Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        });
    }

    private void showToastShort(final String text) {
        if(getActivity() == null || text == null) {
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(getActivity() != null) {
                    Toast toast = Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0, 0);
                    toast.show();
                }
            }
        });
    }

    private void showDialog(final String message) {
        if(getActivity() == null || message == null) {
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(getActivity() != null) {
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setTitle(getString(R.string.dialog_title));
                    dialog.setMessage(message);
                    dialog.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                        }
                    });
                    dialog.show();
                }
            }
        });
    }

    private void showNetWorkDialog(final String message) {
        if(getActivity() == null || message == null) {
            return;
        }
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(getActivity() != null) {
                    final AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());
                    dialog.setTitle(getString(R.string.dialog_title));
                    dialog.setMessage(message);
                    dialog.setNegativeButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    dialog.setNeutralButton(getString(R.string.reconnect), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            try {
                                mAlivcLivePusher.reconnectPushAsync();
                            } catch (IllegalStateException e) {

                            }
                        }
                    });
                    dialog.show();
                }
            }
        });
    }

    @Override
    public void run() {
        if(mIsPushing != null && mAlivcLivePusher != null) {
            try {
                isPushing = mAlivcLivePusher.isPushing();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
            mIsPushing.setText(String.valueOf(isPushing));
        }
        mHandler.postDelayed(this, REFRESH_INTERVAL);

    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.post(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(this);
    }

    public interface BeautyListener {
        void onBeautySwitch(boolean beauty);
    }

    private BeautyListener mBeautyListener = new BeautyListener() {
        @Override
        public void onBeautySwitch(boolean beauty) {
            if(mBeautyButton != null) {
                mBeautyButton.setSelected(beauty);
            }
        }
    };
}
