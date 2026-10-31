package com.jepongdevxyz.clone;
import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
/** Persists clone records + per-app counters in SharedPreferences (JSON).
 *  Counters never reuse a number: Muse clones go 2, 3, 4... even after deletes.
 *
 *  Durability rules (stress-test fixes, 2026-11-01):
 *  - Every write uses commit(), not apply(): an apply() write lost in a process
 *    death could persist the clone list without its counter (or vice versa),
 *    and the next clone would then REUSE a number — and its data folder.
 *  - nextNumber() never falls back to a constant. The next number is
 *    max(stored counter, highest existing clone number for the package) + 1,
 *    so a corrupted/lost counter can only skip numbers, never reuse one.
 *  - Records parse per item: one malformed record is skipped, it cannot wipe
 *    the whole list. save() also keeps the previous good JSON as a backup,
 *    and all() falls back to it if the primary value is unparseable. */
public class CloneStore {
    private static final String PREFS="devxyzclone_store";
    private static final String KEY_CLONES="clones_json";
    private static final String KEY_CLONES_BAK="clones_json_backup";
    private static final String KEY_COUNTERS="counters_json";
    private final SharedPreferences sp;
    public CloneStore(Context c){ sp=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE); }
    public List<CloneRecord> all(){
        List<CloneRecord> out=parseList(sp.getString(KEY_CLONES,"[]"));
        if(out==null) out=parseList(sp.getString(KEY_CLONES_BAK,"[]"));
        return out==null?new ArrayList<CloneRecord>():out;
    }
    /** @return null only if the JSON is not a parseable array at all. */
    private List<CloneRecord> parseList(String json){
        if(json==null) return null;
        JSONArray a;
        try { a=new JSONArray(json); } catch(Exception e){ return null; }
        List<CloneRecord> out=new ArrayList<>();
        for(int i=0;i<a.length();i++){
            try { out.add(CloneRecord.fromJson(a.getJSONObject(i))); }
            catch(Exception ignored){ /* skip the bad record, keep the rest */ }
        }
        return out;
    }
    private void save(List<CloneRecord> list){
        try { JSONArray a=new JSONArray();
            for(CloneRecord r:list) a.put(r.toJson());
            String prev=sp.getString(KEY_CLONES,null);
            SharedPreferences.Editor e=sp.edit().putString(KEY_CLONES,a.toString());
            if(prev!=null&&parseList(prev)!=null) e.putString(KEY_CLONES_BAK,prev);
            e.commit();
        } catch(Exception ignored){}
    }
    /** Creates the next numbered clone: first clone of Muse is "Muse 2", then 3, 4... */
    public CloneRecord createNext(String pkg,String label){
        if(label==null||label.trim().isEmpty()) label=pkg;
        int num=nextNumber(pkg);
        String def=label+" "+num;
        CloneRecord r=new CloneRecord(UUID.randomUUID().toString(),pkg,label,def,def,num,
            pkg.replace('.','_')+"_clone"+num,System.currentTimeMillis());
        List<CloneRecord> list=all(); list.add(r); save(list); return r;
    }
    private int nextNumber(String pkg){
        int derived=1; // the original app itself is "1"
        for(CloneRecord x:all()) if(x.packageName.equals(pkg)) derived=Math.max(derived,x.cloneNumber);
        JSONObject o;
        try { o=new JSONObject(sp.getString(KEY_COUNTERS,"{}")); }
        catch(Exception e){ o=new JSONObject(); }
        int last=Math.max(o.optInt(pkg,1),derived);
        int n=last+1;
        try { o.put(pkg,n); sp.edit().putString(KEY_COUNTERS,o.toString()).commit(); }
        catch(Exception ignored){ /* in-memory derived max above still prevents reuse this run */ }
        return n;
    }
    public void rename(CloneRecord r,String newName){
        if(newName==null) return;
        String t=newName.trim();
        if(t.isEmpty()) return; // a clone can never end up nameless
        List<CloneRecord> list=all();
        for(CloneRecord x:list) if(x.id.equals(r.id)) x.displayName=t;
        r.displayName=t; save(list);
    }
    public void resetName(CloneRecord r){ rename(r,r.defaultName); }
    public void delete(CloneRecord r){
        List<CloneRecord> list=all(); List<CloneRecord> keep=new ArrayList<>();
        for(CloneRecord x:list) if(!x.id.equals(r.id)) keep.add(x);
        save(keep);
    }
}
