package com.gesturexr.nativeapp;

import android.app.*;import android.content.*;import android.graphics.*;import android.media.projection.*;import android.os.*;import android.view.*;import android.widget.*;

public class MainActivity extends Activity {
    static final int CAPTURE=1001; FrameView view;
    @Override public void onCreate(Bundle b){super.onCreate(b); view=new FrameView(this); setContentView(view);}
    void requestCapture(){ MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE); startActivityForResult(m.createScreenCaptureIntent(),CAPTURE); }
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==CAPTURE&&c==RESULT_OK){ScreenCaptureService.start(this,c,d);view.running=true;view.invalidate();}}
    class FrameView extends View{
        Paint p=new Paint(Paint.FILTER_BITMAP_FLAG); Bitmap bmp; boolean running; float x=60,y=140; float downX,downY; boolean drag;
        FrameView(Context c){super(c);p.setTextSize(42);setBackgroundColor(Color.rgb(12,12,18));}
        @Override protected void onDraw(Canvas c){super.onDraw(c); if(!running){p.setColor(Color.WHITE);c.drawText("Gesture XR",40,70,p);p.setTextSize(24);c.drawText("Native Android prototype",40,110,p);p.setColor(Color.rgb(124,77,255));c.drawRoundRect(40,150,430,240,22,22,p);p.setColor(Color.WHITE);c.drawText("Share phone screen",70,207,p);return;} byte[] f=ScreenCaptureService.getFrame();int w=ScreenCaptureService.getWidth(),h=ScreenCaptureService.getHeight(); if(f!=null&&w>0&&h>0){bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bmp.copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(f));} if(bmp!=null){float maxW=getWidth()-40,maxH=getHeight()-160,s=Math.min(maxW/bmp.getWidth(),maxH/bmp.getHeight());float rw=bmp.getWidth()*s,rh=bmp.getHeight()*s;c.drawBitmap(bmp,null,new RectF(x,y,x+rw,y+rh),p);} p.setColor(Color.WHITE);p.setTextSize(18);c.drawText("Pinch/drag-ready screen window prototype",24,getHeight()-30,p);postInvalidateDelayed(66);}
        @Override public boolean onTouchEvent(android.view.MotionEvent e){switch(e.getActionMasked()){case 0:downX=e.getX();downY=e.getY();drag=true;return true;case 2:if(drag){x+=e.getX()-downX;y+=e.getY()-downY;downX=e.getX();downY=e.getY();invalidate();}return true;case 1:drag=false;return true;}return true;}
    }
}
