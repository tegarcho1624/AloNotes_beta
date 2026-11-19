package com.alonotes.app;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

/** Íconos de trazo dibujados en una rejilla de 24x24 (sin recursos vectoriales). */
final class Icons extends Drawable {
    static final int MENU = 0, SORT = 1, SEARCH = 2, MAIL = 3, HELP = 4, GEAR = 5, PLUS = 6,
            CLOSE = 7, DOC = 8, PDF = 9, FOLDER = 10, BACK = 11, CHEV = 12, GRID = 13,
            PEN = 14, MARKER = 15, ERASER = 16, TEXT = 17, SHAPES = 18, IMAGE = 19,
            STAR = 20, SELECT = 21, UNDO = 22, REDO = 23, LOCK = 24, ROT_L = 25,
            ROT_R = 26, PENCIL = 27;

    private final int type;
    private final boolean fill;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path q = new Path();
    private final RectF r = new RectF();

    Icons(int type, int color, boolean fill) {
        this.type = type;
        this.fill = fill;
        p.setColor(color);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
    }

    private void ln(Canvas c, float a, float b, float x, float y) { c.drawLine(a, b, x, y, p); }

    private void rr(Canvas c, float l, float t, float rt, float bt, float rad) {
        r.set(l, t, rt, bt);
        c.drawRoundRect(r, rad, rad, p);
    }

    private void poly(Canvas c, boolean close, float... v) {
        q.reset();
        q.moveTo(v[0], v[1]);
        for (int i = 2; i < v.length; i += 2) q.lineTo(v[i], v[i + 1]);
        if (close) q.close();
        c.drawPath(q, p);
    }

    private void undo(Canvas c) {
        poly(c, false, 8, 5, 4, 9, 8, 13);
        q.reset();
        q.moveTo(4.5f, 9);
        q.lineTo(14, 9);
        q.cubicTo(18, 9, 20, 11.5f, 20, 15);
        q.cubicTo(20, 18, 18, 19.5f, 15, 19.5f);
        c.drawPath(q, p);
    }

    private void rot(Canvas c) {
        r.set(5, 5, 19, 19);
        c.drawArc(r, 200, 280, false, p);
        poly(c, false, 5.4f, 5.5f, 5.42f, 9.61f, 9.5f, 10.2f);
    }

