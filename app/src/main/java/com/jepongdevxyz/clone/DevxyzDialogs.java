package com.jepongdevxyz.clone;
import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;
/** Modern in-app dialogs built on dialog_shell.xml — this app never uses
 *  default AlertDialog / browser dialogs. */
public class DevxyzDialogs {
    public interface TextSave { void onSave(String text); }
    /** Names are capped so a paste-bomb can't wreck list layouts or the store. */
    static final int MAX_NAME_LEN=60;
    /** The most recent dialog, so an Activity can dismiss it in onDestroy —
     *  a dialog left open across rotation/destroy leaks its window. */
    private static Dialog current;
    public static void dismissCurrent(){
        if(current!=null){ if(current.isShowing()) current.dismiss(); current=null; } }
    public static Dialog base(Context c,String title,String body){
        // Never stack dialogs: a double-tap could otherwise open two, and the
        // one underneath would outlive its Activity. Every caller opens its
        // next dialog only after dismissing the previous one, so replacing a
        // still-showing dialog here is always the right move.
        if(current!=null){ if(current.isShowing()) current.dismiss(); current=null; }
        Dialog d=new Dialog(c);
        current=d;
        d.setOnDismissListener(x->{ if(current==d) current=null; });
        View v=LayoutInflater.from(c).inflate(R.layout.dialog_shell,null);
        d.setContentView(v);
        ((TextView)v.findViewById(R.id.dlgTitle)).setText(title);
        TextView b=v.findViewById(R.id.dlgBody);
        if(body==null||body.isEmpty()) b.setVisibility(View.GONE); else b.setText(body);
        if(d.getWindow()!=null) d.getWindow().setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        return d; }
    static MaterialButton addBtn(Dialog d,String label,boolean filled){
        View v=d.findViewById(android.R.id.content);
        LinearLayout box=v.findViewById(R.id.dlgButtons);
        // 'filled' must mean something: primary actions are contained buttons,
        // secondary actions (Cancel/Reset/Info...) are text buttons — before
        // this fix every button rendered as a filled primary button.
        MaterialButton btn=new MaterialButton(d.getContext());
        if(!filled){
            // Text-button appearance. Do NOT reference
            // material.R.attr.materialTextButtonStyle: that attr does not
            // exist in Material 1.12.0 and broke CI run #2 at this line
            // (compileDebugJavaWithJavac, "cannot find symbol"). Build the
            // same look from guaranteed APIs instead: transparent fill,
            // primary-colored text (day/night aware via dc_primary),
            // no stroke, no elevation, no vertical insets.
            btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
            btn.setTextColor(d.getContext().getColor(R.color.dc_primary));
            btn.setStrokeWidth(0);
            btn.setElevation(0f);
            btn.setInsetTop(0);
            btn.setInsetBottom(0);
        }
        btn.setText(label); box.addView(btn); return btn; }
    public static void message(Context c,String title,String body,Runnable ok){
        Dialog d=base(c,title,body);
        MaterialButton okBtn=addBtn(d,"OK",true);
        okBtn.setOnClickListener(x->{ d.dismiss(); if(ok!=null) ok.run(); });
        d.show(); }
    public static void confirm(Context c,String title,String body,String yesLabel,Runnable yes){
        Dialog d=base(c,title,body);
        MaterialButton y=addBtn(d,yesLabel,true);
        MaterialButton n=addBtn(d,c.getString(R.string.cancel),false);
        y.setOnClickListener(x->{ d.dismiss(); yes.run(); });
        n.setOnClickListener(x->d.dismiss());
        d.show(); }
    /** Rename dialog: prefilled with current name, Save/Cancel + Reset-to-default.
     *  Empty name is rejected inline and the dialog stays open. */
    public static void rename(Context c,CloneRecord r,TextSave save,Runnable reset){
        Dialog d=base(c,"Rename clone","Default name: "+r.defaultName);
        View v=d.findViewById(android.R.id.content);
        EditText in=v.findViewById(R.id.dlgInput);
        in.setFilters(new InputFilter[]{ new InputFilter.LengthFilter(MAX_NAME_LEN) });
        in.setVisibility(View.VISIBLE); in.setText(r.displayName);
        in.setSelection(in.getText().length());
        TextView err=v.findViewById(R.id.dlgError);
        MaterialButton s=addBtn(d,c.getString(R.string.save),true);
        MaterialButton rs=addBtn(d,c.getString(R.string.reset_default),false);
        MaterialButton cn=addBtn(d,c.getString(R.string.cancel),false);
        s.setOnClickListener(x->{
            String t=in.getText().toString().trim();
            if(t.isEmpty()){ err.setText("Name cannot be empty."); err.setVisibility(View.VISIBLE); return; }
            d.dismiss(); save.onSave(t); });
        rs.setOnClickListener(x->{ d.dismiss(); if(reset!=null) reset.run(); });
        cn.setOnClickListener(x->d.dismiss());
        d.show(); }
}
