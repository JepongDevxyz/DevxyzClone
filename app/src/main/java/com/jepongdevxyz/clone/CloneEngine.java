package com.jepongdevxyz.clone;
import android.content.Context;
import java.io.File;
/** The virtual-runtime boundary. Everything about actually RUNNING a cloned app
 *  goes through this interface, so a real virtual engine (BlackBox-class) can be
 *  dropped in later without touching the UI or the store.
 *  CLEAN DESIGN RULE: a future engine must run the target app's ORIGINAL,
 *  UNMODIFIED APK (no repackaging, no re-signing, no patching). */
public interface CloneEngine {
    boolean isRuntimeWired();
    File dataDirFor(Context ctx, CloneRecord r);
    void prepareClone(Context ctx, CloneRecord r);
    void deleteCloneData(Context ctx, CloneRecord r);
    /** @return null if the clone really launched; otherwise an honest user-facing reason. */
    String launchClone(Context ctx, CloneRecord r);
}
