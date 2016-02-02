package com.bennegeek.jbnativeads;

import com.google.android.gms.ads.formats.NativeAd;

/**
 * Created by john on 2/2/16.
 */
public class JBAdsCacheEntry {
    NativeAd ad;
    long timestamp;
    String keyword;

    JBAdsCacheEntry(NativeAd a, long t, String keyword) {
        ad = a;
        timestamp = t;
        keyword = keyword;
    }
}
