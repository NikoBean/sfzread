package zycm.zynikostationzhaji.com.zyfaceonebyone.niko.sfz;

import android.content.Context;
import android.util.Log;

import com.huashi.otg.sdk.HSIDCardInfo;
import com.huashi.otg.sdk.Hs_Cmd;
import com.huashi.otg.sdk.Utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android_serialport_api.SerialPort;

/**
 * AUTHOR:       Niko
 * VERSION:      V1.0
 * E-MAIL:       JAVADAD@163.COM
 * DESCRIPTION:  description
 * CREATE TIME:  2019-11-16 15:51
 * NOTE:
 */
public class HsSerialPortSDK {
    private FileOutputStream mOutputStream;
    private FileInputStream mInputStream;
    private SerialPort sp;
    private boolean isConn = false;
    private Context context;
    static String filepath = "";
    private int sfzIdCardStatus;
    private boolean isDown;//身份证读卡器是否挂机了

    public boolean isDown() {
        return isDown;
    }

    public void setDown(boolean down) {
        isDown = down;
    }

    public HsSerialPortSDK(Context context, String path) throws Exception {
        this.context = context;
        filepath = path;
        (new Thread() {
            public void run() {
                HsSerialPortSDK.this.copy(HsSerialPortSDK.this.context, "base.dat", "base.dat", HsSerialPortSDK.filepath);
                HsSerialPortSDK.this.copy(HsSerialPortSDK.this.context, "license.lic", "license.lic", HsSerialPortSDK.filepath);
            }
        }).start();
    }

    public boolean isConn() {
        return isConn;
    }



    public int init(String port, int baud, int flag) {
        if(this.isConn) {
            return 0;
        } else {
            try {
                this.sp = new SerialPort(new File(port), baud, flag);
            } catch (SecurityException | IOException var5) {
                return -1;
            }

            if(this.sp == null) {
                return -1;
            } else {
                this.mInputStream = (FileInputStream)this.sp.getInputStream();
                this.mOutputStream = (FileOutputStream)this.sp.getOutputStream();
                if(this.mInputStream == null && this.mOutputStream == null) {
                    return -1;
                } else {
                    this.isConn = true;
                    return 0;
                }
            }
        }
    }

    public void close() throws Exception {
        this.isConn = false;
        this.sp.close();
    }

    public String GetSAM() {
        if(!this.isConn) {
            return "";
        } else {
            try {
                byte[] var2 = this.send2Recv(Hs_Cmd.cmd_SAM, 800L);
                return var2[9] == -112?this.AnalyzeSAM(var2):"";
            } catch (Exception var21) {
                return "";
            }
        }
    }


    public int Authenticate(long timeOut) {
        return !this.isConn?-1:(this.startFindIDCard(timeOut) == 0 && this.selectIDCard(timeOut) == 0?0:-1);
    }

    public int NotAuthenticate(long timeOut) {
        if(!this.isConn) {
            return -1;
        } else {
            this.startFindIDCard(timeOut);
            this.selectIDCard(timeOut);
            return 0;
        }
    }



    public int Read_Card(HSIDCardInfo ic, long timeOut) {
        if(!this.isConn) {
            return -1;
        } else {

            try {
                byte[] var8 = new byte[256];
                byte[] fpData = new byte[1024];
                byte[] wltData = new byte[1024];
                byte[] recv = this.send2Recv(Hs_Cmd.cmd_read_ftp, timeOut);
                if (recv == null) {
                    setDown(true);
                    return -2;
                }
                System.arraycopy(recv, 16, var8, 0, var8.length);
                System.arraycopy(recv, 272, wltData, 0, wltData.length);
                System.arraycopy(recv, 1296, fpData, 0, fpData.length);
                ic.setwltdata(wltData);
                ic.setFpDate(fpData);
                Utility.PersonInfoUtoG(var8, ic);
                return 0;
            } catch (Exception var81) {
                return -3;
            }
        }
    }

    public int Unpack(byte[] wltdata) {
        boolean ret = true;

        try {
            int var4 = Utility.PersonInfoPic(filepath, wltdata, Hs_Cmd.byLicData);
            return var4;
        } catch (Exception var41) {
            return -1;
        }
    }

    private byte startFindIDCard(long timeOut) {
        byte[] recvl;
        byte Status1;
        if((recvl = this.send2Recv(Hs_Cmd.cmd_find, timeOut)) == null) {
            Status1 = 1;
        } else if(recvl[7] == 0 && recvl[8] == 0 && recvl[9] == -97) {
            Status1 = 0;
        } else {
            Status1 = 2;
        }

        return Status1;
    }

