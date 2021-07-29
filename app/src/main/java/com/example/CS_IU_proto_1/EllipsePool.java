package com.example.CS_IU_proto_1;

import java.util.ArrayList;

public class EllipsePool {
    private int totalCount;
    public int useCount;

    public ArrayList<DrawEllipse> drawEllipses;


    public EllipsePool(int _count) {
        totalCount = _count;
        useCount = 0;
        drawEllipses = new ArrayList<>();
        for(int i = 0; i < _count; i++)
            drawEllipses.add(new DrawEllipse());
    }

    public void setEllipse(Ellipse ellipse){
        drawEllipses.get(useCount).setContour(ellipse);
        useCount++;
    }

    public void addEllipse(Ellipse ellipse){
        totalCount++;
        useCount++;
        DrawEllipse drawEllipse = new DrawEllipse();
        drawEllipse.setContour(ellipse);
        drawEllipses.add(drawEllipse);
    }

    public void clear(){
        useCount = 0;
    }

    public boolean isFull(){
        return totalCount == useCount;
    }

}
