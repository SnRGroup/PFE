package com.legsim.snr_cardboard;

import android.util.Log;

import java.util.LinkedList;

/**
 * Created by legsim on 20/06/16.
 */
public class HeadHistory {

    LinkedList<int[]> list;

    public HeadHistory() {
        list = new LinkedList();
    }

    public void addPosition(int x, int y) {
        int[] pos = new int[]{x,y};
        list.add(pos);
        if (list.size() > 50) {
            list.pop();
        }
    }

    public int[] getAverage() {
        int x = 0;
        int y = 0;

        for (int[] pos : list) {
            x+=pos[0];
            y+=pos[1];
        }

        x/=list.size();
        y/=list.size();

        return new int[]{x,y};
    }

    public boolean isStable() {
        int[] avg = this.getAverage();
        for (int[] pos : list) {
            int distance = (int)Math.round(Math.sqrt(Math.pow(pos[0]-avg[0],2)+Math.pow(pos[1]-avg[1],2)));
            if (distance >= 20) {
                return false;
            }
        }
        return true;
    }

}
