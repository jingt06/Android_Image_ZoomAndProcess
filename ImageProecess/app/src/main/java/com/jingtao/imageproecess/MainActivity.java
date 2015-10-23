package com.jingtao.imageproecess;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "Touch";
    private RelativeLayout container;
    private ImageView myimage;
    //These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int DRAW =3;
    int mode = NONE;

    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;

    // Limit zoomable/pannable image
    private float[] matrixValues = new float[9];
    private float maxZoom;
    private float minZoom;
    private float height;
    private float width;
    private RectF viewRect;
    private PointF last = new PointF();

    //variable for counting two successive up-down events
    int clickCount = 0;
    //variable for storing the time of first click
    long startTime;
    //variable for calculating the total time
    long duration;
    //constant for defining the time duration between the click that can be considered as double-tap
    static final int MAX_DURATION = 500;


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus){
            myimage = (ImageView)findViewById(R.id.img);
            RectF drawableRect = new RectF(0, 0,width, height);
            RectF viewRect = new RectF(0, 0, container.getWidth(), container.getHeight());
            Log.e("debug",height+" "+width + " "+container.getWidth()+" "+
                    container.getHeight()+" "+myimage.getHeight()+" "+myimage.getWidth()+" "+
                    +myimage.getMeasuredHeight()+" "+myimage.getMeasuredWidth());
            matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
            myimage.setImageMatrix(matrix);
        }
        //Here you can get the size!
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myimage = (ImageView)findViewById(R.id.img);
        container = (RelativeLayout) findViewById(R.id.container);
        maxZoom = 15;
        minZoom = 0.5f;
        height = myimage.getDrawable().getIntrinsicHeight()+20;
        width = myimage.getDrawable().getIntrinsicWidth()+20;
        viewRect = new RectF(0, 0, myimage.getWidth()+20, myimage.getHeight()+20);
        matrix = new Matrix(myimage.getImageMatrix());
        savedMatrix = new Matrix(myimage.getImageMatrix());
        myimage.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                // Dump touch event to log
                //dumpEvent(event);
                // Handle touch events here...
                PointF curr = new PointF(event.getX(), event.getY());
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        savedMatrix.set(matrix);
                        start.set(event.getX(), event.getY());
                        Log.e(TAG, "mode=DRAG");
                        mode = DRAG;
                        last.set(curr);
                        if(clickCount==0) {
                            startTime = System.currentTimeMillis();
                        }
                        clickCount++;
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        oldDist = spacing(event);
                        Log.e(TAG, "oldDist=" + oldDist);
                        if (oldDist > 10f) {
                            savedMatrix.set(matrix);
                            midPoint(mid, event);
                            mode = ZOOM;
                            Log.e(TAG, "mode=ZOOM");
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                        long time = System.currentTimeMillis() - startTime;
                        if(clickCount == 2)
                        {
                            if(time<= MAX_DURATION)
                            {
                                Log.e(TAG,"duoble tap, duration: "+time);
                            }
                            clickCount = 0;
                            break;
                        }
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        mode = NONE;
                        Log.e(TAG, "mode=NONE");
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (mode == DRAW){ onTouchEvent(event);}
                        if (mode == DRAG) {
                            float deltaX = curr.x - last.x;
                            float deltaY = curr.y - last.y;
                            matrix.postTranslate(deltaX,deltaY);
                            last.set(curr.x, curr.y);
                        }
                        else if (mode == ZOOM) {
                            float newDist = spacing(event);
                            Log.e(TAG, "newDist=" + newDist+" oldDist"+ oldDist);
                            if (newDist > 10f) {
                                matrix.set(savedMatrix);
                                float scale = newDist / oldDist;
                                matrix.getValues(matrixValues);
                                float currentScale = matrixValues[Matrix.MSCALE_X];
                                // limit zoom
                                if (scale * currentScale > maxZoom) {
                                    scale = maxZoom / currentScale;
                                }else if(scale * currentScale < minZoom){
                                    scale = minZoom / currentScale;
                                }
                                Log.e("Scale",scale+"");
                                matrix.postScale(scale, scale, mid.x, mid.y);
                            }
                        }
                        break;
                }
                myimage.setImageMatrix(matrix);
                return true; // indicate event was handled
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }

    //************* Calculate the mid point of the first two fingers
    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }
}
