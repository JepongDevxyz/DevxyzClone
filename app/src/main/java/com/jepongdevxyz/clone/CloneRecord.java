package com.jepongdevxyz.clone;
import org.json.JSONException;
import org.json.JSONObject;
/** One clone of an installed app. displayName is user-editable (Rename feature);
 *  cloneNumber, packageName and dataDirName are immutable identity fields —
 *  renaming NEVER changes them. */
public class CloneRecord {
    public final String id;
    public final String packageName;
    public final String originalLabel;
    public String displayName;
    public final String defaultName;
    public final int cloneNumber;
    public final String dataDirName;
    public final long createdAt;
    CloneRecord(String id,String pkg,String label,String display,String def,int num,String dir,long at){
        this.id=id; this.packageName=pkg; this.originalLabel=label; this.displayName=display;
        this.defaultName=def; this.cloneNumber=num; this.dataDirName=dir; this.createdAt=at; }
    JSONObject toJson() throws JSONException {
        JSONObject o=new JSONObject();
        o.put("id",id); o.put("pkg",packageName); o.put("label",originalLabel);
        o.put("display",displayName); o.put("def",defaultName); o.put("num",cloneNumber);
        o.put("dir",dataDirName); o.put("at",createdAt); return o; }
    static CloneRecord fromJson(JSONObject o) throws JSONException {
        return new CloneRecord(o.getString("id"),o.getString("pkg"),o.getString("label"),
            o.optString("display",o.getString("def")),o.getString("def"),o.getInt("num"),
            o.getString("dir"),o.optLong("at",0L)); }
}
