package com.gesturexr.nativeapp;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {
    static final String START="START", STOP="STOP";
    static volatile byte[] frame; static volatile int width, height;
    MediaProjection projection; VirtualDisplay display; ImageReader reader;

    public static void start(Context c,int code,Intent data){
        Intent i=new Intent(c,ScreenCaptureService.class).setAction(START).putExtra("code",code).putExtra("data",data);
        if(Build.VERSION.SDK_INT>=26)c.startForegroundService(i); else c.startService(i);
    }
    public static byte[] getFrame(){ return frame; }
    public static int getWidth(){ return width; }
    public static int getHeight(){ return height; }

    @Override public void onCreate(){ super.onCreate();
        if(Build.VERSION.SDK_INT>=26){
            NotificationManager nm=getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel("capture","Screen capture",NotificationManager.IMPORTANCE_LOW));
        }
    }
    @Override public int onStartCommand(Intent i,int flags,int id){
        if(i!=null && STOP.equals(i.getAction())){ stopCapture(); stopSelf(); return START_NOT_STICKY; }
        if(i!=null && START.equals(i.getAction())){
            Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"capture"):new Notification.Builder(this);
            startForeground(4173,b.setContentTitle("Gesture XR").setContentText("Screen sharing is active").setSmallIcon(android.R.drawable.ic_menu_view).build());
            startCapture(i);
        }
        return START_NOT_STICKY;
    }
    void startCapture(Intent i){
        stopCapture();
        MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        Intent data=i.getParcelableExtra("data");
        projection=m.getMediaProjection(i.getIntExtra("code",0),data);
        if(projection==null)return;
        int sw=getResources().getDisplayMetrics().widthPixels, sh=getResources().getDisplayMetrics().heightPixels;
        float s=Math.min(1f,Math.min(1280f/sw,720f/sh));
        width=Math.max(2,((int)(sw*s))&~1); height=Math.max(2,((int)(sh*s))&~1);
        reader=ImageReader.newInstance(width,height,PixelFormat.RGBA_8888,2);
        reader.setOnImageAvailableListener(r->{ Image im=null; try{
            im=r.acquireLatestImage(); if(im==null)return; Image.Plane p=im.getPlanes()[0]; ByteBuffer buf=p.getBuffer();
            int rowStride=p.getRowStride(), pixelStride=p.getPixelStride(); byte[] out=new byte[width*height*4]; byte[] row=new byte[rowStride];
            for(int y=0;y<height;y++){ int n=Math.min(rowStride,buf.remaining()); buf.get(row,0,n); for(int x=0;x<width;x++){int src=x*pixelStride,dst=(y*width+x)*4;if(src+3<n){out[dst]=row[src];out[dst+1]=row[src+1];out[dst+2]=row[src+2];out[dst+3]=row[src+3];}}}
            frame=out;
        }catch(Throwable ignored){}finally{if(im!=null)im.close();}},null);
        display=projection.createVirtualDisplay("GestureXR",width,height,getResources().getDisplayMetrics().densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader.getSurface(),null,null);
    }
    void stopCapture(){ if(display!=null){display.release();display=null;} if(reader!=null){reader.close();reader=null;} if(projection!=null){projection.stop();projection=null;} frame=null; }
    @Override public void onDestroy(){stopCapture();super.onDestroy();}
    @Override public IBinder onBind(Intent i){return null;}
}
