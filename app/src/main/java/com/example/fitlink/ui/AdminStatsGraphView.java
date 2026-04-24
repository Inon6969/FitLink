package com.example.fitlink.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.annotation.Nullable;

public class AdminStatsGraphView extends View {

    // פלטת הצבעים המעודכנת מבוססת על #1E88E5
    private final int primaryColor = Color.parseColor("#1E88E5");
    private final int darkPrimaryColor = Color.parseColor("#0D47A1");
    private final int lightPrimaryColor = Color.parseColor("#64B5F6");
    private final int trackColor = Color.parseColor("#F0F4F8"); // אפור-כחלחל סופר עדין לרקע
    private final int glowColor = Color.parseColor("#661E88E5"); // צל כחול רך
    private Paint barPaint;
    private Paint barHighlightPaint; // ליצירת אפקט ברק תלת-ממדי
    private Paint trackPaint;
    private Paint textValuePaint;
    private Paint textLabelPaint;
    private Paint axisPaint;
    private Paint gridPaint;
    private Paint valueBadgePaint;
    private int userCount = 0;
    private int groupCount = 0;
    private int eventCount = 0;
    private float animationProgress = 0f;

    public AdminStatsGraphView(Context context) {
        super(context);
        init();
    }

    public AdminStatsGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        // --- צבע העמודות ---
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setShadowLayer(15f, 0f, 8f, glowColor);

        // --- ברק פנימי (3D Effect) ---
        barHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barHighlightPaint.setStyle(Paint.Style.FILL);

        // --- רקע המסלול ---
        trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        trackPaint.setStyle(Paint.Style.FILL);
        trackPaint.setColor(trackColor);

        // --- הבועה שמכילה את המספרים (עכשיו בצבע המותג!) ---
        valueBadgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valueBadgePaint.setColor(primaryColor);
        valueBadgePaint.setStyle(Paint.Style.FILL);
        valueBadgePaint.setShadowLayer(10f, 0f, 6f, Color.parseColor("#33000000"));

        // --- טקסט המספרים (עכשיו בלבן בוהק) ---
        textValuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textValuePaint.setColor(Color.WHITE);
        textValuePaint.setTextSize(42f);
        textValuePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textValuePaint.setTextAlign(Paint.Align.CENTER);

        // --- טקסט התוויות ---
        textLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textLabelPaint.setColor(Color.parseColor("#607D8B")); // אפור מתכתי מודרני
        textLabelPaint.setTextSize(36f);
        textLabelPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textLabelPaint.setTextAlign(Paint.Align.CENTER);

        // --- ציר תחתון ---
        axisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        axisPaint.setColor(Color.parseColor("#CFD8DC"));
        axisPaint.setStrokeWidth(6f);
        axisPaint.setStrokeCap(Paint.Cap.ROUND);

