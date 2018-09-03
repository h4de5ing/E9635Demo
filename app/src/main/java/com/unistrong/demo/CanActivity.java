package com.unistrong.demo;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.e9631sdk.Command;
import com.android.e9631sdk.CommunicationService;
import com.android.e9631sdk.DataType;

public class CanActivity extends BaseActivity implements View.OnClickListener {

    private static final String TAG = "unistrong";
    private CommunicationService mService;
    private TextView mTv;
    private EditText etId;
    private EditText etData;
    private Spinner mSpFrameFormat;
    private Spinner mSpFrameType;
    int frameFormat = 0;
    int frameType = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_can);
        etId = (EditText) findViewById(R.id.et_id);
        etData = (EditText) findViewById(R.id.et_data);
        mSpFrameFormat = (Spinner) findViewById(R.id.sp_frame_format);
        mSpFrameType = (Spinner) findViewById(R.id.sp_frame_type);
        findViewById(R.id.btn_search_channel).setOnClickListener(this);
        findViewById(R.id.btn_set_channel1).setOnClickListener(this);
        findViewById(R.id.btn_set_channel2).setOnClickListener(this);
        findViewById(R.id.btn_search_mode).setOnClickListener(this);
        findViewById(R.id.btn_set_can_mode).setOnClickListener(this);
        findViewById(R.id.btn_set_baud_125K).setOnClickListener(this);
        findViewById(R.id.btn_set_baud_250K).setOnClickListener(this);
        findViewById(R.id.btn_set_baud_500K).setOnClickListener(this);
        findViewById(R.id.btn_send_data).setOnClickListener(this);
        findViewById(R.id.btn_filter).setOnClickListener(this);
        findViewById(R.id.btn_filter_cancel).setOnClickListener(this);
        mTv = (TextView) findViewById(R.id.tv_result);
        findViewById(R.id.btn_clean).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTv != null) {
                    mTv.setText("");
                }
            }
        });
        mSpFrameFormat.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.can_format)));
        mSpFrameType.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.can_type)));
        mSpFrameFormat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                frameFormat = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        mSpFrameType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                frameType = position;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        initBind();
    }

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
                            break;
                        case TAccOff:
                            break;
                        case TMcuVersion:
                            break;
                        case TMcuVoltage:
                            break;
                        case TCan125:
                            updateText("can 125K set success");
                            break;
                        case TCan250:
                            updateText("can 250K set success");
                            break;
                        case TCan500:
                            updateText("can 500K set success");
                            break;
                        case TDataOBD:
                            break;
                        case TDataJ1939:
                            break;
                        case TChannel:
                            updateText("current channel " + bytes[0]);
                            break;
                        case TDataMode:
                            updateText("current mode " + DataUtils.getDataMode(bytes[0]));
                            break;
                        case TDataCan:
                            handleCanData(bytes);
                            break;
                        case TUnknow:
                            break;
                        case TGPIO:
                            break;
                        case TFilter:
                            updateText((frameFormat == 0 ? "standard id" : "extend id") + " filter " + (bytes[0] == 0x01 ? "success" : " failed"));
                            break;
                        case TCancelFilter:
                            updateText("can id cancel filter " + (bytes[0] == 0x01 ? "success" : " failed"));
                            break;
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCanData(byte[] data) {
        switch (data[0]) {
            case 0x01:
                updateText("we got can channel 1 data:" + DataUtils.saveHex2String(data));
                break;
            case 0x02:
                updateText("we got can channel 2 data:" + DataUtils.saveHex2String(data));
                break;
        }
    }

    private void sendCommand(byte[] data) {
        if (mService != null) {
            if (mService.isBindSuccess()) {
                mService.send(data);
            }
        }
    }

    private void sendCanData(int frameFormat, int frameType, byte[] id, byte[] data) {
        if (mService != null) {
            if (mService.isBindSuccess()) {
                mService.sendCan(frameFormat, frameType, id, data);
            }
        }
    }

    private void updateText(final String string) {
        if (mTv != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTv.append(string + "\n");
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_search_channel:
                sendCommand(Command.Send.SearchChannel());
                break;
            case R.id.btn_set_channel1:
                sendCommand(Command.Send.Channel1());
                break;
            case R.id.btn_set_channel2:
                sendCommand(Command.Send.Channel2());
                break;
            case R.id.btn_search_mode:
                sendCommand(Command.Send.SearchMode());
                break;
            case R.id.btn_set_can_mode:
                sendCommand(Command.Send.ModeCan());
                break;
            case R.id.btn_set_baud_125K:
                sendCommand(Command.Send.Switch125K());
                break;
            case R.id.btn_set_baud_250K:
                sendCommand(Command.Send.Switch250K());
                break;
            case R.id.btn_set_baud_500K:
                sendCommand(Command.Send.Switch500K());
                break;
            case R.id.btn_send_data:
                String strID = etId.getText().toString();
                String strData = etData.getText().toString();
                if (strID.contains("X")) {
                    etId.setError("Error id");
                } else {
                    if (strID.length() == 8) {
                        if (strData.length() == 16) {
                            sendCanData(frameFormat, frameType, int2byte(strID, 4), int2byte(strData, 8));
                        } else {
                            etId.setError("Date is 8 Byte");
                        }
                    } else {
                        etId.setError("Id is 4 Byte");
                    }
                }
                break;
            case R.id.btn_filter:
                /**
                 *
                 过滤id格式  9个字节
                 第一个字节表示数据类型：
                 0x00 标准数据帧
                 0x02 标准远程帧
                 0x04 扩展数据帧
                 0x06 扩展远程帧
                 后面  发送数据 01 31 43 37 37 37 32 58 58
                 8个字节是过滤id的字符串
                 1C7772XX

                 例如过滤扩展数据帧 1C7772XX
                 */
                String strFilterID = etId.getText().toString().trim().toUpperCase();
                byte[] id = strFilterID.getBytes();
                sendCommand(Command.Send.filterCan(frameFormat,frameType,id));
                filterStr = strFilterID;
                break;
            case R.id.btn_filter_cancel:
                sendCommand(Command.Send.cancelFilterCan());
                break;
        }
    }

    String filterStr = "";

    public byte[] int2byte(String string, int byteLength) {
        byte[] data = new byte[byteLength];
        for (int j = 0; j < data.length; j++) {
            data[j] = (byte) (Integer.valueOf(string.substring(j * 2, j * 2 + 2), 16) & 0xff);
        }
        return data;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            try {
                mService.unbind();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static String saveHex2String(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (int i = 0; i < data.length; i++) {
            int value = data[i] & 0xff;
            sb.append(HEX[value / 16]).append(HEX[value % 16]).append(" ");
        }
        return sb.toString();
    }
}
