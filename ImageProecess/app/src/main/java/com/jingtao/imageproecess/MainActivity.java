package com.jingtao.imageproecess;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class MainActivity extends Activity {
    private static final String TAG = "Touch";
    private RelativeLayout container;
    private ImageView myimage;
    private int pickPicture = 0;
    //These matrices will be used to move and zoom image
    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    static final int DRAW =3;
    int mode = NONE;

    private int x,y,a,b;

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
    private int displaywidth;
    private int displayheight;
    private PointF last = new PointF();

    private int penWidth=10;

    //variable for counting two successive up-down events
    int clickCount = 0;
    //variable for storing the time of first click
    long startTime;
    //variable for calculating the total time
    long duration;
    //constant for defining the time duration between the click that can be considered as double-tap
    static final int MAX_DURATION = 500;

    private Path drawPath;
    //drawing and canvas paint
    private Paint drawPaint, canvasPaint, shadow, rec, frame;
    //initial color
    private String paintColor = "#ffff33";
    //canvas
    private Canvas canvas,origin_canvas,crop_canvas;
    //canvas bitmap
    private Bitmap canvas_bitmap,origin_bitmap,previous_bitmap,crop_bitmap;


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if(hasFocus){
            myimage = (ImageView)findViewById(R.id.img);
            RectF drawableRect = new RectF(0, 0,width, height);
            RectF viewRect = new RectF(0, 0, container.getWidth(), container.getHeight());
            matrix.setRectToRect(drawableRect, viewRect, Matrix.ScaleToFit.CENTER);
            myimage.setImageMatrix(matrix);
            hide_setting();
            hide_crop();

        }
        //Here you can get the size!
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myimage = (ImageView)findViewById(R.id.img);
        container = (RelativeLayout) findViewById(R.id.container);
        maxZoom = 5f;
        minZoom = 0.3f;
        height = myimage.getDrawable().getIntrinsicHeight()+20;
        width = myimage.getDrawable().getIntrinsicWidth()+20;
        viewRect = new RectF(0, 0, myimage.getWidth()+20, myimage.getHeight()+20);
        matrix = new Matrix(myimage.getImageMatrix());
        savedMatrix = new Matrix(myimage.getImageMatrix());
        myimage.setOnTouchListener(image_scale);
        origin_bitmap=((BitmapDrawable)myimage.getDrawable()).getBitmap();
        previous_bitmap=origin_bitmap.copy(Bitmap.Config.ARGB_8888, true);
        final ImageButton hand_btn = (ImageButton)findViewById(R.id.hand);
        final ImageButton marker_btn = (ImageButton)findViewById(R.id.marker);
        final ImageButton crop_btn = (ImageButton)findViewById(R.id.crop);
        crop_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(canvas == null){
                    Bitmap workingBipmap = ((BitmapDrawable)myimage.getDrawable()).getBitmap();
                    canvas_bitmap = workingBipmap.copy(Bitmap.Config.ARGB_8888, true);
                    canvas = new Canvas(canvas_bitmap);
                }
                crop_bitmap = canvas_bitmap.copy(Bitmap.Config.ARGB_8888, true);
                crop_canvas = new Canvas(crop_bitmap);

                hand_btn.setBackgroundColor(Color.parseColor("#00000000"));
                crop_btn.setBackgroundColor(Color.parseColor("#2928dd"));
                marker_btn.setBackgroundColor(Color.parseColor("#00000000"));
                hide_setting();
                x = canvas.getWidth() / 3;
                y = canvas.getHeight() / 3;
                a = canvas.getWidth() * 2 / 3;
                b = canvas.getHeight() * 2 / 3;
                shadow = new Paint();
                shadow.setStyle(Paint.Style.FILL);
                shadow.setColor(Color.parseColor("#000000"));
                shadow.setStrokeWidth(50);
                shadow.setAlpha(80);
                crop_canvas.drawRect(1, 1, canvas.getWidth(), canvas.getHeight(), shadow);

                rec = new Paint();
                rec.setStyle(Paint.Style.FILL);
                rec.setColor(Color.parseColor("#ffffff"));
                rec.setStrokeWidth(50);
                rec.setAlpha(110);
                crop_canvas.drawRect(x, y, a, b, rec);

                frame = new Paint();
                frame.setStyle(Paint.Style.STROKE);
                frame.setColor(Color.parseColor("#000000"));
                frame.setStrokeWidth(20);
                frame.setAlpha(130);
                crop_canvas.drawRect(x,y,a,b, frame);
                myimage.setImageBitmap(crop_bitmap);
                myimage.setOnTouchListener(image_crop);
                hide_setting();
                show_crop();
            }
        });
        hand_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(canvas_bitmap==null) {
                    Bitmap workingBipmap = ((BitmapDrawable) myimage.getDrawable()).getBitmap();
                    canvas_bitmap = workingBipmap.copy(Bitmap.Config.ARGB_8888, true);
                    canvas = new Canvas(canvas_bitmap);
                }else{
                    myimage.setImageBitmap(canvas_bitmap);
                }
                myimage.setOnTouchListener(image_scale);
                hand_btn.setBackgroundColor(Color.parseColor("#2928dd"));
                marker_btn.setBackgroundColor(Color.parseColor("#00000000"));
                crop_btn.setBackgroundColor(Color.parseColor("#00000000"));
                hide_setting();
                hide_crop();
            }
        });
        marker_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(canvas_bitmap==null) {
                    Bitmap workingBipmap = ((BitmapDrawable) myimage.getDrawable()).getBitmap();
                    canvas_bitmap = workingBipmap.copy(Bitmap.Config.ARGB_8888, true);
                    canvas = new Canvas(canvas_bitmap);
                }else{
                    myimage.setImageBitmap(canvas_bitmap);
                }
                drawPath = new Path();
                drawPaint = new Paint();
                setPaint();
                myimage.setOnTouchListener(image_draw);
                hand_btn.setBackgroundColor(Color.parseColor("#00000000"));
                crop_btn.setBackgroundColor(Color.parseColor("#00000000"));
                marker_btn.setBackgroundColor(Color.parseColor("#2928dd"));
                show_setting();
                hide_crop();
            }
        });
        Button select = (Button)findViewById(R.id.select);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto, pickPicture);
            }
        });
        setup_btns();
    }

    private float getTouchX(float touchx){
        float[] values = new float[9];
        matrix.getValues(values);
        float globalX = values[Matrix.MTRANS_X];
        float imgwidth = values[Matrix.MSCALE_X]*(width-20);
        return (touchx-globalX)*((float)canvas_bitmap.getWidth()/imgwidth);
    }


    private float getTouchY(float touchy){
        float[] values = new float[9];
        matrix.getValues(values);
        float globalY = values[Matrix.MTRANS_Y];
        float imgheight = values[Matrix.MSCALE_Y]*(height-20);
        return (touchy-globalY)*((float)canvas_bitmap.getHeight()/imgheight);
    }

    View.OnTouchListener image_crop = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            PointF curr = new PointF(event.getX(),event.getY());
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    start.set(event.getX(), event.getY());
                    last.set(curr);
                    float touch_x = getTouchX(curr.x);
                    float touch_y = getTouchY(curr.y);
                    Log.e("MOTION","x: "+x+"  y:"+y +"  a:"+a+"  b:"+b+" X "+curr.x+" Y"+curr.y+" TX:"+touch_x+" TY:"+touch_y);
                    if(touch_x > x && touch_y > y && touch_x < a && touch_y < b){
                        mode = DRAG;
                    }else{
                        mode = NONE;
                    }

                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        float dX = curr.x - last.x;
                        float dY = curr.y - last.y;
                        x=x+(int)dX;
                        a=a+(int)dX;
                        y=y+(int)dY;
                        b=b+(int)dY;
                        crop_bitmap = canvas_bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        crop_canvas = new Canvas(crop_bitmap);
                        crop_canvas.drawRect(1, 1, canvas.getWidth(), canvas.getHeight(), shadow);
                        crop_canvas.drawRect(x, y, a, b, rec);
                        crop_canvas.drawRect(x,y,a,b, frame);
                        myimage.setImageBitmap(crop_bitmap);


                    }
                    break;
            }
            return true;
        }
    };
    View.OnTouchListener image_draw = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event){
            try{
                float[] values = new float[9];
                matrix.getValues(values);
                float globalX = values[Matrix.MTRANS_X];
                float globalY = values[Matrix.MTRANS_Y];
                float imgwidth = values[Matrix.MSCALE_X]*(width-20);
                float imgheight = values[Matrix.MSCALE_Y]*(height-20);
                float touchX = (event.getX()-globalX)*((float)canvas_bitmap.getWidth()/imgwidth);
                float touchY = (event.getY()-globalY)*((float)canvas_bitmap.getHeight()/imgheight);
                Log.e("info", "draw: X: " + touchX + " Y: " + touchY);
                Log.e("info", "draw: imagewidth: " + myimage.getWidth() + " imageheight: " + myimage.getHeight());

                float touch_x = getTouchX(event.getX());
                float touch_y = getTouchY(event.getY());
                PointF curr = new PointF(touch_x,touch_y);
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        start.set(touch_x, touch_y);
                        last.set(curr);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        canvas.drawLine(last.x,last.y,curr.x,curr.y,drawPaint);
                        last.set(curr);
                        //canvas.drawPath(drawPath, drawPaint);
                        //myimage.setImageBitmap(canvas_bitmap);
                        //drawPath.reset();
                        //drawPath.moveTo(touchX, touchY);
                        break;
                    case MotionEvent.ACTION_UP:
                        canvas.drawLine(last.x,last.y,curr.x,curr.y,drawPaint);
                        last.set(curr);
                        break;
                    default:
                        return false;
                }

                myimage.setImageBitmap(canvas_bitmap);
                //respond to down, move and up events
                /*switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        drawPath.moveTo(touchX, touchY);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        drawPath.lineTo(touchX, touchY);
                        //canvas.drawPath(drawPath, drawPaint);
                        //myimage.setImageBitmap(canvas_bitmap);
                        //drawPath.reset();
                        //drawPath.moveTo(touchX, touchY);
                        break;
                    case MotionEvent.ACTION_UP:
                        previous_bitmap=canvas_bitmap.copy(Bitmap.Config.ARGB_8888, true);
                        drawPath.lineTo(touchX, touchY);
                        canvas.drawPath(drawPath, drawPaint);
                        myimage.setImageBitmap(canvas_bitmap);
                        drawPath.reset();
                        break;
                    default:
                        return false;
                }*/
            //Log.e("draw",drawPath.toString());
            //redra
            }catch(Exception e){
                e.printStackTrace();
            }

            return true;
        }
    };

    View.OnTouchListener image_scale = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            PointF curr = new PointF(event.getX(), event.getY());
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    Log.e(TAG, "mode=DRAG");
                    mode = DRAG;
                    last.set(curr);
                    if (clickCount == 0) {
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
                    if (clickCount == 2) {
                        if (time <= MAX_DURATION) {
                            matrix.getValues(matrixValues);
                            float currentScale = matrixValues[Matrix.MSCALE_X];
                            Log.e(TAG, "duoble tap, duration, current scale "+currentScale );
                            if(maxZoom - currentScale < currentScale - minZoom){
                                //clsoe to maxZoom
                                matrix.postScale(minZoom/currentScale, minZoom/currentScale, event.getX(), event.getY());
                            }else{
                                //close to minZoom
                                matrix.postScale(maxZoom/currentScale, maxZoom/currentScale, event.getX(), event.getY());
                            }
                            currentScale = matrixValues[Matrix.MSCALE_X];
                            Log.e(TAG, "after duoble tap, duration, current scale "+currentScale );
                            fix();
                            currentScale = matrixValues[Matrix.MSCALE_X];
                            Log.e(TAG, "after fix tap, duration, current scale " + currentScale);
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
                    if (mode == DRAW) {
                        onTouchEvent(event);
                    }
                    if (mode == DRAG) {
                        float deltaX = curr.x - last.x;
                        float deltaY = curr.y - last.y;
                        matrix.postTranslate(deltaX, deltaY);
                        if(deltaX > 10 || deltaY > 10) clickCount = 0;
                        matrix.getValues(matrixValues);
                        last.set(curr.x, curr.y);
                        fix();
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        Log.e(TAG, "newDist=" + newDist + " oldDist" + oldDist);
                        if (newDist > 10f) {
                            matrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            matrix.getValues(matrixValues);
                            float currentScale = matrixValues[Matrix.MSCALE_X];
                            // limit zoom
                            if (scale * currentScale > maxZoom) {
                                scale = maxZoom / currentScale;
                            } else if (scale * currentScale < minZoom) {
                                scale = minZoom / currentScale;
                            }
                            matrix.postScale(scale, scale, mid.x, mid.y);
                            fix();
                        }
                        clickCount = 0;
                    }
                    break;
            }
            myimage.setImageMatrix(matrix);
            return true; // indicate event was handled
        }
    };
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

    private void fix(){
        float[] values = new float[9];
        matrix.getValues(values);
        float globalX = values[Matrix.MTRANS_X];
        float globalY = values[Matrix.MTRANS_Y];
        float imgwidth = values[Matrix.MSCALE_X]*width;
        float imgheight = values[Matrix.MSCALE_Y]*height;
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        displaywidth = size.x;
        displayheight = size.y;
        Log.e(TAG, "globalX: " + globalX + " globalY:" + globalY);
        Log.e(TAG, "img width: " + imgwidth + " img height:" + imgheight);
        if(globalX< -imgwidth/2){
            float fix=-globalX-imgwidth/2;
            matrix.postTranslate(fix, 0);
        }
        if(globalY< -imgheight/2){
            float fix=-globalY-imgheight/2;
            matrix.postTranslate(0, fix);
        }
        if(displaywidth - globalX < imgwidth/2){
            float fix=(displaywidth-imgwidth/2) - globalX;
            matrix.postTranslate(fix, 0);
        }
        if(displayheight - globalY < imgheight/2){
            float fix=(displayheight-imgheight/2) - globalY;
            matrix.postTranslate(0, fix);
        }
    }


    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        switch (requestCode) {
            case 0://pick from gellary
                if(resultCode == RESULT_OK){
                    Uri selectedImage = intent.getData();
                    Bitmap bitmap_large;
                    try {
                        bitmap_large = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                        myimage.setImageBitmap(bitmap_large);
                        matrix = new Matrix(myimage.getImageMatrix());
                        savedMatrix = new Matrix(myimage.getImageMatrix());
                        height = myimage.getDrawable().getIntrinsicHeight()+20;
                        width = myimage.getDrawable().getIntrinsicWidth()+20;
                        fix();
                    } catch (Exception e) {
                        Log.e("exception", e.toString()+" OnActivityResult");
                    }
                    final ImageButton hand_btn = (ImageButton)findViewById(R.id.hand);
                    final ImageButton marker_btn = (ImageButton)findViewById(R.id.marker);
                    final ImageButton crop_btn = (ImageButton)findViewById(R.id.crop);
                    myimage.setOnTouchListener(image_scale);
                    hand_btn.setBackgroundColor(Color.parseColor("#2928dd"));
                    marker_btn.setBackgroundColor(Color.parseColor("#00000000"));
                    crop_btn.setBackgroundColor(Color.parseColor("#00000000"));
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    origin_bitmap = ((BitmapDrawable)myimage.getDrawable()).getBitmap();
                    canvas_bitmap = origin_bitmap.copy(Bitmap.Config.ARGB_8888, true);
                    canvas = new Canvas(canvas_bitmap);
                }
                break;
        }
    }

    private void setPaint(){
        drawPaint.setColor(Color.parseColor(paintColor));
        drawPaint.setAlpha(100);
        drawPaint.setAntiAlias(true);
        drawPaint.setDither(true);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);
        drawPaint.setStrokeWidth(penWidth);
    }

    private void hide_crop(){
        RelativeLayout crop = (RelativeLayout)findViewById(R.id.crop_saving);
        crop.setVisibility(View.GONE);
    }

    private void show_crop(){
        RelativeLayout crop = (RelativeLayout)findViewById(R.id.crop_saving);
        crop.setVisibility(View.VISIBLE);
    }

    private void hide_setting(){
        LinearLayout setting = (LinearLayout)findViewById(R.id.draw_setting);
        RelativeLayout pick = (RelativeLayout)findViewById(R.id.color_pick);
        SeekBar pen_width = (SeekBar)findViewById(R.id.pen_width_sb);
        pick.setVisibility(View.GONE);
        setting.setVisibility(View.GONE);
        pen_width.setVisibility(View.GONE);
    }
    private void show_pick(){
        RelativeLayout pick = (RelativeLayout)findViewById(R.id.color_pick);
        pick.setVisibility(View.VISIBLE);
        SeekBar pen_width = (SeekBar)findViewById(R.id.pen_width_sb);
        pen_width.setVisibility(View.GONE);
    }
    private void show_pen_width(){
        SeekBar pen_width = (SeekBar)findViewById(R.id.pen_width_sb);
        pen_width.setVisibility(View.VISIBLE);
        pen_width.setProgress(penWidth);
        RelativeLayout pick = (RelativeLayout)findViewById(R.id.color_pick);
        pick.setVisibility(View.GONE);
    }
    private void hide_pick(){
        RelativeLayout pick = (RelativeLayout)findViewById(R.id.color_pick);
        pick.setVisibility(View.GONE);
    }
    private void show_setting(){
        LinearLayout setting = (LinearLayout)findViewById(R.id.draw_setting);
        setting.setVisibility(View.VISIBLE);
        Button pick_color = (Button) findViewById(R.id.color);
        pick_color.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_pick();
            }
        });
        Button width_setting = (Button)findViewById(R.id.pen_width_setting);
        width_setting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                show_pen_width();
            }
        });
    }
    private void setup_btns(){
        SeekBar pen_width = (SeekBar)findViewById(R.id.pen_width_sb);
        pen_width.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        penWidth = progress;
                        drawPaint.setStrokeWidth(penWidth);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                }
        );
        final Button pick = (Button)findViewById(R.id.color);
        ImageButton red = (ImageButton)findViewById(R.id.red);
        red.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick.setBackgroundColor(Color.parseColor("#ff4d4d"));
                paintColor="#ff4d4d";
                drawPaint.setColor(Color.parseColor(paintColor));
                drawPaint.setAlpha(100);
                hide_pick();
            }
        });
        ImageButton yellow = (ImageButton)findViewById(R.id.yellow);
        yellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick.setBackgroundColor(Color.parseColor("#ffff33"));
                paintColor="#ffff33";
                drawPaint.setColor(Color.parseColor(paintColor));
                drawPaint.setAlpha(100);
                hide_pick();
            }
        });
        ImageButton blue = (ImageButton)findViewById(R.id.blue);
        blue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick.setBackgroundColor(Color.parseColor("#4d4dff"));
                paintColor="#4d4dff";
                drawPaint.setColor(Color.parseColor(paintColor));
                drawPaint.setAlpha(100);
                hide_pick();
            }
        });
        ImageButton black = (ImageButton)findViewById(R.id.black);
        black.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pick.setBackgroundColor(Color.parseColor("#262626"));
                paintColor="#262626";
                drawPaint.setColor(Color.parseColor(paintColor));
                drawPaint.setAlpha(100);
                hide_pick();
            }
        });
        Button reset_canvas = (Button)findViewById(R.id.resetDraw);
        reset_canvas.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previous_bitmap=canvas_bitmap.copy(Bitmap.Config.ARGB_8888, true);
                canvas_bitmap=origin_bitmap.copy(Bitmap.Config.ARGB_8888, true);
                myimage.setImageBitmap(canvas_bitmap);
                canvas = new Canvas(canvas_bitmap);
            }
        });
        Button undo_btn = (Button)findViewById(R.id.undo);
        undo_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canvas_bitmap=previous_bitmap.copy(Bitmap.Config.ARGB_8888, true);
                myimage.setImageBitmap(canvas_bitmap);
                canvas = new Canvas(canvas_bitmap);
            }
        });
    }
}