    private byte selectIDCard(long timeOut) {
        byte Status = 0;
        byte[] recvl = new byte[]{-86, -86, -86, -106, 105, 0, 3, 32, 2, 33};
        if((recvl = this.send2Recv(recvl, timeOut)) == null) {
            Status = 1;
        } else if(recvl[7] != 0 || recvl[8] != 0 || recvl[9] != -112 && recvl[9] != -127) {
            Status = 2;
        }

        return Status;
    }

    private String AnalyzeSAM(byte[] sambuffer) {
        if(sambuffer.length < 10) {
            return "";
        } else if(sambuffer[9] != -112) {
            return "设备SAM号读取错误";
        } else {
            byte[] samdate = new byte[4];
            System.arraycopy(sambuffer, 14, samdate, 0, 4);
            byte[] samtenid = new byte[4];
            System.arraycopy(sambuffer, 18, samtenid, 0, 4);
            byte[] samstr = new byte[4];
            System.arraycopy(sambuffer, 22, samstr, 0, 4);
            String samid = String.format("%02d.%02d-%010d-%010d-%010d", new Object[]{Byte.valueOf(sambuffer[10]), Byte.valueOf(sambuffer[12]), Long.valueOf(getLong(samdate)), Long.valueOf(getLong(samtenid)), Long.valueOf(getLong(samstr))});
            return samid;
        }
    }

    private static long getLong(byte[] buf) {
        long i = 0L;
        long tmp = 0L;

        for(int j = 0; j < buf.length; ++j) {
            tmp = (long)((buf[j] & 255) << j * 8);
            i |= tmp;
        }

        return i;
    }

    private byte[] send2Recv(byte[] lenLeft, long timeout) {
        byte[] recv = new byte[4096];
        boolean recvlen = false;
        byte[] rbuf = new byte[4096];
        boolean lenTmp = false;
        int count = 0;

        try {
            if(this.mInputStream.available() > 0) {
                this.mInputStream.read(rbuf);
            }

            this.mOutputStream.write(lenLeft);
            long var181 = System.currentTimeMillis();

            int var17;
            while(System.currentTimeMillis() - var181 < timeout) {
                if(this.mInputStream.available() > 0) {
                    var17 = this.mInputStream.read(rbuf);
                    System.arraycopy(rbuf, 0, recv, count, var17);
                    if((count += var17) > 7) {
                        break;
                    }

                    Thread.sleep(50L);
                }
            }

            if(recv[0] != -86 || recv[1] != -86 || recv[2] != -86 || recv[3] != -106 || recv[4] != 105) {
                return null;
            }

            int var16;
            if((var16 = (recv[5] << 8) + recv[6]) < 4) {
                return null;
            }

            int var13 = var16 + 7 - count;

            while(var13 > 0 && System.currentTimeMillis() - var181 < timeout) {
                if(this.mInputStream.available() > 0) {
                    var17 = this.mInputStream.read(rbuf);
                    System.arraycopy(rbuf, 0, recv, count, var17);
                    var13 -= var17;
                    count += var17;
                    Thread.sleep(50L);
                }
            }

            if(var13 > 0) {
                return null;
            }

            int var15 = var16 + 7 - 5;
            boolean var14 = true;
            lenLeft = recv;
            byte var3 = 0;

            for(var16 = 0; var16 < var15; ++var16) {
                var3 ^= lenLeft[var16 + 5];
            }

            if(var3 != 0) {
                return null;
            }
        } catch (NullPointerException var171) {
            recv = null;
        } catch (Exception var18) {
            recv = null;
        }

        return recv;
    }

    private byte[] send2RecvM1(byte[] lenLeft, long timeout, int Len) {
        byte[] recv = new byte[1024];
        boolean recvlen = false;
        byte[] rbuf = new byte[1024];
        boolean lenTmp = false;
        byte[] SendBuf = new byte[Len];
        System.arraycopy(lenLeft, 0, SendBuf, 0, Len);
        int count = 0;

        try {
            if(this.mInputStream.available() > 0) {
                this.mInputStream.read(rbuf);
            }

            this.mOutputStream.write(SendBuf);
            long var181 = System.currentTimeMillis();

            int var17;
            while(System.currentTimeMillis() - var181 < timeout) {
                if(this.mInputStream.available() > 0) {
                    var17 = this.mInputStream.read(rbuf);
                    System.arraycopy(rbuf, 0, recv, count, var17);
                    if((count += var17) > 7) {
                        break;
                    }

                    Thread.sleep(50L);
                }
            }

            if(recv[0] != -86 || recv[1] != -86 || recv[2] != -86 || recv[3] != -106 || recv[4] != 105) {
                return null;
            }

            int var16;
            if((var16 = (recv[5] << 8) + recv[6]) < 4) {
                return null;
            }

            for(int var13 = var16 + 7 - count; var13 > 0 && System.currentTimeMillis() - var181 < timeout; Thread.sleep(50L)) {
                if(this.mInputStream.available() > 0) {
                    var17 = this.mInputStream.read(rbuf);
                    System.arraycopy(rbuf, 0, recv, count, var17);
                    if((count += var17) > 7) {
                        break;
                    }
                }
            }
        } catch (NullPointerException var161) {
            recv = null;
        } catch (Exception var171) {
            recv = null;
        }

        return recv;
    }

