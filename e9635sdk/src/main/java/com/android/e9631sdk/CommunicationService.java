package com.android.e9631sdk;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.android.guard.IRemoteService;
import com.android.guard.IRemoteServiceCallBack;

import java.io.File;

public class CommunicationService {
    private static CommunicationService sCommunicationService;
    private static Context mActivity;
    private IProcessData mIProcessData;
    private static int mCountTime = 15000;

    public interface IProcessData {
        void process(byte[] data, DataType type);
    }

    public void getData(IProcessData iProcessData) {
        mIProcessData = iProcessData;
    }

    private CommunicationService() {
    }

    public static CommunicationService getInstance(Context activity) {
        if (sCommunicationService == null) {
            sCommunicationService = new CommunicationService();
            mActivity = activity;
        }
        return sCommunicationService;
    }

    private static final String IntentAction = "com.android.intent.action.SHUTDOWN";

    public void setShutdownCountTime(int second) {
        if (second < 10 || second > 30) {
            mCountTime = 10 * 1000;
        } else {
            mCountTime = second * 1000;
        }
        Intent intent = new Intent();
        intent.setAction(IntentAction);
        intent.putExtra("shutdown_value", "shutdown_time");
        intent.putExtra("shutdown_time", mCountTime);
        mActivity.sendBroadcast(intent);
    }


