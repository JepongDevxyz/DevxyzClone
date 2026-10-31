package com.jepongdevxyz.clone;
import android.content.Context;
import java.io.File;
import java.nio.file.Files;
/** First implementation: manages clone records' ISOLATED data directories
 *  (files/clones/<dir>/{data,cache,files}) — one per clone, never shared.
 *  The virtual runtime itself is NOT wired yet (see ENGINE-RESEARCH.md:
 *  no open-source engine credibly supports Android 15), so launchClone
 *  honestly reports that instead of faking success.
 *
 *  Delete safety (stress-test fixes, 2026-11-01): dataDirName comes from
 *  persisted JSON, so deletion verifies the canonical target is really
 *  inside the clones root before recursing — a corrupted/tampered value
 *  like "../something" must never delete outside it — and a symlinked
 *  directory is unlinked, never recursed into (recursing would delete the
 *  link target's contents). */
public class LocalCloneEngine implements CloneEngine {
    @Override public boolean isRuntimeWired(){ return false; }
    @Override public File dataDirFor(Context ctx, CloneRecord r){
        return new File(new File(ctx.getFilesDir(),"clones"), r.dataDirName); }
    @Override public void prepareClone(Context ctx, CloneRecord r){
        File d=dataDirFor(ctx,r);
        new File(d,"data").mkdirs(); new File(d,"cache").mkdirs(); new File(d,"files").mkdirs(); }
    @Override public void deleteCloneData(Context ctx, CloneRecord r){
        try {
            String name=r.dataDirName;
            // Generated names are always a plain file name ("<pkg>_clone<N>").
            // Anything with a separator or dot-segment came from corrupted or
            // tampered storage: refuse rather than aim a delete outside root.
            if(name==null||name.isEmpty()||name.contains("/")||name.contains("\\")
                ||name.equals(".")||name.equals("..")) return;
            File root=new File(ctx.getFilesDir(),"clones");
            File target=new File(root,name);
            if(!Files.isSymbolicLink(target.toPath())){
                // Non-link target: its canonical path must sit inside the root.
                // (A symlink target resolves outside by definition, but
                // deleteRec only ever unlinks a symlink — see below.)
                String rootPath=root.getCanonicalPath();
                if(!target.getCanonicalPath().startsWith(rootPath+File.separator)) return;
            }
            deleteRec(target);
        } catch(Exception ignored){} }
    private void deleteRec(File f){
        if(f==null||!f.exists()) return;
        if(Files.isSymbolicLink(f.toPath())){ f.delete(); return; }
        File[] kids=f.listFiles(); if(kids!=null) for(File k:kids) deleteRec(k);
        f.delete(); }
    @Override public String launchClone(Context ctx, CloneRecord r){
        return "The clone runtime is not wired yet in this first build. "
            +"Your clone \""+r.displayName+"\" and its separate data space are saved and ready, "
            +"but DevxyzClone cannot run "+r.originalLabel+" inside itself yet — "
            +"no open-source virtual engine credibly supports Android 15 today (see ENGINE-RESEARCH.md). "
            +"Nothing was faked: the original app was not modified in any way."; }
}
