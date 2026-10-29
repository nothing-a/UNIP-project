package br.unip.tmb.telas.componentes;

import br.unip.tmb.R;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Gráfico de linha com a evolução da TMB ao longo dos cálculos
public class GraficoHistorico extends View {

    public interface AoSelecionar {
        void selecionado(JSONObject calculo);
    }

    private static final int MAX_DATAS = 5;

    private final float dp;
    private final List<JSONObject> calculos = new ArrayList<>();
    private float[] xs = new float[0];
    private float[] ys = new float[0];
    private long minimo, maximo;
    private int selecionado = -1;
    private float progresso = 1f;
    private AoSelecionar aoSelecionar;

    // Área onde a linha é desenhada (sobra espaço para os rótulos)
    private final RectF area = new RectF();
    private final Path linha = new Path();
    private final Path preenchimento = new Path();

    private final Paint pintaLinha = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaArea = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaGrade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaGuia = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaPonto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaFundoPonto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaRotulo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaBalao = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pintaTextoBalao = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final int corAcento;

    public GraficoHistorico(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        corAcento = ContextCompat.getColor(context, R.color.accent);

        pintaLinha.setStyle(Paint.Style.STROKE);
        pintaLinha.setStrokeWidth(3 * dp);
        pintaLinha.setStrokeCap(Paint.Cap.ROUND);
        pintaLinha.setStrokeJoin(Paint.Join.ROUND);
        pintaLinha.setColor(corAcento);

        pintaGrade.setStyle(Paint.Style.STROKE);
        pintaGrade.setStrokeWidth(1 * dp);
        pintaGrade.setColor(ContextCompat.getColor(context, R.color.separator));

        pintaGuia.setStyle(Paint.Style.STROKE);
        pintaGuia.setStrokeWidth(1.5f * dp);
        pintaGuia.setColor(ContextCompat.getColor(context, R.color.secondary_label));
        pintaGuia.setPathEffect(new DashPathEffect(new float[]{4 * dp, 4 * dp}, 0));

        pintaPonto.setColor(corAcento);
        pintaFundoPonto.setColor(ContextCompat.getColor(context, R.color.card));

        pintaRotulo.setColor(ContextCompat.getColor(context, R.color.secondary_label));
        pintaRotulo.setTextSize(11 * dp);
        pintaRotulo.setTypeface(ResourcesCompat.getFont(context, R.font.poppins_regular));

        pintaBalao.setColor(corAcento);
        pintaTextoBalao.setColor(ContextCompat.getColor(context, R.color.white));
        pintaTextoBalao.setTextSize(13 * dp);
        pintaTextoBalao.setTextAlign(Paint.Align.CENTER);
        pintaTextoBalao.setTypeface(ResourcesCompat.getFont(context, R.font.poppins_semibold));
    }

    // Recebe os cálculos do mais antigo para o mais recente
    public void setCalculos(List<JSONObject> lista) {
        calculos.clear();
        calculos.addAll(lista);
        selecionado = calculos.size() - 1;
        montar();
        animarEntrada();
        avisarSelecao();
    }

    public void setAoSelecionar(AoSelecionar aoSelecionar) {
        this.aoSelecionar = aoSelecionar;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        montar();
    }

    // Calcula a posição de cada ponto e monta a linha
    private void montar() {
        int n = calculos.size();
        if (n == 0 || getWidth() == 0) return;

        minimo = Long.MAX_VALUE;
        maximo = Long.MIN_VALUE;
        for (JSONObject c : calculos) {
            long tmb = c.optLong("tmb");
            minimo = Math.min(minimo, tmb);
            maximo = Math.max(maximo, tmb);
        }
        // Folga em cima e embaixo, arredondada de 50 em 50 kcal
        long folga = Math.max(50, (maximo - minimo) / 5);
        minimo = Math.max(0, (minimo - folga) / 50 * 50);
        maximo = ((maximo + folga) / 50 + 1) * 50;

        float larguraRotulo = pintaRotulo.measureText(String.valueOf(maximo)) + 12 * dp;
        area.set(getPaddingLeft() + larguraRotulo, getPaddingTop() + 32 * dp,
                getWidth() - getPaddingRight() - 6 * dp, getHeight() - getPaddingBottom() - 24 * dp);

        xs = new float[n];
        ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = n == 1 ? area.centerX() : area.left + area.width() * i / (n - 1);
            ys[i] = posicaoY(calculos.get(i).optLong("tmb"));
        }