    private void copy(Context context, String fileName, String saveName, String savePath) {
        File path = new File(savePath);
        if(!path.exists()) {
            path.mkdir();
        }

        try {
            File var12 = new File(savePath + "/" + saveName);
            if(var12.exists() && var12.length() > 0L) {
                Log.i("LU", saveName + "存在了");
                return;
            }

            FileOutputStream fos = new FileOutputStream(var12);
            InputStream inputStream = context.getResources().getAssets().open(fileName);
            byte[] buf = new byte[1024];
            boolean len = false;

            int len1;
            while((len1 = inputStream.read(buf)) != -1) {
                fos.write(buf, 0, len1);
            }

            fos.close();
            inputStream.close();
            Log.i("HSBlueApi", "copy: 解码库复制成功");
        } catch (Exception var121) {
            Log.i("LU", "IO异常");
        }

    }

    public int M1_Request(byte nMode, byte[] pSNR, byte[] pTagType) {
        byte[] CmdBuf = new byte[64];
        byte[] ReadBuf = new byte[1024];
        boolean nLen = false;
        if(nMode == 0) {
            CmdBuf[0] = 0;
            CmdBuf[1] = 97;
            CmdBuf[2] = 0;
        } else {
            CmdBuf[0] = 0;
            CmdBuf[1] = 97;
            CmdBuf[2] = 1;
        }

        byte nLen1 = 3;
        int Eor = this.M1Send(CmdBuf, nLen1, ReadBuf);
        if(Eor == 1) {
            if(ReadBuf[0] == 0 && ReadBuf[1] == 0 && ReadBuf[2] == -15) {
                return 0;
            } else {
                System.arraycopy(ReadBuf, 3, pSNR, 0, 4);
                if(pSNR[0] == 0 && pSNR[1] == 0 && pSNR[2] == 0 && pSNR[3] == 0) {
                    return 4;
                } else {
                    System.arraycopy(ReadBuf, 7, pTagType, 0, 2);
                    return 1;
                }
            }
        } else {
            return Eor;
        }
    }

    public int M1_Auth(byte nMode, byte[] pSNR, byte nBlock, byte[] KeyBuff) {
        byte[] CmdBuf = new byte[64];
        byte[] RetBuf = new byte[1024];
        boolean nLen = false;
        CmdBuf[0] = 0;
        CmdBuf[1] = 98;
        CmdBuf[2] = nMode;
        System.arraycopy(pSNR, 0, CmdBuf, 3, 4);
        CmdBuf[7] = nBlock;
        System.arraycopy(KeyBuff, 0, CmdBuf, 8, 6);
        byte nLen1 = 13;
        int Eor = this.M1Send(CmdBuf, nLen1, RetBuf);
        if(Eor == 1) {
            if(RetBuf[0] == 0 && RetBuf[1] == 0 && RetBuf[2] == -15) {
                return 0;
            } else {
                System.arraycopy(RetBuf, 3, pSNR, 0, 4);
                return pSNR[0] == 0 && pSNR[1] == 0 && pSNR[2] == 0 && pSNR[3] == 0?4:1;
            }
        } else {
            return Eor;
        }
    }

    public int M1_Read(byte nMode, byte[] pSNR, byte nBlock, byte[] KeyBuff, byte[] ReadBuff) {
        byte[] CmdBuf = new byte[64];
        byte[] RetBuf = new byte[1024];
        boolean nLen = false;
        CmdBuf[0] = 0;
        CmdBuf[1] = 99;
        CmdBuf[2] = nMode;
        System.arraycopy(pSNR, 0, CmdBuf, 3, 4);
        CmdBuf[7] = nBlock;
        System.arraycopy(KeyBuff, 0, CmdBuf, 8, 6);
        byte nLen1 = 14;
        int Eor = this.M1Send(CmdBuf, nLen1, RetBuf);
        if(Eor == 1) {
            if(RetBuf[0] == 0 && RetBuf[1] == 0 && RetBuf[2] == -15) {
                return 0;
            } else {
                System.arraycopy(RetBuf, 3, pSNR, 0, 4);
                if(pSNR[0] == 0 && pSNR[1] == 0 && pSNR[2] == 0 && pSNR[3] == 0) {
                    return 4;
                } else {
                    System.arraycopy(RetBuf, 7, ReadBuff, 0, 16);
                    return 1;
                }
            }
        } else {
            return Eor;
        }
    }

