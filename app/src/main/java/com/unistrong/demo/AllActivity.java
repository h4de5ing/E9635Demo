package com.unistrong.demo;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.unistrong.demo.cvbsVideo.VideoService;
import com.unistrong.demo.cvbsVideo.VideoStorage;
import com.unistrong.demo.dashboard.DashboardActivity;
import com.unistrong.demo.gps.GPSActivity;
import com.unistrong.demo.utils.SpannableStringUtils;
import com.unistrong.e9631dmeo.IVideoCallback;
import com.unistrong.e9631sdk.Command;
import com.unistrong.e9631sdk.CommunicationService;
import com.unistrong.e9631sdk.DataType;
import com.unistrong.uartsdk.ProcessData;
import com.unistrong.uartsdk.VanManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class AllActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "gh0st";
    public static final int camera6ID = 6;
    public static final int camera7ID = 7;
    private TextureView video6;
    private TextureView video7;
    private Button btnTakePicture6;
    private Button btnTakePicture7;
    private Button btnRecord6;
    private Button btnRecord7;
    private Chronometer tvRecordingTime6;
    private Chronometer tvRecordingTime7;
    private TextView tvMCUInfo;
    private TextView tvCanInfo;
    private TextView tvUartInfo;
    private TextView tvGSM;
    private VanManager vanManager;
    private MyPhoneStateListener myPhoneStateListener;
    private SurfaceTexture mSurfaceTexture6;
    private SurfaceTexture mSurfaceTexture7;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all);
        startVideoService();
        tvMCUInfo = (TextView) findViewById(R.id.tv_mcu_info);
        tvCanInfo = (TextView) findViewById(R.id.tv_can_info);
        tvUartInfo = (TextView) findViewById(R.id.tv_uart_info);
        tvGSM = (TextView) findViewById(R.id.tv_gsm);
        video6 = (TextureView) findViewById(R.id.video6);
        btnTakePicture6 = (Button) findViewById(R.id.btn_take_picture6);
        btnRecord6 = (Button) findViewById(R.id.btn_record6);
        tvRecordingTime6 = (Chronometer) findViewById(R.id.tv_recording_time6);

        video7 = (TextureView) findViewById(R.id.video7);
        btnTakePicture7 = (Button) findViewById(R.id.btn_take_picture7);
        btnRecord7 = (Button) findViewById(R.id.btn_record7);
        tvRecordingTime7 = (Chronometer) findViewById(R.id.tv_recording_time7);
        video6.setOnClickListener(this);
        video7.setOnClickListener(this);
        findViewById(R.id.btn_gps).setOnClickListener(this);
        findViewById(R.id.btn_dashboard).setOnClickListener(this);
        findViewById(R.id.btn_volume_plug).setOnClickListener(this);
        findViewById(R.id.btn_volume_sub).setOnClickListener(this);
        findViewById(R.id.btn_4).setOnClickListener(this);
        findViewById(R.id.btn_6).setOnClickListener(this);
        findViewById(R.id.btn_7).setOnClickListener(this);
        findViewById(R.id.btn_obd_start).setOnClickListener(this);
        findViewById(R.id.btn_obd_start2).setOnClickListener(this);
        btnTakePicture6.setOnClickListener(this);
        btnTakePicture7.setOnClickListener(this);
        btnRecord6.setOnClickListener(this);
        btnRecord7.setOnClickListener(this);
        tvMCUInfo.setMovementMethod(new ScrollingMovementMethod());
        tvCanInfo.setMovementMethod(new ScrollingMovementMethod());
        tvUartInfo.setMovementMethod(new ScrollingMovementMethod());
        bindVideoService();
        initTextureView();
        openUart();
        initBind();
        //btnOBDStart.performClick();
        initMcuTimer();
        initGSM();
        initVolumes();
        updateMCUText();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindVideoService();
        initTextureView();
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            if (getRecordingState(camera6ID)) {
                mCVBSService.stopVideoRecording(camera6ID);
                btnRecord6.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                tvRecordingTime6.stop();
                tvRecordingTime6.setVisibility(View.INVISIBLE);
            }
            if (getRecordingState(camera7ID)) {
                mCVBSService.stopVideoRecording(camera7ID);
                btnRecord7.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                tvRecordingTime7.stop();
                tvRecordingTime7.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        stopPreview(camera6ID);
        stopPreview(camera7ID);
        //mCVBSService.closeCamera(camera6ID);
        //mCVBSService.closeCamera(camera7ID);
        unbindVideoService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        vanManager.closeUart4();
        if (mService != null) {
            try {
                mService.unbind();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
        if (mMcuTimer != null) {
            mMcuTimer.cancel();
            mMcuTimer.purge();
            mMcuTimer = null;
        }
        //if (mCVBSService.getPreviewState(camera6ID)) mCVBSService.stopPreview(camera6ID);
        //if (mCVBSService.getPreviewState(camera7ID)) mCVBSService.stopPreview(camera7ID);
        if (!getRecordingState(camera6ID) && !getRecordingState(camera7ID)) stopVideoService();
        //unbindVideoService();
    }

    private void startVideoService() {
        Intent intent = new Intent(AllActivity.this, VideoService.class);
        startService(intent);
    }

    private void stopVideoService() {
        Intent intent = new Intent(this, VideoService.class);
        stopService(intent);
    }

    private AudioManager mAudioManager;
    private int mMaxVolume = 0;
    private int mCurrentVolume = 0;

    private void initVolumes() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (mAudioManager != null) {
            mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
    }

    private void updateVolumes() {
        Log.i(TAG, "Current:" + mCurrentVolume);
    }

    private Timer mMcuTimer;
    private boolean isResponse = false;

    private void initMcuTimer() {
        mMcuTimer = new Timer();
        mMcuTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                isResponse = false;
                String status = readStatus(acStatus);
                if ("1".equals(status)) {// ac charger is plugged
                    mHandler.sendEmptyMessageDelayed(0, 100);
                } else {//ac charger not plugged
                    mVersion = "NULL";
                    mVoltage = "NULL";
                    mAccStatus = false;
                    mGpio_mileage = false;
                    mGpio_rada = false;
                    mHandler.sendEmptyMessageDelayed(1, 1000);
                }
                Log.i(TAG, "ac charger detection: " + status);
            }
        }, 10000, 2000);
    }

    private static final String acStatus = "/sys/class/power_supply/ac/present";
    private static final String usbStatus = "/sys/class/power_supply/usb/present";

    public String readStatus(String path) {
        String statue = null;
        try {
            BufferedReader read = new BufferedReader(new FileReader(new File(path)));
            statue = read.readLine();
            read.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return statue;
    }

    private class MyPhoneStateListener extends PhoneStateListener {
        @Override
        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            Class<?> signalStrengthClass = signalStrength.getClass();
            try {
                Method method = signalStrengthClass.getMethod("getDbm");
                method.setAccessible(true);
                Object object = method.invoke(signalStrength);
                Log.i("gh0st", "" + object);
                updateGSM(object.toString() + " Dbm");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private void initGSM() {
        TelephonyManager manager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        if (manager != null) {
            if (manager.getSimState() == TelephonyManager.SIM_STATE_ABSENT) {
                Log.i("gh0st", "noSIM");
                updateGSM("noSIM");
            } else {
                myPhoneStateListener = new MyPhoneStateListener();
                manager.listen(myPhoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        }
    }

    private void updateGSM(final String s) {
        if (tvGSM != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tvGSM != null) {
                        tvGSM.setText(s);
                    }
                }
            });
        }
    }

    private void openUart() {
        vanManager = new VanManager();
        boolean uart4opend = vanManager.openUart4(115200);
        updateUartText(uart4opend ? "uart4 opend success" : "uart4 opend failed");
        vanManager.uartData4(new ProcessData() {
            @Override
            public void process(byte[] bytes, int len) {
                updateUartText("uart4:[ " + len + " ]" + new String(bytes));
            }
        });
        boolean uart6opend = vanManager.openUart6(115200);
        updateUartText(uart6opend ? "uart6 opend success" : "uart6 opend failed");
        vanManager.uartData6(new ProcessData() {
            @Override
            public void process(byte[] bytes, int len) {
                updateUartText("uart6:[ " + len + " ]" + new String(bytes));
            }
        });
        boolean uart7opend = vanManager.openUart7(115200);
        updateUartText(uart7opend ? "uart7 opend success" : "uart7 opend success");
        vanManager.uartData7(new ProcessData() {
            @Override
            public void process(byte[] bytes, int len) {
                updateUartText("uart7:[ " + len + " ]" + new String(bytes));
            }
        });
    }

    private void updateUartText(final String string) {
        if (tvUartInfo != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tvUartInfo != null) {
                        tvUartInfo.append(string + "\n");
                        int offset = tvUartInfo.getLineCount() * tvUartInfo.getLineHeight() - tvUartInfo.getHeight();
                        if (offset >= 500) {
                            tvUartInfo.setText("");
                        }
                        tvUartInfo.scrollTo(0, offset > 0 ? offset : 0);
                    }
                }
            });
        }
    }

    private CommunicationService mService;
    private boolean isStart = false;
    //public byte ECUType = (byte) 0xDF;
    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    sendCommandList();
                    break;
                case 1:
                    updateMCUText();
                    break;
                case 2:
                    if (!isResponse) {
                        mGpio_mileage = false;
                        mGpio_rada = false;
                        updateMCUText();
                    }
                    break;
            }
        }
    };

    private void sendCommandList() {
        final List<byte[]> list = new ArrayList<>();
        list.add(Command.Send.Version());
        list.add(Command.Send.Voltage());
        list.add(Command.Send.SearchAccStatus());
        list.add(Command.Send.Gpio().get(2));
        list.add(Command.Send.Gpio().get(8));
        new Thread(new Runnable() {
            @Override
            public void run() {
                for (byte[] bytes : list) {
                    sendCommand(bytes);
                    SystemClock.sleep(300);
                }
                mHandler.sendEmptyMessageDelayed(2, 200);
            }
        }).start();
    }

    private byte[] start11 = new byte[]{0x01, 0x07, (byte) 0xDF, 0x00, 0x00, 0x02, 0x01, 0x00};//ISO15756 500K 11bit
    long mLastTime = 0;
    long mCurTime = 0;
    private boolean isFullScreen6 = false;
    private boolean isFullScreen7 = false;

    //gh0stOnClick
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_obd_start:
                if (mService != null) {
                    isStart = true;
                    sendCommand(Command.Send.Channel1());
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendCommand(Command.Send.SearchMode());
                        }
                    }, 200);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            sendOBDData(start11);
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (App.Companion.getAECUType() == (byte) 0xDF) {
                                        updateOBDText("can 1 connect failed");
                                    }
                                    isStart = false;
                                }
                            }, 3000);
                        }
                    }, 500);
                }
                break;
            case R.id.btn_obd_start2:
                if (mService != null) {
                    isStart = true;
                    sendCommand(Command.Send.Channel2());
                    sendOBDData(start11);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (App.Companion.getAECUType() == (byte) 0xDF) {
                                updateOBDText("can 2 connect failed");
                            }
                            isStart = false;
                        }
                    }, 3000);
                }
                break;
            case R.id.btn_4:
                if (vanManager != null && vanManager.isOpenUart4()) {
                    vanManager.sendData2Uart4("ttyS4".getBytes());
                }
                break;
            case R.id.btn_6:
                if (vanManager != null && vanManager.isOpenUart4()) {
                    vanManager.sendData2Uart6("ttyS6".getBytes());
                }
                break;
            case R.id.video6:
                mLastTime = mCurTime;
                mCurTime = System.currentTimeMillis();
                if (mCurTime - mLastTime < 300) {
                    mCurTime = 0;
                    mLastTime = 0;
                    Log.i(TAG, "double click");
                    if (!isFullScreen6) {
                        fullScale6();
                    } else {
                        smallScale6();
                    }
                    isFullScreen6 = !isFullScreen6;
                }
                break;
            case R.id.video7:
                mLastTime = mCurTime;
                mCurTime = System.currentTimeMillis();
                if (mCurTime - mLastTime < 300) {
                    mCurTime = 0;
                    mLastTime = 0;
                    Log.i(TAG, "double click");
                    if (!isFullScreen7) {
                        fullScale7();
                    } else {
                        smallScale7();
                    }
                    isFullScreen7 = !isFullScreen7;
                }
                break;
            case R.id.btn_7:
                if (vanManager != null && vanManager.isOpenUart4()) {
                    vanManager.sendData2Uart7("ttyS7".getBytes());
                }
                break;
            case R.id.btn_take_picture6:
                takePicture6();
                break;
            case R.id.btn_record6:
                record6();
                break;
            case R.id.btn_take_picture7:
                takePicture7();
                break;
            case R.id.btn_record7:
                record7();
                break;
            case R.id.btn_volume_plug:
                if (mAudioManager != null) {
                    mCurrentVolume++;
                    if (mCurrentVolume > mMaxVolume) {
                        mCurrentVolume = mMaxVolume;
                    }
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
                    updateVolumes();
                }
                break;
            case R.id.btn_volume_sub:
                if (mAudioManager != null) {
                    mCurrentVolume--;
                    if (mCurrentVolume <= 0) {
                        mCurrentVolume = 0;
                    }
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mCurrentVolume, 0);
                    updateVolumes();
                }
                break;
            case R.id.btn_gps:
                startActivity(new Intent(AllActivity.this, GPSActivity.class));
                break;
            case R.id.btn_dashboard:
                startActivity(new Intent(AllActivity.this, DashboardActivity.class));
                break;
        }
    }

    private void fullScale6() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(720, 576);
        video6.setLayoutParams(params);
    }

    private void smallScale6() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(180, 144);
        params.topMargin = 53;
        params.leftMargin = 310;
        video6.setLayoutParams(params);
    }

    private void fullScale7() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(720, 576);
        video7.setLayoutParams(params);
    }

    private void smallScale7() {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(180, 144);
        params.topMargin = 53;
        params.leftMargin = 560;
        video7.setLayoutParams(params);
    }

    private boolean mAccStatus = false;
    private String mVersion = "NULL";
    private String mVoltage = "NULL";
    private boolean mGpio_rada = false;
    private boolean mGpio_mileage = false;

    private void initBind() {
        try {
            mService = CommunicationService.getInstance(this);
            mService.setShutdownCountTime(12);
            mService.bind();
            mService.getData(new CommunicationService.IProcessData() {
                @Override
                public void process(byte[] bytes, DataType dataType) {
                    Log.e(TAG, dataType.name() + " " + DataUtils.saveHex2String(bytes));
                    switch (dataType) {
                        case TAccOn:
                            mAccStatus = true;
                            updateMCUText();
                            break;
                        case TAccOff:
                            mAccStatus = false;
                            updateMCUText();
                            break;
                        case TMcuVersion:
                            mVersion = new String(bytes);
                            updateMCUText();
                            break;
                        case TMcuVoltage:
                            mVoltage = new String(bytes);
                            updateMCUText();
                            break;
                        case TCan250:
                            updateOBDText("can 250K set success");
                            break;
                        case TCan500:
                            updateOBDText("can 500K set success");
                            break;
                        case TChannel:
                            updateOBDText("current channel " + bytes[0]);
                            break;
                        case TDataMode:
                            if (bytes[0] != 0x02) {
                                sendCommand(Command.Send.ModeOBD());
                            }
                            updateOBDText("current mode " + DataUtils.getDataMode(bytes[0]));
                            break;
                        case TDataCan:
                            break;
                        case TDataOBD:
                            //updateOBDText("we got obd data:" + DataUtils.saveHex2String(bytes));
                            handleOBD(bytes);
                            break;
                        case TDataJ1939:
                            break;
                        case TUnknow:
                            break;
                        case TGPIO:
                            isResponse = true;
                            if (bytes[0] == 0x12) {
                                mGpio_rada = true;
                            } else if (bytes[0] == 0x22) {
                                mGpio_mileage = true;
                            }
                            updateMCUText();
                            break;
                        case TAccStatus:
                            break;
                    }
                }
            });
            mHandler.sendEmptyMessageDelayed(0, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateMCUText() {
        if (tvMCUInfo != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SpannableStringBuilder string = SpannableStringUtils.getBuilder("MCUVersion:")
                            .setForegroundColor(Color.BLACK)
                            .append(mVersion)
                            .setForegroundColor(Color.BLACK)
                            .append("\nVoltage:")
                            .setForegroundColor(Color.BLACK)
                            .append(mVoltage)
                            .setForegroundColor(Color.BLACK)
                            .append("\nAccStatus:")
                            .setForegroundColor(Color.BLACK)
                            .append("" + (mAccStatus ? "ON" : "OFF"))
                            .setForegroundColor(mAccStatus ? Color.RED : Color.BLACK)
                            .append("\nGPIORada:")
                            .setForegroundColor(Color.BLACK)
                            .append(mGpio_rada ? "High" : "Low")
                            .setForegroundColor(mGpio_rada ? Color.RED : Color.BLACK)
                            .append("\nGPIOMileage:")
                            .setForegroundColor(Color.BLACK)
                            .append(mGpio_mileage ? "High" : "Low")
                            .setForegroundColor(mGpio_rada ? Color.RED : Color.BLACK)
                            .create();
                    tvMCUInfo.setText(string);
                }
            });
        }
    }

    private void updateOBDText(final String string) {
        if (tvCanInfo != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (tvCanInfo != null) {
                        tvCanInfo.append(string + "\n");
                        int offset = tvCanInfo.getLineCount() * tvCanInfo.getLineHeight() - tvCanInfo.getHeight();
                        if (offset >= 500) {
                            tvCanInfo.setText("");
                        }
                        tvCanInfo.scrollTo(0, offset > 0 ? offset : 0);
                    }
                }
            });
        }
    }

    private void sendCommand(byte[] data) {
        if (mService != null) {
            if (mService.isBindSuccess()) {
                mService.send(data);
            }
        }
    }

    private void sendOBDData(byte[] data) {
        if (mService != null) {
            if (mService.isBindSuccess()) {
                mService.sendOBD(data);
            }
        }
    }

    private void handleOBD(byte[] bytes) {
        if (isStart) {
            updateOBDText("connect success");
            App.Companion.setAECUType((byte) (bytes[3] - (byte) 0x08));
            initTimer();
        } else {
            //00 00 07 E8 03 41 0D EA
            byte pci = bytes[4];
            byte frameType = (byte) (pci & (byte) 0xf0);
            int length = pci & 0x0f;
            switch (frameType) {
                case 0x00://SINGLE_FRAME
                    byte[] valueBytes = new byte[length - 2];
                    System.arraycopy(bytes, bytes.length - length + 2, valueBytes, 0, valueBytes.length);
                    byte pid = bytes[6];
                    switch (pid) {
                        case 0x01:
                            break;
                        case 0x0D:
                            //hex(EA)=des(234)
                            int speed = valueBytes[0] & 0xff;
                            updateOBDText("Vehicle speed:" + speed + " KM/H");
                            break;
                    }
                    break;
                case 0x10://FIRST_FRAME
                    break;
                case 0x20://CONTINUOUS_FRAME
                    break;
                case 0x30://Flow_FRAME
                    break;
            }
        }
    }

    private Timer timer;

    private void initTimer() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                byte pid = 0x0D;//0x0D  Vehicle speed
                sendOBDData(new byte[]{0x01, 0x07, App.Companion.getAECUType(), 0x00, 0x00, 0x02, 0x01, pid});
            }
        }, 5000, 1000);
    }

    //gh0stcvbs
    private void takePicture6() {
        if (mCVBSService != null) {
            mCVBSService.takePicture(camera6ID);
            Toast.makeText(AllActivity.this, "take done 6", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePicture7() {
        if (mCVBSService != null) {
            mCVBSService.takePicture(camera7ID);
            Toast.makeText(AllActivity.this, "take done 7", Toast.LENGTH_SHORT).show();
        }
    }


    private void record6() {
        if (new File(VideoStorage.fileRootPath).exists()) {
            if (mCVBSService != null) {
                if (getRecordingState(camera6ID)) {
                    mCVBSService.stopVideoRecording(camera6ID);
                    tvRecordingTime6.stop();
                    tvRecordingTime6.setVisibility(View.INVISIBLE);
                    btnRecord6.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                } else {
                    mCVBSService.startVideoRecording(camera6ID, mSurfaceTexture6);
                    tvRecordingTime6.setVisibility(View.VISIBLE);
                    tvRecordingTime6.setBase(SystemClock.elapsedRealtime());
                    tvRecordingTime6.start();
                    btnRecord6.setBackgroundResource(R.drawable.ic_stop_black_24dp);
                }
            }
        }
    }

    private void record7() {
        if (new File(VideoStorage.fileRootPath).exists()) {
            if (mCVBSService != null) {
                if (getRecordingState(camera7ID)) {
                    mCVBSService.stopVideoRecording(camera7ID);
                    tvRecordingTime7.stop();
                    tvRecordingTime7.setVisibility(View.INVISIBLE);
                    btnRecord7.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                } else {
                    mCVBSService.startVideoRecording(camera7ID, mSurfaceTexture7);
                    tvRecordingTime7.setVisibility(View.VISIBLE);
                    tvRecordingTime7.setBase(SystemClock.elapsedRealtime());
                    tvRecordingTime7.start();
                    btnRecord7.setBackgroundResource(R.drawable.ic_stop_black_24dp);
                }
            }
        }
    }


    public void initTextureView() {
        if (video6 != null) {
            video6.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    mSurfaceTexture6 = surface;
                    startPreview(camera6ID, surface);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    if (mCVBSService != null) {
                        stopPreview(camera6ID);
                        closeCamera(camera6ID);
                    }
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
        if (video7 != null) {
            video7.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    mSurfaceTexture7 = surface;
                    startPreview(camera7ID, surface);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    if (mCVBSService != null) {
                       stopPreview(camera7ID);
                       closeCamera(camera7ID);
                    }
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {

                }
            });
        }
    }

    private void initVideo6() {
        if (mCVBSService != null) {
            startPreview(camera6ID, video6.getSurfaceTexture());
        }
    }

    private void initVideo7() {
        if (mCVBSService != null) {
            startPreview(camera7ID, video7.getSurfaceTexture());
        }
    }

    private void startPreview(int cameraId, SurfaceTexture surfaceTexture) {
        if (mCVBSService != null && (surfaceTexture != null)) {
            mCVBSService.startPreview(cameraId, surfaceTexture);
        }
    }

    public void stopPreview(int cameraId) {
        if (mCVBSService != null) {
            mCVBSService.stopPreview(cameraId);
        }
    }
    private void closeCamera(int cameraId) {
        if (mCVBSService != null) {
            mCVBSService.closeCamera(cameraId);
        }
    }

    private VideoService mCVBSService = null;
    private ServiceConnection mVideoServiceConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName classname, IBinder obj) {
            mCVBSService = ((VideoService.LocalBinder) obj).getService();
            initVideo6();
            initVideo7();
            if (mCVBSService != null) {
                mCVBSService.registerCallback(mVideoCallback);
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            if (mCVBSService != null) {
                mCVBSService.unregisterCallback(mVideoCallback);
            }
            mCVBSService = null;
        }
    };

    private void bindVideoService() {
        Intent intent = new Intent(this, VideoService.class);
        bindService(intent, mVideoServiceConn, Context.BIND_AUTO_CREATE);
    }

    private void unbindVideoService() {
        if (mVideoServiceConn != null) {
            unbindService(mVideoServiceConn);
        }
    }

    private boolean getRecordingState(int index) {
        if (mCVBSService != null)
            return mCVBSService.getRecordingState(index);
        return false;
    }

    private IVideoCallback.Stub mVideoCallback = new IVideoCallback.Stub() {
        @Override
        public void onUpdateTimes(int index, String times) throws RemoteException {
        }
    };

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            if (getRecordingState(camera6ID)) {
                mCVBSService.stopVideoRecording(camera6ID);
                btnRecord6.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                tvRecordingTime6.stop();
                tvRecordingTime6.setVisibility(View.INVISIBLE);
            }
            if (getRecordingState(camera7ID)) {
                mCVBSService.stopVideoRecording(camera7ID);
                btnRecord7.setBackgroundResource(R.drawable.ic_play_arrow_black_24dp);
                tvRecordingTime7.stop();
                tvRecordingTime7.setVisibility(View.INVISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        finish();
    }
}