        // Curva suave ligando os pontos
        linha.reset();
        linha.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) {
            float meio = (xs[i - 1] + xs[i]) / 2;
            linha.cubicTo(meio, ys[i - 1], meio, ys[i], xs[i], ys[i]);
        }

        preenchimento.reset();
        preenchimento.addPath(linha);
        preenchimento.lineTo(xs[n - 1], area.bottom);
        preenchimento.lineTo(xs[0], area.bottom);
        preenchimento.close();

        int transparente = corAcento & 0x00FFFFFF;
        pintaArea.setShader(new LinearGradient(0, area.top, 0, area.bottom,
                transparente | 0x55000000, transparente, Shader.TileMode.CLAMP));
    }

    private float posicaoY(long tmb) {
        return area.bottom - area.height() * (tmb - minimo) / (float) (maximo - minimo);
    }

    // A linha vai sendo "desenhada" da esquerda para a direita
    private void animarEntrada() {
        ValueAnimator animacao = ValueAnimator.ofFloat(0f, 1f);
        animacao.setDuration(900);
        animacao.setInterpolator(new DecelerateInterpolator());
        animacao.addUpdateListener(a -> {
            progresso = (float) a.getAnimatedValue();
            invalidate();
        });
        animacao.start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        int n = calculos.size();
        if (n == 0 || xs.length != n) return;

        desenharGrade(canvas);
        desenharDatas(canvas);

        // Tudo que está à direita do progresso ainda não apareceu
        canvas.save();
        canvas.clipRect(0, 0, area.left + (area.width() + 12 * dp) * progresso, getHeight());
        if (n > 1) {
            canvas.drawPath(preenchimento, pintaArea);
            canvas.drawPath(linha, pintaLinha);
        }
        for (int i = 0; i < n; i++) {
            canvas.drawCircle(xs[i], ys[i], 4.5f * dp, pintaFundoPonto);
            canvas.drawCircle(xs[i], ys[i], 3 * dp, pintaPonto);
        }
        canvas.restore();

        if (progresso >= 1f && selecionado >= 0) desenharSelecao(canvas);
    }

    // Três linhas horizontais com os valores de referência
    private void desenharGrade(Canvas canvas) {
        Paint.FontMetrics fm = pintaRotulo.getFontMetrics();
        for (int i = 0; i <= 2; i++) {
            long valor = minimo + (maximo - minimo) * i / 2;
            float y = posicaoY(valor);
            canvas.drawLine(area.left, y, area.right, y, pintaGrade);
            pintaRotulo.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(String.valueOf(valor), getPaddingLeft(), y - (fm.ascent + fm.descent) / 2, pintaRotulo);
        }
    }

    // Datas embaixo do gráfico, espaçadas para não se sobreporem
    private void desenharDatas(Canvas canvas) {
        int n = calculos.size();
        int quantidade = Math.min(n, MAX_DATAS);
        SimpleDateFormat formato = new SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR"));
        float y = getHeight() - getPaddingBottom();
        for (int k = 0; k < quantidade; k++) {
            int i = quantidade == 1 ? n - 1 : Math.round(k * (n - 1) / (float) (quantidade - 1));
            String data = formato.format(new Date(calculos.get(i).optLong("data")));
            if (quantidade == 1) pintaRotulo.setTextAlign(Paint.Align.CENTER);
            else if (k == 0) pintaRotulo.setTextAlign(Paint.Align.LEFT);
            else if (k == quantidade - 1) pintaRotulo.setTextAlign(Paint.Align.RIGHT);
            else pintaRotulo.setTextAlign(Paint.Align.CENTER);
            float x = quantidade == 1 ? xs[i] : k == 0 ? area.left : k == quantidade - 1 ? area.right : xs[i];
            canvas.drawText(data, x, y, pintaRotulo);
        }
    }

    // Linha guia, ponto destacado e balão com o valor
    private void desenharSelecao(Canvas canvas) {
        float x = xs[selecionado];
        float y = ys[selecionado];
        canvas.drawLine(x, area.top, x, area.bottom, pintaGuia);
        canvas.drawCircle(x, y, 8 * dp, pintaFundoPonto);
        canvas.drawCircle(x, y, 6 * dp, pintaPonto);

        String texto = calculos.get(selecionado).optLong("tmb") + " kcal";
        float largura = pintaTextoBalao.measureText(texto) + 20 * dp;
        float altura = 28 * dp;
        float esquerda = Math.max(0, Math.min(x - largura / 2, getWidth() - largura));
        float topo = Math.max(0, y - altura - 10 * dp);
        RectF balao = new RectF(esquerda, topo, esquerda + largura, topo + altura);
        canvas.drawRoundRect(balao, altura / 2, altura / 2, pintaBalao);
        Paint.FontMetrics fm = pintaTextoBalao.getFontMetrics();
        canvas.drawText(texto, balao.centerX(), balao.centerY() - (fm.ascent + fm.descent) / 2, pintaTextoBalao);
    }

    @Override
    public boolean onTouchEvent(MotionEvent evento) {
        if (calculos.isEmpty()) return false;
        switch (evento.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                selecionarMaisProximo(evento.getX());
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                return true;
        }
        return super.onTouchEvent(evento);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void selecionarMaisProximo(float x) {
        int maisProximo = 0;
        for (int i = 1; i < xs.length; i++) {
            if (Math.abs(xs[i] - x) < Math.abs(xs[maisProximo] - x)) maisProximo = i;
        }
        if (maisProximo != selecionado) {
            selecionado = maisProximo;
            progresso = 1f;
            invalidate();
            avisarSelecao();
        }
    }

    private void avisarSelecao() {
        if (aoSelecionar != null && selecionado >= 0) {
            aoSelecionar.selecionado(calculos.get(selecionado));
        }
    }
}
