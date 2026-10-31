package com.jepongdevxyz.clone;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private CloneStore store; private CloneEngine engine;
    private CloneAdapter adapter; private List<CloneRecord> clones;
    private TextView emptyView, emptyBody;
    private boolean pickerOpen=false;

    @Override protected void onCreate(Bundle s){
        super.onCreate(s); setContentView(R.layout.activity_main);
        store=new CloneStore(this); engine=new LocalCloneEngine();
        emptyView=findViewById(R.id.emptyView); emptyBody=findViewById(R.id.emptyBody);
        RecyclerView rv=findViewById(R.id.cloneList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter=new CloneAdapter(); rv.setAdapter(adapter);
        findViewById(R.id.addCloneBtn).setOnClickListener(v->{
            if(pickerOpen) return; // double-tap must not stack two pickers
            pickerOpen=true;
            startActivity(new Intent(this,AppPickerActivity.class)); });
    }
    @Override protected void onResume(){ super.onResume(); pickerOpen=false; refresh(); }
    @Override protected void onDestroy(){ DevxyzDialogs.dismissCurrent(); super.onDestroy(); }
    private void refresh(){
        clones=store.all(); adapter.notifyDataSetChanged();
        int vis=clones.isEmpty()?View.VISIBLE:View.GONE;
        emptyView.setVisibility(vis); emptyBody.setVisibility(vis);
    }
    private void toast(String m){ Toast.makeText(this,m,Toast.LENGTH_LONG).show(); }

    private void onLaunch(CloneRecord r){
        String err=engine.launchClone(this,r);
        if(err==null){ toast("Launching "+r.displayName+"…"); }
        else { DevxyzDialogs.message(this,"Cannot launch yet",err,null); }
    }
    private void onRename(CloneRecord r){
        DevxyzDialogs.rename(this,r,
            name->{ store.rename(r,name); refresh(); toast("Renamed to "+name); },
            ()->{ store.resetName(r); refresh(); toast("Name reset to "+r.defaultName); });
    }
    private void onInfo(CloneRecord r){
        boolean installed=true;
        try { getPackageManager().getApplicationInfo(r.packageName,0); }
        catch(PackageManager.NameNotFoundException e){ installed=false; }
        String body="Clone #"+r.cloneNumber+" of "+r.originalLabel+"\n"
            +"Package: "+r.packageName+"\n"
            +(installed?"":"Note: the original app is no longer installed.\n")
            +"Default name: "+r.defaultName+"\n"
            +"Isolated data folder:\n"+engine.dataDirFor(this,r).getAbsolutePath()+"\n\n"
            +"This folder belongs to this clone only — no other clone and not the original app shares it.";
        DevxyzDialogs.message(this,r.displayName,body,null);
    }
    private void onDelete(CloneRecord r){
        DevxyzDialogs.confirm(this,"Delete "+r.displayName+"?",
            "This removes the clone AND its separate data folder. The original app is not touched.",
            getString(R.string.delete),
            ()->{ engine.deleteCloneData(this,r); store.delete(r); refresh(); toast(r.displayName+" deleted"); });
    }
    private class CloneAdapter extends RecyclerView.Adapter<CloneAdapter.H> {
        class H extends RecyclerView.ViewHolder {
            ImageView icon; TextView name,pkg; View menu;
            H(View v){ super(v); icon=v.findViewById(R.id.cloneIcon);
                name=v.findViewById(R.id.cloneName); pkg=v.findViewById(R.id.clonePkg);
                menu=v.findViewById(R.id.cloneMenuBtn); }
        }
        @NonNull @Override public H onCreateViewHolder(@NonNull ViewGroup p,int t){
            return new H(LayoutInflater.from(p.getContext()).inflate(R.layout.item_clone,p,false)); }
        @Override public void onBindViewHolder(@NonNull H h,int pos){
            CloneRecord r=clones.get(pos);
            h.name.setText(r.displayName); // custom (renamed) name shown everywhere
            String sub=r.packageName+" • Clone #"+r.cloneNumber;
            try { Drawable d=getPackageManager().getApplicationIcon(r.packageName); h.icon.setImageDrawable(d); }
            catch(PackageManager.NameNotFoundException e){
                // Original was uninstalled after cloning: say so, don't show a blank icon.
                h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
                sub+=" • Original app uninstalled"; }
            h.pkg.setText(sub);
            h.itemView.setOnClickListener(v->onLaunch(r));
            h.itemView.setOnLongClickListener(v->{ showActions(r); return true; });
            h.menu.setOnClickListener(v->showActions(r));
        }
        @Override public int getItemCount(){ return clones==null?0:clones.size(); }
    }
    private void showActions(CloneRecord r){
        android.app.Dialog d=DevxyzDialogs.base(this,r.displayName,r.packageName+" • Clone #"+r.cloneNumber);
        DevxyzDialogs.addBtn(d,getString(R.string.launch),true).setOnClickListener(v->{ d.dismiss(); onLaunch(r); });
        DevxyzDialogs.addBtn(d,getString(R.string.rename),false).setOnClickListener(v->{ d.dismiss(); onRename(r); });
        DevxyzDialogs.addBtn(d,getString(R.string.info),false).setOnClickListener(v->{ d.dismiss(); onInfo(r); });
        DevxyzDialogs.addBtn(d,getString(R.string.delete),false).setOnClickListener(v->{ d.dismiss(); onDelete(r); });
        DevxyzDialogs.addBtn(d,getString(R.string.cancel),false).setOnClickListener(v->d.dismiss());
        d.show();
    }
}
