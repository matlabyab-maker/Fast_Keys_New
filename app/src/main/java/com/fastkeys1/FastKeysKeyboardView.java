package com.fastkeys1;

import android.app.AlertDialog;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.os.Handler;
import android.view.*;
import android.content.DialogInterface;
import android.widget.*;
import java.util.*;

public class FastKeysKeyboardView extends View {
    private final FastKeysInputMethodService service;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler = new Handler();
    private float gap, keyH;
    private boolean caps = false;
    private boolean magnifier = false;
    private float magnifierX = -1, magnifierY = -1;
    private Runnable repeat;
    // Visual key-press light; this changes only the pressed-key appearance.
    private boolean pressGlow = false;
    private boolean pressHeld = false;
    private float pressL, pressT, pressR, pressB;
    private final Runnable clearPressGlow = () -> {
        if (!pressHeld) {
            pressGlow = false;
            invalidate();
        }
    };
    private final String[] suggestions = new String[3];
    private static final String[][] WORDS = {
        {"سلام","سلامت","سلامتی"},{"من","منم","منطقه"},{"این","اینجا","اینجانب"},
        {"برای","برنامه","بررسی"},{"کیبورد","کیبوردی","کیبوردها"},{"است","استفاده","استان"},
        {"یک","یکی","یکم"},{"دارم","دارد","دارند"},{"می","میرم","میز"},{"خوب","خوبه","خوبی"},
        {"تایپ","تایپی","تایپ کردن"},{"کلمه","کلمات","کلمه‌های"}
    };
    private final int BG=Color.rgb(239,238,232), DEFAULT_KEY=Color.rgb(250,249,244),
            BLUE=Color.rgb(20,112,235), NAVY=Color.rgb(18,38,78), BLACK=Color.rgb(25,29,34),
            GREEN=Color.rgb(45,205,55);
    private int KEY;

    public FastKeysKeyboardView(FastKeysInputMethodService s){
        super(s);
        service=s;
        KEY = service.getSharedPreferences("fast_keys_settings", android.content.Context.MODE_PRIVATE)
                .getInt("keyboard_key_color", DEFAULT_KEY);
        setBackgroundColor(BG);
    }

    private void txt(Canvas c,String s,float x,float y,float size,int color){
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
        p.setTextSize(size);
        p.setColor(color);
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(s,x,y-(p.ascent()+p.descent())/2,p);
    }

