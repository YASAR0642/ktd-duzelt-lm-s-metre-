package com.ktd.analyzer;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/**
 * Deneysel gradyometre metre cetveli icin saha kalibrasyon diyaloğu.
 * Gercek C kanali, 0-1023 ham Z, REF, GPR zaman/derinlik etkilenmez.
 * Birden fazla bilinen hedef ile yerel saha kontrolu gerekir.
 */
public final class GradiometerCalibrationDialog {
    private GradiometerCalibrationDialog() {}

    public static void show(Activity a, Runnable onApplied){
        final float old=Prefs.gradiometerDepthFactor(a);
        LinearLayout fields=new LinearLayout(a);
        fields.setOrientation(LinearLayout.VERTICAL);
        int pad=(int)(16*a.getResources().getDisplayMetrics().density);
        fields.setPadding(pad,pad,pad,pad);

        TextView note=new TextView(a);
        note.setTextColor(Color.WHITE);
        note.setText(String.format(Locale.forLanguageTag("tr-TR"),
            "Deneysel gradyometre derinlik cetveli • mevcut katsayı: %.4f×\n\n"
           +"Bilinen hedefin gösterilen derinliğini ve gerçek derinliğini girin. "
           +"Örnek: gösterilen 40 m, bilinen 4 m → 0,10×. "
           +"Ham Z, referans, CSV ve GPR değişmez. Bu tek nokta başka hedeflerin kesin doğruluğunu kanıtlamaz.",old));
        fields.addView(note);

        EditText shown=new EditText(a);
        shown.setHint("Ekranda görülen metre (örn. 40)");
        shown.setTextColor(Color.WHITE);
        shown.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        fields.addView(shown);

        EditText known=new EditText(a);
        known.setHint("Gerçekte bilinen metre (örn. 4)");
        known.setTextColor(Color.WHITE);
        known.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        fields.addView(known);

        AlertDialog dialog=new AlertDialog.Builder(a)
            .setTitle("GRADYOMETRE • Metre kalibrasyonu")
            .setView(fields)
            .setPositiveButton("KALİBRE ET",null)
            .setNeutralButton("SIFIRLA (1×)",null)
            .setNegativeButton("İPTAL",null)
            .create();
        dialog.setOnShowListener(w->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                try {
                    float before=Float.parseFloat(shown.getText().toString().trim().replace(',','.'));
                    float actual=Float.parseFloat(known.getText().toString().trim().replace(',','.'));
                    float current=Prefs.gradiometerDepthFactor(a);
                    // Girilen "görünen" sayı halihazırda kalibreli ise ikinci
                    // kalibrasyonda sonucu oranlayarak uygula: new=old*(actual/before).
                    if(!Float.isFinite(before)||!Float.isFinite(actual)||before<=0f||actual<=0f)
                        throw new IllegalArgumentException("Metreler pozitif olmalı");
                    float next=current*actual/before;
                    if(!Float.isFinite(next)||next<.001f||next>100f)
                        throw new IllegalArgumentException("Katsayı geçerli aralık dışında");
                    Prefs.setGradiometerDepthFactor(a,next);
                    Toast.makeText(a,String.format(Locale.forLanguageTag("tr-TR"),"Kalibrasyon: %.4f×",next),Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    if(onApplied!=null)onApplied.run();
                }catch(Exception ex){
                    shown.setError("Değerleri kontrol edin");
                    Toast.makeText(a,"Geçerli, pozitif metre değerleri girin",Toast.LENGTH_LONG).show();
                }
            });
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{
                Prefs.setGradiometerDepthFactor(a,1f);
                dialog.dismiss();
                if(onApplied!=null)onApplied.run();
            });
        });
        dialog.show();
    }
}
