package com.hxrdline.oxide;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class OverlayService extends Service {
    private WindowManager wm;
    private View panel;
    private SharedPreferences prefs;
    private int selected = 0;
    private float downX, downY;
    private int startX, startY;

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        prefs = getSharedPreferences("overlay_ui", MODE_PRIVATE);
        showPanel();
    }

    private GradientDrawable bg(int color, int stroke) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(22);
        d.setStroke(1, stroke); return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(12, 9, 12, 9);
        return t;
    }

    private LinearLayout column() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void showPanel() {
        if (panel != null) return;
        LinearLayout card = column();
        card.setPadding(14, 12, 14, 14);
        card.setBackground(bg(Color.rgb(17,10,27), Color.rgb(174,76,225)));
        card.setElevation(18);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("HXRDLINE  /  OXIDE", 15, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        Button close = button("×");
        close.setOnClickListener(v -> stopSelf());
        header.addView(close, new LinearLayout.LayoutParams(42, 40));
        header.setOnTouchListener((v, e) -> {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) panel.getLayoutParams();
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                downX = e.getRawX(); downY = e.getRawY(); startX = lp.x; startY = lp.y; return true;
            }
            if (e.getAction() == MotionEvent.ACTION_MOVE) {
                lp.x = startX + (int)(e.getRawX() - downX);
                lp.y = startY + (int)(e.getRawY() - downY);
                wm.updateViewLayout(panel, lp); return true;
            }
            return e.getAction() == MotionEvent.ACTION_UP;
        });
        card.addView(header);

        TextView subtitle = text("ПАНЕЛЬ УПРАВЛЕНИЯ · UI BUILD", 10, 0xFFC99BEA);
        card.addView(subtitle);

        LinearLayout tabs = new LinearLayout(this);
        String[] names = {"ОСНОВНОЕ", "ВИЗУАЛ", "ПРОЧЕЕ"};
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            Button tab = button(names[i]);
            tab.setTextSize(9);
            tab.setOnClickListener(v -> { selected = index; renderBody(card); });
            tabs.addView(tab, new LinearLayout.LayoutParams(0, 40, 1));
        }
        card.addView(tabs);

        LinearLayout body = column();
        body.setTag("body");
        card.addView(body);
        fillBody(body);

        TextView note = text("Модули оригинального APK пока не подключены. Переключатели ниже сохраняют только настройки интерфейса.", 10, 0xFFB7A9C5);
        card.addView(note);

        panel = card;
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            dp(310), WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START; lp.x = dp(18); lp.y = dp(110);
        wm.addView(panel, lp);
    }

    private void renderBody(LinearLayout card) {
        LinearLayout body = (LinearLayout) card.findViewWithTag("body");
        if (body != null) fillBody(body);
    }

    private void fillBody(LinearLayout body) {
        body.removeAllViews();
        String[] sections = {
            "Быстрое меню", "Показывать подсказки", "Компактный интерфейс",
            "Подсветка активного раздела", "Сохранять положение панели", "Вибрация интерфейса"
        };
        int from = selected == 0 ? 0 : selected == 1 ? 2 : 4;
        int to = selected == 0 ? 2 : selected == 1 ? 4 : 6;
        for (int i = from; i < to; i++) {
            final String key = "pref_" + i;
            Switch sw = new Switch(this);
            sw.setText(sections[i]); sw.setTextColor(Color.WHITE); sw.setTextSize(12);
            sw.setPadding(8, 10, 8, 10);
            sw.setChecked(prefs.getBoolean(key, i == 0 || i == 3));
            sw.setOnCheckedChangeListener((b, checked) -> prefs.edit().putBoolean(key, checked).apply());
            body.addView(sw);
        }
        TextView status = text("СТАТУС: оболочка меню запущена", 10, 0xFFB88BDA);
        body.addView(status);
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label); b.setTextColor(Color.WHITE); b.setTextSize(11);
        b.setAllCaps(false); b.setPadding(4, 0, 4, 0);
        b.setBackground(bg(0xFF39204D, 0xFF75439B));
        return b;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (panel == null) showPanel();
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        if (panel != null) { try { wm.removeView(panel); } catch (Exception ignored) {} panel = null; }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