    public int M1_Write(byte nMode, byte[] pSNR, byte nBlock, byte[] KeyBuff, byte[] WriteBuff) {
        byte[] CmdBuf = new byte[64];
        byte[] RetBuf = new byte[1024];
        boolean nLen = false;
        CmdBuf[0] = 0;
        CmdBuf[1] = 100;
        CmdBuf[2] = nMode;
        System.arraycopy(pSNR, 0, CmdBuf, 3, 4);
        CmdBuf[7] = nBlock;
        System.arraycopy(KeyBuff, 0, CmdBuf, 8, 6);
        System.arraycopy(WriteBuff, 0, CmdBuf, 14, 16);
        byte nLen1 = 30;
        int Eor = this.M1Send(CmdBuf, nLen1, RetBuf);
        if(Eor == 1) {
            if(RetBuf[0] == 0 && RetBuf[1] == 0 && RetBuf[2] == -15) {
                return 0;
            } else {
                System.arraycopy(RetBuf, 3, pSNR, 0, 4);
                return pSNR[0] == 0 && pSNR[1] == 0 && pSNR[2] == 0 && pSNR[3] == 0?4:1;
            }
        } else {
            return Eor;
        }
    }

    public int M1_Vaule(byte nMode, byte[] pSNR, byte nBlock, byte[] KeyBuff, byte nFuncType, byte[] SetBuff) {
        byte[] CmdBuf = new byte[64];
        byte[] RetBuf = new byte[1024];
        boolean nLen = false;
        CmdBuf[0] = 0;
        CmdBuf[1] = 100;
        CmdBuf[2] = nMode;
        System.arraycopy(pSNR, 0, CmdBuf, 3, 4);
        CmdBuf[7] = nBlock;
        System.arraycopy(KeyBuff, 0, CmdBuf, 8, 6);
        CmdBuf[14] = nFuncType;
        int var11 = 15;
        if(nFuncType == 3) {
            CmdBuf[15] = SetBuff[0];
            ++var11;
        } else if(nFuncType != 5) {
            System.arraycopy(SetBuff, 0, CmdBuf, 15, 4);
            var11 += 4;
        }

        int Eor = this.M1Send(CmdBuf, var11, RetBuf);
        if(Eor == 1) {
            if(RetBuf[0] == 0 && RetBuf[1] == 0 && RetBuf[2] == -15) {
                return 0;
            } else {
                System.arraycopy(RetBuf, 3, pSNR, 0, 4);
                if(pSNR[0] == 0 && pSNR[1] == 0 && pSNR[2] == 0 && pSNR[3] == 0) {
                    return 4;
                } else {
                    if(nFuncType == 5) {
                        System.arraycopy(RetBuf, 4, SetBuff, 0, 4);
                    }

                    return 1;
                }
            }
        } else {
            return Eor;
        }
    }

    public int M1Send(byte[] SendBuf, int nLen, byte[] ReadBuf) {
        boolean LenRead = false;
        boolean LenCmd = false;
        byte[] CmdHead = new byte[]{-86, -86, -86, -106, 105};
        byte[] tmpSendBuf = new byte[64];
        System.arraycopy(CmdHead, 0, tmpSendBuf, 0, CmdHead.length);
        tmpSendBuf[5] = 0;
        tmpSendBuf[6] = (byte)(nLen + 1);
        System.arraycopy(SendBuf, 0, tmpSendBuf, 7, nLen);
        int CHK_SUM = 0;

        for(int rbuf = 0; rbuf < nLen + 2; ++rbuf) {
            CHK_SUM ^= tmpSendBuf[5 + rbuf];
        }

        tmpSendBuf[7 + nLen] = (byte)CHK_SUM;
        int var12 = 7 + nLen + 1;
        byte[] var13 = new byte[1024];
        byte[] recv = new byte[1024];
        var13 = this.send2RecvM1(tmpSendBuf, 3000L, var12);
        if(var13.length < 8) {
            return 2;
        } else {
            System.arraycopy(var13, 0, recv, 0, var13.length);
            int var11 = recv[5] * 256 + recv[6] + 7;
            if(var11 > 8) {
                System.arraycopy(recv, 7, ReadBuf, 0, var11 - 8);
                return 1;
            } else {
                return 4;
            }
        }
    }
}