    private void text(Canvas c, String s, float size, float x, float y) {
        p.setStyle(Paint.Style.FILL);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(size);
        p.setFakeBoldText(true);
        c.drawText(s, x, y, p);
        p.setFakeBoldText(false);
        p.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void draw(Canvas c) {
        Rect b = getBounds();
        if (b.isEmpty()) return;
        c.save();
        c.translate(b.left, b.top);
        float s = b.width() / 24f;
        c.scale(s, s);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.8f);
        p.setPathEffect(null);
        switch (type) {
            case MENU: ln(c, 4, 6, 20, 6); ln(c, 4, 12, 20, 12); ln(c, 4, 18, 20, 18); break;
            case SORT:
                ln(c, 4, 6, 14, 6); ln(c, 4, 12, 11, 12); ln(c, 4, 18, 8, 18);
                ln(c, 18, 5, 18, 19); poly(c, false, 15, 16, 18, 19, 21, 16); break;
            case SEARCH: c.drawCircle(11, 11, 6.5f, p); ln(c, 16, 16, 21, 21); break;
            case MAIL: rr(c, 3, 5, 21, 19, 2); poly(c, false, 3.5f, 6.5f, 12, 13, 20.5f, 6.5f); break;
            case HELP: c.drawCircle(12, 12, 9, p); text(c, "?", 12, 12, 16.2f); break;
            case GEAR:
                c.drawCircle(12, 12, 3, p);
                c.drawCircle(12, 12, 6.5f, p);
                for (int i = 0; i < 8; i++) {
                    double a = Math.toRadians(45 * i);
                    float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
                    ln(c, 12 + 6.5f * cs, 12 + 6.5f * sn, 12 + 9f * cs, 12 + 9f * sn);
                }
                break;
            case PLUS: ln(c, 12, 5, 12, 19); ln(c, 5, 12, 19, 12); break;
            case CLOSE: ln(c, 6, 6, 18, 18); ln(c, 18, 6, 6, 18); break;
            case DOC: poly(c, true, 6, 3, 14, 3, 19, 8, 19, 21, 6, 21); poly(c, false, 14, 3, 14, 8, 19, 8); break;
            case PDF:
                poly(c, true, 6, 3, 14, 3, 19, 8, 19, 21, 6, 21);
                poly(c, false, 14, 3, 14, 8, 19, 8);
                text(c, "PDF", 5.5f, 12.5f, 17); break;
            case FOLDER:
                if (fill) p.setStyle(Paint.Style.FILL_AND_STROKE);
                poly(c, true, 3, 7, 3, 19, 21, 19, 21, 9, 12, 9, 10, 7); break;
            case BACK: poly(c, false, 15, 5, 8, 12, 15, 19); break;
            case CHEV: poly(c, false, 6, 9, 12, 15, 18, 9); break;
            case GRID:
                rr(c, 4, 4, 10, 10, 1.5f); rr(c, 14, 4, 20, 10, 1.5f);
                rr(c, 4, 14, 10, 20, 1.5f); rr(c, 14, 14, 20, 20, 1.5f); break;
            case PEN:
            case PENCIL:
                poly(c, true, 4, 20, 5.5f, 14.5f, 15.5f, 4.5f, 19.5f, 8.5f, 9.5f, 18.5f);
                ln(c, 13, 7, 17, 11); break;
            case MARKER: poly(c, true, 5, 19, 5, 15, 15, 5, 19, 9, 9, 19); ln(c, 4, 21.5f, 11, 21.5f); break;
            case ERASER:
                poly(c, true, 3.5f, 14.5f, 13, 5, 19.5f, 11.5f, 12, 19, 8, 19); ln(c, 12, 19, 21, 19); break;
            case TEXT: ln(c, 5, 6, 19, 6); ln(c, 12, 6, 12, 19); ln(c, 9, 19, 15, 19); break;
            case SHAPES:
                poly(c, true, 8, 3, 13, 11, 3, 11); rr(c, 3, 14, 10, 21, 1);
                c.drawCircle(16.5f, 16.5f, 4.5f, p); break;
            case IMAGE:
                rr(c, 3, 4, 21, 20, 2); c.drawCircle(9, 10, 1.8f, p);
                poly(c, false, 3, 17, 9, 12, 13, 16, 16, 13, 21, 18); break;
            case STAR:
                rr(c, 3, 3, 21, 21, 5);
                q.reset();
                for (int i = 0; i < 10; i++) {
                    float rad = (i % 2 == 0) ? 5f : 2.2f;
                    double a = Math.toRadians(-90 + 36 * i);
                    float x = 12 + rad * (float) Math.cos(a), y = 12.5f + rad * (float) Math.sin(a);
                    if (i == 0) q.moveTo(x, y); else q.lineTo(x, y);
                }
                q.close();
                c.drawPath(q, p); break;
            case SELECT:
                p.setPathEffect(new DashPathEffect(new float[]{2.5f, 2.5f}, 0));
                rr(c, 3, 3, 15, 15, 1.5f);
                p.setPathEffect(null);
                poly(c, true, 12, 12, 21, 15.5f, 16.5f, 17, 15, 21.5f); break;
            case UNDO: undo(c); break;
            case REDO: c.save(); c.scale(-1, 1, 12, 12); undo(c); c.restore(); break;
            case LOCK:
                rr(c, 5, 11, 19, 21, 2);
                q.reset(); q.moveTo(8, 11); q.lineTo(8, 8);
                r.set(8, 4, 16, 12); q.arcTo(r, 180, 180); q.lineTo(16, 11);
                c.drawPath(q, p); break;
            case ROT_L: rot(c); break;
            case ROT_R: c.save(); c.scale(-1, 1, 12, 12); rot(c); c.restore(); break;
            default: break;
        }
        c.restore();
    }

    @Override public void setAlpha(int alpha) { p.setAlpha(alpha); }
    @Override public void setColorFilter(ColorFilter cf) { p.setColorFilter(cf); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
