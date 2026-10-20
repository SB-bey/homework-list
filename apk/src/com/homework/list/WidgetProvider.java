package com.homework.list;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
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

/**
 * 桌面小组件：显示当天作业清单，点击打开 App。
 * 打开时从 GitHub 拉最新 JSON，失败自动显示上次缓存。
 */
public class WidgetProvider extends AppWidgetProvider {

    private static final String DATA_URL = "https://raw.githubusercontent.com/tyeod-yomuh/homework-list/main/data/latest.json?v=";
    private static final String PREFS = "hw_widget_cache";

    @Override
    public void onUpdate(Context context, AppWidgetManager mgr, int[] ids) {
        for (int id : ids) {
            updateWidget(context, mgr, id);
        }
    }

    static void updateWidget(final Context ctx, final AppWidgetManager mgr, final int id) {
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
                    // 网络失败，走缓存
                }

                if (json == null) {
                    json = ctx.getSharedPreferences(PREFS, 0).getString("data", null);
                }

                RemoteViews rv = new RemoteViews(ctx.getPackageName(), R.layout.widget_layout);

                if (json == null) {
                    rv.setTextViewText(R.id.w_title, "作业清单");
                    rv.setTextViewText(R.id.w_more, "网络不可用，点开 App 查看");
                    hideItems(rv);
                } else {
                    try {
                        JSONObject d = new JSONObject(json);
                        JSONArray items = d.optJSONArray("items");
                        int n = items == null ? 0 : items.length();
                        rv.setTextViewText(R.id.w_title, d.optString("label", "作业清单"));
                        rv.setTextViewText(R.id.w_more, "今天共 " + n + " 项作业 · 点开写作业");
                        int[] ids = {R.id.w_i1, R.id.w_i2, R.id.w_i3, R.id.w_i4, R.id.w_i5, R.id.w_i6};
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
                    } catch (Exception e) {
                        rv.setTextViewText(R.id.w_title, "作业清单");
                        rv.setTextViewText(R.id.w_more, "数据解析失败，点开 App 查看");
                        hideItems(rv);
                    }
                }

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
        }).start();
    }

    private static void hideItems(RemoteViews rv) {
        int[] ids = {R.id.w_i1, R.id.w_i2, R.id.w_i3, R.id.w_i4, R.id.w_i5, R.id.w_i6};
        for (int id : ids) {
            rv.setViewVisibility(id, View.GONE);
        }
    }
}