    private void key(Canvas c,float l,float t,float r,float b,String label,int color,boolean square){
        p.setColor(KEY);
        p.setStyle(Paint.Style.FILL);
        float rad=square?3:7;
        c.drawRoundRect(l,t,r,b,rad,rad,p);
        p.setColor(Color.rgb(205,204,199));
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1);
        c.drawRoundRect(l,t,r,b,rad,rad,p);
        p.setStyle(Paint.Style.FILL);
        if(label!=null&&!label.isEmpty())
            txt(c,label,(l+r)/2,(t+b)/2,Math.min(22,(b-t)*.42f),color);
    }

    private void magnifierIcon(Canvas c,float cx,float cy,float size,boolean active){
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(2,size*.10f));
        p.setColor(active?GREEN:NAVY);
        c.drawCircle(cx-size*.10f,cy-size*.10f,size*.25f,p);
        c.drawLine(cx+size*.08f,cy+size*.08f,cx+size*.30f,cy+size*.30f,p);
        p.setStyle(Paint.Style.FILL);
    }

    private boolean drawerOpen = false;

    private void showDrawer() {
        final LinearLayout panel = new LinearLayout(service);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(24, 18, 24, 18);

        TextView title = new TextView(service);
        title.setText("امکانات");
        title.setTextSize(20);
        title.setTextColor(BLACK);
        title.setGravity(Gravity.CENTER);
        panel.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button transparency = drawerButton("شفافیت کیبورد");
        Button palette = drawerButton("رنگ کیبورد");
        Button emoji = drawerButton("ساخت Emoji");
        Button steering = drawerButton("فرمان ماشین");
        Button arabic = drawerButton("حرکت‌ها و صداهای عربی");

        panel.addView(transparency);
        panel.addView(palette);
        panel.addView(emoji);
        panel.addView(steering);
        panel.addView(arabic);

        final PopupWindow popup = new PopupWindow(panel,
                Math.min((int)(getWidth() * 0.92f), 700),
                WindowManager.LayoutParams.WRAP_CONTENT, true);
        popup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popup.setOutsideTouchable(true);
        popup.setElevation(8f);

        transparency.setOnClickListener(v -> {
            popup.dismiss();
            showTransparency();
        });
        palette.setOnClickListener(v -> {
            popup.dismiss();
            showColorPalette();
        });
        emoji.setOnClickListener(v -> {
            popup.dismiss();
            showEmojiMaker();
        });
        steering.setOnClickListener(v -> {
            popup.dismiss();
            showSteeringWheel();
        });
        arabic.setOnClickListener(v -> {
            popup.dismiss();
            showArabicHarakat();
        });

        popup.showAtLocation(this, Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, 8);
        drawerOpen = true;
        popup.setOnDismissListener(() -> drawerOpen = false);
    }

    private Button drawerButton(String text) {
        Button b = new Button(service);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(NAVY);
        b.setAllCaps(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 58);
        lp.setMargins(0, 3, 0, 3);
        b.setLayoutParams(lp);
        return b;
    }

    private void showColorPalette() {
        final int[] colors = {
                Color.rgb(250,249,244), Color.rgb(255,255,255), Color.rgb(245,245,245),
                Color.rgb(255,244,230), Color.rgb(255,235,235), Color.rgb(235,245,255),
                Color.rgb(235,250,240), Color.rgb(245,238,255), Color.rgb(255,248,205),
                Color.rgb(225,240,235), Color.rgb(235,235,225), Color.rgb(225,230,240)
        };

        GridLayout grid = new GridLayout(service);
        grid.setColumnCount(4);
        grid.setPadding(18, 12, 18, 12);

        for (int color : colors) {
            Button b = new Button(service);
            b.setText("");
            b.setBackgroundColor(color);
            b.setOnClickListener(v -> {
                KEY = color;
                service.getSharedPreferences("fast_keys_settings", android.content.Context.MODE_PRIVATE)
                        .edit().putInt("keyboard_key_color", color).apply();
                invalidate();
            });
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = 70;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(5, 5, 5, 5);
            grid.addView(b, lp);
        }

        LinearLayout root = new LinearLayout(service);
        root.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(service);
        title.setText("رنگ کیبورد");
        title.setTextSize(19);
        title.setTextColor(NAVY);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 52));
        root.addView(grid, new LinearLayout.LayoutParams(-1, 240));

        new AlertDialog.Builder(service)
                .setView(root)
                .setNegativeButton("بستن", null)
                .show();
    }

    private void showTransparency() {
        LinearLayout root = new LinearLayout(service);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 10, 28, 10);

        TextView title = new TextView(service);
        title.setText("شفافیت کیبورد");
        title.setTextSize(19);
        title.setTextColor(BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 52));

        SeekBar bar = new SeekBar(service);
        bar.setMax(99);
        int current = Math.max(1, Math.min(100, Math.round(getAlpha() * 100f)));
        bar.setProgress(current - 1);
        root.addView(bar, new LinearLayout.LayoutParams(-1, 56));

        TextView value = new TextView(service);
        value.setText(current + "%");
        value.setTextSize(17);
        value.setTextColor(BLACK);
        value.setGravity(Gravity.CENTER);
        root.addView(value, new LinearLayout.LayoutParams(-1, 48));

        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int progress, boolean fromUser) {
                int v = progress + 1;
                value.setText(v + "%");
                setAlpha(v / 100f);
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });

        new AlertDialog.Builder(service).setView(root).setNegativeButton("بستن", null).show();
    }

    private void showEmojiMaker() {
        LinearLayout root = new LinearLayout(service);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 10, 24, 10);

        TextView title = new TextView(service);
        title.setText("ساخت Emoji");
        title.setTextSize(19);
        title.setTextColor(BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 52));

        EditText input = new EditText(service);
        input.setHint("ترکیب Emoji را وارد کنید");
        input.setTextSize(24);
        input.setGravity(Gravity.CENTER);
        root.addView(input, new LinearLayout.LayoutParams(-1, 70));

        GridLayout quick = new GridLayout(service);
        quick.setColumnCount(4);
        String[] parts = {"😀","😂","❤️","👍","🔥","⭐","✨","😎"};
        for (String part : parts) {
            Button b = new Button(service);
            b.setText(part);
            b.setTextSize(22);
            b.setAllCaps(false);
            b.setOnClickListener(v -> input.append(part));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = 62;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(2, 2, 2, 2);
            quick.addView(b, lp);
        }
        root.addView(quick, new LinearLayout.LayoutParams(-1, 132));

        new AlertDialog.Builder(service)
                .setView(root)
                .setPositiveButton("ساخت و درج", (d, which) -> {
                    String emojiText = input.getText().toString();
                    if (!emojiText.isEmpty()) service.type(emojiText);
                })
                .setNegativeButton("بستن", null)
                .show();
    }

    private void showSteeringWheel() {
        final SteeringView wheel = new SteeringView(service);
        final PopupWindow popup = new PopupWindow(wheel, 360, 430, false);
        popup.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        popup.setOutsideTouchable(false);
        popup.setElevation(12f);
        wheel.setPopup(popup);
        popup.showAtLocation(this, Gravity.CENTER, 0, 0);
    }

    private class SteeringView extends View {
        private final Paint wp = new Paint(Paint.ANTI_ALIAS_FLAG);
        private PopupWindow popup;
        private float cx, cy, radius;
        private float downX, downY, startX, startY;
        private boolean resizing;
        private long lastMove;
        private RectF upRect = new RectF(), downRect = new RectF(), leftRect = new RectF(), rightRect = new RectF();
        private RectF closeRect = new RectF(), padRect = new RectF();

        SteeringView(android.content.Context c) {
            super(c);
            setBackgroundColor(Color.WHITE);
        }

        void setPopup(PopupWindow p) { popup = p; }

        private void button(Canvas c, RectF r, String label) {
            wp.setStyle(Paint.Style.FILL);
            wp.setColor(Color.rgb(245,245,245));
            c.drawRoundRect(r, 14, 14, wp);
            wp.setStyle(Paint.Style.STROKE);
            wp.setStrokeWidth(2);
            wp.setColor(NAVY);
            c.drawRoundRect(r, 14, 14, wp);
            wp.setStyle(Paint.Style.FILL);
            wp.setTextAlign(Paint.Align.CENTER);
            wp.setTextSize(30);
            wp.setColor(NAVY);
            c.drawText(label, r.centerX(), r.centerY()-(wp.ascent()+wp.descent())/2, wp);
        }

        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            float w=getWidth(), h=getHeight();
            wp.setStyle(Paint.Style.FILL);
            wp.setColor(Color.rgb(255,255,255));
            c.drawRect(0,0,w,h,wp);

            // Close button on the mouse control window itself.
            closeRect.set(w-62, 10, w-10, 54);
            wp.setColor(Color.rgb(235,235,235));
            c.drawRoundRect(closeRect, 12, 12, wp);
            wp.setColor(NAVY); wp.setTextSize(18); wp.setTextAlign(Paint.Align.CENTER);
            c.drawText("Close", closeRect.centerX(), closeRect.centerY()-(wp.ascent()+wp.descent())/2, wp);

            float padSize=Math.min(w-90, 260);
            float padLeft=(w-padSize)/2f;
            float padTop=72;
            padRect.set(padLeft,padTop,padLeft+padSize,padTop+padSize);
            wp.setColor(Color.rgb(245,247,250));
            c.drawRoundRect(padRect, 22, 22, wp);
            wp.setStyle(Paint.Style.STROKE); wp.setStrokeWidth(3); wp.setColor(NAVY);
            c.drawRoundRect(padRect,22,22,wp); wp.setStyle(Paint.Style.FILL);
            wp.setColor(Color.rgb(210,214,220));
            c.drawLine(padRect.centerX(),padRect.top+18,padRect.centerX(),padRect.bottom-18,wp);
            c.drawLine(padRect.left+18,padRect.centerY(),padRect.right-18,padRect.centerY(),wp);
            wp.setColor(NAVY);
            c.drawCircle(padRect.centerX(),padRect.centerY(),24,wp);

            float bs=58, gapB=10;
            float bx=w/2f-bs/2f;
            upRect.set(bx, padRect.bottom+16, bx+bs, padRect.bottom+16+bs);
            downRect.set(bx, upRect.bottom+gapB, bx+bs, upRect.bottom+gapB+bs);
            leftRect.set(bx-bs-gapB, upRect.top, bx-gapB, upRect.bottom);
            rightRect.set(bx+bs+gapB, upRect.top, bx+2*bs+gapB, upRect.bottom);
            button(c,upRect,"↑"); button(c,downRect,"↓"); button(c,leftRect,"←"); button(c,rightRect,"→");

            wp.setColor(Color.rgb(130,130,130));
            c.drawRect(w-22,h-22,w-4,h-4,wp);
        }

        private void moveBy(float dx, float dy) {
            if(Math.abs(dx)>Math.abs(dy))
                service.move(dx<0?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT);
            else
                service.move(dy<0?KeyEvent.KEYCODE_DPAD_UP:KeyEvent.KEYCODE_DPAD_DOWN);
        }

        private void handleButton(float x,float y){
            if(closeRect.contains(x,y)){ popup.dismiss(); return; }
            if(upRect.contains(x,y)){ service.move(KeyEvent.KEYCODE_DPAD_UP); return; }
            if(downRect.contains(x,y)){ service.move(KeyEvent.KEYCODE_DPAD_DOWN); return; }
            if(leftRect.contains(x,y)){ service.move(KeyEvent.KEYCODE_DPAD_LEFT); return; }
            if(rightRect.contains(x,y)){ service.move(KeyEvent.KEYCODE_DPAD_RIGHT); return; }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float x=e.getX(), y=e.getY();
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                handleButton(x,y);
                if(closeRect.contains(x,y)||upRect.contains(x,y)||downRect.contains(x,y)||leftRect.contains(x,y)||rightRect.contains(x,y)) return true;
                downX=x; downY=y; startX=getTranslationX(); startY=getTranslationY();
                resizing=(x>getWidth()-35 && y>getHeight()-35);
                return true;
            }
            if(e.getAction()==MotionEvent.ACTION_MOVE){
                if(resizing){
                    int nw=Math.max(300,(int)(getWidth()+x-downX));
                    int nh=Math.max(360,(int)(getHeight()+y-downY));
                    popup.setWidth(nw); popup.setHeight(nh);
                    downX=x; downY=y;
                }else if(padRect.contains(x,y)){
                    moveBy(x-downX,y-downY);
                    downX=x; downY=y;
                }else{
                    setTranslationX(startX+x-downX);
                    setTranslationY(startY+y-downY);
                }
                invalidate();
                return true;
            }
            if(e.getAction()==MotionEvent.ACTION_UP){
                resizing=false;
                return true;
            }
            return true;
        }
    }

    private void showArabicHarakat() {
        final String[][] groups = {
                {"َ","فتحه / صدای کوتاه a"},
                {"ِ","کسره / صدای کوتاه i"},
                {"ُ","ضمه / صدای کوتاه u"},
                {"ْ","سکون"},
                {"ّ","تشدید"},
                {"ً","تنوین فتح"},
                {"ٍ","تنوین کسر"},
                {"ٌ","تنوین ضم"},
                {"ٓ","مدّ"},
                {"ٰ","الف خنجری"},
                {"ٔ","همزه بالا"},
                {"ٕ","همزه پایین"},
                {"ٖ","نشان کوچک زیر"},
                {"ٗ","نشان کوچک بالا"},
                {"َا","صدای بلند آ / ا"},
                {"ِی","صدای بلند ای / ی"},
                {"ُو","صدای بلند او / و"},
                {"ٱ","الف وصل"}
        };

        LinearLayout root = new LinearLayout(service);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 10, 20, 10);

        TextView title = new TextView(service);
        title.setText("حرکت‌ها و صداهای عربی");
        title.setTextSize(20);
        title.setTextColor(BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 54));

        GridLayout grid = new GridLayout(service);
        grid.setColumnCount(3);
        for (String[] item : groups) {
            Button b = new Button(service);
            b.setText(item[0] + "\n" + item[1]);
            b.setTextSize(14);
            b.setTextColor(NAVY);
            b.setAllCaps(false);
            final String mark = item[0];
            b.setOnClickListener(v -> service.type(mark));
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = 72;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(3, 3, 3, 3);
            grid.addView(b, lp);
        }
        root.addView(grid, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        AlertDialog dialog = new AlertDialog.Builder(service)
                .setView(root)
                .setNegativeButton("بستن", null)
                .create();
        dialog.show();
    }

    @Override protected void onDraw(Canvas c){
        super.onDraw(c);
        float w=getWidth(),h=getHeight();
        gap=1;
        int rows=7;
        keyH=(h-gap*(rows+1))/rows;

        drawKeyboard(c);

        if (pressGlow) drawPressGlow(c);

        if(magnifier && magnifierX >= 0 && magnifierY >= 0)
            drawMagnifier(c);
    }

    private void drawKeyboard(Canvas c){
        float w=getWidth();
        float y=gap;

        float[] wt={.55f,1.45f,1.55f,1.05f,1.0f,1.0f,1.0f,1.25f,.55f};
        String[] top={"","Copy All","Copy Screen","Paste","Cut","Undo","Redo","100\nHistory","⌄"};
        row(c,y,wt,top);
        float wtSum = 0f;
        for (float value : wt) wtSum += value;
        drawMousePointer(c, gap, y, gap + wt[0] * ((w-gap*(wt.length+1))/wtSum), y + keyH);

        y+=keyH+gap;
        String[] sug={suggestions[0],suggestions[1],suggestions[2]};
        for(int i=0;i<3;i++)
            key(c,i*w/3f,y,(i+1)*w/3f,y+keyH,sug[i],BLUE,false);

        y+=keyH+gap;
        float[] w2={.55f,.9f,.9f,.9f,.9f,.9f,.9f,.9f,.9f,.9f,.9f,.9f,.9f,1.45f};
        String[] sy={"⌃","!\n۱","@\n۲","#\n۳","$\n۴","%\n۵","^\n۶","&\n۷","*\n۸","(\n۹",")\n۰","-\n_","=\n+","⌫"};
        row(c,y,w2,sy);

        y+=keyH+gap;
        drawArrow(c,0,y,keyH,keyH,"↑");
        drawCapsAndMagnifierRow(c,y);

        y+=keyH+gap;
        drawArrow(c,0,y,keyH,keyH,"↓");
        String[] r4={"ش","س","ی","ب","ل","ت","ا","ک","گ","؛","«"};
        rowFrom(c,y,keyH,r4);
        key(c,w-keyH-gap,y,w,y+2*keyH+gap,"Enter",NAVY,false);

        y+=keyH+gap;
        String[] r5={"،","ژ","ذ","ز","گ","چ","پ","ب","ن","م","،","/\n؟"};
        rowFrom(c,y,keyH,r5);

        y+=keyH+gap;
        float[] bw={1,1,1,3.9f,1.35f,1.35f,1.15f,1.15f};
        String[] b={"!#@","◎","☺","Space","←","→","↑","↓"};
        row(c,y,bw,b);
    }

    private void drawCapsAndMagnifierRow(Canvas c,float y){
        float left=keyH+gap;
        float available=getWidth()-left-gap;
        int count=15; // CAPS + magnifier + 13 existing keys
        float capsW=keyH*.62f;
        float magW=keyH*.62f;
        float remaining=available-capsW-magW-gap*(count+1);
        float normalW=remaining/13f;
        float x=left+gap;

        key(c,x,y,x+capsW,y+keyH,"",NAVY,false);
        txt(c,"Caps",x+capsW/2,y+keyH/2,Math.min(13,keyH*.25f),NAVY);
        if(caps){
            p.setColor(GREEN);
            c.drawCircle(x+10,y+10,4,p);
        }
        x+=capsW+gap;

        key(c,x,y,x+magW,y+keyH,"",NAVY,false);
        magnifierIcon(c,x+magW/2,y+keyH/2,Math.min(magW,keyH)*.72f,magnifier);
        x+=magW+gap;

        String[] letters={"ض","ص","ث","ق","ف","غ","ع","ه","خ","ج","{\n[","}\n]","|\n\\"};
        for(String s:letters){
            if(s.contains("\n")){
                key(c,x,y,x+normalW,y+keyH,"",NAVY,false);
                String[] a=s.split("\\n");
                txt(c,a[0],x+normalW/2,y+keyH*.35f,Math.min(20,keyH*.3f),NAVY);
                txt(c,a[1],x+normalW/2,y+keyH*.7f,Math.min(20,keyH*.3f),NAVY);
            }else{
                key(c,x,y,x+normalW,y+keyH,s,BLUE,false);
            }
            x+=normalW+gap;
        }
    }

    private void drawMousePointer(Canvas c,float l,float t,float r,float b){
        float cx=l+(r-l)*.50f;
        float cy=t+(b-t)*.50f;
        float s=Math.min(r-l,b-t)*.34f;
        Path pointer=new Path();
        pointer.moveTo(cx-s*.65f, cy-s);
        pointer.lineTo(cx-s*.65f, cy+s*.72f);
        pointer.lineTo(cx-s*.08f, cy+s*.30f);
        pointer.lineTo(cx+s*.22f, cy+s*.92f);
        pointer.lineTo(cx+s*.50f, cy+s*.72f);
        pointer.lineTo(cx+s*.20f, cy+s*.12f);
        pointer.lineTo(cx+s*.82f, cy+s*.12f);
        pointer.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(NAVY);
        c.drawPath(pointer,p);
    }

    private void row(Canvas c,float y,float[] weights,String[] labels){
        float total=0;
        for(float q:weights)total+=q;
        float ww=(getWidth()-gap*(weights.length+1))/total,x=gap;
        for(int i=0;i<weights.length;i++){
            float cw=ww*weights[i];
            String s=labels[i];
            if(s.contains("\n")){
                key(c,x,y,x+cw,y+keyH,"",NAVY,false);
                String[] a=s.split("\\n");
                txt(c,a[0],x+cw/2,y+keyH*.35f,Math.min(20,keyH*.3f),NAVY);
                txt(c,a[1],x+cw/2,y+keyH*.7f,Math.min(20,keyH*.3f),NAVY);
            }else{
                key(c,x,y,x+cw,y+keyH,s,NAVY,false);
            }
            x+=cw+gap;
        }
    }

    private void rowFrom(Canvas c,float y,float left,String[] labels){
        float x=left+gap,available=getWidth()-left-gap;
        float ww=(available-gap*(labels.length+1))/labels.length;
        for(String s:labels){
            key(c,x,y,x+ww,y+keyH,s,BLUE,false);
            x+=ww+gap;
        }
    }

    private void drawArrow(Canvas c,float x,float y,float ww,float hh,String s){
        key(c,x,y,x+ww,y+hh,"",NAVY,true);
        txt(c,s,x+ww/2,y+hh/2,hh*.55f,NAVY);
    }

    private void drawPressGlow(Canvas c){
        float pad = Math.max(3f, keyH * .045f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(Color.argb(65, 40, 140, 255));
        c.drawRoundRect(pressL-pad*1.8f, pressT-pad*1.8f, pressR+pad*1.8f, pressB+pad*1.8f, 10, 10, p);
        p.setColor(Color.argb(115, 70, 160, 255));
        c.drawRoundRect(pressL-pad, pressT-pad, pressR+pad, pressB+pad, 8, 8, p);
        p.setColor(Color.argb(105, 255, 255, 255));
        c.drawRoundRect(pressL, pressT, pressR, pressB, 7, 7, p);
    }

    private void drawMagnifier(Canvas c){
        // Circular zoom preview of the keyboard around the last touch.
        float radius=Math.min(getWidth(),getHeight())*.16f;
        float zoom=1.8f;
        Bitmap bm=Bitmap.createBitmap(getWidth(),getHeight(),Bitmap.Config.ARGB_8888);
        Canvas bc=new Canvas(bm);
        drawKeyboard(bc);

        c.save();
        Path clip=new Path();
        clip.addCircle(magnifierX,magnifierY,radius,Path.Direction.CW);
        c.clipPath(clip);

        float srcR=radius/zoom;
        Rect src=new Rect(
                Math.max(0,(int)(magnifierX-srcR)),
                Math.max(0,(int)(magnifierY-srcR)),
                Math.min(getWidth(),(int)(magnifierX+srcR)),
                Math.min(getHeight(),(int)(magnifierY+srcR)));
        RectF dst=new RectF(
                magnifierX-radius,
                magnifierY-radius,
                magnifierX+radius,
                magnifierY+radius);
        c.drawBitmap(bm,src,dst,p);
        c.restore();

        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(3);
        p.setColor(GREEN);
        c.drawCircle(magnifierX,magnifierY,radius,p);
        p.setStyle(Paint.Style.FILL);
    }

    public void refreshSuggestions(){
        CharSequence q=service.getCurrentInputConnection()==null?null:
                service.getCurrentInputConnection().getTextBeforeCursor(80,0);
        String word="";
        if(q!=null){
            String b=q.toString();
            int i=b.length()-1;
            while(i>=0&&!Character.isWhitespace(b.charAt(i)))i--;
            word=b.substring(i+1);
        }
        Arrays.fill(suggestions,"");
        if(word.length()==0){invalidate();return;}
        int n=0;
        for(String[] group:WORDS)
            for(String x:group)
                if(x.startsWith(word)&&!x.equals(word)&&n<3)suggestions[n++]=x;
        if(n==0)
            for(String[] group:WORDS)
                for(String x:group)
                    if(x.contains(word)&&n<3)suggestions[n++]=x;
        invalidate();
    }

    private void setPressGlow(float l, float t, float r, float b, boolean held){
        pressL=l; pressT=t; pressR=r; pressB=b;
        pressHeld=held;
        pressGlow=true;
        handler.removeCallbacks(clearPressGlow);
        if (!held) handler.postDelayed(clearPressGlow, 140);
        invalidate();
    }

    private void clearPressGlowNow(){
        pressHeld=false;
        pressGlow=false;
        handler.removeCallbacks(clearPressGlow);
        invalidate();
    }

    private void pressRectFor(float x, float y, boolean held){
        float w=getWidth();
        int row=(int)((y-gap)/(keyH+gap));
        if(row<0 || row>6){ clearPressGlowNow(); return; }
        float l,r,t=gap+row*(keyH+gap),b=t+keyH;

        if(row==0){
            float[] wt={.55f,1.45f,1.55f,1.05f,1.0f,1.0f,1.0f,1.25f,.55f};
            float total=0; for(float q:wt) total+=q;
            float ww=(w-gap*(wt.length+1))/total;
            l=gap;
            for(int i=0;i<wt.length;i++){
                r=l+ww*wt[i];
                if(x>=l && x<=r){ setPressGlow(l,t,r,b,held); return; }
                l=r+gap;
            }
            return;
        }
        if(row==1){
            float cw=w/3f; int i=Math.max(0,Math.min(2,(int)(x/cw)));
            setPressGlow(i*cw,t,(i+1)*cw,b,held); return;
        }
        if(row==2){
            if(x>w-keyH*1.6f){ setPressGlow(w-keyH*1.45f,t,w,t+keyH,held); return; }
            float left=keyH*.55f; float totalW=w-left-gap; float cw=totalW/13f;
            int i=Math.max(0,Math.min(11,(int)((x-left)/cw)));
            l=left+i*cw; r=left+(i+1)*cw; setPressGlow(l,t,r,b,held); return;
        }
        if(row==3){
            float left=keyH+gap;
            float capsW=keyH*.62f, magW=keyH*.62f;
            if(x>=left+gap && x<left+gap+capsW){ setPressGlow(left+gap,t,left+gap+capsW,b,held); return; }
            float magLeft=left+gap+capsW+gap;
            if(x>=magLeft && x<magLeft+magW){ setPressGlow(magLeft,t,magLeft+magW,b,held); return; }
            float available=w-left-gap;
            float normalW=(available-capsW-magW-gap*(15+1))/13f;
            float lettersLeft=magLeft+magW+gap;
            int i=Math.max(0,Math.min(12,(int)((x-lettersLeft)/(normalW+gap))));
            l=lettersLeft+i*(normalW+gap); r=l+normalW; setPressGlow(l,t,r,b,held); return;
        }
        if(row==4){
            if(x<keyH*1.5f){ setPressGlow(0,t,keyH,b,held); return; }
            if(x>w-keyH*1.5f){ setPressGlow(w-keyH-gap,t,w,b,held); return; }
            float left=keyH; float available=w-left-gap; float cw=(available-gap*12)/11f;
            int i=Math.max(0,Math.min(10,(int)((x-left)/(cw+gap))));
            l=left+i*(cw+gap); r=l+cw; setPressGlow(l,t,r,b,held); return;
        }
        if(row==5){
            float cw=w/12f; int i=Math.max(0,Math.min(11,(int)(x/cw)));
            setPressGlow(i*cw,t,(i+1)*cw,b,held); return;
        }
        if(row==6){
            float cw=w/8f; int i=Math.max(0,Math.min(7,(int)(x/cw)));
            setPressGlow(i*cw,t,(i+1)*cw,b,held); return;
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){
            pressRectFor(e.getX(), e.getY(), true);
            if(magnifier){
                magnifierX=e.getX();
                magnifierY=e.getY();
                invalidate();
            }
            handle(e.getX(),e.getY());
            // A normal tap keeps its light briefly; Backspace keeps it lit while held.
            if (!isBackspaceAt(e.getX(), e.getY())) {
                pressHeld=false;
                handler.removeCallbacks(clearPressGlow);
                handler.postDelayed(clearPressGlow, 140);
            }
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_MOVE && magnifier){
            magnifierX=e.getX();
            magnifierY=e.getY();
            invalidate();
            return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL){
            stopRepeat();
            clearPressGlowNow();
        }
        return true;
    }

    private boolean isBackspaceAt(float x,float y){
        float w=getWidth();
        int row=(int)((y-gap)/(keyH+gap));
        return row==2 && x>w-keyH*1.6f;
    }

    private void handle(float x,float y){
        float w=getWidth();
        int row=(int)((y-gap)/(keyH+gap));

        if(row==0){
            int i=(int)(x/(w/9f));
            if(i==1)service.copyAll();
            else if(i==3)service.paste();
            else if(i==4)service.cut();
            else if(i==5)service.undo();
            else if(i==8)showDrawer();
            return;
        }

        if(row==1){
            int i=(int)(x/(w/3f));
            if(i>=0&&i<3&&!suggestions[i].isEmpty())service.replaceCurrentWord(suggestions[i]);
            return;
        }

        if(row==2){
            if(x>w-keyH*1.6f){startBackspace();return;}
            String[] s={"!","@","#","$","%","^","&","*","(",")","-","="};
            int i=Math.max(0,Math.min(11,(int)((x-keyH*.55f)/(w/13f))));
            service.type(s[i]);
            return;
        }

        if(row==3){
            float left=keyH+gap;
            float available=w-left-gap;
            float capsW=keyH*.62f;
            float magW=keyH*.62f;
            if(x>=left+gap && x<left+gap+capsW){
                caps=!caps;
                invalidate();
                return;
            }
            float magLeft=left+gap+capsW+gap;
            if(x>=magLeft && x<magLeft+magW){
                magnifier=!magnifier;
                if(!magnifier){magnifierX=-1;magnifierY=-1;}
                invalidate();
                return;
            }
            float lettersLeft=magLeft+magW+gap;
            float normalW=(available-capsW-magW-gap*(15+1))/13f;
            int i=Math.max(0,Math.min(9,(int)((x-lettersLeft)/(normalW+gap))));
            String[] s={"ض","ص","ث","ق","ف","غ","ع","ه","خ","ج"};
            if(i<s.length)service.type(s[i]);
            return;
        }

        if(row==4){
            if(x<keyH*1.5f){service.move(KeyEvent.KEYCODE_DPAD_DOWN);return;}
            if(x>w-keyH*1.5f){service.enter();return;}
            String[] s={"ش","س","ی","ب","ل","ت","ا","ک","گ","؛","«"};
            int i=Math.max(0,Math.min(10,(int)((x-keyH)/(w/12f))));
            service.type(s[i]);
            return;
        }

        if(row==5){
            String[] s={"،","ژ","ذ","ز","گ","چ","پ","ب","ن","م","،","/؟"};
            int i=Math.max(0,Math.min(11,(int)(x/(w/12f))));
            service.type(s[i]);
            return;
        }

        if(row==6){
            int i=(int)(x/(w/8f));
            if(i==0)service.type("!#@");
            else if(i==1)service.switchInputMethod(null);
            else if(i==2)service.type("🙂");
            else if(i==3)service.type(" ");
            else if(i==4)service.move(KeyEvent.KEYCODE_DPAD_LEFT);
            else if(i==5)service.move(KeyEvent.KEYCODE_DPAD_RIGHT);
            else if(i==6)service.move(KeyEvent.KEYCODE_DPAD_UP);
            else if(i==7)service.move(KeyEvent.KEYCODE_DPAD_DOWN);
        }
    }

    private void startBackspace(){
        service.backspace();
        stopRepeat();
        repeat=()->{
            service.backspace();
            handler.postDelayed(repeat,55);
        };
        handler.postDelayed(repeat,350);
    }

    private void stopRepeat(){
        if(repeat!=null){
            handler.removeCallbacks(repeat);
            repeat=null;
        }
    }
}
