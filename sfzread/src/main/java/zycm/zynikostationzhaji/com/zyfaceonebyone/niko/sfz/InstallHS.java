package zycm.zynikostationzhaji.com.zyfaceonebyone.niko.sfz;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.util.Log;

import com.huashi.otg.sdk.HSIDCardInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;



/**
 * @author hww
 * @date 2019-09-29.
 * email：
 * description：
 */
public class InstallHS {


    private HsSerialPortSDK hsSerialPortSDK;
    private String filePath = "";
    private static final String SERIAL_PORT = "/dev/ttyS1";
    private static final int BAUD = 115200;
    private Handler handler = new Handler();
    private IdCardCallBack idCardCallBack;
    private Thread thread;
    private boolean test = true;

    public InstallHS(Context context, IdCardCallBack idCardCallBack) {
        this.idCardCallBack = idCardCallBack;
        filePath = context.getFilesDir().getPath() + "/wltlib";
        try {
            hsSerialPortSDK = new HsSerialPortSDK(context, filePath);
        } catch (Exception e) {
            this.idCardCallBack.initFailed("exception: " + e.toString());
            e.printStackTrace();
        }

        int ret = hsSerialPortSDK.init(SERIAL_PORT, BAUD, 0);
        if (0 == ret) {
            this.idCardCallBack.initSuccess();

            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    readIdCard();
                }
            });
            thread.start();


        } else {
            this.idCardCallBack.initFailed("init fail  ret = " + ret);
        }


    }


    /**
     * 身份证
     */
    private static final String TYPE_CARD_SFZ = " ";
    /**
     * 港澳台居住证
     */
    private static final String TYPE_CARD_GATJZZ = "J";
    /**
     * 外国人永久居留证
     */
    private static final String TYPE_CARD_WGRYJJLZ = "I";

    private int testSum = 0;
    private long testTimeSum = 0;
    private long testTimeCurrent = 0;

    private boolean threadSwitch = true;//线程开关
    private boolean isReadIdCardSwitch = true;//是否继续读卡

    private void readIdCard() {
        while (threadSwitch) {
            try {
                //idCardHeart();
                if (isReadIdCardSwitch && null != hsSerialPortSDK) {
                    idCardHeart();
                    testTimeCurrent = System.currentTimeMillis();
                    int auth = hsSerialPortSDK.Authenticate(200);

//                  if (0 == auth) {
                    HSIDCardInfo ic = new HSIDCardInfo();
                    int ret = hsSerialPortSDK.Read_Card(ic, 2300);
                    if (0 == ret) {
                        byte[] fpDate = new byte[1024];
                        fpDate = ic.getFpDate();

                        if (fpDate[4] == (byte) 0x01) {
                            /*m_FristPFInfo = String.format("指纹  信息：第一枚指纹注册成功。指位：%s。指纹质量：%d \n", GetFPcode(fpDate[5]), fpDate[6]);*/
                        } else {
                            /*m_FristPFInfo = "身份证无指纹 \n";*/
                        }
                        if (fpDate[512 + 4] == (byte) 0x01) {
                            /*m_SecondPFInfo = String.format("指纹  信息：第二枚指纹注册成功。指位：%s。指纹质量：%d \n", GetFPcode(fpDate[512 + 5]), fpDate[512 + 6]);*/
                        } else {
                            /*m_SecondPFInfo = "身份证无指纹 \n";*/
                        }

                        String type = ic.getcertType();
                        if (type == TYPE_CARD_SFZ) {
                            /*身份证*/
                        /*"证件类型：身份证\n" + "姓名：" + ic.getPeopleName() + "\n" + "性别：" + ic.getSex() + "\n" + "民族：" + ic.getPeople() + "\n" + "出生日期：" + df.format(ic.getBirthDay()) + "\n"
                                + "地址：" + ic.getAddr() + "\n" + "身份号码：" + ic.getIDCard()+ "\n" + "签发机关：" + ic.getDepartment() + "\n" + "有效期限：" + ic.getStrartDate() + "-" + ic.getEndDate() + "\n" + m_FristPFInfo + "\n" + m_SecondPFInfo*/
                        } else {
                            if (type == TYPE_CARD_GATJZZ) {
                                /*港澳台居住证*/
                            /*"证件类型：港澳台居住证（J）\n" + "姓名：" + ic.getPeopleName() + "\n" + "性别：" + ic.getSex() + "\n"
                                    + "签发次数：" + ic.getissuesNum() + "\n" + "通行证号码：" + ic.getPassCheckID() + "\n" + "出生日期：" + df.format(ic.getBirthDay())
                                    + "\n" + "地址：" + ic.getAddr() + "\n" + "身份号码：" + ic.getIDCard() + "\n" + "签发机关：" + ic.getDepartment() + "\n" + "有效期限：" + ic.getStrartDate() + "-" + ic.getEndDate() + "\n" + m_FristPFInfo + "\n" + m_SecondPFInfo);*/
                            } else {
                                if (type == TYPE_CARD_WGRYJJLZ) {
                                    /*外国人永久居留证*/
                                /*"证件类型：外国人永久居留证（I）\n"+ "英文名称：" + ic.getPeopleName() + "\n" + "中文名称：" + ic.getstrChineseName() + "\n" + "性别：" + ic.getSex() + "\n"
                                        + "永久居留证号：" + ic.getIDCard() + "\n"+ "国籍：" + ic.getstrNationCode() + "\n" + "出生日期：" + df.format(ic.getBirthDay()) + "\n"
                                        + "证件版本号：" + ic.getstrCertVer() + "\n" + "申请受理机关：" + ic.getDepartment() + "\n" + "有效期限：" + ic.getStrartDate() + "-" + ic.getEndDate() + "\n" + m_FristPFInfo + "\n" + m_SecondPFInfo*/
                                }
                            }
                        }

                        Log.e("hww", "type = " + type);
                        if (type == TYPE_CARD_SFZ || type == TYPE_CARD_GATJZZ || type == TYPE_CARD_WGRYJJLZ) {
                            try {
                                int unPack = hsSerialPortSDK.Unpack(ic.getwltdata());// 照片解码

                                if (unPack != 0) {// 读卡失败  //unPack != 0
                                    idCardCallBack.readCardFailed(-6, "头像解码失败 " + unPack);
                                } else {
                                    FileInputStream fis = new FileInputStream(filePath + "/zp.bmp");
                                    Bitmap bmp = BitmapFactory.decodeStream(fis);
                                    fis.close();
                                    idCardCallBack.readCardSuccess(ic, bmp);
                                    long current = (System.currentTimeMillis() - testTimeCurrent);
                                    testTimeSum = testTimeSum + current;
                                    testSum++;

                                    if (testSum > 200) {
                                        testSum = 0;
                                        testTimeSum = 0;
                                    } else {
                                        Log.i("hww", "平均时间:" + (testTimeSum / testSum) + "   当前消耗时间:" + current + "   累计次数:" + testSum + "   累计时间(ms):" + testTimeSum);
                                    }
                                }

                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                                idCardCallBack.readCardFailed(-1, e.toString());
                            } catch (IOException e) {
                                e.printStackTrace();
                                idCardCallBack.readCardFailed(-2, e.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                                idCardCallBack.readCardFailed(-3, e.toString());
                            }
                        }
                    } else if (ret == -1) {//断开连接
                        idCardCallBack.readCardFailed(-4, "扫码器连接断开");
                    } else if (ret == -2) {//断电
                        idCardCallBack.readCardFailed(-5, "扫码器电源断开");
                    } else if (ret == -3) {//
                        idCardCallBack.readCardFailed(0, "寻卡失败，未找到身份证");
                    } else {//出现异常
                        idCardCallBack.readCardFailed(-7, "读卡器出现异常" + ret);
                    }

                } else {

                }
            } catch (Exception e) {
                e.printStackTrace();
                idCardCallBack.readCardException(e.toString());
                break;
            } finally {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }

        }
    }

    public void cancelRead() {
        if (handler != null) {
            threadSwitch = false;
            if (thread != null) {
                thread.interrupt(); //该方法需要配合异常抛出break来结合使用，此处不结束线程，暂时不写
                thread = null;
            }

        }
        try {
            if (hsSerialPortSDK != null) {
                hsSerialPortSDK.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 指纹 指位代码
     *
     * @param FPcode
     * @return
     */
    String GetFPcode(int FPcode) {
        switch (FPcode) {
            case 11:
                return "右手拇指";
            case 12:
                return "右手食指";
            case 13:
                return "右手中指";
            case 14:
                return "右手环指";
            case 15:
                return "右手小指";
            case 16:
                return "左手拇指";
            case 17:
                return "左手食指";
            case 18:
                return "左手中指";
            case 19:
                return "左手环指";
            case 20:
                return "左手小指";
            case 97:
                return "右手不确定指位";
            case 98:
                return "左手不确定指位";
            case 99:
                return "其他不确定指位";
            default:
                return "未知";
        }
    }

    //将逗号分隔的字符串转换为byte数组
    public int String2byte(byte[] b, String StrBuf) {
        String[] parts = StrBuf.split(",");
        int Itmp;
        int Len = parts.length;
        if (Len == b.length) {
            for (int i = 0; i < Len; i++) {
                try {
                    Itmp = Integer.valueOf(parts[i], 16);
                    b[i] = (byte) Itmp;
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            return Len;
        }
        return -1;
    }


    public interface IdCardCallBack {
        void readCardException(String exception);

        void initSuccess();

        void initFailed(String message);

        void readCardSuccess(HSIDCardInfo idCardInfo, Bitmap headImg);

        void readCardFailed(int status, String msg);

        void readCardHeart(int msg);
    }

    public void setReadIdCardSwitch(boolean readIdCardSwitch) {
        isReadIdCardSwitch = readIdCardSwitch;
    }

    public boolean isReadIdCardSwitch() {
        return isReadIdCardSwitch;
    }

    public boolean isHeart = true;

    public void idCardHeart() {
        if (isHeart) {
            isHeart = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(3500);

                        idCardCallBack.readCardHeart(1);//心跳
                        isHeart = true;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        }

    }

}