    public void send(byte[] data) {
        if (mService != null) {
            try {
                if (new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + "debug.txt").exists()) {
                    Log.i("gh9st", "write:" + saveHex2String(data));
                }
                mService.handleData(data);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }


    @Deprecated
    public void sendCan(byte[] id, byte[] data) {
        send(Command.Send.sendData(byteArrayAddByteArray(id, data), Command.SendDataType.Can));
    }

    public void sendCan(int frameFormat, int frameType, byte[] id, byte[] data) {
        String zeroString = "00000000000000000000000000000000";
        if (frameFormat == 0) {//11位 数据帧
            int count = (id[0] & 0xff) << 24 | (id[1] & 0xff) << 16 | (id[2] & 0xff) << 8 | (id[3] & 0xff);
            int shlInt = count << 21;
            String shlBinary = Integer.toBinaryString(shlInt);
            if (shlBinary.length() < 32) {
                shlBinary = zeroString.substring(0, 32 - shlBinary.length()) + shlBinary;
            }
            for (int i = 0; i < id.length; i++) {
                id[i] = J1939Utils.string2byte(J1939Utils.getHex(shlBinary.substring(i * 8, (i + 1) * 8)));
            }
        } else {//29位扩展帧
            int count = (id[0] & 0xff) << 24 | (id[1] & 0xff) << 16 | (id[2] & 0xff) << 8 | (id[3] & 0xff);
            int shlInt = count << 3;
            String shlBinary = Integer.toBinaryString(shlInt);
            if (shlBinary.length() < 32) {
                shlBinary = zeroString.substring(0, 32 - shlBinary.length()) + shlBinary;
            }
            for (int i = 0; i < id.length; i++) {
                id[i] = J1939Utils.string2byte(J1939Utils.getHex(shlBinary.substring(i * 8, (i + 1) * 8)));
            }
        }
        byte[] canData = null;
        if (frameFormat == 0 && frameType == 0) {//标准数据帧
            id[3] = id[3];
            canData = new byte[1 + 4 + 1 + data.length];//1个字节通道 + 4个字节ID + 1个字节数据长度 + N个字节数据(N小于等于8)
        } else if (frameFormat == 0 && frameType == 1) {//标准远程帧
            id[3] = (byte) (id[3] | 0x02);
            canData = new byte[5];//1个字节通道 + 4个字节ID
        } else if (frameFormat == 1 && frameType == 0) {//扩展数据帧
            id[3] = (byte) (id[3] | 0x04);
            canData = new byte[1 + 4 + 1 + data.length];
        } else if (frameFormat == 1 && frameType == 1) {//扩展远程帧 输入值左移3位 然后 & 000类型
            id[3] = (byte) (id[3] | 0x06);
            canData = new byte[5];
        }
        if (canData != null) {
            canData[0] = 0x00;//通道,不处理
            System.arraycopy(id, 0, canData, 1, id.length);//id
            if (frameType == 0) {//数据帧
                canData[5] = (byte) data.length;
                System.arraycopy(data, 0, canData, 6, data.length);//data
            }
            send(Command.Send.sendData(canData, Command.SendDataType.Can));
        }
    }

    private byte[] byteArrayAddByteArray(byte[] id, byte[] data) {
        byte[] resultData = new byte[id.length + data.length + 2];
        resultData[0] = 0x00;
        System.arraycopy(id, 0, resultData, 1, id.length);
        resultData[id.length + 1] = (byte) data.length;
        System.arraycopy(data, 0, resultData, id.length + 2, data.length);
        return resultData;
    }

    public void sendOBD(byte[] data) {
        send(Command.Send.sendData(data, Command.SendDataType.OBDII));
    }

    public void sendJ1939(byte[] data) {
        send(Command.Send.sendData(data, Command.SendDataType.J1939));
    }


    public void bind() throws Exception {
        if (mActivity != null) {
            Intent intent = new Intent();
            intent.setAction("com.android.guard.E9631Service");
            intent.setPackage("com.android.guard");
            mActivity.bindService(intent, conn, Context.BIND_AUTO_CREATE);
        } else {
            throw new Exception("gh0st -- bind fail , activity is Null");
        }
    }


    public void unbind() throws Exception {
        if (mActivity != null) {
            if (mService != null) {
                try {
                    mService.unregisterCallback(mCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mActivity.unbindService(conn);
                mService = null;
            }
        } else {
            throw new Exception("gh0st -- unbind fail , activity is Null");
        }
    }

    public boolean isBindSuccess() {
        return mService != null;
    }

    IRemoteService mService;
    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                mService = IRemoteService.Stub.asInterface(service);
                mService.registerCallback(mCallback);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private IRemoteServiceCallBack mCallback = new IRemoteServiceCallBack.Stub() {

        @Override
        public void valueChanged(byte[] protocolData) throws RemoteException {
            if (new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + File.separator + "debug.txt").exists()) {
                Log.i("gh9st", "read:" + saveHex2String(protocolData));
            }
            postData2(protocolData);
        }
    };


    private void postData2(byte[] protocolData) {
        byte type = protocolData[0];
        int length = ((protocolData[1] & 0xFF) << 8 | ((protocolData[2] & 0xFF)));
        byte dataType = protocolData[3];
        byte[] data = new byte[length];
        System.arraycopy(protocolData, 3, data, 0, data.length);
        if (type == 0x21) {
            mIProcessData.process(data, DataType.TMcuVersion);
        } else if (type == 0x30 && dataType == 0x01) {//acc on
            mIProcessData.process(data, DataType.TAccOn);
        } else if (type == 0x30 && dataType == 0x00) {//acc off
            mIProcessData.process(data, DataType.TAccOff);
        } else if (type == 0x31 && dataType == 0x12) {//can 125
            mIProcessData.process(data, DataType.TCan125);
        } else if (type == 0x31 && dataType == 0x25) {//can 250
            mIProcessData.process(data, DataType.TCan250);
        } else if (type == 0x31 && dataType == 0x50) {//can 500
            mIProcessData.process(data, DataType.TCan500);
        } else if (type == 0x33) {
            mIProcessData.process(data, DataType.TMcuVoltage);
        } else if (type == 0x41) {
            mIProcessData.process(data, DataType.TDataCan);
            //updateCan(data);
        } else if (type == 0x61) {
            mIProcessData.process(data, DataType.TDataOBD);
        } else if (type == 0x71) {
            //mIProcessData.process(data, DataType.TDataJ1939);
            updateJ1939(data);
        } else if (type == (byte) 0x81) {
            mIProcessData.process(data, DataType.TDataMode);
        } else if (type == (byte) 0x83) {
            mIProcessData.process(data, DataType.TChannel);
        } else if (type == (byte) (0x91)) {
            mIProcessData.process(data, DataType.TGPIO);
        } else if (type == 0x50) {
            mIProcessData.process(data, DataType.TFilter);
        } else if (type == 0x51) {
            mIProcessData.process(data, DataType.TCancelFilter);
        } else {
            mIProcessData.process(data, DataType.TUnknow);
            Log.d("gh0st", "we don't support:" + saveHex2String(protocolData));
        }
    }

    private void updateCan(byte[] bytes) {
        byte[] id = new byte[4];
        System.arraycopy(bytes, 1, id, 0, id.length);//ID
        byte[] data = null;
        int frameFormatType = (id[3] & 7);
        int frameFormat = 0;
        int frameType = 0;
        byte[] newId = null;
        switch (frameFormatType) {
            case 0://标准数据
                frameFormat = 0;
                frameType = 0;
                newId = new KtUtils().ushr3(id, 21);
                int dataLength = bytes[5];
                data = new byte[dataLength];
                System.arraycopy(bytes, 6, data, 0, dataLength);
                break;
            case 2://标准远程
                frameFormat = 0;
                frameType = 1;
                newId = new KtUtils().ushr3(id, 21);
                break;
            case 4://扩展数据
                frameFormat = 1;
                frameType = 0;
                newId = new KtUtils().ushr3(id, 3);
                int dataLengthExtra = bytes[5];
                data = new byte[dataLengthExtra];
                System.arraycopy(bytes, 6, data, 0, dataLengthExtra);
                break;
            case 6://扩展远程
                frameFormat = 1;
                frameType = 1;
                newId = new KtUtils().ushr3(id, 3);
                break;
        }
        //channel ->  bytes[0]
        //frameFormat
        //frameType
        //id  -> newId
        //if(frameType == 0) data
    }

    private void updateJ1939(byte[] bytes) {
        byte[] id = new KtUtils().handleJ1939(bytes);
        byte[] data = new byte[bytes.length];
        data[0] = bytes[0];
        System.arraycopy(id, 0, data, 1, id.length);
        System.arraycopy(bytes, 5, data, 5, bytes.length - 5);
        if (mIProcessData != null) {
            mIProcessData.process(data, DataType.TDataJ1939);
        }
    }

    public static String saveHex2String(byte[] data) {
        StringBuilder sb = new StringBuilder();
        final char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (byte aData : data) {
            int value = aData & 0xff;
            sb.append(HEX[value / 16]).append(HEX[value % 16]).append(" ");
        }
        return sb.toString();
    }
}
