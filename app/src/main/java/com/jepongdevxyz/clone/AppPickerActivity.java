package com.jepongdevxyz.clone;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppPickerActivity extends AppCompatActivity {
    static class AppItem { String label,pkg; Drawable icon; }
    private final List<AppItem> all=new ArrayList<>();
    private final List<AppItem> shown=new ArrayList<>();
    private AppAdapter adapter;
    private boolean picked=false;
    private volatile String query="";

    @Override protected void onCreate(Bundle s){
        super.onCreate(s); setContentView(R.layout.activity_picker);
        RecyclerView rv=findViewById(R.id.appList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter=new AppAdapter(); rv.setAdapter(adapter);
        loadAppsAsync();
        EditText search=findViewById(R.id.searchBox);
        search.addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence c,int a,int b,int n){}
            public void onTextChanged(CharSequence c,int a,int b,int n){ query=c.toString(); filter(query); }
            public void afterTextChanged(Editable e){} });
    }
    /** Label/icon loading for every installed app is slow binder work —
     *  doing it in onCreate froze the picker for seconds (stress-test fix).
     *  Load on a worker thread; only the list swap + notify touch the UI. */
    private void loadAppsAsync(){
        new Thread(()->{
            PackageManager pm=getPackageManager();
            Intent q=new Intent(Intent.ACTION_MAIN); q.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> res=pm.queryIntentActivities(q,0);
            Set<String> seen=new HashSet<>();
            List<AppItem> loaded=new ArrayList<>();
            for(ResolveInfo ri:res){
                String pkg=ri.activityInfo.packageName;
                if(pkg.equals(getPackageName())||!seen.add(pkg)) continue;
                AppItem it=new AppItem(); it.pkg=pkg;
                CharSequence lbl=null; Drawable ic=null;
                try { lbl=ri.loadLabel(pm); } catch(Exception ignored){}
                try { ic=ri.loadIcon(pm); } catch(Exception ignored){}
                // A label-less app must not crash the picker or clone into " 2".
                it.label=(lbl==null||lbl.toString().trim().isEmpty())?pkg:lbl.toString();
                it.icon=ic;
                loaded.add(it); }
            Collections.sort(loaded,(a,b)->a.label.compareToIgnoreCase(b.label));
            runOnUiThread(()->{ if(isFinishing()||isDestroyed()) return;
                all.clear(); all.addAll(loaded); filter(query); });
        }).start();
    }
    private void filter(String text){
        shown.clear(); String t=text.toLowerCase(Locale.ROOT);
        for(AppItem it:all) if(it.label.toLowerCase(Locale.ROOT).contains(t)||it.pkg.toLowerCase(Locale.ROOT).contains(t)) shown.add(it);
        if(adapter!=null) adapter.notifyDataSetChanged();
    }
    private void pick(AppItem it){
        if(picked) return; // double-tap guard: one pick creates exactly one clone
        picked=true;
        CloneStore store=new CloneStore(this);
        CloneRecord r=store.createNext(it.pkg,it.label);
        new LocalCloneEngine().prepareClone(this,r);
        Toast.makeText(this,"Clone created: "+r.displayName+" (separate data space ready)",Toast.LENGTH_LONG).show();
        finish();
    }
    private class AppAdapter extends RecyclerView.Adapter<AppAdapter.H> {
        class H extends RecyclerView.ViewHolder {
            ImageView icon; TextView name,pkg;
            H(View v){ super(v); icon=v.findViewById(R.id.appIcon);
                name=v.findViewById(R.id.appName); pkg=v.findViewById(R.id.appPkg); }
        }
        @NonNull @Override public H onCreateViewHolder(@NonNull ViewGroup p,int t){
            return new H(LayoutInflater.from(p.getContext()).inflate(R.layout.item_app,p,false)); }
        @Override public void onBindViewHolder(@NonNull H h,int pos){
            AppItem it=shown.get(pos);
            h.name.setText(it.label); h.pkg.setText(it.pkg); h.icon.setImageDrawable(it.icon);
            h.itemView.setOnClickListener(v->pick(it));
        }
        @Override public int getItemCount(){ return shown.size(); }
    }
}
