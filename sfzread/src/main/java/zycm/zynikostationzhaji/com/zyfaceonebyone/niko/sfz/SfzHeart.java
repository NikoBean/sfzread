package zycm.zynikostationzhaji.com.zyfaceonebyone.niko.sfz;

import java.util.Timer;
import java.util.TimerTask;

/**
 * AUTHOR:       Niko
 * VERSION:      V1.0
 * E-MAIL:       JAVADAD@163.COM
 * DESCRIPTION:  description
 * CREATE TIME:  2019-11-16 15:23
 * NOTE:
 */
public class SfzHeart {
    private Timer timer;
    private int heartNum = 0;
    private int heartTime;
    public SfzHeart(final SfzHeartListener sfzHeartListener,int heartTime) {
        this.heartTime = heartTime;
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                sfzHeartListener.theSfzReadCardDown();
            }
        }, heartTime, heartTime);
    }


    public void shutDownSfzHeart(){
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }




    public interface SfzHeartListener{
        void theSfzReadCardDown();
    }
}