        // --- רשת מקווקות נמוגה ---
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setStrokeWidth(3f);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{15f, 15f}, 0f));
    }

    public void setStats(int users, int groups, int events) {
        this.userCount = users;
        this.groupCount = groups;
        this.eventCount = events;

        startAnimation();
    }

    private void startAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(1600); // הארכנו מעט כדי לתת זמן לאפקט הגל (Stagger)
        animator.setInterpolator(new OvershootInterpolator(1.1f));
        animator.addUpdateListener(animation -> {
            animationProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        int paddingBottom = 130;
        int paddingTop = 120;
        int paddingSides = 60;

        float graphHeight = height - paddingBottom - paddingTop;

        // הגדרת רשת נמוגה (Fading Grid) - שקוף בצדדים, אטום באמצע
        LinearGradient gridShader = new LinearGradient(
                paddingSides, 0, width - paddingSides, 0,
                new int[]{Color.TRANSPARENT, Color.parseColor("#E0E0E0"), Color.parseColor("#E0E0E0"), Color.TRANSPARENT},
                new float[]{0f, 0.2f, 0.8f, 1f},
                Shader.TileMode.CLAMP
        );
        gridPaint.setShader(gridShader);

        // ציור רשת
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            float y = paddingTop + (graphHeight / gridLines) * i;
            canvas.drawLine(paddingSides, y, width - paddingSides, y, gridPaint);
        }

        // ציר X
        canvas.drawLine(paddingSides, height - paddingBottom, width - paddingSides, height - paddingBottom, axisPaint);

        int maxCount = Math.max(10, Math.max(userCount, Math.max(groupCount, eventCount)));
        maxCount = (int) (maxCount * 1.15);

        float availableWidth = width - (2 * paddingSides);
        float unit = availableWidth / 10f;
        float barWidth = unit * 2f;
        float spacing = unit;
        float maxBarHeight = height - paddingBottom - paddingTop;

        String[] labels = {"Users", "Groups", "Events"};
        int[] values = {userCount, groupCount, eventCount};

        for (int i = 0; i < 3; i++) {
            float left = paddingSides + spacing + (i * (barWidth + spacing));
            float right = left + barWidth;
            float centerX = left + (barWidth / 2f);
            float bottom = height - paddingBottom;
            float cornerRadius = barWidth / 2f;

            // 1. ציור רקע ה"מסלול" (Track)
            RectF trackRect = new RectF(left, paddingTop, right, bottom);
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint);

            // --- חישוב אנימציה מדורגת (Staggered Animation) ---
            // כל עמודה מתחילה קצת אחרי הקודמת כדי ליצור אפקט של "גל"
            float delay = i * 0.15f;
            float localProgress = (animationProgress - delay) / (1f - delay);
            // אם האנימציה הכוללת עברה את 1.0 (בגלל Overshoot), נשמור על התנופה גם פה
            if (localProgress < 0) localProgress = 0f;

            // 2. חישוב גובה העמודה הפעילה
            float targetHeight = (((float) values[i] / maxCount) * maxBarHeight);
            float barHeight = targetHeight * localProgress;
            if (barHeight < 0) barHeight = 0;
            float top = bottom - barHeight;

            // 3. הגרדיאנט המרכזי של העמודה
            LinearGradient gradient = new LinearGradient(
                    0, bottom, 0, top,
                    new int[]{darkPrimaryColor, primaryColor, lightPrimaryColor},
                    new float[]{0f, 0.6f, 1f},
                    Shader.TileMode.CLAMP
            );
            barPaint.setShader(gradient);

            // 4. ציור העמודה
            RectF barRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint);

            // 5. ציור הברק העליון (Glossy Highlight) - נותן תחושה של זכוכית תלת-ממדית
            if (barHeight > cornerRadius * 2) {
                RectF highlightRect = new RectF(left + 6, top + 6, right - 6, top + (barWidth));
                LinearGradient highlightGradient = new LinearGradient(
                        0, top, 0, top + barWidth,
                        Color.argb(120, 255, 255, 255), // לבן חצי שקוף
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP
                );
                barHighlightPaint.setShader(highlightGradient);
                canvas.drawRoundRect(highlightRect, cornerRadius, cornerRadius, barHighlightPaint);
            }

            // חישוב המספר שמוצג כרגע (כדי לא לרדת מתחת לאפס ולעצור בערך המדויק)
            int currentDisplayValue = Math.min(values[i], (int) (values[i] * Math.max(0, localProgress)));
            String valueText = String.valueOf(currentDisplayValue);

            // 6. ציור הבועה (Tooltip) רק אם העמודה התרוממה מספיק
            if (localProgress > 0.1f) {
                float textY = top - 45;
                Paint.FontMetrics fm = textValuePaint.getFontMetrics();
                float textWidth = textValuePaint.measureText(valueText);

                float badgeTop = textY + fm.ascent - 15;
                float badgeBottom = textY + fm.descent + 15;

                // ציור החץ
                Path arrowPath = new Path();
                arrowPath.moveTo(centerX - 15, badgeBottom - 2);
                arrowPath.lineTo(centerX + 15, badgeBottom - 2);
                arrowPath.lineTo(centerX, badgeBottom + 15);
                arrowPath.close();

                // בועת הרקע עכשיו בצבע כחול
                RectF badgeRect = new RectF(
                        centerX - (textWidth / 2f) - 35,
                        badgeTop,
                        centerX + (textWidth / 2f) + 35,
                        badgeBottom
                );
                canvas.drawRoundRect(badgeRect, 24f, 24f, valueBadgePaint);
                canvas.drawPath(arrowPath, valueBadgePaint);

                // הטקסט הלבן
                canvas.drawText(valueText, centerX, textY, textValuePaint);
            }

            // 7. תוויות ציר X
            canvas.drawText(labels[i], centerX, height - paddingBottom + 65, textLabelPaint);
        }
    }
}