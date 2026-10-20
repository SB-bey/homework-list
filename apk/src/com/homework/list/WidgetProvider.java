package com.homework.list;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

/**
 * 桌面小组件：显示当天作业（内置兜底数据，断网也不空白），
 * 底部"随机抽一个作业"按钮，点击随机挑一项显示；点击其余区域打开 App。
 */
public class WidgetProvider extends AppWidgetProvider {

    private static final String DATA_URL = "https://raw.githubusercontent.com/tyeod-yomuh/homework-list/main/data/latest.json?v=";
    private static final String PREFS = "hw_widget_cache";
    public static final String ACTION_RANDOM = "com.homework.list.ACTION_RANDOM";

    /** 断网兜底：与 App 内置一致（最近一次作业） */
    private static final String FALLBACK_JSON =
        "{\"date\":\"2026-10-21\",\"label\":\"10月21日 周三\",\"key\":\"hw-list-2026-10-21\",\"photo\":\"\",\"items\":[" +
        "{\"text\":\"基础训练往后写一页\",\"subject\":\"数学\",\"done\":false}," +
        "{\"text\":\"作文\",\"subject\":\"英语\",\"done\":false}," +
        "{\"text\":\"听写\",\"subject\":\"英语\",\"done\":false}," +
        "{\"text\":\"背诵\",\"subject\":\"英语\",\"done\":false}," +
        "{\"text\":\"抄文章最后一段\",\"subject\":\"英语\",\"done\":false}," +
        "{\"text\":\"抄单词\",\"subject\":\"英语\",\"done\":false}," +
        "{\"text\":\"往后写一课\",\"subject\":\"历史\",\"done\":false}," +
        "{\"text\":\"试卷 38、39\",\"subject\":\"化学\",\"done\":false}," +
        "{\"text\":\"试卷反面写完\",\"subject\":\"物理\",\"done\":false}" +
        "]}";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            refreshData(context, id, null);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_RANDOM.equals(intent.getAction())) {
            // 随机抽一个：先保证有数据（有缓存用缓存，没有就用兜底），再随机显示
            SharedPreferences sp = context.getSharedPreferences(PREFS, 0);
            String json = sp.getString("data", null);
            if (json == null) {
                json = FALLBACK_JSON;
                sp.edit().putString("data", json).apply();
            }
            AppWidgetManager mgr = AppWidgetManager.getInstance(context);
            ComponentName cn = new ComponentName(context, WidgetProvider.class);
            for (int id : mgr.getAppWidgetIds(cn)) {
                render(context, mgr, id, json, true);
            }
        }
    }

    /** 拉取最新数据（成功存缓存），然后用缓存/兜底渲染 */
    private static void refreshData(final Context ctx, final int id, final String randomText) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String json = null;
                try {
                    HttpURLConnection c = (HttpURLConnection) new URL(DATA_URL + System.currentTimeMillis()).openConnection();
                    c.setConnectTimeout(8000);
                    c.setReadTimeout(8000);
                    if (c.getResponseCode() == 200) {
                        BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        json = sb.toString();
                        ctx.getSharedPreferences(PREFS, 0).edit().putString("data", json).apply();
                    }
                } catch (Exception e) {
                    // 网络失败，走缓存/兜底
                }
                if (json == null) {
                    json = ctx.getSharedPreferences(PREFS, 0).getString("data", null);
                    if (json == null) {
                        json = FALLBACK_JSON;
                        ctx.getSharedPreferences(PREFS, 0).edit().putString("data", json).apply();
                    }
                }
                render(ctx, AppWidgetManager.getInstance(ctx), id, json, false);
            }
        }).start();
    }

    /** 渲染小组件；pickRandom=true 时把随机抽取结果显示在 w_random */
    private static void render(Context ctx, AppWidgetManager mgr, int id, String json, boolean pickRandom) {
        RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_layout);
        try {
            JSONObject d = new JSONObject(json);
            JSONArray items = d.optJSONArray("items");
            int n = items == null ? 0 : items.length();
            rv.setTextViewText(R.id.w_title, d.optString("label", "作业清单") + " · 共" + n + "项");

            int[] ids = {R.id.w_i1, R.id.w_i2, R.id.w_i3, R.id.w_i4};
            for (int i = 0; i < ids.length; i++) {
                if (i < n) {
                    JSONObject it = items.optJSONObject(i);
                    String subj = it == null ? "" : it.optString("subject", "");
                    String text = it == null ? "" : it.optString("text", "");
                    rv.setTextViewText(ids[i], subj + " " + text);
                    rv.setViewVisibility(ids[i], View.VISIBLE);
                } else {
                    rv.setViewVisibility(ids[i], View.GONE);
                }
            }

            if (pickRandom && n > 0) {
                JSONObject pick = items.optJSONObject(new Random().nextInt(n));
                String subj = pick == null ? "" : pick.optString("subject", "");
                String text = pick == null ? "" : pick.optString("text", "");
                rv.setTextViewText(R.id.w_random, "🎲 " + subj + " " + text + "   （再点换一个）");
            } else {
                rv.setTextViewText(R.id.w_random, "🎲 随机抽一个作业");
            }
        } catch (Exception e) {
            rv.setTextViewText(R.id.w_title, "作业清单");
            rv.setTextViewText(R.id.w_random, "🎲 随机抽一个作业");
            for (int i : new int[]{R.id.w_i1, R.id.w_i2, R.id.w_i3, R.id.w_i4}) {
                rv.setViewVisibility(i, View.GONE);
            }
        }

        // 随机按钮：点击发广播，抽一个
        Intent rand = new Intent(ctx, WidgetProvider.class);
        rand.setAction(ACTION_RANDOM);
        PendingIntent rpi = PendingIntent.getBroadcast(ctx, 1, rand,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.w_random, rpi);

        // 其余区域：打开 App
        Intent open = new Intent(ctx, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        rv.setOnClickPendingIntent(R.id.widget_root, pi);

        try {
            mgr.updateAppWidget(id, rv);
        } catch (Exception e) {
            // 忽略更新异常
        }
    }
}
